# Task 3 review (3a7c47a..ffe2ab2)

### Spec Compliance

- ✅ Spec compliant. Everything in the brief's Produces list is there:
  - ten PNGs in `docs/test-in-gioco/m3d-screenshot/`, all 960x540, with only IHDR/IDAT/IEND chunks (checked with the brief's own parser);
  - the checklist, with header, both "Cosa c'è" sections, "Già verificato" with all fourteen checks and the six things the cloud cannot prove, section 0, steps 1-19 in the brief's order with the brief's content, "Cosa riportare", and the four owner questions;
  - all six documents. §13 and one sentence of §5.2 are the only parts of the design touched.
- Rulings applied. The §13 "proposal" passages now say the coordinator confirmed them (ruling 12): the intro, "Posizione (… confermata da chi coordina)" and "Secondo giro (… confermato …)". Criterion (a) and "chosen by eye" follow rulings 45 and 46. Ruling 14: m3a got one sentence, m3c is untouched, and the new checklist's header carries the warning about the older screenshots.
- ⚠️ Cannot verify from the repository:
  - whether F3+T actually logs a `Suit rendering failed` line (checklist step 15; no cloud run reloads resources);
  - that the hook applies in the production jars (steps 1 and 3).
  Both are what the PC checklist is for.

Checks I ran outside the diff, each with the risk it answered:
- **Wrong numbers in §13's "Valori scelti" table.** I compared the table with `/home/user/m3d-candidati/misure.txt` and with the Javadoc of `FirstPersonArm.SCALE`/`SIDE_SHIFT_PX` (FirstPersonArm.java:41-85). Every coverage, bar, box and "bordo da y" value matches (407/418, 336/361, 463/465; 4.0→634, 6.0→731, 2.5→14).
- **Wrong in-game chat strings.** The mod's `it_it.json`/`en_us.json` lines 16-21 match the checklist's quotes.
- **The claim that the creative-tab cell is charged.** `ModCreativeTabs.java:33` appends `CellItem::charged`.
- **The cross-reference to "m3b passo 11".** `m3b-entrare-e-uscire.md:80` matches it.
- **The log pattern.** `SuitDraw.java:85` logs "Suit rendering failed with …", which the `Select-String` pattern matches.
- **Wrong vanilla Italian labels.** I read the cached `minecraft/lang/it_it.json` (same object d4de73d6… in the 1.20.1 and 1.21.1 asset indexes); results in F1 and F2.
- **Stale local `main`.** `e064c9e` is an ancestor of HEAD. `git diff --stat e064c9e ffe2ab2 -- docs/test-in-gioco/m3c-giocarci.md tools/log_allowlist.txt src/main/resources src/main/resources-mc1.20.1 src/main/resources-mc1.21.1` prints nothing, so using `e064c9e` in place of the stale `main` (2e35aa6) is right.
- **Whether the reference shots are identical across variants.** I compared the implementer's run shots with `compare -metric AE`; result in F3.
- **Leftover chat lines in the reference shots.** I looked at six of the ten shots: no chat line above the hotbar, both arms in `mappa`, the arm on the left in `mancino`, and no skin between the bars in `nudo`. `docs/…/1.20.1-forge-braccio-rottame.png` is pixel-identical to the run's `braccio-rottame.png`.
- **Missing world-setup content in section 0.** Item E repeats m3c step 7 (survival, cheats, superflat, no structures); nothing is dropped.
- **Missing rows in the `tools/README.md` table.** The table lists no client sequence, not even `m3c2_shots.sh`. The intro paragraph is the right place, and it was updated.
- **Each `m3d_shots.sh` threshold quoted in `tools/client/README.md`** (10 000, 19 900, 3-14 %, 1/20, 300, 130, 70, 200, 120, 90-110 %, 80-120 %, 12 px, `@attendi 12`) agrees with the script (m3d_shots.sh:79-131).

### Strengths

- The §13 "Valori scelti" table (design §13) records the result, shows that the second round was needed, and gives the minimal-shift evidence for each K (half a pixel less → bar 634/731/14). It matches the Javadoc and misure.txt digit for digit.
- The checklist is built for a literal follower:
  - the world is created at step 2, so steps 2-3 come before any F5 or E (Review Focus 2);
  - step 6 warns that `/clear @s` alone would also remove the suit's placeholders;
  - step 5 explains how to keep the grass block out of the empty hand;
  - step 14 gives the exact side to click and what to do if the piece lands on the other side;
  - step 19 ties the `Suit rendering failed` count back to step 15;
  - PowerShell commands are given for the log checks.
- The LAN, create-world, "Mano principale/Sinistra/Destra", "Campo visivo" and "Fatto" labels are all correct against the vanilla Italian lang.
- The `tools/client/README.md` updates are accurate: inventory width 166-174, `m3d_shots.sh` in the `EXO_SHOT_SERVER_CONFIG` row (closes T2-F6), the durations from this task, and the new "Cosa non copre" bullet.
- The report is honest: it has the six-row §13 "fatta quando" table per variant and flags the stale `main` and the unverified labels.

### Issues

#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)

- **T3-F1 (plan-mandated) docs/test-in-gioco/m3d-braccio.md:17, :78 (and :80 by reference).**
  - The skin-customisation button is not "Personalizzazione skin...". In both versions' vanilla `it_it.json`, `options.skinCustomisation` = "Aspetto..." (its screen title is "Personalizzazione dell'aspetto del personaggio").
  - "Mano principale", "Sinistra" and "Destra" are right.
  - The brief's step 12 uses the same wrong label, hence plan-mandated.
  - Why it matters: an assistant with an Italian client who follows step 12 literally will not find the button. The English label beside it helps only with an English client.
  - Fix: "Opzioni... → Aspetto... → Mano principale: Sinistra".
- **T3-F2 docs/test-in-gioco/m3d-braccio.md:85 (step 16).** Two problems.
  - The Italian path is "Opzioni... → Grafica... → Scala", not "impostazioni video / scala dell'interfaccia".
  - GUI scales 3 and 4 exist only if the window is large enough. Minecraft caps the GUI scale at the largest value that leaves at least 320x240 GUI pixels, so at the launcher's default 854x480 the maximum is 2. The step does the GUI scales before F11.
  - Fix: do the GUI-scale part in fullscreen or in a maximized window, and add "se un valore non c'è, annota il massimo che il gioco offre".
- **T3-F3 docs/test-in-gioco/m3d-braccio.md:46, :48.** Three claims are stronger than the evidence.
  - "le altre due varianti danno scatti uguali al pixel" is false. In the implementer's own runs, 1.20.1-fabric and 1.20.1-forge `braccio-rottame` differ in 36 386 pixels, 1 541 of them above arm_measure's 4 % threshold, inside 959x294+0+246 (the ground). 1.21.1-fabric and neoforge differ in 9 067 pixels, none above 4 %. The measures are equal; the shots are not.
  - "salvo di poco … dell'anteprima dell'inventario": on 1.20.1 the inventory magenta is about 1.75 times that on 1.21.1 (18 830 against 10 784, uncropped against cropped).
  - "nella stessa posizione rispetto ai bordi" at other window sizes holds only at the same aspect ratio. The hand projection uses a vertical FOV, so the offset from the centre scales with window height, not width. It was not measured.
  - Fix: "le stesse misure e scatti uguali a occhio"; "con una finestra 16:9 di altra misura …". To state more than that, run one shot at a different window size.
- **T3-F4 docs/test-in-gioco/m3d-braccio.md:87 (step 18).**
  - "lui vede la tua tuta intera" is a trap for a literal follower. At step 18 the host is still inside the step-14 frame: one Braccio Rottame and a cell, since steps 15-17 do not change it.
  - The same sentence, and step 9, define "intera" for the other player as "due braccia, tutti i pezzi". The second player will see a bare frame with one arm piece and may mark the step non-OK.
  - Fix: say what the second player should see (the frame with the right arm piece only, all its bones present), or have the host wear the six-piece suit before step 18.
- **T3-F5 (plan-mandated, outside the file list) tools/client/m3d_shots.sh:62-67.**
  - The head comment's measured ranges are now stale against this task's runs:
    - durations: comment 208-226 s (112-121 and 76-85); now 207-225 s (111-119 and 76-86);
    - third-person magenta: comment 23595-23745; 23593 measured;
    - Forge inventory: comment 170-172 wide, magenta 18796-18816; now 174 wide, magenta 18830;
    - 1.20.1-fabric inventory: comment 166-168 wide from x 322-323, magenta 18712-18778; now 170 wide from x 321, magenta 18786.
  - These figures now disagree with tools/client/README.md:135 and :186, which were updated.
  - The checks are unaffected: they compare widths against 130 and 70.
  - The script is outside Task 3's file list, so the implementer was right not to touch it. Route it to the final review wave.

### Assessment

**Task quality:** Approved

**Reasoning:** The documents, checklist and reference shots match the brief and the rulings. The §13 numbers match the Javadoc and the measurement log exactly, and the stale-`main` deviation is correct. What remains are wording fixes in the checklist (two menu labels, one overclaim, one ambiguous expectation) and a stale comment in a script outside the file list.
