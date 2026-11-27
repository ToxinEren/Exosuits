### Spec Compliance
- ✅ Spec compliant.
  - Missing: none.
  - Misunderstood: none.
  - Extra, each judged below and all justified:
    - the public `UNLOCK_DONE`/`LOCK_DONE`;
    - the `ExoframeCommand.Target` record;
    - `Bench.removeStationAndFrames()`;
    - the comment in `GameTestKit.java`, a file outside the list;
    - extra assertions in `operatorTogglesAnyLock` and `lockingFromTheStationClosesAnotherScreen`.
  - The 18-file stat touches nothing under `tools/art/`, the texture, mixins, `tools/log_allowlist.txt` or `core/`.

  I checked each brief file and each ruling against the diff:
  - **`station/StationActions.java`.** The `TOGGLE_LOCK` branch is in the plan's order: write turn (:81), then record and `canUse` (:85), then `toggleLock` (:94, :113-126). `toggleLock` gives `REFUSED` without `canToggleLock`, makes exactly one `updatePlaced`, and only after it succeeds puts `UNLOCK_DONE`/`LOCK_DONE` on the action bar (:121) and plays `UNLOCK`/`LOCK` at `frame.position()` (:123). `FAILED` reuses `StationTexts.FAILED` (:98). The 4b numbers stay `IGNORED` without taking the turn (`handled`).
  - **`frame/FramePermissions.java`.** `canToggleLock(UUID, boolean, FrameState)` (:61) returns `operator || actor.equals(owner)`. The `ServerPlayer` overload (:45) calls it with `hasPermissions(2)`. Its Javadoc names `ExoframeCommand.setUnlocked` and `source.hasPermission(2)`, and the `setUnlocked` Javadoc links back to it.
  - **`frame/FrameSounds.java`.** `LOCK(IRON_DOOR_CLOSE, BLOCKS)` and `UNLOCK(IRON_DOOR_OPEN, BLOCKS)` are added, with the table row in the Javadoc.
  - **`command/ExoframeCommand.java`.**
    - `setUnlocked` uses the shared rule with `source.hasPermission(2)` (:303).
    - `debug reinforced <valore>` uses `BoolArgumentType` (:165) inside the `debug` literal, whose `requires` asks for permission 2 (:148-149). It acts on the worn frame (`mutate`+`syncTo`) or the one in sight (`updatePlaced`), with no owner check.
    - Success replies `debug.reinforced.done` with `String.valueOf` and returns 1. No frame gives `no_frame` and 0. A refused write gives 0 with no message.
    - Ruling A7: the class Javadoc documents the branch.
  - **`compat/GameTests.java`, `compat/impl/GameTestsImpl.java`.**
    - `operatorPlayer` is `mockPlayer(helper, 2, false)` (:130); `mockServerPlayer` is unchanged in behaviour, as `mockPlayer(helper, 0, true)` (:125).
    - `getPermissionLevel()` returns `Math.max(level, super)` (:206-207), and the operators list is not touched.
    - Ruling A6: `isCreative()` is `alwaysCreative || super.isCreative()` (:201-202). The `operatorPlayer` Javadoc says it follows the game mode, and so does `docs/compat.md:85`.
  - **`client/StationLayout.java`.**
    - `SHIELD` 276,3-290,15 (:87) and `LOCK` 292,3-308,15 (:92).
    - `lockSprite` (:430) returns the existing `BLOCCO_CHIUSO_*`/`BLOCCO_APERTO_*` constants; no new `Sprite` constant.
    - `lockTooltipKeys` (:454) gives `LOCKED`/`UNLOCKED`, then `OWNER_ONLY`, then `EVERYONE`, as in the brief.
  - **`client/StationScreen.java`.**
    - The shield is drawn only when `reinforced`. The lock is always drawn, as `lockSprite(!unlocked, buttonState(canToggleLock, pressed, hover))` with `TEXTURE.blit`, inside `renderBg`'s try/finally (:137, :149-160).
    - The click sends 26 only with `canToggleLock` (:304).
    - Tooltips: the lock's keys, first line unstyled and the others `GRAY` (:373-376); the shield shows `REINFORCED`.
  - **`station/StationTexts.java` and the two language files.** The five keys follow `.holds_armor_how`, and `debug.reinforced.done` follows `debug.place.scrap`. The words are exactly those of "Testi", and all five keys are in `KEYS`.
  - **`test/client/StationLayoutTest.java`.**
    - `SHIELD` and `LOCK` are in the overlap and 4b-reservation tables.
    - The screen points are `32x24+748+44`, (764, 56) and `28x24+716+44`.
    - New `lockAndShieldTakeTheirSprites`: four states × two shapes, 16×12 and 14×12.
    - New `lockTooltipFollowsTheView`: the three cases of the brief.
  - **`gametest/StationActionGameTests.java`.** The five tests are registered and follow the brief: `ownerTogglesTheLockFromTheStation` :531, `othersCannotToggleTheLock` :561, `operatorTogglesAnyLock` :586, `lockingFromTheStationClosesAnotherScreen` :620, `debugReinforcedMarksWornAndPlacedFrames` :649.
  - **Ruling A5 (`operatorTogglesAnyLock`), step by step:**
    - It starts with an owner-less `new FrameState(config)`.
    - A: view without `canToggleLock`, `REFUSED`.
    - O: `operatorPlayer` in survival (`Bench.operator` :781), view with `canToggleLock`, `DONE`, now unlocked.
    - Tick 1: `updatePlaced(setOwner(A))`, then O's `DONE`; owner A, locked.
    - Tick 2: O gets `REFUSED` and the frame stays locked.
    - The test name is unchanged and the count stays 251.
  - **`tools/client/m4a_shots.sh`.**
    - The phases "lucchetto", "vista" and "rinforzato" replace the smoke test before the iron ingots (:274, :277, :279).
    - The checks `lucchetto`, `vista` and `rinforzato` each name their rect (:384, :392, :399).
    - `LOCK_MIN=70` comes from 144 measured and `SHIELD_MIN=270` from 552, both half of the 1.21.1-neoforge measure, with the value beside them (:214-220).
    - The header calls the measured view the `station_view` proof the 4a-1 was waiting for (§13).
  - **`docs/compat.md`.**
    - `operatorPlayer` in `GameTests` (:85).
    - In "Menu e schermate", a new bullet "Il lucchetto e lo scudo" (:257) covers 26, the single rule for menu and command, the messages, the sounds and the shield. `debug reinforced` is mentioned beside the shield (Ruling A7).
    - `Net` and "La vista del telaio" are updated.
  - **`tools/client/README.md`.** It has the new phases, rects, check rows and shots.
  - **`README.md:34`.** My word diff over diff lines 29-55 shows the only change is the appended `debug reinforced` sentence (Ruling A7, that clause only).
  - **Open point from Task 1.** Any tree of this task sits on 8c1f1fe. `scratchpad/m4a2-task2/logs/step7-forge.log` has the eight `ok` lines and `OK 1.20.1-forge`. In `forge/rcon.log`, `execute unless entity @e[type=minecraft:item]` after the release outside the screen gives "Test passed" (:151-152), and `forge/log-check.txt` is 0 bytes. The report states all this.
  - **Counts.** I ran the plan's read-only JUnit count over `versions/*/build/test-results/test/TEST-*.xml`: 407 on each of the four variants, 0 failures and 0 errors, and `StationLayoutTest` has 14 tests with timestamp 20:17 (the follow-up build). `fix-gametests.log` and `step9-gametests.log` have four `OK <variante>: 251 test obbligatori passati, nessun avviso fuori elenco` lines each.
  - **Hygiene.** I grepped the added lines for plan, decision, review and ruling IDs (`Decisione`, `P\d`, `R\d`, `Ruling`, `DEC-`, `Q\d`, `A\d`) and for vendor or model names: none found. The only non-ASCII in added Java and shell lines is `§`, the existing convention. Comments are Italian with ASCII apostrophes, and identifiers and exceptions are in English. Six Java lines show 121-124 *bytes*, but each is ≤120 characters (`§` is 2 bytes).
  - **Checks outside the diff, each against a named risk:**
    - **Lock press handling.** Risk: a press on the lock without `canToggleLock` reaches the base game. I read `StationScreen.java:194-221` because the `mouseClicked` hunk stops after `widgetAt`. Any press on a widget returns true and is remembered, and `act` sends nothing. No issue.
    - **Client prediction.** Risk: the client predicts the lock, so `lucchetto` would not prove the round trip. In `StationMenu.java:285-293`, the client branch only calls `select` for non-writing numbers; 26 returns true without touching the view. No issue.
    - **Step timing.** Risk: `then`/`last` delays are relative, which would break Ruling A5's "next tick". I read `StationActionGameTests.java:806-851` because the bodies of `run`, `then` and `last` are not in the diff. They are `runAfterDelay` from the body, so ticks 0, 1, 2 (and 1, 3) are absolute. No issue.
    - **Entity summary.** Risk: the lock and `reinforced` writes skip `FramePlacement.refreshView` and leave the entity summary stale. `compat/SuitSummary.java:22` carries only pieces. No issue.
    - **Package cycle.** Risk: a `station` ↔ `command` cycle. A grep of the imports shows none.
    - **"No message to B".** Risk: the assertion misses station messages. `StationActionGameTests.java:733`: `modMessages` counts `message.redstone_exoframes.*` (`FAILED`, `FRAME_GONE`), and the test adds the two command keys. No issue.
    - **Tooltip colours.** Risk: the `GRAY` tooltip lines are a new colour. `StationFramePanel.java:120,172,175`: already used there. No issue.
