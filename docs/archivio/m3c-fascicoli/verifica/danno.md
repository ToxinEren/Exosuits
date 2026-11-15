# Dossier "danno": where part 3c can compute piece wear and cancel fall damage

Read-only investigation, 15 Nov. No Gradle, no game run: nothing here was observed at runtime.

**Tags.** [code] file:line in `/home/user/exosuits` (branch `m3c-giocarci` is now `d88e1e0` = `2e35aa6` + two files under `docs/superpowers/specs/ricognizioni/`; `src/` and `tools/` are identical to `2e35aa6`). [javap] `javap -c -p` on one class, with bytecode offsets `@n`. [arch] Architectury sources jar, file:line. [data] file inside a jar. [emul] my IEEE-double emulation of the router. [not checked].
**Jars.** V20 = `minecraft-merged` 1.20.1 (what Fabric 1.20.1 runs); Fo20 = `forge-1.20.1-47.4.29-minecraft-merged`; V21 = `minecraft-merged` 1.21.1 (Fabric 1.21.1); Neo21 = `neoforge-21.1.266-minecraft-merged`. `DS` = `net.minecraft.world.damagesource.DamageSource`, so `(LDS;F)V` stands for `(Lnet/minecraft/world/damagesource/DamageSource;F)V`.
Working files (javap dumps, scripts): `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m3c/verifica/tmp-danno/`.

## 1. Where Architectury fires `EntityEvent.LIVING_HURT` for a server player

The vanilla path of one hit, same on the four variants [javap]: `ServerPlayer.hurt` → `Player.hurt` → `LivingEntity.hurt` → `Player.actuallyHurt` → `LivingEntity.getDamageAfterArmorAbsorb` → `Player.hurtArmor`.

**(a) `ServerPlayer.hurt(LDS;F)Z`** — identical offsets on V20, Fo20, V21, Neo21 [javap]: `@2` `isInvulnerableTo` → false (includes `Player.isInvulnerableTo`: game rules `fallDamage`, `fireDamage`, `drowningDamage`, `freezeDamage` off for the matching tag; changing dimension); `@14-42` `flag = server.isDedicatedServer() && isPvpAllowed() && source.is(IS_FALL)`; `@43-65` `if (!flag && spawnInvulnerableTime > 0 && !source.is(BYPASSES_INVULNERABILITY)) return false`; `@66-145` attacker is a `Player` (or an `AbstractArrow` owned by one) and `!canHarmPlayer` → false; `@149` `return super.hurt`.
**(b) `Player.hurt`** (V20 offsets; V21 has the same statements, `super.hurt@136`; Fo20 the same statements after its hook, about +11, `super.hurt@150`) [javap]: `@2` `isInvulnerableTo`; `@10-31` `abilities.invulnerable && !source.is(BYPASSES_INVULNERABILITY)` → false (creative, spectator); `@32` `noActionTime = 0`; `@38` `isDeadOrDying` → false; `@57` `removeEntitiesOnShoulder()` (server); `@60-125` difficulty scaling if `source.scalesWithDifficulty()` (PEACEFUL → 0, EASY → `min(a/2+1, a)`, HARD → `a*3/2`); `@126-133` `amount == 0` → false; `@137` `super.hurt`. Neo21: same order, scaling is `Math.max(0, source.type().scaling().getScalingFunction().scaleDamage(source, this, amount, difficulty))` `@81`, `super.hurt@103`. `scalesWithDifficulty()` is true for `scaling: always` (explosions) and, for `when_caused_by_living_non_player`, only when the causing entity is a non-player `LivingEntity` [javap `DamageSource`; data `damage_type/*.json`].
**(c) `LivingEntity.hurt`** (V20) [javap]: `@2` `isInvulnerableTo`; `@10-21` client side → false; `@23` dead → false; `@31-52` `IS_FIRE` with Fire Resistance → false; `@87-150` shield: `amount > 0 && isDamageSourceBlocked` → `hurtCurrentlyUsedShield(amount)`, `amount = 0`; `@151-179` freeze ×5 (only `FREEZE_HURTS_EXTRA_TYPES` entities); `@193-276` the i-frame gate: `if (invulnerableTime > 10.0F && !source.is(BYPASSES_COOLDOWN)) { if (amount <= lastHurt) return false; actuallyHurt(source, amount - lastHurt); lastHurt = amount; } else { lastHurt = amount; invulnerableTime = 20; actuallyHurt(source, amount); hurtDuration = hurtTime = 10; }`.

