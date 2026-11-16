# Task 13 - Il tick dell'energia: attivita', salti, consumo

Stato: DONE. BASE 2397378ffde413541996c9912f615c1fcb9db163, HEAD 7a54fe9033bd3d74880333d73cf6c0577641a738.
Commit: `7a54fe9 frame: tick dell'energia (attivita', salti, creativa, cavalcature)`.

## Che cosa ho fatto

- `core/energy/ActivityRule` (Java puro): `WINDOW = 4`, `MOVING = 0.01`, `TELEPORT = 8.0`; `next(distance, sprinting,
  passenger)` e `reset()`. Passeggero o distanza strettamente sopra `TELEPORT`: `reset()` e `IDLE`; altrimenti il
  campione entra in una finestra circolare di 4 (corrente compreso), in movimento se almeno un campione e' strettamente
  sopra `MOVING`; `SPRINT`/`WALK`/`IDLE`. Javadoc: perche' la finestra (0-2 pacchetti per tick), solo orizzontale
  (scala a pioli = fermo, accettato), costanti fuori dalla config.
- `frame/SuitTick`: `LEAVE_FRAME_FIRST`, `SYNC_INTERVAL = 5`, `RECORDS` (`MapMaker().weakKeys()`), `FAILURES_LOGGED`
  (`ConcurrentHashMap.newKeySet()`), `register()` (`PLAYER_PRE` solo `ServerPlayer` con `server.getTickCount()`;
  `PLAYER_QUIT` toglie l'UUID da `FAILURES_LOGGED`), `tick(ServerPlayer, long)` con il corpo del brief trascritto passo
  per passo dentro il `try/catch (RuntimeException)` con la riga `Exoframe tick failed for {}` una volta per UUID,
  `forget`, `hasRecord`, record privato `Record` con i campi del brief (`meter`, `activity`, `lastPos`, `lastDimension`,
  `jumpBase`, `lastNow`, `lastSyncTick = now - SYNC_INTERVAL`, `dirty`). Javadoc della classe con i sei punti del brief.
- `frame/WornState.changed`: in coda, dopo `FrameLog.event`, se `wornFrameId(player)` e' vuoto `SuitTick.forget` e
  `Compat.attributes().clear`; Javadoc della classe e del metodo aggiornati.
- `RedstoneExoframes.init()`: `SuitTick.register()` subito dopo `SlotGuard.register()` con il commento richiesto;
  `Compat.gameTests().register(EnergyGameTests.class)` subito dopo la riga di `CarryOverGameTests`.
- Lingue: `message.redstone_exoframes.leave_frame_first` prima di `frame_taken` in `en_us.json` ("Leave the exoframe
  first") e `it_it.json` ("Esci prima dall'esoscheletro"). `docs/compat.md` invariato (nessuna cucitura nuova).
- `gametest/EnergyGameTests`: gli 11 test del brief, nell'ordine del brief, con gli aiuti `prepared` e `walk` come dati.
- `ActivityRuleTest`: i 7 casi del brief.

## File

- Nuovi: `src/main/java/com/toxineren/exoframes/core/energy/ActivityRule.java`,
  `src/main/java/com/toxineren/exoframes/frame/SuitTick.java`,
  `src/main/java/com/toxineren/exoframes/gametest/EnergyGameTests.java`,
  `src/test/java/com/toxineren/exoframes/core/energy/ActivityRuleTest.java`
- Modificati: `src/main/java/com/toxineren/exoframes/frame/WornState.java`,
  `src/main/java/com/toxineren/exoframes/RedstoneExoframes.java`,
  `src/main/resources/assets/redstone_exoframes/lang/en_us.json`, `.../lang/it_it.json`

## TDD

- RED JUnit (Step 2): `./gradlew --console=plain :1.21.1-neoforge:test --tests '*ActivityRuleTest'` ->
  `Task :1.21.1-neoforge:compileTestJava FAILED`, `ActivityRuleTest.java:15: error: cannot find symbol` (e righe
  seguenti): `ActivityRule` non esisteva ancora, atteso.
- RED GameTest (Step 4): `bash tools/run_gametests.sh 1.21.1-neoforge` -> `Task :1.21.1-neoforge:compileJava FAILED`,
  `EnergyGameTests.java:6: error: cannot find symbol` (import di `SuitTick`), `FAIL 1.21.1-neoforge: Gradle e' uscito
  con 1`: atteso, `SuitTick` mancava.
- GREEN JUnit (Step 6): stesso comando -> `BUILD SUCCESSFUL`; `TEST-...ActivityRuleTest.xml`: `tests="7" skipped="0"
  failures="0" errors="0"`.
- GREEN GameTest (Step 6, una variante): `OK 1.21.1-neoforge: 127 test obbligatori passati, nessun avviso fuori
  elenco`, al primo tentativo.

## Verifiche di fine task (Step 7)

- Build e collectJar delle quattro varianti (comando esatto dei Global Constraints): `BUILD SUCCESSFUL in 26s`, nessun
  `warning:` ne' `error:` nel log.
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `bash tools/run_gametests.sh` (quattro varianti):
  - `OK 1.20.1-forge: 127 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.20.1-fabric: 127 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-fabric: 127 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-neoforge: 127 test obbligatori passati, nessun avviso fuori elenco`
  127 = 116 + 11 (nessun ripiego preso nei Task 11-12: oggi 116 righe `add(tests,` prima di questo task). Tutti i test
  3b di uscita, morte e `debugWear*` verdi. `grep -c 'Exoframe tick failed'` nei quattro `server.log`: 0.
- `grep -rn 'isCreative()' .../SuitTick.java .../ActivityRule.java | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)'` ->
  nessuna riga (exit 1).
- Dopo queste verifiche ho solo riavvolto tre righe di Javadoc oltre 120 caratteri (solo commenti, in `SuitTick`,
  `ActivityRule`, `WornState`); ho rifatto build e collectJar delle quattro varianti (`BUILD SUCCESSFUL in 21s`),
  `OK sources` e `OK jars`; i GameTest non li ho rifatti, perche' il codice compilato non cambia.
- Nessun server, client o build lasciato in esecuzione (resta solo il demone di Gradle, inattivo).

## Scelte

- `ActivityRule`: finestra come array di 4 con indice e conteggio; `reset()` azzera indice e conteggio (i valori vecchi
  oltre il conteggio non si guardano piu'). Il piu' semplice che rispetta "finestra circolare, corrente compreso".
- Il record `Record` e' `private static final class` dentro `SuitTick`, con campi package-private (nessun getter): i
  Task 14 e 19 vi aggiungono campi letti dallo stesso file. Il brief dice "final class Record" privato.
- `SYNC_INTERVAL`, `lastSyncTick` e `dirty` entrano ora come il brief prescrive, anche se li legge solo il Task 14.
- Messaggi dei test: in inglese, con il valore trovato e quello atteso; per `dimensionChangeRestartsTheWindow` il
  messaggio a 4 998 e' esattamente quello del brief ("4 998: the window was not restarted at the dimension change").
- In `tickListenerCreatesTheRecord` ho aggiunto un controllo `!hasRecord` prima dell'evento, come chiede il brief.
- Javadoc in italiano su ogni test, con il motivo del numero atteso (stile di `CarryOverGameTests`).

## Scostamenti dal brief

Nessuno nel comportamento o nelle firme. Il corpo di `tick` e' quello del brief, con i commenti numerati riscritti come
commenti di riga in italiano accanto al codice.

## Autorevisione

- Ordine dei passi 1-5 di §12.3 come nel brief: `isPassenger()` letto prima di `stopRiding()`, posizione letta dopo la
  discesa, base dei salti riletta a ogni tick prima del ramo creativa/spettatore, `dirty` solo con `mutate` vero e
  almeno 1 RE tolto.
- `now` entra solo dall'ascoltatore o dai test; nessun `getTickCount()` nel record; `now < lastNow` fa ripartire
  finestra e `lastSyncTick`.
- Nessun `isCreative()`; nessun `new FrameRules` (usa `RedstoneExoframes.rules()`).
- `WornState.changed` all'ingresso non fa nulla di nuovo (il registro ha gia' il telaio indossato).
- Nessuna preoccupazione aperta.
