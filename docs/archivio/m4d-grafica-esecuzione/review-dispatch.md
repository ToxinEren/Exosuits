You are reviewing part 4d's interface art for the Minecraft mod Redstone Exoframes (repo /home/user/exosuits, four variants from one tree: 1.20.1-forge, 1.20.1-fabric, 1.21.1-fabric, 1.21.1-neoforge; docs in Italian). The unit is a preview job, the owner's approval and one integration task. This review is both the task review (spec compliance and quality) and the final whole-range review before a fast-forward to `main`.

## What was requested

Read, in this order:
1. `/home/user/exosuits/.superpowers/sdd/m4d-grafica/brief.md`: the preview job (commit `74a890c`). It wrote generators in `tools/art/` for the station screen texture, the HUD drawings, the helmet vignette and the kit icon, in the style of the station the owner chose (direction A, "Officina"), plus previews for the owner. Nothing went into the game in that commit.
2. `/home/user/exosuits/.superpowers/sdd/m4d-grafica/integrazione-brief.md`: integration after the owner's approval (commit `e58533c`). The screen texture, the HUD drawings and the vignette go into the game; the kit icon waits for part 4b, whose kit item does not exist yet.

Commit `3c7a0d5` in the range is a controller edit to the design document: it records the owner's answers of 27 November (no slots in the station; question 11; HUD position; energy colours). Check it only for agreement with the code.

The owner's decisions of 27 November 2026 that bind this work:
- The HUD sits at the bottom, in the corner opposite the main hand.
- Energy is green, red under 10 %, and grey at zero.
- The A-style screen, HUD and vignette go into the game.
- He keeps two departures from design §5.12: the plate's copper edge at 71 % opacity instead of 43 %, and a bevelled steel channel for the bar frame instead of the flat `0C0D0F` border.
- A mostly green HUD at full energy is fine.

The controller also ruled that every sprite of the sheet gets a named constant now, so that parts 4a-2 and 4b draw their new widgets from the same sheet.

Global constraints that bind this work, verbatim from the design `docs/superpowers/specs/2026-11-19-m4-stazione-design.md`:
- §3: "Grafica generata da script nel repository, file generati committati e rigenerati identici dalla CI, PNG senza metadati; anteprime al proprietario prima dell'integrazione (DEC-09, D8)."
- §3: "Codice solo client nei quattro package client, mai nominato dal codice comune (regole 7 e 8): la schermata, l'HUD, i tasti e il file delle preferenze del client stanno lì".
- §3: "direttive solo in `compat/impl/`, `platform/`, `compat/Ids.java` (regole 2 e 6 di `tools/check_sources.sh`)".
- §3: "Ogni cucitura nuova entra in `docs/compat.md` nello stesso task (DEC-06)."
- §3: "'Nessun test in mondo può vedere un disegno' (DEC-45): ciò che si vede lo provano il client vero nel cloud (scatti e controlli fatti da script) e la checklist."
- §5.3: "Schermata di 316x232 pixel dell'interfaccia, centrata." … "a 960x540 e scala 2 sta in 480x270 con `leftPos` 82 e `topPos` 19".
- §5.12, "Dove": "Un gruppo di 88x35 pixel dell'interfaccia, a 2 pixel dal bordo laterale" … "`y0 = h − 37`; mano principale destra `x0 = 2`, sinistra `x0 = w − 90`."
- §5.12, the elements table, the "Grafica della parte 4d" paragraph and the colour legend: green `55FF55`, yellow `FFFF55`, red `FF5555`, grey `8A8A8A`. On the bar, yellow never appears.
- §5.12, "Vignetta": "Solo con `FULL`, in prima persona e con la preferenza accesa. Quattro angoli di 72x40 pixel dell'interfaccia" … "al più 0,42 di opacità nel pixel d'angolo, a zero sul lato opposto del pezzo".
- §14, "Per la 4d": the items left for this part by 4c's final review.
- Project rules:
  - Every committed PNG has only IHDR, IDAT and IEND chunks.
  - There is no personal data, and no AI model or vendor names, in docs or code (commit trailers excepted).
  - No new mixins.

