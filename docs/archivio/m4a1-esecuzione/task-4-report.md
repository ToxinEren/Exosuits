# Task 4 report: la vista del telaio (`StationView`, il codec, i dati extra e `station_view`)

**Status:** DONE_WITH_CONCERNS. Concern 0 needs a ruling: edits outside the file list. The others are observations, and no defect is open.
**Commit:** `1dd63b6` station: la vista del telaio nei dati extra e nel pacchetto station_view, mandata quando cambia (SSH-signed, `gpgsig` present; the two trailer lines; branch `m4-stazione`; not pushed; tree clean; 15 files)
**Evidence folder:** `<scratchpad>/m4a1/t4/` (below: `t4/`).

## What I implemented

- **`core/station/StationView`** (pure Java record, design §5.5):
  - Components: `pieces`, `cell`, `unlocked`, `reinforced`, `canToggleLock`, `ownerOnly`, `selection`, `hasStowed`, `free`.
  - Nested records: `PieceInfo(tier, durability, maxDurability, primary, secondary)` and `CellInfo(stored, capacity)`.
  - Constants: `SELECT_CELL = 6`, `SELECT_ALL = 7`.
  - Static methods:
    - `of(state, canToggleLock, ownerOnly, selection, hasStowed, free)`;
    - `selectionOf(slot)`, the ordinal;
    - `initialSelection(state)`: the first mounted piece in `PieceSlot` order, else 0.
  - The constructor copies the map with `new EnumMap<>(PieceSlot.class)` plus a per-entry put. `new EnumMap<>(map)` would throw on the empty unmodifiable map of a bare frame. The copy is wrapped in `Collections.unmodifiableMap`.
  - The constructor rejects:
    - with `NullPointerException`: a null map, a null key or value, or a null cell;
    - with `IllegalArgumentException`: a selection outside [0, 7].
  - `PieceInfo` rejects a null tier (`NullPointerException`); `maxDurability <= 0`, a durability outside [0, max], and a colour that is neither `UNPAINTED` nor in [0, 0xFFFFFF] (`IllegalArgumentException`).
  - `CellInfo` rejects `stored < 0` and `capacity <= 0`. It does not check `stored <= capacity`: the plan lists only those two invariants, and `EnergyCell` already guarantees it.
- **`station/StationViewCodec`** (`PacketCodec<StationView>`, `INSTANCE`), in exactly the format of "Tipi nuovi":
  - a mask byte with bit `i` for `PieceSlot.values()[i]`;
  - for each mounted slot in `PieceSlot` order: `writeUtf(tier.id())`, two `writeVarInt`, two `writeInt`;
  - the cell boolean, with two `writeVarInt` when present;
  - the flags byte (bits 0-5);
  - the selection byte.
  Reading iterates `PieceSlot.values()`, never the map, and rebuilds through the records' constructors, so an unknown tier throws (`Tier.byId`). The largest view that can be written is 188 bytes (six `industrial` pieces, every varint 5 bytes).
- **`station/StationViewPacket`** `(int containerId, StationView view)`: `ID = Ids.of("station_view")`, and a `CODEC` that writes the varint menu id, then the view. A null view throws `NullPointerException`.
- **`station/StationNet`**:
  - `register()` registers the S2C channel. Its handler is `(msg, ctx) -> apply(ctx.player().get(), msg)`.
  - `apply(player, msg)` calls `setView` only when `player != null`, `player.containerMenu instanceof StationMenu`, and the ids match; otherwise it does nothing.
  - It is registered in `RedstoneExoframes.init()` right after `ClientRules.register()`.
