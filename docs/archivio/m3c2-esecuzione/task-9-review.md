# Task 9 review — La partita dall'inizio alla fine, in sopravvivenza

Base d7454a2, head dd696e0 (one commit, three files). Paths under `src/main/java/com/toxineren/exoframes/`.

### Spec Compliance

- ✅ Spec compliant. File by file:
  - `gametest/SurvivalGameTests.java` (new, 596 lines): one test, `survivalFromCraftingToRecharging`, one generator
    line (:119), public no-arg constructor (:113), closes with `helper.succeed()` (:160). Class Javadoc names design
    §12.15, the nine steps and their numbers and the ban on `setStored`, `wearScrap`, `DebugWear` (:43-73); none of
    the three is called.
  - `RedstoneExoframes.java`: import and `Compat.gameTests().register(SurvivalGameTests.class)` after `DataGameTests`.
  - `gametest/GameTestKit.java`: only the class comment (the list of the test classes).
  - No mod code changed (`Produces: ... Nessun codice della mod`).
- Preparation (:168-190): stone floor x,z 1..6 at y 0; `survivalPlayer` at (1.5, 1.0, 1.5); raw materials with
  `setItem(9 + i, …)` in slots 9-13 (34 iron, 14 copper, 2 redstone blocks, 12 dust, 2 quartz), never `add`; premise
  `selected == 0` and empty main hand.
- Step 1 (:197-233): ten crafts with the seven shapes of the plan's "Ricette" table (checked letter by letter; the
  sums are 34 I, 14 C, 2 R, 4 r, 2 Q); 0/0/0/0 and 8 dust; both cells `energy == 0`; main hand empty.
- Step 2 (:240-259): crafted frame item removed, hand asserted empty, `useExoframeItem(helper, p, (2,0,3), true)`,
  torso click → `NO_CELL == 1`, `wornFrameId(p).isEmpty()`.
- Step 3 (:266-286 with `chargeACell` :525-555): charger placed by `clickBlock` on (6,0,6), block entity at (6,1,6),
  hand empty; cell A 2 dust / 400 ticks / 1 000 RE, cell B 4 dust / 800 ticks / 2 000 RE; `STATUS_NO_REDSTONE` counted
  at each cell insert; no dust, reserve 0; empty-hand click gives the cell; `stash`; 2 dust left.
- Step 4 (:293-317): six `hold` + click at the brief's points, `pieces().size() == 6`; `holdCell(1_000)` + torso click
  → `cell().stored() == 1_000`; empty-hand click → worn, `KEYS_HINT == 1`.
- Step 5 (:324-343): `t0 = now`, preparation tick, twenty 0.2 steps along x with `SuitTick.tick(p, t0 + i)` →
  `stored == 992`, `ENERGY_LOW == 1`.
- Step 6 (:349-360): cactus 8.0F true; helmet 179, torso 178, arms 179, legs 179; health below max.
- Step 7 (:366-380): `CellSwap.handle(p, t0 + 30)` → 2 000; one cell in the inventory with 992; tick at `t0 + 31` →
  2 000 and `ENERGY_LOW` still 1.
- Step 8 (:387-405): `FrameEntry.exit(p)` true; placed record at (6.5, 1.0, 3.5) with the cell at 2 000; third charge
  992 → 1 992 with the last 2 dust; cell in hand.
- Step 9 (:412-444): one cell in the main inventory, the one in hand, none in the off hand, `charger.cell().isEmpty()`;
  0 dust in inventory and charger, reserve 0; 3 992 RE = 8 × 500 − 8; `itemsIn(helper).isEmpty()`.
- Cleanup (:149-154): `removeFrame`, charger emptied then air (plus `clearContent` on the mock player).
- Private helpers with the brief's signatures: `craftFromInventory` (:452), `hold` (:475), `holdCell` (:487), `stash`
  (:500), `tick` (:510). The failure message names the step (`atStep`, :591; negative proof in the report shows
  "step 1 (crafting): missing ingredient ...").
- Extra, all inside the task's scope and none a new feature: helpers `chargeACell`, `mount`, `clickAt`,
  `slotOutsideTheHand`, `atStep`; assertions beyond the brief (empty inventory premise, the ten crafted items counted,
  frame position, charger `status()`, pieces still six after the exit, `carryMilli() == 0`, a single stack left in the
  whole inventory). They tighten the test; not a finding.

Focused checks in unchanged code (named risk → what I read):
- "`Handouts.give` could change the stack handed in, or fill the hand first, so `craftFromInventory` would return a
  drained stack or break the empty-hand rule" → `frame/Handouts.java:34-49`: works on `stack.copy()`, the hand slot is
  the last choice. Holds.
