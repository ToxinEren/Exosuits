### Task 2: Strumenti dei test: conteggi esatti nel log per variante, client per il movimento

**Files:**
- Modify: `tools/check_log.py`, `tools/log_allowlist.txt`, `tools/run_gametests.sh` (riga `:43` e commento di testa), `tools/client/client_shot.sh` (riga `:471`, opzioni del client, controllo del nome del tasto, commento di testa), `tools/README.md`, `tools/client/README.md`

**Interfaces:**
- Consumes: `python3 -I tools/check_log.py <ambito> <log>...` (`check_log.py:141-153`); formato `ambito | massimo | messaggio | chi la produce` di `tools/log_allowlist.txt` (parser `check_log.py:49-87`, controllo dei conteggi `:131-133`); il passo `@tieni` di `client_shot.sh` (`:100-104`, controllo del nome `[[ "${w[1]}" =~ ^[A-Za-z0-9_]+$ ]]`); `options.txt` scritto a `client_shot.sh:207-222`; la patch accanto a `docs/superpowers/specs/ricognizioni/2026-11-15-m3c-movimento-e-dati.md` (le due righe di `client_shot.sh`).
- Produces:
  - `python3 -I tools/check_log.py gametest|client --variant <variante> <log>...`: `--variant` obbligatorio in seconda posizione (senza, o con meno di un log: uscita 2 e `ERRORE: uso: python3 -I tools/check_log.py gametest|client --variant <variante> <log>...`); la variante deve corrispondere a `[0-9.]+-(fabric|forge|neoforge)` (altrimenti uscita 2), e il loader è la parte dopo il trattino (`1.21.1-neoforge` → `neoforge`).
  - Campo 2 dell'elenco: `N` (N ≥ 1, al più N volte per log, come oggi), `=N` (N ≥ 1, esattamente N volte per log), `fabric=N,forge=M,neoforge=K` (i tre loader, ciascuno una volta, numeri ≥ 0, almeno uno ≥ 1: esattamente il numero del loader della variante). Altro → uscita 2 con `ERRORE: <file>:<riga>: il conteggio si scrive N (al piu' N volte), =N (esattamente N volte) o fabric=N,forge=M,neoforge=K (esattamente, per loader)`. Il campo si controlla su tutte le righe, di ogni ambito.
  - Messaggi nuovi di problema: `<log>: <n> volte la riga ammessa esattamente <m> volte: <riga>` (accanto a quello del massimo, che resta).
  - `TESTREG` = `[\S ]*exoframes-registry-test[0-9]+[/\\]redstone_exoframes_frames\.dat` (un percorso con spazi).
  - Righe dell'elenco diventate esatte: `:26` `=1`, `:29` `=2`, `:31` `=2`, `:33` `=1`, `:36` `=2`, `:38` `=1`, `:40` `=1`, `:45` (gametest) e `:46` (client) `fabric=1,forge=0,neoforge=0`. Nei log del 14 novembre ciascuna compare esattamente così sulle quattro varianti.
  - `run_gametests.sh:43`: `python3 -I tools/check_log.py gametest --variant "$v" "$log"`; `client_shot.sh:471`: `python3 -I "$root/tools/check_log.py" client --variant "$variant" "$out/gradle-client.log" "$out/gradle-server.log"`.
  - `client_shot.sh`: `autoJump:false` nelle opzioni del client (dopo `rawMouseInput:false`); nel nome del tasto di `@tieni` è ammesso `+` (`^[A-Za-z0-9_+]+$`), per i tasti combinati di xdotool (`ctrl+w`).
- Testi: nessuna chiave nuova. `docs/compat.md`: nessuna riga.

- [ ] **Step 1: prove a mano, da preparare** — in una cartella temporanea (`d=$(mktemp -d)`) due log scritti a mano: `$d/fabric.log` con le righe `[12:00:00] [Server thread/INFO] (Minecraft) Starting minecraft server` e `[12:00:01] [Server thread/ERROR] (Minecraft) No data fixer registered for exoframe`; `$d/quiet.log` con la sola riga INFO. Con il `check_log.py` di oggi `python3 -I tools/check_log.py client --variant 1.21.1-fabric "$d/fabric.log"` — Expected: FAIL d'uso (uscita 2: `--variant` non è un ambito né un log).
- [ ] **Step 2: implementare `check_log.py`** — `LOADERS = ("fabric", "forge", "neoforge")`; `read_allowlist(path, scope, loader)` restituisce `[(tipo, n, espressione, riga)]` con tipo `"max"` o `"exact"` e `n` già scelto per il loader; il conteggio si legge così:
  ```python
  def parse_count(most, loader, where):
      if most.isdigit() and int(most) >= 1:
          return "max", int(most)
      if re.fullmatch(r"=[0-9]+", most) and int(most[1:]) >= 1:
          return "exact", int(most[1:])
      if re.fullmatch(r"[a-z]+=[0-9]+(,[a-z]+=[0-9]+)*", most):
          per = [part.split("=") for part in most.split(",")]
          names = [name for name, _ in per]
          if sorted(names) == sorted(LOADERS) and any(int(n) >= 1 for _, n in per):
              return "exact", int(dict(per)[loader])
      fail_usage(f"{where}: il conteggio si scrive N (al piu' N volte), =N (esattamente N volte) o "
                 f"fabric=N,forge=M,neoforge=K (esattamente, per loader)")
  ```
  In `check` il controllo finale: `"max"` → problema se `count > n` (messaggio di oggi); `"exact"` → problema se `count != n` (messaggio nuovo di "Produces"). `main` legge `argv[1]` ambito, `argv[2] == "--variant"`, `argv[3]` variante, `argv[4:]` log. La docstring dice l'uso nuovo, le tre forme del conteggio e che il loader viene dalla variante.
- [ ] **Step 3: elenco** — `tools/log_allowlist.txt`: `TESTREG` come in "Produces"; i conteggi delle nove righe come in "Produces"; commento di testa: riga 3 "anche una riga di qui che compare piu' volte del massimo o un numero di volte diverso da quello esatto"; riga 9 "- conteggio: `N` al piu' N volte in un log, `=N` esattamente N volte, `fabric=N,forge=M,neoforge=K` esattamente, secondo il loader della variante che `check_log.py` riceve con `--variant` (i test girano tutti in un'esecuzione, una volta)"; riga 13 "- chi la produce: il test di un file di gametest/, o l'avvio che la scrive."; il commento sopra `:45`/`:46` dice "esattamente una volta per avvio su Fabric, mai su Forge e NeoForge".
- [ ] **Step 4: chiamanti** — `run_gametests.sh:43` e `client_shot.sh:471` come in "Produces"; commento di testa di `run_gametests.sh` (`:6-8`): "letto da tools/check_log.py con la variante (`--variant`), che decide i conteggi per loader"; `client_shot.sh`: le due righe della patch della ricognizione (`autoJump:false`, `+` nel nome del tasto) e il commento di testa di `@tieni` (`:21`): "(nome di xdotool: v, F5, space; i tasti combinati con +, per esempio ctrl+w)".
- [ ] **Step 5: Run le prove a mano** — Expected, una per una:
  - `python3 -I tools/check_log.py client --variant 1.21.1-fabric "$d/fabric.log"; echo $?` → nessuna riga, `0`;
  - `python3 -I tools/check_log.py client --variant 1.21.1-neoforge "$d/fabric.log"; echo $?` → `…fabric.log: 1 volte la riga ammessa esattamente 0 volte: client | fabric=1,forge=0,neoforge=0 | …`, `1`;
  - `python3 -I tools/check_log.py client --variant 1.20.1-fabric "$d/quiet.log"; echo $?` → `…quiet.log: 0 volte la riga ammessa esattamente 1 volte: …`, `1`;
  - `python3 -I tools/check_log.py client --variant 1.20.1-forge "$d/quiet.log"; echo $?` → nessuna riga, `0`;
  - `python3 -I tools/check_log.py client "$d/quiet.log"; echo $?` → `ERRORE: uso: …`, `2`; `python3 -I tools/check_log.py client --variant 1.21.1-quilt "$d/quiet.log"; echo $?` → `2`;
  - una riga scritta male in una copia dell'elenco non si prova (il percorso dell'elenco è fisso): basta leggere `parse_count`.
- [ ] **Step 6: Run** `bash tools/run_gametests.sh` — Expected: quattro righe `OK <variante>: 96 test obbligatori passati, nessun avviso fuori elenco` (ogni riga esatta trovata il numero giusto di volte). Poi, sul log vero: `log=versions/1.21.1-neoforge/build/gametest/server.log`:
  - `sed '0,/Ignoring unknown tier id in mod NBT data: mythic/{/Ignoring unknown tier id in mod NBT data: mythic/d}' "$log" > "$d/missing.log"; python3 -I tools/check_log.py gametest --variant 1.21.1-neoforge "$d/missing.log"; echo $?` → `…missing.log: 0 volte la riga ammessa esattamente 1 volte: gametest | =1 | Ignoring unknown tier id …`, `1`;
  - `sed 's#/exoframes-registry-test#/cartella con spazi/exoframes-registry-test#g' "$log" > "$d/spaces.log"; python3 -I tools/check_log.py gametest --variant 1.21.1-neoforge "$d/spaces.log"; echo $?` → nessuna riga, `0` (con il `\S*` di prima le due righe ERROR del registro, `:38` e `:40` dell'elenco, risulterebbero non ammesse).
- [ ] **Step 7: client vero** — `EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh 1.21.1-fabric /tmp/scatti/t2 "@tieni ctrl+w 1"` — Expected: uscita 0, `/tmp/scatti/t2/log-check.txt` vuoto (la riga `No data fixer registered for exoframe` esattamente una volta nel log del client e una in quello del server), `grep -c '^autoJump:false$' versions/1.21.1-fabric/run-shot-client/options.txt` → `1`.
- [ ] **Step 8: documenti** — `tools/README.md`: riga di `run_gametests.sh` ("conta le righe `add(tests,` di tutti i file di `gametest/`"), riga di `check_log.py` (`python3 -I tools/check_log.py gametest|client --variant <variante> <log>...`), paragrafo "Avvisi voluti" (le tre forme del conteggio e perché esatto: un avviso voluto che smette di comparire si nota). `tools/client/README.md`: riga 57 (nomi di xdotool, anche combinati con `+`: `ctrl+w`), paragrafo di `client_shot.sh` (riga 32: "salto automatico spento nelle opzioni del client (`autoJump:false`), così un gradino non diventa un salto"), riga 44 (`log-check.txt` scritto da `check_log.py` con la variante), riga 115 ("esattamente una volta per log, mai su Forge e NeoForge").
- [ ] **Step 9: verifiche di fine task** — build e `collectJar` delle quattro varianti (comando di Global Constraints) → `BUILD SUCCESSFUL`; `bash tools/check_sources.sh` → `OK sources`; `bash tools/check_jars.sh` → `OK jars`; `bash tools/run_gametests.sh` → già fatto allo Step 6.
- [ ] **Step 10: commit** `test: conteggi esatti nel log per variante, client con salto automatico spento e tasti combinati`, con nel corpo le uscite delle prove degli Step 5-7.

