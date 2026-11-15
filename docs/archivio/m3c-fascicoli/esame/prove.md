# Part 3c — examination through the "can it be built and PROVEN" lens

Examiner's report on `m3c/direzione-decisioni.md` (draft 1). Read-only. Evidence tags as in the dossiers: [code] repo file:line (HEAD `d88e1e0`, src = `2e35aa6`), [dossier] `m3c/verifica/<name>.md §`, [log] `versions/<v>/build/gametest/server.log` of 14 Nov, [javap] checked here. `J/` = `src/main/java/com/toxineren/exoframes/`, `GT` = `J/gametest/ExoframeGameTests.java`.

## Findings (most severe first)

### F1 — blocker — 7.2: the cell swap through `update` can create a frame out of nothing
- Problem: `FrameStateAccess.update` creates a fresh worn record when the player has none (`J/compat/impl/FrameStateAccessImpl.java:66-78` → `FrameStateSavedData.updateWorn` → `getOrCreateWorn` `:317-327`: `new FrameRecord(UUID.randomUUID(), …)`, structural). 7.2 routes the swap "through `update`". A `cell_swap` packet handled after a `frame_exit` in the same tick (both keys held: V 1 s and R 2 s), or any lying client, gives a player who is not wearing a brand-new bare worn frame with the inventory cell moved into it; `SlotGuard` then dresses him (`repo.md` §4: guard repairs a record without placeholders). A frame is duplicated from nothing and the cell leaves the inventory into it.
- Evidence: [code] the lines above; `FrameEntry.registerNet` guards only through `exit` (`J/frame/FrameEntry.java:210-213`, `exit` checks wearing); `repo.md` §2.1 table row "no worn record → creates".
- Proposal: 7.2 says: "the handler first requires a worn record (`Compat.frameState().get(player).isPresent()`, alive, not spectator), else does nothing and says nothing; the write goes through a new `FrameStateAccess.updateWorn(ServerPlayer, Consumer<FrameState>) → boolean` that never creates a record (structural + sync like `update`)". GameTest `cellSwapWithoutAFrameChangesNothing`: mock not wearing, a charged cell in slot 0, call the handler → no worn record, cell still in slot 0, record count unchanged.

### F2 — blocker — 7.2 (and 8.3): inventory writes outside the registry consumer can lose the cell
- Problem: on a read-only registry `update` logs once and returns without calling the consumer and without telling the caller (`FrameStateAccessImpl.java:68-71`, returns `void`). If the handler empties the inventory slot first and then calls `update`, the cell vanishes. 7.2 does not fix the order.
- Evidence: [code] above; `repo.md` §2.1 ("registry read-only … returns: no change, no dirty, no sync").
- Proposal: 7.2 adds: "the inventory slot is written inside the registry change (the consumer of `updateWorn`), after the new cell is installed: if the change is refused nothing in the inventory moves". Pin a pure rule `core/energy/CellSwapChoice.pick(int installedStored /* -1 = no cell */, int[] slotEnergies /* -1 = not a cell */) → int slot or -1` (JUnit: order hotbar 0-8 then 9-35, strictly more energy, no cell + any charge, all empty → -1). GameTest asserts conservation: number of cell items in inventory + frame identical before and after, sum of stored RE identical.

### F3 — major — missing: no way to set or read the worn cell's energy from a script
- Problem: the cloud proofs of 5.3 (sprint off under 10 %), 3.6 (LOW/EMPTY messages), 10.2 (HUD colours) and the energy part of "done when" need a suit in LOW/EMPTY on a real client. The creative tab only gives a full cell (`CellItem.charged()`, `GT:913-917`); reaching 999 RE from 10 000 by sprinting takes (10 000 − 999)/20 = 450 s; `/give` with data needs a per-version syntax (1.20.1 NBT `{exoframes:{energy:…}}`, 1.21.1 `[minecraft:custom_data=…]`, `docs/compat.md:29`). The experiment faked LOW with redstone in the off hand (`ricognizione-movimento-e-dati.md` §1(c)). RCON `data get` cannot read the registry.
- Evidence: arithmetic above; [code] `DebugWear.java:68-83` always installs a full cell.
- Proposal: add [P] 11.8 "`/exoframe debug energy` (operators) prints `stored/capacity` and the energy state of the worn cell; `/exoframe debug energy <RE>` sets it (clamped), through `updateWorn` (immediate sync)". GameTest via `exoframeCommand(...)` (`GT:3919`); the real-client script and the PC checklist use it.

