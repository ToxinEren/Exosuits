package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.CompatBlockItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * L'oggetto che posa la stazione di manutenzione (design §5.1): un oggetto di blocco, con il nome del blocco e il
 * suggerimento che ne insegna l'uso.
 */
public final class StationItem extends CompatBlockItem {
    /**
     * Le chiavi delle righe del suggerimento, nell'ordine: dove va il telaio (due blocchi, la distanza predefinita: il
     * client non conosce la config del server), come si apre.
     */
    public static final List<String> TOOLTIP_KEYS = List.of(
            "tooltip." + RedstoneExoframes.MOD_ID + ".station.where",
            "tooltip." + RedstoneExoframes.MOD_ID + ".station.open");

    public StationItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    /** Due righe grigie, una per chiave di {@link #TOOLTIP_KEYS}. */
    @Override
    public void tooltip(ItemStack stack, List<Component> lines, boolean advanced) {
        for (String key : TOOLTIP_KEYS) {
            lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }
}
