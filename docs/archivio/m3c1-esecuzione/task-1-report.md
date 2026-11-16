# Task 1 — Impalcatura dei test in mondo: rapporto

Stato: DONE. BASE 6af544cc7e42a6fcfe2ab5f9cebe382be1e2ca34, HEAD b5cb0114204e52e3d83cecb01048dae43cb2e20c.
Commit: `b5cb011 test: impalcatura della 3c (classi divise, giocatore in sopravvivenza, sonda)`.

## Che cosa ho fatto

- **Step 1, divisione.** Con uno script (spostamento meccanico, senza riscrivere a mano) i 50 metodi `private static`
  non test (49 nomi, `near` due volte) e le 6 costanti di `ExoframeGameTests` sono passati in
  `gametest/GameTestKit.java` (`final class`, package-private, costruttore privato, nessuna annotazione), togliendo
  solo `private`. Verifica: la regione degli aiuti di `HEAD:ExoframeGameTests.java` (righe 3479-4037) con
  `private static` -> `static` e' identica byte per byte alla regione corrispondente di `GameTestKit` (`diff` vuoto);
  costruttore, `generate()` e i 95 corpi di `ExoframeGameTests` sono identici all'originale (righe 158-3477, `diff`
  vuoto). In `ExoframeGameTests` restano 95 metodi `private static void x(GameTestHelper helper)`, `generate()` e il
  costruttore; `import static com.toxineren.exoframes.gametest.GameTestKit.*;` e gli import ancora usati (tolti quelli
  rimasti senza uso). Il commento della classe e' passato in testa a `GameTestKit` con il paragrafo "Per aggiungere un
  test" riscritto (una classe per area, costruttore, `@GameTestGenerator`, `import static`, riga `register`, riga
  `add(tests, ...)`, nomi unici, conteggio su tutti i file di `gametest/`), la voce sul contatore dei tick corretta
  come da brief e la voce nuova su `isCreative()`/`survivalPlayer`; `{@link #startWhenEntitiesLoaded}` e
  `{@link #add}` puntano ai metodi spostati. `ExoframeGameTests` ha un commento corto (test della 3b, regole in
  `GameTestKit`). Nessuna riga di `GameTestKit` comincia con `add(tests,`.
- **Step 2.** `tools/run_gametests.sh`: `tests_dir`, conteggio con `cat "$tests_dir"/*.java | grep -cE ...`, i due
  messaggi dicono `in $tests_dir/*.java`, commento di testa aggiornato.
- **Step 4.** `gametest/CarryOverGameTests.java` (pubblica, costruttore pubblico, `@GameTestGenerator`), prima riga
  `add(tests, "probeLevelTicksTheMockWithoutPlayerTickEvents", ...)`, corpo del test come nel brief, Javadoc su che cosa
  prova e su chi ci conta; registrata in `RedstoneExoframes.init()` subito dopo la riga di `ExoframeGameTests`.
- **Step 6.** `compat/GameTests.survivalPlayer(GameTestHelper)` con il Javadoc richiesto; in `GameTestsImpl`
  `SPAWN_PROTECTION_TICKS = 60` (commento sul campo `spawnInvulnerableTime` del costruttore di `ServerPlayer`) e
  `survivalPlayer` = `mockServerPlayer`, `setGameMode(GameType.SURVIVAL)`, 60 x `player.tick()`, senza direttive.
  In `GameTestKit` i nuovi `now`, `scrapState`, `wearScrap`, `stored`, `worn`, `carry`, `structureBox`, `fillPool`,
  ciascuno con un Javadoc di una riga.
