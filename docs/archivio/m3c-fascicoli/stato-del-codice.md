# State of the code before part 3c ("Giocarci")

Reconnaissance of `/home/user/exosuits`, branch `m3c-giocarci` = `main` = `2e35aa6`. Read-only: nothing in the repository was changed, no Gradle run. Paths are relative to the repository root; `J/` stands for `src/main/java/com/toxineren/exoframes/`. Line numbers are those of commit `2e35aa6`.

How facts were checked:
- **[code]** read in the mod's sources;
- **[javap]** checked with `javap -c -p` against the Mojang-mapped merged jars in `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/` (`minecraft-merged/1.20.1-…`, `minecraft-merged/1.21.1-…`) and the loader-patched jars (`forge-1.20.1-47.4.29-minecraft-merged/…`, `neoforge-21.1.266-minecraft-merged-mojang/…`);
- **[arch]** read in the Architectury sources jars (`architectury-fabric` 9.2.14 and 13.0.11, `architectury-forge` 9.2.14, `architectury-neoforge` 13.0.11, from `~/.gradle/caches/modules-2/files-2.1/dev.architectury/`);
- **[not checked]** where a statement was not verified.

The Stonecutter active variant in the working tree is `1.21.1-neoforge` (`stonecutter.gradle.kts:6`): in `compat/impl/` the `>=1.21`/`neoforge` branches are the uncommented ones.

---

## 1. The pure rules already written in `core/`

### 1.1 `core/frame/FrameRules` (`J/core/frame/FrameRules.java`, 257 lines)

Constructor and constants:
- `public static final int IRON_ARMOR_POINTS = 15` (:34); `private static final double TOUGHNESS = 0.0` (:37); `MILLI_PER_RE = 1000` (:39).
- `public FrameRules(ExoframesConfig cfg)` (:43) — keeps the config by reference. The class comment (:22-30) says the `EnergyMeter` must be built with `new EnergyMeter(EnergyRates.from(config.energy))` and rebuilt when the config changes.

Public methods (all pure; only `tick`, `use`, `jump` mutate, and only the cell):
- `FrameEffects evaluate(FrameState s)` (:48) = `evaluate(s, FrameContext.NONE)`.
- `FrameEffects evaluate(FrameState s, FrameContext ctx)` (:56-108). Reads: the cell (energy state), `cfg.energy` (penalties), installed modules + `activeToggles` + `ctx.submerged()` (enabled modules), `cfg.frame.baseSpeedPenalty`, `cfg.modules.servosSpeedBonus`, `cfg.damage.brokenLegSpeedPenalty`, `cfg.pieces.shares`, `cfg.tiers.protectionMultiplier`, `cfg.frame.knockbackResistance`, `cfg.damage.brokenTorsoConsumptionMultiplier`. Computes:
  - `speedMultiplier` = `(1 − baseSpeedPenalty)` × `(1 + servosSpeedBonus)` if SERVOS enabled × `(1 − brokenLegSpeedPenalty)` per mounted broken leg × `penalties.speedMultiplier()` (:75-84). Defaults: 0.85 normal; 0.85×0.30 = 0.255 at EMPTY.
  - `canSprint` = `penalties.canSprint()` (:98): true only in `EnergyState.NORMAL` (false at LOW and EMPTY).
  - `fallDamageImmune` = constant `true` (:99).
  - `knockbackResistance` = `cfg.frame.knockbackResistance` (:100), default 0.5.
  - `canSwim` = constant `false` (:101).
  - `protectionPoints` = Σ over mounted, non-broken pieces of `shares[slot] × 15 × tiers.protectionMultiplier[tier]`, × `penalties.protectionMultiplier()` (:86-92). Full Scrap set = 15.0; EMPTY halves it.
  - `toughness` = 0.0 (:103).
  - `enabledModules`, `moduleMilliRePerTick` (sum of `costPerSecond × 1000 / 20` of enabled modules, :70-73), `consumptionMultiplier` (1.25 if torso mounted and broken, else 1.0), `energyState` (EMPTY without cell).
- `boolean moduleOperational(FrameState s, ModuleType m)` (:114).
- `boolean canActivate(FrameState s, ModuleType m)` (:131): operational + `EnergyState.NORMAL` + enough charge for per-use cost.
- `int tick(FrameState s, Activity a, EnergyMeter meter)` (:141) = `tick(s, a, meter, FrameContext.NONE, Set.of())`.
- `int tick(FrameState s, Activity a, EnergyMeter meter, FrameContext ctx, Set<ModuleType> held)` (:150-164): returns 0 without a cell; otherwise calls `evaluate(s, ctx)` (so each tick re-evaluates all effects), adds HOLD modules in `held` that `canActivate`, then `meter.tick(cell, a, moduleMilli, fx.consumptionMultiplier())`. **Returns the RE drained; it does not return the effects nor say whether the energy state changed.**
- `boolean use(FrameState s, ModuleType m)` (:173): per-use modules (ON_LANDING/ON_ATTACK) only, else `IllegalArgumentException`.
- `int jump(FrameState s, EnergyMeter meter)` (:189-195): drains `Math.round(meter.rates().jumpCost() × consumptionMultiplier)`; 0 without cell. Not gated by energy state.
- `int protectionPointsFor(Tier t)` (:198).

### 1.2 `core/frame/FrameContext` (`J/core/frame/FrameContext.java:9`)
- `public record FrameContext(boolean submerged)`; `NONE = new FrameContext(false)` (:12). Only feeds `ModuleCondition.SUBMERGED` (REBREATHER). Nothing about water/sinking/sprinting.

### 1.3 `core/frame/FrameEffects` (`J/core/frame/FrameEffects.java:12-37`)
- `record FrameEffects(double speedMultiplier, boolean canSprint, boolean fallDamageImmune, double knockbackResistance, boolean canSwim, double protectionPoints, double toughness, Set<ModuleType> enabledModules, int moduleMilliRePerTick, double consumptionMultiplier, EnergyState energyState)`; compact constructor copies the set (:25-27).
- `int armorPoints()` (:35) = `Math.round(protectionPoints)` — the value to put in the vanilla armor attribute.
- Record equality is value equality: comparing two `FrameEffects` is a ready "effects changed" test (includes `energyState` and `moduleMilliRePerTick`).

### 1.4 Energy (`J/core/energy/`)
- `enum Activity { IDLE, WALK, SPRINT }` (`Activity.java:4`). No code derives it.
- `final class EnergyCell` (`EnergyCell.java`): `EnergyCell(int capacity, int stored)` (:9, capacity > 0, stored in [0, capacity]); `static full(int)` (:20), `static empty(int)` (:24); `capacity()`, `stored()`; `int drain(int)` (:37) and `int charge(int)` (:45) return what moved; `double fraction()` (:52); `boolean isEmpty()` (:56). Mutable.
- `final class EnergyMeter` (`EnergyMeter.java`): `EnergyMeter(EnergyRates)` (:11); `int tick(EnergyCell cell, Activity activity, int extraMilliRePerTick, double multiplier)` (:16-24): milli = round((perSecond × 1000 / 20 + extra) × multiplier), carried in `carry` (milli-RE), drains whole RE; `EnergyRates rates()` (:30); `int carryMilliRe()` (:34). The carry is in memory only (not in `FrameState`, not saved).
- `record EnergyRates(int idlePerSecond, int walkPerSecond, int sprintPerSecond, int jumpCost)` (`EnergyRates.java:6`); `TICKS_PER_SECOND = 20` (:8); `spec()` = (2, 8, 20, 15) (:11); `from(ExoframesConfig.Energy)` (:16); `perSecond(Activity)` (:20).
- `record EnergyPenalties(boolean canSprint, boolean modulesEnabled, double speedMultiplier, double protectionMultiplier)` (`EnergyPenalties.java:6`); `forState(EnergyState, ExoframesConfig.Energy)` (:9): NORMAL (true, true, 1, 1); LOW (false, false, 1, 1); EMPTY (false, false, `emptySpeedMultiplier`, `emptyProtectionMultiplier`).
- `enum EnergyState { NORMAL, LOW, EMPTY }` (`EnergyState.java:4`); `of(EnergyCell, double lowThreshold)` (:10): EMPTY at 0, LOW if `fraction < lowThreshold` (strict), else NORMAL.

