# Task 5 review: 1dd63b6..14df684 (`client: la schermata della stazione …`)

### Spec Compliance

- ✅ **Spec compliant.** Every item in the brief has its hunk. The coordinator rulings P5, P9, P11, P12, P18, P19 and P20 are honoured, and so are the four allowed extras.
  - `test/client/StationLayoutTest.java` (new) has the seven tests the brief names:
    - `widgetsStayInsideTheScreenAndDoNotOverlap` (:40) and `screenFitsTheSmallestInterface` (diff 1366-1381);
    - `screenPointsAtNineSixtyByFiveFortyMatchTheSequence` (:109) and `conditionColoursFollowTheLegend` (diff 1402-1408);
    - `dragTurnsOneAndAHalfDegreesPerPixel` (:136), `previewViewComesFromTheStationView` (:153) and `noScreenColourIsMagentaForTheChecks` (:167).

    The values match the brief: leftPos/topPos 2/4/55/82/19; `632x464+164+38`, `184x232+596+74` and `36x32+730+410`; the five sequence points; the legend; rotate 90/30/270; the PieceInfo→PieceView mapping; and the magenta rule with the FF00FF control. I checked the points by hand. For example, (244,144) is interface point (40,53), inside TORSO 30..50×43..63, and (196,354) is (16,158), inside the first slot at 8..24×150..166.
  - `client/StationLayout.java`:
    - every rectangle of the "Disposizione" table (diff 160-191), with `PIECE_SLOTS` as an EnumMap in `PieceSlot` order;
    - opaque colours and `ALL_COLOURS` (:104);
    - `conditionColor` (:125);
    - `rotate` (:139): 1.5°/px, normalised to [0, 360), with a guard for float rounding;
    - `suitView` (:153): the ordinal of `PieceCondition.of`, and `UNPAINTED` replaced by `TierPalette`.
  - `client/StationScreen.java`:
    - `renderBg` draws in the brief's order (diff 500-520): background and edge; three panels; 36 slot frames from `menu.slots`; frame panel (:229); header (:278); preview (:326) with `<`/`>`; test button.
    - `renderLabels` draws the title and the inventory label in `TEXT`.
    - `renderContent` (:156) draws the tooltips the brief lists.
    - Mouse handling is at :171, :197 and :207. The angle starts at 0 because it is a field default.
    - `FIXED_VIEW` is gone, and nothing handles clicks on the frame panel (P5).
  - `station/StationMenu.java`:
    - `INVENTORY_X/Y`, `HOTBAR_Y` (:70-74).
    - `addPlayerSlots` (:156) adds 27 slots (inventory 9-35), then 9 (inventory 0-8). It is called from the shared private constructor (:148), and both public constructors delegate to it (:122, :131).
    - `quickMoveStack` (:272) moves inventory↔hotbar with `moveItemStackTo`. It returns a copy if something moved, otherwise `EMPTY`.
  - `gametest/StationMenuGameTests.java`: `add(tests, …)` at :130; `quickMoveSwapsInventoryAndHotbar` (:891) with the brief's steps, plus a direct `quickMoveStack` check (:916).
  - Language files: the nine keys of "Testi", in order, after `station.test_button`, identical in both files (diff 1217-1225, 1251-1259).
  - `tools/client/m4a1_shots.sh`:
    - phases `rotazione` (:187), `suggerimento` (:189) and `inventario` (:191), exactly as in the plan table;
    - `orientamento` and `CLIP_RIGHT_RECT` removed;
    - the `rotazione` check (:236-263) has the plan's conditions plus P19's two (magenta > 0, narrower than `stazione-aperta`);
    - `MAGENTA_MIN=9290` (:145) is 18582/2 rounded down to the ten; `ROT_MIN=12160` (:152) is 24332/2 = 12166, rounded down; `BUTTON_MIN` is unchanged.
  - `docs/compat.md`:
    - `CompatContainerScreen` "Uso" (diff 59);
    - the new "Avvio del client" bullet for `StationScreen`/`StationLayout` (:242);
    - the `SuitPreview` bullet without the fixed suit (diff 68);
    - "Menu e schermate", covering slots and quick move (diff 80), with the test in the list (diff 86);
    - the API-table row for `MouseHandler` (:320).
  - Allowed extras, all present:
    - the `SuitRenderer` "Uso" sentence (diff 36);
    - lines ≤ 120 characters (checked; none over in the six changed code and script files);
    - the `README.md` Shift sentence (diff 1624);
    - the `setView` Javadoc (diff 1136-1137), which is accurate: `StationMenu.java:343-344` sets `view = current` when it sends.
  - P20: the drag is anchored at the press (`StationScreen.java:181`, `:199`), the same code on both versions.
  - P12: `inside` is still used (by `pulsante` and `rotazione`), so `measure_lib.sh` correctly stays unchanged.
  - P11 / Review Focus 5: `SuitPreview` is not in the diff, so the `finally` shape from Task 1 stands.
  - Counts:
    - `step8-junit-amend.txt`: 358 on each of the four variants, 0 failures or errors.
    - `step8-gametests-final.txt`: four lines `OK … 235 …`.
    - `step8-checks-amend.txt`: all six checks OK.
    - Real-client runs: three `ok` lines plus `OK <variante>` on 1.21.1-neoforge and 1.20.1-forge, with a 0-byte `log-check.txt` in both shot folders.
  - Extras outside the brief, all doc-only and P18-compatible: `tools/client/README.md`, and the `m4a1_shots.sh` header rules paragraph (diff 1743-1745). That paragraph closes a Task 2 minor; the report names it as a bonus.
