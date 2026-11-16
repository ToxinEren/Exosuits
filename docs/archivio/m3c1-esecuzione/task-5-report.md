# Task 5 - Un file senza sezioni vale come rovinato (passo b); salvataggio all'uscita ristretto

Stato: DONE. BASE 3296eb5f9abf3410d5e861d6c2ccbd4f10d23e7c, HEAD 88e36143c8202857fa59d74089b08fb5e735dec2.
Commit: `88e3614 compat: un file senza sezioni non vale come registro; salvataggio all'uscita limitato ai giocatori e al registro`.

## Che cosa ho fatto

- `FrameRegistryFormat`: `Outcome.WITHOUT_SECTIONS`; in `read`, prima di tutto (quindi prima del controllo dello
  schema), `!contains(SCHEMA) && !contains(FRAMES) && !contains(WORN) && !contains(PLACED)` -> `WITHOUT_SECTIONS`,
  senza log e senza leggere nulla. Javadoc di `read`, dell'enum e del commento della classe ("assente vale 1, se c'e'
  almeno una sezione").
- `FrameStateSavedData`:
  - campo `withoutSections`; `load` per `WITHOUT_SECTIONS` tiene `readOnlyRoot = tag.copy()` e `withoutSections = true`;
  - `reportedWithoutSections(Path)` come nel brief (ERROR una volta per dato, `unreadableFile = file`), chiamato su
    tutte e due le uscite di `get(DimensionDataStorage, Path)`;
  - `static createEmpty()` package-private per i test;
  - `saveWithPlayersIfNeeded`: stesse guardie, poi `server.getPlayerList().saveAll()` e
    `data.saveFile(dataFolder(server).resolve(NAME + ".dat"))` (non piu' `overworld().getDataStorage().save()`);
  - commento della classe: regola del file senza sezioni (accanto a quella del file illeggibile) e salvataggio
    all'uscita ristretto con i due motivi (non guarda `/save-off` perche' `PlayerList.remove` scrive comunque il file
    di chi esce, e separare i due file romperebbe §4.5; tutti i giocatori perche' il segno strutturale e' unico);
    Javadoc del metodo riscritta (tolte "registro compreso" e "Questo salvataggio non guarda /save-off");
    Javadoc di `readOnlyRoot`, `get`, `load`, `readOnly`, `write` aggiornate.
- `CarryOverGameTests`: `quitSaveIgnoresSaveOff` e `registryFileWithoutSectionsIsReadOnly` in coda a `generate()`,
  corpi come nel brief.
- `GameTestsImpl.readRegistry` (+ Javadoc in `compat/GameTests` e voce in `docs/compat.md`): su `>=1.21 && neoforge`
  aspetta `IOUtilities.waitUntilIOWorkerComplete()` (vedi Scostamenti).
- `tools/log_allowlist.txt`: la riga `=1` del brief, con una riga di commento.
- `docs/compat.md`: `:78` (salvataggio all'uscita), `:258` (frase del file senza sezioni dopo quella dello schema piu'
  recente), voce nuova nell'elenco "Se il file del registro non si legge", tabella delle chiavi (`schema`).
- Test JUnit: `fileWithoutSectionsIsReadOnly`, `schemaOneFileIsStillARegistry`, `createEmptyIsWritable`;
  `empty()` di `FrameStateSavedDataTest` e `RegistryFileTest` -> `createEmpty()`; `StackTagsTest` (`createEmpty()` e
  `load` di `{schema: 2}`); `orphanedListIsWrittenBackUnchanged`: `odd` con `schema: 2`.
- Design §11 voce "Un file del registro valido ma senza sezioni": rimanda gia' a §12.14 e descrive il difetto che il
  task chiude; non contraddice il codice, non toccata.

## TDD

RED
- `./gradlew --console=plain :1.21.1-neoforge:test --tests '*FrameStateSavedDataTest' --tests '*RegistryFileTest' --tests '*StackTagsTest'`
  -> `compileTestJava FAILED`, `error: cannot find symbol` su `StackTagsTest.java:47`, `FrameStateSavedDataTest.java:125`,
  `:679`, `RegistryFileTest.java:50` (`createEmpty` non esiste): atteso.
- `bash tools/run_gametests.sh 1.21.1-neoforge` ->
  `quit_save_ignores_save_off failed ... the quit save wrote the other saved data of the overworld` e
  `registry_file_without_sections_is_read_only failed ... a registry file without schema and sections is not read-only`,
  `2 required tests failed`: atteso (i due motivi del brief).

GREEN
- Stessi JUnit: `BUILD SUCCESSFUL`; `FrameStateSavedDataTest` 26 test, `RegistryFileTest` 10, `StackTagsTest` 2,
  0 fallimenti.
- `bash tools/run_gametests.sh 1.21.1-neoforge`, prima esecuzione: 100 test passati ma il controllo del log falliva su
  `[IO-Worker-6/ERROR] [ne.ne.ne.co.IOUtilities/]: Failed to list temporary files in /tmp/exoframes-registry-test...`
  (vedi Scostamenti). Dopo la correzione in `readRegistry`:
  `OK 1.21.1-neoforge: 100 test obbligatori passati, nessun avviso fuori elenco`.

