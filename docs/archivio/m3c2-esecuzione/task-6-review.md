# Task 6 review — Caricatore: tramogge e rottura (c6824ef..9619ec6)

### Spec Compliance

- ✅ Spec compliant. Every item of "Produces" has its hunk and matches the brief:
  - `content/ChargerBlockEntity.java:213-367`: `getContainerSize` 2, `isEmpty`, `getItem` (real stacks, `EMPTY` for
    another slot), `removeItem` (:242, slot 0 returns the cell stack itself, slot 1 `dust.split`, `setChanged()` only
    when something left), `removeItemNoUpdate` (:263, no `setChanged`), `setItem` (:284, `setCell`/`setDust`, throws for
    another slot, Javadoc sentence present), `stillValid`, `clearContent` (:299, reserve and carry untouched),
    `canPlaceItem` (:314-319, the brief's exact expressions), `getSlotsForFace`, `canPlaceItemThroughFace` (:333, `null`
    and `DOWN` false), `canTakeItemThroughFace` (:346, `DOWN`, slot 0, charge >= config capacity), `dropContents`
    (:355). `getMaxStackSize()` is not overridden.
  - `content/ChargerBlock.java:132-138`: `onRemove` public, guarded by `!state.is(newState.getBlock())` and by the
    block entity type, `dropContents()` + `updateNeighbourForOutputSignal(pos, this)`, then always `super`; Javadoc
    names the property-change case.
  - `gametest/ChargerGameTests.java:880-1140`: the six tests with the brief's names, positions, delays and
    assertions; helpers `hopper(...)` and `count(...)`; hoppers cleared before removal, chargers removed.
  - `docs/compat.md`: "Uso" line of `CompatBlockEntity`, faces table, the two hopper facts and the consequence, the
    NBT-paragraph sentence, third-party pipes.
- No missing, extra or misunderstood requirement of Important weight. The two unlisted edits to `docs/compat.md`
  (breaking in the `CompatEntityBlock` "Uso" line; "e piu' avanti `onRemove`" -> "`onRemove`") keep the document true
  to the code this task adds: accepted, not scope creep.
