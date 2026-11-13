# Audit di Mjolnir Armor per Redstone Exoframes

Data: 2026-11-13 · Stato: bozza per Luigi · Fonte: nove letture parallele del repo `Mjolnir Armor mod` (1.0.1, Forge 47 / MC 1.20.1, GeckoLib 4.4.9, Pehkui 3.8.2), sottosistemi: core-registration, armor-items, client-render, hud, shield-effects, assembly-station, grapple, worldgen-loot-data, compat-build-tools. Riferimenti di design: `docs/superpowers/specs/2026-11-12-redstone-exoframes-design.md` (§4–§8) e `claude-doc-2026-11-04-redstone-exoframes.md`.

Convenzioni: i percorsi Java sono relativi a `src/main/java/com/toxineren/mjolnir_armor/`; `File.java:12-34` indica le righe citate dai lettori. Verdetti: **riuso diretto** (copiabile cambiando solo predicato/nomi), **adattare** (struttura copiabile con modifiche per loader/versione), **riscrivere dalle idee** (si riscrive da zero seguendo la tecnica), **scartare**. Dove i lettori non concordano lo si dice esplicitamente.

---

## 1. Sintesi

Mjolnir Armor è un mod Forge 1.20.1 a tema Halo: tre set di armature GeckoLib (MK VI "117", Spartan IV con 4 vernici, Locke) indossabili solo tramite una stazione di assemblaggio multiblocco animata, uno scudo energetico da 100 punti su capability Forge, un HUD da visore con barra scudo poligonale, un sistema "siero/augmentation SPARTAN" che scala il giocatore con Pehkui, un rampino (Grappleshot), un laboratorio sotterraneo generato via jigsaw, loot via Global Loot Modifier e integrazioni opzionali con Dawn of the Flood e Gigeresque. Il codice Java appare originale dell'autore (commenti italiani coerenti, nessun header di terzi), ma gli asset derivano da Vic's Modern Warfare, da un OBJ di provenienza ignota, da render Halo e da suoni di Halo Infinite.

Riusabilità: su circa 190 righe di inventario (alcune classi viste da più lettori) solo 4 file restano **riuso diretto** dopo il consolidamento di §2.10 (`EntityTurnMixin`, `MinecraftGlowMixin`, `tools/obj2geo.py`, `tools/pngio.py`; `SlotMixin`, che un lettore dava come riuso diretto, è consolidato in **adattare**); il resto è da adattare o riscrivere dalle idee, perché ogni hook è Forge-only e ogni nome è IP Halo. Valgono soprattutto le **tecniche**: blocco degli slot armatura, lock di input/camera/posa, assorbimento del danno, sync S2C per l'HUD, modificatori di attributo idempotenti, renderer GeckoLib per stato, multiblocco controller+parte, decorazione jigsaw chunk-safe, pipeline Python per geo/texture. Mjolnir non ha energia a celle, moduli, entità-telaio, sei pezzi né riparazione/verniciatura in stazione: queste parti di Exoframes sono nuove.

**I 5 rischi principali del porting**
1. **Licenza e provenienza**: il mod è "All Rights Reserved" (gradle.properties:53) con un LICENSE.txt che è in realtà la LGPL del template Forge; il rilascio MIT dei sistemi copiati richiede la conferma di Luigi come unico autore, e nessun asset di Mjolnir deve entrare nel repo o nella sua storia git (VMW, Halo, Halo Infinite).
2. **Divergenza GeckoLib 4.8.4 (1.20.1) / 4.9.3 (1.21.1)**: package diversi (`core.*` vs radice), hook di rendering degli item diversi su tutti e quattro i target (`initializeClient` / `RenderProvider` astratto / `createGeoRenderer`), firme colore float→int; senza classi base in `compat/` i `//? if` uscirebbero da `compat/` (verificato con javap dal lettore armor-items).
3. **Hook Forge senza equivalente Fabric/Architectury**: `canEquip`, `damageItem`, `LivingDamageEvent.setAmount`, `MobEffectEvent.Applicable`, `LivingFallEvent`, `RenderPlayerEvent`, `RenderArmEvent`, `MovementInputUpdateEvent`, capability e `getPersistentData`; servono attachment per loader e un budget di mixin deciso prima di M3.
4. **Superficie di API vanilla 1.20.1→1.21.1 più larga delle cinque interfacce di §5.2**: NBT→data component, `ArmorMaterial` come Holder, pacchetti `StreamCodec`, `AttributeModifier` con id `ResourceLocation`, `Block.use`, salvataggio dei BE con `HolderLookup.Provider`, `DamageSource.isIndirect` sostituito da `isDirect()` (logica invertita), `durability(-1)` che in 1.21.1 imposta `MAX_DAMAGE=-1` su un item che resta "danneggiabile" (verificato con javap, §4); `compat/` deve crescere (proposte al §4).
5. **Accoppiamento Pehkui/Halo e bug latenti**: i predicati centrali (`ArmorUtils`) caricano Pehkui in un inizializzatore statico, la coreografia della stazione presuppone giocatori scalati, e lo stato client vive in statici mai azzerati (40 pacchetti/s, timer a orologio di sistema, clone senza `reviveCaps`); copiare il codice importerebbe i bug, quindi si riscrive dalle idee.

---

## 2. Mappa di riuso per classe

Ogni riga riproduce ciò che un lettore ha riportato; una classe vista da più lettori compare in ciascun sottosistema con il verdetto di quel lettore. In fondo al paragrafo, la tabella 2.10 riassume i verdetti discordanti con la decisione consolidata.

### 2.1 core-registration (rischio di porting: medio)

| Classe | Ruolo | Verdetto | Destinazione in Exoframes | Note (file:line) |
|---|---|---|---|---|
| MjolnirArmor.java | Entrypoint `@Mod`: tutti i DeferredRegister, SimpleChannel, config, init GeckoLib/Pehkui, remap id, client setup | riscrivere dalle idee | platform/ (entrypoint) + content/ (registri) | IP. Registri MjolnirArmor.java:104-192; ordine `register(bus)` :234-247 (su Fabric conta); tab creativa senza `.title()` :195-225; `durability(-1)` :109-130; Pehkui forzato :250; MissingMappings :281-294 |
| MjolnirConfig.java | ForgeConfigSpec COMMON, un blocco per programma SPARTAN | riscrivere dalle idee | core/ (modello config POJO) + platform/ (IO Gson via Platform) | IP. Blocco per entità :13-40; valori letti all'uso e riallineati al login :6-8; COMMON non sincronizzato col client |
| ModArmorMaterials.java | Enum `ArmorMaterial` 1.20.1 (TITANIUM, SPARTAN_IV) | adattare | compat/ArmorMaterials (lato 1.20.1) | IP nei valori. Forge `Lazy` :11-31 → `Suppliers.memoize`; `getName()` con namespace :68-71 rompe Fabric 1.20.1 |
| ModSounds.java | Registro di 14 SoundEvent con helper `register(name)` | adattare | content/ModSounds | Helper :35-37; nomi e .ogg non portabili |
| PlayerScaleCommand.java | Comandi operatore `/scaleme`, `/spartanme`, `/spartan` | riscrivere dalle idee | content/ (comando `/exoframe` via CommandRegistrationEvent) | IP+Pehkui :34-44; literal generati da enum :56-64 |
| util/ArmorUtils.java | Predicati set/completo/attivo/visore + scala Pehkui | riscrivere dalle idee | core/ (query sullo stato telaio) | Pehkui in static init :22; `instanceof` ordinato :37-43; gate :80-88 |
| util/ArmorVariant.java | Vernice Spartan IV come tag NBT `MjolnirVariant` | riscrivere dalle idee | compat/ItemDataAccess + core/ (modello colore) | `of/applyTo` con default=assente :35-52; ordinal usato come predicato :23 |
| resources/META-INF/mods.toml | Descrittore Forge (template MDK) | riscrivere dalle idee | platform/ risorse: mods.toml, neoforge.mods.toml, fabric.mod.json | geckolib+pehkui obbligatori :71-82; dotf opzionale :83-89 |
| resources/mjolnir_armor.mixins.json | Config mixin common+client | adattare | un mixins.json per variante; classi in compat/mixin | voci dotf.* :13-20 da togliere; JAVA_17→JAVA_21 su 1.21.1 |
| resources/pack.mcmeta | Metadati del pack | adattare | platform/ risorse | pack_format 15 (1.20.1) → 34 risorse / 48 dati (1.21.1) :6 |
| util/SpartanProgram.java | Enum dei programmi SPARTAN (lore) | scartare | nessuna (forma → enum Tier in core/) | Switch enum→blocco config :53-59; `byId` :66-72 |
| util/SpartanAugmentation.java | Macchina a stati del siero su NBT persistente, modificatori, Pehkui, sync | riscrivere dalle idee | core/ (timer) + compat/ (attachment, Net) + content/ | Modificatori a id fisso :70-85, transienti :437-444; resync login/clone/dimensione :456-491; Pehkui :194-197, :374-379 |
| items/ArmorVariantRecipe.java | CustomRecipe "un pezzo + una tinta" | adattare | content/ (ricetta) dietro compat | `find/assemble` :28-61; costruttore e `CraftingContainer` cambiano in 1.21.1 |
| blocks/StationVariant.java | Enum delle due taglie della stazione | riscrivere dalle idee | block/ | Un enum guida i nomi di blocco/item/BE (MjolnirArmor.java:171-178); `acceptsPiece` per classe :78-81 |
| loot/AddItemLootModifier.java | GLM "aggiungi item" | riscrivere dalle idee | content/ (loot) via evento compat/platform | GLM :14-53 assente su Fabric → `LootEvent.MODIFY_LOOT_TABLE` |
| network/ShieldUpdatePacket.java | S2C float dello scudo scritto in uno statico | riscrivere dalle idee | compat/Net + client/ (store) | Registrato senza direzione (MjolnirArmor.java:259-265); handle :25-30 |
| network/AugmentationSyncPacket.java | S2C id del programma | riscrivere dalle idee | compat/Net | IP. `readUtf(8)` :26 non tronca: oltre 8 caratteri lancia `DecoderException` e chiude la connessione (verificato con javap su 1.20.1); gli id attuali ("ii", "iii", "iv", "") stanno nel limite, il rischio è latente |
| network/GrapplePacket.java | C2S boolean dal tasto | riscrivere dalle idee | compat/Net | IP nel nome. Forma "tasto → pacchetto → server valida" :26-37 |
| compabilities/DotfIntegration.java | Integrazione Dawn of the Flood | scartare | nessuna (tecnica → Platform.isModLoaded dopo la 1.0) | IP. Riconoscimento per id/namespace senza caricare classi :35-66 |
| compabilities/GigeresqueIntegration.java | Integrazione Gigeresque per riflessione | scartare | nessuna | Riflessione su nome semplice :36-48 |
| datagen/DataGenerators.java | GatherDataEvent con un solo provider | scartare | nessuna | Provider vuoto :26 |
| datagen/ModItemModelProvider.java | ItemModelProvider vuoto | scartare | nessuna | `registerModels` vuoto :19-22 |
| items/SpartanSerumItem.java | Consumabile a durata d'uso | riscrivere dalle idee | item/ (kit di riparazione, cambio cella) | IP. Flusso use/onUseTick/finish :47-99; `getUseDuration`/`appendHoverText` cambiano firma |
| items/MjolnirHelmetItem.java | Elmo GeckoLib, stato "rotto" | adattare | item/ (segnaposto) + core/ (stato rotto) | IP. `damageItem` Forge :38-44; `isBroken` :32-36 |
| items/MjolnirArmorItem.java | Pezzo corpo GeckoLib non indossabile a mano | adattare | item/ | IP. `canEquip` :38-41 solo Forge/NeoForge |
| mixin/SlotMixin.java | Blocca il prelievo dagli slot armatura | **riuso diretto** | compat/mixin | :30-36; estendere allo slot 39 e ai segnaposto |
| mixin/ThrownPotionMixin.java | Esclude chi ha l'elmo sigillato dalle pozioni splash | adattare | compat/mixin | `@Redirect` :18 → MixinExtras `@WrapOperation`; firma di `applySplash` cambia |
| mixin/AreaEffectCloudMixin.java | Idem per le nubi persistenti | adattare | compat/mixin | :18; verificare il target per variante |
| mixin/SinglePoolElementMixin.java | Hook dopo il piazzamento di un pezzo jigsaw | scartare | nessuna (idea per content/worldgen) | Parametro `LiquidSettings` in più su 1.21.1 |
| mixin/HumanoidModelMixin.java | Posa delle braccia dopo `setupAnim` | adattare | client/ (mixin sotto compat) | IP solo nella posa rampino; descrittore invariato su 1.21.1 |
| mixin/EntityTurnMixin.java | Blocca la rotazione della visuale | **riuso diretto** | client/ (mixin sotto compat) | :19-25; cambia solo il predicato |
| mixin/MinecraftGlowMixin.java | Forza il contorno luminoso di entità | **riuso diretto** | client/ (mixin sotto compat) | :15-18; predicato → modulo scanner |
| mixin/dotf/DotfClientRenderHelperMixin.java | `@Pseudo` su classi DotF | scartare | nessuna | IP, `remap=false` |
| mixin/dotf/DotfGeckoRenderPlayerMixin.java | `@Pseudo` su classi DotF | scartare | nessuna | IP |
| mixin/dotf/DotfMeleeArmsMixin.java | `@Pseudo` su classi DotF | scartare | nessuna | IP |
| worldgen/StructureStations.java | Decorazioni del laboratorio UNSC | scartare | nessuna (idee per M6) | IP. `box.isInside` per cella :113-135, :155-160; caricamento NBT dei BE :173-188 |
| ArmorEffectsHandler.java | Immunità a proiettili, "magic", caduta | riscrivere dalle idee | content/ (eventi) + core/ (categorie di danno) | `isIndirect()` :18; `getMsgId().equals("magic")` :21; LivingAttackEvent/LivingFallEvent |

### 2.2 armor-items (rischio: medio)

