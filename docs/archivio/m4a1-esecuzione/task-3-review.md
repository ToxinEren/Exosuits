# Task 3 review: portata, sezione `station`, ricerca del telaio, `stillValid` (31da028..913b6bc)

## Spec Compliance

### ✅ Spec compliant: every item of the brief is implemented, file by file

All 16 files of the brief's list, plus the two P16 docs, have their hunk.

**core**
- `core/station/StationReach.java`: has all the "Tipi nuovi" signatures.
  - `BELOW`/`ABOVE`, `Footprint`, `Candidate`.
  - `of` at `:60`; it throws IAE unless `dx + dz == 1`.
  - `distance` at `:74`: Euclidean from the rectangle, `+0.0` on the border.
  - `inReach` at `:84`: both bounds inclusive.
  - `nearest` at `:95`, tie-broken by `frameId().toString()` at `:102`.
  - The Javadoc gives §5.2 and why the bounds are inclusive.
- `core/config/ExoframesConfig.java`: `station` at `:24`; `reachBlocks = 2.0` at `:133`.
- `core/config/ConfigValidation.java`:
  - `MIN_REACH_BLOCKS`/`MAX_REACH_BLOCKS` at `:60`/`:62`;
  - `present(input.station, …)` at `:182`;
  - `Ctx.between` at `:356`, with the exact problem string;
  - the class Javadoc lists the bound.

**station / content**
- `station/StationFinder.java`:
  - `Outcome`;
  - `Result` at `:45`;
  - `footprint` at `:62`;
  - `inReach` at `:71`: same dimension plus `StationReach.inReach` with `station.reachBlocks`;
  - `find` at `:84`: in reach, then `canUse`, then `nearest`, in the brief's order;
  - `reachText` at `:107`.
- `station/StationOpening.java`: `(main, frameId)`; `writeUUID`/`readUUID` at `:26`/`:35`, after the position.
- `station/StationMenu.java`:
  - `FRAME_GONE` at `:52`; `opening` at `:112`; `frameId()` at `:122`;
  - the footprint is computed once in the server constructor (`:83`).
  - `stillValid` at `:166-192`: the three Task 1 checks first, failing silently; then the frame rule (`placed`, `inReach`, `canUse` re-read on each call); `FRAME_GONE` once on the action bar; sticky `frameGone`; all inside the existing `RuntimeException` catch (DEC-39, `:184`). The client menu is always valid.
- `content/StationBlock.java`:
  - `NO_FRAME`.
  - `onClick` at `:231-260`. Off-hand, spectator and dead players `PASS`; the client gets `SUCCESS`. On the server it calls `find` for `mainPos`:
    - `NONE_IN_REACH` → `NO_FRAME` with `reachText`, `CONSUME`;
    - `ALL_LOCKED` → `FrameInteractions.LOCKED`, `CONSUME`;
    - `FOUND` → `StationMenu.open(…, StationMenu.opening(…))`, `SUCCESS`.
- Language files: both keys at `en_us.json:58-59` and `it_it.json:58-59`. They sit after `charger.charging` and before the test button key, with the texts of "Testi".

**tests**
- `StationReachTest`: the eight tests of Step 1 with the brief's points.
- `ExoframesConfigTest`:
  - `defaultsMatchSpec` asserts 2.0 (`:65`);
  - `stationReachBlocksStaysBetweenHalfAndEight` (`:69`) checks the bounds, 0.5 and 8.0 kept, 0.49/8.01/NaN/±∞ with the exact problem string, and `station = null`.
- `ConfigFileTest`:
  - `fileWithoutTheStationSectionTakesItsDefaultsSilently` (`:151`) writes the defaults tree minus `station`, pretty-printed. It asserts no problems, 2.0, `wroteDefaults` false, and the bytes unchanged.
  - `nullStationSectionIsOneProblem` (`:169`).
- In-world tests:
  - `GameTestKit.openStation` (`:817`) asserts `FOUND`, re-reads the record, and builds the server menu with `StationMenu.opening`. `frameOnStation` (`:836`) places at (x+0.5, y+1, z+0.5), yaw 0.
  - `StationMenuGameTests` has nine new `add(tests,` lines (`:90-101`), so 220 + 9 = 229.
  - Each test matches its Step 5 bullet: positions, outcomes, message counts, `getArgs()` = `{"2"}`, the two cursor ingots back with an empty cursor, and two more `stillValid` calls leaving one message.
  - Both Task 1 tests now stand a frame of the player on the station and remove it.
  - The allowed extra is present: the positive controls in `placeholderButtonIgnoresSpectatorsAndTheDead` (`:156` and after switching back to survival).

