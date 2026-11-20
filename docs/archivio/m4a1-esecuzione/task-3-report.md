# Task 3 report: portata, sezione `station` della config, ricerca del telaio e `stillValid`

**Status:** DONE_WITH_CONCERNS (all work done and verified; the concerns are doc drift outside the file list and two small notes, below)
**Commit:** `6ffb904` station: portata e ricerca del telaio, stillValid legato al telaio; config: sezione station con reachBlocks (SSH-signed by the environment, `gpgsig` present, with the two trailer lines; not pushed; tree clean).

Evidence folder (outside the repository): `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t3/`

## What I implemented

- **`core/station/StationReach`** (pure Java, `core/`, no Minecraft):
  - `BELOW = 1.0`, `ABOVE = 2.0`.
  - `record Footprint(minX, minZ, maxX, maxZ, baseY)`.
  - `record Candidate(frameId, x, y, z)`; `frameId` must not be null.
  - `of(mainX, mainY, mainZ, sideX, sideZ)`: either half may be passed as the main; `IllegalArgumentException` unless exactly one of x/z differs, by 1.
  - `distance`: the Euclidean distance from the rectangle [minX, maxX+1] x [minZ, maxZ+1], `+0.0` inside and on the border.
  - `inReach`: `distance <= reach` and `baseY - 1 <= y <= baseY + 2`, borders included.
  - `nearest`: compares by distance from the rectangle, then by horizontal distance from the rectangle's centre, then by `frameId.toString()` (not `UUID.compareTo`).
  - The Javadoc gives design §5.2 and why the borders are included: feet resting on full blocks sit at integer heights, and "within 2 blocks" includes 2, as do the config bounds.
- **Config:**
  - `ExoframesConfig.Station` with `reachBlocks = 2.0`; the `station` field comes last, so Gson writes it last.
  - `ConfigValidation.MIN_REACH_BLOCKS = 0.5` and `MAX_REACH_BLOCKS = 8.0`.
  - `Ctx.between(name, v, def, min, max)`: a NaN, infinite or out-of-range value gives the default and the problem `<name>: <v> is invalid (must be between <min> and <max>), using default <def>`, with numbers in `String.valueOf`.
  - The `station` section is validated through `present(...)`.
  - The class Javadoc lists the new bound.
- **`station/StationFinder`:**
  - `Outcome { FOUND, NONE_IN_REACH, ALL_LOCKED }`.
  - `record Result(outcome, Optional<UUID> frameId)`. Its constructor rejects null components and a FOUND without a frame, or a frame without FOUND.
  - `footprint(main, mainState)`: `StationReach.of` with the main and `StationBlock.otherHalf`.
  - `inReach(frame, dimension, footprint)`: the same dimension, plus `StationReach.inReach` with `station.reachBlocks`.
  - `find` follows the brief: in-reach frames from `placedIn`, then the usable ones (`canUse`), then `nearest`.
  - `reachText`: `"2"` for 2.0, otherwise one decimal with `Locale.ROOT` (`"2.5"`).
- **`station/StationOpening(main, frameId)`:** `writeBlockPos` then `writeUUID`, read back in the same order.
- **`station/StationMenu`:**
  - `FRAME_GONE`, `opening(player, main, frame)` and `frameId()`. `player` is unused until Task 4, as the plan's signature says.
  - The footprint is computed once in the server constructor, from the block state at `opening.main()`.
  - The frame rule in `stillValid` comes after Task 1's three silent checks, inside the existing try/catch. The record must still be `placed`, be `StationFinder.inReach` of the footprint, and pass `canUse`, which is read again on every call. The first failure sends `FRAME_GONE` to the action bar once and sets a per-menu flag; from then on the menu returns false, even if the frame becomes usable again.
  - The client menu is always valid.
