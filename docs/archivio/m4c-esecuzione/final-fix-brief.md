# Final fix wave — part 4c "L'HUD"

Repo /home/user/exosuits, branch `m4-stazione` at `8ecd206`. A Minecraft mod built in four variants (1.20.1-forge, 1.20.1-fabric, 1.21.1-fabric, 1.21.1-neoforge) from one tree. Docs, comments and checklists are in Italian; keep the surrounding style. The final whole-branch review of part 4c found no Critical or Important defects; the coordinator decided the fixes below (ruling R22). Do all of them and nothing else. The full review is in `final-review.md` next to this file, if you need the reviewer's evidence for a fix.

## Rules

- No personal data in docs or code (write "il proprietario", "l'assistente sul PC"); no AI model or vendor names in docs or code.
- The checklists in `docs/test-in-gioco/` are followed literally by an assistant on the owner's PC: every quoted label, text and command must match the game and the language files (`src/main/resources/assets/redstone_exoframes/lang/it_it.json`, `en_us.json`). Copy vanilla labels from where the same checklist already quotes them.
- Commits are signed by the environment: never `-c commit.gpgsign=false`, never `--no-verify`, never set a git identity. Never rewrite history. Do not push (the coordinator pushes).
- One Gradle run at a time; never start a game client or a server.
- You do not dispatch subagents of any kind.

## Fixes

