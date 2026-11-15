package com.toxineren.exoframes.configfile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.toxineren.exoframes.core.config.ConfigValidation;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.List;

/**
 * Il file di configurazione {@code config/redstone_exoframes.json} (design §12.9), letto una volta all'avvio da
 * {@code RedstoneExoframes.init()}: Java con Gson e {@code java.nio}, senza log (i problemi li scrive chi chiama) e
 * senza nulla di Minecraft, quindi provato in JUnit.
 *
 * <ul>
 *   <li>File assente: si scrivono i predefiniti (JSON indentato) in un temporaneo della stessa cartella
 *       ({@code <nome del file>-<numero>.tmp}, creato nuovo con {@code CREATE_NEW}, quindi con i permessi
 *       predefiniti dei file nuovi del processo, 0666 meno la umask, come gli altri file di {@code config/}, e non
 *       con {@code Files.createTempFile}, che su POSIX lo farebbe leggibile solo dal proprietario), poi lo si
 *       sposta sul nome del file senza sostituire (nella stessa cartella e' un cambio di nome: chi legge non vede
 *       mai un file a meta'; se intanto il file e' comparso, lo spostamento fallisce e quel file non si tocca); si
 *       usano i predefiniti. Una scrittura fallita e' un problema, non un'eccezione.</li>
 *   <li>File illeggibile (anche una cartella), non JSON, radice che non e' un oggetto, o che Gson non riesce a
 *       trasformare in config (tipo sbagliato, compreso un numero che un campo intero non puo' tenere, cioe' un
 *       decimale come 0.5 o un intero fuori dall'intervallo di {@code int}; chiave di enumerazione sconosciuta): un
 *       problema, tutta la config ai predefiniti. L'albero di {@code JsonParser} serve solo a dire se il testo e'
 *       JSON e se la radice e' un oggetto; la config si costruisce dal testo, con il lettore a flusso di Gson, che
 *       rifiuta quei numeri: la conversione dall'albero li troncherebbe in silenzio (0.5 diventerebbe 0, un intero
 *       oltre i 32 bit perderebbe i bit alti).</li>
 *   <li>File letto: {@link ConfigValidation#validate} decide valore per valore (sezione nulla, mappa scritta in
 *       parte, valori fuori dai limiti) e il risultato normalizzato e' la config. Un campo assente tiene il valore
 *       del costruttore senza argomenti, che Gson usa; le chiavi in piu' si ignorano.</li>
 * </ul>
 *
 * <p>Il file si scrive solo quando manca: un file con problemi non viene mai riscritto ne' corretto.
 */
public final class ConfigFile {
    public static final String FILE_NAME = "redstone_exoframes.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Esito della lettura: la config da usare, i problemi in inglese (uno per riga di log, vuoto se il file era
     * giusto) e se il file dei predefiniti e' stato appena scritto.
     */
    public record Result(ExoframesConfig config, List<String> problems, boolean wroteDefaults) {
        public Result {
            problems = List.copyOf(problems);
        }
    }

    private ConfigFile() {
    }

    /** Legge {@code file}, o lo scrive con i predefiniti se manca. Non lancia eccezioni per problemi del file. */
    public static Result load(Path file) {
        if (!Files.exists(file)) {
            return writeDefaults(file);
        }
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return fallback("could not be read (" + e + "), using the defaults");
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(text);
        } catch (JsonParseException e) {
            return fallback("is not valid JSON (" + e.getMessage() + "), using the defaults");
        }
        if (!root.isJsonObject()) {
            return fallback("is not a JSON object, using the defaults");
        }
        ExoframesConfig parsed;
        try {
            // Dal testo, non da root: il lettore a flusso rifiuta un decimale o un intero oltre int in un campo int.
            parsed = GSON.fromJson(text, ExoframesConfig.class);
        } catch (RuntimeException e) {
            // Una chiave di enumerazione sconosciuta diventa null e poi NullPointerException in EnumMap.put.
            return fallback("does not match the config format (" + e + "), using the defaults");
        }
        ConfigValidation.ValidationResult validated = ConfigValidation.validate(parsed);
        return new Result(validated.normalized(), validated.problems(), false);
    }

    private static Result writeDefaults(Path file) {
        ExoframesConfig defaults = ExoframesConfig.defaults();
        Path tmp = null;
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            tmp = parent.resolve(file.getFileName() + "-" + Long.toUnsignedString(RANDOM.nextLong()) + ".tmp");
            try {
                // CREATE_NEW: permessi dei file nuovi del processo (la umask), mai un file che c'era gia'.
                Files.writeString(tmp, GSON.toJson(defaults), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException e) {
                tmp = null; // quel file non e' nostro: il finally non lo cancella
                throw e;
            }
            // Senza REPLACE_EXISTING: un file comparso nel frattempo non si sovrascrive.
            Files.move(tmp, file);
            return new Result(defaults, List.of(), true);
        } catch (IOException | RuntimeException e) {
            return new Result(defaults, List.of("could not be written: " + e), false);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // Un temporaneo rimasto non cambia la config in uso.
                }
            }
        }
    }

    private static Result fallback(String problem) {
        return new Result(ExoframesConfig.defaults(), List.of(problem), false);
    }
}
