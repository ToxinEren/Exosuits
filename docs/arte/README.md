# Grafica di Redstone Exoframes

La grafica (modelli, texture, animazioni) è generata da script nella sessione cloud, su decisione di Luigi del 13 novembre 2026. Questo file tiene la direzione artistica, le scelte fatte e le anteprime mostrate.

## Direzione

Esoscheletro industriale pesante: sagoma massiccia, piastre rivettate, pistoni e tubi a vista, una fonte di energia sulla schiena. L'ispirazione generale indicata da Luigi sono le armature potenziate di Fallout 4 e Iron Monger (Iron Man, 2008). Si prende l'idea, non le forme: nessun elmo, piastra, simbolo o scritta riconoscibile di design esistenti.

Stile Minecraft: scatole allineate agli assi, texture a pixel senza sfumature morbide.

**Richiesta di Luigi del 13 novembre 2026, viste le anteprime del primo telaio:** «il telaio ci sta ma lo voglio più dettagliato e metallico, poi lo scheletro della tuta dovrebbe essere più grande del player, tipo mecha». Da qui: la tuta è più grande di chi la pilota, che resta a grandezza normale e vi sta dentro; più componenti; texture a densità doppia; resa da metallo. Il perché e il come sono nel design della milestone 3, §4.7. Alle prime tavole Luigi ha risposto «va bene 1.4 ma manca la testa della power armor, il player deve essere alzato da terra dalle gambe della power armor»: ne è nato il telaio v2, descritto più sotto. Due passate successive della prova d'arte (fuori dal repository) hanno allargato lo scheletro alle braccia e al petto e poi, su un riferimento di proporzioni mandato da Luigi, l'hanno reso pesante in alto con un'armatura a piastre; della quarta passata Luigi ha detto «mi piace l'aspetto dell'armatura rottame nelle ultime tavole»: è il telaio e il livello Rottame di oggi, descritti nella prossima sezione.

## Telaio e pezzi Rottame approvati (quarta passata, 13 novembre 2026)

Anteprime in `anteprime/rottame-v4/`, in scala 1,65, fatte con `node shot.js` in `tools/art/preview/` (il pilota è disegnato come nel gioco e non fa parte del modello):
- `telaio-fronte.png`, `telaio-retro.png` (3/4), `telaio-lato.png` (lato destro), `telaio-porta.png` (davanti, griglia a blocchi, porta 1×2 in bianco, box di collisione in piedi in giallo): il telaio nudo, con il pilota visibile dalla vita in su;
- `armata-fronte.png`, `armata-retro.png`, `armata-lato.png`, `armata-porta.png`: i sei pezzi Rottame montati, stato 0, colori predefiniti;
- `armata-fronte-stato1.png`, `-stato2.png`, `-stato3.png`: gli stessi pezzi negli stati danneggiato, critico e rotto;
- `armata-fronte-colori-3A6EA5-D9D9D9.png`: stato 0 verniciato di blu e grigio chiaro, per vedere quali parti prendono i due colori.

