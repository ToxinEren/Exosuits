# Task 20 — Una sola capacita', barre e suggerimenti degli oggetti: rapporto

Stato: DONE. BASE 0f469e22b213feb00530d1067b8456851e382b86, HEAD 3abc47d (un commit).

## Che cosa ho fatto

- `core/energy/CellBar` (Java puro, `final`, costruttore privato): `MAX_WIDTH = 13`, `CELL_COLOR = 0xE0201C`,
  `width(stored, capacity)` (0 se `capacity <= 0`, altrimenti `Math.round(13f * clamp / capacity)` limitato a [0, 13]),
  `visible(stored, capacity)` = `capacity > 0 && stored < capacity`.
- `compat/impl/ItemBase` (con le direttive): `abstract class ItemBase extends Item`, `tooltip(stack, lines, advanced)`
  vuoto; override di `appendHoverText` per versione (1.21.1 con `Item.TooltipContext`, 1.20.1 con `Level` nel ramo
  commentato), prima `super` poi `tooltip(..., flag.isAdvanced())`; import di `Level` dentro `//? if <1.21 {`.
- `compat/CompatItem` (senza direttive) davanti a `ItemBase`, Javadoc come `CompatEntity`.
- `CellItem extends CompatItem` (resta `final`): `TOOLTIP_CHARGE`, `TOOLTIP_EMPTY`; barre da
  `Compat.itemData().cellEnergy` e `ClientRules.cellCapacity()` con `CellBar`; colore `CELL_COLOR`; suggerimento
  grigio (vuota, oppure `shown / capacity` con la carica tagliata). Javadoc della classe aggiornato (client usa
  `ClientRules`, `toCell` la config del server).
- `PieceItem extends CompatItem`: `TOOLTIP_DURABILITY`, `TOOLTIP_NEW`; barra visibile solo con dati e durata sotto il
  massimo; larghezza con `CellBar.width` (0 senza dati); colore `Mth.hsvToRgb(max(0, d/max) / 3, 1, 1)`, senza dati
  `super`; suggerimento grigio con durata e massimo, o "nuovo" senza argomenti.
- `ExoframeItem extends CompatItem`: quattro righe grigie `place`, `mount`, `enter`, `unmount`.
- `FrameStateCodec`: nel `read` privato il ramo della cella si divide con `trusted`. `read` (server) da' a ogni cella
  `new EnergyCell(cfg.energy.cellCapacity, min(cap, readEnergy))` senza leggere `capacity` (nessuna cella scartata
  piu' per una capacita' mancante, zero o negativa); `readTrusted` invariato, avviso compreso; `write` scrive ancora
  `capacity`. Javadoc di classe, `read`, `readTrusted` aggiornati (design §12.10).
- `RedstoneExoframes.init()`: `Compat.gameTests().register(CellGameTests.class);` subito dopo `MovementGameTests`.
- Lingue: le otto chiavi `tooltip.*` in coda ai due file dopo `condition.redstone_exoframes.broken`, stesso ordine;
  valore nuovo di `message.redstone_exoframes.no_cell` alla riga 32 (en e it). Insieme e ordine delle chiavi uguali
  nei due file (61 chiavi, controllato con uno script).
- `docs/compat.md`: sezione nuova "Fuori dalle interfacce: `CompatItem` — base degli oggetti con suggerimenti" dopo
  `CompatEntity` (File, A cosa serve, Differenze chiuse in `ItemBase`, Condizione, Uso); tabella API, righe `cell` e
  `capacity` dello schema NBT, voce "Capacita' della cella solo nello stato" chiusa nella 3c-1.

## Test

### TDD — RED
- `./gradlew --console=plain :1.21.1-neoforge:test --tests '*CellBarTest' --tests '*FrameStateCodecTest'` prima di
  ogni codice: `compileTestJava FAILED`, `CellBarTest.java:13: error: cannot find symbol` (manca `CellBar`). Atteso.
- Stesso comando dopo `CellBar`, prima di toccare il codec: `FrameStateCodecTest > readCutsTheCellToTheConfigCapacity()
  FAILED`, `21 tests completed, 1 failed` (`read` teneva 20 000). Atteso: la ragione giusta.
- `bash tools/run_gametests.sh 1.21.1-neoforge` con `CellGameTests` e la registrazione, prima del codice:
  `CellGameTests.java:88: error: cannot find symbol` (`tooltip`, `TOOLTIP_*` mancanti), `FAIL 1.21.1-neoforge: Gradle
  e' uscito con 1`. Atteso.

