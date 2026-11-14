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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * L'ingresso in un telaio piazzato e l'uscita (design §5.5). Entrando il record passa da piazzato a indossato,
 * l'armatura del giocatore si stiva nel record, i quattro segnaposto vanno negli slot armatura e l'entita' sparisce;
 * uscendo i segnaposto se ne vanno, l'armatura stivata torna al giocatore, il record torna piazzato ai suoi piedi,
 * l'entita' rinasce e il giocatore fa un passo fuori dal telaio; ogni volta tutto nello stesso tick. Alla morte
 * ({@link #onDeath}) il telaio resta nel mondo con l'armatura stivata dentro. Il registro e' la verita' (§4.5): ogni
 * effetto sul giocatore e sul mondo segue l'esito di {@link FrameStateAccess#enter} e {@link FrameStateAccess#exit}, e
 * nulla (telaio, armatura, segnaposto) si perde o si duplica. Qui sta anche il pacchetto C2S dell'uscita ({@link #registerNet()}).
 */
public final class FrameEntry {
    /** Messaggio sulla barra delle azioni quando il telaio non ha una cella con carica. */
    public static final String NO_CELL = "message." + RedstoneExoframes.MOD_ID + ".no_cell";
    /** Messaggio sulla barra delle azioni a chi e' gia' dentro un telaio. */
    public static final String ALREADY_INSIDE = "message." + RedstoneExoframes.MOD_ID + ".already_inside";
    /** Messaggio sulla barra delle azioni a chi non e' il proprietario di un telaio che contiene ancora armatura. */
    public static final String HOLDS_ARMOR = "message." + RedstoneExoframes.MOD_ID + ".holds_armor";
    /**
     * Messaggio in chat a chi lascia il telaio (uscendo o morendo) quando il telaio non resta sotto di lui ma va
     * all'ultima posizione sicura o al punto di rinascita del mondo: coordinate x, y, z e dimensione.
     */
    public static final String ELSEWHERE = "message." + RedstoneExoframes.MOD_ID + ".frame_elsewhere";

    /** Distanza in blocchi dal centro del telaio del posto in cui il giocatore fa il passo fuori. */
    private static final double STEP = 1.0;
    /** Meta' larghezza e altezza del giocatore vanilla in piedi, per il posto del passo fuori. */
    private static final double PLAYER_HALF_WIDTH = 0.3;
    private static final double PLAYER_HEIGHT = 1.8;
    /** Margine sul blocco di caduta concesso fino al pavimento del passo fuori. */
    private static final double FLOOR_EPSILON = 1.0e-3;

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
     * Poi, nello stesso tick: {@link FrameStateAccess#enter}, e se rifiuta ci si ferma; l'armatura gia' stivata nel
     * telaio (quella lasciata dal proprietario morendo) resta nel record e torna all'uscita come il resto, quindi
     * entrare non consegna nulla e nulla cade a terra; l'armatura indossata che non e' un segnaposto si stiva nel record con il
     * suo slot (accanto a quella gia' stivata: due voci per lo stesso slot vanno bene) e solo dopo lascia gli slot;
     * {@link #dress} con il riassunto dello stato; il giocatore scende da cio' che cavalca e va nel punto del telaio
     * con il suo yaw (il pitch resta il suo); l'entita' si elimina; le dimensioni del giocatore si ricalcolano subito.
     * Gli slot armatura cambiano in silenzio ({@link Placeholders#setArmorSlot}). Il punto del telaio diventa la prima
     * posizione sicura del giocatore ({@link SafeSpots}).
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

        if (!frames.enter(player, record.frameId())) {
            return false;
        }
        stowWornArmor(player, frames);
        dress(player, SuitSummary.of(record.state()));
        player.stopRiding();
        player.teleportTo(level, spot.x, spot.y, spot.z, record.yaw(), player.getXRot());
        frame.discard();
        SafeSpots.remember(player, level.dimension(), spot);
        player.refreshDimensions();
        FrameLog.event(record.frameId(), "entered", player, level, spot);
        return true;
    }

    /**
     * Toglie il giocatore dal telaio che indossa, sul server (design, par. 5.5), tutto nello stesso tick: e' l'uscita
     * voluta (il tasto). Se non ne indossa uno, non e' vivo (un pacchetto del tasto arrivato dopo la morte: la morte la
     * gestisce solo {@link #onDeath}) o e' spettatore, non tocca nulla e da' falso. Altrimenti, in quest'ordine:
     * <ol>
     * <li>tutti i segnaposto lasciano il giocatore, in silenzio, da ogni posto in cui possono stare
     * ({@link Placeholders#removeAll}); uno stack vero in uno slot armatura resta dov'e';</li>
     * <li>l'armatura stivata torna al giocatore ({@link #returnStowed}): cio' che esce dal record gli e' gia'
     * restituito prima che succeda altro;</li>
     * <li>il record diventa piazzato nel punto di riposo del telaio (vedi {@link #restingSpot}), con il yaw del
     * giocatore riportato a [-180, 180) come per l'oggetto telaio ({@link FrameStateAccess#exit});</li>
     * <li>l'entita' del record piazzato ({@link FramePlacement#spawnView}), se il chunk del punto ha le entita'
     * caricate (altrimenti la crea il riallineamento quando si carica), e le dimensioni del giocatore ricalcolate
     * subito;</li>
     * <li>se gli occhi del giocatore stanno nel riquadro del telaio appena lasciato, il giocatore fa un passo fuori
     * ({@link #stepOut}); altrimenti, e se non c'e' un posto libero accanto, resta dov'e'.</li>
     * </ol>
     * Se {@link FrameStateAccess#exit} rifiuta (non dovrebbe: il giocatore indossa un telaio) non succede altro, il
     * giocatore tiene cio' che ha riavuto, e il risultato e' falso.
     *
     * @return true se il giocatore e' uscito e il telaio e' piazzato
     */
    public static boolean exit(ServerPlayer player) {
        Objects.requireNonNull(player, "player must not be null");
        FrameStateAccess frames = Compat.frameState();
        if (!player.isAlive() || player.isSpectator() || frames.wornFrameId(player).isEmpty()) {
            return false;
        }
        Placeholders.removeAll(player);
        returnStowed(player, frames);
        Optional<RestingSpot> left = leaveFrame(player, frames, "left");
        left.ifPresent(spot -> stepOut(player, spot));
        return left.isPresent();
    }

    /**
     * La morte di chi indossa un telaio (design, par. 5.5), su {@code EntityEvent.LIVING_DEATH}: l'evento arriva in
     * testa a {@code ServerPlayer.die}, prima che il gioco faccia cadere o tenga l'inventario. Se il giocatore indossa
     * un telaio: i segnaposto lo lasciano come in {@link #exit}, da ogni posto in cui possono stare, griglia di
     * fabbricazione e menu aperto compresi (cosi' non cadono a terra, ne' ora ne' quando il gioco toglie il giocatore
     * morto, e con {@code keepInventory} non passano al giocatore rinato); l'armatura stivata resta nel record; il
     * record diventa piazzato nel punto di riposo del telaio ({@link #restingSpot}) e l'entita' nasce li' se il chunk
     * ha le entita' caricate. Non annulla mai la morte e non lancia: un errore si scrive nel log e la morte prosegue.
     */
    public static void onDeath(ServerPlayer player) {
        try {
            FrameStateAccess frames = Compat.frameState();
            if (frames.wornFrameId(player).isEmpty()) {
                return;
            }
            Placeholders.removeAll(player);
            leaveFrame(player, frames, "left at death");
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
     * silenzio ({@link Placeholders#setArmorSlot}). Non ricalcola le dimensioni: lo fa chi chiama, alla fine.
     */
    public static void dress(ServerPlayer player, SuitSummary summary) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        List<ItemStack> displaced = new ArrayList<>();
        for (EquipmentSlot slot : Placeholders.ARMOR_SLOTS) {
            ItemStack old = player.getItemBySlot(slot);
            if (!old.isEmpty() && !Placeholders.isPlaceholder(old)) {
                displaced.add(old);
            }
            Placeholders.setArmorSlot(player, slot, Placeholders.create(slot, summary));
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
        for (EquipmentSlot slot : Placeholders.ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && !Placeholders.isPlaceholder(stack)) {
                worn.add(new StowedStack(slot, stack));
            }
        }
        if (worn.isEmpty()) {
            return;
        }
        frames.stow(player, worn);
        for (StowedStack entry : worn) {
            Placeholders.setArmorSlot(player, entry.slot(), ItemStack.EMPTY);
        }
    }

    /**
     * L'armatura stivata nel record indossato torna al giocatore ({@link FrameStateAccess#takeStowed}): ogni stack nel
     * suo slot se e' vuoto, in silenzio, altrimenti con {@link Handouts#give}. Chi chiama toglie prima i segnaposto,
     * cosi' gli slot sono liberi. Una voce che non si legge resta nel record (vedi {@code takeStowed}).
     */
    static void returnStowed(ServerPlayer player, FrameStateAccess frames) {
        for (StowedStack entry : frames.takeStowed(player)) {
            EquipmentSlot slot = entry.slot();
            if (Placeholders.ARMOR_SLOTS.contains(slot) && player.getItemBySlot(slot).isEmpty()) {
                Placeholders.setArmorSlot(player, slot, entry.stack());
            } else {
                Handouts.give(player, entry.stack());
            }
        }
    }

    /**
     * La parte comune dell'uscita e della morte: il record indossato diventa piazzato nel punto di riposo, con il yaw
     * del giocatore; l'entita' nasce se il chunk del punto ha le entita' caricate; le dimensioni si ricalcolano. Una
     * riga nel log dice dove e' andato il telaio ({@code event}: lasciato, lasciato alla morte), e se non e' rimasto
     * sotto il giocatore un messaggio glielo dice ({@link #ELSEWHERE}).
     *
     * @return il punto di riposo, o vuoto se il registro ha rifiutato l'uscita
     */
    private static Optional<RestingSpot> leaveFrame(ServerPlayer player, FrameStateAccess frames, String event) {
        RestingSpot spot = restingSpot(player);
        Optional<UUID> frameId = frames.exit(player, spot.level(), spot.position(), Mth.wrapDegrees(player.getYRot()));
        if (frameId.isEmpty()) {
            return Optional.empty();
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
        FrameLog.event(frameId.get(), event + spot.where().note, player, level, spot.position());
        if (spot.where() != Where.BELOW) {
            Vec3 at = spot.position();
            player.sendSystemMessage(Component.translatable(ELSEWHERE, Mth.floor(at.x), Mth.floor(at.y),
                    Mth.floor(at.z), level.dimension().location().toString()));
        }
        return Optional.of(spot);
    }

    /**
     * Il passo fuori dal telaio appena lasciato (design, par. 5.5): se gli occhi del giocatore stanno nel riquadro del
     * telaio (il clic successivo lo prenderebbe al posto di qualunque blocco), il giocatore va in un posto libero a un
     * blocco dal centro del telaio, nell'ordine davanti (dove guarda), a sinistra, a destra, dietro. Un posto va bene
     * se c'e' spazio per un giocatore vanilla in piedi, il pavimento e' al massimo un blocco sotto (il giocatore ci
     * si appoggia), sta dentro il bordo del mondo e non c'e' lava ne' fuoco. Senza un posto cosi', o su un veicolo, il
     * giocatore resta dov'e'. Yaw e pitch restano i suoi.
     */
    private static void stepOut(ServerPlayer player, RestingSpot spot) {
        Vec3 frame = spot.position();
        ServerLevel level = player.serverLevel();
        if (spot.level() != level || player.isPassenger()
                || !SuitSize.standingBox(frame).contains(player.getEyePosition())) {
            return;
        }
        double yaw = Math.toRadians(player.getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        // davanti (-sin, cos), sinistra (cos, sin), destra (-cos, -sin), dietro (sin, -cos): yaw 0 guarda a +Z
        double[][] directions = {{-sin, cos}, {cos, sin}, {-cos, -sin}, {sin, -cos}};
        for (double[] direction : directions) {
            Vec3 candidate = new Vec3(frame.x + direction[0] * STEP, frame.y, frame.z + direction[1] * STEP);
            Optional<Vec3> target = standingSpot(player, level, candidate);
            if (target.isPresent()) {
                Vec3 to = target.get();
                player.teleportTo(level, to.x, to.y, to.z, player.getYRot(), player.getXRot());
                return;
            }
        }
    }

    /**
     * I piedi di un giocatore vanilla in piedi in {@code candidate}, appoggiato al pavimento che sta al massimo un
     * blocco sotto; vuoto se li' non c'e' spazio, se il pavimento manca o e' piu' in basso, fuori dal bordo del mondo, o
     * con lava o fuoco nel riquadro o nel blocco sotto i piedi.
     */
    private static Optional<Vec3> standingSpot(ServerPlayer player, ServerLevel level, Vec3 candidate) {
        AABB box = new AABB(candidate.x - PLAYER_HALF_WIDTH, candidate.y, candidate.z - PLAYER_HALF_WIDTH,
                candidate.x + PLAYER_HALF_WIDTH, candidate.y + PLAYER_HEIGHT, candidate.z + PLAYER_HALF_WIDTH);
        if (!level.noCollision(player, box)) {
            return Optional.empty();
        }
        double depth = 1.0 + FLOOR_EPSILON;
        double drop = Shapes.collide(Direction.Axis.Y, box,
                level.getBlockCollisions(null, box.expandTowards(0.0, -depth, 0.0)), -depth);
        if (drop <= -depth) {
            return Optional.empty();
        }
        Vec3 feet = candidate.add(0.0, drop, 0.0);
        if (!FramePlacement.insideBorder(level, feet)) {
            return Optional.empty();
        }
        AABB standing = box.move(0.0, drop, 0.0);
        boolean hot = BlockPos.betweenClosedStream(standing.expandTowards(0.0, -0.5, 0.0)).anyMatch(pos -> {
            BlockState state = level.getBlockState(pos);
            return state.getFluidState().is(FluidTags.LAVA) || state.is(BlockTags.FIRE);
        });
        return hot ? Optional.empty() : Optional.of(feet);
    }

    /**
     * Dove va il telaio che il giocatore lascia (design, par. 5.4), uguale per l'uscita e per la morte: il suolo sotto
     * la posizione del giocatore ({@link FramePlacement#groundBelow}); se sotto non c'e' suolo (o il punto e' sotto il
     * limite inferiore del mondo, o fuori dal bordo del mondo, dove il telaio non si potrebbe toccare), l'ultima
     * posizione sicura ({@link SafeSpots#last}), in qualunque dimensione sia, appoggiata al suolo che c'e' ora sotto di
     * essa se il suo chunk e' caricato (il pavimento puo' essere stato tolto dopo), cosi' com'e' stata ricordata se il
     * chunk non e' caricato, e scartata se il chunk e' caricato e sotto non c'e' piu' suolo o se sta fuori dal bordo;
     * in mancanza anche di quella, il punto di rinascita del mondo (quello condiviso dell'overworld), appoggiato al
     * suolo sotto se c'e'. Non carica chunk ne' dimensioni: {@code getLevel} da' solo livelli gia' caricati e
     * {@code groundBelow} non carica chunk.
     */
    private static RestingSpot restingSpot(ServerPlayer player) {
        ServerLevel here = player.serverLevel();
        Optional<Vec3> ground = FramePlacement.groundBelow(here, player.position())
                .filter(feet -> FramePlacement.insideBorder(here, feet));
        if (ground.isPresent()) {
            return new RestingSpot(here, ground.get(), Where.BELOW);
        }
        MinecraftServer server = here.getServer();
        Optional<SafeSpots.Spot> safe = SafeSpots.last(player);
        if (safe.isPresent()) {
            ServerLevel level = server.getLevel(safe.get().dimension());
            if (level != null) {
                Vec3 position = safe.get().position();
                ChunkPos chunk = new ChunkPos(BlockPos.containing(position));
                Optional<Vec3> floor = level.getChunkSource().hasChunk(chunk.x, chunk.z)
                        ? FramePlacement.groundBelow(level, position) : Optional.of(position);
                if (floor.isPresent() && FramePlacement.insideBorder(level, floor.get())) {
                    return new RestingSpot(level, floor.get(), Where.SAFE_SPOT);
                }
            }
        }
        ServerLevel overworld = server.overworld();
        Vec3 spawn = Vec3.atBottomCenterOf(overworld.getSharedSpawnPos());
        return new RestingSpot(overworld, FramePlacement.groundBelow(overworld, spawn).orElse(spawn), Where.SPAWN);
    }

    /** Dove e' finito il telaio lasciato, rispetto al giocatore; {@code note} completa la riga del log. */
    private enum Where {
        BELOW(""),
        SAFE_SPOT(" (no ground below the player: at the last safe spot)"),
        SPAWN(" (no ground below the player and no usable safe spot: at the world spawn)");

        private final String note;

        Where(String note) {
            this.note = note;
        }
    }

    /** Il livello e il punto dei piedi in cui il telaio lasciato si piazza, e perche' li'. */
    private record RestingSpot(ServerLevel level, Vec3 position, Where where) {
    }
}
