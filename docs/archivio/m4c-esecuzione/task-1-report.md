# Task 1 report: le regole pure dell'HUD (HudLevel, HudModel, HudLayout)

Status: DONE_WITH_CONCERNS (all checks pass; one design-level observation on the vignette at 320x240, below). Commit 6f195e8.

## What I implemented

- `core/frame/HudLevel` (public enum, Java 17, only core types): `FULL`, `BAR_ONLY`, `NONE`,
  `of(boolean wearsSuit, Optional<FrameState> view)`. `NONE` without the suit or the view; `FULL` with a mounted
  helmet whose condition is not `BROKEN` (critical still works); `BAR_ONLY` otherwise. The cell does not count.
  `view` null -> NullPointerException. No reference to the client package (rule 7); SuitHud named bare in Javadoc.
- `client/HudModel` (package-private record, only core types): the nine components of "Tipi nuovi"; `Paint(fill,
  outline)`; the compact constructor copies the silhouette into an unmodifiable `EnumMap` (via `putAll`, because
  `new EnumMap<>(map)` rejects an empty non-EnumMap) and rejects `spareCells < 0`, `percent` outside [0, 100],
  `fillWidth` outside [0, FILL_MAX]; `NONE`; the legend `GREEN YELLOW RED GREY` and `WHITE`; the nine drawing
  colours; `ALL_COLOURS` (legend + white + nine + 4 outlines + 4 lighter rows of the energy colours; lighter(WHITE)
  is WHITE itself, listed as the plan says); `FILL_MAX = 61`, `BLINK_TICKS = 10`; `conditionColor`, `energyColor`
  (Javadoc: question 9), `outline`, `lighter`, `ticksInState`, `warningOn`, `of(...)` (NONE -> the NONE constant,
  state may be null; otherwise NullPointerException on a null state), `spareColor()`. Each constant's Javadoc names
  the design line it comes from (§5.12 "Colori"/"Elementi" rows; the three cell-icon colours, which the design does
  not give, from the §12 preview script anteprime.py, draw_cell_glyph: (60,63,68), (150,150,150), redstone red).
- `client/HudLayout` (final, package-private): `Rect(x0, y0, x1, y1)` with `width`, `height`, `intersects`,
  `contains`, `offset`; `screen(Rect, int)`; every constant of "Tipi nuovi" and of the "Disposizione dell'HUD" table
  (`GROUP_W/H`, `SILHOUETTE_W`, `ENERGY_W`, `EDGE`, `ABOVE_BOTTOM`, `NARROW_WIDTH`, `VIGNETTE_W/H`, `WARNING`,
  `TEXT_X`, `TEXT_X_AFTER_WARNING`, `TEXT_Y`, `PERCENT_AREA`, `SPARE_RIGHT`, `CELL_ICON_W/H`, `SPARE_AREA`,
  `BAR_FRAME`, `BAR_FILL`, `NO_CELL_X` 2, `NO_CELL_Y` 20 as ruling R1 says); `narrow`, `group` (Javadoc: question
  8), `silhouetteGroup`, `energyGroup`, `plate` (NONE -> IllegalArgumentException), `piece`, `spareTextX`,
  `vignetteCorners`. No public name outside the plan's list (Task 3 consumes them); the text row (2,8,65,17) has no
  constant in the plan, so the test builds it from TEXT_X, TEXT_Y and SPARE_RIGHT.
- `client/StationLayout`: `CONDITION_INTACT/_DAMAGED/_CRITICAL/_BROKEN` are now `HudModel.GREEN/YELLOW/RED/GREY`;
  `conditionColor` returns `HudModel.conditionColor(condition)` (Javadoc: "la legenda e' quella dell'HUD, design
  §5.3 e §5.12"); the now unused `java.util.Objects` import is gone. `StationLayoutTest` is unchanged and passes.
  Every line of the touched and new files is within 120 characters; only ASCII plus the `§` already used.

## Tests

- `HudLevelTest` (3): `fullWithAWorkingHelmet`, `barOnlyWithoutAWorkingHelmet`, `noneWithoutTheSuitOrTheView`, with
  the premise conditions asserted (90 DAMAGED, 1 CRITICAL, 0 BROKEN on max 180).
- `HudModelTest` (10): the brief's ten, with its exact values. Small additions inside them, all specified behaviour:
  the constructor's percent/fill range checks (in `percentRoundsDown...`), the silhouette map is unmodifiable (in
  `silhouetteColours...`), NullPointerException for a null state with BAR_ONLY (in `barOnlyCarriesNoSilhouette`).
