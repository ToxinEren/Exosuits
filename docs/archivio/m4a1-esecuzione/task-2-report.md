# Task 2 report: il blocco 2x1 della stazione

**Status:** DONE_WITH_CONCERNS (all work done and verified; the concerns are below)
**Commit:** `dc503e4` content: la stazione 2x1 (posa, rottura dalla metà laterale, bottino dalla principale, pistoni) con ricetta e segnaposto; compat: CompatBlock.onPlayerWillDestroy (SSH-signed like the previous commits, with the two trailer lines).

Evidence folder (outside the repository): `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t2/`

## What I implemented

- **The compat hook** (`compat/impl/BlockBase.java`, Javadoc in `compat/CompatBlock.java`):
  - the neutral `public void onPlayerWillDestroy(Level, BlockPos, BlockState, Player)`, empty by default;
  - two version overrides in the existing `//? if >=1.21 { … //?} else { /* … *///?}` form: on 1.21.1 `public BlockState playerWillDestroy(...)`, which calls the hook and then returns `super`; on 1.20.1 `public void playerWillDestroy(...)`, which calls the hook and then `super`;
  - checked in Stonecutter's generated 1.20.1-forge source: the `void` branch is the active one there.
- **`content/StationPart`**: an enum `MAIN("main")`, `SIDE("side")` implementing `StringRepresentable`.
- **`content/StationBlock`**:
  - `FACING` and `PART`; the default state is north and `MAIN`.
  - `otherHalf`: from the main, `FACING.getCounterClockWise()`; from the side, `FACING.getClockWise()`. `mainPos` builds on it.
  - `getStateForPlacement`:
    - `FACING` is the opposite of the player's horizontal direction.
    - It returns `null` if the side position fails `canBeReplaced(context)`, `isWithinBounds`, or `isUnobstructed(sideState, sidePos, CollisionContext.of(player) / empty())`.
  - `setPlacedBy`, on the server, does what the bed does:
    - places the side with flags 3 (`Block.UPDATE_ALL`);
    - calls `blockUpdated(pos, Blocks.AIR)`;
    - calls `updateNeighbourShapes(level, pos, 3)`.
  - `updateShape` is public. It returns air only when `direction` is the other half's direction and the neighbour there is not the same station (same block, the other `PART`, the same `FACING`).
  - `onPlayerWillDestroy` acts only on the server, and only on the side half, when the player has `instabuild` or `!hasCorrectToolForDrops(state)`. It then removes the main of the same station:
    - with flags 35 (`UPDATE_ALL | UPDATE_SUPPRESS_DROPS`);
    - with `levelEvent(player, 2001, …)` (`LevelEvent.PARTICLES_DESTROY_BLOCK`).
    - I checked with javap that the named constants equal 3, 32 and 2001 on both versions.
  - `rotate` and `mirror` change `FACING` only. The Javadoc says that mirroring breaks the station, and that this is accepted.
  - `createBlockStateDefinition`.
  - `onClick` opens `new StationOpening(mainPos(pos, state))`.
- **`content/StationItem`**: `TOOLTIP_KEYS` (`tooltip.redstone_exoframes.station.where`, `.open`) and the two grey lines. Both language files get the keys after `charger.hoppers`, with the texts from "Testi".
- **Assets**:
  - `blockstates/station.json`: eight variants `facing`×`part`, with `y` 0/90/180/270 as in `charger.json`.
  - `models/block/station_side.json`: parent `orientable`, top `station_top`, front `station_front_side`, side `station_side`.
  - `tools/gen_placeholders.py`:
    - draws `station_front_side.png`, an opaque panel with a three-bar metal grille and the same copper drawer;
    - factors the shared frame and drawer out of `station_front_main`. Every existing PNG stayed byte-identical (`git status` showed only the new file), and `check_art` reports `OK art`.
