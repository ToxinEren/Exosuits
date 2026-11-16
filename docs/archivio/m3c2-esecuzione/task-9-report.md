# Task 9 — La partita dall'inizio alla fine, in sopravvivenza

Stato: DONE. BASE `d7454a271d306209de222f5798b59da179e01789`, HEAD `dd696e0f1b525a6108d0e52db7ccf78b80c59b38`.
Un commit: `dd696e0 test: una partita in sopravvivenza dall'inizio alla fine`.
All'inizio HEAD era BASE e l'albero di lavoro era pulito: nessun tentativo precedente da riprendere.
`<tmp>` qui sotto e' la cartella temporanea della sessione (fuori dal repository).

## Che cosa ho implementato

**`gametest/SurvivalGameTests.java`** (nuova, un test, registrata in `RedstoneExoframes.init()`): la prova di design
§12.15 "dall'inizio alla fine". Javadoc della classe con i nove passi e i numeri, la regola della mano vuota e il
divieto delle scorciatoie di debug (`setStored`, `wearScrap`, `DebugWear` non compaiono nel codice). Nessun codice
della mod cambia.

`survivalFromCraftingToRecharging` chiama un metodo privato per passo; tutto sta in un tick del server.

- Preparazione (`prepare`): pavimento di pietra per `x` e `z` da 1 a 6 a quota 0; `survivalPlayer` in piedi in
  (1.5, 1.0, 1.5), yaw 0; premesse `selected == 0`, mano vuota, inventario vuoto; materie prime con `setItem(9 + i, …)`:
  34 ferro, 14 rame, 2 blocchi di redstone, 12 polveri, 2 quarzi; mano ancora vuota.
- Passo 1 (`craftEverything`): dieci `craftFromInventory` con le righe di "Ricette" (telaio, elmo, torso, due braccia,
  due gambe, due celle, caricatore) → 0 ferro, 0 rame, 0 blocchi, 0 quarzi, 8 polveri; i dieci oggetti contati
  nell'inventario; le due celle con `energy == 0` (gli stack restituiti e quelli nell'inventario); mano vuota; nulla a
  terra.
- Passo 2 (`placeTheFrameAndGetRefused`): l'oggetto telaio fabbricato tolto dall'inventario, mano vuota asserita,
  `useExoframeItem(helper, p, (2, 0, 3), true)` → telaio in (2.5, 1.0, 3.5), mano vuota (consumato); clic a mano vuota
  sul torso → `messages(p, FrameEntry.NO_CELL) == 1`, `wornFrameId(p).isEmpty()`.
- Passo 3 (`placeTheChargerAndChargeTwoCells`): `hold(CHARGER, 1)`, `clickBlock` su (6, 0, 6) → block entity del
  caricatore in (6, 1, 6), mano vuota. Poi `chargeACell(…, 0, 2, 1_000)`, `stash`, `chargeACell(…, 0, 4, 2_000)`,
  `stash` → mano vuota, 2 celle e 2 polveri nell'inventario. `chargeACell` fa: `holdCell(from)` e clic (cella dentro
  con `from` RE, mano vuota, un messaggio `STATUS_NO_REDSTONE` in piu'); `hold(REDSTONE, dust)` e clic (polveri dentro,
  mano vuota, `status() == CHARGING`); `tick(dust * 200)` (cella a `to` RE, nessuna polvere, riserva 0,
  `status() == NO_REDSTONE`); clic a mano vuota (cella con `to` RE in mano, caricatore senza cella).
- Passo 4 (`armAndEnter`): sei `mount` (`hold` + clic: elmo (0, 2.5, 0), torso (0, 1.6, 0), due volte braccio
  (-0.35, 1.6, 0), due volte gamba (-0.35, 0.5, 0); ogni clic `SUCCESS` e mano vuota) → `pieces().size() == 6`;
  `holdCell(1_000)` e clic sul torso → `cell().stored() == 1_000`, mano vuota; clic a mano vuota → `wornFrameId(p)` e'
  l'id del telaio, `messages(p, EntryHints.KEYS_HINT) == 1`.
