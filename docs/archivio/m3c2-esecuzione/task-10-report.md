# Task 10 — Client vero, checklist per il PC, documenti, verifica finale della 3c

Stato: **DONE_WITH_CONCERNS** (lavoro completo e verificato; le note in "Problemi e dubbi" sono per chi coordina).
BASE `dd696e0f1b525a6108d0e52db7ccf78b80c59b38`, HEAD `9107aaef9540c5f5a0b03ac90bc54a3ca81f6930`.

Commit:

- `0026c52` docs: checklist completa della 3c, prova del caricatore nel client vero, documenti
- `9107aae` docs: al vecchio percorso della checklist 3c-1 un rimando a m3c-giocarci.md

Nessun file Java, nessun dato e nessuna risorsa del mod cambia in questo task: script, checklist, scatti, documenti.

## Che cosa ho fatto

1. **`tools/client/m3c2_shots.sh`** (eseguibile, 100755): la sequenza di "Produces", parola per parola, in una esecuzione
   di `client_shot.sh` in sopravvivenza; commento di testa come `m3c_shots.sh` (gli otto passi, che cosa non prova, la
   durata misurata); riga finale `OK`/`FAIL` e `rm -f log-check.txt` prima dell'esecuzione come `m3c_rules_shots.sh`.
   `<cella>` per variante come nel Task 5 Step 6.
2. **Client vero sulle quattro varianti** (Step 2), una esecuzione alla volta: `m3c2_shots.sh` e `m3c_shots.sh`, e in
   piu' `m3c_rules_shots.sh` (vedi "Scelte"). Dodici esecuzioni, tutte `OK`.
3. **Quattro scatti di riferimento** in `docs/test-in-gioco/m3c-screenshot/`, senza metadati (Step 3).
4. **Server dedicato** avviato e fermato con `stop` sulle quattro varianti, mondo cancellato prima di ogni avvio (Step 4).
5. **`docs/test-in-gioco/m3c-giocarci.md`**: `git mv` della checklist della 3c-1 e poi le aggiunte (Step 5). Al vecchio
   percorso un rimando di poche righe (Ruling del ledger), in un secondo commit.
6. **Documenti** (Step 6): `README.md`, `tools/README.md`, `tools/client/README.md`, `docs/compat.md`, le note di testa
   di `m3a-vedere-il-telaio.md` e `m3b-entrare-e-uscire.md`, il design (solo le correzioni elencate dal brief).
7. **Verifica finale** del "fatta quando" della 3c (Step 7), tabella sotto.

### La checklist, in breve

- 112 passi numerati, 23 sezioni (0-22). I passi 1-75 hanno numero e testo di prima: il confronto riga per riga con
  `git show HEAD:docs/test-in-gioco/m3c1-consumare.md` da' una sola differenza, il passo 1 (da dove si prendono i jar).
  Le sezioni 0-15 tengono numero e titolo; la sezione 15 non ha cambi di testo (solo il passo nuovo 112 in coda).
- Sezioni nuove: 16 Ricette (76-79), 17 Caricatore (80-93), 18 Tramogge (94-97), 19 Rottura e salvataggio (98-100),
  20 La partita completa (101-107). "Cosa riportare" e' la 21, le domande la 22.
- Passi nuovi in coda a sezioni esistenti, numerati dopo l'ultimo della sezione 20: 108 (sezione 1: il pacchetto di
  dati su 1.21.1), 109 (sezione 7: incantesimi e regole dei mob), 110 e 111 (sezione 12: il passo fuori su un gradino
  basso e lontano dai pericoli), 112 (sezione 15: consumi e movimento su un server vero).
- Titolo e introduzione: "parte 3c (giocarci)", i jar di dopo l'unione della 3c-2, il paragrafo per chi ha gia' fatto i
  passi 1-75 (rifare 1, 2, 5, 7, poi le sezioni 16-20 e i passi 108-112).
- "Cosa c'e' da vedere": ricette (tabella delle forme, sblocco al primo lingotto di rame, la cella esce vuota, i
  materiali per una tuta completa), caricatore (che cosa fa, tabella dei casi del clic, tempi), tramogge, i quattro
  testi di stato nella tabella dei messaggi, il suggerimento a cinque righe.
- "Cosa non c'e' ancora": tolti caricatore e ricette; restano riparazione, animazione del cambio, tuta senza cella
  (T22-F3: la riga "Energia: nessuna cella" resta non raggiungibile, e la checklist continua a dirlo), HUD vero,
  incantesimi e regole dei mob.
