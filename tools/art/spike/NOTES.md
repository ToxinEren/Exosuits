# Redstone Exoframes: Rottame (Scrap) art spike

Three procedurally generated directions for the first armor tier, each shown as the bare
exoframe and with all six Rottame pieces mounted. Throwaway spike, not for the mod repo.

Conventions (all directions): 1 unit = 1 texture px = 1/16 block, y up, model stands on
y = 0, **front faces -z (north)**. Bone pivots use the Bedrock/GeckoLib humanoid numbers
(right arm at -x: `armorRightArm` pivot (-5,22,0), `armorRightLeg` (-1.9,12,0), head/body
(0,24,0)). Every box belongs to `armor<Part>` -> `frame_<part>` (always visible) or
`piece_<part>` (the removable armor piece). Atlas: 128x128 per direction, one rectangle
per box face (mirrored left/right copies share the right side's rectangles with a u-flip,
as Minecraft's mirror flag does). Used height: A 53, B 41, C 57 rows, so there is room
to give left/right their own wear later.

Renders include a stand-in player (dark brown undersuit, tan head) so the frame is read as
something worn; it is not part of the model.

## Shared frame
Spine and back yoke, hip belt, two chest struts with a cross bar, redstone wire along the
yoke, power housing on the back with a glowing redstone cell, brass-hub joints at shoulder,
elbow and knee, shoulder brackets, outer arm struts, wrist bands and hand plates, front leg
struts, calf pistons (brass cylinder + steel rod), ankle bands and foot plates (soles that
stick out 1 px forward). The back housing changes per direction (A boxed, B open cage,
C low saddle under the boiler).

## Paint and decal regions (all directions)
`build/<D>/tintmask.png` is the authoritative map: **R = primary paint, G = secondary paint,
B = banner decal**. Chipped pixels (bare iron showing through) are excluded from the paint
masks, so a later dye only tints intact paint. In preview the primary is a faded ochre, the
secondary a slate teal-grey; the decal shows a placeholder (light field, darker border and
diagonal) that a banner pattern would replace.

## A - Bulkhead (armored)
Slab construction: a riveted 9x9x9 helmet with a single horizontal visor slit, a jaw grille
and a brow bar, sunk between two tall squared pauldrons (6x9x8, rising to y 30) that each
carry a bolted outer plate. Heavy painted gauntlets, painted thigh slabs, copper knee cops,
wide boots on the frame's foot plates; the back is a vented torso core with the boxed cell
housing sticking out. Original points: the shoulders are taller than the helmet sides so
the head reads "inside" the suit, and the face is a flat bulkhead with a slit rather than
any eye layout.
- Size: 24 x 34 x 13 px (x -12..12, y 0..34, z -6..7).
- Decals: chest plate front, 7x7 (x -3.5..3.5, y 16.5..23.5, z = -6, north face of
  `chest_decal`); left shoulder, 7x7 on the east face of `shoulder_decal` (x = 12,
  y 22..29, z -3.5..3.5).
- Primary paint: chest plate, both pauldrons, thighs. Secondary: gauntlets, helmet cheeks.
- With more time: break the pauldron slab into two stepped plates, give the helmet a
  hinge/hatch detail on the sides, less uniform boots.

## B - Scavenger (skeletal)
The frame is the costume: smaller, mismatched plates bolted to an exposed skeleton with
extra arm pistons, an open cage around the glowing cell wrapped in copper coils and a
status lamp. Strongly asymmetric: a welded half-mask with one lens on the left and bare
cage bars on the right (the wearer's face shows through), a stepped rusty pauldron and
copper cuff on the right arm vs a tall painted pauldron on the left, different boots,
rib bars on the left chest, a tilted belly piston. Lightest silhouette of the three.
- Size: 20.5 x 35 x 13 px (x -10.5..10, y 0..35 incl. antenna, z -6.5..6.5).
- Decals: chest, 6x6 north face of `chest_decal` (x -4..2, y 17..23, z = -5) on the
  salvaged right-chest plate; left shoulder, 7x7 east face of `shoulder_decal`
  (x = 10, y 20.5..27.5, z -3.5..3.5).
- Primary paint: right-chest plate. Secondary: left pauldron. (Little paint by design.)
- With more time: the read is cluttered at a distance; fewer, bolder plates and a clearer
  head shape; the single lens is generic and could be replaced by something more unusual.

## C - Boiler Back (hunchback boiler)
A riveted copper boiler (12x16x5 with iron bands, a chimney stub, a valve and a side
pressure gauge) climbs from the waist to above the head, so the small domed helmet with
its square porthole and rebreather sits low in front of it. Oversized painted forearms
with copper knuckle plates and glowing vent grilles, copper pipes running from the boiler
over the shoulders and down the back of the arms, shoulders as sloped slabs (two of the
few rotated boxes) with hanging flaps, a glowing firebox grate on the back, short heavy
legs with hose rings on the boots.
- Size: 24 x 35 x 15 px (x -12..12, y 0..35, z -6..9). Depth is 1 px over the 14 target
  because of the foot-plate toes; the boiler itself is z 3..8.
- Decals: chest, 7x7 north face of `chest_decal` (x -3.5..3.5, y 16.5..23.5, z = -5);
  left shoulder, 7x7 east face of the hanging flap `shoulder_decal` (x = 11, y 18..25).
- Primary paint: chest plate, sloped pauldrons. Secondary: forearms.
- With more time: make the hunch read from the front too (boiler shoulders wider than the
  helmet), sort out the rotated pauldrons (they intersect the flaps), animate a smoke puff.

## Pipeline
Files (all in this directory):
- `exo.py` - model description (bones -> frame/piece groups -> boxes with min/max corner,
  optional pivot+rotation), `sym()` for mirrored pairs, face-rectangle packer, coplanar
  face (z-fighting) checker, exports `model.json` and Bedrock `.geo.json`.
- `paint.py` - procedural painter. Each texel knows its 3D position so value noise, rust
  and patina are continuous across box edges. Materials: iron (3 tints), frame steel,
  copper with patina, brass, chrome rods, rubber/copper ribbed pipes, chipped paint
  (primary/secondary), decal placeholder, redstone/wire/cell (emissive), visor, grilles
  (optionally glowing), gauge, porthole, lamp. Writes `atlas.png`, `glow.png` (emissive)
  and `tintmask.png`.
- `designs.py` - the shared frame and directions A, B, C.
- `build.py` - builds, checks (prints visible coplanar faces) and exports to `build/<D>/`.
- `viewer/index.html` - three.js page: builds the boxes from `model.json`, NearestFilter,
  no mipmaps, ambient + key + rim light, orthographic camera fitted to the full suit (frame
  views share the full-suit scale). URL params `model`, `view`, `mannequin=0`.
- `render.js` - tiny Node HTTP server + Playwright Chromium (swiftshader flags),
  1000x1000 CSS px at deviceScaleFactor 2 -> `out/<D>_<view>.png`.
- `sheets.py` - contact sheets `out/<D>_sheet.png` and `out/overview.png`.

Regenerate everything: `./make.sh` (or `./make.sh A` for one direction).

What worked: WebGL in headless Chromium worked first try with the swiftshader flags
(about 1 s per view), so no software-rasterizer fallback was needed. The checker caught
most z-fighting; coplanar patches buried inside other boxes or the player are ignored.
Downward faces are not checked (never seen in these views).

What did not / caveats:
- `.geo.json` export exists (`build/<D>/exoframe_<d>.geo.json`, format 1.12.0, per-face
  UV, mirrored faces as negative `uv_size`) but was **not loaded in Blockbench or GeckoLib**.
  Per-face UV orientation (especially `up`/`down`) and the sign of the few rotations may
  need flipping on import.
- Remaining checker warnings are frame-vs-piece faces hidden in normal views (e.g. wrist
  band vs belt) - acceptable for the spike, but they would flicker in game from some angles.
- Arm and body pieces overlap by 0.5-1 px like vanilla armor; arm swing will clip a bit.
- Rotated boxes are not checked for intersections.
