# Task 1 report — i pulsanti della stazione (parte 4a-2)

**Status:** DONE_WITH_CONCERNS (minor notes only, see "Concerns"; every check of the brief passes).
**Commits** (on `m4-stazione`, on top of BASE `638346d`; signed by the environment, not pushed):
- `7b5010f` station, frame, client: i pulsanti della stazione (scelta, montare e smontare pezzi e cella dal cursore), una regola sola con le mani, una scrittura per tick; via il pulsante di prova; tools: m4a_shots.sh
- `8c1f1fe` client: la schermata della stazione ricorda per ogni tasto la pressione presa da un widget, cosi' il rilascio di un tasto tenuto su un widget non arriva al gioco base anche se nel frattempo un altro tasto ci e' andato (see "Follow-up" at the end)

## What I implemented

- **core/station/StationAction** (Java 17): `record StationAction(Kind kind, int arg)`, the 16 kinds with `first()`/`count()` exactly as in "Tipi nuovi", `decode`, `button()`, `button(Kind,int)`, `writes()`, `slot()`, `HIGHEST = 102`; constructor NPE/IAE; Javadoc carries the design §5.6 table and says the 4b numbers already decode.
- **core/station/TickGate**: `open`/`use`/`tryUse`, a `used` flag instead of a sentinel (first tick always passes, `Integer.MIN_VALUE` included; any tick different from the last used one is open, so a counter going back restarts the limit).
- **StationView.withSelection(int)** (IAE outside [0, 7] via the constructor).
- **station/StationTexts**: `CELL`, `ALL_PIECES`, `EMPTY_SLOT`, `DURABILITY`, `ROTATE`, `CONDITION_PREFIX`, the new `ALL`, `EMPTY`, `UNMOUNT`, `HOLDS_ARMOR`, `HOLDS_ARMOR_HOW`, and `FAILED`; `KEYS` with the four condition names.
- **frame/FrameFitting** (`Result { DONE, REFUSED, FAILED }`): `mountPiece` (wrong item → silent REFUSED first, then occupied → `SLOT_TAKEN` + REFUSED), `insertCell` (swap in one `updatePlaced`, old cell to `Handouts.give` even in creative), `unmountPiece` (item built first; no item → the existing WARN + REFUSED), `removeCell`. One `updatePlaced` each; only on success: hand out / shrink the given stack in place (not with `instabuild`), `FramePlacement.refreshView(player.serverLevel(), frameId)`, sound at `frame.position()` of the record.
- **FramePlacement.refreshView(ServerLevel, UUID)**: summary from the record if placed and `level.getEntity(id)` is an `ExoframeEntity`; otherwise nothing.
- **FrameInteractions**: lost `refreshSummary`, `unmountPiece` and its mount/insert copies; the hands pick the slot (first free of the clicked side order, none free → first of the order) and call `FrameFitting`; `DONE` → `SUCCESS`, otherwise `CONSUME`. Stowed armour and the empty-frame pickup unchanged. `ExoframeGameTests` untouched and green.
- **station/StationActions.press** in the order of "L'ordine di `press`": decode + alive → IGNORED; `SELECT_*` → `menu.select` → SELECTED (no write turn); 4b kinds and `TOGGLE_LOCK` → IGNORED before the write turn (no turn, no log); `takeWriteTurn` → THROTTLED; record + `canUse` → REFUSED; `FrameFitting` by kind; FAILED → `StationTexts.FAILED` on the action bar; same-name Outcome.
- **StationMenu**: removed `TEST_BUTTON`, the `DataSlot`, `testPresses()`, `NEVER`, `lastSentTick`; mutable `selection` with `selection()`/`select()` (client: also `view = view.withSelection`); package-private `takeWriteTurn` (write `TickGate`); view sending through a second `TickGate` (`open` as the early return, `use` only when a view is sent: same behaviour as before); `clickMenuButton`: server (`level != null`) → `press`, true for SELECTED/DONE; client (`level == null`) → true for any decoded number, `select` first for the three selections. `setView` does not touch the server-side selection.
- **refusePlacedUpdates seam**: `GameTests.refusePlacedUpdates(server)` → `GameTests.Refusal` (idempotent close); `FrameStateSavedData.placedUpdateRefusals` (package-private counter); `FrameStateAccessImpl.updatePlaced` returns false silently while > 0 (checked before read-only, so no log); `readOnly()`, save and ERRORs untouched.
- **SuitView.visualState(PieceCondition)**, used by `SuitView.of` and `StationLayout.suitView`.
- **client/StationLayout**: `STOWED` 144,6-274,15, `ALL_BUTTON` 41,103-71,119, `UNMOUNT` 195,20-207,32; `Cursor`, `ButtonState`, `buttonState`, `allSprite`, `unmountSprite`, `ghostSprite` (return existing constants only; no new `Sprite` field), `placeAt`, `placeButton`, `unmountButton`, `fitsBeside` (`HEADER_GAP` 4); small helpers `mounted(view, place)`, `CELL_PLACE`, `ICON_INSET` (moved here from the screen). Test-button members removed.
- **client/StationFramePanel** (places, icons, ghosts, `scelta`, "Tutti" with label centred in `TEXT`, place tooltips; owns the icon cache) and **client/StationHeader** (name/condition/numbers/bar with `fitsBeside`, empty slot on two lines in `TEXT_DIM`, cell numbers always in the row, the arrow with `unmountButton` state and the `UNMOUNT` tooltip). Both draw with `StationScreen.TEXTURE`, now package-visible.
- **client/StationScreen** (452 → about 340 lines): click rules (widget under the pointer → remembered per mouse button, drag/release of a remembered button return true without `super`, left button only acts, a press of the same button off the widgets clears that button's stale memory; see "Follow-up"), cursor class from `menu.getCarried()`, `Screen.hasShiftDown()`, numbers sent like vanilla menus (`menu.clickMenuButton` then `handleInventoryButtonClick`), `PRESSED` while the left button is held on "Tutti"/the arrow, `STOWED` line in `renderLabels`, `HOLDS_ARMOR_HOW` tooltip, preview rotation unchanged (distance from the press point).
- **Gametests**: `StationActionGameTests` (11 tests, with a small `Bench` helper for setup/cleanup/delayed steps), registered after `StationBlockGameTests`; `StationMenuGameTests`: `buttonsOfAnotherOrAClosedMenuAreDiscarded`, `buttonsIgnoreSpectatorsAndTheDead` (renamed and rewritten, same count); `GameTestKit.scrapPieceStack`; `everyItemAndBlockHasANameInBothLanguages` now also checks `StationTexts.KEYS`, `StationItem.TOOLTIP_KEYS`, `StationBlock.NO_FRAME`, `StationMenu.FRAME_GONE`, `StationMenu.TITLE_KEY`.
- **Lang files**: Task 1 keys in the given places, `test_button` removed; `tools/log_allowlist.txt`: the one `=1` line for `unmountWithoutAnItemKeepsThePiece`.
- **tools/client/m4a_shots.sh** (`git mv` of `m4a1_shots.sh`): phases 9 scelta, 10 smontaggio, 11 montaggio after the tooltip, then 12 vista (smoke test), 13 inventario (iron after the last measured shot), 14 chiusura, 15 rottura, 16 fine; checks `anteprima`, `rotazione` (`girata` vs `stazione-aperta`), `scelta`, `smontaggio`, `montaggio`; `pulsante` gone; `M4A_SOLO_CONTROLLI`; header comment with the new phases, checks and what the sequence does not prove; thresholds measured (below).
- **Docs**: `docs/compat.md` (GameTests: `refusePlacedUpdates`; SuitRenderer/SuitView: `visualState`; "Menu e schermate": the buttons paragraph — table, `press` order, one write per tick per menu with `TickGate`, client-predicted selection, `FrameFitting` shared with the hands, `refreshView`, new test names; the other station paragraphs no longer describe the test button; `m4a_shots.sh` everywhere); `tools/client/README.md` (sequence paragraph, check table with measures, `M4A_SOLO_CONTROLLI`, timings, pointer example); `tools/README.md`, `README.md`, `tools/client/measure_lib.sh` (comment); Javadocs of `GameTestKit.openStation`, `StationMenuGameTests`, `StationLayoutTest`.

## Tests and TDD evidence

Logs in `<scratchpad>/logs/`.

- **Step 1 RED** — `./gradlew --console=plain :1.21.1-neoforge:test --tests '*StationActionTest' --tests '*TickGateTest' --tests '*StationViewTest'` → `BUILD FAILED`, 108 compile errors: `package ...StationAction does not exist`, `cannot find symbol` for `TickGate` and `method withSelection(int)`.
- **Step 2 GREEN** — same command → `BUILD SUCCESSFUL`; `StationActionTest tests=6`, `TickGateTest tests=4`, `StationViewTest tests=7` (6 + 1), 0 failures.
- **Step 3 RED** — `./gradlew --console=plain :1.21.1-neoforge:test --tests '*StationLayoutTest' --tests '*SuitViewTest'` → `BUILD FAILED`: missing `Cursor`, `ButtonState`, `ALL_BUTTON`, `UNMOUNT`, `STOWED`, `ICON_INSET`, `ghostSprite`, `fitsBeside`, `unmountButton`, `buttonState`, `placeAt`, `visualState(PieceCondition)`.
- **Step 4 GREEN** — same command → `BUILD SUCCESSFUL`; `StationLayoutTest tests=12` (8 + 4), `SuitViewTest tests=5` (4 + 1), 0 failures.
- **Step 5 RED** — `./gradlew --console=plain :1.21.1-neoforge:compileJava` → `BUILD FAILED`: missing `StationActions`/`Outcome`, `FrameFitting`, `refusePlacedUpdates`/`Refusal`, `StationTexts`, `StationMenu.selection()`. (`scrapPieceStack` did not show up because I added it to `GameTestKit` together with the tests in this step.)
- **Step 6** — four-variant build `BUILD SUCCESSFUL`, 405 JUnit per variant; `OK sources`; `bash tools/run_gametests.sh` → four `OK <variante>: 246 test obbligatori passati, nessun avviso fuori elenco`; the expected WARN appears exactly once (`Cannot unmount HELMET from frame …: no item for a INDUSTRIAL HELMET piece`). `ExoframeGameTests` unchanged (empty diff) and green. `:1.20.1-forge:compileJava` checked right after Step 6 and after Step 7.
- **Step 7** — four-variant build `BUILD SUCCESSFUL`; `grep -rn 'TEST_BUTTON\|testPresses\|test_button' src` → no lines; `bash tools/check_art.sh` → `OK art`; no added Java line over 120 characters.
- **Step 8** — `bash -n tools/client/m4a_shots.sh` → no output; `grep -rln 'm4a1_shots\|M4A1_SOLO' src tools README.md docs/compat.md` → no lines.
- **Step 10** — `grep -c 'refusePlacedUpdates' docs/compat.md` → 2.

## Real client (Step 9)

`EXO_ACCEPT_EULA=true bash tools/client/m4a_shots.sh 1.21.1-neoforge …/scratchpad/shots/neo` (142 s, `client_shot.sh` 134 s), first run with the provisional thresholds:

```
ok anteprima: ritaglio magenta=18582 riquadro=167x198+604+96; schermata magenta=18582 riquadro=167x198+604+96
ok rotazione: sagoma pixel=24245 copertura=4.68 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96
ok scelta: schermata pixel=972 riquadro=232x88+222+78; ritaglio pixel=0 riquadro=0x0+0+0
ok smontaggio: posto dell'elmo pixel=952 riquadro=40x40+224+80; ritaglio da magenta=9225 riquadro=111x198+629+96 a magenta=8728 riquadro=111x192+629+102 (cala di 497); schermata magenta=8728 riquadro=111x192+629+102
ok montaggio: ritaglio pixel=0 riquadro=0x0+0+0; posto dell'elmo pixel=0 riquadro=0x0+0+0; schermata pixel=648 riquadro=28x28+218+454
OK 1.21.1-neoforge: scatti in …/shots/neo
```

Shots looked at: `scelta-torso.png` (the `scelta` ring around the torso, not the helmet; header "Scrap Torso"), `smontato.png` (helmet place empty with the helmet ghost; suit without helmet: the magenta box loses its top 6 rows; helmet in the second hotbar slot), `montato.png` (as `scelta-torso` plus the helmet in the second hotbar slot); also `stazione-aperta.png` ("All" button, arrow, header) and `suggerimento-torso.png` (four tooltip lines). The stop rule did not trigger: helmet place and suit magenta changed after the unmount, both returned to the pixel after the mount, and every box is the expected one.

Thresholds set to half the neoforge measure, rounded down to ten, with the measure beside them in the script: `SEL_MIN=480` (972), `PLACE_MIN=470` (952), `HELMET_MIN=240` (497).

`M4A_SOLO_CONTROLLI=1 bash tools/client/m4a_shots.sh 1.21.1-neoforge …/shots/neo` → the same five `ok` lines, `OK 1.21.1-neoforge`.
`EXO_ACCEPT_EULA=true bash tools/client/m4a_shots.sh 1.20.1-forge …/shots/forge` (141 s, `client_shot.sh` 132 s) → five `ok` lines with identical measures (rotation sagoma 24503, as before on 1.20.1), `OK 1.20.1-forge`.
`log-check.txt` empty (0 bytes) for both.

## Counts per variant

| Variant | JUnit | In-world |
|---|---|---|
| 1.20.1-forge | 405 | 246 |
| 1.20.1-fabric | 405 | 246 |
| 1.21.1-fabric | 405 | 246 |
| 1.21.1-neoforge | 405 | 246 |

N₀ 389 + 16 = 405 (re-run with `--rerun` on the four test tasks: 405 each, 0 failures/errors). 235 + 11 = 246.

## End-of-task outputs (Step 11)

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` → `BUILD SUCCESSFUL` (run again on the final tree after a comment-only fix: `BUILD SUCCESSFUL in 8s`).
- JUnit count → 405 on each of the four variants.
- `bash tools/check_sources.sh` → `OK sources`
- `bash tools/check_jars.sh` → `OK jars`
- `bash tools/check_data.sh` → `OK data`
- `bash tools/check_art.sh` → `OK art`
- `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure`
- `bash tools/client/client_shot.sh --selftest` → `OK passi`
- `bash tools/run_gametests.sh` → `OK 1.20.1-forge: 246 test obbligatori passati, nessun avviso fuori elenco`, same for `1.20.1-fabric`, `1.21.1-fabric`, `1.21.1-neoforge`.
- `git status --short` after the commit: clean, no shots.

## Files changed

New: `core/station/StationAction.java`, `core/station/TickGate.java`, `station/StationActions.java`, `station/StationTexts.java`, `frame/FrameFitting.java`, `client/StationFramePanel.java`, `client/StationHeader.java`, `gametest/StationActionGameTests.java`, `test/core/station/StationActionTest.java`, `test/core/station/TickGateTest.java`.
Modified: `core/station/StationView.java`, `station/StationMenu.java`, `frame/FrameInteractions.java`, `frame/FramePlacement.java`, `compat/GameTests.java`, `compat/impl/GameTestsImpl.java`, `compat/impl/FrameStateSavedData.java`, `compat/impl/FrameStateAccessImpl.java`, `compat/client/SuitView.java`, `client/StationLayout.java`, `client/StationScreen.java`, `gametest/StationMenuGameTests.java`, `gametest/GameTestKit.java`, `gametest/ChargerGameTests.java`, `RedstoneExoframes.java`, `test/core/station/StationViewTest.java`, `test/client/StationLayoutTest.java`, `test/compat/client/SuitViewTest.java`, `lang/en_us.json`, `lang/it_it.json`, `tools/log_allowlist.txt`, `tools/client/measure_lib.sh`, `tools/client/README.md`, `tools/README.md`, `README.md`, `docs/compat.md`; renamed `tools/client/m4a1_shots.sh` → `tools/client/m4a_shots.sh`.

## Self-review

- Completeness: every item of "Produces", the click table, the `press` order, the layout table and the Task 1 rows of the sequence are covered; each brief test is there with the assertions it lists.
- Order of checks in `FrameFitting.mountPiece` is wrong type before occupied (the arm-on-8 test depends on it). The write turn is taken before the record check, so each REFUSED/FAILED on one menu is in its own tick in the tests.
- The view gate keeps the old shape (`open` early return, `use` only on a send); verified by the unchanged `viewIsSentAtMostOncePerTick`/`viewFollowsTheRecordWithinATick`.
- Refusal seam: opened and closed with try-with-resources inside one synchronous body; checked before the read-only branch, so nothing is logged.
- Hygiene: no plan/review IDs, vendor names or personal data in added lines; no added Java line over 120 characters; Javadoc and comments in Italian with ASCII apostrophes; identifiers/exceptions/logs in English.
- Fixed during review: long lines (reflowed only the paragraphs I added), three `{@link` tags split across lines, one wording slip in a `StationMenu` field comment.

## Concerns

1. Step order: to keep the Step 6 four-variant build green after `StationMenu.TEST_BUTTON` went away, I removed the test-button code from `StationScreen`/`StationLayout` already in Step 6 (Step 7 then rewrote the screen). `scrapPieceStack` and the registration were added in Step 5 with the tests, so they are not in the Step 5 RED list.
2. Choices where the brief is silent: "Tutti" has no tooltip (none is specified) and its label is drawn without shadow like the screen's other texts; the arrow's `UNMOUNT` tooltip shows also when the arrow is OFF; `StationMenu.selection()` on the client is the last predicted selection (the screen reads the view's selection; `setView` does not touch the server-side field, which only buttons 0-7 change); `TOGGLE_LOCK` is IGNORED before the write turn until the lock has its handler.
3. `StationLayout` gained small helpers beyond the listed API (`mounted`, `CELL_PLACE`, `ICON_INSET` moved from the screen, `HEADER_GAP`); it is now 554 lines, mostly the 70 sprite constants and their Javadoc.
4. Disclosures: the "no rectangle touches the 4b widgets" check in `widgetsStayInsideTheScreenAndDoNotOverlap` runs over every rectangle of the test's table except `PIECE_PANEL`, skipped by name because it is the panel that contains those widgets. The `STOWED` row ("Holds armour" and its tooltip) was never drawn in a real client (the sequence's frame holds no armour); "Tutti" and the arrow were exercised only in the one-off run of the follow-up, not in `m4a_shots.sh`.
5. Out of this task's scope and left as they are: `docs/test-in-gioco/m4a1-banco.md` still describes the test button (the plan gives it a note in Task 5); the reference shots in `docs/test-in-gioco/m4a1-screenshot/` were not redone (Task 4). Some pre-existing Java lines in touched files exceed 120 characters (for example `GameTests.java` 90/107/161, `RedstoneExoframes.java` 100/114); I did not touch them.

## Follow-up (after the first commit)

**What changed (`8c1f1fe`).** The first version of `StationScreen.mouseClicked` forgot the remembered widget press on any press, of any button. With the left button held on a place (in creative the mounted piece is still on the cursor), a right-button press elsewhere cleared the memory, and the later left release reached vanilla, which could throw or place the cursor stack. Now the screen remembers widget presses per mouse button (`widgetButtons`, one bit per button, plus `leftPressed` for the `PRESSED` drawing): a press on a widget sets that button's bit; a press off the widgets clears only that button's bit (a lost release of that same button can still not steal a later vanilla release); drag and release return true while the bit is set; `pressed(widget)` is tied to the left button. `docs/compat.md` says the same in the `StationScreen` paragraph.

**Verification on `8c1f1fe`.**
- `./gradlew --console=plain :1.21.1-neoforge:compileJava :1.20.1-forge:compileJava :1.20.1-fabric:compileJava :1.21.1-fabric:compileJava` → `BUILD SUCCESSFUL`.
- `EXO_ACCEPT_EULA=true bash tools/client/m4a_shots.sh 1.21.1-neoforge …/scratchpad/shots/neo2` → the same five `ok` lines and measures as before (`scelta` 972, `smontaggio` 952 and a 497 drop, `montaggio` 0/0/648 in `28x28+218+454`), `OK 1.21.1-neoforge`, `log-check.txt` empty.
- One-off run, not committed: `client_shot.sh 1.21.1-neoforge …/scratchpad/shots/widgets` with the opening steps of the sequence, then `@clic 276 260` ("All": shot `tutti`, the "All" face pressed in bronze, the white ring around each of the six pieces, header "All pieces", arrow OFF), `@clic 566 90` with "All" selected (nothing: `execute unless data … Slot:1b` passed), `@clic 244 100` and `@clic 566 90` (the arrow unmounted the helmet: `execute if data … Slot:1b … scrap_helmet` passed), `@punta 566 90` (shot `freccia`: tooltip "Unmount (goes to your inventory)", the empty helmet place with its ghost and ring, header "Helmet" / "Empty" on two dim rows, the suit without the helmet, the helmet in hotbar slot 1). Exit 0, `log-check.txt` empty.
- End-of-task list re-run on `8c1f1fe`: build with `collectJar` → `BUILD SUCCESSFUL`; JUnit (forced `--rerun`) → 405 on each variant; `OK sources`, `OK jars`, `OK data`, `OK art`, `OK arm_measure`, `OK passi`; `bash tools/run_gametests.sh` → `OK 1.20.1-forge|1.20.1-fabric|1.21.1-fabric|1.21.1-neoforge: 246 test obbligatori passati, nessun avviso fuori elenco`; `git status --short` clean.
- The forge real-client run (`…/shots/forge`) was made before this change; the change is client input handling only and the neoforge re-run covers it.
