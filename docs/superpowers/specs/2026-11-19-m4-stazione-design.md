# Milestone 4 — Stazione e HUD: progetto

Data: 19 novembre 2026. Stato: scritto dalla sessione cloud su delega del proprietario, che lo legge dopo e può correggere qualunque scelta. Poi l'ha letto un costruttore che non l'aveva scritto (controllo del 19 novembre): le sue domande hanno qui la risposta, e le scelte nate da quel controllo, fatte da chi coordina, sono segnate "scelta del controllo". Fonti: documento di gioco (il file con `2026-11-04` nel nome, in questa cartella; sigla GIOCO), design operativo (`2026-11-12-redstone-exoframes-design.md`, sigla GEN), progetto della milestone 3 (`2026-11-13-m3-telaio-design.md`, sigla M3; §13 è la parte 3d), `docs/compat.md` (COMPAT), le tre ricognizioni del 19 novembre in `ricognizioni/` (`2026-11-19-m4-requisiti.md`, `2026-11-19-m4-codice.md`, `2026-11-19-m4-piattaforma.md`; i codici RP-, C-, L-, Q-, DEC-, ST-, GUI-, RIP-, VER-, HUD-, TAS-, ANI- sono i loro), e alcuni fatti riletti con `javap` sui jar in cache per questo progetto (segnati [javap]).

**Chi decide che cosa.** Le decisioni D1-D8 sono di chi coordina e qui sono vincolanti; sono citate con la loro sigla. Le voci segnate "scelta del costruttore" sono mie, ciascuna con la sua ragione, e sono correggibili come quelle di M3. Le domande al proprietario sono in §9, ciascuna con il valore che il lavoro segue finché non risponde. Dove una frase di M3 dice altro su stazione, HUD, riparazione, verniciatura o sui tasti, vale questo progetto (in particolare per la riga dell'energia di M3 §12.13). Nei testi per il PC chi esegue le checklist è "l'assistente sul PC".

**Anteprime.** Le immagini di prova (MOCK-UP) di questo progetto stanno in `anteprime/2026-11-19-m4/`, accanto a questo file, con lo script che le disegna (`anteprime.py`, che legge i rettangoli di `layout.py`; vuole anche alcune texture del gioco base estratte a parte, che non entrano nel repository): `gui-stazione.png` e `gui-stazione-640x480.png` (la GUI a scala 2 su 960x540 e alla grandezza minima dell'interfaccia), `hud-destra-1-energia-piena.png`, `hud-destra-2-energia-bassa-pezzo-danneggiato.png`, `hud-destra-3-pezzo-rotto.png`, `hud-destra-4-senza-elmo.png`, `hud-sinistra-2-energia-bassa-pezzo-danneggiato.png` (l'HUD su 960x540 con i riquadri del braccio disegnati), `blocco-stazione.png` (la stazione nel mondo a tre quarti), e `controllo-disposizione.txt`, l'esito del controllo delle sovrapposizioni riportato in §12. Le disegna uno script che legge gli stessi rettangoli scritti in questo progetto; la grafica vera (D8) la generano poi gli script del repository, dopo che il proprietario ha visto le anteprime.

## 1. Intento

Alla fine della milestone 4 il telaio ha la sua officina e il pilota il suo cruscotto. Si costruisce una stazione di manutenzione, ci si mette accanto il telaio piazzato e da una schermata sola si montano e smontano pezzi e cella, si riparano i pezzi con il materiale del loro livello, si dipinge ogni pezzo con le tinte del gioco base (primario e secondario), si salvano e si riapplicano tre combinazioni di colori, si blocca o si sblocca il telaio per gli altri giocatori, guardando la tuta girare in un'anteprima 3D. Sul campo un kit ripara un quarto di un pezzo. Dentro la tuta, al posto della riga di testo di M3, un HUD vero: barra dell'energia con le celle di scorta, sagoma dei sei pezzi colorata per stato, avviso di energia bassa, vignetta del casco, con l'elmo che decide quanto se ne vede, il tasto H per nasconderlo, e niente sopra il braccio della tuta in prima persona, con la mano destra e con la sinistra. Tutto sulle quattro varianti da un solo sorgente.

Perché ora: da M3 un pezzo rotto resta rotto (M3 §8 domanda 12) e i colori si cambiano solo con un comando di debug; la personalizzazione è ciò che i giocatori mostrano (GIOCO, "va curata fin dalla prima release"); la riga dell'energia era un'eccezione dichiarata "fino a M4". La stazione è anche il blocco su cui M5 metterà la fabbricazione dei pezzi dei livelli 2-5 e dei moduli: M4 ne fa le fondamenta senza chiudere quella strada.

## 2. Obiettivi, non obiettivi, anti-obiettivi

**Obiettivi**

1. Blocco stazione di manutenzione 2x1, come un letto: posa, rottura di una qualunque delle due metà, un solo oggetto che cade, ricetta, nessun contenuto salvato nel blocco (D3).
2. GUI della stazione su un telaio piazzato sopra la stazione o entro 2 blocchi: pannello del telaio con i sei pezzi e la cella, pannello del pezzo scelto, anteprima 3D girevole disegnata con `client/SuitDraw`, inventario del giocatore, preset; spazio riservato ai moduli, vuoto (D1).
3. Montaggio e smontaggio dalla stazione di pezzi e cella, con le stesse regole del montaggio a mano.
4. Riparazione alla stazione con il materiale del livello (Rottame: lingotto di ferro, domanda 1) e kit di riparazione da campo, oggetto con ricetta (domanda 2), che ripara il 25 % di un pezzo (`damage.repairKitFraction`).
5. Verniciatura con le sedici tinte del gioco base, per pezzo e per regione (primaria, secondaria), o di tutta la tuta insieme; ritorno al colore del livello; anteprima del colore prima di pagare.
6. Tre preset di colori per giocatore, salvati sul server in un dato salvato loro (D4).
7. Interruttore di sblocco alla stazione (del proprietario, con le regole dei comandi di oggi); `rinforzato` mostrato e conservato (D6); il telaio raccolto come oggetto conserva proprietario, `unlocked` e `reinforced` e li ritrova quando si ripiazza (RP-07).
8. HUD dentro la tuta al posto della riga dell'energia (D2): barra dell'energia con percentuale e celle di scorta, avviso di energia bassa, sagoma dei sei pezzi con i colori del documento di gioco, vignetta del casco disattivabile, tasto H; senza un elmo funzionante solo la barra. Dalla parte giusta per la mano principale, lontano dal braccio della 3d.
9. Preferenze solo del client (HUD nascosto, vignetta) in un file del client (D4).
10. Test in mondo, JUnit, sequenze nel client vero e checklist per il PC per ogni parte; server dedicato avviato e fermato sulle quattro varianti a ogni parte (DEC-46).
11. Animazioni di apertura e posa di ingresso, ma solo come ultima parte, dietro uno spike da buttare, e fuori dal "fatta quando" della milestone finché lo spike non le prova (D7).

**Non obiettivi (arrivano dopo)**: moduli (D1, M5): niente tasti G e N, niente controlli dell'installatore, niente slot né icone dei moduli, niente pacchetto `module_toggle`, niente widget morti; fabbricazione alla stazione dei pezzi dei livelli 2-5 e dei moduli (M5, D1); decalcomanie da stendardo (M5, D1); effetto di `rinforzato` (M5, D6); colore secondario delle gambe (domanda 6); livelli 2-5 (M5 e M7); scintille dello stato critico (C-10, M5 con il passaggio sulla grafica); mixin di rifinitura del lampeggio degli slot bloccati (RP-13, M5: un mixin nuovo, e il lampeggio non toglie nulla al giocatore); animazione del cambio cella (C-09: con le animazioni della parte 4e, se lo spike le prova, altrimenti M5); i riporti grafici di M3 §10-§11 che aspettano le prove sul PC o una risposta del proprietario (Q-02, Q-03, Q-05, Q-06, §11); un'interfaccia di config nel gioco.

**Anti-obiettivi**: nulla si perde e nulla si duplica (DEC-14) per nessuna azione della stazione, del kit, della raccolta o della posa del telaio, anche con due giocatori, con la morte, l'uscita dal server, il cambio di dimensione, l'inventario pieno, il registro in sola lettura (§5.18); nessuna forma, nome o texture riconoscibile di Fallout, Iron Man o Halo, e nessuna forma riconoscibile di un'armatura potenziata famosa nella stazione, nella GUI, nella sagoma dell'HUD o nella vignetta; nessun asset della mod precedente; nessun dato personale nei jar; nessuna differenza di versione o di loader fuori da `compat/impl/`, `platform/` e `compat/Ids.java`; nessun HUD che copra il braccio della tuta, la barra degli oggetti, le barre di stato, il mirino o le righe della chat (D2, §5.12).

## 3. Vincoli

Le regole di progetto restano tutte (DEC-01 … DEC-48 della ricognizione dei requisiti, §5). Quelle che questa milestone tocca di più:

- Un solo sorgente, quattro varianti (1.20.1-forge, 1.20.1-fabric, 1.21.1-fabric, 1.21.1-neoforge), Stonecutter e Architectury (9.2.14 / 13.0.11), GeckoLib 4.8.4 / 4.9.3; direttive solo in `compat/impl/`, `platform/`, `compat/Ids.java` (regole 2 e 6 di `tools/check_sources.sh`).
- `core/` in Java 17 puro, senza Minecraft, loader, Architectury né Gson; ogni tipo nuovo di `core/` citato in un test e con il suo JUnit (regole 1, 1b, 1c; DEC-02).
- Codice solo client nei quattro package client, mai nominato dal codice comune (regole 7 e 8): la schermata, l'HUD, i tasti e il file delle preferenze del client stanno lì; menu, blocco, regole e pacchetti nel codice comune.
- Ogni file che registra un pacchetto C2S nomina `Compat.frameState()` (regola 5). Questa milestone non aggiunge pacchetti C2S (§5.13).
- Il registro dei telai è la verità e vive sul server (DEC-10, DEC-11): prima la scrittura del registro, poi, solo se è riuscita, si prende o si consegna l'oggetto (DEC-16). Nessun innalzamento dello `schema` del registro in M4 (D4).
- Il client non valuta le regole con la propria config e non confronta l'energia con soglie proprie (DEC-23): costi, rese e indicatori arrivano dal server.
- Config: un solo file, letto all'avvio, mai riscritto se esiste (DEC-31, DEC-32, D5); limiti e test di `ConfigValidation` per ogni campo nuovo (DEC-33, DEC-34).
- Suoni solo da `frame/FrameSounds`, dal server, con costanti di `SoundEvents` che sono un `SoundEvent` semplice su tutte e due le versioni (DEC-40).
- Regole della 3c per il codice nuovo: nessun `isCreative()` (capacità `instabuild`, `invulnerable`, `isSpectator()`), in creativa l'oggetto non si consuma, le funzioni di ogni tick catturano `RuntimeException` con un ERROR una volta, ogni regola di tempo confronta `server.getTickCount()` con un valore salvato (DEC-39).
- Mixin solo dove nessun evento copre il bisogno; questa milestone non ne aggiunge (scelta del costruttore, §5.20); un mixin nuovo resta legato alla domanda Q-10.
- Grafica generata da script nel repository, file generati committati e rigenerati identici dalla CI, PNG senza metadati; anteprime al proprietario prima dell'integrazione (DEC-09, D8). Fino alla risposta, segnaposto generati da `tools/gen_placeholders.py`.
- "Nessun test in mondo può vedere un disegno" (DEC-45): ciò che si vede lo provano il client vero nel cloud (scatti e controlli fatti da script) e la checklist.
- Ogni cucitura nuova entra in `docs/compat.md` nello stesso task (DEC-06).

## 4. Approcci considerati

### 4.1 Come le azioni della GUI arrivano al server

Ogni azione della stazione cambia il registro e l'inventario sul server; il client mostra e chiede.

- **A. Pezzi come slot veri del menu, legati al registro.** Il pannello del telaio sarebbe fatto di `Slot` su un contenitore finto che legge e scrive il registro: il gioco base gestirebbe da sé cursore, clic con le maiuscole, tasti numerici, doppio clic, trascinamento e lancio. Scartato: ogni tipo di clic del gioco base (sette: raccogliere, spostamento rapido, scambio con la barra, clonazione, lancio, distribuzione, raccolta di tutto) sposta oggetti prima di chiedere al contenitore, e il client li predice sulla sua copia; un registro in sola lettura o un record sparito nello stesso tick farebbero sparire o raddoppiare un pezzo, proprio la superficie che D3 vuole evitare.
- **B. Pulsanti del menu con `clickMenuButton` (scelto).** Il client manda il pacchetto del gioco base `ServerboundContainerButtonClickPacket` con un numero; il server, che ha già controllato da sé che il menu sia quello aperto, che il giocatore non sia spettatore e che `stillValid` sia vero, chiama `clickMenuButton(giocatore, numero)` del menu, e se risponde vero manda i cambi con `broadcastChanges` [javap: `handleContainerButtonClick`, uguale sulle due versioni]. Il menu decodifica il numero con una tabella in Java puro (§5.6), rilegge il registro, controlla, scrive, poi prende o dà gli oggetti. Il pezzo da montare si prende dallo stack sul cursore, che il server conosce da sé (`getCarried`). Nessun pacchetto C2S nuovo da validare. Vincolo: su 1.20.1 il numero viaggia in un byte con segno [javap: `writeByte`/`readByte` su 1.20.1, `VAR_INT` su 1.21.1], quindi ogni numero sta fra 0 e 127; la verniciatura (sedici tinte per due regioni per sei pezzi) non ci sta se il numero porta anche il pezzo: per questo la scelta del pezzo è uno stato del menu sul server, cambiato da pulsanti suoi (§5.6).
- **C. Un pacchetto C2S per azione, con i suoi dati.** Libero nei dati (slot, regione, colore RGB, nome di un preset), ma con validazioni proprie che il gioco base già fa per B (menu aperto, spettatore, distanza), un file che nomina `Compat.frameState()` (regola 5) e un intervallo da tenere a mano. Scartato per M4; resta la strada se un'azione futura avrà bisogno di dati liberi (nomi dei preset, colori RGB liberi).

Scelto B: meno superficie, le validazioni del gioco base gratis, nessun pacchetto in più verso il server. Lo stato del telaio verso il client viaggia invece con i dati extra all'apertura e con un pacchetto S2C nuovo (§5.5).

### 4.2 Da dove vengono materiali e tinte

- **A. Slot di ingresso transitori, come l'incudine (la via di D3).** Uno slot per il materiale di riparazione (e uno per la tinta) in un contenitore del menu, svuotato alla chiusura con `clearContainer`. Familiare, e il giocatore decide che cosa mettere. Costi: lo spostamento rapido da e verso quegli slot, la restituzione alla chiusura, e un preset che usa più tinte diverse non ci sta in uno slot solo.
- **B. Niente slot: si paga dall'inventario, si monta dal cursore (scelto).** "Ripara" prende il materiale dall'inventario del giocatore, la tinta cliccata nella tavolozza prende quella tinta dall'inventario, un preset prende tutte le tinte che servono; il pannello mostra quante ne ha il giocatore e spegne quello che non può pagare. Un pezzo si monta lasciandolo dal cursore sul suo posto nel pannello del telaio. Nel menu non vive nessun oggetto: alla chiusura non c'è nulla da restituire, la sola cosa fuori dall'inventario resta lo stack sul cursore, gestito dal gioco base come per ogni contenitore (§5.18).
- **C. Slot che restano nel blocco.** Scartato da D3: servirebbe la cucitura degli stack nelle block entity che 1.21.1 non ha, e avrebbe la stessa garanzia debole del caricatore (M3 §7 punto 27).

Scelto B (scelta del costruttore, dentro D3: nessuno slot persistente, e nemmeno transitori): nessuna finestra di perdita in più di quelle del gioco base, preset con più tinte possibili, e la stazione non ha bisogno di una block entity (§5.1). GIOCO parla di "slot del pezzo selezionato (materiali di riparazione, moduli)": il materiale si vede in un riquadro a forma di slot con l'icona e quante unità ha il giocatore, ma non contiene nulla; i moduli di M5 useranno la stessa forma (lasciati dal cursore su un posto del pezzo), se chi farà M5 non sceglie altro.

### 4.3 Dove sta l'HUD

GIOCO vuole l'energia in basso a sinistra, la sagoma in basso a destra, i moduli in alto a destra. Il braccio della tuta in prima persona della 3d occupa, a 960x540 con la scala dell'interfaccia 2, il riquadro 286x205+674+335 con la mano destra e 286x205+0+335 con la sinistra (D2): l'angolo in basso a destra non c'è per un destrorso, quello in basso a sinistra non c'è per un mancino.

- **A. Un gruppo solo nell'angolo in basso opposto al braccio disegnato, che cambia lato con la mano principale (scelto).** Sagoma e barra stanno insieme sotto le righe della chat, accanto al bordo, prima dello slot della seconda mano: in basso a sinistra per la mano destra, in basso a destra per la sinistra, come il gioco base sposta lo slot della seconda mano. È il più vicino a GIOCO (l'energia resta in basso a sinistra per i destrorsi), sta accanto a cuori e barra degli oggetti dove l'occhio va già, e non tocca mai il braccio disegnato: misurato a 960x540, e dedotto per le altre proporzioni della finestra, perché il braccio scala con l'altezza e resta nell'angolo opposto (§12). Costo: due disposizioni da provare; sotto i 424 pixel di larghezza dell'interfaccia (per esempio 1024x768 a scala 3) accanto allo slot della seconda mano non c'è posto, e il gruppo va in alto a sinistra (§5.12).
- **B. Tutto in alto a sinistra, dov'è oggi la riga.** Una disposizione sola, lontana da braccio, chat e barra; ma lontana anche da GIOCO e dal punto in cui si guardano cuori e oggetti, e sotto la schermata di F3.
- **C. La divisione di GIOCO con la sagoma sopra il braccio.** L'energia in basso a sinistra e la sagoma a destra sopra il riquadro del braccio (y sotto 335): due posti, la sagoma a mezz'aria, dove stanno anche il punteggio laterale e i sottotitoli.

