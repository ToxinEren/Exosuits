# Task 8 review (Config da file), cd5b057..2f6d61e

### Spec Compliance

- ✅ Spec compliant. Every item in the brief has its hunk:
  - `configfile/ConfigFile` (ConfigFile.java:36-112): `FILE_NAME`, `Result` with `List.copyOf` (:45-49), `load(Path)` (:55-83). It has the four failure messages exactly as the brief words them (:63, :69, :72, :79). The absent-file branch uses `createTempFile(parent, "redstone_exoframes", ".json.tmp")`, `Files.move` without `REPLACE_EXISTING`, and `deleteIfExists` in `finally` (:85-107). There are no Minecraft imports, no logging and no directives.
  - `ConfigValidation`: the five constants (:42-50). `atMost` overloads apply the limit to exactly the fields the brief lists. Those are cellCapacity (positive, ≤1M), idle/walk/sprint/jumpCost (≤MAX_RATE), the new chargerRePerSecond (positive, ≤MAX_RATE), chargerRePerRedstone (≤1M), cellSwapSeconds and exitHoldSeconds (≤30), brokenTorsoConsumptionMultiplier (atLeastOne, then ≤10) and tiers.protectionMultiplier (≤10, :136). Module costs use `validateCosts` (≤MAX_RATE, :233). The message format matches. The class Javadoc lists the limits and gives the reason for them.
  - `ExoframesConfig.Energy.chargerRePerSecond = 50` sits after `chargerRePerRedstone`. The class Javadoc points to `configfile/ConfigFile`.
  - `RedstoneExoframes.init()` does the file read first, before the INFO line. It writes one `Config file {}: {}` WARN per problem, then calls `setConfig`. The `setConfig` Javadoc is updated.
  - `CarryOverGameTests.configFileExists` is one `add(tests,` line plus its body. `run_gametests.sh` runs `rm -f` after `mkdir -p` and before `./gradlew`, and its header comment says so. `tools/README.md`, `docs/compat.md` (the Platform **Uso** line) and `README.md` (the paragraph after "Comandi") carry the brief's text word for word.
  - JUnit: all 13 `ConfigFileTest` cases from Step 1 are there. The three `ExoframesConfigTest` changes (`defaultsMatchSpec` +50, `upperBoundsFallBackToDefaults`, `chargerRePerSecondDefaultsToFifty`) are there with the brief's values.
- Checks I ran:
  - The game-test run directory is `run-gametest` for all four variants (build.gradle.kts:77, one `gameTestRunDir`), so the `rm -f` path is right everywhere.
  - Null values inside maps (`{"shares": {"HELMET": null}}`) reach `validateShares`/`positive` as null and are already handled (ConfigValidation.java:187-190).
  - Logs left by Step 10 match the report. versions/1.21.1-neoforge/run/logs/latest.log has exactly one exoframe WARN, the `is not valid JSON` line, plus one `Done (`. The 1.20.1-forge latest.log has no exoframe WARN/ERROR. No `Config file` line is in any game-test server.log.
- Implementer concern 2 (the Forge WARN `Object did not get ID it asked for`, redstone_exoframes:exoframe 1274 -> 1275): this is Forge remapping item IDs for the stale world in versions/1.20.1-forge/run/world. It is unrelated to the config. A clean second start, plus the file written in both starts, meets Step 10's purpose (`configDir()` answers in the mod constructor). Not a finding.

### Strengths

- The bound checks are four-argument overloads that call the existing method and then a shared `atMost` (ConfigValidation.java:285-362). The lower-bound messages stay byte for byte the same. A value already reset to its default gets no second message, because the defaults validate cleanly; the second load in `missingFileWritesTheDefaults` proves this.
- `brokenFileIsNeverRewritten` is stronger than the brief asks (ConfigFileTest.java:167-181). It sets the mtime back an hour so a rewrite on a coarse-grained file system would still show, and it also asserts that problems are non-empty and that `wroteDefaults` is false.
- Several tests compare whole objects through `new Gson().toJson`, not single fields: `wrongType`, `unknownEnumKey` and `writeFailure`. `partialMapReportsTheMissingKeys` checks the exact text of each missing-key problem and their exact count.
- Using `toAbsolutePath().getParent()` is a sound guard against a null parent for a single-name relative path. For the absolute `configDir()` path it behaves the same.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

