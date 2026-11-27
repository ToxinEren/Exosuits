# Report — part 4d, rest of step (1): screen, HUD, kit icon and vignette in direction A's style

Status: DONE_WITH_CONCERNS. Commit `74a890c` on `m4-stazione` (signed by the environment, like its parent; not pushed).
Note: the branch was at `3c7a0d5`, not `e90e46d`, when I committed: a docs-only commit landed while I worked (the
owner's answers of 27 November: questions 8 and 9 confirmed with the provisional values, 10 still open). Nothing I
draw changes with it; my commit sits on top.

## What each generator writes

`python3 tools/art/gui.py --out <dir>` (`--out` mandatory, never writes into `src/main`; `--map` prints every sprite's
place). Modules: `tools/art/gui_layout.py` (design geometry from layout.py / StationLayout / HudLayout, the two
atlases, legend, dyes), `tools/art/gui_officina.py` (the drawings, built with `pixel.Canvas`/`RAMPS`/`sprite`),
`tools/art/gui.py` (entry + checks). numpy only, `exo.write_png`, integer arithmetic, seeds from canvas names; two runs
give identical bytes (checked).

- `textures/gui/station.png`, 512x256. `fondo` 316x232 at (0,0), fully opaque. Widgets beside it (x 318-511) and in
  the strip below (y 234-251); everything else alpha 0. Buttons in four states each (normale, sopra, premuto, spento):
  `tutti` 30x16, `ripara` 44x16, `ripara_tutto` 59x16, `applica` 18x16 (brush), `salva` 18x16 (book), `regione`
  62x12, `smonta` 12x12 (eject arrow), `blocco.chiuso` / `blocco.aperto` 16x12. `rinforzato` 14x12. `posto.integro`,
  `.danneggiato`, `.critico`, `.rotto`, `.vuoto`, `.cella` 20x20; `scelta` 22x22. Ghost icons 16x16 for empty places:
  `fantasma.elmo`, `.torso`, `.braccio`, `.gamba`, `.cella`. `materiale` and `materiale.spento` 18x18, plus
  `materiale.velo` 16x16 (dims the icon). `freccia.sinistra` / `.destra` 5x7, normal and `.sopra`. `tinta` 12x10
  (bezel, transparent window), `tinta.spenta` 12x10 (veil), `tinta.sopra` 14x12 (ring). `predefinito` 14x22 (two
  windows) and `predefinito.sopra` 16x24. `preset.colori` / `preset.vuoto` 74x10. `durata.fondo` and
  `durata.integro`, `.danneggiato`, `.critico`, `.rotto`, `.cella`, all 126x5.
- `textures/gui/hud.png`, 256x128 (the name for the report): `piastra` 88x35 and `piastra.barra` 67x35; five
  silhouette figures 21x35 (`sagoma.integro`, `.danneggiato`, `.critico`, `.rotto`, `.assente`). Each piece is cut
  from its state's figure with its subgroup rectangle (`gui_layout.silhouette_piece`). Also `barra.cornice` 63x8;
  fills `barra.verde`, `.rossa`, `.grigia`, `.bianca` 61x6; `avviso.rossa` and `avviso.grigia` 8x8; `cella` and
  `cella.spenta` 5x8.
- `textures/gui/hud_vignette.png`, 144x80: the four 72x40 corners where `SuitHud.drawVignette` takes them, so
  integration is a file swap.
- `textures/item/repair_kit.png`, 16x16 flat icon.

Checks inside `gui.py`: it exits 1 and writes nothing when one fails.
- Atlas: every sprite is inside its texture, no two overlap, alpha is 0 outside sprites, the background is opaque.
- Exact legend:
  - place borders, including CELL_RED and 464646;
  - durability fills, with a lighter first row;
  - silhouette pieces, filled with the colour and an outline at a third, nothing else;
  - unmounted pieces, fill 5A000000 and outline 464646;
  - energy fills, with `lighter` on the first row;
  - warnings.
- No texel is "magenta" by arm_measure's rule, in all four files.
- Vignette: 107 (0.42) at the four corner pixels and never above; 0 on both opposite sides of every corner;
  reflections only in the top corners.

I injected nine defects in memory: all nine were reported.

Sprite map (`python3 tools/art/gui.py --map`; u v w h), for integration:

```
textures/gui/station.png (512x256): nome u v larghezza altezza
  fondo                           0    0  316  232
  tutti.normale                 318    2   30   16
  tutti.sopra                   318   18   30   16
  tutti.premuto                 318   34   30   16
  tutti.spento                  318   50   30   16
  ripara.normale                350    2   44   16
  ripara.sopra                  350   18   44   16
  ripara.premuto                350   34   44   16
  ripara.spento                 350   50   44   16
  ripara_tutto.normale          396    2   59   16
  ripara_tutto.sopra            396   18   59   16
  ripara_tutto.premuto          396   34   59   16
  ripara_tutto.spento           396   50   59   16
  applica.normale               457    2   18   16
  applica.sopra                 457   18   18   16
  applica.premuto               457   34   18   16
  applica.spento                457   50   18   16
  salva.normale                 477    2   18   16
  salva.sopra                   477   18   18   16
  salva.premuto                 477   34   18   16
  salva.spento                  477   50   18   16
  rinforzato                    497    2   14   12
  regione.normale               318   68   62   12
  regione.sopra                 318   80   62   12
  regione.premuto               318   92   62   12
  regione.spento                318  104   62   12
  smonta.normale                382   68   12   12
  smonta.sopra                  382   80   12   12
  smonta.premuto                382   92   12   12
  smonta.spento                 382  104   12   12
  blocco.chiuso.normale         396   68   16   12
  blocco.chiuso.sopra           396   80   16   12
  blocco.chiuso.premuto         396   92   16   12
  blocco.chiuso.spento          396  104   16   12
  blocco.aperto.normale         414   68   16   12
  blocco.aperto.sopra           414   80   16   12
  blocco.aperto.premuto         414   92   16   12
  blocco.aperto.spento          414  104   16   12
  materiale                     432   68   18   18
  materiale.spento              432   88   18   18
  materiale.velo                452   68   16   16
  freccia.sinistra              470   68    5    7
  freccia.destra                477   68    5    7
  freccia.sinistra.sopra        470   77    5    7
  freccia.destra.sopra          477   77    5    7
  scelta                        486   68   22   22
  tinta.sopra                   486   92   14   12
  posto.integro                 318  118   20   20
  posto.danneggiato             338  118   20   20
  posto.critico                 358  118   20   20
  posto.rotto                   378  118   20   20
  posto.vuoto                   398  118   20   20
  posto.cella                   418  118   20   20
  fantasma.elmo                 440  118   16   16
  fantasma.torso                456  118   16   16
  fantasma.braccio              472  118   16   16
  fantasma.gamba                488  118   16   16
  fantasma.cella                456  134   16   16
  tinta                         318  140   12   10
  tinta.spenta                  332  140   12   10
  predefinito                   346  140   14   22
  predefinito.sopra             362  140   16   24
  preset.colori                 380  140   74   10
  preset.vuoto                  380  152   74   10
  durata.fondo                    2  234  126    5
  durata.integro                  2  240  126    5
  durata.danneggiato              2  246  126    5
  durata.critico                130  234  126    5
  durata.rotto                  130  240  126    5
  durata.cella                  130  246  126    5
textures/gui/hud.png (256x128): nome u v larghezza altezza
  piastra                         0    0   88   35
  piastra.barra                   0   36   67   35
  sagoma.integro                 90    0   21   35
  sagoma.danneggiato            112    0   21   35
  sagoma.critico                134    0   21   35
  sagoma.rotto                  156    0   21   35
  sagoma.assente                178    0   21   35
  barra.cornice                  69   37   63    8
  barra.verde                    69   46   61    6
  barra.rossa                    69   53   61    6
  barra.grigia                   69   60   61    6
  barra.bianca                   69   67   61    6
  avviso.rossa                  134   37    8    8
  avviso.grigia                 143   37    8    8
  cella                         152   37    5    8
  cella.spenta                  158   37    5    8
pezzi della sagoma in textures/gui/hud.png (stato.pezzo u v larghezza altezza):
  integro.helmet                 98    3    6    6
  integro.torso                  97   10    8   11
  integro.left_arm               93   10    3   11
  integro.right_arm             106   10    3   11
  integro.left_leg               97   22    3   11
  integro.right_leg             102   22    3   11
  danneggiato.helmet            120    3    6    6
  danneggiato.torso             119   10    8   11
  danneggiato.left_arm          115   10    3   11
  danneggiato.right_arm         128   10    3   11
  danneggiato.left_leg          119   22    3   11
  danneggiato.right_leg         124   22    3   11
  critico.helmet                142    3    6    6
  critico.torso                 141   10    8   11
  critico.left_arm              137   10    3   11
  critico.right_arm             150   10    3   11
  critico.left_leg              141   22    3   11
  critico.right_leg             146   22    3   11
  rotto.helmet                  164    3    6    6
  rotto.torso                   163   10    8   11
  rotto.left_arm                159   10    3   11
  rotto.right_arm               172   10    3   11
  rotto.left_leg                163   22    3   11
  rotto.right_leg               168   22    3   11
  assente.helmet                186    3    6    6
  assente.torso                 185   10    8   11
  assente.left_arm              181   10    3   11
  assente.right_arm             194   10    3   11
  assente.left_leg              185   22    3   11
  assente.right_leg             190   22    3   11
```

Integration notes:
- Swatches and `predefinito`: fill the colour first, then blit the bezel on top. The bezel's window is transparent,
  apart from a translucent highlight row and shade row.
- Preset strips: blit at (colori.x0 - 1, colori.y0 - 1), then fill the twelve 5x8 cells.
- The durability bar and HUD fills are cropped to the filled width.
- Cell icon: `cella.spenta` when N = 0 (my choice).

## Choices where the design was open (A: dark riveted steel, bronze, copper, redstone red as paint)

Screen:
- Background: brushed dark steel plate, black outline, 2-px bronze rim (light top-left, dark bottom-right), bronze
  rivets in the four corners.
  - Panels are inset: darker interior, black shadow top-left, steel lip bottom-right.
  - Baked in because they never change: the 36 player slots, dark sockets round the six places and the cell, the
    groove between pieces and cell, the recess round the durability bar, the palette tray, preset row grooves, the
    cost-row display.
  - The 3D preview's frame is baked too: three-band backdrop, a bronze turntable under the suit's feet (feet at
    clip bottom - 6), bronze corner brackets.
  - Modules area left blank (D1). The title and label areas are plain plate, so the E6E6E6 text reads.
- One button family for all buttons:
  - normale: brushed steel face with relief;
  - sopra: lighter face and copper outline;
  - premuto: sunken bronze face;
  - spento: dark flat face;
  - chamfered corners (corner texel transparent).
  The design gives: regions premuto (active) and spento (legs); Ripara/Tutto spento; lock spento; applica spento
  (empty preset); salva spento (read-only). Every other state is added for uniformity. Examples: Tutti premuto
  (proposed for "all selected"), lock premuto.
- Icons:
  - smonta: eject arrow;
  - applica: brush with a redstone-red tip;
  - salva: bronze book with a copper label and red ribbon;
  - lock: brass padlock; open = shackle raised one pixel, right leg out of the body;
  - shield: bronze heater shield with a ridge.
- Places: 1-px border in the exact colour round an inset slot (darker when empty); `scelta` stays white (design).
  Ghost icons: my own silhouettes in light steel at 25 % alpha.
- Material: bronze-rimmed slot; spento: steel rim and darker interior, plus a 59 % veil to dim the icon.
- Swatches: bezel, hatched "spenta" veil and copper hover ring. The 16 colours stay code fills from the dye table
  (also keeps magenta/purple out of the texture). Predefinito: two code-filled windows in the same bezel.
- Durability bar: dark channel with quarter ticks. Fill in the exact condition colour with a lighter first row, like
  the HUD bar (station's bar is flat today).
- Preview arrows: bronze 5x7 triangles at the measured place of today's "<" ">" glyphs (219,122 and 300,122).

HUD (questions 8-10 at their provisional values; 8 and 9 are now confirmed):
- Plate: darkest steel (20,22,26) at 60 %, the design's opacity. Copper edge with relief at 71 %; the design has
  6EB87333, i.e. 43 %. Four bronze rivets inside the corners, outside every element for both hands and both levels.
- Silhouette: legend fill and third outline exactly; the helmet's top corners are chamfered and the torso narrows at
  the waist. Arms and legs are the plain design rects. No inner lines, so each piece stays "pieno".
- Warning: today's stepped triangle in exact red and grey, with the "!" opaque dark steel instead of transparent.
  Only red and grey exist: warnings only show for LOW and EMPTY.
- Cell icon 5x8: steel cap, copper body, red charge; a dim variant for x0.
- Bar frame: inset steel channel (black top-left, light lip bottom-right). The back is gun 2 (41,45,51), close to
  282A2E, with quarter ticks. The design's frame was one flat 0C0D0F.

Vignette:
- Opacity 107*(1-d)^2 with d normalised on 71 and 39, so it is 0 exactly on the opposite sides.
- Colour runs from the dark steel of the frame edge (near the corner) to the dark teal of the helmet glass.
- Two stepped diagonal reflections in each top corner (slope 1/2), in the glass highlight (190,230,230) and the same
  pushed towards white (236,250,250), alpha 42 and 58. They replace the darkening. In my first pass I composited them
  over it, and they read as dark scratches on the daylight sky, so I fixed that.

Kit icon: a toolbox in the same three-quarter projection and outline as the station icon. Bronze lid, steel body with
a side face, steel handle, two copper latches, a band of redstone-red paint; no cross.

## How the previews are composed

`python3 -I tools/art/preview/gui_preview.py --vanilla <dir> [--assets <dir>] [--out <dir>]`:
- It runs `gui.py` into the ignored `tools/art/preview/out/gui/` when `--assets` is absent.
- Every drawing is cropped from the generated PNGs with the `gui_layout` atlas. It is placed in the design rectangle
  at GUI scale 1, upscaled x2 without filtering, and composed with PIL.
- Code-drawn parts are drawn as the code would: dye, tier and preset colours, text in the vanilla font (font and
  seven vanilla item icons read from a local extraction of the 1.21.1 client jar, outside the repo; mod icons from
  `src/main`).
- Written with `exo.write_png`; the script stops if a PNG has other chunks. Two runs give identical bytes.

Images in `docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/`:
- `schermata-960x540.png`: over the committed shot `m4a1-screenshot/1.21.1-neoforge-stazione-colori-suoi.png`, at the
  game's own place (leftPos 82, topPos 19). The suit in the 3D preview is that shot's clip, copied pixel for pixel
  with PREVIEW_BG and the "<" ">" pixels keyed out. Example content of §12: torso damaged and selected, left arm
  critical, left leg broken, cell, iron x12, four dyes owned, two presets and one empty, mouse on the red dye with the
  vanilla tooltip.
- `schermata-640x480.png`: the same at the minimum size, over `stazione-posata` darkened like vanilla (C0→D0101010),
  with the mouse on "Ripara".
- `schermata-texture.png`: station.png x3 on a checkerboard, ten numbered sprite groups with a legend.
- `hud-texture.png`: hud.png's used part, x6.
- HUD images:
  - `hud-destra-1-energia-piena.png`, `hud-destra-2-energia-bassa-pezzo-danneggiato.png`,
    `hud-destra-3-pezzo-rotto.png`, `hud-destra-4-senza-elmo.png`;
  - `hud-sinistra-1-energia-piena.png`, `hud-sinistra-2-energia-bassa-pezzo-danneggiato.png`,
    `hud-sinistra-3-pezzo-rotto.png`.
  They use the §12 states, drawn over the vignette-free m4c shots `1.21.1-neoforge-danneggiato.png` (right hand) and
  `1.21.1-neoforge-mancino-danneggiato.png` (left). The flat HUD drawn in those shots is first covered with the grass
  rows just above it, mirrored, because the plate is translucent. Each image has the group enlarged x3 in the sky.
- `vignetta.png`: the new vignette and the HUD over the same right-hand shot. Below it, three corners enlarged x2
  without the HUD, the placeholder (from the committed `src/main` texture, read only) above the new one.
- `kit-di-riparazione.png`: station icon and kit icon at x8, x2 and x1.

## check_art.sh

```
$ bash tools/check_art.sh
OK art
```
(Run before and after the commit; no existing generated file was touched. `check_art.sh` and `selftest.py` are
unchanged, following the station's precedent: `ef73434`, the preview commit, touched neither; `704cef7`, the
integration, added them. The new generator's invariants live in its own checks.)

## PNG chunk check (every new PNG)

```
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/hud-destra-1-energia-piena.png 960x540 IHDR IDAT IEND
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/hud-destra-2-energia-bassa-pezzo-danneggiato.png 960x540 IHDR IDAT IEND
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/hud-destra-3-pezzo-rotto.png 960x540 IHDR IDAT IEND
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/hud-destra-4-senza-elmo.png 960x540 IHDR IDAT IEND
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/hud-sinistra-1-energia-piena.png 960x540 IHDR IDAT IEND
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/hud-sinistra-2-energia-bassa-pezzo-danneggiato.png 960x540 IHDR IDAT IEND
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/hud-sinistra-3-pezzo-rotto.png 960x540 IHDR IDAT IEND
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/hud-texture.png 1224x600 IHDR IDAT IEND
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/kit-di-riparazione.png 596x230 IHDR IDAT IEND
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/schermata-640x480.png 640x480 IHDR IDAT IEND
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/schermata-960x540.png 960x540 IHDR IDAT IEND
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/schermata-texture.png 1560x1044 IHDR IDAT IEND
ok   docs/superpowers/specs/anteprime/2026-11-27-m4d-grafica/vignetta.png 984x956 IHDR IDAT IEND
ok   tools/art/preview/out/gui/textures/gui/hud.png 256x128 IHDR IDAT IEND
ok   tools/art/preview/out/gui/textures/gui/hud_vignette.png 144x80 IHDR IDAT IEND
ok   tools/art/preview/out/gui/textures/gui/station.png 512x256 IHDR IDAT IEND
ok   tools/art/preview/out/gui/textures/item/repair_kit.png 16x16 IHDR IDAT IEND
```
The 13 committed previews, plus the four generated textures (written to the ignored `out/` folder, not committed):
only IHDR, IDAT, IEND, CRCs valid.

## What I would improve with more passes

- The screen is dark and fairly uniform. A stronger A identity would come from more rivets along the rim and the
  4-px gaps between panels, maybe a bronze title plate and corner brackets on the panels.
- Small buttons: the brushing streaks are a little busy on the 12-px faces; tone them down.
- The preview backdrop's three flat bands could become a finer stepped gradient. The turntable could get rivets or
  segment marks.
- The ghost icons are faint. Tune alpha and shapes against the real item icons.
- Silhouette: explore joints without losing the "pieno" fill.
- Look at the vignette in night and cave scenes; only daylight shots were available. Look at the HUD and screen at GUI
  scales 1, 3 and 4 in the real client after integration.
- Kit icon: check it at x1 in a hotbar in-game. The red band may be too dominant; a wrench motif is an alternative.

## Concerns

1. Plate and bar frame depart from the design's exact non-legend ARGB values: edge at 71 % instead of 43 %, an inset
   bar frame instead of a flat 0C0D0F border. The 4c plan says the plate and bar "seguono il design al pixel". The
   owner should confirm. At integration, HudModel's PLATE_EDGE, BAR_EDGE, BAR_BACK and their tests change or go.
2. Magenta and purple dye colours (C74EBD, 8932B8) are "magenta" by arm_measure's rule. The textures have none,
   because colours are code fills. But once 4b draws the palette, the `anteprima` check of `m4a1_shots.sh` (screen
   magenta == clip magenta) will count them in any shot that shows the palette. That check needs to exclude the
   palette, or the shot needs to avoid them.
3. At full energy with every piece intact the HUD is mostly green: green silhouette, text and fill. That is the
   design's legend, kept exactly. The plate and frame are steel, copper and bronze, not a green terminal, but the
   owner should look at it with the anti-goal in mind.
4. The previews contain vanilla font glyphs and seven vanilla item icons, rendered from a local extraction. The §12
   previews and the in-game shots do the same; nothing vanilla is committed as a file.
5. Extra states beyond what the design names (listed above) are harmless, but integration should use only what it
   needs.
6. The positions of code-drawn text in the previews (button labels centred, cost-row text at +3,+1 and +3,+10,
   "vuoto" at +4) are my guesses for integration, not measured in the game.
7. The design's §14 "Per la 4d" notes stay with integration: the `HudLayoutTest` pins and the single constant for
   the cell red. That constant disappears, because the cell icon is now a sprite. The `122x10+52+510` bar-body pin
   still holds, because the fill keeps a distinct first row.
