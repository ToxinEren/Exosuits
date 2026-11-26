# tools

Strumenti del repository, da lanciare dalla radice. Istruzioni per la grafica del telaio (`tools/art/build.py`) in
`tools/art/README.md`; per il client senza schermo (`tools/client/client_shot.sh`, le sequenze `m3a_shots.sh`,
`m3b_shots.sh`, `m3c_shots.sh`, `m3c_rules_shots.sh`, `m3c2_shots.sh`, `m3d_shots.sh`, `m4a1_shots.sh` e lo strumento
di misura degli scatti, `arm_measure.sh`) in `tools/client/README.md`.

| Strumento | Che cosa fa |
|---|---|
| `bash tools/check_sources.sh` | regole sui sorgenti (separazione di `core/`, direttive di Stonecutter, codice client): `OK sources` |
| `bash tools/check_jars.sh` | controlli sui quattro jar in `build/libs/all` (metadati, mixin, risorse, dati della sola versione del jar, privacy): `OK jars`; serve Python 3.11 o più recente |
| `python3 tools/check_jar_privacy.py <jar>...` | il controllo di privacy sui jar, chiamato da `check_jars.sh`: percorsi locali di un computer, e-mail, `Built-By` nel manifest, metadati di immagini e suoni, autore nelle strutture, e le parole di `EXOFRAMES_PRIVATE_WORDS` |
| `bash tools/check_art.sh` | prove negative del generatore della grafica, poi rigenera la grafica del telaio e le texture placeholder in una cartella temporanea e le confronta con quelle committate: `OK art` |
| `bash tools/check_data.sh` | prove negative del generatore dei dati, poi rigenera ricette, sblocchi, bottino e tag in una cartella temporanea e li confronta con quelli committati; i due alberi per versione contengono solo i file generati: `OK data` |
| `bash tools/run_gametests.sh [variante...]` | test in gioco sulle varianti (di default tutte e quattro); conta le righe `add(tests,` di tutti i file di `gametest/`; una variante passa se tutti i test elencati sono passati e il log non ha avvisi del mod fuori elenco, ciascuno il numero di volte che l'elenco ammette per il suo loader; prima di ogni variante toglie `versions/<variante>/run-gametest/config/redstone_exoframes.json`, che la mod riscrive con i predefiniti all'avvio (i test contano sui predefiniti) |
| `python3 -I tools/check_log.py gametest\|client --variant <variante> <log>...` | avvisi ed errori del mod in un log, confrontati con `tools/log_allowlist.txt` con i conteggi del loader della variante (lo chiamano `run_gametests.sh` e `client/client_shot.sh`) |
| `python3 -I tools/gen_placeholders.py [--out <dir>]` | rigenera le texture placeholder (sotto) |
| `python3 -I tools/gen_data.py [--out <dir> \| --selftest]` | rigenera i dati per versione: ricette, sblocchi, bottino e tag (sotto) |

## Avvisi voluti: `log_allowlist.txt`

