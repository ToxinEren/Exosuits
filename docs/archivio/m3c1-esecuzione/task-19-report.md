# Task 19 - Acqua e scatto: report

Status: DONE
BASE 930439ed75f1c12858c71bd055c68e3f95ea7fe4, HEAD 0f469e22b213feb00530d1067b8456851e382b86
Commit: `0f469e2 frame: in acqua la tuta affonda e cammina; niente scatto sotto il 10 per cento`

## Cosa ho fatto

- `frame/SuitMovement.java`: le regole di design §12.6. `WATER_HINT`; `affectedByFluids` falso con la tuta, `isInWater()` e
  non `isInLava()`; `pushedByFluid` falso con la tuta (acqua e lava); `afterUpdateSwimming` mette `setSwimming(false)`
  alla tuta che nuota; `sprintAllowed` falso con la tuta e indicatori presenti con `canSprint` falso (senza indicatori
  permesso). "Con la tuta" = `SuitSize.wearsSuit(player) && !player.isSpectator()` in un aiuto privato `suited`, con
  `wearsSuit` per primo. Javadoc della classe con i punti chiesti dal brief (regola per chi gioca, i due lati senza
  pacchetto, gradino e acqua indipendenti da energia e pezzi, `canSwim` non letto, scatto senza indicatori permesso,
  divieto vero sul client e passo 7 del server, spettatori con le regole del gioco).
- `frame/SuitTick.java`: campo `boolean waterHintShown` nel record; passo 9 in coda, dopo il passo 8, dentro lo stesso
  `try`, con la riga esatta del brief; Javadoc di `tick` passi 1-9 e un paragrafo della classe sul suggerimento.
- `gametest/MovementGameTests.java` (nuovo): quattro test, `suitIgnoresWaterForMovement`,
  `suitDoesNotSwimSprintingUnderwater`, `lavaKeepsVanillaMovement`, `waterHintOncePerEntry`, con vasca, controllo e
  pulizia come da brief; registrato in `RedstoneExoframes.init()` subito dopo `DamageGameTests`.
- Testi: `message.redstone_exoframes.water_hint` fra `leave_frame_first` e `frame_taken`, nei due file.
- `docs/compat.md`, sezione Mixin, punto **Usura, acqua e scatto**: la frase "per ora lasciano il valore del gioco"
  sostituita con cio' che fanno ora `exoframes$fluidMovement`, `exoframes$fluidPush`, `exoframes$noSwimming` e
  `exoframes$sprintGate`.

## TDD

RED (`bash tools/run_gametests.sh 1.21.1-neoforge`, con la sola costante `WATER_HINT` aggiunta per compilare e la
classe registrata), i quattro falliscono per le ragioni attese dal brief:
```
redstone_exoframes:suit_ignores_water_for_movement failed ... ! fluids still affect the suit in water
redstone_exoframes:suit_does_not_swim_sprinting_underwater failed ... ! the suit swims sprinting under water
redstone_exoframes:lava_keeps_vanilla_movement failed ... ! lava currents still push the suit
redstone_exoframes:water_hint_once_per_entry failed ... ! water hint shown 0 times in the first entry, expected 1
```
(Un primo RED con un solo `doTick` falliva invece sulla condizione di partenza, `inWater true, underWater false`: vedi
Scostamenti.)

GREEN (Step 4):
```
OK 1.21.1-neoforge: 156 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-forge: 156 test obbligatori passati, nessun avviso fuori elenco
```

## Client vero (Step 5)

Comando del brief, uscita in `/tmp/m3c-acqua-<variante>`. Tutte e due: uscita 0, `log-check.txt` vuoto (0 byte),
`grep -c 'moved wrongly' server.log` = 0.

| Misura | attesa | 1.20.1-forge | 1.21.1-fabric |
|---|---|---|---|
| TUTA: Y 1,5 s dopo partenza a -58 | -60.0d | -60.0d | -60.0d |
| TUTA: Y dopo Spazio 3 s | -60.0d | -60.0d | -60.0d |
| TUTA: Z dopo W 2 s (da 0.5) | +>= 6 | 7.7187 (+7.22) | 7.7187 (+7.22) |
| TUTA: X dopo 3 s nella corrente | 42.5d | 42.5d | 42.5d |
| TUTA: sponda alta 1 dopo W 1,5 s | Y -60.0d, X 26.8-29.2 | Y -60.0d, X 27.884 | Y -60.0d, X 27.884 |
| SENZA-TUTA: Y con Spazio | sopra -59 | -57.214 | -57.489 |
| SENZA-TUTA: Z dopo W 2 s | +< 5 | 4.106 (+3.61) | 4.008 (+3.51) |
| SENZA-TUTA: X nella corrente | oltre 45 | 45.974 | 46.021 |
| SENZA-TUTA: sponda | (controllo) | Y -61.0d, X 24.70 | Y -61.0d, X 24.70 |

