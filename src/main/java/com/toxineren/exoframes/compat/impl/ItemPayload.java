package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.NbtKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
//? if >=1.21 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
//?}

/**
 * Accesso grezzo al tag radice {@link NbtKeys#ROOT} dello stack: l'unico punto in cui cambia la versione.
 * <p>
 * Su 1.20.1 il tag sta nel tag dello stack; su 1.21.1 nel componente vanilla custom_data, in cui il DFU di vanilla
 * migra il tag dello stack di 1.20.1. Il contenuto del tag radice e' identico sulle due versioni.
 */
final class ItemPayload {
    private ItemPayload() {
    }

    /** Una copia del tag radice, o null se lo stack non lo ha. */
    static CompoundTag read(ItemStack stack) {
        //? if >=1.21 {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag all = data.copyTag();
        return all.contains(NbtKeys.ROOT, CompoundTag.TAG_COMPOUND) ? all.getCompound(NbtKeys.ROOT) : null;
        //?} else {
        /*CompoundTag root = stack.getTagElement(NbtKeys.ROOT);
        return root == null ? null : root.copy();*/
        //?}
    }

    /** Sostituisce il tag radice dello stack con quello dato. */
    static void write(ItemStack stack, CompoundTag tag) {
        //? if >=1.21 {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.put(NbtKeys.ROOT, tag));
        //?} else {
        /*stack.getOrCreateTag().put(NbtKeys.ROOT, tag);*/
        //?}
    }
}
