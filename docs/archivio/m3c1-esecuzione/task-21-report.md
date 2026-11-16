# Task 21 — Cambio cella: rapporto

**Stato:** DONE
**BASE:** 3abc47d3a552b545109fb0adc9d0918710c3eec0
**HEAD:** a102066ee1502ed2c6834be75be9faac3ccc5aac
**Commit:** `a102066 frame: cambio cella tenendo R e scambio su un telaio piazzato`

## Che cosa ho fatto

- `core/energy/CellSwapRule` (Java puro): `SLOTS = 37`, `OFFHAND = 36`, `pick(installedStored, slotEnergies)`
  con il confronto stretto in un solo giro da 0 a 36; lunghezza diversa da 37 → `IllegalArgumentException`
  ("slotEnergies must have 37 entries: N"), `null` → `NullPointerException`.
- `frame/CellSwapPacket`: record vuoto, `ID = Ids.of("cell_swap")`, `CODEC` copiato da `FrameExitPacket`.
- `frame/CellSwap`: `NO_BETTER_CELL`, `registerNet()` (C2S → `handle(player, player.server.getTickCount())`),
  `handle(ServerPlayer, long now)` nei sei passi del brief (rifiuti silenziosi; intervallo
  `cellSwapSeconds * 10L` con `now < last` accettato; `CellSwapRule.pick` su `items` 0-35 e `offhand` 0 con la carica
  tagliata da `toCell`; `changeWorn` prima, inventario dopo e solo se riuscito: `split(1)`, la vecchia nello stesso
  slot se vuoto, altrimenti `Handouts.give`, `setChanged`; suono `CELL_SWAP`; `LAST_SWAP.put`). Mappa
  `ConcurrentHashMap` mai svuotata. Javadoc: ordine atomico, perche' mai `update`, intervallo a meta' dell'attesa,
  deroga a §5.6.
- `frame/EntryHints`: `KEYS_HINT`, `EXIT_KEY`, `CELL_SWAP_KEY`, insieme `SHOWN`, `register()` (svuota a
  `PLAYER_QUIT`), `onEntered` (riga in chat con due `Component.keybind`).
- `FrameEntry.enter`: `EntryHints.onEntered(player)` subito prima di `return true` (dopo `WornState.changed` e il
  suono dell'ingresso); Javadoc aggiornato. `DebugWear` non lo chiama (confermato nel client vero: nessuna riga in
  chat dopo `debug wear`).
- `FrameInteractions.insertCell`: niente piu' `SLOT_TAKEN` per la cella; `updatePlaced` con vecchia fuori e nuova
  dentro; falso → `CONSUME`; vero → `shrink(1)` fuori dalla creativa, la vecchia a `Handouts.give` (anche in
  creativa), `refreshSummary`, `MOUNT`. Javadoc della classe, di `onInteract`, del metodo e di `SLOT_TAKEN` aggiornati.
- `client/ExitKey.keyDown()`; `client/CellSwapKey` (tasto R, categoria della mod, `ExitHold` proprio,
  `canCount` con `!ExitKey.keyDown()`, `ClientRules.cellSwapSeconds()`, messaggio `cell_swap_hold`).
- Registrazioni: `CellSwap.registerNet()` subito dopo `ClientRules.register()` e `EntryHints.register()` accanto, con un
  commento ciascuno; `CellSwapKey.register()` dopo `ExitKey.register()`.
- Testi: le quattro chiavi dopo `exit_hold` e prima di `energy_low`, nei due file, stesso ordine (65 chiavi,
  stesso elenco).
- `docs/compat.md`: canale `cell_swap` in "Uso" di `Net`, `CellSwapKey.register()` in "Avvio del client",
  `CellSwapKey` accanto a `ExitKey`/`ExitHold` nelle cuciture client, riga dei pacchetti C2S della tabella API.
- Test: `CellSwapRuleTest` (6 casi) e 9 test in mondo in `CellGameTests`; coda di
  `ExoframeGameTests.mountPieceAndCellByHand` riscritta (seconda cella scambiata, la vecchia da 4 000 torna) con il
  Javadoc del brief.

## TDD

