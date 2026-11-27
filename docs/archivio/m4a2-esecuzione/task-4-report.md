# Task 4 report: the 4a sequence on the four variants, the reference shots, the dedicated server

Status: DONE_WITH_CONCERNS (one documentation pointer for Task 5, see "Concerns"). One commit on `m4-stazione`, not
pushed:

- `24c26d3` tools: m4a_shots.sh con le misure delle quattro varianti; docs: scatti di riferimento della 4a

The commit is signed (`gpgsig` present), has the environment identity and both trailers. `git status --short` after
the commit is empty. Logs, shots and helper scripts are under
`<scratchpad>/m4a2-task4/` (written `<S>/` below).

## Commit used by each run

Every real-client and server run was made at the committed head `d0a5342` (BASE, all of Tasks 1-3), before any edit.
Each run log starts with `git rev-parse --short HEAD` and `git status --short`:

| Run | `HEAD` | `git status --short` |
|---|---|---|
| Step 2, `m4a_shots.sh` on the four variants (`<S>/logs/step2-<v>.log`) | `d0a5342` | empty |
| Step 3, the two `stazione-colori-suoi` runs (`<S>/logs/step3-<v>.log`) | `d0a5342` | empty |
| Step 4, the four dedicated servers (`<S>/logs/step4-<v>.txt`) | `d0a5342` | only `?? docs/test-in-gioco/m4a-screenshot/` (the ten stripped PNGs, no code) |

The edits (the header comment of `m4a_shots.sh`, `tools/client/README.md`) came after all ten runs. The script change
is comments only: `git diff -U0 d0a5342 HEAD -- tools/client/m4a_shots.sh`, minus the `#` lines, has 0 changed lines.
After the edit, `M4A_SOLO_CONTROLLI=1` on the four shot folders gives the same eight `ok` lines and `OK` as the runs
(`diff` of the `ok`/`OK` lines: identical on all four).

## Step by step

- **Step 1.** `bash tools/client/client_shot.sh --selftest` gives `OK passi` (exit 0). `client_shot.sh` is unchanged
  (`git diff d0a5342 HEAD -- tools/client/client_shot.sh` is empty).
- **Step 2.** Before the first run I ran `./gradlew --stop`, which freed the idle daemon. Then I ran
  `EXO_ACCEPT_EULA=true bash tools/client/m4a_shots.sh <v> <S>/shots-<v>` once per variant, in the order 1.20.1-forge,
  1.20.1-fabric, 1.21.1-fabric, 1.21.1-neoforge, through `<S>/run_seq.sh`. The wrapper records the head, the status,
  the exit code, the durations and the size of `log-check.txt`. All four were green on the first attempt; I made no
  reruns.
- **Step 3.** I ran the brief's `client_shot.sh` command (17 steps, `stazione-colori-suoi`) on 1.20.1-forge and
  1.21.1-neoforge through `<S>/run_colori.sh`, into the separate folders `<S>/colori-<v>`. Then I ran the brief's
  `convert … -strip -define png:exclude-chunks=date,time` for the five shots of each variant.
- **Step 4.** I started and stopped the dedicated server on the four variants, one at a time, through
  `<S>/dedicated.sh`. That is the 4a-1 helper's procedure, with bounded waits:
  - `eula=true` in `versions/<v>/run/eula.txt` (it was already there);
  - `rm -rf versions/<v>/run/world`;
  - `./gradlew --console=plain :<v>:runServer`, with stdin from a FIFO, until `Done (`, then `stop`;
  - `python3 -I tools/check_log.py client --variant <v> versions/<v>/run/logs/latest.log`.
- **Step 5.** I updated the `m4a_shots.sh` paragraph in `tools/client/README.md`:
  - the table now says "sulle quattro varianti" for the six new checks;
  - the side-on shots run from `girata` to `rinforzato`;
  - the `kill` lands "entro il secondo dopo";
  - the reference shots are now the five in `m4a-screenshot/`, and the 4a-1 ones are kept as history;
  - "Quanto dura" has the four-variant durations of `m4a_shots.sh` and the new `stazione-colori-suoi` ones.

  `grep -c 'm4a_shots' tools/client/README.md` gives 8.
