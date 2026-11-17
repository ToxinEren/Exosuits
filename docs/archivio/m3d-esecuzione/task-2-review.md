### Spec Compliance

- ❌ Issues found: criterion (a) was misjudged for the chosen candidate (T2-F1), and the brief's wording of (a) cannot be met by any candidate (T2-F2, plan-mandated). Everything else in the brief is implemented as written.
  - **T2-F1**: at K 1.3 with shift 4.5 the steel palm reaches the right edge of the screen. That is the same condition the report used to reject K 1.65 with shift 6.5. FirstPersonArm.java:39-62, `misure.txt`, report §"Il giudizio su (a)".
  - **T2-F2**: the thumb is hidden behind the palm at every candidate, so "le tre dita e il pollice scuri" can never be seen.
- ✅ The rest checks out item by item:
  - `SCALE`/`SIDE_SHIFT_PX` (FirstPersonArm.java:62, :73): only the two constants and their Javadoc changed. The Javadoc numbers match `/home/user/m3d-candidati/misure.txt`. `{@link SuitScale#S}` resolves because `SuitScale` is in the same package. The constants test accepts the new values (FirstPersonArmTest.java:106-117).
  - Step 1 procedure:
    - All three round-1 candidates were measured.
    - Round 2 ran from the largest K down, with a "half a pixel below fails (b)" probe at each K (6.0/6.5 and 4.0/4.5).
    - The tool check row is 18075 px, 0/0/0, box 161x149+677+391.
    - The `<candidati>` contents are complete (`misure.txt` with an (a) line per `cand`, `k165/k130/k100.png`, the round-2 copies, `scelto-sopravvivenza.png`).
  - `EXO_SHOT_MAIN_HAND` in client_shot.sh:
    - The check at :164-168 sits in the environment section and uses `fatal`, which exits 2 (:74). It runs before `mkdir` of the output folder (:191).
    - `mainHand:"$main_hand"` is written in the options.txt block (:262).
    - The variable is listed in the head comment, both in the variable list and in the causes of exit 2.
  - m3d_shots.sh:
    - Mode is 100755. The script has `unset EXO_SHOT_SERVER_CONFIG` and `u=${EXO_SHOT_USER:-Exo}`.
    - Steps A1-A9 and B match the brief one for one. The only change is the first `@attendi 12`, accepted by the Task 2 ruling.
    - `EXO_SHOT_MAIN_HAND` is forced per run (:183).
    - All fourteen checks are present with the brief's names, measures and thresholds. `MAGENTA_MIN` = 19900. No width threshold needed lowering (318/130, 98/70, 258/120).
    - Each threshold comment gives its reason and the measured value.
    - Exits are `OK`/0 or `FAIL`/1 on stderr. `M3D_SOLO_CONTROLLI=1` works as specified.
  - README: both usage lines, the `m3d_shots.sh` paragraph and table with what it does not prove, the `arm_measure.sh` paragraph with both definitions and both rules, the `EXO_SHOT_MAIN_HAND` row and the durations.
  - The four-variant runs, checked on disk rather than re-run: `/home/user/m3d-scatti/<variante>.controlli.txt` has 14 `ok` lines and `OK` for all four. All eight `log-check.txt` files are 0 bytes. `criteri`, `nudo`, `mancino` and `specchio` are identical across the four variants.
- ⚠️ Cannot verify from diff:
  - Design §13 (design.md:733) ends with "I valori scelti e le misure li scrive qui l'esecuzione". Task 2's file list does not include the design. The controller should confirm that Task 3 writes the final values into §13.
  - `docs/test-in-gioco/m3d-braccio.md` is referenced by the README and the script but does not exist at HEAD. The design names it, so Task 3 is expected to create it.
  - K 1.0 in round 2 (the minimal shift that frees (b), and whether (a) and (c) hold there) was never run in the client. The statements in T2-F1 and T2-F2 about K 1.0 with a shift come only from the projection model; only a client run can settle them.

**How T2-F1/F2 were established.** This is one focused check of the named risk "criterion (a) of the chosen candidate".

- **The model.** I reprojected the frame arm's cubes from `tools/art/designs/frame.py:155-186`. The chain is:
  - the rest transforms of `ItemInHandRenderer.renderPlayerArm`;
  - the arm pivot (−5, 2, 0) with zRot 0.1;
  - `FirstPersonArm`'s mapping `h_v + d + K(X − h_s)`;
  - the hand projection (fov 70, 960x540).
