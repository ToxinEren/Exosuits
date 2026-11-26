package com.toxineren.exoframes.client;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.configfile.ClientPrefsFile;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Le due preferenze dell'HUD in uso nel client (design §5.12 "Tasti" e §5.14): HUD nascosto con H e vignetta del
 * casco. Si leggono una volta all'avvio dal file {@code config/redstone_exoframes-client.json} della cartella di config
 * della piattaforma ({@link ClientPrefsFile}); un file con problemi lascia i predefiniti e un WARN. Ogni cambio fatto
 * con i tasti di {@link HudKeys} riscrive il file subito; una scrittura non riuscita lascia un WARN, e il valore nuovo
 * resta in uso fino alla chiusura del gioco. Solo client. {@link #load()} gira una volta durante l'init della mod, che
 * su Forge e NeoForge e' un thread del caricamento delle mod, prima che il gioco legga i valori; tutto il resto gira
 * sul thread del client.
 */
final class ClientPrefs {
    /** L'inizio del problema di {@link ClientPrefsFile#save}, che il WARN della scrittura non ripete. */
    private static final String WRITE_PROBLEM = "could not be written: ";

    private static Path file;
    private static ClientPrefsFile.Values values = ClientPrefsFile.Values.DEFAULTS;

    private ClientPrefs() {
    }

    /**
     * Legge il file delle preferenze. Una volta, da {@link RedstoneExoframesClient#init()}, prima dei tasti: durante
     * l'init della mod, su Forge e NeoForge in un thread del caricamento delle mod.
     */
    static void load() {
        file = Compat.platform().configDir().resolve(ClientPrefsFile.FILE_NAME);
        ClientPrefsFile.Loaded loaded = ClientPrefsFile.load(file);
        loaded.problem().ifPresent(problem -> RedstoneExoframes.LOGGER.warn(
                "Client preferences file {} {}; using the defaults: the next change with the HUD keys replaces it",
                file, problem));
        values = loaded.values();
    }

    /** Vero se il giocatore ha nascosto l'HUD con H. */
    static boolean hudHidden() {
        return values.hudHidden();
    }

    /** Vero se la vignetta del casco e' accesa. */
    static boolean vignette() {
        return values.vignette();
    }

    /** Nasconde o mostra l'HUD, scrive il file e restituisce il valore nuovo (vero: nascosto). */
    static boolean toggleHud() {
        values = new ClientPrefsFile.Values(!values.hudHidden(), values.vignette());
        save();
        return values.hudHidden();
    }

    /** Accende o spegne la vignetta, scrive il file e restituisce il valore nuovo (vero: accesa). */
    static boolean toggleVignette() {
        values = new ClientPrefsFile.Values(values.hudHidden(), !values.vignette());
        save();
        return values.vignette();
    }

    private static void save() {
        Optional<String> problem = ClientPrefsFile.save(file, values);
        problem.ifPresent(p -> RedstoneExoframes.LOGGER.warn(
                "Client preferences file {} could not be written ({}); the change lasts until the game closes", file,
                p.startsWith(WRITE_PROBLEM) ? p.substring(WRITE_PROBLEM.length()) : p));
    }
}