### 1.5 Damage and pieces (`J/core/armor/`)
- `enum DamageKind { HEAD, FROM_ABOVE, FALL, GENERIC }` (`DamageKind.java:4`).
- `final class DamageRouter` (`DamageRouter.java`, 224 lines):
  - `DamageRouter(ExoframesConfig.Damage damage, ExoframesConfig.Pieces pieces)` (:66) — validates `primaryShare` in [0,1] and shares finite ≥ 0.
  - `record Split(Map<PieceSlot,Integer> perPiece, int unabsorbed)` (:51).
  - `Split route(DamageKind kind, int amount, Map<PieceSlot,Piece> mounted)` (:90) — memoryless.
  - `Split route(DamageKind kind, int amount, Map<PieceSlot,Piece> mounted, Map<PieceSlot,Integer> carry)` (:116-139) — **today's "carry" variant takes `amount` in whole points (`int`) and multiplies internally by 1000** (:123-127); updates `carry` (values in [0,999]) in place; `unabsorbed` is always 0 when some piece can absorb. The design (§5.7) wants a signature taking the amount **in thousandths** so that fractions of a vanilla float damage reach the carry: that overload does not exist.
  - Primaries: HEAD→{HELMET}, FROM_ABOVE→{HELMET,TORSO}, FALL→{LEFT_LEG,RIGHT_LEG}, GENERIC→{} (:216-223).
- `final class Piece` (`Piece.java`): `Piece(PieceSlot, Tier, int maxDurability, int durability)` (:14); `static fresh(PieceSlot, Tier, ExoframesConfig.Tiers)` (:32); `slot()`, `tier()`, `durability()`, `maxDurability()`; `PieceCondition condition()` (:57) — this is the "visual state"; `boolean isBroken()` (:61, durability == 0); `int damage(int amount)` (:66) returns what was removed; `repairFraction(double)` (:79); `repairFull()` (:87). No event/notification when the condition changes: the caller must compare `condition()` before/after.
- `enum PieceCondition { INTACT, DAMAGED, CRITICAL, BROKEN }` (`PieceCondition.java`), `of(int durability, int max)` (:14): INTACT if d×2 > max; CRITICAL if d×100 < 15×max; BROKEN at 0; else DAMAGED.
- `enum PieceSlot` (`PieceSlot.java`): HELMET(0.15,1), TORSO(0.30,2), LEFT_ARM(0.125,1), RIGHT_ARM(0.125,1), LEFT_LEG(0.15,1), RIGHT_LEG(0.15,1).
- `enum Tier` (`Tier.java:5-9`): SCRAP(1,"scrap",0,1.000,CHEAP_FRAGILE), INDUSTRIAL(2,…,1.333), ARMORED(3,…,1.733), NETHERITE(4,…,1.600), PROTOTYPE(5,…,2.000); `byId`, `atLeast`.
- `enum TierTrait { CHEAP_FRAGILE, MINING_POWER, PROJECTILE_RESISTANCE, FIRE_IMMUNE_LAVA_WALK, SELF_REPAIR }` (`TierTrait.java`). Nothing reads traits.

### 1.6 `core/frame/FrameState` (`J/core/frame/FrameState.java`, 233 lines)
- `FrameState(ExoframesConfig cfg)` (:50) keeps the config (module installer).
- Cell: `cell()` (:56), `insertCell(EnergyCell)` (:61, replaces), `removeCell()` (:66).
- Pieces: `pieces()` read-only view (:73), `mount(Piece)` (:82, resets modules, carry and appearance of the slot), `unmount(PieceSlot)` (:95).
- Modules: `takeModules` (:106), `modules()` (:116), `installModule` (:128), `restoreModules` (:151).
- `Map<PieceSlot,Integer> damageCarryMilli()` (:177) — the **live** mutable map to pass to `DamageRouter.route(…, carry)`; saved by the codec as `carry_milli` per piece.
- `appearance(slot)` (:182), `setAppearance` (:191), `activeToggles()` (:204), `reinforced`/`setReinforced` (:208-214), `owner`/`setOwner` (:217-223), `unlocked`/`setUnlocked` (:226-232).

### 1.7 Modules as far as 3c touches them (`J/core/module/`)
- `ModuleType` (`ModuleType.java:7-16`): JETPACK(HOLD, 60/s), SHOCKWAVE(ON_LANDING, 200/use), NIGHT_VISION(TOGGLE, 3/s), SCANNER(TOGGLE, 5/s), REBREATHER(PASSIVE, 4/s, SUBMERGED), HYDRAULIC_BLADE(ON_ATTACK, 10/use), DRILL(HOLD, 25/s), ENERGY_SHIELD(TOGGLE, 30/s), SCULK_DETECTOR(PASSIVE, 1/s), SERVOS(PASSIVE, 5/s). No module items exist, so in 3c `enabledModules` is always empty in play and the `held` set of `tick` is always empty.
- `Activation`, `EnergyCost`, `ModuleCondition`, `ModuleFamily`, `ModuleInstaller`, `ModuleSlots`: unchanged by 3c.

### 1.8 Who calls what (game code, excluding `gametest/` and `src/test/`)
- **Called by game code:** `FrameState` (everywhere), `Piece` (`fresh`, `damage` in `frame/DebugWear.java:77-78`, `content/PieceItem.java:57-63`), `PieceCondition` (`DebugWear`, `SuitSummary.of` `J/compat/SuitSummary.java:43`, `SuitView`, `SuitSummaryTags`), `EnergyCell` (`CellItem`, `FrameEntry.enter` :115-116 checks `cell == null || cell.isEmpty()`), `Tier`, `PieceSlot`, `Appearance`, `DamageKind` (only inside `DamageClassifierImpl`), `FrameEffects` (only as the parameter type of `FrameAttributes.apply`).
- **Called by nothing in game code:** `FrameRules` (no instance anywhere), `FrameContext`, `EnergyMeter`, `EnergyRates`, `EnergyPenalties`, `EnergyState`, `Activity`, `DamageRouter`, `ConfigValidation`. They are exercised only by JUnit (`src/test/java/com/toxineren/exoframes/core/...`: `FrameRulesTest` 34 tests, `DamageRouterTest` 14, `EnergyMeterTest` 7, `ExoframesConfigTest` 10, …).
- `Compat.attributes()` (`J/compat/Compat.java:37`) and `Compat.damage()` (:41) have **no caller at all** (grep), and no JUnit or GameTest covers `FrameAttributesImpl` or `DamageClassifierImpl`.

---

## 2. The config

