# Final whole-branch review: part 4c "L'HUD" (f6da189..8ecd206)

**How I reviewed.** I read the dispatch, the whole plan (421 lines), the Global Constraints, the spec sections the plan names (§4.3, §5.12, §5.14, §5.16, §5.18-5.20, §6, §8, §9, §10, §11, §12 and the new §13) and the ledger with R1-R21. The 512 KB package was too large for one read, so I went through it in about fourteen passes: the Java code, then the tests, the language files, the tools, and the docs. For line numbers and the full checklist I also read the files at HEAD.

Checks I made myself, without running Gradle, a client or a server:
- **Labels.** Every vanilla label the checklist quotes, against the cached assets. The Italian `it_it.json` has the same hash (`3e2996…`) in the 1.20.1 and 1.21.1 indexes. The English is `en_us.json` from both client jars.
- **Mod texts and commands.** Mod texts against both language files and `ModItems`; command forms against the tree in `ExoframeCommand.java:125-155` (`debug wear rottame <0-3>|nudo|via`, `debug energy <int ≥ 0>`).
- **Bytecode (javap), on the mapped 1.20.1 and 1.21.1 jars.**
  - `MobEffectInstance`, `EffectCommands` and `Gui`'s `showIcon()` check;
  - `GuiGraphics.fill`, which draws through `RenderType.gui()`;
  - `KeyboardHandler.keyPress` (1.20.1).
- **Screenshots.** I viewed all 34 reference shots: the 20 of the 4c, the 4 of the 3c and the 10 of the 3d, as crops or reduced images.
- **One measurement.** I ran `arm_measure.sh colore` (ImageMagick only; the tree stayed clean) to place the cell icon.
- **Hand-offs.** I checked every item the per-task reviews passed forward: the Task 1 silhouette look, R5, run C live on NeoForge, the Fabric confirmations, the spare-cell checklist cases and the `check_art.sh` header. All are resolved or in the ledger.
- **Commits.** Every commit has a signature header. The two commits after the final four-variant verification (5b1941a and 8ecd206) change only comments and docs in code files: no non-comment line changed in `src/` or in `m4c_shots.sh`.
- **Scans.** Added lines contain no vendor or model names and no personal data. No Stonecutter directive, mixin or allowlist line was added. Both language files have the same 104 keys in the same order. The removed keys and classes are referenced nowhere in `src/`.

## Strengths

- **The design is clean.** Three pure rules decide everything that is drawn: `core/frame/HudLevel`, `client/HudModel` and `client/HudLayout`. `SuitHud` only draws. `ClientPrefsFile` holds no Minecraft code. `HudKeys` handles only input. Each open owner question has one home:
  - question 8: `HudLayout.group`;
  - question 9: `HudModel.energyColor`;
  - question 10: `Values.DEFAULTS` and `HudKeys.VIGNETTE_DEFAULT`.
- **Per-tick and per-frame error handling follows DEC-39.**
  - `SuitHud.tick` (SuitHud.java:96-118), `SuitHud.render` (:147-174) and `HudKeys.tick` (HudKeys.java:62-86) each wrap all their work in a `try`.
  - Each logs one ERROR per session, and the flag resets on `CLIENT_PLAYER_QUIT`.
  - Blending is switched off in a `finally` (SuitHud.java:204-206).
  - The `fill` calls after the vignette draw through `RenderType.gui()` (javap, both versions), which sets and clears its own blend state, so switching blending off does not affect the translucent plate. The shots agree: the vignette shows through the plate in the bottom-left corner.
- **`ClientPrefsFile` is careful.** It does what §5.14 asks:
  - it never throws;
  - it writes a `CREATE_NEW` temporary file with a random name and deletes it in a `finally`;
  - it moves the file atomically, with a fallback when the file system cannot;
  - it keeps unknown keys in order, including `null` ones (`serializeNulls`);
  - it never downgrades a newer `version`;
  - any type error resets both values to the defaults.
