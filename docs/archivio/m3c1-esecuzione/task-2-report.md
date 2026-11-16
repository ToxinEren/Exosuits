# Task 2 - Strumenti dei test: conteggi esatti nel log per variante, client per il movimento

Stato: DONE. BASE b5cb0114204e52e3d83cecb01048dae43cb2e20c, HEAD f754b0e3c468d94f0d8c6844615ff93f635c498a.
Commit: `f754b0e test: conteggi esatti nel log per variante, client con salto automatico spento e tasti combinati`.

## Che cosa e' stato fatto

Un primo esecutore aveva lasciato nel working tree, non committati, gli Step 1-5 e 8 (vedi la riga
`Task 2: BLOCKED` del ledger). Li ho riletti contro il brief, uno per uno, e tenuti; ho rifatto le prove degli Step 1 e
5, poi fatto gli Step 6, 7, 9 e 10.

- `tools/check_log.py`: uso `gametest|client --variant <variante> <log>...` (`--variant` obbligatorio in seconda
  posizione, almeno un log, altrimenti uscita 2 con `ERRORE: uso: ...`); variante `[0-9.]+-(fabric|forge|neoforge)`
  (altrimenti uscita 2), loader = parte dopo il trattino; `LOADERS`, `parse_count` come nel brief;
  `read_allowlist(path, scope, loader)` restituisce `(tipo, n, espressione, riga)` e controlla il conteggio su tutte le
  righe, di ogni ambito; in `check` `"max"` -> problema se `count > n` (messaggio di prima), `"exact"` -> problema se
  `count != n` (`<log>: <n> volte la riga ammessa esattamente <m> volte: <riga>`); docstring con l'uso nuovo, le tre
  forme e il loader dalla variante.
- `tools/log_allowlist.txt`: `TESTREG` = `[\S ]*exoframes-registry-test[0-9]+[/\\]redstone_exoframes_frames\.dat`; le
  sette righe dei test a `=1`/`=2` come nel brief e le due righe di Fabric a `fabric=1,forge=0,neoforge=0`; commento di
  testa (riga 3, campo conteggio, "chi la produce: il test di un file di gametest/, o l'avvio che la scrive.") e
  commento sopra le righe di Fabric ("esattamente una volta per avvio su Fabric, mai su Forge e NeoForge").
- `tools/run_gametests.sh`: chiamata con `--variant "$v"` e commento di testa.
- `tools/client/client_shot.sh`: `autoJump:false` dopo `rawMouseInput:false` (e nel commento delle opzioni), `+`
  ammesso nel nome del tasto di `@tieni` (le due righe della patch della ricognizione), commento di testa di `@tieni`,
  chiamata di `check_log.py` con `--variant "$variant"`.
- `tools/README.md`, `tools/client/README.md`: le righe e i paragrafi dello Step 8.

## Prove ed esiti

### Step 1 (RED, con il `check_log.py` di BASE, estratto con `git show`)

```
$ python3 -I tools/check_log.py client --variant 1.21.1-fabric "$d/fabric.log"
ERRORE: log non trovato: --variant
uscita 2
```
Atteso: fallimento d'uso, perche' il `check_log.py` di prima prende `--variant` per un log.

### Step 5 (GREEN, prove a mano)

```
$ check_log.py client --variant 1.21.1-fabric $d/fabric.log
uscita 0
$ check_log.py client --variant 1.21.1-neoforge $d/fabric.log
$d/fabric.log: 1 volte la riga ammessa esattamente 0 volte: client | fabric=1,forge=0,neoforge=0 | No data fixer registered for exoframe | avvio del server e del client su Fabric (registrazione dell'entita')
uscita 1
$ check_log.py client --variant 1.20.1-fabric $d/quiet.log
$d/quiet.log: 0 volte la riga ammessa esattamente 1 volte: client | fabric=1,forge=0,neoforge=0 | No data fixer registered for exoframe | avvio del server e del client su Fabric (registrazione dell'entita')
uscita 1
$ check_log.py client --variant 1.20.1-forge $d/quiet.log
uscita 0
$ check_log.py client $d/quiet.log
ERRORE: uso: python3 -I tools/check_log.py gametest|client --variant <variante> <log>...
uscita 2
$ check_log.py client --variant 1.21.1-quilt $d/quiet.log
ERRORE: variante '1.21.1-quilt' non valida: si scrive <versione>-<loader>, con loader fabric, forge o neoforge (per esempio 1.21.1-neoforge)
uscita 2
$ check_log.py client --variant 1.21.1-fabric          (nessun log)
ERRORE: uso: python3 -I tools/check_log.py gametest|client --variant <variante> <log>...
uscita 2
```

### Step 6 (test in gioco e log vero)

