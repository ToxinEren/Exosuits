# M3 recon: GeckoLib 4.8.4 / 4.9.3 and the loader hooks for the exoframe suit

Read-only research. Nothing in the repository was changed and Gradle was not run. Every fact below comes from the jars the project already resolves.

## Sources

The Loom-remapped (mojmap) jars and their sources jars:

- **GeckoLib:** under `/home/user/exosuits/.gradle/loom-cache/remapped_mods/remapped/software/bernie/geckolib/`
  - `geckolib-fabric-1.20.1-65f153da/4.8.4/…-4.8.4(-sources).jar` (abbreviated **F20** below)
  - `geckolib-forge-1.20.1-25585d85/4.8.4/…` (**Fo20**)
  - `geckolib-fabric-1.21.1-4c662d4c/4.9.3/…` (**F21**)
  - `geckolib-neoforge-1.21.1-45b9ccb9/4.9.3/…` (**N21**)
- **Architectury:** `…/remapped/dev/architectury/architectury-{fabric-65f153da/9.2.14, forge-25585d85/9.2.14, fabric-4c662d4c/13.0.11, neoforge-45b9ccb9/13.0.11}`
- **Fabric API rendering-v1:**
  - `…/remapped/net/fabricmc/fabric-api/fabric-rendering-v1-65f153da/3.0.10+54550fb677` (0.92.12, 1.20.1)
  - `…/fabric-rendering-v1-4c662d4c/5.2.1+f44c2eea19` (0.116.18, 1.21.1)
- **Minecraft and the patched loader jars (mojmap):** `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/`
  - `minecraft-merged/1.20.1-…` and `minecraft-merged/1.21.1-…` (vanilla, **MC20** and **MC21**)
  - `forge-1.20.1-47.4.29-minecraft-merged/…` (**Forge**)
  - `neoforge-21.1.266-minecraft-merged/…` (**Neo**)
  - Raw Forge and NeoForge sources: `~/.gradle/caches/modules-2/.../forge-1.20.1-47.4.29-sources.jar` and `neoforge-21.1.266-sources.jar`

Within one GeckoLib version, the Fabric and Forge/NeoForge sources differ only in these places:

- import order;
- event firing: `MinecraftForge.EVENT_BUS` (Forge 4.8.4), Fabric `EVENT.invoker()`, `GeckoLibServices` (4.9.3);
- Fabric remapped sources that show intermediary names (`renderBuffers.method_23000()`);
- **one behavioural difference** in Forge 4.8.4, described in §2.

---

## 1. Core types on 4.8.4 and 4.9.3

**Answer.** 4.9.3 removed the whole `software.bernie.geckolib.core.*` tree. Animatable, animation, `PlayState`, `Color`, `DataTicket` and `RenderUtils` all moved, and `RenderUtils` was renamed `RenderUtil`. The animation API itself is identical apart from packages.

The breaking signature changes are:

- every render hook takes colour as `int colour` (ARGB) instead of `float red, float green, float blue, float alpha`;
- `GeoModel.handleAnimations` gained a `float partialTick` parameter;
- `GeoArmorRenderer.prepForRender` gained a 10-argument overload. The 4-argument form still exists but is deprecated for removal.

ResourceLocation handling did not change for callers: `getModelResource`, `getTextureResource` and `getAnimationResource` have the same signatures.

| Type | 4.8.4 | 4.9.3 |
|---|---|---|
| GeoAnimatable | `core.animatable.GeoAnimatable` | `animatable.GeoAnimatable` |
| AnimatableInstanceCache | `core.animatable.instance.AnimatableInstanceCache` | `animatable.instance.AnimatableInstanceCache` |
| AnimatableManager(.ControllerRegistrar), AnimationController, RawAnimation, AnimationState | `core.animation.*` | `animation.*` |
| PlayState | `core.object.PlayState` | `animation.PlayState` |
| Color | `core.object.Color` | `util.Color` |
| DataTicket / SerializableDataTicket | `core.object.DataTicket` / `network.SerializableDataTicket` | `constant.dataticket.*` |
| RenderUtils | `util.RenderUtils` | `util.RenderUtil` (renamed) |
| DyeableGeoArmorRenderer, DynamicGeoEntityRenderer | `renderer.*` | `renderer.specialty.*` |
| Same package on both | `model.GeoModel`, `model.Defaulted{,Entity,Item}GeoModel`, `animatable.{GeoEntity,GeoItem,GeoReplacedEntity,SingletonGeoAnimatable}`, `util.GeckoLibUtil`, `cache.object.{GeoBone,BakedGeoModel}`, `renderer.{GeoRenderer,GeoArmorRenderer,GeoEntityRenderer,GeoItemRenderer,GeoObjectRenderer,GeoReplacedEntityRenderer}`, `renderer.layer.{GeoRenderLayer,AutoGlowingGeoLayer}` | same |
| Only in 4.9.3 | `renderer.layer.vanilla.{AttachedAnimatableRenderLayer, GeoObjectVanillaRenderLayer, GeoEntityVanillaRenderLayer, ...}` and `animatable.client.GeoRenderProvider` | |

**Evidence: javap of Fo20 and N21, package prefix shortened.**

