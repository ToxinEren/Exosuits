package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.ItemDataAccess;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
 * Pezzi ed energia si leggono con il lettore condiviso con lo stato del telaio ({@link PieceTags}), che garantisce
 * gli intervalli dichiarati in {@link ItemDataAccess}.
 * <p>
 * Le scritture partono dal tag radice esistente e sovrascrivono solo le chiavi del record, cosi' le altre chiavi
 * (per esempio {@link NbtKeys#STOWED}) restano intatte.
 */
public final class ItemDataAccessImpl implements ItemDataAccess {
    @Override
    public int cellEnergy(ItemStack stack) {
        CompoundTag root = ItemPayload.read(stack);
        return root == null ? 0 : PieceTags.readEnergy(root);
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
        return root == null ? Optional.empty() : PieceTags.readPiece(root);
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
        Optional<UUID> owner = SchemaIds.readUuid(root, NbtKeys.OWNER);
        Optional<Integer> cellEnergy = root.contains(NbtKeys.CELL, Tag.TAG_COMPOUND)
                ? Optional.of(PieceTags.readEnergy(root.getCompound(NbtKeys.CELL)))
                : Optional.empty();
        Map<PieceSlot, PieceData> pieces = new EnumMap<>(PieceSlot.class);
        CompoundTag piecesTag = root.getCompound(NbtKeys.PIECES);
        for (String key : piecesTag.getAllKeys()) {
            PieceSlot slot = SchemaIds.slotById(key);
            if (slot == null) {
                SchemaIds.warnUnknown("piece slot", key);
            } else if (piecesTag.contains(key, Tag.TAG_COMPOUND)) {
                PieceTags.readPiece(piecesTag.getCompound(key)).ifPresent(piece -> pieces.put(slot, piece));
            }
        }
        return Optional.of(new FrameItemData(owner, root.getBoolean(NbtKeys.UNLOCKED),
                root.getBoolean(NbtKeys.REINFORCED), cellEnergy, pieces));
    }

    @Override
    public void setFrame(ItemStack stack, FrameItemData data) {
        CompoundTag root = rootOrEmpty(stack);
        if (data.owner().isPresent()) {
            root.put(NbtKeys.OWNER, NbtUtils.createUUID(data.owner().get()));
        } else {
            root.remove(NbtKeys.OWNER);
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
            piecesTag.put(SchemaIds.slotId(entry.getKey()), pieceTag);
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
}
