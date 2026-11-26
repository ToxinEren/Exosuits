### Spec Compliance

- ❌ Issues found: every file and section the brief lists is present, but one documented claim is contradicted by the implementer's own Fabric run. On 1.21.1-fabric the vignette is drawn over the effect icon, the same as on 1.20.1.
  - The checklist and the client README say that on both 1.21.1 variants the icon is crisp and above the vignette, and they present this as verified in the cloud (Important 1).
  - The Task 5 dispatch asked for "the Fabric confirmations" (`progress.md:88`), and these sentences claim a confirmation the run does not give.
  - The brief's own per-version expectation, repeated in the checklist, is wrong for 1.21.1-fabric (Important 2, plan-mandated).
  - The report's "tutto corrisponde" for the compat.md re-read is contradicted by the same run: `docs/compat.md:248` has the per-version sentence.
- ✅ The brief's file list, file by file:
  - new: `docs/test-in-gioco/m4c-hud.md` and 20 shots in `m4c-screenshot/`.
  - 4 shots redone in `m3c-screenshot/` and 10 in `m3d-screenshot/`.
  - one 4c note each in `m3a-vedere-il-telaio.md:9`, `m3b-entrare-e-uscire.md:7`, `m3c-giocarci.md:9-20`, `m3d-braccio.md:9` and `m4a1-banco.md:11`, each placed after the existing notes.
  - `README.md` Stato and Documenti; the `tools/client/README.md` paragraph and "Quanto dura".
  - comment-only hunks in `m4c_shots.sh`, `m3d_shots.sh` and `m3c_shots.sh`.
  - `docs/compat.md` re-read, unchanged.
- ✅ Checklist structure matches the brief: header, "Cosa c'è da vedere", "Cosa non c'è ancora", "Già verificato", preparation A-F with C replaced by the prefs-file step, steps 1-24 in the brief's order, "Cosa riportare", and questions 8, 9 and 10.
  - The list of choices to show covers design §8 points 12-17, 19 and 21. Checked against design.md:474-505.
- ✅ Rulings reflected: R11 in the `riavvio` row (m4c-hud.md:61); R13 in the `barra-esaurita` row (:53); R14, toasts above on both versions (:26); R16 in the `vignetta` row (:57); R17, nothing asked on Forge 1.20.1 (:136). R19 was run: 3 ok, verdict `OK 1.21.1-neoforge` (`/home/user/exo-runs/task5/1.21.1-neoforge.m4a1.out`).
- ✅ The brief's Step 4 and Step 5 checks, re-run at HEAD:
  - no "manca" line for m3c, m3d or m4c.
  - the 4c note counted once in each of the five files.
  - 24 steps; one step 1; `Scala dell.interfaccia` 0; `m4c_shots` mentioned 8 times.
- ✅ Extra, harmless: `tools/README.md:3-4` (outside the file list) adds `m4c_shots.sh` to the list of sequences. The implementer disclosed it.
- ⚠️ Cannot verify from the diff alone:
  - `SuitHud.java:181-188` (Javadoc), `docs/compat.md:248` and design §5.12/§10 carry the same per-version sentence that 1.21.1-fabric contradicts. All three are outside this diff and need a controller ruling, as R14, R17 and R18 did.
  - Subtitles and the saving indicator on 1.21.1-fabric (m4c-hud.md:26, :133): no shot shows them, so they are not observed in game. By the mechanism below they are expected under the group there, as on 1.20.1. A client run on 1.21.1-fabric with a subtitle shot would confirm it.
  - The cause, from bytecode:
    - Vanilla 1.21.1 `Gui.render` is `enableDepthTest` → `layers.render` → `disableDepthTest` → return.
    - Fabric API rendering-v1 5.2.1 `InGameHudMixin` fires `HudRenderCallback` at TAIL of `Gui.render`, so after the depth test is off.
    - Architectury Fabric 13.0.11 `EventHandlerImpl` feeds `ClientGuiEvent.RENDER_HUD` from `HudRenderCallback.EVENT`.
    - NeoForge 21.1.266 fires `RenderGuiEvent.Post` inside `GuiLayerManager.render`, before `disableDepthTest`.
    - So on Fabric 1.21.1 the HUD and the vignette paint over every vanilla HUD layer, and on NeoForge they lose to the layers at higher z.

