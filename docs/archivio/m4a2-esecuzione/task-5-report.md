# Task 5 report — la checklist della 4a, i documenti e la verifica finale (parte 4a-2)

Status: DONE_WITH_CONCERNS. One commit on `m4-stazione`, not pushed:

- `d0d454b` docs: checklist della 4a (m4a-stazione.md), nota in testa alla checklist della 4a-1, documenti della parte 4a-2

The commit is signed (`gpgsig` present). It has the environment identity and both trailers. `git status --short` after
the commit is empty. Logs are under `<scratchpad>/m4a2-task5/`
(written `<S5>/` below).

The concerns are in their own section: the allowlist count in Step 4, the hidden action-bar messages, and choices
beyond the brief.

## History: the stop, and the coordinator's rulings

Before writing anything I stopped (NEEDS_CONTEXT, tree clean at `24c26d3`). Two brief steps could not pass as written,
and one 4a-1 bench step was missing. The coordinator answered; all answers are binding:

- **Q1, step (21).** Use the death path. A normal exit gives the armour back (`FrameEntry.exit` → `returnStowed`);
  only `onDeath` leaves it in the frame. The step must say what to note if the row or the helmet does not appear.
- **Q2, step (19).** Split the step:
  - Step 19 is for one player: the lock button, both messages, the iron-door sounds, then `/exoframe lock` with the
    screen closed and reopen.
  - The live update with the screen open moves to step (23): the host runs `/exoframe debug reinforced true` while the
    second player's screen is open.
  - Without a second player it is "non provato". "Già verificato" says that `vista` and `rinforzato` prove the live
    update in the cloud.
- **Q3.** The 4a-1 reach test goes into step (9) as sub-steps, before the frame goes on top, keeping the numbering.
- **Notes:**
  - step (23) opens the LAN with "Comandi: No" and says why;
  - every step that expects a station action-bar message tells the assistant to press Esc right after the click and to
    note whether the message was readable;
  - a new owner question covers where the station should show its messages, worded as a known limit.

## What I did, step by step

### Step 1 — the checklist `docs/test-in-gioco/m4a-stazione.md` (new, 236 lines)

It follows the form of `m4a1-banco.md` and `m4c-hud.md`, and it is written for the assistant on the PC, who cannot ask
questions.

**Header.** It is for the assistant on the PC, with the four production jars of the whole 4a (branch `m4-stazione`, or
`main` after the merge). To recognise the jars: in the station screen there are the lock and "Tutti" ("All"), and no
test button.

**Cosa c'è da vedere.** It keeps the 4a-1 list without the test button, and adds the 4d look of the block and the
screen. It describes:
- the selection ("Tutti" stays bronze while chosen; the white ring and the header; an empty place shows its name and
  "Vuoto");
- mounting from the cursor ("Quello slot è già occupato"; a wrong place does nothing);
- unmounting with Shift or with the arrow, into the inventory, or at the feet when it is full;
- the cell mounted, swapped and unmounted;
- the ghosts of empty places;
- the row "Custodisce un'armatura";
- the lock, with its tooltips and the iron-door sounds, and the shield;
- the picked-up frame item: "Sbloccato" and "Rinforzato", never the name, and the owner rule (the owner's answer of
  27 November 2026);
- creative mode (the piece stays on the cursor);
- the hidden action-bar messages, as a known limit;
- every tooltip text, in Italian and English.

**Cosa non c'è ancora.** The 4b: repair, kit, painting and presets, so the palette tray, the preset rows and the cost
row stay empty, and "Tutti" only selects. The 4e animations.

**Già verificato (27 novembre 2026).**
- In-world tests by class, with counts: `StationActionGameTests` 16, `FrameItemGameTests` 5, `StationMenuGameTests` 17,
  `StationBlockGameTests` 10, and `DataGameTests` with the station.
- The new JUnit: `StationActionTest` 6, `TickGateTest` 4, `StationViewTest` +1, `StationLayoutTest` +6, `SuitViewTest`
  +1.