- ⚠️ Cannot verify from diff:
  - **Step 7 tree.** Both real-client runs were made on the uncommitted tree before `405d4aa`. That only comments and docs changed afterwards is the implementer's claim. The follow-up `261fd01` changes no production code.
  - **End-of-task checks.** The logs folder has no output for `check_sources`, `check_jars`, `check_data`, `check_art` or `arm_measure --selftest`; `check.tmp` holds only `OK passi`. The report claims all OK, and the controller can re-run them.
  - **Stale README lines (Task 5).** `README.md:38` still says `m4a_shots.sh` has "cinque controlli" (now eight, plus the lock, view and reinforced phases). `README.md:32` still says "oggi 235 test" (now 251). Ruling A7 keeps both out of this task, so Task 5 must update them.
  - **Tooltip colour reading.** For "la prima nel colore del testo", the implementer used the unstyled tooltip default (`StationScreen.java:374`). This is consistent with the first lines of `StationFramePanel`'s tooltips. The controller should confirm the plan did not mean `StationLayout.TEXT`.
  - **Tooltips never rendered.** No run ever showed the lock or shield tooltips, because the sequence has no hover step for them. They belong in the Task 5 checklist.
  - **Build-log noise.** `fix-build.log` and `step5-build.log` have javac "deprecated API" notes naming `StationBlock.java` and `ChargerBlock.java`, which are not in the diff, and a 15 Nov `final-build.log` already has the note. This is pre-existing and not from this task.