### F4 — major — 3.3: per-tick activity under-counts walking on a real server, and no cloud test can see it
- Problem: 3.3 classifies one tick at a time (`> 0.01` blocks moved in the tick). Move packets are handled between server ticks; 0, 1 or 2 land in one tick (`test.md` §3: "per-tick values are jittery … classify over a short window"). Each 0-packet tick of a walking player is IDLE (0.1 RE instead of 0.4), each 2-packet tick is one WALK: walking drains less than 8 RE/s, sprinting is also split. On the cloud the client and server share localhost, jitter is near zero, so a real-client drain test passes for the wrong reason; the mock never moves, so GameTests cannot reproduce it.
- Evidence: `test.md` §3 last paragraph and §6.6 ("thresholds and window length … need numbers from a real client [not measured]"); `EnergyRates.spec()` = 2/8/20 RE/s (`stato-del-codice.md` §1.4).
- Proposal: 3.3 says: "activity is decided over the last `energy.activityWindowTicks` = 4 ticks: moving = horizontal distance summed over the window > `energy.walkThreshold` × window; sprint = moving and `isSprinting()` now; the window resets on dimension change, passenger, teleport (> 8 blocks in one tick), and its first sample is IDLE". Put the classifier in `core/energy/ActivityWindow` (pure; `check_sources.sh` rule 1c then requires a test naming it) with JUnit fed with synthetic jitter sequences (0,0.43,0,0.43… must be WALK every tick; 0,0,0,0 IDLE; 9.0 → IDLE and reset). Spike task: log per-tick distances of a real client walking and sprinting 5 s (`client_shot.sh`, localhost) to fix the numbers; the PC checklist covers a remote server.

### F5 — major — 3.5, 3.6, 5.4, 5.5, 12: nothing a GameTest can observe proves a sync or a packet
- Problem: the decisions promise "immediate sync", "at most every 5 ticks", indicators in the owner packet and a `client_rules` packet, and §12 lists the transitions as GameTests. A mock's packets go into an `EmbeddedChannel` nobody reads (`J/compat/impl/GameTestsImpl.java:102-103`); on 1.21.1 NeoForge `sendToPlayer` returns silently when the client did not negotiate the channel (`J/compat/impl/NetImpl.java:94-101`), which is the mock's case. No JUnit or GameTest touches a packet today (`repo.md` §7). So "sync happened" and "packet carries X" are unprovable as written; a test would pass even with the sync call removed.
- Evidence: [code] above; `repo.md` §7 last bullet.
- Proposal: pin (a) a pure `compat/OwnerIndicators.of(FrameState, ExoframesConfig) → record(canSprint, canSwim, EnergyState)` (or in `core/frame`), JUnit; (b) a pure `frame/SyncThrottle` with `boolean due(long now, boolean structural, boolean energyChanged)` and JUnit (interval 5, first call due); (c) a test-only counter `GameTests.syncCount(ServerPlayer)` incremented in `FrameStateAccessImpl.syncTo`, so GameTests assert "one sync at the LOW transition, none on an unchanged tick"; (d) a JUnit round trip of `FrameStateSyncPacket.CODEC` and the `client_rules` codec over `new FriendlyByteBuf(Unpooled.buffer())` — first confirm in one test that `FriendlyByteBuf` loads without a game bootstrap on both versions, else move it to a GameTest. The receipt on the client is proven only by the real-client run (HUD and sprint behaviour) — say so in §12.

### F6 — major — 3.2, 12: the energy tick must be a method tests call with their own clock
- Problem: mocks never fire `PLAYER_PRE` (`test.md` §1.2-1.3: only `doTick()` runs `Player.tick`, and `doTick()` adds physics, food and healing). The decision registers the tick on the event but names no callable entry and keys the throttle and cooldown on unspecified clocks. A test calling the tick N times inside one server tick sees one `server.getTickCount()`: a throttle on that clock suppresses every sync, a modulo-5 clock makes results depend on the test's start tick (flaky across variants, whose batches start at different ticks).
- Evidence: `test.md` §6.5 ("the per-player server tick must be a directly callable method"); [log] 1.21.1 runs two batches (`versions/1.21.1-fabric/build/gametest/server.log:118,205`), so tests start at different server ticks.
- Proposal: 3.2 pins `public static void SuitTick.tick(ServerPlayer player, long now)`; the listener is `p -> { if (p instanceof ServerPlayer sp) tick(sp, sp.server.getTickCount()); }`; every time rule (sync interval, swap cooldown) compares `now` with a stored `long`, never `now % n`. GameTests pass `now = t0 + i`. One wiring test calls `TickEvent.PLAYER_PRE.invoker().tick(mock)` once and asserts the record exists (pattern of `GT:2494`, which does it for `PLAYER_POST`).

### F7 — major — 12, 4.2: version-specific tests collide with the count rule and the no-directive rule
- Problem: `run_gametests.sh` requires the same N = number of literal `add(tests,` lines on every variant (`tools/run_gametests.sh:20-21, 39-41`), and `gametest/` may not hold Stonecutter directives (`check_sources.sh` rule 6, `repo.md` §8). "Step modifier on 1.21.1" cannot be a 1.21.1-only test. On 1.20.1 the same assertion `maxUpStep() == 1.0F` passes with no mod code (`ServerPlayer.<init>` sets 1.0F, `attributi.md` §2): wrong-reason pass.
- Evidence: [code] the lines above; `attributi.md` §2 table.
- Proposal: §12 says "every GameTest exists on the four variants; a version difference in the expectation comes from a `compat/GameTests` helper (e.g. `boolean stepIsAttribute()` and `double frameModifier(ServerPlayer, String id)` returning NaN when absent)". The step test asserts the modifier on 1.21.1 and its absence plus 1.0F on 1.20.1; the 1.20.1 client step is proven only by the real client (`autoJump:false`, scalino alto 1, Y +1).

