### Spec Compliance
- ✅ Spec compliant. I checked every file in the brief's Files list against its hunk. All 10 created files and all modified files are present, including the `git mv` to `m4a_shots.sh` and the reference updates in `measure_lib.sh`, both READMEs, `docs/compat.md`, and the Javadocs of `GameTestKit.openStation`, `StationMenuGameTests` and `StationLayoutTest`. Requirement by requirement:
  - **`StationAction`** has the 16 kinds with `first`/`count`, plus `decode`, `button()`, `button(Kind,int)`, `writes`, `slot` and `HIGHEST = 102`. The constructor throws NPE for a null kind and IAE for an out-of-range argument. The Javadoc carries the §5.6 table and says the 4b numbers already decode (`core/station/StationAction.java`).
  - **`TickGate`** has no sentinel; any tick other than the last one used is open (`core/station/TickGate.java`).
  - **`withSelection`**: `StationView.withSelection` throws IAE outside [0, 7] (`core/station/StationView.java:131`).
  - **`press` order**: `StationActions.press` follows "L'ordine di press" exactly (`station/StationActions.java:62-101`):
    - decode, then alive → `IGNORED` (:66);
    - `SELECT_*` → `select` → `SELECTED`, without using the write turn (:70);
    - the 4b numbers and `TOGGLE_LOCK` → `IGNORED`, before the turn (:74; the brief makes the lock `IGNORED` until Task 2);
    - `takeWriteTurn` → `THROTTLED` (:78);
    - record plus `canUse` → `REFUSED` (:82);
    - `FrameFitting` by kind; on `FAILED`, `StationTexts.FAILED` on the action bar (:94).
  - **`StationMenu`**:
    - `TEST_BUTTON`, the `DataSlot`, `testPresses`, `NEVER` and `lastSentTick` are gone.
    - `select` throws IAE outside [0, 7] and also predicts on the client (`station/StationMenu.java:257-264`); `takeWriteTurn` is package-private (:272).
    - The view gate gives the same behaviour as the old `NEVER`/`lastSentTick` code (:367, :381).
    - On the server, `clickMenuButton` returns true for `SELECTED`/`DONE`. On the client it returns true for any decoded number, calling `select` first for the three selections (:284).
    - `removed` is not overridden: a grep for `removed(` in `StationMenu.java` finds nothing.
  - **`FrameFitting`** (`frame/FrameFitting.java:58-174`):
    - each method makes exactly one `updatePlaced`;
    - delivery, `shrink(1)` (except with `instabuild`), `refreshView` and the sound come only after it succeeds;
    - `mountPiece` checks the wrong type silently first, then the taken slot with `SLOT_TAKEN`;
    - the old cell goes to the player even in creative;
    - the WARN of a tier without an item is unchanged;
    - nothing is ever put on the cursor.
  - **Hands**: `FrameInteractions` now only picks the slot. When no slot is free it takes the first of the order, and `FrameFitting` reports it taken (:140). `DONE` → `SUCCESS`, anything else → `CONSUME` (:232). `refreshSummary` and the duplicated mount/unmount bodies are gone.
  - **`refreshView`**: `FramePlacement.refreshView` exists and is public (`frame/FramePlacement.java:124`).
  - **Refusal seam**: a counter in `FrameStateSavedData` (:115), read only by `FrameStateAccessImpl.updatePlaced` (:200), before the read-only branch so nothing is logged. `GameTests.Refusal` closes idempotently (`compat/impl/GameTestsImpl.java:262-270`).
  - **`SuitView.visualState`** is used by `SuitView.of` and `StationLayout.suitView`.
  - **`StationLayout`**:
    - rects `STOWED` 144,6-274,15, `ALL_BUTTON` 41,103-71,119 and `UNMOUNT` 195,20-207,32 (`client/StationLayout.java:76-80`);
    - `Cursor`, `ButtonState`, `buttonState` (:384), `allSprite`/`unmountSprite`/`ghostSprite` (:422, existing constants only);
    - `placeAt` (:439), `placeButton` (:468, every row of "Le regole dei clic"), `unmountButton` (:493), `fitsBeside` with `HEADER_GAP` 4 (:512).
  - **`StationScreen`**:
    - The cursor class comes from `menu.getCarried()` (`client/StationScreen.java:293`).
    - Numbers are sent like the vanilla menus: `clickMenuButton`, then `handleInventoryButtonClick` (:283).
    - Only the left button acts; a press on a widget is remembered per button, and its drag and release return true without `super` (:171-235).
    - The rotation still uses the distance from the press point.
    - `TEXTURE` is package-visible (:57); `GuiTexture.endBlend()` stays in `renderBg`'s `finally` (:131).
    - The test button and its drawing are gone.
  - **Drawing**:
    - Ghosts are drawn at +2,+2 (`client/StationFramePanel.java:137`).
    - "Tutti" is `allSprite(buttonState(true, pressed or selection 7, hover))`, with its label centred in `TEXT` (:148).
    - The header uses `fitsBeside` and shows an empty slot on two `TEXT_DIM` rows (`client/StationHeader.java:97`).
    - The arrow's state comes from `unmountButton` (:52).
    - The `STOWED` line is drawn in `renderLabels`; its `HOLDS_ARMOR_HOW` tooltip and the arrow's `UNMOUNT` tooltip show only with an empty cursor.
    - No new fill and no new colour (`ALL_COLOURS` is unchanged).
  - **Texts and allowlist**: the language keys are in the stated positions with the stated words, and `test_button` is removed. The single `log_allowlist.txt` line matches verbatim (`tools/log_allowlist.txt:47`). `everyItemAndBlockHasANameInBothLanguages` adds the five key sources.
  - **JUnit**: `StationActionTest` 6, `TickGateTest` 4, and `StationViewTest.withSelectionChangesOnlyTheSelection` (:151). `StationLayoutTest` adds the four new tests (:297, :311, :343, :356), takes the three new rects and drops the test button (:63), and adds `RESERVED_4B` with the nine rects of the brief (:57) and the new sequence points (:157). `SuitViewTest.visualStateFollowsTheCondition` is at :67. Each assertion of brief Steps 1 and 3 is present.
  - **In-world tests**: `StationActionGameTests` has all 11 tests (`gametest/StationActionGameTests.java:111-493`), registered after `StationBlockGameTests`. The two `StationMenuGameTests` tests were renamed and rewritten as the brief says (:143, :181). `scrapPieceStack` is exactly the stated expression.
  - **Review Focus**: items 1, 2, 3 and 5 each have the named tests, pinning the stated behaviour:
    - one write per tick per menu, checked across two menus (:388);
    - the record and the lock re-read before B's menu closes (:425);
    - the emptied selection stays (:139);
    - a full inventory drops at the feet with the pickup delay (:313);
    - creative keeps the stack on the cursor (:173, :336);
    - packets for another or closed menu are discarded (`StationMenuGameTests.java:143`);
    - the release outside the screen in the `montaggio` phase.
  - **`m4a_shots.sh`**:
    - the phases and steps match "La sequenza", and the `pulsante` check and phase are gone;
    - `rotazione` compares `girata` with `stazione-aperta`;
    - `scelta`, `smontaggio` and `montaggio` use the exact rects;
    - `SEL_MIN=480` (972), `PLACE_MIN=470` (952) and `HELMET_MIN=240` (497) are half the neoforge values rounded down to ten (`tools/client/m4a_shots.sh:186-194`);
    - `M4A_SOLO_CONTROLLI` is in place, and the header lists what the sequence does not prove.
  - **Docs**: `docs/compat.md` covers `refusePlacedUpdates`, `visualState`, the buttons paragraph (:256) and the `StationScreen` paragraph. `tools/client/README.md` has the new phases, checks and `M4A_SOLO_CONTROLLI`.
  - **Not in this task's scope**: a live search for `pulsante di prova|test button|BUTTON_MIN|748 426|TEST_BUTTON|testPresses` over `src`, `docs/compat.md`, both READMEs and the script found only `README.md:9`, the "Stato" paragraph. Plan Task 5 Step 2 rewrites that paragraph.
