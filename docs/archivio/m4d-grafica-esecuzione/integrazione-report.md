# Report — part 4d, integration: the screen, the HUD and the vignette in style A, into the game

Status: DONE_WITH_CONCERNS. Commit `e58533c` (`e58533c849fbd2e07cd063876080c568683ae902`) on `m4-stazione`, parent
`74a890c`. It has a `gpgsig` header (signed by the environment) and ends with the two trailers; 58 files; not pushed.

`blit` needed no new seam: `GuiGraphics.blit(ResourceLocation, int x, int y, float u, float v, int w, int h, int texW,
int texH)` is the same on 1.20.1 and 1.21.1 (the 4c vignette already called it from common code), and so are
`RenderSystem.enableBlend`, `defaultBlendFunc` and `disableBlend`. No Stonecutter condition, no mixin, no new seam;
the HUD draws only text and `blit` at scale 1.

## What changed, file by file

### Art generators and checks

- `tools/art/gui.py`: now the only writer of the three GUI textures of the mod. `OUTPUTS` holds `textures/gui/station.png`,
  `hud.png` and `hud_vignette.png`; the kit icon (`textures/item/repair_kit.png`) is written only with `--kit` (it goes
  in with 4b's kit). Docstring: integrated, regenerate with `--out src/main/resources/assets/redstone_exoframes`, the
  `--map` names are the Java constants. Pixels unchanged from `74a890c`.
- `src/main/resources/assets/redstone_exoframes/textures/gui/station.png` (new, 512x256), `textures/gui/hud.png` (new,
  256x128), `textures/gui/hud_vignette.png` (replaced, 144x80): written by
  `python3 tools/art/gui.py --out src/main/resources/assets/redstone_exoframes`.
- `tools/gen_placeholders.py`: no longer writes the vignette (19 → 18 files): `draw_hud_vignette`, its constants,
  `GUI_DIR` and `import math` are gone, with a comment that `gui.py` writes it. No committed file is left without a
  writer (`textures/gui/` held only the vignette before).
- `tools/check_art.sh`: a new section after the station's runs `gui.py --out $tmp/gui` and compares every PNG with the
  committed one (`compare_png.py`). It fails if `gui.py` writes nothing, and it fails on any file in `textures/gui/`
  that `gui.py` did not write. The header is updated.
- `tools/art/selftest.py`: nine negative cases for the GUI generator's checks (15-23):
  - a sprite outside its texture;
  - two overlapping sprites;
  - a non-transparent texel outside every sprite;
  - a hole in the screen background;
  - a legend colour changed in `station.png` (a place border, a durability bar);
  - a legend colour changed in `hud.png` (silhouette, bar, warning);
  - a "magenta" texel in each of the four images;
  - the vignette outside the design rule (corner opacity, opposite sides, glints only on top);
  - `gui.py` exiting 1 and writing nothing on failing textures.

  Result: `OK selftest: 25 difetti su 25 segnalati, premesse valide`.
- `tools/art/preview/gui_preview.py`: calls `gui.py --kit` (the previews show the kit). It reads the base shots and the
  placeholder vignette with `git show` at `74a890c` (`SHOTS_REV`), because this commit redoes those shots. The 13
  previews still reproduce byte for byte. I checked this again after the redone shots were in the working tree:
  `gui.py --kit` into a scratch folder, then `gui_preview.py --vanilla <extraction> --assets <it> --out <scratch>`
  gave 13 of 13 files identical to the committed ones. Two comments now say the shot colours are those of
  `SHOTS_REV`.

### Client code (common code, no version conditions)

- `client/Sprite.java` (new): `record Sprite(u, v, width, height)`, pure Java.
  - `part(du, dv, w, h)`: a silhouette piece cut from its figure, or half of a button.
  - `left(w)`: a proportional fill.
  - Both refuse a part outside the sprite.
- `client/GuiTexture.java` (new): `record GuiTexture(id, width, height)`.
  - `blit(graphics, sprite, x, y)` turns blending on before each `blit`. `blit` does not, and text drawn in between turns
    it off when it flushes.
  - `endBlend()` is called by the drawers in a `finally`.
- `client/HudLayout.java`: `TEXTURE_W/H` 256x128 and the 16 HUD sprite constants (below). Rectangles and positions are
  unchanged. New helpers:
  - `figure(Paint)` picks the silhouette figure from the legend fill (green, yellow, red, grey, unmounted);
  - `silhouettePiece(figure, slot)` cuts the piece with today's subgroup rectangle;
  - `barFill(colour)` takes GREEN, RED, GREY or WHITE;
  - `warning(colour)` takes RED or GREY;
  - `cellIcon(spare)` gives the dim icon at 0.
- `client/HudModel.java`:
  - The flat-look constants are gone (`PLATE`, `PLATE_EDGE`, `BAR_EDGE`, `BAR_BACK`, `CELL_BODY`, `CELL_CAP`,
    `CELL_CHARGE`).
  - `ALL_COLOURS` is now only what the HUD still draws itself, its texts (GREEN, RED, GREY, WHITE).
  - The legend values are unchanged. `UNMOUNTED_FILL` and `UNMOUNTED_OUTLINE` stay: they pick the "assente" figure.
- `client/SuitHud.java`: draws everything from `hud.png`: the plate (`PIASTRA` or `PIASTRA_BARRA`), the six pieces,
  the warning, the cell icon, the bar frame and the fill (`barFill(colour).left(fillWidth)`).
  - The vignette corners come from `hud_vignette.png`, through `GuiTexture`.
  - `framed`, `drawWarning` and `drawCellIcon`, the flat fills, are gone.
  - The `RuntimeException` catch with one ERROR per session (DEC-39) is unchanged. Blending is switched off in an inner
    `finally`.
- `client/StationLayout.java`: `TEXTURE_W/H` 512x256 and the 70 screen sprite constants, each with a Javadoc (below).
  - `ARROW_LEFT` / `ARROW_RIGHT`: the two 5x7 arrows, 3 px from the clip edges, where the "<" and ">" characters were.
  - `TEST_BUTTON_HALF` = 9; `place(condition)`, `durability(condition)`, `testButton(presses, hovered)`.
  - The flat-look colours are gone (`EDGE`, `BACKGROUND`, `PANEL`, `SLOT_EDGE`, `SLOT_BG`, `EMPTY_EDGE`, `EMPTY_BG`,
    `SELECTION`, `BAR_BG`, `PREVIEW_BG`, `TEST_IDLE`, `TEST_DONE`, `CELL_RED`, `lighter`).
  - `ALL_COLOURS` keeps the text and legend colours. The layout rectangles are unchanged.
- `client/StationScreen.java`: `renderBg` blits, in this order, inside a `try`/`finally` that switches blending off:
  1. `FONDO`: the frame, the panels, the inventory slot frames and the preview background;
  2. the frame panel: `place(condition)`, `POSTO_VUOTO`, `POSTO_CELLA` and `SCELTA`;
  3. the header bar: `DURATA_FONDO`, then `durability(condition).left(filled)` or `DURATA_CELLA`;
  4. the preview: the suit, then the two arrow sprites;
  5. the stand-in test button: the two outer 9-px halves of `TUTTI_NORMALE`, `TUTTI_SOPRA` or `TUTTI_PREMUTO`, with
     the number on top.

  Behaviour is unchanged: the same rectangles, the same drag rotation and the same clicks. The m4a1 `pulsante` and
  `rotazione` checks pass on the four variants. There is one visual difference: once pressed, the button no longer
  lightens under the mouse. The sheet has no "pressed and hovered" face, so `testButton` gives `TUTTI_PREMUTO` with or
  without hover.

The sprite pickers throw on a colour outside their set, where the old fills took any colour. No reachable input does
this:

- `energyColor` is only GREEN, RED, GREY or WHITE;
- `warningOn` is true only with LOW (red) or EMPTY (grey);
- every silhouette paint is a condition colour or the unmounted fill;
- `fillWidth` stays in 0..61, because `EnergyCell` keeps stored in [0, capacity].

Any `RuntimeException` would still be caught and logged once (DEC-39).

### Tests (JUnit 381 → 386 on every variant: 5 new tests, 3 changed)

- `client/SpriteSheet.java` (new test helper):
  - loads a mod PNG from the classpath and collects a class's `Sprite` constants by reflection;
  - asserts that the sprites are inside the texture, disjoint and non-empty, that every non-transparent texel is in a
    sprite, and that no texel is magenta for `arm_measure.sh`;
  - offers assertions for exact colours, rings and opacity.
- `HudLayoutTest`, four new tests:
  - `silhouetteAndWarningRectsAreFixed`: design §14 "Per la 4d", the six piece rectangles and `WARNING` 2,8-10,16;
  - `spritesCoverTheHudTextureAndCarryTheLegend`:
    - `hud.png` is covered exactly by the sprites;
    - the plate, frame, figure and cell-icon sizes equal the rectangles they fill;
    - each figure has exactly the legend fill and outline of every paint the model gives;
    - the fills have the lighter first row, and the warnings have their colours;
  - `spritePartsStayInsideTheirSprite`;
  - `rectIntersectsAndContainsSayYesAndNo`: §14 "Test in più", a true `intersects` and a false `contains`.

  Changed: `TWO_DIGIT_SPARE` = 18 is now measured in game ("×10" on 1.21.1-neoforge: the icon at 41 in the energy
  subgroup, the text at 47). `screenRectsAtNineSixtyMatchTheSequence` also pins the bar body `122x10+52+510`.
- `HudModelTest`: `noHudColourIsMagentaForTheChecks` now checks the text colours only. The drawings' texels are checked
  on the PNG.
- `StationLayoutTest`:
  - New: `spritesCoverTheScreenTextureAndCarryTheLegend` checks:
    - `station.png` is covered exactly by the sprites;
    - `FONDO` is 316x232 and opaque;
    - the place rings carry the legend colours, `464646` for an empty place and `C81E1E` for the cell;
    - the `SCELTA` ring is white;
    - the durability bar fills, the arrow and button sizes, and `testButton`.
  - Changed: `widgetsStayInsideTheScreenAndDoNotOverlap` places the two arrows; the colour test drops the hover
    colours.
  - The cell's charge red is one private test constant `CELL_RED`. The code no longer writes it: it lives in the
    sprites (design §14: "one constant, or none if the sprite replaces it").

### Shot scripts

- `tools/client/m4c_shots.sh`:
  - `testo` also checks the exact box `46x14+50+482` (`TEXT_BOX_RIGHT`).
  - The vignette's tint check uses `182C34` instead of the placeholder's `0A0C0E`. `182C34` is the helmet glass, the
    commonest texel colour of the new texture, and no HUD drawing is within 4 % of it.
  - `VIGNETTE_MIN` goes from 320 to 160, half of the smallest new measure (326). The new vignette changes fewer pixels
    than the near-black placeholder.
  - Every other threshold stays as 4c set it. The comments carry the new measures next to the old ones, from both
    chains:
    - pieces 992 vs 1024;
    - colour 408 vs 424;
    - spare 312 vs 228;
    - vignette per variant;
    - `barra-bassa` 2064 or 1992 depending on the blink.
- `tools/client/m4a1_shots.sh`: comments only.
  - Step 7 now says "premuto (la faccia di bronzo) con 1".
  - New measures: button 852 and rotation 24245/24503.
  - `BUTTON_MIN` 550 and `ROT_MIN` 12160 stay.

### Docs

- `docs/test-in-gioco/m4c-hud.md` and `docs/test-in-gioco/m4a1-banco.md`: one note each at the top, after the existing
  notes, in the form of the 4c and 4d notes. Each says: the final style A look, legend and layout unchanged, the
  reference shots redone, what to look for. In m4a1 it also says the empty 4a-2/4b recesses are not errors, and that
  the test button is steel, then bronze.
- Design §5.12: a paragraph "Grafica della parte 4d", one sentence for each departure the owner accepted on
  27 November 2026: the copper plate edge at 71 % with relief and rivets, and the bar frame as a recessed steel
  channel.
- `tools/art/README.md` and `docs/arte/README.md`: the GUI textures are in the game, written only by `gui.py` and
  checked by `check_art.sh`.
- `README.md`, `tools/README.md`: who writes the vignette, and the check_art.sh row.
- `tools/client/README.md`: the m4c and m4a1 rows and paragraphs with the new measures.
- `docs/compat.md`: `StationScreen` and `SuitHud` draw from the textures.

### Reference shots redone (no metadata)

Every shot was converted with `convert <src> -strip -define png:exclude-chunks=date,time <dst>` from the final runs
below, and each file equals its source pixel for pixel (checked). Shots of 1.21.1-neoforge and 1.20.1-forge:

- `docs/test-in-gioco/m4c-screenshot/<v>-{pieno,danneggiato,critico,bassa,esaurita,scorta,senza-elmo,terza,vignetta-effetti,mancino-danneggiato}.png`
  (20 files: all show the HUD).
- `docs/test-in-gioco/m4a1-screenshot/<v>-{stazione-aperta,girata,suggerimento-torso,stazione-colori-suoi}.png`
  (8 files: all show the screen). `stazione-colori-suoi` comes from a separate `client_shot.sh` run with the
  documented steps, rerun on the final code.
- Not redone: `<v>-stazione-posata.png`, which has no screen. The final runs' shots equal the committed ones (0
  differing pixels on both variants).

