# Task 3 — La regola di carica (`ChargerRule`): rapporto

Stato: DONE. BASE `b5ac1b51152cb32c0f3fcfad7069834e2af36125`, HEAD `12011825618a536db16f406d26f7383a4ebf2058`.
Commit: `1201182 core: regola di carica del caricatore` (firmato dall'ambiente, come i precedenti).

## Cosa ho implementato

`src/main/java/com/toxineren/exoframes/core/energy/ChargerRule.java` (nuovo; `final`, costruttore privato, Java 17
puro, nessun import oltre a `EnergyRates.TICKS_PER_SECOND` dello stesso package), con le firme di "Nomi, numeri e
testi comuni":

- `public enum Status { NO_CELL, FULL, NO_REDSTONE, CHARGING }`
- `public record Result(int stored, int dust, int reserve, int carryMilli, boolean charging) {}`
- `public static Result tick(int stored, int capacity, int dust, int reserve, int carryMilli, int rePerRedstone, int rePerSecond)`
- `public static Status status(boolean hasCell, int stored, int capacity, int dust, int reserve)`
- `public static int percent(int stored, int capacity)`
- `public static int comparator(boolean hasCell, int stored, int capacity)`

`tick` segue i sei punti del brief nell'ordine: controllo degli argomenti (`IllegalArgumentException` con nome e
valore: `capacity must be > 0: 0`, `stored must be >= 0: -1`, `carryMilli must be in [0, 999]: 1000`, ...); cella
piena o sopra la capacita' → `Result(stored, dust, reserve, 0, false)`; riserva vuota e polvere → una polvere
bruciata; riserva ancora vuota → `Result(stored, dust, 0, 0, false)`; `long budget = carryMilli + rePerSecond * 1000L
/ TICKS_PER_SECOND`, `whole`, `rest`, `moved = min(whole, reserve, capacity - stored)`; `Result(stored + moved, dust,
reserve - moved, moved == whole ? rest : 0, true)`. `status`, `percent` e `comparator` come nel brief (prodotto in
`long`, per difetto).

