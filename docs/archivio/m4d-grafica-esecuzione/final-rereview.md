### Finding Verdicts

- **A (3c/3d sequences rerun, shots, comments, checklists)**: ADDRESSED. Report table shows 16 runs, each OK with empty log-check. The 14 shots (4 `m3c-screenshot/*riga-energia*`, 10 `m3d-screenshot/*`) are rewritten with only IHDR/IDAT/IEND (walked by me); `1.21.1-neoforge-mappa.png` shows the new copper-edged HUD. Measures updated in `tools/client/m3d_shots.sh` (23629 -> 23086, "perde 4 pixel", `93x52+867+488`, old values kept beside) and `tools/client/README.md` (m3d paragraph and `mappa` row). Notes added at the top of `m3c-giocarci.md` and `m3d-braccio.md`, steps untouched; m3a/m3b/m3c1 reread, no 4c look described.
- **B1 (selftest coverage)**: ADDRESSED. `tools/art/gui.py` `ring_texels` now checks all four sides for `scelta`. `tools/art/selftest.py` cases 24-29 cover the ring, the lighter first rows, `sagoma.assente` fill and outline, the missing top glint, the kit size and the `ValueError` in both sheets. I ran `python3 tools/art/selftest.py`: `OK selftest: 31 difetti su 31 segnalati`.
- **B2 (design §5.12 text)**: ADDRESSED. Design line 280 now has `9914161A`, the cut helmet corners and torso waist, the opaque dark-steel "!" and the dim spare-cell icon, and "mentre il fondo resta nero al 60 %" is gone. "Two departures" is kept.
- **B3 (pickers driven from the model)**: ADDRESSED. `HudLayoutTest.java` `modelPaints()` goes through `HudModel.of` for every `PieceCondition` plus the unmounted paint into `figure`. `energyStates()` over a full blink goes through `energyColor` into `barFill`, and into `warning` only where `warningOn()` is true. It asserts the warning lights for exactly LOW and EMPTY. Picker signatures unchanged.
- **B4 (Python layout tied to Java by a test)**: ADDRESSED. `tools/art/gui_layout.py` `BACKGROUND`, with `gui_officina.background()` now reading it. `gui.py` `gui_map()` and `--map-json` write `tools/art/gui_map.json`, outside `src/main/resources`. `tools/check_art.sh` regenerates it and compares with `cmp`. I ran `bash tools/check_art.sh`: `OK art`. `GuiMapTest` compares each sprite name and rectangle in both directions (swapped same-size names fail, since each name maps to its constant's rectangle). It compares each baked rectangle with `StationLayout`. `BACKGROUND_ONLY` lists the six 4b widgets by name. It also covers the silhouette pieces.
- **B5 (oracles in HudModel)**: ADDRESSED. `HudModel.java` Javadoc on `lighter` and on `Paint.outline` says drawing code does not use them and names the tests. Neither is removed.
- **C1 (compat.md)**: ADDRESSED. The `StationBlock` entry (`docs/compat.md:135`) now describes the 4d generator, the files and the check. No `orientable` or `gen_placeholders` is left in the entry. The HUD entries have no 4c flat-drawing sentence.
- **C2 (design §14)**: ADDRESSED. Design lines 645-648 each keep their text and gain one outcome clause. The `CELL_RED` item says it no longer applies, with the reason.
- **C3 (plan-decision citations)**: ADDRESSED. `grep -iE 'decisione|piano\b'` over the six named files returns nothing. The replacements are reasons or design sections, with no plan, ledger or review IDs.

### New Breakage in the Fix Diff

None. Working tree clean at `f5b3ac8`. The report shows the covering tests with commands and output: GuiMapTest, HudLayoutTest, HudModelTest and StationLayoutTest, four mutations each failing its intended test, 389 JUnit per variant, and the four checks. Constraints hold:
- no Stonecutter directive added;
- no vendor names in the diff;
- nothing new under `src/main/resources`;
- the map is under `tools/art/`.

### Out-of-Scope Observations

- `tools/client/m4c_shots.sh:16` and `:63` still cite "(Decisione N del piano)". The fix diff does not touch the file, and C3's list does not name it.
- `SuitHud.java` class Javadoc keeps a pre-existing "DEC-" plan ID. Outside C3's named comments.
- `gui_officina.background()` still hardcodes one decorative groove line (`hline(93, 10, 70)`). It is around no widget rectangle, and the implementer flagged it. The "e solo questi" wording in the new comments refers to widget rectangles.

### Verdict
**Fix round:** All findings addressed, no new Critical/Important breakage
