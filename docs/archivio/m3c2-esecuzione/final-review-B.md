# Final review, area B — branch m3c-giocarci (614b25f..9107aae)

Area B: the game from the first recipe to the recharged cell, the tools and the documents (tasks 1, 9, 10).
Read-only review; nothing was built or run (Gradle, game, test scripts and real client are forbidden to this seat).
Done in one pass over the production diff, the tests diff and the checklist diff, then a second pass that followed
the checklist step by step into the code (ChargerBlock, ChargerBlockEntity, ChargerRule, FrameInteractions,
FrameEntry.stepOut, CellItem, PieceItem, Handouts, the language files, the blockstate and the models).

## Strengths

- The checklist's new steps are accurate against the code. Every number I recomputed matches: 28 iron / 10 copper /
  2 redstone blocks / 2 dust / 1 quartz for the seven items (step 77) and 34 / 14 / 2 / 4 / 2 for the full game
  (step 101, 20 dust left at step 102); 20 s for 2 dust, 200 s for 20 dust, percent 0-9 then "no redstone"
  (steps 91-92, `ChargerRule.tick` and `percent`); comparator 0 / 7 / 15 (`ChargerRule.comparator`, step 93);
  break times 2.65 s with a wooden pickaxe and 17.5 s by hand for hardness 3.5 (step 82); 9 or 8 dust left after a
  break (step 98); 15 % after 30 s (step 99); 100 s and 80 s for 10 and 8 dust (step 103); 200 s of sprinting from
  5000 to 1000 RE (step 105). The twelve charger texts of steps 80-92 are the ones in `en_us.json` and `it_it.json`,
  letter for letter, the Italian `%s %%` included.
- The Italian texts of the base game quoted by the new steps ("Banco da fabbricazione", "Cornice riempita",
  "Cornice svuotata", "Nuove ricette sbloccate!", "Cerca...", "Pacchetti di dati", "Altro",
  "(Creato per una versione di Minecraft più vecchia)", "Salva ed esci dal mondo") are the ones of the vanilla
  `it_it.json` in Loom's asset cache (same file for 1.20.1 and 1.21.1). The task reviewer had left these unverified.
- Steps 110 and 111 (new steps about 3c-1 code) agree with `FrameEntry.stepOut`: order front, left, right, behind;
  a carpet and a slab are climbed (`MAX_STEP_UP` 0.5), a full block is not; campfire and magma are in `hazard()`.
- `SurvivalGameTests` really is one game with no debug shortcut: the ingredients leave the inventory one by one and
  the grid goes through the server's recipe manager; charger placed, fed and emptied with the game's own
  `ServerPlayerGameMode.useItemOn`; the frame armed and entered with `interactAt`; a real `hurt`; the final count
  covers every inventory slot, the charger's slots, reserve and carry, the placed frame and the ground. The step name
  at the head of the failure message is a real help.
- `m3c2_shots.sh` checks with server commands, not by eye: `recipe take` for the unlock, `execute if data block`
  for the NBT, two energy readings for the charge, `execute if entity` for the drops. The geometry in its header
  (pitch 55 hits the grass at z 1.63, pitch 60 hits the north face) is right. The three reference shots I opened
  show what the checklist says (lit front with three red marks and "Charger: charging 30%"; empty front toward the
  player; the cell bar at a quarter). The six new PNGs carry only IHDR/IDAT/IEND.
- The design corrections of Task 10 match both the code and the rulings: the cut only on the manual click
  (`ChargerBlock.insertCell` through `CellItem.toCell`), never in `setCell`/`setItem`/`readData`; one dust per tick;
  the fresh stack on entry; `onClick` with only `useItemOn` overridden on 1.21.1; the swap sounding as an insert.
- `docs/compat.md`: the three new bases, the five test helpers, the NBT table and the hopper faces are all there and
  every test name it cites exists (27 charger tests, 5 data tests; 208 `add(tests,` lines in all, as README and
  checklist say). The blockstate has all 16 variants and every texture the three models name exists.
- The client branch of the click returns `SUCCESS` only for what the server will handle and changes nothing, so no
  ghost item and no ghost block; the server reads the hand from its own state.

