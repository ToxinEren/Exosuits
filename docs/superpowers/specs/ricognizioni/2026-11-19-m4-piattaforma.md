# M4, ricognizione: fatti di piattaforma sulle quattro varianti

Ricognizione per la Milestone 4 ("Stazione e HUD"): stazione di manutenzione 2x1 con GUI (riparazione, verniciatura con le tinte vanilla, montaggio dei pezzi sul telaio piazzato), anteprima 3D girevole della tuta, tre preset di colore per giocatore, HUD vero dentro la tuta, tasti nuovi. Nulla è stato cambiato nel repository (ramo `m4-stazione`); niente Gradle, niente gioco: solo `javap` e `unzip` sui jar già in cache e lettura dei sorgenti delle librerie.

Segni usati: **[javap]** letto nel bytecode; **[sorgente]** letto nei sorgenti delle librerie (Architectury, GeckoLib, moduli di Fabric API, classi di Forge e NeoForge); **[repo]** già nel codice o nei documenti del repository; **[deduzione]** ragionamento non provato a parte; **[non verificato]** detto a memoria o non controllato.

## Fonti

- **Minecraft con nomi Mojang** (cache di Loom, `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/`): **V20** `minecraft-merged` 1.20.1, **Fo20** `forge-1.20.1-47.4.29-minecraft-merged` (con le patch di Forge e le classi `net.minecraftforge.*`), **V21** `minecraft-merged` 1.21.1, **Neo21** `neoforge-21.1.266-minecraft-merged`. I `-sources.jar` di Fo20 e Neo21 contengono solo le classi del loader, non quelle di Minecraft: per Minecraft solo `javap -c -p`.
- **Architectury** 9.2.14 (Fabric e Forge) e 13.0.11 (Fabric e NeoForge), **GeckoLib** 4.8.4 (Fabric e Forge) e 4.9.3 (Fabric e NeoForge): jar rimappati con i sorgenti in `.gradle/loom-cache/remapped_mods/` del repository.
- **Fabric API** (stessa cartella): screen-handler-api-v1 1.3.34 / 1.3.91, key-binding-api-v1 1.0.38 / 1.0.47, rendering-v1 3.0.10 / 5.2.1, convention-tags-v1 1.5.6 (1.20.1), convention-tags-v2 2.12.0 (1.21.1), data-attachment-api-v1 1.0.2 / 1.4.7.
- **Jar universali** di Forge 47.4.29 e NeoForge 21.1.266 (`~/.gradle/caches/modules-2/`) per i file dei tag.
- I testi `javap` usati (Gui, BedBlock, Options, AbstractContainerMenu) sono accanto a questo file (`gui20.txt`, `gui21.txt`, `guifo20.txt`, `guineo21.txt`, `bed20.txt`, `bed21.txt`, `opt20.txt`, `opt21.txt`, `acm_*.txt`), con i sorgenti estratti in `x/`.
- Confronto fra il jar vanilla e quello del loader fatto solo per: `KeyMapping`, `MenuScreens`, le firme di `InventoryScreen`, `stillValid` di `AbstractContainerMenu`, l'elenco dei metodi di `BedBlock`, `DyeColor`/`DyeItem`, il costruttore di `Minecraft`, e su NeoForge `Gui.renderItemHotbar`/`renderPlayerHealth`. Il resto (`Gui` di Forge, `Screen`, `GuiGraphics`, `LevelChunk`, `DoorBlock`, `FriendlyByteBuf`, `SavedData`, il pacchetto dei `DataSlot`) è letto solo su V20 e V21: per Forge e NeoForge vale "come vanilla" solo dove il punto lo dice.

## Riassunto

| Punto | Uguale sulle quattro | Cambia | Da non perdere |
|---|---|---|---|
| 1. Menu da un blocco | `MenuRegistry` e `ExtendedMenuProvider` di Architectury identici in 9.2.14 e 13.0.11, sempre con `FriendlyByteBuf` | sotto: `NetworkHooks.openScreen` (Forge), `ServerPlayer.openMenu(provider, Consumer<RegistryFriendlyByteBuf>)` (NeoForge), `ExtendedScreenHandlerType` (Fabric; su 1.21.1 un `byte[]`) | su Fabric 1.21.1 il buffer è un `FriendlyByteBuf` semplice, su NeoForge un `RegistryFriendlyByteBuf`: niente `ItemStack.STREAM_CODEC` nei dati extra. Su NeoForge `registerScreenFactory` va chiamato durante la costruzione della mod (oggi `RedstoneExoframesClient.init()`): da `CLIENT_SETUP` è troppo tardi e fallisce in silenzio. I `DataSlot` viaggiano come `short` a 16 bit |
| 2. Schermata | `GuiGraphics` (salvo `blitSprite`), `AbstractContainerScreen`, `mouseDragged`, `Button.builder`, `AbstractWidget` | `renderBackground(GuiGraphics)` → `(GuiGraphics, int, int, float)`; `mouseScrolled` a 3 → 4 `double`; `ImageButton`; `setTooltipDelay(int)` → `(Duration)` | su 1.21.1 `Screen.render` chiama già lo sfondo; il `float` di `render` non è il partialTick su nessuna versione |
| 3. Anteprima 3D | i passi di `renderEntityInInventory`, `Lighting.setupForEntityInInventory()`, `GuiGraphics.bufferSource()`/`flush()` | firme di `renderEntityInInventory*` (solo `LivingEntity`, e il telaio non lo è); `mulPoseMatrix` → `mulPose(Matrix4f)` | la tuta si disegna con `client/SuitDraw` sulla posa della `GuiGraphics`, con una `SuitView` fatta sul client (anteprima dei colori senza andare al server); GeckoLib nell'anteprima vanilla è già provato sulle quattro, e 1.21.1 ritaglia al riquadro, 1.20.1 no |
| 4. Blocco 2x1 | schema del letto: `getStateForPlacement`, `setPlacedBy`, `updateShape`, tabella di bottino con `block_state_property`; `newBlockEntity` può dare `null` per una metà | `playerWillDestroy` `void` → `BlockState`; `isPathfindable` a 4 → 2 argomenti | serve una seconda cucitura come `use`/`useItemOn`; per la porta l'aiuto di `DoublePlantBlock` cambia nome |
| 5. Tasti | `KeyMappingRegistry.register(KeyMapping)` identico | sotto: Fabric registra subito (e lancia se le opzioni esistono già), Forge e NeoForge all'evento `RegisterKeyMappingsEvent` | V, R, G, N, H liberi in vanilla; su Forge/NeoForge `isDown()` passa dal contesto di conflitto (predefinito `UNIVERSAL`) |
| 6. HUD | `ClientGuiEvent.RENDER_HUD` dopo tutto l'HUD vanilla | `(GuiGraphics, float)` → `(GuiGraphics, DeltaTracker)`; su 1.21.1 strati a z distanti 200 | su 1.21.1 ciò che si disegna nel gancio sta a z = 0, sotto gli strati vanilla dove si sovrappone; posizioni vanilla uguali salvo l'indicatore di salvataggio |
| 7. Tinte | `DyeItem.getDyeColor()`, `DyeItem.byColor`, stessi RGB | `float[] getTextureDiffuseColors()` → `int getTextureDiffuseColor()` (ARGB opaco); `DyeableLeatherItem.dyeArmor` → `DyedItemColor.applyDyes`; tag diversi su ogni loader | i dati di 1.20.1 sono comuni a Forge e Fabric: un tag deve coprire `forge:` e `c:` con voci opzionali |
| 8. Preset per giocatore | `SavedData` (la via scelta in M2b) | firme di `save` e `computeIfAbsent` | i dati del loader (persistent data, attachment) non sono uniformi; Architectury non ha nulla. Dentro il registro dei telai una sezione nuova la perde il primo salvataggio di una versione vecchia, e alzare lo schema mette tutti i telai in sola lettura per le versioni vecchie: un `SavedData` a parte non ha né l'uno né l'altro effetto |