- **`content/StationBlock`:**
  - Adds `NO_FRAME`.
  - On the server, `onClick` uses `mainPos`. It passes `state.setValue(PART, MAIN)` as the main's state, because both halves share `FACING`; the side half's own state would shift the footprint one block east.
  - `NONE_IN_REACH` puts `NO_FRAME` with `reachText(reachBlocks)` on the action bar and returns `CONSUME`. `ALL_LOCKED` puts `FrameInteractions.LOCKED` there and returns `CONSUME`.
  - `FOUND` reads the record, then calls `StationMenu.open(player, StationMenu.opening(player, main, record))` and returns `SUCCESS`.
  - The off-hand, spectator, dead-player and client paths are unchanged.
- **Language files:** the two keys sit after `charger.charging` and before the test button, with the texts from "Testi" in both files.
- **`gametest/GameTestKit`:**
  - `openStation` now goes through `StationFinder.find`. It asserts `FOUND`, reads the record, and builds the server menu with `StationMenu.opening` into `player.containerMenu`.
  - New `frameOnStation(helper, state, relativeMain)`: `placeFrame` at (x + 0.5, y + 1, z + 0.5), yaw 0.
- **`gametest/StationMenuGameTests`:** the nine tests of the brief (229 in-world in all).
  - The two Task 1 tests now place a frame of the player on the station and remove it at the end.
  - Every test cleans up its menus, frames and records, and the station, on the failure path too. Delayed tests clean up in the delayed body's `finally`, or right away if the synchronous part throws before the delay is scheduled.
  - The waits are `runAfterDelay(2, () -> runInSequence(...))`.
- **`tools/client/m4a1_shots.sh`:**
  - Phases "senza telaio" and "telaio", and "chiusura" with `/exoframe remove`, with the exact steps of the phase table.
  - `shots=(stazione-posata nessun-telaio stazione-aperta premuto telaio-tolto)`.
  - The header now covers phases 1-10, the frame's position, what the sequence does not prove (the in-world tests prove the rest), and the measured duration.
  - No new measured check, so it still prints three `ok` lines.
- **`README.md`:** one sentence at the end of the configuration paragraph, as Step 8 describes.

### Deferred findings this task closes

- **Task 1 minor** (`placeholderButtonIgnoresSpectatorsAndTheDead` had no positive control): `helper.assertTrue(menu.stillValid(p), …)` now comes right after `openStation`, and again after switching back to survival, before `setHealth(0f)`. This closes that finding.
- **Task 2 minor** (no test of a side-half click going through `mainPos`): `clickWithNoFrameInReachSaysSo` clicks the main and then the side half and gets two `NO_FRAME` messages, which covers it. `clickOnlyNearLockedFramesSaysLocked` also clicks the side half with a locked frame that only the true footprint reaches, so it also covers the main-state derivation in `onClick` (mutation M2 below).

### Additions beyond the brief's wording (same test count)

- **`modMessages(player)`** (private in `StationMenuGameTests`): it counts only the mod's `message.redstone_exoframes.*` keys. The first neoforge run failed `clickOnlyNearLockedFramesSaysLocked` and `menuClosesForOthersWhenTheOwnerLocks`. The cause: every mock player created later, by the same test or another one, sends a `multiplayer.player.joined` line to the earlier ones, and the game-test framework broadcasts "X passed!" lines. The production code was right.
  - Tests with a second player, or with a delay, now count the mod's messages.
  - `clickWithOffHandOrAsSpectatorDoesNothing` keeps the brief's `messagesTo(A)` empty check. It is a single-player, synchronous body, so no other test can run in between, and it passed on all four variants.
- **Extra assertions**, each cheap and each tied to a rule of this task:
  - `finderUsesTheFootprintAndTheHeight`: a record in the Nether right above the main is not in reach for the overworld station, but would be for its own dimension (covers the dimension clause of `inReach`).
  - `clickWithNoFrameInReachSaysSo`: the server result is `CONSUME`, and `reachText` gives 2.0→"2", 8.0→"8", 2.5→"2.5", 0.5→"0.5". The `"2.5"` branch has no other coverage, since JUnit stays at 342.
  - `clickWithOffHandOrAsSpectatorDoesNothing`: the server does not consume the action, and a control click (main hand, survival) does give `NO_FRAME`.
  - `clickOnlyNearLockedFramesSaysLocked`: the side-half click with the frame west of the side half, at 1.5 blocks from the true footprint and 2.5 from a footprint taken from the side's own state.
  - `menuClosesForOthersWhenTheOwnerLocks`: after unlocking again, B's closed menu stays invalid and repeats no message. This is the brief's "poi sempre falso" rule.
  - `menuClosesWithoutAMessageForOtherReasons`: the client menu built from the buffer reads back `stationPos` and `frameId`. This is the only Task 3 check of the `writeUUID`/`readUUID` round trip.
  - Premises are asserted before relying on them: `ownerOnlyFrames`, `!canUse` for the locked cases, `reachBlocks == 2.0`, a separate Nether level, and the side half following the main when it is removed.

