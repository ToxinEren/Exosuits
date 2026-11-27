# Generatore della grafica

Genera da script la grafica della tuta (telaio nudo e pezzi, con i loro stati visivi): geometria GeckoLib, atlante,
maschera luminosa e animazioni. Direzione
artistica e scelte in `docs/arte/README.md`; convenzioni delle ossa in
`docs/superpowers/specs/2026-11-13-m3-telaio-design.md`, §5.1. La stazione di manutenzione ha un generatore suo
(`station.py`, parte 4d), descritto in fondo, in "Stazione di manutenzione".

## Comandi

Dalla radice del repository (serve Python 3 con numpy, versione in `tools/art/requirements.txt`:
`python3 -m pip install -r tools/art/requirements.txt`):

    python3 tools/art/build.py                 rigenera i quattro file in src/main/resources/assets/redstone_exoframes/
    python3 tools/art/build.py --out <dir>     li scrive in <dir>/geo, <dir>/textures/entity, <dir>/animations
    python3 tools/art/build.py --out <dir> --designs frame
                                               solo i design indicati (per esempio il telaio nudo, per le anteprime)
    bash tools/check_art.sh                    prove negative, poi rigenera in una cartella temporanea e confronta:
                                               "OK art"
    python3 tools/art/selftest.py              solo le prove negative dei controlli (vedi sotto)
    python3 tools/art/swing.py [--part gambe|braccia|testa|tutto] [--max N] [--all]
                                               arti e testa in movimento: per ogni parte e direzione il primo angolo
                                               (1..81 gradi, il passo di corsa del gioco base) a cui una sua scatola
                                               entra nel busto; gambe e braccia avanti e indietro, la testa a destra,
                                               a sinistra, in su e in giu'; predefinito gambe. Da rifare quando si
                                               cambiano gambe, braccia, testa, bacino o schiena (non e' un controllo
                                               della build); i risultati di oggi sono in docs/arte/README.md

File generati (uguali sulle quattro varianti):

- `geo/exoframe.geo.json`: geometria Bedrock `format_version` 1.12.0, identificatore `geometry.exoframe`;
- `textures/entity/exoframe.png`: atlante 512x512, PNG senza metadati (solo IHDR, IDAT, IEND);
- `textures/entity/exoframe_glowmask.png`: maschera luminosa, stessa forma dell'atlante: i texel accesi dei
  materiali luminosi (`cell`, `lamp`, `glass`; negli stati 2 e 3 alcuni sono spenti) e trasparente il resto, per lo strato luminoso di GeckoLib
  (`AutoGlowingGeoLayer`, che cerca `<texture>_glowmask.png`);
- `animations/exoframe.animation.json`: `{"format_version": "1.8.0", "animations": {}}`.

