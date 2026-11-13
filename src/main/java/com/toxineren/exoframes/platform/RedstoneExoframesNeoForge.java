package com.toxineren.exoframes.platform;

//? if neoforge {
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.client.RedstoneExoframesClient;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

// Entrypoint NeoForge: Architectury 13 non richiede la registrazione del bus del mod; le registrazioni solo
// client (init client comune, che aggiunge anche il listener AddLayers al bus del mod) girano solo sul client fisico.
@Mod(RedstoneExoframes.MOD_ID)
public final class RedstoneExoframesNeoForge {
    public RedstoneExoframesNeoForge(IEventBus modBus) {
        RedstoneExoframes.init();
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> RedstoneExoframesClient.init());
    }
}
//?}
