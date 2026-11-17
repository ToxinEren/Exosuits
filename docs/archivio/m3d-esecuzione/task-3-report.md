# Task 3: scatti di riferimento, checklist per il PC, documenti, verifica finale

Stato: **DONE_WITH_CONCERNS** (le preoccupazioni sono minori, in fondo).
BASE 3a7c47ad95817dc2c7a2843e20d2142c0533c7b2, HEAD ffe2ab2f73c305ad545b2a76486b89ead79c30e7.

Commit:
- `883483f docs: checklist del braccio in prima persona, scatti di riferimento, documenti`
- `ffe2ab2 docs: checklist 3d, i nomi del menu della rete locale, lo stato 2 e il braccio montato sull'altro lato` (solo `m3d-braccio.md`, dopo la rilettura: passo 18 con i nomi del menu della rete locale presi da `m3c-giocarci.md` passo 7; passo 7 con la descrizione dello stato 2 presa da `m3a-vedere-il-telaio.md`; passo 14 con una frase per il caso in cui il braccio finisce montato sull'altro lato). Dopo questo commit rifatti i controlli dello Step 4 (19 passi, una riga `**1.**`, nessuno scatto mancante, `git diff --stat e064c9e -- docs/test-in-gioco/m3c-giocarci.md` vuoto) e quelli del design dello Step 5 (0 e 1): invariati.

## Che cosa ho fatto

- **Step 1.** `m3d_shots.sh` sulle quattro varianti, una alla volta: quattordici `ok`, riga `OK <variante>`, uscita 0, i due `log-check.txt` vuoti (0 byte) per ciascuna.
- **Step 2.** `client_shot.sh` con lo scatto `braccio-colori-suoi` su 1.20.1-forge e 1.21.1-neoforge (uscita 0, `log-check.txt` vuoto; guardati: nessuna riga di chat sopra la barra, nessun avviso in alto a destra). Dieci file in `docs/test-in-gioco/m3d-screenshot/` con `convert -strip -define png:exclude-chunks=date,time`; il controllo dei chunk del brief non stampa nulla (uscita 0); tutti 960x540. Guardati tutti: braccio-rottame, braccio-nudo, mappa (le due braccia ai lati, la mappa vuota libera al centro), braccio-mancino, colori suoi.
- **Step 3.** Server dedicato sulle quattro varianti fino a `Done (` e `stop` (script in scratchpad: Gradle `runServer` con lo stdin da una FIFO, attesa di `Done (`, poi `stop`): `Done (1.923s)` neoforge, `Done (3.212s)` forge, `Done (2.788s)` fabric 1.20.1, `Done (0.863s)` fabric 1.21.1; una riga `Stopping server` per ciascuna, Gradle uscito con 0 in 14-20 s; `python3 -I tools/check_log.py client --variant <v> versions/<v>/run/logs/latest.log` nessuna uscita, uscita 0, sulle quattro; zero righe `InvalidInjectionException` o `Mixin apply`. `eula=true` c'era gia' nei quattro `versions/<v>/run/eula.txt`.
- **Step 4.** `docs/test-in-gioco/m3d-braccio.md`, nuova: intestazione (per chi, quali jar, come riconoscerli, l'avvertenza sugli scatti in prima persona delle checklist precedenti), "Cosa c'e' da vedere", "Cosa non c'e' ancora", "Gia' verificato nella sessione cloud" (la tabella dei quattordici controlli, gli scatti di riferimento, cio' che il cloud non prova), "0. Preparazione" (A-E: jar, istanze, cartella degli scatti, tasti, mondo di prova), passi 1-19 nell'ordine del brief, "Cosa riportare", "Domande per il proprietario" (grandezza, posto, braccio del pilota nel telaio nudo, mappa).
- **Step 5.** Documenti:
  - design §5.2: la frase della prima persona diventa "In prima persona, dalla parte 3d, il braccio è quello della tuta (§13)."
  - design §13: l'introduzione e le due etichette "(proposta del piano, raccomandata dalla revisione)" dicono che chi coordina ha confermato i due punti (ruling 12); il criterio (a) come l'ha precisato chi coordina (ruling 45) e la scelta "guardando gli scatti" invece di "il piu' grande" (ruling 46), con il rimando al Task 2 Step 1, nella frase dei criteri e in quella del secondo giro; dopo "I valori scelti e le misure li scrive qui l'esecuzione." un paragrafo "Valori scelti" con K 1,3, spostamento 4,5, che e' servito il secondo giro, e la tabella delle misure dei sei candidati (dal Javadoc di `SCALE` e dal rapporto del Task 2). Nient'altro del design.
  - `m3a-vedere-il-telaio.md`: solo la frase del passo "Prima persona.".
  - `README.md`: "Stato" (la parte 3d fatta, verificata nel cloud, aspetta il PC con `m3d-braccio.md`), il paragrafo degli script (`m3d_shots.sh`, `arm_measure.sh` e `--selftest`), "Documenti" (`m3d-braccio.md` con `m3d-screenshot/`; piani fino a M3d).
  - `tools/README.md`: `m3d_shots.sh` e `arm_measure.sh` fra gli strumenti del client senza schermo.
  - `tools/client/README.md` (riletto contro `m3d_shots.sh`, `arm_measure.sh`, `client_shot.sh`): la prima riga dice anche la prima persona; la larghezza dell'anteprima su 1.20.1 "fra 166 e 174" (misurata 174 su Forge in questo task); dove stanno gli scatti di riferimento; `m3d_shots.sh` fra le sequenze che tolgono `EXO_SHOT_SERVER_CONFIG` (il minore T2-F6: lo script lo fa, riga `unset EXO_SHOT_SERVER_CONFIG`); le durate di questo task (207-225 s, 111-119 la prima esecuzione, 76-86 il mancino; `client_shot.sh` con quattro passi 74-86 s; `--selftest` 4-5 s); in "Cosa non copre" il braccio in prima persona fuori dalla vista a riposo, con il rimando alla checklist.
  - `docs/compat.md` (riletto contro `SuitRenderer.renderArm`, `SuitRendererImpl.renderArm/draw/applyBoneVisibilityBySlot`, `SuitBoneRules.visibleForArm`, `PlayerRendererMixin.exoframes$suitArm`, `FirstPersonArm.onHandPrepared`: le voci corrispondono al codice): aggiunti i valori scelti nella voce `FirstPersonArm`; nella "Verifica" dei mixin `m3d_shots.sh` sulle quattro varianti e il server dedicato di questo task, e `m3d-braccio.md` passi 1 e 3 per i jar di produzione.

## Test e prove

### Step 1, `EXO_ACCEPT_EULA=true bash tools/client/m3d_shots.sh <v> <scratch>/m3d/<v>`

| Variante | Durata (A + B) | Righe | Ultima riga | `log-check.txt` A / B |
|---|---|---|---|---|
| 1.21.1-neoforge | 216 s (115 + 81) | 14 `ok` | `OK 1.21.1-neoforge: scatti in .../m3d/1.21.1-neoforge` | 0 / 0 byte |
| 1.20.1-forge | 225 s (119 + 86) | 14 `ok` | `OK 1.20.1-forge: scatti in .../m3d/1.20.1-forge` | 0 / 0 byte |
| 1.20.1-fabric | 207 s (111 + 76) | 14 `ok` | `OK 1.20.1-fabric: scatti in .../m3d/1.20.1-fabric` | 0 / 0 byte |
| 1.21.1-fabric | 209 s (112 + 77) | 14 `ok` | `OK 1.21.1-fabric: scatti in .../m3d/1.21.1-fabric` | 0 / 0 byte |

Le misure sono uguali al pixel sulle quattro varianti salvo `terza-persona` e `inventario`. Righe di 1.21.1-neoforge:

```
ok controllo: pixel=18075 copertura=3.49 mirino=0 barra=0 energia=0 riquadro=161x149+677+391
ok dipinto: magenta=39962 riquadro=286x188+674+352; meta' sinistra magenta=0 riquadro=0x0+0+0
ok criteri: pixel=45442 copertura=8.77 mirino=0 barra=0 energia=0 riquadro=286x205+674+335
ok solo-tuta: sagoma=18075 uguali=0
ok nudo: magenta=0 riquadro=0x0+0+0; pixel=40026 copertura=7.72 mirino=0 barra=0 energia=0 riquadro=260x205+700+335; sagoma=18075 uguali=0
ok bastone: magenta=0 riquadro=0x0+0+0
ok mappa: sinistra magenta=28407 riquadro=365x179+0+361; destra magenta=28977 riquadro=369x182+591+358
ok terza-persona: magenta=23593 riquadro=318x312+321+221
ok inventario: magenta=10784 riquadro=98x140+356+120
ok senza-tuta: pixel=0 copertura=0.00 mirino=0 barra=0 energia=0 riquadro=0x0+0+0
ok telaio-piazzato: pixel=52142 copertura=10.06 mirino=3600 barra=0 energia=0 riquadro=258x284+351+113; magenta=39962 riquadro=286x188+674+352, il 100 % di braccio-rottame
ok mancino-controllo: pixel=18075 copertura=3.49 mirino=0 barra=0 energia=0 riquadro=161x149+122+391
ok mancino: meta' destra magenta=0 riquadro=0x0+0+0; pixel=45442 copertura=8.77 mirino=0 barra=0 energia=0 riquadro=286x205+0+335
ok specchio: mancino magenta=39962 riquadro=286x188+0+352, il 100 % del destro (magenta=39962 riquadro=286x188+674+352); riquadro specchiato 286x188+674+352, scarti sinistra 0 alto 0 destra 0 basso 0
OK 1.21.1-neoforge: scatti in <area-temporanea>/.../scratchpad/m3d/1.21.1-neoforge
```

Le due misure che cambiano: `terza-persona` magenta 23606 (forge), 23737 (fabric 1.20.1), 23723 (fabric 1.21.1), riquadro 318x312+321+221 ovunque; `inventario` 18830 riquadro 174x202+319+66 (forge), 18786 riquadro 170x202+321+66 (fabric 1.20.1), 10799 riquadro 98x140+356+120 (fabric 1.21.1).

### Step 2

`client_shot.sh` "@attendi 8" "execute as Exo run exoframe debug wear rottame" "@attendi 2" "@scatto braccio-colori-suoi": 1.20.1-forge `EXIT 0`, 86 s, `log-check.txt` 0 byte; 1.21.1-neoforge `EXIT 0`, 74 s, `log-check.txt` 0 byte. Il controllo dei chunk del brief su `docs/test-in-gioco/m3d-screenshot/*.png`: nessuna uscita, uscita 0.

### Step 4

```
$ grep -c '^- \[ \] \*\*[0-9]*\.\*\*' docs/test-in-gioco/m3d-braccio.md
19
$ grep -n '^- \[ \] \*\*1\.\*\*' docs/test-in-gioco/m3d-braccio.md
64:- [ ] **1.** Avvia il client fino al menu principale con ...
$ git diff --stat e064c9e -- docs/test-in-gioco/m3c-giocarci.md
(nessuna riga)
$ cd docs/test-in-gioco; for f in $(grep -o 'm3d-screenshot/[A-Za-z0-9._-]*\.png' m3d-braccio.md | sort -u); do [ -f "$f" ] || echo "manca $f"; done
(nessuna uscita; dieci scatti nominati)
```

### Step 5

```
grep -rn 'm3d_shots\|arm_measure' README.md tools/README.md tools/client/README.md | wc -l   -> 13
grep -c 'La prima persona (.renderHand' docs/compat.md                                       -> 0
grep -c 'resta quello vanilla fino a M4' docs/superpowers/specs/2026-11-13-m3-telaio-design.md   -> 0
grep -c 'il braccio è quello della tuta (§13)' docs/superpowers/specs/2026-11-13-m3-telaio-design.md -> 1
```

### Step 6, i controlli di chiusura

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` -> `BUILD SUCCESSFUL in 2s`, `52 actionable tasks: 52 up-to-date` (questo task non tocca il codice: i JUnit non rigirano, e i risultati sono quelli dell'ultima esecuzione sullo stesso codice, che Gradle considera aggiornata).
- Conto dei JUnit: `1.20.1-forge 331`, `1.20.1-fabric 331`, `1.21.1-fabric 331`, `1.21.1-neoforge 331`; nessuna suite con `failures` o `errors` diversi da 0.
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `bash tools/check_art.sh` -> `OK art`
- `bash tools/check_data.sh` -> `OK data`
- `bash tools/run_gametests.sh` (129 s):
  ```
  OK 1.20.1-forge: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 208 test obbligatori passati, nessun avviso fuori elenco
  ```
- `bash tools/client/arm_measure.sh --selftest` -> `OK arm_measure` (5 s)
- `git diff --stat e064c9e -- tools/log_allowlist.txt src/main/resources src/main/resources-mc1.20.1 src/main/resources-mc1.21.1` -> nessuna riga (vedi "Scostamenti": `e064c9e` al posto di `main`).

### Step 6, il "fatta quando" di design §13

| Che cosa | Prova | 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric | 1.21.1-neoforge |
|---|---|---|---|---|---|
| il braccio della tuta al posto di quello del gioco, a mano vuota | `m3d_shots.sh`: `dipinto`, `criteri`, `solo-tuta` | ok, ok, ok | ok, ok, ok | ok, ok, ok | ok, ok, ok |
| il pezzo, lo stato e i colori della terza persona | `armDrawShowsOnlyTheBonesOfThatArm`, `armDrawFollowsTheStateOfItsOwnPiece`, `armDrawShowsNothingTheWholeDrawHides` (JUnit, in `SuitBoneRulesTest`, 331 senza errori); `dipinto` | passati; ok | passati; ok | passati; ok | passati; ok |
| con il telaio nudo, senza il braccio del pilota | `armDrawWithoutTheArmPieceShowsTheFrameArmOnly` (JUnit); `nudo` | passato; ok | passato; ok | passato; ok | passato; ok |
| la mappa | `mappa` | ok | ok | ok | ok |
| il mancino | `mancino-controllo`, `mancino`, `specchio` | ok, ok, ok | ok, ok, ok | ok, ok, ok | ok, ok, ok |
| senza tuta e con un oggetto in mano nulla è cambiato | `senza-tuta`, `bastone` | ok, ok | ok, ok | ok, ok | ok, ok |
| la tuta in terza persona, nell'inventario e piazzata è intera | `terza-persona`, `inventario`, `telaio-piazzato`; in `SuitRendererImpl.applyBoneVisibilityBySlot` il ciclo `for (String name : SuitBoneRules.BONES)` con `arm == null ? visible : visibleForArm`, e `arm = null` nel `finally` di `draw` (letto nel codice) | ok, ok, ok | ok, ok, ok | ok, ok, ok | ok, ok, ok |
| grandezza e posto secondo i criteri | `criteri`, `nudo`, `mancino`; le misure scritte in §13 | ok, ok, ok; scritte | ok, ok, ok | ok, ok, ok | ok, ok, ok |
| un solo sorgente, quattro jar, l'aggancio in ognuno | build, `check_sources.sh`, `check_jars.sh` (e2) | BUILD SUCCESSFUL, OK sources, OK jars (sui quattro jar) | idem | idem | idem |

I quattro test JUnit nominati: presenti in `SuitBoneRulesTest` e girati in ogni variante (il conto 331 e nessuna suite con errori; `grep` dei nomi nei file `TEST-*SuitBoneRulesTest.xml` delle quattro varianti).

## File cambiati

- nuovi: `docs/test-in-gioco/m3d-braccio.md`, `docs/test-in-gioco/m3d-screenshot/{1.20.1-forge,1.21.1-neoforge}-{braccio-rottame,braccio-nudo,mappa,braccio-mancino,braccio-colori-suoi}.png`
- modificati: `README.md`, `tools/README.md`, `tools/client/README.md`, `docs/compat.md`, `docs/test-in-gioco/m3a-vedere-il-telaio.md` (una frase), `docs/superpowers/specs/2026-11-13-m3-telaio-design.md` (§5.2 una frase, §13)
- non toccati: `m3c-giocarci.md`, codice, script.

## Scelte

- **La preparazione con lettere (A-E) e non con numeri.** La checklist di `m3c` numera la preparazione 1-4; qui i passi devono partire da 1 (il brief e i numeri dei passi citati nel Review Focus del piano: 1, 3, 11, 14, 15), e il controllo `grep '\*\*1\.\*\*'` vuole una riga sola. Con le lettere la preparazione resta "ripetuta, non rimandata" e i passi hanno i numeri del brief.
- **"Per l'assistente ... del proprietario" e "Domande per il proprietario"** al posto dei nomi propri che usano le checklist precedenti: le regole del repository vietano dati personali nei documenti, e il brief stesso dice "assistente" e "proprietario".
- **Quali passi su quali varianti:** 1-15 e 19 su tutte e quattro (i passi 1 e 3 sono la prova dei jar di produzione, Review Focus 4), 16 e 17 una volta su una 1.20.1 e una volta su una 1.21.1, 18 una volta se c'e' un secondo giocatore.
- **La creazione del mondo al passo 2**, descritta in E: cosi' i passi 2 e 3 vengono prima di ogni F5 o E (Review Focus 2) e il passo 1 resta l'avvio con il controllo del log.
- **Il passo 9 rimette la tuta magenta** (`debug wear rottame 0 FF00FF FF00FF`) prima di F5: dopo il passo 8 la tuta e' nuda, e i passi 11 e 12 si confrontano con scatti di riferimento magenta.
- **Il passo 5 senza rompere blocchi e con una leva data da `/setblock`**: in sopravvivenza un clic lungo sull'erba la romperebbe e la terra finirebbe nella mano vuota; la leva sta due blocchi a sud.
- **Il passo 6 toglie il bastone con `/clear @s minecraft:stick`** (e l'avviso che `/clear @s` da solo toglierebbe i segnaposto), e il passo 11 crea una mappa vera da una mappa vuota (`/give @s minecraft:map` e clic destro) invece di una `filled_map` senza dati come lo script: sul PC si deve poter dire se la mappa si legge. Il bastone nell'altra mano e la pulizia con `/item replace ... weapon.offhand` come nello script.
- **I comandi del log da PowerShell** (`Select-String`), perche' la checklist e' per un PC Windows e l'assistente non puo' fare domande.
- **Il passo 18 su una partita aperta in LAN** con `/execute as <nome> run exoframe debug wear ...` dall'ospite (in LAN solo l'ospite ha i comandi).
- **La riga 1,0 con 3,0 della tabella di §13** (barra 0 anche con il telaio nudo, mirino ed energia 0) l'ho controllata in `/home/user/m3d-candidati/misure.txt` (righe `k100-s30 pezzi` e `k100-s30 nudo`), non dedotta da "passa".
- **Design §13: il paragrafo "Valori scelti" e' separato** dalla frase "I valori scelti e le misure li scrive qui l'esecuzione.", con una tabella dei sei candidati (copertura, barra, riquadro, esito, "bordo da y" dove misurato). Le frasi dei criteri e del secondo giro corrette in loco, con il rimando al Task 2 Step 1 (ruling 45 e 46); l'introduzione e le due etichette dicono la conferma di chi coordina (ruling 12).
- **`tools/client/README.md`: corretto il minore T2-F6** (`m3d_shots.sh` nella riga di `EXO_SHOT_SERVER_CONFIG`), perche' il file e' fra quelli che il brief mi chiede di rileggere contro gli script; gli altri minori (T2-F3/F4/F5, T1-F2/F3) stanno in file fuori dal mio elenco e restano aperti.

## Scostamenti dal brief

- **`git diff --stat main` usa `e064c9e` al posto di `main`.** Il `main` locale di questa macchina e' fermo a `2e35aa6` (l'unione della parte 3b), e con lui `git diff --stat main -- docs/test-in-gioco/m3c-giocarci.md` dava 401 righe aggiunte (il file su quel `main` non c'e'). `git ls-remote origin refs/heads/main` dice `e064c9e`, che e' anche la base del ramo scritta nel registro ed e' antenato di HEAD. Con `e064c9e` i due controlli (Step 4 e Step 6) non stampano nulla. Il `main` locale non l'ho toccato.
- **Step 3: il server avviato con uno script mio nello scratchpad** (Gradle `runServer` con lo stdin da una FIFO, `stop` scritto nella FIFO dopo `Done (`); il brief non dice come mandare `stop`.
- **Step 6, la build:** tutta `up-to-date` (il codice non cambia in questo task), quindi i JUnit non sono rigirati; il conto viene dai risultati dell'ultima esecuzione sullo stesso codice.

## Autorevisione

- Completezza: ogni voce di "Produces" c'e' (dieci scatti, la checklist con i diciannove passi nell'ordine e con il contenuto del brief, i sei documenti). Ogni passo della checklist dice comando o tasto, risultato atteso, e "annota" dove il risultato non e' noto.
- Corretto durante l'autorevisione: "un dodicesimo dello schermo" (sbagliato: 8,77 % e' circa un undicesimo) diventato "circa l'8,8 %"; i nomi dei menu di cui non sono sicuro in italiano (campo visivo, impostazioni video) scritti con il nome inglese accanto; la durata di `--selftest` misurata 5 s, il README diceva 4.
- Nessun nome proprio, di modello o di fornitore nei file del commit (controllato con `grep`); nessun processo di gioco, server o Xvfb rimasto acceso (`pgrep`).

## Preoccupazioni

- **I nomi dei menu italiani** della checklist che non vengono da un'altra checklist (Personalizzazione skin, Mano principale, Sinistra/Destra) li ho scritti a memoria, con il nome inglese accanto; non ho un client in italiano per controllarli. Quelli della rete locale sono copiati da `m3c-giocarci.md`.
- **Il commento di testa di `m3d_shots.sh`** (non nel mio elenco) dice il magenta della terza persona "fra 23595 e 23745": in questo task 1.21.1-neoforge ha dato 23593, e la larghezza dell'anteprima su 1.20.1-forge 174 (il commento dice 170-172). Il controllo usa la larghezza (soglie 130 e 70), quindi passa; il commento e' solo un po' stretto. Anche le durate del commento (208-226 s, 112-121 e 76-85) ora differiscono da quelle di `tools/client/README.md` (207-225 s, 111-119 e 76-86, di questo task). Ho aggiornato `tools/client/README.md`, non lo script.
- **Il `main` locale e' fermo a `2e35aa6`**: i comandi del piano che dicono `main` lo leggono in modo sbagliato su questa macchina (vedi "Scostamenti").
- **Il passo 15 della checklist** dice che una riga `Suit rendering failed` per ricarica "non e' un errore": e' la formulazione del brief, ma non ho provato una ricarica nel cloud (lo script non la fa), quindi non so se la riga compare davvero.
