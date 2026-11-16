# Task 24 — Client vero, testi e documenti della 3c-1, verifica finale

BASE `ac3501f65f13f175522335654ebe5e7c1642aa6c`, HEAD `d67e8b66b0ea797e05ecf1358e35606b24dd97eb`.
Commit: `d67e8b6 test: prova della 3c-1 nel client vero; documenti`.

## Che cosa ho fatto

- **Step 1.** `tools/client/m3c_shots.sh` (eseguibile), come in "Produces": formati di cella e pezzo per versione, `P`, `at`, `beyond`, `energy` verbatim, piu' un aiuto `set_energy` (una riga `execute as $u run exoframe debug energy N`); i passi 1-11 nell'ordine del brief, con gli stessi comandi, attese, tasti e soglie; riga `=== $variant m3c` prima e `OK`/`FAIL` dopo come `m3b_shots.sh`. Commento di testa: uso, che cosa controlla ogni passo con la soglia, scatti, durata misurata (116-127 s). `bash -n` senza uscita.
- **Step 2.** Client vero sulle quattro varianti, una alla volta (cartelle nel mio scratchpad). Tutte e quattro `OK`, uscita 0, `log-check.txt` di 0 byte, `grep -c 'moved wrongly' server.log` = 0 (gli avvisi `moved too quickly` ci sono, 3 su NeoForge: l'artefatto noto).
- **Step 3.** Dieci scatti in `docs/test-in-gioco/m3c-screenshot/` con `convert -strip -define png:exclude-chunks=date,time`: per 1.20.1 `1.20.1-forge-riga-energia`, `1.20.1-forge-barre-inventario`, `1.20.1-fabric-cambio-cella-tenuto`, `1.20.1-fabric-vasca-spazio`, `1.20.1-fabric-riga-energia-esaurita`; per 1.21.1 `1.21.1-neoforge-riga-energia`, `1.21.1-neoforge-barre-inventario`, `1.21.1-fabric-cambio-cella-tenuto`, `1.21.1-fabric-vasca-spazio`, `1.21.1-fabric-riga-energia-bassa` (1,2 MB in tutto). Il controllo dei blocchi PNG del brief: nessuna uscita, uscita 0.
- **Step 4.** Server dedicato sulle quattro varianti, uno alla volta (`./gradlew --console=plain :<v>:runServer` con lo stdin da una fifo, atteso `Done (`, poi `stop`).
- **Step 5.** Documenti (sotto).
- **Step 6.** Verifiche di fine task (sotto). **Step 7.** Commit.

## Prove (Step 2): posizioni da `rcon.log` e durate

| Passo (soglia) | 1.21.1-neoforge | 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric |
|---|---|---|---|---|
| 1 affondare (entro 0,1 da 12,5 −60 0,5) | 12,5 −60 0,5 | uguale | uguale | uguale |
| 2 Spazio sul fondo (entro 0,1) | 12,5 −60 0,5 | uguale | uguale | uguale |
| 3 avanti sul fondo (oltre 6) | Z 7,72 (7,22) | Z 7,72 (7,22) | Z 7,62 (7,12) | Z 7,54 (7,04) |
| 4 acqua profonda 1 (entro 0,1) | 22,5 −61 0,5 | uguale | uguale | uguale |
| 4 uscita dalla sponda (Y −60, entro 1,2 da X 28) | X 27,70 Y −60 | X 27,97 Y −60 | X 27,97 Y −60 | X 27,97 Y −60 |
| 5 corrente (entro 0,05) | 42,5 −60 −19,5 | uguale | uguale | uguale |
| 6 scalino (Y −59, entro 1,5 da −4,5 −59 6,8) | Z 5,88 | Z 5,88 | Z 5,76 | Z 5,88 |
| 7 corsa a 999 RE (fra 6 e 8,2) | 7,22 | 7,22 | 7,22 | 7,04 |
| 7 corsa a 5 000 RE (oltre 8,6) | 9,38 | 9,38 | 9,38 | 9,38 |
| 8 carica prima / dopo 5 s di cammino | 4955 / 4912 | 4955 / 4913 | 4956 / 4914 | 4956 / 4913 |
| 10 carica dopo il cambio (4900..5000) / celle nell'inventario | 4996 / 1 | 4995 / 1 | 4996 / 1 | 4996 / 1 |
| durata (dello script / totale con la chiusura) | 120 s / 122 s | 127 s / 129 s | 116 s / 120 s | 117 s / 119 s |

Righe finali:

```
OK 1.21.1-neoforge: scatti in .../m3c/1.21.1-neoforge
OK 1.20.1-forge: scatti in .../m3c/1.20.1-forge
OK 1.20.1-fabric: scatti in .../m3c/1.20.1-fabric
OK 1.21.1-fabric: scatti in .../m3c/1.21.1-fabric
```

Scatti guardati uno per uno su tutte e quattro le varianti (fogli di contatto e ritagli ingranditi):

- `vasca-spazio`: sott'acqua, sul fondo della vasca di vetro, prima persona normale (nessuna posa a nuoto: la visuale e' agli occhi della tuta, bolle d'aria sulla barra), riga verde "Energy 9987 / 10000 RE (99%)".
- `riga-energia`: verde, in alto a sinistra (non in basso a destra: vedi Scostamenti), "Energy 4912/4913/4914 / 10000 RE (49%)".
- `riga-energia-bassa`: gialla, "Energy 496-498 / 10000 RE (4%)", e nella barra delle azioni "Energy low: no sprinting".
- `riga-energia-esaurita`: rossa, "Energy 0 / 10000 RE (0%)", "Energy empty: the exoframe barely moves"; la barra dell'armatura scende (velocita' e armatura della tuta a energia zero).
- `barre-inventario`: inventario aperto; nella barra degli oggetti la cella con la barra rossa a meta' (5 000 / 10 000) e l'elmo Rottame con la barra di durata gialla a meta' (90 / 180), uguali sulle quattro varianti (ritagli ingranditi).
- `cambio-cella-tenuto`: "Hold to swap the cell... 50%" sulla barra delle azioni, riga gialla a ~996 RE.
- `cambio-cella-fatto`: riga verde "Energy 4995/4996 / 10000 RE (49%)"; nello slot 0 la cella vecchia con la barra rossa quasi vuota (1 000 RE).
- `back.png` (1.20.1-fabric, 1.21.1-fabric): la tuta da dietro con vasca e scalino sullo sfondo.