## Issues

### Critical (Must Fix)

None.

### Important (Should Fix)

**FB-1 — Checklist sections 17-19: the items a step needs were destroyed by an earlier step** (T10)

- `docs/test-in-gioco/m3c-giocarci.md:348` (step 98): "Poi rifallo a mano vuota con il caricatore di scorta". The
  spare charger is the one given at line 318, and step 82 (line 322) already has the assistant place it and break it
  by hand, where "non cade nulla". At step 98 there is no spare. If the assistant uses the only charger left, the
  crafted one, the hand break destroys it; the third part of step 98 then breaks a creative-tab charger in creative,
  which drops no block either. Steps 99 and 100 (lines 349-350) say "Metti nel caricatore..." with no charger left
  and no instruction to get one.
- Line 341 (step 96): "togli le due tramogge", with no tool named, in a section that allows survival. A hopper
  broken by hand drops nothing (vanilla `requiresCorrectToolForDrops`, checked with javap on the 1.20.1 jar), and
  line 337 gives exactly three hoppers, all of which step 97 needs.
- Redstone dust: section 17 gives 64 (line 318) and nothing is given again before section 20. Steps 84-99 burn or
  bury about 69 if nothing is recovered (2 + 1 + 2 + 20 + 10 + 2 + 8 + about 4 + 20), plus 15 lying in the
  comparator line of step 93; step 99 alone wants 20. Likely short at step 99.
- Why it matters: the checklist is followed to the letter by an assistant that cannot ask. These are steps that
  cannot be done as written; the likely outcome is a skipped step 98-100 (the break by hand with a cell inside and
  the save while charging are two of the checks that matter for "nothing is lost") or an improvised `/give`.
- Fix (text only): a charger broken by hand cannot be kept, so give a new one where it is needed: add
  `/give @s redstone_exoframes:charger` to step 98 before the hand break, and again before step 99 (or say there
  which charger to place); say "con il piccone" in step 96 for the hoppers (or give three more at step 97); add
  `/give @s minecraft:redstone 64` to the opening of section 19.

### Minor (Nice to Have)

**FB-2 — Stale comments left by the recipes** (T7, T10)

- `src/main/java/com/toxineren/exoframes/content/ModItems.java:24-25`: "Nessuna ricetta in questa parte: si prendono
  dalla scheda creativa", above the four Scrap pieces. False since Task 7.
- `build.gradle.kts:179-181`: "Oggi porta le texture trasparenti del layer armatura vanilla di 1.20.1": the folder
  now carries the 32 data files too. This is ledger minor T7-F5, still open at HEAD.
- Effect: none for a player; the next part reads these to learn where things come from.

**FB-3 — The sequences that presuppose the default config inherit `EXO_SHOT_SERVER_CONFIG`** (T1)

- `tools/client/client_shot.sh:159` honours the variable from the environment. `m3a_shots.sh`, `m3b_shots.sh`,
  `m3c_shots.sh` and `m3c2_shots.sh` call it without clearing the variable, and their thresholds (999 RE under 10 %,
  the 2 s of the cell swap, "charging 30%", 50 RE per second) are the defaults'. A variable left exported in the
  session gives a false FAIL, or a reference shot with other numbers, with no hint of the cause.
- Fix: `unset EXO_SHOT_SERVER_CONFIG` at the top of the four sequences (only `m3c_rules_shots.sh` sets it).

**FB-4 — Section 17 opening: the burning dust does not always finish "entro 10 secondi"** (T10)

