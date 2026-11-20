# Task 6 report: the sequence on four variants, reference shots, dedicated server, reduced checklist, docs, final verification

Status: DONE_WITH_CONCERNS. Two commits on `m4-stazione`, both signed (`gpgsig` present) with both trailers. The tree is clean, and nothing is pushed.

- `cee81a0`: the task.
- `51e6a69`: a checklist-only follow-up from the final check. Step 7 now places the piston before the station. Step 18 no longer switches the difficulty. See "Self-review findings".

Logs and shots are under `<scratchpad>/m4a1/t6/` (written `t6/` below).

## What I implemented

- **Step 1.** `m4a1_shots.sh` ran on the committed tree (14df684) on all four variants, one at a time, after `./gradlew --stop`.
  - All four were green, with every shot inspected (details below).
  - I then updated the script's header comment: the per-variant measures and durations next to the thresholds, the recipe-unlock rule, the timing of the `kill` after `/exoframe remove`, and the "non prova" list (sounds, second player, death and leaving with the screen open). These are comment changes only: the `steps` array, checks and thresholds are unchanged, verified with `git diff | grep -v '^[-+]#'`.
- **Step 2.** Two `stazione-colori-suoi` runs (1.20.1-forge, 1.21.1-neoforge) with the brief's exact command.
  - Ten files in `docs/test-in-gioco/m4a1-screenshot/`, stripped with the brief's `convert` command.
  - The metadata check is silent. Chunk types left: `IDAT`, `IEND`, `IHDR`.
  - Pixels are identical to the sources (`compare -metric AE` = 0 for all ten).
- **Step 3.** The dedicated server on all four variants, one at a time: `rm -rf versions/<v>/run/world`, `runServer` until `Done (`, then `stop`, then `check_log.py`. The helper is `t6/dedicated.sh`.
- **Step 4.** `docs/test-in-gioco/m4a1-banco.md`: 20 steps plus preparation A–F, following the form of `m3d-braccio.md`.
- **Step 5.** `tools/client/README.md`, `README.md` and `docs/compat.md` updated after a reread. `tools/README.md` needed nothing: it already lists `m4a1_shots.sh` and the station data.
- **Step 6.** The full end-of-task list, run on the final tree before the commit (results below).

### Choices in the checklist the brief did not fix (please check)

1. **Step 18 (death): the brief's command does not kill.** I measured it in the real client (1.20.1-forge, `t6/wither-1.20.1-forge/`, run log `t6/wither-1.20.1-forge.txt`).
   - With `effect give Exo minecraft:wither 10 255` on a full-health survival player, health was 17.0 after ~5 s and 15.0 after ~11 s. The death count stayed 0, and the shot shows the player alive.
   - The likely reasons are the hurt cooldown (no `bypasses_cooldown` damage-type tag exists in 1.20.1 or 1.21.1) and natural regeneration.
   - The checklist therefore uses `/gamerule naturalRegeneration false` plus `/effect give @s minecraft:wither 120 0`. In the same run that measured 9.0 HP at ~20 s, then 0.0 and the death count 1 at ~42 s. The server log says `Exo withered away`, and the shot shows "You Died!".
   - Death at ~40 s gives the PC assistant time to open the station and take the stack.
   - The cloud test server runs at `difficulty=peaceful` (`client_shot.sh` writes it into `server.properties`), so both measurements were at peaceful. That is the same difficulty the checklist world has from step 3. Since `51e6a69`, step 18 does not change the difficulty, and it restores `naturalRegeneration true` afterwards.
   - The wither damage type scales only "when caused by a living non-player". With natural regeneration off, the peaceful heal is off too, so the result should not depend on the difficulty. That is a reading of the code, not a measurement.
2. **Step 19 (leaving the server): the host kicks the second player.** Esc in a container screen closes it first and returns the cursor stack to the inventory, so the stack can only be on the cursor at disconnect if the server drops the player.
   - The second player must be in survival. In LAN with cheats every player is an operator (level 4), and `FramePermissions.canUse` lets a creative operator use locked frames, so the lock would not close the screen.
   - The step uses emeralds, which the host picks up and counts, because a rejoining player respawns on the dropped stack and would pick it up.
