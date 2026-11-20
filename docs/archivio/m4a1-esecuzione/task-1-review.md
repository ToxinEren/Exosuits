# Task 1 review: dcd16bb..99e4605 (45722f9 + 99e4605, ruling P11)

The 243 KB diff was read in passes: Read at offsets 1-1822 in chunks of 60-280 lines; `sed` from 1822 to 2904, with long lines cut at 600-900 characters; and one pass over only the changed lines of `tools/client/README.md`, printed in full. Outside the diff file, I ran one read-only `git show 99e4605:tools/client/m4a1_shots.sh` and grepped the checkout's `tools/client/*.sh` and `tools/client/README.md`. These served the duplication check (Important 1) and the "Regole degli scatti misurati" reference in the `m4a1_shots.sh` header (present, README line 174). No other changed file was opened. Source line numbers below were mapped from the diff's hunk headers.

## Spec Compliance

- ✅ **Spec compliant.** Every file in the brief's Create and Modify lists has its hunk, and so do the three P11 files (`compat/client/GuiEntityScale.java`, `compat/impl/client/GuiEntityScaleImpl.java`, `ClientCompat.java`).
  - **"Produces", item by item:**
    - `BlockBase` takes over `onClick`, `useItemOn` and `use` verbatim, with the same directives and the same translation table (`BlockBase.java:45-68`).
    - `EntityBlockBase extends BlockBase implements EntityBlock` and has no copy and no directives left (`EntityBlockBase.java`).
    - `ContainerScreenBase.render` is `public final`: `//? if <1.21` `renderBackground(graphics)`, then `super.render`, `renderContent`, `renderTooltip` (`ContainerScreenBase.java:45-51`).
    - `StationMenu`:
      - two constructors share one private constructor, so the single `DataSlot` is added in the same order (`StationMenu.java:55-70`);
      - `clickMenuButton` is true and +1 only for 102 with a living player, and logs nothing (`:99-105`);
      - `stillValid` (`:120-136`) is true when there is no level. Otherwise it checks `player.level() == level`, the static helper with `ModBlocks.STATION`, alive and not a spectator, all inside `try`; the ERROR is logged once per menu with the exception;
      - `quickMoveStack` returns `EMPTY`, and `removed` is not overridden.
    - `StationBlock.onClick`: PASS for the off hand, a spectator or a dead player; on the server it opens the menu; SUCCESS on both sides (`StationBlock.java:32-41`).
    - `GameTestKit`: `STATION_MENU_ID = 100` (`GameTestKit.java:134`); `openStation` and `pressButton` match the brief, including the Javadoc on why the menu is not opened through the network (`:800-825`).
    - The screen registration comes after `CellSwapKey.register()`, with a two-line comment (`RedstoneExoframesClient.java:28-30`).
    - `ExoframeEntityRenderer.render` replaces only its four steps with `SuitPreview.frameSpace` (`ExoframeEntityRenderer.java:69`).
  - **`StationScreen`:**
    - `renderBg`: the full 316×232 opaque fill with a 1 px edge, the panel, the preview background, `drawInGui(leftPos + 216, topPos + 18, leftPos + 308, topPos + 134, 0f, FIXED_VIEW, minecraft.player, model)`, then the button (`StationScreen.java:65-75`).
    - The model is baked in `init()` with `young = false` (`:56-61`).
    - Title only in `renderLabels`; the tooltip in `renderContent`.
    - A left click on the button only calls `handleInventoryButtonClick(containerId, 102)` (`:93-99`).
    - `FIXED_VIEW`: five pieces in `TierPalette` colours and the right arm `FF00FF` (`:119-127`).
  - **`SuitPreview.drawInGui`** follows the plan's step order, with P11's `ClientCompat.guiEntityScale().apply(pose, 32)` in place of `pose.scale` (`SuitPreview.java:61-83`).
  - **Game tests:**
    - both tests do every step of Step 3: ids 0/101/103/127/-1, `containerId + 1`, the second 102, and the stale id after `closeContainer()`;
    - the spectator and dead-player checks run in one tick, with health restored in `finally`;
    - cleanup is in `finally`, and the class is registered after `SurvivalGameTests`.
  - **Assets and language files:**
    - blockstate variant `""`, `orientable` with `top`/`front`/`side`, `item/generated`;
    - four textures in `outputs()`, block faces through `solid()`;
    - keys at the plan's positions, with the plan's texts.
  - **`client_shot.sh`:**
    - the selftest has exactly the 11 good and 11 bad cases of Step 1;
    - width and height are read and checked before the step loop (`:154-158`);
    - `check_point` (`:165-169`);
    - `EXO_SHOT_STEPS_ONLY` exits after the loop with `OK passi: <N>` (`:226-229`);
    - `held_button` is released first in `cleanup` (`:378-381`);
    - `@clic` sets `held_key=Shift_L` before `keydown`;
    - `@trascina` does ten equal moves with `sleep 0.03`.
  - **`m4a1_shots.sh`:**
    - the Task 1 phases and steps match the plan's table one for one;
    - the three checks use the plan's rectangles;
    - `MAGENTA_MIN=590` (half of 1193, rounded down) and `BUTTON_MIN=550` (half of 1112, rounded down);
    - `unset EXO_SHOT_SERVER_CONFIG`, `rm -f log-check.txt`, and `M4A1_SOLO_CONTROLLI`;
    - mode 755.
  - **Docs (Step 10 and DEC-06):**
    - the `CompatBlock` section takes the click table (`docs/compat.md:117`), and the `CompatEntityBlock` section now points to it;
    - the `GuiEntityScale` section (`:216`), the `CompatContainerScreen` section (`:225`) and "Menu e schermate" (`:247`) are all there;
    - the `SuitPreview` and registration bullets in "Avvio del client", and the API-table rows;
    - `grep -c CompatContainerScreen` gives at least 3;
    - both READMEs carry everything Step 10 lists, and both say "ventidue".
  - **Rulings:**
    - P1: both per-version bases were created here.
    - P2: `rotateZ(π)` plus `frameSpace(pose, 180 - angle)` (`SuitPreview.java:71-73`).
    - P3: no game test opens the menu through the network.
    - P9: button 102 is kept.
    - P11: the seam, its accessor and its doc section are all present. `GuiEntityScaleImpl` makes exactly the calls P11 specifies (`GuiEntityScaleImpl.java:28-33`): `mulPoseMatrix(new Matrix4f().scaling(s, s, -s))` on 1.20.1 and `pose.scale(s, s, -s)` on 1.21.1. The pixel-identical clips confirm the outcome.
  - **Extra, beyond the brief (my verdicts):**
    - `StationLayout.lighter` (`StationLayout.java:53-67`), for the hover colour. It is justified: the plan asks for "al passaggio del mouse più chiaro" but lists only seven Task 1 colours. It is pure Java and cannot produce magenta (`TEST_IDLE` → (107, 111, 116), `TEST_DONE` → (98, 168, 129)). It has no JUnit test, consistent with the table's fixed 331 for Task 1; Task 5's `StationLayoutTest` magenta check should include `lighter(TEST_IDLE)` and `lighter(TEST_DONE)`.
    - `StationMenu.TITLE_KEY` (`StationMenu.java:40`). A harmless named constant, used once in `provider`, in the same style as the plan's `NO_FRAME`/`FRAME_GONE`.
    - `requireNonNull` plus `immutable()` in the `StationOpening` compact constructor (`StationOpening.java:15-17`). Harmless and defensive: a `MutableBlockPos` from a future caller cannot be captured. Current callers already pass immutable positions.
    - None of the three is over-engineering.
