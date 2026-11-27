# Part 4d: direction A, "Officina", in the game — report

Status: **DONE_WITH_CONCERNS** (concerns at the end; none is one of the stop rule's four defects).

Commit: `704cef7ed377c622c569d99f3bd09233834cb8d6` on `m4-stazione` (one commit over `ef73434`, signed by the environment: the commit object carries an SSH signature; `%G?` prints `N` here only because no `gpg.ssh.allowedSignersFile` is configured for verification), message `client, tools: la stazione di manutenzione con la grafica scelta dal proprietario (A, Officina); check_art.sh con il generatore della stazione; scatti della 4a-1 rifatti`, ending with the two required lines; not pushed. `git status --short` after the commit: empty (run folders and shots stay outside the repository, in `/home/user/exo-runs/m4d/`, or in git-ignored folders).

## What changed, file by file

Generated art (all written by `python3 tools/art/station.py --direction officina --out src/main/resources/assets/redstone_exoframes`; two runs into scratch folders gave identical bytes, and the committed files equal a third run):

- `models/block/station_main.json`, `models/block/station_side.json`: modified. The placeholders (parent `minecraft:block/orientable`) are replaced by the generator's models: 84 + 66 zero-thickness single-face elements, no parent, textures `particle`, `top`, `side`, `front`, `back`, `bottom`.
- `textures/block/station_top.png`, `station_side.png`, `station_front_main.png`, `station_front_side.png`: modified (the same four names the placeholders used).
- `textures/block/station_back_main.png`, `station_back_side.png`, `station_bottom.png`: new.
- `textures/item/station.png`: modified (the generator's icon).
- `blockstates/station.json`, `models/item/station.json`: hand-written, unchanged (see "Blockstate" below).
- Deleted: nothing. Every committed station asset is either written by the generator (10 files) or hand-written (the blockstate and the item model); `check_art.sh` now refuses any `models/block/station*.json`, `textures/block/station*.png` or `textures/item/station*.png` that the generator does not write.

Tools:

- `tools/gen_placeholders.py`: the station is gone (`draw_station_top`, `draw_station_side`, `station_front`, `draw_station_front_main`, `draw_station_front_side`, `STATION_TEXTURES`, the `STATION` sprite and the two `outputs()` entries); a two-line comment says where the station comes from now; the docstring says "facce del blocco del caricatore" and names `station.py`. It writes 19 files instead of 24 (checked: `--out` writes 19, none named `station`). Helpers the charger still uses (`charger_plate`, `solid`, `HOUSING`, `CHARGER_RED`...) stay; no unused top-level name is left.
- `tools/check_art.sh`: after the suit, it regenerates the station (`station_direction=officina`) into `$tmp/station` and compares every generated file with the committed one (JSON with `cmp`, PNG with `compare_png.py`), fails on zero files, and fails on a committed station model/texture/icon the generator does not write. Header comment updated. Negative checks run by hand (then restored): an extra `textures/block/station_extra.png` → `FAIL: ... non viene da tools/art/station.py ... va tolto`, exit 1; one edited coordinate in `station_main.json` → `FAIL: ... diverso da quello generato`, exit 1; `station_bottom.png` removed → `FAIL: manca ...`, exit 1; after regenerating → `OK art`.
- `tools/art/selftest.py`: three new cases, numbered after the existing ten, with a premise (the real `officina` bench, built without the fault, passes `Bench.check()`; otherwise the case fails as `premessa non valida`):
  - 11, a front pocket down to the ground row → `tasca di front aperta verso bottom nel voxel (20, 0, 0)`;
  - 12, a front pocket joined to a top pocket without opening onto either face (the case also refuses its own premise if any `aperta` report appears) → `tasche di front e top unite nel voxel (4, 14, 1)`;
  - 13, `station.py` (its `load` swapped for the officina design plus the ground pocket) exits 1 and writes no file, like case 10 for `build.py`.
  Mutation check in memory: with `Bench.check` returning `[]` all three fail (`nessuna segnalazione`, `station.py e' uscito con 0 invece di 1`); with the joined-pocket reports filtered out, cases 11 and 12 rerun: 11 passes, 12 fails. `python3 tools/art/selftest.py` → `OK selftest: 15 difetti su 15 segnalati, premesse valide`.
- `tools/art/station.py`: docstring only (the mod's station is direction A; how to regenerate it; `check_art.sh` checks it).
- `tools/art/preview/station.js`: comments only (`--check` draws the mod's station; `--vanilla` was for the placeholders' parents). Verified: `node station.js --check 1` without `--vanilla` renders the Officina station from the mod's files (`tools/art/preview/out/stazione/controllo.png`, git-ignored).
- `tools/client/m4a1_shots.sh`: comment only (what `stazione-posata` shows; edited after the last client run, since bash reads a running script incrementally). `bash -n` clean; `git diff ef73434 HEAD` of the file has 0 changed non-comment lines, and the committed script, rerun with `M4A1_SOLO_CONTROLLI=1` on the four run folders, prints the same three `ok` lines and `OK <variant>` on each.

Docs:

- `README.md` line 28: the station has its own generator (`station.py --direction officina --out ...`); `gen_placeholders.py` writes "oggetti, icona, facce del blocco del caricatore, vignetta dell'HUD"; `check_art.sh` checks "tutti e tre i gruppi".
- `tools/README.md`: the intro names `station.py`; the `check_art.sh` row mentions the station; "Texture placeholder": nineteen files, the station entries removed, a sentence that the station comes from `station.py` (direction A) and was a placeholder from here until part 4c; "i diciannove file".
- `tools/art/README.md`: selftest list 11-13 and the premise sentence; "Stazione di manutenzione": the owner chose `officina` on 27 November 2026, the mod's files are its output, `check_art.sh` regenerates and compares them; the regeneration command; why the hand-written blockstate fits the generator's orientation; "Luce nel gioco" with the in-game measurements; a new paragraph "Crepe di un pixel"; `--check` rewritten (the 27 November placeholder check kept as history; `--vanilla` no longer needed; the real client drew the models on the four variants, rotation-90 ledges included).
- `docs/arte/README.md`, "Stazione: tre direzioni": the owner chose A on 27 November 2026 and it is in the game; the lighting sentence now reports what the client showed; "Dove sta cosa" lists the generated files.
- `docs/test-in-gioco/m4a1-banco.md`: one note after the 4c note, in the same form ("Con i jar di dopo la parte 4d ..."): the station has its final look (A, "Officina"); "La grafica vera" no longer holds for the block and the icon (the screen keeps flat colours); a short description of the block; which passages change ("Cosa c'è da vedere" / "Il blocco", step 4, the description of `stazione-posata` in "Già verificato", step 3's icon); no light; the reference shots are redone (same names); what to look for and note on every variant: the four facings, no missing/purple-black/stretched/flipped face, the dark recess walls are not an error, any light dots flashing along recess edges, the break particles.
- `tools/client/README.md`: what `stazione-posata` shows now; the m4a1 references were redone in part 4d.
- `docs/compat.md`: unchanged (it does not list who writes the station's assets).

Reference shots: the ten files in `docs/test-in-gioco/m4a1-screenshot/` (below).

## Blockstate

`bench.py` draws both models with the front to the north (model z = 0) and the side half to the west of the main half (model x = 16 − bench x for the main half, 32 − bench x for the side half), i.e. to the right of whoever looks at the front. `StationBlock.otherHalf` puts the side half at `FACING.getCounterClockWise()` from the main one (north → west, east → north, south → east, west → south), and the blockstate turns both models by y 0/90/180/270 for north/east/south/west, the furnace convention (`BlockModelRotation`: y 90 takes north to east). For every facing the main model's seam face (model west) and the side model's seam face (model east) end up facing each other on the `getCounterClockWise()` side, so the blockstate needed no change. In game (facing north, the only facing the scripts place): the main half is where the player clicked (0 −60 1), the side half to its right (−1 −60 1), as in `a-officina-mondo.png`.

## In-game comparison with the preview

Station shots (run outside the repository, `/home/user/exo-runs/m4d/`): `stazione.sh` places the station exactly as `m4a1_shots.sh` does (item in hand, right click on the floor, `execute if block` on both halves), then switches the player to spectator (no hand, no gravity), hides the interface with F1, and teleports the camera: front three-quarter from the main-half end (north-east) and back three-quarter from the side-half end (south-west), close and a bit farther, straight front and back, then a Rottame frame with its own colours on the main half facing north (as in the previews) and front/back three-quarter views of both. Daylight (`time set 6000`, clear weather: `client_shot.sh`). Smooth lighting is on: `client_shot.sh`'s `options.txt` has no `ao` line, the client did not rewrite the file, and `Options` creates `options.ao` with default `true` on both versions (`javap -c` on the mapped 1.21.1 and 1.20.1 client jars: `ldc "options.ao"; iconst_1; OptionInstance.createBoolean`). No model or texture error in any client log (`Missing textures`, `Unable to load`, `Failed to load model`: 0 lines).

Best shots (1.21.1-neoforge; the same views exist for the other three variants in `/home/user/exo-runs/m4d/<variant>/stazione/`):

- `/home/user/exo-runs/m4d/1.21.1-neoforge/stazione/davanti-vicino.png` (front three-quarter) and `/home/user/exo-runs/m4d/1.21.1-neoforge/stazione/dietro-vicino.png` (back three-quarter);
- also `davanti.png`, `dietro.png`, `fronte.png`, `retro.png`, `telaio-davanti.png`, `telaio-dietro.png` in the same folder;
- enlargements in `/home/user/exo-runs/m4d/1.21.1-neoforge/ispezione/` (`davanti-vicino-x3.png`, `dietro-vicino-x3.png`, `estremo-est-alto-x4.png`, `cassetti-x6.png`, `giro-montaggio.png`, the crack crops) and the preview crops in `/home/user/exo-runs/m4d/ispezione/` (`anteprima-davanti-vicino.png`, `anteprima-dietro-vicino.png`, `anteprima-cassetti-x5.png`).
- Extra views on 1.21.1-neoforge (`stazione_extra.sh`, folder `/home/user/exo-runs/m4d/1.21.1-neoforge/stazione-extra/`): both ends from above (`estremo-est.png`, `estremo-ovest.png`), four low views (`sotto-*.png`, eye 0.15 above the grass, looking slightly up, to see the walls that face the ground), and a ring of eight three-quarter views every 45° (`giro-000.png` ... `giro-315.png`).
- The item icon in game (1.21.1-neoforge, `oggetto.sh`): `/home/user/exo-runs/m4d/1.21.1-neoforge/oggetto/oggetto-in-mano.png` (hotbar and hand) and `inventario.png` (creative inventory hotbar row).

Against `a-officina-mondo.png`, face by face (asymmetric markers listed from `officina.py` before looking):

- **Same faces, same orientation, halves not swapped.** Front: main half on the left with the recessed redstone screen, the three copper knobs under it and the lever in its black slot to the right of the screen; side half on the right with the two raised drawers, copper handles and brass labels. Back (seen from behind): copper pipe along the whole length with three brackets, the dark grille under it on the left (side half) and the hatch on the right (main half). Ends: vertical copper pipe with two brackets, nearer the front on both ends. Top: bronze worktop with raised border, bolts, highlight band and the dark joint along the seam, front to back. Same on all eight ring views and on all four variants.
- **No missing face.** Geometry check: the two committed models cover exactly the exposed voxel faces of the officina bench, each once (main 1778 of 1778, side 1792 of 1792, none missing, none extra, none doubled). In the shots no hole shows the world through the block, except the one-pixel cracks below.
- **No stretched face, no face rotated wrongly.** The only faces with UV `rotation` 90 are the up/down ledges of the end recesses (4 per half). Seen from above (`estremo-est.png`, enlarged in `estremo-est-alto-x4.png`), the bottom ledge shows the plinth row's texels along its length, the two rivets at about a quarter and three quarters of the way, as in the preview; the top ledge faces the ground and is black (below). All other faces show texels at the same size as their neighbours.
- **No z-fighting.** None in any shot; by construction no two faces of a half are coplanar with the same normal (the coverage check above counts each unit face once), and the seam faces of the two halves are back to back.
- **Variants.** The station looks the same on the four variants: inside the station crop of the close shots, forge, fabric 1.20.1 and fabric 1.21.1 differ from neoforge by 957/955, 796/429, 788/418 pixels (front/back), scattered grass and silhouette-edge pixels (diff image `/home/user/exo-runs/m4d/ispezione/diff-forge-neoforge-davanti-vicino.png`).

**How dark the recessed walls are** (smooth lighting on; in game the light of a recessed face comes from the block in the face's direction, as the generator's report said):

- Walls facing the other half are **lighter than in the preview, not darker**: smooth lighting grades them. The left side of the upper drawer (it faces the main half) reads (23,25,27) at the top and (19,20,22) lower down, next to the lit drawer front at (67,72,78), about a third of it; the preview draws the same wall at (6,6,7) next to (79,85,92), about a twelfth.
- Walls facing the ground are **darker than the preview's rule**: pure black, (1,1,1) on the underside of the top's bronze lip in `sotto-fronte.png`, against (82,62,40) for the same bronze row on the lit front; the preview's rule (down shade 0.5 × light ~0.1) would give about (7,5,4). They are visible only from below the station's top (eye lower than 14/16 of a block above the station's base), so no preview view and no normal viewpoint from above shows them.
- Smooth lighting also darkens the lower part of every face near the ground (vanilla ambient occlusion), which the previews (flat lighting) do not show.

## Runs: the `ok` lines

All runs on the development client, 960×540, GUI scale 2, one at a time (`/home/user/exo-runs/m4d/varianti.out`). Every `log-check.txt` is empty (0 bytes): `m4a1`, `stazione` and `colori` folders of each variant, plus `stazione-extra` and `oggetto` on neoforge.

`EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh <variant> /home/user/exo-runs/m4d/<variant>/m4a1`:

- 1.21.1-neoforge (exit 0, 156 s):
  - `ok anteprima: ritaglio magenta=18582 riquadro=167x198+604+96; schermata magenta=18582 riquadro=167x198+604+96`
  - `ok pulsante: pixel=1112 copertura=0.21 mirino=0 barra=0 hud=0 riquadro=36x32+730+410`
  - `ok rotazione: sagoma pixel=24332 copertura=4.69 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96`
  - `OK 1.21.1-neoforge: scatti in /home/user/exo-runs/m4d/1.21.1-neoforge/m4a1`
- 1.20.1-forge (exit 0, 165 s): the same `ok anteprima` and `ok pulsante` lines; `ok rotazione: sagoma pixel=24587 copertura=4.74 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96`; `OK 1.20.1-forge: scatti in /home/user/exo-runs/m4d/1.20.1-forge/m4a1`
- 1.20.1-fabric (exit 0, 137 s): identical to 1.20.1-forge; `OK 1.20.1-fabric: scatti in /home/user/exo-runs/m4d/1.20.1-fabric/m4a1`
- 1.21.1-fabric (exit 0, 139 s): identical to 1.21.1-neoforge; `OK 1.21.1-fabric: scatti in /home/user/exo-runs/m4d/1.21.1-fabric/m4a1`

The measures are those of part 4a-1 (18582, 1112, 24332 on 1.21.1 / 24587 on 1.20.1, 9225): the block does not enter any check.

Station shots (`stazione.sh`): exit 0 on the four variants (neoforge 181 s, forge 120 s, fabric 1.20.1 103 s, fabric 1.21.1 107 s). `stazione_extra.sh` on neoforge: exit 0 (137 s). `stazione-colori-suoi` (the archived 4a-1 command: place the station, `/exoframe debug place rottame` on it, open it): exit 0 on 1.21.1-neoforge (97 s) and 1.20.1-forge (99 s). `oggetto.sh` on neoforge: exit 0.

## Reference shots

Old against new, whole frame (518 400 pixels) and inside the screen rectangle 632x464+164+38 (293 248 pixels), `compare -metric AE`:

| Shot | 1.21.1-neoforge | 1.20.1-forge |
|---|---|---|
| `stazione-posata` | 242 772 differ (the block) | 242 772 |
| `stazione-aperta` | 58 538, 0 inside the screen | 58 538, 0 inside |
| `girata` | 58 538, 0 inside | 58 538, 0 inside |
| `suggerimento-torso` | 58 538, 0 inside | 58 538, 0 inside |
| `stazione-colori-suoi` | 58 592, 0 inside | 58 574, 0 inside |

So all five names show the block (the four screen shots around the opaque screen) and all ten files were redone (the two new `stazione-posata` references carry no crack pixel: `crepe.py` finds 0 isolated sky-coloured pixels in each): `convert <run shot> -strip -define png:exclude-chunks=date,time docs/test-in-gioco/m4a1-screenshot/<variant>-<shot>.png`, pixel-identical to the run shots (`compare -metric AE` = 0 for all ten). Inside the screen the four variants are still equal to the pixel, apart from the side-turned suit's light between 1.20.1 and 1.21.1 (11 906 pixels, as already documented in part 4a-1).

No other committed reference shot shows the station: contact sheets of all 72 shots in `m3a-screenshot` (18), `m3b-screenshot` (8), `m3c-screenshot` (16), `m3d-screenshot` (10) and `m4c-screenshot` (20) (`/home/user/exo-runs/m4d/ispezione/riferimenti-*.png`) show the suit, the frame, the charger, the water tank, inventories, arms and the HUD, never the station; the only scripts that place it are `m4a1_shots.sh` and the `stazione-colori-suoi` command. The four m3c shots with a recipe toast date from 16 November 2026 (`0026c52`), before the station existed (`StationBlock` added on 19 November), so the toast cannot show its icon. The station previews and mock-ups (`docs/superpowers/specs/anteprime/2026-11-27-m4d/`, `docs/superpowers/specs/anteprime/2026-11-19-m4/blocco-stazione.png`) are design records, not reference shots, and are unchanged.

## Dedicated server

`/home/user/exo-runs/m4d/server.sh` (the 4c helper, adapted): `eula=true` in `versions/<v>/run/eula.txt`, `rm -rf versions/<v>/run/world`, `./gradlew --console=plain :<v>:runServer` with stdin from a FIFO until `Done (`, `stop`, then `python3 -I tools/check_log.py client --variant <v> versions/<v>/run/logs/latest.log` (output `/home/user/exo-runs/m4d/server/servers.out`, logs `server-<v>.log`):

| Variant | `Done (` | Gradle | `check_log.py` | log |
|---|---|---|---|---|
| 1.20.1-forge | after 24 s, `Done (3.961s)` | exit 0 | no output, exit 0 | new (09:21:16), 2 "Stopping" lines, 0 lines naming the station |
| 1.20.1-fabric | after 18 s, `Done (3.758s)` | exit 0 | no output, exit 0 | new (09:21:42), same |
| 1.21.1-fabric | after 16 s, `Done (1.378s)` | exit 0 | no output, exit 0 | new (09:22:02), same |
| 1.21.1-neoforge | after 16 s, `Done (0.846s)` | exit 0 | no output, exit 0 | new (09:22:20), same |

## End-of-task commands

Run on the final tree, one at a time, before the commit (the only later edit is one sentence of `m4a1-banco.md`, which none of these reads). Logs in `/home/user/exo-runs/m4d/fine/`.

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` → `BUILD SUCCESSFUL in 41s` (`52 actionable tasks: 24 executed, 28 up-to-date`; the four `test` tasks executed, result XML written at 09:23).
- JUnit count (the brief's loop) → `1.20.1-forge 381`, `1.20.1-fabric 381`, `1.21.1-fabric 381`, `1.21.1-neoforge 381`; skipped + failures + errors = 0 on each.
- `bash tools/check_sources.sh` → `OK sources`
- `bash tools/check_jars.sh` → `OK jars`
- `bash tools/check_data.sh` → `OK data`
- `bash tools/check_art.sh` → `OK art`
- `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure`
- `bash tools/client/client_shot.sh --selftest` → `OK passi`
- `bash tools/run_gametests.sh` → exit 0:
  - `OK 1.20.1-forge: 235 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.20.1-fabric: 235 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-fabric: 235 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-neoforge: 235 test obbligatori passati, nessun avviso fuori elenco`

Also run: `python3 tools/art/selftest.py` → `OK selftest: 15 difetti su 15 segnalati, premesse valide`; no AI model or vendor name and no personal data in the added lines (grep of the text diff). The four jars collected at 09:23 in `build/libs/all/` each carry the twelve station assets (the ten generated files, the blockstate, the item model) byte-equal to the committed ones (`unzip -p | cmp`), and exactly the ten generated `station*` files in `models/block`, `textures/block`, `textures/item`: the checklist's "jar di dopo la parte 4d" have the new look.

## PNG chunk check

`/home/user/exo-runs/m4d/blocchi.py` (lists every chunk; exits 1 on anything but IHDR, IDAT, IEND, or a bad signature or tail): every new or changed PNG has only `IHDR,IDAT,IEND`:

- generated, 16×16: `textures/block/station_back_main.png`, `station_back_side.png`, `station_bottom.png`, `station_front_main.png`, `station_front_side.png`, `station_side.png`, `station_top.png`, `textures/item/station.png`;
- reference shots, 960×540: `docs/test-in-gioco/m4a1-screenshot/{1.20.1-forge,1.21.1-neoforge}-{stazione-posata,stazione-aperta,girata,suggerimento-torso,stazione-colori-suoi}.png`.

## Concerns

1. **One-pixel cracks, on all four variants (not one of the stop rule's four defects, so reported for the coordinator's decision rather than fixed).** The generator merges exposed voxel faces into rectangles that meet with T-junctions (a rectangle's corner in the middle of another's side). Along those edges the rasteriser can leave a pixel uncovered, and since nothing is drawn behind the block's surface (the neighbours' faces are culled by the full cube), that pixel shows the sky colour (175,205,255). Seen: two such pixels in one of the eight station shots on every variant (`dietro-vicino.png`, at (408,357) and (417,414), the left end of the back pipe and the lower-left inner corner of the back recess), and two more in fourteen extra views on neoforge (`sotto-fronte.png`, `giro-000.png`, both on the screen recess's right edge): always on concave edges of pockets, never a missing face (coverage check above). In motion they would flicker as single light dots. They are visible in `dietro-vicino.png`, one of the two shots returned. The committed reference shots carry none. I left the art as generated: the cracks are none of the four stop defects, and fixing them changes the generator, which this task only runs. Possible fixes: a T-junction-free tessellation in `bench._rectangles` (split rectangles at every neighbour's corner), or a dark inner face behind each pocket floor so a crack shows dark steel instead of sky. Cost of either: a generator change (plus a selftest case), the two models regenerated (more elements for the first), `check_art.sh`, and the in-game station shots and the ten reference shots redone on the four variants (about 30 minutes of client runs). The checklist note asks the PC tester to report any such dots on a real GPU. Crops: `/home/user/exo-runs/m4d/1.21.1-neoforge/ispezione/crepe-dietro-vicino-x8.png`, `crepa-giro-000-contesto-x4.png`, `crepa-sotto-fronte-x12.png`.
2. **Walls facing the ground are pure black**, darker than the preview's rule, but they are only visible from below the station's top; walls facing the other half are lighter than in the previews. Neither changes what the owner saw from above.
3. **Only the north facing is pictured in game** (as in the previews): the scripts place the station with the player facing south. The other three facings rely on the blockstate rotation, the vanilla furnace's (y 90/180/270, no uvlock, nothing direction-specific in the models); the checklist note asks the PC tester to look at all four.
4. **The item icon is seen in game only on 1.21.1-neoforge** (hotbar, hand, creative inventory); it is a plain `item/generated` sprite on every variant.
5. The remaining 4d items (GUI texture, HUD drawings, kit icon, vignette, glow layer) are untouched, as the ledger's ruling D3 says; `gen_placeholders.py` still writes the HUD vignette and the other placeholders.

## Fix round 1

Status: **DONE_WITH_CONCERNS** (minor concerns below; no crack is left on the station).

Commit: `70cbd3dac605e0d18ff5edc1ea54632c2001df2b` on `m4-stazione`, one commit over `704cef7`, signed by the environment (SSH signature in the commit object), message `tools, client: la stazione senza crepe (facce senza giunzioni a T); scatti rifatti`, ending with the two required lines; not pushed. `git status --short` after the commit: empty. This round's runs are in `/home/user/exo-runs/m4d-fix/` (the first round's, the "before" evidence, stay in `/home/user/exo-runs/m4d/`).

### Approach

- `tools/art/bench.py`: `models()` still merges each plane's exposed voxel faces into rectangles (`_rectangles`), then `_conform` splits them until no vertex of any face lies strictly inside an edge of another face, over the whole bench (both halves in their in-game placement, side half at bench x 16..32, so the seam conforms too). Each pass collects every rectangle corner on its axis-parallel lines; a rectangle with corners strictly inside its edges is cut along the perpendicular through each of them; repeat until nothing is cut (the cuts fall on existing coordinates, so it terminates). A cut must cross the whole rectangle (stopping inside would leave a T), so each line rings around the surface until it meets existing vertices. The pieces keep their rectangle's key (texture, UV map, cullface), so every texel stays where it was; the elements are sorted by direction, plane, then corner, so the output is deterministic (two runs: identical bytes).
- Smarter splits: the result is canonical here. All 64 per-direction merge orders (rows first or columns first) give exactly the same counts, and a pairwise re-merge pass finds nothing to merge (every split line is a ring forced by a vertex; removing one would need all its segments at once). The brief's 576/316 is each half conformed on its own; with the seam conforming as required it is 648/432.
- Face counts and model sizes (one face per element):

  | Model | Before (`704cef7`) | After |
  |---|---|---|
  | `station_main.json` | 84 faces, 10 421 bytes | 648 faces, 80 183 bytes |
  | `station_side.json` | 66 faces, 8 372 bytes | 432 faces, 54 225 bytes |

  Both under 1000. The other two directions also generate under the new rule: B 678 + 246, C 726 + 654.
- The look does not change: a cell-by-cell comparison with the game's mapping (FaceInfo vertex order, BlockFaceUV with the rotation shift) gives, for all 1778 + 1792 exposed unit faces, the same face, texture, cullface and UV at the four corners, before and after; the texture section of each model is the same, and the eight PNGs are byte-identical (only the two model JSONs changed in the assets).

### The T-junction check and its selftest

- `bench.t_junctions(models)` checks the written models, independently of `_conform`: it rebuilds every face rectangle in bench coordinates from the JSON elements of both halves, indexes all corners by axis-parallel line, and reports every corner strictly inside an edge of a face. `station.build()` runs it after `bench.check()` on every generation and raises (station.py exits 1 without writing anything), so `check_art.sh` fails on one. On the `704cef7` models it reports 142 T-junctions, on the new ones 0; a brute-force count (every vertex against every edge) gives the same 142 and 0.
- `tools/art/selftest.py` case 14 (premise: the mod's models have no T-junction): (1) the first face at least 2 long along an axis, split at its middle, is reported (`vertice a T (1, 1, 0) del banco dentro un lato della faccia north di station_main da [13, 0, 0] a [15, 2, 0]`); (2) with `_conform` replaced by the identity (the pre-fix rectangles; premise: they do have T-junctions), `station.py` exits 1 and writes no file. Mutation check: with `t_junctions` always empty the case fails at (1); with `station.py` not calling it the case fails at (2). `python3 tools/art/selftest.py` → `OK selftest: 16 difetti su 16 segnalati, premesse valide`.

### `crepe.py` (isolated sky-coloured pixels), this round

| Variant | 8 station shots (`stazione/`) | Notes |
|---|---|---|
| 1.21.1-neoforge | 0 in each | plus the 14 extra views (`stazione-extra/`: ends from above, low views, ring of 8): 0 in each |
| 1.20.1-forge | 0 in each | |
| 1.20.1-fabric | 0 in 7; `dietro-vicino.png`: 2, at (838,54) and (320,56) | the same two pixels as in the `704cef7` run, with identical 3x3 neighbourhoods in both shots: (838,54) on the fog line of the far horizon (sky-fog (147,175,196) above it), (320,56) in distant grass (78,102,48) all round; outside the station (about x 300-630, y 155-445) and unchanged while the station's mesh changed, so not the station; the two station cracks of that shot, (408,357) and (417,414), are gone (crop of the top band: `/home/user/exo-runs/m4d-fix/ispezione/fabric-orizzonte-x2.png`) |
| 1.21.1-fabric | 0 in each | |

The four crack pixels of the first round, read directly in the new 1.21.1-neoforge shots: `dietro-vicino` (408,357) (175,205,255) → (226,174,127), copper; (417,414) → (32,34,38), steel; `sotto-fronte` (335,252) → (11,12,13); `giro-000` (450,290) → (14,15,17). Enlarged crops, before and after side by side: `/home/user/exo-runs/m4d-fix/1.21.1-neoforge/ispezione/crepe-dietro-prima-dopo.png` and `crepe-altre-prima-dopo.png`. No model or texture error in any client log; every `log-check.txt` of this round is empty (0 bytes).

The drawer side and lip underside values quoted in `tools/art/README.md` and `docs/arte/README.md` were re-sampled at the same pixels: unchanged ((23,25,27) to (19,20,22) next to (67,72,78); (1,1,1) against (82,62,40)). The new station shots differ from the first round's by 299 to 2 757 pixels each, mostly by 1/255 (smooth lighting is interpolated per vertex, and the faces are split differently) plus single pixels on texel boundaries; nothing else (diff `/home/user/exo-runs/m4d-fix/1.21.1-neoforge/ispezione/diff-davanti-vicino-x3.png`).

### The other three facings (1.21.1-neoforge, `/home/user/exo-runs/m4d-fix/facce.sh`)

Each placed with the item and a right click, as `m4a1_shots.sh` does for north, and checked with `execute if block` on both halves (6 checks, all passed): east (main 9 -60 1, side 9 -60 0), south (main 0 -60 11, side 1 -60 11), west (main -9 -60 1, side -9 -60 2). One front three-quarter shot each, from the main-half end, the north `davanti-vicino` camera turned with the station:

- `/home/user/exo-runs/m4d-fix/1.21.1-neoforge/facce/davanti-est.png`
- `/home/user/exo-runs/m4d-fix/1.21.1-neoforge/facce/davanti-sud.png`
- `/home/user/exo-runs/m4d-fix/1.21.1-neoforge/facce/davanti-ovest.png`

All three look the same as north (`/home/user/exo-runs/m4d-fix/1.21.1-neoforge/ispezione/facce-montaggio.png`): the screen, knobs and lever of the main half on the viewer's left, the side half with the drawers on the right, the joint on the top; nothing visible through the seam. `crepe.py`: 0 on `davanti-sud` and `davanti-ovest`; 1 on `davanti-est`, at (276,114), with grass on all eight neighbours, away from both stations in the frame (`ispezione/est-pixel-x5.png`). Its ray meets the ground at about (-1.05, -60, 0.00), on the z = 0 chunk border, so it is a seam in the vanilla terrain, not the station. The field of view used is calibrated on the same shot: with 77° (70° × 1.1, the flying modifier of a spectator) the near station's top corners (9,-59,0), (9,-59,2), (10,-59,0) project to (528,162), (304,228), (626,195), against the measured extremes of the bronze top (525,163), (305,227), (625,194); with 70° they would be 15-25 pixels off (and the ray would land at (1.77, -60, 0.15), still next to z = 0). The east, south and west seams are inside one chunk (z 0/1, x 0/1, z 1/2); the north one (x 0 / -1, the `m4a1_shots.sh` placement) lies on a chunk border, and no crack showed there in any of the 22 north views.

### Runs, the `ok` lines

`EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh <variant> /home/user/exo-runs/m4d-fix/<variant>/m4a1`, all exit 0, the same measures as before (the block enters no check):

- 1.21.1-neoforge (143 s) and 1.21.1-fabric (127 s): `ok anteprima: ritaglio magenta=18582 riquadro=167x198+604+96; schermata magenta=18582 riquadro=167x198+604+96`, `ok pulsante: pixel=1112 copertura=0.21 mirino=0 barra=0 hud=0 riquadro=36x32+730+410`, `ok rotazione: sagoma pixel=24332 copertura=4.69 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96`, then `OK 1.21.1-neoforge: scatti in /home/user/exo-runs/m4d-fix/1.21.1-neoforge/m4a1` and `OK 1.21.1-fabric: scatti in /home/user/exo-runs/m4d-fix/1.21.1-fabric/m4a1`.
- 1.20.1-forge (144 s) and 1.20.1-fabric (130 s): the same `ok anteprima` and `ok pulsante`; `ok rotazione: sagoma pixel=24587 copertura=4.74 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96`; `OK 1.20.1-forge: scatti in /home/user/exo-runs/m4d-fix/1.20.1-forge/m4a1`, `OK 1.20.1-fabric: scatti in /home/user/exo-runs/m4d-fix/1.20.1-fabric/m4a1`.
- Station shots (`stazione.sh`): exit 0 on the four variants; `stazione_extra.sh` (neoforge) exit 0; `stazione-colori-suoi` (forge, neoforge) exit 0; `facce.sh` (neoforge) exit 0.

### Reference shots

All ten differ from the `704cef7` ones (`compare -metric AE`, whole frame / inside the screen 632x464+164+38): `stazione-posata` 10 570 / - (neoforge) and 11 177 / - (forge); `stazione-aperta`, `girata`, `suggerimento-torso` 1 416 (neoforge) and 1 459 (forge), 0 inside the screen; `stazione-colori-suoi` 2 214 and 2 245, 0 inside the screen. The differences are on the block (1/255 light and texel-boundary pixels), and in the colours shots also a few pixels in the screen corners and on the left, probably run-to-run variation (a guess: the vanilla vignette's brightness is smoothed over time; not verified with a second run). All ten were redone: `convert <run shot> -strip -define png:exclude-chunks=date,time docs/test-in-gioco/m4a1-screenshot/<variant>-<shot>.png`, pixel-identical to the run shots (AE 0 for all ten); the two new `stazione-posata` have no crack pixel.

### PNG chunk check

New or changed PNGs this round: the ten reference shots, `docs/test-in-gioco/m4a1-screenshot/{1.20.1-forge,1.21.1-neoforge}-{stazione-posata,stazione-aperta,girata,suggerimento-torso,stazione-colori-suoi}.png`, 960x540: each only `IHDR,IDAT,IEND` (`/home/user/exo-runs/m4d/blocchi.py`). No generated PNG changed (the eight station textures and the icon are byte-identical to `704cef7`).

### Docs

- `tools/art/README.md`: "Facce senza vertici a T" (two sentences, replacing "Crepe di un pixel"): why (a crack along such an edge shows the sky, since nothing is drawn behind the surface), how (`_conform`, 648 + 432 faces with the same texels), and that `station.py` refuses models where `bench.t_junctions` finds one; the structure note and the selftest list (case 14).
- `tools/art/bench.py` module docstring, `tools/art/station.py` docstring (exit 1 also on a T-junction).
- `docs/test-in-gioco/m4a1-banco.md`, the 4d note: "se sulla stazione lampeggia qualche puntino chiaro mentre ti muovi (lungo gli spigoli degli incassi o fra le due metà), annotalo con uno screenshot (nel cloud non se ne vedono)".

### Commands (exact, on the final tree)

- `./gradlew --console=plain :1.20.1-forge:build ... :1.21.1-neoforge:collectJar` → `BUILD SUCCESSFUL in 33s` (`52 actionable tasks: 24 executed, 28 up-to-date`; the four `test` tasks executed).
- JUnit count → `1.20.1-forge 381`, `1.20.1-fabric 381`, `1.21.1-fabric 381`, `1.21.1-neoforge 381`; skipped + failures + errors = 0.
- `bash tools/check_sources.sh` → `OK sources`; `bash tools/check_jars.sh` → `OK jars`; `bash tools/check_data.sh` → `OK data`; `bash tools/check_art.sh` → `OK art`; `bash tools/client/client_shot.sh --selftest` → `OK passi`.
- The four jars in `build/libs/all/` carry the twelve station assets byte-equal to the committed ones (the new models included).
- No gametests and no dedicated server this round, as the fix brief says.

### Concerns

1. Two frames still have sky-coloured pixels, neither on the station: the far horizon in 1.20.1-fabric `dietro-vicino.png` (two pixels, identical before and after the fix) and one terrain pixel on the z = 0 chunk border in `davanti-est.png`. The brief's "0 everywhere" holds for every station pixel, not for those two frames. The commit message says "nessun pixel del colore del cielo negli scatti della stazione", written before the Fabric result: this report is the precise statement (not amended).
2. The face count is 648 + 432, above the brief's 576/316: that estimate is each half conformed on its own, while the seam conforms too here, as required; all 64 merge orders give the same count.
3. Six to eight times the faces: 648 + 432 quads for one station (from 150). It renders fine on the four variants in the cloud; a world with many stations has proportionally more quads to draw, and the two model files grow from 18.8 KB to 134.4 KB (uncompressed; `OK jars` still passes).
4. The north placement's seam lies on a chunk border (x 0 / -1), where the two halves are drawn in different chunk sections and their shared vertices could differ by rounding; no crack showed there in 22 views, but it cannot be excluded by construction (vanilla blocks across chunk borders have the same exposure).
