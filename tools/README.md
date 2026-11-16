# tools

Strumenti del repository, da lanciare dalla radice. Istruzioni per la grafica del telaio in `tools/art/README.md`,
per il client senza schermo in `tools/client/README.md`.

| Strumento | Che cosa fa |
|---|---|
| `bash tools/check_sources.sh` | regole sui sorgenti (separazione di `core/`, direttive di Stonecutter, codice client): `OK sources` |
| `bash tools/check_jars.sh` | controlli sui quattro jar in `build/libs/all` (metadati, mixin, risorse, privacy): `OK jars`; serve Python 3.11 o più recente |
| `bash tools/check_art.sh` | prove negative del generatore della grafica, poi rigenera la grafica del telaio e le texture placeholder in una cartella temporanea e le confronta con quelle committate: `OK art` |
| `bash tools/run_gametests.sh [variante...]` | test in gioco sulle varianti (di default tutte e quattro); conta le righe `add(tests,` di tutti i file di `gametest/`; una variante passa se tutti i test elencati sono passati e il log non ha avvisi del mod fuori elenco, ciascuno il numero di volte che l'elenco ammette per il suo loader; prima di ogni variante toglie `versions/<variante>/run-gametest/config/redstone_exoframes.json`, che la mod riscrive con i predefiniti all'avvio (i test contano sui predefiniti) |
| `python3 -I tools/check_log.py gametest\|client --variant <variante> <log>...` | avvisi ed errori del mod in un log, confrontati con `tools/log_allowlist.txt` con i conteggi del loader della variante (lo chiamano `run_gametests.sh` e `client/client_shot.sh`) |
| `python3 -I tools/gen_placeholders.py [--out <dir>]` | rigenera le texture placeholder (sotto) |

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
byte. Tranne i due layer armatura trasparenti, sono segnaposto, da sostituire con la grafica definitiva degli oggetti
e dell'icona. Scrive diciotto file:

- `src/main/resources/icon.png` (64x64): l'icona del mod;
- `src/main/resources/assets/redstone_exoframes/textures/item/` (16x16):
  - `redstone_cell.png`: la cella di redstone;
  - `exoframe.png`: l'oggetto del telaio da piazzare;
  - `scrap_helmet.png`, `scrap_torso.png`, `scrap_arm.png`, `scrap_leg.png`: i pezzi Rottame;
  - `exoframe_helmet.png`, `exoframe_torso.png`, `exoframe_legs.png`, `exoframe_boots.png`: i segnaposto che
    stanno negli slot armatura di chi indossa il telaio (`exoframe_torso.png` è anche la texture dell'oggetto di
    prova `suit_model`);
- `src/main/resources/assets/redstone_exoframes/textures/block/` (16x16, del tutto opache: il blocco è un cubo pieno):
  `charger_top.png`, `charger_side.png` e i tre fronti del caricatore, `charger_front.png` (alloggiamento vuoto),
  `charger_front_cell.png` (con la cella) e `charger_front_lit.png` (con la cella e le tacche rosse accese);
- `src/main/resources-mc1.20.1/assets/minecraft/textures/models/armor/redstone_exoframes_frame_layer_1.png` e
  `_layer_2.png` (64x32), solo nei jar 1.20.1: completamente trasparenti, perché il layer armatura di vanilla non
  disegni nulla sopra la tuta. Questi due non sono segnaposto: restano così.

La grafica del telaio sul giocatore e a terra (`textures/entity/exoframe.png` e `exoframe_glowmask.png`) non viene da
qui ma da `tools/art/build.py`.

    python3 -I tools/gen_placeholders.py               riscrive i file nei sorgenti
    python3 -I tools/gen_placeholders.py --out <dir>   scrive gli stessi file sotto <dir> (percorsi relativi a src/main)

`tools/check_art.sh` (anche nella CI) usa `--out` e confronta i diciotto file con quelli committati, pixel per pixel.
