# Task 4 — Caricatore: contenuto, tick, stato del blocco, salvataggio

Stato: DONE. BASE `12011825618a536db16f406d26f7383a4ebf2058`, HEAD `2b7c6963bd49464e54ec3203f5ee8dbc5416f450`.
Un commit: `2b7c696 content: il caricatore tiene cella e polvere, carica a ogni tick, si salva`.
All'inizio HEAD era BASE e l'albero di lavoro era pulito: nessun tentativo precedente da riprendere.

## Che cosa ho implementato

**`content/ChargerBlockEntity`** (firme di "Nomi, numeri e testi comuni"):

- campi `cell` e `dust` (due veri `ItemStack`, `ItemStack.EMPTY` se vuoti), `reserve`, `carryMilli`; `cell()` e
  `dust()` restituiscono quegli stack, mai una copia; `MAX_DUST = 64`.
- `setCell(stack)`: vuoto -> slot vuoto; altrimenti un solo `CellItem`
  (`IllegalArgumentException("not a single redstone cell: " + stack)`), e lo slot prende uno stack nuovo di
  `ModItems.REDSTONE_CELL` con la sola carica, non tagliata; lo stack passato non si tocca; poi `setChanged()`.
- `setDust(stack)`: vuoto -> slot vuoto; altrimenti `Items.REDSTONE` con al piu' `MAX_DUST` oggetti
  (`IllegalArgumentException`), e lo slot tiene proprio lo stack passato; poi `setChanged()`.
- `readData(tag)`: scrive i campi direttamente (niente `setCell`, niente `setChanged()`); `reserve` in
  [0, `MAX_RE_PER_REDSTONE`], `carry_milli` in [0, 999], polveri in [0, 64] (vuoto se 0), cella presente se `has_cell`
  con carica `max(0, cell_energy)`, mai tagliata alla capacita'. Nessuna eccezione per chiavi mancanti o di tipo
  sbagliato.
- `writeData(tag)`: sempre le cinque chiavi (`has_cell`, `cell_energy`, `redstone`, `reserve`, `carry_milli`).
- `status()`, `chargePercent()`, `comparatorOutput()`: `ChargerRule` con la capacita' della config.
- `serverTick(level, pos, state, charger)`: tutto dentro un `try` che cattura `RuntimeException` (prima volta per
  `GlobalPos`, in un `Set` statico concorrente: `LOGGER.error("Charger tick failed at {} in {}", pos,
  level.dimension().location(), e)`; poi silenzio). Senza cella `charging = false`; con la cella `ChargerRule.tick`
  con i tre valori della config; si applica solo cio' che e' cambiato (`setCellEnergy` sullo stack dello slot,
  `dust.shrink(1)` con lo slot a `ItemStack.EMPTY` se resta vuoto, `reserve`, `carryMilli`), `setChanged()` se
  qualcosa e' cambiato; poi `cell`/`lit` e `level.setBlock(..., 3)` solo se lo stato del blocco differisce.
- Javadoc della classe: il modello "come una fornace", perche' due veri `ItemStack`, perche' ne' il caricamento ne'
  `setCell` tagliano la carica, chi scrive lo stato del blocco, il limite dichiarato di design §7 punto 27.

**`content/ChargerBlock`**: `getTicker` (`null` sul client e per un altro tipo; sul server
`(l, p, s, be) -> ChargerBlockEntity.serverTick(l, p, s, (ChargerBlockEntity) be)`), `public boolean
hasAnalogOutputSignal(BlockState)` -> vero, `public int getAnalogOutputSignal(BlockState, Level, BlockPos)` ->
`comparatorOutput()` della block entity, 0 se manca. Javadoc della classe aggiornato (non e' piu' "vuoto").

**`gametest/ChargerGameTests`**: sette test in coda al generatore (181 -> 188) e gli aiuti privati `tick`,
`removeCharger`, `redstone`, piu' tre asserzioni comuni (vedi "Scelte").

**`docs/compat.md`**: "Schema NBT", paragrafo "Caricatore (block entity `redstone_exoframes:charger`)" con le cinque
chiavi, tipo, limiti in lettura e la regola "la block entity non taglia mai la carica alla capacita' della config: ne'
al caricamento, ne' in `setCell`"; "Limiti noti e rinvii", voce "Garanzia del caricatore — limite dichiarato (design §7
punto 27, §8 domanda 10)". La riga di `carry_milli` dello stato del telaio e' rimasta com'e'.

## Prove

### TDD

