# Task 17 - Usura dei pezzi: report

Stato: DONE_WITH_CONCERNS (una correzione piccola a un test, vedi "Scostamenti dal brief").
BASE 56d07033a1ac44e58e2f79c6dfb63c79bdf3c31c, HEAD 133406d3a3ba7e2054de0962624107f072320e04.
Commit: `133406d frame: usura dei pezzi dove il gioco consuma l'armatura`.

## Che cosa ho fatto

- `core/armor/DamageRouter`: nuovo `public Split routeMilli(DamageKind, long amountMilli, Map, Map carry)` con
  controllo `[0, MAX_MILLI]` (`MAX_MILLI = (long) Integer.MAX_VALUE * MILLI`, messaggio
  `amountMilli must be in [0, <MAX_MILLI>]: <valore>`); quote calcolate direttamente sull'importo in millesimi,
  resti maggiori con totale `amountMilli`, poi riporto per slot come prima; senza pezzi che assorbono
  `Split(vuota, (int) (amountMilli / 1000))` e riporto intatto. `route(kind, int, mounted, carry)` ora e'
  `requireNonNegative(amount); return routeMilli(kind, (long) amount * MILLI, mounted, carry);`. `quotas` prende un
  `long`. `route` senza riporto invariato. Javadoc della classe, di `routeMilli` e della variante senza riporto
  aggiornati (in gioco si usa `routeMilli`).
- `compat/impl/DamageClassifierImpl.classify`: dopo la caduta e prima di ogni prova sulla posizione,
  `source.is(DamageTypeTags.DAMAGES_HELMET)` -> `HEAD`; Javadoc dell'ordine (caduta, `damages_helmet`, `FROM_ABOVE`,
  `HEAD` per posizione, `GENERIC`).
- `RedstoneExoframes`: campo `private static volatile DamageRouter damageRouter` accanto a `rules`, ricostruito in
  `setConfig`; `public static DamageRouter damageRouter()` con Javadoc (istanza unica, `SuitDamage` non ne costruisce);
  `SuitDamage.register()` subito dopo `SuitRestrictions.register()` con commento di una riga;
  `Compat.gameTests().register(DamageGameTests.class)` subito dopo `EnergyGameTests`.
- `frame/SuitDamage`: costanti `PIECE_CRITICAL`, `PIECE_BROKEN`; `register()` (pulizia di `FAILURES_LOGGED` a
  `PLAYER_QUIT`); `onArmorHurt` con le guardie nell'ordine del brief (`ServerPlayer`, `amount > 0F`,
  `!instabuild`, `!isSpectator()`, poi nel `try` `wornFrameId(...).isPresent()`), `catch (RuntimeException)` con
  `Exoframe wear failed for {}` (`getScoreboardName()`) una volta per UUID e sessione; `wear(...)` trascritto dal brief
  (un solo `mutate`, messaggi nella barra delle azioni, suono `PIECE_BROKEN`, `SuitTick.markDirty`); `pieceName(slot)`.
  Javadoc della classe: dove e perche' li', le guardie, che cosa non fa.
- `gametest/DamageGameTests`: nove test (le righe `add(tests,` del brief), con tre aiuti privati (`suited`,
  `assertUnworn`, `assertWear`/`assertPiece`).
