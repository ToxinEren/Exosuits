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
 * <p>I moduli restano associati allo slot anche se il pezzo viene smontato: funzionano solo con un pezzo
 * integro montato in quello slot (vedi {@link FrameRules#moduleOperational(FrameState, ModuleType)}).
 */
public final class FrameState {

    private static final ModuleInstaller INSTALLER = new ModuleInstaller();

    private final Map<PieceSlot, Piece> pieces = new EnumMap<>(PieceSlot.class);
    private final Map<PieceSlot, List<ModuleType>> modules = new EnumMap<>(PieceSlot.class);
    private final Set<ModuleType> activeToggles = EnumSet.noneOf(ModuleType.class);
    private EnergyCell cell;
    private boolean reinforced;
    private UUID owner;
    private boolean unlocked;

    /** La config non viene conservata: le regole che la leggono stanno in {@link FrameRules}. */
    public FrameState(ExoframesConfig cfg) {
        Objects.requireNonNull(cfg, "cfg");
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

    /** Monta il pezzo nel suo slot, sostituendo quello eventualmente presente. */
    public void mount(Piece p) {
        Objects.requireNonNull(p, "piece");
        pieces.put(p.slot(), p);
    }

    /** Smonta il pezzo dallo slot e lo restituisce (null se lo slot era vuoto). */
    public Piece unmount(PieceSlot s) {
        return pieces.remove(Objects.requireNonNull(s, "slot"));
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
        ModuleInstaller.Reason reason = INSTALLER.canInstall(piece, installed, module);
        if (reason == ModuleInstaller.Reason.OK) {
            installed.add(module);
        }
        return reason;
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
