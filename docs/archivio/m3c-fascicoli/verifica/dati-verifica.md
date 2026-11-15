# Verification of dati.md (data formats per version, generator)

Independent re-check, read-only. Jars: V120 / V121 = vanilla merged 1.20.1 / 1.21.1; FG = forge-1.20.1-47.4.29-minecraft-merged (Mojang names); FGS = its `-sources.jar` (Forge-own classes only); NF = neoforge-21.1.266-minecraft-merged. Fabric API module lists derived from the two `fabric-api` POMs (0.92.12+1.20.1: 44 jars, 0.116.18+1.21.1: 43 jars). Work files: `verifica/tmp-verifica-dati/`. (Note: `verifica/tmp-dati/` is the dossier author's own folder; I wrote one stray file there by mistake and removed it at once; nothing of theirs was changed.)

Result: 14 claims, none refuted. C14 is confirmed except for one part that was not checked: I re-ran gen_data.py but not check_data.sh. Several claims need caveats, listed under each claim and in "Missed".

## C1 — folder names: CONFIRMED
- [javap V120] `RecipeManager.<init>()` @4 `ldc "recipes"`; [javap FG] `"recipes"` sits in `<init>(ICondition$IContext)` @4. `LootDataType` clinit: `"loot_tables"` @87 on V120 and FG. `ServerAdvancementManager(LootDataManager)` @4 `"advancements"`; FG also has `(LootDataManager, ICondition$IContext)`. `TagManager.<clinit>` puts `Registries.BLOCK`→`"tags/blocks"`, `Registries.ITEM`→`"tags/items"`, plus entity_types, fluids and game_events. The FG getstatic/ldc pairs were read in order.
- [javap V121, NF] `RecipeManager(HolderLookup$Provider)` @4-7 and `ServerAdvancementManager(HolderLookup$Provider)` @4-7 call `Registries.elementsDirPath(Registries.RECIPE / ADVANCEMENT)`. `TagManager` calls `Registries.tagsDirPath`. `ReloadableServerRegistries` calls `elementsDirPath`, and `LootDataType` uses `Registries.LOOT_TABLE`. `Registries.<clinit>` creates the keys `recipe`, `loot_table`, `advancement`, `block` and `item` on both jars. `tagsDirPath` concatenates the string `"tags/\u0001"`. On V121 `elementsDirPath` is `location().getPath()`; on NF it is `CommonHooks.prefixNamespace`, which returns the bare path for the `minecraft` namespace (bytecode @0-17).
- Spelling scan (my own Python scan, exact Utf8 constant-pool entries, classes under `net/minecraft/data/` excluded):
  - V121 and NF: no `loot_tables`, `tags/blocks` or `tags/items`. `recipes` and `advancements` appear only as member names, for example the field `ServerAdvancementManager.advancements` and the getters of `ReloadableServerResources`.
  - V120 and FG: no `tags/block` or `tags/item`. `loot_tables`, `tags/blocks` and `tags/items` each appear only in the one class named above.
  - Fabric API, both versions: no folder use. The hits are `ResourceReloadListenerKeys` (reload-listener ids `recipes`/`advancements`, 1.21.1), a `"tags/"` prefix parser in `TagRemovalInternals`, and mixin member names.

## C2 — shaped recipe form: CONFIRMED (vanilla only; see Missed 1)
- [data] I diffed the sorted-key JSON of piston, stick (count 4, tag, group), torch (array) and crafting_table. The only differences are `result` (`{"item"}`/`{"count","item"}` on 1.20.1, always `{"count","id"}` on 1.21.1) and `show_notification`: on 1.21.1 it is written only when false.
- [javap V120 `ShapedRecipe$Serializer.fromJson`] `group` default `""`; `category` read with `CraftingBookCategory.CODEC.byName(...,MISC)`; `show_notification` read with `getAsBoolean(...,true)`. FG has the same `group` and `show_notification` defaults. [javap V120 `ShapedRecipe.itemStackFromJson`]: a `data` key throws; `count` is read with `getAsInt(...,1)` and must be ≥1; the method returns `new ItemStack(ItemLike,int)`.
- [javap V121 = NF, codec lambda]: `group` `optionalFieldOf("group","")`; `category` `fieldOf().orElse(MISC)`; `ShapedRecipePattern.MAP_CODEC`; `ItemStack.STRICT_CODEC.fieldOf("result")`; `show_notification` `optionalFieldOf(…,true)`. `ItemStack.CODEC` (`method_57371`): `id` (`ITEM_NON_AIR_CODEC`); `ExtraCodecs.intRange(1,99).fieldOf("count").orElse(1)`; `DataComponentPatch.CODEC.optionalFieldOf("components", EMPTY)`.
- Caveat [not checked]: because `count` uses `orElse(1)`, an out-of-range `count` (0, or above 99) on 1.21.1 probably falls back to 1 instead of failing. I did not confirm the DFU `orElse` semantics.

## C3 — result with data: CONFIRMED
- [arch-like: FGS `net/minecraftforge/common/crafting/CraftingHelper.java:183-205`] `getItemStack(JsonObject, boolean readNBT, boolean disallowsAirInRecipe)` reads an optional `nbt` (via `getNBT`) and a `count` default of 1 (written into the stack tag as `Count`). [javap FG] `ShapedRecipe.itemStackFromJson` = `CraftingHelper.getItemStack(json, true, true)`.
- [javap V120] has no `nbt` read. fabric-recipe-api-v1 1.0.25 mixins: `IngredientMixin`, `PacketEncoderMixin` and `ShapelessRecipeMixin` only [data: its mixins.json]. No 1.20.1 Fabric API refmap names `class_1869`; I confirmed that `class_1869` is ShapedRecipe because it is the class holding "Disallowed data tag found" in the intermediary jar.
- [code] `ItemDataAccessImpl.java:32-35` → `ItemPayload.read`, which returns null without data on both Stonecutter branches (`ItemPayload.java:22-33`), so the energy is 0. `PieceItem.java:56-58` returns `Piece.fresh`. `ExoframeItem.java:49` builds `new FrameState(...)`.
- Also true but not in the claim: the dossier names `CellItem.java:24-28`. `toCell` takes `cellEnergy`, which is 0 without data.

## C4 — count vs max stack size: CONFIRMED
- [javap V121] `validateStrict` @30-50 compares `getCount() > getMaxStackSize()` and returns `DataResult.error`. Its lambda `method_57384` concatenates through bootstrap #18, whose recipe is `"Item stack with stack size of \u0001 was larger than maximum: \u0001"`. [javap NF] has the same code; there `getMaxStackSize()` delegates to `Item.getMaxStackSize(ItemStack)` (a NeoForge extension that is still component-based). Both use `STRICT_CODEC` for `result`. 1.20.1 only rejects a count below 1 (see C2).
- [code] `ModItems.java:19,22,66` all use `stacksTo(1)`.

## C5 — unlock advancement form: CONFIRMED
- [data] `recipes/redstone/piston` and `recipes/misc/stick` on both versions match the dossier's text. `recipes/root` exists on both versions (a single `minecraft:impossible` criterion).
- [javap V120, FG] `ItemPredicate.fromJson` reads `items` with `GsonHelper.getAsJsonArray(obj,"items",null)`, so `items` must be an array. FG first checks for a `type` key, which is a Forge custom predicate.
- [javap V121, NF] `ItemPredicate`: `RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("items")`. `HolderSetCodec` = `either(TagKey.hashedCodec, either(listOf, element))`. NF has one more `either`, for NeoForge's custom holder-set objects.
- [proto] The dossier's generator, re-run by me, produces exactly the stated form; for example the 1.21.1 `charger.json` advancement.

## C6 — tag criterion, requirements default: CONFIRMED
- [data] stick: `{"tag":"minecraft:planks"}` on 1.20.1, `{"items":"#minecraft:planks"}` on 1.21.1.
- [javap V121, NF] the `ItemPredicate` codec strings are exactly `items`, `count`, `components` and `predicates`, and `items` is optional, so an unknown `tag` key leaves `items` empty. "Matches any item" is inferred from the empty optional; I did not trace `InventoryChangeTrigger` [not checked].
- [javap V121, NF `Advancement` codec] `requirements` `optionalFieldOf`, then `orElseGet` → `AdvancementRequirements.allOf`; `sends_telemetry_event` `optionalFieldOf(…, false)`. [javap V120 `Advancement$Builder.fromJson`]: `requirements` read with `getAsJsonArray(...,null)`; @239-302, when its length is 0 one group is built per key of `criteria.keySet()`; telemetry `getAsBoolean(…,false)` @543.

## C7 — loot tables: CONFIRMED
- [data, md5] `blocks/iron_block.json` is identical (`d074ca5a…`); furnace differs only in its function (`copy_name` vs `copy_components` + `include:["minecraft:custom_name"]`).
- [javap] `BlockBehaviour.getLootTable()` (final): it returns `ResourceLocation` on V120 (@22 `"blocks/"` `withPrefix`) and `ResourceKey<LootTable>` on V121 (@25).
- `random_sequence` is optional in both parsers: V120 `LootTable$Serializer` reads it under `has(...)`; V121 and NF read it with `optionalFieldOf`.
- `LootItemFunctions` strings:
  - V120 and FG: `set_nbt`, `copy_name`, `copy_nbt`, `copy_state`.
  - V121 and NF: `set_custom_data`, `set_components`, `copy_name`, `copy_custom_data`, `copy_state`, `copy_components`.

## C8 — tag files: CONFIRMED
- [javap `TagFile` on all four jars] `values` `fieldOf`; `replace` `optionalFieldOf(…, false)`. FG and NF add an optional `remove`.
- `BlockTags.MINEABLE_WITH_PICKAXE` and `NEEDS_STONE_TOOL` exist in all four jars.
- [data] `tags/blocks/{needs_stone_tool,mineable/pickaxe}.json` (V120) and `tags/block/…` (V121) exist. V121 `incorrect_for_wooden_tool` = `#needs_diamond_tool`, `#needs_iron_tool`, `#needs_stone_tool`.
- NF ships additive `incorrect_for_*_tool` files that add `#neoforge:needs_netherite_tool` (no `replace`), so the `needs_stone_tool` reference survives on NF.

## C9 — missing tag reference; drops without a mineable tag: CONFIRMED on all four (the dossier checked two)
- [javap TagLoader, all four jars] `build(Map)` → `Either.ifLeft(log)` / `ifRight(map.put)`, so a tag with missing references is never stored. The message is `"Couldn't load tag {} as it is missing following references: {}"` with `Logger.error(String,Object,Object)`.
- The joiner of the missing references differs: V120 and V121 `", "`; FG `", \n\t"`; NF prefix and separator `"\n\t"`. On Forge and NeoForge the id therefore lands on a continuation line (see Missed 3).
- [javap] `Player.hasCorrectToolForDrops` is `!requiresCorrectToolForDrops() || selected.isCorrectToolForDrops(state)` on V120, FG, V121 and NF. On NF it is followed by `EventHooks.doPlayerHarvestCheck`.
  - V120 `DiggerItem.isCorrectToolForDrops` ends with `state.is(this.blocks)`.
  - FG: `isCorrectToolForDrops(ItemStack,BlockState)` = `state.is(blocks) && TierSortingRegistry.isCorrectTierForDrops`, and the one-argument version also needs `state.is(blocks)`.
  - V121 and NF `Tool.isCorrectForDrops`: the first rule whose `HolderSet` holds the state wins; otherwise `iconst_0`.
  - Fabric 1.20.1: `fabric-mining-level-api-v1` `MiningToolItemMixin` injects into `DiggerItem.isCorrectToolForDrops` and can only `setReturnValue(false)` (when the tool level is below the required level). It never grants true.
- Conclusion: a block in no `mineable/*` tag drops nothing on all four variants.

## C10 — assets version-independent: CONFIRMED
- [data, md5] `blockstates/furnace.json`, `models/block/furnace.json`, `furnace_on.json`, `orientable.json`, `models/item/furnace.json` and `atlases/blocks.json` are identical between V120 and V121.
- The folders `blockstates/`, `models/block|item/` and `textures/block|item/` are present in both jars.

## C11 — pack_format: CONFIRMED
- [code] `versions/*/gradle.properties:3` = 15/15/34/34. `pack.mcmeta` holds `${pack_format}`. `build.gradle.kts:200` (props) and `:207-209` (`filesMatching … expand`). `tools/check_jars.sh` section (b) at about line 71.
- [javap -constants `SharedConstants`] V120 15/15; V121 `RESOURCE_PACK_FORMAT=34`, `DATA_PACK_FORMAT=48`.
- Callers of `isCompatible` (my scan of the constant pools):
  - Server-side pack checks happen only in V120 `MinecraftServer.method_46220`, FG `MinecraftServer.lambda$fillSystemReport$17` (the system report string), and V121 and NF `PackRepository.method_59808`.
  - The other callers are `LevelSummary.isCompatible` (world version, in `Main` and `WorldOpenFlows`) and client or Realms UI.

## C12 — lang: CONFIRMED
- [javap, all four jars] `Block.getDescriptionId()` → `Util.makeDescriptionId("block", key)`; `BlockItem.getDescriptionId()` → `getBlock().getDescriptionId()`.
- [code] The two lang files: 38 = 38 keys, same keys in the same order. Families: item 11, commands 14, message 9, entity 1, itemGroup 1, key 1, key.categories 1. No `block.` key.
- Grep of `src/test`, `.github` and `tools` (excluding `node_modules`): nothing reads the lang files.

## C13 — ingredient ids and conventional tags: CONFIRMED
- [javap Items / Blocks constant pools, data] All eight ids have their registration string, an item model and vanilla recipe uses on both versions. iron_block, redstone_block and copper_block are registered in `Blocks`.
- Conventional tags:
  - FG: `data/forge/tags/items/ingots/copper.json`.
  - fabric-convention-tags-v1 1.5.6: `data/c/tags/items/copper_ingots.json`.
  - NF and fabric-convention-tags-v2 2.12.0: `data/c/tags/item/ingots/copper.json`.

## C14 — build and checks: CONFIRMED, except the check_data.sh run (not checked)
- [code] `build.gradle.kts:180-181` adds the `srcDir`. `processResources` runs from `:189` to `:210`, with `expand` limited to the four files.
- `check_jars.sh`, `check_sources.sh` and `check_jar_privacy.py` have no rule on `data/` contents (grep).
- `run_gametests.sh:43` → `check_log.py`, which requires every WARN/ERROR/FATAL record matching `exoframe` (case-insensitive, including its attached trace lines) to be in the allowlist.
- [javap] Every parse error carries the resource id and is logged with `Logger.error`:
  - `Parsing error loading recipe {}` (V121, NF).
  - `Parsing error loading custom advancement {}: {}` (V120, FG, V121, NF).
  - `Couldn't parse element` in `LootDataType`. Its text differs per jar: V120 and FG `{}:{}`, V121 `{}/{} - {}`, NF `{}:{} - {}`.
- [proto] I ran `tmp-dati/proto/gen_data.py --out` twice: the two trees are byte-identical, with the 14 stated files.
- [not checked] I did not run `check_data.sh` (its stale and orphan detection).

## Missed (what a planner could be misled by)
1. **Forge and NeoForge replace vanilla data.**
   - FG ships 178 `data/minecraft/recipes/*` overrides; NF ships 191 `data/minecraft/recipe/*`, 29 loot tables, 9 advancements and 15 tags.
   - For example, the FG piston uses `forge:cobblestone/normal` and `forge:ingots/iron`; the NF piston uses `c:cobblestones/normal` and `c:ingots/iron`.
   - So "the vanilla example" is what Fabric runs, not what Forge or NeoForge run. The forms are unaffected, but any test or selfcheck that compares the game's loaded recipe with the vanilla jar's JSON will differ on the two non-Fabric loaders.
2. **The tool rule was not checked on all loaders.** The dossier marked Forge and NeoForge's tool rules [not checked] and named no Fabric 1.20.1 hook. I checked them: Forge `TierSortingRegistry`, NF `EventHooks.doPlayerHarvestCheck`, and Fabric 1.20.1 `fabric-mining-level-api-v1` `MiningToolItemMixin`. None of them makes a block that is in no mineable tag drop, so the "mineable tag is mandatory" rule holds on all four.
3. **The missing-reference id is on a continuation line on Forge and NeoForge.** The joiner is `", \n\t"` on FG and `"\n\t"` on NF, so the first log line names only the vanilla tag (`minecraft:mineable/pickaxe`) and the misspelled `redstone_exoframes:` id sits on following tab-indented lines. `check_log.py` still catches it, but only because its `TRACE` regex (`^\s+\S`) attaches those lines to the record. Any future allowlist entry or log grep that looks at the first line alone would miss it.
4. **The loot error text differs per jar** (V121 `{}/{} - {}`, NF `{}:{} - {}`, 1.20.1 `{}:{}`). An allowlist or assertion keyed on the message text must not assume one form.
5. **1.21.1 `count` falls back silently** [not checked in DFU]. `count` is `intRange(1,99).fieldOf("count").orElse(1)`, so a typo such as 0 or 100 probably becomes 1 instead of failing; only 2..99 above the max stack size is rejected by `validateStrict`. `validate()` in the generator should hold the whole rule, not rely on the game. The prototype only checks 1..64 and does not enforce "1 for unstackable items", although the dossier says it does.
6. **Category fallback is silent on 1.21.1 too.** The dossier stresses it for 1.20.1 only, but the 1.21.1 `category` field also uses `orElse(MISC)`, so an unknown recipe-book category is silent on both versions. Only the generator can catch it.
7. **`TagFile.remove` (Forge and NeoForge only) must not be emitted.** It is ignored by vanilla and Fabric on both versions, so a generator that writes `remove` would behave differently per loader within one Minecraft version. The dossier mentions the key but not this consequence for a per-version (not per-loader) generator.
8. **Display-only incompatibility is wider than stated.** Besides the server system report, the mod's 1.21.1 data pack (34 against 48) is also judged by the client pack-selection UI (`TransferableSelectionList$PackEntry`). This is display only, but on the PC checklist the pack may appear as "made for an older version" in the data-pack screen; the dossier concedes it was not observed.
