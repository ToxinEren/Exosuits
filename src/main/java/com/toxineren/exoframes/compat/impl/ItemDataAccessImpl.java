package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.ItemDataAccess;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Implementazione di {@link ItemDataAccess}: serializza i record con le chiavi di {@link NbtKeys} in un CompoundTag
 * e delega a {@link ItemPayload} lettura e scrittura del tag radice. Qui non c'e' nessuna condizione di versione:
 * lo schema e' lo stesso su 1.20.1 e 1.21.1.
 * <p>
 * Le scritture partono dal tag radice esistente e sovrascrivono solo le chiavi del record, cosi' le altre chiavi
 * (per esempio {@link NbtKeys#STOWED}) restano intatte.
 */
public final class ItemDataAccessImpl implements ItemDataAccess {
    /** Id sconosciuti gia' segnalati nel log, per avvisare una volta sola per id. */
    private static final Set<String> WARNED_IDS = ConcurrentHashMap.newKeySet();

    @Override
    public int cellEnergy(ItemStack stack) {
        CompoundTag root = ItemPayload.read(stack);
        return root == null ? 0 : root.getInt(NbtKeys.ENERGY);
    }

    @Override
    public void setCellEnergy(ItemStack stack, int energy) {
        CompoundTag root = rootOrEmpty(stack);
        root.putInt(NbtKeys.ENERGY, energy);
        ItemPayload.write(stack, root);
    }

    @Override
    public Optional<PieceData> piece(ItemStack stack) {
        CompoundTag root = ItemPayload.read(stack);
        return root == null ? Optional.empty() : readPiece(root);
    }

    @Override
    public void setPiece(ItemStack stack, PieceData data) {
        CompoundTag root = rootOrEmpty(stack);
        writePiece(root, data);
        ItemPayload.write(stack, root);
    }

    @Override
    public Optional<FrameItemData> frame(ItemStack stack) {
        CompoundTag root = ItemPayload.read(stack);
        if (root == null) {
            return Optional.empty();
        }
        UUID owner = root.contains(NbtKeys.OWNER, Tag.TAG_INT_ARRAY) ? NbtUtils.loadUUID(root.get(NbtKeys.OWNER)) : null;
        Optional<Integer> cellEnergy = root.contains(NbtKeys.CELL, Tag.TAG_COMPOUND)
                ? Optional.of(root.getCompound(NbtKeys.CELL).getInt(NbtKeys.ENERGY))
                : Optional.empty();
        Map<PieceSlot, PieceData> pieces = new EnumMap<>(PieceSlot.class);
        CompoundTag piecesTag = root.getCompound(NbtKeys.PIECES);
        for (String key : piecesTag.getAllKeys()) {
            PieceSlot slot = slotById(key);
            if (slot == null) {
                warnUnknown("piece slot", key);
            } else if (piecesTag.contains(key, Tag.TAG_COMPOUND)) {
                readPiece(piecesTag.getCompound(key)).ifPresent(piece -> pieces.put(slot, piece));
            }
        }
        return Optional.of(new FrameItemData(owner, root.getBoolean(NbtKeys.UNLOCKED),
                root.getBoolean(NbtKeys.REINFORCED), cellEnergy, pieces));
    }

    @Override
    public void setFrame(ItemStack stack, FrameItemData data) {
        CompoundTag root = rootOrEmpty(stack);
        if (data.owner() == null) {
            root.remove(NbtKeys.OWNER);
        } else {
            root.put(NbtKeys.OWNER, NbtUtils.createUUID(data.owner()));
        }
        root.putBoolean(NbtKeys.UNLOCKED, data.unlocked());
        root.putBoolean(NbtKeys.REINFORCED, data.reinforced());
        if (data.cellEnergy().isPresent()) {
            CompoundTag cell = new CompoundTag();
            cell.putInt(NbtKeys.ENERGY, data.cellEnergy().get());
            root.put(NbtKeys.CELL, cell);
        } else {
            root.remove(NbtKeys.CELL);
        }
        CompoundTag piecesTag = new CompoundTag();
        for (Map.Entry<PieceSlot, PieceData> entry : data.pieces().entrySet()) {
            CompoundTag pieceTag = new CompoundTag();
            writePiece(pieceTag, entry.getValue());
            piecesTag.put(slotId(entry.getKey()), pieceTag);
        }
        root.put(NbtKeys.PIECES, piecesTag);
        ItemPayload.write(stack, root);
    }

    @Override
    public boolean hasData(ItemStack stack) {
        return ItemPayload.read(stack) != null;
    }

    /** Il tag radice esistente (copia) o uno nuovo vuoto. */
    private static CompoundTag rootOrEmpty(ItemStack stack) {
        CompoundTag root = ItemPayload.read(stack);
        return root == null ? new CompoundTag() : root;
    }

    /** Legge un pezzo da un tag (il tag radice di un pezzo o una voce di {@link NbtKeys#PIECES}). */
    private static Optional<PieceData> readPiece(CompoundTag tag) {
        if (!tag.contains(NbtKeys.TIER, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        Tier tier = tierById(tag.getString(NbtKeys.TIER));
        if (tier == null) {
            return Optional.empty();
        }
        List<ModuleType> modules = new ArrayList<>();
        ListTag list = tag.getList(NbtKeys.MODULES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ModuleType module = moduleById(list.getString(i));
            if (module != null) {
                modules.add(module);
            }
        }
        return Optional.of(new PieceData(tier, tag.getInt(NbtKeys.DURABILITY), tag.getInt(NbtKeys.MAX_DURABILITY),
                List.copyOf(modules), tag.getInt(NbtKeys.COLOR_PRIMARY), tag.getInt(NbtKeys.COLOR_SECONDARY)));
    }

    /** Scrive tutti i campi di un pezzo nel tag dato. */
    private static void writePiece(CompoundTag tag, PieceData data) {
        tag.putString(NbtKeys.TIER, data.tier().id());
        tag.putInt(NbtKeys.DURABILITY, data.durability());
        tag.putInt(NbtKeys.MAX_DURABILITY, data.maxDurability());
        ListTag modules = new ListTag();
        for (ModuleType module : data.modules()) {
            modules.add(StringTag.valueOf(module.id()));
        }
        tag.put(NbtKeys.MODULES, modules);
        tag.putInt(NbtKeys.COLOR_PRIMARY, data.colorPrimary());
        tag.putInt(NbtKeys.COLOR_SECONDARY, data.colorSecondary());
    }

    private static Tier tierById(String id) {
        try {
            return Tier.byId(id);
        } catch (IllegalArgumentException e) {
            warnUnknown("tier", id);
            return null;
        }
    }

    private static ModuleType moduleById(String id) {
        try {
            return ModuleType.byId(id);
        } catch (IllegalArgumentException e) {
            warnUnknown("module", id);
            return null;
        }
    }

    private static String slotId(PieceSlot slot) {
        return slot.name().toLowerCase(Locale.ROOT);
    }

    private static PieceSlot slotById(String id) {
        for (PieceSlot slot : PieceSlot.values()) {
            if (slotId(slot).equals(id)) {
                return slot;
            }
        }
        return null;
    }

    private static void warnUnknown(String kind, String id) {
        if (WARNED_IDS.add(kind + ":" + id)) {
            RedstoneExoframes.LOGGER.warn("Ignoring unknown {} id in item data: {}", kind, id);
        }
    }
}
