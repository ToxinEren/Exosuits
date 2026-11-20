# Task 2 review: il blocco 2x1 (a722480..92c10ff)

## Spec Compliance

### ❌ One requirement missing

Ruling P13, as given to this review, says both accepted platform gaps "are to be documented, not fixed, in this task". Only one is.

- **NeoForge (not documented anywhere).** The one-argument `Player.hasCorrectToolForDrops(BlockState)` used at `StationBlock.java:159` does not fire `PlayerEvent.HarvestCheck`; the break path's `canHarvestBlock` does fire it. Two checks find no mention of this:
  - `grep -i harvest` over the whole diff: 0 hits;
  - `grep -niE 'hasCorrectToolForDrops|HarvestCheck|harvest|un argomento|doPlayerHarvestCheck|canHarvestBlock'` at HEAD on `docs/compat.md`, `BlockBase.java` and `StationBlock.java`: the only hit is the code line `StationBlock.java:159`.

  The gap exists only in the implementer's report (concern 1).
- **Forge 1.20.1 (documented as a mechanism only).** The call order is documented in `compat/impl/BlockBase.java:56-60` and `docs/compat.md:132`. The consequence P13 names is not stated next to the rule in `StationBlock.onPlayerWillDestroy` (`StationBlock.java:146-157`): a pickaxe that breaks on the same hit that breaks the side half loses the station.
- **The two readings of P13 conflict.** The implementer's report records P13 as "accepted as documented, nothing to change". The ruling text given to this review says "both are to be documented". The controller should settle which reading holds. Under the ruling text, this is a missing requirement.

### ✅ Everything else matches the brief, file by file

**compat**
- `compat/impl/BlockBase.java`:
  - neutral hook at `:62`;
  - the 1.21.1 override at `:67-69` calls it before `super` and returns `super`'s state;
  - the 1.20.1 override at `:73-75` calls it before `super`.
- `compat/CompatBlock.java`: the Javadoc is updated (diff:245-252).

**content**
- `content/StationPart.java` (new): `MAIN("main")` and `SIDE("side")`, implementing `StringRepresentable`.
- `content/StationBlock.java`:
  - `FACING` and `PART`; the default state is north and `MAIN`.
  - `otherHalf` and `mainPos`.
  - `getStateForPlacement` (`:102`): `FACING` is the opposite of the placer's horizontal direction. It returns `null` when the side position fails `canBeReplaced(context)`, `isWithinBounds`, or `isUnobstructed(sideState, sidePos, CollisionContext.of(player)/empty())`.
  - `setPlacedBy` (`:120`): server only. It places the side with flags 3, then calls `blockUpdated(pos, AIR)` and `updateNeighbourShapes(level, pos, 3)`.
  - `updateShape` (`:137`): public. It reacts only in the direction of its own other half, and checks same block, other `PART` and same `FACING` through `isOtherHalf` (`:90`).
  - `onPlayerWillDestroy` (`:157`): server only, side half only, when `instabuild || !hasCorrectToolForDrops`. It removes the main with flags 35 (`:165`) and event 2001.
  - `rotate` (`:177`) and `mirror` (`:187`) change `FACING` only; the Javadoc says mirroring is accepted.
  - `onClick` (`:204`) opens `new StationOpening(mainPos(pos, state))`.
- `content/StationItem.java`: `TOOLTIP_KEYS` at `:21` and the grey lines at `:32`.

**Assets and language files**
- `en_us.json:83-84` and `it_it.json:83-84`: right after `charger.hoppers`, with the texts from "Testi".
- `blockstates/station.json`: eight `facing=…,part=…` variants, with `y` 0, 90, 180 and 270 (diff:1757-1792).
- `models/block/station_side.json`: parent `orientable`, with top, front and side as prescribed.
- `textures/block/station_front_side.png`: new, drawn by `gen_placeholders.py`. The script now has a shared `station_front()`; no other PNG changes in the diff.

