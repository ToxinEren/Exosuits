# Verification of dossier `repo.md` (claims c1-c14)

Read-only, 15 Nov. No Gradle, no game, no repo script run. Paths: `J/` = `/home/user/exosuits/src/main/java/com/toxineren/exoframes/`, `SD` = `J/compat/impl/FrameStateSavedData.java`, `FSA` = `J/compat/impl/FrameStateAccessImpl.java`, `FE` = `J/frame/FrameEntry.java`, `GT` = `J/gametest/ExoframeGameTests.java`.
Jar labels: V120 / V121 = vanilla merged 1.20.1 / 1.21.1; F120 = `forge-1.20.1-47.4.29-minecraft-merged` (Mojang names); N121 = `neoforge-21.1.266-minecraft-merged-mojang`. Every javap below was run by me on each named jar (helper `tmp-repo/jp.sh`). [arch] = Architectury sources jars 9.2.14 (forge, fabric) and 13.0.11 (neoforge).

Summary: 13 confirmed (some with precision notes), 1 refuted in part (c9), 0 unverifiable.

## c1 update vs mutate — CONFIRMED
- [code] `FSA:66-78`: `update` gets `SD.get`, if `readOnly()` -> `logRefusedChange()` and `return` *before* the `try` (no sync); else `try { data.updateWorn(uuid, () -> new FrameState(RedstoneExoframes.config()), change) } finally { syncTo(p) }`.
- [code] `SD:317-327` `getOrCreateWorn` creates `new FrameRecord(UUID.randomUUID(), factory.get())` + `markStructural()`; `SD:335-347` `updateWorn` applies change, `markStructural()` in `finally`.
- [code] `FSA:81-89` `mutate` -> `SD:355-367` `mutateWorn`: `readOnly() ? null : worn.get(player)`; null -> `false`; else change + `setDirty()` in `finally`. No creation, no sync.
- [code] `SD:818-821` `markStructural(){ structuralUnsaved = true; setDirty(); }`.
- Read-only: `logRefusedChange()` (`SD:791-804`) logs ERROR **once per data object** (`refusalLogged`), not once per call.
- Readers of the flag: game logic only `saveWithPlayersIfNeeded` (`SD:841-848`) registered at `FSA:297` on `PlayerEvent.PLAYER_QUIT`. Precision: the test hook `GameTestsImpl.registryChangeUnsaved` (`GameTestsImpl.java:213`) and JUnit (`FrameStateSavedDataTest:739-749`, `RegistryFileTest:117-133`) also read `structuralUnsaved()`.

## c2 SlotGuard redraw — CONFIRMED
- [code] `J/frame/SlotGuard.java:54-58` registers `TickEvent.PLAYER_POST` filtered to `ServerPlayer`; `tick` `:76-123`: `Compat.frameState().get(player)` `:78`, `SuitSummary.of(state)` `:88`, chest slot compared with cached/parsed summary `:98-100`, replaced via `Placeholders.create(slot, summary)` `:109`, `refreshDimensions()` if changed `:120-122`.
- `summaryFollowsTheState` `GT:2512-2536` (mutate a helmet to broken, then calls `SlotGuard.tick(player)` directly, asserts BROKEN on the torso). Note it calls the method directly, not through the event.
- grep over `src/`: `Placeholders.create(` only at `FE:230` and `SlotGuard.java:109`.

