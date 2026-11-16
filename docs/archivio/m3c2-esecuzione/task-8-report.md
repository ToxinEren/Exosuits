# Task 8 — I dati in gioco: ricette, sblocchi, bottino, piccone

Stato: DONE. BASE `b5db281cf7d73f5f109b6372dacb9e92cb47b18b`, HEAD `d7454a271d306209de222f5798b59da179e01789`.
Un commit: `d7454a2 test: ricette, sblocchi, bottino e piccone del caricatore provati in mondo`.
All'inizio HEAD era BASE e l'albero di lavoro era pulito: nessun tentativo precedente da riprendere.
`<tmp>` qui sotto e' la cartella temporanea della sessione (fuori dal repository).

## Che cosa ho implementato

**`gametest/DataGameTests.java`** (nuova, 5 test, registrata in `RedstoneExoframes.init()`): Javadoc sui dati per
versione (design §12.12); tabella privata `RECIPES` delle sette ricette (record `RecipeCase`: oggetto, categoria,
oggetto atteso, righe della forma), scritta a mano con i valori di "Ricette" del piano, non letta dai file.

- `everyRecipeIsFoundByKey`: per le sette `recipeResult(server, Ids.of(<oggetto>))` presente, oggetto atteso,
  `getCount() == 1`; per `redstone_cell` `cellEnergy == 0` e `!hasData`; in piu' una chiave che non c'e' da' vuoto.
- `everyRecipeCraftsFromItsShape`: per le sette `craft(level, 3, 3, craftingGrid(<righe>))` e' l'oggetto atteso, uno;
  controlli: la griglia del telaio con la casella 1 (premessa: rame) sostituita da `Items.GOLD_INGOT` → vuoto; nove
  caselle vuote → vuoto.
- `everyRecipeHasItsUnlockAdvancement`: `hasAdvancement(server, Ids.of("recipes/<categoria>/<oggetto>"))` per le sette;
  `!hasAdvancement(server, Ids.of("recipes/misc/exoframe"))`.
- `aCopperIngotUnlocksTheSevenRecipes`: `survivalPlayer`; premessa: nessuna delle sette nel libro; un lingotto di rame
  nell'inventario e `CriteriaTriggers.INVENTORY_CHANGED.trigger(p, p.getInventory(), copper)` → le sette nel libro.
- `chargerNeedsAPickaxeToDropItself`: il caricatore in `MINEABLE_WITH_PICKAXE`, la pietra anche, il caricatore non in
  `MINEABLE_WITH_AXE`; `hasCorrectToolForDrops` falso a mano vuota e vero con il piccone di legno; caricatore in
  (3, 2, 3) con `setCell(cell(1_000))` e 3 polveri, `p.gameMode.destroyBlock(abs)` con il piccone → vero, aria, a terra
  esattamente tre stack: un `charger` (uno), una cella con 1 000 RE, 3 polveri; tolti. Secondo caricatore vuoto in
  (5, 2, 3), a mano vuota: `destroyBlock` → vero, aria, nessun oggetto a terra.

**`compat/GameTests` e `compat/impl/GameTestsImpl`** ("Esiste solo per i test", direttive dentro i metodi):
`Optional<ItemStack> recipeResult(MinecraftServer, ResourceLocation)`, `ItemStack craft(ServerLevel, int, int,
List<ItemStack>)`, `boolean hasAdvancement(MinecraftServer, ResourceLocation)`. Su 1.20.1 `craft` usa un
`TransientCraftingContainer` con un `AbstractContainerMenu` anonimo senza slot (`quickMoveStack` → `EMPTY`,
`stillValid` → falso); su 1.21.1 `CraftingInput.of`. Le firme del brief ("verificate con javap") compilano cosi' come
sono sulle quattro varianti.

**`gametest/GameTestKit.craftingGrid(String... rows)`**: nove stack per righe, lista nuova e modificabile; lettere
`I C R r Q _`; `IllegalArgumentException` per un altro carattere, una riga vuota o piu' lunga di tre, piu' di tre righe.

**`docs/compat.md`**, sezione `GameTests`: i tre aiuti in "A cosa serve" e in "Condizione" (i tipi per versione di
`RecipeManager.byKey`, della griglia e di `getRecipeFor`, di `ServerAdvancementManager.get`/`getAdvancement`);
"Tabella API": la riga `CustomRecipe / CraftingContainer → CraftingInput` resta "—" per il codice della mod, con la
nota che solo i test costruiscono una griglia e passano da `GameTests.craft`.

## Che cosa ho provato e con quale esito

Verifiche di fine task, sull'albero poi committato (nessuna modifica fra le verifiche e il commit):