- **DEC-23 is respected.** Colour and warning come from the server's energy state, the level comes from `clientView`, and charge/capacity is used only for the percentage and the bar fill. `EnergyCell` keeps `0 ≤ stored ≤ capacity` and `capacity > 0`, so `HudModel`'s range checks cannot fire in play.
- **The tests pin exact values.** The 29 new JUnit tests check:
  - rounding down;
  - the blink phases;
  - every ARGB value of the legend, the outlines and the lighter first row;
  - the magenta guard;
  - the seven GUI sizes against vanilla elements with decimal geometry;
  - the screen rectangles shared with the scripts.

  The counts match the plan (3+10+7+9; 381 in total).
- **The cross-task constants agree.** Every rectangle in `arm_measure.sh` (`HUD_RIGHT`, `HUD_LEFT`) and in `m4c_shots.sh` is pinned to the code by `HudLayoutTest.screenRectsAtNineSixtyMatchTheSequence`, with two exceptions. `BAR_FILL_BODY_RIGHT` (`122x10+52+510`) and the R13 box `WARNING_PERCENT_RIGHT` (`42x16+50+482`) are derived boxes that only the client runs guard. The station takes its legend from `HudModel`, and `HudModelTest` checks that.
- **The screenshot checks reject wrong but plausible images.** They use exact boxes: the group, `BAR_ONLY`, the bar fill body, and the warning plus "0%" box (R13). They add colour checks, the translucency check (R16) and the restart comparison across runs (R11). I recomputed all ten thresholds; each is half the measured value, rounded down to the ten.
- **Gate discipline was good.** The Task 2 gate proved the key binding on four variants. The Task 3 stop rule was used when a check failed. Every deviation is a recorded ruling.
- **The checklist is accurate where a literal reader needs it.**
  - All quoted vanilla labels are right in both languages, including "Allow Commands" on 1.21.1, "Swap Item With Off Hand", "Incudine atterra", "Salvataggio del mondo...", "[Diagnostica]: …" and "Ripristina".
  - Every mod text matches the language files.
  - The JSON lines expected in steps 12-14 match Gson's pretty-printed key order.
  - The numbers in steps 6, 9 and 23 are right: "9 %", "50 %", "11 %" then "10 %", and about 12 s walking or 5 s running.
  - The routing across variants holds, including the restart after step 14.
- **The reference shots match what the checklist says.**
  - `pieno` (both variants): the group bottom left, dark corners with the two glints at the top, the suit's arm bottom right.
  - `danneggiato` and `critico`: a yellow and a red silhouette.
  - `mancino-danneggiato`: the group bottom right, the silhouette on the right edge and the text reading from the left.
  - `terza`: the group with no dark corners.
  - `bassa`: the triangle is on in both variants.
  - `esaurita`: grey, steady triangle, empty bar.
  - `scorta`: white "×1".
  - `senza-elmo`: only the energy part, in place.
  - `vignetta-effetti`: on Forge the vignette covers the icon; on NeoForge the icon is crisp on top.
  - The redone 3c shots show 49 %, 4 % and 0 %.
  - All ten redone 3d shots show the HUD in the corner opposite the hand, with the vignette wherever a working helmet is seen in first person. `braccio-nudo` shows the bar-only HUD with no vignette, and in `mappa` the group sits over the left map arm.

## Issues

### Critical (Must Fix)

None.

### Important (Should Fix)

None.

### Minor (Nice to Have)

1. **`docs/test-in-gioco/m4c-hud.md:90` (preparation E), `:133` (step 15, subtitles) and `:102` (step 3). Fix before merge; this is a deferred line I am promoting.**
   - **What is wrong.**
     - Preparation B uses the same four instances as the earlier checklists, and step 4 of `m3c-giocarci.md:151` turns "Sottotitoli: Sì" on. No later checklist turns it off.
     - E then says to leave subtitles as they are. Step 15 expects a button reading "Sottotitoli: No" that turns into "Sì", and at the end it sets "No".
     - E also leaves the main hand as it is, while step 3 expects the group bottom left.
   - **Why it matters.** On most instances the reader cannot follow step 15 as written. They might switch subtitles off right before the subtitle test.
   - **Fix.** In E: set "Sottotitoli: No" (note it if it was "Sì") and check "Mano principale: Destra".
