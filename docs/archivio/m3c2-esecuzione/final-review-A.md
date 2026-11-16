# Final review, area A (the charger, its rule and the data)

Branch `m3c-giocarci`, range `614b25f..9107aae`, plan "Milestone 3c-2 - Ricaricare", tasks 2-8.
Read-only review: nothing was built or run except `javap`/`unzip -p` on jars already in the Gradle caches and
`python3 -I tools/gen_data.py --selftest` (in memory, writes nothing). One pass over the production diff line by line
(`final-diff-A.patch`), the generated data (one file of each kind per version, read in full), and the tests as far as
the Review Focus points and the test-gap question needed (`ChargerGameTests`, `DataGameTests`, `ChargerRuleTest`,
the `GameTestKit` additions).

## Strengths

- **Nothing lost, nothing duplicated holds on every path I could trace.** Hand insert, swap, take
  (`ChargerBlock.java:193-216`, `226-240`), hopper push (`setItem` -> `setCell`, a fresh stack, the hopper drops its
  reference), hopper pull that succeeds (`removeItem` hands over the very stack and empties the slot,
  `ChargerBlockEntity.java:242-259`), hopper pull that fails (1.20.1 puts back a copy made before, 1.21.1 the same
  stack: read with `javap` in `HopperBlockEntity.tryTakeInItemFromSlot` of both jars; both go through `setCell`, one
  stack of one, same charge), break (`onRemove` guarded by `!state.is(newState.getBlock())`, `ChargerBlock.java:132-138`),
  `/setblock` and `/clone` (`clearContent` empties first, so nothing drops twice).
- **No stack shared between two owners.** `setCell` always makes a fresh stack, `removeItem`/`dropContents` clear the
  field after handing the stack over, the creative insert leaves a different object in the hand (tested:
  `creativeHandIsNotConsumed`), and the dust stack is kept by identity only where a hopper needs it.
- **The "never cut a saved charge" rule is implemented once and consistently**: `readData` only floors at 0,
  `setCell`/`setItem` never cut, the rule treats `stored >= capacity` as full and returns it untouched
  (`ChargerRule.java:82-85`), the comparator and the percent clamp only for display. The one cut is
  `CellItem.toCell` in the hand insert (`ChargerBlock.java:228`), as ruled.
- **Loader hooks checked at the source, not assumed.** Forge 47.4.29 and NeoForge 21.1.266 look for an item-handler
  capability first (`VanillaInventoryCodeHooks.getItemHandler` / `getItemHandlerAt`) and fall back to the vanilla
  `Container` path when there is none; NeoForge's `CapabilityHooks` registers providers only for the vanilla
  block-entity types and its fallback covers entities and items, not blocks. So the four variants really do run the
  same vanilla hopper code against the charger. This closes the "cannot verify" line of Task 6 in the ledger.
- **The per-version bases are right for their versions**: `use` (1.20.1) vs `useItemOn` only (1.21.1, called for an
  empty hand too, `PASS` -> `PASS_TO_DEFAULT_BLOCK_INTERACTION` so the held item is still tried); `load`/
  `saveAdditional(tag)` vs `loadAdditional`/`saveAdditional(tag, registries)`, each `final` and calling `super` first;
  `appendHoverText` with `Level` vs `Item.TooltipContext`. No update tag is sent, and nothing on the client reads the
  block entity: what the player sees is the block state.
- **The data are in the format each version loads.** Folder names (plural on 1.20.1, singular on 1.21.1), the recipe
  result (`item` vs `id`), the list form of the item predicate (`"items": [{"items": [...]}]`: `ItemPredicate` reads the
  key `items` on both versions, checked with `javap`), `category` values, loot table, tag with `replace: false` in the
  `minecraft` namespace. `check_data.sh` refuses a missing file, a different file and an extra file in the two trees
  and any data in the common resources; (f3) refuses a folder of the other version in any namespace of a jar.
- **The rule is pure, in `long` where it must be, and its tests recompute conservation over 20 000 ticks at four
  config corners.** `charging` stays true across the tick a dust ends when another dust follows, so `lit` does not
  flicker.
- **Tests go through the real game paths**: the real click (`ServerPlayerGameMode.useItemOn`), real hoppers left to
  tick, the real `destroyBlock` of a survival player, the real `inventory_changed` trigger, on four variants. The
  put-back test proves the cycle ran by stack identity (`c.cell() != before`), which is the right observable.

## Issues

### Critical (Must Fix)

None.

### Important (Should Fix)

None.

### Minor (Nice to Have)

