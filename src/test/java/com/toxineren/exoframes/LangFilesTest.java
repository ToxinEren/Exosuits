package com.toxineren.exoframes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Controllo senza gioco dei due file di lingua: stesse chiavi nello stesso ordine, nessuna chiave scritta due volte,
 * gli stessi segnaposto nelle due lingue, nessun testo vuoto. I file si leggono con {@link JsonReader}, un nome alla
 * volta: l'albero di Gson terrebbe solo l'ultimo valore di una chiave ripetuta e la nasconderebbe.
 */
class LangFilesTest {

    private static final String EN_US = "/assets/redstone_exoframes/lang/en_us.json";
    private static final String IT_IT = "/assets/redstone_exoframes/lang/it_it.json";

    /** Una riga di un file di lingua. */
    private record Entry(String key, String value) {
    }

    private static List<Entry> english;
    private static List<Entry> italian;

    @BeforeAll
    static void load() throws IOException {
        english = read(EN_US);
        italian = read(IT_IT);
    }

    /** Le righe del file nell'ordine in cui sono scritte, ripetizioni comprese. */
    private static List<Entry> read(String resource) throws IOException {
        List<Entry> entries = new ArrayList<>();
        try (InputStream in = LangFilesTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, resource);
            try (JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                reader.beginObject();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    entries.add(new Entry(key, reader.nextString()));
                }
                reader.endObject();
            }
        }
        assertFalse(entries.isEmpty(), resource + " has no keys");
        return entries;
    }

    private static List<String> keys(List<Entry> entries) {
        return entries.stream().map(Entry::key).toList();
    }

    /** Quanti {@code %s} ha il testo; {@code %%} e' un segno di percento e non conta. */
    private static int placeholders(String value) {
        int count = 0;
        for (int i = 0; i + 1 < value.length(); i++) {
            if (value.charAt(i) != '%') {
                continue;
            }
            if (value.charAt(i + 1) == 's') {
                count++;
            }
            i++; // il carattere dopo il percento appartiene a questo percento ("%%s" non e' un segnaposto)
        }
        return count;
    }

    @Test
    void sameKeysInTheSameOrder() {
        List<String> en = keys(english);
        List<String> it = keys(italian);
        for (int i = 0; i < Math.min(en.size(), it.size()); i++) {
            assertEquals(en.get(i), it.get(i),
                    "first difference at position " + i + ": en_us has '" + en.get(i) + "', it_it has '" + it.get(i)
                            + "'");
        }
        if (en.size() != it.size()) {
            boolean moreEnglish = en.size() > it.size();
            List<String> longer = moreEnglish ? en : it;
            int first = Math.min(en.size(), it.size());
            assertEquals(en.size(), it.size(), "key '" + longer.get(first) + "' is only in "
                    + (moreEnglish ? "en_us" : "it_it") + " (first key beyond the end of the other file)");
        }
    }

    @Test
    void noKeyIsWrittenTwice() {
        assertNoRepeatedKey(EN_US, english);
        assertNoRepeatedKey(IT_IT, italian);
    }

    private static void assertNoRepeatedKey(String resource, List<Entry> entries) {
        Set<String> seen = new HashSet<>();
        for (Entry entry : entries) {
            assertTrue(seen.add(entry.key()), "key '" + entry.key() + "' is written twice in " + resource);
        }
    }

    @Test
    void samePlaceholdersInBothLanguages() {
        for (int i = 0; i < Math.min(english.size(), italian.size()); i++) {
            Entry en = english.get(i);
            Entry it = italian.get(i);
            if (!en.key().equals(it.key())) {
                continue; // lo dice sameKeysInTheSameOrder, con la chiave
            }
            assertEquals(placeholders(en.value()), placeholders(it.value()),
                    "key '" + en.key() + "': %s placeholders differ between en_us and it_it");
        }
    }

    @Test
    void noValueIsEmpty() {
        assertNoEmptyValue(EN_US, english);
        assertNoEmptyValue(IT_IT, italian);
    }

    private static void assertNoEmptyValue(String resource, List<Entry> entries) {
        for (Entry entry : entries) {
            assertFalse(entry.value().isBlank(), "key '" + entry.key() + "' has an empty value in " + resource);
        }
    }
}