### F8 — major — 4.1: the respawn hook needs a seam, and only the End return can test it
- Problem: `PLAYER_RESPAWN` has two arities (2 on 9.2.14, 3 on 13.0.11, `attributi.md` §5); a listener in `frame/` cannot carry the directive (rule 6) and `compat/` imports nothing from `frame/` (`repo.md` §4 end). Death already clears the worn record (`FrameEntry.onDeath`, `FE:192-203`), so "re-applied at respawn" has a subject only for `keepEverything = true` (End → Overworld, `test.md` §5). A respawned player is a plain `ServerPlayer` with `spawnInvulnerableTime = 60` again and records no messages (`test-verifica.md` missed 5).
- Evidence: [code] `J/compat/impl/FrameStateAccessImpl.java:299-301`; dossiers cited.
- Proposal: 4.1 pins `Compat.events().onRespawn(Consumer<ServerPlayer>)` (directive inside `compat/impl`) or a neutral callback list in `FrameStateAccess`; and a seam `GameTests.respawn(ServerPlayer, boolean keepEverything) → ServerPlayer` (1.21.1: `RemovalReason.CHANGED_DIMENSION`/`KILLED`). GameTest: wear, apply, `respawn(p, true)` → the new object has `frame_armor` once; `respawn(p, false)` after death → no modifier.

### F9 — major — scope: this is two parts, not one
- Problem: the decisions imply 27 tasks (list below), against 16 for 3b, which the design called the size limit when it split M3 ("oltre trenta task" for all of M3, §4.6). Four new seams (block, block entity, tooltip item, HUD), three mixin targets, two packets, a generator with a CI check, a block with hoppers, seven carry-overs that rewrite 3b code first. Each half can be playable.
- Evidence: task list below; design §4.6.
- Proposal: [Q] to the owner, default "split": **3c-1 "Consumare"** = tasks 1-19 (carry-overs, config, damage, fall, attributes and step, energy tick, owner packet, water/sprint, cell item bar, cell swap, HUD, debug energy). Done when: "with items from the creative tab, on the four variants: enter, consume energy walking/sprinting/jumping, see the charge (HUD, bar), go LOW (no sprint) and EMPTY, take damage (pieces wear and break, legs from falls), swap to a charged cell; build, tests, GameTests, dedicated server green; real-client run of water, step, sprint, HUD". **3c-2 "Ricaricare"** = tasks 20-27 (block seams, charger, hoppers, data generator, recipes, advancements, loot, tags, lang, checklist), done when = the design's row 3c. The binding row is not cut, only delivered in two merges; the reason is review size and the block-entity risk being isolated.

### F10 — major — missing: the first GameTest must be the harness probe, with the survival helpers
- Problem: every damage, fall and timing test rests on "the level runs `ServerPlayer.tick()` on a mock" and on 60 calls of `tick()` ending spawn protection — read in bytecode, never run (`test.md` open points; `test-verifica.md` C1). The decisions do not schedule it. If it fails on a variant, a dozen tests fail at once late in the part.
- Evidence: `test.md` §6.1-6.3; `GT:136` comment is wrong (`test.md` §0).
- Proposal: task 1 of the plan: helpers `survivalMock(helper)` (= mock + `setGameMode(SURVIVAL)` + 60 × `tick()`, moved into the structure) and the probe test (`int t = p.tickCount; hurt(cactus, 1.0F); runAfterDelay(5) → tickCount == t + 5 && invulnerableTime == 15`), plus the comment fix at `GT:136`.

### F11 — major — 3.1/3.3/3.4, 12: energy tests that pass for the wrong reason
- Problem: "energy consumption over N ticks" on a mock that never moves covers only IDLE; a mock is in CREATIVE (`GameTestServer.create`, `test.md` §1.5) so a drain path that wrongly reads `abilities.instabuild`/`isCreative()` makes "no drain in creative" pass trivially and makes `isCreative()` always true (`GameTestsImpl.java:134-137`); a first-sample baseline adds or removes one tick.
- Evidence: rates 100/400/1000 milli-RE per tick idle/walk/sprint (`EnergyMeter.tick`, `stato-del-codice.md` §1.4: round(perSecond × 1000 / 20)), jump 15.
- Proposal: §12 lists exact cases, each on a survival mock with a cell of 5 000 RE and expectations computed from `config()`: 20 calls IDLE → −2; 20 calls with `setPos(x + 0.2 · i)` → −8; same with `setSprinting(true)` → −20; one `awardStat(Stats.JUMP)` → −15; passenger → IDLE; 9-block jump in one tick → IDLE; a creative mock and a survival mock side by side, the survival one must drain (pairing). LOW: cell at 1 000 (fraction 0.10 is NORMAL, `EnergyState.of` is strict), one SPRINT call → 999, LOW, one action-bar message, `isSprinting()` false after; EMPTY: 1 → 0. Code rule in the plan: no `isCreative()` in 3c code (`test.md` §6.4).

