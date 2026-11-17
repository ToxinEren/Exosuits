# Final review: m3d-braccio (e064c9e..ffe2ab2), Milestone 3d, the first-person arm

Reviewer scope: the whole branch (three tasks), against the plan header (lines 1-89), design section 13 and the coordinator's rulings in the ledger. I did the review alone, in passes: production code, the hook and the jars, GeckoLib state, the tools, the documents and the checklist, then the deferred minors. I ran nothing that the rules forbid. What I checked by tool:
- `javap` on the eight game jars in the Gradle caches (vanilla, Forge 47.4.29, NeoForge 21.1.266, intermediary, SRG);
- the GeckoLib 4.8.4 (Forge and Fabric) and 4.9.3 sources jars;
- the four built mod jars (target strings, and `SCALE`/`SIDE_SHIFT_PX` with `javap -constants`);
- the Italian language file named by both asset indexes (1.20.1-5 and 1.21.1-17 point to the same object, d4de73d6);
- the reference screenshots, plus the leftover cloud shots in /home/user/m3d-scatti, measured with PIL in memory.

What I did not re-run, and take from the task reports and reviews: the build, the 331 JUnit tests per variant, the 208 game tests, `OK sources`, `OK jars`, the four `m3d_shots.sh` runs, and the dedicated server started up to `Done (`. For check_sources rules 4, 7 and 8 I applied the regular expressions by grep to the nine changed sources myself.

## Strengths

- **The hook is right on all four variants.** I re-checked it with javap.
  - `PlayerRenderer.renderHand` has the same body on vanilla 1.20.1, vanilla 1.21.1, Forge and NeoForge. It contains exactly one `invokevirtual PlayerModel.setupAnim(LivingEntity;FFFFF)V`, at offset 42, followed by `ModelPart.xRot = 0`.
  - On Forge and NeoForge the only callers are `renderRightHand` and `renderLeftHand`. Each fires `renderSpecificFirstPersonArm` and then calls `renderHand`. The only callers of those two are `ItemInHandRenderer.renderPlayerArm` and `renderMapHand`. I scanned every class of the jars.
  - The remapped names are exactly what `tools/check_jars.sh` (e2) pins: `method_23205` and `class_591.method_17087` on Fabric (1.20.1 and 1.21.1), `m_117775_` and `m_6973_` on Forge, and the Mojang names on NeoForge. The four built jars carry them.
  - `GameRenderer.renderItemInHand` keeps out the spectator, F1, third person and sleeping cases, on 1.20.1 and on 1.21.1. `ItemInHandRenderer` keeps out invisibility: `renderArmWithItem`, `renderOneHandedMap` and `renderTwoHandedMap` all check `isInvisible`. So the missing spectator check in `FirstPersonArm` is safe.
- **A frame is clean from start to end** (`SuitRendererImpl.java:80-125`).
  - Every draw sets `hidden` and `childrenHidden` on all 86 names of `BONES`, in both modes. `GeoBone.setHidden` does both on 4.8.4 Forge, 4.8.4 Fabric and 4.9.3. The geometry has exactly 86 bones: 6 top-level, 6 frame, 72 piece and 2 decals. I checked all 86 parents: each frame, piece and decal bone hangs directly under its own slot's top-level bone.
  - `arm` goes back to `null` in the `finally`, together with the five fields that GeckoLib nulls itself on Fabric and 4.9.3 but not on Forge 4.8.
  - `young`, `crouching` and the six-part pose are copied again from the base model on every `preRender` (`applyBaseModel`, identical on the three GeckoLib copies). So nothing from the arm draw leaks into the inventory preview, a placed frame or another player's suit later in the same frame.
  - The two changes to the player model, `young = false` and `arm.xRot = 0`, are either harmless or rewritten by the next `setupAnim`.
- **The draw cannot throw into the frame** (`FirstPersonArm.java:188-217`).
  - `SuitLayer.suitView` catches. Everything else, including the holder's `ExceptionInInitializerError` (a `LinkageError`), is caught.
  - The work happens on a private `PoseStack`. After an error the method returns `false`, so the base arm is drawn.
