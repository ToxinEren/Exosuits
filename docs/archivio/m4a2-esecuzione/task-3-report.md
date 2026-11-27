# Task 3 report: la raccolta e la posa del telaio con i suoi dati (parte 4a-2)

**Status:** DONE. Every step of the brief is done and every end-of-task check passes.

**Commits** on `m4-stazione`, on top of BASE `261fd01`. Both are signed by the environment (each has a `gpgsig` header
like the earlier commits), carry the two trailers, and are not pushed:
- `d26df94` content, frame: l'oggetto telaio raccolto porta sblocco, rinforzo e, se raccolto da un altro, il proprietario; la posa li rilegge e rifiuta un oggetto con pezzi o cella
- `d0a5342` content, gametest: due righe di Javadoc dell'oggetto telaio raccolto e della prova dei testi riportate entro le 120 colonne

  The second commit came from my final check. It reflows two of my Javadoc lines that were 121 characters, in
  `ExoframeItem.pickedUp` and `ChargerGameTests.everyItemAndBlockHasANameInBothLanguages`, with no code change. I
  re-ran the end-of-task commands at this HEAD (see "At HEAD `d0a5342`" below).

## What I implemented

### `content/ExoframeItem`
- **`FULL`**: `public static final String FULL = "message.redstone_exoframes.frame_item_full"`.
- **`TEXT_KEYS`**: a public list of exactly the plan's seven keys. These are the four grey tooltip keys of today, the
  private `TOOLTIP_UNLOCKED` and `TOOLTIP_REINFORCED`, and `FULL`. `NO_ROOM` is not in it, as the plan lists.
- **`pickedUp(FrameState state, UUID picker)`**:
  - the owner goes in the item only if the frame has one and it is not `picker`;
  - with no owner to carry and both flags false, it returns `new ItemStack(EXOFRAME)` with no `setFrame` call at all
    (`hasData` is false);
  - otherwise it calls `setFrame(stack, new FrameItemData(owner, unlocked, reinforced, Optional.empty(), Map.of()))`.

  The Javadoc states both rules in words:
  - a frame picked up by another player stays its owner's;
  - a frame picked up by its owner is free, and whoever places it becomes the owner;
  - with nothing to carry, the item stays plain.

  It cites design M4 §5.11 and has no decision or question IDs.
- **`useOn`**, in the brief's order:
  1. On the client it returns `SUCCESS`.
  2. It reads the item data. If the data has a piece or a cell, it shows `FULL` on the action bar (only when there is
     a player) and returns `FAIL`, and the stack is untouched. This comes before any room check.
  3. The new state takes the owner from the data if present, otherwise from the player if there is one (nobody without
     a player). `unlocked` and `reinforced` come from the data, and are false without data.
  4. Then today's placement runs unchanged, including `NO_ROOM` and consumption on `!instabuild`.
- **`tooltip`**: "Sbloccato" and "Rinforzato" in `ChatFormatting.GOLD` when true, before the four grey lines. The
  owner is never shown.
- **Javadocs**: the class, `useOn` and `tooltip` Javadocs are updated. The old "chi lo usa come proprietario" and
  "Quattro righe grigie" were no longer the whole truth.

### `frame/FrameInteractions.unmount`
The pick-up branch builds `ItemStack item = ExoframeItem.pickedUp(state, player.getUUID())` from `record.state()`
before `removePlaced`. It calls `Handouts.give(player, item)` only after the removal succeeded. The unused
`ModItems` import is gone. The `unmount` Javadoc says what the item carries.

### `gametest/GameTestKit`
- A new `static ExoframeEntity useExoframeItem(GameTestHelper, ServerPlayer, ItemStack stack, BlockPos relative,
  boolean expectPlaced)` puts `stack` in the main hand and runs today's body.
- The old 4-argument method delegates to it with `new ItemStack(ModItems.EXOFRAME.get())`.
- `FrameItemGameTests` joins the class list in the kit's Javadoc.
- I added no third record accessor. The new tests use the existing `recordState`, which returns everything they read
  (owner, flags, pieces, cell), so `StationActionGameTests` and `StationMenuGameTests` are untouched.

### `gametest/FrameItemGameTests` (new)
The class has a public no-arg constructor, a `@GameTestGenerator` with five `add(tests,` lines, and is registered
after `StationActionGameTests` in `RedstoneExoframes.init()`.
- **Set-up:**
  - A and B are `mockServerPlayer` mocks, set to `GameType.SURVIVAL` and standing in the structure.
  - Each frame is placed with `placeFrame` on the stone at (3,1,3), feet (3.5,2,3.5).
  - "Picking up" means sneaking with an empty main hand and calling `click(frame, p, 0.0, 1.0, 0.0)`.
  - "Placing" means the new `useExoframeItem` on the same stone, with the stack taken out of the picker's inventory
    (`split(1)`), so no stack instance is shared between two inventories.
