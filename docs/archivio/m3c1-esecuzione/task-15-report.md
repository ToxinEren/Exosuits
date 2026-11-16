# Task 15 - Niente letti con la tuta: rapporto

Stato: DONE_WITH_CONCERNS (una questione sul client Fabric, sotto "Problemi e dubbi").
BASE ae43516dc8cdded98fee8fd747688867ede21688, HEAD 786c77e3f1989e5ee867a3c49d633a104995a5d3.
Commit: `786c77e frame: con la tuta non si dorme`.

## Cosa ho fatto

- `frame/SuitRestrictions.java` (nuovo): `public final class SuitRestrictions` con
  - `public static void register()` = `InteractionEvent.RIGHT_CLICK_BLOCK.register(SuitRestrictions::onUseBlock);`
  - `public static EventResult onUseBlock(Player player, InteractionHand hand, BlockPos pos, Direction face)`:
    `pass()` se `!SuitSize.wearsSuit(player)` o il blocco in `pos` non e' un `BedBlock`; altrimenti
    `interruptFalse()` sui due lati e per le due mani, e solo dal server (`!player.level().isClientSide()`) e solo per
    `MAIN_HAND` `player.displayClientMessage(Component.translatable(SuitTick.LEAVE_FRAME_FIRST), true)`.
  - Javadoc di classe con i punti del brief (§12.3, domanda 5 di §8, ingombro alto 2,97 in una stanza bassa, due lati
    e due mani, messaggio solo dal server e solo per la mano principale, cavalcature nel passo 3 di `SuitTick`).
- `RedstoneExoframes.init()`: `SuitRestrictions.register();` subito dopo `SuitTick.register();`, con import.
- `gametest/EnergyGameTests.java`: `add(tests, "bedIsRefusedInASuit", ...)` in coda a `generate()` e il corpo del
  test come nel brief (Javadoc in italiano sopra il metodo), import necessari; nel commento di classe aggiunto "letti"
  all'elenco degli argomenti.
- Testi: nessuna chiave nuova. `docs/compat.md`: nessuna riga (nessuna cucitura nuova).

## File cambiati

- `src/main/java/com/toxineren/exoframes/frame/SuitRestrictions.java` (nuovo)
- `src/main/java/com/toxineren/exoframes/RedstoneExoframes.java`
- `src/main/java/com/toxineren/exoframes/gametest/EnergyGameTests.java`

## TDD

RED - `bash tools/run_gametests.sh 1.21.1-neoforge` con il solo test (nessun ascoltatore):
```
[Server thread/ERROR] [minecraft/LogTestReporter]: redstone_exoframes:bed_is_refused_in_a_suit failed at -12413307, -60, -5620021! bed accepted in a suit: dev.architectury.event.EventResult@595ef39c
[Server thread/INFO] [minecraft/GameTestServer]: 1 required tests failed :(
[Server thread/INFO] [minecraft/GameTestServer]:    - redstone_exoframes:bed_is_refused_in_a_suit
FAIL 1.21.1-neoforge: Gradle e' uscito con 1
```
Atteso: senza ascoltatore l'evento restituisce `pass()`, quindi la prima asserzione cade con il messaggio previsto.

GREEN - stesso comando dopo l'implementazione:
```
OK 1.21.1-neoforge: 135 test obbligatori passati, nessun avviso fuori elenco
```

