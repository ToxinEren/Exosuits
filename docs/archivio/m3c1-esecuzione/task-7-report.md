# Task 7 - Il passo fuori rivisto; uscita rifiutata sopra un altro telaio

Stato: DONE. BASE e0593d784e262251d6fe88f2f28729528eb0960e, HEAD cd5b0574c56a1f1f627e6f64bc092166c5edb9c6.

Commit: `cd5b057 frame: passo fuori con gradino, percorsi per asse e piu' pericoli; uscita rifiutata sopra un altro telaio`

## Che cosa ho fatto

`frame/FrameEntry`:
- `public static final String FRAME_TAKEN = "message." + MOD_ID + ".frame_taken"`, `private static final double MAX_STEP_UP = 0.5`.
- `exit`: dopo le guardie calcola `restingSpot(player)` prima di togliere i segnaposto; se `getEntitiesOfClass(ExoframeEntity.class, SuitSize.standingBox(spot.position()))` non e' vuoto, `FrameInteractions.tell(player, FRAME_TAKEN)` e `return false`, nient'altro. Poi `Placeholders.removeAll`, `returnStowed`, `leaveFrame(player, frames, "left", spot)`, `stepOut`.
- `leaveFrame(ServerPlayer, FrameStateAccess, String event, RestingSpot spot)`: il punto lo passa chi chiama; `onDeath` passa `restingSpot(player)` e non guarda gli altri telai.
- `standingSpot`: riquadro vanilla con i piedi a `candidate + 0,5`, `depth = 0,5 + 1 + 1e-3`, `Shapes.collide` sulle forme dei blocchi (quelle gia' urtate non fanno da pavimento), poi rifiuti nell'ordine del brief: `noCollision(player, standing)`, `insideBorder(feet)`, `hazard` in `betweenClosedStream(standing.expandTowards(0, -0.5, 0))`, `ExoframeEntity` in `standing`, `AbstractMinecart` in `standing.expandTowards(0, -1, 0)`.
- `hazard(BlockState)` come il brief (lava, fuoco, falo', portali, magma, bacche dolci, rosa del Wither, neve polverosa, cactus).
- `wayIsFree(player, level, candidate, to)` con i due percorsi a L e `segment(...)` esattamente come il brief; `stepOut` gli passa `target.get()`.
- Javadoc riscritti per `exit`, `onDeath` (una frase: la morte non si rifiuta), `leaveFrame`, `stepOut`, `wayIsFree`, `standingSpot`, `hazard`, le due costanti nuove.

`gametest/GameTestKit`: `static void assertExitStepsLeft(GameTestHelper, ServerPlayer, UUID frameId, Vec3 spot, String hazard)` come da brief.

`gametest/CarryOverGameTests`: otto righe `add(tests, ...)` e i loro corpi: `exitStepsUpASmallLip`, `exitTakesAnAxisPathAroundAPost`, `exitAvoidsCampfireMagmaAndBerries`, `exitAvoidsWitherRosePowderSnowAndCactus`, `exitAvoidsAPortalBelow`, `exitAvoidsABoatBelowTheSpot`, `exitAvoidsAMinecartBelowTheSpot`, `exitRefusedOverAnotherFrame` (le attese dell'ultimo sono quelle del brief, parola per parola salvo il testo dell'asserzione sulla posizione, che il brief lasciava con "...").

Lingue: `message.redstone_exoframes.frame_taken` in coda ai due file, dopo `exit_hold`, con i testi del brief. `docs/compat.md` non tocca (nessuna cucitura nuova).

## TDD

RED - `bash tools/run_gametests.sh 1.21.1-neoforge` con i test e la sola costante `FRAME_TAKEN` (nessun'altra modifica a `FrameEntry`): `8 required tests failed :(`, ciascuno per il motivo previsto:
```
exit_steps_up_a_small_lip failed ... player did not step up a lip of 1/16: (-1.26290775E7, -58.0625, 4783971.5), expected (-1.26290775E7, -58.0, 4783972.5)
exit_avoids_a_portal_below failed ... player stepped onto an end portal: (-1.26291285E7, -59.0, 4783987.5)
exit_avoids_a_boat_below_the_spot failed ... player stepped onto a boat: (-1.26291155E7, -59.0, 4783987.5)
exit_avoids_campfire_magma_and_berries failed ... player stepped onto a campfire: (-1.26291545E7, -58.5625, 4783987.5)
exit_avoids_wither_rose_powder_snow_and_cactus failed ... player stepped onto a wither rose: (-1.26291415E7, -58.0, 4783987.5)
exit_takes_an_axis_path_around_a_post failed ... player did not step ahead around the post: (-1.262906420710678E7, -58.0, 4783973.207106781), expected (-1.262906279289322E7, -58.0, 4783973.207106781)
exit_avoids_a_minecart_below_the_spot failed ... player stepped onto a minecart: (-1.26291025E7, -59.0, 4783987.5)
exit_refused_over_another_frame failed ... exit over another frame accepted
```
(il palo: il giocatore e' andato a destra, come il brief prevedeva; il sentiero: e' rimasto sul posto.)

GREEN - stesso comando dopo l'implementazione:
```
All 110 required tests passed :)
OK 1.21.1-neoforge: 110 test obbligatori passati, nessun avviso fuori elenco
```
I test della 3b sul passo fuori e sull'uscita elencati allo Step 4 restano verdi (fanno parte dei 110).

## Verifiche di fine task (quattro varianti)

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` -> `BUILD SUCCESSFUL in 24s`, nessun warning o errore del compilatore nel log.
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `bash tools/run_gametests.sh`:
```
OK 1.20.1-forge: 110 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 110 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 110 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 110 test obbligatori passati, nessun avviso fuori elenco
```
110 = 102 del Task 6 + 8, come la tabella di Global Constraints.

## File cambiati

- src/main/java/com/toxineren/exoframes/frame/FrameEntry.java
- src/main/java/com/toxineren/exoframes/gametest/CarryOverGameTests.java
- src/main/java/com/toxineren/exoframes/gametest/GameTestKit.java
- src/main/resources/assets/redstone_exoframes/lang/en_us.json
- src/main/resources/assets/redstone_exoframes/lang/it_it.json

## Scelte

- `exitStepsUpASmallLip`: i due casi passano da un aiuto privato di `CarryOverGameTests`, `assertStepsAheadTo(helper, player, feet, step)` (yaw 0, `exit` vero, `distanceTo(atteso) < 1e-6`), come gia' fa `assertEyesInsidePick` nella stessa classe: non e' un aiuto comune, quindi non va in `GameTestKit`. Il sentiero si posa prima di `wearFrameAt`, perche' l'ingresso controlla lo spazio nel punto del telaio.
- `exitTakesAnAxisPathAroundAPost`: la premessa sui limiti del palo confronta con una tolleranza di 1e-9 (0,375 e 0,625 sono esatti in binario, ma la tolleranza non costa nulla); aggiunta anche `assertRoomToStand` sul posto davanti, come fanno i test della 3b, cosi' il test non passa per un posto senza pavimento.
- `exitAvoidsWitherRosePowderSnowAndCactus`: prima di `succeed()` tolgo, dopo il cactus, anche la sabbia in `(3, 0, 4)`: la sabbia posata ha un tick programmato e, con aria sotto la struttura, cadrebbe dopo la fine del test; tolta nello stesso tick, il suo tick trova un altro blocco e non fa nulla.
- `exitAvoidsAPortalBelow`: la premessa "collisione vuota" del portale dell'End e' un'asserzione nel test.
- `assertExitStepsLeft`: "rimette yaw 0" con `moveTo` sulla posizione corrente e yaw 0, come `assertStepAvoids`; i messaggi di rifiuto di `enter`/`exit` nominano il pericolo, per capire subito quale caso e' fallito.
- In `exit` il parametro della lambda di `stepOut` si chiama `placed` (il nome `spot` e' ora la variabile locale del punto di riposo).
- Javadoc di `onDeath`: una frase in piu' dice che la morte non guarda gli altri telai (design §12.14: morte e spostamenti forzati restano come oggi).

## Scostamenti dal brief

- Step 2: non ho fatto girare la versione senza la costante (il fallimento di compilazione e' certo: `FrameEntry.FRAME_TAKEN` e' nominata dai test). Il RED l'ho preso direttamente "con una costante provvisoria", che e' gia' quella definitiva (nome, valore e Javadoc del brief); il brief prevede entrambe le letture.

## Revisione di me stesso

- Ordine dei rifiuti di `standingSpot` e formula di `wayIsFree` uguali al brief; nessun `isCreative()`; nessuna condizione Stonecutter; nomi di blocchi e tag uguali sulle due versioni (le quattro varianti compilano e passano).
- Nulla si perde: il rifiuto per un altro telaio avviene prima di toccare segnaposto, armatura stivata o registro (lo prova `exitRefusedOverAnotherFrame`: record indossato, segnaposto, elmo stivato, posizione invariati; poi l'uscita restituisce l'elmo).
- Righe entro 120 caratteri nei tre file Java toccati; commenti in italiano con apostrofi ASCII; i testi in it_it hanno le lettere accentate come il resto del file.
- Nessun processo lasciato acceso (Gradle esce alla fine di ogni comando; il demone resta come nei task precedenti).

## Dubbi

Nessuno.