- **`station/StationOpening`** gains its third component, `(BlockPos main, UUID frameId, StationView view)`. `write`/`read` handle the view with the codec after the UUID, in the same order; a null view throws `NullPointerException`.
- **`frame/FramePermissions.canToggleLock(ServerPlayer, FrameState)`**: `player.hasPermissions(2) || player.getUUID().equals(state.owner())`, with null checks like `canUse`.
- **`station/StationMenu`**:
  - The private constructor now receives the `Inventory` and keeps `inventory.player` as `viewer`.
  - The selection lives in its own final field, taken from the opening view. It is not recomputed and not read from `view`, which `setView` can overwrite.
  - `view` and `lastSentView` both start at the opening view; `viewsSent` starts at 0.
  - `lastSentTick` starts at `NEVER` (`Integer.MIN_VALUE`), so a send can happen in the opening tick, as `viewIsSentAtMostOncePerTick` requires.
  - New methods: `view()`, `setView(view)` (null rejected), `lastSentView()`, `viewsSent()`. The Javadoc of `viewsSent()` says it counts the menu's own calls to `Compat.net().sendToPlayer`, including those `Net` skips on NeoForge 1.21.1, per the coordinator's resolution.
  - `opening(...)` builds the view through a private `viewFor(player, frame, selection)` helper, which `broadcastChanges` shares. The arguments are exactly the brief's: `canToggleLock`, `config().server.ownerOnlyFrames`, `hasStowed`, `getAbilities().instabuild`. No `isCreative()`.
  - `broadcastChanges()` runs, in order:
    1. `super.broadcastChanges()`;
    2. if the menu is on the server, a `try`. A `RuntimeException` there writes the ERROR `Station view update failed for player {}` with the exception once per menu (its own flag, `viewFailureLogged`), and the menu goes on;
    3. inside the `try`: `now = server.getTickCount()`; if `now < lastSentTick`, the limit restarts;
    4. if a view was already sent in this tick, or the viewer is not a `ServerPlayer`, return;
    5. with no record, return (`stillValid` closes the menu);
    6. rebuild the view; if it `equals` the last sent one, return;
    7. otherwise `sendToPlayer(viewer, ID, new StationViewPacket(containerId, view))`, then update `lastSentView`, `view`, `lastSentTick` and `viewsSent`.

## Tests and results

### TDD evidence

- **Step 1 RED** (`t4/step1-red.txt`): `./gradlew --console=plain :1.21.1-neoforge:test --tests '*StationViewTest' --tests '*StationViewCodecTest'` gave `BUILD FAILED`, with `compileTestJava` reporting 83 errors (`cannot find symbol: class StationView`, `class StationViewPacket`). That was expected, since the types did not exist yet.
- **Step 2 GREEN:**
  - the same command (`t4/step2-green-neoforge.txt`) gave `BUILD SUCCESSFUL`, with XML `StationViewTest tests=6 failures=0` and `StationViewCodecTest tests=3 failures=0`;
  - `./gradlew --console=plain :1.20.1-forge:test --tests '*StationViewCodecTest'` (`t4/step2-green-forge.txt`) gave `BUILD SUCCESSFUL` and `StationViewCodecTest tests=3 failures=0`.
  - Step 2 also pulled in `canToggleLock`, the view in `StationMenu.opening` and `view()`, and fixed the two-argument `StationOpening` call in `assertClientMenuStaysValid`. The game tests live in `src/main`, so `:test` cannot compile without them. `lastSentView`, `viewsSent` and `StationNet` waited for Step 4, so that Step 3's RED is the one the brief names.
- **Step 3 RED** (`t4/step3-red.txt`): `./gradlew --console=plain :1.21.1-neoforge:compileJava` gave `BUILD FAILED`, with 33 errors and only these missing symbols: 14 `method viewsSent()`, 14 `method lastSentView()`, and 5 `StationNet` (4 as a variable, 1 as a class). They were counted in the compiler output, before Gradle's failure summary repeats them. These are exactly the brief's.
- **Step 4 GREEN:**
  - `bash tools/run_gametests.sh 1.20.1-forge` (`t4/step4-gametests-forge-1.txt`) → `OK 1.20.1-forge: 234 test obbligatori passati, nessun avviso fuori elenco`. I ran this variant first, as advised, because 1.20.1 encodes and sends the packet to the mock connection; NeoForge skips it.
  - `bash tools/run_gametests.sh 1.21.1-neoforge` (`t4/step4-gametests-neoforge-1.txt`) → `OK 1.21.1-neoforge: 234 …`.
  - The four-variant results are under "Step 7".

### Mutation checks (are the new tests vacuous?)

The mutations were applied by `t4/mutate.py` (exact replacements, each found once, with backups), then restored. After all of them, `sha256sum -c t4/pristine.sha256` reported every file OK.

- **J** (JUnit, `t4/mutation-J.txt`): the reader swapped the `hasStowed`/`free` bits and read the slot bits reversed. Exactly two tests failed:
  - `roundTripsAFullViewAndTheOpening` ("the full view without HELMET");
  - `roundTripsABareFrameWithoutCell` ("the bare frame with only flag 4").
  Both were caught only by the loops I added (see below). With all flags true in one view and all false in the other, and the slot mask all ones in the full view, the brief's two cases alone would have let both mutations through.
