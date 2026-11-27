package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.compat.ItemDataAccess.FrameItemData;
import com.toxineren.exoframes.compat.ItemDataAccess.PieceData;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.content.ExoframeItem;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.FrameInteractions;
import com.toxineren.exoframes.frame.FramePermissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco dell'oggetto telaio raccolto e posato di nuovo (design M4 §5.11, la raccolta e la posa, e §5.14):
 * l'oggetto che torna da un telaio vuoto raccolto a mano porta lo sblocco, il rinforzo e, se a raccoglierlo non e' il
 * proprietario, il proprietario ({@link ExoframeItem#pickedUp}); la posa li rilegge, e chi posa un telaio libero ne
 * diventa il proprietario; un telaio senza nulla da portare torna un oggetto senza dati, come uno fabbricato; un
 * oggetto con pezzi o una cella nei dati non si posa e resta in mano; il suggerimento dell'oggetto mostra "Sbloccato"
 * e "Rinforzato", mai il proprietario.
 * <p>
 * I giocatori A e B sono giocatori finti di {@code mockServerPlayer} in sopravvivenza, in piedi nella struttura e
 * rivolti a nord: il livello di permesso 0 li tiene fuori dalla regola degli operatori di
 * {@link FramePermissions#canUse}, anche se {@code isCreative()} dice sempre vero, e in sopravvivenza l'oggetto posato
 * si consuma. Il telaio sta sulla pietra dei test ({@link #STONE}), messo da {@code placeFrame} o posato con l'oggetto
 * in mano ({@code useExoframeItem}); raccoglierlo e' il clic accovacciati a mano vuota sul telaio vuoto, come per le
 * mani ({@link FrameInteractions}). Ogni test toglie, anche se fallisce, i telai della sua struttura con i loro record,
 * gli oggetti a terra e gli inventari dei giocatori.
 */
public final class FrameItemGameTests {
    /** Il blocco di pietra dei test, relativo alla struttura: il telaio ci sta sopra, con i piedi in {@link #FEET}. */
    private static final BlockPos STONE = new BlockPos(3, 1, 3);

    /** I piedi del telaio sulla pietra, relativi alla struttura: dove lo mette {@code placeFrame} e dove lo si posa. */
    private static final Vec3 FEET = new Vec3(3.5, 2.0, 3.5);

    /** Dove stanno i piedi del giocatore A, relativo alla struttura: due blocchi a sud della pietra. */
    private static final Vec3 STAND_A = new Vec3(3.5, 1.0, 5.5);

    /** Dove stanno i piedi del giocatore B, relativo alla struttura: accanto ad A, a ovest. */
    private static final Vec3 STAND_B = new Vec3(2.5, 1.0, 5.5);

    /** La riga "Sbloccato" del suggerimento dell'oggetto. */
    private static final String UNLOCKED_LINE = "tooltip.redstone_exoframes.exoframe.unlocked";

    /** La riga "Rinforzato" del suggerimento dell'oggetto. */
    private static final String REINFORCED_LINE = "tooltip.redstone_exoframes.exoframe.reinforced";

    /** Le quattro righe grigie del suggerimento, sempre, nell'ordine: piazzarlo, montare, entrare, togliere le cose. */
    private static final List<String> CONTROL_LINES = List.of("tooltip.redstone_exoframes.exoframe.place",
            "tooltip.redstone_exoframes.exoframe.mount", "tooltip.redstone_exoframes.exoframe.enter",
            "tooltip.redstone_exoframes.exoframe.unmount");

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public FrameItemGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "ownerPickUpFreesTheFrameAndKeepsTheFlags",
                FrameItemGameTests::ownerPickUpFreesTheFrameAndKeepsTheFlags);
        add(tests, "borrowedFrameKeepsItsOwnerWhenPlacedAgain",
                FrameItemGameTests::borrowedFrameKeepsItsOwnerWhenPlacedAgain);
        add(tests, "plainPickUpGivesAPlainItem", FrameItemGameTests::plainPickUpGivesAPlainItem);
        add(tests, "frameItemWithPiecesOrACellIsNotPlaced", FrameItemGameTests::frameItemWithPiecesOrACellIsNotPlaced);
        add(tests, "frameTooltipShowsTheFlagsNeverTheOwner",
                FrameItemGameTests::frameTooltipShowsTheFlagsNeverTheOwner);
        return tests;
    }

    /**
     * Il telaio di A, sbloccato e rinforzato, raccolto da A: il record se ne va e nell'inventario di A c'e' un oggetto
     * telaio con le due bandierine e senza proprietario (ne' pezzi ne' cella); posato da B, il record nuovo e' di B,
     * sbloccato e rinforzato, e l'oggetto ha lasciato la mano di B.
     */
    private static void ownerPickUpFreesTheFrameAndKeepsTheFlags(GameTestHelper helper) {
        ServerPlayer a = survivor(helper, STAND_A);
        ServerPlayer b = survivor(helper, STAND_B);
        MinecraftServer server = helper.getLevel().getServer();
        helper.setBlock(STONE, Blocks.STONE);
        try {
            FrameState state = ownedBy(a);
            state.setUnlocked(true);
            state.setReinforced(true);
            ExoframeEntity frame = placeFrame(helper, state, FEET, 0f);
            helper.assertTrue(pickUp(frame, a) == InteractionResult.SUCCESS, "A could not pick up the frame");
            helper.assertTrue(Compat.frameState().placed(server, frame.frameId()).isEmpty() && frame.isRemoved(),
                    "the frame picked up by A is still placed");

            ItemStack item = takeFrameItem(helper, a);
            Optional<FrameItemData> data = Compat.itemData().frame(item);
            helper.assertTrue(data.equals(Optional.of(new FrameItemData(Optional.empty(), true, true, Optional.empty(),
                    Map.of()))), "the item picked up by its owner carries " + data);

            ExoframeEntity placed = useExoframeItem(helper, b, item, STONE, true);
            FrameState again = recordState(helper, placed);
            helper.assertTrue(b.getUUID().equals(again.owner()) && again.unlocked() && again.reinforced(),
                    "the frame placed by B: owner " + again.owner() + " (B " + b.getUUID() + "), unlocked "
                            + again.unlocked() + ", reinforced " + again.reinforced());
            helper.assertTrue(again.pieces().isEmpty() && again.cell() == null, "the frame placed by B is not empty");
            helper.assertTrue(b.getMainHandItem().isEmpty(), "the item placed by B is still in hand: "
                    + b.getMainHandItem());
        } finally {
            cleanUp(helper, a, b);
        }
        helper.succeed();
    }

    /**
     * Il telaio di A, sbloccato, prestato: B lo raccoglie e l'oggetto ha A come proprietario, sbloccato e non
     * rinforzato; B lo posa, e il record nuovo e' di A, sbloccato, e B lo puo' usare. Quando A lo blocca
     * ({@code updatePlaced}), B non lo raccoglie piu': il messaggio {@link FrameInteractions#LOCKED}, il telaio resta
     * piazzato e B non riceve nulla.
     */
    private static void borrowedFrameKeepsItsOwnerWhenPlacedAgain(GameTestHelper helper) {
        ServerPlayer a = survivor(helper, STAND_A);
        ServerPlayer b = survivor(helper, STAND_B);
        MinecraftServer server = helper.getLevel().getServer();
        helper.setBlock(STONE, Blocks.STONE);
        try {
            helper.assertTrue(RedstoneExoframes.config().server.ownerOnlyFrames, "ownerOnlyFrames is not the default");
            FrameState lent = ownedBy(a);
            lent.setUnlocked(true);
            ExoframeEntity frame = placeFrame(helper, lent, FEET, 0f);
            helper.assertTrue(pickUp(frame, b) == InteractionResult.SUCCESS, "B could not pick up A's unlocked frame");

            ItemStack item = takeFrameItem(helper, b);
            Optional<FrameItemData> data = Compat.itemData().frame(item);
            helper.assertTrue(data.equals(Optional.of(new FrameItemData(Optional.of(a.getUUID()), true, false,
                    Optional.empty(), Map.of()))), "the item picked up by B carries " + data + ", A is " + a.getUUID());

            ExoframeEntity placed = useExoframeItem(helper, b, item, STONE, true);
            FrameState again = recordState(helper, placed);
            helper.assertTrue(a.getUUID().equals(again.owner()) && again.unlocked() && !again.reinforced(),
                    "the frame placed again by B: owner " + again.owner() + " (A " + a.getUUID() + "), unlocked "
                            + again.unlocked() + ", reinforced " + again.reinforced());
            helper.assertTrue(FramePermissions.canUse(b, again), "B cannot use the frame it placed again");

            helper.assertTrue(Compat.frameState().updatePlaced(server, placed.frameId(), s -> s.setUnlocked(false)),
                    "A could not lock the frame");
            helper.assertTrue(pickUp(placed, b) == InteractionResult.CONSUME, "B's click on the locked frame passed");
            helper.assertTrue(messages(b, FrameInteractions.LOCKED) == 1, "locked messages to B: "
                    + messages(b, FrameInteractions.LOCKED));
            helper.assertTrue(Compat.frameState().placed(server, placed.frameId()).isPresent() && !placed.isRemoved(),
                    "B picked up the locked frame");
            helper.assertTrue(countInMainInventory(b, ModItems.EXOFRAME.get()) == 0, "B was given a frame item");
        } finally {
            cleanUp(helper, a, b);
        }
        helper.succeed();
    }

    /**
     * Il telaio di A, bloccato e non rinforzato, raccolto da A: nulla da portare, quindi un oggetto senza dati, uguale
     * a uno fabbricato; posato da B, il telaio e' di B, come uno fabbricato.
     */
    private static void plainPickUpGivesAPlainItem(GameTestHelper helper) {
        ServerPlayer a = survivor(helper, STAND_A);
        ServerPlayer b = survivor(helper, STAND_B);
        helper.setBlock(STONE, Blocks.STONE);
        try {
            ExoframeEntity frame = placeFrame(helper, ownedBy(a), FEET, 0f);
            helper.assertTrue(pickUp(frame, a) == InteractionResult.SUCCESS, "A could not pick up the frame");

            ItemStack item = takeFrameItem(helper, a);
            helper.assertTrue(!Compat.itemData().hasData(item), "the plain frame item has data: "
                    + Compat.itemData().frame(item));
            helper.assertTrue(ItemStack.matches(item, new ItemStack(ModItems.EXOFRAME.get())),
                    "the plain frame item differs from a crafted one: " + item);

            ExoframeEntity placed = useExoframeItem(helper, b, item, STONE, true);
            FrameState again = recordState(helper, placed);
            helper.assertTrue(b.getUUID().equals(again.owner()) && !again.unlocked() && !again.reinforced(),
                    "the plain frame placed by B: owner " + again.owner() + " (B " + b.getUUID() + "), unlocked "
                            + again.unlocked() + ", reinforced " + again.reinforced());
        } finally {
            cleanUp(helper, a, b);
        }
        helper.succeed();
    }

    /**
     * Un oggetto telaio con un elmo nei dati (fatto con un comando: il gioco non lo scrive), usato da A sulla pietra,
     * dove c'e' spazio: non si posa, A ha il messaggio {@link ExoframeItem#FULL} (e non quello dello spazio), l'oggetto
     * resta in mano uguale e i telai piazzati non cambiano. Lo stesso con una cella nei dati e nessun pezzo.
     */
    private static void frameItemWithPiecesOrACellIsNotPlaced(GameTestHelper helper) {
        ServerPlayer a = survivor(helper, STAND_A);
        helper.setBlock(STONE, Blocks.STONE);
        try {
            List<UUID> before = placedIds(helper.getLevel());
            PieceData helmet = new PieceData(Tier.SCRAP, 180, 180, List.of(), Appearance.DEFAULT);
            ItemStack withPiece = frameItem(new FrameItemData(Optional.empty(), false, false, Optional.empty(),
                    Map.of(PieceSlot.HELMET, helmet)));
            assertNotPlaced(helper, a, withPiece, 1, before, "with a helmet in its data");

            ItemStack withCell = frameItem(new FrameItemData(Optional.empty(), false, false, Optional.of(500),
                    Map.of()));
            assertNotPlaced(helper, a, withCell, 2, before, "with a cell in its data");
            helper.assertTrue(messages(a, ExoframeItem.NO_ROOM) == 0, "A was told there is no room");
        } finally {
            cleanUp(helper, a);
        }
        helper.succeed();
    }

    /**
     * Il suggerimento dell'oggetto: con i dati del telaio di A, sbloccato e rinforzato, le righe "Sbloccato" e
     * "Rinforzato" in oro, prima delle quattro righe grigie; solo rinforzato, la sola "Rinforzato"; senza dati,
     * nessuna delle due. Le quattro righe grigie ci sono sempre, e nessuna riga dice il nome o l'identita' di A.
     */
    private static void frameTooltipShowsTheFlagsNeverTheOwner(GameTestHelper helper) {
        ServerPlayer a = survivor(helper, STAND_A);
        UUID picker = UUID.randomUUID();
        FrameState both = ownedBy(a);
        both.setUnlocked(true);
        both.setReinforced(true);
        ItemStack lent = ExoframeItem.pickedUp(both, picker);
        helper.assertTrue(ownerInData(lent).equals(Optional.of(a.getUUID())), "the item of the unlocked and"
                + " reinforced frame does not carry A as its owner: " + Compat.itemData().frame(lent));
        assertTooltip(helper, a, lent, List.of(UNLOCKED_LINE, REINFORCED_LINE), "unlocked and reinforced");

        FrameState reinforced = ownedBy(a);
        reinforced.setReinforced(true);
        ItemStack shielded = ExoframeItem.pickedUp(reinforced, picker);
        helper.assertTrue(ownerInData(shielded).equals(Optional.of(a.getUUID())), "the item of the reinforced frame"
                + " does not carry A as its owner: " + Compat.itemData().frame(shielded));
        assertTooltip(helper, a, shielded, List.of(REINFORCED_LINE), "only reinforced");

        assertTooltip(helper, a, new ItemStack(ModItems.EXOFRAME.get()), List.of(), "without data");
        helper.succeed();
    }

    /** Un giocatore finto in sopravvivenza, in piedi nel punto relativo alla struttura, rivolto a nord. */
    private static ServerPlayer survivor(GameTestHelper helper, Vec3 feet) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        standFacingNorth(helper, player, feet);
        return player;
    }

    /** Raccogliere il telaio: accovacciati, a mano vuota, il clic sul telaio ({@code click}, a un metro dai piedi). */
    private static InteractionResult pickUp(ExoframeEntity frame, ServerPlayer player) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        return click(frame, player, 0.0, 1.0, 0.0);
    }

    /**
     * L'oggetto telaio raccolto, tolto dall'inventario principale del giocatore (come lo prende un altro giocatore):
     * deve essercene uno solo.
     */
    private static ItemStack takeFrameItem(GameTestHelper helper, ServerPlayer player) {
        int count = countInMainInventory(player, ModItems.EXOFRAME.get());
        helper.assertTrue(count == 1, count + " frame items in the inventory after the pick up, expected 1");
        return findInMainInventory(player, ModItems.EXOFRAME.get()).split(1);
    }

    /** Un oggetto telaio con i dati dati, come lo farebbe un comando. */
    private static ItemStack frameItem(FrameItemData data) {
        ItemStack stack = new ItemStack(ModItems.EXOFRAME.get());
        Compat.itemData().setFrame(stack, data);
        return stack;
    }

    /** Il proprietario nei dati dell'oggetto telaio; vuoto senza dati o senza proprietario. */
    private static Optional<UUID> ownerInData(ItemStack stack) {
        return Compat.itemData().frame(stack).flatMap(FrameItemData::owner);
    }

    /**
     * L'oggetto telaio, usato dal giocatore sulla pietra dei test, non si posa: il messaggio {@link ExoframeItem#FULL}
     * arriva (in tutto {@code fullMessages} volte), l'oggetto resta in mano con lo stesso numero e gli stessi dati, e
     * le identita' dei telai piazzati restano {@code before}.
     */
    private static void assertNotPlaced(GameTestHelper helper, ServerPlayer player, ItemStack stack, int fullMessages,
            List<UUID> before, String what) {
        ItemStack expected = stack.copy();
        useExoframeItem(helper, player, stack, STONE, false);
        helper.assertTrue(messages(player, ExoframeItem.FULL) == fullMessages, "the frame item " + what + ": "
                + messages(player, ExoframeItem.FULL) + " full messages, expected " + fullMessages);
        helper.assertTrue(ItemStack.matches(expected, player.getMainHandItem()), "the frame item " + what
                + " did not stay in hand as it was: " + player.getMainHandItem());
        helper.assertTrue(placedIds(helper.getLevel()).equals(before), "the frame item " + what
                + " changed the placed frames");
    }

    /**
     * Il suggerimento dell'oggetto ha le righe delle bandierine attese, in oro, e poi le quattro righe grigie; nessuna
     * riga contiene il nome o l'identita' del proprietario.
     */
    private static void assertTooltip(GameTestHelper helper, ServerPlayer owner, ItemStack stack, List<String> flags,
            String what) {
        List<Component> lines = new ArrayList<>();
        ModItems.EXOFRAME.get().tooltip(stack, lines, false);
        List<String> keys = new ArrayList<>();
        for (Component line : lines) {
            keys.add(line.getContents() instanceof TranslatableContents contents ? contents.getKey() : "?");
        }
        List<String> expected = new ArrayList<>(flags);
        expected.addAll(CONTROL_LINES);
        helper.assertTrue(keys.equals(expected), what + ": tooltip " + keys + ", expected " + expected);
        TextColor gold = TextColor.fromLegacyFormat(ChatFormatting.GOLD);
        TextColor gray = TextColor.fromLegacyFormat(ChatFormatting.GRAY);
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            TextColor colour = i < flags.size() ? gold : gray;
            helper.assertTrue(colour.equals(line.getStyle().getColor()), what + ": tooltip line " + keys.get(i)
                    + " has the colour " + line.getStyle().getColor() + ", expected " + colour);
            String text = line.getString();
            helper.assertTrue(!text.contains(owner.getScoreboardName()) && !text.contains(owner.getUUID().toString()),
                    what + ": the tooltip line \"" + text + "\" names the owner");
        }
    }

    /**
     * La pulizia di ogni test, anche se fallisce: i telai piazzati nella struttura, record ed entita' (uno raccolto non
     * c'e' piu'), gli oggetti a terra e gli inventari dei giocatori.
     */
    private static void cleanUp(GameTestHelper helper, ServerPlayer... players) {
        ServerLevel level = helper.getLevel();
        FrameStateAccess frames = Compat.frameState();
        AABB box = structureBox(helper);
        for (PlacedFrame placed : frames.placedIn(level)) {
            if (box.contains(placed.position())) {
                frames.removePlaced(level.getServer(), placed.frameId());
            }
        }
        for (ExoframeEntity frame : level.getEntitiesOfClass(ExoframeEntity.class, box)) {
            frame.discard();
        }
        for (ItemEntity drop : itemsIn(helper)) {
            drop.discard();
        }
        for (ServerPlayer player : players) {
            player.getInventory().clearContent();
        }
    }
}