- **RED** — `bash tools/run_gametests.sh 1.21.1-neoforge` con i soli test scritti:
  `> Task :1.21.1-neoforge:compileJava FAILED`, 53 errori `cannot find symbol` in `ChargerGameTests.java`, tutti sui
  metodi che mancavano (`setCell(ItemStack)`, `setDust(ItemStack)`, `cell()`, `dust()`, `carryMilli()`, `status()`,
  `comparatorOutput()`, `serverTick(ServerLevel,BlockPos,BlockState,ChargerBlockEntity)`), poi
  `FAIL 1.21.1-neoforge: Gradle e' uscito con 1`. Atteso: e' il fallimento che il brief prevede allo Step 2.
- **GREEN** — dopo l'implementazione, stesso comando:
  `OK 1.21.1-neoforge: 188 test obbligatori passati, nessun avviso fuori elenco`; e
  `bash tools/run_gametests.sh 1.20.1-fabric`:
  `OK 1.20.1-fabric: 188 test obbligatori passati, nessun avviso fuori elenco`. Passati alla prima esecuzione.
- **Controllo per mutazione** (dopo il commit, una sola variante, poi file ripristinato da HEAD): con
  `readData` che taglia `cell_energy` alla capacita' della config il test fallisce come deve:
  `redstone_exoframes:charger_load_never_throws_and_never_cuts_a_charge failed ... over the capacity: cell 10000 RE,
  dust 64, reserve 0; expected 40000, 64, 0`, `1 required tests failed`. Ripristinato il file, di nuovo
  `OK 1.21.1-neoforge: 188 test obbligatori passati, nessun avviso fuori elenco`.

