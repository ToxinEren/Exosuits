package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.entity.ExoframeEntity;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Il telaio piazzato che un giocatore guarda, per i comandi che si riferiscono a un telaio non indossato
 * ({@code /exoframe unlock}, {@code lock}, {@code remove}).
 */
public final class FrameSight {
    /** Fin dove, in blocchi dagli occhi, un comando trova il telaio piazzato che il giocatore guarda. */
    public static final double SIGHT = 5.0;

    private FrameSight() {
    }

    /**
     * Il telaio piazzato che il giocatore guarda entro {@link #SIGHT} blocchi: la prima entita' telaio sulla linea di
     * vista dagli occhi, che si ferma al primo blocco solido (un muro nasconde il telaio che ha dietro).
     */
    public static Optional<ExoframeEntity> inSight(ServerPlayer player) {
        Vec3 eyes = player.getEyePosition();
        Vec3 reach = player.getLookAngle().scale(SIGHT);
        Vec3 end = eyes.add(reach);
        HitResult block = player.level().clip(new ClipContext(eyes, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        AABB around = player.getBoundingBox().expandTowards(reach).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eyes, end, around,
                entity -> entity instanceof ExoframeEntity && !entity.isRemoved(), eyes.distanceToSqr(end));
        return hit == null ? Optional.empty() : Optional.of((ExoframeEntity) hit.getEntity());
    }
}