3. **Step 2 (the 3c config file) needs the file in place before step 1.** The first launch with the new jars would otherwise write a defaults file that already has the `station` section.
   - Preparation C keeps an existing file only if it has no `station`. Otherwise it creates the brief's `{"energy": {"cellCapacity": 10000}}` with `Set-Content ... -Encoding ascii`, which avoids a Notepad BOM.
   - It also copies the file to `.prima`. Step 2 compares the hash and greps the log for `Config file` and the file for `station`.
   - The silence of that exact text is covered by the JUnit analogue `ConfigFileTest.missingFieldTakesItsDefault` (`{"energy": {"walkPerSecond": 5}}`, no problems).
4. **Step 9 uses both arming paths, in order.** First the frame item (sneak + right-click on the station top), with one Scrap Arm and a cell mounted by hand. That opening shows the empty places, dark with a grey border (the dark-fill-and-grey-border decision for 4a-1). Then `/exoframe remove` and `/exoframe debug place rottame` standing on the station, for the step 10 comparison with `stazione-colori-suoi`.
5. **Other choices.**
   - Step 15 defines "2" and "3" blocks as the 2nd and 3rd grass block in front of the main half. A frame placed with the item is block-centred, so the distances to the footprint are 1.5 and 2.5.
   - Step 5 gives the hand-break time as about 18 s: hardness 3.5, divisor 100 without the right tool, 350 ticks.
   - Step 12 rotates only with an empty cursor and distributes the stack outside the preview. The known release issue (a rotation drag released with a stack on the cursor still reaches the base game) is not relied on and not mentioned.
   - The two accepted breaking limits (Forge 1.20.1 pickaxe; NeoForge HarvestCheck) are mentioned in step 5 as known limits, not steps.

## Tests and results

### Step 1: `EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh <v> t6/shots-<v>`

| Variant | exit | `log-check.txt` | anteprima (clip / screen) | pulsante | rotazione (sagoma; girata magenta screen / clip, box) | total / `client_shot.sh` |
|---|---|---|---|---|---|---|
| 1.20.1-forge | 0 | 0 bytes | 18582 / 18582, `167x198+604+96` | 1112, `36x32+730+410` | 24587; 9225 / 9225, `111x198+629+96` | 152 s / 144 s |
| 1.20.1-fabric | 0 | 0 bytes | 18582 / 18582 | 1112 | 24587; 9225 / 9225 | 132 s / 125 s |
| 1.21.1-fabric | 0 | 0 bytes | 18582 / 18582 | 1112 | 24332; 9225 / 9225 | 135 s / 128 s |
| 1.21.1-neoforge | 0 | 0 bytes | 18582 / 18582 | 1112 | 24332; 9225 / 9225 | 137 s / 130 s |

Each run printed three `ok` lines and `OK <v>: scatti in …`. Logs are in `t6/step1-<v>.txt`.
- **RCON on all four:** `Took 1 recipes`, 9 `Test passed`, `Exoframe unlocked`, `Exoframe removed`, `Killed 7 entities`.
- **Rerun of the checks after the header edit:** `M4A1_SOLO_CONTROLLI=1` on the four saved folders gives `OK` on each.
- **Carried item 7 (the final StationScreen):**
  - The 1.20.1-forge and 1.21.1-neoforge shots are **bit-identical** (`compare -metric AE` = 0, full frame, all seven shots) to Task 5's runs (`t5/shots-forge`, `t5/shots-neoforge`). So the StationScreen edited after the last Task 5 run renders exactly as Task 5 described.
  - Inside the screen rect, the two variants of each version are identical, and the versions are identical at the front.
  - With the suit side-on (`girata`, `suggerimento-torso`), the versions differ in 11906 pixels, all within 4%. This is the 1/255 lighting difference Task 5 described.
  - I looked at `stazione-aperta`, `girata`, `suggerimento-torso`, `telaio-tolto` and `nessun-telaio`, including both Fabric ones. They match Task 5's description.