## Sprite constants

All 86 sprites of `python3 tools/art/gui.py --map` have a constant named after the map (upper case, `.` → `_`), with
a Javadoc naming the widget and its state.

- `SpriteSheet.constants` reads them by reflection, so a sprite without a constant, or a constant outside the
  texture, fails the JUnit.
- I checked the names and rectangles against `--map` with a script: 70 of 70 in `StationLayout`, 16 of 16 in
  `HudLayout`.
- The map's 30 "pezzi della sagoma" are not separate sprites. `HudLayout.silhouettePiece(figure, slot)` cuts them
  from the five figures; the offsets match the map for all 30 (helmet +8,+3, torso +7,+10, arms +3/+16,+10, legs
  +7/+12,+22).
- The vignette's four corners are private `Sprite`s in `SuitHud` (`VIGNETTE_CORNERS`: corner i in column i % 2, row
  i / 2).

`StationLayout` (size: constant (u,v)):

- 316x232: `FONDO` (0,0)
- 30x16: `TUTTI_NORMALE` (318,2), `TUTTI_SOPRA` (318,18), `TUTTI_PREMUTO` (318,34), `TUTTI_SPENTO` (318,50)
- 44x16: `RIPARA_NORMALE` (350,2), `RIPARA_SOPRA` (350,18), `RIPARA_PREMUTO` (350,34), `RIPARA_SPENTO` (350,50)
- 59x16: `RIPARA_TUTTO_NORMALE` (396,2), `RIPARA_TUTTO_SOPRA` (396,18), `RIPARA_TUTTO_PREMUTO` (396,34),
  `RIPARA_TUTTO_SPENTO` (396,50)