- **RED (JUnit):** `./gradlew --console=plain :1.21.1-neoforge:test --tests '*CellSwapRuleTest'` → `compileTestJava
  FAILED`, `CellSwapRuleTest.java:13: error: cannot find symbol` (e righe seguenti): atteso, `CellSwapRule` mancava.
- **RED (in mondo):** `bash tools/run_gametests.sh 1.21.1-neoforge` → `CellGameTests.java:19/21/...: error: cannot find
  symbol`, `FAIL 1.21.1-neoforge: Gradle e' uscito con 1`: atteso, `CellSwap` ed `EntryHints` mancavano.
- **GREEN (JUnit):** stesso comando → `BUILD SUCCESSFUL`; `TEST-...CellSwapRuleTest.xml`: `tests="6" skipped="0"
  failures="0" errors="0"`.
- **GREEN (in mondo, una variante):** `bash tools/run_gametests.sh 1.21.1-neoforge` → `OK 1.21.1-neoforge: 168 test
  obbligatori passati, nessun avviso fuori elenco` (al primo giro).

## Verifiche di fine task (ultime righe)

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
  :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` →
  `BUILD SUCCESSFUL in 25s` (nessun `warning:` ne' `error:` nel log).
- `bash tools/check_sources.sh` → `OK sources` (rieseguito anche dopo l'ultimo ritocco di un Javadoc: `OK sources`).
- `bash tools/check_jars.sh` → `OK jars`.
- `grep -n 'frameState().update(' src/main/java/com/toxineren/exoframes/frame/CellSwap.java` → nessuna riga (uscita 1).
- `bash tools/run_gametests.sh`:
  - `OK 1.20.1-forge: 168 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.20.1-fabric: 168 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-fabric: 168 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-neoforge: 168 test obbligatori passati, nessun avviso fuori elenco`
  168 = 159 + 9: nessun ripiego dei Task 11-12 e' stato preso. `mountPieceAndCellByHand` e i test di ingresso della 3b
  sono fra i 168 passati.
- Dopo le verifiche e prima del commit ho solo riflesso le righe di un Javadoc in `FrameInteractions.onInteract`
  (nessun codice): ricompilato `:1.21.1-neoforge:compileJava` (uscita 0) e `OK sources`; i GameTest non sono stati
  ripetuti per un cambio di soli commenti. Il ritocco e' nel commit.

## Client vero (Step 7)

`EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <variante> <cartella> "gamemode survival Exo" "execute as Exo
run exoframe debug wear rottame" "@attendi 2" "@tieni r 3 cambio-cella-tenuto 1" "@attendi 0.5" "@scatto
nessuna-cella-migliore"`, una variante alla volta:

- `1.20.1-forge`: uscita 0, `fatto in 83 s`, `log-check.txt` vuoto (0 byte), nessuna riga `registered after` nei log
  del client. `cambio-cella-tenuto.png`: "Hold to swap the cell… 52%"; `nessuna-cella-migliore.png`: "No cell in your
  inventory holds more energy" (la cella indossata e' piena: il pacchetto e' arrivato, il server ha risposto).
- `1.21.1-fabric`: uscita 0, `fatto in 67 s`, `log-check.txt` vuoto, nessuna riga `registered after`; gli stessi due
  scatti con gli stessi testi (52 %).

Scatti e log in `<scratchpad>/shot-120forge/` e
`.../shot-121fabric/` (fuori dal repository, non committati). Nessun server, client o Gradle lasciato acceso.

## File cambiati

Nuovi: `src/main/java/com/toxineren/exoframes/core/energy/CellSwapRule.java`, `frame/CellSwap.java`,
`frame/CellSwapPacket.java`, `frame/EntryHints.java`, `client/CellSwapKey.java`,
`src/test/java/com/toxineren/exoframes/core/energy/CellSwapRuleTest.java`.
Modificati: `RedstoneExoframes.java`, `client/ExitKey.java`, `client/RedstoneExoframesClient.java`,
`frame/FrameEntry.java`, `frame/FrameInteractions.java`, `gametest/CellGameTests.java`,
`gametest/ExoframeGameTests.java` (solo la coda e il Javadoc di `mountPieceAndCellByHand`), `lang/en_us.json`,
`lang/it_it.json`, `docs/compat.md`.