## Tests and results

### TDD evidence

- **Step 1 RED** (`step1-red.txt`): `./gradlew --console=plain :1.21.1-neoforge:test --tests '*StationReachTest'` → `BUILD FAILED`. `compileTestJava` failed with `error: package StationReach does not exist` and `cannot find symbol` on every use, as expected before `StationReach` existed.
- **Step 2 GREEN** (`step2-green.txt`): the same command → `BUILD SUCCESSFUL`. The XML shows `StationReachTest tests="8" failures="0" errors="0"`.
- **Step 3 RED** (`step3-red.txt`): `./gradlew --console=plain :1.21.1-neoforge:test --tests '*ExoframesConfigTest' --tests '*ConfigFileTest'` → `BUILD FAILED`, `cannot find symbol` for `station`, `MIN_REACH_BLOCKS` and `MAX_REACH_BLOCKS`. This was expected.
- **Step 4 GREEN** (`step4-green.txt`): the same command → `BUILD SUCCESSFUL`, with `ExoframesConfigTest tests=14 failures=0` and `ConfigFileTest tests=18 failures=0`.
- **Step 5 RED** (`step5-compile-red.txt`): `./gradlew --console=plain :1.21.1-neoforge:compileJava` with only the tests written → `BUILD FAILED`, with 84 errors. The missing symbols were `StationFinder`, `frameOnStation`, `FRAME_GONE`, `NO_FRAME` and `frameId()`, and the old one-argument `StationOpening` constructor no longer matched. All of this was expected, because none of it existed yet.
- **Step 6 GREEN:**
  - `:1.21.1-neoforge:compileJava :1.20.1-forge:compileJava` → `BUILD SUCCESSFUL`.
  - `bash tools/run_gametests.sh 1.21.1-neoforge` → `OK 1.21.1-neoforge: 229 test obbligatori passati, nessun avviso fuori elenco`. That is `step6-gametests-neoforge-3.txt`; runs `-1` (the join-message failure above) and `-2` came before it.
  - Then the four-variant build (`step6-build.txt`, 342 JUnit on each variant, 0 failures) and `bash tools/run_gametests.sh` (`step6-gametests.txt`, four `OK … 229` lines).

### Mutation checks (are the new tests vacuous?)

Each run was `bash tools/run_gametests.sh 1.21.1-neoforge` with temporary edits. Afterwards the sources were restored from `mutation-backup/`, and `cmp` confirmed it.

- **Run A** (`mutation-A.txt`) failed exactly 5 tests.
  - **M1**, the frame rule removed from `stillValid`:
    - `menu_closes_when_the_frame_is_picked_up` ("still open after the frame was picked up");
    - `menu_closes_when_someone_enters_the_frame`;
    - `menu_closes_for_others_when_the_owner_locks`.
  - **M2**, `onClick` passing the clicked state instead of the main's: `click_only_near_locked_frames_says_locked`, which got `no_frame` instead of `locked` on the side half.
  - **M3**, no dimension check in `inReach`: `finder_uses_the_footprint_and_the_height` ("the dimension does not decide the reach…").