I test in gioco provocano di proposito alcuni avvisi ed errori del mod (un file del registro rovinato apposta, una
voce di armatura che non si può leggere, un id sconosciuto), e su Fabric il gioco scrive all'avvio `No data fixer
registered for exoframe`. `tools/log_allowlist.txt` li elenca uno per riga: ambito (`gametest` o `client`), quante
volte per log, il messaggio intero come espressione regolare, il test (o l'avvio) che lo produce. Ogni
altra riga WARN/ERROR/FATAL che nomina il mod o la sua entità (anche di vanilla, per esempio `UUID of added entity
already exists: ExoframeEntity[...]`), ogni errore di applicazione dei mixin e ogni traccia che passa dal codice del
mod fa fallire `run_gametests.sh` e il client (`log-check.txt`). Chi aggiunge un test che provoca un avviso di
proposito aggiunge la sua riga nello stesso commit; il formato è spiegato in testa al file.

Il conteggio si scrive in tre forme: `N` (al più N volte per log), `=N` (esattamente N volte) e
`fabric=N,forge=M,neoforge=K` (esattamente, secondo il loader della variante che `check_log.py` riceve con
`--variant`, per esempio la riga di Fabric: una volta su Fabric, mai su Forge e NeoForge). Gli avvisi voluti di oggi
sono tutti esatti: un avviso voluto che smette di comparire (un test che non arriva più al punto che lo provoca, o un
avviso spostato su un altro loader) fa fallire il controllo come uno in più, invece di passare inosservato.

## Texture placeholder: `gen_placeholders.py`

Disegni originali e procedurali, solo libreria standard di Python 3; due esecuzioni danno file identici byte per
byte. Tranne i due layer armatura trasparenti, sono segnaposto, da sostituire con la grafica definitiva. Scrive
ventiquattro file:

- `src/main/resources/icon.png` (64x64): l'icona del mod;
- `src/main/resources/assets/redstone_exoframes/textures/item/` (16x16):
  - `redstone_cell.png`: la cella di redstone;
  - `exoframe.png`: l'oggetto del telaio da piazzare;
  - `scrap_helmet.png`, `scrap_torso.png`, `scrap_arm.png`, `scrap_leg.png`: i pezzi Rottame;
  - `exoframe_helmet.png`, `exoframe_torso.png`, `exoframe_legs.png`, `exoframe_boots.png`: i segnaposto che
    stanno negli slot armatura di chi indossa il telaio (`exoframe_torso.png` è anche la texture dell'oggetto di
    prova `suit_model`);
  - `station.png`: l'oggetto della stazione di manutenzione, un'icona piatta (un banco con sopra un telaio piccolo);
- `src/main/resources/assets/redstone_exoframes/textures/block/` (16x16, del tutto opache: i blocchi sono cubi pieni):
  `charger_top.png`, `charger_side.png` e i tre fronti del caricatore, `charger_front.png` (alloggiamento vuoto),
  `charger_front_cell.png` (con la cella) e `charger_front_lit.png` (con la cella e le tacche rosse accese); della
  stazione di manutenzione, sulla stessa piastra, `station_top.png` (un piano di lavoro con due guide di rame),
  `station_side.png` (una fascia di rame e tre feritoie) e i fronti delle due metà, `station_front_main.png` (un
  pannello con una spia di redstone e un cassetto) e `station_front_side.png` (un pannello con una griglia di metallo
  e un cassetto), segnaposto fino alla grafica della parte 4d;
- `src/main/resources/assets/redstone_exoframes/textures/gui/hud_vignette.png` (144x80): i quattro angoli della
  vignetta del casco dell'HUD, di 72x40 ciascuno e ciascuno al suo posto (alto a sinistra, alto a destra, basso a
  sinistra, basso a destra: l'HUD li disegna senza specchiarli), traslucidi, scuri dal pixel d'angolo dello schermo
  verso l'interno e con due riflessi chiari del vetro nei due in alto, con la formula delle anteprime del design
  (`draw_vignette` di `docs/superpowers/specs/anteprime/2026-11-19-m4/anteprime.py`); segnaposto fino alla parte 4d;
- `src/main/resources-mc1.20.1/assets/minecraft/textures/models/armor/redstone_exoframes_frame_layer_1.png` e
  `_layer_2.png` (64x32), solo nei jar 1.20.1: completamente trasparenti, perché il layer armatura di vanilla non
  disegni nulla sopra la tuta. Questi due non sono segnaposto: restano così.

La grafica del telaio sul giocatore e a terra (`textures/entity/exoframe.png` e `exoframe_glowmask.png`) non viene da
qui ma da `tools/art/build.py`.

    python3 -I tools/gen_placeholders.py               riscrive i file nei sorgenti
    python3 -I tools/gen_placeholders.py --out <dir>   scrive gli stessi file sotto <dir> (percorsi relativi a src/main)

`tools/check_art.sh` (anche nella CI) usa `--out` e confronta i ventiquattro file con quelli committati, pixel per
pixel.

## Dati per versione: `gen_data.py`

Ricette, avanzamenti di sblocco, tabelle di bottino e tag non si scrivono a mano: li scrive `tools/gen_data.py` (solo
libreria standard di Python 3; due esecuzioni danno file identici byte per byte) in due alberi, uno per versione di
Minecraft, perché le cartelle dei dati hanno nomi diversi (al plurale su 1.20.1, al singolare su 1.21.1) e una
cartella con il nome dell'altra versione il gioco la ignora in silenzio:

| | `src/main/resources-mc1.20.1/data/` | `src/main/resources-mc1.21.1/data/` |
|---|---|---|
| 8 ricette | `redstone_exoframes/recipes/<oggetto>.json` | `redstone_exoframes/recipe/<oggetto>.json` |
| 8 sblocchi | `redstone_exoframes/advancements/recipes/<categoria>/<oggetto>.json` | `redstone_exoframes/advancement/recipes/<categoria>/<oggetto>.json` |
| bottino del caricatore e della stazione | `redstone_exoframes/loot_tables/blocks/<blocco>.json` | `redstone_exoframes/loot_table/blocks/<blocco>.json` |
| tag `minecraft:mineable/pickaxe` | `minecraft/tags/blocks/mineable/pickaxe.json` | `minecraft/tags/block/mineable/pickaxe.json` |

Sono 38 file, diciannove per albero, con gli stessi id sulle due versioni. La sola differenza di contenuto è il
risultato di una ricetta: `{"count": 1, "item": "<id>"}` su 1.20.1, `{"count": 1, "id": "<id>"}` su 1.21.1. La build
non ha nulla di apposta: ogni variante aggiunge già alle risorse la cartella `resources-mc<versione>` della sua
versione.

Le ricette stanno nella tabella `RECIPES` del generatore (oggetto, categoria, righe della forma, con `_` per il
vuoto) e gli ingredienti in `INGREDIENTS` (simbolo → id dell'oggetto; `T` è il banco da lavoro, nella ricetta della
stazione). Per cambiarne o aggiungerne una si tocca la tabella e si rigenera; una ricetta nuova va aggiunta anche
all'elenco del controllo (f3) di `check_jars.sh`. I blocchi stanno in `BLOCKS` (nome, proprietà dello stato da cui
cade l'oggetto): ciascuno ha la sua tabella di bottino, che fa cadere l'oggetto del blocco se sopravvive
all'esplosione, e sta nel tag `minecraft:mineable/pickaxe`. La stazione fa cadere l'oggetto solo dalla metà
principale, come il letto dalla testa: la voce della sua tabella ha la condizione `minecraft:block_state_property` con
`part: main`.

    python3 -I tools/gen_data.py               riscrive i file nei sorgenti
    python3 -I tools/gen_data.py --out <dir>   scrive gli stessi file sotto <dir> (percorsi relativi a src/main)
    python3 -I tools/gen_data.py --selftest    prove negative di validate(): OK selftest (9 difetti segnalati)

Prima di scrivere, `validate()` controlla i file delle due versioni insieme, e se ne rifiuta uno non si scrive
nulla. Impone:

- in ogni ricetta un risultato con `count` 1 e con la chiave della sua versione (`item` o `id`);
- una categoria fra `building`, `redstone`, `equipment`, `misc`;
- un `pattern` rettangolare, da 1×1 a 3×3; una chiave per ogni simbolo di `pattern` diverso dallo spazio, e nessuna
  chiave non usata;
- ogni id nella forma `spazio:percorso`;
- per ogni ricetta il suo sblocco (`recipes/<categoria>/<oggetto>`), con lo stesso id nei premi;
- in ogni condizione `minecraft:block_state_property` di una voce di una tabella di bottino, `block` uguale al blocco
  della tabella (`redstone_exoframes:<nome>` per `redstone_exoframes:blocks/<nome>`) e `properties` un oggetto non
  vuoto di stringhe;
- gli stessi id di ricette, sblocchi, bottini e tag sulle due versioni;
- nessun percorso di una versione nelle cartelle dell'altra;
- nei file dei tag nessuna chiave `remove`, e `replace` falso (il tag è del gioco: il file aggiunge un valore).

`--selftest` inietta in memoria un difetto alla volta (`count` 2, la categoria `tools`, un simbolo senza chiave, una
chiave non usata, `remove` in un tag, una ricetta tolta da una sola versione, un file di 1.20.1 sotto `recipe/`, la
condizione della metà della stazione che nomina il caricatore, la stessa condizione con `properties` vuote) e pretende
che `validate()` lo rifiuti nominando quel file.

`tools/check_data.sh` (anche nella CI) esegue `--selftest`, rigenera con `--out` e confronta i 38 file con quelli
committati, byte per byte; fallisce anche per un file in più nei due alberi (`non viene dal generatore`) e per
qualunque file sotto `src/main/resources/data` che non sia una delle due strutture vuote dei test: i dati per
versione non stanno nelle risorse comuni. Sui jar, il controllo (f3) di `check_jars.sh` ripete la regola delle
cartelle e pretende in ogni jar i diciannove file della sua versione.