Scelto A (scelta del costruttore, permessa da D2; domanda 8): rispetta D2 per le due mani e il controllo sulle anteprime lo conferma (§12). La sagoma resta vista da dietro e non si specchia mai: con la mano sinistra cambiano di posto solo i due sottogruppi (sagoma verso il bordo, barra verso l'interno).

## 5. Il progetto

### 5.1 Il blocco stazione

**Che cos'è.** Blocco `redstone_exoframes:station` ("Stazione di manutenzione", "Maintenance Station"), classe `content/StationBlock` (il nome finisce in `Block`, ST-08), registrato in `content/ModBlocks` prima degli oggetti come il caricatore. Due metà affiancate in orizzontale, come un letto ma per il lungo davanti a chi la posa: proprietà `FACING` (orizzontale, il fronte verso chi l'ha posata) e `PART` (`main`, `side`). Forma di collisione e di contorno: un cubo pieno per metà (scelta del costruttore: un telaio può stare sopra la stazione con i piedi a un blocco da terra, senza altezze strane, e ci si cammina sopra). Durezza e attrezzo come il caricatore: resistenza 3,5, piccone per farla cadere (`requiresCorrectToolForDrops`, tag `minecraft:mineable/pickaxe`), suono di metallo, colore di mappa metallo, nessuna luce. **Nessuna block entity** (D3, §4.2): il blocco non custodisce nulla, il menu nasce dal clic come quello del banco da lavoro.

**Posa.** `getStateForPlacement`: la metà principale va dove il gioco posa il blocco, la laterale alla destra di chi posa guardando la stazione (`pos.relative(FACING.getCounterClockWise())`); si posa solo se quella posizione si può sostituire (`canBeReplaced` con il contesto della posa), sta dentro il bordo del mondo e non ha entità che impediscono di costruire (lo stesso controllo che `BlockItem` fa per la posizione cliccata); altrimenti `null` e l'oggetto resta in mano. `setPlacedBy`, sul server, mette la metà laterale con i segni 3, come fa il letto [ricognizione della piattaforma §4].