- **Run B** (`mutation-B.txt`) failed exactly 5 tests.
  - **M4**, the message on every failed check, and the menu valid again when the frame comes back:
    - `menu_closes_when_the_frame_is_picked_up` ("frame gone messages after two more checks");
    - `menu_closes_for_others_when_the_owner_locks` ("B's closed station menu came back…").
  - **M5**, `find` ignoring `canUse`:
    - `finder_picks_the_nearest_usable_frame`, which found L instead of U;
    - `click_only_near_locked_frames_says_locked`, where the click tried to open the menu: "Payload neoforge:advanced_open_screen may not be sent to the client!".
  - **M6**, the client read losing the frame id: `menu_closes_without_a_message_for_other_reasons` ("the client menu read …").

### Step 7: the real client, 1.21.1-neoforge only

I ran `./gradlew --stop`, then `EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh 1.21.1-neoforge /tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t3/shots-neoforge`. It exited 0 after 133 s, of which 128 s was `client_shot.sh` (`client-neoforge.txt`):

```
ok anteprima: ritaglio magenta=1193 riquadro=41x85+604+152; schermata magenta=1193 riquadro=41x85+604+152
ok orientamento: meta' destra del ritaglio magenta=0 riquadro=0x0+0+0
ok pulsante: pixel=1112 copertura=0.21 mirino=0 barra=0 energia=0 riquadro=36x32+730+410
OK 1.21.1-neoforge: scatti in /tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t3/shots-neoforge
```

- **Measurements:** the same values as Tasks 1 and 2 (1193 / 0 / 1112). The painted frame behind the opaque screen does not leak into the measured rectangles.
- **Logs:** `log-check.txt` is 0 bytes. Every command step succeeded, among them:
  - `execute if entity @e[type=redstone_exoframes:exoframe,x=0.5,y=-59,z=1.5,distance=..0.2]`;
  - `exoframe remove` ("Exoframe removed; its contents are on the ground");
  - `execute unless entity @e[type=redstone_exoframes:exoframe]`;
  - `kill @e[type=minecraft:item]`, in the same second as the remove, well inside the 2 s pickup delay.
- **What I saw in the shots:**
  - `shots-neoforge/nessun-telaio.png`: no screen, the two halves seen from the front, and on the action bar "No exoframe nearby: place one on the station or within 2 blocks".
  - `shots-neoforge/telaio-tolto.png`: no screen (pitch −40, sky), and on the action bar "The exoframe is no longer available at the station".
  - `shots-neoforge/stazione-aperta.png`: the screen opened on the frame, with the fixed suit in the preview. The magenta-painted frame's legs show in the world above the screen, outside the measured rectangles.
- **After the last comment edits:** `bash -n` → `syntax-ok`, and `M4A1_SOLO_CONTROLLI=1 bash tools/client/m4a1_shots.sh 1.21.1-neoforge …/shots-neoforge` → the same three `ok` lines and `OK`, exit 0.
- **Steps without the game:** `EXO_SHOT_STEPS_ONLY=true` printed `OK passi: 33`.

### Step 9: end-of-task commands, after the last edit

| Command | Result |
|---|---|
| `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` | `BUILD SUCCESSFUL` (`step9-build.txt`; the tasks were up to date, because only `README.md` and the script changed after the step 6 build, which ran the four `:test` tasks fresh) |
| JUnit count (`step9-junit.txt`) | `1.20.1-forge 342`, `1.20.1-fabric 342`, `1.21.1-fabric 342`, `1.21.1-neoforge 342`, all with 0 failures and 0 errors |
| `bash tools/check_sources.sh` | `OK sources` |
| `bash tools/check_jars.sh` | `OK jars` |
| `bash tools/check_data.sh` | `OK data` |
| `bash tools/check_art.sh` | `OK art` |
| `bash tools/client/arm_measure.sh --selftest` | `OK arm_measure` |
| `bash tools/client/client_shot.sh --selftest` | `OK passi` |
| `bash tools/run_gametests.sh` (`step9-gametests.txt`) | four lines `OK <variante>: 229 test obbligatori passati, nessun avviso fuori elenco` |

The checks' output is in `step9-checks.txt`.

**javac:** with `-Xlint:deprecation` (`lint.txt`, on 1.21.1-neoforge and 1.20.1-forge), the warnings in the touched files all come from Task 2's existing code (`hasCorrectToolForDrops`, `rotate`, `updateShape`, `mirror`). Nothing in the new code is deprecated.

