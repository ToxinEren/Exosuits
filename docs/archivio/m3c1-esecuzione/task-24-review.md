# Task 24 review (ac3501f..d67e8b6)

### Spec Compliance

- ✅ Spec compliant.
  - `tools/client/m3c_shots.sh:47-127` checked against "Produces" line by line: `u`, the `case` for cell and piece, `P`, `at`, `beyond`, `energy` are verbatim (`:56-69`); the setup steps and passes 1-11 send the same commands, waits, keys, snapshot names and thresholds in the brief's order (`:71-115`); `=== $variant m3c` before and `OK`/`FAIL` after, the same as `m3b_shots.sh` (`:117-127`). The one addition, `set_energy` (`:69`), only prints `execute as $u run exoframe debug energy N`, so the commands sent are the brief's own. The header (`:2-43`) has the usage line, each step with its threshold, the snapshots and the measured time (116-127 s). The file has mode 100755.
  - Step 2: I checked the run folders, which are still on this machine (`<scratchpad>/m3c/<variant>/`). On all four variants `log-check.txt` is 0 bytes and `grep -c 'moved wrongly' server.log` is 0 ('moved too quickly' appears 3-5 times). The `Pos` lines in `rcon.log` match the report's table: Z 7.72/7.62/7.54 on the tank floor, X 27.97 and 27.70 out of the bank, Z 5.88/5.76 on the step, sprint 7.22/7.04 and 9.38. The neoforge `rcon.log` has every `debug energy` value, `clear … 0`, `matches 4900..5000` and `matches 1`.
  - "In basso a destra" (Step 2) and the brief's riga 33 and `:273-278` line numbers: the implementer followed the Task 22 ruling (energy line top-left) and the content of each entry. These are correct readings and not gaps.
  - Step 3: ten PNGs. Each version has `riga-energia`, `barre-inventario`, `cambio-cella-tenuto` and `vasca-spazio`, plus `riga-energia-esaurita` (1.20.1) and `riga-energia-bassa` (1.21.1), which answer the [T22-F3] ruling. I ran the brief's chunk check: no output, exit 0, and the only chunks are IHDR, IDAT and IEND. I enlarged the bar region of both `barre-inventario` shots: the cell has a red bar at half and the helmet a yellow bar at half. `cambio-cella-tenuto` shows "Hold to swap the cell... 50%" with a yellow line at 996 RE. `vasca-spazio` is on the tank floor with the camera near the surface, which means the eyes are at suit height and the player is not swimming.
  - Step 5 docs:
    - `README.md:9`: "Stato" covers 3c-1 and names 3c-2 as next.
    - `README.md:31`: "oggi 172 test".
    - `README.md:35`: the `m3c_shots.sh` usage.
    - `README.md:62,65`: the screenshots folder and M3c-1 among the plans.
    - Removing the 3b branch sentence is accurate: `2e35aa6` is an ancestor of `origin/main`.
  - `tools/client/README.md`: usage line `:24`, paragraph and table `:77-92`, duration `:116`. The table gives the thresholds with ×0.85 and `frame.baseSpeedPenalty`, which is 0.15 in `ExoframesConfig.java:109` and used in `FrameRules.java:75`.
  - `docs/compat.md`, one focused check per entry against the code:
    - `FrameIndicators.of(RedstoneExoframes.rules().evaluate(...))` is at `FrameStateAccessImpl.java:149`.
    - The `frame_state_sync` wire order (UUID, presence flag, tag, two booleans, enum) matches `FrameStateSyncPacket.java:30-51`.
    - `localStep` runs only when `player.isLocalPlayer()` (`SuitSize.java:100-101`).
    - `damages_helmet` is at `DamageClassifierImpl.java:38`.
    - `addHudRenderer` has no directives (`ClientRenderHooksImpl.java:82-87`).
    - `configDir()` is called at `RedstoneExoframes.java:53`.
    - Every method of `GameTests.java`, `readRegistry` included, appears in the entry.
    - `ClientRulesTest` and `ClientRulesPacketTest` exist.
    - The "Limiti noti" destinations match design §12.14 `:662-663`, and the closed "Scritture a ogni tick" entry matches §12.4 `:473`.
  - Design:
    - §12.14: `DebugEnergy.java:28,50` (`REFUSED`) is right.
    - §12.10: `CellSwapKey.java:53` (`!ExitKey.keyDown()`) is right.
    - §12.9: `ConfigValidation.java:191` is right.
    - The two edits (§12.13 `:616`, §12.15 `:688`) each fix a real contradiction, cite their task and are reported as builder choices. The new §12.13 text matches `EnergyHud.java:41-83`: x 4, y 4, a second line 10 below, yellow for CRITICAL and red for BROKEN, and the `worst` key renders as "Torso in stato critico".
- ⚠️ Cannot verify from diff: none. The run folders, which the brief does not ask to commit, were readable and match the report. The Step 4 dedicated-server logs and the Step 6 command output are in the report only; I did not re-run them, following the contract.

### Strengths

- The script is the brief transcribed without drift. It is easy to diff against `m3b_shots.sh`, and every threshold in the header and the README table comes from the measured runs.
- Re-reading `compat.md` against the code found and fixed five old untrue sentences ([T5-F3], [T11-F1], [T12-F1], [T14-F2], [T19-F3]) inside the entries the brief asked to re-read. One fix states honestly that a real client cannot tell a received `client_rules` value from the local default.
- The report says which committed claims are not evidence: the first-person `vasca-spazio` cannot show a pose, and `riga-energia-bassa` reads 4 % rather than 5 % after one second of idle drain.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T24-F1** `docs/compat.md:292`, "Ripristino dei moduli e config di chi legge — Milestone 4": the entry now opens by saying the config is read from file (part 3c-1), but its last sentence still reads "Direzione: decidere, insieme al caricamento della config, se il server debba tenere i moduli fuori regola…". Config loading is no longer a future step, so the direction is stale. Fix: "Direzione (M4, con gli oggetti modulo): decidere se il server debba tenere i moduli fuori regola invece di scartarli."

### Assessment

**Task quality:** Approved

**Reasoning:** The script matches the brief exactly. The four real-client runs check out against their own logs, the committed screenshots carry no metadata and show what the docs say, and every `compat.md` and design statement I checked against the code is true. One stale clause in a deferred-limits entry remains.