- "Gia' verificato nella sessione cloud": 208 test, i tre script sulle quattro varianti, la tabella dei controlli
  nuovi, l'elenco di cio' che il cloud non prova; "Sono le sezioni 1-20".
- Nessun passo aspetta "silenzio in chat" con il registro in sola lettura (cercato: nessuno lo faceva); la tabella delle
  risposte di `/exoframe debug energy` ora dice che "Non indossi un telaio" e' anche la risposta in quel caso.
- Quattro domande nuove (9 quarzo, 10 garanzia del caricatore, 11 chiunque prende una cella, 12 tempi di carica).

Voci dell'elenco "Checklist per il PC" di design §12.15 e il loro passo:

| Voce di §12.15 | Passo |
|---|---|
| suoni sentiti | 64; caricatore: 83, 85, 86 |
| consumo camminando su un server con latenza vera | **112 (nuovo: mancava)** |
| doppio W e scatto a interruttore spenti sotto il 10 % | 25 |
| un secondo giocatore vede la tuta camminare sott'acqua | 75 |
| i quattro agganci applicati nei jar di produzione | 7 (log), 46-52 (usura), 34-36 e 41 (acqua, corrente), 37 (nuoto) |
| suggerimenti degli oggetti al passaggio del mouse | 58-60, 80 |
| campo visivo che si stringe | 26 |
| tramogge in catena | 97 |
| sensazione dei consumi e del tempo di carica | 17-20, 91, 92, 103-107, domande 8 e 12 |
| salto automatico che paga i gradini | 21 |
| incantesimi d'armatura e regole dei mob | **109 (nuovo: mancava)** |
| pacchetto di dati di 1.21.1 "fatto per una versione piu' vecchia" | **108 (nuovo: mancava)** |
| colonne di bolle, scatto sul fondo, controllo in aria | 44, 37, 45 |

## Che cosa ho provato, e gli esiti

### Step 1

`bash -n tools/client/m3c2_shots.sh`: nessuna uscita.

### Step 2 — client vero, una esecuzione alla volta (`EXO_ACCEPT_EULA=true bash tools/client/<script> <variante> <cartella>`)

| Variante | `m3c2_shots.sh` | `m3c_shots.sh` | `m3c_rules_shots.sh` |
|---|---|---|---|
| 1.20.1-forge | OK, 100 s | OK, 126 s | OK, 113 s |
| 1.20.1-fabric | OK, 80 s | OK, 113 s | OK, 90 s |
| 1.21.1-fabric | OK, 80 s | OK, 117 s | OK, 91 s |
| 1.21.1-neoforge | OK, 84 s | OK, 119 s | OK, 94 s |

Per ciascuna delle dodici: ultima riga `OK <variante>: scatti in <cartella>`, uscita 0, `log-check.txt` di 0 byte; in
`server.log` delle esecuzioni di `m3c_shots.sh` nessun `moved wrongly`. Ultime righe di `m3c2_shots.sh` su
`1.20.1-forge`:

```
[73s] comando eseguito: execute if block 0 -60 1 minecraft:air
[73s] comando eseguito: execute if entity @e[type=minecraft:item,nbt={Item:{id:"redstone_exoframes:redstone_cell"}}]
[73s] comando eseguito: execute if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:redstone"}}]
[73s] comando eseguito: gamemode survival Exo
[73s] comando eseguito: tp Exo 0.5 -60 -5.5 0 15
[97s] log: nessun avviso o errore del mod fuori dall'elenco degli avvisi voluti
[97s] fatto in 97 s, chiusura
OK 1.20.1-forge: scatti in <tmp>/shots/c2-forge
```

Carica letta due volte a 4 s (`rcon.log`): 3 120 e 3 322 RE su `1.20.1-forge`, 3 117 e 3 320 sulle altre tre.

Scatti guardati (i tre di `m3c2_shots.sh` sulle quattro varianti; uguali fra loro):