- 18x16: `APPLICA_NORMALE` (457,2), `APPLICA_SOPRA` (457,18), `APPLICA_PREMUTO` (457,34), `APPLICA_SPENTO` (457,50),
  `SALVA_NORMALE` (477,2), `SALVA_SOPRA` (477,18), `SALVA_PREMUTO` (477,34), `SALVA_SPENTO` (477,50)
- 14x12: `RINFORZATO` (497,2), `TINTA_SOPRA` (486,92)
- 62x12: `REGIONE_NORMALE` (318,68), `REGIONE_SOPRA` (318,80), `REGIONE_PREMUTO` (318,92), `REGIONE_SPENTO` (318,104)
- 12x12: `SMONTA_NORMALE` (382,68), `SMONTA_SOPRA` (382,80), `SMONTA_PREMUTO` (382,92), `SMONTA_SPENTO` (382,104)
- 16x12: `BLOCCO_CHIUSO_NORMALE` (396,68), `BLOCCO_CHIUSO_SOPRA` (396,80), `BLOCCO_CHIUSO_PREMUTO` (396,92),
  `BLOCCO_CHIUSO_SPENTO` (396,104), `BLOCCO_APERTO_NORMALE` (414,68), `BLOCCO_APERTO_SOPRA` (414,80),
  `BLOCCO_APERTO_PREMUTO` (414,92), `BLOCCO_APERTO_SPENTO` (414,104)
- 18x18: `MATERIALE` (432,68), `MATERIALE_SPENTO` (432,88)
- 16x16: `MATERIALE_VELO` (452,68), `FANTASMA_ELMO` (440,118), `FANTASMA_TORSO` (456,118), `FANTASMA_BRACCIO`
  (472,118), `FANTASMA_GAMBA` (488,118), `FANTASMA_CELLA` (456,134)
- 5x7: `FRECCIA_SINISTRA` (470,68), `FRECCIA_DESTRA` (477,68), `FRECCIA_SINISTRA_SOPRA` (470,77),
  `FRECCIA_DESTRA_SOPRA` (477,77)
- 22x22: `SCELTA` (486,68)
- 20x20: `POSTO_INTEGRO` (318,118), `POSTO_DANNEGGIATO` (338,118), `POSTO_CRITICO` (358,118), `POSTO_ROTTO` (378,118),
  `POSTO_VUOTO` (398,118), `POSTO_CELLA` (418,118)
- 12x10: `TINTA` (318,140), `TINTA_SPENTA` (332,140)
- 14x22: `PREDEFINITO` (346,140); 16x24: `PREDEFINITO_SOPRA` (362,140)
- 74x10: `PRESET_COLORI` (380,140), `PRESET_VUOTO` (380,152)
- 126x5: `DURATA_FONDO` (2,234), `DURATA_INTEGRO` (2,240), `DURATA_DANNEGGIATO` (2,246), `DURATA_CRITICO` (130,234),
  `DURATA_ROTTO` (130,240), `DURATA_CELLA` (130,246)

`HudLayout`:

- 88x35: `PIASTRA` (0,0); 67x35: `PIASTRA_BARRA` (0,36)
- 21x35: `SAGOMA_INTEGRO` (90,0), `SAGOMA_DANNEGGIATO` (112,0), `SAGOMA_CRITICO` (134,0), `SAGOMA_ROTTO` (156,0),
  `SAGOMA_ASSENTE` (178,0)
- 63x8: `BARRA_CORNICE` (69,37); 61x6: `BARRA_VERDE` (69,46), `BARRA_ROSSA` (69,53), `BARRA_GRIGIA` (69,60),
  `BARRA_BIANCA` (69,67)
- 8x8: `AVVISO_ROSSA` (134,37), `AVVISO_GRIGIA` (143,37); 5x8: `CELLA` (152,37), `CELLA_SPENTA` (158,37)

Drawn today:

- from `station.png`: `FONDO`, the six `POSTO_*`, `SCELTA`, the six `DURATA_*`, `FRECCIA_SINISTRA` and
  `FRECCIA_DESTRA`, and halves of `TUTTI_NORMALE`, `TUTTI_SOPRA` and `TUTTI_PREMUTO` for the stand-in button 102;
- from `hud.png`: all 16 HUD sprites.

Waiting for 4a-2/4b: every other screen sprite.

## In game against the previews

Shots (all committed, see above):

- HUD:
  - `docs/test-in-gioco/m4c-screenshot/1.21.1-neoforge-pieno.png` (full HUD and vignette);
  - `-bassa.png` (warning, red fill);
  - `-mancino-danneggiato.png` (left hand);
  - `-vignetta-effetti.png`.
- Screen:
  - `docs/test-in-gioco/m4a1-screenshot/1.21.1-neoforge-stazione-colori-suoi.png`;
  - `-suggerimento-torso.png` (the pressed bronze button with "1", and a tooltip).

**HUD** (against `hud-destra-1-energia-piena.png` and `hud-destra-2-…png`, compared on enlarged crops of the group):
the plate, the copper edge with its relief and the four bronze rivets, the silhouette figures, the warning triangle,
the cell icon and the steel bar channel with its quarter notches are the same drawings, at the same pixels.

- All eight states look right: pieno, danneggiato, critico, bassa (red warning, 5 %, red fill), esaurita (grey warning,
  empty channel), scorta (lit icon, "×1"), senza-elmo (the bar-only plate), and mancino-danneggiato (the silhouette on
  the right edge, the inside not mirrored).
