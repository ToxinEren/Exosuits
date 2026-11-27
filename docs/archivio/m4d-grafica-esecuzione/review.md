### Spec Compliance

✅ overall: both briefs and the owner's 27 November decisions are met. Nothing is missing, extra or misunderstood at merge level. Three ⚠️ items follow the list.

**Preview job (`74a890c`)**
- ✅ Generators exist and follow the conventions of `tools/art/`.
  - `tools/art/gui.py`, `gui_layout.py` and `gui_officina.py`.
  - Integer arithmetic only, seeds from canvas names, PNGs written with `exo.write_png`.
  - `gui.py --out` is mandatory, and the script exits 1 without writing anything when a check fails (`gui.py:87-155`).
- ✅ Screen texture `station.png`, 512x256:
  - the opaque 316x232 `fondo` at (0,0);
  - every widget of §5.3 in its states, at the design sizes;
  - transparent space outside the sprites.
- ✅ The other textures:
  - HUD texture `hud.png`, named in the report;
  - vignette `hud_vignette.png`, 144x80;
  - kit icon, 16x16, written only with `--kit`.
- ✅ The 13 previews in `docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/` are composed from the generated PNGs and committed shots. Their labels are in Italian.

**Integration (`e58533c`)**
- ✅ The three GUI textures are in `src/main/resources/assets/redstone_exoframes/textures/gui/`, and `gui.py` is their only writer.
  - `tools/gen_placeholders.py` is down to 18 outputs, none under `textures/gui/` (checked by calling `outputs()`).
  - `tools/check_art.sh:100-130` regenerates the textures and compares them with `compare_png.py`. It also refuses any file in `textures/gui/` that `gui.py` did not write.
  - `tools/art/selftest.py:495-606` adds cases 15-23.
- ✅ `SuitHud` draws everything from `hud.png` at the 4c rectangles: plate, silhouette, warning, cell icon, bar frame and fill.
  - The legend colours are exact. They are checked on the PNG in `HudLayoutTest.spritesCoverTheHudTextureAndCarryTheLegend`, line 191.
  - Rectangles and positions are unchanged: `groupSitsInTheCornerOppositeTheMainHand` and `screenRectsAtNineSixtyMatchTheSequence:333` still pin them.
  - The DEC-39 catch is kept (`SuitHud.java:186-190`).
  - Only `blit` and text are drawn at scale 1, with no 3D drawing.
- ✅ `StationScreen.renderBg` draws `FONDO`, the places, the selection ring, the durability bar, the arrows and the stand-in button from `station.png` (`StationScreen.java:133-141`).
  - The layout is unchanged.
  - The `pulsante` and `rotazione` checks of `m4a1` pass on all four variants.
- ✅ All 86 sprites have a constant named after the map, with a Javadoc. I checked this with my own script: names and rectangles of `gui.py --map` equal the Java constants, 70/70 in `StationLayout` and 16/16 in `HudLayout`. I also looked at the enlarged sheet: states and open/closed lock sit under the right names.
- ✅ Every item of §14 "Per la 4d" that the brief lists is done:
  - silhouette and warning rectangles (`HudLayoutTest:169`);
  - the bar body `122x10+52+510` (`HudLayoutTest:346`);
  - the `testo` box (`m4c_shots.sh:166`);
  - the cell red, which is no longer a constant in main code; it lives in the sprites;
  - `TWO_DIGIT_SPARE` = 18, which is really measured. I verified it on `runs/dieci/scorta-dieci.png`:
    - the white "×10" spans screen x 140-173, so GUI 70, which is 47 in the energy subgroup;
    - the red charge is at 130-133, so the icon is at 41.

  Two related items:
  - from §14 "Test in più", the `intersects`/`contains` test is added (`HudLayoutTest:181`);
  - the §14 item that is conditional, the `barra-esaurita` box, is not pinned in JUnit. See Declined to judge.
