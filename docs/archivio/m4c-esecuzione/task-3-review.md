### Spec Compliance
- ✅ Spec compliant. I checked every file in the brief's Files list against the diff, which I read in four passes: Java and language files; docs and README; arm_measure.sh plus the R9 sequences; m3c, m3d and m4c.
  - Create: `client/SuitHud.java`. Matches "Tipi nuovi" without the vignette:
    - `register()` at SuitHud.java:72.
    - `tick` at :83. NONE without a player. The level comes from `HudLevel.of(SuitSize.wearsSuit, clientView)`. The energy state comes from `clientIndicators`. `ticksInState` carries over from the tick before. Spare cells are counted only when the level is not NONE, at :113-125.
    - `render` at :132. It draws nothing with NONE, hideGui, `hudHidden`, no player or a spectator (:137-138). Drawing order: plate, silhouette only with FULL, then the energy subgroup.
    - The warning, cell icon, text positions and bar rows follow the plan's pixel description.
    - Both ERROR texts are verbatim and logged once per session. The flags reset on CLIENT_PLAYER_QUIT (:239).
    - The whole tick and the whole render are each inside a `try`. No pose stack, no scale, only `fill` and `drawString`.
  - Delete: `EnergyHud`, `EnergyLine` and `EnergyLineTest` are gone.
    - The brief's Step 3 grep over `src` prints nothing.
    - The JUnit XML on disk (19:17, the final build) shows 381 tests on each variant, 51 suites, 0 failures, no EnergyLineTest.
  - `RedstoneExoframesClient.java:24`: `SuitHud.register();` replaces the hook.
  - Language files:
    - `percent` takes the place of `energy`: "%s%%" / "%s %%".
    - `no_cell` is "No cell" / "Senza cella" (R1).
    - `energy`, `worst`, `condition.*.critical` and `condition.*.broken` are removed, in the same order in both files.
  - Javadocs:
    - FirstPersonArm: criterion (b) and the "energia 0 (… 360x30+0+0)" note.
    - SuitDamage.pieceName: "messaggi … e schermata della stazione". Checked: `StationScreen.java:312,384,387,403` call it.
    - DebugEnergy: "(scatto, HUD)".
  - `docs/compat.md`:
    - :215, the "Uso" line of `addHudRenderer`.
    - :248, the HUD paragraph. It covers every Step 8 item: the hook and the client tick, why partialTick is not needed, HudLevel deciding NONE, HudModel's legend and question 9, HudLayout and question 8, their JUnit, F1/H/spectator, under screens, 1.21.1 above and 1.20.1 below, no 3D drawing.
    - The init bullet also changed. The Global Constraints reserve that list for Tasks 2 and 4, but it linked the deleted `EnergyHud.java`, so removing the deleted class forces the edit. Accepted.
  - `arm_measure.sh`:
    - `HUD_RIGHT` and `HUD_LEFT` at :59-60.
    - `sagoma … [destra|sinistra]` with `hud=`; any other third argument exits 2 (:277).
    - `differenza` (:176) and `colore` (:187). `colour_mask` (:117) is in the magenta mask's form, with the same 4 % per channel.
    - The selftest has 15 expectations, exactly the plan's list. I recomputed `hud=8400`: HUD_LEFT overlaps the magenta 200x140+700+400 in 120x70. The 1/255 patch sits at `10,470 109,499` in `rgb(90,140,71)`.
    - TDD evidence: scratch/red.txt has 11 `selftest:` blocks and exit 1; green.txt has `OK arm_measure` and exit 0.
  - `m3d_shots.sh`:
    - `nudo-senza` after `braccio-nudo` (:158) and in `shots_a` (:181), with "sedici" in the header.
    - The `nudo` check measures `braccio-nudo` against `nudo-senza` (:265).
    - `hud` in `criteria_bc` and in `controllo`, and the new `CRITERIA`.
    - `sinistra` in both left-hand checks (:314, :323).
    - The header paragraph matches the brief (:9-14).
  - `m3c_shots.sh`: the `hud-energia*` shots keep the same charge checks. The step-8 header lines and the "L'HUD sta in basso…" sentence are in.
  - `m4c_shots.sh`, with every row compared to the plan's tables:
    - Run A from "pieno" to "terza persona", including the "senza elmo" `@tieni j 0.2`.
    - Run B (:241).
    - Run C (:249) per R11.
    - All sixteen Task 3 checks. Rectangle and colour constants are named at the top (:138-161) and equal the "Disposizione dell'HUD" table.
    - Thresholds are half the neoforge measures, floored to tens. I recomputed them: 6160, 1470, 510, 210, 120, 610, 110, 40, 4690. `LOW_FILL_MAX_W=10` is fixed.
  - `tools/client/README.md`:
    - The usage line.
    - The arm_measure table: `hud=` with both rectangles, `differenza`, `colore`, the "del colore" rule, "HUD" in the magenta note.
    - The 3c row and sentence; the 3d paragraph and rows; the m4c paragraph with runs A, B and C and the sixteen-check table; durations.
  - Root `README.md`: "HUD dell'energia".
  - The brief's Step 8 grep finds only `docs/compat.md:47` (past tense, 3c-1) and :248 ("al posto della riga…").
