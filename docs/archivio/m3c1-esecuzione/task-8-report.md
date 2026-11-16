# Task 8 - Config da file: report

Status: DONE_WITH_CONCERNS (una sola osservazione minore, sotto "Dubbi")
BASE cd5b0574c56a1f1f627e6f64bc092166c5edb9c6, HEAD 2f6d61e0fbd7a73fbe5e881f158e9be523a3de9e
Commit: `2f6d61e config: lettura da config/redstone_exoframes.json con limiti superiori`

## Che cosa ho fatto

- `configfile/ConfigFile` (nuovo): `FILE_NAME`, `record Result(config, problems, wroteDefaults)` con
  `List.copyOf`, `load(Path)`. File assente: predefiniti in JSON indentato in un temporaneo della stessa cartella
  (`createTempFile(parent, "redstone_exoframes", ".json.tmp")`), `Files.move` senza `REPLACE_EXISTING`,
  `deleteIfExists(tmp)` nel `finally`; errore -> `could not be written: <e>`, predefiniti, `wroteDefaults` falso.
  File presente: `readString` (-> `could not be read (...)`), `JsonParser.parseString` (-> `is not valid JSON (...)`),
  radice non oggetto (-> `is not a JSON object, ...`), `GSON.fromJson` con cattura di `RuntimeException` (anche la NPE
  della chiave di enumerazione sconosciuta -> `does not match the config format (...)`); poi
  `ConfigValidation.validate` e risultato normalizzato. Nessun log, nessun import di Minecraft, nessuna direttiva.
- `ExoframesConfig`: `Energy.chargerRePerSecond = 50` dopo `chargerRePerRedstone`; Javadoc della classe: la lettura
  del file sta in `configfile/ConfigFile` (prima diceva `platform/`).
- `ConfigValidation`: le cinque costanti pubbliche (`MAX_CELL_CAPACITY`, `MAX_RATE`, `MAX_RE_PER_REDSTONE`,
  `MAX_HOLD_SECONDS`, `MAX_MULTIPLIER`); nel `Ctx` overload a quattro argomenti di `positive` (int e double),
  `nonNegative` e `atLeastOne` con `max`, che chiamano il metodo di oggi e poi `atMost` (messaggio
  `"<campo>: <v> is invalid (must be <= <max>), using default <def>"`); limiti applicati esattamente ai campi del
  brief (cellCapacity; idle/walk/sprint/jumpCost; costi dei moduli in `validateCosts`; chargerRePerRedstone;
  chargerRePerSecond nuovo, positivo e <= MAX_RATE; cellSwapSeconds ed exitHoldSeconds; brokenTorsoConsumptionMultiplier
  e tiers.protectionMultiplier). I casi senza limite restano sui metodi a tre argomenti. Javadoc della classe con
  l'elenco dei limiti e il perche'.
- `RedstoneExoframes.init()`: prima istruzione la lettura del file in `Compat.platform().configDir()`, una WARN
  `Config file {}: {}` per problema, poi `setConfig(loaded.config())`, poi la riga INFO e le registrazioni. Javadoc
  di `setConfig` aggiornata.
- `CarryOverGameTests.configFileExists` (test in mondo 111).
- `tools/run_gametests.sh`: `rm -f "versions/$v/run-gametest/config/redstone_exoframes.json"` dopo `mkdir -p` e prima
  di Gradle, con la frase nel commento di testa; `tools/README.md` riga di `run_gametests.sh`.
- `docs/compat.md` (riga **Uso** di `Platform`, testo del brief) e `README.md` (paragrafo "Configurazione" dopo
  "Comandi", testo del brief).

## File cambiati

- src/main/java/com/toxineren/exoframes/configfile/ConfigFile.java (nuovo)
- src/test/java/com/toxineren/exoframes/configfile/ConfigFileTest.java (nuovo)
- src/main/java/com/toxineren/exoframes/core/config/ExoframesConfig.java
- src/main/java/com/toxineren/exoframes/core/config/ConfigValidation.java
- src/test/java/com/toxineren/exoframes/core/config/ExoframesConfigTest.java
- src/main/java/com/toxineren/exoframes/RedstoneExoframes.java
- src/main/java/com/toxineren/exoframes/gametest/CarryOverGameTests.java
- tools/run_gametests.sh, tools/README.md, docs/compat.md, README.md

## TDD

