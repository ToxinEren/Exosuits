package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.content.ModEntities;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.entity.ExoframeEntity;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Piazzamento di un telaio nel mondo (design, par. 4.5 e 5.4): prima il record nel registro, poi l'entita' che ne e' la
 * vista, con lo stesso UUID. Solo sul server, sul thread di gioco. Chi piazza su richiesta di un giocatore (l'oggetto,
 * il comando di debug) passa prima il punto da {@link #groundBelow}: un telaio nasce appoggiato al suolo.
 */
public final class FramePlacement {
    private FramePlacement() {
    }

    /**
     * Piazza un telaio con lo stato dato (che da qui appartiene al registro) con i piedi in {@code feet} e
     * l'orientamento {@code yaw} in gradi; non cerca il suolo. Rifiuta, senza creare nulla, se il riquadro della tuta
     * in piedi li' urta blocchi ({@code level.noCollision}) o il riquadro di un altro telaio (due telai nello stesso
     * punto non si distinguerebbero con un clic), e se il dato salvato e' in sola lettura. Giocatori e creature nel
     * riquadro non contano.
     *
     * @return l'entita' del telaio piazzato, o vuoto se rifiutato
     */
    public static Optional<ExoframeEntity> place(ServerLevel level, FrameState state, Vec3 feet, float yaw) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(feet, "feet must not be null");
        AABB box = SuitSize.standingBox(feet);
        if (!level.noCollision(box) || !level.getEntitiesOfClass(ExoframeEntity.class, box).isEmpty()) {
            return Optional.empty();
        }
        UUID frameId = Compat.frameState().place(level, state, feet, yaw);
        if (frameId == null) {
            return Optional.empty(); // dato in sola lettura: nessun record, quindi nessuna entita'
        }
        PlacedFrame frame = Compat.frameState().placed(level.getServer(), frameId)
                .orElseThrow(() -> new IllegalStateException("placed frame " + frameId + " not found after place"));
        return Optional.of(spawnView(level, frame));
    }

    /**
     * Il primo appoggio sotto {@code from}: la posizione dei piedi a cui arriva il riquadro della tuta in piedi
     * lasciato cadere dritto da {@code from} finche' non posa su una forma di collisione di un blocco (lastre,
     * tappeti e staccionate con la loro forma vera; i fluidi non sono suolo, quindi si arriva sul fondo di un lago);
     * {@code from} stesso se ci posa gia'. Mai piu' in alto di {@code from}, mai di lato. Le forme che il riquadro
     * urta gia' in {@code from} non fanno da suolo (come nel movimento vanilla): li' {@link #place} poi rifiuta.
     *
     * @return vuoto se il riquadro arriva al fondo del mondo senza appoggiarsi (anche in colonne senza chunk caricati)
     */
    public static Optional<Vec3> groundBelow(ServerLevel level, Vec3 from) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(from, "from must not be null");
        double depth = from.y - level.getMinBuildHeight();
        if (depth <= 0) {
            return Optional.empty();
        }
        AABB box = SuitSize.standingBox(from);
        Iterable<VoxelShape> below = level.getBlockCollisions(null, box.expandTowards(0, -depth, 0));
        double drop = Shapes.collide(Direction.Axis.Y, box, below, -depth);
        if (drop <= -depth) {
            return Optional.empty();
        }
        return Optional.of(new Vec3(from.x, from.y + drop, from.z));
    }

    /**
     * Crea e aggiunge al livello l'entita' di un record piazzato che esiste gia': con il suo UUID, nel suo punto, con
     * il suo orientamento e con il riassunto del suo stato, giusto fin dal primo pacchetto ai client.
     *
     * @throws IllegalArgumentException se il record e' piazzato in un'altra dimensione
     */
    public static ExoframeEntity spawnView(ServerLevel level, PlacedFrame frame) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(frame, "frame must not be null");
        if (!frame.dimension().equals(level.dimension())) {
            throw new IllegalArgumentException("frame " + frame.frameId() + " is placed in " + frame.dimension()
                    + ", not in " + level.dimension());
        }
        ExoframeEntity entity = new ExoframeEntity(ModEntities.EXOFRAME.get(), level);
        entity.setUUID(frame.frameId());
        Vec3 spot = frame.position();
        entity.moveTo(spot.x, spot.y, spot.z, frame.yaw(), 0f);
        entity.setSummary(SuitSummary.of(frame.state()));
        level.addFreshEntity(entity);
        return entity;
    }
}
