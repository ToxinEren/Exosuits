### Task 14: Il tick dell'energia: effetti, transizioni, sincronizzazione

**Files** (Java da `src/main/java/com/toxineren/exoframes/`):
- Modify: `frame/SuitTick.java`, `gametest/EnergyGameTests.java`, `src/main/resources/assets/redstone_exoframes/lang/en_us.json`, `it_it.json`, `docs/compat.md`

**Interfaces:**
- Consumes: `SuitTick` del Task 13 (record, passi 1-5, `SYNC_INTERVAL`, `lastNow`, `lastSyncTick`, `dirty`, cattura delle eccezioni); `RedstoneExoframes.rules().evaluate(FrameState) : FrameEffects` (record: `equals` sui valori; `canSprint()` falso in `LOW` e `EMPTY`; `energyState()`; `EnergyState.of` è stretto: 1 000 su 10 000 = 0,10 è `NORMAL`, 999 è `LOW`, 0 è `EMPTY`); `Piece.condition() : PieceCondition`, `FrameState.pieces()`; `FrameStateAccess.syncTo(ServerPlayer)`, `onSynced(Consumer<ServerPlayer>)`, `get` (Task 11); `Compat.attributes().apply(LivingEntity, FrameEffects)` (oggi; idempotente dal Task 16: qui si chiama comunque solo quando gli effetti cambiano); `FrameSounds.play(ServerLevel, Vec3, Sound)`, `Sound.ENERGY_LOW`, `Sound.ENERGY_EMPTY`, `FrameSounds.record(ServerLevel, AABB) : Recording` (Task 9); `FrameInteractions.tell`; `GameTests.syncCount` (Task 11); `GameTestKit.structureBox`, `setStored`, `messages`.
- Produces:
  - Costanti `public static final String ENERGY_LOW = "message." + RedstoneExoframes.MOD_ID + ".energy_low"` e `ENERGY_EMPTY = "message." + RedstoneExoframes.MOD_ID + ".energy_empty"` in `SuitTick`.
  - Campi nuovi del record: `FrameEffects applied` (gli ultimi effetti valutati dal tick; `null` = prima valutazione da fare), `FrameEffects sentEffects`, `EnergyState sentEnergy`, `Map<PieceSlot, PieceCondition> sentConditions` (ciò che è partito con l'ultima sincronizzazione, chiunque l'abbia chiamata).
  - Passi 6-8 in coda al tick, dopo il passo 5 (corpo, perché le basi del confronto non le decide la firma):
    ```java
    FrameState state = worn.get();
    FrameEffects fx = RedstoneExoframes.rules().evaluate(state);       // 6
    boolean first = r.applied == null;
    if (!fx.equals(r.applied)) {
        Compat.attributes().apply(player, fx);
    }
    EnergyState energy = fx.energyState();
    if (first) {                                                        // rientro o rinascita: non e' un evento
        if (energy != EnergyState.NORMAL) {
            FrameInteractions.tell(player, energy == EnergyState.LOW ? ENERGY_LOW : ENERGY_EMPTY);
        }
    } else if (energy == EnergyState.EMPTY && r.applied.energyState() != EnergyState.EMPTY) {
        FrameInteractions.tell(player, ENERGY_EMPTY);
        FrameSounds.play(player.serverLevel(), player.position(), Sound.ENERGY_EMPTY);
    } else if (energy == EnergyState.LOW && r.applied.energyState() == EnergyState.NORMAL) {
        FrameInteractions.tell(player, ENERGY_LOW);
        FrameSounds.play(player.serverLevel(), player.position(), Sound.ENERGY_LOW);
    }                                                                   // i miglioramenti tacciono
    r.applied = fx;
    if (!fx.canSprint() && player.isSprinting()) {                      // 7
        player.setSprinting(false);
    }
    if (first || !fx.equals(r.sentEffects) || energy != r.sentEnergy   // 8
            || !conditions(state).equals(r.sentConditions)
            || (r.dirty && now - r.lastSyncTick >= SYNC_INTERVAL)) {
        Compat.frameState().syncTo(player);                             // afterSync aggiorna il record
    }
    ```
    con `private static Map<PieceSlot, PieceCondition> conditions(FrameState state)` (una `EnumMap` slot → `piece.condition()` dei pezzi montati). Le transizioni confrontano con `applied`, mai con i `sent*`: una sincronizzazione strutturale fuori dal tick (`changeWorn`) aggiorna i `sent*` e non deve zittire l'avviso.
  - `private static void afterSync(ServerPlayer player)`, registrato in `register()` con `Compat.frameState().onSynced(SuitTick::afterSync);`: solo se il record esiste; se `get(player)` è presente, `sentEffects = rules().evaluate(state)`, `sentEnergy = sentEffects.energyState()`, `sentConditions = conditions(state)`; in ogni caso `lastSyncTick = r.lastNow` (il `now` dell'ultimo tick del record, mai `getTickCount()`: i test passano il loro orologio) e `dirty = false`. Corpo dentro la stessa cattura di `tick` (`RuntimeException` → l'ERROR `Exoframe tick failed for {}` una volta per giocatore), perché gira dentro ogni `syncTo`, anche nell'ingresso e nell'uscita.
  - `static void markDirty(ServerPlayer player)` (package-private, per `SuitDamage`, Task 17): `dirty = true` se il record esiste; senza record nulla (il primo tick sincronizza comunque).
  - Javadoc di `SuitTick` completato con la regola di §12.4 (subito al primo tick, a un cambio di effetti, di stato dell'energia o di condizione dei pezzi rispetto all'ultima sincronizzazione; altrimenti al più ogni 5 tick finché c'è qualcosa "da sincronizzare": un consumo di almeno 1 RE o, dal Task 17, l'usura) e con le transizioni (prima valutazione: messaggio di `LOW` o `EMPTY` senza suono; dopo, solo i peggioramenti, con messaggio e suono).