- Differences, none from the drawings:
  - The dev client is English, "100%"; the previews are Italian, "100 %".
  - The previews show "×2" with a lit icon; the sequence has no spare cell, "×0" with the dim icon.
  - Through the translucent plate the game shows the real world; the preview showed mirrored grass from an old shot.
  - Between 1.21.1-neoforge and 1.20.1-forge the group region differs only in about 100 pixels of world seen through
    the plate.

**Vignette** (against the "nuova" corners of `vignetta.png`): the same. The corners darken from the edge steel towards
the dark glass green, with the two light diagonal glints in the top corners.

- The effect icon (`vignetta-effetti`) behaves as documented in 4c: on NeoForge 1.21.1 it sits above the vignette; on
  the other three the vignette covers it. In pixel (930, 25) the green channel is 76 on NeoForge and 69 on the three
  others, and the 60x60 corner is identical on those three.

**Screen** (against `schermata-960x540.png`): `FONDO` matches the preview: the bronze-framed brushed steel plate, the
recessed panels, the inventory slots and the three-band preview background. So do the place frames with the legend
borders, the white selection ring, the durability channel, the bronze turntable and corner brackets, and the two
bronze arrows.

Differences, all expected:

- The 4a-2/4b widgets of the preview are not drawn yet, so their recesses are empty: the palette tray, the preset rows
  and the cost box.
  - The preview's other widgets are absent too: Ripara, Tutto, Primario/Secondario, the material box, eject, lock,
    shield and Tutti.
- The stand-in test button sits where 4a-1 put it, at the bottom right of the preset panel. It is steel with "0" and
  bronze with "1" once pressed.
- The dev client is English.
- The sequence paints the suit magenta; `stazione-colori-suoi` shows its own colours.
- The colori shot from the final code equals the earlier colori run inside the screen (`632x464+164+38`: 0 differing
  pixels). It differs only in the world around the screen.

## Runs on the real client (final chain, code as committed)

Each variant ran, one at a time, `EXO_ACCEPT_EULA=true bash tools/client/m4c_shots.sh <v> <dir>` and
`EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh <v> <dir>`, with dirs under
`<cartella temporanea della sessione>/final/`.

- Every run exited 0, with `OK <v>`.
- Every `log-check.txt` is empty, including m4c's `mancino/` and `riavvio/`.
- There are no `KO` or `FAIL` lines.
- Every ok line below is from these live runs. The earlier chain and its recheck are superseded.
- After the runs, only comments changed in `m4c_shots.sh` (the measures of both chains). I then ran
  `M4C_SOLO_CONTROLLI=1 bash tools/client/m4c_shots.sh <v> <final folder>` with the committed script on all four
  folders. Each gave exit 0, `OK <v>` and the same 17 ok lines as its live run.

| run | 1.21.1-neoforge | 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric |
|---|---|---|---|---|
| m4c (A/B/C, s) | 17 ok, 149/94/79 | 17 ok, 149/99/87 | 17 ok, 142/88/75 | 17 ok, 140/88/80 |
| m4a1 (s) | 3 ok, 122 | 3 ok, 129 | 3 ok, 119 | 3 ok, 117 |

m4c, 1.21.1-neoforge:

```
ok chat-e-tasti: True False
ok riavvio-file: False False
ok gruppo: pixel=12320 riquadro=176x70+4+466; sagoma pixel=2940 riquadro=42x70+4+466
ok testo: colore=244 riquadro=46x14+50+482
ok barra-normale: colore=1220 riquadro=122x10+52+510
ok sagoma: pixel=992 riquadro=32x60+10+472; verde prima colore=408 riquadro=28x56+12+474; giallo dopo colore=408 riquadro=28x56+12+474; verde dopo colore=0 riquadro=0x0+0+0
ok critico: rosso colore=408 riquadro=28x56+12+474; giallo colore=0 riquadro=0x0+0+0
ok barra-bassa: pixel=2064 riquadro=124x38+50+482; rosso nella barra colore=60 riquadro=6x10+52+510; verde nella barra colore=0 riquadro=0x0+0+0; rosso nella sagoma colore=0 riquadro=0x0+0+0
ok barra-esaurita: verde nella barra colore=0 riquadro=0x0+0+0; rosso colore=0 riquadro=0x0+0+0; grigio colore=248 riquadro=42x16+50+482
ok scorta: pixel=312 riquadro=34x16+142+482; grigio prima colore=112 riquadro=22x14+152+482; bianco dopo colore=84 riquadro=22x14+152+482
ok solo-barra: pixel=9380 riquadro=134x70+46+466; sagoma pixel=0 riquadro=0x0+0+0
ok terza-persona: pixel=12320 riquadro=176x70+4+466; angoli pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0
ok vignetta: schermo pixel=10119 riquadro=960x540+0+0, somma degli angoli 10119; angoli pixel=3916 riquadro=96x52+0+0; pixel=3916 riquadro=96x52+864+0; pixel=326 riquadro=59x32+0+508; pixel=1961 riquadro=78x44+882+496; tinta 182C34 in pieno/senza-vignetta per angolo 0/0 0/0 0/0 0/0
ok mancino-gruppo: pixel=12320 riquadro=176x70+780+466; sagoma pixel=2940 riquadro=42x70+914+466; percentuale colore=244 riquadro=46x14+784+482
ok mancino-sagoma: pixel=992 riquadro=32x60+920+472; giallo colore=408 riquadro=28x56+922+474
ok mancino-solo-barra: pixel=9380 riquadro=134x70+780+466; sagoma pixel=0 riquadro=0x0+0+0
ok riavvio: pixel=0 riquadro=0x0+0+0; pixel=12320 riquadro=176x70+4+466
OK 1.21.1-neoforge
```

On the other three variants every ok line is byte-identical to these, except:

```
1.20.1-forge   ok vignetta: schermo pixel=10121 riquadro=960x540+0+0, somma degli angoli 10121; angoli pixel=3916 riquadro=96x52+0+0; pixel=3916 riquadro=96x52+864+0; pixel=328 riquadro=59x32+0+508; pixel=1961 riquadro=78x44+882+496; tinta 182C34 in pieno/senza-vignetta per angolo 0/0 0/0 0/0 0/0
1.21.1-fabric  ok barra-bassa: pixel=1992 riquadro=124x38+50+482; rosso nella barra colore=60 riquadro=6x10+52+510; verde nella barra colore=0 riquadro=0x0+0+0; rosso nella sagoma colore=0 riquadro=0x0+0+0
```

- 1992 means the warning was off when the shot was taken. That count has no threshold, as in 4c.
- In the earlier chain, 1.20.1-forge measured 3914/3916/328/1937; both are now written in the comments.

m4a1, 1.21.1-neoforge (1.21.1-fabric identical):

```
ok anteprima: ritaglio magenta=18582 riquadro=167x198+604+96; schermata magenta=18582 riquadro=167x198+604+96
ok pulsante: pixel=852 copertura=0.16 mirino=0 barra=0 hud=0 riquadro=32x28+732+412
ok rotazione: sagoma pixel=24245 copertura=4.68 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio magenta=9225 riquadro=111x198+629+96
OK 1.21.1-neoforge
```