- `caricatore-posato`: il caricatore sull'erba davanti al giocatore, il fronte con l'alloggiamento vuoto e scuro verso
  di lui, sopra e fianchi con le loro texture (nessuna faccia viola e nera), mano vuota; in alto a destra l'avviso
  `New Recipes Unlocked!` (e' lo sblocco del passo 1).
- `caricatore-acceso`: il fronte acceso (tre tacche rosse) e sopra la barra degli oggetti `Charger: charging 30%`.
- `caricatore-svuotato`: l'alloggiamento vuoto, `Charger: no cell`, la cella in mano e nel primo posto della barra.
- In piu', dei quattro `regole-barra` di `m3c_rules_shots.sh` ho ingrandito lo slot della cella: barra rossa lunga un
  quarto sulle quattro varianti.

Gli scatti di `m3c_shots.sh` non li ho riguardati uno per uno: di quelle quattro esecuzioni riporto l'esito dei
controlli (`OK`, nessun `moved wrongly`), non le immagini.

Prima esecuzione di `m3c2_shots.sh` riuscita al primo colpo: nessuna correzione ai passi del brief. Fatti "mai eseguiti
da nessuno" (nota del ledger) visti qui: `loot give <giocatore> loot redstone_exoframes:blocks/charger` da' un
caricatore sulle due versioni; `recipe take` riesce per le sette ricette dopo il `give` del lingotto di rame, nel client
vero, sulle quattro varianti.

### Step 3 — scatti di riferimento

`convert <cartella>/<scatto>.png -strip -define png:exclude-chunks=date,time docs/test-in-gioco/m3c-screenshot/<variante>-<scatto>.png`
per `1.20.1-forge` e `1.21.1-neoforge`, `caricatore-posato` e `caricatore-acceso`. Il controllo Python del brief su
`docs/test-in-gioco/m3c-screenshot/*.png` (tutti e sedici i file): nessuna uscita, uscita 0.

### Step 4 — server dedicato

Per ogni variante: `eula=true` in `versions/<v>/run/eula.txt`, `rm -rf versions/<v>/run/world`,
`./gradlew --console=plain :<v>:runServer` con l'ingresso su una FIFO, attesa di `Done (`, poi `stop` scritto sull'ingresso.

| Variante | `Done (` | Arresto | `check_log.py client --variant <v> …/run/logs/latest.log` |
|---|---|---|---|
| 1.20.1-forge | `Done (3.430s)!`, 17 s dall'avvio di Gradle | `stop`, Gradle esce con 0 | nessuna uscita, uscita 0 |
| 1.20.1-fabric | `Done (2.908s)!`, 15 s | `stop`, 0 | nessuna uscita, uscita 0 |
| 1.21.1-fabric | `Done (0.983s)!`, 13 s | `stop`, 0 | nessuna uscita, uscita 0 |
| 1.21.1-neoforge | `Done (1.485s)!`, 15 s | `stop`, 0 | nessuna uscita, uscita 0 |

Nessuna riga WARN o ERROR che nomina il mod su Forge e NeoForge; sulle due Fabric la sola riga voluta
(`No data fixer registered for exoframe`).

### Step 5 — controlli sulla checklist

```
grep -c '^- \[ \] \*\*[0-9]*\.\*\*' docs/test-in-gioco/m3c-giocarci.md      -> 112
grep -n '^- \[ \] \*\*75\.\*\*' docs/test-in-gioco/m3c-giocarci.md          -> una riga
grep -c '^## [0-9]*\. ' docs/test-in-gioco/m3c-giocarci.md                   -> 23
grep -n '^## 15\. Server dedicato' docs/test-in-gioco/m3c-giocarci.md        -> una riga
grep -rn 'm3c1-consumare' README.md docs tools --include='*.md' | grep -v 'superpowers/plans'
   -> due righe, tutte e due la frase che racconta la rinomina:
      docs/test-in-gioco/m3c-giocarci.md:5 (introduzione) e il design §12.15 ("Come e' andata (Task 10 della 3c-2)")
```

I numeri dei passi vanno da 1 a 112 senza buchi e senza doppioni.

### Step 7 — verifica finale del "fatta quando" della 3c

Comandi di chiusura, sulle quattro varianti, eseguiti prima dei commit (dopo, sono cambiati solo documenti):

```
./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
    :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar
BUILD SUCCESSFUL in 2s
52 actionable tasks: 52 up-to-date
```

Tutto aggiornato perche' nessun sorgente cambia in questo task. Per avere i JUnit girati davvero su questo HEAD:
`./gradlew --console=plain :1.20.1-forge:test --rerun :1.20.1-fabric:test --rerun :1.21.1-fabric:test --rerun :1.21.1-neoforge:test --rerun`
→ `BUILD SUCCESSFUL in 19s`, 4 task eseguiti; dai risultati XML: 319 test, 0 falliti, 0 errori, 0 saltati su ciascuna
delle quattro varianti.

```
bash tools/check_sources.sh   -> OK sources
bash tools/check_jars.sh      -> OK jars
bash tools/check_art.sh       -> OK art
bash tools/check_data.sh      -> OK data
bash tools/run_gametests.sh   ->
OK 1.20.1-forge: 208 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 208 test obbligatori passati, nessun avviso fuori elenco
grep -rn 'isCreative()' src/main/java/com/toxineren/exoframes/content src/main/java/com/toxineren/exoframes/core
    | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)'            -> nessuna riga
git diff --stat 614b25f -- tools/log_allowlist.txt          -> nessuna riga (vedi "Scostamenti dal brief")
```

La tabella del brief, riga per riga. "208/208" vuol dire che il test nominato e' fra i 208 passati su quella variante
(`All 208 required tests passed` nel log, e la riga `OK` di `run_gametests.sh`); le righe `add(tests,` sono 208 in tutto
(Charger 27, Energy 29, Damage 13, Cell 13, Data 5, Survival 1, il resto nelle altre classi).

| Che cosa | Prova | 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric | 1.21.1-neoforge |
|---|---|---|---|---|---|
| costruire con le ricette | `everyRecipeIsFoundByKey`, `everyRecipeCraftsFromItsShape`, `aCopperIngotUnlocksTheSevenRecipes` (208/208); `m3c2_shots.sh` passo 1 (sette `recipe take` riusciti) | sì; sì | sì; sì | sì; sì | sì; sì |
| caricare al caricatore | i 27 test di `ChargerGameTests`; `m3c2_shots.sh` passi 3-6 (posa, cella, polvere, carica 3 120→3 322 o 3 117→3 320 RE, ritiro) | sì; sì | sì; sì | sì; sì | sì; sì |
| entrare, consumare energia | i 29 test di `EnergyGameTests`; `m3c_shots.sh` (OK, 126/113/117/119 s) | sì; sì | sì; sì | sì; sì | sì; sì |
| prendere danno, pezzi che si consumano e si rompono | i 13 test di `DamageGameTests` | sì | sì | sì | sì |
| cambiare cella | i 13 test di `CellGameTests`; `m3c_shots.sh` passo 10 (carica 4 900-5 000 dopo R, una cella nell'inventario) | sì; sì | sì; sì | sì; sì | sì; sì |
| ricaricare, tutto di fila | `survivalFromCraftingToRecharging` | sì | sì | sì | sì |
| nulla si perde, nulla si duplica | Review Focus 1: `fullHopperBelowNeitherLosesNorDuplicatesTheCell`, `hopperAboveInsertsOneCell`, `hopperBelowTakesOnlyAFullCell`, `cellClickSwapsWithTheCellInside`, `twoPlayersOneCell`, i conti finali di `survivalFromCraftingToRecharging`; Focus 2: `breakingDropsTheCellAndTheDust`, `litTogglesTwiceWithoutLosingTheCell`, `chargerSavesAndLoadsItsContents`; Focus 3: `chargerLoadNeverThrowsAndNeverCutsACharge`, `fullHopperBelowNeitherLosesNorDuplicatesTheCell`, `cellClickInsertsIntoAnEmptyCharger` | sì | sì | sì | sì |
| un solo sorgente, quattro jar | build (`BUILD SUCCESSFUL`, quattro jar in `build/libs/all/`), `OK sources`, `OK jars`; server dedicato avviato e fermato | sì | sì | sì | sì |

## TDD

Non si applica nel senso stretto: il task non scrive codice del mod ne' test. Lo script e' stato scritto dal brief e
provato subito sul gioco (Step 1 e 2): riuscito alla prima esecuzione sulle quattro varianti. Non ho fatto una prova
negativa dello script (per esempio una ricetta mancante che faccia fallire `recipe take`): che un controllo che non
torna fermi `client_shot.sh` con uscita 1 e' gia' provato per quello strumento (Task 1, il file `{}`).

## File cambiati

- Creati: `tools/client/m3c2_shots.sh`;
  `docs/test-in-gioco/m3c-screenshot/1.20.1-forge-caricatore-posato.png`, `1.20.1-forge-caricatore-acceso.png`,
  `1.21.1-neoforge-caricatore-posato.png`, `1.21.1-neoforge-caricatore-acceso.png`;
  `docs/test-in-gioco/m3c1-consumare.md` (il rimando, secondo commit).
- Rinominato e modificato: `docs/test-in-gioco/m3c1-consumare.md` → `docs/test-in-gioco/m3c-giocarci.md`.
- Modificati: `README.md`, `tools/README.md`, `tools/client/README.md`, `docs/compat.md`,
  `docs/test-in-gioco/m3a-vedere-il-telaio.md`, `docs/test-in-gioco/m3b-entrare-e-uscire.md`,
  `docs/superpowers/specs/2026-11-13-m3-telaio-design.md`.

Documenti, punto per punto:

- `README.md`: "Stato" (fatta la 3c-2; la milestone 3 verificata nel cloud aspetta il PC con `m3c-giocarci.md`); un
  paragrafo "Dati" con `gen_data.py` e `check_data.sh`; 208 test; nella config la regola del taglio ("a mano, in un
  caricatore") e i due campi del caricatore; gli script del client (`m3c_rules_shots.sh`, che mancava, e
  `m3c2_shots.sh`); "Documenti" (la checklist nuova, i piani fino a M3c-2).
- `tools/README.md`: riletto contro gli strumenti (provati `gen_data.py --selftest` = 7 difetti, 18 file di
  `gen_placeholders.py --out`, 32 di `gen_data.py --out`, i passi della CI): era aggiornato; aggiunti la riga di
  `check_jar_privacy.py` e, in testa, i nomi degli script del client.
- `tools/client/README.md`: riga d'uso, paragrafo di `m3c2_shots.sh` con la tabella dei passi, gli scatti, che cosa non
  prova, la durata misurata (80-100 s); due voci di "Cosa non copre" completate.
- `docs/compat.md`: riletto contro `EntityBlockBase`, `BlockEntityBase`, `BlockItemBase`, le firme dei cinque aiuti di
  `GameTests`, le costanti `CHARGER_*` di `NbtKeys` e i nomi dei test citati (ci sono tutti): era fedele. Cinque
  correzioni: l'apertura non dice piu' che i blocchi staranno in un futuro `block/` (minore T2-F2 del ledger); il ramo
  client del clic e il suggerimento rimandano allo script e al passo 80; nei limiti noti il taglio "a mano" in un
  caricatore; nella tabella API la riga dei dati per versione nomina `gen_data.py` e `check_data.sh`.
- Design: solo le correzioni elencate dal brief, ciascuna con il suo task: §12.10 (due frasi), §12.11 (sei), §12.12
  (due), §12.15 (due), §12.16 (quattro). 14 righe toccate.

## Scelte

- **Due commit invece di uno.** Il rimando al vecchio percorso (Ruling del ledger) sta in un commit suo: nello stesso
  commit della rinomina il vecchio percorso continuerebbe a esistere e git non vedrebbe una rinomina
  (`git log --follow docs/test-in-gioco/m3c-giocarci.md` ora risale a `614b25f` e `605fca6`).
- **`m3c_rules_shots.sh` rifatto sulle quattro varianti**, anche se lo Step 2 non lo chiede: la checklist dice "gia'
  verificato" dei tre script, e l'ultima esecuzione era del Task 1, prima del caricatore. Otto minuti.
- **Scatti di riferimento da `1.20.1-forge` e `1.21.1-neoforge`**: il brief lascia la scelta; sono le due varianti con
  un loader diverso da quelle degli scatti del cambio cella e della vasca.
- **Dove stanno i passi nuovi delle sezioni esistenti**: 108 in coda alla sezione 1 (avvio e mondo), 109 alla 7 (usura:
  e' il posto dell'armatura), 110-111 alla 12 (l'altra regola dell'uscita), 112 alla 15 (server dedicato).
- **Passo 1 della checklist**: unico passo vecchio con il testo cambiato, perche' "la parte 3c-1 e' unita a `main`" come
  fonte dei jar e' cio' che la 3c-2 ha reso falso.
- **La partita completa con 10 + 8 + 2 polveri**: il brief da' 24 polveri (4 per le due celle, 20 da bruciare) e chiede
  di caricare due celle e poi ricaricare; con 10 e 8 le due celle escono a 5 000 e 4 000 RE (100 e 80 s) e ne restano 2
  per la ricarica finale.
- **La catena di tramogge con celle a 9 000 RE** (passo 97): quattro celle in un minuto e mezzo invece di tredici
  minuti; la versione con celle vuote e' indicata come facoltativa, e i 200 s si misurano al passo 92.
- **Nomi delle persone**: non ne ho scritti di nuovi. La checklist rinominata tiene quelli che aveva (intestazione,
  "Domande per …", "le risposte di …"), che sono testo esistente, come nelle altre checklist e nel design.
- **`tools/README.md`**: oltre alla rilettura, la riga di `check_jar_privacy.py`, che e' uno strumento della cartella e
  non era in tabella.
- **Ruling sulla polvere per tick**: la frase mancava in design §12.11 e l'ho aggiunta, come dice il ledger.

## Scostamenti dal brief

- **`git diff --stat main -- tools/log_allowlist.txt` stampa una riga** in questo checkout (39 righe cambiate): il ramo
  locale `main` e' fermo a `2e35aa6` (l'unione della 3b), mentre `main` sul remoto e la base del ramo sono `614b25f`
  (`git ls-remote origin main`, e il ledger). Le differenze sono dei commit `f754b0e` e `88e3614` della 3c-1. Contro la
  base vera il controllo torna: `git diff --stat 614b25f -- tools/log_allowlist.txt` → nessuna riga, e
  `git log 614b25f..HEAD -- tools/log_allowlist.txt` → nessun commit. L'intento del passo (questa parte non aggiunge
  avvisi voluti) e' rispettato; non ho toccato il ramo `main` locale.
