# Final review of part 3c-1: the findings to fix

Branch m3c-giocarci, review range 2e35aa6..d67e8b6. Three reviewers read the whole branch (reports: final-review-A.md, final-review-B.md, final-review-C.md in this folder). No Critical finding; two Important ones, both in area C. This file is the complete list for the single fix wave, in the order to work. Finding ids are the ones the re-review will use.

The ids under verification: FC-1, FC-2, FA-1, FA-3, FB-1, FC-3, FC-4, FC-6, T16-F1, T24-F1, T22-R1-B2, FC-5.

## 1. Code: no start may destroy stored energy

### [FC-1] (Important) One start with the config at its defaults permanently cuts the charge of every cell in the frame registry

From tasks: T8, T20. Reviewer's finding, verbatim:

> RedstoneExoframes.java:53-58 sets the defaults (or energy.cellCapacity's default) on any whole-file fallback of ConfigFile.java:74-91 (syntax error, 0.5 or >int in an int field, unknown or lowercase enum key, duplicated map key), on an out-of-range or null energy section, or silently when the cellCapacity line is missing. At world load FrameRegistryFormat.java:110/126/147 reads every worn and placed record with RedstoneExoframes.config(), and FrameStateCodec.java:151-154 gives each cell cfg.energy.cellCapacity and min(capacity, energy), with no log line; FrameRegistryFormat.java:353 then saves the clamped in-memory state at the first save after any change (any tick of a worn suit marks the registry dirty), and _old follows at the next save. A server that raised cellCapacity (e.g. 50 000) loses every charge above 10 000 for good; fixing the file brings nothing back. This is silent loss of saved data, against the anti-goal and against §12's statement that 3c loses nothing but the charger limit; no test covers it. It reopens the rulings on [T8-F1] and [T8-R1-B1] only on this point: they priced a whole-file fallback as 'loses that start's settings' and did not consider that Task 20's load-time clamp (§12.10) makes one start at the defaults a permanent rewrite of the registry. Fix (design decision): (a) at load never lower a stored charge: keep the written capacity key (still written for compatibility) for a cell whose charge exceeds the config capacity, one WARN naming the frame, clamp only on swap/mount; or (b) when ConfigFile.Result had problems (whole-file fallback or a cellCapacity problem) read cells as readTrusted does for that session; or (c) at minimum a WARN per clamped cell (frame id, RE removed) plus a sentence in README 'Configurazione' and compat.md. Add a FrameStateCodecTest case for the chosen rule.

**What to do (coordinator's ruling):** option (a) of the finding, exactly this rule. When the registry file is read on the server, the capacity of a cell becomes the config's capacity, as now, ONLY IF the cell's stored charge fits in it. A cell whose stored charge is greater than the config capacity keeps the capacity written in the file and its charge is not cut (if the written capacity is missing, not positive or smaller than the charge, the capacity becomes the charge itself). Nothing else changes: the clamp when a cell is mounted, swapped or put in a charger stays as it is, and so does `readTrusted`. Log: one WARN per registry load, not per cell, only when at least one cell is in that case: `<N> cell(s) in the frame registry hold more than the configured capacity (<cfg> RE): they keep their own capacity until they are swapped or drained`, written where the load already logs. Tests, JUnit only (no new in-world test, so that no new line appears in the GameTest logs): in FrameStateCodecTest, a cell with 40 000 of 50 000 read with a config capacity of 10 000 comes back 40 000 of 50 000; a cell with 4 000 of 50 000 comes back 4 000 of 10 000; a cell with 40 000 and no valid written capacity comes back 40 000 of 40 000; in RegistryFileTest or FrameStateSavedDataTest, the round trip that used to lose the charge (read with capacity 10 000, save, read again: still 40 000). Adjust the Task 20 tests whose expected values relied on the cut at load, and say in the fix report which ones. If the WARN can appear in a GameTest run, it must not: check the four logs. Documents: design section 12.10, paragraph 'Una sola capacita'' (state the exception and its reason: a start with a broken config file must not destroy stored energy); README 'Configurazione' (one sentence); docs/compat.md where the load rule is described; the Javadoc of the codec method.


## 2. Documents for the owner's PC: the old checklists

### [FC-2] (Important) The 3a and 3b PC checklists now contradict the merged behaviour in six steps

From tasks: T9, T13, T15, T16, T19, T20, T21, T24. Reviewer's finding, verbatim:

> docs/test-in-gioco/m3b-entrare-e-uscire.md:25 says a second cell gives 'Quello slot è già occupato' (since Task 21 it swaps, §12.10); :26 and :79 (step 12) quote 'Al telaio serve una cella carica' (since Task 20 the text is 'Il telaio ha bisogno di una cella carica (le celle si caricano nel caricatore)'); :82 (step 15) expects the armor bar gone and no sound on entering (since Task 16 the frame_armor modifier shows the mounted helmet and arm, since Task 9 entering plays PISTON_CONTRACT); :107 (step 30) has 'Non c'è un risultato atteso' for carts, boats and beds (since Tasks 13 and 15: dismount at the first tick and bed refused, both with 'Esci prima dall'esoscheletro'); docs/test-in-gioco/m3a-vedere-il-telaio.md:88 asks to swim with the suit (since Task 19 it never swims). Every entry step also shows the top-left energy line and the keys line in chat, absent from the m3a/m3b reference screenshots. README.md:9 sends the owner to these checklists for the pending PC confirmation of 3a and 3b, and CI jars from main after this merge are 3c-1 jars: intended behaviour would be reported as failures. No single task could see it. Fix: a header note in both checklists like the one the 3b merge added at m3a-vedere-il-telaio.md:5 ('Con i jar di dopo l'unione della parte 3c-1 cambiano ...'), listing these six points and pointing to the coming 3c-2 checklist. 'likely' only because the owner's use of post-merge jars is inferred from README and that precedent.

**What to do (coordinator's ruling):** add a header note to both checklists, docs/test-in-gioco/m3a-vedere-il-telaio.md and docs/test-in-gioco/m3b-entrare-e-uscire.md, in the style of the note the 3b merge put at m3a-vedere-il-telaio.md:5. The note says that with the jars built after the merge of part 3c-1 these steps give a different result, and lists them one by one with the step number and the new expected result (the six points of the finding: check each against the code and the language files before writing it), plus one line saying that inside the suit the screen now also shows the energy line at the top left and, at the first entry, the line with the keys in chat. It ends by saying that the checklist of part 3c (docs/test-in-gioco/m3c-giocarci.md) comes with part 3c-2. Do not rewrite the steps themselves and do not touch the reference screenshots.


## 3. Texts that later tasks made false (documents, Javadoc, comments: no behaviour changes)

### [FA-1] (Minor) /exoframe debug energy's REFUSED outcome can never happen, and four texts describe read-only behaviour that does not exist

From tasks: T23, T21. Reviewer's finding, verbatim:

> Files: frame/DebugEnergy.java:27-28, 41, 49-50; command/ExoframeCommand.java:96-97, 238-240, 259-261; frame/CellSwap.java:62-63; design docs/superpowers/specs/2026-11-13-m3-telaio-design.md:658 (§12.14). What is wrong: a read-only registry never holds a worn record. FrameRegistryFormat.read returns READ_ONLY before it reads any entry (FrameRegistryFormat.java:104-108), and the without-sections and unreadable paths read nothing either. So FrameStateAccessImpl.get and wornFrameId (FrameStateAccessImpl.java:62-71, 247-251) are empty for every player. DebugEnergy.set therefore returns NOT_WEARING at lines 41-43 and never reaches changeWorn. When a worn record does exist the registry cannot be read-only, so changeWorn cannot return false, and REFUSED is dead code. In a read-only session the operator reads 'You are not wearing an exoframe' in chat, and no 'changes are not applied' ERROR is written. That is the opposite of what design §12.14 and the two Javadocs promise ('non scrive nulla in chat ... resta la riga del log del registro'). SuitDamage.java:56-57 already states the true fact: with a read-only registry wornFrameId is empty. Why it matters: the ruling at line 13 assumed REFUSED was reachable and did not consider that read-only data reads no worn record. No player is hurt, since the not_wearing answer is accurate in effect. The harm is in the seam documents and the design: they describe a branch that no test can reach. Fix: no code change is needed. In the three Javadocs and in design §12.14, say that a read-only registry has no worn frame this session, so 'energy <re>' answers not_wearing and CellSwap does nothing. Keep REFUSED as a defensive branch or drop it.

**What to do (coordinator's ruling):** no code change: keep REFUSED as a defensive branch. In the three Javadocs the finding names and in design section 12.14, say what is true: with a read-only registry no frame is worn in that session, so `/exoframe debug energy <re>` answers 'not wearing' and the cell swap does nothing. Add a line to the ledger-free documents only; the earlier ruling that assumed REFUSED could be reached is superseded by this one.

### [FA-3] (Minor) Leftover documentation about mutate, changeWorn and the sync rule

From tasks: T14, T17, T21. Reviewer's finding, verbatim:

> Three texts are out of date. (1) docs/compat.md:68 says 'il chiamante di oggi e' frame/SuitTick'. Since Task 17, SuitDamage.wear also calls mutate (SuitDamage.java:157). The same sentence says the tick syncs '... solo quando un consumo ha tolto almeno 1 RE', which leaves out the wear path through SuitTick.markDirty. SuitTick.java:74-80 describes the rule correctly. (2) compat/impl/FrameRegistry.java:172-173 calls mutateWorn the high-frequency write 'l'energia a ogni tick, in una parte successiva', but that part is this one. (3) compat/FrameStateAccess.java:62-63: get's Javadoc lists the write methods as update, mutate and clear, and leaves out changeWorn. Why it matters: these are the seam documents that part 3c-2 (the charger, which writes energy) will read when it decides how to write the registry. Nothing breaks at run time. Fix: three wording edits. The files belong to areas B and C, but the leftovers come from area A's tasks 14 and 21, together with Task 17.

**What to do (coordinator's ruling):** the three wording edits the finding lists (they overlap with FB-1: one edit per place).

### [FB-1] (Minor) Javadoc and docs made stale by later tasks: who calls mutate, when the registry is read-only, what the damage tests share

From tasks: T4, T5, T11, T17, T18. Reviewer's finding, verbatim:

> Four places describe the code as it was before later tasks. (1) FrameRegistry.java:171-173: mutateWorn calls itself the per-tick energy write 'in una parte successiva'. In this part it has two high-frequency callers: SuitTick.java:179 and SuitDamage.wear at SuitDamage.java:157, at every applied hit and every cancelled fall. (2) docs/compat.md:68 says 'il chiamante di oggi e' frame/SuitTick'; SuitDamage is a caller too. (3) The same line says 'Se il dato salvato viene da uno schema piu' recente, le quattro scritture non fanno nulla'; this also holds for a file without sections (Task 5) and an unreadable file. FrameStateAccessImpl.java:40-42 has the same gap ('in sola lettura (schema piu' recente)'). (4) DamageGameTests.java:31-37 (deferred T18-F1, still true): the class Javadoc says every test is a cactus hit on six new Scrap pieces, but the fall, bare-frame, lava, anvil and creative tests are not. No effect on players. A reader of the next part (3c-2 adds the charger and its own writes) would think only the energy tick mutates a worn frame and only a newer schema makes the registry read-only. Fix: reword the four places, one clause each.

**What to do (coordinator's ruling):** reword the four places, one clause each; where FA-3 names the same line, one edit covers both.

### [FC-3] (Minor) README 'Configurazione' and 'Comandi' say less, or other, than the code

From tasks: T8, T23, T24. Reviewer's finding, verbatim:

> README.md:33 'i valori sbagliati tornano ai predefiniti uno per uno' is false for [0,1] fields (clamped, ConfigValidation.java:312), multipliers below 1 (clamped to 1.0, :326) and shares (normalized, :180): an admin writing "lowThreshold": 10 for 10 % gets 1.0, i.e. a suit that is LOW and cannot sprint unless the cell is full, not the promised default. README.md:33 also omits two whole-file cases: an unknown or lowercase map key ("HELMT", "scrap") and a key written twice inside a map (ConfigFile.java:88-91; ruled T8-R1-B1). README.md:31 'Comandi' does not list /exoframe debug energy [RE] (Task 23), on which tools/client/README.md:90 relies. Fix: three sentences (plus the FC-1 sentence if option (c) is chosen).

**What to do (coordinator's ruling):** the three sentences the finding asks for in README.md (what happens to a value outside its limits, field by field kind; the two whole-file cases; the `/exoframe debug energy [RE]` line under 'Comandi'), plus the sentence about cell charges from FC-1.

### [FC-4] (Minor) compat.md and test-kit comments still imply a creative mock player; one stale direction

From tasks: T1, T17, T24. Reviewer's finding, verbatim:

> docs/compat.md:86 'Giocatore finto ... sempre creativa come quello vanilla' (same words in GameTestsImpl.java:76 and :146), and compat.md:85 justifies survivalPlayer by hits and falls not counting 'in creativa': the Process ruling after Task 17 established that only isCreative() is forced and the abilities follow the world's mode, not creative on 1.20.1-forge. GameTestKit.java:104-105 tells test authors to use survivalPlayer for survival but not the ruling's other half (set creative yourself), so a future test counting on a creative mock passes on three variants and fails on 1.20.1-forge only. docs/compat.md:292 still gives 'decidere, insieme al caricamento della config' as the direction for the module restore, though the config is loaded since Task 8 ([T24-F1]). Fix: one clause in each place ('abilities follow the game mode; tests set it themselves') and drop the stale phrase.

**What to do (coordinator's ruling):** one clause in each place the finding names: the mock player's abilities follow the game mode of the test world, only isCreative() is forced, and a test that needs creative or survival sets it itself (the Process ruling in the ledger). This also covers [T24-F1]: drop the stale phrase at docs/compat.md:292.

### [FC-6] (Minor) Design §12.15 and §12.16 no longer say what the code does

From tasks: T24, T15, T16, T21, T22, T23. Reviewer's finding, verbatim:

> §12.15 'Client vero nel cloud' says the recognition scripts (tools/client/ricognizione-3c/prova.sh, prova_b.sh in the recognition patch) are brought into tools/client/ by the first task that uses them; none was: Task 24 wrote tools/client/m3c_shots.sh, which keeps pool, Space, bottom walk, depth-1 water, bank exit, current, step and sprint checks (0.85 factor) but drops prova_b's bubble column, sprint on the bottom, air control and lava checks; §12.6 says bubble columns still lift the suit and nothing re-checks it now (lava is covered by GameTest lavaKeepsVanillaMovement). §12.16's GameTests helper list lacks respawn(player, keepEverything) (plan, code and compat.md have it) and its class list lacks frame/SuitRestrictions, frame/EntryHints, frame/DebugEnergy, client/EnergyLine. Everything else in §12 that 3c-1 builds matches the code (§12.3-§12.10, §12.13 as amended by T22, §12.14, §12.15 probe and in-world list); §12.4's 'una riga di log una volta sola' is once per name in ClientRules.java:90 (same intent). Fix: one sentence in §12.15 naming m3c_shots.sh and what it leaves to the PC checklist; the five names in §12.16.

**What to do (coordinator's ruling):** the sentence in design section 12.15 that names tools/client/m3c_shots.sh, what it checks and what it leaves to the PC checklist of part 3c-2 (bubble columns, sprint on the bottom, air control), and the five names missing from section 12.16. This also covers [T22-R1-B2]: design section 12.13 must describe the energy line as it is now (two lines at the top left, the worst piece on its own line coloured by its condition, no '·' join).

### [T16-F1] (Minor) FrameAttributesImpl class Javadoc says the opposite of the idempotence rule

compat/impl/FrameAttributesImpl.java:24-26 still states the reverse of the rule the code (lines 94-106) and design section 12.5 follow. **What to do:** one sentence, so that the Javadoc says what the code does.

### [T24-F1] (Minor) Stale direction in docs/compat.md:292

Covered by FC-4 above; give it its own line in the fix report.

### [T22-R1-B2] (Minor) Design section 12.13 still describes the one-line format

Covered by FC-6 above; give it its own line in the fix report.

## 4. Tools: the real-client script checks its own premises

### [FC-5] (Minor) m3c_shots.sh does not check two of its own premises (moved wrongly, default config)

From tasks: T2, T8, T24. Reviewer's finding, verbatim:

> tools/client/m3c_shots.sh:41 and tools/client/README.md:90 leave 'moved wrongly' (Review Focus 6: client and server agreeing in water) to a manual grep -c; the script exits 0 when the server logs it, and vanilla's line does not name the mod, so check_log.py never sees it: a later regression of the water rule passes the only automated real-client run. tools/client/client_shot.sh:180-184 recreate world and server.properties but keep run-shot-server/run-shot-client config/redstone_exoframes.json, while every m3c threshold (999 RE under 10 %, the 0.85 speed factor, 2 s swap) assumes the defaults; likewise README.md:29 offers ./gradlew :<variante>:runGameTestServer as an equivalent of run_gametests.sh, but cleanGameTestWorld (build.gradle.kts:118-121) deletes only the world, so that run keeps an edited test config (configFileExists then fails, at least clearly). Fix: in m3c_shots.sh fail when grep -c 'moved wrongly' "$out/server.log" > 0; delete config/redstone_exoframes.json in both run folders at the start of client_shot.sh; add the test config file to cleanGameTestWorld.

**What to do (coordinator's ruling):** the three small edits of the finding: (i) tools/client/m3c_shots.sh fails (non-zero exit, one ERRORE line) when the server log of the run contains 'moved wrongly', and tools/client/README.md says so; (ii) tools/client/client_shot.sh deletes config/redstone_exoframes.json in its two run folders at the start, like the world, and its README says so; (iii) the test config file is added to what `cleanGameTestWorld` deletes in build.gradle.kts. Then run `EXO_ACCEPT_EULA=true bash tools/client/m3c_shots.sh 1.21.1-fabric <a folder under this workspace>` once and check that it still ends with exit 0, an empty log-check.txt and no ERRORE line. The README.md:29 sentence about runGameTestServer becomes true with (iii); check its wording.


## Decided by the coordinator: NOT part of this wave (do not touch)

- [FA-2] No real client has shown the client_rules packet being received with a server config different from the defaults. It becomes the first verification of part 3c-2 (real client on the four variants with a changed server config) and a line of its PC checklist.
- Every deferred minor of the ledger that the three reviewers marked 'leave' or 'obsolete' (their lists are in the three reports), among them [T15-F2], [T19-F2], [T22-R1-B1] and the test gaps [T9-F1], [T12-F2], [T13-F1], [T14-F1], [T16-F2], [T20-F1], [T21-F1], [T21-F2], [T22-F3], [T23-F1..F4].
- Every line the reviewers listed under 'Declined to judge': each is a behaviour the design or an earlier part decided.
- The plan file (docs/superpowers/plans/2026-11-15-m3c1-consumare.md) is never edited.