- **Data** (`tools/gen_data.py`):
  - `T` = `minecraft:crafting_table`;
  - the recipe `("station", "misc", ("CCC", "ITI", "IrI"))`;
  - a `BLOCKS` table (`charger` with no condition, `station` with `{"part": "main"}`). It builds one loot table per block, with the `block_state_property` condition only on the station's entry and `survives_explosion` on the pool. The pickaxe tag is built from the same table: `[charger, station]`.
  - The charger tables are byte-identical (the `git diff --stat` on both files is empty).
  - `_validate_loot_table(path, file_id, table)` checks `block` against the table's block (`file_id.replace(":blocks/", ":")`) and requires `properties` to be a non-empty object of strings. Both docstrings say so.
  - Two new `DEFECTS`, which brings the selftest to `OK selftest (9 difetti segnalati)`.
  - 38 data files.
- **Tests and kit**:
  - `GameTestKit`:
    - `placeStation(helper, relativeMain, facing)` uses `helper.setBlock` (main, then side at `relativeMain.relative(facing.getCounterClockWise())`) and returns the side's relative position;
    - `stationsOnTheGround(helper)`;
    - the letter `T` in `craftingGrid`, with its Javadoc;
    - the class Javadoc lists the new class.
  - `StationMenuGameTests`: both tests place the station with `placeStation(helper, MAIN, Direction.NORTH)`, where `MAIN` = (3, 1, 3). `closeStation(helper, p, side)` removes the main first and then the side, still from each test's `finally`. The known positive-control gap in `placeholderButtonIgnoresSpectatorsAndTheDead` is untouched.
  - `StationBlockGameTests` holds the ten tests of the brief, registered after `StationMenuGameTests` in `RedstoneExoframes.init()`:
    - `placingTheStationSetsBothHalves`
    - `placementNeedsRoomForTheSideHalf`
    - `pickaxeOnTheSideHalfDropsOneStation`
    - `pickaxeOnTheMainHalfDropsOneStation`
    - `noPickaxeDropsNothing`
    - `creativeBreakingDropsNothing`
    - `onlyTheMainHalfHasLoot`
    - `pistonsCannotMoveTheStation`
    - `removingOneHalfByCommandTakesTheOther`
    - `adjacentStationsStayIndependent`
  - Details of the new tests:
    - Cleanups always remove the main half first. The piston test removes the power, then the pistons, then the station, in a `finally` inside the delayed body, which goes through `runInSequence`.
    - Premises are asserted:
      - the test border puts the main inside and the side outside;
      - both pistons have a neighbour signal;
      - an empty hand is not a correct tool;
      - the creative player has `instabuild`.
    - The second fake player that stands in the way gets `setGameMode(SURVIVAL)` explicitly, because the entity check skips spectators.
    - The border is narrowed and restored in a `finally` within one tick, like `worldBorderKeepsFramesInside`.
    - Items are counted with `stationsOnTheGround` and discarded right after the count. In `adjacentStationsStayIndependent` they are discarded after the final count of 2.
  - `DataGameTests`:
    - the station `RecipeCase`;
    - "sette" becomes "otto" in the Javadocs;
    - `aCopperIngotUnlocksTheSevenRecipes` becomes `aCopperIngotUnlocksEveryRecipe` (name and `add(tests,` line; the count is unchanged).
- **`tools/check_jars.sh` (f3)**:
  - `station:misc` in the recipe loop;
  - `$data/$loot_tables/blocks/station.json` among the required files;
  - the comment says "le otto ricette, i loro otto sblocchi, le tabelle di bottino del caricatore e della stazione".
- **`tools/client/m4a1_shots.sh`**:
  - phases "ricetta" and "rottura", and "posa" with the item instead of `setblock`, with exactly the steps of the phase table;
  - the head comment describes the phases and what they don't prove, with the measured duration;
  - `shots=(stazione-posata stazione-aperta premuto)`, which follows the `m3d_shots.sh` convention of listing every shot.