## Prove (Step 4): server dedicato

| Variante | `Done (` dopo | Gradle | `check_log.py client` | `No data fixer…` | `InvalidInjection`/`Mixin apply` |
|---|---|---|---|---|---|
| 1.20.1-forge | 16 s (`Done (2.943s)`) | 0 | nessuna uscita, 0 | 0 | 0 |
| 1.20.1-fabric | 13 s (`Done (2.719s)`) | 0 | nessuna uscita, 0 | 1 | 0 |
| 1.21.1-fabric | 11 s (`Done (0.556s)`) | 0 | nessuna uscita, 0 | 1 | 0 |
| 1.21.1-neoforge | 12 s (`Done (0.394s)`) | 0 | nessuna uscita, 0 | 0 | 0 |

## Prove (Step 6): verifiche di fine task

- Build e `collectJar` delle quattro varianti: `BUILD SUCCESSFUL in 1s` (52 task aggiornati: il task non tocca codice).
- `bash tools/check_sources.sh` → `OK sources`
- `bash tools/check_jars.sh` → `OK jars`
- `bash tools/check_art.sh` → `OK art`
- `bash tools/run_gametests.sh` (114 s):
  ```
  OK 1.20.1-forge: 172 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 172 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 172 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 172 test obbligatori passati, nessun avviso fuori elenco
  ```
  (172 = numero del Task 23 e 24; nessun ripiego dei Task 11-12 nel codice: 172 righe `add(tests,`.)