## Verifiche di fine task

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`
  -> `BUILD SUCCESSFUL in 25s` (i quattro `:test` eseguiti; `FrameStateSavedDataTest` 26/26 su ogni variante).
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh` ->
  ```
  OK 1.20.1-forge: 100 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 100 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 100 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 100 test obbligatori passati, nessun avviso fuori elenco
  ```
  (la riga ERROR nuova esattamente una volta per variante: `=1` nell'elenco; `quitSavesTheRegistryWithThePlayers`
  verde).

## File cambiati

- `src/main/java/com/toxineren/exoframes/compat/impl/FrameRegistryFormat.java`
- `src/main/java/com/toxineren/exoframes/compat/impl/FrameStateSavedData.java`
- `src/main/java/com/toxineren/exoframes/compat/impl/GameTestsImpl.java`
- `src/main/java/com/toxineren/exoframes/compat/GameTests.java` (solo Javadoc)
- `src/main/java/com/toxineren/exoframes/gametest/CarryOverGameTests.java`
- `src/test/java/com/toxineren/exoframes/compat/impl/FrameStateSavedDataTest.java`, `RegistryFileTest.java`, `StackTagsTest.java`
- `tools/log_allowlist.txt`, `docs/compat.md`

## Scelte

- `schemaOneFileIsStillARegistry` usa `FrameRegistryFormat.FRAMES` per la chiave `frames` (stesso package, costante gia'
  usata dal formato) e un ciclo sui tre tag `{frames: {}}`, `{placed: {}}`, `{schema: 2}` con il tag nel messaggio
  dell'asserzione.
- `load` usa uno `switch` sull'esito (READ_ONLY, WITHOUT_SECTIONS, default per READ) invece di due `if`.
- `logRefusedChange` non cambia: nel gioco un dato senza sezioni passa sempre da `get`, che imposta `unreadableFile`,
  quindi la riga e' quella del file da ripristinare, come dice il brief. Solo un dato caricato a mano in un JUnit
  (senza `get`) cadrebbe nel ramo dello schema; nessun test lo guarda.
- In `quitSaveIgnoresSaveOff` l'asserzione sul file su disco calcola prima `String frameId` per stare nei 120
  caratteri; stesso contenuto del brief.
- `docs/compat.md`, tabella delle chiavi: `schema` "assente = 1, se c'e' almeno una sezione", per non contraddire la
  regola nuova (riga non nominata dal brief).
- `FrameRegistryFormat`, commento della classe: una frase sul tag senza schema ne' sezioni accanto a "assente vale 1".

## Scostamenti dal brief

- **`readRegistry` aspetta il lavoratore di I/O di NeoForge 1.21.1.** Su NeoForge 1.21.1 `DimensionDataStorage.readTagFromDisk`,
  dopo un file letto con successo, mette `IOUtilities.tryCleanupTempFiles(cartella, nome)` su `IOUtilities.withIOWorker`
  (verificato con `javap` sul jar del gioco e nei sorgenti di NeoForge 21.1.266). `registryFileWithoutSectionsIsReadOnly`
  e' il primo test che legge con successo un file in una cartella temporanea e la cancella subito nel `finally`: la
  pulizia arrivava dopo e scriveva `ERROR ... IOUtilities: Failed to list temporary files in /tmp/exoframes-registry-test...`
  (NoSuchFileException), fuori elenco. (`registryFileIsReadBack` non lo vedeva: nella copia i file sono troncati e la
  lettura del gioco fallisce prima di quel punto.) Correzione minima e deterministica in `compat/impl/GameTestsImpl.readRegistry`,
  dove le condizioni di versione e loader sono ammesse: `//? if >=1.21 && neoforge { IOUtilities.waitUntilIOWorkerComplete(); //?}`
  (nome completo, nessun import condizionale), cosi' `readRegistry` torna quando il loader non usa piu' la cartella.
  Detto nella Javadoc di `GameTests.readRegistry` e nella voce di `docs/compat.md` (regola delle cuciture). Nessuna
  firma cambia; il comportamento della mod in gioco non cambia. Alternativa scartata: rimandare la cancellazione con
  `runAfterDelay` (non deterministico).
- **Riga dell'elenco degli avvisi:** il brief dice "dopo la riga `:40`", ma la `:40` e' il commento "... il mod usa la
  copia precedente ..." che introduce la `:41`; la riga nuova (con un commento proprio) va dopo la `:43`, in coda al
  gruppo del registro dei test in gioco, per non spezzare quel gruppo.

## Self-review

- Completezza: tutti i punti di "Produces" fatti; test JUnit e in mondo come nel brief.
- `saveFile` scrive solo se `isDirty()`; con `structuralUnsaved` vero il dato e' sempre da salvare (`markStructural`
  chiama `setDirty`), e in sola lettura la guardia esce prima.
- Un dato senza sezioni in sola lettura: `isDirty()` falso, quindi ne' il salvataggio del gioco ne' quello all'uscita
  riscrivono il file; il test in mondo lo prova (byte uguali, nessun file accanto).
- Righe di Javadoc oltre 120 caratteri: solo due gia' presenti prima del task (`FrameStateSavedData`, ora righe 146 e 469).

## Problemi o dubbi

Nessuno aperto. Da sapere per la revisione: lo scostamento di `readRegistry` sopra (una riga condizionale NeoForge 1.21.1
nell'aiuto dei test).
