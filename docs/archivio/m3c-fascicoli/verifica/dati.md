# M3c dossier — data and assets per Minecraft version (recipes, unlock advancements, loot, tags, blockstates/models, lang)

Read-only investigation, 15 November 2026. Nothing was built or run except `unzip`, single-class `javap` and Python over the jars.
Jar labels: **V120** / **V121** = vanilla merged 1.20.1 / 1.21.1 (`minecraft-merged-<ver>-loom.mappings…jar`); **FG** = `forge-1.20.1-47.4.29-minecraft-merged` (Mojang names); **NF** = `neoforge-21.1.266-minecraft-merged`. The Fabric variants run the V120 / V121 classes plus Fabric API (0.92.12+1.20.1, 0.116.18+1.21.1).
Evidence tags: [code] repo file:line; [javap] jar + class + member; [data] file inside a jar; [proto] prototype run under `verifica/tmp-dati/proto/`; [other recon] proved by a real build in `ricognizione-movimento-e-dati.md` §2, not re-run here; [not checked].
Repo state: the working tree is `d88e1e0` on `m3c-giocarci` = `2e35aa6` + two files under `docs/superpowers/specs/ricognizioni/` (no code or resource change) [code: `git diff --stat 2e35aa6..HEAD`].

## 1. Shaped crafting recipes

**Folder and id.** 1.20.1 `data/<ns>/recipes/<name>.json`, 1.21.1 `data/<ns>/recipe/<name>.json`; id `<ns>:<name>`; vanilla keeps them flat (1174 / 1290 files, no subfolder) [data; path constants in §6].

**The same vanilla recipes on both versions** [data; the whole diff of `piston.json` is the `result` object and the `show_notification` line]:

| | 1.20.1 | 1.21.1 |
|---|---|---|
| piston (tag ingredient `T`) | `{"type":"minecraft:crafting_shaped","category":"redstone","key":{"#":{"item":"minecraft:cobblestone"},"R":{"item":"minecraft:redstone"},"T":{"tag":"minecraft:planks"},"X":{"item":"minecraft:iron_ingot"}},"pattern":["TTT","#X#","#R#"],"result":{"item":"minecraft:piston"},"show_notification":true}` | identical up to `"pattern"`, then `"result":{"count":1,"id":"minecraft:piston"}}` |
| iron_chestplate | `…"category":"equipment","key":{"X":{"item":"minecraft:iron_ingot"}},"pattern":["X X","XXX","XXX"],"result":{"item":"minecraft:iron_chestplate"},"show_notification":true` | `…"result":{"count":1,"id":"minecraft:iron_chestplate"}` |
| stick (count > 1, tag, group) | `…"category":"misc","group":"sticks","key":{"#":{"tag":"minecraft:planks"}},"pattern":["#","#"],"result":{"count":4,"item":"minecraft:stick"},"show_notification":true` | `…"result":{"count":4,"id":"minecraft:stick"}` |
| torch (alternatives) | `"X":[{"item":"minecraft:coal"},{"item":"minecraft:charcoal"}]` | the same |

**Every difference** (survey of all shaped recipes: 590 on 1.20.1, 634 on 1.21.1) [data]:
1. folder `recipes` → `recipe`;
2. result key `item` → `id`; `count` is omitted when 1 on 1.20.1 (235 results `{item}`, 355 `{count,item}`) and always written on 1.21.1 (634 `{count,id}`);
3. `show_notification` is written in all 590 files on 1.20.1 (589 `true`; `false` only in `crafting_table`); on 1.21.1 only `crafting_table` has it (`false`);
4. nothing else: `type`, `category` (`building` / `redstone` / `equipment` / `misc`), optional `group`, `pattern`, `key` have the same form. Ingredient forms: `{"item":id}` (815 / 882 uses), `{"tag":id}` (58 / 58), an array of such objects (11 / 12); no bare-string ingredient on 1.21.1.

