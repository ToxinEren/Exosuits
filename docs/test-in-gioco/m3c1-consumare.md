# Checklist in gioco: milestone 3, parte 3c-1 (consumare)

Per Jarvis, sul PC Windows di Luigi, con i quattro jar di produzione di questa parte. Non serve aver seguito il lavoro: qui sotto c'è tutto ciò che occorre sapere. I passi sono numerati; accanto a ciascuno c'è ciò che deve succedere. Annota l'esito di ogni passo (OK / non OK, con uno screenshot quando non è OK). Dove un passo dice "annota" o "riporta" senza un risultato atteso, il risultato non è noto in anticipo: scrivi che cosa succede.

Questa checklist prova solo ciò che si vede della parte 3c-1, con gli oggetti della scheda creativa (telaio, pezzi Rottame, cella carica) e con i comandi. La checklist completa della parte 3c, con il caricatore e le ricette, arriva con la parte 3c-2.

## Cosa c'è da vedere

Dalla parte 3b il telaio si piazza, si arma a mano, ci si entra e se ne esce (checklist `m3b-entrare-e-uscire.md`). Da questa parte la tuta **consuma** e **si consuma**:

- **Energia.** Chi è nella tuta consuma la carica della cella montata: 2 RE al secondo da fermo, 8 camminando, 20 correndo, 15 per ogni salto (con il torso rotto tutto per 1,25). Una cella piena tiene 10000 RE: circa 83 minuti da fermi, 21 camminando, 8 correndo. Sotto il 10 % della carica (cioè sotto 1000 RE) l'energia è **bassa**: niente scatto. A zero è **esaurita**: niente scatto, la tuta va a meno di un terzo della velocità e protegge la metà. In **creativa** non si consuma nulla. La tuta, anche piena, rallenta un po' il pilota (−15 % di velocità) e restringe un po' il campo visivo, come l'effetto lentezza; a energia esaurita molto di più.
- **La riga dell'energia.** In alto a sinistra sullo schermo, solo per chi indossa la tuta, una riga con ombra: "Energia 7420 / 10000 RE (74 %)" ("Energy 7420 / 10000 RE (74%)"): verde con l'energia normale, gialla bassa, rossa esaurita. Sotto, su una riga sua e solo quando serve, il pezzo messo peggio: "Torso in stato critico" ("Torso critical") in giallo, "Torso fuori uso" ("Torso broken") in rosso. Si nasconde con F1; la copre la schermata di F3. Senza una cella la riga sarebbe "Energia: nessuna cella" ("Energy: no cell"), ma in questa parte non c'è modo di indossare un telaio senza cella (vedi "Cosa non c'è ancora").
- **Il comando `/exoframe debug energy`** (solo operatori, cioè in un mondo con i trucchi attivi): senza numero scrive carica, capacità e stato della cella del telaio che indossi; con un numero porta la carica a quel valore (al più la capacità). Serve per arrivare subito all'energia bassa o esaurita.
- **Usura dei pezzi.** Ogni colpo che l'armatura prenderebbe consuma i pezzi montati (un pezzo Rottame ha 180 punti di durata), divisi fra i pezzi secondo il tipo di colpo; lava e fuoco li consumano a ogni colpo. Un pezzo sotto il 15 % della durata è **critico**, a zero è **rotto**: resta montato ma non protegge più; una gamba rotta toglie il 15 % di velocità, il torso rotto fa consumare il 25 % in più. Nessuna riparazione in questa parte.
- **Caduta.** Il pilota non prende mai danno da caduta; la caduta consuma i pezzi, soprattutto le gambe.
- **Gradino.** La tuta sale un gradino alto un blocco camminando, senza saltare (non staccionate e muretti, alti un blocco e mezzo).
- **Acqua.** La tuta non nuota e non galleggia: affonda come se cadesse in aria, sul fondo cammina e corre come a terra, Spazio non la fa salire, la corrente non la sposta. Si respira come sempre (gli occhi sono a circa 2,7 blocchi dai piedi). Da una vasca si esce da una sponda alta al più un blocco, da una scala di blocchi, o lasciando la tuta (tenendo V). Le colonne di bolle la sollevano come sollevano un giocatore.
- **Niente letti e niente cavalcature** con la tuta: il letto rifiuta il clic, e da una barca, un carrello o un cavallo si viene fatti scendere subito.
- **Barre e suggerimenti.** La cella mostra la barra rossa della carica quando non è piena, e al passaggio del mouse la carica; i pezzi la barra della durata e la durata; il telaio quattro righe che insegnano i comandi.
- **Cambio cella.** Dentro la tuta, tenendo premuto **R** per due secondi, la cella montata si cambia con la più carica dell'inventario (se ne ha più di quella montata); la vecchia va al posto della nuova. Cliccando con una cella in mano un telaio piazzato che ha già una cella, le due si scambiano.
- **Suoni.** Un suono del gioco base per ogni azione (tabella al passo 59).
- **Configurazione da file.** `config/redstone_exoframes.json` nella cartella dell'istanza (o del server), scritto con i valori predefiniti al primo avvio, letto solo all'avvio.

Messaggi (in italiano; tra parentesi il testo inglese). Tutti sulla barra delle azioni (la riga sopra la barra degli oggetti), tranne il primo, che va in chat:

| Quando | Messaggio |
|---|---|
| primo ingresso in un telaio piazzato in ogni sessione (in chat) | Tieni premuto V per uscire dal telaio, R per cambiare cella (Hold V to leave the exoframe, hold R to swap the cell) |
| l'energia scende sotto il 10 % | Energia bassa: niente scatto (Energy low: no sprinting) |
| l'energia arriva a zero | Energia esaurita: la tuta si muove appena (Energy empty: the exoframe barely moves) |
| un pezzo diventa critico | Pezzo in stato critico: Torso (Critical piece: Torso), con il nome del pezzo |
| un pezzo si rompe | Pezzo fuori uso: Torso (Broken piece: Torso), con il nome del pezzo |
| letto, barca, carrello, cavallo con la tuta | Esci prima dall'esoscheletro (Leave the exoframe first) |
| primo contatto con l'acqua dopo ogni ingresso | L'esoscheletro affonda: esci camminando, oppure tieni premuto il tasto di uscita per lasciarlo (The exoframe sinks: walk out, or hold the exit key to leave it) |
| R tenuto premuto | Tieni premuto per cambiare cella… N% (Hold to swap the cell… N%) |
| R, e nessuna cella dell'inventario ha più energia di quella montata | Nessuna cella nell'inventario ha più energia (No cell in your inventory holds more energy) |
| V dove il telaio andrebbe a toccare un altro telaio piazzato | Qui c'è già un altro telaio: spostati prima di uscire (Another exoframe stands where yours would be left: move first) |
| entrare in un telaio senza cella, o con una cella vuota | Il telaio ha bisogno di una cella carica (le celle si caricano nel caricatore) (The exoframe needs a charged cell (charge cells in a charger)) |

