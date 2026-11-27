# Part 4d integration, fix round 1: a station without cracks

Your commit `704cef7` is pushed. Your concern 1 is a correctness defect (the sky shows through the block, and in motion the dots flicker), so it is fixed before the review. Concerns 2 and 4 stay as observations; concern 3 gets one check in game (step 3). The rules of `integrazione-brief.md` still bind (no Java change: if one seems needed, stop and report; PNGs only IHDR/IDAT/IEND; no personal data, no model or vendor names; one Gradle, client or server at a time; signed commits, never `--no-verify` or `-c commit.gpgsign=false`, never set an identity; do not push; no subagents).

## Steps

1. **Faces without T-junctions, by construction.** Change `tools/art/bench.py` so that no vertex of any emitted face lies strictly inside an edge of another face, across the whole bench: both halves together in their in-game placement, so the seam between the halves conforms too. The generator refuses to write faces with a T-junction (the check runs on every generation, so `check_art.sh` fails on one), and `tools/art/selftest.py` gets a case that builds a T-junction and expects the refusal. Reference: splitting today's faces naively (each face cut along the perpendicular line through every vertex that lies inside one of its edges, repeated until none is left) gives about 576 faces for `station_main` and 316 for `station_side`, from 84 and 66; a smarter split may give fewer. If a half would go above 1000 faces, stop and report before committing. The look does not change: the same texels on every visible face, UVs split with the faces.
2. **Into the assets, and in game.** Regenerate into `src/main/resources/assets/redstone_exoframes/`; `bash tools/check_art.sh` → `OK art`. On each of the four variants: the station shots of your step 3 and `EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh <variant> <folder>` (its `ok` lines, `OK <variant>`, an empty `log-check.txt`). `crepe.py` on every station shot of the four variants and on the fourteen extra views on 1.21.1-neoforge → 0 isolated sky pixels everywhere; also look at the enlarged crops where the cracks were. If a crack is still there after the change, stop and report with the crops (no second attempt).
3. **The other three facings.** On 1.21.1-neoforge, place the station facing east, south and west (main half where clicked, side half where the game puts it) and take one three-quarter front shot of each: same look as north, side half to the right of whoever faces the front, nothing visible through the seam.
4. **Reference shots.** Compare the ten shots in `docs/test-in-gioco/m4a1-screenshot/` with the new 1.21.1-neoforge and 1.20.1-forge runs (`compare -metric AE`); redo, without metadata, those that differ, and say which.
5. **Docs.** `tools/art/README.md`: one or two sentences on why the station's faces have no T-junctions (a crack along one shows the sky, since nothing is drawn behind the surface) and that the generator refuses them. The note at the top of `docs/test-in-gioco/m4a1-banco.md`: keep asking the PC tester to report light dots on the station, adjusted if it mentions the cracks as known.
6. **Commands (exact):** `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` → `BUILD SUCCESSFUL`; 381 JUnit on each variant (the count command of `integrazione-brief.md`); `bash tools/check_sources.sh` → `OK sources`; `bash tools/check_jars.sh` → `OK jars`; `bash tools/check_data.sh` → `OK data`; `bash tools/check_art.sh` → `OK art`; `bash tools/client/client_shot.sh --selftest` → `OK passi`. No gametests and no dedicated server this round: the server never reads block models, and both ran green on `704cef7`.

## Commit and report

One commit, e.g. `tools, client: la stazione senza crepe (facce senza giunzioni a T); scatti rifatti`, ending with exactly:

```
(prima riga di attribuzione dell'ambiente)
(seconda riga di attribuzione dell'ambiente)
```

`git status --short` empty. Append a section `## Fix round 1` to `integrazione-report.md`: the approach, the face counts and model file sizes before and after, the T-junction check and its selftest, the `crepe.py` results per variant, the paths of the three facing shots, which reference shots changed, the PNG chunk check of every new or changed PNG, the command outputs, concerns. Return only: status, commit, a one-line verification summary, the paths of the three facing shots, concerns.
