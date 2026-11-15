# Dossier "attributi" — small API facts on both versions that the plan of part 3c will copy

Read-only investigation (no Gradle, no game process). Repository `/home/user/exosuits` at `2e35aa6`; `J/` = the mod's main Java package directory under `src/main/java/` (same shorthand as `stato-del-codice.md`).

How each fact was checked:
- **[code]** file:line in the repository; **[arch]** Architectury sources jars (`architectury-forge-9.2.14-sources`, `architectury-neoforge-13.0.11-sources`: Mojang names; `architectury-fabric-*-sources`: intermediary names), path relative to `dev/architectury/`; **[data]** a file in a jar or in the Gradle cache; **[not checked]**.
- **[javap]** `javap -c -p` on one class. Jar labels: **V120** = `minecraft-merged-1.20.1` (= what both 1.20.1 variants compile against; Fabric runs it unpatched), **F120** = `forge-1.20.1-47.4.29-minecraft-merged` (Mojang names), **V121** = `minecraft-merged-1.21.1`, **N121** = `neoforge-21.1.266-minecraft-merged-mojang`. "all four" = the fact was read in V120, F120, V121 and N121. Fabric API (0.92.12 / 0.116.18) and Architectury ship no mixin on `AttributeInstance`, `AttributeMap` or the attribute part of `ServerEntity` [data: jar listings], so V120/V121 stand for the Fabric variants.

---

## 1. Attributes

### 1.1 The mod today [code]
- `J/compat/FrameAttributes.java:26,29`: `void apply(LivingEntity e, FrameEffects fx)`, `void clear(LivingEntity e)`. `J/compat/impl/FrameAttributesImpl.java:31-34` ids `frame_speed`, `frame_knockback`, `frame_armor`, `frame_toughness`; `:54-60` `set(...)` = `inst.removeModifier(key(id)); inst.addTransientModifier(modifier(...))` on every call. 1.21.1 branch (active) `:69-80`: key = the `ResourceLocation`, `new AttributeModifier(id, amount, ADD_MULTIPLIED_TOTAL | ADD_VALUE)`. 1.20.1 branch (Stonecutter comment) `:81-111`: key = `UUID.nameUUIDFromBytes(id.toString().getBytes(UTF_8))` precomputed, `new AttributeModifier(uuid, id.toString(), amount, MULTIPLY_TOTAL | ADDITION)`.
- `Compat.attributes()` (`J/compat/Compat.java:37`) has no caller. Respawn listener to extend: `J/compat/impl/FrameStateAccessImpl.java:292-302` (already has the two `PLAYER_RESPAWN` arities, §5).

### 1.2 API an idempotent `apply` can use [javap; member lists identical V120=F120 and V121=N121, method bodies of `AttributeInstance`, `AttributeMap`, `AttributeModifier`, `RangedAttribute` identical after normalising constant-pool indexes]
Package `net.minecraft.world.entity.ai.attributes` (abbreviated `…`).

| | 1.20.1 (V120, F120) | 1.21.1 (V121, N121) |
|---|---|---|
| look up | `AttributeModifier getModifier(UUID)` `(Ljava/util/UUID;)L…/AttributeModifier;` (null if absent) | `AttributeModifier getModifier(ResourceLocation)` `(Lnet/minecraft/resources/ResourceLocation;)L…/AttributeModifier;` |
| has | `boolean hasModifier(AttributeModifier)` (looks up by `getId()` only) | `boolean hasModifier(ResourceLocation)` |
| add | `void addTransientModifier(AttributeModifier)` | `void addTransientModifier(AttributeModifier)`; `void addOrUpdateTransientModifier(AttributeModifier)` |
| remove | `void removeModifier(UUID)`; `void removeModifier(AttributeModifier)`; `boolean removePermanentModifier(UUID)` | `boolean removeModifier(ResourceLocation)`; `void removeModifier(AttributeModifier)` |
| modifier | class; ctor `(UUID, String, double, AttributeModifier.Operation)` `(Ljava/util/UUID;Ljava/lang/String;DL…/AttributeModifier$Operation;)V`; `UUID getId()`, `String getName()`, `double getAmount()`, `Operation getOperation()`; **`equals`/`hashCode` use the id only** | record `AttributeModifier(ResourceLocation id, double amount, Operation operation)`; `id()`, `amount()`, `operation()`, `boolean is(ResourceLocation)`; record `equals` over the three components |
| operations | `ADDITION`, `MULTIPLY_BASE`, `MULTIPLY_TOTAL` | `ADD_VALUE`, `ADD_MULTIPLIED_BASE`, `ADD_MULTIPLIED_TOTAL` |
| instance | `AttributeInstance(Attribute, Consumer<AttributeInstance>)`; `Attribute getAttribute()` | `AttributeInstance(Holder<Attribute>, Consumer<AttributeInstance>)`; `Holder<Attribute> getAttribute()` |
| entity | `LivingEntity.getAttribute(Attribute)`; `Attributes.X` is `Attribute` | `LivingEntity.getAttribute(Holder<Attribute>)`; `Attributes.X` is `Holder<Attribute>` |

Behaviour read in the bytecode:
- `addTransientModifier` (all four): `modifierById.putIfAbsent(id, m)`; if an entry existed → `IllegalArgumentException("Modifier is already applied on this attribute!")`; else add to the per-operation collection and `setDirty()`.
- 1.21.1 `addOrUpdateTransientModifier` (V121 offsets 0-47): `old = modifierById.put(m.id(), m); if (m == old) return;` (**reference** comparison, `if_acmpne` at 20) `getModifiers(m.operation()).put(m.id(), m); setDirty();`. A new record equal to the old one therefore still marks the attribute dirty; the method is not idempotent by itself. It also leaves a stale entry if the operation changed (not our case: one operation per id).
- 1.20.1 `removeModifier(UUID)`: `m = getModifier(id); if (m != null) removeModifier(m)` → nothing happens (no dirty) when absent; `removeModifier(AttributeModifier)` always calls `setDirty()`.
- 1.21.1 `removeModifier(ResourceLocation)`: `modifierById.remove(id)`; null → `return false` (no dirty); else removes from the operation map and `permanentModifiers`, `setDirty()`, `return true`.
- `setDirty()` (all four, `protected`): `dirty = true; onDirty.accept(this)` → `AttributeMap.onAttributeModified`. `setBaseValue(d)` returns early when the value is unchanged.
- Vanilla's own pattern, `LivingEntity.setSprinting(boolean)`: 1.20.1 `if (inst.getModifier(SPEED_MODIFIER_SPRINTING_UUID) != null) inst.removeModifier(SPEED_MODIFIER_SPRINTING); if (sprinting) inst.addTransientModifier(SPEED_MODIFIER_SPRINTING);` — 1.21.1 `inst.removeModifier(SPEED_MODIFIER_SPRINTING.id()); if (sprinting) inst.addTransientModifier(SPEED_MODIFIER_SPRINTING);`.