Nomi dei pezzi nei messaggi: Elmo, Torso, Braccio sinistro, Braccio destro, Gamba sinistra, Gamba destra (Helmet, Torso, Left arm, Right arm, Left leg, Right leg).

Le quattro risposte di `/exoframe debug energy`, in chat:

| Quando | Risposta |
|---|---|
| senza numero, dentro la tuta | Cella: 9980 / 10000 RE (normale) (Cell: 9980 / 10000 RE (normal)); al posto di "normale" anche "bassa" (low) o "esaurita" (empty) |
| con un numero, dentro la tuta | Carica della cella portata a 999 RE (Cell charge set to 999 RE) |
| fuori dalla tuta, in rosso | Non indossi un telaio (You are not wearing an exoframe) |
| dentro una tuta senza cella, in rosso | Il telaio che indossi non ha una cella (The exoframe you are wearing has no cell): in questa parte non si può vedere, come la riga "Energia: nessuna cella" |

## Cosa non c'è ancora (non è un errore)

- **Il caricatore** non esiste: le celle non si ricaricano. Una cella si ottiene carica dalla scheda creativa, e la carica si rimette con `/exoframe debug energy`. Due testi nominano già il caricatore e vanno bene così: il suggerimento della cella vuota, "Vuota: caricala in un caricatore" ("Empty: charge it in a charger"), e il rifiuto d'ingresso "Il telaio ha bisogno di una cella carica (le celle si caricano nel caricatore)".
- **Le ricette**: telaio, pezzi e celle non si fabbricano, quindi in sopravvivenza la tuta non si costruisce. Gli oggetti si prendono dalla scheda creativa "Redstone Exoframes" (passando un momento in creativa) o con `/give`.
- **La riparazione dei pezzi**: un pezzo consumato o rotto resta così (arriva con la stazione, milestone 4).
- **L'animazione del cambio cella**: il cambio si vede solo dall'avanzamento sulla barra delle azioni, dal suono e dalla riga dell'energia; la tuta non si muove.
- **Una tuta senza cella**: per entrare serve una cella carica, `/exoframe debug wear` mette sempre una cella carica e il cambio cella mette sempre una cella al posto della vecchia; quindi la riga "Energia: nessuna cella" e la risposta "Il telaio che indossi non ha una cella" non compaiono mai.
- **L'HUD vero** (arriva con la milestone 4): la riga dell'energia è l'unica cosa disegnata sullo schermo.
- Dentro la tuta non valgono gli incantesimi dell'armatura né le regole dei mob legate all'armatura (piglin e oro, neve polverosa e stivali di cuoio, Respirazione): negli slot ci sono i segnaposto. Arriva con la milestone 5.

## Già verificato nella sessione cloud (16 novembre 2026)

**Test in mondo** (`tools/run_gametests.sh`, 172 test, sulle quattro varianti, sul server con giocatori finti): i consumi esatti (fermo, cammino, corsa, salto), creativa e spettatore senza consumo, i passaggi a energia bassa ed esaurita con messaggio e suono, l'usura con un colpo noto e la finestra di invulnerabilità, la rottura di un pezzo, la caduta senza danno con l'usura delle gambe, gli attributi, le regole dell'acqua sul server, la lava come nel gioco base, il cambio cella (scelta, intervallo, nessuna cella in più o in meno), lo scambio su un telaio piazzato, cavalcature e letti rifiutati, il suono di ogni azione, `/exoframe debug energy`, l'uscita rifiutata sopra un altro telaio.

**Client vero** (`tools/client/m3c_shots.sh`, sul client di **sviluppo** di ognuna delle quattro varianti, non sui jar di produzione, in sopravvivenza, con tasti veri):

