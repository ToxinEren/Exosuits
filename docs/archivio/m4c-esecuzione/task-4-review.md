### Spec Compliance
- ✅ Spec compliant. Every step of the brief is implemented, and the diff touches exactly the eight files in the brief's Files list: the new PNG plus the seven modified files.
  - Step 1:
    - `tools/gen_placeholders.py:23` adds `GUI_DIR`.
    - `:506-538` adds `draw_hud_vignette()`. The corner pixels are (0,0), (143,0), (0,79), (143,79); the formula is the brief's; the glints are at `8+off+t` and `w-9-off-t` = `135-off-t`.
    - `:553` adds the `outputs()` row.
    - `:2-4` adds the vignette to the docstring.
  - Step 2:
    - `SuitHud.java:62` adds `VIGNETTE_TEXTURE = Ids.of(...)`.
    - `:158-161`: the vignette is drawn first, after the early returns (NONE/F1/H/no player/spectator), gated on `FULL`, `isFirstPerson()` and `ClientPrefs.vignette()`.
    - `:188-203`: `enableBlend` and `defaultBlendFunc`, then the four `blit(..., 72, 40, 144, 80)` calls in a `try`, with (u,v) = (i%2*72, i/2*40), i.e. (0,0),(72,0),(0,40),(72,40) in `vignetteCorners` order, and `disableBlend` in a `finally`. The outer `try` at `:168` remains (Review Focus 4).
  - Step 3, in `tools/client/m4c_shots.sh`:
    - `:263` adds the "effetti" phase, and `:287` adds the shot to `shots_a`.
    - `:216` sets `VIGNETTE_MIN=320`. The smallest corner measured 653, and half of 653, rounded down to the ten, is 320.
    - `:348-361` adds `from_corner`, and `:472-485` adds the `vignetta` check exactly as in the plan's row at `contesto-comune.md:207`.
    - `:84-108` updates the header: what `solo-barra`, `terza-persona`, `mancino-solo-barra` and `riavvio` now prove.
    - The `want` texts of `solo-barra`, `terza-persona` (`:468`) and `riavvio` (`:518`) are updated.
    - The check count is 17.
  - Step 4: `tools/client/m3d_shots.sh:16-27` adds the vignette paragraph and the `solo-tuta` note. The changed measured values are updated at `:83` (third person 23147-23745) and `:126-129` (map 23629). No threshold changed.
  - Step 5:
    - `tools/README.md:59` adds the texture row, and the text now says "ventiquattro" in both places.
    - `tools/client/README.md:130` covers the vignette and the `solo-tuta` note in the m3d paragraph, and `:183` adds the `vignetta` row.
    - `docs/compat.md:248` updates the `SuitHud` entry in "Avvio del client".
  - Step 7: the commit subject matches the brief verbatim.
- ✅ The plan's row is met, but the row itself omits the colour clause of the Global Constraint at `contesto-comune.md:47`. That is reported below as Important 1, plan-mandated: the implementer followed the row.
- ✅ Coordinator rulings:
  - R2: the formula is repeated in the generator, and the comment says so at `gen_placeholders.py:506-510`.
  - R5: the bottom corners' alpha in the hotbar-band columns is 1/255.
  - R11: the `riavvio` check compares `nascosto` with `riavvio-nascosto`, expected 0, unchanged.
  - R13: `barra-esaurita` still pins `42x16+50+482`.
  - R14: the correction is right on its merits (javap check below).
- ⚠️ Cannot verify from diff (R14 follow-up): design §5.12 "Vignetta" (`docs/superpowers/specs/2026-11-19-m4-stazione-design.md:291`) and contesto-comune "Fatti del gioco" (`contesto-comune.md:74`) still list toasts ("avvisi") among the elements whose stacking differs by version. They now contradict the correct Javadoc at `SuitHud.java:184-186` and `docs/compat.md:248`. The coordinator should amend them or record R14 there before the Task 5 checklist is written.
- ⚠️ Cannot verify from diff (Task 5): the implementer's concern #2 is confirmed: on 1.20.1-forge the saving indicator is never drawn (check below).
  - Effect on this diff: the clause "l'indicatore del salvataggio … su 1.20.1 sotto" at `SuitHud.java:182-184` and `docs/compat.md:248` holds on 1.20.1-fabric but says nothing true on 1.20.1-forge.
  - Effect on Task 5: the checklist must not ask to see the indicator above or below the vignette or group on 1.20.1-forge.
