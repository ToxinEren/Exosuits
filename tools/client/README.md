# Client senza schermo

Scatti dal client vero di Minecraft, senza schermo, per vedere la tuta come la vede un giocatore: in terza persona, dalla parte 3d il braccio della tuta in prima persona e dalla parte 4a-1 le schermate della mod, cliccate con il puntatore. Il client di sviluppo della variante gira sotto Xvfb con l'OpenGL software di Mesa (llvmpipe), entra in un server dedicato locale in modalità offline e lo script comanda il server via RCON e la telecamera con tasti finti (xdotool).

**Solo Linux.** È per la sessione cloud e per macchine simili alla CI, non per il PC Windows: lì vale la checklist in `docs/test-in-gioco/`. Le due cose non si sostituiscono: questo strumento usa l'ambiente di sviluppo, la checklist i jar di produzione.

## Cosa serve

- Xvfb, xdotool, ImageMagick (`import`, `identify`, `convert`), `glxinfo` e il driver software di Mesa. Su Debian/Ubuntu: `apt-get install -y xvfb xdotool imagemagick mesa-utils libgl1-mesa-dri`.
- JDK 21 (Gradle), `python3` (client RCON, solo libreria standard), `flock` (util-linux).
- Memoria: per un'esecuzione circa 3 GB per il gioco (client 2 GB, server 1 GB) più i due demoni di Gradle che lo avviano, uno per il server e uno per il client, fino a 3 GB ciascuno (`-Xmx3G` in `gradle.properties`), che restano accesi a riposo dopo la fine: fino a circa 9 GB in tutto. Due esecuzioni insieme (due varianti, o un secondo clone) arrivano a circa 18 GB, più della macchina cloud (15 GB): una alla volta, oppure `./gradlew --stop` tra l'una e l'altra se si fa altro.
- Rete verso i Maven del progetto e verso i server delle risorse di Minecraft, solo la prima volta.

Lo script controlla all'avvio che i programmi ci siano, che Xvfb parta e che sotto Xvfb ci sia OpenGL core 3.2 o più; se manca qualcosa lo nomina ed esce con 2. Il numero dello schermo virtuale lo sceglie Xvfb (`-displayfd`): due esecuzioni partite insieme non finiscono sullo stesso schermo.

## EULA

Il server di prova parte solo con `EXO_ACCEPT_EULA=true`. Impostarla significa accettare l'[EULA di Minecraft](https://aka.ms/MinecraftEULA) per quel server locale: lo script scrive allora `eula=true` nella propria cartella del server, e in nessun altro posto. Senza la variabile si ferma e lo spiega.

## Uso

Dalla radice del repository:

```
EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <variante> <cartella> ["comando" | "@passo" ...]
EXO_ACCEPT_EULA=true bash tools/client/m3a_shots.sh <variante> <cartella>
EXO_ACCEPT_EULA=true bash tools/client/m3b_shots.sh <variante> <cartella>
EXO_ACCEPT_EULA=true bash tools/client/m3c_shots.sh <variante> <cartella>
EXO_ACCEPT_EULA=true bash tools/client/m3c_rules_shots.sh <variante> <cartella>
EXO_ACCEPT_EULA=true bash tools/client/m3c2_shots.sh <variante> <cartella>
EXO_ACCEPT_EULA=true bash tools/client/m3d_shots.sh <variante> <cartella>
EXO_ACCEPT_EULA=true bash tools/client/m4a1_shots.sh <variante> <cartella>
bash tools/client/client_shot.sh --selftest
bash tools/client/arm_measure.sh sagoma <con.png> <senza.png> | magenta <scatto.png> [<LxA+X+Y>] | uguali <a.png> <b.png> <senza.png> | --selftest
```

Varianti: `1.20.1-forge`, `1.20.1-fabric`, `1.21.1-fabric`, `1.21.1-neoforge`.

**`client_shot.sh`** fa un'esecuzione: server e client della variante, giocatore dentro (in creativa, mondo piatto, mezzogiorno, sereno, rivolto a sud in 0,5 -60 0,5), un passo avanti e ritorno per allineare il corpo alla testa (il client crea il giocatore con il corpo girato a caso, e da fermo il corpo resta indietro fino a 50 gradi: senza il passo, su 1.20.1 metà degli scatti "davanti" venivano di tre quarti), poi i comandi e i passi del client (sotto), in ordine; salto automatico spento nelle opzioni del client (`autoJump:false`), così un gradino non diventa un salto; i comandi vanno via RCON, senza la barra iniziale; il giocatore si chiama `Exo` salvo `EXO_SHOT_USER`. Esempio:

```
EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh 1.21.1-fabric /tmp/scatti/rottame "execute as Exo run exoframe debug wear rottame 2"
```

Nella cartella lascia:

- `back.png` da dietro, `front.png` davanti, `quarter.png` da dietro a tre quarti, con l'interfaccia nascosta (F1);
- `<nome>.png` per ogni passo `@scatto <nome>` (e `@tieni` con uno scatto);
- `gradle-server.log`, `gradle-client.log`: uscita di Gradle e del gioco; `server.log`, `client.log`: i `latest.log`;
- `rcon.log`: comandi e risposte; `xvfb.log`: uscita di Xvfb;
- `log-check.txt`: avvisi ed errori del mod nei due log che non sono nell'elenco di quelli voluti, `tools/log_allowlist.txt` (righe WARN/ERROR che nominano il mod o la sua entità, anche quelle di vanilla come `UUID of added entity already exists: ExoframeEntity[...]`; tracce che passano da `com.toxineren`; errori di applicazione dei mixin), o che vi compaiono un numero di volte diverso da quello ammesso, scritti da `tools/check_log.py` con la variante (`--variant`, che sceglie i conteggi per loader); vuoto se non ce ne sono. Se non è vuoto lo script, dopo aver salvato scatti e log, esce con 1.

All'inizio lo script cancella dalla cartella solo questi file, per nome (compresi gli scatti dei passi); il resto della cartella non lo tocca. `server.log` e `client.log` li copia solo se in questa esecuzione server e client sono partiti.

Ogni comando dell'utente passa da `execute store success`: se il server lo rifiuta (sintassi, bersaglio inesistente, colore non valido di `/exoframe`...) o non risponde entro 15 s lo script si ferma. Serve perché un errore dentro `execute as ... run` non arriva nella risposta RCON. Conta come rifiutato anche un comando che il server esegue ma con risultato 0, per esempio un `fill` che non cambia nessun blocco o un `execute as` senza bersagli.