### F12 — major — 2.1-2.2, 12: fall tests need the frameless control and the survival mock
- Problem: "fall without damage" passes with no mod code in creative (`Player.causeFallDamage` returns at `mayfly`) and inside the first 60 ticks (spawn protection, falls included since `isDedicatedServer()` is false on `GameTestServer`); `causeFallDamage` returns true even when cancelled. A mock cannot really fall (`ServerPlayer.checkFallDamage` is empty).
- Evidence: `test.md` §1.6(c) and "Control for (c), mandatory"; `test-verifica.md` C7.
- Proposal: §12 fall test = survival mock, `p.causeFallDamage(10.0F, 1.0F, p.damageSources().fall())`, assert health unchanged and each leg lost the routed share of 7 (FALL primaries: 7 points → 0.6 × 7 = 4.2 split over two legs = 2.1 each → 2 points, carry 100 each, with the default 0.6 primary share and the other four pieces sharing 2.8), and in the same test a frameless survival mock loses exactly 7.0. Ender pearl: `p.hurt(fall(), 5.0F)` direct. Guards of 2.2 (creative, spectator, `abilities.invulnerable`, dead, client side: LIVING_HURT fires on `LocalPlayer` on Fabric/Forge, `danno.md` §1) each get one assertion.

### F13 — major — 8.2-8.5, 12: charger tests cannot wait for the game
- Problem: a full charge at 50 RE/s is 10 000 / 50 = 200 s = 4 000 ticks; a test has 100 ticks minus up to 20 for entities (`GT:150-152`); a hopper moves one item per 8 ticks; `helper.assertContainerContains` throws for a non-`BaseContainerBlockEntity` (`blocco.md` §7). The `lit` change rewrites the blockstate and calls `onRemove` (`blocco.md` §1.2): a missing guard drops the cell on every lit toggle, which no listed test would catch. The hopper-out failure path puts back a copy (`setItem` after `removeItem`) — the dup path — and is not listed.
- Evidence: arithmetic; dossiers cited.
- Proposal: pin `public static void ChargerBlockEntity.serverTick(Level, BlockPos, BlockState, ChargerBlockEntity)` (the ticker calls it) and `core/energy/ChargerRule` (JUnit: dust → buffer only when buffer empty and cell not full, 2.5 RE/tick with carry, nothing without a cell, full cell stops). GameTests: direct-tick charge (cell 9 990 + 1 dust, 4 calls → 10 000, dust 0, buffer 490); one real-time ticker test (20 ticks → +50 ± 2.5, polled with `succeedWhen`); `lit` toggles twice and the cell is still inside and nothing dropped; hopper in (top cell, side dust); hopper out only when full; hopper out into a full hopper → exactly one cell exists; break drops cell (with energy) + dust and the test discards them; save/load round trip through `GameTests.saveBlockEntity/loadBlockEntity`; clicks through `player.gameMode.useItemOn` on a survival mock (creative skips consumption).

### F14 — major — 1.1/1.2 and 12: the wear hook is untested on the path that matters most (effects after a break)
- Problem: 1.2 leaves the armor recomputation to the energy tick, which never runs for a mock unless called; the summary redraw needs `SlotGuard.tick`. A test "piece breaks → armor attribute drops, summary shows BROKEN" passes only if it calls both; as listed ("wear from a known hit") it checks durability only.
- Evidence: `repo.md` §2.2 (summary on the next `PLAYER_POST`, mocks need `SlotGuard.tick`); `danno.md` §8.5 (do not change attributes inside the hook).
- Proposal: §12 adds "break test": Scrap helmet at durability 1, survival mock, `hurt(cactus, 8.0F)`, then `SuitTick.tick(p, now)` and `SlotGuard.tick(p)` → helmet BROKEN, `frame_armor` = 15 − round(0.15 × 15) per `FrameRules.evaluate`, one action-bar message. Known-hit test numbers (GENERIC, full Scrap, 8.0 → 8 000 milli): helmet 1 (carry 200), torso 2 (400), arms 1 each, legs 1 each (200).

### F15 — major — 5.1, 12: the water GameTest proposed by the experiment passes or fails for the wrong reason
- Problem: `isInWater()` returns the field `wasTouchingWater`, written only by the protected `updateInWaterStateAndDoFluidPushing()` inside `Entity.baseTick`, i.e. inside `doTick()` for a mock [javap V20/V21 here: `protected boolean wasTouchingWater; public boolean isInWater(); protected boolean updateInWaterStateAndDoFluidPushing()`]. A mock placed in water without `doTick()` is "not in water": "in lava `isAffectedByFluids()` true" and "after a tick `isSwimming()` false" pass with no mixin at all; "in water → false" fails though the mixin works.
- Evidence: javap above; experiment's test list `ricognizione-movimento-e-dati.md` §1(a) "Test".
- Proposal: §12 water tests: survival mock in a 3-deep pool, `p.doTick()` once, assert precondition `p.isInWater()` (and `isUnderWater()`), then the mixin results; frameless control in the same pool expects vanilla values; `setSprinting(true)` + `doTick()` → `isSwimming()` false only with the suit (control true). Everything about movement (sinking, bottom speed, current) stays with the real client.

