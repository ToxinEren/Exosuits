package com.toxineren.exoframes.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.compat.StowedStack;
import com.toxineren.exoframes.content.CellItem;
import com.toxineren.exoframes.content.ModBlocks;
import com.toxineren.exoframes.content.ModEntities;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.content.PieceItem;
import com.toxineren.exoframes.content.StationBlock;
import com.toxineren.exoframes.content.StationPart;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.DebugWear;
import com.toxineren.exoframes.frame.FrameEntry;
import com.toxineren.exoframes.frame.FramePlacement;
import com.toxineren.exoframes.frame.Placeholders;
import com.toxineren.exoframes.frame.SafeSpots;
import com.toxineren.exoframes.frame.SuitSize;
import com.toxineren.exoframes.station.StationFinder;
import com.toxineren.exoframes.station.StationMenu;
import dev.architectury.event.events.common.TickEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Aiuti e costanti comuni ai test in gioco (GameTest vanilla), uguali sulle quattro varianti, senza differenze di
 * versione o loader: quelle stanno dietro {@link Compat#gameTests()}. Le classi dei test li importano con
 * {@code import static com.toxineren.exoframes.gametest.GameTestKit.*;}. I test si lanciano con
 * {@code ./gradlew :<variante>:runGameTestServer} o con {@code bash tools/run_gametests.sh}.
 *
 * <p>Per aggiungere un test. I test stanno in una classe per area, in {@code gametest/}: {@code ExoframeGameTests} i
 * test della parte 3b, {@code CarryOverGameTests} i riporti della 3c, {@code EnergyGameTests},
 * {@code DamageGameTests}, {@code MovementGameTests} e {@code CellGameTests} la parte 3c-1, {@code ChargerGameTests},
 * {@code DataGameTests} e {@code SurvivalGameTests} la parte 3c-2, {@code StationMenuGameTests} il menu della stazione
 * e {@code StationBlockGameTests} il suo blocco di due meta' (parte 4a-1), {@code StationActionGameTests} i suoi
 * pulsanti (parte 4a-2). Ogni classe ha un costruttore pubblico senza
 * argomenti, un metodo {@link GameTestGenerator}, {@code import static GameTestKit.*} e una riga
 * {@code Compat.gameTests().register(Classe.class)} in {@code RedstoneExoframes.init()}. Un test e' un metodo
 * {@code private static void temaCaso(GameTestHelper helper)} che chiude con {@code helper.succeed()}, piu' una riga
 * {@code add(tests, "temaCaso", Classe::temaCaso);} nel generatore della sua classe ({@link #add}); i nomi sono unici
 * fra tutte le classi. tools/run_gametests.sh conta quelle righe in tutti i file di {@code gametest/} per sapere
 * quanti test aspettarsi; la lista e' la stessa sulle quattro varianti (il corpo di un test puo' dipendere dalla
 * versione, la lista no).
 *
 * <p>Tutti i test girano sullo stesso server, con lo stesso dato salvato e lo stesso livello, e cominciano insieme,
 * ciascuno appena i chunk della sua struttura hanno le entita' caricate (di solito nello stesso tick; vedi
 * {@link #startWhenEntitiesLoaded}). Percio' un test:
 * <ul>
 * <li>controlla solo i suoi giocatori finti e i suoi {@code frameId}, mai conteggi globali (quanti telai, quanti
 * giocatori, quante voci orfane in tutto);</li>
 * <li>sposta i suoi giocatori finti dentro la sua struttura prima di cercare qualcosa intorno a loro;</li>
 * <li>prima di {@code succeed()} toglie quello che ha creato: i record piazzati e indossati, le entita', gli oggetti a
 * terra, le voci orfane ({@code GameTests.forgetOrphaned});</li>
 * <li>rimette com'erano le regole del gioco e il bordo del mondo che cambia, anche se fallisce ({@code finally});</li>
 * <li>non dipende dal contatore dei tick del giocatore finto: il livello lo fa avanzare e chiama
 * {@code ServerPlayer.tick()} a ogni tick del server (ne scendono {@code invulnerableTime} e la protezione
 * d'ingresso), ma non chiama mai {@code doTick()}, quindi niente {@code Player.tick()}, niente fisica e niente
 * {@code TickEvent.PLAYER_PRE}/{@code PLAYER_POST}: chi conta sul contatore lo imposta, e i tick della mod
 * ({@code SlotGuard.tick}, {@code SafeSpots}, {@code SuitTick}) si chiamano a mano;</li>
 * <li>sa che il giocatore finto dice sempre di essere in creativa ({@code isCreative()}), ma che le sue capacita'
 * ({@code instabuild}, {@code invulnerable}) seguono la modalita' di gioco del mondo dei test, che su 1.20.1-forge non
 * e' la creativa: i test che vogliono la sopravvivenza usano {@code Compat.gameTests().survivalPlayer(helper)}, quelli
 * che vogliono la creativa chiamano {@code setGameMode(GameType.CREATIVE)}, e nessuno conta sulla modalita' di
 * partenza;</li>
 * <li>non lascia avvisi nel log oltre a quelli che prova di proposito.</li>
 * </ul>
 *
 * <p>I test nascono da un {@link GameTestGenerator} e non dall'annotazione {@code @GameTest}, perche' i loader
 * risolvono in modo diverso il nome della struttura dell'annotazione; il costruttore a 7 argomenti di
 * {@link TestFunction} e' identico su 1.20.1 e 1.21.1. Nome del test e struttura portano entrambi il namespace della
 * mod: Forge 1.20.1 filtra i test generati sul namespace del nome, NeoForge su quello della struttura.
 */
final class GameTestKit {
    /** Struttura vuota 8x8x8 della mod: data/redstone_exoframes/structures/empty.nbt (1.20.1) e structure/ (1.21). */
    static final String EMPTY_STRUCTURE = RedstoneExoframes.MOD_ID + ":empty";
    /** Lato della struttura vuota, in blocchi. */
    static final int STRUCTURE_SIZE = 8;
    static final int MAX_TICKS = 100;
    /** Quanti tick al massimo un test aspetta le entita' dei chunk della sua struttura prima di partire. */
    static final int ENTITY_LOAD_TICKS = 20;
    /** Versione dei dati di Minecraft 1.20.1. */
    static final int DATA_VERSION_1_20_1 = 3465;
    static final List<EquipmentSlot> ARMOR_SLOTS =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    /** Lato della griglia del banco da lavoro, in caselle. */
    static final int CRAFTING_GRID_SIDE = 3;
    /** L'id del menu della stazione aperto da {@link #openStation}. */
    static final int STATION_MENU_ID = 100;

    /** Solo aiuti statici: nessuna istanza. */
    private GameTestKit() {
    }

    /** Quante volte il giocatore finto ha avuto il messaggio del telaio lasciato altrove ({@code ELSEWHERE}). */
    static int elsewhereMessages(ServerPlayer player) {
        return messages(player, FrameEntry.ELSEWHERE);
    }

    /** Quante volte il giocatore finto ha avuto il messaggio con la chiave di traduzione data. */
    static int messages(ServerPlayer player, String key) {
        int count = 0;
        for (Component message : Compat.gameTests().messagesTo(player)) {
            if (message.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Usa l'oggetto telaio (uno, in mano) sulla faccia est del blocco dato, relativo alla struttura; l'unico telaio
     * piazzato nella colonna del blocco accanto, o null se l'uso e' stato rifiutato.
     */
    static ExoframeEntity useExoframeItemOnSide(GameTestHelper helper, ServerPlayer player,
            BlockPos relative) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EXOFRAME.get()));
        BlockPos clicked = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked).add(0.5, 0.0, 0.0), Direction.EAST,
                clicked, false);
        InteractionResult result = ModItems.EXOFRAME.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                hit));
        if (!result.consumesAction()) {
            return null;
        }
        BlockPos column = clicked.east();
        List<ExoframeEntity> found = helper.getLevel().getEntitiesOfClass(ExoframeEntity.class,
                new AABB(column.getX(), column.getY() - 8, column.getZ(), column.getX() + 1, column.getY() + 1,
                        column.getZ() + 1));
        helper.assertTrue(found.size() == 1, found.size() + " frames below " + column + ", expected 1");
        return found.get(0);
    }

    /** Cancella la cartella temporanea di un test con tutto quello che contiene; nulla se e' null. */
    static void deleteTree(Path folder) {
        if (folder == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("could not delete " + folder, e);
        }
    }

    /** Il segnaposto dello slot, come lo darebbe {@code /give}: senza riassunto. */
    static ItemStack placeholderFor(EquipmentSlot slot) {
        return new ItemStack(switch (slot) {
            case HEAD -> ModItems.EXOFRAME_HELMET.get();
            case CHEST -> ModItems.EXOFRAME_TORSO.get();
            case LEGS -> ModItems.EXOFRAME_LEGS.get();
            default -> ModItems.EXOFRAME_BOOTS.get();
        });
    }

    /**
     * Mette il giocatore dentro un telaio con lo stato dato, in piedi su un blocco di pietra posato sotto il punto
     * relativo alla struttura, con un clic come un giocatore vero ({@link FrameEntry#enter}); da' l'identita' del
     * telaio.
     */
    static UUID wearFrameAt(GameTestHelper helper, ServerPlayer player, FrameState state, Vec3 relativeFeet) {
        helper.setBlock(BlockPos.containing(relativeFeet).below(), Blocks.STONE);
        ExoframeEntity frame = placeFrame(helper, state, relativeFeet, 0f);
        helper.assertTrue(FrameEntry.enter(player, frame), "enter refused");
        return frame.frameId();
    }

    /**
     * Il giocatore, dentro il telaio, cammina fino al punto relativo alla struttura (un blocco di pietra posato sotto,
     * diverso da quello dove e' entrato, che e' gia' la sua posizione sicura) e ci sta a terra; poi fa un tick del
     * giocatore passando dall'evento del gioco ({@code TickEvent.PLAYER_POST}, quello a cui {@link SafeSpots} e'
     * registrato), con il contatore dei tick impostato su un multiplo dell'intervallo: la posizione si ricorda. Da' il
     * punto assoluto.
     */
    static Vec3 rememberSafeSpot(GameTestHelper helper, ServerPlayer player, Vec3 relativeFeet) {
        helper.setBlock(BlockPos.containing(relativeFeet).below(), Blocks.STONE);
        Vec3 spot = helper.absoluteVec(relativeFeet);
        helper.assertTrue(!SafeSpots.last(player).map(SafeSpots.Spot::position).equals(Optional.of(spot)),
                "the new spot is already the safe spot");
        player.moveTo(spot.x, spot.y, spot.z, player.getYRot(), player.getXRot());
        player.setOnGround(true);
        player.tickCount = SafeSpots.INTERVAL_TICKS;
        TickEvent.PLAYER_POST.invoker().tick(player);
        helper.assertTrue(SafeSpots.last(player).equals(Optional.of(new SafeSpots.Spot(helper.getLevel().dimension(),
                spot))), "safe spot not remembered: " + SafeSpots.last(player) + ", expected " + spot);
        return spot;
    }

    /** Una porta di ferro chiusa, le due meta', con la meta' inferiore nel punto relativo e l'orientamento dato. */
    static void placeClosedIronDoor(GameTestHelper helper, BlockPos lower, Direction facing) {
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.OPEN, false);
        helper.setBlock(lower, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(lower.above(), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /**
     * Premessa dei test del passo fuori: la forma di collisione del blocco nel punto relativo sta tutta fra
     * {@code minZ} e {@code maxZ} del blocco (lungo z), cioe' e' l'ostacolo sottile che il test vuole.
     */
    static void assertThinAlongZ(GameTestHelper helper, BlockPos relative, double minZ, double maxZ) {
        BlockPos pos = helper.absolutePos(relative);
        AABB bounds = helper.getLevel().getBlockState(pos).getCollisionShape(helper.getLevel(), pos).bounds();
        helper.assertTrue(Math.abs(bounds.minZ - minZ) < 1e-6 && Math.abs(bounds.maxZ - maxZ) < 1e-6,
                "obstacle at " + relative + " is not the thin shape the test expects: " + bounds);
    }

    /**
     * Premessa dei test del passo fuori: un giocatore vanilla in piedi con i piedi nel punto assoluto {@code feet} ha
     * spazio e un pavimento sotto i piedi: il passo non ci va per cio' che sta in mezzo, non perche' manchi il posto.
     */
    static void assertRoomToStand(GameTestHelper helper, Vec3 feet) {
        AABB box = new AABB(feet.x - 0.3, feet.y, feet.z - 0.3, feet.x + 0.3, feet.y + 1.8, feet.z + 0.3);
        helper.assertTrue(helper.getLevel().noCollision(box) && !helper.getLevel().noCollision(box.move(0, -0.01, 0)),
                "premise: no room to stand at " + feet);
    }

    /**
     * Il giocatore, nel telaio piazzato {@code frameId} (fermo nel punto in cui era stato indossato), ci rientra
     * rivolto a {@code yaw}, controlla che il posto davanti a un blocco oltre l'ostacolo abbia spazio e pavimento, ed
     * esce: il passo va a sinistra, dalla sua parte dell'ostacolo, il cui lato vicino e' al {@code near} assoluto (z).
     * Alla fine il giocatore e' fuori, il telaio piazzato.
     */
    static void assertStepAvoids(GameTestHelper helper, ServerPlayer player, UUID frameId, float yaw,
            double near) {
        ServerLevel level = helper.getLevel();
        if (Compat.frameState().wornFrameId(player).isEmpty()) {
            PlacedFrame record = Compat.frameState().placed(level.getServer(), frameId).orElseThrow();
            Vec3 at = record.position();
            player.moveTo(at.x, at.y, at.z, yaw, 0f);
            helper.assertTrue(FrameEntry.enter(player, (ExoframeEntity) level.getEntity(frameId)), "enter refused");
        }
        Vec3 spot = player.position();
        player.moveTo(spot.x, spot.y, spot.z, yaw, 0f);
        double radians = Math.toRadians(yaw);
        assertRoomToStand(helper, spot.add(-Math.sin(radians), 0.0, Math.cos(radians)));

        helper.assertTrue(FrameEntry.exit(player), "exit refused");

        Vec3 left = spot.add(Math.cos(radians), 0.0, Math.sin(radians));
        helper.assertTrue(player.getBoundingBox().maxZ <= near + 1e-9, "player stepped through the obstacle to "
                + player.position() + " facing " + yaw);
        helper.assertTrue(player.position().distanceTo(left) < 1e-6, "player facing " + yaw + " did not step left: "
                + player.position() + ", expected " + left);
    }

    /**
     * Il giocatore, nel telaio piazzato {@code frameId} nel punto assoluto {@code spot} (dove era stato indossato), ci
     * rientra se non ne indossa uno, esce rivolto a +Z (yaw 0) e fa il passo a sinistra, un blocco verso +X: il posto
     * davanti, con {@code hazard}, e' scartato. Alla fine il giocatore e' fuori, il telaio piazzato.
     */
    static void assertExitStepsLeft(GameTestHelper helper, ServerPlayer player, UUID frameId, Vec3 spot,
            String hazard) {
        ServerLevel level = helper.getLevel();
        if (Compat.frameState().wornFrameId(player).isEmpty()) {
            player.moveTo(spot.x, spot.y, spot.z, 0f, 0f);
            helper.assertTrue(FrameEntry.enter(player, (ExoframeEntity) level.getEntity(frameId)),
                    "enter refused before " + hazard);
        }
        Vec3 at = player.position();
        player.moveTo(at.x, at.y, at.z, 0f, 0f);

        helper.assertTrue(FrameEntry.exit(player), "exit refused before " + hazard);

        helper.assertTrue(player.position().distanceTo(spot.add(1.0, 0.0, 0.0)) < 1e-6,
                "player stepped onto " + hazard + ": " + player.position());
    }

    /** Gli oggetti a terra entro 2,5 blocchi da {@code center}, come li lascia nel suo tick una morte in quel punto. */
    static List<ItemEntity> deathDrops(ServerLevel level, Vec3 center) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(center, center).inflate(2.5));
    }

    /**
     * Un telaio piazzato con lo stato dato e un paio di stivali di ferro stivati, come dopo una morte: il giocatore
     * entra dal registro, stiva gli stivali ed esce; poi l'entita' nel punto relativo alla struttura.
     */
    static ExoframeEntity frameWithStowedBoots(GameTestHelper helper, ServerPlayer player, FrameState state,
            Vec3 relativeFeet) {
        ServerLevel level = helper.getLevel();
        FrameStateAccess frames = Compat.frameState();
        Vec3 feet = helper.absoluteVec(relativeFeet);
        UUID frameId = frames.place(level, state, feet, 0f);
        helper.assertTrue(frameId != null && frames.enter(player, frameId), "place or enter refused");
        frames.stow(player, List.of(new StowedStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS))));
        helper.assertTrue(frames.exit(player, level, feet, 0f).isPresent(), "exit refused");
        return spawnFrame(level, frameId, feet, 0f);
    }

    /** Nulla e' cambiato dopo un ingresso rifiutato: record piazzato, entita', elmo di ferro, nessun segnaposto. */
    static void assertNotEntered(GameTestHelper helper, ServerPlayer player, ExoframeEntity frame,
            String when) {
        FrameStateAccess frames = Compat.frameState();
        helper.assertTrue(frames.placed(helper.getLevel().getServer(), frame.frameId()).isPresent()
                && !frame.isRemoved(), when + ": frame no longer placed");
        helper.assertTrue(frames.wornFrameId(player).isEmpty(), when + ": player has a worn record");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
                && countPlaceholders(player) == 0, when + ": player's armor changed");
    }

    /**
     * Toglie il record indossato del test e i segnaposto ({@link DebugWear#remove}); l'armatura ancora stivata torna
     * al giocatore.
     */
    static void leaveTestFrame(ServerPlayer player) {
        DebugWear.remove(player);
    }

    /** Lo stato con una cella carica a meta'. */
    static FrameState charged(FrameState state) {
        int capacity = RedstoneExoframes.config().energy.cellCapacity;
        state.insertCell(new EnergyCell(capacity, capacity / 2));
        return state;
    }

    /**
     * Usa l'oggetto telaio (uno, in mano) sulla faccia superiore del blocco dato con il suo {@code useOn}; se
     * {@code expectPlaced} restituisce l'unico telaio in piedi sul blocco, altrimenti verifica il rifiuto e da' null.
     */
    static ExoframeEntity useExoframeItem(GameTestHelper helper, ServerPlayer player, BlockPos relative,
            boolean expectPlaced) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EXOFRAME.get()));
        BlockPos clicked = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked).add(0.0, 0.5, 0.0), Direction.UP, clicked,
                false);
        InteractionResult result = ModItems.EXOFRAME.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                hit));
        if (!expectPlaced) {
            helper.assertTrue(!result.consumesAction(), "refused use reported " + result);
            return null;
        }
        helper.assertTrue(result.consumesAction(), "use of the exoframe item reported " + result);
        return frameAt(helper, Vec3.atBottomCenterOf(clicked.above()));
    }

    /** L'unico telaio in piedi esattamente in {@code feet} (riquadro della tuta). */
    static ExoframeEntity frameAt(GameTestHelper helper, Vec3 feet) {
        List<ExoframeEntity> found = helper.getLevel().getEntitiesOfClass(ExoframeEntity.class,
                SuitSize.standingBox(feet).inflate(0.5));
        helper.assertTrue(found.size() == 1, found.size() + " frames near " + feet + ", expected 1");
        ExoframeEntity frame = found.get(0);
        helper.assertTrue(frame.position().equals(feet), "frame at " + frame.position() + ", expected " + feet);
        return frame;
    }

    /** Nessun record piazzato in {@code feet} e nessun telaio nel riquadro della tuta li'. */
    static void assertNothingPlacedAt(GameTestHelper helper, Vec3 feet) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(Compat.frameState().placedIn(level).stream().noneMatch(p -> p.position().equals(feet)),
                "a record was placed at " + feet);
        helper.assertTrue(level.getEntitiesOfClass(ExoframeEntity.class, SuitSize.standingBox(feet)).isEmpty(),
                "a frame entity stands at " + feet);
    }

    /** Verifica il suolo trovato da {@link FramePlacement#groundBelow} partendo da un punto relativo alla struttura. */
    static void assertGround(GameTestHelper helper, Vec3 from, Vec3 expected, String where) {
        Optional<Vec3> ground = FramePlacement.groundBelow(helper.getLevel(), helper.absoluteVec(from));
        Vec3 want = helper.absoluteVec(expected);
        helper.assertTrue(ground.isPresent() && ground.get().distanceTo(want) < 1e-6,
                where + ": ground " + ground + ", expected " + want);
    }

    /** Crea l'entita' telaio con l'identita' data nel punto e con l'orientamento dati e la aggiunge al livello. */
    static ExoframeEntity spawnFrame(ServerLevel level, UUID frameId, Vec3 feet, float yaw) {
        ExoframeEntity frame = new ExoframeEntity(ModEntities.EXOFRAME.get(), level);
        frame.setUUID(frameId);
        frame.moveTo(feet.x, feet.y, feet.z, yaw, 0f);
        level.addFreshEntity(frame);
        return frame;
    }

    /** Piazza nel registro un telaio con lo stato dato e ne aggiunge l'entita' nel punto relativo alla struttura. */
    static ExoframeEntity placeFrame(GameTestHelper helper, FrameState state, Vec3 relativeFeet, float yaw) {
        Vec3 feet = helper.absoluteVec(relativeFeet);
        UUID frameId = Compat.frameState().place(helper.getLevel(), state, feet, yaw);
        helper.assertTrue(frameId != null, "place returned no frame id");
        return spawnFrame(helper.getLevel(), frameId, feet, yaw);
    }

    /** Lo stato vivo del record piazzato del telaio. */
    static FrameState recordState(GameTestHelper helper, ExoframeEntity frame) {
        PlacedFrame record = Compat.frameState().placed(helper.getLevel().getServer(), frame.frameId()).orElse(null);
        helper.assertTrue(record != null, "frame " + frame.frameId() + " is not placed");
        return record.state();
    }

    /** Toglie il record piazzato del telaio, se c'e' ancora, e l'entita'. */
    static void removeFrame(MinecraftServer server, ExoframeEntity frame) {
        Compat.frameState().removePlaced(server, frame.frameId());
        frame.discard();
    }

    /**
     * L'unico oggetto del tipo dato a terra vicino al giocatore: uno stack da uno, ai suoi piedi e fermo (non
     * lanciato). Chi chiama lo toglie dal mondo.
     */
    static ItemEntity assertDroppedAtFeet(GameTestHelper helper, ServerPlayer player, Item item) {
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(2.0), entity -> entity.getItem().is(item));
        helper.assertTrue(dropped.size() == 1 && dropped.get(0).getItem().getCount() == 1,
                dropped.size() + " dropped " + item + ", expected one stack of 1");
        ItemEntity drop = dropped.get(0);
        helper.assertTrue(drop.position().distanceTo(player.position()) < 1e-6
                && drop.getDeltaMovement().equals(Vec3.ZERO), "dropped " + item + " not lying at the feet: "
                + drop.position() + " moving " + drop.getDeltaMovement() + ", feet " + player.position());
        return drop;
    }

    /**
     * L'unico oggetto del tipo dato a terra vicino a {@code feet}: uno stack da uno, fermo (non lanciato), in
     * {@code feet}. Chi chiama lo toglie dal mondo.
     */
    static ItemEntity assertDroppedAt(GameTestHelper helper, Vec3 feet, Item item) {
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(feet, feet).inflate(2.0), entity -> entity.getItem().is(item));
        helper.assertTrue(dropped.size() == 1 && dropped.get(0).getItem().getCount() == 1,
                dropped.size() + " dropped " + item + ", expected one stack of 1");
        ItemEntity drop = dropped.get(0);
        helper.assertTrue(drop.position().distanceTo(feet) < 1e-6 && drop.getDeltaMovement().equals(Vec3.ZERO),
                "dropped " + item + " not lying at " + feet + ": " + drop.position() + " moving "
                        + drop.getDeltaMovement());
        return drop;
    }

    /** L'oggetto a terra c'e' ancora, con uno stack uguale a quello atteso (dati compresi). */
    static void assertStillOnTheGround(GameTestHelper helper, ItemEntity drop, ItemStack expected,
            String when) {
        helper.assertTrue(!drop.isRemoved() && helper.getLevel().getEntity(drop.getUUID()) == drop,
                "dropped " + expected + " gone (" + when + ")");
        helper.assertTrue(ItemStack.matches(expected, drop.getItem()),
                "dropped stack changed (" + when + "): " + drop.getItem() + ", expected " + expected);
    }

    /** Uno stato vuoto con il giocatore come proprietario. */
    static FrameState ownedBy(ServerPlayer player) {
        FrameState state = new FrameState(RedstoneExoframes.config());
        state.setOwner(player.getUUID());
        return state;
    }

    /** Un pezzo Rottame nuovo nello slot. */
    static Piece scrapPiece(PieceSlot slot) {
        return Piece.fresh(slot, Tier.SCRAP, RedstoneExoframes.config().tiers);
    }

    /**
     * L'oggetto di un pezzo Rottame dello slot con la durata data su 180 (il massimo predefinito del Rottame), senza
     * moduli e con l'aspetto dato: come lo da' lo smontaggio.
     */
    static ItemStack scrapPieceStack(PieceSlot slot, int durability, Appearance look) {
        return PieceItem.fromPiece(new Piece(slot, Tier.SCRAP, 180, durability), List.of(), look);
    }

    /** Un clic destro con la mano principale sul telaio, nel punto dato relativo ai suoi piedi. */
    static InteractionResult click(ExoframeEntity frame, ServerPlayer player, double x, double y, double z) {
        return frame.interactAt(player, new Vec3(x, y, z), InteractionHand.MAIN_HAND);
    }

    /** L'unico stack del tipo dato nell'inventario principale; vuoto se non c'e'. */
    static ItemStack findInMainInventory(ServerPlayer player, Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Le identita' dei telai piazzati nel livello. */
    static List<UUID> placedIds(ServerLevel level) {
        return Compat.frameState().placedIn(level).stream().map(PlacedFrame::frameId).toList();
    }

    /** Una voce dell'armatura stivata scritta a mano: slot della testa, versione dei dati e stack dati. */
    static CompoundTag stowedEntry(int dataVersion, CompoundTag item) {
        CompoundTag entry = new CompoundTag();
        entry.putString(NbtKeys.SLOT, "head");
        entry.putInt(NbtKeys.DATA_VERSION, dataVersion);
        entry.put(NbtKeys.ITEM, item);
        return entry;
    }

    /** Verifica che la lista abbia una sola voce, nello slot della testa, uguale allo stack atteso. */
    static void assertOneStowedHelmet(GameTestHelper helper, List<StowedStack> taken, ItemStack expected,
            String where) {
        helper.assertTrue(taken.size() == 1, where + ": expected one stowed stack, got " + taken);
        StowedStack stowed = taken.get(0);
        helper.assertTrue(stowed.slot() == EquipmentSlot.HEAD, where + ": stowed slot is " + stowed.slot());
        helper.assertTrue(ItemStack.matches(expected, stowed.stack()) && stowed.stack().getDamageValue() == 5,
                where + ": stowed stack " + stowed.stack() + " differs from " + expected);
    }

    /** Verifica larghezza, altezza, occhi e occhi da accovacciato del giocatore. */
    static void assertSize(GameTestHelper helper, ServerPlayer player, String when, float width,
            float height, float eyes, float crouchingEyes) {
        helper.assertTrue(near(player.getBbWidth(), width) && near(player.getBbHeight(), height)
                && near(player.getEyeHeight(), eyes) && near(player.getEyeHeight(Pose.CROUCHING), crouchingEyes),
                "wrong size " + when + ": " + size(player) + ", expected w=" + width + " h=" + height + " eye="
                        + eyes + " crouchEye=" + crouchingEyes);
    }

    /**
     * Mette il giocatore nella posa (il cambio di posa ricalcola le dimensioni), verifica larghezza, altezza del
     * riquadro e occhi, e lo rimette in piedi.
     */
    static void assertPoseSize(GameTestHelper helper, ServerPlayer player, Pose pose, float width,
            float height, float eyes) {
        player.setPose(pose);
        String size = size(player);
        boolean ok = near(player.getBbWidth(), width) && near(player.getBbHeight(), height)
                && near((float) player.getBoundingBox().getYsize(), height) && near(player.getEyeHeight(), eyes);
        player.setPose(Pose.STANDING);
        helper.assertTrue(ok, "wrong size in pose " + pose + " with the suit: " + size + ", expected w=" + width
                + " h=" + height + " eye=" + eyes);
    }

    /** Ingombro e occhi del giocatore nella posa data, letti mettendolo in quella posa; poi lo rimette in piedi. */
    static String sizeInPose(ServerPlayer player, Pose pose) {
        player.setPose(pose);
        String size = String.format(Locale.ROOT, "pose=%s w=%.4f h=%.4f box=%.4f eye=%.4f", player.getPose(),
                player.getBbWidth(), player.getBbHeight(), player.getBoundingBox().getYsize(), player.getEyeHeight());
        player.setPose(Pose.STANDING);
        return size;
    }

    /** Posa, ingombro e occhi del giocatore, per i messaggi dei test. */
    static String size(ServerPlayer player) {
        return String.format(Locale.ROOT, "pose=%s w=%.4f h=%.4f eye=%.4f crouchEye=%.4f", player.getPose(),
                player.getBbWidth(), player.getBbHeight(), player.getEyeHeight(),
                player.getEyeHeight(Pose.CROUCHING));
    }

    static boolean near(float a, float b) {
        return Math.abs(a - b) < 1e-3f;
    }

    static boolean near(AABB a, AABB b) {
        return Math.abs(a.minX - b.minX) < 1e-3 && Math.abs(a.minY - b.minY) < 1e-3 && Math.abs(a.minZ - b.minZ) < 1e-3
                && Math.abs(a.maxX - b.maxX) < 1e-3 && Math.abs(a.maxY - b.maxY) < 1e-3
                && Math.abs(a.maxZ - b.maxZ) < 1e-3;
    }

    /** Esegue un comando che deve riuscire; un errore di sintassi o di esecuzione fa fallire il test. */
    static void runCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source,
            String command) {
        try {
            dispatcher.execute(command, source);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("command failed: " + command + ": " + e.getMessage(), e);
        }
    }

    /** Il giocatore in piedi nel punto relativo alla struttura, rivolto a nord (z decrescenti), in orizzontale. */
    static void standFacingNorth(GameTestHelper helper, ServerPlayer player, Vec3 relativeFeet) {
        Vec3 feet = helper.absoluteVec(relativeFeet);
        player.moveTo(feet.x, feet.y, feet.z, 180f, 0f);
    }

    /**
     * Esegue {@code /exoframe <sub>} come il giocatore con il livello di permesso dato e ne restituisce il risultato;
     * un errore di sintassi fa fallire il test.
     */
    static int exoframeCommand(GameTestHelper helper, ServerPlayer player, int permission, String sub) {
        String command = "exoframe " + sub;
        try {
            return helper.getLevel().getServer().getCommands().getDispatcher().execute(command,
                    player.createCommandSourceStack().withPermission(permission).withSuppressedOutput());
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException("command failed: " + command + ": " + e.getMessage(), e);
        }
    }

    /** Verifica che i quattro slot armatura abbiano ciascuno il proprio segnaposto. */
    static void assertPlaceholdersWorn(GameTestHelper helper, ServerPlayer player) {
        Map<EquipmentSlot, Item> expected = Map.of(
                EquipmentSlot.HEAD, ModItems.EXOFRAME_HELMET.get(),
                EquipmentSlot.CHEST, ModItems.EXOFRAME_TORSO.get(),
                EquipmentSlot.LEGS, ModItems.EXOFRAME_LEGS.get(),
                EquipmentSlot.FEET, ModItems.EXOFRAME_BOOTS.get());
        expected.forEach((slot, item) -> helper.assertTrue(player.getItemBySlot(slot).is(item),
                "slot " + slot + " holds " + player.getItemBySlot(slot) + ", expected " + item));
    }

    /** Quanti segnaposto ha il giocatore in tutto l'inventario (slot armatura e mano secondaria compresi). */
    static int countPlaceholders(ServerPlayer player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (Placeholders.isPlaceholder(player.getInventory().getItem(i))) {
                count += player.getInventory().getItem(i).getCount();
            }
        }
        return count;
    }

    /** Quanti item del tipo dato ci sono nell'inventario principale (senza slot armatura e mano secondaria). */
    static int countInMainInventory(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Aggiunge un test obbligatorio nella struttura vuota, il cui corpo parte con {@link #startWhenEntitiesLoaded}. Il
     * nome {@code temaCaso} diventa {@code redstone_exoframes:tema_caso}: Forge lo legge come ResourceLocation, che non
     * ammette maiuscole.
     */
    static void add(List<TestFunction> tests, String name, Consumer<GameTestHelper> test) {
        String id = RedstoneExoframes.MOD_ID + ":" + name.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT);
        tests.add(new TestFunction(RedstoneExoframes.MOD_ID, id, EMPTY_STRUCTURE, MAX_TICKS, 0L, true,
                helper -> startWhenEntitiesLoaded(helper, test)));
    }

    /**
     * Fa partire il corpo del test solo quando i chunk coperti dalla sua struttura hanno le entita' caricate
     * ({@code ServerLevel.areEntitiesLoaded}). Il gioco le carica in modo asincrono quando il chunk si carica, e su
     * 1.20.1 un test comincia nel tick dopo quello in cui la sua struttura e' piazzata, senza aspettarle (1.21.1
     * aspetta almeno che i chunk della struttura facciano muovere le entita', che non vuol dire ancora averle
     * caricate): su una macchina lenta un test puo' cominciare prima che ci siano, e il riallineamento, che salta per
     * scelta quei chunk, non agirebbe al primo tick del test. Se le entita' ci sono gia', il corpo parte subito, come
     * sempre; altrimenti una sequenza del test riguarda a ogni tick e fa partire il corpo nel tick in cui ci sono, al
     * piu' dopo {@value #ENTITY_LOAD_TICKS} tick (presi dai {@value #MAX_TICKS} del test); se non arrivano il test
     * fallisce e dice quali chunk mancano.
     */
    static void startWhenEntitiesLoaded(GameTestHelper helper, Consumer<GameTestHelper> test) {
        if (chunksWithoutEntities(helper).isEmpty()) {
            test.accept(helper);
            return;
        }
        int[] waited = {0};
        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (!chunksWithoutEntities(helper).isEmpty() && ++waited[0] < ENTITY_LOAD_TICKS) {
                        throw new GameTestAssertException("waiting for the entities of the test's chunks");
                    }
                })
                .thenExecute(() -> {
                    List<ChunkPos> missing = chunksWithoutEntities(helper);
                    if (!missing.isEmpty()) {
                        helper.fail("entities of chunks " + missing + " not loaded after " + ENTITY_LOAD_TICKS
                                + " ticks: the test did not start");
                    }
                    runInSequence(helper, test);
                });
    }

    /**
     * Il corpo del test dentro una sequenza: la sequenza fa fallire il test solo per un {@link GameTestAssertException}
     * e lascerebbe uscire dal tick del server ogni altra eccezione, che invece l'avvio normale del test raccoglie;
     * percio' qui diventa un fallimento del test, con l'eccezione come causa.
     */
    static void runInSequence(GameTestHelper helper, Consumer<GameTestHelper> test) {
        try {
            test.accept(helper);
        } catch (GameTestAssertException e) {
            throw e;
        } catch (RuntimeException e) {
            GameTestAssertException failure = new GameTestAssertException(e.toString());
            failure.initCause(e);
            throw failure;
        }
    }

    /** I chunk coperti dalla struttura del test le cui entita' non sono ancora caricate. */
    static List<ChunkPos> chunksWithoutEntities(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos a = helper.absolutePos(BlockPos.ZERO);
        BlockPos b = helper.absolutePos(new BlockPos(STRUCTURE_SIZE - 1, STRUCTURE_SIZE - 1, STRUCTURE_SIZE - 1));
        List<ChunkPos> missing = new ArrayList<>();
        for (int x = Math.min(a.getX(), b.getX()) >> 4; x <= Math.max(a.getX(), b.getX()) >> 4; x++) {
            for (int z = Math.min(a.getZ(), b.getZ()) >> 4; z <= Math.max(a.getZ(), b.getZ()) >> 4; z++) {
                if (!level.areEntitiesLoaded(ChunkPos.asLong(x, z))) {
                    missing.add(new ChunkPos(x, z));
                }
            }
        }
        return missing;
    }

    /** Il tick del server ({@code server.getTickCount()}): il {@code now} da dare alle regole di tempo della mod. */
    static long now(GameTestHelper helper) {
        return helper.getLevel().getServer().getTickCount();
    }

    /** Telaio del giocatore con sei pezzi Rottame nuovi e una cella della capacita' in config con {@code stored} RE. */
    static FrameState scrapState(ServerPlayer owner, int stored) {
        FrameState state = ownedBy(owner);
        for (PieceSlot slot : PieceSlot.values()) {
            state.mount(scrapPiece(slot));
        }
        state.insertCell(new EnergyCell(RedstoneExoframes.config().energy.cellCapacity, stored));
        return state;
    }

    /** Fa entrare il giocatore in un {@link #scrapState} con {@link #wearFrameAt} ({@code stored} >= 1); il suo id. */
    static UUID wearScrap(GameTestHelper helper, ServerPlayer player, Vec3 relativeFeet, int stored) {
        return wearFrameAt(helper, player, scrapState(player, stored), relativeFeet);
    }

    /** La carica della cella del telaio indossato; {@link IllegalStateException} senza telaio o senza cella. */
    static int stored(ServerPlayer player) {
        EnergyCell cell = Compat.frameState().get(player).map(FrameState::cell)
                .orElseThrow(() -> new IllegalStateException("no worn frame with a cell"));
        return cell.stored();
    }

    /**
     * Porta a {@code stored} RE la cella del telaio indossato con {@code changeWorn} (cambio strutturale, sincronizza):
     * {@link IllegalStateException} senza telaio o senza cella.
     */
    static void setStored(ServerPlayer player, int stored) {
        boolean changed = Compat.frameState().changeWorn(player, s -> {
            EnergyCell cell = s.cell();
            if (cell == null) {
                throw new IllegalStateException("no cell in the worn frame");
            }
            s.insertCell(new EnergyCell(cell.capacity(), stored));
        });
        if (!changed) {
            throw new IllegalStateException("no worn frame");
        }
    }

    /** Il pezzo dello slot nel telaio indossato, null se vuoto; {@link IllegalStateException} senza telaio. */
    static Piece worn(ServerPlayer player, PieceSlot slot) {
        return Compat.frameState().get(player).orElseThrow(() -> new IllegalStateException("no worn frame"))
                .pieces().get(slot);
    }

    /** Riporto del danno dello slot nel telaio indossato, in millesimi; {@link IllegalStateException} senza telaio. */
    static int carry(ServerPlayer player, PieceSlot slot) {
        return Compat.frameState().get(player).orElseThrow(() -> new IllegalStateException("no worn frame"))
                .damageCarryMilli().getOrDefault(slot, 0);
    }

    /** Uno stack di cella con {@code stored} RE. */
    static ItemStack cell(int stored) {
        return CellItem.fromCell(new EnergyCell(RedstoneExoframes.config().energy.cellCapacity, stored));
    }

    /** La carica scritta nei dati dello stack. */
    static int energy(ItemStack stack) {
        return Compat.itemData().cellEnergy(stack);
    }

    /**
     * Il clic destro vero del gioco sul blocco relativo alla struttura, con la mano data e cio' che tiene: lo stesso
     * percorso di un giocatore ({@code ServerPlayerGameMode.useItemOn}: prima il blocco, poi l'oggetto in mano), sulla
     * faccia di sopra, al centro del blocco. Restituisce il risultato del gioco.
     */
    static InteractionResult clickBlock(GameTestHelper helper, ServerPlayer player, BlockPos relative,
            InteractionHand hand) {
        BlockPos abs = helper.absolutePos(relative);
        return player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(hand), hand,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }

    /**
     * Apre la stazione per il giocatore come la apre il clic sul server, ma senza la rete: cerca il telaio con la
     * ricerca del clic ({@link StationFinder#find}), che deve trovarlo ({@code FOUND}, altrimenti il test fallisce), e
     * costruisce il menu del server con il costruttore che il clic vero usa attraverso {@code StationMenu.provider},
     * con l'apertura del clic ({@link StationMenu#opening}), per la stazione con la meta' principale in
     * {@code relativeMain} (relativa alla struttura), con l'id {@link #STATION_MENU_ID}; lo mette in
     * {@code player.containerMenu} e lo restituisce. Il menu non si apre dalla rete del loader perche' i loader non lo
     * permettono al giocatore finto: su Fabric aprire un tipo di menu "esteso" con {@code ServerPlayer.openMenu} del
     * gioco base lancia, e su NeoForge il pacchetto dell'apertura con i dati extra, che non e' del gioco base, si
     * rifiuta verso una connessione senza canali negoziati come quella del giocatore finto. Senza sincronizzatore il
     * menu lavora sul server come quello vero (pulsanti, {@code stillValid}, {@code broadcastChanges}, chiusura) e non
     * manda pacchetti di slot ne' di dati del menu; la vista del telaio parte da {@code broadcastChanges} con
     * {@code Net}, che su NeoForge 1.21.1 salta il giocatore finto, e il menu conta i suoi invii
     * ({@code viewsSent}). Che il clic apra davvero la schermata lo prova il client vero (tools/client/m4a_shots.sh).
     */
    static StationMenu openStation(GameTestHelper helper, ServerPlayer player, BlockPos relativeMain) {
        ServerLevel level = helper.getLevel();
        BlockPos main = helper.absolutePos(relativeMain);
        StationFinder.Result found = StationFinder.find(player, level, main, level.getBlockState(main));
        helper.assertTrue(found.outcome() == StationFinder.Outcome.FOUND, "the station at " + relativeMain
                + " found no frame for " + player.getGameProfile().getName() + ": " + found);
        PlacedFrame frame = found.frameId().flatMap(id -> Compat.frameState().placed(level.getServer(), id))
                .orElse(null);
        helper.assertTrue(frame != null, "the frame found by the station is not placed: " + found);
        StationMenu menu = new StationMenu(STATION_MENU_ID, player.getInventory(), level,
                StationMenu.opening(player, main, frame));
        player.containerMenu = menu;
        return menu;
    }

    /**
     * Un telaio con lo stato dato con i piedi sopra la meta' principale della stazione in {@code relativeMain}
     * (relativa alla struttura), al centro del blocco, con yaw 0 ({@link #placeFrame}).
     */
    static ExoframeEntity frameOnStation(GameTestHelper helper, FrameState state, BlockPos relativeMain) {
        return placeFrame(helper, state, new Vec3(relativeMain.getX() + 0.5, relativeMain.getY() + 1,
                relativeMain.getZ() + 0.5), 0f);
    }

    /**
     * Il pulsante {@code buttonId} del menu {@code containerId} premuto dal giocatore come lo preme il client: il
     * pacchetto del gioco base passa dal suo gestore sul server, con i suoi controlli (l'id del menu aperto, lo
     * spettatore, {@code stillValid}) e il {@code broadcastChanges} finale.
     */
    static void pressButton(ServerPlayer player, int containerId, int buttonId) {
        player.connection.handleContainerButtonClick(new ServerboundContainerButtonClickPacket(containerId, buttonId));
    }

    /**
     * Posa con {@code helper.setBlock} una stazione con il fronte verso {@code facing}: la meta' principale in
     * {@code relativeMain} (relativa alla struttura), poi la laterale accanto, verso
     * {@code facing.getCounterClockWise()} (alla destra di chi guarda il fronte, come la posa l'oggetto). Restituisce
     * la posizione relativa della laterale. Per toglierla, prima la principale: la laterale rimasta sola diventa aria
     * senza bottino, mentre una principale rimasta sola diventerebbe aria lasciando a terra una stazione.
     */
    static BlockPos placeStation(GameTestHelper helper, BlockPos relativeMain, Direction facing) {
        BlockState main = ModBlocks.STATION.get().defaultBlockState().setValue(StationBlock.FACING, facing);
        BlockPos relativeSide = relativeMain.relative(facing.getCounterClockWise());
        helper.setBlock(relativeMain, main);
        helper.setBlock(relativeSide, main.setValue(StationBlock.PART, StationPart.SIDE));
        return relativeSide;
    }

    /** Quante stazioni ci sono a terra nel riquadro della struttura: la somma dei conti degli oggetti stazione. */
    static int stationsOnTheGround(GameTestHelper helper) {
        int count = 0;
        for (ItemEntity drop : itemsIn(helper)) {
            if (drop.getItem().is(ModItems.STATION.get())) {
                count += drop.getItem().getCount();
            }
        }
        return count;
    }

    /** Gli oggetti a terra dentro il riquadro della struttura del test ({@link #structureBox}). */
    static List<ItemEntity> itemsIn(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, structureBox(helper));
    }

    /** Il riquadro assoluto della struttura del test ({@value #STRUCTURE_SIZE} blocchi per lato), ruotata o no. */
    static AABB structureBox(GameTestHelper helper) {
        BlockPos a = helper.absolutePos(BlockPos.ZERO);
        BlockPos b = helper.absolutePos(new BlockPos(STRUCTURE_SIZE - 1, STRUCTURE_SIZE - 1, STRUCTURE_SIZE - 1));
        return new AABB(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()) + 1);
    }

    /**
     * Nove stack per una griglia 3x3 del banco da lavoro, per righe, in una lista nuova che chi chiama puo' cambiare:
     * al piu' tre righe, ciascuna da uno a tre caratteri, riempite a destra e in basso con il vuoto. {@code I} lingotto
     * di ferro, {@code C} lingotto di rame, {@code R} blocco di redstone, {@code r} polvere di redstone, {@code Q}
     * quarzo, {@code T} banco da lavoro, {@code _} vuoto: le lettere delle forme di design §12.12 e, per la stazione,
     * di design M4 §5.1. {@link IllegalArgumentException} per un altro carattere, per una riga vuota o troppo lunga e
     * per piu' di tre righe.
     */
    static List<ItemStack> craftingGrid(String... rows) {
        if (rows.length > CRAFTING_GRID_SIDE) {
            throw new IllegalArgumentException(rows.length + " rows do not fit a crafting grid");
        }
        List<ItemStack> grid = new ArrayList<>();
        for (int row = 0; row < CRAFTING_GRID_SIDE; row++) {
            String cells = row < rows.length ? rows[row] : "_";
            if (cells.isEmpty() || cells.length() > CRAFTING_GRID_SIDE) {
                throw new IllegalArgumentException("crafting row '" + cells + "' is not one to three characters");
            }
            for (int column = 0; column < CRAFTING_GRID_SIDE; column++) {
                char letter = column < cells.length() ? cells.charAt(column) : '_';
                grid.add(switch (letter) {
                    case 'I' -> new ItemStack(Items.IRON_INGOT);
                    case 'C' -> new ItemStack(Items.COPPER_INGOT);
                    case 'R' -> new ItemStack(Items.REDSTONE_BLOCK);
                    case 'r' -> new ItemStack(Items.REDSTONE);
                    case 'Q' -> new ItemStack(Items.QUARTZ);
                    case 'T' -> new ItemStack(Items.CRAFTING_TABLE);
                    case '_' -> ItemStack.EMPTY;
                    default -> throw new IllegalArgumentException("unknown crafting letter '" + letter + "' in '"
                            + cells + "'");
                });
            }
        }
        return grid;
    }

    /**
     * Vasca: {@code fluid} fra gli angoli relativi dati (compresi), bordo di vetro, pavimento di pietra sotto; bordo e
     * pavimento (un blocco oltre gli angoli) devono stare dentro la struttura.
     */
    static void fillPool(GameTestHelper helper, BlockPos from, BlockPos to, BlockState fluid) {
        int minX = Math.min(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxY = Math.max(from.getY(), to.getY());
        int maxZ = Math.max(from.getZ(), to.getZ());
        for (int x = minX - 1; x <= maxX + 1; x++) {
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                helper.setBlock(new BlockPos(x, minY - 1, z), Blocks.STONE);
                boolean wall = x < minX || x > maxX || z < minZ || z > maxZ;
                for (int y = minY; wall && y <= maxY; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.GLASS);
                }
            }
        }
        // il fluido per ultimo, con pavimento e pareti gia' al loro posto
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    helper.setBlock(new BlockPos(x, y, z), fluid);
                }
            }
        }
    }
}
