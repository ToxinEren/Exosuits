# Task 1 — Riporti dalla 3c-1 e `client_rules` ricevuto nel client vero

Stato: **DONE**. BASE `e993d7e0dbf8000df9c0c28f3ef6ae1a7a31d694`, HEAD `1846274fe8707f6dede8bd132be755b875b63097`.
Un commit: `1846274 tools: riporti della 3c-1; client_rules provato nel client vero con la config del server cambiata`.

`<tmp>` qui sotto è la cartella temporanea della sessione
(`<area-temporanea>/scratchpad/t1`).

## Che cosa è stato fatto

- **Step 1, `LangFilesTest`** (`src/test/java/com/toxineren/exoframes/LangFilesTest.java`): legge `en_us.json` e
  `it_it.json` dal class loader e li scorre con `JsonReader`, nome per nome. Quattro test: `sameKeysInTheSameOrder`
  (il messaggio nomina la prima chiave che differisce, o la prima chiave oltre la fine del file più corto),
  `noKeyIsWrittenTwice`, `samePlaceholdersInBothLanguages` (`%%` non conta), `noValueIsEmpty`.
- **Step 2, tre test in mondo** (172 → 175): `firstEvaluationOfAnEmptyCellTellsWithoutSound` e
  `improvementFromEmptyToLowIsSilent` in `EnergyGameTests`; `cellSwapTakesTheOffhandCellAndRefusesASpectator` in
  `CellGameTests`. Fissano un comportamento che c'era già: nessuna riga di codice del mod è cambiata.
