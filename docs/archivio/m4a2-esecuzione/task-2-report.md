# Task 2 report — il lucchetto, il rinforzato e `/exoframe debug reinforced` (parte 4a-2)

**Status:** DONE_WITH_CONCERNS. Every check in the brief passes. The concerns are notes, not defects (see "Concerns").
**Commits** (on `m4-stazione`, on top of BASE `8c1f1fe`; signed by the environment, with a `gpgsig` header like the
earlier commits; not pushed):
- `405d4aa` station, command, client: il lucchetto e lo scudo nella schermata, una regola sola del lucchetto con il comando, /exoframe debug reinforced; tools: m4a_shots.sh con la vista misurata
- `261fd01` gametest, compat: la prova del lucchetto del proprietario conta le viste mandate; il giocatore finto sempre in creativa e' quello di mockServerPlayer, non l'operatore. A follow-up from my final self-check; see "Follow-up" at the end.

## History: the stop before implementation, and the answers

Before writing anything I stopped and asked (NEEDS_CONTEXT): ruling A5 step 3 could not pass, because
`MockPlayer.isCreative()` always returns true and `FramePermissions.canUse` grants creative operators. The coordinator
answered with two binding rulings:
- **Q1: option (b).** The operator mock's `isCreative()` follows its game mode (true only in `GameType.CREATIVE`).
  `mockServerPlayer` and its always-creative behaviour are unchanged. The three steps of ruling A5 stand as written.
- **Q2: yes.** `debug reinforced` is documented in the `ExoframeCommand` Javadoc, mentioned beside the shield in
  `docs/compat.md` ("Menu e schermate"), and gets one clause in `README.md:34` ("Comandi"). Nothing else in
  `README.md` changes.

## What I implemented

- **Server, `station/StationActions.press`.** `TOGGLE_LOCK` comes in its place in "L'ordine di press": after the
  write turn, the record and `canUse`.
  - `handled(kind)`, which replaces `mountsOrUnmounts`, now includes `TOGGLE_LOCK`. Only the 4b numbers are still
    `IGNORED` without taking the turn.
  - The switch returns an `Outcome` directly. `outcome(FrameFitting.Result)` maps mount and unmount results by name.
  - New `toggleLock(player, frame)`:
    - without `FramePermissions.canToggleLock` it gives `REFUSED`, with no message;
    - otherwise one `updatePlaced(s -> s.setUnlocked(!unlocked))`, where `unlocked` is read from the record;
    - only if that write succeeds: `ExoframeCommand.UNLOCK_DONE`/`LOCK_DONE` on the action bar, then
      `FrameSounds.Sound.UNLOCK`/`LOCK` at `frame.position()`;
    - a refused write gives `FAILED`, with the station's `FAILED` message, as for mounting.
