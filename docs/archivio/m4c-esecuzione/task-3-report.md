# Task 3 report: SuitHud al posto della riga, e gli strumenti che la misuravano

(Resumed after the coordinator's R11/R12 and completed: see "Ripresa dopo la decisione" at the end; final commit
e53ab48 on top of c3b43aa. The status below is the one of the first handback.)

Status: BLOCKED at the Step 6 gate. Commit c3b43aa on m4-stazione (parent 8b8afbc), signed (ssh), both trailers, with
the gate's message `client: controlli della 4c NON passati su 1.21.1-neoforge: in attesa di una decisione`.
`git status --short` is empty; no shots or logs are committed.

Scratch folder (shots, logs, outputs): <cartella temporanea>/m4c/t3/
(written `scratch/` below). The neoforge run is in scratch/neo (A), scratch/neo/mancino (B), scratch/neo/riavvio (C);
its output is scratch/neo.out.

## Progress

- [x] Step 1: arm_measure.sh selftest first (RED): 11 expectations differ, exit 1 (scratch/red.txt)
- [x] Step 2: the three measures (GREEN): `OK arm_measure`, exit 0 (scratch/green.txt); usage errors exit 2
- [x] Step 3: SuitHud, EnergyHud/EnergyLine/EnergyLineTest removed, language files, three Javadocs: four-variant build
  BUILD SUCCESSFUL, 381 JUnit on each variant (scratch/build-step3.log); the brief's grep finds nothing; `OK sources`
- [x] Step 4: m3d_shots.sh and m3c_shots.sh: `bash -n` clean
- [x] Step 5: m4c_shots.sh: `bash -n` clean; a dry run on placeholder shots evaluates all 16 checks (scratch/dry)
- [~] Step 6: 1.21.1-neoforge run done: 15 of 16 checks ok with exact rectangles, `riavvio` KO (the gate). Stopped here,
  as the gate says: thresholds not applied (values below), no M4C_SOLO_CONTROLLI pass, no 1.20.1-forge run
- [ ] Step 7: m3d (neoforge, forge) and m3c (neoforge) runs: not started (gate)
- [ ] Step 8: docs (compat.md, tools/client/README.md, README.md): not started (gate); a draft of the compat.md
  paragraph is in scratch/compat-hud-draft.md
