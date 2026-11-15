package com.toxineren.exoframes.gametest;

import static com.toxineren.exoframes.gametest.GameTestKit.*;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.frame.DebugWear;
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
}
