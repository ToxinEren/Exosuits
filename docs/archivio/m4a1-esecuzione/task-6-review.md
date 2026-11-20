# Task 6 review: 14df684..51e6a69 (`docs: checklist della parte 4a-1, scatti di riferimento, documenti; tools: m4a1_shots.sh …` + the step 7/18 follow-up)

I read the diff file (560 lines, 152 KB) in six passes: lines 1-80, 80-160, 160-220, 219-258, 258-313 and 313-560. I checked all ten PNGs' metadata and viewed seven of them. Outside the diff I made only the focused checks listed under "Checks run", one per named risk.

### Spec Compliance

- ✅ **Spec compliant.** Every "Produces" item is present, and so are the twenty steps in the brief's order, the coordinator rulings and the carried items. There is one plan-mandated defect: step 17 (under Important).
  - **Header** (`docs/test-in-gioco/m4a1-banco.md:1-9`):
    - who the checklist is for: "il proprietario", "l'assistente";
    - which jars to use: `m4-stazione`, or `main` after the merge;
    - how to recognise them (:5, the Station after the Cell Charger);
    - "guarda e basta" (:7).
  - **"Cosa c'è da vedere"** (:11-27) has every element the brief lists:
    - block, recipe, opening, two messages and screen;
    - the white border on the first mounted piece, in `PieceSlot` order (:19; checked against `core/armor/PieceSlot.java:5-10`);
    - button and closing.
  - **"Cosa non c'è ancora"** (:29-35) has everything the brief lists, plus the P21 note on empty places (:32).
  - **"Già verificato"** (:37-66):
    - the area counts: `StationBlockGameTests` 10 and `StationMenuGameTests` 17 (checked by counting `add(tests,`);
    - the JUnit tests: 8/6/3/7, plus the three config tests (checked by counting `@Test` and grepping the names);
    - the check table on the four variants, quoting the real P19 `rotazione` check (:52; matches `tools/client/m4a1_shots.sh:265-267`);
    - the dedicated server (:54) and the "non prova" list (:66).
  - **"0. Preparazione" A-F** (:68-81): A-F repeat the model's preparation; they do not just point back to it. C (:72-76) moves the creation of the 3c config file before the first launch. Without that move, the brief's step 2 would test a file the new jar had just written. That is a necessary and correct change.
  - **Steps 1-20** (:85-139) follow the brief's order and content:
    - **P13:** the two known limits are in step 5 (:99).
    - **P22:** step 12 rotates only with an empty cursor (:121).
    - **P23:** `gamerule naturalRegeneration false` plus `wither 120 0`, put back afterwards (:135).
    - **"Cosa riportare"** is at :141-152.
    - **The three questions** (:154-160) map to design §9 q.7 and §8 points 4, 5 and 6.
  - **Shots:** ten, named `<variante>-<scatto>.png`, only under `docs/test-in-gioco/m4a1-screenshot/`.
  - **Docs:**
    - `README.md:9`: "Stato", with 4a-1 and "otto ricette";
    - `README.md:13-14`: "per il proprietario" and the M4 design entry;
    - `README.md:32`: "oggi 235 test";
    - `README.md:38`: `m4a1_shots.sh` in the client-scripts paragraph;
    - `README.md:60,63`: the "Documenti" entries;
    - `tools/client/README.md`: the measured table and the shots (:157-163), the durations (:208) and "Cosa non copre" (:216);
    - `docs/compat.md`: BlockBase now has three conditions (:133; checked in `compat/impl/BlockBase.java:12,65,79`), the P13 tag is gone with its reasons kept (:132), the view bullet is rewritten (:256), and `station_view` is "sulle quattro varianti" (:47);
    - the script header (comments only).
  - **The brief's greps:**
    - 20 numbered steps;
    - one `**1.**`, at :85;
    - all 10 named shots exist;
    - `208 test` → 0;
    - `m4a1_shots` → 9 lines.
