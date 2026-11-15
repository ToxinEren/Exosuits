# Challenge of `esame/prove.md` (lens: testability, scope and order)

Read-only re-derivation, 15 Nov. `J/` = `/home/user/exosuits/src/main/java/com/toxineren/exoframes/`, `GT` = `J/gametest/ExoframeGameTests.java`, dossiers in `m3c/verifica/`.

## F1 — HOLDS (blocker)
- `FrameStateAccessImpl.update` (J/compat/impl/FrameStateAccessImpl.java:65-78) calls `data.updateWorn(uuid, () -> new FrameState(config), change)`; `FrameStateSavedData.getOrCreateWorn` (:317-327) puts `new FrameRecord(UUID.randomUUID(), factory.get())` and marks structural when absent. `SlotGuard.tick` (J/frame/SlotGuard.java:76-121) dresses any player with a worn record. The exit handler (FrameEntry.java:210-213) guards only via `exit`. A swap packet with no worn frame creates a frame. Confirmed.
- Fix: correct. Smallest form: the swap handler is a `public static` method (callable from GameTests; mocks cannot send C2S) whose first check is `frames.wornFrameId(p).isPresent() && p.isAlive() && !p.isSpectator()`; on the server thread check-then-act is atomic. The new `updateWorn(…) → boolean` (wrapping `FrameStateSavedData.updateWorn` without the factory, like `mutateWorn` at :355-366 but structural + sync) is worth adding because it also fixes F2.

## F2 — HOLDS, severity major (not blocker)
- `update` returns `void` and on `readOnly()` only logs (:68-71): a caller that cleared the slot first loses the cell. 7.2 fixes no order. But the loss needs a damaged (read-only) registry and a wrong implementation order: major.
- Fix: correct. Equivalent and simpler to read: "write the inventory slot only after `updateWorn` returned true" or inside its consumer; either is fine on the single server thread. `CellSwapChoice.pick` in core and the conservation GameTest (count of cells and sum of RE before = after) are the right proof.

## F3 — HOLDS (major)
- Creative tab: `ModCreativeTabs.java:33` appends `CellItem::charged` (full); `DebugWear.rebuild` (:82) and `ExoframeCommand.java:349` install `CellItem.charged()`. Entry refuses an empty cell (FrameEntry.java:116-119), so EMPTY is unreachable by entering. (10 000 − 999)/20 = 450 s of sprint to LOW: confirmed.
- Alternative without code: the real-client script writes `config/redstone_exoframes.json` with a small `cellCapacity` (6.x) — e.g. 400: LOW below 40 after (400−39)/20 = 18 s of sprint, real drain end to end. But it cannot pick exact HUD values and needs a per-run config. The proposed `/exoframe debug energy [<RE>]` is the better fix; keep it operator-only, clamp to [0, capacity], go through `updateWorn` (immediate sync). Note 3.4: `client_shot.sh:377` puts the player in creative, where 3.4 stops drain — the script must switch to survival for any drain proof (m3b_shots.sh:82 already does).

## F4 — HOLDS on the problem, PARTLY on the remedy
- `test.md` §3 states the 0/1/2-packets-per-tick jitter and recommends a window; 3.3 ignores it. A per-tick rule under alternating 0/2 packets drains (0.1+0.4)/2 = 0.25 RE/tick instead of 0.4 when walking (−37.5 %).
- Remedy: a window is right, but the "spike on localhost to fix the numbers" cannot fix them (the examiner itself says localhost jitter is ~0); drop it. Simplest rule: "moving if the horizontal distance in any of the last N = 4 sampled ticks exceeds `walkThreshold`" (a hold, not a sum), reset to IDLE on dimension change, passenger, teleport. Pure in `core/energy/ActivityWindow` with JUnit on synthetic sequences; real-latency drain to the PC checklist. Note it changes F11's expected numbers at the start/stop of movement (first sample IDLE, 3 ticks of tail).

## F5 — HOLDS, proposal partly oversized
- Mock packets go into an unread `EmbeddedChannel` (GameTestsImpl.java:102-103); NeoForge 1.21.1 `sendToPlayer` returns before sending when the channel is not negotiated (NetImpl.java:94-101). No test touches `FrameStateSyncPacket` (grep of src/test and gametest: none).
- Keep (c) (a test-visible counter of `syncTo` calls via `GameTests`) and (d) (codec round trip; JUnit already loads `net.minecraft` classes, e.g. `FrameStateCodecTest`, so `FriendlyByteBuf(Unpooled.buffer())` is plausible). Drop (a): `FrameEffects` already carries `canSprint`, `canSwim`, `energyState` (FrameEffects.java:12-23) and `FrameRules.evaluate` is JUnit-covered; the packet copies three fields. (b) can live inside `SuitTick` and be proven by (c).

