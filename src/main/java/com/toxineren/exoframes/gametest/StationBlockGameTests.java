package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.content.ModBlocks;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.content.StationBlock;
import com.toxineren.exoframes.content.StationPart;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco del blocco della stazione di manutenzione (design §5.1): due meta' affiancate, la principale e la
 * laterale, che si posano insieme e spariscono insieme, con al piu' una stazione a terra per stazione. Le posizioni
 * sono relative alla struttura del test: la principale sta in {@link #MAIN} se il test non dice altro, e il giocatore,
 * in sopravvivenza se il test non dice altro, due blocchi a sud ({@link #STAND}), rivolto a nord. La posa passa dal
 * clic vero del gioco ({@code clickBlock}); le stazioni gia' posate le mette {@code placeStation}, con le due meta'.
 * Si rompe come rompe un giocatore, con {@code ServerPlayerGameMode.destroyBlock} (senza il pacchetto), che chiama
 * {@code playerWillDestroy} del blocco e i controlli dell'attrezzo del gioco base. Le stazioni cadute si contano nel
 * riquadro della struttura ({@code stationsOnTheGround}) e gli oggetti a terra si tolgono subito dopo; una stazione
 * rimasta si toglie dalla principale, perche' la laterale rimasta sola diventa aria senza bottino. Gli aiuti comuni
 * stanno in {@link GameTestKit}.
 */
public final class StationBlockGameTests {
    /** Dove sta la meta' principale della stazione dei test, relativa alla struttura. */
    private static final BlockPos MAIN = new BlockPos(3, 1, 3);

    /** Dove stanno i piedi del giocatore, relativo alla struttura: due blocchi a sud di {@link #MAIN}. */
    private static final Vec3 STAND = new Vec3(3.5, 1.0, 5.5);

    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public StationBlockGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "placingTheStationSetsBothHalves", StationBlockGameTests::placingTheStationSetsBothHalves);
        add(tests, "placementNeedsRoomForTheSideHalf", StationBlockGameTests::placementNeedsRoomForTheSideHalf);
        add(tests, "pickaxeOnTheSideHalfDropsOneStation",
                StationBlockGameTests::pickaxeOnTheSideHalfDropsOneStation);
        add(tests, "pickaxeOnTheMainHalfDropsOneStation",
                StationBlockGameTests::pickaxeOnTheMainHalfDropsOneStation);
        add(tests, "noPickaxeDropsNothing", StationBlockGameTests::noPickaxeDropsNothing);
        add(tests, "creativeBreakingDropsNothing", StationBlockGameTests::creativeBreakingDropsNothing);
        add(tests, "onlyTheMainHalfHasLoot", StationBlockGameTests::onlyTheMainHalfHasLoot);
        add(tests, "pistonsCannotMoveTheStation", StationBlockGameTests::pistonsCannotMoveTheStation);
        add(tests, "removingOneHalfByCommandTakesTheOther",
                StationBlockGameTests::removingOneHalfByCommandTakesTheOther);
        add(tests, "adjacentStationsStayIndependent", StationBlockGameTests::adjacentStationsStayIndependent);
        return tests;
    }

    /**
     * L'oggetto posa le due meta' con il clic vero del gioco, sulla faccia di sopra di una pietra: la principale sopra
     * la pietra, il fronte verso chi posa e la laterale alla destra di chi posa; ogni stazione posata costa un oggetto.
     * Rivolto a nord: la principale in (3, 1, 3) e la laterale in (4, 1, 3), con il fronte a sud. Rivolto a est: la
     * principale in (3, 1, 6) e la laterale in (3, 1, 7), con il fronte a ovest.
     */
    private static void placingTheStationSetsBothHalves(GameTestHelper helper) {
        BlockPos firstFloor = new BlockPos(3, 0, 3);
        BlockPos firstSide = new BlockPos(4, 1, 3);
        BlockPos secondFloor = new BlockPos(3, 0, 6);
        BlockPos secondMain = new BlockPos(3, 1, 6);
        BlockPos secondSide = new BlockPos(3, 1, 7);
        helper.setBlock(firstFloor, Blocks.STONE);
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, STAND);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STATION.get(), 2));
        InteractionResult first = clickBlock(helper, p, firstFloor, InteractionHand.MAIN_HAND);
        helper.assertTrue(first.consumesAction(), "the click facing north reported " + first);
        assertStationAt(helper, MAIN, firstSide, Direction.SOUTH, "placed facing north");
        assertHolding(helper, p, 1, "after the first station");

        Vec3 secondStand = helper.absoluteVec(new Vec3(1.5, 1.0, 6.5));
        p.moveTo(secondStand.x, secondStand.y, secondStand.z, -90f, 0f);
        helper.setBlock(secondFloor, Blocks.STONE);
        InteractionResult second = clickBlock(helper, p, secondFloor, InteractionHand.MAIN_HAND);
        helper.assertTrue(second.consumesAction(), "the click facing east reported " + second);
        assertStationAt(helper, secondMain, secondSide, Direction.WEST, "placed facing east");
        helper.assertTrue(p.getMainHandItem().isEmpty(), "the hand still holds " + p.getMainHandItem()
                + " after the second station");

        removeStation(helper, MAIN, firstSide);
        removeStation(helper, secondMain, secondSide);
        helper.setBlock(firstFloor, Blocks.AIR);
        helper.setBlock(secondFloor, Blocks.AIR);
        helper.succeed();
    }

    /**
     * La stazione si posa solo se anche la laterale ha posto. Rivolto a nord, la laterale andrebbe in (4, 1, 3): con
     * una pietra li', con un secondo giocatore in piedi li' e con quel punto fuori dal bordo del mondo (stretto attorno
     * ai blocchi da 0 a 3 della struttura: la principale, x 3, dentro, la laterale, x 4, fuori) il clic non posa nulla
     * e l'oggetto resta in mano. Il bordo torna com'era nello stesso tick, anche se il test fallisce.
     */
    private static void placementNeedsRoomForTheSideHalf(GameTestHelper helper) {
        BlockPos floor = new BlockPos(3, 0, 3);
        BlockPos side = new BlockPos(4, 1, 3);
        helper.setBlock(floor, Blocks.STONE);
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, STAND);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STATION.get()));

        helper.setBlock(side, Blocks.STONE);
        assertNothingPlaced(helper, p, floor, "a stone where the side half goes");
        helper.setBlock(side, Blocks.AIR);

        ServerPlayer other = Compat.gameTests().mockServerPlayer(helper);
        other.setGameMode(GameType.SURVIVAL);
        Vec3 inTheWay = helper.absoluteVec(new Vec3(4.5, 1.0, 3.5));
        other.moveTo(inTheWay.x, inTheWay.y, inTheWay.z, 0f, 0f);
        assertNothingPlaced(helper, p, floor, "a player standing where the side half goes");
        Vec3 away = helper.absoluteVec(new Vec3(7.5, 1.0, 7.5));
        other.moveTo(away.x, away.y, away.z, 0f, 0f);

        WorldBorder border = helper.getLevel().getWorldBorder();
        double centerX = border.getCenterX();
        double centerZ = border.getCenterZ();
        double size = border.getSize();
        try {
            // il bordo copre i blocchi da 0 a 3 della struttura: la principale (x 3) dentro, la laterale (x 4) fuori
            Vec3 corner = helper.absoluteVec(new Vec3(0.0, 0.0, 0.0));
            border.setCenter(corner.x + 2.0, corner.z + 2.0);
            border.setSize(4.0);
            helper.assertTrue(border.isWithinBounds(helper.absolutePos(MAIN))
                    && !border.isWithinBounds(helper.absolutePos(side)), "test border not where expected");
            assertNothingPlaced(helper, p, floor, "the side half outside the world border");
        } finally {
            border.setCenter(centerX, centerZ);
            border.setSize(size);
        }
        helper.setBlock(floor, Blocks.AIR);
        helper.succeed();
    }

    /**
     * Con un piccone di legno, in sopravvivenza, rompere la meta' laterale toglie anche la principale e lascia a terra
     * una stazione, una sola: la principale diventa aria per {@code updateShape} e fa cadere il suo bottino.
     */
    private static void pickaxeOnTheSideHalfDropsOneStation(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer p = playerWithAPickaxe(helper);
        breakBlock(helper, p, side, "the side half with a pickaxe");
        assertAir(helper, "the side half broken with a pickaxe", MAIN, side);
        assertStationsDropped(helper, 1, "the side half broken with a pickaxe");
        helper.succeed();
    }

    /**
     * Con un piccone di legno, in sopravvivenza, rompere la meta' principale toglie anche la laterale e lascia a terra
     * una stazione, una sola: quella della principale; la laterale diventa aria senza bottino.
     */
    private static void pickaxeOnTheMainHalfDropsOneStation(GameTestHelper helper) {
        BlockPos side = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer p = playerWithAPickaxe(helper);
        breakBlock(helper, p, MAIN, "the main half with a pickaxe");
        assertAir(helper, "the main half broken with a pickaxe", MAIN, side);
        assertStationsDropped(helper, 1, "the main half broken with a pickaxe");
        helper.succeed();
    }

    /**
     * A mano vuota, in sopravvivenza, una stazione rotta dalla laterale e una rotta dalla principale spariscono tutte
     * e due senza lasciare nulla: la mano vuota non e' l'attrezzo giusto, e dalla laterale la principale si toglie
     * prima, senza bottino.
     */
    private static void noPickaxeDropsNothing(GameTestHelper helper) {
        BlockPos firstSide = placeStation(helper, MAIN, Direction.NORTH);
        BlockPos secondMain = new BlockPos(3, 1, 6);
        BlockPos secondSide = placeStation(helper, secondMain, Direction.NORTH);
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, STAND);
        helper.assertTrue(p.getMainHandItem().isEmpty(), "premise: the new player holds " + p.getMainHandItem());
        helper.assertTrue(!p.hasCorrectToolForDrops(helper.getBlockState(MAIN)),
                "premise: an empty hand is a correct tool for the station");
        breakBlock(helper, p, firstSide, "the side half with an empty hand");
        breakBlock(helper, p, secondMain, "the main half with an empty hand");
        assertAir(helper, "the stations broken with an empty hand", MAIN, firstSide, secondMain, secondSide);
        assertStationsDropped(helper, 0, "the stations broken with an empty hand");
        helper.succeed();
    }

    /**
     * In creativa, con un piccone in mano, una stazione rotta dalla laterale e una rotta dalla principale spariscono
     * tutte e due senza lasciare nulla, come ogni blocco rotto in creativa.
     */
    private static void creativeBreakingDropsNothing(GameTestHelper helper) {
        BlockPos firstSide = placeStation(helper, MAIN, Direction.NORTH);
        BlockPos secondMain = new BlockPos(3, 1, 6);
        BlockPos secondSide = placeStation(helper, secondMain, Direction.NORTH);
        ServerPlayer p = Compat.gameTests().mockServerPlayer(helper);
        p.setGameMode(GameType.CREATIVE);
        standFacingNorth(helper, p, STAND);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
        helper.assertTrue(p.getAbilities().instabuild, "premise: the creative player cannot build instantly");
        breakBlock(helper, p, firstSide, "the side half in creative");
        breakBlock(helper, p, secondMain, "the main half in creative");
        assertAir(helper, "the stations broken in creative", MAIN, firstSide, secondMain, secondSide);
        assertStationsDropped(helper, 0, "the stations broken in creative");
        helper.succeed();
    }

    /**
     * Il bottino della stazione sta tutto nella meta' principale: la sua tabella da' una stazione, quella della
     * laterale nulla; e le due meta' vogliono il piccone (tag {@code mineable/pickaxe}, non {@code mineable/axe}). Per
     * questo un'esplosione, che toglie le due meta' facendo cadere il bottino di ciascuna, ne fa cadere al piu' una.
     * L'esplosione vera non si fa qui: ha un esito casuale e arriva alle strutture vicine (i test condividono il
     * server); la guarda la checklist della parte.
     */
    private static void onlyTheMainHalfHasLoot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(MAIN);
        BlockState main = ModBlocks.STATION.get().defaultBlockState();
        BlockState side = main.setValue(StationBlock.PART, StationPart.SIDE);
        List<ItemStack> mainDrops = Block.getDrops(main, level, pos, null);
        helper.assertTrue(mainDrops.size() == 1 && mainDrops.get(0).is(ModItems.STATION.get())
                && mainDrops.get(0).getCount() == 1, "the main half drops " + mainDrops + ", not one station");
        List<ItemStack> sideDrops = Block.getDrops(side, level, pos, null);
        helper.assertTrue(sideDrops.isEmpty(), "the side half drops " + sideDrops);
        for (BlockState half : List.of(main, side)) {
            helper.assertTrue(half.is(BlockTags.MINEABLE_WITH_PICKAXE) && !half.is(BlockTags.MINEABLE_WITH_AXE),
                    half + " is not mined with a pickaxe only");
        }
        helper.succeed();
    }

    /**
     * I pistoni non spostano la stazione ({@code PushReaction.BLOCK}): la principale in (4, 1, 3) e la laterale in
     * (3, 1, 3); un pistone in (2, 1, 3) che spinge la laterale verso est e uno in (4, 1, 2) che spinge la principale
     * verso sud, ciascuno alimentato da un blocco di redstone dietro di se'. Dopo 4 tick nessuno dei due si e' esteso e
     * le due meta' sono al loro posto con i loro stati.
     */
    private static void pistonsCannotMoveTheStation(GameTestHelper helper) {
        BlockPos main = new BlockPos(4, 1, 3);
        BlockPos side = placeStation(helper, main, Direction.NORTH);
        BlockPos eastPiston = new BlockPos(2, 1, 3);
        BlockPos eastPower = new BlockPos(1, 1, 3);
        BlockPos southPiston = new BlockPos(4, 1, 2);
        BlockPos southPower = new BlockPos(4, 1, 1);
        helper.setBlock(eastPiston, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,
                Direction.EAST));
        helper.setBlock(southPiston, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,
                Direction.SOUTH));
        helper.setBlock(eastPower, Blocks.REDSTONE_BLOCK);
        helper.setBlock(southPower, Blocks.REDSTONE_BLOCK);
        ServerLevel level = helper.getLevel();
        helper.assertTrue(level.hasNeighborSignal(helper.absolutePos(eastPiston))
                && level.hasNeighborSignal(helper.absolutePos(southPiston)), "premise: the pistons are not powered");
        helper.runAfterDelay(4, () -> runInSequence(helper, h -> {
            try {
                for (BlockPos piston : List.of(eastPiston, southPiston)) {
                    BlockState state = h.getBlockState(piston);
                    h.assertTrue(state.is(Blocks.PISTON) && !state.getValue(PistonBaseBlock.EXTENDED),
                            "the piston at " + piston + " after 4 ticks: " + state);
                }
                assertStationAt(h, main, side, Direction.NORTH, "after 4 ticks of powered pistons");
            } finally {
                // prima la corrente, poi i pistoni: un pistone ancora alimentato davanti all'aria si estenderebbe
                h.setBlock(eastPower, Blocks.AIR);
                h.setBlock(southPower, Blocks.AIR);
                h.setBlock(eastPiston, Blocks.AIR);
                h.setBlock(southPiston, Blocks.AIR);
                removeStation(h, main, side);
            }
            h.succeed();
        }));
    }

    /**
     * Togliere una meta' con un comando (qui {@code helper.setBlock}, come {@code /setblock}) toglie anche l'altra per
     * {@code updateShape}, con il bottino della meta' che diventa aria: tolta la laterale, la principale cade come una
     * stazione; tolta la principale di una seconda stazione, la laterale sparisce senza nulla.
     */
    private static void removingOneHalfByCommandTakesTheOther(GameTestHelper helper) {
        BlockPos firstSide = placeStation(helper, MAIN, Direction.NORTH);
        helper.setBlock(firstSide, Blocks.AIR);
        assertAir(helper, "the side half set to air", MAIN, firstSide);
        assertStationsDropped(helper, 1, "the side half set to air");

        BlockPos secondMain = new BlockPos(3, 1, 6);
        BlockPos secondSide = placeStation(helper, secondMain, Direction.NORTH);
        helper.setBlock(secondMain, Blocks.AIR);
        assertAir(helper, "the main half set to air", secondMain, secondSide);
        assertStationsDropped(helper, 0, "the main half set to air");
        helper.succeed();
    }

    /**
     * Due stazioni accostate restano indipendenti, tutte e due rivolte a nord: A con la principale in (5, 1, 3) e la
     * laterale in (4, 1, 3), B con la principale in (3, 1, 3) e la laterale in (2, 1, 3), cosi' la laterale di A tocca
     * la principale di B. Rotta con il piccone la laterale di A, A sparisce con una stazione a terra e B resta intera
     * con i suoi stati; rotta poi la principale di B, anche B sparisce, e le stazioni a terra sono due in tutto.
     */
    private static void adjacentStationsStayIndependent(GameTestHelper helper) {
        BlockPos mainA = new BlockPos(5, 1, 3);
        BlockPos sideA = placeStation(helper, mainA, Direction.NORTH);
        BlockPos sideB = placeStation(helper, MAIN, Direction.NORTH);
        ServerPlayer p = playerWithAPickaxe(helper);
        breakBlock(helper, p, sideA, "the side half of A");
        assertAir(helper, "the side half of A broken", mainA, sideA);
        assertStationAt(helper, MAIN, sideB, Direction.NORTH, "B after A was broken");
        int afterA = stationsOnTheGround(helper);
        helper.assertTrue(afterA == 1, afterA + " stations on the ground after A was broken, expected 1");

        breakBlock(helper, p, MAIN, "the main half of B");
        assertAir(helper, "the main half of B broken", MAIN, sideB);
        assertStationsDropped(helper, 2, "A and B broken");
        helper.succeed();
    }

    /** Il giocatore in sopravvivenza in {@link #STAND}, rivolto a nord, con un piccone di legno in mano. */
    private static ServerPlayer playerWithAPickaxe(GameTestHelper helper) {
        ServerPlayer p = Compat.gameTests().survivalPlayer(helper);
        standFacingNorth(helper, p, STAND);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
        return p;
    }

    /** Il giocatore rompe il blocco nel punto relativo come lo rompe il gioco; la rottura deve riuscire. */
    private static void breakBlock(GameTestHelper helper, ServerPlayer player, BlockPos relative, String what) {
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(relative)), "could not break " + what);
    }

    /**
     * In {@code main} la meta' principale e in {@code side} la laterale di una stazione con il fronte verso
     * {@code facing}: gli stati esatti.
     */
    private static void assertStationAt(GameTestHelper helper, BlockPos main, BlockPos side, Direction facing,
            String when) {
        BlockState expectedMain = ModBlocks.STATION.get().defaultBlockState().setValue(StationBlock.FACING, facing);
        BlockState expectedSide = expectedMain.setValue(StationBlock.PART, StationPart.SIDE);
        BlockState atMain = helper.getBlockState(main);
        BlockState atSide = helper.getBlockState(side);
        helper.assertTrue(atMain.equals(expectedMain) && atSide.equals(expectedSide), when + ": " + main + " holds "
                + atMain + " and " + side + " holds " + atSide + ", expected " + expectedMain + " and "
                + expectedSide);
    }

    /** In ogni punto relativo dato c'e' aria. */
    private static void assertAir(GameTestHelper helper, String when, BlockPos... positions) {
        for (BlockPos pos : positions) {
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(state.isAir(), when + ": " + pos + " holds " + state);
        }
    }

    /** Il giocatore tiene nella mano principale {@code count} stazioni. */
    private static void assertHolding(GameTestHelper helper, ServerPlayer player, int count, String when) {
        ItemStack held = player.getMainHandItem();
        helper.assertTrue(held.is(ModItems.STATION.get()) && held.getCount() == count, when + ": the hand holds "
                + held + ", not " + count + " stations");
    }

    /**
     * Il clic con la stazione in mano sulla faccia di sopra della pietra in {@code floor} non posa nulla: il gioco non
     * lo conta come un'azione riuscita, sopra la pietra resta aria e la stazione resta in mano.
     */
    private static void assertNothingPlaced(GameTestHelper helper, ServerPlayer player, BlockPos floor,
            String why) {
        InteractionResult result = clickBlock(helper, player, floor, InteractionHand.MAIN_HAND);
        helper.assertTrue(!result.consumesAction(), why + ": the click reported " + result);
        assertAir(helper, why, floor.above());
        assertHolding(helper, player, 1, why);
    }

    /**
     * Le stazioni a terra nella struttura ({@code stationsOnTheGround}) sono {@code expected}; prima di controllarlo
     * toglie dal mondo tutti gli oggetti a terra della struttura.
     */
    private static void assertStationsDropped(GameTestHelper helper, int expected, String when) {
        int stations = stationsOnTheGround(helper);
        List<ItemEntity> items = itemsIn(helper);
        List<ItemStack> dropped = items.stream().map(ItemEntity::getItem).toList();
        items.forEach(ItemEntity::discard);
        helper.assertTrue(stations == expected, when + ": " + stations + " stations on the ground, expected "
                + expected + " (dropped " + dropped + ")");
    }

    /**
     * Toglie le due meta' di una stazione: prima la principale, cosi' la laterale rimasta sola diventa aria senza
     * bottino; poi la laterale, se c'e' ancora.
     */
    private static void removeStation(GameTestHelper helper, BlockPos main, BlockPos side) {
        helper.setBlock(main, Blocks.AIR);
        helper.setBlock(side, Blocks.AIR);
    }
}