## c3 per-tick registry lookups — CONFIRMED
- [code] callers: `SlotGuard.java:78` (each ServerPlayer, each PLAYER_POST); `SafeSpots.java:64-65` (`tickCount % 20`, alive, `onGround()`, `!isPassenger()`, then `wornFrameId`); `FrameRealign.java:41, 50, 63-69, 78` (`SERVER_LEVEL_POST`, per-level counter, `INTERVAL_TICKS = 100`, `placedIn(level)`). Also once per entity on its first tick (`ExoframeEntity.java:110`), not per tick.
- [code] `SD:181-183` `get(server)` = `server.overworld().getDataStorage()` + `dataFolder` (`DATA_FOLDERS.computeIfAbsent`, `:190-193`) + `get(storage, folder)` `:204-226`; on >=1.21 `new SavedData.Factory<>(fresh, (tag, provider) -> loader.apply(tag), null)` (`:212-213`, one Factory and one capturing lambda per call); on 1.20.1 `storage.computeIfAbsent(loader, fresh, NAME)`.
- [javap] `DimensionDataStorage.computeIfAbsent` -> `get` -> `cache.get(name)` (Map), `containsKey`/`readSavedData`/`put` only on a miss: V120 (`computeIfAbsent(Function,Supplier,String)` off 3; `get` off 5), F120 (same plus `DummySavedData.DUMMY` identity test off 15-18), V121 and N121 (`computeIfAbsent(SavedData$Factory,String)` off 3; `get` off 5).

## c4 config and unused seams — CONFIRMED
- grep `src/main` + `src/test`: no call of `setConfig` (only its declaration `RedstoneExoframes.java:65` and a Javadoc link `:56`), none of `platform()`, `attributes()`, `damage()` (also not as method references); `ConfigValidation.validate` only in `ExoframesConfigTest.java` (8 calls: :76, 94, 111, 123, 140, 157, 175, 184).
- `RedstoneExoframes.config()` outside `gametest/`: 12 sites (FramePermissions:26, DebugWear:77, ExoframeItem:49, CellItem:26, 43, PieceItem:57, 63, ExoframeCommand:340, FrameStateClientCache:29, FSA:74, SD:619, client/ExitKey:54), all in method bodies/lambdas.
- `J/core/frame/FrameState.java:34, 50-53`: `cfg` stored, used only by `new ModuleInstaller(cfg.pieces)` (no other occurrence of `cfg` in the file).

## c5 step-out — CONFIRMED (with a pose condition)
- [code] `FE:63-68` constants; `FE:318-339` `stepOut`: returns if `spot.level() != player.serverLevel()`, `player.isPassenger()`, or `!SuitSize.standingBox(frame).contains(player.getEyePosition())`; directions front/left/right/behind at `STEP`, candidate y = `frame.y` (`:331`).
- `SuitSize.java:24, 30, 42, 101-105`: height = `1.8f * 1.65f` = 2.97 (float). [javap V120, V121] `AABB.contains(DDD)`: `>= min` and `< max` on each axis. So with the standing eye height 1.62 the bound is feet < frame.y + 1.35. **Condition**: `getEyePosition()` follows the current pose (crouching ~1.27 -> ~1.70; swimming / fall-flying 0.4), and `client/ExitKey.java:53` does not require standing: the 1.35 figure is the standing case only. Also the box test is horizontal too: a frame put at a safe spot or spawn elsewhere never triggers a step.
- `standingSpot` `FE:368-393`: `!level.noCollision(player, box)` at candidate y; `depth = 1.0 + 1.0e-3`; reject if `drop <= -depth`; `insideBorder(level, feet)`; hazard over `BlockPos.betweenClosedStream(standing.expandTowards(0,-0.5,0))` with `getFluidState().is(FluidTags.LAVA) || is(BlockTags.FIRE)` ([javap V120, V121] floor of min and max on each axis, closed range); `getEntitiesOfClass(ExoframeEntity.class, standing)`. `wayIsFree` `FE:354-360`: one hull AABB -> `level.noCollision(player, way)`.

## c6 no room / other-frame test at the resting spot — CONFIRMED
- [code] `restingSpot` `FE:407-432` uses only `groundBelow` / `insideBorder` / `SafeSpots.last` / spawn; `leaveFrame` `FE:283-306` and `FSA:185-197` / `SD:572-581` `exit` do no room or frame check. Contrast `FramePlacement.java:43-47` (`insideBorder`, `noCollision(box)`, `getEntitiesOfClass(ExoframeEntity.class, box)`).
- `GT:2104-2128`: ground y 2, player moved to y 5 (relative), asserts `player.position().equals(air)` at `:2125`.