### Strengths

- **The labels, texts and commands quoted for the PC assistant match the game.** This was the named risk, and I checked it.
  - Vanilla Italian labels come from it_it.json (asset `3e2996…`, the same hash in the 1.20.1-5 and 1.21.1-17 indexes). The English ones come from both `minecraft-client.jar`. Keys checked: menu.options, options.controls, controls.keybinds, key.keyboard.unknown, gui.done, menu.returnToGame, menu.returnToMenu, menu.quit, options.skinCustomisation, options.mainHand.*, options.video, options.guiScale(.auto), options.sounds, options.showSubtitles, options.on/off, debug.prefix, debug.advanced_tooltips.*, debug.reload_resourcepacks.message, menu.savingLevel, subtitles.block.anvil.land, controls.reset, key.swapOffhand (including "Off Hand" on 1.21.1), selectWorld.* and effect.minecraft.luck.
  - Every mod text matches `lang/it_it.json` and `en_us.json`.
  - The `/exoframe` literals match `ExoframeCommand.java:124-160`.
  - The charged-cell `/give` forms match `m3c_shots.sh:65-67` and `m4c_shots.sh:245-246`.
  - The step 13 WARN fragments match `ClientPrefs.java:36` and `ClientPrefsFile.java:92`.
  - The step 12-14 file lines and key order match `ClientPrefsFile.save`, which inserts version, hudHidden and vignette in that order and keeps unknown keys after them.
- **Step 8's hit counts follow from the code.**
  - `/damage` without a position is classified `GENERIC` (DamageClassifierImpl:53-56), which has no primary pieces.
  - The default shares split each 80-point hit as torso 24, helmet 12, arms 10 each, legs 12 each.
  - The torso goes 90 → 66 → 42 → 18 (critical at hit 3) → 0 (broken at hit 4). The other pieces stay at 42 or more, so they stay yellow, as written.
- **Step 9's "×1" and the added `/clear` are right.** In creative the held item is kept (`FrameInteractions.java:173` and `:209`, `ExoframeItem.java:84`). A cell given without data is empty (`CellItem.toCell`, which reads 0 when the data is missing), so step 7's "resta ×1" holds.
- **Every reference shot is what it claims to be.**
  - All 34 PNGs are 960×540 and contain only IHDR, IDAT and IEND chunks.
  - Each is pixel-identical (ImageMagick AE = 0) to the source shot the report names. The forge m4c shots come from `m4c-2`, the neoforge ones from `m4c`, `mancino-danneggiato` from `mancino/`, the m3c shots from the named `hud-energia*` files, and the m3d shots from `m3d`, `m3d/mancino` and `colori`.
- **The measurement claims hold across variants.** The four m4c `ok` sets differ from neoforge only in the `barra-bassa` pixel count and the two bottom vignette corners, as the docs say. The m3d values (18075, 39962, 8.77, 7.72, 23629/28977, 0, 100 %) match the `m3d_shots.sh` comments.
- **The run evidence backs the report.** All exit codes come from `/home/user/exo-runs/task5/*.out` and `final.out`:
  - m4c: 17 ok on all four variants, plus the second forge run.
  - m3d: 14 ok on all four. m3c: OK on all four. m4a1: 3 ok on neoforge.
  - JUnit: 381 per variant. In-world tests: 235 per variant.
  - OK sources, jars, data, art, arm_measure and passi; all three log-check.txt files are empty on every variant.
- **The commit and the tree are in order.** The commit carries a gpgsig header and both trailers, it is on `m4-stazione`, and the tree is clean. The script hunks change comments only, and no threshold or rectangle moved.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

