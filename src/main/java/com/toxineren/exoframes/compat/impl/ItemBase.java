package com.toxineren.exoframes.compat.impl;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if <1.21 {
/*import net.minecraft.world.level.Level;
*///?}

/**
 * Base degli oggetti della mod con un suggerimento: chiude le differenze di {@link Item} fra 1.20.1 e 1.21.1 dietro un
 * metodo neutro senza versione. Il codice della mod non la estende direttamente: estende {@code compat.CompatItem}.
 * <p>
 * Differenze chiuse:
 * <ul>
 * <li>suggerimento: {@code appendHoverText(ItemStack, Level, List<Component>, TooltipFlag)} su 1.20.1 (il livello puo'
 * essere nullo), {@code appendHoverText(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)} su 1.21.1; le
 * sottoclassi aggiungono le loro righe in {@link #tooltip}, che l'override di ogni versione chiama dopo le righe del
 * gioco base, con {@code TooltipFlag.isAdvanced()} (le informazioni avanzate, F3+H).</li>
 * </ul>
 * Le barre sotto l'icona ({@code isBarVisible}, {@code getBarWidth}, {@code getBarColor}) hanno la stessa firma sulle
 * due versioni: le sottoclassi le ridefiniscono direttamente.
 */
public abstract class ItemBase extends Item {
    protected ItemBase(Item.Properties properties) {
        super(properties);
    }

    /**
     * Aggiunge in coda a {@code lines} le righe del suggerimento di {@code stack}; {@code advanced} e' vero con le
     * informazioni avanzate accese. Di base non aggiunge nulla.
     */
    public void tooltip(ItemStack stack, List<Component> lines, boolean advanced) {
    }

    //? if >=1.21 {
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        tooltip(stack, lines, flag.isAdvanced());
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        tooltip(stack, lines, flag.isAdvanced());
    }
    *///?}
}
