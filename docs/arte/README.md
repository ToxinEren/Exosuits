# Grafica di Redstone Exoframes

La grafica (modelli, texture, animazioni) è generata da script nella sessione cloud, su decisione di Luigi del 13 novembre 2026. Questo file tiene la direzione artistica, le scelte fatte e le anteprime mostrate.

## Direzione

Esoscheletro industriale pesante: sagoma massiccia, piastre rivettate, pistoni e tubi a vista, una fonte di energia sulla schiena. L'ispirazione generale indicata da Luigi sono le armature potenziate di Fallout 4 e Iron Monger (Iron Man, 2008). Si prende l'idea, non le forme: nessun elmo, piastra, simbolo o scritta riconoscibile di design esistenti.

Stile Minecraft: scatole allineate agli assi, texture a pixel senza sfumature morbide.

**Richiesta di Luigi del 13 novembre 2026, viste le anteprime del primo telaio:** «il telaio ci sta ma lo voglio più dettagliato e metallico, poi lo scheletro della tuta dovrebbe essere più grande del player, tipo mecha». Da qui: la tuta è più grande di chi la pilota, che resta a grandezza normale e vi sta dentro; più componenti; texture a densità doppia; resa da metallo. Il perché e il come sono nel design della milestone 3, §4.7. Alle prime tavole Luigi ha risposto «va bene 1.4 ma manca la testa della power armor, il player deve essere alzato da terra dalle gambe della power armor»: ne è nato il telaio v2, descritto più sotto, che sostituisce il primo.

## Livello Rottame: tre direzioni proposte (13 novembre 2026)

Le tavole sono in `anteprime/2026-11-13-direzioni/`, disegnate sul primo telaio; con il telaio v2 i pezzi si ripropongono sulla nuova forma (design della milestone 3, §8). I riquadri chiari su petto e spalla segnano solo il posto riservato alle decalcomanie; il manichino dentro il telaio nudo non fa parte del modello.

| Direzione | Idea | Giudizio di chi l'ha disegnata |
|---|---|---|
| A, piastre squadrate | Spalle alte, elmo incassato con fessura luminosa, guanti pesanti | Sagoma più pulita e leggibile |
| B, telaio a vista | Piastre piccole e asimmetriche, pistoni scoperti, mezza maschera con una lente | La più debole: confusa da lontano |
| C, caldaia | Caldaia di rame sulla schiena con comignolo, manometro e tubi; avambracci grossi; elmo piccolo a oblò | La più caratteristica; davanti un po' affollata |

**Scelta di Luigi:** in attesa. **Predefinita se non risponde:** C per il Rottame (sembra costruita con pezzi di recupero), A come direzione dei livelli più avanzati.

## Colori predefiniti del Rottame

Usati quando un pezzo non è verniciato. Primario `0xB5651D` (arancio ruggine), secondario `0x5F7D7A` (verde rame spento). Il task dei pezzi Rottame può cambiarli se la resa lo richiede; il codice li legge da qui.

## Telaio v2: scheletro in scala 1,4 (13 novembre 2026)

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

- Generatore: `tools/art/` (istruzioni in `tools/art/README.md`); il telaio è in `tools/art/designs/frame.py`. Lo spike del primo telaio non c'è più: i pezzi Rottame partono dalla prova d'arte del telaio v2.
- File generati: `src/main/resources/assets/redstone_exoframes/geo/`, `textures/entity/` (atlante e maschera luminosa), `animations/`.
- Convenzioni sui nomi delle ossa e sui perni: `docs/superpowers/specs/2026-11-13-m3-telaio-design.md`, §5.1.