## Verifiche di fine task (tutte dopo l'ultima modifica, prima del commit)

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` -> `BUILD SUCCESSFUL in 21s` (demone caldo; nessun `warning:`/`error:` nel log)
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `bash tools/run_gametests.sh` (quattro varianti):
```
OK 1.20.1-forge: 135 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 135 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 135 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 135 test obbligatori passati, nessun avviso fuori elenco
```
135 = 134 del Task 14 piu' 1 (nessun ripiego preso nei Task 11-12, come dice la tabella).

Nessun build, server o client lasciato in esecuzione (resta solo il demone Gradle inattivo).

## Scelte

- Lato server riconosciuto con `!player.level().isClientSide()` (come `SlotGuard` e `ExoframeEntity`), non con
  `instanceof ServerPlayer`: e' la forma gia' usata nel codice comune e non dipende dalla classe del giocatore.
- Messaggio mandato con `player.displayClientMessage(...)` direttamente, come scrive il brief, e non con
  `FrameInteractions.tell`, che vuole un `ServerPlayer` ed e' package-private: il risultato e' lo stesso.
- Ordine dei controlli: prima `wearsSuit` (economico), poi lo stato del blocco; il parametro `face` non serve e resta
  per la firma dell'evento.

## Scostamenti dal brief

Nessuno nel comportamento. Solo forma: le righe del test oltre 120 colonne sono andate a capo come nel resto del file,
e il commento di classe di `EnergyGameTests` ora nomina anche i letti.

## Autorevisione

Rilette le tre modifiche: firme pubbliche come in "Nomi e chiavi comuni"; nessun `isCreative()`; nessuna condizione
Stonecutter; identificatori e messaggi dei test in inglese, Javadoc in italiano con apostrofi ASCII; registrazione nel
punto chiesto. Il test passa dall'evento, quindi prova anche la registrazione.

## Problemi e dubbi

1. **Sul client Fabric il messaggio non arrivera' mai** (da guardare nella checklist del PC della 3c-2, o da decidere
   ora). Ho letto i sorgenti di Fabric API `fabric-events-interaction-v0` (0.6.6 per 1.20.1 e 0.7.14 per 1.21.1, nella
   cache di Gradle), `ClientPlayerInteractionManagerMixin.interactBlock`: se `UseBlockCallback` restituisce un esito
   diverso da `PASS`, il pacchetto di uso del blocco parte verso il server solo se l'esito e' `SUCCESS`; con `FAIL`
   (che e' cio' che `interruptFalse()` diventa su Architectury Fabric) il client non manda nulla. Quindi su Fabric il
   letto si rifiuta (bene), ma il server non vede il clic e il messaggio "Esci prima dall'esoscheletro" non compare:
   rifiuto muto. Su Forge 1.20.1 e NeoForge 1.21.1 il pacchetto parte comunque dalla predizione del client, il server
   rifa' l'evento e il messaggio arriva. Il design (§12.3) e il brief chiedono proprio "annullato sui due lati,
   messaggio solo dal server", quindi non ho cambiato nulla; una correzione possibile (per esempio mostrare il
   messaggio dal client, oppure non annullare sul client Fabric) cambierebbe la regola del design ed e' una decisione
   del coordinatore. Il GameTest non puo' vederlo (il giocatore finto non ha client).

## Fix round 1

FIX_BASE 786c77e3f1989e5ee867a3c49d633a104995a5d3. Nessuna modifica in sospeso trovata all'avvio (albero pulito).
Seguite le quattro righe `Task 15: Ruling:` del registro: T15-F2 non si tocca (ogni clic destro su un letto con la
tuta resta rifiutato), T15-F1 e T15-F3 corretti qui.

### T15-F1 - il messaggio sui due lati

- `frame/SuitRestrictions.java`, `onUseBlock`: la condizione del messaggio e' ora solo `hand == MAIN_HAND` (tolto
  `!player.level().isClientSide()`): la stessa chiamata `player.displayClientMessage(Component.translatable(
  SuitTick.LEAVE_FRAME_FIRST), true)` sui due lati, solo per la mano principale; `interruptFalse()` per le due mani sui
  due lati come prima. Nessuna cucitura di loader, nessuna classe solo client.
- Javadoc di classe: il messaggio lo mostrano tutti e due i lati, solo per la mano principale; su Fabric un clic
  annullato sul client non arriva mai al server, quindi deve parlare il client; su Forge e NeoForge la copia del server
  finisce nella stessa barra delle azioni con lo stesso testo, che il giocatore legge come un messaggio solo.
- Design §12.3 (`docs/superpowers/specs/2026-11-13-m3-telaio-design.md:469`): "il messaggio lo manda solo il server,
  una volta per clic" diventa "il messaggio lo mostrano tutti e due i lati, solo per la mano principale, quindi una
  volta per clic: su Fabric un clic annullato sul client non arriva mai al server, e se parlasse solo il server il
  rifiuto sarebbe muto; su Forge e NeoForge il pacchetto parte comunque e la copia del server finisce nella stessa riga
  della barra delle azioni, con lo stesso testo, e il giocatore vede un messaggio solo".
- Altri documenti: `git grep -n 'solo il server' -- docs README.md` trova solo quella riga del design. La regola
  vecchia resta, con altre parole ("e solo dal server", "il messaggio solo dal server"), nel testo del Task 15 del
  piano (`docs/superpowers/plans/2026-11-15-m3c1-consumare.md:927-928`): non l'ho cambiato, perche' e' l'ordine del
  task che la decisione del registro sostituisce, e il piano lo tiene chi coordina. Nessun'altra menzione (cercati
  anche `letto`, `BedBlock`, `SuitRestrictions` in `docs`, `README.md`, `tools`).
- Test in mondo invariato: il giocatore finto e' un giocatore del server, quindi un messaggio per la mano principale e
  nessuno per l'altra, come prima.

### T15-F3 - Javadoc di onUseBlock

Tolta la frase "Pubblico per i test." (il metodo e' pubblico perche' il piano lo elenca fra i metodi pubblici; nessun
test lo chiama direttamente).

### Client vero (tre varianti, una alla volta)

Script in una cartella di lavoro fuori dal repository, stesso comando per le tre varianti:
```
EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <variante> .superpowers/sdd/2026-11-15-m3c1-consumare/task-15-client/<variante> \
  "setblock 0 -60 2 minecraft:red_bed[facing=south,part=foot]" \
  "setblock 0 -60 3 minecraft:red_bed[facing=south,part=head]" \
  "execute as Exo run exoframe debug wear rottame 0" \
  "time set 18000" \
  "tp Exo 0.5 -60 0.5 0 47" \
  "@attendi 3" "@scatto mirino" \
  "@click destro" "@attendi 0.3" "@scatto letto-rifiutato" \
  "@attendi 2" \
  "execute unless data entity Exo SleepingX" \
  "@scatto dopo" \
  "time set 6000"
