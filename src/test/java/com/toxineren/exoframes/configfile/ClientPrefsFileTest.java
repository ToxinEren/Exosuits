package com.toxineren.exoframes.configfile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientPrefsFileTest {

    @TempDir
    Path dir;

    private Path file() {
        return dir.resolve(ClientPrefsFile.FILE_NAME);
    }

    private ClientPrefsFile.Loaded loadText(String text) throws IOException {
        Files.writeString(file(), text, UTF_8);
        return ClientPrefsFile.load(file());
    }

    /** I valori letti, senza problemi. */
    private static void assertRead(ClientPrefsFile.Values expected, ClientPrefsFile.Loaded loaded) {
        assertEquals(Optional.empty(), loaded.problem());
        assertEquals(expected, loaded.values());
    }

    private JsonObject savedObject() throws IOException {
        return JsonParser.parseString(Files.readString(file(), UTF_8)).getAsJsonObject();
    }

    private static List<String> namesIn(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(p -> p.getFileName().toString()).collect(Collectors.toList());
        }
    }

    @Test
    void missingFileGivesTheDefaultsAndIsNotCreated() {
        ClientPrefsFile.Loaded loaded = ClientPrefsFile.load(file());
        assertRead(new ClientPrefsFile.Values(false, true), loaded);
        assertEquals(ClientPrefsFile.Values.DEFAULTS, loaded.values());
        assertFalse(Files.exists(file()));
    }

    @Test
    void validFileIsRead() throws IOException {
        assertRead(new ClientPrefsFile.Values(true, false),
                loadText("{\"version\": 1, \"hudHidden\": true, \"vignette\": false}"));
    }

    @Test
    void missingKeysTakeTheirDefaultsSilently() throws IOException {
        assertRead(ClientPrefsFile.Values.DEFAULTS, loadText("{}"));
        assertRead(new ClientPrefsFile.Values(true, true), loadText("{\"hudHidden\": true}"));
        assertRead(new ClientPrefsFile.Values(false, false),
                loadText("{\"version\": 1, \"vignette\": false, \"other\": [1, 2]}"));
    }

    @Test
    void brokenFilesGiveTheDefaultsAndOneProblem() throws IOException {
        Map<String, String> broken = Map.of(
                "{ broken", "is not valid JSON",
                "[true]", "is not a JSON object",
                "{\"hudHidden\": \"yes\"}", "has a wrong type for hudHidden",
                "{\"vignette\": null}", "has a wrong type for vignette",
                "{\"version\": \"one\"}", "has a wrong type for version");
        for (Map.Entry<String, String> entry : broken.entrySet()) {
            assertDefaultsAndProblem(loadText(entry.getKey()), entry.getValue(), entry.getKey());
        }
        Files.delete(file());
        Files.createDirectory(file());
        assertDefaultsAndProblem(ClientPrefsFile.load(file()), "could not be read", "a folder");
    }

    private static void assertDefaultsAndProblem(ClientPrefsFile.Loaded loaded, String start, String what) {
        assertEquals(ClientPrefsFile.Values.DEFAULTS, loaded.values(), what);
        assertTrue(loaded.problem().isPresent(), what);
        assertTrue(loaded.problem().get().startsWith(start), what + ": " + loaded.problem().get());
    }

    @Test
    void savingCreatesTheFileAndItsFolder() throws IOException {
        Path folder = dir.resolve("config");
        Path file = folder.resolve(ClientPrefsFile.FILE_NAME);
        assertEquals(Optional.empty(), ClientPrefsFile.save(file, new ClientPrefsFile.Values(true, false)));
        assertTrue(Files.isRegularFile(file));
        String text = Files.readString(file, UTF_8);
        assertTrue(text.contains("\"version\": 1"), text);
        assertRead(new ClientPrefsFile.Values(true, false), ClientPrefsFile.load(file));
        assertEquals(List.of(ClientPrefsFile.FILE_NAME), namesIn(folder));
    }

    @Test
    void savingKeepsUnknownKeys() throws IOException {
        Files.writeString(file(), "{\"version\": 1, \"hudHidden\": false, \"vignette\": true, \"future\": {\"a\": 1},"
                + " \"note\": \"x\"}", UTF_8);
        assertEquals(Optional.empty(), ClientPrefsFile.save(file(), new ClientPrefsFile.Values(true, true)));
        JsonObject saved = savedObject();
        assertEquals(JsonParser.parseString("{\"a\": 1}"), saved.get("future"));
        assertEquals(new JsonPrimitive("x"), saved.get("note"));
        assertTrue(saved.get("hudHidden").getAsBoolean());
        assertEquals(List.of("version", "hudHidden", "vignette", "future", "note"), List.copyOf(saved.keySet()));

        // Una chiave sconosciuta con il valore null resta anche lei.
        Files.writeString(file(), "{\"later\": null}", UTF_8);
        assertEquals(Optional.empty(), ClientPrefsFile.save(file(), ClientPrefsFile.Values.DEFAULTS));
        JsonElement later = savedObject().get("later");
        assertEquals(JsonNull.INSTANCE, later);
    }

    @Test
    void savingReplacesABrokenFile() throws IOException {
        Files.writeString(file(), "{ broken", UTF_8);
        assertEquals(Optional.empty(), ClientPrefsFile.save(file(), new ClientPrefsFile.Values(false, false)));
        assertRead(new ClientPrefsFile.Values(false, false), ClientPrefsFile.load(file()));
    }

    @Test
    void savingKeepsANewerVersionNumber() throws IOException {
        Map<String, Integer> versions = Map.of("2", 2, "0", 1, "\"x\"", 1);
        for (Map.Entry<String, Integer> entry : versions.entrySet()) {
            Files.writeString(file(), "{\"version\": " + entry.getKey() + "}", UTF_8);
            assertEquals(Optional.empty(), ClientPrefsFile.save(file(), ClientPrefsFile.Values.DEFAULTS),
                    entry.getKey());
            assertEquals(entry.getValue().intValue(), savedObject().get("version").getAsInt(), entry.getKey());
        }
    }

    @Test
    void writeFailureIsAProblemNotAnException() throws IOException {
        Path blocker = dir.resolve("blocker");
        Files.writeString(blocker, "not a folder", UTF_8);
        Optional<String> problem = ClientPrefsFile.save(blocker.resolve(ClientPrefsFile.FILE_NAME),
                new ClientPrefsFile.Values(true, false));
        assertTrue(problem.isPresent());
        assertTrue(problem.get().startsWith("could not be written"), problem.get());
    }
}