| Controllo | Esito sulle quattro varianti |
|---|---|
| Teletrasportata sopra una vasca profonda 3, la tuta va sul fondo in 1,5 s | sì |
| Spazio tenuto 3 s sul fondo: non sale; niente posa da nuoto | sì |
| W tenuto 2 s sul fondo: più di 6 blocchi (velocità di terra) | sì |
| Acqua profonda 1: Spazio non la muove; camminando esce da una sponda alta 1 (cella piena) | sì |
| Acqua che scorre: la tuta non si sposta | sì |
| Scalino alto 1: salito camminando, senza saltare (salto automatico spento) | sì |
| Ctrl+W a 999 RE: niente scatto; a 5000 RE lo scatto c'è | sì |
| La carica cala camminando; riga dell'energia in alto a sinistra | sì |
| Barre di una cella a metà e di un elmo a 90/180 nell'inventario | sì |
| R tenuto: avanzamento, poi la cella da 5000 montata e la vecchia nell'inventario | sì |
| Nessun "moved wrongly" nel log del server (client e server d'accordo sul movimento) | sì |

Gli scatti di riferimento sono in `m3c-screenshot/`, uno per scena, da varianti diverse: `1.20.1-forge-riga-energia.png` e `1.21.1-neoforge-riga-energia.png` (riga verde), `1.21.1-fabric-riga-energia-bassa.png` (gialla, 500 RE), `1.20.1-fabric-riga-energia-esaurita.png` (rossa, 0 RE), `1.20.1-fabric-vasca-spazio.png` e `1.21.1-fabric-vasca-spazio.png` (sul fondo con Spazio tenuto), `1.20.1-forge-barre-inventario.png` e `1.21.1-neoforge-barre-inventario.png`, `1.20.1-fabric-cambio-cella-tenuto.png` e `1.21.1-fabric-cambio-cella-tenuto.png`. Sono del client di sviluppo con testi in inglese. Il giallo e il rosso della riga si vedono negli scatti solo di una variante ciascuno. La riga ora sta in alto a sinistra: in un primo momento stava in basso a destra, ed è stata spostata perché copriva la barra degli oggetti.

Il rifiuto del letto è stato provato nel client vero di tre varianti, non su 1.20.1 Forge.

Il cloud non prova: il **jar di produzione** (i mixin applicati nel jar finale); i **suoni** (nel cloud non c'è audio); i **suggerimenti** al passaggio del mouse; il **doppio W** e lo **scatto a interruttore** con l'energia bassa; l'**uscita dall'acqua con la cella vuota** (mai provata da nessuna parte); le **colonne di bolle**, lo **scatto sul fondo** e il **controllo in aria** (provati solo in un primo esperimento, non dallo script finale); la seconda riga della riga dell'energia con un **pezzo rotto**; il **letto su 1.20.1 Forge**; il **file di config** su un PC vero; un **server dedicato** con valori diversi da quelli del client. Sono le sezioni 1-14.

## 0. Preparazione

- [ ] **1.** Procurati i quattro jar di questa parte come nella sezione 0 di `m1-setup.md`: artefatto `redstone-exoframes-jars` dell'ultima esecuzione verde della CI sul branch **`main`** (la parte 3c-1 è unita a `main`), oppure build locale con `collectJar` da `main`. Sono `redstone-exoframes-fabric-0.1.0+mc1.20.1.jar`, `redstone-exoframes-fabric-0.1.0+mc1.21.1.jar`, `redstone-exoframes-forge-0.1.0+mc1.20.1.jar`, `redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar`. Controllali con `bash tools/check_jars.sh <cartella>`: deve stampare `OK jars`.
- [ ] **2.** Usa le quattro istanze client della milestone 1 (tabella dei profili in `m1-setup.md`, sezione 2). Sostituisci nella cartella `mods` il jar vecchio con quello nuovo; nessun altro mod oltre alle dipendenze della tabella (loader API e GeckoLib). Se nella cartella `config` dell'istanza c'è già un file `redstone_exoframes.json` (da una prova precedente), cancellalo: questa checklist parte dai valori predefiniti.
- [ ] **3.** Tieni aperta la cartella `docs/test-in-gioco/m3c-screenshot/` del repository (scatti di riferimento, citati sotto).
- [ ] **4.** Screenshot con F2 (finiscono in `<istanza>\screenshots\`); F1 nasconde l'interfaccia, F5 cambia la telecamera (una volta: da dietro; due volte: davanti; tre: di nuovo in prima persona). Nelle opzioni del client: Opzioni... → Comandi... (Options... → Controls...) → **Salto automatico: No** (Auto-Jump: OFF), e Opzioni... → Musica e suoni... (Music & Sounds...) → **Mostra sottotitoli: Sì** (Show Subtitles: ON): i sottotitoli in basso a destra aiutano a riconoscere i suoni.

Ripeti le sezioni 1-12 per ognuna delle quattro varianti, ciascuna nella propria istanza. La sezione 13 (config) va fatta per intero su una variante 1.20.1 e una 1.21.1; il primo passo (il file creato all'avvio) su tutte e quattro. Le sezioni 14 e 15 sono facoltative.

## 1. Avvio, mondo di prova e comandi

- [ ] **5.** Avvia il client fino al menu principale: nessun crash, nessuna schermata d'errore del loader. Nella cartella dell'istanza c'è ora `config\redstone_exoframes.json` (lo controlla la sezione 13).
- [ ] **6.** In Opzioni... → Comandi... → Assegnazione dei tasti... (Options... → Controls... → Key Binds...), categoria "Redstone Exoframes": "Esci dal telaio" ("Leave the exoframe") sul tasto **V** e "Cambia cella" ("Swap the cell") sul tasto **R**.
- [ ] **7.** Crea un mondo nuovo: **sopravvivenza, superpiatto, trucchi attivi** (Allow Cheats: ON), **strutture spente** (nella scheda Mondo, "Genera strutture: No"; in inglese World → Generate Structures: OFF), così attorno a 0 0 non ci sono villaggi. I trucchi attivi ti rendono operatore nel tuo mondo, e servono per `/exoframe debug ...`. Se un mondo è già stato creato senza trucchi: Esc → "Apri in LAN" → trucchi attivi → "Avvia mondo in LAN" (Open to LAN → Allow Cheats: ON → Start LAN World); vale finché non chiudi il mondo. Entra. In `logs\latest.log` nessuna riga `ERROR` o `WARN` che nomina `redstone_exoframes`, `Redstone Exoframes`, `Mixin`, `InvalidInjectionException` o `MixinApplyError`; in particolare nessuna riga che comincia con `Config file`. Sulle due varianti Fabric c'è una riga `No data fixer registered for exoframe`: è nota e innocua.
- [ ] **8.** Prepara le costruzioni di prova, tutte attorno al punto 0 0 del mondo superpiatto (dove il terreno è erba e si sta in piedi a Y −60). Esegui i comandi uno per uno, nell'ordine:
  - vasca di vetro profonda 3 (acqua da Y −60 a −58, pareti alte quattro blocchi, aperta in alto): `/fill 9 -60 -3 15 -57 13 minecraft:glass`, `/fill 10 -60 -2 14 -57 12 minecraft:air`, `/fill 10 -60 -2 14 -58 12 minecraft:water`;
  - canale d'acqua profondo 1 nel terreno, con le sponde alte un blocco: `/fill 20 -61 -2 24 -61 12 minecraft:water`;
  - acqua che scorre: `/setblock 40 -60 -20 minecraft:water`;
  - scalino di pietra alto 1: `/fill -6 -60 3 -3 -60 10 minecraft:stone`;
  - tubo per la colonna di bolle (acqua alta sette blocchi in un tubo di vetro 1 × 1 con il tetto, sabbia delle anime sul fondo): `/fill 30 -61 30 32 -53 32 minecraft:glass`, `/fill 31 -60 31 31 -54 31 minecraft:water`, poi `/setblock 31 -61 31 minecraft:soul_sand`; dopo un secondo nel tubo salgono le bolle (se non salgono, guarda il tubo da vicino e annotalo);
  - pozza di lava profonda 1 nel terreno: `/fill 50 -61 0 54 -61 4 minecraft:lava`;
  - fuoco che non si spegne: `/setblock 60 -61 0 minecraft:netherrack`, poi `/setblock 60 -60 0 minecraft:fire`.

  Ogni comando risponde con il numero di blocchi cambiati; nessun errore.
- [ ] **9.** Prendi gli oggetti: `/gamemode creative`, scheda creativa "Redstone Exoframes": un **Telaio**, un Elmo, un Torso, due Braccia e due Gambe Rottame, tre **Celle di redstone**; poi `/gamemode survival`. Non indossare armatura (gli slot dell'armatura vuoti). Per tornare ogni volta al punto di partenza: `/tp @s 0.5 -60 -5.5`.

## 2. Primo ingresso e riga dell'energia

- [ ] **10.** Sull'erba, lontano dalle costruzioni, piazza il Telaio, monta i sei pezzi (braccia e gambe sui due lati: clic sulla metà sinistra e poi sulla metà destra del corpo del telaio come lo vedi tu, le gambe in basso) e una cella. Mano vuota, clic destro: entri. Si sente un pistone che si ritrae. In chat compare **una volta** "Tieni premuto V per uscire dal telaio, R per cambiare cella" ("Hold V to leave the exoframe, hold R to swap the cell"), con i tasti scritti come sono assegnati.
- [ ] **11.** In alto a sinistra: "Energia 10000 / 10000 RE (100 %)" in **verde**, con l'ombra, e nessuna seconda riga (i pezzi sono nuovi). Pochi secondi dopo il numero scende (consumo da fermo, 2 RE al secondo). Riferimento: `m3c-screenshot/1.20.1-forge-riga-energia.png` (lì in inglese e a metà carica). Sopra la salute la barra dell'armatura mostra i punti dei pezzi montati.
- [ ] **12.** Esci con V (tieni premuto un secondo): la riga dell'energia sparisce. Rientra: la riga dei tasti in chat **non** ricompare. Esci di nuovo con V, poi esci dal mondo (Esc → "Salva ed esci", in inglese Save and Quit to Title), rientra nel mondo e rientra nel telaio: la riga dei tasti ricompare una volta (è una nuova sessione).
- [ ] **13.** Dentro la tuta premi **F1**: la riga dell'energia sparisce con il resto dell'interfaccia; F1 di nuovo: torna. Premi **F3**: la schermata di debug copre la riga; F3 di nuovo: torna. Apri l'inventario (E): la riga resta visibile in alto a sinistra.
- [ ] **14.** **Posizione della riga.** Gioca un paio di minuti dentro la tuta, camminando e guardandoti attorno, anche con la chat aperta (T). Annota per Luigi se la riga in alto a sinistra è comoda, se copre qualcosa (per esempio la chat o i messaggi di altre mod) e dove la vorrebbe, se altrove: la posizione si può cambiare.

## 3. Consumi e `/exoframe debug energy`

Dentro la tuta, in sopravvivenza. Il consumo continua anche mentre scrivi un comando (la chat aperta non mette in pausa il gioco): conta anche quei secondi come "fermo", 2 RE ciascuno.

- [ ] **15.** Esci dalla tuta e scrivi `/exoframe debug energy`: in rosso "Non indossi un telaio" ("You are not wearing an exoframe"). Rientra.
- [ ] **16.** `/exoframe debug energy 10000`: "Carica della cella portata a 10000 RE" ("Cell charge set to 10000 RE"). Subito dopo `/exoframe debug energy`: "Cella: N / 10000 RE (normale)" ("Cell: N / 10000 RE (normal)") con N poco sotto 10000. Prova anche `/exoframe debug energy 99999`: "Carica della cella portata a 10000 RE" (mai oltre la capacità).
- [ ] **17.** **Fermo.** `/exoframe debug energy 10000`, poi resta fermo, senza toccare nulla, per 30 secondi contati; poi `/exoframe debug energy`. La carica è scesa di circa 2 RE per ogni secondo passato dal primo comando (30 secondi più quelli per scrivere: in tutto circa 70-90 RE).
- [ ] **18.** **Cammino.** `/exoframe debug energy 10000`, poi cammina in linea retta sull'erba (solo W, senza Ctrl) per 15 secondi contati; poi `/exoframe debug energy`. La carica è scesa di circa 8 RE per secondo di cammino più 2 per ogni secondo da fermo: circa 120 per il cammino più 20-30 per i comandi.
- [ ] **19.** **Corsa.** Come il passo 18, ma correndo (Ctrl+W, o doppio W) per 10 secondi: circa 200 per la corsa più 20-30 per i comandi. La tuta corre davvero (più veloce del cammino).
- [ ] **20.** **Salti.** `/exoframe debug energy 10000`, poi da fermo salta 5 volte sul posto, poi `/exoframe debug energy`: circa 75 RE per i salti (15 ciascuno) più 2 al secondo per il tempo passato.
- [ ] **21.** **Salto automatico.** Riaccendi per un momento il salto automatico (Opzioni → Comandi → Salto automatico: Sì). `/exoframe debug energy 10000`, cammina contro lo scalino di pietra del passo 8 (`/tp @s -4.5 -60 0.5 0 0`, poi W), poi `/exoframe debug energy`: con il salto automatico il gioco salta il gradino invece di salirlo, e il salto si paga (15 RE in più di un cammino normale). È accettato: annotalo solo se ti sembra strano. Rispegni il salto automatico.
- [ ] **22.** **Creativa.** `/gamemode creative` (resti nella tuta). `/exoframe debug energy 10000`, cammina, corri e salta per 20 secondi, poi `/exoframe debug energy`: "Cella: 10000 / 10000 RE (normale)": in creativa non si consuma nulla. La riga dell'energia resta verde e ferma a 10000. Torna in sopravvivenza (`/gamemode survival`).

## 4. Energia bassa ed esaurita

- [ ] **23.** `/exoframe debug energy 1000`: la riga resta **verde** ("Energia 1000 / 10000 RE (10 %)"): il 10 % esatto è ancora normale. Nessun messaggio, nessun suono.
- [ ] **24.** `/exoframe debug energy 999`: subito sulla barra delle azioni "Energia bassa: niente scatto" ("Energy low: no sprinting") e il suono di un faro che si spegne; la riga diventa **gialla**. Riferimento: `m3c-screenshot/1.21.1-fabric-riga-energia-bassa.png`. `/exoframe debug energy` risponde "(bassa)" ("(low)").
- [ ] **25.** Con l'energia bassa prova a scattare in tre modi: Ctrl+W; doppio W (premi W due volte in fretta e tieni); scatto a interruttore (Opzioni → Comandi → Scatto: Attiva/disattiva, in inglese Sprint: Toggle; premi una volta Ctrl e poi cammina). In nessuno dei tre casi la tuta scatta: va alla velocità del cammino, senza le particelle e senza il campo visivo che si allarga dello scatto. Rimetti lo scatto su "Tieni premuto" (Hold).
- [ ] **26.** `/exoframe debug energy 0`: subito "Energia esaurita: la tuta si muove appena" ("Energy empty: the exoframe barely moves") e il suono di una torcia di redstone che si brucia; la riga diventa **rossa**: "Energia 0 / 10000 RE (0 %)". Riferimento: `m3c-screenshot/1.20.1-fabric-riga-energia-esaurita.png`. Ora la tuta: cammina molto piano (a meno di un terzo della velocità di prima); non scatta; il campo visivo si stringe parecchio (come con la lentezza); la barra dell'armatura sopra la salute si dimezza; salta ancora, e i salti non costano nulla (la carica resta 0, non va sotto). Si esce ancora tenendo V e si cambia ancora cella tenendo R.
- [ ] **27.** **I miglioramenti tacciono.** Da energia esaurita: `/exoframe debug energy 500`: la riga diventa gialla, **nessun** messaggio e **nessun** suono. `/exoframe debug energy 10000`: la riga torna verde, nessun messaggio, nessun suono; lo scatto torna.
- [ ] **28.** **Ingresso con la cella già bassa.** Esci dalla tuta. Prendi una cella a 500 RE: su 1.20.1 `/give @s redstone_exoframes:redstone_cell{exoframes:{energy:500}}`, su 1.21.1 `/give @s redstone_exoframes:redstone_cell[minecraft:custom_data={exoframes:{energy:500}}]`. Con quella cella in mano clicca il telaio (che ha già una cella): le due celle si scambiano (passo 58). Entra: sulla barra delle azioni "Energia bassa: niente scatto" **senza** il suono del faro (entrare non è un peggioramento), e la riga è gialla. Poi `/exoframe debug energy 10000`.
- [ ] **29.** **Una cella vuota non fa entrare.** Esci. `/give @s redstone_exoframes:redstone_cell` (una cella vuota). Scambiala con quella del telaio (clic con la cella in mano) e prova a entrare a mano vuota: "Il telaio ha bisogno di una cella carica (le celle si caricano nel caricatore)". Rimetti nel telaio una cella carica (clic con la cella in mano) ed entra.

## 5. Gradino

Salto automatico spento (passo 4).

- [ ] **30.** Dentro la tuta, `/tp @s -4.5 -60 0.5 0 0` (guardi verso lo scalino di pietra), poi tieni W: la tuta **sale sullo scalino alto un blocco senza saltare**, come su un gradino di mezzo blocco. Prova anche una scala di blocchi pieni (due o tre gradini alti uno, costruiti a mano): la sale camminando. Una staccionata o un muretto (alti un blocco e mezzo) non li sale.
- [ ] **31.** Esci dalla tuta (V) e cammina contro lo stesso scalino senza saltare: **ti fermi contro** (senza tuta il gradino è quello normale, mezzo blocco). Rientra, rifai il passo 30 (sale di nuovo), riesci e riprova (si ferma di nuovo). Fai questo passo con particolare attenzione su 1.20.1, dove il gradino lo alza il client.

## 6. Acqua

Dentro la tuta, energia normale (`/exoframe debug energy 10000` all'inizio della sezione). Gli occhi della tuta finiscono sott'acqua nella vasca profonda 3: nelle bolle dell'aria sopra la barra della fame l'aria cala come per un giocatore, e dopo una quindicina di secondi arriva il danno da annegamento. Non restare sul fondo più di dieci secondi di fila: esci con `/tp @s 0.5 -60 -5.5` e torna.

- [ ] **32.** **Affondare.** `/tp @s 12.5 -57 0.5 0 0` (sopra la vasca, guardando lungo la vasca). La tuta cade fino al fondo come in aria, senza rallentare entrando in acqua, e resta in piedi sul fondo. Al primo contatto con l'acqua, sulla barra delle azioni: "L'esoscheletro affonda: esci camminando, oppure tieni premuto il tasto di uscita per lasciarlo" ("The exoframe sinks: walk out, or hold the exit key to leave it"), una volta sola.
- [ ] **33.** **Spazio.** Sul fondo, tieni Spazio per tre secondi: la tuta **non sale** e non salta. F5 una volta (da dietro): la tuta è in piedi, mai nella posa da nuoto. Riferimento: `m3c-screenshot/1.20.1-fabric-vasca-spazio.png`. F5 altre due volte per tornare in prima persona.
- [ ] **34.** **Camminare sul fondo.** `/tp @s 12.5 -60 -1.5 0 0` e tieni W per due secondi: la tuta cammina sul fondo alla velocità di terra (in due secondi fa circa sette blocchi), non a quella lenta di un giocatore in acqua.
- [ ] **35.** **Correre sul fondo** (il cloud non l'ha provato con lo script finale). `/tp @s 12.5 -60 -1.5 0 0`, poi Ctrl+W per due secondi: la tuta corre sul fondo, più veloce del passo 34, **in piedi**, mai nella posa da nuoto (F5 per guardarla da dietro mentre corre). Annota se lo scatto non parte, se la tuta si mette a nuotare o se torna indietro di scatto.
- [ ] **36.** **Il suggerimento torna a ogni ingresso.** Esci dalla vasca (`/tp @s 0.5 -60 -5.5`), rientra nella vasca (`/tp @s 12.5 -57 0.5 0 0`): il suggerimento **non** ricompare. Lascia la tuta (tieni V sulla terraferma, fuori dall'acqua), rientra nel telaio, torna nella vasca: il suggerimento ricompare una volta.
- [ ] **37.** **La corrente.** `/tp @s 42.5 -60 -19.5` (nell'acqua che scorre dalla sorgente del passo 8): resta fermo tre secondi. La tuta **non viene spostata** dalla corrente (senza tuta un giocatore verrebbe trascinato di qualche blocco).
- [ ] **38.** **Uscire da una sponda alta un blocco, cella piena.** `/exoframe debug energy 10000`, `/tp @s 22.5 -61 0.5 -90 0` (nel canale profondo 1, guardando verso est). Tieni Spazio due secondi: la tuta non si muove (nemmeno qui salta). Poi tieni W: la tuta **sale sulla sponda** senza saltare ed esce sull'erba.
- [ ] **39.** **Uscire da una sponda alta un blocco, cella VUOTA** (mai provato da nessuna parte). `/tp @s 22.5 -61 0.5 -90 0`, poi `/exoframe debug energy 0` (messaggio e suono dell'energia esaurita), poi tieni W per una decina di secondi. Secondo il progetto la tuta, lentissima, sale comunque sulla sponda (il gradino non dipende dall'energia). Annota che cosa succede: se esce, quanto ci mette; se resta nel canale, se con un altro tentativo (girandoti un po', o andando verso l'altra sponda, `/tp @s 22.5 -61 0.5 90 0`) ci riesce; uno screenshot in ogni caso. Se resta bloccata: si esce tenendo V. Poi `/exoframe debug energy 10000`.
- [ ] **40.** **Colonna di bolle** (il cloud non l'ha provata con lo script finale). `/tp @s 31.5 -60 31.5` (dentro il tubo di vetro, sopra la sabbia delle anime). La tuta viene sollevata dalle bolle fino al tetto del tubo, come succederebbe a un giocatore senza tuta, e lì resta con la testa contro il tetto finché sei nella colonna. Annota se non sale, se resta incastrata nel vetro o se la visuale finisce dentro un blocco. Esci con `/tp @s 0.5 -60 -5.5`. Una colonna di bolle conta per il gioco come uno stacco da terra, quindi può costare qualche salto (15 RE ciascuno): non è un errore.
- [ ] **41.** **Controllo in aria** (il cloud non l'ha provato con lo script finale). (a) Sulla terraferma corri e salta, e in aria gira con il mouse e premi A e D: la tuta si muove in aria come un giocatore normale. (b) `/tp @s 12.5 -50 -1.5 0 0` (dieci blocchi sopra il fondo della vasca) e, mentre cadi, tieni W: la tuta cade dritta come in aria, entra in acqua senza rallentare, si sposta in avanti come in un salto normale e atterra sul fondo; nessun danno al pilota. Annota se in aria la tuta non risponde ai tasti, se scatta indietro (torna di colpo dov'era) o se si ferma a mezz'aria.

## 7. Usura dei pezzi, fuoco e lava, caduta

Per vedere i numeri di durata si esce dalla tuta e si smontano i pezzi (accovacciato, mano vuota, clic sulla parte del telaio: in alto esce l'elmo, in basso una gamba; come al passo 20 di `m3b-entrare-e-uscire.md`): al passaggio del mouse il pezzo smontato dice "Durata N / 180" ("Durability N / 180"). Un pezzo nuovo, mai montato, dice "Nuovo" ("New").

- [ ] **42.** **Pezzo critico e pezzo rotto.** Dentro la tuta: `/exoframe debug wear rottame 1` (i sei pezzi Rottame danneggiati, a 90 punti su 180, e una cella piena; "Indossi il telaio di rottame"), poi `/effect give @s minecraft:resistance 600 4 true` (Resistenza V: il pilota non prende danno, ma i pezzi si consumano lo stesso). Poi `/damage @s 80 minecraft:cactus`, e ripetilo aspettando ogni volta almeno due secondi fra un comando e l'altro (un secondo colpo dentro mezzo secondo non conta). Al **terzo** colpo: "Pezzo in stato critico: Torso" ("Critical piece: Torso"), senza suono, e sotto la riga dell'energia compare in **giallo** "Torso in stato critico" ("Torso critical"). Al **quarto**: "Pezzo fuori uso: Torso" ("Broken piece: Torso") con il suono di un oggetto che si rompe, e la seconda riga diventa **rossa**: "Torso fuori uso" ("Torso broken"). Se i messaggi arrivano un colpo prima o dopo non è un errore; conta l'ordine (prima critico, poi fuori uso) e che compaiano una volta sola. Riferimento per la posizione delle righe: `m3c-screenshot/1.20.1-forge-riga-energia.png`.
- [ ] **43.** **Che cosa cambia con il torso rotto.** Subito dopo il passo 42: la barra dell'armatura sopra la salute è calata (il torso rotto non protegge più). `/exoframe debug energy 10000`, resta fermo 20 secondi contati, `/exoframe debug energy`: il consumo da fermo è circa 2,5 RE al secondo invece di 2 (torso rotto: consumi per 1,25).
- [ ] **44.** **Tutti i pezzi rotti.** `/exoframe debug wear rottame 3`: la seconda riga, rossa, dice "Elmo fuori uso" ("Helmet broken") (a parità vince il primo pezzo, l'elmo); nessun messaggio sulla barra delle azioni (i pezzi non si sono rotti adesso). La barra dell'armatura è sparita (nessun pezzo protegge); la tuta cammina più piano di prima (le due gambe rotte tolgono il 15 % ciascuna). Con `/exoframe debug wear rottame 2` la seconda riga è invece gialla, "Elmo in stato critico" ("Helmet critical"), e la barra dell'armatura torna; con `/exoframe debug wear rottame` (pezzi nuovi) la seconda riga sparisce. Mai due righe sovrapposte.
- [ ] **45.** **Lava.** `/exoframe debug wear rottame 2` (pezzi critici, a 26 punti su 180), `/effect give @s minecraft:resistance 600 4 true`. Entra camminando nella pozza di lava (`/tp @s 52.5 -60 -1.5 0 0`, poi qualche passo avanti) e resta dentro. In lava la tuta si muove lenta, come un giocatore nella lava (la regola dell'acqua non vale per la lava). Entro una quindicina di secondi "Pezzo fuori uso: Torso" con il suono della rottura, e poi altri pezzi. Esci dalla lava (`/tp @s 0.5 -60 -5.5`): il pilota brucia ancora per qualche secondo (le fiamme sullo schermo), senza danno con la Resistenza. Annota se la tuta resta incastrata nella lava.
- [ ] **46.** **Fuoco.** `/exoframe debug wear via` ("Eliminato il telaio che indossavi, con i suoi pezzi e la sua cella; l'armatura stivata ti è tornata"). Prendi dalla scheda creativa un altro Telaio, i sei pezzi e una cella (come al passo 9), piazza il telaio, monta tutto, entra, `/effect give @s minecraft:resistance 600 4 true`. Vai sul fuoco del passo 8 (`/tp @s 60.5 -60 0.5`) e restaci 20 secondi. Torna sull'erba (`/tp @s 0.5 -60 -5.5`), aspetta che le fiamme si spengano, esci con V, smonta l'elmo (accovacciato, mano vuota, clic in alto sul telaio): il suggerimento dell'elmo dice "Durata N / 180" con N sotto 180 (il fuoco consuma i pezzi a ogni colpo).
- [ ] **47.** **Caduta.** `/effect clear @s`. Rimonta l'elmo, rientra nel telaio (pezzi nuovi, salvo l'elmo del passo 46). Annota i cuori. `/tp @s ~ ~40 ~` e lasciati cadere sull'erba: si sente il tonfo della caduta, ma il pilota **non perde cuori**. Esci con V e smonta le due gambe (accovacciato, mano vuota, clic in basso sul telaio, due volte) e l'elmo (clic in alto): le gambe dicono circa "Durata 169 / 180" (hanno perso circa 11 punti ciascuna), l'elmo ha perso circa 3 punti rispetto a quanti ne aveva dopo il passo 46: le gambe hanno perso molto più degli altri pezzi. Rimonta tutto.
- [ ] **48.** **Un colpo vero.** Facoltativo: dentro la tuta, in sopravvivenza, fatti colpire qualche volta da uno zombie (`/summon minecraft:zombie ~ ~ ~3`), senza Resistenza: il pilota perde cuori meno che senza armatura, e nulla di strano succede (nessun messaggio finché nessun pezzo diventa critico). Uccidi lo zombie.

## 8. Letti e cavalcature

Da fare su **tutte e quattro** le varianti: il rifiuto del letto il cloud non l'ha visto su 1.20.1 Forge.

- [ ] **49.** **Letto.** `/give @s minecraft:red_bed`, piazzalo sull'erba, entra nella tuta. Di giorno, mano vuota, clic destro sul letto: non ti sdrai, e sulla barra delle azioni compare **una sola riga** "Esci prima dall'esoscheletro" ("Leave the exoframe first"): non due righe, non due messaggi che si alternano, nessun altro messaggio del gioco (per esempio quello del punto di rinascita o "puoi dormire solo di notte"). Riprova con un blocco di terra in mano: stesso risultato. Poi `/time set night` e riprova: stesso risultato, non ti sdrai. `/time set day`.
- [ ] **50.** **Barca.** `/give @s minecraft:oak_boat`, mettila sull'erba (clic destro sul terreno) e salici sopra con il clic destro: vieni fatto scendere subito (al più un istante seduto) con "Esci prima dall'esoscheletro". Annota dove finisci (accanto alla barca, sopra, incastrato).
- [ ] **51.** **Carrello.** `/give @s minecraft:rail 3` e `/give @s minecraft:minecart`, posa tre binari in fila sull'erba e il carrello sopra; clic destro sul carrello: vieni fatto scendere subito, con lo stesso messaggio.
- [ ] **52.** **Cavallo.** `/summon minecraft:horse ~ ~ ~3`, mano vuota, clic destro sul cavallo: vieni fatto scendere subito, con lo stesso messaggio. Annota se tuta o cavallo finiscono in una posizione strana.

## 9. Barre e suggerimenti degli oggetti

Da fare su tutte e quattro le varianti: il codice dei suggerimenti è diverso fra 1.20.1 e 1.21.1. Fuori dalla tuta, inventario aperto (E), mouse sopra l'oggetto.

- [ ] **53.** **Telaio.** L'oggetto Telaio ha sotto il nome quattro righe grigie: "Clic destro su un blocco per piazzarlo", "Clic destro con pezzi e una cella carica per montarli", "Clic destro a mano vuota per entrare", "Accovacciato, clic destro a mano vuota per togliere le cose" ("Right-click a block to place it", "Right-click it with pieces and a charged cell to mount them", "Right-click it with an empty hand to get in", "Sneak and right-click with an empty hand to take things off"). Nessuna barra.
- [ ] **54.** **Celle.** La cella della scheda creativa: "10000 / 10000 RE" in grigio, **nessuna barra** (è piena). Una cella a metà (su 1.20.1 `/give @s redstone_exoframes:redstone_cell{exoframes:{energy:5000}}`, su 1.21.1 `/give @s redstone_exoframes:redstone_cell[minecraft:custom_data={exoframes:{energy:5000}}]`): "5000 / 10000 RE" e una **barra rossa a metà** sotto l'icona. Una cella vuota (`/give @s redstone_exoframes:redstone_cell`): "Vuota: caricala in un caricatore" ("Empty: charge it in a charger") e la barra vuota (solo lo sfondo scuro). Una cella tolta da un telaio dopo un po' di uso mostra la sua carica e la barra. Riferimento: `m3c-screenshot/1.20.1-forge-barre-inventario.png` (gli scatti sono piccoli: guarda l'inventario da vicino).
- [ ] **55.** **Pezzi.** Un pezzo della scheda creativa: "Nuovo" ("New"), **nessuna barra**. Un elmo a metà durata (su 1.20.1 `/give @s redstone_exoframes:scrap_helmet{exoframes:{tier:"scrap",durability:90,max_durability:180}}`, su 1.21.1 `/give @s redstone_exoframes:scrap_helmet[minecraft:custom_data={exoframes:{tier:"scrap",durability:90,max_durability:180}}]`): "Durata 90 / 180" ("Durability 90 / 180") e una barra a metà, gialla, come quella di un attrezzo consumato a metà. I pezzi smontati nei passi 46 e 47: "Durata N / 180" e una barra quasi piena e verde. Un pezzo rotto (`/exoframe debug wear rottame 3`, esci con V e smonta un pezzo): "Durata 0 / 180" e la barra vuota.

## 10. Cambio cella

Dentro la tuta, in sopravvivenza, nessuna schermata aperta.

- [ ] **56.** Togli dall'inventario tutte le celle: `/clear @s redstone_exoframes:redstone_cell` (la cella montata nella tuta non è nell'inventario e resta). `/exoframe debug energy 1000`. Prendi una cella da 5000 RE e una da 8000 RE (il `/give` del passo 54, una volta con `energy:5000` e una con `energy:8000`) e una cella vuota (`/give @s redstone_exoframes:redstone_cell`); ricorda in quale slot è finita la cella da 8000. Tieni premuto **R**: sulla barra delle azioni "Tieni premuto per cambiare cella… N%" ("Hold to swap the cell… N%") che sale; lascialo prima di due secondi: il conto si azzera e non cambia nulla. Riferimento: `m3c-screenshot/1.21.1-fabric-cambio-cella-tenuto.png`. Tienilo di nuovo per più di due secondi: si sente il suono di un'ancora di rinascita che si carica, la riga dell'energia diventa circa "Energia 8000 / 10000 RE (80 %)" (la cella più carica dell'inventario), e nello slot dov'era la cella da 8000 ora c'è la cella vecchia (suggerimento: circa "1000 / 10000 RE"). Nell'inventario le celle sono sempre tre (nessuna in più, nessuna in meno).
- [ ] **57.** Tieni di nuovo R per due secondi: "Nessuna cella nell'inventario ha più energia" ("No cell in your inventory holds more energy"), nulla cambia (la cella da 5000 ne ha meno di quella montata). Tieni insieme **V e R**: vince l'uscita (esci dalla tuta). Rientra. Apri l'inventario e tieni R: non succede nulla (con una schermata aperta il tasto non conta).
- [ ] **58.** **Scambio su un telaio piazzato.** Esci. Con una cella in mano clicca il telaio (che ha già una cella): la cella in mano entra nel telaio, quella che c'era arriva nell'inventario (o a terra se l'inventario è pieno), si sente una botola di ferro che si chiude; nessun "Quello slot è già occupato". In creativa (`/gamemode creative`) la cella in mano resta in mano, e la cella tolta arriva lo stesso nell'inventario. Torna in sopravvivenza.

## 11. Suoni

- [ ] **59.** Ascolta un suono per ogni azione (con i sottotitoli accesi compare anche la riga del gioco base in basso a destra). Gli slot dell'armatura cambiano in silenzio: entrando e uscendo **non** si sente il suono dell'armatura indossata.

| Azione | Suono che si deve sentire |
|---|---|
| piazzare un telaio con l'oggetto | un blocco di metallo posato |
| montare un pezzo o una cella | una botola di ferro che si chiude |
| smontare un pezzo o la cella (accovacciato) | una botola di ferro che si apre |
| entrare nel telaio | un pistone che si ritrae |
| uscire (V) | un pistone che spinge |
| cambio cella con R | un'ancora di rinascita che si carica |
| cella scambiata su un telaio piazzato | una botola di ferro che si chiude |
| un pezzo si rompe | un oggetto che si rompe (come un attrezzo consumato) |
| energia bassa (passo 24) | un faro che si spegne |
| energia esaurita (passo 26) | una torcia di redstone che si brucia |

Annota (passo 59) ogni azione senza suono, con un suono diverso o con il suono due volte.

## 12. Uscita rifiutata dove c'è un altro telaio

- [ ] **60.** Piazza un telaio vuoto sull'erba (oggetto Telaio, o `/exoframe debug place nudo`, che lo piazza ai tuoi piedi; poi fai un passo indietro). Allontanati di qualche blocco ed entra in una tuta (`/exoframe debug wear rottame`, o un telaio armato). Cammina fin dentro il telaio vuoto (il telaio non blocca il passo) e tieni V: l'avanzamento arriva in fondo, poi "Qui c'è già un altro telaio: spostati prima di uscire" ("Another exoframe stands where yours would be left: move first"), e resti nella tuta. Allontanati di due blocchi dal telaio vuoto e tieni V: esci, e il tuo telaio resta in piedi accanto all'altro, senza toccarlo.

## 13. File di configurazione

Il file è `<istanza>\config\redstone_exoframes.json`. Si apre con il Blocco note. Si legge solo all'avvio del gioco: per ogni prova chiudi del tutto il client, cambia il file, riavvia.

- [ ] **61.** (Su tutte e quattro le varianti.) Dopo il primo avvio il file c'è, scritto in JSON indentato, con le sezioni `"energy"`, `"damage"`, `"pieces"`, `"tiers"`, `"modules"`, `"frame"`, `"world"`, `"server"`; fra gli altri valori `"cellCapacity": 10000`, `"walkPerSecond": 8` e `"cellSwapSeconds": 2` dentro `"energy"`, `"exitHoldSeconds": 1` dentro `"frame"`. Nel log nessuna riga `Config file`.
- [ ] **62.** **Un valore cambiato.** Chiudi il gioco. Nel file, dentro `"frame"`, scrivi `"exitHoldSeconds": 3` al posto di `1` e salva. Riavvia, entra nel mondo e in una tuta: tenendo V l'avanzamento sale tre volte più piano (circa 33 % dopo un secondo) e si esce solo dopo tre secondi; lasciando V a due secondi non si esce. Nel log nessuna riga `Config file`.
- [ ] **63.** **Un file scritto male.** Chiudi il gioco. Lascia `"exitHoldSeconds": 3` e in più, dentro `"energy"`, scrivi `"walkPerSecond": 0.5` al posto di `8` (un decimale in un campo che vuole un numero intero). Salva e annota data e ora dell'ultima modifica del file (in Esplora file). Riavvia. Nel log c'è **una** riga `WARN` il cui testo comincia con `Config file` e il percorso del file, dice `does not match the config format`, nomina il valore (`Expected an int but was 0.5`, di solito con `$.energy.walkPerSecond`) e finisce con `using the defaults`. In gioco valgono **tutti** i predefiniti: tenendo V si esce di nuovo dopo **un** secondo (non tre). Riapri il file: è esattamente come l'hai lasciato (`0.5` e `3` ancora lì) e la data dell'ultima modifica non è cambiata: il gioco non lo riscrive.
- [ ] **64.** Chiudi il gioco e cancella il file. Riavvia: il file torna, con i valori predefiniti (`"walkPerSecond": 8`, `"exitHoldSeconds": 1`), e nel log nessuna riga `Config file`.

## 14. L'energia salvata non si perde (facoltativo, avanzato)

Una volta, su una variante qualsiasi. Prova che abbassare la capacità delle celle nel file non toglie energia a una cella già dentro un telaio.

- [ ] **65.** Chiudi il gioco. Nel file, dentro `"energy"`, scrivi `"cellCapacity": 50000`. Avvia, entra nel mondo, `/exoframe debug wear rottame` (la cella che mette è piena alla capacità del file: "Energia 50000 / 50000 RE (100 %)"), poi `/exoframe debug energy 40000`: "Carica della cella portata a 40000 RE", e la riga "Energia 40000 / 50000 RE (80 %)". Esc subito (il menu mette in pausa il mondo) e "Salva ed esci"; chiudi il gioco.
- [ ] **66.** Nel file scrivi `"cellCapacity": 10000`. Avvia e rientra nel mondo (sei ancora nella tuta): la riga dice "Energia 40000 / 50000 RE (80 %)" (o qualche RE in meno, per i secondi passati da fermo): la cella indossata tiene ancora la sua carica e la sua capacità. Nel log c'è una riga `WARN` `1 cell(s) in the frame registry hold more than the configured capacity (10000 RE): they keep their own capacity until they are swapped or drained`. Poi `/exoframe debug wear via`, chiudi il gioco e cancella il file (al prossimo avvio torna con i predefiniti).

## 15. Server dedicato con valori diversi (facoltativo)

Solo per chi prova un server dedicato (come in `m1-setup.md`), su una variante 1.20.1 e una 1.21.1. Il client resta con il suo file di config predefinito (o senza file). Il server ha il suo file in `<cartella-server>\config\redstone_exoframes.json`, scritto al primo avvio del server.

- [ ] **67.** Ferma il server (`stop`). Nel file del **server**: dentro `"energy"` `"cellCapacity": 20000`, dentro `"frame"` `"exitHoldSeconds": 3`. Riavvia il server; dalla console `op <nome>`; entra con il client.
- [ ] **68.** Prendi una cella a 5000 RE con il `/give` del passo 54: il suggerimento dice "5000 / 20000 RE" e la barra rossa è a **un quarto** (con la capacità del server, non con quella del client). `/exoframe debug wear rottame`: la riga dice "Energia 20000 / 20000 RE (100 %)". Tieni V: l'avanzamento sale piano e si esce dopo **tre** secondi, come dice il server; lasciando V a due secondi non si esce.
- [ ] **69.** Esci dal server e apri un mondo in singolo con la stessa istanza: una cella presa con lo stesso `/give` dice "5000 / 10000 RE" con la barra a metà, e V fa uscire dopo un secondo: fuori dal server valgono di nuovo i valori del client.

## 16. Cosa riportare

Per ognuna delle quattro varianti:

- [ ] esito (OK / non OK) di ogni passo numerato fatto su quella variante;
- [ ] se il client non parte: crash report e ultime cento righe di `latest.log`;
- [ ] per ogni passo non OK: uno screenshot e una riga che dice cosa si vede di diverso da ciò che è scritto (o dal riferimento);
- [ ] gli screenshot chiesti anche quando va tutto bene: passo 11 (riga verde), 24 (gialla), 26 (rossa), 42 (seconda riga gialla e poi rossa), 39 (uscita dall'acqua con la cella vuota), 40 (colonna di bolle), 49 (letto con il messaggio), 54-55 (barre e suggerimenti);
- [ ] i numeri letti con `/exoframe debug energy` nei passi 17-20 e 43, con i secondi contati;
- [ ] per i passi 35, 39, 40 e 41 una frase su che cosa succede, anche se è come scritto;
- [ ] ogni riga `ERROR` o `WARN` del log (client e server) che nomina il mod, GeckoLib o un mixin, con qualche riga di contesto; per i passi 63 e 66 la riga `WARN` per intero;
- [ ] versioni realmente usate (loader, Java, dipendenze) se diverse dalla tabella di `m1-setup.md`, e i passi saltati con il motivo;
- [ ] le risposte di Luigi alle domande qui sotto.

Riepilogo finale: una tabella con le quattro varianti per righe e le sezioni 1-15 per colonne, con OK o non OK, più i dettagli dei problemi; "n.d." per le sezioni non fatte su quella variante.

## 17. Domande per Luigi

Nessuna blocca il lavoro: per ognuna c'è scritto cosa fa la mod oggi, e se Luigi non risponde resta così. Le domande 2-7 sono anche nel design (`docs/superpowers/specs/2026-11-13-m3-telaio-design.md`, §8, domande 4-9).

1. **La riga dell'energia** (passo 14): oggi in alto a sinistra, testo con ombra, verde, gialla o rossa, con sotto il pezzo messo peggio. È comoda lì? Si può spostare (per esempio in basso a sinistra, sopra la barra degli oggetti, o accanto al mirino) o cambiare nei colori.
2. **Il costo del salto, 15 RE**: correndo e saltando una cella si svuota in meno di quattro minuti. Va bene, o va abbassato?
3. **Niente letti e niente cavalcature con la tuta** (sezione 8): oggi vietati, con il messaggio. Va bene?
4. **L'acqua** (sezione 6): oggi la tuta in acqua non salta mai, nemmeno dal fondo, e sul fondo cammina alla velocità di terra. Va bene, o deve saltare dal fondo come a terra?
5. **La creativa non consuma** (passo 22): né energia né pezzi. Va bene?
6. **Il gradino sale qualunque blocco pieno** (sezione 5), non staccionate e muretti. Va bene?
7. **Il campo visivo si stringe** con la tuta, molto a energia esaurita (passo 26), come fa il gioco con la lentezza. Dà fastidio?
8. **I consumi in generale**: una cella piena dura circa 83 minuti da fermi, 21 camminando, 8 correndo. Ti sembrano giusti dopo averli provati?