## Files changed (16, in `6ffb904`)

- Created:
  - `core/station/StationReach.java`
  - `station/StationFinder.java`
  - `test/core/station/StationReachTest.java`
- Modified:
  - `core/config/ExoframesConfig.java`, `core/config/ConfigValidation.java`
  - `test/core/config/ExoframesConfigTest.java`, `test/configfile/ConfigFileTest.java`
  - `content/StationBlock.java`
  - `station/StationOpening.java`, `station/StationMenu.java`
  - `gametest/GameTestKit.java`, `gametest/StationMenuGameTests.java`
  - `en_us.json`, `it_it.json`
  - `README.md`
  - `tools/client/m4a1_shots.sh`

## Self-review

- **Line length:** no new line exceeds 120 characters, counted in characters. The long lines left in `ConfigValidation` were already there, outside my hunks.
- **ASCII apostrophes** in the new Javadoc. The `§` signs follow the existing Javadoc.
- **No `isCreative()`** in the new code. The existing `FramePermissions.canUse` still uses it, unchanged.
- **The count:** nine new `add(tests,` lines, 229 in all; `run_gametests.sh` counts them.
- **The JUnit test names** are the brief's, verbatim.

## Concerns

1. **Docs that this change makes stale, outside my file list.** I left them unchanged, because Task 6 rereads `docs/compat.md` against the code and rewrites the m4a1 paragraph of `tools/client/README.md`:
   - `docs/compat.md:135`, the CompatBlock "Uso" paragraph, still says the click opens with `new StationOpening(mainPos(pos, state))`, with no search.
   - `docs/compat.md:251`, "I dati extra", says `StationOpening` carries only the main's position (`writeBlockPos`/`readBlockPos`). It now carries `frameId` too (`writeUUID`/`readUUID`).
   - `docs/compat.md:254`, "Tenere aperto", has no frame rule and no `FRAME_GONE`.
   - `docs/compat.md:255` lists only the two Task 1 tests.
   - `tools/client/README.md:153`, the m4a1 paragraph, still says `Escape` closes the screen, and lists neither the "senza telaio" and "telaio" phases nor the two new shots.
   - `tools/client/README.md:206` still gives the m4a1 duration as "misurati 126 s su 1.21.1-neoforge"; the script header now says 133 s (128 s of it `client_shot.sh`).
2. **The server constructor of `StationMenu` assumes the station's main half is there.** It computes the footprint from `level.getBlockState(opening.main())`, and a non-station state would throw `IllegalArgumentException` (the `FACING` lookup). Both callers, the click and `GameTestKit.openStation`, build the menu right after a successful `find` in the same tick, so it cannot happen today. The constructor's Javadoc states the precondition.
3. **The brief's `messagesTo(A)` empty check is fragile in principle.** `clickWithOffHandOrAsSpectatorDoesNothing` relies on it, and it holds only because the body is synchronous and creates a single player. A later edit that adds a second mock player or a delay would make it pick up join lines; `modMessages` is the robust form.
4. **The deferred Task 2 minor about the header line** "nessun oggetto che sblocca ricette … (la sequenza non ne da')" is untouched; it is recorded for the final review. The new "telaio" phase adds no recipe-unlocking item: `debug wear` and the exit with V only move placeholders, and every recipe of the mod unlocks with the copper ingot only.
5. **The client run and its duration are 1.21.1-neoforge only**, as Step 7 says. The four-variant client runs are Task 6's.
6. **Carry-over for Task 6: a pickup race.** The "chiusura" phase's `kill @e[type=minecraft:item]` (the plan's step) succeeds only while the pieces and cell that `/exoframe remove` drops are still on the ground, inside the 2 s pickup delay (`Handouts.PICKUP_DELAY`, 40 ticks). The player stands about one block away and would pick them up after that. On 1.21.1-neoforge `kill` ran in the same second as the remove. If that step fails on a slower variant in Task 6, the cause is this race, not the station.

## Fix before review (ruling P16)