Stesso input, stessi byte: semi ricavati dai nomi delle scatole, nessuna data, ordine stabile di ossa, scatole e
rettangoli, JSON con chiavi in ordine fisso e numeri interi quando possibile, PNG compresse con lo zlib di Python
(livello 9). La pittura usa solo aritmetica intera per le scelte casuali (`paint.HashRng`, niente generatore di
numpy, il cui flusso puo' cambiare tra versioni); numpy resta comunque fissato in `requirements.txt`. La pittura
pero' usa ancora i numeri in virgola mobile di numpy (anche `cos` e `sin` in `paint._line`): i byte sono gli stessi
su x86-64 con il numpy fissato, ma su un'altra architettura (per esempio ARM) l'ultima cifra puo' cambiare. Se
`check_art.sh` fallisce solo su una macchina cosi', si rigenera dove gira la CI (x86-64) e non si committa il
risultato dell'altra macchina.
`check_art.sh` confronta byte per byte i due JSON; per le due PNG (atlante e maschera luminosa) confronta l'elenco
dei blocchi (solo IHDR, IDAT, IEND), IHDR e i pixel decompressi (`tools/art/compare_png.py`), perche' i byte
compressi cambiano con la build di zlib (per esempio zlib-ng) anche a pixel identici.

`build.py` si ferma (uscita 1, senza scrivere nulla) se il modello non passa i controlli di `exo.Model.check()` o
quello della pittura degli stati (`paint.check_wear`, sotto):

- facce complanari con la stessa normale che si sovrappongono, in tutte e sei le direzioni (anche verso il basso),
  salvo la parte coperta da altre scatole (controllata cella per cella sulla griglia dei quarti di pixel, quindi
  anche una striscia scoperta di 0.25 px viene segnalata). Coprono solo le scatole che si disegnano sempre insieme
  alle due facce (`Model._always_with`): il telaio, e i pezzi dello slot e dello stato delle due facce; mai un pezzo
  di un altro slot (con l'armatura parziale puo' mancare) ne' le decalcomanie (il renderer le nasconde);
- scatole troppo vicine al pilota, misurate come nel gioco: la tuta scalata di `exo.SUIT_SCALE` (1.65) attorno ai
  piedi, il pilota a grandezza normale sollevato di `exo.PILOT_LIFT_PX` (24(S-1) = 15.6 px, collo e spalle
  allineati a quelli della tuta) e senza gambe (non si disegnano: stanno nelle cosce chiuse della tuta). Margini:
  0.25 px dal corpo e dalle braccia (dove sta il secondo strato della skin), 0.5 px dalla testa (il cappello);
- scatole fuori dall'ingombro, in pixel non scalati: per le ossa del telaio (regione `frame`, `exo.FRAME_MIN/MAX`)
  x -12.75..12.75, y 0..31.25, z -8..10; per le ossa dei pezzi e delle decalcomanie (`exo.SUIT_MIN/MAX`)
  x -13.5..13.5, y 0..32, z -8.75..10.5;
- per ogni pezzo e regione di colore, la luminosita' media delle facce non cala da uno stato visivo al successivo
  (uno stato piu' alto non deve mai sembrare meno rovinato).

Le facce interamente sepolte in altre scatole dello stesso osso non vengono esportate (niente spazio sprecato
nell'atlante); anche questo si decide cella per cella sulla griglia dei quarti di pixel, quindi una faccia con una
striscia scoperta di 0.25 px non sparisce.

`tools/art/selftest.py` (eseguito da `check_art.sh`, quindi anche dalla CI) prova che i controlli funzionano:
inietta un difetto alla volta in una copia del modello costruita in memoria e pretende la segnalazione. Le prove:

1. facce complanari con la stessa normale: (a) la parte comune coperta solo da un pezzo di un altro slot, (b) solo
   da una decalcomania, (c) una faccia "sepolta" con una striscia scoperta di 0.25 px su un lato di 0.75 px, che non
   deve sparire dall'atlante;
2. scatola nella testa del pilota;
3. scatola fuori dai limiti;
4. coordinata fuori dalla griglia dei quarti di pixel;
5. linea sottile fuori dalla sua scatola;
6. stato visivo piu' chiaro del precedente;
7. regione dell'atlante piena;
8. faccia fine diversa dalla sua versione a densita' normale;
9. un osso di primo livello con i pezzi di due slot (regola: un solo slot per osso di primo livello, su cui si
   regge il controllo delle facce complanari);
10. `build.py` si ferma su un modello che non passa i controlli: esce con 1 senza scrivere nessun file.

Le prove 1a, 1b e 1c verificano prima la loro premessa sul disegno del mod (che cosa copre la parte iniettata) e
falliscono dicendolo se il disegno cambia; 1a e 1b contano solo la segnalazione sulla faccia di cui hanno verificato
la premessa (nord). Esce con 1 se un difetto passa, se una premessa non vale piu' o se non c'e' nessuna prova
registrata. Un controllo nuovo va aggiunto anche li'.

`ExoframeGeometryTest` (JUnit) legge il file generato e controlla nomi e perni delle ossa (quelli dei
pezzi e delle decalcomanie costruiti da `SuitBones`), i due ingombri, che le quattro copie di un pezzo abbiano le
stesse scatole e stiano ognuna nella sua zona dell'atlante, che braccia e gambe sinistre siano lo specchio delle
destre (scatole e rettangoli), che le decalcomanie siano piatte e trasparenti, dimensione dell'atlante e UV.
`SuitScaleTest.matchesTheArtGenerator` confronta `exo.SUIT_SCALE` con la scala del gioco.

Misure in blocchi: il giocatore e' reso a 15/16, quindi un blocco vale 16 / 0.9375 = 17.07 px di modello non
scalati, e una misura in px del modello vale px * 1.65 * 0.9375 / 16 blocchi: il telaio nudo e' alto 3.02 blocchi, la
tuta con i pezzi Rottame 3.09 (misure complete in `docs/arte/README.md`).

## Struttura

- `exo.py`: descrizione del modello (ossa, scatole, `sym()` per le coppie destra/sinistra), orientamento delle facce,
  impacchettamento nell'atlante, controlli, esportazione JSON e PNG. Non conosce nessun design.
- `paint.py`: pittura procedurale a pixel, aspetto metallico (spazzolatura, spigoli chiari verso la luce dipinta,
  occlusione nelle rientranze, bulloni, graffi, tinta di calore, grasso vicino ai mozzi) e usura degli stati visivi
  (`wear`, tabella `WEAR`); ogni texel conosce la sua posizione 3D, quindi il rumore prosegue da una faccia
  all'altra. `paint_model` restituisce atlante e maschera luminosa. L'elenco dei materiali e delle opzioni e' in testa
  al file.
- `designs/*.py`: un file per design. `build.py` li carica tutti in ordine di `ORDER` (poi di nome) e chiama
  `build(model)`. `frame.py` e' il telaio nudo (lo scheletro), `scrap.py` i sei pezzi del livello Rottame con le
  decalcomanie; tutti e due vengono dalla quarta passata della prova d'arte (vedi `docs/arte/README.md`).
- `build.py`: il punto d'ingresso.
- `preview/`: il visore (vedi sotto).

## Come un design dichiara ossa, scatole, materiali e regioni di colore

Coordinate in pixel del file (1 px = 1/16 di blocco), angolo minimo e massimo di ogni scatola, y in alto, il modello
poggia su y = 0, **il fronte guarda verso -z** e **il lato destro di chi indossa la tuta sta a x negativa**. Le
coordinate sono in pixel non scalati (il renderer moltiplica per `SUIT_SCALE`) e vanno in quarti di pixel; ogni
pixel prende `exo.DENSITY` = 2 texel, arrotondati in su (un lato di 0.25 px prende un texel).

```python
import exo

ORDER = 10

def build(m):
    # osso figlio di un osso di primo livello, con la regione dell'atlante
    m.bone('frameBody', 'armorBody', 'frame')
    # scatola: osso, nome unico, minimo, massimo, materiale (o {'north': 'vent', 'default': 'steel'}), opzioni
    m.box('frameBody', 'seat', (-3, 12.5, -2.75), (3, 15, 2.75), 'gun', bolts=True)
    # le dodici ossa di un pezzo: piece<Slot>_s<0-3>_<p1|p2|n>, regioni piece_s0..piece_s3
    m.piece_bones('Torso', 'armorBody')
    # scatola di un pezzo: si dichiara una volta nell'osso modello exo.piece(slot, regione) e finisce nei quattro
    # stati, con lo stesso seme
    m.box(exo.piece('Torso', 'p1'), 'chest_1', (-8.5, 16.25, -6.5), (8.5, 24.75, -6), 'paint')
    # coppia simmetrica: si scrive il lato destro (x <= 0) in un osso "Right", la copia va nell'osso "Left"
    m.sym(exo.piece('RightArm', 'p1'), 'elbow_cop', (-13.25, 15, -2.75), (-12.75, 19, 2.75), 'paint')
```

- **Ossa di primo livello** (fisse in `exo.TOP_BONES`, contratto con il renderer): `armorHead` e `armorBody` con
  perno (0,24,0), `armorRightArm` (-5,22,0), `armorLeftArm` (5,22,0), `armorRightLeg` (-2,12,0), `armorLeftLeg`
  (2,12,0). Ogni osso dichiarato con `m.bone` ne diventa figlio con lo stesso perno.
- **Stati visivi:** i controlli non confrontano due ossa con `state` diversi. Nello stesso pezzo non sono mai
  visibili insieme; pezzi di slot diversi in stati diversi invece si', ed e' innocuo solo perche' le quattro copie di
  un pezzo hanno le stesse scatole (lo verifica `ExoframeGeometryTest`); le dipinge `paint.wear` (sotto).
- **Regioni di colore:** `paint='p1'`/`'p2'` per le ossa da colorare (materiale `paint`, in toni di grigio),
  `'n'` per il metallo neutro. Il nome dell'osso porta la regione (`piece<Slot>_s<0-3>_p1`, `_p2`, `_n`, come
  `SuitBones.piece`). `piece_bones` dichiara anche le ossa senza scatole (oggi `_n` delle braccia e delle gambe e
  `_p2` delle gambe: dalla sesta passata il pezzo delle gambe e' tutto nella regione primaria, vedi
  `docs/arte/README.md`, "Regioni di colore"): il renderer le cerca tutte.
- **Decalcomanie:** un osso nella regione `decal` (`piece<Slot>_decal`, come `SuitBones.decal`) con una scatola piatta
  di materiale `decal`: i suoi texel restano trasparenti. Con `hide` si esporta solo la faccia esterna.
- **Materiali e opzioni:** in testa a `paint.py`. Un materiale nuovo si aggiunge in `paint_face`.
- **Tono:** l'opzione `tone=<fattore>` moltiplica il colore di base della scatola (prima di occlusione, calore e usura;
  non sui materiali luminosi, il cui colore va nella maschera luminosa):
  separa per luminosita' parti vicine dello stesso materiale o colore, come fanno i contorni in un disegno.
  `even=True` toglie alla scatola la sfumatura verticale e gli spigoli chiari e scuri: per una piastra fatta di piu'
  scatole impilate, che deve avere un tono solo.
- **Linee sottili:** l'opzione `lines=[('h', y, x0, x1), ('v', x, y0, y1), ...]` (coordinate del file) disegna linee
  scure spesse un texel fine sulle facce davanti e dietro della scatola, e da' a quelle due facce (`exo.FINE_FACES`,
  `Box.fine_for`) una griglia di texel quattro volte piu' fine (`fine`, predefinito 4 con `lines`: 8 texel per
  pixel); le altre facce restano alla densita' normale. Una linea fuori dalla scatola e' un errore; gli estremi di
  una linea che cadono su un bordo di texel non aggiungono un texel, cosi' la linea ha la stessa lunghezza davanti e
  dietro. La pittura di base
  di quelle facce resta quella normale, ogni texel ripetuto 4 x 4 (`paint.base_face`), e le linee si scuriscono di
  `paint.LINE_DARK` dopo l'usura, quindi restano scure in tutti gli stati. `paint.check_fine` (in `build.py`) costruisce
  lo stesso disegno con `Model(fine=False)` e verifica che ogni faccia fine sia esattamente quella faccia dipinta alla
  densita' normale e ingrandita, linee a parte, maschera luminosa compresa. Nell'atlante quelle facce occupano 16
  volte i texel: si usa solo sulle scatole che portano feritoie, tacche o giunture (oggi la parte bassa delle gambe,
  che riproduce l'immagine di riferimento del proprietario per sua decisione). Le misure di quella parte sono date come
  distanza |x| dal piano di mezzo: `designs/_legs.py` (`ax`, `leg`) le porta alla gamba destra e aggiunge la
  sinistra speculare.
- Ogni scatola ha un seme suo (dal nome; le copie di stato di un pezzo dal nome dichiarato, quindi lo stesso):
  aggiungere scatole non cambia il rumore di quelle esistenti. Occlusione, calore e grasso guardano le scatole
  vicine, ma il telaio guarda solo il telaio (`paint.looks_at`): aggiungere o cambiare un pezzo non cambia nessun
  texel del telaio, e il telaio nudo non ha ombre di pezzi assenti. Un pezzo guarda il telaio e i pezzi del suo stato.

## Stati visivi dei pezzi

Lo stato 0 e' la pittura metallica di sempre (sulla vernice: bordi consumati, scheggiature, colature sotto i
rivetti). Gli stati 1-3 aggiungono sopra, in `paint.wear`, l'usura descritta dalla tabella `paint.WEAR`, una
colonna per stato: scurimento generale, fuliggine a chiazze larghe (un rumore continuo su tutta la tuta, cosi' si
legge da lontano), scheggiature piu' larghe, ammaccature (bordo alto in ombra, basso illuminato), graffi lunghi (un
solco scuro con il labbro chiaro), crepe scure dallo stato 2 (larghe 2 texel nel 3), bruciato nello stato 3 (carbone
con isole di vernice), texel spenti di spie e vetri negli stati 2 e 3 (fuori dalla maschera luminosa). Le luci di un
pezzo rotto: circa meta' dei texel spenti, gli altri restano rossi e accesi (il bruciato non copre i materiali
luminosi), quindi un pezzo rotto ha meno luce di uno critico (144, 144, 128, 67 texel accesi negli stati 0-3) e
mai un texel nella maschera luminosa che non sia di una luce.