### Strengths
- **One lock rule.** Both entry points share one rule (`FramePermissions.java:61`). The station uses the player's permission (:45) and the command uses the source's permission (`ExoframeCommand.java:303`). The two Javadocs name each other, and `ExoframeGameTests` is untouched.
- **`operatorTogglesAnyLock` tests the order of `press`.** At tick 2 the operator in survival gets `REFUSED` only because `canUse` comes before `canToggleLock`. Swapping the order would give `DONE`.
- **Ruling A6 is applied minimally.** `mockServerPlayer` keeps always-creative (`GameTestsImpl.java:125`), and the operator mock follows the game mode with `max(level, super)` permission.
- **The follow-up commit fixed an assertion that could not fail.** `!lastSentView().unlocked()` alone always passed, because the opening view is already locked. Adding `viewsSent() == 2` (`StationActionGameTests.java:550`) makes it a real check.
- **`ExoframeCommand.Target` (:346)** removes the worn/in-sight lookup duplicated between `setUnlocked` and `setReinforced`. The behaviour is the same: worn first, then in sight; `mutate`+`syncTo` vs `updatePlaced`.
- **`Bench.removeStationAndFrames()` (:865)** can safely run twice. The new cleanup order removes item drops after the station halves are gone, where before it removed them before.
- **The `GameTestKit.java` comment** was kept accurate after Ruling A6, which made its old wording false.
- **Graphics follow the rule.** Only existing sprites are drawn, through `TEXTURE.blit`, inside the try/finally. There are no fills and no new colours.
- **The client does not predict the lock.** The client's `clickMenuButton` leaves the view alone for 26 (`StationMenu.java:290-293`), and the screen only sends the number (`StationScreen.java:304`). So the measured `lucchetto` check really proves the round trip through the server.
- **The real-client evidence holds up.** I cropped the title bar of the neoforge `montato`, `sbloccato`, `bloccato` and `rinforzato` shots: closed lock; open lock with the shackle raised; closed lock; bronze shield left of the closed lock. This matches the report and the 144 and 552 pixel measures.