- Chiavi delle lingue: nessuna uscita (stesse chiavi, stesso ordine).
- `grep -rn 'isCreative()' …frame …core …client | grep -vE <commenti>`: una sola riga, `frame/FramePermissions.java:30`, del commit `1bb938f` della 3b (gia' su `main`); nessuna nei file della 3c-1.
- Nessun processo di gioco, server, client o Xvfb rimasto aperto.

## Documenti (Step 5)

- `tools/client/README.md`: riga d'uso di `m3c_shots.sh`; paragrafo con la sequenza, una tabella passo → che cosa controlla con le soglie (ricognizione × 0,85, `frame.baseSpeedPenalty` dal Task 14) e i valori misurati, `/exoframe debug energy` per corsa e riga dell'energia, `moved wrongly`, che cosa non prova (doppio W e scatto a interruttore, suoni, suggerimenti al passaggio del mouse, uscita dall'acqua a cella vuota: checklist della 3c-2); durata misurata in "Quanto dura".
- `README.md`: "Stato" (3c-1 fatta, sul branch `m3c-giocarci`; 3c-2 con caricatore e ricette e' la prossima; tolta la frase ormai falsa "3b sul branch `m3b-entrare-uscire` finche' non viene unita": e' su `main` a `2e35aa6`); riga degli scatti con `m3c_shots.sh`; paragrafo dei test in gioco con "oggi 172 test"; "Documenti" con `m3c-screenshot/` e "M3c-1" fra i piani.
- `docs/compat.md`, riletto contro il codice per ogni cucitura della 3c-1 (`FrameIndicators` e `of`, `changeWorn`, `clientIndicators`, `onSynced`, formato di `frame_state_sync` contro `FrameStateSyncPacket.CODEC`, `client_rules` e `cell_swap` in `Net`, `FrameAttributes` idempotente con `frame_step` e `localStep` chiamato da `SuitSize.tick` solo per il giocatore locale, `damages_helmet` in `DamageClassifierImpl`, gli aiuti di `GameTests` uno per uno contro l'interfaccia, `CompatItem`, `addHudRenderer`, `configDir()` in `RedstoneExoframes.init()`, i due mixin). C'erano tutte; corretto dove non diceva il vero:
  - `Net`: la frase finale diceva che "la meta' client del pacchetto e il tasto" erano provati con `m3b_shots.sh` (letta come se valesse per `client_rules`, [T12-F1]): ora `frame_exit` con `m3b_shots.sh`, `cell_swap` e indicatori con `m3c_shots.sh`, e `client_rules` detto com'e' (arriva senza avvisi, ma con i predefiniti sui due lati il client vero non distingue il valore ricevuto: lo provano `ClientRulesTest` e `ClientRulesPacketTest`).
  - `FrameStateAccess`: "(un tick fermo non manda nulla)" → "(un tick fermo in cui il consumo resta sotto 1 RE non manda nulla)" ([T14-F2]); la frase di `clear` riportata accanto agli altri tre metodi di scrittura invece che dopo `onSynced` ([T11-F1]); `FrameIndicators.of(RedstoneExoframes.rules().evaluate(stato))`.
  - `GameTests`, "Condizione": aggiunto `readRegistry` (costruttore di `DimensionDataStorage` per versione, attesa del lavoratore di I/O su NeoForge 1.21.1) ([T5-F3]).
  - `CompatItem`: diceva che il suggerimento al passaggio del mouse lo prova il client vero; lo strumento non porta il puntatore sugli slot: ora lo prova la checklist della 3c-2, e le barre le mostra `barre-inventario`.
  - Mixin, "Verifica": aggiunta la prova con i gestori veri (`m3c_shots.sh` sulle quattro varianti, server dedicato) ([T19-F3]).
  - "Limiti noti": il ripristino dei moduli diventa "Milestone 4" (la config ora si legge da file; il ripristino senza i controlli dell'installatore resta per M4, design §12.14); il pezzo con livello sconosciuto diventa "Milestone 4/5" (prima parte che aggiunge livelli, slot o moduli, §12.14) al posto di "riporti per la 3c"; la capacita' della cella era gia' chiusa dal Task 20 (invariata); "Scritture a ogni tick" chiusa nella 3c-1 con la regola di §12.4.
- Design: controllati contro il codice §12.14 (`WornState.changed` a cinque argomenti, `frame/WornState.java:41`; `DebugEnergy.Outcome.REFUSED`), §12.10 (`CellSwapKey` conta solo con `!ExitKey.keyDown()`, e con il conteggio fermo `ExitHold` lo azzera) e §12.9 (`<mappa>.<CHIAVE>: missing, using default <valore>` in `ConfigValidation`): li rispettano, nessuna modifica. Due correzioni dove l'esecuzione contraddice il testo (scelte del costruttore, sotto).

## Scelte

- **Design §12.13, riga dell'energia.** Il testo dava l'esempio di una riga sola con "· torso critico"; il Task 22 (ruling del coordinatore) l'ha portata in alto a sinistra su due righe, la seconda con pezzo e condizione colorata. Ho riscritto quella frase dicendo com'e' fatta e perche', con il rimando al Task 22 ([T22-R1-B2]). La regola del pezzo messo peggio e il resto del paragrafo non cambiano.
- **Design §12.15, test dell'acqua.** "`doTick()` una volta" → due chiamate, con il motivo e il rimando al Task 19 (ruling su [T19-F1]).
- **Scatti di riferimento.** Dieci invece dei quattro per versione minimi: i quattro chiesti per ciascuna versione divisi fra i due loader, piu' `riga-energia-esaurita` (1.20.1) e `riga-energia-bassa` (1.21.1), perche' i colori giallo e rosso li mostra solo il client vero ([T22-F3]).
- **`set_energy`** nello script: un aiuto in piu' accanto a quelli del brief, per non ripetere sei volte la stessa riga; i comandi mandati sono quelli del brief.
- **README, stato della 3b.** Tolta la frase "sul branch `m3b-entrare-uscire` finche' non viene unita a `main`" (la 3b e' su `main` a `2e35aa6`); la stessa forma l'ho usata per la 3c-1 su `m3c-giocarci`.

## Scostamenti dal brief

- **Riga dell'energia "in basso a destra"** (Step 2): per il ruling del Task 22 la riga e' in alto a sinistra; ho controllato li'.
- **`riga-energia-bassa` "5 %"**: lo scatto mostra 496-498 RE e "(4%)": fra `debug energy 500` e lo scatto passa un secondo di consumo da fermi (2 RE/s), e la percentuale e' per difetto. E' il comportamento giusto; nel commento di testa ho scritto "sotto il 5 %".
- **README "riga 33"**: la riga degli scatti oggi e' la 35 (il Task 8 ha aggiunto il paragrafo della configurazione); ho modificato quella.
- **compat.md "Limiti noti" `:273-278`**: oggi sono le righe 291-297; ho seguito il contenuto delle voci (`:273` → ripristino dei moduli, `:274` → pezzo con livello sconosciuto, `:275` → capacita' della cella, `:278` → scritture a ogni tick).
- Il brief non chiedeva di toccare i punti differiti [T5-F3], [T11-F1], [T12-F1], [T14-F2], [T19-F3] di `docs/compat.md`; li ho corretti perche' la rilettura di Step 5 chiede che ogni voce "dica il vero" e cadevano nelle voci rilette.

## File cambiati

- nuovi: `tools/client/m3c_shots.sh`, `docs/test-in-gioco/m3c-screenshot/` (10 PNG)
- modificati: `tools/client/README.md`, `README.md`, `docs/compat.md`, `docs/superpowers/specs/2026-11-13-m3-telaio-design.md`

## Autorevisione

- Lo script segue l'ordine e i valori del brief; la riga del pezzo per 1.21.1 supera i 120 caratteri (132) perche' e' la stringa del brief verbatim; il resto sta sotto i 120.
- Ogni numero scritto nei documenti (7,0-7,2; 27,70-27,97; 5,76-5,88; 7,04-7,22; 9,38; 116-127 s; 172) viene dalle esecuzioni sopra.
- TDD non applicabile (nessun codice del mod: script di prova e documenti).

## Problemi o dubbi

- Nessuno bloccante. Restano, come da brief, alla checklist della 3c-2: suggerimenti al passaggio del mouse, doppio W e scatto a interruttore, suoni, uscita dall'acqua a cella vuota, jar di produzione.
- Lo scatto `vasca-spazio` e' in prima persona: "niente posa a nuoto" si vede dalla visuale agli occhi della tuta sul fondo e dalla posizione invariata, non da un'inquadratura del corpo.