- ⚠️ **Cannot verify from the diff:**
  - **The runs and the final verification.** I did not re-run them. The evidence files are in the scratchpad and agree with the report:
    - `step6-gametests.txt`: four `OK <v>: 235 …` lines;
    - `step6-junit-count.txt`: 358 ×4;
    - `log-check.txt`: 0 bytes in the four `shots-*` and the two `colori-*` runs;
    - `step3-*.txt`: `Config file` 0 lines, hash unchanged.
  - **The Fabric shots** are not in the repository. The claim that the four variants are pixel-identical inside the screen rests on the report. The measures in `step1-*.txt` are identical.
  - **Vanilla behaviour the PC steps rely on**, which nobody ran:
    - the lever on top of a piston;
    - the TNT drop count;
    - `/kick` from a LAN host (javap reasoning in the report);
    - the wither timing on 1.21.1 (measured only on 1.20.1-forge);
    - that production Forge and NeoForge logs carry no line matching `Config file` case-insensitively. Step 2's `Select-String` is case-insensitive, and the dev logs of all four variants have 0 such lines.

### Strengths

- **The checklist is followable literally almost everywhere.** Every mod text matches both language files: the messages, the four tooltip lines, the slot and item names, and the `lock`/`unlock`/`remove`/`debug place` replies. The quoted numbers are right:
  - 180 is the Scrap max durability for every slot (`ExoframesConfig.java:73`);
  - 10000 is the cell capacity (`:33`);
  - 0.9 s and 17.5 s are the break times (hardness 3.5, ÷30 with the tool, ÷100 without);
  - 40 s is the time to die with wither 0.

  I computed the break times, the death time and the ~28° piston pitch in step 7 from the vanilla formulas; I did not measure them. The implementer measured the death time (~42 s, on 1.20.1-forge).
- **The edge cases are designed carefully.**
  - **Step 7** now places the piston first. At a pitch of about 28° the nearest looking direction is horizontal, so the head faces the player.
  - **Step 15**'s distances of 1.5 and 2.5 follow from `ExoframeItem.useOn`, which uses `Vec3.atBottomCenterOf` (`content/ExoframeItem.java:72`).
  - **The hole and the station top.** `FramePlacement.place` rejects a frame only when `level.noCollision(SuitSize.standingBox(feet))` fails or another frame is in the box (`frame/FramePlacement.java:45-47`). The box is 0.8 × 2.97, centred on the block (`frame/SuitSize.java:106-110`). So the frame fits in the one-block hole beside the station, and on the station top with three free blocks above, as steps 9 and 15 say.
  - **Step 19** uses `/kick` and collects the emeralds on the host side. That avoids the respawn-pickup confound. `Player.remove` → `doCloseContainer` → `removed` → `drop` for a disconnected player holds on both versions (javap, `Player.remove` offsets 10/21/28 and `AbstractContainerMenu.removed` 20/30/39).
  - **Step 20** reuses "il telaio del passo 15 o del passo 17", so it works on the variants that skip 16-18.
- **The screen description matches `StationScreen`:**
  - the arrows `<` `>` (:331-334);
  - tooltips suppressed with a stack on the cursor (:157);
  - the empty cell place dark with a grey border (:245, :263-266);
  - the empty-slot header in `TEXT_DIM` (:302).
- **The shots are clean and match the text.**
  - The ten PNGs are 960×540 and contain only IHDR, IDAT and IEND.
  - Inside `632x464+164+38`, forge and neoforge are identical (AE 0) in `stazione-aperta` and `stazione-colori-suoi`. In `girata` and `suggerimento-torso` they differ in 11906 pixels, with a maximum channel difference of 257/65535 = 1/255. That is exactly the docs' claim.
  - `stazione-posata` shows the red light and drawer on the left and the grid on the right, with the side half cut by the edge (P15).
  - The station item icon is "un banco con sopra un telaio piccolo".
- **The docs are true to the code where they changed.**
  - BlockBase has three `//? if >=1.21` conditions.
  - The 2 s cleanup claim matches `Handouts.PICKUP_DELAY = 40`.
  - The script diff is comment-only, ASCII, with lines of at most 120 characters.
  - The private first name is gone from `README.md:13`.
  - The added lines contain no ledger IDs, AI or vendor names, local paths or e-mails.