### Passi del client

Fra i comandi possono stare passi che cominciano con `@`: li esegue il client, con mouse e tastiera finti (xdotool), nello stesso ordine dei comandi.

| Passo | Cosa fa |
|---|---|
| `@click destro`, `@click sinistro` | un clic del mouse dove punta il mirino (il clic destro usa l'oggetto in mano o tocca l'entità o il blocco puntati) |
| `@tieni <tasto> <secondi>` | tiene premuto un tasto per quei secondi; `<tasto>` è un nome di xdotool (`v`, `w`, `e`, `F5`, `Escape`, `space`...), anche combinato con `+` (`ctrl+w`) |
| `@tieni <tasto> <secondi> <nome> <dopo>` | come sopra, e salva `<nome>.png` `<dopo>` secondi dopo la pressione, a tasto ancora premuto (`<dopo>` minore di `<secondi>`): per esempio l'avanzamento di un tasto da tenere |
| `@scatto <nome>` | salva `<nome>.png` subito, senza attese, con l'interfaccia com'è in quel momento (visibile, salvo un F1 dei passi) |
| `@attendi <secondi>` | aspetta |
| `@punta <x> <y>` | porta il puntatore nel punto (`xdotool mousemove`) |
| `@clic <x> <y> [sinistro\|destro] [maiuscole]` | porta il puntatore nel punto e clicca, di base con il tasto sinistro; con `maiuscole` tiene premuto Maiusc sinistro (`Shift_L`) durante il clic. Il tasto va prima di `maiuscole` |
| `@trascina <x1> <y1> <x2> <y2>` | porta il puntatore in (x1, y1), preme il tasto sinistro, si sposta fino a (x2, y2) in dieci passi uguali da 0,03 s l'uno e lascia il tasto |

I tre passi del puntatore servono con una schermata aperta (un inventario, la stazione): senza schermata il gioco tiene il mouse, e muoverlo gira la telecamera. Le coordinate sono pixel della finestra, e degli scatti: interi da 0 a `EXO_SHOT_W` − 1 e da 0 a `EXO_SHOT_H` − 1, perché senza un gestore di finestre la finestra del gioco sta nell'angolo (0, 0) dello schermo virtuale, che ha la sua misura; un punto fuori dalla finestra è un passo scritto male (uscita 2). Per cliccare un punto di una schermata: con la scala dell'interfaccia 2 (quella di `options.txt`) l'interfaccia è metà della finestra (480×270 a 960×540), una schermata larga L e alta A ha l'angolo in `leftPos` = (480 − L) / 2 e `topPos` = (270 − A) / 2 (divisioni intere), e il suo punto (x, y) è il punto (2·(`leftPos` + x), 2·(`topPos` + y)) della finestra. Per esempio la schermata della stazione, 316×232, ha l'angolo in 82, 19, e il centro del suo pulsante di prova, (292, 194), è il punto (748, 426).

I secondi possono avere decimali (`0.5`). Il nome di uno scatto ha lettere, cifre, `_` e `-`, non si ripete e non può essere `back`, `front` o `quarter`. Dove guarda il giocatore e cosa tiene in mano si decide con comandi del server fra un passo e l'altro, per esempio `tp Exo 0.5 -60 0.5 0 39` (yaw 0 = sud, pitch positivo = verso il basso; gli occhi sono a 1,62 dai piedi, 2,67 nella tuta) e `item replace entity Exo weapon.mainhand with ...`. La telecamera si cambia con `@tieni F5 0.2`. Esempio:

```
EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh 1.21.1-fabric /tmp/scatti/prova "@attendi 3" "@scatto mirino" "@click destro" "@tieni F5 0.2" "@attendi 2" "@scatto terza-persona" "@tieni F5 0.2" "@tieni F5 0.2"
```

Tutti i passi si controllano prima di avviare qualunque cosa: un passo sconosciuto o scritto male (argomenti in più o in meno, secondi non numerici o zero, nome di scatto non valido o ripetuto, un punto fuori dalla finestra) ferma lo script con uscita 2 e una riga `ERRORE:` che lo nomina. Con `EXO_SHOT_STEPS_ONLY=true` lo script si ferma lì: controlla variante, cartella, passi e coordinate, stampa `OK passi: <N>` (N passi) ed esce con 0, prima di guardare sistema, programmi ed EULA e senza creare la cartella; serve a provare una sequenza di passi senza avviare nulla. `bash tools/client/client_shot.sh --selftest` lo fa da sé su una lista di passi giusti (i tre passi del puntatore in tutte le forme, e i quattro di prima) e sbagliati (punti fuori dalla finestra o negativi, coordinate mancanti o non numeriche, un tasto del mouse che non c'è, `maiuscole` prima del tasto, `@scatto` con due nomi), con la finestra di 960×540 e senza `EXO_ACCEPT_EULA`, e stampa `OK passi` (uscita 0) o una riga `FAIL:` per caso (uscita 1). I tasti dei passi `@tieni` si provano appena parte lo schermo virtuale, prima di server e client, perché xdotool con un nome sconosciuto avvisa ma esce con 0: un tasto sconosciuto dà uscita 2. Un passo che non si riesce a eseguire (client chiuso, giocatore uscito dal server, xdotool che segnala un errore, scatto quasi tutto di un colore) ferma lo script con uscita 1, e la chiusura rilascia per primi il pulsante del mouse di un `@trascina` e il tasto di un `@tieni` o di un `@clic ... maiuscole` interrotti. I tre scatti finali partono dalla prima persona con l'interfaccia visibile: chi con i passi lascia la telecamera in terza persona (F5) o l'interfaccia nascosta (F1) li cambia, quindi i passi dovrebbero rimetterle com'erano.

**`m3a_shots.sh`** fa l'insieme standard della parte 3a, una esecuzione e una sottocartella per scatto: `01-nudo`, `02-rottame-s0` … `05-rottame-s3`, `06-colori` (`3A6EA5` e `D9D9D9`), `07-mani` (piccone di ferro nella mano principale, scudo nell'altra), `08-sdraiato` (telaio nudo, posa strisciante forzata da un blocco di vetro sopra la testa; qui `front.png` non serve, perché la telecamera davanti finisce contro la testa: contano `back.png` e `quarter.png`). Negli scatti 01 e 02 c'è una colonna di tre blocchi di lana bianca a tre blocchi dal giocatore, verso ovest, come metro per l'altezza. Alla fine monta `stati-fronte.png` (le quattro viste frontali degli stati affiancate) e stampa un riepilogo. Una esecuzione per scatto invece di una lunga: ogni scatto parte dallo stesso stato e un errore resta nel suo scatto; costa un minuto in più a scatto.