```
// 4.8.4
public interface core.animatable.GeoAnimatable {
  void registerControllers(core.animation.AnimatableManager$ControllerRegistrar);
  core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache();
  double getTick(Object); ...}
public static core.animatable.instance.AnimatableInstanceCache GeckoLibUtil.createInstanceCache(core.animatable.GeoAnimatable);
public abstract class model.GeoModel<T extends core.animatable.GeoAnimatable> implements core.animatable.model.CoreGeoModel<T> {
  public ResourceLocation getModelResource(T, renderer.GeoRenderer<T>);  public abstract ResourceLocation getModelResource(T);
  public ResourceLocation getTextureResource(T, renderer.GeoRenderer<T>); public abstract ResourceLocation getTextureResource(T);
  public abstract ResourceLocation getAnimationResource(T);
  public Optional<cache.object.GeoBone> getBone(String);
  public void handleAnimations(T, long, core.animation.AnimationState<T>);
// 4.9.3
public interface animatable.GeoAnimatable { void registerControllers(animation.AnimatableManager$ControllerRegistrar); animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache(); ...}
public abstract class model.GeoModel<T extends animatable.GeoAnimatable> {   // no CoreGeoModel any more
  ... same three resource methods ...
  public void handleAnimations(T, long, animation.AnimationState<T>, float);
  public void setCustomAnimations(T, long, animation.AnimationState<T>);
```

Other facts from the same javap and sources:

- **AnimationController.** The constructors are the same on both versions: `(T, AnimationStateHandler)`, `(T, String, StateHandler)`, `(T, int, StateHandler)` and `(T, String, int, StateHandler)`. So are `RawAnimation.begin().thenLoop/thenPlay/thenPlayAndHold`, `PlayState.CONTINUE/STOP` and `AnimationState.setAndContinue`. After normalising packages, the javap diff shows only three changes:
  - `setTransitionLength(int)` was removed in 4.9.3;
  - `RawAnimation.STREAM_CODEC` was added;
  - `process(...)` now takes a `GeoModel` instead of a `CoreGeoModel`.
- **GeoEntity.** 4.8.4 has `stopTriggeredAnimation(String,String)`; 4.9.3 renames it `stopTriggeredAnim`.
- **GeoItem → SingletonGeoAnimatable.** The render-provider hook differs per loader and per version:
  - Fabric 4.8.4: `public abstract void createRenderer(Consumer<Object>)` and `public abstract Supplier<Object> getRenderProvider()`, both abstract.
  - Forge 4.8.4: neither method exists. Forge uses `Item.initializeClient(Consumer<IClientItemExtensions>)`.
  - 4.9.3: `default void createGeoRenderer(Consumer<GeoRenderProvider>)` and `default Object getRenderProvider()`.
- **Resource paths.** These are unchanged. `DefaultedGeoModel` builds:
  - `geo/<subtype>/<path>.geo.json`
  - `animations/<subtype>/<path>.animation.json`
  - `textures/<subtype>/<path>.png`

  The only difference is the construction: 4.8.4 uses `new ResourceLocation(ns, …)` and 4.9.3 uses `basePath.withPath(…)`.

  `getAnimationResource` is resolved only inside `GeoModel.getAnimation`, which runs when a controller requests an animation. If the file is missing at that point, GeckoLib throws `"Unable to find animation file."`.

**Differences that matter when the code is written once for both versions:**

1. Package moves, and therefore import differences, in every class that implements `GeoAnimatable`/`GeoItem`/`GeoEntity` or overrides `getRenderColor`.
2. The colour type in every overridden render hook: `float r, g, b, a` in 4.8.4, `int colour` in 4.9.3.
3. `handleAnimations(…, partialTick)`.
4. `RenderUtils` → `RenderUtil`.
5. `Color`'s package.
6. The render-provider hooks of `GeoItem`.

---

## 2. Renderers

**Answer.**

- **Unchanged on both versions:**
  - all constructors;
  - the entry point `GeoRenderer.defaultRender(PoseStack, T, MultiBufferSource, @Nullable RenderType, @Nullable VertexConsumer, float yaw, float partialTick, int packedLight)`;
  - `GeoBone.setHidden(boolean)`, `setChildrenHidden(boolean)` and `isHidden()`;
  - `GeoRenderLayer.render(...)`, `preRender(...)` and `renderForBone(...)`, whose parameters carry no colour;
  - `AutoGlowingGeoLayer(GeoRenderer)` with the `<texture>_glowmask.png` convention.
- **Changed:** `preRender`, `actuallyRender`, `postRender`, `renderFinal`, `reRender`, `renderRecursively`, `renderCubesOfBone`, `renderCube` and `createVerticesOfQuad` take `int colour` in 4.9.3 instead of 4 floats.
- **Per-bone colour:** `DyeableGeoArmorRenderer` exists on both versions, with abstract `isBoneDyeable(GeoBone)` and `getColorForBone(GeoBone)`. Only its package and `Color`'s package differ.

**Evidence.**

```
// GeoRenderer 4.8.4 (Fo20) – defaultRender body
Color renderColor = getRenderColor(animatable, partialTick, packedLight);
float red = renderColor.getRedFloat(); ...
BakedGeoModel model = getGeoModel().getBakedModel(getGeoModel().getModelResource(animatable, this));
if (renderType == null) renderType = getRenderType(animatable, getTextureLocation(animatable), bufferSource, partialTick);
if (buffer == null) buffer = bufferSource.getBuffer(renderType);
preRender(...red, green, blue, alpha); if (firePreRenderEvent(...)) { preApplyRenderLayers(...); actuallyRender(...); applyRenderLayers(...); postRender(...); firePostRenderEvent(...);} ... doPostRenderCleanup();
// GeoRenderer 4.9.3 (N21)
int renderColor = getRenderColor(animatable, partialTick, packedLight).argbInt(); ... preRender(..., renderColor) ... doPostRenderCleanup(); MolangQueries.clearActor();
default void renderCubesOfBone(PoseStack, GeoBone bone, VertexConsumer, int packedLight, int packedOverlay, int colour) { if (bone.isHidden()) return; ...}
default void renderChildBones(...) { if (bone.isHidingChildren()) return; ...}
```

Constructors (javap):

