# Task 11 - Indicatori, sincronizzazione e changeWorn: rapporto

Stato: DONE. BASE d1e6bccc42a6d535cf609d182a4293ed7d8de0f7, commit `ccce810`.

## Che cosa ho fatto

- `compat/FrameIndicators` (nuovo): `record FrameIndicators(boolean canSprint, boolean canSwim, EnergyState energyState)`,
  costruttore compatto con `Objects.requireNonNull(energyState, "energyState")`, `of(FrameEffects)`.
- `RedstoneExoframes`: `private static volatile FrameRules rules = new FrameRules(config);` dopo `config`,
  `public static FrameRules rules()`, `setConfig` assegna `config` e poi `rules = new FrameRules(newConfig)`; Javadoc
  come da brief. Nessun altro `new FrameRules` nel codice principale (grep).
- `compat/impl/FrameStateSyncPacket`: terzo componente `Optional<FrameIndicators> indicators`; costruttore compatto con
  i tre `requireNonNull` e l'`IllegalArgumentException("state and indicators must be both present or both absent")`.
  Filo: UUID, flag, e solo se presente tag, `canSprint`, `canSwim`, `writeEnum(energyState)`; lettura nello stesso
  ordine, con un tag riletto null che vale assente (indicatori letti e scartati). Javadoc del formato nuovo.
- `compat/impl/FrameStateClientCache`: mappa `UUID -> Entry(FrameState, FrameIndicators)` (record privato);
  `accept` mette `new Entry(readTrusted(...), msg.indicators().orElseThrow())` o toglie; `view` invariato nel
  comportamento; nuovo `indicators(UUID)` (o null); `clear` invariato.
- `compat/impl/SyncCounter` (nuovo, package-private, solo statici, `ConcurrentHashMap<UUID, Integer>`):
  `increment`, `count` (0 se mai), `forget`.
- `compat/FrameStateAccess`: `clientIndicators(Player)`, `changeWorn(ServerPlayer, Consumer<FrameState>)`,
  `onSynced(Consumer<ServerPlayer>)` con Javadoc in italiano; paragrafo "Sincronizzazione" della classe aggiornato
  (indicatori funzione del solo stato perche' la sincronizzazione dell'ingresso parte prima della vestizione,
  `changeWorn` fra le scritture, `onSynced`); "al massimo 4-5 volte al secondo" resta.
- `compat/impl/FrameStateAccessImpl`: `syncListeners` (`CopyOnWriteArrayList`); `syncTo` nell'ordine del brief
  (`SyncCounter.increment`, record indossato, tag e indicatori insieme da `RedstoneExoframes.rules().evaluate`,
  `sendToPlayer`, ascoltatori); `changeWorn` (null-check, sola lettura -> `logRefusedChange()` e falso, record mancante
  -> falso senza `syncTo`, altrimenti `data.changeWorn` con `syncTo` nel `finally`); `clientIndicators`; `onSynced`;
  in `PLAYER_QUIT` anche `SyncCounter.forget` dopo `saveWithPlayersIfNeeded`. Javadoc della classe aggiornato.
- `compat/impl/FrameRegistry.changeWorn` (package-private) + delegato di una riga in `FrameStateSavedData`.
- `compat/GameTests.syncCount(ServerPlayer)` con Javadoc; `GameTestsImpl` = `SyncCounter.count(player.getUUID())`,
  senza direttive.
- `gametest/GameTestKit.setStored(ServerPlayer, int)` come da brief.
- `gametest/CarryOverGameTests`: `changeWornNeverCreatesARecord`, `changeWornChangesAndSyncsOnce` in coda a
  `generate()`.
- `docs/compat.md`: `Net` "Uso" (gli indicatori di `frame_state_sync`), `FrameStateAccess` "File" (`SyncCounter.java`,
  `FrameIndicators.java`), "A cosa serve" (testo del brief dopo `mutate`), "Differenze chiuse" (frase del codec),
  `GameTests` "A cosa serve" (`syncCount`).

## TDD

RED: `./gradlew --console=plain :1.21.1-neoforge:test --tests '*FrameStateSyncPacketTest' --tests '*FrameStateSavedDataTest'`
-> exit 1, errori di compilazione attesi:
```
FrameStateSyncPacketTest.java:6: error: cannot find symbol            (FrameIndicators)
FrameStateSavedDataTest.java:635: error: cannot find symbol           (changeWorn)
FrameStateSyncPacketTest.java:49: error: constructor FrameStateSyncPacket in record FrameStateSyncPacket cannot be applied to given types;
```
GREEN, stesso comando: `BUILD SUCCESSFUL`; XML: FrameStateSyncPacketTest tests="3" failures="0" errors="0",
FrameStateSavedDataTest tests="29" failures="0" errors="0".