Ogni segno nasce da un rumore o da un `HashRng` con il seme della scatola e della faccia, che e' lo stesso nei quattro
stati; lo stato sceglie solo la soglia, la quota della lunghezza o l'intensita'. Le soglie scendono e le quote
crescono con lo stato, quindi le scheggiature e le ammaccature di uno stato contengono quelle del precedente e lo
stesso graffio si allunga: il pezzo sembra lo stesso oggetto che invecchia. `check_wear` controlla che la luminosita'
media cali sempre.

Le regioni `p1` e `p2` sono in grigio chiaro e il renderer le moltiplica per il colore del pezzo: li' l'usura puo'
essere solo piu' scura (vernice saltata, ammaccature, fuliggine, crepe, bruciato) o piu' chiara (il labbro dei
graffi), mai di un altro colore. Il metallo nudo dei graffi e la ruggine con i loro colori compaiono solo nella
regione neutra (acciaio, ottone e acciaio brunito delle ossa `_n`; le griglie scure si sporcano e basta, perche' la
ruggine le schiarirebbe). Il bruciato non schiarisce mai un texel gia' piu' scuro del carbone.

## Atlante

512x512, 2 texel per pixel di modello, regioni fisse (`exo.ATLAS_REGIONS`); ogni osso dice in quale va, e ogni
regione si impacchetta da sola (scaffali, faccia piu' alta prima, poi la piu' larga, poi l'ordine di
dichiarazione). Una faccia piu' larga della regione (oltre 256 px di modello) ferma l'impacchettatore con un errore.

