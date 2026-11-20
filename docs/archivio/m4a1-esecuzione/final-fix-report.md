# Final fix wave report — part 4a-1 "Il banco" (base a9cb5cb)

Status: DONE_WITH_CONCERNS. Every item of A, B and C1-C7 is applied, the verification passed, and there are three
signed commits (937b28a, 73aaa25, f6da189), not pushed. The concerns are informational ("Concerns and notes for the
coordinator", at the end):

- the brief's "no super call" reason holds only for vanilla and Fabric (the fix is still safe);
- the P22 evidence is two runs (a negative control was added);
- names outside the brief were left untouched.

## Orientation (done)

- Read: final-fix-brief.md, final-review.md (Important, triage, rulings notes), progress.md (P22, P24, P27, P28),
  contesto-comune.md Global Constraints (lines 15-45).
- Start state: branch m4-stazione, HEAD a9cb5cb, `git status` clean; machine idle (15 GB free, no Gradle daemons).

## Items

Line numbers are at the fixed tree (after all edits).

### A. Ruling P22: the release that ends a rotation drag (commit 1)

- `src/main/java/com/toxineren/exoframes/client/StationScreen.java:215-222`, `mouseReleased`: if the left button
  ends a rotation (`rotating`), it sets `rotating = false` and returns `true` without calling `super`; any other
  release goes to `super` as before. New Javadoc at :208-214: the release that ends a rotation does not reach the
  base game, because there the release puts down the stack on the cursor (dropped outside the screen, placed in the
  slot under the mouse); the press never reached the base game, so it has nothing pending; every other release goes
  to the base game. The old sentence "il rilascio va comunque al gioco base" is gone.
- Hardening, `StationScreen.java:177`: in `mouseClicked`, every left press first sets `rotating = false`; a press in
  the preview clip sets it back to `true` (:182-187). So any left press outside the clip, the test button included,
  disarms a rotation whose release was lost. Javadoc :169-170 says so.
- Docs that describe the old release: none. `git grep -n -i "rilascio\|rilascia\b\|mouseReleased\|lascia il
  tasto\|lasciare il tasto\|lasci il tasto"` outside docs/superpowers finds only the StationScreen Javadoc (now
  rewritten). docs/compat.md:229 only lists `mouseReleased` among the unchanged API, and :242 describes the press and
  the drag, not the release. tools/client/README.md mentions releases only for the `@trascina` step and the script's
  clean-up. So no doc line changed for A.
- Super-call check (beyond the final review's vanilla javap): `javap -c` on the cached Mojang-named patched jars shows
  that Forge 1.20.1 (47.4.29) and NeoForge 1.21.1 (21.1.266) patch `AbstractContainerScreen.mouseReleased` to call
  `Screen.mouseReleased` first (offset 5, `invokespecial`, result popped; vanilla and Fabric make no such call). That
  default (`ContainerEventHandler.mouseReleased`) only clears the widget-drag flag and forwards the release to a child
  widget. The station screen has no child widgets (no `addRenderableWidget` or `addWidget` in StationScreen,
  CompatContainerScreen or ContainerScreenBase), and the rotation press never reached `super.mouseClicked`, so no
  drag flag was set. Skipping `super` is safe on all four variants.
  - Checked with `javap -c` on the same patched jars. `Screen` does not override `mouseReleased` on either version,
    so the call lands in `ContainerEventHandler.mouseReleased` (default method).
  - Forge 1.20.1: `setDragging(false)`, then `getChildAt(x, y).filter(child -> child.mouseReleased(...)).isPresent()`.
  - NeoForge 1.21.1: if the button is 0 and `isDragging()`, `setDragging(false)`, and with a focused child it returns
    `getFocused().mouseReleased(...)`; otherwise the same `getChildAt` forward.
  - Both are widget-only, so the claim in commit 1's message ("serve solo ai widget") holds.
- Ruling P28, checklist `docs/test-in-gioco/m4a1-banco.md:121-123`, step 12: after the pile of ingots is taken onto
  the cursor, two indented sub-steps as in step 5. The first is new ("Girare la tuta con la pila sul cursore"): press
  on the suit, drag to the right out of the screen onto the world around it (inside the game window) and release
  there; the suit turns, the pile stays on the cursor and nothing falls. Again, released over an empty slot: the suit
  turns, the pile stays on the cursor and the slot stays empty. Otherwise note it with a screenshot and say where the
  button was released. The second sub-step is the old distribution ("Distribuire la pila"): the same instruction,
  with the lead-in "Con la pila ancora sul cursore". It lands with C5 (same step).
- Evidence: see "Cloud check of P22" below.

### B. Rulings P24 + P27: the owner's name (commit 2)

Done by a script of exact substrings, each with its expected count (no blind sed):
`<scratchpad>/m4a1/fix/owner_replace.py`. "di/da/a/per
il proprietario" became "del/dal/al/per il proprietario"; sentence starts got "Il proprietario". Lines changed (fixed tree):

- `docs/arte/README.md`: :3, :7 (2 occurrences), :11 (4), :25, :36 (2), :49, :53, :65, :85. That is 14 occurrences
  on 9 lines.
- `docs/audit/mjolnir-armor-audit.md`: :3, :16, :595, :601, :604, :637 (heading "## 10. Domande aperte per il
  proprietario"), and :646 "come dice il documento iniziale?" → "come dice il documento iniziale del proprietario?".
- `src/main/java/com/toxineren/exoframes/client/SuitScale.java:13-16`: the Javadoc paragraph, reflowed to stay within
  120 characters.
- `src/test/java/com/toxineren/exoframes/client/SuitScaleTest.java:63` ("Richiesta del proprietario:").
- `tools/art/README.md:174`.
- `tools/art/designs/frame.py`: docstring :2-8 (two occurrences, paragraph reflowed), :16-17, comments :32-34 and
  :200-202 (reflowed).
- `tools/art/designs/scrap.py`: docstring :2-4 (reflowed) and :6; comment :23-25 (broken by hand into three lines);
  comment :164-165.
- `tools/art/designs/_legs.py:1-2`.
- `docs/test-in-gioco/m1-setup.md:3` (two occurrences) and :83; `m2b-compat.md:3`; `m3a-vedere-il-telaio.md` :3, :79,
  :89, :93, :108; `m3b-entrare-e-uscire.md` :3, :139, :143 (heading "## 7. Domande per il proprietario"), :145;
  `m3c-giocarci.md` :3, :166, :381, :385 (heading "## 22. Domande per il proprietario"), :387.
- Checks:
  - `git grep -n -w il proprietario -- . ':!docs/superpowers/**'` prints nothing. The case-insensitive form prints nothing too.
  - No anchor links to the three renamed headings exist anywhere in the repository. m3d and m4a1 already use
    "Domande per il proprietario".
  - A word diff of the Java and tools files shows only the name words changed, plus tokens moved by the reflow. No
    edited Java or Python line is over 120 characters.
- Non-prose occurrences left: none. Every occurrence was prose or a comment or docstring. The design docstrings do
  not reach the generated art: build.py reads only its own `__doc__`, for argparse.
- Art output unchanged: `python3 tools/art/build.py` (in place, as tools/art/README.md says; numpy 2.5.3 as pinned)
  exits 0 in 18 s. Then `git status --short -- src/main/resources/assets/redstone_exoframes/geo
  .../textures/entity .../animations` prints nothing: the regenerated geometry, atlas, glow mask and animations are
  byte-identical. `check_art.sh` is in the verification below.
- Left untouched, as the brief says: README.md:11 (a link to a file by its name) and everything under
  docs/superpowers/**.
- Observations, not changed because they are outside the brief:
  - "the PC assistant", the name of the assistant on the PC, in m1-setup.md:3, m2b-compat.md:3, m3a-vedere-il-telaio.md:3,
    m3b-entrare-e-uscire.md:3 and m3c-giocarci.md:3. The newer checklists say "l'assistente".
  - The vendor-named file name `<documento iniziale>` at docs/audit/mjolnir-armor-audit.md:3,
    a reference to the file by its name, like README.md:11.
  - `mod_authors=ToxinEren` (a handle) at mjolnir-armor-audit.md:595. It is also the package name.

### C. The seven deferred minors (commit 3)

1. `src/main/java/com/toxineren/exoframes/content/StationBlock.java:168`: "Due limiti accettati (decisione P13),
   perche'..." → "Due limiti accettati, perche'..."; the reasons are unchanged. A grep for `decisione P[0-9]`,
   `Ruling P[0-9]` and `\bP[0-9]{1,2}\b` in src, tools and docs (outside docs/superpowers) now finds only a test
   argument `"P1"` in SuitBonesTest.java:55, which is not a ledger ID.
2. `src/main/java/com/toxineren/exoframes/gametest/StationMenuGameTests.java:181`: `p.setGameMode(GameType.SURVIVAL);`
   right after the mock player is created in `placeholderButtonIgnoresSpectatorsAndTheDead`, as the other tests do
   (for example :143). The positive control at :186 now runs in a set game mode.
3. Base-game Italian labels in `docs/test-in-gioco/m4a1-banco.md`: :135 "Scala dell'interfaccia" → "Scala"; :137 "Sei
   morto!" → "Sei morto/a!"; :138 "Apri in LAN" → "Rete locale..." and "Avvia il mondo in LAN" → "Apri il mondo in rete
   locale".
   - **Verified on both versions:** the asset indexes 1.20.1 (`indexes/1.20.1-5.json`) and 1.21.1
     (`indexes/1.21.1-17.json`) in `~/.gradle/caches/fabric-loom/assets` both point `minecraft/lang/it_it.json` to the
     same object, `2cc820c6664c248ef625ce8bd72b88125f38af2b` (size 621865; `sha1sum` matches). In it:
     `options.guiScale` = "Scala", `deathScreen.title` = "Sei morto/a!", `menu.shareToLan` = "Rete locale...",
     `lanServer.start` = "Apri il mondo in rete locale".
   - The English sides come from `assets/minecraft/lang/en_us.json` in both cached client jars. They are the same on
     both versions ("GUI Scale", "You Died!", "Open to LAN", "Start LAN World"), so the keys are right.
   - One wording serves both versions.
   - The other quoted labels are also right on both: Giocatore singolo, Crea un nuovo mondo, Gioco, Mondo,
     Sopravvivenza, Multigiocatore, Fatto, Opzioni..., Grafica..., Comandi, Tipo, Piatto, Strutture, Rinasci and Nuove
     ricette sbloccate!.
4. `docs/compat.md:47`: "(... a schermata aperta dopo `/exoframe unlock`; nella 4a-1 la vista non cambia nulla che si
   veda)" → "(... dopo `/exoframe unlock`; quel cambio non si vede: il lucchetto è della 4a-2)".
   - Its twins in tools/client/README.md:153 and the m4a1_shots.sh phase-11 header already speak of "la vista nuova"
     after the unlock, which is true, so they are left as they are.
5. Checklist step 12, `m4a1-banco.md:121`: "Ora girala di circa un quarto di giro trascinando **verso destra** e
   lascia: il petto della tuta va verso destra e il braccio che nell'anteprima era a sinistra (il braccio destro della
   tuta) viene verso di te; alla fine la tuta è di fianco, con il petto verso destra, come in [girata]". A parenthesis
   adds that dragging left turns it the other way, while the reference was turned right.
   - The direction was measured in the cloud (shots `destra-30` and `destra-90`, below) and matches the transform.
   - The new sub-step of A is in the same step.
6. Checklist step 16, `m4a1-banco.md:135`: the window range narrowed to 640-660 × 480-500, "non meno" (below 640×480
   the game falls back to scale 1). The assistant notes the `Display:` value. The text now says the honest size: at
   that size the highest usable scale is 2 and the interface is half the window, rounded up, so 320×240 at `640x480`
   and at most 330×250 at `660x500`.
   - The margins at `640x480` are 2 interface pixels left and right and 4 above and below.
   - This follows vanilla `Window.calculateScale` (w/(i+1) >= 320 and h/(i+1) >= 240) and the rounding up in
     `setGuiScale`.
   - Checked with `javap -c` on `com.mojang.blaze3d.platform.Window` in the cached Mojang-named jars of 1.20.1 and
     1.21.1; the bytecode is the same on both.
   - `calculateScale(int guiScale, boolean forceUnicode)`: `i = 1`, then `i++` while `i != guiScale && i <
     framebufferWidth && i < framebufferHeight && framebufferWidth / (i + 1) >= 320 && framebufferHeight / (i + 1) >=
     240`; then, if `forceUnicode` and `i` is odd, `i++`.
   - `setGuiScale(double)`: `guiScaledWidth = (int) (w / scale)`, plus 1 if `w / scale` is larger, and the same for
     the height. That is a ceiling.
   - So at 640-660 × 480-500 the scale is 2 and the interface is 320-330 × 240-250. Below 640 or 480 it is scale 1.
   - The one exception: with the "Forza font Unicode" option on (off by default), scale 1 becomes 2. The checklist
     does not mention it.
7. Checklist step 18, `m4a1-banco.md:137`: "Subito: clic destro sul fronte di una delle due metà, sotto il telaio (si
   apre; non cliccare il telaio: a mano vuota ci entreresti)". FrameInteractions: an empty hand while standing enters
   the frame.

## Cloud check of P22

Two runs of `tools/client/client_shot.sh` on **1.21.1-neoforge**, with the same 41 steps (script
`<scratchpad>/m4a1/fix/p22_check.sh`, validated first with
`EXO_SHOT_STEPS_ONLY=true` → `OK passi: 41`). The first run is a negative control on the unmodified tree (a9cb5cb),
the second runs on the fixed StationScreen. Folders (not committed): `.../scratchpad/m4a1/fix/p22-neg/` and
`.../scratchpad/m4a1/fix/p22-fix/` (shots, `rcon.log`, `run.txt`, client and server logs).

Steps (window points; the station screen is `632x464+164+38`, the preview clip `184x232+596+74`, the first slot of
the main inventory, `inventory.0` = `Slot:9b`, is centred at (196, 354)):

1. `@attendi 12`; the station placed with the item (`tp Exo 0.5 -60 0.5 0 55`, `@click destro`); `tp` onto the main
   half and `execute as Exo run exoframe debug place rottame`; `item replace entity Exo inventory.0 with
   minecraft:iron_ingot 3`; the station opened (`tp Exo 0.5 -60 0.5 0 60`, `@click destro`); shot `aperta`.
2. For item C5, with an empty cursor: `@trascina 620 250 660 250` (20 GUI px, 30 degrees), shot `destra-30`;
   `@trascina 620 250 700 250` (another 60 degrees, 90 in all), shot `destra-90`.
3. The pile onto the cursor: `@clic 196 354`; shot `pila-sul-cursore`; check
   `execute unless data entity Exo Inventory[{id:"minecraft:iron_ingot"}]` (the ingots left the inventory).
4. Case 1, release outside the screen: `@trascina 620 250 100 250` (pressed in the preview, released over the world
   left of the screen); shot `rilascio-fuori`; checks `execute at Exo unless entity
   @e[type=minecraft:item,distance=..8]`, `execute unless entity @e[type=minecraft:item]`, `execute unless data entity
   Exo Inventory[{id:"minecraft:iron_ingot"}]`. All checks run right after the release, well inside the 2 s pickup
   delay of a thrown item.
5. Case 2, release over the empty slot: `@trascina 620 250 196 354`; shot `rilascio-sullo-slot`; checks
   `execute unless data entity Exo Inventory[{Slot:9b}]` (the slot stayed empty), `execute unless entity
   @e[type=minecraft:item]`.
6. `@tieni Escape 0.2`; shot `chiusa`; checks `execute store result score #ingots exo_shot run clear Exo
   minecraft:iron_ingot 0` and `execute if score #ingots exo_shot matches 3` (all three ingots back in the inventory),
   `execute at Exo unless entity @e[type=minecraft:item,distance=..8]`, `execute unless entity @e[type=minecraft:item]`.
7. Positive control (vanilla still throws on a plain click outside): ingots reset into `inventory.0`, station
   reopened, `@clic 196 354` (pile on the cursor), `@clic 100 250` (a plain press and release outside the screen);
   shot `controllo-clic-fuori`; check `execute at Exo if entity @e[type=minecraft:item,distance=..8]`; `kill
   @e[type=minecraft:item]`; Escape.

Every command goes through client_shot.sh's `execute store success` wrapper: a failing check stops the run with
exit 1.

**Negative control, a9cb5cb (unfixed), 104 s:** exit 1, as expected, at step 4:
`ERRORE: comando rifiutato o non riuscito: "execute at Exo unless entity @e[type=minecraft:item,distance=..8]"`.
`rcon.log`: the step-3 check `Test passed` / `#exo has 1`, then the step-4 check with no reply and `#exo has 0`, so an
item entity was near the player. Shot `rilascio-fuori`: the cursor is empty (no ingots at the pointer) and the suit
has turned. The old release threw the pile, so the steps exercise the path.

**Fixed tree, 104 s:** exit 0, `log-check.txt` empty (0 bytes), `log: nessun avviso o errore del mod fuori
dall'elenco degli avvisi voluti`. The rcon replies, in order (each followed by `#exo has 1 [exo_shot]`):

| Check | Reply |
|---|---|
| step 3: `execute unless data entity Exo Inventory[{id:"minecraft:iron_ingot"}]` | `Test passed` |
| step 4: `execute at Exo unless entity @e[type=minecraft:item,distance=..8]` | `Test passed` |
| step 4: `execute unless entity @e[type=minecraft:item]` | `Test passed` |
| step 4: `execute unless data entity Exo Inventory[{id:"minecraft:iron_ingot"}]` | `Test passed` |
| step 5: `execute unless data entity Exo Inventory[{Slot:9b}]` | `Test passed` |
| step 5: `execute unless entity @e[type=minecraft:item]` | `Test passed` |
| step 6: `execute store result score #ingots exo_shot run clear Exo minecraft:iron_ingot 0` | `Found 3 matching item(s) on player Exo` |
| step 6: `execute if score #ingots exo_shot matches 3` | `Test passed` |
| step 6: `execute at Exo unless entity @e[type=minecraft:item,distance=..8]` | `Test passed` |
| step 6: `execute unless entity @e[type=minecraft:item]` | `Test passed` |
| step 7: `execute at Exo if entity @e[type=minecraft:item,distance=..8]` | `Test passed, count: 1` |
| step 7: `kill @e[type=minecraft:item]` | `Killed Iron Ingot` |

Shots (fixed run): `rilascio-fuori` shows the three ingots on the cursor at the pointer, over the world left of the
screen, with the suit turned; `rilascio-sullo-slot` shows them at the pointer over the first slot, which `rcon`
proves empty; `controllo-clic-fuori` shows an empty cursor after the plain click outside.

**Result:** with the fix, a rotation drag released outside the screen or over an empty slot leaves the pile on the
cursor, nothing falls, and Escape returns all three ingots to the inventory. On the unfixed tree the same steps threw
the pile. A plain click outside the screen still throws the pile, as in vanilla.

**Direction of the drag (for C5, from the shots `aperta`, `destra-30`, `destra-90` of the negative-control run, with
the suit in its own colours; the rotation code is the same on both trees):** dragging to the right turns the suit's
chest to the right; the arm that is on the left of the preview at the start (the suit's right arm) comes toward the
viewer; after a quarter turn the suit is side-on with the chest to the right, as in the `girata` reference shots. The
same follows from SuitPreview's transform: angle 90 gives yaw 90, a facing of -x, which `Rz(180)` and `(s, s, -s)`
turn into +x on the screen.

## Verification

Run once, after all edits, one Gradle or client run at a time. Outputs are in
`<scratchpad>/m4a1/fix/`.

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
  :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`
  (`build.txt`) → `BUILD SUCCESSFUL in 40s`. `compileJava` and `test` re-ran on all four variants.
- JUnit count (the command in contesto-comune.md) (`junit-count.txt`):
  ```
  1.20.1-forge 358
  1.20.1-fabric 358
  1.21.1-fabric 358
  1.21.1-neoforge 358
  ```
  Each variant has 48 suites, all `failures="0" errors="0"`, with results written at 05:31.
- `checks.txt`:
  - `bash tools/check_sources.sh` → `OK sources` (exit 0)
  - `bash tools/check_jars.sh` → `OK jars` (exit 0)
  - `bash tools/check_data.sh` → `OK data` (exit 0)
  - `bash tools/check_art.sh` → `OK art` (exit 0)
  - `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure` (exit 0)
  - `bash tools/client/client_shot.sh --selftest` → `OK passi` (exit 0)
- `bash tools/run_gametests.sh` (`gametests.txt`), exit 0 after 112 s:
  ```
  OK 1.20.1-forge: 235 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 235 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 235 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 235 test obbligatori passati, nessun avviso fuori elenco
  ```
  `versions/<v>/build/gametest/server.log` holds `All 235 required tests passed` on each. The edited test is
  registered at StationMenuGameTests.java:111.
- `EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh <v> .../fix/m4a1-<v>`, one at a time (`m4a1-<v>/run.txt`):

  | Variant | exit | time | anteprima | pulsante | rotazione | `log-check.txt` |
  |---|---|---|---|---|---|---|
  | 1.20.1-forge | 0, `OK 1.20.1-forge` | 133 s | ok, magenta 18582 / 18582 | ok, 1112 px in `36x32+730+410` | ok, 24587 px; magenta 9225 in `111x198+629+96` | 0 bytes |
  | 1.20.1-fabric | 0, `OK 1.20.1-fabric` | 117 s | ok, 18582 / 18582 | ok, 1112 px | ok, 24587 px; magenta 9225 | 0 bytes |
  | 1.21.1-fabric | 0, `OK 1.21.1-fabric` | 125 s | ok, 18582 / 18582 | ok, 1112 px | ok, 24332 px; magenta 9225 | 0 bytes |
  | 1.21.1-neoforge | 0, `OK 1.21.1-neoforge` | 122 s | ok, 18582 / 18582 | ok, 1112 px | ok, 24332 px; magenta 9225 | 0 bytes |

  Three `ok` on each variant, the same measures as Task 6 (24587 on 1.20.1, 24332 on 1.21.1). Extra check: the new
  shots `stazione-posata`, `stazione-aperta`, `girata` and `suggerimento-torso` of 1.20.1-forge and 1.21.1-neoforge
  against the committed reference shots in docs/test-in-gioco/m4a1-screenshot/: `compare -metric AE` = 0 for all
  eight, so the fix changes nothing that is drawn.
- Checklist Step 4 greps (`final-greps.txt`):
  - `grep -c '^- \[ \] \*\*[0-9]*\.\*\*' docs/test-in-gioco/m4a1-banco.md` → `20`;
  - `grep -n '^- \[ \] \*\*1\.\*\*' docs/test-in-gioco/m4a1-banco.md` → one line (85);
  - the "ogni scatto nominato esiste" loop → no output; 10 shots are named.
- `git grep -n -w il proprietario -- . ':!docs/superpowers/**'` → nothing (exit 1).
- `git status --short` lists the 18 changed source and doc files and no `.png` (`git status --short | grep -i
  '\.png'` → exit 1). The shots and logs stayed in the scratchpad.
- The two P22 client runs (above) came before this verification, on the same StationScreen; the verification build
  and the four m4a1 runs compiled the final tree.

## Commits

On `m4-stazione`, on top of a9cb5cb, not pushed. Each commit carries a `gpgsig` header (signed by the environment's
SSH signing) and ends with the two trailer lines (`git log -1 --format=%B <c> | grep -c` of the two exact lines → 2
for each). `%G?` prints N only because no `gpg.ssh.allowedSignersFile` is configured for verification, as for the
earlier commits of the part.

| Commit | Subject | Files |
|---|---|---|
| `937b28a` | client: il rilascio che chiude una rotazione non arriva al gioco base (la pila sul cursore resta); checklist | StationScreen.java; m4a1-banco.md step 12 (A, P28 sub-step, C5) |
| `73aaa25` | docs: il proprietario al posto del nome nei documenti e nei commenti | the 13 files of B |
| `f6da189` | docs, test: le correzioni minori della revisione finale della 4a-1 | StationBlock.java, StationMenuGameTests.java, docs/compat.md, m4a1-banco.md steps 16, 18 and 19 (C1-C4, C6, C7) |

`git diff --stat a9cb5cb HEAD` → `18 files changed, 86 insertions(+), 72 deletions(-)`; `git status --short` → empty.
The checklist was staged for commit 1 right after the step-12 edits, and the later edits stayed in the working tree
until commit 3. The commits split exactly the tree that was verified: no file changed after the verification, except
this git-ignored report.

## Concerns and notes for the coordinator

- The final review said vanilla `mouseReleased` makes no super call. That is true for vanilla and Fabric, but
  Forge 1.20.1 and NeoForge 1.21.1 patch it to call `Screen.mouseReleased` first. Skipping it is still safe, because
  the station screen has no widgets and the rotation press never reached `super` (details under A). This does not
  change the fix.
- The P22 evidence has two runs, not one. I added a negative control on the unfixed tree (a9cb5cb) with the same
  steps, so that the passing run means something. That costs one extra client run of 104 s. The fixed run also covers
  the release over a slot and a positive control (a plain click outside still throws).
- The negative control stops at the first failing check, by design. So the release-over-slot case has no unfixed-run
  evidence; that case rests on the bytecode reading in final-review I-1.
- Left untouched, outside the brief, to mention to the owner with ruling P27:
  - "the PC assistant" (the PC assistant's name) on line 3 of five older checklists;
  - the vendor-named file name at docs/audit/mjolnir-armor-audit.md:3;
  - the handle `ToxinEren` at audit:595, which is also the Java package name.