---

## 1. Menu con dati extra aperti da un blocco

### API comune (Architectury) [sorgente]

`dev.architectury.registry.menu.MenuRegistry` e `ExtendedMenuProvider` sono **identici** in 9.2.14 e 13.0.11 (diff vuoto dei due file):

```java
// MenuRegistry (9.2.14 = 13.0.11)
public static void openExtendedMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> bufWriter);
public static void openExtendedMenu(ServerPlayer player, ExtendedMenuProvider provider);          // @ExpectPlatform
public static void openMenu(ServerPlayer player, MenuProvider provider);                           // player.openMenu(provider)
public static <T extends AbstractContainerMenu> MenuType<T> ofExtended(ExtendedMenuTypeFactory<T> factory);
@Deprecated(forRemoval = true) public static <T extends AbstractContainerMenu> MenuType<T> of(SimpleMenuTypeFactory<T> factory);
@Environment(EnvType.CLIENT)
public static <H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>> void registerScreenFactory(MenuType<? extends H> type, ScreenFactory<H, S> factory);

interface ExtendedMenuTypeFactory<T extends AbstractContainerMenu> { T create(int id, Inventory inventory, FriendlyByteBuf buf); }
interface ScreenFactory<H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>> { S create(H containerMenu, Inventory inventory, Component component); }

// ExtendedMenuProvider (9.2.14 = 13.0.11)
public interface ExtendedMenuProvider extends MenuProvider { void saveExtraData(FriendlyByteBuf buf); }
```

Il tipo del menu si registra come gli altri registri della mod: `DeferredRegister.create(MOD_ID, Registries.MENU)` con `MenuRegistry.ofExtended(...)`. Il costruttore vanilla `MenuType(MenuType$MenuSupplier<T>, FeatureFlagSet)` è `private` in V20 e V21 [javap]: si passa da Architectury.

### Sotto, per piattaforma [sorgente]

| Variante | Apertura (server) | Tipo del menu | Buffer che riceve il costruttore del menu sul client |
|---|---|---|---|
| Fabric 1.20.1 (Arch 9.2.14, screen-handler 1.3.34) | `player.openMenu(new ExtendedScreenHandlerFactory() { writeScreenOpeningData(ServerPlayer, FriendlyByteBuf buf) { provider.saveExtraData(buf); } ... })` | `new ExtendedScreenHandlerType<>(factory::create)` | il `FriendlyByteBuf` del pacchetto, trattenuto (`retain`) e letto sul thread del client in `ClientNetworking.openScreen`, rilasciato dopo la creazione della schermata |
| Forge 1.20.1 (Arch 9.2.14, Forge 47.4.29) | `NetworkHooks.openScreen(player, provider, provider::saveExtraData)`: scrive in `new FriendlyByteBuf(Unpooled.buffer())`; **lancia `IllegalArgumentException` oltre 32600 byte** | `IForgeMenuType.create(factory::create)` = `new MenuType<>(IContainerFactory, FeatureFlags.DEFAULT_FLAGS)`, con `T create(int windowId, Inventory inv, FriendlyByteBuf data)` | `FriendlyByteBuf` |
| Fabric 1.21.1 (Arch 13.0.11, screen-handler 1.3.91) | `ExtendedScreenHandlerFactory<byte[]>.getScreenOpeningData(ServerPlayer)`: `PacketByteBufs.create()` (un `FriendlyByteBuf` **semplice**), `saveExtraData`, poi `ByteBufUtil.getBytes` e `release()` | `new ExtendedScreenHandlerType<>((syncId, inventory, data) -> { FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data)); T menu = factory.create(syncId, inventory, buf); buf.release(); return menu; }, ByteBufCodecs.BYTE_ARRAY.mapStream(Function.identity()))` | `FriendlyByteBuf` semplice sopra un `byte[]`, **rilasciato subito dopo il costruttore** |
| NeoForge 1.21.1 (Arch 13.0.11, NeoForge 21.1.266) | `player.openMenu(provider, provider::saveExtraData)` → `IPlayerExtension.openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)` (massimo 32600 byte secondo il javadoc) | `IMenuTypeExtension.create(factory::create)` = `new MenuType<>(IContainerFactory, FeatureFlags.DEFAULT_FLAGS)`, con `T create(int windowId, Inventory inv, RegistryFriendlyByteBuf data)` | `RegistryFriendlyByteBuf` |

**Per M4.**
- Su 1.21.1 il buffer dei dati extra è un `RegistryFriendlyByteBuf` su NeoForge e un `FriendlyByteBuf` semplice su Fabric [sorgente]: nel codice comune niente `ItemStack.STREAM_CODEC`, `ComponentSerialization.STREAM_CODEC` né cast a `RegistryFriendlyByteBuf` (su Fabric 1.21.1 il cast lancerebbe) [deduzione]. Il costruttore del menu legge tutto subito e non tiene il buffer (Fabric 1.21.1 lo rilascia appena torna).
- Metodi di `FriendlyByteBuf` con la stessa forma sorgente sulle due versioni [javap]: `writeBlockPos`/`readBlockPos`, `writeUUID`/`readUUID`, `writeVarInt`/`readVarInt`, `writeUtf`/`readUtf`, `writeEnum`/`readEnum`, `writeResourceLocation`/`readResourceLocation`, `writeBitSet`, `writeNbt` (parametro `CompoundTag` su 1.20.1, `Tag` su 1.21.1: una chiamata con un `CompoundTag` compila su tutte e due) e `readNbt()` → `CompoundTag`; `writeCollection`/`readCollection`, `writeOptional`, `writeMap` con lambda (tipi `FriendlyByteBuf$Writer`/`$Reader` su 1.20.1, `StreamEncoder`/`StreamDecoder` su 1.21.1: la stessa lambda compila). Solo su 1.20.1: `writeItem`/`readItem`, `writeComponent`/`readComponent`.
- Lo stato del telaio piazzato non arriva mai al client (ogni client riceve solo il proprio stato, [repo] `docs/compat.md`, "Sincronizzazione solo del proprio stato"): la stazione lo deve mandare all'apertura, e la via comune ai quattro buffer è `writeNbt`/`readNbt` di un `CompoundTag` (lo schema di `FrameStateCodec` già esiste) o un pacchetto di `Net` [deduzione].

### Registrazione della schermata sul client: quando