- **A** (in-world, 1.21.1-neoforge, `t4/mutation-A.txt`): four mutations, exactly 4 tests failed, each with its own message:
  - `apply` ignoring the id → `view_for_another_menu_is_ignored`;
  - no once-per-tick limit → `view_is_sent_at_most_once_per_tick` ("2 views sent, helmet at 160");
  - `canToggleLock` always true → `view_carries_the_players_rights` ("B's view … canToggleLock=true");
  - `StationOpening.read` dropping the view → `opening_carries_the_frame_view` ("the client menu read …").
- **B** (`t4/mutation-B.txt`): two mutations, exactly 4 tests failed:
  - the view never sent → `view_follows_the_record_within_a_tick` ("0 views sent, torso at 180"), `view_is_sent_at_most_once_per_tick` and `view_carries_the_players_rights`;
  - the opening selection always 0 → `opening_carries_the_frame_view` ("only the torso selects 0").
- **C** (`t4/mutation-C.txt`): two mutations, exactly 2 tests failed:
  - the limit starting at the opening tick → `view_is_sent_at_most_once_per_tick` ("0 views sent");
  - `hasStowed` always false → `view_carries_the_players_rights` ("the view of a frame with stowed boots").
- **D** (`t4/mutation-D.txt`): `free` from `isCreative()`, which the mock player always answers true. Exactly 2 tests failed: `opening_carries_the_frame_view` and `view_carries_the_players_rights`.

### Step 5: the real client, one variant at a time

`./gradlew --stop` first.

- **1.21.1-neoforge** (`t4/client-neoforge.txt`, shots in `t4/shots-neoforge/`): `EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh 1.21.1-neoforge …/shots-neoforge` exited 0 after 135 s (130 s in `client_shot.sh`):
  ```
  ok anteprima: ritaglio magenta=1193 riquadro=41x85+604+152; schermata magenta=1193 riquadro=41x85+604+152
  ok orientamento: meta' destra del ritaglio magenta=0 riquadro=0x0+0+0
  ok pulsante: pixel=1112 copertura=0.21 mirino=0 barra=0 energia=0 riquadro=36x32+730+410
  OK 1.21.1-neoforge: scatti in …/shots-neoforge
  ```
  `log-check.txt` was 0 bytes. In `rcon.log`, `exoframe unlock` answered "Exoframe unlocked: anyone can use it", and `exoframe remove` came after it. There was no "lost connection" or "left the game" for Exo. `telaio-tolto.png` shows the screen closed with "The exoframe is no longer available at the station".
- **1.20.1-forge** (`t4/client-forge.txt`, `t4/shots-forge/`): exit 0 after 134 s (130 s in `client_shot.sh`). The same three `ok` lines with the same numbers (1193 / 0 / 1112), `OK 1.20.1-forge`, and a 0-byte `log-check.txt`. The unlock succeeded and the player did not disconnect.
- **Diagnostic run, never committed** (`t4/client-neoforge-diag.txt`, `t4/shots-neoforge-diag/`). The smoke test alone would also pass if no packet were ever sent. I therefore added two temporary INFO lines (`mutate.py` set `DIAG`), ran once on 1.21.1-neoforge (exit 0, the same `ok` lines), and restored the files (checksums OK). The logs show exactly one send, after the unlock, and its arrival in the real client's open menu:
  ```
  [00:39:41] [Server thread/INFO] [Redstone Exoframes/]: [diag-t4] sent station_view to Exo for menu 1: unlocked=true, views sent 1
  [00:39:41] [Render thread/INFO] [Redstone Exoframes/]: [diag-t4] applied station_view to the open menu 1: unlocked=true
  ```
  So the opening-time extra data plus the test button press caused no spurious sends.
- **Steps without the game:** `EXO_SHOT_STEPS_ONLY=true` printed `OK passi: 35` (Task 3 had 33, plus the two new `@attendi`).

### Step 7: end-of-task commands, after the last edit

The last source edit was a Javadoc rewrap in `StationMenu`.

