# Task 6 — Caricatore: tramogge e rottura

Stato: DONE. BASE `c6824efea8eaccb02b4513800b3574844163010c`, HEAD `9619ec68ef115006f6234168b99f9642f2cc2fc0`.
Un commit: `9619ec6 content: il caricatore con le tramogge; la rottura fa cadere cella e polvere`.
All'inizio HEAD era BASE e l'albero di lavoro era pulito: nessun tentativo precedente da riprendere.
`<tmp>` qui sotto e' la cartella temporanea della sessione (fuori dal repository).

## Che cosa ho implementato

**`content/ChargerBlockEntity implements WorldlyContainer`** (nessuna direttiva, nessun codice per loader), come in
"Produces":

- `getContainerSize()` 2; `isEmpty()` (cella e polvere vuote; riserva e riporto non contano); `getItem(slot)`: 0 ->
  `cell`, 1 -> `dust` (gli stack veri, mai una copia), altro -> `ItemStack.EMPTY`.
- `removeItem(slot, amount)`: con `amount >= 1`, dallo slot 0 esce lo stack stesso della cella e lo slot resta vuoto;
  dallo slot 1 `dust.split(amount)`, con lo slot a `EMPTY` se non resta nulla; `setChanged()` solo se e' uscito
  qualcosa. `removeItemNoUpdate(slot)`: lo stack dello slot, slot vuoto, senza `setChanged()`.
- `setItem(slot, stack)`: 0 -> `setCell` (che non taglia la carica), 1 -> `setDust`, altro ->
  `IllegalArgumentException("no such slot in a charger: N")`. Javadoc: lancia per uno stack che `canPlaceItem`
  rifiuterebbe; tramogge e distributori chiedono prima `canPlaceItem`.
- `stillValid(player)` -> `Container.stillValidBlockEntity(this, player)`; `clearContent()`: i due slot vuoti, riserva
  e riporto restano.
- `canPlaceItem(slot, stack)`: slot 0 -> `stack.getItem() instanceof CellItem && stack.getCount() == 1 &&
  cell.isEmpty()`; slot 1 -> `stack.is(Items.REDSTONE)`; altro -> falso. `getMaxStackSize()` non e' ridefinito.
- `getSlotsForFace`: `UP` e `DOWN` -> `{0}`, i lati -> `{1}`. `canPlaceItemThroughFace`: `UP` -> slot 0 e
  `canPlaceItem(0, stack)`; lato -> slot 1 e `canPlaceItem(1, stack)`; `DOWN` o `null` -> falso.
  `canTakeItemThroughFace`: `face == DOWN && slot == 0`, cella presente e carica >= capacita' della config.
- `public void dropContents()`: `Handouts.dropStill(level, Vec3.atCenterOf(worldPosition), ...)` per la cella e per la
  polvere, poi i due slot vuoti; la riserva non si rimborsa; nulla se il caricatore e' vuoto o non e' in un livello.
- Javadoc della classe: un paragrafo "Tramogge" (facce, dove stanno i limiti, i due fatti delle tramogge, perche'
  `setItem` non taglia).

**`content/ChargerBlock.onRemove`** (`public`, firma del brief): solo se `!state.is(newState.getBlock())` e in quel
punto c'e' una `ChargerBlockEntity`: `dropContents()` e `level.updateNeighbourForOutputSignal(pos, this)`; poi sempre
`super.onRemove(...)`. Javadoc: `onRemove` scatta anche a un cambio di proprieta' (`lit`, `cell`), e li' non cade nulla.

**`gametest/ChargerGameTests`**: sei test in coda al generatore (196 -> 202), con i nomi del brief:
`hopperAboveInsertsOneCell`, `hopperAtTheSideAddsDustToANonEmptySlot`, `hopperBelowTakesOnlyAFullCell`,
`fullHopperBelowNeitherLosesNorDuplicatesTheCell`, `facesAcceptOnlyWhatTheyShould`, `breakingDropsTheCellAndTheDust`.
Aiuti privati nuovi: `hopper(helper, relative, facing)` e `count(Container, Item)`, quelli del brief. Le tramogge sono
vere (`Blocks.HOPPER`), e ogni test le svuota (`clearContent()`) prima di toglierle.

