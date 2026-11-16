# Final fix wave — part 3c-2 "Ricaricare" — fix report

FIX_BASE: 9107aaef9540c5f5a0b03ac90bc54a3ca81f6930 (branch m3c-giocarci).
Written as the work went on; the closing checks and the commits are at the end.

## FB-1 (Important) — checklist sections 16-20: items a step needs

Confirmed. Walking sections 16-20 with a tally found the three gaps of the reviewer and four more of the same kind:

| Where | Gap | Fix (docs/test-in-gioco/m3c-giocarci.md) |
|---|---|---|
| step 98 | the spare charger was destroyed at step 82 | new opening of section 19 gives `/give @s redstone_exoframes:charger 2` and says which is which; step 98 says to place the spare, put the same cell and 10 dust, and that it is lost |
| steps 99-100 | no charger named after step 98 | step 99: place the charger picked up with the pickaxe at step 98 (fallback `/give @s redstone_exoframes:charger`); step 100: "sul caricatore del passo 99, svuotato" |
| step 96 | hoppers removed with no tool named | "togli **con il piccone** anche le due tramogge", and the opening of section 18 says that in survival charger and hoppers are taken back with the pickaxe (by hand nothing drops), and that in creative a placed block stays in the hand |
| dust | 64 at section 17 do not reach step 99 | `/give @s minecraft:redstone 64` at the opening of section 18 and of section 19, each with the number the section needs (22 + 40 optional; 40); step 93 gets a fallback `/give @s minecraft:redstone 15`; the optional run of step 97 a fallback `/give @s minecraft:redstone 40` |
| step 94 (new) | "Posa il caricatore": the only charger left stands next to the comparator of step 93 | opening of section 18 gives `/give @s redstone_exoframes:charger`; step 94 says "il caricatore preso in testa alla sezione" |
| step 97 (new) | the chain needs the charger and three hoppers, but one hopper and the charger still stand from step 96, with 2 dust inside and the full cell in the hopper | step 97 opens with the dismantling: take the 2 dust by hand, break charger and hopper with the pickaxe (the broken hopper drops the full cell and, if it collected it, the charger item) |
| step 98 (new) | "nel caricatore" could be the charger of the chain: a hopper under it collects what falls, and the step counts what falls | opening of section 19: the charger of steps 98-100 is placed on the grass at least three blocks from the hoppers |
| cells (new) | steps 92, 93, 94, 96, 97 (optional), 98, 99 ask for an empty cell or a cell at a given charge without the command; at step 93 a cell "a 5000 RE" or "vuota" left from earlier steps may have been charged in between (the comparator would read 8 or 1) | the exact `/give` (or the reference to the `/give` of step 59 with the number) added to each of those steps and to the openings of sections 17, 18, 19 |

No step was renumbered and no expected result was changed; every addition is a command, a tool, or which item to use.

### Inventory tally (survival, following the text to the letter after the fix)

Dust is a lower bound: everything put into a charger that is charging is counted as burnt, "qualche polvere" of step 88 is counted as 10. "inv" = in the inventory, "posati" = standing in the world. Pickaxe = uses of the wooden pickaxe (59 available).