**Telaio** (`tools/art/designs/frame.py`, 209 scatole). Scheletro massiccio, più grande del pilota alle braccia, al petto e alle gambe. Testa scheletrica stretta attorno alla testa del pilota (visiera aperta, una spia sulla fronte). Petto a botte: montanti larghi che si allargano verso le spalle, costole sottili, giogo d'acciaio sotto il mento, collare che sale ai lati e dietro la testa; vita stretta a quattro travi attorno al sedile. Gli attuatori delle spalle (alloggiamenti grandi con mozzo d'ottone, frontale e coperchio d'acciaio, alette per lo spallaccio) sono del busto, e pistoni li collegano alla schiena e al bacino. Braccia: il braccio del pilota sta in una gabbia con l'impugnatura sotto la mano; fuori, il braccio meccanico (trave con due pistoni, gomito grande con mozzi d'ottone, avambraccio pesante con pistoni e griglia, polso d'ottone, mano grande con tre dita e pollice, fondo all'altezza del ginocchio). Schiena: spina a vertebre, alloggiamento grande della cella di redstone dietro tre barre, griglie laterali, due camini, cavi e tubi. Gambe: prese delle cosce chiuse, ginocchio grande con mozzo e snodo, stinco spesso, piede largo.

**Pezzi Rottame** (`tools/art/designs/scrap.py`, 90 scatole per stato). Armatura pesante a piastre di recupero. Elmo basso e stretto, incassato nel collare: calotta e tetto a gradino, piastra del viso a due gradini con una banda di visione orizzontale dietro tre sbarre, tettuccio scuro sopra, tre feritoie per l'aria sul mento. Torso: petto a tre lastre annidate con angoli a gradino e una chiglia verticale scura al centro, tre fessure luminose basse sul lato sinistro, una toppa a destra, collare attorno alla testa, vita a tre fasce scure, falde basse davanti e sui fianchi, piastra della schiena; **gli spallacci stanno nel torso** (perché gli attuatori delle spalle sono del busto): tre strati piatti, gonne esterna, davanti e dietro, feritoie a lamelle davanti, una toppa sullo spallaccio destro. Braccia: piastra esterna del braccio, cubitiera, bracciale grosso (esterno, davanti, dietro, rinforzo, toppa dietro), paranocche. Gambe: cosciale, ginocchiera con costola, schiniere a due strati, fianco dello stinco, puntale e caviglia dello stivale. Braccia e gambe sinistre sono la copia speculare delle destre (un solo item per braccio e uno per gamba): nella prova la toppa del bracciale c'era solo a sinistra, qui c'è su tutti e due. Elmo e torso restano asimmetrici.

**Regioni di colore.** Ogni pezzo ha tre ossa per stato: `_p1` (primaria: lastre del petto, collare, falde, spallacci, piastre principali di braccia e gambe, calotta e viso dell'elmo), `_p2` (secondaria: chiglia, toppe, vita, strati alti e gonne degli spallacci, rinforzi, tetto e tettuccio dell'elmo) e `_n` (neutra: vetro della visiera, sbarre, feritoie per l'aria, lamelle degli spallacci, fessure luminose). Le prime due hanno texture in grigio chiaro che il gioco moltiplica per il colore del pezzo; la neutra ha i colori finali. Braccia e gambe non hanno parti neutre (le loro ossa `_n` esistono, vuote).

**Stati visivi.** Quattro copie di ogni pezzo con le stesse scatole: 0 integro, 1 danneggiato, 2 critico, 3 rotto; il gioco ne mostra una. Cambia solo la pittura, e ogni segno nasce nello stesso punto in tutti gli stati e cresce con lo stato, così il pezzo invecchia invece di cambiare:
- 0: vernice consumata ai bordi, scheggiature piccole, colature sotto i rivetti (come nelle tavole approvate);
- 1: più scuro, le scheggiature si allargano, ammaccature scure con il bordo basso illuminato, i primi graffi lunghi (un solco scuro con il labbro chiaro), un velo di fuliggine, le prime macchie di ruggine sul metallo nudo;
- 2: ancora più scuro, chiazze larghe di fuliggine, ammaccature più grandi, più graffi e più lunghi, crepe scure; qualche texel di spie e vetri spento, più ruggine;
- 3: bruciato: la superficie diventa carbone e la vernice resta solo a isole, crepe larghe due texel, metà delle spie spenta.
Sulle regioni colorate l'usura può essere solo più scura o più chiara del grigio (il colore la moltiplica): metallo nudo e ruggine con il loro colore compaiono solo sulla regione neutra. Dal generatore: la luminosità media di ogni pezzo e regione cala da uno stato al successivo (per esempio il p1 del torso 128, 99, 72, 45 su 255), e `build.py` si ferma se non è così.

**Decalcomanie (riservate).** Due ossa con una scatola piatta 3,5×3,5 px (7×7 texel), 0,25 px sopra la piastra, texel trasparenti, nascoste dal gioco fino a M5: `pieceTorso_decal` sulla lastra centrale del petto, accanto alla chiglia sul lato destro di chi indossa (a sinistra per chi guarda); `pieceLeftArm_decal` sulla piastra esterna del braccio sinistro, sotto lo spallaccio (sullo spallaccio non ci starebbe dentro l'ingombro, e l'osso del braccio la porta con sé quando il braccio si muove).

**Scala 1,65 (richiesta di Luigi del 13 novembre, dopo le tavole approvate).** Luigi ha visto che l'armatura sarebbe stata alta due blocchi e mezzo e la vuole alta almeno 3 blocchi: la tuta si disegna in scala 1,65 invece di 1,4 (fino a 1,66 il box in piedi del giocatore, 1,8 S blocchi, passa ancora sotto un soffitto a 3 blocchi) e il pilota è sollevato di 24 × 0,65 = 15,6 pixel del modello (0,91 blocchi). Il pilota resta a grandezza normale, quindi dentro la tuta è relativamente più piccolo: nelle coordinate del modello è un giocatore scalato di 1/1,65 attorno a (0, 24, 0), con la testa tra y 24 e 28,85, il busto e le braccia tra 16,73 e 24, le gambe (non disegnate) tra 9,45 e 16,73. Le forme approvate restano; cambiano solo le parti che devono seguire il pilota: la testa del telaio sale fino a y 31,25 (fronte più alta, con la spia; calotta, tempie e nuca più alte; montanti della visiera fino alla fronte), l'elmo fino a 32 (lati, viso e retro più alti, i due tetti sopra), le impugnature salgono di 1,5 px sotto le mani del pilota (a y 16..16,5) e il sedile sale fino a y 16,5 e si stringe di 0,25 px per lato, così chiude da ogni lato il posto delle gambe del pilota sotto il busto.

**Misure.** Un blocco vale 17,07 pixel del modello non scalati; misure in blocchi = pixel × 1,65 × 0,9375 / 16.

| | pixel del modello (larg. × alt. × prof.) | blocchi (alt. × larg. × prof.) |
|---|---|---|
| telaio nudo | 25,5 × 31,25 × 17,5 (x ±12,75, y 0..31,25, z −7,5..10) | 3,02 × 2,47 × 1,69 |
| con i pezzi Rottame | 27 × 32 × 18,5 (x ±13,5, y 0..32, z −8,5..10) | 3,09 × 2,61 × 1,79 |
| limite del telaio nudo (`exo.FRAME_MIN/MAX`, `ExoframeGeometryTest`) | x ±12,75, y 0..31,25, z −8..10 | |
| limite con i pezzi (`exo.SUIT_MIN/MAX`, `ExoframeGeometryTest`) | x ±13,5, y 0..32, z −8,75..10,5 | |

Rispetto al box di collisione in piedi in scala (0,6S × 1,8S = 0,99 × 2,97 blocchi) la tuta armata sporge di circa 0,81 blocchi per parte e la cima (il tetto dell'elmo) di 0,12; il telaio nudo sporge di 0,74 per parte e di 0,05 in alto. Non passa da una porta 1×2.

**Somiglianze.** Il riferimento di Luigi per le proporzioni era un'armatura pesante di un film: se ne sono prese la sagoma pesante in alto, il petto a botte, la testa incassata, le spalle grandi, gli avambracci grossi e il colore di metallo scuro caldo, non gli elementi che la identificano. Niente disco luminoso sul petto (al centro c'è una chiglia; le luci sono tre fessure basse a lato), niente volto di quell'elmo (qui una piastra piatta con una banda di visione dietro sbarre), spallacci a lastre piatte con feritoie e non a cupola con file di fori, niente barra sopra spalle e testa, la cella resta sulla schiena in un alloggiamento a barre; niente loghi né scritte.

## Livello Rottame: tre direzioni proposte (13 novembre 2026, superate dalla quarta passata)

Le tavole sono in `anteprime/2026-11-13-direzioni/`, disegnate sul primo telaio, e restano come storico. I riquadri chiari su petto e spalla segnano solo il posto riservato alle decalcomanie; il manichino dentro il telaio nudo non fa parte del modello.

| Direzione | Idea | Giudizio di chi l'ha disegnata |
|---|---|---|
| A, piastre squadrate | Spalle alte, elmo incassato con fessura luminosa, guanti pesanti | Sagoma più pulita e leggibile |
| B, telaio a vista | Piastre piccole e asimmetriche, pistoni scoperti, mezza maschera con una lente | La più debole: confusa da lontano |
| C, caldaia | Caldaia di rame sulla schiena con comignolo, manometro e tubi; avambracci grossi; elmo piccolo a oblò | La più caratteristica; davanti un po' affollata |

Nessuna delle tre è stata scelta: il Rottame è l'armatura a piastre della quarta passata, descritta sopra.

## Colori predefiniti del Rottame

Usati quando un pezzo non è verniciato. Primario `0x8A6F52` (bronzo invecchiato), secondario `0x4A4F55` (canna di fucile). Sono quelli delle tavole approvate (prima erano `B5651D` e `5F7D7A`); `client/TierPalette.java` li ha uguali e `TierPaletteTest` li confronta con questa riga. Le anteprime di `tools/art/preview` li usano come colori predefiniti.

## Telaio v2: scheletro in scala 1,4 (13 novembre 2026, sostituito dalla quarta passata)

Anteprime in `anteprime/telaio-v2/`: `fronte.png` e `retro.png` (3/4), `lato.png` (lato destro), `porta.png` (davanti, con griglia a blocchi, porta 1×2 in bianco e box di collisione in piedi in giallo). Il pilota dentro il telaio non fa parte del modello: è disegnato come nel gioco. Si rifanno con `node shot.js` in `tools/art/preview/` (istruzioni in `tools/art/README.md`).

**Scala e pilota.** Il gioco disegna la tuta in scala 1,4 attorno ai piedi. Il pilota resta a grandezza normale, sollevato di 9,6 pixel del modello (circa 0,56 blocchi) perché collo e spalle combacino con quelli della tuta; le sue gambe non si disegnano: entrano nelle cosce chiuse della tuta, e sotto ci sono solo ginocchia, stinchi e piedi meccanici, che lo tengono sollevato.

**Scheletro e pezzi.** Il telaio è solo scheletro: travi, mozzi, pistoni, spina dorsale, alloggiamento della cella, cavi e tubi. Le piastre (spallacci, pettorali, cosciali, schinieri, elmo) sono i pezzi, che si montano sopra; il telaio porta i punti dove si agganciano: borchie cromate sui montanti del petto, sugli avambracci, sugli stinchi e sulle cosce, alette d'acciaio sulle spalle (due per spallaccio), sul giogo e ai lati della testa per l'elmo. Il telaio resta sempre visibile, anche sotto i pezzi, ed è ciò che sta in piedi nel mondo senza pezzi montati.

**Com'è fatto.** Nessuna scatola ruotata, 181 scatole.
- Testa: calotta sottile, tempie, guance e mascelle d'acciaio, nuca, fronte pesante con due spie, mentoniera, due montanti della visiera; la visiera è aperta e il viso del pilota si vede. Mozzi d'ottone alle orecchie, un sensore rosso sulla guancia destra.
- Busto: gabbia aperta sul petto (due montanti e due costole), giogo davanti, sui fianchi e dietro la nuca; sotto, il sedile del bacino (fondo, bordo davanti, schienale) che chiude in alto le prese delle cosce.
- Schiena: spina a vertebre su un'anima cromata, alloggiamento della cella (due colonne, cornice, cella di redstone incassata dietro tre barre di protezione, griglie sui lati, spie), due camini di scarico con fascia d'ottone e bocca calda, cavi di rame verso il giogo e verso il sedile, tubi di gomma.
- Braccia: alloggiamento della spalla con grande mozzo d'ottone, trave sul lato esterno, pistoni davanti e dietro, gomito con mozzo, avambraccio con spia. Le mani del pilota poggiano su un'impugnatura cromata dentro una piccola gabbia; sotto c'è la mano della tuta, meccanica: palmo, tre dita in due falangi, pollice, barra d'ottone sulle nocche. È lì che il gioco disegnerà l'oggetto impugnato.
- Gambe: presa della coscia chiusa con due fasce e un pistone davanti, mozzo dell'anca; ginocchio con mozzo esterno e snodo davanti; stinco a due montanti con pistoni davanti e dietro; caviglia con mozzo; piede largo (suola, punta, calotta, tallone, sperone).
- Resa: due texel per pixel del modello, atlante 512×512. Acciaio brunito per la struttura, acciaio chiaro per le parti lavorate, ottone per i mozzi (anelli e bullone cromato al centro), steli cromati, cavi di rame ad anelli, gomma. Spazzolatura verticale, spigoli chiari verso la luce dipinta (dall'alto, davanti, a destra di chi indossa) e scuri dall'altra parte, occlusione nelle rientranze, bulloni, graffi e scheggiature, tinta di calore attorno a griglie e camini, grasso vicino ai mozzi. Cella e spie sono anche nella maschera luminosa (`exoframe_glowmask.png`, per lo strato luminoso di GeckoLib).

**Misure.** Un blocco vale 17,07 pixel del modello non scalati (il giocatore è reso a 15/16); misure in blocchi = pixel × 1,4 × 0,9375 / 16.

| | pixel del modello (larg. × alt. × prof.) | blocchi (alt. × larg. × prof.) |
|---|---|---|
| telaio nudo | 19 × 30,5 × 14,5 (x ±9,5, y 0..30,5, z −5,5..9) | 2,50 × 1,56 × 1,19 |
| limite del telaio nudo (`exo.FRAME_MIN/MAX`, `ExoframeGeometryTest`) | x ±9,5, y 0..30,7, z −7..9 | |
| limite con i pezzi (`exo.SUIT_MIN/MAX`) | x ±10,75, y 0..31,5, z −8..9,5 | 2,58 × 1,76 |

Rispetto al box di collisione in piedi in scala (0,6S × 1,8S = 0,84 × 2,52 blocchi) la cima del telaio sta dentro (0,02 blocchi sotto); di lato sporgono di circa 0,36 blocchi per parte i mozzi delle spalle, i gomiti e le mani; dietro la cella sporge di circa 0,32 blocchi. Il telaio non passa da una porta 1×2: è troppo largo e, sopra i 2 blocchi, restano fuori testa, spalle e camini. Accovacciato (1,5S = 2,1 blocchi) non passa sotto un architrave a 2 blocchi.

**Somiglianze.** Pilota in un telaio aperto con spalle alte e una fonte d'energia sulla schiena è l'idea generica dell'armatura potenziata, che era la richiesta. Niente volantino a raggiera (la cella è una finestra rettangolare dietro barre dritte), niente emblema rotondo sul petto (il petto è una gabbia di barre), niente elmo chiuso, niente scritte o loghi.

**Da sapere per l'animazione.** Le braccia della tuta ruotano attorno a (±5, 22), quelle del pilota circa attorno a (±3,6, 22,6): con l'oscillazione la mano del pilota si sposta un po' sull'impugnatura. Testa del pilota e testa della tuta condividono il perno (0, 24, 0). Il sedile (osso del busto) e le prese delle cosce (ossa delle gambe), come la mascella (testa) e il giogo (busto), si compenetrano da fermi: con le gambe o la testa in movimento si separano un poco. La luce dipinta e l'occlusione presuppongono la posa a riposo.

## Primo telaio (13 novembre 2026, sostituito dal telaio v2)

Anteprime in `anteprime/telaio-nudo/` (fronte, retro, lato; il manichino non fa parte del modello), tenute come storico. Il telaio è sempre visibile, anche sotto i pezzi, ed è ciò che sta in piedi nel mondo senza pezzi montati. Acciaio scuro per montanti e barre, mozzi d'ottone ai giunti (spalle, gomiti, anche, ginocchia, tempie), cella di redstone accesa in un alloggiamento sulla schiena, protetta da due barre, con cavi di redstone verso le spalle e cavi di rame verso la cintura; pistoni d'ottone con steli cromati dietro le braccia e i polpacci; collare aperto davanti che poggia sulle spalle; gabbia leggera attorno alla testa (montanti alle tempie, arco sopra, cerchio alla fronte con una spia) che lascia il viso libero; polsiere a U, piastre davanti ai pugni, caviglie a U, puntali e suole sotto i piedi. Nessuna scatola ruotata; il telaio resta ad almeno 0.25 px dal corpo e 0.5 px dalla testa del giocatore. Era a grandezza del giocatore, con il pilota in piedi a terra; atlante 256×256, un texel per pixel.

## Dove sta cosa

- Generatore: `tools/art/` (istruzioni in `tools/art/README.md`); il telaio è in `tools/art/designs/frame.py`, i pezzi Rottame in `tools/art/designs/scrap.py`, la pittura degli stati in `tools/art/paint.py` (`wear`). La prova d'arte da cui vengono resta fuori dal repository.
- File generati: `src/main/resources/assets/redstone_exoframes/geo/`, `textures/entity/` (atlante e maschera luminosa), `animations/`.
- Convenzioni sui nomi delle ossa e sui perni: `docs/superpowers/specs/2026-11-13-m3-telaio-design.md`, §5.1.
