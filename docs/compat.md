# Interfacce `compat/`: cosa chiudono e dove

Il package [`com.toxineren.exoframes.compat`](../src/main/java/com/toxineren/exoframes/compat/) è l'unico punto in cui il codice della mod vede le differenze tra Minecraft 1.20.1 e 1.21.1 e tra i loader (Fabric, Forge, NeoForge). Il resto del codice (`content/`, e in futuro `entity/`, `block/`, `item/`, `client/`) chiama solo le interfacce di `compat/` tramite il punto di accesso [`Compat`](../src/main/java/com/toxineren/exoframes/compat/Compat.java); `core/` non dipende da Minecraft.

Regole di struttura (Milestone 2b):

- ogni interfaccia ha una sola implementazione `...Impl` in [`compat/impl/`](../src/main/java/com/toxineren/exoframes/compat/impl/), creata pigramente da `Compat` (idioma del holder);
- le condizioni Stonecutter (`//? if >=1.21`, `//? if fabric`, `//? if forge`) stanno solo nelle classi di `compat/impl/`, negli entrypoint di [`platform/`](../src/main/java/com/toxineren/exoframes/platform/) e nell'unica eccezione [`compat/Ids.java`](../src/main/java/com/toxineren/exoframes/compat/Ids.java); le interfacce non hanno condizioni;
- `compat.impl` si importa solo da dentro `compat/`.

Tutto è verificato da [`tools/check_sources.sh`](../tools/check_sources.sh) (vedi in fondo).

## Le sette interfacce

### `Platform` — ambiente di esecuzione