- `HudLayoutTest` (7): the brief's seven. Vanilla elements computed in the test with doubles, as the plan says (arm
  `w/2 + 97k` to the side, top `h - 102.5k`, `k = h/270`; hotbar band `w/2 -+ 95`, from `h - 50`; the offhand slot
  of the main hand's side; chat `0..332 x h-130..h-40`; crosshair `+-15` around the centre). Additions:
  `BAR_FILL.width() == HudModel.FILL_MAX` (the model's widest fill is the bar's), the percentage after the warning
  does not touch the warning, the no-cell text starts at BAR_FRAME's corner and ends by 65.
- The vignette test asserts exactly what the brief says: at the five wide sizes no corner touches the band, the two
  offhand slots, the chat or the crosshair; at 341x256 and 320x240 the bottom corners pass over the offhand slots.
  It says nothing about the band at 320x240 (see the concern below).

## Orientation

- Branch m4-stazione, HEAD e755e05, working tree clean at start.
- Layout pre-check (Python, decimals, the plan's vanilla sizes): the group and the BAR_ONLY plate touch nothing at
  the seven sizes, both hands. Script and output: 
  <cartella temporanea>/m4c/t1/layout_check.py
- Finding (not a failure of any test the brief specifies): at 320x240 the two bottom vignette corners overlap the
  hotbar band (w/2 - 95 = 65 < 72 on the left; 248 < w/2 + 95 = 255 on the right), and the vanilla hotbar itself
  (w/2 - 91 = 69 < 72). Design §5.12 names only the offhand slot below 384. At 341x256 they touch only the offhand.

## TDD evidence

### RED (tests written first; no main class yet, StationLayout untouched)

Command: `./gradlew --console=plain :1.21.1-neoforge:test --tests '*HudLevelTest' --tests '*HudModelTest' --tests '*HudLayoutTest'`
Log: <cartella temporanea>/m4c/t1/red.log (4m12s)

    HudLayoutTest.java:8: error: package com.toxineren.exoframes.client.HudLayout does not exist
    HudModelTest.java:9: error: package com.toxineren.exoframes.client.HudModel does not exist
    HudLevelTest.java:57: error: cannot find symbol   (class HudLevel)
    missing symbols: class HudLevel 4, class HudModel 1, class Rect 37, variable HudLayout 45, variable HudLevel 5,
    variable HudModel 1 -> 100 errors; BUILD FAILED in 4m 8s

Expected: the brief says "FAIL (non compila: i tre tipi non esistono)"; the only missing symbols are the three types.

### GREEN

Command: `./gradlew --console=plain :1.21.1-neoforge:test --tests '*HudLevelTest' --tests '*HudModelTest' --tests '*HudLayoutTest' --tests '*StationLayoutTest'`
Log: <cartella temporanea>/m4c/t1/green.log (BUILD SUCCESSFUL in 12s)
From versions/1.21.1-neoforge/build/test-results/test/TEST-*.xml (written 16:23:43 UTC):

    com.toxineren.exoframes.core.frame.HudLevelTest tests=3 failures=0 errors=0
    com.toxineren.exoframes.client.HudModelTest tests=10 failures=0 errors=0
    com.toxineren.exoframes.client.HudLayoutTest tests=7 failures=0 errors=0
    com.toxineren.exoframes.client.StationLayoutTest tests=7 failures=0 errors=0   (unchanged file, still passes)

## Files changed

- Created: `src/main/java/com/toxineren/exoframes/core/frame/HudLevel.java`,
  `src/main/java/com/toxineren/exoframes/client/HudModel.java`,
  `src/main/java/com/toxineren/exoframes/client/HudLayout.java`,
  `src/test/java/com/toxineren/exoframes/core/frame/HudLevelTest.java`,
  `src/test/java/com/toxineren/exoframes/client/HudModelTest.java`,
  `src/test/java/com/toxineren/exoframes/client/HudLayoutTest.java`.
- Modified: `src/main/java/com/toxineren/exoframes/client/StationLayout.java` (constants from HudModel, delegation,
  one import removed). `StationLayoutTest` untouched.

## Self-review

- Read my diff end to end. Fixed during review: `HudModel.of` computed a piece's condition colour twice (now a
  private `mounted(condition)` helper); `GREY`'s Javadoc lacked the hex value the other legend colours carry; three
  test and three main lines over 120 characters (rewrapped). Re-ran everything after those edits (below).
