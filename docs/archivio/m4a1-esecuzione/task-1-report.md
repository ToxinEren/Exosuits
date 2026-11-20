# Task 1 — report (la prova delle due cuciture, con il pulsante 102)

**Status: DONE_WITH_CONCERNS.** The gate passes on all four variants. The concern is the preview's lighting on 1.20.1 (see "Concerns"); it is a visual difference, not a gate condition.

**Update:** the concern was resolved by ruling P11 in commit `99e4605`; see "Follow-up (ruling P11)" at the end. The final status is DONE.

Commit: `45722f9 client: prova delle cuciture della stazione (schermata registrata, anteprima, pulsante 102 al server e ritorno); tools: passi @punta @clic @trascina, m4a1_shots.sh` on `m4-stazione`, SSH-signed by the environment (not pushed).

All evidence files are under `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t1/` (called `$S` below): `$S/evidence/*.txt` holds the command outputs, and `$S/<variante>/` and `$S/m3d-<variante>/` hold the run folders.

## What was implemented

- **Seams (the two per-version bases, both in this task as ruled).**
  - `compat/CompatBlock` sits in front of `compat/impl/BlockBase extends Block`, which carries `onClick` plus `use` (1.20.1) or `useItemOn` (1.21.1) with the translation table. The code and Javadoc were moved from `EntityBlockBase`, with the same directives.
  - `EntityBlockBase` now `extends BlockBase implements EntityBlock`, with no copy of the click and no directives left.
  - The Javadoc of `CompatEntityBlock` now says who translates the click.
  - `compat/client/CompatContainerScreen<T>` sits in front of `compat/impl/client/ContainerScreenBase<T> extends AbstractContainerScreen<T>`. It has a `protected renderContent(...)` (empty by default) and a `public final render(...)`: `//? if <1.21` `renderBackground(graphics)`, then `super.render`, `renderContent`, `renderTooltip`.
- **Common code.**
  - `content/StationBlock extends CompatBlock` is minimal, with no state properties. `onClick`:
    - returns PASS for the off hand, a spectator or a player who is not alive;
    - otherwise returns SUCCESS, and on the server first calls `StationMenu.open(player, new StationOpening(pos))`.
  - `content/StationItem extends CompatBlockItem` is empty.
  - `ModBlocks.STATION` uses the properties from the plan. `ModItems.STATION` is created with `new Item.Properties()`. The creative tab lists the station after the charger.
  - `content/ModMenus`: `MENUS` and `STATION` = `MenuRegistry.ofExtended(StationMenu::new)`. `register()` is called after `ModBlockEntities.register()`.
  - `station/StationOpening(BlockPos main)` is a record with `write`/`read` (`writeBlockPos`/`readBlockPos`); `main` is null-checked and made `immutable()`.
  - `station/StationMenu`:
    - two public constructors (client: reads the buffer immediately; server: `ContainerLevelAccess.create`), sharing one private constructor, so the single `DataSlot.standalone()` is added in the same order;
    - `provider`, `open`, `stationPos()`, `testPresses()`, `TEST_BUTTON = 102`, `TITLE_KEY`;
    - `clickMenuButton`: true and +1 only for 102 with a living player;
    - `quickMoveStack` → `EMPTY`;
    - `stillValid`: always true on the client (no level). On the server it requires `player.level() == level`, the static vanilla helper with the station block, a living player and not a spectator. It runs inside `try`; a `RuntimeException` logs `Station menu check failed for player {}` once per menu and returns false.