- **Step 7.** Quattro varianti con la misura (sotto). Somma 115 s < 15 min: `.github/workflows/build.yml` non toccato.
- **Step 8.** `docs/compat.md` sezione `GameTests` (riga "File" e "A cosa serve" con `survivalPlayer` e il perche');
  `README.md:29`.

## TDD

- RED (Step 5): `./gradlew --console=plain :1.21.1-neoforge:compileJava` ->
  `CarryOverGameTests.java:41: error: cannot find symbol  symbol: method survivalPlayer(GameTestHelper)`,
  `BUILD FAILED` — atteso: il metodo non esisteva ancora.
- GREEN: `bash tools/run_gametests.sh 1.21.1-neoforge` ->
  `OK 1.21.1-neoforge: 96 test obbligatori passati, nessun avviso fuori elenco`.

## Prove (ultime righe)

- Step 3, dopo la sola divisione: `OK 1.21.1-neoforge: 95 test obbligatori passati, nessun avviso fuori elenco`.
- Step 7 (`for v in ...; do ...; bash tools/run_gametests.sh "$v"; echo "$v: N s"; done`, con `EXO_ACCEPT_EULA=true`):
  ```
  OK 1.20.1-forge: 96 test obbligatori passati, nessun avviso fuori elenco
  1.20.1-forge: 59 s
  OK 1.20.1-fabric: 96 test obbligatori passati, nessun avviso fuori elenco
  1.20.1-fabric: 19 s
  OK 1.21.1-fabric: 96 test obbligatori passati, nessun avviso fuori elenco
  1.21.1-fabric: 18 s
  OK 1.21.1-neoforge: 96 test obbligatori passati, nessun avviso fuori elenco
  1.21.1-neoforge: 19 s
  ```
  La sonda passa sulle quattro varianti. Dopo questa esecuzione sono cambiati solo `docs/compat.md` e `README.md`,
  nessun codice del mod: vale come esecuzione di fine task.
- Step 9: build e `collectJar` delle quattro varianti -> `BUILD SUCCESSFUL in 25s`; `bash tools/check_sources.sh` ->
  `OK sources`; `bash tools/check_jars.sh` -> `OK jars`.

## File

- Nuovi: `src/main/java/com/toxineren/exoframes/gametest/GameTestKit.java`,
  `src/main/java/com/toxineren/exoframes/gametest/CarryOverGameTests.java`.
- Modificati: `gametest/ExoframeGameTests.java`, `compat/GameTests.java`, `compat/impl/GameTestsImpl.java`,
  `RedstoneExoframes.java`, `tools/run_gametests.sh`, `docs/compat.md`, `README.md`.

## Scelte

- `structureBox` usa `STRUCTURE_SIZE - 1` invece del letterale 7, come fa `chunksWithoutEntities`: stesso valore.
- `fillPool` posa prima pavimento e bordo di vetro e poi il fluido, cosi' il fluido non trova mai un lato aperto nel
  momento in cui nasce; usa `helper.setBlock(BlockPos, Block/BlockState)`, uguale sulle due versioni.
- `stored`/`worn`/`carry` leggono il record con `Compat.frameState().get(player)` (lo stato vivo, solo letto). `worn` e
  `carry` lanciano `IllegalStateException("no worn frame")` senza record (il brief lo fissa per `worn`; per `carry` ho
  preso la stessa regola).
- Costruttore privato di `GameTestKit` con un Javadoc di una riga.
- Gli import di `ExoframeGameTests` e di `GameTestKit` sono quelli dell'originale che ciascun file usa ancora.
- Il commento di testa di `CarryOverGameTests` dice che la classe raccoglie i riporti di §11 della 3c e che in testa
  c'e' la sonda.

## Scostamenti dal brief

Nessuno.

## Autorevisione e note

- Diff degli aiuti e dei corpi verificato identico all'originale (vedi sopra); 58 metodi `static` in `GameTestKit`
  (50 spostati + 8 nuovi), 6 costanti.
- Le durate dello Step 7 sono state misurate con il demone Gradle caldo e la compilazione gia' fatta dalla corsa
  precedente di NeoForge (la prima variante, Forge 1.20.1, ha pagato la sua compilazione: 59 s); in CI, a freddo,
  saranno piu' lunghe, ma lontane dai 15 minuti.
- `tools/log_allowlist.txt:13` dice ancora "il test di gametest/ExoframeGameTests.java" in un commento: il file non e'
  nella lista del task e la frase resta vera per le righe di oggi; da aggiornare quando un'altra classe produce una
  riga dell'elenco (minore).