**Rottura di una metà.** `updateShape`: se nella direzione dell'altra metà non c'è più l'altra metà della stessa stazione (stesso blocco, l'altra `PART`, stesso `FACING`), la metà rimasta diventa aria. `playerWillDestroy` (cucitura nuova, §5.20: `void` su 1.20.1, `BlockState` su 1.21.1): sul server, solo se la metà cliccata è la **laterale** e il giocatore è in creativa (`instabuild`) o non ha l'attrezzo giusto per far cadere il blocco, toglie prima la metà principale con i segni 35 (senza oggetti) e l'evento 2001 (particelle e suono); poi il gioco rompe la metà cliccata. Cliccando la principale non fa nulla di speciale: la principale da sé non fa cadere nulla (in creativa, o senza attrezzo con `requiresCorrectToolForDrops`), e la laterale diventa aria per `updateShape` senza bottino. È il modo del letto e della porta del gioco base, che agiscono solo dalla metà senza bottino (il letto dal piede, ricognizione della piattaforma §4; la porta dalla metà alta [javap: `DoublePlantBlock.preventDropFromBottomPart` guarda `UPPER`]). Scelta del controllo: togliere per prima la laterale quando si clicca la principale farebbe diventare aria la principale per `updateShape`, con il suo bottino (i vicini ricevono i segni senza il 32, `flags & -34` [javap]), e una stazione cadrebbe anche in creativa o senza piccone. La tabella del bottino fa cadere l'oggetto solo dalla metà principale (`block_state_property` con `part: main`, più `survives_explosion`), come quella del letto. Quindi: rompendo l'una o l'altra metà con il piccone in sopravvivenza cade una stazione (la principale da sé, o tolta da `updateShape` quando si rompe la laterale: la metà che diventa aria passa da `Block.updateOrDestroy`, che chiama `destroyBlock` con gli oggetti se manca il segno 32 [javap, uguale sulle due versioni]; lo prova il test in mondo che rompe la metà laterale con il piccone); senza piccone, o in creativa, nessuna; un'esplosione ne fa cadere al più una; qualunque altro modo di togliere una metà (comando, un'altra mod) fa sparire l'altra con al più un oggetto, dalla principale. I pistoni non la spostano (`PushReaction.BLOCK`, scelta del costruttore: una metà spinta via la spezzerebbe).

**Clic.** Dalla cucitura del clic (`onClick`, §5.20): solo la mano principale, giocatore vivo e non spettatore, come il caricatore; sul client `SUCCESS` (nessun uso dell'oggetto in mano), sul server la ricerca del telaio (§5.2) e l'apertura. Si apre con qualunque cosa in mano; per posare un blocco o il telaio contro la stazione ci si accovaccia (il gioco base allora non chiama il blocco quando si ha un oggetto in mano). Un telaio si mette sopra la stazione accovacciandosi e cliccandone il piano con l'oggetto telaio: la regola di posa del telaio (M3 §5.4) lo appoggia sul piano; servono tre blocchi liberi sopra la stazione.

**Oggetto e dati.** `content/StationItem` (cucitura `CompatBlockItem`, con le righe del suggerimento); modello dell'oggetto piatto, un'icona (scelta del costruttore: una metà del blocco in mano sembrerebbe un cubo qualunque). Ricetta di base (domanda 7): `CCC` / `ITI` / `IrI` (rame, ferro, banco da lavoro, polvere di redstone), categoria `misc`, sbloccata dal lingotto di rame come le altre; simbolo nuovo `T` = `minecraft:crafting_table` in `tools/gen_data.py`, che scrive ricetta, sblocco, bottino con la condizione della metà e il tag del piccone nei due alberi per versione; `tools/check_data.sh` li confronta e il controllo (f3) di `tools/check_jars.sh` nomina la stazione come il caricatore. Modelli, stati e texture: segnaposto di `tools/gen_placeholders.py` fino alla grafica (§6, parte 4d).

### 5.2 Trovare il telaio, aprire, tenere aperto

**Dove deve stare il telaio** (GIOCO: "sul blocco o entro 2 blocchi"; L-02). Regola pura `core/station/StationReach` (JUnit): l'impronta della stazione è il rettangolo orizzontale dei suoi due blocchi; un telaio piazzato è a portata se è nella stessa dimensione, se la distanza orizzontale (euclidea) fra i suoi piedi (la posizione del record) e quel rettangolo è al più `station.reachBlocks` (2,0; zero se i piedi stanno sopra l'impronta, cioè sulla stazione) e se i piedi stanno fra un blocco sotto e due sopra la base della stazione (sul pavimento accanto, un gradino più in basso, o sul piano a un blocco). La stazione agisce solo sui telai piazzati, mai sulla tuta indossata (scelta del costruttore: un percorso solo nel registro, `updatePlaced`, e l'anteprima disegna il telaio che si sta toccando; chi indossa la tuta esce accanto alla stazione con V e la apre: il telaio resta dove era).

**Quale telaio.** Fra i telai piazzati della dimensione (`placedIn`) a portata e che il giocatore può usare (`frame/FramePermissions.canUse`), il più vicino all'impronta; a parità, il più vicino al centro dell'impronta, poi il `frameId` minore in forma testuale (scelta del costruttore: deterministico, e un secondo telaio si sceglie spostando il primo). Nessun telaio a portata: niente schermata, sulla barra delle azioni "Nessun telaio vicino: piazzalo sulla stazione o entro 2 blocchi". Telai a portata ma nessuno usabile: niente schermata, il messaggio di oggi `message.redstone_exoframes.locked`. Così il menu ha sempre un telaio, fissato all'apertura per tutta la sua vita.

**Apertura.** `MenuRegistry.openExtendedMenu(giocatore, provider, scrittore)` di Architectury, uguale sulle due versioni; i dati extra (§5.5) portano la posizione della metà principale, il `frameId`, la scelta iniziale e la vista del telaio. Tipo del menu registrato in `content/ModMenus` (`DeferredRegister` di `Registries.MENU` con `MenuRegistry.ofExtended`). Il costruttore del menu sul client legge tutto subito e non tiene il buffer (Fabric 1.21.1 lo rilascia appena torna).

**Tenere aperto.** `StationMenu.stillValid(giocatore)` sul server, che il gioco chiama a ogni tick del giocatore e prima di ogni pulsante [javap: `Player.tick` e `handleContainerButtonClick`], è vero solo se: (1) il giocatore è nel livello della stazione (l'aiuto statico del gioco base non confronta le dimensioni [javap: la sua funzione riceve solo blocco e giocatore]); (2) l'aiuto statico `stillValid(accesso, giocatore, blocco)` del gioco base dice sì (il blocco c'è ancora, la distanza: 8 blocchi dal centro su 1.20.1, con l'attributo di portata di Forge su Forge 1.20.1, `canInteractWithBlock(pos, 4.0)` su 1.21.1); (3) il giocatore è vivo e non spettatore; (4) il record del telaio è ancora piazzato, ancora a portata della stazione e ancora usabile dal giocatore. Falso: il gioco chiude la schermata al tick dopo (`closeContainer`, poi `removed`). Quando la ragione è il telaio (raccolto, indossato, tolto con un comando, bloccato dal proprietario) il server lo dice una volta sulla barra delle azioni: "Il telaio non è più disponibile alla stazione" (scelta del costruttore: chi si allontana dal blocco non ha bisogno di spiegazioni, come per un forziere).

### 5.3 La GUI: disposizione e widget

Schermata di 316x232 pixel dell'interfaccia, centrata. Il gioco garantisce almeno 320x240 pixel dell'interfaccia (`Window.calculateScale` alza la scala solo finché larghezza e altezza restano almeno 320 e 240 [javap]): la schermata ci sta sempre, con 2 e 4 pixel di margine; a 960x540 e scala 2 sta in 480x270 con `leftPos` 82 e `topPos` 19 (scatto `gui-stazione.png`; `gui-stazione-640x480.png` è la grandezza minima). Stile della mod: acciaio scuro, bronzo e rame, il rosso della redstone; testi chiari (il grigio scuro del gioco base non si legge sul fondo scuro, quindi `renderLabels` si ridefinisce). Coordinate relative all'angolo della schermata, `(x0, y0, x1, y1)` con `x1`, `y1` esclusi:

```
x 0  7         73  77                                    211  215                 309  316
y  +--------------------------------------------------------------------------------------+
 6 | Stazione di manutenzione                                         [scudo] [lucchetto] |
17 | +-----------+ +---------------------------------------+ +------------------------+  |
   | |  [ elmo ] | | Torso Rottame                     [^] | |                        |  |
   | |[bd][to][bs]| | Danneggiato               81 / 180   | |     anteprima 3D       |  |
   | | [gd]  [gs] | | ##############.................... | |    (trascinare per     |  |
   | |-----------| | [ferro x12] [ Ripara ] [ Tutto: 3 ]   | |       girarla)         |  |
   | | [cella]   | | [ Primario ] [ Secondario ]           | |                        |  |
   | |   [Tutti] | | 16 tinte in due file      [predef.]   | |                        |  |
   | |           | | (moduli: spazio riservato a M5, vuoto) | | <                    > |  |
135| +-----------+ +---------------------------------------+ +------------------------+  |
139| Inventario                            +- Preset ------------------------------------+ |
149| [][][][][][][][][]                    | 1 colori x12  [applica] [salva]             | |
   | [][][][][][][][][]                    | 2 colori x12  [applica] [salva]             | |
   | [][][][][][][][][]                    | 3 vuoto       [applica] [salva]             | |
205|                                       | Costo: 1 Tinta rossa / Ne hai 4             | |
207| [][][][][][][][][]   (barra)          +---------------------------------------------+ |
232+--------------------------------------------------------------------------------------+
```

| Widget | Riquadro | Che cosa mostra | Clic |
|---|---|---|---|
| titolo | 8,6 | "Stazione di manutenzione" | — |
| rinforzato | 276,3-290,15 | uno scudo di bronzo, solo se il telaio è `reinforced`; suggerimento "Rinforzato" | — |
| lucchetto | 292,3-308,15 | lucchetto aperto o chiuso; suggerimento "Sbloccato: chiunque può usarlo" o "Bloccato: solo il proprietario", più "Solo il proprietario può cambiarlo" se il giocatore non può | blocca o sblocca (§5.11); spento se il giocatore non può |
| pannello del telaio | 7,17-73,135 | i sei posti dei pezzi in forma di corpo, visto di fronte come l'anteprima all'inizio (il braccio destro del telaio a sinistra): elmo 30,21-50,41; braccio destro 9,43-29,63; torso 30,43-50,63; braccio sinistro 51,43-71,63; gamba destra 19,65-39,85; gamba sinistra 41,65-61,85. Ogni posto è uno slot 18x18 dentro un bordo di 1 pixel del colore della condizione (verde, giallo, rosso, grigio come l'HUD, §5.12) con l'icona dell'oggetto del pezzo; vuoto, l'icona in trasparenza del tipo di pezzo; il pezzo scelto ha un secondo bordo bianco. Suggerimento: nome, livello, durata, condizione | clic con il cursore vuoto: sceglie quel pezzo; clic con un pezzo sul cursore: lo monta lì (§5.7); clic con le maiuscole su un pezzo montato: lo smonta nell'inventario |
| cella | 19,101-39,121 | lo slot della cella con bordo rosso redstone e la carica nel suggerimento ("7420 / 10000 RE") | come i pezzi, con una cella: con il cursore vuoto la sceglie, con una cella la monta o la scambia, con le maiuscole la smonta |
| Tutti | 41,103-71,119 | pulsante | sceglie tutti i pezzi montati: riparazione e verniciatura valgono per tutti (scelta del costruttore: riparare o dipingere la tuta intera con un clic) |
| nome e smonta | 81,22 e 195,20-207,32 | nome del pezzo scelto (o "Cella", o "Tutti i pezzi"); un pulsante a freccia | smonta il pezzo scelto (o la cella) nell'inventario |
| condizione e durata | 81,34; 152,34-207,43; barra 81,44-207,49 | condizione nel suo colore, "81 / 180", barra della durata | — |
| materiale | 81,52-99,70 | riquadro a forma di slot con l'icona del materiale del livello e quante unità ne ha il giocatore (nessun oggetto dentro); spento se ne ha zero | — |
| Ripara | 102,53-146,69 | pulsante; suggerimento "+45 punti, 1 Lingotto di ferro" | ripara di un'unità (§5.8) |
| Tutto | 148,53-207,69 | "Tutto: 3" (le unità che servono fino al massimo, o quelle che il giocatore ha) | ripara fin dove si arriva |
| regioni | 81,73-143,85 e 145,73-207,85 | "Primario" e "Secondario", quella attiva premuta; "Secondario" spento per le gambe (domanda 6), con il suggerimento "Le gambe non hanno ancora un colore secondario" | sceglie la regione (solo nel client) |
| tavolozza | 81,88-191,110 | sedici riquadri 12x10 nell'ordine di `DyeColor` (due file da otto, passo 14 e 12); spento se il giocatore non ha quella tinta (in creativa mai) | passandoci sopra l'anteprima mostra quel colore sulla regione scelta; clic: vernicia (§5.9) |
| predefinito | 193,88-207,110 | i due colori del livello | torna al colore del livello, gratis |
| moduli | 81,113-171,131 | niente: spazio riservato a M5 (cinque posti da 18), senza disegni (D1) | — |
| anteprima | 215,17-309,135 (ritaglio 216,18-308,134) | la tuta in 3D (§5.4), con due frecce in basso | trascinare con il tasto sinistro gira la tuta |
| inventario | 7,149-169,203 e barra 7,207-169,225; etichetta 8,139 | l'inventario del giocatore, slot del gioco base | come nel gioco base (gli oggetti si prendono e si posano; il cursore porta i pezzi al pannello del telaio) |
| preset | 175,138-309,225; righe 177,150 / 168 / 186, alte 16 | per riga: il numero, dodici riquadri 5x8 con primario e secondario dei sei pezzi (o "vuoto"), pulsante "applica" (icona del pennello, 18x16) e "salva" (icona del libro, 18x16) | applica: dipinge il telaio con il preset (§5.10), passandoci sopra l'anteprima lo mostra; salva: scrive i colori del telaio nel preset |
| riga dei costi | 177,205-307,224 | due righe: il costo dell'azione sotto il mouse ("Costo: 1 Tinta rossa") e quante ne ha il giocatore, o "Gratis in creativa", o che cosa manca ("Ti manca: 2 Tinta nera") | — |

Il controllo delle anteprime (§12) verifica che i cinquantasei widget non si sovrappongono e stanno dentro la schermata. Testi e larghezze sono misurati con il font del gioco (per esempio "Secondario" è largo 55 pixel su 62, "Stazione di manutenzione" 123 su 132): un testo inglese più lungo si accorcia nel file della lingua, non si allarga il widget.

### 5.4 L'anteprima 3D

Disegnata nel `render` della schermata con `client/SuitDraw.draw`, l'unico disegno della tuta (COMPAT, "SuitRenderer"), sulla posa di `GuiGraphics`: `pushPose`; traslazione al centro in basso del ritaglio e in avanti di 50 in z; scala `(s, s, -s)` con `s` = 32 pixel dell'interfaccia per blocco (la tuta armata, alta circa 3,09 blocchi, sta nei 116 pixel del ritaglio); rotazione attorno all'asse verticale dall'angolo trascinato; poi la stessa trasformazione privata di `client/ExoframeEntityRenderer` (180° − angolo, ribaltamento, scala 0,9375, piedi a `SuitDraw.FEET_Y`), estratta in un aiuto comune dei due; `Lighting.setupForEntityInInventory()`; `SuitDraw.draw(entità, modello, posa, graphics.bufferSource(), luce piena 0xF000F0, partialTick 0, vista, sollevamento 0)`; `graphics.flush()`; `Lighting.setupFor3DItems()`; `popPose`. Le firme sono uguali sulle due versioni (`PoseStack.scale`, `mulPose(Quaternionf)`, `translate`; `Lighting.setupForEntityInInventory()`, `GuiGraphics.bufferSource()`, `flush()`, `enableScissor`) [javap]. Il ritaglio: `enableScissor` sul riquadro dell'anteprima prima del disegno, `flush()` prima di `disableScissor()` (su 1.21.1 il gioco ritaglia da sé solo dentro `renderEntityInInventoryFollowsMouse`, che qui non si usa).

- **Entità e modello.** L'entità passata è il giocatore locale, che c'è sempre a schermata aperta e non è un animabile di GeckoLib (il contratto di `SuitRenderer`); il modello di base è un `HumanoidModel` cotto da `ModelLayers.PLAYER` con `young` falso, uno per schermata, come quello di `ExoframeEntityRenderer`. Niente `renderEntityInInventory*`: vogliono un `LivingEntity` e firme diverse sulle due versioni.
- **Vista.** Una `SuitView` fatta sul client dalla vista del telaio ricevuta (§5.5), con stato e colori di ogni pezzo e i colori predefiniti di `client/TierPalette` per i non verniciati; passando il mouse su una tinta o sul colore predefinito, la regione scelta dei pezzi scelti si mostra di quel colore; su "applica" di un preset, il preset intero. Si paga solo cliccando: il colore si vede prima.
- **Rotazione.** Trascinando con il tasto sinistro dentro il riquadro dell'anteprima l'angolo cambia di 1,5° per pixel dell'interfaccia (scelta del costruttore); fuori dal riquadro `mouseDragged` va al gioco base, che lo usa per distribuire gli oggetti negli slot. Si parte di fronte (0°), come il pannello del telaio; l'angolo vive nella schermata e riparte a ogni apertura. Nessuna animazione, partialTick 0 (il `float` di `render` non è il partialTick su nessuna delle due versioni).
- **Se non riesce.** `SuitDraw` non lancia mai dentro il fotogramma (scrive l'errore una volta): il riquadro resta vuoto e il resto della schermata funziona. La prima prova (parte 4a, task 1) lo verifica sulle quattro varianti nel client vero; se su una variante non disegna, il lavoro si ferma lì e la decisione torna a chi coordina, con un ripiego pronto: disegnare nel riquadro l'entità del telaio piazzato con il suo renderer (`EntityRenderDispatcher.render`, che accetta qualunque `Entity` sulle due versioni), già provato nel mondo, rinunciando ai colori in prova prima di pagare.

### 5.5 Come lo stato del telaio arriva al client

Al client oggi arriva solo il proprio stato indossato e il riassunto visivo dei telai che vede (COMPAT, "Sincronizzazione solo del proprio stato"); la durata, la cella e le bandierine di un telaio piazzato restano sul server, e `FrameStateCodec` è chiuso in `compat/impl`. La stazione non lo apre (scelta del costruttore): manda una vista sua, con solo ciò che la schermata mostra.

- **`core/station/StationView`** (record in Java puro, JUnit), fatta sul server dallo stato del record: per ogni slot montato livello, durata, durata massima, primario e secondario (o non verniciato), e i punti che un'unità di materiale gli renderebbe; la cella (presente, carica, capacità); `unlocked`, `reinforced`; se il giocatore può cambiare il blocco (`canToggleLock`); se la regola `server.ownerOnlyFrames` è accesa (`ownerOnly`, calcolato sul server, perché il client non usa la propria config, DEC-23; scelta del controllo); la scelta attuale del menu (un byte: 0-5 lo slot, 6 la cella, 7 tutti; scelta del controllo, §5.6); se il telaio custodisce un'armatura (`hasStowed`, solo il sì o il no, mai che cosa); se il giocatore non paga (`free`, la capacità `instabuild`); i costi dalla config del server (id del materiale del Rottame, tinte per regione nella verniciatura e nei preset); i tre preset del giocatore e se i preset sono in sola lettura. Non ci sono il proprietario, i moduli, i toggle, i riporti del danno né l'armatura stivata. Nella 4a la vista porta pezzi, cella, bandierine, `canToggleLock`, `ownerOnly`, la scelta, `hasStowed` e `free`; costi e preset arrivano con la 4b, e record e codec crescono lì (nessun formato salvato: cambia solo il pacchetto, che client e server hanno sempre della stessa versione).
- **Codec** `station/StationViewCodec implements PacketCodec<StationView>`: interi variabili, booleani, una stringa per l'id; niente NBT né codec degli stack, così vale sui quattro buffer (su Fabric 1.21.1 è un `FriendlyByteBuf` semplice, su NeoForge un `RegistryFriendlyByteBuf` [ricognizione della piattaforma §1]). Meno di un kilobyte, contro il limite di 32 600 byte di Forge e NeoForge.
- **All'apertura**: nei dati extra del menu, con la posizione della stazione, il `frameId` e la scelta iniziale (il primo pezzo montato nell'ordine di `PieceSlot`, altrimenti l'elmo).
- **Mentre la schermata è aperta**: `StationMenu.broadcastChanges()`, che il server chiama a ogni tick e dopo ogni pulsante, rifà la vista e la confronta con l'ultima mandata (uguaglianza dei record); se è cambiata manda a quel giocatore il pacchetto S2C `redstone_exoframes:station_view` (numero del menu, vista). Così ogni cambio, da qualunque parte venga (un secondo giocatore a un'altra stazione sullo stesso telaio, un montaggio a mano, un kit usato sul telaio, un comando), si vede entro un tick, senza che nessuno lo debba annunciare. Quante tinte e quanto ferro ha il giocatore il client lo conta dal proprio inventario, che il gioco base sincronizza; il server ricontrolla a ogni azione.
- **Sul client** il gestore del pacchetto, codice comune, guarda `containerMenu` del giocatore: se è uno `StationMenu` con quel numero ci mette la vista, altrimenti (un pacchetto arrivato dopo la chiusura) non fa nulla. La schermata legge la vista del menu a ogni fotogramma.

### 5.6 Le azioni: pulsanti del menu

Tutte le azioni passano da `clickMenuButton` (§4.1). Il numero si decodifica con `core/station/StationAction` (Java puro, JUnit per ogni numero valido e per quelli fuori tabella, negativi compresi); un numero che non c'è non fa nulla e non scrive nel log.

| Numeri | Azione |
|---|---|
| 0-5 | scegli il pezzo dello slot (ordine di `PieceSlot`: elmo, torso, braccio sinistro, braccio destro, gamba sinistra, gamba destra) |
| 6 | scegli la cella |
| 7 | scegli tutti i pezzi montati |
| 8-13 | monta nello slot il pezzo che il giocatore ha sul cursore |
| 14 | monta la cella che il giocatore ha sul cursore (se ce n'è già una, quella va all'inventario) |
| 16-21 | smonta il pezzo dello slot nell'inventario |
| 22 | smonta la cella nell'inventario |
| 24 | ripara di un'unità la scelta |
| 25 | ripara la scelta fin dove si arriva |
| 26 | blocca o sblocca il telaio |
| 32-47 | primario della scelta con la tinta 0-15 (ordine di `DyeColor`: bianco, arancione, magenta, azzurro, giallo, lime, rosa, grigio, grigio chiaro, ciano, viola, blu, marrone, verde, rosso, nero) |
| 48 | primario della scelta al colore del livello |
| 64-79 | secondario della scelta con la tinta 0-15 |
| 80 | secondario della scelta al colore del livello |
| 96-98 | applica il preset 1-3 |
| 100-102 | salva nel preset 1-3 |

Il numero più alto è 102: ci sta nel byte di 1.20.1. La regione attiva, l'angolo dell'anteprima e il passaggio del mouse restano nel client; la scelta del pezzo è sul server perché il numero di una tinta non porta il pezzo.

**La scelta** (scelta del controllo). La tiene il menu sul server e cambia solo con i pulsanti 0-7, mai da sola: uno slot che si svuota (lo smonta un altro giocatore, da un'altra stazione o a mano) resta scelto e le azioni su di lui non fanno nulla e non costano nulla (§5.18). La vista la porta (§5.5): il client la cambia subito al clic, come il gioco base fa con i pulsanti dei suoi menu, e se la vista dice altro vince il server. Test in mondo: si sceglie un pezzo, un altro giocatore lo smonta, si vernicia: nulla cambia e nulla si paga. I pacchetti dello stesso giocatore arrivano e si eseguono in ordine sul thread del server, quindi "scegli" e poi "vernicia" non si scambiano.

**Ordine dei controlli sul server**, per ogni azione: (1) numero valido; (2) giocatore vivo (il gioco base ha già escluso spettatori e menu non validi); (3) per le azioni che scrivono (tutte tranne la scelta), al più una per tick del server per menu: le altre dello stesso tick si ignorano, e se il contatore del server è tornato indietro il limite riparte (DEC-39); (4) il record del telaio c'è ancora; (5) il giocatore lo può usare (`canUse`, riletto a ogni azione); (6) i controlli dell'azione (§5.7-§5.11); (7) che il giocatore abbia ciò che paga, salvo `free`; (8) un solo `updatePlaced` con tutto il cambio; (9) solo se è riuscito, si prende dall'inventario ciò che si paga, si dà ciò che si smonta (`frame/Handouts.give`: ciò che non entra cade fermo ai piedi del giocatore), si rifà il riassunto dell'entità del telaio se è caricata, suona. Un `updatePlaced` che non riesce con il record ancora lì (registro in sola lettura) non prende nulla e dice "La stazione non è riuscita a cambiare il telaio (vedi il log del server)" sulla barra delle azioni. Si paga dall'inventario nell'ordine del cambio cella: barra degli oggetti 0-8, inventario 9-35, seconda mano per ultima; mai dal cursore. Il riassunto dell'entità oggi lo rifà `FrameInteractions.refreshSummary`, privato: diventa un aiuto pubblico in `frame/` (per esempio `FramePlacement.refreshView(livello, frameId)`), usato dalle mani, dalla stazione e dal kit.

### 5.7 Montare e smontare dalla stazione

Le regole sono quelle del montaggio a mano (M3 §5.4), con il codice in comune: la parte che oggi sta in `FrameInteractions.mountPiece` e nello smontaggio passa in un aiuto di `frame/` che chiamano tutti e due (scelta del costruttore: una regola sola, due ingressi).

- **Montare un pezzo (8-13).** Sul cursore un oggetto pezzo del tipo dello slot (`PieceKind.slots()`); lo slot vuoto, altrimenti "Quello slot è già occupato" (il messaggio di oggi) e nulla cambia; il pezzo da `PieceItem.toPiece`, moduli con `restoreModules` come a mano (i controlli dell'installatore arrivano con gli oggetti modulo, D1), aspetto dai dati dello stack; un solo `updatePlaced`; poi lo stack sul cursore cala di uno (non in creativa, la convenzione della 3b). Il client lo sa senza pacchetti nuovi: dopo un pulsante che risponde vero il gioco chiama `broadcastChanges`, che manda anche il cursore (`synchronizeCarriedToRemote` [javap, uguale sulle due versioni]). Nessuno scambio: per cambiare un pezzo si smonta prima (scelta del costruttore: uno scambio metterebbe un oggetto sul cursore, l'unica scrittura del cursore che la stazione non ha).
- **Montare la cella (14).** Scelta del costruttore: D3 e GIOCO parlano di pezzi, ma la cella è ciò che serve per entrare, e montarla dalla stessa schermata evita di uscirne; la regola è quella delle mani. Sul cursore una cella: un solo `updatePlaced` toglie la vecchia e mette la nuova (con la capacità della config, M3 §12.10); il cursore cala di uno (non in creativa); la vecchia, se c'era, va all'inventario con `Handouts.give`, come a mano.
- **Smontare (16-22).** L'oggetto si costruisce prima di cambiare il registro (`FrameInteractions.pieceStack`; un livello senza oggetto lancia e il pezzo resta montato, con l'avviso di oggi), poi `updatePlaced`, poi `Handouts.give`: nell'inventario, e ciò che non entra cade fermo ai piedi del giocatore con il ritardo di raccolta. Dalla stazione non si raccoglie il telaio vuoto e non si ritira l'armatura stivata: restano gesti a mano, e la schermata, se il telaio custodisce un'armatura, lo dice con una riga ("Custodisce un'armatura: si ritira a mano, accovacciati").
- **Suoni**: quelli di oggi, `MOUNT` e `UNMOUNT` di `FrameSounds`, nel punto del telaio.

### 5.8 Riparare, e il kit da campo

**Alla stazione.** Materiale per livello; in M4 c'è solo il Rottame: `station.repairMaterialScrap`, di base `minecraft:iron_ingot` (domanda 1; GIOCO dà "ferro + rame" per il Rottame e "il materiale del tier (ferro, diamante, netherite…)" per la riparazione). Un'unità di materiale ripara il pezzo di `Piece.repairFraction(station.repairUnitFraction)`, di base 0,25: 45 punti su 180 per un pezzo Rottame, arrotondati all'intero più vicino e mai oltre il massimo, come un'unità di materiale all'incudine del gioco base (domanda 1). "Ripara" (24) dà un'unità al pezzo scelto, o a ogni pezzo montato sotto il massimo se sono scelti tutti, al costo di un'unità per pezzo; se il materiale non basta per tutti, ripara quelli che può nell'ordine di `PieceSlot` (in parte, come "Tutto": ogni unità sta da sé; il suggerimento del pulsante dice quanti pezzi raggiunge; scelta del controllo); "Tutto" (25) dà a ogni pezzo scelto le unità che servono per arrivare al massimo (per eccesso: l'ultima può andare oltre il massimo e costa intera, come all'incudine), finché il materiale basta, nell'ordine di `PieceSlot`. Un pezzo già al massimo: pulsante spento, e sul server "Questo pezzo non ha bisogno di riparazioni". Un pezzo rotto si ripara allo stesso modo e cambia condizione: si rifà il riassunto dell'entità (il telaio nel mondo cambia aspetto), e gli effetti tornano quando il telaio si indossa. In creativa nulla da pagare. Regola pura `core/station/RepairRule` (punti di un'unità, unità che servono, unità che si possono pagare; JUnit). Suono `REPAIR` (`SMITHING_TABLE_USE`, `BLOCKS`), nel punto del telaio.

**Kit di riparazione** (`redstone_exoframes:repair_kit`, "Kit di riparazione", "Repair Kit"; `content/RepairKitItem` sulla cucitura `CompatItem`; pile da 16, scelta del costruttore). Ripara di `damage.repairKitFraction` (0,25, già in config) il pezzo messo peggio: la durata relativa più bassa fra i pezzi sotto il massimo (confronto in interi, come la riga dell'energia di M3), a parità nell'ordine di `PieceSlot`; un pezzo rotto è sempre il primo. Dove si usa:

- **dentro la tuta**: clic destro con il kit in mano (in aria o su un blocco: `Item.use`, con la stessa firma sulle due versioni [javap]); scrittura con `changeWorn` (strutturale, sincronizza subito al proprietario); il riassunto del segnaposto del torso lo riscrive la guardia degli slot quando la condizione cambia;
- **su un telaio piazzato** (scelta del costruttore: "sul campo" comprende il telaio ritrovato dopo una morte lontano da una stazione): clic destro sul telaio con il kit; una voce nuova in `FrameInteractions`, con il permesso di ogni azione sul telaio (`canUse`), `updatePlaced` e il riassunto rifatto;
- altrove: "Usa il kit dentro la tuta o su un telaio piazzato", e il kit resta.

Nessun pezzo da riparare: il kit resta, "Nessun pezzo da riparare". Uso istantaneo, senza attesa (scelta del costruttore: una durata d'uso vorrebbe una cucitura, perché `Item.getUseDuration` cambia firma fra le due versioni [javap]). In creativa non si consuma. Messaggio dopo l'uso con il pezzo e i punti ("Torso: +45"); il suggerimento dell'oggetto non scrive numeri (il client non conosce la config del server). Ricetta di base (domanda 2): `_I_` / `CrC` / `_I_` (due lingotti di ferro, due di rame, una polvere di redstone), un kit per ricetta (il generatore dei dati vuole un risultato da uno), categoria `equipment`, sbloccata dal lingotto di rame. Suono `KIT` (`ANVIL_USE`, `PLAYERS`), dove sta il pezzo.

### 5.9 Verniciare con le tinte del gioco base

**Colori.** Le sedici tinte del gioco base, con i valori RGB del primo intero del costruttore di ogni `DyeColor`, uguali su 1.20.1 e 1.21.1 [javap: stessi sedici interi nello stesso ordine]. Su 1.20.1 il gioco li tiene anche come tre `float` e su 1.21.1 come ARGB opaco: la mod usa la tabella degli interi, in `core/station/DyePalette` (indice = ordine di `DyeColor`, nome = `DyeColor.getName()`), e non li ricalcola mai dai `float`. Una tinta in inventario si riconosce con `getItem() instanceof DyeItem` e `getDyeColor()`, uguali sulle due versioni (valgono anche le tinte di altre mod che estendono `DyeItem`).

| # | Tinta | RGB | | # | Tinta | RGB |
|---|---|---|---|---|---|---|
| 0 | bianco (`white`) | `F9FFFE` | | 8 | grigio chiaro (`light_gray`) | `9D9D97` |
| 1 | arancione (`orange`) | `F9801D` | | 9 | ciano (`cyan`) | `169C9C` |
| 2 | magenta (`magenta`) | `C74EBD` | | 10 | viola (`purple`) | `8932B8` |
| 3 | azzurro (`light_blue`) | `3AB3DA` | | 11 | blu (`blue`) | `3C44AA` |
| 4 | giallo (`yellow`) | `FED83D` | | 12 | marrone (`brown`) | `835432` |
| 5 | lime (`lime`) | `80C71F` | | 13 | verde (`green`) | `5E7C16` |
| 6 | rosa (`pink`) | `F38BAA` | | 14 | rosso (`red`) | `B02E26` |
| 7 | grigio (`gray`) | `474F52` | | 15 | nero (`black`) | `1D1D21` |

Più il "colore del livello" (`Appearance.UNPAINTED`), che per il Rottame è primario `8A6F52`, secondario `4A4F55` (`client/TierPalette`). Una tinta è un colore: le tinte non si mescolano come nel cuoio (scelta del costruttore: sedici colori per due regioni per sei pezzi bastano, e il protocollo non cambierebbe se il proprietario volesse la miscela: il server calcolerebbe il colore da quello di prima e dalla tinta).

**Per pezzo e per regione.** Primario e secondario di ogni pezzo montato (`Appearance(primary, secondary, decals)`; `FrameState.setAppearance` lancia senza un pezzo nello slot, quindi si controlla prima); le decalcomanie non si toccano (M5). Con "Tutti" la stessa regione di tutti i pezzi montati. **Gambe** (C-06, domanda 6): oggi il pezzo delle gambe ha solo la regione primaria, quindi il secondario delle gambe è spento nella schermata e rifiutato sul server (i numeri 64-80 non fanno nulla su una gamba scelta; con "Tutti" le gambe si saltano). La regola sta in `core/station/PaintRegions.hasSecondary(slot)`, e un JUnit la lega alla geometria generata contando i cubi: vero se e solo se almeno un osso `_p2` di quel pezzo, in uno qualunque dei quattro stati, ha almeno un cubo. Le ossa `_p2` delle gambe esistono, vuote, perché lo schema dei nomi di `compat/SuitBones` le prevede per ogni slot, e restano (un osso cercato e assente darebbe un WARN, COMPAT); oggi, sommando i quattro stati, elmo 8 cubi, torso 68, ogni braccio 16, ogni gamba 0 (scelta del controllo); quando la grafica darà alle gambe un secondario, il test fallisce finché la regola non cambia con lei.

**Costo** (domanda 3): `station.paintDyesPerRegion` tinte di quel colore (di base 1) per ogni regione di un pezzo che cambia davvero; ridipingere una regione dello stesso colore non costa e non fa nulla; tornare al colore del livello è gratis; in creativa gratis. Tinte che mancano: nulla cambia, "Ti manca: 2 Tinta nera". Dopo il cambio si rifà il riassunto dell'entità: tutti i client vedono il telaio ridipinto senza pacchetti nuovi (VER-08), e la tuta indossata dopo porta i colori nel riassunto del segnaposto del torso. I pezzi sciolti non si dipingono alla stazione: si montano, si dipingono, si smontano, e i colori restano sull'oggetto (`PieceItem.fromPiece` con l'aspetto). Suono `PAINT` (`DYE_USE`, `PLAYERS`), nel punto del telaio.

### 5.10 Preset

Un preset è, per ognuno dei sei slot, una coppia facoltativa di colori (primario e secondario, ciascuno RGB o colore del livello): uno slot senza coppia non si tocca applicandolo. Tre per giocatore, numerati 1-3, senza nome (scelta del costruttore: un nome vorrebbe testo dal client, cioè un pacchetto C2S; il numero e la striscia di dodici colori bastano a riconoscerli).

- **Salvare (100-102)**: dai colori attuali del telaio, per ogni slot con un pezzo montato; uno slot vuoto resta senza coppia; sostituisce il preset che c'era; gratis. Suono `PRESET_SAVED` (`BOOK_PAGE_TURN`, `PLAYERS`).
- **Applicare (96-98)**: per ogni slot con una coppia e un pezzo montato, il primario, e il secondario salvo le gambe (§5.9); le regioni già uguali non cambiano. Costo (domanda 4): `station.presetDyesPerRegion` (di base 1) tinte del colore di arrivo per ogni regione che cambia verso una tinta; verso il colore del livello gratis. Un colore che non è una delle sedici tinte (un preset salvato da un telaio dipinto con il comando di debug o da un operatore) non si può pagare: l'applicazione si rifiuta tutta, "Il preset contiene un colore che non viene da una tinta"; in creativa tutto è gratis e si applica. Tutto o niente: se manca qualcosa nulla cambia e "Ti manca: …" nomina la prima tinta che manca nell'ordine della tavolozza; altrimenti un solo `updatePlaced` con tutti gli aspetti, poi si prendono le tinte. Suono `PAINT`.
- **Regole pure**: `core/station/ColorPreset` (record) e `core/station/PresetPlan` (cambi e costi da aspetti, preset e tavolozza), con i loro JUnit. Dove si salvano: §5.14. I preset viaggiano al client con la vista (§5.5); se il dato dei preset è in sola lettura (§5.14) "applica" funziona e "salva" è spento, con il suggerimento "I preset sono in sola lettura su questo server".

### 5.11 Sbloccato e rinforzato; il telaio raccolto e ripiazzato

- **`unlocked`** (D6). Pulsante del lucchetto (26), per chi può: il proprietario del telaio o un operatore di livello 2, e un telaio senza proprietario solo un operatore, la stessa regola di `/exoframe unlock|lock` (`command/ExoframeCommand.setUnlocked`), che restano. `server.ownerOnlyFrames` resta com'è: se è spento tutti usano ogni telaio comunque, il pulsante scrive lo stesso il valore (conta se la regola si riaccende) e il suggerimento lo dice ("Su questo server tutti possono usare ogni telaio"), sapendolo dalla vista (`ownerOnly`, §5.5). Scrittura con `updatePlaced`; sulla barra delle azioni i testi del comando (`commands.redstone_exoframes.unlock.done`, `lock.done`); suoni `LOCK` e `UNLOCK` (`IRON_DOOR_CLOSE`, `IRON_DOOR_OPEN`, `BLOCKS`). Bloccare chiude la schermata di un altro giocatore aperta sullo stesso telaio al tick dopo (non può più usarlo, §5.2).
- **`reinforced`** (D6). Solo mostrato: lo scudo nella schermata e una riga nel suggerimento dell'oggetto telaio. Nessuna regola lo legge e la stazione non lo scrive; l'effetto è una domanda per M5 (domanda 5). Per le prove nel cloud e sul PC: `/exoframe debug reinforced <true|false>` (operatori, come gli altri rami di debug) sul telaio indossato o su quello guardato (scelta del costruttore: senza, in M4 nessuna prova potrebbe vedere un telaio rinforzato).
- **Raccolta e posa** (RP-07). Quando il telaio vuoto si raccoglie a mano, l'oggetto riceve con `ItemDataAccess.setFrame` `unlocked`, `reinforced` e il proprietario **solo se a raccogliere non è il proprietario** (cella e pezzi assenti: si raccoglie solo vuoto). `ExoframeItem.useOn` li rilegge con `ItemDataAccess.frame`: il proprietario dell'oggetto se c'è, altrimenti chi lo posa (la regola di oggi, che vale per un telaio fabbricato), e le due bandierine. Così (domanda 11, valore corretto dal controllo): un telaio sbloccato raccolto da un altro e ripiazzato resta del suo proprietario, e "sbloccato" presta il telaio senza regalarlo; un telaio raccolto dal suo proprietario è libero, e chi lo posa ne diventa proprietario, quindi si può regalare, scambiare o ritrovare dopo una morte senza che diventi inservibile. Il valore di prima (il proprietario scritto a ogni raccolta) faceva posare a chi riceveva l'oggetto un telaio altrui e bloccato, che non poteva né usare né raccogliere di nuovo, contro "nulla si perde" (§2). Un altro giocatore raccoglie un telaio solo se lo può usare (`FramePermissions.canUse`): quindi un oggetto con il proprietario viene da un telaio sbloccato, salvo un operatore in creativa o un server con `server.ownerOnlyFrames` spento, casi da amministratore. Un oggetto telaio con pezzi o cella nei dati (il gioco non lo scrive; solo un comando può farlo) non si posa: "Questo telaio porta pezzi o una cella: si posa solo vuoto", e l'oggetto resta (scelta del costruttore: posarlo vorrebbe un percorso nuovo per pezzi e cella dai dati di un oggetto, che il gioco non scrive; rifiutare non perde nulla). Il suggerimento dell'oggetto aggiunge "Sbloccato" e "Rinforzato" quando sono veri; mai il nome del proprietario.

### 5.12 L'HUD

**Livelli** (HUD-01, C-04; AUD-01). Regola pura `core/frame/HudLevel.of(stato)` (JUnit), che M5 riuserà per i visori ("elmo rotto: niente visori"): `FULL` se l'elmo è montato e non è rotto (un elmo critico funziona ancora: GIOCO dice "rotto"), `BAR_ONLY` se si indossa la tuta senza elmo o con l'elmo rotto (anche il telaio nudo): si vedono la barra con la percentuale, le celle di scorta e l'avviso, sulla loro piastra (scelta del costruttore su C-04: D2 dice "solo la barra dell'energia", e GIOCO descrive quella barra come "barra energia con percentuale e numero di celle di scorta"; l'avviso è quello di energia bassa che GIOCO vuole nell'HUD, HUD-06), `NONE` se il giocatore locale non indossa la tuta (`SuitSize.wearsSuit`) o il client non ha la sua copia dello stato (`clientView`). Il client la calcola dalla propria copia dello stato, che è del server: non è una soglia del client (DEC-23). Niente si disegna anche con l'interfaccia nascosta (F1), con l'HUD nascosto da H, da spettatore. In terza persona restano barra e sagoma, non la vignetta (scelta del costruttore: uno spettatore non pilota la tuta; la vignetta è il vetro dell'elmo visto da dentro). Sotto una schermata l'HUD resta come quello del gioco base (la riga per scrivere in chat copre gli ultimi 14 pixel dello schermo, come copre la barra degli oggetti).

**Dove** (§4.3, D2). Un gruppo di 88x35 pixel dell'interfaccia, a 2 pixel dal bordo laterale, che comincia 3 pixel sotto l'ultima riga della chat (la chat finisce a `h − 40`): `y0 = h − 37`; mano principale destra `x0 = 2`, sinistra `x0 = w − 90`. A 480x270 (960x540, scala 2): `(2, 233)` e `(390, 233)`; in pixel dello schermo 176x70+4+466 e 176x70+780+466, con il solo elemento della barra 134x70+46+466 e 134x70+780+466. Due sottogruppi: la sagoma, 21x35, verso il bordo; l'energia, 67x35, verso l'interno; con la mano sinistra si scambiano di lato, ma l'interno non si specchia mai (la sagoma resta vista da dietro, il testo si legge da sinistra). Se accanto allo slot della seconda mano non c'è posto (`w/2 − 124 < 88`, cioè `w < 424`: per esempio 1024x768 a scala 3, 341x256, o la grandezza minima 320x240) il gruppo va in alto a sinistra, `(2, 2)`, con la disposizione della mano destra, per tutte e due le mani; lì le barre dei boss, di passaggio, possono passarci sopra. Con la mano sinistra, nell'angolo in basso a destra compaiono di passaggio l'indicatore del salvataggio (per un secondo, a ogni salvataggio automatico) e, se attivi nelle opzioni, i sottotitoli: si sovrappongono al gruppo, sopra di lui su NeoForge 1.21.1 e sotto su Fabric 1.21.1 e su 1.20.1 (dipende dal loader: "Vignetta", qui sotto); accettato (scelta del costruttore: spostare il gruppo più in alto lo porterebbe nelle righe della chat, e i due elementi sono brevi o facoltativi). I riquadri del braccio sono misurati solo a 960x540 con la scala 2 (M3 §13); per le altre grandezze il controllo li ricava scalandoli con l'altezza, come scala il disegno in prima persona: è una deduzione, e gli scatti della 4c la provano solo a 960x540. I numeri e il controllo sulle anteprime sono in §12; `client/HudLayout`, Java puro con JUnit, li calcola per mano e grandezza dell'interfaccia.

**Elementi** (pixel dell'interfaccia, relativi al sottogruppo; colori ARGB):

| Elemento | Riquadro | Disegno |
|---|---|---|
| piastra di fondo | tutto il gruppo, 88x35 (con `BAR_ONLY` solo il sottogruppo dell'energia, 67x35) | nero al 60 % (`99101216`) con un bordo di 1 pixel di rame al 43 % (`6EB87333`) |
| sagoma | dentro il sottogruppo della sagoma: elmo 6x6 a (8,3); torso 8x11 a (7,10); braccio sinistro 3x11 a (3,10); braccio destro 3x11 a (16,10); gamba sinistra 3x11 a (7,22); gamba destra 3x11 a (12,22) | vista da dietro, il braccio sinistro a sinistra; ogni pezzo pieno del colore della sua condizione con un contorno di 1 pixel a un terzo di quel colore; un pezzo non montato: fondo scuro (`5A000000`) e contorno grigio (`FF464646`). Solo con `FULL` |
| avviso | 8x8 a (2,8) del sottogruppo dell'energia | triangolo con il punto esclamativo, nel colore dell'energia; con `LOW` lampeggia, mezzo secondo acceso e mezzo spento a partire dal tick in cui lo stato è diventato `LOW`; con `EMPTY` fisso; altrimenti non c'è |
| percentuale | da (2,8), o da (12,8) dopo l'avviso | carica per difetto, `hud.redstone_exoframes.percent` ("%s %%" in italiano, "%s%%" in inglese), con ombra, nel colore dell'energia; senza cella il testo `hud.redstone_exoframes.no_cell`, "Senza cella" ("No cell"), in grigio, con ombra, da (2,20), sulla riga della barra (che senza cella è vuota), al posto della percentuale; "Nessuna cella" è largo 69 pixel in una riga di 63 (decisione 1 del piano della 4c) |
| celle di scorta | allineate a destra a (65,8) | un'icona di cella 5x8 e "×N", bianco, grigio se N è 0 (HUD-02, L-07: le celle dell'inventario, barra, inventario e seconda mano, con carica maggiore di zero, contate dal client sui dati degli stack una volta per tick) |
| barra | cornice 63x8 a (2,20); riempimento 61x6 | bordo `FF0C0D0F`, fondo `FF282A2E`, riempimento in proporzione a carica / capacità della copia dello stato, nel colore dell'energia, con la prima riga più chiara; senza cella vuota; prima del primo pacchetto con gli indicatori, bianca come la riga di oggi |
| vignetta | quattro angoli di 72x40, fissi: `(0,0)`, `(w−72,0)`, `(0,h−40)`, `(w−72,h−40)` | §"Vignetta" qui sotto; solo con `FULL` e in prima persona |
| moduli | riservato: `x` da `w − 100` a `w − 2`, `y` da 54 a 72 (a 480: 380-478), sotto le due file di effetti del gioco base | niente in M4 (D1); lì può stare il punteggio laterale: è un problema di M5 |

**Grafica della parte 4d** (stile A, "Officina"; disegni della texture `textures/gui/hud.png`, scritta da `tools/art/gui.py`), con due scostamenti dalla tabella che il proprietario ha accettato il 27 novembre 2026. Il bordo di rame della piastra è al 71 % di opacità (`B4`) invece del 43 %, con il rilievo (più chiaro in alto e a sinistra) e un rivetto di bronzo in ogni angolo. La cornice della barra è un canale d'acciaio incassato (ombra nera in alto e a sinistra, labbro chiaro in basso e a destra, fondo `FF292D33` con le tacche dei quarti) invece del bordo `FF0C0D0F` pieno sul fondo `FF282A2E`. Il resto sta dentro i riquadri della tabella, com'era nelle anteprime che il proprietario ha visto: il fondo della piastra resta al 60 %, ma nell'acciaio più scuro dello stile A, `9914161A`, non `99101216`; l'elmo ha gli angoli in alto smussati e il torso si stringe in vita, quindi questi due pezzi non riempiono più tutto il loro rettangolo; il punto esclamativo dell'avviso è opaco, d'acciaio scuro (`FF101114`); con "×0" l'icona della cella di scorta è spenta, più scura.

**Colori, e il conflitto C-03.** Una legenda sola per tutto l'HUD:

| Significato | Colore | Sagoma (condizione del pezzo) | Energia (stato dal server) |
|---|---|---|---|
| va bene | verde `55FF55` | integro | `NORMAL` |
| danneggiato | giallo `FFFF55` | danneggiato (dal 15 al 50 %) | — |
| agire subito | rosso `FF5555` | critico (sotto il 15 %) | `LOW` (sotto il 10 %: niente scatto, moduli spenti) |
| fuori servizio | grigio `8A8A8A` | rotto | `EMPTY` |

GIOCO dà alla sagoma "verde, giallo, rosso, grigio = rotto" (D2). La barra prende la stessa legenda, così un colore vuol dire la stessa cosa dovunque nell'HUD; il server ha tre stati dell'energia e il client non confronta la carica con soglie sue (DEC-23), quindi sulla barra il giallo non compare mai. Cambia rispetto alla riga di M3: `LOW` era giallo e diventa rosso, `EMPTY` era rosso e diventa grigio (scelta del costruttore, domanda 9). Il bordo dei pezzi nella GUI della stazione usa la stessa legenda (§5.3). Verde, giallo e rosso sono i colori della chat del gioco base già usati dalla riga; il grigio è nuovo.

**Vignetta** (HUD-05). Solo con `FULL`, in prima persona e con la preferenza accesa. Quattro angoli di 72x40 pixel dell'interfaccia, disegnati per primi nel gancio dell'HUD, sotto barra e sagoma: un oscuramento dall'angolo (al più 0,42 di opacità nel pixel d'angolo, a zero sul lato opposto del pezzo) e, nei due angoli in alto, due riflessi deboli del vetro dell'elmo. Texture `textures/gui/hud_vignette.png`, segnaposto fino alla grafica (parte 4d). Solo gli angoli (scelta del costruttore): così, a ogni grandezza da 384 pixel in su, la vignetta resta fuori dalla barra degli oggetti, dalle barre di stato e dalle righe della chat (gli angoli in basso cominciano a `h − 40`, dove la chat finisce); sotto 384 gli angoli in basso passano sopra lo slot della seconda mano, con una tinta leggera (accettato). Elementi del gioco base che la toccano solo di passaggio, accettati: in alto a destra le icone degli effetti e gli avvisi, in basso a destra l'indicatore del salvataggio e i sottotitoli. Differenza fra i loader: il gancio dell'HUD disegna dopo tutto l'HUD del gioco base, quindi su 1.20.1 la vignetta sta sopra quegli elementi, e su NeoForge 1.21.1 sotto (gli strati del gioco base sono a z più alte [ricognizione della piattaforma §6]). Tre fatti trovati eseguendo la 4c: gli avvisi (le notifiche a comparsa) il gioco li disegna dopo tutta l'interfaccia su tutte e quattro le varianti, quindi stanno sempre sopra la vignetta; su Forge 1.20.1 l'indicatore del salvataggio non si vede mai, perché l'interfaccia di Forge non lo disegna; su Fabric 1.21.1 la vignetta sta sopra quegli elementi come su 1.20.1, perché Fabric API manda il suo evento dell'HUD alla fine di `Gui.render`, dopo che il test di profondità si è spento, mentre NeoForge lo manda dentro il gestore degli strati, con il test acceso, dove gli strati più alti vincono (misurato sull'icona di un effetto; i sottotitoli e l'indicatore del salvataggio seguono lo stesso meccanismo). La tinta è leggera e la differenza si guarda negli scatti della 4c e nella checklist. Un gancio prima dell'HUD (come la vignetta del gioco base) vorrebbe un mixin su Fabric, che non ha strati: scartato.

**Tasti** (TAS-01, TAS-03). `H`: `key.redstone_exoframes.hide_hud`, predefinito H, nella categoria della mod, registrato come V e R; letto con `consumeClick` nel tick del client, senza schermate aperte: nasconde o mostra tutto l'HUD della mod (vignetta compresa), lo scrive nel file delle preferenze e lo dice sulla barra delle azioni ("HUD della tuta nascosto: %s per mostrarlo" / "HUD della tuta visibile"); vale fra le sessioni. Vignetta: `key.redstone_exoframes.vignette`, **senza tasto predefinito** (come il "contorno degli spettatori" del gioco base), da assegnare nelle impostazioni dei controlli; accende e spegne la vignetta, nello stesso file, con un messaggio (scelta del costruttore, domanda 10: GIOCO la vuole "disattivabile" e nomina solo H; un tasto predefinito in più toglierebbe una lettera ad altre mod, un pulsante nella stazione costringerebbe ad andarci). G e N non esistono in M4 (D1). La riga dei tasti al primo ingresso (TAS-04) nomina anche H: "Tieni premuto %s per uscire dal telaio, %s per cambiare cella; %s nasconde l'HUD".

**Al posto della riga** (HUD-07, M3 §12.13). `client/EnergyHud`, `client/EnergyLine` e `EnergyLineTest` se ne vanno; disegna `client/SuitHud`, agganciato con la cucitura di oggi `ClientRenderHooks.addHudRenderer` (il partialTick non serve: l'HUD cambia a tick); `client/HudModel` (Java puro: livello, riempimento, percentuale, celle di scorta, colore di ogni slot, avviso) e `client/HudLayout` (Java puro: i rettangoli per mano e grandezza) con i loro JUnit. Le chiavi `hud.redstone_exoframes.energy` e `.worst` se ne vanno, `hud.redstone_exoframes.no_cell` resta con il testo nuovo. La seconda riga (il pezzo messo peggio) sparisce: la sagoma mostra ogni pezzo, e senza elmo GIOCO non vuole informazioni sui pezzi; i messaggi della barra delle azioni per un pezzo critico o rotto e i suoni restano (DEC-21).

**Che cosa si rompe spostando la riga, e si aggiorna nella parte 4c** (D2):

- `tools/client/arm_measure.sh`: il rettangolo `ENERGY=360x30+0+0`, le attese del suo `--selftest` e il campo `energia=` diventano il rettangolo dell'HUD della mano dello scatto (`176x70+4+466` con la destra, `176x70+780+466` con la sinistra) e il campo `hud=`;
- `tools/client/m3d_shots.sh`: il criterio (b) sul rettangolo dell'HUD (il braccio non ci entra mai, per costruzione), il testo di `CRITERIA` e gli scatti in cui si vedeva la riga;
- `tools/client/m3c_shots.sh`: gli scatti del passo 8 (`riga-energia`, `riga-energia-bassa`, `riga-energia-esaurita`) diventano `hud-energia`, `hud-energia-bassa`, `hud-energia-esaurita`, con gli stessi controlli sulla carica, e il commento sulla posizione;
- `tools/client/README.md` (righe delle sequenze 3c e 3d) e la riga sugli scatti del `README.md` della radice;
- gli scatti di riferimento `docs/test-in-gioco/m3c-screenshot/*riga-energia*.png` e quelli della 3d con la riga in vista, rifatti;
- le checklist `m3c-giocarci.md` (passi 11-14, 24, 26, 46, 61) e `m3d-braccio.md` (passi 3 e 19, tabella dei criteri), e le note di `m3a` e `m3b` che nominano la riga: una nota in testa, "Con i jar della parte 4c …", nella forma di quelle che ci sono, senza cambiare i passi;
- i Javadoc di `client/FirstPersonArm` (criterio b), `frame/SuitDamage`, `frame/DebugEnergy`; `docs/compat.md` (il paragrafo della riga e l'uso di `addHudRenderer`).

Il progetto di M3 resta com'è, come storia: per la riga di §12.13 e per il rettangolo dell'energia del criterio (b) di §13 vale questo progetto.

**Mappa a due mani o nella seconda mano.** Con una mappa a due mani il gioco disegna le due braccia della tuta, una per angolo (M3 §13), e con una mappa nella seconda mano disegna il braccio di quel lato: il gruppo resta dov'è e sta sopra quel braccio, come la barra degli oggetti sta sopra la mappa. Fuori dai due riquadri di D2, che sono quelli a riposo: accettato (scelta del costruttore).

### 5.13 Rete

| Pacchetto | Direzione | Contenuto | Chi lo manda | Validazione e limiti |
|---|---|---|---|---|
| dati extra dell'apertura | S2C (apertura della schermata del loader) | posizione della metà principale, `frameId`, scelta iniziale, `StationView` | `openExtendedMenu` all'apertura | fatti dal server; il menu del client li legge una volta |
| `redstone_exoframes:station_view` (nuovo) | S2C | numero del menu (intero variabile), `StationView` | `StationMenu.broadcastChanges`, solo se la vista è cambiata | al più una volta per tick per menu aperto; il client lo ignora se il suo menu aperto non ha quel numero; su NeoForge 1.21.1 un client senza il canale si salta (COMPAT) |
| `ServerboundContainerButtonClickPacket` (gioco base) | C2S | numero del menu, numero del pulsante 0-127 | i widget della schermata | gioco base: menu aperto, non spettatore, `stillValid`; la mod: §5.6 (vivo, una scrittura per tick per menu, record, `canUse`, controlli dell'azione, pagamento) |
| clic sugli slot (gioco base) | C2S | solo gli slot dell'inventario del giocatore | gioco base | il menu della stazione ha solo l'inventario del giocatore; lo spostamento rapido sposta fra inventario e barra degli oggetti, come in un menu senza contenitore; nessuno spostamento rapido monta pezzi (§4.1) |

Nessun pacchetto C2S nuovo: la regola 5 non cambia e `module_toggle` resta di M5 (D1). `client_rules` non cambia: costi e rese della stazione viaggiano nella vista, e l'HUD non ha bisogno di valori nuovi. Il pacchetto nuovo si registra nell'init comune (`station/StationNet.register()`), il suo gestore è codice comune (§5.5).

### 5.14 Dati salvati (D4)

| Dato | Dove | Versione | Che cosa fa un jar più vecchio |
|---|---|---|---|
| registro dei telai | `data/redstone_exoframes_frames.dat` | `schema` 2, invariato: M4 scrive solo chiavi che esistono (aspetto, durata, pezzi, cella, `unlocked`) | un jar della milestone 3 lo legge come oggi |
| preset | `data/redstone_exoframes_presets.dat`: un `SavedData` suo nell'overworld; `schema`, poi `players` → UUID in forma testuale → `p1`, `p2`, `p3` → slot in minuscolo (`helmet`, `torso`, …) → `color_primary`, `color_secondary` (chiavi di `NbtKeys`; colore assente = colore del livello, slot assente = nessuna coppia, preset assente = vuoto) | `schema` 1 | un jar della milestone 3 non lo chiede mai, e il gioco non apre né cancella i file di dati che nessuno chiede: resta sul disco com'è e torna buono con il jar nuovo. Uno `schema` maggiore di 1 letto da M4: preset in sola lettura, un ERROR, nessuna scrittura, come il registro |
| oggetto telaio raccolto | dati dell'oggetto, radice `exoframes`: `owner`, `unlocked`, `reinforced` (chiavi di oggi) | — | `useOn` della milestone 3 ignora i dati: telaio nuovo di chi lo posa, bandierine perse (come oggi), nessun errore |
| stazione e kit | stato del blocco nel chunk (`facing`, `part`); l'oggetto kit | — | senza la mod che li conosce il gioco toglie blocchi e oggetti sconosciuti, come per ogni contenuto di una mod tolta |
| preferenze del client | `config/redstone_exoframes-client.json` | `version` 1 | un jar della milestone 3 non lo legge |
| config | `config/redstone_exoframes.json`, sezione nuova `station` | — | un jar della milestone 3 ignora la sezione (le chiavi in più si ignorano, DEC-32) |

- **Perché i preset in un file loro** (D4): una sezione nuova dentro il registro la perderebbe in silenzio il primo salvataggio di un jar più vecchio (riscrive solo le sezioni che conosce), e alzare lo schema metterebbe tutti i telai in sola lettura per quel jar [ricognizione della piattaforma §8]. Cucitura nuova `compat/PresetAccess` (`Compat.presets()`), con `PresetAccessImpl` e `PresetSavedData` in `compat/impl/` (`save` e `computeIfAbsent` cambiano firma fra le versioni). Scrittura con il salvataggio del gioco (`setDirty`), con copia a ogni cambio: la voce di un giocatore è immutabile e il riferimento alla mappa si sostituisce intero, così `save` vede sempre uno stato intero da qualunque thread lo chiami (su NeoForge 1.21.1 il gioco scrive i `SavedData` in un altro thread, COMPAT). Un crash può perdere gli ultimi preset salvati dopo il salvataggio automatico: accettato, sono preferenze e non oggetti (scelta del costruttore).
- **File dei preset illeggibile** (scelta del controllo). Se il gioco non riesce a leggere un dato salvato scrive "Error loading saved data", ne crea uno vuoto e al primo cambio sovrascrive il file [javap: `readSavedData` cattura l'eccezione su 1.20.1 e 1.21.1]: i preset di tutti i giocatori sparirebbero in silenzio. Quindi, nel ramo che crea il dato, se il file esiste già i preset vanno in sola lettura con un ERROR una volta, come per uno `schema` più nuovo ("salva" spento, §5.10), e il file non si riscrive. Test in mondo con il file rovinato e la riga ERROR prevista nel log.
- **La regola dello schema** (D1, come la decide chi coordina): resta quella di oggi, chi aggiunge un livello, uno slot o un id di modulo alza lo `schema` del registro (RP-10, COMPAT "Limiti noti"); M4 non ne aggiunge, quindi non lo alza (D4).
- **Il file delle preferenze del client** (D4): `config/redstone_exoframes-client.json`, nella cartella di config della piattaforma, oggetto JSON `{"version": 1, "hudHidden": false, "vignette": true}`; letto una volta all'avvio del client (`configfile/ClientPrefsFile`, Java e Gson come `ConfigFile`, con JUnit). Lo scrive il client a ogni cambio fatto con H o con il tasto della vignetta: tutto il file in un temporaneo della stessa cartella, poi spostato al suo posto in un colpo, tenendo le chiavi che non conosce (quelle di una versione più nuova restano). File assente: HUD visibile e vignetta accesa, e il file nasce al primo cambio. Illeggibile, non JSON o con tipi sbagliati: i predefiniti, un WARN con il nome del file, e il primo cambio lo sostituisce con uno valido (il WARN lo dice). Chiave assente: il suo predefinito, in silenzio. A differenza della config della mod, questo file la mod lo riscrive: è uno stato del client, non una configurazione scritta a mano.

### 5.15 Config (D5)

Sezione nuova `station` di `core/config/ExoframesConfig`, con i limiti in `ConfigValidation` e i casi in `ExoframesConfigTest` e `ConfigFileTest` (DEC-34):

| Campo | Predefinito | Limiti | Note |
|---|---|---|---|
| `station.reachBlocks` (decimale) | 2,0 | da 0,5 a 8,0 | GIOCO "entro 2 blocchi"; in config per scelta del costruttore (DEC-35 vuole regolabili i valori del gioco, e questo lo usa solo il server); nasce nella 4a, che lo usa per trovare il telaio |
| `station.repairMaterialScrap` (testo) | `minecraft:iron_ingot` | forma `spazio:percorso` controllata nel core (la stessa espressione di `tools/gen_data.py`); che l'oggetto esista lo controlla il server all'avvio, con un WARN e il predefinito | domanda 1 |
| `station.repairUnitFraction` (decimale) | 0,25 | sopra 0, al più 1 | domanda 1 |
| `station.paintDyesPerRegion` (intero) | 1 | da 0 a 64 | domanda 3 |
| `station.presetDyesPerRegion` (intero) | 1 | da 0 a 64 | domanda 4 |

La sezione nasce nella parte 4a con `reachBlocks`; la 4b aggiunge le altre quattro chiavi. Fuori dai limiti vale il predefinito con una riga fra i problemi, come per gli altri campi. Esistono già e non cambiano `damage.repairKitFraction` (0,25) e `server.ownerOnlyFrames` (vero). Nessun valore nuovo per il client: i costi arrivano nella vista (§5.5).

**Un file scritto dalla parte 3c, senza la sezione `station`** (D5): Gson tiene la sezione che fa il costruttore senza argomenti, quindi i cinque valori prendono i loro predefiniti **in silenzio, senza nessuna riga nel log**, come oggi ogni campo assente (DEC-32; Javadoc di `ConfigFile`: "Un campo assente tiene il valore del costruttore senza argomenti"); solo `"station": null` scritto apposta dà il problema "station: section missing, using defaults". Il file non si riscrive mai: per cambiare un valore si aggiunge la sezione a mano. Un caso JUnit con il file com'è oggi (i predefiniti della 3c) controlla: nessun problema, la sezione ai predefiniti. Il caso nasce nella 4a e la 4b lo estende alle chiavi nuove (un file scritto dalla 4a, con la sola `reachBlocks`, prende le altre quattro in silenzio allo stesso modo); la checklist della 4a ha un passo che dice la stessa cosa (nel log del server nessuna riga su `station`) e quella della 4b lo ripete, e il `README.md` lo scrive in una riga per chi fa modpack.

### 5.16 Testi

In `en_us.json` e `it_it.json`, stesse chiavi nello stesso ordine (`LangFilesTest`). I testi qui sotto sono la traccia; le parole esatte restano al costruttore (§7). I nomi delle tinte e del materiale sono quelli degli oggetti del gioco base.

| Chiave | Italiano | Inglese |
|---|---|---|
| `block.redstone_exoframes.station`, `container.redstone_exoframes.station` | Stazione di manutenzione | Maintenance Station |
| `item.redstone_exoframes.repair_kit` | Kit di riparazione | Repair Kit |
| `gui.redstone_exoframes.station.all`, `.all_pieces`, `.cell` | Tutti; Tutti i pezzi; Cella | All; All pieces; Cell |
| `gui.redstone_exoframes.station.repair`, `.repair_all`, `.repair_tip` | Ripara; Tutto: %s; +%s punti, %s | Repair; All: %s; +%s points, %s |
| `gui.redstone_exoframes.station.primary`, `.secondary`, `.tier_colour`, `.no_secondary` | Primario; Secondario; Colore del livello; Le gambe non hanno ancora un colore secondario | Primary; Secondary; Tier colour; Legs have no secondary colour yet |
| `gui.redstone_exoframes.station.unmount`, `.holds_armor`, `.rotate` | Smonta (va nell'inventario); Custodisce un'armatura: si ritira a mano, accovacciati; Trascina per girare | Unmount (goes to your inventory); Holds armour: take it by hand, crouching; Drag to rotate |
| `gui.redstone_exoframes.station.presets`, `.preset_apply`, `.preset_save`, `.preset_empty`, `.presets_read_only` | Preset; Applica il preset %s; Salva i colori del telaio nel preset %s; vuoto; I preset sono in sola lettura su questo server | Presets; Apply preset %s; Save the frame's colours to preset %s; empty; Presets are read-only on this server |
| `gui.redstone_exoframes.station.cost`, `.have`, `.free`, `.missing` | Costo: %s %s; Ne hai %s; Gratis in creativa; Ti manca: %s %s | Cost: %s %s; You have %s; Free in creative; You need: %s %s |
| `gui.redstone_exoframes.station.locked`, `.unlocked`, `.owner_only`, `.everyone`, `.reinforced` | Bloccato: solo il proprietario; Sbloccato: chiunque può usarlo; Solo il proprietario può cambiarlo; Su questo server tutti possono usare ogni telaio; Rinforzato | Locked: owner only; Unlocked: anyone can use it; Only the owner can change this; On this server everyone can use every exoframe; Reinforced |
| `gui.redstone_exoframes.condition.intact`, `.damaged`, `.critical`, `.broken` | Integro; Danneggiato; Critico; Rotto | Intact; Damaged; Critical; Broken |
| `message.redstone_exoframes.station.no_frame`, `.frame_gone`, `.failed` | Nessun telaio vicino: piazzalo sulla stazione o entro 2 blocchi; Il telaio non è più disponibile alla stazione; La stazione non è riuscita a cambiare il telaio (vedi il log del server) | No exoframe nearby: place one on the station or within 2 blocks; The exoframe is no longer available at the station; The station could not change the exoframe (see the server log) |
| `message.redstone_exoframes.station.not_needed`, `.missing`, `.not_a_dye` | Questo pezzo non ha bisogno di riparazioni; Ti manca: %s %s; Il preset contiene un colore che non viene da una tinta | This piece needs no repair; You need: %s %s; The preset has a colour no dye makes |
| `message.redstone_exoframes.repair_kit.done`, `.nothing`, `.where` | %s: +%s; Nessun pezzo da riparare; Usa il kit dentro la tuta o su un telaio piazzato | %s: +%s; Nothing to repair; Use the kit inside the exoframe or on a placed one |
| `message.redstone_exoframes.frame_item_full` | Questo telaio porta pezzi o una cella: si posa solo vuoto | This exoframe carries pieces or a cell: only an empty one can be placed |
| `message.redstone_exoframes.hud_hidden`, `.hud_shown`, `.vignette_on`, `.vignette_off` | HUD della tuta nascosto: %s per mostrarlo; HUD della tuta visibile; Vignetta del casco accesa; Vignetta del casco spenta | Exoframe HUD hidden: %s to show it; Exoframe HUD shown; Helmet vignette on; Helmet vignette off |
| `key.redstone_exoframes.hide_hud`, `key.redstone_exoframes.vignette` | Nascondi l'HUD della tuta; Vignetta del casco | Hide exoframe HUD; Helmet vignette |
| `hud.redstone_exoframes.percent` (nuova), `hud.redstone_exoframes.no_cell` (testo nuovo) | %s %%; Senza cella | %s%%; No cell |
| `tooltip.redstone_exoframes.station.what`, `.where`, `.open` | Ripara, vernicia e monta i pezzi di un telaio piazzato; Il telaio va sopra la stazione o entro 2 blocchi; Clic destro per aprirla | Repairs, paints and fits the pieces of a placed exoframe; The exoframe goes on the station or within 2 blocks; Right-click to open |
| `tooltip.redstone_exoframes.repair_kit.what`, `.where` | Ripara una parte del pezzo messo peggio; Clic destro dentro la tuta o su un telaio piazzato | Repairs part of the most damaged piece; Right-click inside the exoframe or on a placed one |
| `tooltip.redstone_exoframes.exoframe.unlocked`, `.reinforced` | Sbloccato; Rinforzato | Unlocked; Reinforced |
| `commands.redstone_exoframes.debug.reinforced.done` | Rinforzato: %s | Reinforced: %s |
| `message.redstone_exoframes.keys_hint` (testo nuovo) | Tieni premuto %s per uscire dal telaio, %s per cambiare cella; %s nasconde l'HUD | Hold %s to leave the exoframe, %s to swap cells; %s hides the HUD |

Se ne vanno `hud.redstone_exoframes.energy` e `hud.redstone_exoframes.worst`.

### 5.17 Suoni

Tutti da `frame/FrameSounds`, dal server, volume 1 e tono 1, con il registratore dei test (DEC-40); voci nuove dell'enum `Sound`, tutte costanti che sono un `SoundEvent` semplice su 1.20.1 e 1.21.1 [javap: lo sono `SMITHING_TABLE_USE`, `ANVIL_USE`, `DYE_USE`, `BOOK_PAGE_TURN`, `IRON_DOOR_OPEN`, `IRON_DOOR_CLOSE`; `UI_BUTTON_CLICK` è un `Holder` su tutte e due e non si usa]. Tabella scelta dal costruttore:

| Azione | Suono | Fonte | Dove |
|---|---|---|---|
| riparare alla stazione | `SMITHING_TABLE_USE` | `BLOCKS` | punto del telaio |
| usare il kit | `ANVIL_USE` | `PLAYERS` | il giocatore (tuta) o il telaio piazzato |
| verniciare, applicare un preset | `DYE_USE` | `PLAYERS` | punto del telaio |
| salvare un preset | `BOOK_PAGE_TURN` | `PLAYERS` | punto del telaio |
| bloccare / sbloccare | `IRON_DOOR_CLOSE` / `IRON_DOOR_OPEN` | `BLOCKS` | punto del telaio |
| montare / smontare dalla stazione | `MOUNT` / `UNMOUNT` di oggi (`IRON_TRAPDOOR_CLOSE` / `OPEN`) | `PLAYERS` | punto del telaio |

Aprire la stazione, H e il tasto della vignetta non suonano; posare e rompere la stazione suonano come un blocco di metallo del gioco base.

### 5.18 Casi che non devono rompere nulla

La stazione non aggiunge eccezioni a "nulla si perde e nulla si duplica": nel blocco e nel menu non vive nessun oggetto. Durante la schermata l'unica cosa fuori dall'inventario è lo stack sul cursore, del gioco base come in ogni contenitore: alla chiusura torna nell'inventario, oppure cade nel punto del giocatore se è morto o ha lasciato il server; il gioco salva il giocatore prima di toglierlo e chiudere il menu, ed è per questo che `removed` fa cadere invece di restituire [javap: `PlayerList.remove` chiama `save` prima di `removePlayerImmediately`, che arriva a `Player.remove` → `doCloseContainer` → `removed`; `removed` lascia cadere lo stack se il giocatore è morto o `hasDisconnected`]. La stazione non mette mai nulla sul cursore; `StationMenu.removed` chiama quello del gioco base e non aggiunge restituzioni. Le eccezioni dichiarate restano le due di M3 (creativa con l'inventario del tutto pieno, caricatore); in creativa, come a mano, montare non consuma il pezzo sul cursore (convenzione della 3b).

| Caso | Che cosa succede | Prova |
|---|---|---|
| due giocatori alla stessa stazione | ciascuno ha il suo menu, nessuno stato in comune; ognuno lavora sul telaio usabile più vicino per lui | test in mondo |
| due giocatori sullo stesso telaio (due stazioni, o stazione e mani) | le azioni si eseguono una alla volta sul thread del server, ognuna rilegge il registro; la vista dell'altro si aggiorna entro un tick (§5.5); un'azione su uno slot svuotato nel frattempo non fa nulla | test in mondo |
| il telaio esce di portata o lo usa un altro mentre la schermata è aperta (raccolto, indossato, tolto con un comando, bloccato dal proprietario) | `stillValid` falso: la schermata si chiude al tick dopo, con "Il telaio non è più disponibile alla stazione"; i pulsanti arrivati dopo il gioco base li scarta | test in mondo |
| il giocatore muore con la schermata aperta | `stillValid` falso per un giocatore morto: si chiude al tick dopo; lo stack sul cursore cade nel punto della morte come per ogni contenitore; i pulsanti da morto non fanno nulla (§5.6) | test in mondo se il giocatore finto lo permette, altrimenti checklist |
| cambio di dimensione con la schermata aperta | da un portale il client chiude da sé le schermate dei contenitori; con un teletrasporto `stillValid` vede l'altro livello e chiude; il cursore torna nell'inventario | test in mondo della regola, client vero o checklist per il teletrasporto |
| il giocatore lascia il server con la schermata aperta | il gioco salva il giocatore, poi chiude il menu: il cursore cade nel punto dove era, come per ogni contenitore | checklist |
| inventario pieno | ciò che si smonta e la cella scambiata vanno con `Handouts.give`: ciò che non entra cade fermo ai piedi, con il ritardo di raccolta (M3 §7 punto 26); pagare non dà nulla indietro | test in mondo |
| un chunk si scarica | la stazione va usata da vicino, quindi il suo chunk è caricato; il registro non dipende dai chunk; se l'entità del telaio non è caricata il riassunto si rifà quando torna (`alignToRecord` del primo tick) | test in mondo del riassunto |
| creativa | nulla da pagare né consumare (`free`); un operatore in creativa usa anche i telai bloccati altrui (`canUse`); bloccare resta del proprietario o dell'operatore di livello 2 | test in mondo |
| spettatore | il clic sulla stazione non fa nulla; i pulsanti li scarta il gioco base; passando a spettatore la schermata si chiude | test in mondo |
| registro in sola lettura | ogni `updatePlaced` dà falso: nulla si prende e nulla si dà, "La stazione non è riuscita…", l'ERROR del registro una volta | test in mondo (`GameTests` ha già gli aiuti per il registro) |
| preset in sola lettura (`schema` più nuovo, o file illeggibile, §5.14) | "applica" funziona, "salva" spento, il file non si riscrive | test in mondo |
| la stazione si rompe, esplode o un pistone la spinge con la schermata aperta | rotta o esplosa: `stillValid` falso, si chiude; i pistoni non la spostano | test in mondo |
| pacchetti in ritardo | un pulsante dopo la chiusura: il numero del menu non torna e il gioco base lo scarta; una vista dopo la chiusura: il client la ignora | test in mondo per il primo |
| clic a raffica | al più una scrittura per tick per menu, la scelta è libera | test in mondo |
| il kit su un telaio piazzato da due giocatori nello stesso tick | due azioni in fila, ciascuna sul pezzo messo peggio in quel momento; due kit, due riparazioni | test in mondo |
| un pezzo di un livello senza oggetto (M5) | non si smonta, l'avviso di oggi; si ripara solo se il suo livello ha un materiale in config | JUnit e test in mondo |

### 5.19 Prove

- **JUnit** (Java puro): `StationReachTest` (distanza dall'impronta, finestra verticale, scelta fra più telai, pareggi), `StationActionTest` (ogni numero valido, i buchi della tabella, negativi, nessun numero oltre 127), `RepairRuleTest` (punti di un'unità per 180, 360, 520, 650, 800 di massimo, arrotondamento, tetto, unità che servono e che si pagano, pezzo peggiore del kit con pareggi e pezzo rotto), `DyePaletteTest` (i sedici valori fissati dal bytecode, nome e indice), `PaintRegionsTest` (con il legame alla geometria: le ossa `_p2` delle gambe senza cubi), `ColorPresetTest` e `PresetPlanTest` (cambi, costi, gambe, colore che non è una tinta, colore del livello gratis, tutto o niente, prima tinta mancante), `StationViewTest` e un andata e ritorno di `StationViewCodec` su un `FriendlyByteBuf` (come `ClientRulesPacketTest`), `HudLevelTest`, `HudModelTest` (al posto di `EnergyLineTest`), `HudLayoutTest` (i rettangoli di §12 a 427x240, 480x270, 640x360, 683x384, 640x270, 341x256 e 320x240 per le due mani, contro il braccio scalato, la fascia della barra, lo slot della seconda mano, le righe della chat e il mirino; la soglia di 424), `ClientPrefsFileTest` (assente, rovinato, chiavi sconosciute tenute, chiavi mancanti, scrittura sostitutiva), `ExoframesConfigTest` e `ConfigFileTest` (limiti della sezione `station`; il file della 3c senza problemi). Ogni tipo nuovo di `core/` citato in un test (regola 1c).
- **Test in mondo** (GameTest, quattro varianti; i conti esatti li fissano i piani, oggi 208 per variante): circa 24 nella parte 4a (posa delle due metà e i rifiuti, rottura di ogni metà con e senza piccone e in creativa con il conto degli oggetti, esplosione, pistoni; apertura sopra la stazione, a 2 blocchi e oltre, telaio bloccato, il più vicino; `stillValid` dopo raccolta, ingresso, blocco, rimozione; montare dal cursore e i rifiuti, smontare con l'inventario pieno, la cella scambiata; il lucchetto per proprietario, altro giocatore e operatore; una scrittura per tick; raccolta e posa con proprietario e bandierine; oggetto con pezzi rifiutato; registro in sola lettura senza nulla preso) e circa 24 nella 4b (riparazione di un'unità e fino in fondo con il conto del ferro, pezzo al massimo, creativa, tutti i pezzi; il kit nella tuta e sul telaio piazzato, nulla da riparare, creativa; verniciatura per regione con la tinta presa e il riassunto dell'entità rifatto, stesso colore gratis, colore del livello, gambe, tutti i pezzi, tinta mancante; preset salvati e applicati con i costi, colore che non è una tinta, tutto o niente, sola lettura, salvataggio e rilettura del dato dei preset, due giocatori). La 4c non ha test in mondo (nessun test in mondo vede un disegno). Aiuti nuovi dei test: posare una stazione, aprirla con il clic vero, premere un pulsante come fa il gioco base (`stillValid`, `clickMenuButton`, `broadcastChanges`), mettere uno stack sul cursore, salvare e rileggere i preset. Se su un loader l'apertura vera con il giocatore finto non passa (l'apertura di Architectury passa dalla rete del loader), i test costruiscono il menu del server con lo stesso costruttore e l'apertura la prova il client vero.
- **Client vero nel cloud.** `tools/client/client_shot.sh` impara a cliccare nelle schermate (oggi non sa: la ricognizione del codice §10): passi nuovi `@punta <x> <y>`, `@clic <x> <y> [sinistro|destro] [maiuscole]`, `@trascina <x1> <y1> <x2> <y2>`, con xdotool, che lo script usa già; le posizioni dei widget a 960x540 e scala 2 sono quelle di §5.3 per due (`leftPos` 82, `topPos` 19). Sequenze nuove: `m4a_shots.sh` (apertura con `@click destro`, l'anteprima disegnata nel suo riquadro sulle quattro varianti, il trascinamento che cambia solo il riquadro dell'anteprima, un pezzo montato e smontato con i clic, il lucchetto), `m4b_shots.sh` (riparazione con la barra della durata che cresce, verniciatura con un colore che nient'altro ha sullo schermo e il telaio nel mondo che lo mostra dopo la chiusura, un preset, il kit nella tuta), `m4c_shots.sh` (i quattro stati dell'HUD con `/exoframe debug wear rottame <stato>` e `/exoframe debug energy <RE>`, comandi che ci sono già, in `command/ExoframeCommand`: lo stato vale per tutti e sei i pezzi insieme, quindi una sagoma mista come quella di `hud-destra-3-pezzo-rotto.png` resta nelle anteprime; con la mano destra e con la sinistra, `EXO_SHOT_MAIN_HAND`; controlli sugli scatti: i pixel dell'HUD, presi come differenza con lo stesso scatto dopo H, stanno tutti nel rettangolo atteso e nessuno nel riquadro del braccio, nella fascia della barra, nel mirino o nelle righe della chat; la vignetta c'è in prima persona e sparisce con il suo tasto, assegnato nelle opzioni che lo script scrive). Più le sequenze della 3c e della 3d aggiornate (§5.12). Gli scatti di riferimento in `docs/test-in-gioco/m4a-screenshot/`, `m4b-screenshot/`, `m4c-screenshot/`.
- **Server dedicato** avviato e fermato sulle quattro varianti a ogni parte (DEC-46).
- **Checklist per il PC**: `docs/test-in-gioco/m4a-stazione.md`, `m4b-riparare-verniciare.md`, `m4c-hud.md`, `m4d-grafica.md`, nella forma delle precedenti (intestazione, che cosa c'è da vedere, che cosa non c'è ancora, già verificato nel cloud, passi numerati, che cosa riportare, domande per il proprietario), scritte per essere seguite alla lettera dall'assistente sul PC, che non può fare domande; con i passi che il cloud non prova: suoni, suggerimenti al passaggio del mouse, un secondo giocatore, l'uscita dal server con la schermata aperta, il jar di produzione, la vignetta sopra e sotto gli elementi del gioco base sulle due versioni, la config della 3c (§5.15).

### 5.20 Classi e cuciture nuove

- **Comuni**: `content/StationBlock`, `content/StationItem`, `content/RepairKitItem`, `content/ModMenus`; `station/StationMenu`, `station/StationFinder` (la regola di §5.2 sul registro), `station/StationActions` (i gestori dei pulsanti), `station/StationViewCodec`, `station/StationNet`; un aiuto di `frame/` per montare e smontare in comune con le mani; `FramePlacement.refreshView` pubblico; in `frame/FrameInteractions` la voce del kit; in `content/ExoframeItem` la lettura dei dati dell'oggetto; voci nuove in `frame/FrameSounds`; `/exoframe debug reinforced` in `command/ExoframeCommand`; H nella riga di `frame/EntryHints`.
- **Core**: `core/station/` (`StationReach`, `StationAction`, `RepairRule`, `DyePalette`, `PaintRegions`, `ColorPreset`, `PresetPlan`, `StationView`), `core/frame/HudLevel`, la sezione `Station` di `core/config/ExoframesConfig`.
- **Client**: `client/StationScreen`, `client/SuitPreview` (la trasformazione del telaio in comune con `ExoframeEntityRenderer`), `client/SuitHud`, `client/HudModel`, `client/HudLayout`, `client/HudKeys` (H e vignetta), `client/ClientPrefs`; `configfile/ClientPrefsFile` (Java e Gson, come `ConfigFile`).
- **Cuciture** (in `docs/compat.md` nello stesso task):
  - `compat/CompatBlock` davanti a `compat/impl/BlockBase`: il clic neutro come `CompatEntityBlock` e un metodo neutro per `playerWillDestroy` (`void` su 1.20.1, `BlockState` su 1.21.1); `updateShape` si dichiara pubblico nel codice comune come già `rotate` e `mirror`. Se `EntityBlockBase` debba riusare la traduzione del clic lo decide il costruttore.
  - `compat/client/CompatContainerScreen` davanti a `compat/impl/client/ContainerScreenBase` (la "base client" rinviata da COMPAT): su 1.20.1 la sottoclasse chiama `renderBackground(GuiGraphics)`, su 1.21.1 lo fa già `Screen.render` e chiamarlo di nuovo scurirebbe due volte; un metodo neutro per il contenuto.
  - `compat/PresetAccess` con `compat/impl/PresetAccessImpl` e `PresetSavedData`.
  - Niente cuciture per menu e dati extra (`MenuRegistry` ed `ExtendedMenuProvider` di Architectury sono identici sulle due versioni, sempre con `FriendlyByteBuf`), pulsanti, tasti, gancio dell'HUD, tinte, `Item.use`, `GuiGraphics` (uguali [javap, ricognizione della piattaforma]).
  - **Registrazione della schermata.** `client/RedstoneExoframesClient.init()` chiama `ModMenus.STATION.listen(tipo -> MenuRegistry.registerScreenFactory(tipo, StationScreen::new))`, senza direttive. Perché così: `registerScreenFactory` vuole l'istanza del tipo e su NeoForge la cattura subito in un ascoltatore di `RegisterMenuScreensEvent` [sorgente di Architectury 13.0.11]; durante la costruzione della mod il tipo non c'è ancora e `RegistrySupplier.get()` lancerebbe [sorgente: "Value missing"]; `listen` richiama quando il tipo è registrato (subito su Fabric, all'evento dei registri su Forge e NeoForge) [sorgente; javap: `default void listen(Consumer<T>)` c'è in 9.2.14 e in 13.0.11]; su NeoForge gli eventi dei registri girano dentro `ClientModLoader.begin` (`CommonModLoader.begin` → `postRegisterEvents`), prima che `ClientHooks.initClientHooks` mandi `RegisterMenuScreensEvent` [sorgente di NeoForge 21.1.266 e ricognizione della piattaforma §1], quindi l'ascoltatore arriva in tempo; da `CLIENT_SETUP` sarebbe troppo tardi, in silenzio. È una deduzione: la prova nel client vero è il primo task della parte 4a, e passa solo se la schermata si apre davvero su NeoForge. Ripiego: un ascoltatore di `RegisterMenuScreensEvent` nell'entrypoint di NeoForge, che dentro l'evento chiama `event.register(ModMenus.STATION.get(), …)`.
  - **Nessun mixin nuovo** (scelta del costruttore): gli elenchi esatti di `tools/check_jars.sh` per i mixin non cambiano; cambia solo il controllo (f3) dei dati, che nomina la stazione e il kit.

## 6. Le parti

Cinque parti (D7), ognuna con piano, revisione e unione a `main` proprie, ognuna di circa dieci task, ognuna lascia un gioco giocabile. Una parte si unisce quando build, controlli, JUnit, test in mondo, sequenza del client vero e server dedicato sono verdi nel cloud sulle quattro varianti; le prove sul PC procedono in parallelo e ciò che trovano diventa il primo task della parte dopo (DEC-48). Ordine: 4a, 4b, 4c, 4d; la 4e è l'ultima e aspetta il suo spike.

| Parte | Contenuto | Fatta quando | Il proprietario può provare |
|---|---|---|---|
| **4a — Il banco** | Prova delle due cuciture; blocco 2x1; sezione `station` della config con `reachBlocks` e il caso del file della 3c; ricerca del telaio e `stillValid`; vista e `station_view`; schermata con segnaposto, pannello del telaio e anteprima girevole; pulsanti con la tabella di §5.6; montare e smontare pezzi e cella; lucchetto e rinforzato; raccolta e posa con i dati; passi nuovi di `client_shot.sh` e `m4a_shots.sh`; testi, suoni, `docs/compat.md`, checklist | Sulle quattro varianti la stazione si fabbrica, si posa e si rompe senza doppioni; si apre su un telaio piazzato sopra o entro 2 blocchi e ne mostra l'anteprima 3D, che si gira trascinando; dalla schermata si montano e smontano pezzi e cella e si blocca e sblocca il telaio; un telaio raccolto e ripiazzato conserva sblocco e rinforzo, e il proprietario quando a raccoglierlo è stato un altro giocatore (§5.11); tutto verde | fabbricare e posare la stazione, metterci accanto o sopra un telaio, aprirla, girare la tuta, montare e smontare pezzi e cella, bloccare il telaio e vedere che un altro giocatore non ci arriva |
| **4b — Riparare e verniciare** | Le altre quattro chiavi della sezione `station`; riparazione; kit da campo con la ricetta; tavolozza e verniciatura; preset con il loro dato salvato; la schermata completa (riga dei costi, anteprima dei colori); `m4b_shots.sh`; testi, suoni, checklist | Un pezzo rotto torna integro alla stazione con il ferro e cresce di un quarto con il kit, dentro la tuta e su un telaio piazzato; i pezzi si dipingono per regione con le tinte, l'anteprima mostra il colore prima di pagare e il mondo vede il telaio ridipinto; tre preset per giocatore si salvano, si applicano pagando e restano dopo un riavvio; un file di config della 3c funziona senza righe nel log; tutto verde | rompere un pezzo e ripararlo, fabbricare un kit e usarlo dentro la tuta, dipingere la tuta, salvare, cambiare e riapplicare un preset, riavviare il mondo |
| **4c — L'HUD** | `HudLevel`, `HudModel`, `HudLayout`; file delle preferenze; `SuitHud` al posto della riga; H e tasto della vignetta; vignetta; tutto ciò che la riga si porta dietro (§5.12); `m4c_shots.sh`; checklist | Chi indossa la tuta vede barra, celle di scorta e avviso, e con un elmo funzionante anche la sagoma colorata per stato e la vignetta; senza elmo o con l'elmo rotto solo la barra; con la mano destra e con la sinistra l'HUD sta nell'angolo opposto al braccio e i controlli sugli scatti lo trovano fuori da braccio, barra degli oggetti, barre di stato, mirino e righe della chat; H e la vignetta si ricordano dopo un riavvio del client; le sequenze della 3c e della 3d passano con la disposizione nuova; tutto verde | indossare la tuta e guardare l'HUD, consumare energia fino all'avviso, rompere l'elmo con `/exoframe debug wear rottame 3`, premere H, assegnare e premere il tasto della vignetta, mettere la mano principale a sinistra |
| **4d — La grafica** (D8) | Generatori del modello e delle texture della stazione, delle icone di stazione e kit, della texture della GUI (512x256 con lo sfondo di 316x232 e i disegni dei widget), dei disegni dell'HUD e degli angoli della vignetta, con i loro controlli in `tools/check_art.sh`; anteprime dagli script al proprietario (due o tre direzioni per il blocco, come per la tuta in M3); integrazione dopo la sua risposta; lo strato luminoso della tuta (RP-14, `AutoGlowingGeoLayer` con la maschera che c'è già) con la sua prova nel client vero (scelta del costruttore: M3 lo prometteva "in M4", ed è arte già generata); checklist | La grafica scelta dal proprietario è generata da script, controllata da `check_art.sh` e integrata, e non resta nessun segnaposto; lo strato luminoso c'è, oppure è rinviato a M5 con il motivo (se rompe la regola della visibilità delle ossa su una variante); tutto verde | vedere stazione, schermata e HUD con la grafica scelta, e la tuta con le luci di redstone accese |
| **4e — Animazioni** (D7) | Prima uno spike da buttare: un controller di GeckoLib sull'oggetto modello della tuta (`SuitModelItem`, l'unico `GeoItem`) con `getInstanceId` ridefinito (ANI-02), guidato per telaio, che deve provare sulle quattro varianti che l'aggancio della visibilità delle ossa resta (GeckoLib lo chiama solo per un'entità non animabile, e l'entità passata resta tale), che due telai si animano ciascuno per sé, che braccio in prima persona e anteprima della stazione non cambiano. Se lo prova: animazione di apertura all'ingresso e posa di ingresso (ANI-01), con l'ingresso che resta di un tick sul server (L-10: l'animazione si mostra dopo), e se ci stanno C-09 e ANI-06 | Fuori dal "fatta quando" della milestone finché lo spike non le prova (D7); se lo spike non riesce, M5 | — |

**Il primo task della 4a** (D7): un blocco stazione minimo (un blocco solo, segnaposto) il cui clic apre, con i dati extra, un menu la cui schermata è registrata come in §5.20 e disegna con `SuitDraw` una tuta fissa nel suo riquadro, con il ritaglio. Nella schermata c'è anche un pulsante segnaposto che il client senza schermo preme con `@clic` (il passo nuovo di §5.19, che nasce qui): il clic va al server con `clickMenuButton` e il numero 102, il più alto della tabella di §5.6, e torna come un cambio che si vede nella schermata (decisione di chi coordina: ogni task dopo si prova cliccando nella schermata, e né il clic di xdotool dentro una schermata sotto Xvfb né il numero nel byte di 1.20.1 né il ritorno dal server sono mai stati visti girare). Sulle quattro varianti nel client vero: passa se la schermata si apre davvero su ognuna (su NeoForge un errore sarebbe silenzioso), se nel riquadro dell'anteprima ci sono i pixel della tuta, se il pulsante premuto cambia la schermata e se il log è pulito. Se non passa su una variante, il lavoro si ferma e la decisione torna a chi coordina (ripieghi: §5.4 e §5.20).

**La 4a in due pezzi** (decisione di chi coordina, come la 3c): la **4a-1** sono i task 1-5 qui sotto (la prova delle cuciture con il pulsante; il blocco 2x1; portata, ricerca del telaio e `stillValid`; la vista; la schermata con il pannello del telaio, in sola lettura, e l'anteprima girevole), con i passi nuovi di `client_shot.sh` che servono, una sequenza `m4a1_shots.sh`, il server dedicato e una checklist ridotta; si unisce a `main` da sola. La **4a-2** sono i task 6-10 (azioni, montare e smontare, lucchetto e rinforzato, raccolta e posa, il resto degli strumenti, testi e checklist della 4a). La 4a-2 è il codice che cambierebbe con le risposte del proprietario sugli slot della schermata (§8 punto 1) e sulla domanda 11, quindi si pianifica dopo quelle risposte (arrivate il 27 novembre 2026: nessuno slot, e la domanda 11 con il valore di §5.11).

**Ordine indicativo dei task** (i piani lo fissano):

- 4a: (1) la prova delle due cuciture; (2) il blocco 2x1 con la cucitura nuova, bottino, ricetta e segnaposto; (3) `StationReach`, la sezione `station` con `reachBlocks`, ricerca del telaio e `stillValid`; (4) `StationView`, codec, dati extra e `station_view`; (5) `CompatContainerScreen` e la schermata con pannello del telaio e anteprima; (6) `StationAction`, ordine dei controlli, scelta, montare e smontare; (7) lucchetto, rinforzato, `/exoframe debug reinforced`; (8) raccolta e posa con i dati; (9) passi nuovi di `client_shot.sh`, `m4a_shots.sh`, server dedicato; (10) testi, suoni, `docs/compat.md`, checklist.
- 4b: (1) le altre chiavi della sezione `station`; (2) `RepairRule` e riparazione; (3) il kit; (4) `DyePalette`, `PaintRegions` e verniciatura; (5) la schermata: costi, tavolozza, anteprima dei colori; (6) preset: regole e dato salvato; (7) preset nella schermata; (8) `m4b_shots.sh`, server dedicato; (9) testi, suoni, `README.md`, checklist.
- 4c: (1) `HudLevel`, `HudModel`, `HudLayout`; (2) il file delle preferenze; (3) `SuitHud`; (4) i tasti; (5) la vignetta; (6) ciò che la riga si porta dietro; (7) `m4c_shots.sh` e le sequenze della 3c e della 3d sulle quattro varianti; (8) checklist.
- 4d: (1) generatori e anteprime per il proprietario; (2) integrazione dopo la risposta; (3) strato luminoso; (4) scatti e checklist.

**Il "fatta quando" della milestone 4**: 4a, 4b, 4c e 4d fatte; così GEN §8 ("Blocco stazione con GUI, riparazione, verniciatura, HUD, tasti", con il solo tasto H e quello della vignetta: G e N sono di M5, D1) è coperto sulle quattro varianti. La 4d aspetta la risposta del proprietario alle anteprime: gli script delle anteprime si possono scrivere già durante la 4a, così la risposta arriva mentre si costruiscono 4b e 4c.

## 7. Che cosa resta al costruttore

I testi esatti dei messaggi, dei suggerimenti e dei pulsanti (la traccia è in §5.16); i nomi interni delle classi e degli aiuti dei test che qui non sono fissati; le sfumature dei pannelli e dei segnaposto; la sensibilità esatta del trascinamento e la scala dell'anteprima, entro quanto scritto; come si confronta la vista a ogni tick; se `EntityBlockBase` riusa la traduzione del clic della base nuova; la forma interna del codec della vista e del dato dei preset entro le chiavi date; le soglie dei controlli sugli scatti (misurate sulla prima variante e scritte nello script con il valore misurato accanto); l'ordine dei task dentro una parte, salvo il primo della 4a e lo spike della 4e.

## 8. Scelte del costruttore da far vedere al proprietario

1. Nessuno slot nella stazione, nemmeno transitorio: materiali e tinte si pagano dall'inventario, i pezzi si montano lasciandoli dal cursore sul loro posto; la stazione non ha block entity (§4.2, dentro D3). Confermata dal proprietario il 27 novembre 2026.
2. Tutte le azioni passano dai pulsanti del menu del gioco base, con una tabella di numeri da 0 a 102 e la scelta del pezzo sul server; un pacchetto nuovo solo dal server al client, nessuno dal client al server (§4.1, §5.6, §5.13).
3. La stazione lavora solo sui telai piazzati, sul più vicino a portata che il giocatore può usare; chi indossa la tuta esce accanto alla stazione (§5.2).
4. Due metà affiancate davanti a chi posa, la laterale alla sua destra; cubi pieni, così un telaio può stare anche sopra; i pistoni non la spostano; senza piccone non cade (§5.1).
5. Una sola stazione cade, da qualunque metà la si rompa con il piccone; in creativa o senza piccone nessuna (§5.1).
6. Schermata di 316x232, che ci sta anche alla grandezza minima dell'interfaccia; pannello del telaio visto di fronte come l'anteprima (§5.3).
7. Nessuno scambio di pezzi dalla stazione: per cambiare un pezzo lo si smonta prima (§5.7).
8. Il kit ripara il pezzo messo peggio, all'istante, dentro la tuta o su un telaio piazzato (§5.8).
9. Una tinta è un colore, senza miscele; tornare al colore del livello è gratis; i pezzi sciolti non si dipingono (§5.9).
10. Tre preset numerati, senza nome; applicare un preset è tutto o niente (§5.10).
11. Un telaio raccolto da un altro giocatore resta del suo proprietario quando lo si ripiazza (§5.11; domanda 11).
12. L'HUD in un gruppo solo nell'angolo in basso opposto al braccio, che cambia lato con la mano principale; in alto a sinistra se l'interfaccia è più stretta di 424 pixel (§4.3, §5.12).
13. Una legenda di colori per tutto l'HUD: anche l'energia diventa verde, rossa, grigia (§5.12; domanda 9).
14. La vignetta solo negli angoli, in prima persona, con un tasto senza predefinito (§5.12; domanda 10).
15. Le celle di scorta sono le celle cariche dell'inventario (§5.12).
16. Il pezzo messo peggio non ha più una riga sua: lo mostra la sagoma; senza elmo nessuna informazione sui pezzi, come vuole GIOCO (§5.12).
17. Con la mappa a due mani l'HUD resta sopra il braccio dell'altra mano (§5.12).
18. I preset in un dato salvato loro, scritti con il salvataggio del gioco; un crash può perdere gli ultimi preset, mai oggetti (§5.14).
19. Il file delle preferenze del client lo riscrive il client, tenendo le chiavi che non conosce (§5.14).
20. Nessun mixin nuovo; nessun innalzamento dello schema del registro (§5.20, §5.14).
21. Senza un elmo funzionante si vedono barra, percentuale, celle di scorta e avviso: la "barra" di GIOCO (§5.12, C-04).
22. Dalla stazione si monta e si scambia anche la cella, e il pulsante "Tutti" ripara e dipinge la tuta intera (§5.7, §5.3).
23. Il kit funziona anche su un telaio piazzato (§5.8).
24. Lo strato luminoso della tuta entra nella parte 4d (§6, RP-14).
25. `/exoframe debug reinforced` per provare il rinforzato; `station.reachBlocks` in config; un oggetto telaio con pezzi nei dati non si posa (§5.11, §5.15).

Scelte del controllo (le ha fatte chi coordina, dopo la lettura del costruttore):

26. La stazione si rompe come il letto: solo dalla metà laterale si toglie prima la principale, senza oggetti; così né la creativa né la mano nuda fanno cadere una stazione (§5.1).
27. La scelta del pezzo resta sul server, cambia solo con i pulsanti e viaggia nella vista; uno slot svuotato resta scelto e non costa nulla (§5.6).
28. Le gambe non hanno il secondario finché le loro ossa `_p2` non hanno cubi; le ossa vuote restano (§5.9).
29. Un file dei preset illeggibile mette i preset in sola lettura invece di cancellarli (§5.14).
30. "Ripara" con tutti i pezzi scelti e poco ferro ripara quelli che può, nell'ordine degli slot (§5.8).
31. La vista dice al client se `server.ownerOnlyFrames` è acceso (§5.5).
32. Le anteprime e il loro script entrano nel repository, in `anteprime/2026-11-19-m4/` (in testa).

## 9. Domande per il proprietario

Ognuna con il valore che il lavoro segue finché non risponde.

1. **Riparazione del Rottame** (D6): materiale e resa di un'unità. Intanto un lingotto di ferro ripara il 25 % della durata massima (45 punti su 180), come un'unità all'incudine del gioco base (`station.repairMaterialScrap`, `station.repairUnitFraction`).
2. **Ricetta del kit da campo** (D6). Intanto `_I_` / `CrC` / `_I_` (due ferro, due rame, una redstone), un kit per ricetta, pile da 16; ripara il 25 % del pezzo messo peggio (`damage.repairKitFraction`).
3. **Costo della verniciatura** (D6). Intanto una tinta per regione e per pezzo che cambia; tornare al colore del livello gratis (`station.paintDyesPerRegion`).
4. **Costo di un preset** (D6). Intanto come la verniciatura, una tinta per ogni regione che cambia; salvare gratis (`station.presetDyesPerRegion`).
5. **Effetto di `rinforzato`** (D6, per M5). Intanto nessuno: si conserva e si mostra.
6. **Colore secondario delle gambe** (C-06, Q-04). Intanto le gambe hanno solo il primario e il secondario delle gambe è spento; se lo vuoi, la grafica aggiunge la fascia alla caviglia (la proposta di M3) e la regola si accende con lei.
7. **Ricetta della stazione** (L-13). Intanto `CCC` / `ITI` / `IrI` (tre rame, quattro ferro, un banco da lavoro, una redstone).
8. **Posizione dell'HUD** (C-01, Q-01). Intanto in basso, nell'angolo opposto alla mano principale; l'alternativa è tutto in alto a sinistra. **Risposta del proprietario (27 novembre 2026): in basso, nell'angolo opposto alla mano principale.**
9. **Colori dell'energia** (C-03). Intanto verde, rosso sotto il 10 %, grigio a zero, come la sagoma; prima erano verde, giallo, rosso. **Risposta del proprietario (27 novembre 2026): verde, rosso sotto il 10 %, grigio a zero.**
10. **Vignetta**. Intanto accesa, con un tasto da assegnare per spegnerla; si può dare un tasto predefinito o un interruttore nella stazione.
11. **Di chi è un telaio raccolto e ripiazzato**. Intanto: se l'ha raccolto un altro giocatore (un telaio sbloccato prestato), resta del suo proprietario; se l'ha raccolto il proprietario, chi lo posa ne diventa proprietario, così un telaio si può regalare o scambiare (§5.11). Prima della milestone 4 diventava sempre di chi lo posava. L'alternativa: il proprietario resta sempre nell'oggetto e la posa si rifiuta a chi non potrebbe usare il telaio, con l'oggetto che resta in mano. **Risposta del proprietario (27 novembre 2026): il valore di sopra; un telaio raccolto da un altro giocatore resta del suo proprietario.**

## 10. Rischi

- **La registrazione della schermata su NeoForge** (§5.20): la via scelta viene dai sorgenti, non è mai stata vista girare; se sbaglia, la schermata non si apre e nessuno lo dice. Mitigazione: è il primo task, con la prova nel client vero sulle quattro varianti e un ripiego pronto.
- **La stessa registrazione su Forge 1.20.1**: lì `registerScreenFactory` chiama subito `MenuScreens.register` [sorgente di Architectury 9.2.14], quindi dentro l'evento dei registri, durante il caricamento delle mod, e scrive una mappa del gioco base non sincronizzata; se quel caricamento corresse in parallelo con un'altra mod che fa lo stesso ci sarebbe una corsa [non verificato: oggi le registrazioni client della mod girano allo stesso punto senza problemi osservati]. Mitigazione: il primo task la guarda su Forge; ripiego, la registrazione da `CLIENT_SETUP` con il lavoro accodato sul thread principale, che su Forge arriva in tempo.
- **La tuta disegnata in una schermata** (§5.4): luce, normali con la scala negativa in z, ritaglio su 1.20.1, il segno `young` del modello: mai provato fuori dal mondo e dall'anteprima dell'inventario. Mitigazione: nello stesso primo task, con il ripiego del renderer dell'entità.
- **Il giocatore finto dei test e l'apertura del menu**: l'apertura di Architectury passa dalla rete del loader, che il giocatore finto può non avere; mitigazione in §5.19 (il menu costruito direttamente, l'apertura provata dal client vero).
- **I passi nuovi del client senza schermo** (`@punta`, `@clic`, `@trascina`): strumento nuovo, coordinate fisse a 960x540; mitigazione: un selftest dei passi, e gli scatti misurati solo dopo aver visto che il clic arriva.
- **La vignetta sopra o sotto l'HUD del gioco base secondo il loader** (§5.12): sotto su NeoForge 1.21.1, sopra su Fabric 1.21.1 e su 1.20.1; differenza voluta e leggera; la guardano gli scatti della 4c e la checklist.
- **Il dato dei preset con il salvataggio di NeoForge in un altro thread** (§5.14): mitigazione con i dati immutabili sostituiti interi.
- **Lo spike delle animazioni** (4e): può non riuscire; per questo è fuori dal "fatta quando".
- **L'arte della 4d aspetta il proprietario**: fino ad allora la milestone vive con i segnaposto; gli script delle anteprime partono presto.

## 11. Tracciabilità

Ogni voce delle ricognizioni che tocca M4, con dove la tratta questo progetto.

| Voce | Dove |
|---|---|
| M4-01 … M4-06 (perimetro, quattro varianti, checklist, che cosa non è M4) | §1, §2, §6, §5.19 |
| RP-01 stazione e GUI, verniciatura dal gioco | §5.1-§5.11; parti 4a, 4b |
| RP-02 HUD vero al posto della riga | §5.12; parte 4c |
| RP-03 posizione della riga | §4.3; domanda 8 |
| RP-04 moduli attivi | M5 (D1) |
| RP-05 animazioni di apertura e posa di ingresso | parte 4e, dietro lo spike (D7) |
| RP-06 riparazione | §5.8; parte 4b |
| RP-07 `reinforced` e `unlocked` persi nella raccolta | §5.11; parte 4a |
| RP-08 sblocco dal gioco | §5.11; parte 4a |
| RP-09 controlli dell'installatore dei moduli | M5 (D1) |
| RP-10 regola dello `schema` per livelli, slot, moduli nuovi | resta com'è; M4 non ne aggiunge (§5.14, D1) |
| RP-11 `module_toggle` | M5 (D1) |
| RP-12 dati extra dei menu, basi client | §5.5, §5.20; parte 4a |
| RP-13 mixin del lampeggio degli slot | M5 (§2) |
| RP-14 strato luminoso | parte 4d, o M5 con il motivo |
| RP-15 da M3 §10 "per M4": arti nelle piastre, `getInstanceId`, coscia, braccia del pilota, generatore oltre un livello | `getInstanceId` e braccia del pilota con la 4e; arti nelle piastre e coscia aspettano Q-02; generatore oltre un livello M5 |
| RP-16 passaggio sulla grafica (ombra, mantello, pilota accovacciato, `Face.occlusion`) | non M4: aspettano le prove sul PC e Q-03, Q-05, Q-06 |
| RP-17 animazione del cambio cella | 4e se lo spike riesce, altrimenti M5 |
| RP-18 braccio in prima persona | fatto nella 3d; vincola l'HUD (§4.3, §5.12, §12) |
| C-01 angoli dell'HUD | §4.3; domanda 8 |
| C-02 HUD e braccio | §4.3, §5.12, §12 |
| C-03 colori delle condizioni | §5.12; domanda 9 |
| C-04 regola dell'elmo | §5.12 (`HudLevel`) |
| C-05 moduli in M4 o in M5 | M5 (D1); spazio riservato in GUI e HUD |
| C-06 secondario delle gambe | §5.9; domanda 6 |
| C-07 scala della tuta e stazione | §5.1 (tre blocchi liberi sopra la stazione), §5.4 (scala dell'anteprima) |
| C-08 decalcomanie | M5 (D1) |
| C-09 animazione del cambio cella | 4e o M5 |
| C-10 scintille dello stato critico | M5 (§2) |
| C-11 differenze minori | blocchi e oggetti restano in `content/`; `Platform` non prende un "menu type" (basta `MenuRegistry` di Architectury); in M4 i tasti sono H e la vignetta |
| L-01 blocco 2x1 | §5.1 |
| L-02 "entro 2 blocchi" e il telaio che sparisce | §5.2, §5.18 |
| L-03 contenuto e vita della GUI, due giocatori | §4.2, §5.3, §5.18 |
| L-04 riparazione e kit | §5.8; domande 1 e 2 |
| L-05 verniciatura | §5.9; domanda 3 |
| L-06 preset | §5.10, §5.14; domanda 4 |
| L-07 dettagli dell'HUD | §5.12 |
| L-08 G e N senza moduli | non esistono in M4 (D1) |
| L-09 rinforzato e sblocco | §5.11; domanda 5 |
| L-10 animazione di apertura e atomicità | parte 4e |
| L-11 config dei valori nuovi | §5.15 |
| L-12 suoni | §5.17 |
| L-13 ricette | §5.1, §5.8; domande 2 e 7 |
| Q-01 posizione della riga | domanda 8 |
| Q-02, Q-03, Q-05, Q-06 grafica della tuta | restano aperte; non M4 |
| Q-04 secondario delle gambe | domanda 6 |
| Q-07 braccio in prima persona | resta aperta; se cambiano grandezza o spostamento del braccio, si rimisurano i riquadri di D2 e i controlli di §12 |
| Q-08 nessuna riparazione fino a M4 | chiusa dalla parte 4b |
| Q-09 decalcomanie | M5 |
| Q-10 politica dei mixin | M4 non aggiunge mixin |
| Q-11 telai piazzati e permessi | la stazione segue le regole di oggi (§5.2, §5.11) |
| Q-12 bilanciamento | invariato; l'HUD lo rende visibile |

## 12. Anteprime e controllo della disposizione

Le anteprime (MOCK-UP, in `anteprime/2026-11-19-m4/`; elenco in testa) sono disegnate da uno script che legge gli stessi rettangoli di §5.3 e §5.12, su scene fatte con gli scatti di riferimento del repository (il braccio della 3d, con il magenta di prova riportato al bronzo; le barre di stato della sopravvivenza dalla 3c; la tuta della 3a per l'anteprima 3D). Lo stesso script controlla le sovrapposizioni; l'esito, in pixel dello schermo a 960x540 con la scala dell'interfaccia 2:

| Mano, livello | HUD | Mirino 60x60+450+240 | Barra e stato 380x100+290+440 | Righe della chat x 0-664, y 280-460 | Braccio disegnato | Seconda mano | Di passaggio |
|---|---|---|---|---|---|---|---|
| destra, `FULL` | 176x70+4+466 | libero | libero | libero | 286x205+674+335: libero | 58x46+240+494: libero | nessuno |
| destra, `BAR_ONLY` | 134x70+46+466 | libero | libero | libero | libero | libero | nessuno |
| sinistra, `FULL` | 176x70+780+466 | libero | libero | libero | 286x205+0+335: libero | 58x46+662+494: libero | indicatore del salvataggio (1.21.1 e Fabric 1.20.1; Forge 1.20.1 non lo disegna), riga più bassa dei sottotitoli |
| sinistra, `BAR_ONLY` | 134x70+780+466 | libero | libero | libero | libero | libero | gli stessi |

- Ogni HUD sta nel riquadro del braccio **dell'altra mano**, che con quella mano non si disegna; lo tocca solo con la mappa a due mani (§5.12, accettato).
- Angoli della vignetta: in alto a sinistra e in basso a sinistra nessun elemento del gioco base; in alto a destra, di passaggio, effetti e avvisi; in basso a destra, di passaggio, indicatore del salvataggio e sottotitoli. L'angolo in basso a sinistra comincia a y 460, dove finiscono le righe della chat.
- Altre grandezze dell'interfaccia (i riquadri del braccio sono misurati solo a 960x540, M3 §13; qui sono ricavati scalandoli con l'altezza, come il disegno 3D, e restano una deduzione): a 427x240, 480x270, 640x360, 683x384 e 640x270 il gruppo sta in basso nell'angolo giusto e non tocca nulla, con le due mani; a 341x256 e 320x240 va in alto a sinistra e non tocca nulla, salvo le barre dei boss, di passaggio.
- GUI della stazione, 316x232: ci sta in 320x240 (margini 2 e 4), in 427x240 (55 e 4), in 480x270 (82 e 19); i cinquantasei widget non si sovrappongono e stanno tutti dentro la schermata.

Esito del controllo: tutto libero. Le immagini mostrano anche ciò che il controllo non misura: la riga della chat subito sopra il gruppo (`hud-destra-1-energia-piena.png`), la sagoma con un pezzo rotto, uno critico e uno non montato (`hud-destra-3-pezzo-rotto.png`), la sola barra senza elmo (`hud-destra-4-senza-elmo.png`), lo scambio di lato con la mano sinistra (`hud-sinistra-2-energia-bassa-pezzo-danneggiato.png`), ogni immagine con un ingrandimento x2 del gruppo; la schermata con il suggerimento di una tinta (`gui-stazione.png`) e alla grandezza minima (`gui-stazione-640x480.png`); la stazione con il telaio a portata e la zona dei 2 blocchi (`blocco-stazione.png`). Sono anteprime di disposizione e di stile, non la grafica: quella la decide il proprietario sugli script della parte 4d (D8).

## 13. Riporti della parte 4a-1

La 4a-1 è su `main` (fino a `f6da189`); le carte della sua esecuzione sono sul ramo `archivio/m3c-fascicoli`, in `docs/archivio/m4a1-esecuzione/`. Ciò che resta per le parti dopo, dalle revisioni dei task e dalla revisione finale:

- **Per la 4a-2:**
  - **Il lucchetto.** La regola di chi può bloccare e sbloccare oggi sta in due copie: `FramePermissions.canToggleLock` (con `player.hasPermissions(2)`) e il comando in `ExoframeCommand` (con `source.hasPermission(2)`). Quando arriva il pulsante del lucchetto, il comando chiama `canToggleLock` (tenendo esplicita la differenza della sorgente), oppure i due Javadoc si nominano a vicenda.
  - **La prova misurata di `station_view`.** Nella 4a-1 il pacchetto che arriva a schermata aperta ha solo la prova di fumo `/exoframe unlock`; con il lucchetto c'è un cambio che si vede, e la sequenza lo misura.
  - **Gli spazi vuoti del pannello.** L'icona tenue del tipo di pezzo di §5.3 arriva qui: nella 4a-1 c'è il fondo scuro con il bordo grigio.
  - **L'intestazione del pannello centrale è stretta.**
    - "Braccio sinistro: vuoto" occupa 114 pixel in un campo di 110.
    - Con `tiers.maxDurability` da 10 000 in su, i numeri della durata toccano la parola della condizione; lo stesso vale per la cella con un `energy.cellCapacity` molto grande.
  - **`StationScreen` è già di 443 righe.** Con i widget della 4a-2 il disegno del pannello del telaio e dell'intestazione va in classi sue.
  - **I gesti del mouse.**
    - Ogni widget che segue un trascinamento usa la distanza dal punto premuto, come l'anteprima: su 1.21.1 `mouseDragged` arriva sommato per fotogramma.
    - Il rilascio che chiude un gesto della schermata non arriva al gioco base, che altrimenti getterebbe o poserebbe la pila sul cursore.
  - **Il pulsante di prova 102 se ne va** quando il 102 ha la sua azione. Il controllo `pulsante` di `m4a1_shots.sh` diventa quello di un pulsante vero, per esempio la scelta 0-7.
- **Per la 4c:** il piano (`docs/superpowers/plans/2026-11-20-m4c-hud.md`) segue i valori provvisori delle domande 8-10. La sua decisione 1 ("Senza cella" sulla riga della barra vuota, perché "Nessuna cella" non entra nella riga) è stata seguita: chiesta al proprietario prima del Task 1, finora senza obiezioni; cambiarla costa un testo e una costante.
- **Limiti accettati della 4a-1** (in `docs/compat.md` e nel Javadoc di `StationBlock`):
  - su Forge 1.20.1, un piccone che si rompe proprio sul colpo che rompe la metà laterale fa perdere la stazione;
  - su NeoForge, la regola della rottura usa `hasCorrectToolForDrops` a un argomento, che non manda `PlayerEvent.HarvestCheck`.

## 14. Riporti della parte 4c

Le carte dell'esecuzione della 4c sono sul ramo `archivio/m3c-fascicoli`, in `docs/archivio/m4c-esecuzione/`. Ciò che resta per le parti dopo, dalla revisione finale (decisione R22); sono righe brevi, per chi le raccoglie.

- **Per la 4d, che ridisegna la grafica dell'HUD:**
  - Fissare in `HudLayoutTest`, a grafica finita, i rettangoli della sagoma e dell'avviso e la larghezza vera di due cifre delle celle di scorta (oggi `TWO_DIGIT_SPARE` = 18 è dedotta, non misurata). Fatto nella 4d: `HudLayoutTest.silhouetteAndWarningRectsAreFixed` fissa i sei pezzi e l'avviso, e `TWO_DIGIT_SPARE` = 18 è misurata nel gioco ("×10" su 1.21.1-neoforge: l'icona a 41 e il testo a 47 nel sottogruppo dell'energia).
  - Fissare il corpo della barra `122x10+52+510` (`BAR_FILL` senza la prima riga) in `screenRectsAtNineSixtyMatchTheSequence`; il riquadro `barra-esaurita` `42x16+50+482` dipende dalla larghezza di "0%" e si fissa solo con una larghezza misurata nel gioco. Fatto nella 4d: il corpo è fissato in `screenRectsAtNineSixtyMatchTheSequence`, e il riquadro `barra-esaurita` lo controlla esatto, nel gioco, `m4c_shots.sh` (`WARNING_PERCENT_RIGHT`), non un JUnit.
  - Fissare il riquadro `testo` `46x14+50+482` in `m4c_shots.sh` (si controlla con `M4C_SOLO_CONTROLLI=1` sugli scatti che ci sono). Fatto nella 4d: `TEXT_BOX_RIGHT` di `m4c_shots.sh`, nel controllo `testo`.
  - Un'unica costante per il rosso della carica della cella: oggi `HudModel.CELL_CHARGE` e `StationLayout.CELL_RED`, con lo stesso valore. Non vale più: con la 4d le due costanti non ci sono più, perché il codice non disegna più quel rosso: sta nei disegni delle texture (nella stazione `CELL_RED` di `tools/art/gui_layout.py`, nell'HUD l'icona della cella).
- **Test in più, quando si toccano quei file:**
  - In `ClientPrefsFileTest`, un file con una voce giusta e una sbagliata (`{"hudHidden": "yes", "vignette": false}`): ne vengono i predefiniti.
  - In `HudLayoutTest`, un `intersects` vero e un `contains` falso.
- **Dalla prova sul PC (DEC-48: ciò che trova diventa il primo compito della parte dopo):** che cosa si vede su Fabric 1.21.1 dei sottotitoli e dell'indicatore del salvataggio, e su NeoForge 1.21.1 di F3 sopra il gruppo; sono dedotti dal bytecode, non visti nel cloud (le decisioni sono nelle carte archiviate sopra).
- **Ciò che l'esecuzione ha smentito del piano** (il piano resta com'era stato scritto):
  - "×0" è largo 12, non 11.
  - `options.txt` vuole la riga `version:` perché le righe `key_` contino.
  - Se gli strati del gioco base stanno sopra o sotto l'HUD dipende dal loader, non dalla versione (§5.12).
  - Il controllo `riavvio` confronta il `nascosto` della prova A con il `riavvio-nascosto` della C, e `riavvio-senza-tuta` non c'è più (la tuta alza la camera).
- **Le domande 8-10 del proprietario** restano ai valori provvisori; la decisione 1 ("Senza cella") è seguita.
