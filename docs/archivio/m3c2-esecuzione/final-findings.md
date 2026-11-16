# Final fix wave — part 3c-2 "Ricaricare"

Two reviewers read the whole range 614b25f..9107aae: no Critical, one Important (FB-1), seven Minor. Their full reports: final-review-A.md and final-review-B.md in this folder (each finding has its section there, with file and line). Work in this order. Items 1-3 change only the checklist; item 6 is the only change to the mod's production code.

## To fix

### 1. FB-1 (Important) — docs/test-in-gioco/m3c-giocarci.md, sections 16-20: a step needs items that an earlier step destroyed or used up
The reviewer found: step 98 asks for the spare charger that step 82 already placed and broke by hand (nothing drops); after step 98 the steps 99 and 100 have no charger; step 96 says to remove the two hoppers without naming a tool (broken by hand they drop nothing, and step 97 needs three); the 64 redstone dust given at the opening of section 17 do not last to step 99.
Fix, text only:
- Walk through sections 16-20 from the first step to the last and keep an inventory table: what the tester holds after each step (chargers, hoppers, cells, redstone dust, pickaxe, comparator and anything else a later step uses). Put the table in the fix report.
- Wherever a step needs something the tester no longer has, add the exact command that gives it (for example `/give @s redstone_exoframes:charger`, `/give @s minecraft:redstone 64`) to that step or to the opening of its section, or name the tool that keeps the item ("con il piccone"). Prefer giving the item again to changing what a step tests.
- Do not renumber any step and do not change what a step checks. The tester follows the text to the letter and cannot ask.

### 2. FB-4 (Minor) — same file, opening of section 17 (line 318)
The sentence that says the burning dust "finisce di passare la sua energia entro 10 secondi anche se nel frattempo cambi la cella" is true only while a cell that is not full is inside. Say instead that the energy of the burning dust stays in the charger until there is a cell to charge, and then passes in 10 seconds at most (so the front can light up with no dust inside).

### 3. FA-1 (Minor; ruled: no code in this part) — same file
A right button held for four ticks or more repeats the click every 4 ticks (0.2 s), and each charger action leaves the hand in the state that triggers the opposite action (cell in, cell out, cell in, ...). The coordinator ruled: no code change now, the owner decides (see the ledger). In the checklist:
- at the opening of section 17 add one sentence: on the charger click briefly; while the right button stays down the action repeats every 0.2 s (the base game does the same with a flower pot);
- add ONE new step at the end of section 17, with the next free number after the last step of the checklist (the ledger's numbering rule): hold the right button for about one second with a cell in hand on an empty charger; expected: the cell goes in and out several times with its sounds, and at the end there is exactly one cell, in the hand or in the charger, never lost and never doubled; the step says that this was read in the code and never seen in the client, and asks to report what happens if it goes differently;
- add one question for the owner in the questions section: holding the right button on a charger repeats the action every 0.2 s, so a long click puts the cell in and takes it out again; should the charger ignore the repeats while the button stays down?
- update the summary table and any count of steps or questions the file states.

### 4. FB-2 and FA-3 (Minor; closes the deferred minor T7-F5) — three comments this part made false
- src/main/java/com/toxineren/exoframes/content/ModItems.java:24-25 says the Scrap pieces have no recipe and come from the creative tab: they have recipes since Task 7.
- build.gradle.kts:179-181 says what the per-version resource folder carries: it now also carries the data files of its version.
- tools/check_art.sh:51 lists the placeholder textures in a comment without the block textures of the charger.
Rewrite the three sentences so that they say what is true at HEAD. Comments only.

### 5. FB-3 (Minor) — the client sequences that presuppose the default config inherit EXO_SHOT_SERVER_CONFIG
tools/client/m3a_shots.sh, m3b_shots.sh, m3c_shots.sh and m3c2_shots.sh call client_shot.sh with thresholds that hold only for the default config. Add `unset EXO_SHOT_SERVER_CONFIG` near the top of each of the four, with a one-line comment that says why. m3c_rules_shots.sh sets the variable itself and stays as it is. Check with `bash -n` on the four files and by reading; no real-client run is needed for this item.

### 6. FA-4 (Minor; closes the deferred minor T6-F2) — the 64-dust limit is not in canPlaceItem
src/main/java/com/toxineren/exoframes/content/ChargerBlockEntity.java, canPlaceItem: the dust slot accepts any redstone and relies on the stack's own maximum. Make it `stack.is(Items.REDSTONE) && dust.getCount() < MAX_DUST` (use the names the class already has). Add the assertions to the existing test facesAcceptOnlyWhatTheyShould (no new `add(tests,` line): with 64 dust in the slot canPlaceItem for the dust slot is false and the side face refuses; with 63 it accepts. If docs/compat.md or design 12.11 state the rule of the dust slot, add the clause there.

### 7. FA-2 (Minor; closes the deferred minor T4-F3) — two assertions that protect saved charger content
No new `add(tests,` line: both go into existing tests of ChargerGameTests.
- (a) In chargerSavesAndLoadsItsContents assert that the saved tag holds exactly the five keys written as literal strings in the test: "has_cell", "cell_energy", "redstone", "reserve", "carry_milli" (not through the NbtKeys constants: the point is that renaming a constant must fail a test).
- (b) Assert once that a change of content marks the chunk for saving: clear the chunk's unsaved flag, call setCell (and, separately, setDust), and read the flag again. The reviewer says LevelChunk.setUnsaved(false) and isUnsaved() have the same signature on both versions; verify it. gametest/ takes no Stonecutter directive: if the two versions differ, go through compat/GameTests as the other helpers do, or, if that grows beyond a few lines, leave (b) out and list FA-2 under notFixed with the reason.

### 8. T10-F3 (Minor, deferred) — docs/compat.md:114
The CompatItem entry points to "la checklist per il PC della parte 3c-2" without file or step. Name the file docs/test-in-gioco/m3c-giocarci.md and the steps (the reviewer says 59-60; check them in the checklist).

## Closing checks
Item 6 changes production code and items 6-7 change tests: run the full closing checks of the plan header on HEAD after the last commit, plus `bash tools/check_data.sh` -> `OK data` and `bash tools/check_art.sh` -> `OK art`. The number of in-world tests stays 208 on each of the four variants.

## NOT to fix in this wave (do not touch)
- FA-1 as code: no interval, no debounce, no change to the click.
- Every other deferred minor of the ledger (the reviewers' verdict is 'leave'): T1-F1, T1-F2, T1-F3, T2-F1, T3-F1, T3-F2, T4-F1, T4-F2, T5-F1, T5-F2, T5-F3, T6-F1, T6-F3, T6-F4, T7-F1, T7-F2, T7-F3, T7-F4, T8-F1, T8-F2, T8-F3, T9-F1, T9-F2, T9-F3, T9-F4, T10-F2. T2-F2 and T10-F1 are obsolete.
- The reviewers' 'Declined to judge' items (other mods' pipes, the rulings on the cut of the charge, the lost custom name and one dust per tick, the declared limits of the design).
