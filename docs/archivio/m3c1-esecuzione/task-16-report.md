# Task 16 - Attributi idempotenti e gradino di un blocco: rapporto

Stato: DONE. BASE ff75a4c1316dd5e62d8259560054edbef1da9f9f, HEAD 56d07033a1ac44e58e2f79c6dfb63c79bdf3c31c.
Commit: `56d0703 compat: attributi applicati solo quando cambiano, gradino di un blocco sulle due versioni`.

## Che cosa ho fatto

- `compat/impl/FrameAttributesImpl.java`: `set` idempotente (codice comune: `getModifier(key(id))`; presente e `same` -> nulla; presente e diverso -> `removeModifier`; poi `addTransientModifier`). `same` nei due rami (1.21.1 `amount()`/`operation()` con `ADD_VALUE`/`ADD_MULTIPLIED_TOTAL`; 1.20.1 `getAmount()`/`getOperation()` con `ADDITION`/`MULTIPLY_TOTAL`). Costanti comuni `STEP = Ids.of("frame_step")`, `STEP_BONUS = 0.4`. `applyStep(e)` in fondo ad `apply`, `clearStep(e)` in fondo a `clear`: su 1.21.1 `set`/`remove` su `Attributes.STEP_HEIGHT`; su 1.20.1 vuoti con il commento. `localStep(Player, boolean)`: corpo intero in `//? if <1.21`, regola del brief, `SUIT_STEP = 1.0F`, `VANILLA_STEP = 0.6F`, `RAISED = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap())` solo nel ramo 1.20.1. Javadoc della classe riscritto (idempotenza, mai `addOrUpdateTransientModifier`/`equals`, quinto modificatore, `localStep` per versione).
- `compat/FrameAttributes.java`: `void localStep(Player player, boolean wearing)` con Javadoc; Javadoc della classe e di `apply` riscritti (idempotente; cinque modificatori su 1.21.1; li applica `frame/SuitTick` alla prima valutazione di ogni oggetto giocatore nuovo e quando gli effetti cambiano, mai `hurtArmor`; `clear` da `frame/WornState`). Sparita la frase sul listener di respawn.
- `frame/SuitSize.java`, `tick`: `if (player.isLocalPlayer()) Compat.attributes().localStep(player, now);` dopo il `refreshDimensions` condizionato, e una frase nel Javadoc.
- `compat/GameTests.java` + `compat/impl/GameTestsImpl.java`: `attributesToSync`, `stepIsAttribute`, `frameModifier`, `respawn`, come in "Produces", con Javadoc "solo per i test".
- `gametest/EnergyGameTests.java`: `attributesAppliedOnceAndNotResent`, `attributesComeBackOnANewPlayerObject`, `stepModifierFollowsTheVersion`, `leavingClearsTheAttributes` in coda al `generate()`; costanti `ATTRIBUTE_FEET`, `MOD_ATTRIBUTES`; aiuto privato `modAttributesToSync`.
- `docs/compat.md`: sezione `FrameAttributes` ("A cosa serve", "Differenze chiuse", "Condizione") e sezione `GameTests` ("A cosa serve", "Condizione") con i quattro aiuti.

## TDD

