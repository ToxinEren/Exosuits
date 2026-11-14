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
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * L'ingresso in un telaio piazzato e l'uscita (design §5.5). Entrando il record passa da piazzato a indossato,
 * l'armatura del giocatore si stiva nel record, i quattro segnaposto vanno negli slot armatura e l'entita' sparisce;
 * uscendo i segnaposto se ne vanno, l'armatura torna al giocatore, il record torna piazzato ai suoi piedi e l'entita'
 * rinasce; ogni volta tutto nello stesso tick. Alla morte ({@link #onDeath}) il telaio resta nel mondo con l'armatura
 * stivata dentro. Il registro e' la verita' (§4.5): ogni effetto sul giocatore e sul mondo segue l'esito di
 * {@link FrameStateAccess#enter} e {@link FrameStateAccess#exit}, e nulla (telaio, armatura, segnaposto) si perde o
 * si duplica. Qui sta anche il pacchetto C2S dell'uscita ({@link #registerNet()}).
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
     * Toglie il giocatore dal telaio che indossa, sul server (design, par. 5.5), tutto nello stesso tick: e' l'uscita
     * voluta (il tasto). Se non ne indossa uno, o non e' vivo (un pacchetto del tasto arrivato dopo la morte: la
     * morte la gestisce solo {@link #onDeath}), non tocca nulla e da' falso. Altrimenti, in quest'ordine:
     * <ol>
     * <li>tutti i segnaposto lasciano il giocatore, in silenzio: slot armatura, inventario principale, mano secondaria
     * ({@link DebugWear#remove}) e lo stack tenuto sul cursore; uno stack vero in uno slot armatura resta dov'e';</li>
     * <li>l'armatura stivata esce dal record ({@link FrameStateAccess#takeStowed}) e ogni stack torna nel suo slot se e'
     * vuoto, in silenzio, altrimenti va al giocatore con {@link Handouts#give}: cio' che esce dal record gli e' gia'
     * restituito prima che succeda altro;</li>
     * <li>il record diventa piazzato nel punto di riposo del telaio (vedi {@link #restingSpot}), con il yaw del
     * giocatore riportato a [-180, 180) come per l'oggetto telaio ({@link FrameStateAccess#exit});</li>
     * <li>l'entita' del record piazzato ({@link FramePlacement#spawnView}), se il chunk del punto ha le entita'
     * caricate (altrimenti la crea il riallineamento quando si carica), e le dimensioni del giocatore ricalcolate
     * subito.</li>
     * </ol>
     * Il giocatore resta dov'e'. Se {@link FrameStateAccess#exit} rifiuta (non dovrebbe: il giocatore indossa un
     * telaio) non succede altro, il giocatore tiene cio' che ha riavuto, e il risultato e' falso.
     *
     * @return true se il giocatore e' uscito e il telaio e' piazzato
     */
    public static boolean exit(ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        FrameStateAccess frames = Compat.frameState();
        if (!player.isAlive() || frames.wornFrameId(player).isEmpty()) {
            return false;
        }
        removePlaceholders(player);
        for (StowedStack entry : frames.takeStowed(player)) {
            EquipmentSlot slot = entry.slot();
            if (DebugWear.ARMOR_SLOTS.contains(slot) && player.getItemBySlot(slot).isEmpty()) {
                setArmorSlot(player, slot, entry.stack());
            } else {
                Handouts.give(player, entry.stack());
            }
        }
        return leaveFrame(player, frames);
    }

    /**
     * La morte di chi indossa un telaio (design, par. 5.5), su {@code EntityEvent.LIVING_DEATH}: l'evento arriva in
     * testa a {@code ServerPlayer.die}, prima che il gioco faccia cadere o tenga l'inventario. Se il giocatore indossa
     * un telaio: i segnaposto lo lasciano come in {@link #exit} (cosi' non cadono a terra e, con
     * {@code keepInventory}, non passano al giocatore rinato); l'armatura stivata resta nel record; il record diventa
     * piazzato nel punto di riposo del telaio ({@link #restingSpot}) e l'entita' nasce li' se il chunk ha le entita'
     * caricate. Non annulla mai la morte e non lancia: un errore si scrive nel log e la morte prosegue.
     */
    public static void onDeath(ServerPlayer player) {
        try {
            FrameStateAccess frames = Compat.frameState();
            if (frames.wornFrameId(player).isEmpty()) {
                return;
            }
            removePlaceholders(player);
            leaveFrame(player, frames);
        } catch (RuntimeException e) {
            RedstoneExoframes.LOGGER.error("Could not leave the frame of player {} at death", player.getUUID(), e);
        }
    }

    /**
     * Registra il pacchetto di uscita ({@link FrameExitPacket}) e il suo gestore, che chiama {@link #exit} per chi lo
     * manda: a chi non indossa un telaio non succede nulla e non si dice nulla. Una volta, nell'init comune, su
     * entrambi i lati.
     */
    public static void registerNet() {
        Compat.net().registerC2S(FrameExitPacket.ID, FrameExitPacket.class, FrameExitPacket.CODEC,
                (packet, player) -> exit(player));
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

    /** Tutti i segnaposto lasciano il giocatore, in silenzio: l'inventario ({@link DebugWear#remove}) e il cursore. */
    private static void removePlaceholders(ServerPlayer player) {
        DebugWear.remove(player);
        if (DebugWear.isPlaceholder(player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    /**
     * La parte comune dell'uscita e della morte: il record indossato diventa piazzato nel punto di riposo, con il yaw
     * del giocatore; l'entita' nasce se il chunk del punto ha le entita' caricate; le dimensioni si ricalcolano.
     *
     * @return false se il registro ha rifiutato l'uscita
     */
    private static boolean leaveFrame(ServerPlayer player, FrameStateAccess frames) {
        RestingSpot spot = restingSpot(player);
        Optional<UUID> frameId = frames.exit(player, spot.level(), spot.position(), Mth.wrapDegrees(player.getYRot()));
        if (frameId.isEmpty()) {
            return false;
        }
        PlacedFrame record = frames.placed(player.server, frameId.get()).orElseThrow(
                () -> new IllegalStateException("placed frame " + frameId.get() + " not found after exit"));
        ServerLevel level = spot.level();
        ChunkPos chunk = new ChunkPos(BlockPos.containing(spot.position()));
        // come nel riallineamento: entita' caricate e chunk pieno, senza caricare ne' generare nulla; altrove basta il
        // record, e l'entita' la crea il riallineamento quando il chunk si carica
        if (level.areEntitiesLoaded(chunk.toLong()) && level.getChunkSource().hasChunk(chunk.x, chunk.z)) {
            FramePlacement.spawnView(level, record);
        }
        player.refreshDimensions();
        return true;
    }

    /**
     * Dove va il telaio che il giocatore lascia (design, par. 5.4), uguale per l'uscita e per la morte: il suolo sotto
     * la posizione del giocatore ({@link FramePlacement#groundBelow}); se sotto non c'e' suolo (o il punto e' sotto il
     * limite inferiore del mondo), l'ultima posizione sicura ({@link SafeSpots#last}), in qualunque dimensione sia; in
     * mancanza anche di quella, il punto di rinascita del mondo (quello condiviso dell'overworld), appoggiato al suolo
     * sotto se c'e'. Non carica chunk ne' dimensioni: {@code getLevel} da' solo livelli gia' caricati e
     * {@code groundBelow} non carica chunk.
     */
    private static RestingSpot restingSpot(ServerPlayer player) {
        ServerLevel here = player.serverLevel();
        Optional<Vec3> ground = FramePlacement.groundBelow(here, player.position());
        if (ground.isPresent()) {
            return new RestingSpot(here, ground.get());
        }
        MinecraftServer server = here.getServer();
        Optional<SafeSpots.Spot> safe = SafeSpots.last(player);
        if (safe.isPresent()) {
            ServerLevel level = server.getLevel(safe.get().dimension());
            if (level != null) {
                return new RestingSpot(level, safe.get().position());
            }
        }
        ServerLevel overworld = server.overworld();
        Vec3 spawn = Vec3.atBottomCenterOf(overworld.getSharedSpawnPos());
        return new RestingSpot(overworld, FramePlacement.groundBelow(overworld, spawn).orElse(spawn));
    }

    /** Il livello e il punto dei piedi in cui il telaio lasciato si piazza. */
    private record RestingSpot(ServerLevel level, Vec3 position) {
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