**`m3b_shots.sh`** fa la sequenza della parte 3b in una sola esecuzione, con clic e tasti veri: il giocatore in sopravvivenza, con elmo e corazza di ferro, piazza il telaio con l'oggetto (clic destro sull'erba), monta elmo, braccio destro (cliccando il lato destro del telaio) e cella carica con l'oggetto in mano, entra a mano vuota, esce tenendo `V`, rientra, si sposta nella tuta, muore (`kill`) e, rinato, rientra nel telaio lasciato dalla morte e ne riesce. Dopo ogni passo uno scatto (`01-piazzato` … `15-armatura-ritrovata`, elencati in testa allo script); fra i passi comandi `execute if`/`unless` controllano dove sta l'entità telaio e quante ce ne sono, i segnaposto negli slot armatura solo mentre il giocatore è dentro, i pezzi montati a ogni ingresso (nel riassunto visivo del segnaposto del torso: elmo e braccio destro sì, braccio sinistro no), l'armatura di ferro nei suoi slot a ogni uscita, il passo fuori dal telaio a ogni uscita (il giocatore un blocco davanti al centro del telaio), niente a terra dopo la morte, e al rientro nel telaio lasciato dalla morte l'armatura che resta nel telaio (non nell'inventario) e torna nei suoi slot all'uscita; `data get entity` sul telaio scrive il suo stato in `rcon.log`. Un controllo che non torna ferma l'esecuzione con uscita 1. Alla fine i tre scatti di `client_shot.sh`, con il giocatore otto blocchi davanti al telaio.

**`m3c_shots.sh`** fa la sequenza della parte 3c-1 ("consumare") in una sola esecuzione, in sopravvivenza: il giocatore indossa la tuta Rottame (`/exoframe debug wear rottame`, cella a 10 000 RE) accanto alle costruzioni della ricognizione `docs/superpowers/specs/ricognizioni/2026-11-15-m3c-movimento-e-dati.md` (vasca profonda 3 con pareti di vetro, canale d'acqua profondo 1, una sorgente d'acqua che scorre, uno scalino di pietra alto 1). Dopo ogni prova `data get entity Exo Pos` scrive la posizione in `rcon.log` e un `execute if`/`unless entity @a[...,distance=..r]` la controlla. Le soglie sono quelle della ricognizione con le distanze moltiplicate per 0,85, perché la tuta applica `frame.baseSpeedPenalty` (dal Task 14), che la ricognizione non aveva:

| Passo | Che cosa controlla |
|---|---|
| affondare | tp sopra la vasca (Y −58): dopo 1,5 s la tuta è sul fondo, entro 0,1 da 12,5 −60 0,5 |
| Spazio sul fondo | Spazio tenuto 3 s: resta sul fondo, entro 0,1 (senza la regola dell'acqua salirebbe a −57,5); scatto `vasca-spazio` |
| avanti sul fondo | W tenuto 2 s: oltre 6 blocchi dal punto di partenza (velocità di terra della tuta, circa 7,2: 8,5 della ricognizione × 0,85; misurati 7,0-7,2) |
| acqua profonda 1 | Spazio tenuto 2 s nel canale: non si muove (entro 0,1); poi W 1,5 s verso est: esce dalla sponda alta 1 con il gradino, Y −60, entro 1,2 da 28 −60 0,5 (misurati X 27,70-27,97; senza gradino resterebbe a Y −61, X 24,70) |
| corrente | 3 s nell'acqua che scorre: non si sposta (entro 0,05) |
| scalino | W 1,5 s: sale lo scalino alto 1 senza saltare, Y −59, entro 1,5 da −4,5 −59 6,8 (misurati Z 5,76-5,88; senza gradino resterebbe a Y −60, a più di 3,9) |
| corsa | `Ctrl+W` 2 s a 999 RE (sotto il 10 %): niente scatto, fra 6 e 8,2 blocchi (misurati 7,04-7,22); a 5 000 RE lo scatto c'è, oltre 8,6 (misurati 9,38) |
| riga dell'energia | carica letta con `/exoframe debug energy` prima e dopo 5 s di cammino: dopo minore di prima; scatti `riga-energia` (verde), `riga-energia-bassa` (500 RE, gialla), `riga-energia-esaurita` (0 RE, rossa), in alto a sinistra |
| barre | una cella da 5 000 RE e un elmo Rottame a 90/180 con `give`, inventario aperto: scatto `barre-inventario` (la cella con la barra rossa a metà, l'elmo con la barra di durata gialla a metà) |
| cambio cella | carica a 1 000 RE, `R` tenuto 3 s: scatto `cambio-cella-tenuto` a 1 s (l'avanzamento sulla barra delle azioni); poi la cella installata ha fra 4 900 e 5 000 RE (quella da 5 000, meno il consumo da fermi) e nell'inventario c'è una cella sola (la vecchia, tornata al posto della nuova); scatto `cambio-cella-fatto` |

La corsa e la riga dell'energia portano la carica dove serve con `/exoframe debug energy <RE>`. Alla fine i tre scatti di `client_shot.sh`, con il giocatore all'aperto in 0,5 −60 −5,5. Gli avvisi `moved too quickly` del server dopo un `tp` sono un artefatto noto della ricognizione; `moved wrongly` non deve comparire in `server.log` (client e server non d'accordo sul movimento della tuta, per esempio in acqua): se compare, lo script stampa una riga `ERRORE:` con il numero di righe ed esce con 1, anche se `client_shot.sh` è uscito con 0 (la riga è del gioco e non nomina la mod, quindi `check_log.py` non la vede). Non prova il doppio W e lo scatto a interruttore, i suoni, i suggerimenti al passaggio del mouse e l'uscita dall'acqua a cella vuota: sono nella checklist per il PC, `docs/test-in-gioco/m3c-giocarci.md`.

**`m3c_rules_shots.sh`** prova, in una sola esecuzione in sopravvivenza, che il client vero riceve dal server i tre valori del pacchetto `client_rules` (capacità della cella, secondi del cambio cella, secondi dell'uscita). Il server parte con una config diversa dai predefiniti, che lo script scrive in `<cartella>/server-config.json` e passa con `EXO_SHOT_SERVER_CONFIG`: `energy.cellCapacity` 20 000 (predefinito 10 000), `energy.cellSwapSeconds` 4 (2), `frame.exitHoldSeconds` 3 (1). Il client resta ai predefiniti (il suo file di config si toglie e la mod lo riscrive), quindi ciò che mostra o conta con i valori del server lo ha ricevuto all'ingresso; con i valori della sua config i controlli non tornerebbero. I controlli sono comandi `execute if`/`unless` (uno che non torna ferma l'esecuzione con uscita 1); la barra va guardata nello scatto:

| Passo | Che cosa controlla |
|---|---|
| capacità | una cella da 5 000 RE con `give`, inventario aperto: scatto `regole-barra`, **da guardare**: la barra rossa della cella è lunga un quarto (3 pixel su 13, cioè 6 su 26 nello scatto: 5 000 su 20 000), non metà (7 su 13: 5 000 su 10 000). Poi `/exoframe debug wear rottame`: la cella indossata ha fra 19 900 e 20 000 RE (il server ha davvero la config cambiata) |
| uscita | `V` tenuto 2 s: nessun telaio piazzato, il giocatore è ancora nella tuta (con il secondo del predefinito sarebbe uscito); scatto `regole-uscita` a 1,5 s: l'avanzamento circa al 50 % (misurati 48-51 %) |
| cambio cella | carica a 1 000 RE, `R` tenuto 3 s: nessun cambio, la carica resta sotto 1 000 (con i 2 s del predefinito sarebbe entrata la cella da 5 000); `R` tenuto 5 s: il cambio c'è, carica fra 4 900 e 5 000 RE |
| uscita vera | `V` tenuto 4 s: con i secondi del server si esce, e il telaio resta piazzato |

Alla fine i tre scatti di `client_shot.sh`, con il giocatore in 0,5 −60 −5,5 e il telaio lasciato dall'uscita sei blocchi davanti a lui. `log-check.txt` deve restare vuoto: in particolare nessuna riga `Config file` (il file del server è valido) né `Received client rule` (il client accetta i tre valori). Che il controllo della capacità distingua un server con la config cambiata da uno senza si è provato una volta con un file `{}` (predefiniti): il controllo a 19 900-20 000 RE fallisce, uscita 1.

**`m3c2_shots.sh`** fa la sequenza della parte 3c-2 ("ricaricare") in una sola esecuzione, in sopravvivenza, con clic veri sul caricatore di celle. Il giocatore resta in 0,5 −60 0,5 rivolto a sud: con l'inclinazione 55 il mirino cade sull'erba del blocco 0 −61 1 (il caricatore posato lì finisce in 0 −60 1, con il fronte a nord, verso il giocatore), con l'inclinazione 60 sul fianco nord di quel caricatore. I controlli sono comandi del server (`recipe take`, `clear … 0`, `execute if block`, `execute if data block`, `execute if entity`): uno che non torna ferma l'esecuzione con uscita 1. La carica letta dal blocco (`data get block 0 -60 1 cell_energy`) resta in `rcon.log`.

| Passo | Che cosa controlla |
|---|---|
| sblocco | `give` di un lingotto di rame; dopo 2 s `recipe take` riesce per ciascuna delle sette ricette della parte 3c-2 (riesce solo su una ricetta che il giocatore conosce: l'ha sbloccata il lingotto; quella della stazione la prende `m4a1_shots.sh`) |
| bottino | `loot give Exo loot redstone_exoframes:blocks/charger`: nell'inventario c'è un caricatore (`clear … 0` ne conta uno) |
| posa | con il caricatore in mano, clic destro sull'erba: in 0 −60 1 c'è `redstone_exoframes:charger[facing=north,cell=false,lit=false]`; scatto `caricatore-posato` (il fronte vuoto verso il giocatore) |
| cella | clic destro con una cella da 3 000 RE: `{has_cell:1b,cell_energy:3000}` nei dati del blocco e stato `cell=true,lit=false` |
| polvere e carica | clic destro con 8 polveri di redstone; scatto `caricatore-acceso` mezzo secondo dopo il clic (il fronte acceso e, sopra la barra degli oggetti, `Charger: charging 30%`); poi stato `cell=true,lit=true` e la carica letta due volte a 4 s di distanza: la seconda è maggiore (misurati 3 117-3 120 e 3 320-3 322 RE) |
| ritiro | clic destro a mano vuota; scatto `caricatore-svuotato` un secondo dopo (`Charger: no cell`, la cella in mano); stato `cell=false,lit=false` e una cella nell'inventario |
| rottura | la cella torna dentro (clic destro), poi in creativa un clic sinistro rompe il blocco: al suo posto c'è aria, e fra gli oggetti a terra ci sono la cella e la polvere di redstone rimasta |

Gli scatti con la riga dello stato vengono subito dopo l'attesa che segue il clic e prima dei comandi di controllo: la riga comincia a svanire dopo 2 s e a 3 s non c'è più. Dopo il clic che rompe il blocco il giocatore viene spostato subito in 0,5 −60 −5,5: cella e polvere cadono ferme al centro del blocco, a un blocco da lui, e passato il ritardo di raccolta (40 tick) le raccoglierebbe, anche in creativa, prima dei controlli. Lì restano poi i tre scatti di `client_shot.sh`. In `caricatore-posato` e `caricatore-acceso` si vede di solito anche l'avviso `New Recipes Unlocked!` del passo dello sblocco. Non prova: il clic da accovacciati (la lettura dello stato a mani vuote) e il banco da lavoro, perché lo strumento non clicca con un tasto premuto e non trascina oggetti; i suoni; le tramogge, che provano i test in mondo con tramogge vere (`ChargerGameTests`); la rottura in sopravvivenza con e senza piccone, che prova il test in mondo `chargerNeedsAPickaxeToDropItself`. Sono nella checklist per il PC, `docs/test-in-gioco/m3c-giocarci.md`.

**`m3d_shots.sh`** fa la sequenza della parte 3d ("il braccio in prima persona"): due esecuzioni di `client_shot.sh`, la seconda con il giocatore mancino (`EXO_SHOT_MAIN_HAND=left`, scatti in `<cartella>/mancino`), e poi quattordici controlli che lo script fa da sé sugli scatti con `arm_measure.sh`. Il giocatore resta in creativa in 0,5 −60 0,5 rivolto a sud, con l'interfaccia visibile (con F1 il gioco non disegna la mano). I pezzi della tuta sono dipinti di magenta (`exoframe debug wear rottame 0 FF00FF FF00FF`), un colore che nient'altro sullo schermo ha; lo scatto "senza braccio" di ogni coppia è lo stesso scatto con il giocatore invisibile (`effect give Exo minecraft:invisibility 30 0 true`: da invisibile il gioco non disegna nessun braccio, e l'ultimo argomento nasconde l'icona dell'effetto).

Scatti dell'esecuzione A, in `<cartella>`: `base-prima` e `base-senza` (senza tuta), `braccio-rottame` e `tuta-senza` (la tuta dipinta: è il primo braccio della tuta di un client appena avviato, prima di ogni F5 e dell'inventario), `braccio-nudo` (`debug wear nudo`), `bastone` (un bastone in mano), `mappa` (una mappa a due mani, con l'inclinazione 50: con 15 mappa e mani stanno sotto lo schermo), `terza-persona` (F5, da dietro), `base-dopo` (`debug wear via`), `telaio-piazzato` (un telaio Rottame non dipinto piazzato cinque blocchi davanti, rivolto al giocatore, con il braccio della tuta sullo schermo), `inventario` (l'inventario della sopravvivenza, con l'anteprima del giocatore). Scatti dell'esecuzione B, in `<cartella>/mancino`: `base-mancino`, `base-senza`, `braccio-mancino`, `tuta-senza`. Ogni cartella ha anche i tre scatti finali e i log di `client_shot.sh`; i tre scatti finali, fatti con F1, non mostrano il braccio.

| Controllo | Misura (`arm_measure.sh`) | Deve valere |
|---|---|---|
| `controllo` | `sagoma base-prima base-senza` | almeno 10 000 pixel, nessuno in mirino, barra ed energia, riquadro nel quarto in basso a destra (X ≥ 480, Y ≥ 270): è il braccio del gioco, e se non torna è sbagliato lo strumento, non la mod |
| `dipinto` | `magenta braccio-rottame`, anche nella sola metà sinistra | almeno 19 900 pixel magenta (misurati 39 962), nessuno nella metà sinistra |
| `criteri` | `sagoma braccio-rottame tuta-senza` | i criteri di grandezza e posto del design §13: nessun pixel nel mirino (`60x60+450+240`), nella barra degli oggetti con le barre di stato (`380x100+290+440`) e nella riga dell'energia (`360x30+0+0`); copertura fra il 3 % e il 14 % (misurata 8,77); riquadro nel quarto in basso a destra. Un braccio a metà misura (il segno `young` non spento) starebbe sotto il 3 % |
| `solo-tuta` | `uguali base-prima braccio-rottame base-senza` | dei pixel del braccio del gioco, al più uno su venti è rimasto uguale (misurati 0 su 18 075): il braccio del gioco non si disegna insieme a quello della tuta |
| `nudo` | `magenta braccio-nudo`; `sagoma braccio-nudo tuta-senza`; `uguali base-prima braccio-nudo base-senza` | nessun pixel magenta; i criteri (copertura misurata 7,72); al più uno su venti uguale: con il telaio nudo si vede il solo braccio d'acciaio, senza il braccio del pilota |
| `bastone` | `magenta bastone` | nessun pixel magenta: con un oggetto in mano nessun braccio |
| `mappa` | `magenta mappa` nella metà sinistra e nella metà destra | almeno 300 ciascuna (misurati 28 407 e 28 977): le due braccia della tuta |
| `terza-persona` | `magenta terza-persona` | riquadro largo almeno 130 (misurato 318): la tuta intera, non un braccio solo |
| `inventario` | `magenta inventario 216x210+304+60` | riquadro largo almeno 70 (misurati 98 su 1.21.1, che ritaglia l'anteprima nel suo riquadro, fra 166 e 174 su 1.20.1): la tuta intera nell'anteprima |
| `senza-tuta` | `sagoma base-dopo base-prima` | al più 200 pixel diversi (misurati 0): tolta la tuta, lo scatto di prima |
| `telaio-piazzato` | `sagoma telaio-piazzato braccio-rottame`; `magenta telaio-piazzato` | riquadro largo almeno 120 (misurato 258): il telaio piazzato è intero; magenta fra il 90 % e il 110 % di quello di `braccio-rottame` (misurato il 100 %): il braccio non ha perso né preso pezzi |
| `mancino-controllo` | `sagoma mancino/base-mancino mancino/base-senza` | almeno 10 000 pixel e riquadro tutto nella metà sinistra: l'opzione `mainHand` ha avuto effetto (se non torna è un problema dello strumento) |
| `mancino` | `magenta mancino/braccio-mancino` nella metà destra; `sagoma mancino/braccio-mancino mancino/tuta-senza` | nessun pixel magenta a destra; nessun pixel in mirino, barra ed energia, copertura fra il 3 % e il 14 %, riquadro nel quarto in basso a sinistra |
| `specchio` | `magenta mancino/braccio-mancino` contro `magenta braccio-rottame` | il magenta fra l'80 % e il 120 % di quello del destro (misurato il 100 %) e il riquadro specchiato (X′ = 960 − X − L) entro 12 pixel per lato (misurati 0) |

Lo script stampa una riga per controllo, `ok <nome>: <misure>` oppure `KO <nome>: <misure> (atteso …)`, tutti valutati anche dopo un fallimento, e per ultima `OK <variante>: scatti in <cartella>` (uscita 0) solo se le due esecuzioni sono uscite con 0 e ogni controllo è `ok`; altrimenti `FAIL <variante>: …` su stderr (gli avvisi in `log-check.txt`, l'uscita di `client_shot.sh`, i controlli falliti per nome) e uscita 1. Se l'esecuzione A fallisce la B non parte, e i controlli girano solo se ci sono tutti e quindici gli scatti. Con `M3D_SOLO_CONTROLLI=1` salta le due esecuzioni e rifà i controlli sugli scatti che la cartella ha già. Le soglie stanno in testa allo script, ciascuna con la ragione e il valore misurato; le misure sono uguali al pixel sulle quattro varianti, salvo il magenta della terza persona e dell'anteprima dell'inventario. `EXO_SHOT_MAIN_HAND` la imposta lo script per le due esecuzioni, qualunque valore abbia nell'ambiente. Il primo passo di ogni esecuzione è `@attendi 12`: vedi le regole degli scatti misurati, qui sotto.

**OK non dice che la mano si veda bene** (il criterio (a) del design §13 si guarda): dopo una modifica a grandezza o posto vanno guardati `braccio-rottame`, `braccio-nudo`, `mappa` e `mancino/braccio-mancino`. Gli stessi scatti di `1.20.1-forge` e `1.21.1-neoforge`, più `braccio-colori-suoi` (i pezzi con i loro colori, da un'esecuzione di `client_shot.sh` a parte), sono i riferimenti della checklist, in `docs/test-in-gioco/m3d-screenshot/`. La sequenza non prova: il jar di produzione (qui gira l'ambiente di sviluppo, con i nomi Mojang: che l'aggancio si applichi nel jar finale lo provano `tools/check_jars.sh` e la checklist sul PC); il pezzo montato su un lato solo (il comando di debug monta sei pezzi o nessuno); una ricarica delle risorse (F3+T) con il braccio sullo schermo; la tuta di un secondo giocatore. Sono nella checklist per il PC, `docs/test-in-gioco/m3d-braccio.md`.

**`m4a1_shots.sh`** fa la sequenza della parte 4a-1 ("il banco"), in una sola esecuzione di `client_shot.sh` in creativa, e poi i controlli che lo script fa da sé sugli scatti con `arm_measure.sh`. È la prova delle due cuciture nuove nel client vero: dopo `@attendi 12` un lingotto di rame sblocca la ricetta della stazione (`recipe take` riesce solo così; poi `@attendi 12`, perché l'avviso delle ricette sbloccate sparisca), un clic destro con la stazione in mano e il mirino sul pavimento davanti la posa con l'oggetto, la metà principale in 0 −60 1 e la laterale in −1 −60 1, con il fronte a nord, verso il giocatore (`execute if block` sulle due metà; scatto `stazione-posata`, da guardare: due blocchi affiancati con le texture segnaposto, nessun blocco viola e nero), un clic destro con il mirino sul fianco nord della principale la apre (scatto `stazione-aperta`, con il puntatore nel punto neutro 10, 10: la schermata scura al centro e nel riquadro di destra la tuta fissa di fronte, in piedi, con il braccio destro magenta sul lato sinistro dello schermo), poi `@clic 748 426` preme il pulsante di prova al centro (scatto `premuto`, il puntatore di nuovo nel punto neutro: il pulsante in basso a destra verde con `1`), `Escape` chiude la schermata, un clic sinistro sul fianco nord della laterale rompe in creativa le due metà senza lasciare oggetti a terra (`execute if block` e `execute unless entity`) e il giocatore va in 0,5 −60 −5,5 per i tre scatti finali. Con la finestra di 960×540 e la scala 2 la schermata della stazione (316×232) occupa `632x464+164+38` negli scatti, il ritaglio dell'anteprima `184x232+596+74` e il pulsante di prova `36x32+730+410`.

| Controllo | Misura (`arm_measure.sh`) | Deve valere |
|---|---|---|
| `anteprima` | `magenta stazione-aperta 184x232+596+74`; `magenta stazione-aperta 632x464+164+38` | la prima almeno `MAGENTA_MIN` (590; misurati 1193 sulle quattro varianti, nel riquadro `41x85+604+152`), la seconda uguale alla prima: la schermata si è aperta, l'anteprima disegna la tuta, e nella schermata il magenta sta solo nel ritaglio |
| `orientamento` | `magenta stazione-aperta 92x232+688+74` (la metà destra del ritaglio) | zero: la tuta fissa ha magenta il solo braccio destro, che di fronte sta a sinistra dello schermo |
| `pulsante` | `sagoma premuto stazione-aperta` | almeno `BUTTON_MIN` pixel (550; misurati 1112 sulle quattro varianti, nel riquadro `36x32+730+410`, il pulsante intero), con il riquadro dentro `36x32+730+410`: il numero 102 è andato al server ed è tornato come il cambio del pulsante, e nient'altro è cambiato |

Le soglie stanno in testa allo script, ciascuna con la ragione e il valore misurato (la metà del più piccolo sulle quattro varianti, arrotondata per difetto alla decina). Le misure sono uguali al pixel sulle quattro varianti, e così il ritaglio dell'anteprima: la scala della tuta passa da `GuiEntityScale` (vedi `docs/compat.md`), che la applica come il gioco base di ogni versione. Lo script stampa una riga per controllo, `ok <nome>: <misure>` oppure `KO <nome>: <misure> (atteso …)`, tutti valutati anche dopo un fallimento, e per ultima `OK <variante>: scatti in <cartella>` (uscita 0) solo se `client_shot.sh` è uscito con 0 e ogni controllo è `ok`; altrimenti `FAIL <variante>: …` su stderr e uscita 1. Con `M4A1_SOLO_CONTROLLI=1` salta l'esecuzione e rifà i controlli sugli scatti che la cartella ha già. La sequenza cresce con i task della parte 4a-1 (il telaio, la vista, l'anteprima girevole, l'inventario nella schermata) e la completa l'ultimo.

**`measure_lib.sh`** non si esegue: `m3d_shots.sh` e `m4a1_shots.sh` lo caricano con `source` e ne usano gli aiuti, che trasformano le righe di `arm_measure.sh` in controlli `ok` o `KO`: `take_measure` (la riga di una misura, o `misura-non-riuscita`), `val` (il valore di un nome nella riga, senza espandere `*` e `?`), `box` (il riquadro della misura), `inside` (il riquadro sta dentro un rettangolo) e `verdict` (la riga del controllo, con l'elenco `failed` dei controlli falliti). Chi lo carica imposta prima `measure`, il percorso di `arm_measure.sh`; una sequenza nuova che misura scatti lo carica allo stesso modo.

**`arm_measure.sh`** misura scatti di 960×540 (altrimenti uscita 2) con ImageMagick, senza avviare nulla; stampa una riga ed esce con 0.

| Modo | Uscita | Che cosa misura |
|---|---|---|
| `sagoma <con.png> <senza.png>` | `pixel=<N> copertura=<P.PP> mirino=<N> barra=<N> energia=<N> riquadro=<LxA+X+Y>` | i pixel in cui i due scatti differiscono, la loro parte dello schermo in percento, quanti cadono nei tre rettangoli (mirino `60x60+450+240`, barra `380x100+290+440`, energia `360x30+0+0`) e il riquadro che li contiene |
| `magenta <scatto.png> [<LxA+X+Y>]` | `magenta=<N> riquadro=<LxA+X+Y>` | i pixel color magenta, del solo rettangolo se è dato; il riquadro è nelle coordinate dello scatto intero |
| `uguali <a.png> <b.png> <senza.png>` | `sagoma=<N> uguali=<N>` | dei pixel in cui `a` differisce da `senza`, quanti sono uguali in `a` e in `b` |
| `--selftest` | `OK arm_measure` | prova lo strumento su immagini fatte al momento |

Senza pixel il riquadro è `0x0+0+0`. Due pixel **differiscono** se un canale qualunque dista più del 4 % (una conversione in grigio non vedrebbe una differenza nel solo blu). Un pixel è **magenta** se, con i canali fra 0 e 1, `r > 0.2`, `b > 0.2`, `g < 0.45·r` e `g < 0.45·b`: il colore `FF00FF` dato ai pezzi resta di quella tinta sotto ogni luce, e cielo, erba, barra degli oggetti, riga dell'energia, acciaio e ottone del telaio non lo sono. Nell'interfaccia sono magenta anche il titolo dell'avviso delle ricette, i nomi degli oggetti di rarità epica e il viola scuro dei testi.

Lo strumento confronta pixel, quindi una riga di chat o un avviso presente in uno solo dei due scatti di una coppia entra nella misura. **Regole degli scatti misurati:**

- **niente `gamemode` negli 11 s prima** di uno scatto misurato con `sagoma` o `uguali`: il comando lascia nella chat del giocatore una riga che dura 10 s e copre x 0-664, y 442-460, dentro il rettangolo della barra. `client_shot.sh` manda il suo `gamemode creative` circa 6 s prima del primo passo. Gli altri comandi usati da `m3d_shots.sh` non scrivono nella chat del giocatore (il server parte con `broadcast-rcon-to-ops=false`);
- **niente avvisi sullo schermo**: un oggetto che sblocca ricette (un bastone ne sblocca otto) fa comparire per 5 s, più l'entrata e l'uscita, l'avviso delle ricette, che ha il titolo magenta: uno scatto misurato con `magenta` aspetta 8 s. Entrando nel server di prova il client mostra in alto a destra l'avviso "Chat messages can't be verified" (circa `460x110+500+0`, un decimo dello schermo), che sparisce fra 7,0 e 8,1 s dopo il primo passo (misurato con uno scatto al secondo su 1.21.1-neoforge, 1.20.1-fabric e 1.20.1-forge; la riga in chat sparisce fra 3,5 e 4,6 s): per questo un'esecuzione con scatti misurati comincia con `@attendi 12`.

Variabili d'ambiente:

| Variabile | Default | Significato |
|---|---|---|
| `EXO_ACCEPT_EULA` | (nessuno) | `true` per accettare l'EULA del server di prova; obbligatoria |
| `EXO_SHOT_PORT` | 25601 | porta del server; RCON usa la porta + 10 |
| `EXO_SHOT_W`, `EXO_SHOT_H` | 960, 540 | misura della finestra e dello schermo virtuale; i punti dei passi del puntatore stanno dentro |
| `EXO_SHOT_STEPS_ONLY` | (nessuno) | `true` per controllare soltanto variante, cartella, passi e coordinate e uscire con `OK passi: <N>`, senza avviare nulla (vedi "Passi del client") |
| `EXO_SHOT_USER` | Exo | nome del giocatore offline |
| `EXO_SHOT_SETTLE` | 8 | secondi di attesa dopo i comandi, prima degli scatti |
| `EXO_SHOT_SERVER_CONFIG` | (nessuno) | percorso di un file di config della mod per il solo server: lo script lo copia in `versions/<variante>/run-shot-server/config/redstone_exoframes.json`; un file che non esiste o non si legge ferma lo script con uscita 2, prima di avviare qualunque cosa. Le sequenze che presuppongono la config predefinita (`m3a_shots.sh`, `m3b_shots.sh`, `m3c_shots.sh`, `m3c2_shots.sh`, `m3d_shots.sh`, `m4a1_shots.sh`) la tolgono dall'ambiente prima di chiamare `client_shot.sh`; `m3c_rules_shots.sh` la imposta da sé |
| `EXO_SHOT_MAIN_HAND` | `right` | `left` o `right`: la mano principale del giocatore, scritta in `options.txt` del client (`mainHand:"left"`); con `left` il gioco disegna a sinistra il braccio della prima persona. Un altro valore ferma lo script con uscita 2 prima di avviare qualunque cosa |

Lo script lavora sul checkout in cui si trova (compila il codice com'è, anche le modifiche non committate) in cartelle di esecuzione proprie, `versions/<variante>/run-shot-client` e `run-shot-server`, ignorate da git; non tocca `run/` e `run-gametest/`. Il mondo del server si ricrea a ogni esecuzione, e il file di config della mod (`config/redstone_exoframes.json`) si toglie nelle due cartelle: ogni esecuzione parte dai predefiniti, che gli script della parte 3c presuppongono (999 RE sotto il 10 %, il fattore 0,85, i 2 s del cambio cella, i 50 RE al secondo del caricatore), salvo `EXO_SHOT_SERVER_CONFIG`, che dà al solo server il file indicato (la cartella del client resta ai predefiniti). Server e RCON ascoltano solo su 127.0.0.1 e il client si collega a quell'indirizzo; la password RCON è nuova a ogni esecuzione, non si stampa, passa a `rcon.py` nella variabile d'ambiente `EXO_RCON_PASSWORD` (non sulla riga di comando) e resta solo in `server.properties` della cartella del server, leggibile solo dal proprietario. Una sola esecuzione per variante alla volta (c'è un lucchetto); varianti diverse in parallelo vogliono porte diverse, ma con quattro processori il rendering software rallenta molto.

## Errori

Uscita 0: scatti salvati e `log-check.txt` vuoto. Uscita 1: qualcosa si è fermato durante l'esecuzione, con una riga `ERRORE:` su stderr che dice cosa (build fallita, server o client che non partono, giocatore che non entra o viene espulso, comando rifiutato o senza risposta, passo del client non riuscito, scatto quasi tutto di un colore), oppure alla fine `log-check.txt` non è vuoto (gli scatti ci sono, i log da guardare sono quelli). Uscita 2: uso o ambiente sbagliati (variante sconosciuta, passo sconosciuto o scritto male, tasto sconosciuto, programmi mancanti, EULA non accettata, file di `EXO_SHOT_SERVER_CONFIG` che non esiste o non si legge, `EXO_SHOT_MAIN_HAND` che non è `left` né `right`, porta occupata, un'altra esecuzione sulla stessa variante, Xvfb che non parte, OpenGL core 3.2 assente); `m3a_shots.sh` allora non prova gli scatti successivi. Uscita 130: interrotto con Ctrl-C; 143: terminato con TERM. In ogni caso lo script rilascia per primo il tasto che un passo `@tieni` stava tenendo premuto (se l'errore o l'interruzione arriva durante la pressione), ferma il server con `stop`, manda TERM ai suoi due processi Gradle (che annullano la build nel demone, così un server o un client non ancora partiti non partono più) e alle JVM del gioco di quell'esecuzione, dopo 30 s le uccide, chiude Xvfb e copia i log. La chiusura dura al massimo circa 35 s, e un secondo Ctrl-C non la interrompe.

Uno scatto non si salva se il client non è più vivo o il giocatore ha lasciato il server: uno scatto del menu principale non passa mai per buono.

**OK non vuol dire che l'immagine sia giusta.** Lo script controlla che il client sia vivo, che il giocatore sia nel server e che lo scatto abbia almeno 64 colori; non controlla che i tasti (F1, F5, il passo avanti) abbiano avuto effetto né che il terreno sia già disegnato. Un tasto perso darebbe, per esempio, un `front.png` in prima persona con uscita 0. Ogni scatto va guardato.

## Quanto dura

Misurato nella sessione cloud (4 processori, 15 GB), a cache calde: circa un minuto per esecuzione, di cui 15-35 s per il server e 25-50 s perché il client entri. L'insieme standard di `m3a_shots.sh` dura circa 8,5-10 minuti per variante (misurati 503-589 s sulle quattro varianti); la sequenza di `m3b_shots.sh` circa due minuti (misurati 115-132 s), quella di `m3c_shots.sh` altrettanto (misurati 116-127 s), quella di `m3c_rules_shots.sh` poco meno (misurati 95-128 s), quella di `m3c2_shots.sh` circa un minuto e mezzo (misurati 80-100 s), quella di `m3d_shots.sh`, che fa due esecuzioni, circa tre minuti e mezzo (misurati 207-225 s sulle quattro varianti: 111-119 s la prima, 76-86 s quella del mancino; 232 s una volta, la prima sequenza dopo una build), quella di `m4a1_shots.sh` circa due minuti (misurati 123 s su 1.21.1-neoforge; prima della ricetta, della posa con l'oggetto e della rottura 88-107 s sulle quattro varianti). Un'esecuzione di `client_shot.sh` con quattro passi, come quella dello scatto `braccio-colori-suoi`, dura circa un minuto e mezzo (misurati 74-86 s). `arm_measure.sh` impiega meno di un secondo per `sagoma` e `uguali`, circa un secondo per `magenta`; il suo `--selftest` circa 4-5 s. La prima esecuzione in assoluto scarica circa 800 MB di risorse del gioco e compila la variante: qualche minuto in più.

## Cosa non copre

- **Davanti da sdraiati.** Con la posa strisciante la telecamera davanti finisce contro la testa: lo scatto non mostra nulla di utile.
- **Vista laterale vera.** Si gira solo la testa (mouse finto); il corpo la segue in ritardo, fino a 50 gradi, quindi `quarter.png` è una vista a tre quarti più o meno di lato. Per un lato esatto servirebbe un secondo client come telecamera.
- **Audio** (non c'è un dispositivo audio), **fluidità** e animazioni nel tempo (si scattano fotogrammi fermi, a 30 fps in rendering software), **un secondo giocatore** che guarda il primo.
- Il **jar di produzione**: qui gira l'ambiente di sviluppo di Loom. Che i mixin si applichino nel jar finale lo prova solo la checklist sul PC.
- Il **braccio in prima persona** fuori dalla vista a riposo di `m3d_shots.sh`: l'oscillazione del colpo, la camminata, altre misure della finestra, altri campi visivi e scale dell'interfaccia, la ricarica delle risorse (F3+T), un pezzo del braccio montato su un lato solo. Li prova la checklist `docs/test-in-gioco/m3d-braccio.md`.
- Pose da comandi e tasti diversi da quelli usati qui (accovacciarsi, correre, nuotare): vedi la checklist.
- **Cliccare nel mondo con un tasto premuto**: `@clic ... maiuscole` tiene premuto Maiusc solo dentro una schermata, dove il puntatore è libero; nel mondo i passi si eseguono uno dopo l'altro e `@click` clicca dove punta il mirino senza tasti premuti, quindi il clic da accovacciati (smontare un pezzo, togliere la cella, raccogliere il telaio come oggetto, leggere lo stato del caricatore a mani vuote) non ha un'esecuzione nel client vero: lo provano i test in gioco e la checklist.
- **Trascinare oggetti fuori dalle schermate**: dentro una schermata `@clic` e `@trascina` prendono e spostano oggetti fra gli slot (un clic con Maiusc sposta uno stack), ma una sequenza che lo fa deve calcolare i punti degli slot dalla disposizione della schermata (vedi "Passi del client"), e le schermate del gioco base che non sono della mod (inventario, banco da lavoro) non hanno ancora una sequenza che le usi: lanciare un segnaposto fuori dall'inventario o riempire la griglia del banco da lavoro li prova la checklist; le ricette le provano anche i test in gioco (`DataGameTests`).

## Messaggi innocui nei log

- `UnknownHostException` o errori di connessione verso `sessionserver.mojang.com` e `api.minecraftservices.com`, `Failed to request yggdrasil public key`, `Couldn't look up profile properties`, `Ignoring chat session ... missing Services public key`: account offline di sviluppo.
- `Error starting SoundSystem. Turning off sounds & music`, `Failed to open OpenAL device`, `Missing sound for event`: niente audio.
- `Reference map 'geckolib.refmap.json' for geckolib.mixins.json could not be read. If this is a development environment you can ignore this message`: GeckoLib in sviluppo; non nomina il mod, quindi `log-check.txt` non la prende.
- Su NeoForge: `Attempted to select a dependency jar for JarJar which was passed in as source: library:mixinextras.neoforge`: ambiente di sviluppo; non nomina il mod.
- `Shader rendertype_entity_translucent_emissive could not find sampler named Sampler2`: avviso del gioco base su 1.21.1.
- Sulle due varianti Fabric, in client e server: `[ERROR] (Minecraft) No data fixer registered for exoframe`, il tipo dell'entità telaio senza correttore di dati (vedi `docs/compat.md`, `CompatEntity`); è nell'elenco degli avvisi voluti, esattamente una volta per log, mai su Forge e NeoForge.
- Sul server: `No key layers in MapLike[{}]` (impostazioni del mondo piatto lasciate vuote) e gli avvisi `SERVER IS RUNNING IN OFFLINE/INSECURE MODE`.
