# Part 4d, step (1): three art directions for the station block — report

Status: **DONE_WITH_CONCERNS** (concerns at the end; none blocks the owner's choice).

Commit: `ef7343452b62d0cc9c22c4e0586747e5c21998bd` on `m4-stazione` (one signed commit over `ef53e6f`), message `tools, docs: generatore della stazione in tre direzioni e anteprime per il proprietario (parte 4d)`; not pushed.

## The three directions

- **A, Officina** — the mock-up the owner saw, refined and more metallic: dark riveted steel body recessed between a bronze worktop and a plinth, a recessed redstone screen (paint only), three copper knobs and a lever on the main half, two drawers with copper handles on the side half, a copper pipe on the back and on the ends.
- **B, Rame** — the recipe made into a bench (`CCC` / `ITI` / `IrI`): three riveted copper plates on top, light iron posts at the ends, the crafting table's oak core in the middle (an iron plate with a 3×3 grid on the main half, a plank door on the side half), a channel of redstone dust at the bottom between the iron feet.
- **C, Pistoni** — a machine-bench from the suit's world: bolted steel with a tread-plate top, two exposed hydraulic pistons (cylinders, chrome rods, brass glands) on the main half, the redstone cell behind three guard bars on the side half (like the cell housing on the suit's back), copper pipes along the plinth and on the back.

All three: a low 2×1 bench of two full cubes the frame stands on; nothing wraps the suit; no yellow-and-black; no light (red texels are paint). Each reads differently at comparison size: A dark steel + bronze, B bright copper + oak + light iron, C mid-grey steel + red + copper.

## Files

New:
- `tools/art/station.py` — CLI: `--list`; `--direction <officina|rame|pistoni> --out <dir>` (out is required; nothing integrated now). Writes `models/block/station_main.json`, `station_side.json`, `textures/block/station_{top,side,front_main,front_side,back_main,back_side,bottom}.png`, `textures/item/station.png`. The four existing texture names and `item/station.png` are reused; three are new (`station_back_main`, `station_back_side`, `station_bottom`). Blockstate and `models/item/station.json` stay valid unchanged.
- `tools/art/bench.py` — the two halves as one 32×16×16 voxel bench; designs `cut` pockets / `fill` raised parts from a face; exposed voxel faces become flat single-face elements merged into rectangles (no coplanar overlap by construction); projection UVs (same as vanilla defaults) on floors/outer faces, "folded" neighbour texels on pocket walls (UV `rotation` 90 where the axes need it); `cullface` only on block-boundary faces, seam faces drawn without; top and ends symmetric (side half uses `station_top`/`station_side` mirrored). `check()` refuses pockets that open toward another outer face or join a pocket of another face (x-ray holes, since the block is an occluding full cube); UVs asserted inside 0..16 and aligned to FaceInfo; textures asserted opaque.
- `tools/art/pixel.py` — palette canvases (material + ramp level per texel; integer only), noise/brush via `paint.HashRng`, rivets, bevels, `relief` (light/shadow from the depth map).
- `tools/art/station_designs/{__init__,officina,rame,pistoni}.py` — one direction per file.
- `tools/art/preview/blockmodel.js` — vanilla block-model reader mirroring FaceBakery (FaceInfo vertex order, BlockFaceUV.getU/getV with rotation, BlockElement.uvsByFace defaults, blockstate y rotation of positions, direction and cullface) plus world face lighting.
- `tools/art/preview/station.js` — runs the generator per direction into `tools/art/preview/out/stazione/gen/<name>` (ignored by git), renders the views with `scene.html`, composes the labelled sheets in Chromium, checks PNG chunks; `--check 1 --vanilla <dir>` renders today's placeholder for the ground-truth comparison.
- `docs/superpowers/specs/anteprime/2026-11-27-m4d/`: `confronto.png`, `a-officina-mondo.png`, `a-officina-texture.png`, `b-rame-mondo.png`, `b-rame-texture.png`, `c-pistoni-mondo.png`, `c-pistoni-texture.png`.

