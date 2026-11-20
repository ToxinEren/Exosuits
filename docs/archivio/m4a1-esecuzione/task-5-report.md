# Task 5 report: the station screen (read-only frame panel, rotatable preview, inventory)

Status: DONE_WITH_CONCERNS. Commit `14df684` on `m4-stazione` (signed: `gpgsig` present; both trailers). Tree clean after the commit. Not pushed.

Logs and shots are under `<scratchpad>/m4a1/t5/` (written `t5/` below).

## What I implemented

- **`client/StationLayout`**, now complete and still pure Java:
  - Every rectangle of the "Disposizione della schermata" table: `TITLE`, `FRAME_PANEL`, `PIECE_SLOTS` (an unmodifiable `EnumMap<PieceSlot, Rect>`), `CELL`, `PIECE_PANEL`, `NAME`, `CONDITION`, `DURABILITY_NUMBERS`, `DURABILITY_BAR`, `PREVIEW_PANEL`, `PREVIEW_CLIP`, `INVENTORY_LABEL`, `INVENTORY`, `HOTBAR`, `TEST_BUTTON`.
  - All the colours (opaque), with `ALL_COLOURS` listing them.
  - `conditionColor`.
  - `rotate(angle, dragX)`, which also maps a non-finite drag to "no change".
  - `suitView(StationView)`.
  - `DEGREES_PER_PIXEL = 1.5`.
  - `Rect.grow(int)`, used for the selection ring and the slot interior.
  - The 121-character line 5 is gone.
- **`client/StationScreen`**, the screen for this part. `FIXED_VIEW` is removed.
  - `renderBg` draws, in order:
    - the screen and its edge;
    - the three panels;
    - the 36 slot frames, drawn from `menu.slots`;
    - the frame panel:
      - each piece: a 1-px border in its condition colour, the slot background, then `renderItem` and `renderItemDecorations` of `PieceItem.fromPiece` with the view's durability and appearance;
      - an empty place: a dark background with a grey border;
      - the cell: a red border and the `CellItem.fromCell` icon;
      - the selection: a second white ring outside the first;
    - the header (piece / empty / cell / all pieces);
    - the preview: `SuitPreview.drawInGui` with `StationLayout.suitView(menu.view())` at the dragged angle, with `<` and `>` at the bottom of the clip;
    - the test button.
  - `renderLabels` draws the title and "Inventario" in `TEXT`.
  - `renderContent` draws the tooltips:
    - piece: four lines (slot, item name, grey durability, condition in its colour);
    - empty place: "<name>: vuoto";
    - cell: name and charge;
    - preview: "Trascina per girare", hidden while rotating;
    - test button.
  - Mouse:
    - a left click in `PREVIEW_CLIP` starts rotating and returns true;
    - `mouseDragged` with the left button, while rotating, sets the angle and returns true; otherwise it goes to `super`;
    - `mouseReleased` stops on the left button and calls `super`.
  - The angle starts at 0 at each opening and never goes to the server. No click on the frame panel does anything.
  - The suit view and the icon stacks are rebuilt only when `menu.view()` returns a new instance.
  - `SuitPreview.drawInGui` is unchanged, so Task 1's `finally` shape stays (Review Focus 5).
  - No line is over 120 characters: the old `:40`, `:66` and `:104` lines are gone.
- **`station/StationMenu`**:
  - `INVENTORY_X = 8`, `INVENTORY_Y = 150`, `HOTBAR_Y = 208`.
  - `addPlayerSlots`, called from the shared private constructor, so both public constructors add the slots in the same order, after Task 1's `DataSlot`: 27 main-inventory slots (inventory indices 9-35) then 9 hotbar slots (0-8).
  - `quickMoveStack`: inventory (0-26) to hotbar (27-35) and back, with `moveItemStackTo`. It returns the copy from before if anything moved, otherwise `EMPTY`; it also returns `EMPTY` for an out-of-range index or an empty slot.
  - The class Javadoc now says the menu has the player's slots.
  - `setView` Javadoc: see the deferred findings below.
