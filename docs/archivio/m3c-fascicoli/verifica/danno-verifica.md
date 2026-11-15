# Verification of dossier "danno" (damage path, wear hook, fall damage)

Read-only. No Gradle, no game run. All evidence re-derived independently; working dumps in
`/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m3c/verifica/tmp-danno-v/`.

Jars: V20 = vanilla merged 1.20.1; FO = forge-1.20.1-47.4.29-minecraft-merged (Mojang names); FOS = its SRG copy;
V21 = vanilla merged 1.21.1; NEO = neoforge-21.1.266-minecraft-merged; I20/I21 = minecraft-merged-intermediary.
Arch sources: architectury-fabric 9.2.14 / 13.0.11, architectury-forge 9.2.14, architectury-neoforge 13.0.11 (-sources.jar),
plus the binary fabric jars (refmaps). Repo HEAD is `d88e1e0` (= `2e35aa6` + docs); `src/` and `tools/` read there.

Summary: 13 of 14 claims confirmed on all variants they name; C12 is refuted only in its "smallest n" detail
(conclusion unchanged). Nothing found that changes the dossier's recommendations; eight additions under "Missed".

---

## C1-fabric-event-point — CONFIRMED (both 9.2.14 and 13.0.11)
- [arch] `dev/architectury/mixin/fabric/PlayerAttackInvoker.java:31-38` identical in both sources jars: `@Mixin(value = {class_1657.class})`,
  `@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)`, body: `if (LIVING_HURT.invoker().hurt(...).isFalse() && this instanceof class_1657) cir.setReturnValue(false)`.
- Refmap (binary jars, `architectury-fabric-refmap.json`), both versions: `PlayerAttackInvoker.hurt -> Lnet/minecraft/class_1657;method_5643(Lnet/minecraft/class_1282;F)Z`.
  Listed in `architectury.mixins.json` "mixins" (common). `MixinLivingEntity` (same file set, `hurt` HEAD on class_1309) returns immediately for `class_1657`: one event per player hit.
- [javap V20, V21] `ServerPlayer.hurt`: @2 isInvulnerableTo, @14-31 isDedicatedServer/isPvpAllowed/IS_FALL, @48-58 spawnInvulnerableTime/BYPASSES_INVULNERABILITY, @90/@138 canHarmPlayer, @149 `invokespecial Player.hurt`. So the HEAD of `Player.hurt` is after all of those.
- [javap V20, V21] `Player.hurt` after HEAD: @2 isInvulnerableTo, @11-31 abilities.invulnerable, @34 noActionTime, @38 isDeadOrDying, @57 removeEntitiesOnShoulder, @61 scalesWithDifficulty..., @127-133 `== 0` return, @136/137 super.hurt. `Player.isInvulnerableTo` (checked on all four) does NOT read abilities, so creative players do reach the HEAD.
- [arch] `EventResult.interruptFalse()` returns `FALSE = new EventResult(true, false)` (:37, :88), `isFalse()` = `BooleanUtils.isFalse(value)` (:153).

## C2-forge-event-point — CONFIRMED
- [arch] forge 9.2.14 `EventHandlerImplCommon.java:226-231`: `@SubscribeEvent(priority = EventPriority.HIGH) public static void event(LivingAttackEvent event)` → `setCanceled(true)` on `isFalse()`; import `net.minecraftforge.event.entity.living.LivingAttackEvent` (:44).
- [javap FO `Player.hurt`] @3 `ForgeHooks.onPlayerAttack(LivingEntity,DamageSource,F)Z`, @6 `ifne 11`, @9-10 `iconst_0; ireturn`; then the vanilla statements shifted (+11): isInvulnerableTo @13, abilities.invulnerable @22-42, scaling @72-137, super.hurt @150.
- [javap FO `ForgeHooks`] `onPlayerAttack`: posts `new LivingAttackEvent(entity, source, amount)`, returns `!post(...)`. `onLivingAttack`: @1 `instanceof Player`, @4 `ifne 28` → returns true without posting. [javap FO `LivingEntity.hurt`] @3 `onLivingAttack`.

