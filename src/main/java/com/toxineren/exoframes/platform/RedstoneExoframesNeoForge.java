package com.toxineren.exoframes.platform;

//? if neoforge {
import com.toxineren.exoframes.RedstoneExoframes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

// Entrypoint NeoForge: Architectury 13 non richiede la registrazione del bus del mod.
@Mod(RedstoneExoframes.MOD_ID)
public final class RedstoneExoframesNeoForge {
    public RedstoneExoframesNeoForge(IEventBus modBus) {
        RedstoneExoframes.init();
    }
}
//?}