- **The ray cast.** I ray-cast it, with occlusion, onto a 2 px grid.
- **Validation against measured `nudo` boxes.** The ray cast reproduces every measured `nudo` box I compared to within 1 px:
  - k130-s45 → x700, y334 (measured 260x205+700+335);
  - k130-s40 → 690/336 (measured +690+335);
  - k165-s65 → 704/302 (measured +703+303);
  - k165-s60 → 694/302 (measured +694+303);
  - k100-s00 → x 638-926, y 364 (measured 288x177+638+363, so the right edge 926 matches too).
- **Visible parts it gives:**

  | Candidate | Knuckles | Three fingers | Palm | Thumb |
  |---|---|---|---|---|
  | K 1.3, shift 4.5 (chosen) | x 700-854 | x 718-886 | x 718-**960**, at the right edge for y 420-458 | not visible at all |
  | K 1.65, shift 6.5 (rejected) | x 704-910 | x 726-936 | x 726-**960**, at the right edge for y 362-400 | not visible |
  | K 1.0, shift 2.5 / 3.0 / 3.5 | | | at the right edge (y 496-508 / 472-500 / 458-492) | not visible |
  | K 1.0, shift 0 | | | does not reach the edge (but fails (b)) | not visible |

- **What is measured and what is inferred.**
  - Measured: the boxes above.
  - Measured: the pixels at x 952-960. In `k130-s45-nudo.png` they are grass down to y 420, then steel grey, then a white highlight at about y 445-455, then dark from y 455. In `k165-s65-nudo.png` the same sequence starts at about y 365.
  - Measured: in `k130-s45.png` the magenta plate reaches x 960 from about y 410.
  - Inferred from the model: that the part at the edge is the palm and the thing below it the forearm vent; that the thumb is occluded; and everything about K 1.0 with a shift.

### Strengths

- Step 1 was run with the brief's discipline: both round-2 minima have their "half a pixel below fails (b)" probe. The extra `k130-s45-forge` probe showed the (b) margin is the same on another loader. Every measure in the Javadoc and the report traces to `misure.txt`.
- The checks are defensive in the right places:
  - A failed measure becomes `misura-non-riuscita`, so `val` returns -1 and the check is forced to KO (m3d_shots.sh:205-231).
  - `box` falls back to 0x0+0+0.
  - Stale left-handed shots are deleted before a run (:191).
- The KO branch was exercised on a tampered copy of the shots: eight KO lines, a `FAIL` line, exit 1, plus the missing-shot path and the usage path.
- The head comment of m3d_shots.sh explains each wait, including the measured timing of the "chat not verified" notice behind the 12 s first wait. `specchio` checks all four sides of the mirrored box (0 px off on all four variants).
- The report is candid: it names the (a) judgement as its one doubt and leaves the shots for the coordinator.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

- **T2-F1** FirstPersonArm.java:39-62 (the `SCALE` Javadoc) and the choice of K 1.3 / shift 4.5. **Criterion (a) was applied inconsistently, and by the brief's own rule the chosen candidate does not pass.**
  - **What the report says.** K 1.65 at shift 6.5 was rejected because "il palmo d'acciaio arriva a x 960 e il pollice resta fuori". K 1.3 at shift 4.5 was accepted as "la mano intera a 80 pixel dal bordo destro", with "il pollice scuro a destra".
  - **Measured in the real shots.** In both shots, at the right screen edge (x 952-960), grass gives way to steel grey, then a white highlight, then dark:
    - in `k130-s45-nudo.png` from y 420;
    - in `k165-s65-nudo.png` from y about 365.

    It is the same sequence at both candidates, so the same part of the hand reaches the edge in both. The "80 px from the edge" matches only the fingers' extent (x ≤ 886).
  - **Inferred from the ray-cast model** (see "How T2-F1/F2 were established"): the part at the edge is the steel palm (y 420-458 at 1.3, y 362-400 at 1.65), and the thumb is hidden behind the palm at both. So the thumb is neither "a destra" at 1.3 nor "fuori" at 1.65.
  - **What the brief says.** (a) requires the palm "nessuno tagliato dal bordo dello schermo" and adds "nel dubbio (una parte della mano tocca il bordo dello schermo o non si distingue) il candidato non passa". The implementer also recorded a doubt about the thumb ("pochi pixel", "che leggo come il pollice").
  - **Why it matters.** The choice of K is the decision this task exists to make, and the Javadoc's stated reason ("la mano intera a 80 pixel dal bordo destro", 1.65 rejected for the palm at the edge) is contradicted by the shots.
  - **Fix.** Do not change the constants on this finding until T2-F2 is ruled. Under the brief as written, the implementer's path is:
    1. Mark K 1.3 / 4.5 as failing (a).
    2. Run K 1.0 round 2 in the client: the minimal shift that frees (b), then (a) and (c) there. The model predicts the palm reaches the edge there as well, but nobody has run it.
    3. If that also fails, take the "nessuno passa" branch: K 1.0, shift 0, the commit "in attesa di una decisione", status BLOCKED (ruling 13).