- **Carried item 5 (the 2 s cleanup):** on every variant the `kill` landed in the integer second after `/exoframe remove` (forge 117→118, fabric 98→99, fabric 1.21 101→102, neoforge 103→104) and killed all 7 items. No timing fix was needed. The header and README now say so.
- **Carried item 6 (the three ingots):** they show in `telaio-tolto` and in the three final shots, none of which is a reference shot. The README says so.

### Step 2

| Variant | exit | `log-check.txt` | duration |
|---|---|---|---|
| 1.20.1-forge | 0 | 0 bytes | 101 s |
| 1.21.1-neoforge | 0 | 0 bytes | 88 s |

- The two `stazione-colori-suoi` shots are pixel-identical inside the screen.
- The shot shows the Scrap suit in its own colours, the helmet selected, and the grey "0" button.
- Ten files are in the folder, and the brief's Python metadata check gives no output (rc 0).

### Step 3, dedicated server (`t6/step3-<v>.txt`, logs `t6/srv-<v>-latest.log`)

| Variant | `Done (` after | `stop` | `check_log.py` | `Config file` lines | config sha before = after | `station` in config |
|---|---|---|---|---|---|---|
| 1.20.1-forge | 25 s | yes, gradle exit 0 | no output, exit 0 | 0 | `208821b4…` = `208821b4…` | 0 |
| 1.20.1-fabric | 14 s | yes, exit 0 | no output, exit 0 | 0 | same | 0 |
| 1.21.1-fabric | 13 s | yes, exit 0 | no output, exit 0 | 0 | same | 0 |
| 1.21.1-neoforge | 16 s | yes, exit 0 | no output, exit 0 | 0 | same | 0 |

The dev config files are 3c-era (written 15 Nov, no `station` section). So this is also a live four-variant check of design §5.15 on the dev server, not the production jar.
- Each log has one `Redstone Exoframes init` line.
- There is no `Registry minecraft:item` renumbering.
- `No data fixer registered for exoframe` appears once on the two Fabric variants. It is allowed.

### Step 4 commands

- `grep -c '^- \[ \] \*\*[0-9]*\.\*\*' docs/test-in-gioco/m4a1-banco.md` → `20`.
- `grep -n '^- \[ \] \*\*1\.\*\*' …` → one line (85).
- The "ogni scatto nominato esiste" loop → no output. Ten shots are named, all present.

### Step 5 commands

- `grep -c '208 test' README.md` → `0`.
- `grep -rn 'm4a1_shots' README.md tools/README.md tools/client/README.md | wc -l` → `9`.
- `sette ricette` → 0 and `otto ricette` → 1 in `README.md`.

### Step 6: final verification (on the tree of `cee81a0`, before that commit)

`51e6a69` changes only `docs/test-in-gioco/m4a1-banco.md`, so the build, tests and checks below still apply. Step 4's greps were rerun on it.

