# Task 4 — La vignetta del casco: rapporto dell'implementatore

Stato: DONE_WITH_CONCERNS (commit 7dcdb1e; i dubbi in fondo, "Problemi e dubbi").

BASE: 5f5d252 (ramo m4-stazione). Cartella di lavoro (scatti, log, uscite):
<cartella temporanea>/m4c/t4/ (sotto, `scratch/`).

## Progress

- [x] Orientamento: brief, contesto-comune.md, design §5.12 "Vignetta" e §10, `draw_vignette` di anteprime.py, codice
  e rapporto del Task 3.
- [x] Step 1: generatore e texture. `python3 -I tools/gen_placeholders.py` → scrive 24 file, git vede solo la texture
  nuova (le altre 23 identiche); `convert ... info:` → `144 80 107 107 0`; `bash tools/check_art.sh` → `OK art`.
  Controprova: scratch/crosscheck_vignette.py disegna `draw_vignette` delle anteprime su una tela trasparente a
  480x270 e 320x240 e confronta i quattro angoli con la texture → 0 pixel diversi (7596 pixel con alfa > 0).
- [x] R5: alfa massima 1/255 (0,0039) in tutti e due gli angoli in basso (sotto, "R5").
- [x] Step 2: `SuitHud` (VIGNETTE_TEXTURE, drawVignette con la fusione e il finally) → build delle quattro varianti
  BUILD SUCCESSFUL (scratch/build-step2.log), 381 JUnit su ciascuna, nessuna suite con fallimenti; la texture e'
  nei jar. javap: su 1.20.1 e 1.21.1 l'unico `blit` pubblico a 9 argomenti con la ResourceLocation e'
  `(RL, int, int, float, float, int, int, int, int)`; `enableBlend`, `defaultBlendFunc`, `disableBlend`,
  `CameraType.isFirstPerson` ci sono sulle due.
- [x] Step 3: `m4c_shots.sh` (controllo `vignetta`, fase "effetti"); prove a secco (sotto); 1.21.1-neoforge 17 ok;
  `VIGNETTE_MIN=320`; `M4C_SOLO_CONTROLLI=1` 17 ok; 1.20.1-forge 17 ok.
- [x] Step 4: m3d su 1.21.1-neoforge e 1.20.1-forge, 14 ok ciascuna; valori cambiati scritti.
- [x] Step 5: documenti (tools/README.md, tools/client/README.md, docs/compat.md); `grep -c hud_vignette
  tools/README.md` → 1.
- [x] Step 6: verifiche di fine task, tutte ok (scratch/final-checks.log; sotto).
- [x] Step 7: commit 7dcdb1e, firmato (intestazione gpgsig), con le due righe finali; albero pulito; non spinto.
- [x] Autorevisione (sotto).

## Che cosa ho fatto