Modified:
- `tools/art/preview/scene.html` — new optional cfg keys: `blocks` (blockstate + variant + block position), `assets`, `vanilla`, `dy` for models, `antialias`, `background`, `floor.color`. Defaults unchanged; `node shot.js --views fronte` still renders.
- `tools/art/README.md` — section "Stazione di manutenzione (parte 4d)": commands, structure, in-game lighting note, preview commands and the ground-truth check.
- `docs/arte/README.md` — section "Stazione: tre direzioni (anteprime del 27 novembre 2026)" naming the images and each direction in one sentence; a line in "Dove sta cosa".

Untouched: `src/main/resources/`, the suit's generator outputs, `tools/gen_placeholders.py`, `tools/check_art.sh`.

## How the previews are rendered, and why

Rendered by the existing `tools/art/preview/` pipeline (three.js in headless Chromium via Playwright), extended with a vanilla block-model reader. Cheapest faithful option because:
- it already draws the **real suit** with GeckoLib's math (verified against GeckoLib's sample armour), so the frame on top is the real model at the real scale (suit 1.65, 1 block = 16/0.9375 scene px), with no stand-in;
- the block side is a small reader of the very JSON + PNG files the generator writes, implementing FaceBakery's rules; the blocks are placed through the mod's own `blockstates/station.json` (facing=north main at (0,0,0), side half to the west = right of whoever looks at the front);
- lighting is the game's face lighting, not three.js lighting: texel sRGB × face shade (up 1.0, N/S 0.8, E/W 0.6, down 0.5) × light of the neighbour block in the face's direction, with no colour management, nearest-neighbour texels, no antialiasing, no smooth lighting. The neighbour-light rule comes from `ModelBlockRenderer.calculateShape` (javap on the 1.20.1 client): for a block with a full collision shape, bit 0 is set even for inset quads, so the game takes their light from the block in the face direction;
- **ground truth**: today's placeholder station (mod blockstate + models + vanilla `orientable`/`cube` parents extracted from the cached client jar into a temporary folder, not committed) rendered through the same reader matches `docs/test-in-gioco/m4a1-screenshot/1.20.1-forge-stazione-posata.png`: red lamp on the left of the main front, grille on the side half to the right, top guides running left-right, light edges of the top at front/right and of the front at top/left. That shot covers only two faces (front and top) of a full cube with default UVs; everything else the generator and the reader rely on was read with javap and is byte-identical in the 1.20.1 and 1.21.1 jars: `FaceInfo` vertex order in all six directions, `BlockFaceUV.getU/getV` with the rotation-shifted index, `BlockElement.uvsByFace` defaults, and the blockstate rotation sign (`BlockModelRotation`: `rotateYXZ(-y, -x, 0)`, so y 90 turns north to east); the inset-face light rule (`calculateShape` bit 0 + `renderModelFaceFlat`) is the same in both versions;
- the real client would need the assets inside a jar: either touching `src/main/resources` (forbidden now) or resource-pack runs with Gradle + client per direction (minutes each on 4 cores) for no visible gain at preview size; a pure Python renderer would have to re-implement the suit's GeckoLib math that `geo.js` already has.

The frame stands where the game puts it when its item clicks the top of the main half (`ExoframeItem`: `Vec3.atBottomCenterOf` of the block above, yaw facing the player, so facing north like the station front); Rottame pieces, state 0, default colours, no pilot (a placed frame is empty), no glow layer (not in the game yet). The suit keeps the existing preview Lambert lights.

## Verification

- `bash tools/check_art.sh` → `OK art` (existing generated files untouched; run again after the last edits, before the final commit).
- Determinism: the generator run twice per direction into two folders: `diff -r` identical (30 files); the same bytes as the files the previews were drawn from.
- PNG chunk check (all new PNGs), every file only IHDR, IDAT, IEND (previews have several IDAT, no other chunk):
  - committed previews (7): `a-officina-mondo.png`, `a-officina-texture.png`, `b-rame-mondo.png`, `b-rame-texture.png`, `c-pistoni-mondo.png`, `c-pistoni-texture.png`, `confronto.png` — OK;
  - generator outputs, not committed (24 = 3 × (7 textures + icon)) — OK; `station.js` also refuses any written PNG with other chunks.
