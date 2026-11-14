package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.content.ModItems;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Vestizione di prova del telaio: mette i quattro segnaposto negli slot armatura vanilla e scrive il riassunto visivo
 * sullo stack del torso, cosi' il layer di rendering ha qualcosa da disegnare. Non tocca lo stato del telaio
 * ({@link Compat#frameState()}): serve solo a vedere la tuta. Lo usa il comando di debug. Dopo aver messo o tolto i
 * segnaposto ricalcola le dimensioni del giocatore ({@link SuitSize}): la grandezza reale cambia subito. La mappa dei
 * segnaposto per slot sta qui sola, e la vestizione vera ({@link FrameEntry#dress}) la usa.
 */
public final class DebugWear {
    /** Il segnaposto di ciascuno slot armatura vanilla. */
    static final Map<EquipmentSlot, RegistrySupplier<Item>> PLACEHOLDERS = Map.of(
            EquipmentSlot.HEAD, ModItems.EXOFRAME_HELMET,
            EquipmentSlot.CHEST, ModItems.EXOFRAME_TORSO,
            EquipmentSlot.LEGS, ModItems.EXOFRAME_LEGS,
            EquipmentSlot.FEET, ModItems.EXOFRAME_BOOTS);
    /** Gli slot armatura nell'ordine in cui si vestono. */
    static final List<EquipmentSlot> ARMOR_SLOTS =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private DebugWear() {
    }

    /**
     * Veste il giocatore con i quattro segnaposto e scrive {@code summary} sullo stack del torso
     * ({@link FrameEntry#dress}), poi ricalcola subito le dimensioni. Cio' che occupava gli slot armatura va
     * nell'inventario, o a terra ai piedi del giocatore se non entra ({@link Handouts#give}); i segnaposto gia'
     * indossati si scartano (si sostituiscono, non finiscono nell'inventario).
     */
    public static void wear(ServerPlayer player, SuitSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        FrameEntry.dress(player, summary);
        player.refreshDimensions(); // grandezza reale nello stesso tick, senza aspettare SuitSize.tick
    }

    /**
     * Toglie tutti i segnaposto dall'inventario del giocatore: slot armatura, inventario principale e mano
     * secondaria. Non rimette l'armatura di prima. Restituisce quanti item ha tolto.
     */
    public static int remove(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int removed = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isPlaceholder(stack)) {
                removed += stack.getCount();
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        player.refreshDimensions(); // grandezza vanilla nello stesso tick, senza aspettare SuitSize.tick
        return removed;
    }

    /** Vero se lo stack e' uno dei quattro segnaposto del telaio. */
    public static boolean isPlaceholder(ItemStack stack) {
        for (RegistrySupplier<Item> placeholder : PLACEHOLDERS.values()) {
            if (stack.is(placeholder.get())) {
                return true;
            }
        }
        return false;
    }
}