**Data**
- Six new files (diff:1524-1752):
  - the loot entry carries `block_state_property` (`block` station, `{"part": "main"}`), and the pool carries `survives_explosion`, as for the bed;
  - the recipe is `CCC/ITI/IrI` in category `misc`, with `T` the crafting table;
  - the unlock is on the copper ingot;
  - `mineable/pickaxe` is `[charger, station]` in both trees.
- Neither `charger.json` loot table has a hunk, so both are byte-identical.

**Generators**
- `tools/gen_data.py`:
  - `T` at `:41`, the recipe at `:54`, `BLOCKS` at `:61`;
  - `_validate_loot_table(path, file_id, table)` at `:238` and the following lines, with both messages;
  - the two new `DEFECTS` at `:388-391`, with the files and message fragments the brief gives;
  - both docstrings are updated.

**Game tests**
- `gametest/GameTestKit.java`: `placeStation` at `:838`, `stationsOnTheGround` at `:847`, and `T` at `:896` and in the Javadoc.
- `gametest/StationMenuGameTests.java`: `placeStation(helper, MAIN, NORTH)` at `:59` and `:96`; `closeStation` removes the main, then the side (`:130-131`).
- `gametest/StationBlockGameTests.java`: the ten tests, with positions and steps checked one by one against the brief:
  - `placingTheStationSetsBothHalves` (`:82`);
  - `placementNeedsRoomForTheSideHalf` (`:119`), with the three cases and the border restored in `finally` (`:151-153`);
  - `pickaxeOnTheSideHalfDropsOneStation` and `pickaxeOnTheMainHalfDropsOneStation`;
  - `noPickaxeDropsNothing` (`:190`);
  - `creativeBreakingDropsNothing` (`:210`);
  - `onlyTheMainHalfHasLoot` (stands in for a real explosion, ruling P8);
  - `pistonsCannotMoveTheStation` (`:256`), with a 4-tick delay through `runInSequence`;
  - `removingOneHalfByCommandTakesTheOther`;
  - `adjacentStationsStayIndependent` (`:317`).

  The class is registered after `StationMenuGameTests` (`RedstoneExoframes.java:107`). Its ten `add(tests,` lines bring the total to 220.
- `gametest/DataGameTests.java`: the station `RecipeCase` at `:60`; the rename at `:93` and `:163`; "otto" in the Javadocs.

**Scripts**
- `tools/check_jars.sh`: `station:misc` at `:286`, the station loot table at `:293`, the comment at `:270`.
- `tools/client/m4a1_shots.sh`:
  - the ricetta, posa and rottura phases are exactly as in the phase table, plus the `@attendi 1` from P14 (`:121`);
  - `shots` lists `stazione-posata`;
  - the head comment describes the phases, the P15 framing, and in "Non prova" the real explosion (P8).

**Docs**
- `docs/compat.md`:
  - CompatBlock at `:120` (the hook and the `updateShape` rule);
  - `:132` (both signatures, called before `super`);
  - `:133` (the tests that prove it);
  - the API-table rows at `:310-311`;
  - "otto ricette" at `:85` and `:305`.
- `grep -c 'sette ricette' docs/compat.md tools/README.md` gives 0 and 0 (I ran it).
- `tools/README.md`:
  - "ventitré" at `:41` and `:69`;
  - "38 file, diciannove per albero" at `:85`;
  - eight recipes and unlocks, both loot tables, and `9 difetti`.

**Commit**
- The subject of `dc503e4` matches Step 9 word for word (diff line 5).

**Extras: documentation and generator cleanup, no new features**
- The root `README.md` data sentence.
- The `tools/client/README.md` m4a1 paragraph, its duration, and the m3c2 row.
- The `BLOCKS` table and the `station_front()` helper. Both are small generalizations that the brief's "una tabella per blocco" invites.

### ⚠️ Cannot verify from the diff