- ⚠️ **Cannot verify from the diff:**
  1. **The client evidence may predate the final `StationScreen.java`.**
     - `StationScreen.java` was last written at 02:02:40, and `tools/client/README.md` at 02:02:08. The last real-client run produced its shots at 01:58 (`t5/shots-forge`).
     - `t5/src-neo/` is empty, and the report lists no `StationScreen` change after the client run.
     - The controller should confirm the 02:02 edit was comment or Javadoc only, or rely on Task 6's four-variant run.
     - For comparison, `StationMenu.java` (written 02:07:22) differs from `t5/StationMenu.java.orig` only in the `quickMoveStack` Javadoc.
  2. **The plan departs from the design for empty places.** Design §5.3 says an empty place shows "l'icona in trasparenza del tipo di pezzo". The brief says "fondo scuro e bordo grigio", and the code follows the brief. This is a question for the plan, not a defect of the implementer.
  3. **Cross-task.** P19 changes the definition of the `rotazione` check, and Task 6's checklist and README must quote the extended check. The rotation sagoma differs by variant (24332 / 24587), so Task 6 will see four values.
  4. **A brief-mandated behaviour for the coordinator (Minor 2 below).** `StationScreen.java:207-212` hands every release to `super`, as the brief requires. So a rotation drag released outside the screen, with a stack on the cursor, throws that stack into the world, and released over a slot it places or swaps the stack. Nothing is lost, but 4a-2 will make carried stacks common. Please rule on whether the release that ends a rotation should skip `super`.

### Strengths

- **`quickMoveStack` has the right contract, and the test proves it.** The method returns `EMPTY` when nothing moves; otherwise the base game's loop would never end. I checked the three mutation logs: each ends with "1 required tests failed", naming `quick_move_swaps_inventory_and_hotbar`.
  - `t5/mutation-noslots.txt:513, :981-982`: "Index 0 out of bounds for length 0".
  - `t5/mutation-emptymove.txt:548, :1009-1010`: "after the quick move from the inventory: slot 9 holds 3 …".
  - `t5/mutation-liar.txt:544, :1006-1007`: "a quick move with a full hotbar returned 3 minecraft:iron_ingot".

  The direct call at `StationMenuGameTests.java:916` is a guard aimed at the hang.
- **The 1.21.1 drag finding is real, and the fix is minimal.** javap confirms the premise:
  - on 1.21.1, `MouseHandler.handleAccumulatedMovement` calls `mouseDragged` and `onMove` does not;
  - on 1.20.1, `onMove` calls it.

  Anchoring the angle at the press removes the jump on both versions without a seam (P20).
- **The P19 check does its job.** It rejects the recorded back view and passes the side view:
  - back view (`t5/client-neoforge-backview-solo-controlli.txt`): `KO rotazione`, box width 173 against 167 at the front;
  - final runs: width 111.
- **`StationLayoutTest` binds the sequence to the code.** It ties the sequence's click points to the layout, and `INVENTORY`/`HOTBAR` to the `StationMenu` slot corners (diff 1356-1363). It also asserts the result stays in [0, 360) over a grid that includes the float-rounding edges (`359.99997f`, `-1e-9`) and non-finite drags.
- **The screen rebuilds only when the view changes.** The icons and the preview suit are rebuilt only when `menu.view()` returns a new instance (`StationScreen.java:215-217`).
- **`pieceIcon` catches the right exception.** `PieceItem.fromPiece` throws `IllegalArgumentException` for a tier without an item (`PieceItem.java:98`), and that is exactly what the catch handles (`StationScreen.java:422`).
- **The screenshots show what the report says.** I looked at `t5/shots-neoforge/stazione-aperta.png`, `girata.png`, `suggerimento-torso.png` and `t5/zoom-frame-panel.png`:
  - green borders and the white outer ring on the helmet;
  - the red cell border;
  - "Scrap Helmet / Intact / 180 / 180" with a full bar;
  - the magenta suit from the front, then in profile;
  - the four-line torso tooltip.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

