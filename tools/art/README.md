# Generatore della grafica

Genera da script la grafica del telaio: geometria GeckoLib, atlante e animazioni. Direzione artistica e scelte in
`docs/arte/README.md`; convenzioni delle ossa in `docs/superpowers/specs/2026-11-13-m3-telaio-design.md`, §5.1.

## Comandi

Dalla radice del repository (serve Python 3 con numpy, versione in `tools/art/requirements.txt`:
`python3 -m pip install -r tools/art/requirements.txt`; Pillow serve solo per ridimensionare le anteprime):

    python3 tools/art/build.py                 rigenera i tre file in src/main/resources/assets/redstone_exoframes/
    python3 tools/art/build.py --out <dir>     li scrive in <dir>/geo, <dir>/textures/entity, <dir>/animations
    bash tools/check_art.sh                    rigenera in una cartella temporanea e confronta: "OK art"

File generati (uguali sulle quattro varianti):

- `geo/exoframe.geo.json`: geometria Bedrock `format_version` 1.12.0, identificatore `geometry.exoframe`;
- `textures/entity/exoframe.png`: atlante 256x256, PNG senza metadati (solo IHDR, IDAT, IEND);
- `animations/exoframe.animation.json`: `{"format_version": "1.8.0", "animations": {}}`.