### F16 — minor — 1.4/1.1: wear amounts differ between versions for helmet sources
- Problem: on 1.21.1 `hurtHelmet` multiplies `DAMAGES_HELMET` hits by 0.75 before the gate, on 1.20.1 after `actuallyHurt` (`danno.md` §7): the amount at `hurtArmor` for an anvil is 75 % on 1.21.1 only. Any test with exact numbers on such a source differs per version.
- Proposal: §12: exact-number wear tests use `cactus`; the HEAD test asserts only that `classify` returns HEAD for `damages_helmet` sources and that the helmet is a recipient; the design states the 1.21.1 ×0.75 as accepted.

### F17 — minor — 12: "two hits inside the window count once" is wrong for a larger second hit
- Problem: inside the window a second hit with `amount > lastHurt` passes with the difference (`test.md` §1.6(b)): the hook sees `amount − lastHurt`.
- Proposal: §12 reads "a second equal or smaller hit in the same tick does not wear; a larger one wears by the difference"; two assertions in one test (5.0 after 8.0 → no change; 10.0 after 8.0 → wear routed from 2 000 milli).

### F18 — minor — 1.5, 3.6, 7.2, 8.3, 10.1: sounds cannot be proven in the cloud
- Problem: the headless client has no audio and the server sends sounds as packets nobody inspects; nine sound events are promised.
- Proposal: all sounds through one `frame/FrameSounds.play(ServerLevel, double x, double y, double z, SoundEvent)` (`level.playSound(null, …, SoundSource.PLAYERS/BLOCKS, 1, 1)`, `attributi.md` §8.8) with a test-only recorder (`GameTests.lastSounds(level)`); GameTests assert the event per action; audibility goes to the PC checklist. Pin the table event → `SoundEvents` constant (36 safe ones, `attributi.md` §3).

### F19 — minor — 4.2: compare-then-set every client tick fights other mods; the 1.20.1 proof is client-only
- Problem: "0.6F otherwise, checked every client tick" resets every other mod's step on 1.20.1 every tick, wearing or not.
- Proposal: 4.2: "the client sets 1.0F while wearing and restores 0.6F only if the mod set it (a flag per `LocalPlayer` instance, re-evaluated when the instance changes)". Proof: real client, scalino alto 1 with `autoJump:false`, on both 1.20.1 variants; GameTests cannot prove 1.20.1 (F7).

### F20 — minor — 6.2: the config loader's proof
- Problem: JUnit covers parsing, but "first statement of init()" and `configDir()` answering during mod construction on each loader are unproven (`repo.md` §12.4 [not checked]); a config file left in a developer's run folder changes every test expectation that uses literals.
- Proposal: pin `config/ConfigFile.load(Path dir) → Result(ExoframesConfig config, List<String> problems, boolean wroteDefaults)` with no logging inside (JUnit asserts problems: missing file → written pretty with defaults; not JSON; null section; unknown enum key → NPE caught; extra keys ignored; file never overwritten when broken). GameTest `configFileExists`: `Files.isRegularFile(Compat.platform().configDir().resolve("redstone_exoframes.json"))` on four variants. Every GameTest derives expected numbers from `RedstoneExoframes.config()`, never from literals.

### F21 — minor — 9.1-9.4: data needs positive tests and the lang needs a check
- Problem: a wrong-version folder is ignored silently on every variant; a misspelled id in `minecraft:mineable/pickaxe` drops the whole tag for every block (`dati.md` §4, §6); nothing checks the two lang files (`dati.md` §7).
- Proposal: GameTests: each of the seven recipe ids `server.getRecipeManager().byKey(id).isPresent()` (compiles on both without naming the type); each unlock advancement present; `CHARGER.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)` and the control `Blocks.STONE` too; survival mock with an iron pickaxe breaks the charger through `gameMode.destroyBlock` → the charger item drops; without a pickaxe nothing drops but the stored cell still does. `tools/check_data.sh` (freshness + orphan rule, `dati.md` §8) as a CI step; `check_jars.sh` names the five folder kinds per version. JUnit `LangFilesTest`: same key set and order in `en_us.json` and `it_it.json`, and every key constant of a `Messages`/`Keys` class present.

### F22 — minor — 12 "real client in the cloud": the scripts are not in the repository
- Problem: the movement proofs need `autoJump:false` and two-key holds (`ctrl+w`) in `tools/client/client_shot.sh`; those lines exist only in the experiment patch (`ricognizione-movimento-e-dati.md` §5), and its scripts carry the clone path inside. HUD and bars are visual only (no numeric assertion).
- Proposal: a task ports the two `client_shot.sh` changes and writes `tools/client/m3c_shots.sh` with numeric thresholds from the experiment (pool depth 3: Space 3 s → Y of the bottom; ≥ 8 blocks in 2 s on the bottom; current drift 0; step +1; sprint with `debug energy 999` ≤ 9 blocks in 2 s, with 5 000 ≥ 10.5); HUD/bars/charger screenshots committed and judged by eye. Budget: 101-136 s per variant per script (experiment §4).