## F6 — HOLDS
- `PLAYER_PRE` fires only inside `Player.tick()` reached from `doTick()` (test-verifica C2); `doTick()` adds physics/food. A loop of N calls inside one server tick sees one `getTickCount()`. GT:2494 shows the `TickEvent.PLAYER_POST.invoker().tick(player)` pattern.
- Fix correct. Add: any test that also calls `doTick()` (water tests, F15) fires the listener with `server.getTickCount()` into the same record, so tests must use `now = server.getTickCount() + k` (monotonic), or the tick treats `now < last` as a reset (see missed M2).

## F7 — HOLDS
- `run_gametests.sh:20-21` counts `add(tests,` in one file; :39-41 require passed == expected on every variant; `check_sources.sh` rule 6 forbids directives in `gametest/`. `ServerPlayer.<init>` sets 1.0F on 1.20.1 (test.md §1.1, attributi.md §2): the 1.20.1 assertion passes without mod code.
- Fix correct and minimal (one compat helper; `Attributes.STEP_HEIGHT` does not exist on 1.20.1, so the test cannot name it directly).

## F8 — PARTLY
- The arity problem is already solved in the repo: `FrameStateAccessImpl.registerCommon` registers both arities of `PLAYER_RESPAWN` (:298-302) and calls `syncTo`. The gap is only how that listener reaches the frame/ "worn state changed" point; and death clears the record (FrameEntry.onDeath :192-203), so only the End return (keepEverything; 1.21.1 only the first time, test.md §5) matters. Modifiers are not copied to the new object (attributi-verifica: `restoreFrom` copies none on 1.20.1, base values only on 1.21.1).
- Cheaper correct fix: make the tick self-healing — `SuitTick`'s record is bound to the `ServerPlayer` instance (identity-keyed map, or the record holds the player and is rebuilt when `record.player != player`), and a new record has "last applied effects = none", so the first tick applies attributes. That covers join and respawn with no new event and no seam; GameTest: a fresh mock with a worn record and no modifier gets `frame_armor` once after one `SuitTick.tick`. If an explicit hook is still wanted, extend the existing listener at :298-302 with a neutral callback list, not a new `Compat.events()` family. The `GameTests.respawn` seam is optional (a respawned mock is a plain `ServerPlayer` born at the world spawn, test.md:158).
- Danger the examiner missed here: if the record is keyed by UUID (as 3.2's "removed at PLAYER_QUIT" suggests), after respawn the stale "last applied FrameEffects" equals the new evaluation and the tick never re-applies (M1).

## F9 — PARTLY
- Size is real, but the 27 includes tasks the examiner's own findings add (debug energy, `FrameSounds`, `ActivityWindow`/`SyncThrottle`, client script port, `check_data.sh`). Design §4.6 (line 76) makes the division "scelta del costruttore": a further split is the builder's decision, not necessarily an owner [Q]. Correct statement: [P] "3c is delivered in two merges (3c-1 consume, 3c-2 recharge+recipes); the binding done-when applies at the end of 3c-2"; tell the owner, no blocking question. The proposed boundary is sound (recipes need the charger, crafted cells are empty).

## F10 — PARTLY
- "Never run" is wrong: test-verifica C1 has runtime corroboration — mocks earn "Suit Up" 12/12/9/12 times in the 14 Nov logs, only reachable via `ServerPlayer.tick()` → `broadcastChanges`; spawn-protection decrement is in the same `tick()` (C3). Risk is lower than stated.
- Keep the cheap part: `survivalMock`/`endSpawnProtection` helpers and the probe (`hurt(cactus,1)`, `runAfterDelay(5)` → `invulnerableTime == 15`) as the first step of the damage task, and fix the comment at GT:136 (it says the server does not advance the mock's tick counter; it does). Not a separate risk item.

## F11 — HOLDS (the isCreative part partly)
- Rates: `EnergyMeter.tick` milli = round(perSecond·1000/20): 100/400/1000. 20 IDLE = 2000 → −2; 20 WALK → −8; SPRINT → −20; jump 15 (FrameRules.java:194). 1000/10 000 = 0.10 is NORMAL (strict, EnergyState.java:9); one sprint tick 1000 milli → 999 → LOW. Confirmed.
- First-sample effect re-derived: if the first call has no previous position it is IDLE: 100 + 19·400 = 7700 milli → −7 (carry 700), not −8. Tests must prime with one call.
- 3.4 already names `abilities.instabuild` (correct: `setGameMode` updates abilities, test.md §1.5), so the `isCreative()` trap is an implementation risk, not a decision defect; the "no `isCreative()` in 3c code" rule is a cheap guard (MockPlayer overrides it to true, GameTestsImpl.java:134-137).

## F12 — HOLDS, one item untestable
- Creative `mayfly` early return; `isDedicatedServer()` false on `GameTestServer` so falls are spawn-protected (test-verifica C3); `causeFallDamage` returns true after `hurt` regardless. Amount: ceil((10−3)·1) = 7 on both versions. Routing re-derived with the code's largest-remainder logic (FALL, primaryShare 0.6, shares 0.15/0.30/0.125/0.125/0.15/0.15): legs 2 each carry 100; torso 1 carry 200; helmet 0 carry 600; arms 0 carry 500. Frameless survival control loses 7.0 (fall bypasses armor, no scaling).
- Correction: the "client side" guard cannot be asserted in a GameTest (no client level); make it the first statement of the handler and leave it to review.

## F13 — HOLDS
- 10 000/50 = 200 s = 4000 ticks; hopper cooldown 8 (blocco.md §7); `assertContainerContains` only for `BaseContainerBlockEntity` (blocco-verifica); `onRemove` on every state change (blocco-verifica C7). Direct-tick numbers re-derived: 2500 milli/tick → 2,3,2,3 = 10 RE in 4 calls, buffer 500 − 10 = 490.
- Precision for the dup-path test: a hopper pulls only when `!inventoryFull()`, so "into a full hopper" never reaches the put-back; the setup must be a hopper with five slots each holding one non-mergeable item (not full, no room). Rollback differs by version (blocco-verifica point 2: 1.21.1 `setCount` + `setItem` only for a count-1 original): a one-item cell slot is safe on both, which is exactly what the test should pin.

## F14 — HOLDS
- Re-derived GENERIC 8000 milli: helmet 1 (c200), torso 2 (c400), arms 1 (c0), legs 1 (c200). After the helmet breaks: protection 0.85·15 = 12.75, `armorPoints()` = round → 13 (FrameEffects.java:34-36, FrameAttributesImpl.java:41). Needs a survival mock (creative `abilities.invulnerable` refuses the hit). Fix correct.

## F15 — HOLDS
- `ServerPlayer.tick()` (what the level runs on a mock) has no `baseTick`; `wasTouchingWater` is updated only in `baseTick` via `doTick()` (test.md §1.3 table). The recon list (ricognizione-movimento-e-dati.md:123) has "after a tick `isSwimming()` false" (true without mixin if "tick" = `tick()`) and "in lava true" (vanilla). Fix correct; note that `doTick()` fires `PLAYER_PRE`/`POST` on the mock (energy tick and SlotGuard run too) — see M2.

## Missed (same lens)
- **M1 (major, 3.2/4.1):** `SuitTick`'s per-player record is keyed so that it survives a respawn (removed only at QUIT and when the worn state ends); its "last applied FrameEffects" then equals the new evaluation on the new `ServerPlayer`, so the tick never re-applies attributes and the respawn hook becomes the only defence. Pin: the record is bound to the player instance; a new instance starts with "nothing applied". GameTest: a fresh mock with an existing worn record gets the modifiers at its first `SuitTick.tick`.
- **M2 (major, 3.2/12):** two clocks in one record — direct `SuitTick.tick(p, t0+i)` and the listener's `server.getTickCount()` when a test calls `doTick()`. Pin: tests derive `now` from `server.getTickCount()` and only move forward; `SuitTick` treats `now < lastNow` as a reset of throttle/cooldown/activity.
- **M3 (major, done-when):** §12 tests the loop only in pieces with creative shortcuts (`CellItem.charged()`, `update`). Add one "partita" GameTest on a survival mock with real items: assemble the cell recipe (empty cell), charger direct tick with 1 dust (2 RE after one call: enough to enter), mount pieces and cell on a placed frame through the 3b interaction methods, `FrameEntry` enter, N `SuitTick` calls drain, swap to the inventory cell, recharge the old one. All direct calls, well within 100 ticks.
- **M4 (minor, 7.2/8.3):** the swap handler and the charger interaction are named only as packet/`use` behaviour; mocks cannot send C2S packets. Pin `public static` entry points (`CellSwap.handle(ServerPlayer, long now)`, the neutral per-hand charger method) as the GameTest targets.
- **M5 (minor, 3.4/12):** the real-client base script starts in creative (`client_shot.sh:377`), where 3.4 stops all drain; HUD-decrease and drain proofs silently show a constant value. The 3c script must switch to survival (as `m3b_shots.sh:82`) and assert the HUD number changed.
