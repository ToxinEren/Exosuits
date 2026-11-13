package com.toxineren.exoframes;

import com.toxineren.exoframes.command.ExoframeCommand;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.content.ModCreativeTabs;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.frame.SuitSize;
import com.toxineren.exoframes.gametest.ExoframeGameTests;
import dev.architectury.event.events.common.TickEvent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Punto d'ingresso comune a tutte le varianti: i bootstrap di piattaforma chiamano {@link #init()}. */
public final class RedstoneExoframes {
    public static final String MOD_ID = "redstone_exoframes";
    public static final Logger LOGGER = LoggerFactory.getLogger("Redstone Exoframes");

    private static volatile ExoframesConfig config = ExoframesConfig.defaults();

    private RedstoneExoframes() {
    }

    public static void init() {
        LOGGER.info("Redstone Exoframes init ({})", MOD_ID);
        Compat.armorMaterials().register(); // prima degli item: su 1.21.1 i pezzi usano il materiale registrato
        ModItems.register();
        ModCreativeTabs.register();
        Compat.frameState().registerCommon();
        ExoframeCommand.register();
        TickEvent.PLAYER_PRE.register(SuitSize::tick); // grandezza reale: ricalcolo quando cambia la tuta, sui due lati
        Compat.gameTests().register(ExoframeGameTests.class); // solo sul server di test (proprieta' del loader), mai in produzione
    }

    /** La configurazione in uso: i valori di specifica finche' non ne viene impostata un'altra con {@link #setConfig}. */
    public static ExoframesConfig config() {
        return config;
    }

    /**
     * Sostituisce la configurazione in uso. La chiamera' il caricatore del file di configurazione di una milestone
     * successiva; gli stati del telaio gia' creati conservano la configurazione con cui sono nati.
     */
    public static void setConfig(ExoframesConfig newConfig) {
        config = Objects.requireNonNull(newConfig, "config");
    }
}