## c7 collision of boats vs minecarts — CONFIRMED on all four (V120, V121, F120, N121)
- [javap] `CollisionGetter.noCollision(Entity, AABB)` identical in the four: block shapes (off 3-43), `getEntityCollisions` non-empty -> false (off 50-64), border (off 66-101).
- [javap] `EntityGetter.getEntityCollisions` identical in the four: size < 1e-7 -> empty; predicate `CAN_BE_COLLIDED_WITH` (null entity) or `NO_SPECTATORS.and(entity::canCollideWith)`; box inflated 1e-7.
- [javap] `Entity.canCollideWith` = `canBeCollidedWith() && !isPassengerOfSameVehicle(other)`; `Entity.canBeCollidedWith()` = `iconst_0`; `Boat.canBeCollidedWith()` = `iconst_1` in all four; `AbstractMinecart` (Entity subclass on 1.20.1, `VehicleEntity` subclass on 1.21.1) and `VehicleEntity` declare no `canBeCollidedWith` in all four; the loader interfaces they gain (`IForgeAbstractMinecart`, `IAbstractMinecartExtension`) cannot override a class method. `Level`, `ServerLevel`, `LevelReader` declare neither `noCollision` nor `getEntityCollisions` in any of the four. This settles the dossier's open point about loader patches.

## c8 sync and order on the worn-state sites — CONFIRMED
- [code] `FSA:169-182` `enter`: `syncTo(player)` at `:180`; `FE:134-145` order enter -> `stowWornArmor` (`frames.stow` -> `FSA:206-222`, no sync) -> `dress` -> `stopRiding` -> `teleportTo` -> `discard` -> `SafeSpots.remember` -> `refreshDimensions` -> `FrameLog`.
- `FE:176-178` exit: `Placeholders.removeAll`, `returnStowed`, `leaveFrame`; `FE:198-199` death: `removeAll`, `leaveFrame`. `takeStowed` -> `SD:487-489` `takeStowedOfWorn` reads `worn.get(player)`.
- grep `FrameLog`: callers `FE:144, 299`, `FrameInteractions:247`, `ExoframeItem:64`, `ExoframeCommand:300, 358`; none in `DebugWear`. `FSA:295` JOIN `this::syncTo`; `:299` / `:301` RESPAWN lambdas only `syncTo`.

## c9 FrameStateSavedData layout, allow-list, JUnit entry — REFUTED IN PART
- Confirmed: file 1022 lines; `:300` opens `frames()` Javadoc, `:595` `load(CompoundTag)`, `:605` `load(CompoundTag, BiFunction<CompoundTag, ExoframesConfig, FrameState>)`, `:776` end of `readSpot`, `:916` Javadoc of `write`, `:1021` end of `writeRecord`. `SD:261` (previous copy, `PREVIOUS_COPY_COST` `:135-137`), `SD:272` (both unreadable), `SD:549` (orphan WARN) are the three SD lines matched in full by `tools/log_allowlist.txt` lines 38, 40, 31/33 (my own fullmatch count over the four logs agrees, see c14). `load(new CompoundTag(), FrameStateCodec::read)` is the empty writable registry at `FrameStateSavedDataTest.java:125` and `RegistryFileTest.java:50`.
- **Wrong**: at `StackTagsTest.java:47` and `:52` it is not used as a registry: the result is discarded; the call exists only to run the private constructor's `StackTags.forgetOutcomes()` (`SD:176-178`) and the test asserts `StackTags.rememberedOutcomes() == 0` after it (`:46-53`). A "valid file without sections -> not a registry" rule does not break these two lines as long as `load` still constructs an instance; if the reset moves out of the constructor, they break.
- Also: JUnit has other package-private entries besides `load`: `afterUnreadFile` (`RegistryFileTest:152, 187, 216, 240`), `fileRoot`, `saveTo`, `markSaved`, `structuralUnsaved`, `write`.