- `tools/gen_placeholders.py`: `GUI_DIR` (`textures/gui` negli asset della mod), `import math`, le costanti
  `VIGNETTE_W`/`VIGNETTE_H` (72, 40), `VIGNETTE_TINT` (10, 12, 14), `VIGNETTE_MAX_ALPHA` 0,42, `VIGNETTE_GLINTS`
  ((0,16, 10), (0,10, 18)) e `draw_hud_vignette()`: la texture 144x80 con i quattro quarti al loro posto; in ogni
  quarto, con (cx, cy) il pixel d'angolo esterno ((0, 0), (143, 0), (0, 79), (143, 79)),
  `dist = sqrt(((x - cx)/72)^2 + ((y - cy)/40)^2)`, `a = 0.42 * max(0, 1 - dist)**2`, colore
  `(10, 12, 14, int(a * 255))` su ogni pixel del quarto (il brief; le anteprime lasciano (0, 0, 0, 0) dove a = 0: la
  differenza e' solo nel colore di pixel del tutto trasparenti, invisibile); poi i due riflessi, che sostituiscono il
  pixel, in (8 + off + t, 2 + t // 2) e (135 - off - t, 2 + t // 2), `(255, 255, 255, int(k * 255))`. Le operazioni in
  virgola mobile sono nello stesso ordine delle anteprime, quindi gli stessi valori al bit. In `outputs()` la riga
  `(os.path.join(GUI_DIR, "hud_vignette.png"), 144, 80, draw_hud_vignette)`; nella docstring la vignetta fra i
  segnaposto. La texture generata: `src/main/resources/assets/redstone_exoframes/textures/gui/hud_vignette.png`.
- `client/SuitHud.java`:
  - `static final ResourceLocation VIGNETTE_TEXTURE = Ids.of("textures/gui/hud_vignette.png")`, e le due misure della
    texture (`2 * VIGNETTE_W`, `2 * VIGNETTE_H`) come costanti private.
  - In `render`, dopo i controlli che non disegnano nulla (NONE, F1, H, nessun giocatore, spettatore) e per prima:
    con `FULL`, `minecraft.options.getCameraType().isFirstPerson()` e `ClientPrefs.vignette()`, `drawVignette`.
  - `drawVignette`: `RenderSystem.enableBlend()`, `RenderSystem.defaultBlendFunc()`, poi in un `try` i quattro
    `graphics.blit(VIGNETTE_TEXTURE, angolo.x0(), angolo.y0(), u, v, 72, 40, 144, 80)` nell'ordine di
    `vignetteCorners`, con (u, v) = (i % 2 · 72, i / 2 · 40), cioe' (0, 0), (72, 0), (0, 40), (72, 40); nel
    `finally` `RenderSystem.disableBlend()`. Il `try` esterno di `render` resta: un'eccezione in un `blit` spegne la
    fusione e poi scrive l'ERROR della sessione.
  - Javadoc: la classe (la vignetta, `blit`, segnaposto, la fusione spenta comunque), `render` (l'ordine), e
    `drawVignette` con la differenza fra le versioni (design §10), corretta per gli avvisi (vedi "Problemi", 1).
- `tools/client/m4c_shots.sh`: la fase 11 "effetti" (`effect give $u minecraft:luck 60 0`, `@attendi 4`,
  `@scatto vignetta-effetti`, `effect clear $u`) fra la terza persona e la fine, lo scatto in `shots_a`; le costanti
  `SCREEN_W`/`SCREEN_H` e `CORNER_BOTTOM_LEFT`; la soglia `VIGNETTE_MIN`; l'aiuto `from_corner` (abbastanza pixel e il
  riquadro sui due bordi dello schermo dell'angolo); il controllo `vignetta` dopo `terza-persona`, come nella tabella
  del piano; i testi `want` di `solo-barra`, `terza-persona` e `riavvio`; il commento di testa: che cosa provano ora
  `solo-barra`, `terza-persona`, `mancino-solo-barra` (la vignetta accesa ma assente senza elmo e in terza persona) e
  `riavvio` (la vignetta spenta ricordata dopo il riavvio: `riavvio-visibile` contro `riavvio-nascosto` ha il riquadro
  del gruppo, senza angoli), la fase "effetti", il controllo `vignetta`, il "Non prova" e le durate.
- `tools/client/m3d_shots.sh`: nel commento di testa la vignetta negli scatti con l'elmo in prima persona, uguale nei
  due scatti di ogni coppia, la nota su `solo-tuta` del brief con la misura, i valori cambiati (mappa a sinistra,
  terza persona, durate).
- `tools/README.md`, `tools/client/README.md`, `docs/compat.md`: Step 5 (sotto, "File cambiati").

## R5

A 320x240 la fascia della barra va da x 65 a 255 (esclusa) e gli angoli in basso da x 0 a 72 e da 248 a 320, per
tutta la loro altezza (y 200-240 dentro 190-240). Le colonne sovrapposte sono x 65-71 del quarto in basso a sinistra
della texture e x 72-78 di quello in basso a destra (lo specchio), righe 40-79. Comandi (dalla radice del repository):

    T=src/main/resources/assets/redstone_exoframes/textures/gui/hud_vignette.png
    convert "$T" -alpha extract -crop 7x40+65+40 +repage -format '%[fx:int(255*maxima+0.5)]' info:   → 1
    convert "$T" -alpha extract -crop 7x40+72+40 +repage -format '%[fx:int(255*maxima+0.5)]' info:   → 1

e la stessa misura dai byte della PNG decompressi con Python (zlib), che dice anche dove:

    basso a sinistra, x 65-71: alfa massima 1/255 = 0.0039 nel pixel (65, 79)
    basso a destra, x 72-78: alfa massima 1/255 = 0.0039 nel pixel (78, 79)

Massimo 0,0039, sotto lo 0,05 (13/255) di R5: si disegna. Dalla formula: nel pixel (65, 79) d = 65/72 = 0,903,
0,42 · (1 − 0,903)² = 0,00397, int(0,00397 · 255) = 1.

## Prove e risultati

### Prove a secco del controllo `vignetta` (prima del client vero)

Su una copia degli scatti neoforge del Task 3 (scratch/dry-t3, con `vignetta-effetti.png` finto), con
`M4C_SOLO_CONTROLLI=1`:
- `pieno` del Task 3 (senza vignetta): `KO vignetta: schermo pixel=0 ...` e gli altri 16 `ok`, uscita 1;
- `pieno` sintetico = la texture x2 incollata ai quattro angoli di `senza-vignetta` (scratch/synth-good.png):
  `ok vignetta: schermo pixel=12875 ..., somma degli angoli 12875; angoli 98x56+0+0; 98x56+862+0; 92x52+0+488;
  80x46+880+494`, uscita 0;
- l'angolo in alto a sinistra incollato non specchiato in alto a destra (scratch/synth-bad.png): `KO`, riquadro
  `98x56+816+0` (X + L = 914, non 960);
- 100 pixel in piu' al centro (scratch/synth-extra.png): `KO`, schermo 12975 contro la somma 12875.

### m4c_shots.sh

`EXO_ACCEPT_EULA=true bash tools/client/m4c_shots.sh 1.21.1-neoforge scratch/neo` → uscita 0, 5m56s (A 147 s, B
93 s, C 80 s), con `VIGNETTE_MIN` ancora provvisoria a 10 (scratch/neo.out). Poi `VIGNETTE_MIN=320` e
`M4C_SOLO_CONTROLLI=1 bash tools/client/m4c_shots.sh 1.21.1-neoforge scratch/neo` → 17 ok, uscita 0
(scratch/neo-recheck.out). Le righe:

```
ok chat-e-tasti: True False
ok riavvio-file: False False
ok gruppo: pixel=12320 riquadro=176x70+4+466; sagoma pixel=2940 riquadro=42x70+4+466
ok testo: colore=244 riquadro=46x14+50+482
ok barra-normale: colore=1220 riquadro=122x10+52+510
ok sagoma: pixel=1024 riquadro=32x60+10+472; verde prima colore=424 riquadro=28x56+12+474; giallo dopo colore=424 riquadro=28x56+12+474; verde dopo colore=0 riquadro=0x0+0+0
ok critico: rosso colore=424 riquadro=28x56+12+474; giallo colore=0 riquadro=0x0+0+0
ok barra-bassa: pixel=2052 riquadro=124x38+50+482; rosso nella barra colore=60 riquadro=6x10+52+510; verde nella barra colore=0 riquadro=0x0+0+0; rosso nella sagoma colore=0 riquadro=0x0+0+0
ok barra-esaurita: verde nella barra colore=0 riquadro=0x0+0+0; rosso colore=0 riquadro=0x0+0+0; grigio colore=248 riquadro=42x16+50+482
ok scorta: pixel=228 riquadro=24x16+152+482; grigio prima colore=112 riquadro=22x14+152+482; bianco dopo colore=84 riquadro=22x14+152+482
ok solo-barra: pixel=9380 riquadro=134x70+46+466; sagoma pixel=0 riquadro=0x0+0+0
ok terza-persona: pixel=12320 riquadro=176x70+4+466; angoli pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0
ok vignetta: schermo pixel=11624 riquadro=960x540+0+0, somma degli angoli 11624; angoli pixel=4260 riquadro=98x56+0+0; pixel=4260 riquadro=98x56+862+0; pixel=653 riquadro=70x44+0+496; pixel=2451 riquadro=82x46+878+494
ok mancino-gruppo: pixel=12320 riquadro=176x70+780+466; sagoma pixel=2940 riquadro=42x70+914+466; percentuale colore=244 riquadro=46x14+784+482
ok mancino-sagoma: pixel=1024 riquadro=32x60+920+472; giallo colore=424 riquadro=28x56+922+474
ok mancino-solo-barra: pixel=9380 riquadro=134x70+780+466; sagoma pixel=0 riquadro=0x0+0+0
ok riavvio: pixel=0 riquadro=0x0+0+0; pixel=12320 riquadro=176x70+4+466
OK 1.21.1-neoforge: scatti in .../scratch/m4c/t4/neo
```

`VIGNETTE_MIN` = 320, accanto i valori misurati: 4260 (alto a sinistra), 4260 (alto a destra), **653** (basso a
sinistra, il piu' piccolo: il gruppo sta sopra la vignetta, e l'erba e' scura), 2451 (basso a destra, erba e braccio
della tuta); 653 / 2 = 326,5, per difetto alla decina 320. Ogni riquadro sui due bordi del suo angolo dello schermo
(alto a sinistra X 0, Y 0; alto a destra 862 + 98 = 960, Y 0; basso a sinistra X 0, 496 + 44 = 540; basso a destra
878 + 82 = 960, 494 + 46 = 540); lo schermo intero 11624 = 4260 + 4260 + 653 + 2451.

`EXO_ACCEPT_EULA=true bash tools/client/m4c_shots.sh 1.20.1-forge scratch/forge` → 17 ok, uscita 0, 6m03s (A 153 s,
B 94 s, C 81 s), con `VIGNETTE_MIN=320` (scratch/forge.out). Le righe:

```
ok chat-e-tasti: True False
ok riavvio-file: False False
ok gruppo: pixel=12320 riquadro=176x70+4+466; sagoma pixel=2940 riquadro=42x70+4+466
ok testo: colore=244 riquadro=46x14+50+482
ok barra-normale: colore=1220 riquadro=122x10+52+510
ok sagoma: pixel=1024 riquadro=32x60+10+472; verde prima colore=424 riquadro=28x56+12+474; giallo dopo colore=424 riquadro=28x56+12+474; verde dopo colore=0 riquadro=0x0+0+0
ok critico: rosso colore=424 riquadro=28x56+12+474; giallo colore=0 riquadro=0x0+0+0
ok barra-bassa: pixel=1992 riquadro=124x38+50+482; rosso nella barra colore=60 riquadro=6x10+52+510; verde nella barra colore=0 riquadro=0x0+0+0; rosso nella sagoma colore=0 riquadro=0x0+0+0
ok barra-esaurita: verde nella barra colore=0 riquadro=0x0+0+0; rosso colore=0 riquadro=0x0+0+0; grigio colore=248 riquadro=42x16+50+482
ok scorta: pixel=228 riquadro=24x16+152+482; grigio prima colore=112 riquadro=22x14+152+482; bianco dopo colore=84 riquadro=22x14+152+482
ok solo-barra: pixel=9380 riquadro=134x70+46+466; sagoma pixel=0 riquadro=0x0+0+0
ok terza-persona: pixel=12320 riquadro=176x70+4+466; angoli pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0
ok vignetta: schermo pixel=11649 riquadro=960x540+0+0, somma degli angoli 11649; angoli pixel=4260 riquadro=98x56+0+0; pixel=4260 riquadro=98x56+862+0; pixel=654 riquadro=70x44+0+496; pixel=2475 riquadro=82x46+878+494
ok mancino-gruppo: pixel=12320 riquadro=176x70+780+466; sagoma pixel=2940 riquadro=42x70+914+466; percentuale colore=244 riquadro=46x14+784+482
ok mancino-sagoma: pixel=1024 riquadro=32x60+920+472; giallo colore=424 riquadro=28x56+922+474
ok mancino-solo-barra: pixel=9380 riquadro=134x70+780+466; sagoma pixel=0 riquadro=0x0+0+0
ok riavvio: pixel=0 riquadro=0x0+0+0; pixel=12320 riquadro=176x70+4+466
OK 1.20.1-forge: scatti in .../scratch/m4c/t4/forge
```

(`barra-bassa` 1992: lo scatto cade nel mezzo secondo spento dell'avviso, come nel Task 3.) Gli angoli in basso
differiscono di poco fra le versioni perche' lo sfondo differisce gia' senza la vignetta: confronto esatto dei
`senza-vignetta` delle due versioni, angolo per angolo (pixel diversi, scarto massimo): alto a sinistra (613, 1/255),
alto a destra (534, 1/255), basso a sinistra (92, 26/255: l'erba), basso a destra (5332, 2/255: il braccio); con la
vignetta in `pieno` i due angoli in alto diventano uguali al pixel (0, 0), e in basso qualche pixel passa la soglia del
4 % su una sola versione. Il disegno della vignetta e' lo stesso sulle due.

In tutte e sei le cartelle (neo, neo/mancino, neo/riavvio, forge, forge/mancino, forge/riavvio) `log-check.txt` e'
vuoto (0 byte); nei client.log nessuna riga `Suit HUD` e nessuna `Failed to load options`.

Prove in piu' sugli scatti neoforge (`arm_measure.sh differenza`):
- `riavvio/riavvio-visibile` contro `senza-vignetta` della A, schermo intero: `pixel=0` (il client riavviato mostra
  esattamente l'HUD della A con la vignetta spenta); `nascosto` contro `riavvio/riavvio-nascosto`: `pixel=0`;
- gli scatti del Task 3 (senza il codice della vignetta) contro i miei, schermo intero: `senza-vignetta`, `nascosto`,
  `danneggiato`, `bassa`, `esaurita`, `scorta`, `senza-elmo` → `pixel=0` ciascuno: dove la vignetta non va, il codice
  nuovo non cambia nulla, `senza-elmo` compreso (la preferenza e' accesa, il livello e' `BAR_ONLY`).

### m3d_shots.sh

`EXO_ACCEPT_EULA=true bash tools/client/m3d_shots.sh 1.21.1-neoforge scratch/m3d-neo` → 14 ok, uscita 0, 3m24s (A
111 s, B 75 s); `... 1.20.1-forge scratch/m3d-forge` → 14 ok, uscita 0, 3m31s (A 113 s, B 79 s); log-check.txt vuoti
nelle quattro cartelle. neoforge:

```
ok controllo: pixel=18075 copertura=3.49 mirino=0 barra=0 hud=0 riquadro=161x149+677+391
ok dipinto: magenta=39962 riquadro=286x188+674+352; meta' sinistra magenta=0 riquadro=0x0+0+0
ok criteri: pixel=45442 copertura=8.77 mirino=0 barra=0 hud=0 riquadro=286x205+674+335
ok solo-tuta: sagoma=18075 uguali=0
ok nudo: magenta=0 riquadro=0x0+0+0; pixel=40026 copertura=7.72 mirino=0 barra=0 hud=0 riquadro=260x205+700+335; sagoma=18075 uguali=0
ok bastone: magenta=0 riquadro=0x0+0+0
ok mappa: sinistra magenta=23629 riquadro=365x179+0+361; destra magenta=28977 riquadro=369x182+591+358
ok terza-persona: magenta=23147 riquadro=318x312+321+221
ok inventario: magenta=10850 riquadro=98x140+356+120
ok senza-tuta: pixel=0 copertura=0.00 mirino=0 barra=0 hud=0 riquadro=0x0+0+0
ok telaio-piazzato: pixel=52142 copertura=10.06 mirino=3600 barra=0 hud=0 riquadro=258x284+351+113; magenta=39962 riquadro=286x188+674+352, il 100 % di braccio-rottame
ok mancino-controllo: pixel=18075 copertura=3.49 mirino=0 barra=0 hud=0 riquadro=161x149+122+391
ok mancino: meta' destra magenta=0 riquadro=0x0+0+0; pixel=45442 copertura=8.77 mirino=0 barra=0 hud=0 riquadro=286x205+0+335
ok specchio: mancino magenta=39962 riquadro=286x188+0+352, il 100 % del destro (magenta=39962 riquadro=286x188+674+352); riquadro specchiato 286x188+674+352, scarti sinistra 0 alto 0 destra 0 basso 0
OK 1.21.1-neoforge: scatti in .../scratch/m4c/t4/m3d-neo
```

forge (le stesse salvo `terza-persona` e `inventario`):

```
ok controllo: pixel=18075 copertura=3.49 mirino=0 barra=0 hud=0 riquadro=161x149+677+391
ok dipinto: magenta=39962 riquadro=286x188+674+352; meta' sinistra magenta=0 riquadro=0x0+0+0
ok criteri: pixel=45442 copertura=8.77 mirino=0 barra=0 hud=0 riquadro=286x205+674+335
ok solo-tuta: sagoma=18075 uguali=0
ok nudo: magenta=0 riquadro=0x0+0+0; pixel=40026 copertura=7.72 mirino=0 barra=0 hud=0 riquadro=260x205+700+335; sagoma=18075 uguali=0
ok bastone: magenta=0 riquadro=0x0+0+0
ok mappa: sinistra magenta=23629 riquadro=365x179+0+361; destra magenta=28977 riquadro=369x182+591+358
ok terza-persona: magenta=23166 riquadro=318x312+321+221
ok inventario: magenta=18645 riquadro=166x202+323+66
ok senza-tuta: pixel=0 copertura=0.00 mirino=0 barra=0 hud=0 riquadro=0x0+0+0
ok telaio-piazzato: pixel=52142 copertura=10.06 mirino=3600 barra=0 hud=0 riquadro=258x284+351+113; magenta=39962 riquadro=286x188+674+352, il 100 % di braccio-rottame
ok mancino-controllo: pixel=18075 copertura=3.49 mirino=0 barra=0 hud=0 riquadro=161x149+122+391
ok mancino: meta' destra magenta=0 riquadro=0x0+0+0; pixel=45442 copertura=8.77 mirino=0 barra=0 hud=0 riquadro=286x205+0+335
ok specchio: mancino magenta=39962 riquadro=286x188+0+352, il 100 % del destro (magenta=39962 riquadro=286x188+674+352); riquadro specchiato 286x188+674+352, scarti sinistra 0 alto 0 destra 0 basso 0
OK 1.20.1-forge: scatti in .../scratch/m4c/t4/m3d-forge
```

Che cosa e' cambiato con la vignetta, e perche' poco:
- la vignetta c'e' negli scatti con l'elmo in prima persona: contro gli scatti del Task 3 (neoforge) cambiano solo i
  quattro angoli (somma degli angoli = schermo intero): `braccio-rottame` 4088 + 4074 + 624 + 3529 = 12315,
  `tuta-senza` 10848, `mappa` 10363, `telaio-piazzato` 12448, `bastone` 10605, `inventario` 2308 (sotto la schermata),
  `mancino/braccio-mancino` 12292, `mancino/tuta-senza` 10826; uguali al pixel `braccio-nudo`, `nudo-senza` (telaio
  senza elmo) e i `base-*` (senza tuta). Su forge `inventario` ha 253 pixel diversi fuori dagli angoli, tutti dentro il
  rettangolo dell'anteprima (`differenza ... 216x210+304+60` → `pixel=253 riquadro=159x100+327+127`): l'anteprima
  dell'inventario di 1.20.1 cambia fra due esecuzioni (gia' scritto nel commento di testa); negli angoli 1126,
  1126, 0 e 60 pixel (sotto lo scuro della schermata);
- il magenta scurito dalla vignetta resta magenta per `arm_measure.sh` (r e b > 0,2, g < 0,45 r e 0,45 b): `dipinto`
  39962, `criteri` 45442, `telaio-piazzato` 100 %, `specchio` 100 % come prima; `mappa` a sinistra 23629 (era 23637,
  8 pixel in meno, sulle due varianti); la terza persona 23147 e 23166, sotto il 23157-23745 scritto (la terza
  persona non ha vignetta: cambia fra le esecuzioni, controllo largo >= 130 invariato): intervallo scritto
  23147-23745; le durate 204-211 s (scritto 204-225 s, 75-86 s la B);
- `solo-tuta`: la nota del brief e' nel commento di testa e nel README, con la misura: il braccio del gioco base entra
  nell'angolo in basso a destra con 762 dei suoi 18075 pixel (`differenza base-prima base-senza 144x80+816+460` →
  `pixel=762 riquadro=22x68+816+472`, uguale su neoforge e forge), sul lato interno dell'angolo, dove la texture ha
  alfa al piu' 2/255 (`convert $T -alpha extract -crop 11x34+72+46 ...` → 2); `differenza` di `braccio-rottame` del
  Task 3 e mio in `22x68+816+472` → `pixel=0`; lo scuro che arm_measure.sh vede in quell'angolo sta in
  `92x56+868+484`. La perdita di sensibilita' e' quindi nulla in pratica;
- soglie invariate.

### Verifiche di fine task

scratch/final-checks.sh, uscita 0 in 2m38s (scratch/final-checks.log), sull'albero del commit (dopo la build del Java
sono cambiati solo commenti, documenti e script):
- `./gradlew --console=plain :1.20.1-forge:build ... :1.21.1-neoforge:collectJar` (il comando esatto) → BUILD
  SUCCESSFUL (48 compiti aggiornati: i test erano gia' girati su questo codice alla build dello Step 2, 20:06, 381
  per variante; nessun JUnit legge dal disco i file cambiati dopo: `grep -rnE 'tools/|docs/|"tools"|m4c_shots|...'
  src/test/java` trova solo commenti e i due file letti da TierPaletteTest e SuitScaleTest, docs/arte/README.md e
  tools/art/exo.py, che non ho toccato);
- JUnit: `1.20.1-forge 381`, `1.20.1-fabric 381`, `1.21.1-fabric 381`, `1.21.1-neoforge 381`; nessuna suite con
  fallimenti o errori;
- `OK sources`, `OK jars`, `OK data`, `OK art` (con la texture nuova), `OK arm_measure`, `OK passi`; `bash -n`
  pulito su tutti i tools/client/*.sh;
- `bash tools/run_gametests.sh` → `OK 1.20.1-forge: 235 test obbligatori passati, nessun avviso fuori elenco`, e la
  stessa riga per 1.20.1-fabric, 1.21.1-fabric e 1.21.1-neoforge.

Dopo le ultime modifiche ai commenti degli script, i controlli rifatti con gli script finali sugli scatti di tutte
e quattro le cartelle: `M4C_SOLO_CONTROLLI=1` su scratch/neo e scratch/forge → 17 ok, uscita 0 (righe identiche a
quelle sopra); `M3D_SOLO_CONTROLLI=1` su scratch/m3d-neo e scratch/m3d-forge → 14 ok, uscita 0
(scratch/*-final-recheck.out).

## Che cosa ho visto negli scatti

1.21.1-neoforge (scratch/neo; ritagli scratch/corners-both.png, pair-senza-elmo-terza.png, rv-small.png,
effetti-neo.png, effetti-both.png):
- `pieno.png`: i quattro angoli scuri a partire dagli angoli dello schermo; in alto, sul cielo, l'oscuramento e le due
  strisce chiare in diagonale per angolo; in basso a sinistra il gruppo e' disegnato sopra la vignetta (pezzi e testi
  nitidi, la piastra traslucida un po' piu' scura verso l'angolo); in basso a destra il braccio della tuta scurito verso
  l'angolo. Tinta leggera.
- `senza-vignetta.png`: gli stessi angoli chiari, nessuna striscia.
- `senza-elmo.png`: il solo sottogruppo dell'energia, nessun angolo scuro; `terza.png`: gruppo con sagoma e barra, la
  tuta da dietro, nessun angolo scuro.
- `riavvio/riavvio-visibile.png`: l'HUD visibile, nessun angolo scuro (ed e' uguale al pixel a `senza-vignetta`).
- `vignetta-effetti.png`: l'icona della fortuna (quadrifoglio verde in una cornice scura) in alto a destra, nitida
  **sopra** la vignetta: copre la fine della striscia piu' esterna.

1.20.1-forge (scratch/forge; scratch/forge-sheet.png, effetti-both.png): `pieno`, `senza-vignetta`, `senza-elmo`,
`terza`, `riavvio/riavvio-visibile` come su neoforge; `vignetta-effetti.png`: la vignetta **sopra** l'icona, la
striscia chiara attraversa l'angolo alto a sinistra della sua cornice e l'icona e' scurita (un pixel verde
dell'icona, (930, 30): (24, 76, 28) su 1.21.1, (22, 68, 26) su 1.20.1). E' la differenza di design §10.

## File cambiati

- Nuovo (generato): `src/main/resources/assets/redstone_exoframes/textures/gui/hud_vignette.png`
- `tools/gen_placeholders.py`
- `src/main/java/com/toxineren/exoframes/client/SuitHud.java`
- `tools/client/m4c_shots.sh`
- `tools/client/m3d_shots.sh`
- `tools/README.md` ("Scrive ventiquattro file", la riga della texture; anche "confronta i ventiquattro file" della
  stessa sezione e "da sostituire con la grafica definitiva", che diceva solo oggetti e icona)
- `tools/client/README.md` (paragrafo e tabella di m4c: fase "effetti", riga `vignetta`, `solo-barra`,
  `terza-persona`, `mancino-solo-barra`, `riavvio`, la nota sulle due versioni, il "Non prova"; paragrafo e tabella di
  m3d: la vignetta, la nota su `solo-tuta`, la mappa; le durate in "Quanto dura")
- `docs/compat.md` (in "Avvio del client", la voce di `SuitHud`: la vignetta, `blit` con la fusione e il `finally`,
  solo `FULL`, prima persona e preferenza, le due versioni; "Solo `fill`, `drawString` e `blit`")

## Autorevisione

- Completezza: Step 1-5 come nel brief; R5 misurata prima di disegnare; R2 (la formula ripetuta nel generatore, detto
  nel commento); R11 (`riavvio` confronta `nascosto` della A e `riavvio-nascosto` della C, 0, e il commento di testa
  dice che cosa prova con la vignetta); R13 (`barra-esaurita` con il riquadro `42x16+50+482`, ok sulle due); il
  `version:` di options.txt non toccato (J funziona: `chat-e-tasti` e `riavvio-file` ok).
- Regola di arresto: nessun riquadro o colore atteso cambiato, nessun controllo allargato, soglie della m3d invariate;
  nessun controllo e' fallito.
- Codice: la vignetta e' disegnata per prima, solo con FULL, in prima persona e con la preferenza; la fusione e'
  accesa solo attorno ai quattro `blit` e spenta in un `finally`; nessuna pila delle pose toccata, nessuna scala;
  nessun `isCreative()`; nessuna direttiva Stonecutter (`blit` e `RenderSystem` sono uguali sulle due versioni:
  javap). Righe sotto i 120 caratteri; commenti in italiano con apostrofi ASCII; identificatori in inglese.
- Il paragrafo di testa della m3d l'ho riavvolto tutto a 120 colonne: anche le righe del Task 3 cambiano a capo (solo
  a capo, testo uguale).
- `m5` dichiarata e non usata in `checks()` di m4c_shots.sh viene dal Task 3: non l'ho toccata.
- Fuori dal mio elenco di file, non toccato: il commento di testa della sezione delle texture placeholder di
  `tools/check_art.sh` elenca ancora "oggetti, facce del blocco caricatore, icona del mod, layer armatura" (non la
  stazione, ne' ora la vignetta); il controllo pero' confronta tutto cio' che il generatore scrive (`OK art`).

## Problemi e dubbi

1. **Gli avvisi (i toast) su 1.20.1 non stanno sotto la vignetta.** Il brief vuole nel Javadoc "su 1.21.1 gli elementi
   del gioco base che toccano gli angoli (effetti, avvisi, salvataggio, sottotitoli) stanno sopra la vignetta, su
   1.20.1 sotto" (design §5.12 e §10). javap: `GameRenderer.render` chiama `ToastComponent.render` dopo `Gui.render` e
   dopo le schermate su 1.20.1 (vanilla e con le patch di Forge 47.4.29) e su 1.21.1; il gancio dell'HUD sta dentro
   `Gui.render` su 1.20.1 (Fabric API 3.0.10: `@Inject(method = "render", at = @At("TAIL"))`; Forge: `ForgeGui.render`
   manda `RenderGuiEvent.Post` dopo tutte le sue strisce). Quindi i toast stanno sopra la vignetta su tutte e due le
   versioni. Nel Javadoc di `drawVignette` e in docs/compat.md ho scritto la frase del brief per effetti, salvataggio
   e sottotitoli e, a parte, che gli avvisi stanno sopra su tutte e due (lo dice anche il corpo del commit 7dcdb1e).
   Gli scatti provano solo l'icona dell'effetto
   (sopra su 1.21.1, sotto su 1.20.1). Da decidere: tenere la correzione o la frase del design.
2. **Osservazione per la checklist (Task 5), non verificata in gioco:** su Forge 1.20.1 l'indicatore del salvataggio
   sembra non essere disegnato affatto: `Gui.renderSavingIndicator` e' privato e la chiama solo `Gui.render` di
   vanilla, che `ForgeGui.render` sostituisce senza chiamarla; nessuna classe client di Forge la chiama, e fra le
   strisce di `VanillaGuiOverlay` non c'e'. Un passo della checklist che chieda di vederlo sopra o sotto il gruppo o la
   vignetta su 1.20.1-forge non lo trovera'.
3. I valori della m3d e della m4c scritti nei commenti e nel README vengono da 1.21.1-neoforge e 1.20.1-forge; le
   varianti Fabric le misura il Task 5. L'intervallo del magenta della terza persona della m3d e' allargato a
   23147-23745 (misurati 23147 e 23166; il controllo, largo >= 130, non cambia).

## Correzione 1

Rilettura: .superpowers/sdd/2026-11-20-m4c-hud/task-4-review.md (Important 1, dal piano). Decisioni del coordinatore:
R16 (in ogni angolo il conto del colore della tinta 0A0C0E in `pieno` uguale a quello in `senza-vignetta`: la vignetta
e' traslucida) e R17 (l'indicatore del salvataggio su 1.20.1: sotto la vignetta con Fabric, mai disegnato con Forge,
nel Javadoc di `SuitHud` e in docs/compat.md). Il Minor della rilettura e' rinviato: non toccato.

Progress:
- [x] R16: m4c_shots.sh (costante, controllo, commento, `want`, commento di testa) e la riga del README
- [x] R17: Javadoc di `SuitHud.drawVignette` e docs/compat.md
- [x] `bash -n`, `M4C_SOLO_CONTROLLI=1` su neoforge e forge, la prova negativa con la vignetta opaca
- [x] `check_sources.sh`, compilazione di una variante
- [x] commit 3a69b25 `tools: m4c, la vignetta deve essere traslucida; client: l'indicatore di salvataggio su Forge
  1.20.1`, sopra 7dcdb1e, firmato (intestazione gpgsig), con le due righe finali; albero pulito; non spinto. La copia
  scratch/neo-opaca non e' committata.

### Che cosa ho cambiato

- `tools/client/m4c_shots.sh`:
  - la costante `VIGNETTE_TINT=0A0C0E` (il `VIGNETTE_TINT` (10, 12, 14) di tools/gen_placeholders.py), con i colori;
  - nel controllo `vignetta`, per ciascuno dei quattro angoli, `colore pieno 0A0C0E <angolo>` e
    `colore senza-vignetta 0A0C0E <angolo>`: i due conti devono essere uguali (e non una misura fallita), con un
    commento breve sul perche' (disegnata opaca, cioe' con la fusione spenta, la vignetta dipingerebbe gli angoli della
    sua tinta; i 400 pixel in basso a sinistra sono la cornice della barra, 0C0D0F, disegnata sopra la vignetta);
  - `want` dice la clausola nuova; la riga stampa i conti, `tinta 0A0C0E in pieno/senza-vignetta per angolo a/b ...`;
  - nel commento di testa, la voce `vignetta` dice la clausola, il perche' e i valori misurati.
- `tools/client/README.md`: la riga `vignetta` della tabella (le due misure `colore` per angolo e la clausola, con i
  valori misurati), e in "Quanto dura" i controlli di m4c_shots.sh: circa quaranta secondi (misurati 39 e 40 s; prima
  circa mezzo minuto).
- R17, `client/SuitHud.java` (Javadoc di `drawVignette`) e `docs/compat.md` (voce di `SuitHud` in "Avvio del
  client"): su 1.21.1 effetti, indicatore del salvataggio e sottotitoli sopra la vignetta; su 1.20.1 effetti e
  sottotitoli sotto, l'indicatore del salvataggio sotto con Fabric e mai disegnato con Forge (`ForgeGui` non chiama il
  `Gui.render` del gioco base, l'unico che lo disegna); gli avvisi (i toast) sopra su tutte e due, come prima (R14).
- In piu' di R17, nella stessa voce di docs/compat.md: la frase del Task 3 sul gruppo ("dove gli elementi del gioco
  base si sovrappongono al gruppo (l'indicatore del salvataggio, i sottotitoli), su 1.21.1 stanno sopra ... e su
  1.20.1 sotto") diceva la stessa cosa dell'indicatore del salvataggio; ho aggiunto "(l'indicatore del salvataggio
  solo con Fabric: con Forge 1.20.1 non si disegna)", perche' la voce non si contraddica. Se non va, e' una
  parentesi da togliere.
- Il Minor della rilettura (la riga corta nel "Non prova" del commento di testa) e' rinviato: non toccato.

### Verifiche

`bash -n tools/client/m4c_shots.sh` → uscita 0, nessuna uscita.

`M4C_SOLO_CONTROLLI=1 bash tools/client/m4c_shots.sh 1.21.1-neoforge scratch/neo` → uscita 0 in 39 s, 17 ok
(scratch/neo-corr1.out):

```
ok chat-e-tasti: True False
ok riavvio-file: False False
ok gruppo: pixel=12320 riquadro=176x70+4+466; sagoma pixel=2940 riquadro=42x70+4+466
ok testo: colore=244 riquadro=46x14+50+482
ok barra-normale: colore=1220 riquadro=122x10+52+510
ok sagoma: pixel=1024 riquadro=32x60+10+472; verde prima colore=424 riquadro=28x56+12+474; giallo dopo colore=424 riquadro=28x56+12+474; verde dopo colore=0 riquadro=0x0+0+0
ok critico: rosso colore=424 riquadro=28x56+12+474; giallo colore=0 riquadro=0x0+0+0
ok barra-bassa: pixel=2052 riquadro=124x38+50+482; rosso nella barra colore=60 riquadro=6x10+52+510; verde nella barra colore=0 riquadro=0x0+0+0; rosso nella sagoma colore=0 riquadro=0x0+0+0
ok barra-esaurita: verde nella barra colore=0 riquadro=0x0+0+0; rosso colore=0 riquadro=0x0+0+0; grigio colore=248 riquadro=42x16+50+482
ok scorta: pixel=228 riquadro=24x16+152+482; grigio prima colore=112 riquadro=22x14+152+482; bianco dopo colore=84 riquadro=22x14+152+482
ok solo-barra: pixel=9380 riquadro=134x70+46+466; sagoma pixel=0 riquadro=0x0+0+0
ok terza-persona: pixel=12320 riquadro=176x70+4+466; angoli pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0; pixel=0 riquadro=0x0+0+0
ok vignetta: schermo pixel=11624 riquadro=960x540+0+0, somma degli angoli 11624; angoli pixel=4260 riquadro=98x56+0+0; pixel=4260 riquadro=98x56+862+0; pixel=653 riquadro=70x44+0+496; pixel=2451 riquadro=82x46+878+494; tinta 0A0C0E in pieno/senza-vignetta per angolo 0/0 0/0 400/400 0/0
ok mancino-gruppo: pixel=12320 riquadro=176x70+780+466; sagoma pixel=2940 riquadro=42x70+914+466; percentuale colore=244 riquadro=46x14+784+482
ok mancino-sagoma: pixel=1024 riquadro=32x60+920+472; giallo colore=424 riquadro=28x56+922+474
ok mancino-solo-barra: pixel=9380 riquadro=134x70+780+466; sagoma pixel=0 riquadro=0x0+0+0
ok riavvio: pixel=0 riquadro=0x0+0+0; pixel=12320 riquadro=176x70+4+466
OK 1.21.1-neoforge: scatti in scratch/neo
```

`M4C_SOLO_CONTROLLI=1 bash tools/client/m4c_shots.sh 1.20.1-forge scratch/forge` → uscita 0 in 40 s, 17 ok
(scratch/forge-corr1.out); la riga `vignetta`:

```
ok vignetta: schermo pixel=11649 riquadro=960x540+0+0, somma degli angoli 11649; angoli pixel=4260 riquadro=98x56+0+0; pixel=4260 riquadro=98x56+862+0; pixel=654 riquadro=70x44+0+496; pixel=2475 riquadro=82x46+878+494; tinta 0A0C0E in pieno/senza-vignetta per angolo 0/0 0/0 400/400 0/0
```

Su tutte e due le cartelle le altre 16 righe sono identiche a quelle di prima della correzione (diff con
`*-final-recheck.out`).

Prova negativa: scratch/neo-opaca, copia di scratch/neo (non committata) in cui `pieno.png` e'
`senza-vignetta.png` con la texture x2 incollata opaca ai quattro angoli: alfa > 0 portata a 255
(`convert hud_vignette.png -channel A -fx 'a>0?1:0' +channel -scale 200%`, istogramma dell'alfa: 15696 pixel a 0,
30384 a 255), poi i quattro quarti ciascuno nel suo angolo, come l'immagine della rilettura. Misure dirette: angoli
`pixel=7596 riquadro=132x74+0+0`, `pixel=7596 riquadro=132x74+828+0`, `pixel=7284 riquadro=132x74+0+466`,
`pixel=7596 riquadro=132x74+828+466` (le clausole di prima passerebbero tutte); tinta 7388, 7388, 7684, 7596.
`M4C_SOLO_CONTROLLI=1 bash tools/client/m4c_shots.sh 1.21.1-neoforge scratch/neo-opaca` → uscita 1, 16 ok e 1 KO, le
16 righe ok identiche a quelle della cartella vera (scratch/neo-opaca.out):

```
KO vignetta: schermo pixel=30072 riquadro=960x540+0+0, somma degli angoli 30072; angoli pixel=7596 riquadro=132x74+0+0; pixel=7596 riquadro=132x74+828+0; pixel=7284 riquadro=132x74+0+466; pixel=7596 riquadro=132x74+828+466; tinta 0A0C0E in pieno/senza-vignetta per angolo 7388/0 7388/0 7684/400 7596/0 (atteso in ogni angolo pixel >= 320 e il riquadro dal suo angolo dello schermo (alto a sinistra X=0 e Y=0, alto a destra X+L=960 e Y=0, basso a sinistra X=0 e Y+A=540, basso a destra X+L=960 e Y+A=540); nello schermo intero pixel uguale alla somma dei quattro: nulla fuori dagli angoli; in ogni angolo tanti pixel della tinta 0A0C0E in pieno quanti in senza-vignetta: la vignetta e' traslucida (opaca, con la fusione spenta, dipingerebbe gli angoli della sua tinta))
FAIL 1.21.1-neoforge: controlli falliti: vignetta
```

`bash tools/check_sources.sh` → `OK sources`, uscita 0.

`./gradlew --console=plain :1.21.1-neoforge:compileJava` → `> Task :1.21.1-neoforge:compileJava`, `BUILD SUCCESSFUL
in 3s`, `1 actionable task: 1 executed`, nessun avviso (SuitHud.class riscritto alle 21:06:30;
scratch/corr1-compile.log).