**tools / docs**
- `tools/client/m4a1_shots.sh`:
  - phases "senza telaio", "telaio" and "chiusura" step for step as in the phase table (`:128-171`);
  - `shots` gains `nessun-telaio` and `telaio-tolto`;
  - no new measured check, so still three `ok`;
  - the header is renumbered 1-10, with the "non prova" list and the duration updated.
- `README.md`: the Step 8 sentence, in the configuration paragraph.
- P16: `docs/compat.md` 135, 251, 254 and 255, and `tools/client/README.md` 153 and 206, are rewritten for the new behaviour. They are accurate, except for one false guarantee at 251 (Important 1).
- P12: untouched by this diff; no new measuring helper is needed.

**Counts:** 331 + 8 + 1 + 2 = 342 JUnit, and 229 in-world tests, as in the tables.

**Extras beyond the brief, judged acceptable:**
- the `Result` invariants (FOUND ⇔ frame present);
- `modMessages`, the robust count of the mod's messages, needed because join lines from later mock players reach earlier ones (`:662-672`);
- the Nether-record dimension check in `finderUsesTheFootprintAndTheHeight`;
- the `CONSUME` and `reachText` assertions in `clickWithNoFrameInReachSaysSo`;
- the control click in `clickWithOffHandOrAsSpectatorDoesNothing`;
- the side-half click in `clickOnlyNearLockedFramesSaysLocked`;
- the re-unlock check in `menuClosesForOthersWhenTheOwnerLocks`;
- the client-menu `stationPos`/`frameId` read-back;
- the `-∞` config case;
- the textual-vs-`UUID.compareTo` tie test.

All are cheap and each pins a rule of this task. Where `menuClosesForOthersWhenTheOwnerLocks` checks `modMessages(a).isEmpty()` instead of "no messages" for A, the deviation is justified by the join lines.

### ⚠️ Cannot verify from the diff
- **The real client on the other three variants.** Step 7 asks for 1.21.1-neoforge only, and Task 6 runs all four. I looked at `nessun-telaio.png` and `telaio-tolto.png`: both show no screen and the expected action-bar text.
- **The `ALL_LOCKED` action-bar flag.** It is in code only (`StationBlock.java:247-248` passes `true`). No test or shot asserts the overlay flag; `NO_FRAME` and `FRAME_GONE` are confirmed as action-bar text only by the neoforge shots.
- **A pickup race in "chiusura".** The `kill @e[type=minecraft:item]` (`m4a1_shots.sh:153`) succeeds only within the 2 s pickup delay after `/exoframe remove`. It is the plan's step and is carried to Task 6 (report concern 6).
- **A stale test count in `README.md:31`.** It still says "oggi 208 test" while the count is 229. This predates the task (stale since Task 1) and is outside its hunks.
- **Where the exception of Important 1 lands on 1.20.1 and the loaders.** Only vanilla 1.21.1 was checked.
- **The commit claims.** The SSH signatures, the two trailer lines and the clean tree are the report's word. I did not consult git (read-only review, diff file only).

## Strengths
- **`StationReach` is small, pure and deterministic.**
  - The tie-break on the textual id, with a test that would catch `UUID.compareTo` (`StationReachTest`, the `ffffffff-…` case), closes a real trap.
  - `+0.0` on the border keeps `assertEquals(0.0, …)` exact.
- **The `stillValid` rewrite is well-shaped.**
  - The three silent checks come first and the frame rule last.
  - The `frameGone` flag gives "once per menu, then always false".
  - It all stays inside the existing per-menu ERROR catch.
  - The footprint is computed once, as the brief asks.
- **The tests verify real behaviour, and they bite.** I checked the mutation logs in the evidence folder: `mutation-A.txt` and `mutation-B.txt` each end with "5 required tests failed", and the failing names and messages match the report's M1-M6.
  - The side-half click (`clickOnlyNearLockedFramesSaysLocked`, `:288`) is placed so that only the true footprint reaches the frame. It catches a footprint derived from the side's own state.
  - The Nether record catches a missing dimension clause.
- **Cleanup is careful.**
  - The `delayed` flag pattern cleans on both the synchronous and the delayed failure path.
  - The double removals are safe. `removeFrame` tolerates a missing record; `leaveTestFrame` is `DebugWear.remove`, which deletes B's worn frame.
- **The config work is to the letter, with exact problem strings.** The 3c-shaped file test also asserts that the file bytes are unchanged (D5).
- **The client sequence matches the phase table step for step.**
  - The log shows every command succeeded, and `log-check.txt` is empty.
  - The measurements are unchanged (1193 / 0 / 1112), so the painted frame behind the opaque screen leaks nothing into the measured rectangles.
