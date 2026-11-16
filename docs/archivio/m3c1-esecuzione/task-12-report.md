# Task 12 - Valori del server per il client (client_rules): rapporto

Stato: DONE. BASE ccce810a44c45cac072440e0fce3a4113f6cc53c, HEAD 2397378ffde413541996c9912f615c1fcb9db163.
Commit: `2397378 frame: i valori del server che il client conta da se'`.

## Cosa ho fatto

- `frame/ClientRulesPacket.java` (nuovo): `public record ClientRulesPacket(int cellCapacity, int cellSwapSeconds,
  int exitHoldSeconds)`, `ID = Ids.of("client_rules")`, `CODEC` con tre `writeVarInt`/`readVarInt` nell'ordine dei
  campi, nessun filtro; `of(ExoframesConfig)` = `energy.cellCapacity`, `energy.cellSwapSeconds`,
  `frame.exitHoldSeconds`.
- `frame/ClientRules.java` (nuovo, codice comune, nessuna classe del client): `register()` (canale S2C con
  `Compat.net().registerS2C` e invio a `PlayerEvent.PLAYER_JOIN` dei valori di `RedstoneExoframes.config()`),
  `accept` (limiti `[1, MAX_CELL_CAPACITY]` e `[0, MAX_HOLD_SECONDS]`; fuori: predefinito di
  `ExoframesConfig.defaults()` e WARN `Received client rule {} = {} out of range, using {}` una volta per nome, con
  un `Set<String>` da `ConcurrentHashMap.newKeySet()`), `reset()`, i tre lettori (ricevuto, altrimenti config
  locale). Valori in un `private record Received` in un campo `volatile`, `null` = nulla ricevuto. Javadoc con chi lo
  legge, il server che non lo legge mai, il gioco in singolo e il server dedicato/GameTest.
- `RedstoneExoframes.init()`: `ClientRules.register();` subito dopo `FrameEntry.registerNet();`, con commento di
  una riga.
- `client/RedstoneExoframesClient.init()`: `ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player ->
  ClientRules.reset());` subito dopo `Compat.frameState().registerClient();`.
- `client/ExitKey.tick`: `ExitHold.ticksNeeded(ClientRules.exitHoldSeconds())`; Javadoc della classe: i secondi
  sono quelli del server (config locale prima del pacchetto).
- `docs/compat.md`: `Net` "Uso" con il canale `redstone_exoframes:client_rules`; "Cuciture client", riga di
  `ExitKey` con il testo del brief; riga di `RedstoneExoframesClient.init()` con l'azzeramento (vedi Scelte).

## Test

### TDD
- RED: `./gradlew --console=plain :1.21.1-neoforge:test --tests '*ClientRulesTest' --tests '*ClientRulesPacketTest'`
  -> `compileTestJava FAILED`, `ClientRulesTest.java:17: error: cannot find symbol` (e le altre righe che nominano
  `ClientRules`/`ClientRulesPacket`), `package ClientRulesPacket does not exist`. Atteso: le due classi mancavano.
- GREEN: stesso comando -> `BUILD SUCCESSFUL`; risultati: `ClientRulesTest` tests="3" failures="0" errors="0",
  `ClientRulesPacketTest` tests="1" failures="0" errors="0". Nell'output del test le tre WARN, una per nome:
  `Received client rule cellCapacity = 0 out of range, using 10000`, `... cellSwapSeconds = 31 ... using 2`,
  `... exitHoldSeconds = 31 ... using 1` (le chiamate successive fuori limite non ne scrivono altre).
- Step 4 grep: `grep -rn 'ClientRules' src/main/java/com/toxineren/exoframes/frame .../compat .../content` -> solo
  `frame/ClientRules.java` e `frame/ClientRulesPacket.java`.

### Verifiche di fine task (quattro varianti)
- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
  :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` ->
  `BUILD SUCCESSFUL in 26s` (i risultati JUnit di `ClientRulesTest` e `ClientRulesPacketTest` ci sono in tutte e
  quattro le varianti: 8 file).
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh` ->
  ```
  OK 1.20.1-forge: 116 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 116 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 116 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 116 test obbligatori passati, nessun avviso fuori elenco
  ```
  116 come dopo il Task 11: il Task 11 non ha preso il ripiego e qui il codec si prova in JUnit, quindi nessun test
  in mondo in piu'. I giocatori finti ricevono ora `client_rules` all'ingresso, senza righe fuori elenco.
- Nessun server, client o build lasciato acceso (resta solo il demone Gradle inattivo).

## File cambiati
- `src/main/java/com/toxineren/exoframes/frame/ClientRules.java` (nuovo)
- `src/main/java/com/toxineren/exoframes/frame/ClientRulesPacket.java` (nuovo)
- `src/main/java/com/toxineren/exoframes/RedstoneExoframes.java`
- `src/main/java/com/toxineren/exoframes/client/RedstoneExoframesClient.java`
- `src/main/java/com/toxineren/exoframes/client/ExitKey.java`
- `docs/compat.md`
- `src/test/java/com/toxineren/exoframes/frame/ClientRulesTest.java` (nuovo)
- `src/test/java/com/toxineren/exoframes/frame/ClientRulesPacketTest.java` (nuovo)

## Scelte
- `ClientRulesPacketTest` in JUnit e non in mondo: il rapporto del Task 11 dice che `FriendlyByteBuf` e netty si
  caricano in JUnit sulle quattro varianti (nessun ripiego), e qui e' stato cosi' (risultati sulle quattro).
- Nel test, i tre valori si controllano con un aiuto privato `assertRules(capacita', cambio, uscita)` e la config
  locale da 4 000, 4, 2 con un aiuto `local()`: stesse asserzioni del brief, scritte una volta.
- I lettori copiano `received` in una variabile locale prima del confronto con `null`, perche' il campo e'
  `volatile` e un `reset` da un altro thread fra confronto e lettura darebbe un `NullPointerException`.
- `docs/compat.md`, "Avvio del client": aggiunta alla riga di `RedstoneExoframesClient.init()` la registrazione
  dell'azzeramento di `ClientRules` a `CLIENT_PLAYER_QUIT`, perche' quella riga elenca cio' che l'init client
  registra; il brief nomina solo le righe `:47` e `:156`. La tabella delle API (`frame_exit`) non l'ho toccata: e'
  la copertura delle API che cambiano, e `frame_state_sync` non c'e' neppure.
- Il Javadoc del metodo `exitHoldSeconds()` dice "I secondi dell'uscita" (non "dell'uscita dal telaio") per stare
  nei 120 caratteri.

## Scostamenti dal brief
- Nessuno nel comportamento. `run_gametests.sh` lanciato con `EXO_ACCEPT_EULA=true` davanti, come vuole la macchina.

## Autorevisione
- Completezza: tutte le voci di "Produces" ci sono, con i nomi e il testo della WARN del brief; nessuna chiave di
  testo nuova.
- `frame/ClientRules` non importa nulla del client (regola 7 e 8: `OK sources`); `ClientPlayerEvent` solo in
  `client/RedstoneExoframesClient`.
- Righe sotto i 120 caratteri nei file toccati (le due righe lunghe gia' esistenti in `RedstoneExoframes.java` non
  sono mie).

## Dubbi
- Nessuno. La ricezione vera nel client la prova il client vero del Task 24, come dice il brief.