- **Documents**:
  - `docs/compat.md`, all in the CompatBlock section and the API table, not in the GuiEntityScale section:
    - in CompatBlock:
      - `onPlayerWillDestroy`, with both signatures, the call before `super` and its use by the station;
      - the per-loader call order, a javap finding: Forge calls the hook after `mineBlock`, see concern 1;
      - the rule of the public `updateShape` override;
      - the tests that prove it (`StationBlockGameTests`, by name);
      - a "Uso" paragraph rewritten for the 2x1 block.
    - two new API-table rows:
      - `Block.playerWillDestroy`, `void` → `BlockState`: `CompatBlock`;
      - `updateShape`, `public` → `protected`: declared public, no seam.
    - "sette ricette" becomes "otto" in the GameTests section and the CraftingInput row;
    - the "Cartelle dati" row now names the station too.
  - `tools/README.md`:
    - placeholders: "ventitré" in both places and the new file;
    - data:
      - 8 recipes and 8 unlocks;
      - the loot of the charger and of the station;
      - "38 file, diciannove per albero";
      - `BLOCKS` and `T`;
      - the new validate rule and the two new selftest defects;
      - `9 difetti`;
      - "diciannove" for f3.
  - Checked: `grep -c 'sette ricette' docs/compat.md tools/README.md` gives `0` for both.

## Tests and results

### TDD evidence

- **Step 1 RED (data):** `python3 -I tools/gen_data.py --selftest` was run after adding the data and the two defects, before the validation (`step1-selftest-red.txt`). It failed for the reason I expected: the validation didn't exist yet, so exactly the two new defects went unreported.
  ```
  FAIL: la condizione della meta' che nomina un altro blocco: difetto non segnalato
  FAIL: la condizione della meta' con properties vuote: difetto non segnalato
  exit 1
  ```
- **Step 2 GREEN (data)** (`step2-data-green.txt`):
  - `python3 -I tools/gen_data.py --selftest` → `OK selftest (9 difetti segnalati)`
  - `python3 -I tools/gen_data.py` → `38 file di dati scritti sotto /home/user/exosuits/src/main`
  - `bash tools/check_data.sh` → `OK data`
  - `git diff --stat` on the two `charger.json` loot tables → no lines.
- **Step 3 RED (block):** `./gradlew --console=plain :1.21.1-neoforge:compileJava` was run with only the ten tests written (`step3-compile-red.txt`). It ended in `BUILD FAILED` with `cannot find symbol` for:
  - `placeStation(GameTestHelper,BlockPos,Direction)`
  - `stationsOnTheGround(GameTestHelper)`
  - `variable PART`, `variable FACING`
  - `class StationPart` / `variable StationPart`

  This was expected, because none of these existed yet.
- **Step 4 GREEN (block):**
  - `compileJava` → `BUILD SUCCESSFUL`.
  - `bash tools/run_gametests.sh 1.21.1-neoforge` → `OK 1.21.1-neoforge: 220 test obbligatori passati, nessun avviso fuori elenco` (`gametests-neoforge-1.txt`).

### Mutation checks (are the new tests vacuous?)

All three runs used `bash tools/run_gametests.sh 1.21.1-neoforge` with temporary edits, restored afterwards; `cmp` against the backup and a clean `git diff` confirmed the restore.

1. Three mutations together (`gametests-mutation.txt`): `onPlayerWillDestroy` made a no-op, the placement room checks disabled, and `PushReaction.NORMAL`. Exactly 4 tests failed:
   - `placement_needs_room_for_the_side_half` ("a stone where the side half goes: the click reported CONSUME");
   - `no_pickaxe_drops_nothing` ("1 stations on the ground, expected 0");
   - `creative_breaking_drops_nothing` (same message);
   - `pistons_cannot_move_the_station` ("piston … extended=true").
2. Only the `isUnobstructed` check disabled → 1 failure: "a player standing where the side half goes: the click reported CONSUME" (`gametests-mutation-unobstructed.txt`).
3. Only the border check disabled → 1 failure: "the side half outside the world border: the click reported CONSUME" (`gametests-mutation-border.txt`).

### Step 5 and Step 8: end-of-task commands

The final runs came after the last source edit (`step8-build.txt`, `step8-checks.txt`, `step8-gametests.txt`):

