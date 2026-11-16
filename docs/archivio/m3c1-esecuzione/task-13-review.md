# Task 13 review (2397378..7a54fe9)

### Spec Compliance

- ✅ Spec compliant. Every file in the brief's list has its hunk, and nothing extra was added:
  - `core/energy/ActivityRule.java` (new): `WINDOW = 4`, `MOVING = 0.01`, `TELEPORT = 8.0` (lines 18-24); `next` returns `IDLE` and calls `reset()` for a passenger or a distance strictly above `TELEPORT` (41-44). The sample goes into a circular window of 4 that includes the current one (45-49), and the window counts as moving when any single sample is strictly above `MOVING` (51-57). `reset()` empties the window (64-67). The class Javadoc covers why the window exists, that only horizontal distance counts (ladder accepted) and why the constants are not in the config. Design §12.3 "Attivita'" is met.
  - `frame/SuitTick.java` (new): `LEAVE_FRAME_FIRST` is public and `SYNC_INTERVAL = 5` is package-private (60-63). `RECORDS` uses `MapMaker().weakKeys()` (66) and `FAILURES_LOGGED` uses `ConcurrentHashMap.newKeySet()` (69). `register()` matches the brief exactly (79-86). The body of `tick` (95-153) is the brief's code line for line inside `try/catch (RuntimeException)`, with the one-time ERROR `Exoframe tick failed for {}` (149-152). In particular: `isPassenger()` is read before `stopRiding()` (119-123); the position is read after the dismount (124); at a dimension change the window restarts and the sample is 0 (126-130); the jump base is reread on every tick (133-134); nothing is drained under `instabuild`/`isSpectator()` (135); there is one `mutate`, and `dirty` is set only when `mutate` returns true and the drain is at least 1 (139-148). `forget` is package-private and `hasRecord` is public (156-164). The `Record` fields and the `lastSyncTick = now - SYNC_INTERVAL` initial value are as listed (167-188). The class Javadoc covers all six required points.
  - `frame/WornState.java:402-405`: the tail runs after `FrameLog.event` exactly as written, and the Javadoc is updated (class and method).
  - `RedstoneExoframes.java:57-59`: `SuitTick.register()` comes right after `SlotGuard.register()`, with the required comment. Line 69 registers `EnergyGameTests` right after `CarryOverGameTests`.
  - Lang files: `leave_frame_first` sits right before `frame_taken` in both files, with the exact texts (en_us.json:707, it_it.json:725). Both files have the same keys in the same order.
  - `ActivityRuleTest`: the 7 cases match the brief's sequences one for one.
  - `EnergyGameTests`: 11 `add(tests,` lines in the brief's order (452-463; the line-wrapped `add` at 462 follows the existing style in `ExoframeGameTests`/`CarryOverGameTests`). The `prepared`/`walk` helpers are verbatim. Each test uses the brief's times, positions and expected numbers. I re-derived every expected value from the default rates: idle 100 + 20×100 → 4 998; walk 100 + 20×400 → 4 992; sprint → 4 980; jump 15 with carry 500 at the end; teleport 1 100 → 4 999; dimension 1 300 then +400 → 4 999 (without the restart the three WALK ticks give 4 998); boat 2 100 → 4 998; spectator 100 + 8 000 → 4 992. Each test closes with `leaveTestFrame` and `succeed`. The dimension test restores the overworld in `finally`, and the boat test discards the boat in `finally`.
- ⚠️ Cannot verify from diff: none. The test counts in the report (four × `OK <variant>: 127`, 116 + 11) agree with the 11 new `add(tests,` lines. I did not re-run them, per the contract.

Focused checks outside the diff (named risk → what I checked):
- Risk: `isSpectator()` on the mock player always returns false, as it does in vanilla `makeMockServerPlayerInLevel`, which would make `spectatorDrainsNothingAndKeepsTheRecord` meaningless. Checked `compat/impl/GameTestsImpl.java:139-162`: `MockPlayer` overrides only `isCreative()`, so `isSpectator()` follows the game mode. The test is real.
- Risk: the new `Compat.attributes().clear` in `WornState.changed` runs before the exit step-out and could change the player's size. Checked `compat/impl/FrameAttributesImpl.java:46-51`: it only removes four transient modifiers (speed, knockback, armor, toughness), so it does not touch the size or the step.
- Risk: `FrameInteractions.tell` might not show in the action bar or might not reach `messages()`. Checked `frame/FrameInteractions.java:314-316`: it calls `displayClientMessage(..., true)`, which goes through `sendSystemMessage(Component, boolean)`, the method `MockPlayer` records.
- Risk: comments with non-ASCII apostrophes (Global Constraints). Grepped the six changed Java files for non-ASCII: the only hits are `§`, which existing files already use.

### Strengths

- `SuitTick.tick` is a faithful transcription of the ordered body. The comments the implementer kept are the ones that carry the traps ("letto prima di stopRiding", "dopo la discesa", "a ogni tick, pagato o no").
- `ActivityRule` is minimal and correct: the "first `count` slots after a reset to index 0" invariant makes the scan right without clearing the array, and `stopKeepsThreeTicksOfTail` / `jitteryWalkStaysWalk` exercise the wrap-around.
- The in-world tests prove the rules rather than just running them. The creative test is paired with a survival player. The spectator test has a survival second half. The dimension test fails with the brief's diagnostic text when the window is not restarted. The boat test moves the dismount point so that a missing passenger rule would show as walking. `noJumpCostWithAnEmptyCell` proves the jump base is reread without payment.
- The `WornState` Javadoc now states exactly when the record and the attributes go and why entry does nothing.

### Issues

#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)
- [T13-F1] `src/test/java/com/toxineren/exoframes/core/energy/ActivityRuleTest.java:90-104`: no JUnit case calls `reset()` directly or resets a window that holds more than one sample. A `reset()` that sets `next = 0` but leaves `count` alone still passes both `teleportIsIdleAndClearsTheWindow` and `passengerIsIdleAndClearsTheWindow`: after the reset the single stale slot is overwritten, and the extra slot it scans is the array's default 0.0. Only the in-world `dimensionChangeRestartsTheWindow` (three walk samples, then a reset) would catch it. Why it matters: `reset()` is public and the dimension and clock-going-back paths in `SuitTick` (Task 14) call it directly, so the pure rule should pin it in JUnit too. Fix: add a case that runs `0.2` three times, then `reset()`, then `next(0.0, false, false)` and expects `IDLE`.

### Assessment

**Task quality:** Approved

**Reasoning:** The diff matches the brief's ordered tick body, the `ActivityRule` contract, the `WornState` tail, the registrations, the lang keys and all 18 tests one for one, and every expected number re-derives from the default rates. The only gap is a small JUnit coverage hole around a direct `reset()`, which the in-world tests already cover.
