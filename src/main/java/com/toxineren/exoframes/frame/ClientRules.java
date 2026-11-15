package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.core.config.ConfigValidation;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import dev.architectury.event.events.common.PlayerEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * I valori della config del server che il client conta da se' (design §12.4): la capacita' della cella e i secondi dei
 * due tasti da tenere, arrivati con il pacchetto {@code client_rules} ({@link ClientRulesPacket}) quando il giocatore
 * entra nel server. Ogni lettura restituisce il valore ricevuto dall'ultimo {@link #reset()}, altrimenti quello della
 * config locale ({@link RedstoneExoframes#config()}).
 * <p>
 * Lo legge il client: {@code ExitKey}, poi {@code CellSwapKey} e le barre degli oggetti. Il codice del server
 * (intervallo del cambio cella, celle, registro, caricatore) non lo legge mai e usa la propria config. Nel gioco in
 * singolo arrivano gli stessi valori dal server integrato; su un server dedicato o in un GameTest nessuno chiama
 * {@link #accept}, quindi vale la config locale.
 * <p>
 * Il client controlla ogni valore ricevuto con i limiti della config ({@link ConfigValidation}): un valore fuori dai
 * limiti vale il predefinito di {@link ExoframesConfig#defaults()}, con una riga WARN una volta per nome nella vita del
 * processo. Codice comune: nessuna classe del client; l'azzeramento all'uscita dal mondo lo registra il client.
 */
public final class ClientRules {
    /** I valori ricevuti, gia' controllati; {@code null} = nulla ricevuto dall'ultimo azzeramento. */
    private static volatile Received received;

    /** I nomi dei valori per cui la WARN del valore fuori dai limiti e' gia' stata scritta. */
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private ClientRules() {
    }

    /**
     * Registra il canale S2C (nell'init comune, sui due lati) e l'invio dei valori della config del server a ogni
     * giocatore che entra. Una volta, da {@link RedstoneExoframes#init()}.
     */
    public static void register() {
        Compat.net().registerS2C(ClientRulesPacket.ID, ClientRulesPacket.class, ClientRulesPacket.CODEC,
                (msg, ctx) -> accept(msg.cellCapacity(), msg.cellSwapSeconds(), msg.exitHoldSeconds()));
        PlayerEvent.PLAYER_JOIN.register(player -> Compat.net().sendToPlayer(player, ClientRulesPacket.ID,
                ClientRulesPacket.of(RedstoneExoframes.config())));
    }

    /**
     * Controlla e tiene i valori ricevuti dal server. Limiti, gli stessi della config: {@code cellCapacity} in
     * {@code [1, MAX_CELL_CAPACITY]}, i due secondi in {@code [0, MAX_HOLD_SECONDS]}; un valore fuori vale il
     * predefinito di {@link ExoframesConfig#defaults()} (non la config locale).
     */
    public static void accept(int cellCapacity, int cellSwapSeconds, int exitHoldSeconds) {
        ExoframesConfig defaults = ExoframesConfig.defaults();
        received = new Received(
                checked("cellCapacity", cellCapacity, 1, ConfigValidation.MAX_CELL_CAPACITY,
                        defaults.energy.cellCapacity),
                checked("cellSwapSeconds", cellSwapSeconds, 0, ConfigValidation.MAX_HOLD_SECONDS,
                        defaults.energy.cellSwapSeconds),
                checked("exitHoldSeconds", exitHoldSeconds, 0, ConfigValidation.MAX_HOLD_SECONDS,
                        defaults.frame.exitHoldSeconds));
    }

    /** Dimentica i valori ricevuti: da qui vale la config locale. Le WARN gia' scritte restano scritte. */
    public static void reset() {
        received = null;
    }

    /** La capacita' della cella: quella ricevuta dal server, altrimenti {@code energy.cellCapacity} locale. */
    public static int cellCapacity() {
        Received r = received;
        return r != null ? r.cellCapacity() : RedstoneExoframes.config().energy.cellCapacity;
    }

    /** I secondi del cambio cella: quelli ricevuti dal server, altrimenti {@code energy.cellSwapSeconds} locale. */
    public static int cellSwapSeconds() {
        Received r = received;
        return r != null ? r.cellSwapSeconds() : RedstoneExoframes.config().energy.cellSwapSeconds;
    }

    /** I secondi dell'uscita: quelli ricevuti dal server, altrimenti {@code frame.exitHoldSeconds} locale. */
    public static int exitHoldSeconds() {
        Received r = received;
        return r != null ? r.exitHoldSeconds() : RedstoneExoframes.config().frame.exitHoldSeconds;
    }

    private static int checked(String name, int value, int min, int max, int fallback) {
        if (value >= min && value <= max) {
            return value;
        }
        if (WARNED.add(name)) {
            RedstoneExoframes.LOGGER.warn("Received client rule {} = {} out of range, using {}", name, value, fallback);
        }
        return fallback;
    }

    private record Received(int cellCapacity, int cellSwapSeconds, int exitHoldSeconds) {
    }
}