1. **`docs/compat.md:256` (the "La vista del telaio" bullet) still says the screen does not show the view.**
   - It ends "Oggi la schermata non la disegna ancora: la mostrerà il pannello del telaio." This is an unchanged context line (diff line 84).
   - After this task the sentence is false, and it contradicts the new "Avvio del client" bullet (:242).
   - It is a missed doc update under Step 7 / P18. Replace it with something like "La schermata la mostra in sola lettura (`StationScreen`, in "Avvio del client")", or delete it.
2. **Releasing a rotation drag can throw or place the stack on the cursor (brief-mandated).**
   - `StationScreen.java:207-212` always passes the release to `super`.
   - If a stack is on the cursor when a drag that began in the clip is released outside the screen, the base game sends PICKUP at slot −999 and throws the stack into the world. Released over an inventory slot, the stack is placed or swapped there.
   - I confirmed this with javap on 1.20.1 `AbstractContainerScreen.mouseReleased`:
     - offsets 22-31 set k to −1, then to the slot index, then to −999 when the release is outside the screen;
     - offsets 704-714 enter the branch for a carried stack when no quick-craft is in progress;
     - offsets 751-799 clear the Shift flag when k = −999;
     - offsets 838-859 call `slotClicked(slot, k, button, flag ? QUICK_MOVE : PICKUP)`.
   - The clip ends 8 GUI px from the right edge of the screen, so a drag to the right can easily end outside it.
   - Nothing is lost or duplicated, and the base game behaves the same way for a press on empty space released outside. But the rotation affordance makes this gesture likely, and in 4a-2 players will carry pieces on the cursor.
   - The brief mandates "`mouseReleased` smette di girare e chiama `super`", so the coordinator decides. A suggested fix: when the release ends a rotation, clear `rotating` and return `true` without `super`, or call `super` only when the cursor is empty.
3. **The build output has compiler noise.**
   - `t5/step5-build.txt:11-12, :38-39, :86-87` and `t5/step8-build-final.txt` (same lines) print "uses or overrides a deprecated API". One of them names `StationBlock.java` (Task 2).
   - These notes predate this task: they appear in the `t2` build logs, and they are the deferred Task 4 minor.
   - Nobody has run `-Xlint:deprecation` to show that Task 5's new calls add none (for example `Slot.setByPlayer`, `renderItemDecorations`, `renderComponentTooltip`).
   - A one-time `-Xlint:deprecation` compile is the coordinator's call; this review did not run Gradle.
4. **The `rotazione` width condition has no margin** (`m4a1_shots.sh:263`, `[ "$side_width" -lt "$front_width" ]`).
   - Any silhouette narrower than the front passes, so a turn of roughly 30° or 150° would too.
   - The measurements leave room for a stricter bound: side 111, front 167 (66 %), back 173. A bound such as `side_width * 4 <= front_width * 3` would reject small angles.
   - This goes beyond P19 as approved, so it is optional.
5. **Tooltips are hidden while a stack is on the cursor, including the test button's** (`StationScreen.java:157`).
   - This changes the Task 1 contract ("al passaggio del mouse più chiaro e il suggerimento"): with a stack on the cursor the button still lightens, but it shows no tooltip.
   - The change is disclosed and matches how the base game treats slot tooltips. Task 6's checklist should not expect the button tooltip with a stack on the cursor.
6. **The new in-world test does not cover a partial merge.** In `quickMoveSwapsInventoryAndHotbar` a case like this is missing: hotbar slot 0 holds 63 iron; one ingot merges and two stay; the base game's loop must stop on its second call, which returns `EMPTY`. The code handles it by contract, so a test is optional.
7. **`StationScreen.java` has grown a lot** (128 → 443 lines). It is still one cohesive screen. The 4a-2 widgets (material, repair, regions, palette, presets, costs) will grow it much further, so consider extracting the frame-panel and header drawing when those arrive.
8. **Header text wider than its widget (report concern 3): fine for 4a-1, flag for 4a-2.**
   - "Braccio sinistro: vuoto" measures 114 px against the 110 px of `NAME`; this is the report's measurement.
   - The cell's charge text is drawn right-aligned in `DURABILITY_NUMBERS` (55 px) and runs into the `CONDITION` column (the comment at `StationScreen.java` `drawHeader`, diff 676-679).
   - Neither can appear in 4a-1. The selection is fixed at opening (P5): the first mounted piece, otherwise the helmet ("Elmo: vuoto"), never 6.
   - Both go against design §5.3, "un testo … più lungo si accorcia nel file della lingua, non si allarga il widget". 4a-2 should shorten the text or lay it out again when the selection can move.