- **`gametest/StationMenuGameTests.quickMoveSwapsInventoryAndHotbar`**, as in the brief:
  - inventory to hotbar, hotbar to inventory, and with a full hotbar (9×64 dirt) nothing moves;
  - plus a direct `menu.quickMoveStack(a, 0)` check that it returns `EMPTY` with a full hotbar, done before `clicked`. The game's loop would hang forever on a wrong non-empty return.
  - "Nothing lost" is checked with `countInMainInventory`, an empty cursor and `itemsIn` empty.
  - At the end it clears the mock player's inventory.
- **Language keys**, after `gui…station.test_button`, in the order of "Testi", in both files: `.station.cell`, `.all_pieces`, `.empty_slot`, `.durability`, `.rotate`, `gui…condition.intact|damaged|critical|broken`.
- **`tools/client/m4a1_shots.sh`**:
  - New phases "rotazione", "suggerimento" and "inventario", with the plan's exact steps. `orientamento` and `CLIP_RIGHT_RECT` are removed.
  - The new `rotazione` check.
  - `MAGENTA_MIN`, then `ROT_MIN`: provisional 100, then measured. `BUTTON_MIN` is unchanged.
  - Header rewritten: 14 phases, checks, rules, "non prova", durations.
  - `inside` is still used (by `pulsante` and `rotazione`), so `measure_lib.sh` is unchanged.
- **`docs/compat.md`** (Step 7):
  - the `CompatContainerScreen` "Uso" now describes what `StationScreen` draws in `renderBg`, `renderLabels` and `renderContent`;
  - a new "Avvio del client" bullet covers `StationScreen` and `StationLayout`;
  - the `SuitPreview` bullet no longer describes the fixed suit;
  - "Menu e schermate" covers the slots and the quick move, and the new in-world test is in the list;
  - one row in the API table for the 1.21.1 mouse finding below.

## The rotation finding, and its fix

The first 1.21.1-neoforge run passed all three checks, but `girata.png` showed the suit **from the back**, not from the side. Logs: `t5/shots-neoforge-run1-backview/`; its recheck is `t5/client-neoforge-backview-solo-controlli.txt`.

The cause was in the game, which I confirmed with `javap -c` on the jars:
- On 1.21.1, `MouseHandler.onMove` only accumulates the movement (`accumulatedDX`).
- `handleAccumulatedMovement` then sends **one `mouseDragged` per frame** with the accumulated delta.
- At the software-rendered client's low frame rate, the jump from the neutral point (10,10) to the drag start (620,250) lands in the same frame as the press. The observed back view is consistent with the first drag carrying 305 extra GUI pixels: 90° + 457.5° ≡ 187.5°. This is an inference from the bytecode and the before/after shots; nothing logged the delta, and the fix does not depend on the exact number.
- On 1.20.1, `onMove` dispatches each drag itself and has no accumulation.

Players on 1.21.1 would also see this, as a jump on a quick move-and-press.

**Fix in `StationScreen`, the same on both versions, with no seam:**
- `mouseClicked` stores the press x and the angle at that moment.
- `mouseDragged` sets `angle = StationLayout.rotate(startAngle, mouseX - startX)`. So it still uses `rotate`, with dragX being the drag since the press.

This is documented in the `mouseDragged` Javadoc, in a `docs/compat.md` bullet and table row, and in the `@trascina` row of `tools/client/README.md`. `client_shot.sh` itself behaved correctly and is unchanged.

**An addition to the `rotazione` check.** The plan's check (sagoma ≥ `ROT_MIN` inside the clip; the two magenta counts equal) passed on the back-view shots: sagoma 23548, magenta 9636 = 9636. It cannot tell 90° from 180°. I added two conditions:
- magenta in `girata` > 0;
- the girata magenta box narrower than the `stazione-aperta` one. Measured widths: front 167, side 111, back 173.

With the new conditions, the back-view shots give `KO rotazione` and the good runs give `ok`. Please confirm this addition. It changes the check's definition, which Task 6's checklist and README must quote.

## Tests and results

### TDD evidence (JUnit, `StationLayoutTest`, 7 tests)

**RED** (`t5/step1-red.txt`): `./gradlew --console=plain :1.21.1-neoforge:test --tests '*StationLayoutTest'` gave `BUILD FAILED in 46s`, with 43 `error: cannot find symbol` errors:
- `FRAME_PANEL`, `PIECE_SLOTS`, `CELL`, `PIECE_PANEL`, `NAME`, `CONDITION`, `DURABILITY_*`, `INVENTORY_LABEL`, `INVENTORY`, `HOTBAR`;
- `ALL_COLOURS`, `conditionColor`, `rotate(float,double)`, `suitView`, `grow(int)`;
- `StationMenu.INVENTORY_X/INVENTORY_Y/HOTBAR_Y`.