| Command | Result |
|---|---|
| `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` | `BUILD SUCCESSFUL`; the four `:test` tasks ran |
| JUnit count | `1.20.1-forge 331`, `1.20.1-fabric 331`, `1.21.1-fabric 331`, `1.21.1-neoforge 331` |
| `bash tools/check_sources.sh` | `OK sources` |
| `bash tools/check_jars.sh` | `OK jars` |
| `bash tools/check_data.sh` | `OK data` |
| `bash tools/check_art.sh` | `OK art` |
| `bash tools/client/arm_measure.sh --selftest` | `OK arm_measure` |
| `bash tools/client/client_shot.sh --selftest` | `OK passi` |
| `bash tools/run_gametests.sh` | four lines `OK <variante>: 220 test obbligatori passati, nessun avviso fuori elenco` |

The Step 5 run before the docs edits had identical results.

- **The seven mod warnings:** the 1.21.1-neoforge game-test log has seven WARN/ERROR lines that mention the mod (`grep -iE 'WARN|ERROR' … | grep -iE 'station|loot|redstone_exoframes'`; the same count, 7, on all four variants). In the one I listed, all are the existing, allow-listed frame-registry warnings; none mention the station or a loot table.
- **javac:** it prints the existing "Some input files use or override a deprecated API" note. With `-Xlint:deprecation` on 1.21.1-neoforge, the new code adds:
  - `Player.hasCorrectToolForDrops(BlockState)` in `StationBlock` and `StationBlockGameTests`. NeoForge deprecates this one-argument form; the brief prescribes it and `DataGameTests` already uses it;
  - `BlockState.rotate` in `StationBlock.mirror`, the same as `ChargerBlock`.

### Step 6: the real client, 1.21.1-neoforge only

`EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh 1.21.1-neoforge /tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t2/shots-neoforge`, run after `./gradlew --stop` (output in `client-neoforge.txt`):

```
ok anteprima: ritaglio magenta=1193 riquadro=41x85+604+152; schermata magenta=1193 riquadro=41x85+604+152
ok orientamento: meta' destra del ritaglio magenta=0 riquadro=0x0+0+0
ok pulsante: pixel=1112 copertura=0.21 mirino=0 barra=0 energia=0 riquadro=36x32+730+410
OK 1.21.1-neoforge: scatti in /tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t2/shots-neoforge
```

- The run took 123 s, of which 118 s was `client_shot.sh`.
- The values are identical to Task 1's (1193 / 0 / 1112).
- `log-check.txt` is 0 bytes.
- `rcon.log` shows:
  - `Took 1 recipes from Exo`;
  - both `execute if block … [facing=north,part=main|side]` passed;
  - both `execute if block … minecraft:air` and `execute unless entity @e[type=minecraft:item]` passed after the left click.
- I grepped the whole client log for missing models or textures: there are none. The only WARN lines are vanilla's goat-horn sounds and the chat key.

After the last edit to the script's comment, I ran the committed script again in controls-only mode: `bash -n tools/client/m4a1_shots.sh` gives `syntax-ok`, and `M4A1_SOLO_CONTROLLI=1 bash tools/client/m4a1_shots.sh 1.21.1-neoforge …/shots-neoforge` gives the same three `ok` lines and `OK 1.21.1-neoforge`, exit 0, which also exercises the three-entry `shots` list.

`shots-neoforge/stazione-posata.png` shows two blocks side by side with the placeholder textures and the front (red light) toward the player, with no purple-and-black block. The station item is still mid-lowering in the hand, see concern 2. The new front texture is shown enlarged next to the main's in `fronts-main-side.png`, and the shot's side front is cropped in `side-front-crop.png`.

## Files changed (31, from `git show --stat HEAD`)

- Created:
  - `content/StationPart.java`
  - `gametest/StationBlockGameTests.java`
  - `models/block/station_side.json`
  - `textures/block/station_front_side.png`
  - six generated data files: recipe, advancement `recipes/misc/station` and loot table, in each tree.
- Modified:
  - `compat/impl/BlockBase.java`, `compat/CompatBlock.java`
  - `content/StationBlock.java`, `content/StationItem.java`
  - `blockstates/station.json`
  - `gametest/GameTestKit.java`, `gametest/StationMenuGameTests.java`, `gametest/DataGameTests.java`
  - `RedstoneExoframes.java`
  - the two generated `mineable/pickaxe.json` tags
  - `en_us.json`, `it_it.json`
  - `tools/gen_data.py`, `tools/gen_placeholders.py`
  - `tools/check_jars.sh`
  - `tools/client/m4a1_shots.sh`
  - `tools/README.md`, `tools/client/README.md`
  - `docs/compat.md`
  - the root `README.md`: one sentence, "tabella di bottino del caricatore" becomes "tabelle di bottino del caricatore e della stazione".