- ✅ Hygiene checks I ran on the diff's added lines:
  - no Java line over 120 characters (UTF-8 count);
  - no non-ASCII character other than `§` in `.java`/`.sh` lines;
  - no plan, decision or review ID and no vendor name in `.java`, `.sh`, `.md`, `.json` or `.txt` lines;
  - no `isCreative()`;
  - no client class named in common-code lines.
- ✅ Counts I recomputed:
  - JUnit: the plan's one-liner over `versions/*/build/test-results/test/TEST-*.xml` gives 405 on all four variants, with 0 failures or errors on 1.20.1-forge and 1.21.1-neoforge (result files from 18:57).
  - In-world: `head2-gametests.log` has four `OK <variante>: 246 test obbligatori passati, nessun avviso fuori elenco`.
  - The `Cannot unmount` WARN appears 4 times, once per variant.
  - `head2-build.log` has `BUILD SUCCESSFUL` with no warning lines.
  - The five `ok` lines of `shots-neo-2.log` and `shots-forge-1.log` match the report, and both `log-check.txt` files are empty (0 bytes).
- ✅ Out-of-diff reads, each for a named risk:
  - Changed files read outside their hunks:
    - `StationMenu.java`: `stillValid` reads only the record, not the entity, so `unmountGivesThePieceBack`'s menu stays valid after `discard`; there is no `removed(`.
    - `StationViewTest.java`: the `paintedHelmetAndTorso` fixture has two pieces and a cell, as the brief requires.
    - `GameTestKit.java`: `runInSequence` (:709-719) only turns non-assert exceptions into `GameTestAssertException` and never ends the test. `startWhenEntitiesLoaded` (:682-702) runs the body outside the delayed-runnable iteration. So `Bench.then`/`last` steps run and their assertions fail the test.
  - Unchanged files:
    - `core/frame/FrameState.java:77-88`: `mount` replaces an occupied slot (Minor 1).
    - `content/PieceItem.toPiece`: it throws only for a slot of the wrong kind, which `FrameFitting` checks first.
    - `withSelection(` callers over `src` (Minor 2).
    - `CellGameTests.placedFrameSwapsCells` (:392-426, unchanged): it pins the creative cell swap through the shared `FrameFitting.insertCell` rule. The held cell stays and the old cell is handed back, which covers Review Focus 3's "la cella tolta va comunque al giocatore". `cellMountsSwapsAndUnmounts` (:336) mounts in creative only onto an empty record, as the brief says.
    - The plan's Task 5 text, for the README "Stato" line.
  - Other:
    - The implementer's logs under the scratchpad `logs/` folder.
    - I read the diff in passes, and the long Markdown hunks by extracting only their changed lines.
    - I ran `git rev-parse --short HEAD` (`8c1f1fe`) and `git status --short` (clean) once. Both are read-only.