### F23 — minor — 7.1, 5.5: bar and tooltip tests
- Problem: `getBarWidth(ItemStack)` needs a registry-backed `ItemStack` (no JUnit, `repo.md` §9.4); on a remote client the bar must use the server's capacity from `client_rules`, which no GameTest reaches.
- Proposal: pure `CellBar.width(int stored, int capacity)` and `color(...)` (JUnit: 0 → 0, full → 13, round); GameTest on `stack.getBarWidth()`/`isBarVisible()` for a cell and a damaged piece; the neutral `tooltip(stack, lines, advanced)` of `CompatItem` called directly in a GameTest; the client holder (`synced value if connected to a server, else local config, cleared at disconnect`) as pure Java with JUnit.

### F24 — minor — 3.6: what happens at entry is not decided
- Problem: "on the transition into LOW and EMPTY: message and sound" — entering with a LOW cell (allowed: "una cella con carica") is no transition from a previous `FrameEffects` (the record starts empty), and the swap from LOW to NORMAL has no message. Tests cannot assert an undecided behaviour.
- Proposal: 3.6: "the first evaluation after the record is created applies attributes and syncs, and shows the LOW/EMPTY message if the state is not NORMAL; NORMAL ← LOW/EMPTY shows no message".

### F25 — minor — 1.2: exception policy inside `hurt`
- Problem: a `RuntimeException` in the wear handler propagates into `LivingEntity.hurt` (crash or lost hit); `danno.md` open points leaves it undecided; `FrameEntry.onDeath` catches (`FE:201`).
- Proposal: 1.2: "catch `RuntimeException`, log one ERROR per player per minute naming the frame, never cancel the hit"; add its allow-list line only if a test provokes it (none planned).

### F26 — minor — 7.2: cooldown, off hand and open screens unpinned
- Problem: "(client: no screen open, like exit.)" ends with a question mark; the off hand is neither in nor out of the scan; the cooldown clock is unnamed and its record dies at exit.
- Proposal: 7.2: client counts only with no screen open; scan = hotbar 0-8 then 9-35, off hand excluded; cooldown = `now − lastSwap < cellSwapSeconds × 20` with `now` from F6, stored in the `SuitTick` record (lost at exit: accepted). GameTest: two calls in one tick → second refused with a message; after `runAfterDelay(41)` accepted (20 + 41 < 100).

### F27 — minor — 11.7: exact log counts need the variant
- Problem: rule `:45` of `log_allowlist.txt` is Fabric-only; `check_log.py` receives a scope and paths, not the variant (`repo.md` §10).
- Proposal: 11.7 pins `check_log.py gametest --variant <v> <log>` and a field-2 syntax `fabric=1,forge=0,neoforge=0`; `run_gametests.sh` counts `add(tests,` over `gametest/*.java` (one total).

### F28 — minor — 12 and design §5.9: name what only the PC can prove
- Problem: §12 lists a PC checklist without its content; several promises have no cloud proof.
- Proposal: the checklist `docs/test-in-gioco/m3c-giocarci.md` carries: sounds heard (F18); activity drain on a remote server with real latency (F4); double-tap W and toggle-sprint off under 10 %; a second player sees the suit walking (not swimming) under water; production jars apply the four mixins; tooltip hover text; FOV narrowing from the speed modifier (×0.6275 at EMPTY, `attributi.md` §8.4); hopper chains visually; the "feel" of drain rates and charge time (200 s per cell).

## Proof map (decision → how it is proven)

J = JUnit (pure, no bootstrap), G = GameTest (four variants), C = real headless client in the cloud, S = dedicated server start, PC = only a human on a PC.