| Renderer | 4.8.4 | 4.9.3 |
|---|---|---|
| `GeoEntityRenderer<T extends Entity & GeoAnimatable>` | `(EntityRendererProvider.Context, GeoModel<T>)` and `(Context, EntityType)` | same |
| `GeoItemRenderer<T extends Item & GeoAnimatable>` | `(GeoModel<T>)` and `(BlockEntityRenderDispatcher, EntityModelSet, GeoModel<T>)` | same, but `renderInGui` gains a trailing `float` |
| `GeoArmorRenderer<T extends Item & GeoItem> extends HumanoidModel implements GeoRenderer<T>` | `(GeoModel<T>)`, which bakes `ModelLayers.PLAYER_INNER_ARMOR` from `Minecraft.getInstance().getEntityModels()` | same |
| `GeoObjectRenderer<T extends GeoAnimatable>` | `(GeoModel<T>)`; `render(PoseStack, T, MultiBufferSource, RenderType, VertexConsumer, int packedLight)` | `render(..., int packedLight, float partialTick)`, marked `@ApiStatus.Internal` |
| `GeoReplacedEntityRenderer<E extends Entity, T extends GeoAnimatable>` | `(Context, GeoModel<T>, T)` | same |

Notes on the renderers:

- **GeoObjectRenderer** translates for blocks: its `preRender` does `poseStack.translate(0.5f, 0.51f, 0.5f)`.
- **Colour per bone.** Either override `renderRecursively`/`renderCubesOfBone` (colour signature differs) or use `DyeableGeoArmorRenderer`:

  ```
  // 4.8.4 renderer.DyeableGeoArmorRenderer
  public void renderCubesOfBone(PoseStack, GeoBone, VertexConsumer, int, int, float, float, float, float) { if (dyeableBones.contains(bone)) { Color color = getColorForBone(bone); red *= color.getRedFloat(); ... } super...}
  protected abstract boolean isBoneDyeable(GeoBone); protected abstract core.object.Color getColorForBone(GeoBone);
  // 4.9.3 renderer.specialty.DyeableGeoArmorRenderer
  public void renderCubesOfBone(..., int colour) { if (dyeableBones.contains(bone)) colour = FastColor.ARGB32.multiply(colour, getColorForBone(bone).argbInt()); ...}
  protected abstract util.Color getColorForBone(GeoBone);
  ```

  `getColorForBone` runs on every render, not once per model. Only the set of dyeable bones is cached per baked model (`checkBoneDyeCache`).

- **Texture per render.** There is one texture per render pass: `getTextureLocation(animatable) → model.getTextureResource(animatable, this)`. A per-piece texture therefore needs either several passes or a `GeoRenderLayer` that calls `getRenderer().reRender(...)`, and `reRender`'s colour signature differs between versions.
- **Emissive parts.** `AutoGlowingGeoLayer` behaves differently on the two versions:
  - 4.8.4: `reRender(bakedModel, ..., emissiveRenderType, ..., LightTexture.FULL_SKY, OverlayTexture.NO_OVERLAY, 1,1,1,1)`.
  - 4.9.3: the same call, but the colour is `getRenderColor(...).argbInt()`, and the layer handles invisible and glowing entities.

  Both read `<texture>_glowmask.png` (`AutoGlowingTexture.APPENDIX = "_glowmask"`).
- **Forge 4.8.4 behavioural difference.** Forge's `GeoArmorRenderer.doPostRenderCleanup()` does not clear its fields:

  ```
  // Disabled temporarily on Forge/Neoforge due to an incompatibility with the model retrieval hook
  //this.baseModel = null; //this.currentEntity = null; ...
  ```

  Fabric 4.8.4 and both 4.9.3 builds do clear them. Consequences:
  - prepare the renderer again before every `defaultRender`;
  - on Forge 1.20.1 the renderer keeps a reference to the last entity it drew.

---

## 3. Drawing a GeckoLib model on the player as a vanilla RenderLayer

**Recommendation: option (a).** Use a `GeoArmorRenderer` subclass, specifically a `DyeableGeoArmorRenderer` subclass, driven by hand from our own vanilla `RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>`.

The animatable is one registered, never-equipped "suit model" `Item implements GeoItem`. The `ItemStack` passed in is a dummy `new ItemStack(SUIT_MODEL)`.

Why (a):

- `GeoArmorRenderer` already copies the pose of the `HumanoidModel`, the baby scaling and crouching, and the per-bone visibility.
- Calling it from a `RenderLayer` reproduces exactly the pose context GeckoLib uses inside `HumanoidArmorLayer`.

**Per frame, in the layer's `render(...)`** (the `RenderLayer.render` signature is identical on MC20 and MC21):

1. `renderer.prepForRender(player, SUIT_STACK, EquipmentSlot.CHEST, getParentModel())`. The 4-argument form exists on both versions: normal in 4.8.4, `@Deprecated(forRemoval=true)` in 4.9.3, where it forwards to the 10-argument form.
2. Push our state (visible pieces, colours, texture for this pass) into the renderer.
3. `renderer.defaultRender(poseStack, renderer.getAnimatable(), bufferSource, null /*or RenderType for this texture*/, null, 0f, partialTick, packedLight)`. Its signature is identical on both versions.

Do not call `renderToBuffer`, because the vanilla `Model` signature differs:

```
MC20: abstract void renderToBuffer(PoseStack, VertexConsumer, int, int, float, float, float, float)
MC21: abstract void renderToBuffer(PoseStack, VertexConsumer, int, int, int)
```

**What `prepForRender` needs.** It needs an entity, a stack, a slot and the base model. The stack may be a dummy: the only requirement is that `stack.getItem()` is the `Item & GeoItem` animatable (`this.animatable = (T)stack.getItem()`).

