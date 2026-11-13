package com.toxineren.exoframes.compat.impl.client;

import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.compat.impl.SuitModelItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Il modello GeckoLib della tuta: geometria, texture e animazioni generate da tools/art. I percorsi sono espliciti
 * perche' i file non seguono lo schema per sottotipo di {@code DefaultedGeoModel} (la texture sta in
 * {@code textures/entity/}, geometria e animazioni no). Il package di {@link GeoModel} e i tre metodi astratti sono
 * uguali in GeckoLib 4.8 e 4.9: nessuna condizione di versione.
 */
public final class SuitModel extends GeoModel<SuitModelItem> {
    private static final ResourceLocation MODEL = Ids.of("geo/exoframe.geo.json");
    private static final ResourceLocation TEXTURE = Ids.of("textures/entity/exoframe.png");
    private static final ResourceLocation ANIMATION = Ids.of("animations/exoframe.animation.json");

    @Override
    @SuppressWarnings("deprecation") // deprecato in GeckoLib 4.9 a favore della forma con il renderer, ma resta astratto
    public ResourceLocation getModelResource(SuitModelItem animatable) {
        return MODEL;
    }

    @Override
    @SuppressWarnings("deprecation") // deprecato in GeckoLib 4.9 a favore della forma con il renderer, ma resta astratto
    public ResourceLocation getTextureResource(SuitModelItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SuitModelItem animatable) {
        return ANIMATION;
    }
}
