package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.content.ModItems;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * I quattro segnaposto del telaio indossato, in un posto solo: quale va in quale slot armatura vanilla, come si
 * riconoscono, come si scrive uno slot armatura in silenzio e come si tolgono da un giocatore. Li usano la vestizione
 * e l'uscita ({@link FrameEntry}), l'autoriparazione degli slot ({@link SlotGuard}) e il comando di debug
 * ({@link DebugWear}). Un segnaposto non e' mai un oggetto vero: dove non deve stare si elimina, non si restituisce.
 * <p>
 * I posti del giocatore in cui un segnaposto puo' finire, e da cui {@link #removeAll} e {@link #removeStrays} lo
 * tolgono: gli slot armatura, l'inventario principale, la mano secondaria, lo stack sul cursore, la griglia 2x2 di
 * fabbricazione e gli slot del menu aperto che non sono l'inventario del giocatore (una cassa, un banco da lavoro).
 * Alla morte e alla rimozione del giocatore morto il gioco lascerebbe cadere a terra cio' che resta nelle griglie e
 * nel menu aperto ({@code AbstractContainerMenu.clearContainer}), percio' la rimozione li copre tutti.
 */
public final class Placeholders {
    /** Il segnaposto di ciascuno slot armatura vanilla. */
    static final Map<EquipmentSlot, RegistrySupplier<Item>> BY_SLOT = Map.of(
            EquipmentSlot.HEAD, ModItems.EXOFRAME_HELMET,
            EquipmentSlot.CHEST, ModItems.EXOFRAME_TORSO,
            EquipmentSlot.LEGS, ModItems.EXOFRAME_LEGS,
            EquipmentSlot.FEET, ModItems.EXOFRAME_BOOTS);
    /** Gli slot armatura nell'ordine in cui si vestono. */
    static final List<EquipmentSlot> ARMOR_SLOTS =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private Placeholders() {
    }

    /** Vero se lo stack e' uno dei quattro segnaposto del telaio. */
    public static boolean isPlaceholder(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        for (RegistrySupplier<Item> placeholder : BY_SLOT.values()) {
            if (item == placeholder.get()) {
                return true;
            }
        }
        return false;
    }

    /** Vero se lo stack e' il segnaposto dello slot, da solo (una pila di uno). */
    static boolean isPlaceholderOf(EquipmentSlot slot, ItemStack stack) {
        return stack.getCount() == 1 && stack.is(BY_SLOT.get(slot).get());
    }

    /** Un segnaposto nuovo per lo slot; quello del torso porta {@code summary}. */
    static ItemStack create(EquipmentSlot slot, SuitSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        ItemStack placeholder = new ItemStack(BY_SLOT.get(slot).get());
        if (slot == EquipmentSlot.CHEST) {
            Compat.itemData().setSuitSummary(placeholder, summary);
        }
        return placeholder;
    }

    /**
     * Scrive uno slot armatura in silenzio: direttamente nella lista {@code armor} dell'inventario, non con
     * {@code setItemSlot}, che chiama {@code LivingEntity.onEquipItem} (suono dell'equipaggiamento ed evento di gioco
     * {@code EQUIP}, che sveglia i sensori sculk) su 1.20.1 e 1.21.1. Il resto segue da solo come per ogni cambio
     * dell'inventario: al tick dopo {@code LivingEntity.detectEquipmentUpdates} confronta gli slot con l'ultimo
     * valore visto, aggiorna i modificatori degli attributi e manda l'equipaggiamento a chi vede il giocatore, e il
     * menu dell'inventario manda lo slot al giocatore stesso.
     */
    static void setArmorSlot(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        player.getInventory().armor.set(slot.getIndex(), stack);
        player.getInventory().setChanged();
    }

    /**
     * Toglie ogni segnaposto dal giocatore, in silenzio, da tutti i posti elencati sopra, slot armatura compresi. Uno
     * stack vero resta dov'e'. Non ricalcola le dimensioni: lo fa chi chiama.
     *
     * @return quanti segnaposto ha tolto
     */
    public static int removeAll(ServerPlayer player) {
        return remove(player, true);
    }

    /**
     * Come {@link #removeAll}, ma lascia gli slot armatura come sono: le copie dei segnaposto finite altrove.
     *
     * @return quanti segnaposto ha tolto
     */
    static int removeStrays(ServerPlayer player) {
        return remove(player, false);
    }

    private static int remove(ServerPlayer player, boolean armorToo) {
        Objects.requireNonNull(player, "player must not be null");
        Inventory inventory = player.getInventory();
        int inInventory = clear(inventory.items) + clear(inventory.offhand) + (armorToo ? clear(inventory.armor) : 0);
        if (inInventory > 0) {
            inventory.setChanged();
        }
        int removed = inInventory + clearMenu(inventory, player.inventoryMenu);
        if (player.containerMenu != player.inventoryMenu) {
            removed += clearMenu(inventory, player.containerMenu);
        }
        return removed;
    }

    private static int clear(NonNullList<ItemStack> stacks) {
        int removed = 0;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (isPlaceholder(stack)) {
                removed += stack.getCount();
                stacks.set(i, ItemStack.EMPTY);
            }
        }
        return removed;
    }

    /**
     * Il cursore del menu e i suoi slot che non sono dell'inventario del giocatore (quelli li copre gia'
     * {@link #remove}, e gli slot armatura del menu dell'inventario non vanno toccati da {@link #removeStrays}).
     */
    private static int clearMenu(Inventory inventory, AbstractContainerMenu menu) {
        int removed = 0;
        ItemStack carried = menu.getCarried();
        if (isPlaceholder(carried)) {
            removed += carried.getCount();
            menu.setCarried(ItemStack.EMPTY);
        }
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (slot.container != inventory && isPlaceholder(stack)) {
                removed += stack.getCount();
                slot.set(ItemStack.EMPTY);
            }
        }
        return removed;
    }
}