## Self-review notes

- Lines over 120 characters: I reflowed the four I had added. The ones left in `RedstoneExoframes.java` (lines 97 and 110) were already there.
- Line counts: exactly ten `add(tests,` lines in the new class, 220 in all. The reflowed Javadoc line ` * {@code add(tests, …` cannot match `run_gametests.sh`'s `^[[:space:]]*add\(tests,`.
- `placeStation` works out the side in relative coordinates, which is right because the test structures are never rotated (the `TestFunction` rotation is NONE). The placement test checks the block's own rule with hard-coded positions, so the kit and the block are not checked against each other.
- No `isCreative()` in the new code. The station logic uses `getAbilities().instabuild`, and the tests set game modes explicitly.

## Concerns

1. **On Forge 1.20.1, the hook runs after the tool takes damage.**
   - **Finding (javap):**
     - On vanilla 1.20.1/1.21.1 (Fabric) and NeoForge 21.1.266, `ServerPlayerGameMode.destroyBlock` calls `playerWillDestroy` before `ItemStack.mineBlock`.
     - On Forge 47.4.29 it calls `canHarvestBlock`, then `mineBlock`, then `removeBlock` → `IForgeBlock.onDestroyedByPlayer` → `playerWillDestroy`.
   - **Consequence:** only on 1.20.1-forge, if a pickaxe breaks on the very hit that breaks the side half, `onPlayerWillDestroy` sees an empty hand (`!hasCorrectToolForDrops`) and removes the main silently, so the station is lost. It is a rare edge case, and the brief's tests use fresh pickaxes.
   - **A related mismatch on NeoForge (javap):** the one-argument `Player.hasCorrectToolForDrops(BlockState)`, which the brief prescribes, is the plain vanilla check there. The break path's `canHarvestBlock` instead calls the three-argument form, which fires `EventHooks.doPlayerHarvestCheck`. So another mod that changes harvestability through `PlayerEvent.HarvestCheck` could make the hook and the game disagree about whether the side half's break should drop the station. On Forge 1.20.1 the one-argument form does fire `ForgeEventFactory.doPlayerHarvestCheck`. With vanilla tools nothing differs. It is the same kind of mismatch as the Forge call order, and one seam could fix both.
   - **What I did:** I followed the design rule verbatim, did not redesign, and documented the call order in `BlockBase`'s Javadoc and in `docs/compat.md`. A fix would need a loader-specific seam, for example remembering the tool check from Forge's `BreakEvent`. That is for the controller to decide.
2. **The `stazione-posata` shot has the held item in the way.** The plan's step list takes `@scatto stazione-posata` right after `item replace … minecraft:air`, so the station item is still lowering and covers most of the side half's front. The side's top and part of its front are visible. I kept the plan's exact steps. An `@attendi 1` before the shot would show the side front unobstructed, if wanted.
3. **Doc edits beyond the brief's file list, all text only:**
   - `tools/client/README.md`:
     - the m4a1 paragraph said "la stazione si posa con `setblock`";
     - its duration;
     - "le sette ricette della mod" in the m3c2 row, which now says "della parte 3c-2" and points to m4a1 for the station's recipe;
   - the class Javadoc of `GameTestKit`, which lists the test classes;
   - the "Cartelle dati" row of `docs/compat.md`;
   - the root `README.md` sentence about what `gen_data.py` writes.

   I left the historical `m3c2_shots.sh` and `docs/test-in-gioco/m3c-giocarci.md` unchanged.