- **Opacity of `station_front_side.png`.** The file is binary. Opacity rests on `gen_placeholders.py` ending in `solid(px)` and on `OK art` in `step8-checks.txt`.
- **Deferred to Task 6.** The real explosion (P8) and the four-variant client run of the new phases. Step 6 only asked for 1.21.1-neoforge, and that run is in the evidence: `client-neoforge-p14.txt` has three `ok` lines and an empty `log-check.txt`.
- **Stale lines in the root `README.md`.** "oggi 208 test" (line 31) and "sette ricette" in "Stato" (line 9) are stale. Plan Task 6 Step 5 (plan line 421) schedules both "Stato" and the test count. The controller may want to confirm that Task 6 also covers the recipe count on line 9.
- **The javap findings behind P13** (Forge call order, NeoForge harvest check) are the implementer's. I did not re-verify them.
- **Commit signatures and the two trailer lines** of `dc503e4` and `92c10ff` (a global constraint). The diff package does not show them, and I did not run git.

## Checks run outside the diff

| Named risk | Check | Result |
|---|---|---|
| `ChargerBlock` reaches the new `playerWillDestroy` override through `EntityBlockBase`; an override of its own would conflict or skip the hook | `grep -rn playerWillDestroy src/main/java` | no other override, so the charger's behavior is unchanged |
| P13 documentation | the two greps quoted under ❌ | the only hit is the code line |
| Stale counts | `grep -c 'sette ricette'` on `docs/compat.md` and `tools/README.md`; the plan grepped for the root README count | 0 and 0; the root README count is planned for Task 6 |
| Evidence | read `step8-gametests.txt`, `step8-checks.txt` and `client-neoforge-p14.txt`; recounted JUnit from the existing result XMLs (no build); looked at `shots-neoforge-p14/stazione-posata.png` | matches the report |

I read the diff file in 19 passes (2639 lines, long lines). I did not open any changed file separately; the line anchors come from `grep -n` on the files at HEAD.

## Strengths

- **`StationBlock` reproduces the vanilla bed and door behavior.** Its `isOtherHalf` is stricter than vanilla (it also checks `FACING`). Because `updateShape` reacts only in the direction of its own other half, adjacent stations stay independent (review focus 2).
- **I traced every break path** and the drop counts match the tests: survival with and without a pickaxe, creative, the main or the side half, `/setblock`, and the `35 → flags & -34` shape-update chain that drops the side half's (empty) loot. The `onPlayerWillDestroy` Javadoc correctly explains why only the side half acts.
- **The tests are not vacuous.** The mutation runs fail exactly the expected tests (`gametests-mutation*.txt`): a no-op hook, no room checks, `PushReaction.NORMAL`, and each room check alone.
- **Premises are asserted, not assumed:**
  - the test border puts the main inside and the side outside (`:148-149`);
  - both pistons are powered;
  - an empty hand is not a correct tool;
  - the creative player has `instabuild`.

  The second player is explicitly put in survival (`:132`) because `isUnobstructed` ignores spectators. That detail is easy to miss.
- **Cleanup is careful.** The shared world border is restored in `finally` within the same tick. The piston test removes the power before the pistons, and the comment says why. `assertStationsDropped` discards the items before asserting.
- **`gen_data.py` stays general.** The `BLOCKS` table gives one loot table per block, with the half condition only where needed, and leaves the charger byte-identical. The two new selftest defects pin the new validation rule.
- **The evidence holds up:**
  - four `OK … 220 test obbligatori` lines in `step8-gametests.txt`;
  - JUnit 331 on all four variants, recounted from the 22:08 result XMLs;
  - six `OK` lines in `step8-checks.txt`;
  - the P14 `stazione-posata.png` shows both halves uncovered, with the side half's outer end cut by the right edge (P15).

## Issues

### Critical (Must Fix)

None.

### Important (Should Fix)