| Decision | Proof | Cannot be proven in the cloud |
|---|---|---|
| 1.1 wear at `hurtArmor` | G: known cactus hit (exact numbers), i-frame pair (F17), `generic()` no wear, lava ×10 same tick → one wear; S: mixin applies (crash otherwise); `check_jars.sh` markers | production jar → PC |
| 1.2 handler | G (F14 break test), J `routeMilli` | — |
| 1.3 `routeMilli` | J: delegation equality for 0..1200, fractional sums exact, clamp of huge amounts | — |
| 1.4 HEAD for `damages_helmet` | G classification only (F16) | — |
| 1.5 break message + sound | G message; sound via recorder (F18) | hearing → PC |
| 1.6 no reduction | nothing to test | — |
| 2.1-2.2 fall | G with control (F12) | — |
| 2.3 sounds kept | none | PC |
| 3.1-3.4 energy | J `ActivityWindow`, G (F11) | drain under real latency → PC |
| 3.5 sync | J `SyncThrottle`, G `syncCount` (F5) | receipt → C (HUD) |
| 3.6 effects/messages | G (F11, F24) | sound → PC |
| 3.7 sprint off server | G (`isSprinting()` false after the tick) | client side → C |
| 3.8 carry lost | J of `EnergyMeter` (exists) | — |
| 4.1 idempotent apply | G same-instance modifier, respawn keep=true (F8) | — |
| 4.2 step | G 1.21.1 modifier (F7); C on four | — |
| 5.1 water | G with `doTick()` (F15); C numbers | second client's view → PC |
| 5.3 sprint mixin | C with `debug energy` (F3, F22) | double-tap/toggle → PC |
| 5.4 indicators | J `OwnerIndicators`, J codec round trip | — |
| 5.5 `client_rules` | J holder + codec; C with a non-default server config | — |
| 6.x config | J `ConfigFile`; G file exists; S file created | — |
| 7.1 bars/tooltip | J pure width; G stack methods; C screenshot | hover → PC |
| 7.2 swap | J `CellSwapChoice`; G with/without cell, no frame, cooldown, conservation (F1, F2, F26); C hold indicator (`@tieni r 2`) | — |
| 8.x charger | J `ChargerRule`; G (F13); C render of `lit` | — |
| 9.x data | G (F21); `check_data.sh`; `check_jars.sh` | — |
| 10.1 sounds | G recorder | PC |
| 10.2 HUD | C screenshot | — |
| 11.1-11.7 | G per carry-over (existing pattern), scripts | — |

## Time budget

- [log] 14 Nov, four variants in sequence, warm daemon: 1.20.1-forge 06:25:10→06:25:44 (34 s, of which world creation 17.7 s and tests 3 s), 1.20.1-fabric 11 s (tests 1-2 s), 1.21.1-fabric 10 s (tests 3 s, two batches of 50 + 45), 1.21.1-neoforge 12 s (3 s, two batches). Whole step about 80 s + Gradle configuration; CI limit 20 min.
- Cost of ~40 more tests (the list implied by §12 and F-findings): on 1.21.1, 135 tests = 3 batches of ≤ 50 run one after the other; a batch lasts as long as its slowest test (≤ 100 ticks = 5 s). Upper bound ≈ +10-15 s per variant, under 1 min in total. Time is not the constraint; the 100-tick cap per test is (F13, F26).
- Real client: 101-136 s per variant per script (experiment §4); an m3c script ≈ 3-4 min per variant, ≈ 15 min for four; run by the implementer, not in CI.

## Work items in dependency order (deliverable — proof)

3c-1 "Consumare"
1. Harness: `survivalMock`, `endSpawnProtection`, probe test, comment `GT:136`; split the test class (shared helpers class, new classes per area), `run_gametests.sh` counts all files, `check_log.py --variant` + exact counts + `TESTREG` with spaces (11.7) — probe G on four; scripts' own dry run.
2. Worn-state notifier (11.1) incl. debug log line, neutral respawn hook seam (F8) — existing 3b G stay green + G "debug wear logs".
3. `FrameStateSavedData` split + "valid file without sections" + chunk predicate once (11.2) — existing J (23+10+2+13) unchanged, new J for the section rule.
4. `/exoframe remove` and line of sight to `frame/` (11.3) — existing G + G "eyes inside a frame's box pick that frame".
5. Step-out revisions + exit refused over another frame (11.4) — one G per hazard, small step, diagonal, two frames.
6. `CLOSE_MENU` stray placeholder (11.5), narrowed quit save honouring `/save-off` (11.6) — G each.
7. Config file (6.x) — J `ConfigFile`, G file exists, `ConfigValidation` + `ExoframesConfigTest` for new keys.
8. `FrameSounds` + 3b sounds (10.1 [D]) — G recorder.
9. `routeMilli` (1.3) — J.
10. Wear mixin + `SuitDamage` + classifier HEAD (1.1, 1.2, 1.4, 1.5) + `check_jars.sh` markers + `docs/compat.md` + design §3/§7 — G (F14, F16, F17).
11. Fall (2.x) — G (F12).
12. Idempotent `apply`, `frame_step`, 1.20.1 client step (4.x) — G same instance + respawn; C step.
13. `ActivityWindow`, `SyncThrottle`, `SuitTick` (3.x) + `/exoframe debug energy` (F3) — J + G (F6, F11).
14. Owner packet indicators + client cache + `client_rules` + client holder (5.4, 5.5) — J codec/indicators/holder.
15. Movement mixins water + sprint (5.1, 5.3) + `check_jars.sh` — G (F15); C.
16. `CompatItem` tooltip seam, cell and piece bars (7.1) — J + G.
17. Cell swap key, `cell_swap` packet, `updateWorn`, `CellSwapChoice` (7.2) — J + G (F1, F2, F26); C hold.
18. HUD line + client seam (10.2) — C screenshot.
19. `client_shot.sh` port + `m3c_shots.sh` + checklist 3c-1 + docs — C on four; S.