- Process note: the two `HudModel` review edits landed at 16:26:22, while the first `run_gametests.sh` was running
  (its 1.20.1-forge compile had already run on the earlier code; the other three compiled the edited code). The edits
  are a behaviour-preserving refactor and Javadoc in client-only code that the dedicated game-test server never
  loads. To keep the evidence exact anyway, I re-ran the whole end-of-task chain afterwards on the final files,
  without edits in between (section "Final verification").
- Java 17 for the 1.20.1 variants: no Java 21 API or syntax (switch expressions on enums, records, `Optional.isEmpty`
  are 11-17); the four-variant build compiles it.
- Exactly 20 new `@Test` methods (3 + 10 + 7), the brief's names; helpers are private statics and a private record.
- Rule 7: `HudLevel` and `HudLevelTest` contain no `com.toxineren.exoframes.client.` string.

## Concerns

- Vignette at 320x240 (design-level, no Task 1 value changes): the two bottom corners overlap not only the offhand
  slots (the overlap design §5.12 accepts below 384) but also the hotbar band (left corner x 0-72 vs band from
  w/2 - 95 = 65; right corner from 248 vs band to w/2 + 95 = 255; y 200-240 vs 190-240) and the vanilla hotbar
  itself (w/2 - 91 = 69 < 72; 248 < w/2 + 91 = 251). At 341x256 they touch only the offhand slots (band from 75.5;
  corner to 72). The test asserts what the brief says and nothing about the band at 320x240, so this is neither
  locked in nor hidden by it; whether the design's sentence ("sotto 384 gli angoli in basso passano sopra lo slot
  della seconda mano") should also name the hotbar at the minimum size is for the coordinator. Numbers from
  <cartella temporanea>/m4c/t1/layout_check.py (output in this
  session; rerun with `python3 -I layout_check.py`).

## Final verification (Global Constraints end-of-task commands, on the committed files)

All run in sequence, no edits in between, from 16:28:10 to 16:31:15 UTC; `sha256sum -c` afterwards confirmed the
seven files were unchanged, and they are exactly what commit 6f195e8 holds.
Summary: <cartella temporanea>/m4c/t1/final/summary.txt
Logs: .../m4c/t1/final/build.log, check_jars.log, gametests.log (same folder).

    ./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
      :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar
        -> BUILD SUCCESSFUL in 23s (the four :test tasks executed, results written 16:28:16-16:28:33)
    JUnit count (plan's command)        -> 1.20.1-forge 378 / 1.20.1-fabric 378 / 1.21.1-fabric 378 / 1.21.1-neoforge 378
    bash tools/check_sources.sh          -> OK sources
    bash tools/check_jars.sh             -> OK jars
    bash tools/check_data.sh             -> OK data
    bash tools/check_art.sh              -> OK art
    bash tools/client/arm_measure.sh --selftest -> OK arm_measure
    bash tools/client/client_shot.sh --selftest -> OK passi
    bash tools/run_gametests.sh          -> OK 1.20.1-forge: 235 test obbligatori passati, nessun avviso fuori elenco
                                            OK 1.20.1-fabric: 235 test obbligatori passati, nessun avviso fuori elenco
                                            OK 1.21.1-fabric: 235 test obbligatori passati, nessun avviso fuori elenco
                                            OK 1.21.1-neoforge: 235 test obbligatori passati, nessun avviso fuori elenco

Also: failures + errors = 0 on all four variants' JUnit XML; the new tests write nothing to system-out/err; the
build log has no compiler warnings. Earlier runs (before the two review edits) gave the same results:
.../m4c/t1/build.log (BUILD SUCCESSFUL in 1m, 378 x4), .../m4c/t1/gametests.log (235 x4, OK), checks-static.log.
The long Gradle commands ran in the background (log in a temporary folder) because the foreground tool stops at 10
minutes; nothing else ran on Gradle meanwhile.

## Commit

6f195e8 core, client: le regole pure dell'HUD (HudLevel, HudModel, HudLayout), una legenda sola con la stazione
(signed by the environment; the two trailer lines; not pushed). `.superpowers/sdd` is git-ignored, so this report is
not in the commit.