1.20.1-forge and 1.20.1-fabric: identical, except `ok rotazione: sagoma pixel=24503 copertura=4.73 …`, with the rest
of that line the same. This is the 1.20.1/1.21.1 difference that 4a-1 already had (24587/24332).

`stazione-colori-suoi`, one separate `client_shot.sh` run each, final code: 1.21.1-neoforge 85 s and 1.20.1-forge
82 s. Both exited 0 with an empty `log-check.txt`.

## Dedicated server

The servers ran as in earlier parts: `./gradlew :<v>:runServer` with `eula=true`, a fresh world, `stop` after
`Done (`, then `python3 -I tools/check_log.py client --variant <v> versions/<v>/run/logs/latest.log`.

| variant | ready | exit | check_log |
|---|---|---|---|
| 1.20.1-forge | `Done (2.376s)` | 0 | exit 0, no output |
| 1.20.1-fabric | `Done (2.301s)` | 0 | exit 0, no output |
| 1.21.1-fabric | `Done (0.976s)` | 0 | exit 0, no output |
| 1.21.1-neoforge | `Done (0.660s)` | 0 | exit 0, no output |

## End-of-task outputs

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
  :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` →
  `BUILD SUCCESSFUL in 14s`.
  - The four `:test` tasks were UP-TO-DATE: the sources were unchanged since the JUnit run at 15:23, which passed.
- JUnit count: `1.20.1-forge 386`, `1.20.1-fabric 386`, `1.21.1-fabric 386`, `1.21.1-neoforge 386`, with 0 failures,
  errors or skipped (381 before).
  - New tests: `HudLayoutTest.silhouetteAndWarningRectsAreFixed`, `spritesCoverTheHudTextureAndCarryTheLegend`,
    `spritePartsStayInsideTheirSprite`, `rectIntersectsAndContainsSayYesAndNo`, and
    `StationLayoutTest.spritesCoverTheScreenTextureAndCarryTheLegend`.
  - Changed: `HudLayoutTest.screenRectsAtNineSixtyMatchTheSequence` (bar body) and `TWO_DIGIT_SPARE` (measured);
    `HudModelTest.noHudColourIsMagentaForTheChecks`; `StationLayoutTest.widgetsStayInsideTheScreenAndDoNotOverlap`
    (arrows) and its colour test (no hover colours).
- `bash tools/check_sources.sh` → `OK sources`
- `bash tools/check_jars.sh` → `OK jars`
- `bash tools/check_data.sh` → `OK data`
- `bash tools/check_art.sh` → `OK art` (40 s; selftest 25/25)
- `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure`
- `bash tools/client/client_shot.sh --selftest` → `OK passi`
- `bash tools/run_gametests.sh` →
  ```
  OK 1.20.1-forge: 235 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 235 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 235 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 235 test obbligatori passati, nessun avviso fuori elenco
  ```
- `git status --short` after the commit: empty. No stray shots or run folders; run folders are git-ignored.

## PNG chunk check (every new or changed PNG)

Every file below has only `IHDR`, `IDAT` and `IEND`, checked by walking the chunks. The shots are 960x540, 8-bit RGB.

- `textures/gui/station.png` 512x256, `textures/gui/hud.png` 256x128, `textures/gui/hud_vignette.png` 144x80.
- The 20 `docs/test-in-gioco/m4c-screenshot/{1.21.1-neoforge,1.20.1-forge}-*.png` listed above.
- The 8 `docs/test-in-gioco/m4a1-screenshot/{1.21.1-neoforge,1.20.1-forge}-{stazione-aperta,girata,suggerimento-torso,stazione-colori-suoi}.png`.

## Concerns

1. **m3c and m3d reference shots still show the 4c look.** `docs/test-in-gioco/m3c-screenshot/` and
   `m3d-screenshot/` have shown the HUD since 4c, and the vignette in first person with the helmet. They still show the
   flat HUD and the placeholder vignette: the brief limited the redo to m4c and m4a1, so `m3c_shots.sh` and
   `m3d_shots.sh` were not rerun.
   - I expect their checks to pass with the new look. This is argued from the code, not run.
   - `m3c_shots.sh` measures nothing of the HUD; its checks are commands.
   - For `m3d_shots.sh`: the design rule caps the corner opacity at 0.42, which `gui.py` checks. Under that cap any
     tint keeps FF00FF magenta for `arm_measure.sh` (worst case, white over it: g = 107 < 0.45 × 255).
   - Where the base arm enters the bottom-right corner, the new texture's opacity is still at most 2/255 (checked), so
     the comment of `m3d_shots.sh` stays true.
   - Only its vignette-era measured numbers in comments (for example "the map loses 8 pixels") come from the
     placeholder.
2. **Some sprites are unused until 4a-2/4b.** The stand-in test button draws two 9-px halves of the 30-px `TUTTI_*`
   face; 4a-2 replaces the stand-in. `FRECCIA_*_SOPRA` and `TUTTI_SPENTO` have constants but are not drawn: arrow
   hover was left out, because the brief asks for unchanged behaviour.
3. **The 4a-1 screen shows empty recesses** where 4a-2/4b will put the palette, the presets and the cost line. The
   m4a1 note tells the tester they are not errors.
4. **A stale line in `docs/compat.md` (pre-existing, not touched).** In the `StationBlock` entry it still says the
   station's models have parent `minecraft:block/orientable` and use textures from `tools/gen_placeholders.py`.
   `tools/art/station.py` has written them since the station's 4d step.
5. **The `:test` tasks were UP-TO-DATE in the final build.** The JUnit results come from the 15:23 run on the same
   inputs. After it, only comments in `m4c_shots.sh`, `tools/client/README.md` and `gui_preview.py` changed, plus the
   reference shots.
6. **Design §14 "Per la 4d" is left as written, although every item on it is done.** The design is a record, and the
   brief did not ask for it to be edited. So it still names `HudModel.CELL_CHARGE` and the "dedotta" two-digit width.
7. **The pressed test button no longer lightens under the mouse** (see StationScreen above). It is a stand-in that
   4a-2 replaces.

## Fix round 1

Status: DONE_WITH_CONCERNS (the concerns are small and listed at the end). Two commits on `m4-stazione`, both with a
`gpgsig` header (signed by the environment) and both trailers, not pushed:

- `d77c36502eec460f03cbabae4dc39e0e0fb010aa`: code, tests, the generated map and the docs;
- `f5b3ac8b0b83730b973fdaf0cd0b4317d5ef596a`: the 14 redone reference shots.

No game behaviour changed: in `src/main` only Javadoc and comments changed, so no gametest, m4c/m4a1 sequence or
server run was needed.

### A. The 3c and 3d client sequences (review ⚠️ 1)

All 16 runs used the code of `e58533c`: they ran before any edit under `src/` or `tools/client/`, and at the end of
the batch `git status --short -- src tools/client` was empty. Command, one at a time:
`EXO_ACCEPT_EULA=true bash tools/client/<seq>_shots.sh <variant> <dir>`, with the dirs under
`<cartella temporanea della sessione>/fix1/<variant>/<seq>`.

| sequence | 1.21.1-neoforge | 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric |
|---|---|---|---|---|
| `m3c_shots.sh` | `OK 1.21.1-neoforge`, log-check empty, 141 s | `OK 1.20.1-forge`, empty, 130 s | `OK 1.20.1-fabric`, empty, 115 s | `OK 1.21.1-fabric`, empty, 110 s |
| `m3c2_shots.sh` | `OK 1.21.1-neoforge`, empty, 80 s | `OK 1.20.1-forge`, empty, 84 s | `OK 1.20.1-fabric`, empty, 74 s | `OK 1.21.1-fabric`, empty, 76 s |
| `m3c_rules_shots.sh` | `OK 1.21.1-neoforge`, empty, 86 s | `OK 1.20.1-forge`, empty, 96 s | `OK 1.20.1-fabric`, empty, 82 s | `OK 1.21.1-fabric`, empty, 84 s |
| `m3d_shots.sh` | `OK 1.21.1-neoforge`, 14 ok, both log-checks empty (also `mancino/`), 202 s | `OK 1.20.1-forge`, 14 ok, both empty, 214 s | `OK 1.20.1-fabric`, 14 ok, both empty, 190 s | `OK 1.21.1-fabric`, 14 ok, both empty, 196 s |

Every OK line reads `OK <variant>: scatti in <dir>`; no `KO`, `FAIL` or `moved wrongly`. No check failed, so no
threshold and no code changed for A.

**m3d measures.** On all four variants 12 of the 14 ok lines are byte-identical to each other. They are also equal
to the measures in the script's comments (18075, 39962, 8.77, 7.72, 0 uguali, 258, 100 %, 0 mirror offsets),
except `mappa`. The two lines that vary between runs stay inside their documented ranges:

- `terza-persona`: 23273, 23166, 23331 and 23354, against "fra 23147 e 23745";
- `inventario`: 10826 and 10800 on 1.21.1 (`98x140+356+120`); on Forge 18740, 166 wide at x 323; on Fabric 18789,
  172 wide at x 320.

The only measure that changed, the same on the four variants: `mappa`, left half, **23629 → 23086**; right half
28977, unchanged.

**The header numbers no ok line prints** (re-measured with one more `client_shot.sh` run on 1.21.1-neoforge, `e58533c`
code, log-check empty). It took shots with the vignette key (J) on and off and with H (HUD hidden), and measured them
with `arm_measure.sh`:

- arm magenta 39962 with the vignette, without it and without the HUD: "il magenta del braccio non cambia" still
  holds;
- map, left half: 23086 with HUD and vignette, 23090 with the vignette off, 28407 with the HUD hidden. So with the 4d
  art the vignette costs **4** pixels (8 with the 4c placeholder) and the HUD **5317** (4770 in 4c, 28407 − 23637):
  the 71 % copper edge hides the magenta under its 1-pixel lines. 23629 − 23086 = 543 = (5317 − 4770) − (8 − 4);
- vignette on/off differ only in the four corners: 3649 + 3635 + 355 + 3231 = 10870 px;
- in the bottom-right corner the darkening arm_measure sees is in **`93x52+867+488`** (was `92x56+868+484`);
- in the base arm's area of that corner, `22x68+816+472`, the vignette changes 0 pixels;
- the base arm still enters the corner with 762 of its 18075 pixels (`differenza base-prima base-senza
  144x80+816+460` → `pixel=762 riquadro=22x68+816+472`);
- the texture's alpha there is at most 2/255, read from `hud_vignette.png`, corner-local x 0-10, y 6-39.

These shots equal the sequence's shots within arm_measure's threshold (`differenza` 0 for `base-prima`,
`braccio-rottame` and `mappa`).

**Comments updated** (old values kept next to the new ones, as the script already did):

- `tools/client/m3d_shots.sh`:
  - :21-29: 4 pixels with the 4d vignette (8 before); the base-arm claim re-measured; `93x52+867+488`
    (`92x56+868+484` before);
  - :83-86: the measures were re-measured with the 4d art, which changes only the map's left half;
  - :128-133: `MAP_MIN_HALF`'s comment gives 23086, then 23090 and 28407 with the vignette off and the HUD hidden,
    the HUD's 5317 and the vignette's 4, and the 4c values.
- `tools/client/README.md`: the m3d paragraph (:130) and the `mappa` row of the table (:142), with the same numbers.

After these comment-only edits,
`M3D_SOLO_CONTROLLI=1 bash tools/client/m3d_shots.sh <variant> <m3d dir>` on the four folders gave exit 0, `OK
<variant>` and the same 14 ok lines as each live run. `bash -n` passes, and the diff of the script has only comment
lines. The m3c scripts' comments carry no HUD or vignette measure; their durations were left as written (see
concerns).

**Reference shots redone (14).** Each was made with `convert <src> -strip -define png:exclude-chunks=date,time <dst>`
and keeps its variant prefix. Each has only IHDR, IDAT and IEND (walked) and is 960x540; `compare -metric AE` with its
source gives 0.

- `docs/test-in-gioco/m3c-screenshot/`, same names, new content (checklist steps 24 and 26 and the shot list cite
  `*riga-energia*`, and the steps must not change, as in 4c):
  - `1.20.1-forge-riga-energia.png` and `1.21.1-neoforge-riga-energia.png`, from `hud-energia.png`;
  - `1.21.1-fabric-riga-energia-bassa.png`, from `hud-energia-bassa.png`;
  - `1.20.1-fabric-riga-energia-esaurita.png`, from `hud-energia-esaurita.png`.
- `docs/test-in-gioco/m3d-screenshot/`, for both `1.21.1-neoforge` and `1.20.1-forge`:
  - `-braccio-rottame`, `-braccio-nudo` and `-mappa`, from the m3d runs;
  - `-braccio-mancino`, from `mancino/braccio-mancino.png`;
  - `-braccio-colori-suoi`, from a separate `client_shot.sh <v> <dir> "@attendi 12" "execute as Exo run exoframe
    debug wear rottame" "@attendi 4" "@scatto braccio-colori-suoi"` (exit 0, empty log-check, 84 s and 81 s).
