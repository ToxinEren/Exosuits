# Interfacce `compat/`: cosa chiudono e dove

Il package [`com.toxineren.exoframes.compat`](../src/main/java/com/toxineren/exoframes/compat/) è l'unico punto in cui il codice della mod vede le differenze tra Minecraft 1.20.1 e 1.21.1 e tra i loader (Fabric, Forge, NeoForge). Il resto del codice (`content/`, e in futuro `entity/`, `block/`, `item/`, `client/`) chiama solo le interfacce di `compat/` tramite il punto di accesso [`Compat`](../src/main/java/com/toxineren/exoframes/compat/Compat.java); `core/` non dipende da Minecraft.

Regole di struttura (Milestone 2b):

- ogni interfaccia ha una sola implementazione `...Impl` in [`compat/impl/`](../src/main/java/com/toxineren/exoframes/compat/impl/), creata pigramente da `Compat` (idioma del holder);
- le direttive Stonecutter (condizioni come `//? if >=1.21`, `//? if fabric`, `//? if forge`, e qualunque altra direttiva `//?`, `//$`, `//~`, anche in un commento a blocco) stanno solo nelle classi di `compat/impl/`, negli entrypoint di [`platform/`](../src/main/java/com/toxineren/exoframes/platform/) e nell'unica eccezione [`compat/Ids.java`](../src/main/java/com/toxineren/exoframes/compat/Ids.java); le interfacce non hanno condizioni e i test non hanno direttive;
- `compat.impl` si referenzia (import o nome qualificato) solo da dentro `compat/`;
- il codice solo client sta nei package `client`, `compat.client` e `compat.impl.client`, che si referenziano solo da quei package e dagli entrypoint di `platform/`: un server dedicato non li carica mai (vedi "Cuciture client" e la regola 7).

Tutto è verificato da [`tools/check_sources.sh`](../tools/check_sources.sh) (vedi in fondo).

## Le nove interfacce comuni

### `Platform` — ambiente di esecuzione

