# Task 6 — `/exoframe remove` e la linea di vista in `frame/`: rapporto

Stato: DONE. BASE 88e36143c8202857fa59d74089b08fb5e735dec2, HEAD e0593d784e262251d6fe88f2f28729528eb0960e.

## Che cosa ho fatto

Commit 1 `5fc2824 frame: rimozione e linea di vista dei comandi in frame/` (spostamento puro, nessun test nuovo):
- `frame/FrameSight.java` (nuovo): `public static final double SIGHT = 5.0`, `public static Optional<ExoframeEntity>
  inSight(ServerPlayer)` = il corpo di `frameInSight` di prima, identico (`COLLIDER`, `Fluid.NONE`, stessa
  `getEntityHitResult`).
- `frame/FrameRemoval.java` (nuovo): `public record Result(boolean removed, List<PieceSlot> deleted)` con costruttore
  compatto `deleted = List.copyOf(deleted)`; `public static Result remove(ServerPlayer by, ExoframeEntity frame)` con
  gli stessi passi nello stesso ordine del vecchio `removeFrame` (pezzi: `pieceStack`, su `IllegalArgumentException`
  lo slot va in `deleted`; `updatePlaced(unmount)` poi `dropStill`; cella: `fromCell`, `updatePlaced(removeCell)`,
  `dropStill`; `takeStowedOfPlaced` -> `dropStill`; `removePlaced`; `discard`; `FrameLog.event(..., "removed", ...)`).
  Esiti: record assente -> `Result(false, List.of())`; rifiuto a meta' -> `Result(false, deleted)`; fine ->
  `Result(true, deleted)`.
- `command/ExoframeCommand.java`: via `frameInSight`, `SIGHT` e il corpo di `removeFrame`; il comando segue il codice
  del brief (livelli letti in un `EnumMap` prima di `remove`, poi `remove.done` / WARN invariata + `remove.done_deleted`
  con i nomi `livello slot` uniti da `", "`); `setUnlocked` chiama `FrameSight.inSight`; Javadoc della classe con
  `{@link FrameSight#SIGHT}`, `{@link FrameSight#inSight}` e `{@link FrameRemoval#remove}`; import inutili tolti.

Commit 2 `e0593d7 frame: i comandi mirano come il clic`:
- `FrameSight.inSight`: (1) telai il cui riquadro contiene gli occhi (`getEntitiesOfClass` su
  `new AABB(eyes, eyes).inflate(1.0e-7)`, filtro `!isRemoved() && getBoundingBox().contains(eyes)`): vince il centro
  piu' vicino agli occhi, senza guardare i blocchi; (2) altrimenti la ricerca di prima con `ClipContext.Block.OUTLINE`.
  Javadoc con le due regole e il perche'.
