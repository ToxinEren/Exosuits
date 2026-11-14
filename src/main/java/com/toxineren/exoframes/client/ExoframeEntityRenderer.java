package com.toxineren.exoframes.client;

import com.google.common.collect.MapMaker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.entity.ExoframeEntity;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Il renderer del telaio piazzato (design §4.2 B, §5.4): lo stesso disegno della tuta indossata ({@link SuitDraw}),
 * cosi' un telaio piazzato e la tuta di un giocatore sono lo stesso oggetto sullo schermo, stessa grandezza e stessi
 * pezzi e colori. La vista viene dal riassunto sincronizzato dell'entita'.
 * <p>
 * La posa e' quella di riposo: un {@link HumanoidModel} del giocatore appena costruito, mai animato e mai disegnato
 * (in piedi, braccia lungo i fianchi, testa avanti, nessun passo ne' respiro). Lo spazio del modello e' quello che
 * {@code LivingEntityRenderer} e {@code PlayerRenderer} preparano per un giocatore, girato dello yaw dell'entita' come
 * il corpo di un'entita' viva: i piedi della tuta stanno sul blocco e la tuta e' alta quanto quella indossata da un
 * giocatore in piedi accanto. Nessun sollevamento da annullare: il telaio non ha pilota.
 * <p>
 * L'ombra e' rotonda e ha il raggio di quella del giocatore vanilla ({@link #SHADOW_RADIUS}), cosi' entrare e uscire
 * da un telaio non la cambia. Il culling usa il riquadro allargato dell'entita'
 * ({@link ExoframeEntity#getBoundingBoxForCulling}), che {@code EntityRenderer.shouldRender} legge sulle due versioni.
 */
public final class ExoframeEntityRenderer extends EntityRenderer<ExoframeEntity> {
    /** La scala del modello del giocatore in {@code PlayerRenderer.scale}: la tuta indossata la riceve con il giocatore. */
    private static final float PLAYER_MODEL_SCALE = 0.9375f;

    /**
     * Il raggio dell'ombra: quello del giocatore vanilla ({@code PlayerRenderer}), che la tuta indossata non cambia.
     * Un'ombra piu' grande per entrambi e' una scelta di grafica successiva.
     */
    private static final float SHADOW_RADIUS = 0.5f;

    private static final ResourceLocation TEXTURE = Ids.of("textures/entity/exoframe.png");

    /** Il modello umanoide a riposo che da' la posa alle ossa della tuta; non si disegna mai. */
    private final HumanoidModel<LivingEntity> restPose;

    /**
     * Vista per entita', rifatta solo quando cambia il riassunto sincronizzato (che l'entita' tiene come una sola
     * istanza finche' il dato non cambia). Chiavi deboli e confrontate per identita': un'entita' tolta dal mondo esce
     * dalla cache con il garbage collector.
     */
    private final Map<ExoframeEntity, Decoded> views = new MapMaker().weakKeys().makeMap();

    public ExoframeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.restPose = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER));
        // I modelli nascono "giovani": GeckoLib copia il segno e rimpicciolirebbe la tuta come quella di un cucciolo.
        this.restPose.young = false;
        this.shadowRadius = SHADOW_RADIUS;
        SuitDraw.resetFailureLog();
    }

    @Override
    public void render(ExoframeEntity entity, float entityYaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int packedLight) {
        SuitView view = view(entity);
        if (view != null) {
            pose.pushPose();
            // Come LivingEntityRenderer per il corpo e PlayerRenderer.scale per la scala: lo spazio del modello del
            // giocatore, con i piedi a y = FEET_Y (y verso il basso).
            pose.mulPose(Axis.YP.rotationDegrees(180f - entityYaw));
            pose.scale(-1f, -1f, 1f);
            pose.scale(PLAYER_MODEL_SCALE, PLAYER_MODEL_SCALE, PLAYER_MODEL_SCALE);
            pose.translate(0f, -SuitDraw.FEET_Y, 0f);
            SuitDraw.draw(entity, restPose, pose, buffers, packedLight, partialTick, view, 0f);
            pose.popPose();
        }
        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ExoframeEntity entity) {
        return TEXTURE;
    }

    /** La vista dell'entita' dalla cache, o null dopo un errore (scritto nel log da {@link SuitDraw}). */
    private SuitView view(ExoframeEntity entity) {
        try {
            SuitSummary summary = entity.summary();
            Decoded cached = views.get(entity);
            if (cached == null || cached.summary() != summary) {
                cached = new Decoded(summary, SuitDraw.view(summary));
                views.put(entity, cached);
            }
            return cached.view();
        } catch (RuntimeException | LinkageError e) {
            SuitDraw.logFailureOnce(e);
            return null;
        }
    }

    /** Il riassunto da cui e' uscita una vista, confrontato per identita', e la vista. */
    private record Decoded(SuitSummary summary, SuitView view) {
    }
}