## Scelte

- `CellSwapRule.pick`: la soglia di partenza e' `Math.max(0, installedStored)`. Per `-1` (nessuna cella) e per ogni
  valore >= 0 e' esattamente la regola del brief (`e > 0` senza cella, `e > installedStored` altrimenti); un valore
  negativo diverso da -1, che non dovrebbe arrivare, non fa prendere una cella vuota.
- `CellSwapRuleTest`: l'array di partenza (`new int[CellSwapRule.SLOTS]` riempito di `-1`) sta in un aiuto privato
  `noCells()` invece di ripetere le due righe in ogni caso; i casi e i valori sono quelli del brief.
- `EntryHints.EXIT_KEY` e `CELL_SWAP_KEY` sono package-private (li usa solo `frame/`); `KEYS_HINT` pubblico come dice il
  brief.
- `EntryHints.onEntered` sta dopo il suono dell'ingresso, subito prima di `return true`: soddisfa "dopo
  `WornState.changed`, prima di `return true`" e lascia il suono legato all'ingresso come prima.
- In `FrameInteractions.insertCell` l'ordine finale e' `shrink`, `Handouts.give` della vecchia, `refreshSummary`,
  `MOUNT` (come il codice di prima: riassunto poi suono).
- `CellGameTests`: aiuti privati della classe `cell(n)`, `energy(stack)`, `cellsAndRe(p)` (come chiede il brief) piu'
  `cellItem()`, `placedCharge(server, id)` e `keybind(arg)` per leggibilita'; `cellsAndRe` somma `getCount()` (le celle
  stanno a uno per stack, ma cosi' un duplicato nello stesso stack si vedrebbe). `cellSwapTakesTheFullestBetterCell`
  controlla anche la premessa `{4, 23000}`. Nel test della morte tolgo dal mondo gli oggetti caduti vicino al
  giocatore (`deathDrops`), come gli altri test di morte. Il Javadoc della classe dice ora che ci sono anche i test del
  cambio cella e della riga dei tasti.
- `cellSwapTakesTheFullestBetterCell`, `cellSwapWithoutABetterCellSaysSo`: il registratore dei suoni avvolge solo la
  chiamata a `handle` (il suono dell'ingresso di `wearScrap` resta fuori).

## Scostamenti dal brief

- `placedFrameSwapsCells`, parte in creativa: dopo `mockServerPlayer` aggiungo `creative.setGameMode(GameType.CREATIVE)`
  prima della premessa `instabuild`. Motivo: la regola del coordinatore nel ledger (Process Ruling dopo il Task 18: il
  giocatore finto non nasce in creativa su 1.20.1-forge, ogni test che vuole la creativa la imposta da se'). La premessa
  resta nel test.
- `keysHintOncePerSession`: la sequenza segue il brief (ingresso → 1; uscita e rientro → ancora 1; `PLAYER_QUIT`;
  uscita e rientro → 2). Nessuno scostamento, solo l'uso di `Compat.gameTests().messagesTo` per leggere gli argomenti.

## Autorevisione

- Review Focus 1: nessun ramo di `handle` tocca l'inventario prima che `changeWorn` abbia restituito vero; `update` non
  compare (grep vuoto); i rifiuti (senza telaio, dopo l'uscita, dopo la morte, menu aperto, intervallo) non creano record,
  non spostano celle, non dicono nulla, e il rifiuto con il menu non segna l'intervallo (provato). Celle e RE contati
  prima e dopo uguali.
- Regola 5 di `check_sources.sh`: `FrameStateAccess frames = Compat.frameState();` e' una riga di codice di
  `frame/CellSwap.java`, il file che registra il C2S. Regola 7: `EntryHints` non nomina `client/`.
- Nessun `isCreative()` nel codice nuovo (`instabuild` in `FrameInteractions`, gia' cosi').
- Righe entro 120 colonne nei file nuovi e nelle parti toccate; apostrofi ASCII nei commenti.

## Problemi e dubbi

Nessuno aperto. Da notare per chi coordina (gia' deciso nel ledger): il corpo del test 3b `mountPieceAndCellByHand`
cambia perche' design §12.10 fa della seconda cella uno scambio.