| Variant | Architectury code | Hook in the game | Position |
|---|---|---|---|
| Fabric 1.20.1 (9.2.14) | [arch] `dev/architectury/mixin/fabric/PlayerAttackInvoker.java:31-38`: `@Mixin(Player)`, `@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)`; refmap entry `Lnet/minecraft/class_1657;method_5643(Lnet/minecraft/class_1282;F)Z` | HEAD of `Player.hurt` | after (a), before all of (b) |
| Fabric 1.21.1 (13.0.11) | same file, same lines, same refmap entry | same | same |
| Forge 1.20.1 (9.2.14) | [arch] `dev/architectury/event/forge/EventHandlerImplCommon.java:226-231`: `@SubscribeEvent(priority = HIGH) event(LivingAttackEvent)` | [javap Fo20] `Player.hurt@3` `ForgeHooks.onPlayerAttack(this, source, amount)` posts `LivingAttackEvent`; `LivingEntity.hurt@3` `ForgeHooks.onLivingAttack` returns true without posting when the entity is a `Player` | after (a), before all of (b) |
| NeoForge 1.21.1 (13.0.11) | [arch] `EventHandlerImplCommon.java:227-232`: `event(LivingIncomingDamageEvent)` | [javap Neo21] `LivingEntity.hurt@81` `CommonHooks.onEntityIncomingDamage(this, damageContainers.peek())` | after (a), (b) and the first four checks of (c); before sleeping wake-up, shield, i-frames |

Non-players go through `MixinLivingEntity.java:37-43` on Fabric, which skips `Player` instances (:39), so a player gets exactly one event [arch].

**`amount` at the event:**

| | spawn invulnerability, PvP, game rules | creative `abilities.invulnerable`, dead player | difficulty scaling, `amount == 0` | Fire Resistance | shield | i-frame gate | armor |
|---|---|---|---|---|---|---|---|
| Fabric (both), Forge | already applied | **not yet**: the event fires for creative, spectator and dead players | **not applied**: raw amount, also for hits Peaceful will scale to 0 | not yet | not yet | not yet | not yet |
| NeoForge | applied | applied | applied (scaled amount) | applied | not yet | not yet | not yet |

Also client side: Fabric injects the same handler at HEAD of `LocalPlayer.hurt` and `RemotePlayer.hurt` ([arch] `mixin/fabric/client/ClientPlayerAttackInvoker.java:33-40`); Fo20 `LocalPlayer.hurt@3` and `RemotePlayer.hurt@3` call `ForgeHooks.onPlayerAttack` [javap]; Neo21 leaves both unpatched (`return false` / `return true`) [javap]. A listener must start with `entity instanceof ServerPlayer`.

**What `EventResult.interruptFalse()` does** (`new EventResult(true, false)`; the caller tests `isFalse()`; same class in the four jars [arch `EventResult.java:37,88,153`]):
- Fabric: `cir.setReturnValue(false)` → `Player.hurt` and then `ServerPlayer.hurt` return false. Nothing of (b) has run.
- Forge: `event.setCanceled(true)` → `onPlayerAttack` returns false → `Player.hurt` returns false at `@9-10`. Nothing of (b) has run.
- NeoForge: `event.setCanceled(true)` → `LivingEntity.hurt` returns false at `@87-88`. Already happened: `noActionTime = 0`, `removeEntitiesOnShoulder()` (shoulder parrots dropped), scaling. And `LivingEntity.hurt@53-69` has pushed a `DamageContainer` on `protected java.util.Stack<DamageContainer> damageContainers`, which this return does **not** pop (pops only at `@397` and `@1057`) [javap Neo21; NeoForge patch `patches/net/minecraft/world/entity/LivingEntity.java.patch:174-175` in `neoforge-21.1.266-userdev.jar`]: one stale object per cancelled hit, until that `ServerPlayer` object is replaced. Later hits push on top and read `peek()`, so behaviour stays correct.

**Over-counting if wear were computed in the event** [javap V20, V21]: `Entity.baseTick@247-254` calls `lavaHurt()` every tick in lava, and `lavaHurt` calls `hurt(lava, 4.0F)`; the gate accepts one such hit per 10 ticks → the event counts 10× the hits vanilla applies. The same holds for `BaseFireBlock.entityInside` (`hurt(inFire, fireDamage)`), `CactusBlock.entityInside` (`hurt(cactus, 1.0F)`) and `MagmaBlock.stepOn` (`hurt(hotFloor, 1.0F)`), which call `hurt` on every invocation [that the game invokes them once per tick of contact: not re-read]. Shield-blocked hits count in full on the four variants. On Fabric and Forge a creative-mode pilot in lava would be worn 4 points per tick while taking no damage.

**Reconnaissance §10 (LIVING_HURT):** every statement confirmed (positions, "before i-frames and armor", `interruptFalse` cancels on the three). Added: creative/dead/client-side firing on Fabric and Forge; the NeoForge side effects.

## 2. The vanilla armor-wear point

Declarations, identical on V20/Fo20 and on V21/Neo21 [javap -p]; `ServerPlayer` declares only `hurt` and `isInvulnerableTo` of these:

| Method | Declared in | Overridden by | Descriptor | Visibility |
|---|---|---|---|---|
| `hurt` | `Entity` | `LivingEntity`, `Player`, `ServerPlayer` | `(LDS;F)Z` | public |
| `actuallyHurt` | `LivingEntity` | `Player` | `(LDS;F)V` | protected |
| `getDamageAfterArmorAbsorb` | `LivingEntity` | nobody | `(LDS;F)F` | protected |
| `hurtArmor` | `LivingEntity` (empty body) | `Player` (and `Wolf` on 1.21.1) | `(LDS;F)V` | protected |
| `hurtHelmet` | `LivingEntity` (empty body) | `Player` | `(LDS;F)V` | protected |
| 1.20.1 only: `Inventory.hurtArmor` | `Inventory` | — | `(LDS;F[I)V` | public; `public static final int[] ALL_ARMOR_SLOTS, HELMET_SLOT_ONLY` |
| 1.21.1 only: `doHurtEquipment` | `LivingEntity` | nobody (`Player`, `Wolf` call it) | `(LDS;F[Lnet/minecraft/world/entity/EquipmentSlot;)V` | protected, varargs |

Chain after the i-frame gate [javap]:
- `Player.actuallyHurt(source, x)`: `@0-8` `isInvulnerableTo` → return; then `x = getDamageAfterArmorAbsorb(source, x)` (V20 `@12`, V21 `@12`), then magic absorb, absorption hearts, health.
- `LivingEntity.getDamageAfterArmorAbsorb` — same shape on the four: `@0-7` `if (!source.is(BYPASSES_ARMOR)) {` `@13 hurtArmor(source, x);` then `getArmorValue()` (`@18` V20/Fo20, `@20` V21/Neo21) and `CombatRules.getDamageAfterAbsorb(...)` `}`. So `hurtArmor` runs **before** the armor value of the same hit is read.
- `Player.hurtArmor`: V20/Fo20 `inventory.hurtArmor(source, x, Inventory.ALL_ARMOR_SLOTS)`; V21/Neo21 `doHurtEquipment(source, x, FEET, LEGS, CHEST, HEAD)`.
- V20/Fo20 `Inventory.hurtArmor`: `@0-6` `if (x <= 0) return`; `@7-20` `x /= 4; if (x < 1) x = 1`; per slot: skip if `source.is(IS_FIRE) && item.isFireResistant()`; `if (item instanceof ArmorItem) stack.hurtAndBreak((int) x, player, ...)`.
- V21 `doHurtEquipment`: `@0-6` `if (x <= 0) return`; `@7-17` `int i = (int) Math.max(1.0F, x / 4.0F)`; per slot: `item instanceof ArmorItem && stack.canBeHurtBy(source)` → `stack.hurtAndBreak(i, this, slot)`.

Loader patches [javap + patch files in the userdev jars]:
- **Forge 1.20.1.** `LivingEntity.hurt`: `ForgeHooks.onLivingAttack@3`; shield through `ForgeHooks.onShieldBlock@115` (`ShieldBlockEvent`; `amount -= ev.getBlockedDamage()`, partial blocks possible). `Player.actuallyHurt@8-21`: `x = ForgeHooks.onLivingHurt(this, source, x)` (`LivingHurtEvent`, cancelable, amount settable); `if (x <= 0) return;` then `getDamageAfterArmorAbsorb@25`; `ForgeHooks.onLivingDamage@62` (`LivingDamageEvent`) after absorption. `getDamageAfterArmorAbsorb`, `Player.hurtArmor`, `Inventory.hurtArmor` unpatched.
- **NeoForge 1.21.1.** `LivingEntity.hurt` keeps the hit in a `DamageContainer`: `amount = container.getNewDamage()` `@115-128`; shield through `CommonHooks.onDamageBlock@159` (`LivingShieldBlockEvent`); `container.setNewDamage(amount)` `@335-346`; in the gate `container.setReduction(INVULNERABILITY, lastHurt)` `@403-420` then `actuallyHurt(source, amount - lastHurt)@431`; `invulnerableTime = container.getPostAttackInvulnerabilityTicks()` `@450-464`. `Player.actuallyHurt` **ignores its float parameter** and calls `getDamageAfterArmorAbsorb(source, container.getNewDamage())@49`; `CommonHooks.onLivingDamagePre@86` (`LivingDamageEvent.Pre`), `onLivingDamagePost@295`. `getDamageAfterArmorAbsorb` unpatched. `doHurtEquipment`: `if (x > 0) { int i = (int) max(1, x/4); CommonHooks.onArmorHurt(source, slots, (float) i, this); }` → posts the cancelable `ArmorHurtEvent` (per slot: `i` if `ArmorItem && canBeHurtBy`, else 0), then `stack.hurtAndBreak((int) entry.newDamage, entity, slot)`.