- Not redone: the other 3c shots.
  - `vasca-spazio`, `barre-inventario` and `cambio-cella-tenuto` still show the 3c energy line, not the HUD; they
    stay, as 4c decided and as the 4c note in `m3c-giocarci.md` says.
  - `caricatore-*` and `regole-barra` have no suit on.

**Checklists.**

- New notes at the top, after the existing notes, in the files' form ("Con i jar di dopo la grafica dell'interfaccia
  della parte 4d … i passi restano come sono"):
  - `docs/test-in-gioco/m3c-giocarci.md:22`: the HUD and vignette have the final look (pointer to the `m4c-hud.md`
    note), what each piece looks like (the warning of steps 24 and 26 included), and the `riga-energia` shots
    redone;
  - `docs/test-in-gioco/m3d-braccio.md:11`: the look at step 3 and the shots redone.
- `m3a-vedere-il-telaio.md`, `m3b-entrare-e-uscire.md` and `m3c1-consumare.md`, reread: no sentence describes the
  flat 4c look.
  - m3a and m3b give the HUD's position and contents and "la vignetta scura", still true, and their shots have no
    HUD.
  - m3c1 is only a pointer.
  - So no note.

### B1. Selftest coverage

- `tools/art/gui.py:86` `ring_texels`: the four sides of a 1-pixel ring. It is used for the places (`:106`) and now for
  `scelta` (`:111`), which checked only the top row before.
