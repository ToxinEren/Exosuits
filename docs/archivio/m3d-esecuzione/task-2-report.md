# Task 2 — Grandezza e posto scelti nel client vero, la sequenza: rapporto

Stato: **DONE_WITH_CONCERNS** (un solo dubbio, sul giudizio a occhio del criterio (a): vedi "Dubbi"). BASE 3bfc80c155296aa792b7accd15ca09e1b1542a8d, HEAD a8c6c8fbf80288809a8e02e3640cef68ad875510, ramo `m3d-braccio`.
Un commit, firmato dall'ambiente: `a8c6c8f client: grandezza e posto del braccio in prima persona scelti nel client vero; tools: m3d_shots.sh`.

**Scelto: K = 1,3 con `SIDE_SHIFT_PX` = 4,5** (secondo giro), cioe' la previsione del piano. Le misure del valore scelto sono uguali al pixel sulle quattro varianti.

Cartelle fuori dal repository, per chi coordina:
- candidati: `/home/user/m3d-candidati/` (`misure.txt`, `k165.png`, `k130.png`, `k100.png`, i `con.png` del giro 2 come `k165-s60.png`, `k165-s65.png`, `k130-s40.png`, `k130-s45.png` con il loro `-nudo.png`, `scelto-sopravvivenza.png`; ogni prova ha la sua sottocartella con scatti e log);
- sequenza: `/home/user/m3d-scatti/<variante>/` (scatti e log delle due esecuzioni, `mancino/` dentro), `/home/user/m3d-scatti/<variante>.log` (uscita dell'esecuzione), `<variante>.controlli.txt` (i controlli rifatti con lo script del commit).

## Che cosa ho fatto

- **Step 1.** Dieci esecuzioni di `cand` (nove su 1.21.1-neoforge, una su 1.20.1-forge) piu' lo scatto in sopravvivenza; le due costanti di `client/FirstPersonArm` con i valori scelti e, nel Javadoc, la ragione con le misure del candidato scelto e degli scartati. Nessun'altra riga Java toccata.
- **Step 2.** `EXO_SHOT_MAIN_HAND` in `tools/client/client_shot.sh` (controllo nella sezione dell'ambiente con `fatal`, riga `mainHand:"<valore>"` nel blocco di `options.txt`, commento di testa: elenco delle variabili e delle cause dell'uscita 2, commento del blocco delle opzioni). `tools/client/m3d_shots.sh` (nuovo, eseguibile): due esecuzioni, quattordici controlli, soglie in testa con ragione e valore misurato, `M3D_SOLO_CONTROLLI=1`.
- **Step 3.** La sequenza sulle quattro varianti, una alla volta: quattordici `ok` e `OK` su ciascuna.
- **Step 4.** `tools/client/README.md`: due righe d'uso, il paragrafo di `m3d_shots.sh` (scatti, tabella dei quattordici controlli, uscite, che cosa non prova), il paragrafo di `arm_measure.sh` (i tre modi e `--selftest`, le definizioni di "differisce" e "magenta", le regole degli scatti misurati), la riga di `EXO_SHOT_MAIN_HAND` nella tabella delle variabili e nella frase dell'uscita 2, le durate.
- **Step 5 e 6.** Controlli di fine task sul codice del commit, commit.

## File cambiati

Creato: `tools/client/m3d_shots.sh` (380 righe, modo 100755).
Modificati: `src/main/java/com/toxineren/exoframes/client/FirstPersonArm.java` (le due costanti e il loro Javadoc), `tools/client/client_shot.sh` (solo `EXO_SHOT_MAIN_HAND`), `tools/client/README.md`.
`git status --short` dopo il commit: vuoto (scatti e candidati stanno fuori dal repository).

## Step 1: le misure (`/home/user/m3d-candidati/misure.txt`)

Controllo dello strumento, prima esecuzione (e uguale in tutte e dieci): `base: pixel=18075 copertura=3.49 mirino=0 barra=0 energia=0 riquadro=161x149+677+391`: almeno 10000 pixel, nulla nei tre rettangoli, riquadro nel quarto in basso a destra. Lo strumento va.

| Prova | pezzi (`sagoma con senza`) | nudo (`sagoma nudo senza`) | Esito |
|---|---|---|---|
| k165-s00 | copertura 13,26 barra 13175 riquadro 469x237+491+303 | 11,87 barra 7965 398x237+562+303 | cade su (b) |
| k130-s00 | 9,61 barra 8468 406x205+554+335 | 8,55 barra 4469 351x205+609+335 | cade su (b) |
| k100-s00 | 7,05 barra 4868 358x177+602+363 | 6,00 barra 1877 288x177+638+363 | cade su (b) |
| k165-s60 | 10,74 barra 731 300x237+660+303 | 9,43 barra 0 266x237+694+303 | cade su (b): 6,5 e' il minimo a 1,65 |
| k165-s65 | 10,33 barra 0 288x237+672+303 | 9,10 barra 0 257x237+703+303 | (b) e (c) si', **(a) no**: la mano tagliata dal bordo destro |
| k130-s40 | 8,99 barra 634 298x205+662+335 | 7,94 barra 0 270x205+690+335 | cade su (b): 4,5 e' il minimo a 1,3 |
| **k130-s45** | **8,77 barra 0 286x205+674+335** | **7,72 barra 0 260x205+700+335** | **passa (a), (b), (c): scelto** |
| k130-s45 su 1.20.1-forge | 8,77 barra 0 286x205+674+335 | 7,72 barra 0 260x205+700+335 | uguale al pixel |

In ogni riga `mirino=0` ed `energia=0`.

- **Giro 1:** nessun K passa, come prevedeva il piano: l'avambraccio finisce dietro la barra degli oggetti (179, 116 e 68 pixel dentro il rettangolo, con i pezzi, a 1,65, 1,3 e 1,0).
- **Giro 2, K 1,65:** spostamento minimo 6,5 (a 6,0 `barra=731`), e li' il bordo destro taglia la mano: in `k165-s65-nudo.png` il palmo d'acciaio arriva a x 960 e il pollice resta fuori. Scartato per (a). Uno spostamento maggiore la taglierebbe di piu', uno minore non libera la barra.
- **Giro 2, K 1,3:** spostamento minimo 4,5 (a 4,0 `barra=634`); a 4,5 il braccio resta 4 pixel fuori dal rettangolo (riquadro da X 674, il rettangolo finisce a 670). (c): 8,77 e 7,72. (a): vedi sotto. **Scelto.** K 1,0 con lo spostamento non l'ho misurato: il brief sceglie il K piu' grande che passa.
- Mezzo pixel di modello sposta il braccio di 12 pixel dello schermo (a 1,65 e a 1,3).
- Previsione del piano (6,5 per 1,65, 4,5 per 1,3, il pugno tagliato a 1,65): confermata tutta. Il 3 per K 1,0 non e' stato provato.

**Il giudizio su (a) per il candidato scelto** (guardati `k130-s45/con.png`, `nudo.png` e i loro ingrandimenti): la mano e' intera sullo schermo, vista da dietro e dall'alto come il gioco mostra il suo braccio: la fila di nocche d'ottone, le tre dita scure oltre le nocche, il dorso del palmo d'acciaio e il pollice scuro a destra; il punto piu' a destra della mano e' a x 880 circa, 80 pixel dal bordo; sotto c'e' un tratto di avambraccio che esce dal bordo destro da y 415 circa e dal bordo in basso. Con i pezzi la piastra del dorso copre il palmo; nocche, dita e pollice restano in vista. Nessuna parte della mano tocca un bordo. Riquadro nel quarto in basso a destra (X 674, Y 335).

`scelto-sopravvivenza.png` (pezzi con i loro colori, barre di stato): il braccio in basso a destra, fuori dalla barra degli oggetti e dalle barre di stato.

`./gradlew --console=plain :1.21.1-neoforge:test --tests '*FirstPersonArmTest'` con le costanti scelte: `BUILD SUCCESSFUL`, `tests="6" skipped="0" failures="0" errors="0"`.

## Cio' che la nota del registro chiedeva di riferire

- **Formato di `mainHand` in `options.txt`:** `mainHand:"left"`, con le virgolette, sulle due versioni; dopo l'esecuzione del mancino il client riscrive il file con la stessa riga sulle quattro varianti, e `mancino-controllo` trova il braccio del gioco a sinistra (`riquadro=161x149+122+391`, lo specchio di `161x149+677+391`).
- **L'avviso "Chat messages can't be verified" (T1-F1):** misurato con uno scatto al secondo (una esecuzione su 1.21.1-neoforge, 1.20.1-fabric, 1.20.1-forge; differenza nel rettangolo `460x110+500+0` contro l'ultimo scatto): intero (50600 pixel) fino a 4,6 s dopo il primo passo, in uscita a 5,8 s (6842-12618) e a 7,0 s (394-2388), sparito a 8,1 s. La riga in chat del `gamemode creative` sparisce fra 3,5 e 4,6 s. Con `@attendi 8` il primo scatto misurato verrebbe meno di un secondo dopo la fine dell'avviso: nella sequenza il primo passo e' `@attendi 12` (vedi "Scostamenti"). Le dieci esecuzioni dello Step 1, fatte con `@attendi 8` come dice il brief, hanno tutte la riga `base` pulita (riquadro `161x149+677+391`, 18075 pixel): l'avviso non c'era in nessuno scatto misurato.
- **K, spostamento e soglie contro la previsione:** K 1,3 e 4,5 come previsto; nessuna soglia del brief e' stata cambiata (ogni larghezza misurata supera la sua soglia di piu' di un quarto: 318 contro 130, 98 contro 70, 258 contro 120). `MAGENTA_MIN` = 19900 (la meta' di 39962, per difetto al centinaio).

## Step 2 e 3: la sequenza sulle quattro varianti

`bash -n tools/client/m3d_shots.sh`: nessuna uscita (`shellcheck`: nessun avviso).
`EXO_ACCEPT_EULA=true EXO_SHOT_MAIN_HAND=centro bash tools/client/client_shot.sh 1.21.1-neoforge <cartella>; echo $?`:
```
ERRORE: EXO_SHOT_MAIN_HAND: si scrive left oppure right, non 'centro'
2
```
(la cartella non viene creata, nulla parte).

Prima esecuzione su 1.21.1-neoforge con `MAGENTA_MIN=1000`: quattordici `ok`. Fissate le soglie, `M3D_SOLO_CONTROLLI=1`: quattordici `ok`. Poi le quattro varianti con le soglie definitive (l'ultima, 1.21.1-neoforge, lanciata con `EXO_SHOT_MAIN_HAND=left` nell'ambiente: lo script la imposta da se', l'esecuzione A resta destra):

| Variante | durata | esito | `log-check.txt` (A e B) |
|---|---|---|---|
| 1.20.1-forge | 224 s (A 119, B 85) | 14 `ok`, `OK 1.20.1-forge: scatti in /home/user/m3d-scatti/1.20.1-forge`, uscita 0 | vuoti |
| 1.20.1-fabric | 210 s (A 114, B 76) | 14 `ok`, `OK 1.20.1-fabric: scatti in /home/user/m3d-scatti/1.20.1-fabric`, uscita 0 | vuoti |
| 1.21.1-fabric | 219 s (A 120, B 78) | 14 `ok`, `OK 1.21.1-fabric: scatti in /home/user/m3d-scatti/1.21.1-fabric`, uscita 0 | vuoti |
| 1.21.1-neoforge | 215 s (A 115, B 80) | 14 `ok`, `OK 1.21.1-neoforge: scatti in /home/user/m3d-scatti/1.21.1-neoforge`, uscita 0 | vuoti |

Nessuna riga `Suit rendering failed` negli otto `client.log`. Le righe dei controlli, uguali sulle quattro varianti salvo dove detto:
```
ok controllo: pixel=18075 copertura=3.49 mirino=0 barra=0 energia=0 riquadro=161x149+677+391
ok dipinto: magenta=39962 riquadro=286x188+674+352; meta' sinistra magenta=0 riquadro=0x0+0+0
ok criteri: pixel=45442 copertura=8.77 mirino=0 barra=0 energia=0 riquadro=286x205+674+335
ok solo-tuta: sagoma=18075 uguali=0
ok nudo: magenta=0 riquadro=0x0+0+0; pixel=40026 copertura=7.72 mirino=0 barra=0 energia=0 riquadro=260x205+700+335; sagoma=18075 uguali=0
ok bastone: magenta=0 riquadro=0x0+0+0
ok mappa: sinistra magenta=28407 riquadro=365x179+0+361; destra magenta=28977 riquadro=369x182+591+358
ok terza-persona: magenta=23595 riquadro=318x312+321+221
ok inventario: magenta=10782 riquadro=98x140+356+120
ok senza-tuta: pixel=0 copertura=0.00 mirino=0 barra=0 energia=0 riquadro=0x0+0+0
ok telaio-piazzato: pixel=52142 copertura=10.06 mirino=3600 barra=0 energia=0 riquadro=258x284+351+113; magenta=39962 riquadro=286x188+674+352, il 100 % di braccio-rottame
ok mancino-controllo: pixel=18075 copertura=3.49 mirino=0 barra=0 energia=0 riquadro=161x149+122+391
ok mancino: meta' destra magenta=0 riquadro=0x0+0+0; pixel=45442 copertura=8.77 mirino=0 barra=0 energia=0 riquadro=286x205+0+335
ok specchio: mancino magenta=39962 riquadro=286x188+0+352, il 100 % del destro (magenta=39962 riquadro=286x188+674+352); riquadro specchiato 286x188+674+352, scarti sinistra 0 alto 0 destra 0 basso 0
OK 1.21.1-neoforge: scatti in /home/user/m3d-scatti/1.21.1-neoforge
```
Differenze fra varianti (e fra due esecuzioni della stessa): `terza-persona` magenta 23595-23745, sempre `318x312+321+221`; `inventario` su 1.21.1 `98x140+356+120` (magenta 10782-10843: 1.21.1 ritaglia l'anteprima nel suo riquadro), su 1.20.1-forge `magenta=18816 riquadro=172x202+320+66`, su 1.20.1-fabric `magenta=18778 riquadro=168x202+322+66`.

Dopo le ultime correzioni di forma allo script (righe lunghe, commenti) ho rifatto i controlli con lo script del commit sugli scatti delle quattro varianti (`M3D_SOLO_CONTROLLI=1`): quattro volte quattordici `ok`, `OK`, uscita 0 (`/home/user/m3d-scatti/<variante>.controlli.txt`).

Il ramo che fallisce, provato su una copia degli scatti manomessa (in scratchpad): `braccio-rottame` e `braccio-nudo` sostituiti da `base-prima`, un file illeggibile al posto di `terza-persona`, un braccio destro al posto di quello del mancino: `KO` per `dipinto`, `criteri`, `solo-tuta` (`sagoma=18075 uguali=18075`), `nudo`, `terza-persona` (`misura-non-riuscita`), `telaio-piazzato`, `mancino`, `specchio`, gli altri `ok`, ultima riga `FAIL 1.21.1-neoforge: controlli falliti: dipinto criteri solo-tuta nudo terza-persona telaio-piazzato mancino specchio`, uscita 1; tolto uno scatto: `FAIL 1.21.1-neoforge: mancano gli scatti: mappa`, uscita 1; con un argomento solo: la riga `Uso:` e uscita 2.

Scatti guardati (1.21.1-neoforge): `braccio-rottame` (il braccio con le piastre magenta in basso a destra, grande, fuori dalla barra), `braccio-nudo` (il braccio d'acciaio del telaio con le nocche d'ottone, nessun braccio di pelle dentro), `mappa` (le due braccia della tuta ai lati, in basso; la mappa sta davanti alle braccia, intera, e le braccia non la coprono: si legge; e' una mappa vuota, senza disegno), `mancino/braccio-mancino` (lo specchio del destro, a sinistra), `terza-persona` (la tuta intera da dietro), `inventario` (la tuta intera nell'anteprima), `telaio-piazzato` (il telaio Rottame intero cinque blocchi davanti, con il braccio magenta sullo schermo), `bastone` (il bastone, nessun braccio).

## Controlli di fine task (Step 5), sul codice del commit

Rifatti tutti dopo l'ultima modifica (una frase di Javadoc), prima del commit.
- Build, comando dei Global Constraints: `BUILD SUCCESSFUL in 6s`, `52 actionable tasks: 18 executed, 34 up-to-date` (la build completa precedente: `BUILD SUCCESSFUL in 29s`); nessun `warning:` ne' `error:` nel log.
- Conto dei JUnit:
  ```
  1.20.1-forge 331
  1.20.1-fabric 331
  1.21.1-fabric 331
  1.21.1-neoforge 331
  ```
- `bash tools/check_sources.sh` → `OK sources`
- `bash tools/check_jars.sh` → `OK jars`
- `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh`, uscita 0:
  ```
  OK 1.20.1-forge: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 208 test obbligatori passati, nessun avviso fuori elenco
  ```
- `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure`
- `git status --short` prima del commit: solo i quattro file del task; dopo: vuoto.

## Prove TDD

Il task non aggiunge test (i JUnit restano 331). Il test che fissa le costanti c'era gia' (`constantsAreAmongTheDecidedValuesAndTheLeftArmMirrorsTheRight`) e passa con i valori scelti. La prova della sequenza e' la sequenza stessa: RED non c'e' nel senso di JUnit; il ramo `KO` dei controlli l'ho provato sugli scatti manomessi (sopra), GREEN sono le quattro esecuzioni.

## Scelte

- **(a) si giudica sulla mano non tagliata dal bordo dello schermo.** Dal punto di vista del gioco la mano si vede da dietro e dall'alto: nocche, dita oltre le nocche, dorso del palmo, pollice. Il brief elenca le parti e dice "nessuno tagliato dal bordo dello schermo"; il design dice "il pugno e un tratto di avambraccio sono sullo schermo". Ho giudicato cosi': passa se nessuna parte della mano tocca un bordo dello schermo e le parti si riconoscono nello scatto del telaio nudo. Con i pezzi la piastra del dorso copre il palmo: e' la tuta che copre se stessa, non il bordo.
- **Fermarsi al primo K che passa nel giro 2** (1,3): il brief dice "per K dal piu' grande al piu' piccolo" e "il K piu' grande che passa"; K 1,0 con lo spostamento non serve alla scelta.
- **Una prova in piu' su 1.20.1-forge** (`k130-s45-forge`) prima di costruire le soglie: il margine su (b) e' di 4 pixel, e un fallimento allo Step 3 avrebbe rifatto tutto. Misure uguali al pixel.
- **Se l'esecuzione A fallisce la B non parte**, e i controlli girano solo se ci sono tutti e quindici gli scatti (all'avvio lo script toglie i quattro scatti di `mancino/`, cosi' non si misurano quelli di una volta prima). Il brief dice solo "i controlli girano solo se le due esecuzioni hanno salvato gli scatti".
- **Una misura che non riesce** (`arm_measure.sh` esce con errore) rende `KO` il controllo che la usa, con `misura-non-riuscita` al posto della misura.
- **`specchio`, "entro 12 pixel per lato":** i quattro lati del riquadro (sinistra, alto, destra, basso) del mancino specchiato contro quelli del destro.
- **`mancino-controllo`:** oltre alla riga `KO`, il testo "atteso" dice che e' un problema dello strumento; lo stesso per `controllo`.
- **Nomi delle prove del giro 2:** `k<K>-s<spostamento>` come nel brief (`k130-s45`); le copie dei `con.png` portano quel nome, e ho copiato anche i `nudo.png` (`-nudo.png`), che servono a giudicare (a).
- **Javadoc:** le misure dei candidati stanno in `SCALE` (un elenco), quelle del minimo dello spostamento in `SIDE_SHIFT_PX`.
- **Commento di testa di `client_shot.sh`:** oltre all'elenco delle variabili, `EXO_SHOT_MAIN_HAND` e' nominata fra le cause dell'uscita 2 e nel commento del blocco di `options.txt`.

## Scostamenti dal brief

- **Il primo passo delle due esecuzioni di `m3d_shots.sh` e' `@attendi 12`, non `@attendi 8`.** Ragione: l'avviso "Chat messages can't be verified" (T1-F1, senza una decisione nel registro) sparisce fra 7,0 e 8,1 s dopo il primo passo, misurato su tre varianti; con 8 s il margine e' sotto il secondo, con 12 almeno 4. `base-prima` deve essere senza avviso anche perche' `senza-tuta` la confronta con uno scatto di un minuto dopo. Il numero sta in una costante (`FIRST_WAIT`) con la ragione nel commento di testa, e il README lo dice fra le regole degli scatti misurati. Costo: 8 s in piu' per variante. Se l'avviso restasse in uno solo dei due scatti di una coppia, `controllo`, `criteri` o `mancino-controllo` fallirebbero (il riquadro partirebbe da Y 0). Lo Step 1 l'ho fatto con `@attendi 8` come scritto, e ogni riga `base` prova che l'avviso non c'era.
- Nient'altro: passi, comandi, rettangoli, soglie e nomi sono quelli del brief.

## Autorevisione

- Completezza: ogni voce di "Produces" c'e' (costanti e Javadoc; `misure.txt`, `k165.png`, `k130.png`, `k100.png`, `scelto-sopravvivenza.png`; `EXO_SHOT_MAIN_HAND` nei tre punti; i quattordici controlli con i nomi del brief; gli undici e i quattro scatti; i paragrafi del README).
- Review Focus: 1 `terza-persona`, `inventario`, `telaio-piazzato` verdi sulle quattro; 2 `criteri` sul primo scatto del braccio, prima di ogni F5 (copertura 8,77); 3 `solo-tuta` e `nudo` (`uguali=0`); 4 le quattro varianti partono e disegnano; 6 `mancino-controllo`, `mancino`, `specchio` (specchio esatto al pixel); 7 `mappa`; 8 `nudo`; 9 `senza-tuta` (`pixel=0`) e `bastone`.
- Regole: nessuna riga di log nuova; `log-check.txt` vuoti; commenti in italiano con apostrofi ASCII, README con gli accenti come il resto del file; nessun dato personale; nessun test aggiunto; nessun file fuori dai quattro del task.
- Corretto durante l'autorevisione: righe oltre 120 colonne nello script; la frase del Javadoc che diceva le misure provate solo su due varianti (ora quattro); la riga di `misure.txt` che per `k165-s00` confondeva (a) con (b).

## Dubbi

- **Il giudizio su (a) e' a occhio** (lo dice il piano), e il brief chiede "le tre dita e il pollice scuri" visibili: nello scatto del telaio nudo si distinguono le tre dita oltre le nocche e un blocco scuro a destra che leggo come il pollice; con i pezzi il palmo e' coperto dalla piastra del dorso. Sono pochi pixel. Se chi coordina, guardando `k130-s45.png`, `k130-s45-nudo.png` e `scelto-sopravvivenza.png`, giudica che la mano non si distingue abbastanza, nessun candidato passerebbe e varrebbe il caso "non passa nessuno" del brief (K 1,0 senza spostamento, decisione a chi coordina).
- Il margine su (b) e' di 4 pixel dello schermo (per costruzione: lo spostamento minimo). Vale a 960x540, scala dell'interfaccia 2, campo visivo predefinito, come dice il design; con un'altra finestra o un altro campo visivo il braccio puo' toccare la barra, e nessuna prova qui lo misura.
- Processi: alla fine restano solo i demoni di Gradle in attesa; nessun server, client o Xvfb.

## Fix round 1

Stato: **BLOCKED**: nessun candidato passa il criterio (a) come lo scrive il brief. Le costanti sono al valore provvisorio del piano per questo caso, K 1,0 e spostamento 0 (brief, Step 1, "Se non passa nessuno nemmeno cosi'"; ruling 13 del registro). FIX_BASE a8c6c8fbf80288809a8e02e3640cef68ad875510, HEAD 9efe9a7189be9c73303fa7acddb790ac2280494f. Un commit: `9efe9a7 client: braccio in prima persona, K 1,0 in attesa di una decisione`. Il working tree all'inizio era pulito: non c'era nessun tentativo interrotto da riprendere.

### T2-F1: il giudizio su (a) di K 1,3 con 4,5 (corretto)

- **Il finding regge.** Ho guardato gli stessi ingrandimenti (`k130-s45-nudo.png` e `k165-s65-nudo.png`, ritaglio 300x220+660+320 ingrandito 3 volte). In tutti e due lo stesso blocco d'acciaio, con il filo bianco e poi lo scuro, arriva al bordo destro. Gli "80 pixel dal bordo" del primo rapporto erano l'estensione delle dita, non della mano. Il pollice non lo distinguo in nessuno dei due.
- **Misura del bordo**, nuova e uguale per tutti i candidati: la prima riga y in cui lo scatto differisce da `senza.png` nelle colonne x 952-959 (stessa definizione di "differisce" di `arm_measure.sh`, soglia 4 %).

  | Prova | nudo | con i pezzi |
  |---|---|---|
  | k165-s65 | da y 361 | da y 336 |
  | k130-s45 | da y 418 | da y 407 |
  | k100-s30 | da y 465 | da y 463 |
  | k100-s00 | mai | da y 514 |

- **Giro 2 a K 1,0, fatto nel client vero** (1.21.1-neoforge, la funzione `cand` del brief, e la riga `base` sempre `pixel=18075 … riquadro=161x149+677+391`):
  - `k100-s30`: pezzi copertura 6,85, `barra=0`, riquadro 279x177+681+363; nudo 6,17, `barra=0`, 259x177+701+363.
  - `k100-s25`: pezzi `barra=14`, riquadro 291x177+669+363; nudo `barra=0`. Quindi a K 1,0 lo spostamento minimo e' 3,0, come prevedeva il piano.
  - (c) vale a 3,0. (a) no: si vedono la fila di nocche d'ottone e le tre dita scure, ma il blocco d'acciaio del palmo arriva al bordo destro da y 465 e il pollice non si distingue. Nel dubbio non passa.
  - Copie in `/home/user/m3d-candidati/`: `k100-s30.png`, `k100-s30-nudo.png`, `k100-s25.png`, `k100-s25-nudo.png`, con le sottocartelle e i `.run.log`.
- **`misure.txt`:** ho aggiunto righe in coda, senza riscrivere quelle di prima:
  - le misure di `k100-s30` e `k100-s25`;
  - il giudizio (a) di `k100-s30` e `k100-s25`;
  - una correzione per `k130-s45` (non passa);
  - una precisazione per `k165-s65` (il pollice non "resta fuori": non si distingue);
  - la riga `esito`.
- **Esito, sul brief cosi' com'e':** nessun candidato passa.
  - Lo spostamento zero, dove ogni K cade su (b), e' anche il solo caso in cui uno scatto del telaio nudo non tocca il bordo destro, misurato solo per `k100-s00`. Il bordo di `k165-s00` e `k130-s00` non l'ho misurato: i loro riquadri `nudo` arrivano a x 960 (562+398, 609+351), e quale parte tocchi li' nessuno l'ha guardato.
  - Il pollice non si distingue in nessuno scatto.
  - Ho preso il ramo "nessuno passa": `SCALE = 1.0f`, `SIDE_SHIFT_PX = 0f`.
- **Il Javadoc delle due costanti** (`src/main/java/com/toxineren/exoframes/client/FirstPersonArm.java`), riscritto:
  - dice che nessun candidato passa e che 1,0 e 0 sono il valore provvisorio in attesa di una decisione;
  - riporta le misure di tutti i candidati: giro 1, i tre minimi del giro 2 con la prova mezzo pixel sotto, e la riga del bordo;
  - non dice piu' "il pollice resta fuori" ne' "a 80 pixel dal bordo";
  - `SIDE_SHIFT_PX` non si dice piu' "il minimo che libera la barra".
  - Che il blocco al bordo sia il palmo l'ho riconosciuto a occhio negli ingrandimenti, e coincide con il modello della revisione. Le altre inferenze del modello, per esempio "il pollice coperto dal palmo", non vi sono scritte come fatto: il Javadoc dice solo che il pollice non si distingue.
- **Rispetto alla nota della correzione** ("non cambiare le costanti su questo finding finche' T2-F2 non e' deciso"):
  - Non ho scelto un nuovo vincitore con una mia lettura di (a). Per esempio 1,65 con 6,5, che passerebbe con la lettura "il pugno = nocche e dita".
  - 1,0 e 0 sono il ramo che il brief e il ruling 13 danno per "nessuno passa", cioe' il passo 3 della correzione stessa.
  - Quando T2-F2 sara' deciso, le costanti andranno al valore che ne segue.

### Cio' che dipende da K e non ho toccato (concern)

Con 1,0 e 0 il controllo `criteri` di `m3d_shots.sh` non puo' passare, come dice il brief. Non ho rifatto la sequenza ne' ritoccato le soglie. Restano i numeri misurati a K 1,3 con 4,5:
- in `tools/client/m3d_shots.sh`:
  - `MAGENTA_MIN=19900` (riga 94) e il suo commento "misurato 39962" (riga 93);
  - i commenti con "misurati 8,77 … 7,72" (riga 96);
  - i valori di `telaio-piazzato` e `specchio` (righe 119-125);
- in `tools/client/README.md`, la tabella dei controlli (righe 128-140): 39 962, 8,77, 18 075/0, 28 407/28 977, 258.

Le soglie di larghezza e quella del magenta vanno rifissate quando K sara' deciso, rifacendo gli Step 2 e 3 sul valore scelto. Mostrano ancora K 1,3 con 4,5:
- `/home/user/m3d-candidati/scelto-sopravvivenza.png` e `scelto/`;
- tutti gli scatti di `/home/user/m3d-scatti/`.

### Test rifatti, con i comandi e le ultime righe

- `./gradlew --console=plain :1.21.1-neoforge:test --tests '*FirstPersonArmTest'` → `BUILD SUCCESSFUL in 5s`; `tests="6" skipped="0" failures="0" errors="0"`. `constantsAreAmongTheDecidedValuesAndTheLeftArmMirrorsTheRight` accetta 1,0 e 0.
- Build e collectJar delle quattro varianti, con il comando dei Global Constraints → `BUILD SUCCESSFUL in 31s`, `52 actionable tasks: 36 executed, 16 up-to-date`. Nel log nessun `warning:` e nessun `error:`.
- Conto dei JUnit:
  ```
  1.20.1-forge 331
  1.20.1-fabric 331
  1.21.1-fabric 331
  1.21.1-neoforge 331
  ```
- `bash tools/check_sources.sh` → `OK sources`
- `bash tools/check_jars.sh` → `OK jars`
- `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure`
- `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh`, uscita 0:
  ```
  OK 1.20.1-forge: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 208 test obbligatori passati, nessun avviso fuori elenco
  ```
- `git status --short` dopo il commit: vuoto. Alla fine non restano server, client o Xvfb.
- I client veri di questo giro sono due esecuzioni di `cand`, `k100-s30` e `k100-s25`, su 1.21.1-neoforge. Sono uscite con 0 e hanno `log-check.txt` vuoto.

### Per chi coordina (T2-F2)

La decisione che serve e' che cosa chiede (a) nella vista che il gioco da'. Le due letture, con i dati misurati:
- **Lettura stretta, il testo del brief** (nessuna parte della mano al bordo, pollice visibile): nessun candidato passa. Resta 1,0 con 0, e l'avambraccio passa dietro la barra degli oggetti (pezzi `barra=4868`).
- **Lettura "il pugno = nocche e tre dita sullo schermo", il palmo puo' uscire dal bordo:** passerebbero i tre minimi del giro 2. A 1,65 con 6,5 l'ho guardato solo a occhio: in `k165-s65-nudo.png` nocche e dita stanno sullo schermo, con l'erba a destra. Il modello della revisione mette le dita fino a x 936 e le nocche fino a x 910. Con "il K piu' grande" si sceglie 1,65 con 6,5: copertura 10,33 (9,10), barra 0, riquadro 288x237+672+303.

Dopo la decisione, vanno rifatti:
- le due costanti con il loro Javadoc;
- lo Step 1 `scelto-sopravvivenza.png`;
- gli Step 2 e 3 della sequenza: soglie, `MAGENTA_MIN`, i numeri nel README;
- poi il Task 3.

## Fix round 1 (T2-R1, dopo le decisioni di chi coordina su T2-F1 e T2-F2)

Stato: **DONE**. FIX_BASE 9efe9a7189be9c73303fa7acddb790ac2280494f, HEAD 3a7c47ad95817dc2c7a2843e20d2142c0533c7b2. Un commit, firmato dall'ambiente: `3a7c47a client: braccio in prima persona a K 1,3 con lo spostamento 4,5, scelti da chi coordina`. All'inizio il working tree era pulito: nessun tentativo interrotto da riprendere. Nessun processo rimasto da prima (solo il demone di Gradle).

### T2-R1: che cosa ho cambiato

1. **Costanti** (`src/main/java/com/toxineren/exoframes/client/FirstPersonArm.java`): `SCALE = 1.3f`, `SIDE_SHIFT_PX = 4.5f`. Nessun'altra riga Java.
2. **Javadoc di `SCALE`**, riscritto:
   - (a) come l'ha decisa chi coordina: nocche e dita del pugno intere sullo schermo, un tratto di avambraccio in vista; palmo e avambraccio possono uscire dal bordo destro o da quello in basso; il pollice non serve.
   - La scelta: 1,3 con 4,5 l'ha scelto chi coordina guardando gli scatti, fra le grandezze che passano, non "il K piu' grande".
   - Per ogni candidato che cosa mostrano gli scatti, con le misure di tutti: il giro 1 (tre K, cadono su (b)); 1,3 con 4,5 (scelto: dita fino a x 886, palmo d'acciaio al bordo destro da y 420 circa, "bordo da y" 407 con i pezzi e 418 nudo, il pollice non si vede); 1,65 con 6,5 (passa, un decimo dello schermo, palmo al bordo da meta' altezza); 1,0 con 3,0 (passa; a 2,5 barra 14). La frase sbagliata "la mano intera a 80 pixel dal bordo destro" non c'e' piu'.
   - Ho tenuto le due misure del bordo, ciascuna con il suo nome: "da y 420 circa" e' quello che si vede nello scatto nudo (la decisione e la revisione); "bordo da y 407 (418)" e' la prima riga che differisce nelle colonne x 952-959, misurata con la soglia di `arm_measure.sh` (con i pezzi e' la piastra del dorso).
3. **Javadoc di `SIDE_SHIFT_PX`**: di nuovo il minimo che libera la barra a K 1,3 (4,0 barra 634; 4,5 fuori di 4 pixel), piu' i minimi degli altri due K (6,5 a 1,65 e 3,0 a 1,0, mezzo pixel sotto barra 731 e 14).
4. **JUnit**: nessun test fissa il testo del Javadoc (grep in `src/test`); `constantsAreAmongTheDecidedValuesAndTheLeftArmMirrorsTheRight` accetta 1,3 e 4,5. Nessun test aggiunto o cambiato.
5. **`tools/client/m3d_shots.sh` e `tools/client/README.md`**: le soglie e i valori accanto (39962, 19900, 8,77, 7,72, 18075/0, 28407/28977, 318, 258, 100 %) erano gia' quelli di K 1,3 con 4,5, e l'ultimo commit non li aveva toccati: nulla di stantio. Nessuna frase su "mano intera", "pollice" o "il piu' grande". Ho aggiornato solo le misure che questo giro ha portato fuori dagli intervalli scritti: le durate (208-226 s, A 112-121 s, B 76-85 s; prima 210-224 e 114-120) e la larghezza dell'anteprima dell'inventario su 1.20.1 (Forge 170x202+321+66 magenta 18796, Fabric 166x202+323+66 magenta 18712; scritto prima 172 e 168), nel commento di testa, nel commento di `INVENTORY_MIN_W` e nella tabella del README. Le soglie non cambiano.
6. **`/home/user/m3d-candidati/scelto-sopravvivenza.png`**: rifatto (una esecuzione di `client_shot.sh` con il comando del brief, 82 s, uscita 0, `log-check.txt` vuoto). Mostra il braccio della tuta con i suoi colori in basso a destra, fuori dalla barra degli oggetti e dalle barre di stato. In `misure.txt` una riga in coda con l'esito della decisione (nulla riscritto).

### La sequenza sulle quattro varianti, una alla volta

`EXO_ACCEPT_EULA=true bash tools/client/m3d_shots.sh <variante> /home/user/m3d-scatti/<variante>`, uscita in `/home/user/m3d-scatti/<variante>.log`:

| Variante | durata | esito | `log-check.txt` (A e B) |
|---|---|---|---|
| 1.20.1-forge | 226 s (A 121, B 85) | 14 `ok`, `OK 1.20.1-forge: scatti in /home/user/m3d-scatti/1.20.1-forge`, uscita 0 | 0 byte, 0 byte |
| 1.20.1-fabric | 208 s (A 112, B 76) | 14 `ok`, `OK 1.20.1-fabric: …`, uscita 0 | 0, 0 |
| 1.21.1-fabric | 209 s (A 112, B 77) | 14 `ok`, `OK 1.21.1-fabric: …`, uscita 0 | 0, 0 |
| 1.21.1-neoforge | 214 s (A 113, B 81) | 14 `ok`, `OK 1.21.1-neoforge: …`, uscita 0 | 0, 0 |

Nessuna riga `Suit rendering failed` negli otto `client.log`. Le misure sono quelle del primo rapporto, al pixel (`criteri` 8,77 riquadro 286x205+674+335, `nudo` 7,72 260x205+700+335, `dipinto` 39962, `specchio` scarti 0 sui quattro lati, `telaio-piazzato` 258 e 100 %), salvo i due magenta che gia' variavano: `terza-persona` 23658-23732 (sempre 318x312+321+221), `inventario` 10792-10804 su 1.21.1 (98x140+356+120), 18796 e 18712 su 1.20.1 (sopra). Ultime righe su 1.21.1-neoforge:
```
ok mancino: meta' destra magenta=0 riquadro=0x0+0+0; pixel=45442 copertura=8.77 mirino=0 barra=0 energia=0 riquadro=286x205+0+335
ok specchio: mancino magenta=39962 riquadro=286x188+0+352, il 100 % del destro (magenta=39962 riquadro=286x188+674+352); riquadro specchiato 286x188+674+352, scarti sinistra 0 alto 0 destra 0 basso 0
OK 1.21.1-neoforge: scatti in /home/user/m3d-scatti/1.21.1-neoforge
```
Dopo le modifiche ai soli commenti dello script, `M3D_SOLO_CONTROLLI=1` sulle quattro cartelle: quattro volte 14 `ok`, `OK`, uscita 0 (`/home/user/m3d-scatti/<variante>.controlli.txt`); `bash -n` nessuna uscita; nessuna riga oltre 120 colonne.

### Test e controlli di fine task, con i comandi e le ultime righe

- `./gradlew --console=plain :1.21.1-neoforge:test --tests '*FirstPersonArmTest'` (test eseguito, non dalla cache) → `BUILD SUCCESSFUL in 3s`; `tests="6" skipped="0" failures="0" errors="0"`.
- Build e collectJar delle quattro varianti con il comando dei Global Constraints, dopo l'ultima modifica Java (dopo sono cambiati solo commenti dello script e il README) → `BUILD SUCCESSFUL in 26s`, `52 actionable tasks: 33 executed, 3 from cache, 16 up-to-date`; nessun `warning:` ne' `error:`.
- Conto dei JUnit: `1.20.1-forge 331`, `1.20.1-fabric 331`, `1.21.1-fabric 331`, `1.21.1-neoforge 331`.
- `bash tools/check_sources.sh` → `OK sources`
- `bash tools/check_jars.sh` → `OK jars`
- `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure`
- `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh`, uscita 0, 122 s:
  ```
  OK 1.20.1-forge: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 208 test obbligatori passati, nessun avviso fuori elenco
  ```
- `git status --short` dopo il commit: vuoto. Alla fine nessun server, client o Xvfb.

### Note

- Che il pollice "a ogni grandezza resta dietro il palmo" il Javadoc lo dice come lo dice la decisione; viene dal modello della revisione, negli scatti si vede solo che non si distingue.
- Restano per il Task 3, come dice la decisione: K, spostamento e misure nel par. 13 del design, e `docs/test-in-gioco/m3d-braccio.md` (nominato dal README e dallo script, non esiste ancora).
- Che 1,65 con 6,5 e 1,0 con 3,0 "passano" (a) come l'ha decisa chi coordina, il Javadoc lo dice sulla base di cio' che il giro precedente ha guardato e annotato in `misure.txt` (nocche e dita sullo schermo in `k165-s65-nudo.png` e `k100-s30-nudo.png`) e del modello della revisione; in questo giro quegli scatti non li ho riguardati, e la decisione stessa li elenca fra le grandezze che passano.
- Verificato: le righe `ok` delle quattro varianti, tolte `terza-persona` e `inventario`, sono identiche a quelle di 1.21.1-neoforge (`diff` vuoto); il commit porta la firma (`gpgsig`), tocca i soli tre file e sta sul ramo `m3d-braccio`.