- **The placement arithmetic is sound.** `translateAndRotate`, then the shift and the scale, then the inverse rotation and translation, then GeckoLib's bone pivot and rotation: together they reduce to `pose·T(a)·R·T(p)·S`. That is the arm's own system, as the Javadoc says. The rotation constants of `renderPlayerArm` (Y 45, Z 120, X 200, Y -135) are identical in both versions' bytecode.
- **The rules are kept.** No new Stonecutter directive. Every changed source is in a client package, so rules 4, 7 and 8 hold. No new log line: the code reuses `logFailureOnce`. No new mixin class.
- **The tools are well designed.**
  - `arm_measure.sh` has a real self-test.
  - In `m3d_shots.sh` every threshold sits next to its measured value. The 12 s wait is explained by a measurement, and every measure failure counts as KO.
  - I checked on the leftover cloud shots (1.21.1-neoforge) that the "base arm drawn too" regression can be caught. 1 092 of the 18 075 base-arm pixels lie outside the suit silhouette, against the `solo-tuta` limit of 904, so even a fully hidden base arm would fail it. `nudo` leaves 5 399 outside, a wide margin.
- **The documents agree with the code.** Section 13 of the design, `docs/compat.md` and the Javadoc of `SCALE` and `SIDE_SHIFT_PX` match each other figure for figure: the six candidate rows, 286x205+674+335, x 886, edge from y 407 (418), and 4 px clear of the hotbar rectangle. The stale "first person does not change" sentences are fixed in §2, §5.2, §7.6, `PilotPose`, `compat.md` and `m3a-vedere-il-telaio.md`.

## Issues

### Critical (Must Fix)

None.

### Important (Should Fix)

None.

### Minor (Nice to Have)

**FF-1. "Clear of the hotbar" holds only at the measured window. The checklist and the README state it without that condition.**
- Where: `docs/test-in-gioco/m3d-braccio.md:69` (step 3: "fuori dalla barra degli oggetti") and `:48` ("in proporzione all'altezza della finestra, nella stessa posizione rispetto ai bordi"); `README.md:9` ("fuori dal mirino e dalla barra degli oggetti").
- What I measured. In the 960x540 shots the suit arm's left edge is at x 674 over rows 460-539. The base game's own arm is at x 677-679 over the same rows. The hotbar ends at x 662 (GUI scale 2) and the plan's rectangle at x 670.
- Why it changes with the window (javap on both versions):
  - `renderItemInHand` calls `getFov(camera, pt, false)`, which is 70 whatever the FOV option says, and `getProjectionMatrix` calls `Matrix4f.perspective` with the window's width/height. The 70° is therefore vertical, and the hand's offset from the screen centre scales with the window height: 194 px at 540 rows.
  - The hotbar is 91 GUI px from the centre to its right end, times an integer GUI scale. `Window.calculateScale` gives the largest scale that keeps width/scale ≥ 320 and height/scale ≥ 240. "Automatica", the default, therefore picks floor(H/240) on 16:9 screens.
- What follows. At the launcher's default 854x480 window (auto scale 2) the suit forearm's lower-left corner sits about 10 px behind the last hotbar slot (x ≈ 599 against 609, rows 436-479). It does about 14 px at 1280x720 and 29 px at 2560x1440. At 1920x1080 it is clear by 24 px.
- The base game's arm does the same at those sizes, only 3 px less. So nothing changes for a player compared with vanilla, and I do not reopen the size ruling.
- Why it matters. An assistant following step 3 literally on the default window will mark the step as failed. The "same position relative to the edges" sentence is true only for the hand, not for the hotbar, and only at 16:9.
- Fix, wording only:
  - Step 3: "fuori dal mirino; in basso a sinistra l'avambraccio arriva fin dove arriva il braccio del gioco base al passo 2: con una finestra di altra misura, o con certe scale dell'interfaccia, il suo angolo può finire sotto l'ultima casella della barra, come quello del gioco base".
  - `:48`: say that the hotbar grows in steps with the interface scale, and that at other sizes only the hand scales.
  - `README.md:9`: "fuori dal mirino e, a 960×540, dalla barra degli oggetti".
- If the coordinator wants the corner always clear, that would be a new ruling, not part of this fix. By my arithmetic `SIDE_SHIFT_PX` 5.0 would be the smallest step that might do it, with a margin of about 1 px; I did not measure it.
- Confidence: likely. The positions come from projection arithmetic checked against the 960x540 shots; I did not run other window sizes.

**FF-2. A half-updated sentence in the `PilotPose` class Javadoc.**
- Where: `src/main/java/com/toxineren/exoframes/client/PilotPose.java:28-31`.
- What's wrong. The Javadoc still says "Tutta la logica sta qui. I due mixin client (...) fanno una sola chiamata per aggancio ai metodi on... di questa classe". The third hook of `PlayerRendererMixin` now calls `FirstPersonArm.onHandPrepared`, which is logic outside this class. The sentence added after it softens the claim but does not correct it.
- Fix: "La logica del pilota sta qui: gli agganci del pilota (...) chiamano i metodi on... di questa classe; il terzo aggancio di PlayerRendererMixin, il braccio in prima persona, chiama FirstPersonArm."
- Do it in the same commit as T1-F2 and T1-F3 below.