- `docs/test-in-gioco/m3c-giocarci.md:318`: "finisce di passare la sua energia entro 10 secondi anche se nel
  frattempo cambi la cella". True only while a cell that is not full is inside. With no cell the tick does not call
  the rule, and with a full cell `ChargerRule.tick` returns the reserve unchanged ("la riserva resta per la cella
  dopo", `ChargerRule.java`, first branch): the reserve waits, minutes if need be, and the next cell lights the front
  with no dust inside. No numbered step states a wrong result because of this (steps 83, 91, 96 start from a newly
  placed charger, and the reserve is 0 after steps 92, 96, 97, 103), but an assistant who sees "in carica" long after
  taking the dust out has only this sentence to go by.
- Fix: "resta nel caricatore finché non c'è una cella da caricare, poi passa in al più 10 secondi".

## Deferred minors of tasks 1, 9, 10

| Id | Verdict | Reason |
|---|---|---|
| T1-F1 | leave | Only the label of the final FAIL line after an exit 2; the exit code is right. The two new scripts already clear the file. |
| T1-F2 | leave | Needs the variable to name the server's own run-folder file; the script stops with an ERRORE line and exit 2, nothing is started. |
| T1-F3 | leave | Neither language file has a positional placeholder today (searched for `%1$`); the test is right for the texts that exist. |
| T9-F1 | leave | Eleven lines of test helper copied once; no behaviour. |
| T9-F2 | leave | The third copy is what makes the survival test independent of the table it checks. |
| T9-F3 | leave | The stack trace of the origin is kept; only nested causes are dropped, and only in a failing test. |
| T9-F4 | leave | A new `ItemStack` of the frame item equals the crafted one (the recipe result carries no data); the method Javadoc at lines 236-238 says so. One sentence of the class Javadoc is loose. |
| T10-F1 | obsolete | The ledger ruling provides for a new step in an old section (number after the last, end of the section) and design §12.15 asks for the real-latency check; step 112 follows both. |
| T10-F2 | leave | The checklist's own introduction (line 5) names the change to step 1 and says what to redo; the pointer sends there. |
| T10-F3 | leave | One vague pointer in `docs/compat.md:114`; the steps are 59-60 of `m3c-giocarci.md`. Worth one edit if the fix wave touches the file. |

## Behaviours of this area that no automated test would catch

- The client branch of the click on the four variants and the real tooltip of the charger item: proved once by
  `m3c2_shots.sh` and left to checklist step 80; neither runs in CI. A regression there shows as a ghost block or an
  arm swing, not as lost items, because the client changes nothing.
- The crafting table itself (result slot, shift-click, recipe book fill): `SurvivalGameTests` and `DataGameTests`
  ask the recipe manager, not the menu. Vanilla code; checklist steps 76-77.
- None of these can lose or duplicate an item or damage saved data.

## Recommendations

- When FB-1 is fixed, walk sections 16-19 once more with a tally of what the player holds (chargers, hoppers, dust,
  cells) after each step; the three gaps found are all of that kind.
- `SurvivalGameTests` leaves its 6x6 stone floor in the structure; harmless, but the other charger tests remove
  their floor.

## Declined to judge

- Build, the 208 world tests, JUnit and the three real-client scripts: not run, by rule; their results are the
  reports'.
- Vanilla client behaviour that the new steps state and the repository cannot answer: the shield raised when
  sneaking with it in the off hand (step 88; the server-side rule I did confirm in the 1.20.1 jar), the piglin and
  the powder snow of step 109, whether the mod's data pack shows in red on 1.21.1 (step 108), the recipe-book tab
  icons (step 76).
- The order in which Forge and NeoForge hoppers look for a capability and for the vanilla container
  (`docs/compat.md`, "Tramogge"): not re-read in the patched jars; the result is the same either way.
- A cell above the configured capacity loses the excess when it is put back in a charger by hand: ruled in the
  ledger, not re-opened.
- In Italian the mod's frame and the vanilla loom are both "Telaio" (step 76 searches `telaio`): the name is from
  part 3b; in the checklist's flow the loom recipe is still locked, so the step holds.
- First names in the checklist header: already in the 3c-1 file; the plan's personal-data rule is about the jars.
- Area A's internals (strength of the hopper tests, `gen_data.py`, `check_jars.sh` f3): read only where the
  checklist or a document led there.
- Three of the six new reference shots were checked for size and PNG chunks only, not opened.

## Assessment

**Ready to merge? With fixes.**

**Reasoning:** code, tests, tools and design text of this area agree with each other and with the rulings, and I
found nothing that loses or duplicates an item. The one thing to fix before the merge is the checklist's item
bookkeeping in sections 17-19 (FB-1), a text-only change; the three minors can ride the same wave.