9. **The iron ingots are never cleared (report concern 4): acceptable, and plan-level.**
   - The plan's `inventario` steps have no cleanup, so the three ingots stay in the hotbar and in the hand.
   - I opened `t5/shots-neoforge/telaio-tolto.png`. It shows the ingots in hotbar slot 1 and in the hand, and the chat line "Exo has made the advancement [Acquire Hardware]", next to the expected "The exoframe is no longer available at the station". The three final shots of `client_shot.sh` change the same way.
   - None of these shots is measured, and the `rottura` checks passed.
   - A `clear $u minecraft:iron_ingot` step after the `inventario` checks would restore the earlier shots, but it is not in the plan's exact steps, so it is the coordinator's call.

### Checks outside the diff (each against a named risk)

- **Argument order of the `Piece` constructor in `pieceIcon`:** `Piece.java:14` is `(slot, tier, maxDurability, durability)`, which matches the call. Order is correct.
- **Argument order of the `EnergyCell` constructor in `refresh`:** `EnergyCell.java:9` is `(capacity, stored)`, which matches `new EnergyCell(cell.capacity(), charge(cell))`. Order is correct.
- **What `pieceIcon` catches:** `PieceItem.fromPiece` throws IAE (`PieceItem.java:98`). The catch is right.
- **Server and client menus get the same slots:** both public constructors (`StationMenu.java:122`, `:131`) delegate to the private one, which calls `addPlayerSlots` (`:148`). This holds.
- **The `setView` Javadoc claim:** `StationMenu.java:343-344` assigns `view = current` on send. The claim is true.
- **`box` and `inside` under `set -u` in the `rotazione` check:** `measure_lib.sh` `box` always sets integer `BL/BA/BX/BY` (0 when there is no box), and a failed measure gives KO. This is safe.
- **The `MouseHandler` claim (`docs/compat.md:320`, the premise of P20):** I checked it with javap on both merged jars. On 1.21.1, `mouseDragged` is called from `handleAccumulatedMovement` and not from `onMove`; on 1.20.1 it is called from `onMove`. The claim is true.
- **The base game's release path (Minor 2):** I checked it with javap on 1.20.1 `AbstractContainerScreen.mouseReleased`, offsets 22-31, 704-714, 751-799 and 838-859. A carried stack, with no quick-craft in progress, goes to `slotClicked(slot, k, button, PICKUP)`, and k = −999 outside the screen. This confirms the finding.
- **The mutation claims in the report (Strengths):** grep of `t5/mutation-*.txt`. Each log ends with "1 required tests failed", and the only test named is `quick_move_swaps_inventory_and_hotbar`, with the expected message. Confirmed.
- **Whether the plan's `inventario` steps leave visible effects (Minor 9):** I viewed `t5/shots-neoforge/telaio-tolto.png`. The ingots are in the hand and in hotbar slot 1, and the advancement chat line is there.
- **Stale text in live docs and code:** I grepped `docs/compat.md`, `README.md`, `tools/`, `src/` and `docs/test-in-gioco` for "tuta fissa", `FIXED_VIEW`, "orientamento", `CLIP_RIGHT_RECT`, "non la disegna ancora" and "non ha slot".
  - The only stale text is `docs/compat.md:256` (Minor 1).
  - `m4a1_shots.sh:89` ("con la tuta fissa") is a correct reference to Task 1.
- **Lines ≤ 120 characters:** UTF-8 character count over `StationLayout`, `StationScreen`, `StationMenu`, `StationMenuGameTests`, `StationLayoutTest` and `m4a1_shots.sh`. None is over.
- **Non-ASCII characters in the changed Java and the script:** only `§`. One read-only `git diff -- StationMenuGameTests.java` was used for this, together with read-only `rev-parse`/`status`/`log -1`. No state changed.
- **Evidence matches the report:**
  - the `ok`/`KO`/`OK` lines in `t5/client-*.txt`;
  - JUnit 358 × 4;
  - gametests 235 × 4;
  - all check scripts OK;
  - `log-check.txt` 0 bytes.
- **Restore after the mutations:** `StationMenu.java` against `t5/StationMenu.java.orig` differs only in Javadoc.
- **Where the deprecation notes come from:** they appear in the `t2` build logs, so they predate this task.
- **How I read the diff:** in passes over lines 1-118, 119-345, 345-841, 841-1005, 1005-1202, 1202-1270, 1270-1540, 1540-1635 and 1635-1941. No hunk was cut off mid-function. Changed files were touched only by the targeted greps above, for line numbers and the named risks.

### Assessment

**Task quality:** Approved

**Reasoning:**
- Every brief item is implemented as specified, with P19 and P20 applied. The quick-move contract, the layout and the press-anchored rotation are verified by real tests, mutations, bytecode checks and the screenshots.
- The remaining items are minor:
  - a stale doc sentence;
  - a brief-mandated edge case where releasing a rotation affects the stack on the cursor;
  - pre-existing build noise;
  - two 4a-2 layout and sequence notes;
  - an evidence-freshness question about a `StationScreen` edit made after the client run.