| After | Chargers inv / placed | Hoppers inv | Chests inv | Dust inv (min) | Pickaxe uses | Cells and other |
|---|---|---|---|---|---|---|
| 16 opening | 0 / 0 | 0 | 0 | 0 | - | crafting table placed |
| 76 | 0 / 0 | 0 | 0 | 0 | - | 1 copper ingot |
| 77 | 1 / 0 | 0 | 0 | 0 | - | given 28 iron, 9 copper, 2 redstone blocks, 2 dust, 1 quartz: all used (28/10/2/2/1); frame, helmet, torso, arm, leg, 1 empty cell, charger |
| 78 | 1 / 0 | 0 | 0 | 0 | - | frame and cell back in the inventory |
| 79 | 1 / 0 | 0 | 0 | 0 | - | frame and piece back in the inventory |
| 17 opening | 2 / 0 | 0 | 0 | 64 | 0 | + wooden pickaxe, + spare charger; cells by `/give` |
| 80 | 2 / 0 | 0 | 0 | 64 | 0 | |
| 81 | 1 / 1 | 0 | 0 | 64 | 4 | placed five times, taken back four times with the pickaxe |
| 82 | 1 / 0 | 0 | 0 | 64 | 5 | spare placed and broken by hand: lost (as the step says) |
| 83 | 0 / 1 | 0 | 0 | 64 | 5 | crafted cell in the charger |
| 84 | 0 / 1 | 0 | 0 | 60 | 5 | |
| 85 | 0 / 1 | 0 | 0 | 60 (62-63 typical) | 5 | cell A (a few hundred RE) in the inventory |
| 86 | 0 / 1 | 0 | 0 | 60 | 5 | + cell B `energy:5000` (in the charger), A in hand |
| 87 | 0 / 1 | 0 | 0 | 60 | 5 | + cell C `energy:10000`; A, B, C in the inventory, charger empty |
| 88 | 0 / 1 | 0 | 0 | 50 | 5 | + shield; cells A, B, C |
| 89 | 0 / 1 | 0 | 0 | 50 | 5 | + dirt (placed); unburnt dust back |
| 90 | 0 / 1 | 0 | 0 | 50 | 5 | creative: two cell copies more, charger emptied |
| 91 | 0 / 1 | 0 | 0 | 48 | 6 | + empty cell D (ends at 1000 RE) |
| 92 | 0 / 1 | 0 | 0 | 28 | 6 | + empty cell E (ends full) |
| 93 | 0 / 1 | 0 | 0 | 13 | 6 | + comparator (placed), 15 dust on the ground; + one empty cell, + one cell at 5000 RE; charger empty |
| 113 | 0 / 1 | 0 | 0 | 13 | 6 | any one cell; the count of cells does not change |
| 18 opening | 1 / 1 | 3 | 2 | 77 | 6 | + charger, + 64 dust, + 3 hoppers, + 2 chests |
| 94 | 0 / 2 | 2 | 2 | 77 | 6 | + 2 empty cells (one in the charger, one in the inventory) |
| 95 | 0 / 2 | 1 | 2 | 67 | 6 | |
| 96 | 0 / 2 | 2 | 2 | 63 | 9 | charger and 2 hoppers taken back with the pickaxe; + cell `energy:9000` (full, in the hopper below); 2 dust left in the charger |
| 97 | 0 / 2 | 0 | 0 | 57 (17 after the optional run) | 11 | dismantling gives back charger, hopper, 2 dust, the full cell; + 4 cells `energy:9000`, all four in the lower chest at the end |
| 19 opening | 1 / 3 | 0 | 0 | 81 (min) | 11 | + 2 chargers (one placed at once), + 64 dust, + cell `energy:5000`, + empty cell |
| 98 | 1 / 2 | 0 | 0 | 61 | 12 | pickaxe break: charger back (2 inv); spare placed and broken by hand: lost (1 inv); creative part uses a charger of the creative tab |
| 99 | 0 / 3 | 0 | 0 | 41 | 12 | empty cell, full at the end |
| 100 | 0 / 3 | 0 | 0 | 41 | 12 | two clients, the cells of step 99 |
| 101 (new world) | 0 / 0 | - | - | 24 | - | 34 iron, 14 copper, 2 redstone blocks, 2 quartz, crafting table, sand, cactus |
| 102 | 1 / 0 | - | - | 20 | - | frame, 6 pieces, 2 empty cells, charger: 34 iron, 14 copper, 2 blocks, 2 quartz, 4 dust used |
| 103 | 0 / 1 | - | - | 2 | - | cells at 5000 and 4000 RE |
| 104 | 0 / 1 | - | - | 2 | - | frame placed with six pieces and the 5000 cell; sand and cactus placed |
| 105 | 0 / 1 | - | - | 2 | - | |
| 106 | 0 / 1 | - | - | 0 | - | old cell in the charger |
| 107 | 0 / 1 | - | - | 0 | - | two cells, as the step says |

Section 20 needs no change: nothing in it is broken or taken back, and the materials of step 101 are exactly what steps 102-106 use.

Covering test: none (text only); re-read step by step against the tally above.

## FB-4 (Minor) — the burning dust

Confirmed (`ChargerRule.tick` leaves the reserve untouched with a full cell, and the block entity does not call the rule with no cell). Opening of section 17, docs/test-in-gioco/m3c-giocarci.md:318: the sentence now says that the energy of the burning dust stays in the charger until there is a cell to charge (a cell that is not full) and then passes in 10 seconds at most, that the front can therefore light up with no dust inside, and that a charger broken and placed again starts without that energy.

## FA-1 (Minor; text only, as ruled) — the right button held down

