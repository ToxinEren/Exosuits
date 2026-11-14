package com.toxineren.exoframes.frame;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Consegna al giocatore di cio' che gli torna da un telaio (pezzi, celle, armatura stivata, il telaio stesso): mai
 * perso, in nessuna modalita' di gioco.
 * <p>
 * Cio' che la mod lascia a terra ({@link #dropStill}) ha il ritardo di raccolta di un oggetto lanciato, cosi' nessuno
 * lo raccoglie, o lo cancella, nel tick in cui arriva; che poi un giocatore in creativa con l'inventario del tutto
 * pieno, passandoci sopra, lo cancelli e' la regola vanilla della creativa, e resta.
 */
public final class Handouts {
    /** Il ritardo di raccolta, in tick, che {@code Player.drop} da' a un oggetto lanciato (40 su 1.20.1 e 1.21.1). */
    public static final int PICKUP_DELAY = 40;

    private Handouts() {
    }

    /**
     * Mette lo stack nell'inventario del giocatore, prima sulle pile uguali che hanno spazio e poi negli slot liberi
     * (quello in mano per ultimo: la mano vuota resta vuota finche' si puo', cosi' il clic dopo smonta ancora invece di
     * rimontare cio' che e' appena tornato), e cio' che non entra lo lascia a terra ai piedi del giocatore, fermo (non
     * lanciato). Lo stack conserva i suoi dati; quello passato non si tocca. Uno stack vuoto non fa nulla.
     * <p>
     * Non usa {@code Inventory.add}: in creativa cancella cio' che non entra.
     */
    public static void give(ServerPlayer player, ItemStack stack) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(stack, "stack must not be null");
        if (stack.isEmpty()) {
            return;
        }
        ItemStack rest = stack.copy();
        Inventory inventory = player.getInventory();
        while (!rest.isEmpty() && fillOneSlot(inventory, rest)) {
            // ogni giro sposta almeno un oggetto in uno slot
        }
        inventory.setChanged();
        if (!rest.isEmpty()) {
            dropStill(player.level(), player.position(), rest);
        }
    }

    /**
     * Lascia lo stack a terra nel punto dato: un oggetto fermo (non lanciato), con il ritardo di raccolta
     * {@link #PICKUP_DELAY}. Lo stack passa all'oggetto a terra cosi' com'e', dati compresi. Uno stack vuoto non fa
     * nulla e da' null.
     *
     * @return l'oggetto a terra aggiunto al livello, o null per uno stack vuoto
     */
    public static ItemEntity dropStill(Level level, Vec3 spot, ItemStack stack) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(spot, "spot must not be null");
        Objects.requireNonNull(stack, "stack must not be null");
        if (stack.isEmpty()) {
            return null;
        }
        ItemEntity dropped = new ItemEntity(level, spot.x, spot.y, spot.z, stack, 0.0, 0.0, 0.0);
        dropped.setPickUpDelay(PICKUP_DELAY);
        level.addFreshEntity(dropped);
        return dropped;
    }

    /**
     * Sposta parte di {@code rest} in uno slot: una pila uguale con spazio, altrimenti uno slot libero
     * dell'inventario principale ({@link #freeSlot}). Falso se non c'e' posto.
     */
    private static boolean fillOneSlot(Inventory inventory, ItemStack rest) {
        int partial = inventory.getSlotWithRemainingSpace(rest);
        if (partial >= 0) {
            ItemStack there = inventory.getItem(partial);
            int room = Math.min(there.getMaxStackSize(), inventory.getMaxStackSize()) - there.getCount();
            int moved = Math.min(room, rest.getCount());
            if (moved > 0) {
                there.grow(moved);
                rest.shrink(moved);
                return true;
            }
        }
        int free = freeSlot(inventory);
        if (free < 0) {
            return false;
        }
        inventory.setItem(free, rest.split(Math.min(rest.getMaxStackSize(), inventory.getMaxStackSize())));
        return true;
    }

    /** Il primo slot vuoto dell'inventario principale che non e' quello in mano; quello in mano se e' l'unico; -1. */
    private static int freeSlot(Inventory inventory) {
        for (int i = 0; i < inventory.items.size(); i++) {
            if (i != inventory.selected && inventory.items.get(i).isEmpty()) {
                return i;
            }
        }
        return inventory.getFreeSlot();
    }
}
