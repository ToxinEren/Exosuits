package com.toxineren.exoframes.configfile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Optional;

/**
 * Il file delle preferenze del client {@code config/redstone_exoframes-client.json} (design §5.14), con le due scelte
 * dei tasti dell'HUD: HUD nascosto e vignetta del casco. {@code ClientPrefs} lo legge una volta all'avvio del client e
 * lo riscrive a ogni cambio fatto con i tasti; qui c'e' solo Java con Gson e {@code java.nio}, senza log (i problemi
 * li scrive chi chiama) e senza nulla di Minecraft, quindi provato in JUnit.
 *
 * <ul>
 *   <li>File assente: i predefiniti ({@link Values#DEFAULTS}: HUD visibile, vignetta accesa), nessun problema; il
 *       file non si crea alla lettura, nasce al primo cambio.</li>
 *   <li>File illeggibile (anche una cartella), non JSON, radice che non e' un oggetto, {@code version} presente ma non
 *       un numero, {@code hudHidden} o {@code vignette} presenti ma non {@code true} o {@code false} (anche
 *       {@code null}): un problema, entrambe le preferenze ai predefiniti. Chi chiama lo scrive nel log, e il primo
 *       cambio sostituisce il file con uno valido.</li>
 *   <li>Chiave assente: il suo predefinito, in silenzio; le chiavi sconosciute si ignorano. Il numero di
 *       {@code version} non cambia la lettura: il file di una versione piu' nuova si legge come questo.</li>
 *   <li>Scrittura ({@link #save}): si parte dall'oggetto JSON del file, se il file si legge come un oggetto, altrimenti
 *       da un oggetto nuovo (il file rovinato si sostituisce); si mettono {@code version} ({@link #VERSION}, salvo un
 *       numero maggiore gia' nel file, che resta: il file di una versione piu' nuova non si declassa),
 *       {@code hudHidden} e {@code vignette}, e ogni altra chiave resta com'e' e al suo posto, anche con il valore
 *       {@code null}. Il JSON indentato va in un temporaneo nuovo della stessa cartella
 *       ({@code <nome del file>-<numero>.tmp}, {@code CREATE_NEW}, con la cartella creata se manca), poi spostato sul
 *       nome del file sostituendolo, con uno spostamento atomico se il file system lo permette: chi legge non vede mai
 *       un file a meta'. Il temporaneo si cancella in ogni caso. Una scrittura fallita e' un problema, non
 *       un'eccezione.</li>
 * </ul>
 *
 * <p>A differenza di {@link ConfigFile}, che non riscrive mai un file che c'e', questo file la mod lo riscrive: e' uno
 * stato del client, cambiato con i tasti, non una configurazione scritta a mano.
 */
public final class ClientPrefsFile {
    public static final String FILE_NAME = "redstone_exoframes-client.json";
    /** La versione del formato che questo client scrive. */
    public static final int VERSION = 1;

    private static final String KEY_VERSION = "version";
    private static final String KEY_HUD_HIDDEN = "hudHidden";
    private static final String KEY_VIGNETTE = "vignette";

    // serializeNulls: una chiave sconosciuta con il valore null resta nel file riscritto (senza, Gson la toglierebbe).
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Le due preferenze: HUD nascosto con H, vignetta del casco accesa. */
    public record Values(boolean hudHidden, boolean vignette) {
        /** HUD visibile e vignetta accesa (design §5.14 e §9, domanda 10). */
        public static final Values DEFAULTS = new Values(false, true);
    }

    /** Esito della lettura: le preferenze da usare e il problema in inglese, vuoto se il file era giusto o assente. */
    public record Loaded(Values values, Optional<String> problem) {
    }

    private ClientPrefsFile() {
    }

    /**
     * Legge {@code file}; un file assente da' i predefiniti e non si crea. Non lancia eccezioni per problemi del file.
     */
    public static Loaded load(Path file) {
        if (!Files.exists(file)) {
            return new Loaded(Values.DEFAULTS, Optional.empty());
        }
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            return fallback("could not be read (" + e + ")");
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(text);
        } catch (JsonParseException e) {
            return fallback("is not valid JSON (" + e.getMessage() + ")");
        }
        if (!root.isJsonObject()) {
            return fallback("is not a JSON object");
        }
        JsonObject object = root.getAsJsonObject();
        JsonElement version = object.get(KEY_VERSION);
        if (version != null && !(version.isJsonPrimitive() && version.getAsJsonPrimitive().isNumber())) {
            return fallback("has a wrong type for " + KEY_VERSION);
        }
        for (String key : new String[] {KEY_HUD_HIDDEN, KEY_VIGNETTE}) {
            JsonElement value = object.get(key);
            if (value != null && !isBoolean(value)) {
                return fallback("has a wrong type for " + key);
            }
        }
        return new Loaded(new Values(read(object, KEY_HUD_HIDDEN, Values.DEFAULTS.hudHidden()),
                read(object, KEY_VIGNETTE, Values.DEFAULTS.vignette())), Optional.empty());
    }

    /**
     * Scrive {@code values} in {@code file}, tenendo le chiavi che questo client non conosce. Vuoto se il file e' stato
     * scritto, altrimenti il problema in inglese; non lancia eccezioni.
     */
    public static Optional<String> save(Path file, Values values) {
        Path tmp = null;
        try {
            JsonObject object = existingObject(file).orElseGet(JsonObject::new);
            if (!isNewerVersion(object.get(KEY_VERSION))) {
                object.addProperty(KEY_VERSION, VERSION);
            }
            object.addProperty(KEY_HUD_HIDDEN, values.hudHidden());
            object.addProperty(KEY_VIGNETTE, values.vignette());
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            tmp = parent.resolve(file.getFileName() + "-" + Long.toUnsignedString(RANDOM.nextLong()) + ".tmp");
            try {
                // CREATE_NEW: mai un file che c'era gia', con i permessi dei file nuovi del processo (la umask).
                Files.writeString(tmp, GSON.toJson(object), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException e) {
                tmp = null; // quel file non e' nostro: il finally non lo cancella
                throw e;
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return Optional.empty();
        } catch (IOException | RuntimeException e) {
            return Optional.of("could not be written: " + e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // Un temporaneo rimasto non cambia le preferenze in uso.
                }
            }
        }
    }

    /** L'oggetto JSON del file, se il file c'e' e si legge come un oggetto; altrimenti vuoto (si riparte da zero). */
    private static Optional<JsonObject> existingObject(Path file) {
        try {
            if (!Files.isRegularFile(file)) {
                return Optional.empty();
            }
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            return root.isJsonObject() ? Optional.of(root.getAsJsonObject()) : Optional.empty();
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Vero se il valore e' un numero maggiore di {@link #VERSION}: il numero di una versione piu' nuova resta. */
    private static boolean isNewerVersion(JsonElement version) {
        if (version == null || !version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()) {
            return false;
        }
        try {
            return version.getAsDouble() > VERSION;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isBoolean(JsonElement value) {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
    }

    private static boolean read(JsonObject object, String key, boolean fallback) {
        JsonElement value = object.get(key);
        return value == null ? fallback : value.getAsBoolean();
    }

    private static Loaded fallback(String problem) {
        return new Loaded(Values.DEFAULTS, Optional.of(problem));
    }
}