- Passo 5 (`walkAndDrain`): premessa: il giocatore sta in (2.5, 1.0, 3.5); `t0 = now(helper)`, `SuitTick.tick(p, t0)`
  → 1 000 RE e nessun avviso (premessa); venti `setPos` da 0,2 lungo x con `SuitTick.tick(p, t0 + i)` → arrivo in
  (6.5, 1.0, 3.5), `stored(p) == 992`, `messages(p, SuitTick.ENERGY_LOW) == 1`.
- Passo 6 (`takeACactusHit`): premessa vita piena; `p.hurt(cactus, 8.0F)` vero; elmo 179, torso 178, braccia 179,
  gambe 179; `getHealth() < getMaxHealth()`.
- Passo 7 (`swapTheCell`): `CellSwap.handle(p, t0 + 30)` → `stored(p) == 2_000`; nell'inventario una cella sola, con
  992 RE, mani vuote; `SuitTick.tick(p, t0 + 31)` → 2 000 RE e `ENERGY_LOW` ancora 1.
- Passo 8 (`leaveAndRecharge`): `FrameEntry.exit(p)` vero, nessun telaio indossato; record piazzato in
  (6.5, 1.0, 3.5) con la cella a 2 000 RE e i sei pezzi; la sua entita' c'e' (`frameAt`) con lo stesso id; mano vuota;
  `chargeACell(…, 992, 2, 1_992)` → la cella da 1 992 RE in mano.
- Passo 9 (`countEverything`): `countInMainInventory(REDSTONE_CELL) == 1`, quella in mano, mano secondaria vuota,
  `charger.cell().isEmpty()`; 0 polveri nell'inventario e nel caricatore, riserva 0, riporto 0; in tutto l'inventario
  (armatura e mano secondaria comprese) un solo stack; 2 000 RE nel telaio piazzato + 1 992 in mano = 3 992 =
  8 × 500 − 8; `itemsIn(helper).isEmpty()`.
- Pulizia: `removeFrame`, caricatore svuotato e aria, inventario del giocatore svuotato; poi `helper.succeed()`.

Aiuti privati, con le firme del brief: `craftFromInventory(helper, p, Item expected, String... rows)`,
`hold(helper, p, Item item, int count)`, `holdCell(helper, p, int stored)`, `stash(p)`,
`tick(helper, charger, times)`. In piu' (vedi "Scelte"): `chargeACell`, `mount`, `clickAt`, `slotOutsideTheHand`,
`atStep`.

**`RedstoneExoframes.java`**: import e riga `Compat.gameTests().register(SurvivalGameTests.class);` dopo quella di
`DataGameTests`.

**`gametest/GameTestKit.java`**: solo il commento della classe: "{@code ChargerGameTests}, {@code DataGameTests} e
{@code SurvivalGameTests} la parte 3c-2" al posto di "{@code ChargerGameTests} e le altre della parte 3c-2".

## Che cosa ho provato e come e' andata

Il test e' passato alla prima esecuzione su 1.21.1-neoforge e poi sulle altre tre: nessun difetto del codice della mod
e' emerso, nessun file di un altro task e' stato toccato. I numeri del brief (1 000 e 2 000 RE dopo 400 e 800 tick,
992 dopo la camminata partendo da 1 000, un solo avviso di energia bassa, 179/178/179/179, 1 992 alla fine) sono
quelli che il gioco da' sulle quattro varianti.

Step 2, `bash tools/run_gametests.sh 1.21.1-neoforge` (ultime righe):

```
BUILD SUCCESSFUL in 26s
7 actionable tasks: 3 executed, 4 up-to-date
OK 1.21.1-neoforge: 208 test obbligatori passati, nessun avviso fuori elenco
```

Step 3, `bash tools/run_gametests.sh` (le righe `OK`, uscita 0):

```
OK 1.20.1-forge: 208 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 208 test obbligatori passati, nessun avviso fuori elenco
```

Conteggio statico delle righe che lo script conta (`cat gametest/*.java | grep -cE '^[[:space:]]*add\(tests,'`): 208
(207 + la riga di `SurvivalGameTests`).