- ⚠️ Cannot verify from diff:
  - **Forge real-client run predates the second commit.** `shots-forge-1.log` ran before `8c1f1fe`, which changed the widget press/release memory. That `shots-neo-2.log` ran at `8c1f1fe` rests on the implementer's report and on the file timestamps (log 18:55, head build 18:57). Controller: run `EXO_ACCEPT_EULA=true bash tools/client/m4a_shots.sh 1.20.1-forge <dir>` at head, or let Task 4's four-variant run close it.
  - **Four-variant claim in `docs/compat.md:255`.** The paragraph now says the real client "sulle quattro varianti" proves that "i clic sul pannello del telaio scelgono, smontano e montano". In this task the sequence ran only on 1.21.1-neoforge and 1.20.1-forge. Task 4 must make this true.
  - **End-of-task outputs not in the logs.** The report claims `OK sources`, `OK jars`, `OK data`, `OK art` and `OK arm_measure` at head. The log folder keeps only `OK passi` (`final-check.tmp`, `head2-check.tmp`). Controller: re-run `bash tools/check_sources.sh`, `check_jars.sh`, `check_data.sh`, `check_art.sh` and `bash tools/client/arm_measure.sh --selftest`, or accept the claim.
  - **Stale README line.** `README.md:9` still says "Un pulsante di prova va al server e torna", and that mounting arrives in later parts. Task 5 Step 2 (README "Stato") must replace both.

