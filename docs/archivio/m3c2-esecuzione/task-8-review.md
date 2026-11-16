# Task 8 review — I dati in gioco: ricette, sblocchi, bottino, piccone

Base b5db281cf7d73f5f109b6372dacb9e92cb47b18b, head d7454a271d306209de222f5798b59da179e01789 (one commit, six files, +365 −3).
Paths below are under `src/main/java/com/toxineren/exoframes/` unless they start with `docs/`.

### Spec Compliance

- ✅ Spec compliant. File by file against the brief:
  - `gametest/DataGameTests.java` (new): Javadoc on the per-version data and design §12.12 (:35-45); private table of the seven recipes (:48-55), item, category and rows equal to the plan's "Ricette" table and to design §12.12, one by one; five `add(tests,` lines (:85-89); public no-argument constructor (:79), `@GameTestGenerator` (:82), every test `private static void name(GameTestHelper)` closing with `helper.succeed()`.
    - `everyRecipeIsFoundByKey` (:97-113): present, expected item, count 1 for the seven; for the cell `cellEnergy == 0 && !hasData`.
    - `everyRecipeCraftsFromItsShape` (:119-137): `craft(level, 3, 3, craftingGrid(rows))` for the seven; exoframe grid with slot 1 replaced by gold → empty; nine empty slots → empty.
    - `everyRecipeHasItsUnlockAdvancement` (:143-152): the seven `recipes/<categoria>/<oggetto>`; `!hasAdvancement(recipes/misc/exoframe)`.
    - `aCopperIngotUnlocksTheSevenRecipes` (:158-175): `survivalPlayer`, premise, `add(copper.copy())`, `INVENTORY_CHANGED.trigger(p, p.getInventory(), copper)`, the seven in the recipe book.
    - `chargerNeedsAPickaxeToDropItself` (:183-234): the three tag checks, `hasCorrectToolForDrops` false with an empty hand and true with the wooden pickaxe, first charger at (3, 2, 3) with `setCell(cell(1_000))` and 3 dust broken with `p.gameMode.destroyBlock` → true, air, exactly three drops (one charger, one cell with 1 000 RE, one stack of 3 dust), discarded; second charger at (5, 2, 3), empty hand → true, air, `itemsIn(helper).isEmpty()`.
  - `compat/GameTests.java` (:187-203): the three signatures of the brief, each with "Esiste solo per i test".
  - `compat/impl/GameTestsImpl.java` (:363-416): directives inside the methods; on 1.20.1 the anonymous menu without slots (`quickMoveStack` → `EMPTY`, `stillValid` → false), `TransientCraftingContainer`, `getAdvancement`; on 1.21.1 `CraftingInput.of`, `holder.value()`, `get`.
  - `gametest/GameTestKit.java` (:808-840): `craftingGrid(String... rows)`, nine stacks, the six letters, another character → `IllegalArgumentException`.
  - `RedstoneExoframes.java` (:99): `Compat.gameTests().register(DataGameTests.class);`.
  - `docs/compat.md`: the three helpers in "A cosa serve" (:85) and in "Condizione" (:87) with the per-version types; table row `CustomRecipe / CraftingContainer → CraftingInput` still "—" with the note on `GameTests.craft`.
- Extras, all small and inside the brief's intent, accepted: a control that a key with no recipe gives empty (DataGameTests :110-111); the premise of the unlock test looks at all seven recipes, a superset of the brief's one (:160-163); a premise that slot 1 of the exoframe grid is copper before the swap (:128-129); `craft` and `craftingGrid` reject a malformed grid (GameTestsImpl :380-383, GameTestKit :816-824).
- Checks I made in unchanged code (one per named risk):
  - Count of the lines `run_gametests.sh` counts (`grep -cE '^[[:space:]]*add\(tests,'` over `gametest/*.java`): 207, the plan's number for Task 8. Working tree clean at head.
  - Risk "the unlock premise is vacuous or polluted by another test": `GameTestsImpl.mockServerPlayer` (:120) gives every mock player a random UUID, so the recipe book is new.
  - Risk "a discarded drop is still counted by the second break": `GameTestKit.itemsIn` (:796) reads `getEntitiesOfClass` on the structure box; the second charger sits in the same box and the test discards the three drops first (:222).
  - Stonecutter directives only in `compat/impl/GameTestsImpl.java`; `DataGameTests` and `GameTestKit` do not name `compat.impl` or `net.minecraft.client`.
  - Non-ASCII in the new Java lines: only `§`, as the other `gametest/` classes already use.
