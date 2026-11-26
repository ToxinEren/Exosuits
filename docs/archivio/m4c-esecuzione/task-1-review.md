# Task 1 review: le regole pure dell'HUD (e755e05..6f195e8)

### Spec Compliance

- ✅ **Spec compliant.** Every constant, colour, rectangle and rule of "Tipi nuovi" and "Disposizione dell'HUD" matches
  value by value (table below). The 20 tests carry the brief's names and values (`HudLevelTest` 3, `HudModelTest` 10,
  `HudLayoutTest` 7). `StationLayout` takes the legend from `HudModel` as required. R1 is followed (`NO_CELL_X` 2,
  `NO_CELL_Y` 20, `HudLayout.java:74,76`). Nothing outside the brief's seven files changed, and the commit message is
  the brief's.
- ⚠️ **Cannot verify from the diff:**
  (a) the commit signature and trailer lines (no git commands were run, as instructed);
  (b) R5: at 320x240 the two bottom vignette corners also cross the hotbar band. Task 4 measures this. The Task 1
  test (`HudLayoutTest.java:206-214`) asserts only the overlap with the offhand slot at the two narrow sizes, so the
  hotbar-band overlap is neither locked in nor ruled out;
  (c) no test pins the exact values of `WARNING` or the six piece rectangles (Minor 3), and Task 3's screenshot checks
  count colours in 42x70 / 58x18 boxes, so they will not catch a 1-pixel error there. A human should look at the
  silhouette in the Task 3 screenshots;
  (d) whether the tested tree equals commit 6f195e8 rests on two things. `sha256sum -c final/sources.sha256` passes
  for all seven files on disk, and every line number in the diff matches the on-disk files. Neither is a git check.

Value-by-value check (all ✅):