### 2.1 `ExoframesConfig` (`J/core/config/ExoframesConfig.java`, 124 lines) — public mutable POJO, every field with its default
- Top level (:15-22): `energy`, `damage`, `pieces`, `tiers`, `modules`, `frame`, `world`, `server`; `static defaults()` (:25).
- `Energy` (:30-41): `cellCapacity = 10_000`, `idlePerSecond = 2`, `walkPerSecond = 8`, `sprintPerSecond = 20`, `jumpCost = 15`, `lowThreshold = 0.10`, `emptySpeedMultiplier = 0.30`, `emptyProtectionMultiplier = 0.50`, `chargerRePerRedstone = 500`, `cellSwapSeconds = 2`.
- `Damage` (:44-49): `primaryShare = 0.60`, `repairKitFraction = 0.25`, `brokenTorsoConsumptionMultiplier = 1.25`, `brokenLegSpeedPenalty = 0.15`.
- `Pieces` (:52-62): `EnumMap<PieceSlot,Double> shares` (= `PieceSlot.protectionShare()`), `EnumMap<PieceSlot,Integer> baseModuleSlots`.
- `Tiers` (:65-79): `maxDurability` SCRAP 180, INDUSTRIAL 360, ARMORED 520, NETHERITE 650, PROTOTYPE 800; `protectionMultiplier` = `Tier.protectionMultiplier()`.
- `Modules` (:85-103): `costPerSecond`, `costPerUse` (from `ModuleType.cost()`), `jetpackMaxSeconds = 4`, `shockwaveMinFallBlocks = 6`, `servosSpeedBonus = 0.20`, `bladeBonusDamage = 6`.
- `Frame` (:106-110): `baseSpeedPenalty = 0.15`, `knockbackResistance = 0.5`, `exitHoldSeconds = 1`.
- `World` (:113-118): `ruinedVillage`, `buriedLab`, `armoredIllager`, `chestLoot`, all `true`.
- `Server` (:121-123): `ownerOnlyFrames = true`.
- **Not present** (values the design mentions or implies for 3c): a charger charge rate (§5.8 "carica a un ritmo dalla config"), a charger fuel buffer size, the sync rate to the owner (§5.6 "4 volte al secondo"), activity thresholds (§6 builder's choice), a step-height value (§8 question 2d).

### 2.2 `ConfigValidation` (`J/core/config/ConfigValidation.java`, 289 lines)
- `record ValidationResult(List<String> problems, ExoframesConfig normalized)` (:36); `static ValidationResult validate(ExoframesConfig input)` (:44) — returns a new normalized instance, never mutates input; null input → defaults with one problem.
- Rules per field (:58-144): `cellCapacity`, `chargerRePerRedstone` > 0; consumption values and `cellSwapSeconds` ≥ 0; `lowThreshold`, `empty*Multiplier`, `primaryShare`, `repairKitFraction`, `brokenLegSpeedPenalty`, `servosSpeedBonus`, `baseSpeedPenalty`, `knockbackResistance` clamped to [0,1] (NaN → default); `brokenTorsoConsumptionMultiplier` ≥ 1; shares normalized to sum 1; `tiers.maxDurability` and `protectionMultiplier` > 0; module costs ≥ 0 (extra entries dropped); `jetpackMaxSeconds` > 0; `exitHoldSeconds` ≥ 0; `world.*`, `server.*` copied.
- Messages in English starting with the field path. A new config field must be added here too (and `ExoframesConfigTest`).
- **Not called anywhere in `src/main/java`** (grep).

### 2.3 Who reads the config, where it comes from
- `RedstoneExoframes` (`J/RedstoneExoframes.java`): `private static volatile ExoframesConfig config = ExoframesConfig.defaults()` (:28); `static ExoframesConfig config()` (:57); `static void setConfig(ExoframesConfig)` (:65, comment: "the loader of a later milestone will call it; frame states already created keep the config they were born with"). **`setConfig` has no caller: no file is read anywhere; the config is always the defaults.** No Gson/JSON/TOML code in `src/main/java`.
- Callers of `RedstoneExoframes.config()` outside `gametest/`:
  - `frame/FramePermissions.java:26` (`server.ownerOnlyFrames`);
  - `frame/DebugWear.java:77` (`tiers`);
  - `content/ExoframeItem.java:49` (`new FrameState(config())`);
  - `content/CellItem.java:26,43` (`energy.cellCapacity`);
  - `content/PieceItem.java:57,63` (`tiers`);
  - `command/ExoframeCommand.java:340` (`new FrameState`);
  - `compat/impl/FrameStateAccessImpl.java:74` (`new FrameState` for `update`);
  - `compat/impl/FrameStateSavedData.java:619` (config captured **when the registry is loaded**: every loaded `FrameState` keeps it);
  - `compat/impl/FrameStateClientCache.java:29` (client, `readTrusted(…, config())`);
  - `client/ExitKey.java:54` (client, `frame.exitHoldSeconds`).
- Ordering constraint for a loader: the registry is loaded lazily on the first `FrameStateSavedData.get(server)` (first `PLAYER_JOIN` sync, first `SlotGuard` tick or first realign level tick), and captures `config()` then (:619); the creative tab's `CellItem::charged` supplier reads `cellCapacity` when the tab is built.

### 2.4 Reading a file on each loader
- `compat/Platform` (`J/compat/Platform.java`): `isClient()`, `isDedicatedServer()`, `isDevelopment()`, `Path configDir()` (:17), `isModLoaded(String)`, `loaderName()`. `PlatformImpl.configDir()` (`J/compat/impl/PlatformImpl.java:32`) = `dev.architectury.platform.Platform.getConfigFolder()`. `Compat.platform()` has **no caller** today.
- [arch] `dev.architectury.platform.Platform.getConfigFolder()` exists in 9.2.14 and 13.0.11 (Fabric sources :79 and :94). `LifecycleEvent.SERVER_BEFORE_START`, `SERVER_STARTING`, `SERVER_STARTED`, `SERVER_STOPPING`, `SERVER_STOPPED`, `SETUP` exist with the same names on both Architectury versions — a place to (re)load the file before the registry is read.
- Gson: `tools/check_sources.sh` rule 1b forbids `import com.google.gson` in `core/`; rule 1 forbids `net.minecraft`, loader and Architectury imports in `core/`. Gson may be used in `compat/`, `frame/`, a new package, or `platform/`. `ExoframesConfig` comment (:9-11): "la lettura/scrittura da file spetta a platform/". Gson ships with Minecraft on both versions [not checked by javap, standard].

### 2.5 Config values the client reads today, and what it will need
- Today: `frame.exitHoldSeconds` (`client/ExitKey.java:54`); `energy.cellCapacity` via `CellItem.charged()` (creative tab, built on the client too); the `ExoframesConfig` passed to `FrameStateCodec.readTrusted` (`FrameStateClientCache.java:29`; it only builds `new FrameState(cfg)` — modules restored without checks, cell capacity taken from the packet).
- On a remote client these are always the client's defaults; nothing is sent by the server. For 3c the design (§5.3 last bullet, §7 point 23) wants the owner packet to carry `canSprint`, `canSwim`, energy state computed by the server, "so the client does not evaluate the rules with its own config". Other server values a 3c client would use: `energy.cellSwapSeconds` (hold time of the swap key), `frame.exitHoldSeconds` (already read locally), `energy.cellCapacity` (charge bar of a cell stack: the stack has no capacity, `CellItem.java:11-12`).

---

## 3. The seams part 3c will use

### 3.1 `compat/FrameAttributes` and `compat/impl/FrameAttributesImpl`
- Interface (`J/compat/FrameAttributes.java`): `void apply(LivingEntity e, FrameEffects fx)` (:26), `void clear(LivingEntity e)` (:29). Javadoc (:7-16) already names the respawn listener in `FrameStateAccessImpl.registerCommon` as "the natural point where Milestone 3 will call apply".
- Impl (`J/compat/impl/FrameAttributesImpl.java`): ids `Ids.of("frame_speed")`, `frame_knockback`, `frame_armor`, `frame_toughness` (:31-34). `apply` (:37-43): MOVEMENT_SPEED multiply-total `speedMultiplier − 1`; KNOCKBACK_RESISTANCE add `knockbackResistance`; ARMOR add `armorPoints()` (int); ARMOR_TOUGHNESS add `toughness`. `clear` (:46-51).
- `set(...)` (:54-60) **always** `removeModifier(key)` then `addTransientModifier(...)`: **not idempotent** today (each call dirties the attribute and resends attributes to the client — Javadoc :20-21 says so).
- Identification per version: 1.21.1 key = the `ResourceLocation` (:69-80, `ADD_MULTIPLIED_TOTAL` / `ADD_VALUE`); 1.20.1 key = `UUID.nameUUIDFromBytes(id)` precomputed per id (:81-111), name = id string, `MULTIPLY_TOTAL` / `ADDITION`.
- [javap] what an idempotent `apply` can use: 1.20.1 `AttributeInstance.getModifier(UUID)`, `removeModifier(UUID)`, `addTransientModifier`; `AttributeModifier.getAmount()`, `getOperation()`, `getId()`. 1.21.1 `getModifier(ResourceLocation)`, `hasModifier(ResourceLocation)`, `boolean removeModifier(ResourceLocation)`, `addOrUpdateTransientModifier(AttributeModifier)`; `AttributeModifier` is a record with `id()`, `amount()`, `operation()`. The accessor names differ, so the "same value and operation" test belongs in `compat/impl`.
- [javap] 1.21.1 also has `Attributes.STEP_HEIGHT`, `SAFE_FALL_DISTANCE`, `FALL_DAMAGE_MULTIPLIER`, `GRAVITY`, `WATER_MOVEMENT_EFFICIENCY`, `SCALE`; 1.20.1 has none of these (13 attributes in `Attributes`); step height on 1.20.1 is `Entity.maxUpStep()` / `setMaxUpStep(float)` (Forge's `STEP_HEIGHT_ADDITION` [not checked]).
- Placeholders contribute nothing to armor: the `frame` armor material has defense, toughness and knockback 0 (`compat/impl/ArmorMaterialsImpl.java:33-37, 71-99`).
- Callers: **none**.

### 3.2 `compat/DamageClassifier` and `compat/impl/DamageClassifierImpl`
- Interface (`J/compat/DamageClassifier.java`): `DamageKind classify(LivingEntity victim, DamageSource source)` (:22), `boolean isProjectile(DamageSource)` (:25), `boolean isFall(DamageSource)` (:28), `boolean bypassesArmor(DamageSource)` (:31).
- Impl (`J/compat/impl/DamageClassifierImpl.java:32-52`): FALL if `DamageTypeTags.IS_FALL`; FROM_ABOVE if `IS_PROJECTILE` and shooter `getY()` (feet) > `victim.getEyeY() + 1.0` (or source position without shooter); HEAD if `getSourcePosition().y >= victim.getEyeY() − 0.25`; else GENERIC. `bypassesArmor` = `DamageTypeTags.BYPASSES_ARMOR` (:65). No version directives. With the real size the wearer's `getEyeY()` is ~2.67 above the feet (§4.8), which moves the HEAD/FROM_ABOVE thresholds up with it.
- Callers: **none**.

### 3.3 `compat/Net` and the existing packets
- `J/compat/Net.java`: `registerS2C(ResourceLocation, Class<T>, PacketCodec<T>, BiConsumer<T, ClientContext>)` (:27), `registerC2S(…, BiConsumer<T, ServerPlayer>)` (:33), `sendToPlayer(ServerPlayer, ResourceLocation, T)` (:43), `sendToServer(ResourceLocation, T)` (:51), `record ClientContext(Supplier<Player> player)` (:57). Handlers run on the game thread. `PacketCodec<T>` (`J/compat/PacketCodec.java`): `write(FriendlyByteBuf, T)`, `read(FriendlyByteBuf)`.
- Packets registered today (exactly two):
  1. **S2C `redstone_exoframes:frame_state_sync`** — `compat/impl/FrameStateSyncPacket` (`record FrameStateSyncPacket(UUID player, Optional<CompoundTag> state)`, ID :19, CODEC :21). Payload: UUID, presence boolean, full `FrameStateCodec.write(state)` NBT (pieces with tier/durability/max/modules/appearance/carry_milli, cell energy+capacity, active toggles, reinforced, owner, unlocked). **No indicators** (`canSprint`, `canSwim`, energy state). Registered in `FrameStateAccessImpl.registerCommon` (:292-293); client handler `FrameStateClientCache.accept`. Sent only by `FrameStateAccessImpl.syncTo` (:108-112), which is called by: `update` (:76, always, in `finally`), `clear` (:99), `enter` (:180, once), `exit` (:194, absent state), `PlayerEvent.PLAYER_JOIN` (:295), `PlayerEvent.PLAYER_RESPAWN` (:299; 1.20.1 two-argument form :301), and `/exoframe lock|unlock` on a worn frame (`command/ExoframeCommand.java:228-231`, `mutate` + `syncTo`). `mutate` never sends.
  2. **C2S `redstone_exoframes:frame_exit`** — `frame/FrameExitPacket` (empty record, ID :15, empty CODEC). Registered by `FrameEntry.registerNet()` (`J/frame/FrameEntry.java:210-213`, handler `exit(player)`), called from `RedstoneExoframes.init()` (:40). Sent by `client/ExitKey.tick` (:59, :62).
- `cell_swap` and `module_toggle` are listed in `docs/compat.md` table as "Rinviati (M3/M4)". Rule 5 of `tools/check_sources.sh`: the file that calls `registerC2S(` must reference `Compat.frameState()` in non-comment code.
- `NetImpl` (`J/compat/impl/NetImpl.java`): one `CustomPacketPayload.Type` + `StreamCodec` per id on 1.21.1; registering the same id twice throws (:123).

### 3.4 `compat/FrameStateAccess` (`J/compat/FrameStateAccess.java`) and impl
- Methods: `Optional<FrameState> get(Player)` (:61; server reads the saved data without creating, client returns the client cache), `void update(ServerPlayer, Consumer<FrameState>)` (:68), `boolean mutate(ServerPlayer, Consumer<FrameState>)` (:77), `void clear(ServerPlayer)` (:85), `Optional<FrameState> clientView(Player)` (:91), `void syncTo(ServerPlayer)` (:94), `UUID place(ServerLevel, FrameState, Vec3, float)` (:104), `Optional<PlacedFrame> placed(MinecraftServer, UUID)` (:107), `List<PlacedFrame> placedIn(ServerLevel)` (:110), `boolean updatePlaced(MinecraftServer, UUID, Consumer<FrameState>)` (:118), `boolean removePlaced(MinecraftServer, UUID)` (:127), `boolean enter(ServerPlayer, UUID)` (:135), `Optional<UUID> exit(ServerPlayer, ServerLevel, Vec3, float)` (:145), `Optional<UUID> wornFrameId(ServerPlayer)` (:148), `void stow(ServerPlayer, List<StowedStack>)` (:160), `List<StowedStack> takeStowed(ServerPlayer)` (:167), `List<StowedStack> takeStowedOfPlaced(MinecraftServer, UUID)` (:170), `CompoundTag writeSummaryTag(SuitSummary)` (:176), `Optional<SuitSummary> readSummaryTag(CompoundTag)` (:182), `void registerCommon()` (:188), `void registerClient()` (:194).
- `update` (`FrameStateAccessImpl.java:66-78`): creates the worn record if absent (`new FrameState(config())`), applies the change, **marks structural** (`FrameStateSavedData.updateWorn` :335-347 → `markStructural` in `finally`), then `syncTo` in `finally`.
- `mutate` (:81-89 → `FrameStateSavedData.mutateWorn` :355-367): only if a worn record exists; applies the change; `setDirty()` in `finally`; **not structural, no sync**; returns false if no record or read-only.
- Structural flag (`FrameStateSavedData.java`): field `structuralUnsaved` (:163); `markStructural()` (:818-821) sets it and `setDirty()`; `structuralUnsaved()` (:824); `markSaved()` (:829-832, after a successful file write); `saveWithPlayersIfNeeded(MinecraftServer)` (:841-849) on `PLAYER_QUIT` (`FrameStateAccessImpl.java:297`): if structural and not read-only → `server.getPlayerList().saveAll()` + `server.overworld().getDataStorage().save()`. Structural operations (javadoc :811-816): worn record created (`getOrCreateWorn` :325) or removed (`removeWorn` :384), `updateWorn` (:344), `place` (:399), `removePlaced` (:439), `updatePlaced` (:457), `addStowed` (:478), `take` stowed (:510), `enter` (:564), `exit` (:579). Non-structural: `mutateWorn` (:364), `forgetOrphaned` (:530). Consequence for 3c: energy drain and wear via `mutate` are non-structural (correct per the definition); a cell swap moves an item between inventory and registry and therefore must go through `update` (structural + sync) or a new structural path.
- The registry captures the config at load (:619) and `FrameStateSavedData.get(server)` (:180-206) does a `computeIfAbsent` on the overworld storage on every call (cheap, but called by every per-tick listener).

### 3.5 `compat/ItemDataAccess` (`J/compat/ItemDataAccess.java`)
- `int cellEnergy(ItemStack)` (never negative, 0 if absent), `void setCellEnergy(ItemStack, int)`; `Optional<PieceData> piece(ItemStack)`, `void setPiece(ItemStack, PieceData)`; `Optional<FrameItemData> frame(ItemStack)`, `void setFrame(ItemStack, FrameItemData)`; `Optional<SuitSummary> suitSummary(ItemStack)`, `void setSuitSummary(ItemStack, SuitSummary)`; `boolean hasData(ItemStack)`.
- `record PieceData(Tier tier, int durability, int maxDurability, List<ModuleType> modules, Appearance appearance)`; `record FrameItemData(Optional<UUID> owner, boolean unlocked, boolean reinforced, Optional<Integer> cellEnergy, Map<PieceSlot,PieceData> pieces)`.
- Data live under root key `exoframes` (`NbtKeys.ROOT`, `J/compat/NbtKeys.java:20`): stack tag on 1.20.1, `custom_data` on 1.21.1. The cell stack carries only `energy` (`NbtKeys.ENERGY` :22), never `capacity`. `frame(…)`/`setFrame(…)` are not used by game code (the exoframe item placed/returned carries no data — see §11 "il telaio raccolto perde reinforced e unlocked").

### 3.6 `compat/GameTests` (`J/compat/GameTests.java`) — every test-only helper
- `register(Class<?>)` (:29); `ServerPlayer mockServerPlayer(GameTestHelper)` (:39); `List<Component> messagesTo(ServerPlayer)` (:45); `writeRawItemData(ItemStack, CompoundTag)` (:53); `boolean addRawStowed(MinecraftServer, UUID, CompoundTag)` (:62); `List<CompoundTag> orphanedStowed(MinecraftServer, UUID)` (:68); `int forgetOrphaned(MinecraftServer, UUID)` (:75); `Path saveRegistry(MinecraftServer)` (:81); `Path registryFolder(MinecraftServer)` (:87); `CompoundTag registryContents(MinecraftServer)` (:90); `Optional<CompoundTag> readRegistry(MinecraftServer, Path)` (:98); `boolean registryChangeUnsaved(MinecraftServer)` (:104); `boolean portalsAccept(Entity, ServerLevel)` (:113); `EquipEventCounter countEquipEvents(ServerLevel, BlockPos)` (:120) with `count()`, `close()`.
- Nothing to hurt, age (spawn invulnerability), tick, or move a mock player, nor to read attribute modifiers.

---

## 4. Where the worn state changes today; tick listeners

### 4.1 Every place that creates, replaces, moves or removes a worn record, or changes what a player wears
1. **`FrameEntry.enter(ServerPlayer, ExoframeEntity)`** (`J/frame/FrameEntry.java:95-146`): checks (permission → `LOCKED`, charged cell → `NO_CELL`, already inside → `ALREADY_INSIDE`, `noCollision(standingBox)` → `NO_ROOM`, stowed armor of another owner → `HOLDS_ARMOR`), then `frames.enter` (:134, structural + one sync), `stowWornArmor` (:137), `dress(player, SuitSummary.of(state))` (:138: four placeholders + summary, silent), `stopRiding`, `teleportTo` frame spot/yaw (:140), `frame.discard()` (:141), `SafeSpots.remember` (:142), `player.refreshDimensions()` (:143), `FrameLog.event(…, "entered", …)` (:144). No attributes.
2. **`FrameEntry.exit(ServerPlayer)`** (:170-181): `Placeholders.removeAll` (:176), `returnStowed` (:177), `leaveFrame(…, "left")` (:178), `stepOut` (:179).
3. **`FrameEntry.onDeath(ServerPlayer)`** (:192-203, from `EntityEvent.LIVING_DEATH`, `RedstoneExoframes.java:47-52`): `Placeholders.removeAll`, `leaveFrame(…, "left at death")`; stowed armor stays in the record; catches `RuntimeException`.
4. **`FrameEntry.leaveFrame`** (private, :283-306), shared by 2 and 3: `restingSpot` (:284), `frames.exit` (:285, structural + sync of absent state), `FramePlacement.spawnView` if `areEntitiesLoaded && hasChunk` (:295-297), `refreshDimensions` (:298), `FrameLog.event` (:299), chat message `ELSEWHERE`/`ELSEWHERE_BORDER` if not below (:300-304). No attributes cleared.
5. **`DebugWear.wear(ServerPlayer, SuitSummary)`** (`J/frame/DebugWear.java:36-45`): `frames.update(player, rebuild)` (creates or replaces pieces and inserts a full charged cell, :68-83; structural + sync), `FrameEntry.dress`, `refreshDimensions`. **No log line.** Called by `/exoframe debug wear nudo|rottame` (`ExoframeCommand.java:178-184`).
6. **`DebugWear.remove(ServerPlayer)`** (:55-65): `Placeholders.removeAll`, `returnStowed`, `frames.clear` (structural, orphans leftover stowed, sync), `refreshDimensions`. **No log line.** Called by `/exoframe debug wear via` (`ExoframeCommand.java:190-201`).
7. **`SlotGuard.tick(ServerPlayer)`** (`J/frame/SlotGuard.java:76-123`): does not change the record, but changes what the player wears: with a record, puts missing/wrong placeholders back and **rewrites the torso summary whenever `SuitSummary.of(state)` differs** (:88-115; so a piece changing condition through `mutate` is redrawn within one tick without any other code), hands out displaced real armor, deletes strays, `refreshDimensions` if changed; without a record, removes placeholders (armor every tick, everything every 20 ticks).
8. **`/exoframe lock|unlock`** on the worn frame (`ExoframeCommand.java:208-241`): `frames.mutate(player, s -> s.setUnlocked(...))` + `syncTo` (:227-231) — not structural.
9. **`FrameStateAccessImpl.registerCommon`** (`J/compat/impl/FrameStateAccessImpl.java:292-302`): `PLAYER_JOIN` → `syncTo`; `PLAYER_RESPAWN` → `syncTo`. These are the join/respawn hooks the design wants `FrameAttributes.apply` on.
- Placed-frame changes (`FrameInteractions` mount/insert cell/unmount/pick up, `FramePlacement.place`, `/exoframe remove`, `/exoframe debug place`) only touch placed records, never a worn one.
- There is no common "worn state changed" notifier; each site does its own subset of {placeholders, summary, `refreshDimensions`, sync, log}.

### 4.2 Tick listeners registered today
- `TickEvent.PLAYER_PRE` → `SuitSize::tick` (`RedstoneExoframes.java:43`; `J/frame/SuitSize.java:92-98`), both sides, every player: one slot read + a weak-map `put`; `refreshDimensions` on change.
- `TickEvent.PLAYER_POST` → `SlotGuard.tick` (`SlotGuard.java:55-59`), server: `Compat.frameState().get(player)` (saved-data lookup) every tick; for a wearer `SuitSummary.of(state)` + equality + four slot identity checks + `removeStrays`; for others four armor slots, full scan every 20 ticks.
- `TickEvent.PLAYER_POST` → `SafeSpots.tick` (`J/frame/SafeSpots.java:51-55, 63-70`), server: returns unless `tickCount % 20 == 0`; then `wornFrameId` lookup.
- `TickEvent.SERVER_LEVEL_POST` → `FrameRealign.onLevelTick` (`J/frame/FrameRealign.java:50, 63-69`): per level, every 100 ticks scans `placedIn(level)`.
- `PlayerEvent.PLAYER_QUIT` → `SafeSpots::forget` (`SafeSpots.java:56`), `FrameStateSavedData.saveWithPlayersIfNeeded` (`FrameStateAccessImpl.java:297`).
- `PlayerEvent.DROP_ITEM` → `SlotGuard::onDrop` (`SlotGuard.java:60`).
- `EntityEvent.LIVING_DEATH` → `FrameEntry.onDeath` (`RedstoneExoframes.java:47-52`).
- Client: `ClientTickEvent.CLIENT_POST` → `ExitKey::tick` (`J/client/ExitKey.java:43`); `ClientPlayerEvent.CLIENT_PLAYER_QUIT` → `HOLD.reset()` (:44) and `FrameStateClientCache.clear()` (`FrameStateAccessImpl.java:307`).
- No `EntityEvent.LIVING_HURT` listener exists.
- Mock players are not ticked by the server through `Player.tick`: the test class says the server does not advance their tick counter (`ExoframeGameTests.java:126-140`), and tests call `player.doTick()` (which runs `Player.tick` and fires PLAYER_PRE/POST, e.g. `suitCrawlsUnderLowCeiling` :599-623) or call `SlotGuard.tick`/`SafeSpots.tick`/`SuitSize.tick` directly. A 3c per-player tick must therefore have a directly callable method for tests.

---

## 5. Content

- Items (`J/content/ModItems.java`), registered with Architectury `DeferredRegister` in `ModItems.register()` (:61-63 → `RedstoneExoframes.init` :37):
  - `exoframe` → `ExoframeItem` (stacksTo 1) (:18); places a new empty frame (`ExoframeItem.useOn` `J/content/ExoframeItem.java:44-69`, `MAX_DROP = 3.0` :30, `NO_ROOM` :24).
  - `redstone_cell` → `CellItem` (stacksTo 1) (:21).
  - `scrap_helmet`, `scrap_torso`, `scrap_arm`, `scrap_leg` → `PieceItem(Tier.SCRAP, PieceKind.…)` (:26-33); `PIECES` list (:35). `PieceItem.toPiece(ItemStack, PieceSlot)` (`PieceItem.java:49`), `static fromPiece(Piece, List<ModuleType>, Appearance)` (:72, throws for a tier without item).
  - Placeholders `exoframe_helmet`, `exoframe_torso`, `exoframe_legs`, `exoframe_boots` → `Compat.armorMaterials().newPlaceholder(...)` (:44-51), outside the creative tab.
  - `suit_model` → `Compat.suitAssets().newSuitModelItem(...)` (:55), outside the tab.
- Creative tab `redstone_exoframes:main` (`J/content/ModCreativeTabs.java:20-22`, icon `REDSTONE_CELL`): `EXOFRAME`, the four Scrap pieces, `CellItem::charged` (:28-34).
- `CellItem` (`J/content/CellItem.java`): `EnergyCell toCell(ItemStack)` (:24, capacity from config, charge clamped), `static ItemStack fromCell(EnergyCell)` (:34, drops capacity), `static ItemStack charged()` (:42). **No `appendHoverText`, no `isBarVisible`/`getBarWidth`/`getBarColor`: no charge bar, no tooltip.** `PieceItem` has no durability bar or tooltip either.
- [javap] `Item.isBarVisible(ItemStack)`, `getBarWidth(ItemStack)`, `getBarColor(ItemStack)`, `inventoryTick(ItemStack, Level, Entity, int, boolean)` have the same signatures on 1.20.1 and 1.21.1; `appendHoverText` differs: 1.20.1 `(ItemStack, Level, List<Component>, TooltipFlag)`, 1.21.1 `(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)` — a tooltip needs a compat seam (`docs/compat.md` table lists it with "—").
- Entities: `ModEntities.EXOFRAME` (`J/content/ModEntities.java:24-29`), `ExoframeEntity extends CompatEntity` (`J/entity/ExoframeEntity.java:41`). `CompatEntity` (`J/compat/CompatEntity.java:14`) is an abstract class without directives extending `compat/impl/EntityBase` (`J/compat/impl/EntityBase.java:35`), which holds the version differences — the pattern a `CompatBlockEntity` would copy.
- **No block, block item, block entity, block entity type, menu, recipe, loot table or tag exists.** `src/main/resources/data/` holds only `redstone_exoframes/structure/empty.nbt` and `structures/empty.nbt` (GameTest structure, one per version naming).
- Language files (`src/main/resources/assets/redstone_exoframes/lang/en_us.json`, `it_it.json`, 38 keys each, same keys): `item.redstone_exoframes.*` (11: exoframe, redstone_cell, four scrap pieces, four placeholders, suit_model), `entity.redstone_exoframes.exoframe`, `itemGroup.redstone_exoframes.main`, `commands.redstone_exoframes.*` (14), `message.redstone_exoframes.*` (9: no_room, locked, slot_taken, no_cell, already_inside, frame_elsewhere, frame_elsewhere_border, holds_armor, exit_hold), `key.redstone_exoframes.exit`, `key.categories.redstone_exoframes`. No `block.` keys. No test checks that the two files have the same keys.
- Item models (`assets/redstone_exoframes/models/item/*.json`): 11 files, all `{"parent":"minecraft:item/generated","textures":{"layer0":"redstone_exoframes:item/<name>"}}`; `suit_model.json` reuses `item/exoframe_torso`. No blockstates, no block models.
- Sprites: `tools/gen_placeholders.py` (stdlib only, deterministic) writes `textures/item/redstone_cell.png` (`draw_cell`), `icon.png`, the four placeholder icons (`PIECE_ICONS` :213), the Scrap pieces and `exoframe` from 16×16 character grids (`SCRAP_ICONS` :338-339, `SPRITE_COLORS`), and the two transparent 1.20.1 armor layers in `src/main/resources-mc1.20.1/`. `outputs()` (:342) is the list; `tools/check_art.sh` regenerates into a temp dir and compares each generated file with the committed one (a committed PNG not produced by the script is not compared).

---

## 6. The client

- Client cache of the frame state: `compat/impl/FrameStateClientCache` (`J/compat/impl/FrameStateClientCache.java`): `Map<UUID, FrameState> STATES` (:17); `accept(FrameStateSyncPacket)` (:27-33) stores `readTrusted` state or removes it; `view(UUID)` (:36); `clear()` (:41) on `CLIENT_PLAYER_QUIT`. Holds the full state (pieces, cell energy/capacity, modules, owner, flags) of the local player only. Updated on every `syncTo` (see §3.3 list).
- **Nothing in client code reads it**: no call to `Compat.frameState().get`/`clientView` in `client/`, `compat/client/`, `compat/impl/client/` or `mixin/` (grep). The client decides "wearing the suit" only from the torso placeholder (`SuitSize.wearsSuit` `J/frame/SuitSize.java:59-64`, `SuitLayer.wearsSuit` `J/client/SuitLayer.java:61`).
- Key handling pattern: `client/ExitKey` (`J/client/ExitKey.java`): static `KeyMapping KEY` (`key.redstone_exoframes.exit`, `GLFW_KEY_V`, category `key.categories.redstone_exoframes`) (:26-32), `register()` (:41-45: `KeyMappingRegistry.register`, `ClientTickEvent.CLIENT_POST`, reset on quit), `tick(Minecraft)` (:47-67): `canCount = keyDown && screen == null && SuitLayer.wearsSuit(player)`, `needed = ExitHold.ticksNeeded(config().frame.exitHoldSeconds)`, switch on `ExitHold.Step` → action-bar message `message.redstone_exoframes.exit_hold` with percent / clear / `sendToServer(FrameExitPacket)`. `client/ExitHold` (package-private, pure Java, tested by `src/test/java/.../client/ExitHoldTest.java`): `enum Step { NONE, SHOW_PROGRESS, CLEAR_PROGRESS, SEND, CLEAR_AND_SEND }`, `static int ticksNeeded(int seconds)`, `Step tick(boolean keyDown, boolean canCount, int needed)`, `long percent()`, `void reset()`. A 2-second cell-swap key can reuse `ExitHold` as is (it is generic).
- Client init: `client/RedstoneExoframesClient.init()` (`J/client/RedstoneExoframesClient.java:16-21`): `registerClient`, `addPlayerLayer(SuitLayer::new)`, `EntityRendererRegistry.register(EXOFRAME, ExoframeEntityRenderer::new)`, `ExitKey.register()`.
- What is drawn: `client/SuitLayer` (player render layer: reads the torso placeholder summary → `SuitView` → `ClientCompat.suitRenderer()`), `client/ExoframeEntityRenderer` (`:34`, placed frame), `client/PilotPose` + three client mixins (`PlayerRendererMixin`, `ItemInHandLayerMixin`, `LivingEntityCullingMixin`). Piece condition 0-3 drives which geometry copy is visible (`SuitView` :41).
- **No HUD** of any kind; only action-bar messages. [arch] `ClientGuiEvent.RENDER_HUD` exists but its callback differs: 9.2.14 `renderHud(GuiGraphics, float tickDelta)`, 13.0.11 `renderHud(GuiGraphics, DeltaTracker)` (compat.md lists the HUD callback as a deferred client seam). The design's cell-swap indicator is "nella barra delle azioni", which needs no seam.
- Movement is client-authoritative: nothing today touches sprinting or swimming on either side.

---

## 7. Build, resources, checks, CI

### 7.1 Resources and per-version folders
- Common resources: `src/main/resources/` (assets, `data/redstone_exoframes/structure(s)/empty.nbt`, `fabric.mod.json`, `META-INF/mods.toml`, `META-INF/neoforge.mods.toml`, `pack.mcmeta`, `icon.png`).
- Per-version folders **already exist**: `src/main/resources-mc1.20.1/` (the two transparent armor layers + `redstone_exoframes.mixins.json` JAVA_17) and `src/main/resources-mc1.21.1/` (`redstone_exoframes.mixins.json` JAVA_21). `build.gradle.kts:180-181`: `sourceSets.main { resources.srcDir(rootProject.file("src/main/resources-mc$mc")) }` (missing dir ignored). This is where the design's `tools/gen_data.py` output would go.
- `tasks.processResources` (`build.gradle.kts:189-210`): excludes the other loaders' metadata files; `expand(props)` only on `fabric.mod.json`, `META-INF/mods.toml`, `META-INF/neoforge.mods.toml`, `pack.mcmeta`. Everything else is copied verbatim.
- [javap/jar listing] vanilla data folders: 1.20.1 `data/minecraft/recipes/`, `loot_tables/`, `advancements/`, `tags/blocks/`, `tags/items/`; 1.21.1 `recipe/`, `loot_table/`, `advancement/`, `tags/block/`, `tags/item/`. Shaped recipe result: 1.20.1 `"result":{"item":"minecraft:iron_chestplate"}`; 1.21.1 `"result":{"count":1,"id":"minecraft:iron_chestplate"}`. `tools/gen_data.py` does not exist.
- `stonecutter.gradle.kts`: active variant `1.21.1-neoforge`; constants `fabric`, `forge`, `neoforge`. `versions/<variant>/gradle.properties` per variant (e.g. 1.20.1-forge: `mod.java=17`, `mod.pack_format=15`, `deps.architectury=9.2.14`, `deps.geckolib=4.8.4`).

### 7.2 `tools/check_sources.sh` (one line per rule)
1. No `net.minecraft`, `net.fabricmc`, `net.minecraftforge`, `net.neoforged`, `dev.architectury` import in `core/`.
1b. No `com.google.gson` import in `core/`.
1c. Every type in `core/` is named in at least one file of `src/test/java` (a new core class needs a test).
2/6. Stonecutter directives only in `compat/impl/`, `platform/` and `compat/Ids.java` (never in tests or interfaces).
3. Forge entrypoint registers the mod bus before `RedstoneExoframes.init()`.
4. `com.toxineren.exoframes.compat.impl.` referenced only from `compat/` (main and test).
5. A file that calls `registerC2S(` (other than `Net`/`NetImpl`) must mention `Compat.frameState()` in non-comment code.
7. Client-only packages (`client`, `compat.client`, `compat.impl.client`, `mixin.client`) referenced only from those packages and the three client entrypoints in `platform/`.
8. `net.minecraft.client.` / `com.mojang.blaze3d.` referenced only from the same files (even in comments).

### 7.3 `tools/check_jars.sh`
- (a) exactly four jars with the expected names; one metadata file per loader; (b) `pack_format` 15/34; (c) Minecraft range; (d) main class and declared entrypoints in the jar; (e) no refmap; (e2) mixin config at root with `required: true`, `defaultRequire: 1`, right `compatibilityLevel`, **`mixins` section exactly `[PlayerSize120Mixin]` or `[PlayerSize121Mixin]`, `server` empty, `client` exactly the three classes `client.ItemInHandLayerMixin`, `client.LivingEntityCullingMixin`, `client.PlayerRendererMixin`**, target names per loader/version hard-coded in `TARGETS`, names in the right namespace, config declared in the loader metadata; (f) `icon.png`; (f2) the two transparent armor layers present only in 1.20.1 jars; (g) `tools/check_jar_privacy.py` (no local paths, e-mails, image/sound metadata). **A new mixin (e.g. for sprint/swim/fall) requires editing the hard-coded lists and target names in `check_jars.sh`.** A new block, recipe or data file needs nothing from this script beyond privacy.

### 7.4 `tools/check_art.sh`
- Runs `tools/art/selftest.py`, regenerates geometry/atlas/glowmask/animations (`tools/art/build.py`) and compares; regenerates `tools/gen_placeholders.py --out` and compares each produced PNG with the committed one. New item or block textures, if generated, must be added to `gen_placeholders.py` `outputs()`.

### 7.5 `tools/run_gametests.sh`, `tools/check_log.py`, `tools/log_allowlist.txt`
- `run_gametests.sh`: per variant `./gradlew :<v>:runGameTestServer` (clean world first, `build.gradle.kts:118-122`); passes only with exit 0, a line `All N required tests passed` with N > 0 and **N equal to the number of `add(tests,` lines** in `ExoframeGameTests.java`, and `check_log.py gametest` clean. Log kept in `versions/<v>/build/gametest/server.log`.
- `check_log.py`: every WARN/ERROR/FATAL line (with its trace) that mentions "exoframe" in any form, plus any mixin-apply error or trace through `com.toxineren`, must match a regex line of `log_allowlist.txt` for the scope, at most N times. Empty/unparsable log fails.
- `log_allowlist.txt`: 7 `gametest` lines today (unknown tier id, unreadable stowed entry, orphaned stowed ×2, vanilla "Error loading saved data", previous copy used, read-only) + Fabric "No data fixer registered for exoframe" for `gametest` and `client`. **A new block entity type on Fabric will very likely add a "No data fixer registered for <id>" WARN** (same mechanism as the entity [not checked for block entities]) and needs an allowlist line; every new intentional WARN does.

### 7.6 CI (`.github/workflows/build.yml`)
- Steps: checkout; Temurin 21; setup-gradle; `:<4 variants>:test`; `:<4 variants>:build` + `collectJar`; `bash tools/run_gametests.sh` (timeout 20 min); `bash tools/check_sources.sh`; Python 3.12; `pip install -r tools/art/requirements.txt`; `bash tools/check_art.sh`; `bash tools/check_jars.sh`; upload jars. No dedicated-server start step in CI (the design's "server dedicato" check is done by hand/scripts, `tools/client/client_shot.sh`). A `tools/gen_data.py` freshness check would be a new CI step.

---

## 8. The tests' infrastructure

- In-world tests: `J/gametest/ExoframeGameTests.java` (4038 lines, 95 `add(tests, …)` lines in `generate()` :163-273). Rule (class comment :121-124): a `private static void temaCaso(GameTestHelper helper)` ending with `helper.succeed()`, plus `add(tests, "temaCaso", ExoframeGameTests::temaCaso);` — the list must be the same on the four variants.
- `add(...)` (:3967-3971): test id `redstone_exoframes:<snake_case>`, structure `redstone_exoframes:empty` (8×8×8), `MAX_TICKS = 100` (:150), required. Body runs through `startWhenEntitiesLoaded` (:3984ff): waits up to `ENTITY_LOAD_TICKS = 20` (:152) for the structure chunks' entities, taken from the 100 ticks.
- Shared-server rules (class comment :126-140): all tests share one server, one saved data and one level and start together; each test checks only its own mock players and `frameId`s (no global counts), moves its players into its own structure, removes everything it created before `succeed()` (placed/worn records, entities, ground items, orphans via `GameTests.forgetOrphaned`), restores game rules and world border in `finally`, does not rely on the mock player's tick counter (sets it if needed), leaves no log warnings except intended ones.
- Mock player (`J/compat/impl/GameTestsImpl.java:93-147`): `MockPlayer extends ServerPlayer`, `isCreative()` always true; `isSpectator()` follows its game mode; records system messages; connection on an `EmbeddedChannel`; stays in the player list. Tests switch `setGameMode(GameType.SURVIVAL/CREATIVE)` (e.g. :1299, :1310). Can be moved (`moveTo`, `teleportTo`), killed (`kill()` :2233, `setHealth`), ticked only by calling `doTick()`.
- [javap] Hurting a mock player: `ServerPlayer.hurt` (1.20.1 and 1.21.1) returns false while `spawnInvulnerableTime > 0` unless the source is in `BYPASSES_INVULNERABILITY` (bytecode :1545-1565 on 1.20.1); `spawnInvulnerableTime` is set to 60 in the constructor (:74-76) and decremented in `ServerPlayer.tick()` (:816-821), which the level ticks. A mock player created at the start of a test is therefore spawn-invulnerable for about 60 server ticks out of the 100 [whether the level ticks the mock player from tick 0 not checked]. In creative mode `abilities.invulnerable` also blocks damage. `Player.causeFallDamage` returns early when `abilities.mayfly` (creative) (javap). Tests of "damage wears pieces" and "fall without damage" either wait, use survival and a bypassing source (which `bypassesArmor` may then skip), or call the 3c hurt handler directly.
- JUnit: `src/test/java/com/toxineren/exoframes/` — `core/` (armor, config, energy, frame, module), `compat/` (`SuitBonesTest`), `compat/client/`, `compat/impl/` (codec, saved data, piece tags, registry file, schema ids, stack tags, summary tags), `client/` (`ExitHoldTest`, `PilotPoseTest`, `SuitScaleTest`, `TierPaletteTest`), `art/ExoframeGeometryTest`. Run on the four variants in CI.
- Existing tests touching behaviour 3c will change:
  - `DamageRouterTest` (route with `int` amount and carry, :138-260) — affected by the thousandths signature.
  - `FrameRulesTest` (34 tests), `EnergyMeterTest`, `EnergyRatesTest`, `EnergyStateTest`, `ExoframesConfigTest` (any new config field).
  - GameTests: `cellItemCarriesItsCharge` (:900ff; `CellItem` round trip), `enterRefusedWithoutAChargedCell`, `debugWear*` (debug wear inserts a full cell), `summaryFollowsTheState`, `frameStateOnMockPlayer` (:283), `lockAndUnlockTheWornFrame` (mutate+sync), every enter/exit/death test (attributes would now be applied/cleared there), `quitSavesTheRegistryWithThePlayers` (structural flag).
  - No test covers `FrameAttributes` or `DamageClassifier`.

---

## 9. The design's §11, first group ("per il piano della parte 3c")

1. **One notifier for "worn state changed".** Sites: `FrameEntry.enter` (`J/frame/FrameEntry.java:134-144`), `FrameEntry.leaveFrame` (:283-306, used by `exit` :170 and `onDeath` :192), `DebugWear.wear` (`J/frame/DebugWear.java:36-45`), `DebugWear.remove` (:55-65), `SlotGuard.tick` (`J/frame/SlotGuard.java:76-123`), plus `PLAYER_JOIN`/`PLAYER_RESPAWN` (`FrameStateAccessImpl.java:295-301`) and `/exoframe lock|unlock` worn branch (`ExoframeCommand.java:227-231`). Size: one new class in `frame/` and a call at ~6 sites; each site today does a different subset of placeholders/summary/`refreshDimensions`/sync/log. Prerequisite for attributes (§5.6) and for the death/exit `clear`.
2. **Split what is together by accident.** (a) `FrameStateSavedData` (`J/compat/impl/FrameStateSavedData.java`, 1022 lines): registry operations (:311-594) vs file format and tolerance rules (load :595-760, write/save :852-1022, unread/orphan handling) — a refactor across `compat/impl/` (+ `GameTestsImpl`, `FrameStateSavedDataTest` 751 lines). (b) `/exoframe remove` procedure (`ExoframeCommand.removeFrame` :252-311) and line-of-sight search (`frameInSight` :317-330) to `frame/`, reusing `FrameInteractions` (`pieceStack` :283, unmount :216-273) — one class moved, ~100 lines. (c) "chunk has entities loaded" predicate duplicated in `FrameEntry.java:295` and `FrameRealign.java:83` — a few lines.
3. **Unknown pieces inside a readable record** are dropped on load (`FrameStateCodec.read` via `PieceTags.readPiece`, `SchemaIds.warnUnknown`; rule in the comment `FrameStateSavedData.java:99-102`): a codec change if addressed; otherwise the "raise the schema" rule holds. Independent.
4. **Sounds** for place, mount/unmount, enter/exit: none exists (`Placeholders.setArmorSlot` silent by design); call sites `FramePlacement.place`, `FrameInteractions` (mount :137-175, insert cell :189-204, unmount :216-273), `FrameEntry.enter/exit`. A few lines per site; GameTest `countEquipEvents` checks armor-slot silence and must stay at zero.
5. **Modules restored without installer checks when mounting by hand** (`FrameInteractions.java:154-165`, `restoreModules`): no module items exist; small.
6. **Picked-up frame loses `reinforced` and `unlocked`** (`FrameInteractions.java:248`, `new ItemStack(ModItems.EXOFRAME.get())`; `ItemDataAccess.setFrame` unused; `ExoframeItem.useOn` :49 builds a fresh state): small–medium (write `FrameItemData` on pick-up, read it on place).
7. **Command line of sight vs click aim** (`ExoframeCommand.frameInSight` :321 uses `ClipContext.Block.COLLIDER`): a few lines; naturally done with item 2(b).
8. **Two frames at the same point** on exit/death (`FrameEntry.restingSpot` :407, `leaveFrame`): `ExoframeItem`/`FramePlacement.place` refuse it, exit/death do not. Small–medium (policy decision).
9. **Placeholder escaping the guard between ticks** (`SlotGuard`, menu close): small (a menu-close hook) [Architectury event availability not checked].
10. **A piece of a tier without item cannot be unmounted** (`FrameInteractions.pieceStack` :283-290, `PieceItem.fromPiece` throws :84): vanishes in M5.
11. **Debug command leaves no log line** (`DebugWear.wear`/`remove`): a few lines, best inside the notifier (item 1).
12. **Valid registry file without sections loads as empty and writable** (`FrameStateSavedData.load` :605-610 uses `getCompound`, which returns empty): a few lines in the load path; depends on 2(a) if done after the split.
13. **Shrinking world border** does not move placed frames (`FrameRealign.check` :75-105): small.
14. **Exit more than ~1.33 blocks above ground**: no step-out (`FrameEntry.stepOut` :318-323, eyes outside the frame box): small.
15. **Step-out is cautious** (`FrameEntry.standingSpot` :368, `wayIsFree` :354): medium (per-axis paths, small step-up).
16. **Step-out avoids only lava and fire** (`FrameEntry.java:387`): a few lines (more block tags).
17. **Save on player quit is wider than the registry** (`FrameStateSavedData.saveWithPlayersIfNeeded` :841-849): small–medium; touches 2(a).
18. **Allowlist gives a maximum, not an exact count; `TESTREG` regex fails on paths with spaces** (`tools/log_allowlist.txt` `= TESTREG \S*…`, `tools/check_log.py`): small, tooling only.
- Dependencies: 1 before attributes/effects; 11 fits into 1; 2(a) before 12 and 17 if those are done; 7 with 2(b). Everything else is independent.

---

## 10. Gaps and decisions the plan must make

Seams and classes the design names that do not exist:
- `compat/CompatBlockEntity` (design §5.8) does not exist; nor `CompatEntityBlock` (`docs/compat.md` table: "Rinviata (M3/M4)"). [javap] differences to close: `BlockEntity.load(CompoundTag)`/`saveAdditional(CompoundTag)`/`getUpdateTag()` (1.20.1) vs `loadAdditional(CompoundTag, HolderLookup.Provider)`/`saveAdditional(CompoundTag, Provider)`/`getUpdateTag(Provider)` (1.21.1); `BlockBehaviour.use(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)` (1.20.1) vs `useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)` returning `ItemInteractionResult` + `useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)` (1.21.1); `BaseEntityBlock` on 1.21.1 has an abstract `MapCodec codec()`; `BlockEntityType.Builder.of(...)`/`build(Type)` identical.
- No block registry (`ModBlocks`), block item, block entity type registry, blockstate/model files, block loot table, `block.` lang keys, creative-tab entry for the charger.
- `tools/gen_data.py` does not exist (resource folders per version do).
- The `cell_swap` C2S packet, a cell-swap key, an owner-indicator packet (or extension of `FrameStateSyncPacket`) do not exist.

Signatures the design assumes and the code does not have:
- `DamageRouter.route` in thousandths: today `route(DamageKind, int amount, Map, Map carry)` takes whole points (`DamageRouter.java:116`).
- `FrameAttributes.apply` idempotent: today always remove + add (`FrameAttributesImpl.java:54-60`); no caller.
- `FrameRules.tick` returns only RE drained; detecting "energy state changed" or "effects changed" needs the previous `FrameEffects` kept per player (record equality is enough).
- `FrameStateSyncPacket` carries no `canSprint`/`canSwim`/energy-state indicators (design §5.3).
- `CellItem` has no charge bar or tooltip; the cell stack has no capacity (charge bar width needs `energy.cellCapacity` on the client, which is the client's default config unless sent).

Config values the design mentions that the config does not have:
- charger charge rate (§5.8), charger fuel capacity, sync rate (§5.6, "4/s"), activity thresholds (§6), step height (§8 2d). Present: `chargerRePerRedstone = 500`, `cellSwapSeconds = 2`, `lowThreshold = 0.10` (the "10 %" sprint cut), `exitHoldSeconds = 1`.
- No file is read; `setConfig` and `ConfigValidation.validate` have no caller; the loader must run before the registry is first loaded (it captures the config, `FrameStateSavedData.java:619`); `FrameState` instances keep the config they were created with.

Behaviour of the game/libraries that the design text does not account for:
- [arch] `EntityEvent.LIVING_HURT` is fired: Fabric — `@Inject` at HEAD of `Player.hurt` (`PlayerAttackInvoker`, both 9.2.14 and 13.0.11), i.e. after `ServerPlayer.hurt`'s invulnerability/spawn-protection checks and **before** Player's difficulty scaling; Forge 1.20.1 — `LivingAttackEvent` from `ForgeHooks.onPlayerAttack` at the head of `Player.hurt` [javap: forge patched `Player.hurt` offset 3], also before difficulty scaling; NeoForge 1.21.1 — `LivingIncomingDamageEvent` from `CommonHooks.onEntityIncomingDamage` inside `LivingEntity.hurt` (javap offset 81), **after** difficulty scaling. On all loaders it fires **before** the vanilla 10-tick `invulnerableTime` handling (javap: `invulnerableTime` read later in `LivingEntity.hurt`) and before armor reduction. So the event's `amount` differs by loader (difficulty) and includes hits that i-frames will absorb or reduce: wear computed from it would over-count rapid hits. Returning `EventResult.interruptFalse()` cancels the hurt on all three.
- Fall damage: cancelling in `LIVING_HURT` leaves `Player.causeFallDamage`'s stat and `LivingEntity.causeFallDamage`'s fall sounds [sound order not checked]; 1.21.1 also has `SAFE_FALL_DISTANCE`/`FALL_DAMAGE_MULTIPLIER` attributes, 1.20.1 does not.
- [javap] Jump counting: `Player.jumpFromGround()` awards `Stats.JUMP` on both versions, and the server calls `ServerPlayer.jumpFromGround()` from `ServerGamePacketListenerImpl.handleMovePlayer` (1.20.1 offset 744, 1.21.1 offset 684), so the server-side statistic increments per jump; read with `player.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP))` (same API both versions). Mock players never go through `handleMovePlayer`.
- Sprinting is decided on the client (`LocalPlayer`), and the server only receives start/stop commands; disabling sprint needs code on both sides and the client needs the server's `canSprint` (design §9 risk). Same for swimming/sinking. Any client-side intervention without an event would be a new mixin, which means editing `check_jars.sh` (e2) lists.
- `SlotGuard` already rewrites the torso summary within one tick whenever any piece's `condition()` changes (`SlotGuard.java:88-115`), so §5.7 "when a piece changes visual state the visual summary is rewritten" is satisfied for worn frames without new code; only the effects recomputation is missing.
- A cell swap moves an item between inventory and registry, so it is structural: `mutate` would not mark it (`FrameStateSavedData.mutateWorn` :364); `update` does and also syncs.
- The 100-tick GameTest limit with a ~60-tick spawn invulnerability on a fresh mock player constrains the damage/fall tests (§8).
- The design says the `EnergyMeter` map is emptied "all'uscita dal server"; `PLAYER_QUIT` exists; the meter's carry is never saved (sub-RE fractions lost at quit — acceptable per design, but a decision).
- Placeholder armor material gives 0 defense, so protection comes only from the `frame_armor` modifier, as the design (§5.7) assumes; but without any `apply` call today a worn frame gives **no protection at all** and the vanilla armor the player wore is stowed — players in 3b are less protected in a frame than without it.