1. **The docs claim a cloud verification that the 1.21.1-fabric run contradicts.** On 1.21.1-fabric the effect icon sits under the vignette, as on 1.20.1. These sentences are the implementer's own, not the brief's.

   Evidence, from the implementer's own runs:
   - `arm_measure.sh differenza … 60x60+900+0` on `vignetta-effetti.png`:
     - 1.21.1-fabric against 1.20.1-fabric: 0 pixels differ.
     - 1.21.1-fabric against 1.20.1-forge `m4c-2`: 0 pixels differ.
     - 1.21.1-fabric against 1.21.1-neoforge: 816 pixels differ, in `46x38+910+4`.
   - Pixel samples: at (930,25) the icon green is 65 on both 1.20.1 runs and on 1.21.1-fabric, and 76 on neoforge. At (915,8) the icon frame is 68 where the reflection crosses it (1.20.1 and 1.21.1-fabric) and 33 on neoforge.
   - In 4× crops I viewed, the glass reflection crosses the icon frame on 1.21.1-fabric, exactly as on 1.20.1.
   - The run is the right one: `1.21.1-fabric/m4c/client.log` says "Loading Minecraft 1.21.1 with Fabric Loader 0.20.1".

   Wrong statements, all written by the implementer:
   - `m4c-hud.md:63`: "l'icona dell'effetto è nitida sopra la vignetta sulle due 1.21.1", presented as checked in the cloud.
   - `m4c-hud.md:89`: the neoforge shot is named as the reference for both 1.21.1 variants. For `vignetta-effetti` it does not match 1.21.1-fabric.
   - `tools/client/README.md:189`: "uguale sulle due varianti di ciascuna", a phrase this diff adds.
   - `task-5-report.md:102`: "Messaggi e icona dell'effetto guardati: come su fabric 1.21.1".

   Why it matters:
   - The owner is told a cross-loader behaviour was verified in the cloud when the evidence says otherwise.
   - The dispatch asked for the Fabric confirmations (`progress.md:88`); the Fabric run gave a contrary result, and it was reported as a confirmation.

   How to fix:
   - Write the measured fact in those four places: the icon is above the vignette only on NeoForge 1.21.1, and under it on 1.21.1-fabric and on both 1.20.1 variants.
   - Name a reference for `vignetta-effetti` on 1.21.1-fabric: the 1.20.1-forge shot matches it to the pixel in the corner.

2. **Plan-mandated: the per-version expectation the brief imposes is wrong for 1.21.1-fabric.**
   - `m4c-hud.md:26` says "Su 1.21.1 sottotitoli, indicatore del salvataggio e icone degli effetti stanno sopra l'HUD e la vignetta, su 1.20.1 sotto".
   - Step 15 at `:133` and `:135` says "attesa sopra … su 1.21.1".
   - These repeat the brief's step 15 (`task-5-brief.md:25`), the plan's "Fatti del gioco" (`contesto-comune.md:74`) and R14 ("effects, saving indicator and subtitles keep the brief's sentence").
   - The same evidence as Important 1, together with the bytecode under ⚠️, shows that on 1.21.1-fabric the HUD paints over the vanilla layers.
   - Why it matters: the assistant does these steps once per version. If they pick 1.21.1-fabric, the expectation is wrong and the reference shot does not match, so a correct result looks like a failure.
   - How to fix: this needs a controller ruling, as R14 and R17 did. Then make the expectations depend on the loader (above on NeoForge 1.21.1, under on Fabric 1.21.1 and on 1.20.1), or turn them into "annota" on 1.21.1-fabric. The same ruling covers the out-of-diff carriers: SuitHud Javadoc, compat.md:248, and design §5.12/§10.

#### Minor (Nice to Have)