- **Client.**
  - `client/StationLayout` (package-private, pure Java):
    - `WIDTH`, `HEIGHT`, the rectangles `TITLE`, `PREVIEW_PANEL`, `PREVIEW_CLIP`, `TEST_BUTTON`, and a `Rect` record with `contains`, `width` and `height`;
    - `leftPos`, `topPos`, and the seven Task-1 colours (the proposed values);
    - plus `lighter(argb)` for the hover state (see "Self-review").
  - `client/SuitPreview` (package-private): `frameSpace`, the four steps moved verbatim from `ExoframeEntityRenderer`, which now calls it. `drawInGui` follows the plan's order exactly, with pose, scissor and light restored in a `finally` and `flush()` inside the scissor.
  - `client/StationScreen` (minimal):
    - `renderBg`: opaque fill with a 1 px bronze border, panel, preview background, preview with `FIXED_VIEW` (the right arm is FF00FF), then the test button (grey or green, lighter on hover, with the confirmed count drawn in it);
    - `renderLabels`: the title only;
    - `renderContent`: the tooltip `gui.redstone_exoframes.station.test_button`;
    - `mouseClicked`: a left click on the button calls only `handleInventoryButtonClick(containerId, 102)` and returns true; anything else goes to `super`;
    - the `HumanoidModel` is baked in `init()` with `young = false`.
  - The registration in `RedstoneExoframesClient.init()` comes after `CellSwapKey.register()`: `ModMenus.STATION.listen(type -> MenuRegistry.registerScreenFactory(type, StationScreen::new));`, with a two-line comment.
- **Tests and helpers.**
  - `gametest/StationMenuGameTests` has `placeholderButtonCountsOnTheServer` and `placeholderButtonIgnoresSpectatorsAndTheDead`. It is registered after `SurvivalGameTests`. Cleanup (close the menu if open, remove the station) runs in `finally`.
  - `GameTestKit` gains `STATION_MENU_ID = 100`, `openStation` (with the Javadoc on why the menu is not opened through the network) and `pressButton` (the vanilla `handleContainerButtonClick`).
- **Assets.**
  - Blockstate `station.json` (variant `""`); models `block/station_main.json` (parent `orientable`) and `item/station.json` (parent `item/generated`).
  - Four textures from `tools/gen_placeholders.py`: `station_top`, `station_side`, `station_front_main` (opaque via `solid()`) and `item/station.png` (a flat icon: a bench with a small frame standing on it, in the charger and cell palette).
  - Language keys in both files at the positions the plan gives.
- **Tools.**
  - `client_shot.sh`: `--selftest`; `EXO_SHOT_W`/`EXO_SHOT_H` read and checked before the step loop; coordinate checks; `EXO_SHOT_STEPS_ONLY=true` exits after the loop with `OK passi: <N>`.
  - `client_shot.sh` steps: `@punta`, `@clic` (`maiuscole` uses `held_key=Shift_L`), and `@trascina` (`held_button`; `cleanup` releases it first).
  - `client_shot.sh` text: the header comment and the unknown-step message list the new steps.
  - `tools/client/m4a1_shots.sh` (mode 755): the Task 1 phases, the checks `anteprima`, `orientamento` and `pulsante`, and `M4A1_SOLO_CONTROLLI=1`.
- **Docs.**
  - `docs/compat.md`:
    - new sections for `CompatBlock` (which now holds the click table) and `CompatContainerScreen`;
    - `CompatEntityBlock` refers to `CompatBlock`;
    - a new "Menu e schermate" section;
    - `SuitPreview` and the screen registration added under "Avvio del client";
    - the API table rows for menu extra data, `Block.use`, and `renderBackground`/`renderEntityInInventory`;
    - a "Limiti noti" entry for the 1.20.1 lighting;
    - `station/` added to the intro.
    - `grep -c CompatContainerScreen docs/compat.md` → 4.
  - `tools/client/README.md`: the step table rows (with the note on the free pointer and the formula for GUI points), `EXO_SHOT_STEPS_ONLY`, `--selftest`, the `m4a1_shots.sh` usage line and section (checks, measures), the two rewritten "Cosa non copre" items, and the durations.
  - `tools/README.md`: `m4a1_shots.sh` added, the four textures listed, and "ventidue" in both places.

## TDD evidence