2. **`docs/test-in-gioco/m3c-giocarci.md:12` (the 4c note). Fix before merge; deferred line promoted.**
   - **What is wrong.** The note changes step 12 only. Step 10 (`:175`) also quotes the keys hint word for word, in Italian and in English, and with the 4c jars both texts change. The English goes from "…, hold R to swap the cell" to "…, R to swap cells; H hides the HUD".
   - **Fix.** Write "Passi 10 e 12" and add the English text.
3. **Deferred line L99 (the particle in `1.20.1-forge-vignetta-effetti.png`): its suggested remedy is wrong.**
   - `effect give $u minecraft:luck 60 0 true` would also hide the effect icon, which is the subject of the shot.
   - Evidence from javap on 1.20.1 and 1.21.1:
     - `MobEffectInstance(effect, int, int, boolean, boolean)` passes `visible` twice, so `showIcon` equals `visible`;
     - `EffectCommands` builds the effect with that constructor and `!hideParticles`;
     - `Gui` checks `showIcon()`.
   - `m3d_shots.sh` relies on exactly this to hide the invisibility icon.
   - Leave `m4c_shots.sh:279` as it is. The checklist already explains the particle at `m4c-hud.md:80` and in step 15.
4. **`src/test/java/.../client/HudLayoutTest.java:38-40` and `:134-137`.**
   - `ONE_DIGIT_SPARE = 11` and `TWO_DIGIT_SPARE = 17` are the plan's estimates.
   - **What I measured.** `arm_measure.sh colore` puts the cell icon's charge, `C81E1E`, at `6x8+142+488` in both `1.21.1-neoforge-pieno` ("×0") and `1.20.1-forge-scorta` ("×1"). That is the icon at interface x 70, so at 47 in the energy subgroup with the text at 53: one-digit "×N" is 12 wide in the game, as Task 3 found.
   - So the one-digit assertion, text at 54 with the icon at 48, describes a layout the game never draws.
   - **Not measured.** The two-digit width: no shot has "×10" or more. 18 is only a deduction.
   - The test's conclusions still hold: both cases still fit inside `SPARE_AREA`, which is why nothing failed. But the comment calls these widths the game's.
   - **Fix.** Use 12 for one digit, labelled measured. For two digits, measure the width in the game, or label 17/18 as an estimate.
5. **The design, which binds, still describes the pre-R1 no-cell text.**
   - §5.12 "percentuale" (design:274) and §5.16 (design:376) still give "Nessuna cella" in the percentage slot.
   - §13 (design:633) still says Decision 1 "aspetta la conferma del proprietario prima del Task 1".
   - What ships is "Senza cella" / "No cell" on the bar's row (R1).
   - R18 amended the design for the layering facts; R1 deserves the same one-line amendment.
6. **`m4c-hud.md:19`.** "Le celle di scorta. In alto a destra nel gruppo" is true only for the right hand. For a left-hander they sit at the top right of the energy part, mid-group next to the silhouette (see `mancino-danneggiato`). **Fix.** "in alto a destra nella parte dell'energia".
7. **`README.md:28`.** It still says `gen_placeholders.py` regenerates "le texture provvisorie degli oggetti e l'icona". It now also writes the block faces (part 4a-1) and the HUD vignette (this branch).
8. **`HudModel.java:74` and `StationLayout.java:80`.**
   - `HudModel.CELL_CHARGE` holds the same value as `StationLayout.CELL_RED` (`0xFFC81E1E`) as a second constant.
   - Its Javadoc cites the design previews (§12), but there the redstone is `C0201A` and fills rows 2-5. The real source is the station's red.
   - This goes with deferred line L37: let one constant refer to the other and fix the citation when the file is next edited.

## Deferred minors triage

The ledger has 22 `minor (deferred)` lines. Line 110 is the dispatch entry, not a finding. Line numbers below are at HEAD where they moved.

