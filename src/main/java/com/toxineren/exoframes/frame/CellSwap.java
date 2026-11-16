package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.core.energy.CellSwapRule;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Il cambio cella dall'interno della tuta, sul server (design §12.10): il client tiene premuto il tasto e manda
 * {@link CellSwapPacket}; qui si decide tutto dal registro e dall'inventario del giocatore.
 * <p>
 * La cella scelta e' la piu' carica fra quelle che contengono piu' energia dell'installata ({@link CellSwapRule}):
 * deroga dichiarata a design §5.6, che diceva "la prima cella carica".
 * <p>
 * L'ordine e' atomico (anti-obiettivo "nulla si perde, nulla si duplica"): prima il registro, con
 * {@link FrameStateAccess#changeWorn} (vecchia cella fuori, nuova dentro), poi l'inventario, e solo se la scrittura e'
 * riuscita. Mai {@code update}: crea il record se manca, e un pacchetto arrivato dopo un'uscita o una morte
 * creerebbe un telaio dal nulla; {@code changeWorn} tocca solo un record indossato che esiste.
 * <p>
 * Il server non puo' sapere se il tasto e' stato tenuto davvero: per questo tiene un intervallo minimo fra due cambi
 * dello stesso giocatore, meta' dell'attesa del client ({@code cellSwapSeconds * 10} tick), cosi' un server lento non
 * rifiuta un giocatore onesto. L'ultimo cambio sta in una mappa per UUID che non si svuota mai: sopravvive all'uscita
 * dal telaio e dal server.
 */
public final class CellSwap {
    /** Messaggio sulla barra delle azioni quando nessuna cella dell'inventario ha piu' energia dell'installata. */
    public static final String NO_BETTER_CELL = "message." + RedstoneExoframes.MOD_ID + ".no_better_cell";

    /** Il tick del server dell'ultimo cambio riuscito, per giocatore. Mai svuotata. */
    private static final Map<UUID, Long> LAST_SWAP = new ConcurrentHashMap<>();

    private CellSwap() {
    }

    /**
     * Registra il pacchetto C2S del cambio cella: il gestore inoltra a {@link #handle} con il tick del server e basta.
     * Una volta, nell'init comune, su entrambi i lati.
     */
    public static void registerNet() {
        Compat.net().registerC2S(CellSwapPacket.ID, CellSwapPacket.class, CellSwapPacket.CODEC,
                (packet, player) -> handle(player, player.server.getTickCount()));
    }

    /**
     * Il cambio cella chiesto dal giocatore al tick {@code now} del server. In ordine:
     * <ol>
     * <li>nulla, senza dire nulla, se il giocatore non indossa un telaio nel registro, non e' vivo, e' spettatore o ha
     * un menu aperto;</li>
     * <li>nulla, in silenzio, se l'ultimo cambio e' di meno di {@code cellSwapSeconds * 10} tick fa (un {@code now}
     * minore dell'ultimo cambio vuol dire un contatore ripartito: si accetta);</li>
     * <li>la cella da prendere con {@link CellSwapRule#pick} (cariche tagliate alla capacita' della config); se non ce
     * n'e' una, il messaggio {@link #NO_BETTER_CELL} e basta;</li>
     * <li>il registro: vecchia cella fuori, nuova dentro, con {@link FrameStateAccess#changeWorn}; se rifiuta
     * l'inventario non si tocca (ramo di difesa: con il registro in sola lettura nessun telaio e' indossato in questa
     * sessione, quindi il cambio non fa nulla gia' al primo punto);</li>
     * <li>solo dopo, l'inventario: la cella presa lascia il suo slot (anche in creativa) e la vecchia, se c'era, va
     * nello stesso slot se ora e' vuoto, altrimenti a {@link Handouts#give};</li>
     * <li>il suono del cambio e il tick nella mappa dell'intervallo.</li>
     * </ol>
     * Pubblico per i test, che lo chiamano con tempi propri.
     */
    public static void handle(ServerPlayer player, long now) {
        FrameStateAccess frames = Compat.frameState();
        if (frames.wornFrameId(player).isEmpty() || !player.isAlive() || player.isSpectator()
                || player.containerMenu != player.inventoryMenu) {
            return;
        }
        UUID uuid = player.getUUID();
        Long last = LAST_SWAP.get(uuid);
        if (last != null && now >= last && now - last < RedstoneExoframes.config().energy.cellSwapSeconds * 10L) {
            return;
        }
        EnergyCell installedCell = frames.get(player).map(FrameState::cell).orElse(null);
        int installed = installedCell == null ? -1 : installedCell.stored();
        Inventory inventory = player.getInventory();
        int[] energies = new int[CellSwapRule.SLOTS];
        for (int i = 0; i < CellSwapRule.OFFHAND; i++) {
            energies[i] = energyOf(inventory.items.get(i));
        }
        energies[CellSwapRule.OFFHAND] = energyOf(inventory.offhand.get(0));
        int index = CellSwapRule.pick(installed, energies);
        if (index == -1) {
            FrameInteractions.tell(player, NO_BETTER_CELL);
            return;
        }
        int slotIndex = index == CellSwapRule.OFFHAND ? Inventory.SLOT_OFFHAND : index;
        ItemStack source = inventory.getItem(slotIndex);
        EnergyCell incoming = ((CellItem) source.getItem()).toCell(source);
        EnergyCell[] old = new EnergyCell[1];
        boolean changed = frames.changeWorn(player, s -> {
            old[0] = s.removeCell();
            s.insertCell(incoming);
        });
        if (!changed) {
            return;
        }
        source.split(1);
        if (old[0] != null) {
            ItemStack back = CellItem.fromCell(old[0]);
            if (inventory.getItem(slotIndex).isEmpty()) {
                inventory.setItem(slotIndex, back);
            } else {
                Handouts.give(player, back);
            }
        }
        inventory.setChanged();
        FrameSounds.play(player.serverLevel(), player.position(), FrameSounds.Sound.CELL_SWAP);
        LAST_SWAP.put(uuid, now);
    }

    /** La carica della cella dello stack, tagliata alla capacita' della config; {@code -1} se non e' una cella. */
    private static int energyOf(ItemStack stack) {
        return stack.getItem() instanceof CellItem cell ? cell.toCell(stack).stored() : -1;
    }
}
