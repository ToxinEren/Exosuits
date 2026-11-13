package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
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
 * 1.21.1, e i pezzi usano le stesse chiavi e gli stessi tipi di {@link ItemDataAccessImpl}.
 * <p>
 * Schema: {@link NbtKeys#PIECES} con una voce per slot (id in minuscolo) che contiene {@link NbtKeys#TIER},
 * {@link NbtKeys#DURABILITY}, {@link NbtKeys#MAX_DURABILITY}, {@link NbtKeys#MODULES} e
 * {@link NbtKeys#CARRY_MILLI}; {@link NbtKeys#CELL} con {@link NbtKeys#ENERGY} e {@link NbtKeys#CAPACITY} (assente
 * = nessuna cella); {@link NbtKeys#ACTIVE}; {@link NbtKeys#REINFORCED}; {@link NbtKeys#OWNER} (assente = nessun
 * proprietario); {@link NbtKeys#UNLOCKED}. {@link NbtKeys#STOWED} non e' ancora scritto ne' letto.
 * <p>
 * La lettura e' tollerante: id sconosciuti di slot, livello o modulo vengono ignorati con un solo avviso per id
 * (conversioni condivise con gli item in {@link SchemaIds}); un proprietario malformato vale assente;
 * valori fuori intervallo vengono riportati nell'intervallo valido o, se non recuperabili, scartati con un avviso.
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
     * Ricostruisce lo stato. Ordine: pezzi montati, poi i loro moduli, poi i riporti del danno (montare un pezzo
     * azzera il riporto e toglie moduli e toggle dello slot), infine i toggle accesi.
     */
    static FrameState read(CompoundTag root, ExoframesConfig cfg) {
        FrameState state = new FrameState(cfg);
        CompoundTag piecesTag = root.getCompound(NbtKeys.PIECES);
        Map<PieceSlot, CompoundTag> mounted = new EnumMap<>(PieceSlot.class);
        for (String key : piecesTag.getAllKeys()) {
            PieceSlot slot = SchemaIds.slotById(key);
            if (slot == null) {
                SchemaIds.warnUnknown("piece slot", key);
            } else if (piecesTag.contains(key, Tag.TAG_COMPOUND)) {
                CompoundTag pieceTag = piecesTag.getCompound(key);
                Piece piece = readPiece(slot, pieceTag);
                if (piece != null) {
                    state.mount(piece);
                    mounted.put(slot, pieceTag);
                }
            }
        }
        for (Map.Entry<PieceSlot, CompoundTag> entry : mounted.entrySet()) {
            PieceSlot slot = entry.getKey();
            ListTag list = entry.getValue().getList(NbtKeys.MODULES, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ModuleType module = SchemaIds.moduleById(list.getString(i));
                if (module != null) {
                    ModuleInstaller.Reason reason = state.installModule(slot, module);
                    if (reason != ModuleInstaller.Reason.OK) {
                        RedstoneExoframes.LOGGER.warn("Dropping module {} from frame slot {}: {}", module.id(),
                                SchemaIds.slotId(slot), reason);
                    }
                }
            }
            int carry = clampCarry(entry.getValue().getInt(NbtKeys.CARRY_MILLI));
            if (carry > 0) {
                // damageCarryMilli() e' la mappa viva dello stato: si scrive direttamente
                state.damageCarryMilli().put(slot, carry);
            }
        }
        if (root.contains(NbtKeys.CELL, Tag.TAG_COMPOUND)) {
            CompoundTag cellTag = root.getCompound(NbtKeys.CELL);
            int capacity = cellTag.getInt(NbtKeys.CAPACITY);
            if (capacity > 0) {
                int stored = Math.max(0, Math.min(capacity, cellTag.getInt(NbtKeys.ENERGY)));
                state.insertCell(new EnergyCell(capacity, stored));
            } else {
                RedstoneExoframes.LOGGER.warn("Dropping frame cell with invalid capacity {}", capacity);
            }
        }
        ListTag active = root.getList(NbtKeys.ACTIVE, Tag.TAG_STRING);
        for (int i = 0; i < active.size(); i++) {
            ModuleType module = SchemaIds.moduleById(active.getString(i));
            if (module != null) {
                // activeToggles() e' l'insieme vivo dello stato: si scrive direttamente
                state.activeToggles().add(module);
            }
        }
        state.setReinforced(root.getBoolean(NbtKeys.REINFORCED));
        state.setOwner(SchemaIds.readUuid(root, NbtKeys.OWNER).orElse(null));
        state.setUnlocked(root.getBoolean(NbtKeys.UNLOCKED));
        return state;
    }

    /** Il riporto del danno riportato nell'intervallo [0, 999]. */
    static int clampCarry(int carry) {
        return Math.max(0, Math.min(999, carry));
    }

    /** Legge il pezzo di una voce di {@link NbtKeys#PIECES}, o null se il livello o la durabilita' non sono validi. */
    private static Piece readPiece(PieceSlot slot, CompoundTag tag) {
        if (!tag.contains(NbtKeys.TIER, Tag.TAG_STRING)) {
            return null;
        }
        Tier tier = SchemaIds.tierById(tag.getString(NbtKeys.TIER));
        if (tier == null) {
            return null;
        }
        int max = tag.getInt(NbtKeys.MAX_DURABILITY);
        if (max <= 0) {
            RedstoneExoframes.LOGGER.warn("Dropping frame piece in slot {} with invalid max durability {}",
                    SchemaIds.slotId(slot), max);
            return null;
        }
        int durability = Math.max(0, Math.min(max, tag.getInt(NbtKeys.DURABILITY)));
        return new Piece(slot, tier, max, durability);
    }

    private static ListTag idList(Iterable<ModuleType> modules) {
        ListTag list = new ListTag();
        for (ModuleType module : modules) {
            list.add(StringTag.valueOf(module.id()));
        }
        return list;
    }
}