## Verifiche di fine task (Step 7), ultima esecuzione, sul codice committato

- `./gradlew --console=plain :1.20.1-forge:build ... :1.21.1-neoforge:collectJar` -> `BUILD SUCCESSFUL`
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `bash tools/run_gametests.sh` (uscita 0):
```
OK 1.20.1-forge: 156 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 156 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 156 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 156 test obbligatori passati, nessun avviso fuori elenco
```
156 = 152 del Task 18 + 4. Nessun server, client o Xvfb lasciato acceso.

## File cambiati

- `src/main/java/com/toxineren/exoframes/frame/SuitMovement.java`
- `src/main/java/com/toxineren/exoframes/frame/SuitTick.java`
- `src/main/java/com/toxineren/exoframes/gametest/MovementGameTests.java` (nuovo)
- `src/main/java/com/toxineren/exoframes/RedstoneExoframes.java`
- `src/main/resources/assets/redstone_exoframes/lang/en_us.json`, `it_it.json`
- `docs/compat.md`

## Scelte

- "Con la tuta" sta in un aiuto privato `suited(player)` di `SuitMovement` (`wearsSuit` per primo, poi
  `!isSpectator()`), invece di ripetere la condizione nei quattro metodi: stessa regola, un solo posto.
- Nei test la vasca, il controllo e la condizione di partenza sono aiuti privati (`suitInPool`, `controlInPool`,
  `settle`, `assertUnderWater`), come `suited` in `DamageGameTests`.
- `waterHintOncePerEntry` rilascia il telaio anche alla fine (`leaveTestFrame` dopo il secondo ingresso), per pulizia.
- Nella condizione di partenza della lava ho asserito `isInLava()` per tutti e due i giocatori (il brief dice
  "condizione di partenza `isInLava()`" senza dire per chi; per l'acqua la vuole per tutti e due).

## Scostamenti dal brief

- **Due `doTick()` invece di uno per la condizione di partenza.** Con un solo `doTick` la condizione
  `isInWater() && isUnderWater()` falliva per la tuta (`inWater true, underWater false`): `Entity.updateFluidOnEyes`
  copia in `wasEyeInWater` (che `isUnderWater()` legge) i fluidi sugli occhi del tick prima e solo dopo li ricalcola,
  quindi gli occhi sott'acqua si vedono al secondo tick. L'aiuto `settle(player)` fa due `doTick()`; la regola del brief
  (nessun campo vecchio, nessun test che passi per la ragione sbagliata) resta, ed e' detta nel Javadoc. Conseguenza in
  `waterHintOncePerEntry`: il secondo `doTick` fa gia' girare `SuitTick` con la tuta in acqua (isInWater gia' vero),
  quindi il suggerimento puo' partire li'; i due tick diretti dopo non lo ripetono e il conteggio resta 1, come atteso
  dal brief; il Javadoc del test lo dice. In `suitDoesNotSwimSprintingUnderwater` il `doTick` dopo `setSprinting(true)`
  resta uno.
- **Ordine dei passi 1-3.** Per avere un RED che compila ho aggiunto la costante `WATER_HINT` e la registrazione in
  `init()` prima del RED (il resto dello Step 3 dopo).
- **Corpo del commit:** dice "vasca profonda 4"; la vasca della ricognizione e' profonda 3 (acqua da -60 a -58). Errore
  di parola nel solo corpo del commit, i numeri sono giusti; non ho riscritto il commit (vietato).

## Autorevisione

- Le quattro regole corrispondono a "Produces" e a design §12.6; il passo 9 e' dentro il `try`, dopo il passo 8, e
  non dipende da creativa (solo lo spettatore e' escluso, come dice il brief).
- `SuitMovement` e' comune e non nomina classi client (`OK sources`); `clientIndicators` e' gia' un metodo comune di
  `FrameStateAccess`.
- Nessuna riga oltre 120 caratteri nei file toccati.
- Lo scatto sotto il 10 % non ha un test qui (il server non ha indicatori): lo prova il client vero al Task 23, come
  dice il brief.
