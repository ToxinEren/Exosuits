package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.entity.ExoframeEntity;
import java.util.Comparator;
import java.util.List;
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
     * Il telaio piazzato che il giocatore guarda entro {@link #SIGHT} blocchi, scelto come lo sceglie la mira del clic
     * destro (design §12.14), con due regole nell'ordine:
     * <ol>
     * <li>se gli occhi sono dentro il riquadro di uno o piu' telai, vince quello con il centro del riquadro piu' vicino
     * agli occhi, senza guardare i blocchi: con gli occhi dentro un telaio il clic prende quello, mai uno piu' lontano
     * sulla stessa linea;</li>
     * <li>altrimenti la prima entita' telaio sulla linea di vista dagli occhi, che si ferma al contorno del primo blocco
     * (non alla sua forma di collisione): come la mira del client, erba alta, fiori, torce e lastre nascondono il
     * telaio che hanno dietro, oltre ai muri.</li>
     * </ol>
     */
    public static Optional<ExoframeEntity> inSight(ServerPlayer player) {
        Vec3 eyes = player.getEyePosition();
        List<ExoframeEntity> around = player.level().getEntitiesOfClass(ExoframeEntity.class,
                new AABB(eyes, eyes).inflate(1.0e-7), f -> !f.isRemoved() && f.getBoundingBox().contains(eyes));
        if (!around.isEmpty()) {
            return around.stream()
                    .min(Comparator.comparingDouble(f -> f.getBoundingBox().getCenter().distanceToSqr(eyes)));
        }
        Vec3 reach = player.getLookAngle().scale(SIGHT);
        Vec3 end = eyes.add(reach);
        HitResult block = player.level().clip(new ClipContext(eyes, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        AABB searched = player.getBoundingBox().expandTowards(reach).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eyes, end, searched,
                entity -> entity instanceof ExoframeEntity && !entity.isRemoved(), eyes.distanceToSqr(end));
        return hit == null ? Optional.empty() : Optional.of((ExoframeEntity) hit.getEntity());
    }
}
