package com.toxineren.exoframes.station;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import net.minecraft.world.entity.player.Player;

/**
 * Il canale S2C {@code redstone_exoframes:station_view} (design M4 §5.5 e §5.13): la vista del telaio piazzato per
 * chi lo guarda dalla stazione ({@link StationViewPacket}), che {@link StationMenu#broadcastChanges} manda quando
 * cambia, al piu' una volta per tick per menu. Il gestore e' codice comune: sul client mette la vista nel menu della
 * stazione aperto con quel numero, e la schermata la legge a ogni fotogramma. Su NeoForge 1.21.1 un client senza il
 * canale si salta ({@code Net}).
 */
public final class StationNet {
    private StationNet() {
    }

    /**
     * Registra il canale nell'init comune, sui due lati (il gestore gira solo sul client). Una volta, da
     * {@link RedstoneExoframes#init()}.
     */
    public static void register() {
        Compat.net().registerS2C(StationViewPacket.ID, StationViewPacket.class, StationViewPacket.CODEC,
                (msg, ctx) -> apply(ctx.player().get(), msg));
    }

    /**
     * Mette la vista del pacchetto nel menu aperto del giocatore, se e' un menu della stazione con il numero del
     * pacchetto; altrimenti non fa nulla: un pacchetto arrivato dopo la chiusura, o per un menu che non e' piu' quello
     * aperto, si ignora, e cosi' un giocatore che non c'e' ({@code null}).
     */
    public static void apply(Player player, StationViewPacket msg) {
        if (player != null && player.containerMenu instanceof StationMenu menu
                && menu.containerId == msg.containerId()) {
            menu.setView(msg.view());
        }
    }
}