| Classe | Ruolo | Verdetto | Destinazione in Exoframes | Note (file:line) |
|---|---|---|---|---|
| items/MjolnirArmorItem.java | Base GeckoLib di torso/gambe/stivali; rifiuta l'equipaggiamento manuale | riscrivere dalle idee | item/ | IP. `canEquip=false` + `use()` :38-49; prima di sei copie di `initializeClient` :51-67 |
| items/MjolnirArmorItem2.java | Pezzo Spartan IV (tooltip vernice + renderer) | scartare | nessuna | IP. Tooltip :30-34; `initializeClient` :36-52 |
| items/MjolnirArmorItemLocke.java | Pezzo Locke (solo cambio renderer) | scartare | nessuna | IP. :23-37 |
| items/MjolnirHelmetItem.java | Elmo: unico pezzo danneggiabile, stato rotto derivato | riscrivere dalle idee | core/ | IP. `isBroken` :32-36; clamp a max-1 :38-44 |
| items/MjolnirHelmetItem2.java | Elmo Spartan IV | scartare | nessuna | IP. :37-61 |
| items/MjolnirHelmetItemLocke.java | Elmo Locke | scartare | nessuna | IP. :19-33 |
| items/SpartanSerumItem.java | Siero a 64 tick | riscrivere dalle idee | item/ | IP. :47-105; `UseAnim.BOW` per evitare il blocco del sigillo :102-105 |
| items/ArmorVariantRecipe.java | Ricetta pezzo + tinta | adattare | content/ | `Object[]` non tipizzato :28-47; utile solo se resta una ricetta da banco |
| util/ArmorVariant.java | Vernice come NBT | riscrivere dalle idee | compat/ | IP. 16×16 combinazioni primario/secondario → tinta a runtime, non una texture per colore |
| util/ArmorUtils.java | Predicati di set | riscrivere dalle idee | core/ | IP. :22, :37-43 |
| ModArmorMaterials.java | Enum materiali 1.20.1 | adattare | compat/ | IP. Metodi :45-81; record registrato su 1.21.1 |
| MjolnirArmor.java | Registrazioni degli item | adattare | content/ | IP. Asimmetria elmi/corpo :105-140; `durability(-1)` |
| ModSounds.java | Registro suoni | adattare | content/ | IP nei nomi |
| util/SpartanAugmentation.java | Back-end del siero | scartare | nessuna | IP. Tecniche da tenere: id fissi :70-85, transienti :430-444 |
| util/SpartanProgram.java | Lore | scartare | nessuna | IP |
| MjolnirConfig.java | ForgeConfigSpec | scartare | nessuna | IP. Tenere l'idea "lettura pigra, riallineo al login" :5-9 |
| network/AugmentationSyncPacket.java | Sync programma | scartare | nessuna | IP |
| PlayerScaleCommand.java | Comandi Pehkui | scartare | nessuna | IP. Idea `/exoframe give|repair|energy|info` |
| mixin/SlotMixin.java | Lock degli slot | adattare | compat/ | :30-36; non tocca `Inventory.dropAll` |
| loot/AddItemLootModifier.java | GLM | scartare | nessuna | → Architectury LootEvent |
| client/renderer/MjolnirGeoArmorRenderer.java | Base dei renderer d'armatura | riscrivere dalle idee | client/ | `renderToBuffer` float→int color su 1.21 |
| client/renderer/MjolnirHelmetRenderer2.java | Texture per vernice × rotto | riscrivere dalle idee | client/ | IP. :21-25 |
| client/model/MjolnirArmorModel.java | GeoModel (tre risorse) | adattare | client/ | IP negli asset; API GeoModel invariata in 4.9.3 |
| client/renderer/MjolnirHelmetItemRenderer.java | GeoItemRenderer per l'icona 3D | adattare | client/ | IP. Costruttore identico in 4.4.9/4.8.4/4.9.3 (verificato); unica via di icona 3D su tutti e quattro i target |

### 2.3 client-render (rischio: medio)

| Classe | Ruolo | Verdetto | Destinazione in Exoframes | Note (file:line) |
|---|---|---|---|---|
| client/GeckoPlayerArmor.java | Shim a riflessione per le matrici di DotF | scartare | nessuna | FQCN fisso :17; lookup fallito messo in cache per sempre :43-56 |
| client/FirstPersonArmorArms.java | Braccio corazzato in prima persona | adattare | client/ | `RenderArmEvent` :42-54 → mixin su `PlayerRenderer.renderHand` su Fabric; la cancellazione sopprime i renderer di altri mod :51-53 |
| client/PlayerModelHider.java | Nasconde la skin sotto l'armatura | adattare | client/ | Regole per slot :45-70; testa 0.85 con elmo rotto :31, :79-85 |
| client/VisorTracker.java | Predicato del contorno (Flood entro 24 blocchi) | riscrivere dalle idee | client/ | IP. :15-22; scansione per entità per frame |
| client/HelmetSounds.java | Suono "visore online" + allarme | adattare | client/ | :19-37; statici mai azzerati :14-15 |
| client/AssemblyPose.java | Posa a braccia aperte con blend smoothstep | riscrivere dalle idee | client/ | :28-48; `getDeltaFrameTime` :33 sparisce in 1.21; WeakHashMap per Player :23-24 |
| client/AssemblyInputLock.java | Lock di yaw/pitch e input | adattare | client/ | :21-48; `MovementInputUpdateEvent` solo Forge/NeoForge |
| client/model/MjolnirArmorModel.java | GeoModel MK VI | riscrivere dalle idee | client/ | IP. :10-22; un GeoModel parametrico per pezzo/tier |
| client/model/MjolnirArmorModel2.java | GeoModel Spartan IV | riscrivere dalle idee | client/ | IP. :12-24 |
| client/model/MjolnirArmorModelLocke.java | GeoModel Locke | scartare | nessuna | IP |
| client/model/MjolnirHelmetModel.java | GeoModel elmo MK VI | riscrivere dalle idee | client/ | IP. Convenzione "una geo per slot, scheletro a 14 bone" |
| client/model/MjolnirHelmetModel2.java | GeoModel elmo Spartan IV | scartare | nessuna | IP |
| client/model/MjolnirHelmetModelLocke.java | GeoModel elmo Locke | scartare | nessuna | IP |
| client/renderer/MjolnirGeoArmorRenderer.java | Base GeoArmorRenderer: braccio in prima persona + percorso DotF | adattare | client/ | `renderFirstPersonArm` :115-136 da tenere; ramo DotF :38-46, :54-106 da togliere; bookkeeping duplicato :62-68 |
| client/renderer/MjolnirArmorRenderer.java | Sottoclasse vuota | scartare | nessuna | IP |
| client/renderer/MjolnirArmorRenderer2.java | Texture per vernice | riscrivere dalle idee | client/ | IP. :13-17; usare `DyeableGeoArmorRenderer` |
| client/renderer/MjolnirArmorRendererLocke.java | Sottoclasse vuota | scartare | nessuna | IP |
| client/renderer/MjolnirHelmetRenderer.java | Texture rotta per l'elmo | riscrivere dalle idee | client/ | IP. :17-25; seme degli stati visivi |
| client/renderer/MjolnirHelmetRenderer2.java | Matrice vernice × rotto | riscrivere dalle idee | client/ | IP. :18-25; in Exoframes due passate indipendenti (tinta + overlay danno) |
| client/renderer/MjolnirHelmetRendererLocke.java | Duplicato Locke | scartare | nessuna | IP |
| client/renderer/MjolnirHelmetItemRenderer.java | GeoItemRenderer dell'elmo | scartare | nessuna | Codice morto: il modello item è `forge:obj` (models/item/mjolnir_helmet_117.json:2) |
| client/renderer/MjolnirHelmetItemRenderer2.java | GeoItemRenderer elmo Spartan IV | scartare | nessuna | Codice morto come sopra |
| mixin/HumanoidModelMixin.java | Posa a TAIL di `setupAnim` | adattare | client/ | :29-40; l'armatura GeckoLib segue da sola |
| mixin/MinecraftGlowMixin.java | Contorno luminoso | **riuso diretto** | client/ | :12-18 |
| mixin/EntityTurnMixin.java | Lock della visuale | **riuso diretto** | client/ | :16-25; solo nella lista mixin client |
| client/DotfSkinHider.java | Nasconde ossa del modello DotF | scartare | nessuna | Trucco `setHidden(true)+setChildrenHidden(false)` :66-73 da ricordare |
| mixin/dotf/DotfClientRenderHelperMixin.java | `@Pseudo` DotF | scartare | nessuna | :20-31 |
| mixin/dotf/DotfGeckoRenderPlayerMixin.java | `@Pseudo` DotF | scartare | nessuna | :20-30 |
| mixin/dotf/DotfMeleeArmsMixin.java | `@Redirect` in renderer DotF | scartare | nessuna | :19-38; `ModelPart.render` float→int color in 1.21 |
| client/GrappleFirstPerson.java | Braccio del rampino in prima persona | scartare | nessuna | IP. Proiezione NDC :104-125 come tecnica |
| client/renderer/SpartanCorpseRenderer.java | Renderer del cadavere (PlayerModel + armor layer) | riscrivere dalle idee | entity/ | IP. :30-44; `setupRotations` riceve `scale` in 1.21.1 :47 |
| blocks/ArmorAssemblyBlockRenderer.java | GeoBlockRenderer della stazione | riscrivere dalle idee | block/ | Compensazione di scala :24-33; ossa nascoste :35-40 |
| util/ArmorVariant.java | Vernice | riscrivere dalle idee | item/ | IP. `ItemProperties.register` esiste ancora su 1.21.1 |
| util/ArmorUtils.java | Gate del visore | riscrivere dalle idee | core/ | IP. :22, :80-88 |
| items/MjolnirArmorItem.java | Cablaggio client per item | adattare | compat/ | `initializeClient` :51-67; texture vanilla reale per evitare spam :69-74; `registerControllers` vuoto :76-80 |
| MjolnirArmor.java | ClientModEvents | adattare | platform/ | IP. :297-328 |

### 2.4 hud (rischio: medio)

| Classe | Ruolo | Verdetto | Destinazione in Exoframes | Note (file:line) |
|---|---|---|---|---|
| client/overlay/MjolnirHudOverlay.java | HUD: cornice del visore, barra scudo poligonale, glow, impulso, indicatore rampino | riscrivere dalle idee | client/ | IP. Hook `RenderGuiOverlayEvent.Post`+HOTBAR :161-167; `fillShape` :342-375; costanti misurate su arte Halo :76-101, :133-143; BufferBuilder :264-269, :386 |
| util/ArmorUtils.java | Gate `isVisorOnline` | riscrivere dalle idee | core/ | IP. :85-88 → HudGate NONE/BAR_ONLY/FULL |
| MjolnirArmor.java | Contiene lo statico `CLIENT_SHIELD_VALUE` | scartare | nessuna | IP. :92 |
| network/ShieldUpdatePacket.java | Sync S2C del valore | riscrivere dalle idee | compat/ | Payload HUD completo in Exoframes |
| events/ShieldEventHandler.java | Assorbimento + tick + invio | riscrivere dalle idee | compat/ | Nessun filtro di fase :70-88 |
| capabilities/ShieldCapability.java | Pool scudo 0..100 | riscrivere dalle idee | core/ | `System.currentTimeMillis` :35, :48-57 |
| capabilities/ShieldProvider.java | Provider di capability | scartare | nessuna | Boilerplate Forge |
| client/GrappleClient.java | Tasto G, fisica di trazione, cooldown | riscrivere dalle idee | client/ | IP. Loop tasti :76-86; frazione di cooldown :65-68 |
| util/Grappleshot.java | Lato server del rampino | scartare | nessuna | IP. Cooldown come tick "pronto-a" :65-67 |
| entity/GrappleHookEntity.java | Proiettile rampino | scartare | nessuna | IP. `clientHookOf` :92-101 |
| items/MjolnirHelmetItem.java | Stato rotto per l'HUD | riscrivere dalle idee | core/ | IP. :34-44 |
| util/SpartanAugmentation.java | Programmi SPARTAN | scartare | nessuna | IP. `canWearMarkVI` usa Pehkui :110-114 |
| client/HelmetSounds.java | Audio dell'HUD | adattare | client/ | Avviso energia <10% |
| client/AssemblyInputLock.java | Ospita il tick di HelmetSounds | scartare | nessuna | :26 (accoppiamento improprio) |
| client/VisorTracker.java | Predicato del contorno | adattare | client/ | Scanner e rilevatore di Sculk |
| mixin/MinecraftGlowMixin.java | Contorno | **riuso diretto** | client/ | :15-18 |
| compabilities/DotfIntegration.java | DotF | scartare | nessuna | IP |
| compabilities/GigeresqueIntegration.java | Gigeresque | scartare | nessuna | :36-39 |
| network/AugmentationSyncPacket.java | Sync programma | scartare | nessuna | IP |
| network/GrapplePacket.java | C2S rampino | scartare | nessuna | IP |
| ArmorEffectsHandler.java | Immunità | scartare | nessuna | Confronto stringa :21 |
| ModSounds.java | Suoni | scartare | nessuna | IP |

### 2.5 shield-effects (rischio: medio)

| Classe | Ruolo | Verdetto | Destinazione in Exoframes | Note (file:line) |
|---|---|---|---|---|
| capabilities/ShieldCapability.java | Pool scudo, drenaggio, ricarica ritardata, suoni, NBT | adattare | core/ (EnergyBuffer / ShieldModel puro) | Aritmetica :28-63; `lastDamageTime` non salvato :65-71 |
| capabilities/ShieldProvider.java | Colla ICapabilityProvider | scartare | platform/ (sostituito da attachment) | :16-49 |
| events/ShieldEventHandler.java | Attach, clone, assorbimento, tick, rimozione effetti | riscrivere dalle idee | compat/ | Assorbi/resto :49-67; clone senza `reviveCaps` :40-46; effetti rimossi a posteriori :94-107 |
| events/HelmetSealLock.java | Niente cibo/bevande, esclusione dalle pozioni | riscrivere dalle idee | nessuna (non nel design) | :39-48; `toList()` immutabile :33-36 |
| ArmorEffectsHandler.java | Immunità a indiretto/magic/caduta | riscrivere dalle idee | compat/ | `isIndirect()` esiste solo su 1.20.1; su 1.21.1 c'è `isDirect()` con logica invertita (entrambi verificati con javap) :18 |
| util/SpartanAugmentation.java | Siero | scartare | nessuna | IP |
| util/SpartanProgram.java | Lore | scartare | nessuna | IP |
| network/ShieldUpdatePacket.java | S2C float | adattare | compat/ | :17-28 |
| network/AugmentationSyncPacket.java | S2C programma | scartare | nessuna | IP |
| mixin/AreaEffectCloudMixin.java | Filtro nubi | scartare | nessuna | Descrittore identico 1.20.1/1.21.1 (verificato) |
| mixin/ThrownPotionMixin.java | Filtro splash | scartare | nessuna | `applySplash(List)` → `(Iterable)` su 1.21.1 (verificato) |
| mixin/SlotMixin.java | Lock degli slot | adattare | compat/ | In 1.21.1 `ArmorSlot.mayPickup` chiama `super`, l'inject resta valido |

### 2.6 assembly-station (rischio: medio)

| Classe | Ruolo | Verdetto | Destinazione in Exoframes | Note (file:line) |
|---|---|---|---|---|
| blocks/ArmorAssemblyBlock.java | Controller del multiblocco: FACING+LIFT, verifica di piazzamento, apertura GUI, cascata | adattare | block/MaintenanceStationBlock + compat/CompatEntityBlock | Verifica celle :75-83; `setPlacedBy` :86-96; cascata :111-127; `NetworkHooks` :103; `Block.use` :100 |
| blocks/ArmorAssemblyBlockEntity.java | BE: inventario 4 slot, GeckoLib, tick a keyframe, teletrasporto | riscrivere dalle idee | block/MaintenanceStationBlockEntity + compat/CompatBlockEntity + core/ | IP. `ItemStackHandler` :82-97; tabella tick :71-77; update tag minimo :243-255; `setItemSlot` :407-415; `getRenderBoundingBox` :491-494 |
| blocks/ArmorAssemblyBlockModel.java | GeoModel per variante | adattare | client/station/MaintenanceStationModel | :10-17 |
| blocks/ArmorAssemblyBlockRenderer.java | GeoBlockRenderer con scala e ossa nascoste | adattare | client/station/ + compat/client/CompatGeoBlockRenderer | `preRender` float→int :21-23; :25-40 |
| blocks/ArmorAssemblyMenu.java | Menu 4 slot | riscrivere dalle idee | block/MaintenanceStationMenu; MenuType via `MenuRegistry.ofExtended` | Costruttore client :30-32; handler fittizio :36-39; `stillValid` :74-77 |
| blocks/ArmorAssemblyPartBlock.java | Cella invisibile del multiblocco | adattare | block/MaintenanceStationPartBlock | :54-74; `getCloneItemStack` Forge-only :77-84 |
| blocks/ArmorAssemblyPartBlockEntity.java | BE della cella con posizione del controller | adattare | o sostituito da blockstate HALF (stile letto) | `NbtUtils` :39-50 |
| blocks/ArmorAssemblyStructure.java | Rotazione locale→mondo | adattare | block/MaintenanceStationShape o core/ | `toWorld` :23-31 |
| blocks/AssemblyInteractionLock.java | Annulla le interazioni durante il ciclo | riscrivere dalle idee | content/events/ExoframeInteractionLock + core/ | Predicato :19-22; perdita item :49-55 |
| blocks/StationVariant.java | Taglie della stazione | scartare | nessuna | IP. `playerScale` mai letto :45 |
| client/screen/ArmorAssemblyScreen.java | Schermata minima | riscrivere dalle idee | client/station/ + compat/client/CompatContainerScreen | `renderBackground` :25; blit con dimensioni :31-34 |
| client/AssemblyInputLock.java | Lock input/visuale | riscrivere dalle idee | client/lock/ + mixin KeyboardInput | :39-48 |
| client/AssemblyPose.java | Blend della posa | riscrivere dalle idee | client/pose/ | IP (posa "scena Halo 4"). :28-48 |
| mixin/EntityTurnMixin.java | Lock della visuale | adattare | mixin client | :19-25 |
| mixin/HumanoidModelMixin.java | Posa | adattare | mixin client | :29-40 |
| worldgen/StructureStations.java | Stazioni nei jigsaw | scartare | nessuna (idea per M6) | IP. :153-160 |
| MjolnirArmor.java | Registrazione stazione | riscrivere dalle idee | content/ModBlocks, ModBlockEntities, ModMenus; client/ClientInit; platform/ | IP. :171-192 |