- The eight `m4a_shots.sh` checks, with measures and "sì" on the four variants.
- The live-update note (Q2) and the dedicated server.
- The five `m4a-screenshot/` pairs and the three 4a-1 shots still used, with what differs in each.
- What the cloud does not prove: production jars, sounds, tooltips seen by a person, a real second player, the release
  over a slot, death and exit with the screen open, a real explosion, other scales, F3+T, the 3c config file, and
  more.

**0. Preparazione.** A-F are written out, C included (the 3c config file before the first start), not referred back to
`m4a1-banco.md`.
- C also deletes a file left by an earlier step 28 (`ownerOnlyFrames": *false`). Its `.prima` copy serves steps 2 and
  28.
- F gives both labels for allowing commands: "Allow Cheats" on 1.20.1 and "Allow Commands" on 1.21.1 (keys
  `selectWorld.allowCommands` and `.new`).
- A closing paragraph says:
  - which steps to do on which variants: 1-22, 27 and 29 on all four; 24-26 on one 1.20.1 and one 1.21.1; 28 once;
    23 once if there is a second player;
  - the log command (step 1's patterns plus `Cannot unmount`);
  - the note on the hidden messages, and on "La stazione non è riuscita…".

**The 29 steps, in the brief's order.**
- **1-8.** As in the 4a-1, plus:
  - in step 4, a walk-around look check;
  - in step 5's creative bullet, the station placed facing east, south and west (the 4d note's orientation check).
- **9.** Q3: the reach test as sub-steps (2nd block opens, 3rd does not, a hole opens; then the hole is filled), then
  the frame on top as in the 4a-1, with ghosts.
- **10.** Compared with `m4a-screenshot/<v>-stazione-colori-suoi.png`.
- **11.** Tooltips of the places, the cell, the preview, the arrow and the lock. It also says that "Tutti" and the
  empty title row show nothing.
- **12-13.** As in the 4a-1: the rotation, its two release sub-steps, and Shift-click.
- **14.** The selection, compared with `scelta-torso`.
- **15.** Unmounting:
  - Shift-click on the helmet, compared with `smontato`;
  - the arrow on the left arm, giving "Braccio sinistro" / "Vuoto";
  - then everything else, so that every ghost is seen.
- **16.** Mounting in survival: the right place, a wrong place, an occupied place (with the Esc instruction), and the
  cell swap with a second, empty cell. Then the creative case.
- **17.** In creative, the release with a piece on the cursor: outside the screen and over an empty slot, then the
  same pressed on "Tutti".
- **18.** In survival, a full inventory: the piece lies at the feet.
- **19.** Q2, alone: the lock button, both messages with Esc, the iron-door sounds, then `/exoframe unlock|lock` with
  the screen closed and reopening.
- **20.** The shield and its tooltip, compared with `rinforzato`, then set back to `false`.
- **21.** Q1, the death path. It says what to note if the frame, the row, its tooltip or the helmet is missing.
- **22.** Pick-up and placing again, in survival: the tooltip with "Sbloccato" and "Rinforzato" and no name; picked up
  again it still has them. Then the `/give` with `{exoframes:{cell:{energy:500}}}` in each version's form is refused.
- **23.** A second player, on a LAN opened with "Comandi: No" (the step says why):
  - the lent frame stays the host's ("Questo telaio non è tuo");
  - two players at the same station; for the second, the lock is off with "Solo il proprietario può cambiarlo";
  - the live update by command (Q2);
  - the host's lock closes the second player's screen;
  - kick with a stack on the cursor.
- **24-26.** As in the 4a-1, with the label "Scala".
- **27.** The sounds, with `/playsound` references for the iron trapdoor and the iron door.
- **28.** `ownerOnlyFrames` off, once:
  - first the log check, because the restart starts a new `latest.log`;
  - then the brief's `Set-Content`, the tooltip line, and the `Copy-Item` restore with a hash check.
- **29.** The log command of step 1. It says what the log covers on the step-28 variant.