- `gametest/CarryOverGameTests.java`: due righe `add(tests, ...)` e i test `tallGrassHidesTheFrameFromTheCommand`,
  `eyesInsideAFrameBoxPickThatFrame` (con l'aiuto privato `assertEyesInsidePick` per le due coppie).

## TDD

RED (Step 4), `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh 1.21.1-neoforge`, con la mira di prima (`COLLIDER`):
```
redstone_exoframes:tall_grass_hides_the_frame_from_the_command failed at 3354438, -60, 4530595! unlocked a frame behind tall grass
redstone_exoframes:eyes_inside_a_frame_box_pick_that_frame failed at 3354451, -60, 4530595! the sight did not pick the frame around the eyes
2 required tests failed :(
FAIL 1.21.1-neoforge: Gradle e' uscito con 1
```
Atteso: il raggio con le forme di collisione attraversa l'erba (le premesse sulle forme dell'erba sono passate, la
prima asserzione che fallisce e' quella di `unlock`). Il test degli occhi e' fallito anch'esso su questa variante
(il brief lo dava come possibile, non garantito), con le premesse (occhi dentro il riquadro di A) passate.

GREEN (Step 6), stesso comando dopo Step 5:
```
All 102 required tests passed :)
OK 1.21.1-neoforge: 102 test obbligatori passati, nessun avviso fuori elenco
```

## Verifiche

Step 2 (dopo lo spostamento, prima del commit 1): build delle quattro varianti con `collectJar` -> `BUILD SUCCESSFUL`;
`OK sources`; `OK jars`; `bash tools/run_gametests.sh`:
```
OK 1.20.1-forge: 100 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 100 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 100 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 100 test obbligatori passati, nessun avviso fuori elenco
```
(fra i 100 obbligatori i sei del brief: `wallHidesTheFrameFromTheCommand`, `removeDropsTheContents`,
`ownerUnlocksAndStrangerCanUse`, `strangerCannotUnlock`, `debugNeedsOperator`, `lockAndUnlockTheWornFrame`).

Step 7 (fine task, prima del commit 2), comandi esatti del piano:
```
./gradlew --console=plain :1.20.1-forge:build ... :1.21.1-neoforge:collectJar   -> BUILD SUCCESSFUL in 21s
bash tools/check_sources.sh -> OK sources
bash tools/check_jars.sh    -> OK jars
bash tools/run_gametests.sh (EXO_ACCEPT_EULA=true):
OK 1.20.1-forge: 102 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 102 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 102 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 102 test obbligatori passati, nessun avviso fuori elenco
```
Nessun server o client lasciato acceso.

## File cambiati

- `src/main/java/com/toxineren/exoframes/frame/FrameSight.java` (nuovo)
- `src/main/java/com/toxineren/exoframes/frame/FrameRemoval.java` (nuovo)
- `src/main/java/com/toxineren/exoframes/command/ExoframeCommand.java`
- `src/main/java/com/toxineren/exoframes/gametest/CarryOverGameTests.java`

Nessuna chiave di testo, nessuna riga in `docs/compat.md` (come da brief).

## Scelte

- `FrameRemoval.remove` legge il record con `frames.placed(by.getServer(), frame.frameId())` e il livello da
  `frame.level()`, come faceva il comando; il `frameId` per i passi viene dal record (uguale a quello dell'entita').
- Il comando tiene `UUID frameId = record.get().frameId()` per la riga WARN, che ha lo stesso testo e gli stessi
  argomenti di prima.
- Nel test dell'erba la premessa controlla anche che il blocco sia davvero `TALL_GRASS` (oltre a collisione vuota e
  contorno non vuoto), per non far passare il test con un'erba che `setBlock` non ha messo.
- Nel test degli occhi anche i telai B sono di `ownedBy(owner)` (il brief elide lo stato): cosi' un `unlock` che
  prendesse B darebbe 1 e il fallimento lo coglie `!recordState(helper, b).unlocked()`, con il messaggio del brief.
- Le due coppie condividono un aiuto privato `assertEyesInsidePick` in `CarryOverGameTests` (non in `GameTestKit`:
  serve solo a questo test).
- Nella seconda regola la variabile del riquadro di ricerca si chiama `searched` (prima `around`), perche' `around`
  e' ora la lista dei telai attorno agli occhi.

## Scostamenti dal brief

Nessuno.

## Autorevisione

- Lo spostamento conserva ordine dei passi ed esiti; l'unica differenza visibile e' nessuna: il messaggio dei pezzi
  eliminati si compone dagli slot di `deleted` con i livelli letti prima, nello stesso ordine degli slot.
- `new EnumMap<>(PieceSlot.class)` riempita con `forEach`, mai `new EnumMap<>(Map)`.
- Javadoc e commenti in italiano con apostrofi ASCII, righe entro 120 caratteri nelle parti toccate.
- Nessun nuovo `isCreative()`; nessuna condizione Stonecutter; `new AABB(Vec3, Vec3)` esiste sulle due versioni
  (build verde sulle quattro varianti).

## Dubbi

Nessuno.