### 2.7 grapple (rischio: medio)

| Classe | Ruolo | Verdetto | Destinazione in Exoframes | Note (file:line) |
|---|---|---|---|---|
| entity/GrappleHookEntity.java | Proiettile a tre stati sincronizzati | riscrivere dalle idee | content/ (registrazione), entity/ (pattern), core/ (decisioni pure) | IP. `defineSynchedData` :68-72; rivalidazione owner :120-124; `ForgeEventFactory` :135 |
| client/GrappleClient.java | Tasto, trazione client, predizione | adattare | client/ + core/ (spinta jetpack, cooldown) | IP. `pull()` :127-148; fronte del salto :99-104; `releaseSent` :38-39, :96, :100 |
| client/GrappleFirstPerson.java | Braccio mirato in prima persona | scartare | nessuna | IP. Costanti dal modello Halo :53-54 |
| client/model/GrappleHookModel.java | Modello del gancio | scartare | nessuna | IP |
| client/renderer/GrappleHookRenderer.java | Gancio + cavo | scartare | nessuna | IP. `endVertex()` :94-95 sparisce in 1.21 |
| network/GrapplePacket.java | C2S intent | riscrivere dalle idee | compat/Net + core/ (enum intent) | :26-37 |
| util/Grappleshot.java | Facade server, cooldown, origine al polso | riscrivere dalle idee | core/ (modello cooldown/burn) + servizio server | IP. Origine dentro i muri :51, :79-85; mappa statica :31 |
| mixin/HumanoidModelMixin.java | Braccio alzato | adattare | mixin client + helper posa | IP nel corpo |
| client/overlay/MjolnirHudOverlay.java (solo drawGrappleIndicator) | Indicatore al mirino | adattare | client/hud | IP. :224-239 → barra del jetpack |

### 2.8 worldgen-loot-data (rischio: medio)

| Classe | Ruolo | Verdetto | Destinazione in Exoframes | Note (file:line) |
|---|---|---|---|---|
| worldgen/StructureStations.java | Tabella di decorazioni per template jigsaw | riscrivere dalle idee | content/worldgen/StructureDecorations (helper in compat/) | IP. Box per chunk :117-162; trasformazione locale :121-122; spawn entità :195-201 |
| mixin/SinglePoolElementMixin.java | Inject a RETURN di `SinglePoolElement.place` | riscrivere dalle idee | content/worldgen/DecoratedPoolElement (preferito) o compat/mixin | :29-37; con `defaultRequire 1` una firma sbagliata crasha |
| loot/AddItemLootModifier.java | GLM con codec item+min/max | riscrivere dalle idee | content/loot/ExoLootInjector + compat/LootHooks | Nessun clamp alla stack size :45-46 |
| entity/SpartanCorpseEntity.java | LivingEntity immobile con 4 armature | riscrivere dalle idee | entity/ExoframeEntity (M3, riusata in M6) | IP. Interazione :67-84; no push :111-118; yaw bloccato :121-138; salvataggio :167-187; `EquipmentSlot.Type.ARMOR` :152, :157 |
| client/model/SpartanCorpseModel.java | PlayerModel in posa seduta | scartare | nessuna (la posa diventa animazione GeckoLib) | :19-33 |
| client/renderer/SpartanCorpseRenderer.java | Renderer del cadavere | scartare | client/render/ExoframeEntityRenderer prende l'idea di seduta | IP. :47-51; nametag soppresso :54-56 |
| client/PlayerModelHider.java | Nasconde la skin | adattare | client/render/PlayerSkinHider (hook in compat/ o platform/) | :45-85 |
| datagen/DataGenerators.java | Datagen vuoto | scartare | nessuna | `Dist.CLIENT` su datagen :14 |
| datagen/ModItemModelProvider.java | Provider vuoto | scartare | nessuna | :19-34 |

### 2.9 compat-build-tools (rischio: basso)

