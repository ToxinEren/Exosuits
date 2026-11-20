# Task 4 review: la vista del telaio (`StationView`, codec, dati extra, `station_view`)

Base `0e7b20f`, head `1dd63b6` (one commit, 15 files).

**How it was read.**
- **The diff.** I read the diff file in eight passes, by diff-file lines: 1-151, 152-381, 381-610, 611-836, 837-1141, 1140-1396, 1396-1697 and 1698-1894. No hunk was cut off, so I read no changed file to judge a hunk.
- **Outside the diff.** I made two kinds of read, both outside any hunk:
  - one focused grep of the unchanged `cleanUp`/`closeStation` helpers in `StationMenuGameTests.java`, for a named risk (menus left open);
  - `wc -l` line counts of six changed and new files, for the structure question.
  The other out-of-diff reads are listed under "Checks outside the diff", one per named risk.
- **References.** All `file:line` references are new-file lines, mapped from the hunk headers.
- **Design.** I read design §5.5 (spec lines 166-175) and §5.13 (lines 311-321) to check the brief against its binding text.
- **Runs.** I started no test, Gradle, git or client run.

### Spec Compliance

- ✅ **Spec compliant.**

#### File list, one file at a time

- **Files to create.** All six have their hunk: `core/station/StationView.java`, `station/StationViewCodec.java`, `station/StationViewPacket.java`, `station/StationNet.java`, `test/core/station/StationViewTest.java` and `test/station/StationViewCodecTest.java`.
- **Files to modify.** All seven have their hunk: `station/StationOpening.java`, `station/StationMenu.java`, `frame/FramePermissions.java`, `RedstoneExoframes.java`, `gametest/StationMenuGameTests.java`, `tools/client/m4a1_shots.sh` and `docs/compat.md`.
- **Edits outside the list.** There are two, both doc-only and both the ones ruling P18 accepts: `tools/client/README.md:153,161,206`, and the `openStation` Javadoc in `GameTestKit.java:815-817` (no code).

#### Produces

- **`StationView`.**
  - The record has the nine components in the plan's order (`StationView.java:39-41`).
  - The compact constructor copies into an `EnumMap` wrapped unmodifiable. It throws NPE for a null map, key, value or cell, and IAE for a selection outside [0, 7] (`:47-57`).
  - `PieceInfo` (`:66-84`) and `CellInfo` (`:91-100`) carry exactly the plan's invariants.
  - `of` is at `:108-123`, `selectionOf` (the ordinal) at `:126-128`, `initialSelection` at `:134-142`. `SELECT_CELL` is 6 and `SELECT_ALL` is 7.
- **`StationViewCodec.INSTANCE`.**
  - The writer follows the "Tipi nuovi" format exactly (`StationViewCodec.java:49-74`):
    - the mask byte;
    - for each mounted slot, in `PieceSlot` order: `writeUtf`, two `writeVarInt`, two `writeInt`;
    - the cell boolean, plus two varints when present;
    - the flags byte, bits 0-5;
    - the selection byte.
  - The reader iterates `PieceSlot.values()` and rebuilds through the record constructors. An unknown tier throws IAE from `Tier.byId` (`:77-100`).
- **`StationViewPacket(int containerId, StationView view)`** has `ID = Ids.of("station_view")` and a `CODEC` that writes the varint id, then the view (`StationViewPacket.java:20-38`).
- **`StationNet`.**
  - `register()` uses exactly the brief's lambda (`StationNet.java:22-25`), and it is called right after `ClientRules.register()` (`RedstoneExoframes.java:78-79`).
  - `apply` is null-tolerant and requires `instanceof StationMenu` plus a matching id (`StationNet.java:32-37`).
- **`StationOpening(main, frameId, view)`** writes and reads in the same order, the view through the codec (`StationOpening.java:20-43`).
- **`FramePermissions.canToggleLock`** is `hasPermissions(2) || uuid.equals(owner)` (`FramePermissions.java:41-45`), with no `isCreative()`.
- **`StationMenu`.**
  - `viewer = inventory.player` (`StationMenu.java:117`).
  - The selection is fixed at opening (`:123`), as ruled.
  - At opening, `view` and `lastSentView` are the opening view and `viewsSent` is 0 (`:124-125`).
  - `opening` passes the brief's arguments through the shared `viewFor` (`:145-160`).
  - `view()`, `setView`, `lastSentView()` and `viewsSent()` are at `:173-202`.
  - The `viewsSent()` Javadoc says it counts the menu's own `sendToPlayer` calls, including those `Net` skips on NeoForge 1.21.1 (`:193-199`), as ruled.