- ⚠️ Cannot verify from the repository: see the list at the end.

### Strengths

- The recipe table is written by hand from the design and not read from the generated files, and the class Javadoc says why (:43-44): a wrong file cannot confirm itself.
- Each test has a control that can fail: gold for copper, empty grid, `recipes/misc/exoframe`, the missing key, stone in the pickaxe tag, the second break with an empty hand.
- The drop check is exact (`dropped.size() == 3` plus one of each kind, :219-221), so a duplicated cell or a second charger item fails it; this serves "nulla si perde, nulla si duplica" also on the `destroyBlock` path.
- Version differences stay inside three short methods of `GameTestsImpl`; the test class has no directive.
- `docs/compat.md` says the per-version types for each helper, as the brief asks, and the reason the table row stays "—".

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T8-F1** `gametest/DataGameTests.java:240-245` and `:58`: the private helper `charger(helper, relative)` is a verbatim copy of `ChargerGameTests.charger` (`gametest/ChargerGameTests.java:1145-1150`), and `STAND` repeats `ChargerGameTests.STAND` (`:83`). Two copies of a fixture drift apart. Fix: move both to `GameTestKit` (touches `ChargerGameTests`, outside this task's file list, so it fits the final review).
- **T8-F2** `gametest/DataGameTests.java:178-179`: the Javadoc of `chargerNeedsAPickaxeToDropItself` says the charger is in the pickaxe tag "e in nessun altro", but the test looks at one other tag only (`MINEABLE_WITH_AXE`, :189). Design §12.12 does say "e nessun altro"; the comment claims a proof the test does not give. Fix: either word it as "non in quello dell'ascia", or add `MINEABLE_WITH_SHOVEL` and `MINEABLE_WITH_HOE`.
- **T8-F3** `gametest/DataGameTests.java:158-175`: `aCopperIngotUnlocksTheSevenRecipes` has no control that another item leaves the recipes locked. With ledger minor T7-F1 (`validate()` does not look inside an unlock), an unlock file whose `inventory_changed` criterion had no item condition would pass every check. Fix: before the copper, trigger `INVENTORY_CHANGED` with an iron ingot and assert that the seven are still unknown.

### Assessment

**Task quality:** Approved

**Reasoning:** The five tests, the three helpers, `craftingGrid`, the registration and the documentation match the brief line by line, and the tests ask the running server for what it loaded, with controls that can fail. The three Minor items are a duplicated fixture, one over-stated comment and one missing negative control.

### Cannot verify from the repository

1. Build of the four variants with `collectJar`, `OK sources`, `OK jars`, `OK data` and the four lines `OK <variante>: 207 test obbligatori passati, nessun avviso fuori elenco` are the implementer's reported output: not re-run, as the contract says. The static count (207) and the clean tree at head agree with it.
2. The RED run (nine "cannot find symbol" errors) and the two mutation runs on 1.21.1-neoforge (the four data folders renamed → the five new tests fail; only `loot_table` renamed → the pickaxe test fails) left no trace in the repository. They are the only evidence that the tests see the data folders.
3. The game signatures the helpers rely on (`RecipeManager.byKey`, `getRecipeFor`, `assemble`, `TransientCraftingContainer`, `ServerAdvancementManager.get` / `getAdvancement`) were not re-read with `javap`; the reported four-variant build is the evidence.
