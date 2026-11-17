### Finding Verdicts

- **[T2-R1] constants back to SCALE 1.3f / SIDE_SHIFT_PX 4.5f, Javadoc says what the shots show, sequence re-run on four variants** — ADDRESSED.
  - Constants: src/main/java/com/toxineren/exoframes/client/FirstPersonArm.java:73 `SCALE = 1.3f`, :85 `SIDE_SHIFT_PX = 4.5f`.
  - Criterion (a) as ruled (knuckles and fingers whole, part of the forearm seen, palm/forearm may leave by the right or bottom edge, thumb not needed): FirstPersonArm.java:42-47.
  - Choice by the coordinator by eye, not "the largest K": FirstPersonArm.java:48-51.
  - Per-candidate shots: 1.3/4.5 fingers to x 886, steel palm at the right edge from y 420 (FirstPersonArm.java:61-64); the wrong "la mano intera a 80 pixel dal bordo destro" is gone (grep: no match); round 1 measures kept (:55-60); 1.65/6.5 (:65-67); K 1.0 round 2 with 2.5 (bar 14, box 291x177+669+363) and 3.0 (:68-70).
  - SIDE_SHIFT_PX Javadoc: minimum at K 1.3 (4.0 bar 634, 4.5 clear by 4 px), the other two minima kept (FirstPersonArm.java:76-84).
  - JUnit: no test pins the Javadoc; src/test/java/com/toxineren/exoframes/client/FirstPersonArmTest.java:106-117 accepts 1.3 (among K) and 4.5 (0-8, multiple of 0.5). The report shows FirstPersonArmTest 6/6 and 331 JUnit per variant.
  - Script/README: thresholds and measured values (8.77, 7.72, 39962, ...) already K 1.3/4.5 (tools/client/m3d_shots.sh:97, tools/client/README.md:129,131); the fix only refreshed durations and 1.20.1 inventory widths (m3d_shots.sh:65-73,112; README.md:135,190); no threshold changed.
  - Sequence: /home/user/m3d-scatti/<variant>.log has 14 `ok` and a final `OK` on all four variants; all eight log-check.txt (A and mancino) are 0 bytes (checked).
  - scelto-sopravvivenza.png refreshed (/home/user/m3d-candidati/scelto-sopravvivenza.png, 13:18).
  - Closing checks: report lists build, check_sources, check_jars, arm_measure --selftest, run_gametests with outputs.
  - One commit 3a7c47a, in Italian, says the choice is the coordinator's ("scelti da chi coordina", "ha scelto a occhio 1,3 con 4,5"); working tree clean.

### New Breakage in the Fix Diff

None. No line over 120 columns in FirstPersonArm.java or m3d_shots.sh (checked with awk).

### Out-of-Scope Observations

None.

### Verdict

**Fix round:** All findings addressed, no new Critical/Important breakage.