| Che cosa (design §6, la parte 4a-1) | Prova | 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric | 1.21.1-neoforge |
|---|---|---|---|---|---|
| la stazione si fabbrica e la ricetta si sblocca | `DataGameTests` (`everyRecipeIsFoundByKey`, `everyRecipeCraftsFromItsShape`, `everyRecipeHasItsUnlockAdvancement`, `aCopperIngotUnlocksEveryRecipe`); fase "ricetta" (`recipe take` → `Took 1 recipes`) | OK | OK | OK | OK |
| si posa come blocco 2x1, la metà laterale a destra, e senza spazio non si posa | `placingTheStationSetsBothHalves`, `placementNeedsRoomForTheSideHalf`; fase "posa" (the two `execute if block` passed; `stazione-posata` looked at) | OK | OK | OK | OK |
| si rompe senza doppioni e i pistoni non la spostano | the 4 breaking tests of `StationBlockGameTests`, `onlyTheMainHalfHasLoot`, `removingOneHalfByCommandTakesTheOther`, `adjacentStationsStayIndependent`, `pistonsCannotMoveTheStation`; fase "rottura" (air in both halves, no item entity) | OK | OK | OK | OK |
| si apre su un telaio piazzato sopra o entro 2 blocchi, il più vicino usabile; altrimenti il messaggio | `StationReachTest` (8, JUnit), `finderUsesTheFootprintAndTheHeight`, `finderPicksTheNearestUsableFrame`, `clickWithNoFrameInReachSaysSo`, `clickOnlyNearLockedFramesSaysLocked`, `clickWithOffHandOrAsSpectatorDoesNothing`; fasi "senza telaio" (`nessun-telaio`: no screen, "No exoframe nearby…" on the action bar) and "apertura" | OK | OK | OK | OK |
| resta aperta finché il telaio c'è, è a portata ed è usabile, e si chiude con il messaggio quando no | `menuClosesWhenTheFrameIsPickedUp`, `menuClosesWhenSomeoneEntersTheFrame`, `menuClosesForOthersWhenTheOwnerLocks`, `menuClosesWithoutAMessageForOtherReasons`; fase "chiusura" (`telaio-tolto`: no screen, "The exoframe is no longer available at the station"; `execute unless entity` passed) | OK | OK | OK | OK |
| la schermata si registra e si apre sulle quattro varianti, con l'anteprima 3D nel suo riquadro | the Task 1 gate; `anteprima` (18582 = 18582) | OK | OK | OK | OK |
| il pannello del telaio mostra la vista del server, che arriva all'apertura e torna quando cambia | `StationViewTest`, `StationViewCodecTest` (JUnit), `openingCarriesTheFrameView`, `viewFollowsTheRecordWithinATick`, `viewIsSentAtMostOncePerTick`, `viewCarriesThePlayersRights`, `viewForAnotherMenuIsIgnored`; fase "vista" (`Exoframe unlocked`, still connected, log clean); `stazione-aperta` looked at | OK | OK | OK | OK |
| l'anteprima si gira trascinando | `dragTurnsOneAndAHalfDegreesPerPixel` (JUnit); `rotazione` with the strengthened check (sagoma ≥ 12160 in the clip, magenta > 0 only in the clip, box narrower than the front) | OK (24587, 111 < 167) | OK (24587) | OK (24332) | OK (24332) |
| il pulsante 102 va al server e torna | `placeholderButtonCountsOnTheServer`, `placeholderButtonIgnoresSpectatorsAndTheDead`; `pulsante` (1112 in `36x32+730+410`) | OK | OK | OK | OK |
| `station.reachBlocks` in config, e il file della 3c senza righe nel log | `stationReachBlocksStaysBetweenHalfAndEight`, `fileWithoutTheStationSectionTakesItsDefaultsSilently`, `nullStationSectionIsOneProblem` (JUnit); checklist step 2 (written, for the PC); dev dedicated server with the 3c file (Step 3: 0 `Config file` lines, file unchanged) | OK | OK | OK | OK |
| un solo sorgente, quattro jar, server dedicato | build, `check_sources.sh`, `check_jars.sh` (f3 with the station), `check_data.sh`, `check_art.sh`; Step 3 | OK | OK | OK | OK |

Commands and results (`t6/step6-*.txt`):