| # | Ledger line | Decision | Reason |
|---|---|---|---|
| 1 | L37 `HudModel.java:73-74`, `CELL_CHARGE` Javadoc source | Can stay | Comment only. Fold in Minor 8 when the file is next edited. |
| 2 | L38 `HudLayoutTest` one-sided `contains`/`intersects` checks | Can stay | `Rect.contains` and `intersects` are correct by inspection. The checks against vanilla elements use the test's own `Area.touches`, and `vignetteCornersStayInTheCorners` asserts that method true at the narrow sizes, so the main layout guarantees are not vacuous. Add one positive `intersects` and one negative `contains` assertion when the test is next touched. |
| 3 | L39 `WARNING` and piece rectangles not pinned | Can stay | These are placeholders that part 4d redraws. Pin the final geometry then. Until then the design table and the plan are the reference. |
| 4 | L40 `HudModelTest` station loop true by construction | Can stay | Harmless. It still catches a later change that gives the station its own constants again (Decision 3). |
| 5 | L50 `ClientPrefs` Javadoc "solo sul thread del client" | Already fixed (R12, e53ab48) | `ClientPrefs.java:9-16` and `SuitHud`'s Javadoc now say that `load()`/`register()` run on the mod-loading thread on Forge and NeoForge. |
| 6 | L51 no broken-file case pins "both values reset" | Can stay | `load` returns `fallback(DEFAULTS)` on any type error (`ClientPrefsFile.java:98-107`), as §5.14 asks. Add `{"hudHidden": "yes", "vignette": false}` → `DEFAULTS` as a regression guard. |
| 7 | L52 `WRITE_PROBLEM` copies the literal | Can stay | The worst case is a doubled phrase in a WARN that only a failed write produces. |
| 8 | L53 `client_shot.sh` waits 900 s on a rejected `options.txt` | Can stay | Harness convenience only. The cause, the missing `version:` line, is fixed (R6). |
| 9 | L65 `testo` counts green only | Can stay | `HudModelTest` pins the value, the area pins the side, and `barra-normale` and `barra-esaurita` pin exact boxes. Pinning `46x14+50+482` needs only `M4C_SOLO_CONTROLLI` on existing shots. |
| 10 | L66 spare-cell count only seen with one main-inventory cell | Can stay | Checklist step 7 covers the empty cell and the offhand cell on all four variants. Cells do not stack. |
| 11 | L78 `m4c_shots.sh` unwrapped comment line | Can stay | Cosmetic. |
| 12 | L79 `tools/check_art.sh` header lists only some texture families | Can stay | Comment only. `:51` also leaves out the station faces, an omission from before this branch. The check itself compares every generated file. |
| 13 | L94 `m4c-hud.md:124` "J in rosso" | Can stay | B allows no mods besides the dependencies, and none of them binds J, so the literal path never meets a conflict. |
| 14 | L95 `m3c-giocarci.md:12` names step 12 only | **Fix before merge** | Minor 2: step 10 quotes the hint word for word in both languages, and both changed. One clause. |
| 15 | L96 `m4c-hud.md:90`, E leaves subtitles as they are | **Fix before merge** | Minor 1: m3c step 4 left subtitles on in these instances, so step 15 cannot be followed as written. E should also check the main hand. |
| 16 | L97 `m4c-hud.md:93` leftover `/clear … 0` sentence | Can stay | True and harmless. Remove it when the paragraph is next edited. |
| 17 | L98 m4a1 durations 132-152 s vs 118 s | Can stay | It only documents a measured duration. |
| 18 | L99 Luck particle in the forge reference shot | Can stay; **do not apply the suggested `… 60 0 true`** | That flag also hides the icon (Minor 3). The checklist already explains the particle. |
| 19 | L100 `m4c-hud.md:15` "non tocca mai il braccio" | Can stay | Step 19 states the map exception. Optionally add "a riposo". |
| 20 | L101 loader WARNs in the gametest logs | Can stay | They are not from the mod, and `run_gametests.sh` reports none outside its list. |
| 21 | L107 step 21 (now `:145`), NeoForge F3 expectation from bytecode | Can stay | It is hedged ("possono") and asks the reader to note what they see. |
| 22 | L108 `m4c-hud.md:93` wordy routing | Can stay | I traced it for all four variants, including the restart after step 14, and it is consistent. |