```
// 4.8.4
public void prepForRender(@Nullable Entity entity, ItemStack stack, @Nullable EquipmentSlot slot, @Nullable HumanoidModel<?> baseModel) {
    if (entity == null || slot == null || baseModel == null) return;
    this.baseModel = baseModel; this.currentEntity = entity; this.currentStack = stack; this.animatable = (T)stack.getItem(); this.currentSlot = slot; }
// 4.9.3
@Deprecated(forRemoval = true) public void prepForRender(@Nullable Entity entity, ItemStack stack, @Nullable EquipmentSlot slot, @Nullable HumanoidModel<?> baseModel) { ... prepForRender(entity, stack, slot, baseModel, mc.levelRenderer.renderBuffers.bufferSource(), mc.getTimer().getGameTimeDeltaPartialTick(true), 0, 0, 0, 0); }
public void prepForRender(Entity, ItemStack, EquipmentSlot, HumanoidModel<?>, MultiBufferSource, float partialTick, float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch)
```

Other things the renderer does with the stack:

- `getInstanceId`: with no GeckoLib id on the stack, the animation instance is keyed per entity and slot, so each player gets independent animations.

  ```
  4.8.4: if (stackId == Long.MAX_VALUE) { int id = currentEntity.getId() * 13; return (long)id*id*id * -(currentSlot.ordinal()+1); }
  4.9.3: if (stackId == Long.MAX_VALUE) return (long)Math.pow(currentEntity.getId(), 7) * -(currentSlot.ordinal() + 1);
  ```

- 4.9.3 `getRenderColor` calls `currentStack.is(ItemTags.DYEABLE)`. On an unregistered `Item` the holder's tags are not bound, so the suit model item must be registered.

**How the pose is copied.** Both versions are identical, apart from `RenderUtils` vs `RenderUtil`. In `preRender`:

```
applyBaseModel(this.baseModel);            // young, crouching, riding, arm poses, and head/hat/body/arms/legs .visible
grabRelevantBones(...);                    // bones "armorHead","armorBody","armorRightArm","armorLeftArm","armorRightLeg","armorLeftLeg","armorRightBoot","armorLeftBoot"
applyBaseTransformations(this.baseModel);
scaleModelForBaby(...); scaleModelForRender(...);
if (!(this.currentEntity instanceof GeoAnimatable)) applyBoneVisibilityBySlot(this.currentSlot);

protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
  RenderUtil(s).matchModelPartRot(baseModel.head, this.head);         this.head.updatePosition(headPart.x, -headPart.y, headPart.z);
  ... body:      updatePosition(bodyPart.x, -bodyPart.y, bodyPart.z)
  ... rightArm:  updatePosition(rightArmPart.x + 5, 2 - rightArmPart.y, rightArmPart.z)
  ... leftArm:   updatePosition(leftArmPart.x - 5f, 2f - leftArmPart.y, leftArmPart.z)
  ... rightLeg (+rightBoot): updatePosition(rightLegPart.x + 2, 12 - rightLegPart.y, rightLegPart.z)
  ... leftLeg  (+leftBoot):  updatePosition(leftLegPart.x - 2, 12 - leftLegPart.y, leftLegPart.z) }
actuallyRender: poseStack.translate(0, 24/16f, 0); poseStack.scale(-1, -1, 1); handleAnimations(...)
```

**Overrides our subclass needs.** It lives in `compat/impl` because of the package and colour differences.

- **Per-piece visibility.** Override `protected void applyBoneVisibilityBySlot(EquipmentSlot)`; the signature is the same on both versions. The default implementation shows only the bones of one slot (`setAllBonesVisible(false)`, then for example `case CHEST -> body, rightArm, leftArm`). Ours should:
  - set the six top-level `armor*` bones visible;
  - toggle the child bones from our state with `GeoBone.setHidden` and `setChildrenHidden`. The children are found with `getGeoModel().getBone(name)` (`Optional<GeoBone>` on both versions).
- **Paint.** Implement `isBoneDyeable` and `getColorForBone` (DyeableGeoArmorRenderer).
- **Damage-state textures.** Override `getTextureLocation`, or return the pass texture from `GeoModel.getTextureResource`. Run one `defaultRender` pass per distinct texture, with visibility filtered to the pieces that use that texture. Call `prepForRender` again before each pass, because `doPostRenderCleanup` clears the fields on 3 of the 4 targets.

**Bone names implied for the geometry files.** These are the GeckoLib armor template:

- **Top-level bones:** `armorHead`, `armorBody`, `armorRightArm`, `armorLeftArm`, `armorRightLeg`, `armorLeftLeg`. `armorRightBoot` and `armorLeftBoot` are optional; a missing bone just stays null.
- **Pivots:** they must match the template (head and body at 0,24,0; arms at ∓5,22,0; legs at ∓2,12,0), because of the `+5/-5`, `2-y` and `12-y` offsets above.
- **Children (proposal):** `frameHead`, `frameTorso`, `frameLeftArm`, … for the bare frame; `pieceHelmet`, `pieceTorso`, `pieceLeftArm`, `pieceRightArm`, `pieceLeftLeg`, `pieceRightLeg` for the pieces. Paint regions should be their own bones, for example `pieceTorso_paint1` and `pieceTorso_paint2`, because dye works per bone.
- **Leg pieces** may also need children under `armorLeftBoot` and `armorRightBoot`.

**Why not (b), GeoObjectRenderer or a custom GeoRenderer:**

- The `GeoRenderer` abstract methods are compatible across versions: `getGeoModel`, `getAnimatable`, `fireCompileRenderLayersEvent`, `firePreRenderEvent`, `firePostRenderEvent` and `updateAnimatedTextureFrame`.
- But the pose mapping would have to be copied by hand: `RenderUtils.matchModelPartRot` vs `RenderUtil.matchModelPartRot`, `updatePosition`, and the translate/scale in `actuallyRender`.
- `handleAnimations` (3 or 4 arguments) and the colour hooks would also need version splits.
- `GeoObjectRenderer.render` differs between versions, and its `preRender` applies a block offset (0.5, 0.51, 0.5).