| Command | Result |
|---|---|
| the four-variant `build` + `collectJar` command | `BUILD SUCCESSFUL`. The full run with tests was `t4/step7-build.txt` (45 s, the four `:test` tasks executed). The rerun after the comment-only edit is `t4/step7-build-final.txt`: `compileJava` reran, and `:test` was up to date because the bytecode was identical. |
| JUnit count (`t4/step7-junit.txt`), the Global Constraints command | `1.20.1-forge 351`, `1.20.1-fabric 351`, `1.21.1-fabric 351`, `1.21.1-neoforge 351`; failures+errors 0 on each; the two `StationView*` suites on each |
| `bash tools/check_sources.sh` | `OK sources` |
| `bash tools/check_jars.sh` | `OK jars` |
| `bash tools/check_data.sh` | `OK data` |
| `bash tools/check_art.sh` | `OK art` |
| `bash tools/client/arm_measure.sh --selftest` | `OK arm_measure` |
| `bash tools/client/client_shot.sh --selftest` | `OK passi` |
| `bash tools/run_gametests.sh` (`t4/step7-gametests.txt`) | four lines `OK <variante>: 234 test obbligatori passati, nessun avviso fuori elenco` (1.20.1-forge, 1.20.1-fabric, 1.21.1-fabric, 1.21.1-neoforge), exit 0 after 140 s |

The checks' output is in `t4/step7-checks.txt`. Step 6: `grep -c 'station_view' docs/compat.md` → `3`.

## Additions beyond the brief's wording (same test counts: 6 + 3 JUnit, 5 in-world)

- **`StationViewCodecTest`:**
  - `roundTripsAFullViewAndTheOpening` also round-trips the full view minus each slot in turn, so each mask bit is off once;
  - `roundTripsABareFrameWithoutCell` also round-trips the bare frame with each flag alone;
  - `fullViewStaysUnderAKilobyte` also checks the largest writable view, not only the full one. The test asserts `< 1024`. I observed the 188-byte figure (in the `StationViewCodec` Javadoc and `docs/compat.md`) with a temporary probe: the assertion was made to fail so it would print the count, giving "the largest view takes 188 bytes" (`t4/probe-188.txt`). I then restored the test file by checksum and reran the build (`t4/step7-build-after-probe.txt`: `BUILD SUCCESSFUL`; `t4/step7-junit-after-probe.txt`: 351 on each variant, 0 failures).
  - Mutation J shows these loops catch reader/writer mismatches that the brief's two cases cannot.
- **`StationViewTest.constructorRejectsInconsistentValues`:** also a null cell, a null map value and a null tier (`NullPointerException`), the plan's "un componente nullo".
- **In-world tests:**
  - a premise that the record holds the test's helmet, torso and cell;
  - `buf.refCnt() == 0` after the release, so a lazy read would throw;
  - in each delayed stage, the menu is still open, otherwise "still 1" would be vacuous;
  - `menuOfB.viewsSent() == 0` after A changes game mode, which shows the views are per player.
  - The two-stage test schedules both delays up front (2 and 4). The first stage cleans up on its failure path, because a failed stage stops the test and a later stage never runs.
  - The stowed boots are taken back with `takeStowedOfPlaced` before `removeFrame`; otherwise the registry would log an orphaning WARN.
- **Helpers in `StationMenuGameTests`:** `placedRecord(helper, frame)` and `durability(view, slot)`. `assertClientMenuStaysValid` now writes `StationMenu.opening(player, main, record)`.

## Files changed

- Created:
  - `src/main/java/com/toxineren/exoframes/core/station/StationView.java`
  - `src/main/java/com/toxineren/exoframes/station/StationViewCodec.java`
  - `src/main/java/com/toxineren/exoframes/station/StationViewPacket.java`
  - `src/main/java/com/toxineren/exoframes/station/StationNet.java`
  - `src/test/java/com/toxineren/exoframes/core/station/StationViewTest.java`
  - `src/test/java/com/toxineren/exoframes/station/StationViewCodecTest.java`
- Modified, in the brief's list:
  - `station/StationOpening.java`, `station/StationMenu.java`, `frame/FramePermissions.java`, `RedstoneExoframes.java`
  - `gametest/StationMenuGameTests.java`
  - `tools/client/m4a1_shots.sh`
  - `docs/compat.md`