| Regione | Righe | Contenuto |
|---|---|---|
| `frame` | 0..111 | telaio nudo (oggi ne usa 56) |
| `decal` | 112..127 | decalcomanie (texel trasparenti; oggi 7 righe) |
| `piece_s0` | 128..223 | i sei pezzi, stato 0 (integro; oggi 49 righe su 96, 47 libere, come gli altri stati) |
| `piece_s1` | 224..319 | stato 1 (danneggiato) |
| `piece_s2` | 320..415 | stato 2 (critico) |
| `piece_s3` | 416..511 | stato 3 (rotto) |

Aggiungere i pezzi non sposta i rettangoli del telaio. Le quattro regioni dei pezzi hanno la stessa forma: lo stesso
pezzo dichiarato con le stesse scatole nei quattro stati finisce nella stessa posizione relativa. Una copia
specchiata (`sym`) riusa i rettangoli della sorgente con `uv_size` negativo. Le righe usate le stampa `build.py`
(`righe usate: ...`).

### Aggiungere un livello

Il generatore oggi fa un livello solo (il Rottame), e chi aggiunge il secondo incontra tre limiti:

- i nomi delle ossa non portano il livello (`piece<Slot>_s<stato>_<regione>`, come `SuitBones` nel codice Java): un
  secondo design di pezzi si ferma in `piece_bones` con "osso duplicato". Servono nomi con il livello, nel generatore
  e in `SuitBones`, e il renderer che sceglie le ossa del livello del pezzo;
