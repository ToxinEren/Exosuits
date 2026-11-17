# Task 1 review - Il braccio della tuta al posto di quello del gioco (78e15a0..3bfc80c)

### Spec Compliance

- ✅ Spec compliant. Every file on the brief's list has its hunk, and nothing on the "Produces" list is missing, extra or misread. File by file:
  - `compat/client/SuitBoneRules.java:115-132` `visibleForArm`: top-level and frame bone of that arm, PIECE bones only when `entry.slot() == arm && visible(...)`, HIDDEN false; NPE for null `view`/`arm`, `IllegalArgumentException("not an arm slot: " + arm)` (line 120), IAE for a bone outside `BONES`; class Javadoc sentence at 20-23.
  - `compat/client/SuitRenderer.java:34-45` `renderArm`: signature of the plan header, contract in the Javadoc (same pose/space/entity, bones from `visibleForArm`, IAE before touching anything, nothing left set).
  - `compat/impl/client/SuitRendererImpl.java:57,63-104,113-120`: `render` and `renderArm` go through one private `draw`; `this.arm = arm` next to `this.view = ...` (84-85), `this.arm = null` next to `this.view = BARE` in the `finally` (95-96); the loop runs over every name of `BONES` in both modes (117-120). No new Stonecutter directive (0 added `//?` lines).
  - `client/FirstPersonArm.java`: nine constants with the reason of each number, five pure functions, `onHandPrepared` (157-186) is the brief's body line for line; `public final`, private constructor, no static field of a Minecraft type, only `onHandPrepared` public.
  - `mixin/client/PlayerRendererMixin.java:57-70`: third handler exactly as in the brief (INVOKE on `PlayerModel.setupAnim`, `shift = AFTER`, `cancellable`, one call); class Javadoc 15-37 has the third hook, why after `setupAnim`, and the javap verification.
  - `client/PilotPose.java:30-31`: the one Javadoc sentence, verbatim. `PilotPoseTest.java`: only `GEO` and `handCubes` lose `private`.
  - `SuitBoneRulesTest.java`: the six tests of Step 1 with the named helpers; `FirstPersonArmTest.java`: the six tests of Step 4 (candidates, tolerances and literal values as in the brief). 6 + 6 new tests = 331.
  - `tools/check_jars.sh:123-124,142-170`: `renderHand(` and `setupAnim(` in `MOJANG`; `client.PlayerRendererMixin` in `TARGETS` for the four pairs with the brief's names; head comment updated.
  - `docs/compat.md`: all six points (renderArm in "A cosa serve", the Uso line, `visibleForArm`, three hooks with the third in place of the sentence "La prima persona ... non cambia.", `FirstPersonArm` in "Disegno del giocatore", the `client/` class entry). No sentence left that says the first person is unchanged (grep of every "prima persona" occurrence in the file).
  - `tools/client/arm_measure.sh` (mode 100755): `sagoma`, `magenta`, `uguali`, `--selftest`, the plan's formulas, thresholds and rectangles; selftest images and the seven expected outputs are the brief's, character for character.
- ⚠️ Cannot verify from the repository:
  - Whether the palm of the suit lands on the centre of the base hand on screen. JUnit proves the arithmetic in the arm's system and the stitching is the brief's code; the shots show the arm in the lower right at full size, not the exact place. Task 2 Step 1 measures it.
  - Whether the "Chat messages can't be verified" notice is gone at the first measured shot of Task 2's sequence. I only know it is fully on screen 9.4 s and 11.6 s after the join (see T1-F1).
  - Review Focus 1 at run time (whole suit after the arm in the same frame): only the shape of the code can be read here, as the plan says; Task 2's `terza-persona`, `inventario`, `telaio-piazzato` checks prove it.
  - Review Focus 5 (an exception inside the hand draw): no run provokes it; shape of the code only (below).
  - Production jars on a real PC (Task 3). The four jars do carry the remapped names (below).

### Strengths