- ✅ Rulings:
  - R9: `measure_lib.sh:3` names m4c_shots.sh. `unset EXO_SHOT_CLIENT_PREFS` is in m3a:28, m3b:45, m3c:50, m3c2:41, m3d:83 and m4a1:132.
  - R10: m3d_shots.sh:74 and :123, and the README `inventario` row, now read 166-176, x 318-323, 18633-18831.
  - R11 in m4c_shots.sh:
    - Run C has no `riavvio-senza-tuta`.
    - The first measure is `differenza nascosto riavvio/riavvio-nascosto 176x70+4+466` (:449), expected 0.
    - The header and the README row give the reason.
    - Outside `docs/superpowers/` nothing names `riavvio-senza-tuta` any more.
  - R12: the class Javadoc of SuitHud.java (end) and ClientPrefs.java:13-16 and :30.
- ✅ Extras beyond the brief and the rulings: none in code. The comment ranges were widened past R10's literal scope:
  - m3d `terza-persona` 23157-23745 (:72).
  - 1.21.1 inventory up to 10850 (:73).
  - `mappa` left value 23637 (:115).

  These are measured values and the checks are unchanged. The HUD cannot cause the third-person change: the group spans x 4-180 and the suit's magenta box spans x 321-639. For `mappa`, 28407 − 23637 = 4770, which is the magenta the group now covers; design §5.12 accepts the group over the map arm. Accepted.
- ✅ Gate: c3b43aa is the stop at the Step 6 gate, with the brief's message and diagnosis; e53ab48 completes the task. Recheck outputs:
  - neo-final-recheck.out and forge-final-recheck.out each show 16 `ok` lines and `OK <variante>`.
  - `log-check.txt` is 0 bytes in all six run folders.
  - The client logs of the m4c, m3d and m3c runs contain no `Suit HUD` and no `Failed to load options` lines.
- ✅ Other evidence:
  - m3d: 14 ok on neoforge and on forge.
  - m3c: OK on neoforge.
  - gametests-final.log: four `OK <variante>: 235 test obbligatori passati` lines.
- ✅ Shots I opened, all as the brief describes:
  - neo/senza-vignetta.png
  - neo/mancino/mancino.png: group bottom right, silhouette on the right edge, text read from the left.
  - forge/terza.png
  - hud-states.png: yellow, red, low with warning and short red bar, empty with grey warning and empty bar, ×1 white, bar only.
  - sv-crop.png
  - m3c-hud.png
