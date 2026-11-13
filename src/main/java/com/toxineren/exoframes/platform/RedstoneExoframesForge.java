package com.toxineren.exoframes.platform;

//? if forge {
/*import com.toxineren.exoframes.RedstoneExoframes;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// Entrypoint Forge: Architectury 9 vuole il bus del mod registrato prima di qualsiasi registrazione.
@Mod(RedstoneExoframes.MOD_ID)
public final class RedstoneExoframesForge {
    @SuppressWarnings("removal") // FMLJavaModLoadingContext.get() e' deprecato in 47.4 ma resta il percorso documentato da Architectury 9
    public RedstoneExoframesForge() {
        EventBuses.registerModEventBus(RedstoneExoframes.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        RedstoneExoframes.init();
    }
}
*///?}
