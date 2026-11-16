# Task 14 - Il tick dell'energia: effetti, transizioni, sincronizzazione

Stato: DONE. BASE 7a54fe9033bd3d74880333d73cf6c0577641a738, HEAD ae43516dc8cdded98fee8fd747688867ede21688.

Commit: `ae43516 frame: effetti, avvisi dell'energia e sincronizzazione del tick`

## Che cosa ho fatto

- `frame/SuitTick.java`
  - costanti `ENERGY_LOW`, `ENERGY_EMPTY` (pubbliche);
  - campi nuovi del record: `applied`, `sentEffects`, `sentEnergy`, `sentConditions`;
  - passi 6-8 in coda al tick, dopo il passo 5, come nel corpo del brief (valutazione, `apply` solo se gli effetti
    cambiano, transizioni confrontate con `applied`, corsa spenta senza scatto, sincronizzazione con la regola di
    §12.4);
  - `private static Map<PieceSlot, PieceCondition> conditions(FrameState)` (EnumMap slot -> `piece.condition()`);
  - `private static void afterSync(ServerPlayer)`, registrato in `register()` con
    `Compat.frameState().onSynced(SuitTick::afterSync)`: solo con record; con stato indossato scrive
    `sentEffects`/`sentEnergy`/`sentConditions`; sempre `lastSyncTick = r.lastNow` e `dirty = false`; dentro una
    cattura di `RuntimeException` con la stessa riga ERROR `Exoframe tick failed for {}` una volta per giocatore;
  - `static void markDirty(ServerPlayer)` package-private;
  - Javadoc della classe completato con "Effetti e transizioni" e "Sincronizzazione" (regola di §12.4), Javadoc di
    `register`, `tick`, campo `dirty`.
- `gametest/EnergyGameTests.java`: sette test nuovi in coda a `generate()` (vedi sotto).
- `lang/en_us.json`, `lang/it_it.json`: `energy_low`, `energy_empty` prima di `leave_frame_first`, testi del brief.
- `docs/compat.md:68`: frase su `frame/SuitTick` dopo "(al massimo 4-5 volte al secondo)".

## Test

Test in mondo nuovi: `firstEvaluationOfALowCellTellsWithoutSound`, `lowTransitionTellsSoundsAndSyncsOnce`,
`emptyTransitionTellsAndSounds`, `lowAfterAChangeWornStillSpeaks`, `idleTickWithoutAWholeReDoesNotSync`,
`walkingSyncsEveryFiveTicks`, `clockGoingBackwardsRestartsTheLimits`, con le attese del brief.

### TDD Evidence

RED 1 - `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh 1.21.1-neoforge` con i soli test:
```
EnergyGameTests.java:281: error: cannot find symbol
            int low = messages(p, SuitTick.ENERGY_LOW);
  symbol:   variable ENERGY_LOW
FAIL 1.21.1-neoforge: Gradle e' uscito con 1
```
Atteso: le costanti non esistevano.

RED 2 - stesso comando, aggiunte solo le due costanti:
```
redstone_exoframes:first_evaluation_of_a_low_cell_tells_without_sound failed ... 0 energy-low messages, expected 1
redstone_exoframes:low_transition_tells_sounds_and_syncs_once failed ... 0 energy-low messages, expected 1
redstone_exoframes:empty_transition_tells_and_sounds failed ... 0 energy-low messages at the first evaluation, expected 1
redstone_exoframes:low_after_a_change_worn_still_speaks failed ... 0 energy-low messages, expected 1
redstone_exoframes:idle_tick_without_a_whole_re_does_not_sync failed ... 0 syncs after the first whole RE, expected 1
redstone_exoframes:walking_syncs_every_five_ticks failed ... syncs at ticks [], expected [5, 10, 15, 20]
redstone_exoframes:clock_going_backwards_restarts_the_limits failed ... 0 syncs after the clock went back, expected 1
7 required tests failed :(
```
Atteso: nessun messaggio, nessun suono, nessuna sincronizzazione dal tick.