- `tools/art/selftest.py`, six new cases (docstring updated). Each patched atlas or function is restored in a
  `finally`.
  - 24 (`:649`): one texel of the `scelta` ring flipped at the middle of each side. Against the `e58533c` check the
    bottom, left and right defects pass unnoticed: old False, new True, run side by side.
  - 25 (`:666`): the lighter first row of `durata.integro` (station.png) and of `barra.verde` (hud.png), matched on
    "prima riga".
  - 26 (`:682`): `sagoma.assente`, the alpha of the torso's centre texel and the red of its bottom outline.
  - 27 (`:702`): both top glints removed by colour only, set to the glass green with the same alpha.
  - 28 (`:717`): a 16x17 kit icon.
  - 29 (`:726`): a 19x20 `posto.critico` makes `screen_sheet` raise `ValueError`, and a 5x7 `cella.spenta` does the
    same in `hud_sheet`; both messages name the sprite.
  - Case 23 (`:631`) now also passes `--map-json`, and still finds nothing written.
- `tools/art/README.md:96-109` and `:516-526`: the case list (24-29), and the checks paragraph with the ring, the
  first rows, the unmounted figure, the top glints, the kit size and the `ValueError`.

### B2. The design's record of the HUD art

`docs/superpowers/specs/2026-11-19-m4-stazione-design.md:280`, "Grafica della parte 4d": the two departures are
unchanged, and "mentre il fondo resta nero al 60 %" is gone. A new sentence says the rest sits inside the table's
rectangles, as in the previews the owner saw, with one clause each:

- the plate fill stays at 60 % but in the style's darkest steel, `9914161A`, not `99101216`;
- the helmet's cut top corners and the torso's waist, so those two pieces no longer fill their rectangles;
- the opaque dark-steel "!" of the warning (`FF101114`);
- the dim spare-cell icon at "×0".

All four values were read from `hud.png`.

### B3. The HUD sprite pickers driven from the model

`src/test/java/com/toxineren/exoframes/client/HudLayoutTest.java`, in `spritesCoverTheHudTextureAndCarryTheLegend`:

- `:233`: every paint from `modelPaints()` (`:405`) goes into `figure`. It builds a helmet with a durability for
  each `PieceCondition` (searched 0..180, asserting every condition is reached), runs it through `HudModel.of` and
  takes `silhouette().values()`, so the unmounted paint comes from the other five slots.
- `:261-272`: for every `EnergyState` plus no indicators (`energyStates()`, `:420`), over a full blink (0..19 ticks),
  `HudModel.of(...).energyColor()` goes into `barFill`, and into `warning` whenever `warningOn()` is true. It asserts
  that the warning was lit for exactly LOW and EMPTY.
- The pickers' signatures are unchanged.

### B4. Python layout and Java layout tied by a test

- `tools/art/gui_layout.py:113` `union`.
- `tools/art/gui_layout.py:124` `BACKGROUND`: the 20 widget rectangles the background bakes around.
  - The four panels, the six place sockets and the cell's, and the bar recess.
  - `tavolozza` and `predefinito` (the tray around them), and `anteprima.ritaglio`.
  - `inventario` and `barra`, then `preset.0` and `preset.1` (the grooves under them) and `riga.costi`.
- `tools/art/gui_officina.py:110`: `background()` reads them from that table. The tray (`:132`) and the preset
  grooves (`:148`) are now derived from it instead of the literals `(80, 87, 208, 111)` and `166`/`184` from 177 to
  301. The three textures stay pixel-identical (`compare_png.py`, and `OK art`).
- `tools/art/gui.py:167` `gui_map()`, and `--map-json FILE` (`:194`, `:217`):
  - it writes, with `exo.dumps_json`, the size and the sprites of `station.png` and `hud.png`, the `sfondo` rectangles
    and the `sagoma` pieces;
  - it writes only after the checks pass;
  - `--out` or `--map-json` is required, and gui_preview's `--out --kit` call is unchanged.
- `tools/art/gui_map.json` (new, under `tools/art/`, never in a jar). `tools/check_art.sh:102-104, :135-141`
  regenerates it outside `$tmp/gui` and compares it with `cmp`. Checked: a hand-edited map gives `FAIL:
  tools/art/gui_map.json diverso da quello generato (rigenerare con python3 tools/art/gui.py --out
  src/main/resources/assets/redstone_exoframes --map-json tools/art/gui_map.json)`.
- `src/test/java/com/toxineren/exoframes/client/GuiMapTest.java`, new, reads the file as
  `SuitScaleTest.matchesTheArtGenerator` does (walking up from `user.dir`):
  - `spriteNamesAndPlacesMatchTheArtGenerator` (`:45`): texture size, and set equality in both directions of the map's
    names (upper case, `.` → `_`) with the `Sprite` constants (70 and 16), then each rectangle;
  - `backgroundIsBakedWhereTheStationLayoutPutsTheWidgets` (`:54`): an explicit name → `StationLayout.Rect` map, plus
    `BACKGROUND_ONLY` (`:41`) = `pannello.preset`, `tavolozza`, `predefinito`, `preset.0`, `preset.1` and
    `riga.costi`, the 4b widgets. Together they must equal the exported names, then each matched rectangle is
    compared;
  - `silhouettePiecesAreWhereTheHudLayoutCutsThem` (`:71`): `sagoma` against `HudLayout.piece`.
- Docs: `tools/art/README.md:479-483, :503-513`, `tools/README.md:14`, `README.md:28`, and `docs/compat.md:242`
  (StationLayout entry, one clause on `GuiMapTest`).

### B5. Test oracles in `HudModel`

`src/main/java/com/toxineren/exoframes/client/HudModel.java`:

- `:181` `lighter`: drawing code does not use it. It is the rule
  `HudLayoutTest.spritesCoverTheHudTextureAndCarryTheLegend` (energy bar) and
  `StationLayoutTest.spritesCoverTheScreenTextureAndCarryTheLegend` (durability bar) test the committed textures
  against.
