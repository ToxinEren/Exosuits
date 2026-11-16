# Task 21 review: Cambio cella (3abc47d..a102066)

### Spec Compliance

- ✅ Spec compliant. Each item of the brief's "Produces", "Testi", `docs/compat.md` and Step 1/3 lists has its hunk:
  - `core/energy/CellSwapRule.java:17-50`: `final`, private constructor, `SLOTS = 37`, `OFFHAND = 36`. The IAE message is exact, `null` throws an NPE, and the comparison is strict in a single pass. `Math.max(0, installedStored)` gives exactly `e > 0` for `-1` and `e > installedStored` for every value >= 0.
  - `frame/CellSwapPacket.java`: identical in shape to `FrameExitPacket` (compared against `frame/FrameExitPacket.java`).
  - `frame/CellSwap.java`: `registerNet` is verbatim. `handle` follows the six steps in order:
    - `Compat.frameState()` is on the first line (rule 5).
    - The four silent guards are on `:72-73`.
    - The interval check is `now >= last && now - last < cellSwapSeconds * 10L`, so it is a long product and `now < last` is accepted.
    - Energies are clipped through `toCell`.
    - `changeWorn` runs before the inventory is touched. On `false` it returns with no inventory change, no sound and no interval mark.
    - Then `split(1)`, the old cell into the same slot if that slot is empty, otherwise `Handouts.give`, then `setChanged`, `CELL_SWAP` and `LAST_SWAP.put`.
    - The class Javadoc covers the atomic order, why `update` is never used, the half-wait interval and the §5.6 derogation.
    - There is no `update(` in the file.
  - `frame/EntryHints.java`: `KEYS_HINT` is public. The two key names are package-private, which is consistent with the visibility rule because only `frame/` uses them. `SHOWN` is cleared on `PLAYER_QUIT`. `onEntered` sends `Component.keybind` ×2 in chat.
  - `FrameEntry.java:159`: `onEntered` runs after `WornState.changed` and the ENTER sound, just before `return true`. A grep finds `FrameEntry.enter` called only from `FrameInteractions.enter:222` and the tests, so `DebugWear` does not trigger the hint.
  - `FrameInteractions.java:198-217`: there is no `SLOT_TAKEN` for cells anymore. The swap goes through `updatePlaced`, `shrink` happens only outside instabuild, `Handouts.give` returns the old cell even in creative, then `refreshSummary` and `MOUNT`. The class, `onInteract`, method and `SLOT_TAKEN` Javadocs are updated.
  - `ExitKey.keyDown()` and `CellSwapKey` are present. `CellSwapKey`'s `canCount` includes `!ExitKey.keyDown()`, and its switch is identical to `ExitKey.tick`.
  - Registrations: `RedstoneExoframes.java:144-145` has a one-line comment each, and `RedstoneExoframesClient.java:275` is right after `ExitKey.register()`.
  - Lang: the four keys and texts are exact, after `exit_hold` and before `energy_low`, in both files.
  - `docs/compat.md`: all four places (Net "Uso", the C2S row of the API table, the `ExitKey` client seam entry, "Avvio del client") are updated with the brief's wording.
  - `CellSwapRuleTest`: all six cases with the brief's values.
  - Nine in-world tests with the brief's assertions, each ending with `leaveTestFrame` or `removeFrame`.
  - The tail of `mountPieceAndCellByHand` and its Javadoc follow the brief and the ruling.
  - The one stated deviation (`creative.setGameMode(GameType.CREATIVE)` in `placedFrameSwapsCells`) follows the Process ruling on the mock player's game mode.
- ⚠️ Cannot verify from diff: nothing open.
  - Step 7 evidence was checked on disk: `scratchpad/shot-120forge/log-check.txt` is 0 bytes, and `nessuna-cella-migliore.png` shows "No cell in your inventory holds more energy" on the action bar.

Named risks checked outside the diff:
- **A refused registry write still applies the lambda (cell duplicated in memory).** In `compat/impl/FrameRegistry.java:156-168` and `:269-281`, the read-only and missing-record checks return before `change.accept`. Safe.
- **The lambda throws after `removeCell` (cell lost).** In `core/frame/FrameState.java:61-70`, `insertCell` throws only on null, and `incoming` is never null. Safe.
- **Several cells in one stack would make the "same slot if empty" branch wrong.** `content/ModItems.java:22` declares the cell with `stacksTo(1)`. Safe.
- **`CellSwapKey` behaves differently from `ExitKey`.** `client/ExitHold.java` returns `CLEAR_PROGRESS` only on the transition, and `CellSwapKey` mirrors `ExitKey.tick` exactly.

### Strengths

- `CellSwap.handle` is a clean transcription of the atomic order. Every refusal returns before any inventory or interval side effect, and the menu-refusal test (`CellGameTests` `cellSwapRefusedWithAMenuOpen`) proves that the interval is not marked.
- `cellsAndRe` sums `getCount()`, which is stricter than counting stacks: a duplicate inside one stack would show. `cellSwapTakesTheFullestBetterCell` also asserts its own premise `{4, 23000}`.
- The control half of `cellSwapWithoutAFrameChangesNothing` keeps that test from passing against an empty `handle`.
- The new Java has no `isCreative()` and no non-ASCII characters in comments. All four variants report 168 tests.

### Issues

#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)

- **T21-F1 (plan-mandated)**
  - **Where:** `src/main/java/com/toxineren/exoframes/gametest/CellGameTests.java:229-230`.
  - **What is wrong:** The Javadoc of `cellSwapAfterDeathChangesNothing` says that with `update` in place of `changeWorn` "qui ne nascerebbe uno". That is not true while step 1 stands: `handle` returns at `frames.wornFrameId(player).isEmpty()` (`CellSwap.java:72`) before any registry call. The test proves the overall behaviour but not the `update`/`changeWorn` choice. Only the Step 9 grep and Task 11's `changeWornNeverCreatesARecord` guard that choice.
  - **Why it matters:** The sentence is copied from the brief, so a reader of Review Focus 1 may believe this test covers the `update` regression.
  - **How to fix:** Reword it, for example "il gestore si ferma senza un telaio indossato; changeWorn e' la seconda difesa".
- **T21-F2**
  - **Where:** `src/main/java/com/toxineren/exoframes/frame/CellSwap.java:94` and `:72`.
  - **What is wrong:** No in-world test takes the cell from the off hand, which would cover the mapping of index 36 to `Inventory.SLOT_OFFHAND` and the old cell returning to the off hand. No test covers the spectator refusal either. In `cellSwapTakesTheFullestBetterCell` the off hand holds only the losing tie.
  - **Why it matters:** The mapping is correct by inspection. A slip such as index 36 = the boots slot would be a `ClassCastException` in a packet handler, and no test would see it.
  - **How to fix:** Add a case with the only better cell in the off hand. Coverage only, not required by the brief.

### Assessment

**Task quality:** Approved

**Reasoning:** The swap is correct and atomic on both paths (worn and placed). A refused registry write leaves the inventory and the interval untouched. Tests, lang, docs and registrations match the brief item for item. Only a misleading test Javadoc and a coverage gap remain, both Minor.
