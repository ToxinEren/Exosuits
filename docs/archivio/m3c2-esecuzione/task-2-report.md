# Task 2 — Le basi per versione e il caricatore vuoto

Stato: **DONE**. BASE `1846274fe8707f6dede8bd132be755b875b63097`, HEAD `b5ac1b51152cb32c0f3fcfad7069834e2af36125`.
Un commit: `b5ac1b5 content: basi per versione di blocco, block entity e oggetto del blocco; caricatore vuoto`.

`<tmp>` qui sotto è la cartella temporanea della sessione
(`<area-temporanea>/scratchpad`); nulla di ciò che contiene è nel repository.

## Che cosa è stato fatto

- **Tre coppie di cuciture** (classe astratta pubblica senza direttive in `compat/`, base con le direttive in
  `compat/impl/`, sul modello di `CompatEntity`/`EntityBase` e `CompatItem`/`ItemBase`):
  - `CompatEntityBlock` ← `EntityBlockBase extends Block implements EntityBlock`: il testo del brief alla lettera
    (`onClick` neutro, di base `PASS`; `useItemOn` protetto su 1.21.1 con la mappa dei risultati; `use` pubblico su
    1.20.1; `useWithoutItem` non ridefinito).
  - `CompatBlockEntity` ← `BlockEntityBase extends BlockEntity`: `readData`/`writeData` astratti e protetti; su 1.21.1
    `protected final loadAdditional(tag, registries)` e `saveAdditional(tag, registries)`, su 1.20.1
    `public final load(tag)` e `protected final saveAdditional(tag)`; ciascuno chiama prima `super`, poi il metodo neutro.
  - `CompatBlockItem` ← `BlockItemBase extends BlockItem`: come `ItemBase`, costruttore `(Block, Item.Properties)`,
    `tooltip(stack, lines, advanced)` vuoto di base.
- **`compat/GameTests`** + `GameTestsImpl`: `saveBlockEntity(level, pos)` (`saveWithFullMetadata`, con
  `level.registryAccess()` su 1.21.1; `IllegalStateException("no block entity at " + pos)`) e
  `loadBlockEntity(level, pos, saved)` (`BlockEntity.loadStatic` con lo stato del blocco in quel punto, poi
  `level.setBlockEntity`; `IllegalStateException` se `loadStatic` dà `null`). Direttiva solo dove passa il provider.
- **`compat/NbtKeys`**: `CHARGER_HAS_CELL`, `CHARGER_CELL_ENERGY`, `CHARGER_REDSTONE`, `CHARGER_RESERVE`,
  `CHARGER_CARRY_MILLI`, ciascuna con il suo Javadoc; una frase nel Javadoc della classe (le chiavi del caricatore
  stanno alla radice del tag della sua block entity, non sotto `ROOT`). `CARRY_MILLI` e il suo Javadoc non toccati.
- **`content/`**: `ModBlocks` (`BLOCKS`, `CHARGER` con le proprietà di "Nomi comuni", `register()`),
  `ModBlockEntities` (`BLOCK_ENTITY_TYPES`, `CHARGER` con `build(null)`, `register()`), `ChargerBlock` (tre
  proprietà, quattro chiavi di stato, stato predefinito, `newBlockEntity`, `getStateForPlacement`,
  `createBlockStateDefinition`, `rotate` e `mirror` pubblici, `onClick` del primo passo), `ChargerBlockEntity`
  (riserva limitata in lettura a [0, `ConfigValidation.MAX_RE_PER_REDSTONE`], `reserve()`, `onlyOpCanSetNbt()` vero),
  `ChargerItem` (`TOOLTIP_KEYS`, cinque righe grigie), `ModItems.CHARGER`.
- **Registrazioni**: in `RedstoneExoframes.init()` `ModBlocks.register();` subito prima di `ModItems.register();`,
  `ModBlockEntities.register();` subito dopo, ciascuna con il commento sull'ordine;
  `Compat.gameTests().register(ChargerGameTests.class);` in coda. In `ModCreativeTabs.register()`
  `CreativeTabRegistry.append(MAIN, ModItems.CHARGER);` per ultima; Javadoc della classe aggiornato.
- **Risorse**: `blockstates/charger.json` (16 varianti `cell=<b>,facing=<d>,lit=<b>`; `y` 90/180/270, omessa per
  `north`), `models/block/charger.json`, `charger_cell.json`, `charger_lit.json` (`minecraft:block/orientable`),
  `models/item/charger.json`; cinque texture 16×16 opache da `tools/gen_placeholders.py`
  (`charger_top`, `charger_side`, `charger_front`, `charger_front_cell`, `charger_front_lit`, tacche
  `(224, 32, 28)`), elencate in `outputs()`.