- **`broadcastChanges`** (`:241-255`, with `sendViewIfChanged` at `:258-280`) follows the brief's order:
  1. `super.broadcastChanges()` first (`:242`).
  2. Server only: `level == null` returns (`:243`).
  3. A `try` whose catch writes the once-per-menu ERROR `Station view update failed for player {}` with the exception (`:246-254`).
  4. `now = getTickCount()`, with a reset when `now < lastSentTick` (`:260-263`).
  5. At most one send per tick (`:264`).
  6. No record: nothing happens (`:267-270`).
  7. The view is rebuilt with the opening selection and compared by record equality (`:271-274`).
  8. The send, then `lastSentView`, `view`, the tick and `viewsSent` are updated (`:275-279`).

  The tick check comes before the rebuild. The outcome is the same as the brief's order (a pending change waits for the next tick), and it skips a useless rebuild.

#### Tests against the brief

- **JUnit.** All six `StationViewTest` cases (`StationViewTest.java:56,84,99,108,120,151`) and all three `StationViewCodecTest` cases (`StationViewCodecTest.java:50,78,92`) use the brief's exact values.
- **In-world tests.**
  - The five tests are registered with `add(tests,` (`StationMenuGameTests.java:121-125`) and implemented as specified (`:612,681,736,788,848`).
  - Each one sets the game mode and stands its players next to the station.
  - Each one cleans up through `cleanUp`, whose `closeStation` closes every open menu (`:1031-1045`).
  - Delays: 6 ticks at most, each delayed body through `runInSequence`.
- **Counts.** 351 JUnit ×4 (`t4/step7-junit.txt`, failures+errors 0) and 234 in-world ×4 (`t4/step7-gametests.txt`, "nessun avviso fuori elenco").

#### Review Focus #3 (extra data read late)

1. `openingCarriesTheFrameView` writes the buffer and builds the client menu from it.
2. It releases the buffer in a `finally` (`:644`).
3. It asserts `buf.refCnt() == 0` (`:646`).
4. Only then does it compare position, `frameId` and view (`:647-649`).

A lazy read would hit the released buffer. Mutation A ("`StationOpening.read` dropping the view") fails this test (`t4/mutation-A.txt`).

#### Steps 5, 6 and 8

- **Step 5 (real client, 1.21.1-neoforge and 1.20.1-forge).**
  - `t4/client-neoforge.txt` and `t4/client-forge.txt` each have the three `ok` lines and `OK <variante>`.
  - `log-check.txt` is 0 bytes in both shot folders.
  - `rcon.log` answers "Exoframe unlocked: anyone can use it" on both variants.
  - The client constructor reads the view eagerly, so `anteprima` passing on both variants also shows that the real extra data, view included, was decoded on those two loaders.
- **Step 6.**
  - `docs/compat.md:47` (`Net`, **Uso**) and `:403` ("Sincronizzazione solo del proprio stato") carry the brief's text.
  - `station_view` appears on 3 lines (47, 255, 403); the brief expects at least 2.
- **Step 8 (commit content).** No diagnostic code was committed: `diag-t4` occurs 0 times in the diff, and the three "diag" matches are the word *diagnosi*.

#### ⚠️ Cannot verify from the diff

- **Client-side `apply` in the real client** is proven only by the uncommitted diagnostic run on 1.21.1-neoforge. `t4/shots-neoforge-diag/` has "[diag-t4] sent station_view to Exo for menu 1: unlocked=true, views sent 1" and "[diag-t4] applied station_view to the open menu 1".
  - The committed "vista" phase (`m4a1_shots.sh:159-160`) would also pass if the server never sent the packet. Ruling P4 accepts this for 4a-1.
  - On 1.20.1-forge the phase shows only that nothing failed. The Fabric variants did not run it; Task 6 runs all four.
  - The coordinator should make sure 4a-2's lock gives this phase a measured check.
