# Note per il piano della milestone 3 (dalla revisione finale della seconda metà della milestone 2)

Questo file conserva, in forma tracciata, i rilievi consultivi del revisore "prontezza per la milestone 3" e le
decisioni prese il 13 novembre 2026 che cambiano il contenuto della milestone 3. È un ingresso per il piano, non
un piano: il piano di M3 va scritto a partire da qui, dal documento di design e dall'audit (§9).

## Decisioni del 13 novembre che toccano M3

- La grafica (modelli GeckoLib, texture, animazioni) è realizzata nella sessione cloud, generata via codice, con
  immagini di anteprima da mostrare a Luigi prima dell'integrazione. Direzione artistica: esoscheletro industriale
  pesante, con ispirazione generale alle power armor di Fallout 4 e a Iron Monger (Iron Man, 2008) e forme originali.
- Le decalcomanie da stendardo entrano nell'alpha: il renderer e la disposizione UV di M3 devono già prevedere il
  layer della decalcomania.
- I test in mondo senza client si possono fare con il GameTest framework, qui e nella CI.
- Il server dedicato parte già sulle quattro varianti (provato nel cloud); le prove con il client sono sul PC.

## Limiti noti ereditati

Vedi la sezione "Limiti noti e rinvii" di `docs/compat.md`.

## Rilievi del revisore (testo originale, in inglese)


### 1. Minor - BREAKING LATER: `ItemDataAccess.PieceData` has no room for decals, so adding them breaks the record's canonical constructor
- file: compat/ItemDataAccess.java:48-54
- problem: `record PieceData(Tier tier, int durability, int maxDurability, List<ModuleType> modules, int colorPrimary, int colorSecondary)`. Decals are now in the alpha (the design doc had them in M7; see design 7.10 and 10). Each piece will carry 0..N banner layers. The only way to add them is a new record component. That changes the canonical constructor and the accessor list, so every call site breaks. Today there are only 2 `PieceData(` occurrences, but M3 will add more: piece items, the crafting result, the entity drop and the station.
- impact: if M3 builds on the current record, the decal milestone (M4, the station) has to rewrite every constructor call of an interface type. The same holds for `FrameState`, which has no cosmetics at all. The data reviewer already reported that colours exist only on items. Decals widen that gap, because the GeckoLib layer renders from `FrameState`, not from items.
- fix (first task of M3, before any production code builds `PieceData`): group the cosmetics into a nested record, for example `Appearance(int primary, int secondary, List<Decal> decals)` with `Appearance.DEFAULT`, and replace the two colour ints with it. Add the same `Appearance` per slot to `FrameState` (read and written by `FrameStateCodec` under a new key) so the renderer reads it from the synced state. Adding keys to the NBT schema is additive. The record shape is the part that breaks later.
- evidence: `grep -rn "PieceData(" src` returns 2 occurrences. compat/ItemDataAccess.java:48. core/frame/FrameState.java:30-41 has no colour or decal field.

