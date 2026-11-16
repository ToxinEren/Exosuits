# Task 5 — Caricatore: il clic

Stato: DONE. BASE `2b7c6963bd49464e54ec3203f5ee8dbc5416f450`, HEAD `c6824efea8eaccb02b4513800b3574844163010c`.
Un commit: `c6824ef content: il clic sul caricatore (cella, polvere, mano vuota, stato)`.
All'inizio HEAD era BASE e l'albero di lavoro era pulito: nessun tentativo precedente da riprendere.
`<tmp>` qui sotto e' la cartella temporanea della sessione (fuori dal repository).

## Che cosa ho implementato

**`content/ChargerBlock.onClick`**, al posto del clic del Task 2, nell'ordine del brief:

1. `hand != MAIN_HAND`, spettatore o giocatore non vivo -> `PASS`.
2. Nella mano principale altro che una cella (`CellItem`), polvere (`Items.REDSTONE`) o nulla -> `PASS`, senza messaggi.
3. Sul client -> `SUCCESS`, senza toccare nulla.
4. Sul server, senza una `ChargerBlockEntity` in quel punto o con un giocatore che non e' un `ServerPlayer` -> `PASS`.
5. L'azione, una per clic, tutta nella chiamata:
   - cella in mano (`insertCell`): `old = charger.cell()`;
     `charger.setCell(CellItem.fromCell(((CellItem) held.getItem()).toCell(held)))` (il taglio alla capacita' della
     config avviene qui e solo qui); se non `instabuild`, `held.shrink(1)`; se `old` non e' vuoto, va nella mano
     principale se ora e' vuota, altrimenti `Handouts.give`; suono `CHARGER_INSERT`;
   - polvere in mano (`addDust`): `moved = min(held.getCount(), MAX_DUST - dust().getCount())`; se `moved > 0`,
     `setDust(new ItemStack(Items.REDSTONE, dentro + moved))` e, se non `instabuild`, `held.shrink(moved)`; nessun
     suono;
   - mano vuota, accovacciati: nulla;
   - mano vuota, in piedi: la cella va in mano (`setItemInHand(MAIN_HAND, charger.cell())`, poi
     `setCell(ItemStack.EMPTY)`), suono `CHARGER_TAKE`; se non c'e' la cella e c'e' polvere, tutta la polvere in mano,
     senza suono; altrimenti nulla.
6. Sempre, dopo l'azione: lo stato nella barra delle azioni (`statusLine`: `STATUS_NO_CELL`, `STATUS_FULL`,
   `STATUS_NO_REDSTONE`, o `STATUS_CHARGING` con `chargePercent()`), poi `SUCCESS` se il contenuto e' cambiato,
   `CONSUME` altrimenti. I suoni a `Vec3.atCenterOf(pos)`.

Javadoc del metodo: i casi, "una cella sta nel caricatore o in un inventario, mai in tutti e due", il taglio
all'ingresso a mano (e non con le tramogge, che passano da `setCell`), la creativa, chiunque puo' prendere la cella
(design §8 domanda 11), che cosa restituisce. Javadoc della classe: tolta la frase "Il clic e' ancora quello del primo
passo", al suo posto una frase sui casi del clic. Nessun `isCreative()`: si guarda `getAbilities().instabuild` e
`isSpectator()`.

**`frame/FrameSounds.Sound`**: in coda all'enum `CHARGER_INSERT(SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS)` e
`CHARGER_TAKE(SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS)`; la tabella nel Javadoc della classe li nomina
(con "polvere e tramogge: nessun suono") e la prima frase dice "del telaio e del caricatore".

**Testi**, nei due file di lingua, dopo `message.redstone_exoframes.charger.no_cell`, nello stesso ordine:
`.charger.full` (Charger: cell full / Caricatore: cella piena), `.charger.no_redstone` (Charger: no redstone /
Caricatore: niente redstone), `.charger.charging` (Charger: charging %s%% / Caricatore: in carica %s %%).