GREEN - stesso comando dopo l'implementazione:
```
All 134 required tests passed :)
OK 1.21.1-neoforge: 134 test obbligatori passati, nessun avviso fuori elenco
```

### Verifiche di fine task

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`
  -> `BUILD SUCCESSFUL in 23s` (nessun `error:` ne' `warning:` nel log)
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `grep -n 'getTickCount' .../frame/SuitTick.java | grep -vE '^[0-9]+:[[:space:]]*(\*|//|/\*)'` -> una riga:
  `112:                tick(serverPlayer, serverPlayer.server.getTickCount());` (l'ascoltatore di `register()`)
- `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh` (quattro varianti, exit 0):
```
OK 1.20.1-forge: 134 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 134 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 134 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 134 test obbligatori passati, nessun avviso fuori elenco
```
  127 + 7 = 134; nessun ripiego dei Task 11-12 nel conteggio (quello del piano, 134, coincide). I test del Task 13
  restano verdi e invariati.

## File cambiati

- `src/main/java/com/toxineren/exoframes/frame/SuitTick.java`
- `src/main/java/com/toxineren/exoframes/gametest/EnergyGameTests.java`
- `src/main/resources/assets/redstone_exoframes/lang/en_us.json`
- `src/main/resources/assets/redstone_exoframes/lang/it_it.json`
- `docs/compat.md`

## Scelte

- La riga ERROR e il suo segno stanno in un metodo privato `logFailure(player, e)` usato dalla cattura di `tick` e da
  quella di `afterSync`: il brief vuole per `afterSync` "la stessa cattura di `tick`", e un solo punto evita due copie
  della riga. Comportamento invariato (stesso insieme `FAILURES_LOGGED`, una riga per giocatore e sessione).
- Nel passo 8 la chiamata e' `frames.syncTo(player)` invece di `Compat.frameState().syncTo(player)`: `frames` e'
  la variabile locale del tick, gia' uguale a `Compat.frameState()`.
- Nei test conto le sincronizzazioni in variabili locali chiamate `count` (non `now`, che e' l'aiuto di
  `GameTestKit`). In `lowTransitionTellsSoundsAndSyncsOnce` ho aggiunto un'asserzione che a 1 000 RE la preparazione
  non abbia dato il messaggio di `LOW` (il brief lo dice tra parentesi: "0,10: NORMAL, nessun messaggio").
- In `walkingSyncsEveryFiveTicks` la base del conteggio si aggiorna a ogni sincronizzazione vista, cosi' `at` elenca i
  tick con almeno una sincronizzazione ("se `syncCount(p)` e' cresciuto").
- Javadoc di `markDirty`: "la sincronizzazione parte al primo tick ad almeno 5 tick dall'ultima" (piu' preciso di un
  "al piu' tardi").

## Scostamenti dal brief

- `docs/compat.md`: la frase e' inserita subito dopo la parentesi "(al massimo 4-5 volte al secondo)", come dice il
  brief; il ";" che seguiva resta e ora separa la frase nuova dalla descrizione di `changeWorn`. Nessun altro
  scostamento.

## Self-review

- Le transizioni confrontano con `applied`, mai con i `sent*` (provato da `lowAfterAChangeWornStillSpeaks`).
- `afterSync` usa `r.lastNow`, mai `getTickCount()` (provato da `clockGoingBackwardsRestartsTheLimits`, dove il
  `now` del test e' lontano da quello del server).
- `afterSync` gira anche nell'uscita (record ancora presente se `WornState.changed` lo scarta dopo `syncTo`): senza
  stato indossato aggiorna solo `lastSyncTick` e `dirty`, come il brief.
- Un'eccezione di `apply` lascia `applied` com'era: il tick dopo riprova (comportamento della cattura).
- Righe sotto i 120 caratteri; Javadoc e commenti in italiano con apostrofi ASCII; identificatori e log in inglese.

## Problemi o dubbi

Nessuno.