- `:242-246` `Paint`: SuitHud picks the figure by `fill` alone. The `outline` component is not read by drawing code;
  it is the rule `HudLayoutTest.spritesCoverTheHudTextureAndCarryTheLegend` tests the figures' outlines against.
- Neither member was removed.

### C1. `docs/compat.md`

- `:135`, `StationBlock`: instead of parent `orientable` and textures from `gen_placeholders.py`, it now says that
  since part 4d the two models (own elements, no parent), their seven `textures/block/station_*.png` and the item
  icon are written only by `tools/art/station.py --direction officina`. It also says that `check_art.sh`
  regenerates and compares them and refuses any station model or texture the generator does not write, and that
  the blockstate and item model are hand-written.
- The HUD entries (`:212-215` `addHudRenderer`, `:248` SuitHud/HudModel/HudLayout), reread: no sentence describes
  the 4c flat drawing; they already name `hud.png`, `hud_vignette.png` and `GuiTexture.blit`.

### C2. Design §14, "Per la 4d"

`docs/superpowers/specs/2026-11-19-m4-stazione-design.md:645-648`: each item keeps its text and gains one clause.

- 645: done, `silhouetteAndWarningRectsAreFixed`, and `TWO_DIGIT_SPARE` = 18 measured in game ("×10" on
  1.21.1-neoforge, icon at 41, text at 47).
- 646: done, the body pinned in `screenRectsAtNineSixtyMatchTheSequence`; the `barra-esaurita` box is checked
  exactly in game by `m4c_shots.sh` (`WARNING_PERCENT_RIGHT`), not by a JUnit.
- 647: done, `TEXT_BOX_RIGHT` of `m4c_shots.sh`, in the `testo` check.
- 648: no longer applies. Both constants are gone because code no longer draws that red: it lives in the textures'
  drawings (in the station `CELL_RED` of `gui_layout.py`, in the HUD the cell icon).

### C3. Plan decision citations

Replaced with the reason or the design section:

- `HudModel.java:23` (one legend, design §5.3 and §5.12), `:110-111` (with little charge the bar can stay empty; the
  colour gives the state) and `:227` (the warning keeps its place, so the percentage does not jump).
- `HudKeys.java:20` (the preference is the player's, not the suit's).
- `HudLayout.java:57-58` and `:68-69` (from the text row of design §5.12 and the game font) and `:77-81` (design
  §5.12, "percentuale" row).
- `SuitHud.java:52`, `:60` (nothing mirrored, each corner its own drawing) and `:134` ("celle di scorta" row; the sum
  stays right if cells ever stack).
- `HudLayoutTest.java:48` (game font, as `Font.width`), `:174` and `:444` (design §12 and `anteprime.py`).
- `HudModelTest.java:84` and `:131`.

No plan, ledger or review ID was added; `grep -i 'piano\|decision'` over the six files is now empty.

### Covering tests and checks

- `./gradlew --console=plain :1.21.1-neoforge:test --tests '…GuiMapTest' --tests '…HudLayoutTest' --tests
  '…HudModelTest' --tests '…StationLayoutTest'`: `BUILD SUCCESSFUL`; 3, 11, 10 and 8 tests, 0 failures (result XMLs).
- Mutations, one run, all restored and verified afterwards. Each mutation fails its intended test:
  - `TUTTI_SOPRA` and `TUTTI_PREMUTO` swapped in `StationLayout` → `spriteNamesAndPlacesMatchTheArtGenerator`:
    "StationLayout.TUTTI_SOPRA ==> expected (318,18) but was (318,34)";
  - `pezzo.helmet` moved in the map → `backgroundIsBaked…`: "pezzo.helmet: the background bakes it elsewhere";
  - the silhouette helmet moved → `silhouettePieces…`: "helmet: the figures draw it elsewhere";
  - `HudModel.energyColor` LOW → YELLOW → `HudLayoutTest.spritesCover…`: "no bar fill for ffffff55".
- A second mutation run adds an unclassified `moduli.riservato` to `sfondo` → "what the background bakes, in
  tools/art/gui_map.json".
- `python3 tools/art/selftest.py`: `OK selftest: 31 difetti su 31 segnalati, premesse valide` (was 25 cases).

### JUnit count per variant

`1.20.1-forge 389`, `1.20.1-fabric 389`, `1.21.1-fabric 389` and `1.21.1-neoforge 389` (386 before), with 0
failures, errors or skipped, from the result XMLs of the final build, in which all four `:test` tasks executed.

- New: `GuiMapTest.spriteNamesAndPlacesMatchTheArtGenerator`,
  `backgroundIsBakedWhereTheStationLayoutPutsTheWidgets` and `silhouettePiecesAreWhereTheHudLayoutCutsThem`.
- Changed: `HudLayoutTest.spritesCoverTheHudTextureAndCarryTheLegend` (model-driven); comments only in
  `HudLayoutTest` and `HudModelTest`.

### End-of-task outputs (in order, on `f5b3ac8`, clean tree)

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
  :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` →
  `BUILD SUCCESSFUL in 30s`. The four `:test` tasks executed (not UP-TO-DATE).
- JUnit (the brief's command): 389 on each variant, as above.
- `bash tools/check_sources.sh` → `OK sources`
- `bash tools/check_jars.sh` → `OK jars`
- `bash tools/check_data.sh` → `OK data`
- `bash tools/check_art.sh` → `OK art`
- `python3 tools/art/selftest.py` → 31 `ok` lines, `OK selftest: 31 difetti su 31 segnalati, premesse valide`
- `git status --short` → empty.

### Concerns

1. `tools/client/m4c_shots.sh:16`, `:63` and `:160` still cite "Decisione 6/10 del piano" and the plan's layout
   table. The brief's C3 list does not name that script, so it is unchanged.
2. The duration ranges in the 3c and 3d script comments were left as written. This run was 110-141 s for m3c, 74-84 s
   for m3c2, 82-96 s for m3c_rules and 190-214 s for m3d; some fall just outside the stated ranges. They depend on
   load, not on the art, and the comments say "circa".
3. The three 3c-era shots (`vasca-spazio`, `barre-inventario`, `cambio-cella-tenuto`) still show the 3c energy line
   and the bare arm. This is unchanged from 4c, and the 4c note in `m3c-giocarci.md` covers it.
4. `SuitHud`'s class Javadoc keeps a pre-existing "(DEC-23)". It is outside C3, and I added no ID.
5. `GuiMapTest` has its own copy of `SuitScaleTest`'s ten-line `findInRepository`, so that test stays untouched.
6. `gui_officina.background()` still hardcodes one decorative line, on purpose: the groove between the pieces and
   the cell in the frame panel (`hline(93, 10, 70)` and the row under it, `tools/art/gui_officina.py:129`). It sits
   around no widget rectangle, so it stays out of `gui_layout.BACKGROUND` and of `gui_map.json`; "e solo questi" in
   those comments is about widget rectangles.