- **Testi**: le sette chiavi di questo task nei due file di lingua, nelle posizioni di "Nomi, numeri e testi comuni".
- **`gametest/`**: `GameTestKit.cell(int)` ed `energy(ItemStack)` spostati da `CellGameTests` (corpi invariati),
  `clickBlock`, `itemsIn`; `ChargerGameTests` con i sei test del brief e gli aiuti privati `POS`, `charger`,
  `stateAt`, più `readLang` e la lista `CHARGER_TEXT_KEYS`.
- **Documenti**: `docs/compat.md` (tre sezioni nuove dopo quella di `CompatItem`, con la tabella dei risultati di
  1.21.1 e la nota sull'accovacciarsi; riga **Uso** di `CompatItem` al presente; i due aiuti in "A cosa serve" e in
  "Condizione" di `GameTests`; tre righe della "Tabella API"; riga `reserve` nello "Schema NBT");
  `tools/README.md` ("diciotto file" due volte, riga delle cinque texture del blocco).

## Prove

### TDD

- **RED** (Step 2), dopo aver scritto `ChargerGameTests`, gli aiuti di `GameTestKit` e la riga di registrazione, prima
  di ogni classe del mod: `bash tools/run_gametests.sh 1.21.1-neoforge` → uscita 1, 84 errori di compilazione, tutti in
  `ChargerGameTests.java`, perché mancavano le classi e gli aiuti da scrivere:

  ```
  ChargerGameTests.java:10: error: cannot find symbol
  import com.toxineren.exoframes.content.ChargerBlock;
    symbol:   class ChargerBlock
  ChargerGameTests.java:96: error: cannot find symbol
          p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHARGER.get()));
    symbol:   variable CHARGER
  BUILD FAILED in 5s
  FAIL 1.21.1-neoforge: Gradle e' uscito con 1
  ```

- **GREEN** (Step 5), dopo l'implementazione e le risorse, al primo tentativo:

  ```
  OK 1.21.1-neoforge: 181 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-forge: 181 test obbligatori passati, nessun avviso fuori elenco
  ```

### Step 4

`bash tools/check_art.sh` → `OK art`; i due comandi `python3 -I -c ...` sui cinque JSON e sulle 16 varianti: nessuna
uscita.

### Step 7, client vero

Comando del brief alla lettera, una variante alla volta, cartelle `<tmp>/shot-fabric120` e `<tmp>/shot-neoforge121`.

| Variante | Uscita | `log-check.txt` | `grep -E '(WARN\|ERROR)' client.log \| grep -i charger` | Durata |
|---|---|---|---|---|
| `1.20.1-fabric` | 0 | 0 byte | nessuna riga (anche in `server.log`) | 73 s |
| `1.21.1-neoforge` | 0 | 0 byte | nessuna riga (anche in `server.log`) | 75 s |

Ultime righe (uguali sulle due):

```
[73s] log: nessun avviso o errore del mod fuori dall'elenco degli avvisi voluti
[73s] fatto in 73 s, chiusura
```

Scatti guardati uno per uno, uguali sulle due varianti:

- `caricatori.png`: tre cubi grigi affiancati con il fronte verso il giocatore: al centro (x 0) l'alloggiamento vuoto
  e scuro, a sinistra (x 1) la cella con le tacche spente, a destra (x −1) la cella con le tre tacche rosse; sommità
  con la botola; nessun cubo viola e nero. Nella barra degli oggetti l'icona del blocco, e il blocco in mano.
- `caricatore-inventario.png`: la schermata della creativa aperta, con l'icona del caricatore nel primo posto della
  barra.

Gli scatti non sono nel repository (il brief non elenca file di scatti per questo task).

### Step 8, verifiche di fine task (ultima esecuzione, sui sorgenti del commit)

```
./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build \
    :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar
BUILD SUCCESSFUL in 2s
52 actionable tasks: 2 executed, 5 from cache, 45 up-to-date
```

(la prima esecuzione sugli stessi sorgenti: `BUILD SUCCESSFUL in 34s`, `52 actionable tasks: 32 executed, 1 from cache,
19 up-to-date`, con i quattro `:test` eseguiti)

```
bash tools/check_sources.sh   → OK sources
bash tools/check_jars.sh      → OK jars
bash tools/check_art.sh       → OK art
bash tools/run_gametests.sh   →
OK 1.20.1-forge: 181 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 181 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 181 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 181 test obbligatori passati, nessun avviso fuori elenco
```