| Item | Plan | Code |
|---|---|---|
| `HudLevel` | enum FULL, BAR_ONLY, NONE; `of(boolean, Optional<FrameState>)`; NONE without suit or view; FULL with a helmet whose condition is not BROKEN; cell ignored | `HudLevel.java:15,32-38` |
| `HudModel` record and components | package-private record; nine components in the plan's order; `Paint(fill, outline)` | `HudModel.java:35-36,256` |
| Constructor | copies into an unmodifiable `EnumMap`; IAE for `spareCells<0`, `percent` outside [0,100], `fillWidth` outside [0,61] | `HudModel.java:104-120` |
| Legend | GREEN 55FF55, YELLOW FFFF55, RED FF5555, GREY 8A8A8A, WHITE FFFFFF | `HudModel.java:39,44,46,48,53` |
| Drawing colours | 99101216, 6EB87333, FF0C0D0F, FF282A2E, 5A000000, FF464646, FF3C3F44, FF969696, FFC81E1E | `HudModel.java:55-74` |
| `ALL_COLOURS` | the above + 4 outlines + `lighter` of GREEN/RED/GREY/WHITE | `HudModel.java:91-93` |
| `FILL_MAX`, `BLINK_TICKS` | 61, 10 | `HudModel.java:77,82` |
| `conditionColor`, `energyColor` | per the legend; `energyColor` Javadoc names question 9 | `HudModel.java:160-167,172,175-184` |
| `outline`, `lighter` | alpha FF, `c/3`; alpha FF, `c+(255-c)/2` | `HudModel.java:190-200,243-248` |
| `ticksInState`, `warningOn` | 0 on change, else +1; EMPTY always, LOW `(t/10)%2==0`, else false | `HudModel.java:207-225` |
| `of`, `spareColor` | NONE constant for NONE (null state allowed), else NPE; silhouette only with FULL; percent and fill rounded down with long arithmetic; `warningSlot` for LOW and EMPTY | `HudModel.java:133-153,228-230,239-241` |
| `HudLayout` constants | 88, 35, 21, 67, 2, 37, 424, 72, 40 | `HudLayout.java:21-43` |
| Elements | WARNING 2,8-10,16; TEXT_X 2; TEXT_X_AFTER_WARNING 12; TEXT_Y 8; PERCENT_AREA 2,8-31,17; SPARE_RIGHT 65; CELL_ICON 5x8; SPARE_AREA 40,8-65,17; BAR_FRAME 2,20-65,28; BAR_FILL 3,21-64,27 | `HudLayout.java:46-72` |
| Pieces | 8,3-14,9; 7,10-15,21; 3,10-6,21; 16,10-19,21; 7,22-10,33; 12,22-15,33 (equal to design §5.12's sizes and origins) | `HudLayout.java:178-183` |
| Functions | `narrow`, `group` (Javadoc names question 8), `silhouetteGroup`, `energyGroup`, `plate` (IAE for NONE), `piece`, `spareTextX`, `vignetteCorners`, `screen`, `Rect` API | `HudLayout.java:88-211` (question 8 at :95) |
| `StationLayout` | four constants = HudModel GREEN/YELLOW/RED/GREY; `conditionColor` delegates; Javadoc "la legenda e' quella dell'HUD, design §5.3 e §5.12" | `StationLayout.java:94-100,123-129` |

I recomputed the derived screen rectangles at 480x270 (scale 2): the fill 122x12+52+508, the percent area
58x18+50+482 and 58x18+784+482, the spare area 50x18+126+482, and the four corners. All match the plan table.

### Strengths

- **Exact values.** The values are exact and every constant cites its design row. The Javadoc of `group` and
  `energyColor` names questions 8 and 9 as the brief asks.
- **Independent vanilla model.** The layout test computes the vanilla elements itself, in doubles, with no call
  into `HudLayout` (`HudLayoutTest.java:275-309`). It uses its own `Area.touches` predicate, not `Rect.intersects`, and
  that predicate has a positive control (`:212-213`). At 480x270 this model reproduces design §12's screen rectangles
  exactly: band 380x100+290+440, offhand 58x46+240+494, crosshair 60x60+450+240, chat x 0-664 / y 280-460, arm
  286x205+674+335. So the test checks the same geometry the design checked.
- **Magenta test.** It builds the list of drawn colours independently of `ALL_COLOURS` and includes a positive
  control (`HudModelTest.java:193-215`). Its rule is identical to `arm_measure.sh:94`.
- **Careful details.** The empty non-`EnumMap` corner case of `new EnumMap<>(map)` is handled and commented
  (`HudModel.java:116-119`). `HudModel.conditionColor` keeps the old switch's NPE message (`:161`), so behaviour seen
  by `StationScreen` is unchanged. Percent and fill use long arithmetic.
- **Small premise assertions in tests.** The tests state their premises (`HudLevelTest.java:28,32,41` assert
  DAMAGED, CRITICAL and BROKEN on max 180) and add only specified behaviour.
- **Clean evidence.** The final run is clean and tied to the files on disk (checks below).

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

1. **`HudModel.java:73-74`: the `CELL_CHARGE` Javadoc cites the wrong source.**
   - What's wrong: it says "il rosso della redstone (design §5.12 ...; §12)", pointing at the §12 previews. The
     preview's `draw_cell_glyph` uses `REDSTONE = (0xC0, 0x20, 0x1A)`, which is `C0201A` (`anteprime.py:30,250`), not
     `C81E1E`. The code value follows the plan's "Tipi nuovi" and is correct per spec. It also equals the station's
     `StationLayout.CELL_RED` (`StationLayout.java:80`). `CELL_BODY` and `CELL_CAP` do match the preview
     (`anteprime.py:249,251`).
   - Why it matters: a reader keeping the placeholder in step with the previews is misled. The implementer's report
     makes the same inaccurate claim (`task-1-report.md:19-20`).
   - Fix: cite the plan and the station's cell red instead. **For the human:** the plan's cell-icon red differs from
     the preview's.
2. **`HudLayoutTest.java:96,112,115,124,131-132,143-145,149,151,158-159`: one-sided containment and overlap checks.**
   - What's wrong: these checks use the code under test (`Rect.contains`/`intersects`, `HudLayout.java:198-205`) as
     their oracle, and always in one direction: `assertTrue(…contains)` and `assertFalse(…intersects)`. A regression
     that made `contains` always true, or `intersects` always false, would turn all of them into no-ops. Today's
     implementations are correct.
   - Fix: add two or three control asserts inside an existing test, so the JUnit count stays 378. For example
     `assertFalse(new Rect(0,0,10,10).contains(new Rect(5,5,11,10)))`,
     `assertTrue(new Rect(0,0,10,10).intersects(new Rect(9,9,20,20)))`, and an edge-sharing pair that must not
     intersect.
3. **Several constants are not value-pinned by any test.**
   - Which: `WARNING` (`HudLayout.java:46`), the six piece rectangles (`:178-183`), `CELL_ICON_H` (`:63`), and
     `PLATE`, `PLATE_EDGE`, `BAR_EDGE`, `BAR_BACK`, `CELL_BODY`, `CELL_CAP`, `CELL_CHARGE` (`HudModel.java:55-61,70-74`).
   - What's wrong: the tests check only containment, non-overlap, order, "in `ALL_COLOURS`" and "not magenta", so a
     typo such as a 7x7 warning or a shifted helmet passes. The brief did not ask for these pins, so this is coverage.
   - Fix: add literal `assertEquals` lines inside the existing `elementsStayInsideTheirSubgroupAndDoNotOverlap` and
     `noHudColourIsMagentaForTheChecks`.
4. **`HudModelTest.java:132-135`: the station-equals-HUD loop is true by construction.**
   - What's wrong: `StationLayout.conditionColor` now delegates (`StationLayout.java:128`), so the loop cannot fail.
     It is brief-mandated.
   - Why it is not Important: both legends are pinned by literals (`HudModelTest.java:127-130`,
     `StationLayoutTest.java:129-132`). The loop only guards against a future copy of the switch, so leaving it is
     fine.
5. **Process: long Gradle commands ran in the background (no code impact).**
   - What's wrong: the Global Constraints say long commands run in the foreground (`contesto-comune.md:48`). The
     implementer ran them in the background (`task-1-report.md:149-150`). As a result, two review edits landed while a
     `run_gametests.sh` run was in progress (`:102-106`).
   - How it was remedied: the whole end-of-task chain was re-run from 16:28:10 to 16:31:15 with no edits in between.
     `sha256sum -c final/sources.sha256` passes for all seven files on disk.

### Checks outside the diff (risk → what I checked → result)

1. **Division by zero, or `percent`/`fillWidth` out of range, making `HudModel`'s constructor throw every tick.**
   Checked `EnergyCell.java:9-18` (the constructor requires `capacity > 0` and `0 <= stored <= capacity`) and
   `:37-50` (`drain` and `charge` clamp). Result: this cannot happen from `HudModel.of`.
2. **The test's magenta rule drifting from the script's.** Checked `arm_measure.sh:28,94`:
   `(r>0.2 && b>0.2 && g<0.45*r && g<0.45*b)`. Result: identical to `HudModelTest.java:241-247`. The borderline
   `PLATE_EDGE` (b = 51/255 = 0.2 exactly) is not magenta under either rule, and it also fails the g criterion.
3. **The removed `java.util.Objects` import is still used.** Grepped `Objects` in `StationLayout.java`. Result: no
   matches.
4. **The `StationLayout` colour contract changed (colours now come from `HudModel`).** Checked the call sites
   `StationScreen.java:245,316,395` and `StationLayoutTest.java:129-132,170`. Result: identical values and the same NPE
   message. `CONDITION_*` is used only inside `StationLayout.ALL_COLOURS`.
5. **Cost of `state.pieces()` in the per-slot loop.** Checked `FrameState.java:73-75`. Result: it returns an
   unmodifiable view, not a copy.
6. **Provenance of the cell-icon colours.** Checked `anteprime.py:30,248-251`. Result: see Minor 1.
7. **Line length and ASCII.** I read the changed files on disk directly (grep and python). Result: no non-ASCII
   character except `§`, and every line is at most 120 characters. Three lines are 121 bytes only because `§` takes
   two bytes in UTF-8, so the report's claim holds.
8. **The implementer's evidence.** Results:
   - `final/summary.txt`: BUILD SUCCESSFUL, JUnit 378 on all four variants, every check OK, 235 game tests on all
     four variants.
   - `final/build.log`: no warning, deprecation or error lines.
   - `green.log`: the only noise is environmental (`Picked up JAVA_TOOL_OPTIONS` and Gradle's configuration-cache
     hint), with no compiler warnings.
   - `sha256sum -c final/sources.sha256`: all seven files OK.

No tests or Gradle builds were run for this review.

### Assessment

**Task quality:** Approved

**Reasoning:** The three pure types match the plan's signatures, constants and rules exactly. The vanilla geometry
in the tests is modelled independently and reproduces design §12. The station now has one legend with unchanged
behaviour. The open items are documentation and test-coverage polish, none of which affects behaviour.