Ruling P16 (concern 1): fix the lines that described the old behaviour now, as a second commit of Task 3, before the review.

**Commit:** `913b6bc` docs: la stazione nei documenti dopo la ricerca del telaio (compat.md, README del client). It is SSH-signed (`gpgsig` present) and carries the two trailer lines; not pushed; tree clean. It changes 2 files, 6 insertions and 6 deletions. The edits are text only, in Italian, in the style of the surrounding text, with ASCII apostrophes.

### What changed

The line numbers did not change, because each paragraph is one line.

- **`docs/compat.md:135`**, CompatBlock "Uso", the last sentence about the click:
  - on the client the click succeeds and that's all;
  - on the server it searches for a frame with `StationFinder.find`, for the main half through `mainPos`, deriving the main's state from the clicked one with `PART` `main`;
  - `NO_FRAME`, with `station.reachBlocks` written by `reachText`, gives `CONSUME`;
  - `FrameInteractions.LOCKED` gives `CONSUME`;
  - a frame found gives `StationMenu.open` with `StationMenu.opening(...)` and `SUCCESS`.
- **`docs/compat.md:251`**, "I dati extra":
  - `StationOpening` now also carries the `frameId`, fixed for the menu's whole life (`writeBlockPos`/`writeUUID`, read back in the same order);
  - the server constructor computes the footprint once from the main's state (`StationFinder.footprint`; the main must be there);
  - the click opens only once the frame is found, with `StationMenu.opening`.
- **`docs/compat.md:254`**, "Tenere aperto":
  - the three base checks fail without a message;
  - then the frame rule (`placed`, `StationFinder.inReach`, `FramePermissions.canUse` read on every call);
  - `FRAME_GONE` once per menu on the action bar;
  - after that the menu is never valid again, even if the frame comes back.
- **`docs/compat.md:255`**, the in-world tests:
  - `openStation` goes through `StationFinder.find` and `StationMenu.opening`;
  - the real click (`clickBlock`) is used only where it opens nothing;
  - the eleven `StationMenuGameTests` are listed by name, in four groups.
- **`tools/client/README.md:153`**, the m4a1 paragraph:
  - the "senza telaio" phase (shot `nessun-telaio`);
  - the "telaio" phase (`/exoframe debug wear …`, V, the frame at 0,5 −59 1,5);
  - the opening on the frame;
  - the screen closing through `/exoframe remove` instead of `Escape` (shot `telaio-tolto`, `execute unless entity`, `kill`).
- **`tools/client/README.md:206`**, the m4a1 duration: 133 s on 1.21.1-neoforge; 126 s before the frame phases.

### Covering checks

- `git diff -U0` hunks, before committing: `docs/compat.md` 135, 251, 254-255; `tools/client/README.md` 153, 206. Nothing else.
- Typographic apostrophes or quotes (U+2018, U+2019, U+201C, U+201D) in the added lines: none, checked character by character with Python. A byte-wise `grep` had first counted 3, matching the bytes of the existing `−` and `…`.
- `bash tools/check_sources.sh` → `OK sources`, exit 0.
- `git diff --stat`, before the commit:
  ```
   docs/compat.md         | 8 ++++----
   tools/client/README.md | 4 ++--
   2 files changed, 6 insertions(+), 6 deletions(-)
  ```
- `git show --stat HEAD` gives the same two files.

Concerns 2-4 need no action now, per the ruling; concern 4 (the pickup race) goes into Task 6's dispatch.

## Fix round 1 (ruling P17)

**The finding (Important):** clicking a lone side half with a usable frame in reach threw an uncaught `IllegalArgumentException`. `onClick` searched with a main state derived from the clicked half and never read the block at the main position. The search gave `FOUND`, and `StationMenu`'s server constructor then read the real block there (air) and failed on `getValue(FACING)`. `docs/compat.md:251` (written in P16) claimed this could not happen.

**Commit:** `0e7b20f` station: il clic su una meta' laterale sola non apre nulla (legge la principale vera prima della ricerca). It is SSH-signed (`gpgsig` present) and carries the two trailer lines; not pushed; tree clean. It changes 3 files, 62 insertions and 8 deletions.