**`x` at `Player.hurtArmor`, on the four variants:** the damage armor is about to reduce — after (a), creative check, difficulty scaling, Fire Resistance, shield, [1.21.1: ×0.75 for `DAMAGES_HELMET` sources, §7], the i-frame gate (only hits that pass; `amount − lastHurt` inside the window), [Forge: after `LivingHurtEvent`]; before armor and toughness, protection enchantments, Resistance, absorption. It is 0.0 after a full shield block: `hurtArmor(source, 0.0F)` is still called on V20, V21 and Neo21 (Fo20 returns earlier), and the `x <= 0` test is inside the callee.
**Only caller:** the name `hurtArmor` occurs only in `LivingEntity`, `Player`, `Inventory` (1.20.1) / `Wolf` (1.21.1) in each of the four jars, loader classes included; `getDamageAfterArmorAbsorb` only in `LivingEntity` and `Player` [constant-pool search, `scripts/refs.py`].
**Vanilla's rate:** each worn `ArmorItem` loses `(int) max(1, x/4)` per accepted hit.

## 3. Mixin injection targets

No event reaches this point uniformly: the four Architectury source jars contain nothing for armor wear, a later hurt phase or falling [arch, grep]; Fabric API has `ServerLivingEntityEvents.ALLOW_DAMAGE` (at `INVOKE isSleeping` in `LivingEntity.hurt`: before shield and i-frames) in both versions and `AFTER_DAMAGE` (at TAIL of `hurt`, with the amounts of the whole hit) only in `fabric-entity-events-v1` 1.8.0 = Fabric API 0.116.18+1.21.1, not in 1.6.1 = 0.92.12+1.20.1 [Fabric API sources]; Forge has `LivingHurtEvent`, NeoForge `ArmorHurtEvent` / `LivingDamageEvent.Pre` (§2).

| | Target (Mojang names, as written in the source) | Forge SRG | Fabric intermediary | Extra test needed |
|---|---|---|---|---|
| **A (recommended)** | `@Mixin(Player.class)`, `@Inject(method = "hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"))`, handler `(DamageSource, float, CallbackInfo)` | `m_6472_` | `method_6105` | `amount > 0` (see §2), `this instanceof ServerPlayer`, a worn frame. `BYPASSES_ARMOR` is already excluded by vanilla |
| B | `@Mixin(LivingEntity.class)`, `method = "getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"`, `at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V")` (one call, `@13` on the four) | `m_21161_`, `m_6472_` | `method_6132`, `method_6105` | as A; runs for every living entity |
| C (not recommended) | `Player.actuallyHurt` `(LDS;F)V` | `m_6475_` | `method_6074` | needs its own `BYPASSES_ARMOR` test; on Neo21 the float parameter is not the value used; on Fo20 HEAD is before `LivingHurtEvent` |
| helmet path (§7) | `@Mixin(Player.class)`, `method = "hurtHelmet(Lnet/minecraft/world/damagesource/DamageSource;F)V"`, HEAD | `m_142642_` | `method_36977` | `amount > 0` |
| fall (§4) | `@Mixin(LivingEntity.class)`, `method = "calculateFallDamage(FF)I"`, RETURN, cancellable | `m_5639_` | `method_23329` | a player wearing the suit; `Player` does not override it on any variant |

SRG names read in the SRG-named Forge jar (`javap -p` of `Player`, `LivingEntity`); intermediary names from `mappings.tiny` of both versions, the same on 1.20.1 and 1.21.1. Other SRG names of the chain: `hurt` `m_6469_`, `causeFallDamage` `m_142535_`, `Inventory.hurtArmor` `m_150072_`.
**One class serves both game versions** for A, B, C, `hurtHelmet` and `calculateFallDamage`: same owner, name and descriptor on V20, Fo20, V21, Neo21 [javap]. Two classes would be needed only to touch `Inventory.hurtArmor` (1.20.1) or `doHurtEquipment` (1.21.1).

**How targets are named per loader today** [code]: the five mixins write Mojang names with full descriptors (`mixin/PlayerSize120Mixin.java:24,30`, `PlayerSize121Mixin.java:24`, `mixin/client/LivingEntityCullingMixin.java:22`, `PlayerRendererMixin.java:27,34-38`, `ItemInHandLayerMixin.java:28-33`). No refmap: Loom's mixin annotation processor is off and `remapJar` rewrites the annotation strings (`build.gradle.kts:106-114`, `docs/compat.md:175`). In the jars built on 14 Nov (`build/libs/all/`, `strings` on the classes): Fabric `method_18377(Lnet/minecraft/class_4050;)Lnet/minecraft/class_4048;` (method and class names intermediary); Forge `m_6972_(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;` (SRG method name, Mojang class names); NeoForge unchanged. `Player.getDimensions` and `LivingEntity.getBoundingBoxForCulling` are overrides of methods declared in `Entity`, like `Player.hurtArmor` (declared in `LivingEntity`): remapping an override target already works.
Configuration: `src/main/resources-mc1.20.1/redstone_exoframes.mixins.json` (`JAVA_17`, `mixins: [PlayerSize120Mixin]`) and `-mc1.21.1/` (`JAVA_21`, `[PlayerSize121Mixin]`), `client` with the three client classes, `required: true`, `injectors.defaultRequire: 1`.