- ⚠️ **Cannot verify from the diff:**
  - Commit signing and the branch.
  - `git status --short` free of screenshots. The diff's file list contains no shots, which is consistent.
  - Anything in the production jars beyond the reported `OK jars`: the client gate ran only in the development environment.
  - The javap claims in `docs/compat.md`, for example that 1.21.1's `AbstractContainerScreen.renderBackground` calls `renderTransparentBackground` and `renderBg`, or the vanilla call shapes of `InventoryScreen`. They are consistent with my reading of vanilla, but I did not re-run javap.
  - The Step 8 `m3d_shots.sh` regression ran on 45722f9, before P11. P11 does not touch `frameSpace` or `ExoframeEntityRenderer` (checked in the diff), so the result still applies, but no run exists after P11.

## Strengths

- **The click translation moved, not copied.** `BlockBase.java` is the old `EntityBlockBase` body, word for word, and `EntityBlockBase` lost every directive. The charger's 27 tests still pass, with 210 game tests per variant in the evidence.
- **Review Focus 5 holds** (`SuitPreview.java:64-81`):
  - scissor (`:64`) and `pushPose` (`:65`) come before the `try`;
  - `flush()` (`:77`) runs inside the scissor;
  - the `finally` restores the light, the pose stack and the scissor (`:79-81`).
  - The guarantee that `SuitDraw.draw` does not throw also holds: `SuitDraw.java:55-77` wraps the whole body and catches `RuntimeException | LinkageError`, on a private `PoseStack`.