- **`frame/FramePermissions`.** New `canToggleLock(UUID actor, boolean operator, FrameState)`, which returns
  `operator || actor.equals(owner)`.
  - `canToggleLock(ServerPlayer, FrameState)` calls it with `player.hasPermissions(2)`.
  - The two Javadocs name each other: the new one names `ExoframeCommand.setUnlocked` and `source.hasPermission(2)`
    (with `execute as` from RCON the source's level counts); `setUnlocked` links the new method.
- **`frame/FrameSounds`.** `LOCK(IRON_DOOR_CLOSE, BLOCKS)` and `UNLOCK(IRON_DOOR_OPEN, BLOCKS)`. javap shows both are
  plain `SoundEvent` fields on 1.20.1 and 1.21.1. The sound table in the Javadoc has both rows.
- **`command/ExoframeCommand`.**
  - Public `UNLOCK_DONE`/`LOCK_DONE`, shared with the station.
  - `setUnlocked` now uses `FramePermissions.canToggleLock(player.getUUID(), source.hasPermission(2), state)`.
  - New `debug reinforced <valore>` branch, with `BoolArgumentType.bool()`:
    - acts on the worn frame (`mutate` + `syncTo`) or on the frame looked at (`updatePlaced`), with no owner check;
    - on success: `commands.redstone_exoframes.debug.reinforced.done` with `String.valueOf(value)`, result 1;
    - no frame: `no_frame`, result 0; a refused write: 0, silently.
  - `setUnlocked` and `setReinforced` share a private `record Target(FrameState state, Optional<UUID> placedId)`:
    `Target.of(player)` finds the frame (worn first, then in sight) and `change(...)` applies the edit. The old body of
    `setUnlocked` moved there unchanged.
  - The class Javadoc covers the shared rule and the new branch.
- **Test seam, `compat/GameTests.operatorPlayer(helper)` and `GameTestsImpl`.**
  - `mockServerPlayer` is now `mockPlayer(helper, 0, true)` and `operatorPlayer` is `mockPlayer(helper, 2, false)`.
  - `MockPlayer` takes a permission level and an `alwaysCreative` flag:
    - `getPermissionLevel()` returns the higher of that level and `super`'s (javap on the four jars: `protected int`);
    - `isCreative()` returns `alwaysCreative || super.isCreative()`.
  - The operators list is not touched.
  - Javadocs (interface, impl class, `MockPlayer`) state the difference from `mockServerPlayer` (ruling Q1).
- **`station/StationTexts`.** New `LOCKED`, `UNLOCKED`, `OWNER_ONLY`, `EVERYONE` and `REINFORCED`, all in `KEYS`, which
  the in-world text test checks in both language files.
- **Language files.** The five `gui.redstone_exoframes.station.*` keys go after `.holds_armor_how`, and
  `commands.redstone_exoframes.debug.reinforced.done` after `debug.place.scrap`. The words are the plan's. Both files
  keep the same keys in the same order (115 each).
- **`client/StationLayout`** (still pure Java):
  - `SHIELD` 276,3-290,15 and `LOCK` 292,3-308,15;
  - `lockSprite(boolean locked, ButtonState)` returns the existing `BLOCCO_CHIUSO_*`/`BLOCCO_APERTO_*` constants
    (no new `Sprite`);
  - `lockTooltipKeys(view)`: `LOCKED` or `UNLOCKED`, then `OWNER_ONLY` without `canToggleLock`, then `EVERYONE`
    without `ownerOnly`;
  - the class Javadoc is reflowed, which also fixes the short line that a deferred Task 1 note pointed out.
- **`client/StationScreen`.**
  - `Widget.LOCK`, found by `widgetAt`. It sits under the per-button press memory of `8c1f1fe`: a press on the lock is
    always taken by the screen.
  - The left click sends 26 only with `canToggleLock`; otherwise nothing is sent. The client does not predict the lock.
  - `drawShieldAndLock`: `RINFORZATO` only with `reinforced`, and the lock always, as
    `lockSprite(!unlocked, buttonState(canToggleLock, pressed(LOCK), hover))`, drawn with `TEXTURE.blit` inside the
    existing `try/finally`.
  - Tooltips: the lock's lines from `lockTooltipKeys`, the first unstyled and the others `ChatFormatting.GRAY`; the
    shield's `REINFORCED`, only when it is drawn.
  - Javadocs updated.
- **In-world tests, `gametest/StationActionGameTests`.** Five tests, registered after the Task 1 ones:
  - `ownerTogglesTheLockFromTheStation`
  - `othersCannotToggleTheLock`
  - `operatorTogglesAnyLock` (the three steps of ruling A5, with option (b))
  - `lockingFromTheStationClosesAnotherScreen`
  - `debugReinforcedMarksWornAndPlacedFrames`

  Changes to `Bench`:
  - `operator(feet)`: the same set-up as `player(feet)`, with `operatorPlayer`;
  - `removeStationAndFrames()`: closes menus with an empty cursor, removes frames, records and both station halves. It
    was taken out of `cleanUp`, which now calls it, and the reinforced test calls it mid-test. Behaviour is unchanged.

  I added no third record accessor: the tests use `recordState` from `GameTestKit` and the existing `record`.
- **`tools/client/m4a_shots.sh`.**
  - The phases "lucchetto" (`@clic 764 56`, `sbloccato`), "vista" (`tp … -40`, `/exoframe lock`, `bloccato`) and
    "rinforzato" (`/exoframe debug reinforced true`, `rinforzato`) replace the smoke test, before the iron ingots.
    Later phases are renumbered (15-18).
  - The checks `lucchetto`, `vista` and `rinforzato` are added, with `LOCK_RECT=32x24+748+44` and
    `SHIELD_RECT=28x24+716+44`.
  - Thresholds are measured (below).
  - Header comment: the new phases; the checks, with the measured view as the `station_view` proof that the 4a-1 was
    waiting for (design §13); what the sequence does not prove (the operator, the refused lock, the other screen
    closing, the worn reinforced frame, the pick-up and placing again, the lock and shield tooltips); the durations
    measured on neoforge and forge.
- **Documents.**
  - `docs/compat.md`:
    - `GameTests`: `operatorPlayer`, with its difference from `mockServerPlayer` in one sentence;
    - `Net`: `station_view` now measured;
    - `StationScreen`/`StationLayout`: `lockSprite`, `lockTooltipKeys`, the shield and the lock, the click and the
      checks;
    - "Menu e schermate": "I pulsanti" no longer lists the lock as unhandled; a new bullet "Il lucchetto e lo scudo"
      (number 26, its place in `press`, the single `canToggleLock` rule for the menu and the command, the messages,
      the sounds, another screen closing, drawing, click and tooltip, the shield, `/exoframe debug reinforced`, and
      the tests);
    - "La vista del telaio": the measured checks.
  - `tools/client/README.md`: the three phases, the lock and shield rectangles and the lock point, three new rows in
    the check table, the shots to look at, and what the sequence does not prove.
  - `README.md:34`: one clause for `/exoframe debug reinforced <true|false>`.

## Tests and results

| Variant | JUnit | In-world |
|---|---|---|
| 1.20.1-forge | 407 | 251 |
| 1.20.1-fabric | 407 | 251 |
| 1.21.1-fabric | 407 | 251 |
| 1.21.1-neoforge | 407 | 251 |

N₀ 389 + 18 = 407. The four test tasks were re-run on the final tree with `--rerun` after each task (fresh XML
timestamps), with 0 failures and 0 errors. In-world: 246 + 5 = 251. The command tests of `ExoframeGameTests`
(`ownerUnlocksAndStrangerCanUse`, `strangerCannotUnlock`, `lockAndUnlockTheWornFrame`) pass, and the file is unchanged
(empty diff).

Logs are in `<scratchpad>/m4a2-task2/logs/`.

### TDD evidence

- **Step 1 RED.** `./gradlew --console=plain :1.21.1-neoforge:test --tests '*StationLayoutTest'` gives `BUILD FAILED`
  in `compileTestJava`, with 27 errors. Missing symbols: `SHIELD`, `LOCK`, `lockSprite(boolean,ButtonState)`,
  `lockTooltipKeys(StationView)`, and `StationTexts.LOCKED`, `UNLOCKED`, `OWNER_ONLY`, `EVERYONE`. Expected: the tests
  name the API before it exists.
- **Step 2 GREEN.** The same command gives `BUILD SUCCESSFUL`: `StationLayoutTest` has 14 tests (12 + 2), with
  `lockAndShieldTakeTheirSprites` and `lockTooltipFollowsTheView`, and 0 failures.
- **Step 3 RED.** `./gradlew --console=plain :1.21.1-neoforge:compileJava` gives `BUILD FAILED` with 12 errors:
  - `operatorPlayer(GameTestHelper)` (interface `GameTests`);
  - `Sound.LOCK` and `Sound.UNLOCK`;
  - `ExoframeCommand.UNLOCK_DONE` and `LOCK_DONE`.

  Expected: the five tests come before the server code. The command constants are mine; the brief only names
  `operatorPlayer` and `Sound.LOCK`.
- **Step 4 GREEN.** The four-variant build gives `BUILD SUCCESSFUL` with 407 JUnit each. `bash tools/run_gametests.sh`
  gives `OK 1.20.1-forge|1.20.1-fabric|1.21.1-fabric|1.21.1-neoforge: 251 test obbligatori passati, nessun avviso
  fuori elenco`.
- **Step 5.** The four-variant build gives `BUILD SUCCESSFUL`; `bash tools/check_art.sh` gives `OK art`.
- **Step 6.** `bash -n tools/client/m4a_shots.sh` gives no output (exit 0).
- **Step 8.** `grep -c 'operatorPlayer' docs/compat.md` gives 2.

## Real client (Step 7)

**1.21.1-neoforge.** `EXO_ACCEPT_EULA=true bash tools/client/m4a_shots.sh 1.21.1-neoforge …/scratchpad/m4a2-task2/neo`
took 140 s, of which `client_shot.sh` took 131 s. First run, with the provisional thresholds:

```
ok anteprima: ritaglio magenta=18582 riquadro=167x198+604+96; schermata magenta=18582 riquadro=167x198+604+96
ok rotazione: sagoma pixel=24245 copertura=4.68 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96
ok scelta: schermata pixel=972 riquadro=232x88+222+78; ritaglio pixel=0 riquadro=0x0+0+0
ok smontaggio: posto dell'elmo pixel=952 riquadro=40x40+224+80; ritaglio da magenta=9225 riquadro=111x198+629+96 a magenta=8728 riquadro=111x192+629+102 (cala di 497); schermata magenta=8728 riquadro=111x192+629+102
ok montaggio: ritaglio pixel=0 riquadro=0x0+0+0; posto dell'elmo pixel=0 riquadro=0x0+0+0; schermata pixel=648 riquadro=28x28+218+454
ok lucchetto: schermata pixel=144 riquadro=28x20+750+46
ok vista: schermata pixel=144 riquadro=28x20+750+46; lucchetto contro montato pixel=0 riquadro=0x0+0+0
ok rinforzato: schermata pixel=552 riquadro=28x24+716+44
OK 1.21.1-neoforge: scatti in …/m4a2-task2/neo
```

The shots I looked at:
- `sbloccato.png`: the lock is open, with the shackle raised and its right leg out of the body; the rest is as in
  `montato`.
- `bloccato.png`: the lock is closed, as in `montato`.
- `rinforzato.png`: the bronze shield is left of the lock; the rest is as in `bloccato`.

Everything outside the screen rectangle differs between `sbloccato` and `bloccato` because the camera pitch changes,
and the checks never measure there. The stop rule did not trigger: the lock changed after the click and after
`/exoframe lock`, and every rectangle is inside the expected one.

**Thresholds set** at half the neoforge measure, rounded down to the ten, with the measure beside them in the script:
- `LOCK_MIN=70`: measured 144 in `28x20+750+46`, the shackle and body of the lock (the same between `bloccato` and
  `sbloccato`);
- `SHIELD_MIN=270`: measured 552 in `28x24+716+44`, the whole shield.

`M4A_SOLO_CONTROLLI=1 bash tools/client/m4a_shots.sh 1.21.1-neoforge …/neo` gives the same eight `ok` lines and
`OK 1.21.1-neoforge`.

**1.20.1-forge (closes the open point from Task 1).** This run was made from this task's tree on top of `8c1f1fe`, so
`git merge-base --is-ancestor 8c1f1fe HEAD` holds and the per-button press memory is in the run. The tree is the one
later committed as `405d4aa`. After the run only comments and documents changed: the `OPERATOR_LEVEL` Javadoc wording,
the class Javadoc reflow of `StationActionGameTests`, the durations in the script's header comment, and the docs. The
JUnit and in-world runs of Step 9 are on the committed tree.
`EXO_ACCEPT_EULA=true bash tools/client/m4a_shots.sh 1.20.1-forge …/scratchpad/m4a2-task2/forge` took 144 s, of which
`client_shot.sh` took 135 s:

```
ok anteprima: ritaglio magenta=18582 riquadro=167x198+604+96; schermata magenta=18582 riquadro=167x198+604+96
ok rotazione: sagoma pixel=24503 copertura=4.73 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96
ok scelta: schermata pixel=972 riquadro=232x88+222+78; ritaglio pixel=0 riquadro=0x0+0+0
ok smontaggio: posto dell'elmo pixel=952 riquadro=40x40+224+80; ritaglio da magenta=9225 riquadro=111x198+629+96 a magenta=8728 riquadro=111x192+629+102 (cala di 497); schermata magenta=8728 riquadro=111x192+629+102
ok montaggio: ritaglio pixel=0 riquadro=0x0+0+0; posto dell'elmo pixel=0 riquadro=0x0+0+0; schermata pixel=648 riquadro=28x28+218+454
ok lucchetto: schermata pixel=144 riquadro=28x20+750+46
ok vista: schermata pixel=144 riquadro=28x20+750+46; lucchetto contro montato pixel=0 riquadro=0x0+0+0
ok rinforzato: schermata pixel=552 riquadro=28x24+716+44
OK 1.20.1-forge: scatti in …/m4a2-task2/forge
```

In the montaggio phase on forge, `rcon.log` shows the release outside the screen dropping nothing:
- `execute unless data entity Exo Inventory[{Slot:1b}]`: `Test passed` (the helmet is on the cursor);
- then `@trascina 244 100 100 250`;
- `execute unless entity @e[type=minecraft:item]`: `Test passed` (nothing on the ground after the release outside the
  screen);
- `execute if data … Slot:1b … scrap_helmet`: `Test passed` (the helmet is back in slot 1).

`/exoframe lock` answered "Exoframe locked: only its owner can use it" and `debug reinforced true` answered
"Reinforced: true", on both variants. `log-check.txt` is empty (0 bytes) for both runs. The `kill` of the items on the
ground comes 1 s after `/exoframe remove` on both.

## End-of-task outputs (Step 9, on `405d4aa`'s tree; repeated on `261fd01`'s tree, see "Follow-up")

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` → `BUILD SUCCESSFUL`
- JUnit: 407 on each of the four variants (re-run with `--rerun`).
- `bash tools/check_sources.sh` → `OK sources`; `bash tools/check_jars.sh` → `OK jars`; `bash tools/check_data.sh` → `OK data`; `bash tools/check_art.sh` → `OK art`; `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure`; `bash tools/client/client_shot.sh --selftest` → `OK passi`
- `bash tools/run_gametests.sh` → `OK 1.20.1-forge: 251 test obbligatori passati, nessun avviso fuori elenco`, and the same for `1.20.1-fabric`, `1.21.1-fabric` and `1.21.1-neoforge`.
- `git status --short` after the commit: clean. No shots are in the repository.

## Files changed

`README.md`, `docs/compat.md`, `tools/client/README.md`, `tools/client/m4a_shots.sh`, the two language files, and:
- `client/StationLayout.java`, `client/StationScreen.java`
- `command/ExoframeCommand.java`
- `compat/GameTests.java`, `compat/impl/GameTestsImpl.java`
- `frame/FramePermissions.java`, `frame/FrameSounds.java`
- `gametest/StationActionGameTests.java`
- `station/StationActions.java`, `station/StationTexts.java`
- `test/client/StationLayoutTest.java`

That is 17 files, +758/−168, in `405d4aa`. `261fd01` adds `compat/GameTests.java` (again),
`gametest/GameTestKit.java` and `gametest/StationActionGameTests.java` (again): 3 files, +15/−13. Paths are relative
to the mod's package root as in the plan.

## Self-review

- **Completeness.** Every step 1-10 of the brief, ruling A5 with Q1 (b), Q2, and the forge open point (above).
- **Order of `press`.** `TOGGLE_LOCK` takes the write turn, then the record and `canUse` (step 5) are read before the
  lock's own check. `operatorTogglesAnyLock` step 3 pins this: O in survival on A's locked frame gives `REFUSED` and
  the record stays locked.
- **One write per action.** `toggleLock` makes exactly one `updatePlaced`. The message and sound come only after it
  succeeds; nothing else writes.
- **The client evaluates no rule.** The screen decides from the view only (`canToggleLock`, `unlocked`, `reinforced`,
  `ownerOnly`), and the lock changes on screen only when the server's view arrives (the measured `lucchetto` check).
- **Graphics.** Only the existing sprite constants are drawn, with `StationScreen.TEXTURE.blit`. There is no new fill
  and no new colour (the grey tooltip lines are `ChatFormatting.GRAY`, as in the other station tooltips).
  `check_art` gives `OK art`.
- **Hygiene.** No plan, decision or review IDs and no vendor names in the added lines; no accented letters in Java;
  ASCII apostrophes; identifiers, exceptions and logs in English. Every added Java or shell line is at most 120
  characters (Markdown paragraphs are one line by convention). The Javadoc blocks I touched are reflowed, with no
  `{@...}` tag split across lines.
- **Fixed during self-review.** `{@link FramePermissions}` was used only in a Javadoc in the test class; I replaced it
  with `{@code}` and dropped the import. I reworded the `OPERATOR_LEVEL` Javadoc ("chiunque" read as "anyone") and
  reflowed four Javadoc paragraphs that had long or short lines.

## Concerns

1. **`README.md:38` is out of date for Task 5.** It still says `m4a_shots.sh` has "cinque controlli"; there are now
   eight. Ruling Q2 lets me touch only the clause at line 34, so I left it. Task 5 rewrites README.
2. **Tooltip colour.** For the lock's "first line in the text colour" I used the unstyled tooltip default, as the
   first lines of the station's place and cell tooltips do, not `StationLayout.TEXT`. The other lines are
   `ChatFormatting.GRAY`. Neither the lock tooltip nor the shield tooltip was hovered in the real client: the sequence
   has no hover step for them, and its header and README list them for the checklist.
3. **Choices beyond the brief's text.**
   - The command keys are shared as public constants in `ExoframeCommand` (`UNLOCK_DONE`, `LOCK_DONE`), so `station/`
     now imports `command/`. There is no source rule against this.
   - `setUnlocked` and `setReinforced` share a private `Target` record instead of duplicating the worn/in-sight lookup.
   - `Bench.cleanUp` was split, with `removeStationAndFrames()`.
   - Two assertions go beyond the brief: `operatorTogglesAnyLock` checks that the sounds are `[UNLOCK, LOCK]` at the
     end (the refused press is silent), and `lockingFromTheStationClosesAnotherScreen` checks right after A's press
     that the record is locked. "No message to B" counts the two command keys plus the existing
     `message.redstone_exoframes.*` counter, because `modMessages` alone would not see `commands.*` keys.
4. **Durations.** The header comment now has the durations and measures for neoforge and forge only; Task 4 measures
   the four variants.
5. **A leftover stone.** `debugReinforcedMarksWornAndPlacedFrames` leaves the stone that `wearScrap` places under
   `STAND`, as the other tests using `wearFrameAt` do. The worn record goes in a `finally` (`leaveTestFrame`), and
   `wearScrap` sits inside that `try`.
6. **Lock paths without a test.**
   - A press on the lock without `canToggleLock` (taken by the screen, no packet, the `OFF` sprite) has neither a test
     nor a client run. Only the server's `REFUSED` is tested, by `othersCannotToggleTheLock` and
     `operatorTogglesAnyLock`. In the sequence the player owns the frame, so the lock is always enabled.
   - A press on the lock with a stack on the cursor, released outside the screen, was checked once in a client run
     that is not committed (see "Follow-up"). It is not in `m4a_shots.sh`.
7. **Comments in `frame/`.** `frame/SuitDamage.java:36` and `frame/SuitTick.java:53` still say that "i giocatori finti
   dei test rispondono sempre vero" to `isCreative()`. Their reason for never calling `isCreative()` still holds, since
   `mockServerPlayer` is unchanged, but the operator mock is now an exception. Both files are outside this task's
   list, so I left them as they are.
8. **One file outside the list.** `gametest/GameTestKit.java` changed in a comment only (see "Follow-up").

## Follow-up (`261fd01`, after my final self-check)

**What changed.** Comments and one assertion only; no production code.
- `compat/GameTests.java`: the `survivalPlayer` Javadoc said `isCreative()` "resta vero come per ogni giocatore
  finto". That is no longer true for the operator mock (ruling Q1), so it now says "come per ogni giocatore finto di
  `mockServerPlayer`".
- `gametest/GameTestKit.java`: the guide for test writers said the mock "dice sempre di essere in creativa". It now
  adds that the operator one of `operatorPlayer` follows the game mode. Comment only; the file is outside the Task 2
  list.
- `gametest/StationActionGameTests.ownerTogglesTheLockFromTheStation`: the last check alone,
  `!lastSentView().unlocked()`, could never fail, because the opening view is already locked. It now also requires
  `menu.viewsSent() == 2`: one view for each change, unlocked then locked. That count holds whichever order the server
  ticks the player and runs the test steps in. `StationMenuGameTests` already asserts exact `viewsSent()` counts.

**Verification on `261fd01`'s tree.**
- The four-variant build with `collectJar` gives `BUILD SUCCESSFUL`.
- JUnit: the four test tasks ran again (fresh timestamps): 407 on each variant, with 0 failures and 0 errors.
- `bash tools/run_gametests.sh` gives `OK <variante>: 251 test obbligatori passati, nessun avviso fuori elenco` on all
  four variants.
- `OK sources`, `OK jars`, `OK data`, `OK art`, `OK arm_measure`, `OK passi`.
- `git status --short` is clean.

**One-off client run** (not committed, 1.21.1-neoforge). Review Focus 1 names the lock among the widgets whose release
must not reach the base game. Shots are in
`<scratchpad>/m4a2-task2/lock-cursor/`. The run, with
`client_shot.sh`:
1. the opening steps of the sequence;
2. `item replace entity Exo hotbar.1 with minecraft:stick`, then `@clic 232 470`, which puts the stick on the cursor
   (`execute unless data entity Exo Inventory[{Slot:1b}]`: `Test passed`);
3. `@trascina 764 56 100 250`: pressed on the lock, released outside the screen;
4. `execute unless entity @e[type=minecraft:item]`: `Test passed`. Shot `pila-sul-cursore`: the stick is still on the
   cursor at (100, 250);
5. `@clic 232 470` puts the stick back (`execute if data … Slot:1b … minecraft:stick`: `Test passed, count: 1`).

The run exited 0 and `log-check.txt` is empty. The recipe toast the stick triggers covers the lock in both shots, so
this run shows only the release behaviour, not the lock's state after the press.