Idempotent `set` that follows from the above (same shape in both branches of `FrameAttributesImpl`, only accessor names differ):
```java
AttributeModifier cur = inst.getModifier(key(id));                       // UUID on 1.20.1, ResourceLocation on 1.21.1
if (cur != null && cur.getOperation() == op && cur.getAmount() == amount) return;   // 1.21.1: cur.operation(), cur.amount()
if (cur != null) inst.removeModifier(key(id));                           // 1.21.1 may call it unconditionally (boolean, no dirty when absent)
inst.addTransientModifier(modifier(id, multiplyTotal, amount));
```
`clear` as written today is already idempotent on both versions (remove-by-key does not dirty when absent).

### 1.3 "Transient": saving and client sync [javap]
- Saving: `AttributeInstance.save()` writes base + only `permanentModifiers` (list key `Modifiers` on V120, `modifiers` on V121). Transient modifiers are never written to the player file.
- Dirty set: 1.20.1 `AttributeMap.onAttributeModified`: if `attribute.isClientSyncable()` → `dirtyAttributes.add(inst)`; accessors `Set<AttributeInstance> getDirtyAttributes()`, `Collection<AttributeInstance> getSyncableAttributes()`. 1.21.1: always `attributesToUpdate.add(inst)`, and if syncable `attributesToSync.add(inst)`; accessors `getAttributesToSync()`, `getAttributesToUpdate()`, `getSyncableAttributes()` (**no `getDirtyAttributes()` on 1.21.1**). Syncable = `Attribute.setSyncable(true)` was called at registration (`boolean isClientSyncable()`).
- Sending: `ServerEntity.sendDirtyEntityData()` (private, all four identical): `s = living.getAttributes().getDirtyAttributes()` / `getAttributesToSync()`; `if (!s.isEmpty()) broadcastAndSend(new ClientboundUpdateAttributesPacket(entity.getId(), s)); s.clear();`. `broadcastAndSend` = every player tracking the entity **and** the entity's own connection when it is a `ServerPlayer`. It runs from `sendChanges()` only inside `if (tickCount % updateInterval == 0 || entity.hasImpulse || entityData.isDirty())`; `EntityType.PLAYER` is built with `.updateInterval(2)` (V120 offset 4398, V121 5938) → at most 2 ticks after the change.
- Packet content: for each dirty instance `AttributeSnapshot(attribute, getBaseValue(), getModifiers())` = base value plus **all** modifiers (transient included). Client `ClientPacketListener.handleUpdateAttributes`: `setBaseValue(base); removeModifiers(); for (m : modifiers) addTransientModifier(m);` — each resend replaces the client's whole modifier list of that attribute (including the sprint modifier as the server sees it at that moment).
- `ServerEntity.sendPairingData` sends `getSyncableAttributes()` to a player that starts tracking; the owner gets its own values only through the dirty path.
- Cost of today's non-idempotent `set`: every `apply` dirties MOVEMENT_SPEED, ARMOR and ARMOR_TOUGHNESS → one `ClientboundUpdateAttributesPacket` to the owner and to every tracker (KNOCKBACK_RESISTANCE is never sent, see below).

### 1.4 The attributes [javap: `Attributes.<clinit>` parsed; F120 identical to V120; N121 differs from V121 only in using `net.neoforged.neoforge.common.PercentageAttribute` (a `RangedAttribute` subclass, tooltip formatting) for MOVEMENT_SPEED and KNOCKBACK_RESISTANCE]
Registry id / default / range / syncable. On the player `MOVEMENT_SPEED` base is `0.10000000149011612` (`Player.createAttributes()`, all four); the others keep the default.

| Attribute | 1.20.1 | 1.21.1 | Read by |
|---|---|---|---|
| `MOVEMENT_SPEED` | `generic.movement_speed`, 0.699999988079071, [0, 1024], **syncable** | same with default 0.7 | `Player.aiStep` (`setSpeed((float) value)`), `Player.getSpeed()`, `LivingEntity.setSprinting`; client `AbstractClientPlayer.getFieldOfViewModifier()`: `f *= ((float) value / abilities.getWalkingSpeed() + 1.0F) / 2.0F` (both versions) |
| `KNOCKBACK_RESISTANCE` | `generic.knockback_resistance`, 0, [0, 1], **not syncable** | same, **not syncable** | `LivingEntity.knockback(double, double, double)`: `strength *= 1.0 - value` (server; the result reaches the client as velocity) |
| `ARMOR` | `generic.armor`, 0, [0, 30], syncable | same | `LivingEntity.getArmorValue()` = `Mth.floor(value)` → `getDamageAfterArmorAbsorb`; client armor HUD |
| `ARMOR_TOUGHNESS` | `generic.armor_toughness`, 0, [0, 20], syncable | same | `getDamageAfterArmorAbsorb` → `CombatRules.getDamageAfterAbsorb` |
| `STEP_HEIGHT` | absent | `generic.step_height`, 0.6, [0, 10], syncable | `LivingEntity.maxUpStep()` (§2) |
| `SAFE_FALL_DISTANCE` | absent | `generic.safe_fall_distance`, 3.0, [-1024, 1024], syncable | `LivingEntity.calculateFallDamage(float, float)`, `checkFallDamage` (landing particles) |
| `FALL_DAMAGE_MULTIPLIER` | absent | `generic.fall_damage_multiplier`, 1.0, [0, 100], syncable | `calculateFallDamage`: `Mth.ceil((double) ((fall - safe) * mult) * value)` |
| `WATER_MOVEMENT_EFFICIENCY` | absent | `generic.water_movement_efficiency`, 0.0, [0, 1], syncable | `LivingEntity.travel` water branch (V121 offset 121): only raises water speed towards land speed; 0 is already the minimum |
| `GRAVITY` | absent | `generic.gravity`, 0.08, [-1, 1], syncable | `LivingEntity.getDefaultGravity()` → `final double Entity.getGravity()` |