- Checks made outside the diff (one per named risk):
  - `dropContents` passes an empty stack when only one slot is filled: `frame/Handouts.java:58-69` returns null for an
    empty stack and adds nothing. Fine.
  - The delayed bodies: `gametest/GameTestKit.java:687-697` `runInSequence` only runs the body and rewraps runtime
    exceptions; three bodies scheduled up front at 20/40/60 do not interfere, and the test ends at tick 60 as the
    brief's "20 + altri 20 + altri 20" intends. Accepted.
  - Test count: `grep -cE '^[[:space:]]*add\(tests,'` over `gametest/*.java` (the rule of `tools/run_gametests.sh`)
    gives 202, the plan's number for Task 6.
  - Implementer concern 1 (`/item replace block`): javap on the mapped 1.20.1 and 1.21.1 jars: `ItemCommands.setBlockItem`
    calls `Container.setItem` with no `canPlaceItem`, and `Commands.performCommand` has a `java.lang.Exception` handler
    on both versions, so the `IllegalArgumentException` of `setItem` ends as "command failed", no crash, nothing lost
    or duplicated. It is the brief's contract; not a defect. The missing test is in T6-F3.
  - Implementer concern 2 (hopper below with no room, two `setChanged` per tick): follows from the vanilla
    take-and-put-back and equals what a chest above a blocked hopper does; the put-back happens inside the hopper's
    own tick, so the `cell` block state never flickers. No finding.
  - Implementer concern 3 (choices): `setChanged()` in `dropContents`/`clearContent` is what design 12.11 says ("ogni
    cambio di contenuto chiama setChanged()"); the extra comparator notification in `onRemove` is harmless.
    `removeItem` with amount < 1 removing nothing is right (`ItemStack.split` with a negative amount would grow the
    stack). Up-front delays: see above. No finding.
- ⚠️ Cannot verify from the repository:
  - Build of the four variants with `collectJar`, `OK sources`, `OK jars`, and the four
    `OK <variante>: 202 test obbligatori passati, nessun avviso fuori elenco` lines are the implementer's reported
    output; not re-run, as the contract says.
  - The mutation runs ("controprova": 3 and 10 failing tests on 1.21.1-neoforge) and the restored-file `cmp` left no
    trace in the repository; the working tree is clean at 9619ec6.
  - Forge 47.4.29 / NeoForge 21.1.266 hopper hooks falling back to the vanilla `WorldlyContainer` were not re-read in
    the patched jars; the reported green hopper tests on those two variants are the evidence.

### Strengths

- The container is small and exact: limits live in `canPlaceItem` and the faces, `setItem` delegates to the two
  existing setters, so the "never cut" rule (ruling in the ledger, Review Focus 3) has one home.
- `fullHopperBelowNeitherLosesNorDuplicatesTheCell` proves the put-back really happened (`c.cell() != before`) before
  asserting count and charge, for both the 10 000 and the 40 000 RE cell: the assertion cannot pass vacuously.
- `onRemove` is guarded on the block, not on the state, and the report's mutation run shows
  `litTogglesTwiceWithoutLosingTheCell` catches the guard's removal.
- `removeItem` guards `amount < 1`, `dropContents` guards `level == null`: both avoid a silent loss or growth.
- `docs/compat.md` explains why `getItem(1)` must not be a copy and why `setItem` must not cut, next to the tests
  that pin them.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T6-F1 (plan-mandated)** `gametest/ChargerGameTests.java:908-931` — in `hopperAtTheSideAddsDustToANonEmptySlot` the
  hopper never tries its cobblestone or its cell. A hopper pushes one item per 8 ticks from the first slot that
  succeeds; slot 0 holds 5 dusts, so the fifth push comes no earlier than tick 32 and the check runs at tick 30. The
  assertions at :922 ("a cell entered from the side") and :923-926 ("the hopper lost its cobblestone or its cell")
  cannot fail for the reason they name; the refusal from a side is pinned only by the direct calls in
  `facesAcceptOnlyWhatTheyShould`. The numbers (5 dusts, 30 ticks, total 8) are the brief's. Fix if wanted: 2 dusts in
  the hopper (total 5): after tick ~9 the hopper retries the stone and the cell at every tick until the check.
- **T6-F2 (plan-mandated)** `content/ChargerBlockEntity.java:317` — on 1.21.1 the dust slot can grow past `MAX_DUST`.
  `canPlaceItem(1, …)` is `stack.is(Items.REDSTONE)` (the brief's text), and `HopperBlockEntity.tryMoveInItem` of
  1.21.1 grows the stack from `getItem(1)` in place up to `stack.getMaxStackSize()` (javap), which is a component
  there: redstone given with `max_stack_size=99` (a command, or a datapack that hands out such stacks) reaches 99 in
  the slot through a side hopper, `writeData` (:427) saves 99 and `readData` (:411) clamps to 64: up to 35 dusts lost
  at the next load. Dust only, never a cell, and not reachable with plain survival items. Fix if wanted: slot 1 also
  asks `dust.getCount() < MAX_DUST` (hoppers and droppers push one item at a time).
- **T6-F3** `content/ChargerBlockEntity.java:242-290`, `:333` — container paths no test touches: `removeItem(1, n)` (the
  `split` branch and the amount < 1 guard: no face lets dust out, so no hopper reaches it), `removeItemNoUpdate`,
  `setItem` for another slot or for a stack the slot cannot hold (the throw the Javadoc promises, and what
  `/item replace block` meets), `canPlaceItemThroughFace(…, null)`. A few assertions inside
  `facesAcceptOnlyWhatTheyShould` would pin them without changing the count of 202.
- **T6-F4 (plan-mandated)** `gametest/ChargerGameTests.java` — the file grew from 1010 to 1316 lines in this task
  (ledger minor T5-F3 continues); the plan keeps every charger test in this class.

### Assessment

**Task quality:** Approved

**Reasoning:** The container, the faces and the breaking path match the brief and the ledger's "never cut" ruling line
by line, and the tests that guard loss and duplication (cell taken and put back, two cells queued, breaking once) assert
real behaviour through real hoppers. The four findings are Minor: one vacuous pair of assertions and one
operator-only dust overflow that both come from the brief's own text, untested side paths of the container, and the
growing test class.