Step 4, verifiche di fine task, sul codice del commit:

```
$ ./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build \
    :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar
BUILD SUCCESSFUL in 27s
52 actionable tasks: 28 executed, 24 up-to-date
$ bash tools/check_sources.sh
OK sources
$ bash tools/check_jars.sh
OK jars
$ bash tools/check_data.sh        (non richiesto: il task non tocca i dati)
OK data
```

Nessuna riga `warning:` o `error:` nel log della build. `check_art.sh` non l'ho eseguito: il task non tocca cio' che
controlla.

## Evidenza TDD

Il task aggiunge un test su codice che c'e' gia': non c'e' un'implementazione da far diventare verde. Il "rosso" e' la
prova negativa del brief.

- **Prova negativa (RED voluto), disfatta subito.** Nella preparazione `new ItemStack(Items.IRON_INGOT, 33)` al posto
  di 34, poi `bash tools/run_gametests.sh 1.21.1-neoforge`:

  ```
  [Server thread/ERROR] [minecraft/LogTestReporter]: redstone_exoframes:survival_from_crafting_to_recharging failed at
    -10643962, -60, -13899454! step 1 (crafting): missing ingredient: no minecraft:iron_ingot left in the inventory to
    craft redstone_exoframes:charger
  [Server thread/INFO] [minecraft/GameTestServer]: 1 required tests failed :(
  [Server thread/INFO] [minecraft/GameTestServer]:    - redstone_exoframes:survival_from_crafting_to_recharging
  FAIL 1.21.1-neoforge: Gradle e' uscito con 1
  ```

  Fallisce al passo 1 perche' manca un ingrediente, come atteso (il lingotto che manca e' uno dei cinque del
  caricatore, l'ultimo oggetto fabbricato). Il file e' stato rimesso da una copia in `<tmp>` e confrontato con `cmp`
  prima di proseguire; le esecuzioni di Step 3 e Step 4 sono successive al ripristino.
- **GREEN:** le righe `OK` qui sopra.

La prova negativa e' stata eseguita due volte: la prima ha mostrato che il nome del passo non compariva nel messaggio
(vedi "Che cosa ho trovato rivedendo il mio lavoro"), la seconda, dopo la correzione, da' l'uscita riportata qui.

## File cambiati

- `src/main/java/com/toxineren/exoframes/gametest/SurvivalGameTests.java` (nuovo)
- `src/main/java/com/toxineren/exoframes/RedstoneExoframes.java` (import e registrazione)
- `src/main/java/com/toxineren/exoframes/gametest/GameTestKit.java` (commento della classe)

## Scelte

