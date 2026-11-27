# Part 4d, interface art: the fix wave after the review

You own the single fix wave of this unit. A previous implementer put the station screen texture, the HUD drawings and the helmet vignette into the game (commit `e58533c` on branch `m4-stazione`, repo `/home/user/exosuits`). A reviewer then found no Critical or Important issue, five Minor ones, and one known gap. This wave closes all of them. The branch goes to `main` after one scoped re-review of your commits.

Read first, in this order:
1. `/home/user/exosuits/.superpowers/sdd/m4d-grafica/review.md`: the findings, with file:line, the reasons and the suggested fixes. Its "Spec Compliance" item ⚠️ 1 and "Minor" items 1-5 are yours.
2. `/home/user/exosuits/.superpowers/sdd/m4d-grafica/integrazione-brief.md`: the task as it was given, with its constraints and end-of-task commands.
3. `/home/user/exosuits/.superpowers/sdd/m4d-grafica/integrazione-report.md`: what the previous implementer did and how. It names the run commands, the run folders, the shot names and the measures. It is your memory of the task; you append your own report to it (below).
4. `tools/client/README.md`: the client shot tools, sequence by sequence.

## What to do

### A. The 3c and 3d client sequences (review ⚠️ 1)

1. Rerun `m3c_shots.sh`, `m3c2_shots.sh`, `m3c_rules_shots.sh` and `m3d_shots.sh` on the four variants (`1.21.1-neoforge`, `1.20.1-forge`, `1.20.1-fabric`, `1.21.1-fabric`) with the code of `e58533c`. Use `EXO_ACCEPT_EULA=true` (the owner consented to the EULA for cloud clients and servers).
2. Every run must end with `OK <variant>` and an empty `log-check.txt`.
3. If a check fails, find the cause with evidence before you change anything:
   - Change a threshold only with measures from all four variants.
   - Write the measures in the script's comment, next to the old ones, as `m3d_shots.sh` already does.
   - Change the code only if the cause is in the code.
4. Redo the committed reference shots in `docs/test-in-gioco/m3c-screenshot/` and `docs/test-in-gioco/m3d-screenshot/`:
   - Redo every shot in which the HUD or the vignette is in view. Take it from the new runs, keeping each file's variant prefix.
   - The sequence now writes `hud-energia*` where the folder holds `*riga-energia*`. Keep whichever names the checklists cite (`docs/test-in-gioco/m3c-giocarci.md` and its sibling files), or rename the files and the citations together.
5. Update the measured numbers in the comments of `m3d_shots.sh`, and of any other script whose comments carry measures that changed (the review names 23629 and "perde 8 pixel").
6. Reread the checklists of parts 3a-3d (`docs/test-in-gioco/m3a-*.md`, `m3b-*.md`, `m3c-*.md`, `m3c1-*.md`, `m3d-*.md`) wherever they describe how the HUD or the vignette looks.
   - Where a sentence describes the 4c look (flat fills, thin borders), fix it with a note in the form the files already use, as 4c did: a note at the top, without changing the steps.
   - Do not change the steps themselves.

### B. The five Minor findings

1. **Selftest coverage** (Minor 1). Widen the `scelta` check in `gui.check` to all four sides of the ring. Then add one negative case to `tools/art/selftest.py` for each check the review lists as uncovered:
   - the `scelta` ring;
   - the lighter first rows of `durata.*` and `barra.*`;
   - the fill and outline of `sagoma.assente`;
   - the missing top glint;
   - the kit size;
   - the `ValueError` for a drawing of the wrong size in `screen_sheet` and `hud_sheet`.
2. **The design's record of the HUD art** (Minor 2). The owner approved the art as drawn, so the art stays and the text changes. In design §5.12, "Grafica della parte 4d", give one clause each to:
   - the plate fill `9914161A`;
   - the cut helmet corners and torso waist, which no longer fill their rectangles;
   - the opaque dark-steel "!" of the warning;
   - the dim spare-cell icon at ×0.

   Keep "two departures accepted by the owner" true: the new clauses describe the look inside the table's rectangles, which the owner saw in the previews.
3. **The HUD sprite pickers** (Minor 3). Drive the picker tests from the model instead of hand-written colour lists:
   - every `PieceCondition` through `HudModel`'s colour rule, plus the unmounted paint, into `figure`;
   - `NORMAL`, `LOW`, `EMPTY` and "no indicators yet" through `energyColor` into `barFill`;
   - every state in which `warningOn` can be true into `warning`.

   Keep the pickers' signatures; this is a test change.