- **Step 8, un commit**: sono due (vedi "Scelte").
- Nient'altro: i passi dello script sono quelli del brief senza correzioni.

## Autorevisione

- Riletta la sequenza dello script contro "Produces" passo per passo: stessi comandi, stesso ordine, stesse attese.
- Nella checklist ho corretto, rileggendo: il passo 96 non diceva di togliere le tramogge dei passi 94-95; il passo 98
  diceva che anche l'oggetto del caricatore cade "fermo" (ferme cadono cella e polvere); il passo 103 chiedeva di
  leggere uno stato che in piedi non si legge senza prendere la cella; il passo 109 usava coordinate relative per
  rimettere il blocco.
- Ogni fatto del caricatore scritto nella checklist viene dal codice (`ChargerBlock.onClick`, `FrameEntry.stepOut`,
  `FrameInteractions`, `PieceItem`) o dai file di lingua; i nomi italiani del gioco (Banco da fabbricazione, Cornice
  riempita/svuotata, "Nuove ricette sbloccate!", "(Creato per una versione di Minecraft più vecchia)") vengono dal file
  di lingua italiano del gioco nella cache delle risorse.
- Apostrofi tipografici nelle righe aggiunte: zero. Nessun nome di strumenti o modelli, nessun percorso della macchina.