- **Final evidence.**
  - `step9-junit.txt` gives 342 on all four variants.
  - `step9-checks.txt` gives all `OK`.
  - `step9-gametests.txt` gives four `OK … 229 … nessun avviso fuori elenco`.
  - The `deprecated API` notes in `step6-build.txt` come from pre-existing code (`lint.txt` lists only Task 2 lines of `StationBlock` (149-215), `ChargerBlock` and older test classes, none of this diff's new code).

## Issues

### Critical (Must Fix)
None.

### Important (Should Fix)

**1. A click on a lone side half now throws out of the click handler, and the docs promise this cannot happen.**

`StationBlock.java:240-243` never reads the block at `main`. It derives the main's state from the clicked one (`state.setValue(PART, StationPart.MAIN)`) and searches with that. The server constructor then reads the real block at `main` (`StationMenu.java:83`) and calls `StationBlock.otherHalf` → `towardOtherHalf`, which does `state.getValue(FACING)` (`StationBlock.java:93-95`). When the main half is not there, that throws `IllegalArgumentException`.

So a successful `find` does **not** establish the constructor's precondition on the click path. The report's concern 2 (report line 187: "it cannot happen today") is wrong for the click. The P16 commit wrote the same false guarantee into `docs/compat.md:251` ("la principale deve esserci, come dopo la ricerca del clic"); the constructor's Javadoc (`StationMenu.java:78-80`) states the precondition.

- **Reachable with plain game commands.**
  - Verified with javap on vanilla 1.21.1:
    - `SetBlockCommand.setBlock` places through `BlockInput.place(level, pos, 2)`, then calls `blockUpdated`.
    - `BlockInput.place` keeps the raw state when `Block.updateFromNeighbourShapes` yields air.
    - The station's `updateShape` (`StationBlock.java:149-155`) yields air for a lone half.
    - So `/setblock <pos> redstone_exoframes:station[part=side]` with no main beside it places a lone side half.
  - Not javap-checked, known vanilla mechanics (lone bed halves placed with `/setblock` behave the same way):
    - the lone half then stays;
    - flag 2 and `blockUpdated` update only the neighbours;
    - a later neighbour change re-runs its `updateShape` for that direction, but only a change on the `towardOtherHalf` side (`:151`) turns it into air.
  - Clicking it with a usable frame within 2 blocks of that phantom footprint gives `FOUND` → `StationMenu.open` → the provider's constructor → IAE.
  - Nothing catches it in between: `BlockBase.useItemOn`/`use` call `onClick` directly (`compat/impl/BlockBase.java:81-96`).
  - The report's own evidence shows that an exception inside `StationMenu.open` leaves `onClick` uncaught. In `mutation-B.txt` (M5), `click_only_near_locked_frames_says_locked` failed with NeoForge's "Payload neoforge:advanced_open_screen may not be sent to the client!", thrown out through `clickBlock` → `useItemOn` → `onClick`.
- **Effect (an uncaught exception and a logged ERROR, not a crash).**
  - On vanilla 1.21.1, `PacketListener.onPacketError` rethrows as a `ReportedException`. `BlockableEventLoop.doRunTask` then logs it at ERROR with the FATAL marker and a stack trace through the mod's classes (javap). The click silently does nothing.
  - 1.20.1, Forge and NeoForge were not checked.
- **The "no action" ruling rests on this false premise.** The report (line 242) says concerns 2-4 "need no action now, per the ruling". No such ruling was among those given to this review. For concern 2, its premise ("cannot happen today") does not hold. The controller should re-decide it rather than treat this finding as already ruled.
- **This is a regression from Task 2.** The same click used to build the menu without reading the block (`new StationOpening(mainPos(pos, state))`). The menu then closed on the next tick through the vanilla block check.
- **Fix.**
  - In `onClick`, read the real state at `main`. If it is not this block with `PART == MAIN` and the clicked half's `FACING`, return without searching (`CONSUME`, silent or with a message of the coordinator's choosing).
  - Pass that real state to `find`. Search and constructor then derive one footprint from one state, as `GameTestKit.openStation` already does (`GameTestKit.java:820`).
  - Correct `docs/compat.md:251` to say the click guards it.
  - Add a lone-side assertion to an existing click test (keeps the 229 count). For example: `level.setBlock(side, sideState, 2 | 16)` with no main, plus a usable frame in reach; `clickBlock` → no exception, no menu.

### Minor (Nice to Have)