### 2. Minor - Banner decals need a version-neutral stored form and a compat seam; nothing in compat/ covers them yet
- file: compat/NbtKeys.java:14-57; compat/ItemDataAccess.java:22-42
- problem: the vanilla representations differ by version. On 1.20.1, patterns live in the static `BuiltInRegistries.BANNER_PATTERN`, and banner NBT stores the short `hashname` (`Patterns`/`Pattern`/`Color`). On 1.21.1, `BannerPattern` is a datapack registry: it exists only as `Registries.BANNER_PATTERN`, and resolving it needs a `RegistryAccess`. The banner item's data is in the `banner_patterns` component. `ItemDataAccess` and `FrameStateCodec.read(tag, cfg)` have no `RegistryAccess` parameter. M3 also has nothing to copy patterns from a banner item, and no way to find a pattern's texture on the client (`Sheets` material on 1.20.1, `assetId` on 1.21.1).
- impact: storing either vanilla form would break the promise that "data survives the 1.20.1 -> 1.21.1 world upgrade" (NbtKeys.java:6-8), because the DFU migrates vanilla banners, not our tag. Resolving holders at read time would force a `RegistryAccess` into every reader. On the art side, programmatically generated textures need a decal region in the UV layout and a draw order. If M3 generates the Scrap models without that region, M4 has to regenerate the art.
- fix: (a) Store each decal as `{pattern: "<namespace:path registry id>", color: "<DyeColor serialized name>"}`, add new keys in NbtKeys, and keep them as strings in core. Resolve to holders only on the client at render time. (b) M4 adds a `compat/BannerDecals` interface with `List<Decal> fromBanner(ItemStack)` and a client-only `textureOf(String patternId)`. (c) M3 already writes into the programmatic asset spec (bone names, UV, tint masks, design 7.11) the decal UV rectangle for each piece (torso front and helmet at least) and the layering order base -> tint -> decal -> damage overlay. Add a headless JUnit check that parses the generated `.geo.json` files and asserts the bone names and decal UV the code uses.
- evidence: javap on minecraft-merged 1.20.1 shows `BuiltInRegistries.BANNER_PATTERN`, `BannerPattern.getHashname()`, `byHash(String)` and `BannerBlockEntity.TAG_PATTERNS/TAG_PATTERN/TAG_COLOR`. javap on 1.21.1 shows only `Registries.BANNER_PATTERN` (a ResourceKey), with no BuiltInRegistries entry, and `BannerPattern(ResourceLocation, String)` with `assetId()`.