Javadoc della classe: la regola in parole, i numeri dei predefiniti (2,5 RE per tick, una polvere 200 tick, cella
vuota piena in 4 000 tick con 20 polveri), la conseguenza dichiarata (una polvere al piu' per tick: con
`rePerSecond / 20 > rePerRedstone` il ritmo vero e' `rePerRedstone` RE per tick; con i predefiniti 500 contro 2,5),
il rimando a design §12.11. Aggiunge una frase che il brief implica ma non scrive: il riporto e' ritmo, non energia,
per questo lasciarlo cadere non rompe la conservazione.

`src/test/java/com/toxineren/exoframes/core/energy/ChargerRuleTest.java` (nuovo, 15 test): i 13 del brief con i
valori esatti, piu' due miei (vedi "Scelte").

Nessun altro file toccato: `docs/compat.md` non cambia (nessuna cuciture nuova in `compat/`), nessun test in mondo
nuovo, nessun testo, nessuna riga di log.

## Verifiche e risultati

Ho rifatto a mano i conti di ogni valore atteso del brief prima di scrivere i test: tornano tutti, nessuna correzione.

### TDD

- RED: `./gradlew --console=plain :1.21.1-neoforge:test --tests '*ChargerRuleTest'` con il solo test scritto →
  `> Task :1.21.1-neoforge:compileTestJava FAILED`, `error: package com.toxineren.exoframes.core.energy.ChargerRule
  does not exist` (e `cannot find symbol` a seguire). Atteso: la classe mancava.
- GREEN: stesso comando dopo l'implementazione → `BUILD SUCCESSFUL in 3s`; poi
  `./gradlew --console=plain :1.20.1-forge:test --tests '*ChargerRuleTest'` (Java 17) → `BUILD SUCCESSFUL in 3s`.
  In `versions/<variante>/build/test-results/test/TEST-...ChargerRuleTest.xml`: `tests="15" skipped="0" failures="0"
  errors="0"`.

### Verifiche di fine task (una volta, sulle quattro varianti, prima del commit)

1. `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build
   :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`

   ```
   > Task :1.20.1-fabric:collectJar
   > Task :1.21.1-fabric:collectJar
   > Task :1.21.1-neoforge:collectJar

   BUILD SUCCESSFUL in 28s
   52 actionable tasks: 40 executed, 12 up-to-date
   ```

   Nessuna riga `warning:` o `error:` nel log. `ChargerRuleTest` sulle quattro varianti: `tests="15" skipped="0"
   failures="0" errors="0"` in ciascuna.
2. `bash tools/check_sources.sh` → `OK sources` (uscita 0; regole 1, 1b e 1c comprese: `ChargerRule` e' nominato in
   `ChargerRuleTest`).
3. `bash tools/check_jars.sh` → `OK jars` (uscita 0).
4. `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh` (uscita 0):

   ```
   OK 1.20.1-forge: 181 test obbligatori passati, nessun avviso fuori elenco
   OK 1.20.1-fabric: 181 test obbligatori passati, nessun avviso fuori elenco
   OK 1.21.1-fabric: 181 test obbligatori passati, nessun avviso fuori elenco
   OK 1.21.1-neoforge: 181 test obbligatori passati, nessun avviso fuori elenco
   ```

`check_art.sh` non eseguito: il task non tocca cio' che controlla. Alla fine nessun build, server o client in
esecuzione; albero di lavoro pulito.

## File cambiati

- `src/main/java/com/toxineren/exoframes/core/energy/ChargerRule.java` (nuovo)
- `src/test/java/com/toxineren/exoframes/core/energy/ChargerRuleTest.java` (nuovo)

## Scelte

1. **`status`, `percent` e `comparator` controllano i loro argomenti** (`capacity > 0`, `stored >= 0`; `status`
   anche `dust >= 0` e `reserve >= 0`) con la stessa `IllegalArgumentException` di `tick`. Il brief elenca i
   controlli solo per `tick`. Motivo: senza controllo una capacita' 0 darebbe una `ArithmeticException` senza nome
   dell'argomento, e una carica negativa una percentuale o un'uscita del comparatore negative, in silenzio; lo stile
   di `CellSwapRule` e di `EnergyCell` e' rifiutare. I controlli valgono anche con `hasCell == false`. Chi li chiama
   (la block entity dei Task 4-6) passa la capacita' della config (> 0 per `ConfigValidation`) e valori limitati in
   lettura (`cell_energy` almeno 0, `redstone` e `reserve` almeno 0), quindi nel mod non scattano; attenzione pero':
   `comparator` e `status` si chiamano anche fuori dal ticker, che e' il solo punto con il `catch` di
   `RuntimeException`. Coperti dal test in piu' `invalidArgumentsOfTheReadingsAreRefused`.
2. **Due test oltre i 13 del brief.** `invalidArgumentsOfTheReadingsAreRefused` (la scelta 1) e
   `chargingStaysTrueOnTheTickTheReserveEnds` (la frase del brief "resta vero al tick in cui la riserva finisce e
   c'e' altra polvere", che nessuno dei 13 fissava da sola: `t(0, 1, 2, 500)` → `Result(2, 1, 0, 0, true)` e il tick
   dopo → `Result(4, 0, 498, 500, true)`; senza altra polvere il tick dopo ha `charging` falso).
   In `invalidArgumentsAreRefused` una asserzione in piu' fissa il testo di un messaggio (`carryMilli must be in
   [0, 999]: 1000`), perche' il brief chiede che l'eccezione nomini argomento e valore.
3. **`Result` non controlla i suoi campi** (nessun costruttore compatto): il brief non lo chiede e i test costruiscono
   `Result` di partenza con `charging` qualunque.
4. **Ho eseguito anche `run_gametests.sh`**, che lo Step 5 del brief non elenca: le Global Constraints lo chiedono se
   il task tocca codice del mod, e una classe nuova entra nei quattro jar.

## Scostamenti dal brief

Nessuno: i passi sono stati eseguiti nell'ordine scritto, con i valori del brief alla lettera. Le sole aggiunte sono
quelle elencate in "Scelte".

## Autorevisione

- Completezza: i sei punti di `tick`, `status`, `percent`, `comparator` e i 13 test del brief ci sono tutti, con i
  valori esatti.
- Trabocchi: `rePerSecond * 1000L` e' `long`; `whole` resta `long` fino al `min`; `moved <= capacity - stored` e
  `moved <= reserve`, quindi il cast a `int` e `stored + moved` sono sicuri per ogni `int` valido in ingresso, non
  solo entro i limiti della config. `percent` e `comparator` moltiplicano in `long`.
- `reserve` puo' superare `rePerRedstone` (il brief chiede solo `reserve >= 0`): succede se la config abbassa
  `chargerRePerRedstone` con una polvere gia' accesa; la regola la spende tutta, senza perdite.
- Nessuna nota nuova per il registro: niente di cio' che la riga "Note: never run by anyone" elenca passa da questo
  task.

## Dubbi

Nessuno sul comportamento. Un punto da guardare in revisione, non un difetto: la scelta 1 (letture che rifiutano gli
argomenti invece di limitarli). Se chi coordina preferisce letture che non lanciano mai, il cambio e' di poche righe
in `scaled` e in `status`, piu' il test `invalidArgumentsOfTheReadingsAreRefused`.