- un livello grande come il Rottame non ci sta: le regioni dei pezzi hanno 96 righe e il Rottame ne usa 49, quindi
  restano 47 righe per stato, meno di un secondo Rottame;
- le regioni (`exo.ATLAS_REGIONS`) e la grandezza dell'atlante, 512x512, sono fisse, e `ExoframeGeometryTest` (JUnit)
  controlla la stessa grandezza: un atlante piu' grande o un atlante per livello cambia anche il test e la texture
  che il renderer usa.

Prima del secondo livello va deciso come: un atlante e una geometria per livello, oppure un atlante piu' grande con
regioni per livello e stato.

## Orientamento delle facce

GeckoLib, caricando il file, nega x (`BakedModelFactory`): la faccia `east` di una scatola e' quella a **x minima**
del file, `west` quella a x massima (lo spike le aveva al contrario). Per ogni faccia `exo.face_frame` dice dove
cade l'angolo in alto a sinistra del rettangolo e in che direzione crescono u e v; e' ricavato da `VertexSet` e
`GeoQuad.build` di GeckoLib 4.x e confermato dal visore. Le rotazioni (non usate dal telaio) seguono la stessa
lettura: GeckoLib usa (-rx, -ry, rz) attorno al perno con x negata, applicate in ordine Z, Y, X.
Le facce `down` si scrivono con `uv_size` positivo, a differenza di Blockbench (e dell'esempio di GeckoLib) che usa
una v negativa: per GeckoLib e' giusto, ma importato in Blockbench e riesportato il sotto apparira' capovolto.

## Anteprime (solo locali, non nella CI)

Due visori con lo stesso `geo.js`: `shot.js` + `scene.html` per la tuta in scala come nel gioco, `render.js` +
`index.html` per un modello solo, senza scala (orientamento, pose, armatura d'esempio di GeckoLib). Il secondo
mostra tutte le ossa insieme, quindi con i pezzi le quattro copie di stato si sovrappongono: per i pezzi si usa il
primo.

### Viste in scala

```
cd tools/art/preview && npm install && node shot.js
```

rifa' le viste standard in `tools/art/preview/out/` (ignorata da git): `fronte`, `retro`, `lato` (3/4 davanti, 3/4
dietro, lato destro), `testa` e `gamba` (primi piani), `porta` (davanti, con griglia a blocchi di 17.07 px, porta
1x2 in bianco con stipiti e architrave, box di collisione in piedi 0.6S x 1.8S blocchi in giallo). La tuta e'
scalata di `exo.SUIT_SCALE` (letta da `exo.py`, oggi 1.65) attorno ai piedi, il pilota e' a grandezza normale, sollevato
di 15.6 px e senza gambe, la maschera luminosa fa da emissiva, le ossa `_p1`/`_p2` si tingono con i colori predefiniti
del Rottame (0x8A6F52, 0x4A4F55).
Come nel gioco si vedono solo le ossa dei pezzi di uno stato (predefinito 0), mai le decalcomanie. Opzioni:
`--views fronte,porta`, `--state 0..3` o `--state nudo` (telaio senza pezzi), `--colors RRGGBB,RRGGBB` (colori dei
pezzi), `--assets <dir>` per un modello scritto da `build.py --out <dir>` invece dei file del mod,
`--jobs <file.json>` per viste a piacere (`[{name, cfg, width, height}]`, `cfg` descritto in testa a
`scene.html`). Fuori dallo stato 0 con i colori predefiniti i nomi hanno un suffisso (`fronte-s2.png`,
`fronte-nudo.png`, `fronte-s0-3A6EA5-D9D9D9.png`). Gli screenshot sono 2000x2000 e `shot.js` si ferma se una PNG ha
blocchi oltre a IHDR, IDAT e IEND: le anteprime in `docs/arte/anteprime/rottame-v4/` sono copiate da qui cosi' come
sono (`telaio-<vista>` = `<vista>-nudo`, `armata-<vista>` = `<vista>`, `armata-fronte-stato<N>` = `fronte-s<N>`).

### Un modello solo

```
cd tools/art/preview && npm install && node render.js
```

scrive `tools/art/preview/out/exoframe_front.png`, `_back.png`, `_side.png` (ignorati da git). Opzioni:
`--views front,back,side,left,front_flat,back_flat,top,bottom`, `--mannequin 0|1` (giocatore di riferimento,
predefinito 1), `--pose 'armorRightArm:-45,0,0;armorHead:0,30,0'` (rotazioni aggiunte, in gradi, come le darebbe
un'animazione), `--fit suit|model`, `--geo <file> --texture <file>` per un altro modello, `--name <prefisso>`.
Per entrambi serve Playwright con il suo Chromium (WebGL via SwiftShader); `three` viene da npm. Nella sessione cloud
Playwright e' gia' installato globalmente con il suo browser (per questo non e' in `package.json`: una seconda copia
non troverebbe quel browser); altrove: `npm install playwright && npx playwright install chromium` in
`tools/art/preview`. Senza Playwright `render.js` e `shot.js` escono con un messaggio che lo dice.

Il visore (`preview/geo.js` + `index.html`) legge direttamente il `.geo.json` e l'atlante, senza file intermedi, e
li costruisce con la matematica di GeckoLib riportata riga per riga (x negata, perni, rotazioni Z-Y-X, UV per
faccia o box UV, `uv_size` negativo, flag `mirror`, `inflate`, `uv_rotation`, nessuna eliminazione delle facce
posteriori). Per le texture animate (strisce verticali di fotogrammi) usa il primo fotogramma.

Per controllare l'orientamento sul modello stesso: `python3 tools/art/build.py --out /tmp/dbg --debug-position` dipinge
ogni texel con il colore della sua posizione 3D e un punto bianco nell'angolo in alto a sinistra di ogni faccia;
nel visore (`node render.js --geo /tmp/dbg/geo/exoframe.geo.json --texture /tmp/dbg/textures/entity/exoframe.png
--name dbg --mannequin 0`) i colori devono proseguire senza salti su tutti gli spigoli.

### Verifica del visore con l'armatura d'esempio di GeckoLib

Da ripetere se si cambia `geo.js`. I file di riferimento restano fuori dal repository:

```
mkdir -p /tmp/gecko-ref && cd /tmp/gecko-ref
unzip -o -j ~/.gradle/caches/modules-2/files-2.1/software.bernie.geckolib/geckolib-forge-1.20.1/4.8.4/*/geckolib-forge-1.20.1-4.8.4.jar \
    assets/geckolib/geo/item/armor/gecko_armor.geo.json assets/geckolib/textures/item/armor/gecko_armor.png
cd <repo>/tools/art/preview
node render.js --geo /tmp/gecko-ref/gecko_armor.geo.json --texture /tmp/gecko-ref/gecko_armor.png \
    --name gecko --views front,back,side,top,bottom --mannequin 0 --fit model
```

Cosa deve mostrare (verificato il 13 novembre 2026): un costume da geco coerente, con la pancia chiara sul davanti,
il muso in avanti, gli occhi in cima alla testa con il riflesso bianco, la parte inferiore della coda e delle zampe
chiara (vista dal basso), le dita delle mani verso il basso, le dita dei piedi in avanti e aperte a ventaglio
(ossa ruotate di ±22.5 gradi attorno a y), la coda che scende verso terra (osso ruotato di -30 gradi attorno a x nel
file) e che si piega poco verso l'alto nei due segmenti successivi (+5 gradi). Texture dritte su tutte le facce,
sopra e sotto comprese, facce specchiate (`uv_size` negativo) specchiate. La texture d'esempio e' animata
(64x192, tre fotogrammi): se il visore la mostra stirata, ha perso la gestione dei fotogrammi.

## Stazione di manutenzione (parte 4d)

Genera la grafica del blocco stazione (design M4, §5.1) in una delle direzioni proposte al proprietario: i modelli di
blocco del gioco base delle due meta', le loro texture 16x16 (16 texel per lato di blocco) e l'icona piatta 16x16
dell'oggetto. Finche' il proprietario non sceglie, nel mod restano i segnaposto di `tools/gen_placeholders.py`: il
generatore scrive solo dove gli si dice.

    python3 tools/art/station.py --list        le direzioni: nome, lettera, titolo, idea
    python3 tools/art/station.py --direction <officina|rame|pistoni> --out <dir>
                                               scrive <dir>/models/block/station_main.json e station_side.json,
                                               <dir>/textures/block/station_top, station_side, station_front_main,
                                               station_front_side, station_back_main, station_back_side,
                                               station_bottom (.png) e <dir>/textures/item/station.png

`--out` e' obbligatorio. L'integrazione (parte 4d, dopo la risposta) sara'
`--out src/main/resources/assets/redstone_exoframes` con la direzione scelta: il blockstate `station.json` e il
modello dell'oggetto `item/station.json` di oggi restano validi (stessi nomi). Restano da fare allora: togliere la
stazione da `tools/gen_placeholders.py` (che oggi scrive `station_top`, `station_side`, `station_front_*` e
`item/station.png`, e che `check_art.sh` confronta), mettere il nuovo generatore in `check_art.sh`.

Stesse regole del generatore della tuta: stesso input, stessi byte (semi dai nomi, `paint.HashRng`, solo interi),
JSON con `exo.dumps_json`, PNG con `exo.write_png` (solo IHDR, IDAT, IEND, zlib 9), numpy di `requirements.txt`.

**Struttura.**
- `bench.py`: le due meta' descritte insieme come un banco di 32 x 16 x 16 pixel visto da chi sta davanti (la
  principale a sinistra, x 0..16, la laterale a destra, come le posa il gioco), una griglia di voxel piena in cui
  un design scava tasche (`cut`) e rialza parti (`fill`) da una faccia, e dipinge le tele delle facce. Le facce
  scoperte dei voxel diventano elementi piatti con una faccia sola, uniti in rettangoli: nessuna sovrapposizione
  complanare per costruzione. UV: il fondo delle tasche e i piani esterni prendono la proiezione piana della tela
  (come le UV predefinite del gioco base); le pareti delle tasche prendono i texel della parte piena accanto, piegati
  attorno allo spigolo (con `rotation` 90 quando gli assi della faccia lo chiedono). `cullface` solo sul bordo del
  blocco; le facce della giuntura fra le due meta' sono disegnate senza (una meta' da sola resta chiusa). Sopra e gli
  estremi sono simmetrici: si dipingono per la principale e la laterale li prende specchiati (`station_top` e
  `station_side` con le UV rovesciate). `check()` ferma il generatore se una tasca si apre verso un'altra faccia
  esterna (terra, piano, un altro lato) o tocca una tasca di un'altra faccia: il blocco e' un cubo pieno che nasconde
  le facce dei vicini, e una tasca aperta cosi' lascerebbe vedere attraverso il mondo. Provato a mano (27 novembre
  2026) con tre difetti iniettati in memoria (una tasca del fronte fino a terra, una fino al piano, una che tocca una
  tasca del piano: segnalate; una tasca chiusa: nessuna segnalazione); la prova negativa in `selftest.py`, che questo
  file chiede per ogni controllo nuovo, si aggiunge con l'integrazione, quando `station.py` entra in `check_art.sh`.