- **Il nome del passo nel messaggio di fallimento.** Il brief vuole che "il messaggio dica a quale passo", ma da'
  agli aiuti (`hold`, `holdCell`, …) firme senza un argomento per il passo. Il corpo del test tiene una variabile
  locale `step` e un solo `try/catch` che rilancia ogni fallimento (anche una `RuntimeException` che non viene da
  un'asserzione, per esempio da `stored(p)`) come `GameTestAssertException` con "step N (…): " in testa. Cosi' anche i
  fallimenti degli aiuti di `GameTestKit` (`frameAt`, `recordState`, `useExoframeItem`) dicono il passo.
- **Il giocatore guarda a sud (yaw 0)**, verso il pavimento: il brief dice solo dove sta. Ne segue che il telaio,
  piazzato "girato di 180 gradi" rispetto a chi lo usa, guarda a nord, e che all'uscita il passo fuori porta il
  giocatore in (6.5, 1.0, 2.5), sopra il pavimento. Il test non asserisce dove finisce il giocatore dopo l'uscita.
- **`craftFromInventory` prende gli ingredienti dagli slot diversi da quello in mano** (lo stesso cercatore di `hold`
  e `holdCell`, `slotOutsideTheHand`): il brief dice "dall'inventario principale"; con la mano vuota, che e' la regola
  del test, e' la stessa cosa, e cosi' nessun aiuto tocca mai lo slot in mano.
- **Aiuti in piu' rispetto all'elenco del brief:** `chargeACell(helper, p, charger, from, dust, to)` (la sequenza
  cella, polvere, tick, ritiro e' la stessa tre volte: celle A e B al passo 3, la cella da 992 RE al passo 8);
  `mount` (un `hold` e un clic, sei volte); `clickAt` (il `click` di `GameTestKit` con un `Vec3`);
  `slotOutsideTheHand`; `atStep`.
- **Asserzioni in piu' rispetto al brief**, tutte dello stesso segno: l'inventario vuoto come premessa; nulla a terra
  dopo il passo 1 e i dieci oggetti contati; il telaio in (2.5, 1.0, 3.5); `status()` `CHARGING` dopo la polvere e
  `NO_REDSTONE` dopo i tick, e un messaggio `STATUS_NO_REDSTONE` in piu' a ogni cella inserita; il tick di
  preparazione che lascia 1 000 RE e nessun avviso; la camminata che arriva in (6.5, 1.0, 3.5); mani vuote dopo il
  cambio; al passo 8 i sei pezzi ancora nel record e l'entita' del telaio presente con lo stesso id; al passo 9 il
  riporto del caricatore a 0 e un solo stack in tutto l'inventario (la cella in mano).
- **Pulizia:** oltre a telaio e caricatore svuoto l'inventario del giocatore finto (`clearContent`), come fa
  `DataGameTests`. Il pavimento di pietra resta, come nei test che usano `wearFrameAt`.

## Scostamenti dal brief

Nessuno che cambi cio' che il task consegna. Due precisazioni:

- Il brief scrive i clic sul telaio come `click(frame, p, x, y, z)`; nel test passano da `clickAt(frame, p, punto)`,
  che chiama quello stesso `click` con le coordinate di una costante (`HELMET_POINT`, `TORSO_POINT`, `ARM_POINT`,
  `LEG_POINT`: i valori del brief).
- "Riserva 0" al passo 9 e' asserita insieme a `carryMilli() == 0`, che il brief non nomina.

## Che cosa ho trovato rivedendo il mio lavoro

- **Corretto prima del commit: il nome del passo spariva dal log.** La prima versione di `atStep` metteva l'eccezione
  d'origine come causa (`initCause`), come fa `GameTestKit.runInSequence`. Il gioco pero' scrive nel log il messaggio
  della causa piu' profonda (`Util.describeError`), quindi la prima prova negativa mostrava "missing ingredient: …"
  senza "step 1 (crafting): ". Ora `atStep` non mette la causa e copia la traccia dell'origine con `setStackTrace`; la
  seconda prova negativa mostra il passo. Lo dice il Javadoc di `atStep`.
- La mano principale e' asserita vuota in ogni punto in cui il brief lo chiede e all'ingresso di ogni `hold` e
  `holdCell`; `useExoframeItem` e' il solo punto in cui uno stack nasce in mano, ed e' preceduto dalla rimozione
  dell'oggetto telaio fabbricato.
- Nessuna asserzione vuota: ogni attesa e' un'uguaglianza su un numero del brief.

## Dubbi e punti aperti

Nessuno che fermi il task. Da sapere:

- Le forme delle sette ricette sono scritte una terza volta (dopo `tools/gen_data.py` e `DataGameTests.RECIPES`, che
  e' privata): il brief le vuole "con le righe di Ricette" nel test. E' la stessa natura del minore T8-F1.
- `tick` (il tick del caricatore chiamato a mano) e' la copia dell'aiuto privato di `ChargerGameTests`, come chiede il
  brief ("Aiuti privati: … `void tick(helper, charger, times)`").
- Se il test fallisce a meta', cio' che ha creato (telaio, caricatore, oggetti) resta nel mondo dei test, come per
  ogni altro test della suite.
- La prova negativa e le sue uscite non lasciano traccia nel repository: l'evidenza e' questo rapporto.
- Al termine non resta in esecuzione nessun server o client; resta solo il demone di Gradle, inattivo, che c'era gia'.
