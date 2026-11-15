package com.toxineren.exoframes.frame;

import com.google.common.collect.MapMaker;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameStateAccess;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.energy.Activity;
import com.toxineren.exoframes.core.energy.ActivityRule;
import com.toxineren.exoframes.core.energy.EnergyMeter;
import com.toxineren.exoframes.core.energy.EnergyRates;
import com.toxineren.exoframes.core.energy.EnergyState;
import com.toxineren.exoframes.core.frame.FrameEffects;
import com.toxineren.exoframes.core.frame.FrameRules;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.frame.FrameSounds.Sound;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Il tick dell'energia di chi indossa un telaio (design §12.3): attivita', salti e consumo, effetti e avvisi
 * dell'energia, sincronizzazione al proprietario; un giocatore alla volta.
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
 * <b>Effetti e transizioni.</b> A ogni tick {@code RedstoneExoframes.rules().evaluate(state)}: gli attributi si
 * applicano solo quando gli effetti sono diversi dagli ultimi applicati (e alla prima valutazione del record). Alla
 * prima valutazione (rientro nel server, rinascita: non e' un evento) uno stato {@code LOW} o {@code EMPTY} da' il suo
 * messaggio ({@link #ENERGY_LOW}, {@link #ENERGY_EMPTY}) senza suono; dopo parlano solo i peggioramenti, da
 * {@code NORMAL} a {@code LOW} e da {@code NORMAL} o {@code LOW} a {@code EMPTY}, con messaggio e suono; i
 * miglioramenti tacciono. Le transizioni si confrontano con gli ultimi effetti applicati dal tick, mai con quelli
 * mandati: una sincronizzazione strutturale fuori dal tick non zittisce l'avviso. Senza scatto ({@code LOW},
 * {@code EMPTY}) il server spegne la corsa per i propri conti; il divieto vero lo fa il client.
 * <p>
 * <b>Sincronizzazione</b> (design §12.4). Il tick manda lo stato al proprietario con {@code syncTo} subito al primo
 * tick del record e a un cambio di effetti, di stato dell'energia o di condizione dei pezzi rispetto all'ultima
 * sincronizzazione; altrimenti al piu' ogni {@value #SYNC_INTERVAL} tick finche' c'e' qualcosa "da sincronizzare": un
 * consumo di almeno 1 RE o, dal danno, l'usura ({@link #markDirty}). Ogni {@code syncTo}, anche quelle strutturali
 * fuori dal tick, aggiorna nel record i valori mandati e il tick dell'ultima sincronizzazione e toglie il segno: un
 * ascoltatore di {@code onSynced}, che come tick usa l'ultimo {@code now} visto dal record.
 * <p>
 * <b>Suggerimento sull'acqua</b> (design §12.6). Al primo tick del record in cui il giocatore tocca l'acqua, fuori
 * dallo spettatore, una riga nella barra delle azioni ({@link SuitMovement#WATER_HINT}); il segno sta nel record.
 * L'uscita dal telaio scarta il record, quindi a ogni ingresso il suggerimento torna una volta; un rientro nel server
 * o una rinascita (oggetto giocatore nuovo, record nuovo) lo ripresentano.
 * <p>
 * Ogni eccezione del tick si cattura: una riga ERROR alla prima per giocatore e sessione, poi silenzio.
 */
public final class SuitTick {
    /** Chiave del messaggio "esci prima dall'esoscheletro": cavalcature qui, letti in {@code SuitRestrictions}. */
    public static final String LEAVE_FRAME_FIRST = "message." + RedstoneExoframes.MOD_ID + ".leave_frame_first";

    /** Chiave del messaggio "energia bassa": la cella e' scesa in {@code LOW}. */
    public static final String ENERGY_LOW = "message." + RedstoneExoframes.MOD_ID + ".energy_low";

    /** Chiave del messaggio "energia esaurita": la cella e' scesa in {@code EMPTY}. */
    public static final String ENERGY_EMPTY = "message." + RedstoneExoframes.MOD_ID + ".energy_empty";

    /** Tick minimi fra due sincronizzazioni dovute al consumo (design §12.4). */
    static final int SYNC_INTERVAL = 5;

    /** I record, per oggetto giocatore: chiavi deboli confrontate per identita' ({@code MapMaker.weakKeys}). */
    private static final Map<ServerPlayer, Record> RECORDS = new MapMaker().weakKeys().makeMap();

    /** I giocatori il cui tick ha gia' scritto la riga ERROR in questa sessione; si toglie all'uscita dal server. */
    private static final Set<UUID> FAILURES_LOGGED = ConcurrentHashMap.newKeySet();

    private SuitTick() {
    }

    /**
     * Registra il tick sul server ({@code TickEvent.PLAYER_PRE}, con {@code server.getTickCount()} come {@code now}),
     * l'ascoltatore delle sincronizzazioni ({@code onSynced}) e la pulizia del segno della riga ERROR all'uscita dal
     * server ({@code PlayerEvent.PLAYER_QUIT}). Una volta, nell'init comune.
     */
    public static void register() {
        TickEvent.PLAYER_PRE.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer, serverPlayer.server.getTickCount());
            }
        });
        Compat.frameState().onSynced(SuitTick::afterSync);
        PlayerEvent.PLAYER_QUIT.register(player -> FAILURES_LOGGED.remove(player.getUUID()));
    }

    /**
     * Un tick dell'energia del giocatore, passi 1-9 di §12.3: senza telaio indossato scarta il record e toglie gli
     * attributi; altrimenti crea il record se manca, fa scendere un passeggero, calcola attivita' e salto e, fuori da
     * creativa e spettatore, consuma con {@link FrameRules#tick} e {@link FrameRules#jump} in un solo {@code mutate};
     * poi valuta gli effetti, applica gli attributi e dice le transizioni, spegne la corsa senza scatto, sincronizza
     * il proprietario con la regola di §12.4 e, al primo contatto con l'acqua del record, manda il suggerimento
     * {@link SuitMovement#WATER_HINT} (non a uno spettatore).
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
            FrameState state = worn.get();
            FrameEffects fx = RedstoneExoframes.rules().evaluate(state); // 6
            boolean first = r.applied == null;
            if (!fx.equals(r.applied)) {
                Compat.attributes().apply(player, fx);
            }
            EnergyState energy = fx.energyState();
            if (first) {
                // rientro o rinascita: non e' un evento, messaggio senza suono
                if (energy != EnergyState.NORMAL) {
                    FrameInteractions.tell(player, energy == EnergyState.LOW ? ENERGY_LOW : ENERGY_EMPTY);
                }
            } else if (energy == EnergyState.EMPTY && r.applied.energyState() != EnergyState.EMPTY) {
                FrameInteractions.tell(player, ENERGY_EMPTY);
                FrameSounds.play(player.serverLevel(), player.position(), Sound.ENERGY_EMPTY);
            } else if (energy == EnergyState.LOW && r.applied.energyState() == EnergyState.NORMAL) {
                FrameInteractions.tell(player, ENERGY_LOW);
                FrameSounds.play(player.serverLevel(), player.position(), Sound.ENERGY_LOW);
            } // i miglioramenti tacciono
            r.applied = fx;
            if (!fx.canSprint() && player.isSprinting()) { // 7
                player.setSprinting(false);
            }
            if (first || !fx.equals(r.sentEffects) || energy != r.sentEnergy // 8
                    || !conditions(state).equals(r.sentConditions)
                    || (r.dirty && now - r.lastSyncTick >= SYNC_INTERVAL)) {
                frames.syncTo(player); // afterSync aggiorna il record
            }
            if (!r.waterHintShown && !player.isSpectator() && player.isInWater()) { // 9
                player.displayClientMessage(Component.translatable(SuitMovement.WATER_HINT), true);
                r.waterHintShown = true;
            }
        } catch (RuntimeException e) {
            logFailure(player, e);
        }
    }

    /**
     * Ascoltatore di {@code onSynced}, in coda a ogni {@code syncTo} (anche quelle strutturali fuori dal tick:
     * ingresso, uscita, {@code changeWorn}): se il giocatore ha un record, vi scrive cio' che e' partito (effetti,
     * stato dell'energia, condizione dei pezzi, se il telaio e' ancora indossato), come tick dell'ultima
     * sincronizzazione l'ultimo {@code now} visto dal record, e toglie il segno "da sincronizzare".
     */
    private static void afterSync(ServerPlayer player) {
        try {
            Record r = RECORDS.get(player);
            if (r == null) {
                return;
            }
            Optional<FrameState> worn = Compat.frameState().get(player);
            if (worn.isPresent()) {
                FrameState state = worn.get();
                r.sentEffects = RedstoneExoframes.rules().evaluate(state);
                r.sentEnergy = r.sentEffects.energyState();
                r.sentConditions = conditions(state);
            }
            r.lastSyncTick = r.lastNow;
            r.dirty = false;
        } catch (RuntimeException e) {
            logFailure(player, e);
        }
    }

    /** La riga ERROR del tick, una volta per giocatore e sessione. */
    private static void logFailure(ServerPlayer player, RuntimeException e) {
        if (FAILURES_LOGGED.add(player.getUUID())) {
            RedstoneExoframes.LOGGER.error("Exoframe tick failed for {}", player.getGameProfile().getName(), e);
        }
    }

    /** La condizione di ciascun pezzo montato, per slot. */
    private static Map<PieceSlot, PieceCondition> conditions(FrameState state) {
        Map<PieceSlot, PieceCondition> result = new EnumMap<>(PieceSlot.class);
        for (Map.Entry<PieceSlot, Piece> e : state.pieces().entrySet()) {
            result.put(e.getKey(), e.getValue().condition());
        }
        return result;
    }

    /**
     * Segna il record del giocatore "da sincronizzare" (l'usura ha tolto almeno un punto a un pezzo): la
     * sincronizzazione parte al primo tick ad almeno {@value #SYNC_INTERVAL} tick dall'ultima. Senza record nulla: il
     * primo tick sincronizza comunque.
     */
    static void markDirty(ServerPlayer player) {
        Record r = RECORDS.get(player);
        if (r != null) {
            r.dirty = true;
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
        /** Da sincronizzare: il consumo ha tolto almeno 1 RE (o l'usura un punto) dall'ultima sincronizzazione. */
        boolean dirty;
        /** Gli ultimi effetti valutati dal tick; null finche' la prima valutazione e' da fare. */
        FrameEffects applied;
        /** Gli effetti partiti con l'ultima sincronizzazione, chiunque l'abbia chiamata. */
        FrameEffects sentEffects;
        /** Lo stato dell'energia partito con l'ultima sincronizzazione. */
        EnergyState sentEnergy;
        /** La condizione dei pezzi partita con l'ultima sincronizzazione. */
        Map<PieceSlot, PieceCondition> sentConditions;
        /** Il suggerimento sull'acqua e' gia' partito per questo record (design §12.6); falso alla creazione. */
        boolean waterHintShown;

        Record(Vec3 lastPos, ResourceKey<Level> lastDimension, int jumpBase, long now) {
            this.lastPos = lastPos;
            this.lastDimension = lastDimension;
            this.jumpBase = jumpBase;
            this.lastNow = now;
            this.lastSyncTick = now - SYNC_INTERVAL;
        }
    }
}