- **Step 1, RED.** `bash tools/client/client_shot.sh --selftest; echo $?` (`$S/evidence/step1-red.txt`) gives exit 1.
  - The seven new good cases fail with `ERRORE: passo sconosciuto "@punta 0 0" (passi: @click, @tieni, @scatto, @attendi; ...)` and so on.
  - The four old good cases fail because, without `EXO_SHOT_STEPS_ONLY`, they reach the EULA check (exit 2; nothing starts, since the selftest unsets `EXO_ACCEPT_EULA`).
  - This is expected: the steps and the steps-only mode did not exist yet.
- **Step 2, GREEN.** `bash tools/client/client_shot.sh --selftest` → `OK passi`, exit 0. `bash -n` prints nothing.
  - Extra manual checks: `EXO_SHOT_W=100` with `@punta 100 2` → exit 2 with the step in the message; `EXO_SHOT_W=abc` → exit 2; `@punta 010 2` → accepted (base 10); a multi-step run → `OK passi: 3` without creating the folder.
- **Step 3, RED.** `./gradlew --console=plain :1.21.1-neoforge:compileJava` (`$S/evidence/step3-red-compile.txt`) → `BUILD FAILED`:
  - `StationMenuGameTests.java:7: error: package com.toxineren.exoframes.station does not exist`;
  - `cannot find symbol`: class `StationMenu`, method `openStation(GameTestHelper,ServerPlayer,BlockPos)`, variable `STATION`.
  - This is expected: the menu and the helpers did not exist yet.
- **GREEN.** Steps 4-6 build on all four variants, and `run_gametests.sh` gives 210 on each (see below). The two new tests are included, and the 27 charger tests pass with the moved click translation.

## End-of-task verification (Step 11, run after the last Java edit)

- `./gradlew --console=plain :1.20.1-forge:build ... :1.21.1-neoforge:collectJar` → `BUILD SUCCESSFUL in 41s` (`$S/evidence/final-build.txt`; compile and test tasks re-ran for all four).
- JUnit per variant (fresh XML): `1.20.1-forge 331`, `1.20.1-fabric 331`, `1.21.1-fabric 331`, `1.21.1-neoforge 331`.
- `OK sources`, `OK jars`, `OK data`, `OK art`, `OK arm_measure`, `OK passi` (`$S/evidence/final-checks.txt`, all exit 0).
- `bash tools/run_gametests.sh` (`$S/evidence/final-gametests.txt`), exit 0:
  - `OK 1.20.1-forge: 210 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.20.1-fabric: 210 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-fabric: 210 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-neoforge: 210 test obbligatori passati, nessun avviso fuori elenco`

## Step 8: m3d_shots regression (the transform extracted from ExoframeEntityRenderer)

- 1.21.1-neoforge (`$S/m3d-1.21.1-neoforge`): 14 `ok` lines, `OK 1.21.1-neoforge: scatti in ...`, exit 0, 261 s. Both `log-check.txt` files are empty.
- 1.20.1-forge (`$S/m3d-1.20.1-forge`): 14 `ok` lines, `OK 1.20.1-forge: scatti in ...`, exit 0, 243 s. Both `log-check.txt` files are empty.
- The measures are identical to the documented ones. For example, `telaio-piazzato: ... riquadro=258x284+351+113; magenta=39962 ..., il 100 % di braccio-rottame`, and `terza-persona` is 318 px wide.
- No `station`, model or texture warnings appear in `client.log` or `server.log`.

## Step 9: the gate (m4a1_shots.sh), in the order neoforge, forge, fabric 1.20.1, fabric 1.21.1

Final thresholds: `MAGENTA_MIN=590` (half of 1193, measured on 1.21.1-neoforge, rounded down to the ten) and `BUTTON_MIN=550` (half of 1112, rounded down). The measured values are in the comments. After setting them, `M4A1_SOLO_CONTROLLI=1` was rerun on all four folders: three `ok` each (`$S/evidence/m4a1-final-checks.txt`).

