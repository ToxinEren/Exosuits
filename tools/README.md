# tools

Strumenti del repository, da lanciare dalla radice. Istruzioni per la grafica del telaio in `tools/art/README.md`,
per il client senza schermo in `tools/client/README.md`.

| Strumento | Che cosa fa |
|---|---|
| `bash tools/check_sources.sh` | regole sui sorgenti (separazione di `core/`, direttive di Stonecutter, codice client): `OK sources` |
| `bash tools/check_jars.sh` | controlli sui quattro jar in `build/libs/all` (metadati, mixin, risorse, privacy): `OK jars`; serve Python 3.11 o più recente |
| `bash tools/check_art.sh` | prove negative del generatore della grafica, poi rigenera la grafica del telaio e le texture placeholder in una cartella temporanea e le confronta con quelle committate: `OK art` |
| `bash tools/run_gametests.sh [variante...]` | test in gioco sulle varianti (di default tutte e quattro); una variante passa se tutti i test elencati sono passati e il log non ha avvisi del mod fuori elenco |
| `python3 -I tools/check_log.py gametest\|client <log>...` | avvisi ed errori del mod in un log, confrontati con `tools/log_allowlist.txt` (lo chiamano `run_gametests.sh` e `client/client_shot.sh`) |
| `python3 -I tools/gen_placeholders.py [--out <dir>]` | rigenera le texture placeholder (sotto) |

## Avvisi voluti: `log_allowlist.txt`

I test in gioco provocano di proposito alcuni avvisi ed errori del mod (un file del registro rovinato apposta, una
voce di armatura che non si può leggere, un id sconosciuto), e su Fabric il gioco scrive all'avvio `No data fixer
registered for exoframe`. `tools/log_allowlist.txt` li elenca uno per riga: ambito (`gametest` o `client`), quante
volte al massimo per log, il messaggio intero come espressione regolare, il test (o l'avvio) che lo produce. Ogni
altra riga WARN/ERROR/FATAL che nomina il mod o la sua entità (anche di vanilla, per esempio `UUID of added entity
already exists: ExoframeEntity[...]`), ogni errore di applicazione dei mixin e ogni traccia che passa dal codice del
mod fa fallire `run_gametests.sh` e il client (`log-check.txt`). Chi aggiunge un test che provoca un avviso di
proposito aggiunge la sua riga nello stesso commit; il formato è spiegato in testa al file.

## Texture placeholder: `gen_placeholders.py`

Disegni originali e procedurali, solo libreria standard di Python 3; due esecuzioni danno file identici byte per
byte. Tranne i due layer armatura trasparenti, sono segnaposto, da sostituire con la grafica definitiva degli oggetti
e dell'icona. Scrive tredici file:

- `src/main/resources/icon.png` (64x64): l'icona del mod;
- `src/main/resources/assets/redstone_exoframes/textures/item/` (16x16):
  - `redstone_cell.png`: la cella di redstone;
  - `exoframe.png`: l'oggetto del telaio da piazzare;
  - `scrap_helmet.png`, `scrap_torso.png`, `scrap_arm.png`, `scrap_leg.png`: i pezzi Rottame;
  - `exoframe_helmet.png`, `exoframe_torso.png`, `exoframe_legs.png`, `exoframe_boots.png`: i segnaposto che
    stanno negli slot armatura di chi indossa il telaio (`exoframe_torso.png` è anche la texture dell'oggetto di
    prova `suit_model`);
- `src/main/resources-mc1.20.1/assets/minecraft/textures/models/armor/redstone_exoframes_frame_layer_1.png` e
  `_layer_2.png` (64x32), solo nei jar 1.20.1: completamente trasparenti, perché il layer armatura di vanilla non
  disegni nulla sopra la tuta. Questi due non sono segnaposto: restano così.

La grafica del telaio sul giocatore e a terra (`textures/entity/exoframe.png` e `exoframe_glowmask.png`) non viene da
qui ma da `tools/art/build.py`.

    python3 -I tools/gen_placeholders.py               riscrive i file nei sorgenti
    python3 -I tools/gen_placeholders.py --out <dir>   scrive gli stessi file sotto <dir> (percorsi relativi a src/main)

`tools/check_art.sh` (anche nella CI) usa `--out` e confronta i tredici file con quelli committati, pixel per pixel.