## Declined to judge

- `README.md:11` names the historical first spec file, whose name contains a vendor name. The line was already there at f6da189 (it is only context in this diff), so this branch did not add it. The rule's owner decides.
- Commit metadata (author and trailers) names the assistant. The Global Constraints say commits are signed "come fa l'ambiente", and the no-names rule covers docs and code.
- Holding H, or the vignette key, toggles repeatedly through OS key repeat and rewrites the file each time. javap of 1.20.1 `KeyboardHandler.keyPress` shows a click on any action other than release. Vanilla's own toggle bindings, such as F5, take the same path, and the spec says nothing about it.
- Other mods' HUD elements in the bottom corners, such as minimaps or inventory HUDs, may overlap the group. The spec and the checks cover vanilla elements only.
- A UTF-8 BOM, or a symlink, as the preferences file. A BOM reads as invalid JSON: defaults plus a WARN, and the first toggle rewrites the file and drops unknown keys, the same as `ConfigFile` for the BOM. The atomic move replaces a symlink with a plain file. The mod writes this file, not the user, and §5.14 does not cover these cases.
- Accessibility of the red/green legend for colour-blind players. That is the owner's choice (question 9).
- Showing RE numbers on the HUD instead of only a percentage. That is a design choice; `/exoframe debug energy` gives the RE.
- Subtitles and the saving indicator on Fabric 1.21.1, and F3 over the group on NeoForge 1.21.1. Both are deduced from bytecode (R20, R21), no shot shows them, and the checklist asks the reader to note them. They cannot be verified here.
- The production jars on a real graphics card. They are the PC checklist's job.
- The gate commit c3b43aa ("controlli … NON passati") stays in the history. Merge versus squash is the coordinator's call.
- Whether to amend the plan file. It is tracked and still carries facts that execution disproved:
  - plan:76, "×0" is 11;
  - plan:151, `options.txt` without `version:`;
  - plan:182, 333 and 344, the layering per version;
  - plan:189 and 211, `riavvio-senza-tuta`.

  The ledger with R1-R21 is git-ignored. R7 deliberately leaves the plan as it is; I make a recommendation below instead.

## Recommendations

1. Before merge, make the two checklist edits (Minor 1 and 2). They are doc-only and need no rerun.
2. After merge, do what was done for part 4a-1:
   - archive this execution folder (ledger, briefs, reports, reviews) on the archive branch;
   - add "Riporti della parte 4c" to design §13, with the R1 amendment (Minor 5) and the items marked "can stay": pinning the silhouette and warning rectangles when part 4d redraws them, the `testo` box, the per-key-fallback test case, and what the PC run sees on Fabric 1.21.1 (subtitles, saving indicator, F3).

   Without this, nothing tracked explains why the plan and the code differ.
3. Cheap test hardening, next time these files are touched:
   - in `HudLayoutTest`, one positive `intersects` and one negative `contains` assertion, and the font widths measured in the game (Minor 4);
   - in `screenRectsAtNineSixtyMatchTheSequence`, the bar body `122x10+52+510`, which follows from `BAR_FILL` minus its first row. The R13 box `42x16+50+482` also depends on the width of the English "0%", so it can only be pinned together with a width measured in the game;
   - in `ClientPrefsFileTest`, a broken case that mixes a valid and an invalid key.
4. Keep the `vignetta-effetti` shot as it is (Minor 3). If a reference without the particle is wanted, re-take the shot. Do not use `hideParticles`.

## Assessment

**Ready to merge?** With fixes

**Reasoning:** The code, tests, sequences and docs match the plan and spec as amended by R1-R21, on four variants, with no Critical or Important defect. The error handling, the preferences file and the layering facts are sound. The only changes I would make before merging are two doc-only checklist corrections (Minor 1 and 2) on the path the PC assistant follows literally; they need no code change and no rerun.
