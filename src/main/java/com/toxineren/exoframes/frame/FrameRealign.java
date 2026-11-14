package com.toxineren.exoframes.frame;

import com.google.common.collect.MapMaker;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.entity.ExoframeEntity;
import dev.architectury.event.events.common.TickEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Riallineamento fra registro ed entita' (design, par. 4.5 e 5.4): il registro e' la verita'. Ogni
 * {@link #INTERVAL_TICKS} tick di ogni livello del server si verificano i telai piazzati in quel livello il cui chunk
 * ha le entita' caricate (ed e' pieno, cosi' le sue entita' si trovano per UUID):
 * <ul>
 * <li>se manca l'entita' con l'identita' del record, la prima verifica se lo ricorda soltanto e la seconda
 * consecutiva, se manca ancora, la ricrea con {@link FramePlacement#spawnView} (un'entita' appena tolta da un comando
 * o caduta nel vuoto torna in pochi secondi, senza gareggiare con chi la sta ricreando in quel momento); se nel
 * mezzo l'entita' e' ricomparsa, il record e' andato via o il chunk non ha piu' le entita' caricate, il ricordo si
 * perde;</li>
 * <li>se l'entita' c'e' ma non sta nel punto o con l'orientamento del record (un {@code /tp} di un operatore), la si
 * riporta li'.</li>
 * </ul>
 * L'entita' senza record, invece, si elimina da se' al primo tick ({@link ExoframeEntity}).
 * <p>
 * Lo stato (il contatore dei tick e i telai mancanti visti alla verifica precedente) e' per livello, in una mappa a
 * chiavi deboli confrontate per identita': quando un server si ferma i suoi livelli non sono piu' raggiungibili e il
 * loro stato se ne va con loro, e i livelli di un altro mondo aperto nella stessa sessione di gioco (giocatore singolo)
 * sono oggetti nuovi, quindi nulla passa da un mondo all'altro o da un livello all'altro.
 */
public final class FrameRealign {
    /** Ogni quanti tick di un livello si verifica: 5 secondi. */
    public static final int INTERVAL_TICKS = 100;

    private static final Map<ServerLevel, LevelState> STATES = new MapMaker().weakKeys().makeMap();

    private FrameRealign() {
    }

    /** Registra la verifica periodica alla fine del tick di ogni livello del server. Una volta, nell'init comune. */
    public static void register() {
        TickEvent.SERVER_LEVEL_POST.register(FrameRealign::onLevelTick);
    }

    /**
     * Esegue subito una verifica del livello, con lo stesso ricordo dei telai mancanti delle verifiche periodiche.
     *
     * @return quante entita' ha ricreato (non conta quelle solo riportate al loro punto)
     */
    public static int checkNow(ServerLevel level) {
        Objects.requireNonNull(level, "level must not be null");
        return check(level, stateOf(level));
    }

    private static void onLevelTick(ServerLevel level) {
        LevelState state = stateOf(level);
        if (++state.ticks >= INTERVAL_TICKS) {
            state.ticks = 0;
            check(level, state);
        }
    }

    private static LevelState stateOf(ServerLevel level) {
        return STATES.computeIfAbsent(level, key -> new LevelState());
    }

    private static int check(ServerLevel level, LevelState state) {
        Set<UUID> stillMissing = new HashSet<>();
        int recreated = 0;
        for (PlacedFrame frame : Compat.frameState().placedIn(level)) {
            Vec3 spot = frame.position();
            ChunkPos chunk = new ChunkPos(BlockPos.containing(spot));
            // entita' caricate (brief) e chunk pieno: solo allora getEntity vede le entita' del chunk, e un'entita'
            // che c'e' non sembra mancare
            if (!level.areEntitiesLoaded(chunk.toLong()) || !level.getChunkSource().hasChunk(chunk.x, chunk.z)) {
                continue;
            }
            Entity entity = level.getEntity(frame.frameId());
            if (entity == null) {
                if (state.missing.contains(frame.frameId())) {
                    // non aggiunta se il livello conosce gia' l'UUID (un'entita' portata in un chunk non visibile)
                    ExoframeEntity spawned = FramePlacement.spawnView(level, frame);
                    if (level.getEntity(frame.frameId()) == spawned) {
                        recreated++;
                    }
                } else {
                    stillMissing.add(frame.frameId());
                }
            } else if (entity instanceof ExoframeEntity && (!entity.position().equals(spot)
                    || entity.getYRot() != frame.yaw())) {
                entity.moveTo(spot.x, spot.y, spot.z, frame.yaw(), 0f);
            }
        }
        state.missing = stillMissing;
        return recreated;
    }

    /** Lo stato della verifica di un livello. */
    private static final class LevelState {
        /** Tick dall'ultima verifica periodica. */
        private int ticks;
        /** I telai trovati senza entita' all'ultima verifica, che la prossima ricrea se mancano ancora. */
        private Set<UUID> missing = Set.of();
    }
}