- **P11 is minimal and correct.** On each version it makes exactly the calls P11 specifies, which match my reading of vanilla `renderEntityInInventory` (javap not re-run).
  - I checked the implementer's shots with `compare -metric AE`. Without fuzz, the four preview clips are pixel-identical (0 differing pixels), and so is the whole `632x464+164+38` screen rectangle in `stazione-aperta.png` across the four variants.
  - The docs' "uguale al pixel" therefore holds exactly, not only within the 4 % fuzz the report mentions.
- **The game tests go through vanilla's real button handler** (`handleContainerButtonClick`), not just the menu method. They cover a stale id, a closed menu and a byte-range id. That proves the server path, including vanilla's own guards.
- **`client_shot.sh` validation is tight:**
  - at most five digits, so a huge number cannot overflow the arithmetic;
  - forced base 10, so `010` is not read as octal;
  - the selftest compares exact output (`OK passi: 1`, nothing on stderr);
  - the selftest also asserts that the output folder was not created;
  - every held key or button is recorded before it is pressed.
- **`m4a1_shots.sh` is robust.** Every check runs even after a failure, and a failed measurement forces `KO` (`verdict`).
- **The visual claim checks out.** `p11/1.20.1-forge/stazione-aperta.png` shows the suit upright and facing the front, with the magenta right arm on the left of the screen, fully inside the clip, and a grey `0` button.

## Issues

### Critical (Must Fix)

None.

### Important (Should Fix)

1. **`tools/client/m4a1_shots.sh:136-172`: the measuring and verdict helpers are a verbatim copy of `tools/client/m3d_shots.sh:204-233`.** This is plan-mandated in effect.
   - **What is copied:** `failed=()`, `take_measure`, `val`, `box` and `verdict`. `diff` of the two blocks shows no difference except the added `inside` and its comment (`:155-161`).
   - **Why "plan-mandated":** the brief says to follow "lo schema di `m3c2_shots.sh` e di `m3d_shots.sh`", and its file list neither creates a shared helper nor modifies `m3d_shots.sh`.
   - **Why it matters:** the logic that turns `arm_measure.sh` output into ok/KO verdicts now lives in two places, so any fix has to be made twice. For example, `val`'s unquoted `for w in $1` is subject to pathname expansion.
   - **It will get worse:** Task 5 extends `m4a1_shots.sh` (`rotazione`), and decision 9 says the 4a-2 will create `m4a_shots.sh`, which would likely be a third copy.
   - **Fix:** move these helpers, plus `inside`, into one file under `tools/client/` that `m3d_shots.sh` and `m4a1_shots.sh` both `source`. Do it before Task 5. It touches a file outside Task 1's list, so it needs a coordinator ruling; the alternative is to accept the copy explicitly.

### Minor (Nice to Have)

1. **`docs/compat.md:202` (the "Uso" bullet of `SuitRenderer`).** The insertion split the old sentence "con la vista di `SuitDraw.view(riassunto)` = `SuitView.of(riassunto, TierPalette::primary, TierPalette::secondary)`".
   - Now "= `SuitView.of(...)`" describes "una vista fatta dalla schermata", which is wrong: `StationScreen.java:119-127` builds `new SuitView(pieces)` by hand (`FIXED_VIEW`).
   - The statement about what `SuitDraw.view` is has also been lost.
   - Fix: restore the equation after `SuitDraw.view(riassunto)`, and say that the preview gets a view built by the screen (today the fixed one).