1. **`StationMenuGameTests.java:329-342`: the spectator half of `clickWithOffHandOrAsSpectatorDoesNothing` cannot fail on this code.** Vanilla `ServerPlayerGameMode.useItemOn` sends a spectator through `getMenuProvider` and returns `PASS` before the block is called (javap, 1.21.1). So this half proves the observable outcome, as the plan asks, but not the `player.isSpectator()` guard in `onClick` (`StationBlock.java:233`). The test's wording is plan-mandated; worth knowing when reading its coverage.
2. **`StationMenuGameTests.java:512-573`: `menuClosesWithoutAMessageForOtherReasons` does not pin the order of the checks.** The frame stays available throughout, so "no `FRAME_GONE`" would also hold if the frame rule ran before the silent checks.
   - To pin it, remove the record while A is a spectator (or in the Nether), call `stillValid`, and assert false with zero `FRAME_GONE`.
   - The Javadoc at `:508` says validity "torna vero ogni volta che la ragione se ne va". The test asserts it once, after all four reasons are undone (`:552`).
3. **`StationMenuGameTests.java:149-156`: the new positive control runs before any `setGameMode`.** It relies on the mock player's default mode, against the global constraint "Ogni test imposta da sé la modalità di gioco". The omission predates this task (Task 1), but this task's assertion now depends on it. Add `p.setGameMode(GameType.SURVIVAL)` at the start.
4. **`StationFinder.java:107-111`: `reachText` follows the plan's format ("altrimenti con uno"), which can misstate the server's value.** This is plan-mandated text.
   - 2.04 gives "2.0", which hides the decimals and breaks the "no decimals if integer" look.
   - 2.25 gives "2.3", more than the real reach.
   - 1.0 gives "within 1 blocks" / "entro 1 blocchi".
5. **`tools/client/README.md:161`: one sentence is stale.** It still lists "il telaio" among the phases the m4a1 sequence will grow, but the frame phases landed in this task. This line is outside P16's list; a text-only fix.

## Checks run (named risk → what I read)
- **How the diff was read.**
  - `review-31da028..913b6bc.diff` in ten passes: lines 1-200, 200-449, 449-698, 698-947, 947-1196, 1196-1395, 1395-1644, 1644-1893, 1893-2092, 2092-2240.
  - Every hunk is complete.
  - The line numbers above are HEAD line numbers, collected with `grep -n` on the changed files.
  - No test, build, client or git command was run.
- **Lone side half: does the constructor's IAE escape the click?**
  - `StationBlock.java:83-96` (`otherHalf`/`mainPos`/`towardOtherHalf` read `FACING`/`PART`) and `:149-155` (`updateShape`).
  - `compat/impl/BlockBase.java:81-96`: `onClick` is not wrapped.
  - javap on vanilla 1.21.1:
    - `SetBlockCommand.setBlock`: places through `BlockInput.place` with flag 2;
    - `BlockInput.place`: the raw state is kept when the shape update gives air;
    - `PacketUtils`/`PacketListener.onPacketError`: rethrown as `ReportedException`;
    - `BlockableEventLoop.doRunTask`: logged at ERROR.
- **Does B's exit leave a frame behind in `menuClosesWhenSomeoneEntersTheFrame`?** `GameTestKit.java:356-358` (`leaveTestFrame` = `DebugWear.remove`, which deletes the worn frame) and `:439-442` (`removeFrame`, safe on a removed record). No leak.
- **Spectator coverage:** javap on vanilla 1.21.1 `ServerPlayerGameMode.useItemOn`, spectator branch (Minor 1).
- **The report's evidence** (`…/scratchpad/m4a1/t3/`):
  - `step9-junit.txt`, `step9-checks.txt`, `step9-gametests.txt` (verdict lines, and grep of WARN/ERROR: nothing station-related);
  - `mutation-A.txt` and `mutation-B.txt` (failing test names);
  - `client-neoforge.txt`;
  - `lint.txt` (where the deprecation warnings come from);
  - the two "da guardare" shots, viewed.
- **The binding design texts:** spec §5.2, §5.15, §5.18; each requirement maps to code or a test above.

## Assessment

**Task quality:** Needs fixes

**Reasoning:** The brief is implemented to the letter, and the tests demonstrably catch the mutations that matter. One robustness gap remains. The click searches with a main state derived from the clicked half, while the menu constructor reads the real one, so a lone side half made with `/setblock` turns a click into an uncaught IAE (an ERROR log). The report and `docs/compat.md:251` declare that impossible. The fix is a few lines in `onClick` plus a doc correction.