- ✅ The owner's decisions are in the code and the design:
  - HUD at the bottom, opposite the main hand: `HudLayout.group`, unchanged;
  - energy green, red under 10 % and grey at zero: `HudModel.energyColor`;
  - the 71 % edge and the bevelled channel, recorded at design:280. The record is incomplete; see Minor 2.
  - a mostly green HUD at full energy is kept.
  - `3c7a0d5` agrees with the code (Q8, Q9, and no slots: `StationMenu` holds only the player's slots).
- ✅ Design §3 constraints:
  - CI regenerates the textures: `.github/workflows/build.yml` installs `tools/art/requirements.txt` and runs `check_art.sh`.
  - All 44 PNGs added or changed in the range have only IHDR, IDAT and IEND, with valid CRCs. I walked their chunks myself.
  - `Sprite` and `GuiTexture` are client code in `client/`.
  - There are no Stonecutter directives and no mixins in the range, and no new seam. The `StationScreen` and `SuitHud` entries of `docs/compat.md` are updated.
  - There is no personal data and no model or vendor name in the diff (grep; the commit trailers are the exception the rules allow).
- ✅ Reference shots:
  - 28 are redone without metadata, and they match the previews. I compared enlarged crops:
    - the `pieno` HUD against `hud-destra-1`;
    - six HUD states (low, empty, bar only, spare cell, critical, left hand);
    - `stazione-colori-suoi` against `schermata-960x540`.
  - Every `log-check.txt` of the final runs is empty (m4c including `mancino/` and `riavvio/`, m4a1, colori).
- ✅ Tests and commits:
  - JUnit gives 386 on every variant with 0 failures and errors, read from the result XMLs. The new tests are present.
  - In `final/build.log` (lines 20, 37, 55, 71), all four `:test` tasks are UP-TO-DATE, so those results cover the committed inputs.
  - `final/gametests.log` has the four `OK <variante>: 235 test obbligatori passati` lines.
  - The four dedicated servers reach `Done (`.
  - `final/checks/` gives `OK sources`, `OK jars`, `OK data`, `OK art`, `OK arm_measure` and `OK passi`.
  - All three commits are signed and end with both trailers.
  - I re-ran no suite. After my read-only scripts, `git status --short` is still empty.

**⚠️ Could not verify**

1. **The 3c/3d sequences have not been rerun (known).** From the code, I expect every one of their checks to pass:
   - **`arm_measure.sh` `hud=`:** the rectangles `176x70+4+466` and `176x70+780+466` (`arm_measure.sh:59-60`) still equal `HudLayout.group` at 480x270.
   - **`m3d_shots.sh` criterion (b)** (`criteria_bc`, line 231): both shots of each pair carry the same HUD and vignette, so `hud=0`.
   - **`solo-tuta`:** the vanilla arm's 762 px in the bottom-right corner (`22x68+816+472`) are at corner-local x 0-10. There the new opacity, 107·(2769−r)²/2769², is at most 2/255, under the 4 % threshold.
   - **`mappa`** (`MAP_MIN_HALF`=300 against 23629 measured on the left half):
     - the plate over magenta gives (114,13,118), still magenta;
     - the 71 % copper edge now hides the magenta under its 1-px lines: (220,88,126) and (184,55,106) fail g < 0.45·b, where the 43 % edge passed. It loses a few hundred pixels at most;
     - the vignette, at 0.42 or less, keeps magenta magenta.
   - **`dipinto` and the `mancino` halves** stay at 0: neither the HUD texels nor their blends over grass and sky are magenta.
   - **3c sequences:** `m3c_shots.sh` step 8 only takes shots, and `m3c_rules_shots.sh` and `m3c2_shots.sh` do not measure the HUD.
   - **What will change:** the reference shots in `docs/test-in-gioco/m3c-screenshot/` (`*riga-energia*`) and `m3d-screenshot/`, and the measured numbers in the comments of `m3d_shots.sh` (23629, "perde 8 pixel" and others).
   - **Controller:** rerun the four sequences on four variants, redo those shots and update the numbers.
2. **The empty places have no ghost icon.** Design §5.3 (design:138) says "vuoto, l'icona in trasparenza del tipo di pezzo".
   - The `FANTASMA_*` sprites exist (`StationLayout.java:212-220`), but `drawEmptyPlace` draws only `POSTO_VUOTO` (`StationScreen.java:269-270`).
   - The 4a-1 checklist says "senza icona" (`m4a1-banco.md:25`).
   - The brief did not ask for them. However, the frame panel is a 4a-1 widget, so no plan may own this.
   - **Controller:** confirm that the 4a-2 plan draws them, or assign them.
3. **The look outside daylight at GUI scale 2 was not seen in the cloud:**
   - the vignette and its glints at night and in caves;
   - GUI scales 1, 3, 4 and automatic.

   The notes in `m4c-hud.md:11` and `m4a1-banco.md:15` send the PC tester to check them (DEC-45). **Controller:** make sure that run happens and that its findings come back.

### Strengths

- **A three-layer safety net for the art:**
  - `gui.check` refuses bad output: overlap, out of bounds, stray texels, holes in the background, inexact legend, magenta, the vignette rule. It then writes nothing.
  - The selftest proves the main refusal paths.
  - `SpriteSheet` ties every `Sprite` constant to the committed PNG through coverage, disjointness, legend colours and the magenta rule.
- **One writer for `textures/gui/`:** `check_art.sh` refuses any file there that `gui.py` did not write, as it already does for the station.
- **Blending is handled correctly and the reason is written down.** `GuiTexture.blit` turns blending on before every blit, because a text flush turns it off. The drawers turn it off in a `finally`. The HUD keeps DEC-39.
- **Inputs are checked, and zero widths are guarded:**
  - the pickers throw on unknown inputs instead of drawing a wrong sprite;
  - `Sprite.part` and `Sprite.left` check bounds;
  - both callers guard zero-width fills (`StationScreen.java:327`, `SuitHud.java:252`).
- **The `m4c` vignette check was retargeted with care.**
  - The tint is now `182C34`, the glass colour and the commonest texel, so drawing the vignette opaque would still be caught.
  - `VIGNETTE_MIN` is rederived by the documented rule.
  - The old and new measures sit side by side in the comments.
- **In game the drawings match the approved previews.** The previews rebuild byte for byte through `SHOTS_REV`.
- **The reports are candid** (the stand-in hover, the m3c/m3d gap, the stale compat line), and the claims I checked held up.
- **The checklist notes tell the PC tester exactly what changed and what to look at.**

### Issues

#### Critical

None.

#### Important

None.

#### Minor

1. **The selftest does not cover every check `gui.check` holds** (`tools/art/selftest.py:495-606` against `tools/art/gui.py:87-155`).
   - **What is wrong:** there is no negative case for:
     - the `scelta` ring (`gui.py:102`), whose check also looks only at the top row;
     - the lighter first rows of `durata.*` and `barra.*` (`gui.py:101`, `124`);
     - the fill and outline of `sagoma.assente` (`gui.py:114-120`);
     - the missing top glint (`gui.py:152`);
     - the kit size (`gui.py:153`);
     - the `ValueError` for a drawing of the wrong size in `screen_sheet` and `hud_sheet`.
   - **Why it matters:** brief step 1 asked for negative cases for "whatever else those checks hold". A check with no negative case can rot unnoticed. The JUnit backs several of these on the committed PNG, but not the generator's refusal path.
   - **Fix:** widen the `scelta` check to all four sides. Then inject one defect for each uncovered check: a texel of a first row, the alpha at the centre of `sagoma.assente`, both top glints removed, a 19x20 place.
2. **The design's record of the HUD departures is incomplete** (`docs/superpowers/specs/2026-11-19-m4-stazione-design.md:280`, against the table at :271).
   - **What is wrong:** the "Grafica della parte 4d" paragraph says there are two departures and "il fondo resta nero al 60 %". However, the plate's fill in `hud.png` is `9914161A` (gun 0, `tools/art/gui_officina.py:594`), not the table's `99101216`. The paragraph also leaves out the other look changes inside the §5.12 rectangles:
     - the helmet's top corners and the torso's waist are cut away (`gui_officina.py:602-619`), so the pieces no longer fill their rectangles, and the m4c `sagoma` count went from 1024 to 992;
     - the "!" of the warning is opaque dark steel (`gui_officina.py:649`);
     - the spare-cell icon is dim at ×0.
   - **Why it matters:** §5.12 is the record a maintainer compares the art against. An exact ARGB in its table now matches nothing, and the new paragraph says it still holds. The owner approved this look, so this is about accuracy, not a reversal.
   - **Fix:** either draw the fill with (16,18,22), or write `9914161A` and the shape and icon choices into the paragraph, one clause each. The `m4c-hud.md:11` note can stay as it is.
3. **The HUD sprite pickers are keyed on ARGB, but the test feeds them hand-picked colours** (`src/test/java/com/toxineren/exoframes/client/HudLayoutTest.java:217-220`, `244`; `HudLayout.java:220-263`).
   - **What is wrong:** `figure`, `barFill` and `warning` throw on any colour outside their `switch`. The JUnit feeds them hand-written colour lists instead of everything `HudModel` can produce.
   - **Why it matters:** no JUnit ties what `HudModel` produces to what the pickers accept. A later legend edit in `HudModel` could therefore reach the game unnoticed, for example a different colour for `LOW` or a new condition. In game, every frame's HUD would then be cut off mid-draw:
     - with `figure`, right after the plate;
     - with `barFill`, after the silhouette and the texts.

     `SuitHud.java:186-190` logs this only once per session. Nothing guards the rule "only what fills them changes" from the model's side.
   - **Fix:** drive the test from the model:
     - each `PieceCondition` through `HudModel.conditionColor` and `outline`, plus the unmounted paint, into `figure`;
     - `NORMAL`, `LOW`, `EMPTY` and no indicators through `energyColor` into `barFill`;
     - the states where `warningOn` can be true into `warning`.

     Or key the pickers on `PieceCondition` and `EnergyState` instead of ARGB.
4. **Nothing ties the background baked from Python to the Java layout** (`tools/art/gui_layout.py:14-52` and `tools/art/gui_officina.py:103-153`, against `StationLayout.java`, for example `:54`, `:62`, `:64`).
   - **What is wrong:** `FONDO` bakes several parts at positions taken from a hand-copied `GUI` dictionary:
     - the place sockets;
     - the recess of the durability bar;
     - the inventory slot frames;
     - the palette tray, the preset grooves and the cost tray.

     No automated check ties these positions to `StationLayout`. The same holds for sprite names: the coverage test does not catch two same-size sprites with swapped names, such as `TUTTI_SOPRA`/`TUTTI_PREMUTO` or `BLOCCO_CHIUSO_*`/`BLOCCO_APERTO_*`. Today everything agrees; I checked it with ad-hoc scripts (13 rectangles, 6 places, the arrows, the test button, 86/86 constants).
   - **Why it matters:** parts 4a-2 and 4b will add Java rectangles for widgets whose recesses are already baked. A drift or a swapped name would pass every check and show only in shots that nothing measures.
   - **Fix:** add a JUnit in the style of `SuitScaleTest.matchesTheArtGenerator` (`SuitScaleTest.java:97` already reads `tools/art/exo.py`). It would read `gui_layout.py`, or a JSON that `gui.py --map` could write, and compare:
     - the shared rectangles with `StationLayout`;
     - the atlas names and rectangles with the `Sprite` constants.
5. **Two members of `HudModel` survive only as test oracles** (`src/main/java/com/toxineren/exoframes/client/HudModel.java:181` and `:239`).
   - **What is wrong:**
     - `HudModel.lighter` is now used only by the tests;
     - `HudModel.Paint.outline` is never read by drawing code: `SuitHud` picks the figure by `paint.fill()` alone.
   - **Why it matters:** a reader of `HudModel` will assume both still drive the drawing.
   - **Fix:** say in their Javadoc that they are the rule the texture is tested against, or move them into the tests.

### Declined to judge

- **The pressed stand-in test button no longer lightens under the mouse** (`StationLayout.testButton:339`): this is documented in the Javadoc, the m4a1 note and the report, and 4a-2 removes the stand-in.
- **Vanilla font glyphs and item icons inside the committed previews:** the design-phase previews do the same (`docs/superpowers/specs/anteprime/2026-11-19-m4/anteprime.py:41-88`), and so do the in-game shots. No vanilla file is committed.
- **The `barra-esaurita` box `42x16+50+482` is not pinned in `HudLayoutTest`:** §14 makes that pin conditional, and `m4c_shots.sh:167` already checks the box exactly in game.
- **The thresholds kept from 4c** (`BUTTON_MIN`, `SIL_CHANGE_MIN`, `SIL_COLOUR_MIN`, `SPARE_MIN`) are above half of the new measures. That is stricter than the rule, it is documented, and they keep at least 35 % margin.
- **Blending is left off after the HUD and after `renderBg`:** that is the same end state as before, since a text flush turns it off, and NeoForge posts the HUD event after the vanilla layers.
- **The station's durability bar now has a lighter first row:** a look choice within "only what fills them". The body colour is exact (`StationLayoutTest`).
- **`gui_preview.py` reads the shots of `74a890c` with git:** it is a local-only tool, and history is never rewritten.
- **The kit icon is not in `check_art.sh` yet:** this is by design until 4b, and `gui.check` validates the icon on every run.
- **The stale `StationBlock` line in `docs/compat.md`** (report concern 4) predates `e90e46d` and is outside this range.
- **How the vignette and the HUD look at night, in caves and at GUI scales 1, 3 and 4:** DEC-45 leaves this to the PC checklist (⚠️ 3).

### Assessment

**Ready to merge?** With fixes.

**Reasoning:** The art, code, tests and docs meet both briefs and the owner's 27 November decisions, with no Critical or Important issue. Merge once the planned fix round has rerun the 3c and 3d sequences. I expect all their checks to pass, but their reference shots and the measured numbers in their comments must be redone. The five Minors can ride along, and Minors 2 and 3 are the cheapest and most useful.