Nei quattro log nessuna riga `No data fixer registered for charger` (`grep -ci` → 0), neanche sulle due Fabric.
I quattro jar di `build/libs/all/` contengono blockstate, tre modelli del blocco, modello dell'oggetto e cinque
texture del caricatore.

### Ciò che la nota del registro chiedeva di riferire ("never run by anyone before execution")

- **`useItemOn` e `destroyBlock` con il giocatore finto sotto gli eventi di Forge e NeoForge**: funzionano come su
  Fabric. `ServerPlayerGameMode.useItemOn` con il giocatore finto in sopravvivenza posa il blocco dall'oggetto (lo
  stack si consuma), fa arrivare il clic a `onClick` per la mano principale e per la secondaria, e lascia passare
  all'oggetto in mano dopo un `PASS`; `ServerLevel.destroyBlock(pos, false)` toglie blocco e block entity. Nessun
  avviso nei log.
- **File di lingua dal class loader**: `RedstoneExoframes.class.getClassLoader().getResourceAsStream(
  "assets/redstone_exoframes/lang/<file>")` li trova su `1.20.1-forge`, `1.21.1-neoforge` e `1.20.1-fabric` (visto con
  una riga di log provvisoria, tolta prima delle verifiche finali: il file è stato rimesso identico, `cmp`, e nei jar
  la riga non c'è); su `1.21.1-fabric` il test passa, non ho guardato per quale delle due strade. Il ripiego su
  `Class.getResourceAsStream("/assets/…")` resta nel test, mai servito sulle tre varianti guardate.
- **Firme del gioco**: riguardate con `javap` sui jar vanilla 1.20.1 e 1.21.1 e su quelli con le patch di Forge
  47.4.29 e NeoForge 21.1.266 (`BlockBehaviour.use`/`useItemOn`/`useWithoutItem`/`rotate`/`mirror`,
  `BlockEntity.load`/`loadAdditional`/`saveAdditional`/`saveWithFullMetadata`/`loadStatic`/`onlyOpCanSetNbt`,
  `ServerPlayerGameMode.useItemOn`): sono quelle del brief.

## File cambiati

35 file, +1135 −23 (`git show --stat b5ac1b5`).

- Nuovi: `compat/CompatEntityBlock.java`, `CompatBlockEntity.java`, `CompatBlockItem.java`;
  `compat/impl/EntityBlockBase.java`, `BlockEntityBase.java`, `BlockItemBase.java`; `content/ModBlocks.java`,
  `ModBlockEntities.java`, `ChargerBlock.java`, `ChargerBlockEntity.java`, `ChargerItem.java`;
  `gametest/ChargerGameTests.java`; in `src/main/resources/assets/redstone_exoframes/`: `blockstates/charger.json`,
  `models/block/charger.json`, `charger_cell.json`, `charger_lit.json`, `models/item/charger.json`,
  `textures/block/charger_top.png`, `charger_side.png`, `charger_front.png`, `charger_front_cell.png`,
  `charger_front_lit.png`.
- Modificati: `RedstoneExoframes.java`, `compat/GameTests.java`, `compat/NbtKeys.java`,
  `compat/impl/GameTestsImpl.java`, `content/ModItems.java`, `content/ModCreativeTabs.java`,
  `gametest/GameTestKit.java`, `gametest/CellGameTests.java`, `lang/en_us.json`, `lang/it_it.json`,
  `tools/gen_placeholders.py`, `tools/README.md`, `docs/compat.md`.

## Scelte

1. **Pulizia dei test**: ogni test che posa un blocco rimette aria prima di `succeed()` (il caricatore in `POS`, la
   pietra sotto nel test della posa), come vogliono i Global Constraints per i test di questa parte.
2. **`emptyHandClickTellsNoCell`** sposta il giocatore nella struttura (`standFacingNorth`, come nel test della posa)
   e controlla per primo che abbia le mani vuote: la regola di `GameTestKit` vuole i giocatori finti dentro la
   struttura del test, e il test conta su una mano vuota che nessuno gli garantisce.
3. **`chargerEntitySavesAndLoads`** chiede anche `saved.contains(CHARGER_RESERVE)`: `getInt` dà 0 anche per una chiave
   assente, e così il test prova che `writeData` scrive davvero.
4. **`ChargerBlockEntity` solo con ciò che il brief elenca "in questo task"**: niente `MAX_DUST`, slot, `serverTick`
   (arrivano con il Task 4). Le quattro chiavi `STATUS_*` ci sono tutte, come dice il brief; nei file di lingua c'è
   solo `no_cell` (le altre tre sono del Task 5) e `CHARGER_TEXT_KEYS` contiene solo `STATUS_NO_CELL` e le cinque del
   suggerimento.
5. **Variante `cell=false,lit=true`** del blockstate → `charger_lit`, per la regola del brief ("`charger_lit` se
   `lit=true`"); è uno stato che il gioco non produce (si accende solo con una cella).
6. **Texture**: piastra `(132, 136, 142)` con bordo `(56, 58, 66)`, luce in alto e a sinistra, ombra in basso e a
   destra, quattro rivetti; lato con una scanalatura, sommità con una botola quadrata; fronte con cornice e
   alloggiamento `(26, 26, 32)`; la cella con le tinte grigio-rame già nel generatore; tacche scure da spenta, rosse
   `(224, 32, 28)` da accesa. Una funzione `solid()` del generatore lancia se un pixel non ha alfa 255: "tutte opache"
   è così una proprietà controllata a ogni generazione, e quindi da `check_art.sh`.
7. **Messaggio di `loadBlockEntity`** quando `loadStatic` dà `null`: `"no block entity loaded at <pos> from <tag>"` (il
   brief fissa solo il tipo dell'eccezione).
8. **Nessun `@SuppressWarnings("deprecation")`** su `use`, `rotate`, `mirror` (deprecati in `BlockBehaviour` di
   1.20.1, nel senso "si ridefiniscono, non si chiamano"): il codice esistente fa lo stesso
   (`ExoframeEntity.isPushedByFluid`), e la riga di javac `Note: Some input files use or override a deprecated API`
   c'era già a BASE; nessun avviso nuovo nel log dei test.
9. **Javadoc in ASCII**: la mappa dei risultati in `EntityBlockBase` è scritta a parole ("da'"), senza frecce; la
   tabella con le frecce sta in `docs/compat.md`.
10. **`docs/compat.md`**: nella sezione di `CompatEntityBlock` anche la regola dei metodi `public` nel codice comune
    (dai Global Constraints) e il motivo per cui non si estende `BaseEntityBlock`; nella riga della tabella di
    `saveAdditional/load` una frase su `getUpdateTag`, che quella riga nomina e che la base non tocca.
11. **Javadoc di `GameTestKit`**: l'elenco delle classi dei test nomina ora `ChargerGameTests`.

## Scostamenti dal brief

Nessuno nel codice consegnato. Due cose fatte in più del testo, senza cambiare ciò che il task consegna:

- una riga di log provvisoria in `readLang` per sapere quale strada legge i file di lingua (la nota del registro lo
  chiedeva), tolta prima delle verifiche finali e mai committata;
- una ricompilazione di due varianti con `-Xlint:deprecation` da uno script di init in `<tmp>` (nessun file del
  repository toccato), per vedere quali avvisi di deprecazione sono nuovi.

## Autorevisione

- Completezza: ogni voce di "Produces" e ogni step del brief ha il suo pezzo; i sei test sono quelli del brief con le
  asserzioni elencate. Nessuna direttiva fuori da `compat/impl/` (`OK sources`, regole 4 e 6): `content/` nomina solo
  `compat.CompatEntityBlock`, `CompatBlockEntity`, `CompatBlockItem`.
- Nessun `isCreative()` nel codice nuovo; `onClick` guarda `isSpectator()` e `isAlive()`.
- Righe entro 120 colonne nei file Java e in `gen_placeholders.py`; identificatori, eccezioni e messaggi dei test in
  inglese, Javadoc in italiano con apostrofi ASCII.
- Trovato e corretto durante il lavoro: tre righe oltre le 120 colonne, frecce non ASCII in due Javadoc, una cartella
  `tools/__pycache__` creata dalla mia anteprima delle texture (ignorata da git, tolta).

## Problemi e dubbi

Nessuno che fermi il task. Per chi coordina:

- Fino al Task 7 il caricatore non ha tabella di bottino né il tag del piccone: in sopravvivenza si scava lento e non
  lascia nulla (`requiresCorrectToolForDrops` senza il tag). È l'ordine del piano.
- `docs/compat.md`, riga 3, dice ancora "e in futuro `block/`": il blocco vive in `content/`, come vuole il design
  §12.16. Non l'ho toccata (fuori dal brief); è una frase per la revisione finale o per il Task 10.
- Tre costanti `STATUS_*` di `ChargerBlock` non hanno ancora un testo né un uso: arrivano con il Task 5, che le
  aggiunge anche a `CHARGER_TEXT_KEYS`.