- `pixel.py`: tele a palette (un materiale e un gradino della sua scala per texel: luci e ombre a gradini, senza
  sfumature), rumore, spazzolatura, rivetti, e `relief`, che illumina e ombreggia i bordi delle tasche dalla mappa
  delle profondita' (luce dall'alto a sinistra).
- `station_designs/*.py`: una direzione per file (`build(bench)`, `icon()`, lettera, titolo e idea per le
  anteprime), nell'ordine di `station_designs.DIRECTIONS`.
- `station.py`: il punto d'ingresso.

**Luce nel gioco.** La stazione ha la forma di collisione piena, quindi il gioco prende la luce di ogni faccia, anche
rientrata, nel blocco accanto nella direzione della faccia (`ModelBlockRenderer.calculateShape`, bit 0, e
`renderModelFaceFlat`, letti con javap su 1.20.1 e 1.21.1). Le pareti delle tasche rivolte verso terra o verso
l'altra meta' prendono la luce di un blocco opaco: nere con la luce liscia spenta, come nelle anteprime. Con la luce
liscia accesa (il predefinito) dovrebbero restare piu' scure delle altre ma sfumate: e' una deduzione, non letta nel
bytecode ne' vista nel client.

**Anteprime** (solo locali, non nella CI; Playwright come sopra):

    cd tools/art/preview && node station.js    tutte le direzioni in docs/superpowers/specs/anteprime/2026-11-27-m4d/
    node station.js --directions rame --out <dir>
    node station.js --check 1 --vanilla <dir>  controllo del visore sulla stazione segnaposto di oggi (sotto)