- Review Focus 1 is argued by more than the loop: the geometry has 86 bones and `BONES` has 86 (6 + 6 + 72 + 2), all six top-level bones are in `BONES`, so no bone escapes the per-draw decision in either mode; `log-check.txt` is empty on the four runs, so no missing-bone WARN. `renderArm` rejects the slot before any state is written (`SuitRendererImpl.java:72-76`), and `draw` writes `this.arm` only after the three null checks (81-85).
- `onHandPrepared` keeps the caller's `PoseStack` untouched (private copy, 170-172), catches `RuntimeException | LinkageError`, logs once and returns false so the base arm is drawn (182-185); `model.young = false` precedes the draw (168).
- `armDrawShowsNothingTheWholeDrawHides` pins the design's "never a bone more than the third person" over three views, both arms and all 86 bones; `suitPalmIsTheCentreOfThePalmCubeOfTheGeneratedGeometry` reads the generated geometry instead of repeating the constant.
- `arm_measure.sh` handles the two cases where ImageMagick's `%@` lies (mask touching the edges, all-white mask) with a one-pixel black frame, and documents its exit codes.
- The report's evidence is reproducible: every number I rechecked matched to the pixel.

### Checks I ran (each for a named risk; the diff was read in passes because of its size)

- Risk: a geometry bone outside `BONES` drawn by the arm-only draw. Parsed `src/main/resources/assets/redstone_exoframes/geo/exoframe.geo.json`: 86 bones, top level exactly the six `armor*`, the arm pieces and `frame*Arm` are children of `armor*Arm`.
- Risk: `setVisible` not reaching children. Read `SuitRendererImpl.java:138-146`: `GeoBone.setHidden(!visible)`; `BONES` lists containers first (existing test).
- Risk: `SuitLayer.suitView` filled only by the third-person layer (arm missing until F5). Read `client/SuitLayer.java:66-99`: it decodes on demand, cached per stack, never throws.
- Risk: the hook not remapped in a production jar (Review Focus 4). `strings` on `PlayerRendererMixin.class` of the four jars in `build/libs/all` (built 10:37-10:40; nothing under `src/` is newer than them): Fabric 1.20.1 and 1.21.1 `method_23205(` and `class_591;method_17087(`; Forge `m_117775_(` and `PlayerModel;m_6973_(`; NeoForge `renderHand(` and `PlayerModel;setupAnim(`. `FirstPersonArm.class` is in each jar.
- Risk: the new `MOJANG` markers weakening (e2). Read `tools/check_jars.sh:199-229`: markers only forbid un-remapped names on Fabric/Forge and require one on NeoForge; `TARGETS` adds exact names. No weakening.
- Risk: Step 9 claims (Review Focus 2, 3, 9) resting on the report alone. Shots still at `<area-temporanea>/scratchpad/shot-<variante>/`. `arm_measure.sh magenta` on the eight shots: `braccio.png` 57297 / 57261 / 57084 / 57401 px (forge, fabric 1.20.1, fabric 1.21.1, neoforge), box `457x224+503+316` on all four (full size on GeckoLib 4.8.4 and 4.9.3, in a freshly started client: `braccio.png` is the first shot of each run, written before `base.png` and before the tool's own three closing shots, and the step list of Step 9 has no key step); `base.png` `magenta=0 riquadro=0x0+0+0` on all four. Looked at `braccio.png` of 1.21.1-neoforge (magenta plates, steel bar, brass, large, lower right) and `base.png` of 1.20.1-fabric (the bare arm). `log-check.txt` 0 bytes on the four; no `Suit rendering failed`, `InvalidInjection` or `Mixin apply` in the four `client.log`.
- Risk: closing checks run on other code than the commit. Timeline from file times: JUnit results and jars 10:37-10:40, game tests 10:42-10:43 (`All 208 required tests passed` in the four `versions/*/build/gametest/server.log`), client shots 10:44-10:48, commit 6ddc8b9 10:49; the only later change is the two lines of `docs/compat.md` in 3bfc80c.
- Constraints: both commits carry a `gpgsig` header like the base commit; no added `LOGGER` line; added Java text is ASCII except "§" in two Javadocs, which `SuitBoneRules` already used.
- Not re-run: Gradle build, JUnit, game tests, `check_sources.sh`, `check_jars.sh`, the selftest of `arm_measure.sh`.

### The implementer's four concerns

1. "Chat messages can't be verified" notice still on screen 11 s after the join: confirmed, real risk for Task 2, no defect of Task 1 (the notice has no magenta pixel: `base.png` gives `magenta=0`). Raised as T1-F1 for a ruling before Task 2 starts.
2. Arm behind the hotbar at K 1.65 with no shift: no finding. Reproduced (`magenta braccio.png 380x100+290+440` = 9137 / 9101 / 8924 / 9241, box `167x100+503+440`). It is the plan's prediction; size and place belong to Task 2 and the hotbar ruling.
3. Second commit 3bfc80c outside the brief's text: no finding. Two lines of `docs/compat.md`, a file on the task's list; the four names it adds are the ones I read in the jars and the ones (e2) pins; the "Verifica" sentence describes the Step 9 runs as they were. Leaving the "Nessun refmap" list incomplete would have been the defect.
4. Stale Javadoc of `SuitLayer.suitView`: confirmed, T1-F2.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T1-F1 (plan-mandated: for the coordinator, before Task 2; not for the implementer).** The plan's rules for measured shots (plan line 87, "Scatti misurati: senza righe di chat e senza avvisi") and design §13 "Prove" cover the `gamemode` chat line and the recipe toast, not the "Chat messages can't be verified" notice the client shows after joining the test server (`online-mode=false`). Measured on `shot-1.21.1-neoforge`: join 10:44:07.4, `braccio.png` 10:44:16.8 (9.4 s later), `base.png` 10:44:19.0 (11.6 s later), notice fully shown in both; also in `base.png` of 1.20.1-fabric. Its dark fill alone is `448x100+506+6` (39 636 px); with the frame about `460x110+500+0`, about 50 600 px, 9.8 % of 518 400. If it is in only one shot of a `sagoma` or `uguali` pair: `copertura` rises by up to about 9.8 points (enough to lift a half-size arm over the 3 % floor of Task 2's `criteri` check, Review Focus 2, or to push a good candidate over 14 %), `pixel` and `sagoma=` rise by the same pixels, and `riquadro` starts at Y 0, out of the lower-right quarter (criterion (a)). The three rectangles are not touched. Options: a longer first wait in `m3d_shots.sh` with the measured duration next to it; or a guard that fails a measured pair when the region `460x110+500+0` differs between the two shots.
- **T1-F2 (plan-mandated: file not on the task's list).** `src/main/java/com/toxineren/exoframes/client/SuitLayer.java:66-67`: the Javadoc of `suitView` says "Per {@link PilotPose}, che non decodifica il riassunto una seconda volta"; `FirstPersonArm.onHandPrepared` (`FirstPersonArm.java:159`) now calls it too, and its `null` is what leaves the arm to the base game. The brief lists `SuitLayer` under "Consumes" only, so the implementer was right not to touch it. One sentence; the coordinator decides which task carries it.
- **T1-F3.** `src/main/java/com/toxineren/exoframes/compat/client/SuitRenderer.java:10-13`: the class Javadoc still says the seam is used by the worn suit and the placed frame and is called "dentro il disegno dell'entita' (per esempio da un RenderLayer o da un EntityRenderer)"; `renderArm` (34-45, added by this task in this file) is called from the first-person hand draw, outside any entity draw. One sentence naming the third use.

### Assessment

**Task quality:** Approved

**Reasoning:** The diff is the brief's design built as written, with the visibility decided per draw for all 86 bones and the hand draw unable to throw into the frame; the jars carry the remapped targets and the eight first-view shots reproduce the reported measures. Only three Minor items remain, one of which (the notice in measured shots) needs a coordinator ruling before Task 2 runs.
