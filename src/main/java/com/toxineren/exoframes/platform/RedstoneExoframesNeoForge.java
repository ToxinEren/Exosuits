package com.toxineren.exoframes.platform;

//? if neoforge {
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

// Entrypoint NeoForge: Architectury 13 non richiede la registrazione del bus del mod; le registrazioni solo
// client girano solo sul client.
@Mod(RedstoneExoframes.MOD_ID)
public final class RedstoneExoframesNeoForge {
    public RedstoneExoframesNeoForge(IEventBus modBus) {
        RedstoneExoframes.init();
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> Compat.frameState().registerClient());
    }
}
//?}
