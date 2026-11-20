package com.toxineren.exoframes.station;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.content.ModBlocks;
import com.toxineren.exoframes.content.ModMenus;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.station.StationReach;
import com.toxineren.exoframes.core.station.StationView;
import com.toxineren.exoframes.frame.FramePermissions;
import dev.architectury.registry.menu.MenuRegistry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
 * Il menu della stazione di manutenzione (design §5.2, §5.5, §5.6 e §5.13), del tipo {@link ModMenus#STATION}. Nasce
 * dal clic sulla stazione ({@link #open}) con i dati extra di Architectury ({@link StationOpening}), uguali sulle due
 * versioni, e ha sempre un telaio: quello che la ricerca ({@link StationFinder#find}) ha scelto all'apertura, fissato
 * per tutta la vita del menu ({@link #frameId}). Nel menu non vive nessun oggetto: non ha slot e non ridefinisce
 * {@code removed}.
 * <p>
 * Ha due costruttori: quello del server, con il livello della stazione, che calcola una volta l'impronta della
 * stazione; e quello del client, la fabbrica del tipo, che legge subito i dati extra e non tiene il buffer. Il gioco
 * chiama {@link #stillValid} solo sul server, a ogni tick del giocatore e prima di ogni pulsante: il menu resta aperto
 * finche' il giocatore puo' stare alla stazione e il telaio resta piazzato, a portata e usabile da lui. Il pulsante di
 * prova ({@link #TEST_BUTTON}) va al server con il pacchetto dei pulsanti del gioco base, e il numero delle pressioni
 * confermate ({@link #testPresses}) torna al client con la sincronizzazione dei dati del menu del gioco base: il cambio
 * che la schermata mostra viene dal server.
 * <p>
 * La vista del telaio per chi ha aperto il menu ({@link #view}, una {@link StationView}) arriva al client nei dati
 * extra e, quando cambia, con il pacchetto {@code station_view}: la manda {@link #broadcastChanges}, al piu' una volta
 * per tick, e sul client {@link StationNet#apply} la mette nel menu aperto con quel numero. La scelta del pezzo e'
 * quella dell'apertura: nella parte 4a-1 il pannello e' in sola lettura.
 */
public final class StationMenu extends AbstractContainerMenu {
    /**
     * Il pulsante di prova: il numero piu' alto della tabella di design §5.6 (dove andra' "salva" del preset 3). Su
     * 1.20.1 il pacchetto dei pulsanti scrive il numero in un byte, quindi ogni numero sta fra 0 e 127.
     */
    public static final int TEST_BUTTON = 102;

    /** La chiave del titolo della schermata. */
    public static final String TITLE_KEY = "container." + RedstoneExoframes.MOD_ID + ".station";

    /** Il messaggio sulla barra delle azioni quando il menu si chiude perche' il suo telaio non e' piu' disponibile. */
    public static final String FRAME_GONE = "message." + RedstoneExoframes.MOD_ID + ".station.frame_gone";

    /** Il tick dell'ultimo invio della vista prima del primo: nessuno, quindi il primo puo' partire subito. */
    private static final int NEVER = Integer.MIN_VALUE;

    /** Chi ha aperto il menu: il giocatore per cui si fa la vista. */
    private final Player viewer;
    private final BlockPos stationPos;
    /** Il telaio su cui il menu si e' aperto. */
    private final UUID frameId;
    /** Il livello della stazione sul server; null nel menu del client, che non lo conosce. */
    private final ServerLevel level;
    private final ContainerLevelAccess access;
    /** L'impronta della stazione, calcolata una volta all'apertura sul server; null nel menu del client. */
    private final StationReach.Footprint footprint;
    /** Le pressioni del pulsante di prova confermate dal server, che il gioco base sincronizza con il client. */
    private final DataSlot testPresses = DataSlot.standalone();
    /** La scelta del pezzo, quella dell'apertura: nella 4a-1 non cambia (i pulsanti 0-7 sono della 4a-2). */
    private final int selection;
    /** La vista del telaio che la schermata mostra: all'apertura quella dei dati extra. */
    private StationView view;
    /** L'ultima vista mandata al client: all'apertura quella dei dati extra. */
    private StationView lastSentView;
    /** Quante viste il menu ha mandato con il pacchetto {@code station_view} dall'apertura. */
    private int viewsSent;
    /** Il tick del server dell'ultimo invio della vista; {@link #NEVER} prima del primo. */
    private int lastSentTick = NEVER;
    /** Vero dopo la prima eccezione di {@link #stillValid}, scritta nel log una volta sola per menu. */
    private boolean checkFailureLogged;
    /** Vero dopo la prima eccezione della vista in {@link #broadcastChanges}, scritta nel log una volta per menu. */
    private boolean viewFailureLogged;
    /** Vero dopo che il telaio ha smesso di essere disponibile: il messaggio e' stato detto e il menu non vale piu'. */
    private boolean frameGone;

    /**
     * Il menu del client, la fabbrica del tipo: legge subito l'apertura dai dati extra e non tiene il buffer (su Fabric
     * 1.21.1 si rilascia appena il costruttore torna).
     */
    public StationMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, null, ContainerLevelAccess.NULL, null, StationOpening.read(extraData));
    }

    /**
     * Il menu del server, per la stazione e il telaio dell'apertura nel livello dato, aperto dal giocatore
     * dell'inventario. La meta' principale della stazione deve esserci ancora, come dopo la ricerca del clic, perche'
     * l'impronta si calcola qui dal suo stato.
     */
    public StationMenu(int containerId, Inventory inventory, ServerLevel level, StationOpening opening) {
        this(containerId, inventory, level, ContainerLevelAccess.create(level, opening.main()),
                StationFinder.footprint(opening.main(), level.getBlockState(opening.main())), opening);
    }

    private StationMenu(int containerId, Inventory inventory, ServerLevel level, ContainerLevelAccess access,
            StationReach.Footprint footprint, StationOpening opening) {
        super(ModMenus.STATION.get(), containerId);
        this.viewer = inventory.player;
        this.level = level;
        this.access = access;
        this.footprint = footprint;
        this.stationPos = opening.main();
        this.frameId = opening.frameId();
        this.selection = opening.view().selection();
        this.view = opening.view();
        this.lastSentView = opening.view();
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

    /**
     * L'apertura della stazione con la meta' principale in {@code main} sul telaio piazzato {@code frame}, quello che
     * la ricerca ha scelto per il giocatore, con la vista del telaio per lui e la scelta iniziale
     * ({@link StationView#initialSelection}: il primo pezzo montato, altrimenti l'elmo).
     */
    public static StationOpening opening(ServerPlayer player, BlockPos main, PlacedFrame frame) {
        return new StationOpening(main, frame.frameId(),
                viewFor(player, frame, StationView.initialSelection(frame.state())));
    }

    /**
     * La vista del telaio piazzato per il giocatore, con la scelta data: se puo' cambiarne il blocco
     * ({@link FramePermissions#canToggleLock}), la regola {@code server.ownerOnlyFrames} della config del server, se il
     * telaio custodisce un'armatura e se il giocatore non paga (la capacita' {@code instabuild}).
     */
    private static StationView viewFor(ServerPlayer player, PlacedFrame frame, int selection) {
        FrameState state = frame.state();
        return StationView.of(state, FramePermissions.canToggleLock(player, state),
                RedstoneExoframes.config().server.ownerOnlyFrames, selection, frame.hasStowed(),
                player.getAbilities().instabuild);
    }

    /** La posizione della meta' principale della stazione. */
    public BlockPos stationPos() {
        return stationPos;
    }

    /** L'identita' del telaio su cui il menu si e' aperto. */
    public UUID frameId() {
        return frameId;
    }

    /** La vista del telaio che la schermata mostra: quella dell'apertura, poi l'ultima arrivata dal server. */
    public StationView view() {
        return view;
    }

    /**
     * Mette la vista arrivata dal server con il pacchetto {@code station_view} ({@link StationNet#apply}); la schermata
     * la legge a ogni fotogramma.
     */
    public void setView(StationView view) {
        this.view = Objects.requireNonNull(view, "view must not be null");
    }

    /**
     * L'ultima vista che questo menu ha mandato al client: all'apertura quella dei dati extra. La tiene il server; per
     * i test e la diagnosi.
     */
    public StationView lastSentView() {
        return lastSentView;
    }

    /**
     * Quante viste questo menu ha mandato con il pacchetto {@code station_view} dall'apertura (0 all'apertura: la prima
     * viaggia nei dati extra), cioe' quante volte ha deciso di mandarla e ha chiamato
     * {@code Compat.net().sendToPlayer}. Su NeoForge 1.21.1 {@code Net} salta in silenzio un giocatore che non ha
     * negoziato il canale, come il giocatore finto dei test: il menu conta lo stesso quell'invio, cosi' il conto dice
     * la stessa cosa sulle quattro varianti. Per i test e la diagnosi.
     */
    public int viewsSent() {
        return viewsSent;
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
     * Il gioco lo chiama solo sul server, a ogni tick del giocatore e dopo ogni pulsante accettato. Prima il gioco base
     * manda al client cio' che e' cambiato nei dati del menu; poi, sul server, la vista del telaio (design §5.5 e
     * §5.13): rifatta dal record per chi ha aperto il menu, con la scelta dell'apertura, va al client con il pacchetto
     * {@code station_view} se e' diversa dall'ultima mandata (l'uguaglianza dei record) e se in questo tick non se n'e'
     * mandata un'altra; altrimenti aspetta il tick dopo. Cosi' ogni cambio del telaio, da qualunque parte venga (un
     * altro giocatore, un comando, un danno), arriva al client entro un tick, senza che nessuno lo annunci, e al piu'
     * una volta per tick per menu. Senza record non manda nulla: il telaio non c'e' piu' e {@link #stillValid} chiude
     * il menu. Se il contatore dei tick del server torna indietro, il limite riparte. Un'eccezione scrive una riga
     * ERROR alla prima volta per menu, e il menu prosegue.
     */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (level == null) {
            return;
        }
        try {
            sendViewIfChanged();
        } catch (RuntimeException e) {
            if (!viewFailureLogged) {
                viewFailureLogged = true;
                RedstoneExoframes.LOGGER.error("Station view update failed for player {}",
                        viewer.getGameProfile().getName(), e);
            }
        }
    }

    /** La vista nuova, mandata se e' cambiata e se in questo tick non se n'e' mandata un'altra (broadcastChanges). */
    private void sendViewIfChanged() {
        MinecraftServer server = level.getServer();
        int now = server.getTickCount();
        if (now < lastSentTick) {
            lastSentTick = NEVER; // il contatore dei tick e' tornato indietro: il limite riparte
        }
        if (now == lastSentTick || !(viewer instanceof ServerPlayer player)) {
            return;
        }
        Optional<PlacedFrame> frame = Compat.frameState().placed(server, frameId);
        if (frame.isEmpty()) {
            return;
        }
        StationView current = viewFor(player, frame.get(), selection);
        if (current.equals(lastSentView)) {
            return;
        }
        Compat.net().sendToPlayer(player, StationViewPacket.ID, new StationViewPacket(containerId, current));
        lastSentView = current;
        view = current;
        lastSentTick = now;
        viewsSent++;
    }

    /**
     * Vero finche' il menu puo' restare aperto. Il gioco lo chiama solo sul server, e nel menu del client (senza
     * livello) e' sempre vero. Sul server, in quest'ordine:
     * <ol>
     * <li>il giocatore e' nel livello della stazione (l'aiuto del gioco base non confronta le dimensioni), la stazione
     * c'e' ancora ed e' alla distanza del gioco base, il giocatore e' vivo e non e' spettatore; se no il menu non vale,
     * senza messaggi (chi si allontana non ha bisogno di spiegazioni, come per un forziere);</li>
     * <li>il record del telaio c'e' ancora (quindi e' piazzato e non indossato), e' a portata dell'impronta della
     * stazione ({@link StationFinder#inReach}) e il giocatore lo puo' usare ({@link FramePermissions#canUse}, riletto
     * a ogni chiamata). Se no, la prima volta il server lo dice sulla barra delle azioni ({@link #FRAME_GONE}), e da
     * li' il menu non vale piu', anche se il telaio tornasse.</li>
     * </ol>
     * Un'eccezione scrive una riga ERROR alla prima volta per menu e da' falso: la schermata si chiude.
     */
    @Override
    public boolean stillValid(Player player) {
        if (level == null) {
            return true;
        }
        try {
            if (player.level() != level
                    || !AbstractContainerMenu.stillValid(access, player, ModBlocks.STATION.get())
                    || !player.isAlive() || player.isSpectator()) {
                return false;
            }
            if (!frameGone && frameAvailableTo(player)) {
                return true;
            }
            if (!frameGone) {
                frameGone = true;
                player.displayClientMessage(Component.translatable(FRAME_GONE), true);
            }
            return false;
        } catch (RuntimeException e) {
            if (!checkFailureLogged) {
                checkFailureLogged = true;
                RedstoneExoframes.LOGGER.error("Station menu check failed for player {}",
                        player.getGameProfile().getName(), e);
            }
            return false;
        }
    }

    /** Vero se il telaio del menu e' ancora piazzato, a portata della stazione e usabile dal giocatore. */
    private boolean frameAvailableTo(Player player) {
        Optional<PlacedFrame> frame = Compat.frameState().placed(level.getServer(), frameId);
        return frame.isPresent() && StationFinder.inReach(frame.get(), level.dimension(), footprint)
                && player instanceof ServerPlayer serverPlayer
                && FramePermissions.canUse(serverPlayer, frame.get().state());
    }
}