- Fabric (9.2.14 e 13.0.11): `MenuScreens.register(type, factory::create)` subito [sorgente] (in vanilla è `private`, Architectury lo allarga).
- Forge 9.2.14: `MenuScreens.register` subito [sorgente]; su Fo20 il metodo è `public` [javap]. Chiamato dal costruttore della mod (oggi `RedstoneExoframesClient.init()` dentro `EnvExecutor.runInEnv`) scrive in `SCREENS`, una mappa vanilla non sincronizzata, durante la fase di costruzione di Forge, che è parallela [non verificato: nessun problema osservato finora con le altre registrazioni client fatte allo stesso punto].
- NeoForge 13.0.11: `EventBusesHooks.whenAvailable(ArchitecturyConstants.MOD_ID, bus -> bus.addListener(RegisterMenuScreensEvent.class, event -> event.register(type, factory::create)))` [sorgente]. L'evento lo posta `MenuScreens.init()` [javap Neo21], chiamato da `ClientHooks.initClientHooks` [sorgente], che il costruttore di `Minecraft` chiama all'offset 2186, dopo `ClientModLoader.begin` (costruzione delle mod, offset 945) e prima del primo `createReload` (offset 2440) [javap]; `FMLClientSetupEvent`, e quindi `ClientLifecycleEvent.CLIENT_SETUP` di Architectury, gira dentro quel ricaricamento (`ClientModLoader.onResourceReload` → `load`) [sorgente]. **Quindi su NeoForge `registerScreenFactory` va chiamato durante la costruzione della mod: da `CLIENT_SETUP` l'ascoltatore arriverebbe dopo l'evento e la schermata non si registrerebbe mai, senza errori** [deduzione dall'ordine, non osservata in gioco]. La stessa chiamata dal costruttore va bene anche su Fabric e Forge.

### Il menu (vanilla, identico) [javap]

`AbstractContainerMenu` ha la stessa superficie su V20 e V21: `public abstract ItemStack quickMoveStack(Player, int)`, `public abstract boolean stillValid(Player)`, `protected static boolean stillValid(ContainerLevelAccess, Player, Block)`, `protected Slot addSlot(Slot)`, `protected DataSlot addDataSlot(DataSlot)`, `protected void addDataSlots(ContainerData)`, `public boolean clickMenuButton(Player, int)`, `public void removed(Player)`, `public void slotsChanged(Container)`, `public void setData(int, int)`, `public void broadcastChanges()`.

- **Pulsanti senza pacchetti propri:** sul client `MultiPlayerGameMode.handleInventoryButtonClick(int containerId, int buttonId)` (uguale su V20 e V21) arriva al server come `clickMenuButton(Player, int)` del menu aperto [javap delle firme; il percorso del pacchetto vanilla non riletto]. Un intero per azione (ripara, vernicia, preset 1-3...).
- **`DataSlot` a 16 bit:** `ClientboundContainerSetDataPacket` scrive e legge il valore con `writeShort`/`readShort` su V20 e V21 [javap]. Non ci stanno né l'energia (fino a 40 000 RE) né un colore a 24 bit: colori e stato del telaio viaggiano nei dati extra o in un pacchetto, o si spezzano in più `DataSlot` [deduzione].
- **`stillValid` statico:** controlla che alla posizione del `ContainerLevelAccess` ci sia il blocco (`BlockState.is(Block)`) e la distanza: V20 `player.distanceToSqr(centro del blocco) <= 64.0`; Fo20 la stessa forma con l'attributo di portata di Forge (`RegistryObject.get` → `getAttributeValue`, più 3,5; formula esatta non ricostruita); V21 e Neo21 `player.canInteractWithBlock(pos, 4.0)` [javap]. Per il blocco 2x1 basta la posizione della metà con la block entity: le due metà sono lo stesso blocco.

---

## 2. `AbstractContainerScreen` e `GuiGraphics`

### `GuiGraphics` [javap]

Uguale su V20 e V21, salvo i `blitSprite` che esistono solo su 1.21.1 (sprite dell'atlante della GUI). Metodi comuni utili:

```java
public void blit(net.minecraft.resources.ResourceLocation, int x, int y, int u, int v, int width, int height);                      // texture 256x256
public void blit(net.minecraft.resources.ResourceLocation, int x, int y, float u, float v, int width, int height, int texWidth, int texHeight);
public void blit(net.minecraft.resources.ResourceLocation, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, int texWidth, int texHeight);
public void fill(int, int, int, int, int);  public void fillGradient(int, int, int, int, int, int);  public void renderOutline(int, int, int, int, int);
public int drawString(net.minecraft.client.gui.Font, net.minecraft.network.chat.Component, int, int, int, boolean);
public void drawCenteredString(net.minecraft.client.gui.Font, net.minecraft.network.chat.Component, int, int, int);
public void renderTooltip(net.minecraft.client.gui.Font, net.minecraft.network.chat.Component, int, int);
public void renderComponentTooltip(net.minecraft.client.gui.Font, java.util.List<net.minecraft.network.chat.Component>, int, int);
public void renderTooltip(net.minecraft.client.gui.Font, net.minecraft.world.item.ItemStack, int, int);
public void renderItem(net.minecraft.world.item.ItemStack, int, int);  public void renderItemDecorations(net.minecraft.client.gui.Font, net.minecraft.world.item.ItemStack, int, int);
public void enableScissor(int, int, int, int);  public void disableScissor();  public void setColor(float, float, float, float);
public com.mojang.blaze3d.vertex.PoseStack pose();  public net.minecraft.client.renderer.MultiBufferSource$BufferSource bufferSource();  public void flush();
public int guiWidth();  public int guiHeight();
// solo 1.21.1:
public void blitSprite(net.minecraft.resources.ResourceLocation, int, int, int, int);   // e altre tre forme
```

Una texture propria della stazione disegnata con `blit(ResourceLocation, ...)` vale per le due versioni senza condizioni.

### `Screen` e `AbstractContainerScreen` [javap]

| | 1.20.1 | 1.21.1 |
|---|---|---|
| sfondo | `public void renderBackground(net.minecraft.client.gui.GuiGraphics)` | `public void renderBackground(net.minecraft.client.gui.GuiGraphics, int, int, float)`; in più `renderTransparentBackground(GuiGraphics)`, `renderBlurredBackground(float)`, `renderMenuBackground(...)` |
| chi lo chiama | nessuno: la sottoclasse lo chiama all'inizio del proprio `render` | `Screen.render` lo chiama per primo; in `AbstractContainerScreen` è ridefinito come `renderTransparentBackground` + `renderBg` |
| `AbstractContainerScreen.render` | `renderBg`, poi `Screen.render` (widget), poi slot, oggetto sotto il cursore | `Screen.render` (sfondo + `renderBg` + widget), poi slot |
| rotella | `mouseScrolled(double, double, double)` | `mouseScrolled(double, double, double, double)` (orizzontale e verticale) |
| ritardo del suggerimento | `AbstractWidget.setTooltipDelay(int)` | `setTooltipDelay(java.time.Duration)` |
| `ImageButton` | `ImageButton(int, int, int, int, int, int, ResourceLocation, Button$OnPress)` e varianti con u/v/texture | `ImageButton(int, int, int, int, WidgetSprites, Button$OnPress)` e varianti |

Uguali [javap]: `protected abstract void renderBg(GuiGraphics, float, int, int)`, `protected void renderLabels(GuiGraphics, int, int)`, `protected void renderTooltip(GuiGraphics, int, int)` (l'oggetto sotto il cursore), i campi `imageWidth`, `imageHeight`, `leftPos`, `topPos`, `titleLabelX/Y`, `inventoryLabelX/Y`, `hoveredSlot`; `init()`, `containerTick()`, `removed()`, `onClose()`, `keyPressed(int, int, int)`, `mouseClicked(double, double, int)`, `mouseReleased(double, double, int)`, `mouseDragged(double, double, int, double, double)` (gli ultimi due sono lo spostamento dall'evento precedente); `addRenderableWidget`, `addWidget`; `Button.builder(Component, Button$OnPress)`; `AbstractWidget(int, int, int, int, Component)` con `protected abstract void renderWidget(GuiGraphics, int, int, float)` e `protected abstract void updateWidgetNarration(NarrationElementOutput)`; `onClick`, `onDrag`, `onRelease`; `setTooltip(Tooltip)`, `Tooltip.create(Component)`; `AbstractSliderButton(int, int, int, int, Component, double)` con `updateMessage()` e `applyValue()` astratti.

**Il `float` di `render` non è il partialTick** [javap `GameRenderer.render`]: su 1.20.1 `Screen.renderWithTooltip` riceve `Minecraft.getDeltaFrameTime()`, su 1.21.1 `DeltaTracker.getRealtimeDeltaTicks()`. Il partialTick vero: 1.20.1 `Minecraft.getFrameTime()`, 1.21.1 `Minecraft.getTimer().getGameTimeDeltaPartialTick(true)` (è quello che usa GeckoLib 4.9.3 [sorgente]). Per una tuta ferma in posa di riposo basta `0f` [deduzione].

**Per M4.**
- Una base di schermata in `compat/impl/client` (la "`CompatContainerScreen`" rinviata dalla tabella di `docs/compat.md`) con un `render` neutro: su 1.20.1 chiama `renderBackground(g)`, su 1.21.1 **non** lo chiama (lo fa già `Screen.render`: chiamarlo di nuovo ridisegna lo sfondo, scurendo due volte) [deduzione]; più la rotella, se serve lo zoom.
- Pulsanti e caselle propri come sottoclassi di `AbstractWidget`/`AbstractButton` disegnate con `blit(ResourceLocation, ...)`: niente `ImageButton`, che cambia forma.
- Trascinamento per l'anteprima: `mouseDragged` uguale sulle due; `AbstractContainerScreen` lo usa per il trascinamento degli oggetti negli slot, quindi la sottoclasse lo prende solo dentro il riquadro dell'anteprima e altrimenti chiama `super` [deduzione].

---

## 3. Disegnare un'entità o un modello nella schermata

### Firme [javap]

| | 1.20.1 (V20) | Forge 1.20.1 aggiunge | 1.21.1 (V21) | NeoForge 21.1 aggiunge |
|---|---|---|---|---|
| segue il mouse | `public static void renderEntityInInventoryFollowsMouse(net.minecraft.client.gui.GuiGraphics, int, int, int, float, float, net.minecraft.world.entity.LivingEntity)` | `renderEntityInInventoryFollowsAngle(GuiGraphics, int, int, int, float, float, LivingEntity)` | `public static void renderEntityInInventoryFollowsMouse(net.minecraft.client.gui.GuiGraphics, int, int, int, int, int, float, float, float, net.minecraft.world.entity.LivingEntity)` (x1, y1, x2, y2, scala, yOffset, mouseX, mouseY) | `renderEntityInInventoryFollowsAngle(GuiGraphics, int, int, int, int, int, float, float, float, LivingEntity)` |
| base | `public static void renderEntityInInventory(net.minecraft.client.gui.GuiGraphics, int, int, int, org.joml.Quaternionf, org.joml.Quaternionf, net.minecraft.world.entity.LivingEntity)` | — | `public static void renderEntityInInventory(net.minecraft.client.gui.GuiGraphics, float, float, float, org.joml.Vector3f, org.joml.Quaternionf, org.joml.Quaternionf, net.minecraft.world.entity.LivingEntity)` | — |

Tutte vogliono un `LivingEntity`: l'entità del telaio piazzato (`entity/ExoframeEntity`) non lo è [repo], quindi non si passa a queste.

### Che cosa fa `renderEntityInInventory` (da copiare) [javap]

Su V20 e V21, nell'ordine: `pose().pushPose()`; `translate(x, y, 50.0)`; scala `(s, s, -s)` (V20 `mulPoseMatrix(new Matrix4f().scaling(s, s, -s))`, V21 `scale(s, s, -s)` più `translate` del vettore); `mulPose(rotazione)`; `Lighting.setupForEntityInInventory()`; `EntityRenderDispatcher.overrideCameraOrientation(...)` (coniugata; su V21 anche `rotateY(π)`); `setRenderShadow(false)`; `RenderSystem.runAsFancy(() -> dispatcher.render(entità, 0, 0, 0, 0f, 1f, pose, graphics.bufferSource(), 15728880))` (luce piena, `0xF000F0`; posizione 0, yaw 0, partialTick 1); `graphics.flush()`; `setRenderShadow(true)`; `popPose()`; `Lighting.setupFor3DItems()`.

Firme vicine [javap]: `PoseStack.mulPoseMatrix(Matrix4f)` su 1.20.1 → `PoseStack.mulPose(Matrix4f)` su 1.21.1 (`mulPose(Quaternionf)`, `scale`, `translate`, `rotateAround` uguali); `Lighting.setupForEntityInInventory()` sulle due, più `setupForEntityInInventory(Quaternionf)` su 1.21.1; `EntityRenderDispatcher.render(E extends Entity, double, double, double, float, float, PoseStack, MultiBufferSource, int)` uguale (accetta qualunque `Entity`); `Minecraft.getEntityModels()` sulle due; `com.mojang.math.Axis` presente sulle due.

### Come disegnare la tuta

- **Con `client/SuitDraw` [deduzione sul codice del repo]:** `SuitDraw.draw(entità, modello di base, pose, buffer, luce, partialTick, vista, undoRaisePx)` prende qualunque `PoseStack` e qualunque `MultiBufferSource` e copia la posa in uno stack privato. Nella schermata: i passi qui sopra, con `graphics.pose()` e `graphics.bufferSource()`, poi le trasformazioni che `client/ExoframeEntityRenderer` già applica per lo spazio del modello del giocatore (`scale(-1, -1, 1)`, `scale(0.9375f)`, `translate(0, -SuitDraw.FEET_Y, 0)`), una rotazione `Axis.YP.rotationDegrees(yaw)` presa dal trascinamento, e al posto di `dispatcher.render` la chiamata a `SuitDraw.draw(..., 0f)` come undoRaise; poi `graphics.flush()`. Il modello di base: un `HumanoidModel` cotto da `Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER)` con `young = false`, come fa `ExoframeEntityRenderer`.
- **Entità da passare:** il contratto di `SuitRenderer` vuole un `Entity` qualunque che non sia un animabile di GeckoLib, usato per identità e bagliore [repo, `docs/compat.md` "SuitRenderer"]. Vanno bene l'`ExoframeEntity` del client (se la stazione ne conosce l'id) o il giocatore locale [deduzione].
- **Vista:** `SuitView` si costruisce sul client (`SuitView.of(riassunto, ...)` o direttamente), quindi l'anteprima mostra i colori scelti prima di applicarli, senza andare al server [repo + deduzione].
- **In alternativa** `dispatcher.render(exoframeEntity, ...)` passa da `ExoframeEntityRenderer`, ma disegna il riassunto sincronizzato, non i colori in prova [deduzione].
- **GeckoLib:** `GeoArmorRenderer.getRenderType` dà `RenderType.armorCutoutNoCull(texture)` su 4.8.4 e 4.9.3 [sorgente]. La tuta indossata nell'anteprima dell'inventario vanilla (cioè GeckoLib sotto `Lighting.setupForEntityInInventory()`, passando da `SuitLayer`) è già provata nel client vero sulle quattro varianti: controllo `inventario` di `tools/client/m3d_shots.sh`, "nell'anteprima dell'inventario la tuta intera" [repo, `docs/test-in-gioco/m3d-braccio.md`]. Lì si è misurata anche una differenza: **1.21.1 ritaglia la tuta nel riquadro dell'anteprima, 1.20.1 no** (su 1.20.1 se ne vede circa 1,75 volte tanto) [repo]: il `renderEntityInInventoryFollowsMouse` di V21 chiama `enableScissor`/`disableScissor` sui due angoli che riceve, l'`InventoryScreen` di V20 non le chiama mai [javap]. L'anteprima della stazione non passa da quel metodo: se vuole il ritaglio lo fa da sé, sulle due versioni [deduzione]. Non provata resta la chiamata diretta di `SuitDraw.draw` da una schermata.
- **Grandezza:** la tuta è alta circa `SuitScale.S` (1,65) × 1,8 × 0,9375 ≈ 2,8 blocchi; con `s` pixel di GUI per blocco viene alta circa 2,8·s: per un riquadro di 70 px, `s` ≈ 25 [deduzione aritmetica].
- **Ritaglio:** `enableScissor` taglia i disegni scaricati prima di `disableScissor`, quindi `graphics.flush()` prima di toglierlo [deduzione].

---

## 4. Un blocco 2x1 fatto di due metà

### Lo schema del letto [javap `BedBlock` su V21]

Proprietà `PART` (`EnumProperty<BedPart>`, `HEAD`/`FOOT`) e `FACING` orizzontale. Le sequenze di chiamate di `getStateForPlacement`, `setPlacedBy`, `playerWillDestroy` e `updateShape` sono identiche in V20 e V21, e Fo20 e Neo21 non cambiano l'elenco dei metodi di `BedBlock` [javap, confronto dei due testi].

- **Piazzamento:** `getStateForPlacement(BlockPlaceContext)`: l'altra posizione è `getClickedPos().relative(getHorizontalDirection())`; si piazza solo se `level.getBlockState(altra).canBeReplaced(context)` e `level.getWorldBorder().isWithinBounds(altra)`, altrimenti `null`. Poi `setPlacedBy(Level, BlockPos, BlockState, LivingEntity, ItemStack)` (firma uguale sulle due), sul server: `level.setBlock(altra, state.setValue(PART, HEAD), 3)`, `level.blockUpdated(pos, Blocks.AIR)`, `state.updateNeighbourShapes(level, pos, 3)`.
- **Rottura di una metà:** `updateShape(BlockState, Direction, BlockState, LevelAccessor, BlockPos, BlockPos)`: se la direzione è quella dell'altra metà e lì non c'è più l'altra metà dello stesso blocco, restituisce `Blocks.AIR.defaultBlockState()`.
- **Creativa:** `playerWillDestroy`: sul server, se il giocatore è in creativa e rompe il `FOOT`, toglie la `HEAD` con `level.setBlock(altra, AIR, 35)` (35 = 32 + 2 + 1: senza oggetti) e `level.levelEvent(player, 2001, altra, Block.getId(stato))`; poi `super`.
- **Bottino:** la metà che resta sola sparisce passando da `updateShape` → aria, cioè con i suoi oggetti, salvo il segno 32 [deduzione, `Block.updateOrDestroy` non riletto]; per questo la tabella del letto fa cadere l'oggetto solo dalla metà `head`, ed è identica sulle due versioni salvo la cartella (`loot_tables/blocks/` su 1.20.1, `loot_table/blocks/` su 1.21.1) [unzip]:

```json
{"type":"minecraft:block","pools":[{"bonus_rolls":0.0,"conditions":[{"condition":"minecraft:survives_explosion"}],"entries":[{"type":"minecraft:item","conditions":[{"block":"minecraft:red_bed","condition":"minecraft:block_state_property","properties":{"part":"head"}}],"name":"minecraft:red_bed"}],"rolls":1.0}],"random_sequence":"minecraft:blocks/red_bed"}
```

### La porta (in verticale) [javap `DoorBlock` e `DoublePlantBlock`, V20 e V21]

`DoorBlock.HALF` (`EnumProperty<DoubleBlockHalf>`, `UPPER`/`LOWER`) con le stesse firme del letto per `updateShape`, `getStateForPlacement`, `setPlacedBy`, `canSurvive` (`protected` su 1.21.1) e lo stesso cambio di `playerWillDestroy`. L'aiuto statico per la rottura senza oggetti **cambia nome**: 1.20.1 `protected static void preventCreativeDropFromBottomPart(Level, BlockPos, BlockState, Player)`, chiamato se `player.isCreative()`; 1.21.1 `protected static void preventDropFromBottomPart(Level, BlockPos, BlockState, Player)`, chiamato dopo `isCreative()` e `hasCorrectToolForDrops(...)` (cioè anche senza l'attrezzo giusto [deduzione sulla condizione esatta]). È `protected` in `DoublePlantBlock`: per la stazione si riscrive, non si chiama.