It needs more split code and gains nothing beyond not needing an `Item`.

**Why not (c), GeoReplacedEntityRenderer:** it replaces the whole `EntityRenderer`. For the player that means losing the skin, held items, the cape and the vanilla layers.

**Note on 4.9.3's own vanilla layers:** `AttachedAnimatableRenderLayer` and `GeoObjectVanillaRenderLayer` are not usable here. They exist only in 4.9.3 and are abstract wrappers around the same idea.

**Cost of (a): the animatable item.** Because of the packages and Fabric 4.8.4's abstract `createRenderer` and `getRenderProvider`, the class implementing `GeoItem` has to live in `compat/impl`. It can be a small `SuitModelItem extends Item implements GeoItem`:

- `getAnimatableInstanceCache` returns `GeckoLibUtil.createInstanceCache(this)`;
- `registerControllers` delegates to a version-neutral hook;
- on Fabric 4.8.4, `getRenderProvider` returns `() -> RenderProvider.DEFAULT`. `RenderProvider.of(item)` casts `getRenderProvider().get()`, so it must not return null.

Keep this item out of armor slots and give it a plain item model, so that GeckoLib's own armor and item hooks never handle it.

---

## 4. Invisible placeholders and hiding the vanilla player model

**Answer.**

- **Placeholders.** GeckoLib offers nothing for items that are not GeoItems. Vanilla `HumanoidArmorLayer` will draw the four placeholder `ArmorItem`s. Today they would show the missing-texture pattern.
- **1.21.1.** The cleanest fix is a `frame` material with `layers = List.of()`. The base pass loops over `material.layers()`, so it then draws nothing, on Fabric and on NeoForge.
- **1.20.1.** The base pass always runs. Ship fully transparent `frame_layer_1.png` and `frame_layer_2.png` textures, or add a per-loader hook.
- **Vanilla skin.** Hiding it needs a pre-render hook: `RenderPlayerEvent.Pre` on Forge and NeoForge, a mixin on Fabric. One common mixin on `PlayerRenderer.setModelProperties` would cover all four targets.

**Evidence.**

- **MC21, `HumanoidArmorLayer.renderArmorPiece`:**

  ```
  ArmorItem.getMaterial:()Holder; Holder.value; ... ArmorMaterial.layers:()List; List.iterator; Iterator.hasNext; Iterator.next; ArmorMaterial$Layer.texture(Z)ResourceLocation; renderModel(...)
  ```

  This is a loop, so an empty list means no draw. The NeoForge-patched version loops `material.layers()` the same way and calls `ClientHooks.getArmorTexture(...)` for each layer.

  The repository currently registers `List.of(new ArmorMaterial.Layer(Ids.of("frame")))` (`compat/impl/ArmorMaterialsImpl.java`). That will draw `redstone_exoframes:textures/models/armor/frame_layer_{1,2}.png`, and no such texture exists (`src/main/resources/assets/redstone_exoframes/textures` holds only `item/`).
- **MC20, `renderArmorPiece`:** it always calls `renderModel(..., ArmorItem, HumanoidModel, boolean, float, float, float, String)` (up to 3 calls, the extra ones for dyeable items), then the trim and the glint.
  - `getArmorLocation` builds `"textures/models/armor/" + material.getName() + "_layer_" + n + ".png"`. With the name `frame` that resolves to `minecraft:textures/models/armor/frame_layer_1.png` (`_layer_2` for legs).
  - Forge 1.20.1 `HumanoidArmorLayer.getArmorResource(Entity, ItemStack, EquipmentSlot, String)` uses the same format: `"%s:textures/models/armor/%s_layer_%d%s.png"`, with namespace `minecraft` when the name has no `:`.
- **Hooks per loader** (all would need to go in `compat/impl` or `platform/`):
  - Fabric, both lines: `ArmorRenderer.register(ArmorRenderer, ItemLike...)`. Fabric API's `ArmorFeatureRendererMixin` cancels `renderArmorPiece` when a renderer is registered: `if (renderer != null) { renderer.render(...); ci.cancel(); }`.
  - Forge 1.20.1: `IClientItemExtensions.getHumanoidArmorModel/getGenericArmorModel`, or `Item.getArmorTexture`.
  - NeoForge 1.21.1: `RegisterClientExtensionsEvent.registerItem(IClientItemExtensions, Item...)`.
- **Trims and glint** still render if present. Placeholders have enchantability 0 and are not in `#minecraft:trimmable_armor`, so neither should occur.
- **When to hide skin parts.** `PlayerRenderer.render` calls `setModelProperties`, then posts `RenderPlayerEvent.Pre` (Forge and NeoForge bytecode: `invokevirtual setModelProperties` then `new RenderPlayerEvent$Pre … post`).
  - `setModelProperties` resets visibility every frame (`PlayerModel.setAllVisible(true)`, then `jacket`, `leftPants`, … from `isModelPartShown`). Anything we hide is restored automatically on the next frame.
  - `setModelProperties` is `private void setModelProperties(AbstractClientPlayer)` on both MC20 and MC21, so a Fabric (or common) `@Inject(at=TAIL)` is stable.
  - Fabric rendering-v1 has no pre-render player event: its `LivingEntityFeatureRenderEvents` contains only `AllowCapeRender`.