- ✅ Checks I ran, each for a named risk:
  - **Risk: the `sagoma` output changed (`energia=` became `hud=`) and a caller still reads `energia`.**
    - I grepped every `sagoma` call in `tools/` and `.github/`. The callers are m3d_shots.sh, all updated, and m4a1_shots.sh:254 and :265, which read only `pixel` and the riquadro.
    - No reader of `energia` remains in `tools/` or `.github/`.
    - No `riga-energia` shot name remains there.
  - **Risk: stale references to the deleted classes.** `EnergyHud` and `EnergyLine` appear nowhere in `tools/` or `docs/` outside `docs/superpowers/`.
  - **File growth:**
    - m4c_shots.sh went from 167 to 471 lines. That is the plan's growth across Tasks 2-5, in m3d's form; m3d_shots.sh is 365 lines.
    - SuitHud.java is 245 new lines with one responsibility: the model per tick and the drawing per frame.
    - arm_measure.sh is 298 lines.

    No structural concern.
  - **Output noise:**
    - build-step3.log compiled all four variants (`compileJava` ran on each) with no warning.
    - build-final.log has no warning. There, forge and neoforge `compileJava` were UP-TO-DATE and the two fabric variants compiled the final sources.
    - The only extra line is the environment's `Picked up JAVA_TOOL_OPTIONS` from the test JVM, not project output.
- ⚠️ Cannot verify from the diff alone:
  - **Run C on neoforge.** The committed run C (R11, without `riavvio-senza-tuta`) ran live only on 1.20.1-forge (scratch/forge/riavvio). The 1.21.1-neoforge 16/16 comes from `M4C_SOLO_CONTROLLI=1` on run C shots taken with the pre-R11 steps; scratch/neo/riavvio still holds `riavvio-senza-tuta.png`. This does not block. It matches the resume instruction ("recheck"), and the removed step is only one shot before `WEAR 0`. Task 5's four-variant runs confirm it live, including R11's cross-run equality on a second loader.
  - **Fabric variants.** m4c and the changed m3d values (`mappa` left 23637, `nudo` against `nudo-senza`) were measured on neoforge and forge only. m3d_shots.sh:70 still says the measures are "uguali al pixel sulle quattro varianti". Task 5 should confirm on the fabric variants.
  - **Plan fact.** "Fatti del gioco" says `font.width("×0")` = 11; in game it is 12 (glyphs from energy x 53, shadow ending at 65). No code depends on it: SuitHud.java:187 measures at run time. Carry it like R7. The plan file also still describes the pre-R11 run C (plan :189, :211). Task 4 and 5 dispatches need R11.
  - **Commit hygiene.** Not checked here, since I ran no git commands: signatures and trailers of c3b43aa and e53ab48, and a clean tree. The diff stat lists no image or log files.
  - **Spare-cell cases.** Whether the Task 5 checklist covers an empty cell and an offhand cell for the spare-cell count (Minor 2).

### Strengths
- `SuitHud` reads like the plan's type description:
  - Model snapshot taken once per frame (SuitHud.java:134).
  - `framed()` (:230-236) draws the 1-pixel edge as four strips plus the inside. The translucent plate and the unmounted-piece fills therefore never blend a pixel twice. The implementer verified this to the pixel on both variants.
  - Exception handling is DEC-39 exactly: whole tick and whole render inside a `try`, one ERROR per session, flags reset on quit.
- The implementer went beyond the plan's count thresholds and checked 19 exact pixel counts and rectangles against HudLayout and HudModel, all ok on both variants (scratch/exact-neo.txt, exact-forge.txt). They cover:
  - piece fills and outlines;
  - the warning (32 GUI px);
  - the bar's lighter row, five full rows, frame ring and empty background;
  - the three cell-icon colours.

  This closes much of the "unpinned rectangles" gap carried from Task 1, at least for this run.
- The gate was respected. The implementer stopped at `riavvio`'s KO and named the cause precisely: eye height ×1.65 with the suit gives 7128 to 7132 grass pixels in the group. They showed the HUD really was hidden: 0 HUD green, and pixel-equal shots across runs. They offered two fixes instead of widening the check. R11 was then applied minimally.
- m4c_shots.sh is well organised:
  - every rectangle and colour is a named constant from HudLayout and HudModel;
  - every threshold carries its measured value;
  - all three runs' shots and copied prefs are removed before starting;
  - B and C are gated on A;
  - rectangles are compared exactly (`is_box`, :316) wherever the plan says "uguale a".