### Firme che cambiano [javap `BedBlock`, V20 e V21]

| Metodo | 1.20.1 | 1.21.1 |
|---|---|---|
| `playerWillDestroy` | `public void playerWillDestroy(Level, BlockPos, BlockState, Player)` | `public BlockState playerWillDestroy(Level, BlockPos, BlockState, Player)` |
| `isPathfindable` | `public boolean isPathfindable(BlockState, BlockGetter, BlockPos, PathComputationType)` | `protected boolean isPathfindable(BlockState, PathComputationType)` |
| `updateShape` | `public BlockState updateShape(BlockState, Direction, BlockState, LevelAccessor, BlockPos, BlockPos)` | stessa firma, `protected` |
| `getRenderShape` | `public` | `protected` |
| `codec()` | — | `public MapCodec<BedBlock> codec()` |

`getStateForPlacement`, `setPlacedBy`, `newBlockEntity` hanno la stessa firma.

### Block entity su una metà sola [javap]

`LevelChunk.setBlockState` chiama `EntityBlock.newBlockEntity(pos, state)` e controlla `ifnull` prima di aggiungerla, su V20 e V21: un `newBlockEntity` che dà `null` per la metà senza dati è ammesso. Il letto invece mette una block entity su tutte e due le metà.

### Spinta dei pistoni [javap]

