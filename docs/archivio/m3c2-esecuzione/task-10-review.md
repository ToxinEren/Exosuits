# Task 10 review (dd696e0..9107aae)

### Spec Compliance

- ✅ Spec compliant. Every file of the brief has its hunk: `tools/client/m3c2_shots.sh` (new, 100755), four reference shots in `docs/test-in-gioco/m3c-screenshot/`, the rename `m3c1-consumare.md` → `m3c-giocarci.md` (commit 0026c52 records it as a rename, `git log --follow` reaches 614b25f and 605fca6), `README.md`, `tools/README.md`, `tools/client/README.md`, `docs/compat.md`, the head notes of `m3a-vedere-il-telaio.md` and `m3b-entrare-e-uscire.md` (one sentence each), the design (14 lines, only §12.10, §12.11, §12.12, §12.15, §12.16), and the pointer at the old path that the ledger ruling asks for (commit 9107aae).
- Script against "Produces", step by step (`tools/client/m3c2_shots.sh:62-100`): same commands, same order, same waits as the brief's eight steps; head comment and README say what it does not prove; final `OK`/`FAIL` line and `rm -f log-check.txt` as in `m3c_rules_shots.sh`.
- Checklist against "Produces" (`docs/test-in-gioco/m3c-giocarci.md`): 112 numbered steps, no gaps or duplicates; 23 sections 0-22; sections 0-15 keep number and title; steps 2-75 are byte-identical to the 3c-1 file at dd696e0 (compared line by line), step 1 changes only where the jars come from (line 137; the brief allows "ciò che la 3c-2 ha reso falso"); new steps 76-107 in sections 16-20 in order; 108 (section 1), 109 (section 7), 110-111 (section 12), 112 (section 15) sit at the end of their sections; "Cosa riportare" is 21 and covers sections 1-20 (line 380), questions are 22 with the four new ones (lines 394-397); "Sono le sezioni 1-20" (line 133); "Cosa non c'è ancora" has the five items the brief lists (lines 86-90); T22-F3 kept (lines 14, 82, 88).
- Every item of the design §12.15 "Checklist per il PC" list has a step (the report's table checked against the file; 108, 109 and 112 are the three that were missing).
- Rulings: the cut only by hand is in design §12.10 and §12.11 (design lines 555, 572, 574, 576, 578), README and compat.md; one dust per tick is in §12.11 (line 572); the pointer file exists and is four lines.
- ⚠️ Cannot verify from the repository: see "Cannot verify" below.

Checks I ran (read-only), each for a named risk:

- Stale `main` (concern 1): local `main` is 2e35aa6, `origin/main` and the merge base are 614b25f; `git diff --stat 614b25f HEAD -- tools/log_allowlist.txt` prints nothing and no commit after 614b25f touches the file. The intent of the closing check holds.
- Step 5 greps: 112, one line for step 75, 23, one line for section 15; `m3c1-consumare` appears only in the two sentences that tell the rename (m3c-giocarci.md:5, design:695).
- Reference shots: the brief's PNG check passes on all 16 files (the four new ones hold only IHDR/IDAT/IEND). Viewed `1.20.1-forge-caricatore-posato.png` (front with the empty dark housing towards the player, textured top and sides, no purple-black face) and `1.21.1-neoforge-caricatore-acceso.png` (three red notches, `Charger: charging 30%`).
- Client runs: the output folders are still in the scratchpad; twelve `OK <variante>: scatti in ...` last lines, twelve `log-check.txt` of 0 bytes, no `moved wrongly` in the four `m3c_shots.sh` server logs, charge read 3117 then 3320 in `rcon.log`, seven `recipe take` answered.
- Dedicated server: `versions/<v>/run/logs/latest.log` of the four variants each has `Done (` and the stop line; `python3 -I tools/check_log.py client --variant <v>` on each: no output, exit 0.
- `isCreative()` grep on `content/` and `core/`: no line. `bash -n tools/client/m3c2_shots.sh`: clean. Script is pure ASCII.
- Checklist facts against the code (concern 3), the ones the repository can answer:
  - materials: one of each object = 28 iron, 10 copper, 2 redstone blocks, 2 dust, 1 quartz (step 77); a full suit with two cells and a charger = 34/14/2/2 and 4 dust, 20 dust left of 24 (steps 101-103);
  - break times from `strength(3.5F)` + `requiresCorrectToolForDrops`: 17.5 s by hand, 2.65 s with a wooden pickaxe (step 82);
  - comparator: `ChargerRule.comparator` = floor(15·stored/capacity), 0 without a cell → 0, 0, 7, 15 (step 93);
  - status precedence and texts: `ChargerRule.status` and the two language files match the message table and steps 80, 83-92;
  - click branches: `ChargerBlock.onClick` (main hand only, sneaking empty = status only, empty hand takes the cell and then the dust) match steps 85, 88, 89;
  - an empty cell mounts on a frame (`FrameInteractions.insertCell` has no charge check) with `MOUNT` = iron trapdoor (step 78);
  - step out: order front, left, right, back; `MAX_STEP_UP = 0.5` (carpet and slab are climbed, a full block is not), hazards = the list of step 111 (`FrameEntry.java:349-444`);
  - creative tab: the charger is appended last (`ModCreativeTabs.java:34`), as the introduction says;
  - block model `orientable` with top/front/side: "sopra e sotto la stessa piastra, tre fianchi uguali" (step 81);
  - the test names cited in the READMEs and in compat.md exist; the `add(tests,` lines of the test classes sum to 208.

### Strengths

- The script is the brief's sequence without creative additions and ran first time on four variants; the evidence (rcon.log, log-check, durations) is on disk and matches the report.
- The checklist states the "burning dust" reserve up front (section 17 introduction) and resets it where a timing depends on it (step 91 breaks and re-places the charger; steps 96-97 use cells at 9000 RE with exactly two dusts each), so the timed expectations are exact, not approximate.
- Old steps untouched: a tester mid-way through the 3c-1 checklist keeps every number; the "se hai già fatto i passi 1-75" paragraph says exactly what to redo and how to recognise the new jars.
- Where a result is not known in advance the step says "annota" instead of inventing an expectation (108, 112).
- Design corrections are confined to the listed sections, each with its task reference; nothing else in the design moved.
- The stale-`main` deviation is reported with the command that proves the intent, and local `main` was left alone.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

None.

#### Minor (Nice to Have)

- **T10-F1 (plan-mandated tension, for the coordinator)** `docs/test-in-gioco/m3c-giocarci.md:305`: step 112 is a new step in section 15, while the brief says of section 15 "resta com'è, senza passi nuovi". The same brief asks for a step for every §12.15 item, and "consumo camminando su un server con latenza vera" had none; section 15 is the only dedicated-server section, and steps 72-75 and the section text are unchanged. The report lists the placement under "Scelte" but not under "Scostamenti dal brief". Also note that this §12.15 item now lives in a section titled "(facoltativo)", like step 75 before it. No change proposed; the coordinator decides whether the sentence of the brief meant "no new steps for FA-2" (my reading) or "no new steps at all".
- **T10-F2** `docs/test-in-gioco/m3c1-consumare.md:5`: the pointer says steps 1-75 have "gli stessi numeri e lo stesso testo", but step 1 changed (`m3c-giocarci.md:137`); the new checklist's own introduction says so ("è cambiato solo, nel passo 1, da dove si prendono i jar", line 5). The wording comes from the ruling. Fix: the same parenthesis in the pointer.
- **T10-F3** `docs/compat.md:114`: the `CompatItem` entry still says the real tooltip is proved by "la checklist per il PC della parte 3c-2", without file or step, while the twin sentence for `CompatBlockItem` (line 161) now names `m3c-giocarci.md`, passo 80. Fix: name the file and steps 58-60.

Implementer concerns, judged:

1. Stale local `main`: verified, not a defect of the task (see checks). The coordinator may want to fast-forward local `main` before the final review, because any `main`-relative command in this checkout sees 3c-1 too.
2. Two commits: correct choice; a single commit would have lost the rename. Not a finding.
3. Expectations never seen in game: those the repository can answer I checked and found right (list above); the rest are vanilla-client behaviours, under "Cannot verify".
4. `m3c_shots.sh` shots not viewed again: the brief's Step 2 names only the three charger shots to look at; the four runs passed their position checks and have no `moved wrongly`. Not a gap.
5. First names: the old file has five lines with them, the new one has the same five, and no added line elsewhere in the diff has one. No new personal data written by this task; whether the existing ones must go is outside what the plan header says (it speaks of the jars).
6. Deferred minors: T2-F2 closed here (`docs/compat.md:3`); the others are outside this task's files. Correct scope.

### Cannot verify

- Vanilla-client expectations of the new steps that the repository cannot answer and that nobody saw in game: the shield in the off hand while sneaking (step 88), the recipe-book tabs by icon and the 2×2 inventory book hiding the recipes (section 16 introduction, step 76), the Italian texts of the base game ("Cornice riempita", "Cornice svuotata", "Nuove ricette sbloccate!", the data-pack line), the piglin and the powder snow of step 109, the data-pack screen of step 108 including "Su 1.20.1 non compare". They agree with the vanilla rules as I know them; a wrong one would show on the PC as a "non OK" that is a checklist error.
- Build of the four variants with `collectJar`, `OK sources`, `OK jars`, `OK art`, `OK data`, the four `OK <variante>: 208 test obbligatori passati` lines and the 319 JUnit per variant are the implementer's reported output: not re-run, as the contract says (no source, data or resource of the mod changes in this diff).
- Whether a pipeline rule on personal data in documents covers the first names the renamed checklist already had: the rule is not in the plan header or in the brief.
- Two of the four committed reference shots (`1.20.1-forge-caricatore-acceso.png`, `1.21.1-neoforge-caricatore-posato.png`) I did not open; the other two are right.

### Assessment

**Task quality:** Approved

**Reasoning:** The script, the checklist and the documents match the brief and the rulings, the facts in the new steps that the code can answer are right, and the evidence of the client and server runs is on disk. Three minors, one of which is a tension inside the brief for the coordinator.
