package com.toxineren.exoframes.station;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.content.ModBlocks;
import com.toxineren.exoframes.content.ModMenus;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.station.StationAction;
import com.toxineren.exoframes.core.station.StationReach;
import com.toxineren.exoframes.core.station.StationView;
import com.toxineren.exoframes.core.station.TickGate;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Il menu della stazione di manutenzione (design §5.2, §5.5, §5.6 e §5.13), del tipo {@link ModMenus#STATION}. Nasce
 * dal clic sulla stazione ({@link #open}) con i dati extra di Architectury ({@link StationOpening}), uguali sulle due
 * versioni, e ha sempre un telaio: quello che la ricerca ({@link StationFinder#find}) ha scelto all'apertura, fissato
 * per tutta la vita del menu ({@link #frameId}). Nel menu non vive nessun oggetto: ha solo gli slot dell'inventario
 * del giocatore, le tre file e la barra, perche' la schermata li mostri e il cursore li usi come nel gioco base, e non
 * ridefinisce {@code removed}.
 * <p>
 * Ha due costruttori: quello del server, con il livello della stazione, che calcola una volta l'impronta della
 * stazione; e quello del client, la fabbrica del tipo, che legge subito i dati extra e non tiene il buffer. Il gioco
 * chiama {@link #stillValid} solo sul server, a ogni tick del giocatore e prima di ogni pulsante: il menu resta aperto
 * finche' il giocatore puo' stare alla stazione e il telaio resta piazzato, a portata e usabile da lui.
 * <p>
 * I pulsanti della schermata (la tabella di design §5.6, decodificata da {@link StationAction}) vanno al server con il
 * pacchetto dei pulsanti del gioco base, che prima controlla l'id del menu aperto, lo spettatore e {@link #stillValid};
 * sul server li esegue {@link StationActions#press}, con al piu' un'azione che scrive per tick per menu
 * ({@link #takeWriteTurn}). La scelta ({@link #selection}) la tiene il menu sul server e cambia solo con i pulsanti
 * 0-7; il client la predice come i menu del gioco base ({@link #select}), e se la vista del server dice altro vince il
 * server. Ogni altro cambio la schermata lo vede arrivare con la vista: nel menu non vive nessun oggetto, e la
 * stazione non mette mai nulla sul cursore.
 * <p>
 * La vista del telaio per chi ha aperto il menu ({@link #view}, una {@link StationView}) arriva al client nei dati
 * extra e, quando cambia, con il pacchetto {@code station_view}: la manda {@link #broadcastChanges}, al piu' una volta
 * per tick, e sul client {@link StationNet#apply} la mette nel menu aperto con quel numero.
 */
public final class StationMenu extends AbstractContainerMenu {
    /** La chiave del titolo della schermata. */
    public static final String TITLE_KEY = "container." + RedstoneExoframes.MOD_ID + ".station";

    /** Il messaggio sulla barra delle azioni quando il menu si chiude perche' il suo telaio non e' piu' disponibile. */
    public static final String FRAME_GONE = "message." + RedstoneExoframes.MOD_ID + ".station.frame_gone";

    /**
     * L'ascissa dell'oggetto del primo slot di ogni fila dell'inventario e della barra, relativa all'angolo della
     * schermata (design §5.3: le file stanno in 7..169, cornici comprese).
     */
    public static final int INVENTORY_X = 8;
    /** L'ordinata dell'oggetto della prima fila dell'inventario principale (le tre file stanno in 149..203). */
    public static final int INVENTORY_Y = 150;
    /** L'ordinata dell'oggetto della barra degli oggetti (la barra sta in 207..225). */
    public static final int HOTBAR_Y = 208;

    /** Il passo degli slot nella schermata, in pixel dell'interfaccia: uno slot del gioco base con la sua cornice. */
    private static final int SLOT_PITCH = 18;
    /** Gli slot di una fila dell'inventario e della barra. */
    private static final int ROW_SLOTS = 9;
    /** Le file dell'inventario principale, sopra la barra. */
    private static final int MAIN_ROWS = 3;
    /** Gli slot dell'inventario principale nel menu, i primi (0-26); dopo vengono i nove della barra (27-35). */
    private static final int MAIN_SLOTS = MAIN_ROWS * ROW_SLOTS;

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
    /**
     * La scelta: 0-5 lo slot nell'ordine di {@code PieceSlot}, 6 la cella, 7 tutti i pezzi. All'apertura quella dei
     * dati extra; poi cambia solo con i pulsanti 0-7 ({@link #select}).
     */
    private int selection;
    /** La vista del telaio che la schermata mostra: all'apertura quella dei dati extra. */
    private StationView view;
    /** L'ultima vista mandata al client: all'apertura quella dei dati extra. */
    private StationView lastSentView;
    /** Quante viste il menu ha mandato con il pacchetto {@code station_view} dall'apertura. */
    private int viewsSent;
    /** Al piu' un invio della vista per tick del server (design §5.13). */
    private final TickGate viewGate = new TickGate();
    /** Al piu' un'azione che scrive per tick del server (design §5.6): le altre dello stesso tick si ignorano. */
    private final TickGate writeGate = new TickGate();
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
        addPlayerSlots(inventory);
    }

    /**
     * Gli slot dell'inventario del giocatore, uguali nel menu del server e in quello del client: prima le tre file
     * dell'inventario principale (indici 9-35 dell'inventario, slot 0-26 del menu), poi la barra (indici 0-8, slot
     * 27-35), con gli angoli {@link #INVENTORY_X}, {@link #INVENTORY_Y} e {@link #HOTBAR_Y}.
     */
    private void addPlayerSlots(Inventory inventory) {
        for (int row = 0; row < MAIN_ROWS; row++) {
            for (int column = 0; column < ROW_SLOTS; column++) {
                addSlot(new Slot(inventory, ROW_SLOTS + row * ROW_SLOTS + column, INVENTORY_X + SLOT_PITCH * column,
                        INVENTORY_Y + SLOT_PITCH * row));
            }
        }
        for (int column = 0; column < ROW_SLOTS; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + SLOT_PITCH * column, HOTBAR_Y));
        }
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
     * la legge a ogni fotogramma. E' per il menu del client: sul server {@link #broadcastChanges} rifa' la vista dal
     * record e la sovrascrive al prossimo invio.
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

    /**
     * La scelta del menu: sul server quella con cui si rifa' la vista; sul client l'ultima predetta (la schermata
     * mostra quella della vista, {@link #view}).
     */
    public int selection() {
        return selection;
    }

    /**
     * Cambia la scelta: sul server la mette il pulsante 0-7 ({@link StationActions#press}), e la vista mandata dopo la
     * porta al client; sul client e' la predizione, come i menu del gioco base, e cambia anche la vista che la
     * schermata mostra, finche' non arriva quella del server.
     *
     * @throws IllegalArgumentException se la scelta non sta fra 0 e {@link StationView#SELECT_ALL}
     */
    public void select(int selection) {
        if (selection < 0 || selection > StationView.SELECT_ALL) {
            throw new IllegalArgumentException("selection must be in [0, " + StationView.SELECT_ALL + "]: "
                    + selection);
        }
        this.selection = selection;
        if (level == null) {
            view = view.withSelection(selection);
        }
    }

    /**
     * Il turno di scrittura del tick dato: vero, e il turno e' preso, se in quel tick il menu non ha ancora fatto
     * un'azione che scrive; falso altrimenti. Un contatore dei tick che torna indietro fa ripartire il limite.
     */
    boolean takeWriteTurn(int tick) {
        return writeGate.tryUse(tick);
    }

    /**
     * Un pulsante della schermata. Sul server lo esegue {@link StationActions#press}, nell'ordine dei controlli di
     * design §5.6, e da' vero se la scelta e' cambiata o il telaio e' cambiato (il gioco base manda allora al client
     * cio' che e' cambiato, il cursore compreso); ogni altro esito da' falso. Sul client, dove la schermata lo chiama
     * prima di mandare il numero, come i menu del gioco base: vero per ogni numero della tabella
     * ({@link StationAction}), e per le tre scelte prima la predizione ({@link #select}).
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level == null) {
            Optional<StationAction> action = StationAction.decode(id);
            if (action.isEmpty()) {
                return false;
            }
            if (!action.get().writes()) {
                select(id);
            }
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        StationActions.Outcome outcome = StationActions.press(this, serverPlayer, id);
        return outcome == StationActions.Outcome.SELECTED || outcome == StationActions.Outcome.DONE;
    }

    /**
     * Lo spostamento rapido (il clic con le maiuscole) dello slot {@code index}: da uno slot dell'inventario
     * principale (0-26) alla barra (27-35), dalla barra all'inventario principale, nel primo posto che lo accoglie.
     * Restituisce la copia dello stack di prima se qualcosa si e' spostato, altrimenti {@link ItemStack#EMPTY}: il
     * gioco base ripete la chiamata finche' riceve uno stack non vuoto e lo slot tiene ancora lo stesso oggetto, quindi
     * uno stack non vuoto senza spostamento non finirebbe mai. Nulla esce dal menu: nessuno spostamento rapido monta
     * pezzi (design §5.13).
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        boolean moved = index < MAIN_SLOTS
                ? moveItemStackTo(stack, MAIN_SLOTS, slots.size(), false)
                : moveItemStackTo(stack, 0, MAIN_SLOTS, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return before;
    }

    /**
     * Il gioco lo chiama solo sul server, a ogni tick del giocatore e dopo ogni pulsante accettato. Prima il gioco base
     * manda al client cio' che e' cambiato nei dati del menu; poi, sul server, la vista del telaio (design §5.5 e
     * §5.13): rifatta dal record per chi ha aperto il menu, con la scelta del menu, va al client con il pacchetto
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
        if (!viewGate.open(now) || !(viewer instanceof ServerPlayer player)) {
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
        viewGate.use(now);
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