## C3-neoforge-event-point — CONFIRMED
- [arch] neoforge 13.0.11 `EventHandlerImplCommon.java:227-232`: `event(LivingIncomingDamageEvent)` (import :46), priority HIGH.
- [javap NEO `LivingEntity.hurt`] @2 isInvulnerableTo, @10-21 isClientSide → false, @23 isDeadOrDying, @31-52 IS_FIRE + FIRE_RESISTANCE, @53-69 `damageContainers.push(new DamageContainer(source, amount))`, @70-81 `CommonHooks.onEntityIncomingDamage(this, peek())`, @84-88 `ifeq 89; iconst_0; ireturn`, then @90 isSleeping/stopSleeping, @115-128 `amount = peek().getNewDamage()`, shield @156-159, gate @363-.
- Field: `protected java.util.Stack<net.neoforged.neoforge.common.damagesource.DamageContainer> damageContainers;`. `Stack.pop` occurs only at @397 and @1057 of `hurt` (whole-class grep: no other pop/clear in LivingEntity, Player, ServerPlayer).
- [javap NEO `Player.hurt`] creative @11-31, removeEntitiesOnShoulder @57, scaling @62-86 (`IScalingFunction.scaleDamage`, `Math.max`), super.hurt @103 — all before the event.
- [userdev `neoforge-21.1.266-userdev.jar`, `patches/net/minecraft/world/entity/LivingEntity.java.patch`] lines 174-175 are exactly the push and the `if (onEntityIncomingDamage(...)) return false;`.

## C4-event-overcounts — CONFIRMED
- Gate [javap]: V20 @194-262, FO @249-317, V21 @229-297, NEO @363-470: `invulnerableTime > 10.0F && !is(BYPASSES_COOLDOWN)` → `amount <= lastHurt` return false / `actuallyHurt(source, amount - lastHurt)`; else `lastHurt = amount; invulnerableTime = 20; actuallyHurt(source, amount)`. Shield before it on all four (V20/V21 @95-103, FO @106-138 via `onShieldBlock`, NEO @156-159 via `onDamageBlock`). The event point (C1-C3) precedes both.
- [javap E.* all four] `Entity.baseTick` @247/250-254/257 `isInLava` → `lavaHurt()`; `lavaHurt` → `hurt(damageSources().lava(), 4.0F)` (V20 @25, FO @24, V21 @26, NEO @25).
- Window length: `invulnerableTime` for a ServerPlayer is decremented only by `ServerPlayer.tick@24-38` (LivingEntity.baseTick @612-629 skips `instanceof ServerPlayer`, V20; same exclusion pattern in the other three, decrement sites listed in `LE.*`/`SP.*`). One decrement per tick → one accepted lava hit per 10 ticks, the event every tick: 10x. [inferred from bytecode, not run]
- Client: [javap] V20/V21/NEO `LocalPlayer.hurt` = `return false`, `RemotePlayer.hurt` = `return true` (no hook); FO both call `ForgeHooks.onPlayerAttack` and `pop` the result. Fabric: `client.ClientPlayerAttackInvoker` (`@Mixin({class_746, class_745})`, HEAD of hurt) in the "client" list of `architectury.mixins.json`. NEO's `LivingEntity.hurt` returns at @10-21 on the client before the event.

## C5-wear-point — CONFIRMED
- [javap -p] `protected void hurtArmor(DamageSource, float)` declared in `Player` on V20, FO, V21, NEO; `LivingEntity.hurtArmor` body is `0: return` on all four.
- Only call: `LivingEntity.getDamageAfterArmorAbsorb` @13 `invokevirtual hurtArmor`, inside `@0-7 if (!source.is(BYPASSES_ARMOR))`, on all four (callers of hurtArmor/getDamageAfterArmorAbsorb grepped in LE/Player/ServerPlayer; getDamageAfterArmorAbsorb called from `Player.actuallyHurt` @12 V20/V21, @25 FO, @49 NEO).
- Constant-pool scan of every class in the four jars (python zipfile, substring `hurtArmor`): V20/FO: LivingEntity, Inventory, Player; V21/NEO: Wolf, Player, LivingEntity. FO and NEO jars include the loader classes (ForgeHooks, CommonHooks present).

## C6-amount-at-wear-point — CONFIRMED
- NEO `Player.actuallyHurt`: @9-49 `getDamageAfterArmorAbsorb(source, damageContainers.peek().getNewDamage())`; the float parameter is not read there. [javap NEO `DamageContainer.setReduction`] calls `modifyReduction` then `newDamage -= reduction` — so in the gate `setReduction(INVULNERABILITY, lastHurt)` makes `getNewDamage()` = amount − lastHurt (absent mod reduction functions). Same value as vanilla's delta.
- Zero after a full block: V20 `Inventory.hurtArmor` @0-6 `x <= 0 → return`; FO `Inventory.hurtArmor` @1-3 `ifle 115` (return); V21 `doHurtEquipment` @0-6 return; NEO `doHurtEquipment` @1-3 `ifle 28`. FO `Player.actuallyHurt` @11-21: `x = ForgeHooks.onLivingHurt(...)`, `x <= 0 → return` before getDamageAfterArmorAbsorb @25.
- Rate: 1.20.1 `x /= 4.0F; if (x < 1) x = 1; hurtAndBreak((int) x, ...)` per ArmorItem (fire-resistant items skipped for IS_FIRE); 1.21.1 `(int) Math.max(1.0F, x / 4.0F)`, per `ArmorItem && canBeHurtBy(source)`; NEO passes `(float) i` to `CommonHooks.onArmorHurt`.

