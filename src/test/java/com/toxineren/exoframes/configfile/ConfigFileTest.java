package com.toxineren.exoframes.configfile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.Gson;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigFileTest {

    private static final String NOT_JSON = "{energy: [";
    private static final String WRONG_TYPE = "{\"energy\": {\"cellCapacity\": \"big\", \"walkPerSecond\": 5}}";
    private static final String UNKNOWN_ENUM_KEY =
            "{\"pieces\": {\"shares\": {\"WING\": 0.1}}, \"energy\": {\"walkPerSecond\": 5}}";
    private static final String OUT_OF_RANGE = "{\"energy\": {\"cellCapacity\": 2000000, \"walkPerSecond\": 5}}";
    private static final String DECIMAL_IN_INT = "{\"energy\": {\"walkPerSecond\": 0.5}}";
    private static final String BEYOND_INT = "{\"energy\": {\"cellCapacity\": 4294977296}}";

    @TempDir
    Path dir;

    private Path file() {
        return dir.resolve(ConfigFile.FILE_NAME);
    }

    private ConfigFile.Result loadText(String text) throws IOException {
        Files.writeString(file(), text, UTF_8);
        return ConfigFile.load(file());
    }

    private static void assertDefaults(ExoframesConfig config) {
        Gson gson = new Gson();
        assertEquals(gson.toJson(ExoframesConfig.defaults()), gson.toJson(config));
    }

    private List<Path> listDir() throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.collect(Collectors.toList());
        }
    }

    @Test
    void missingFileWritesTheDefaults() throws IOException {
        ConfigFile.Result r = ConfigFile.load(file());
        assertTrue(r.wroteDefaults());
        assertTrue(r.problems().isEmpty(), r.problems().toString());
        assertDefaults(r.config());
        assertTrue(Files.isRegularFile(file()));
        String text = Files.readString(file(), UTF_8);
        assertTrue(text.contains("\"cellCapacity\": 10000"), text);
        assertTrue(text.contains("\n  "), text);
        assertEquals(List.of(file()), listDir());

        ConfigFile.Result again = ConfigFile.load(file());
        assertFalse(again.wroteDefaults());
        assertTrue(again.problems().isEmpty(), again.problems().toString());
        assertDefaults(again.config());
    }

    @Test
    void validFileIsUsed() throws IOException {
        ConfigFile.Result r = loadText("{\"energy\": {\"cellCapacity\": 20000, \"walkPerSecond\": 5}}");
        assertEquals(20_000, r.config().energy.cellCapacity);
        assertEquals(5, r.config().energy.walkPerSecond);
        assertEquals(2, r.config().energy.idlePerSecond);
        assertTrue(r.problems().isEmpty(), r.problems().toString());
        assertFalse(r.wroteDefaults());
    }

    @Test
    void unreadableFileFallsBackToDefaults() throws IOException {
        Files.createDirectory(file());
        ConfigFile.Result r = ConfigFile.load(file());
        assertEquals(1, r.problems().size(), r.problems().toString());
        assertDefaults(r.config());
        assertFalse(r.wroteDefaults());
        assertTrue(Files.isDirectory(file()));
    }

    @Test
    void notJsonFallsBackToDefaults() throws IOException {
        ConfigFile.Result r = loadText(NOT_JSON);
        assertEquals(1, r.problems().size(), r.problems().toString());
        assertDefaults(r.config());
    }

    @Test
    void wrongTypeFallsBackToDefaults() throws IOException {
        ConfigFile.Result r = loadText(WRONG_TYPE);
        assertEquals(1, r.problems().size(), r.problems().toString());
        assertEquals(8, r.config().energy.walkPerSecond);
        assertDefaults(r.config());
    }

    @Test
    void unknownEnumKeyFallsBackToDefaults() throws IOException {
        ConfigFile.Result r = loadText(UNKNOWN_ENUM_KEY);
        assertEquals(1, r.problems().size(), r.problems().toString());
        assertEquals(8, r.config().energy.walkPerSecond);
        assertDefaults(r.config());
    }

    @Test
    void decimalInAnIntFieldFallsBackToDefaults() throws IOException {
        assertWholeFileRefused(loadText(DECIMAL_IN_INT), "0.5");
    }

    @Test
    void integerBeyondIntFallsBackToDefaults() throws IOException {
        // 2^32 + 10000: troncato a 32 bit diventerebbe 10000, un valore valido.
        assertWholeFileRefused(loadText(BEYOND_INT), "4294977296");
    }

    /** Un numero che un campo intero non puo' tenere e' un tipo sbagliato: un problema, tutto ai predefiniti. */
    private static void assertWholeFileRefused(ConfigFile.Result r, String written) {
        assertEquals(1, r.problems().size(), r.problems().toString());
        assertTrue(r.problems().get(0).startsWith("does not match the config format"), r.problems().toString());
        assertTrue(r.problems().get(0).contains(written), r.problems().toString());
        assertDefaults(r.config());
        assertFalse(r.wroteDefaults());
    }

    @Test
    void nullSectionFallsBackForThatSectionOnly() throws IOException {
        ConfigFile.Result r = loadText("{\"energy\": null, \"frame\": {\"exitHoldSeconds\": 3}}");
        assertEquals(1, r.problems().size(), r.problems().toString());
        assertTrue(r.problems().get(0).startsWith("energy"), r.problems().toString());
        assertEquals(10_000, r.config().energy.cellCapacity);
        assertEquals(3, r.config().frame.exitHoldSeconds);
    }

    @Test
    void missingFieldTakesItsDefault() throws IOException {
        ConfigFile.Result r = loadText("{\"energy\": {\"walkPerSecond\": 5}}");
        assertTrue(r.problems().isEmpty(), r.problems().toString());
        assertEquals(10_000, r.config().energy.cellCapacity);
        assertEquals(2, r.config().energy.idlePerSecond);
    }

    @Test
    void partialMapReportsTheMissingKeys() throws IOException {
        String text = "{\"tiers\": {\"maxDurability\": {\"SCRAP\": 200}}}";
        ConfigFile.Result r = loadText(text);
        ExoframesConfig defaults = ExoframesConfig.defaults();
        assertEquals(200, r.config().tiers.maxDurability.get(Tier.SCRAP));
        for (Tier tier : Tier.values()) {
            if (tier != Tier.SCRAP) {
                int def = defaults.tiers.maxDurability.get(tier);
                assertEquals(def, r.config().tiers.maxDurability.get(tier), tier.name());
                assertTrue(r.problems().contains("tiers.maxDurability." + tier + ": missing, using default " + def),
                        r.problems().toString());
            }
        }
        assertEquals(Tier.values().length - 1, r.problems().size(), r.problems().toString());
        assertEquals(text, Files.readString(file(), UTF_8));
        assertFalse(r.wroteDefaults());
    }

    @Test
    void extraKeysAreIgnored() throws IOException {
        ConfigFile.Result r = loadText("{\"energy\": {\"walkPerSecond\": 5, \"warpDrive\": 9}, \"colour\": \"red\"}");
        assertTrue(r.problems().isEmpty(), r.problems().toString());
        assertEquals(5, r.config().energy.walkPerSecond);
    }

    @Test
    void outOfRangeValuesFallBackOneByOne() throws IOException {
        ConfigFile.Result r = loadText(OUT_OF_RANGE);
        assertEquals(1, r.problems().size(), r.problems().toString());
        assertTrue(r.problems().get(0).startsWith("energy.cellCapacity"), r.problems().toString());
        assertEquals(10_000, r.config().energy.cellCapacity);
        assertEquals(5, r.config().energy.walkPerSecond);
    }

    @Test
    void brokenFileIsNeverRewritten() throws IOException {
        for (String text : List.of(NOT_JSON, WRONG_TYPE, UNKNOWN_ENUM_KEY, OUT_OF_RANGE, DECIMAL_IN_INT, BEYOND_INT)) {
            Files.writeString(file(), text, UTF_8);
            // Un'ora indietro: una riscrittura durante load cambierebbe l'ora anche su un file system a grana grossa.
            FileTime before = FileTime.fromMillis(Files.getLastModifiedTime(file()).toMillis() - 3_600_000L);
            Files.setLastModifiedTime(file(), before);
            byte[] bytes = Files.readAllBytes(file());
            ConfigFile.Result r = ConfigFile.load(file());
            assertFalse(r.problems().isEmpty(), text);
            assertFalse(r.wroteDefaults(), text);
            assertArrayEquals(bytes, Files.readAllBytes(file()), text);
            assertEquals(before, Files.getLastModifiedTime(file()), text);
            assertEquals(List.of(file()), listDir(), text);
        }
    }

    @Test
    void defaultsFileFollowsTheUmask() throws IOException {
        assumeTrue(Files.getFileAttributeView(dir, PosixFileAttributeView.class) != null, "not a POSIX file system");
        ConfigFile.load(file());
        Path reference = dir.resolve("reference.json");
        Files.writeString(reference, "{}", UTF_8);
        assertEquals(Files.getPosixFilePermissions(reference), Files.getPosixFilePermissions(file()));
    }

    @Test
    void writeFailureIsOneProblem() throws IOException {
        Files.writeString(dir.resolve("blocker"), "not a folder", UTF_8);
        ConfigFile.Result r = ConfigFile.load(dir.resolve("blocker").resolve(ConfigFile.FILE_NAME));
        assertFalse(r.wroteDefaults());
        assertEquals(1, r.problems().size(), r.problems().toString());
        assertTrue(r.problems().get(0).contains("could not be written"), r.problems().toString());
        assertDefaults(r.config());
    }
}