```
./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build \
    :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar
> Task :1.20.1-forge:compileJava
> Task :1.20.1-fabric:compileJava
> Task :1.21.1-fabric:compileJava
> Task :1.21.1-neoforge:compileJava
BUILD SUCCESSFUL in 6s
52 actionable tasks: 18 executed, 34 up-to-date

bash tools/check_sources.sh   → OK sources
bash tools/check_jars.sh      → OK jars
bash tools/check_data.sh      → OK data
bash tools/check_art.sh       → OK art   (non richiesto dal task; girato una volta prima dell'ultimo ritocco a un Javadoc)

bash tools/run_gametests.sh
OK 1.20.1-forge: 207 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 207 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 207 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 207 test obbligatori passati, nessun avviso fuori elenco
```

(La build completa era gia' passata una prima volta in 34 s, `52 actionable tasks: 35 executed`; le due successive,
dopo ritocchi ai soli commenti, ricompilano le quattro varianti e chiudono in 6 s.)

## Evidenza TDD

**RED** (Step 2, dopo aver scritto solo `DataGameTests.java`): `bash tools/run_gametests.sh 1.21.1-neoforge`

```
> Task :1.21.1-neoforge:compileJava FAILED
.../gametest/DataGameTests.java:100: error: cannot find symbol
            Optional<ItemStack> found = Compat.gameTests().recipeResult(server, recipe.recipeId());
  symbol:   method recipeResult(MinecraftServer,ResourceLocation)
  location: interface GameTests
.../gametest/DataGameTests.java:122: error: cannot find symbol
            ItemStack crafted = Compat.gameTests().craft(level, 3, 3, craftingGrid(recipe.rows()));
  symbol:   method craftingGrid(String[])
BUILD FAILED in 2s
FAIL 1.21.1-neoforge: Gradle e' uscito con 1
```

Nove errori, tutti "cannot find symbol" sui quattro nomi attesi: `recipeResult`, `craft`, `hasAdvancement`,
`craftingGrid`. E' il fallimento che il brief prevede: gli aiuti non esistevano.

**GREEN** (Step 3-4): `bash tools/run_gametests.sh 1.21.1-neoforge` → `All 207 required tests passed :)`,
`OK 1.21.1-neoforge: 207 test obbligatori passati, nessun avviso fuori elenco`; poi `1.20.1-forge` → `OK 1.20.1-forge:
207 ...`; poi le quattro varianti (sopra). Tutti e cinque i test sono passati al primo colpo sulle quattro varianti:
nessuna correzione a `gen_data.py` e' servita.

**Prova che i test vedono i dati** (due mutazioni temporanee su 1.21.1-neoforge, poi ripristinate; `git status` dopo
ciascuna mostrava solo i file del task, e `bash tools/check_data.sh` → `OK data`):

1. Le quattro cartelle di dati di 1.21.1 rinominate con i nomi di 1.20.1 (`recipe`→`recipes`,
   `advancement`→`advancements`, `loot_table`→`loot_tables`, `tags/block`→`tags/blocks`): `5 required tests failed`,
   esattamente i cinque nuovi:
   - `every_recipe_is_found_by_key failed ... no recipe with the key redstone_exoframes:exoframe`
   - `every_recipe_crafts_from_its_shape failed ... the shape of exoframe crafts 0 minecraft:air, not one exoframe`
   - `every_recipe_has_its_unlock_advancement failed ... no unlock advancement redstone_exoframes:recipes/equipment/exoframe`
   - `a_copper_ingot_unlocks_the_seven_recipes failed ... a copper ingot did not unlock redstone_exoframes:exoframe`
   - `charger_needs_a_pickaxe_to_drop_itself failed ... the charger is not in mineable/pickaxe`
2. Solo `loot_table`→`loot_tables`: `1 required tests failed`:
   `charger_needs_a_pickaxe_to_drop_itself failed ... the charger broken with a pickaxe dropped [1
   redstone_exoframes:redstone_cell, 3 minecraft:redstone], not one charger, one cell with 1000 RE and 3 redstone dust`.

Le mutazioni non lasciano traccia nel repository (i log stavano in `<tmp>`).

## Cio' che nessuno aveva mai fatto girare (nota del registro) e che cosa ho visto

- Il JSON degli sblocchi sulle due versioni: caricato dalle quattro varianti (`hasAdvancement` vero per le sette),
  senza righe WARN o ERROR (`run_gametests.sh`: "nessun avviso fuori elenco").
- `INVENTORY_CHANGED` per il giocatore finto: il criterio scatta sulle quattro varianti e le sette ricette entrano nel
  libro nello stesso tick; nessun avviso dal pacchetto del libro delle ricette mandato al giocatore finto.