2. **`StationMenuGameTests.java:94-119` (`placeholderButtonIgnoresSpectatorsAndTheDead`).**
   - **Missing positive control:** the test never asserts `menu.stillValid(p)` is true before turning the player into a spectator or a dead player. The two `!stillValid` assertions (`:103`, `:111`) would pass even if `stillValid` were false for another reason. Today the only positive control is indirect: the first press in the sibling test succeeds only if the vanilla handler finds `stillValid` true.
   - **Fix:** add `helper.assertTrue(menu.stillValid(p), …)` after `openStation`, and again after the switch back to survival, before `setHealth(0f)`.
   - **Untested branches (outside the brief's list, noted only):** the `player.level() == level` branch and the `RuntimeException` branch of `StationMenu.stillValid` (`StationMenu.java:125-134`).
3. **Four added Java lines exceed 120 characters (121-122).** They are `StationLayout.java:5` and `StationScreen.java:40`, `:66` and `:104`, counted in characters, not bytes. This matches the report: six lines minus the two rewrapped in P11. Nothing enforces 120, and older files exceed it.
4. **`tools/client/client_shot.sh:561-590`: two of the new step paths have never run.** The `maiuscole` path of `@clic` (`:568-576`) and all of `@trascina` (`:577-590`) are only parsed by the selftest. No Task 1 sequence uses them, so their first real execution comes in Task 5 (the `rotazione` and `inventario` phases). If they fail there, look here first. The plain `@clic` path did run (`@clic 748 426`).
5. **`tools/client/README.md:214`: the wording is slightly off.** It says "`@clic ... maiuscole` tiene premuto Maiusc solo dentro una schermata". The step holds Shift wherever it runs; the real limit is that without a screen the `mousemove` turns the camera (as `:72` says correctly).

## Checks outside the diff (one focused check per named risk)

- **Risk: a server-side `SUCCESS` from `StationBlock.onClick` breaks the project's convention** (an extra swing, unlike vanilla's `CONSUME`).
  - I read `ChargerBlock.java:186-218`: the charger also returns `SUCCESS` on the server when it acts.
  - The brief mandates `SUCCESS` (brief line 12).
  - No finding.
- **Risk: an exception escaping `SuitDraw.draw` skips the in-scissor `flush()` and leaks unclipped vertices** (Review Focus 5). I read `SuitDraw.java:55-77`: the guarantee holds.
- **Risk: verbatim duplication of the measuring helpers.**
  - I compared `m3d_shots.sh:204-233` with the same block of `m4a1_shots.sh`, using one read-only `git show 99e4605:tools/client/m4a1_shots.sh` and `grep` on the checkout. Nothing was modified.
  - Result: Important 1.
- **Evidence I opened** (under `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t1/`):
  - `p11/final-checks.txt`: all six checks `OK`.
  - `p11/final-junit.txt`: 331 ×4.
  - `p11/final-checks-m4a1.txt`: three `ok` ×4.
  - `p11/m4a1-1.20.1-forge.txt`.
  - The `OK` lines of `p11/final-gametests.txt`: 210 ×4.
  - `p11/final-build.txt`: no warnings or notes; `BUILD SUCCESSFUL`.
  - `evidence/m3d-*.txt`: 14 `ok` lines each, then `OK`.
  - The client and server logs of the four P11 runs: no WARN or ERROR line mentions the station or the mod.
  - The clip and screen comparisons described above.
  - The test output is clean.

## Assessment

**Task quality:** Needs fixes

**Reasoning:** The implementation matches the brief and rulings P1, P2, P3, P9 and P11 line by line. It is correct on both versions, and the real-client evidence is exact: the clips are pixel-identical across the four variants. The only Important item is a plan-mandated verbatim copy of the m3d measuring helpers into `m4a1_shots.sh`. It needs a coordinator ruling before Task 5 grows the script: extract the helpers into a shared file, or accept the copy explicitly. The Minor items are a misworded docs sentence, a missing positive control in one game test, two step paths that have not run yet, and polish.