- Testi (prima di `message.redstone_exoframes.leave_frame_first`, Task 13): `"message.redstone_exoframes.energy_low": "Energy low: no sprinting"` / `"Energia bassa: niente scatto"`; `"message.redstone_exoframes.energy_empty": "Energy empty: the exoframe barely moves"` / `"Energia esaurita: la tuta si muove appena"`. `docs/compat.md`, `FrameStateAccess` "A cosa serve" (`:68`), dopo "(al massimo 4-5 volte al secondo)": "; il chiamante di oggi è `frame/SuitTick`, che sincronizza subito a un cambio di effetti, di stato dell'energia o di condizione di un pezzo e altrimenti al più ogni 5 tick, solo quando un consumo ha tolto almeno 1 RE (un tick fermo non manda nulla)". (La frase di `FrameAttributes` e di `docs/compat.md:52` sul listener di respawn come "punto naturale" di `apply` la riscrive il Task 16.)

- [ ] **Step 1: test in mondo** — in coda a `EnergyGameTests.generate()`, con `prepared`, `walk` e `FEET` del Task 13; `int synced = Compat.gameTests().syncCount(p)` letto subito dopo `prepared`; dove il test guarda i suoni, il registratore `try (FrameSounds.Recording sounds = FrameSounds.record(helper.getLevel(), structureBox(helper)))` si apre dopo l'ingresso (che suona `ENTER`), nel punto detto per ciascun test (registra solo la sua struttura: i test girano insieme); `start = p.position()` dopo la preparazione. Il conto che fissa i tempi: dopo la preparazione il riporto è 100 millesimi; camminando (400 per tick) 1 RE esce a `t0+3`, `t0+5`, `t0+8`, `t0+10`, `t0+13`, `t0+15`, `t0+18`, `t0+20`; la preparazione sincronizza (prima valutazione) e fissa `lastSyncTick = t0`.
  - `firstEvaluationOfALowCellTellsWithoutSound`: la preparazione per esteso, perché il registratore deve vedere il tick di preparazione ma non il suono `ENTER` dell'ingresso: `p = survivalPlayer(helper)`, `wearScrap(helper, p, FEET, 500)` (5 %, `LOW`), `int synced0 = syncCount(p)`, poi il registratore, poi `SuitTick.tick(p, t0)` → `messages(p, SuitTick.ENERGY_LOW) == 1`, `messages(p, SuitTick.ENERGY_EMPTY) == 0`, `sounds.sounds().isEmpty()`, `syncCount(p) == synced0 + 1` (la prima valutazione sincronizza).
  - `lowTransitionTellsSoundsAndSyncsOnce`: `prepared(helper, FEET, 1_000, t0)` (0,10: `NORMAL`, nessun messaggio); registratore; `p.setSprinting(true)`, `p.setPos(start.x + 0.3, start.y, start.z)`, `tick(t0 + 1)` → `stored(p) == 999`, `messages(p, ENERGY_LOW) == 1`, `sounds.sounds().equals(List.of(Sound.ENERGY_LOW))`, `syncCount(p) == synced + 1` (una sola: lo stato dell'energia è cambiato, e la stessa chiamata toglie il segno), `!p.isSprinting()`; poi i miglioramenti tacciono: `setStored(p, 5_000)`, `tick(t0 + 2)` → `messages(p, ENERGY_LOW) == 1`, `sounds.sounds().equals(List.of(Sound.ENERGY_LOW))`.
  - `emptyTransitionTellsAndSounds`: preparazione per esteso come nel primo test, con 1 RE (`int synced0 = syncCount(p)` letto dopo `wearScrap`, registratore aperto dopo `wearScrap` e prima del tick a `t0`) → `messages(p, ENERGY_LOW) == 1` e nessun suono; corsa con spostamento di 0,3, `tick(t0 + 1)` → `stored(p) == 0`, `messages(p, ENERGY_EMPTY) == 1`, `sounds.sounds().equals(List.of(Sound.ENERGY_EMPTY))`, `syncCount(p) == synced0 + 2` (una alla prima valutazione a `t0`, una al passaggio in `EMPTY`), `!p.isSprinting()`.
  - `lowAfterAChangeWornStillSpeaks`: `prepared(…, 5_000, t0)`; registratore; `setStored(p, 999)` (sincronizzazione strutturale: `afterSync` mette `sentEnergy = LOW`); `int afterChange = syncCount(p)`; `tick(t0 + 1)` fermo → `messages(p, ENERGY_LOW) == 1`, `[ENERGY_LOW]`, `syncCount(p) == afterChange` (gli effetti mandati sono già quelli: nessuna seconda sincronizzazione; prova anche che `afterSync` ascolta `syncTo`).
  - `idleTickWithoutAWholeReDoesNotSync`: `prepared(…, 5_000, t0)`; per `i` da 1 a 8 `tick(t0 + i)` e dopo ognuno `syncCount(p) == synced` (riporto da 200 a 900: lo stato non cambia); `tick(t0 + 9)` → `stored(p) == 4_999` e `syncCount(p) == synced + 1` (segnato e `9 ≥ 5`).
  - `walkingSyncsEveryFiveTicks`: `prepared(…, 5_000, t0)`; `List<Integer> at = new ArrayList<>()`; per `i` da 1 a 20: `p.setPos(start.x + 0.2 * i, start.y, start.z)`, `tick(t0 + i)`, se `syncCount(p)` è cresciuto `at.add(i)` → `at.equals(List.of(5, 10, 15, 20))` e `stored(p) == 4_992`.
  - `clockGoingBackwardsRestartsTheLimits` (unica eccezione voluta al "sempre in avanti"): `prepared(helper, FEET, 5_000, t0 + 100)`; `walk(p, start, 0.2, t0, 101, 103)` → `stored(p) == 4_999` e `syncCount(p) == synced` (a `t0+103` esce 1 RE ma `103 − 100 < 5`); `tick(t0)` fermo → `syncCount(p) == synced + 1` (`now < lastNow`: `lastSyncTick = now − 5`, e il segno è ancora su; senza la regola `now − lastSyncTick` sarebbe −100 e non partirebbe nulla).
- [ ] **Step 2: Run** `bash tools/run_gametests.sh 1.21.1-neoforge` — Expected: FAIL (non compila: `ENERGY_LOW` manca; poi, aggiunte le costanti, falliscono i sette test: nessun messaggio, nessun suono, nessuna sincronizzazione dal tick).
- [ ] **Step 3: implementare** i passi 6-8, `afterSync`, `markDirty`, la registrazione di `onSynced` e le due chiavi, come in "Produces".
- [ ] **Step 4: Run** `bash tools/run_gametests.sh` — Expected: quattro righe `OK <variante>: 134 test obbligatori passati, nessun avviso fuori elenco` (127 più 7; uno in più per ciascun ripiego dei Task 11-12); i test del Task 13 invariati e verdi.
- [ ] **Step 5:** `docs/compat.md` come in "Interfaces".
- [ ] **Step 6: verifiche di fine task** — `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` → `BUILD SUCCESSFUL`; `bash tools/check_sources.sh` → `OK sources`; `bash tools/check_jars.sh` → `OK jars`; `grep -n 'getTickCount' src/main/java/com/toxineren/exoframes/frame/SuitTick.java | grep -vE '^[0-9]+:[[:space:]]*(\*|//|/\*)'` → una sola riga, nell'ascoltatore di `register()` (il Javadoc che lo nomina è escluso); GameTest già allo Step 4.
- [ ] **Step 7: commit** `frame: effetti, avvisi dell'energia e sincronizzazione del tick`.