### Verifiche di fine task (sulle quattro varianti, prima del commit)

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
  :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`:

  ```
  BUILD SUCCESSFUL in 30s
  52 actionable tasks: 34 executed, 18 up-to-date
  ```

  Nessuna riga `warning:` o `error:` nel log.
- `bash tools/check_sources.sh`: `OK sources`
- `bash tools/check_jars.sh`: `OK jars`
- `bash tools/run_gametests.sh` (config ai predefiniti, `EXO_ACCEPT_EULA=true`):

  ```
  OK 1.20.1-forge: 188 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 188 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 188 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 188 test obbligatori passati, nessun avviso fuori elenco
  ```

  In ciascun log `All 188 required tests passed`; zero righe `Charger tick failed`.
- `tools/check_art.sh` e `tools/check_data.sh` non girati: il task non tocca ne' la grafica ne' i dati.

### I sette test

| Test | Che cosa prova |
|---|---|
| `chargerChargesTwoAndAHalfRePerTick` | 2 RE al primo tick (19 polveri, riserva 498, riporto 500, `CELL` e `LIT` veri), 5 al secondo, 500 RE e riserva 0 a 200 tick, 502 RE e 18 polveri a 201 |
| `chargerStopsWhenFullOrOutOfRedstone` | 9 999 -> 10 000 con riserva 499 e `LIT` vero; il tick dopo `LIT` falso, `FULL`, riserva 499; cella vuota: 200 tick -> 499 RE e riserva 0; il tick dopo `LIT` falso, `NO_REDSTONE`, carica 499 |
| `litTogglesTwiceWithoutLosingTheCell` | `lit` acceso e spento due volte, `cell` una; dopo ogni tick stessa block entity, non rimossa, nulla a terra |
| `chargerRealTickerChargesInRealTime` | il ticker vero: dopo 40 tick del server carica fra 95 e 105 RE, `LIT` e `CELL` veri, una polvere rimasta |
| `chargerSavesAndLoadsItsContents` | il tag salvato ha le cinque chiavi con i valori attesi, tutte numeriche; la block entity letta ha gli stessi valori e il tick dopo arriva a 1 239 RE |
| `chargerLoadNeverThrowsAndNeverCutsACharge` | (a) 40 000 RE non tagliati, 64 polveri, riserva 0, riporto 999, `FULL`, comparatore 15, un tick non cambia nulla; (b) carica negativa -> 0, `redstone` stringa -> nessuna polvere, riserva 1 000 000; (c) solo `id`, `x`, `y`, `z` -> zeri; `setCell` con 40 000 RE: stack nuovo, carica intera, stack passato intatto |
| `comparatorReadsTheCharge` | `hasAnalogOutputSignal`; 0 senza cella; 666 -> 0, 667 -> 1, 5 000 -> 7, 9 999 -> 14, 10 000 -> 15 |

### Visto per la prima volta (nota del ledger sulle cose mai eseguite)

Il ticker vero di una block entity dentro la struttura di un GameTest gira sulle quattro varianti:
`chargerRealTickerChargesInRealTime` passa ovunque con la finestra 95-105 RE dopo `runAfterDelay(40, ...)`. Il numero
esatto di tick non l'ho letto (il test non lo scrive nel log). Le tramogge vere non sono di questo task.

## File cambiati

- `src/main/java/com/toxineren/exoframes/content/ChargerBlockEntity.java` (riscritto: +218 righe circa)
- `src/main/java/com/toxineren/exoframes/content/ChargerBlock.java` (ticker, comparatore, Javadoc della classe)
- `src/main/java/com/toxineren/exoframes/gametest/ChargerGameTests.java` (sette test, aiuti)
- `docs/compat.md`

## Scelte

1. **Il `Set` dei fallimenti del tick non si svuota mai.** Il brief chiede "un `Set` statico concorrente" per
   `GlobalPos` e nient'altro, e cosi' e'. Su un server dedicato "una volta per posizione e sessione" e "una volta da
   quando il gioco e' partito" sono la stessa cosa; nel gioco in singolo, con due mondi aperti uno dopo l'altro senza
   chiudere il gioco, un caricatore rotto nella stessa posizione e dimensione del secondo mondo non riscriverebbe la
   riga. Svuotarlo alla chiusura del server vorrebbe una registrazione in `RedstoneExoframes.init()`, fuori dai file
   del task. Il Javadoc dice "da quando il gioco e' partito", che e' cio' che il codice fa.
2. **`storedEnergy()` privato**: 0 senza cella, senza chiedere i dati a `ItemStack.EMPTY`; lo usano le tre letture e
   `writeData`. Motivo: un solo posto per "carica o 0".
3. **Messaggio di `setDust`**: `"not redstone dust, or more than 64: " + stack` (il brief dice solo
   `IllegalArgumentException`).
4. **`getTicker` senza annotazione di nullabilita'**: il codice della mod non ne usa nessuna.
5. **`docs/compat.md`, tabella delle chiavi**: la riga `reserve` del Task 2 e' diventata una riga sola per le cinque
   chiavi che rimanda al paragrafo "Caricatore", per non avere due descrizioni della riserva. Nella voce di
   `CompatEntityBlock`, `getAnalogOutputSignal` e `hasAnalogOutputSignal` non sono piu' "piu' avanti" (resta
   `onRemove`).
6. **Aiuti di test in piu'**, privati di `ChargerGameTests`: `assertContents` (carica, polveri, riserva),
   `assertCellAndLit` (le due proprieta' dello stato del blocco), `assertSameEntityNothingDropped`, e la costante
   `CHARGER_NBT_KEYS`. Motivo: le stesse tre righe ripetute una ventina di volte.
7. **Nel caso (c) di `chargerLoadNeverThrowsAndNeverCutsACharge`** il tag e' costruito da zero con le sole quattro
   chiavi `id`, `x`, `y`, `z` copiate dal salvato (su Forge e NeoForge il salvato puo' portare altro).
8. **`tick` dei test** prende la posizione da `c.getBlockPos()` e rilegge lo stato del blocco dal livello a ogni
   chiamata, come fa il ticker del gioco.
9. **Javadoc sul perche' `setCell` non taglia**: scritto come proprieta' del metodo ("non distingue un ingresso da un
   rimettere"), senza nominare `setItem`, che arriva con il Task 6.

## Scostamenti dal brief

Nessuno. I numeri del brief tornano tutti con `ChargerRule` cosi' com'e'.

## Autorevisione

- Completezza: tutte le voci di "Produces" ci sono, con le firme della sezione comune; sette test, 188 per variante.
- `setCell` e `setDust` non scrivono mai lo stato del blocco; lo scrive solo `serverTick`, una volta, e solo se
  differisce (provato da `litTogglesTwiceWithoutLosingTheCell`: la block entity resta la stessa).
- Lettura: nessun ramo lancia; i quattro limiti sono quelli di "NBT del caricatore"; la carica non si taglia
  (provato anche per mutazione).
- Regole dei sorgenti: nessuna direttiva Stonecutter fuori da `compat/impl/`, nessun nome di `compat.impl` o del
  package client in `content/` e `gametest/`, identificatori e messaggi in inglese, Javadoc in italiano con apostrofi
  ASCII, righe entro 120 colonne.
- Nessuna riga WARN o ERROR nuova nei log dei test; `tools/log_allowlist.txt` non toccato.

## Osservazioni (non bloccano)

- Mentre carica, il tick chiama `setChanged()` a ogni tick (cambia la carica o il riporto): il chunk resta da salvare
  e i comparatori vicini vengono riletti a ogni tick. E' cio' che chiedono brief e design ("ogni cambio di contenuto
  chiama `setChanged()`"); una fornace del gioco base per l'avanzamento non lo fa. Lo segnalo solo perche' si sappia.
- `setDust` tiene lo stack passato: un eventuale nome o altro dato di quello stack di polvere resta in memoria fino al
  prossimo caricamento del chunk e non arriva al disco (nell'NBT c'e' solo il numero). Chi decide che cosa entra sono
  il clic (Task 5) e il contenitore (Task 6).
- Vedi "Scelte" 1 per il `Set` dei fallimenti nel gioco in singolo.