- **Step 3, testi e commenti**: FW-B3 (`FrameStateAccessImpl`, "file illeggibile con la sua copia precedente"); T10-F1
  (commento dei mixin in `build.gradle.kts`); T1-F1 (via quattro import di `GameTestKit`); T1-F2 (commento della classe
  `GameTestKit`); T1-F3 (Javadoc di `fillPool`); T21-F1 (Javadoc di `cellSwapAfterDeathChangesNothing`); sonda
  (`TickEvent.PLAYER_PRE.register(listener)` spostato dopo l'asserzione su `player.hurt`).
- **Step 4-5, FW-B1**: `m3c_shots.sh` toglie `"$out/server.log"` prima di chiamare `client_shot.sh`.
- **Step 6, FW-B2**: `EXO_SHOT_SERVER_CONFIG` in `client_shot.sh` (controllo insieme agli altri dell'ambiente, percorso
  reso assoluto appena letto, copia subito dopo la riga che toglie i due file di config), nel commento di testa
  (paragrafo delle cartelle, tabella delle variabili, elenco delle uscite) e in `tools/client/README.md`.
- **Step 7, `tools/client/m3c_rules_shots.sh`** (eseguibile, modo 100755 nell'indice), con i passi del brief alla
  lettera.
- **Step 8, client vero**: FA-2 passa sulle quattro varianti; prova negativa su `1.21.1-fabric`; due scatti copiati
  senza metadati in `docs/test-in-gioco/m3c-screenshot/` (solo i chunk `IHDR`, `IDAT`, `IEND`).
- **Step 9, documenti**: `tools/client/README.md` (riga d'uso, paragrafo `m3c_rules_shots.sh` con tabella dei passi,
  durata misurata) e `docs/compat.md`, sezione `Net`, riga **Uso** (frase del brief alla lettera).

## Prove e risultati

### TDD: RED e GREEN

- **FW-B1, RED** (prima della modifica), `mkdir -p <tmp>/fwb1 && echo 'moved wrongly' > <tmp>/fwb1/server.log && bash
  tools/client/m3c_shots.sh 1.21.1-fabric <tmp>/fwb1; echo "uscita $?"` senza `EXO_ACCEPT_EULA`:
  ```
  ERRORE: 1 righe 'moved wrongly' in <tmp>/fwb1/server.log: client e server non sono d'accordo sul movimento della tuta
  FAIL 1.21.1-fabric: moved wrongly in <tmp>/fwb1/server.log
  uscita 2
  ```
  Atteso: il messaggio è falso, il log è di un'esecuzione vecchia.
- **FW-B1, GREEN** (stesso comando, dopo la modifica; rifatto anche dopo l'ultimo ritocco al commento):
  ```
  FAIL 1.21.1-fabric: client_shot.sh e' uscito con 2
  uscita 2
  ```
  Nessuna riga `moved wrongly`; `server.log` non c'è più nella cartella.
- **FW-B2**, `EXO_SHOT_SERVER_CONFIG=<tmp>/non-esiste.json EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh
  1.21.1-fabric <tmp>/fwb2; echo "uscita $?"`:
  ```
  ERRORE: EXO_SHOT_SERVER_CONFIG: file non trovato o non leggibile: <tmp>/non-esiste.json
  uscita 2
  ```
  Nessun server avviato, la cartella `<tmp>/fwb2` non viene nemmeno creata. Con un percorso relativo
  (`EXO_SHOT_SERVER_CONFIG=rel/x.json`, lanciato da `<tmp>`) il messaggio mostra il percorso assoluto
  `<tmp>/rel/x.json`.
- **I test nuovi fissano un comportamento che c'è già**, quindi il loro RED è la prova negativa del brief:
  - `LangFilesTest`: `./gradlew --console=plain :1.21.1-neoforge:test --tests '*LangFilesTest'` → `BUILD SUCCESSFUL`,
    `tests="4" failures="0"`. Prova negativa (righe 30 e 31 di `it_it.json` scambiate): `exit 1`,
    ```
    LangFilesTest > sameKeysInTheSameOrder() FAILED
        org.opentest4j.AssertionFailedError: first difference at position 28: en_us has 'message.redstone_exoframes.locked', it_it has 'message.redstone_exoframes.slot_taken' ==> ...
    4 tests completed, 1 failed
    ```
    File rimesso con `git checkout --`, `git diff --stat` senza `it_it.json`.
  - Test in mondo: `bash tools/run_gametests.sh 1.21.1-neoforge` →
    `OK 1.21.1-neoforge: 175 test obbligatori passati, nessun avviso fuori elenco`. Prova negativa
    (`Inventory.SLOT_OFFHAND` → `39` in `frame/CellSwap.java`): `exit 1`,
    ```
    redstone_exoframes:cell_swap_takes_the_offhand_cell_and_refuses_a_spectator failed at ...! class net.minecraft.world.item.ArmorItem cannot be cast to class com.toxineren.exoframes.content.CellItem ...
    1 required tests failed :(
       - redstone_exoframes:cell_swap_takes_the_offhand_cell_and_refuses_a_spectator
    FAIL 1.21.1-neoforge: Gradle e' uscito con 1
    ```
    (lo slot 39 è l'elmo: il gestore prende il segnaposto al posto della cella). `CellSwap.java` rimesso con
    `git checkout --`.

### Client vero, FA-2 (Step 8)

`EXO_ACCEPT_EULA=true bash tools/client/m3c_rules_shots.sh <variante> <tmp>/fa2-<variante>`, una variante alla volta:

```
OK 1.20.1-forge: scatti in <tmp>/fa2-1.20.1-forge        (uscita 0, fatto in 121 s)
OK 1.20.1-fabric: scatti in <tmp>/fa2-1.20.1-fabric      (uscita 0, fatto in 97 s)
OK 1.21.1-fabric: scatti in <tmp>/fa2-1.21.1-fabric      (uscita 0, fatto in 128 s)
OK 1.21.1-neoforge: scatti in <tmp>/fa2-1.21.1-neoforge  (uscita 0, fatto in 95 s)
```

Sulle quattro: `log-check.txt` vuoto (0 byte); nessuna riga `Config file` né `Received client rule` in
`gradle-server.log`, `gradle-client.log`, `server.log`, `client.log` (contate: 0 in tutti e sedici i file); risposte di
`/exoframe debug energy` in `rcon.log`, uguali sulle quattro: `Cell: 19996 / 20000 RE (normal)` (capacità),
`Cell: 989 / 20000 RE (low)` (dopo R tenuto 3 s: nessun cambio), `Cell: 4996 / 20000 RE (normal)` (dopo R tenuto 5 s).

Scatti guardati:
- `regole-barra.png`, sulle quattro: inventario aperto, la cella nel primo slot della barra rapida con la barra rossa
  corta. Misurata sui pixel dello scatto (scala dell'interfaccia 2): 6 pixel rossi su 26, cioè **3 su 13**, uguale
  sulle quattro varianti (a metà sarebbero 14 su 26).
- `regole-uscita.png`: "Hold to leave the exoframe… 51 %" (`1.21.1-fabric`), 51 % (`1.20.1-forge`), 50 %
  (`1.20.1-fabric`), 48 % (`1.21.1-neoforge`); in alto a sinistra la riga "Energy 19993 / 20000 RE (99 %)".
- `back.png` finale: il giocatore fuori dalla tuta, il telaio Rottame lasciato dall'uscita sei blocchi davanti.

**Prova negativa** (`1.21.1-fabric`, config `{}`), comando del brief:
```
[41s] comando eseguito: execute store result score #cap exo_shot run execute as Exo run exoframe debug energy
[41s] ERRORE: comando rifiutato o non riuscito: "execute if score #cap exo_shot matches 19900..20000" (risposta in <tmp>/fa2-neg/rcon.log)
uscita 1
```
(`rcon.log`: `Test failed`, `#exo has 0 [exo_shot]`.)

### Verifiche di fine task (Step 10), sulle quattro varianti

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
  :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` →
  `BUILD SUCCESSFUL in 32s` (52 actionable tasks: 33 executed, 19 up-to-date; i quattro `:test` eseguiti;
  `LangFilesTest` `tests="4" skipped="0" failures="0" errors="0"` nei risultati di ciascuna variante).
- `bash tools/check_sources.sh` → `OK sources`.
- `bash tools/check_jars.sh` → `OK jars`.
- `bash tools/run_gametests.sh` (uscita 0):
  ```
  OK 1.20.1-forge: 175 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 175 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 175 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 175 test obbligatori passati, nessun avviso fuori elenco
  ```
- `EXO_ACCEPT_EULA=true bash tools/client/m3c_shots.sh 1.21.1-fabric <tmp>/m3c-1.21.1-fabric` →
  `OK 1.21.1-fabric: scatti in <tmp>/m3c-1.21.1-fabric`, uscita 0, fatto in 126 s, `log-check.txt` vuoto, 0 righe
  `moved wrongly`. Dopo questa esecuzione la config del server in `run-shot-server/config/` è di nuovo quella
  predefinita (il file della prova FA-2 non resta da un'esecuzione all'altra).
- `bash -n` sui tre script toccati: nessuna uscita. `shellcheck`: solo le note informative SC2015 (`A && B || C`)
  dello stile che il file aveva già (cinque prima, sette ora: le due nuove sono le due righe `… || fatal`).
- `git diff --check`: pulito. Nessun server, client o Xvfb rimasto in esecuzione.

Dopo le verifiche di fine task ho ritoccato solo due commenti di script (un a capo nel commento di testa di
`client_shot.sh`, sei parole nel commento della riga `rm -f` di `m3c_shots.sh`): rifatti `bash -n`, le due prove
anticipate (FW-B1 e FW-B2) e `check_sources.sh`; build, GameTest e client vero non rifatti, perché nessuna riga
eseguita è cambiata.

## Ciò che il registro chiedeva di riferire ("never run by anyone before execution")

- **File di lingua letti dal class loader su Forge e NeoForge**: funziona.
  `LangFilesTest.class.getResourceAsStream("/assets/redstone_exoframes/lang/…")` trova i due file nei test JUnit
  delle quattro varianti (quattro test passati su ciascuna).
- **Gli scatti**: presi e guardati, vedi sopra.
- Osservato in più: un file di config parziale e valido sul server (`{"energy": {...}, "frame": {...}}`) resta
  com'è scritto, la mod non lo riscrive e non scrive avvisi; nella cartella del client, senza file, la mod riscrive i
  predefiniti all'avvio.

## File cambiati

Creati: `tools/client/m3c_rules_shots.sh`, `src/test/java/com/toxineren/exoframes/LangFilesTest.java`,
`docs/test-in-gioco/m3c-screenshot/1.20.1-forge-regole-barra.png`,
`docs/test-in-gioco/m3c-screenshot/1.21.1-neoforge-regole-barra.png`.

Modificati: `tools/client/client_shot.sh`, `tools/client/m3c_shots.sh`, `tools/client/README.md`,
`src/main/java/com/toxineren/exoframes/compat/impl/FrameStateAccessImpl.java` (solo Javadoc), `build.gradle.kts`
(solo un commento), `src/main/java/com/toxineren/exoframes/gametest/GameTestKit.java` (import e commenti),
`gametest/CarryOverGameTests.java`, `gametest/EnergyGameTests.java`, `gametest/CellGameTests.java`,
`docs/compat.md`.

## Scelte

1. **`m3c_rules_shots.sh` toglie `"$out/log-check.txt"` prima di chiamare `client_shot.sh`.** Il brief dice "riga
   finale come `m3c_shots.sh`", che dopo un'uscita anticipata di `client_shot.sh` (uscita 2) legge il `log-check.txt`
   di un'esecuzione precedente e stamperebbe "avvisi del mod fuori elenco": è lo stesso difetto di FW-B1 su un altro
   file. Nello script nuovo l'ho chiuso con una riga; negli script vecchi non l'ho toccato (vedi "Punti aperti").
2. **`mkdir -p "$out"` con uscita 2 e una riga `ERRORE:`** se la cartella non si può creare (il brief ha solo
   `mkdir -p "$out"`): senza, la riga dopo scriverebbe la config in una cartella che non c'è.
3. **Una copia che non riesce** (`mkdir`/`cp` nella cartella del server) ferma `client_shot.sh` con uscita 2 e
   `ERRORE: EXO_SHOT_SERVER_CONFIG: non riesco a copiare …`: a quel punto nulla è ancora partito, ed è un errore
   d'ambiente come gli altri. Il brief dà solo le due righe `mkdir -p` e `cp`.
4. **Controllo del file**: `[ -f ] && [ -r ]` (un file regolare che si legge); una cartella con quel nome è "non
   trovato o non leggibile".
5. **`sameKeysInTheSameOrder` con file di lunghezza diversa**: se uno dei due file finisce prima, il messaggio nomina
   la prima chiave oltre la fine dell'altro e in quale file sta.
6. **`samePlaceholdersInBothLanguages`** salta le posizioni in cui le chiavi differiscono (lo dice già, con la
   chiave, `sameKeysInTheSameOrder`), così un file disallineato non produce un secondo messaggio fuorviante.
7. **T1-F2 in Javadoc**: i nomi delle classi sono scritti `{@code EnergyGameTests}` e così via, come il resto del
   commento (il brief li dà con gli apici del Markdown).
8. **Import di `GameTestKit`**: tolti i quattro del brief, ciascuno dopo una ricerca del nome semplice fuori dai
   commenti (zero usi nel codice). `GameTestGenerator` resta: il codice non lo usa, ma lo nominano due `{@link}` del
   Javadoc, che senza import non si risolverebbero.
9. **Sonda**: una riga di commento sopra la registrazione spostata dice perché sta lì.
10. **README**: oltre a ciò che chiede il brief, `EXO_SHOT_SERVER_CONFIG` nell'elenco delle cause dell'uscita 2
    (sezione "Errori") e la durata di `m3c_rules_shots.sh` in "Quanto dura" (misurati 95-128 s).
11. **Un solo commit**, con l'oggetto dello Step 11 alla lettera.

## Scostamenti dal brief

Nessuno che cambi ciò che il task consegna. Da segnalare:

- Step 8, `regole-uscita.png`: l'avanzamento è 48-51 % secondo la variante ("circa al 50 %" del brief).
- Per confrontare le note di `shellcheck` con quelle di prima ho usato una volta `git stash` e subito
  `git stash pop` sull'albero di lavoro (nessun commit toccato, nessun file perso: `git status` uguale prima e dopo).

## Autorevisione

- Completezza: gli undici passi fatti nell'ordine; le prove negative fatte e disfatte (`git status` pulito sui file
  provati); i due scatti copiati; il numero dei test è quello della tabella (175).
- Larghezza delle righe: le righe nuove stanno in 120 colonne (le più lunghe rimaste nei file toccati c'erano già).
- I commenti nuovi sono in italiano con apostrofi ASCII; messaggi delle asserzioni in inglese.
- Nessun comportamento del mod è cambiato: in `src/main` solo test in mondo, Javadoc, commenti e import.

## Punti aperti (minori, per chi coordina)

- **`m3b_shots.sh` e `m3c_shots.sh` hanno lo stesso difetto di FW-B1 su `log-check.txt`**: dopo un'uscita anticipata
  di `client_shot.sh` con un `log-check.txt` non vuoto rimasto da un'esecuzione fallita, la riga finale dice
  "avvisi del mod fuori elenco" invece di "client_shot.sh e' uscito con 2". Non toccati: il brief limita FW-B1 a
  `server.log` di `m3c_shots.sh`. La correzione sarebbe aggiungere `"$out/log-check.txt"` alla stessa riga `rm -f`.
- `EXO_SHOT_SERVER_CONFIG` che punta proprio a `versions/<variante>/run-shot-server/config/redstone_exoframes.json`:
  lo script toglie quel file prima di copiarlo, quindi si ferma con `ERRORE: … non riesco a copiare …` (uscita 2) e
  il file è perso. Caso improbabile, non documentato.