### Issues

#### Critical (Must Fix)

None.

#### Important (Should Fix)

1. **Plan-mandated: step 17 expects a resource reload with the station screen open, which cannot happen on any variant.**
   - **Where:** `docs/test-in-gioco/m4a1-banco.md:134`.
   - **The step:** "Con la schermata aperta tieni premuto F3 e premi T. Il gioco ricarica le risorse … e **non si chiude**."
   - **Why it cannot happen:** `KeyboardHandler.keyPress` calls `handleDebugKeys` (F3+T, F3+H, …) only when no screen is open. I checked with javap:
     - **vanilla 1.20.1** (Fabric 1.20.1): `getfield Minecraft.screen; ifnonnull 929` at offset 580-583, before `handleDebugKeys` at 809;
     - **Forge 47.4.29:** `ifnonnull 925` at 579, before `handleDebugKeys` at 805;
     - **vanilla 1.21.1 and NeoForge 21.1.266:** the flag is `screen == null || (screen instanceof PauseScreen && !showsPauseMenu())`, offsets 598-653. `handleDebugKeys` at 771 (782 on NeoForge) is reached only when it is true (`iload 11; ifeq 872`).
   - **Effect on the step:** the fallback the implementer added ("Se con la schermata aperta F3+T non fa nulla … chiudi la stazione, premi F3+T senza schermata e poi riaprila") makes the step completable. But the first expectation is wrong on all four variants, so the assistant will always report a deviation.
   - **Effect on the docs:** four places promise the PC covers "F3+T con la schermata aperta", which no keyboard path can produce:
     - `docs/test-in-gioco/m4a1-banco.md:66`;
     - `tools/client/README.md:163` and `:216`;
     - `tools/client/m4a1_shots.sh:106`.
   - **Why it matters:** the brief mandated this step (item (17), `task-6-brief.md:19`), and plan Review Focus 5 leans on it (`contesto-comune.md:52`). The step cannot exercise "una ricarica delle risorse con la schermata aperta", and the docs claim a coverage that does not exist.
   - **Fix:**
     - make the fallback the step: open, close, F3+T with no screen, reopen, the suit is there, count `Suit rendering failed`;
     - add one line saying the base game ignores F3 shortcuts while a container screen is open;
     - reword the four "non prova" mentions to "F3+T e poi la schermata riaperta";
     - tell the coordinator that Review Focus 5's reload-with-screen-open case is unreachable from the keyboard. `SuitPreview`'s `finally` still covers GeckoLib exceptions.

#### Minor (Nice to Have)

1. **Some Italian labels of the base game are wrong.** The English labels in parentheses are right, and `m3d-braccio.md:87,85` had the correct Italian ones. Fix each to the vanilla it_it text:
   - **`m4a1-banco.md:136`:**
     - "Apri in LAN" should be "Rete locale..." (`menu.shareToLan`);
     - "Avvia il mondo in LAN" should be "Apri il mondo in rete locale" (`lanServer.start`).
   - **`:133`:** "Scala dell'interfaccia" should be "Scala" (`options.guiScale`). This wording came from the brief.
   - **`:135`:** "Sei morto!" should be "Sei morto/a!" (`deathScreen.title`).
2. **`docs/compat.md:47` still says "nella 4a-1 la vista non cambia nulla che si veda".**
   - This is the generalisation the report calls false (a piece mounted by another player shows up), and `:256`, rewritten in this diff, now contradicts it.
   - Suggested: "nella 4a-1 quel cambio non si vede (il lucchetto è della 4a-2)".
   - `tools/client/README.md:153` and `m4a1_shots.sh:65` say "la vista nuova", scoped to the unlock, and are fine.
3. **`m4a1-banco.md:121` (step 12) does not say which way to drag.**
   - The `girata` reference was made by dragging right (620→740). Dragging left shows the mirrored profile, which a literal assistant may report as a difference.
   - Fix: add "verso destra".
4. **`m4a1-banco.md:133` (step 16) overstates the interface size.**
   - The accepted window range (640-700 × 480-520) gives an interface of up to 350×260, not "320×240", so the minimum is tested only at about 640×480.
   - Ask the assistant to note the `Display:` value used.
