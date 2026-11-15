# Part 3c decisions — examination through the architecture lens

Examiner: architecture (the code that would have to be written, in this repository, under its rules).
Document examined: `m3c/direzione-decisioni.md` (draft 1). Repo `/home/user/exosuits`, branch `m3c-giocarci` (`d88e1e0`).
Paths: `J/` = `src/main/java/com/toxineren/exoframes/`; `T/` = `src/test/java/com/toxineren/exoframes/`.
No `…-verifica.md` file existed when this was written; the dossiers were taken as they are.

## Findings, most severe first

### A1 — blocker — decision 7.2: the cell swap "through `update`" can create a frame from nothing, and the inventory/registry order is unpinned
- **Problem.** `FrameStateAccess.update` *creates the worn record if absent* (`J/compat/FrameStateAccess.java:63-68`; `FrameStateAccessImpl.java:66-78` → `new FrameState(config())`). A `cell_swap` packet that arrives after an exit, a death or a debug remove (two keys held, latency, a modified client) would create an empty worn record, move the inventory cell into it, and `SlotGuard.tick` would then dress the player (`SlotGuard.java:76-123`): a frame duplicated out of nothing. Second hole: on a read-only registry `update` returns before running the consumer and without a sync (`FrameStateAccessImpl.java:68-71`, repo.md §2.1), so code that edits the inventory outside the consumer loses or duplicates a cell.
- **Evidence.** Code lines above; `update` returns `void`, so the caller cannot know whether the change happened.
- **Proposal.** 7.2 should say: the handler (in a `frame/` class that references `Compat.frameState()`, rule 5 of `check_sources.sh`) refuses unless `wornFrameId(player)` is present, the player is alive and not a spectator, and `player.containerMenu == player.inventoryMenu`. Add to `FrameStateAccess` a structural write that never creates and reports the outcome, e.g. `boolean updateWorn(ServerPlayer, Consumer<FrameState>)` (structural + sync, false when absent or read-only). Order: choose the slot → `updateWorn(s -> { old = s.removeCell(); s.insertCell(newCell); })` → only if true, set the inventory slot to `old == null ? EMPTY : CellItem.fromCell(old)`. The cooldown counts server ticks (`server.getTickCount()`), not `player.tickCount` (tests write it, test.md §0).

### A2 — major — decision 4.1 / 11.1: the notifier cannot register `PLAYER_RESPAWN` from `frame/`
- **Problem.** The Architectury callback differs: 9.2.14 `(ServerPlayer, boolean)`, 13.0.11 `(ServerPlayer, boolean, RemovalReason)` (attributi.md §5; `FrameStateAccessImpl.java:298-302` already needs a Stonecutter directive for it). Directives are allowed only in `compat/impl/`, `platform/`, `compat/Ids.java` (`tools/check_sources.sh` rules 2 and 6). A `frame/WornState` notifier that registers its own respawn listener fails the source check, and `compat/` imports nothing from `frame/` (repo.md §4, §12.1).
- **Proposal.** Pin a seam: `FrameStateAccess` (or a new tiny `compat/PlayerEvents`) gets `void onJoinOrRespawn(Consumer<ServerPlayer>)`, implemented in `FrameStateAccessImpl.registerCommon` next to the existing two listeners and called *after* `syncTo`. The notifier in `frame/` registers through it. State in 4.1 that the 1.21.1 third argument is ignored (it is `null` on NeoForge, test.md §5).