1. **`m4c-hud.md:124` describes a key conflict wrongly.** It says "Se J è scritto in rosso … un altro tasto che resta bianco". On both versions a conflicting key shows as "[ J ]": the J stays white, the brackets are red, and a red bar appears left of the button (javap of `KeyBindsList$KeyEntry` in the 1.20.1 and 1.21.1 merged jars). The brief's "se J è rosso" is part of the cause. The case is unlikely, since no other mods are installed. Fix: describe the red brackets and the red bar.
2. **The m3c note misses step 10.** `m3c-giocarci.md:12` gives the new keys hint under "Passo 12", but the line is quoted word for word, with English, at step 10 (`m3c-giocarci.md:175`). The English also changed, to "Hold V to leave the exoframe, R to swap cells; H hides the HUD". The brief listed step 12 only. Fix: "Passi 10 e 12", with the English.
3. **Preparation E conflicts with the m3c checklist on subtitles.** `m4c-hud.md:90` says to leave subtitles as they are. The m3c checklist (step 4, `m3c-giocarci.md:151`) turned "Sottotitoli: Sì" on in the same instances, and no later checklist turns it off. Step 15 (`:133`) assumes "No" at the start and ends by setting "No". Fix: E sets "Sottotitoli: No" and checks "Mano principale: Destra".
4. **`m4c-hud.md:93` has a leftover sentence.** "`/clear @s <oggetto> 0` non toglie nulla…" was copied from m4a1, and no m4c step uses `/clear … 0`. Remove it.
5. **A duration in a paragraph this task edited is out of date.** `tools/client/README.md:239` (and the comment in `m4a1_shots.sh`) still give `m4a1_shots.sh` 132-152 s, but the R19 run took 118 s. The implementer disclosed this. Widen the range.
6. **The forge effect reference has a large particle in it.** `m4c-screenshot/1.20.1-forge-vignetta-effetti.png` shows a large Luck particle under the crosshair. The checklist warns about it at `m4c-hud.md:80`. A later change to `m4c_shots.sh:278` (`effect give $u minecraft:luck 60 0 true`, which hides the particles) would make the reference clean.
7. **`m4c-hud.md:15` overstates.** "così non tocca mai il braccio della tuta" conflicts with step 19, where the group covers part of the left arm with a two-handed map and this is accepted.
8. **Gametest log noise in the evidence.** The gametest logs in `final.out` contain Forge and Minecraft loader WARNs (ModFileParser, ForgeConfigSpec). They are pre-existing infrastructure noise, not from the mod, and `run_gametests.sh` reports no warnings outside its list.

Out-of-diff checks I ran, each tied to a named risk:
- Quoted labels and texts: vanilla lang assets and mod lang files.
- Commands: `ExoframeCommand`.
- WARN text: `ClientPrefs` and `ClientPrefsFile`.
- Creative item handling: `FrameInteractions` and `ExoframeItem`.
- Empty cell: `CellItem`.
- Step 8 hit counts: `DamageClassifierImpl`, `PieceSlot` and `DamageRouter`.
- Key-conflict display: javap of `KeyBindsList$KeyEntry`.
- Step 20 "cursore": `Options` uses `ClampingLazyMaxIntRange` on both versions, which is a slider.
- Step 18 "sotto il velo": `AbstractContainerScreen.renderBackground` on 1.21.1 calls `renderTransparentBackground`, with no blur.
- HUD layering on 1.21.1: javap of vanilla and NeoForge `Gui.render`, NeoForge `GuiLayerManager.render`, Fabric API `InGameHudMixin` and Architectury `EventHandlerImpl`.
- compat.md:47: a past-tense 3c-1 provenance statement, left as history; not a finding.

Shots viewed:
- m4c full frames: 1.20.1-forge pieno, vignetta-effetti and terza; 1.21.1-neoforge vignetta-effetti and mancino-danneggiato.
- Group crops: bassa ×2, esaurita ×2, senza-elmo, scorta, danneggiato and critico.
- The four m3c group crops.
- A contact sheet of the ten m3d shots.
- 4× corner crops of vignetta-effetti on 1.20.1-forge, 1.21.1-neoforge, 1.21.1-fabric and 1.20.1-fabric.
- `1.21.1-fabric/m4c/messaggio-nascosto.png`.
- Corner brightness of all 34 committed shots, which confirms which ones show the vignette.

### Assessment

**Task quality:** Needs fixes

**Reasoning:** The checklist's quoted labels, commands and expected values, and the 34 reference shots, check out against the game assets, the code and the run outputs. But the implementer's own 1.21.1-fabric run shows the HUD painting over the effect icon, as on 1.20.1. The docs report this as a cloud confirmation of the opposite (Important 1), and the expectation the brief imposes for 1.21.1 is wrong on Fabric (Important 2, plan-mandated, needs a ruling).