### GREEN
- `./gradlew --console=plain :1.20.1-forge:test --tests '*CellBarTest' --tests '*FrameStateCodecTest'
  :1.21.1-neoforge:test --tests '*CellBarTest' --tests '*FrameStateCodecTest'` → `BUILD SUCCESSFUL`; nei risultati
  XML di entrambe le varianti `CellBarTest` tests="5" failures="0", `FrameStateCodecTest` tests="16" failures="0".
- `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh` (dopo l'implementazione, e di nuovo dopo l'ultima modifica, un
  a capo in `CellGameTests`), ultime righe dell'ultima esecuzione:
  ```
  OK 1.20.1-forge: 159 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 159 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 159 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 159 test obbligatori passati, nessun avviso fuori elenco
  ```
  159 = 156 del Task 19 + 3 (nessun ripiego dei Task 11-12 nel conteggio, come prima).

### Verifiche di fine task
- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
  :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` →
  `BUILD SUCCESSFUL in 27s` (le due firme di `appendHoverText` compilano ciascuna nella sua variante; nessun
  `warning:` nel log).
- `bash tools/check_sources.sh` → `OK sources`
- `bash tools/check_jars.sh` → `OK jars`
- Nessun server, client o build lasciato in esecuzione (resta solo il demone di Gradle inattivo).

## File cambiati

Nuovi: `src/main/java/com/toxineren/exoframes/compat/CompatItem.java`, `compat/impl/ItemBase.java`,
`core/energy/CellBar.java`, `gametest/CellGameTests.java`, `src/test/java/.../core/energy/CellBarTest.java`.
Modificati: `content/CellItem.java`, `content/PieceItem.java`, `content/ExoframeItem.java`,
`compat/impl/FrameStateCodec.java`, `RedstoneExoframes.java`, `lang/en_us.json`, `lang/it_it.json`, `docs/compat.md`,
`src/test/java/.../compat/impl/FrameStateCodecTest.java`.

## Scelte

- In `CellGameTests` la premessa comune (`capacity` dalla config e `ClientRules.cellCapacity() == capacity`) sta in un
  terzo aiuto privato, `localCapacity(helper)`, accanto a `keys` e `args`: la stessa asserzione con lo stesso
  messaggio in tutti e tre i test, senza ripeterla.
- `ExoframeItem` tiene le quattro chiavi in una costante privata `TOOLTIP_KEYS` (lista immutabile, nell'ordine): il
  brief non chiede costanti pubbliche per il telaio e il test usa le stringhe letterali.
- In `FrameStateCodecTest` i tre casi nuovi condividono un aiuto privato `bigCellTag()` (lo stato con la cella da
  20 000/15 000 scritto con `write`), come nel brief.
- `ItemBase` non annota il `Level` di 1.20.1 con `@Nullable` (il codice della mod non usa annotazioni di nullita');
  il Javadoc dice che puo' essere nullo.

## Scostamenti dal brief

- `docs/compat.md`, tabella API: la riga indicata univa `Item.getUseDuration` e `appendHoverText` ("... ; `appendHoverText`
  con `TooltipContext` | — |"). L'ho divisa in due: `getUseDuration` resta con "—" (non ha copertura), la riga nuova
  `Item.appendHoverText` con `Level` → con `Item.TooltipContext` ha a destra "`CompatItem` (`ItemBase`, parte 3c-1)".
  Cambiare solo la colonna di destra della riga unita avrebbe detto coperto anche `getUseDuration`.
- I numeri di riga di `docs/compat.md` del brief (`:203`, `:235`, `:253`, `:275`) non corrispondevano piu' (il file e'
  cresciuto nei task prima); ho cercato le righe per contenuto.

## Revisione personale

- Completezza: tutti i punti di "Produces", le tre GameTest con le asserzioni del brief, i casi JUnit, testi e
  documento. `check_sources` regola 4 (content/ nomina `CompatItem`, mai `ItemBase`), 6 (direttive solo in
  `ItemBase`) e 1c (`CellBar` in `CellBarTest`) passano.
- Righe sotto i 120 caratteri nei file toccati (le due piu' lunghe in `RedstoneExoframes.java` e una in
  `ExoframeItem.java` erano gia' cosi').
- Il vero suggerimento al passaggio del mouse (l'override per versione) non lo raggiunge un GameTest, come dice il
  brief: lo prova il client vero del Task 24 e la checklist per il PC della 3c-2.

## Dubbi

Nessuno.