**Adding one common mixin requires:**
1. The class in `mixin/`, package-private and abstract, no directives, each hook one call into `frame/` (the rule in `docs/compat.md:161`). No change to `build.gradle.kts` (the exclusion at :185-186 is only for the two size mixins). The hook can also be one more method of another common `Player` mixin; the `check_jars.sh` entries are per class.
2. Its name in `"mixins"` of **both** json files.
3. `tools/check_jars.sh` (e2): line 168 `if common != [size_mixin]` must accept the new class; `TARGETS` (144-150) gets, for A, `b"hurtArmor("` (neoforge), `b"m_6472_("` (forge), `b"method_6105("` (fabric 1.20.1 and 1.21.1); `MOJANG` (152-153) gets `b"hurtArmor("`, otherwise the NeoForge jar fails with "nomi Mojang attesi nelle annotazioni" (lines 201-210 require a marker in every listed class on NeoForge, none on Fabric/Forge plus a `method_N` / `m_N_` name); the comment at 115-123.
4. `docs/compat.md` "Mixin" (159-176; the name lists at 175) and the design (§5.7 names `LIVING_HURT`; §7 list of mixin exceptions).
5. A wrong target stops the game at start-up (`required`, `defaultRequire 1`); a common mixin is applied on the dedicated and GameTest servers too.
The movement reconnaissance already ran this pattern on the four variants with a common `@Mixin(Player.class)` class in both json files, and found that `check_jars.sh` fails only on the two messages above until updated (`docs/superpowers/specs/ricognizioni/2026-11-15-m3c-movimento-e-dati.md:282-284`). Target A itself has not been run [not checked].

## 4. Fall damage

Server entry: `ServerPlayer.doCheckFallDamage(DDDZ)V` → `checkFallDamage` → `Entity.checkFallDamage` (on ground, `fallDistance > 0`) → `Block.fallOn@13` → `entity.causeFallDamage(fallDistance, 1.0F, damageSources().fall())` [javap V20, V21]. `Entity.checkFallDamage` has no side test, so the local player's client runs the same chain.
- `Player.causeFallDamage(FFLDS;)Z` (public). V20: `@0-11` `abilities.mayfly` → false; `@12-32` `if (dist >= 2) awardStat(Stats.FALL_ONE_CM, round(dist*100))`; `@39` `super`. Fo20: plus `ForgeEventFactory.onPlayerFall` when flying. V21: plus the wind-charge impulse block `@35-100` and `if (result) resetCurrentImpulseContext()`. Neo21: `mayFly()` and `EventHooks.onPlayerFall`.
- `LivingEntity.causeFallDamage` (V20 and V21, same offsets): `@4` `Entity.causeFallDamage`; `@12` `i = calculateFallDamage(dist, mult)`; `if (i > 0) {` `@31 playSound(getFallDamageSound(i), 1, 1);` `@35 playBlockFallSound();` `@43 hurt(source, (float) i);` (result discarded) `return true; }`. Fo20 and Neo21 start with `ForgeHooks/CommonHooks.onLivingFall@3` (`LivingFallEvent`, cancelable: `null` → `return false` before any sound).
- `calculateFallDamage(FF)I` (protected): V20/Fo20 `ceil((dist - 3.0F - jumpBoostLevel) * mult)`; V21/Neo21 `ceil((double) ((dist - (float) SAFE_FALL_DISTANCE) * mult) * FALL_DAMAGE_MULTIPLIER)` (two attributes that 1.20.1 does not have).

**If `hurt` is cancelled by the event, still happening:** the `FALL_ONE_CM` statistic; `PLAYER_SMALL_FALL` (`i <= 4`) or `PLAYER_BIG_FALL` (`Player.getFallSounds`); the block's fall sound (`SoundType.getFallSound()`, volume ×0.5, pitch ×0.75); `causeFallDamage` returns true. `Player.playSound` is `level().playSound(this, x, y, z, ...)` [javap]: other players get it from the server, the falling player from the local run of the same code [that the server overload skips `this`: not checked]. Not happening: damage, hurt sound and animation, i-frames, `DAMAGE_TAKEN`, exhaustion. On NeoForge also the side effects of §1.
During the first 60 ticks of a `ServerPlayer` object the fall never reaches the event, except on a dedicated server with PvP allowed (§1 a).

