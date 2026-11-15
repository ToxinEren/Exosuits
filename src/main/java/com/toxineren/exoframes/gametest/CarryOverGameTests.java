package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.compat.GameTests;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.DebugWear;
import com.toxineren.exoframes.frame.FrameEntry;
import com.toxineren.exoframes.frame.FrameSight;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Test in gioco dei riporti di §11 nella parte 3c (i lavori rimasti dalla 3b), con in testa la sonda sul tick del
 * giocatore finto su cui contano i test della parte. Le regole comuni a tutte le classi di test e gli aiuti stanno in
 * {@link GameTestKit}.
 */
public final class CarryOverGameTests {
    /** Costruttore pubblico senza argomenti: GameTestRegistry vanilla istanzia la classe per chiamare il generatore. */
    public CarryOverGameTests() {
    }

    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> tests = new ArrayList<>();
        add(tests, "probeLevelTicksTheMockWithoutPlayerTickEvents",
                CarryOverGameTests::probeLevelTicksTheMockWithoutPlayerTickEvents);
        add(tests, "strayPlaceholderInAChestGoesWhenTheMenuCloses",
                CarryOverGameTests::strayPlaceholderInAChestGoesWhenTheMenuCloses);
        add(tests, "placeholderOfANonWearerGoesWhenAMenuCloses",
                CarryOverGameTests::placeholderOfANonWearerGoesWhenAMenuCloses);
        add(tests, "quitSaveIgnoresSaveOff", CarryOverGameTests::quitSaveIgnoresSaveOff);
        add(tests, "registryFileWithoutSectionsIsReadOnly", CarryOverGameTests::registryFileWithoutSectionsIsReadOnly);
        add(tests, "tallGrassHidesTheFrameFromTheCommand", CarryOverGameTests::tallGrassHidesTheFrameFromTheCommand);
        add(tests, "eyesInsideAFrameBoxPickThatFrame", CarryOverGameTests::eyesInsideAFrameBoxPickThatFrame);
        add(tests, "exitStepsUpASmallLip", CarryOverGameTests::exitStepsUpASmallLip);
        add(tests, "exitTakesAnAxisPathAroundAPost", CarryOverGameTests::exitTakesAnAxisPathAroundAPost);
        add(tests, "exitAvoidsCampfireMagmaAndBerries", CarryOverGameTests::exitAvoidsCampfireMagmaAndBerries);
        add(tests, "exitAvoidsWitherRosePowderSnowAndCactus",
                CarryOverGameTests::exitAvoidsWitherRosePowderSnowAndCactus);
        add(tests, "exitAvoidsAPortalBelow", CarryOverGameTests::exitAvoidsAPortalBelow);
        add(tests, "exitAvoidsABoatBelowTheSpot", CarryOverGameTests::exitAvoidsABoatBelowTheSpot);
        add(tests, "exitAvoidsAMinecartBelowTheSpot", CarryOverGameTests::exitAvoidsAMinecartBelowTheSpot);
        add(tests, "exitRefusedOverAnotherFrame", CarryOverGameTests::exitRefusedOverAnotherFrame);
        return tests;
    }

    /**
     * Sonda: il livello fa avanzare {@code tickCount} del giocatore finto e chiama il suo {@code ServerPlayer.tick()},
     * che scala {@code invulnerableTime} da 20 dopo un colpo (15 dopo cinque tick); {@code Player.tick()}, e quindi
     * {@code TickEvent.PLAYER_PRE}, per il giocatore finto non girano mai. I test di energia, danno e movimento della
     * parte contano su questo: chiamano a mano i tick della mod e non aspettano che un evento li faccia girare.
     */
    private static void probeLevelTicksTheMockWithoutPlayerTickEvents(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        Vec3 feet = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(feet.x, feet.y, feet.z, 0f, 0f);
        helper.assertTrue(!player.getAbilities().instabuild && !player.getAbilities().invulnerable
                && !player.isSpectator(), "survivalPlayer is not in survival");
        boolean[] seen = {false};
        TickEvent.Player listener = ticked -> {
            if (ticked == player) {
                seen[0] = true;
            }
        };
        TickEvent.PLAYER_PRE.register(listener);
        int start = player.tickCount;
        helper.assertTrue(player.hurt(player.damageSources().cactus(), 1.0F), "spawn protection still on");
        helper.runAfterDelay(5, () -> {
            try {
                helper.assertTrue(player.tickCount == start + 5,
                        "tickCount " + player.tickCount + ", expected " + (start + 5));
                helper.assertTrue(player.invulnerableTime == 15,
                        "invulnerableTime " + player.invulnerableTime + ", expected 15");
                helper.assertTrue(!seen[0], "PLAYER_PRE fired for the mock player");
            } finally {
                TickEvent.PLAYER_PRE.unregister(listener);
            }
            helper.succeed();
        });
    }

    /**
     * Una copia del segnaposto del torso finita in una cassa (piu' clic in un solo pacchetto, prima del tick della
     * guardia) se ne va quando il menu della cassa si chiude ({@code PlayerEvent.CLOSE_MENU}, design §12.14): per chi
     * indossa la tuta si tolgono solo le copie fuori posto, gli slot armatura restano gli stessi stack, e l'oggetto
     * vero accanto nella cassa non si tocca. Senza la pulizia alla chiusura il test non passerebbe: il tick della
     * guardia ({@code SlotGuard.tick}, su {@code PLAYER_POST}) per il giocatore finto non scatta mai, e comunque,
     * chiuso il menu, la cassa non e' piu' raggiungibile da nessun tick.
     */
    private static void strayPlaceholderInAChestGoesWhenTheMenuCloses(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        DebugWear.wear(player, SuitSummary.BARE);
        List<ItemStack> worn = List.copyOf(player.getInventory().armor);
        SimpleContainer chest = new SimpleContainer(27);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, chest),
                Component.literal("chest")));
        helper.assertTrue(player.containerMenu instanceof ChestMenu, "chest menu not open");
        chest.setItem(4, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        chest.setItem(5, new ItemStack(Items.DIRT, 3));
        player.closeContainer();
        helper.assertTrue(chest.getItem(4).isEmpty(), "stray torso placeholder left in the chest");
        helper.assertTrue(chest.getItem(5).is(Items.DIRT) && chest.getItem(5).getCount() == 3, "real item touched");
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "menu still open");
        for (int i = 0; i < worn.size(); i++) {
            helper.assertTrue(player.getInventory().armor.get(i) == worn.get(i), "armor slot " + i + " changed");
        }
        helper.assertTrue(countPlaceholders(player) == 4, countPlaceholders(player) + " placeholders, expected 4");
        DebugWear.remove(player);
        helper.succeed();
    }

    /**
     * Un segnaposto nell'inventario di chi non indossa un telaio se ne va quando un menu si chiude, anche quello
     * dell'inventario (che e' il menu aperto: {@code CLOSE_MENU} arriva lo stesso); l'oggetto vero accanto resta. Senza
     * la pulizia alla chiusura il test non passerebbe: {@code SlotGuard.tick} e' su {@code PLAYER_POST}, che per il
     * giocatore finto non scatta mai, quindi solo {@code CLOSE_MENU} puo' togliere quel segnaposto.
     */
    private static void placeholderOfANonWearerGoesWhenAMenuCloses(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        NonNullList<ItemStack> items = player.getInventory().items;
        items.set(5, new ItemStack(ModItems.EXOFRAME_HELMET.get()));
        items.set(6, new ItemStack(Items.DIRT, 2));
        player.closeContainer();
        helper.assertTrue(items.get(5).isEmpty(), "helmet placeholder left in the inventory");
        helper.assertTrue(countPlaceholders(player) == 0, countPlaceholders(player) + " placeholders, expected 0");
        helper.assertTrue(countInMainInventory(player, Items.DIRT) == 2,
                countInMainInventory(player, Items.DIRT) + " dirt, expected 2");
        helper.succeed();
    }

    /**
     * Il salvataggio all'uscita di un giocatore (design §12.14) non guarda {@code /save-off}: con il salvataggio
     * spento su tutti i livelli un telaio piazzato e' sul disco dopo {@code PlayerEvent.PLAYER_QUIT}, perche' il gioco
     * scrive comunque il file di chi esce e i due file vanno sul disco insieme. Scrive solo il file del registro: un
     * altro dato salvato dell'overworld (le incursioni), segnato da salvare, resta da salvare. Tutto nello stesso
     * tick, quindi nessun altro test salva i dati dell'overworld fra {@code setDirty} e l'ultima asserzione.
     */
    private static void quitSaveIgnoresSaveOff(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        GameTests tests = Compat.gameTests();
        Map<ServerLevel, Boolean> noSave = new HashMap<>();
        server.getAllLevels().forEach(level -> noSave.put(level, level.noSave));
        ExoframeEntity frame = null;
        try {
            server.getAllLevels().forEach(level -> level.noSave = true); // come /save-off
            frame = placeFrame(helper, ownedBy(player), new Vec3(2.5, 1.0, 2.5), 0f);
            helper.assertTrue(tests.registryChangeUnsaved(server), "placing a frame is not an unsaved change");
            Raids raids = server.overworld().getRaids();
            raids.setDirty();
            PlayerEvent.PLAYER_QUIT.invoker().quit(player);
            helper.assertTrue(!tests.registryChangeUnsaved(server), "the registry was not saved under /save-off");
            Optional<CompoundTag> onDisk = tests.readRegistry(server, tests.registryFolder(server));
            String frameId = frame.frameId().toString();
            helper.assertTrue(onDisk.isPresent() && onDisk.get().getCompound(NbtKeys.PLACED).contains(frameId),
                    "the placed frame is not in the registry on disk");
            helper.assertTrue(raids.isDirty(), "the quit save wrote the other saved data of the overworld");
        } finally {
            noSave.forEach((level, value) -> level.noSave = value);
            if (frame != null) {
                removeFrame(server, frame);
            }
        }
        helper.succeed();
    }

    /**
     * Un file del registro che il gioco legge ma che non ha {@code schema} ne' alcuna sezione (qui {@code data} =
     * {@code {foo: 1}}, in una copia in una cartella temporanea, mai il file del mondo) non e' un registro: il dato
     * resta in sola lettura con un ERROR (design §12.14, passo b), il file non si tocca e accanto non compare nulla
     * (nessuna copia messa da parte, nessun {@code _old}).
     */
    private static void registryFileWithoutSectionsIsReadOnly(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        GameTests tests = Compat.gameTests();
        Path copy = null;
        try {
            copy = Files.createTempDirectory("exoframes-registry-test");
            Path file = copy.resolve("redstone_exoframes_frames.dat");
            CompoundTag data = new CompoundTag();
            data.putInt("foo", 1);
            CompoundTag root = new CompoundTag();
            root.put("data", data);
            root.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
            try (OutputStream out = Files.newOutputStream(file)) {
                NbtIo.writeCompressed(root, out);
            }
            byte[] before = Files.readAllBytes(file);
            helper.assertTrue(tests.readRegistry(server, copy).isEmpty(),
                    "a registry file without schema and sections is not read-only");
            helper.assertTrue(Arrays.equals(before, Files.readAllBytes(file)), "the file without sections was changed");
            try (Stream<Path> files = Files.list(copy)) {
                helper.assertTrue(files.count() == 1, "extra files next to the registry");
            }
        } catch (IOException e) {
            throw new IllegalStateException("could not prepare the registry file without sections", e);
        } finally {
            deleteTree(copy);
        }
        helper.succeed();
    }
    /**
     * L'erba alta fra il giocatore e il telaio lo nasconde al comando come al clic (design §12.14: la linea di vista
     * dei comandi si ferma ai contorni dei blocchi, non alle forme di collisione): ne' {@code unlock} ne'
     * {@code remove} lo trovano; tolta l'erba, {@code unlock} lo trova. Premessa: le due meta' dell'erba non hanno
     * forma di collisione e hanno un contorno, altrimenti il test passerebbe anche per un blocco solido.
     */
    private static void tallGrassHidesTheFrameFromTheCommand(GameTestHelper helper) {
        ServerPlayer owner = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ExoframeEntity frame = placeFrame(helper, ownedBy(owner), new Vec3(2.5, 1.0, 2.5), 0f);
        standFacingNorth(helper, owner, new Vec3(2.5, 1.0, 5.5)); // occhi a y relativa 2,62
        BlockPos lower = new BlockPos(2, 1, 4);
        BlockPos upper = new BlockPos(2, 2, 4);
        BlockState grass = Blocks.TALL_GRASS.defaultBlockState();
        helper.setBlock(new BlockPos(2, 0, 4), Blocks.DIRT);
        helper.setBlock(lower, grass);
        helper.setBlock(upper, grass.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        for (BlockPos pos : List.of(lower, upper)) {
            BlockPos at = helper.absolutePos(pos);
            BlockState state = helper.getLevel().getBlockState(at);
            helper.assertTrue(state.is(Blocks.TALL_GRASS), "no tall grass at " + pos);
            helper.assertTrue(state.getCollisionShape(helper.getLevel(), at).isEmpty()
                    && !state.getShape(helper.getLevel(), at).isEmpty(),
                    "tall grass at " + pos + " has a collision shape or no outline");
        }

        helper.assertTrue(exoframeCommand(helper, owner, 0, "unlock") == 0, "unlocked a frame behind tall grass");
        helper.assertTrue(exoframeCommand(helper, owner, 2, "remove") == 0, "removed a frame behind tall grass");
        helper.assertTrue(!recordState(helper, frame).unlocked() && !frame.isRemoved(),
                "frame behind tall grass changed");

        // tolta l'erba (prima la meta' alta, poi la bassa) il telaio si trova: e' stata l'erba a nasconderlo
        helper.setBlock(upper, Blocks.AIR);
        helper.setBlock(lower, Blocks.AIR);
        helper.assertTrue(exoframeCommand(helper, owner, 0, "unlock") == 1 && recordState(helper, frame).unlocked(),
                "frame not unlocked without the tall grass");
        removeFrame(server, frame);
        helper.succeed();
    }

    /**
     * Con gli occhi dentro il riquadro di un telaio il comando prende quel telaio, come il clic (design §12.14), e non
     * uno piu' lontano sulla stessa linea. Due coppie con l'ordine di creazione invertito: oggi il risultato dipende
     * dall'ordine in cui il gioco restituisce le entita' ({@code getEntityHitResult} mette a zero la distanza di
     * un'entita' che contiene il punto di partenza e la fa sostituire da qualunque entita' colpita dopo).
     */
    private static void eyesInsideAFrameBoxPickThatFrame(GameTestHelper helper) {
        ServerPlayer owner = Compat.gameTests().mockServerPlayer(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ExoframeEntity a1 = placeFrame(helper, ownedBy(owner), new Vec3(2.5, 1.0, 5.5), 0f);
        ExoframeEntity b1 = placeFrame(helper, ownedBy(owner), new Vec3(2.5, 1.0, 2.5), 0f);
        ExoframeEntity b2 = placeFrame(helper, ownedBy(owner), new Vec3(5.5, 1.0, 2.5), 0f);
        ExoframeEntity a2 = placeFrame(helper, ownedBy(owner), new Vec3(5.5, 1.0, 5.5), 0f);
        assertEyesInsidePick(helper, owner, a1, b1, new Vec3(2.5, 1.0, 5.5));
        assertEyesInsidePick(helper, owner, a2, b2, new Vec3(5.5, 1.0, 5.5));
        for (ExoframeEntity frame : List.of(a1, b1, a2, b2)) {
            removeFrame(server, frame);
        }
        helper.succeed();
    }

    /**
     * Il giocatore in piedi nel telaio {@code a} guarda verso {@code b}: la mira e {@code unlock} prendono {@code a}.
     */
    private static void assertEyesInsidePick(GameTestHelper helper, ServerPlayer owner, ExoframeEntity a,
            ExoframeEntity b, Vec3 relativeFeet) {
        standFacingNorth(helper, owner, relativeFeet);
        helper.assertTrue(a.getBoundingBox().contains(owner.getEyePosition()), "eyes not inside the frame box");
        helper.assertTrue(FrameSight.inSight(owner).map(ExoframeEntity::frameId).equals(Optional.of(a.frameId())),
                "the sight did not pick the frame around the eyes");
        helper.assertTrue(exoframeCommand(helper, owner, 0, "unlock") == 1 && recordState(helper, a).unlocked()
                && !recordState(helper, b).unlocked(), "the command unlocked the wrong frame");
    }

    /**
     * Il passo fuori sale un piccolo gradino (design §12.14): da un sentiero (sommita' a 15/16) sulla pietra accanto,
     * un sedicesimo piu' in alto, e da un telaio sulla pietra a una lastra in basso, mezzo blocco piu' in alto, il
     * limite ammesso. Con il riquadro all'altezza del telaio il gradino lo urtava e il posto davanti era scartato.
     */
    private static void exitStepsUpASmallLip(GameTestHelper helper) {
        ServerPlayer onPath = Compat.gameTests().mockServerPlayer(helper);
        ServerPlayer onStone = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        // il sentiero prima del telaio: wearFrameAt mette la pietra sotto, in (2, 0, 2)
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.DIRT_PATH);
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.STONE);
        UUID pathFrame = wearFrameAt(helper, onPath, charged(ownedBy(onPath)), new Vec3(2.5, 1.9375, 2.5));
        helper.setBlock(new BlockPos(5, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 2, 3), Blocks.STONE_SLAB);
        UUID slabFrame = wearFrameAt(helper, onStone, charged(ownedBy(onStone)), new Vec3(5.5, 2.0, 2.5));

        assertStepsAheadTo(helper, onPath, helper.absoluteVec(new Vec3(2.5, 2.0, 3.5)), "a lip of 1/16");
        assertStepsAheadTo(helper, onStone, helper.absoluteVec(new Vec3(5.5, 2.5, 3.5)), "a half slab");
        removeFrame(server, (ExoframeEntity) level.getEntity(pathFrame));
        removeFrame(server, (ExoframeEntity) level.getEntity(slabFrame));
        helper.succeed();
    }

    /** Il giocatore, dentro il telaio, esce rivolto a +Z (yaw 0) e fa il passo davanti, con i piedi in {@code feet}. */
    private static void assertStepsAheadTo(GameTestHelper helper, ServerPlayer player, Vec3 feet, String step) {
        Vec3 spot = player.position();
        player.moveTo(spot.x, spot.y, spot.z, 0f, 0f);
        helper.assertTrue(FrameEntry.exit(player), "exit refused before " + step);
        helper.assertTrue(player.position().distanceTo(feet) < 1e-6, "player did not step up " + step + ": "
                + player.position() + ", expected " + feet);
    }

    /**
     * Il passo fuori in diagonale prova i percorsi asse per asse (design §12.14): rivolto a -45 gradi il posto davanti
     * sta oltre l'angolo della cella in cui una staccionata da sola e' un palo al centro del blocco; il percorso prima
     * lungo x urta il palo, quello prima lungo z no, e il giocatore va davanti. Con un solo riquadro attorno a tutto il
     * percorso il palo lo scartava e il giocatore andava a destra.
     */
    private static void exitTakesAnAxisPathAroundAPost(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        for (BlockPos floor : List.of(new BlockPos(3, 1, 3), new BlockPos(4, 1, 3), new BlockPos(3, 1, 4),
                new BlockPos(4, 1, 4))) {
            helper.setBlock(floor, Blocks.STONE);
        }
        BlockPos post = new BlockPos(4, 2, 3);
        helper.setBlock(post, Blocks.OAK_FENCE);
        BlockPos at = helper.absolutePos(post);
        AABB bounds = level.getBlockState(at).getCollisionShape(level, at).bounds();
        helper.assertTrue(bounds.minX >= 0.375 - 1e-9 && bounds.maxX <= 0.625 + 1e-9 && bounds.minZ >= 0.375 - 1e-9
                && bounds.maxZ <= 0.625 + 1e-9, "premise: the fence is not a lone post: " + bounds);
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(3.5, 2.0, 3.5));
        Vec3 spot = player.position();
        player.moveTo(spot.x, spot.y, spot.z, -45f, 0f);
        double r = Math.toRadians(-45);
        Vec3 ahead = spot.add(-Math.sin(r), 0.0, Math.cos(r));
        assertRoomToStand(helper, ahead);

        helper.assertTrue(FrameEntry.exit(player), "exit refused");

        helper.assertTrue(player.position().distanceTo(ahead) < 1e-6, "player did not step ahead around the post: "
                + player.position() + ", expected " + ahead);
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }

    /**
     * Il passo fuori evita un falo', un blocco di magma e un cespuglio di bacche dolci davanti (design §12.14): ogni
     * volta il giocatore va a sinistra, sulla pietra. Il falo' e' a terra, con la sommita' a 7/16: il passo ci
     * atterrerebbe sopra; il cespuglio non ha collisione, quindi il posto davanti c'e' e lo scarta il pericolo.
     */
    private static void exitAvoidsCampfireMagmaAndBerries(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        // giocatore in (3.5, 2, 3.5) rivolto a sud (+Z): davanti (3, 2, 4), sinistra (4, 2, 3)
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(3.5, 2.0, 3.5));
        Vec3 spot = player.position();
        BlockPos ahead = new BlockPos(3, 1, 4);

        helper.setBlock(new BlockPos(3, 0, 4), Blocks.STONE);
        helper.setBlock(ahead, Blocks.CAMPFIRE);
        assertExitStepsLeft(helper, player, frameId, spot, "a campfire");
        helper.setBlock(ahead, Blocks.MAGMA_BLOCK);
        assertExitStepsLeft(helper, player, frameId, spot, "a magma block");
        helper.setBlock(ahead, Blocks.DIRT);
        helper.setBlock(ahead.above(), Blocks.SWEET_BERRY_BUSH);
        assertRoomToStand(helper, spot.add(0.0, 0.0, 1.0));
        assertExitStepsLeft(helper, player, frameId, spot, "a sweet berry bush");
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }

    /**
     * Il passo fuori evita una rosa del Wither, la neve polverosa e un cactus davanti (design §12.14): ogni volta il
     * giocatore va a sinistra. La rosa e la neve polverosa non hanno collisione per il calcolo del pavimento, quindi il
     * posto c'e' e lo scarta il pericolo; sul cactus il passo atterrerebbe, a 15/16. Il cactus accanto alla pietra del
     * telaio si romperebbe al tick dopo: si mette per ultimo e si toglie nello stesso tick, con la sabbia sotto.
     */
    private static void exitAvoidsWitherRosePowderSnowAndCactus(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(3.5, 2.0, 3.5));
        Vec3 spot = player.position();
        BlockPos below = new BlockPos(3, 1, 4);
        BlockPos ahead = below.above();

        helper.setBlock(below, Blocks.DIRT);
        helper.setBlock(ahead, Blocks.WITHER_ROSE);
        assertRoomToStand(helper, spot.add(0.0, 0.0, 1.0));
        assertExitStepsLeft(helper, player, frameId, spot, "a wither rose");
        helper.setBlock(ahead, Blocks.AIR);
        helper.setBlock(below, Blocks.STONE);
        helper.setBlock(ahead, Blocks.POWDER_SNOW);
        assertExitStepsLeft(helper, player, frameId, spot, "powder snow");
        helper.setBlock(ahead, Blocks.AIR);
        helper.setBlock(below.below(), Blocks.SAND);
        helper.setBlock(below, Blocks.CACTUS);
        assertExitStepsLeft(helper, player, frameId, spot, "a cactus");
        // il cactus e poi la sabbia (che non cade: il suo tick trova un altro blocco), nello stesso tick
        helper.setBlock(below, Blocks.AIR);
        helper.setBlock(below.below(), Blocks.AIR);
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }

    /**
     * Il passo fuori evita i portali (design §12.14): un portale dell'End in una fossa davanti (il passo ci cadrebbe
     * dentro, sulla pietra sotto) e un portale del Nether sulla pietra davanti. Ogni volta il giocatore va a sinistra;
     * i due portali si mettono per ultimi e prima della fine i due posti tornano aria.
     */
    private static void exitAvoidsAPortalBelow(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(3.5, 2.0, 3.5));
        Vec3 spot = player.position();
        BlockPos below = new BlockPos(3, 1, 4);
        BlockPos ahead = below.above();

        helper.setBlock(below.below(), Blocks.STONE);
        helper.setBlock(below, Blocks.END_PORTAL);
        BlockPos portal = helper.absolutePos(below);
        helper.assertTrue(level.getBlockState(portal).getCollisionShape(level, portal).isEmpty(),
                "premise: the end portal has a collision shape");
        assertExitStepsLeft(helper, player, frameId, spot, "an end portal");
        helper.setBlock(below, Blocks.STONE);
        helper.setBlock(ahead, Blocks.NETHER_PORTAL);
        assertExitStepsLeft(helper, player, frameId, spot, "a nether portal");
        helper.setBlock(ahead, Blocks.AIR);
        helper.setBlock(below, Blocks.AIR);
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }

    /**
     * Il passo fuori non scende in una fossa davanti dove c'e' una barca (design §12.14): la barca, un ostacolo per il
     * gioco, sta nel riquadro del giocatore in fondo alla fossa; il giocatore va a sinistra. Il posto in fondo alla
     * fossa si controlla prima di creare la barca, che lo occuperebbe.
     */
    private static void exitAvoidsABoatBelowTheSpot(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(3.5, 2.0, 3.5));
        Vec3 spot = player.position();
        helper.setBlock(new BlockPos(3, 0, 4), Blocks.STONE);
        Vec3 pit = helper.absoluteVec(new Vec3(3.5, 1.0, 4.5));
        assertRoomToStand(helper, pit);
        Boat boat = new Boat(level, pit.x, pit.y, pit.z);
        level.addFreshEntity(boat);

        assertExitStepsLeft(helper, player, frameId, spot, "a boat");

        boat.discard();
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }

    /**
     * Il passo fuori non scende in una fossa davanti dove c'e' un carrello su un binario (design §12.14): il carrello
     * non e' un ostacolo per il gioco, quindi lo si cerca a parte; il giocatore va a sinistra.
     */
    private static void exitAvoidsAMinecartBelowTheSpot(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        helper.setBlock(new BlockPos(4, 1, 3), Blocks.STONE);
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(3.5, 2.0, 3.5));
        Vec3 spot = player.position();
        helper.setBlock(new BlockPos(3, 0, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 4), Blocks.RAIL);
        Vec3 pit = helper.absoluteVec(new Vec3(3.5, 1.0, 4.5));
        assertRoomToStand(helper, pit);
        Minecart cart = new Minecart(level, pit.x, pit.y + 0.0625, pit.z);
        level.addFreshEntity(cart);

        assertExitStepsLeft(helper, player, frameId, spot, "a minecart");

        cart.discard();
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }

    /**
     * L'uscita voluta si rifiuta quando nel punto di riposo c'e' gia' un altro telaio piazzato (design §12.14), con un
     * messaggio e senza toccare nulla: il record resta indossato, i segnaposto restano, l'elmo stivato resta nel record
     * e il giocatore resta dov'e'. Tolto l'altro telaio, l'uscita riesce e l'elmo torna in testa.
     */
    private static void exitRefusedOverAnotherFrame(GameTestHelper helper) {
        ServerPlayer player = Compat.gameTests().mockServerPlayer(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        UUID frameId = wearFrameAt(helper, player, charged(ownedBy(player)), new Vec3(3.5, 2.0, 3.5));
        Vec3 spot = player.position();
        ExoframeEntity other = placeFrame(helper, new FrameState(RedstoneExoframes.config()),
                new Vec3(3.5, 2.0, 3.5), 0f);

        helper.assertTrue(!FrameEntry.exit(player), "exit over another frame accepted");
        helper.assertTrue(frames.wornFrameId(player).equals(Optional.of(frameId)), "worn record changed");
        assertPlaceholdersWorn(helper, player);
        helper.assertTrue(countInMainInventory(player, Items.IRON_HELMET) == 0, "stowed helmet handed back");
        helper.assertTrue(frames.placed(server, frameId).isEmpty() && player.position().equals(spot),
                "refused exit placed the frame or moved the player to " + player.position());
        helper.assertTrue(messages(player, FrameEntry.FRAME_TAKEN) == 1, "not told why");
        removeFrame(server, other);
        helper.assertTrue(FrameEntry.exit(player), "exit refused once the spot is free");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "stowed helmet lost");
        removeFrame(server, (ExoframeEntity) level.getEntity(frameId));
        helper.succeed();
    }
}