## Problemi e dubbi

1. **Il ramo locale `main` e' vecchio** (`2e35aa6`, non `614b25f`): ogni comando di un brief che confronta con `main`
   in questo checkout vede anche la 3c-1. Vedi "Scostamenti dal brief".
2. **Attese della checklist che nessuno ha visto in gioco.** Scritte dal codice o dalle regole del gioco base, non
   provate nel cloud perche' lo strumento non puo': i tempi di rottura (circa 17 s a mano, meno di 3 s con il piccone di
   legno), lo scudo nella mano secondaria da accovacciati (il clic non arriva al blocco), le schede del ricettario per
   icona, il ricettario dell'inventario che non mostra ricette piu' grandi di 2 × 2, i 7 e 15 del comparatore con una
   fila di polvere, il piglin e la neve polverosa del passo 109, la schermata dei pacchetti di dati del passo 108 (il
   nome del pacchetto dipende dal loader: il passo dice "annota"). Se una e' sbagliata, il PC la trova come "non OK" di
   un passo che invece e' un errore della checklist.
3. **`m3c_shots.sh`: scatti non riguardati** in questo task (solo l'esito dei controlli).
4. **Dei minori rinviati del ledger** ho chiuso solo T2-F2 (apertura di `compat.md`), perche' sta in un file del task.
   T7-F5 (commento vecchio in `build.gradle.kts`) e gli altri restano: fuori dai file di questo task.
5. Nessun processo lasciato acceso: restano solo i demoni di Gradle a riposo.