`station.js` esegue `station.py` per ogni direzione in `out/stazione/gen/<nome>` e disegna con `scene.html` da quei
file e dal blockstate del mod: la principale con facing=north e la laterale a ovest, alla destra di chi guarda il
fronte, il telaio vero (Rottame, stato 0, senza pilota e senza strato luminoso) in piedi al centro della principale
come lo posa l'oggetto, rivolto a nord. I blocchi passano da `blockmodel.js`, che legge i modelli del gioco base come
`FaceBakery` (FaceInfo, UV per vertice con la rotazione, UV predefinite, rotazione y del blockstate) e da' a ogni
faccia il colore del texel per l'ombra della direzione (sopra 1.0, nord e sud 0.8, est e ovest 0.6, sotto 0.5) e per
la luce del blocco accanto (1.0, o circa 0.1 dentro un blocco opaco), senza gestione del colore, texel senza filtri,
senza antialiasing. Opzioni nuove di `scene.html` (le viste di `shot.js` non cambiano): `blocks`, `assets`,
`vanilla`, `dy` dei modelli, `antialias`, `background`, `floor.color`. Scrive `<L>-<nome>-mondo.png`,
`<L>-<nome>-texture.png` e `confronto.png`, e si ferma se una PNG ha blocchi oltre a IHDR, IDAT, IEND.