`EXO_ACCEPT_EULA=true bash tools/run_gametests.sh` -> uscita 0:
```
OK 1.20.1-forge: 96 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 96 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 96 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 96 test obbligatori passati, nessun avviso fuori elenco
```
Sul log di 1.21.1-neoforge (`versions/1.21.1-neoforge/build/gametest/server.log`):
```
$ missing.log (prima riga "mythic" tolta con sed)
$d/missing.log: 0 volte la riga ammessa esattamente 1 volte: gametest | =1 | Ignoring unknown tier id in mod NBT data: mythic | debugWearHandlesTorsoWithoutValidSummary
uscita 1
$ spaces.log (cartella dei test sotto "/tmp/cartella con spazi/", due righe ERROR del registro)
uscita 0
$ spaces.log con il check_log.py e l'elenco di BASE (controprova del vecchio \S*)
$d/spaces.log:348: riga non ammessa: [17:47:01] [Server thread/ERROR] [Redstone Exoframes/]: Frame registry file /tmp/cartella con spazi/exoframes-registry-test.../redstone_exoframes_f...
$d/spaces.log:415: riga non ammessa: [17:47:01] [Server thread/ERROR] [Redstone Exoframes/]: Frame registry file /tmp/cartella con spazi/exoframes-registry-test.../redstone_exoframes_f...
uscita 1
```

### Step 7 (client vero)

`EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh 1.21.1-fabric /tmp/scatti/t2 "@tieni ctrl+w 1"` -> uscita 0:
```
[44s] passo eseguito: @tieni ctrl+w 1
[68s] log: nessun avviso o errore del mod fuori dall'elenco degli avvisi voluti
[68s] fatto in 68 s, chiusura
```
`/tmp/scatti/t2/log-check.txt`: 0 byte. `No data fixer registered for exoframe`: 1 volta in `gradle-client.log`, 1 in
`gradle-server.log`. `grep -c '^autoJump:false$' versions/1.21.1-fabric/run-shot-client/options.txt` -> `1`.

### Step 9 (verifiche di fine task)

```
$ ./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar
BUILD SUCCESSFUL in 2s
$ bash tools/check_sources.sh
OK sources
$ bash tools/check_jars.sh
OK jars
```
(La build e' durata 2 s perche' `run_gametests.sh` aveva appena compilato le quattro varianti e il task non tocca
codice del mod.) `git diff --check` pulito. Nessun server, client o Gradle in esecuzione alla fine (restano solo
demoni Gradle inattivi).

## File cambiati

`tools/check_log.py`, `tools/log_allowlist.txt`, `tools/run_gametests.sh`, `tools/client/client_shot.sh`,
`tools/README.md`, `tools/client/README.md`.

## Scelte

- Commento di testa dell'elenco, campo conteggio: le tre forme sono scritte fra virgolette doppie (`"N"`, `"=N"`,
  `"fabric=N,forge=M,neoforge=K"`) invece che fra apici inversi come nel brief: e' un file di testo, non Markdown, e il
  resto del commento cita gia' cosi' (`".*"`, `"= NOME espressione"`). Il testo e' quello del brief.
- Variante scritta male: messaggio di uscita 2 con la forma attesa e un esempio (`variante '<x>' non valida: si scrive
  <versione>-<loader>, ...`); il brief chiedeva solo l'uscita 2.
- Il messaggio d'uso sta in una costante `USAGE`, usata per tutti i casi d'uso sbagliato (ambito, `--variant` mancante,
  nessun log).
- Righe dell'elenco: quella del commento sopra le righe di Fabric aggiunge "sul server e sul client" (gia' detto nel
  commento di prima), dopo il testo del brief.

## Scostamenti dal brief

- `parse_count`: la prima forma usa `re.fullmatch(r"[0-9]+", most)` invece di `most.isdigit()`. `str.isdigit()` e'
  vero anche per cifre non ASCII (per esempio `²`), su cui `int()` solleva `ValueError` invece di dare l'uscita 2 con
  il messaggio del brief; il comportamento sulle forme valide e' identico.
- Numeri di riga dell'elenco: il commento di testa ora e' piu' lungo di tre righe, quindi le righe che il brief cita come
  `:26`...`:46` (numerazione di BASE) stanno tre righe piu' in basso; i conteggi sono quelli del brief, riga per riga.

## Autorevisione

Riletto tutto il diff contro "Produces" e gli Step 2-4 e 8: uso, controllo della variante, tre forme del conteggio
(con `=0` e `fabric=0,forge=0,neoforge=0` rifiutati, un loader mancante o ripetuto rifiutato), messaggi, `TESTREG`,
conteggi delle nove righe, chiamanti, `autoJump:false`, `+` nel tasto, documenti. Una riga scritta male nell'elenco non
si prova a mano (percorso fisso, come dice lo Step 5): letta in `parse_count`. Nessun problema trovato.

## Problemi o dubbi

Nessuno.