- **Interaction with the GeckoLib layer.** `GeoArmorRenderer.applyBaseModel` copies `baseModel.head/body/...visible` into itself, and the default `applyBoneVisibilityBySlot` uses those flags. Hiding the vanilla skin would therefore hide the suit too. Our override of `applyBoneVisibilityBySlot` must not read `model.xxx.visible`.
- **Alternative with no hook:** make the suit fully enclose the player and accept that the skin shows through gaps in the bare frame.
- **First person.** The hands do not draw layers (`PlayerRenderer.renderRightHand/renderLeftHand/renderHand`). Drawing the suit arm needs:
  - Forge `RenderArmEvent` (`getArm`, `getPoseStack`, `getMultiBufferSource`, `getPackedLight`, `getPlayer`);
  - NeoForge `RenderArmEvent`, which is cancellable;
  - a mixin on Fabric.

---

## 5. Loader hooks

**Answer.**

- **Entity renderers.** Architectury `EntityRendererRegistry.register(Supplier<EntityType>, EntityRendererProvider)` has the same signature on 9.2.14 and 13.0.11. `EntityModelLayerRegistry` and `EntityAttributeRegistry` are also unchanged.
- **Player layers.** Architectury has no API on either line for adding layers to existing renderers. This has to be done per loader:
  - Fabric, both lines: `LivingEntityFeatureRendererRegistrationCallback`, the same on both Fabric API versions.
  - Forge 1.20.1: `EntityRenderersEvent.AddLayers`, with player skins addressed by `String` (`"default"`, `"slim"`).
  - NeoForge 1.21.1: the same event, with skins addressed by `PlayerSkin.Model`.

  Both patched loaders make `LivingEntityRenderer.addLayer` public.
- **Item renderers** are not needed for the recommended design.

**Evidence.**

```
// Architectury 9.2.14 and 13.0.11 (javap, identical)
public static <T extends Entity> void EntityRendererRegistry.register(Supplier<? extends EntityType<? extends T>>, EntityRendererProvider<T>);
public static void EntityModelLayerRegistry.register(ModelLayerLocation, Supplier<LayerDefinition>);
public static void EntityAttributeRegistry.register(Supplier<? extends EntityType<? extends LivingEntity>>, Supplier<AttributeSupplier$Builder>);
```

Architectury's registration timing differs per loader:

- **Fabric:** `EntityRendererRegistryImpl.register` calls `EntityRendererRegistry.register(type.get(), provider)` immediately. The `EntityType` must therefore already be registered when the client init calls it.
- **Forge 9.2 and NeoForge 13:** registrations are stored and applied in `@SubscribeEvent EntityRenderersEvent.RegisterRenderers` on the mod bus. On Forge this goes through `EventBuses.onRegistered`, which is why `RedstoneExoframesForge` already calls `EventBuses.registerModEventBus` first; NeoForge uses `EventBusesHooks.whenAvailable`.

Fabric rendering-v1, `LivingEntityFeatureRendererRegistrationCallback`, identical on 3.0.10 and 5.2.1:

```
void registerRenderers(EntityType<? extends LivingEntity>, LivingEntityRenderer<?, ?>, RegistrationHelper, EntityRendererProvider.Context);
<T extends LivingEntity> void RegistrationHelper.register(RenderLayer<T, ? extends EntityModel<T>>);
// EntityRenderersMixin: player skins go through a separate redirect, called once per skin renderer:
LivingEntityFeatureRendererRegistrationCallback.EVENT.invoker().registerRenderers(EntityType.PLAYER, (LivingEntityRenderer) entityRenderer, new RegistrationHelperImpl(accessor::callAddFeature), context);
```

So on Fabric: `if (renderer instanceof PlayerRenderer pr) helper.register(new SuitLayer(pr));`. This catches both the default and the slim renderer.

`EntityRenderersEvent.AddLayers` (javap of the patched jars):

```
// Forge 47.4.29
public Set<String> getSkins();
public <R extends EntityRenderer<? extends Player>> R getPlayerSkin(String);              // new
public <R extends LivingEntityRenderer<? extends Player, ? extends EntityModel<? extends Player>>> R getSkin(String);  // deprecated
public <T extends Entity, R extends EntityRenderer<T>> R getEntityRenderer(EntityType<? extends T>);
public EntityModelSet getEntityModels(); public EntityRendererProvider$Context getContext();
// NeoForge 21.1.266
public Set<PlayerSkin$Model> getSkins();
public <R extends EntityRenderer<? extends Player>> R getSkin(PlayerSkin$Model);
public <T extends Entity, R extends EntityRenderer<T>> R getRenderer(EntityType<? extends T>);
// both patched jars: public final boolean LivingEntityRenderer.addLayer(RenderLayer<T, M>);  (vanilla MC20/MC21: protected final)
```

Forge's javadoc says: "Minecraft provides two default skin names: `default` … and `slim`". Iterate over `getSkins()`, cast to `PlayerRenderer` and call `addLayer`.

`RenderPlayerEvent.Pre` (for hiding the skin) has the same accessors on both patched loaders: `getRenderer`, `getPartialTick`, `getPoseStack`, `getMultiBufferSource` and `getPackedLight`. NeoForge's is `ICancellableEvent`.

**Client setup timing:**

- **Fabric:** `ClientModInitializer`. The repository's `RedstoneExoframesFabricClient.onInitializeClient` runs after the main entrypoint, so the common registries are already populated.
- **Forge and NeoForge:** `EnvExecutor.runInEnv(Env.CLIENT, …)` in the mod constructor, as the repository already does. Architectury defers entity renderers to `RegisterRenderers`. Listeners for `AddLayers` and `RenderPlayerEvent` must be added by us: `AddLayers` on the mod bus, `RenderPlayerEvent` on the game bus.
- **Renderer construction:** `GeoArmorRenderer`'s constructor bakes `ModelLayers.PLAYER_INNER_ARMOR` from `Minecraft.getInstance().getEntityModels()`. Build it inside the layer, which is created at renderer reload when the models are loaded, or lazily on first render. Never build it in static or common init.

