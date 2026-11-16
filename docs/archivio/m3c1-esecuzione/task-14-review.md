# Task 14 review (7a54fe9..ae43516)

### Spec Compliance

- ✅ Spec compliant. Every item of "Produces" is in the diff:
  - `ENERGY_LOW` / `ENERGY_EMPTY` public constants, `SuitTick.java:126-130` (diff lines), values as in the brief.
  - Record fields `applied`, `sentEffects`, `sentEnergy`, `sentConditions` (`SuitTick.java:301-308` in the record).
  - Steps 6-8 at `SuitTick.java:182-209` (working tree) are the brief's body line for line; the only change is
    `frames.syncTo(player)` for `Compat.frameState().syncTo(player)`, and `frames` is that same singleton
    (`SuitTick.java:130`, `Compat.java:45-46`). Transitions compare with `r.applied`, never with `sent*`.
  - `conditions(FrameState)` builds an `EnumMap` slot -> `piece.condition()` (`SuitTick.java` after `logFailure`);
    `FrameState.pieces()` never holds null values (`FrameState.java:73`, `FrameRules.java:87` iterates without a
    null check), so no NPE risk.
  - `afterSync` (`SuitTick.java:221-249`): early return without a record, `sent*` only when `get` is present,
    `lastSyncTick = r.lastNow` and `dirty = false` in any case, inside a `RuntimeException` catch that writes the
    same once-per-player ERROR. Registered in `register()` (`SuitTick.java:115`).
  - `markDirty` package-private, no-op without a record.
  - Javadoc of the class carries the §12.4 sync rule and the transitions (diff lines 104-118).
  - Lang keys in both files, right texts, right place (before `leave_frame_first`, after `exit_hold`), same order.
  - `docs/compat.md:68`: the sentence is the brief's text verbatim, right after "(al massimo 4-5 volte al secondo)".
  - Seven tests appended to `EnergyGameTests.generate()` with the brief's names, set-up order (recorder opened after
    the entry, `synced` read right after `prepared`, `start` after preparation) and assertions.
- Checks run:
  - Step 6 grep: the only non-comment `getTickCount` in `SuitTick.java` is line 112 (the `register()` listener);
    lines 47, 48, 105 and 126 are Javadoc and filtered out.
  - Named risk "the listener never fires or fires on another instance": `Compat.frameState()` is a holder
    singleton (`Compat.java:45-46`, `:82`), `FrameStateAccessImpl.syncTo` calls the listeners after the send on
    every call (`FrameStateAccessImpl.java:151-154`), and `SuitTick.register()` is called once from
    `RedstoneExoframes.java:65`.
  - Named risk "a sync inside the tick stamps the previous tick": `r.lastNow = now` is set at `SuitTick.java:151`,
    before any `syncTo` in the tick, and nothing between record creation and step 8 syncs.
  - Clock-back rule (`now < lastNow` -> `lastSyncTick = now - SYNC_INTERVAL`, `SuitTick.java:146-150`, Task 13)
    is what `clockGoingBackwardsRestartsTheLimits` relies on; `walk` places the first step at `start + step`
    (`EnergyGameTests.java:458-463`), so the 1 RE at `t0+103` matches the brief's count.
  - Line length: no line over 120 characters in the two Java files; 134 `add(tests, "` registrations (the 135th
    grep hit is the Javadoc example in `GameTestKit.java:84`), matching the plan's 134.
  - Javadoc and comments in Italian with ASCII apostrophes; identifiers, assertion messages and log in English.

### Strengths

- The step 6-8 body is transcribed exactly, comments included, so the plan's pinned order (apply before
  transitions, `applied` set before the sprint gate, sync last) is intact.
- `logFailure` gives `tick` and `afterSync` one ERROR line and one `FAILURES_LOGGED` set instead of two copies,
  which is what "la stessa cattura di `tick`" asks for.
- `lowAfterAChangeWornStillSpeaks` proves both that `afterSync` listens to structural `syncTo` and that the
  transitions ignore `sent*`; `clockGoingBackwardsRestartsTheLimits` proves `afterSync` uses the record's `now`,
  not the server's.
- TDD evidence is complete: compile RED, seven behavioural REDs with distinct messages, GREEN on four variants with
  a clean log.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T14-F1** `gametest/EnergyGameTests.java` (new tests): two branches of step 6 are not pinned by any test. The
  first evaluation at `EMPTY` (`ENERGY_EMPTY` without sound, `SuitTick.java:192`) is never exercised, so a
  regression that always tells `ENERGY_LOW` on a first evaluation passes; and the `EMPTY -> LOW` improvement is
  never exercised (only `LOW -> NORMAL` is), so a condition that also spoke on `EMPTY -> LOW` passes. The brief
  does not ask for these; a later task (e.g. Task 21's cell swap from an empty cell) could add them.
- **T14-F2** (plan-mandated) `docs/compat.md:68`: "(un tick fermo non manda nulla)" reads as "an idle tick never
  syncs", but at idle 2 RE/s every tenth idle tick completes 1 RE and syncs (the task's own
  `idleTickWithoutAWholeReDoesNotSync` asserts the sync at `t0+9`). The text is the brief's verbatim; a wording such
  as "(un tick fermo che non completa 1 RE non manda nulla)" would match the code. Coordinator's call.

### Assessment

**Task quality:** Approved

**Reasoning:** The diff implements steps 6-8, `afterSync`, `markDirty`, the listener registration, the keys and the
docs sentence exactly as the brief's body pins them, with seven tests that each prove one rule of §12.3/§12.4; the
only gaps are two untested branches and an imprecise plan-mandated doc parenthetical, both Minor.