This was expected: the test was written against the layout this task creates.

**GREEN** (`t5/step2-green.txt`): the same command gave `BUILD SUCCESSFUL`, with `TEST-…StationLayoutTest.xml: tests="7" skipped="0" failures="0" errors="0"`.

The tests beyond the brief's wording use the same 7 test names, so the counts are unchanged:
- `widgetsStayInsideTheScreenAndDoNotOverlap` also checks:
  - every rect is non-empty;
  - `PIECE_SLOTS` holds the six slots;
  - each place's selection ring (`grow(1)`) stays inside `FRAME_PANEL` and touches no other place;
  - `INVENTORY_LABEL` is in the no-overlap set;
  - `INVENTORY`/`HOTBAR` equal the 18-px slot frames from `StationMenu`'s corners.
- `screenPointsAtNineSixtyByFiveFortyMatchTheSequence` also checks that the sequence's drag gives `rotate(0, 60) == 90`.
- `dragTurnsOneAndAHalfDegreesPerPixel` also covers NaN and ±∞ drags, and angles -90, 720 and 359.99997.
- `noScreenColourIsMagentaForTheChecks` also checks opacity (alpha `FF`) and `lighter(c)`, the hover colour.

### The in-world test is real (mutations on 1.21.1-neoforge, file restored by checksum each time)