5. **`m4a1-banco.md:135` (step 18) does not say where to click.** "clic destro sulla stazione (si apre)" sits in survival, empty-handed, under an armed frame. A click on the frame would enter it. Repeat step 10's "sul fronte, sotto il telaio".
6. **Optional: step 19 could cover the one visible view change.**
   - Step 19 already has two players at the station. It is the only place in 4a-1 where a visible view change can be checked: the second player mounts or unmounts a piece by hand on the host's unlocked frame while the host has the screen open, and the panel should change within a tick.
   - `docs/compat.md:256` now states this behaviour; it is proven on the server by `viewFollowsTheRecordWithinATick`, but never on screen. Plan decision 5 says 4a-1 has no visible proof.
   - The brief did not ask for this.

### Checks run

Each risk I named, with what I checked:

- **Mod texts.** `lang/it_it.json` and `lang/en_us.json` against every quoted message and tooltip: all match.
- **Vanilla labels.** `it_it.json` from the Loom asset index (hash `2cc820c6…`, the same for 1.20.1 and 1.21.1) and `en_us.json` from both client jars.
- **F3+T.** `javap -c` of `KeyboardHandler.keyPress` in the four game jars (offsets above).
- **Selection order.** `PieceSlot` order; Scrap durability 180 and `cellCapacity` 10000.
- **The screen.** `StationScreen` arrows, carried-stack tooltips, empty cell and the tooltip lines.
- **Frame placement.** `ExoframeItem.useOn` placement point and the frame width.
- **Cleanup delay.** `Handouts.PICKUP_DELAY`.
- **Locked frames.** `FramePermissions.canUse`: a creative operator bypasses the lock, which justifies step 19's `/gamemode survival`.
- **Disconnect.** `Player.remove` and `AbstractContainerMenu.removed` on both versions.
- **Test names.** Test counts and names in `StationBlockGameTests`, `StationMenuGameTests`, `DataGameTests` and the four JUnit classes.
- **Log texts.** The texts that steps 1 and 20 search for exist: `StationMenu.java:318,385`, `SuitDraw.java:85`, and vanilla `MenuScreens` "Failed to create screen for menu type".
- **PNGs.** Chunk types and sizes; `compare -metric AE` inside and outside the screen; per-channel maximum difference.
- **Script.** The diff touches only `#` lines; ASCII; no line over 120 characters.
- **`docs/compat.md` conditions.** BlockBase's three conditions; the `CompatContainerScreen` and `StationScreen` entries reread.
- **Privacy.** Grep of the added lines for AI/vendor names, the first name, local paths and e-mails: none.
- **Commits.** Both are signed and carry the environment's trailers.
- **Frame placement rule.** `FramePlacement.place` and `SuitSize.standingBox` (cited above).
- **Stale counts.** `sette ricette`, `208` and `331` in `tools/README.md`, `tools/client/README.md`, `docs/compat.md` and `README.md`. The only hit is `tools/client/README.md:118`, which is scoped to the 3c-2 sequence (`m3c2_shots.sh` has 7 `recipe take`) and already points to `m4a1_shots.sh` for the station. `tools/README.md:80` says "8 ricette". The report's "tools/README.md needed nothing" holds.
- **Noise in the reported test output** (`step6-gametests.txt`, `step3-*.txt`):
  - the WARN and ERROR lines of the registry-corruption tests, which the allowlist covers ("nessun avviso fuori elenco" on all four);
  - `No data fixer registered for exoframe` on Fabric, allowlisted at `tools/log_allowlist.txt:50-51`;
  - Gradle's "Consider enabling configuration cache" hint.

  All three are expected or pre-existing, not introduced by this task: no finding.

### Assessment

**Task quality:** Needs fixes

**Reasoning:** The checklist, shots and docs are thorough and accurate, and almost every step is followable literally. The exception is step 17: it is built on a resource reload with the screen open, which the base game's keyboard handler never performs on any of the four variants. Its expectation, and four "non prova" mentions, need rewording. The fix is a few lines of docs.