1.20.1 has 13 attributes, 1.21.1 has 31. All nine above are in `LivingEntity.createLivingAttributes()` of their version, so the player has them. Forge 1.20.1 adds to living entities `ForgeMod.SWIM_SPEED`, `NAMETAG_DISTANCE`, `ENTITY_GRAVITY` (`forge.entity_gravity`, 0.08, [-8, 8]), `STEP_HEIGHT_ADDITION` and to players `BLOCK_REACH`, `ENTITY_REACH` (all `RegistryObject<Attribute>`, all syncable) [javap F120 + Forge sources `ForgeMod.java:151-175`]; NeoForge 1.21.1 adds `NeoForgeMod.SWIM_SPEED`, `NAMETAG_DISTANCE`, `CREATIVE_FLIGHT`.
- Value [javap `AttributeInstance.calculateValue`, V120 and V121]: `d = base + Σ add; e = d + Σ(d × multiply-base); e *= Π(1 + multiply-total); return attribute.sanitizeValue(e)` (clamp to the range for `RangedAttribute`). Vanilla's sprint modifier is `0.30000001192092896` multiply-total on both versions, so it multiplies with the frame's `speedMultiplier - 1` modifier: base × 1.3 × 0.85.
- Fall damage order (all four): `LivingEntity.causeFallDamage`: `i = calculateFallDamage(...)`; if `i > 0`: `playSound(getFallDamageSound(i), 1, 1); playBlockFallSound(); hurt(source, i);`. The fall sounds come **before** `hurt`, so cancelling in `LIVING_HURT` keeps them. On 1.21.1 a `FALL_DAMAGE_MULTIPLIER` modifier `ADD_MULTIPLIED_TOTAL -1.0` makes `i == 0`: no sound, no `hurt`, no event. Forge/NeoForge also post a fall event first (`ForgeHooks.onLivingFall` / `CommonHooks.onLivingFall`), which Architectury does not expose.

### 1.5 When modifiers disappear, and what the client keeps [javap]
- Login: nothing saved → absent on the new `ServerPlayer`.
- `PlayerList.respawn(...)` creates a new `ServerPlayer` and calls `restoreFrom(old, keepEverything)`: on 1.20.1 it copies **no** attribute data; on 1.21.1 only `getAttributes().assignBaseValues(old.getAttributes())` (V121 offsets 42-49). Modifiers are gone after a death respawn and after leaving the End.
- Client: the server sends `ClientboundRespawnPacket` with keep-flag `keepEverything ? 1 : 0` from `PlayerList.respawn` and `3` from `ServerPlayer.changeDimension(...)` (1.20.1 also `teleportTo(ServerLevel, …)`). `ClientPacketListener.handleRespawn` builds a new `LocalPlayer` and, with flag bit 1, copies the old attributes with their modifiers (1.20.1 `assignValues`, 1.21.1 `assignAllValues`; otherwise 1.21.1 `assignBaseValues`). So: dimension change keeps modifiers on both sides (the `ServerPlayer` object is kept); leaving the End leaves the client with the old modifiers and the server with none until `apply` runs, whose dirty sync then overwrites the client's list.
- Conclusion: `apply` at `PLAYER_JOIN` and `PLAYER_RESPAWN` is required and sufficient on both versions; the `FrameAttributes` Javadoc (`:10-15`) is correct.

### 1.6 Unit-testing `apply` [javap]
- 1.20.1: `Attribute`, `RangedAttribute`, `AttributeInstance` have no static initializer; a bare `new AttributeInstance(new RangedAttribute("x", 0, 0, 1024), i -> {})` works without the game.
- 1.21.1: `Attribute.<clinit>` reads `BuiltInRegistries.ATTRIBUTE` (static `CODEC`), and `BuiltInRegistries.internalRegister` calls `Bootstrap.checkBootstrapCalled`, which throws when the game is not bootstrapped. Loading `Attribute` in a plain JUnit JVM fails. No existing unit test bootstraps the game: they import only `net.minecraft.nbt.*` [code: `src/test/java`, six files under `compat/impl/`].
- So the idempotence proof belongs in a GameTest. A direct assertion that needs no dirty-set name: the modifier **object** returned by `getModifier(key)` is the same instance before and after a second `apply` with equal effects (remove+add would create a new one).

---

## 2. Step height

