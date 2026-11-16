package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.CompatBlockItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** L'oggetto che posa il caricatore di celle: un oggetto di blocco con il suggerimento che ne insegna l'uso. */
public final class ChargerItem extends CompatBlockItem {
    /**
     * Le chiavi delle righe del suggerimento, nell'ordine: a che serve, la cella a mano, la polvere a mano, leggere lo
     * stato, le tramogge.
     */
    public static final List<String> TOOLTIP_KEYS = List.of(
            "tooltip." + RedstoneExoframes.MOD_ID + ".charger.what",
            "tooltip." + RedstoneExoframes.MOD_ID + ".charger.cell",
            "tooltip." + RedstoneExoframes.MOD_ID + ".charger.dust",
            "tooltip." + RedstoneExoframes.MOD_ID + ".charger.status",
            "tooltip." + RedstoneExoframes.MOD_ID + ".charger.hoppers");

    public ChargerItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    /** Cinque righe grigie, una per chiave di {@link #TOOLTIP_KEYS}. */
    @Override
    public void tooltip(ItemStack stack, List<Component> lines, boolean advanced) {
        for (String key : TOOLTIP_KEYS) {
            lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }
}
