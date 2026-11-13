package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.RedstoneExoframes;
import net.minecraft.resources.ResourceLocation;

/** Costruzione degli identificatori: l'unico punto in cui l'API di ResourceLocation cambia tra 1.20.1 e 1.21.1. */
public final class Ids {
    private Ids() {
    }

    public static ResourceLocation of(String path) {
        return of(RedstoneExoframes.MOD_ID, path);
    }

    @SuppressWarnings("removal") // su Forge 1.20.1 il costruttore e' deprecato, ma su quella versione e' l'unica via
    public static ResourceLocation of(String namespace, String path) {
        //? if >=1.21 {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //?} else {
        /*return new ResourceLocation(namespace, path);*/
        //?}
    }
}