- Modified, outside the brief's list (ruling P16: each task leaves the docs true):
  - **`tools/client/README.md`:**
    - the m4a1 paragraph (line 153) gets the "vista" phase;
    - the m4a1 duration (line 206) becomes 135 s / 134 s;
    - line 161, "La sequenza cresce con i task … (il telaio, la vista, …)", now lists only what is still to come. This also closes the deferred Task 3 minor about "il telaio" there.
  - **`gametest/GameTestKit.java`:** one sentence of the `openStation` Javadoc. It said the menu "non manda pacchetti", which is no longer true now that `broadcastChanges` sends the view through `Net`. No code change.

`m4a1_shots.sh` changes:
- the "vista" phase (step 8) before "chiusura", exactly the table's steps;
- the header's phase list (renumbered 8-11, the frame on the station from step 5 to 9);
- "Non prova", reworded: the view's content and a visible change are proved elsewhere, and the visible change comes with the 4a-2 lock;
- the measured duration.

`docs/compat.md` changes:
- the `Net` "Uso" line gets the channel and its proofs;
- "Limiti noti", "Sincronizzazione solo del proprio stato", gets the station's own view, never the record nor `FrameStateCodec`;
- "Menu e schermate":
  - "I dati extra" now includes the view;
  - a new point, "La vista del telaio";
  - "I test in mondo" gets the five tests and the corrected "no slot/data packets" wording.

## Self-review

- **Line length and quotes:** every added line in `.java`/`.sh` is 120 characters or fewer, counted in characters with Python. No typographic quotes anywhere in the added lines (1133 lines checked). Javadoc and comments are in Italian with ASCII apostrophes; identifiers, exceptions and the log line are in English.
- **DEC-39:** no `isCreative()` in new code (mutation D proves the tests would notice). The per-tick `broadcastChanges` catches `RuntimeException` and logs one ERROR per menu. The time rule compares `getTickCount()` with a saved value and restarts if the counter went back.
- **`check_sources`:** `StationNet` names no client class, not even in a comment. `StationView`, `PieceInfo` and `CellInfo` are named in `src/test/java` (rule 1c).
- **The client constructor** still reads everything immediately; the view is a record copy, and `openingCarriesTheFrameView` releases the buffer before asserting.
- **Cleanup:** no new log lines. The 234 runs pass the allowlist check on every variant, and no new allowlist entries were added.

## Concerns

0. **For the controller to rule on: edits outside the brief's file list**, made under ruling P16:
   - `tools/client/README.md`: lines 153, 161 and 206;
   - one sentence of the `GameTestKit.openStation` Javadoc (no code).
   The line 161 edit also closes the Task 3 minor the ledger deferred to the final review ("README:161 still lists 'il telaio' among the phases the m4a1 sequence will grow"). That entry can be struck.
1. **The codec over a mock connection only runs on 1.20.1.**
   - On NeoForge 1.21.1, `Net` skips the mock player, which is why `viewsSent` counts decisions.
   - On Fabric 1.21.1, `Net` hands the payload object to the mock's embedded channel without encoding it.
   - On 1.20.1, `NetImpl` encodes eagerly.
   - The codec itself is covered by JUnit on all four variants and by the real client on 1.21.1-neoforge and 1.20.1-forge. The diagnostic run shows the real decode and apply.
2. **One last view before a forced close.** `ServerPlayer.tick()` calls `broadcastChanges` before `stillValid`. In the tick where the owner locks the frame for B, B can get one last view (showing it locked) just before the close. This is harmless: it is B's own view of a frame B was using, and the next packet closes the screen.
3. **On the server, `view()` and `lastSentView()` can differ** after someone calls `setView` on the server menu. Only `viewForAnotherMenuIsIgnored` does that. `broadcastChanges` compares with `lastSentView`, so sending is unaffected.
4. **`NEVER = Integer.MIN_VALUE`.** If the int tick counter ever wrapped to exactly that value, one tick's send would be delayed by a tick. This is negligible (2^31 ticks is about 3.4 years of uptime) and self-correcting.
5. **The client step ran on two variants only**, as the brief says. The four-variant client runs are Task 6's.
6. **Two branches of `broadcastChanges` have no test.**
   - The `RuntimeException` catch, with its once-per-menu ERROR: an in-world test that triggers it would write an ERROR line, which the log allowlist forbids (this part adds no allowlist entries), and JUnit cannot build the menu without a server.
   - The reset when the tick counter goes back: the in-world tests cannot move `server.getTickCount()` backwards.
   - Task 1's review raised the same gap for `stillValid`'s catch. Both branches follow the same DEC-39 pattern as `stillValid`, which reading the code verifies.