| Classe / file | Ruolo | Verdetto | Destinazione in Exoframes | Note (file:line) |
|---|---|---|---|---|
| compabilities/DotfIntegration.java | Integrazione DotF | scartare | nessuna (idea → Platform.isModLoaded) | IP. Rami VISOR/SUIT identici :94-98; `Float.MAX_VALUE` danno :84 |
| compabilities/GigeresqueIntegration.java | Integrazione Gigeresque | scartare | nessuna | IP. :36-48 |
| mixin/dotf/DotfClientRenderHelperMixin.java | `@Pseudo` DotF | scartare | nessuna | IP. :20-31 |
| mixin/dotf/DotfGeckoRenderPlayerMixin.java | `@Pseudo` DotF | scartare | nessuna | IP. :20-29 |
| mixin/dotf/DotfMeleeArmsMixin.java | `@Redirect` DotF | scartare | nessuna | IP. :19-38 |
| client/DotfSkinHider.java | Ossa DotF | scartare | client/ (solo idea) | IP. Muta un modello condiviso :56-64 |
| build.gradle | ForgeGradle 6 + MixinGradle | scartare | nessuna (checklist per build.gradle.kts) | Remap refmap per Pehkui :85-88; Pehkui :172; repo azuredoom inutile :136 |
| gradle.properties | Versioni e metadati | scartare | nessuna | IP. `mod_license=All Rights Reserved` :53; mappature ufficiali :39-42 |
| settings.gradle | Repo dei plugin | scartare | nessuna | :1-13 |
| resources/mjolnir_armor.mixins.json | Config mixin | adattare | resources/redstone_exoframes.mixins.json | `defaultRequire 1` :21-23 vale anche per i `@Pseudo` |
| resources/META-INF/mods.toml | Metadati Forge | adattare | mods.toml (1.20.1-forge), neoforge.mods.toml, fabric.mod.json | license :12; pehkui :77-82 |
| LICENSE.txt | Licenza di Forge (LGPL 2.1), non del mod | scartare | LICENSE (MIT) + avviso asset ARR | :1-4, :23-28, :66-523 |
| CREDITS.md | Crediti dei suoni Halo Infinite | scartare | nessuna | IP. :5-12 |
| CREDITS.txt | Crediti Forge MDK | scartare | nessuna | :1-64 |
| README.txt | README Forge MDK | scartare | nessuna | :33-39 nota mappature |
| changelog.txt | Changelog di Forge | scartare | nessuna | Non intercettato da .gitignore (il pattern è `forge*changelog.txt`, .gitignore:26) |
| .gitignore | Esclusioni IDE/Gradle + `run-data`, `*.bak`, `__pycache__` | scartare | nessuna (si parte dal .gitignore del template Architectury) | Aggiunto in verifica. `run-data` è escluso ma già tracciato (vedi sotto) |
| .gitattributes | `eol=lf` per `src/generated/**` (MDK) | scartare | nessuna | Aggiunto in verifica. Boilerplate MDK, nessuna cartella `src/generated` |
| gradle/wrapper/gradle-wrapper.properties, gradlew, gradlew.bat | Wrapper Gradle 8.8 | scartare | nessuna (wrapper del template Architectury/Stonecutter) | Aggiunto in verifica. Solo la versione (8.8) come dato |
| release/changelog-1.0.0.md, release/changelog-1.0.1.md | Note di rilascio | scartare | nessuna | Aggiunto in verifica. IP (MJOLNIR, SPARTAN, Halo); utili solo come elenco di funzioni e come forma di changelog |
| release/curseforge-description.md | Descrizione CurseForge | scartare | nessuna | Aggiunto in verifica. IP; chiude con l'avviso "Halo © Microsoft ... non-commercial fan project" (§8) |
| release/icon.png, src/main/resources/logo.png | Icona/logo (render dell'elmo Mark VI) | scartare | nessuna | Aggiunto in verifica. IP (già elencati in §5) |
| run-data/ (config/fml.toml, logs/*.log, *.log.gz) | Output di un'esecuzione di datagen/dev | scartare | nessuna | Aggiunto in verifica. Tracciato in git nonostante `.gitignore`; i log contengono percorsi locali Windows con il nome utente: da non copiare né pubblicare |
| assets/minecraft/atlases/blocks.json | Aggiunge 6 texture d'armatura all'atlante dei blocchi | scartare | nessuna | Aggiunto in verifica. Serve alle icone `forge:obj` che campionano le texture d'armatura; in Exoframes non serve se le icone sono sprite 2D |
| tools/README.md | Indice degli script | riscrivere dalle idee | tools/README.md (solo dev) | IP. Provenienza VMW :12-18 |
| tools/bb2geo.py | Blockbench → geo/animazioni/texture | adattare | tools/assets/bb2geo.py | Percorsi Windows :8-10; `KeyError` su "assemble" :111; solo `data_points[0]` :94 |
| tools/obj2geo.py | OBJ a scatole → cubi GeckoLib con UV per faccia | **riuso diretto** | tools/assets/obj2geo.py | Crash senza riga `o` :27-29 |
| tools/pngio.py | PNG IO senza dipendenze | **riuso diretto** | tools/assets/pngio.py | Solo 8 bit non interlacciati :10 |
| tools/modelbiped2geo.py | Importa ModelBiped decompilati | scartare | nessuna | IP (usato su VMW) :1-2 |
| tools/geo2icons.py | Icone 3D OBJ dalle geo | adattare | tools/assets/geo_mesh.py + geo_icons.py (sprite 2D) | IP nei set :100-108; `forge:obj` :112 non va su Fabric; `cube_faces` :36-53 |
| tools/mod_icon.py | Rasterizzatore software per icona/logo | adattare | tools/assets/render_geo.py | IP: elmo e sfondo UNSC :18-21, :86-106; motore :34-84 |
| tools/hud_textures.py | HUD derivati da VMW | scartare | nessuna | IP. :1-18 |
| tools/make_small.py | Copia ridotta del bbmodel stazione | scartare | nessuna (idea: scalare al render) | IP. :1-6 |
| tools/side_arms.py | IK delle braccia della stazione | riscrivere dalle idee | tools/assets/ (generatore IK per la stazione) | IP. Convenzioni di segno GeckoLib :34-36; IK :46-73 |
| tools/retime_back_arm.py | Ritiming keyframe | scartare | nessuna | Backup mai scritto :65-66 |
| tools/add_arm_clamps.py | Aggiunge bone/cubi a un bbmodel | riscrivere dalle idee | tools/assets/bbmodel_edit.py | IP. Helper box-UV :41-54 |
| tools/remove_foot_clamps.py | Rimuove gruppi | scartare | nessuna | Sovrascrive senza backup :20 |
| tools/halo2geo.py | Set Spartan IV da OBJ catturato | scartare | nessuna (idee → atlas.py) | IP. `region()` :40-53; import circolare :210 |
| tools/mkvi2geo.py | Set MK VI da modello VMW decompilato | scartare | nessuna (idea → damage_states.py) | IP. `crack()` definita a :59 e chiamata a :291 per `mjolnir_helmet_broken.png`; il file ha 291 righe, quindi la citazione :306-326 di client-render è errata |
| tools/locke2geo.py | Armatura Locke + atlas procedurale | scartare | nessuna (idee → procedural_placeholder.py) | IP. Packer e pittori :186-258 |
| tools/corpse_skin.py | Skin del cadavere | scartare | nessuna | IP. :20-28 |
| tools/grapple_hook_texture.py | Texture del gancio | scartare | nessuna | IP. :15-24 |

### 2.10 Verdetti discordanti tra lettori e decisione consolidata

| Classe | Verdetti dei lettori | Decisione consolidata |
|---|---|---|
| MjolnirArmor.java | riscrivere (core-registration, assembly-station), adattare (armor-items, client-render), scartare (hud) | **Riscrivere dalle idee**: si tiene la forma delle registrazioni, nessuna riga. |
| MjolnirConfig.java | riscrivere vs scartare | **Riscrivere dalle idee** in core/ (Gson); ForgeConfigSpec scartato. |
| SlotMixin.java | riuso diretto (core) vs adattare (armor-items, shield-effects) | **Adattare**: stesso target, ma predicato sui segnaposto, slot 39 incluso, più guardie per swap, `/clear` e morte. |
| ThrownPotionMixin / AreaEffectCloudMixin | adattare (core) vs scartare (shield-effects) | **Scartare** per l'alpha (il sigillo dell'elmo non è nel design); tecnica documentata per un eventuale respiratore anti-gas. |
| EntityTurnMixin.java | riuso diretto vs adattare (station) | **Riuso diretto** (cambia solo il predicato). |
| SinglePoolElementMixin.java | scartare (core) vs riscrivere (worldgen) | **Riscrivere dalle idee** come tipo di `StructurePoolElement` registrato (decisione in M6). |
| SpartanAugmentation.java | riscrivere (core) vs scartare (altri tre) | **Scartare** il file, estrarre le tecniche (id fissi, transienti, resync). |
| AddItemLootModifier.java | riscrivere vs scartare | **Riscrivere dalle idee** con `LootEvent`. |
| PlayerScaleCommand.java | riscrivere vs scartare | **Riscrivere dalle idee** come `/exoframe` di debug. |
| ArmorEffectsHandler.java | riscrivere (core, shield) vs scartare (hud) | **Riscrivere dalle idee** (niente caduta, categorie di danno via tag). |
| MjolnirArmorItem / MjolnirHelmetItem | adattare vs riscrivere | **Riscrivere dalle idee**: i segnaposto non sono GeoItem; lo stato rotto va in core/. |
| ModSounds.java | adattare vs scartare | **Adattare** l'helper; nessun suono portato. |
| AugmentationSyncPacket / GrapplePacket | riscrivere (core) vs scartare (hud, shield) | **Scartare** i file; la forma vive in `Net`. |
| ShieldUpdatePacket / ShieldCapability | riscrivere vs adattare | **Riscrivere dalle idee** come payload HudSync e EnergyBuffer in core/. |
| AssemblyInputLock.java | adattare (client-render), scartare (hud), riscrivere (station) | **Riscrivere dalle idee** come ExoframeInputLock, separando i suoni. |
| VisorTracker.java | riscrivere vs adattare | **Adattare** il meccanismo, nuovo predicato (scanner). |
| MjolnirGeoArmorRenderer.java | riscrivere (armor-items) vs adattare (client-render) | **Riscrivere dalle idee**, tenendo `renderFirstPersonArm` come ricetta da ri-verificare. |
| MjolnirHelmetItemRenderer.java | adattare (armor-items) vs scartare (client-render: codice morto) | **Scartare** il file; GeoItemRenderer resta la via per icone 3D se decise. |
| MjolnirArmorModel.java | adattare vs riscrivere | **Riscrivere dalle idee**: un GeoModel parametrico. |
| ArmorAssemblyBlockRenderer.java | riscrivere (client-render) vs adattare (station) | **Adattare** (scala e ossa nascoste). |
| StructureStations.java | scartare (core, station) vs riscrivere (worldgen) | **Riscrivere dalle idee** in M6. |
| SpartanCorpseRenderer.java | riscrivere (client-render) vs scartare (worldgen) | **Scartare**: l'entità telaio usa un GeoEntityRenderer. |
| GrappleClient / Grappleshot / GrappleHookEntity | rispettivamente riscrivere/scartare (hud) vs adattare/riscrivere (grapple) | **Scartare** i file; la fisica di `pull()` va in core/ per il jetpack (M5). |
| MjolnirHudOverlay.java | riscrivere (hud) vs adattare (grapple, solo indicatore) | **Riscrivere dalle idee**. |

---

## 3. Hook specifici di Forge da astrarre

| Hook Forge | Dove è usato | Equivalente Architectury / Fabric API / NeoForge | Note |
|---|---|---|---|
| `@Mod` + costruttore `FMLJavaModLoadingContext`; `@Mod.EventBusSubscriber` | MjolnirArmor.java:85, :227-229; tutti gli handler | Architectury: `ExoframesMod.init()` chiamato da Fabric `ModInitializer`, Forge `@Mod`, NeoForge `@Mod(IEventBus, ModContainer)`; eventi registrati da codice | Solo in platform/; client in un entrypoint client separato (MjolnirArmor.java:297-328 importa classi client nel common) |
| `DeferredRegister` / `RegistryObject` / `ForgeRegistries` | MjolnirArmor.java:104-192, ModSounds.java:11-37 | Architectury `DeferredRegister.create(MOD_ID, Registries.X)` + `RegistrySupplier`; Fabric `Registry.register`; NeoForge `DeferredRegister`/`DeferredHolder` | Su Fabric la registrazione è immediata nell'ordine di chiamata: BLOCKS prima di ITEMS/BE (MjolnirArmor.java:234-247) |
| `CreativeModeTab.builder()` senza titolo | MjolnirArmor.java:195-225 | Architectury `CreativeTabRegistry.create(title, icon)`; Fabric `FabricItemGroup.builder()` | Il titolo va passato esplicitamente |
| `SimpleChannel`, `NetworkEvent.Context`, `PacketDistributor.PLAYER` | MjolnirArmor.java:96-101, :259-269; ShieldEventHandler.java:82-85; tutti i pacchetti | Architectury `NetworkManager` (9.2: `registerReceiver` + FriendlyByteBuf; 13.0: `registerS2CPayloadType` + StreamCodec; `sendToPlayer`); Fabric `PayloadTypeRegistry` + `ServerPlayNetworking`/`ClientPlayNetworking`; NeoForge `RegisterPayloadHandlersEvent` | Pacchetti tipizzati per direzione; versione del canale intenzionale (MjolnirArmor.java:96-101 confronto stretto) |
| `ForgeConfigSpec` + `registerConfig(COMMON)` | MjolnirConfig.java; MjolnirArmor.java:243 | NeoForge `ModConfigSpec`; Fabric nessuno → Gson in `Platform.getConfigFolder()` (design §9) | Valori che servono all'HUD vanno sincronizzati (COMMON non lo è) |
| `IForgeMenuType.create`, `NetworkHooks.openScreen` | MjolnirArmor.java:180-181; ArmorAssemblyBlock.java:103; ArmorAssemblyPartBlock.java:71 | Architectury `MenuRegistry.ofExtended` / `openExtendedMenu`; Fabric `ExtendedScreenHandlerType`; NeoForge `IMenuTypeExtension.create` | Via Architectury gli extra data restano `FriendlyByteBuf` su entrambe le versioni: `ExtendedMenuProvider.saveExtraData(FriendlyByteBuf)` e `ExtendedMenuTypeFactory.create(int, Inventory, FriendlyByteBuf)` sia in 9.2.14 sia in 13.0.11 (verificato con javap). RegistryFriendlyByteBuf/StreamCodec servono solo usando direttamente le API Fabric/NeoForge 1.21.1 |
| `FMLClientSetupEvent` + `MenuScreens.register` | MjolnirArmor.java:303-304 | Architectury `MenuRegistry.registerScreenFactory`; NeoForge `RegisterMenuScreensEvent` | — |
| `ItemProperties.register` (in setup client) | MjolnirArmor.java:305-310 | Architectury `ItemPropertiesRegistry.register` (vanilla funziona anche su Fabric) | Esiste ancora su 1.21.1 |
| `EntityRenderersEvent.RegisterRenderers` / `RegisterLayerDefinitions` | MjolnirArmor.java:314-326 | Architectury `EntityRendererRegistry`, `BlockEntityRendererRegistry`, `EntityModelLayerRegistry` | — |
| `EntityAttributeCreationEvent` | MjolnirArmor.java:245, :272-275 | Architectury `EntityAttributeRegistry.register`; Fabric `FabricDefaultAttributeRegistry` | Per ExoframeEntity |
| `RegisterCommandsEvent` | PlayerScaleCommand.java:28-32 | Architectury `CommandRegistrationEvent.EVENT`; Fabric `CommandRegistrationCallback` | Il codice Brigadier è identico sulle due versioni |
| `RegisterKeyMappingsEvent` | GrappleClient.java:150-156 | Architectury `KeyMappingRegistry.register`; Fabric `KeyBindingHelper` | Tasti V/R/G/N/H |
| `TickEvent.PlayerTickEvent` | ShieldEventHandler.java:70-88; SpartanAugmentation.java:253-268; GigeresqueIntegration.java:16-19 | Architectury `TickEvent.PLAYER_POST`; NeoForge `PlayerTickEvent.Post` | Mjolnir dimentica il filtro di fase (doppio tick) |
| `TickEvent.ClientTickEvent` | AssemblyInputLock.java:21-37; GrappleClient.java:71 | Architectury `ClientTickEvent.CLIENT_POST`; Fabric `ClientTickEvents.END_CLIENT_TICK`; NeoForge `ClientTickEvent.Post` | Mappare con cura Pre/Post (GrappleClient divide la logica tra START e END) |
| `MovementInputUpdateEvent` | AssemblyInputLock.java:39-48 | NeoForge stesso evento; Fabric/Architectury nessuno → mixin a TAIL di `KeyboardInput.tick` | Campi `Input` invariati fino a 1.21.1 |
| `LivingAttackEvent` (cancel) | ArmorEffectsHandler.java:15-25 | Architectury `EntityEvent.LIVING_HURT` (solo cancel); NeoForge `LivingIncomingDamageEvent`; Fabric `ServerLivingEntityEvents.ALLOW_DAMAGE` | Basta per caduta e scudo ad assorbimento totale |
| `LivingDamageEvent.setAmount` (assorbimento parziale) | ShieldEventHandler.java:49-67 | Forge 1.20.1 stesso; NeoForge `LivingDamageEvent.Pre.setNewDamage` o `LivingIncomingDamageEvent.setAmount`; Fabric: solo mixin `@ModifyVariable/@ModifyArg`; Architectury: nessuno | Serve per protezione 50% a energia zero e ripartizione per pezzo se modifica l'importo |
| `LivingFallEvent` | ArmorEffectsHandler.java:27-33; SpartanAugmentation.java:364-369 | Cancel in `LIVING_HURT` con `DamageTypeTags.IS_FALL`; NeoForge `LivingFallEvent`; Fabric mixin su `causeFallDamage` | L'onda d'urto (>6 blocchi) richiede un hook sull'atterraggio che Mjolnir non ha |
| `LivingHurtEvent`, `MobEffectEvent.Applicable` | SpartanAugmentation.java:346, :354-359; DotfIntegration.java:73-112 | NeoForge stessi nomi (Result.DO_NOT_APPLY); Fabric mixin `LivingEntity.canBeAffected`; Architectury nessuno | ShieldEventHandler.java:94-107 rimuove gli effetti dopo l'applicazione: non copiare |
| `PlayerEvent.Clone` / `PlayerLoggedInEvent` / `PlayerChangedDimensionEvent` | ShieldEventHandler.java:40-46; SpartanAugmentation.java:458-473 | Architectury `PlayerEvent.PLAYER_CLONE` / `PLAYER_JOIN` / `CHANGE_DIMENSION`; attachment `copyOnDeath` | Exoframes: alla morte il telaio diventa entità e lo stato si azzera |
| `LivingEntityUseItemEvent.Start` | HelmetSealLock.java:39-48 | Architectury `InteractionEvent.RIGHT_CLICK_ITEM`; Fabric `UseItemCallback` | Non serve all'alpha |
| `PlayerInteractEvent.*`, `AttackEntityEvent`, `ItemTossEvent` | AssemblyInteractionLock.java:24-55 | Architectury `InteractionEvent.LEFT_CLICK_BLOCK/RIGHT_CLICK_BLOCK/RIGHT_CLICK_ITEM/INTERACT_ENTITY`, `PlayerEvent.ATTACK_ENTITY`, `PlayerEvent.DROP_ITEM`; Fabric `AttackBlockCallback`, `UseBlockCallback`, `UseItemCallback`, `UseEntityCallback`, `AttackEntityCallback` | Verificare che `DROP_ITEM` annulli davvero su Fabric |
| `AttachCapabilitiesEvent`, `CapabilityManager`, `LazyOptional`, `@AutoRegisterCapability` | ShieldEventHandler.java:31-37; ShieldProvider.java; ShieldCapability.java:14 | NeoForge `AttachmentType` (serialize(Codec), copyOnDeath, sync verificati in 21.1.266); Fabric `fabric-data-attachment-api` (1.0.2 su 1.20.1 senza sync, 1.4.7 su 1.21.1); Forge 1.20.1 capability dietro wrapper; Architectury nessuno | Un'unica interfaccia `FrameStateAccess` in compat/, implementazioni in platform/ |
| `Entity.getPersistentData` / `PERSISTED_NBT_TAG` | SpartanAugmentation.java:98, :147-151, :241-249; Grappleshot.java:49, :66 | Attachment (sopra) | Assente su Fabric |
| `ItemStackHandler`, `ForgeCapabilities.ITEM_HANDLER`, `SlotItemHandler` | ArmorAssemblyBlockEntity.java:39-42, :82-98, :440-452; ArmorAssemblyMenu.java:13-15 | Vanilla `SimpleContainer` + `ContainerHelper`; esposizione a Transfer API/capability solo in platform/ | Un'implementazione per tutti i loader |
| `IForgeItem.canEquip` | MjolnirArmorItem.java:38-41 | NeoForge `IItemExtension.canEquip`; Fabric nessuno | I segnaposto si mettono solo via `setItemSlot` + SlotMixin |
| `IForgeItem.damageItem` | MjolnirHelmetItem.java:38-44 | NeoForge `IItemExtension.damageItem`; Fabric mixin su `hurtAndBreak` | Evitato tenendo la durabilità in core/ |
| `IForgeItem.initializeClient` / `IClientItemExtensions` | sei classi item (MjolnirArmorItem.java:51-67 ecc.) | NeoForge `RegisterClientExtensionsEvent`; GeckoLib 4.8.4 Fabric `RenderProvider` (astratto); GeckoLib 4.9.3 `createGeoRenderer(Consumer<GeoRenderProvider>)`; Fabric API `ArmorRenderer` | Evitato con il layer GeckoLib sul giocatore (§5.4) |
| `IForgeItem.getArmorTexture` | MjolnirArmorItem.java:69-74 | NeoForge con `ArmorMaterial.Layer`; Fabric nessuno → texture `_layer_1/2` trasparenti | Nessun hook se le texture vanilla sono trasparenti |
| `IForgeBlock.getCloneItemStack(state, HitResult, ...)` | ArmorAssemblyPartBlock.java:77-84 | `getCloneItemStack` vanilla (BlockGetter su 1.20.1, LevelReader su 1.21.1) | La versione Forge non è chiamata su Fabric |
| `IForgeBlockEntity.getRenderBoundingBox` | ArmorAssemblyBlockEntity.java:491-494 | `BlockEntityRenderer.shouldRenderOffScreen` = true; NeoForge 1.21 sul renderer | Ignorato in silenzio su Fabric |
| `RenderGuiOverlayEvent.Post` filtrato su HOTBAR | MjolnirHudOverlay.java:161-167 | Architectury `ClientGuiEvent.RENDER_HUD` (9.2: float; 13.0: DeltaTracker); Fabric `HudRenderCallback`; NeoForge `RegisterGuiLayersEvent`; Forge `RegisterGuiOverlaysEvent` | Un solo callback per frame; controllare `hideGui` a mano |
| `RenderPlayerEvent.Pre/Post` | PlayerModelHider.java:34-42 | NeoForge stesso; Fabric mixin su `PlayerRenderer.render` o TAIL di `PlayerModel.setupAnim` | La scala della testa va ripristinata comunque |
| `RenderArmEvent` | FirstPersonArmorArms.java:42-54 | NeoForge stesso; Fabric mixin su `PlayerRenderer.renderHand` (stessa firma 1.20.1/1.21.1) | Solo se le braccia in prima persona entrano nell'alpha |
| `RenderHandEvent` | GrappleFirstPerson.java:21, :62 | NeoForge stesso; Fabric mixin su `ItemInHandRenderer` | Scartato col rampino |
| GLM: `LootModifier`, `GLOBAL_LOOT_MODIFIER_SERIALIZERS`, `data/forge/loot_modifiers` | AddItemLootModifier.java; MjolnirArmor.java:183-184 | Architectury `LootEvent.MODIFY_LOOT_TABLE`; NeoForge GLM con MapCodec in `data/neoforge`; Fabric `LootTableEvents` | Un solo percorso config-driven per i quattro jar |
| `MissingMappingsEvent` | MjolnirArmor.java:277-294 | NeoForge esiste; Fabric nessuno | Congelare gli id prima dell'alpha |
| `ModList.get().isLoaded` | DotfIntegration.java:51-54 | Architectury `Platform.isModLoaded` | In `compat/Platform` |
| `ForgeEventFactory.onProjectileImpact` | GrappleHookEntity.java:135 | NeoForge `EventHooks.onProjectileImpact`; Fabric nessuno | Scartato |
| `GatherDataEvent`, `ItemModelProvider` | DataGenerators.java; ModItemModelProvider.java | NeoForge stesso; Fabric `DataGeneratorEntrypoint` | Strategia di datagen da decidere (§9 del design) |
| `EntityMountEvent` | DotfIntegration.java:74-85 | NeoForge stesso; Fabric nessuno | Scartato |
| `net.minecraftforge.common.util.Lazy` | ModArmorMaterials.java:11-31 | Guava `Suppliers.memoize` | — |
| `forge/biome_modifier`, tag `forge:` | data/forge/** | NeoForge biome modifier; tag comuni `c:` su 1.21.1 | Contenuto titanio scartato |
| `GeckoLib.initialize()` | MjolnirArmor.java:249 | Presente sulla linea 1.20.1, assente in 4.9.3 (armor-items, verificato) | core-registration lo lascia come domanda: si chiude con il dato verificato |

---

## 4. API vanilla che cambiano tra 1.20.1 e 1.21.1 toccate da questo codice

Le interfacce di §5.2 sono `ItemDataAccess`, `ArmorMaterials`, `Net`, `Ids`, `Platform`. Le righe marcate **(nuova)** propongono un'interfaccia o classe base aggiuntiva in `compat/`.

| API | Classi coinvolte | Interfaccia compat/ che le deve coprire |
|---|---|---|
| `new ResourceLocation(ns, path)` → `ResourceLocation.fromNamespaceAndPath` | Ovunque (MjolnirArmor.java:97, :147, :152, :306; ArmorVariant.java:62; ModSounds.java:36; GeoModel; MjolnirHudOverlay.java:146-150) | Ids |
| NBT sull'ItemStack (`getTag`, `getOrCreateTag`, `setTag`) → data components | ArmorVariant.java:35-52; MjolnirArmorRenderer2 | ItemDataAccess |
| `ItemStack.save(CompoundTag)` / `ItemStack.of` → `save(HolderLookup.Provider)` / `parseOptional` | SpartanCorpseEntity.java:171, :182; ArmorAssemblyBlockEntity.java:467-487 | ItemDataAccess |
| `Item.Properties.durability(-1)`: su 1.20.1 item non danneggiabile; su 1.21.1 imposta i componenti `MAX_DAMAGE=-1`, `DAMAGE=0`, `MAX_STACK_SIZE=1` e `isDamageableItem()` resta vero (solo `has(MAX_DAMAGE) && !has(UNBREAKABLE) && has(DAMAGE)`; verificato con javap), quindi l'item si comporta come già al limite della durabilità (lettura di armor-items). Il codec `POSITIVE_INT` (core-registration) scatterebbe solo serializzando il componente, cosa che per i default del prototipo non avviene: non verificato a runtime. In ogni caso non va usato | MjolnirArmor.java:109-130 | ArmorMaterials (+ regola: i segnaposto non hanno durabilità, o UNBREAKABLE via ItemDataAccess) |
| `ItemStack.isDamageableItem` (maxDamage>0 vs presenza dei componenti) | MjolnirHelmetItem.java:32-36 | ItemDataAccess (lo stato rotto vive in core/) |
| `ArmorMaterial` enum → record registrato `Holder<ArmorMaterial>` con layer; `ArmorItem(Holder, Type, Properties)` | ModArmorMaterials.java:45-81; tutte le classi item | ArmorMaterials |
| `ArmorMaterial.getName()` con namespace: rompe `HumanoidArmorLayer` su Fabric 1.20.1 | ModArmorMaterials.java:68-71 | ArmorMaterials |
| `FriendlyByteBuf` encode/decode → `CustomPacketPayload` + `StreamCodec` | Tutti i pacchetti | Net |
| Extra data dei menu: con Architectury restano `FriendlyByteBuf` su 1.20.1 e 1.21.1 (verificato su 9.2.14 e 13.0.11); il cambio a RegistryFriendlyByteBuf/StreamCodec riguarda solo le API dirette dei loader | ArmorAssemblyMenu.java:30 | nessuna nuova interfaccia se si usa `MenuRegistry` di Architectury; Platform (apertura menu) |
| `AttributeModifier(UUID, String, double, Operation)` → `(ResourceLocation, double, Operation)`; `ADDITION/MULTIPLY_TOTAL` → `ADD_VALUE/ADD_MULTIPLIED_TOTAL`; `removeModifier(UUID)` → `(ResourceLocation)`; inoltre `LivingEntity.getAttribute(Attribute)` → `getAttribute(Holder<Attribute>)` e le costanti `Attributes.*` diventano `Holder` (tutto verificato con javap) | SpartanAugmentation.java:71-85, :384-451 | **(nuova)** `Attributes` (velocità −15%, knockback, servomotori, energia zero −70%) |
| `CustomRecipe(ResourceLocation, CraftingBookCategory)` → senza id; `CraftingContainer` → `CraftingInput`; `assemble(..., RegistryAccess)` → `HolderLookup.Provider`; `SimpleCraftingRecipeSerializer` | ArmorVariantRecipe.java:23-61 | **(nuova)** `Recipes` (solo se resta una ricetta da banco) |
| `Item.getUseDuration(ItemStack)` → `(ItemStack, LivingEntity)`; `appendHoverText(..., Level, ...)` → `Item.TooltipContext` | SpartanSerumItem.java:97-118; MjolnirArmorItem2.java:30-34 | **(nuova)** classe base `compat/CompatItem` |
| `DamageSource.isIndirect()` solo su 1.20.1; su 1.21.1 esiste `isDirect()` con logica invertita (entrambi verificati con javap in verifica; hud diceva "esiste ancora", errato); `getMsgId()` | ArmorEffectsHandler.java:18-21 | **(nuova)** `DamageClassifier` (tag `DamageTypeTags` su entrambe le versioni) |
| `MobEffect` → `Holder<MobEffect>` in `hasEffect`/`removeEffect`/chiavi | ShieldEventHandler.java:95-105; DotfIntegration.java:91-92 | **(nuova)** `Effects` (o dentro DamageClassifier) |
| `Block.use(...)` → `useWithoutItem` / `useItemOn` | ArmorAssemblyBlock.java:100; ArmorAssemblyPartBlock.java:66 | **(nuova)** classe base `compat/CompatEntityBlock` |
| `saveAdditional/load(CompoundTag)` → con `HolderLookup.Provider` / `loadAdditional`; `getUpdateTag()` → `(Provider)`; `NbtUtils.writeBlockPos/readBlockPos` | ArmorAssemblyBlockEntity.java:251, :467-487; ArmorAssemblyPartBlockEntity.java:39-50 | **(nuova)** classe base `compat/CompatBlockEntity` |
| `BlockEntity.load` per cartelli/stendardi; `Component.Serializer.toJson(Component)` → con Provider | StructureStations.java:165-225 | **(nuova)** helper worldgen in compat/ (M6) |
| `Screen.renderBackground(GuiGraphics)` → `(GuiGraphics, int, int, float)` e doppio sfondo su 1.21 | ArmorAssemblyScreen.java:25 | **(nuova)** `compat/client/CompatContainerScreen` |
| `InventoryScreen.renderEntityInInventory` (firma diversa) | anteprima 3D della stazione (nessun precedente in Mjolnir) | `compat/client` (nuova, con la precedente) |
| `Model.renderToBuffer(..., float r,g,b,a)` → `int color`; `ModelPart.render` idem; GeckoLib `GeoArmorRenderer/GeoBlockRenderer.preRender` colore → int | MjolnirGeoArmorRenderer.java:38-46; ArmorAssemblyBlockRenderer.java:21-23; DotfMeleeArmsMixin | **(nuova)** `compat/client` basi GeckoLib (`CompatGeoBlockRenderer`, renderer del layer) |
| Package GeckoLib `software.bernie.geckolib.core.*` (4.4.9/4.8.4) → radice (4.9.3); id degli stack GeoItem in componente | MjolnirArmorItem.java:24-27; MjolnirHelmetItem.java:11-15; ArmorAssemblyBlockEntity.java:45-52 | **(nuova)** `compat/client` + basi GeckoLib (non vanilla, ma stessa disciplina) |
| `Minecraft.getFrameTime()` / `getDeltaFrameTime()` → `getTimer()` (DeltaTracker) | AssemblyPose.java:33; GrappleClient (armProgress) | **(nuova)** `compat/client/ClientTiming` |
| `Tesselator.getBuilder()+begin`, `vertex().color().endVertex()`, `BufferUploader.drawWithShader(b.end())` → `begin` che ritorna il builder, `addVertex/setColor`, `buildOrThrow` | MjolnirHudOverlay.java:264-269, :386; GrappleHookRenderer.java:94-95 | **(nuova)** `compat/client/QuadEmitter` (o barre fatte con `GuiGraphics.fill`, identico sulle due versioni) |
| Callback HUD di Architectury: `(GuiGraphics, float)` → `(GuiGraphics, DeltaTracker)` | MjolnirHudOverlay.java:161-167 | Platform (registrazione HUD) |
| `SynchedEntityData`: `defineSynchedData()` → `defineSynchedData(Builder)` | GrappleHookEntity.java:68-72; futura ExoframeEntity | **(nuova)** base `compat/CompatEntity` |
| `LivingEntityRenderer.setupRotations` con parametro `scale` | SpartanCorpseRenderer.java:47 | compat/client |
| `EquipmentSlot.Type.ARMOR` → `HUMANOID_ARMOR` | SpartanCorpseEntity.java:152, :157 | Ids (costanti) o CompatEntity |
| `SinglePoolElement.place` con `LiquidSettings` in più | SinglePoolElementMixin.java:30-33 | compat/mixin (Stonecutter o MixinExtras) |
| `ThrownPotion.applySplash(List)` → `(Iterable)` | ThrownPotionMixin.java:18 | compat/mixin (selettore per nome) |
| `DropExperienceBlock(Properties, UniformInt)` → `(IntProvider, Properties)` | MjolnirArmor.java:162-165 | nessuna (Exoframes non ha minerali) |
| Id delle loot table `ResourceLocation` → `ResourceKey<LootTable>`; condizione `forge:loot_table_id` → `neoforge:` | AddItemLootModifier; data/forge/loot_modifiers | **(nuova)** `LootHooks` |
| Cartelle dati `structures/ loot_tables/ recipes/ tags/items tags/blocks` → singolari su 1.21; tag `forge:` → `c:` | tutto `data/` | build per variante (rinomina in processResources), non codice |
| `pack_format` 15 → 34/48 | pack.mcmeta:6 | proprietà Stonecutter |
| Tag `minecraft:no_knockback` esiste solo su 1.21.1 | data/minecraft/tags/damage_type/no_knockback.json | dati per variante |
| `compatibilityLevel` JAVA_17 → JAVA_21 | mjolnir_armor.mixins.json | mixins.json per variante |
| `new AABB(BlockPos)`: presente su 1.21.1 (verificato con javap); sparisce invece `new AABB(BlockPos, BlockPos)` (→ `AABB.encapsulatingFullBlocks`), non usato da Mjolnir | ArmorAssemblyStructure.java:52, :58 | nessuna |
| `BlockEntityType.Builder.build(null)` valido fino a 1.21.1 | MjolnirArmor.java:174, :192 | nessuna (fino a 1.21.1) |
| Stabili 1.20.1→1.21.1 (verificato dai lettori): `Slot.mayPickup`, `Entity.turn`, `Minecraft.shouldEntityAppearGlowing`, `HumanoidModel.setupAnim(LivingEntity,FFFFF)`, campi di `Input`, `KeyMapping(String,int,String)`, `SoundEvent.createVariableRangeEvent`, Brigadier, `GuiGraphics.fill/blit`, `ItemProperties.register` | mixin e client | nessuna |

---

## 5. Contenuti legati all'IP da NON portare, e pipeline riusabile

**Da non portare (né nel repo né nella sua storia git)**

- **Nomi e testi**: Mjolnir/MJOLNIR, Spartan, SPARTAN-II/III/IV, MK VI/Mark VI, "117", Spartan IV, Locke, HUNTER, ODST, UNSC, Grappleshot/Quickshot, Flood, Fallen Spartan, "MJOLNIR online", ASTER/CHRYSANTHEMUM/ORCHID e nomi delle procedure (SpartanProgram.java:17-38), tutto `lang/en_us.json` (si può riprendere lo schema delle chiavi `message./tooltip./subtitles.`), id come `unsc_mjolnir_lab`, `spartan_corpse`, `spartan_*_serum`; anche titanio e i relativi tag/ricette (non IP ma fuori dal design).
- **Geometrie e animazioni**: `geo/mjolnir_{armor,helmet}*.geo.json` (MK VI dal modello decompilato di **Vic's Modern Warfare** 1.11.7.7, tools/mkvi2geo.py:1-3; Spartan IV da `halo_armor.obj` **di provenienza ignota**, "Drive", cattura `armor.soldier.halo`, tools/halo2geo.py:1, :26-28; Locke da render **Halopedia/Halo 5**, tools/locke2geo.py:1-9; petto e Grappleshot di **Halo Infinite**, tools/mkvi2geo.py:81-91, :107-113); `geo/armor_assembly_*.geo.json` e `animations/armor_assembly_*.animation.json` (stazione ispirata alla scena di Halo 4); modelli `models/obj/*.obj` e `models/item/*` generati dalle geo.
- **Texture**: `textures/armor/*.png` (18 file), `textures/gui/mjolnir_hud*.png` (HUD di VMW, `mjolnir_hud.png` = `spartan.png` di VMW tale e quale, tools/hud_textures.py:1-10), `textures/gui/shield_bar*.png` e `mjolnir_helmet_icon.png` (inutilizzati), `textures/gui/armor_assembly.png`, `textures/block/armor_assembly_block.png`, `textures/entity/spartan_corpse.png`, `textures/entity/grapple_hook.png`, `textures/item/*serum*`, `logo.png`, `release/icon.png` (render dell'elmo Mark VI).
- **Suoni**: `shield_hit1-4`, `shield_down`, `shield_warning`, `shield_recharge` sono di **Halo Infinite** (CREDITS.md:5-9); gli altri sono dichiarati "sintetizzati da make_sounds.py", script assente dal repo: provenienza non verificabile, si rigenera tutto.
- **Dati derivati dall'arte**: costanti della barra HUD misurate su texture Halo e su uno screenshot di Halo 5 (MjolnirHudOverlay.java:76-101, :133-143); costanti SHOULDER/MUZZLE dal modello Halo (GrappleFirstPerson.java:53-54); numeri del rampino presi da Infinite (40 blocchi, 60 tick).
- **Strutture e loot**: `structures/unsc_mjolnir_lab.nbt`, i JSON della struttura, `loot_tables/chests/mjolnir_lab*.json`, i 15 GLM (anche quelli su `dotf:chests/*`), il damage type `augmentation` e le aggiunte ai tag vanilla.
- **Integrazioni**: Dawn of the Flood (Halo Flood), Gigeresque (Alien).
- **Altri gruppi di risorse (aggiunti in verifica, conteggi dal tree)**: `assets/mjolnir_armor/blockstates/` (5: stazioni, parte, minerali di titanio) e `models/block/` (3), `models/item/` (33), `models/obj/` (13), `geo/` (8), `animations/` (4), `sounds/` (16 .ogg + `sounds.json`), `textures/gui/` (8), `textures/item/` (7), `lang/en_us.json`; `assets/minecraft/atlases/blocks.json` (aggiunge 6 texture d'armatura all'atlante blocchi per le icone `forge:obj`); contenuto titanio: `worldgen/configured_feature/` e `placed_feature/` (3+3 `ore_titanium*`), `forge/biome_modifier/` (1), `data/forge/tags/{blocks,items}/**` (minerali, lingotti, grezzi), `data/minecraft/tags/blocks/mineable/`, `loot_tables/blocks/` (4), `recipes/` (6 di fusione + `spartan_iv_variant.json`); laboratorio: `worldgen/template_pool/unsc_mjolnir_lab/`, `worldgen/structure/`, `worldgen/structure_set/`, `tags/worldgen/biome/has_structure/`; danno: `damage_type/augmentation.json` più 5 tag vanilla in `data/minecraft/tags/damage_type/`; `data/forge/loot_modifiers/global_loot_modifiers.json` (elenca i 15 GLM, 8 dei quali puntano a forzieri DotF). Tutto IP o fuori dal design: nessun file si porta; si riusano solo le forme (sezione seguente).

**Pipeline riusabile (in `tools/` di Exoframes, solo dev, fuori dai jar)**

| Script | Uso | Modifiche necessarie |
|---|---|---|
| tools/obj2geo.py | OBJ a scatole → cubi GeckoLib con UV per faccia | Riuso diretto; gestire OBJ senza riga `o` e facce senza `vt` (:27-29) |
| tools/pngio.py | PNG IO senza dipendenze | Riuso diretto; limite a 8 bit non interlacciati (:10) |
| tools/bb2geo.py | Blockbench → geo + animazioni (con animazione invertita) + texture | Percorsi da CLI (:8-10); inversione "assemble→disassemble" opzionale (:109-117, `KeyError` a :111); keyframe pre/post e bezier (:94); namespace `redstone_exoframes` |
| tools/geo2icons.py | Matematica dei cubi GeckoLib, display transform automatici | Tenere `cube_faces` (:36-53), `write_obj`, `display` (:78-98); togliere i set Halo (:100-108), `ROOT` (:17) e l'output `forge:obj` (:112); produrre sprite 2D o modelli JSON |
| tools/mod_icon.py | Rasterizzatore software per icona e anteprime della spec asset | Tenere `render()` (:34-84); togliere elmo fisso (:18-21) e riflesso dorato/sfondo UNSC (:86-106) |
| tools/side_arms.py, add_arm_clamps.py | IK → keyframe; aggiunta programmatica di bone/cubi in un bbmodel | Riscrivere per la nuova stazione 2x1; tenere le convenzioni di segno GeckoLib (:34-36) e l'helper box-UV (:41-54); niente scala Pehkui `K` (:15) |
| tools/locke2geo.py, halo2geo.py (idee) | Packer a scaffali, pittori procedurali, bleed di 1 texel, `region()` per assegnare scatole ai bone | Riscrivere come `procedural_placeholder.py`/`atlas.py` per i placeholder grigi del §7.6/7.11 |
| tools/mkvi2geo.py (idea) | Proiezione di crepe in spazio modello | Riscrivere come `damage_states.py` per gli stati danneggiato/critico/rotto (`crack()` a :59, chiamata a :291; la citazione :306-326 era errata: il file ha 291 righe) |
| tools/make_small.py (idea) | Scalare al render, non la geometria, per non rompere le UV box | Solo come regola |
| tools/README.md (forma) | Una riga per script + ordine della pipeline | Testo nuovo, senza riferimenti a VMW/Halo |

Pattern di dati riusabili (riscritti con id nuovi): struttura jigsaw data-only (structure + start pool + structure_set + tag bioma), damage type con aggiunte ai tag, `sounds.json` con varianti e sottotitoli, ricetta speciale `{type, category}`.

---

## 6. Tecniche da copiare e trappole trovate

### 6.1 Tecniche da copiare

**Telaio, slot e stato**
- Lock degli slot armatura con un solo inject HEAD su `Slot.mayPickup` (click, shift-click, tasti numerici, Q, doppio click, anche in creativa): SlotMixin.java:30-36.
- Lock a tre livelli: `canEquip=false` + `use()` che passa (MjolnirArmorItem.java:38-49), SlotMixin, inserimento solo via `player.setItemSlot` che salta `canEquip` (ArmorAssemblyBlockEntity.java:407-415); se lo slot di destinazione è occupato il pezzo cade a terra (:417-426).
- Stato "rotto ma montato" derivato dalla durabilità, così ogni riparazione lo ripristina senza flag da pulire, e clamp che lascia l'ultimo punto: MjolnirHelmetItem.java:32-44 (in Exoframes: funzione pura in core/ con soglie 50%/15%/0).
- Modificatori di attributo a id fisso, rimuovi-poi-aggiungi, transienti riscritti solo se cambia l'importo, sweep di rimozione: SpartanAugmentation.java:70-85, :430-452.
- Resync dello stato su login, clone e cambio dimensione: SpartanAugmentation.java:456-491.
- Config letta all'uso e riapplicata al login: MjolnirConfig.java:5-9.
- "Default = dato assente" perché gli stack di default si impilino con quelli nuovi: ArmorVariant.java:42-52.
- Valori dipendenti dai registri risolti pigramente in un enum: ModArmorMaterials.java:17-21.
- Un enum come unica fonte per i nomi di registrazione: StationVariant.java:21-41 con MjolnirArmor.java:171-178; literal dei comandi da `values()`: PlayerScaleCommand.java:56-64.
- Entità immobile interagibile che custodisce equipaggiamento (yaw bloccato, niente push, rimozione solo in creativa, svestizione uno/tutti con fallback a terra, latch "vestito"): SpartanCorpseEntity.java:38-39, :61-94, :111-138, :167-187.

**Danno ed energia**
- Assorbi-poi-passa: se l'assorbitore copre il colpo annulla, altrimenti drena e imposta il resto: ShieldEventHandler.java:56-64.
- Ricarica gated da "tempo dall'ultimo colpo" con flag di fronte "ricarica iniziata": ShieldCapability.java:47-63 (in Exoframes con tick di gioco persistiti).
- Lo stato emette gli eventi di fronte (colpito/esaurito) in un unico punto: ShieldCapability.java:32-39.
- Invio dello stato solo al giocatore proprietario: ShieldEventHandler.java:81-85.
- Annullare il danno da caduta col telaio: ArmorEffectsHandler.java:27-33 (con `DamageTypeTags.IS_FALL`).
- Damage type data-driven risolto dal registro: SpartanAugmentation.java:278-279 + data/mjolnir_armor/damage_type/augmentation.json (utile per l'onda d'urto).
- Consumabile a durata d'uso con interruzione se la precondizione cambia e suono a metà: SpartanSerumItem.java:47-105 (kit di riparazione, cambio cella di 2 s).

**Rete, tasti, predizione**
- Tasto → un piccolo pacchetto C2S d'intento → il server valida e agisce, senza fidarsi di numeri del client: GrapplePacket.java:26-37; guardie in un'unica funzione server: Grappleshot.java:48.
- Predizione ottimistica con conferma dallo stato sincronizzato e guardia `releaseSent` per non duplicare i pacchetti: GrappleClient.java:38-39, :77-86, :96, :100.
- Controller di velocità con "grip": `v += (voluta − v)·GRIP` più compensazione di gravità 0.08 e `fallDistance=0`: GrappleClient.java:143-148 (spinta del jetpack).
- Cooldown come tick "pronto-a" e frazione per l'HUD: GrappleClient.java:65-68, Grappleshot.java:65-67.
- Stato di gioco come int in `SynchedEntityData`, transizioni guidate dal server: GrappleHookEntity.java:68-72, :125-130.
- Integrazione opzionale riconoscendo id/namespace, `isLoaded` in cache, senza caricare classi: DotfIntegration.java:35-66.

**Client, rendering, HUD**
- Trio posa/lock per momenti scriptati: EntityTurnMixin.java:19-24; AssemblyInputLock.java:21-48; AssemblyPose.java:28-48 (smoothstep :38).
- Inject a TAIL di `HumanoidModel.setupAnim`: l'armatura GeckoLib e gli overlay seguono da soli: HumanoidModelMixin.java:29-40.
- Contorno di entità via `shouldEntityAppearGlowing`: MinecraftGlowMixin.java:15-18 (con predicato in cache per tick).
- Braccio GeckoLib in prima persona: MjolnirGeoArmorRenderer.java:115-136 con il setup vanilla di FirstPersonArmorArms.java:42-54 (da ri-verificare su 4.8.4/4.9.3).
- Nascondere la skin solo durante il render e rimpicciolire la testa a 0.85 sotto un visore rotto: PlayerModelHider.java:45-70, :79-85.
- Texture scelta in `getTextureLocation` dallo stato dello stack: MjolnirHelmetRenderer.java:19-25, MjolnirHelmetRenderer2.java:21-25.
- Nascondere i cubi di un GeoBone tenendo i figli: DotfSkinHider.java:66-73; nascondere sottoalberi: ArmorAssemblyBlockRenderer.java:35-40.
- Pivot di riposo vanilla e conversione di matrici nello spazio GeoArmorRenderer: MjolnirGeoArmorRenderer.java:71-80, :99-101.
- Convenzione dello scheletro geo `bipedX` (vuoto, pivot vanilla) → `armorX` (cubi), stivali separati: assets/mjolnir_armor/geo/mjolnir_armor.geo.json.
- Restituire una texture vera dall'hook vanilla e nessun controller vuoto per evitare spam di log: MjolnirArmorItem.java:69-80.
- HUD in spazio di riferimento 1280×720 con letterbox o stretch: MjolnirHudOverlay.java:184-200; barre in coordinate float: :248-265, :377-387; scomposizione in trapezi: :342-375; drenaggio simmetrico: :106-131, :292-336; rampa di colori: :389-405; glow di ricarica 350 ms: :241-258, :283-286; impulso a zero: :274-278; dati sopra la cornice: :212-214.
- Suono UI sul fronte di salita e beep periodico: HelmetSounds.java:19-41.
- Proiettare un punto del braccio in prima persona in NDC e riportarlo nel mondo: GrappleFirstPerson.java:104-111, :120-125.

**Stazione e mondo**
- Rifiutare il piazzamento se una cella non è sostituibile: ArmorAssemblyBlock.java:75-83; controller che scrive la sua posizione nelle parti: :86-96 + ArmorAssemblyPartBlockEntity.java:27-30; cascata simmetrica di rottura: ArmorAssemblyBlock.java:111-127 + ArmorAssemblyPartBlock.java:54-62; click su qualunque cella apre il controller: :66-74.
- Rotazione locale→mondo allineata a GeoBlockRenderer: ArmorAssemblyStructure.java:23-31.
- Sync minimo via update tag con un solo int: ArmorAssemblyBlockEntity.java:243-255; animazioni one-shot da server con `triggerAnim`: :199, :498-504; eventi e suoni su una tabella di tick allineata ai keyframe: :71-74, :161-176, :266-274; azioni solo per il giocatore che ha avviato il ciclo: :128-133, :161-167.
- Compensazione della scala prima di GeoBlockRenderer (`0.5·(1−s)`): ArmorAssemblyBlockRenderer.java:24-33.
- Decorazione di strutture chunk-safe con `box.isInside` e trasformazione locale→mondo: StructureStations.java:117-162, :121-122; spawn di entità in worldgen: :195-201; contenuti dei BE senza livello: :139-144, :173-187.
- Struttura jigsaw tutta in dati: worldgen/structure/unsc_mjolnir_lab.json:1-20 e file correlati.

### 6.2 Trappole e bug

**Rete e stato**
- `ShieldUpdatePacket` registrato senza direzione: accettato anche dal server (MjolnirArmor.java:259-265, ShieldUpdatePacket.java:25-30).
- Stato client in statici mutabili della classe common, mai azzerati alla disconnessione: MjolnirArmor.java:92, SpartanAugmentation.java:88, HelmetSounds.java:14-15, MjolnirHudOverlay.java:245-246, :256, GrappleClient.java:43 (cooldown fantasma cambiando mondo).
- `PlayerTickEvent` senza filtro di fase: doppio tick e circa 40 pacchetti/s per giocatore anche senza cambiamenti (ShieldEventHandler.java:69-89).
- Ricarica a orologio di sistema, che corre in pausa; `lastDamageTime` non salvato (ShieldCapability.java:35, :48-57, :65-71).
- `PlayerEvent.Clone` senza `reviveCaps()`: la copia non avviene e lo scudo torna a 100 (ShieldEventHandler.java:40-46).
- `readUtf(8)` con limite stretto: una stringa più lunga non viene troncata ma fa lanciare `DecoderException` e cade la connessione (AugmentationSyncPacket.java:26); oggi gli id sono al massimo 3 caratteri, quindi il bug è latente.
- Config COMMON non sincronizzata col client (MjolnirArmor.java:243); valori letti prima del caricamento lanciano eccezioni (SpartanAugmentation.java:140, :203, :337).
- Mappa statica server `ACTIVE` mai ripulita se l'owner è offline (Grappleshot.java:31, GrappleHookEntity.java:242-247); mappa client in classe common (GrappleHookEntity.java:48, :94-101).
- Soglie duplicate client/server che possono divergere (GrappleHookEntity.java:164-175 vs GrappleClient.java:132-140).
- Origine del gancio al polso senza raycast: attraversa muri sottili (Grappleshot.java:51, :79-85).
- Velocità decisa dal client: un jetpack con costo energetico deve avere energia e stop decisi dal server (GrappleClient.java:30).

**Item e materiali**
- `durability(-1)` sui pezzi corpo: trucco 1.20.1 che su 1.21.1 lascia l'item "danneggiabile" con `MAX_DAMAGE=-1`, cioè già oltre il limite (verificato con javap; l'ipotesi "invalido per `POSITIVE_INT`" vale solo in serializzazione e non è verificata) (MjolnirArmor.java:109-130).
- `getName()` con namespace: rompe il lookup delle texture su Fabric 1.20.1 (ModArmorMaterials.java:68-71).
- Set/tier dedotti da catene di `instanceof` dipendenti dall'ordine (ArmorUtils.java:37-43, StationVariant.java:78-81, ArmorAssemblyBlockEntity.java:384-387).
- Pehkui caricato in inizializzatore statico (ArmorUtils.java:22) e forzato (MjolnirArmor.java:250): ogni classe che importa ArmorUtils trascina Pehkui.
- `damageItem` chiamato solo da Forge; soglia max-1 duplicata in SpartanCorpseEntity.java:52.
- `canEquip` sempre false blocca anche mob, dispenser e armor stand (MjolnirArmorItem.java:38-41).
- SlotMixin agisce solo se lo slot contiene già il pezzo e non copre swap con un'altra armatura, `/clear`, morte (SlotMixin.java:28-35).
- Sei blocchi `initializeClient` quasi identici (MjolnirArmorItem.java:51-67 e cinque copie).
- Ordinal dell'enum usato come valore persistente del predicato icona (ArmorVariant.java:23).
- Gate usato in `use()` ma non visibile al client (SpartanSerumItem.java:49).
- Accoppiamento tra sistemi tramite `UseAnim` (HelmetSealLock.java:38-48, SpartanSerumItem.java:102-105).
- Tab creativa senza `.title()` (MjolnirArmor.java:195-225).
- Su Fabric, `BlockItem` e BE chiamano `BLOCKS.get()`: l'ordine di registrazione conta (MjolnirArmor.java:234-247).

**Danno ed effetti**
- `isIndirect()` annulla ogni colpo indiretto (anche TNT accesa da un giocatore) e, essendo prima di `LivingDamageEvent`, lo scudo non assorbe mai i proiettili (ArmorEffectsHandler.java:18-20); su 1.21.1 il metodo è sostituito da `isDirect()` (logica invertita).
- Confronto di stringa `getMsgId().equals("magic")` (ArmorEffectsHandler.java:21).
- Lo scudo assorbe qualunque tipo di danno, anche fame, annegamento e vuoto (ShieldEventHandler.java:49-67).
- Effetti negativi rimossi dopo l'applicazione invece di essere negati (ShieldEventHandler.java:94-107).
- `@Redirect` su `Level.getEntitiesOfClass` esclusivo e in conflitto con altri mod (ThrownPotionMixin.java:18-22, AreaEffectCloudMixin.java:18-22, DotfMeleeArmsMixin.java:30-32).

**Client e rendering**
- Velocità del blend legata a quante volte il modello viene impostato per frame (AssemblyPose.java:33); WeakHashMap per Player con id riusati (:23-24).
- Se `RenderPlayerEvent.Post` non arriva, la testa a 0.85 resta su tutti i giocatori (PlayerModelHider.java:49, :79-85).
- Workaround GeckoLib non spiegato in prima persona: niente layer, glint né animazioni (MjolnirGeoArmorRenderer.java:108-136); bookkeeping interno duplicato (:62-68).
- Due autorità sull'orientamento (server teletrasporta, client forza lo yaw): jitter (ArmorAssemblyBlockEntity.java:215-235, AssemblyInputLock.java:28-36).
- `EntityTurnMixin` tocca `Minecraft.getInstance()` da un mixin su classe common: sicuro solo nella lista client (mixins.json:12-20).
- `RenderArmEvent` annullato sopprime i renderer di braccia di altri mod (FirstPersonArmorArms.java:51-53).
- Icone item `forge:obj` e renderer degli item mai chiamati (models/item/mjolnir_helmet_117.json:2); `forge:obj` non esiste su Fabric (geo2icons.py:112).
- HUD: hook Post+HOTBAR sparisce se un altro mod annulla la hotbar (MjolnirHudOverlay.java:161-167); HUD intero nascosto in terza persona (:173); allocazioni per frame (:320-325, :345-360, :390-401); raycast di 40 blocchi per frame (:233); disallineamento sub-pixel (:189-200); stile Locke opaco su tutto lo schermo (:133-151, :205-210); una volta disegnava 20+ volte per frame (:163-164).
- Scansione per entità per frame del predicato del contorno (VisorTracker.java:15-22).

**Stazione**
- Perdita di item se l'inventario è pieno durante il lock (AssemblyInteractionLock.java:49-55).
- Il lock colpisce chiunque sia sulla piattaforma, non solo il soggetto (AssemblyInteractionLock.java:19-22, AssemblyPose.java:57-72); la piattaforma spinge tutti (ArmorAssemblyBlockEntity.java:360-371).
- Chi carica il chunk a metà ciclo vede l'idle (ArmorAssemblyBlockEntity.java:506-509); cooldown, lastSubject e cellsAligned non salvati (:467-487).
- Update tag che si affida al comportamento di default di Forge (:250-255).
- `getRenderBoundingBox` e `getCloneItemStack` Forge-only, ignorati su Fabric (:491-494; ArmorAssemblyPartBlock.java:77-84).
- `destroyBlock(controller, true)` droppa sempre, anche in creativa (ArmorAssemblyPartBlock.java:58).
- Verifica di piazzamento che non ruota coi template e non vede il limite di altezza (ArmorAssemblyBlock.java:75-83).
- Menu client con handler fittizio se il BE non è ancora sincronizzato (ArmorAssemblyMenu.java:30-39).

**Mondo e dati**
- Un solo laboratorio per mondo (`concentric_rings` count 1) e bioma non vincolato (structure_set/unsc_mjolnir_lab.json:3-7; tags/worldgen/biome/has_structure/unsc_mjolnir_lab.json:4).
- Decorazioni duplicate con più piazzamenti (StructureStations.java:149-152); coordinate fisse dentro la struttura di un altro mod (:87-89); log INFO a ogni piazzamento (:138, :202).
- Nessun toggle di config per struttura e loot.
- GLM senza clamp alla stack size (AddItemLootModifier.java:45-46).
- Firma di `SinglePoolElement.place` per versione con `defaultRequire 1`: crash al caricamento (SinglePoolElementMixin.java:30-33).
- `DataGenerators` con `Dist.CLIENT` e nessun output; `build_lab.py` citato ma assente (StructureStations.java:80, :91).

**Build e tool**
- Refmap SRG→MCP solo per Pehkui: non copiarla (build.gradle:85-88); repo inutili (build.gradle:136, :153-159).
- `defaultRequire 1` vale anche per i `@Pseudo` (mixins.json:21-23).
- Percorsi Windows fissi negli script (bb2geo.py:8-10 e altri); script che sovrascrivono la sorgente senza backup (side_arms.py:153/161, retime_back_arm.py:65-66, add_arm_clamps.py:101, remove_foot_clamps.py:20); import circolare (halo2geo.py:210).
- `changelog.txt` di Forge tracciato: il `.gitignore` esclude solo `forge*changelog.txt`.
- `run-data/` (config e 8 file di log) tracciato in git nonostante sia in `.gitignore`; i log contengono percorsi locali Windows con il nome utente: nulla di `run-data/` va copiato o pubblicato (aggiunto in verifica).

---

## 7. Dipendenze esterne e come Exoframes ne fa a meno

| Dipendenza in Mjolnir | Dove | In Exoframes |
|---|---|---|
| **Pehkui** 3.8.2 (obbligatoria, LGPL-3 secondo core-registration, "creduta LGPL-3, non verificata" secondo compat-build-tools) | build.gradle:172; mods.toml:77-82; MjolnirArmor.java:250; ArmorUtils.java:22, :90-107; SpartanAugmentation.java:194-197, :374-379; StationVariant; tools di coreografia (scala 1.5/1.15) | Assente (anti-obiettivo §2, decisione §7.13): il telaio è un'armatura GeckoLib più grande senza cambiare l'hitbox; i predicati vanno in core/ senza scala; la stazione si anima per un giocatore a scala 1.0; niente proprietà refmap in build. |
| **Dawn of the Flood** (opzionale, runtimeOnly via CurseMaven) + fabridge, clockworklib, AAA Particles | build.gradle:176-179; mods.toml:83-89; DotfIntegration, mixin/dotf/*, GeckoPlayerArmor, DotfSkinHider, MjolnirGeoArmorRenderer.java:38-106 | Scartata per intero; resta la tecnica "riconoscimento per id + `Platform.isModLoaded`" per la compatibilità FE/Create/Mekanism dopo la 1.0. |
| **Gigeresque** (solo riflessione, nessuna dipendenza di build) | GigeresqueIntegration.java | Scartata; nessuna integrazione per riflessione. |
| **Vic's Modern Warfare** (non è una dipendenza: sorgente di asset) | tools/mkvi2geo.py, modelbiped2geo.py, hud_textures.py, tools/README.md:12-18 | Nessun asset né script che lo importi; tutto il modello e l'HUD sono nuovi. |
| `halo_armor.obj` di provenienza ignota; render Halopedia; suoni Halo Infinite | halo2geo.py, locke2geo.py, CREDITS.md | Esclusi. |
| **GeckoLib** 4.4.9 (MIT) | ovunque nel client | Resta, ma 4.8.4 (1.20.1) e 4.9.3 (1.21.1); package e hook diversi assorbiti in compat/client. |
| **Architectury** 9.2.14 (solo runtimeOnly in Mjolnir, LGPL-3) | build.gradle:182 | Dipendenza principale (9.2.14 / 13.0.11): registri, eventi, rete, menu. |
| Forge capability, `getPersistentData`, ForgeConfigSpec, GLM, `IForgeItem`/`IClientItemExtensions`, `ItemStackHandler` | §3 | Attachment per loader dietro `FrameStateAccess`; config Gson; LootEvent; layer GeckoLib; `SimpleContainer`. |
| ForgeGradle 6 + MixinGradle 0.7; repo JitPack (solo Pehkui), maven.azuredoom.com (inutilizzato), CurseMaven | build.gradle | Architectury Loom + Stonecutter; solo i repo che servono (GeckoLib Cloudsmith con filtro di contenuto, build.gradle:145-152, come checklist). |
| Python numpy (BSD-3), Pillow (MIT-CMU) | tools/ | Solo per i tool di sviluppo, non nei jar. |
| Mixin (MIT), MixinExtras | mixin | Mixin resta; MixinExtras per `@WrapOperation` (disponibilità per target da confermare: compresa in Forge 47.4.29, NeoForge e Fabric Loader 0.20.1 secondo worldgen-loot-data). |

---

## 8. Licenza e provenienza

**Cosa dicono i file**
- **LICENSE.txt** non è una licenza del mod: è il LICENSE.txt del Forge 1.20.1 MDK, cioè il preambolo di Forge ("Minecraft Forge, Forge Mod Loader, and all parts herein are licensed under the terms of the LGPL 2.1", LICENSE.txt:1-4; nota di non-infettività per i mod, :23-28; MCP, FML, ComparableVersion, javaxdelta, Monocraft) seguito dal testo integrale della GNU LGPL 2.1 (:66-523). Concede diritti su Forge, non su Mjolnir Armor, e contraddice la dichiarazione del mod.
- **gradle.properties:53** `mod_license=All Rights Reserved` (default dell'MDK), espanso in `mods.toml:12`. Il codice di Mjolnir è quindi "tutti i diritti riservati" del suo autore (`mod_authors=ToxinEren`, gradle.properties:61, che il progetto identifica con Luigi): nessun file concede diritti a terzi.
- **CREDITS.txt, README.txt, changelog.txt** sono file dell'MDK di Forge (crediti Forge/FML, istruzioni di setup, changelog di Forge), non del mod. **CREDITS.md** è del mod: dichiara che i suoni dello scudo vengono da Halo Infinite e che il mod è fatto secondo le "Game Content Usage Rules" di Microsoft (CREDITS.md:5-9), definendosi "progetto amatoriale non commerciale" (:11-12); gli altri suoni sarebbero sintetizzati da un `make_sounds.py` che non è nel repo; release/curseforge-description.md chiude con "Halo (c) Microsoft ... free, non-commercial fan project".
- **Header**: nessun file Java o Python ha header di licenza, SPDX, `@author` di altri o note "based on/tutorial" (grep di compat-build-tools su src/main/java). I commenti sono in italiano e coerenti. Boilerplate da template: commenti dell'ExampleMod MDK in MjolnirArmor.java (:84, :88-91, :154-158, :194, :231-240, :296), mods.toml, pack.mcmeta, build.gradle; `DataGenerators`/`ModItemModelProvider` e la forma di `AddItemLootModifier` ricalcano il tutorial Kaupenjoe Forge 1.20.x; `ShieldProvider` ricalca il tutorial delle capability; il blocco `IClientItemExtensions` ricalca il wiki GeckoLib (esempi MIT). Tutto questo viene scartato o riscritto.
- **Storia git**: un solo commit compresso (38d35e9, 2026-10-10, autore ToxinEren), quindi la provenienza del codice non si verifica dalla storia.

**Conseguenze per Exoframes**
1. Il codice che Exoframes copia o adatta da Mjolnir è oggi ARR; può uscire sotto **MIT** solo per decisione esplicita di Luigi come unico autore. Va confermato che nessun file contenga codice di altri (tutorial, snippet da Discord o chat di terzi, codice di VMW o DotF). Le letture non hanno trovato codice di terzi, ma solo l'autore può confermarlo.
2. Il boilerplate MDK/tutorial non va copiato: Exoframes parte dai template Architectury/Stonecutter. I file LICENSE.txt/CREDITS.*/README.txt/changelog.txt di Mjolnir **non** vanno copiati.
3. Exoframes deve avere: `LICENSE` (MIT, solo codice); un avviso separato per gli asset ("Tutti i diritti riservati": texture, modelli, suoni, animazioni); `license="MIT"` in mods.toml/neoforge.mods.toml e `"license": "MIT"` in fabric.mod.json, con la nota ARR per gli asset nel README; un'attribuzione nel README del tipo "sistemi adattati da Mjolnir Armor dello stesso autore" (design §7.3) che non nomina asset Halo e chiarisce che nessun asset, nome o dato derivato da Halo è stato riusato.
4. **Nessun asset di Mjolnir nel repo, nemmeno nella storia git**: niente fork né import del repo Mjolnir (design §7.3); i file si copiano a mano uno per uno, già ripuliti da nomi Halo e riferimenti a VMW nei commenti; nessun commit intermedio deve contenere `.png`, `.ogg`, `.geo.json`, `.animation.json`, `.nbt`, `.obj` o testi di lang di Mjolnir. Se succede, va riscritta la storia prima che il repo diventi pubblico. I `.bbmodel` sorgente sono sul disco D: di Luigi e non nel repo.
5. Le dipendenze usate come librerie non impongono nulla al codice MIT: Forge/NeoForge (LGPL-2.1, non infettiva per i mod), Fabric Loader/API (Apache-2.0), Architectury (LGPL-3.0), GeckoLib (MIT), Mixin (MIT), Cloth Config (LGPL-3.0). Non vanno copiati né inclusi loro sorgenti modificati. Le mappature Mojang si usano ma non si ridistribuiscono.
6. Gli script `tools/*.py` sono dell'autore e possono diventare MIT; vanno tenuti fuori dai jar e ripuliti da percorsi e riferimenti a VMW/Halo prima di un repo pubblico.

---

## 9. Cosa informa ciascuna feature di Exoframes e ordine per la milestone 3

| Feature | Cosa viene da Mjolnir | Cosa è nuovo |
|---|---|---|
| **Telaio / entità** | SpartanCorpseEntity come base di ExoframeEntity (immobile, persistente, interagibile, svestibile); SlotMixin + `setItemSlot` + drop a terra per i quattro segnaposto bloccati; trio posa/lock (EntityTurnMixin, AssemblyInputLock, AssemblyPose, HumanoidModelMixin) e lock delle interazioni con predicato condiviso (AssemblyInteractionLock) per ingresso, uscita con V tenuto 1 s e cambio cella; modificatori di attributo a id fisso per velocità −15%, knockback, energia zero −70%; annullamento della caduta; resync login/clone/dimensione | Stato del telaio in attachment (`FrameStateAccess`); entità telaio alla morte (Mjolnir copia sul clone, e in modo rotto); armatura civile stivata; proprietario; guardie per swap, `/clear` e morte; niente nuoto |
| **Pezzi e danni** | Stato rotto derivato e clamp (MjolnirHelmetItem.java:32-44) come funzione pura; texture per stato (MjolnirHelmetRenderer); assorbi-poi-passa come unico punto di intercettazione del danno | Sei pezzi con durabilità in core/; ripartizione per categoria via `DamageClassifier` e tag; protezione dei segnaposto ricalcolata dai pezzi; overlay di danno (crepe generate offline, idea mkvi2geo.crack) |
| **Energia e scudo** | ShieldCapability come EnergyBuffer puro (drena, soglie, eventi di fronte) con tick di gioco; scudo energetico come assorbimento totale dei proiettili via `LIVING_HURT` (cross-loader); avviso <10% e allarme a zero come HelmetSounds; cambio cella di 2 s come SpartanSerumItem | Celle da 10.000 RE via ItemDataAccess; consumo per azione; caricatore; niente ricarica spontanea |
| **Stazione** | Multiblocco controller+parte (o, più semplice, un blocco con HALF stile letto) con verifica di piazzamento e cascata; GeckoLib block renderer con scala e ossa nascoste; sync minimo via update tag; `triggerAnim` da server; tabella di tick allineata ai keyframe; `MenuRegistry.ofExtended`; ricetta pezzo+tinta come eventuale fallback da banco | GUI a sei pezzi con `clickMenuButton`/`DataSlot`; legame con l'entità telaio entro 2 blocchi; riparazione, moduli, verniciatura, preset, crafting tier 2–5; anteprima 3D |
| **Moduli, tasti e pacchetti** | Tasto → C2S intento → server valida (GrapplePacket); `KeyMapping` + `consumeClick` con guardia `mc.screen == null` (GrappleClient.java:76-86) via KeyMappingRegistry; predizione con `releaseSent`; scanner e rilevatore di Sculk con MinecraftGlowMixin; jetpack con il controller `pull()` e cooldown "pronto-a"; servomotori come modificatore transitorio; comando `/exoframe` di debug | V tenuto 1 s (`isDown` + contatore), menu radiale G, onda d'urto (hook sull'atterraggio da creare), lama, trivella, visore, respiratore |
| **HUD** | Pipeline della barra (rampa colori, glow di ricarica, impulso a zero, coordinate float, trapezi) dietro `QuadEmitter` o con `GuiGraphics.fill`; layout 1280×720 per la vignetta; un solo hook (`ClientGuiEvent.RENDER_HUD`); gate unico → `HudGate` NONE/BAR_ONLY/FULL; payload `HudSync` S2C sul cambiamento (≤4-5 Hz) con store client azzerato alla disconnessione | Sagoma dei sei pezzi, moduli attivi con consumo, testo percentuale e celle di scorta; geometria della barra da JSON accanto alla texture |
| **Rendering GeckoLib** | Convenzione dello scheletro `bipedX`→`armorX`; GeoModel parametrico; `getTextureLocation` per stato; `setHidden`+`setChildrenHidden`; pivot di riposo; PlayerModelHider (mixin su Fabric); braccio in prima persona (da ri-verificare); `DyeableGeoArmorRenderer` per le vernici | Layer GeckoLib sul giocatore che legge lo stato (non per item); classi base in compat/client per 4.8.4/4.9.3; texture vanilla trasparenti per i segnaposto; asset placeholder e spec (bone, UV, maschere di tinta) |
| **Mondo e loot** | Struttura jigsaw tutta in dati; decorazione chunk-safe con trasformazione locale; spawn di entità in worldgen; forma "un tipo di loot + tante piccole istanze" | `LootEvent.MODIFY_LOOT_TABLE` gated da config; villaggio in rovina; laboratorio nel Deep Dark; Illager Corazzato (raid: `Raid.RaiderType` per loader in compat/) |

**Ordine consigliato per la milestone 3 (Telaio)**

1. **Spike di rendering (prima di tutto)**: un pezzo placeholder disegnato da un layer GeckoLib sul giocatore, su tutti e quattro i target, con classi base in `compat/client` per package e firme di 4.8.4/4.9.3; skin nascosta (PlayerModelHider, mixin su Fabric). Decide se i segnaposto sono GeoItem (raccomandato: no).
2. **`FrameStateAccess`**: attachment per loader (NeoForge AttachmentType, Fabric data-attachment, Forge 1.20.1 capability dietro wrapper) con codec da core/; resync su join/clone/cambio dimensione; payload `Net` minimo.
3. **Segnaposto e `ArmorMaterials`**: quattro item senza durabilità, materiale senza namespace nel nome e senza ingrediente di riparazione, texture `_layer_1/2` trasparenti, SlotMixin esteso allo slot 39 con guardie per swap, `/clear` e morte; equipaggiamento solo via `setItemSlot`.
4. **ExoframeEntity**: dall'idea di SpartanCorpseEntity, con GeoEntityRenderer; piazzamento dall'item, montaggio dei pezzi con tasto destro, proprietario.
5. **Ingresso e uscita**: lock interazioni (Architectury InteractionEvent), lock input/visuale (EntityTurnMixin + mixin KeyboardInput), posa (HumanoidModelMixin), animazione; V tenuto 1 s; armatura civile stivata; morte → entità.
6. **Effetti base**: `Attributes` (velocità −15%, knockback), caduta annullata via `LIVING_HURT` + `IS_FALL`, niente nuoto, niente sprint sotto 10%.
7. **Tier Rottame**: materiali e valori da tabella, modelli placeholder generati coi tool (bb2geo/obj2geo/atlas procedurale).
8. **Cella e caricatore**: energia via `ItemDataAccess`; cambio cella di 2 s con flusso a durata d'uso e lock; caricatore a redstone.
9. **Comando `/exoframe`** (give/energy/repair/info) per i test sul PC, poi checklist in `docs/test-in-gioco/`.

---

## 10. Domande aperte per Luigi

1. **Provenienza e licenza del codice**: confermi di essere l'unico autore di tutto il Java di Mjolnir e degli script `tools/*.py` (niente codice da tutorial, Discord, chat di terzi, VMW o DotF) e che i file adattati possono uscire sotto MIT con l'attribuzione nel README?
2. **Dimensioni del giocatore**: confermi "nessun cambio di dimensioni, niente Pehkui" (§7.13)? Il sì rende inutilizzabile la coreografia della stazione di Mjolnir e i predicati legati alla scala.
3. **Strategia di rendering**: va bene il layer GeckoLib sul giocatore che legge lo stato, con segnaposto non GeoItem e texture vanilla trasparenti? Le braccia corazzate in prima persona entrano nell'alpha (servono mixin su Fabric e la ricetta di Mjolnir è da ri-verificare)?
4. **Verniciatura**: tinta a runtime per bone (`DyeableGeoArmorRenderer`, zone primaria/secondaria come bone separati in scala di grigi) o maschere con un `GeoRenderLayer`? La scelta fissa la spec asset.
5. **Budget di mixin**: accetti SlotMixin, EntityTurnMixin, MinecraftGlowMixin, HumanoidModelMixin, più i mixin solo-Fabric (KeyboardInput, PlayerRenderer.render/renderHand, atterraggio/`causeFallDamage`, modifica dell'importo del danno), tutti in `compat/mixin` con un mixins.json per variante?
6. **Danno e scudo**: lo scudo energetico assorbe totalmente i proiettili (cancel, uguale sui quattro loader) e costa RE per punto di danno oltre ai 30 RE/s? La resistenza ai proiettili del tier 3 e la protezione al 50% a energia zero sono percentuali (serve modificare l'importo per loader)?
7. **Stazione**: blocco unico con HALF stile letto o controller+parte? Il telaio si lega "il più vicino entro 2 blocchi" all'apertura o con un aggancio esplicito (UUID nel BE)? Gli slot dei pezzi in GUI sono un inventario reale o una vista sullo stato?
8. **Elmo rotto**: mostrare il volto (testa rimpicciolita a 0.85 come Mjolnir) o solo il pezzo annerito come dice il Claude Doc?
9. **Config e HUD**: i valori di bilanciamento arrivano al client con un payload al login (uniforme sui quattro loader) invece di una config SERVER sincronizzata? La geometria della barra HUD va in un JSON accanto alla texture (ripersonalizzabile dai resource pack)?
10. **Id, icone e tool**: congeliamo gli id di registro prima dell'alpha (Fabric non ha remap)? Icone item con GeoItemRenderer o sprite 2D renderizzati offline (`forge:obj` non esiste su Fabric)? La cartella `tools/` Python sta nel repo pubblico o in uno privato insieme ai `.bbmodel`?

---

## 11. Verifica di completezza

Revisione del 2026-11-13 sul tree reale (`Mjolnir Armor mod`, commit 38d35e9). Le firme vanilla sono state controllate con `javap` sui jar Minecraft 1.20.1 e 1.21.1 con mappature Mojang della cache Loom, le firme di Architectury su 9.2.14 e 13.0.11, i package di GeckoLib su 4.8.4 e 4.9.3.

**Copertura**

| Gruppo | Nel tree | Già nel §2 della bozza | Aggiunti in verifica |
|---|---|---|---|
| File Java in `src/main/java` | 85 | 85 | 0 |
| File in `tools/` (18 script + README.md) | 19 | 19 | 0 |
| File di build/metadati (build.gradle, settings.gradle, gradle.properties, LICENSE.txt, CREDITS.md/.txt, README.txt, changelog.txt, mods.toml, pack.mcmeta, mixins.json, .gitignore, .gitattributes, gradlew, gradlew.bat, wrapper .properties/.jar, 4 file in `release/`, `run-data/`, logo.png, atlases/blocks.json) | 25 | 11 | 14 (9 righe in §2.9) |
| **Totale** | **129** | **115** | **14** |
| Gruppi di risorse (cartelle sotto `assets/` e `data/`) | 33 cartelle con file | 22 citate in §5 | 11 aggiunte in §5 (blockstates, models/block, atlases, configured/placed_feature, biome_modifier, template_pool, recipes, loot_tables/blocks, tag forge/minecraft, global_loot_modifiers) |

Conteggi confermati: 18 texture in `textures/armor/`, 15 GLM in `loot_modifiers/`, 8 geo, 4 animazioni.

**Affermazioni controllate (file:riga)**

Confermate (circa 50 citazioni file:riga, più una ventina di firme API):
- §3: MjolnirArmor.java:96-101 (SimpleChannel), :243 (config COMMON), :249-250 (`GeckoLib.initialize`, Pehkui forzato), :277-294 (MissingMappings), :303-310 (MenuScreens, ItemProperties), :314-326; ArmorAssemblyBlock.java:100/:103 (`use` + `NetworkHooks`); ArmorAssemblyPartBlock.java:77-84 (`getCloneItemStack` Forge con HitResult); ArmorAssemblyBlockEntity.java:491-494; MjolnirHudOverlay.java:161-167 e :173; GrappleClient.java:150-156; ArmorEffectsHandler.java:15-33; ShieldEventHandler.java:40-46, :49-67, :69-89; build.gradle:136, :172, :182.
- §4: SpartanAugmentation.java:70-85 (UUID fissi), :430-452; ArmorVariant.java:23, :35-52; ArmorAssemblyScreen.java:25; SpartanCorpseEntity.java:152, :157; MjolnirArmor.java:162-165 (`DropExperienceBlock`, argomenti invertiti su 1.21.1); SinglePoolElementMixin.java:29-37 (`LiquidSettings` in più su 1.21.1); ThrownPotionMixin.java:18 (`applySplash(Iterable)` su 1.21.1); AssemblyPose.java:33 (`getDeltaFrameTime` assente su 1.21.1).
- §6: SlotMixin.java:30-36 (slot 36-38, elmo escluso); MjolnirArmorItem.java:38-49, :51-67, :69-80; MjolnirHelmetItem.java:32-44; SpartanCorpseEntity.java:52; ShieldCapability.java:35, :48-57, :65-71; MjolnirArmor.java:92, :259-265; AddItemLootModifier.java:45-46; ArmorAssemblyPartBlock.java:58; AssemblyInteractionLock.java:49-55; GrappleClient.java:143-148; EntityTurnMixin.java:19-25; MinecraftGlowMixin.java:15-18; HumanoidModelMixin.java:29; tools/obj2geo.py:27-29, pngio.py:10, bb2geo.py:111, hud_textures.py:1-10, mkvi2geo.py:1-3.
- §8: LICENSE.txt:1-4 (preambolo Forge, LGPL 2.1), :23-28 (non infettività), testo LGPL 2.1 da ~:66 a :523 (fine file); gradle.properties:53 `mod_license=All Rights Reserved`, :61 `mod_authors=ToxinEren`; mods.toml:12 `license="${mod_license}"`; CREDITS.md:5-12; CREDITS.txt, README.txt, changelog.txt dell'MDK; un solo commit 38d35e9 del 2026-10-10.
- §4, API 1.21.1 controllate con javap: `ResourceLocation.fromNamespaceAndPath` (costruttori pubblici rimossi); `ArmorMaterial` record e `ArmorItem(Holder<ArmorMaterial>, …)`; `AttributeModifier(ResourceLocation, double, Operation)` con `ADD_VALUE/ADD_MULTIPLIED_BASE/ADD_MULTIPLIED_TOTAL`; `getUseDuration(ItemStack, LivingEntity)` e `appendHoverText(…, TooltipContext, …)`; `useWithoutItem/useItemOn`; `getCloneItemStack(LevelReader, …)`; `renderBackground(GuiGraphics, int, int, float)`; `Minecraft.getTimer()` (DeltaTracker); `DamageTypeTags.NO_KNOCKBACK` solo su 1.21.1; `Slot.mayPickup`, `Entity.turn`, `shouldEntityAppearGlowing` invariati; Architectury `NetworkManager.registerS2CPayloadType` + StreamCodec in 13.0.11 e `ClientGuiEvent.RenderHud(GuiGraphics, DeltaTracker)`; GeckoLib `core.*` in 4.8.4 → radice in 4.9.3, `GeckoLib.initialize()` presente solo in 4.8.4, `GeoBlockRenderer.preRender` colore float → int.

Corrette (20 modifiche al testo):
1. §1: i file a riuso diretto sono 4, non 5 (SlotMixin è consolidato in "adattare" da §2.10).
2. §1, §2.5, §4, §6.2: `isIndirect()` non è semplicemente sparito, su 1.21.1 c'è `isDirect()` con logica invertita (javap); il lettore hud ("esiste ancora") sbagliava.
3. §2.1, §6.2: `readUtf(8)` non tronca, lancia `DecoderException` (bytecode di 1.20.1); il bug è latente perché gli id sono ≤3 caratteri.
4. §2.9, §5: `mkvi2geo.py` ha 291 righe; `crack()` è a :59 (chiamata a :291); la citazione :306-326 era errata.
5. §3, §4: con Architectury gli extra data dei menu restano `FriendlyByteBuf` su 9.2.14 e 13.0.11; tolta la richiesta di verifica e la necessità di un codec in `Net`.
6. §4: `new AABB(BlockPos)` esiste su 1.21.1: da "da verificare" a stabile.
7. §4: aggiunto che `getAttribute` prende un `Holder<Attribute>` su 1.21.1.
8. §1, §4, §6.2: `durability(-1)` su 1.21.1 descritto da bytecode (`MAX_DAMAGE=-1`, `isDamageableItem()` vero); l'ipotesi "invalido per POSITIVE_INT" resta non verificata.
9. §8: formulazione di CREDITS.md riportata fedelmente; autore citato come `mod_authors=ToxinEren`.
10. §2.9: 9 righe nuove per i file non coperti; §5: elenco dei gruppi di risorse mancanti; §6.2: `run-data/` tracciato con log che contengono percorsi locali con il nome utente.

Non verificabili qui (restano come li riportano i lettori):
- Disponibilità di MixinExtras per target (§7) e il comportamento a runtime di `DROP_ITEM` su Fabric (§3).
- `getRenderBoundingBox` sul renderer in NeoForge 1.21.1 e le versioni di `fabric-data-attachment-api` (§3): non controllate con javap.
- Gli effetti a runtime di `durability(-1)` su 1.21.1 (rotto al primo colpo o rifiutato in serializzazione).
- Origine del codice (nessun header di terzi trovato, ma solo l'autore può confermarla, §10 domanda 1) e la licenza di Pehkui (LGPL-3 non verificata).
- Le righe citate solo per descrivere tecniche (es. MjolnirHudOverlay.java :76-101, :342-375, StructureStations.java :117-162) non sono state riaperte tutte; sulle circa 54 citazioni riaperte, 4 erano sbagliate o imprecise (readUtf, isIndirect, mkvi2geo, menu extra data): tasso d'errore basso ma non nullo.

**Punti aperti**
- Il §2 ha ancora righe duplicate per classe (una per lettore); la decisione da seguire è sempre quella di §2.10.
- La rimozione di `run-data/` e `release/` dalla storia di Mjolnir non riguarda Exoframes, ma conferma la regola del §8.4: non importare nulla dal repo Mjolnir via git.
- Va deciso prima di M3 se usare `MenuRegistry` di Architectury (extra data uniformi) o le API dirette dei loader.