**FF-3. Checklist step 7 does not mention the second HUD line that state 2 shows.**
- Where: `docs/test-in-gioco/m3d-braccio.md:73`.
- What's wrong. `/exoframe debug wear rottame 2 …` makes every piece critical. The 3c HUD then shows a second, yellow line under the energy line (`EnergyHud.java:22-23`; `m3a-vedere-il-telaio.md:7` says the same). Step 7 does not mention it, so a literal follower may report it as unexpected.
- Fix: one sentence saying the line belongs to part 3c.
- Confidence: likely.

**FF-4. Mobs in the survival test world can wear the real frame's arm piece during steps 14-19.**
- Where: step E and step 2 (`m3d-braccio.md:58`, `:68`); the expectation of step 14 ("come al passo 4").
- What's wrong. The test world is survival superflat with mobs on. Slimes spawn there, and hostile mobs come at night, during a 19-step session. A mob hit goes through `Player.hurtArmor`, which `PlayerSuitMixin.java:28-30` hooks to `SuitDamage.onArmorHurt`. That can wear the real frame's Braccio Rottame from step 14, and the piece would no longer look intact as step 4 shows it.
- How much one hit wears, and whether it would change the visual state within the session, I did not check.
- Fix (optional): `/gamerule doMobSpawning false`, or "Difficoltà: Pacifica", at step 2. The 3b and 3c checklists use the same world, so this is not new in this part.
- Confidence: possible.

## Cases checked, none with a finding

- **Left-handed player.** `ItemInHandRenderer` passes the player's main arm to `renderPlayerArm`, which calls `renderLeftHand` and so `renderHand(model.leftArm, …)`. `FirstPersonArm.java:195` then picks `left = arm == model.leftArm` and the `LEFT_ARM` slot. The mirrored constants are pinned by JUnit (`constantsAreAmongTheDecidedValuesAndTheLeftArmMirrorsTheRight`); the run checks are `mancino` and `specchio`.
- **Off hand.** With an empty off hand the base game draws no off-hand arm, because `renderArmWithItem` draws the player's arm for the main hand only. With a map in the off hand, `renderOneHandedMap` draws the off arm through the same hook, with that side's piece.
- **Map in one hand and in two.** `renderOneHandedMap` and `renderTwoHandedMap` (two `renderMapHand` calls) both go through `renderHand`. Each call decides its own side.
- **Entering or leaving the suit between two frames.** `FirstPersonArm` keeps no state. `SuitLayer.suitView` reads the chest slot on every call; its cache is keyed by the stack and checked with `ItemStack.matches`.
- **No arm piece, or a broken one.** `visibleForArm` shows the frame arm always and delegates the piece bones to `visible`: the three bones of the mounted piece's state, for any of the four states. JUnit pins it (`armDrawWithoutTheArmPieceShowsTheFrameArmOnly`, `armDrawFollowsTheStateOfItsOwnPiece`).
- **Painted colours.** `getColorForBone` reads the same `view` field that the draw sets (`SuitRendererImpl.java:133-136`).
- **Spectator, F1, third person, sleeping, invisible.** The base game never calls the hook in these cases (javap, see Strengths).

**Italian labels in the checklist.** I checked every quoted label against `minecraft/lang/it_it.json`. These are right: "Opzioni...", "Mano principale", "Sinistra", "Destra", "Fatto", "Giocatore singolo", "Crea un nuovo mondo", the "Gioco" and "Mondo" tabs, "Modalità"/"Sopravvivenza", "Comandi: Sì", "Tipo: Piatto", "Strutture: No", "Rete locale...", "Apri il mondo in rete locale", "Multigiocatore" and "Campo visivo". The mod's own texts match the mod's `it_it.json`: "Indossi il telaio di rottame", "Indossi il telaio nudo", "Hai piazzato un telaio di rottame", "Eliminato il telaio che indossavi…", "Telaio", "Braccio Rottame", "Cella di redstone", and the tab "Redstone Exoframes". Two are wrong: "Personalizzazione skin..." should be "Aspetto..." (T3-F1), and step 16 gives no Italian for "Video Settings... → GUI Scale", which is "Grafica... → Scala" (T3-F2).

## Deferred minors: triage