### Strengths
- **Exhaustive `StationAction` tests.** `StationActionTest` covers all 65 valid numbers, the encode-back round trip from −200 to 400, every hole, the byte bound, and every constructor rejection.
- **Single `TickGate` type for both gates.** It has no sentinel, so `Integer.MIN_VALUE` passes. The view gate keeps the old behaviour: `open` as the early return, `use` only on a send.
- **The hands share one rule.** `FrameInteractions` lost about 100 lines of duplicated mount/unmount code and only chooses the slot. `ExoframeGameTests` is untouched and green.
- **Per-button widget memory at head.** It remembers one bit per mouse button (`client/StationScreen.java:171-235`), and only a press of the same button off the widgets clears that button's bit. So the release of a button held on a widget never reaches vanilla, even after another button went to vanilla meanwhile, and a lost release cannot steal a later vanilla release. That this fixed a hole in the first commit comes from the implementer's report; I saw only the combined diff.
- **Click rules are pure and tested.** They are pure functions in `StationLayout`, and the tests pin the `fitsBeside` boundary at the pixel (145 = 207 − 62).
- **Reliable cleanup in `Bench`.** It cleans up on failure in the body, in intermediate steps and in the last step (`gametest/StationActionGameTests.java:633-700`). Every write sits in its own tick, except in the throttle test itself. The refusal seam opens and closes inside one synchronous body.
- **The visual checks are real.** The real-client thresholds equal half the measures, rounded down to ten. The `montaggio` check pins the release outside the screen with `execute unless entity @e[type=minecraft:item]`.

### Issues
#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)
1. **`frame/FrameFitting.java:66`, `:139`, `:161`: the guards trust the caller's `PlacedFrame` snapshot.** `FrameState.mount` silently replaces an occupied slot (`core/frame/FrameState.java:77-88`). With a stale `frame`:
   - a mount would destroy the mounted piece;
   - `unmountPiece`/`removeCell` would hand out the snapshot's durability or charge instead of the live one.

   There is no live bug: both callers read the record in the same synchronous call (`station/StationActions.java:81-82`; `FrameInteractions.onInteract`). But `FrameFitting` is now a public rule with two entry points, and tests call it directly. Fix: state in the class Javadoc that `frame` must be read from the registry in the same call.
2. **`core/station/StationView.java:126-127`: the Javadoc claims a server use that does not exist.** It says `withSelection` also serves "il server che rifa' la vista dopo una scelta". A search of `withSelection(` over `src` finds only the client prediction in `StationMenu.select` (`station/StationMenu.java:264`) and the test; the server rebuilds the view with `StationView.of`. Fix: drop the server clause.
3. **Javadoc reflow left short lines mid-paragraph.** `gametest/GameTestKit.java:89` ("…pubblico senza"), `gametest/StationMenuGameTests.java:72` ("struttura del") and `client/StationLayout.java:24-25` ("(parte 4d," / "stile A:") each break a paragraph early. Cosmetic; reflow those paragraphs.
4. **Test helpers duplicated across two classes.** `gametest/StationActionGameTests.java:543` `record` is verbatim `gametest/StationMenuGameTests.java:1061` `placedRecord`. `StationActionGameTests.java:534` `withPieces` generalises `StationMenuGameTests.java:1068` `helmetAndTorso`. Both could move to `GameTestKit` next to `scrapPiece` and `scrapPieceStack`. They are small accessors, not logic blocks.
5. **Compile-output noise not mentioned in the report.** The implementer's compile logs contain javac notes "Some input files use or override a deprecated API":
   - `step6-compile.log:8,18` on 1.21.1-neoforge and 1.20.1-forge;
   - `step6-build.log:34` on 1.20.1-fabric;
   - `step7-build.log:11,38,86`.

   On 1.20.1-fabric, javac names the unchanged `content/StationBlock.java` (`step7-build.log:38`), and I see no deprecated call in the new code, so they are most likely pre-existing. Fix: confirm with one base compile using `-Xlint:deprecation` and note the result. If they come from this diff, remove the deprecated call.

### Assessment
**Task quality:** Approved
**Reasoning:** Every item of the brief is implemented and pinned by the named tests: the button table, the `press` order, one write per tick per menu, the shared `FrameFitting` rule, the refusal seam, the click and release rules, the sprite-only widgets and the sequence. The counts (405 JUnit, 246 in-world on four variants) are confirmed from the result files. What remains is minor documentation and precondition wording, plus the unseen end-of-task outputs and the forge re-run listed under ⚠️.
