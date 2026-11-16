package com.toxineren.exoframes.compat.impl;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
//? if <1.21 {
/*import net.minecraft.world.level.Level;
*///?}

/**
 * Base degli oggetti della mod che posano un blocco e hanno un suggerimento: come {@link ItemBase}, ma estende
 * {@link BlockItem}. Chiude la stessa differenza fra 1.20.1 e 1.21.1 dietro lo stesso metodo neutro. Il codice della
 * mod non la estende direttamente: estende {@code compat.CompatBlockItem}.
 * <p>
 * Differenze chiuse:
 * <ul>
 * <li>suggerimento: {@code appendHoverText(ItemStack, Level, List<Component>, TooltipFlag)} su 1.20.1 (il livello puo'
 * essere nullo), {@code appendHoverText(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)} su 1.21.1; le
 * sottoclassi aggiungono le loro righe in {@link #tooltip}, che l'override di ogni versione chiama dopo le righe del
 * gioco base (quelle di {@link BlockItem}, che chiede anche al blocco le sue), con {@code TooltipFlag.isAdvanced()}
 * (le informazioni avanzate, F3+H).</li>
 * </ul>
 */
public abstract class BlockItemBase extends BlockItem {
    protected BlockItemBase(Block block, Item.Properties properties) {
        super(block, properties);
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
