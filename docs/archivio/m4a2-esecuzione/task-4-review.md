### Spec Compliance
- ✅ Spec compliant.
- Step 1: selftest `OK passi`, `client_shot.sh` unchanged (report; the diff touches no such file).
- Step 2: four variants, each exit 0, eight `ok` lines + `OK`, `log-check.txt` 0 bytes (checked `wc -c` on the four `shots-*/log-check.txt`). Durations 146/135/136/133 s (client_shot 138/127/128/123) are in the header, m4a_shots.sh:154-155, and in README "Quanto dura". Measures are in the thresholds block (m4a_shots.sh:192-220); the six new checks read "stesso sulle quattro varianti" (m4a_shots.sh:162), the sagoma per variant (:199). Thresholds unchanged (the diff has comments only), consistent with half of the neoforge measures (972->480, 952->470, 497->240, 144->70, 552->270).
- Step 3: ten PNGs present, two `stazione-colori-suoi` runs exit 0, `log-check.txt` empty (checked).
- Step 4: four servers reached `Done (`, `check_log.py` silent (step4-*.txt in logs, per report; logs exist).
- Step 5: README paragraph updated, `m4a_shots` count 8 (>=2).
- Step 6 counts (407 JUnit, 256 in-world) reported with fresh evidence; not re-run by me.
- Step 7: one commit; no shot outside `m4a-screenshot/`.
- Open point 1 (`docs/compat.md:255` "sulle quattro varianti"): closed, four `ok` runs on all four variants for anteprima/scelta/smontaggio/montaggio. Open point 2 (committed head): each log opens with `HEAD d0a5342`, `status: []` (checked step2-1.21.1-fabric.log, step3-1.20.1-forge.log). Step 4 runs had only the untracked PNG dir.
- ⚠️ Cannot verify: JUnit 407, in-world 256, gradle server runs, I did not re-run (read-only); rely on the logs in scratchpad `step6-*`.

### Strengths
- Eight checks identical on the four variants, with all runs at a committed clean head; the report adds RCON evidence and pixel comparisons across variants.
- Script change is comments-only; README table updated consistently with the measured values.
- All ten shots viewed: each shows what its name says. `stazione-aperta`: station open, helmet selected, magenta suit from the front. `scelta-torso`: white border on the torso, header "Scrap Torso", suit side-on. `smontato`: empty helmet place with ghost, helmet in hotbar slot 1, no helmet on the suit. `rinforzato`: bronze shield beside the lock, helmet back on the suit. `stazione-colori-suoi`: suit in its own brown colours. No chat, toast, tooltip or pointer in frame; forge and neoforge look identical for the same shot. Metadata checked by the controller.

### Issues
#### Critical (Must Fix)
None.
#### Important (Should Fix)
None.
#### Minor (Nice to Have)
- tools/client/README.md:170 (end of the m4a_shots.sh paragraph) and :252 still point to `docs/test-in-gioco/m4a1-banco.md`; they need `m4a-stazione.md` once Task 5 writes it (the implementer flagged this; Task 5's file list does not include this README, so add it to the Task 5 dispatch).
- m4a_shots.sh:154 "Dura poco piu' di due minuti" while forge measured 146 s (2.4 min): fine, but the "Quanto dura" row in README keeps the same wording; no action needed.

### Assessment
**Task quality:** Approved
**Reasoning:** Every brief step and both open points are evidenced (clean committed head per run, four variants green, empty log-checks), the header and README numbers match the logs, and the ten shots show what they claim. Only the Task 5 documentation pointer remains.