- **File:** [`Platform.java`](../src/main/java/com/toxineren/exoframes/compat/Platform.java), implementazione [`PlatformImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/PlatformImpl.java).
- **A cosa serve:** lato fisico (`isClient()`, `isDedicatedServer()`), ambiente di sviluppo (`isDevelopment()`), cartella di configurazione (`configDir()`), mod caricate (`isModLoaded(id)`), nome del loader (`loaderName()`: `"fabric"`, `"forge"` o `"neoforge"`).
- **Differenze chiuse:** le API di ambiente dei tre loader, tramite `dev.architectury.platform.Platform` (uguale sulle due versioni di Architectury). Il nome del loader è una costante scelta per variante.
- **Condizione:** `PlatformImpl` (`//? if fabric` / `forge` / altrimenti `neoforge`).
- **Uso:** `Compat.platform().configDir()`.

### `ItemDataAccess` — dati tipizzati sugli item

- **File:** [`ItemDataAccess.java`](../src/main/java/com/toxineren/exoframes/compat/ItemDataAccess.java), implementazione [`ItemDataAccessImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/ItemDataAccessImpl.java) con l'accesso grezzo in [`ItemPayload.java`](../src/main/java/com/toxineren/exoframes/compat/impl/ItemPayload.java).
- **A cosa serve:** energia delle celle (`cellEnergy`/`setCellEnergy`), dati dei pezzi (`piece`/`setPiece` con il record `PieceData`: tier, durabilità, durabilità massima, moduli, aspetto `Appearance` con due colori e le decalcomanie), dati del telaio come item (`frame`/`setFrame` con il record `FrameItemData`: proprietario `Optional<UUID>`, sbloccato, rinforzato, energia della cella `Optional<Integer>`, pezzi per slot), riassunto visivo del telaio indossato (`suitSummary`/`setSuitSummary` con il record `SuitSummary`, vedi sotto), presenza dei dati (`hasData`). I record rifiutano componenti null. In lettura (dati non fidati: `/give`, inventario creativo, altre mod) gli intervalli sono garantiti: un pezzo ha `maxDurability > 0` (altrimenti nessun pezzo) e `durability` in [0, `maxDurability`]; i moduli sono noti, senza duplicati, nell'ordine salvato e al massimo uno per tipo; i colori valgono `Appearance.UNPAINTED` (−1, colore predefinito del livello) se la chiave manca e altrimenti sono ridotti a 24 bit; le decalcomanie valide sono al massimo `Appearance.MAX_DECALS` (4) e quelle malformate si saltano; l'energia della cella è sempre `>= 0` (il limite superiore, la capacità, resta al chiamante).
- **Riassunto visivo:** [`SuitSummary`](../src/main/java/com/toxineren/exoframes/compat/SuitSummary.java) è la descrizione solo visiva del telaio indossato: per ogni slot con un pezzo montato un `PieceLook` (livello, `PieceCondition`, `Appearance`); la mappa è una copia non modificabile, `SuitSummary.BARE` è il telaio nudo e `SuitSummary.of(FrameState)` lo ricava dallo stato (livello e condizione del pezzo, aspetto dello slot). Sta nei dati del segnaposto del torso sotto la chiave `visual`, così la sincronizzazione vanilla degli slot armatura lo porta a ogni client che vede il giocatore, su ogni loader e versione; il layer di rendering lo legge. `suitSummary` è vuoto se manca il tag radice o se `visual` manca o non è un compound; un `visual` senza `pieces` compound vale il telaio nudo. Lettura tollerante e mai con eccezioni ([`SuitSummaryTags`](../src/main/java/com/toxineren/exoframes/compat/impl/SuitSummaryTags.java)): si guardano solo le voci dei sei slot noti (le chiavi sconosciute o in eccesso si ignorano), una voce non compound o con livello assente, non string o sconosciuto si salta, una condizione assente, non string o sconosciuta vale `INTACT`, l'aspetto si legge con `PieceTags` con le garanzie dei pezzi. `setSuitSummary` sostituisce tutto `visual` e conserva le altre chiavi del tag radice.
- **Differenze chiuse:** su 1.20.1 i dati stanno nel tag NBT dello stack (`getTagElement`/`getOrCreateTag`); su 1.21.1 nel componente vanilla `minecraft:custom_data` (nessun componente registrato dalla mod). Il contenuto del tag radice `exoframes` è identico, così il DFU di vanilla, che lo copia così com'è in `custom_data`, conserva i dati primitivi di un mondo 1.20.1 aggiornato a 1.21.1; gli stack annidati (gli item riposti di `stowed`) non vengono convertiti e avranno bisogno di una migrazione esplicita. Le scritture conservano le chiavi non toccate e non fanno nulla su uno stack vuoto.
- **Condizione:** solo `ItemPayload` (lettura e scrittura del tag radice); `ItemDataAccessImpl` e `SuitSummaryTags` non hanno condizioni.
- **Uso:** `Compat.itemData().cellEnergy(stack)`.

### `ArmorMaterials` — materiale d'armatura segnaposto

- **File:** [`ArmorMaterials.java`](../src/main/java/com/toxineren/exoframes/compat/ArmorMaterials.java), implementazione [`ArmorMaterialsImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/ArmorMaterialsImpl.java).
- **A cosa serve:** il materiale "frame" a difesa, robustezza, contraccolpo e incantabilità zero, condiviso dai quattro slot dei pezzi (la protezione reale è un attributo, vedi `FrameAttributes`). `register()` va chiamato una volta sola nell'init comune prima degli item; `newPlaceholder(type, props)` crea l'`ArmorItem` con la pila sempre a 1 su entrambe le versioni, e le proprietà possono avere o no una durabilità vanilla (su 1.21.1 si imposta `stacksTo(1)`; su 1.20.1 le proprietà passano invariate, perché lì `stacksTo` lancia se c'è già una durabilità e il costruttore di `ArmorItem` porta comunque la pila a 1); `frameMaterialHandle()` restituisce il materiale nella forma della versione (tipo `Object`).
- **Differenze chiuse:** su 1.20.1 `ArmorMaterial` è un'interfaccia implementata da un enum privato, con il nome `frame` senza namespace perché con un namespace il percorso della texture si romperebbe su Fabric 1.20.1; su 1.21.1 è un record registrato nel registro vanilla `armor_material` (id `redstone_exoframes:frame`, con il `DeferredRegister` di Architectury) e `ArmorItem` prende un `Holder<ArmorMaterial>`.
- **Condizione:** `ArmorMaterialsImpl`.
- **Uso:** `Compat.armorMaterials().newPlaceholder(ArmorItem.Type.HELMET, new Item.Properties())`.

### `Net` — pacchetti di rete tipizzati

- **File:** [`Net.java`](../src/main/java/com/toxineren/exoframes/compat/Net.java) con [`PacketCodec.java`](../src/main/java/com/toxineren/exoframes/compat/PacketCodec.java), implementazione [`NetImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/NetImpl.java).
- **A cosa serve:** `registerS2C`/`registerC2S` (id, classe del messaggio, `PacketCodec`, gestore) da chiamare nell'init comune su entrambi i lati; `sendToPlayer`/`sendToServer`. I gestori girano sempre sul thread di gioco di chi riceve; le loro eccezioni restano nel log della piattaforma. Inviare su un id non registrato o registrato per la direzione opposta lancia `IllegalStateException`, un messaggio del tipo sbagliato `IllegalArgumentException`. Solo su NeoForge 1.21.1 `sendToPlayer` chiede prima ad Architectury (`NetworkManager.canPlayerReceive` con il `CustomPacketPayload.Type`, che lì legge i canali negoziati nella fase di configurazione) se il client del giocatore ha il canale, e se no salta l'invio in silenzio (una riga DEBUG per id): lì i payload di Architectury sono opzionali, un client senza la mod supera la negoziazione e un invio senza controllo lancerebbe un'eccezione dentro l'evento di ingresso, con il giocatore disconnesso da un messaggio fuorviante. Sugli altri tre target il pacchetto si invia sempre e un client senza il canale lo ignora; su 1.20.1 il controllo sarebbe anzi sbagliato, perché i canali del client arrivano al server solo dopo l'evento di ingresso.
- **Differenze chiuse:** su 1.20.1 (Architectury 9.2) canali grezzi con `FriendlyByteBuf` (decodificato prima di accodare il gestore); su 1.21.1 (Architectury 13.0) `CustomPacketPayload.Type` + `StreamCodec` su `RegistryFriendlyByteBuf`, con un record `Payload` interno. Lato server dedicato 1.21.1 si registra solo il tipo del payload S2C (`registerS2CPayloadType`), mai insieme al ricevitore.
- **Condizione:** `NetImpl` (versione; la guardia `canPlayerReceive` con `//? if >=1.21 && neoforge`).
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
- **A cosa serve:** `get(player)` (dato salvato sul server, copia sincronizzata sul client) e `clientView` restituiscono lo stato vivo, da trattare in sola lettura; ogni modifica passa da uno dei tre metodi di scrittura, solo sul server: `update(serverPlayer, change)` crea lo stato se assente, applica, chiama `setDirty()` e sincronizza il giocatore (`setDirty()` e la sincronizzazione avvengono anche se `change` lancia); `mutate(serverPlayer, change)` applica la modifica solo se lo stato esiste (altrimenti restituisce `false`), chiama `setDirty()` (anche se `change` lancia) e **non** sincronizza: è il percorso per le modifiche ad alta frequenza come il consumo di energia a ogni tick, e il chiamante invia lo stato con `syncTo` al proprio ritmo (al massimo 4-5 volte al secondo); `clear` rimuove lo stato, chiama `setDirty()` e sincronizza l'assenza. Se il dato salvato viene da uno schema più recente, le tre scritture non fanno nulla (vedi lo schema NBT più sotto). Poi `syncTo`; `registerCommon()` (pacchetto S2C ed eventi di ingresso e respawn, dall'init comune) e `registerClient()` (pulizia della copia all'uscita dal mondo, solo dagli entrypoint client).
- **Differenze chiuse:** persistenza con un unico `SavedData` nell'overworld (`data/redstone_exoframes_frames.dat`, mappa UUID → stato) uguale sui quattro target, al posto di capability Forge, attachment NeoForge o data-attachment Fabric; tra versioni cambiano la firma di `SavedData.save` (con `HolderLookup.Provider` su 1.21.1), `DimensionDataStorage.computeIfAbsent` (`Function, Supplier, String` su 1.20.1, `SavedData.Factory, String` su 1.21.1) e la lambda di `PlayerEvent.PLAYER_RESPAWN` di Architectury (un parametro in più su 1.21.1). Codec e pacchetto (UUID, flag di presenza, tag) sono identici.
- **Condizione:** `FrameStateSavedData` (save, computeIfAbsent) e `FrameStateAccessImpl` (evento di respawn).
- **Uso:** `Compat.frameState().update(serverPlayer, state -> state.setUnlocked(true))`.

### `GameTests` — test in gioco

- **File:** [`GameTests.java`](../src/main/java/com/toxineren/exoframes/compat/GameTests.java), implementazione [`GameTestsImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/GameTestsImpl.java); i test comuni stanno in [`gametest/ExoframeGameTests.java`](../src/main/java/com/toxineren/exoframes/gametest/ExoframeGameTests.java).
- **A cosa serve:** `register(testClass)` aggancia la classe dei test in gioco (GameTest vanilla) al framework del loader, solo se è attivo il server di test (mai in produzione; altrimenti non fa nulla), dall'init comune; `mockServerPlayer(helper)` crea un giocatore finto connesso nel livello del test. I test si lanciano con `./gradlew :<variante>:runGameTestServer` o, sulle quattro varianti, con `bash tools/run_gametests.sh`.
- **Differenze chiuse:** registrazione: su Forge e NeoForge un ascoltatore di `RegisterGameTestsEvent` sul bus del mod (bus di Architectury su Forge, del `ModContainer` su NeoForge), aggiunto solo se `isGametestServer()`; su Fabric `register` non fa nulla, perché la classe è l'entrypoint `fabric-gametest` di `fabric.mod.json`. Giocatore finto: su 1.21.1 `GameTestHelper.makeMockServerPlayerInLevel()`; su 1.20.1 un `ServerPlayer` creativo con una `Connection` agganciata a un `EmbeddedChannel` (senza canale Forge lancia un NPE all'ingresso e Fabric lascia i pacchetti in coda).
- **Condizione:** `GameTestsImpl` (loader in `register`, versione in `mockServerPlayer`).
- **Uso:** `Compat.gameTests().mockServerPlayer(helper)`.

### `SuitAssets` — item modello della tuta

- **File:** [`SuitAssets.java`](../src/main/java/com/toxineren/exoframes/compat/SuitAssets.java), implementazione [`SuitAssetsImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/SuitAssetsImpl.java) con [`SuitModelItem.java`](../src/main/java/com/toxineren/exoframes/compat/impl/SuitModelItem.java).
- **A cosa serve:** `newSuitModelItem(props)` crea l'item "modello della tuta", l'unico `GeoItem` della mod: l'animatable che il renderer della tuta (vedi "Cuciture client") disegna con uno stack fittizio. È registrato come `redstone_exoframes:suit_model` (`ModItems.SUIT_MODEL`), perché GeckoLib 4.9 interroga i tag dello stack, ma non si indossa mai, non sta nella scheda creativa e ha un modello d'item normale, così gli agganci di GeckoLib per armature e item non lo trattano. Non ha animazioni.
- **Differenze chiuse:** i package di `AnimatableInstanceCache` e `AnimatableManager` (sotto `core` in GeckoLib 4.8, alla radice in 4.9); su Fabric 1.20.1 (GeckoLib 4.8.4) `createRenderer` e `getRenderProvider` sono astratti: il fornitore viene da `GeoItem.makeRenderer`, che sul server dedicato non chiama `createRenderer`, e sul client `createRenderer` dà `RenderProvider.DEFAULT` (`RenderProvider.of` non accetta un fornitore nullo). Forge 1.20.1 non ha quei metodi, GeckoLib 4.9 li ha di default.
- **Condizione:** `SuitModelItem` (versione per gli import, `<1.21 && fabric` per i due metodi).
- **Server dedicato:** interfaccia, implementazione e item sono codice comune che il server carica: nessun campo, inizializzatore statico o lambda tocca classi client. Il test in mondo `suitModelItemIsRegistered` gira sul server di test dedicato delle quattro varianti.
- **Uso:** `ModItems.SUIT_MODEL` (registrazione); nessun altro chiamante.

### Fuori dalle interfacce: `SuitBones`

[`SuitBones.java`](../src/main/java/com/toxineren/exoframes/compat/SuitBones.java) è l'unico posto in cui il codice costruisce i nomi delle ossa della geometria della tuta (codice comune, senza dipendenze client): le costanti delle sei ossa di primo livello (`ARMOR_HEAD` … `ARMOR_LEFT_LEG`, elencate in `TOP_LEVEL`), `frame(parte)` per il telaio nudo (`frameHead`, parti in `PARTS`), `piece(slot, stato, regione)` per i pezzi (`pieceLeftArm_s2_p1`; stato in [0, 3], regione `p1`, `p2` o `n`, altrimenti `IllegalArgumentException`) e `decal(slot)` (`pieceTorso_decal`; gli slot con un osso di decalcomania sono in `DECAL_SLOTS`). `ExoframeGeometryTest` confronta questi nomi con il file generato.

### Fuori dalle interfacce: `Ids`

[`Ids.java`](../src/main/java/com/toxineren/exoframes/compat/Ids.java) costruisce i `ResourceLocation` (`Ids.of(path)`, `Ids.of(namespace, path)`): `new ResourceLocation(ns, path)` su 1.20.1, `ResourceLocation.fromNamespaceAndPath` su 1.21.1. È una classe statica usata direttamente dal codice dei contenuti, ed è l'unico file di `compat/` fuori da `impl/` ammesso a contenere direttive Stonecutter.

## Cuciture client

Il codice solo client vive in tre package: `client/` (codice comune della mod che gira solo sul client), [`compat/client/`](../src/main/java/com/toxineren/exoframes/compat/client/) (interfacce e tipi neutri delle cuciture client) e [`compat/impl/client/`](../src/main/java/com/toxineren/exoframes/compat/impl/client/) (implementazioni, con le condizioni di versione). Nessun file fuori da quei package e da `platform/` li referenzia, e `RedstoneExoframes.init()` nemmeno: un server dedicato non li carica mai (regola 7 di `check_sources.sh`). Il punto di accesso è [`ClientCompat`](../src/main/java/com/toxineren/exoframes/compat/client/ClientCompat.java), con lo stesso idioma del holder di `Compat`.

### `SuitRenderer` — renderer della tuta

- **File:** [`SuitRenderer.java`](../src/main/java/com/toxineren/exoframes/compat/client/SuitRenderer.java) e [`SuitView.java`](../src/main/java/com/toxineren/exoframes/compat/client/SuitView.java), implementazione [`SuitRendererImpl.java`](../src/main/java/com/toxineren/exoframes/compat/impl/client/SuitRendererImpl.java) con il modello GeckoLib [`SuitModel.java`](../src/main/java/com/toxineren/exoframes/compat/impl/client/SuitModel.java).
- **A cosa serve:** `render(entità, modello umanoide di base, PoseStack, buffer, luce, partialTick, SuitView)` disegna telaio e pezzi in una sola passata, con una sola texture (`textures/entity/exoframe.png`) e la geometria `geo/exoframe.geo.json`. Le sei ossa di primo livello copiano la posa dal modello di base (come fa GeckoLib per le armature); la visibilità non legge i flag `visible` del modello di base: le ossa di primo livello e del telaio sono sempre visibili; per ogni slot con un pezzo si mostrano le tre ossa dello stato dato e si nascondono le altre nove, per uno slot senza pezzo tutte e dodici; le ossa `_decal` restano nascoste. `_p1` si colora con il colore primario, `_p2` con il secondario, `_n` resta bianca. Un osso cercato e assente nella geometria si salta, e il suo nome finisce nel log (WARN) una volta sola per sessione. Scala e spostamenti della tuta rispetto al giocatore li applica il chiamante sul `PoseStack`. Va chiamato sul thread di rendering; la prima chiamata a `ClientCompat.suitRenderer()` costruisce il renderer, che prende i modelli delle entità da Minecraft, quindi mai prima del caricamento delle risorse.
- **`SuitView`:** record neutro, `pieces` è una mappa non modificabile slot → `PieceView(stato, primaryRgb, secondaryRgb)`; mappa vuota = telaio nudo. `SuitView.of(riassunto, predefinitoPrimario, predefinitoSecondario)` la ricava da un `SuitSummary`: stato 0 `INTACT`, 1 `DAMAGED`, 2 `CRITICAL`, 3 `BROKEN`; un colore `UNPAINTED` prende il predefinito del livello. `PieceView` rifiuta uno stato fuori da [0, 3] e tiene dei colori solo i 24 bit RGB (mai −1).
- **Differenze chiuse:** il renderer estende `DyeableGeoArmorRenderer<SuitModelItem>` di `software.bernie.geckolib.renderer` (GeckoLib 4.8, 1.20.1) o `renderer.specialty` (4.9, 1.21.1), e usa il `Color` di `core.object` o di `util`; `prepForRender` a 4 argomenti in 4.8 e a 10 in 4.9 (dove quella a 4 è deprecata); i colori passano da `getColorForBone`, con la stessa firma nelle due versioni, così i ganci con il colore a quattro float (4.8) o intero (4.9) restano quelli di GeckoLib. Lo stack fittizio è dell'item `ModItems.SUIT_MODEL` nello slot del torso. Dopo ogni disegno il renderer azzera entità, stack e modello di base anche su Forge 1.20.1, dove GeckoLib non lo fa.
- **Condizione:** `SuitRendererImpl` (import e `prepForRender`); `SuitModel` non ne ha bisogno.
- **Uso:** `ClientCompat.suitRenderer().render(player, getParentModel(), pose, buffers, light, partialTick, SuitView.of(summary, primarioDelLivello, secondarioDelLivello))`, dal layer sul giocatore.

## Tabella API vanilla/loader → interfaccia

Ripresa da [`docs/audit/mjolnir-armor-audit.md`](audit/mjolnir-armor-audit.md) §4 e aggiornata a ciò che esiste dopo la Milestone 2b. "Rinviata" indica le classi previste che arrivano con i loro consumatori; "—" indica che in M2b non c'è un'interfaccia (la proposta dell'audit non è stata costruita: si decide quando arriva il primo uso).

| API che cambia | Copertura oggi |
|---|---|
| `new ResourceLocation(ns, path)` → `ResourceLocation.fromNamespaceAndPath` | `Ids` |
| NBT sull'ItemStack (`getTag`, `getOrCreateTag`) → data components (`custom_data`) | `ItemDataAccess` (`ItemPayload`) |
| `ItemStack.save(CompoundTag)` / `ItemStack.of` → `save(HolderLookup.Provider)` / `parseOptional` | Rinviata alla Milestone 3 (ingresso e uscita dal telaio, che stiva l'armatura civile): chiave `stowed` definita in `NbtKeys` ma non ancora scritta né letta; vedi "Limiti noti e rinvii" |
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
| Package GeckoLib (`core.*` in 4.8, radice in 4.9; `DyeableGeoArmorRenderer` in `renderer.specialty` in 4.9), colore dei ganci di rendering a quattro float o intero, `prepForRender` a 4 o 10 argomenti, metodi astratti di `GeoItem` su Fabric 4.8.4 | `SuitRenderer` (client) e `SuitAssets` (`SuitModelItem`) |
| `Screen.renderBackground` con firma diversa; `InventoryScreen.renderEntityInInventory`; `Model.renderToBuffer` con colore int; package GeckoLib; `Minecraft.getFrameTime()` → `getTimer()`; `Tesselator`/`BufferUploader`; callback HUD di Architectury con `DeltaTracker`; `setupRotations` con `scale` | Rinviate (M3/M4): basi client (`CompatContainerScreen`, basi GeckoLib, `ClientTiming`) |
| Id delle loot table `ResourceLocation` → `ResourceKey<LootTable>`; condizione `forge:loot_table_id` → `neoforge:` | Rinviata: `LootHooks` (con il bottino, M6 secondo il piano M2b) |
| `BlockEntity.load` di cartelli/stendardi, `Component.Serializer.toJson` con Provider (worldgen) | — (M6) |
| Mixin: `SinglePoolElement.place` con `LiquidSettings`; `ThrownPotion.applySplash(Iterable)` | — (nessun mixin in M2b) |
| Cartelle dati al singolare su 1.21, tag `forge:` → `c:`; tag `minecraft:no_knockback` solo su 1.21.1 | Non codice: dati per variante |
| `pack_format` 15 → 34 | Proprietà per variante `mod.pack_format` (`versions/*/gradle.properties`) |
| `compatibilityLevel` dei mixin JAVA_17 → JAVA_21 | Non codice: `mixins.json` per variante, quando servirà |
| `DropExperienceBlock`, `new AABB(BlockPos)`, `BlockEntityType.Builder.build(null)` e le API stabili elencate nell'audit | Nessuna |

## Schema NBT ([`NbtKeys`](../src/main/java/com/toxineren/exoframes/compat/NbtKeys.java))

`NbtKeys` è l'unica fonte delle chiavi. Gli item usano il tag radice `exoframes` (nel tag dello stack su 1.20.1, in `custom_data` su 1.21.1); lo stato del telaio per giocatore (`FrameStateCodec`) riusa le stesse chiavi e gli stessi tipi. Le conversioni comuni ai due codec stanno in due helper: [`SchemaIds`](../src/main/java/com/toxineren/exoframes/compat/impl/SchemaIds.java) (id di slot, livelli e moduli, lettura dell'UUID valida solo per un int array di 4 elementi, altrimenti assente, e avvisi una volta per chiave, con l'insieme delle chiavi limitato a 256 voci e gli id nel log troncati a 64 caratteri e senza caratteri di controllo) e [`PieceTags`](../src/main/java/com/toxineren/exoframes/compat/impl/PieceTags.java) (lettura di pezzo ed energia con gli intervalli garantiti descritti sopra per `ItemDataAccess`, lettura e scrittura dell'aspetto). Così i due codec leggono id, UUID, pezzi ed energia e leggono e scrivono l'aspetto allo stesso modo; il resto della scrittura e dello schema resta in ciascun codec (lo stato del telaio ha il riporto, la capacità della cella e i toggle). Lo stato del telaio ripristina l'aspetto dopo aver montato il pezzo, come il riporto. Lo stato del telaio ripristina un toggle acceso solo se il modulo è di tipo `TOGGLE` ed è installato in qualche slot.

| Chiave | Tipo | Dove |
|---|---|---|
| `exoframes` | compound | radice dei dati sugli item |
| `energy` | int | cella (item); dentro `cell` |
| `tier` | string (`Tier#id`) | pezzo; voce di `pieces` (anche dentro `visual`) |
| `durability`, `max_durability` | int | pezzo; voce di `pieces` |
| `modules` | lista di string (`ModuleType#id`) | pezzo; voce di `pieces` |
| `color_primary`, `color_secondary` | int RGB (ridotto a 24 bit in lettura; assente = `UNPAINTED`) | pezzo; voce di `pieces` (anche dentro `visual`) |
| `decals` | lista di compound con `pattern` e `color` (string non vuote, al massimo 64 caratteri), al massimo 4 voci; le voci malformate si saltano, le eccedenti si tagliano; assente = nessuna | pezzo; voce di `pieces` (anche dentro `visual`) |
| `owner` | UUID come int array | telaio (assente = nessun proprietario) |
| `unlocked`, `reinforced` | boolean | telaio |
| `cell` | compound con `energy` (e `capacity` nello stato) | telaio (assente = nessuna cella) |
| `pieces` | compound, una voce per `PieceSlot` in minuscolo | telaio; dentro `visual` |
| `visual` | compound con `pieces`: una voce per `PieceSlot` in minuscolo con `tier`, `condition`, `color_primary`, `color_secondary`, `decals` (stesse regole dei pezzi); assente o non compound = nessun riassunto | segnaposto del torso: riassunto visivo (`SuitSummary`) |
| `condition` | string, `PieceCondition` in minuscolo (`intact`, `damaged`, `critical`, `broken`); assente o sconosciuta = `intact` | voce di `pieces` dentro `visual` |
| `stowed` | lista di tag item | definita, non ancora usata (rinviata alla Milestone 3, vedi "Limiti noti e rinvii") |
| `capacity` | int | solo stato del telaio, dentro `cell` (Task 8) |
| `active` | lista di string (`ModuleType#id`) | solo stato del telaio: toggle accesi (Task 8) |
| `carry_milli` | int in [0, 999] | solo stato del telaio, voce di `pieces`: riporto del danno in millesimi (Task 8) |
| `schema` | int (assente = 1) | solo radice del dato salvato degli stati, accanto a `frames`; nessun marcatore sugli item |

Il `SavedData` salva gli stati sotto la chiave `frames`, una voce per UUID del giocatore in forma testuale, e accanto il marcatore `schema` (oggi 1). In lettura ogni voce è isolata: una voce che non si legge (valore non compound, chiave non UUID, eccezione del lettore) viene saltata con un avviso nel log che ne nomina la chiave, resta in memoria come tag grezzo e viene riscritta identica a ogni salvataggio; si toglie solo quando lo stato di quel giocatore viene creato o sostituito con `update()` o rimosso con `clear()`. Le altre voci si leggono normalmente, invece di far ricreare vuoto l'intero dato. Se `schema` è maggiore di quello noto (un mondo aperto con una versione della mod più vecchia di quella che l'ha salvato) o non è un numero, il dato è in sola lettura per questa versione: un solo ERROR al caricamento, nessuna voce viene letta, `get()` resta vuoto, e `update`, `mutate` e `clear` non fanno nulla (niente modifica, niente `setDirty`, niente sync; un solo ERROR che chiede di aggiornare la mod). Il file non viene mai riscritto da questa versione: nulla viene letto, cambiato o perso.

## Limiti noti e rinvii

Cose che la Milestone 2b lascia volutamente aperte; ciascuna ha la milestone che la chiude e la direzione prevista. Vanno copiate nel piano della milestone quando viene scritto.

- **Item riposti (`stowed`) — Milestone 3 (ingresso e uscita dal telaio).** L'armatura che il giocatore indossava prima di entrare viene stivata nello stato del telaio e rimessa addosso all'uscita. Direzione: gli stack stanno accanto al `FrameState` nella voce del giocatore nel `SavedData` (per esempio un piccolo record con `FrameState` e `List<ItemStack>`), salvati con `stack.save(new CompoundTag())` / `ItemStack.of` su 1.20.1 e con `stack.save(provider)` / `ItemStack.parseOptional(provider, tag)` su 1.21.1, usando il provider che `save` e il deserializzatore della `Factory` ricevono già; due metodi su `FrameStateAccess` per leggerli e scriverli; non vengono inviati al client. Su un mondo aggiornato da 1.20.1 servirà una migrazione esplicita degli stack (vanilla non li converte).
- **Ripristino dei moduli e config di chi legge — Milestone 3/4.** `FrameStateCodec.read` reinstalla i moduli con `FrameState.installModule`, che dipende dalla config di chi legge (capienza degli slot): con una config diversa un modulo può essere scartato. Direzione: un percorso di ripristino grezzo nel core (senza i controlli di installazione), insieme al caricamento della config.
- **Pezzo con livello sconosciuto — Milestone 3/4.** Un pezzo con un `tier` sconosciuto viene scartato in lettura e poi perso quando `setFrame` riscrive `pieces`. Direzione: fondere le voci ignote invece di sostituire l'intero `pieces`.
- **Capacità della cella solo nello stato — Milestone 3/4.** L'aspetto dei pezzi (colori e decalcomanie) sta ora in entrambi gli schemi (Milestone 3a: `Appearance` per slot in `FrameState`, chiave assente = `UNPAINTED`), ma gli item portano la cella senza `capacity` e lo stato del telaio porta `carry_milli` e `capacity`: i due schemi non si convertono ancora l'uno nell'altro senza perdita. Direzione: per la cella, capacità anche sull'item o presa sempre da `energy.cellCapacity`.
- **Sincronizzazione solo del proprio stato — Milestone 3.** Ogni client riceve solo il proprio stato, quindi `get()` e `clientView()` per un altro giocatore sono sempre vuoti sul client. Per disegnare i telai altrui serve scegliere tra una sincronizzazione al tracking (inizio del tracciamento dell'entità, eventi per loader) e dati visivi negli stack segnaposto.
- **Scritture a ogni tick — Milestone 3.** Il consumo di energia a ogni tick passa da `mutate` (segna il dato come da salvare, nessun pacchetto) e la sincronizzazione con `syncTo` a bassa frequenza (al massimo 4-5 volte al secondo), non da `update`, che invia lo stato intero a ogni chiamata.

## Regole di [`tools/check_sources.sh`](../tools/check_sources.sh)

Lo script gira dalla radice del repository (`bash tools/check_sources.sh`), stampa `OK sources` se tutto passa e altrimenti un `FAIL` per regola con i file coinvolti, uscendo con codice 1.

1. `core/` non importa `net.minecraft`, `net.fabricmc`, `net.minecraftforge`, `net.neoforged`, `dev.architectury` (anche con `import static`).
   - **1b.** `core/` non importa `com.google.gson`.
   - **1c.** Ogni tipo di `core/` è citato per nome in almeno un file di `src/test/java`.
2. Le direttive Stonecutter stanno solo in `compat/` e `platform/` di `src/main/java`, mai nei test. Si riconosce il marcatore, non la parola `if`: `grep -rlE '(//|/\*)[?$~]'` su `src/main/java` e `src/test/java` (quindi anche `//?if` senza spazio, condizioni senza `if`, direttive di scambio `//$` e di sostituzione `//~`, e le stesse in un commento a blocco).
3. Forge: `RedstoneExoframesForge` chiama `EventBuses.registerModEventBus` prima di `RedstoneExoframes.init()`.
4. Qualunque riferimento a `com.toxineren.exoframes.compat.impl.` (import, anche `import static`, o nome qualificato nel codice) compare solo in file sotto `compat/`, in `src/main/java` e `src/test/java` (cioè `src/(main|test)/java/com/toxineren/exoframes/compat/`).
5. Ogni file che chiama `registerC2S(` (tranne `Net` e `NetImpl`) referenzia `Compat.frameState()` in una riga che non è un commento: i gestori lato server validano le richieste sullo stato del telaio del giocatore. La regola guarda solo il file che registra, quindi la registrazione e i suoi gestori devono stare nello stesso file.
6. Regola finale, più stretta della 2, con lo stesso riconoscimento del marcatore: le direttive Stonecutter sono ammesse solo in `compat/impl/`, in `platform/` e in `compat/Ids.java`.
7. Il codice solo client (package `client`, `compat.client`, `compat.impl.client`) si referenzia (import, anche `import static`, o nome qualificato) solo da file di quei package e di `platform/`, in `src/main/java` e `src/test/java`: un server dedicato non deve mai caricarlo, e `RedstoneExoframes.init()` ne resta fuori.