---

## 6. Entity side

**Answer.**

- **Base class.** An armor-stand-like `LivingEntity` subclass, like vanilla `ArmorStand`, needs:
  - the four abstract methods `getArmorSlots`, `getItemBySlot`, `setItemSlot` and `getMainArm`;
  - `readAdditionalSaveData` and `addAdditionalSaveData` (`CompoundTag`, the same on both versions);
  - interaction via `interact` or `interactAt(Player, Vec3, InteractionHand)` (the same on both).
- **Version differences:** synced data, `getAddEntityPacket`, `EquipmentSlot.Type.ARMOR` vs `HUMANOID_ARMOR`, and `EntityType.Builder.eyeHeight`, which exists only on 1.21.1.
- **Spawn packet.** No override is needed. The vanilla `Entity.getAddEntityPacket` is concrete on both versions, and Architectury's `createAddEntityPacket` is only required for extra spawn data.
- **GeoEntity.** Implementing it forces version-split imports. Alternative: render the exoframe entity with a `LivingEntityRenderer<…, HumanoidModel>`, as vanilla `ArmorStandRenderer` does, plus the same suit layer as the player. Then the entity does not need to be a GeoAnimatable, and one GeckoLib renderer serves both the player and the stand. Its animation instances are keyed by entity id through `getInstanceId`.

**Evidence (javap).**

```
MC20 Entity: protected abstract void defineSynchedData();
             public Packet<ClientGamePacketListener> getAddEntityPacket();
MC21 Entity: protected abstract void defineSynchedData(SynchedEntityData$Builder);
             public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity);
MC20 SynchedEntityData: public <T> void define(EntityDataAccessor<T>, T);    defineId(Class<? extends Entity>, EntityDataSerializer<T>)
MC21 SynchedEntityData$Builder: public <T> Builder define(EntityDataAccessor<T>, T);   defineId(Class<? extends SyncedDataHolder>, EntityDataSerializer<T>)
EntityType$Builder (both): of(EntityFactory<T>, MobCategory), sized(float,float), noSummon(), fireImmune(), clientTrackingRange(int), build(String)
MC21 only: eyeHeight(float)
LivingEntity (both): static AttributeSupplier$Builder createLivingAttributes();  (ArmorStand.createAttributes() only on MC21)
EquipmentSlot$Type: MC20 {HAND, ARMOR}; MC21 {HAND, HUMANOID_ARMOR, ANIMAL_ARMOR}
Architectury 9.2.14: public static Packet<ClientGamePacketListener> NetworkManager.createAddEntityPacket(Entity);
   forge impl: return (Packet) NetworkHooks.getEntitySpawningPacket(entity);  fabric impl: return SpawnEntityPacket.create(entity);
Architectury 13.0.11: public static Packet<ClientGamePacketListener> NetworkManager.createAddEntityPacket(Entity, ServerEntity);
   both impls: return SpawnEntityPacket.create(entity, serverEntity);
both: interface EntitySpawnExtension { void saveAdditionalSpawnData(FriendlyByteBuf); void loadAdditionalSpawnData(FriendlyByteBuf); }
```

**Attributes.** Register them with `EntityAttributeRegistry.register(() -> TYPE, ExoframeEntity::createAttributes)` from common init. The signature is the same on both versions. Inside, use `LivingEntity.createLivingAttributes()`, which exists on both versions.

**GeoEntity on the entity**, if wanted: `getAnimatableInstanceCache` and `registerControllers` use the moved types from §1, so that part must sit in a `compat/impl` base class.

---

## 7. Against docs/audit/mjolnir-armor-audit.md §4 and §9

**Confirmed:**

- §4: "Package GeckoLib `software.bernie.geckolib.core.*` (4.4.9/4.8.4) → radice (4.9.3)" is confirmed, and extends further than listed. It also covers `PlayState`, `Color`, `DataTicket`, the `RenderUtils` → `RenderUtil` rename, and the `DyeableGeoArmorRenderer` → `renderer.specialty` move.
- §4: "GeckoLib GeoArmorRenderer/GeoBlockRenderer.preRender colore → int" is confirmed for every render hook, not only `preRender`.
- §4: "`Model.renderToBuffer(..., float r,g,b,a)` → `int color`" is confirmed (MC20/MC21 javap above). This is why the recommended layer calls `defaultRender` instead.
- §4: `defineSynchedData()` → `defineSynchedData(Builder)` and `EquipmentSlot.Type.ARMOR` → `HUMANOID_ARMOR` are confirmed.
- §9 rendering: the `armorX` bone convention, `setHidden` + `setChildrenHidden`, and "Layer GeckoLib sul giocatore che legge lo stato" are all confirmed as feasible with the real API.
- §9: `DyeableGeoArmorRenderer` for paints is confirmed: it exists on both versions with the same abstract methods. Note that `getColorForBone` is evaluated on every render.
- §9: "texture vanilla trasparenti per i segnaposto" is confirmed as necessary on 1.20.1. On 1.21.1 an empty `layers()` list is cleaner (see §4).
- §9: "PlayerModelHider (mixin su Fabric)" is confirmed: Fabric API has no pre-render player event. Forge and NeoForge can use `RenderPlayerEvent.Pre`, which fires after `setModelProperties`.
- §9 step 1, "Decide se i segnaposto sono GeoItem (raccomandato: no)", is confirmed.
  - On 4.9.3, GeckoLib's `HumanoidArmorLayerMixin` would route any `GeoItem` in an armor slot through `GeoRenderProvider.of(item).getGeoArmorRenderer(...)`.
  - On Fabric 4.8.4, `MixinHumanoidArmorLayer` would route it through `RenderProvider.of(stack)`.
  - Forge 4.8.4 has no mixin at all and relies on `IClientItemExtensions`.

  So placeholders as GeoItems would split behaviour three ways. Use a separate, never-equipped suit-model GeoItem instead.