RED (Step 2): `./gradlew --console=plain :1.21.1-neoforge:test --tests '*ConfigFileTest' --tests '*ExoframesConfigTest'`
```
> Task :1.21.1-neoforge:compileTestJava FAILED
.../ConfigFileTest.java:37: error: package ConfigFile does not exist
.../ConfigFileTest.java:34: error: cannot find symbol
```
Atteso: `ConfigFile`, le costanti e il campo non esistevano.

GREEN (Step 5), stesso comando: `BUILD SUCCESSFUL`; rapporti JUnit
`ConfigFileTest tests="13" failures="0" errors="0"`, `ExoframesConfigTest tests="12" failures="0" errors="0"`.

RED in mondo (Step 7): `bash tools/run_gametests.sh 1.21.1-neoforge` -> `FAIL 1.21.1-neoforge: Gradle e' uscito con 1`,
nel log `redstone_exoframes:config_file_exists failed ... config file missing:
/home/user/exosuits/versions/1.21.1-neoforge/run-gametest/config/redstone_exoframes.json` e `1 required tests failed`
(solo quello).

GREEN in mondo (Step 9): `OK 1.21.1-neoforge: 111 test obbligatori passati, nessun avviso fuori elenco`.

## Server dedicato (Step 10)

Avvio con `./gradlew --console=plain :<v>:runServer` con lo stdin da una FIFO, attesa di `Done (`, `stop` sulla
console, Gradle uscito con 0 ogni volta. `eula=true` era gia' in `versions/<v>/run/eula.txt` sulle due varianti.

- 1.20.1-forge, primo avvio dopo `rm -f` del file: file scritto, `"chargerRePerSecond": 50` presente; il grep delle
  WARN/ERROR con "exoframe" ha stampato una riga, non della config:
  `[main/WARN] [net.minecraftforge.registries.ForgeRegistry/REGISTRIES]: Registry minecraft:item: Object did not get ID
  it asked for. Name: redstone_exoframes:exoframe Expected: 1274 Got: 1275` - e' la rimappatura degli id di Forge per
  il mondo vecchio di `versions/1.20.1-forge/run/world` (ultimo avvio il 13 novembre, prima degli oggetti nuovi della
  3b). Secondo avvio, di nuovo con `rm -f` del file: file riscritto con `"chargerRePerSecond": 50`, `Done (2.983s)!`,
  grep vuoto.
- 1.21.1-neoforge, primo avvio dopo `rm -f`: file scritto, `"chargerRePerSecond": 50`, `Done (0.436s)!`, grep vuoto.
  Quindi `configDir()` risponde nel costruttore della mod su Forge e NeoForge.
- 1.21.1-neoforge con il file rovinato (`printf '{energy:'`, copia accanto): una sola riga
  `[modloading-worker-0/WARN] [Redstone Exoframes/]: Config file /home/user/exosuits/versions/1.21.1-neoforge/run/config/redstone_exoframes.json: is not valid JSON (java.io.EOFException: End of input at line 1 column 9 path $.energy), using the defaults`,
  `Done (` presente, `cmp` fra file e copia senza differenze; poi `rm -f` del file e della copia.

