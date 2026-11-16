### Spec Compliance

- ✅ Spec compliant. Checked against the brief's "Produces" item by item:
  - `frame/DebugEnergy` is `final`, with `public enum Outcome { DONE, NOT_WEARING, NO_CELL, REFUSED }` (DebugEnergy.java:18-27). `set` returns NOT_WEARING when there is no `wornFrameId` or no state (:39-42), NO_CELL on a null cell before any `changeWorn` (:43-45), and otherwise calls `changeWorn(player, s -> s.insertCell(new EnergyCell(cap, max(0, min(stored, cap)))))` with true giving DONE and false giving REFUSED (:46-48). `cell` = `get(player).map(FrameState::cell)` (:55-58). This is empty when there is no record, and also when the cell is null, because `Optional.map` treats a null result as empty. The class Javadoc covers the operator tool, the real cloud client and checklist, LOW/EMPTY, and `changeWorn` being structural with an immediate sync (:10-16).
  - The command tree (ExoframeCommand.java:155-158) is `literal("energy").executes(showEnergy).then(argument("re", integer(0)).executes(setEnergy))`. I counted the parentheses in the HEAD file: the branch is a child of the `debug` literal, which carries `.requires(source -> source.hasPermission(OPERATOR_LEVEL))` (ExoframeCommand.java:131-132 at HEAD). So without level 2 the branch does not exist.
  - `showEnergy` (ExoframeCommand.java:214-235): it fails with `not_wearing` and returns 0, or with `no_cell` and returns 0. Otherwise it calls `sendSuccess(translatable(LANG+"energy.show", stored, capacity, translatable("energy."+MOD_ID+"."+state lower)), false)` with `state = RedstoneExoframes.rules().evaluate(state).energyState()`, and returns `stored`. The `EnergyState` constants NORMAL/LOW/EMPTY (core/energy/EnergyState.java:5-7) match the three lang keys.
  - `setEnergy` (ExoframeCommand.java:242-264) follows the brief's mapping. DONE sends `energy.set` with the charge read back after clamping (`DebugEnergy.cell(...).map(stored)`) and returns 1. NOT_WEARING and NO_CELL use `sendFailure` and return 0. REFUSED returns 0 with no message, as the Ruling at progress.md:13 requires.
  - Class Javadoc: the two new branches are listed among the debug branches, with design §12.14 and the read-only case.
  - Lang: there are seven keys, appended after `tooltip.redstone_exoframes.piece.new` in both files, in the same order and with the brief's texts verbatim (en_us.json / it_it.json, diff lines 491-498 / 516-523). The JSON is valid, with no trailing comma.
  - Tests: there are four `add(tests, ...)` lines at the end of `generate()`, which makes 168+4=172. The `failures(ServerPlayer, String)` helper matches the key itself or a direct child (EnergyGameTests.java:733-741). The four bodies match the brief's steps: sync +1, show returns 999, `cell` Optional; clamp to 10 000, -5 rejected with a syntax error and nothing changed, `set(-5)` gives 0; NOT_WEARING with nothing created; NO_CELL with no sync.
  - `docs/compat.md` is untouched, which is correct: `DebugEnergy` is in `frame/`, not a `compat/` seam.
  - The deviation the report declares (a private `execute` that wraps `CommandSyntaxException` because `Consumer<GameTestHelper>` cannot throw it) keeps the brief's unsuppressed source and the `== 0` assertions. It is a mechanical necessity, not a scope change.