### 3. Minor - Other players' frames are never synced; Architectury has no tracking event and `Net` has no multi-recipient send
- file: compat/FrameStateAccess.java:16-19; compat/impl/FrameStateAccessImpl.java:60-76; compat/impl/FrameStateClientCache.java:98-121; compat/Net.java:41
- problem: design 5.4 says the GeckoLib layer on the player renderer draws the six pieces from the frame state. That includes other players, since the placeholders are not rendered. The contract is "each player receives only their own state". Sync happens only on join, respawn, update and clear. The cache is keyed by UUID, so it can hold several states, but nothing fills it for others. Entries are dropped only at world quit, so a remote player who leaves or exits the frame would stay cached. The full packet would also leak the owner, toggles and cell energy to everyone.
- impact: in multiplayer every other player in a frame renders as a plain player, or with a stale frame. Fixing it needs a per-loader hook, which the compat layer cannot express today.
- fix (M3, together with the GeckoLib layer): add a visual-only S2C packet (pieces' tier and condition, appearance, maybe the toggles that have visuals such as the jetpack) and a `Net.sendToTracking(Entity, id, msg)` (Architectury `NetworkManager.sendToPlayers` exists on both versions). Add a `compat` hook for start-tracking, implemented in platform/: Fabric `EntityTrackingEvents.START_TRACKING`, Forge/NeoForge `PlayerEvent.StartTracking`. On update, send the visual packet to the trackers. On stop-tracking, or on an "absent" message, evict the entry. Reword FrameStateAccess.java:16-19.
- evidence: javap of `PlayerEvent`/`EntityEvent` in architectury-forge-9.2.14 and architectury-neoforge-13.0.11 lists JOIN, QUIT, RESPAWN, CLONE, CHANGE_DIMENSION, LIVING_HURT, LIVING_DEATH, ADD and so on, with no tracking event (`unzip -l | grep -ic track` returns 0). `NetworkManager.sendToPlayers(Iterable<ServerPlayer>, ...)` is present in both.

### 4. Minor - The frame entity cannot persist, sync or transfer a `FrameState` without reaching into compat.impl
- file: compat/impl/FrameStateCodec.java:37,41,77; compat/FrameStateAccess.java:26-35
- problem: M3's placeable frame entity holds pieces, cell and owner, with the same content as `FrameState`. It must save that to entity NBT, send it to clients for rendering, hand it to the player on enter, take it back on exit, and create it on death (design 5.4). The only codec, `FrameStateCodec`, is package-private (`final class`, `static` methods with package access) in compat/impl. `FrameStateAccess` offers only `update(Consumer)`, which mutates a fresh or existing state. There is no put-whole-state or take-and-clear. Copying a `FrameState` field by field goes through `installModule`, which re-checks the slots against config.
- impact: M3 either duplicates the codec in content/, which gives two schemas that drift apart, or widens compat.impl visibility ad hoc. Enter and exit become two non-atomic steps (read, then clear) with two syncs. On a crash between them, a frame is duplicated or lost.
- fix (M3): expose `CompoundTag FrameStateAccess.writeTag(FrameState)` and `FrameState readTag(CompoundTag)` (delegating to the codec, so the entity and the SavedData share one schema). Add `void put(ServerPlayer, FrameState)` (replace and sync once) and `Optional<FrameState> take(ServerPlayer)` (remove and sync once). The `stowed` gap the plan reviewer reported belongs in the same change.
- evidence: FrameStateCodec.java:37 `final class FrameStateCodec`, :41 `static CompoundTag write`, :77 `static FrameState read`. FrameStateAccess.java:26-53 has no tag or transfer method.

### 5. Minor - The config is not distributed to clients, yet the client needs it in M3 and not only in M4
- file: compat/impl/FrameStateClientCache.java:107; RedstoneExoframes.java:16,38
- problem: the client rebuilds the synced state with its own `RedstoneExoframes.config()` (ruling 4 covers module restore). M3 also needs the client to evaluate `FrameRules` for things that are client-driven. Sprinting is decided on the client (`canSprint`, no sprint under 10%), and so are swimming and the HUD energy state. Today config never leaves the server.
- impact: once M4 loads a config file, a server with different `pieces.baseModuleSlots`, energy thresholds or tier values makes clients drop modules and gate sprint at the wrong threshold (rubber-banding). Players see a HUD that disagrees with the server.
- fix: in M3 (where sprint gating first lands), send the effective config in an S2C packet at join, before the state sync, and keep a client-side "server config" that `FrameStateClientCache` and client `FrameRules` use. Alternatively, M3 syncs the few derived flags (`canSprint`, `canSwim`, energy state) instead of evaluating on the client. Either way, decide it in M3 and not in the config-loading milestone.
- evidence: FrameStateClientCache.java:107 `FrameStateCodec.read(msg.state().get(), RedstoneExoframes.config())`. No other packet is registered besides `frame_state_sync` (FrameStateAccessImpl.java:68).

### 6. Minor - The damage path has no seam to modify damage, and fractional damage is lost before the carry
- file: compat/DamageClassifier.java:22; core/armor/DamageRouter.java:115; core/armor/Piece.java:243
- problem: the M3 path is classify, then `DamageRouter.route(kind, int amount, mounted, carry)`, then `Piece.damage`, inside `FrameStateAccess.update`. The common hook, Architectury `LIVING_HURT`, receives a `float` and can only pass or cancel. It cannot change the amount. `route` takes an `int`, so 4.5 damage becomes 4 or 5, even though the carry is already kept in thousandths. Design features need to reduce damage, not only cancel it: Armored-tier projectile resistance, the energy shield absorbing projectiles (cancelling may do), and Netherite fire immunity (cancelling works). None of these has a compat seam. The ordering is also undocumented: wear from the pre-armor amount, as vanilla `hurtArmor` does.
- impact: every per-loader damage-modifying event (Forge `LivingHurtEvent.setAmount`, NeoForge `LivingIncomingDamageEvent`, a Fabric mixin) would end up ad hoc in content code, or M5 would bolt it on later. Systematic rounding of float damage also biases wear.
- fix: M3 adds a `route` overload that takes the amount in thousandths (`Math.round(amount * 1000)`), so fractions go into the existing carry. It documents "wear from the pre-armor amount, skipped when `bypassesArmor`". It also adds `compat/DamageHooks.onIncoming(LivingEntity, DamageSource, float) -> float`, implemented per loader in platform/. In M3 it is used only for wear. M5 (Armored tier) uses it for reduction.
- evidence: javap `EntityEvent$LivingHurt`: `EventResult hurt(LivingEntity, DamageSource, float)` in both Architectury jars. DamageRouter.java:115 `route(DamageKind kind, int amount, ...)`.

### 7. Minor - Tick rate: `FrameAttributes.apply` re-sends on every call, with no change detection, and respawn or dimension reapply is not wired
- file: compat/FrameAttributes.java:20-23; compat/impl/FrameAttributesImpl.java:210-216
- problem: the speed multiplier and protection depend on the energy state (−70% speed and 50% protection at zero), which changes during play. M3 has to call `apply` on every effects change, and in practice it will compute effects every tick. `set()` always calls `removeModifier` plus `addTransientModifier`, and that marks the attribute dirty and re-sends it. The modifiers are transient, so a respawned player entity loses them. Only the frame state sync is wired to JOIN and RESPAWN (FrameStateAccessImpl.java:70-75). Attribute reapply on respawn or CHANGE_DIMENSION is left to M3 with no hook. (The per-tick `update()` full sync is already reported by the plan reviewer and is not repeated here.)
- impact: one attribute packet per player per tick if M3 follows the obvious loop. If M3 forgets the respawn reapply, a respawned player in a frame has vanilla speed and armor.
- fix (M3): make `set()` idempotent: when `inst.getModifier(key)` already has the same amount and operation, return without touching it (`getModifier(UUID)` on 1.20.1, `getModifier(ResourceLocation)` on 1.21.1, both public). `apply` is then safe per tick. Register attribute reapply next to the sync on PLAYER_RESPAWN and CHANGE_DIMENSION. Keep the per-player `EnergyMeter` (its sub-RE carry is not in `FrameState`) in a transient server map cleared on PLAYER_QUIT.
- evidence: javap `AttributeInstance` on both versions: `public AttributeModifier getModifier(UUID|ResourceLocation)`, `protected void setDirty()`.

### 8. Minor - The locked placeholder slots have no seam: nothing prevents removal, dropping, or the death drop
- file: compat/ArmorMaterials.java:28-32; content/ModItems.java:13
- problem: design 5.4 needs four placeholder stacks that cannot be removed, thrown or dropped at death. Architectury offers `PlayerEvent.DROP_ITEM`, but no hook for armor-slot pickup and no way to keep an item at death. `LIVING_DEATH` cannot edit drops. A cheap cross-version route is vanilla Curse of Binding plus Curse of Vanishing on the placeholders. But enchanting a stack differs by version: `ItemStack.enchant(Enchantment, int)` on 1.20.1, `Holder<Enchantment>` from the registry on 1.21.1. Binding does not hold in creative mode.
- impact: without a decision, M3 adds mixins per loader or leaves a duplication exploit (placeholders taken out of the slot or dropped on death).
- fix (M3): decide between "binding+vanishing via a small `compat` helper" and "a mixin on `ArmorSlot.mayPickup` plus a death-drop filter in platform/". In either case, also strip placeholders on frame exit and on login if the state is absent (self-healing). Write a GameTest for "death with frame -> no placeholder drops".
- evidence: javap PlayerEvent and EntityEvent field lists (see item 3). ModItems.java:13 registers only `redstone_cell` so far.

### 9. Minor - The headless GameTest path is not wired, and the S2C send to a mock player should be the first test
- file: platform/RedstoneExoframesFabric.java, RedstoneExoframesForge.java, RedstoneExoframesNeoForge.java (no gametest entry); there is no gametest configuration in the build files
- problem: GameTest registration differs by loader (Fabric `fabric-gametest` entrypoint, Forge/NeoForge `RegisterGameTestsEvent`/`@GameTestHolder`), so it needs a platform/ seam. Test classes in common code must avoid compat.impl. `FrameStateAccess.update` always calls `syncTo`, which calls `Net.sendToPlayer`. On a `makeMockServerPlayerInLevel()` player, NeoForge's payload channel checks may refuse a payload that was not negotiated. I have not verified this, so treat it as a risk to check, not a defect.
- impact: without the seam, the M3 in-world checks (enter, exit, death, damage routing, attribute values) stay manual on the PC. If sync throws on mock players, every state test fails for a reason unrelated to its subject.
- fix (start of M3): add the per-loader GameTest registration in platform/ and a `runGameTestServer` CI step. The first test does `update()` on a mock server player on all four variants. If the send fails, have `NetImpl.sendToPlayer` skip players whose connection has not negotiated the channel (log at debug).
- evidence: javap `GameTestHelper` shows `public ServerPlayer makeMockServerPlayerInLevel()` on both 1.20.1 and 1.21.1. `grep -rni gametest` over the worktree sources and build files returns nothing.