### A3 — major — decision 5.1: the swim rule asks the registry on the server and the packet/summary on the client; use the placeholder on both sides
- **Problem.** (a) `isAffectedByFluids`, `isPushedByFluid`, `updateSwimming` run on both sides 5-6 times per player per tick (ricognizione §1(a)); on the server each `Compat.frameState().get(player)` is a `FrameStateSavedData.get` (`computeIfAbsent`, plus a `Factory` and a lambda allocated per call on 1.21.1, repo.md §2.3). (b) Server and client then answer from different sources that change at different moments (registry first; placeholders and packet later; `enter` syncs before dressing, repo.md §4 constraint 5), so the server's `updateSwimming` can set the synced swim flag and SWIMMING pose (seen by everyone, and it changes the suit's box to 0.8 × 0.99) while the client says the opposite. (c) "can it swim" has no answer to read: `FrameRules.evaluate` sets `canSwim` to the constant `false` (`J/core/frame/FrameRules.java:101`), and `SuitSummary` carries no swim flag. Other players never receive the owner packet (`FrameStateClientCache` is filled only by the owner sync, `FrameStateAccessImpl.java:108-112`).
- **Proposal.** 5.1: `frame/SuitMovement` decides with `SuitSize.wearsSuit(player)` on both sides (the torso placeholder, already the rule for size and synced by vanilla), exactly as the reconnaissance ran it (ricognizione §1(a) "Logica"). The `canSwim` indicator of 5.4 stays in the packet because design §5.3 binds it, but nothing reads it in M3; say so.

### A4 — major — decisions 1.1, 5.1, 5.3: the check and documentation edits are listed for one mixin out of three, and `check_jars.sh` has a substring trap
- **Problem.** 1.1 lists design §3/§7, `docs/compat.md` and `check_jars.sh` for `hurtArmor`. 5.1 (`PlayerMovementMixin`, common) and 5.3 (`LocalPlayerSprintMixin`, client) say nothing. Design §3 lists the mixins exhaustively and ends "Tutto il resto passa da eventi Architectury o da hook per loader in `platform/`": adding two more without amending §3 and §7 (point 14/17 list) overrides the design silently. `check_jars.sh` fails on each: `common != [size_mixin]` (:168), `CLIENT` exact list (:143), and on NeoForge every listed class must contain a `MOJANG` marker (:152-153, :203-204), while on Fabric/Forge *no* class may contain one (:205-206). That test is a substring search over the whole class file: if a handler or a called method is named after the vanilla target (`exoframes$hurtArmor(`, `SuitMovement.isAffectedByFluids(`), the Fabric and Forge jars fail with "nomi Mojang … non rimappati".
- **Proposal.** One decision "Mixins of 3c": `mixin/SuitDamageMixin` (or one `PlayerMixin` holding `hurtArmor`, `isAffectedByFluids`, `isPushedByFluid`, `updateSwimming`: one class, one check entry, `@Mixin(Player.class)`, no directive) and `mixin/client/LocalPlayerSprintMixin`; both json files (`resources-mc1.20.1/` and `-mc1.21.1/`); in `check_jars.sh` the `mixins` check becomes `sorted(common) == sorted([size_mixin, "PlayerMixin"])`, `CLIENT` gains the sprint mixin, `MOJANG` gains `b"hurtArmor("`, `b"isAffectedByFluids("`, `b"hasEnoughFoodToStartSprinting("`, `TARGETS` pins `method_6105`/`m_6472_`, `method_29920`/`m_6129_`, `method_46743`/`m_255269_` (danno.md §3, test.md §4); rule: handler and `frame/` method names never contain a target name. Design §3, §7 and `docs/compat.md` "Mixin" amended for all of them in the same task.