| Id | Verdict | Reason |
|---|---|---|
| T1-F1 | obsolete | Ruled at Task 2 (12 s first wait); the notice and the wait are documented in the `m3d_shots.sh` head comment and in `tools/client/README.md`. |
| T1-F2 | fix | Still true at HEAD (`SuitLayer.java:67`, "Per PilotPose"). `FirstPersonArm.onHandPrepared` relies on its null. One line. |
| T1-F3 | fix | Still true at HEAD (`SuitRenderer.java:10-13`). The class Javadoc names only the worn suit and the placed frame, "dentro il disegno dell'entita'", but `renderArm` is called from the first-person hand draw. One line. |
| T2-F3 | leave | The FAIL verdict is right; only the diagnosis of which checks break is lost. It matters only when a run already failed. |
| T2-F4 | leave | A failed measure is rare (wrong file or size). Running `arm_measure.sh` by hand shows why. |
| T2-F5 | fix | Breaks a stated Global Constraint, and every other client tool names its functions in English. Renaming `esito`/`mis` (`m3d_shots.sh:206`, `:224`) costs minutes. |
| T2-F6 | obsolete | The `EXO_SHOT_SERVER_CONFIG` row of `tools/client/README.md` now lists `m3d_shots.sh`. |
| T3-F1 | fix | Still at `m3d-braccio.md:17`, `:78` (and `:80` by reference). The vanilla `options.skinCustomisation` is "Aspetto..." on both versions, so an assistant on an Italian client cannot find "Personalizzazione skin...". |
| T3-F2 | fix | Still at `:85`: no Italian path is given ("Opzioni... → Grafica... → Scala"). GUI scales 3 and 4 are not offered at the default 854x480 window, so do the scale part after F11 and say "annota il massimo offerto". |
| T3-F3 | fix | Still at `:46` and `:48` ("uguali al pixel" for the shots, "nella stessa posizione rispetto ai bordi"). Same edit as FF-1. |
| T3-F4 | fix | Still at `:87`. At step 18 the host wears the step-14 frame with one arm piece, while "intera" for the other player means "due braccia, tutti i pezzi". |
| T3-F5 | fix | Still at `m3d_shots.sh:62-67` (208-226 s, 23595, 170-172…); now disagrees with `tools/client/README.md`. Three numbers. |

## Recommendations

- Put T1-F2, T1-F3, FF-2 (Javadoc), T2-F5 (renames; check them with `M3D_SOLO_CONTROLLI=1` on existing shots, no client run needed), T3-F5, and the checklist edits (T3-F1 to T3-F4, FF-1, FF-3, and FF-4 if wanted) in one small documentation commit before the merge. None of them changes behaviour.
- The `solo-tuta` check passes with a margin of about 20 % (1 092 base pixels outside the suit silhouette against the limit of 904). `nudo` is the robust guard for Review Focus 3. If size or place changes again, recheck `solo-tuta`'s margin, or base it on `nudo`.

## Declined to judge

- The invisible player in the suit sees no arm. This is base-game behaviour, and §13 says the suit arm appears "dovunque il gioco base disegna" the arm.
- A Forge or NeoForge mod that cancels `RenderArmEvent` stops `renderHand` from being reached, so neither the suit arm nor the base arm is drawn: that mod's own arm wins. This is reasonable and not specified.
- In the two-handed map pose the suit arms enter the hotbar rectangle (`1.20.1-forge-mappa.png`). Criteria (a)-(c) and the hotbar ruling are for the rest pose, and owner question 4 asks about the map.
- With the bare frame, the first-person view hides the pilot's arm while third person shows it. This is a §13 decision; owner question 3 asks about it.
- The slim-skin hand centre is half a pixel off. This was ruled.
- An injection point on a call instead of at the method head, and its intolerance of mods that redirect `setupAnim` with `defaultRequire` 1. This was ruled.
- `m3d_shots.sh` cannot see a primary/secondary colour swap, because both colours are FF00FF. Checklist step 7 (3A6EA5/D9D9D9 against third person) covers it by eye.
- An exception partway through the GeckoLib draw could leave half-written vertices with the base arm drawn on top. This is the same risk `SuitDraw.draw` already accepts; no run can provoke it.
- On 4.9.3 the `finally` of `SuitRendererImpl.draw` does not clear `bufferSource`/`partialTick` after a failed draw. The code predates this part, and the next `prepForRender` overwrites both.
- Arm size and margin at aspect ratios other than 16:9, beyond the wording in FF-1. Design §13 fixes the measuring window at 960x540, and step 16 asks the owner to look.

## Assessment

**Ready to merge?** With fixes

**Reasoning:** The code, the hook and the tools are correct on all four variants. A frame ends clean after the arm draw, and no exception can reach it. What remains is documentation: the T1-F2/T1-F3/FF-2 Javadoc, the T2-F5 shell renames, the T3-F5 head comment, and checklist labels and expectations that would stop an assistant who cannot ask (T3-F1, T3-F2, T3-F4, FF-1, FF-3, optionally FF-4). None of it needs a ruling.
