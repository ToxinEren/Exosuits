You are reviewing part 4d's station work for the Minecraft mod Redstone Exoframes (repo /home/user/exosuits, four variants from one tree: 1.20.1-forge, 1.20.1-fabric, 1.21.1-fabric, 1.21.1-neoforge; docs in Italian). The piece is one task plus its preview commit, so this review is both the task review (spec compliance and quality) and the final whole-range review before a fast-forward to `main`.

## What was requested

Read, in this order:
1. `/home/user/exosuits/.superpowers/sdd/m4d-anteprime/brief.md`: the preview job (commit `ef73434`). It wrote a station generator in `tools/art/` with three art directions and previews for the owner.
2. `/home/user/exosuits/.superpowers/sdd/m4d-anteprime/integrazione-brief.md`: integration of the owner's choice, direction A "Officina" (commit `704cef7`).
3. `/home/user/exosuits/.superpowers/sdd/m4d-anteprime/integrazione-fix-brief.md`: a fix round before this review (commit `70cbd3d`). It removes one-pixel cracks: T-junctions between faces let the sky show through the block.

Global constraints that bind this work (verbatim from the design `docs/superpowers/specs/2026-11-19-m4-stazione-design.md` and the project's rules):
- §3: "Grafica generata da script nel repository, file generati committati e rigenerati identici dalla CI, PNG senza metadati; anteprime al proprietario prima dell'integrazione (DEC-09, D8)."
- §5.1: "Due metà affiancate in orizzontale, come un letto ma per il lungo davanti a chi la posa" … "la laterale alla destra di chi posa guardando la stazione (`pos.relative(FACING.getCounterClockWise())`)" … "Forma di collisione e di contorno: un cubo pieno per metà" … "nessuna luce" … "modello dell'oggetto piatto, un'icona".
- §6, row 4d: "La grafica scelta dal proprietario è generata da script, controllata da `check_art.sh` e integrata". The GUI texture, the HUD drawings, the icons and the glow layer are later units of 4d, not this one.
- The owner's anti-goals: no recognisable Fallout, Iron Man or Halo shapes; no gantry or frame wrapping the suit; not the yellow-and-black scheme; the block emits no light (bright redstone texels are paint only).
- Stonecutter conditions only in `compat/impl/` (subfolders included), `platform/`, `compat/Ids.java`; no new mixins; this work should need no Java change.
- Every committed PNG has only IHDR, IDAT, IEND chunks; generated files are regenerated identically by `tools/check_art.sh`.
- No personal data, and no AI model or vendor names, in docs or code (commit trailers excepted); the owner's reference image and other projects' assets never enter the repo.

## What the implementers claim

Read `/home/user/exosuits/.superpowers/sdd/m4d-anteprime/anteprime-report.md` (preview job) and `/home/user/exosuits/.superpowers/sdd/m4d-anteprime/integrazione-report.md` (integration and its `## Fix round 1`). Treat both as unverified claims, and check them against the diff. In-game shots and run folders are in `/home/user/exo-runs/m4d/`; the Read tool shows images, so look at what you need to judge a claim.

## Diff under review

Base `ef53e6f` (main), head `70cbd3d`. Package: `/home/user/exosuits/.superpowers/sdd/m4d-anteprime/review-package-ef53e6f-70cbd3d.txt` (the two generated block models, `station_main.json` and `station_side.json`, are left out of its full diff because they are 2500 lines of generated JSON; `tools/check_art.sh` regenerates them and compares them byte for byte, and you can read them in the checkout if a risk needs it) (the commit list, the stat summary, and the full diff with context; binary files show as changed). Read it once. Inspect code outside the diff only to check a concrete risk you can name (one focused check per risk, and say what you checked). Your review is read-only on this checkout: do not touch the working tree, the index, HEAD or branches. If you need another revision, use `git worktree add /tmp/review-<sha> <sha>`.

## Tests

The implementers ran the builds, 381 JUnit per variant, the four `check_*.sh` scripts, the selftests, `run_gametests.sh` (4×235 on `704cef7`), the client sequences and the dedicated server, and their outputs are in the reports. Do not re-run suites. Run a focused command only when the code raises a specific doubt that no existing run answers (for example `python3 tools/art/station.py --direction officina --out <tmp>` and a comparison, or `bash tools/check_art.sh`), and never a Gradle build, a client or a server.

## You do not dispatch subagents

Do the whole review yourself, in passes if needed.

## Calibration and output

- **The spec is a vision document.** Where it is silent, judge by what a reasonable person using this would expect: a player looking at the station, the owner's PC tester following the checklist, a maintainer regenerating the art. A reasonable person's expectation is a requirement.
- **Severity:** Critical / Important / Minor. Important means this cannot be merged as is: incorrect or fragile behaviour, a missed requirement, maintainability damage you would block a merge over.
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

Write the full review, in the output format above, to `/home/user/exosuits/.superpowers/sdd/m4d-anteprime/review.md`. Your final message is short: the spec-compliance verdict (✅/❌, plus the ⚠️ items in one line each), the merge verdict, and one line per Critical or Important issue (file:line and the problem). Count the Minor issues, and leave the rest in the file.