- Model structure (independent check of the 6 JSON): from/to inside 0..16 and from ≤ to, each element zero-thickness with one face, UV inside 0..16, rotation only 0/90, every texture variable defined and its file present, `cullface` exactly on block-boundary faces except the seam; 0 problems. Elements per half: A 84 + 66, B 107 + 43, C 85 + 79.
- `bench.check()` passes for all three (no pocket opens toward another face or joins another face's pocket). It was probed in memory with injected faults: a front pocket down to the ground row (4 reports), one up to the top (4), a front pocket touching a top pocket (8 reports of joined pockets); a closed pocket gives none. Its permanent negative test in `tools/art/selftest.py` (which `tools/art/README.md` asks for with every new check) is left to integration, when `station.py` joins `check_art.sh`.
- `node shot.js --views fronte` still renders with the modified `scene.html`.
- No AI model or vendor names, no personal names in the new or changed files (grep).

## What I would improve with more passes

- A: the bronze top still reads a little like wood at a distance; a more specular bronze (sharper highlight band, fewer brush streaks) or a darker bronze with bright edges.
- B: the back's X braces are busy; the iron posts could carry a forged look (bevelled corners); the redstone channel could show a wire path rather than speckle.
- C: the end hub is a painted disc on a square boss; a proper pivot with a piston rod going down the end would sell the "machine" more.
- Icons: hand-pixelled in one pass; they read at ×2 but need a second pass at ×1 (inventory size) next to vanilla icons.
- Relief: designs could keep dark outlines where inset walls meet the other half, so walls look the same lit or unlit (see concern 1).
- Previews: an optional smooth-lighting (AO) mode, and a shot at inventory scale of the icon among vanilla items.
- A validation step in the generator itself for what the ad-hoc JSON check did (boundary cullfaces, single-face elements), so `check_art.sh` can run it after integration.

## Concerns

1. **Dark inset walls in game.** Because the station has a full collision shape, the game lights every inset quad from the neighbour block in its direction (javap, both versions). Pocket walls facing the ground or the other half sample an opaque block: near-black with smooth lighting off (as the previews show, e.g. the right wall of the side half's front recess, C's bar sides; the ~0.1 lightmap value is estimated from the LightTexture formula, not measured). With smooth lighting on (the default) they should be darker but graded — a deduction, neither read in bytecode nor seen in a client. It reads as shadow lines; if the owner dislikes it, the alternative is fewer or no recesses (flat textured cubes, like vanilla furnace/crafting table).
2. **Not seen in the real client.** The models use zero-thickness single-face elements (valid vanilla JSON; vanilla `cross` uses zero-thickness planes) and a few faces with UV `rotation` 90 for pocket walls. The reader reproduces FaceBakery and matched the 4a-1 placeholder shot, but these particular features were not rendered by the game. The integration step should take one `client_shot.sh` screenshot on one variant first.
3. **Integration needs two edits besides running the generator**: drop the station entries from `tools/gen_placeholders.py` (it writes `station_top`, `station_side`, `station_front_main`, `station_front_side`, `item/station.png`, which `check_art.sh` compares and which the generator will overwrite), and add the station generator to `check_art.sh`. Noted in `tools/art/README.md`.
4. The preview's suit lighting is the existing preview Lambert lighting, not the game's entity lighting; only the station is lit as in the game, as the brief asks.
5. Facing rotations other than north are not pictured; the models carry nothing rotation-specific and the blockstate rotates them whole (uvlock off), as for the placeholder today.
6. **Direction B is the least metallic.** Oak covers most of its front, back and ends, so it reads close to a crate. I read the brief as "both alternatives built from the recipe's materials and from the suit's world", and gave B the recipe and C the suit's world; if the coordinator reads it as both alternatives coming from the heavy-industrial world, B needs repainting (e.g. the oak core reduced to a band behind iron), not a new structure.