- 1.21.1 [javap V121 = N121]: `Entity.maxUpStep()` returns `0.0F`; there is no field and no `setMaxUpStep`. `LivingEntity.maxUpStep()`: `f = (float) getAttributeValue(Attributes.STEP_HEIGHT); return getControllingPassenger() instanceof Player ? Math.max(f, 1.0F) : f;`. Readers: `Entity.collide(Vec3)` and `Player.maybeBackOffFromEdge`. `ServerPlayer` and `RemotePlayer` do not touch the attribute. A transient `ADD_VALUE +0.4` modifier on the server (0.6 + 0.4 == 1.0 exactly in doubles; 1.0 is what vanilla horses use: `AbstractHorse` has `STEP_HEIGHT` 1.0 on 1.21.1 and `setMaxUpStep(1.0F)` on 1.20.1) is synced to the owner like any syncable attribute; the client's `LocalPlayer` then steps one block, and the server's own `move` uses the same value.
- 1.20.1 [javap V120 = F120]: `Entity` has `private float maxUpStep`, `public float maxUpStep()`, `public void setMaxUpStep(float)`. Writers in the whole jar (constant-pool scan): constructors only — `LivingEntity.<init>` `setMaxUpStep(0.6F)`, `ServerPlayer.<init>` `setMaxUpStep(1.0F)`, `RemotePlayer.<init>` `setMaxUpStep(1.0F)`, plus ten mob classes. `LocalPlayer` never sets it, so the local player has 0.6F. `LivingEntity.maxUpStep()` = the field, raised to at least 1.0F when the entity is controlled by a player passenger (no field write). Never reset by vanilla; never synced (plain field, no entity data, no packet). A new `LocalPlayer` (created by `handleRespawn` on every respawn **and** dimension change) starts again at 0.6F; the `ServerPlayer` already has 1.0F.
- Forge 1.20.1 [javap F120 + Forge sources]: `ForgeMod.STEP_HEIGHT_ADDITION` = `RegistryObject<Attribute>`, registry name `forge:step_height_addition`, `new RangedAttribute("forge.step_height", 0.0D, -512.0D, 512.0D).setSyncable(true)`; there is no `ForgeMod.STEP_HEIGHT` in 47.4.29. `IForgeEntity.getStepHeight()` = `(float) Math.max(0, self().maxUpStep() + attributeValue)` for living entities. The patched `Entity.collide` calls `getStepHeight()` (offset 134) instead of `maxUpStep()`; `MoveControl` and the node evaluators too. `Player.maybeBackOffFromEdge` and `isAboveGround` still call `maxUpStep()`.
- Which side must hold the value (the local player's movement runs on the client):

| Variant | What makes a one-block step work |
|---|---|
| 1.21.1 Fabric, 1.21.1 NeoForge | server: a fifth transient modifier on `Attributes.STEP_HEIGHT` in `FrameAttributesImpl` (`>=1.21` branch); reaches the client by the normal attribute sync |
| 1.20.1 Fabric | **client**: `localPlayer.setMaxUpStep(1.0F)` while the suit is worn, re-set after every new `LocalPlayer`; nothing needed on the server |
| 1.20.1 Forge | either the same client-side setter (works: `getStepHeight()` = field + 0), or a server-side transient modifier `+0.4` on `ForgeMod.STEP_HEIGHT_ADDITION.get()` (synced; the `ServerPlayer` then has 1.4, harmless) |

- `Player.isLocalPlayer()` exists on both versions in common code (false in `Player`, true in `LocalPlayer`) [javap], so a common class can pick the local player without touching client-only classes. `frame/SuitSize.tick(Player)` already runs on `TickEvent.PLAYER_PRE` on both sides for every player and knows "wears the suit" (`J/frame/SuitSize.java:92-98`).
- Side effect to expect: `Player.maybeBackOffFromEdge` uses the step height as the drop a sneaking player will walk down, so with 1.0 a sneaking pilot steps down one-block ledges (both versions).

---

## 3. Sounds

- Field types in `net.minecraft.sounds.SoundEvents` [javap, all four; F120 = V120 and N121 = V121 for every one of the 42]. All exist on both versions.
  - **Plain `SoundEvent` on both versions (36):** `ANVIL_PLACE`, `ANVIL_LAND`, `IRON_DOOR_OPEN`, `IRON_DOOR_CLOSE`, `IRON_TRAPDOOR_OPEN`, `IRON_TRAPDOOR_CLOSE`, `PISTON_EXTEND`, `PISTON_CONTRACT`, `ITEM_FRAME_ADD_ITEM`, `ITEM_FRAME_REMOVE_ITEM`, `NETHERITE_BLOCK_PLACE`, `METAL_PLACE`, `COPPER_PLACE`, `RESPAWN_ANCHOR_CHARGE`, `RESPAWN_ANCHOR_SET_SPAWN`, `BEACON_ACTIVATE`, `BEACON_DEACTIVATE`, `BEACON_POWER_SELECT`, `CONDUIT_ACTIVATE`, `LEVER_CLICK`, `STONE_BUTTON_CLICK_ON`, `DISPENSER_DISPENSE`, `DISPENSER_FAIL`, `SMITHING_TABLE_USE`, `GRINDSTONE_USE`, `IRON_GOLEM_REPAIR`, `IRON_GOLEM_STEP`, `EXPERIENCE_ORB_PICKUP`, `FIRE_EXTINGUISH`, `REDSTONE_TORCH_BURNOUT`, `COMPARATOR_CLICK`, `CHAIN_PLACE`, `LODESTONE_PLACE`, `BARREL_CLOSE`, `SHULKER_BOX_CLOSE`, `SHULKER_BOX_OPEN`.
  - **`SoundEvent` on 1.20.1, `Holder<SoundEvent>` on 1.21.1 (5):** `ARMOR_EQUIP_IRON`, `ARMOR_EQUIP_GENERIC`, `ARMOR_EQUIP_NETHERITE`, `ARMOR_EQUIP_CHAIN`, `CROSSBOW_LOADING_END`.
  - **`Holder.Reference<SoundEvent>` on both (1):** `UI_BUTTON_CLICK` (use `.value()`).
- `net.minecraft.world.level.Level` [javap, all four]:
  - `public void playSound(@Nullable Player, double, double, double, SoundEvent, SoundSource, float, float)` — `(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V` — both versions.
  - `public void playSound(@Nullable Player, BlockPos, SoundEvent, SoundSource, float, float)` — `(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V` — both versions.
  - Also on both: `playSound(@Nullable Player, Entity, SoundEvent, SoundSource, float, float)`, `playSound(@Nullable Entity, BlockPos, SoundEvent, SoundSource, float, float)`, `playLocalSound(double, double, double, SoundEvent, SoundSource, float, float, boolean)`. **Only on 1.21.1:** `playSound(Player, double, double, double, Holder<SoundEvent>, SoundSource, float, float)` and `playSound(Player, double, double, double, SoundEvent, SoundSource)`.
  - The first parameter is the player **excluded** from the broadcast: `ServerLevel.playSeededSound` → `PlayerList.broadcast(player, x, y, z, sound.getRange(volume), dimension, ClientboundSoundPacket)`. `null` = everybody in range hears it.
  - `Player.playSound(SoundEvent, float, float)` passes `this` as the excluded player (a server-side call is not heard by that player); `Entity.playSound` passes `null`; `ServerPlayer.playNotifySound(SoundEvent, SoundSource, float, float)` sends to that player only.
- `net.minecraft.sounds.SoundSource` constants, identical on both: `MASTER`, `MUSIC`, `RECORDS`, `WEATHER`, `BLOCKS`, `HOSTILE`, `NEUTRAL`, `PLAYERS`, `AMBIENT`, `VOICE`.
- Existing use [code]: only `J/compat/impl/ArmorMaterialsImpl.java:59,115` (`ARMOR_EQUIP_IRON`, already inside the two version branches).

---

## 4. Item bar and tooltip

- `net.minecraft.world.item.Item` [javap, all four]:
  - `public boolean isBarVisible(ItemStack)` `(Lnet/minecraft/world/item/ItemStack;)Z`; `public int getBarWidth(ItemStack)` `(…ItemStack;)I`; `public int getBarColor(ItemStack)` `(…ItemStack;)I` — identical on both versions and on Forge/NeoForge. `public static final int MAX_BAR_WIDTH = 13` on both.
  - `appendHoverText` differs: 1.20.1 `public void appendHoverText(ItemStack, @Nullable Level, List<Component>, TooltipFlag)` `(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V`; 1.21.1 `public void appendHoverText(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)` `(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V`. `TooltipFlag` has `isAdvanced()` and `isCreative()` on both. Neither method nor `TooltipFlag` carries a client-only annotation in the merged jars: common code may override it.
  - Same on both: `inventoryTick(ItemStack, Level, Entity, int, boolean)`, `getName(ItemStack)`, `isFoil(ItemStack)`.
- Vanilla bar for damageable items: `isBarVisible` = `stack.isDamaged()`; `getBarWidth` = `Math.round(13.0F - (float) damage * 13.0F / (float) max)` (1.20.1 reads the field `maxDamage`; 1.21.1 `stack.getMaxDamage()` and clamps with `Mth.clamp(…, 0, 13)`); `getBarColor` = `Mth.hsvToRgb(Math.max(0.0F, ((float) max - (float) damage) / (float) max) / 3.0F, 1.0F, 1.0F)` (hue 0 red → 1/3 green). `Mth.hsvToRgb(float, float, float)` returns `int` on both. Callers: `ItemStack` delegates, `GuiGraphics` draws.
- The mod's items [code]: `J/content/CellItem.java:14` and `PieceItem.java:22` are `final class … extends Item` with no bar or tooltip; `ExoframeItem.java:22` likewise; `ModItems.java:21-22,65-67` construct them with `new Item.Properties().stacksTo(1)`. The cell stack carries only the charge: `Compat.itemData().cellEnergy(stack)` (`J/compat/ItemDataAccess.java:37`); capacity is `RedstoneExoframes.config().energy.cellCapacity` (`CellItem.java:26`). A piece stack gives `Optional<PieceData> piece(stack)` with `durability`, `maxDurability` (`ItemDataAccess.java:46,75`).
- Where the seams go:
  - **Bar: no seam.** The three bar methods can be overridden directly in `content/CellItem` (fraction = `cellEnergy / cellCapacity`) and `content/PieceItem` (fraction = `durability / maxDurability`), without directives.
  - **Tooltip: a seam in the mod's entity pattern.** `J/compat/impl/EntityBase.java:35` holds the version-specific overrides and exposes neutral hooks; `J/compat/CompatEntity.java:14` is the directive-free abstract class the mod extends (rule 4 of `tools/check_sources.sh`: `compat.impl` is referenced only from `compat/`). The same for items: `compat/impl/ItemBase extends Item` overriding `appendHoverText` in both branches and calling a neutral `protected void tooltip(ItemStack stack, List<Component> lines, boolean advanced)`; `compat/CompatItem extends ItemBase` (abstract, no directives); `CellItem`, `PieceItem` (and `ExoframeItem` if it gets a tooltip) extend `CompatItem`. `docs/compat.md:203` lists `appendHoverText` with "—" (no seam yet): the table row needs updating.
  - The Architectury alternative `ClientTooltipEvent.ITEM` also differs between the versions (§5) and is client-only, so it does not avoid a directive.

---

## 5. Architectury events (9.2.14 for 1.20.1, 13.0.11 for 1.21.1) [arch]

The event classes are the same 13 common + 12 client files in both versions; in the classes below no `Event<…>` field exists in only one version. **Signatures that differ are in bold.** Package `dev.architectury.event.events.{common,client}`.

| Event | Callback | Fired (Fabric / Forge 1.20.1 / NeoForge 1.21.1) |
|---|---|---|
| `TickEvent.PLAYER_PRE`, `PLAYER_POST` | `void tick(Player instance)` (`TickEvent.Player`) | both sides, every `Player`: mixin HEAD / RETURN of `Player.tick` · `TickEvent.PlayerTickEvent` START/END (`ForgeEventFactory.onPlayerPreTick/PostTick`, F120 `Player.tick` offsets 1 / 393) · `PlayerTickEvent.Pre/Post` (N121 offsets 1 / 495) |
| `TickEvent.SERVER_LEVEL_POST` | `void tick(ServerLevel instance)` | `ServerTickEvents.END_WORLD_TICK` · `LevelTickEvent` server side, phase END · `LevelTickEvent.Post` |
| `EntityEvent.LIVING_HURT` | `EventResult hurt(LivingEntity entity, DamageSource source, float amount)` | HEAD of `LivingEntity.hurt` (non-players), HEAD of `Player.hurt`, HEAD of `LocalPlayer.hurt`/`RemotePlayer.hurt` · `LivingAttackEvent` · `LivingIncomingDamageEvent`; a false result cancels |
| `EntityEvent.LIVING_DEATH` | `EventResult die(LivingEntity entity, DamageSource source)` | HEAD of `die` in `LivingEntity`, `Player`, `ServerPlayer` · `LivingDeathEvent` · `LivingDeathEvent` |
| `PlayerEvent.PLAYER_JOIN` | `void join(ServerPlayer player)` | RETURN of `PlayerList.placeNewPlayer` · `PlayerLoggedInEvent` (F120 offset 1164, last statement) · same (N121 1008) |
| `PlayerEvent.PLAYER_QUIT` | `void quit(ServerPlayer player)` | HEAD of `PlayerList.remove` · `PlayerLoggedOutEvent` (offset 1) · same |
| `PlayerEvent.PLAYER_RESPAWN` | **9.2.14 `void respawn(ServerPlayer newPlayer, boolean conqueredEnd)` — 13.0.11 `void respawn(ServerPlayer newPlayer, boolean conqueredEnd, Entity.RemovalReason removalReason)`** | RETURN of `PlayerList.respawn` · `PlayerRespawnEvent` (F120 offset 733) · same (N121 478) |
| `PlayerEvent.PLAYER_CLONE` | `void clone(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wonGame)` | RETURN of `ServerPlayer.restoreFrom` · `PlayerEvent.Clone` with `!isWasDeath()` · same |
| `PlayerEvent.CHANGE_DIMENSION` | `void change(ServerPlayer player, ResourceKey<Level> oldLevel, ResourceKey<Level> newLevel)` | HEAD of `ServerPlayer.triggerDimensionChangeTriggers` · `PlayerChangedDimensionEvent` · same |
| `PlayerEvent.OPEN_MENU` | `void open(Player player, AbstractContainerMenu menu)` | server only: RETURN of `ServerPlayer.openMenu` (if opened) and `openHorseInventory` · `PlayerContainerEvent.Open` · same |
| `PlayerEvent.CLOSE_MENU` | `void close(Player player, AbstractContainerMenu menu)` | server only, in `ServerPlayer.doCloseContainer()` on all: after `containerMenu.removed(this)` (Fabric) / after `inventoryMenu.transferState(...)` (`PlayerContainerEvent.Close`, F120 and N121 offsets 19-34), always before `containerMenu = inventoryMenu`; `menu` is the menu being closed, the player's own inventory menu included |
| `PlayerEvent.DROP_ITEM` | `EventResult drop(Player player, ItemEntity entity)` | RETURN of `Player.drop(ItemStack, boolean, boolean)`, false makes it return null · `ItemTossEvent`, result ignored · same, ignored |
| `PlayerEvent.PICKUP_ITEM_PRE` | `EventResult canPickup(Player player, ItemEntity entity, ItemStack stack)` | `ItemEntity.playerTouch` before `ItemStack.getCount()`, false cancels · `EntityItemPickupEvent` (false cancels; true sets `Result.ALLOW` = picked up without entering the inventory) · `ItemEntityPickupEvent.Pre` (`setCanPickup(TriState)`) |
| `InteractionEvent.RIGHT_CLICK_BLOCK` | `EventResult click(Player player, InteractionHand hand, BlockPos pos, Direction face)` | `UseBlockCallback` · `PlayerInteractEvent.RightClickBlock` (a present result cancels and denies block and item use) · same |
| `InteractionEvent.INTERACT_ENTITY` | `EventResult interact(Player player, Entity entity, InteractionHand hand)` | `Player.interactOn` before the first `getItemInHand`, plus client `MultiPlayerGameMode.interact` · `PlayerInteractEvent.EntityInteract` · same |
| `BlockEvent.BREAK` | `EventResult breakBlock(Level level, BlockPos pos, BlockState state, ServerPlayer player, @Nullable IntValue xp)` | `ServerPlayerGameMode.destroyBlock`, `xp` null · `BlockEvent.BreakEvent`, `xp` non-null · `BreakEvent`, `xp` null |
| `BlockEvent.PLACE` | `EventResult placeBlock(Level level, BlockPos pos, BlockState state, @Nullable Entity placer)` | `BlockItem.place` before `placeBlock` · `BlockEvent.EntityPlaceEvent` · same |
| `LifecycleEvent.SETUP` | `Event<Runnable>`, `void run()` | Architectury's own client/server entrypoint (`init/fabric/ArchitecturyClient`, `ArchitecturyServer`), i.e. after every mod's `main` entrypoint · `FMLCommonSetupEvent` · same |
| `LifecycleEvent.SERVER_BEFORE_START` | `void stateChanged(MinecraftServer instance)` (`ServerState`) | Fabric API `ServerLifecycleEvents.SERVER_STARTING`: in `MinecraftServer.runServer` right before `initServer()` · `ServerAboutToStartEvent`: inside `initServer()` **before `loadLevel()`** (F120 Dedicated 467 < 491, Integrated 38 < 47) · same (N121 504 < 523, 38 < 42) |
| `LifecycleEvent.SERVER_STARTING` | same | mixin RETURN of `DedicatedServer.initServer` / `IntegratedServer.initServer` when true (levels loaded) · `ServerStartingEvent` (after `loadLevel()`) · same |
| `LifecycleEvent.SERVER_STARTED` | same | `ServerLifecycleEvents.SERVER_STARTED` · `ServerStartedEvent` (`runServer`, after `initServer()`) · same |
| `LifecycleEvent.SERVER_STOPPING` | same | HEAD of `MinecraftServer.stopServer` · `ServerStoppingEvent` · same |
| `ClientTickEvent.CLIENT_PRE`, `CLIENT_POST` | `void tick(Minecraft instance)` (`ClientTickEvent.Client`) | `ClientTickEvents.START_CLIENT_TICK/END_CLIENT_TICK` · `TickEvent.ClientTickEvent` START/END · `ClientTickEvent.Pre/Post` |
| `ClientPlayerEvent.CLIENT_PLAYER_JOIN` | `void join(LocalPlayer player)` | `ClientPacketListener.handleLogin` · `ClientPlayerNetworkEvent.LoggingIn` · same |
| `ClientPlayerEvent.CLIENT_PLAYER_QUIT` | `void quit(@Nullable LocalPlayer player)` | `Minecraft.clearLevel(Screen)` (9.2.14) / `Minecraft.disconnect(Screen, boolean)` (13.0.11) · `ClientPlayerNetworkEvent.LoggingOut` · same |
| `ClientPlayerEvent.CLIENT_PLAYER_RESPAWN` | `void respawn(LocalPlayer oldPlayer, LocalPlayer newPlayer)` | `handleRespawn` when the new player is added (every respawn packet: death and dimension change) · `ClientPlayerNetworkEvent.Clone` · same |
| `ClientTooltipEvent.ITEM` | **9.2.14 `void append(ItemStack stack, List<Component> lines, TooltipFlag flag)` — 13.0.11 `void append(ItemStack stack, List<Component> lines, Item.TooltipContext tooltipContext, TooltipFlag flag)`** | `ItemTooltipCallback` · `ItemTooltipEvent` · same |
| `ClientGuiEvent.RENDER_HUD` | **9.2.14 `void renderHud(GuiGraphics graphics, float tickDelta)` — 13.0.11 `void renderHud(GuiGraphics graphics, DeltaTracker deltaTracker)`** | `HudRenderCallback` · `RenderGuiEvent.Post` · same |
| `KeyMappingRegistry.register` | `public static void register(KeyMapping mapping)` (`dev.architectury.registry.client.keymappings`) | `KeyBindingHelper.registerKeyBinding` · queued until `RegisterKeyMappingsEvent`; a later call logs WARN `Key mapping %s registered after event` with a stack trace · same |

Other signatures that differ between the two versions (not requested, same classes): `PlayerEvent.PLAYER_ADVANCEMENT` (`Advancement` → `AdvancementHolder`), `ClientGuiEvent.RENDER_PRE`/`RENDER_POST` (`float delta` → `DeltaTracker delta`), `ClientRawInputEvent.MOUSE_SCROLLED` and `ClientScreenInputEvent.MOUSE_SCROLLED_PRE/POST` (`double amount` → `double amountX, double amountY`). Architectury has no fall event and no step-height helper.

---

## 6. Keys

- Vanilla defaults [javap: `Options.<init>` parsed; F120 = V120, N121 = V121; identical on the two versions]: W A S D (move), SPACE (`key.jump`), LEFT_SHIFT (`key.sneak`), LEFT_CONTROL (`key.sprint`), E (`key.inventory`), F (`key.swapOffhand`), Q (`key.drop`), T (`key.chat`), TAB, SLASH, P (`key.socialInteractions`), L (`key.advancements`), F2, F5, F11, 1-9, **C (`key.saveToolbarActivator`)**, **X (`key.loadToolbarActivator`)**; `key.smoothCamera` and `key.spectatorOutlines` are unbound (`InputConstants.UNKNOWN`). No other vanilla class contains a `key.categories.*` string (constant-pool scan), so `Options` holds all the vanilla key mappings.
- Among R, G, H, B, N, Z, X, C: **free on both versions: R, G, H, B, N, Z. Taken: C and X.** The mod uses V (`GLFW.GLFW_KEY_V`).
- Caveat for B [javap `KeyboardHandler.keyPress`, both versions]: a hard-coded Ctrl+B (`key == 66 && Screen.hasControlDown()`) cycles the narrator (1.21.1 additionally requires the `narratorHotkey` option); the default sprint key is Left Control, so B pressed while sprinting triggers it. `KeyboardHandler.handleDebugKeys(int)` (only while F3 is held) switches on A B C D G H I L N P Q S T F4 (1.21.1 also 1 2 3): B, C, G, H, N are F3 combinations; R, X, Z are not.
- Reusing the exit key code [code]:
  - `J/client/ExitHold.java:14` is `final class ExitHold` (package-private, pure Java): `enum Step { NONE, SHOW_PROGRESS, CLEAR_PROGRESS, SEND, CLEAR_AND_SEND }` (`:19-30`), `static int ticksNeeded(int seconds)` (`:45`, `max(0, seconds) * 20`), `Step tick(boolean keyDown, boolean canCount, int needed)` (`:53`), `long percent()` (`:77`), `void reset()` (`:82`). It holds no key, duration or message: `needed` is passed on every tick, and the caller decides what `SEND` and the progress text mean. Seven JUnit tests in `src/test/java/.../client/ExitHoldTest.java`.
  - `J/client/ExitKey.java`: `KEY = new KeyMapping("key.redstone_exoframes.exit", GLFW.GLFW_KEY_V, "key.categories.redstone_exoframes")` (`:26-32`), one `ExitHold HOLD` (`:35`), `register()` (`:41-45`: `KeyMappingRegistry.register(KEY)`, `ClientTickEvent.CLIENT_POST.register(ExitKey::tick)`, `CLIENT_PLAYER_QUIT` → `HOLD.reset()`), `tick` (`:47-67`): `canCount = keyDown && minecraft.screen == null && SuitLayer.wearsSuit(player)`, `needed = ExitHold.ticksNeeded(config().frame.exitHoldSeconds)`, switch on the step.
  - A second hold-key needs: its own `ExitHold` instance (the state is per instance), its own `KeyMapping` in the same category, `needed = ExitHold.ticksNeeded(config().energy.cellSwapSeconds)` (default 2 → 40 ticks), its own message key with `%s%%`, its own packet, a call from `RedstoneExoframesClient.init()` (`J/client/RedstoneExoframesClient.java:20`) and two lang keys in each of `en_us.json`/`it_it.json`. It must live in package `client` (or `ExitHold` becomes public). No change to `ExitHold` or its test is required; a rename (it is no longer exit-only) would touch the test and `docs/compat.md:156`.
  - Both keys write the action bar; held together they overwrite each other's progress line (cosmetic).
  - The hold duration is read from the client's own config: on a remote server it is the client's value, not the server's (see §7).

---

## 7. Config file plumbing

- Nothing reads a file [code, grep]: no `com.google.gson`, `JsonParser`, `Files.read…`, reader or TOML use in `src/main/java` outside `gametest/` (which only touches the registry file). `RedstoneExoframes.setConfig(ExoframesConfig)` (`J/RedstoneExoframes.java:65`) has no caller; `ConfigValidation.validate` (`J/core/config/ConfigValidation.java:44`) is called only by `ExoframesConfigTest`; `Compat.platform()` has no caller. The config is always `ExoframesConfig.defaults()` (`RedstoneExoframes.java:28`, a `static volatile` field).
- Where `config()` is read (identical to the list in `stato-del-codice.md` §2.3) [code]: `frame/FramePermissions.java:26`; `frame/DebugWear.java:77`; `content/ExoframeItem.java:49`; `content/CellItem.java:26,43`; `content/PieceItem.java:57,63`; `command/ExoframeCommand.java:340`; `compat/impl/FrameStateAccessImpl.java:74`; `compat/impl/FrameStateSavedData.java:619` (captured **once**, when the registry is loaded; every loaded `FrameState` keeps it); `compat/impl/FrameStateClientCache.java:29` (client); `client/ExitKey.java:54` (client, every tick). None is a static initializer: all run from listeners, item/command methods or the registry load.
- The folder: `Platform.configDir()` (`J/compat/Platform.java:17`) → `PlatformImpl.java:32-35` → `dev.architectury.platform.Platform.getConfigFolder()` returning `java.nio.file.Path` on both Architectury versions [arch `platform/Platform.java:79` (9.2.14) and `:94` (13.0.11)]: Fabric `FabricLoader.getInstance().getConfigDir().toAbsolutePath()…`, Forge/NeoForge `FMLPaths.CONFIGDIR.get()`.
- Gson at runtime [data: `/root/.gradle/caches/fabric-loom/<version>/mojang_minecraft_info.json`]: Minecraft 1.20.1 lists `com.google.code.gson:gson:2.10`, 1.21.1 lists `gson:2.10.1`. It is a separate library (0 Gson classes inside the merged jars) used by server-side vanilla classes on both versions (`net.minecraft.util.GsonHelper`, `server.players.StoredUserList`, `server.players.GameProfileCache`, …) [constant-pool scan], so it is present on client and dedicated server; Forge and NeoForge classes use it too.
- Gson in unit tests [code]: `build.gradle.kts:151-153` declares only JUnit; Gson comes with the game libraries. Three existing tests already import `com.google.gson` (`art/ExoframeGeometryTest`, `client/PilotPoseTest`, `client/SuitScaleTest`) and run on the four variants. Existing tests do use Minecraft classes, but only `net.minecraft.nbt.*` (six files in `compat/impl/`), never a bootstrapped game.
- Gson and `ExoframesConfig` [javap gson-2.10 and gson-2.10.1 + code]: the POJO has public fields, nested `public static final class` sections with no-arg constructors and six `EnumMap` fields (`ExoframesConfig.java:53-54,66-67,86-87`). Both Gson versions create `EnumMap` (`ConstructorConstructor` special case) and have `JsonParser.parseReader`/`parseString`. `PieceSlot`, `Tier`, `ModuleType` do not override `toString`, so map keys are the enum names. A missing key keeps the field's default; a JSON map replaces the pre-filled `EnumMap` (entries not listed are then absent, which `ConfigValidation` restores). An **unknown enum key** makes `EnumTypeAdapter.read` return null and `MapTypeAdapterFactory` then calls `EnumMap.put(null, …)` → `NullPointerException`, not a `JsonParseException` [the JDK's null-key behaviour not checked by bytecode].
- Rule to respect [code `tools/check_sources.sh` 1b]: no `com.google.gson` import in `core/`; the loader goes in a non-core package. A loader with no Minecraft imports is unit-testable as is.
- Startup order (dedicated server and client) [code + arch + javap]:
  1. Mod init: Fabric `ModInitializer.onInitialize()` of entrypoint `main` (`J/platform/RedstoneExoframesFabric.java`); Forge and NeoForge the `@Mod` class constructor (`RedstoneExoframesForge`, `RedstoneExoframesNeoForge`). All three call `RedstoneExoframes.init()` first; the client init follows (Fabric entrypoint `client`; Forge/NeoForge inside the same constructor under `EnvExecutor.runInEnv(Env.CLIENT, …)`). [Position of the Fabric entrypoint call inside the game's bootstrap: not checked; it precedes server creation by construction, since it registers content.]
  2. `LifecycleEvent.SETUP` (see §5).
  3. Server creation (dedicated: once per process; client: the integrated server, once per world opened) → `SERVER_BEFORE_START` → `loadLevel()` → `SERVER_STARTING` → `SERVER_STARTED` → ticks → `PLAYER_JOIN`. On all four variants `SERVER_BEFORE_START` precedes `loadLevel()` (§5).
  4. The frame registry is read lazily, at the first `FrameStateSavedData.get(server)` (`J/compat/impl/FrameStateSavedData.java:181-215`), which needs `server.overworld()` and therefore comes after step 3's `loadLevel()`.
  - Consequence: a file read as the first statement of `RedstoneExoframes.init()` precedes every `config()` use on the four variants, because every reader is reached only through something `init()` (or the client init after it) registers, or through a running server. A re-read at `SERVER_BEFORE_START` would also precede the registry load, on the server thread.

---

## 8. What this changes for the plan of part 3c

Recommendations
1. **Idempotent `apply`**: keep `removeModifier` + `addTransientModifier`, guarded by "current modifier has the same amount and operation → return" (snippet in §1.2). Do **not** rely on `addOrUpdateTransientModifier` for idempotence (reference comparison), and do not compare with `equals` on 1.20.1 (id only). `clear` needs no change.
2. **Call sites**: `PLAYER_JOIN`, `PLAYER_RESPAWN` (two arities already in `FrameStateAccessImpl.java:299-301`) and every effects change; nothing on a plain dimension change. `clear` on exit and death.
3. **Knockback resistance is server-only** (attribute not syncable on either version): nothing to verify on the client.
4. **FOV**: the speed modifier narrows the client's field of view by `(m + 1) / 2` — ×0.925 at the 0.85 base, ×0.6275 at the 0.255 of an empty cell (subject to the player's FOV-effects option). Decide whether that is wanted; changing it needs a client hook.
5. **Fall damage**: on 1.21.1 a fifth-or-sixth modifier `FALL_DAMAGE_MULTIPLIER` `ADD_MULTIPLIED_TOTAL -1.0` removes damage, hurt animation and fall sound with no event; 1.20.1 has no such attribute, and cancelling in `LIVING_HURT` leaves the fall-damage sound on every variant. Choosing `LIVING_HURT` everywhere (the design's text) gives one behaviour on four variants, with the sound; choosing the attribute on 1.21.1 makes the versions differ.
6. **One-block step** (design §8, question 2d): cheap on all four, no mixin. 1.21.1: one more transient modifier (`STEP_HEIGHT`, `ADD_VALUE`, 0.4) inside `FrameAttributesImpl`. 1.20.1 (both loaders, one code path): a compat method that calls `player.setMaxUpStep(1.0F / 0.6F)` for `player.isLocalPlayer()` only, driven every tick from the existing `PLAYER_PRE` listener (`SuitSize.tick`) because a new `LocalPlayer` restarts at 0.6F; remember that the mod raised it, so the reset does not fight another mod. The design's "a bit less cheap on Fabric 1.20.1" does not hold.
7. **Idempotence test**: a GameTest (same-instance assertion on the modifier), not JUnit — `Attribute` cannot be loaded without a game bootstrap on 1.21.1. The design's §5.9 line "idempotenza degli attributi dove testabile" (JUnit) should move to the GameTest list; the helper that returns a frame modifier belongs in `compat/GameTests` (key type differs per version).
8. **Sounds**: use only the 36 plain `SoundEvent` constants and `level.playSound(null, x, y, z, sound, SoundSource.PLAYERS or BLOCKS, volume, pitch)` from server code; that call is identical on the four variants and needs no seam. Avoid `ARMOR_EQUIP_*`, `CROSSBOW_LOADING_END` (type changes) and `UI_BUTTON_CLICK` (`Holder.Reference`) unless inside `compat/impl`. Never `player.playSound(...)` on the server for a sound the pilot must hear.
9. **Cell and piece bars**: override the three bar methods directly in `CellItem`/`PieceItem`; copy the vanilla formulas (`Math.round(13.0F * fraction)`, `Mth.hsvToRgb(fraction / 3.0F, 1.0F, 1.0F)`). The cell bar reads `cellCapacity` from the local config.
10. **Tooltip**: new `compat/impl/ItemBase` + `compat/CompatItem` (§4), `CellItem`/`PieceItem` re-parented; update the `docs/compat.md` table row.
11. **Second key**: default **R, G, H, N or Z** (not C, X; not B because of Ctrl+B). Reuse `ExitHold` unchanged with a second instance from a new class in package `client`; register it in `RedstoneExoframesClient.init()` so that Forge/NeoForge see it before `RegisterKeyMappingsEvent` (a late registration logs a WARN containing the key name, which the log check would catch).
12. **Config loader**: read the file as the first statement of `RedstoneExoframes.init()` (`setConfig(validate(load(Compat.platform().configDir())).normalized())`), in a non-core, Minecraft-free class with Gson; catch `RuntimeException` and `IOException` (unknown enum keys throw `NullPointerException`), log `ConfigValidation` problems, write the defaults when the file is missing. JUnit can cover it on the four variants.
13. **Menu-close hook** (design §11, placeholder escaping between ticks): `PlayerEvent.CLOSE_MENU` exists on both versions, server side, same signature — usable without a seam.
14. **Events needing a version branch if used**: `PLAYER_RESPAWN`, `ClientTooltipEvent.ITEM`, `ClientGuiEvent.RENDER_HUD`; everything else in §5 is source-compatible. `DROP_ITEM` cannot cancel on Forge/NeoForge; `BlockEvent.BREAK`'s `xp` is null except on Forge.

Open points I could not settle
- Whether the client actually steps one block with each recipe of §2 (bytecode says so; the client runs are another agent's).
- Runtime mixins of other mods on these classes; GeckoLib's jars were not scanned for attribute mixins.
- The exact point where Fabric Loader invokes the `main` entrypoint (not in the jars read); the argument in §7 does not depend on it.
- Whether the registry's one-time capture of `config()` (`FrameStateSavedData.java:619`) should be refreshed if the plan re-reads the file per world on a client that opens several worlds in one session.
- Server-authoritative values a remote client reads from its own file (`exitHoldSeconds`, `cellSwapSeconds`, `cellCapacity` for the bar): which of them the server should send.

## 9. `stato-del-codice.md`: statements in this dossier's area

- **Confirmed**: §3.1 accessor lists for both versions; "13 attributes on 1.20.1, STEP_HEIGHT/SAFE_FALL_DISTANCE/FALL_DAMAGE_MULTIPLIER/GRAVITY/WATER_MOVEMENT_EFFICIENCY/SCALE only on 1.21.1"; `set` not idempotent; no caller of `Compat.attributes()`; §2.3 list of `config()` readers, `setConfig`/`validate`/`Compat.platform()` without callers; §2.4 `getConfigFolder()` and the lifecycle event names on both versions; §5 bar methods identical and `appendHoverText` different; §6 `RENDER_HUD` signatures, `ExitHold` generic; §3.3 two-argument `PLAYER_RESPAWN` on 1.20.1.
- **Corrected / completed**: §3.1 lists `addOrUpdateTransientModifier` among what "an idempotent apply can use" — it dirties the attribute unless the very same object is passed. "Forge's `STEP_HEIGHT_ADDITION` [not checked]" → exists (§2). "Gson ships with Minecraft [not checked]" → 2.10 / 2.10.1, checked. §9 item 9 "Architectury event availability not checked" → `PlayerEvent.CLOSE_MENU` exists on both. §10 "fall sounds [sound order not checked]" → sounds precede `hurt` on all four. The design's remark that the step is less cheap on Fabric 1.20.1 is not supported by the bytecode.
- **Cannot check here**: the bytecode offsets it gives for `LIVING_HURT` relative to difficulty scaling and invulnerability (the damage dossier's subject); only the event-to-loader mapping is confirmed.