### A5 — major — decision 2.2 contradicts design §5.7 and the document's own test list
- **Problem.** Design §5.7: "se il danno aggira l'armatura si salta". `fall` and `stalagmite` are in `bypasses_armor` (danno.md §4). 2.2 wears the legs with the cancelled fall amount, a silent override (1.1 is the only declared one). §12 then lists both "damage that bypasses armor does not wear" and "fall … with leg wear". 2.2 also brings back what 1.1 rejected LIVING_HURT for: on Fabric and Forge the event fires before the creative check and for dead players, and on every loader before the i-frame gate (danno.md §1 table). A fall landing inside the 10-tick window of another hit, which vanilla would reduce or drop, would still be counted in full.
- **Proposal.** Either drop 2.2 (the design's reading; `DamageKind.FALL` stays unused in M3), or write it as a declared override of §5.7 with its guards: `instanceof ServerPlayer`, `isAlive()`, `!abilities.invulnerable`, `amount > 0`, and the i-frame arithmetic copied (`invulnerableTime > 10` → only `amount - lastHurtByPlayerAmount` if positive). Also amend §12's line to "damage that bypasses armor does not wear, except a cancelled fall".

### A6 — major — decision 4.2 (1.20.1): resetting to 0.6F every tick fights every other mod, and the code has no legal home as written
- **Problem.** "`0.6F` otherwise, checked every client tick" writes 0.6 on every 1.20.1 local player that does *not* wear a suit, every tick: any other mod's step value (step assist, mounts) is overwritten forever. The reconnaissance said to write only at transitions (ricognizione §1(b): "Meglio solo al cambio di stato … per non pestare altre mod"), and attributi.md §8.6 to remember that the mod raised it. Location: `Entity.setMaxUpStep` exists only on 1.20.1 (attributi.md §2), so the call needs a directive: it cannot be in `client/` or `frame/` (rules 2/6), and `client/` may not reach `compat.impl` (rule 4).
- **Proposal.** 4.2: a method `FrameAttributes.localStep(Player, boolean wearing)` (1.21.1 branch empty) in `FrameAttributesImpl`, called from `SuitSize.tick` (already on `PLAYER_PRE`, both sides) for `player.isLocalPlayer()`. It sets 1.0F when wearing and the value is below 1.0, and goes back to 0.6F only if the mod raised it (a weak per-player flag; a new `LocalPlayer` has no entry, so it counts as a transition).

### A7 — major — decision 11.6 breaks the "same save" rule the registry relies on
- **Problem.** (a) The structural flag is global (`FrameStateSavedData.java:818-849`): player A swaps a cell (structural), player B quits. If the save is narrowed to "the player files" meaning the quitting player, the registry is written with B's file and A's file stays old: a crash then leaves A's inventory before the swap and the registry after it, so a cell is duplicated or lost. (b) Under `/save-off`, vanilla still writes the quitting player's file: `PlayerList.remove@14` calls `save(player)` with no save-off test (javap 1.21.1). The class comment relies on that and writes the registry with it (`FrameStateSavedData.java:56-59`). Honouring `/save-off` at quit therefore splits a player file from the registry on exactly the path that guarded it.
- **Proposal.** 11.6: keep, beside the flag, the set of players with an unsaved structural change. At quit, save those players' files (`PlayerList.save` is protected, so through `saveAll` filtered, or each `ServerPlayer` via `server.getPlayerList()`, in `compat/impl`) plus the registry file only (`saveFile`), not `overworld().getDataStorage().save()`. Do **not** honour `/save-off` for that write (state why in the class comment). Do it after 11.2, which moves this code.

### A8 — major — decision 3.3: one-tick classification against jittery packets underestimates drain, and the threshold is not config material
- **Problem.** Between 0 and 2 client move packets land in one server tick (test.md §3: "Per-tick values are jittery … classify over a short window"). With "moved in this tick > 0.01", a walking player has IDLE ticks (2 RE/s instead of 8). Over the same ticks the two-packet ticks count WALK only once, so the error does not cancel. A cross-dimension move under 8 blocks (Nether coordinates near 0) is counted as walking. `energy.walkThreshold` as a config value (6.3) adds a field, a `ConfigValidation` rule and test lines for an internal heuristic. Design §6 calls it a builder's value, not a server option.
- **Proposal.** 3.3: sum the horizontal distance over the last 5 server ticks (ring buffer in the `SuitTick` record); moved = sum > 5 × 0.01; reset the buffer on a level change, on a step > 8 blocks, while a passenger, and on a new player object. Keep the threshold a constant in a pure class (see A16), drop `walkThreshold` from 6.3.

### A9 — major — decisions 8.2/8.4: "no item stack, primitives only" plus hoppers on a 64-dust slot destroys dust
- **Problem.** A vanilla hopper that inserts into a non-empty slot merges with an in-place `grow` on the stack returned by `getItem(slot)`, then shrinks its own stack (blocco.md §4, trap (a)). If `getItem(1)` returns a stack built from the persisted dust count, the hopper's dust is consumed and the growth lands on a throw-away copy. Each hopper transfer into a non-empty slot 1 then deletes one redstone dust: a loss, against the invariant.
- **Proposal.** 8.2/8.4: the block entity keeps slot 1 as a live `ItemStack` field in memory (persisted as a count, so still no stack in NBT), `getItem(1)` returns that field, `setItem(1, s)` accepts only redstone dust, `getMaxStackSize()` 64 with slot 0 limited by `canPlaceItem` to an empty slot and one cell. Slot 0 may return a fresh view (one item, extraction by `removeItem`). Add a GameTest: hopper feeds 3 dust into a charger already holding 1 → count 4.

### A10 — major — decision 11.2 mixes a behaviour-preserving split with a behaviour change
- **Problem.** "Split registry operations vs file format" can be a pure move pinned by `T/compat/impl/FrameStateSavedDataTest` (23 tests), `RegistryFileTest` (10), `StackTagsTest`, `FrameStateCodecTest`, and the GameTests `registryFileIsReadBack`, `quitSavesTheRegistryWithThePlayers`, `registryFolderIsRememberedPerServer` (repo.md §5.3). "A file that parses but has no `data`/sections is treated as damaged" changes behaviour, and it breaks the tests' own "empty registry" helper `load(new CompoundTag(), …)` (`FrameStateSavedDataTest.java:125`, `RegistryFileTest.java:50`, `StackTagsTest.java:47,52`; repo.md §5.4). In one task, a red test cannot say which half caused it.
- **Proposal.** 11.2a: cut along repo.md §5.4 (plain holder, format class, `FrameStateSavedData` with one-line delegates under today's names): no test file changes. 11.2b: the "no `schema` and no `frames` → damaged, read-only" rule, with a new explicit empty-registry helper and the four call sites changed. 11.2c the "entities loaded" predicate. 11.6 after 11.2a.

### A11 — major — decision 11.1 bundles new behaviour into the refactor and lists a site that is not a worn-state change
- **Problem.** 11.1 makes the notifier also do "attributes apply/clear and the per-player tick record", i.e. features of §3/§4, so it is not behaviour-preserving. The existing enter/exit/death/debug tests (`registryPlaceEnterExitMovesOneRecord`, `stowedArmorSurvivesEnterAndExit`, `debugWear*`, `summaryFollowsTheState`, `rejoinRestoresPlaceholders`) pin the refactor only if nothing else changes. "Slot repair" (`SlotGuard.tick`) never changes the registry (repo.md §4 table); calling the notifier there would run `apply` and the log line on every repair. The order constraints of repo.md §4 (placeholders removed before `takeStowed`/`exit`; `teleportTo` before `refreshDimensions`; `refreshDimensions` before `stepOut`) must survive.
- **Proposal.** 11.1 = one `frame/WornState.changed(ServerPlayer, String event)` called *after* the registry write at enter (after teleport), `leaveFrame` (after `frames.exit`), `DebugWear.wear/remove`, and join/respawn (via A2); it does `refreshDimensions()` and the log line (the debug log line is the one behaviour addition, named). Placeholder removal stays at the sites. Attributes and the tick record are added to that method in the feature tasks (§3/§4). SlotGuard keeps its own `refreshDimensions`.

### A12 — minor — decision 5.4: the client read path for the indicators is unspecified
- **Problem.** `FrameStateClientCache` is package-private in `compat/impl` and stores only `FrameState` (`FrameStateClientCache.java:17-44`). `client/` and `mixin/client` may not name `compat.impl` (rule 4). A type in `core/` falls under rule 1c. 5.3, 7.2 and 10.2 all need it, so different engineers would each invent one.
- **Proposal.** Pin: `compat/FrameIndicators` record `(boolean canSprint, boolean canSwim, EnergyState energy)`, `FrameStateAccess.clientIndicators(Player) → Optional<FrameIndicators>`. The cache stores them with the state, written only when the state is present. `syncTo` computes them with a shared `new FrameRules(RedstoneExoframes.config()).evaluate(state)`. Update the packet Javadoc and `docs/compat.md:68,78`.

### A13 — minor — decision 5.5: "synced on a client connected to a server, local otherwise" cannot be decided where it is read
- **Problem.** `CellItem.getBarWidth(ItemStack)` has no level and runs in common code, so it cannot tell "client connected to a server". The handler must store into a common holder (rules 7/8; repo.md §7(c)).
- **Proposal.** Holder rule: "the value received since the last `CLIENT_PLAYER_QUIT`, else the local config". Single player receives the same values from its integrated server. Server-side code (cooldown, `toCell`) never reads the holder.

### A14 — minor — decision 1.3: unit of `unabsorbed` in the milli overload not pinned
- `DamageRouterTest` asserts `unabsorbed` in points (`T/core/armor/DamageRouterTest.java:88` expects 50; :184-185 expect 3). Pin: `routeMilli` returns `Split` with `unabsorbed = amountMilli / 1000` (points), `route` delegates unchanged. The caller clamps `round(amount * 1000.0)` into `[0, Integer.MAX_VALUE]` before narrowing (danno.md §5).

### A15 — minor — decision 1.2: exceptions inside `hurtArmor`
- A `RuntimeException` from classify/route/mutate would propagate into `LivingEntity.hurt` on every hit. Pin: `SuitDamage.onArmorHurt` catches `RuntimeException` and logs once per player per session (pattern of `FrameEntry.onDeath`, `FE:192-203`). Side effects (1.5 message and sound) run after `mutate` returns, outside the consumer.

### A16 — minor — decision 12 (JUnit): "activity classification (pure)" and "cell-swap choice (pure)" have no pure home
- `ItemStack`, `Inventory`, `Player` cannot load in JUnit (no bootstrap; repo.md §9.4). 3.2 puts activity in `frame/SuitTick`, which needs `Player`. Pin: `core/energy/ActivityRule` (distances, sprint flag, passenger, level-changed → `Activity`) and `core/energy/CellSwapRule` (list of `(slot, energy)` + installed energy → slot or none), both named in a test (rule 1c). `SuitTick` and the swap handler only adapt Minecraft objects.

### A17 — minor — decision 12: "two hits inside the invulnerability window count once" is not what vanilla does
- Inside the window a larger second hit passes as `amount − lastHurt` (danno.md §1(c)). Pin the assertion: an equal or smaller second hit adds no wear, a larger one adds the difference. Reset with the public `invulnerableTime = 0` to leave the window (test.md §6.3).

### A18 — minor — decision 3.2: key and lifetime of the per-player record
- Pin: keyed by UUID. Removed by the notifier when the worn state ends and at `PLAYER_QUIT`. On join/respawn the notifier applies and stores `lastApplied`, so the tick does not re-apply. "Previous position" and "previous jump statistic" are reset when the `ServerPlayer` object changes (death or End return), and a decrease of the statistic is a new baseline (test.md §2). A public static `SuitTick.tick(ServerPlayer)` is the entry tests call (mocks never fire `PLAYER_PRE`, test.md §1.2).

### A19 — minor — decisions 3.5/3.6: two sync triggers that half-overlap, and wear never reaches the owner at EMPTY
- 3.6 adds "immediate sync when effects change" on top of 3.5 "immediately at energy-state change". 3.5 throttles on "energy changed", so wear done through `mutate` with an empty cell (energy constant) never reaches the owner's cached durability until a structural change. Pin one rule: a dirty bit set by every `mutate` of the tick and of `SuitDamage`; sync now on a structural change or when `FrameEffects` differs from the last synced value, else at most every 5 ticks when dirty.

### A20 — minor — decision 10.2 overrides design §2 silently and invents a seam that exists
- Design §2 "Non obiettivi: … HUD … (M4–M5)". 10.2 adds a HUD line without declaring the override (only 1.1 is declared). Seam: `compat/client/ClientRenderHooks` already exists with `compat/impl/client/ClientRenderHooksImpl` (directives allowed there). Add `onHud(Consumer<GuiGraphics>)` there instead of a new seam, and add the line to design §7 as a builder's choice.

### A21 — minor — decision 7.2 deviates from design §5.6 text without saying so
- Design §5.6: "con la prima cella carica dell'inventario". 7.2 skips cells with less energy than the installed one. That reads better, but it is an override of `[D]` text: declare it. Also pin the scan: offhand included or not (today unspecified), indices 0-8 then 9-35 of `Inventory.items`. The `?` about menus: the server can test `containerMenu != inventoryMenu`, so the refusal belongs on both sides (see A1).

### A22 — minor — decision 8.3: the per-hand interaction and the seam it needs
- Per-hand means the off hand is offered too. With an empty main hand and a cell in the off hand, the main-hand call withdraws instead of inserting. `FrameInteractions` uses the main hand only. Pin: main hand only, off-hand call → PASS. Name the seams: `compat/impl/EntityBlockBase` + `compat/CompatEntityBlock` (click), `compat/impl/BlockEntityBase` + `compat/CompatBlockEntity` (load/save: `load(CompoundTag)` vs `loadAdditional(…, Provider)`), as design §5.8 and blocco.md §2. Do not extend `BaseEntityBlock` (abstract `codec()` on 1.21.1). Registration order `ModBlocks` → `ModItems` → `ModBlockEntities` (Fabric runs suppliers immediately, blocco.md §3). Update `docs/compat.md:205-206`.

### A23 — minor — decision 11.7: per-loader exact counts need an input `check_log.py` does not have
- `check_log.py` receives a scope and log paths, not the variant (`run_gametests.sh:43`; repo.md §10). Pin: `run_gametests.sh` and `tools/client/client_shot.sh` pass the variant, and allowlist field 2 accepts `=N` or `fabric=N,forge=…`. Splitting the test class needs one `Compat.gameTests().register(X.class)` per class in `init()` and the helpers moved to an annotation-free shared class (repo.md §9.3).

### A24 — minor — decision 9.3: a blanket "own folders only" rule trips on the existing structure pair
- Both `data/redstone_exoframes/structure/` and `structures/` ship in every jar today (stato-del-codice §5). Pin the five folder kinds (recipe(s), advancement(s), loot_table(s), tags/block(s), tags/item(s)) or `gen_data.py --list <version>` as the source of truth (dati.md §11.8).

### A25 — minor — decision 1.2/1.4: helmet-source wear differs by version
- On 1.21.1 `hurtHelmet` and `amount *= 0.75` run before the i-frame gate, so `hurtArmor` sees 75 % for anvils, falling blocks and stalactites; on 1.20.1 100 % (danno.md §7). With 1.4 routing these to HEAD, the helmet wears 25 % less on 1.21.1. Accept and write it in the design, or divide by 0.75 for `DAMAGES_HELMET` on 1.21.1 (a directive in `DamageClassifierImpl`).

### A26 — minor — missing: field of view under the speed modifier
- `frame_speed` is `MULTIPLY_TOTAL speed−1`: the client narrows the FOV by `(m+1)/2`, i.e. ×0.925 always and ×0.6275 at EMPTY (attributi.md §8.4). Nobody decided it. Changing it needs a client hook (Forge/NeoForge FOV event, Fabric a mixin). Decide: accepted for M3 (and put it on the PC checklist), or one more client seam.

### A27 — minor — decision 6.2: validation problems and placement
- Unspecified: what happens to a readable file whose values `ConfigValidation` corrects. Pin: one WARN per problem (the mod's logger name matches "exoframe", so `check_log.py` would flag it in a test run; the test config must be clean), file untouched, normalized values used. Package: a top-level `config/` next to `core/config/` is legal but confusing; name it `configfile/ConfigFile` (no rule involved, a naming pin).

## Contradictions with the binding design that the document does not declare

| Design text (section) | Decision | Status |
|---|---|---|
| "se il danno aggira l'armatura si salta" (§5.7) | 2.2 wears legs on fall (a bypassing type) | silent override (A5) |
| "Tutto il resto passa da eventi Architectury o da hook per loader in `platform/`", exhaustive mixin list (§3, §7 p.14/17) | 5.1 `PlayerMovementMixin`, 5.3 `LocalPlayerSprintMixin` | silent (A4); 1.1 declared only for itself |
| "Non obiettivi: … HUD" (§2) | 10.2 HUD line | silent (A20) |
| "con la prima cella carica dell'inventario" (§5.6) | 7.2 "MORE energy than the installed one" | silent (A21) |
| "idempotenza degli attributi dove testabile" in JUnit (§5.9) | 12 moves it to GameTest | consistent ("dove testabile": not testable in JUnit on 1.21.1, attributi.md §1.6) |
| "In `LIVING_HURT` … `DamageRouter.route`" (§5.7) | 1.1 `hurtArmor` mixin | declared |
| "caricatore … (`compat/CompatBlockEntity`)" (§5.8) | 8.x names no seam | gap, not contradiction (A22) |
| "passo … un po' meno [economico] su Fabric 1.20.1" (§8.2d) | 4.2 same cost | fact correction, fine |

## Contradictions with the code as it is

| Decision | Assumption | Code |
|---|---|---|
| 7.2 | `update` is the safe structural path | `update` creates a record (`FrameStateAccess.java:63-68`), silent no-op when read-only (A1) |
| 4.1 / 11.1 | a `frame/` notifier listens to respawn | callback arity differs, needs a directive (`FrameStateAccessImpl.java:298-302`) (A2) |
| 5.1 | client knows "can swim" for remote players from the summary | `SuitSummary` has no such field; only the owner gets the packet (A3) |
| 5.4 | "the client cache exposes them" | cache is package-private in `compat/impl`, no accessor on `FrameStateAccess` (A12) |
| 4.2 | client code can call `setMaxUpStep` | method exists only on 1.20.1 → directive → `compat/impl` (A6) |
| 8.2/8.4 | primitives + hopper merge | hopper grows the returned stack in place (A9) |
| 11.6 | `/save-off` can be honoured at quit | vanilla saves the quitting player regardless (`PlayerList.remove@14`) (A7) |
| 1.3 | delegation keeps tests | true, if `unabsorbed` stays in points (A14) |

## Placement table (where each decision's code lives, seam, rule touched)

| Dec. | Code | Seam used / needed | Rules touched |
|---|---|---|---|
| 1.1-1.5 | `mixin/PlayerMixin` → `frame/SuitDamage`; `DamageClassifierImpl` (+`DAMAGES_HELMET`) | `Compat.damage()`, `FrameStateAccess.mutate` | both mixin json, `check_jars.sh` e2 (A4), design §3/§7, `docs/compat.md` |
| 1.3 | `core/armor/DamageRouter.routeMilli` | — | 1c (test exists) |
| 2.1 | `frame/SuitDamage` on `EntityEvent.LIVING_HURT` | none (same signature both versions) | — |
| 3.x | `frame/SuitTick` on `TickEvent.PLAYER_PRE` + `core/energy/ActivityRule` | `mutate`, `syncTo`, `Compat.attributes()` | 1c for the pure class |
| 4.1 | `FrameAttributesImpl.apply` idempotent (+ `frame_step` on ≥1.21) | `FrameAttributes` | directives stay in `compat/impl` |
| 4.2 (1.20.1) | `FrameAttributes.localStep` from `SuitSize.tick` | new method (A6) | rules 2/6/4 |
| 5.1 | `mixin/PlayerMixin` → `frame/SuitMovement` (`SuitSize.wearsSuit`) | none | e2 |
| 5.3 | `mixin/client/LocalPlayerSprintMixin` → `frame/SuitMovement.sprintAllowed` | `FrameStateAccess.clientIndicators` (A12) | e2 `CLIENT`, rule 7 (mixin/client → frame is allowed) |
| 5.4 | `FrameStateSyncPacket`, `FrameStateClientCache`, `FrameStateAccessImpl.syncTo` | `compat/FrameIndicators` | rule 4 |
| 5.5 | `frame/ClientRules` holder + S2C registered in common init | `Net.registerS2C` | rules 7/8 (handler common) |
| 6.x | `configfile/ConfigFile` (Gson), first line of `init()` | `Compat.platform().configDir()` | 1b (Gson outside core) |
| 7.1 | `compat/impl/ItemBase` + `compat/CompatItem`; bars directly in `CellItem`/`PieceItem` | tooltip seam | rule 4, 6 |
| 7.2 | `client/CellSwapKey` (+`ExitHold` instance); `frame/CellSwap` handler | `FrameStateAccess.updateWorn` (A1) | rule 5 |
| 8.x | `content/ChargerBlock`, `ChargerBlockEntity`, `ModBlocks`, `ModBlockEntities`; `core/energy/ChargerRule` | `CompatEntityBlock`, `CompatBlockEntity` | 1c, 4, 6 |
| 9.x | `tools/gen_data.py`, `resources-mc*/data/` | — | `check_jars.sh` new rule (A24), CI step |
| 10.2 | `client/EnergyHud` | `ClientRenderHooks.onHud` (A20) | rules 7/8 |
| 11.1 | `frame/WornState` | join/respawn hook (A2) | rule 6 |
| 11.2 | `compat/impl/` three classes | — | none new |

## Carry-overs (§11 of the decisions): order and pinning

| Item | Behaviour-preserving? | Pinned by | Order note |
|---|---|---|---|
| 11.1 notifier | yes only without attributes/tick (A11); + debug log line | enter/exit/death/debug/rejoin GameTests | first; needs A2 seam |
| 11.2 split | split yes; "file without sections" no (A10) | `FrameStateSavedDataTest`, `RegistryFileTest`, `StackTagsTest`, `FrameStateCodecTest`, 3 registry GameTests | before 11.6 |
| 11.3 command → `frame/` | move yes; OUTLINE + "eyes inside wins" no | `wallHidesTheFrameFromTheCommand`, `lockAndUnlockTheWornFrame`, `removeDropsTheContents`, `ownerUnlocksAndStrangerCanUse`, `strangerCannotUnlock`, `debugNeedsOperator` | move, then change, two commits |
| 11.4 step-out | no (new behaviour) | existing step-out GameTests + new ones | independent; "exit refused over a frame" is new policy |
| 11.5 CLOSE_MENU | new behaviour, small | new GameTest | independent |
| 11.6 save at quit | no; invariant risk (A7) | `quitSavesTheRegistryWithThePlayers` | after 11.2a |
| 11.7 tooling | n/a | — | before the first 3c GameTest class is added |
No feature decision makes a carry-over moot. 7.2's structural swap makes 11.6 (A7) more important, because swaps add structural changes by players who do not quit.