- Testi: le due chiavi `message.*.piece_critical/piece_broken` fra `energy_empty` e `leave_frame_first`; in coda dopo
  `frame_taken` le sei `piece.*` e le due `condition.*`; stesso insieme e stesso ordine nei due file (52 chiavi,
  controllato con uno script che confronta l'elenco ordinato delle chiavi).
- `docs/compat.md`: `DamageClassifier`, "A cosa serve" con l'ordine nuovo e la nota sulla riduzione a 0,75 di 1.21.1;
  sezione Mixin, punto **Usura, acqua e scatto**, frase sostituita come da brief.

## TDD

- RED (JUnit): `./gradlew --console=plain :1.21.1-neoforge:test --tests '*DamageRouterTest'` ->
  `error: cannot find symbol ... method routeMilli(DamageKind,long,...)`, 8 errori, `BUILD FAILED`: atteso,
  `routeMilli` non esisteva.
- GREEN (JUnit): stesso comando -> `BUILD SUCCESSFUL`; `TEST-...DamageRouterTest.xml`: `tests="18" failures="0"
  errors="0"` (i 14 di prima piu' i 4 nuovi).
- RED (GameTest): `bash tools/run_gametests.sh 1.21.1-neoforge` con `onArmorHurt` vuoto -> `9 required tests failed`,
  tutti e nove per la ragione attesa:
  - `cactus_hit_wears_the_pieces_by_share`: `after one hit: HELMET 180 / 0, expected 179 / 200`
  - `second_hit_inside_the_window_wears_only_the_difference`: `after the smaller hit: HELMET 180 / 0, expected 179 / 200`
  - `zero_amount_wears_nothing`: `TORSO carry 0 after one thousandth, expected 1`
  - `armor_bypassing_damage_wears_nothing`: `helmet carry 0 after the cactus hit, expected 200`
  - `no_wear_in_creative_or_spectator`: `survivor helmet 180 / 0, expected 179 / 200`
  - `lava_wears_once_per_applied_hit`: `after lava: HELMET 180 / 0, expected 180 / 600`
  - `anvil_hits_the_helmet`: `anvil classified as GENERIC, expected HEAD`
  - `a_breaking_piece_lowers_the_armor_and_tells`: `sounds []`
  - `wear_at_empty_still_reaches_the_owner`: `0 syncs after the tick, expected 1`
- GREEN (GameTest): `OK 1.21.1-neoforge: 148 test obbligatori passati, nessun avviso fuori elenco`; su 1.20.1-forge il
  primo giro ha dato `no_wear_in_creative_or_spectator failed! premise: game modes` (vedi Scostamenti), dopo la
  correzione `OK 1.20.1-forge: 148 test obbligatori passati, nessun avviso fuori elenco`.

## Verifiche di fine task (ultime righe)

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`
  -> `BUILD SUCCESSFUL in 25s`; `DamageRouterTest` 18/18 su tutte e quattro le varianti (file dei risultati JUnit).
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh` ->
  - `OK 1.20.1-forge: 148 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.20.1-fabric: 148 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-fabric: 148 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-neoforge: 148 test obbligatori passati, nessun avviso fuori elenco`
  - `grep -c "Exoframe wear failed"` nei quattro `server.log`: 0.

148 = 139 (Task 16) + 9, come la tabella delle Global Constraints.

## File cambiati

- `src/main/java/com/toxineren/exoframes/core/armor/DamageRouter.java`
- `src/main/java/com/toxineren/exoframes/compat/impl/DamageClassifierImpl.java`
- `src/main/java/com/toxineren/exoframes/frame/SuitDamage.java`
- `src/main/java/com/toxineren/exoframes/RedstoneExoframes.java`
- `src/main/java/com/toxineren/exoframes/gametest/DamageGameTests.java` (nuovo)
- `src/test/java/com/toxineren/exoframes/core/armor/DamageRouterTest.java`
- `src/main/resources/assets/redstone_exoframes/lang/en_us.json`, `it_it.json`
- `docs/compat.md`

## Scelte

- `DamageGameTests`: aiuto privato `assertWear(helper, p, when, elmo, rElmo, torso, rTorso, braccio, rBraccio, gamba,
  rGamba)`, con braccia e gambe uguali a coppie come in tutte le attese del brief, e `assertUnworn` (durate 180,
  riporti 0); `suited(helper)` = `survivalPlayer` + `wearScrap(FEET, 5_000)`. Riduce nove blocchi di sei asserzioni.
- `largeAmountsDoNotOverflow`: uso l'aiuto `sum` gia' esistente (int) moltiplicato per `1000L`, come scrive il brief;
  la somma dell'usura non supera `Integer.MAX_VALUE`, quindi non trabocca.
- Il messaggio della premessa di `noWearInCreativeOrSpectator` riporta le tre modalita' di gioco, per dire subito quale
  non torna.
- Un solo commit, con l'oggetto dello Step 12.
- Javadoc di `DamageGameTests` senza riferimenti al numero del task: "i test dell'usura e della caduta della parte 3c"
  (la caduta arriva con il Task 18).

## Scostamenti dal brief

- `noWearInCreativeOrSpectator`: dopo `Compat.gameTests().mockServerPlayer(helper)` aggiungo
  `creative.setGameMode(GameType.CREATIVE)`. Su 1.20.1-forge la premessa falliva (`premise: game modes`): il giocatore
  finto di quel server non ha `instabuild` vero appena creato. E' la stessa riga che usa gia'
  `EnergyGameTests.creativeDrainsNothingBesideSurvival`. L'intento del test (creativa vera, premessa che lo prova) non
  cambia; la premessa resta e ora passa sulle quattro varianti.

## Self-review

- Guardie nell'ordine del brief; mai `isCreative()`; nessun attributo toccato; il colpo non si annulla; eccezioni
  catturate con l'ERROR una volta per UUID (ruling del ledger: UUID o oggetto, entrambi ammessi).
- `wear` e' la trascrizione del brief; il `return` su `!changed` copre record assente e registro in sola lettura.
- `DamageRouter` resta Java puro (`OK sources`).
- Righe oltre 120 caratteri: nessuna nuova (quelle segnalate in `DamageRouter.java` 20/75, `RedstoneExoframes.java`
  80/86 e `DamageRouterTest.java` 157 erano gia' cosi').

## Dubbi

- La regola del Global Constraints "su 1.20.1 e 1.21.1 il giocatore finto nasce in creativa" non vale sul server
  GameTest di 1.20.1-forge per `instabuild`: i task seguenti che contano su un `mockServerPlayer` gia' in creativa
  devono chiamare `setGameMode(GameType.CREATIVE)`. Non ho cambiato `GameTests` (fuori dal task).