**FA-1. A right button held for four ticks or more repeats the click, and the charger's actions undo each other**
- File: `src/main/java/com/toxineren/exoframes/content/ChargerBlock.java:176-216`
- What: the client repeats a use every 4 ticks while the use key is down (`Minecraft.startUseItem` sets
  `rightClickDelay = 4`, `handleKeybinds` calls it again when the key is still down: read with `javap` in the 1.21.1
  jar). Each charger action leaves the hand in the state that triggers the inverse action: empty hand takes the cell
  -> the hand now holds a cell -> the repeat puts it back; a cell inserted into an empty charger -> the hand is empty
  -> the repeat takes it out; all the dust moved in -> the hand is empty -> the repeat takes the cell (or, with no
  cell, takes the dust back).
- Why it matters: a press of about 0.2 s or longer ends where it started, with two sounds, and the fuelling case ends
  with the cell in the hand and the charger off. Nothing is lost or duplicated. Vanilla has the same pattern (flower
  pot), and the design dictates each single action, so this is a consequence nobody decided, not a deviation. The
  headless script uses an instant click and cannot see it.
- How to fix: a ruling is needed, not necessarily code. Either accept it (vanilla precedent) and add one line to the
  PC checklist ("click, do not hold"), or ignore on the server a second charger click of the same player within 5
  ticks of one that moved something (a small map UUID -> tick, like `CellSwap`'s interval).
- Confidence: likely (the client mechanism is verified; the effect in game was not run).

**FA-2. Two regressions that would hurt saved data and that no test would catch**
- Files: `src/main/java/com/toxineren/exoframes/compat/NbtKeys.java:142-160`;
  `src/main/java/com/toxineren/exoframes/gametest/ChargerGameTests.java:97-99, 445-475`;
  `src/main/java/com/toxineren/exoframes/content/ChargerBlockEntity.java:153, 191, 210, 256, 305, 364`
- What: (a) the five on-disk names (`has_cell`, `cell_energy`, `redstone`, `reserve`, `carry_milli`) are compared in
  the tests only through their own constants: a constant whose string changes passes every test, and every charger
  saved before reads as empty (missing keys are 0 and false, by design). (b) No test observes the effect of
  `setChanged()` (ledger T4-F3): without it a cell put by hand or by hopper into a charger that is not charging is
  not marked for saving and is gone after the chunk unloads, and comparators stop following the charge.
- Why it matters: both are silent losses of saved content; both are one-line regressions.
- How to fix: in `chargerSavesAndLoadsItsContents` assert the saved tag's keys against the five literal strings; add
  one check that after `setCell` the chunk is unsaved (`LevelChunk.isUnsaved()` after `setUnsaved(false)`, same
  signature on both versions).
- Confidence: certain.

**FA-3. Comments made false by this part**
- Files: `src/main/java/com/toxineren/exoframes/content/ModItems.java:24-25` ("Nessuna ricetta in questa parte: si
  prendono dalla scheda creativa": Task 7 gave the four pieces a recipe); `build.gradle.kts:179-181` ("Oggi porta le
  texture trasparenti...": the folder now carries the 16 data files of its version, and the mixin config; ledger
  T7-F5); `tools/check_art.sh:51` (the placeholder list in the comment has no block textures).
- Why it matters: the next plan is written from these files; the first two say the opposite of what is true.
- How to fix: three sentences.
- Confidence: certain.

**FA-4. The 64-dust limit is not in `canPlaceItem`**
- File: `src/main/java/com/toxineren/exoframes/content/ChargerBlockEntity.java:317` (and the clamp at `:411`)
- What: design §12.11 says every limit lives in `canPlaceItem`/`canPlaceItemThroughFace`. The dust slot accepts any
  redstone and relies on the stack's own maximum to stop at 64. On 1.21.1 that maximum is a component: redstone with
  `max_stack_size` above 64 (an operator's item, not obtainable in survival) grows in place past 64 through a side
  hopper, and `readData` cuts the count to 64 at the next load (ledger T6-F2). `setDust` also throws above 64, so a
  caller that sets the merged stack instead of growing it would get an exception instead of a refusal.
- How to fix: `case SLOT_DUST -> stack.is(Items.REDSTONE) && dust.getCount() < MAX_DUST;` (a hopper moves one item
  at a time, so the slot stops at exactly 64; the hand path does not use `canPlaceItem`).
- Confidence: certain for the code; reaching it needs an operator-made item.

## Review Focus, point by point

1. Cell lost or duplicated: traced as in Strengths; no finding.
2. Broken, state change, unload: `onRemove` acts only on a real block change; a `lit`/`cell` write keeps the same
   block entity (vanilla `LevelChunk.setBlockState` keeps it when the block is the same); save and load carry the
   five numbers; pistons cannot move a block with a block entity; an explosion goes through the same `onRemove`.
   No finding.
3. Energy destroyed at load: no path cuts except the hand insert. No finding.
4. Data on one version only: generator, `validate()`, `check_data.sh` and (f3) read line by line; the committed
   files are the generator's. No finding.
5. Config limits: products in `long`, `reserve` clamped on read to the config maximum; `status`, `percent` and
   `comparator` cannot receive a value they refuse (charge floored at 0, config validated positive). No finding.
6. Odd clicks: off hand, other item, spectator, dead player pass before the client/server split, so both sides
   agree; `instabuild` is the only creative test. The one thing found is FA-1.
7. What GameTest does not see: the 16 block-state variants, the three block models, the item model, the five 16x16
   opaque textures and the ten language keys in both files are there and reference each other correctly (checked
   statically). The client branch returns `SUCCESS` without touching anything, so no ghost item is possible; the hand
   is corrected by the normal inventory sync one tick later.

## Declined to judge

- Pipes and inserters of other mods on Forge and NeoForge do not see the charger (no item-handler capability):
  declared out of M3 in `docs/compat.md` ("Tramogge"); the design speaks of hoppers only.
- The cut of a charge above capacity at the hand insert, the loss of a custom name on entry, one dust per tick at
  most: ruled in the ledger; I found nothing the rulings did not consider.
- A crash between a click and the autosave can lose or duplicate a cell: the declared limit of design §7 point 27.
- The reserve of the burning dust is lost when the block is broken: design §12.11.
- The comparator reads 0 both without a cell and with a cell under one fifteenth: the design's formula.
- Dust cannot leave by hopper; by hand only after the cell is out: the design's faces and click order.
- `check_data.sh` compares bytes, and `.gitattributes` fixes line endings only for `*.sh`: a Windows checkout with
  automatic CRLF would fail the check. The same holds for `check_art.sh` since before this part; CI runs on Linux.
- `check_jars.sh` has no check that the charger's block state, models and textures are in the jars: no such check
  exists for any asset of the mod today; a missing one shows only in the real client.

## Deferred minors (tasks 2-8)

| Id | Verdict | Reason |
|---|---|---|
| T2-F1 | leave | `rotate`/`mirror` are the furnace's two lines; a wrong facing after a structure rotation is cosmetic. |
| T2-F2 | obsolete | The opening of `docs/compat.md` now says blocks live in `content/` (Task 10). |
| T3-F1 | leave | The readings cannot receive a refused value from the block entity (charge floored at 0, config validated). |
| T3-F2 | leave | Message wording of two helpers; no behaviour depends on it. |
| T4-F1 | leave | Diagnostic only: on an integrated server a second world would not log a failure at the same position. |
| T4-F2 | leave | The catch cannot be reached with validated config and clamped state; a test would need a hook only for it. |
| T4-F3 | leave | Real gap, described in FA-2 with a cheap test; today every mutator calls `setChanged()`. |
| T5-F1 | leave | Two guard branches that return `PASS` and touch nothing. |
| T5-F2 | leave | A repeated literal in two tests. |
| T5-F3 | leave | File size of a test class; the plan asked for one class. |
| T6-F1 | leave | The side faces' refusals are proved without ticks by `facesAcceptOnlyWhatTheyShould`. |
| T6-F2 | leave | Needs an operator-made item on 1.21.1; the one-line guard is in FA-4. |
| T6-F3 | leave | Paths no vanilla caller of the charger uses (`removeItemNoUpdate`, `removeItem(1, n)`, a null face). |
| T6-F4 | leave | Same as T5-F3. |
| T7-F1 | leave | The unlocks are proved in world on four variants (`aCopperIngotUnlocksTheSevenRecipes`). |
| T7-F2 | leave | Only the exception type of a guard that never fires on generated content. |
| T7-F3 | leave | The seven proved rules are the ones that guard a silent ignore; the others fail loudly in game. |
| T7-F4 | leave | The whole resource folder is packaged; a recipe missing from the (f3) list is still in the jar. |
| T7-F5 | fix | One false sentence in the build script, with the two other leftovers of FA-3. |
| T8-F1 | leave | Twelve duplicated test lines. |
| T8-F2 | leave | Javadoc wording of a test. |
| T8-F3 | leave | A wrong predicate would fail to parse on 1.21.1 (strict optional field) and log an error; `items` is the key both versions read. |

## Recommendations

- Decide FA-1 before the PC checklist is run, so the tester knows whether a bouncing cell is a defect.
- When the fix wave touches `ChargerGameTests`, add the two assertions of FA-2 there rather than a new test, so the
  count of 208 stays.
- For a later part: an asset list in `check_jars.sh` next to (f3), and `*.json text eol=lf` for the generated trees.

## Assessment

**Ready to merge? With fixes.**

**Reasoning:** I found no path that loses or duplicates a cell, dust or stored energy, and no data file a version
would ignore; the per-version branches match their game versions and the loader hooks were confirmed at the source.
What remains is small: three false comments to correct (FA-3), two cheap assertions that protect saved data (FA-2),
an optional one-line guard (FA-4), and one ruling on the held-button behaviour (FA-1).
