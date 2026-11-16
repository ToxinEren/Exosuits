# Task 3 — La regola di carica (`ChargerRule`): review

Base b5ac1b5, head 1201182 (one commit, two new files, 358 lines). Diff read once; no changed file re-read.

### Spec Compliance

- ✅ Spec compliant.
  - Files: `core/energy/ChargerRule.java` and `src/test/.../core/energy/ChargerRuleTest.java`, both new, nothing else touched (diff stat).
  - Signatures of "Nomi, numeri e testi comuni": `final` class, private constructor (ChargerRule.java:23, 27), `Status` with the four values in order (31-40), `Result` record with the five components (52), `tick` (70-71), `status` (111), `percent` (133), `comparator` (143): all `public static`, names and parameter order as written.
  - `tick`, the six points of the brief in order: argument checks with name and value (72-80), full cell `Result(stored, dust, reserve, 0, false)` (82-85), one dust at most (86-90), empty reserve (91-93), `long budget = carryMilli + rePerSecond * 1000L / TICKS_PER_SECOND`, `whole`, `rest`, `moved = min(whole, reserve, capacity - stored)` (95-99), `Result(stored + moved, dust, reserve - moved, moved == whole ? rest : 0, true)` (101).
  - `status` precedence NO_CELL, FULL, NO_REDSTONE, CHARGING (116-125); `percent` and `comparator` in `long`, rounded down, `min(stored, capacity)` (133-153).
  - Class Javadoc: the rule in words, the default numbers, the declared consequence (the ruling on one dust per tick: one sentence, 19-21), the pointer to design §12.11 (4). Italian with ASCII apostrophes; exception messages in English.
  - Tests: the 13 of the brief are all there with the exact values (ChargerRuleTest.java:30-180), compared one by one against the brief.
  - Design §12.11 "Il modello, come una fornace" read: the code is that algorithm; no conflict between plan and design.
- Extra (Minor, T3-F1 below): the three readings validate their arguments; the brief lists the checks for `tick` only.
- ⚠️ Cannot verify from diff: the build of the four variants, `OK sources`, `OK jars` and the four `181 test obbligatori passati` lines are the implementer's reported output; not re-run, as the contract says.

Checks run outside the diff (one per named risk):
- Risk: a wrong expected value copied from the brief into both code and test. Checked: an independent model of the six points (Python, scratch only) gives `(9997, 0, 3, 500, true)` after 3 999 ticks, then `(10000, 0, 0, 0, true)`, then `charging` false, and the conservation invariant holds on the four config pairs for 20 000 ticks; the three values of `limitsOfTheConfigNeverOverflow` and the two extra tests recomputed by hand.
- Risk: the readings (`comparator`, `status`, `percent`) throw outside the ticker's `catch` (Task 4 calls `comparatorOutput()` from `getAnalogOutputSignal`, Task 5 from the click). Checked: `ItemDataAccessImpl.cellEnergy` → `PieceTags.readEnergy` is `Math.max(0, ...)` (compat/impl/PieceTags.java:91), and the contract of `ItemDataAccess` states "sempre >= 0" (compat/ItemDataAccess.java:31); capacity comes from the config (> 0 by `ConfigValidation`), `dust.getCount()` is never negative, `reserve` is limited at read (plan, NBT section). The throws cannot fire from the callers the plan describes.
- Style of `CellSwapRule` (the brief's "Consumes"): `final`, private constructor, `IllegalArgumentException` with the value in the message. Matches.
- No caller yet: `ChargerRule` is named only in its own file under `src/main/java`.

### Strengths

- The implementation is a line-for-line transcription of the brief's algorithm, with the two overflow-sensitive points commented where they matter (`rePerSecond * 1000L` at 94-95; the cast of `moved` justified at 98-99). `whole` stays `long` until the `min`, so the rule is safe for any valid `int`, not only inside the config limits.
- `invalidArgumentsAreRefused` is a real test: for each of the eight cases the call would return a `Result` without its check (verified case by case), so a missing check fails the test.
- `chargingStaysTrueOnTheTickTheReserveEnds` pins a sentence of the brief that none of the 13 listed tests fixed on its own (`lit` does not blink between one dust and the next).
- `energyIsConserved` labels every assertion with config and tick number, so a failure names its tick.
- The Javadoc sentence "il riporto e' ritmo, non energia" explains why dropping the carry does not break conservation: the one thing a reader of the rule would otherwise ask.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T3-F1** — `ChargerRule.java:112-115` (`status`), `:144-146` (`comparator`), `:150-153` (`scaled`, used by `percent`): the readings throw `IllegalArgumentException` on `capacity <= 0` or negative `stored`/`dust`/`reserve`, also when `hasCell` is false. The brief lists argument checks for `tick` only, and says `comparator` is 0 and `status` is `NO_CELL` without a cell, with no condition. These three are the methods that Task 4 and 5 call outside the ticker's `catch` (comparator read, click). Checked that today's callers cannot pass such values (see above), so nothing can fire; the cost is only that a future caller that reads a raw number gets a server crash instead of a clamped reading. Either leave it and keep the callers' clamps (Task 4's `chargerLoadNeverThrowsAndNeverCutsACharge` already exercises `cell_energy` −5), or make the readings total (clamp `stored` at 0, return 0 / `NO_CELL` before any check when there is no cell). A coordinator's choice; the implementer raised it in the report.
- **T3-F2** — `ChargerRuleTest.java:122-134`: the brief asks that the exception "nomina l'argomento e il valore"; the message is pinned for `carryMilli` only (133-134). The two helpers `requirePositive` / `requireNonNegative` (ChargerRule.java:155-165) produce the other seven messages and none is asserted. One more `assertEquals` on a `requirePositive` message (for example `"capacity must be > 0: 0"`) would cover both shapes.

### Assessment

**Task quality:** Approved

**Reasoning:** The rule is the brief's algorithm exactly, every expected value of the brief is asserted and was recomputed independently, and the overflow and conservation properties of Review Focus 5 are covered by tests that would fail without the code. The two Minor points are a defensible extra (validated readings) and one unpinned message shape.
