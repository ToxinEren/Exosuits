# Task 2 review (b5cb011..f754b0e)

### Spec Compliance

- ✅ Spec compliant. Checked against "Produces" and Steps 2-4, 8, 10 file by file (all six listed files have their hunk):
  - Usage: `tools/check_log.py:241-242` (`len(argv) < 5`, scope, `argv[2] != "--variant"` -> `fail_usage(USAGE)`, `USAGE` at `:117` is the brief's text, printed as `ERRORE: uso: ...` by `fail_usage`); variant `VARIANT.fullmatch` at `:116`/`:245-248`, exit 2; loader = `variant.group(1)` (`:249`).
  - Count field: `parse_count` (`:136-148`) is the brief's code except `re.fullmatch(r"[0-9]+", most)` for `most.isdigit()` (reported deviation; same result on every valid form, and it turns `"²"`, where `int()` raises, into the brief's exit 2). Called before the scope filter (`:179` vs `:191`), so every line of every scope is checked. Duplicate/missing loader, all-zero, `=0`, `0` all reach `fail_usage`.
  - New message `:229-230` matches "Produces"; the max message stays (`:227-228`).
  - `TESTREG` (`tools/log_allowlist.txt` diff line 482) as in "Produces".
  - Nine count lines: mapped BASE lines 26, 29, 31, 33, 36, 38, 40 -> `=1,=2,=2,=1,=2,=1,=1`; 45/46 -> `fabric=1,forge=0,neoforge=0` (now three lines lower, header grew by three).
  - Header comment rows 3, 9, 13 and the comment above the Fabric lines carry the brief's text (double quotes instead of backticks in a plain-text file: consistent with the file's own quoting).
  - Callers: `tools/run_gametests.sh:43` and `tools/client/client_shot.sh:474` exactly as in "Produces"; `$variant` is defined at `client_shot.sh:68`. Header comments `run_gametests.sh:6-8`, `client_shot.sh:21-22` as asked.
  - `autoJump:false` after `rawMouseInput:false`; key regex `^[A-Za-z0-9_+]+$`: identical to the two `client_shot.sh` hunks of `docs/superpowers/specs/ricognizioni/2026-11-15-m3c-movimento-e-dati.patch` (lines 348-366).
  - Docs: `tools/README.md` run_gametests row, check_log row, "Avvisi voluti" paragraph; `tools/client/README.md` lines 32, 44, 57, 115 (line numbers counted in the hunks) as in Step 8. No new keys, `docs/compat.md` untouched.
  - Commit `f754b0e` on `m3c-giocarci`, subject as in Step 10, body carries the Step 5-7 outputs; signature state `N` locally, same as the previous commit b5cb011.
- Named risk "API contract of check_log.py changed, a caller left on the old usage": grep of the repo (`check_log`, plus `.github/`) finds only `run_gametests.sh:43` and `client_shot.sh:474`; CI (`.github/workflows/build.yml:25`) calls `run_gametests.sh` with no arguments, so the four default variants are passed. No stale caller.
- Named risk "the `client` exact count is wrong on the variants the real client was not run on in this task": saved logs on this machine, `No data fixer registered for exoframe` count: 1.20.1-fabric run-shot-client/server latest.log of 14 Nov = 1/1, 1.21.1-fabric (this task) = 1/1, 1.20.1-forge and 1.21.1-neoforge run-shot client/server = 0/0. Consistent with `fabric=1,forge=0,neoforge=0`.
- Named risk "`+` in a key name breaks the key pre-check or the release on exit": `client_shot.sh:316-318` (keydown/keyup on the empty screen), `:425-434` and `:244-246` pass the whole name to xdotool, which takes `ctrl+w` as a combo; the Step 7 run with `@tieni ctrl+w 1` exited 0.

### Strengths

- `parse_count` is a faithful transcription with a justified hardening; the usage message lives in one constant used for every wrong-usage path (`check_log.py:117`, `:242`).
- Problem accounting stays single-line per issue: a Fabric line seen on NeoForge is counted as allowed and reported once as "1 volte ... esattamente 0 volte", not also as "riga non ammessa" (`:214-230`).
- Evidence in the report is complete and matches the brief's expected outputs verbatim, including the BASE counter-proof for the `\S*` -> `[\S ]*` change.
- The `massimo` wording was updated everywhere it would now be wrong (allowlist field name `:6`, error text `:172`, README paragraph), beyond the lines the brief listed.

### Issues

#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)
- T2-F1 `tools/client/client_shot.sh:11-14` (header: log-check.txt holds lines "fuori da tools/log_allowlist.txt"), the `fail` message after `client_shot.sh:476` ("righe del mod fuori dall'elenco degli avvisi voluti") and `tools/run_gametests.sh:44` ("avvisi o errori del mod non previsti in tools/log_allowlist.txt") still describe only lines missing from the list. With exact counts a failure can now be an allowed line that is absent or on the wrong loader ("0 volte la riga ammessa esattamente 1 volte"); the printed problem lines explain it, so the cost is a misleading headline. Fix: add "o contate un numero di volte diverso da quello ammesso" to the three texts (README line 44 already says it).

### Assessment

**Task quality:** Approved

**Reasoning:** Every "Produces" item and step lands as specified, all callers of the changed CLI are updated, and the exact counts are consistent with the saved logs of all four variants; the only gap is stale wording in two failure headlines and one header comment.