- **Cleanup:** each test runs in `try/finally`. Even on failure it removes every placed record in the structure box
  and its entities, removes items on the ground, and clears the players' inventories.
- **`ownerPickUpFreesTheFrameAndKeepsTheFlags`**:
  - A's frame is unlocked and reinforced.
  - A's pick-up gives `SUCCESS`, and the record and entity are gone.
  - The item's `frame(stack)` equals `FrameItemData(empty, true, true, empty, {})`.
  - B places it: the record is B's, unlocked and reinforced, with no pieces or cell, and B's hand is empty.
- **`borrowedFrameKeepsItsOwnerWhenPlacedAgain`**:
  - The test first asserts `ownerOnlyFrames` (the default), then places A's frame unlocked.
  - B's pick-up gives data `FrameItemData(A, true, false, empty, {})`.
  - B places it: the record is A's, unlocked and not reinforced, and `FramePermissions.canUse(B, state)` is true.
  - A locks it with `updatePlaced`.
  - B's next pick-up gives `CONSUME` and one `FrameInteractions.LOCKED` message. The frame stays placed, and B gets no
    frame item.
- **`plainPickUpGivesAPlainItem`**:
  - A's frame is locked and not reinforced.
  - A's item has `hasData` false and also `ItemStack.matches` a crafted one.
  - B places it, and the frame is B's with both flags false.
- **`frameItemWithPiecesOrACellIsNotPlaced`**:
  - First case: the plan's data with one Scrap helmet (180/180). A uses it with `expectPlaced` false. A gets one
    `ExoframeItem.FULL` message, the hand holds a stack that `ItemStack.matches` a copy taken before, and `placedIds`
    is unchanged (compared within one synchronous body).
  - Second case: the same with `Optional.of(500)` and no piece. The `FULL` count reaches 2.
  - Finally the test asserts `messages(a, NO_ROOM) == 0`: there was room, so the refusal is `FULL` and not the room
    check.
- **`frameTooltipShowsTheFlagsNeverTheOwner`**:
  - The stacks come from `ExoframeItem.pickedUp(A's state, another UUID)`. Each asserts first that A is the owner in
    the data, so the "never the owner" check is not vacuous.
  - Unlocked and reinforced give the keys exactly `[unlocked, reinforced, place, mount, enter, unmount]`. Only
    reinforced gives `[reinforced, place, mount, enter, unmount]`. With no data, only the four.
  - The flag lines are GOLD and the four lines are GRAY.
  - No line's `getString()` contains A's name (`getScoreboardName()`) or `A.getUUID().toString()`. Every mock is named
    `test-mock-player`, so the UUID check is the one that discriminates.

### The rest
- **`gametest/ChargerGameTests.everyItemAndBlockHasANameInBothLanguages`**: it adds `wanted.addAll(ExoframeItem.TEXT_KEYS)`,
  and its Javadoc names the frame item's keys.
- **The two language files**:
  - `message.redstone_exoframes.frame_item_full` goes after `message.redstone_exoframes.no_room`;
  - `tooltip.redstone_exoframes.exoframe.unlocked` and `.reinforced` go after `.exoframe.unmount`;
  - the words are the plan's, the order is the same in both files, and each file has 118 keys.
- **`docs/compat.md`, "Schema NBT"**: a new paragraph, "L'oggetto telaio raccolto (`ExoframeItem.pickedUp`, parte
  4a-2)". It says:
  - the pick-up writes `owner`, `unlocked` and `reinforced` from the state read before the removal, with the owner only
    when someone other than the owner picks it up;
  - `setFrame` also writes an empty `pieces`, and with nothing to carry nothing is written;
  - placing reads the data back;
  - an item with a readable piece in `pieces`, or with `cell`, is not placed: `frame_item_full`, before the room
    check, and the item stays in the hand;
  - the tooltip never shows the owner;
  - a milestone-3 jar ignores the data (design M4 §5.14);
  - the paragraph names `FrameItemGameTests`.
- **No change needed in `tools/client/m4a_shots.sh` and `tools/client/README.md`**: their "non prova" lines say "la
  raccolta del telaio e la sua posa di nuovo (test in mondo)" and name no class or test.

## Tests and results

### RED (Step 1), a compile failure as the brief expects
- **Command:** `./gradlew --console=plain :1.21.1-neoforge:compileJava`, with only `FrameItemGameTests` written. The
  log is at `<scratchpad>/m4a2-task3/red-compile.log`.