- **Step 6.** The end-of-task commands (below).
- **Step 7.** I committed with `git add` and explicit paths: the two tool files and the ten PNGs.

## Step 2: the sequence on the four variants (at `d0a5342`)

| Variant | exit | `log-check.txt` | total | `client_shot.sh` ("fatto in") | `OK` line |
|---|---|---|---|---|---|
| 1.20.1-forge | 0 | 0 bytes | 146 s | 138 s | `OK 1.20.1-forge: scatti in <S>/shots-1.20.1-forge` |
| 1.20.1-fabric | 0 | 0 bytes | 135 s | 127 s | `OK 1.20.1-fabric: scatti in <S>/shots-1.20.1-fabric` |
| 1.21.1-fabric | 0 | 0 bytes | 136 s | 128 s | `OK 1.21.1-fabric: scatti in <S>/shots-1.21.1-fabric` |
| 1.21.1-neoforge | 0 | 0 bytes | 133 s | 123 s | `OK 1.21.1-neoforge: scatti in <S>/shots-1.21.1-neoforge` |

The 1.20.1-forge run was the first after `./gradlew --stop`, with cold daemons.

The eight `ok` lines are the same on all four variants, except the sagoma of `rotazione`. That is 24503 on the two
1.20.1 variants and 24245 on the two 1.21.1, with coverage 4.73 and 4.68, as before. Verbatim from 1.20.1-forge:

```
ok anteprima: ritaglio magenta=18582 riquadro=167x198+604+96; schermata magenta=18582 riquadro=167x198+604+96
ok rotazione: sagoma pixel=24503 copertura=4.73 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96
ok scelta: schermata pixel=972 riquadro=232x88+222+78; ritaglio pixel=0 riquadro=0x0+0+0
ok smontaggio: posto dell'elmo pixel=952 riquadro=40x40+224+80; ritaglio da magenta=9225 riquadro=111x198+629+96 a magenta=8728 riquadro=111x192+629+102 (cala di 497); schermata magenta=8728 riquadro=111x192+629+102
ok montaggio: ritaglio pixel=0 riquadro=0x0+0+0; posto dell'elmo pixel=0 riquadro=0x0+0+0; schermata pixel=648 riquadro=28x28+218+454
ok lucchetto: schermata pixel=144 riquadro=28x20+750+46
ok vista: schermata pixel=144 riquadro=28x20+750+46; lucchetto contro montato pixel=0 riquadro=0x0+0+0
ok rinforzato: schermata pixel=552 riquadro=28x24+716+44
```

The other three variants, verbatim from `<S>/logs/step2-<v>.log`:

```
#### 1.20.1-fabric
ok anteprima: ritaglio magenta=18582 riquadro=167x198+604+96; schermata magenta=18582 riquadro=167x198+604+96
ok rotazione: sagoma pixel=24503 copertura=4.73 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96
ok scelta: schermata pixel=972 riquadro=232x88+222+78; ritaglio pixel=0 riquadro=0x0+0+0
ok smontaggio: posto dell'elmo pixel=952 riquadro=40x40+224+80; ritaglio da magenta=9225 riquadro=111x198+629+96 a magenta=8728 riquadro=111x192+629+102 (cala di 497); schermata magenta=8728 riquadro=111x192+629+102
ok montaggio: ritaglio pixel=0 riquadro=0x0+0+0; posto dell'elmo pixel=0 riquadro=0x0+0+0; schermata pixel=648 riquadro=28x28+218+454
ok lucchetto: schermata pixel=144 riquadro=28x20+750+46
ok vista: schermata pixel=144 riquadro=28x20+750+46; lucchetto contro montato pixel=0 riquadro=0x0+0+0
ok rinforzato: schermata pixel=552 riquadro=28x24+716+44
OK 1.20.1-fabric: scatti in <S>/shots-1.20.1-fabric
#### 1.21.1-fabric
ok anteprima: ritaglio magenta=18582 riquadro=167x198+604+96; schermata magenta=18582 riquadro=167x198+604+96
ok rotazione: sagoma pixel=24245 copertura=4.68 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96
ok scelta: schermata pixel=972 riquadro=232x88+222+78; ritaglio pixel=0 riquadro=0x0+0+0
ok smontaggio: posto dell'elmo pixel=952 riquadro=40x40+224+80; ritaglio da magenta=9225 riquadro=111x198+629+96 a magenta=8728 riquadro=111x192+629+102 (cala di 497); schermata magenta=8728 riquadro=111x192+629+102
ok montaggio: ritaglio pixel=0 riquadro=0x0+0+0; posto dell'elmo pixel=0 riquadro=0x0+0+0; schermata pixel=648 riquadro=28x28+218+454
ok lucchetto: schermata pixel=144 riquadro=28x20+750+46
ok vista: schermata pixel=144 riquadro=28x20+750+46; lucchetto contro montato pixel=0 riquadro=0x0+0+0
ok rinforzato: schermata pixel=552 riquadro=28x24+716+44
OK 1.21.1-fabric: scatti in <S>/shots-1.21.1-fabric
#### 1.21.1-neoforge
ok anteprima: ritaglio magenta=18582 riquadro=167x198+604+96; schermata magenta=18582 riquadro=167x198+604+96
ok rotazione: sagoma pixel=24245 copertura=4.68 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96
ok scelta: schermata pixel=972 riquadro=232x88+222+78; ritaglio pixel=0 riquadro=0x0+0+0
ok smontaggio: posto dell'elmo pixel=952 riquadro=40x40+224+80; ritaglio da magenta=9225 riquadro=111x198+629+96 a magenta=8728 riquadro=111x192+629+102 (cala di 497); schermata magenta=8728 riquadro=111x192+629+102
ok montaggio: ritaglio pixel=0 riquadro=0x0+0+0; posto dell'elmo pixel=0 riquadro=0x0+0+0; schermata pixel=648 riquadro=28x28+218+454
ok lucchetto: schermata pixel=144 riquadro=28x20+750+46
ok vista: schermata pixel=144 riquadro=28x20+750+46; lucchetto contro montato pixel=0 riquadro=0x0+0+0
ok rinforzato: schermata pixel=552 riquadro=28x24+716+44
OK 1.21.1-neoforge: scatti in <S>/shots-1.21.1-neoforge
```

Thresholds are unchanged and all measures pass them: `SEL_MIN` 480 (972), `PLACE_MIN` 470 (952), `HELMET_MIN` 240
(497), `LOCK_MIN` 70 (144), `SHIELD_MIN` 270 (552). The stop rule did not trigger.

**RCON, the same on all four** (`<S>/shots-<v>/rcon.log`):
- 13 `Test passed`: the 13 `execute if/unless` of the sequence.
- `Took 1 recipes`.
- In the montaggio phase, in order:
  - `execute unless data entity Exo Inventory[{Slot:1b}]` gives `Test passed`;
  - `execute unless entity @e[type=minecraft:item]` gives `Test passed` (the release outside the screen drops nothing);
  - `execute if data … Slot:1b … scrap_helmet` gives `Test passed, count: 1`.
- Command replies: `Exoframe locked: only its owner can use it`, `Reinforced: true`,
  `Exoframe removed; its contents are on the ground`, `Killed 7 entities`.
- The `kill` lands in the same or the next integer second after `/exoframe remove`: forge 111→112, 1.20.1-fabric
  100→101, 1.21.1-fabric 101→101, neoforge 96→97.

**Pixel comparisons inside the screen rectangle `632x464+164+38`** (ImageMagick `compare -metric AE`):
- The two variants of each version are identical (0) in all nine screen shots: `stazione-aperta`, `girata`,
  `suggerimento-torso`, `scelta-torso`, `smontato`, `montato`, `sbloccato`, `bloccato` and `rinforzato`.
- `stazione-aperta` (the suit from the front) is identical across all four variants.
- From `girata` to `rinforzato` (the suit side-on), 1.20.1-forge and 1.21.1-neoforge differ:
  - in 11906 pixels, and 11470 in `smontato` (no helmet);
  - in at most 1/255 per channel, so the AE is 0 at 4 % fuzz;
  - all inside `114x198+626+96`, within the preview clip.

The header comment and the README now say this, for every side-on shot.