- "The class Javadoc says swap and exit are called as the packets call them" → `frame/CellSwap.java:50`
  (`handle(player, player.server.getTickCount())`) and `frame/FrameEntry.java:243` (`exit(player)`); `now(helper)` is
  `server.getTickCount()` (`GameTestKit.java:718`). Holds.
- "`energy(stack)` in eagerly built failure messages could throw on an empty or foreign stack" →
  `compat/impl/ItemDataAccessImpl.java:32-35`: 0 without a payload. Holds.
- "`PieceSlot.values()` could be more or fewer than the six slots of step 6" → `core/armor/PieceSlot.java:4-10`: six.
- "The stone floor left behind breaks the cleanup rule" → `GameTestKit.wearFrameAt` (:203-204) and `rememberSafeSpot`
  leave their stone too; the plan's cleanup list names chargers, hoppers, dropped items, frames, players. Not a gap.
- Count of the lines `run_gametests.sh` counts at HEAD dd696e0: 208, the plan's number for Task 9; working tree clean.

- ⚠️ Cannot verify from the repository:
  - Build of the four variants with `collectJar`, `OK sources`, `OK jars`, `OK data` and the four lines
    `OK <variante>: 208 test obbligatori passati, nessun avviso fuori elenco` are the implementer's reported output:
    not re-run, as the contract says.
  - The negative proof (33 iron ingots → failure at "step 1 (crafting): missing ingredient", then the file restored
    and compared with `cmp`) left no trace in the repository; it is the only evidence that the step name reaches the
    log.

### Strengths

- The test is one method per step with a step label carried into every failure message, including failures thrown by
  `GameTestKit` helpers and non-assertion exceptions (:128-159). The first negative run showed the label was lost
  (`Util.describeError` prints the deepest cause) and the implementer fixed it before the commit and documented why
  `atStep` sets no cause (:587-594).
- The empty-hand rule is enforced, not assumed: `hold` and `holdCell` assert the hand empty on entry (:476, :488),
  every helper searches outside the selected slot (`slotOutsideTheHand`, :577-585), and the only place a stack is born
  in hand (`useExoframeItem`) is preceded by the removal of the crafted item and the assertion (:241-246).
- Every assertion is an equality on a number of the brief with a message that prints the observed values; the final
  count checks the whole container (armor and off hand included), which is what Review Focus 1 asks of Task 9.
- `chargeACell` removes a three-times repetition (cells A and B, the 992 RE cell) without hiding the per-click checks.
- Named constants for every position and click point, with Javadoc; no magic coordinates in the steps.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- T9-F1 (plan-mandated) `gametest/SurvivalGameTests.java:510-516`: `tick(helper, charger, times)` is a verbatim copy
  of the private helper `ChargerGameTests.tick` (`gametest/ChargerGameTests.java:1184-1190`), Javadoc included. The
  brief lists it among the private helpers of this class. Six lines; the fix is one package-private helper in
  `GameTestKit`. Same nature as the deferred T8-F1.
- T9-F2 (plan-mandated) `gametest/SurvivalGameTests.java:198-207`: the seven recipe shapes are written a third time
  (after `tools/gen_data.py` and the private `DataGameTests.RECIPES`, `gametest/DataGameTests.java:48-55`). The brief
  wants the rows of "Ricette" in the test. A changed recipe fails loudly in both tests, so the cost is three edits,
  not a silent drift.
- T9-F3 `gametest/SurvivalGameTests.java:157-158, 591-595`: for an unexpected `RuntimeException`, `atStep` keeps the
  top-level `toString()` and stack trace and drops the cause chain. A failure that the mod wraps (an exception with a
  cause) reaches the log without its root message and without the root trace. Appending the deepest cause's
  description to the message would keep both the step and the root.
- T9-F4 `gametest/SurvivalGameTests.java:62-63`: the class Javadoc says "Tutto cio' che il giocatore tiene nasce da
  una ricetta", but the frame item used in step 2 is a new stack made by `useExoframeItem`; the crafted one is taken
  out of the inventory and dropped from the count (:241-248, as the brief prescribes, and the step's own Javadoc says
  it at :236-238). The class sentence overstates; one clause fixes it.

### Assessment

**Task quality:** Approved

**Reasoning:** The diff is the brief's nine steps with the brief's numbers, helpers and cleanup, registered and counted
(208), with no mod code touched and no debug shortcut; the checks I made in unchanged code confirm the assumptions the
test rests on. Four Minor notes, two of them mandated by the brief's own text.