- arm_measure.sh grew by reusing `check_shot`, `check_rect`, `diff_mask`, `count` and `bbox` rather than copying them. The new mask follows the magenta mask's form, and the selftest numbers are hand-checkable.

### Issues
#### Critical (Must Fix)
None.

#### Important (Should Fix)
1. **Plan-mandated. m4c_shots.sh:181, :386-391 — no committed check fails if the warning is not drawn.**
   - What is wrong: the plan's row for `barra-esaurita` says the grey measure checks "percentuale e avviso fisso". The implemented measure is grey ≥ `TEXT_MIN` (120) in `PERCENT_AREA_RIGHT`, and the measured 248 is the warning plus "0%":
     - the warning alone is 128 px (exact.sh `colore esaurita 8A8A8A 16x16+50+482` → `colore=128 riquadro=16x16+50+482`);
     - "0%" alone is 120 px.

     So the measure passes with the warning missing (120 ≥ 120) and with the text missing (128 ≥ 120).
   - No other check covers the warning:
     - `barra-bassa` deliberately takes no exact measure of the blinking warning;
     - every other measured shot is in the NORMAL state, where there is no warning;
     - the implementer's exact.sh is scratch only and not committed.

     If `drawWarning` drew nothing, all sixteen checks would still print `ok`. The warning is the HUD's low-energy indicator (design §5.12, HUD-06).
   - Why it matters now: Task 4 edits `SuitHud.render`, which calls `drawWarning`, to add the vignette and the blend `finally`. A lost warning would pass the sequence that guards it.
   - Why it is plan-mandated: the implementer applied the plan's shared-threshold rule correctly (`TEXT_MIN` is half of the smallest of 244, 248 and 244), with the plan's rectangle. The plan's own Global Constraint asks every check to reject a wrong but plausible image ("dice dove … con che forma … di che colore").
   - Fix, a one-line change that needs the controller's ruling because it changes a plan check. Either of:
     - `is_box "$m3" 42x16+50+482` in `barra-esaurita`. That rectangle was identical on neoforge and forge; it pins the warning's left edge at x 50 and the text's right edge at x 91.
     - A separate `colore esaurita 8A8A8A 16x16+50+482` measure, as a named constant `WARNING_RIGHT`. That rectangle is `HudLayout.WARNING` at 960x540. Give it its own threshold: half of 128, floored to tens, is 60.

     Update the README `barra-esaurita` row to match.

#### Minor (Nice to Have)
1. **m4c_shots.sh:339-342 — `testo` is count-only.** Green ≥ 120 in `PERCENT_AREA_RIGHT` also passes a wrong but plausible green text, for example "10%". The numeric percent is pinned by HudModelTest, not the drawn text. The measured riquadro `46x14+50+482` was identical on both variants and could be pinned with `is_box`, as `barra-normale` already does.
2. **SuitHud.java:113-125 — the spare-cell rule is exercised in one case only.**
   - What runs: one charged cell in the main inventory gives "×1" (m4c_shots.sh:228).
   - What does not: the `cellEnergy(stack) > 0` filter (an empty cell must leave "×0"), the offhand list and the stack sum have no automated check. The rule lives in client code, so no JUnit reaches it.
   - Fix: in the `scorta` phase, `give` a 0 RE cell as well and keep the expected "×1"; or make sure the Task 5 checklist has an empty-cell step and an offhand-cell step.

### Assessment
**Task quality:** Needs fixes
**Reasoning:**
- Approved as built: the code, the deletions, the docs and rulings R9-R12. SuitHud matches the plan, the arm_measure selftest went RED then GREEN with 15 expectations, and m4c, m3d and m3c pass on 1.21.1-neoforge and 1.20.1-forge with exact rectangles and clean logs.
- The one fix is in the plan's own check. As written, no committed check would notice a missing warning, just before Task 4 edits the method that draws it. The change is one line, but it needs the controller's ruling.
