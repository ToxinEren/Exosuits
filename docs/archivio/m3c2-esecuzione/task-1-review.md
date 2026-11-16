# Task 1 review — Riporti dalla 3c-1 e `client_rules` ricevuto nel client vero

Base e993d7e, head 1846274 (one commit, signed, subject as Step 11). Diff read once in full; no hunk was cut off.

### Spec Compliance

- ✅ Spec compliant. The 14 files of the brief each have their hunk and no other file is touched.
  - Step 1 `LangFilesTest` (`src/test/java/com/toxineren/exoframes/LangFilesTest.java:1-135`): package, class-loader read,
    `JsonReader` name by name, the four tests with the names of the brief; the failure message of
    `sameKeysInTheSameOrder` names the first differing key (`:84-88`).
  - Step 2: `EnergyGameTests.java:99-101` and `:767-826`, `CellGameTests.java:90-91` and `:466-498`: the three tests follow
    the brief call by call (plus a premise assertion on `{2, 10_000}`). `add(tests,` lines counted: 175.
  - Step 3: FW-B3 (`FrameStateAccessImpl.java:41-43`), T10-F1 (`build.gradle.kts:105-106`, text of the brief), T1-F1 (four
    imports gone from `GameTestKit.java`), T1-F2 (`GameTestKit.java:74-76`), T1-F3 (`GameTestKit.java:776-779`), T21-F1
    (`CellGameTests.java:231-233`), probe (`CarryOverGameTests.java:128-129`, registration right after the `hurt` assertion).
  - Steps 4-5 FW-B1: `tools/client/m3c_shots.sh:119-120`.
  - Step 6 FW-B2: `tools/client/client_shot.sh:157-164` (check among the environment checks, path made absolute at once,
    exact error text, exit 2 through `fatal`), `:199-203` (copy right after the line that removes the two config files),
    header `:33-36` and `:48-50`; `tools/client/README.md` variables table and the "Lo script lavora…" paragraph.
  - Step 7: `tools/client/m3c_rules_shots.sh` (mode 100755 in the index); the `steps=(…)` array, the `cell` case, the
    config line and the `energy`/`worn` helpers are the brief's, character by character.
  - Step 9: `tools/client/README.md` (usage line, paragraph, table, duration) and `docs/compat.md:72` (sentence of the
    brief, verbatim).
- ⚠️ Cannot verify from diff: nothing left open. Checks made outside the diff are listed below.

### Checks made outside the diff (one per named risk)

- Risk: a removed import still used in `GameTestKit` → grep of the four simple names: only inside `{@code …}` and plain
  comment text (lines 94, 98, 100, 638), no `{@link}`; `GameTestGenerator` is still needed by two `{@link}` (78, 109).
- Risk: new tests rely on imports/helpers the diff does not show → `CellGameTests` already imports `Arrays`, `GameType`,
  `Inventory`, `CellSwap` and has `cellsAndRe` (`:530`); `EnergyGameTests` already imports `List`, `FrameSounds`,
  `Sound`, `SuitTick`.
- Risk: the spectator half of the cell-swap test passes for another reason → `frame/CellSwap.java:73-74`: the guard is
  `wornFrameId empty || !isAlive || isSpectator || menu open`, and the interval is recorded only after a real swap, so
  the first call is refused by the spectator check and the second at `t0 + 1` is not blocked by the interval.
- Risk: `EXO_SHOT_SERVER_CONFIG` checked too late or `$PWD` changed before it is read → `client_shot.sh:61-164` read: no
  `cd` before line 161, the check sits after the EULA check and before the port check, `mkdir "$out"` is at `:179`.
- Risk: the script deletes the config that `m3c_rules_shots.sh` writes in `<cartella>` → `client_shot.sh:186-189` removes
  files by name only; `server-config.json` is not among them.
- Risk: the two comment-only edits made after the closing checks (implementer's disclosure) → `bash -n` on the three
  scripts: clean; the two early-exit proofs re-run by me in the scratchpad: FW-B1 gives
  `FAIL 1.21.1-fabric: client_shot.sh e' uscito con 2`, exit 2, no `moved wrongly` line, `server.log` gone; FW-B2 gives
  the exact `ERRORE: EXO_SHOT_SERVER_CONFIG: file non trovato o non leggibile: <path>`, exit 2, folder not created.
  `git status` clean before and after, `git stash list` empty.
- Risk: the committed screenshots do not show what the documents say → both PNGs are 960x540 with only `IHDR`, `IDAT`,
  `IEND`; measured on the pixels: the cell bar is 6 red pixels on a 26-pixel black track (3 of 13) in both.
- Evidence of Step 8 and Step 10 read at the paths the report gives (not re-run): the four `fa2-<variante>` folders have
  `log-check.txt` of 0 bytes, 0 lines `Config file` / `Received client rule` in the four logs each, and the three
  `rcon.log` answers `19996 / 20000`, `989 / 20000`, `4996 / 20000`; `regole-uscita.png` of 1.21.1-neoforge shows
  "Hold to leave the exoframe… 48%" and "Energy 19993 / 20000 RE"; `gt-all.log` has the four
  `OK <variante>: 175 test obbligatori passati, nessun avviso fuori elenco`; `OK sources`, `OK jars`; the negative proof
  ends with exit 1 at the 19900..20000 check.

### Strengths

- The FA-2 script is a real two-sided proof: each of the three values has a check that fails with the client's own
  default (V 2 s, R 3 s, bar at a quarter) and one that passes only with the server's value (R 5 s, V 4 s, capacity
  19 900-20 000), and the capacity check was shown to fail against an empty config.