- **Output:** `BUILD FAILED`, exit 1, with 8 errors:
  ```
  gametest/FrameItemGameTests.java:128: error: method useExoframeItem in class GameTestKit cannot be applied to given types;
  gametest/FrameItemGameTests.java:165: ... useExoframeItem ...   (also :204, :312)
  gametest/FrameItemGameTests.java:251: error: cannot find symbol   symbol: method pickedUp(FrameState,UUID)   (also :258)
  gametest/FrameItemGameTests.java:313: error: cannot find symbol   symbol: variable FULL   (also :314)
  ```
- **Why expected:** these are exactly the three missing pieces the brief names: `pickedUp`, `FULL` and the new
  `useExoframeItem`.

### Behavioural RED (extra): the tests fail on the old behaviour
The new API was present, but `ExoframeItem` was stubbed to behave as before this task. This was a temporary edit,
restored byte for byte (`cmp` identical) and never committed. Both runs used `bash tools/run_gametests.sh 1.21.1-neoforge`.
- **Stub 1:** `pickedUp` returns a plain item, `useOn` ignores the data, `tooltip` ignores the data.
  - Result: `4 required tests failed :(`, which is the expected four.
  - `owner_pick_up_frees_the_frame_and_keeps_the_flags`: "the item picked up by its owner carries Optional.empty".
  - `borrowed_frame_keeps_its_owner_when_placed_again`: "the item picked up by B carries Optional.empty, A is …".
  - `frame_item_with_pieces_or_a_cell_is_not_placed`: "refused use reported CONSUME".
  - `frame_tooltip_shows_the_flags_never_the_owner`: its owner-in-data precondition failed.
  - `plain_pick_up_gives_a_plain_item` passes, as it should: a plain item is today's rule.
- **Stub 2:** the "Unlocked" line gets the owner's UUID appended as a sibling, with keys and colours unchanged.
  - Result: `1 required tests failed :(`. `frame_tooltip_shows_the_flags_never_the_owner`: "unlocked and reinforced:
    the tooltip line \"Unlocked 5fb0e19a-…\" names the owner".

### GREEN while iterating
- `./gradlew --console=plain :1.21.1-neoforge:compileJava` gave `BUILD SUCCESSFUL`.
- `bash tools/run_gametests.sh 1.21.1-neoforge` gave `OK 1.21.1-neoforge: 256 test obbligatori passati, nessun avviso
  fuori elenco` (22 s).
- `bash tools/run_gametests.sh 1.20.1-forge` gave `OK 1.20.1-forge: 256 test obbligatori passati, nessun avviso fuori
  elenco`. This variant stores item data in the stack tag and starts in a different default game mode.
- The server log has the three new `Frame … picked up by test-mock-player` INFO lines, from the three pick-ups that
  succeed. There is no new WARN or ERROR.

### End-of-task commands (Step 4), all on the final tree
These ran with no edit between them and the commit. Logs are under
`<scratchpad>/m4a2-task3/`.
- **Build:** `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build
  :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar
  :1.21.1-neoforge:collectJar` gave `BUILD SUCCESSFUL in 30s`. All four `:test` tasks ran: fresh XML at 21:02-21:03.
- **JUnit count**, with the plan's command (N₀ + 18 = 407, unchanged):
  ```
  1.20.1-forge 407
  1.20.1-fabric 407
  1.21.1-fabric 407
  1.21.1-neoforge 407
  ```
- **Checks:**
  - `bash tools/check_sources.sh`: `OK sources`
  - `bash tools/check_jars.sh`: `OK jars`
  - `bash tools/check_data.sh`: `OK data`
  - `bash tools/check_art.sh`: `OK art`
  - `bash tools/client/arm_measure.sh --selftest`: `OK arm_measure`
  - `bash tools/client/client_shot.sh --selftest`: `OK passi`
- **In-world tests:** `bash tools/run_gametests.sh` exited 0 in 114 s. 256 is 251 + 5, and the `add(tests,` lines in
  `gametest/*.java` also count 256.
  ```
  OK 1.20.1-forge: 256 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 256 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 256 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 256 test obbligatori passati, nessun avviso fuori elenco
  ```
- **The two existing tests:** `exoframeItemPlacesAnEmptyFrame` and `unmountByHeightThenCellThenPickUp` pass with no
  change. `ExoframeGameTests.java` is not in the diff.
- **Step 3 check:** `grep -c 'frame_item_full\|pickedUp' docs/compat.md` gives `1` (one paragraph line).
- **Not run:** this task has no real-client step.