- ⚠️ Cannot verify from diff (outside the Files list): the header comment of the placeholder section of `tools/check_art.sh` still lists only some texture families. It omitted the station before, and now it also omits the vignette. The check itself compares every generator output (`OK art`). Leave it for the whole-branch review.
- Check: the line numbers cited here come from the 7dcdb1e checkout (`git rev-parse --short HEAD` → `7dcdb1e`, working tree clean), taken with `grep -n`/`sed -n` on the changed files. I did not open any changed file beyond the diff.

### Strengths
- The texture is exactly the brief's formula. Check: an independent recomputation of the formula over the PNG's raw RGBA found 0 differing pixels out of 11520. It also found:
  - 7596 pixels with alpha > 0, as the report says;
  - no white pixel in the bottom half (glints only in the top quarters);
  - an exact left/right alpha mirror;
  - max alpha 107.
- Check: `convert … -format '%w %h %[fx:…p{0,0}…] %[fx:…p{143,79}…] %[fx:…p{71,39}…]' info:` → `144 80 107 107 0`, the Step 1 expected value.
- Check: `python3 -I tools/gen_placeholders.py --out <scratch>` wrote 24 files. The vignette is byte-identical to the committed PNG (`cmp`).
- R5 check: the alpha maximum in texture columns 65-71 and 72-78, rows 40-79, is 1/255 (0.0039, under 0.05), in both ImageMagick and Python.
- The drawing has the right shape. The blend is on only around the four `blit` calls and is turned off in a `finally` inside the outer `RuntimeException` catch, as the brief and Review Focus 4 require (`SuitHud.java:188-203`). There are no pose-stack changes, no scaling and no Stonecutter directives. Check: `build-step2.log` shows `compileJava` and `test` executed on all four variants, with no javac warnings.
- The `vignetta` check discriminates where and in what shape. The box must touch both screen edges of its corner, and nothing may change outside the corners. The implementer's dry runs show three KO cases: an unmirrored corner (`X+L=914`), 100 extra pixels in the centre, and no vignette at all.
- Task 3 checks now cover the vignette's absence where the preference is on:
  - `solo-barra` and `mancino-solo-barra` (exact energy box);
  - `terza-persona` (three corners at 0);
  - `riavvio` (exact group box, so the off preference survived the restart).
- R14 check (risk: the new Javadoc and compat text about toasts on 1.20.1 could be false): javap of `GameRenderer.render(float,long,boolean)` in vanilla 1.20.1 and Forge 47.4.29 gives the order `Gui.render` (bytecode offset 610/613), `RenderSystem.clear` (619/622), the screen (747/750), then `ToastComponent.render` (928/931). So toasts are drawn after the HUD hook on 1.20.1 too.
- Saving-indicator check (risk: the Javadoc clause at `SuitHud.java:182-184` is vacuous on Forge):
  - In the Forge 47.4.29 merged jar, a raw grep of `net/minecraftforge/client/**` and `Gui*.class` finds `renderSavingIndicator` only in `Gui.class`. It is private there, and only vanilla `Gui.render` calls it.
  - `javap -c -p ForgeGui` has no `invokespecial Gui.render`. Its only `invokespecial` calls into `Gui` are `clearCache`, `renderExperienceBar`, `renderJumpMeter` and `renderPortalOverlay`.
- Robustness check (risk: the new `sum=$((…))` at `m4c_shots.sh:477` aborts under `set -u` when a measure fails): `measure_lib.sh:22-58` shows `val` returns `-1` for a missing field and `verdict` forces KO on `misura-non-riuscita`. So the check prints KO and the script goes on.
- Check: `HudLayoutTest.java:235` pins all four corner strings, including the new script constant `CORNER_BOTTOM_LEFT=144x80+0+460` (`m4c_shots.sh:175`).
- Evidence checks:
  - `neo.out`, `neo-recheck.out` and `forge.out`: 17 ok, 0 KO, plus the `OK <variante>` line.
  - `m3d-neo.out` and `m3d-forge.out`: 14 ok, 0 KO.
  - The `*-final-recheck.out` files are identical to the run outputs.
  - `log-check.txt` is 0 bytes in all 10 folders.
  - `final-checks.log`: BUILD SUCCESSFUL, 381 ×4, 0 failed suites, the six `OK` lines, four `OK … 235`, exit 0.
