package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Un telaio piazzato del registro dei telai, come lo vede il resto del gioco (vedi {@link FrameStateAccess#placed}):
 * identita', stato, dimensione, posizione dei piedi, orientamento in gradi e se ha armatura stivata (anche voci che
 * non si leggono). Lo stato e' quello vivo del registro: va trattato in sola lettura, e ogni modifica passa da
 * {@link FrameStateAccess#updatePlaced}. Nessun componente puo' essere null.
 */
public record PlacedFrame(UUID frameId, FrameState state, ResourceKey<Level> dimension, Vec3 position, float yaw,
        boolean hasStowed) {
    public PlacedFrame {
        Objects.requireNonNull(frameId, "frameId must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(dimension, "dimension must not be null");
        Objects.requireNonNull(position, "position must not be null");
    }
}
