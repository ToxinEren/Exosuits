# Fix wave after the final review of part 4a-1 (base a9cb5cb)

Every item below is decided. Full reasoning and file:line evidence are in final-review.md (same folder); the rulings are in progress.md. Do exactly these, nothing else.

## A. Ruling P22 (final review I-1): the release that ends a rotation drag
- StationScreen.java:205-212: in `mouseReleased`, if the left button ends a rotation drag (`rotating`), set `rotating = false` and return `true` WITHOUT calling `super`; otherwise call `super` as today. Skipping super is safe (final-review.md I-1: vanilla mouseReleased makes no super call; our mouseClicked never passed the press to vanilla).
- Hardening: in `mouseClicked`, a left press outside the preview clip sets `rotating = false`, so a lost release cannot leave rotation armed.
- Update the Javadoc at :205 ("il rilascio va comunque al gioco base" is no longer true) and any doc line that describes the old release (grep docs/compat.md and tools/client/README.md).
- Ruling P28: add a sub-step to checklist step 12 (docs/test-in-gioco/m4a1-banco.md): with a pile on the cursor (taken from the inventory), drag inside the preview and release outside the screen, then again releasing over an inventory slot: the pile stays on the cursor, nothing falls, the suit turns; what to note otherwise. Land it together with the step-12 wording fix (item C5).
- Evidence (not committed): one cloud run on one variant with client_shot.sh steps that put ingots on the cursor (`@clic` on the inventory slot holding them), drag from the preview to a point outside the screen, release, Escape; then show that no item entity exists near the player (for example an rcon `execute if entity @e[type=item,distance=..8]` whose result the run log shows) and that the ingots are back in the inventory. Describe the steps and the result in your report.

## B. Rulings P24 + P27 (final review I-2): the owner's name in current files
- Replace the owner's first name "il proprietario" with "il proprietario" (or a neutral rephrase that reads well) in every current file OUTSIDE docs/superpowers/**, prose and comments only:
  docs/arte/README.md (9 lines); docs/audit/mjolnir-armor-audit.md (:3, :16, :595, :601, :604, :637); src/main/java/com/toxineren/exoframes/client/SuitScale.java:13 (comment); src/test/java/com/toxineren/exoframes/client/SuitScaleTest.java:63 (comment); tools/art/README.md:174; tools/art/designs/frame.py (5 lines), scrap.py (4), _legs.py:1; docs/test-in-gioco/m1-setup.md (:3, :83), m2b-compat.md:3, m3a-vedere-il-telaio.md (5), m3b-entrare-e-uscire.md (4), m3c-giocarci.md (5).
- Also rephrase docs/audit/mjolnir-armor-audit.md:646 ("il Claude Doc") without the vendor name, e.g. "il documento iniziale del proprietario".
- Leave untouched: README.md:11 (a link to a file by its name) and everything under docs/superpowers/**.
- If an occurrence is not prose or a comment (a string that reaches generated art, a test name, an identifier), leave it and list it in the report.
- Check: `git grep -n -w il proprietario -- . ':!docs/superpowers/**'` prints nothing; the art generators' output is unchanged (check_art OK, and if tools/art/README.md says how to regenerate, regenerate and show `git status` clean for the generated files).

## C. The seven deferred minors marked "fix before merge" (final-review.md, "Deferred minors: triage")
1. StationBlock.java:168: drop the "(decisione P13)" tag (the ID lives only in the git-ignored ledger); keep the reasons.
2. StationMenuGameTests.java:180: `p.setGameMode(GameType.SURVIVAL)` before the positive control (Global Constraints: every test sets its own game mode).
3. Checklist Italian labels of the base game (m4a1-banco.md): :133 "Scala dell'interfaccia" → "Scala"; :135 "Sei morto!" → "Sei morto/a!"; :136 "Apri in LAN" → "Rete locale..."; :136 "Avvia il mondo in LAN" → "Apri il mondo in rete locale". Verify each against the vanilla it_it.json of BOTH 1.20.1 and 1.21.1 (from the cached Minecraft jars); if they differ between versions, write both.
4. docs/compat.md:47: limit "nella 4a-1 la vista non cambia nulla che si veda" to what is true (the only visible-change check in 4a-1 is the /exoframe unlock smoke test).
5. Checklist step 12 (:121): say the direction of the drag ("verso destra") and what the suit does.
6. Checklist step 16 (:133): the window range gives an interface of about 320-350 × 240-260, not 320×240: narrow the range, or have the assistant note the `Display:` line of F3 and compare with the expected range.
7. Checklist step 18 (:135): say where to click: "sul fronte, sotto il telaio" (a click on the armed frame enters it and derails the death test).

## Verification (run once, after all edits; one Gradle or client run at a time)
- The four-variant build with collectJar → BUILD SUCCESSFUL; JUnit 358 per variant (the count command in contesto-comune.md).
- bash tools/run_gametests.sh → four `OK <variante>: 235 test obbligatori passati, nessun avviso fuori elenco`.
- bash tools/check_sources.sh, check_jars.sh, check_data.sh, check_art.sh → OK; arm_measure and client_shot selftests → OK.
- StationScreen changed: EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh <v> <folder> on all four variants, one at a time → three ok each, OK, empty log-check.txt.
- Checklist Step 4 greps (at least 20 steps, one step 1, every named shot exists).
- `git grep -n -w il proprietario -- . ':!docs/superpowers/**'` → nothing.
- `git status --short` lists no screenshots outside docs/test-in-gioco/m4a1-screenshot/.

## Commits
At most three, signed by the environment, Italian messages, each ending with the two trailer lines:
Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_018Fcbc2CFSwuiCoDaNxQwgt
Suggested: (1) `client: il rilascio che chiude una rotazione non arriva al gioco base (la pila sul cursore resta); checklist`; (2) `docs: il proprietario al posto del nome nei documenti e nei commenti`; (3) `docs, test: le correzioni minori della revisione finale della 4a-1`. Do not push.