## c10 frameInSight, removeFrame — CONFIRMED on all four
- [code] `ExoframeCommand.java:104` `SIGHT = 5.0`; `:317-330` clip with `ClipContext.Block.COLLIDER`, `Fluid.NONE`, then `ProjectileUtil.getEntityHitResult(player, eyes, end, around, filter, eyes.distanceToSqr(end))`.
- [javap] V120, V121, F120, N121: containing branch `AABB.contains(start)` (off 82) taken when `best >= 0` (off 90-92) and sets best to 0 (off 110); for a hit by the ray the test `d < best || best == 0.0` (off 146-154) lets a later entity replace it. F120 and N121 add `Entity.canRiderInteract()` (off 171) only inside the same-root-vehicle branch, irrelevant to frames. A containing entity iterated later also replaces an earlier ray hit (`best >= 0` always true), so the result depends on iteration order both ways.
- [code] `:252-311` `removeFrame` mirrors `FrameInteractions.unmount` (`:216-252`) / `unmountPiece` (`:259-275`) with `Handouts.dropStill` instead of `Handouts.give`; order differs (pieces, cell, stowed vs stowed first).

## c11 packets — CONFIRMED
- grep `registerS2C(` / `registerC2S(` outside `Net`/`NetImpl`: only `FSA:293` (`frame_state_sync`) and `FE:211` (`frame_exit`). `new FrameStateSyncPacket` only at `FSA:111` (plus the codec's `read`, `FrameStateSyncPacket.java:33`). `FrameStateClientCache` used only by `FSA:104, 294, 307`; `clientView` called only by `FSA:62` (`get(Player)` on the client side), which no client code calls.
- `NetImpl.java:57, 79` both call `open`; `:120-126` throws `IllegalStateException("Packet <id> is already registered")`.
- `tools/check_sources.sh:76-83` rule 5 as stated (the excluded comment lines are those starting with optional blanks then `//`, `*` or `/*`; `registerC2S(` is matched anywhere, comments included).
- grep `src/test` and `gametest/`: no reference to `FrameStateSyncPacket`, `FrameExitPacket`, `PacketCodec`, `FriendlyByteBuf`, `NetImpl`, `Compat.net()`. (In-world tests do execute `syncTo`/`sendToPlayer` indirectly, but nothing asserts on it.)

## c12 check_sources.sh — CONFIRMED
- `:20-28` 1b (import lines, `com\.google\.gson[.;]`, `$JAVA/core` only); `:30-39` 1c (`grep -rlw "$type" src/test/java`, any file type); `:43-52` + `:87-92` directive regex `(//|/\*)[?$~]` over `src/main/java` and `src/test/java`, allowed only under `$JAVA/(compat/impl|platform)/` or exactly `$JAVA/compat/Ids.java`; `:94-123` rules 7 and 8 textual, allowed under `(client|compat/client|compat/impl/client|mixin/client)/` in main and test plus `CLIENT_ENTRYPOINTS` = `platform/RedstoneExoframesFabricClient.java`, `RedstoneExoframesForge.java`, `RedstoneExoframesNeoForge.java`.

## c13 game-test count and split — CONFIRMED (precision)
- `tools/run_gametests.sh:20-21` single file, `grep -cE '^[[:space:]]*add\(tests,'` -> 95 (my count; the Javadoc mention at `GT:122` does not match).
- [javap] `GameTestRegistry.register(Class)`: V120/F120 `getDeclaredMethods` -> forEach `register(Method)`; V121/N121 the same after sorting the methods by name. `register(Method)` adds to the static `TEST_FUNCTIONS` and also adds `getDeclaringClass().getSimpleName()` to the static `TEST_CLASS_NAMES` (so "no per-class state" is literally inexact, harmless: a set of names). F120/N121 `register(Method)` -> `register(Method, Set.of())` with the namespace filter. [javap F120, N121] `RegisterGameTestsEvent.register(Class)` adds every declared method to `gameTestMethods` (a `Set<Method>`). `GameTestsImpl.register` (`GameTestsImpl.java:70-89`) adds one listener per call on Forge / NeoForge, a direct registration on Fabric: several classes work.
- JUnit: the only Minecraft imports in `src/test` are `net.minecraft.nbt.*` (8 distinct classes; also `net.minecraft.nbt.NbtIo` qualified at `RegistryFileTest:132`); no `Bootstrap` / `SharedConstants` in `src/test`.

## c14 logs and check_log.py — CONFIRMED
- Four `versions/<v>/build/gametest/server.log` dated 14 Nov 06:25-06:26, each `All 95 required tests passed`.
- Own fullmatch counter (`tmp-repo/scripts/count.py`): allow-list lines 26, 29, 31, 33, 36, 38, 40 = 1, 2, 2, 1, 2, 1, 1 on all four (WARN, WARN, WARN, WARN, ERROR, ERROR, ERROR); line 45 (also scope `gametest`, max 1) = 1 at ERROR on both Fabric logs (`1.20.1-fabric:75`, `1.21.1-fabric:73`, logger `(Minecraft)`), 0 on Forge and NeoForge.
- `tools/check_log.py:72` `most.isdigit()` and `int(most) >= 1`; `:131-133` only `count > most`. `TESTREG` (`log_allowlist.txt:21`) starts with `\S*`.

## Missed (what a planner could be misled by)
1. Tick order, all four variants: PLAYER_PRE/POST fire inside `Player.tick` ([arch] Fabric `MixinPlayer` HEAD/RETURN; Forge `PlayerTickEvent` START/END and NeoForge `PlayerTickEvent.Pre/Post`, both at priority HIGH; [javap] F120 `ForgeEventFactory.onPlayerPreTick/onPlayerPostTick`, N121 `EventHooks.firePlayerTickPre/Post` in `Player.tick`). For a real `ServerPlayer`, `Player.tick` runs only from `ServerPlayer.doTick()` ([javap] all four), which `ServerGamePacketListenerImpl.tick()` calls, which `MinecraftServer.tickChildren` reaches after every `ServerLevel.tick` and after the level-post hooks ([javap] V120 off 125 vs 197, V121 142 vs 214, F120 133/172 vs 251, N121 150/189 vs 268). So a `mutate` made during the level tick (mob hits, block effects, FrameRealign) is redrawn by SlotGuard in the same server tick; listeners of one Architectury event run in registration order (`EventFactory` ArrayList loop).
2. c5's "1.35 blocks" holds only in standing pose: the eye test uses the current pose's eye height and the exit key works while crouching, swimming or gliding.
3. c9: `StackTagsTest:47, 52` rely on the constructor reset of `StackTags`, not on an empty registry (see c9).
4. Loader patches on the collision and pick path: none relevant. `noCollision`, `getEntityCollisions`, `canCollideWith`, `canBeCollidedWith` of Boat / AbstractMinecart / VehicleEntity identical in F120 and N121; `ProjectileUtil` differs only by `canRiderInteract()` in the same-vehicle branch. The dossier's first "open point" can be closed.
5. `check_log.py` counts a rule only for records it already selected (`:119-121`: WARN/ERROR/FATAL whose text contains `exoframe` case-insensitively, or any level matching `com\.toxineren|...`). An allow-list line for a message without "exoframe" or `com.toxineren` never counts; with an "exact count" mode it would always fail. Rule order matters too: a message is counted against the first matching rule only (`:124-128`).
6. On 1.21.1 vanilla and NeoForge `GameTestRegistry.register(Class)` sorts methods by name before registering; on 1.20.1 it does not. Harmless for counting, but generator invocation order differs between versions if test classes are split.
7. `SafeSpots.tick` and `SlotGuard.tick` are reached only through PLAYER_POST, i.e. only for players whose connection ticks (`doTick`); fake players of other mods and the test mock players get neither the guard nor safe spots unless something calls `doTick()` or the methods directly. Any 3c per-tick logic hung on PLAYER_POST inherits this.