Step 3 sulle quattro varianti (`:1.20.1-forge:test ... :1.21.1-neoforge:test --tests '*FrameStateSyncPacketTest'`):
`BUILD SUCCESSFUL in 14s`; XML freschi (20:08:29-20:08:38) con tests="3" skipped="0" failures="0" errors="0" su
1.20.1-forge, 1.20.1-fabric, 1.21.1-fabric, 1.21.1-neoforge. `FriendlyByteBuf` e netty si caricano in JUnit
ovunque: **nessun ripiego** (niente `syncPacketRoundTrips`), annotato nel commit.

## Verifiche di fine task

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`
  -> `BUILD SUCCESSFUL in 22s` (52 actionable tasks: 28 executed, 24 up-to-date), nessun `warning:`/`error:`.
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh` -> exit 0:
```
OK 1.20.1-forge: 116 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 116 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 116 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 116 test obbligatori passati, nessun avviso fuori elenco
```
Nessun server, client o build lasciato acceso.

## File cambiati

docs/compat.md; src/main/java/com/toxineren/exoframes/{RedstoneExoframes.java, compat/FrameIndicators.java (nuovo),
compat/FrameStateAccess.java, compat/GameTests.java, compat/impl/FrameRegistry.java, compat/impl/FrameStateAccessImpl.java,
compat/impl/FrameStateClientCache.java, compat/impl/FrameStateSavedData.java, compat/impl/FrameStateSyncPacket.java,
compat/impl/GameTestsImpl.java, compat/impl/SyncCounter.java (nuovo), gametest/CarryOverGameTests.java,
gametest/GameTestKit.java}; src/test/java/com/toxineren/exoframes/compat/impl/{FrameStateSavedDataTest.java,
FrameStateSyncPacketTest.java (nuovo)}.

## Scelte

- `changeWornRefusedWhenReadOnly`: le due radici del brief (`{foo: 1}` e `mixedRoot()` con schema 3) in un ciclo dentro
  un solo test, ciascuna con il suo `ran` e la sua copia: stesse asserzioni del brief, meno duplicazione.
- `docs/compat.md`, "A cosa serve" di `FrameStateAccess`: oltre al testo del brief, "uno dei tre metodi di scrittura" e
  "le tre scritture" diventano "quattro" (con `changeWorn` le scritture sono quattro; lasciarlo avrebbe reso la pagina
  incoerente). Stessa correzione nel Javadoc di classe di `FrameStateAccessImpl`.
- Il commento sopra l'ascoltatore di `PLAYER_QUIT` dice anche che poi il contatore si dimentica; l'ascoltatore e' ora
  una lambda a blocco con le due chiamate nell'ordine del brief.
- Nel codec la scrittura prende `msg.indicators().orElseThrow()` dentro il ramo "presente": il costruttore compatto
  garantisce che ci sia.

## Scostamenti dal brief

Nessuno nei comportamenti. Solo a capo di righe oltre i 120 caratteri nel testo dei test (la lista dei tre indicatori
di `roundTripsStateAndIndicators`, alcune asserzioni dei test in mondo), senza cambiare le asserzioni.

## Autorevisione

- `syncTo` conta prima di leggere il registro e prima dell'invio: il contatore sale anche dove NeoForge 1.21.1 salta
  l'invio (i test in mondo lo confermano su quella variante).
- `changeWorn` senza record non chiama `syncTo` (provato da `changeWornNeverCreatesARecord`); con record sincronizza
  una sola volta (`changeWornChangesAndSyncsOnce`, anche via `setStored`).
- Un'eccezione di un ascoltatore di `onSynced` risale a chi ha chiamato `syncTo`, come il brief prevede (nessuna
  cattura chiesta); con zero ascoltatori oggi nulla cambia. Il Task 14 (`SuitTick.afterSync`) e' il primo ascoltatore.
- `setConfig` assegna `config` e poi `rules` senza atomicita' fra i due: chiamato una volta all'avvio, prima di ogni
  registrazione, come il brief dice.

## Problemi o dubbi

Nessuno.