**Cosa riportare** and **Domande per il proprietario.**
- Question 5 (the effect of `rinforzato`) and question 7 (the station's recipe) are open.
- Question 11 comes with its 27 November 2026 answer.
- A new question asks where the station should show its messages.
- The choices to show are design §8 points 2, 3, 7, 11, 22, 25 and 27.

Facts checked against code and game files while writing:
- **Labels.** LAN and options labels come from `it_it.json` in the asset index and from `en_us.json` in the two client
  jars (`lanServer.*`, `selectWorld.allowCommands(.new)`, `options.guiScale` = "Scala").
- **LAN permissions.** Checked with javap on V20 and V21: `PlayerList.isOp`, `MinecraftServer.getProfilePermissions`,
  and `IntegratedServer.publishServer` (it sets only `allowCheatsForAllPlayers`). `ShareToLanScreen` starts "Comandi"
  from the world's own value.
- **Action bar position.** javap of `Gui`: the action bar is drawn at `guiHeight − 68 − 4`, inside the opaque screen.
  Task 4's `sbloccato.png` confirms it.
- **The `/give` data.** `ItemDataAccessImpl.frame` and `ItemPayload.read` accept `{exoframes:{cell:…}}` on 1.20.1 and
  `custom_data` on 1.21.1.
- **Sounds.** The `/playsound` ids exist in both versions' `sounds.json`. `/exoframe lock|unlock` plays no sound;
  `StationActions.toggleLock` does.
- **Cell tooltips.** An empty cell's item tooltip is "Vuota: caricala in un caricatore" (`CellItem.tooltip`).
- **Messages.** "Tutti" has no tooltip; the arrow's tooltip shows when it is off; `/kill` and LAN-kick messages come
  from the game.

Runs:
```
$ grep -c '^- \[ \] \*\*[0-9]*\.\*\*' docs/test-in-gioco/m4a-stazione.md
29
$ grep -n '^- \[ \] \*\*1\.\*\*' docs/test-in-gioco/m4a-stazione.md
105:- [ ] **1.** Avvia il client fino al menu principale ...      (one line)
$ for f in $(grep -o 'm4a1\?-screenshot/[A-Za-z0-9._-]*\.png' docs/test-in-gioco/m4a-stazione.md | sort -u); do [ -f "docs/test-in-gioco/$f" ] || echo "manca $f"; done
(no output; 16 distinct shots named, 10 in m4a-screenshot/ and 6 in m4a1-screenshot/)
$ grep -c 'Scala dell.interfaccia' docs/test-in-gioco/m4a-stazione.md
0
```
A scan of the file for plan, decision, review or ruling IDs, and for model or vendor names, is empty.
`git diff --check` is clean.

### Step 2 — the note in `m4a1-banco.md`, `README.md`, `tools/README.md`, `tools/client/README.md`

- **`m4a1-banco.md`.** One note after the existing ones, in their form ("> **Con i jar di dopo l'unione della parte
  4a-2** (quelli del branch `m4-stazione` o di `main` dopo l'unione) …"). It says:
  - the test button is gone: step 14 is not done, and its lines do not hold (it names where they are);
  - the screen has the lock, "Tutti" and the arrow;
  - clicks select, mount, unmount and lock;
  - empty places have ghosts and "Vuoto";
  - the whole-4a checklist is `m4a-stazione.md`, and this one stays as history.

  The steps are unchanged.
- **`README.md`:**
  - "Stato": the 4a-1 sentence loses "in sola lettura", the test button and the stale "Le azioni … arrivano con le parti
    successive" with its m4a1-banco pointer; after the 4c sentence comes the brief's 4a-2 sentence, closing on
    "aspetta la conferma sul PC … `docs/test-in-gioco/m4a-stazione.md`";
  - "oggi 235 test" → "oggi 256 test";
  - line 38: `m4a_shots.sh` now lists the lock and shield phases and says "otto controlli";
  - "Documenti": `m4a-stazione.md` with `m4a-screenshot/`, and `m4a1-banco.md` with `m4a1-screenshot/` kept as
    history; the plans list now runs to M4a-2.
- **`tools/README.md`:** no change needed. Task 1 had already replaced `m4a1_shots.sh` with `m4a_shots.sh` (line 6).
- **`tools/client/README.md`:** only the two pointers (`:170` and `:252`) go from `m4a1-banco.md` to `m4a-stazione.md`.
  The history sentence about `m4a1-screenshot/` stays.

Runs:
```
$ grep -c "Con i jar di dopo l'unione della parte 4a-2" docs/test-in-gioco/m4a1-banco.md
1
$ grep -c '235 test' README.md
0
$ grep -rn 'm4a_shots' README.md tools/README.md tools/client/README.md | wc -l
10
```

### Step 3 — `docs/compat.md`, re-read against the code

I read these entries against the code.
- **"Menu e schermate"**, against `StationMenu.clickMenuButton`, `StationActions.press`/`toggleLock`, `FrameFitting`
  (`mountPiece` order, `changed`), `FramePermissions`, `StationScreen`, `StationLayout`, `StationHeader` and
  `StationFramePanel`. It covers the buttons, the selection, `FrameFitting`, the lock and the shield.
- **`GameTests`**, against `GameTestsImpl` (`mockPlayer`, `MockPlayer`). It covers the two test seams.
- **`SuitView.visualState`**, against `SuitView` and `StationLayout.suitView`.
- **"Schema NBT"**, the frame item, against `ExoframeItem`, `ItemDataAccessImpl.frame`/`setFrame` and
  `FrameInteractions.unmount`.
- **`Net`**, `station_view`.

Changes:
- **`GameTests`, "Condizione".** The version directive is now placed in the private `mockPlayer`, from which both
  `mockServerPlayer`'s and `operatorPlayer`'s players come: the constructor with or without `ClientInformation`, and
  `placeNewPlayer` with or without the `CommonListenerCookie`.
- **`GameTests`, "Differenze chiuse".** The mock sentence said the mock always answers `isCreative()`. It now says:
  - the subclass is shared by both players and built by `mockPlayer`;
  - the two differ by permission level (0 or 2, the higher of that and the server's) and by `isCreative()`
    (`mockServerPlayer`'s always true, `operatorPlayer`'s follows the game mode).

  The "A cosa serve" text already said this; it is kept.
- **`Net`.** "`station_view` ricevuto nel client vero sulle quattro varianti con `m4a_shots.sh`" (Task 4 measured it
  on the four).
- **"Il lucchetto e lo scudo".** "lo scrive solo il ramo di debug" became "lo cambia solo il ramo di debug …; la
  raccolta e la posa del telaio lo conservano (l'oggetto telaio raccolto, in "Schema NBT")". Since Task 3, placing a
  picked-up item also writes `reinforced` from the item data.
- `SuitView.visualState` and the "Schema NBT" paragraph matched the code; no change.

Run:
```
$ grep -c 'pulsante di prova\|TEST_BUTTON' docs/compat.md
0
```

### Step 4 — final verification (see the table below and "End-of-task outputs")

### Step 5 — commit `d0d454b`

I staged with `git add` and explicit paths (the five documents) and committed with the brief's subject and the two
trailers.

## Step 4's table, each row with its outcome on each variant

The evidence:
- JUnit: the `--rerun` XML of this task (`<S5>/junit-rerun.log`, `<S5>/junit-count.txt`).
- In-world: this task's `bash tools/run_gametests.sh` (`<S5>/gametests.log`).
- Client checks: `M4A_SOLO_CONTROLLI=1 bash tools/client/m4a_shots.sh <v> <copy>`, re-run now on copies of Task 4's
  shots (`<S5>/solo-checks.log`). The shots were taken at `d0a5342`. `git diff --stat d0a5342 HEAD -- src` is empty,
  and `m4a_shots.sh` has no non-comment change since then.
- Server: Task 4, Step 4, at `d0a5342` (`m4a2-task4/logs/step4-<v>-latest.log`).

| Che cosa (design §6, la riga 4a) | Prova | 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric | 1.21.1-neoforge |
|---|---|---|---|---|---|
| la stazione si fabbrica, si posa e si rompe senza doppioni; si apre su un telaio sopra o entro 2 blocchi e ne mostra l'anteprima 3D, che si gira trascinando | `StationBlockGameTests` (10), `StationMenuGameTests` (17), `DataGameTests` in "All 256 required tests passed"; `anteprima` 18582/18582, `rotazione` 24503 (1.20.1) / 24245 (1.21.1) | sì | sì | sì | sì |
| dalla schermata si scelgono pezzi, cella e tutti, e si montano e smontano pezzi e cella | `StationActionTest` 6/6 (0 failures); the 11 Task 1 tests of `StationActionGameTests`; `buttonsOfAnotherOrAClosedMenuAreDiscarded`, `buttonsIgnoreSpectatorsAndTheDead` (in the 256); `placeClicksFollowTheTable` present and passing; `scelta` 972, `smontaggio` 952 / drop 497, `montaggio` 0/0/648 | sì | sì | sì | sì |
| si blocca e sblocca il telaio, per chi può, e la vista arriva quando cambia | the 5 Task 2 tests of `StationActionGameTests` (in the 256); `lockTooltipFollowsTheView` present and passing; `lucchetto` 144, `vista` 144 / 0, `rinforzato` 552 | sì | sì | sì | sì |
| un telaio raccolto e ripiazzato conserva sblocco e rinforzo, e il proprietario quando a raccoglierlo è stato un altro giocatore | the five `FrameItemGameTests` (in the 256) | sì | sì | sì | sì |
| tutto verde: un solo sorgente, quattro jar, server dedicato | build `BUILD SUCCESSFUL`; six checks OK; JUnit 407 = N₀ 389 + 18; 256 in-world; server `Done (` and `stop` (Task 4, Step 4: forge 2.119 s, 1.20.1-fabric 2.089 s, 1.21.1-fabric 0.691 s, neoforge 0.604 s, `check_log.py` silent) | sì | sì | sì | sì |

The git checks of Step 4 (`BASE` = `638346d`, which sits on `f5b3ac8`; `f5b3ac8..638346d` adds only the plan file):

```
$ git diff "$BASE" -- tools/log_allowlist.txt | grep -c '^+[^+]'
2          # brief expects 1: see "Concerns" 1
$ git diff "$BASE" -- tools/log_allowlist.txt | grep '^+[^+]'
+# Un pezzo di un livello senza oggetto (in questa parte solo il Rottame ne ha) non si smonta dalla stazione: resta montato.
+gametest | =1 | Cannot unmount HELMET from frame {UUID}: no item for a INDUSTRIAL HELMET piece | unmountWithoutAnItemKeepsThePiece
$ git diff --stat "$BASE" -- src/main/resources-mc1.20.1/redstone_exoframes.mixins.json src/main/resources-mc1.21.1/redstone_exoframes.mixins.json tools/art
(no output)
$ git status --short
(before the commit: the five documents only; after the commit: empty; no shot outside docs/test-in-gioco/m4a-screenshot/)
```

The `Cannot unmount HELMET …` WARN appears exactly once per variant in this task's in-world run (4 in
`<S5>/gametests.log`).

## End-of-task outputs

- **Build.** `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build
  :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar
  :1.21.1-neoforge:collectJar` gives `BUILD SUCCESSFUL in 1s`, "52 actionable tasks: 52 up-to-date", because only
  documents changed (`<S5>/build.log`).
- **JUnit.** A fresh rerun, `./gradlew --console=plain :1.20.1-forge:test --rerun :1.20.1-fabric:test --rerun
  :1.21.1-fabric:test --rerun :1.21.1-neoforge:test --rerun`, gives `BUILD SUCCESSFUL in 18s`, "4 executed".
- **JUnit count**, with the plan's command (54 XML files per variant, 0 failures, 0 errors):
  ```
  1.20.1-forge 407
  1.20.1-fabric 407
  1.21.1-fabric 407
  1.21.1-neoforge 407
  ```
  That is N₀ + 18 = 389 + 18.
- **The six checks.** Each exits 0, on the final tree and again on the committed head `d0d454b`:
  - `bash tools/check_sources.sh` → `OK sources`
  - `bash tools/check_jars.sh` → `OK jars`
  - `bash tools/check_data.sh` → `OK data`
  - `bash tools/check_art.sh` → `OK art`
  - `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure`
  - `bash tools/client/client_shot.sh --selftest` → `OK passi`
- **In-world tests.** `bash tools/run_gametests.sh` exits 0 in 124 s:
  ```
  OK 1.20.1-forge: 256 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 256 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 256 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 256 test obbligatori passati, nessun avviso fuori elenco
  ```
- **The eight client checks**, re-run on copies of Task 4's shots: the same `ok` lines and `OK <variante>` on the four
  variants (`<S5>/solo-checks.log`).

## Files changed (`d0d454b`)

- `docs/test-in-gioco/m4a-stazione.md` (new)
- `docs/test-in-gioco/m4a1-banco.md` (one note at the top)
- `README.md` ("Stato", the test count, the `m4a_shots.sh` sentence, "Documenti", the plans list)
- `tools/client/README.md` (two pointers)
- `docs/compat.md` (`GameTests` "Differenze chiuse" and "Condizione", `Net`, "Il lucchetto e lo scudo")

`tools/README.md` is not changed (already correct). No code, art, texture or shot is changed.

## Self-review

I read the checklist as the assistant on the PC would, step by step, and kept track of the world state through it:
- which frame is on the station, locked or unlocked, reinforced or not;
- the game mode in each step;
- what is in the inventory and which hotbar slot gets what (`Handouts.give` skips the selected slot).

Fixes made during the review:
- step 16 names both cell tooltips ("10000 / 10000 RE" and "Vuota: caricala in un caricatore");
- step 27's lock sounds no longer assume the lock's state;
- the cloud sequence's `/exoframe lock` is described as given from the server console with the gaze on the frame;
- the preparation note says what to do if "La stazione non è riuscita…" ever appears.

Hygiene:
- no plan, decision, review or ruling IDs in the documents;
- no model or vendor names in the documents (the commit trailers are the mandated ones);
- no personal names: "il proprietario", "l'assistente sul PC";
- Markdown paragraphs are one line, as in the existing docs;
- `git diff --check` is clean.

## Concerns

1. **The allowlist count of Step 4 prints 2, not 1.** Task 1 added the one entry the plan names (`unmountWithoutAnItemKeepsThePiece`) together with a
   comment line above it, as every entry in `tools/log_allowlist.txt` has. `grep -c '^+[^+]'` counts the comment too.
   Added non-comment lines: 1. The substance holds (one new allowlist entry, Task 1's); the literal output differs from
   the brief.
2. **The station's action-bar messages are hidden behind its own screen.** This is a product limit, not part of this
   task. The coordinator ruled to document it and ask the owner; no code is changed.
   - The messages are "Quello slot è già occupato", the two lock messages and "La stazione non è riuscita…".
   - They are covered whenever the interface is under 376 pixels tall: 960×540 at scale 2, and 1080p at automatic
     scale.
   - The checklist tells the assistant to press Esc right after the click (steps 16 and 19).
   - It is the new question in "Domande per il proprietario".
   - The final review decides whether this part or the 4b fixes it.
3. **Choices beyond the brief.** They are small, and in the spirit of decision 23, carrying over the 4a-1 bench and
   its 4d notes so that nothing is lost when the assistant follows only the new checklist:
   - **step 4:** a walk-around look check, from the 4d note;
   - **step 5:** the creative bullet places the station facing east, south and west (the 4d note's orientation check);
   - **step 17:** adds the same release test pressed on "Tutti" (Review Focus 1 names "Tutti");
   - **step 22:** a second pick-up proves the re-placed frame kept its flags, instead of opening the station on it;
   - **step 28:** a `Config file` log check after the restart with the one-section file (`ConfigFileTest.validFileIsUsed`
     shows a one-section file reads silently).
4. **Nothing in the checklist was run on a real PC.** It was checked against the code and the game's files.
   - **The LAN "Comandi: No" permission levels** rest on reading vanilla code with javap: the host stays level 4 and the
     second player is level 0.
   - **The death path of step 21** is what in-world tests prove:
     - `ExoframeGameTests.deathLeavesTheFrameWithEverythingInside` dies through the game's `kill`, and the placed
       record keeps the stowed helmet;
     - a `StationMenuGameTests` case asserts `hasStowed` in the view of a frame with stowed armour.

     The "Custodisce un'armatura" row itself has never been drawn in a real client (Task 1's note); step 21 is its
     first look.