- Check of the m3d values now written into the header and README:
  - From `m3d-*.out`: `mappa` 23629 on both variants; `terza-persona` 23147 (neo) and 23166 (forge); `solo-tuta uguali=0`.
  - `arm_measure.sh differenza base-prima base-senza 144x80+816+460` on both m3d folders → `pixel=762 riquadro=22x68+816+472`.
  - Texture alpha maximum in `11x34+72+46` (that box in texture coordinates) → 2/255.
- Visual check on my own crops:
  - neo `pieno.png`: four dark corners from the screen corners, two glints per top corner, the group drawn over the bottom-left corner, the arm darkened in the bottom right.
  - `vignetta-effetti.png`, top-right crop: on 1.21.1 the luck icon is crisp above the glint; on 1.20.1 the glint crosses the icon frame and the icon is darker.
  - No chat line appears in either version, which matters because Task 5 will use this shot as a reference.

### Issues
#### Critical (Must Fix)
None.

#### Important (Should Fix)
1. **Plan-mandated.** `tools/client/m4c_shots.sh:472-485`: the `vignetta` check asserts no colour or translucency, so an opaque vignette with the blend off passes.
   - What the constraint asks: Global Constraint `contesto-comune.md:47` asks every check to discard the wrong-but-plausible image by where, shape *and* colour.
   - Why it is plan-mandated: the plan's row (`contesto-comune.md:207`) specifies only counts, corner attachment and the full-screen sum, and the implementer followed it.
   - Check I ran: I pasted the texture ×2 with every alpha > 0 forced opaque onto neo `senza-vignetta.png`. In the top-left corner it gives `pixel=7596 riquadro=132x74+0+0`, so it would pass every corner criterion of today's check. The same holds for the other corners (top right `132x74+828+0`, bottom right `132x74+828+466`).
   - Why it matters:
     - The translucent darkening is the one property that this task's `enableBlend`/`defaultBlendFunc` code exists to produce.
     - Nothing automated asserts it. The script, the JUnits and `check_art.sh` (which pins only the texture) all miss it.
     - The blend state on entry to the hook depends on what the game drew before it, so a regression could look right on one version and opaque on the other.
   - Fix: compare the tint colour per corner. Measured with `arm_measure.sh colore <shot> 0A0C0E <corner>` on the existing shots:
     - On neo and forge, `pieno` and `senza-vignetta` both give 0, 0, 400 and 0 (top left, top right, bottom left, bottom right).
     - The 400 in the bottom-left corner are the bar frame (`BAR_EDGE` 0C0D0F, `94x16+50+506`), opaque and drawn above the vignette.
     - The opaque synthetic image gives 7388, 7388 and 7596 in the top-left, top-right and bottom-right corners.
   - So require, in each corner, the `pieno` count to equal the `senza-vignetta` count (or 0 in the top-left, top-right and bottom-right corners). Saying "= 0 in all four corners" would give a false KO on correct shots.
   - After the fix: add the assertion to `want` and to the header and README description of `vignetta`, then re-run `M4C_SOLO_CONTROLLI=1` on the two existing shot folders. No client run is needed.

#### Minor (Nice to Have)
1. `tools/client/m4c_shots.sh:119`: a 44-character line in the middle of the reflowed "Non prova" paragraph ("# sotto il gruppo; F1, F3, F3+H, F3+T; H con"). Lines 114-118 were rewrapped, but 119-122 were not merged into them. This is cosmetic; reflow lines 118-122 to 120 columns.

### Assessment
**Task quality:** Needs fixes
**Reasoning:** The texture, the drawing with the blend closed in a `finally`, the updated Task 3 checks and the docs match the brief and the rulings, and independent recomputation, javap and the clean run outputs confirm them. But the new `vignetta` check, as the plan's row defines it, accepts an opaque vignette, which breaks the Global Constraint's colour clause. Adding a per-corner tint-colour comparison fixes it, and needs only an `M4C_SOLO_CONTROLLI=1` recheck on the existing shots.