Il controllo `--check` disegna la stazione segnaposto di oggi (modelli del mod con il genitore `orientable` del gioco
base, i cui JSON `orientable`, `orientable_with_bottom`, `cube` e `block` si estraggono a parte dal jar del client
in `<dir>/assets/minecraft/models/block/`, fuori dal repository) in `out/stazione/controllo.png`; confrontata con
`docs/test-in-gioco/m4a1-screenshot/1.20.1-forge-stazione-posata.png` (27 novembre 2026): la spia rossa a sinistra
sul fronte della principale, la griglia sulla laterale a destra, le guide del piano da sinistra a destra, i bordi
chiari del piano davanti e a destra e quelli del fronte in alto e a sinistra, come nel gioco. Quello scatto mostra
solo il fronte e il piano di un cubo con le UV predefinite; il resto delle regole che `bench.py` e `blockmodel.js`
seguono e' letto con javap, uguale su 1.20.1 e 1.21.1: l'ordine dei vertici di `FaceInfo` nelle sei direzioni,
`BlockFaceUV.getU/getV` con l'indice spostato dalla rotazione, le UV predefinite di `BlockElement.uvsByFace` e il
verso della rotazione del blockstate (`BlockModelRotation`: `rotateYXZ(-y, -x, 0)`, quindi y 90 porta il nord a
est). Il client vero non ha ancora disegnato gli elementi piatti a una faccia ne' le facce con `rotation` 90.
