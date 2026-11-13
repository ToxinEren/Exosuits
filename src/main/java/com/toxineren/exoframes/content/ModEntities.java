package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.SuitSize;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Tipi di entita' della mod. */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(RedstoneExoframes.MOD_ID, Registries.ENTITY_TYPE);

    /** Altezza dell'entita' telaio: quella della tuta in piedi, 1,8 * {@link SuitSize#HEIGHT_SCALE} = 2,97. */
    private static final float EXOFRAME_HEIGHT = 1.8f * SuitSize.HEIGHT_SCALE;

    /**
     * Il telaio piazzato: riquadro della tuta in piedi ({@link SuitSize#WIDTH} x 2,97), immune al fuoco, tracciato dai
     * client entro 10 chunk.
     */
    public static final RegistrySupplier<EntityType<ExoframeEntity>> EXOFRAME = ENTITY_TYPES.register("exoframe",
            () -> EntityType.Builder.<ExoframeEntity>of(ExoframeEntity::new, MobCategory.MISC)
                    .sized(SuitSize.WIDTH, EXOFRAME_HEIGHT)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("exoframe"));

    private ModEntities() {
    }

    public static void register() {
        ENTITY_TYPES.register();
    }
}