- **Two branches of `StationMenu.broadcastChanges` have no test:** the `RuntimeException` catch (`StationMenu.java:248-254`) and the tick-counter reset (`:261-263`). I verified them by reading only; see Minor 2.
- **Commit signature and trailers** (Global Constraint: "commit firmati come fa l'ambiente") are not visible in the diff, and I was told not to re-run git. The report claims SSH signing (`gpgsig` present) and the two trailer lines. The controller can confirm with `git cat-file -p 1dd63b6`.

### Strengths

- **`viewFor` is shared** (`StationMenu.java:155-160`): the opening view and the per-tick view come from the same code, so they cannot drift apart. That is what keeps spurious sends at zero. `viewCarriesThePlayersRights` asserts it (`menuOfB.viewsSent() == 0` after A's game-mode change, `StationMenuGameTests.java:816`), and so does the diagnostic run (exactly one send, after the unlock).
- **The codec reader is driven by `PieceSlot.values()`** and the record constructors, never by the map, so a malformed or unknown value throws instead of producing a half-built view (`StationViewCodec.java:77-100`). The 188-byte figure checks out: there are five tiers, `industrial` is the longest id, and 1 + 6×29 + 1 + 10 + 1 + 1 = 188.
- **The tests are not vacuous, and the evidence shows it.** I read all five mutation files.
  - **J** (the reader swaps the `hasStowed`/`free` bits and reverses the slot bits) fails exactly the two codec cases. It is caught only by the loops the implementer added (`StationViewCodecTest.java:54-61,84-88`); the brief's two cases, all-true and all-false, would have let it through.
  - **A**, **B**, **C** and **D** each fail exactly the in-world tests the report names, with distinct messages:
    - A fails four: the id ignored, no per-tick limit, `canToggleLock` always true, the view dropped on read.
    - B fails four: "0 views sent, torso at 180", "0 views sent, helmet at 180", "A's last sent view in creative", "the opening on a frame with only the torso selects 0".
    - C fails two: "after the first change: 0 views sent", "the view of a frame with stowed boots".
    - D (`isCreative()`) fails `opening_carries_the_frame_view` and `view_carries_the_players_rights`.
- **The in-world tests guard against vacuous passes.**
  - Premise assertions check the record (`StationMenuGameTests.java:630-633`).
  - Every delayed stage checks that the menu is still open.
  - The first stage cleans up on its own failure path (`:697-710`).
  - The stowed boots are taken back with `takeStowedOfPlaced` before the frame is removed, so no orphan WARN appears (`:826`).
- **DEC-39 holds in the new code:**
  - no `isCreative()` (mutation D shows the mock player would expose it);
  - one ERROR per menu;
  - the reset when the tick counter goes back.
- **The docs stay true beyond the minimum:**
  - "I dati extra" in `compat.md` (`:251`);
  - a new point, "La vista del telaio" (`:255`);
  - the corrected "no slot or menu-data packets" wording in "I test in mondo" (`:256`) and in `GameTestKit.openStation` (`:815-817`).
- **Hygiene.** Every added `.java`/`.sh` line is 120 characters or fewer, and none has typographic quotes (checked over the diff).

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

1. **Two copies of the lock rule.** `frame/FramePermissions.java:41-45` vs `command/ExoframeCommand.java:286`. This is **plan-mandated**: "la stazione la usa solo per la vista; il comando non cambia".
   - The command allows the change iff `player.getUUID().equals(owner) || source.hasPermission(2)`. `canToggleLock` checks `player.hasPermissions(2)` instead.
   - The two agree for a player typing the command. They differ only under `execute as` from a higher-permission source, which does not apply to the station.
   - This is a one-line predicate, not a duplicated block. But once 4a-2's lock button lands, two features will depend on it. If one copy changes alone, the station will show a lock button the command refuses, or the reverse.
   - **Fix, when 4a-2 adds the lock action:** make the command call `canToggleLock`, keeping the source-permission nuance explicit, or name the twin in both Javadocs.
2. **Two untested branches in `broadcastChanges`.** The once-per-menu ERROR (`StationMenu.java:248-254`) and the tick reset (`:261-263`).
   - Reading confirms both are correct and have the same DEC-39 shape as `stillValid`'s catch, which Task 1's review flagged for the same gap.
   - An in-world test would need an allowlist entry, which this part forbids. JUnit cannot build the menu without a server.
   - If the gap matters later, the throttle (`now`, `lastSentTick`, the reset) is small enough to move into a pure helper in `core/` and test with JUnit.
3. **Pre-existing build noise.** `Note: Some input files use or override a deprecated API.` comes from three compile tasks: `:1.20.1-forge:compileJava`, `:1.20.1-fabric:compileJava` and `:1.21.1-neoforge:compileJava` (`t4/step7-build.txt:12-13,39-40,87-88`).
   - This task did not introduce it. In Task 2, `t2/step8-build.txt` already prints it for 1.20.1-forge and 1.21.1-neoforge. In Task 3, `t3/step6-build.txt` prints it for all three.
   - On an incremental 1.20.1-fabric compile, javac names the file: `content/StationBlock.java` (`t3/fix1-build.txt`, `t4/step7-build-final.txt`).
   - **For the whole-branch review:** one compile with `-Xlint:deprecation` would list the sources. That is the coordinator's call; I ran no Gradle.
4. **`setView` on the server menu.** `StationMenu.java:181-183` is public. Called on the server menu, it makes `view()` differ from `lastSentView()` until the next send; only `viewForAnotherMenuIsIgnored` does this. The Javadoc implies client use ("arrivata dal server"). One sentence saying so, and that on the server `broadcastChanges` overwrites it at the next send, would remove the ambiguity the report raises in its concern 3.

### Checks outside the diff (one per named risk)

- **Risk:** `PieceInfo` or `CellInfo` stricter than their source types. A valid frame would then make `StationView.of` throw, and nothing catches that on the click path (`StationMenu.opening`).
  - **Checked:**
    - `core/armor/Piece.java:14-21` has the same rule: `maxDurability > 0` and `0 ≤ durability ≤ max`. Its mutators clamp (`damage` and `repairFraction`, `:64-89`).
    - `core/armor/Appearance.java:19-30` has the same colour rule.
    - `core/energy/EnergyCell.java:9-14` has the same cell rule, and its mutators clamp.
  - **Result:** closed.
- **Risk:** `canToggleLock` does not match the real `/exoframe lock|unlock` rule.
  - **Checked:** `command/ExoframeCommand.java:286`.
  - **Result:** same rule (Minor 1).
- **Risk:** the 188-byte claim (`StationViewCodec.java:18`, `docs/compat.md:255`) is wrong, or the codec round-trip misses a tier.
  - **Checked:** `core/armor/Tier.java` has five tiers, `industrial` is the longest id, and `byId` throws IAE.
  - **Result:** closed.
- **Risk:** the `apply(null, …)` branch the brief tests is unreachable from the real handler.
  - **Checked:** `compat/Net.java:57`. `ClientContext(Supplier<Player> player)` means `.get()` can return null.
  - **Result:** closed.
- **Risk:** the new tests leave menus open before `succeed()`.
  - **Checked:** a grep of the unchanged helpers at `StationMenuGameTests.java:1031-1045`. `cleanUp` → `closeStation` calls `closeContainer()` on every open menu.
  - **Result:** closed.
- **Risk:** `@throws` in a record-level Javadoc (`StationView.java:36-37`) breaks house style.
  - **Checked:** the same pattern exists in `station/StationFinder.java:43` and `core/station/StationReach.java:45`.
  - **Result:** not a finding.
- **Evidence read:** Step 5, Step 7, and all five mutation files (J, A, B, C, D), in `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t4/`. The game-test WARN/ERROR lines that name the mod are the allowlisted ones earlier data tests provoke on purpose; none is about the view.

### Structure

- **`StationMenuGameTests.java`** grew by 312 lines, to 1050. The plan puts Tasks 1, 3, 4 and 5 in this class. The two new helpers (`placedRecord` and `durability`, `:998-1008`) remove repetition.
- **`StationMenu.java`** is 332 lines (+153, about half of it Javadoc). It still has one responsibility: the menu, with the view part of its server-side job.
- **New files** run from 105 to 181 lines, each with one job. Nothing new is large.

### Assessment

**Task quality:** Approved

**Reasoning:** Every Produces item, test case, client step and doc edit in the brief is present and matches the "Tipi nuovi" formats and design §5.5/§5.13. `broadcastChanges` follows the brief's order and DEC-39, and mutation-checked tests show that send-on-change, once per tick, per-player rights and the eager read are really verified. What remains is Minor: one plan-mandated duplicated predicate, two branches verified only by reading, pre-existing compiler notes, and one Javadoc clarification.