Stesso input, stessi byte: semi ricavati dai nomi delle scatole, nessuna data, ordine stabile di ossa, scatole e
rettangoli, JSON con chiavi in ordine fisso e numeri interi quando possibile, PNG compresse con lo zlib di Python
(livello 9). La pittura usa solo aritmetica intera per le scelte casuali (`paint.HashRng`, niente generatore di
numpy, il cui flusso puo' cambiare tra versioni); numpy resta comunque fissato in `requirements.txt`.
`check_art.sh` confronta byte per byte i due JSON; per l'atlante confronta l'elenco dei blocchi (solo IHDR, IDAT,
IEND), IHDR e i pixel decompressi (`tools/art/compare_png.py`), perche' i byte compressi cambiano con la build di
zlib (per esempio zlib-ng) anche a pixel identici.

`build.py` si ferma (uscita 1) se il modello non passa i controlli di `exo.Model.check()`:

- facce complanari con la stessa normale che si sovrappongono, in tutte e sei le direzioni (anche verso il basso),
  salvo la parte coperta da altre scatole;
- scatole a meno di 0.25 px dal corpo e dagli arti del giocatore di riferimento (dove sta il secondo strato della
  skin) o a meno di 0.5 px dalla testa (dove sta il cappello);
- scatole fuori dall'ingombro x -13..13, y -1..36, z -8..9.

Le facce interamente sepolte in altre scatole dello stesso osso non vengono esportate (niente spazio sprecato
nell'atlante). `ExoframeGeometryTest` (JUnit) legge il file generato e controlla nomi e perni delle ossa, ingombro
e UV.

## Struttura

- `exo.py`: descrizione del modello (ossa, scatole, `sym()` per le coppie destra/sinistra), orientamento delle facce,
  impacchettamento nell'atlante, controlli, esportazione JSON e PNG. Non conosce nessun design.
- `paint.py`: pittura procedurale a pixel; ogni texel conosce la sua posizione 3D, quindi rumore e ruggine
  proseguono da una faccia all'altra. L'elenco dei materiali e delle opzioni e' in testa al file.
- `designs/*.py`: un file per design. `build.py` li carica tutti in ordine di `ORDER` (poi di nome) e chiama
  `build(model)`. Oggi c'e' solo `frame.py`, il telaio nudo.
- `build.py`: il punto d'ingresso.
- `preview/`: il visore (vedi sotto).

## Come un design dichiara ossa, scatole, materiali e regioni di colore

Coordinate in pixel del file (1 px = 1/16 di blocco), angolo minimo e massimo di ogni scatola, y in alto, il modello
poggia su y = 0, **il fronte guarda verso -z** e **il lato destro di chi indossa la tuta sta a x negativa**. Le
coordinate vanno in quarti di pixel; i lati frazionari prendono un texel intero.

```python
ORDER = 10

def build(m):
    # osso figlio di un osso di primo livello, con la regione dell'atlante; state (0-3) e paint ('p1', 'p2', 'n')
    # servono al pittore e ai controlli
    m.bone('pieceBody_s0_p1', 'armorBody', 'piece_s0', state=0, paint='p1')
    # scatola: osso, nome unico, minimo, massimo, materiale (o {'north': 'decal', 'default': 'steel'}), opzioni
    m.box('pieceBody_s0_p1', 'chest', (-4.5, 16, -4), (4.5, 24, -3), 'paint', rivets=True)
    # coppia simmetrica: si scrive il lato destro (x <= 0) in un osso "Right", la copia va nell'osso "Left"
    m.sym('pieceRightArm_s0_n', 'cuff', (-9, 12, -3), (-4.25, 14, 3), 'steel')
```

- **Ossa di primo livello** (fisse in `exo.TOP_BONES`, contratto con il renderer): `armorHead` e `armorBody` con
  perno (0,24,0), `armorRightArm` (-5,22,0), `armorLeftArm` (5,22,0), `armorRightLeg` (-2,12,0), `armorLeftLeg`
  (2,12,0). Ogni osso dichiarato con `m.bone` ne diventa figlio con lo stesso perno.
- **Stati visivi:** due ossa con `state` diversi non sono mai visibili insieme, quindi i controlli non le
  confrontano. Il pittore aumenta usura, ruggine e scheggiature con lo stato.
- **Regioni di colore:** `paint='p1'`/`'p2'` per le ossa da colorare (materiale `paint`, in toni di grigio),
  `'n'` per il metallo neutro. Il nome dell'osso porta la regione (`piece<Parte>_s<0-3>_p1`, `_p2`, `_n`).
- **Materiali e opzioni:** in testa a `paint.py`. Un materiale nuovo si aggiunge in `paint_face`.
- Ogni scatola ha un seme suo (dal nome): aggiungere scatole non cambia i pixel di quelle esistenti.

## Atlante

256x256, regioni fisse (`exo.ATLAS_REGIONS`); ogni osso dice in quale va, e ogni regione si impacchetta da sola
(scaffali, faccia piu' alta prima, poi la piu' larga, poi l'ordine di dichiarazione):

| Regione | Righe | Contenuto |
|---|---|---|
| `frame` | 0..31 | telaio nudo (oggi ne usa 13) |
| `piece_s0` | 32..87 | i sei pezzi, stato 0 (integro) |
| `piece_s1` | 88..143 | stato 1 (danneggiato) |
| `piece_s2` | 144..199 | stato 2 (critico) |
| `piece_s3` | 200..255 | stato 3 (rotto) |

Aggiungere i pezzi non sposta i rettangoli del telaio. Le quattro regioni dei pezzi hanno la stessa forma: lo stesso
pezzo dichiarato con le stesse scatole nei quattro stati finisce nella stessa posizione relativa. Una copia
specchiata (`sym`) riusa i rettangoli della sorgente con `uv_size` negativo.

## Orientamento delle facce

GeckoLib, caricando il file, nega x (`BakedModelFactory`): la faccia `east` di una scatola e' quella a **x minima**
del file, `west` quella a x massima (lo spike le aveva al contrario). Per ogni faccia `exo.face_frame` dice dove
cade l'angolo in alto a sinistra del rettangolo e in che direzione crescono u e v; e' ricavato da `VertexSet` e
`GeoQuad.build` di GeckoLib 4.x e confermato dal visore. Le rotazioni (non usate dal telaio) seguono la stessa
lettura: GeckoLib usa (-rx, -ry, rz) attorno al perno con x negata, applicate in ordine Z, Y, X.
Le facce `down` si scrivono con `uv_size` positivo, a differenza di Blockbench (e dell'esempio di GeckoLib) che usa
una v negativa: per GeckoLib e' giusto, ma importato in Blockbench e riesportato il sotto apparira' capovolto.

## Anteprime (solo locali, non nella CI)

```
cd tools/art/preview && npm install && node render.js
```

scrive `tools/art/preview/out/exoframe_front.png`, `_back.png`, `_side.png` (ignorati da git). Opzioni:
`--views front,back,side,left,front_flat,back_flat,top,bottom`, `--mannequin 0|1` (giocatore di riferimento,
predefinito 1), `--pose 'armorRightArm:-45,0,0;armorHead:0,30,0'` (rotazioni aggiunte, in gradi, come le darebbe
un'animazione), `--fit suit|model`, `--geo <file> --texture <file>` per un altro modello, `--name <prefisso>`.
Serve Playwright con il suo Chromium (WebGL via SwiftShader); `three` viene da npm. Nella sessione cloud
Playwright e' gia' installato globalmente con il suo browser (per questo non e' in `package.json`: una seconda copia
non troverebbe quel browser); altrove: `npm install playwright && npx playwright install chromium` in
`tools/art/preview`. Senza Playwright `render.js` esce con un messaggio che lo dice.

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