## What the implementer claims

Read `/home/user/exosuits/.superpowers/sdd/m4d-grafica/report.md` (preview job) and `/home/user/exosuits/.superpowers/sdd/m4d-grafica/integrazione-report.md` (integration). Treat both as unverified claims, and check them against the diff.

The committed reference shots are in `docs/test-in-gioco/m4c-screenshot/` and `docs/test-in-gioco/m4a1-screenshot/`. The live run folders are under `<cartella temporanea della sessione>/final/`. The previews are in `docs/anteprime/` or where the report says. The Read tool shows images, so look at what you need to judge a claim, for example whether the screen in game matches its preview.

Not done yet, and known: the client sequences of parts 3c and 3d (`m3c_shots.sh`, `m3c2_shots.sh`, `m3c_rules_shots.sh`, `m3d_shots.sh`) have not been rerun with the new HUD and vignette, and their reference shots still show the 4c HUD. They run in the fix round after this review. Where the diff lets you judge their checks (for example `arm_measure.sh`'s `hud=` rectangle, `m3d_shots.sh`'s criterion (b) and its vignette corner, the HUD shots of `m3c_shots.sh` step 8), say what you expect to break.

## Diff under review

Base `e90e46d` (main), head `e58533c`. Package: `/home/user/exosuits/.superpowers/sdd/m4d-grafica/review-e90e46d..e58533c.diff`, 5454 lines. It holds the commit list, the stat summary, and the full diff with context; binary files show as changed. Read it once, in chunks. Inspect code outside the diff only to check a concrete risk you can name, one focused check per risk, and say what you checked.

Your review is read-only on this checkout: do not touch the working tree, the index, HEAD or branches. If you need another revision, use `git worktree add /tmp/review-<sha> <sha>`.

## Tests

The implementer ran the builds, 386 JUnit per variant, the four `check_*.sh` scripts, the selftests, `run_gametests.sh` (4×235), the client sequences m4c and m4a1 on four variants, and the dedicated server. The outputs are in the report. Do not re-run suites.

Run a focused command only when the code raises a specific doubt that no existing run answers, for example:
- `python3 tools/art/gui.py --map`;
- regenerating into a temporary folder and comparing;
- `bash tools/check_art.sh`.

Never run a Gradle build, a client or a server.

## You do not dispatch subagents

Do the whole review yourself, in passes if needed.

## Calibration and output

- **The spec is a vision document.** Where it is silent, judge by what a reasonable person would expect: a player reading the HUD in a fight, a player using the station screen, the owner's PC tester following a checklist, a maintainer regenerating the art or adding a widget in part 4a-2. A reasonable person's expectation is a requirement.
- **Severity:** Critical, Important or Minor. Important means this cannot be merged as is: incorrect or fragile behaviour, a missed requirement, or maintainability damage you would block a merge over.
- **Rationales are claims.** A stated rationale never downgrades a finding.
- Acknowledge what was done well.

Output:

### Spec Compliance
✅ or ❌ (what is missing, extra or misunderstood, with file:line), and ⚠️ for what you could not verify, with what the controller should check.

### Strengths

### Issues
#### Critical
#### Important
#### Minor
For each: file:line, what is wrong, why it matters, how to fix.

### Declined to judge
One line for each behaviour you considered and set aside, with the reason.

### Assessment
**Ready to merge?** Yes | No | With fixes. **Reasoning:** one or two sentences.

Begin directly with the spec-compliance verdict, and add no preamble.

## Where the review goes

Write the full review, in the output format above, to `/home/user/exosuits/.superpowers/sdd/m4d-grafica/review.md`.

Your final message is short:
- the spec-compliance verdict (✅/❌), plus the ⚠️ items in one line each;
- the merge verdict;
- one line per Critical or Important issue (file:line and the problem);
- the number of Minor issues.

Leave everything else in the file.
