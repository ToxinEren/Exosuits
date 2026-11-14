package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.compat.StowedStack;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.content.ExoframeItem;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.entity.ExoframeEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * L'ingresso in un telaio piazzato (design §5.5): il record passa da piazzato a indossato, l'armatura del giocatore si
 * stiva nel record, i quattro segnaposto vanno negli slot armatura e l'entita' sparisce, tutto nello stesso tick. Il
 * registro e' la verita' (§4.5): ogni effetto sul giocatore e sul mondo segue l'esito di
 * {@link FrameStateAccess#enter}, e nulla (telaio, armatura, segnaposto) si perde o si duplica.
 */
public final class FrameEntry {
    /** Messaggio sulla barra delle azioni quando il telaio non ha una cella con carica. */
    public static final String NO_CELL = "message." + RedstoneExoframes.MOD_ID + ".no_cell";
    /** Messaggio sulla barra delle azioni a chi e' gia' dentro un telaio. */
    public static final String ALREADY_INSIDE = "message." + RedstoneExoframes.MOD_ID + ".already_inside";
    /** Messaggio sulla barra delle azioni a chi non e' il proprietario di un telaio che contiene ancora armatura. */
    public static final String HOLDS_ARMOR = "message." + RedstoneExoframes.MOD_ID + ".holds_armor";

    private FrameEntry() {
    }

    /**
     * Mette il giocatore dentro il telaio, sul server. Rifiuti, in quest'ordine, ciascuno con il suo messaggio e
     * senza toccare nulla: senza permesso ({@link FrameInteractions#LOCKED}); senza una cella con carica
     * ({@link #NO_CELL}); il giocatore indossa gia' un telaio ({@link #ALREADY_INSIDE}); il riquadro della tuta in
     * piedi nel punto del telaio non e' libero ({@link ExoframeItem#NO_ROOM}); il telaio contiene ancora armatura
     * stivata che si legge (lasciata da una morte) e il giocatore non ne e' il proprietario, o il telaio non ne ha uno
     * ({@link #HOLDS_ARMOR}): quell'armatura esce solo con un atto voluto, il clic accovacciato di chi puo' toccare il
     * telaio, e non resta mai a terra a sparire. Se il record non e' piu' piazzato o sta in un'altra dimensione, o il
     * giocatore e' morto o spettatore, non succede nulla e non si dice nulla.
     * <p>
     * Poi, nello stesso tick: l'armatura stivata che si legge va al proprietario che entra ({@link Handouts#give});
     * {@link FrameStateAccess#enter}, e se rifiuta ci si ferma (cio' che e' gia' stato consegnato resta consegnato);
     * l'armatura indossata che non e' un segnaposto si stiva nel record con il suo slot e solo dopo lascia gli slot;
     * {@link #dress} con il riassunto dello stato; il giocatore scende da cio' che cavalca e va nel punto del telaio
     * con il suo yaw (il pitch resta il suo); l'entita' si elimina; le dimensioni del giocatore si ricalcolano subito.
     * Gli slot armatura cambiano in silenzio ({@code setArmorSlot}).
     *
     * @return true se il giocatore e' entrato
     */
    public static boolean enter(ServerPlayer player, ExoframeEntity frame) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(frame, "frame must not be null");
        if (!player.isAlive() || player.isSpectator() || !(frame.level() instanceof ServerLevel level)) {
            return false;
        }
        MinecraftServer server = level.getServer();
        FrameStateAccess frames = Compat.frameState();
        Optional<PlacedFrame> placed = frames.placed(server, frame.frameId());
        if (placed.isEmpty()) {
            return false;
        }
        PlacedFrame record = placed.get();
        if (!record.dimension().equals(level.dimension())) {
            return false;
        }
        if (!FramePermissions.canUse(player, record.state())) {
            FrameInteractions.tell(player, FrameInteractions.LOCKED);
            return false;
        }
        EnergyCell cell = record.state().cell();
        if (cell == null || cell.isEmpty()) {
            FrameInteractions.tell(player, NO_CELL);
            return false;
        }
        if (frames.wornFrameId(player).isPresent()) {
            FrameInteractions.tell(player, ALREADY_INSIDE);
            return false;
        }
        Vec3 spot = record.position();
        if (!level.noCollision(frame, SuitSize.standingBox(spot))) {
            FrameInteractions.tell(player, ExoframeItem.NO_ROOM);
            return false;
        }
        if (record.hasStowed() && !player.getUUID().equals(record.state().owner())) {
            FrameInteractions.tell(player, HOLDS_ARMOR);
            return false;
        }

        if (record.hasStowed()) {
            for (StowedStack entry : frames.takeStowedOfPlaced(server, record.frameId())) {
                Handouts.give(player, entry.stack());
            }
        }
        if (!frames.enter(player, record.frameId())) {
            return false;
        }
        stowWornArmor(player, frames);
        dress(player, SuitSummary.of(record.state()));
        player.stopRiding();
        player.teleportTo(level, spot.x, spot.y, spot.z, record.yaw(), player.getXRot());
        frame.discard();
        player.refreshDimensions();
        return true;
    }

    /**
     * La parte comune della vestizione: i quattro segnaposto negli slot armatura e {@code summary} sullo stack del
     * torso. Non cancella mai un oggetto vero: cio' che trova ancora in uno slot armatura e non e' un segnaposto va al
     * giocatore con {@link Handouts#give}; i segnaposto gia' indossati si sostituiscono. Gli slot cambiano in
     * silenzio ({@code setArmorSlot}). Non ricalcola le dimensioni: lo fa chi chiama, alla fine.
     */
    public static void dress(ServerPlayer player, SuitSummary summary) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        List<ItemStack> displaced = new ArrayList<>();
        for (EquipmentSlot slot : DebugWear.ARMOR_SLOTS) {
            ItemStack old = player.getItemBySlot(slot);
            if (!old.isEmpty() && !DebugWear.isPlaceholder(old)) {
                displaced.add(old);
            }
            ItemStack placeholder = new ItemStack(DebugWear.PLACEHOLDERS.get(slot).get());
            if (slot == EquipmentSlot.CHEST) {
                Compat.itemData().setSuitSummary(placeholder, summary);
            }
            setArmorSlot(player, slot, placeholder);
        }
        for (ItemStack stack : displaced) {
            Handouts.give(player, stack);
        }
    }

    /**
     * Stiva nel record indossato l'armatura del giocatore (ogni slot armatura non vuoto che non ha un segnaposto) con
     * il suo slot, e solo dopo la toglie dagli slot. Va chiamato dopo un {@link FrameStateAccess#enter} riuscito.
     */
    private static void stowWornArmor(ServerPlayer player, FrameStateAccess frames) {
        List<StowedStack> worn = new ArrayList<>();
        for (EquipmentSlot slot : DebugWear.ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && !DebugWear.isPlaceholder(stack)) {
                worn.add(new StowedStack(slot, stack));
            }
        }
        if (worn.isEmpty()) {
            return;
        }
        frames.stow(player, worn);
        for (StowedStack entry : worn) {
            setArmorSlot(player, entry.slot(), ItemStack.EMPTY);
        }
    }

    /**
     * Scrive uno slot armatura in silenzio: direttamente nella lista {@code armor} dell'inventario, non con
     * {@code setItemSlot}, che chiama {@code LivingEntity.onEquipItem} (suono dell'equipaggiamento ed evento di gioco
     * {@code EQUIP}, che sveglia i sensori sculk) su 1.20.1 e 1.21.1. Il resto segue da solo come per ogni cambio
     * dell'inventario: al tick dopo {@code LivingEntity.detectEquipmentUpdates} confronta gli slot con l'ultimo
     * valore visto, aggiorna i modificatori degli attributi e manda l'equipaggiamento a chi vede il giocatore, e il
     * menu dell'inventario manda lo slot al giocatore stesso.
     */
    private static void setArmorSlot(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        player.getInventory().armor.set(slot.getIndex(), stack);
        player.getInventory().setChanged();
    }
}