### What changed

- **`content/StationBlock.onClick`**, on the server only (the client path is unchanged):
  - it reads the real block state at `mainPos(pos, state)`;
  - unless that state is this block with `PART` `MAIN` and the clicked half's `FACING`, it returns `CONSUME` with no message. That is the result a click that opens nothing already gives (no frame in reach, or all locked);
  - otherwise it passes that real state to `StationFinder.find`;
  - the derived state (`state.setValue(PART, MAIN)`) and its comment are gone, and the Javadoc describes the new check.
- **`gametest/GameTestKit.openStation`:** unchanged. It already reads the world, `StationFinder.find(player, level, main, level.getBlockState(main))`, so the ruling's condition does not apply.
- **`docs/compat.md`:**
  - **:251** now states the guarantee as it holds: the main half is there because, before the search, the click checks that the main's position holds this station's main half (the same block, `PART` `main`, the same `FACING` as the clicked half) and opens the menu in the same tick; a lone side half opens nothing.
  - **:135**, the click in CompatBlock's "Uso", also changed. It said the main's state was taken from the clicked one, which this fix makes false; it now describes the real-state read and the early `CONSUME`. This is the only edit beyond the ruling's list, and it is the same fix.
- **`gametest/StationMenuGameTests`** (still 229 in all; no new `add(tests,` line):
  - `clickWithNoFrameInReachSaysSo` now ends by calling a new helper, `assertLoneSideHalfOpensNothing`. The helper:
    - places a lone side half facing north with `helper.setBlock` at `LONE_SIDE` (5, 1, 1);
    - asserts the premise that the half stayed and (6, 1, 1), where its main would be, is air;
    - places a frame of A on top of it at (5.5, 2.0, 1.5), in reach of the footprint the old code derived;
    - clicks the lone half with A, catching any `RuntimeException`;
    - asserts no exception, no menu (`containerMenu == inventoryMenu`), no new mod message (so no `NO_FRAME` and no `LOCKED`), and `CONSUME`;
    - removes the frame and the lone half in a `finally`.
  - The frame is 2.12 blocks from the real test station's footprint, so it is out of that station's reach.

### Commands and outputs

The evidence is in `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t3/`.

- **RED**, with the new check against the old `onClick`: `bash tools/run_gametests.sh 1.21.1-neoforge` (`fix1-red-gametests.txt`) → `FAIL 1.21.1-neoforge: Gradle e' uscito con 1`, one test failed:
  ```
  redstone_exoframes:click_with_no_frame_in_reach_says_so failed at …! the click on a lone side half threw java.lang.IllegalArgumentException: Cannot get property DirectionProperty{name=facing, clazz=class net.minecraft.core.Direction, values=[north, south, west, east]} as it does not exist in Block{minecraft:air}
  ```
  This is the reviewer's exception.
- **GREEN**, after the fix: `bash tools/run_gametests.sh 1.21.1-neoforge` (`fix1-green-gametests-neoforge.txt`) → `OK 1.21.1-neoforge: 229 test obbligatori passati, nessun avviso fuori elenco`.
- **Build:** `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` (`fix1-build.txt`) → `BUILD SUCCESSFUL`. The four `:test` tasks ran fresh.
- **JUnit count** (`fix1-junit.txt`), the command from the Global Constraints:
  ```
  1.20.1-forge 342
  1.20.1-fabric 342
  1.21.1-fabric 342
  1.21.1-neoforge 342
  ```
  0 failures and 0 errors on each.
- **`bash tools/run_gametests.sh`** (`fix1-gametests.txt`) → exit 0:
  ```
  OK 1.20.1-forge: 229 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 229 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 229 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 229 test obbligatori passati, nessun avviso fuori elenco
  ```
- **`bash tools/check_sources.sh`** (`fix1-check-sources.txt`) → `OK sources`, exit 0.
- **Style:** the added Java lines are 120 characters or fewer and have no typographic apostrophes; the added doc lines have none either, both checked with Python.
- **No client run**, per the ruling.
