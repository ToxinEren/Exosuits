# Task 4 review — Caricatore: contenuto, tick, stato del blocco, salvataggio

BASE 12011825618a536db16f406d26f7383a4ebf2058, HEAD 2b7c6963bd49464e54ec3203f5ee8dbc5416f450 (one commit, four files, exactly the brief's list).

### Spec Compliance

- ✅ Spec compliant.
  - `content/ChargerBlockEntity.java`: fields `cell`/`dust`/`reserve`/`carryMilli` (:65-72), `cell()`/`dust()` return the slot stacks (:135-143), `setCell` builds a fresh stack with the uncut charge, exact exception text, `setChanged()` (:152-164), `setDust` keeps the passed stack (:173-183), `readData` writes fields directly with the four limits and `max(0, cell_energy)` (:227-239), `writeData` always writes the five keys (:243-249), `status`/`chargePercent`/`comparatorOutput` go through `ChargerRule` with the config capacity (:197-209), `serverTick` wraps everything in `try`/`catch (RuntimeException)` with the exact log line and a concurrent static set keyed by `GlobalPos` (:86-95), applies only what changed, `setChanged()` only if changed, block state written once and only if it differs, flag 3 (:97-133).
  - `content/ChargerBlock.java`: `getTicker` null on the client and for another type (:158-165), `hasAnalogOutputSignal` and `getAnalogOutputSignal` declared `public` (:168-179).
  - `gametest/ChargerGameTests.java`: seven `add(tests,` lines (13 in the file, 181 -> 188), the three helpers of the brief (`tick`, `removeCharger`, `redstone`), every new test ends with `removeCharger`; each assertion of Step 1 is present with the brief's numbers (checked line by line against the brief, including the three `setCell` assertions on the 40 000 RE stack and the `Tag.TAG_ANY_NUMERIC` loop).
  - `docs/compat.md`: the "Caricatore" paragraph with five keys, types, read limits and the bold rule sentence; the frame-state `carry_milli` row is untouched; the "Garanzia del caricatore" entry under "Limiti noti e rinvii".
  - Rulings: `setCell` and the load never cut (ruling line 10), a cell becomes a fresh stack on entry (ruling line 11) — both implemented and stated in Javadoc and compat.md.
- Checks outside the diff (one per named risk):
  - Risk: `ChargerRule.tick` throws on arguments the block entity can hold. Checked `core/energy/ChargerRule.java`: it rejects negative `stored`/`dust`/`reserve` and `carryMilli` outside [0, 999]; `readData` clamps all of them and `cellEnergy` is never negative, so only a broken config can throw, and that is caught.
  - Risk: the tests tick a block entity that is not the one in the level. Checked `compat/impl` `loadBlockEntity`: it ends with `level.setBlockEntity(loaded)`, so the loaded entity replaces the level's one, has a level (`setChanged()` works) and is the one `removeCharger` empties.
  - Risk: non-ASCII apostrophes or accents in the three Java files. Grep: only `§`, which the existing code already uses.
  - Risk: "once per position and session" versus the static set. Checked `frame/SuitTick.java:105-122` and `frame/SuitDamage.java:70-80`: their sets are cleared at `PlayerEvent.PLAYER_QUIT` from a `register()` called in the common init; the charger's set has no such hook (finding T4-F1).
- Test and build evidence (four `OK <variante>: 188 ...` lines, `BUILD SUCCESSFUL`, `OK sources`, `OK jars`, RED and mutation run) is taken from the report and not re-run, as the contract says; the report shows no warning or ERROR line.
- ⚠️ Cannot verify from diff: nothing.

### Strengths

- `tick()` keeps the stack as the single source of truth for the charge (reads `cellEnergy(cell)`, writes back only on change) and never touches the block state from the content setters; the class Javadoc explains each of the four decisions the brief asked for, in terms of what would break otherwise.
- `storedEnergy()` avoids asking `ItemStack.EMPTY` for data and gives `writeData` and the three readings one definition of "charge or 0".
- The tests drive the public `serverTick` inside one server tick, so the numbers are exact and the real ticker cannot interfere; one separate test proves the real ticker. Assertion messages print actual and expected values.
- Mutation check done by the implementer on the property that matters most (Review Focus 3): cutting in `readData` makes `chargerLoadNeverThrowsAndNeverCutsACharge` fail.
- Case (c) of the load test builds the bare tag from the four vanilla keys instead of trusting what Forge/NeoForge add to a saved tag.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T4-F1 (plan-mandated)** — `content/ChargerBlockEntity.java:63, :90`: `FAILURES_LOGGED` is never cleared. Plan line 19 and line 102 say "una volta per posizione e sessione"; the code gives "once per position since the JVM started" (its Javadoc says so honestly). On a dedicated server the two are the same. In single-player, a second world opened in the same game run stays silent for a failing charger at the same coordinates and dimension id, and the set keeps the `GlobalPos` of every failed position for the life of the process. It differs from the two sibling sets (`SuitTick`, `SuitDamage`), which are cleared from a `register()` hook. The brief prescribes exactly "un `Set` statico concorrente" and the fix (a `ChargerBlockEntity.register()` that clears the set at server stop, called from `RedstoneExoframes.init()`) is outside this task's file list, so the coordinator rules: add the hook in a later task of this plan, or accept the edge. Consequence if left: one missing ERROR line in a rare single-player case; no gameplay effect.
- **T4-F2 (plan-mandated)** — `content/ChargerBlockEntity.java:86-95`: the catch path (swallow, log once, then silence) has no test at all. The plan forbids provoking the line (`tools/log_allowlist.txt` takes no new entries; "i test non la provocano"), so this is by design; it remains a required behaviour of design §12 proven only by reading. The code is four lines and reads correctly.
- **T4-F3** — `gametest/ChargerGameTests.java` (new tests, :585-811): no test observes the effect of `setChanged()` (design §12.11: "Ogni cambio di contenuto chiama `setChanged()`"). `comparatorReadsTheCharge` calls `state.getAnalogOutputSignal` directly, so removing `setChanged()` from `tick()`, `setCell` or `setDust` would leave all 188 tests green while the chunk is no longer marked for saving and a comparator next to the charger no longer updates. The brief does not ask for it; a later task with a real comparator or `level.getChunkAt(abs).isUnsaved()` after a tick would pin it.

### The implementer's three concerns

1. Static set never cleared: real but small, see T4-F1 (plan-mandated, Minor).
2. `setChanged()` on every charging tick: not a problem. It is what brief and design require; the cost is marking the chunk unsaved (wanted: the charge must reach the disk) and four neighbour lookups in `updateNeighbourForOutputSignal`, which only reach a comparator's `neighborChanged` when a comparator is actually there, and a comparator schedules a tick only when its input changed. No finding.
3. Exact tick count of the real ticker not read: not a problem. The brief's window (95-105 RE, 38-42 ticks) is the requirement and the test asserts exactly that. No finding.

### Assessment

**Task quality:** Approved

**Reasoning:** The diff implements every item of "Produces" and every assertion of Step 1 as written, with the rulings on cutting and on the fresh stack respected and the never-cut property protected by a test the implementer proved by mutation. The three Minor findings are an edge of the log-once set, an untestable-by-plan catch path and one uncovered `setChanged()` effect; none affects cells, charge or saved data.
