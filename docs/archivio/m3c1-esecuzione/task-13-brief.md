### Task 13: Il tick dell'energia: attività, salti, consumo

**Files** (Java da `src/main/java/com/toxineren/exoframes/`):
- Create: `core/energy/ActivityRule.java`, `frame/SuitTick.java`, `gametest/EnergyGameTests.java`
- Modify: `frame/WornState.java` (dal Task 3), `RedstoneExoframes.java` (`SuitTick.register()`, registrazione di `EnergyGameTests`), `src/main/resources/assets/redstone_exoframes/lang/en_us.json`, `it_it.json`
- Test: `src/test/java/com/toxineren/exoframes/core/energy/ActivityRuleTest.java` (nuovo)

**Interfaces:**
- Consumes: `FrameRules.tick(FrameState, Activity, EnergyMeter) : int` (RE tolti, 0 senza cella) e `jump(FrameState, EnergyMeter) : int` (`Math.round(jumpCost × moltiplicatore del torso rotto)`, 0 senza cella); `RedstoneExoframes.rules()` (Task 11); `new EnergyMeter(EnergyRates)` (riporto in millesimi; fermo 100, cammino 400, corsa 1000 millesimi per tick con i predefiniti), `EnergyRates.from(ExoframesConfig.Energy)`; `enum Activity { IDLE, WALK, SPRINT }`; `FrameStateAccess.get(Player) : Optional<FrameState>`, `mutate(ServerPlayer, Consumer<FrameState>) : boolean` (falso senza record o in sola lettura, con l'ERROR di `logRefusedChange` una volta), `wornFrameId`; `FrameInteractions.tell(ServerPlayer, String)` (package-private: barra delle azioni); `player.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP))` (uguale sulle due versioni; il giocatore finto ha un contatore vero, da 0); `TickEvent.PLAYER_PRE` (`void tick(Player)`), `PlayerEvent.PLAYER_QUIT`; `WornState.changed(ServerPlayer, String, UUID, ServerLevel, Vec3)` (Task 3: `refreshDimensions` e `FrameLog.event`); `Compat.attributes().clear(LivingEntity)`; aiuti di `GameTestKit`: `now`, `wearScrap`, `stored`, `setStored`, `messages(ServerPlayer, String)`, `leaveTestFrame`; `Compat.gameTests().survivalPlayer(helper)`, `mockServerPlayer(helper)`; `ServerPlayer.setServerLevel(ServerLevel)` (uguale sulle due), `Player.awardStat(ResourceLocation)`, `Entity.startRiding(Entity, boolean)`, `new Boat(Level, double, double, double)` (uguale sulle due), `LivingEntity.hurtTime` (campo pubblico).
- Produces:
  - `core/energy/ActivityRule` (Java puro, istanza per giocatore): `public static final int WINDOW = 4; public static final double MOVING = 0.01; public static final double TELEPORT = 8.0;` `public Activity next(double horizontalDistance, boolean sprinting, boolean passenger)`; `public void reset()`. Regola: passeggero o distanza strettamente sopra `TELEPORT` → `reset()` e `IDLE`; altrimenti il campione entra in una finestra circolare degli ultimi `WINDOW` campioni (corrente compreso); in movimento = almeno un campione della finestra strettamente sopra `MOVING` (non una somma); in movimento e di corsa `SPRINT`, in movimento `WALK`, altrimenti `IDLE`; `reset()` svuota la finestra.
    Javadoc: perché la finestra (in un tick del server arrivano 0-2 pacchetti di movimento), solo orizzontale (una scala a pioli conta come ferma, accettato), le costanti non stanno nella config.
  - `public final class SuitTick` in `frame/`:
    - `public static final String LEAVE_FRAME_FIRST = "message." + RedstoneExoframes.MOD_ID + ".leave_frame_first";` (lo usa anche `SuitRestrictions`, Task 15); `static final int SYNC_INTERVAL = 5;` (usato dal Task 14).
    - Record privato `final class Record` in `private static final Map<ServerPlayer, Record> RECORDS = new MapMaker().weakKeys().makeMap();` (identità dell'oggetto, come `SuitSize.LAST_SEEN`): qui i campi `EnergyMeter meter = new EnergyMeter(EnergyRates.from(RedstoneExoframes.config().energy))`, `ActivityRule activity = new ActivityRule()`, `Vec3 lastPos`, `ResourceKey<Level> lastDimension`, `int jumpBase`, `long lastNow`, `long lastSyncTick` (alla creazione `now - SYNC_INTERVAL`), `boolean dirty`; il Task 14 aggiunge `applied`, `sentEffects`, `sentEnergy`, `sentConditions`, il Task 19 `waterHintShown`.
    - `public static void register()`: `TickEvent.PLAYER_PRE.register(player -> { if (player instanceof ServerPlayer serverPlayer) tick(serverPlayer, serverPlayer.server.getTickCount()); });` e `PlayerEvent.PLAYER_QUIT.register(player -> FAILURES_LOGGED.remove(player.getUUID()));`. In `RedstoneExoframes.init()` subito dopo `SlotGuard.register();`, con un commento (`PLAYER_PRE`, solo server: lì la posizione è l'ultima accettata dal client; i giocatori finti non ricevono l'evento e i test chiamano `tick`).
    - `public static void tick(ServerPlayer player, long now)`: tutto il corpo dentro `try { … } catch (RuntimeException e) { if (FAILURES_LOGGED.add(player.getUUID())) RedstoneExoframes.LOGGER.error("Exoframe tick failed for {}", player.getGameProfile().getName(), e); }` (`FAILURES_LOGGED` = `ConcurrentHashMap.newKeySet()`: una riga per giocatore e sessione, poi silenzio). Passi 1-5 di §12.3, in quest'ordine (corpo, perché l'ordine e le basi non li decide la firma):
      ```java
      FrameStateAccess frames = Compat.frameState();
      Optional<FrameState> worn = frames.get(player);
      Record r = RECORDS.get(player);
      if (worn.isEmpty()) {                                            // 1
          if (r != null) { RECORDS.remove(player); Compat.attributes().clear(player); }
          return;
      }
      int jumps = player.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP));
      if (r == null) {                                                 // 2: basi, prima valutazione da fare
          r = new Record(player.position(), player.level().dimension(), jumps, now);
          RECORDS.put(player, r);
      }
      if (now < r.lastNow) {                                           // orologio tornato indietro
          r.activity.reset();
          r.lastSyncTick = now - SYNC_INTERVAL;
      }
      r.lastNow = now;
      boolean passenger = player.isPassenger();                        // 3: letto prima di stopRiding
      if (passenger) {
          player.stopRiding();
          FrameInteractions.tell(player, LEAVE_FRAME_FIRST);
      }
      Vec3 pos = player.position();                                    // 4: attivita', dopo la discesa
      double distance = Math.hypot(pos.x - r.lastPos.x, pos.z - r.lastPos.z);
      if (!player.level().dimension().equals(r.lastDimension)) {
          r.activity.reset();
          r.lastDimension = player.level().dimension();
          distance = 0.0;                                              // campione IDLE
      }
      r.lastPos = pos;
      Activity activity = r.activity.next(distance, player.isSprinting(), passenger);
      boolean jumped = jumps > r.jumpBase && player.hurtTime == 0;     // [0, 1]; in calo = nuova base
      r.jumpBase = jumps;                                              // a ogni tick, pagato o no
      if (!player.getAbilities().instabuild && !player.isSpectator()) { // 5
          FrameRules rules = RedstoneExoframes.rules();
          Record rec = r;
          int[] drained = {0};
          boolean changed = frames.mutate(player, state -> {
              drained[0] += rules.tick(state, activity, rec.meter);
              if (jumped && state.cell() != null && !state.cell().isEmpty()) {
                  drained[0] += rules.jump(state, rec.meter);
              }
          });
          if (changed && drained[0] >= 1) {
              r.dirty = true;
          }
      }
      ```
    - `static void forget(ServerPlayer player)` = `RECORDS.remove(player)`; `public static boolean hasRecord(ServerPlayer player)` = `RECORDS.containsKey(player)` (per i test).
    - Javadoc della classe: dove gira e perché (§12.3), il record legato all'oggetto (un oggetto nuovo, a ingresso, rinascita e ritorno dall'End, riparte da zero; il riporto in millesimi, al più 1 RE, non si salva), il tempo (`now` entra solo dall'ascoltatore, il record non legge mai `getTickCount()`; `now` minore dell'ultimo fa ripartire finestra e limite), creativa e spettatore (nessun `isCreative()`: i giocatori finti rispondono sempre vero), salti (statistica, base riletta a ogni tick: un salto non pagato non si paga più tardi; nessun costo con `hurtTime > 0` né a cella vuota), cavalcature (motivo: gli occhi della tuta seduta in un tunnel alto due finiscono nel soffitto).
  - `WornState.changed`: in coda, dopo `FrameLog.event`, `if (Compat.frameState().wornFrameId(player).isEmpty()) { SuitTick.forget(player); Compat.attributes().clear(player); }` (lo stato indossato è finito: uscita, morte, `debug wear via`; all'ingresso non applica nulla, lo fa il tick alla prima valutazione); Javadoc aggiornato.
  - `RedstoneExoframes.init()`: `Compat.gameTests().register(EnergyGameTests.class);` subito dopo la riga di `CarryOverGameTests`. `public final class EnergyGameTests` (costruttore pubblico senza argomenti, `@GameTestGenerator public static Collection<TestFunction> generate()`, `import static com.toxineren.exoframes.gametest.GameTestKit.*;`), con gli aiuti privati:
    ```java
    private static final Vec3 FEET = new Vec3(2.5, 1.0, 2.5);
    /** Giocatore in sopravvivenza dentro un telaio Rottame con stored RE, e il tick di preparazione a t0: record creato, 100 millesimi di riporto. */
    private static ServerPlayer prepared(GameTestHelper helper, Vec3 feet, int stored, long t0) {
        ServerPlayer player = Compat.gameTests().survivalPlayer(helper);
        wearScrap(helper, player, feet, stored);
        SuitTick.tick(player, t0);
        helper.assertTrue(SuitTick.hasRecord(player) && stored(player) == stored, "preparation tick failed");
        return player;
    }
    /** Passi in avanti lungo x di step blocchi, uno per tick, da t0 + from a t0 + to compresi, dal punto start. */
    private static void walk(ServerPlayer player, Vec3 start, double step, long t0, int from, int to) {
        for (int i = from; i <= to; i++) {
            player.setPos(start.x + step * (i - from + 1), start.y, start.z);
            SuitTick.tick(player, t0 + i);
        }
    }
    ```
- Testi (prima di `message.redstone_exoframes.frame_taken`, l'ultima chiave dell'elenco già nei file, Task 7): `"message.redstone_exoframes.leave_frame_first": "Leave the exoframe first"` / `"Esci prima dall'esoscheletro"`. `docs/compat.md`: nessuna riga (nessuna cucitura nuova).

- [ ] **Step 1: test JUnit** — `ActivityRuleTest` (una `new ActivityRule()` per test; `I`, `W`, `S` per `Activity.IDLE/WALK/SPRINT`):
  - `stillIsIdle`: cinque `next(0.0, false, false)` → tutti `IDLE`;
  - `exactlyTheThresholdIsStill`: `next(ActivityRule.MOVING, false, false)` (0,01) → `IDLE`; poi `next(0.011, false, false)` → `WALK`;
  - `jitteryWalkStaysWalk`: `0.0` → `IDLE`; poi `0.43, 0.0, 0.43, 0.0, 0.43, 0.0` → sei volte `WALK`;
  - `stopKeepsThreeTicksOfTail`: `0.2` → `WALK`; poi `0.0` tre volte → `WALK, WALK, WALK`; la quarta → `IDLE`;
  - `sprintNeedsMovement`: `next(0.0, true, false)` → `IDLE`; `next(0.2, true, false)` → `SPRINT`; `next(0.2, false, false)` → `WALK`;
  - `teleportIsIdleAndClearsTheWindow`: `0.2` → `WALK`; `9.0` → `IDLE`; `0.0` → `IDLE` (la finestra è vuota, senza la coda di tre tick); su una regola nuova `next(ActivityRule.TELEPORT, false, false)` (8,0) → `WALK` (oltre, non uguale);
  - `passengerIsIdleAndClearsTheWindow`: `0.2` → `WALK`; `next(0.2, true, true)` → `IDLE`; `next(0.0, false, false)` → `IDLE`.
- [ ] **Step 2: Run** `./gradlew --console=plain :1.21.1-neoforge:test --tests '*ActivityRuleTest'` — Expected: FAIL (non compila: `ActivityRule` manca).
- [ ] **Step 3: test in mondo** — in `EnergyGameTests.generate()`, in quest'ordine (tutto nello stesso tick del server: nessuna attesa, tempi `t0 = now(helper)` più k, sempre in avanti; ogni test chiude con `leaveTestFrame` dei suoi giocatori e `helper.succeed()`; i numeri vengono dai predefiniti, che `run_gametests.sh` garantisce togliendo il file di config; `p = prepared(helper, FEET, 5_000, t0)` salvo dove detto, `start = p.position()` subito dopo; `tick(t0 + k)` sta per `SuitTick.tick(p, t0 + k)`):
  - `tickListenerCreatesTheRecord`: `mockServerPlayer`, `wearScrap(helper, player, FEET, 5_000)`; `!SuitTick.hasRecord(player)`; `TickEvent.PLAYER_PRE.invoker().tick(player)` → `SuitTick.hasRecord(player)` (prova la registrazione: il server non manda l'evento ai giocatori finti); `leaveTestFrame(player)` → `!SuitTick.hasRecord(player)` (scartato da `WornState.changed`).
  - `idleDrainsTwoPerSecond`: `p = prepared(helper, FEET, 5_000, t0)`; `for (int i = 1; i <= 20; i++) SuitTick.tick(p, t0 + i);` → `stored(p) == 4_998` (100 + 20 × 100 millesimi).
  - `walkingDrainsEight`: `walk(p, p.position(), 0.2, t0, 1, 20)` → `4_992`.
  - `sprintingDrainsTwenty`: `p.setSprinting(true)`, `walk(p, p.position(), 0.2, t0, 1, 20)` → `4_980`.
  - `aJumpCostsFifteen`: `p.awardStat(Stats.JUMP)`, `tick(t0 + 1)` → `4_985`; `tick(t0 + 2)` → `4_985` (la statistica non è cambiata: nessun secondo salto); `p.hurtTime = 10; p.awardStat(Stats.JUMP); tick(t0 + 3)` → `4_985` (contraccolpo: non si paga); `p.hurtTime = 0; tick(t0 + 4)` → `4_985` (il salto non pagato non si paga più tardi; il riporto è a 500).
  - `noJumpCostWithAnEmptyCell`: `prepared(…, 5_000, t0)`, `setStored(p, 0)`, `awardStat(Stats.JUMP)`, `tick(t0 + 1)` → `stored(p) == 0`; `setStored(p, 5_000)`, `tick(t0 + 2)` → `5_000` (la base è stata riletta anche senza pagare). Un'eccezione nel tick scriverebbe `Exoframe tick failed for …`, che il controllo del log fa fallire.
  - `teleportCountsAsIdle`: `start = p.position()`; `p.setPos(start.x + 9.0, start.y, start.z)`, `tick(t0 + 1)`; fermo `tick(t0 + 2 … t0 + 10)` → `4_999` (con il teletrasporto contato come cammino la finestra darebbe `WALK` per quattro tick: `4_998`).
  - `dimensionChangeRestartsTheWindow`: `ServerLevel overworld = p.serverLevel(); ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);` (premessa `nether != null`); `walk(p, start, 0.2, t0, 1, 3)` → `4_999`; nel `try`: `p.setServerLevel(nether)` alla stessa posizione, `tick(t0 + 4 … t0 + 7)` fermo → `4_999` ("4 998: the window was not restarted at the dimension change"); nel `finally` `p.setServerLevel(overworld)` (tutto nello stesso tick: il livello non vede mai il giocatore nell'altra dimensione).
  - `passengerIsDismountedAndIdle`: `Boat boat = new Boat(level, start.x, start.y, start.z + 2.0); level.addFreshEntity(boat);` nel `try`, per `i` da 1 a 20: `boat.setPos(start.x + 0.2 * i, start.y, start.z + 2.0)`, `helper.assertTrue(p.startRiding(boat, true), "could not board")`, `SuitTick.tick(p, t0 + i)`, `helper.assertTrue(!p.isPassenger(), "still riding after tick " + i)`; poi `stored(p) == 4_998` (non `4_992`: il punto di discesa si sposta di 0,2 a ogni tick, e senza la regola del passeggero sarebbe cammino) e `messages(p, SuitTick.LEAVE_FRAME_FIRST) == 20`; `boat.discard()` nel `finally`.
  - `creativeDrainsNothingBesideSurvival`: `survival = prepared(helper, new Vec3(1.5, 1.0, 1.5), 5_000, t0)`; `creative = mockServerPlayer(helper)`, `creative.setGameMode(GameType.CREATIVE)`, premessa `creative.getAbilities().instabuild`; `wearScrap(helper, creative, new Vec3(1.5, 1.0, 5.5), 5_000)`, `SuitTick.tick(creative, t0)`; `walk(survival, …, 0.2, t0, 1, 20)` e `walk(creative, …, 0.2, t0, 1, 20)` → `stored(survival) == 4_992` e `stored(creative) == 5_000` (senza la coppia, "nessun consumo in creativa" passerebbe anche con un tick che non consuma mai).
  - `spectatorDrainsNothingAndKeepsTheRecord`: `prepared(…, 5_000, t0)`; `p.setGameMode(GameType.SPECTATOR)`; `walk(p, start, 0.1, t0, 1, 20)` → `5_000` e `SuitTick.hasRecord(p)`; `p.setGameMode(GameType.SURVIVAL)`; `walk(p, p.position(), 0.1, t0, 21, 40)` → `4_992` (la seconda metà prova che il tick consuma davvero; il riporto della preparazione, 100, è rimasto).
- [ ] **Step 4: Run** `bash tools/run_gametests.sh 1.21.1-neoforge` — Expected: FAIL (non compila: `SuitTick` ed `EnergyGameTests` mancano).
- [ ] **Step 5: implementare** `ActivityRule`, `SuitTick`, la riga in `WornState.changed`, le due registrazioni e la chiave di testo, come in "Produces".
- [ ] **Step 6: Run** `./gradlew --console=plain :1.21.1-neoforge:test --tests '*ActivityRuleTest'` — PASS; `bash tools/run_gametests.sh` — Expected: quattro righe `OK <variante>: 127 test obbligatori passati, nessun avviso fuori elenco` (116 più 11; uno in più per ciascun ripiego dei Task 11-12), con tutti i test 3b di uscita, morte e `debugWear*` verdi (`WornState` ora toglie anche il record del tick e i modificatori).
- [ ] **Step 7: verifiche di fine task** — `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` → `BUILD SUCCESSFUL`; `bash tools/check_sources.sh` → `OK sources` (regola 1c: `ActivityRule` è nominata in `ActivityRuleTest`); `bash tools/check_jars.sh` → `OK jars`; `grep -rn 'isCreative()' src/main/java/com/toxineren/exoframes/frame/SuitTick.java src/main/java/com/toxineren/exoframes/core/energy/ActivityRule.java | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)'` → nessuna riga (i commenti, che lo nominano per vietarlo, sono esclusi); GameTest già allo Step 6.
- [ ] **Step 8: commit** `frame: tick dell'energia (attivita', salti, creativa, cavalcature)`.

