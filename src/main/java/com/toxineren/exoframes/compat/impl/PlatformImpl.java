package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.Platform;
import dev.architectury.utils.Env;
import java.nio.file.Path;

/** Implementazione di {@link Platform} su Architectury; il nome del loader e' fissato per variante da Stonecutter. */
public final class PlatformImpl implements Platform {
    //? if fabric {
    /*private static final String LOADER = "fabric";*/
    //?} else if forge {
    /*private static final String LOADER = "forge";*/
    //?} else {
    private static final String LOADER = "neoforge";
    //?}

    @Override
    public boolean isClient() {
        return dev.architectury.platform.Platform.getEnvironment() == Env.CLIENT;
    }

    @Override
    public boolean isDedicatedServer() {
        return dev.architectury.platform.Platform.getEnvironment() == Env.SERVER;
    }

    @Override
    public boolean isDevelopment() {
        return dev.architectury.platform.Platform.isDevelopmentEnvironment();
    }

    @Override
    public Path configDir() {
        return dev.architectury.platform.Platform.getConfigFolder();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return dev.architectury.platform.Platform.isModLoaded(modId);
    }

    @Override
    public String loaderName() {
        return LOADER;
    }
}