| Variant | anteprima | orientamento | pulsante | log-check.txt | result |
|---|---|---|---|---|---|
| 1.21.1-neoforge | ok, clip magenta=1193 `41x85+604+152`, screen 1193 | ok, right half 0 | ok, pixel=1112 `36x32+730+410` | empty | `OK`, exit 0, 100 s |
| 1.20.1-forge | ok, 999 / 999, same box | ok, 0 | ok, 1112, same box | empty | `OK`, exit 0, 104 s |
| 1.20.1-fabric | ok, 999 / 999, same box | ok, 0 | ok, 1112, same box | empty | `OK`, exit 0, 95 s |
| 1.21.1-fabric | ok, 1193 / 1193, same box | ok, 0 | ok, 1112, same box | empty | `OK`, exit 0, 96 s |

- **Screenshots:** `$S/<variante>/stazione-aperta.png` and `$S/<variante>/premuto.png` for each of the four variants. The montage of all eight is `$S/evidence/all-shots.png`, and `$S/evidence/clip-compare.png` compares the clips at 2x (1.21.1 on the left, 1.20.1 on the right).
- **What I saw in `stazione-aperta.png` (all four variants):**
  - the dark opaque screen in the centre, with a bronze 1 px border and the title "Maintenance Station" at the top left;
  - in the right panel, the suit standing upright and facing front: helmet visor, chest, both legs, the feet near the bottom of the clip;
  - the magenta right arm on the left side of the screen, entirely inside the clip (it does not reach the clip edges);
  - the grey test button with `0` at the bottom right;
  - the world (grass, the station's top) visible only around the screen.
- **What I saw in `premuto.png`:** identical, except that the button is green with `1`.
- **Difference between versions:** on both 1.20.1 variants the suit in the preview is visibly darker than on 1.21.1 (see "Concerns").
- `git status --short` lists no screenshots; all of them are in `/tmp`.

## Files changed (commit 45722f9)

- **New:**
  - `compat/CompatBlock.java`, `compat/impl/BlockBase.java`
  - `compat/client/CompatContainerScreen.java`, `compat/impl/client/ContainerScreenBase.java`
  - `content/StationBlock.java`, `content/StationItem.java`, `content/ModMenus.java`
  - `station/StationOpening.java`, `station/StationMenu.java`
  - `client/StationScreen.java`, `client/StationLayout.java`, `client/SuitPreview.java`
  - `gametest/StationMenuGameTests.java`
  - `tools/client/m4a1_shots.sh`
  - `blockstates/station.json`, `models/block/station_main.json`, `models/item/station.json`
  - `textures/block/station_top.png`, `station_side.png`, `station_front_main.png`, `textures/item/station.png`
- **Modified:**
  - `compat/impl/EntityBlockBase.java`, `compat/CompatEntityBlock.java`
  - `content/ModBlocks.java`, `ModItems.java`, `ModCreativeTabs.java`
  - `RedstoneExoframes.java`, `client/RedstoneExoframesClient.java`, `client/ExoframeEntityRenderer.java`
  - `gametest/GameTestKit.java`
  - `lang/en_us.json`, `lang/it_it.json`
  - `tools/gen_placeholders.py`, `tools/client/client_shot.sh`
  - `tools/client/README.md`, `tools/README.md`, `docs/compat.md`

## Self-review

- **Requirements check:** every "Produces" item and every step of the brief has been checked against the code; the docs items of Step 10 are all present.
- **Small additions beyond the brief's list:**
  - `StationLayout.lighter(argb)`, used for the hover colour ("al passaggio del mouse più chiaro"). It is pure Java, and grey or green lightened by a quarter can't become magenta.
  - `StationMenu.TITLE_KEY` (the plan named no constant). I avoided `TITLE` to keep it distinct from `StationLayout.TITLE`, which is a `Rect`.
  - `StationOpening` calls `requireNonNull` and `immutable()` on `main`.
  - A synthetic-image test of the `m4a1` checks: 1 good case and 5 negatives (magenta in the right half, magenta outside the clip, a stray difference outside the button, an unchanged button, no magenta). All behaved as expected. This was not committed; it lives in `$S/synth`.
- **Not exercised yet:** `StationLayout.leftPos` and `topPos` are not used by Task 1 code (`AbstractContainerScreen.init` computes the same values); the brief lists them in "Produces" for Task 5's test.
- **Parse-only coverage:** `@trascina` and `@clic … maiuscole` are covered by the selftest (parsing) only. No Task 1 sequence uses them in the real client; Task 5 does.
- **Doc wording:** the `ModBlocks.STATION` Javadoc says it "cade solo con l'attrezzo giusto". That describes `requiresCorrectToolForDrops`; there is no loot table and no pickaxe tag yet, since those come in Task 2.
- **Line length:** six new Java lines are 121-124 characters: `client/StationScreen.java` lines 40, 66 and 104, `client/StationLayout.java` line 5, and `client/SuitPreview.java` lines 31 and 51. Nothing enforces 120, and older files exceed it (for example `ModCreativeTabs` at 145 and `PlacedFrame` at 172). I left them as committed rather than make a Java change after Step 11. The two long lines in `client_shot.sh` were already there.
- **Other rules:** no personal data and no model or vendor names in code or docs; no Stonecutter directives outside `compat/impl/`. The two test bodies run in a single tick, and `finally` closes the menu and removes the station.

## Concerns

1. **The preview is lit wrongly on 1.20.1 (darker). It is not a gate condition, and I did not change the plan's `drawInGui` steps.**
   - **What the plan's step does on 1.21.1:** `pose.scale(s, s, -s)` matches vanilla `InventoryScreen.renderEntityInInventory`. It flips the normals' z (`normal × signum`), and 1.21.1's inventory light expects that: `Lighting.INVENTORY_DIFFUSE_LIGHT_0 = (0.2, -1, 1)`.
   - **What vanilla does on 1.20.1:** it applies the same scale with `mulPoseMatrix(new Matrix4f().scaling(s, s, -s))`, which leaves the normals alone. Its inventory light has the opposite z: `(0.2, -1, -1)`.
   - **What our step does on 1.20.1:** `PoseStack.scale(32, 32, -32)` takes the non-uniform branch and multiplies the normals by `Mth.fastInvCubeRoot(-1/32768)`.
     - Calling the jar's method directly returns `7.0054816E26` (garbage), so the normal matrix becomes diag(2.19e25, 2.19e25, −2.19e25).
     - That is z-flipped like 1.21.1, but huge. The vanilla vertex packing clamps each component to [−1, 1]; `javap` shows 1.20.1's `BufferVertexConsumer.normalIntValue` = `clamp(f, -1, 1) * 127`. I did not check whether GeckoLib normalizes the normals before packing.
     - Result: the front faces get less light. If GeckoLib does not normalize, the per-component clamp could also distort normals on rotated bones; that part is a diagnosis, not verified.
   - **Evidence:**
     - the javap output of `PoseStack.scale`, `InventoryScreen.renderEntityInInventory` and the `Lighting` constants on both versions;
     - a direct call of `Mth.fastInvCubeRoot`/`PoseStack.scale` from the 1.20.1 and 1.21.1 jars (`$S/cuberoot/Probe.java`);
     - `$S/evidence/clip-compare.png`;
     - magenta 999 on both 1.20.1 variants against 1193 on both 1.21.1 variants, in the same box: the difference follows the game version, not the loader.
   - **Why I did not fix it:** no single call is correct on both versions (1.20.1 wants the normals untouched, 1.21.1 wants z flipped), so a fix needs a Stonecutter condition, which means a new seam. The plan says "nessuna cucitura per GuiGraphics" and fixes `scale(s, s, -s)` in the step order.
   - **Proposed fix (for you to decide before Task 5):** a small per-version helper in `compat/impl/client`, behind a `compat/client` entry point, that applies the GUI entity scale as vanilla does: `mulPoseMatrix(new Matrix4f().scaling(s, s, -s))` on 1.20.1 and `scale(s, s, -s)` on 1.21.1. Then rerun `m4a1_shots.sh` on the two 1.20.1 variants and re-measure `MAGENTA_MIN` (1.20.1 would likely move up to about 1193).
   - **Where it is documented:** `docs/compat.md` ("Limiti noti e rinvii", plus one sentence in the `SuitPreview` bullet), the `SuitPreview` Javadoc, and the `m4a1_shots.sh` header and README.

## Follow-up (ruling P11)

**Status: DONE.** The 1.20.1 preview is now lit the same as on 1.21.1.

Commit: `99e4605 compat: cucitura GuiEntityScale, la scala (s, s, -s) dell'anteprima come il gioco base su ogni versione (luce giusta su 1.20.1); docs: compat.md`. It sits on top of `45722f9`, is SSH-signed by the environment, and is not pushed. The tree is clean.

Evidence for this section is in `$S/p11/`, where `$S` = `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t1`.

### What changed

- **The seam:**
  - `compat/client/GuiEntityScale`: interface with `void apply(PoseStack pose, float scale)`. Its Javadoc says it applies the GUI entity scale (s, s, −s) exactly as vanilla `InventoryScreen.renderEntityInInventory` does on each version.
  - `compat/impl/client/GuiEntityScaleImpl`: 1.20.1 calls `pose.mulPoseMatrix(new Matrix4f().scaling(s, s, -s))`, which leaves the normals untouched; 1.21.1 calls `pose.scale(s, s, -s)`. This is the only Stonecutter condition, with the `Matrix4f` import inside `//? if <1.21`.
  - `ClientCompat.guiEntityScale()`, using the holder idiom.
- **`SuitPreview.drawInGui`:** `ClientCompat.guiEntityScale().apply(pose, PIXELS_PER_BLOCK)` replaces `pose.scale(s, s, -s)`; nothing else in the step order changed. The class Javadoc now points to the seam, and two Javadoc lines were rewrapped to ≤ 120 characters.
- **`docs/compat.md` (DEC-06):**
  - new section "`GuiEntityScale` — scala delle entità nell'interfaccia": what differs between the versions (the vanilla call and inventory light constant on each version, why 1.20.1's `PoseStack.scale` won't do, the probe of the jar methods, the javap targets);
  - the `SuitPreview` bullet and a new row in the API table;
  - the "Limiti noti" entry about the 1.20.1 lighting is removed.
- **Measures in the docs:** the `m4a1_shots.sh` header and threshold comment, and `tools/client/README.md`, now give the new values: 1193 on all four variants, and the preview clip pixel-identical across them.
- **Extra checks:**
  - javap of the compiled classes: `GuiEntityScaleImpl` calls `PoseStack.mulPoseMatrix` on 1.20.1-forge and 1.20.1-fabric, and `PoseStack.scale` on 1.21.1-fabric and 1.21.1-neoforge.
  - A probe calling the jars shows that the other scales in the suit's drawing chain give the same normals on both versions, to within 1e-6: `frameSpace`'s (−1, −1, 1) and the uniform 0.9375 and 1.65. So only the GUI scale needed the seam.

### Commands run (one Gradle or client run at a time)

1. `./gradlew --console=plain :1.21.1-neoforge:compileJava :1.20.1-forge:compileJava :1.20.1-fabric:compileJava :1.21.1-fabric:compileJava` → `BUILD SUCCESSFUL`; `bash tools/check_sources.sh` → `OK sources`.
2. The build and collectJar line → `BUILD SUCCESSFUL`; `bash tools/check_jars.sh` → `OK jars` (before the client runs).
3. `EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh <variante> $S/p11/<variante>`, in the order 1.21.1-neoforge, 1.20.1-forge, 1.20.1-fabric, 1.21.1-fabric. Then `M4A1_SOLO_CONTROLLI=1` on the four folders with the final script → four `OK` (`$S/p11/final-checks-m4a1.txt`).
4. After the commit, the full end-of-task list on the final tree:
   - the build and collectJar line → `BUILD SUCCESSFUL in 40s` (`$S/p11/final-build.txt`);
   - JUnit: `1.20.1-forge 331`, `1.20.1-fabric 331`, `1.21.1-fabric 331`, `1.21.1-neoforge 331` (fresh XML);
   - `OK sources`, `OK jars`, `OK data`, `OK art`, `OK arm_measure`, `OK passi` (`$S/p11/final-checks.txt`, all exit 0);
   - `bash tools/run_gametests.sh` (`$S/p11/final-gametests.txt`, exit 0):
     - `OK 1.20.1-forge: 210 test obbligatori passati, nessun avviso fuori elenco`
     - `OK 1.20.1-fabric: 210 test obbligatori passati, nessun avviso fuori elenco`
     - `OK 1.21.1-fabric: 210 test obbligatori passati, nessun avviso fuori elenco`
     - `OK 1.21.1-neoforge: 210 test obbligatori passati, nessun avviso fuori elenco`

### New measures (m4a1_shots.sh)

| Variant | anteprima: clip / screen magenta | orientamento | pulsante | log-check.txt | result |
|---|---|---|---|---|---|
| 1.21.1-neoforge | ok, 1193 / 1193, `41x85+604+152` | ok, 0 | ok, 1112, `36x32+730+410` | empty | `OK`, exit 0, 107 s |
| 1.20.1-forge | ok, 1193 / 1193 (was 999), same box | ok, 0 | ok, 1112, same box | empty | `OK`, exit 0, 96 s |
| 1.20.1-fabric | ok, 1193 / 1193 (was 999), same box | ok, 0 | ok, 1112, same box | empty | `OK`, exit 0, 93 s |
| 1.21.1-fabric | ok, 1193 / 1193, same box | ok, 0 | ok, 1112, same box | empty | `OK`, exit 0, 88 s |

- `MAGENTA_MIN` = half of the smallest magenta (1193), rounded down to the ten = **590** (unchanged; the comment now says "misurato 1193 sulle quattro varianti").
- `BUTTON_MIN` = **550** (unchanged; 1112 on all four).

### Side by side

- `$S/p11/clip-compare-p11.png` shows three clips: 1.21.1-neoforge on the left, 1.20.1-forge after the fix in the middle, and 1.20.1-forge before the fix (darker) on the right.
- The suit is now lit the same on both versions. The four preview clips are pixel-identical: `compare -metric AE -fuzz 4%` gives 0 differing pixels for each variant against 1.21.1-neoforge.
- The whole screen rectangle (`632x464+164+38`) is identical across the four variants in both `stazione-aperta.png` and `premuto.png` (0 differing pixels).
- The only differences between the versions' shots are about 145 pixels of the world and hotbar around the screen.
- Shots: `$S/p11/<variante>/stazione-aperta.png` and `premuto.png`.

### Unexpected

Nothing beyond the original finding. The fix behaved exactly as predicted: 1.20.1 rose from 999 to 1193, the same as 1.21.1.

## Fix round 1 (ruling P12)

**Status: DONE.**

Commit: `a722480 tools: measure_lib.sh, gli aiuti delle misure comuni a m3d_shots.sh e m4a1_shots.sh`. It sits on top of `99e4605`, is SSH-signed by the environment, and is not pushed. The tree is clean.

Evidence is in `$S/p12/`, where `$S` = `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m4a1/t1`.

### What changed

- **New `tools/client/measure_lib.sh`** (mode 644: it is sourced, not run).
  - It holds `failed=()`, `take_measure`, `val`, `box`, `inside` and `verdict`, moved verbatim.
  - Its Italian header says it is loaded with `source` and not run, what each helper expects and prints, and that the caller must set `measure` (the path of `arm_measure.sh`) before using `take_measure`.
  - The one code change is in `val`: `read -r -d '' -a words <<<"$1"` and `for w in "${words[@]}"`, so there is no pathname expansion. It still splits on all whitespace, newlines included, as the old unquoted `for w in $1` did.
  - `diff` of the old m4a1 block against the library body (`$S/p12/block-diff.txt`) shows only the `val` lines and its comment.
- **`m3d_shots.sh` and `m4a1_shots.sh`:** each copy is replaced, at the same position, by
  `source "$here/measure_lib.sh" || exit 2`
  with a one-line comment above it. `$here` is the script's own directory, as in the other `tools/client` scripts. Nothing else in either script changed (`git diff --stat`: 4 insertions, 67 deletions).
- **`tools/client/README.md`:** a short `measure_lib.sh` entry right after the `m4a1_shots.sh` section and before `arm_measure.sh`.

### Commands and outputs

1. **Before/after for the `val` fix.** In a folder that contains a file named `x=5` (`$S/p12/globdir`):
   - old `val 'x=*' x` → `5` (`$S/p12/val-before.txt`);
   - new `val 'x=*' x` → `*`, `val 'pixel=12 riquadro=3x4+5+6' riquadro` → `3x4+5+6`, `val 'pixel=12' assente` → `-1` (`$S/p12/val-after.txt`).
2. **`bash -n`** on `tools/client/measure_lib.sh`, `m3d_shots.sh` and `m4a1_shots.sh` → no output, exit 0 each (`$S/p12/bash-n.txt`).
3. **`M3D_SOLO_CONTROLLI=1 bash tools/client/m3d_shots.sh <v> $S/m3d-<v>`** (`$S/p12/m3d-<v>.txt`):
   - 1.21.1-neoforge: 14 `ok`, 0 `KO`, `OK 1.21.1-neoforge: scatti in .../m3d-1.21.1-neoforge`, exit 0;
   - 1.20.1-forge: 14 `ok`, 0 `KO`, `OK 1.20.1-forge: scatti in .../m3d-1.20.1-forge`, exit 0;
   - the `ok` lines are identical to those of the original Step 8 runs (`diff`).
4. **`M4A1_SOLO_CONTROLLI=1 bash tools/client/m4a1_shots.sh <v> $S/p11/<v>`** on the four P11 folders (`$S/p12/m4a1-checks.txt`):
   - each variant gives three `ok`: `anteprima` ritaglio magenta=1193 `41x85+604+152`, schermata 1193; `orientamento` 0; `pulsante` pixel=1112 `36x32+730+410`;
   - then `OK <variante>: scatti in …`, exit 0;
   - all lines are identical to the P11 checks (`diff`).
5. **Extra: the synthetic cases from Step 7, through the library** (`$S/p12/synth.txt`), give the same verdicts as before:
   - `ok` → `OK`;
   - `right` → KO `orientamento`;
   - `outside` → KO `anteprima`;
   - `stray` → KO `pulsante`;
   - `nochange` → KO `pulsante`;
   - `nomag` → KO `anteprima` and `pulsante`.
6. **`bash tools/client/client_shot.sh --selftest`** → `OK passi`, exit 0 (`$S/p12/selftests.txt`).
7. **`bash tools/client/arm_measure.sh --selftest`** → `OK arm_measure`, exit 0.
8. **`bash tools/check_sources.sh`** → `OK sources`, exit 0.

No Gradle or client run was needed: only shell scripts outside the build changed.

### Concerns

None. As the ruling says, the Minor findings were not touched.