**`gametest/ChargerGameTests`**: otto test in coda al generatore (188 -> 196), con i nomi del brief:
`cellClickInsertsIntoAnEmptyCharger`, `cellClickSwapsWithTheCellInside`, `dustClickMovesWhatFits`,
`emptyHandTakesTheCellThenTheDust`, `sneakingWithEmptyHandsOnlyTellsTheStatus`, `otherItemsAndTheOffHandPass`,
`creativeHandIsNotConsumed`, `twoPlayersOneCell`. Ogni clic e' `clickBlock(helper, p, <pos>, MAIN_HAND)` (o `OFF_HAND`
dove il brief lo dice); il solo clic diretto e' quello dello spettatore (`ModBlocks.CHARGER.get().onClick(...)`).
Ogni test chiude con `removeCharger`. `CHARGER_TEXT_KEYS` ora contiene le quattro chiavi di stato (via la nuova lista
`STATUS_KEYS`) e quelle del suggerimento. Aiuti privati nuovi: `lastArg` (del brief), `statusMessages`, `assertDust`,
`assertUntouched`, `assertOneCell` (vedi "Scelte"); costante `STAND`.

**`docs/compat.md`**: sezione di `CompatEntityBlock`, riga **Uso**: i casi del clic in una frase con il rimando a
`content/ChargerBlock` e ai test; riga **Condizione**: "a mano vuota, con una cella o con la polvere di redstone
consuma" (prima diceva solo "a mano vuota"); "Schema NBT", paragrafo del caricatore: "una carica sopra la capacita' si
taglia solo quando la cella entra a mano (`ChargerBlock.onClick`)" e il test del taglio a mano fra i test citati.

## Che cosa ho provato e con quali risultati

### TDD

**RED 1 (Step 2 del brief)** — `bash tools/run_gametests.sh 1.21.1-neoforge` con i soli test scritti:

```
> Task :1.21.1-neoforge:compileJava FAILED
.../gametest/ChargerGameTests.java:562: error: cannot find symbol
  symbol:   variable CHARGER_INSERT
.../gametest/ChargerGameTests.java:669: error: cannot find symbol
  symbol:   variable CHARGER_TAKE
BUILD FAILED in 2s
FAIL 1.21.1-neoforge: Gradle e' uscito con 1
```

Atteso: i due suoni non esistevano (quattro errori, tutti e solo `CHARGER_INSERT`/`CHARGER_TAKE`).