### At HEAD `d0a5342` (the final tree, after the Javadoc reflow)
The same commands, with no edit since. Logs are `head-build.log`, `head-checks.log` and `head-gametests.log` in the
same scratchpad folder.
- **Build:** `BUILD SUCCESSFUL in 9s`. The four `compileJava` tasks ran. The four `:test` tasks were UP-TO-DATE,
  because a Javadoc-only change leaves the class files unchanged, so the JUnit results above still apply.
- **JUnit:** 407 on each of the four variants.
- **Checks:** `OK sources`, `OK jars`, `OK data`, `OK art`, `OK arm_measure`, `OK passi`, each with exit 0.
- **In-world tests:** `bash tools/run_gametests.sh` exited 0 in 113 s:
  ```
  OK 1.20.1-forge: 256 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 256 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 256 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 256 test obbligatori passati, nessun avviso fuori elenco
  ```
- **Tree:** clean after the run.

## Files changed (`d26df94`: 9 files, +494 −22; `d0a5342`: `ExoframeItem.java` and `ChargerGameTests.java`, Javadoc only)
- `src/main/java/com/toxineren/exoframes/gametest/FrameItemGameTests.java` (new)
- `src/main/java/com/toxineren/exoframes/content/ExoframeItem.java`
- `src/main/java/com/toxineren/exoframes/frame/FrameInteractions.java`
- `src/main/java/com/toxineren/exoframes/gametest/GameTestKit.java`
- `src/main/java/com/toxineren/exoframes/gametest/ChargerGameTests.java`
- `src/main/java/com/toxineren/exoframes/RedstoneExoframes.java`
- `src/main/resources/assets/redstone_exoframes/lang/en_us.json`
- `src/main/resources/assets/redstone_exoframes/lang/it_it.json`
- `docs/compat.md`

## Self-review findings
- **Fixed while writing:**
  - Two of my Javadoc lines in the test class, one in `GameTestKit` and one in `ExoframeItem` went over 120
    characters; I reflowed them.
  - After the first commit, a check over all six changed Java files against BASE found two more of my lines at 121
    characters. `d0a5342` reflows them. The remaining long lines in `RedstoneExoframes.java` were there at BASE.
  - I looked for lists of test classes that should name `FrameItemGameTests`. Besides the `GameTestKit` Javadoc
    (done), `docs/compat.md` names `StationActionGameTests` only for the two test seams it uses, which the new class
    does not use, so nothing was added there. `tools/client/m4a_shots.sh` and `tools/client/README.md` name no class
    for the pick-up.
  - A slip in my `docs/compat.md` paragraph ("`unlocked` e `reinforced` sempre") contradicted the no-data case; it now
    says the flags are written as the frame has them, and that with nothing to carry the pick-up writes nothing.
- **Rules:**
  - Javadoc and comments are in Italian with ASCII apostrophes only. A script found no non-ASCII character other than
    `§` in the changed Java files.
  - There are no plan, decision, review or ruling IDs, no AI or vendor names, and no `isCreative()` in new code. The
    test class Javadoc mentions it only to describe the mock, as `GameTestKit` does.
  - `git diff --check` is clean, and no art or texture was touched (`OK art`).
- **Brief fidelity:**
  - The keys, words, test names, signature, positions and commit subject are as given.
  - The pick-up reads the state before `removePlaced` and gives the item only after it succeeds.
  - The `FULL` refusal comes before the room check, as design M4 §5.11 and the plan decide.
- **Beyond the brief (cheap assertions, no new tests):**
  - colour checks on the tooltip lines;
  - `ItemStack.matches` against a crafted item in `plainPickUpGivesAPlainItem`;
  - `NO_ROOM == 0` in the refusal test;
  - the `SUCCESS`/`CONSUME` results of the pick-ups;
  - the owner-in-data precondition in the tooltip test;
  - the `ownerOnlyFrames` default assertion in the borrowed-frame test.

## Issues or concerns
None blocking. Three observations:
1. **`NO_ROOM` is not in `TEXT_KEYS`.** The plan lists exactly seven keys, so `everyItemAndBlockHasANameInBothLanguages`
   still does not check that the older `NO_ROOM` key is in the language files. It is there in both files today.
2. **A placement path is untested.** `useOn` without a player (owner from the data, or nobody) is implemented as the
   plan decides but has no test; the brief's five tests do not cover it.
3. **The full four-variant run came after the stub experiments.** It ran on the restored file; neoforge's
   `compileJava` came `FROM-CACHE` with the outputs of the real source. The run at HEAD `d0a5342` compiled all four
   variants again.
