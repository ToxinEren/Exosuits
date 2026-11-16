package com.toxineren.exoframes;

import com.toxineren.exoframes.command.ExoframeCommand;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.configfile.ConfigFile;
import com.toxineren.exoframes.content.ModBlockEntities;
import com.toxineren.exoframes.content.ModBlocks;
import com.toxineren.exoframes.content.ModCreativeTabs;
import com.toxineren.exoframes.content.ModEntities;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.core.armor.DamageRouter;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.frame.FrameRules;
import com.toxineren.exoframes.frame.CellSwap;
import com.toxineren.exoframes.frame.ClientRules;
import com.toxineren.exoframes.frame.EntryHints;
import com.toxineren.exoframes.frame.FrameEntry;
import com.toxineren.exoframes.frame.FrameRealign;
import com.toxineren.exoframes.frame.SafeSpots;
import com.toxineren.exoframes.frame.SlotGuard;
import com.toxineren.exoframes.frame.SuitDamage;
import com.toxineren.exoframes.frame.SuitRestrictions;
import com.toxineren.exoframes.frame.SuitSize;
import com.toxineren.exoframes.frame.SuitTick;
import com.toxineren.exoframes.gametest.CarryOverGameTests;
import com.toxineren.exoframes.gametest.CellGameTests;
import com.toxineren.exoframes.gametest.ChargerGameTests;
import com.toxineren.exoframes.gametest.DamageGameTests;
import com.toxineren.exoframes.gametest.DataGameTests;
import com.toxineren.exoframes.gametest.EnergyGameTests;
import com.toxineren.exoframes.gametest.ExoframeGameTests;
import com.toxineren.exoframes.gametest.MovementGameTests;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;
import java.nio.file.Path;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Punto d'ingresso comune a tutte le varianti: i bootstrap di piattaforma chiamano {@link #init()}. */
public final class RedstoneExoframes {
    public static final String MOD_ID = "redstone_exoframes";
    public static final Logger LOGGER = LoggerFactory.getLogger("Redstone Exoframes");

    private static volatile ExoframesConfig config = ExoframesConfig.defaults();
    private static volatile FrameRules rules = new FrameRules(config);
    private static volatile DamageRouter damageRouter = new DamageRouter(config.damage, config.pieces);

    private RedstoneExoframes() {
    }

    public static void init() {
        // Config dal file, prima di ogni registrazione (design §12.9): il registro dei telai la prende quando si
        // carica, con il mondo. Letta una volta sola: nessun ricaricamento, ne' comando ne' /reload.
        Path configFile = Compat.platform().configDir().resolve(ConfigFile.FILE_NAME);
        ConfigFile.Result loaded = ConfigFile.load(configFile);
        for (String problem : loaded.problems()) {
            LOGGER.warn("Config file {}: {}", configFile, problem);
        }
        setConfig(loaded.config());
        LOGGER.info("Redstone Exoframes init ({})", MOD_ID);
        Compat.armorMaterials().register(); // prima degli item: su 1.21.1 i pezzi usano il materiale registrato
        ModEntities.register(); // prima degli item: l'oggetto che piazza il telaio usa il tipo dell'entita'
        ModBlocks.register(); // prima degli item: l'oggetto del caricatore usa il suo blocco
        ModItems.register();
        ModBlockEntities.register(); // dopo i blocchi: il tipo della block entity nasce con il blocco del caricatore
        ModCreativeTabs.register();
        Compat.frameState().registerCommon();
        FrameEntry.registerNet(); // pacchetto C2S dell'uscita dal telaio, su entrambi i lati
        ClientRules.register(); // pacchetto S2C con i valori del server che il client conta, mandato a chi entra
        CellSwap.registerNet(); // pacchetto C2S del cambio cella, su entrambi i lati
        EntryHints.register(); // la riga dei tasti torna a mostrarsi dopo l'uscita dal server
        FrameRealign.register(); // registro ed entita' dei telai piazzati allineati, ogni 5 secondi per livello
        ExoframeCommand.register();
        TickEvent.PLAYER_PRE.register(SuitSize::tick); // grandezza reale: ricalcolo quando cambia la tuta, sui due lati
        SafeSpots.register(); // ultima posizione sicura di chi indossa un telaio, ogni secondo, solo sul server
        SlotGuard.register(); // segnaposto allineati al registro a ogni tick, lancio dei segnaposto annullato
        // tick dell'energia su PLAYER_PRE, solo sul server: li' la posizione e' l'ultima accettata dal client; i
        // giocatori finti non ricevono l'evento, e i test chiamano SuitTick.tick a mano
        SuitTick.register();
        SuitRestrictions.register(); // con la tuta il letto si rifiuta, sui due lati e per le due mani
        SuitDamage.register(); // usura dei pezzi: segno della riga ERROR all'uscita; caduta annullata, consuma le gambe
        // la morte nel telaio: in testa a ServerPlayer.die, prima delle cadute; non annulla mai la morte
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                FrameEntry.onDeath(player);
            }
            return EventResult.pass();
        });
        Compat.gameTests().register(ExoframeGameTests.class); // solo sul server di test (proprieta' del loader), mai in produzione
        Compat.gameTests().register(CarryOverGameTests.class);
        Compat.gameTests().register(EnergyGameTests.class);
        Compat.gameTests().register(DamageGameTests.class);
        Compat.gameTests().register(MovementGameTests.class);
        Compat.gameTests().register(CellGameTests.class);
        Compat.gameTests().register(ChargerGameTests.class);
        Compat.gameTests().register(DataGameTests.class);
    }

    /** La configurazione in uso: i valori di specifica finche' non ne viene impostata un'altra con {@link #setConfig}. */
    public static ExoframesConfig config() {
        return config;
    }

    /**
     * Le regole del telaio costruite con la configurazione in uso: l'unica istanza, che {@code syncTo} (indicatori del
     * pacchetto al proprietario) e {@code SuitTick} usano. Nessun altro fa {@code new FrameRules}: {@link #setConfig}
     * la ricostruisce con la config nuova, e {@link #init()} chiama {@code setConfig} una volta, dopo la lettura del
     * file.
     */
    public static FrameRules rules() {
        return rules;
    }

    /**
     * Il ripartitore del danno fra i pezzi costruito con la configurazione in uso: un'istanza sola, che
     * {@code SuitDamage} usa e non costruisce mai. {@link #setConfig} lo ricostruisce con la config nuova, accanto a
     * {@link #rules()}.
     */
    public static DamageRouter damageRouter() {
        return damageRouter;
    }

    /**
     * Sostituisce la configurazione in uso e ricostruisce con essa le regole di {@link #rules()} e il ripartitore di
     * {@link #damageRouter()}. La chiama {@link #init()} una volta all'avvio, con la config letta da
     * {@code config/redstone_exoframes.json} ({@code configfile/ConfigFile}), prima di ogni registrazione. Gli stati
     * del telaio gia' creati conservano la configurazione con cui sono nati.
     */
    public static void setConfig(ExoframesConfig newConfig) {
        config = Objects.requireNonNull(newConfig, "config");
        rules = new FrameRules(newConfig);
        damageRouter = new DamageRouter(newConfig.damage, newConfig.pieces);
    }
}
