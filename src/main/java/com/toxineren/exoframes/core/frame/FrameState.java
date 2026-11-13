package com.toxineren.exoframes.core.frame;

import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.module.ModuleInstaller;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Stato mutabile di un telaio: cella, pezzi montati, moduli installati per slot, toggle accesi,
 * rinforzo e proprietario. Gli effetti si calcolano con {@link FrameRules}.
 *
 * <p>I moduli appartengono al pezzo montato nel loro slot: smontare o sostituire il pezzo toglie dal telaio
 * i moduli di quello slot (e i loro toggle accesi). Per riportarli sull'oggetto del pezzo, il livello di
 * piattaforma chiama {@link #takeModules(PieceSlot)} prima di {@link #unmount(PieceSlot)}.
 *
 * <p>Il riporto frazionario del danno ({@link #damageCarryMilli()}) e' per slot e appartiene al pezzo montato:
 * smontarlo o sostituirlo lo azzera.
 */
public final class FrameState {

    private final ExoframesConfig cfg;
    private final ModuleInstaller installer;
    private final Map<PieceSlot, Piece> pieces = new EnumMap<>(PieceSlot.class);
    private final Map<PieceSlot, List<ModuleType>> modules = new EnumMap<>(PieceSlot.class);
    private final Set<ModuleType> activeToggles = EnumSet.noneOf(ModuleType.class);
    private final EnumMap<PieceSlot, Integer> damageCarryMilli = new EnumMap<>(PieceSlot.class);
    private EnergyCell cell;
    private boolean reinforced;
    private UUID owner;
    private boolean unlocked;

    /**
     * La config viene conservata per riferimento: l'installazione dei moduli legge {@code pieces.baseModuleSlots};
     * gli effetti si calcolano con {@link FrameRules}.
     */
    public FrameState(ExoframesConfig cfg) {
        this.cfg = Objects.requireNonNull(cfg, "cfg");
        this.installer = new ModuleInstaller(cfg.pieces);
    }

    /** Cella inserita, oppure null. */
    public EnergyCell cell() {
        return cell;
    }

    /** Inserisce la cella sostituendo quella eventualmente presente. */
    public void insertCell(EnergyCell c) {
        this.cell = Objects.requireNonNull(c, "cell");
    }

    /** Toglie la cella e la restituisce (null se non c'era). */
    public EnergyCell removeCell() {
        EnergyCell removed = cell;
        cell = null;
        return removed;
    }

    /** Vista in sola lettura dei pezzi montati. */
    public Map<PieceSlot, Piece> pieces() {
        return Collections.unmodifiableMap(pieces);
    }

    /**
     * Monta il pezzo nel suo slot, sostituendo quello eventualmente presente; lo slot riparte senza moduli
     * (quelli del pezzo sostituito vanno ripresi prima con {@link #takeModules(PieceSlot)}) e con il riporto
     * del danno azzerato.
     */
    public void mount(Piece p) {
        Objects.requireNonNull(p, "piece");
        takeModules(p.slot());
        damageCarryMilli.remove(p.slot());
        pieces.put(p.slot(), p);
    }

    /**
     * Smonta il pezzo dallo slot e lo restituisce (null se lo slot era vuoto). Toglie anche i moduli dello
     * slot e i loro toggle (chi deve conservarli li riprende prima con {@link #takeModules(PieceSlot)}) e azzera
     * il riporto del danno dello slot.
     */
    public Piece unmount(PieceSlot s) {
        takeModules(Objects.requireNonNull(s, "slot"));
        damageCarryMilli.remove(s);
        return pieces.remove(s);
    }

    /**
     * Toglie e restituisce i moduli installati nello slot (lista vuota se nessuno), spegnendo i loro toggle.
     * Il pezzo resta montato.
     */
    public List<ModuleType> takeModules(PieceSlot slot) {
        List<ModuleType> removed = modules.remove(Objects.requireNonNull(slot, "slot"));
        if (removed == null) {
            return List.of();
        }
        removed.forEach(activeToggles::remove);
        return List.copyOf(removed);
    }

    /** Vista in sola lettura dei moduli installati per slot. */
    public Map<PieceSlot, List<ModuleType>> modules() {
        Map<PieceSlot, List<ModuleType>> view = new EnumMap<>(PieceSlot.class);
        modules.forEach((slot, list) -> view.put(slot, Collections.unmodifiableList(list)));
        return Collections.unmodifiableMap(view);
    }

    /**
     * Installa il modulo sul pezzo montato nello slot se {@link ModuleInstaller#canInstall} lo consente;
     * restituisce il motivo (OK se installato).
     *
     * @throws IllegalStateException se nello slot non c'e' un pezzo montato
     */
    public ModuleInstaller.Reason installModule(PieceSlot slot, ModuleType module) {
        Objects.requireNonNull(module, "module");
        Piece piece = pieces.get(Objects.requireNonNull(slot, "slot"));
        if (piece == null) {
            throw new IllegalStateException("no piece mounted in " + slot);
        }
        List<ModuleType> installed = modules.computeIfAbsent(slot, k -> new ArrayList<>());
        ModuleInstaller.Reason reason = installer.canInstall(piece, installed, module);
        if (reason == ModuleInstaller.Reason.OK) {
            installed.add(module);
        }
        return reason;
    }

    /**
     * Mappa mutabile del riporto frazionario del danno per slot, in millesimi di punto (uno slot assente vale 0):
     * da passare a {@link com.toxineren.exoframes.core.armor.DamageRouter#route(
     * com.toxineren.exoframes.core.armor.DamageKind, int, Map, Map)}, che la aggiorna. Ogni valore sta in
     * (-1000, 1000): negativo se il pezzo ha gia' ricevuto in anticipo un punto di usura.
     */
    public EnumMap<PieceSlot, Integer> damageCarryMilli() {
        return damageCarryMilli;
    }

    /** Insieme mutabile dei moduli toggle accesi dal giocatore. */
    public Set<ModuleType> activeToggles() {
        return activeToggles;
    }

    public boolean reinforced() {
        return reinforced;
    }

    public void setReinforced(boolean reinforced) {
        this.reinforced = reinforced;
    }

    /** Proprietario del telaio, oppure null finche' non viene assegnato. */
    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    /** True se il proprietario ha sbloccato il telaio per chiunque. */
    public boolean unlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }
}
