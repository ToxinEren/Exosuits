### Task 3: La regola di carica (`ChargerRule`)

**Files:**
- Create: `core/energy/ChargerRule.java`
- Test: `src/test/java/com/toxineren/exoframes/core/energy/ChargerRuleTest.java` (nuovo)

**Interfaces:**
- Consumes: `EnergyRates.TICKS_PER_SECOND` (20); lo stile di `core/energy/CellSwapRule` (classe `final`, costruttore privato, argomenti controllati con `IllegalArgumentException`).
- Produces: `ChargerRule` con le firme di "Nomi, numeri e testi comuni". Regola di `tick` (un tick del server; è l'algoritmo di design §12.11, una polvere al più per tick):
  1. argomenti: `capacity > 0`, `stored >= 0` (può superare `capacity`: vale piena), `dust >= 0`, `reserve >= 0`, `0 <= carryMilli <= 999`, `rePerRedstone > 0`, `rePerSecond > 0`; altrimenti `IllegalArgumentException` che nomina l'argomento e il valore;
  2. `stored >= capacity` → `Result(stored, dust, reserve, 0, false)`: nessuna polvere bruciata, la riserva resta per la cella dopo;
  3. se `reserve == 0 && dust > 0`: `dust - 1`, `reserve = rePerRedstone`;
  4. `reserve == 0` → `Result(stored, dust, 0, 0, false)`;
  5. `long budget = carryMilli + rePerSecond * 1000L / TICKS_PER_SECOND`; `whole = budget / 1000`, `rest = budget % 1000`; `moved = min(whole, reserve, capacity - stored)`;
  6. `Result(stored + moved, dust, reserve - moved, moved == whole ? rest : 0, true)`: il riporto si tiene solo se tutto il ritmo del tick è passato; `charging` è vero anche in un tick con `whole == 0` (ritmi sotto 20 RE/s), così `lit` non lampeggia, e resta vero al tick in cui la riserva finisce e c'è altra polvere.
  Conseguenza dichiarata: con `rePerSecond / 20 > rePerRedstone` il ritmo vero è `rePerRedstone` RE per tick (una polvere per tick), sotto quello della config; con i predefiniti il limite è 500 RE per tick contro 2,5.
  `status`: senza cella `NO_CELL`; `stored >= capacity` `FULL`; `dust == 0 && reserve == 0` `NO_REDSTONE`; altrimenti `CHARGING`. `percent` = `100L * min(stored, capacity) / capacity` (per difetto). `comparator` = 0 senza cella, altrimenti `15L * min(stored, capacity) / capacity` (per difetto: 0 anche con una cella vuota, come dice il design).

- [ ] **Step 1: test JUnit** — `ChargerRuleTest` (`CAP = 10_000`, `DUST_RE = 500`, `RATE = 50`; `t(stored, dust, reserve, carry)` = `ChargerRule.tick(stored, CAP, dust, reserve, carry, DUST_RE, RATE)`):
  - `twoAndAHalfRePerTick`: `t(0, 20, 0, 0)` → `Result(2, 19, 498, 500, true)`; `t(2, 19, 498, 500)` → `Result(5, 19, 495, 0, true)`.
  - `anEmptyCellFillsInFourThousandTicksWithTwentyDust`: da `(0, 20, 0, 0)`, 3 999 tick → `stored == 9_997`, `charging`; un tick ancora → `Result(10_000, 0, 0, 0, true)`; un altro → `Result(10_000, 0, 0, 0, false)`.
  - `aFullCellBurnsNothing`: `t(10_000, 5, 0, 700)` → `Result(10_000, 5, 0, 0, false)`; `t(40_000, 5, 123, 0)` → `Result(40_000, 5, 123, 0, false)`.
  - `noRedstoneMeansNoCharge`: `t(100, 0, 0, 400)` → `Result(100, 0, 0, 0, false)`.
  - `theReserveIsSpentBeforeTheNextDust`: `t(0, 3, 2, 0)` → `Result(2, 3, 0, 500, true)`; da lì → `Result(5, 2, 497, 0, true)`; `t(0, 3, 1, 0)` → `Result(1, 3, 0, 0, true)` (riporto lasciato cadere).
  - `theLastReNeverOverfills`: `t(9_999, 0, 100, 0)` → `Result(10_000, 0, 99, 0, true)`.
  - `slowRateStillCharges`: con `rePerSecond = 1`, da `(0, 1, 0, 0)`: per 19 tick `stored == 0` e `charging`, al diciannovesimo `carryMilli == 950`; al ventesimo `stored == 1`, `carryMilli == 0`.
  - `limitsOfTheConfigNeverOverflow`: `tick(0, 1_000_000, 64, 0, 999, 1_000_000, 100_000)` → `Result(5_000, 63, 995_000, 999, true)`; `tick(0, 1_000_000, 64, 0, 0, 1, 100_000)` → `Result(1, 63, 0, 0, true)`; `tick(999_999, 1_000_000, 0, 1_000_000, 0, 1_000_000, 100_000)` → `Result(1_000_000, 0, 999_999, 0, true)`.
  - `energyIsConserved`: per `(rePerRedstone, rePerSecond)` in `(500, 50)`, `(1, 100_000)`, `(1_000_000, 1)`, `(7, 333)`, capacità 10 000, 64 polveri, 20 000 tick: a ogni tick `stored + reserve + (long) dust * rePerRedstone` è quello di partenza, `0 <= carryMilli <= 999`, `stored <= 10_000`, `reserve >= 0`, `dust >= 0`.
  - `invalidArgumentsAreRefused`: `assertThrows(IllegalArgumentException.class, …)` per `capacity` 0, `stored` −1, `dust` −1, `reserve` −1, `carryMilli` −1 e 1 000, `rePerRedstone` 0, `rePerSecond` 0.
  - `statusPrecedence`: `status(false, 0, CAP, 5, 100) == NO_CELL`; `(true, 10_000, CAP, 0, 0) == FULL`; `(true, 40_000, CAP, 5, 5) == FULL`; `(true, 100, CAP, 0, 0) == NO_REDSTONE`; `(true, 100, CAP, 1, 0) == CHARGING`; `(true, 100, CAP, 0, 1) == CHARGING`.
  - `percentRoundsDown`: `percent(0, CAP) == 0`, `(99) == 0`, `(100) == 1`, `(9_999) == 99`, `(10_000) == 100`, `(40_000) == 100`, `percent(999_999, 1_000_000) == 99`.
  - `comparatorFromZeroToFifteen`: `comparator(false, 5_000, CAP) == 0`; con cella: `0 → 0`, `666 → 0`, `667 → 1`, `5_000 → 7`, `9_999 → 14`, `10_000 → 15`, `40_000 → 15`.
- [ ] **Step 2: Run** `./gradlew --console=plain :1.21.1-neoforge:test --tests '*ChargerRuleTest'` — Expected: FAIL (non compila: `ChargerRule` manca).
- [ ] **Step 3: implementare `ChargerRule`** come in "Produces". Javadoc della classe: la regola in parole, i numeri dei predefiniti, la conseguenza dichiarata, il rimando a design §12.11.
- [ ] **Step 4: Run** lo stesso comando — Expected: PASS; poi `./gradlew --console=plain :1.20.1-forge:test --tests '*ChargerRuleTest'` — PASS (Java 17).
- [ ] **Step 5: verifiche di fine task** — build delle quattro varianti con `collectJar` → `BUILD SUCCESSFUL`; `bash tools/check_sources.sh` → `OK sources` (regole 1, 1b, 1c); `bash tools/check_jars.sh` → `OK jars`. I test in mondo non cambiano (181).
- [ ] **Step 6: commit** `core: regola di carica del caricatore`.