4. **Python layout and Java layout tied by a test** (Minor 4).
   - Make `gui.py` write the sprite map and the shared layout rectangles to a generated file that `tools/check_art.sh` regenerates and compares byte for byte, like the other generated files.
   - Keep that file out of `src/main/resources`, so it never enters a published jar.
   - Add a JUnit in the style of `SuitScaleTest.matchesTheArtGenerator`, which already reads a file under `tools/art/`. It compares:
     - every sprite name and rectangle with the `Sprite` constants of `StationLayout` and `HudLayout` (this catches two same-size sprites with swapped names);
     - every rectangle that `FONDO` bakes from `gui_layout.py` with the matching `StationLayout` rectangle.
   - If the background bakes a rectangle that `StationLayout` does not have yet (4b's palette tray, preset grooves and cost tray), the test lists it as baked-only, by name, so 4b sees it.
5. **Test oracles in `HudModel`** (Minor 5). Do not remove `HudModel.lighter` or `HudModel.Paint.outline`: removing the record component would ripple through 4c's tests. Instead, say in their Javadoc that drawing code does not use them, and that they are the rule the committed texture is tested against (name the test).

### C. Three items from the previous implementer's concerns and the controller

1. `docs/compat.md`: the `StationBlock` entry still says that the models use parent `orientable` and textures from `gen_placeholders.py`. Write what is true since part 4d's station: the generator, the files and the check. Reread the entry for the HUD (`SuitHud`, `addHudRenderer`) and fix any sentence that still describes the 4c flat drawing.
2. Design §14, "Per la 4d": every item there is done or no longer applies (`CELL_CHARGE` and `CELL_RED` are gone, and the two-digit spare width is now measured or still deduced). Do not delete the items. Add each one's outcome in one clause: what was done and where, or why it no longer applies.
3. Several comments in the HUD classes and their tests cite "(Decisione N del piano)": `HudModel`, `HudKeys`, `HudLayout`, `SuitHud`, `HudLayoutTest` and `HudModelTest`. A plan's decision number means nothing to a later reader. Replace each with the reason in a few words or with the design section it comes from (for example "design M4 §5.12"). Never add plan, ledger or review IDs to code or comments.

## Constraints

- The machine runs one Gradle build, one client or one server at a time (4 cores, 15 GB). Never start two.
- Do not change the art. `tools/art/` changes only for B1 and B4, and the regenerated textures must stay byte-identical (`bash tools/check_art.sh` → `OK art`).
- Every committed PNG has only IHDR, IDAT and IEND chunks. There is no personal data, and no AI model or vendor names, in docs, code or commit messages (the two trailer lines excepted).
- Docs are in Italian, plain and short. Code comments follow the files' existing style.
- Commits:
  - The environment signs them. Never use `-c commit.gpgsign=false` or `--no-verify`, and never set an identity.
  - End each message with these two lines:
    ```
    (prima riga di attribuzione dell'ambiente)
    (seconda riga di attribuzione dell'ambiente)
    ```
  - Do not push, force-push or rewrite history. The controller pushes.
  - One commit for the code, tests and docs, and one for the redone shots, is fine.
- You do not dispatch subagents of any kind, reviewers included.

## End-of-task commands

Run these, in this order, with the code as committed:
- the four-variant build of the integration brief (`BUILD SUCCESSFUL`), and the JUnit count per variant (the brief's command);
- `bash tools/check_sources.sh`, `check_jars.sh`, `check_data.sh` and `check_art.sh`, which print `OK sources`, `OK jars`, `OK data` and `OK art`;
- `python3 tools/art/selftest.py`, with all cases passing.

Gametests, the m4c/m4a1 sequences and the dedicated server are not needed, because no game code changes. If you do change game code, run `run_gametests.sh` and the sequences that cover it.

## Report

Append a section `## Fix round 1` to `/home/user/exosuits/.superpowers/sdd/m4d-grafica/integrazione-report.md`. It holds:
- for each item (A, B1-B5, C1-C3), what changed, with file:line;
- the covering tests, with the command and its output;
- a table of the 16 sequence runs (sequence × variant), with the OK line and whether `log-check.txt` was empty;
- the reference shots redone, by name;
- the measures that changed;
- the JUnit count per variant;
- the end-of-task outputs.

Your final message is short:
- status: DONE, DONE_WITH_CONCERNS, NEEDS_CONTEXT or BLOCKED;
- the commit hashes;
- one line of test summary;
- your concerns, one line each.