**`docs/compat.md`**: sezione di `CompatBlockEntity`: riga **Uso** (il caricatore e' un `WorldlyContainer` del gioco
base, senza codice per loader) e, sotto, il paragrafo "Tramogge (parte 3c-2)" con la tabella delle facce, i limiti, i
due fatti delle tramogge e la conseguenza (`setItem`, e quindi `setCell`, non taglia mai), i tubi delle altre mod, i
test; "Schema NBT", paragrafo del caricatore: la frase "Una tramoggia non taglia: una cella che entra da sopra tiene la
sua carica anche oltre la capacita', e vale piena" e il test della tramoggia fra quelli citati; sezione di
`CompatEntityBlock`: la rottura (`onRemove`, `dropContents`) nella riga **Uso**, e `onRemove` non e' piu' "piu'
avanti" nella riga **A cosa serve**.

## Che cosa ho provato e con quali risultati

### TDD

**RED** (Step 2), `bash tools/run_gametests.sh 1.21.1-neoforge` con i soli test nuovi: non compila, come atteso (la
block entity non e' ancora un `Container`):

```
ChargerGameTests.java:894: error: incompatible types: ChargerBlockEntity cannot be converted to Container
            h.assertTrue(count(c, cellItem) + count(hopper, cellItem) == 2 && itemsIn(h).isEmpty(),
ChargerGameTests.java:1046: error: cannot find symbol
        helper.assertTrue(Arrays.equals(c.getSlotsForFace(Direction.UP), new int[] {0})
...
BUILD FAILED in 2s
FAIL 1.21.1-neoforge: Gradle e' uscito con 1
```

**GREEN** (Step 3-4), dopo l'implementazione: `bash tools/run_gametests.sh 1.21.1-neoforge` ->
`OK 1.21.1-neoforge: 202 test obbligatori passati, nessun avviso fuori elenco`; poi le quattro varianti (sotto). I sei
test sono passati al primo giro sulle quattro varianti: nessuna correzione ai tempi.

**Controprova** (il RED era solo di compilazione, quindi ho guastato il codice di proposito su 1.21.1-neoforge, due
giri, e poi ripristinato i file: `cmp` con le copie buone, identici):

- `canTakeItemThroughFace` senza il controllo della carica, e `getItem(1)` che da' una copia: 3 test falliti,
  `faces_accept_only_what_they_should` ("a cell with 9999 RE can be taken from below"),
  `hopper_below_takes_only_a_full_cell` ("cell one RE short of full: charger 0 minecraft:air with 0 RE"),
  `hopper_at_the_side_adds_dust_to_a_non_empty_slot` ("dust in the charger after 30 ticks: 3, not more than 3").
- `onRemove` senza `!state.is(newState.getBlock())`: 10 test falliti, fra cui
  `lit_toggles_twice_without_losing_the_cell` ("items on the ground [ItemEntity['Redstone Cell'..."),
  `breaking_drops_the_cell_and_the_dust`, `full_hopper_below_neither_loses_nor_duplicates_the_cell`.

### Verifiche di fine task (sull'albero del commit)

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
  :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`:

  ```
  > Task :1.21.1-fabric:collectJar
  > Task :1.21.1-neoforge:collectJar

  BUILD SUCCESSFUL in 25s
  52 actionable tasks: 24 executed, 1 from cache, 27 up-to-date
  ```
  Nessuna riga `warning:` o `error:` nel log.
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `bash tools/check_art.sh` -> `OK art` (non richiesto dal brief: il task non tocca cio' che controlla)
- `bash tools/run_gametests.sh` (quattro varianti; girato due volte, allo Step 4 e di nuovo dopo la controprova e la
  build finale, stesso esito):

  ```
  OK 1.20.1-forge: 202 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 202 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 202 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 202 test obbligatori passati, nessun avviso fuori elenco
  ```
  `litTogglesTwiceWithoutLosingTheCell` (Task 4) e `chargerBreaksCleanly` (Task 2) restano verdi. Nessuna riga
  `Charger tick failed` nei log. `tools/log_allowlist.txt` non e' cambiato.

### Che cosa ho visto dei punti "mai girati" del ledger

- **I tempi di una tramoggia vera dentro un GameTest.** Funzionano come dice il brief, sulle quattro varianti: la
  tramoggia senza posto riprova a ogni tick (in 20 tick `c.cell() != before` e' vero sui due caricatori), una mossa
  ogni 8 tick per quella che spinge (il test non conta le mosse: chiede solo "piu' di 3 polveri nel caricatore" e "8 in
  tutto" dopo 30 tick, e passa). Nessun test ha avuto bisogno di tempi piu' larghi.
- **`destroyBlock` sotto gli eventi di Forge e NeoForge** (qui senza giocatore: `level.destroyBlock(abs, false)`):
  `onRemove` scatta sulle quattro varianti e i due oggetti cadono al centro, fermi.
- Con `javap -c` sui jar di Minecraft 1.20.1 e 1.21.1 (mappature Mojang) ho riletto `tryTakeInItemFromSlot`: su 1.20.1
  `copy()` prima di `removeItem` e poi `setItem` con la copia, su 1.21.1 `getCount()`, `removeItem`, `setCount` e
  `setItem` con lo stesso stack. E' cio' che dice il brief.

## File cambiati

- `src/main/java/com/toxineren/exoframes/content/ChargerBlockEntity.java` (+183)
- `src/main/java/com/toxineren/exoframes/content/ChargerBlock.java` (+19, -2)
- `src/main/java/com/toxineren/exoframes/gametest/ChargerGameTests.java` (+306)
- `docs/compat.md` (+19, -4)

## Scelte

1. **`dropContents()` e `clearContent()` chiamano `setChanged()` quando hanno tolto qualcosa.** Il brief lo dice solo
   per `removeItem`; il design (§12.11) dice "ogni cambio di contenuto chiama `setChanged()`". In `onRemove` questo da'
   ai comparatori un avviso in piu' (quello di `setChanged()` e quello esplicito del brief): innocuo, e
   `dropContents()` resta corretto anche se un giorno lo chiama qualcun altro. `removeItemNoUpdate` non lo chiama, come
   dice il brief.
2. **`dropContents()` non fa nulla se la block entity non e' in un livello** (`level == null`): `Handouts.dropStill`
   vuole un livello, e lasciare il contenuto dov'e' e' la scelta che non perde nulla. Un caricatore vuoto esce subito
   (anche sul client, dove la block entity non ha contenuto).
3. **`removeItem` con `amount` sotto 1 non toglie nulla**, anche dallo slot 1 (`ItemStack.split` con un numero
   negativo farebbe crescere lo stack); per uno slot diverso da 0 e 1 da' `ItemStack.EMPTY`, come `getItem`.
4. **`canTakeItemThroughFace` guarda la cella dello slot**, non lo stack passato (il brief scrive `cellEnergy(cell)`):
   la tramoggia passa comunque lo stack avuto da `getItem(0)`, che e' lo stesso oggetto.
5. **`getSlotsForFace` restituisce due array costanti**, come i contenitori del gioco base, non una copia a ogni
   chiamata (le tramogge lo chiamano a ogni tick).
6. **`hopperBelowTakesOnlyAFullCell`: le tre attese (20, 40, 60 tick) sono fissate tutte all'inizio del test**, non
   una dentro l'altra: un `runAfterDelay` chiamato dal corpo di un altro aggiungerebbe una voce alla mappa che il gioco
   sta scorrendo. I ritardi sommati restano 60 tick, il limite delle Global Constraints.
7. **Asserzioni in piu' rispetto al brief**, tutte nello stesso senso: `itemsIn(helper).isEmpty()` anche nei test delle
   tramogge sopra, di lato e sotto; in `breakingDropsTheCellAndTheDust` il blocco e' aria e la block entity non c'e'
   piu', e dopo `Clearable.tryClear` i due slot sono vuoti; in `facesAcceptOnlyWhatTheyShould` anche
   `canPlaceItem(0, una cella)` vero accanto al falso per due celle; nel test della tramoggia senza posto una premessa
   sulle cariche di partenza (10 000 e 40 000).
8. **Dove sta in `docs/compat.md` la frase sui tubi delle altre mod**: nel paragrafo "Tramogge" della sezione di
   `CompatBlockEntity`, accanto al fatto che la spiega (Forge e NeoForge cercano prima la loro capacita'), non nel
   paragrafo dello schema NBT.
9. **Due ritocchi a `docs/compat.md` che il brief non elenca**: la rottura nella riga **Uso** di `CompatEntityBlock`
   (dove sta gia' il clic), e "e piu' avanti `onRemove`" diventato "`onRemove`" (la frase era diventata falsa).

## Scostamenti dal brief

Nessuno: nomi, firme, valori e ordine dei passi sono quelli del brief. Le aggiunte sono in "Scelte".

## Autorevisione

- Completezza: tutti i metodi di "Produces", i sei test, i cinque punti dello Step 5, le verifiche dello Step 6.
- Nessun `isCreative()`, nessuna direttiva Stonecutter fuori da `compat/impl/`, nessun `compat.impl` nominato da
  `content/` o `gametest/` (`OK sources`); identificatori ed eccezioni in inglese, Javadoc in italiano con apostrofi
  ASCII; nessuna riga oltre 120 colonne nei file toccati.
- Conteggi: fra tramoggia e caricatore le celle restano due (`hopperAboveInsertsOneCell`), le polveri otto
  (`hopperAtTheSideAddsDustToANonEmptySlot`), la cella una con la sua carica (`fullHopperBelow...`, 10 000 e 40 000).

## Dubbi e segnalazioni

1. **`/item replace block` e `/loot replace block` su un caricatore (solo operatori).** Con `javap` ho visto che
   `ItemCommands` chiama `Container.setItem` senza chiedere `canPlaceItem`, sulle due versioni. Con il contratto del
   brief (`setItem` lancia per cio' che lo slot non tiene) un operatore che scrive, per esempio, un bastone nello slot
   0 fa lanciare `IllegalArgumentException` dentro il comando; il gioco base raccoglie le eccezioni dei comandi e
   risponde "comando fallito", ma **non l'ho provato in gioco**. Non e' una perdita ne' una duplicazione; lo segnalo
   perche' e' un comportamento visibile che nessun test fissa. Con una cella valida il comando sostituisce quella che
   c'e', come in ogni contenitore del gioco base.
2. **Una tramoggia sotto senza posto tiene il chunk "da salvare" a ogni tick** (toglie e rimette la cella, due
   `setChanged()` e due avvisi ai comparatori per tick): e' cio' che fa il gioco base con qualunque contenitore sopra
   una tramoggia senza posto, e viene dai due fatti del brief. Nessun rimedio qui.
3. `ChargerGameTests.java` e' ora di 1316 righe (era 1010): e' il minor [T5-F3] gia' nel ledger, il piano tiene tutti i
   test del caricatore in questa classe.
4. Non ho provato nel client vero (il brief non lo chiede per questo task): cio' che il GameTest non vede (la
   tramoggia posata da un giocatore, la rottura con il piccone e il bottino acceso, che fa cadere anche il blocco) resta
   alla checklist del Task 10.