- **ConfigFile.java:76, plan-mandated.** `GSON.fromJson(root, ...)` on the tree from `JsonParser.parseString` silently truncates decimals and wraps out-of-range numbers in `int` fields. The tree path reads ints through `JsonTreeReader.nextInt` → `JsonPrimitive.getAsInt` → `LazilyParsedNumber.intValue()`. That chain does `(int) Long.parseLong(...)` and then `BigDecimal.intValue()` (the low 32 bits), and it never fails.
  - I ran the compiled `ConfigFile` from versions/1.21.1-neoforge/build/classes against gson 2.10.1, and the same against 2.10 (scratchpad probe, nothing changed in the checkout):
    - `{"energy": {"walkPerSecond": 0.5}}` → walkPerSecond 0, no problems. Walking becomes free and the log says nothing.
    - `{"frame": {"exitHoldSeconds": 2.9}}` → 2, no problems.
    - `{"energy": {"cellCapacity": 4294977296}}` → 10000, no problems (2^32 + 10000 wraps into range).
    - `10000000000` → a problem that names `1410065408`, not the written value.
    - `1e12` → `-727379968 is invalid (must be > 0)`.
  - This conflicts with design §12.9: an out-of-limits value takes its default "con una riga fra i problemi", and the log says what is wrong (design goal 41). It also hits Review Focus 4 ("tipo sbagliato", "valori enormi") directly.
  - The brief's Step 4.2 prescribes exactly `GSON.fromJson(root, ExoframesConfig.class)`, which is why this is plan-mandated.
  - Fix: keep `JsonParser.parseString` for the not-JSON and not-an-object checks, then convert from the text with `GSON.fromJson(text, ExoframesConfig.class)`. The streaming `JsonReader.nextInt` throws `NumberFormatException: Expected an int but was 0.5` and `... was 4294977296`, which Gson wraps as `JsonSyntaxException`; verified on 2.10.1. That lands in the existing `does not match the config format` branch: one problem, all defaults.
  - Add JUnit cases for `0.5` and `4294977296` in int fields (one problem each), and add them to `brokenFileIsNeverRewritten`.

#### Minor (Nice to Have)

- **ConfigFile.java:91, plan-mandated (implementer concern 1).** `Files.createTempFile` creates the file as 0600, and `Files.move` keeps that. The defaults file ends up `-rw-------` while the process umask is 0022; versions/1.20.1-forge/run/config/redstone_exoframes.json is `-rw-------` now.
  - Other users or groups can't read the config: a panel or backup user, or an admin group. Every other config file in that folder follows the umask.
  - The brief prescribes `createTempFile`, which is why this is plan-mandated.
  - Fix: create the temporary file under a unique name with `Files.writeString(tmp, text, UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)`, which follows the umask. Keep the same move and `finally`.
- **ExoframesConfigTest.java:171, coverage.** `upperBoundsFallBackToDefaults` covers five fields. These new upper bounds have no test:
  - idle/sprint/jumpCost (ConfigValidation.java:93-97)
  - cellSwapSeconds (:108-109)
  - module costs in `validateCosts` (:233)
  - `tiers.protectionMultiplier` (:136)

  Each is correct when read, but a wrong constant on any of these lines would go unnoticed. The brief does not ask for these tests.

### Assessment

**Task quality:** Needs fixes

**Reasoning:** The diff carries out every requirement of the brief faithfully and its tests are thorough. The one Important issue is plan-mandated: converting through the JSON tree lets decimals and over-sized integers into `int` fields silently, against §12.9 and Review Focus 4. The coordinator should rule on it; the fix is to convert from the text with Gson's streaming reader.