3c-2 "Ricaricare"
20. Spike: `EntityBlockBase`/`BlockEntityBase` seams + empty charger block, item, BE type registered (Fabric order) — G place + save/load round trip on four; S.
21. `ChargerRule` + config `chargerRePerSecond` — J.
22. Charger block entity, interaction, blockstate `facing/cell/lit`, comparator, assets — G (F13).
23. `WorldlyContainer` hoppers + break drops — G (F13).
24. `tools/gen_data.py` + `tools/check_data.sh` + CI step + `check_jars.sh` folders — the script's own checks; G loads.
25. Recipes, unlock advancements, loot table, `mineable/pickaxe` (9.x) — G (F21).
26. Lang EN/IT for everything + `LangFilesTest` (9.4) — J.
27. Checklist `m3c-giocarci.md`, README, `docs/compat.md`, design §11 updates; C charger screenshots — S on four.

## Risk ranking (five most likely to cost a day) and the cheapest early proof

1. **Three new mixin targets** (`Player.hurtArmor`, the three movement targets, `LocalPlayer.hasEnoughFoodToStartSprinting`) with Forge SRG remapping and `check_jars.sh` (e2) markers; a wrong target crashes start-up (`required`, `defaultRequire 1`). `hurtArmor` has never been compiled (`danno.md` open points). Spike at task 10's start: empty handlers for all targets in their final classes, build four jars, `check_jars.sh`, `unzip -p` + `javap -c -p` on the mixin class to read the annotation strings (`m_6472_(`, `method_6105(`, `hurtArmor(`), start the dedicated server on four.
2. **Block and block-entity seams** (1.21.1 `validateBlockState` in the BE constructor, Fabric's immediate supplier call order, `BlockEntitySupplier` access widener, `public`/`protected` hook visibility). Spike = task 20 alone, before any charger logic: register, place in a GameTest, save/load round trip, four variants.
3. **Per-version data folders** fail silently (wrong spelling ignored; one bad id drops a whole tag). Proof first: in task 24 generate one recipe and the pickaxe tag, then the G of F21 with the `Blocks.STONE` control, on four, before writing the other recipes.
4. **Client-only code on four variants**: HUD callback signatures (`float` vs `DeltaTracker`), key registration before `RegisterKeyMappingsEvent` (late = WARN with the key name, caught by the log check), 1.20.1 `LocalPlayer` replaced at every respawn/dimension change, client holder reset. Proof: one early real-client run with a stub HUD line and the second key registered, on four (`client_shot.sh`, ~2 min each), at the start of task 14/18 instead of at the end.
5. **Activity thresholds under real packet timing** (F4): the numbers cannot be chosen from bytecode. Proof: a 5-minute client run logging per-tick distances while walking, sprinting, standing (localhost), before task 13; PC follow-up on a remote server.

(Close sixth: the harness probe of F10 — cheapest of all, first task.)

## What the plan must pin (still open in the document)

- Names and signatures: `SuitTick.tick(ServerPlayer, long now)` and its record fields; `SuitDamage.onArmorHurt(Player, DamageSource, float)`; `FrameStateAccess.updateWorn(ServerPlayer, Consumer<FrameState>) → boolean`; `core/energy/ActivityWindow`, `ChargerRule`, `CellSwapChoice` signatures; `OwnerIndicators`; `SyncThrottle`; `config/ConfigFile.load(Path) → Result`; `FrameSounds.play`; `Compat.events().onRespawn` (or equivalent); test seams `GameTests.respawn`, `frameModifier`, `syncCount`, `lastSounds`, `saveBlockEntity`, `loadBlockEntity`, `stepIsAttribute`.
- Mixin class names and which class holds `hurtArmor` (own class or `PlayerMovementMixin`), their `check_jars.sh` markers per loader (Fabric `method_6105`, `method_29920`, `method_5675`, `method_5790`, `method_46743`; Forge `m_6472_`, `m_6129_`, `m_6063_`, `m_5844_`, `m_255269_`; NeoForge Mojang names).
- Numbers: `walkThreshold`, `activityWindowTicks`, teleport distance 8; sync interval 5 ticks; cooldown in ticks; `chargerRePerSecond` 50; charger hardness/resistance (furnace 3.5), light level of `lit`; comparator formula (e.g. vanilla `floor(14 · stored / capacity) + (stored > 0 ? 1 : 0)`); bar colour (redstone red, RGB value); HUD position, format and per-state colours.
- Packet ids and field order: `frame_state_sync` + `canSprint`, `canSwim`, `energyState` (written only when the state is present); `cell_swap` (empty record); `client_rules` (`cellCapacity`, `cellSwapSeconds`, `exitHoldSeconds`, all `int`).
- Keys and lang: `key.redstone_exoframes.cell_swap` default R; every new `message.redstone_exoframes.*` key (swap none/done/cooldown, energy low/empty, piece broken with the piece name, charger status), `block.redstone_exoframes.charger`.
- NBT keys of the charger (`cell`, `energy`, `dust`, `buffer`, `carry_milli`) in `NbtKeys`; block properties `facing` (`HORIZONTAL_FACING`), `cell` (`BooleanProperty.create("cell")`), `lit`.
- Sound table: event → `SoundEvents` constant (from the 36 plain ones) and `SoundSource`.
- Answers or defaults recorded for 3.4, 4.3, 5.2 and the split question of F9 before tests are written.