1. **P13 documentation is half missing.**
   - **Where:** `content/StationBlock.java:146-159`, `compat/impl/BlockBase.java:54-60`, `docs/compat.md:132`.
   - **What's wrong:** two things are missing from the repository.
     - **NeoForge:** the one-argument `hasCorrectToolForDrops` that the hook uses does not fire `PlayerEvent.HarvestCheck`, while the game's break path does (`canHarvestBlock`). This is not documented anywhere (see the greps under ❌).
     - **Forge:** the call order is documented only as a mechanism ("un attrezzo che si rompe su quel colpo e' gia' sparito dalla mano"). The consequence for the station is not stated where the rule lives.
   - **The rulings conflict:** the implementer recorded P13 as "accepted as documented, nothing to change"; the ruling text given to this review says "both are to be documented". The controller decides; under the ruling text this is a missing requirement.
   - **Why it matters:** P13 accepted both gaps on the condition that they be documented in this task. A maintainer reading `onPlayerWillDestroy` gets no warning of two cases:
     - On NeoForge, if the implementer's javap finding holds (I did not re-verify it), another mod that changes harvestability through that event can make the hook and the game disagree:
       - The mod grants harvest to an empty hand. The hook removes the main without loot, and the station is lost.
       - The mod denies harvest to a pickaxe. The main falls through `updateShape` with its loot, and the station drops against the game's decision.

       It never doubles, because only the main half has loot.
     - On 1.20.1-forge, a pickaxe that breaks on the same hit loses the station.
   - **How to fix (documentation only):** add one or two sentences to the `StationBlock.onPlayerWillDestroy` Javadoc, and the same to the `playerWillDestroy` bullet of `docs/compat.md:132` (or to the `BlockBase` Javadoc). State both cases and that P13 accepted them.

### Minor (Nice to Have)

1. **A stale parenthesis in the head comment of `tools/client/m4a1_shots.sh` (`:52-53`).**
   - The rule reads "nessun oggetto che sblocca ricette nei 12 s prima di uno scatto misurato (la sequenza non ne da')". Since phase 2 (`:115`) gives a copper ingot, the parenthesis is now false.
   - The rule itself still holds: `@attendi 12` follows `recipe take`, and the posa phase comes before `stazione-aperta`.
   - **Fix:** reword it to something like "(il lingotto di rame della fase 2 arriva piu' di 12 s prima del primo scatto misurato)".
2. **No test checks `StationItem.TOOLTIP_KEYS` (`StationItem.java:21`) against the language files.**
   - `ChargerGameTests.java:95` passes only `ChargerItem.TOOLTIP_KEYS` to `everyItemAndBlockHasANameInBothLanguages`, and `LangFilesTest` only compares the two files with each other.
   - A typo in a key would show the raw key in the tooltip.
   - The coordinator ruled no action for this task (report concern 4). One concatenation in that test would close it later.
3. **No automated check covers the `onClick` change to `mainPos(pos, state)` (`StationBlock.java:204`).**
   - No game test clicks the side half (by design: the click that opens the menu is tested only in the real client), and the m4a1 sequence opens the menu from the main half's north face.
   - **Fix:** a pure assertion `StationBlock.mainPos(sidePos, sideState).equals(mainPos)` in an existing test, such as `placingTheStationSetsBothHalves`, would cover it without opening anything.
4. **Two edge cases in `tools/gen_data.py` `_validate_loot_table` (`:240`, `:244-247`).**
   - A malformed `conditions` value (not a list of objects) raises `AttributeError` instead of the path-prefixed `ValueError` that the `validate()` docstring promises. The pre-existing `pool.get("entries", [])` loop has the same weakness, and only hand-edited input could reach it.
   - `file_id.replace(":blocks/", ":", 1)` leaves an id outside `blocks/` unchanged, so the message would call a table id "il blocco della tabella". This is harmless while only block tables exist.
5. **New deprecated-API uses add to the existing javac note.**
   - The build output already carried "Some input files use or override a deprecated API". This task adds:
     - NeoForge's deprecated one-argument `hasCorrectToolForDrops`, in `StationBlock.java:159` and `StationBlockGameTests.java:197`;
     - `BlockState.rotate` in `StationBlock.mirror` (`:187`), the same pattern `ChargerBlock` uses.
   - Both are accepted under P13 and the existing pattern; I list them so the noise is on record.

## Assessment

**Task quality:** Needs fixes

**Reasoning:** The block, the data, the tests and the client sequence are correct, match the brief, and are well-evidenced on all four variants. The only blocking gap is the documentation half of ruling P13, which is a doc-only fix of a few sentences. If the controller's reading of P13 is the implementer's ("nothing to change"), the task is approved as is.