**Shots I looked at:**
- 1.20.1-forge and 1.21.1-neoforge: `stazione-aperta`, `scelta-torso`, `smontato` and `rinforzato`.
- 1.20.1-fabric: `montato`, `sbloccato` (the lock open) and `bloccato` (the lock closed).
- 1.21.1-fabric: `telaio-tolto` (no screen, "The exoframe is no longer available at the station", the iron
  advancement) and `nessun-telaio` (the "No exoframe nearby…" line).

All match the descriptions in the header comment.

## Step 3: the reference shots

| Variant | exit | `log-check.txt` | total | `client_shot.sh` |
|---|---|---|---|---|
| 1.20.1-forge | 0 | 0 bytes | 90 s | 88 s |
| 1.21.1-neoforge | 0 | 0 bytes | 80 s | 78 s |

The two `stazione-colori-suoi` shots are identical inside the screen (AE 0). They show the Scrap suit in its own
colours, the helmet selected, the lock closed, "All" and the arrow, and no test button.

The ten files in `docs/test-in-gioco/m4a-screenshot/`:
- `1.20.1-forge-{stazione-aperta,scelta-torso,smontato,rinforzato,stazione-colori-suoi}.png`;
- `1.21.1-neoforge-{stazione-aperta,scelta-torso,smontato,rinforzato,stazione-colori-suoi}.png`.

Checks on them:
- **Metadata.** The 3d plan's snippet (`python3 -I -c 'import struct, sys …' docs/test-in-gioco/m4a-screenshot/*.png`)
  prints nothing and exits 0, run before and after the commit. The only chunk kinds left are `IDAT`, `IEND` and `IHDR`.
- **Pixels.** Each file is pixel-identical to its source (`compare -metric AE` = 0 for all ten). Each is 960×540,
  8-bit. No two files have the same md5.
- **Looked at.** I looked at the ten sources, and at one committed file directly. Each shows the right screen and
  nothing unexpected: no chat line, no toast, no tooltip, the pointer outside the screen.

The 4a-1 shots in `docs/test-in-gioco/m4a1-screenshot/` are untouched.

## Step 4: the dedicated server (at `d0a5342`)

| Variant | `Done (` line (latest.log) | `Done (` after | gradle | `check_log.py` |
|---|---|---|---|---|
| 1.20.1-forge | `[27Nov2026 21:35:51.883] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (2.119s)! For help, type "help"` | 13 s | exit 0, `BUILD SUCCESSFUL in 14s` | no output, exit 0 |
| 1.20.1-fabric | `[21:36:09] [Server thread/INFO] (Minecraft) Done (2.089s)! For help, type "help"` | 11 s | exit 0, `BUILD SUCCESSFUL in 12s` | no output, exit 0 |
| 1.21.1-fabric | `[21:36:21] [Server thread/INFO] (Minecraft) Done (0.691s)! For help, type "help"` | 10 s | exit 0, `BUILD SUCCESSFUL in 11s` | no output, exit 0 |
| 1.21.1-neoforge | `[27Nov2026 21:36:34.368] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (0.604s)! For help, type "help"` | 11 s | exit 0, `BUILD SUCCESSFUL in 12s` | no output, exit 0 |

What each server log shows:
- one `Stopping server` and one `Redstone Exoframes init`;
- 0 `Config file` lines, and the dev config is unchanged (sha `208821b4…` before and after);
- 0 `Object did not get ID it asked for`;
- `No data fixer registered for exoframe` once on the two Fabric variants and never on Forge and NeoForge, which is
  allowed.

The logs are copied to `<S>/logs/step4-<v>-latest.log`.

## Step 6: end-of-task outputs (final tree; the quick checks repeated on the committed head `24c26d3`)

- **Build.** The exact four-variant build with `collectJar` gives `BUILD SUCCESSFUL in 7s`, with "52 actionable
  tasks: 52 up-to-date". No Java changed, so the four `:<v>:test` tasks were `UP-TO-DATE`.
- **JUnit.** For fresh results I re-ran them with
  `./gradlew --console=plain :1.20.1-forge:test --rerun :1.20.1-fabric:test --rerun :1.21.1-fabric:test --rerun
  :1.21.1-neoforge:test --rerun`. That gives `BUILD SUCCESSFUL in 16s`, with "4 executed".