- [x] R9: measure_lib.sh header names m4c_shots.sh; `unset EXO_SHOT_CLIENT_PREFS` in m3a, m3b, m3c, m3c2, m3d, m4a1
- [ ] R10: the 1.20.1 inventory-preview ranges: waits for my own m3d forge run (Step 7), as the ruling asks
- [ ] Step 9: end-of-task checks: not run (gate). Run at the commit: `OK sources`, `OK arm_measure`, `OK passi`,
  `bash -n` on every tools/client/*.sh
- [x] Step 10 (in the gate's form): commit c3b43aa with the gate's message

## The gate: why `riavvio` fails, and what would fix it

Check line (scratch/neo.out):

    KO riavvio: pixel=7132 riquadro=176x70+4+466; pixel=12320 riquadro=176x70+4+466 (atteso pixel=0 nel gruppo: ...)

- The plan's first measure is `differenza riavvio-senza-tuta riavvio-nascosto 176x70+4+466` → 0. The second
  (`differenza riavvio-visibile riavvio-nascosto` → riquadro `176x70+4+466`) passes, exactly.
- Cause: `riavvio-senza-tuta` is taken without the suit and `riavvio-nascosto` with it. The suit raises the player's
  eyes (`SuitSize.eyeHeight`, x1.65, design M3 §4.8), so the camera is about one block higher, the horizon moves and
  the grass under the group is seen in another perspective. The HUD is hidden in both shots: the 7132 pixels are
  grass, not HUD.
  - Full screen, the two shots differ in 161153 pixels, riquadro `960x365+0+175` (everything below the horizon, plus
    the arm).
  - I predicted this before Step 1 on Task 2's m3d shots (scratch/../t2/m3d-forge): `base-senza` vs `tuta-senza` (both
    invisible, without and with the suit) differ in 7128 pixels of `176x70+4+466`; `braccio-nudo` vs `tuta-senza`
    (both with the frame) in 0. I implemented the check as written and let the gate decide, as instructed.
  - Look at: scratch/neo/riavvio/riavvio-senza-tuta.png and riavvio-nascosto.png (scratch/r-pair.png side by side:
    no HUD in either, lower horizon with the suit).
- Not a defect of the code: the HUD was hidden at the restart. Measured on the same shots:
  - `colore riavvio/riavvio-nascosto 55FF55 176x70+4+466` → `colore=0` (no green of the HUD in the group), against
    `colore riavvio/riavvio-visibile 55FF55 176x70+4+466` → `colore=1888 riquadro=162x56+12+474`;
  - across runs the shots are identical to the pixel on the full screen:
    - `differenza nascosto riavvio/riavvio-nascosto` → `pixel=0 riquadro=0x0+0+0`: run C's client, which hid the HUD
      from the file, looks exactly like run A's client after H;
    - `differenza senza-vignetta riavvio/riavvio-visibile` → `pixel=0`: after H, run C shows exactly run A's HUD.
  - `ok riavvio-file: False False`: the restarted client read the file and H showed the HUD.
- Proposals for the coordinator (none applied; each changes what the plan's check compares):
  1. Replace the first measure with `differenza <cartella>/nascosto.png <cartella>/riavvio/riavvio-nascosto.png` →
     `pixel=0` (full screen, or in `176x70+4+466`). It is the strongest: same suit, same place, HUD hidden by H in A and
     by the file in C. It needs two runs to render the same world to the pixel: they did here (0 and 0), and on forge
     two separate runs of Task 2's m3d sequence (scratch/../t2/m3d-forge, A and B, invisible player) give
     `differenza base-senza.png mancino/base-senza.png` → `pixel=0 riquadro=0x0+0+0` and
     `differenza tuta-senza.png mancino/tuta-senza.png` → `pixel=0 riquadro=0x0+0+0` (also 0 in `176x70+4+466`).
  2. Or `colore riavvio/riavvio-nascosto 55FF55 176x70+4+466` → 0, with `riavvio-visibile` > 0. It does not depend on
     cross-run determinism, but proves only the absence of the HUD's green.
  - Either way `riavvio-senza-tuta` then has no use. Dropping it would change the plan's step list for run C.

## What I implemented

- `client/SuitHud` (new, `final`, package-private), as in "Tipi nuovi" without the vignette:
  - `register()`: `addHudRenderer(SuitHud::render)`, `ClientTickEvent.CLIENT_POST.register(SuitHud::tick)`,
    `CLIENT_PLAYER_QUIT` → `reset()`.
  - `tick`: `NONE` without a player; otherwise `HudLevel.of(SuitSize.wearsSuit(player), clientView(player))`, the
    energy state from `clientIndicators(player).map(FrameIndicators::energyState)`, `HudModel.ticksInState` from the
    tick before, and the spare cells (counted only with a level other than `NONE`: `getCount()` of the redstone cells
    with `cellEnergy > 0` in `inventory.items` and `inventory.offhand`), then `HudModel.of`. All in a `try`: a
    `RuntimeException` sets `NONE` and logs once per session `Suit HUD update failed with {}; the HUD is hidden until
    an update works (logged once per session)`.
  - `render`: nothing with `NONE`, `hideGui`, `ClientPrefs.hudHidden()`, no player or a spectator. Otherwise the
    plate (`PLATE` inside, 1-pixel `PLATE_EDGE` ring, not overlapping), with `FULL` the six pieces (outline ring and
    fill, not overlapping), the warning if `warningOn` (the triangle rows of "Tipi nuovi", the stem and dot left
    empty), the percentage from `TEXT_X` or `TEXT_X_AFTER_WARNING`, or without a cell `no_cell` in `GREY` at
    (`NO_CELL_X`, `NO_CELL_Y`); the cell icon (cap, body, charge) and `"×" + n` at
    `spareTextX(font.width(text))` in `spareColor()`; with a cell the bar frame (`BAR_EDGE` ring, `BAR_BACK`) and the
    fill (`lighter` first row, `energyColor` five rows), skipped when `fillWidth` is 0. No pose stack, no scale. All in
    a `try`: once per session `Suit HUD drawing failed with {}; the HUD is skipped for this frame (logged once per
    session)`. The `finally` of the vignette's blend is Task 4's.
  - `reset()`: energy state, tick counter, the two "already logged" flags.
- `RedstoneExoframesClient.init()`: `SuitHud.register();` in place of `addHudRenderer(EnergyHud::render)`.
- Deleted: `client/EnergyHud.java`, `client/EnergyLine.java`, `test/client/EnergyLineTest.java`.
- Language files, in place of `hud.redstone_exoframes.energy`: `percent` ("%s%%" / "%s %%"); `no_cell` "No cell" /
  "Senza cella" (R1); removed `energy`, `worst`, `condition.redstone_exoframes.critical` and `.broken`.
- Javadocs: `FirstPersonArm` criterion (b) "nel rettangolo dell'HUD della mano, design M4 §5.12 (nella 3d era la
  riga dell'energia, in alto a sinistra)", and next to the 3d values "energia 0 (il rettangolo della riga
  dell'energia di allora, 360x30+0+0)"; `SuitDamage.pieceName` "usato dai messaggi dei pezzi critici e rotti qui sopra
  e dalla schermata della stazione" (StationScreen uses it too); `DebugEnergy` "(scatto, HUD)".
- `tools/client/arm_measure.sh`: `HUD_RIGHT=176x70+4+466` and `HUD_LEFT=176x70+780+466` in place of `ENERGY`;
  `sagoma … [destra|sinistra]` with `hud=` (another third argument is exit 2); `differenza <a> <b> [<rect>]`;
  `colore <shot> <RRGGBB> [<rect>]` with a mask in the magenta mask's form (`-fx`, each channel within `DIFF`, the
  same 4 %; a colour that is not six hex digits is exit 2); header, usage and the magenta note ("HUD"); the selftest
  with fifteen expectations and the 1/255 rectangle moved to `10,470 109,499` with `rgb(90,140,71)`.
- `tools/client/m3d_shots.sh`: `nudo-senza` after `braccio-nudo` (in `shots_a`, "tutti e sedici gli scatti"); `nudo`
  measures `sagoma braccio-nudo nudo-senza`; `hud` in `criteria_bc` and `controllo`; the new `CRITERIA`; `sinistra` in
  `mancino-controllo` and `mancino`; the header paragraph of the brief; `unset EXO_SHOT_CLIENT_PREFS` (R9).
- `tools/client/m3c_shots.sh`: shots `hud-energia`, `hud-energia-bassa`, `hud-energia-esaurita` with the same charge
  checks; the step 8 header lines (green, red under 10 % at 500 RE, grey at 0 RE) and "L'HUD sta in basso, nell'angolo
  opposto alla mano principale (parte 4c)"; `unset EXO_SHOT_CLIENT_PREFS` (R9). The wording "percentuale sotto il 50 %"
  of `hud-energia` is not yet checked against a shot (Step 7).
- `tools/client/m4c_shots.sh`: run A with the phases from "pieno" to "terza persona" (the `@tieni j 0.2` of "senza
  elmo" kept), run B (`EXO_SHOT_MAIN_HAND=left`, shots in `<cartella>/mancino`), run C with `riavvio-senza-tuta`,
  `riavvio-nascosto`, `riavvio-visibile`; the sixteen Task 3 checks with rectangles and colours as named constants at
  the top, thresholds provisional at 10, `LOW_FILL_MAX_W=10`; B and C only after A exits 0, C also needs A's prefs
  file, B failing does not stop C; every shot and copied file of the three runs removed before starting.
- R9: `measure_lib.sh` header names `m4c_shots.sh`; `unset EXO_SHOT_CLIENT_PREFS` with a comment in the six sequences.

## Tests and results

- TDD of arm_measure.sh (Step 1-2):
  - RED: `bash tools/client/arm_measure.sh --selftest; echo $?` → eleven `selftest:` blocks with `atteso`/`trovato`
    (the three `sagoma` with `energia=` found, the two hand variants, three `differenza`, three `colore` with the usage
    error found), `ERRORE: selftest: uscite diverse dalle attese`, then `1` (scratch/red.txt).
  - GREEN: the same command → `OK arm_measure`, `0` (scratch/green.txt); `grep -c 'HUD_RIGHT=176x70+4+466'` → 1.
  - Usage errors, each exit 2: `sagoma a a alto`, `sagoma a`, `colore a 55FF5`, `colore a GGFF55`, `colore a 55FF55
    10x10+955+0`, `differenza a`, `differenza a a 0x5+0+0`, five arguments to `colore`.
- Step 3: `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build
  :1.21.1-neoforge:build` → BUILD SUCCESSFUL (scratch/build-step3.log); JUnit with the plan's command:
  `1.20.1-forge 381`, `1.20.1-fabric 381`, `1.21.1-fabric 381`, `1.21.1-neoforge 381`. The brief's grep over `src`
  prints nothing. `OK sources`.
- Step 4-5: `bash -n` clean on m3d, m3c, m4c (and every tools/client/*.sh at the commit). m4c dry runs: placeholder
  shots → all 16 checks print a line (two ok, fourteen KO with measures) in 28 s; empty folder → `FAIL … mancano gli
  scatti`, exit 1; one argument → exit 2.
- Step 6, `EXO_ACCEPT_EULA=true bash tools/client/m4c_shots.sh 1.21.1-neoforge scratch/neo` → exit 1 in 5m54s (A 146
  s, B 93 s, C 79 s); `log-check.txt` empty (0 bytes) in the three folders; no `Suit HUD` and no `Failed to load
  options` line in the three client.logs. Check lines, measured value and threshold (all thresholds provisional 10):

  | Check | Measured | Expected |
  |---|---|---|
  | chat-e-tasti | `True False` | `True False` |
  | riavvio-file | `False False` | `False False` |
  | gruppo | `pixel=12320 riquadro=176x70+4+466`; silhouette `pixel=2940 riquadro=42x70+4+466` | riquadro 176x70+4+466 |
  | testo | `colore=244 riquadro=46x14+50+482` | ≥ TEXT_MIN |
  | barra-normale | `colore=1220 riquadro=122x10+52+510` | riquadro 122x10+52+510 |
  | sagoma | diff `pixel=1024 riquadro=32x60+10+472`; green before 424, yellow after 424 (`28x56+12+474`), green after 0 | inside 42x70+4+466 |
  | critico | red 424 (`28x56+12+474`); yellow 0 | yellow 0 |
  | barra-bassa | diff `pixel=2052 riquadro=124x38+50+482`; red in bar `colore=60 riquadro=6x10+52+510`; green in bar 0; red in silhouette 0 | inside 134x70+46+466; X 52, width ≤ 10 |
  | barra-esaurita | green in bar 0; red 0; grey `colore=248 riquadro=42x16+50+482` | 0; 0; ≥ TEXT_MIN |
  | scorta | diff `pixel=228 riquadro=24x16+152+482`; grey before 112, white after 84 (`22x14+152+482`) | inside 50x18+126+482 |
  | solo-barra | `pixel=9380 riquadro=134x70+46+466`; silhouette 0 | riquadro 134x70+46+466; 0 |
  | terza-persona | `pixel=12320 riquadro=176x70+4+466`; corners 0, 0, 0 | riquadro 176x70+4+466; 0 |
  | mancino-gruppo | `pixel=12320 riquadro=176x70+780+466`; silhouette 2940 (`42x70+914+466`); percentage 244 (`46x14+784+482`) | riquadro 176x70+780+466 |
  | mancino-sagoma | diff `pixel=1024 riquadro=32x60+920+472`; yellow 424 (`28x56+922+474`) | inside 42x70+914+466 |
  | mancino-solo-barra | `pixel=9380 riquadro=134x70+780+466`; silhouette 0 | riquadro 134x70+780+466; 0 |
  | riavvio | **`pixel=7132 riquadro=176x70+4+466`**; `pixel=12320 riquadro=176x70+4+466` | **pixel 0**; riquadro 176x70+4+466 |

  Final line: `FAIL 1.21.1-neoforge: controlli falliti: riavvio`. The lines verbatim (scratch/neo.out):

```
ok chat-e-tasti: True False
ok riavvio-file: False False
ok gruppo: pixel=12320 riquadro=176x70+4+466; sagoma pixel=2940 riquadro=42x70+4+466
ok testo: colore=244 riquadro=46x14+50+482
ok barra-normale: colore=1220 riquadro=122x10+52+510
ok sagoma: pixel=1024 riquadro=32x60+10+472; verde prima colore=424 riquadro=28x56+12+474; giallo dopo colore=424 riquadro=28x56+12+474; verde dopo colore=0 riquadro=0x0+0+0
ok critico: rosso colore=424 riquadro=28x56+12+474; giallo colore=0 riquadro=0x0+0+0
ok barra-bassa: pixel=2052 riquadro=124x38+50+482; rosso nella barra colore=60 riquadro=6x10+52+510; verde nella barra colore=0 riquadro=0x0+0+0; rosso nella sagoma colore=0 riquadro=0x0+0+0
ok barra-esaurita: verde nella barra colore=0 riquadro=0x0+0+0; rosso colore=0 riquadro=0x0+0+0; grigio colore=248 riquadro=42x16+50+482
ok scorta: pixel=228 riquadro=24x16+152+482; grigio prima colore=112 riquadro=22x14+152+482; bianco dopo colore=84 riquadro=22x14+152+482
ok solo-barra: pixel=9380 riquadro=134x70+46+466; sagoma pixel=0 riquadro=0x0+0+0
ok terza-persona: pixel=12320 riquadro=176x70+4+466; angoli pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0
ok mancino-gruppo: pixel=12320 riquadro=176x70+780+466; sagoma pixel=2940 riquadro=42x70+914+466; percentuale colore=244 riquadro=46x14+784+482
ok mancino-sagoma: pixel=1024 riquadro=32x60+920+472; giallo colore=424 riquadro=28x56+922+474
ok mancino-solo-barra: pixel=9380 riquadro=134x70+780+466; sagoma pixel=0 riquadro=0x0+0+0
KO riavvio: pixel=7132 riquadro=176x70+4+466; pixel=12320 riquadro=176x70+4+466 (atteso pixel=0 nel gruppo: il client e' partito con l'HUD nascosto; riquadro=176x70+4+466: H lo mostra)
FAIL 1.21.1-neoforge: controlli falliti: riavvio
```
- Thresholds as Step 6 would set them (half of the smallest measured value, floored to tens; NOT applied, the gate
  stopped first): `GROUP_MIN=6160` (12320 in gruppo, terza-persona, mancino-gruppo), `SIL_MIN=1470` (2940, 2940),
  `SIL_CHANGE_MIN=510` (1024, 1024), `SIL_COLOUR_MIN=210` (424 x4), `TEXT_MIN=120` (244, grey 248, 244),
  `BAR_MIN=610` (1220), `SPARE_MIN=110` (228), `SPARE_COLOUR_MIN=40` (grey 112, white 84), `BAR_ONLY_MIN=4690` (9380).
- Exact pixel checks beyond the plan's (scratch/exact.sh, output scratch/exact-neo.txt: 19 ok, 0 KO). They pin what no
  JUnit pins (the coordinator's "known gap"), expected values computed from HudLayout and HudModel:
  - piece fills: `colore=424 riquadro=28x56+12+474` green, yellow, red (106 GUI px: helmet 4x4, torso 6x9, four limbs
    1x9); outlines `1C551C` / `55551C` `colore=600 riquadro=32x60+10+472` (150 GUI px); left hand `28x56+922+474`;
  - warning (EMPTY, grey) `colore=128 riquadro=16x16+50+482` (32 GUI px, 8x8 at (2,8));
  - bar: five rows `1220` in `122x10+52+510`, lighter row `AAFFAA` `244` in `122x2+52+508`, frame ring `0C0D0F` `552`
    in `126x16+50+506`, empty bar `282A2E` `1464`; low bar red `60` in `6x10+52+510`, `FFAAAA` `12` in `6x2+52+508`;
  - cell icon: charge `6x8+142+488`, cap `6x2+142+482`, body `10x14+140+484`; "x0" grey glyphs `22x14+152+482`.
  - One plan fact is off by one: "Fatti del gioco" says `font.width("×0")` is 11; in the game it is 12 (the grey
    glyphs start at energy x 53 and the shadow ends at 65). The code uses `font.width` at run time, so the text still
    ends at `SPARE_RIGHT` 65 and the icon sits at `spareTextX − 1 − CELL_ICON_W` = 47; my first scratch expectation
    used 11 and missed by one GUI pixel, corrected. "×10" would still fit `SPARE_AREA` (icon from 41).
- Shots looked at (dev client in English), all as the brief describes: senza-vignetta (plate with copper edge bottom
  left, green six-piece silhouette, "100%" green, cell icon and "×0" grey right-aligned, full green bar with a lighter
  first row, the suit arm far away bottom right); danneggiato (yellow silhouette); critico (red); bassa ("5%" red
  after the warning, which was lit, short red bar); esaurita ("0%" grey, grey warning, empty bar); scorta ("×1"
  white); senza-elmo (only the energy subgroup, where it stood); terza (same group, the suit from behind);
  mancino/mancino (group bottom right, silhouette on the right edge, text read from the left). Crops: scratch/
  sv-crop.png, hud-states.png, terza-half.png, m-half.png, m-crop.png, msgs.png ("Helmet vignette off", "Exoframe HUD
  hidden: H to show it", "Exoframe HUD shown").

## Files changed (commit c3b43aa)

- New: src/main/java/com/toxineren/exoframes/client/SuitHud.java
- Deleted: client/EnergyHud.java, client/EnergyLine.java, src/test/.../client/EnergyLineTest.java
- Modified: client/RedstoneExoframesClient.java, client/FirstPersonArm.java, frame/SuitDamage.java,
  frame/DebugEnergy.java, lang/en_us.json, lang/it_it.json, tools/client/arm_measure.sh, m3d_shots.sh, m3c_shots.sh,
  m4c_shots.sh, measure_lib.sh, m3a_shots.sh, m3b_shots.sh, m3c2_shots.sh, m4a1_shots.sh
- Not yet changed (Step 8, after the decision): docs/compat.md (still names EnergyHud in "ClientRenderHooks"/"Uso", the
  init bullet and the EnergyHud/EnergyLine paragraph), tools/client/README.md, README.md.

## Self-review

- SuitHud follows "Tipi nuovi": registration, tick, render order, the two ERROR texts, reset. No pose stack, no scale,
  no `isCreative()`. Rings are drawn as four strips plus the inside, so the translucent plate and unmounted pieces
  never blend a pixel twice. The exact checks confirm every rectangle and colour.
- Source is ASCII except `§` in comments; `×` is the escape `×` in a named constant.
- Lines over 120 characters: none added. Pre-existing ones (DebugEnergy:12, m3a:53, m3c:69/134) are untouched.
- The m4c header says "Dura circa otto minuti a cache calde (misurati: vedi tools/client/README.md)": a placeholder
  from before the run. Measured: 5m54s on neoforge (A 146 s, B 93 s, C 79 s). It is to be corrected with the README in
  Step 8, after the forge run.
- Ruling R10 (inventory ranges) needs my m3d forge run (Step 7), so it waits with Step 7.
- Nit for the resume round (not edited now): the SuitHud class Javadoc ends "Solo client, solo sul thread del
  client". As Task 2's review found for ClientPrefs, on Forge and NeoForge `RedstoneExoframesClient.init()` runs on the
  mod-loading thread, so `register()` and the class initialisation run there; only `tick`, `render`, `reset` and the
  state they share run on the client thread. The Javadoc should say that.
- docs/compat.md line 47 says, in the past tense, that `frame_state_sync` indicators were proven with m3c_shots.sh
  ("colore della riga dell'energia"); it is outside the places the Global Constraints let this task change in
  compat.md, so I plan to leave it (a past fact) unless the coordinator says otherwise.

## Issues and concerns

1. The gate: `riavvio`'s first measure cannot be 0 by construction (eye height with the suit); details and two
   proposals above. Everything else in the plan's check list passed on neoforge with exact rectangles.
2. Not done because of the gate: thresholds (values above), M4C_SOLO_CONTROLLI, the 1.20.1-forge m4c run, Steps 7-9,
   R10. With a decision on `riavvio`, the remaining work is: apply it, set the thresholds, re-check with
   M4C_SOLO_CONTROLLI on scratch/neo (no new neoforge run is needed unless run C's steps change), the forge m4c run,
   then Steps 7-10.
3. Plan fact: `font.width("×0")` is 12 in the game, not 11 ("Fatti del gioco"). No code depends on the 11.

## Ripresa dopo la decisione

Resumed after the coordinator's R11 (the `riavvio` check, option 1) and R12 (thread Javadocs). Commit e53ab48 on top
of c3b43aa (no amend, no rewrite), signed (ssh), both trailers, with the Step 10 message. `git status --short` is
empty; no shots or logs are committed. Not pushed.

Progress:
- [x] R11: the first `riavvio` measure is `differenza nascosto riavvio/riavvio-nascosto 176x70+4+466` → 0; the second
  is unchanged; `riavvio-senza-tuta` removed from run C (steps and shot list); the script header says why (eye height
  x1.65 without the suit, the 7132 pixels, both shots with the suit, the HUD hidden and the vignette off, cross-run
  pixel equality); the README's m4c table says the same
- [x] R12: SuitHud and ClientPrefs Javadocs: `register()` / `load()` run once during mod init, a mod-loading thread on
  Forge and NeoForge; everything else on the client thread
- [x] thresholds from the neoforge measures (below), M4C_SOLO_CONTROLLI recheck on neoforge
- [x] m4c on 1.20.1-forge
- [x] Step 7: m3d on 1.21.1-neoforge and 1.20.1-forge, m3c on 1.21.1-neoforge
- [x] R10, and the other m3d measured values that changed (below); R9 was already complete
- [x] Step 8 docs: docs/compat.md, tools/client/README.md, README.md
- [x] Step 9 end-of-task checks
- [x] Step 10 commit e53ab48

### Thresholds (m4c_shots.sh), half of the smallest value measured on 1.21.1-neoforge, floored to tens

| Threshold | Value | Measured (neoforge; forge identical) |
|---|---|---|
| `GROUP_MIN` | 6160 | 12320 in gruppo, terza-persona, mancino-gruppo (every pixel of the 176x70 group) |
| `SIL_MIN` | 1470 | 2940 in gruppo and mancino-gruppo (every pixel of the 42x70 silhouette subgroup) |
| `SIL_CHANGE_MIN` | 510 | 1024 in sagoma and mancino-sagoma (fills 424 + outlines 600) |
| `SIL_COLOUR_MIN` | 210 | 424 in sagoma (green, yellow), critico (red), mancino-sagoma (yellow) |
| `TEXT_MIN` | 120 | 244 testo, 248 barra-esaurita (grey: "0%" and the steady warning), 244 mancino-gruppo |
| `BAR_MIN` | 610 | 1220 barra-normale |
| `SPARE_MIN` | 110 | 228 scorta |
| `SPARE_COLOUR_MIN` | 40 | 112 (grey "×0"), 84 (white "×1") |
| `BAR_ONLY_MIN` | 4690 | 9380 solo-barra (every pixel of the 134x70 energy subgroup) |
| `LOW_FILL_MAX_W` | 10 (fixed) | 6 (500 RE of 10 000: 3 GUI pixels) |

### New check lines

`M4C_SOLO_CONTROLLI=1 bash tools/client/m4c_shots.sh 1.21.1-neoforge scratch/neo` with the thresholds and the new
`riavvio` → exit 0 (scratch/neo-recheck.out; again with the final script: scratch/neo-final-recheck.out, 16 ok, exit 0):

```
ok chat-e-tasti: True False
ok riavvio-file: False False
ok gruppo: pixel=12320 riquadro=176x70+4+466; sagoma pixel=2940 riquadro=42x70+4+466
ok testo: colore=244 riquadro=46x14+50+482
ok barra-normale: colore=1220 riquadro=122x10+52+510
ok sagoma: pixel=1024 riquadro=32x60+10+472; verde prima colore=424 riquadro=28x56+12+474; giallo dopo colore=424 riquadro=28x56+12+474; verde dopo colore=0 riquadro=0x0+0+0
ok critico: rosso colore=424 riquadro=28x56+12+474; giallo colore=0 riquadro=0x0+0+0
ok barra-bassa: pixel=2052 riquadro=124x38+50+482; rosso nella barra colore=60 riquadro=6x10+52+510; verde nella barra colore=0 riquadro=0x0+0+0; rosso nella sagoma colore=0 riquadro=0x0+0+0
ok barra-esaurita: verde nella barra colore=0 riquadro=0x0+0+0; rosso colore=0 riquadro=0x0+0+0; grigio colore=248 riquadro=42x16+50+482
ok scorta: pixel=228 riquadro=24x16+152+482; grigio prima colore=112 riquadro=22x14+152+482; bianco dopo colore=84 riquadro=22x14+152+482
ok solo-barra: pixel=9380 riquadro=134x70+46+466; sagoma pixel=0 riquadro=0x0+0+0
ok terza-persona: pixel=12320 riquadro=176x70+4+466; angoli pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0
ok mancino-gruppo: pixel=12320 riquadro=176x70+780+466; sagoma pixel=2940 riquadro=42x70+914+466; percentuale colore=244 riquadro=46x14+784+482
ok mancino-sagoma: pixel=1024 riquadro=32x60+920+472; giallo colore=424 riquadro=28x56+922+474
ok mancino-solo-barra: pixel=9380 riquadro=134x70+780+466; sagoma pixel=0 riquadro=0x0+0+0
ok riavvio: pixel=0 riquadro=0x0+0+0; pixel=12320 riquadro=176x70+4+466
OK 1.21.1-neoforge: scatti in scratch/neo
```

`EXO_ACCEPT_EULA=true bash tools/client/m4c_shots.sh 1.20.1-forge scratch/forge` → exit 0 in 6m12s (A 154 s, B 98 s,
C 84 s); `log-check.txt` empty in the three folders, no `Suit HUD` and no `Failed to load options` line in the three
client.logs (scratch/forge.out; the final script again on these shots: scratch/forge-final-recheck.out, 16 ok, exit 0):

```
ok chat-e-tasti: True False
ok riavvio-file: False False
ok gruppo: pixel=12320 riquadro=176x70+4+466; sagoma pixel=2940 riquadro=42x70+4+466
ok testo: colore=244 riquadro=46x14+50+482
ok barra-normale: colore=1220 riquadro=122x10+52+510
ok sagoma: pixel=1024 riquadro=32x60+10+472; verde prima colore=424 riquadro=28x56+12+474; giallo dopo colore=424 riquadro=28x56+12+474; verde dopo colore=0 riquadro=0x0+0+0
ok critico: rosso colore=424 riquadro=28x56+12+474; giallo colore=0 riquadro=0x0+0+0
ok barra-bassa: pixel=1992 riquadro=124x38+50+482; rosso nella barra colore=60 riquadro=6x10+52+510; verde nella barra colore=0 riquadro=0x0+0+0; rosso nella sagoma colore=0 riquadro=0x0+0+0
ok barra-esaurita: verde nella barra colore=0 riquadro=0x0+0+0; rosso colore=0 riquadro=0x0+0+0; grigio colore=248 riquadro=42x16+50+482
ok scorta: pixel=228 riquadro=24x16+152+482; grigio prima colore=112 riquadro=22x14+152+482; bianco dopo colore=84 riquadro=22x14+152+482
ok solo-barra: pixel=9380 riquadro=134x70+46+466; sagoma pixel=0 riquadro=0x0+0+0
ok terza-persona: pixel=12320 riquadro=176x70+4+466; angoli pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0
ok mancino-gruppo: pixel=12320 riquadro=176x70+780+466; sagoma pixel=2940 riquadro=42x70+914+466; percentuale colore=244 riquadro=46x14+784+482
ok mancino-sagoma: pixel=1024 riquadro=32x60+920+472; giallo colore=424 riquadro=28x56+922+474
ok mancino-solo-barra: pixel=9380 riquadro=134x70+780+466; sagoma pixel=0 riquadro=0x0+0+0
ok riavvio: pixel=0 riquadro=0x0+0+0; pixel=12320 riquadro=176x70+4+466
OK 1.20.1-forge: scatti in scratch/forge
```

- Every value equals neoforge except the `barra-bassa` difference count (1992 against 2052): on forge `bassa` fell in
  the warning's dark half-second ("5%" keeps its place after the warning slot, Decision 5; 0 red pixels in the
  warning's 16x16), on neoforge the warning was lit (128 red pixels). That measure has no count threshold.
- Exact pixel checks (scratch/exact.sh): 19 ok, 0 KO on forge too (scratch/exact-forge.txt): the drawing is the same to
  the pixel on 1.20.1 and 1.21.1.

### Step 7

`EXO_ACCEPT_EULA=true bash tools/client/m3d_shots.sh 1.21.1-neoforge scratch/m3d-neo` → 14 ok, exit 0, 3m29s (A 113 s,
B 77 s), `log-check.txt` empty in both folders:

```
ok controllo: pixel=18075 copertura=3.49 mirino=0 barra=0 hud=0 riquadro=161x149+677+391
ok dipinto: magenta=39962 riquadro=286x188+674+352; meta' sinistra magenta=0 riquadro=0x0+0+0
ok criteri: pixel=45442 copertura=8.77 mirino=0 barra=0 hud=0 riquadro=286x205+674+335
ok solo-tuta: sagoma=18075 uguali=0
ok nudo: magenta=0 riquadro=0x0+0+0; pixel=40026 copertura=7.72 mirino=0 barra=0 hud=0 riquadro=260x205+700+335; sagoma=18075 uguali=0
ok bastone: magenta=0 riquadro=0x0+0+0
ok mappa: sinistra magenta=23637 riquadro=365x179+0+361; destra magenta=28977 riquadro=369x182+591+358
ok terza-persona: magenta=23157 riquadro=318x312+321+221
ok inventario: magenta=10850 riquadro=98x140+356+120
ok senza-tuta: pixel=0 copertura=0.00 mirino=0 barra=0 hud=0 riquadro=0x0+0+0
ok telaio-piazzato: pixel=52142 copertura=10.06 mirino=3600 barra=0 hud=0 riquadro=258x284+351+113; magenta=39962 riquadro=286x188+674+352, il 100 % di braccio-rottame
ok mancino-controllo: pixel=18075 copertura=3.49 mirino=0 barra=0 hud=0 riquadro=161x149+122+391
ok mancino: meta' destra magenta=0 riquadro=0x0+0+0; pixel=45442 copertura=8.77 mirino=0 barra=0 hud=0 riquadro=286x205+0+335
ok specchio: mancino magenta=39962 riquadro=286x188+0+352, il 100 % del destro (magenta=39962 riquadro=286x188+674+352); riquadro specchiato 286x188+674+352, scarti sinistra 0 alto 0 destra 0 basso 0
OK 1.21.1-neoforge: scatti in scratch/m3d-neo
```

`EXO_ACCEPT_EULA=true bash tools/client/m3d_shots.sh 1.20.1-forge scratch/m3d-forge` → 14 ok, exit 0, 3m36s (A 116 s,
B 82 s), `log-check.txt` empty in both folders:

```
ok controllo: pixel=18075 copertura=3.49 mirino=0 barra=0 hud=0 riquadro=161x149+677+391
ok dipinto: magenta=39962 riquadro=286x188+674+352; meta' sinistra magenta=0 riquadro=0x0+0+0
ok criteri: pixel=45442 copertura=8.77 mirino=0 barra=0 hud=0 riquadro=286x205+674+335
ok solo-tuta: sagoma=18075 uguali=0
ok nudo: magenta=0 riquadro=0x0+0+0; pixel=40026 copertura=7.72 mirino=0 barra=0 hud=0 riquadro=260x205+700+335; sagoma=18075 uguali=0
ok bastone: magenta=0 riquadro=0x0+0+0
ok mappa: sinistra magenta=23637 riquadro=365x179+0+361; destra magenta=28977 riquadro=369x182+591+358
ok terza-persona: magenta=23158 riquadro=318x312+321+221
ok inventario: magenta=18633 riquadro=166x202+323+66
ok senza-tuta: pixel=0 copertura=0.00 mirino=0 barra=0 hud=0 riquadro=0x0+0+0
ok telaio-piazzato: pixel=52142 copertura=10.06 mirino=3600 barra=0 hud=0 riquadro=258x284+351+113; magenta=39962 riquadro=286x188+674+352, il 100 % di braccio-rottame
ok mancino-controllo: pixel=18075 copertura=3.49 mirino=0 barra=0 hud=0 riquadro=161x149+122+391
ok mancino: meta' destra magenta=0 riquadro=0x0+0+0; pixel=45442 copertura=8.77 mirino=0 barra=0 hud=0 riquadro=286x205+0+335
ok specchio: mancino magenta=39962 riquadro=286x188+0+352, il 100 % del destro (magenta=39962 riquadro=286x188+674+352); riquadro specchiato 286x188+674+352, scarti sinistra 0 alto 0 destra 0 basso 0
OK 1.20.1-forge: scatti in scratch/m3d-forge
```

- `nudo` now measures `braccio-nudo` against `nudo-senza`: copertura 7,72, hud 0, the same riquadro `260x205+700+335`
  as before. In the HUD rectangle `braccio-nudo` vs `nudo-senza` differ in 0 pixels (both show the bar only), while
  against `tuta-senza` they would differ in 3072 (the silhouette): Decision 2 holds in the client.
- Measured values that changed, documented in m3d_shots.sh and the README:
  - `mappa` left half: 23637 on both variants (documented 28407). The HUD group sits over the left map arm, as design
    M4 §5.12 accepts: before, 12065 magenta pixels of the arm lay in `176x70+4+466`, now 7295, and 12065 − 7295 =
    4770 = 28407 − 23637. Shot: scratch/m3d-neo/mappa.png.
  - `terza-persona` magenta: 23157 (neoforge) and 23158 (forge), under the documented 23538-23745, same riquadro
    `318x312+321+221`. The range now reads 23157-23745; the check (width ≥ 130) is unchanged. Evidence that the HUD
    is not the cause: `magenta terza-persona.png 176x70+4+466` is `magenta=0` in Task 2's forge shot and in both of
    mine, and `differenza` between Task 2's and my forge third-person shots puts every differing pixel in
    `293x119+332+343` (5384 pixels: the suit's body and arms, far from the group). Both variants moved together, after
    the 2,5 s that `nudo-senza` adds before the third-person step: the suit's pose at the moment of the shot, not the
    drawing of the HUD.
  - inventory, R10: forge 166 wide from x 323, magenta 18633 (Task 2's run: 176 from x 318, 18831): Forge now reads
    "largo 166-176 pixel da x 318-323, magenta 18633-18831", and 1.20.1 "fra 166 e 176" in the threshold comment and
    the README row. 1.21.1: magenta 10850, so "fra 10782 e 10850".
  - Durations are within the documented 207-225 s.

`EXO_ACCEPT_EULA=true bash tools/client/m3c_shots.sh 1.21.1-neoforge scratch/m3c-neo` → `OK 1.21.1-neoforge`, exit 0,
1m56s; `log-check.txt` empty; 0 `moved wrongly`. Shots `hud-energia.png` ("49%" green, the bar half full),
`hud-energia-bassa.png` ("4%" red after the warning slot, short red bar: in survival the cell drains between
`debug energy 500` and the shot, and the percentage floors, so I dropped the "5 %" I had written in the m3c header),
`hud-energia-esaurita.png` ("0%" grey, the steady grey warning, empty bar), all bottom left (scratch/m3c-hud.png).

### Step 8 (docs)

- docs/compat.md: the EnergyHud/EnergyLine paragraph became the HUD paragraph (SuitHud with `addHudRenderer` and the
  client tick, the partialTick not needed; HudLevel decides NONE; HudModel's legend and the place of the energy
  colours, question 9; HudLayout's place of the position, question 8; their JUnit; nothing with F1, H, spectator; it
  stays under screens; on 1.21.1 vanilla elements that overlap are above, on 1.20.1 below; no 3D drawing); the
  ClientRenderHooks "Uso" line and the init bullet name SuitHud. The past-tense line 47 (what 3c-1 proved with the
  line's colour) stays.
- tools/client/README.md: usage line; the arm_measure table (`sagoma … [destra|sinistra]` with `hud=` and the two
  rectangles, `differenza`, `colore`, the "del colore" rule, "HUD" in the magenta note); the 3c row and sentence
  (`hud-energia*`); the 3d paragraph (the HUD in the shots, `nudo-senza`, criterion (b) on the HUD rectangle,
  `sinistra` in the left-hand checks, "sedici", the mappa and inventory values); the m4c paragraph rewritten with runs
  A, B, C and a table of the sixteen checks (R11's `riavvio` wording included); `EXO_SHOT_CLIENT_PREFS` row (R9);
  durations ("Quanto dura": m4c 354-373 s; arm_measure timings).
- README.md: "riga dell'energia" → "HUD dell'energia" in the 3c sequence's description.
- `grep -n "riga dell'energia\|360x30" tools/client/README.md tools/client/arm_measure.sh tools/client/m3d_shots.sh
  docs/compat.md` → only docs/compat.md:47 and :248, both past tense.

### Step 9 (end-of-task checks)

The build, JUnit, static checks and in-world tests ran on the tree of e53ab48 except for one edit made after the
build: a reflow of shell comments in the m4c_shots.sh header (no code). `bash -n` and both `M4C_SOLO_CONTROLLI=1`
rechecks (neoforge and forge shots, 16 ok each, exit 0) ran on the final script.

- Four-variant `build` + `collectJar` (the exact command) → BUILD SUCCESSFUL (scratch/build-final.log; the test tasks
  ran on the four variants).
- JUnit: `1.20.1-forge 381`, `1.20.1-fabric 381`, `1.21.1-fabric 381`, `1.21.1-neoforge 381`; no suite with failures
  or errors.
- `OK sources`, `OK jars`, `OK data`, `OK art`, `OK arm_measure`, `OK passi`; `bash -n` clean on every
  tools/client/*.sh.
- `bash tools/run_gametests.sh` → `OK 1.20.1-forge: 235 test obbligatori passati, nessun avviso fuori elenco`, and the
  same line for 1.20.1-fabric, 1.21.1-fabric and 1.21.1-neoforge (scratch/gametests-final.log, 2m38s).

### Shots to look at

- m4c, neoforge (scratch/neo) and forge (scratch/forge): `senza-vignetta.png`, `danneggiato.png`, `critico.png`,
  `bassa.png` (warning lit on neoforge, dark on forge), `esaurita.png`, `scorta.png`, `senza-elmo.png`, `terza.png`,
  `mancino/mancino.png`, `messaggio-vignetta.png`, `messaggio-nascosto.png`, `riavvio/riavvio-messaggio.png`. Crops:
  scratch/hud-states.png, scratch/f-states.png, scratch/m-crop.png, scratch/msgs.png.
- m3d: scratch/m3d-neo/mappa.png (the group over the left map arm), braccio-nudo.png and nudo-senza.png
  (scratch/nudo-pair.png).
- m3c: scratch/m3c-neo/hud-energia*.png (scratch/m3c-hud.png).

### Files changed in e53ab48

README.md, docs/compat.md, client/ClientPrefs.java, client/SuitHud.java, tools/client/README.md,
tools/client/m3c_shots.sh, tools/client/m3d_shots.sh, tools/client/m4c_shots.sh.

### Self-review of the resumption

- `riavvio` compares A's `nascosto` with C's `riavvio-nascosto` in the group and keeps the second measure; the stale
  `riavvio-senza-tuta.png` from the first neoforge run is still in scratch/neo/riavvio and no longer read.
- Thresholds have their measured values next to them in the script and in the README table.
- Comment and doc edits after the forge run were rechecked: both shot folders pass with the final script.
- The m3d `terza-persona` range and the 1.21.1 inventory range were widened beyond R10's literal 1.20.1 range, because
  my runs measured outside them; the checks are unchanged.

### Concerns

- None blocking. Notes for the reviewer: the plan's "Fatti del gioco" says `font.width("×0")` is 11, the game gives 12
  (no code depends on it); the m4c `riavvio` check now relies on two client runs rendering the same scene to the
  pixel, measured on both variants here and on Task 2's forge m3d runs.

## Correzione 1

Review finding (Important, plan-mandated) and Ruling R13: `barra-esaurita` passed with the warning missing (the warning
alone is 128 grey pixels, "0%" alone 120, and the measure only asked grey >= `TEXT_MIN` 120). Commit 5f5d252 on top of
e53ab48, signed (ssh), both trailers, `git status --short` empty, not pushed. The two Minor findings are deferred, as
ruled; nothing else changed.

What changed:
- tools/client/m4c_shots.sh: the constant `WARNING_PERCENT_RIGHT=42x16+50+482` (comment: "nell'area della
  percentuale, l'avviso fisso e "0%" in grigio (esaurita)") with the other rectangles; in `barra-esaurita`,
  `&& is_box "$m3" "$WARNING_PERCENT_RIGHT"`, with a comment saying what the box is made of (the steady warning, 8x8
  from (2, 8) of the energy subgroup, and the digits of "0%" after its slot; alone they would make 128 and 120
  pixels, both enough for `TEXT_MIN`, so only the box sees one missing or the warning moved); the `want` text and the
  header's check line ("nel loro riquadro esatto").
- tools/client/README.md: the `barra-esaurita` row says the grey must also have the box `42x16+50+482`, and why.

Covering tests (no client run, no Gradle build):

```
$ bash -n tools/client/m4c_shots.sh; echo "bash -n exit=$?"
bash -n exit=0
```

```
$ M4C_SOLO_CONTROLLI=1 bash tools/client/m4c_shots.sh 1.21.1-neoforge scratch/neo
ok chat-e-tasti: True False
ok riavvio-file: False False
ok gruppo: pixel=12320 riquadro=176x70+4+466; sagoma pixel=2940 riquadro=42x70+4+466
ok testo: colore=244 riquadro=46x14+50+482
ok barra-normale: colore=1220 riquadro=122x10+52+510
ok sagoma: pixel=1024 riquadro=32x60+10+472; verde prima colore=424 riquadro=28x56+12+474; giallo dopo colore=424 riquadro=28x56+12+474; verde dopo colore=0 riquadro=0x0+0+0
ok critico: rosso colore=424 riquadro=28x56+12+474; giallo colore=0 riquadro=0x0+0+0
ok barra-bassa: pixel=2052 riquadro=124x38+50+482; rosso nella barra colore=60 riquadro=6x10+52+510; verde nella barra colore=0 riquadro=0x0+0+0; rosso nella sagoma colore=0 riquadro=0x0+0+0
ok barra-esaurita: verde nella barra colore=0 riquadro=0x0+0+0; rosso colore=0 riquadro=0x0+0+0; grigio colore=248 riquadro=42x16+50+482
ok scorta: pixel=228 riquadro=24x16+152+482; grigio prima colore=112 riquadro=22x14+152+482; bianco dopo colore=84 riquadro=22x14+152+482
ok solo-barra: pixel=9380 riquadro=134x70+46+466; sagoma pixel=0 riquadro=0x0+0+0
ok terza-persona: pixel=12320 riquadro=176x70+4+466; angoli pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0
ok mancino-gruppo: pixel=12320 riquadro=176x70+780+466; sagoma pixel=2940 riquadro=42x70+914+466; percentuale colore=244 riquadro=46x14+784+482
ok mancino-sagoma: pixel=1024 riquadro=32x60+920+472; giallo colore=424 riquadro=28x56+922+474
ok mancino-solo-barra: pixel=9380 riquadro=134x70+780+466; sagoma pixel=0 riquadro=0x0+0+0
ok riavvio: pixel=0 riquadro=0x0+0+0; pixel=12320 riquadro=176x70+4+466
OK 1.21.1-neoforge: scatti in scratch/neo
exit=0
```

```
$ M4C_SOLO_CONTROLLI=1 bash tools/client/m4c_shots.sh 1.20.1-forge scratch/forge
ok chat-e-tasti: True False
ok riavvio-file: False False
ok gruppo: pixel=12320 riquadro=176x70+4+466; sagoma pixel=2940 riquadro=42x70+4+466
ok testo: colore=244 riquadro=46x14+50+482
ok barra-normale: colore=1220 riquadro=122x10+52+510
ok sagoma: pixel=1024 riquadro=32x60+10+472; verde prima colore=424 riquadro=28x56+12+474; giallo dopo colore=424 riquadro=28x56+12+474; verde dopo colore=0 riquadro=0x0+0+0
ok critico: rosso colore=424 riquadro=28x56+12+474; giallo colore=0 riquadro=0x0+0+0
ok barra-bassa: pixel=1992 riquadro=124x38+50+482; rosso nella barra colore=60 riquadro=6x10+52+510; verde nella barra colore=0 riquadro=0x0+0+0; rosso nella sagoma colore=0 riquadro=0x0+0+0
ok barra-esaurita: verde nella barra colore=0 riquadro=0x0+0+0; rosso colore=0 riquadro=0x0+0+0; grigio colore=248 riquadro=42x16+50+482
ok scorta: pixel=228 riquadro=24x16+152+482; grigio prima colore=112 riquadro=22x14+152+482; bianco dopo colore=84 riquadro=22x14+152+482
ok solo-barra: pixel=9380 riquadro=134x70+46+466; sagoma pixel=0 riquadro=0x0+0+0
ok terza-persona: pixel=12320 riquadro=176x70+4+466; angoli pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0
ok mancino-gruppo: pixel=12320 riquadro=176x70+780+466; sagoma pixel=2940 riquadro=42x70+914+466; percentuale colore=244 riquadro=46x14+784+482
ok mancino-sagoma: pixel=1024 riquadro=32x60+920+472; giallo colore=424 riquadro=28x56+922+474
ok mancino-solo-barra: pixel=9380 riquadro=134x70+780+466; sagoma pixel=0 riquadro=0x0+0+0
ok riavvio: pixel=0 riquadro=0x0+0+0; pixel=12320 riquadro=176x70+4+466
OK 1.20.1-forge: scatti in scratch/forge
exit=0
```

Negative proof: scratch/neg-avviso is a copy of scratch/neo in which `esaurita.png` has the warning's `16x16+50+482`
painted over with the plate colour sampled just above it, at (50, 480): `srgb(46,59,35)`
(`convert … -fill "srgb(46,59,35)" -draw 'rectangle 50,482 65,497'`). The copy is not committed. Crop:
scratch/neg-avviso-crop.png ("0%" without the warning).

```
$ bash tools/client/arm_measure.sh colore neo/esaurita.png 8A8A8A 58x18+50+482
colore=248 riquadro=42x16+50+482
$ bash tools/client/arm_measure.sh colore neg-avviso/esaurita.png 8A8A8A 58x18+50+482
colore=120 riquadro=22x14+70+482
$ bash tools/client/arm_measure.sh colore neg-avviso/esaurita.png 8A8A8A 16x16+50+482
colore=0 riquadro=0x0+0+0
$ M4C_SOLO_CONTROLLI=1 bash tools/client/m4c_shots.sh 1.21.1-neoforge scratch/neg-avviso
KO barra-esaurita: verde nella barra colore=0 riquadro=0x0+0+0; rosso colore=0 riquadro=0x0+0+0; grigio colore=120 riquadro=22x14+70+482 (atteso verde nella barra 0 (vuota); rosso nell'energia 0 (non e' LOW); grigio >= 120 con riquadro=42x16+50+482 (l'avviso fisso e la percentuale))
FAIL 1.21.1-neoforge: controlli falliti: barra-esaurita
exit=1
```

The other fifteen checks print `ok` on the copy (15 ok, 1 KO). Without the box the grey count (120 >= 120) would still
have passed: the box is what catches the missing warning.
