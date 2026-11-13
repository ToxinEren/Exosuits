package com.toxineren.exoframes.compat.impl;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.util.GeckoLibUtil;
//? if >=1.21 {
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
//?} else {
/*import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
*///?}
//? if <1.21 && fabric {
/*import java.util.function.Consumer;
import java.util.function.Supplier;
import software.bernie.geckolib.animatable.client.RenderProvider;
*///?}

/**
 * L'item "modello della tuta": l'unico {@link GeoItem} della mod, animatable del renderer della tuta, che lo disegna
 * con uno stack fittizio. Non si indossa mai e ha un modello d'item normale, cosi' gli agganci di GeckoLib per
 * armature e item non lo trattano mai. Non ha animazioni.
 * <p>
 * Codice comune che un server dedicato carica: nessun campo, inizializzatore o lambda risolve classi client. Le
 * differenze: i package di {@code AnimatableInstanceCache} e {@code AnimatableManager} (sotto {@code core} in
 * GeckoLib 4.8, alla radice in 4.9); solo su Fabric 1.20.1 (GeckoLib 4.8.4) {@code createRenderer} e
 * {@code getRenderProvider} sono astratti. Li' il fornitore si crea con {@link GeoItem#makeRenderer}, che sul server
 * dedicato restituisce un fornitore vuoto senza chiamare {@code createRenderer}; sul client {@code createRenderer} da'
 * il {@code RenderProvider.DEFAULT}, perche' {@code RenderProvider.of} non accetta un fornitore nullo. Forge 1.20.1
 * non ha questi metodi e GeckoLib 4.9 li ha di default.
 */
public final class SuitModelItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    //? if <1.21 && fabric {
    /*private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    *///?}

    public SuitModelItem(Item.Properties props) {
        super(props);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // nessuna animazione: la posa viene dal modello umanoide di base
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    //? if <1.21 && fabric {

    /*@Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(RenderProvider.DEFAULT);
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }
    *///?}
}
