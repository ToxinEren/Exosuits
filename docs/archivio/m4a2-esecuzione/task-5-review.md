### Spec Compliance
- ✅ Spec compliant. Every requirement of the brief and of rulings A8-A13 is met.
- Checks run:
  - `grep -c '^- \[ \] \*\*[0-9]*\.\*\*'` gives 29, with steps 1..29 in order.
  - One step 1, at `m4a-stazione.md:105`.
  - All 16 named shots exist: 10 in `m4a-screenshot/`, 6 in `m4a1-screenshot/`.
  - No "Scala dell'interfaccia", no "pulsante di prova"/`TEST_BUTTON` in `docs/compat.md`, no "235 test" in the README.
  - The 4a-1 note opens exactly "Con i jar di dopo l'unione della parte 4a-2" (`m4a1-banco.md:17`).
  - No plan, ruling or model IDs, no personal names, no email in the checklist.
  - The counts add up: `StationActionGameTests` lists 16 names, `StationMenuGameTests` 2+2+3+4+5+1=17, and the new JUnit 6+4+1+6+1=18 (389+18=407).
- Rulings:
  - A8, step 21 (`:185`): `/clear`, `/item replace ... armor.head`, enter, `/kill`, respawn, `/tp`, the row and its tooltip, then Shift plus empty-hand right-click. It says what to note when the row or the helmet is missing. This matches `FrameInteractions.unmount` (`:176-198`), where stowed armour is taken first.
  - A9, step 19 (`:183`): one player. The live update is at step 23 (`:193`). "Già verificato" says `vista` and `rinforzato` prove it (`:68`).
  - A10: reach sub-steps in step 9 (`:132-137`), before the frame goes on top.
  - A11: step 23 opens the LAN with "Comandi: No" and gives the reason.
  - A12:
    - Esc after the click, with a note, at steps 16 and 19.
    - Step 17 says the message need not be read.
    - The preparation paragraph (`:101`), the report list (`:210`) and the "Cosa c'è da vedere" bullet all cover it.
    - The owner question is at `:226`, worded as a known limit.
  - A13: the report states 2 (one entry plus its comment line).
- Stale lines:
  - README "Stato" (old test-button sentence removed, 4a-2 sentence added), 256 tests, `m4a_shots.sh` "otto controlli", "Documenti" and the plans list.
  - `tools/client/README.md` pointers (`m4a1-banco.md` replaced by `m4a-stazione.md`). `tools/README.md` was already correct from Task 1 (`m4a_shots` count is 10, no live `m4a1_shots`).
  - `docs/compat.md` `GameTests` "Condizione" places the directive in the private `mockPlayer` shared by `mockServerPlayer` and `operatorPlayer`.
- Facts checked against `it_it.json`/`en_us.json` and code, all correct:
  - Chat and action-bar texts: "Rinforzato: %s", "Telaio rimosso; il contenuto è a terra", "Hai piazzato un telaio di rottame", "Questo telaio non è tuo", "Questo telaio porta pezzi o una cella: si posa solo vuoto", "Quello slot è già occupato", the two lock messages, "Il telaio non è più disponibile alla stazione", "La stazione non è riuscita…".
  - Tooltip and row texts: "Custodisce un'armatura" with "Si ritira a mano, accovacciati", "Smonta (va nell'inventario)", "Trascina per girare", "Su questo server tutti possono usare ogni telaio", "Solo il proprietario può cambiarlo", "Vuota: caricala in un caricatore", the item-tooltip lines.
  - The lock button writes the same keys as the command, on the action bar (`StationActions.java:121`).
  - Item ids `exoframe`, `scrap_helmet`, `redstone_cell`, `station`.
- ⚠️ Cannot verify (no client, no PC):
  - The pixel measures in the checks table (`:59-66`) come from Task 4 and were not recomputed.
  - The LAN claim that the host stays operator with "Comandi: No" rests on the implementer's javap reading.
  - "Rete locale...", "Comandi: No", and "Scala" labels were not re-checked in the game's own language files.
  - The "Custodisce un'armatura" row has never been drawn in a real client; step 21 is its first look.

### Strengths
- Every step gives the exact command, key or label, with the English in brackets, and the expected sight. Where the result is open it says what to note: steps 9, 14, 21, 23, 25, 26.
- The world state is tracked consistently across steps (frame locked or unlocked, reinforced or not, game mode per step, hotbar contents), and step 99 says which steps run on which variants.
- Step 28 collects the log before the restart because the restart starts a new `latest.log`. The `.prima` copy and the hash check make the config restore verifiable.
- The cloud-versus-PC split is honest: what the cloud proves, what it cannot, and why `vista` and `rinforzato` cannot be done alone on the PC.
- Extras are small and in the spirit of decision 23: the step 5 facing and step 4 walk-around from the 4d notes, the step 17 "Tutti" release, the step 22 second pick-up, the step 28 config log check.

### Issues
#### Critical (Must Fix)
None.

#### Important (Should Fix)
None.

#### Minor (Nice to Have)
- `m4a-stazione.md:182` (step 18): "subito l'elmo si raccoglie da solo" after `/clear` of the cobblestone. A dropped item has a pickup delay of roughly a second, and the assistant may judge "subito" too strictly. Suggest "entro un paio di secondi".
- `m4a-stazione.md:99`: the passo 24/25/26 scope is "una volta su 1.20.1 e una volta su 1.21.1", but 26 and 25 depend on the frame left by earlier steps on that same instance. This holds if the assistant keeps the world, which the text implies, but a one-line "nello stesso mondo" would remove doubt.
- `m4a-stazione.md:139`: the final tooltip sentence in step 9 is very long and carries many actions; it is followable but dense. This matches the 4a-1 form, so no change is required.
- `README.md` "Stato" is a single multi-thousand-character paragraph. It is the existing style, and the new 4a-2 text is accurate (selection, mount/unmount, lock, shield, picked-up frame keeps flags and owner, repair and paint left to 4b, checklist `m4a-stazione.md`).

### Assessment
**Task quality:** Approved
**Reasoning:** The 29 steps are complete, ordered and follow rulings A8-A13. The labels and message texts I checked match the language files and code, and the mechanical greps pass. The README, compat.md, 4a-1 note and tools/client/README pointers are aligned. Only cosmetic polish remains.