- Check: global constraints. There is no `isCreative()`. Identifiers and exception messages are in English ("unknown outcome", "command failed: ..."), and the Javadoc is in Italian with ASCII apostrophes. `DebugEnergy`, `Outcome`, `set` and `cell` are public, as the visibility rule requires. Nothing names `compat.impl` outside `compat/`.
- Check (named risk: `cell` skips the `wornFrameId` guard that `set` and `showEnergy` use, so "wearing" might mean different things). Both `get` and `wornFrameId` read the same `FrameStateSavedData.worn(uuid)` record (FrameStateAccessImpl.java:62-68, 248-251), so the three agree.
- Check (named risk: `insertCell` refusing to replace an existing cell). `FrameState.insertCell` just assigns (FrameState.java:61-63), so the replacement is what the brief intends.
- ⚠️ Cannot verify from the repository: the Step 5 real-client run. The report gives 7.12 and 9.25 blocks on 1.20.1-fabric and 7.22 and 9.38 on 1.21.1-neoforge, exit 0, and an empty `log-check.txt`. These are inside the brief's ranges (6 to 8.2, and over 8.6). The shot folders are in the implementer's session scratchpad, not on the branch. The brief does not ask for them to be committed, and the 3b precedent committed screenshots only in the final checklist task.

### Strengths

- `DebugEnergy` is small and literal to the contract. The NO_CELL early return before `changeWorn` is proven by `debugEnergyWithoutACellSaysSo` (syncCount unchanged, EnergyGameTests.java:709-727). The DONE path through `changeWorn` is proven by `syncs == synced + 1` (:654-655).
- `setEnergy` echoes the charge read back after the clamp, not the requested value, so `debug energy 20000` reports 10 000.
- `debugEnergyWithoutAFrameChangesNothing` asserts both `get(p).isEmpty()` and `wornFrameId(p).isEmpty()`. This covers the "never create a frame from nothing" concern for the new entry point.
- The report is accurate. It names its one deviation and its untested branches (REFUSED, and how the `show` message renders).

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T23-F1** ExoframeCommand.java:244-263. `setEnergy` uses a switch statement with `default -> throw new IllegalStateException("unknown outcome")`. A switch expression would make the four cases exhaustive at compile time: `return switch (...) { case DONE -> { ...; yield 1; } case NOT_WEARING -> {...; yield 0;} ... };`. A fifth `Outcome` would then fail the build instead of throwing at runtime. The report's justification only applies to the statement form.
- **T23-F2** ExoframeCommand.java:218 vs DebugEnergy.java:39. `showEnergy` repeats `frames.wornFrameId(player).isPresent() ? frames.get(player) : Optional.empty()` verbatim from `DebugEnergy.set`. That gives two copies of what "wearing" means for this command. A package-public `DebugEnergy` accessor for the worn state (or having `showEnergy` call `DebugEnergy`) would keep one definition.
- **T23-F3** EnergyGameTests.java:744-751 and :733-741. The private `execute` copies the try/catch and the exact exception message of `GameTestKit.exoframeCommand` (GameTestKit.java:593-601), differing only in the source. `failures` repeats the loop of `GameTestKit.messages` (GameTestKit.java:137-145), adding the sibling check. The brief makes `failures` a private helper of this class, so that half is plan-shaped. `execute` is the implementer's choice, and a `GameTestKit` overload taking a `CommandSourceStack` would avoid the second copy.
- **T23-F4** EnergyGameTests.java:648-727. No test asserts the `energy.show` message or its state argument (at 999 RE it should be `energy.redstone_exoframes.low`). No test runs `debug energy` without an argument when there is no frame or no cell, so `showEnergy`'s two failure branches (ExoframeCommand.java:219-227) are never executed. Step 5 drives only the argument branch. A wrong state key, or a swapped failure key in `showEnergy`, would ship unseen. One unsuppressed `debug energy` in `debugEnergyShowsAndSetsTheCharge`, checking the key and its args, plus one in `debugEnergyWithoutACellSaysSo`, would close this without changing the test count.

### Assessment

**Task quality:** Approved

**Reasoning:** Every "Produces" item, every test step and every lang key is present and matches the brief. REFUSED follows the ruling, and the branch sits under the operator-gated `debug` node. The four findings are polish and coverage width, not correctness defects.