**F1. `docs/test-in-gioco/m4c-hud.md`, preparation E (line 90).** Preparation B reuses the instances of the earlier checklists, and the m3c checklist (step 4, `docs/test-in-gioco/m3c-giocarci.md:151`) turned "Sottotitoli: Sì" on there; step 15 (`:133`) expects a button reading "Sottotitoli: No". E also leaves the main hand as it is, while step 3 (`:102`) expects the group bottom left. Make E set "Sottotitoli: No" (with the menu path the checklist already uses at step 15; "if it read Sì, note it") and check "Mano principale: Destra" (menu path as at the checklist's left-hand steps). Then re-read steps 3 and 15 from that starting state: they must read consistently.

**F2. `docs/test-in-gioco/m3c-giocarci.md:12`, the 4c note at the top.** It changes step 12 only, but step 10 (`:175`) also quotes the keys hint word for word, in Italian and in English, and with the 4c jars both texts change. Write "Passi 10 e 12" and give the new hint in both languages, copied from the keys-hint entry of the two language files (as the game shows it, with the key names V, R, H).

**F3. `src/test/java/com/toxineren/exoframes/client/HudLayoutTest.java:36-40` and `:134-137`.** `ONE_DIGIT_SPARE = 11` and `TWO_DIGIT_SPARE = 17` are the plan's estimates; in game "×0" is 12 wide (measured in part 4c, Task 3; the reference shots put the cell icon at 47 and the text at 53 in the energy part). Set `ONE_DIGIT_SPARE = 12` with a Javadoc saying it is measured in game, and `TWO_DIGIT_SPARE = 18` with a Javadoc saying it is deduced (one more 6-pixel digit) and not measured. Update the assertions that depend on them (`spareTextX` and `cellIconX` for one and two digits) to the values the layout rule gives for these widths — read `HudLayout.spareTextX` and the test's `cellIconX`; for 12 that is text at 53 and icon at 47. Any other assertion using these constants keeps its meaning (both cases fit inside the spare area). Run `./gradlew --console=plain :1.21.1-neoforge:test --tests '*HudLayoutTest'` → PASS with 7 tests (count them in the test-results XML of that variant).

**F4. `docs/superpowers/specs/2026-11-19-m4-stazione-design.md`, the no-cell text.**
- Line 274 (§5.12, the percentage row): "senza cella "Nessuna cella" in grigio" is the text from before decision 1. Read how `SuitHud`/`HudModel`/`HudLayout` draw the no-cell case and write what ships: `hud.redstone_exoframes.no_cell`, "Senza cella" ("No cell"), where and in which colour it is drawn, and in a few words why ("Nessuna cella" is 69 pixels in a 63-pixel row — decision 1 of the 4c plan).
- Line 376 (§5.16 table): "%s %%; Nessuna cella" → "%s %%; Senza cella".
- Line 633 (§13, "Per la 4c"): the decision "aspetta la conferma del proprietario prima del Task 1" → it was followed: asked to the owner, no objection so far; changing it costs one text and one constant.

**F5. `docs/test-in-gioco/m4c-hud.md:19`.** "In alto a destra nel gruppo" is true only for the right hand; write "in alto a destra nella parte dell'energia" (for a left-hander the spare cells sit mid-group next to the silhouette, as `m4c-screenshot/1.21.1-neoforge-mancino-danneggiato.png` shows).

**F6. `README.md:28`.** `gen_placeholders.py` "rigenera le texture provvisorie degli oggetti e l'icona" — it now also writes the station block faces (part 4a-1) and the HUD vignette (part 4c). List what it writes briefly, consistent with the "Texture placeholder" section of `tools/README.md`.

**F7. `src/main/java/com/toxineren/exoframes/client/HudModel.java:73-74`.** `CELL_CHARGE`'s Javadoc cites the design previews (§12), whose redstone is `C0201A`; the value `0xFFC81E1E` is the station's red. Cite `StationLayout.CELL_RED` (same value) instead. Comment only: the constant stays (unifying the two is a 4d carry-over, F8).

**F8. The design: a new section "## 14. Riporti della parte 4c"**, right after §13 and in the same form (read §13 first): short lines grouped by who picks them up, about 10-15 lines in all.
- For part 4d, which redraws the HUD graphics: pin in `HudLayoutTest` the silhouette and warning rectangles, and the real width of two spare-cell digits, once the graphics are final; pin the bar body `122x10+52+510` (`BAR_FILL` minus its first row) in `screenRectsAtNineSixtyMatchTheSequence` (the `barra-esaurita` box `42x16+50+482` depends on the width of "0%" and can be pinned only with a width measured in game); pin the `testo` box `46x14+50+482` in `m4c_shots.sh` (checkable with `M4C_SOLO_CONTROLLI=1` on existing shots); one constant for the cell-charge red (`HudModel.CELL_CHARGE` and `StationLayout.CELL_RED`).
- Extra tests next time the files are touched: in `ClientPrefsFileTest` a file with one valid and one wrong key (`{"hudHidden": "yes", "vignette": false}` → the defaults); in `HudLayoutTest` one `intersects` that is true and one `contains` that is false.
- From the PC run (DEC-48: what it finds becomes the first task of the next part): what is seen on Fabric 1.21.1 of the subtitles and the saving indicator, and on NeoForge 1.21.1 of F3 over the group — deduced from the bytecode, not seen in the cloud (the rulings are in the 4c execution files archived under `docs/archivio/m4c-esecuzione/` on the `archivio/m3c-fascicoli` branch).
- The facts of the 4c plan that execution disproved (the plan stays as it was written): "×0" is 12 wide, not 11; `options.txt` needs its `version:` line for the `key_` lines to count; whether the base game's layers sit above or below the HUD depends on the loader, not on the version (§5.12); the `riavvio` check compares run A's `nascosto` with run C's `riavvio-nascosto`, and `riavvio-senza-tuta` is gone (the suit raises the camera).
- The owner's questions 8-10 keep their provisional values; decision 1 ("Senza cella") is followed.

## Verify

- The `HudLayoutTest` run of F3.
- `bash tools/check_sources.sh` → `OK sources`.
- `grep -n 'Nessuna cella' docs/superpowers/specs/2026-11-19-m4-stazione-design.md` → only where the text explains why it does not fit.
- `git status --short` → only the files above, no screenshots.

## Report and commit

Write `/home/user/exosuits/.superpowers/sdd/2026-11-20-m4c-hud/final-fix-report.md`: for each fix, what changed (file:line), then the commands run with their output, then any concern.

One commit, e.g. `docs, test: le correzioni della revisione finale della 4c; design M4, i riporti della parte 4c`, ending with exactly these two lines:

```
(prima riga di attribuzione dell'ambiente)
(seconda riga di attribuzione dell'ambiente)
```

Return only: status (DONE, DONE_WITH_CONCERNS, BLOCKED), the commit hash, a one-line verification summary, and concerns.
