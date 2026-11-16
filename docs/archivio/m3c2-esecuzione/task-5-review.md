# Task 5 review — Caricatore: il clic (2b7c696..c6824ef)

### Spec Compliance

- ✅ Spec compliant. Checked against the brief, file by file:
  - `content/ChargerBlock.java:159-199` — `onClick` in the brief's order: (1) off hand / spectator / not alive → `PASS` (161); (2) other item → `PASS`, no message (164-169); (3) client → `SUCCESS` (170); (4) no `ChargerBlockEntity` or not a `ServerPlayer` → `PASS` (173-176, plus a `ServerLevel` pattern check that replaces a cast); (5) one action per click: cell (`insertCell`, 209-223: `old = charger.cell()`, `setCell(CellItem.fromCell(((CellItem) held.getItem()).toCell(held)))`, `shrink(1)` unless `instabuild`, old to the main hand if empty else `Handouts.give`), dust (`addDust`, 229-240), sneaking empty hand → nothing, standing empty hand → cell then dust; sounds `CHARGER_INSERT` / `CHARGER_TAKE` at `Vec3.atCenterOf(pos)`, none for dust; (6) status line always, `SUCCESS` if changed, `CONSUME` otherwise. Javadoc has the cases, "una cella sta nel caricatore o in un inventario, mai in tutti e due", the cut at the hand entry and not by hoppers, anyone may take the cell (design §8 q. 11).
  - `frame/FrameSounds.java` — the two values appended to the enum with the events and `SoundSource.BLOCKS` of the plan header; class Javadoc table updated.
  - Language files — three keys after `.charger.no_cell`, same order in both files, texts as in the plan header (checked by loading both JSON files: key lists equal, 85 keys).
  - `gametest/ChargerGameTests.java` — the eight tests with the brief's names, each through `clickBlock` (the spectator case through the neutral `onClick`, as the brief says), each closing with `removeCharger`; `lastArg` helper; `CHARGER_TEXT_KEYS` now holds the four status keys. Static count of `add(tests,` lines in `gametest/*.java`: 196, the plan's number for Task 5.
  - `docs/compat.md` — `CompatEntityBlock` "Uso" line and the "Schema NBT" sentence, both present.
  - No `isCreative()` in `content/Charger*.java`; no non-ASCII character except `§` in the three Java files; no line over 120 columns; checkout is at HEAD c6824ef.
- Design §12.11 "Il clic" and "Due giocatori, un caricatore" read: the brief and the code agree with it. Ruling "cut only by hand" respected: the cut is in `insertCell` via `toCell`, `setCell` untouched.
- Named risks checked in unchanged code (one focused read each):
  - Aliasing between the charger's real stacks and the player's hand (a lost or duplicated cell): `ChargerBlockEntity.setCell` (153-165) replaces the field with a fresh stack and never mutates the old one, `setDust` (174-184) replaces the field; so `old` stays valid after `setCell`, and the stack handed to the player in the take paths is no longer referenced by the charger. No alias, no loss.
  - The click throwing on a malformed cell (negative energy written by command): `PieceTags.readEnergy` (90-92) clamps to 0 before `toCell` builds the `EnergyCell`; `fromCell` gives a stack of one, which `setCell` accepts.
  - Creative with a full inventory voiding the swapped-out cell: `Handouts.give` does not use `Inventory.add`, the rest goes to the ground still.
  - Over-stacked cells in hand: `ModItems.REDSTONE_CELL` is `stacksTo(1)`; with a forced stack the code still shrinks by one and gives the old cell through `Handouts.give`.
- ⚠️ Cannot verify from the repository:
  - Build of the four variants, `OK sources`, `OK jars`, the four `OK <variante>: 196 ...` lines: the implementer's reported output, not re-run (contract).
  - Step 6 (real client on 1.20.1-forge and 1.21.1-fabric): the three screenshots per variant and the empty `log-check.txt` are in the session's temporary folder, not in the repository; the client branch of the click (Review Focus 7) rests on the report's description.

### Strengths

- The conservation argument is visible in the code: each branch removes and adds in the same call, and the three helpers (`insertCell`, `addDust`, `statusLine`) keep `onClick` readable (ChargerBlock.java:159-250).
- Tests pin more than the brief asks without leaving it: exact `SUCCESS` / `CONSUME` so the two results are told apart, `statusMessages` to prove one status per click, `assertOneCell` counting hands, inventories, charger and ground, the check that the creative hand and the charger do not share one stack object (ChargerGameTests.java:803).
- The spectator case is set up so that it fails without the `isSpectator()` check (empty hands, cell in the charger).
- Honest TDD evidence: a second RED on behaviour, with the one test that was already green named and explained.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- T5-F1 — `content/ChargerBlock.java:161` and `:173-176`: the `!player.isAlive()` → `PASS` branch and the "no block entity" guard have no test; removing either keeps the 196 green. One direct `onClick` call with a dead mock player (as done for the spectator) would pin the first.
- T5-F2 — `gametest/ChargerGameTests.java:73` adds the constant `STAND`, but the two older tests at `:137` and `:167` still write the same literal `new Vec3(3.5, 2.0, 5.5)`. Use the constant there too.
- T5-F3 (plan-mandated) — `gametest/ChargerGameTests.java` grew from about 600 to 1010 lines with this task and Task 6 adds six more tests to it; the plan puts all charger tests in this one class. Something for the final review to weigh (a split by theme: click, charge, hoppers), not for this task.

### Assessment

**Task quality:** Approved

**Reasoning:** The click follows the brief and design §12.11 step by step, with no aliasing between the charger's stacks and the player's hand and the capacity cut only at the hand entry; the eight tests exercise the real game click and would fail on each behaviour they name. Only polish and coverage notes remain.