`BlockBehaviour.Properties.pushReaction(PushReaction)` esiste sulle due versioni (il letto usa `DESTROY`): senza, un pistone può separare le metà [deduzione].

**Per M4.**
- Una seconda cucitura come quella del clic (`compat/impl/EntityBlockBase`, [repo]): un metodo neutro per `playerWillDestroy` (e per `isPathfindable`, se si ridefinisce). Gli altri override si dichiarano `public` nel codice comune come già si fa per `rotate`, `mirror`, `onRemove` [repo].
- La tabella di bottino della stazione con la condizione `block_state_property` sulla metà che porta l'oggetto, scritta da `tools/gen_data.py` nei due alberi per versione come le altre [repo].
- La block entity (e `onRemove` con `!state.is(newState.getBlock())`, come `content/ChargerBlock` [repo]) solo sulla metà principale; `stillValid` del menu sulla sua posizione.

---

## 5. Tasti

### `KeyMappingRegistry` di Architectury [sorgente]

`dev.architectury.registry.client.keymappings.KeyMappingRegistry.register(net.minecraft.client.KeyMapping)` ha la stessa forma in 9.2.14 e 13.0.11 (diff vuoto). Sotto:

| Variante | Che cosa fa |
|---|---|
| Fabric (key-binding-api 1.0.38 e 1.0.47, codice uguale) | `KeyBindingHelper.registerKeyBinding(mapping)`: **lancia `IllegalStateException` se `Minecraft.getInstance().options != null`** (opzioni già create), lancia per lo stesso oggetto o lo stesso nome due volte, e aggiunge da sé la categoria all'ordine delle categorie |
| Forge 9.2.14 | mette il tasto in una lista e lo registra a `RegisterKeyMappingsEvent` (bus del mod); dopo l'evento lo aggiunge a `options.keyMappings` con un WARN |
| NeoForge 13.0.11 | lo stesso, con l'ascoltatore sul bus di Architectury; l'evento parte da `ClientHooks.initClientHooks` (`onRegisterKeyMappings`) durante il costruttore di `Minecraft` |

Quindi, come per le schermate, la registrazione va fatta dall'init client chiamato durante la costruzione della mod, dove la fa già il repository. I tasti nuovi possono riusare la categoria che c'è (`key.categories.redstone_exoframes`), già funzionante sulle quattro varianti [repo]; una seconda categoria su Forge e NeoForge non l'ho controllata.

### Come il repository registra V e R oggi [repo]

`client/RedstoneExoframesClient.init()` chiama `ExitKey.register()` e `CellSwapKey.register()`. Ognuno ha `new KeyMapping(NAME, GLFW.GLFW_KEY_V | GLFW.GLFW_KEY_R, "key.categories.redstone_exoframes")` (nomi `key.redstone_exoframes.exit` e `key.redstone_exoframes.cell_swap`, già in `it_it.json` ed `en_us.json` con la categoria), `KeyMappingRegistry.register(KEY)`, e un ascoltatore `ClientTickEvent.CLIENT_POST` che legge `KEY.isDown()` a ogni tick e passa a `client/ExitHold` (conteggio del tasto tenuto, senza schermate aperte e con la tuta disegnata); `CellSwapKey` non conta mentre `ExitKey.keyDown()` è vero.

### Tenuto o premuto [javap]