- **File:** [`Platform.java`](../src/main/java/com/toxineren/exoframes/compat/Platform.java), implementazione [`PlatformImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/PlatformImpl.java).
- **A cosa serve:** lato fisico (`isClient()`, `isDedicatedServer()`), ambiente di sviluppo (`isDevelopment()`), cartella di configurazione (`configDir()`), mod caricate (`isModLoaded(id)`), nome del loader (`loaderName()`: `"fabric"`, `"forge"` o `"neoforge"`).
- **Differenze chiuse:** le API di ambiente dei tre loader, tramite `dev.architectury.platform.Platform` (uguale sulle due versioni di Architectury). Il nome del loader è una costante scelta per variante.
- **Condizione:** `PlatformImpl` (`//? if fabric` / `forge` / altrimenti `neoforge`).
- **Uso:** `Compat.platform().configDir()`.

### `ItemDataAccess` — dati tipizzati sugli item

- **File:** [`ItemDataAccess.java`](../src/main/java/com/toxineren/exoframes/compat/ItemDataAccess.java), implementazione [`ItemDataAccessImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/ItemDataAccessImpl.java) con l'accesso grezzo in [`ItemPayload.java`](../src/main/java/com/toxineren/exoframes/compat/impl/ItemPayload.java).
- **A cosa serve:** energia delle celle (`cellEnergy`/`setCellEnergy`), dati dei pezzi (`piece`/`setPiece` con il record `PieceData`: tier, durabilità, durabilità massima, moduli, due colori RGB), dati del telaio come item (`frame`/`setFrame` con il record `FrameItemData`: proprietario `Optional<UUID>`, sbloccato, rinforzato, energia della cella `Optional<Integer>`, pezzi per slot), presenza dei dati (`hasData`). I record rifiutano componenti null. In lettura (dati non fidati: `/give`, inventario creativo, altre mod) gli intervalli sono garantiti: un pezzo ha `maxDurability > 0` (altrimenti nessun pezzo) e `durability` in [0, `maxDurability`]; i moduli sono noti, senza duplicati, nell'ordine salvato e al massimo uno per tipo; i colori stanno in [0, `0xFFFFFF`]; l'energia della cella è sempre `>= 0` (il limite superiore, la capacità, resta al chiamante).
- **Differenze chiuse:** su 1.20.1 i dati stanno nel tag NBT dello stack (`getTagElement`/`getOrCreateTag`); su 1.21.1 nel componente vanilla `minecraft:custom_data` (nessun componente registrato dalla mod). Il contenuto del tag radice `exoframes` è identico, così il DFU di vanilla, che lo copia così com'è in `custom_data`, conserva i dati primitivi di un mondo 1.20.1 aggiornato a 1.21.1; gli stack annidati (gli item riposti di `stowed`) non vengono convertiti e avranno bisogno di una migrazione esplicita. Le scritture conservano le chiavi non toccate e non fanno nulla su uno stack vuoto.
- **Condizione:** solo `ItemPayload` (lettura e scrittura del tag radice); `ItemDataAccessImpl` non ha condizioni.
- **Uso:** `Compat.itemData().cellEnergy(stack)`.

### `ArmorMaterials` — materiale d'armatura segnaposto

- **File:** [`ArmorMaterials.java`](../src/main/java/com/toxineren/exoframes/compat/ArmorMaterials.java), implementazione [`ArmorMaterialsImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/ArmorMaterialsImpl.java).
- **A cosa serve:** il materiale "frame" a difesa, robustezza, contraccolpo e incantabilità zero, condiviso dai quattro slot dei pezzi (la protezione reale è un attributo, vedi `FrameAttributes`). `register()` va chiamato una volta sola nell'init comune prima degli item; `newPlaceholder(type, props)` crea l'`ArmorItem` con la pila sempre a 1 su entrambe le versioni, e le proprietà possono avere o no una durabilità vanilla (su 1.21.1 si imposta `stacksTo(1)`; su 1.20.1 le proprietà passano invariate, perché lì `stacksTo` lancia se c'è già una durabilità e il costruttore di `ArmorItem` porta comunque la pila a 1); `frameMaterialHandle()` restituisce il materiale nella forma della versione (tipo `Object`).
- **Differenze chiuse:** su 1.20.1 `ArmorMaterial` è un'interfaccia implementata da un enum privato, con il nome `frame` senza namespace perché con un namespace il percorso della texture si romperebbe su Fabric 1.20.1; su 1.21.1 è un record registrato nel registro vanilla `armor_material` (id `redstone_exoframes:frame`, con il `DeferredRegister` di Architectury) e `ArmorItem` prende un `Holder<ArmorMaterial>`.
- **Condizione:** `ArmorMaterialsImpl`.
- **Uso:** `Compat.armorMaterials().newPlaceholder(ArmorItem.Type.HELMET, new Item.Properties())`.

### `Net` — pacchetti di rete tipizzati

- **File:** [`Net.java`](../src/main/java/com/toxineren/exoframes/compat/Net.java) con [`PacketCodec.java`](../src/main/java/com/toxineren/exoframes/compat/PacketCodec.java), implementazione [`NetImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/NetImpl.java).
- **A cosa serve:** `registerS2C`/`registerC2S` (id, classe del messaggio, `PacketCodec`, gestore) da chiamare nell'init comune su entrambi i lati; `sendToPlayer`/`sendToServer`. I gestori girano sempre sul thread di gioco di chi riceve; le loro eccezioni restano nel log della piattaforma. Inviare su un id non registrato o registrato per la direzione opposta lancia `IllegalStateException`, un messaggio del tipo sbagliato `IllegalArgumentException`. `sendToPlayer` chiede prima ad Architectury (`NetworkManager.canPlayerReceive`, con l'id su 1.20.1 e con il `CustomPacketPayload.Type` su 1.21.1) se il client del giocatore ha registrato il canale, e se no salta l'invio in silenzio (una riga DEBUG per id): su NeoForge 1.21.1 i payload di Architectury sono opzionali, un client senza la mod supera la negoziazione e un invio senza controllo lancerebbe un'eccezione dentro l'evento di ingresso, con il giocatore disconnesso da un messaggio fuorviante.
- **Differenze chiuse:** su 1.20.1 (Architectury 9.2) canali grezzi con `FriendlyByteBuf` (decodificato prima di accodare il gestore); su 1.21.1 (Architectury 13.0) `CustomPacketPayload.Type` + `StreamCodec` su `RegistryFriendlyByteBuf`, con un record `Payload` interno. Lato server dedicato 1.21.1 si registra solo il tipo del payload S2C (`registerS2CPayloadType`), mai insieme al ricevitore.
- **Condizione:** `NetImpl`.
- **Uso:** `Compat.net().sendToPlayer(player, id, msg)`. Unico canale registrato oggi: `redstone_exoframes:frame_state_sync` (S2C, di `FrameStateAccess`).

### `FrameAttributes` — effetti del telaio sugli attributi vanilla

- **File:** [`FrameAttributes.java`](../src/main/java/com/toxineren/exoframes/compat/FrameAttributes.java), implementazione [`FrameAttributesImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/FrameAttributesImpl.java).
- **A cosa serve:** `apply(entity, FrameEffects)` imposta quattro modificatori transitori (velocità come "moltiplica totale" di `speedMultiplier - 1`; contraccolpo, armatura da `armorPoints()` e robustezza come addendi), rimuovendo prima quelli con lo stesso id; `clear(entity)` li toglie. Si chiama ai cambi di stato, non a ogni tick: al login, al respawn (compresa l'uscita dall'End, che crea un nuovo `ServerPlayer` con i soli valori base degli attributi, quindi senza i modificatori transitori) e a ogni cambio del telaio; un semplice cambio di dimensione conserva l'entità e i modificatori e non richiede nulla. Il listener di respawn già registrato in `FrameStateAccessImpl.registerCommon` è il punto naturale in cui la Milestone 3 chiamerà `apply`.
- **Differenze chiuse:** su 1.20.1 `AttributeModifier(UUID, String, double, Operation)` con `ADDITION`/`MULTIPLY_TOTAL` e `removeModifier(UUID)`; su 1.21.1 `AttributeModifier(ResourceLocation, double, Operation)` con `ADD_VALUE`/`ADD_MULTIPLIED_TOTAL` e `removeModifier(ResourceLocation)`; le costanti `Attributes.*` sono `Holder` su 1.21.1 (stesso codice sorgente). Id: `redstone_exoframes:frame_speed`, `frame_knockback`, `frame_armor`, `frame_toughness`; su 1.20.1 gli UUID sono derivati una volta con `UUID.nameUUIDFromBytes(id.toString().getBytes(UTF_8))`, quindi stabili tra avvii.
- **Condizione:** `FrameAttributesImpl` (metodi `key` e `modifier`).
- **Uso:** `Compat.attributes().apply(player, effects)`.

### `DamageClassifier` — tipo di danno per la ripartizione sui pezzi

- **File:** [`DamageClassifier.java`](../src/main/java/com/toxineren/exoframes/compat/DamageClassifier.java), implementazione [`DamageClassifierImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/DamageClassifierImpl.java).
- **A cosa serve:** `classify(victim, source)` traduce un `DamageSource` nella `DamageKind` del core (`FALL`, poi `FROM_ABOVE` per proiettili tirati da oltre `eyeY + 1.0`, poi `HEAD` per posizione sorgente con `y >= eyeY - 0.25`, altrimenti `GENERIC`); `isProjectile`, `isFall`, `bypassesArmor` interrogano i tag vanilla.
- **Differenze chiuse:** `DamageSource.isIndirect()` esiste solo su 1.20.1 (su 1.21.1 c'è `isDirect()` con logica invertita): la classificazione passa invece dai `DamageTypeTags`, identici sulle due versioni.
- **Condizione:** nessuna: tag e metodi usati sono identici sulle due versioni. L'interfaccia tiene comunque la logica, che dipende da classi di Minecraft, fuori da `core/`.
- **Uso:** `Compat.damage().classify(player, source)`.

### `FrameStateAccess` — stato del telaio per giocatore

- **File:** [`FrameStateAccess.java`](../src/main/java/com/toxineren/exoframes/compat/FrameStateAccess.java), implementazione [`FrameStateAccessImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/FrameStateAccessImpl.java) con [`FrameStateCodec.java`](../src/main/java/com/toxineren/exoframes/compat/impl/FrameStateCodec.java) (helper di schema condivisi con gli item in [`SchemaIds.java`](../src/main/java/com/toxineren/exoframes/compat/impl/SchemaIds.java) e [`PieceTags.java`](../src/main/java/com/toxineren/exoframes/compat/impl/PieceTags.java)), [`FrameStateSavedData.java`](../src/main/java/com/toxineren/exoframes/compat/impl/FrameStateSavedData.java), [`FrameStateSyncPacket.java`](../src/main/java/com/toxineren/exoframes/compat/impl/FrameStateSyncPacket.java), [`FrameStateClientCache.java`](../src/main/java/com/toxineren/exoframes/compat/impl/FrameStateClientCache.java).
- **A cosa serve:** `get(player)` (dato salvato sul server, copia sincronizzata sul client) e `clientView` restituiscono lo stato vivo, da trattare in sola lettura; ogni modifica passa da uno dei tre metodi di scrittura, solo sul server: `update(serverPlayer, change)` crea lo stato se assente, applica, chiama `setDirty()` e sincronizza il giocatore (`setDirty()` e la sincronizzazione avvengono anche se `change` lancia); `mutate(serverPlayer, change)` applica la modifica solo se lo stato esiste (altrimenti restituisce `false`), chiama `setDirty()` (anche se `change` lancia) e **non** sincronizza: è il percorso per le modifiche ad alta frequenza come il consumo di energia a ogni tick, e il chiamante invia lo stato con `syncTo` al proprio ritmo (al massimo 4-5 volte al secondo); `clear` rimuove lo stato, chiama `setDirty()` e sincronizza l'assenza. Poi `syncTo`; `registerCommon()` (pacchetto S2C ed eventi di ingresso e respawn, dall'init comune) e `registerClient()` (pulizia della copia all'uscita dal mondo, solo dagli entrypoint client).
- **Differenze chiuse:** persistenza con un unico `SavedData` nell'overworld (`data/redstone_exoframes_frames.dat`, mappa UUID → stato) uguale sui quattro target, al posto di capability Forge, attachment NeoForge o data-attachment Fabric; tra versioni cambiano la firma di `SavedData.save` (con `HolderLookup.Provider` su 1.21.1), `DimensionDataStorage.computeIfAbsent` (`Function, Supplier, String` su 1.20.1, `SavedData.Factory, String` su 1.21.1) e la lambda di `PlayerEvent.PLAYER_RESPAWN` di Architectury (un parametro in più su 1.21.1). Codec e pacchetto (UUID, flag di presenza, tag) sono identici.
- **Condizione:** `FrameStateSavedData` (save, computeIfAbsent) e `FrameStateAccessImpl` (evento di respawn).
- **Uso:** `Compat.frameState().update(serverPlayer, state -> state.setUnlocked(true))`.

### Fuori dalle interfacce: `Ids`

[`Ids.java`](../src/main/java/com/toxineren/exoframes/compat/Ids.java) costruisce i `ResourceLocation` (`Ids.of(path)`, `Ids.of(namespace, path)`): `new ResourceLocation(ns, path)` su 1.20.1, `ResourceLocation.fromNamespaceAndPath` su 1.21.1. È una classe statica usata direttamente dal codice dei contenuti, ed è l'unico file di `compat/` fuori da `impl/` ammesso a contenere `//? if`.

## Tabella API vanilla/loader → interfaccia

Ripresa da [`docs/audit/mjolnir-armor-audit.md`](audit/mjolnir-armor-audit.md) §4 e aggiornata a ciò che esiste dopo la Milestone 2b. "Rinviata" indica le classi previste che arrivano con i loro consumatori; "—" indica che in M2b non c'è un'interfaccia (la proposta dell'audit non è stata costruita: si decide quando arriva il primo uso).

| API che cambia | Copertura oggi |
|---|---|
| `new ResourceLocation(ns, path)` → `ResourceLocation.fromNamespaceAndPath` | `Ids` |
| NBT sull'ItemStack (`getTag`, `getOrCreateTag`) → data components (`custom_data`) | `ItemDataAccess` (`ItemPayload`) |
| `ItemStack.save(CompoundTag)` / `ItemStack.of` → `save(HolderLookup.Provider)` / `parseOptional` | Rinviata (M3/M4): serve agli item riposti, chiave `stowed` definita in `NbtKeys` ma non ancora scritta né letta |
| `Item.Properties.durability(-1)` non va usato (su 1.21.1 l'item risulta già al limite); su 1.20.1 `stacksTo` lancia se c'è già una durabilità | `ArmorMaterials`: `newPlaceholder` dà sempre una pila da 1 e accetta proprietà con o senza durabilità vanilla; l'usura dei pezzi vive in `core/` e in `ItemDataAccess` |
| `ItemStack.isDamageableItem` (maxDamage vs presenza dei componenti) | `ItemDataAccess` (`durability`/`max_durability`); lo stato rotto vive in `core/` |
| `ArmorMaterial` enum → record registrato `Holder<ArmorMaterial>` con layer; `ArmorItem(Holder, Type, Properties)` | `ArmorMaterials` |
| `ArmorMaterial.getName()` con namespace rompe `HumanoidArmorLayer` su Fabric 1.20.1 | `ArmorMaterials` (nome `frame` senza namespace su 1.20.1) |
| `FriendlyByteBuf` encode/decode → `CustomPacketPayload` + `StreamCodec` | `Net` + `PacketCodec` |
| Pacchetti C2S `redstone_exoframes:module_toggle`, `frame_exit`, `cell_swap` | Rinviati (M3/M4): arriveranno con i consumatori, validati sul server con `Compat.frameState()` (regola 5) |
| Extra data dei menu (restano `FriendlyByteBuf` con `MenuRegistry` di Architectury) | — (nessuna interfaccia; arriva con la stazione) |
| `AttributeModifier` UUID → `ResourceLocation`, nomi delle operazioni, `removeModifier`, `Attributes.*` come `Holder` | `FrameAttributes` (l'audit la chiamava `Attributes`) |
| `SavedData.save` con `HolderLookup.Provider`; `DimensionDataStorage.computeIfAbsent` con `SavedData.Factory` (non nell'audit) | `FrameStateAccess` (`FrameStateSavedData`) |
| Stato per giocatore: capability Forge / attachment NeoForge / data-attachment Fabric (audit §3) | `FrameStateAccess`: un `SavedData` comune ai quattro target |
| `PlayerEvent.PLAYER_RESPAWN` di Architectury con un parametro in più su 13.0 (non nell'audit) | `FrameStateAccess` (`FrameStateAccessImpl.registerCommon`) |
| `DamageSource.isIndirect()` solo su 1.20.1 (`isDirect()` su 1.21.1) | `DamageClassifier` (tag `DamageTypeTags`) |
| Lato, loader, cartella di config, mod caricate | `Platform` |
| `CustomRecipe` / `CraftingContainer` → `CraftingInput` | — |
| `Item.getUseDuration(ItemStack)` → `(ItemStack, LivingEntity)`; `appendHoverText` con `TooltipContext` | — |
| `MobEffect` → `Holder<MobEffect>` | — |
| `Block.use(...)` → `useWithoutItem` / `useItemOn` | Rinviata (M3/M4): `CompatEntityBlock` |
| `saveAdditional/load(CompoundTag)` → con `HolderLookup.Provider`; `getUpdateTag(Provider)` | Rinviata (M3/M4): `CompatBlockEntity` |
| `SynchedEntityData`: `defineSynchedData()` → `defineSynchedData(Builder)`; `EquipmentSlot.Type.ARMOR` → `HUMANOID_ARMOR` | Rinviata (M3/M4): `CompatEntity` |
| `Screen.renderBackground` con firma diversa; `InventoryScreen.renderEntityInInventory`; `Model.renderToBuffer` con colore int; package GeckoLib; `Minecraft.getFrameTime()` → `getTimer()`; `Tesselator`/`BufferUploader`; callback HUD di Architectury con `DeltaTracker`; `setupRotations` con `scale` | Rinviate (M3/M4): basi client (`CompatContainerScreen`, basi GeckoLib, `ClientTiming`) |
| Id delle loot table `ResourceLocation` → `ResourceKey<LootTable>`; condizione `forge:loot_table_id` → `neoforge:` | Rinviata: `LootHooks` (con il bottino, M6 secondo il piano M2b) |
| `BlockEntity.load` di cartelli/stendardi, `Component.Serializer.toJson` con Provider (worldgen) | — (M6) |
| Mixin: `SinglePoolElement.place` con `LiquidSettings`; `ThrownPotion.applySplash(Iterable)` | — (nessun mixin in M2b) |
| Cartelle dati al singolare su 1.21, tag `forge:` → `c:`; tag `minecraft:no_knockback` solo su 1.21.1 | Non codice: dati per variante |
| `pack_format` 15 → 34 | Proprietà per variante `mod.pack_format` (`versions/*/gradle.properties`) |
| `compatibilityLevel` dei mixin JAVA_17 → JAVA_21 | Non codice: `mixins.json` per variante, quando servirà |
| `DropExperienceBlock`, `new AABB(BlockPos)`, `BlockEntityType.Builder.build(null)` e le API stabili elencate nell'audit | Nessuna |

## Schema NBT ([`NbtKeys`](../src/main/java/com/toxineren/exoframes/compat/NbtKeys.java))

`NbtKeys` è l'unica fonte delle chiavi. Gli item usano il tag radice `exoframes` (nel tag dello stack su 1.20.1, in `custom_data` su 1.21.1); lo stato del telaio per giocatore (`FrameStateCodec`) riusa le stesse chiavi e gli stessi tipi. Le conversioni comuni ai due codec stanno in due helper: [`SchemaIds`](../src/main/java/com/toxineren/exoframes/compat/impl/SchemaIds.java) (id di slot, livelli e moduli, lettura dell'UUID valida solo per un int array di 4 elementi, altrimenti assente, e avvisi una volta per chiave, con l'insieme delle chiavi limitato a 256 voci e gli id nel log troncati a 64 caratteri e senza caratteri di controllo) e [`PieceTags`](../src/main/java/com/toxineren/exoframes/compat/impl/PieceTags.java) (lettura di pezzo ed energia con gli intervalli garantiti descritti sopra per `ItemDataAccess`). Così i due codec leggono id, UUID, pezzi ed energia allo stesso modo; la scrittura e il resto dello schema restano in ciascun codec (gli item hanno i colori, lo stato del telaio il riporto, la capacità della cella e i toggle). Lo stato del telaio ripristina un toggle acceso solo se il modulo è di tipo `TOGGLE` ed è installato in qualche slot.

| Chiave | Tipo | Dove |
|---|---|---|
| `exoframes` | compound | radice dei dati sugli item |
| `energy` | int | cella (item); dentro `cell` |
| `tier` | string (`Tier#id`) | pezzo; voce di `pieces` |
| `durability`, `max_durability` | int | pezzo; voce di `pieces` |
| `modules` | lista di string (`ModuleType#id`) | pezzo; voce di `pieces` |
| `color_primary`, `color_secondary` | int RGB | pezzo (item) |
| `owner` | UUID come int array | telaio (assente = nessun proprietario) |
| `unlocked`, `reinforced` | boolean | telaio |
| `cell` | compound con `energy` (e `capacity` nello stato) | telaio (assente = nessuna cella) |
| `pieces` | compound, una voce per `PieceSlot` in minuscolo | telaio |
| `stowed` | lista di tag item | definita, non ancora usata (rinviata) |
| `capacity` | int | solo stato del telaio, dentro `cell` (Task 8) |
| `active` | lista di string (`ModuleType#id`) | solo stato del telaio: toggle accesi (Task 8) |
| `carry_milli` | int in [0, 999] | solo stato del telaio, voce di `pieces`: riporto del danno in millesimi (Task 8) |
| `schema` | int (assente = 1) | solo radice del dato salvato degli stati, accanto a `frames`; nessun marcatore sugli item |

Il `SavedData` salva gli stati sotto la chiave `frames`, una voce per UUID del giocatore in forma testuale, e accanto il marcatore `schema` (oggi 1). In lettura ogni voce è isolata: una voce che non si legge (valore non compound, chiave non UUID, eccezione del lettore) viene saltata con un avviso nel log che ne nomina la chiave, resta in memoria come tag grezzo e viene riscritta identica a ogni salvataggio; si toglie solo quando lo stato di quel giocatore viene creato o sostituito con `update()` o rimosso con `clear()`. Le altre voci si leggono normalmente, invece di far ricreare vuoto l'intero dato. Se `schema` è maggiore di quello noto (un mondo aperto con una versione della mod più vecchia di quella che l'ha salvato), un solo ERROR nel log e tutte le voci restano grezze: nessuna viene letta, nessuna si perde, e il salvataggio conserva il marcatore letto.

## Regole di [`tools/check_sources.sh`](../tools/check_sources.sh)

Lo script gira dalla radice del repository (`bash tools/check_sources.sh`), stampa `OK sources` se tutto passa e altrimenti un `FAIL` per regola con i file coinvolti, uscendo con codice 1.

1. `core/` non importa `net.minecraft`, `net.fabricmc`, `net.minecraftforge`, `net.neoforged`, `dev.architectury` (anche con `import static`).
   - **1b.** `core/` non importa `com.google.gson`.
   - **1c.** Ogni tipo di `core/` è citato per nome in almeno un file di `src/test/java`.
2. Le condizioni Stonecutter (`//? if`) stanno solo in `compat/` e `platform/`.
3. Forge: `RedstoneExoframesForge` chiama `EventBuses.registerModEventBus` prima di `RedstoneExoframes.init()`.
4. `com.toxineren.exoframes.compat.impl` si importa (anche con `import static`) solo da file di `compat/`, in `src/main/java` e `src/test/java`.
5. Ogni file che chiama `registerC2S(` (tranne `Net` e `NetImpl`) referenzia `Compat.frameState()`: i gestori lato server validano le richieste sullo stato del telaio del giocatore.
6. Regola finale, più stretta della 2: `//? if` è ammesso solo in `compat/impl/`, in `platform/` e in `compat/Ids.java`.