No code touched. In docs/test-in-gioco/m3c-giocarci.md:
- opening of section 17 (line 318): "Sul caricatore clicca breve": while the right button stays down the action repeats every 0.2 s (the base game does the same with a flower pot) and each repeat undoes the one before; points to step 113;
- new step **113** at the end of section 17 (after step 93; 112 was the last number in use, by the ledger's numbering rule): one second of right button with one cell in hand on the empty charger; expected: the cell goes in and out several times with its two sounds, and at the end exactly one cell, in the hand or in the charger, never lost and never doubled; the step says it was read in the code and never seen in the client and asks to report what happens;
- new question **13** in section 22, and the sentence that introduces the questions says that question 13 is not in the design yet;
- counts and lists that the file states: line 7 (what to redo with the new jars) names step 113; "Il cloud non prova" names the held click; section 21 adds step 113 to the steps that want a sentence; the pointer file docs/test-in-gioco/m3c1-consumare.md:5 says "i passi da 76 a 113". The file has no table with a count of steps or questions: the final summary of section 21 is by section (1-20) and stays as it is.

## FB-2 and FA-3 (Minor; closes T7-F5) — three comments

Confirmed, comments only:
- src/main/java/com/toxineren/exoframes/content/ModItems.java:24-26: the Scrap pieces are crafted at the crafting table (recipes in the per-version data written by tools/gen_data.py) and are also in the creative tab (`ModCreativeTabs` appends them: checked).
- build.gradle.kts:179-182: the per-version resource folder carries the mixin config of its version, the data of its version (recipes, unlock advancements, loot table and tag of the charger) and, on 1.20.1 only, the transparent armor-layer textures (checked with `find src/main/resources-mc*`).
- tools/check_art.sh:51-52: the placeholder list in the comment names the faces of the charger block (checked against the output of `tools/gen_placeholders.py --out`: five `textures/block/charger_*.png`).

Covering check: the four-variant build and `bash tools/check_art.sh` -> `OK art` (closing checks).

## FB-3 (Minor) — EXO_SHOT_SERVER_CONFIG inherited by the default-config sequences

Confirmed (`tools/client/client_shot.sh:159` reads the variable from the environment; the four sequences did not clear it). `unset EXO_SHOT_SERVER_CONFIG` with a one-line comment right after `set -u` in tools/client/m3a_shots.sh:25-26, m3b_shots.sh:42-43, m3c_shots.sh:47-48, m3c2_shots.sh:38-39. m3c_rules_shots.sh untouched (it sets the variable on its own `client_shot.sh` line). One clause in the variables table of tools/client/README.md:128.

Checks: `bash -n` on the five scripts: ok; `bash -c 'set -u; unset EXO_SHOT_SERVER_CONFIG'` exits 0 when the variable is not set; with the variable exported, a child shell started after the `unset` sees it empty. No real-client run, as the finding says.

## FA-4 (Minor; closes T6-F2) — the 64-dust limit in canPlaceItem

Confirmed. src/main/java/com/toxineren/exoframes/content/ChargerBlockEntity.java:319, `canPlaceItem`: `case SLOT_DUST -> stack.is(Items.REDSTONE) && dust.getCount() < MAX_DUST;` (was `stack.is(Items.REDSTONE)`). Javadoc of `canPlaceItem`, of `canPlaceItemThroughFace` and the "Tramogge" paragraph of the class say the clause. No other behaviour changes: for base-game dust a hopper already moved nothing into a slot of 64 (the merge is bounded by the stack maximum); the hand path (`ChargerBlock.addDust`) does not go through `canPlaceItem`.

Documents: docs/compat.md:150 (table row of the four sides) and :152 (the sentence on `canPlaceItem`); design §12.11, docs/superpowers/specs/2026-11-13-m3-telaio-design.md:581 ("solo polvere nello slot 1 e al più 64").

Covering test: `facesAcceptOnlyWhatTheyShould` (ChargerGameTests), no new `add(tests,` line: with 63 dust `canPlaceItem(1, dust)` and the four side faces accept; with 64 they refuse.
- RED (test added, production untouched): `bash tools/run_gametests.sh 1.21.1-neoforge` -> `redstone_exoframes:faces_accept_only_what_they_should failed ... dust accepted by a dust slot that holds 64`, `1 required tests failed`.
- GREEN (after the one-line change): `OK 1.21.1-neoforge: 208 test obbligatori passati, nessun avviso fuori elenco`; then `OK 1.20.1-forge: 208 ...`; then the four variants in the closing checks.

## FA-2 (Minor; closes T4-F3) — two assertions that protect saved charger content

Both in `chargerSavesAndLoadsItsContents` (src/main/java/com/toxineren/exoframes/gametest/ChargerGameTests.java), no new `add(tests,` line.
- (a) The saved tag is read with the five literal names (`"has_cell"`, `"cell_energy"`, `"redstone"`, `"reserve"`, `"carry_milli"`: constant `CHARGER_NAMES_ON_DISK` and the literal `getBoolean`/`getInt` calls), and its key set minus `id`, `x`, `y`, `z` must equal exactly those five. The four loaders add no key of their own to the tag of a charger with no attachments or capabilities (the test passes on the four variants).
- (b) `LevelChunk chunk = level.getChunkAt(abs); chunk.setUnsaved(false); c.setCell(...)` -> `chunk.isUnsaved()`; again for `setDust`. `LevelChunk.setUnsaved(boolean)` and `isUnsaved()` compile and behave the same on 1.20.1 and 1.21.1 (four variants built and run), so nothing went through compat/GameTests. The flag is cleared and found set again inside the same server tick: the chunk is never left unmarked.

Mutation checks (each run on 1.21.1-neoforge, then reverted with `git checkout`, tree clean after):
- `NbtKeys.CHARGER_RESERVE = "reserve_x"` -> `charger_saves_and_loads_its_contents failed ... saved charger read by its on-disk names: {carry_milli:500,cell_energy:1236,has_cell:1b,id:"redstone_exoframes:charger",redstone:6,reserve_x:498,...}`; before this wave every test passed with a renamed constant.
- `setChanged()` removed from `setDust` -> `charger_saves_and_loads_its_contents failed ... setDust did not mark the chunk for saving`.

## T10-F3 (Minor, deferred) — docs/compat.md:114

The CompatItem entry now says "la checklist per il PC (`docs/test-in-gioco/m3c-giocarci.md`, passi 58-60: telaio, celle, pezzi; ...". Checked in the checklist: step 58 is the tooltip of the frame item, 59 of the cells, 60 of the pieces; the three items that extend `CompatItem` are `ExoframeItem`, `CellItem` and `PieceItem`, so the pointer is 58-60 and not only 59-60.

## Not fixed / disputed

None. Nothing on the "NOT to fix" list was touched (no code for FA-1; no other deferred minor).

## Closing checks (on HEAD e064c9eba2d85b1364744e8532643f458b32d81d, tree clean)

`./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`

```
BUILD SUCCESSFUL in 35s
52 actionable tasks: 31 executed, 21 up-to-date
```
JUnit (from versions/<variante>/build/test-results/test): 319 tests, 0 skipped, 0 failures, 0 errors on each of the four variants. No `warning:` line in the build output.

```
bash tools/check_sources.sh  -> OK sources
bash tools/check_jars.sh     -> OK jars
bash tools/check_data.sh     -> OK data
bash tools/check_art.sh      -> OK art
```

`bash tools/run_gametests.sh` (exit 0):

```
OK 1.20.1-forge: 208 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 208 test obbligatori passati, nessun avviso fuori elenco
```

208 on the four variants: no `add(tests,` line was added. No real-client run: no finding of this wave asks for one. No Gradle build, server or client left running.

## Commits (9107aae..e064c9e)

```
3ef0a2d docs: checklist 3c, ogni passo delle sezioni 16-20 ha cio' che gli serve; il clic tenuto premuto   (FB-1, FB-4, FA-1 text)
43cd57e docs: tre commenti resi falsi dalla parte 3c-2                                                    (FB-2, FA-3, T7-F5)
f13abce tools: le sequenze del client con la config predefinita tolgono EXO_SHOT_SERVER_CONFIG            (FB-3)
3815cf0 content: il limite di 64 polveri del caricatore sta in canPlaceItem                               (FA-4, T6-F2)
c064de3 test: i nomi sul disco del caricatore e il chunk segnato da salvare                               (FA-2, T4-F3)
e064c9e docs: compat.md, la voce CompatItem nomina il file e i passi della checklist                      (T10-F3)
```

## Concerns

- Step 113 and question 13 of the checklist describe the held right button from the code (client repeat every 4 ticks, the server acting on its own hand state); never seen in a client, as the step itself says.
- The dust numbers of the tally are lower bounds under the stated assumptions ("qualche polvere" of step 88 counted as 10). Sections 18 and 19 give 64 dust each at their opening, so each section is covered on its own whatever the previous one left.
- The checklist change adds three things the findings list did not name but the walk-through found (steps 94 and 97 without the charger or the hoppers in the inventory, step 98 on a charger away from the hoppers, explicit `/give` for the cells); they are of the same kind as FB-1 and change no expected result.