4. **The station tooltip keys have no in-world check.** `ChargerGameTests.everyItemAndBlockHasANameInBothLanguages` checks only the charger's tooltip keys, and the plan's test list has none for the station, so I did not add one. `LangFilesTest` still checks that the keys and their order match in both files.
5. **The duration figures come from 1.21.1-neoforge only.** The comment says so explicitly ("misurati 123 s su 1.21.1-neoforge", next to Task 1's four-variant figures). The four-variant client runs are Task 6's.

## Fix before review (ruling P14)

Coordinator's rulings: concern 1 is accepted as documented (P13), with nothing to change. Concern 2 is fixed here (P14). Concerns 3-5 need no action.

**Commit:** `92c10ff` tools: m4a1_shots.sh, un secondo di attesa prima dello scatto della stazione posata (SSH-signed, with the two trailer lines, not pushed). It changes 2 files, 7 insertions and 6 deletions.

### What changed

- **The step:** in `tools/client/m4a1_shots.sh`, phase 3 (posa) now reads `"item replace entity $u weapon.mainhand with minecraft:air" "@attendi 1" "@scatto stazione-posata"`.
- **The header comment of the phase** now says:
  - the shot comes one second after the hand is emptied, when the lowering item no longer covers the side half;
  - the two halves are side by side and uncovered, with the placeholder textures and the fronts toward the player;
  - the west end of the side half runs past the shot's right edge (see below);
  - no purple-and-black block.
- **The measured duration**, which I updated to this run of the committed sequence, in two places:
  - the script header: "misurati 126 s su 1.21.1-neoforge, di cui 121 s l'esecuzione di client_shot.sh", was 123/118;
  - the matching figure in `tools/client/README.md`: "misurati 126 s su 1.21.1-neoforge", was 123.

  Nothing else in the README changed.

### Commands and outputs

The evidence is in `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t2/`: `client-neoforge-p14.txt` and the folder `shots-neoforge-p14/`.

- **The rerun.** `./gradlew --stop`, then `EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh 1.21.1-neoforge /tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t2/shots-neoforge-p14`. Exit 0, in 126 s.
  ```
  ok anteprima: ritaglio magenta=1193 riquadro=41x85+604+152; schermata magenta=1193 riquadro=41x85+604+152
  ok orientamento: meta' destra del ritaglio magenta=0 riquadro=0x0+0+0
  ok pulsante: pixel=1112 copertura=0.21 mirino=0 barra=0 energia=0 riquadro=36x32+730+410
  OK 1.21.1-neoforge: scatti in /tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t2/shots-neoforge-p14
  ```
  - `log-check.txt` is 0 bytes.
  - The step log shows `[85s] comando eseguito: item replace entity Exo weapon.mainhand with minecraft:air`, then `[86s] passo eseguito: @attendi 1`, then `[86s] salvato stazione-posata.png`.
- **Header edits after the run.** They were only to comments, so I checked them without another client run:
  - `bash -n tools/client/m4a1_shots.sh` → `syntax-ok`;
  - `M4A1_SOLO_CONTROLLI=1 bash tools/client/m4a1_shots.sh 1.21.1-neoforge …/shots-neoforge-p14` → the same three `ok` lines and `OK 1.21.1-neoforge`, exit 0.
- **The selftest.** `bash tools/client/client_shot.sh --selftest` → `OK passi`, exit 0.

### What the shot shows

`shots-neoforge-p14/stazione-posata.png`:

- **Both halves:** side by side and no longer covered by the hand. The empty arm sits low in the bottom-right corner, below the blocks.
- **The main half (left):** the red light and two marks on its front panel, the copper drawer below, and the copper-guide top.
- **The side half (right):** its front shows the panel with all three grille bars and the copper drawer, and its top shows the copper guides.
- **The fronts** face the player.
- **No purple-and-black block.**

One correction to the expectation "both halves fully visible": the side half's outer (west) end runs past the right edge of the 960×540 window.

- **Pixel check:** the pixel column x = 959 is block-coloured from y 80 to y 320 and grass at y ≥ 360, so the right end of the side's top and the outer border of its front plate are out of the frame.
- **Cause:** the plan's fixed viewpoint (`tp $u 0.5 -60 0.5 0 55`), not the hand. The ruling didn't ask for a different viewpoint, so I didn't change it, and the header says so.
- **Possible fix:** a viewpoint further back, or turned slightly west, would frame both halves whole, if wanted.

The crop of the side half is in `p14-side-crop.png`.

## Fix round 1

The review found one Important finding: the two platform gaps accepted under ruling P13 were not documented next to the rule. The NeoForge harvest-check gap was not documented anywhere, and for Forge the call order was described but not the station loss.

**Commit:** `31da028` docs: i due limiti accettati della rottura della stazione (Forge 1.20.1 e il controllo del raccolto di NeoForge). SSH-signed, with the two trailer lines, not pushed; 2 files, 16 insertions and 1 deletion.

### What changed (comments and docs only)

- **`content/StationBlock.java`:** a new paragraph at the end of `onPlayerWillDestroy`'s Javadoc, in Italian with ASCII apostrophes. It says the two limits are accepted (decisione P13), because the rule is the same one vanilla 1.21.1 doors use (`DoorBlock.playerWillDestroy`: creative, or the one-argument `hasCorrectToolForDrops` false), and fixing them would need a different seam per loader. The two limits:
  - **Forge 1.20.1.** The call comes after the break has used up the tool. If the pickaxe breaks on the very hit that breaks the side half, the hand is already empty here and the station is lost: the main half goes without drops and the side half has no loot.
  - **NeoForge.** The one-argument `hasCorrectToolForDrops` does not fire `PlayerEvent.HarvestCheck`. NeoForge fires it from the level/pos-aware calls: the break path asks `canHarvestBlock`, and NeoForge's own door uses the three-argument `hasCorrectToolForDrops`. So a mod that changes harvestability through that event is not consulted by this rule. The station can then be lost, or dropped against that mod's decision, but never twice.
- **`docs/compat.md`, the `playerWillDestroy` entry of CompatBlock:** the same two limits, the same rationale, the same P13 tag and a `[javap]` source mark, appended after the description of the call order.

### Facts checked with javap before writing

- **The door rule:**
  - vanilla 1.21.1 `DoorBlock.playerWillDestroy` calls `Player.isCreative()` and `Player.hasCorrectToolForDrops(BlockState)`, the one-argument form, so the rationale holds for vanilla;
  - vanilla and Forge 1.20.1 `DoorBlock` check `isCreative()` only.
- **NeoForge's door:** NeoForge 21.1.266 patches its `DoorBlock` to `hasCorrectToolForDrops(BlockState, Level, BlockPos)`, the event-firing form. So on NeoForge the door does not share the station's gap, and the docs word it that way.
- **Where NeoForge fires the event:** NeoForge sends the harvest check (`EventHooks.doPlayerHarvestCheck`) from three places:
  - `Player.hasCorrectToolForDrops(state, level, pos)`;
  - `IBlockExtension.canHarvestBlock` (the break path);
  - `BlockBehaviour.getDestroyProgress`.

  Never from the one-argument form.
- **"Never twice":** only the main half has loot, so whatever a harvest-check mod decides, a station drops at most once.

### Covering checks

All output is in `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t2/fix1-checks.txt`.

```
$ bash tools/check_sources.sh
OK sources
exit 0
$ ./gradlew --console=plain :1.21.1-neoforge:compileJava
> Task :1.21.1-neoforge:compileJava
BUILD SUCCESSFUL in 4s
exit 0
$ git diff --stat
 docs/compat.md                                            |  2 +-
 .../com/toxineren/exoframes/content/StationBlock.java     | 15 +++++++++++++++
 2 files changed, 16 insertions(+), 1 deletion(-)
$ non-comment changed lines in the Java diff (expected none):
(end)
```

The last check is `git diff -U0 -- '*.java' | grep -E '^[+-]' | grep -vE '^(\+\+\+|---)' | grep -vE '^[+-][[:space:]]+\*'`. It printed nothing: every changed Java line is a Javadoc line. The added Java lines are ASCII only and at most 120 characters.

### Note

The P13 tag now appears in a committed Javadoc and in `docs/compat.md`, as the coordinator asked. The P-rulings themselves live only in the git-ignored `.superpowers/` progress file, so the text states the reasons in full and does not rely on the tag.
