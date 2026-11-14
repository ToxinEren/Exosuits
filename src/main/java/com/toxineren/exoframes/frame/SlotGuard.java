package com.toxineren.exoframes.frame;

import com.google.common.collect.MapMaker;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.core.frame.FrameState;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Slot armatura bloccati per autoriparazione (design, par. 4.3 e 5.5): il registro e' la verita' (par. 4.5) e i
 * segnaposto ne sono l'ombra. A ogni tick del server, per ogni giocatore: con un record indossato i quattro segnaposto
 * stanno nei loro slot armatura e in nessun altro posto, e il torso porta il riassunto dello stato; senza record non
 * c'e' nessun segnaposto, da nessuna parte. Cosi' valgono anche il rientro nel gioco (stato senza segnaposto: si
 * rimettono; segnaposto senza stato: si tolgono) e qualunque cosa abbiano fatto il giocatore o altre mod. Vale in ogni
 * modalita' di gioco. Difetto noto: cliccando uno slot armatura il segnaposto lampeggia sul cursore per un tick.
 * <p>
 * Per un giocatore con tuta e slot in ordine il tick non decodifica dati degli stack e non scrive nulla: ricorda, per
 * giocatore, gli stack che ha visto o scritto negli slot armatura e il riassunto del torso, e li confronta per
 * identita'. Per chi non indossa un telaio (quasi tutti, quasi sempre) il tick guarda solo i quattro slot armatura, e
 * il resto (inventario, cursore, griglia, menu aperto) una volta ogni {@link #FULL_SCAN_TICKS} tick del giocatore: un
 * segnaposto finito li' senza telaio (da {@code /give}, dopo un rientro) se ne va entro un secondo. Solo sul thread
 * del server.
 */
public final class SlotGuard {
    /** Ogni quanti tick del giocatore, senza un telaio indossato, si guarda tutto l'inventario: un secondo. */
    public static final int FULL_SCAN_TICKS = 20;

    /**
     * Cio' che il tick ha lasciato negli slot armatura del giocatore con un record indossato. Chiavi deboli e
     * confrontate per identita': nulla sopravvive al giocatore (il giocatore rinato e' un oggetto nuovo).
     */
    private static final Map<ServerPlayer, Dressed> DRESSED = new MapMaker().weakKeys().makeMap();

    private SlotGuard() {
    }

    /**
     * Registra il tick dei giocatori sul server ({@code TickEvent.PLAYER_POST}) e il lancio di un oggetto
     * ({@code PlayerEvent.DROP_ITEM}), che per un segnaposto si annulla ({@link #onDrop}). Una volta, nell'init comune.
     */
    public static void register() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer);
            }
        });
        PlayerEvent.DROP_ITEM.register(SlotGuard::onDrop);
    }

    /**
     * Riallinea i segnaposto del giocatore al registro ({@link Compat#frameState()}).
     * <ul>
     * <li>Con un record indossato: ogni slot armatura ha il suo segnaposto; uno mancante si rimette, un segnaposto
     * sbagliato si elimina e si sostituisce, un oggetto vero trovato li' va al giocatore con {@link Handouts#give}. Il
     * riassunto sul torso e' quello dello stato, e si riscrive solo se e' diverso. Le copie dei segnaposto altrove
     * (inventario, mano secondaria, cursore, griglia di fabbricazione, menu aperto) si eliminano.</li>
     * <li>Senza record indossato: i segnaposto negli slot armatura si eliminano a ogni tick
     * ({@link Placeholders#removeFromArmor}), quelli in ogni altro posto quando il contatore dei tick del giocatore e'
     * un multiplo di {@link #FULL_SCAN_TICKS} ({@link Placeholders#removeAll}).</li>
     * </ul>
     * Gli slot armatura cambiano in silenzio; se sono cambiati, le dimensioni del giocatore si ricalcolano subito.
     */
    public static void tick(ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        Optional<FrameState> state = Compat.frameState().get(player);
        if (state.isEmpty()) {
            DRESSED.remove(player);
            int removed = player.tickCount % FULL_SCAN_TICKS == 0 ? Placeholders.removeAll(player)
                    : Placeholders.removeFromArmor(player);
            if (removed > 0) {
                player.refreshDimensions();
            }
            return;
        }
        SuitSummary summary = SuitSummary.of(state.get());
        Dressed last = DRESSED.get(player);
        ItemStack[] seen = new ItemStack[Placeholders.ARMOR_SLOTS.size()];
        List<ItemStack> displaced = new ArrayList<>();
        boolean changed = false;
        for (int i = 0; i < seen.length; i++) {
            EquipmentSlot slot = Placeholders.ARMOR_SLOTS.get(i);
            ItemStack current = player.getInventory().armor.get(slot.getIndex());
            if (Placeholders.isPlaceholderOf(slot, current)) {
                // il riassunto si legge dal torso solo se lo stack non e' quello gia' visto dal tick
                boolean summaryOk = slot != EquipmentSlot.CHEST || summary.equals(
                        last != null && current == last.stacks[i] ? last.summary
                                : Compat.itemData().suitSummary(current).orElse(null));
                if (summaryOk) {
                    seen[i] = current;
                    continue;
                }
            }
            if (!current.isEmpty() && !Placeholders.isPlaceholder(current)) {
                displaced.add(current);
            }
            seen[i] = Placeholders.create(slot, summary);
            Placeholders.setArmorSlot(player, slot, seen[i]);
            changed = true;
        }
        if (last == null || changed || !summary.equals(last.summary) || !Arrays.equals(seen, last.stacks)) {
            DRESSED.put(player, new Dressed(seen, summary));
        }
        Placeholders.removeStrays(player);
        for (ItemStack stack : displaced) {
            Handouts.give(player, stack);
        }
        if (changed) {
            player.refreshDimensions();
        }
    }

    /**
     * Il lancio di un segnaposto, su {@code PlayerEvent.DROP_ITEM} (lato server): si annulla e il segnaposto non
     * torna, ne' a terra ne' come seconda copia; se veniva da uno slot armatura, lo rimette il tick. L'evento arriva
     * quando il gioco ha gia' tolto lo stack da dove stava e ha creato l'oggetto a terra, e il suo esito vale solo su
     * Fabric (che allora non aggiunge l'oggetto al livello); su Forge e NeoForge Architectury non annulla
     * {@code ItemTossEvent} e l'oggetto si aggiunge comunque. Percio' l'oggetto a terra resta senza stack: su Fabric
     * non nasce, su Forge e NeoForge nasce vuoto e si elimina da solo al suo primo tick.
     */
    private static EventResult onDrop(Player player, ItemEntity entity) {
        if (player.level().isClientSide() || !Placeholders.isPlaceholder(entity.getItem())) {
            return EventResult.pass();
        }
        entity.setItem(ItemStack.EMPTY);
        return EventResult.interruptFalse();
    }

    /** Gli stack lasciati negli slot armatura (nell'ordine di {@link Placeholders#ARMOR_SLOTS}) e il riassunto. */
    private record Dressed(ItemStack[] stacks, SuitSummary summary) {
    }
}