**RED 2 (in piu', per vedere i test fallire sul comportamento)** — aggiunti solo i due valori dell'enum, con il clic
ancora quello del Task 2, stesso comando:

```
every_item_and_block_has_a_name_in_both_languages failed ... keys missing from the language files: [en_us.json: message.redstone_exoframes.charger.charging, en_us.json: message.redstone_exoframes.charger.full, ...
cell_click_inserts_into_an_empty_charger failed ... the click with a cell gave PASS
empty_hand_takes_the_cell_then_the_dust failed ... the click that took the cell gave CONSUME
sneaking_with_empty_hands_only_tells_the_status failed ... status messages while charging: 1
creative_hand_is_not_consumed failed ... the creative click with a cell was not consumed
two_players_one_cell failed ... after the click of a: charger 0 minecraft:air, hand of a 1 redstone_exoframes:redstone_cell
cell_click_swaps_with_the_cell_inside failed ... the swapping click gave PASS
dust_click_moves_what_fits failed ... the click that moved dust gave CONSUME
8 required tests failed :(
```

Sette degli otto test nuovi falliscono sul clic vecchio, piu' il test dei file di lingua per le tre chiavi mancanti.
`otherItemsAndTheOffHandPass` passava gia' (il clic vecchio passava negli stessi casi): e' la guardia perche' il clic
nuovo non consumi dove il design dice "passa".

**GREEN (Step 4)** — dopo l'implementazione:

```
OK 1.21.1-neoforge: 196 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-forge: 196 test obbligatori passati, nessun avviso fuori elenco
```

### Client vero (Step 6), uno alla volta, comando del brief parola per parola

- `1.20.1-forge` (`<cella>` = `redstone_exoframes:redstone_cell{exoframes:{energy:3000}}`): uscita 0, 88 s,
  `log-check.txt` vuoto (0 byte); ultime righe:
  ```
  [64s] comando eseguito: execute if block 0 -60 1 redstone_exoframes:charger[cell=false,lit=false]
  [88s] log: nessun avviso o errore del mod fuori dall'elenco degli avvisi voluti
  [88s] fatto in 88 s, chiusura
  ```
- `1.21.1-fabric` (`<cella>` = `redstone_exoframes:redstone_cell[minecraft:custom_data={exoframes:{energy:3000}}]`):
  uscita 0, 72 s, `log-check.txt` vuoto (0 byte); stesse righe finali.

Su tutte e due i quattro `execute if` sono riusciti (`{has_cell:1b,cell_energy:3000}`, `[cell=true,lit=false]`,
`[cell=true,lit=true]`, `[cell=false,lit=false]`). Scatti guardati uno per uno, uguali sulle due varianti:

- `caricatore-cella.png`: il fronte con la cella (barre spente), sopra la barra degli oggetti la riga
  `Charger: no redstone`, mano vuota (la cella e' entrata).
- `caricatore-acceso.png`: il fronte acceso (barre rosse) e la riga `Charger: charging 30%`; mano vuota (le otto
  polveri sono entrate tutte).
- `caricatore-svuotato.png`: l'alloggiamento vuoto, la riga `Charger: no cell`, la cella in mano e nel primo posto
  della barra degli oggetti (il terzo clic, a mano vuota, l'ha ripresa).

Gli scatti stanno in `<tmp>/shot-forge` e `<tmp>/shot-fabric`, non nel repository (il brief non elenca file di scatti
per questo task). E' la prima prova del ramo client del clic (`SUCCESS` senza toccare nulla, poi il server allinea la
mano) sulle due versioni.

### Verifiche di fine task (Step 7), dopo l'ultima modifica ai sorgenti

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
  :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`:
  ```
  BUILD SUCCESSFUL in 38s
  52 actionable tasks: 35 executed, 17 up-to-date
  ```
  I JUnit sono girati sulle quattro (`:<variante>:test` eseguito); `LangFilesTest`: `tests="4" skipped="0"
  failures="0" errors="0"` su ciascuna.
- `bash tools/check_sources.sh`: `OK sources`
- `bash tools/check_jars.sh`: `OK jars`
- `bash tools/run_gametests.sh`:
  ```
  OK 1.20.1-forge: 196 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 196 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 196 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 196 test obbligatori passati, nessun avviso fuori elenco
  ```
- `grep -n 'isCreative()' src/main/java/com/toxineren/exoframes/content/Charger*.java`: nessuna riga (uscita 1).
- `tools/check_art.sh` non eseguito: il task non tocca ne' texture ne' modelli, e lo Step 7 non lo elenca.
- Alla fine nessun server, client, Xvfb o build in esecuzione (resta solo il demone di Gradle in attesa).

## File cambiati

- `src/main/java/com/toxineren/exoframes/content/ChargerBlock.java`
- `src/main/java/com/toxineren/exoframes/frame/FrameSounds.java`
- `src/main/java/com/toxineren/exoframes/gametest/ChargerGameTests.java`
- `src/main/resources/assets/redstone_exoframes/lang/en_us.json`
- `src/main/resources/assets/redstone_exoframes/lang/it_it.json`
- `docs/compat.md`

## Scelte

- **Il risultato esatto del clic nei test.** Dove il brief dice "consuma" alcuni test chiedono il valore esatto che
  "Produces" promette (`SUCCESS` se il contenuto e' cambiato, `CONSUME` se il clic ha solo detto lo stato):
  inserimento, scambio, polvere spostata e polvere che non entra, ritiro della cella e della polvere, clic da
  accovacciati. Il risultato passa inalterato da `ServerPlayerGameMode.useItemOn` sulle quattro varianti (verificato:
  i test sono verdi su tutte). Senza questo nessun test distinguerebbe i due valori.
- **`statusMessages(p)`**: la somma dei messaggi delle quattro chiavi di stato. Serve a "nessuno dei quattro messaggi"
  del brief e, in piu', a controllare che ogni clic dica uno stato solo (per esempio dopo l'inserimento: `NO_REDSTONE`
  1 e stato in tutto 1).
- **`assertOneCell`** (per `twoPlayersOneCell`) conta le celle nel caricatore, nell'inventario principale e nella mano
  secondaria dei due giocatori e fra gli oggetti a terra nella struttura: una cella caduta a terra per errore
  conterebbe come seconda, o mancherebbe, invece di sparire dal conto.
- **`assertUntouched`, `assertDust`**: asserzioni comuni dei casi "passa" e della polvere, per non ripetere le stesse
  righe; in `assertUntouched` anche "nulla a terra".
- **Controlli in piu' dentro i test del brief**: nello scambio in creativa, che il caricatore non tenga lo stesso
  oggetto `ItemStack` della mano (altrimenti il tick cambierebbe la carica della cella in mano) e che nulla sia a
  terra; in `emptyHandTakesTheCellThenTheDust`, che la cella spostata nello slot 5 resti li' con 10 000 RE dopo il
  secondo clic; in `sneakingWithEmptyHandsOnlyTellsTheStatus`, che nessuno dei quattro clic suoni.
- **Lo spettatore del test ha le mani vuote** (la cella della mano secondaria si toglie prima): senza il controllo
  `isSpectator()` il clic prenderebbe la cella del caricatore, quindi il test fallirebbe davvero.
- **`level instanceof ServerLevel`** nello stesso `if` del punto (4): serve un `ServerLevel` per `FrameSounds.play`, e
  sul server il livello lo e' sempre; cosi' non c'e' un cast. Se mai non lo fosse il clic passa, come per una block
  entity mancante.
- **Tre metodi privati** (`insertCell`, `addDust`, `statusLine`) invece di un solo corpo lungo: stesso ordine e stesse
  istruzioni del brief.
- **`docs/compat.md`, riga Condizione** di `CompatEntityBlock`: diceva "a mano vuota consuma"; ora nomina anche cella
  e polvere, perche' con questo task la frase era diventata incompleta.
- **Javadoc di `emptyHandClickTellsNoCell`** (test del Task 2): tolte le parole "Il primo passo del clic", non piu'
  vere; il test e il suo corpo non cambiano.

## Scostamenti dal brief

- Nessuno nel comportamento, nei nomi, nei testi o nei numeri.
- Step 2: oltre al RED del brief (non compila) ho fatto un secondo giro RED con i soli due valori dell'enum, per
  vedere i test fallire sul clic vecchio (sopra, "RED 2"). Un'esecuzione in piu' su una variante, nessun effetto sul
  risultato.

## Che cosa ho visto di cio' che il registro dice "mai eseguito"

- `useItemOn` con il giocatore finto sotto gli eventi di Forge e NeoForge: con una cella, con la polvere, a mano
  vuota, accovacciati, con la mano secondaria e in creativa il risultato del blocco arriva inalterato a chi chiama, e
  dopo un `PASS` il gioco prova l'oggetto in mano, sulle quattro varianti.
- Con il clic vecchio (RED 2) la polvere in mano veniva usata dal gioco come oggetto dopo il `PASS` del blocco
  (risultato `CONSUME`: il gioco posava il filo di redstone). E' il motivo per cui la polvere deve essere consumata
  dal blocco, come fa ora.

## Autorevisione

- Completezza: i sei punti di "Produces", i due suoni, i tre testi, gli otto test, `CHARGER_TEXT_KEYS`, i due punti di
  `docs/compat.md`, il client vero sulle due varianti, le verifiche di fine task: fatti.
- Conservazione: in sopravvivenza ogni ramo toglie da una parte e mette dall'altra nella stessa chiamata (inserimento,
  scambio, polvere, ritiro); la cella tolta da uno scambio va in mano o passa da `Handouts.give`, che lascia a terra
  fermo cio' che non entra. In creativa cella e polvere si duplicano di proposito (la mano non si consuma: design §12).
- Limite noto, non di questo task: lo stato del blocco (`cell`, `lit`) lo riscrive il tick seguente della block
  entity, non il clic (design §12.11: "i metodi del contenitore cambiano solo i campi"); nel client vero il fronte
  cambia entro un tick.
- Righe oltre le 120 colonne: una nel Javadoc di `FrameSounds`, trovata e corretta prima del build finale.

## Dubbi e punti aperti

Nessuno.