- `ServerPlayerGameMode.destroyBlock` con il giocatore finto sotto gli eventi di Forge e NeoForge: restituisce vero,
  il blocco diventa aria, il bottino segue l'attrezzo (con il piccone il blocco cade, a mano vuota no) sulle quattro
  varianti; cella e polvere cadono una volta sola anche per questa strada (`onRemove`).

## File cambiati

- `src/main/java/com/toxineren/exoframes/gametest/DataGameTests.java` (nuovo)
- `src/main/java/com/toxineren/exoframes/compat/GameTests.java`
- `src/main/java/com/toxineren/exoframes/compat/impl/GameTestsImpl.java`
- `src/main/java/com/toxineren/exoframes/gametest/GameTestKit.java`
- `src/main/java/com/toxineren/exoframes/RedstoneExoframes.java`
- `docs/compat.md`

## Scelte

1. `everyRecipeIsFoundByKey` controlla anche che una chiave senza ricetta (`no_such_recipe`) dia vuoto: il contratto di
   `recipeResult` dice "vuoto se non c'e'" e senza questo nessun test lo toccava.
2. La premessa di `aCopperIngotUnlocksTheSevenRecipes` guarda tutte e sette le ricette, non solo `exoframe`: e' un
   sovrainsieme di quella del brief e costa una riga.
3. `craft` lancia `IllegalArgumentException` se `grid.size() != width * height` (o un lato minore di 1): il brief dice
   che la griglia "ha `width * height` stack" senza dire che cosa succede altrimenti; una griglia storta darebbe un
   risultato senza senso su una versione e un'eccezione del gioco sull'altra.
4. `craftingGrid`: al piu' tre righe (anche meno: quelle che mancano sono vuote), ciascuna da uno a tre caratteri;
   una riga vuota, una troppo lunga o una quarta riga → `IllegalArgumentException`, come un carattere sconosciuto. La
   lista e' nuova e modificabile (il test ne sostituisce una casella). Lato della griglia in una costante,
   `CRAFTING_GRID_SIDE`.
5. La griglia vuota del controllo e' scritta per esteso, `craftingGrid("___", "___", "___")`.
6. `DataGameTests` ha un suo aiuto privato `charger(helper, pos)` (quattro righe, uguale a quello privato di
   `ChargerGameTests`): il brief non mette quell'aiuto fra quelli di `GameTestKit`, e spostarlo avrebbe toccato un file
   fuori dal task.
7. In `chargerNeedsAPickaxeToDropItself` il giocatore sta dentro la struttura (`standFacingNorth`, due blocchi a sud
   del primo caricatore), come chiede `GameTestKit` per ogni giocatore finto, anche se `destroyBlock` non guarda la
   distanza. Gli oggetti a terra devono essere esattamente tre e ciascuno del tipo, numero e carica attesi; la loro
   posizione non si controlla (il bottino del blocco cade con uno scarto casuale; cella e polvere al centro le prova
   gia' `breakingDropsTheCellAndTheDust`).
8. Alla fine il test dello sblocco svuota l'inventario del giocatore finto; quello del piccone lascia il giocatore a
   mano vuota, i due blocchi sono aria e a terra non resta nulla.
9. `docs/compat.md`, riga della tabella: resta "—" e fra parentesi dice perche' (le ricette sono dati
   `crafting_shaped`, la mod non tocca la griglia) e che i test passano da `GameTests.craft`.

## Scostamenti dal brief

Nessuno nella sostanza: i passi sono stati fatti nell'ordine scritto e le firme del brief valgono cosi' come sono.
In piu' rispetto al brief: il controllo della scelta 1, la premessa allargata della scelta 2, le due mutazioni
temporanee di prova e un giro di `check_art.sh`.

## Autorevisione

- Completezza: i cinque test, i tre aiuti, `craftingGrid`, la registrazione e `docs/compat.md` ci sono; 207 su quattro.
- Direttive Stonecutter solo in `compat/impl/GameTestsImpl.java`; `DataGameTests` e `GameTestKit` non ne hanno e non
  nominano `compat.impl` (`OK sources`).
- Nessuna riga nuova in `tools/log_allowlist.txt`; nessun avviso nuovo nei quattro log.
- Trovato e corretto rileggendo: due righe di Javadoc andate a capo male o oltre i 120 caratteri in `DataGameTests`.

## Dubbi e punti aperti

Nessuno che fermi il task. Da sapere:

- `ChargerGameTests.charger` e `DataGameTests.charger` sono due copie dello stesso aiuto privato (scelta 6): un
  candidato per `GameTestKit` alla revisione finale.
- Il libro delle ricette "vero" (il client che mostra le ricette sbloccate) non si vede dal GameTest: qui si prova che
  il server le ha date al giocatore; la resa nel client resta alla checklist per il PC.