**Damage-type tags** [data, `data/minecraft/tags/damage_type/`], identical in 1.20.1 and 1.21.1 unless said:
- `bypasses_armor`: on_fire, in_wall, cramming, drown, fly_into_wall, generic, wither, dragon_breath, starve, fall, freeze, stalagmite, magic, indirect_magic, out_of_world, generic_kill, sonic_boom, outside_border. NeoForge's own data pack adds `#neoforge:is_poison` [data Neo21].
- `bypasses_cooldown`: **no file in either version** (empty tag: no vanilla source skips the i-frame gate).
- `bypasses_invulnerability`: out_of_world, generic_kill. `is_fall`: fall, stalagmite. `damages_helmet`: falling_anvil, falling_block, falling_stalactite. `bypasses_shield`: `#bypasses_armor`, falling_anvil, falling_stalactite.
- `is_projectile`: arrow, trident, mob_projectile, unattributed_fireball, fireball, wither_skull, thrown; 1.21.1 adds wind_charge.
- For reference `is_fire`: in_fire, on_fire, lava, hot_floor, unattributed_fireball, fireball (+ campfire in 1.21.1): lava, fire blocks and magma do **not** bypass armor, burning (`on_fire`) does.

**Fall damage never reaches the armor-wear point:** `fall` and `stalagmite` are in `bypasses_armor`, so `getDamageAfterArmorAbsorb@0-7` skips `hurtArmor`. Two paths produce `IS_FALL` damage: `causeFallDamage` (falls; `PointedDripstoneBlock` with `stalagmite`) and the direct call `ThrownEnderpearl.onHit` → `hurt(damageSources().fall(), 5.0F)` (V20 `@273-278`, V21 `@279-284`). An `IS_FALL` test in `LIVING_HURT` cancels both; a `calculateFallDamage` hook cancels only the first.
**Reconnaissance §10 (fall):** confirmed; its "[sound order not checked]" is now checked: both sounds come before `hurt`.

## 5. The mod's own damage code

`DamageRouter.route(DamageKind kind, int amount, Map<PieceSlot, Piece> mounted, Map<PieceSlot, Integer> carry)` [code `core/armor/DamageRouter.java:116-139`]:
1. `Objects.requireNonNull(carry, "carry")`; `amount < 0` → `IllegalArgumentException` (:117-118, :210-214).
2. Recipients: slots in `PieceSlot` order whose piece is mounted and not broken (:149-155); primaries HEAD → {HELMET}, FROM_ABOVE → {HELMET, TORSO}, FALL → {LEFT_LEG, RIGHT_LEG}, GENERIC → {} (:216-223). No recipient → `new Split(empty, amount)`, carry map untouched (:120-122).
3. Exact quotas in points, as doubles (:160-179): `primaryPart = primary.isEmpty() ? 0 : (others.isEmpty() ? amount : amount * primaryShare)`; `otherPart = amount - primaryPart`; each primary `primaryPart / primary.size()`; each other `otherPart * share / (sum of the others' shares)`, equal parts if that sum is 0.
4. Every quota × 1000, then `largestRemainder(milliQuotas, (long) amount * 1000)` (:123-127, :187-208): `floor(q + 1e-9)` each, the missing units one at a time to the largest remainder (within 1e-9 the lower index wins). The sum is exactly `amount * 1000`.
5. For every recipient (:129-137): `v = (carry.get(slot), null or absent = 0) + milli`; `carry.put(slot, (int) (v % 1000))` — always, also when the value is 0; `if (v >= 1000) perPiece.put(slot, Math.toIntExact(v / 1000))`.
6. `return new Split(perPiece, 0)`. Slots that are not recipients keep their carry entry as it is.
Whole points enter only as `amount`; fractions live only in the carry. The router never looks at remaining durability: the caller applies `perPiece` with `Piece.damage(int)` (`Piece.java:66-73`, removes `min(amount, durability)` and returns it), so wear beyond a piece's durability is lost.
`FrameState.damageCarryMilli()` (`core/frame/FrameState.java:177-179`) returns the live `EnumMap`; `mount` (:85) and `unmount` (:97) drop the slot's entry; the codec writes `carry_milli` per mounted piece (`compat/impl/FrameStateCodec.java:59`) and reads it clamped (:127-131). `state.pieces()` is a read-only view of the live `Piece` objects (:73-75).

**What `routeMilli(kind, amountMilli, mounted, carry)` must do** so that `route(kind, n, m, c)` equals `routeMilli(kind, n * 1000, m, c)`:
- same checks and exceptions, same recipients and order, same formulas of step 3 fed with `amountMilli` (quotas come out in thousandths), `largestRemainder(quotas, amountMilli)`, same step 5 (an entry written for **every** recipient, 0 included: `DamageRouterTest.java:168` asserts `carry.size() == 6` after each hit; non-recipients untouched: :180, :186; a `null` value read as 0 and replaced: :249-253), `perPiece` in whole points.
- **`unabsorbed` needs a decision.** `route` returns it in points (`DamageRouterTest.java:184-185` expects 3 for `route(GENERIC, 3, Map.of(), carry)`). For `Split` equality the milli overload must return `amountMilli / 1000`, which reports 0 for a hit under one point when no piece can absorb; or it returns its own record with `unabsorbedMilli` and `route` divides by 1000.
- **Make `route(…, carry)` delegate to `routeMilli`.** Computing the quotas from the milli amount instead of "points, then × 1000" is not bit-identical for large amounts [emul, `scripts/router_sim.py`, `router_sim2.py`]: 0 differences over 907,956 cases (3 share sets × 4 kinds × the 63 non-empty sets of mounted slots × n = 0..1200); in 400,000 random cases with n = 1201..200,000 about 1 % differ, by one thousandth moved between two slots, the smallest such n found being 7736 (my explanation: a quota above 2^22 has an ulp larger than the 1e-9 tolerance). With delegation the equality holds by construction, and the 14 existing tests keep their numbers (their amounts are 0..7 and 200; the three-argument `route` is not touched).
- The sum of the thousandths handed out is exactly `amountMilli` in 200,000 random fractional cases [emul].
- Caller side: `Math.round((double) amount * 1000.0)` (a `long`), skip unless `amount > 0`, clamp before narrowing to `int` (a command can deal `Float.MAX_VALUE` of a non-bypassing type).