## C7-one-mixin-both-versions — CONFIRMED (names); remap behaviour inferred, not run
- Same owner/name/descriptor/visibility on V20, FO, V21, NEO (above).
- [mappings] `fabric-loom/1.20.1/...forge-1.20.1-47.4.29/mappings-srg.tiny`: `(Lben;F)V b m_6472_ method_6105 hurtArmor`; FOS `javap -p Player`: `protected void m_6472_(DamageSource, float)`. Intermediary `method_6105` in both `loom.mappings.1_20_1...` and `1_21_1...` `mappings.tiny`; I20 and I21 `class_1657` declare `protected void method_6105(class_1282, float)`.
- Precedent: built jars in `versions/1.20.1-forge/build/libs/` and `versions/1.20.1-fabric/build/libs/`: `PlayerSize120Mixin.class` carries `m_6972_(Lnet/minecraft/world/entity/Pose;)...` (Forge: SRG method, Mojang classes) and `method_18377(Lnet/minecraft/class_4050;)Lnet/minecraft/class_4048;` (Fabric). The new target was not compiled [not checked].

## C8-adding-a-common-mixin — CONFIRMED
- Both json files: `"mixins": ["PlayerSize120Mixin"]` / `["PlayerSize121Mixin"]`, `defaultRequire: 1`, `required: true`.
- `tools/check_jars.sh`: :144-150 `TARGETS`, :152-153 `MOJANG`, :168 `if common != [size_mixin]`, :196-210 per-class Mojang/intermediary/SRG marker test. `build.gradle.kts:185-186` excludes only the other size mixin. No other tool lists mixin classes (`check_sources.sh` rules touch only `mixin.client`; `check_log.py` only mixin-apply errors). The MOJANG marker `b"hurtArmor("` matches only the annotation string (a handler or callee named `...hurtArmor` has no `(` in its constant-pool entry).

## C9-fall-cancel-leftovers — CONFIRMED (all four)
- `LivingEntity.causeFallDamage`: V20/V21 @12 calculateFallDamage, @19 `ifle 49`, @26-31 `playSound(getFallDamageSound(i),1,1)`, @35 `playBlockFallSound`, @43 `hurt` + @46 `pop`, @47-48 `return true`. FO/NEO: same at @37/@44/@56/@60/@68/@71, after `onLivingFall@3` (null → return false @14).
- `Player.causeFallDamage`: `awardStat(Stats.FALL_ONE_CM, round(dist*100))` (V20 @19-32, FO @25-38, V21 @19-32, NEO @22-35) before super; `ServerPlayer` does not override it (all four).
- `getFallDamageSound`: `i > 4` → big else small (`iconst_4; if_icmple`), `Player.getFallSounds` = `(PLAYER_SMALL_FALL, PLAYER_BIG_FALL)` on all four.
- Extra: on V21/NEO the `true` return also triggers `resetCurrentImpulseContext()` (V21 @112-116, NEO @115-119).

## C10-damage-type-tags — CONFIRMED
- [data V20, V21] values exactly as claimed for is_fall, bypasses_armor (18 entries incl. fall, stalagmite), damages_helmet, bypasses_invulnerability, bypasses_shield; no `bypasses_cooldown.json` in either jar; is_projectile differs only by `minecraft:wind_charge` in 1.21.1.
- Loaders: Forge's vanilla data (`fabric-loom/1.20.1/forge/1.20.1-47.4.29/client-extra.jar`) identical for these six tags; Forge universal adds no damage_type tags; NEO adds `bypasses_armor: ["#neoforge:is_poison"]` (= `neoforge:poison`, optional `#forge:is_poison`).
- Ender pearl [javap `ThrownEnderpearl.onHit`]: V20 @273-278 and V21 @279-284 `hurt(damageSources().fall(), 5.0F)` directly. Nuance: FO @150-330 and NEO @165-313 pass 5.0F into `onEnderPearlLand` and hurt with `EntityTeleportEvent$EnderPearl.getAttackDamage()` (default 5.0, changeable by mods). Not through causeFallDamage on any variant.