## Verifiche di fine task (Step 12)

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`
  -> `BUILD SUCCESSFUL in 23s`; sulle quattro varianti `ConfigFileTest` 13/13 e `ExoframesConfigTest` 12/12.
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `bash tools/run_gametests.sh` (uscita 0):
```
OK 1.20.1-forge: 111 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 111 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 111 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 111 test obbligatori passati, nessun avviso fuori elenco
```
Nessun Gradle, server o client lasciato acceso.

## Scelte

- La cartella da creare e' `file.toAbsolutePath().getParent()` invece di `file.getParent()`: un percorso relativo di
  un solo nome avrebbe `getParent()` nullo; per i percorsi assoluti (quello di `configDir()`) e' lo stesso.
- I limiti nel `Ctx` sono overload a quattro argomenti che chiamano il metodo di oggi e poi un `atMost` comune: il
  controllo inferiore e il messaggio di oggi restano identici, e un valore gia' tornato al predefinito non da' un
  secondo messaggio (i predefiniti stanno tutti sotto i limiti).
- `brokenFileIsNeverRewritten` porta l'ora di modifica del file un'ora indietro prima di `load`: una riscrittura
  dentro lo stesso istante di un file system a grana grossa non passerebbe inosservata. Controlla anche che ci sia
  almeno un problema e che `wroteDefaults` sia falso.
- `wrongTypeFallsBackToDefaults` e `unknownEnumKeyFallsBackToDefaults` confrontano anche l'intera config con i
  predefiniti (oltre a `walkPerSecond` 8); `partialMapReportsTheMissingKeys` controlla il testo esatto dei quattro
  problemi e che siano esattamente quattro.
- Il `deleteIfExists(tmp)` nel `finally` ignora una propria `IOException` (un temporaneo rimasto non cambia la config
  in uso e non deve diventare un'eccezione di `load`).

## Scostamenti dal brief

- Nessuno sostanziale. Solo la cartella assoluta descritta sopra. Lo Step 10 su 1.20.1-forge ha richiesto un secondo
  avvio per avere il grep vuoto (la riga WARN del primo era la rimappatura degli id di Forge del mondo vecchio, non la
  config).

## Autoverifica

Rilette le differenze: nessuna riga nuova oltre 120 caratteri, identificatori e messaggi in inglese, Javadoc e
commenti in italiano con apostrofi ASCII, Gson solo in `configfile/` (regola 1b: `OK sources`), nessun log in
`ConfigFile`. `setConfig` e' chiamata solo da `init()`.

## Dubbi

- Il file dei predefiniti nasce con i permessi `-rw-------` (0600), perche' `Files.createTempFile` del brief crea il
  temporaneo cosi' e lo spostamento li conserva. Il proprietario del processo del server lo legge e lo modifica; un
  altro utente della macchina (per esempio un gruppo di amministratori) no. Non ho cambiato nulla: e' il metodo che
  il brief fissa; se si vuole 0644, servirebbe un temporaneo creato con `CREATE_NEW` e nome fisso, o i permessi
  POSIX impostati dove il file system li ha.

## Fix round 1

FIX_BASE 2f6d61e0fbd7a73fbe5e881f158e9be523a3de9e (nessuna modifica in sospeso nell'albero alla partenza).
Commit: `177fc4d config: un numero che un campo intero non tiene rifiuta il file; predefiniti con i permessi della
umask`, `5389f2b test: limiti superiori di idle, sprint, salto, cambio cella, costi dei moduli e protezione dei
livelli`.

### T8-F1 (Important) - numeri che un campo int non tiene
- `ConfigFile.load`: `JsonParser.parseString` resta solo per "is not valid JSON" e "is not a JSON object"; la config
  si costruisce con `GSON.fromJson(text, ExoframesConfig.class)` (ruling del ledger). Il lettore a flusso lancia
  `NumberFormatException: Expected an int but was ...`, che Gson avvolge in `JsonSyntaxException` e che finisce nel
  ramo "does not match the config format": un problema, tutto ai predefiniti.
- Javadoc della classe: il caso del decimale e dell'intero oltre `int` e il perche' dell'albero solo per i controlli.
- `README.md`, paragrafo "Configurazione": "(tutto, se il file non si legge o non e' una config: non e' JSON, o un
  valore ha il tipo sbagliato, compreso un decimale, come 0.5, o un intero oltre 2147483647 in un campo intero)".
  `docs/compat.md` non descrive questa regola (solo la riga **Uso** di `Platform`): invariato.
- JUnit nuovi in `ConfigFileTest`: `decimalInAnIntFieldFallsBackToDefaults` (`{"energy": {"walkPerSecond": 0.5}}`),
  `integerBeyondIntFallsBackToDefaults` (`{"energy": {"cellCapacity": 4294977296}}`): esattamente un problema, che
  comincia con `does not match the config format` e contiene il valore scritto, config uguale ai predefiniti,
  `!wroteDefaults()`. I due testi sono anche in `brokenFileIsNeverRewritten`.
- Prova a mano con le classi compilate di 1.21.1-neoforge e gson 2.10.1 (sonda nello scratchpad, nulla nel checkout):
  ```
  {"energy": {"walkPerSecond": 0.5}} -> [does not match the config format (com.google.gson.JsonSyntaxException: java.lang.NumberFormatException: Expected an int but was 0.5 at line 1 column 33 path $.energy.walkPerSecond), using the defaults] walk=8
  {"frame": {"exitHoldSeconds": 2.9}} -> [... Expected an int but was 2.9 ... path $.frame.exitHoldSeconds), using the defaults]
  {"energy": {"cellCapacity": 4294977296}} -> [... Expected an int but was 4294977296 ... path $.energy.cellCapacity), using the defaults]
  {"energy": {"cellCapacity": 10000000000}} -> [... Expected an int but was 10000000000 ...), using the defaults]
  {"energy": {"walkPerSecond": 5.0}} -> [] walk=5
  ```
  Gson 2.10 (1.20.1) e' provato dai JUnit delle due varianti 1.20.1 nella build.

### T8-F2 (Minor) - permessi del file dei predefiniti
- `ConfigFile.writeDefaults`: il temporaneo e' `<nome del file>-<numero>.tmp` nella stessa cartella (numero da
  `SecureRandom`, come `RegistryFile`), scritto con `Files.writeString(tmp, text, UTF_8, CREATE_NEW, WRITE)`; stesso
  `Files.move(tmp, file)` senza `REPLACE_EXISTING`, stesso `deleteIfExists(tmp)` nel `finally`. Se il nome esiste gia'
  (`FileAlreadyExistsException`) `tmp` torna `null` prima di rilanciare, cosi' il `finally` non cancella un file non
  nostro; l'eccezione diventa il problema `could not be written: ...` come prima. Javadoc della classe aggiornata.
- JUnit nuovo `defaultsFileFollowsTheUmask` (saltato con `assumeTrue` se la cartella non ha la vista POSIX): i permessi
  del file scritto da `load` sono uguali a quelli di un `reference.json` scritto accanto con `Files.writeString`.
- Dopo i GameTest i file scritti dalla mod sono `-rw-r--r--` in `versions/*/run-gametest/config/` sulle quattro
  varianti (prima `-rw-------`).

### T8-F3 (Minor) - limiti superiori senza JUnit
- `ExoframesConfigTest.otherUpperBoundsFallBackToDefaults`, un caso per campo, ciascuno su una config nuova:
  `energy.idlePerSecond`, `energy.sprintPerSecond`, `energy.jumpCost` (`MAX_RATE + 1`), `energy.cellSwapSeconds`
  (`MAX_HOLD_SECONDS + 1`), `modules.costPerSecond.JETPACK` e `modules.costPerUse.SHOCKWAVE` (`MAX_RATE + 1`, via
  `validateCosts`), `tiers.protectionMultiplier.ARMORED` (`MAX_MULTIPLIER + 0.5`). Oltre il limite: valore al
  predefinito e la lista dei problemi e' esattamente
  `[<campo>: <v> is invalid (must be <= <max>), using default <def>]`; al limite: valore tenuto, nessun problema.

### TDD
RED, `./gradlew --console=plain :1.21.1-neoforge:test --tests '*ConfigFileTest' --tests '*ExoframesConfigTest'`
prima del codice:
```
ConfigFileTest > integerBeyondIntFallsBackToDefaults() FAILED   ([] ==> expected: <1> but was: <0>)
ConfigFileTest > defaultsFileFollowsTheUmask() FAILED   (expected: <[OWNER_READ, OTHERS_READ, OWNER_WRITE, GROUP_READ]> but was: <[OWNER_READ, OWNER_WRITE]>)
ConfigFileTest > decimalInAnIntFieldFallsBackToDefaults() FAILED   ([] ==> expected: <1> but was: <0>)
ConfigFileTest > brokenFileIsNeverRewritten() FAILED   ({"energy": {"walkPerSecond": 0.5}} ==> expected: <false> but was: <true>)
29 tests completed, 4 failed
```
`otherUpperBoundsFallBackToDefaults` passa gia' al primo giro: i limiti erano giusti, mancava il test (era la
richiesta di F3).
GREEN, stesso comando: `BUILD SUCCESSFUL`; `ConfigFileTest tests="16" failures="0" errors="0"`,
`ExoframesConfigTest tests="13" failures="0" errors="0"`.

### Verifiche di fine task
- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`
  -> `BUILD SUCCESSFUL in 21s`; sulle quattro varianti `ConfigFileTest` 16/16 e `ExoframesConfigTest` 13/13, nessuno
  saltato.
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh` (uscita 0):
```
OK 1.20.1-forge: 111 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 111 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 111 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 111 test obbligatori passati, nessun avviso fuori elenco
```
Nessun Gradle, server o client lasciato acceso. Nessun finding contestato.
