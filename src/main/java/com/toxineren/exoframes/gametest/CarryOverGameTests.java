package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.GameTests;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.DebugWear;
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
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
}
