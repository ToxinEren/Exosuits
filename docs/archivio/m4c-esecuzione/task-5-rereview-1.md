# Task 5 re-review, fix round 1 (7e33121..8ecd206)

## Finding Verdicts

- **Important 1, docs claim a cloud verification that 1.21.1-fabric contradicts** — ADDRESSED. m4c-hud.md:63 now says the icon is sharp above the vignette only on 1.21.1-neoforge, and that on 1.21.1-fabric, 1.20.1-fabric and 1.20.1-forge the vignette covers it, "uguali al pixel" in `60x60+900+0`. m4c-hud.md:89 (step D) names `1.20.1-forge-vignetta-effetti.png` as the reference for that corner on 1.21.1-fabric. The list sentence at about :89 of the reference shots carries the same exception. tools/client/README.md:189 replaces "uguale sulle due varianti di ciascuna" with the loader-dependent fact plus the cause. task-5-report.md:102 now says the icon is "diversa da fabric 1.21.1", with the correction noted; the old wording survives only as a quoted correction. I ran a focused check on the existing shots: pixel (930,25) green channel is 76 on 1.21.1-neoforge and 65 on 1.21.1-fabric, 1.20.1-fabric and 1.20.1-forge. The new README text matches it. The old "68 on 1.20.1" is now corrected to the measured 65. A grep for the old phrases in the five carriers and the report finds no stale statement.
- **Important 2, per-version expectation wrong for 1.21.1-fabric** — ADDRESSED. m4c-hud.md:26 now says "Dipende dal loader, non dalla versione": above on NeoForge 1.21.1, under on Fabric 1.21.1 and on both 1.20.1; toasts above on all four; no saving indicator on Forge 1.20.1. Step 15's three sub-steps (subtitles, effect icon, saving indicator) are loader-specific, and the subtitles step marks the Fabric 1.21.1 "sotto" as deduced and says to note what is seen. Carriers outside the checklist are corrected:
  - SuitHud Javadoc (comment only, no code change).
  - docs/compat.md:248, both the group paragraph and the corners sentence.
  - Design §5.12 "Dove" and "Vignetta", and §10 risk.
  - m4c_shots.sh phase-11 comment (comment only).
  - README m4c_shots paragraph at :189.
- **Cannot-verify item, subtitles and saving indicator on 1.21.1-fabric** — ADDRESSED. The checklist expects "sotto" on Fabric 1.21.1 and states it was not seen in the cloud. R21 puts the sub-steps and step 21 (F3) on a 1.20.1 variant plus each 1.21.1 variant. Steps 13, 14, 20 and 22 keep "a 1.20.1 and a 1.21.1". The skip routing is given per variant, e.g. "dal 12 al 15, dal 19 al 21 e dal 21 al 23". Compat and design state the mechanism (HudRenderCallback at the tail of Gui.render after the depth test is off, against RenderGuiEvent.Post inside the layer manager with the test on).

Fix report: the "Fix round 1" and "extension (R21)" sections show the rerun `arm_measure.sh` comparisons (0 px among the three, 816 px against neoforge), `bash -n` on m4c_shots.sh, `check_sources.sh` giving `OK sources`, grep results, and git status. No Java was changed beyond the Javadoc, so a build was not needed. The claims match the diff.

## New Breakage in the Fix Diff

None Critical or Important.
- Minor, m4c-hud.md:136 (new step 21 wording): "su NeoForge 1.21.1 le righe di testo di F3 ... possono coprire il gruppo" is an expectation from bytecode, not seen in any shot. It is hedged ("possono") and framed as something to note, so a literal reader will not misfollow it.
- Minor, m4c-hud.md:93 (variant paragraph): "una 1.20.1 e una 1.21.1" followed by "sull'altra 1.21.1" is wordy. The routing is internally consistent, and step 15's saving-indicator variants are explicit (1.20.1-fabric, 1.21.1-neoforge, 1.21.1-fabric).

## Out-of-Scope Observations

None.

## Verdict

**Fix round:** All findings addressed, no new Critical/Important breakage.