- **JUnit count**, with the plan's command, on the fresh XML (54 files per variant, all written in the rerun,
  0 failures, 0 errors): `1.20.1-forge 407`, `1.20.1-fabric 407`, `1.21.1-fabric 407`, `1.21.1-neoforge 407`. That is
  N₀ + 18 = 389 + 18.
- **The six checks.** Each prints only its `OK` line and exits 0, on the final tree and again on `24c26d3`:
  - `bash tools/check_sources.sh` → `OK sources`;
  - `bash tools/check_jars.sh` → `OK jars`;
  - `bash tools/check_data.sh` → `OK data`;
  - `bash tools/check_art.sh` → `OK art`;
  - `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure`;
  - `bash tools/client/client_shot.sh --selftest` → `OK passi`.
- **In-world tests.** `bash tools/run_gametests.sh` exits 0 in 115 s:

  ```
  OK 1.20.1-forge: 256 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 256 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 256 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 256 test obbligatori passati, nessun avviso fuori elenco
  ```
- **Status.** `git status --short` after the commit is empty: no shot outside `docs/test-in-gioco/m4a-screenshot/`.
  `git diff d0a5342 HEAD -- tools/art src` is empty.

The last README rewording ("dove c'è la schermata, ha ancora il pulsante di prova …") came after the build and the
in-world run. It is a document-only change. The six checks above were re-run on the committed head `24c26d3`.

## The two open points

1. **`docs/compat.md:255`** says: "Lo prova il client vero sulle quattro varianti con `tools/client/m4a_shots.sh`: un
   clic destro sulla stazione apre la schermata con l'anteprima della tuta, e i clic sul pannello del telaio scelgono,
   smontano e montano." **It holds as written.** At `d0a5342`, on each of 1.20.1-forge, 1.20.1-fabric, 1.21.1-fabric
   and 1.21.1-neoforge:
   - **The screen opens with the preview.** `anteprima` ok: 18582 magenta in the clip, the same in the whole screen.
   - **The click selects.** `scelta` ok: 972 pixels inside `408x236+178+72`, 0 in the clip.
   - **Shift-click unmounts.** `smontaggio` ok: 952 in the helmet place, the magenta drops by 497. RCON: the helmet is
     in hotbar slot 1.
   - **The cursor click mounts.** `montaggio` ok: 0 in the clip, 0 in the place, 648 only in hotbar slot 1. RCON: the
     helmet is on the cursor, nothing is on the ground after the release outside the screen, and the helmet is back in
     slot 1.

   The same paragraph's sibling sentences also hold on the four variants. These are "Nel client vero … sceglie il
   torso, smonta l'elmo con le maiuscole e lo rimonta dal cursore, lasciando il tasto fuori dalla schermata senza che
   nulla cada" and the lock and shield sentence (`lucchetto`, `vista`, `rinforzato` ok on all four). I did not edit
   `docs/compat.md`.
2. **Task 2's runs were on an uncommitted tree.** My runs are the first at a committed head containing all of
   Tasks 1-3. All ten real-client and server runs used **`d0a5342`**, recorded in each run log by
   `git rev-parse --short HEAD`, with the status as in the table at the top. The eight checks there give exactly the
   measures Task 2 reported (972, 952/497, 0/0/648, 144, 144/0, 552) on all four variants. So the uncommitted-tree
   runs of Task 2 are confirmed at the committed head.

## Files changed

- `tools/client/m4a_shots.sh`: comments only. The "Dura …" paragraph of the header now gives:
  - the four durations and the four `client_shot.sh` durations;
  - the side-on cross-version difference for every shot from `girata` to `rinforzato` (11906 pixels, 11470 in
    `smontato`, all in the clip);
  - "le stesse sulle quattro varianti" for the six new checks' measures;
  - the `kill` timing "entro il secondo dopo" on the four variants.

  The thresholds' comments are unchanged. Their preamble already says each measure is "lo stesso sulle quattro varianti
  dove non e' detto altro", which is now measured.