**Parsers** (required fields and defaults):
- 1.20.1 [javap V120 `ShapedRecipe$Serializer.fromJson(ResourceLocation, JsonObject)`]: `group` default `""`; `category` default `MISC` (`CraftingBookCategory.CODEC.byName(name, MISC)`: an unknown name silently becomes misc); `key`, `pattern`, `result` required; `show_notification` default `true`. `ShapedRecipe.itemStackFromJson(JsonObject)`: `item` (string); a `data` key throws "Disallowed data tag found"; `count` default 1; returns `new ItemStack(item, count)` — no NBT is read and any other key is ignored.
- 1.20.1 Forge [javap FG]: the body of `ShapedRecipe.itemStackFromJson` is `net.minecraftforge.common.crafting.CraftingHelper.getItemStack(JsonObject, true, true)`: `item`, `count` default 1, optional **`nbt`** (JSON object or SNBT string; `CraftingHelper.getNBT` → `TagParser.parseTag`).
- 1.21.1 [javap V121 and NF `ShapedRecipe$Serializer` codec lambda, same strings in both]: `group` `optionalFieldOf("group", "")`, `category` `fieldOf("category").orElse(MISC)`, `ShapedRecipePattern.MAP_CODEC` (`key`, `pattern`), `ItemStack.STRICT_CODEC.fieldOf("result")`, `show_notification` `optionalFieldOf(…, true)`. `ItemStack.CODEC` [javap V121, NF]: `id` required (not air), `count` `ExtraCodecs.intRange(1, 99).fieldOf("count").orElse(1)`, `components` `DataComponentPatch.CODEC.optionalFieldOf("components", EMPTY)`. `STRICT_CODEC` adds `ItemStack.validateStrict` (`getCount() > getMaxStackSize()` → "Item stack with stack size of N was larger than maximum: M") — **a result count above the item's max stack size fails to load on 1.21.1** (1.20.1 only rejects a count below 1). `exoframe`, `redstone_cell` and the pieces are `stacksTo(1)` [code `content/ModItems.java:19,22,66`]: their recipes need count 1.
- Pattern rules, same messages on both [javap V120 `ShapedRecipe`, V121 `ShapedRecipePattern$Data`]: at most 3 rows × 3 columns, rows of equal width, a key symbol is one character and not a space, every key symbol is used, every pattern symbol is defined.
- A file that fails to parse (a broken rule, or the other version's result form: `id` on 1.20.1, `item` on 1.21.1) is logged `Parsing error loading recipe <id>` with `Logger.error` and skipped; the game goes on [javap `RecipeManager`, four jars].

**Result with extra data.** No vanilla recipe of either version has a result key other than `item` / `id` / `count` [data]. The forms:
- 1.21.1, both loaders: `"result":{"id":"redstone_exoframes:redstone_cell","count":1,"components":{"minecraft:custom_data":{"exoframes":{"energy":10000}}}}`. `CustomData.CODEC = Codec.withAlternative(CompoundTag.CODEC, TagParser.AS_CODEC)` [javap V121], so the value may also be the SNBT string `"{exoframes:{energy:10000}}"`. In the JSON-object form `JsonOps.convertTo` picks the narrowest number type [javap datafixerupper 8.0.16]: 10000 becomes a ShortTag, the SNBT string gives an IntTag; the mod reads with `CompoundTag.getInt` [code `compat/impl/PieceTags.java:90-92`], which takes both.
- 1.20.1 Forge: `"result":{"item":"redstone_exoframes:redstone_cell","count":1,"nbt":{"exoframes":{"energy":10000}}}` [javap FG; not run].
- 1.20.1 Fabric: **not expressible** in a `minecraft:crafting_shaped` file: the vanilla parser never reads `nbt` (silently ignored), and Fabric API's recipe module (fabric-recipe-api-v1 1.0.25) mixes only into `Ingredient`, `ShapelessRecipe` and a packet encoder [data: jar listing]. The same file would give a stack with data on Forge and without on Fabric.
- The 3c recipes do not need it: a cell stack without data is an empty cell (`ItemDataAccessImpl.cellEnergy` returns 0) [code `content/CellItem.java:24-28`, `compat/impl/ItemDataAccessImpl.java:32-35`]; a piece stack without data is a fresh piece [code `content/PieceItem.java:56-58`]; the exoframe item builds a new `FrameState` whatever the stack holds [code `content/ExoframeItem.java:49`].

## 2. Recipe-unlock advancements

**Folder.** 1.20.1 `data/<ns>/advancements/recipes/<category>/<name>.json`, 1.21.1 `data/<ns>/advancement/recipes/<category>/<name>.json`: only the first folder changes; the inner `recipes/<category>/` is the same on both [data: 1161 / 1277 files]. `<category>` is the datagen folder — `building_blocks`, `decorations`, `redstone`, `transportation`, `tools`, `combat`, `food`, `brewing`, `misc` [javap `net.minecraft.data.recipes.RecipeCategory`, both] (vanilla: `redstone/piston`, `combat/iron_chestplate`, `decorations/furnace`) — not the recipe's `category` field.

**The same advancement on both versions** [data: `…/recipes/redstone/piston.json`]:
```
1.20.1  {"parent":"minecraft:recipes/root","criteria":{"has_redstone":{"conditions":{"items":[{"items":["minecraft:redstone"]}]},"trigger":"minecraft:inventory_changed"},"has_the_recipe":{"conditions":{"recipe":"minecraft:piston"},"trigger":"minecraft:recipe_unlocked"}},"requirements":[["has_redstone","has_the_recipe"]],"rewards":{"recipes":["minecraft:piston"]},"sends_telemetry_event":false}
1.21.1  {"parent":"minecraft:recipes/root","criteria":{"has_redstone":{"conditions":{"items":[{"items":"minecraft:redstone"}]},"trigger":"minecraft:inventory_changed"},"has_the_recipe":{"conditions":{"recipe":"minecraft:piston"},"trigger":"minecraft:recipe_unlocked"}},"requirements":[["has_the_recipe","has_redstone"]],"rewards":{"recipes":["minecraft:piston"]}}
```
With a tag [data: `…/recipes/misc/stick.json`]: 1.20.1 `"items":[{"tag":"minecraft:planks"}]`; 1.21.1 `"items":[{"items":"#minecraft:planks"}]`.

**Every difference** (survey of all files under `recipes/`) [data]:
1. first folder `advancements` → `advancement`;
2. item predicate of `minecraft:inventory_changed`: 1.20.1 `{"items":[id]}` (1233 uses, always a one-element array) or `{"tag":id}` (40); 1.21.1 `{"items":id}` (1350, a string) or `{"items":"#tag"}` (40); there is no `tag` key on 1.21.1;
3. `"sends_telemetry_event": false` is in every 1.20.1 file and in no 1.21.1 file;
4. the order inside the single `requirements` group (irrelevant).
Unchanged: `parent` `minecraft:recipes/root` (exists on both; one `minecraft:impossible` criterion), `minecraft:recipe_unlocked` with `{"recipe": id}`, one `requirements` group naming both criteria (an OR), `rewards.recipes` = array of recipe ids.

**Parsers.**
- 1.20.1 [javap V120 `Advancement$Builder.fromJson`]: `parent`, `display`, `rewards` optional; `criteria` required, not empty; `requirements` optional — when absent each criterion becomes its own group, i.e. all are required; `sends_telemetry_event` default `false`. FG adds only loading conditions. `ItemPredicate.fromJson` [javap V120]: `items` must be a JSON array (`GsonHelper.getAsJsonArray`), `tag` is a string; also `count`, `durability`, `nbt`, `potion`, `enchantments`, `stored_enchantments`.
- 1.21.1 [javap V121, NF `Advancement` codec]: same fields; `requirements` absent → `AdvancementRequirements.allOf(criteria)`; `sends_telemetry_event` default `false`. `ItemPredicate.CODEC`: `items` = `RegistryCodecs.homogeneousList(Registries.ITEM)`, `count`, `components`, `predicates`. `HolderSetCodec` takes a `#tag` string, one id string **or an array of ids** [javap V121 `HolderSetCodec`: `Codec.either(TagKey.hashedCodec, either(listOf, element))`].
- So: `{"items":["ns:id"]}` is valid on both versions; `{"tag":…}` on 1.21.1 is an unknown key, **silently ignored**, leaving a predicate that matches any item — a tag criterion needs the per-version form; `requirements` must be written (absent = AND, and `has_the_recipe` is only met once the recipe is unlocked); `"sends_telemetry_event": false` may be written on both.
- A broken file logs `Parsing error loading custom advancement <id>: …` with `Logger.error` and is skipped [javap `ServerAdvancementManager`, both].

## 3. Block loot table of a block that drops itself

**Folder and id.** 1.20.1 `data/<ns>/loot_tables/blocks/<block>.json`, 1.21.1 `data/<ns>/loot_table/blocks/<block>.json` (inner `blocks/` on both). `BlockBehaviour.getLootTable()` builds the id with `withPrefix("blocks/")`; it returns `ResourceLocation` on 1.20.1 and `ResourceKey<LootTable>` on 1.21.1 [javap V120, V121].

**iron_block: byte-identical on the two versions** [data, `cmp`]:
`{"type":"minecraft:block","pools":[{"bonus_rolls":0.0,"conditions":[{"condition":"minecraft:survives_explosion"}],"entries":[{"type":"minecraft:item","name":"minecraft:iron_block"}],"rolls":1.0}],"random_sequence":"minecraft:blocks/iron_block"}`

**furnace** (keeps the custom name): the only difference is the function [data]: 1.20.1 `{"function":"minecraft:copy_name","source":"block_entity"}`; 1.21.1 `{"function":"minecraft:copy_components","include":["minecraft:custom_name"],"source":"block_entity"}`. `copy_name` with source `block_entity` is still registered on 1.21.1 [javap V121 `LootItemFunctions`, `CopyNameFunction$NameSource`]. `copy_nbt` / `set_nbt` exist only on 1.20.1; `copy_components`, `copy_custom_data`, `set_components`, `set_custom_data` only on 1.21.1 [javap `LootItemFunctions`, both]. A table that copies block-entity contents (shulker box) therefore differs between versions; a self-drop table does not.

**Parsers.** 1.20.1 [javap V120 `LootTable$Serializer`, `LootPool$Serializer`]: table `pools` (default empty), `type`, `random_sequence` (optional), `functions`; pool `entries` and `rolls` required, `conditions`, `functions`, `bonus_rolls` optional; FG adds a pool name (`ForgeHooks.readPoolName`). 1.21.1 [javap V121, NF `LootTable`, `LootPool` codecs]: `type` lenient-optional, `random_sequence` optional, `pools`, `functions` optional; pool `entries`, `rolls` required, `bonus_rolls` `orElse(0)`; NF adds an optional pool `name`.
**`random_sequence`** is optional on both; every vanilla block table has it equal to its own id (`minecraft:blocks/<name>`, 925 / 982 files) [data]. A table without it, without `bonus_rolls` and with `"rolls": 1` loaded on the four variants [other recon].

## 4. Tags

**Folders.** 1.20.1 [javap V120, FG `TagManager.CUSTOM_REGISTRY_DIRECTORIES`]: `tags/blocks`, `tags/items`, `tags/fluids`, `tags/entity_types`, `tags/game_events`; every other registry is `"tags/" + registry path` (`tags/damage_type` is the same on both versions). 1.21.1 [javap V121, NF `Registries.tagsDirPath`]: `"tags/" + path` → `tags/block`, `tags/item`. Vanilla files [data]: 1.20.1 `tags/blocks` 170, `tags/items` 99; 1.21.1 `tags/block` 184, `tags/item` 147.

**File form**, same on the four jars [javap `TagFile`, `TagEntry`]: `{"values":[…]}` required; `"replace"` optional, default `false` (vanilla never writes it); FG and NF add an optional `"remove"` list. An entry is `"ns:id"`, `"#ns:tag"` or `{"id":"…","required":false}`. Adding entries to an existing tag = a file at the same path in another pack with `replace` false: for `minecraft:mineable/pickaxe` the mod jar carries `data/minecraft/tags/blocks/mineable/pickaxe.json` (1.20.1) / `data/minecraft/tags/block/mineable/pickaxe.json` (1.21.1) with `{"replace":false,"values":["redstone_exoframes:charger"]}`.

**Mining tags** [data]: `mineable/{pickaxe,axe,shovel,hoe}`, `needs_stone_tool`, `needs_iron_tool`, `needs_diamond_tool` exist under both folder spellings. 1.21.1 adds `incorrect_for_{wooden,stone,iron,gold,diamond,netherite}_tool`, which only reference the `needs_*` tags (`incorrect_for_wooden_tool` = `#minecraft:needs_diamond_tool`, `#minecraft:needs_iron_tool`, `#minecraft:needs_stone_tool`): adding a block to `needs_stone_tool` gates wooden tools on both versions with the same entry. `BlockTags.MINEABLE_WITH_PICKAXE`, `NEEDS_STONE_TOOL`, `NEEDS_IRON_TOOL`, `BlockStateBase.is(TagKey<Block>)` and `BlockBehaviour.Properties.requiresCorrectToolForDrops()` have the same names on both [javap V120, V121].
**The mineable tag is mandatory for a block with `requiresCorrectToolForDrops()`** (the block dossier's example, `blocco.md`, sets it): `Player.hasCorrectToolForDrops(BlockState)` is `!state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state)` on both; on 1.20.1 `DiggerItem.isCorrectToolForDrops` tests the three `NEEDS_*` tags against the tier and then `state.is(this.blocks)` (the tool's `mineable/*` tag); on 1.21.1 `Tool.isCorrectForDrops` returns the value of the first rule whose block set holds the state, else `false` [javap V120, V121]. A block in no `mineable/*` tag drops nothing whatever the tool. Forge's and NeoForge's own tier hooks were not read [not checked].

**Failure mode to know** [javap V120, V121 `TagLoader.build`]: when a tag lists a required id that does not exist, the loader logs `Couldn't load tag <tag> as it is missing following references: …` (`Logger.error`) and **does not store the tag at all**. A misspelled block id in the mod's addition to `minecraft:mineable/pickaxe` empties that tag for every block.

**Item tags for ingredients.** Vanilla has no tag for iron or copper ingots. Loader conventions differ on 1.20.1: Forge `forge:ingots/copper`, `forge:ingots/iron`, `forge:dusts/redstone`, `forge:gems/quartz`, `forge:storage_blocks/redstone` [data FG]; Fabric `c:copper_ingots`, `c:iron_ingots`, `c:redstone_dusts`, `c:quartz` [data fabric-convention-tags-v1 1.5.6]. On 1.21.1 both loaders have `c:ingots/copper`, `c:ingots/iron`, `c:dusts/redstone`, `c:gems/quartz`, `c:storage_blocks/redstone` [data NF; fabric-convention-tags-v2 2.12.0]. A tree per Minecraft version cannot name one tag valid on both 1.20.1 loaders.

## 5. Assets

- **Same folder names on both versions** [data: listings]: `assets/<ns>/blockstates/`, `models/block/`, `models/item/`, `textures/block/`, `textures/item/`, `lang/`. Constants [javap]: `"blockstates"`, `"models"`, `"item/"` in `ModelBakery` (V120, FG); `"blockstates"` in `BlockStateModelLoader`, `"models"`, `"item/"` in `ModelBakery` (V121, NF). `atlases/blocks.json` is byte-identical: `textures/block/` and `textures/item/` of every namespace are stitched [data].
- **Vanilla files byte-identical between the two jars** [data, md5]: `blockstates/furnace.json`, `models/block/{furnace,furnace_on,orientable,orientable_with_bottom,cube,cube_all,cube_column,cube_bottom_top,block}.json`, `models/item/{furnace,iron_block,iron_ingot,generated}.json`, `blockstates/{iron_block,redstone_lamp,observer,dropper}.json`. Parser keys are equal on both [javap `BlockModelDefinition$Deserializer`, `Variant$Deserializer`, `BlockModel$Deserializer`]: `variants` | `multipart`; variant `model`, `x`, `y`, `uvlock`, `weight`; model `parent`, `textures`, `elements`, `display`, `overrides`, `gui_light`, `ambientocclusion`.
- **Blockstate with horizontal facing + boolean** (furnace) [data]: `{"variants":{"facing=north,lit=false":{"model":"minecraft:block/furnace"},"facing=east,lit=false":{"model":"minecraft:block/furnace","y":90},"facing=south,lit=false":{"model":"minecraft:block/furnace","y":180},"facing=west,lit=false":{"model":"minecraft:block/furnace","y":270},` then the same four with `lit=true` → `minecraft:block/furnace_on` `}}`. A variant key is split on `,` then `=` and matched property by property: the order of properties is free and a key may name only some of them [javap V120 `ModelBakery.predicate`, V121 `BlockStateModelLoader.predicate`].
- **Block models** [data]: `{"parent":"minecraft:block/orientable","textures":{"front":"<ns>:block/x_front","side":"<ns>:block/x_side","top":"<ns>:block/x_top"}}` (`orientable`: bottom = top, front on the north face; `orientable_with_bottom` adds `bottom`). A lit variant is a second model with another `front`.
- **Item model of a block item**: `{"parent":"minecraft:block/furnace"}`. **Generated item**: `{"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/iron_ingot"}}`, the form of the mod's 11 item models [code `src/main/resources/assets/redstone_exoframes/models/item/*.json`].
- **Format 15 vs 34**: none of the files above changes; what changed in the listings is elsewhere (`textures/gui` 89 → 502 files, `textures/map` 3 → 37) [data]. Mojang's list of pack-format changes was not consulted [not checked].
- A dedicated server never loads blockstates or models: an asset mistake shows only in a client log.

## 6. pack.mcmeta and the folders each version reads

- **Declared format** [code]: `src/main/resources/pack.mcmeta:3` `"pack_format": ${pack_format}`, expanded (`build.gradle.kts:200,207-209`) from `mod.pack_format` (`versions/<variant>/gradle.properties:3`): 1.20.1-fabric 15, 1.20.1-forge 15, 1.21.1-fabric 34, 1.21.1-neoforge 34. The four built jars carry 15 / 15 / 34 / 34 [data `build/libs/all/*.jar`]; `tools/check_jars.sh:71-74` requires exactly these.
- **What the game expects** [data `version.json`; javap `SharedConstants`]: 1.20.1 `RESOURCE_PACK_FORMAT = 15`, `DATA_PACK_FORMAT = 15`; 1.21.1 `RESOURCE_PACK_FORMAT = 34`, **`DATA_PACK_FORMAT = 48`**. On 1.21.1 the one number 34 is the resource format; as a data pack the mod declares an older one. It does not stop loading: on the server side `Pack.getCompatibility().isCompatible()` is read only to build a display string (V120 `MinecraftServer.method_46220`, V121 `PackRepository.method_59808`) [javap], and the 1.21.1 jars' recipe, loot table and tag were used by the dedicated servers of both 1.21.1 variants [other recon]. 1.21.1 also accepts `supported_formats` [javap V121 `PackMetadataSection`]; 1.20.1 reads only `pack_format` and `description` [javap V120, FG].
- **Folder constants, read on each jar** [javap]:

| What | V120 | FG | V121 | NF |
|---|---|---|---|---|
| recipes: `RecipeManager.<init>` | `"recipes"` | `"recipes"` (in `<init>(ICondition$IContext)`) | `Registries.elementsDirPath(Registries.RECIPE)` | same call |
| loot: `LootDataType.TABLE` | `"loot_tables"` | `"loot_tables"` | `Registries.LOOT_TABLE`; directory from `elementsDirPath` in `ReloadableServerRegistries` | same |
| advancements: `ServerAdvancementManager.<init>` | `"advancements"` | `"advancements"` | `elementsDirPath(Registries.ADVANCEMENT)` | same |
| tags | `TagManager.getTagDir`: `tags/blocks`, `tags/items`, … | same map; fallback via `ForgeHooks.prefixNamespace` | `Registries.tagsDirPath` = `"tags/" + path` | same, via `CommonHooks.prefixNamespace` |

  `Registries.elementsDirPath(key)` = `key.location().getPath()` (V121); on NF `CommonHooks.prefixNamespace(location)`, which returns the bare path for the `minecraft` namespace. Registry key paths [javap `Registries.<clinit>`, V121, NF]: `recipe`, `loot_table`, `advancement`, `block`, `item`.
- **One spelling per version**: scanning every class of the four jars for the exact constants `recipes`, `loot_tables`, `advancements`, `tags/blocks`, `tags/items`, `tags/block`, `tags/item`, `loot_table` finds the plural folder names only in the 1.20.1 classes above and no folder constant of the other spelling in any jar (the other hits are JSON field names and Java member names, e.g. `AdvancementRewards` "recipes", `LootPoolEntries` "loot_table"; datagen classes under `net/minecraft/data/` excluded). No Fabric API module of either version reads them as folders (hits: reload-listener keys, datagen, mixin member names). NeoForge's `TagConventionLogWarning` concerns `forge:`-namespace tags, not folders. A folder with the other version's spelling is ignored without a message on every variant [constant-pool scan].
- Already in the repo: the GameTest structure is committed twice in the common tree, `data/redstone_exoframes/structure/empty.nbt` and `structures/empty.nbt`, and both copies are in all four jars [code; data].

## 7. Lang

- **Block**: `Block.getDescriptionId()` = `Util.makeDescriptionId("block", key)` = `block.<ns>.<path>`. **Its item**: `BlockItem.getDescriptionId()` returns the block's id, so a plain `BlockItem` needs no `item.` key (vanilla has `block.minecraft.furnace` and no `item.minecraft.furnace`). Other items: `item.<ns>.<path>`. Same on both versions [javap V120, V121 `Block`, `BlockItem`, `Item`; data `lang/en_us.json`].
- **Key mappings**: `KeyMapping(String name, int keyCode, String category)` on both [javap]; vanilla names `key.jump`, categories `key.categories.movement` [data]. The mod: `"key." + MOD_ID + ".exit"`, `"key.categories." + MOD_ID` [code `client/ExitKey.java:26,28`].
- **The mod's two files** [code `src/main/resources/assets/redstone_exoframes/lang/en_us.json`, `it_it.json`]: 38 keys each, same keys in the same order. Families: `item.redstone_exoframes.*` 11 (exoframe, redstone_cell, scrap_helmet, scrap_torso, scrap_arm, scrap_leg, exoframe_helmet, exoframe_torso, exoframe_legs, exoframe_boots, suit_model); `entity.redstone_exoframes.exoframe`; `itemGroup.redstone_exoframes.main`; `commands.redstone_exoframes.*` 14; `message.redstone_exoframes.*` 9 (no_room, locked, slot_taken, no_cell, already_inside, frame_elsewhere, frame_elsewhere_border, holds_armor, exit_hold); `key.redstone_exoframes.exit`; `key.categories.redstone_exoframes`. No `block.` key. Flat JSON, 2-space indent, raw UTF-8 (`è`, `…`), LF.
- Nothing checks the two files against each other or against the code: no test and no tool mentions `lang` [code: grep of `src/test`, `tools`, `.github`].

## 8. Build, checks, and a shape for `tools/gen_data.py`

**Build** [code `build.gradle.kts`]: `:180-181` `sourceSets.main { resources.srcDir(rootProject.file("src/main/resources-mc$mc")) }`, `mc = stonecutter.current.version` (`:5`) — added by absolute path, outside Stonecutter. `:189-210` `processResources` drops the other loaders' metadata and runs `expand(props)` only on `fabric.mod.json`, `META-INF/mods.toml`, `META-INF/neoforge.mods.toml`, `pack.mcmeta`; every other file is copied verbatim. Today the per-version trees hold `redstone_exoframes.mixins.json` (both) and two PNGs under `assets/minecraft/textures/models/armor/` (1.20.1); the built jars show each tree only in its version's jars [data]. No build change is needed for `data/` [other recon]. What Gradle does with the same relative path in the common tree and in a per-version tree was not checked [not checked].

**The existing generator and its check** [code]: `tools/gen_placeholders.py` — stdlib only; `MAIN = …/src/main` (`:16`); `outputs()` (`:342-351`) returns `(path, width, height, draw)`; `main()` (`:354-364`) takes `--out <dir>` and writes to `os.path.join(args.out, os.path.relpath(path, MAIN))`; run as `python3 -I`. `tools/check_art.sh:53-73` runs it with `--out`; for every PNG **the generator produced** it requires `src/main/<rel>` to exist and be pixel-equal (`tools/art/compare_png.py`), and fails if nothing was produced. A committed file the generator does not produce is never looked at. `check_art.sh` also needs numpy (`tools/art/build.py`).

**What would reject new data files, or must know about them** [code]:
- `tools/check_jars.sh`: nothing rejects them — (a) metadata, (b) pack_format, (c) range, (d) classes, (e)/(e2) mixins, (f) icon, (f2) two fixed texture paths, (g) privacy; no rule lists the contents of `data/`.
- `tools/check_jar_privacy.py:38-45` scans every jar entry for `/home/<x>`, `/Users/<x>`, `/root/<x>` (only when not preceded by a letter, digit, `.`, `-`, `_`), e-mail addresses, PNG text chunks. `"minecraft:recipes/root"` does not match (`/root` follows `s` and is not followed by `/`).
- `tools/check_sources.sh`: Java rules only. `tools/run_gametests.sh:43` → `tools/check_log.py`: every WARN/ERROR/FATAL line containing `exoframe` must be in `tools/log_allowlist.txt` (`check_log.py:40,119`). The game's parse errors carry the resource id (`Parsing error loading recipe redstone_exoframes:…`, `Parsing error loading custom advancement …`, `Couldn't parse element loot_tables:redstone_exoframes:blocks/…` — on 1.21.1 `loot_table/…` —, `Couldn't load tag … missing following references: redstone_exoframes:…`) and are `Logger.error` [javap V120, V121; the same strings are in FG and NF]: **a malformed data file of the mod already fails the GameTest step**, on the variants whose tree is wrong.
- `.github/workflows/build.yml`: test (`:18-19`), build + collectJar (`:20-21`), GameTest (`:22-25`), `check_sources.sh` (`:26-27`), setup-python 3.12 + pip (`:30-34`), `check_art.sh` (`:35-36`), `check_jars.sh` (`:37-38`), upload. The runner's own `python3` is already used before setup-python (`run_gametests.sh:43`).
- Documents naming the tools: `tools/README.md` (table `:6-13`; "Scrive tredici file" `:30`, `:50`), `README.md:27`.

**Proposed `tools/gen_data.py`** — stdlib only (`argparse`, `json`, `os`, `re`), run as `python3 -I`, same `MAIN` and `--out` convention as `gen_placeholders.py`. The two code blocks below were extracted from this file and run as printed [proto]: the Python one, completed with the imports, `MAIN`, `MOD`, the content tables and `validate()` of `proto/gen_data.py`, writes the same 14 files as the prototype, byte for byte; the Bash one behaves as stated in five cases (nothing committed, fresh, edited file, orphan, generator failing).
```python
DIRS = {"1.20.1": {"recipe": "recipes", "advancement": "advancements", "loot_table": "loot_tables",
                   "block_tag": "tags/blocks", "item_tag": "tags/items"},
        "1.21.1": {"recipe": "recipe", "advancement": "advancement", "loot_table": "loot_table",
                   "block_tag": "tags/block", "item_tag": "tags/item"}}
# content written once: RECIPES = ((name, book_category, advancement_folder, pattern, key, count, unlock_item), …),
# SELF_DROP_BLOCKS = ("charger",), BLOCK_TAGS = {"minecraft:mineable/pickaxe": ("charger",), …}; key values "ns:item" or "#ns:tag"
def ingredient(spec):                      # same on both versions
    return {"tag": spec[1:]} if spec.startswith("#") else {"item": spec}
def item_predicate(version, spec):         # version difference 2: a tag criterion
    if not spec.startswith("#"): return {"items": [spec]}
    return {"tag": spec[1:]} if version == "1.20.1" else {"items": spec}
def shaped(version, name, category, pattern, key, count):   # version difference 1: the result
    result = {"item": MOD + ":" + name, "count": count} if version == "1.20.1" else {"id": MOD + ":" + name, "count": count}
    return {"type": "minecraft:crafting_shaped", "category": category,
            "key": {s: ingredient(v) for s, v in sorted(key.items())}, "pattern": list(pattern), "result": result}
def unlock(version, name, unlock_item):
    recipe, has = MOD + ":" + name, "has_" + unlock_item.lstrip("#").split(":", 1)[1].replace("/", "_")
    return {"parent": "minecraft:recipes/root",
            "criteria": {has: {"conditions": {"items": [item_predicate(version, unlock_item)]}, "trigger": "minecraft:inventory_changed"},
                         "has_the_recipe": {"conditions": {"recipe": recipe}, "trigger": "minecraft:recipe_unlocked"}},
            "requirements": [[has, "has_the_recipe"]], "rewards": {"recipes": [recipe]}, "sends_telemetry_event": False}
def self_drop(block):                      # same on both versions
    return {"type": "minecraft:block", "pools": [{"bonus_rolls": 0.0, "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item", "name": MOD + ":" + block}], "rolls": 1.0}], "random_sequence": MOD + ":blocks/" + block}
def tag(values): return {"replace": False, "values": [MOD + ":" + v for v in values]}
def outputs():                             # [(path relative to src/main, JSON object)], sorted by path
    validate()
    files = []
    for version, dirs in sorted(DIRS.items()):
        root = "resources-mc%s/data" % version
        for name, category, folder, pattern, key, count, unlock_item in RECIPES:
            files.append(("%s/%s/%s/%s.json" % (root, MOD, dirs["recipe"], name), shaped(version, name, category, pattern, key, count)))
            files.append(("%s/%s/%s/recipes/%s/%s.json" % (root, MOD, dirs["advancement"], folder, name), unlock(version, name, unlock_item)))
        for block in SELF_DROP_BLOCKS:
            files.append(("%s/%s/%s/blocks/%s.json" % (root, MOD, dirs["loot_table"], block), self_drop(block)))
        for tag_id, values in sorted(BLOCK_TAGS.items()):
            namespace, path = tag_id.split(":", 1)
            files.append(("%s/%s/%s/%s.json" % (root, namespace, dirs["block_tag"], path), tag(values)))
    files.sort(key=lambda entry: entry[0])
    if len(set(path for path, _ in files)) != len(files): raise ValueError("two outputs share a path")
    return files
def render(obj): return json.dumps(obj, indent=2, ensure_ascii=False) + "\n"
def main():                                # --out <dir>: the same relative paths under <dir> instead of src/main
    ap = argparse.ArgumentParser(); ap.add_argument("--out", default=None); args = ap.parse_args()
    for rel, obj in outputs():
        path = os.path.join(MAIN if args.out is None else args.out, *rel.split("/"))
        os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f: f.write(render(obj))
```
- `validate()` runs first and mirrors the rules only the game would otherwise report: pattern 1–3 rows of equal width 1–3; key symbols = pattern symbols, one character each; known recipe-book category; id syntax; count (1 for the mod's unstackable items).
- [proto] two runs give byte-identical trees (14 files for two example recipes, one block, two tags). `proto/selfcheck.py` builds with these functions the vanilla piston recipe, the piston and stick unlock advancements and the iron_block loot table for both versions: each equals the vanilla JSON of its jar once parser-default fields are dropped (`show_notification: true`, `count: 1`, `sends_telemetry_event: false`) and vanilla's 1.21.1 string `items` is read as a one-element array.
- What it writes for one recipe: 1.20.1 `recipes/redstone_cell.json` `…"result":{"item":"redstone_exoframes:redstone_cell","count":1}}`; 1.21.1 `recipe/redstone_cell.json` `…"result":{"id":"redstone_exoframes:redstone_cell","count":1}}`; the two advancements are identical apart from the folder.

**Proposed freshness check `tools/check_data.sh`** (it ignores the non-data files of the per-version trees, such as `redstone_exoframes.mixins.json`):
```bash
set -eu; cd "$(dirname "$0")/.."; tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
python3 -I tools/gen_data.py --out "$tmp/data" >"$tmp/gen.log" 2>&1 || { cat "$tmp/gen.log" >&2; echo "FAIL: tools/gen_data.py non e' riuscito" >&2; exit 1; }
fail=0; count=0
while IFS= read -r -d '' g; do rel=${g#"$tmp/data/"}; count=$((count + 1))   # every generated file is committed and equal
    if [ ! -f "src/main/$rel" ]; then echo "FAIL: manca src/main/$rel" >&2; fail=1
    elif ! cmp -s "$g" "src/main/$rel"; then echo "FAIL: src/main/$rel diverso da quello generato" >&2; fail=1; fi
done < <(find "$tmp/data" -type f -print0)
[ "$count" -gt 0 ] || { echo "FAIL: nessun file generato" >&2; fail=1; }
for dir in src/main/resources-mc*/data; do [ -d "$dir" ] || continue                # no committed data file the generator does not write
    while IFS= read -r -d '' f; do [ -f "$tmp/data/${f#src/main/}" ] || { echo "FAIL: $f non e' scritto da tools/gen_data.py" >&2; fail=1; }
    done < <(find "$dir" -type f -print0); done
[ "$fail" -eq 0 ] || exit 1; echo "OK data"
```
The second loop is what `check_art.sh` lacks: the game would still load a stale recipe left behind by a rename. The script needs no numpy, so it can be its own CI step right after "Controlli sui sorgenti" (`build.yml:26-27`).

## 9. Ingredient ids

All exist with the same id on both versions. For each id, on V120 and on V121: the registration string is in `net/minecraft/world/item/Items.class` or `net/minecraft/world/level/block/Blocks.class`, `assets/minecraft/models/item/<id>.json` exists, a lang key exists, and a vanilla recipe of that version uses `"minecraft:<id>"` [javap constant pool; data]. Checked, none missing (67): `iron_ingot`, `copper_ingot`, `redstone`, `quartz`, `redstone_block`, `iron_block`, `copper_block`, `iron_nugget`, `gold_ingot`, `gold_nugget`, `iron_bars`, `redstone_torch`, `repeater`, `comparator`, `glass`, `glass_pane`, `leather`, `stick`, `string`, `chain`, `lightning_rod`, `piston`, `hopper`, `furnace`, `blast_furnace`, `smooth_stone`, `iron_trapdoor`, `heavy_weighted_pressure_plate`, `quartz_block`, `observer`, `daylight_detector`, `glowstone_dust`, `amethyst_shard`, `coal`, `charcoal`, `flint`, `iron_door`, `bucket`, `anvil`, `cauldron`, `crafting_table`, `target`, `lever`, `redstone_lamp`, `raw_iron`, `raw_copper`, `cut_copper`, `exposed_copper`, `waxed_copper_block`, `honeycomb`, `slime_ball`, `ender_pearl`, `diamond`, `netherite_ingot`, `netherite_scrap`, `minecart`, `rail`, `powered_rail`, `tripwire_hook`, `dispenser`, `dropper`, `note_block`, `iron_helmet`, `iron_chestplate`, `iron_leggings`, `iron_boots`, `shield`. Redstone dust is `minecraft:redstone` (`item.minecraft.redstone` = "Redstone Dust").

## 10. Earlier reconnaissance: confirmed, corrected, not checked

`stato-del-codice.md`:
- **Confirmed**: §7.1 (contents of the common and per-version trees; `build.gradle.kts:180-181`; `processResources :189-210` with `expand` on four files; the vanilla folder names; result `{"item":…}` vs `{"count":1,"id":…}`; `tools/gen_data.py` absent; the variant properties); §7.3 (nothing in `check_jars.sh` concerns data files beyond privacy); §7.4 (`check_art.sh` compares what `gen_placeholders.py --out` produces; a committed PNG it does not produce is not compared); the CI step list; line 221 (no block, recipe, loot table or tag; `data/` holds only the two `empty.nbt`); line 222 (38 lang keys, the families, no `block.` key, no test on the lang files); line 223 (11 item models of the `item/generated` form, `suit_model` reuses `item/exoframe_torso`); line 224 (`outputs()` at `:342`, `PIECE_ICONS :213`, `SCRAP_ICONS :338-339`).
- **Added, not in it**: on 1.21.1 the data pack format is 48 while the mod declares 34 (§6).

`ricognizione-movimento-e-dati.md` §2:
- **Confirmed by javap**: its two deductions about a result with data (`components` on 1.21.1; `nbt` only on Forge 1.20.1); ingredients stay objects on 1.21.1; and the lookups it left unchecked — `RecipeManager.byKey(ResourceLocation)` returns `Optional<? extends Recipe<?>>` on 1.20.1 (V120, FG) and `Optional<RecipeHolder<?>>` on 1.21.1 (V121, NF); 1.20.1 `MinecraftServer.getLootData()` → `LootDataResolver.getLootTable(ResourceLocation)` (a default method; `LootDataManager` implements it); 1.21.1 `MinecraftServer.reloadableRegistries()` → `ReloadableServerRegistries$Holder.getLootTable(ResourceKey<LootTable>)`.
- **Completed**: it lists four folder kinds for the generator; recipe-unlock advancements add a fifth (`advancements` / `advancement`) with its own per-version difference (§2).
- **Not checked here**: its run results, taken as given.

## 11. What this changes for the plan of part 3c

**Recommendations**
1. **Two format differences, plus folder names.** Recipes: result `item` (1.20.1) vs `id` (1.21.1). Unlock advancements: a tag criterion is `{"tag":…}` vs `{"items":"#…"}`. Self-drop loot tables and tag files are the same JSON under different folders. On both versions the generator can write `count`, omit `show_notification`, write `"sends_telemetry_event": false` and use `{"items":["ns:id"]}`.
2. **Plain item ids as ingredients**, no ingredient tags: the conventional tags differ between the two 1.20.1 loaders (§4) and the generator writes per version, not per loader.
3. **No result data in any recipe.** Crafted cell = empty cell, crafted piece = fresh piece, crafted frame = new frame, all with plain results (§1). A cell crafted already charged cannot be written as data for Fabric 1.20.1: if it is wanted, it is a code task.
4. **Result count 1** for `exoframe`, `redstone_cell` and the four pieces: a larger count fails to load on 1.21.1 only. Put the rule in `validate()`.
5. **Five folders per version** under `src/main/resources-mc<ver>/data/`: `redstone_exoframes/{recipes|recipe}/`; `redstone_exoframes/{advancements|advancement}/recipes/<category>/`; `redstone_exoframes/{loot_tables|loot_table}/blocks/charger.json`; `minecraft/{tags/blocks|tags/block}/mineable/pickaxe.json` — required, not optional, if the block has `requiresCorrectToolForDrops()` (§4); and, only if a wooden pickaxe must not be enough, `minecraft/…/needs_stone_tool.json`. Recipes named by design §5.8: `exoframe`, `scrap_helmet`, `scrap_torso`, `scrap_arm`, `scrap_leg`, `redstone_cell`, `charger`. The design names recipes only: shipping unlock advancements is a choice the plan should state (without them the recipes still craft at the table; that the recipe book then does not offer them beforehand is vanilla behaviour recalled, [not checked]).
6. **Assets go in the common tree**: blockstate, block models, the block item's model and the textures are the same files on both versions (§5): `assets/redstone_exoframes/blockstates/charger.json`, `models/block/charger*.json`, `models/item/charger.json`, `textures/block/charger_*.png`. JSON by hand like the 11 item models, or written into `src/main/resources/` by the same script; the PNGs belong in `gen_placeholders.py` `outputs()` so `check_art.sh` covers them (then "tredici" in `tools/README.md:30,50` changes). Block textures must be fully opaque; `draw_sprite` treats `.` as transparent.
7. **Lang**: add `block.redstone_exoframes.charger` (no `item.` key for a plain `BlockItem`), `key.redstone_exoframes.<name>` for the cell-swap key (the category key exists) and the new `message.` keys, to both files in the same order.
8. **`tools/gen_data.py` + `tools/check_data.sh` + one CI step** as in §8, orphan rule included; add both to `tools/README.md` and `README.md:27`. Optional: `gen_data.py --list <version>` printing the jar paths of its outputs, used by a new rule of `check_jars.sh` (each jar holds its version's files and none of the other's). A blanket rule "no singular folder in a 1.20.1 jar" would trip on the existing `structure/` + `structures/` pair: name the five folder kinds.
9. **Catching what a freshness check cannot** (a file that is fresh but wrong for the game): the GameTest log check already fails on a parse error naming the mod (§8). Add positive assertions that need no version directive: `server.getRecipeManager().byKey(id).isPresent()` for each recipe id (compiles on both versions as long as the code does not name the return type); break the charger and find its item; `state.is(BlockTags.MINEABLE_WITH_PICKAXE)` for the charger **and for `Blocks.STONE`** (guards the dropped-tag failure of §4). Reading a loot table by id or a recipe's result needs `compat/` (signatures in §10).
10. **Assets and lang are outside the CI servers.** Plan a cloud client run for the charger, or a JUnit test over the classpath resources in the style of `ExoframeGeometryTest` (blockstate → model files → texture files exist); the same class can assert that the two lang files have the same keys.
11. **`pack.mcmeta` stays as it is** (15 / 34): the 1.21.1 data format is 48 but nothing gates loading on it, and another number would fail `check_jars.sh` (b).

**Open points**
- Recipe shapes, recipe-book categories and the unlocking item of each advancement are the builder's choice (design §6); the prototype's two recipes are placeholders.
- Not proven by a run: unlock advancements on any variant; a tag file in the `minecraft` namespace shipped by the mod (the run proved a tag in the mod's namespace); Forge's `nbt` result form. All three rest on the parsers and the vanilla data read here.
- Duplicate relative paths between the common and a per-version tree: Gradle's behaviour not checked; the generator should never write a path that exists under `src/main/resources/`.
- If the two `empty.nbt` structures move into the per-version trees (design §10, "prima dell'alpha"), the orphan rule of `check_data.sh` must exempt them or the generator must own them.
- Whether the charger's loot table copies anything from the block entity (name, stored cell, fuel) depends on the block design. If the block spills its contents in code, the table is the version-free one of §3; a copying table differs per version (`copy_nbt` vs `copy_components`).
- Committed JSON is compared byte for byte and `.gitattributes` forces LF only for `*.sh`: a Windows checkout with automatic CRLF conversion would fail `check_data.sh` locally (already true for the geometry JSON in `check_art.sh`). The CI is unaffected.
- That the mod's data pack is labelled incompatible (34 against 48) in pack listings on 1.21.1 is inferred from the code that builds that string, not observed.
