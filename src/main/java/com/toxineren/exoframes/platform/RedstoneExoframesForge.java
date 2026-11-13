package com.toxineren.exoframes.platform;

//? if forge {
/*import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.client.RedstoneExoframesClient;
import dev.architectury.platform.forge.EventBuses;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// Entrypoint Forge: Architectury 9 vuole il bus del mod registrato prima di qualsiasi registrazione; le
// registrazioni solo client (init client comune, che aggiunge anche il listener AddLayers al bus del mod) girano solo
// sul client fisico.
@Mod(RedstoneExoframes.MOD_ID)
public final class RedstoneExoframesForge {
    @SuppressWarnings("removal") // FMLJavaModLoadingContext.get() e' deprecato in 47.4 ma resta il percorso documentato da Architectury 9
    public RedstoneExoframesForge() {
        EventBuses.registerModEventBus(RedstoneExoframes.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        RedstoneExoframes.init();
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> RedstoneExoframesClient.init());
    }
}
*///?}