| Command | Result |
|---|---|
| four-variant `build` + `collectJar` (`step6-build.txt`) | `BUILD SUCCESSFUL in 10s`. Every task was up to date: only docs and a script comment changed since Task 5's build of the same sources. |
| `:<v>:test --rerun` on the four (`step6-junit-rerun.txt`) | `BUILD SUCCESSFUL in 20s`, test results freshly written at 03:27 |
| JUnit count (`step6-junit-count.txt`) | `1.20.1-forge 358`, `1.20.1-fabric 358`, `1.21.1-fabric 358`, `1.21.1-neoforge 358`; failures+errors 0 on each |
| `bash tools/check_sources.sh` | `OK sources` |
| `bash tools/check_jars.sh` | `OK jars` |
| `bash tools/check_data.sh` | `OK data` |
| `bash tools/check_art.sh` | `OK art` |
| `bash tools/client/arm_measure.sh --selftest` | `OK arm_measure` |
| `bash tools/client/client_shot.sh --selftest` | `OK passi` |
| `bash tools/run_gametests.sh` (`step6-gametests.txt`) | four lines `OK <variante>: 235 test obbligatori passati, nessun avviso fuori elenco`, exit 0, 145 s |
| `git diff --stat main -- tools/log_allowlist.txt …mixins.json …mixins.json` | **3 files**, because the local `main` is stale. See concern 1. Against `origin/main` (b82a8da, the merge-base and this part's base) there is **no line**, and no commit of this part touches those paths. |
| `git status --short` before the commit | only the 4 modified docs/script files plus `m4a1-banco.md` and the 10 shots under `docs/test-in-gioco/m4a1-screenshot/`. No shots anywhere else. |

## Carried items

1. **"sette ricette" in `README.md`.** Closed. The 3c-2 sentence now lists the station too: "telaio, pezzi Rottame, cella, caricatore e, dalla parte 4a-1, stazione di manutenzione si fabbricano … con otto ricette". "otto" is only true with the station in the list.
2. **`docs/compat.md:256`.** Closed. The view bullet now says the screen shows the view read-only (panel, header, preview), re-read every frame. It adds that in the real client only the `/exoframe unlock` smoke test exercises a change, which 4a-1 does not draw.
3. **The decision tag in `docs/compat.md`.** Closed: dropped from the breaking-limits sentence, with the reasons kept. The StationBlock Javadoc's tag is left alone, as asked. A grep of all touched files and the new checklist finds no ledger IDs.
4. **The `m4a1_shots.sh` recipe-unlock rule.** It had already been reworded in Task 5 (14df684): the old "(la sequenza non ne da')" was gone. I made it explicit: "la sequenza ne da' due: il lingotto di rame della fase 2, seguito da "@attendi 12" dopo "recipe take" e lontano dal primo scatto misurato…; i lingotti di ferro della fase 10, dopo l'ultimo".
5. **The 2 s cleanup.** No failure, no timing fix (see Step 1). The margin is now documented in the header and README.
6. **The ingots.** Not in any kept shot. Documented in the README.
7. **The final StationScreen.** Confirmed on all four variants (see Step 1).

## Files changed

- `docs/test-in-gioco/m4a1-banco.md` (new)
- `docs/test-in-gioco/m4a1-screenshot/{1.20.1-forge,1.21.1-neoforge}-{stazione-posata,stazione-aperta,girata,suggerimento-torso,stazione-colori-suoi}.png` (new, 10 files)
- `tools/client/m4a1_shots.sh`: header comment only
- `tools/client/README.md`:
  - the m4a1 table values on four variants;
  - the screen-equality paragraph;
  - the kill timing;
  - a new paragraph on reference shots, ingots and "non prova";
  - durations;
  - a "Cosa non copre" bullet for the station;
  - "La sequenza la completa l'ultimo task" removed.
- `README.md`:
  - "Stato" (the 4a-1 paragraph and the eight recipes);
  - the M4 design in the spec list;
  - "oggi 235 test";
  - `m4a1_shots.sh` in the client scripts paragraph;
  - "Documenti" (the checklist and M4a-1 in the plans).
- `docs/compat.md`:
  - `station_view` proven "sulle quattro varianti";
  - the ledger tag dropped;
  - the BlockBase condition count fixed (three `//? if >=1.21` blocks: imports, `playerWillDestroy`, click; the old text said two, with the imports "inside the same condition");
  - the view bullet.

## Self-review findings (fixed before the commit)

- The checklist's first draft of step 18 carried a dated history note for the reader. I removed it; the explanation lives here.
- Step 19 first used diamonds that the rejoining player would pick up on respawn. It now uses emeralds that the host collects and counts.
- Step 7 (fixed in `51e6a69`, after the final check):
  - From in front of a 1-high station, the grass block right behind it is hidden: the sight line over the back edge reaches the ground only blocks further on.
  - Looking at that block from above means a pitch over 45°, and the piston then faces up. So the first wording, piston behind the station placed from the front and retry "un passo più lontano", could not converge.
  - The step now places the piston first, 3 blocks ahead at a shallow pitch so its head faces the player, then the lever on its top, then the station on the grass in front of the head. The lever stays visible and reachable over the 1-high station.
  - Steps 8–15 still use the station of step 7.
  - Step 4's greps rerun after the change: 20 steps, one `**1.**`, all named shots exist.
- The first `docs/compat.md` view sentence claimed a new view "non cambia nulla che si veda" in 4a-1. That is false for a piece mounted by hand by another player, and I rephrased it.
- The README m4a1 paragraph first said the chat line shows in the final shots. Those shots use F1, so only `telaio-tolto` has it; corrected.
- Script: `bash -n` passes, ASCII only, no line over 120 characters. No AI or vendor names in the new docs (grep). Every test name cited exists (grep of `add(tests,` and the JUnit sources).

## Concerns

1. **The local `main` ref is stale** (2e35aa6, behind `origin/main` b82a8da). The brief's literal `git diff --stat main -- …` therefore lists 3 files. Those changes come from commits already on `origin/main` (3c/3d), not from this part: 0 commits of this part touch the paths, and the diff against `origin/main` is empty. I did not move the local `main`.
2. **Step 18 deviates from the brief's command.** The brief gave `wither 10 255`. I replaced it because the measurement shows it does not kill (see "Choices" 1). The cloud measurement was on 1.20.1-forge only, at difficulty peaceful, which is also the checklist world's difficulty. The death and cursor-drop behaviour itself remains for the PC, as the brief intends.
3. **Personal name in `README.md`.** While rewriting "Stato" I replaced a first name in the spec list ("domande aperte per <nome>") with "per il proprietario". It was pre-existing personal data; nothing else changed in that line. Revert if unwanted.
4. **Untested assumptions in the checklist.** These are vanilla facts I did not run here:
   - F3+T works with a container screen open (step 17 says what to note if it doesn't);
   - `/kick` in a LAN world drops the cursor stack at the player's position. That `/kick` exists there is checked: `javap -c` of `Commands` on 1.20.1 and 1.21.1 shows `KickCommand.register` (offset 205/206) before the `includeDedicated` branch (495/564), so it is registered for the integrated server too. The drop follows the design's removal path, as for any container;
   - a lever on top of a piston powers it.
5. **Step 6 build cache.** The four-variant build was fully up to date (10 s), because no source changed since Task 5's build. I reran the four JUnit tasks with `--rerun` to get fresh results. The in-world tests and the client runs exercised the current code.

## Fix round 1 (rulings P25, P26)

Commit `a9cb5cb` "docs: checklist 4a-1, F3+T fra due aperture e la ricerca nel log della config della mod": signed (`gpgsig` present), with both trailers. The tree is clean, and nothing is pushed. Three files changed (+9 / −8): `docs/test-in-gioco/m4a1-banco.md`, `tools/client/README.md`, `tools/client/m4a1_shots.sh` (comment lines only).

This supersedes the first bullet of concern 4 above: F3+T is **not** taken with a container screen open.

### What changed

- **P25, checklist step 17**, now "Ricarica delle risorse fra due aperture", is one sequence:
  1. re-arm the frame;
  2. open the station and see the suit;
  3. close it with Esc ("con una schermata aperta il gioco non prende F3+T");
  4. F3+T without screens, and wait until the reload ends and the world is back;
  5. reopen the station.

  Expected: the suit is in the preview, or the box is empty while the rest of the screen works, with at most one `Suit rendering failed` line per reload. The step then counts the lines, notes the number and whether the suit was there, and on an empty box reopens once more.

  What to report, with a screenshot and the log lines:
  - the game closing;
  - more than one line per reload;
  - the box still empty after the second reopening;
  - a screen that does not respond.
- **P25, the other lines.**
  - `m4a1-banco.md:66` now says "una ricarica delle risorse con **F3+T** fra due aperture della stazione (passo 17)".
  - `tools/client/README.md:163` now says "una ricarica delle risorse (F3+T) fra due aperture della schermata (con una schermata aperta il gioco non prende F3+T)".
  - `tools/client/README.md:216` now says "una ricarica delle risorse (F3+T) fra due aperture della schermata".
  - `tools/client/m4a1_shots.sh:106-109` now says "una ricarica delle risorse (F3+T) fra due aperture della schermata (con una schermata aperta il gioco non prende F3+T)". The comment paragraph is re-wrapped: still ASCII, ≤ 120 columns, and no non-comment line changed.
- **P26, checklist step 2.** The log search is now `Select-String -Path logs\latest.log -Pattern 'Config file .*redstone_exoframes'`. The step explains that it looks only at the mod's lines about its own file, and that nothing printed means the 3c file was read without problems.
  - The only mod line it targets is `RedstoneExoframes.java:65`, `LOGGER.warn("Config file {}: {}", configFile, problem)`, where `configFile` is the path of `redstone_exoframes.json`.
  - No other `"Config file` log call exists in `src/main/java`.

### My own check of the finding (javap on the merged vanilla jars)

`KeyboardHandler.keyPress(long, int, int, int, int)` handles F3 only without a screen:
- **1.20.1:** `getfield Minecraft.screen` / `ifnonnull 929` at offsets 580/583 jumps over the debug-key block. `handleDebugKeys` is at 809.
- **1.21.1:** a local flag is true only if `screen == null`, or if the screen is a `PauseScreen` that does not show the pause menu (offsets 598–653). `iload 11; ifeq 872` at 707/709 skips the block, and `handleDebugKeys` is at 771.

### Covering checks (`t6/fix1-checks.txt`, run on the edited tree before the commit)

```
$ grep -n "F3+T" docs/test-in-gioco/m4a1-banco.md tools/client/README.md tools/client/m4a1_shots.sh README.md docs/compat.md (each hit shown as the clause around F3+T)
docs/test-in-gioco/m4a1-banco.md:66: ...; una ricarica delle risorse con **F3+T** fra due aperture della stazione (passo 17); ...
docs/test-in-gioco/m4a1-banco.md:134: ... Chiudi la stazione (Esc): con una schermata aperta il gioco non prende F3+T. Poi, senza schermate, tieni premuto F3 e premi T: il gioco ricarica le risorse ...
tools/client/README.md:151: ...; una ricarica delle risorse (F3+T) con il braccio sullo schermo; la tuta di un secondo giocatore. ...
tools/client/README.md:163: ...; una ricarica delle risorse (F3+T) fra due aperture della schermata (con una schermata aperta il gioco non prende F3+T); ...
tools/client/README.md:216: ..., una ricarica delle risorse (F3+T) fra due aperture della schermata, la morte e l'uscita dal server ...
tools/client/README.md:217: ..., la ricarica delle risorse (F3+T), un pezzo del braccio montato su un lato solo. Li prova la checklist `docs/test-in-gioco/m3d-braccio.md` ...
tools/client/m4a1_shots.sh:106: # misure della finestra; una ricarica delle risorse (F3+T) fra due aperture della schermata (con una schermata aperta
tools/client/m4a1_shots.sh:107: # il gioco non prende F3+T); un'esplosione vera; ...
```
Every station line now describes a reload between two openings. Two lines do not, and both are outside the station:
- `tools/client/README.md:151` is the m3d paragraph;
- `:217` is the "braccio in prima persona" bullet.

Both are about the 3d arm, where F3+T is pressed in the world with the arm on screen and no GUI screen open. They are correct and unchanged. `README.md` and `docs/compat.md` have no F3+T line.

```
$ grep -c "^- \[ \] \*\*[0-9]*\.\*\*" docs/test-in-gioco/m4a1-banco.md
20
$ grep -n "^- \[ \] \*\*1\.\*\*" docs/test-in-gioco/m4a1-banco.md | cut -c1-60
85:- [ ] **1.** Avvia il client fino al menu principale con
$ for f in $(grep -o 'm4a1-screenshot/[A-Za-z0-9._<>-]*\.png' docs/test-in-gioco/m4a1-banco.md | grep -v '<' | sort -u); do [ -f "docs/test-in-gioco/$f" ] || echo "manca $f"; done
(no output: every named shot exists; 10 shots named)
$ bash tools/client/client_shot.sh --selftest
OK passi
rc=0
$ bash -n tools/client/m4a1_shots.sh
rc=0
$ ASCII and line-length check of tools/client/m4a1_shots.sh
non-ASCII lines: 0; lines over 120: 0
$ git diff tools/client/m4a1_shots.sh: non-comment changed lines
0
$ grep -n "Config file" docs/test-in-gioco/m4a1-banco.md
86: ...Select-String -Path logs\latest.log -Pattern 'Config file .*redstone_exoframes'` non stampa nulla
```

No Gradle or client run was needed: the changes are in a Markdown checklist, a README and a shell comment.