### Issues
#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)
1. **`docs/compat.md:87`, the `GameTests` "Condizione" line.** It still puts the version directive "in `mockServerPlayer` e nel giocatore finto". The `//? if >=1.21` blocks now live in the private `mockPlayer` (`GameTestsImpl.java:137-155`), and `mockServerPlayer` only delegates (:125). This line is the map of where the Stonecutter conditions are. Fix: name `mockPlayer`, shared by `mockServerPlayer` and `operatorPlayer`.
2. **`compat/GameTests.java:71-74`, the `messagesTo` Javadoc.** It still says "arrivati al giocatore finto di `mockServerPlayer` … vuota per un altro giocatore". The operator mock is a `MockPlayer` too, and `messagesTo` returns its messages (`GameTestsImpl.java:169`); the `operatorPlayer` Javadoc already promises this. Fix: "di `mockServerPlayer` e di `operatorPlayer`".
3. **`frame/SuitDamage.java:36` and `frame/SuitTick.java:53`.** Both still justify avoiding `isCreative()` with "i giocatori finti dei test rispondono sempre vero". That is no longer true of every mock (Ruling A6). The reasoning still holds and both files are outside this task's list, so I leave the routing to the controller. Fix: "i giocatori finti di `mockServerPlayer`".
4. **Two untested paths.**
   - The lock's `FAILED` path (`StationActions.java:117-119`, registry refuses) has no in-world assertion, although `GameTests.refusePlacedUpdates` exists from Task 1.
   - No test checks the `commands.redstone_exoframes.debug.reinforced.done` reply; `debugReinforcedMarksWornAndPlacedFrames` checks only the return value and the record.

   The brief did not ask for either. Both could be assertions inside existing tests, so the counts stay 251 and 407.
5. **`client/StationScreen.java:304`.** The lock's click rule (26 only with `canToggleLock`) is inline in `act`, unlike `placeButton` and `unmountButton`, which are pure `StationLayout` functions with JUnit. So the client half of the lock rule has no test and no client run (the implementer's concern 6). Optional fix: a pure `StationLayout` helper plus a JUnit assertion in an existing test.
6. **`station/StationActions.java:3`.** The station handler now imports the Brigadier command class for two key constants (`ExoframeCommand.java:113-115`). There is no cycle, and the constants are compile-time, so there is no runtime coupling. Still, gameplay depending on the command entry point is an odd direction. Acceptable as is; a neutral holder for the two keys would be cleaner.
7. **`tools/client/m4a_shots.sh:145-146` and `tools/client/README.md:170`.** The "non prova" lists already credit "la raccolta del telaio e la sua posa di nuovo (test in mondo)" to tests that only Task 3 adds. At this head it points at tests that do not exist yet. It becomes true with Task 3.

### Assessment
**Task quality:** Approved
**Reasoning:** Every brief step and Rulings A5-A7 are implemented as written: one lock rule for menu and command, a `TOGGLE_LOCK` branch in the plan's order with one write and effects only after it succeeds, and in-world tests that exercise the real ordering. The counts (407/251) and both real-client runs check out against the artifacts; what remains is documentation drift and optional coverage.