- `EXO_SHOT_SERVER_CONFIG` is validated before anything is touched and the copy failure is its own exit-2 error rather
  than a silent start on defaults (`client_shot.sh:162-163`, `:201-202`), which would have made FA-2 pass or fail for
  the wrong reason.
- `LangFilesTest` reads with the streaming reader so a repeated key is visible, treats `%%` correctly
  (`:66-78`, including `%%s`), and skips misaligned positions in the placeholder test so one fault gives one message.
- The new world tests assert a premise and print the observed values in every message; the sound recorder is closed by
  try-with-resources even when an assertion fails.
- The new wrapper removes its stale `log-check.txt` before the call, so it does not inherit the FW-B1 class of defect.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T1-F1 (plan-mandated).** `tools/client/m3c_shots.sh:119-120` and `:132-133` (and `tools/client/m3b_shots.sh`, last
  eight lines, not in this task's file list): FW-B1 is closed for `server.log` only. A non-empty `log-check.txt` left
  by an earlier failed run still makes the final line say `avvisi del mod fuori elenco` after an early exit 2 of
  `client_shot.sh`, which removes that file only at `client_shot.sh:186-189`, after the environment checks. Reproduced:
  a stale `log-check.txt` plus no `EXO_ACCEPT_EULA` gives
  `FAIL 1.21.1-fabric: avvisi del mod fuori elenco in …/log-check.txt` from `m3c_shots.sh`, while
  `m3c_rules_shots.sh` gives `client_shot.sh e' uscito con 2`. The brief (Step 5) names `server.log` alone, so the
  implementer was right not to widen it. Fix when ruled: add `"$out/log-check.txt"` to the `rm -f` of `m3c_shots.sh`
  and the same line to `m3b_shots.sh`.
- **T1-F2.** `tools/client/client_shot.sh:160-164` with `:198-203`: when `EXO_SHOT_SERVER_CONFIG` names
  `versions/<variante>/run-shot-server/config/redstone_exoframes.json` itself, the check passes, line 198 deletes the
  file, and the copy stops with `non riesco a copiare` (exit 2) after the old shots in `<cartella>` have already been
  removed; the message does not say why. Unlikely (the file is one the script always deletes), harmless to the
  checkout. Fix: in the environment check, refuse a path that is the same file as the destination
  (`[ "$server_config" -ef "$root/versions/$variant/run-shot-server/config/redstone_exoframes.json" ]`) with its own
  message, or one sentence in the README row.
- **T1-F3 (plan-mandated).** `src/test/java/com/toxineren/exoframes/LangFilesTest.java:66-78`:
  `placeholders` counts the literal `%s` only, as the brief says. Minecraft also accepts the positional form
  (`%1$s`), which this counts as zero: a translation that reorders arguments with `%2$s … %1$s` fails the test against
  an English `%s … %s`, and a positional placeholder missing in one language against none in the other is not seen.
  No value uses the positional form today (grep of the two files). Fix when ruled: count `%s` and `%<n>$s` alike.

### Implementer's concerns, judged on the code

- Stale `log-check.txt` in `m3b_shots.sh` and `m3c_shots.sh`: real, reproduced, outside the brief's text → T1-F1.
- `EXO_SHOT_SERVER_CONFIG` pointing at the server's own config file: real as described → T1-F2.
- Comment-only edits after the closing checks; one `git stash` / `git stash pop`: no finding. The committed scripts
  parse, the two early-exit proofs give the expected lines on the committed text, the tree is clean and no stash is
  left. The executed lines I read are the ones the reported runs describe.

### Assessment

**Task quality:** Approved

**Reasoning:** Every step of the brief is in the diff as written, and the claims I could check on the repository and on the saved evidence (test count, early-exit proofs, bar width in the committed screenshots, the four real-client runs) hold. The three Minor items are an edge case of the new variable and two limits that the brief's own text sets.