`KeyMapping` ha su tutte e quattro `public boolean isDown()` (stato: tenuto), `public boolean consumeClick()` (conta le pressioni, ogni chiamata ne consuma una), `setDown(boolean)`, `static click(InputConstants$Key)`, `static set(InputConstants$Key, boolean)`, `static releaseAll()`, `static setAll()`, i costruttori `KeyMapping(String, int, String)` e `KeyMapping(String, InputConstants$Type, int, String)`.

- Forge e NeoForge cambiano `isDown()`: `isDown && isConflictContextAndModifierActive()`. Il costruttore vanilla imposta `KeyConflictContext.UNIVERSAL` e `KeyModifier.NONE`. I costruttori con `IKeyConflictContext` (`IN_GAME`, `GUI`, `UNIVERSAL`) e `KeyModifier` esistono solo su Forge e NeoForge: il codice comune resta sul costruttore vanilla, come oggi.
- Un tasto premuto e lasciato (il menu radiale `G`, il visore `N`, `H` per nascondere l'HUD) si legge con `while (KEY.consumeClick())` nel tick del client; un tasto tenuto con `isDown()`, come V e R [deduzione]. Che cosa succede ai due con una schermata aperta (gli eventi della tastiera vanno alla schermata) non l'ho riletto nel bytecode [non verificato].

### Tasti predefiniti di vanilla [javap, costruttore di `Options`, uguali su V20 e V21]

W, A, S, D (87, 65, 83, 68), spazio (32), Maiusc sinistro (340), Ctrl sinistro (341), E (69), F (70), Q (81), T (84), Tab (258), / (47), P (80), F2 (291), F5 (294), F11 (300), L (76), 1-9 (49-57), C (67), X (88), tasti del mouse; `smoothCamera` e `spectatorOutlines` senza tasto. **V (86), R (82), G (71), N (78) e H (72) sono liberi** sulle due versioni. Non verificato: se le combinazioni di debug F3+G, F3+H, F3+N fanno scattare anche un tasto della mod.

---

## 6. Gancio dell'HUD e disposizione vanilla

### `ClientGuiEvent.RENDER_HUD` [sorgente]

| | Firma | Da dove parte |
|---|---|---|
| Architectury 9.2.14 | `void renderHud(net.minecraft.client.gui.GuiGraphics graphics, float tickDelta)` | Fabric: `HudRenderCallback` (rendering-v1 3.0.10, mixin su `Gui.render` a `TAIL`, dopo la lista dei giocatori); Forge: `RenderGuiEvent.Post`, priorità `HIGH`, con `event.getPartialTick()` (`public float getPartialTick()` [sorgente]) |
| Architectury 13.0.11 | `void renderHud(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker)` | Fabric: `HudRenderCallback` (rendering-v1 5.2.1, `TAIL` di `Gui.render`); NeoForge: `RenderGuiEvent.Post`, postato da `GuiLayerManager.render` dopo tutti gli strati |

Il repository lo usa già da `compat/impl/client/ClientRenderHooksImpl.addHudRenderer` con una lambda che ignora il secondo argomento [repo]. `DeltaTracker` (1.21.1) [javap]: `float getGameTimeDeltaTicks()`, `float getGameTimeDeltaPartialTick(boolean)`, `float getRealtimeDeltaTicks()`; sul client `Minecraft.getTimer()` lo restituisce.

Alternative per strati con una posizione (non comuni, quindi per loader): Forge 1.20.1 `RegisterGuiOverlaysEvent` con `VanillaGuiOverlay` e i campi `ForgeGui.leftHeight`/`rightHeight` (partono da 39 e crescono di 10 a ogni riga di cuori, armatura, cibo, aria) [sorgente]; NeoForge 21.1 `RegisterGuiLayersEvent` con `VanillaGuiLayers` (`CAMERA_OVERLAYS`, `CROSSHAIR`, `HOTBAR`, `JUMP_METER`, `EXPERIENCE_BAR`, `PLAYER_HEALTH`, `ARMOR_LEVEL`, `FOOD_LEVEL`, `VEHICLE_HEALTH`, `AIR_LEVEL`, `SELECTED_ITEM_NAME`, `SPECTATOR_TOOLTIP`, `EXPERIENCE_LEVEL`, `EFFECTS`, `BOSS_OVERLAY`, `SLEEP_OVERLAY`, `DEMO_OVERLAY`, `DEBUG_OVERLAY`, `SCOREBOARD_SIDEBAR`, `OVERLAY_MESSAGE`, `TITLE`, `CHAT`, `TAB_LIST`, `SUBTITLE_OVERLAY`, `SAVING_INDICATOR`) e i campi pubblici `Gui.leftHeight`/`rightHeight` [sorgente, javap]; Fabric 1.21.1 con rendering-v1 5.2.1 ha solo `HudRenderCallback` [sorgente]. Su Forge 1.20.1 la barra delle azioni sale con le pile: `y = altezza - max(max(leftHeight, rightHeight) + 9, 68)` [sorgente `ForgeGui`].

### Profondità su 1.21.1 [javap]

`LayeredDraw.render` fa `pushPose`, disegna ogni strato e dopo ciascuno `translate(0, 0, 200)` (`Z_SEPARATION`), poi `popPose`; `GuiLayerManager` di NeoForge fa lo stesso (`Z_SEPARATION = LayeredDraw.Z_SEPARATION`) e posta `RenderGuiEvent.Post` dopo il `popPose`. Il gancio quindi disegna a z = 0, dietro tutti gli strati vanilla. Su 1.20.1 la barra degli oggetti si disegna a z = −90, il resto a z = 0. **Conseguenza [deduzione, coerente con il repo, dove la schermata di F3 copre la riga dell'energia]:** su 1.21.1, dove un elemento nuovo si sovrappone a uno strato vanilla vince lo strato vanilla, a meno di alzare la z (`graphics.pose().translate(0, 0, ...)`) nel disegno della mod. La via semplice è non sovrapporsi.

### Disposizione vanilla a scala 2 su una finestra 960x540

Con la scala 2 l'interfaccia è 480 x 270 pixel di GUI (960/2, 540/2); anche la scala automatica dà 2, perché 540/3 = 180 è sotto il minimo di 240 [deduzione dal calcolo di `Window`, non riletto]. Centro orizzontale x = 240. Formule lette in `Gui` di V20 e V21 [javap]; dove non è detto altro sono uguali sulle due versioni (su 1.21.1 con `guiWidth()`/`guiHeight()` e sprite al posto della texture `icons`).

| Elemento | Formula | A 480 x 270 |
|---|---|---|
| barra degli oggetti | x = w/2 − 91, y = h − 22, 182 x 22 (z −90) | x 149-331, y 248-270 |
| oggetto selezionato | x = w/2 − 92 + 20·slot, y = h − 23; 24 x 22 (1.20.1), 24 x 23 (1.21.1) | da x 148 |
| seconda mano (destrorso) | x = w/2 − 91 − 29, y = h − 23, 29 x 24 | x 120-149, y 247-271 |
| seconda mano (mancino) | x = w/2 + 91, y = h − 23 | x 331-360 |
| barra dell'esperienza (o del salto in sella) | x = w/2 − 91, y = h − 32 + 3, 182 x 5 | x 149-331, y 241-246 |
| numero del livello | centrato, y = h − 31 − 4 | y 235 |
| cuori | x = w/2 − 91 + 8·i, y = h − 39 (righe in più verso l'alto, passo max(10 − (righe − 2), 3)) | x 149-230, y 231-240 |
| armatura | x = w/2 − 91 + 8·i, y = cuori − (righe − 1)·passo − 10 | y 221 con una riga di cuori |
| cibo (o vita della cavalcatura) | dal bordo x = w/2 + 91 verso sinistra, 8 px a icona, y = h − 39 | x 250-331, y 231-240 |
| aria | come il cibo, una riga sopra (y − 10) | y 221 |
| nome dell'oggetto in mano | centrato, y = h − 59 (+14 senza barra della vita); su 1.21.1 con sfondo | y 211 |
| barra delle azioni (`overlayMessage`) | centrata, `translate(w/2, h − 68)`; su 1.21.1 con sfondo | y ~198-207 |
| chat | riga più bassa a `guiHeight − 40`, righe verso l'alto, da x ≈ 0 (larghezza predefinita ~320 [non verificato]) | sopra y 230 |
| sottotitoli (se attivi) | in basso a destra, dal bordo y = h − 35 verso l'alto | sopra y 235, al bordo destro |
| indicatore di salvataggio | 1.20.1: x = w − larghezza testo − 10, y = h − 15; **1.21.1: x = w − larghezza testo − 2, y = h − 35, con sfondo**, disegnato da `GameRenderer` dopo l'HUD (su NeoForge è anche lo strato `SAVING_INDICATOR`) | 1.20.1 y 255; 1.21.1 y 235 |
| effetti | in alto a destra, icone 24 x 24 a x = w − 25·n; benefici a y = 1, dannosi a y = 1 + 26 = 27 (+15 in demo) | x ≥ 480 − 25·n, y 1-51 |

**Forge e NeoForge [sorgente, javap]:** su Forge 1.20.1 l'HUD lo disegna `ForgeGui` (strati `VanillaGuiOverlay`; la barra degli oggetti delega a `Gui.renderHotbar`): cuori e armatura con `left = width / 2 - 91`, `top = height - leftHeight`, cibo e aria con `top = height - rightHeight`, le due pile partono da 39 e crescono di 10 a riga. Su NeoForge 21.1 `renderItemHotbar` è identico a V21, e `renderPlayerHealth` è diviso in `renderHealthLevel`, `renderArmorLevel`, `renderFoodLevel`, `renderAirLevel` con `guiHeight() - leftHeight`/`rightHeight`, rimessi a 39 a ogni fotogramma e cresciuti di 10 a riga. Quindi le posizioni di base sono quelle della tabella finché un'altra mod non aggiunge righe; con altre mod cuori, armatura, cibo e aria possono salire. Gli altri metodi di `Gui` non li ho confrontati con il vanilla.

Non letti nel bytecode [non verificato]: indicatore d'attacco in modalità barra (a destra della barra degli oggetti, a sinistra per i mancini, circa 18 x 18), barre dei boss (in alto al centro), punteggio laterale (a destra a metà altezza), avvisi dei progressi e delle ricette (in alto a destra, 160 x 32 ciascuno).

**Cosa cambia su 1.21.1:** solo l'indicatore di salvataggio (sale da y = h − 15 a h − 35 e si avvicina al bordo), lo sfondo dietro nome dell'oggetto e barra delle azioni, gli sprite e gli strati a z separate. Le altre posizioni sono le stesse [javap].

**Per M4 [deduzione]:** in basso a sinistra c'è posto per x 0-118 e y 231-270 (sotto la chat, a sinistra della seconda mano); in basso a destra, a destra della barra (x ≥ ~362), restano da evitare l'indicatore d'attacco, la seconda mano dei mancini (fino a x 360), l'indicatore di salvataggio (su 1.21.1 y 235-244 al bordo destro) e i sottotitoli (dal y 235 in su); in alto a destra gli effetti (fino a y 51) e gli avvisi. La riga dell'energia di oggi sta in alto a sinistra a (4, 4) e (4, 14) [repo].

---

## 7. Tinte

### `DyeItem` e `DyeColor` [javap]

- `DyeItem`: `public DyeItem(DyeColor, Item$Properties)`, `public DyeColor getDyeColor()`, `public static DyeItem byColor(DyeColor)`: uguali su V20 e V21.
- `DyeColor`, uguali: `getId()`, `getName()`, `getSerializedName()`, `getMapColor()`, `getFireworkColor()`, `getTextColor()`, `byId(int)`, `byName(String, DyeColor)`, `byFireworkColor(int)`.
- Il colore che tinge: **1.20.1** `public float[] getTextureDiffuseColors()`: il costruttore prende l'intero RGB e lo divide in tre `float` (componente / 255). **1.21.1** `public int getTextureDiffuseColor()`: il costruttore lo salva come `FastColor.ARGB32.opaque(rgb)`, cioè ARGB con alfa 0xFF. Gli interi di partenza sono gli stessi sulle due versioni (per esempio `WHITE` 16383998 = 0xF9FFFE e `ORANGE` 16351261 = 0xF9801D, letti nel `<clinit>` di V20 e V21).
- Solo Forge e NeoForge [javap]: `public TagKey<Item> getTag()` e `public static DyeColor getColor(ItemStack)` (da `DyeItem` o dai tag); NeoForge anche `getDyedTag()`. Su Fabric e vanilla non ci sono.

### Il cuoio tinto [javap]

- **1.20.1:** `DyeableLeatherItem.dyeArmor(ItemStack, List<DyeItem>)` (statico) legge `getTextureDiffuseColors()` e lo moltiplica per 255; il colore sta nel tag NBT `display.color`; costante `DEFAULT_LEATHER_COLOR`.
- **1.21.1:** `DyedItemColor.applyDyes(ItemStack, List<DyeItem>)` legge `getTextureDiffuseColor()` con `FastColor.ARGB32.red/green/blue`; il colore sta nel componente `DataComponents.DYED_COLOR`; vale solo per gli oggetti del tag `ItemTags.DYEABLE` (`minecraft:dyeable`, che c'è solo su 1.21.1); costante `LEATHER_COLOR`.
- Sulle due: media dei colori (quello che c'era più le tinte), poi luminosità riportata al massimo (le due chiamate a `Math.max` si vedono in entrambe); valori esatti delle costanti del cuoio non letti.

**Per M4 [deduzione]:** il colore a 24 bit di una tinta è `getTextureDiffuseColor() & 0xFFFFFF` su 1.21.1 e `(int)(f * 255f)` per componente su 1.20.1 (come fa vanilla): nome del metodo diverso, quindi una riga in `compat/` (o una tabella dei 16 interi, uguali sulle due versioni). `Appearance` tiene già i colori a 24 bit [repo]. Mescolare più tinte come il cuoio, o una tinta = un colore, è una scelta di design.

### Tag delle tinte e del ferro [unzip dei jar dei loader]

| | tutte le tinte | una tinta | lingotti di ferro |
|---|---|---|---|
| Forge 1.20.1 (47.4.29) | `forge:dyes` | `forge:dyes/red` | `forge:ingots/iron` |
| Fabric 1.20.1 (convention-tags-v1 1.5.6) | `c:dyes` | `c:red_dyes` | `c:iron_ingots` |
| Fabric 1.21.1 (convention-tags-v2 2.12.0) | `c:dyes` | `c:dyes/red` (include, opzionale, `#c:red_dyes`) | `c:ingots/iron` |
| NeoForge 21.1.266 | `c:dyes` | `c:dyes/red` (include, opzionale, `#forge:dyes/red`) | `c:ingots/iron` (include, opzionale, `#forge:ingots/iron`) |

- Vanilla non ha tag delle tinte né dei lingotti di ferro su nessuna delle due versioni. Il jar universale di Forge 1.20.1 non ha nessun tag `c:`. Il modulo convention-tags-v1 di Fabric 1.21.1 non porta file di dati.
- Costanti Java: Forge e NeoForge `Tags.Items.DYES`, `Tags.Items.INGOTS_IRON` [sorgente]; Fabric `ConventionalItemTags.DYES`, `ConventionalItemTags.IRON_INGOTS` nel package `...tag.convention.v1` su 1.20.1 e `...tag.convention.v2` su 1.21.1 [sorgente].
- `TagEntry` di 1.20.1 ha già il campo `required` e `optionalTag(...)` [javap]: le voci `{"id": "#...", "required": false}` funzionano sulle due versioni (NeoForge le usa nei suoi tag).

**Per M4 [deduzione]:** i dati di 1.20.1 (`src/main/resources-mc1.20.1/`) sono comuni a Forge e Fabric 1.20.1 [repo]: un riferimento a `forge:ingots/iron` o a `c:iron_ingots` vale solo su uno dei due. Un tag della mod (per esempio un materiale di riparazione) può elencare `minecraft:iron_ingot` più `{"id":"#forge:ingots/iron","required":false}` e `{"id":"#c:iron_ingots","required":false}` su 1.20.1, e `#c:ingots/iron` opzionale su 1.21.1. Nel codice, `stack.getItem() instanceof DyeItem dye` → `dye.getDyeColor()` e `stack.is(Items.IRON_INGOT)` valgono sulle quattro senza tag. Oggi le ricette della mod usano solo id semplici [repo].

---

## 8. Dati per giocatore sul server (preset)

### `SavedData` [javap; repo]

| | 1.20.1 | 1.21.1 |
|---|---|---|
| scrittura | `public abstract CompoundTag save(CompoundTag)` | `public abstract CompoundTag save(CompoundTag, HolderLookup$Provider)` |
| lettura/creazione | `DimensionDataStorage.computeIfAbsent(Function<CompoundTag, T>, Supplier<T>, String)` | `DimensionDataStorage.computeIfAbsent(SavedData$Factory<T>, String)` |
| uguali | `setDirty()`, `setDirty(boolean)`, `isDirty()` | |

Il registro dei telai `compat/impl/FrameStateSavedData` è già un `SavedData` nell'overworld (nome `redstone_exoframes_frames`, `CURRENT_SCHEMA = 2`), con le direttive solo su `save` e `computeIfAbsent`, e scrive il proprio file da sé con `RegistryFile` (temporaneo, poi al posto del vecchio, copia `_old`), non con il salvataggio del gioco [repo, `docs/compat.md`]. Un preset può stare dentro questo registro o in un secondo `SavedData`. Due fatti del registro decidono [repo, letti in `FrameRegistryFormat` e `FrameStateSavedData.write`]:

- il salvataggio ricostruisce il tag dalle sole sezioni note (`schema`, `worn`, `placed`, `frames` se ha voci non lette, `orphaned`): una sezione `presets` aggiunta senza alzare lo schema verrebbe **persa in silenzio** dal primo salvataggio di una versione più vecchia della mod;
- alzare `CURRENT_SCHEMA` fa aprire **tutto** il registro in sola lettura a una versione più vecchia: nessun telaio si può più indossare, piazzare o togliere finché non si torna alla versione nuova.

Un secondo `SavedData` con un file suo (per esempio `redstone_exoframes_presets`) non tocca nessuna delle due garanzie: una versione più vecchia non lo conosce e non lo apre [deduzione]. Va scelto come scriverlo: con il salvataggio del gioco (`setDirty()`; su NeoForge 1.21.1 il gioco scrive i `SavedData` in un altro thread, [repo] `docs/compat.md`) o con la stessa strada di `RegistryFile` (file temporaneo, sostituzione, copia `_old`).

### Le alternative dei loader (non uniformi)

- Forge 1.20.1: `IForgeEntity.getPersistentData()` → `CompoundTag` [sorgente]; `Player.PERSISTED_NBT_TAG` (`"PlayerPersisted"`) dentro quel tag, che `ServerPlayer` copia alla rinascita (la stringa compare nel suo bytecode) [javap].
- NeoForge 21.1: lo stesso `getPersistentData()` (`IEntityExtension`) e `PERSISTED_NBT_TAG` [sorgente, javap]; in più le Data Attachments (`AttachmentType`, `IAttachmentHolder`, con copia alla morte) [sorgente, non approfondite].
- Fabric: nessun dato persistente sull'entità; `fabric-data-attachment-api-v1` 1.0.2 (1.20.1) e 1.4.7 (1.21.1), tutte e due `@ApiStatus.Experimental`: `AttachmentRegistry.createPersistent(ResourceLocation, Codec<A>)`, `builder().persistent(codec).copyOnDeath().initializer(...).buildAndRegister(id)` (su 1.21.1 anche `create(id, Consumer<Builder<A>>)`) [sorgente].
- Architectury 9.2.14 e 13.0.11: nessuna API per i dati delle entità (package: `annotations`, `compat`, `core`, `event`, `extensions`, `fluid`, `hooks`, `impl`, `init`, `mixin`, `networking`, `platform`, `plugin`, `registry`, `utils`) [sorgente].

La tabella di `docs/compat.md` dice già la scelta di M2b per lo stato per giocatore: un `SavedData` comune ai quattro target, al posto di capability, attachment e data-attachment [repo].

**Per M4 [deduzione]:** tre preset × due colori per sei slot sono poche decine di interi: viaggiano senza problemi nei dati extra del menu (sotto i 32600 byte di Forge e NeoForge) o in un pacchetto di `Net`. Un pacchetto C2S per salvare un preset deve stare in un file che referenzia `Compat.frameState()` (regola 5 di `tools/check_sources.sh`) [repo].

---

## Cosa non ho potuto verificare

- Nulla è stato eseguito (niente Gradle, niente gioco): tutto viene da bytecode, sorgenti delle librerie e documenti del repository. Le uniche prove in gioco citate sono quelle già nel repository (M3d).
- Forge 1.20.1: se `MenuScreens.register` chiamato dal costruttore della mod (fase di costruzione parallela, mappa vanilla non sincronizzata) può dare problemi con altre mod che fanno lo stesso.
- NeoForge 21.1: che `CLIENT_SETUP` arrivi dopo `RegisterMenuScreensEvent` è dedotto dall'ordine nel costruttore di `Minecraft` e in `ClientModLoader`, non osservato in gioco.
- Il percorso del pacchetto vanilla del pulsante (`handleInventoryButtonClick` → `clickMenuButton`) non è stato riletto istruzione per istruzione; ne ho letto solo le firme.
- Su 1.21.1, che gli strati vanilla coprano ciò che disegna il gancio dell'HUD: dedotto dalle z (200 per strato) e coerente con la nota del repository su F3; lo stato del test di profondità non l'ho letto.
- `SuitDraw.draw` chiamato direttamente da una schermata (luce, normali con la scala z negativa, ritaglio): mai provato. Provata è solo la tuta indossata nell'anteprima vanilla dell'inventario.
- Posizioni non lette nel bytecode: indicatore d'attacco, barre dei boss, punteggio laterale, avvisi (toast), altezza e larghezza della chat chiusa; il calcolo della scala automatica di `Window`.
- Comportamento di `isDown()`/`consumeClick()` con una schermata aperta, e delle combinazioni F3+G, F3+H, F3+N con i tasti della mod.
- Formula esatta della distanza di `stillValid` su Forge 1.20.1 (usa l'attributo di portata di Forge) e condizione esatta di `DoorBlock.playerWillDestroy` su 1.21.1 (ho letto le chiamate, non i salti).
- Che `updateShape` → aria faccia cadere il bottino della metà tolta (`Block.updateOrDestroy` non riletto): è il motivo della condizione `part: head` nella tabella del letto, ma non l'ho visto nel bytecode.
- Valori esatti delle costanti del colore del cuoio; che cosa fa una ricetta che usa un tag inesistente come ingrediente su ciascuna versione.
- Le Data Attachments di NeoForge e di Fabric: lette solo le firme, non provate.
- I jar dei loader confrontati con il vanilla solo per le classi elencate in "Fonti": per le altre (fra cui `Gui` di Forge, `Screen`, `GuiGraphics`, `LevelChunk`, `DoorBlock`, `FriendlyByteBuf`, `SavedData`) le patch di Forge e NeoForge non le ho guardate.