- RED (compilazione), `./gradlew --console=plain :1.21.1-neoforge:compileJava` dopo lo Step 1: `BUILD FAILED`, `cannot find symbol` per `attributesToSync(ServerPlayer)` (3), `frameModifier(ServerPlayer,String)` (10), `localStep(ServerPlayer,boolean)` (2), `respawn(ServerPlayer,boolean)` (1), `stepIsAttribute()` (1). Atteso: le cuciture non esistevano.
- RED (in mondo), `bash tools/run_gametests.sh 1.21.1-neoforge` dopo lo Step 3 (con `localStep` vuoto, vedi Scostamenti): `2 required tests failed`, `FAIL 1.21.1-neoforge: Gradle e' uscito con 1`:
  - `attributes_applied_once_and_not_resent failed ... same effects re-sent [minecraft:generic.armor_toughness, minecraft:generic.movement_speed, minecraft:generic.armor]` (il `set` di oggi sporca gia' alla stessa applicazione, prima ancora di `LOW`: la ragione giusta);
  - `step_modifier_follows_the_version failed ... frame_step OptionalDouble.empty, expected 0.4`.
  Gli altri due passavano gia' (li tengono `SuitTick` e `WornState`), come dice il brief.
- GREEN dopo lo Step 5: `OK 1.21.1-neoforge: 139 test obbligatori passati, nessun avviso fuori elenco`; `OK 1.20.1-fabric: 139 test obbligatori passati, nessun avviso fuori elenco`.

## Step 7 - client vero, gradino di 1.20.1

Comando del brief, una variante alla volta (`/tmp/m3c-gradino-<variante>`). Tutte e due: uscita 0, `log-check.txt` vuoto ("nessun avviso o errore del mod fuori dall'elenco").

- 1.20.1-fabric, `rcon.log`: dopo `TUTA` `[-4.5d, -60.0d, 0.5d]` poi `[-4.5d, -59.0d, 5.883925151725098d]` (salito sullo scalino); dopo `SENZA-TUTA` `[-4.5d, -60.0d, 0.5d]` poi `[-4.5d, -60.0d, 2.699999988079071d]` (fermo contro lo scalino a Z 3, meta' larghezza 0,3).
- 1.20.1-forge, `rcon.log`: identico (`-59.0d` / Z 5.88 con la tuta; `-60.0d` / Z 2.70 senza).

## Verifiche di fine task (quattro varianti)

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` -> `BUILD SUCCESSFUL in 31s`
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `bash tools/run_gametests.sh`:
  ```
  OK 1.20.1-forge: 139 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 139 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 139 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 139 test obbligatori passati, nessun avviso fuori elenco
  ```
  139 = 135 (Task 15) + 4, come la tabella del piano.

Nessun server, client o Gradle lasciato acceso (a parte il demone di Gradle).

## File cambiati

- `src/main/java/com/toxineren/exoframes/compat/FrameAttributes.java`
- `src/main/java/com/toxineren/exoframes/compat/impl/FrameAttributesImpl.java`
- `src/main/java/com/toxineren/exoframes/compat/GameTests.java`
- `src/main/java/com/toxineren/exoframes/compat/impl/GameTestsImpl.java`
- `src/main/java/com/toxineren/exoframes/frame/SuitSize.java`
- `src/main/java/com/toxineren/exoframes/gametest/EnergyGameTests.java`
- `docs/compat.md`

## Scelte

- `SuitSize.tick` passa a `localStep` la variabile `now` gia' calcolata invece di richiamare `wearsSuit(player)`: stesso valore nello stesso tick, una lettura dello slot in meno.
- `GameTestsImpl.frameModifier` su 1.21.1 scorre `BuiltInRegistries.ATTRIBUTE.holders().toList()` con il tipo `Holder<Attribute>`: per questo l'import di `net.minecraft.world.entity.ai.attributes.Attribute` e' comune e non solo nel blocco `<1.21` (il codice del repository non usa `var`). `StandardCharsets` e' nel blocco `<1.21` come da brief.
- `stepModifierFollowsTheVersion` e `leavingClearsTheAttributes` fanno anche loro il tick di preparazione (`SuitTick.tick(p, t0)`) come detto per "tutti i test"; in `leavingClearsTheAttributes` la pietra (2..4, 1, 2..4) e' posata prima di `wearScrap` (che posa comunque la sua in (3, 1, 3)).
- Messaggi delle asserzioni scritti per esteso (il brief li dava con "..."), con il valore trovato e quello atteso.
- I piedi comuni dei quattro test sono una costante privata `ATTRIBUTE_FEET = new Vec3(3.5, 2.0, 3.5)`, accanto a `FEET` della classe (che vale (2.5, 1.0, 2.5) e non e' quella del brief).
- Su 1.20.1 i campi `SUIT_STEP`, `VANILLA_STEP`, `RAISED` stanno in fondo al ramo `else` esistente, accanto a `key`/`modifier`/`same`, con commenti `//` come il resto di quel ramo (dentro il blocco commentato di Stonecutter un Javadoc `/** */` chiuderebbe il commento a blocco).

## Scostamenti dal brief

- Step 3/4: perche' lo Step 4 (RED dei test in mondo) compilasse, la firma `localStep` su `FrameAttributes` e un `localStep` vuoto in `FrameAttributesImpl` sono stati aggiunti gia' allo Step 3 (i test la chiamano e il brief la mette allo Step 5). Lo Step 5 ha poi scritto corpo e Javadoc. Il risultato finale e' quello del brief; cambia solo l'ordine.

## Self-review

- Idempotenza: confronto `==` su `double` e sull'operazione, mai `equals` ne' `addOrUpdateTransientModifier`; `clear` invariato salvo `clearStep`.
- `key(STEP)` su 1.20.1 non si chiama mai (`applyStep`/`clearStep` vuoti; `frameModifier` deriva l'UUID da se').
- `localStep` non fa nulla su un `ServerPlayer` (provato da `stepModifierFollowsTheVersion` sulle quattro varianti) e sul ramo 1.21.1 e' vuoto.
- Direttive solo in `compat/impl/` (`OK sources`).

## Problemi aperti

Nessuno. Il gradino di 1.21.1 e di 1.20.1-forge/fabric con il client vero sulle quattro varianti resta al Task 24, come dice il brief.