**Reconnaissance §1.5, §1.6, §3.2:** confirmed line by line (signatures, the six carry tests, 14 tests in `DamageRouterTest`, `Compat.damage()` at `compat/Compat.java:41` and `DamageRouter` without callers in `src/main/java`). Two additions on `DamageClassifierImpl.classify` (`compat/impl/DamageClassifierImpl.java:32-52`): `getSourcePosition()` is the explicit position or the **direct entity's `position()`**, i.e. its feet [javap `DamageSource`], so a melee attacker on the wearer's level is never HEAD (needs y ≥ wearer's feet + 2.42 with the suit); and an anvil is GENERIC (§7).

## 6. The four placeholder armor items are never damaged by vanilla

[code] `content/ModItems.java:44-51` builds them with `new Item.Properties()`; `compat/impl/ArmorMaterialsImpl.java:89-91` (1.20.1) `new ArmorItem(FrameMaterial.FRAME, type, props)` with `getDurabilityForType` → 0 (:98-101); :74-76 (1.21.1) `new ArmorItem(FRAME, type, props.stacksTo(1))`.
- V20 [javap]: `ArmorItem.<init>@4-12` `super(props.defaultDurability(material.getDurabilityForType(type)))` → `maxDamage` stays 0; `ItemStack.isDamageableItem` = `!isEmpty() && getItem().getMaxDamage() > 0 && !Unbreakable`; `hurtAndBreak(ILLivingEntity;LConsumer;)V` returns at `@32-38`.
- Fo20 [javap]: `isDamageableItem` = `!isEmpty() && getItem().isDamageable(this)`; `IForgeItem.isDamageable` = `Item.canBeDepleted()` = `maxDamage > 0` → false.
- V21 [javap]: `ArmorItem.<init>@2` is `super(props)`, no durability from the material; `isDamageableItem` = `has(MAX_DAMAGE) && !has(UNBREAKABLE) && has(DAMAGE)`; the mod never calls `durability(...)` → false; `hurtAndBreak(int, ServerLevel, ServerPlayer, Consumer)` returns at `@1-7`.
- Neo21 [javap]: same test in `hurtAndBreak(int, ServerLevel, LivingEntity, Consumer)@1-4`. `ArmorHurtEvent` is still posted with the four placeholders on every armor-wearing hit; the damage then does nothing.
They are `ArmorItem`s, so vanilla does call `hurtAndBreak` on them: the `Player.hurtArmor` path runs normally with the suit on.

## 7. `DAMAGES_HELMET` sources: the separate `hurtHelmet` path

`if (source.is(DAMAGES_HELMET) && !getItemBySlot(EquipmentSlot.HEAD).isEmpty()) { hurtHelmet(source, amount); amount *= 0.75F; }` inside `LivingEntity.hurt` [javap]:
- **1.20.1** (V20 `@279-313`, Fo20 `@335-368`): **after** the i-frame gate and `actuallyHurt`. It runs only for hits that passed the gate, with the full post-shield amount (not the delta), and the ×0.75 no longer changes the damage of that hit.
- **1.21.1** (V21 `@180-214`, Neo21 `@300-334`): **before** the gate. It runs also for hits the gate then rejects, and the ×0.75 reduces what reaches `actuallyHurt` and `hurtArmor`.
- `Player.hurtHelmet`: V20/Fo20 `inventory.hurtArmor(source, amount, Inventory.HELMET_SLOT_ONLY)`; V21/Neo21 `doHurtEquipment(source, amount, EquipmentSlot.HEAD)`: the same "÷ 4, at least 1" on the helmet only. These three types are not in `bypasses_armor`, so the same hit also goes through `hurtArmor`.
- With a frame worn the head slot always holds `exoframe_helmet`, whatever pieces are mounted [code `frame/FrameEntry.java:221-231`, `frame/Placeholders.java:35-41`]: on 1.21.1 every pilot takes 75 % from anvils, falling blocks and stalactites; on 1.20.1 100 %.
- Where the source is: `FallingBlockEntity.causeFallDamage` (same offsets V20/V21) runs when the block **lands**, hurting `level().getEntities(this, getBoundingBox(), ...)` `@109-126` with `Fallable.getFallDamageSource(this)` or `damageSources().fallingBlock(this)`: the direct entity is the falling block on the ground, so `getSourcePosition().y` is about the victim's feet, and `classify` answers GENERIC, not HEAD.

## 8. What this changes for the plan of part 3c

1. **Wear does not go in `LIVING_HURT`.** There it would count hits the i-frame gate drops (10× in lava, fire blocks, cactus, magma), shield-blocked hits, unscaled amounts on Fabric and Forge against scaled ones on NeoForge, and creative-mode pilots on Fabric and Forge. Put it in one new common mixin, target A of §3: `@Mixin(Player.class)`, `@Inject(method = "hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"))`, one call into `frame/`. Guards there: `player instanceof ServerPlayer`, `amount > 0`; then `DamageClassifier.classify`, `routeMilli`, `Piece.damage`, all inside `FrameStateAccess.mutate`. The `bypassesArmor` test of design §5.7 becomes unnecessary (vanilla has already made it). Design §5.7 and §7 need the amendment; `check_jars.sh` and `docs/compat.md` as in §3.
2. **Fall: `LIVING_HURT` with `isFall(source)` works on the four variants** (the hit is refused before any damage), with a `ServerPlayer` guard. Its costs: the fall-damage sounds still play; on NeoForge a stale `DamageContainer` per cancelled fall and shoulder parrots dropped. Since a `mixin/` class is being added anyway, a second hook `LivingEntity.calculateFallDamage(FF)I` at RETURN, answering 0 for a player with the suit (`SuitSize.wearsSuit`, `frame/SuitSize.java:59-64`, valid on both sides), removes sounds and side effects for real falls; the event stays as the net for direct `fall` hits (ender pearl). This is one more mixin target for the design's §7 list of builder's choices.
3. **Router:** add `routeMilli`, make `route(kind, int, mounted, carry)` delegate to it, decide the unit of `unabsorbed` (§5), add tests for fractional amounts and for the equality on multiples of 1000.
4. **Classifier:** fall never arrives at the wear hook, so `DamageKind.FALL` is unused in M3 unless the plan decides that a cancelled fall wears the legs (the design says it does not). For anvils and stalactites, add "`source.is(DamageTypeTags.DAMAGES_HELMET)` → HEAD" before the geometric tests, or they are routed as GENERIC.
5. **Recompute effects after the hit, not inside the hook.** `hurtArmor` runs before `getArmorValue()` of the same hit (§2): changing the `frame_armor` modifier inside the hook would remove a breaking piece's protection from the hit that breaks it. The per-player tick can pick it up.
6. **Placeholders:** nothing to do (§6).
7. **GameTests** ("usura dei pezzi da un danno noto", "caduta senza danno"): a fresh `ServerPlayer` refuses damage for 60 ticks (`spawnInvulnerableTime`, private, set at `<init>@74-76`, decremented in `ServerPlayer.tick@16-21`); `GameTestServer.isDedicatedServer()` is false on both versions, so falls are refused too [javap]. The mock must be in survival (`Player.hurt` reads `abilities.invulnerable`, not `isCreative()`). A second hit within 10 ticks is gated unless the test resets the public field `Entity.invulnerableTime`, which for a `ServerPlayer` only `ServerPlayer.tick@25-38` decrements. Use a source that does not scale with difficulty (`damageSources().cactus()`). `causeFallDamage` returns true even when the hit is cancelled: assert on health. A fall test run inside the first 60 ticks passes without the mod. With 100 ticks per test and up to 20 spent waiting for entities (`gametest/ExoframeGameTests.java:150,152`), the alternative is to call the `frame/` handler directly and keep one test that goes through `hurt`.

**Open points**
- Target A has not been compiled or run on any variant (no Gradle allowed here). Evidence: the four jars' signatures, the override-remap precedent in the built jars, and the movement reconnaissance's common `Player` mixin.
- Not observed: that a mock player is ticked by the level from its first tick, so that `spawnInvulnerableTime` really expires inside a test (`ServerLevel.tickNonPassenger@44` calls `Entity.tick()` for any entity [javap]).
- Decisions for the plan: whether a pilot takes ender-pearl and stalagmite damage; whether cancelled falls wear the legs; the unit of `unabsorbed`; whether the 75 % of §7 on 1.21.1 only is acceptable; whether an exception in the wear handler may propagate into `hurt` (`FrameEntry.onDeath` catches `RuntimeException`).
- Other mods can change `x` before `hurtArmor` on Forge (`LivingHurtEvent`) and NeoForge (`LivingIncomingDamageEvent`, reduction functions); the hook then sees what vanilla's own armor wear sees.
