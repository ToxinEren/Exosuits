package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.ItemDataAccess.PieceData;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.module.Activation;
import com.toxineren.exoframes.core.module.ModuleInstaller;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Serializzazione di un {@link FrameState} in un CompoundTag con le chiavi di {@link NbtKeys}, usata sia dal dato
 * salvato sia dal pacchetto di sincronizzazione. Nessuna condizione di versione: lo schema e' lo stesso su 1.20.1 e
 * 1.21.1, e i pezzi e l'energia della cella si leggono (e l'aspetto dei pezzi si scrive) con lo stesso helper degli
 * item ({@link PieceTags}).
 * <p>
 * Schema: {@link NbtKeys#PIECES} con una voce per slot (id in minuscolo) che contiene {@link NbtKeys#TIER},
 * {@link NbtKeys#DURABILITY}, {@link NbtKeys#MAX_DURABILITY}, {@link NbtKeys#MODULES}, l'aspetto
 * ({@link NbtKeys#COLOR_PRIMARY}, {@link NbtKeys#COLOR_SECONDARY}, {@link NbtKeys#DECALS}) e
 * {@link NbtKeys#CARRY_MILLI}; {@link NbtKeys#CELL} con {@link NbtKeys#ENERGY} e {@link NbtKeys#CAPACITY} (assente
 * = nessuna cella); {@link NbtKeys#ACTIVE}; {@link NbtKeys#REINFORCED}; {@link NbtKeys#OWNER} (assente = nessun
 * proprietario); {@link NbtKeys#UNLOCKED}. L'armatura stivata ({@link NbtKeys#STOWED}) non fa parte dello stato: la
 * scrive e la legge il registro dei telai ({@link FrameStateSavedData}), accanto allo stato di ogni record.
 * <p>
 * La lettura e' tollerante: id sconosciuti di slot, livello o modulo vengono ignorati con un solo avviso per id
 * (conversioni condivise con gli item in {@link SchemaIds}); un proprietario malformato vale assente;
 * valori fuori intervallo vengono riportati nell'intervallo valido o, se non recuperabili, scartati con un avviso
 * (una volta per chiave, come gli id sconosciuti). Un toggle acceso si ripristina solo se il modulo ha attivazione
 * {@link Activation#TOGGLE} ed e' installato in qualche slot.
 */
final class FrameStateCodec {
    private FrameStateCodec() {
    }

    static CompoundTag write(FrameState state) {
        CompoundTag root = new CompoundTag();
        CompoundTag piecesTag = new CompoundTag();
        Map<PieceSlot, List<ModuleType>> modules = state.modules();
        for (Map.Entry<PieceSlot, Piece> entry : state.pieces().entrySet()) {
            PieceSlot slot = entry.getKey();
            Piece piece = entry.getValue();
            CompoundTag pieceTag = new CompoundTag();
            pieceTag.putString(NbtKeys.TIER, piece.tier().id());
            pieceTag.putInt(NbtKeys.DURABILITY, piece.durability());
            pieceTag.putInt(NbtKeys.MAX_DURABILITY, piece.maxDurability());
            pieceTag.put(NbtKeys.MODULES, idList(modules.getOrDefault(slot, List.of())));
            PieceTags.writeAppearance(pieceTag, state.appearance(slot));
            pieceTag.putInt(NbtKeys.CARRY_MILLI, state.damageCarryMilli().getOrDefault(slot, 0));
            piecesTag.put(SchemaIds.slotId(slot), pieceTag);
        }
        root.put(NbtKeys.PIECES, piecesTag);
        EnergyCell cell = state.cell();
        if (cell != null) {
            CompoundTag cellTag = new CompoundTag();
            cellTag.putInt(NbtKeys.ENERGY, cell.stored());
            cellTag.putInt(NbtKeys.CAPACITY, cell.capacity());
            root.put(NbtKeys.CELL, cellTag);
        }
        root.put(NbtKeys.ACTIVE, idList(state.activeToggles()));
        root.putBoolean(NbtKeys.REINFORCED, state.reinforced());
        if (state.owner() != null) {
            root.put(NbtKeys.OWNER, NbtUtils.createUUID(state.owner()));
        }
        root.putBoolean(NbtKeys.UNLOCKED, state.unlocked());
        return root;
    }

    /**
     * Ricostruisce lo stato validando i moduli contro la config di chi legge: ogni modulo passa da
     * {@link FrameState#installModule} e quelli rifiutati (slot pieni, livello troppo basso...) vengono scartati con
     * un avviso. E' la lettura del caricamento sul server. Ordine: pezzi montati, poi i loro moduli, i riporti del
     * danno e gli aspetti (montare un pezzo azzera riporto e aspetto e toglie moduli e toggle dello slot), infine i
     * toggle accesi dei moduli toggle installati.
     */
    static FrameState read(CompoundTag root, ExoframesConfig cfg) {
        return read(root, cfg, false);
    }

    /**
     * Come {@link #read}, ma i moduli si ripristinano con {@link FrameState#restoreModules} senza i controlli
     * dell'installatore: nessun modulo viene scartato per colpa della config di chi legge. Serve per uno stato gia'
     * validato da chi l'ha scritto (la copia del client ricevuta dal server). Gli id sconosciuti si saltano come in
     * {@link #read} e i doppioni di uno slot li toglie gia' il lettore condiviso ({@link PieceTags#readPiece}, che
     * tiene il primo), cosi' un tag corrotto non fa lanciare {@code restoreModules}.
     */
    static FrameState readTrusted(CompoundTag root, ExoframesConfig cfg) {
        return read(root, cfg, true);
    }

    private static FrameState read(CompoundTag root, ExoframesConfig cfg, boolean trusted) {
        FrameState state = new FrameState(cfg);
        CompoundTag piecesTag = root.getCompound(NbtKeys.PIECES);
        Map<PieceSlot, CompoundTag> mounted = new EnumMap<>(PieceSlot.class);
        Map<PieceSlot, PieceData> pieceData = new EnumMap<>(PieceSlot.class);
        for (String key : piecesTag.getAllKeys()) {
            PieceSlot slot = SchemaIds.slotById(key);
            if (slot == null) {
                SchemaIds.warnUnknown("piece slot", key);
            } else if (piecesTag.contains(key, Tag.TAG_COMPOUND)) {
                CompoundTag pieceTag = piecesTag.getCompound(key);
                PieceTags.readPiece(pieceTag).ifPresent(data -> {
                    state.mount(new Piece(slot, data.tier(), data.maxDurability(), data.durability()));
                    mounted.put(slot, pieceTag);
                    pieceData.put(slot, data);
                });
            }
        }
        for (Map.Entry<PieceSlot, CompoundTag> entry : mounted.entrySet()) {
            PieceSlot slot = entry.getKey();
            PieceData data = pieceData.get(slot);
            if (trusted) {
                state.restoreModules(slot, data.modules());
            } else {
                installModules(state, slot, data.modules());
            }
            int carry = clampCarry(entry.getValue().getInt(NbtKeys.CARRY_MILLI));
            if (carry > 0) {
                // damageCarryMilli() e' la mappa viva dello stato: si scrive direttamente
                state.damageCarryMilli().put(slot, carry);
            }
            state.setAppearance(slot, data.appearance());
        }
        if (root.contains(NbtKeys.CELL, Tag.TAG_COMPOUND)) {
            CompoundTag cellTag = root.getCompound(NbtKeys.CELL);
            int capacity = cellTag.getInt(NbtKeys.CAPACITY);
            if (capacity > 0) {
                int stored = Math.min(capacity, PieceTags.readEnergy(cellTag));
                state.insertCell(new EnergyCell(capacity, stored));
            } else {
                SchemaIds.warnOnce("drop cell capacity:" + capacity, "Dropping frame cell with invalid capacity {}",
                        capacity);
            }
        }
        ListTag active = root.getList(NbtKeys.ACTIVE, Tag.TAG_STRING);
        for (int i = 0; i < active.size(); i++) {
            ModuleType module = SchemaIds.moduleById(active.getString(i));
            if (module != null && module.activation() == Activation.TOGGLE && isInstalled(state, module)) {
                // activeToggles() e' l'insieme vivo dello stato: si scrive direttamente
                state.activeToggles().add(module);
            }
        }
        state.setReinforced(root.getBoolean(NbtKeys.REINFORCED));
        state.setOwner(SchemaIds.readUuid(root, NbtKeys.OWNER).orElse(null));
        state.setUnlocked(root.getBoolean(NbtKeys.UNLOCKED));
        return state;
    }

    /** Installa i moduli validandoli; quelli rifiutati si scartano con un avviso (una volta per motivo). */
    private static void installModules(FrameState state, PieceSlot slot, List<ModuleType> modules) {
        for (ModuleType module : modules) {
            ModuleInstaller.Reason reason = state.installModule(slot, module);
            if (reason != ModuleInstaller.Reason.OK) {
                SchemaIds.warnOnce("drop module:" + module.id() + ":" + SchemaIds.slotId(slot) + ":" + reason,
                        "Dropping module {} from frame slot {}: {}", module.id(), SchemaIds.slotId(slot), reason);
            }
        }
    }

    /** Il riporto del danno riportato nell'intervallo [0, 999]. */
    static int clampCarry(int carry) {
        return Math.max(0, Math.min(999, carry));
    }

    /** Vero se il modulo e' installato in almeno uno slot dello stato. */
    private static boolean isInstalled(FrameState state, ModuleType module) {
        for (List<ModuleType> installed : state.modules().values()) {
            if (installed.contains(module)) {
                return true;
            }
        }
        return false;
    }

    private static ListTag idList(Iterable<ModuleType> modules) {
        ListTag list = new ListTag();
        for (ModuleType module : modules) {
            list.add(StringTag.valueOf(module.id()));
        }
        return list;
    }
}