**Contradicted or worth re-checking:**

- §4 and `compat.md`: "`ArmorMaterial.getName()` con namespace: rompe `HumanoidArmorLayer` su Fabric 1.20.1". With Fabric API present that is not what the jar shows. Fabric API 0.92.12 rendering-v1 `ArmorFeatureRendererMixin.getArmorTexture` injects at HEAD and handles `ns:path`:

  ```
  final int separator = name.indexOf(ResourceLocation.NAMESPACE_SEPARATOR);
  if (separator != -1) { ... String.format("%s:textures/models/armor/%s_layer_%d%s.png", namespace, path, ...) ...}
  ```

  Forge's `getArmorResource` also splits the namespace. The un-namespaced name stays harmless, but it forces the transparent textures into the `minecraft` namespace on 1.20.1. A namespaced name would let them live in `redstone_exoframes`. This needs an in-game test before relying on it.
- §9 step 2 (attachments per loader) is superseded by the existing `SavedData`-based `FrameStateAccess`, as `compat.md` records.
- `compat.md` ("Sincronizzazione solo del proprio stato") still applies: drawing other players' suits needs their state on the client.

---

## Proposed compat/client seams

These are the minimal seams that hide every difference above. Stonecutter conditions live only in the `impl` classes.

1. **`compat/client/SuitRenderer`** (interface), implemented by `compat/impl/client/SuitRendererImpl extends [renderer|renderer.specialty].DyeableGeoArmorRenderer<SuitModelItem>`. It exposes `render(LivingEntity, HumanoidModel<?> base, PoseStack, MultiBufferSource, int light, float partialTick, SuitView view)`, where `SuitView` is a version-neutral record of visible pieces, colours and texture per pass. Underneath it hides:
   - the package of `DyeableGeoArmorRenderer` and `Color`;
   - the float-vs-int colour hooks;
   - the deprecated 4-argument `prepForRender` vs the 10-argument form;
   - the multi-pass `defaultRender` per texture.
2. **`compat/impl/client/SuitModelItem extends Item implements GeoItem`** plus its `GeoModel`. Underneath it hides:
   - the packages of `GeoAnimatable`, `AnimatableInstanceCache` and `ControllerRegistrar`;
   - Fabric 4.8.4's abstract `createRenderer`/`getRenderProvider` (return `RenderProvider.DEFAULT`), against Forge 4.8.4 (no methods) and 4.9.3 (defaults).

   Common code sees only a `Supplier<Item>`.
3. **`client/SuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>`**, common code with no conditions. It reads the client frame state, builds a `SuitView` and calls `SuitRenderer`. A twin layer for the exoframe entity renderer reuses the same `SuitRenderer`. `RenderLayer.render` has the same signature on both versions.
4. **`compat/ClientRenderHooks`** (interface), implemented by `compat/impl/client/ClientRenderHooksImpl`. It provides:
   - `addPlayerLayer(Function<PlayerRenderer, RenderLayer<…>>)`: Fabric `LivingEntityFeatureRendererRegistrationCallback` vs Forge `AddLayers.getSkins()` with `String` and `getPlayerSkin` vs NeoForge `getSkins()` with `PlayerSkin.Model` and `getSkin`; mod-bus wiring;
   - `onPlayerPreRender(BiConsumer<AbstractClientPlayer, PlayerModel<?>>)`: Forge and NeoForge `RenderPlayerEvent.Pre` on the game bus vs a Fabric mixin on `PlayerRenderer.setModelProperties` TAIL (or one common mixin);
   - optionally `onFirstPersonArm(...)`: `RenderArmEvent` on Forge and NeoForge vs a Fabric mixin;
   - `registerEntityRenderer(...)`: a thin pass-through to Architectury `EntityRendererRegistry`, with no differences; it is listed only for timing on Fabric, where `type.get()` is called immediately.
5. **`compat/CompatLivingEntity`** (abstract base in `compat/impl`, exposed as an abstract class in `compat`). It provides:
   - `defineSynchedData()` vs `defineSynchedData(Builder)`, behind a neutral `defineData(DataSink)`;
   - `getAddEntityPacket()` vs `getAddEntityPacket(ServerEntity)`, only if extra spawn data is ever needed (Architectury `createAddEntityPacket(Entity)` vs `(Entity, ServerEntity)`);
   - `EquipmentSlot.Type.ARMOR` vs `HUMANOID_ARMOR`;
   - eye height: `EntityType.Builder.eyeHeight` on 1.21.1 vs an override on 1.20.1.
6. **`ArmorMaterials` (existing).**
   - 1.21.1: `layers = List.of()` so the placeholders draw nothing.
   - 1.20.1: transparent `frame_layer_1.png` and `frame_layer_2.png`. Today they would go in the `minecraft` namespace because of the un-namespaced name; a namespaced name, if retested, would allow `redstone_exoframes`. This is a data and enum change only, with no new interface.
7. **`Ids` (existing).** A tiny helper for GeckoLib `ResourceLocation`s is optional. `DefaultedGeoModel`'s path scheme is identical on both versions, so the existing `Ids.of` is enough.

**Not answerable from the jars, needs an in-game test:**

- whether the GeckoLib armor template pivots line up with the player model at runtime, including the slim arms (`applyBaseTransformations` uses the same offsets for both skins);
- how expensive several `defaultRender` passes per frame are;
- whether Fabric 1.20.1 really breaks with a namespaced material name once Fabric API is present (the jar says it does not);
- whether the vanilla `ClientboundAddEntityPacket` alone is enough for the modded entity on Forge 1.20.1 (expected yes, since `getAddEntityPacket` is concrete and no extra spawn data is used).