- **T2-F2** (plan-mandated) **The brief's wording of criterion (a) cannot be met by any candidate, and the coordinator must say what (a) means.**
  - **The thumb.** (a) lists "le tre dita e il pollice scuri" as parts that must be seen. In this first-person view the thumb (frame.py:185, on the palm's inner side) is hidden behind the palm at every candidate K and shift, including K 1.0 / 0. This is inferred from the model above. As worded, (a) can never pass, and any "pass" rests on reading another dark area as the thumb, as happened here.
  - **The plate.** The brief judges (a) on `con.png`, and there the arm piece's plate covers the palm: in `k130-s45.png` the magenta plate reaches x 960 from y about 410. So the steel palm cannot be "seen whole" in the judged shot at any candidate either.
  - **The two consistent readings and what each gives:**
    - **Strict reading (the brief's text):** the palm block, or the plate over it, touches the right edge at 1.65 / 6.5 and at 1.3 / 4.5. By projection it also touches the edge at K 1.0 with shifts 2.5-3.5; only K 1.0 / 0, which fails (b), keeps it clear. The likely outcome is "nessuno passa", pending the K 1.0 round-2 run.
    - **Design §13's "il pugno … sullo schermo", read as the front of the fist (knuckle row and three fingers on screen, the palm block allowed to run off the edge):** this holds at 1.65 / 6.5 (fingers to x 936, knuckles to x 910, by the model). 1.65 / 6.5 also passes (b) and (c) (copertura 10.33 / 9.10), so it would be chosen over 1.3.
  - **Fix.** The coordinator rewords (a) for the view the game actually gives and rules which reading holds. Step 1's outcome follows from that ruling, and so do Task 3's checklist and the §13 wording. Either reading changes K. The `SCALE`/`SIDE_SHIFT_PX` Javadoc, the measured values beside the thresholds, `MAGENTA_MIN` and the README numbers all depend on K, so they would follow too.

#### Minor (Nice to Have)

- **T2-F3** tools/client/m3d_shots.sh:193: `run A … && run B …` skips B whenever A exits non-zero. That includes the case where A saved all eleven shots and failed only on `log-check.txt` warnings, which is the most likely real failure (a `Suit rendering failed` line). B's shots were deleted at :191, so none of the fourteen checks run. The FAIL verdict is right; what is lost is the diagnosis of which checks break. The implementer disclosed the choice. Fix: run B unless A exited with 2, or unless A's shots are missing.
- **T2-F4** tools/client/m3d_shots.sh:205: `mis()` sends `arm_measure.sh` stderr to /dev/null. A KO line then says only `misura-non-riuscita`, never why (wrong size, missing file, ImageMagick error). Fix: keep the first stderr line in the token, or let stderr through.
- **T2-F5** tools/client/m3d_shots.sh:205, :223: the function names `esito` and `mis` are Italian. The Global Constraint says "identificatori … in inglese", and the other tool scripts use English names (`energy`, `on_ground`, `check_shot`, `measure_sagoma`, `fatal`). Rename them, e.g. to `verdict` and `measure`.
- **T2-F6** tools/client/README.md:171: the `EXO_SHOT_SERVER_CONFIG` row still lists only `m3a_shots.sh`, `m3b_shots.sh`, `m3c_shots.sh` and `m3c2_shots.sh` as the sequences that drop the variable. `m3d_shots.sh` drops it too. Add it to the list.

### Assessment

**Task quality:** Needs fixes

**Reasoning:** The tooling is well built and matches the brief item by item: `EXO_SHOT_MAIN_HAND`, the fourteen-check sequence, and the README, with 14 `ok` on all four variants. The task's central decision does not hold, though, and the `SCALE` Javadoc asserts facts the shots contradict: at the chosen K 1.3 / 4.5 the palm reaches the right edge just as at the rejected 1.65 / 6.5, and the thumb is not visible. The coordinator must rule on what (a) means (T2-F2) before K and the numbers that depend on it can stand.
