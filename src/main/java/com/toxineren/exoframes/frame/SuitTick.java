package com.toxineren.exoframes.frame;

import com.google.common.collect.MapMaker;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.core.energy.Activity;
import com.toxineren.exoframes.core.energy.ActivityRule;
import com.toxineren.exoframes.core.energy.EnergyMeter;
import com.toxineren.exoframes.core.energy.EnergyRates;
import com.toxineren.exoframes.core.frame.FrameRules;
import com.toxineren.exoframes.core.frame.FrameState;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Il tick dell'energia di chi indossa un telaio (design §12.3): attivita', salti e consumo, un giocatore alla volta.
 * <p>
 * <b>Dove gira.</b> Su {@code TickEvent.PLAYER_PRE} di Architectury, solo sul server: li' la posizione del giocatore e'
 * l'ultima accettata dal client (in {@code PLAYER_POST} porterebbe un passo di fisica del server poi scartato). Il
 * server non manda l'evento ai giocatori finti: i test chiamano {@link #tick} a mano.
 * <p>
 * <b>Il record.</b> Uno per oggetto {@code ServerPlayer}, con chiavi deboli confrontate per identita' come
 * {@code SuitSize.LAST_SEEN}: un oggetto nuovo (ingresso nel server, rinascita, ritorno dall'End) non ha record e
 * riparte da zero al suo primo tick con un telaio indossato. Lo scarta {@link WornState#changed} quando lo stato
 * indossato finisce; all'uscita dal server se ne va con l'oggetto. Il riporto del contatore (millesimi di RE, al piu'
 * 1 RE) non si salva: si perde con il record.
 * <p>
 * <b>Il tempo.</b> {@code now} entra solo dall'ascoltatore ({@code server.getTickCount()}) o dai test: il record non
 * legge mai {@code getTickCount()}. Un {@code now} minore dell'ultimo visto (contatore ripartito) fa ripartire la
 * finestra dell'attivita' e il limite delle sincronizzazioni.
 * <p>
 * <b>Creativa e spettatore.</b> Con {@code getAbilities().instabuild} o {@code isSpectator()} non si consuma nulla,
 * ne' attivita' ne' salti; il record resta. Mai {@code isCreative()}: i giocatori finti dei test rispondono sempre
 * vero.
 * <p>
 * <b>Salti.</b> Dalla statistica vanilla dei salti, che il server incrementa a ogni stacco da terra ricevuto dal
 * client: si paga al piu' un salto per tick, e la base si rilegge a ogni tick, pagato o no, quindi un salto non pagato
 * non si paga piu' tardi; se la statistica cala, la base riparte da li'. Nessun costo con {@code hurtTime > 0} (lo
 * stacco di un contraccolpo) ne' a cella vuota.
 * <p>
 * <b>Cavalcature.</b> Con la tuta non si cavalca: un passeggero di qualunque cosa scende al primo tick e ha il
 * messaggio {@link #LEAVE_FRAME_FIRST}; quel tick conta come fermo. Motivo: seduta in un tunnel alto due la tuta ha gli
 * occhi nel soffitto, e il soffocamento aggira l'armatura.
 * <p>
 * Ogni eccezione del tick si cattura: una riga ERROR alla prima per giocatore e sessione, poi silenzio.
 */
public final class SuitTick {
    /** Chiave del messaggio "esci prima dall'esoscheletro": cavalcature qui, letti in {@code SuitRestrictions}. */
    public static final String LEAVE_FRAME_FIRST = "message." + RedstoneExoframes.MOD_ID + ".leave_frame_first";

    /** Tick minimi fra due sincronizzazioni dovute al consumo (design §12.4). */
    static final int SYNC_INTERVAL = 5;

    /** I record, per oggetto giocatore: chiavi deboli confrontate per identita' ({@code MapMaker.weakKeys}). */
    private static final Map<ServerPlayer, Record> RECORDS = new MapMaker().weakKeys().makeMap();

    /** I giocatori il cui tick ha gia' scritto la riga ERROR in questa sessione; si toglie all'uscita dal server. */
    private static final Set<UUID> FAILURES_LOGGED = ConcurrentHashMap.newKeySet();

    private SuitTick() {
    }

    /**
     * Registra il tick sul server ({@code TickEvent.PLAYER_PRE}, con {@code server.getTickCount()} come {@code now}) e
     * la pulizia del segno della riga ERROR all'uscita dal server ({@code PlayerEvent.PLAYER_QUIT}). Una volta,
     * nell'init comune.
     */
    public static void register() {
        TickEvent.PLAYER_PRE.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer, serverPlayer.server.getTickCount());
            }
        });
        PlayerEvent.PLAYER_QUIT.register(player -> FAILURES_LOGGED.remove(player.getUUID()));
    }

    /**
     * Un tick dell'energia del giocatore, passi 1-5 di §12.3: senza telaio indossato scarta il record e toglie gli
     * attributi; altrimenti crea il record se manca, fa scendere un passeggero, calcola attivita' e salto e, fuori da
     * creativa e spettatore, consuma con {@link FrameRules#tick} e {@link FrameRules#jump} in un solo {@code mutate}.
     *
     * @param now il tick del server ({@code server.getTickCount()})
     */
    public static void tick(ServerPlayer player, long now) {
        try {
            FrameStateAccess frames = Compat.frameState();
            Optional<FrameState> worn = frames.get(player);
            Record r = RECORDS.get(player);
            if (worn.isEmpty()) {
                if (r != null) {
                    RECORDS.remove(player);
                    Compat.attributes().clear(player);
                }
                return;
            }
            int jumps = player.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP));
            if (r == null) {
                // basi del record; la prima valutazione e' ancora da fare
                r = new Record(player.position(), player.level().dimension(), jumps, now);
                RECORDS.put(player, r);
            }
            if (now < r.lastNow) {
                // orologio tornato indietro: finestra e limite ripartono
                r.activity.reset();
                r.lastSyncTick = now - SYNC_INTERVAL;
            }
            r.lastNow = now;
            boolean passenger = player.isPassenger(); // letto prima di stopRiding
            if (passenger) {
                player.stopRiding();
                FrameInteractions.tell(player, LEAVE_FRAME_FIRST);
            }
            Vec3 pos = player.position(); // attivita', dopo la discesa
            double distance = Math.hypot(pos.x - r.lastPos.x, pos.z - r.lastPos.z);
            if (!player.level().dimension().equals(r.lastDimension)) {
                r.activity.reset();
                r.lastDimension = player.level().dimension();
                distance = 0.0; // campione IDLE
            }
            r.lastPos = pos;
            Activity activity = r.activity.next(distance, player.isSprinting(), passenger);
            boolean jumped = jumps > r.jumpBase && player.hurtTime == 0; // [0, 1]; in calo = nuova base
            r.jumpBase = jumps; // a ogni tick, pagato o no
            if (!player.getAbilities().instabuild && !player.isSpectator()) {
                FrameRules rules = RedstoneExoframes.rules();
                Record rec = r;
                int[] drained = {0};
                boolean changed = frames.mutate(player, state -> {
                    drained[0] += rules.tick(state, activity, rec.meter);
                    if (jumped && state.cell() != null && !state.cell().isEmpty()) {
                        drained[0] += rules.jump(state, rec.meter);
                    }
                });
                if (changed && drained[0] >= 1) {
                    r.dirty = true;
                }
            }
        } catch (RuntimeException e) {
            if (FAILURES_LOGGED.add(player.getUUID())) {
                RedstoneExoframes.LOGGER.error("Exoframe tick failed for {}", player.getGameProfile().getName(), e);
            }
        }
    }

    /** Scarta il record del giocatore: lo stato indossato e' finito ({@link WornState#changed}). */
    static void forget(ServerPlayer player) {
        RECORDS.remove(player);
    }

    /** True se il giocatore ha un record del tick (per i test). */
    public static boolean hasRecord(ServerPlayer player) {
        return RECORDS.containsKey(player);
    }

    /** Cio' che il tick ricorda di un oggetto giocatore fra un tick e l'altro. */
    private static final class Record {
        final EnergyMeter meter = new EnergyMeter(EnergyRates.from(RedstoneExoframes.config().energy));
        final ActivityRule activity = new ActivityRule();
        Vec3 lastPos;
        ResourceKey<Level> lastDimension;
        /** La statistica dei salti al tick prima. */
        int jumpBase;
        /** L'ultimo {@code now} visto. */
        long lastNow;
        /** Il tick dell'ultima sincronizzazione; alla creazione {@code now - SYNC_INTERVAL}. */
        long lastSyncTick;
        /** Il consumo ha tolto almeno 1 RE dall'ultima sincronizzazione. */
        boolean dirty;

        Record(Vec3 lastPos, ResourceKey<Level> lastDimension, int jumpBase, long now) {
            this.lastPos = lastPos;
            this.lastDimension = lastDimension;
            this.jumpBase = jumpBase;
            this.lastNow = now;
            this.lastSyncTick = now - SYNC_INTERVAL;
        }
    }
}