- `tools/client/README.md`:
  - the `m4a_shots.sh` table: "sulle quattro varianti" for `scelta`, `smontaggio` (both measures), `montaggio`,
    `lucchetto`, `vista` and `rinforzato`;
  - the paragraph after the table: the side-on range and the `kill` timing;
  - the "OK non dice…" paragraph: the 4a reference shots in `docs/test-in-gioco/m4a-screenshot/`, and the 4a-1 ones
    kept in `m4a1-screenshot/` as the history of `m4a1-banco.md`;
  - "Quanto dura": the `m4a_shots.sh` durations of the four variants, and `stazione-colori-suoi` at 80 s on
    neoforge and 90 s on forge.
- `docs/test-in-gioco/m4a-screenshot/`: ten new PNGs, without metadata.

## Self-review

- **Completeness.** Steps 1-7 are done, and both open points are answered above. The README paragraph keeps the form
  of the others: phases, the table of the eight checks with measured values, the shots, "non prova", and the durations
  in "Quanto dura", as for the other sequences.
- **Discipline.**
  - No code changed. The art and textures are untouched (`OK art`). `docs/compat.md`, the root `README.md`,
    `tools/README.md` and `m4a1-screenshot/` are untouched.
  - No plan, decision or review IDs, and no model or vendor names, in the added lines (scanned).
  - The comments are Italian with ASCII apostrophes. All script lines are within 120 columns. `bash -n` is clean.
- **Evidence.** Every expected output is captured in `<S>/logs/`:
  - `step2-<v>.log`, `solo-<v>.log`, `step3-<v>.log`;
  - `step4-<v>.txt`, `step4-<v>-latest.log`, `step4-<v>-gradle.log`;
  - `step6-build.log`, `step6-junit-rerun.log`, `step6-junit-count.txt`, `step6-checks.log`, `step6-gametests.log`;
  - `step7-postcommit-checks.log`.

## Concerns

1. **Two pointers to the 4a-1 checklist stay in `tools/client/README.md`. Task 5's file list does not include this
   file.** I left both as they are, because `m4a-stazione.md` does not exist yet:
   - the last sentence of the `m4a_shots.sh` paragraph: "Sono nella checklist per il PC,
     `docs/test-in-gioco/m4a1-banco.md`";
   - the "Cosa non copre" bullet on the station: "Li prova la checklist `docs/test-in-gioco/m4a1-banco.md`".

   After Task 5 they should name `docs/test-in-gioco/m4a-stazione.md`. Some of the items they list are only in the new
   checklist: the release over a slot, and the lock and shield tooltips. Suggestion: a pointer in Task 5's dispatch.
   The reference-shot sentence I rewrote says "la checklist della parte 4a" without a file name, so it holds before and
   after Task 5.

   The full list, from `grep -rn 'm4a1-screenshot\|m4a1-banco' README.md tools docs/compat.md` at `24c26d3`:
   - **The two above**, `tools/client/README.md:170` (the last sentence) and `:252`. These are the concern.
   - **The sentence I wrote**, `tools/client/README.md:170`: the 4a-1 shots "restano in
     `docs/test-in-gioco/m4a1-screenshot/`, come storia della sua checklist, `m4a1-banco.md`". It is meant to stay.
   - **`README.md:9`** ("Stato") and **`README.md:60`** ("Documenti": `m4a1-banco.md` and `m4a1-screenshot/`). Task 5
     already rewrites these two paragraphs.
   - **`tools/art/README.md:441`, `tools/art/preview/station.js:12`, `tools/art/preview/gui_preview.py:193,354,366`.**
     The art tools compare against the 4a-1 `stazione-posata` and `stazione-colori-suoi` shots, which stay. `tools/art/`
     must not change, and the paths still resolve.

   `grep -n 'su 1.21.1-neoforge e su 1.20.1-forge' tools/client/README.md tools/client/m4a_shots.sh` finds nothing.
   The only similar wording, `tools/client/README.md:194`, is about the m4c vignette.

   After the work, `ps` shows no game JVM and no Xvfb: only one idle Gradle daemon (the one from the end-of-task build).
2. **JUnit test tasks.** In the exact end-of-task build the JUnit tasks were `UP-TO-DATE`, since no Java changed. The
   407 count comes from a fresh `--rerun` of the four test tasks, with every XML written by that rerun.
