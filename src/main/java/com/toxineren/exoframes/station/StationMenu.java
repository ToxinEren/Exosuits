package com.toxineren.exoframes.station;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.content.ModBlocks;
import com.toxineren.exoframes.content.ModMenus;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Il menu della stazione di manutenzione (design §5.2 e §5.6), del tipo {@link ModMenus#STATION}. Nasce dal clic sulla
 * stazione ({@link #open}) con i dati extra di Architectury ({@link StationOpening}), uguali sulle due versioni. Nel
 * menu non vive nessun oggetto: non ha slot e non ridefinisce {@code removed}.
 * <p>
 * Ha due costruttori: quello del server, con il livello della stazione, e quello del client, la fabbrica del tipo, che
 * legge subito i dati extra e non tiene il buffer. Il gioco chiama {@link #stillValid} solo sul server, a ogni tick del
 * giocatore e prima di ogni pulsante. Il pulsante di prova ({@link #TEST_BUTTON}) va al server con il pacchetto dei
 * pulsanti del gioco base, e il numero delle pressioni confermate ({@link #testPresses}) torna al client con la
 * sincronizzazione dei dati del menu del gioco base: il cambio che la schermata mostra viene dal server.
 */
public final class StationMenu extends AbstractContainerMenu {
    /**
     * Il pulsante di prova: il numero piu' alto della tabella di design §5.6 (dove andra' "salva" del preset 3). Su
     * 1.20.1 il pacchetto dei pulsanti scrive il numero in un byte, quindi ogni numero sta fra 0 e 127.
     */
    public static final int TEST_BUTTON = 102;

    /** La chiave del titolo della schermata. */
    public static final String TITLE_KEY = "container." + RedstoneExoframes.MOD_ID + ".station";

    private final BlockPos stationPos;
    /** Il livello della stazione sul server; null nel menu del client, che non lo conosce. */
    private final ServerLevel level;
    private final ContainerLevelAccess access;
    /** Le pressioni del pulsante di prova confermate dal server, che il gioco base sincronizza con il client. */
    private final DataSlot testPresses = DataSlot.standalone();
    /** Vero dopo la prima eccezione di {@link #stillValid}, scritta nel log una volta sola per menu. */
    private boolean checkFailureLogged;

    /**
     * Il menu del client, la fabbrica del tipo: legge subito l'apertura dai dati extra e non tiene il buffer (su Fabric
     * 1.21.1 si rilascia appena il costruttore torna).
     */
    public StationMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, null, ContainerLevelAccess.NULL, StationOpening.read(extraData));
    }

    /** Il menu del server, per la stazione dell'apertura nel livello dato. */
    public StationMenu(int containerId, Inventory inventory, ServerLevel level, StationOpening opening) {
        this(containerId, level, ContainerLevelAccess.create(level, opening.main()), opening);
    }

    private StationMenu(int containerId, ServerLevel level, ContainerLevelAccess access, StationOpening opening) {
        super(ModMenus.STATION.get(), containerId);
        this.level = level;
        this.access = access;
        this.stationPos = opening.main();
        addDataSlot(testPresses);
    }

    /** Chi apre la stazione: il titolo della schermata e il menu del server per l'apertura data. */
    public static MenuProvider provider(ServerLevel level, StationOpening opening) {
        return new SimpleMenuProvider((containerId, inventory, player) ->
                new StationMenu(containerId, inventory, level, opening), Component.translatable(TITLE_KEY));
    }

    /** Apre la stazione per il giocatore, con l'apertura nei dati extra di Architectury (uguali sulle due versioni). */
    public static void open(ServerPlayer player, StationOpening opening) {
        MenuRegistry.openExtendedMenu(player, provider(player.serverLevel(), opening), opening::write);
    }

    /** La posizione della meta' principale della stazione. */
    public BlockPos stationPos() {
        return stationPos;
    }

    /** Le pressioni del pulsante di prova confermate dal server (sul client, quelle ricevute dal server). */
    public int testPresses() {
        return testPresses.get();
    }

    /**
     * Un pulsante della schermata, sul server: il pulsante di prova, premuto da un giocatore vivo, aumenta di uno le
     * pressioni e da' vero (il gioco base manda allora al client cio' che e' cambiato); ogni altro numero, o un
     * giocatore morto, da' falso senza cambiare nulla e senza scrivere nel log.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != TEST_BUTTON || !player.isAlive()) {
            return false;
        }
        testPresses.set(testPresses.get() + 1);
        return true;
    }

    /** Nessuno slot: niente da spostare. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /**
     * Vero finche' il menu puo' restare aperto. Il gioco lo chiama solo sul server, e nel menu del client (senza
     * livello) e' sempre vero. Sul server: il giocatore e' nel livello della stazione (l'aiuto del gioco base non
     * confronta le dimensioni), la stazione c'e' ancora ed e' alla distanza del gioco base, il giocatore e' vivo e non
     * e' spettatore. Un'eccezione scrive una riga ERROR alla prima volta per menu e da' falso: la schermata si chiude.
     */
    @Override
    public boolean stillValid(Player player) {
        if (level == null) {
            return true;
        }
        try {
            return player.level() == level
                    && AbstractContainerMenu.stillValid(access, player, ModBlocks.STATION.get())
                    && player.isAlive() && !player.isSpectator();
        } catch (RuntimeException e) {
            if (!checkFailureLogged) {
                checkFailureLogged = true;
                RedstoneExoframes.LOGGER.error("Station menu check failed for player {}",
                        player.getGameProfile().getName(), e);
            }
            return false;
        }
    }
}
