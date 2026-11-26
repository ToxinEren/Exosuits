# Task 5 — rapporto

## Progress

- [x] orientamento; advisor consultato due volte (prima di scrivere; durante le esecuzioni)
- [x] Step 1: m4c (17 ok), m3d (14 ok), m3c (OK) sulle quattro varianti; una seconda m4c su forge (17 ok)
- [x] R19: m4a1_shots.sh su 1.21.1-neoforge, tre ok, OK, log-check vuoto
- [x] Step 2: braccio-colori-suoi su forge e neoforge; i 34 scatti copiati senza metadati (controllo dei blocchi:
      nessuna uscita); `git status -uall docs/test-in-gioco` sugli scatti: 20 `??` e 14 ` M`
- [x] Step 3: server dedicato sulle quattro, Done, stop, check_log senza uscita
- [x] Step 4: note in testa; i controlli: nessuna "manca", 1 nota per file
- [x] Step 5: checklist; 24 passi, un passo 1, 0 "Scala dell'interfaccia"; ogni scatto citato esiste
- [x] Step 6: README.md, tools/README.md, tools/client/README.md, commenti di m4c/m3d/m3c_shots.sh; compat.md riletto
      (nessuna modifica); `grep m4c_shots` → 8
- [x] controlli rifatti con gli script finali (recheck.sh): nove cartelle, righe uguali a quelle dal vivo
- [x] Step 7: verifica finale (final.sh uscita 0: build, 381 JUnit x4, OK sources/jars/data/art/arm_measure/passi,
      235 test in mondo x4, nessuna riga dal diff dell'elenco degli avvisi e dei mixin, git status pulito di altro)
- [x] Step 8: commit 7e33121 (firmato dall'ambiente, con le due righe finali; 46 file, 34 dei quali scatti)

## Esecuzioni (cartelle sotto /home/user/exo-runs/task5/)

Le sequenze girano con `/home/user/exo-runs/task5/driver.sh` (una alla volta, si ferma alla prima uscita diversa da
0; ogni passo finito con 0 sta in `status.txt` con la durata, e un nuovo avvio del driver lo salta). Uscita di ogni
passo, con l'ora per riga: `/home/user/exo-runs/task5/<variante>.<sequenza>.out`. Ordine: per 1.20.1-forge,
1.20.1-fabric, 1.21.1-fabric, 1.21.1-neoforge m4c, m3d, m3c; poi m4a1 su neoforge (R19); poi braccio-colori-suoi su
forge e neoforge. Avviato alle 21:10 del 26 novembre.

### 1.20.1-forge m4c — /home/user/exo-runs/task5/1.20.1-forge/m4c — 375 s (A 153, B 95, C 81 s), uscita 0

17 ok, `OK 1.20.1-forge`; i tre log-check.txt vuoti (0 byte). Righe in `1.20.1-forge.m4c.out`. Valori: gruppo 12320 /
sagoma 2940; testo 244; barra-normale 1220 riquadro 122x10+52+510; sagoma 1024, verde 424, giallo 424; critico 424;
barra-bassa 1992, rosso 60 in 6x10+52+510; barra-esaurita grigio 248 in 42x16+50+482; scorta 228, grigio 112, bianco 84;
solo-barra 9380; terza-persona 12320, angoli 0 0 0; vignetta schermo 11646 = 4260+4260+651+2475, tinta 0/0 0/0 400/400
0/0; mancino-gruppo 12320 / 2940 / 244; mancino-sagoma 1024 / 424; mancino-solo-barra 9380 / 0; riavvio 0 e 12320 in
176x70+4+466. File delle preferenze della A: `"version": 1`, `"hudHidden": true`, `"vignette": false` (Gson indentato).

### 1.20.1-forge m3d — /home/user/exo-runs/task5/1.20.1-forge/m3d — 212 s (A 115, B 79 s), uscita 0

14 ok, `OK 1.20.1-forge`; log-check.txt vuoti (A e mancino). controllo 18075; dipinto 39962; criteri 8.77; nudo 7.72;
mappa 23629 / 28977; terza-persona 23147 (318x312+321+221); inventario 18690 (166x202+323+66); senza-tuta 0;
telaio-piazzato 258 / 100 %; mancino 8.77; specchio 100 %, scarti 0.

### 1.20.1-forge m3c — /home/user/exo-runs/task5/1.20.1-forge/m3c — 123 s (client_shot 121 s), uscita 0

`OK 1.20.1-forge`; log-check.txt vuoto.

### 1.20.1-fabric m4c — /home/user/exo-runs/task5/1.20.1-fabric/m4c — 343 s (A 138, B 87, C 74 s), uscita 0

17 ok, `OK 1.20.1-fabric`; i tre log-check.txt vuoti. Uguale a forge salvo: barra-bassa 2052 (forge 1992: il mezzo
secondo dell'avviso), vignetta in basso a sinistra 653 (forge 651), schermo 11648. Prima esecuzione di m4c su Fabric.

### 1.20.1-fabric m3d — /home/user/exo-runs/task5/1.20.1-fabric/m3d — 196 s (A 106, B 72 s), uscita 0

14 ok, `OK 1.20.1-fabric`; log-check.txt vuoti. Uguale a forge salvo terza-persona 23324 (stesso riquadro
318x312+321+221) e inventario 18793 in 172x202+320+66 (dentro i 166-174 da x 319-323 e 18712-18810 scritti per
Fabric). mappa 23629 / 28977 come forge.

### 1.20.1-fabric m3c — /home/user/exo-runs/task5/1.20.1-fabric/m3c — 107 s, uscita 0

`OK 1.20.1-fabric`; log-check.txt vuoto; nessun "moved wrongly".

### 1.21.1-fabric m4c — /home/user/exo-runs/task5/1.21.1-fabric/m4c — 348 s (A 140, B 87, C 75 s), uscita 0

17 ok, `OK 1.21.1-fabric`; i tre log-check.txt vuoti. Prima esecuzione di m4c su 1.21.1-fabric. Uguale a forge salvo:
barra-bassa 2052 (l'avviso acceso nello scatto), vignetta in basso a sinistra 649, in basso a destra 2451 (come
neoforge nel Task 4), schermo 11620.

Nota: in `1.20.1-forge/m4c/vignetta-effetti.png` c'è una macchia verde scura nell'angolo in alto a destra (x 944-960,
y 0-20, sotto la cornice dell'icona dell'effetto), che non c'è nello scatto forge del Task 4 né in quelli Fabric: è
una particella dell'effetto Fortuna (spiegata in "Problemi e dubbi"); gli scatti di riferimento forge vengono da una
seconda esecuzione (sotto).

### 1.21.1-fabric m3d — /home/user/exo-runs/task5/1.21.1-fabric/m3d — 197 s (A 107, B 71 s), uscita 0

14 ok, `OK 1.21.1-fabric`; log-check.txt vuoti. Prima esecuzione della m3d nuova su 1.21.1-fabric. mappa 23629 /
28977 (come le 1.20.1); terza-persona 23168 (318x312+321+221); inventario 10850 in 98x140+356+120 (il ritaglio di
1.21.1); il resto uguale a forge.

### 1.21.1-fabric m3c — /home/user/exo-runs/task5/1.21.1-fabric/m3c — 107 s, uscita 0

`OK 1.21.1-fabric`; log-check.txt vuoto; nessun "moved wrongly".

### Misure fuori dalle righe dei controlli (frasi dei commenti e dei README da confermare sulle quattro varianti)

- `arm_measure.sh differenza <v>/m4c/nascosto.png <v>/m4c/riavvio/riavvio-nascosto.png` (schermo intero, la frase
  "misurato: 0 pixel diversi su tutto lo schermo" del controllo riavvio): `pixel=0 riquadro=0x0+0+0` su 1.20.1-forge,
  1.20.1-fabric, 1.21.1-fabric e 1.21.1-neoforge.
- `arm_measure.sh differenza <v>/m3d/base-prima.png <v>/m3d/base-senza.png 144x80+816+460` (la frase dei 762 pixel
  del braccio del gioco base nell'angolo in basso a destra, commento di m3d_shots.sh e README):
  `pixel=762 riquadro=22x68+816+472` sulle quattro varianti.
- Il gruppo fra varianti (`differenza ... 176x70+4+466` sugli scatti della A): 1.20.1-fabric e 1.21.1-fabric uguali al
  pixel in tutti gli otto scatti di riferimento; 1.20.1-forge contro 1.20.1-fabric 10-22 pixel (l'erba sotto la piastra
  traslucida), 150 in `bassa` (l'avviso spento su forge, acceso su fabric).
- Nessuna riga 'Suit HUD' o 'Client preferences file', in nessuna maiuscola (`grep -ci`, come Select-String senza
  -CaseSensitive), nei 120 log delle esecuzioni (client.log, server.log, gradle-client.log e gradle-server.log di
  tutte le cartelle, comprese m4c-2, m4a1 e colori) e nei latest.log dei quattro server dedicati: 0 righe in tutti.
  Quindi il comando della checklist trova solo righe della mod.

### 1.21.1-neoforge m4c — /home/user/exo-runs/task5/1.21.1-neoforge/m4c — 352 s (A 138, B 90, C 77 s), uscita 0

17 ok, `OK 1.21.1-neoforge`; i tre log-check.txt vuoti. Vignetta 4260, 4260, 653, 2451, schermo 11624 (come nel Task 4);
barra-bassa 2052 (avviso acceso nello scatto). Messaggi guardati: come su fabric 1.21.1. L'icona dell'effetto in
`vignetta-effetti`: nitida sopra la vignetta, diversa da fabric 1.21.1, dove la vignetta le sta sopra come sulle due
1.20.1 (corretto nel Fix round 1, in fondo: qui c'era scritto "Messaggi e icona dell'effetto guardati: come su fabric
1.21.1", sbagliato per l'icona).

Confronto delle righe ok sulle quattro varianti (/home/user/exo-runs/task5/table-m4c.txt): tutti i controlli danno le
stesse misure e gli stessi riquadri, salvo `barra-bassa` (1992 su forge, avviso spento nello scatto; 2052 sulle altre,
avviso acceso) e `vignetta` negli angoli in basso (in basso a sinistra 651, 653, 649, 653; in basso a destra 2475 sulle
due 1.20.1, 2451 sulle due 1.21.1; schermo 11646, 11648, 11620, 11624). Durate: A 138-153 s, B 87-95, C 74-81, totali
343-375 s.

### 1.21.1-neoforge m3d — /home/user/exo-runs/task5/1.21.1-neoforge/m3d — 200 s (A 107, B 74 s), uscita 0

14 ok, `OK 1.21.1-neoforge`; log-check.txt vuoti. Confronto delle quattro varianti (table-m3d.txt): dodici controlli
uguali al pixel; diversi solo terza-persona (23147, 23324, 23168, 23180, sempre 318x312+321+221) e inventario (forge
18690 in 166x202+323+66, fabric 1.20.1 18793 in 172x202+320+66, fabric 1.21.1 10850 e neoforge 10880 in
98x140+356+120). **neoforge 10880 è fuori dall'intervallo scritto "fra 10782 e 10850"**: allargato il commento di
m3d_shots.sh a 10880, controllo invariato (largo >= 70). mappa 23629 / 28977 sulle quattro.

### 1.21.1-neoforge m3c — /home/user/exo-runs/task5/1.21.1-neoforge/m3c — 117 s (client_shot 115 s), uscita 0

`OK 1.21.1-neoforge`; log-check.txt vuoto; nessun "moved wrongly". Le m3c sulle quattro: 123, 107, 107, 117 s.

### R19: m4a1_shots.sh su 1.21.1-neoforge — /home/user/exo-runs/task5/1.21.1-neoforge/m4a1 — 118 s (client_shot 112 s), uscita 0

```
ok anteprima: ritaglio magenta=18582 riquadro=167x198+604+96; schermata magenta=18582 riquadro=167x198+604+96
ok pulsante: pixel=1112 copertura=0.21 mirino=0 barra=0 hud=0 riquadro=36x32+730+410
ok rotazione: sagoma pixel=24332 copertura=4.69 mirino=0 barra=0 hud=0 riquadro=167x198+604+96; schermata magenta=9225 riquadro=111x198+629+96; ritaglio ...
OK 1.21.1-neoforge: scatti in /home/user/exo-runs/task5/1.21.1-neoforge/m4a1
```

Tre `ok`, `OK 1.21.1-neoforge`, log-check.txt vuoto (0 byte): le misure documentate della 4a-1 (18582, 1112, 24332),
uguali con l'HUD nel client (la stazione si apre con la tuta tolta, quindi l'HUD non entra negli scatti misurati).

### Step 2: braccio-colori-suoi — /home/user/exo-runs/task5/<variante>/colori

`EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <variante> <cartella-2> "@attendi 12" "execute as Exo run
exoframe debug wear rottame" "@attendi 4" "@scatto braccio-colori-suoi"`: 1.20.1-forge uscita 0 in 80 s (client_shot
77 s), 1.21.1-neoforge uscita 0 in 75 s (73 s); i due log-check.txt vuoti. Guardati: il braccio con i suoi colori,
l'HUD e la vignetta, nessuna riga in chat.

Il driver è finito alle 22:00:09 ("driver: tutto finito"), tutte le uscite 0.

### Step 2: gli scatti di riferimento

Con `convert <sorgente>.png -strip -define png:exclude-chunks=date,time docs/test-in-gioco/<cartella>/<variante>-<scatto>.png`
(script /home/user/exo-runs/task5/copy_shots.sh; i primi ventiquattro copiati durante la seconda m4c di forge, con lo
stesso comando): m4c di neoforge (dieci, `mancino-danneggiato` da `mancino/`), m3c (forge e neoforge `hud-energia` →
`*-riga-energia.png`, 1.21.1-fabric `hud-energia-bassa` → `1.21.1-fabric-riga-energia-bassa.png`, 1.20.1-fabric
`hud-energia-esaurita` → `1.20.1-fabric-riga-energia-esaurita.png`), m3d (forge e neoforge: braccio-rottame,
braccio-nudo, mappa, `mancino/braccio-mancino`, e braccio-colori-suoi dalle cartelle colori). I dieci di forge della
4c dopo la seconda esecuzione (sotto).

### Step 3: il server dedicato

(script /home/user/exo-runs/task5/server.sh, in sequenza da servers.sh: eula=true in versions/<v>/run/eula.txt,
`rm -rf versions/<v>/run/world`, `./gradlew --console=plain :<v>:runServer` con lo standard input da una FIFO, fino a
`Done (`, poi `stop` nella FIFO; poi `python3 -I tools/check_log.py client --variant <v>
versions/<v>/run/logs/latest.log`). Uscita in /home/user/exo-runs/task5/servers.out e server-<v>.log:

| Variante | Done | Gradle | check_log | log |
|---|---|---|---|---|
| 1.20.1-forge | dopo 14 s, `Done (2.887s)` | uscita 0 | nessuna uscita, 0 | nuovo (22:06:35), mondo preparato, "Stopping server", 0 righe 'Suit HUD'/'Client preferences file' |
| 1.20.1-fabric | dopo 11 s, `Done (2.153s)` | uscita 0 | nessuna uscita, 0 | nuovo (22:06:49), idem |
| 1.21.1-fabric | dopo 9 s, `Done (1.112s)` | uscita 0 | nessuna uscita, 0 | nuovo (22:07:03), idem |
| 1.21.1-neoforge | dopo 17 s, `Done (0.965s)` | uscita 0 | nessuna uscita, 0 | nuovo (22:07:26), idem |

### 1.20.1-forge m4c, seconda esecuzione — /home/user/exo-runs/task5/1.20.1-forge/m4c-2 — 368 s (A 143, B 97, C 82 s), uscita 0

17 ok, `OK 1.20.1-forge`; i tre log-check.txt vuoti. Uguale alla prima salvo barra-bassa 2052 (l'avviso acceso,
come sulle altre) e vignetta in basso a sinistra 654 (schermo 11649, come il forge del Task 4); uguale a neoforge
salvo gli angoli in basso. Il suo `vignetta-effetti`: l'angolo dell'icona pulito e una particella verde dell'effetto
sotto il mirino. Da qui i dieci scatti forge della 4c.

### La riga del file rovinato (passo 13) e il file con la chiave in più (passo 14), provati fuori dal gioco

`ClientPrefsFile.java` compilato con il Gson di 1.20.1 (2.10) e con quello di 1.21.1 (2.10.1), su un file con i byte
`{ rotto\r\n` (quelli di `Set-Content -Encoding ascii`): tutti e due danno i predefiniti e il problema
`is not valid JSON (java.io.EOFException: End of input at line 2 column 1 path $.rotto)`, senza a capo; il WARN di
`ClientPrefs` è quindi una riga sola con `is not valid JSON` e `the next change with the HUD keys replaces it`. Il
primo salvataggio sostituisce il file (`"version": 1`, poi le due chiavi); il file del passo 14 si legge senza
problemi e, dopo due salvataggi, tiene `"futuro": 42` dopo le tre chiavi.

### I controlli rifatti con gli script finali (dopo le modifiche ai commenti)

`/home/user/exo-runs/task5/recheck.sh`: `M4C_SOLO_CONTROLLI=1` sulle cinque cartelle m4c e `M3D_SOLO_CONTROLLI=1` sulle
quattro m3d, e il confronto (ordinato) delle righe ok con quelle delle esecuzioni dal vivo:

```
1.20.1-forge m4c: uscita 0, 17 ok, righe uguali a quelle dal vivo
1.20.1-forge m4c-2: uscita 0, 17 ok, righe uguali a quelle dal vivo
1.20.1-fabric m4c: uscita 0, 17 ok, righe uguali a quelle dal vivo
1.21.1-fabric m4c: uscita 0, 17 ok, righe uguali a quelle dal vivo
1.21.1-neoforge m4c: uscita 0, 17 ok, righe uguali a quelle dal vivo
1.20.1-forge m3d: uscita 0, 14 ok, righe uguali a quelle dal vivo
1.20.1-fabric m3d: uscita 0, 14 ok, righe uguali a quelle dal vivo
1.21.1-fabric m3d: uscita 0, 14 ok, righe uguali a quelle dal vivo
1.21.1-neoforge m3d: uscita 0, 14 ok, righe uguali a quelle dal vivo
uscita 0
```

`bash -n` pulito su tutti i tools/client/*.sh; nessuna riga oltre 120 colonne nei tre script cambiati.

### Step 8: il commit

`7e33121 docs: checklist della parte 4c, scatti di riferimento (4c, e 3c e 3d con l'HUD), note in testa alle checklist
di prima; tools: m4c_shots.sh con le misure delle quattro varianti` — firmato (intestazione gpgsig), con le due righe
le due righe di attribuzione; 46 file: 34 scatti (20 nuovi, 14 rifatti), la checklist nuova, 11 file di testo;
albero pulito dopo il commit. Non spinto (lo fa chi coordina).


## Le misure accanto alle soglie, sulle quattro varianti (m4c_shots.sh)

| Soglia (valore) | misura | forge 1.20.1 | fabric 1.20.1 | fabric 1.21.1 | neoforge 1.21.1 |
|---|---|---|---|---|---|
| GROUP_MIN 6160 | gruppo, terza-persona, mancino-gruppo | 12320 | 12320 | 12320 | 12320 |
| SIL_MIN 1470 | sagoma di gruppo e mancino-gruppo | 2940 | 2940 | 2940 | 2940 |
| SIL_CHANGE_MIN 510 | sagoma, mancino-sagoma | 1024 | 1024 | 1024 | 1024 |
| SIL_COLOUR_MIN 210 | verde, giallo, rosso, giallo del mancino | 424 | 424 | 424 | 424 |
| TEXT_MIN 120 | testo / barra-esaurita / mancino-gruppo | 244 / 248 / 244 | 244 / 248 / 244 | 244 / 248 / 244 | 244 / 248 / 244 |
| BAR_MIN 610 | barra-normale | 1220 | 1220 | 1220 | 1220 |
| SPARE_MIN 110 | scorta | 228 | 228 | 228 | 228 |
| SPARE_COLOUR_MIN 40 | grigio prima / bianco dopo | 112 / 84 | 112 / 84 | 112 / 84 | 112 / 84 |
| BAR_ONLY_MIN 4690 | solo-barra, mancino-solo-barra | 9380 | 9380 | 9380 | 9380 |
| VIGNETTE_MIN 320 | angoli alto-sx, alto-dx, basso-sx, basso-dx | 4260, 4260, 651 (654 nella seconda), 2475 | 4260, 4260, 653, 2475 | 4260, 4260, 649, 2451 | 4260, 4260, 653, 2451 |
| LOW_FILL_MAX_W 10 (fissa) | larghezza del rosso in barra-bassa | 6 | 6 | 6 | 6 |
| (nessuna) | barra-bassa, pixel diversi | 1992 (2052 nella seconda) | 2052 | 2052 | 2052 |
| (nessuna) | vignetta, schermo intero = somma | 11646 (11649 nella seconda) | 11648 | 11620 | 11624 |
| (nessuna) | tinta 0A0C0E pieno/senza per angolo | 0/0 0/0 400/400 0/0 | uguale | uguale | uguale |

Le altre misure della seconda esecuzione forge sono uguali alla prima. Tutti i riquadri uguali sulle quattro varianti (gruppo 176x70+4+466, sagoma 32x60+10+472 e 28x56+12+474, barra
122x10+52+510, esaurita 42x16+50+482, scorta 24x16+152+482, solo-barra 134x70+46+466, angoli 98x56+0+0, 98x56+862+0,
70x44+0+496, 82x46+878+494, mancino 176x70+780+466, 32x60+920+472, 134x70+780+466).

## Verifica delle etichette e dei comandi (checklist e note)

Contro i file di lingua veri, per le due versioni:
- gioco base: `it_it.json` degli asset (oggetto 3e2996320486ed22fcd488dd6555c3fff1f1d173, lo stesso negli indici
  `1.20.1-5.json` e `1.21.1-17.json` di ~/.gradle/caches/fabric-loom/assets) e `en_us.json` dei due
  `minecraft-client.jar` (1.20.1 e 1.21.1). Controllate: menu.options "Opzioni..."/"Options...", options.controls
  "Comandi..."/"Controls...", controls.keybinds "Assegnazione dei tasti..."/"Key Binds...", key.keyboard.unknown
  "Nessuno"/"Not Bound", gui.done "Fatto"/"Done", menu.returnToGame "Riprendi la partita"/"Back to Game",
  menu.returnToMenu "Salva ed esci dal mondo"/"Save and Quit to Title", menu.quit "Esci dal gioco"/"Quit Game",
  options.skinCustomisation "Aspetto..."/"Skin Customization...", options.mainHand "Mano principale"/"Main Hand",
  .left/.right "Sinistra"/"Destra", options.video "Grafica..."/"Video Settings...", options.guiScale "Scala"/"GUI
  Scale", .auto "Automatica"/"Auto", options.sounds "Audio..."/"Music & Sounds...", options.showSubtitles
  "Sottotitoli"/"Show Subtitles", options.on/off "Sì"/"No" ("ON"/"OFF"), debug.prefix "[Diagnostica]:"/"[Debug]:",
  debug.advanced_tooltips.on/off "Descrizioni avanzate visibili/nascoste" ("Advanced tooltips: shown/hidden"),
  debug.reload_resourcepacks.message "Pacchetti di risorse ricaricati"/"Reloaded resource packs", menu.savingLevel
  "Salvataggio del mondo..."/"Saving world", subtitles.block.anvil.land "Incudine atterra"/"Anvil landed",
  controls.reset "Ripristina"/"Reset", key.swapOffhand "Scambia gli oggetti tra le mani" ("Swap Item With Offhand" su
  1.20.1, "Swap Item With Off Hand" su 1.21.1), menu.singleplayer, selectWorld.create, selectWorld.mapType "Tipo",
  generator.minecraft.flat "Piatto", selectWorld.mapFeatures "Strutture". "Comandi" del mondo: 1.20.1 usa
  selectWorld.allowCommands ("Allow Cheats"), 1.21.1 selectWorld.allowCommands.new ("Allow Commands"; le costanti
  delle classi del client 1.21.1 hanno solo .new e .info), in italiano "Comandi" tutte e due: la checklist lo dice.
- mod: `src/main/resources/assets/redstone_exoframes/lang/it_it.json` e `en_us.json`: key.categories
  "Redstone Exoframes", key...hide_hud "Nascondi l'HUD della tuta"/"Hide exoframe HUD", key...vignette "Vignetta del
  casco"/"Helmet vignette", key...exit "Esci dal telaio", key...cell_swap "Cambia cella", hud_hidden/hud_shown,
  vignette_on/off, keys_hint, energy_low "Energia bassa: niente scatto", energy_empty "Energia esaurita: la tuta si
  muove appena", piece_critical "Pezzo in stato critico: %s", piece_broken "Pezzo fuori uso: %s", piece.helmet "Elmo",
  piece.torso "Torso", debug.wear.scrap/bare/removed, hud.percent "%s %%" (italiano con lo spazio, "9 %") /
  "%s%%" (inglese, "5%" negli scatti).
- comandi: le due forme della cella carica da "Fatti del gioco" (NBT su 1.20.1, custom_data su 1.21.1, le stesse di
  m3c_shots.sh e m4c_shots.sh); gli id degli oggetti in ModItems (exoframe, scrap_helmet, redstone_cell); `/damage`,
  `/item replace entity`, `/playsound ... master @s`, `/effect give ... <secondi> <livello> [true]`, `/say` esistono
  uguali sulle due versioni (1.19.4+); `/give` di un elmo senza dati: `PieceItem.toPiece` dà `Piece.fresh`, integro.
- fatti del codice usati dalla checklist: in creativa montare un pezzo, inserire la cella e piazzare il telaio non
  consumano l'oggetto (`FrameInteractions` e `ExoframeItem`, `instabuild`): al passo 9 la cella resta
  nell'inventario, quindi "×1", e il passo la toglie con `/clear` prima di proseguire; la riga dei tasti la manda solo
  `FrameEntry` (EntryHints), una volta per sessione: compare al passo 9; da spettatore la tuta resta (nessun codice la
  toglie), quindi al passo 17 l'HUD torna; il WARN del file rovinato ha `is not valid JSON (...)` (Gson lancia
  `JsonSyntaxException`, una `JsonParseException`, su `{ rotto`) e `the next change with the HUD keys replaces it`;
  il file scritto è JSON indentato di Gson: `"version": 1,` / `"hudHidden": true,` / `"vignette": false` (visto nel
  file copiato dalle esecuzioni A e C).

## Step 7 — la tabella del "fatta quando" (design §6, la parte 4c), con l'esito per variante

Colonne: 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric | 1.21.1-neoforge. "JUnit" = i test nominati, verdi nei 381 di
ogni variante (build della verifica finale); i controlli sono le righe `ok` di m4c_shots.sh (Step 1).

| Che cosa (design §6, la parte 4c) | Prova | forge 1.20.1 | fabric 1.20.1 | fabric 1.21.1 | neoforge 1.21.1 |
|---|---|---|---|---|---|
| chi indossa la tuta vede barra, celle di scorta e avviso | JUnit percentRoundsDownAndTheBarFollowsTheCharge, energyColourFollowsTheServerState, lowWarningBlinksEveryHalfSecondFromTheChange, blinkRestartsWhenTheStateChanges, spareCellsAreWhiteOrGreyAtZero, noCellMeansNoPercentAndNoBar; controlli testo, barra-normale, barra-bassa, barra-esaurita, scorta | sì | sì | sì | sì |
| con un elmo funzionante anche la sagoma colorata per stato e la vignetta | JUnit fullWithAWorkingHelmet, silhouetteColoursFollowTheLegend; controlli gruppo, sagoma, critico, vignetta, mancino-sagoma | sì | sì | sì | sì |
| senza elmo o con l'elmo rotto solo la barra | JUnit barOnlyWithoutAWorkingHelmet, barOnlyCarriesNoSilhouette; controlli solo-barra, mancino-solo-barra | sì | sì | sì | sì |
| con le due mani l'HUD nell'angolo opposto al braccio, fuori da braccio, barra degli oggetti, barre di stato, mirino e righe della chat | JUnit groupSitsInTheCornerOppositeTheMainHand, narrowInterfacePutsTheGroupTopLeft, groupAvoidsTheArmTheHotbarTheOffhandTheChatAndTheCrosshair, insideIsNeverMirrored, screenRectsAtNineSixtyMatchTheSequence; controlli gruppo e mancino-gruppo (12320 pixel, riquadro 176x70+4+466 e 176x70+780+466) e terza-persona | sì | sì | sì | sì |
| H e la vignetta si ricordano dopo un riavvio del client | i nove ClientPrefsFileTest; controlli chat-e-tasti (True False), riavvio-file (False False), riavvio (0; 176x70+4+466) | sì | sì | sì | sì |
| le sequenze della 3c e della 3d passano con la disposizione nuova | m3c_shots.sh e m3d_shots.sh (Step 1) | sì | sì | sì | sì |
| tutto verde, server dedicato | la build, i controlli, i selftest, 381 JUnit, 235 test in mondo; Step 3 | sì | sì | sì | sì |

Dettagli (uguali sulle quattro varianti):

- riga 1: JUnit verdi; testo 244, barra-normale 1220 in 122x10+52+510, barra-bassa (rosso 6 px da X 52), barra-esaurita (grigio 248 in 42x16+50+482), scorta (×0 grigio → ×1 bianco) ok, uguale sulle quattro varianti.
- riga 2: JUnit verdi; gruppo 12320, sagoma 1024 (verde → giallo), critico 424 rosso, vignetta (angoli dal loro angolo, schermo = somma, traslucida), mancino-sagoma ok, uguale sulle quattro varianti.
- riga 3: JUnit verdi; solo-barra 9380 in 134x70+46+466, mancino-solo-barra 134x70+780+466, sagoma 0 ok, uguale sulle quattro varianti.
- riga 4: JUnit verdi; gruppo e mancino-gruppo 12320 nei riquadri esatti, terza-persona 12320 e angoli 0 ok, uguale sulle quattro varianti.
- riga 5: nove ClientPrefsFileTest verdi; chat-e-tasti True False, riavvio-file False False, riavvio 0 e 176x70+4+466 ok, uguale sulle quattro varianti.
- riga 6: m3c OK, m3d 14 ok, uguale sulle quattro varianti.
- riga 7: BUILD SUCCESSFUL (build e collectJar delle quattro); 381 JUnit per variante, nessun fallimento (risultati della build della verifica finale, 22:11); OK sources, OK jars, OK data, OK art, OK arm_measure, OK passi; `bash tools/run_gametests.sh` → `OK <variante>: 235 test obbligatori passati, nessun avviso fuori elenco` sulle quattro; `git diff --stat main -- tools/log_allowlist.txt src/main/resources-mc1.20.1/redstone_exoframes.mixins.json src/main/resources-mc1.21.1/redstone_exoframes.mixins.json` → nessuna riga; `git status --short` solo i file del task (nessuno scatto fuori da m4c-screenshot/, m3c-screenshot/, m3d-screenshot/); server dedicato: Step 3. Uscita completa in /home/user/exo-runs/task5/final.out (final.sh uscita 0).

## File cambiati

- nuovi: `docs/test-in-gioco/m4c-hud.md` (la checklist, 24 passi); `docs/test-in-gioco/m4c-screenshot/` (venti scatti:
  1.20.1-forge dalla seconda m4c, 1.21.1-neoforge dalla m4c del driver)
- rifatti, stessi nomi: `docs/test-in-gioco/m3c-screenshot/` (1.20.1-forge-riga-energia, 1.21.1-neoforge-riga-energia,
  1.21.1-fabric-riga-energia-bassa, 1.20.1-fabric-riga-energia-esaurita) e `m3d-screenshot/` (dieci)
- `docs/test-in-gioco/m3a-vedere-il-telaio.md`, `m3b-entrare-e-uscire.md`, `m3c-giocarci.md`, `m3d-braccio.md`,
  `m4a1-banco.md`: una nota in testa ciascuna, prima di "Cosa c'è da vedere", dopo le note che c'erano
- `README.md`: "Stato" (la parte 4c), il paragrafo degli scatti (m4c_shots.sh; arm_measure.sh misura anche l'HUD:
  differenza e colore), "Documenti" (m4c-hud.md con m4c-screenshot/, piani fino a M4c)
- `tools/README.md`: `m4c_shots.sh` nell'elenco delle sequenze (fuori dall'elenco del brief)
- `tools/client/README.md`: paragrafo di m4c_shots.sh (non prova, scatti di riferimento, misure delle quattro
  varianti), "Quanto dura" (m3c, m3d, m4c, braccio-colori-suoi)
- `tools/client/m4c_shots.sh`, `m3d_shots.sh`, `m3c_shots.sh`: solo commenti (misure e durate delle quattro varianti;
  nella m3d l'intervallo del magenta dell'inventario di 1.21.1 fino a 10880)
- `docs/compat.md`: riletto contro il codice (SuitHud, HudModel, HudLayout, HudLevel, HudKeys, ClientPrefs,
  ClientPrefsFile, l'ordine di RedstoneExoframesClient.init, addHudRenderer): tutto corrisponde, nessuna modifica.
  La riga 47 ("colore della riga dell'energia ... con m3c_shots.sh (parte 3c-1)") è storia della 3c-1 e fuori dai
  posti che la parte può toccare (Global Constraints): lasciata.

## Che cosa ho fatto, passo per passo

1. Orientamento: brief, contesto-comune (le parti richieste), design §5.12, §6, §8, §9, §12; gli script m4c, m3d, m3c,
   m4a1 e client_shot; il modello m4a1-banco.md; le checklist di prima e i passi citati (m3b 8, 10, 13, 14; m3c 11-14,
   24, 26, 46, 61; m3d 3, 7, 19). Consultato l'advisor prima di scrivere (punti seguiti: niente modifiche agli script
   mentre il driver gira; macchina quieta; scatti senza vignetta e "5%" contro "9 %" detti nella checklist; fatti del
   codice verificati prima di scriverli; "Allow Cheats"/"Allow Commands"; un tasto già assegnato al passo 11; misure
   fuori dalle righe dei controlli rimisurate su Fabric; grep di 'Suit HUD' e 'Client preferences file' nei log;
   lo script dei metadati preso dal piano della 3d; il server con la FIFO delle volte prima; m4c_shots.sh anche nel
   README della radice).
2. Step 1 (e R19, e lo scatto braccio-colori-suoi dello Step 2): un driver fuori dal repository che lancia le sequenze
   una alla volta e si ferma al primo errore (/home/user/exo-runs/task5/driver.sh).
3. Intanto (documenti che gli script in esecuzione non leggono): la checklist m4c-hud.md, le cinque note, README.md,
   tools/README.md, il paragrafo degli scatti di riferimento in tools/client/README.md; compat.md riletto.
4. Fabric: prima esecuzione di m4c e della m3d nuova sulle due Fabric, tutto ok, misure uguali a forge e neoforge
   salvo le note (vignetta in basso, barra-bassa, terza persona e inventario della 3d).
5. Lo scatto forge `vignetta-effetti` con una particella dell'effetto vicino all'icona: una seconda esecuzione di m4c su
   forge (tutta, per avere i dieci scatti forge dalla stessa esecuzione) e gli scatti forge da lì.
6. A driver finito: m3d_shots.sh e m3c_shots.sh (commenti: durate, la frase delle misure, l'inventario fino a 10880, la
   mappa uguale sulle quattro), gli scatti copiati senza metadati, il segnaposto del triangolo; poi m4c_shots.sh
   (commenti) e tools/client/README.md (valori e durate), i controlli rifatti sugli scatti con gli script finali, i
   server, la verifica finale, il commit.

## Problemi e dubbi

- La macchia verde in `1.20.1-forge/m4c/vignetta-effetti.png` (x 944-960, y 0-20, sotto la cornice dell'icona
  dell'effetto e scurita dalla vignetta, quindi nel mondo): è una particella dell'effetto Fortuna, che la sequenza dà
  senza nascondere le particelle (`effect give Exo minecraft:luck 60 0`). Il colore dell'effetto in
  `MobEffects` di 1.20.1 è 5882118 = 59C106; la macchia, tolto lo scuro della vignetta, è circa 48A005-4EA906: la
  stessa tinta. Passeggera (non c'è nello scatto forge del Task 4 né sulle altre tre varianti). Una sola seconda
  esecuzione di m4c su forge, e lo scatto di riferimento più pulito fra i due; la riga dell'effetto nella sequenza non
  è mia e non la cambio (si potrebbe dare l'effetto con le particelle nascoste, `... 60 0 true`, come fa la 3d con
  l'invisibilità): lo segnalo a chi coordina. Nella checklist, passo 15, una frase dice che le particelle verdi sono
  del gioco base. Confermato dalla seconda esecuzione: il suo `vignetta-effetti` ha l'angolo pulito e una particella
  verde dell'effetto in mezzo allo schermo, sotto il mirino; quello di neoforge non ne ha. Presi come riferimento i
  dieci scatti forge della seconda esecuzione (m4c-2), con l'angolo dell'icona pulito.
- `tools/README.md` non era nell'elenco dei file del brief: ci ho aggiunto solo `m4c_shots.sh` nell'elenco delle
  sequenze (l'elenco era rimasto alla 4a-1).
- m3d su neoforge: il magenta dell'anteprima dell'inventario è 10880, fuori dall'intervallo scritto (10782-10850). Il
  controllo guarda solo la larghezza (>= 70; misurata 98) ed è passato: ho allargato il commento di m3d_shots.sh a
  10880, come vuole la regola di chi coordina (si allarga il documento, non il controllo).
- La vignetta in basso a sinistra (649-654) cambia di qualche pixel anche fra due esecuzioni della stessa variante
  (forge 651 e 654 qui, 654 nel Task 4): scritto come intervallo, soglia 320 lontana.
- R19: la m4a1 su neoforge è durata 118 s (112 s client_shot.sh), meno dei "132-152 s" e "125-144 s" scritti per
  m4a1_shots.sh nel README e nel suo commento (misure della 4a-1). È una durata, non una misura; non l'ho cambiata
  (m4a1_shots.sh non è fra i file del task): da allargare se chi coordina lo vuole.
- Aggiunte mie ai passi del brief (dettagli nella revisione sotto): `/clear` della cella al passo 9, `wear rottame 0`
  al passo 15, "Ripristina" al passo 11, la finestra di circa 800x600 al passo 20, la frase sulle particelle al passo
  15, "×2" nella nota del passo 11 della 3c.

## Revisione di me stesso

- Completezza: ogni voce di "Produces" (venti scatti della 4c, quattro della 3c, dieci della 3d, cinque note, la
  checklist con le sue sezioni e i ventiquattro passi nell'ordine del brief, i documenti), le decisioni di chi
  coordina (R11, R13, R16 descritti come li fa lo script; R14 gli avvisi sopra su tutte e due; R17 nessun indicatore
  del salvataggio su Forge 1.20.1, mai chiesto lì; R19 la m4a1 su neoforge).
- Aggiunte mie ai passi del brief, tutte per poterli seguire alla lettera: passo 9 `/clear` della cella rimasta in
  inventario (in creativa montarla non la consuma) prima di `wear rottame 0`, così i passi dopo dicono "×0" come gli
  scatti; passo 15 `/exoframe debug wear rottame 0` dopo aver rimesso la mano destra, così `vignetta-effetti` e `terza`
  si confrontano con gli scatti (fatti con la tuta integra); passo 11 "Ripristina" se il tasto è già assegnato da una
  prova precedente; passo 15 la frase sulle particelle verdi dell'effetto; passo 20 la misura della finestra
  (`Display:` circa 800x600) per vedere l'interfaccia stretta.

## Durate misurate (Task 5)

| Sequenza | 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric | 1.21.1-neoforge |
|---|---|---|---|---|
| m4c (totale; A, B, C) | 375 (153, 95, 81) | 343 (138, 87, 74) | 348 (140, 87, 75) | 352 (138, 90, 77) |
| m4c, seconda su forge | 368 (143, 97, 82) | | | |
| m3d (totale; A, B) | 212 (115, 79) | 196 (106, 72) | 197 (107, 71) | 200 (107, 74) |
| m3c | 123 (121) | 107 (104) | 107 (104) | 117 (115) |
| m4a1 (R19) | | | | 118 (112) |
| braccio-colori-suoi | 80 (77) | | | 75 (73) |

(secondi; fra parentesi i "fatto in" di client_shot.sh)
- Qualità: ogni etichetta, testo e comando citato nella checklist e nelle note controllato contro i file di lingua e
  il codice (sezione sopra); le combinazioni con F3 sempre senza schermate aperte; ogni passo dice il comando o il
  tasto, che cosa si vede e che cosa annotare; i passi delle celle di scorta (cella vuota, seconda mano, "×0" dopo
  `/clear`) ci sono; le parti fatte una volta per versione sono dette nel passo e nella frase dopo la preparazione.
- Disciplina: negli script solo righe di commento (`git diff -U0 tools/client/*.sh` senza righe che non siano
  commenti); nessuna soglia, rettangolo o colore cambiato; compat.md non toccato; nessun dato personale né nome di
  modello o fornitore nei file cambiati (grep); le sole immagini nuove o cambiate sono i trentaquattro scatti.
- Prove: le righe dei controlli dal vivo e quelle rifatte con gli script finali (M4C_SOLO_CONTROLLI,
  M3D_SOLO_CONTROLLI) sono identiche (recheck.sh); la verifica finale tutta verde (final.sh uscita 0).
- Gli scatti: i trentaquattro file committati guardati interi in tre fogli di provini (dalle copie in
  docs/test-in-gioco/): nessuna riga di chat, nessun avviso a comparsa, la camera giusta; le sole cose fuori dall'HUD
  sono quelle volute (i messaggi sulla barra delle azioni negli scatti m3c di energia bassa ed esaurita, la cella in
  mano in `scorta`, la particella dell'effetto nel `vignetta-effetti` di forge, detta nella checklist).

## Fix round 1 — le due Important della revisione del Task 5 (decisione R20)

R20 (chi coordina): l'ordine degli strati dipende dal loader, non dalla versione. NeoForge 1.21.1 tiene sopra il gruppo
e la vignetta gli strati del gioco base a z più alte (icone degli effetti, indicatore del salvataggio, sottotitoli);
Fabric 1.21.1 dipinge gruppo e vignetta sopra di loro, come i due loader di 1.20.1; gli avvisi stanno sopra ovunque
(R14); Forge 1.20.1 non disegna l'indicatore del salvataggio (R17); sottotitoli e indicatore su Fabric 1.21.1 attesi
"sotto", da annotare. Nessun cambio di codice; il piano resta com'è.

### Le osservazioni

1. **Important 1 — i documenti davano per verificato nel cloud il contrario di ciò che mostra l'esecuzione su
   1.21.1-fabric.** Lì l'icona dell'effetto sta sotto la vignetta, come sulle due 1.20.1. Io avevo scritto "nitida
   sopra la vignetta sulle due 1.21.1" (m4c-hud.md:63), lo scatto di neoforge come riferimento delle due 1.21.1 anche
   per quell'angolo (m4c-hud.md:89), "uguale sulle due varianti di ciascuna" (tools/client/README.md:189) e qui "come
   su fabric 1.21.1" (riga 102). La causa: avevo guardato a occhio un ritaglio ingrandito dello scatto di
   1.21.1-fabric e l'avevo preso per uguale a quello di neoforge, senza misurare l'angolo contro le altre tre varianti.
2. **Important 2 — l'attesa per versione del brief (sopra su 1.21.1, sotto su 1.20.1) è sbagliata su 1.21.1-fabric**:
   m4c-hud.md:26 e il passo 15 (righe 133 e 135), e fuori dalla checklist il Javadoc di `SuitHud.drawVignette`,
   compat.md:248, design §5.12 e §10. Con R20 le attese ora dipendono dal loader.

### Le prove, rimisurate in questo giro

- `bash tools/client/arm_measure.sh differenza <a> <b> 60x60+900+0` su `vignetta-effetti.png` (cartelle sotto
  /home/user/exo-runs/task5/):
  - 1.21.1-fabric contro 1.20.1-fabric: `pixel=0 riquadro=0x0+0+0`
  - 1.21.1-fabric contro 1.20.1-forge m4c-2: `pixel=0 riquadro=0x0+0+0`
  - 1.20.1-fabric contro 1.20.1-forge m4c-2: `pixel=0 riquadro=0x0+0+0`
  - 1.21.1-neoforge contro 1.20.1-forge m4c-2: `pixel=816 riquadro=46x38+910+4`
  - 1.21.1-fabric contro 1.21.1-neoforge: `pixel=816 riquadro=46x38+910+4`
- Pixel (930, 25), dentro l'icona (`convert <scatto> -format '%[pixel:p{930,25}]' info:`): `srgb(22,65,25)` in
  `docs/test-in-gioco/m4c-screenshot/1.20.1-forge-vignetta-effetti.png`, 1.20.1-forge/m4c-2, 1.20.1-fabric/m4c e
  1.21.1-fabric/m4c; `srgb(24,76,28)` in 1.21.1-neoforge/m4c e in `m4c-screenshot/1.21.1-neoforge-vignetta-effetti.png`.
  Pixel (915, 8), sulla cornice dell'icona: `srgb(68,68,68)` su 1.20.1-forge m4c-2, 1.20.1-fabric e 1.21.1-fabric (il
  riflesso del vetro ci passa sopra), `srgb(33,33,33)` su 1.21.1-neoforge.
- `1.21.1-fabric/m4c/client.log`: "Loading Minecraft 1.21.1 with Fabric Loader 0.20.1" (l'esecuzione è quella giusta).
- Il perché (javap sui jar dell'ambiente di sviluppo): su 1.21.1 il `Gui.render` del gioco base accende il test di
  profondità, chiama `LayeredDraw.render` e lo spegne prima di uscire; NeoForge 21.1.266 chiama al suo posto
  `GuiLayerManager.render`, che manda `RenderGuiEvent.Pre`/`.Post` lì dentro, con il test acceso, e gli strati del
  gioco base a z più alte vincono; Fabric API (rendering-v1 5.2.1, `InGameHudMixin`) manda `HudRenderCallback.EVENT`
  da un'iniezione a `TAIL` di `render`, dopo che il test si è spento, e Architectury Fabric aggancia lì
  `ClientGuiEvent.RENDER_HUD`: l'HUD della mod copre tutto ciò che il gioco base ha disegnato, come su 1.20.1.

### Che cosa è cambiato (file:riga, dopo il fix)

- `docs/test-in-gioco/m4c-hud.md:26` — "Dipende dal loader, non dalla versione": sottotitoli, indicatore del
  salvataggio e icone degli effetti sopra su NeoForge 1.21.1, sotto su Fabric 1.21.1 e sulle due 1.20.1 (l'HUD della
  mod si disegna dopo e li copre); gli avvisi sopra su tutte e quattro le varianti; Forge 1.20.1 come prima (R17).
- `docs/test-in-gioco/m4c-hud.md:63` — l'icona nitida sopra la vignetta solo su 1.21.1-neoforge; su 1.21.1-fabric,
  1.20.1-fabric e 1.20.1-forge la vignetta le sta sopra, e nell'angolo `60x60+900+0` queste tre sono uguali al pixel.
- `docs/test-in-gioco/m4c-hud.md:67` — in più rispetto alle righe elencate, perché portava la stessa affermazione:
  "scatti uguali a occhio, tranne l'icona dell'effetto in `vignetta-effetti`, che su 1.21.1-fabric sta sotto la
  vignetta come su 1.20.1".
- `docs/test-in-gioco/m4c-hud.md:89` (D) — per l'angolo in alto a destra di `vignetta-effetti` su 1.21.1-fabric il
  riferimento è `1.20.1-forge-vignetta-effetti.png`, con il perché.
- `docs/test-in-gioco/m4c-hud.md:133` (passo 15, sottotitoli) — attesi sopra il gruppo su NeoForge 1.21.1, sotto su
  Fabric 1.21.1 e sulle due 1.20.1; su Fabric 1.21.1 non visti nel cloud, attesi sotto per lo stesso motivo dell'icona.
- `docs/test-in-gioco/m4c-hud.md:135` (passo 15, icona) — sopra, nitida, su NeoForge 1.21.1 (scatto di neoforge);
  sotto, scurita, su Fabric 1.21.1 e sulle due 1.20.1 (scatto di 1.20.1-forge).
- `docs/test-in-gioco/m4c-hud.md:136` (passo 15, indicatore del salvataggio) — "annota dove stava (atteso sopra il
  gruppo su NeoForge 1.21.1, sotto su Fabric 1.21.1 e su Fabric 1.20.1; su Fabric 1.21.1 nel cloud non è stato visto)";
  la frase di Forge 1.20.1 come prima.
- `tools/client/README.md:167` — la parentesi dello scatto `vignetta-effetti`: l'icona sopra la vignetta su NeoForge
  1.21.1 e sotto, scurita, su Fabric 1.21.1 e su 1.20.1.
- `tools/client/README.md:189` — la differenza dipende dal loader; le tre varianti uguali al pixel in `60x60+900+0`;
  la causa in una frase. Il pixel ora ha le coordinate, (930, 25): verde 76 su 1.21.1-neoforge e 65 sulle altre tre.
  Prima diceva "in un pixel verde dell'icona ... 76 su 1.21.1 e 68 su 1.20.1", senza coordinate: nei pixel dove
  neoforge ha 76, sotto la vignetta il verde va da 58 a 70 secondo il punto.
- `src/main/java/com/toxineren/exoframes/client/SuitHud.java:182-190` — Javadoc di `drawVignette`, solo commento: "la
  differenza fra i loader"; sopra la vignetta solo su NeoForge 1.21.1, perché NeoForge manda il suo evento dentro il
  gestore degli strati, con il test di profondità acceso; su Fabric 1.21.1 (Fabric API alla fine di `Gui.render`, dopo
  che il test si è spento) e sulle due 1.20.1 la vignetta copre; gli avvisi sopra su tutte e quattro le varianti.
- `docs/compat.md:248` — le due frasi: il gruppo (indicatore del salvataggio e sottotitoli sopra su NeoForge 1.21.1,
  `RenderGuiEvent.Post` dentro il gestore degli strati; sotto su Fabric 1.21.1, `HudRenderCallback` alla fine di
  `Gui.render`, e su 1.20.1) e gli angoli (sopra la vignetta solo su NeoForge 1.21.1; misurato sull'icona di un
  effetto, sottotitoli e indicatore per lo stesso meccanismo).
- `docs/superpowers/specs/2026-11-19-m4-stazione-design.md:265` (§5.12, "Dove") — sopra il gruppo su NeoForge 1.21.1,
  sotto su Fabric 1.21.1 e su 1.20.1 (dipende dal loader); `:291` (§5.12, "Vignetta") — "Differenza fra i loader", e
  "Due eccezioni" diventa "Tre fatti trovati eseguendo la 4c": il terzo è Fabric 1.21.1, con la causa; `:535` (§10) —
  "secondo il loader": sotto su NeoForge 1.21.1, sopra su Fabric 1.21.1 e su 1.20.1. `:607` (§12) non cambiata: la
  cella dice dove l'indicatore si disegna (1.21.1 e Fabric 1.20.1; Forge 1.20.1 no), non se sta sopra o sotto.

In "File cambiati" e in "Revisione di me stesso" sopra, "compat.md ... nessuna modifica" e "compat.md non toccato"
valgono per il commit 7e33121; questo giro cambia `docs/compat.md:248` (R20).

### Comandi e uscite

- `bash tools/check_sources.sh` → `OK sources` (uscita 0; rifatto dopo l'ultima modifica).
- `./gradlew --console=plain :1.21.1-neoforge:build` → `BUILD SUCCESSFUL in 7s`, `9 actionable tasks: 6 executed, 3
  up-to-date` (compileJava e test eseguiti; uscita in /home/user/exo-runs/task5/fix1-build.out). JUnit da
  `versions/1.21.1-neoforge/build/test-results/test/TEST-*.xml`: 381 test in 51 classi, 0 failures, 0 errors,
  0 skipped.
- `grep -n 'z più alte\|1\.21\.1'` sui cinque file, letto riga per riga: le righe sugli strati nominano tutte il
  loader (m4c-hud.md 26, 63, 67, 89, 133, 135, 136; tools/client/README.md 167, 189; SuitHud.java 182-190, dove è
  scritto "z piu'"; compat.md 248; design 265, 291, 535). Le altre righe con 1.21.1 non parlano di strati (le misure
  per versione della vignetta in basso a destra, dell'erba e del braccio; i nomi degli scatti di riferimento; righe di
  compat.md e del design su altro).
- `git status --short` → solo i cinque file (` M`), nessuno scatto.
- Righe aggiunte del diff: nessun nome di modello o fornitore, nessun dato personale (le sole "@" sono i comandi `@s` e
  i `{@code}` del Javadoc).

### La riga 102

Diceva "Messaggi e icona dell'effetto guardati: come su fabric 1.21.1." Per i messaggi era giusto, per l'icona no: su
neoforge l'icona è nitida sopra la vignetta, su fabric 1.21.1 la vignetta le sta sopra come sulle due 1.20.1 (816
pixel diversi nell'angolo fra le due 1.21.1, 0 fra fabric 1.21.1 e le due 1.20.1). Ora la riga dice questo, con un
rimando qui.

### Rimasto fuori (per la revisione finale)

- `tools/client/m4c_shots.sh:46-47`, commento dello script: "l'icona sopra la vignetta su 1.21.1 e sotto, scurita, su
  1.20.1 (design M4 par. 5.12 e 10)". È per versione, ma questo giro esclude gli script.
- `docs/superpowers/plans/2026-11-20-m4c-hud.md:182` e `:333`: la stessa differenza per versione; il piano resta com'è
  (R20).
- `docs/test-in-gioco/m3c-giocarci.md:27` (la riga dell'energia della 3c, che la 4c ha tolto): "con la schermata di F3
  aperta, su 1.21.1 la riga finisce sotto i riquadri di F3". Per lo stesso meccanismo, su Fabric 1.21.1 la riga
  potrebbe essere stata sopra (deduzione, non misurata). Checklist di prima, non toccata.
- Le copie di `SuitHud.java` generate da Stonecutter sotto `versions/{1.20.1-forge,1.20.1-fabric,1.21.1-fabric}/build/
  generated/` hanno il Javadoc di prima fino alla loro prossima build (uscite della build, ignorate da git;
  1.21.1-neoforge compila da `src/`).

### Commit

`5b1941a` "docs, client: su Fabric 1.21.1 l'HUD e la vignetta coprono gli strati del gioco base come su 1.20.1
(misurato); solo NeoForge 1.21.1 li tiene sopra", firmato dall'ambiente, con le due righe finali come in 7e33121; cinque
file, 22 righe aggiunte e 20 tolte; `git status --short` vuoto dopo il commit. Non spinto.

### Controlli dopo il commit

- Il riferimento della riga 89 contro il file committato:
  `arm_measure.sh differenza docs/test-in-gioco/m4c-screenshot/1.20.1-forge-vignetta-effetti.png
  /home/user/exo-runs/task5/1.21.1-fabric/m4c/vignetta-effetti.png 60x60+900+0` → `pixel=0 riquadro=0x0+0+0`.
- Le modifiche oltre le righe elencate da chi coordina (tools/client/README.md:167, il pixel con le coordinate in
  tools/client/README.md:189, m4c-hud.md:67, design:265) portano tutte la stessa affermazione corretta; rilette contro
  le otto Minor della revisione (righe 99-131): nessuna tocca una Minor.
- Da decidere (non cambiato, il giro chiedeva solo le attese per loader): i passi 15 (righe 133 e 135) e 21 (riga 145)
  si fanno "una volta su una variante 1.20.1 e una su una 1.21.1". Con R20 le due 1.21.1 si comportano in modo diverso,
  quindi sul PC se ne vede una sola: i sottotitoli e l'indicatore del salvataggio su Fabric 1.21.1, che R20 vuole
  annotati, si annotano solo se l'assistente sceglie Fabric. Sono anche le righe con 1.21.1 senza loader, in un passo
  sugli strati, che il grep mostra.

## Fix round 1, extension (R21)

R21 (chi coordina): i controlli della checklist che dipendono dall'ordine degli strati (nel passo 15 i sottotitoli,
l'icona dell'effetto e l'indicatore del salvataggio; il passo 21, F3) si fanno una volta su una variante 1.20.1 e una
volta su ciascuna delle due 1.21.1, perché con R20 i due loader di 1.21.1 sono diversi. I passi 13, 14, 20 e 22 restano
una volta per versione. Il commento della fase 11 di m4c_shots.sh dice il fatto di R20 (solo il commento).
m3c-giocarci.md:27 e il piano restano come sono.

### Che cosa è cambiato (file:riga)

- `docs/test-in-gioco/m4c-hud.md:93` — la frase sulle varianti, da seguire alla lettera: si sceglie una variante
  1.20.1 e una 1.21.1, e lì si fanno i passi 13, 14, 20, 21 e 22 e, nel passo 15, i sottotitoli e l'icona
  dell'effetto. Sull'altra 1.21.1 si fanno anche il passo 21 e quei due punti del passo 15, e si saltano 13, 14, 20 e
  22 ("dal passo 12 vai al 15, dal 19 al 21 e dal 21 al 23"). Sull'altra 1.20.1 si saltano 13, 14, 20, 21 e 22 ("dal
  passo 12 vai al 15, e dal 19 al 23") e quei due punti. L'indicatore del salvataggio si annota, se compare, su
  1.20.1-fabric, 1.21.1-neoforge e 1.21.1-fabric (su 1.20.1-forge non compare mai, R17).
- `:133` (sottotitoli) e `:135` (icona dell'effetto) — "(una volta su una variante 1.20.1, una su 1.21.1-neoforge e una
  su 1.21.1-fabric)".
- `:136` (indicatore del salvataggio) — "(una volta su 1.20.1-fabric, una su 1.21.1-neoforge e una su
  1.21.1-fabric)": la variante 1.20.1 è Fabric perché su Forge 1.20.1 non compare mai (R17, la frase in fondo resta).
- `:145` (passo 21, F3) — la parentesi nuova e l'attesa per loader, sempre come cosa da annotare: "Atteso, per lo
  stesso motivo dell'icona dell'effetto al passo 15: su NeoForge 1.21.1 le righe di testo di F3, con il loro fondo
  grigio, possono coprire il gruppo dove ci arrivano; su Fabric 1.21.1 e sulle due 1.20.1 il gruppo è disegnato sopra
  F3."
- `tools/client/m4c_shots.sh:45-48` (fase 11, solo commento; una riga in più): "l'icona sopra la vignetta su NeoForge
  1.21.1 e sotto, scurita, su Fabric 1.21.1 e su 1.20.1 (dipende dal loader, design M4 par. 5.12 e 10)". Lo script
  resta tutto ASCII, come prima ("par." al posto del simbolo di paragrafo).

### L'attesa di F3, controllata nel bytecode prima di scriverla (javap, jar della cache di Gradle)

- Forge 1.20.1 (47.4.29): `ForgeGui.render` manda `RenderGuiEvent$Pre`, disegna le sovrapposizioni
  (`GuiOverlayManager.getOverlays()` e `forEach`), poi manda `RenderGuiEvent$Post`. `DEBUG_TEXT` è una di quelle
  sovrapposizioni (dopo `POTION_ICONS`). In `ForgeGui` i `PoseStack.translate` sono solo in `renderRecordOverlay`,
  `renderTitle` e `renderChat`; `ForgeDebugScreenOverlay.drawGameInformation` spegne il test di profondità.
  Architectury forge 9.2.14 manda `RENDER_HUD` da `RenderGuiEvent$Post`. Quindi il gruppo si disegna dopo F3, alla
  stessa z: sopra.
- Fabric 1.20.1 (fabric-rendering-v1 3.0.10): `InGameHudMixin` ha `@Inject(method = "render", at = @At("TAIL"))`,
  con lo slice da `PlayerListHud.render`. Nel `Gui.render` del gioco base 1.20.1 (javap del jar con i nomi): il test di
  profondità si accende all'inizio (offset 54), poi `renderEffects` (624), `DebugScreenOverlay.render` (645), ...,
  `PlayerTabOverlay.render` (1518), `return` (1537). `DebugScreenOverlay` di 1.20.1 non ha nessun
  `PoseStack.translate`. Quindi il gruppo si disegna dopo F3, alla stessa z: sopra.
- NeoForge 1.21.1 (21.1.266): `GuiLayerManager.render` manda `RenderGuiEvent$Pre`, poi `renderInner` (`pushPose`, a
  ogni strato `render` e `translate`, `popPose`), poi `RenderGuiEvent$Post`. Architectury neoforge 13.0.11 manda
  `RENDER_HUD` da lì. Il gancio disegna quindi alla z di partenza, con il test di profondità acceso, sotto lo strato
  di F3 (`DEBUG_OVERLAY`). `DebugScreenOverlay` (sia del gioco base 1.21.1 sia di NeoForge) non ha nessun `translate`
  e non tocca il test di profondità. Dove F3 arriva sul gruppo, vince F3.
- Fabric 1.21.1: come nel Fix round 1 (`HudRenderCallback` a `TAIL` di `Gui.render`, dopo che il test si è spento):
  il gruppo sopra F3.
- Nel cloud F3 non si prova (nessuna sequenza lo preme): è un'attesa dal codice, coerente con m3c-giocarci.md:178
  ("su 1.21.1 la schermata di debug copre la riga; su 1.20.1 la riga può restare visibile, disegnata sopra le prime
  righe di testo di F3"), che non dice su quale loader di 1.21.1 era stato visto.

### Comandi e uscite

- `bash -n tools/client/m4c_shots.sh` → nessuna uscita, uscita 0. Nessuna riga oltre 120 colonne
  (`awk 'length > 120'`: niente); `file`: "Bourne-Again shell script, ASCII text executable". Nel diff dello script le
  righe tolte e aggiunte cominciano tutte con `#` (le altre sono 0). I controlli non li ho rifatti, perché lo script
  cambia solo nel commento.
- `grep -n 'una su una 1\.21\.1\|1\.21\.1-neoforge e una' docs/test-in-gioco/m4c-hud.md`:
  - forma di prima: 126 (passo 13), 144 (passo 20), 146 (passo 22);
  - forma nuova: 133, 135 e 136 (i tre punti del passo 15), 145 (passo 21).
  - Il passo 14 (riga 127) dice "(sulle stesse due varianti del passo 13)": stessa portata del 13, e il grep non lo
    trova.
- `bash tools/check_sources.sh` → `OK sources`.
- `git status --short` → ` M docs/test-in-gioco/m4c-hud.md` e ` M tools/client/m4c_shots.sh`, nessuno scatto. Nessun
  Java cambiato, quindi nessuna build.

### Commit

`8ecd206` "docs, tools: la checklist della 4c guarda i sottotitoli, l'effetto e F3 su tutte e due le 1.21.1, perché lì
dipende dal loader", firmato dall'ambiente, con le due righe finali. Due file, 7 righe aggiunte e 6 tolte;
`git status --short` vuoto dopo il commit. Non spinto. L'oggetto non porta "(R20)" come nell'esempio di chi coordina:
nessun commit del repository cita una decisione per numero, e chi legge la storia non saprebbe che cosa vuol dire. Il
perché è detto a parole ("perché lì dipende dal loader").