Each mutation gave exactly `1 required tests failed`, always `quick_move_swaps_inventory_and_hotbar`:
- `t5/mutation-noslots.txt`, no slots (the brief's "fallirebbe"): "Index 0 out of bounds for length 0".
- `t5/mutation-emptymove.txt`, the old empty `quickMoveStack`: "after the quick move from the inventory: slot 9 holds 3 minecraft:iron_ingot, hotbar slot 0 holds 0 minecraft:air".
- `t5/mutation-liar.txt`, returning the copy when nothing moved: "a quick move with a full hotbar returned 3 minecraft:iron_ingot". This is the guard against the vanilla loop hanging.

### Step 5

- The four-variant build gave `BUILD SUCCESSFUL` (`t5/step5-build.txt`). The JUnit count was `358` on all four, with 0 failures or errors.
- `bash tools/run_gametests.sh` (`t5/step5-gametests.txt`) gave four lines `OK <variante>: 235 test obbligatori passati, nessun avviso fuori elenco`.

### Step 6, the real client, one variant at a time (`./gradlew --stop` before each)

`EXO_SHOT_STEPS_ONLY=true` gave `OK passi: 47` (35 before, plus 12 pointer steps).

**1.21.1-neoforge**, final run (`t5/client-neoforge.txt`, shots `t5/shots-neoforge/`): exit 0 after 146 s (139 s in `client_shot.sh`), and a 0-byte `log-check.txt`.
```
ok anteprima: ritaglio magenta=18582 riquadro=167x198+604+96; schermata magenta=18582 riquadro=167x198+604+96
ok pulsante: pixel=1112 copertura=0.21 mirino=0 barra=0 energia=0 riquadro=36x32+730+410
ok rotazione: sagoma pixel=24332 … riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96
OK 1.21.1-neoforge
```
- Thresholds: `MAGENTA_MIN` = 18582/2 rounded down to the ten = **9290**; `ROT_MIN` = 24332/2 = **12160**. The measured values are in the comments.
- `M4A1_SOLO_CONTROLLI=1` then gave the same three `ok` lines and `OK 1.21.1-neoforge` (`t5/client-neoforge-solo-controlli.txt`).
- The inventory phase, in `rcon.log`: "Replaced a slot on Exo with [Iron Ingot]", then "Test passed, count: 1" (iron in slot 0), then "Test passed" (slot 9 empty). This was the first real use of `@clic … maiuscole` and `@trascina`, and both worked.

**1.20.1-forge** (`t5/client-forge.txt`, `t5/shots-forge/`): exit 0 after 156 s (148 s in `client_shot.sh`), and a 0-byte `log-check.txt`. Rechecked with `M4A1_SOLO_CONTROLLI=1` after the last edit to the script's comments.
- `ok anteprima` 18582 / 18582, `ok pulsante` 1112, `ok rotazione` sagoma **24587**, magenta 9225 / 9225 in `111x198+629+96`, then `OK 1.20.1-forge`.
- The inventory phase passed the same way.
- Inside the screen, the Forge and NeoForge shots are identical within 4% for every shot. The 429 differing pixels per pair are in the world outside the screen.
- The front view in the clip is bit-identical. The side view differs by at most 1/255 per channel in 11906 pixels (lighting of the rotated normals). That moves pixels across the sagoma's 4% threshold, which explains 24332 against 24587. The script and README say so.

**What the shots show** (NeoForge final run; Forge is the same inside the screen):
- `stazione-aperta.png`:
  - "Maintenance Station";
  - left panel: six pieces with green borders, the helmet with a white outer ring, the cell with a red border and its icon (zoom in `t5/zoom-frame-panel.png`);
  - centre: "Scrap Helmet", "Intact" in green, "180 / 180" right-aligned, a full green bar;
  - right: the magenta suit from the front, with `<` and `>`;
  - below: "Inventory", 27 + 9 empty slots;
  - a grey "0" test button.
- `premuto.png`: the same, with the button green "1".
- `girata.png`: the suit in profile; nothing else changed.
- `suggerimento-torso.png`: a tooltip with "Torso", "Scrap Torso", grey "Durability 180 / 180" and green "Intact".
- `telaio-tolto.png`:
  - no screen, and "The exoframe is no longer available at the station" on the action bar;
  - the three iron ingots in the hand and in hotbar slot 1;
  - a chat line "Exo has made the advancement [Acquire Hardware]" from the iron ingot. This comes after the last measured shot.
- `stazione-posata.png`, `nessun-telaio.png` (both variants, `t5/montage-posata-nessun.png`): as in Tasks 2-4. The two halves are side by side, and the second shot shows "No exoframe nearby: place one on the station or within 2 blocks".

### Step 8, on the committed tree (`14df684`)

The last edits before the amend were:
- one test assertion: `rotate(0, (740 - 620) / 2) == 90`, which ties the sequence's drag to the layout;
- one `docs/compat.md` sentence: I reverted my "sulle quattro varianti" overclaim in "La schermata", because the new phases ran on two variants. The new capabilities are described, without a variant count, in the "Avvio del client" bullet.

| Command | Result |
|---|---|
| four-variant `build` + `collectJar` (`t5/step8-build-amend.txt`) | `BUILD SUCCESSFUL`; `compileTestJava` and `:test` ran on all four |
| JUnit count (`t5/step8-junit-amend.txt`) | `1.20.1-forge 358`, `1.20.1-fabric 358`, `1.21.1-fabric 358`, `1.21.1-neoforge 358`; failures+errors 0 on each |
| `bash tools/check_sources.sh` | `OK sources` (`t5/step8-checks-amend.txt`, all six rows run on the final tree) |
| `bash tools/check_jars.sh` | `OK jars` |
| `bash tools/check_data.sh` | `OK data` |
| `bash tools/check_art.sh` | `OK art` |
| `bash tools/client/arm_measure.sh --selftest` | `OK arm_measure` |
| `bash tools/client/client_shot.sh --selftest` | `OK passi` |
| `bash tools/run_gametests.sh` (`t5/step8-gametests-final.txt`) | four lines `OK <variante>: 235 test obbligatori passati, nessun avviso fuori elenco`, exit 0 after 144 s. This ran on the same main sources as the commit; only a JUnit test and a docs sentence changed after it. |

## Deferred findings closed

1. **`docs/compat.md:202-203`, the SuitRenderer "Uso".** It now reads "con la vista di `SuitDraw.view(riassunto)` = `SuitView.of(riassunto, TierPalette::primary, TierPalette::secondary)` (colori di TierPalette)". It then says the station preview has the view of `StationLayout.suitView(menu.view())`, made from the frame view the server sends.
2. **Lines over 120 characters.** The old `StationLayout.java:5` and `StationScreen.java:40/:66/:104` are gone. Every changed or new Java file is now ≤ 120 characters (checked with a UTF-8 character count; `awk` counts bytes and flags `§` lines wrongly). The shell script is ≤ 120 too.
3. **`client_shot.sh:561-590` (`@clic … maiuscole`, `@trascina`).** Their first real use works on both variants: the shift-click quick move is verified by RCON, and the drag turns the suit. The 1.21.1 misbehaviour was in the game, not in the tool. The tool is unchanged, and its README row explains the 1.21.1 accumulation.
4. **`tools/client/README.md:216`** (was `:214`). It now says that `@clic … maiuscole` holds Shift anywhere but only helps inside a screen, and that without a screen the move turns the camera. This is a text-only fix (P18).
5. **`StationMenu.setView` Javadoc.** It adds that this is for the client menu, and that on the server `broadcastChanges` rebuilds the view from the record and overwrites it at the next send.
6. **Bonus: the Task 2 minor on the `m4a1_shots.sh` header** ("la sequenza non ne da'"). The rules paragraph now says the copper ingot comes well before the first measured shot, and the iron ingots after the last one.

## Files changed

- `src/main/java/com/toxineren/exoframes/client/StationLayout.java`
- `src/main/java/com/toxineren/exoframes/client/StationScreen.java`
- `src/main/java/com/toxineren/exoframes/station/StationMenu.java`
- `src/main/java/com/toxineren/exoframes/gametest/StationMenuGameTests.java`
- `src/test/java/com/toxineren/exoframes/client/StationLayoutTest.java` (new)
- `src/main/resources/assets/redstone_exoframes/lang/en_us.json`, `it_it.json`
- `tools/client/m4a1_shots.sh`
- `docs/compat.md`
- `tools/client/README.md`. It is outside the brief's file list; these are doc-only edits under P18: the m4a1 paragraph, the checks table without `orientamento` and with `rotazione`, thresholds and measures, durations, the `@trascina` note, the Shift sentence, "la completa l'ultimo task".

## Choices the brief left open

- **Empty cell.** With no cell, the cell place is drawn like an empty piece place (dark background, grey border), and the header and tooltip say "Cella: vuoto".
- **Selection 7 (all pieces).** The white ring goes around every mounted piece. Neither 6 nor 7 can occur in 4a-1, where the selection is fixed at opening.
- **Cell header.** "10000 / 10000 RE" (90 px) is wider than `DURABILITY_NUMBERS` (55 px). It is right-aligned at x 207, so it extends into the `CONDITION` column, which the cell does not use. There is a code comment.
- **Tooltips.** Like vanilla slot tooltips, the screen's own tooltips are not shown while an item is on the cursor. This includes the Task 1 test button tooltip, a small behaviour change.
- **Tier without an item.** A piece whose tier has no item (`PieceItem.fromPiece` throws `IllegalArgumentException`) is drawn without an icon. The header then uses the slot name, and the tooltip skips the item-name line.

## Concerns

1. **The `rotazione` check is stronger than the plan's table** ("> 0" and "narrower than the front"). This was deliberate, because the plan's check let the 180° bug pass. Please confirm. Task 6 must quote the extended check.
2. **The 1.21.1 input fact** (one accumulated drag per frame) contradicts the plan's working assumption that the `dragX` deltas sum to the drag. Any 4a-2 widget that integrates `mouseDragged` deltas needs the same press-anchored approach.
3. **Italian header width.** "Braccio sinistro: vuoto" is 114 px against the 110 px of `NAME`. It ends at 194, before the 4a-2 arrow at 195, and it can only appear once 4a-2 lets the selection move to an empty left arm. English fits.
4. **The ingots stay in hand.** After the inventory phase the player holds the three iron ingots for the rest of the sequence, because the plan's steps have no cleanup. This changes the three final `client_shot.sh` shots (an ingot in hand) and adds the advancement chat line to `telaio-tolto.png`. Neither is measured, and creative breaking is unaffected (the `rottura` checks passed).
5. **Two variants only.** The client ran on the two variants the brief asks for. The rotation sagoma is variant-dependent by a few hundred pixels (24332 / 24587), far above `ROT_MIN` 12160. Task 6 will see four values.
6. **Deprecation notes.** The build still prints the pre-existing "deprecated API" notes, which are a Task 4 deferred minor. Nothing in this task's new code is deprecated as far as I know, but I did not run an `-Xlint:deprecation` compile.