```
Mira: occhi a -60 + 2,67 = -57,33, centro del piede del letto a (0,5, -59,5, 2,5): 2,17 sotto e 2 avanti, pitch 47.
Notte (`time set 18000`) cosi' che un letto non rifiutato facesse dormire davvero (di giorno il gioco base scriverebbe
nella stessa barra un altro testo); il giocatore e' in creativa, quindi niente controllo dei mostri vicini.
`execute unless data entity Exo SleepingX` riesce solo se il giocatore non dorme (lo script si ferma se un comando da'
0). Il mirino ha puntato il letto al primo tentativo su tutte e tre.

| Variante | Uscita | `log-check.txt` | Scatti |
|---|---|---|---|
| 1.21.1-fabric | 0 (72 s) | vuoto (0 byte) | `mirino.png` (letto nel mirino), `letto-rifiutato.png` ("Leave the exoframe first" nella barra delle azioni, in piedi), `dopo.png` (stessa vista, scritta che sfuma) |
| 1.20.1-fabric | 0 (64 s) | vuoto (0 byte) | come sopra |
| 1.21.1-neoforge | 0 (68 s) | vuoto (0 byte) | come sopra; una sola riga nella barra (la copia del client e quella del server coincidono) |

Cartelle: `.superpowers/sdd/2026-11-15-m3c1-consumare/task-15-client/{1.21.1-fabric,1.20.1-fabric,1.21.1-neoforge}/`
(guardati a occhio `mirino.png`, `letto-rifiutato.png`, `dopo.png`). In tutte e tre il comando
`execute unless data entity Exo SleepingX` e' riuscito (`comando eseguito` nel log dello script). Prima della
correzione, su Fabric la barra sarebbe rimasta vuota (T15-F1). 1.20.1-forge e i jar di produzione restano alla
checklist del PC della 3c-2, come dice la decisione.

### Verifiche (dopo l'ultima modifica, prima del commit)

- Durante il lavoro: `bash tools/run_gametests.sh 1.21.1-neoforge` -> `OK 1.21.1-neoforge: 135 test obbligatori passati, nessun avviso fuori elenco`.
- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` -> `BUILD SUCCESSFUL in 19s` (nessun `warning:`/`error:` nel log)
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `bash tools/run_gametests.sh` (copre `bedIsRefusedInASuit`, che passa dall'evento):
```
OK 1.20.1-forge: 135 test obbligatori passati, nessun avviso fuori elenco
OK 1.20.1-fabric: 135 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-fabric: 135 test obbligatori passati, nessun avviso fuori elenco
OK 1.21.1-neoforge: 135 test obbligatori passati, nessun avviso fuori elenco
```

Nessun build, server o client lasciato in esecuzione (resta solo il demone Gradle inattivo).

Commit: `ff75a4c frame: il messaggio del letto rifiutato anche dal client (su Fabric il clic annullato non arriva al server)`; HEAD ff75a4c1316dd5e62d8259560054edbef1da9f9f.