## C11-calculate-fall-damage-target — CONFIRMED
- `protected int calculateFallDamage(float, float)` in LivingEntity on all four; SRG `m_5639_` (FOS javap + mappings-srg.tiny), intermediary `method_23329` (both mappings.tiny; I20/I21 class_1309). Constant-pool scan: name in LivingEntity and animal overrides only (V20/FO: Fox, Frog, Goat, Llama, AbstractHorse; V21/NEO: Frog, Goat, Llama, AbstractHorse) — never Player/ServerPlayer.
- Body: V20/FO `MobEffects.JUMP` amplifier and `3.0f`, `Mth.ceil(F)`; V21/NEO `Attributes.SAFE_FALL_DISTANCE`, `FALL_DAMAGE_MULTIPLIER`, `Mth.ceil(D)`. All four first return 0 for `EntityTypeTags.FALL_DAMAGE_IMMUNE`.

## C12-router-milli-overload — REFUTED in one detail (smallest differing n); rest confirmed
- [code] `DamageRouter.java:116-139` behaves exactly as described (quotas ×1000 :123-126, `largestRemainder(milliQuotas, (long) amount * MILLI)` :127, carry put for every recipient :133, `v / MILLI` :135, `Split(empty, amount)` without touching carry :120-122).
- My own IEEE-double emulation (`tmp-danno-v/py/rt.py`, written from the Java source, not from the dossier's scripts), two share sets (config defaults 0.15/0.30/0.125/0.125/0.15/0.15 and the test set 0.10/0.50/0.05/0.05/0.15/0.15 of `DamageRouterTest.java:106-111`), primaryShare 0.6, 4 kinds × 63 subsets:
  - n = 0..1200: 605,304 cases, 0 differences (confirms).
  - exhaustive n = 1201..8000: 493 differences; the smallest is **n = 6145** (test shares, kind HEAD, mounted {TORSO, LEFT_ARM, RIGHT_ARM}: [5120833, 512084, 512083] vs [5120834, 512083, 512083]); with the default shares none up to 8000. So "smallest found: 7736" is not the threshold. The conclusion (route must delegate to the milli overload) stands and is reinforced.

## C13-placeholders-undamageable — CONFIRMED
- [code] `ModItems.java:44-51` `new Item.Properties()`; `ArmorMaterialsImpl.java:74-76` (1.21.1) `props.stacksTo(1)`, :89-91/:98-101 (1.20.1) durability 0.
- [javap V20/FO] `ArmorItem.<init>` @4-12 `defaultDurability(material.getDurabilityForType(type))`; `Item$Properties.defaultDurability`: `maxDamage == 0 ? durability(i) : this` → stays 0. V20 `isDamageableItem` uses `Item.getMaxDamage()`; FO uses `Item.isDamageable(ItemStack)` = `IForgeItem` default `canBeDepleted()` = `maxDamage > 0`. FO `hurtAndBreak` tests `isDamageableItem` @31 before `Item.damageItem` @45.
- [javap V21/NEO] `ArmorItem.<init>` @2 `super(props)`; `isDamageableItem` = has(MAX_DAMAGE) && !has(UNBREAKABLE) && has(DAMAGE). NEO `CommonHooks.onArmorHurt` posts `ArmorHurtEvent` unconditionally (entries for `ArmorItem && canBeHurtBy`).

## C14-helmet-path — CONFIRMED
- [javap] DAMAGES_HELMET getstatic / hurtHelmet / 0.75f: V20 @280/@305/@309 (after gate's actuallyHurt @262), FO @335/@360/@364 (after @317), V21 @181/@206/@210 (before gate @229), NEO @301/@326 (before gate @363, before `setNewDamage` @346).
- [code] `FrameEntry.dress` (`frame/FrameEntry.java:221-231`) puts `Placeholders.create(slot, …)` in all four `ARMOR_SLOTS` (`Placeholders.java:35-41`: HEAD → EXOFRAME_HELMET).
- [javap V20/V21] `DamageSource.getSourcePosition()` = `damageSourcePosition` else `directEntity.position()`; `FallingBlockEntity.causeFallDamage` @64/@77 `Fallable.getFallDamageSource(this)` / `fallingBlock(this)`, @109-114 `level.getEntities(this, getBoundingBox(), …)`. Direct entity = the landed falling block → y at the victim's feet level → `DamageClassifierImpl.classify` (:48 needs `y >= eyeY - 0.25`) answers GENERIC.

## Reconnaissance (stato-del-codice.md)
- §10 statements on LIVING_HURT (positions per loader, before i-frames and armor, interruptFalse cancels) and on fall (stat and sounds left over; 1.21.1 attributes): confirmed by C1-C4, C9, C11. §1.5 and §3.2 (router signatures, no milli overload, classifier rules, no callers): confirmed against the code.

---

## Missed

1. **GameTest mock players never run `Player.tick` (so no natural lava, fire-block or fall damage).** [javap V21 `GameTestHelper.makeMockServerPlayerInLevel` @46-87: a fresh `Connection` handed to `PlayerList.placeNewPlayer`; V20 same shape @43-54] `ServerPlayer.doTick()` (the only caller of `Player.tick` → `baseTick`/`lavaHurt`/movement/`checkFallDamage`, at `doTick@15`) is invoked only from `ServerGamePacketListenerImpl.tick@78`, which runs from `Connection.tick`, which only `ServerConnectionListener.tick` calls for its own `connections` list. The mock's connection is not in it [inferred; consistent with the repo's own note `gametest/ExoframeGameTests.java:136` that the server does not advance the mock player's tick counter]. The tests "caduta senza danno" and any lava/contact wear test must call `causeFallDamage(...)` / `hurt(...)` directly; standing the mock in lava or dropping it does nothing.
2. **NeoForge stack leak scales with per-tick cancellation.** No `clear`/extra `pop` of `damageContainers` exists in LivingEntity, Player or ServerPlayer (NEO). Cancelling a fall is rare, but if the plan also cancels a per-tick source in `LIVING_HURT` (e.g. lava for the `FIRE_IMMUNE_LAVA_WALK` tier trait), NeoForge keeps one `DamageContainer` per tick per player for the lifetime of the `ServerPlayer` object (20/s). Such sources should be blocked earlier (e.g. `isInvulnerableTo`-level) or not via the event on NeoForge.
3. **Do not hook `hurtHelmet` and `hurtArmor` both for wear.** DAMAGES_HELMET types are not in `bypasses_armor`, so the same hit reaches `hurtArmor` too; on 1.21.1 `hurtHelmet` also runs before the gate (hits the gate rejects). The dossier's §3 table lists a helmet-path target without this warning (§7 states the fact).
4. **Per-item fire rule is lost at a HEAD hook.** Vanilla skips fire-resistant items for IS_FIRE (1.20.1 `Inventory.hurtArmor` `isFireResistant` test; 1.21.1 `ItemStack.canBeHurtBy`). Lava, in_fire, hot_floor, fireball are not armor-bypassing and do reach `Player.hurtArmor`; if a tier (FIRE_IMMUNE_LAVA_WALK, NETHERITE) should not wear from fire, the hook needs its own IS_FIRE test.
5. **Ender pearl damage on Forge/NeoForge is event-controlled** (`EntityTeleportEvent$EnderPearl.getAttackDamage()`, default 5.0), so other mods can change it; still a direct `fall` hurt, still only catchable in `LIVING_HURT` (the calculateFallDamage hook does not see it).
6. **1.21.1-only alternative for falls**: `Attributes.FALL_DAMAGE_MULTIPLIER` / `SAFE_FALL_DISTANCE` (read in `calculateFallDamage`, V21/NEO @16-38) could zero fall damage with an attribute modifier and no mixin, removing sounds too; 1.20.1 has no such attributes, so the single `calculateFallDamage` mixin remains the uniform choice. Also Forge/NeoForge `LivingFallEvent` (`onLivingFall@3`) can cancel before sounds, but it is loader-specific and Architectury exposes no fall event (grep of the four sources jars).
7. **Side effect of `causeFallDamage` returning true on 1.21.1**: `Player.causeFallDamage` calls `resetCurrentImpulseContext()` when the super call returns true (V21 @112-116, NEO @115-119), i.e. even when the hurt is cancelled in the event (wind-charge fall-immunity context is reset). Harmless but a difference from a calculateFallDamage=0 hook, where `causeFallDamage` returns false and the context is kept.
8. **Router threshold detail (C12)**: the first mismatch between "points then ×1000" and "quotas from milli" appears at n = 6145 with the shares used by `DamageRouterTest` (none ≤ 8000 with the config defaults); any planned "equality on multiples of 1000" test must go through delegation, not through a second quota computation.
