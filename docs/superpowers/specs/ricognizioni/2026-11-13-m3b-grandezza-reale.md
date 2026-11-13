# M3b, ricognizione: grandezza reale del giocatore nella tuta

Ricognizione per la decisione "grandezza reale" (design M3 §4.7 ultimo paragrafo, §8 punto 2, §10 primo punto di "Per la parte 3b"). Nulla è stato cambiato sotto `/home/user/exosuits`. I fatti vengono dal bytecode (`javap -c -p`) dei jar che il progetto ha già risolto e dai sorgenti delle librerie; l'esperimento è in un clone usa e getta (§9).

## Fonti

- **Minecraft (nomi Mojang)**, sotto `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/`:
  - `minecraft-merged/1.20.1-…/minecraft-merged-1.20.1-….jar` (**V20**, vanilla)
  - `forge-1.20.1-47.4.29-minecraft-merged/…/…-forge-1.20.1-47.4.29.jar` (**Fo20**, con le patch di Forge)
  - `minecraft-merged/1.21.1-…/minecraft-merged-1.21.1-….jar` (**V21**)
  - `neoforge-21.1.266-minecraft-merged/…/…-neoforge-21.1.266.jar` (**Neo21**)
- **Sorgenti dei loader:** `~/.gradle/caches/modules-2/files-2.1/net.minecraftforge/forge/1.20.1-47.4.29/…/forge-1.20.1-47.4.29-sources.jar` e `net.neoforged/neoforge/21.1.266/…/neoforge-21.1.266-sources.jar`.
- **Architectury** 9.2.14 e 13.0.11 (sorgenti dei quattro jar rimappati in `/home/user/exosuits/.gradle/loom-cache/remapped_mods/remapped/dev/architectury/`); **Fabric API** dei jar rimappati in `…/remapped/net/fabricmc/fabric-api/`.
- Dove non è detto altro, V20 e Fo20 hanno lo stesso bytecode nei metodi citati, e così V21 e Neo21 (controllato metodo per metodo).

---

## 1. Come il gioco decide ingombro e occhi del giocatore

### 1.1 Su 1.20.1 (V20 = Fo20 salvo l'evento)

**Risposta.** Il box per posa viene da `Player.getDimensions(Pose)`, che legge una tabella fissa e **ignora `getScale()`**. Gli occhi vengono da `Player.getStandingEyeHeight(Pose, EntityDimensions)`, che restituisce **costanti** e ignora le dimensioni ricevute. Per ingrandire box e occhi servono quindi due agganci distinti. Il gioco copia box e occhi nei campi `dimensions` e `eyeHeight` solo in `refreshDimensions()` (e nel costruttore).

- `Player.getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;`: `POSES.getOrDefault(pose, STANDING_DIMENSIONS)`, un solo `areturn`. È un override completo: non chiama `LivingEntity.getDimensions` (che sarebbe `super.getDimensions(pose).scale(getScale())`, con `getScale()` = 0,5 per i cuccioli, altrimenti 1).
  - Tabella (`Player.<clinit>`): `STANDING_DIMENSIONS = scalable(0.6, 1.8)`; SLEEPING → `LivingEntity.SLEEPING_DIMENSIONS = fixed(0.2, 0.2)`; FALL_FLYING, SWIMMING e SPIN_ATTACK → `scalable(0.6, 0.6)`; CROUCHING → `scalable(0.6, 1.5)`; DYING → `fixed(0.2, 0.2)`.
- `Player.getStandingEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F`: `tableswitch` sulla posa con tre `freturn`: 0,4 per nuoto, volo con elitre e affondo del tridente; 1,27 accovacciato; 1,62 altrimenti.
- `LivingEntity.getEyeHeight(Pose, EntityDimensions)` è `final`: restituisce 0,2 se la posa è SLEEPING, altrimenti `getStandingEyeHeight(...)`.
- `Entity.getEyeHeight(Pose)` = `getEyeHeight(pose, getDimensions(pose))` (calcolato ogni volta); `Entity.getEyeHeight()` = campo `eyeHeight` (in cache).
- `Entity.getBbWidth()` e `getBbHeight()` sono `final` e leggono il campo `dimensions`; `Entity.makeBoundingBox()` = `dimensions.makeBoundingBox(position())`.

**Chi aggiorna e quando.**

- `Entity.refreshDimensions()`: `dimensions = getDimensions(getPose())`, `eyeHeight = getEyeHeight(pose, dims)`, `reapplyPosition()` (ricostruisce il box). Lo chiamano solo:
  - `Entity.onSyncedDataUpdated(EntityDataAccessor)` quando cambia `DATA_POSE`, cioè a ogni `setPose`, sui due lati;
  - `Slime`, per la propria taglia.
- `Entity.fixupDimensions()` fa lo stesso senza `reapplyPosition` ed è usato solo da `Slime`.
- **Nessun cambio di equipaggiamento chiama `refreshDimensions`.** Nell'esperimento, dopo la vestizione senza ricalcolo il box resta 0,6 × 1,8 e gli occhi 1,62, mentre `getEyeHeight(CROUCHING)`, che è calcolato al volo, vale già quello della tuta (riga `wear,no refresh` in §9).

**Posa forzata quando il box non sta.**

- `Player.updatePlayerPose()` (chiamato da `Player.tick`, sui due lati) sceglie la posa voluta, nell'ordine FALL_FLYING, SLEEPING, SWIMMING, SPIN_ATTACK, CROUCHING (Shift e non in volo), STANDING. Se il giocatore non è spettatore né passeggero e `canEnterPose(voluta)` è falso, prova CROUCHING e poi SWIMMING. La prima istruzione esce subito se `canEnterPose(SWIMMING)` è falso: se non sta nemmeno a carponi, la posa resta com'è.
- `Entity.canEnterPose(Pose)` = `level.noCollision(this, getBoundingBoxForPose(pose).deflate(1e-7))`, e `getBoundingBoxForPose` usa **`getDimensions(pose)`**, non il campo.
- Sul client, `LocalPlayer.aiStep` decide `crouching` con `canEnterPose(CROUCHING)` e `!canEnterPose(STANDING)`: chi non sta in piedi si accovaccia.

**Spinta fuori dai blocchi.**

- `refreshDimensions` cerca una posizione libera (`Level.findFreePosition`) solo se `!isClientSide && !firstTick && !noPhysics`, se il box cresce e se l'entità **non** è un `Player`: c'è un `instanceof Player` esplicito, uguale in V20 e Fo20. Per i giocatori il server non sposta nulla.
- L'unica spinta è sul client: `LocalPlayer.aiStep` chiama `moveTowardsClosestSpace(x ± 0,35·larghezza, z ± 0,35·larghezza)`. Spinge orizzontalmente di 0,1 per tick se un angolo cade in un blocco soffocante, verificato con `suffocatesAt`, che usa il box intero in altezza. In verticale non spinge mai.
- Soffocamento: `Entity.isInWall()` prende un box piatto di lato `0,8·larghezza` attorno a `getEyePosition()`. Se tocca un blocco soffocante, `LivingEntity.baseTick` infligge danno `inWall`.
- Server: `ServerGamePacketListenerImpl.handleMovePlayer` rifiuta uno spostamento se `isPlayerCollidingWithAnythingNew(LevelReader, AABB, DDD)` (riporta indietro: "moved wrongly"). Chi è già incastrato può uscire, ma non entrare in collisioni nuove.

### 1.2 Su 1.21.1 (V21 = Neo21 salvo l'evento)

**Risposta.** Ingombro, occhi e punti di attacco stanno in un solo oggetto, `EntityDimensions(width, height, eyeHeight, EntityAttachments attachments, fixed)`. Il punto unico per il giocatore è `Player.getDefaultDimensions(Pose)`. `LivingEntity.getDimensions(Pose)` è `final` e moltiplica per l'attributo `generic.scale`.

- `LivingEntity.getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;` (`final`): SLEEPING → `SLEEPING_DIMENSIONS`, altrimenti `getDefaultDimensions(pose).scale(getScale())`.
- `Player.getDefaultDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;` (pubblico in `Player`): `POSES.getOrDefault(pose, STANDING_DIMENSIONS)`, un solo `areturn`.
  - Tabella: STANDING `scalable(0.6, 1.8).withEyeHeight(1.62).withAttachments(VEHICLE → (0, 0.6, 0))`; FALL_FLYING, SWIMMING e SPIN_ATTACK `scalable(0.6, 0.6).withEyeHeight(0.4)`; CROUCHING `scalable(0.6, 1.5).withEyeHeight(1.27)` con lo stesso attacco VEHICLE; DYING `fixed(0.2, 0.2).withEyeHeight(1.62)`.
- `EntityDimensions.scale(float, float)`: se non è `fixed`, scala larghezza, altezza, **occhi** (per il fattore verticale) e **attacchi** (`attachments.scale(sx, sy, sx)`); se è `fixed` restituisce sé stesso.
- Gli attacchi senza voce esplicita usano i valori di ripiego di `EntityAttachment`: NAME_TAG e PASSENGER `AT_HEIGHT` (0, altezza, 0), VEHICLE `AT_FEET`.
- `Entity.getAttachments()` = `dimensions.attachments()`, quindi segue il box corrente.
- `Entity.getEyeHeight(Pose)` è `final` e vale `getDimensions(pose).eyeHeight()`; `fixupDimensions` e `refreshDimensions` prendono gli occhi da `EntityDimensions.eyeHeight()`.

**Chi aggiorna:**

- come su 1.20.1: `onSyncedDataUpdated(DATA_POSE)`;
- in più, in coda a `LivingEntity.tick()`: `if (getScale() != appliedScale) { appliedScale = getScale(); refreshDimensions(); }`. Un cambio dell'attributo `scale` ricalcola da solo, sui due lati.
- Anche qui la spinta fuori dai blocchi (ora `Entity.fudgePositionAfterSizeChange`) esclude i `Player` (`instanceof Player` in `refreshDimensions`); la chiamano solo `Entity`, `ThrownEgg` e `Tadpole`.

**Posa forzata:**

- `Player.updatePlayerPose()` ha la stessa logica di 1.20.1, con `canPlayerFitWithinBlocksAndEntitiesWhen(Pose)` al posto di `canEnterPose`.
- Quel metodo vale `level.noCollision(this, getDimensions(pose).makeBoundingBox(position()).deflate(1e-7))`.
- `LocalPlayer.aiStep` usa lo stesso metodo per l'accovacciarsi forzato.
- `LivingEntity.wouldNotSuffocateAtTargetPose(Pose)` usa anch'esso `getDimensions(pose)`.

### 1.3 Altri lettori di `getDimensions(pose)` (non del campo)

Stessa lista sulle due versioni:

- `LivingEntity.getLocalBoundsForPose`: smonto da veicoli, `DismountHelper`;
- `Entity.getRelativePortalPosition`, `PortalShape.createPortalInfo` (1.20.1) e il passaggio del portale del Nether (1.21.1): posizione d'uscita dai portali;
- `Entity.getEyeHeight(Pose)`;
- su 1.21.1, `LivingEntity.getPassengerRidingPosition`.

Conta per §2: un aggancio che cambia solo il campo, come gli eventi dei loader, lascia questi percorsi alle dimensioni vanilla.

---

## 2. Cosa offrono i loader senza mixin, e l'attributo `scale`

### 2.1 Forge 1.20.1: `EntityEvent.Size`

- **Porta l'altezza degli occhi:** `getNewSize`/`setNewSize(EntityDimensions)`, `setNewSize(size, true)` (ricalcola gli occhi con `Entity.getEyeHeightAccess`), `getNewEyeHeight`/`setNewEyeHeight(float)`, sul bus `MinecraftForge.EVENT_BUS`.
- **Dove scatta (bytecode Fo20):** solo nel costruttore di `Entity` (`ForgeEventFactory.getEntitySizeForge(Entity, Pose, EntityDimensions, float)`) e in `refreshDimensions()` (variante a 5 argomenti). Il risultato va nei campi `dimensions` ed `eyeHeight`. `fixupDimensions` non lo fa scattare.
- **Dove non scatta:**
  - `getDimensions(Pose)`, `getBoundingBoxForPose`, `canEnterPose`, `getLocalBoundsForPose`, la posizione d'uscita dai portali e `getEyeHeight(Pose)` restano vanilla.
  - Forge offre `getDimensionsForge(Pose)`, ma è solo un alias di `getDimensions`.
  - La variante a 5 argomenti di `getEntitySizeForge` è `@Deprecated(forRemoval = true)`, con il commento "Remove Entity Eye/Size hooks, as they need to be redesigned".

### 2.2 NeoForge 21.1: `EntityEvent.Size`

- Solo `getNewSize`/`setNewSize(EntityDimensions)`: gli occhi stanno dentro `EntityDimensions`.
- Scatta come su Forge: costruttore di `Entity` e `refreshDimensions()` (`EventHooks.getEntitySizeForge`), con gli stessi buchi: `getDimensions`, `canPlayerFitWithinBlocksAndEntitiesWhen`, `wouldNotSuffocateAtTargetPose`, smonto, portali.

**Conseguenza per gli eventi.** In un corridoio alto 2 il gioco valuta "in piedi ci sta?" con il box vanilla di 1,8 e lascia il giocatore in piedi; il box reale, dall'evento, è alto 2,97. La testa entra nel soffitto, gli occhi (2,67) sono nel blocco e scatta il soffocamento; il client spinge solo in orizzontale. Lo stesso vale per lo smonto da un veicolo e per l'uscita da un portale. **Gli eventi non coprono il bisogno**, quindi la regola di §3 del design ("mixin solo dove nessun evento copre il bisogno") ammette i mixin anche su Forge e NeoForge.

### 2.3 Fabric e Architectury

- **Fabric API:** nessuna API per le dimensioni di una singola entità. `fabric-object-builder-api-v1` (11.1.5 e 15.2.1) ha solo le dimensioni per tipo di entità (`FabricEntityTypeBuilder.dimensions`). Nei due `fabric-entity-events-v1` del progetto (1.6.1, 1.8.0) non c'è un evento di cambio dell'equipaggiamento.
- **Architectury** (9.2.14 e 13.0.11): nessun riferimento a `EntityDimensions`, `refreshDimensions` o all'evento `Size` nei sorgenti dei quattro jar.
- Architectury offre però `TickEvent.PLAYER_PRE`/`PLAYER_POST`, che scattano **per ogni `Player` sui due lati**:
  - su Fabric con il proprio `MixinPlayer` in testa e in coda a `Player.tick`;
  - su Forge da `PlayerTickEvent` START/END;
  - su NeoForge da `PlayerTickEvent.Pre/Post`.

  È il punto comune da cui ricalcolare (§4).

### 2.4 L'attributo `generic.scale` di 1.21.1

È `RangedAttribute("generic.scale", 1.0, 0.0625, 16.0).setSyncable(true)`: sincronizzato a chi vede il giocatore e al giocatore stesso. Il ricalcolo è automatico (`appliedScale` in `LivingEntity.tick`). Lo leggono, attraverso `LivingEntity.getScale()`, solo questi metodi (scansione di tutte le classi di V21 che chiamano `getScale()F`):

| Cosa scala | Dove |
|---|---|
| Box, occhi, attacchi (targhetta, veicolo, passeggero) per ogni posa tranne il sonno | `LivingEntity.getDimensions` |
| Posizione dei passeggeri sopra di sé | `LivingEntity.getPassengerRidingPosition` |
| **Il modello disegnato** (`pose.scale(s, s, s)` prima di `setupRotations` e di `scale`) | `LivingEntityRenderer.render` |
| Raggio dell'ombra | `LivingEntityRenderer.getShadowRadius` |
| Distanza della telecamera in terza persona (`getMaxZoom(4.0 · scale)`) | `Camera.setup` |
| Filo della canna da pesca, sovrapposizione del blocco in faccia, anteprima nell'inventario (divide per la scala) | `FishingHookRenderer`, `ScreenEffectRenderer`, `InventoryScreen` |

**Non scala** passo, portata, salto, danno da caduta, velocità e gravità: sono attributi separati (`generic.step_height` 0,6, `player.block_interaction_range` 4,5, `player.entity_interaction_range` 3, `generic.jump_strength` 0,42, `generic.safe_fall_distance` 3, `generic.fall_damage_multiplier` 1, `generic.gravity` 0,08, tutti sincronizzati). `LivingEntity.maxUpStep()` legge solo `STEP_HEIGHT` e `Player.blockInteractionRange()` solo il suo attributo.

**Usarlo qui:**

- **Il pilota crescerebbe.** `LivingEntityRenderer.render` scala la posa prima di chiamare `PlayerRenderer.scale`, quindi pilota, layer e tuta diventerebbero 1,65 volte più grandi, e la tuta 1,65² (2,72) attorno ai piedi. Per tenere il pilota a grandezza normale si dovrebbe controscalare di 1/s in coda a `PlayerRenderer.scale`, nello stesso aggancio del sollevamento. È fattibile, ma:
  - `setupRotations(…, scale)` riceve comunque la scala (spostamento a letto, morte capovolta);
  - l'anteprima nell'inventario divide per la scala e disegnerebbe il pilota con la tuta più piccoli;
  - ogni altra mod che legge `getScale()` per il gameplay o per il disegno vedrebbe "giocatore grande 1,65".
- **Nessun controllo per posa:** accovacciati 2,475, a carponi 0,99 × 0,99, larghezza obbligata a 0,99 (§5: niente pozzi con scala a pioli).
- **Composizione:** altri modificatori di `scale` (comandi, altre mod) si moltiplicano con il nostro.
- **Solo 1.21.1:** su 1.20.1 serve comunque un altro meccanismo, quindi due sistemi da tenere allineati.
- **A favore:** sincronizzazione e ricalcolo gratuiti; telecamera, ombra e targhetta che seguono da sole.

---

## 3. Opzioni a confronto

### (a) Mixin comuni su `Player` per ingombro e occhi, con bersagli per versione (raccomandata)

**Agganci** (verificati con `javap` sui quattro jar e applicati nell'esperimento):

| Versione | Bersaglio | Nome nel jar Forge (SRG) | Nome nel jar Fabric (intermediary) |
|---|---|---|---|
| 1.20.1 | `Player.getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;`, `@At("RETURN")`, cancellabile | `m_6972_` | `method_18377` |
| 1.20.1 | `Player.getStandingEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F`, `@At("RETURN")` (copre i tre `freturn`) | `m_6431_` | `method_18394` |
| 1.21.1 | `Player.getDefaultDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;`, `@At("RETURN")`; occhi e attacchi sono dentro | (NeoForge: nomi Mojang) | `method_55694` |

**Perché funziona:**

- `canEnterPose`, `canPlayerFitWithinBlocksAndEntitiesWhen`, `refreshDimensions`, smonto, portali e `getEyeHeight(Pose)` passano tutti da questi metodi, quindi la posa forzata funziona da sola.
- Su 1.21.1 il valore passa ancora per `LivingEntity.getDimensions`: sonno a parte e `scale` di altre mod moltiplicato sopra.
- Gli eventi `Size` di Forge e NeoForge scattano dopo, in `refreshDimensions`: un'altra mod che li usa si compone sopra il nostro valore.

**Costi:**

- due bersagli diversi tra le versioni (§3.4 per dove mettere il codice);
- un'eccezione in più al "niente mixin", ora anche lato server;
- il ricalcolo va fatto noi (§4).

### (b) Eventi dei loader dove ci sono, mixin su Fabric

**Costi:**

- su Forge e NeoForge la posa forzata, lo smonto e i portali restano alle dimensioni vanilla (§2.2): giocatore in piedi con la testa nel soffitto;
- tre meccanismi diversi (evento con occhi su Forge, evento senza occhi su NeoForge, mixin su Fabric), con i mixin di Fabric comunque da scrivere su due versioni;
- l'API di Forge è deprecata.

Non risolve il problema e costa di più. **Da scartare.**

### (c) `generic.scale` su 1.21.1 più altro su 1.20.1

**Costi** (§2.4):

- controscala del disegno nel mixin client;
- anteprima dell'inventario rimpicciolita;
- nessun controllo per posa e larghezza fissa a 0,6·S;
- interazione con altre mod che leggono la scala;
- su 1.20.1 comunque i mixin di (a).

Unico vantaggio: telecamera e ombra che crescono da sole, cose che (a) può aggiungere a parte se servono. **Da scartare.**

### (d) Altre strade guardate

- **Ridefinire il tipo di entità** (`EntityType.PLAYER` con dimensioni diverse): cambia tutti i giocatori, e le dimensioni per tipo non dipendono dall'equipaggiamento. Scartata.
- **Una sola classe mixin con i bersagli delle due versioni e `require = 0`:** il fallimento diventerebbe silenzioso, contro la scelta del progetto di `defaultRequire 1` ("un bersaglio che non corrisponde fa fallire l'avvio"). Scartata.
- **Pehkui o altre librerie:** dipendenza esterna, esclusa da §8 punto 2 ("senza dipendenze esterne").

### 3.4 Dove sta il codice secondo le regole del repository

**Oggi.**

- I due mixin client stanno in `mixin/client/`.
- Configurazione: `redstone_exoframes.mixins.json`, una per versione in `src/main/resources-mc1.20.1/` (`JAVA_17`) e `src/main/resources-mc1.21.1/` (`JAVA_21`). `build.gradle.kts` aggiunge `src/main/resources-mc$mc` alle risorse della variante.
- Package `com.toxineren.exoframes.mixin`, solo sezione `client`, `required: true`, `defaultRequire: 1`, nessun refmap.
- La configurazione arriva al loader da `fabric.mod.json` (`"mixins"`), da `neoforge.mods.toml` (`[[mixins]]`) e, su Forge, da `loom.forge.mixinConfig(...)`, che la scrive in `MixinConfigs` del manifest.
- I bersagli sono uguali sulle due versioni, quindi nessuna direttiva.
- `tools/check_jars.sh` (e2) verifica la configurazione nei jar e **rifiuta oggi una sezione `mixins` o `server` non vuota** ("i mixin sono solo client"). Riconosce lo spazio dei nomi dalle stringhe `setModelProperties` e `renderArmWithItem`.

**W1, provata nell'esperimento (raccomandata).** Due classi senza direttive, `mixin/PlayerSize120Mixin` e `mixin/PlayerSize121Mixin`, nel package dei mixin ma fuori da `client/`. Ciascuna è elencata solo nella sezione `"mixins"` (comune ai due lati) della configurazione della propria versione: le configurazioni sono già per versione, ed è proprio quello che serve.

- Entrambe le classi si compilano su tutte e quattro le varianti: usano solo `Player`, `Pose`, `EntityDimensions` e `CallbackInfoReturnable`, che esistono ovunque.
- La classe dell'altra versione finisce nel jar ma non è elencata, quindi Mixin non la carica mai.
- `remapJar` stampa un avviso non bloccante per il metodo che in quella versione non esiste, per esempio `Cannot remap getDefaultDimensions(...) because it does not exist in any of the targets [net/minecraft/world/entity/player/Player]` su 1.20.1 e `Cannot remap getStandingEyeHeight(...)` su 1.21.1 Fabric.
- Nei jar le classi elencate hanno i nomi giusti: Forge `m_6972_` e `m_6431_`, Fabric 1.20.1 `method_18377` e `method_18394`, Fabric 1.21.1 `method_55694`.
- `check_sources.sh` passa (`OK sources`).
- **Da cambiare:**
  - `check_jars.sh` (e2): ammettere la sezione `mixins` e aggiungere ai marcatori Mojang i nuovi nomi;
  - `docs/compat.md` ("Mixin client", "gli unici mixin");
  - design §3.

**W2 (alternativa conforme, più cablaggio).** Una sola classe con le direttive Stonecutter in `compat/impl/mixin/`, dove le direttive sono ammesse.

- Il package `com.toxineren.exoframes.mixin` della configurazione esistente non può contenerla. Serve quindi una **seconda configurazione** con package `com.toxineren.exoframes.compat.impl.mixin`, che deve contenere solo mixin.
- La seconda configurazione va dichiarata in `fabric.mod.json`, in un secondo `[[mixins]]` di `neoforge.mods.toml` e in un secondo `loom.forge.mixinConfig`, ed è comunque per versione per `compatibilityLevel`.
- Vantaggio: nessuna classe morta nei jar. Svantaggi: più file e un package di mixin sotto `compat/impl`.

**Logica.** In entrambi i casi i mixin non hanno logica (come quelli client) e chiamano una classe comune, per esempio `frame/SuitSize`, che non può dipendere dal codice client:

- `SuitScale.S` sta in `client/` e la regola 7 di `check_sources.sh` ne vieta l'uso dal codice comune. La costante va spostata in codice comune (o in `core/`, con il suo test per la regola 1c) e `SuitScale` la rilegge.
- La decisione "indossa la tuta" lato comune è la presenza di `EXOFRAME_TORSO` nello slot del petto. `SuitLayer.wearsSuit` (client) chiede in più un riassunto leggibile: conviene che il client usi la stessa regola per l'ingombro, e il riassunto solo per il disegno.

---

## 4. Raccomandazione: (a) con W1

```java
// mixin/PlayerSize120Mixin (elencato solo in resources-mc1.20.1, sezione "mixins")
@Mixin(Player.class)
abstract class PlayerSize120Mixin {
    @Inject(method = "getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;",
            at = @At("RETURN"), cancellable = true)
    private void exoframes$suitDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        cir.setReturnValue(SuitSize.dimensions((Player) (Object) this, pose, cir.getReturnValue()));
    }
    @Inject(method = "getStandingEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F",
            at = @At("RETURN"), cancellable = true)
    private void exoframes$suitEyeHeight(Pose pose, EntityDimensions d, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(SuitSize.eyeHeight((Player) (Object) this, pose, cir.getReturnValueF()));
    }
}
// mixin/PlayerSize121Mixin (solo resources-mc1.21.1): lo stesso primo metodo su
// "getDefaultDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;"
```

**`SuitSize` (comune):**

- `dimensions(player, pose, vanilla)` = `wearsSuit ? vanilla.scale(0.8f / 0.6f, S) : vanilla`. `EntityDimensions.scale(float, float)` esiste con questa firma su 1.20.1 e 1.21.1. Le dimensioni `fixed` (sonno, morte) tornano invariate; su 1.21.1 scala anche occhi, targhetta e attacco del veicolo.
- `eyeHeight(player, pose, vanilla)` = `vanilla · S` (solo 1.20.1).
- **`wearsSuit` sicuro nel costruttore.** `Entity.<init>` chiama `getDimensions` (e su 1.20.1 `getEyeHeight`) prima che `Player.inventory` esista: senza la guardia `player.getInventory() == null → false` il gioco lancia `NullPointerException` alla creazione di ogni giocatore. Vanilla fa lo stesso su 1.21.1: `LivingEntity.getScale()` controlla `getAttributes() == null`.

**Ricalcolo** (nessun evento di equipaggiamento sui due lati e su tutti i loader):

- `TickEvent.PLAYER_PRE` di Architectury chiama `SuitSize.tick(player)` per ogni giocatore sui due lati. Confronta lo stato "indossa" con l'ultimo visto in una mappa con chiavi deboli **per identità** (`new MapMaker().weakKeys().makeMap()`) e chiama `player.refreshDimensions()` quando cambia.
  - Niente `WeakHashMap`: `Entity.equals` confronta gli id, e in giocatore singolo il giocatore del client e quello del server possono avere lo stesso id.
  - Il confronto con l'ultimo stato, invece di "il box non è quello atteso", non litiga con altre mod che cambiano il box con gli eventi `Size`.
- Sul server conviene in più chiamare `refreshDimensions()` subito dopo aver messo o tolto il segnaposto (`DebugWear.wear`/`remove`, ingresso, uscita e morte di 3b), così il box cambia nello stesso tick.

**Sincronizzazione.** Il box non si invia mai: ogni lato lo calcola dalle stesse informazioni.

- Il segnaposto del torso arriva al proprietario con la sincronizzazione degli slot del menu, e agli altri con `ClientboundSetEquipmentPacket` (a ogni cambio e all'inizio del tracciamento).
- La posa viaggia in `DATA_POSE`.
- Basta che la regola e le costanti siano identiche sui due lati: **non metterle in una config non sincronizzata.**
- Per il proprietario la differenza conta, perché il movimento lo calcola il client e il server lo convalida con il proprio box. Fino al ricalcolo sul client (al massimo un tick dopo l'arrivo dello slot) uno spostamento in uno spazio stretto può essere rifiutato ("moved wrongly"), con un piccolo scatto indietro.

**Guardia all'ingresso.**

- Il server non spinge mai fuori un giocatore (§1.1). Se nessuna posa ci sta, il giocatore resta incastrato e soffoca, perché gli occhi sono nel blocco.
- **Caso da 0,1.** Anche contro un muro, con larghezza 0,8 il box entra 0,1 nel muro. Gli angoli di `moveTowardsClosestSpace` (±0,28 dal centro) non lo vedono, mentre il box degli occhi (±0,32) sì: c'è soffocamento senza spinta, finché il giocatore non si sposta.
- **Regola:** l'ingresso (e la vestizione di debug) controlla sul server `level.noCollision(player, box in piedi della tuta nella posizione d'arrivo)`, escludendo l'entità telaio che sta per sparire, e rifiuta con un messaggio se il box non è libero.
- Di conseguenza il telaio piazzato (§5.4 del design, oggi 0,6 × 1,95) dovrebbe avere lo stesso box della tuta in piedi (0,8 × 2,97). Così piazzarlo richiede lo spazio in cui poi si entra.

**Che cosa succede se il box cresce in uno spazio stretto (provato, §9).**

- Stanza alta 2, con la vestizione dentro. Nel tick successivo `PLAYER_PRE` ricalcola: box alto 2,97, nel soffitto.
- Nello stesso tick `updatePlayerPose` non trova posto né in piedi (2,97) né accovacciato (2,475) e mette la tuta a carponi (SWIMMING): box 0,8 × 0,99, occhi 0,66.
- Senza guardia quindi il gioco non soffoca il giocatore, ma lo fa strisciare.
- **Non si entra camminando in uno spazio più basso:** il box urta l'architrave prima. Una tuta non passa una porta alta 2, nemmeno accovacciata. È la conseguenza attesa della grandezza reale (§8 punto 2 del design).

---

## 5. I numeri

S = 1,65 (verticale), larghezza 0,8 (fattore 4/3). Gli occhi sono quelli vanilla per S.

| Posa | Vanilla (L × A, occhi) | Tuta proposta (L × A, occhi) | Note |
|---|---|---|---|
| In piedi (STANDING) | 0,6 × 1,8, 1,62 | **0,8 × 2,97, 2,67** | Passa sotto 3 blocchi (margine 0,03): tunnel alti 3, interno minimo del portale del Nether (2 × 3). Salto impossibile sotto un soffitto a 3. |
| Accovacciato (CROUCHING) | 0,6 × 1,5, 1,27 | **0,8 × 2,475, 2,10** | Passa sotto 2,5 (due blocchi più una lastra inferiore), non sotto 2. |
| Nuoto, carponi, elitre, tridente (SWIMMING, FALL_FLYING, SPIN_ATTACK) | 0,6 × 0,6, 0,4 | **0,8 × 0,99, 0,66** | Sta in un buco 1 × 1 (0,99 < 1). Se Luigi non vuole una tuta che striscia in un cunicolo alto 1, basta un'altezza oltre 1 (per esempio 1,2): niente cunicoli d'acqua alti 1, e in uno spazio troppo basso il giocatore resta incastrato invece di strisciare. |
| Sonno (SLEEPING) | 0,2 × 0,2 fisse, 0,2 | invariato | Dimensioni `fixed`. Al risveglio la posizione la sceglie `BedBlock.findStandUpPosition(getType(), …)` con le dimensioni **del tipo** (vanilla): si può svegliarsi in uno spazio troppo basso (vedi §8). |
| Morte (DYING) | 0,2 × 0,2 fisse | invariato | — |
| A cavallo, in barca o carrello (posa STANDING, passeggero) | box in piedi; 1.20.1 `Player.getMyRidingOffset()` = −0,35; 1.21.1 attacco VEHICLE (0; 0,6; 0) | box in piedi della tuta; 1.21.1 attacco scalato a 0,99 (automatico, tiene i fianchi della tuta sulla sella); 1.20.1 −0,35 invariato, a meno di un terzo aggancio su `getMyRidingOffset()D` (× S = −0,58) | `updatePlayerPose` non controlla lo spazio per un passeggero: occhi a circa 2,1-2,3 sopra il binario, quindi soffocamento nei tunnel da carrello alti 2 (§8). |

**Larghezza: perché 0,8 e non 0,6·S = 0,99 o 1,4.**

- **Scala a pioli:** è spessa 3/16 (`LadderBlock`, `box(0,0,0,3,16,16)`) e lascia 13/16 = 0,8125 di spazio libero. Le stesse 0,8125 le lasciano il pannello di una porta aperta (`DoorBlock`, `box(0,0,0,16,16,3)`) e una botola aperta.
- **Con 0,8** si sale un pozzo 1 × 1 con la scala, si passa un vano largo 1 con la porta aperta (se l'altezza lo consente), si percorre un corridoio largo 1.
- **Con 0,99** si passa un corridoio largo 1 (0,99 < 1), ma **non** un pozzo con la scala né un vano con la porta aperta.
- **Con 1,4** (golem di ferro):
  - niente passaggi larghi 1, niente pozzi con la scala, niente porte;
  - sì portali (interno largo 2), carrelli e barche (la cavalcatura non guarda la larghezza);
  - i mob colpiscono da più lontano: su 1.21.1 `Mob.isWithinMeleeAttackRange` interseca il proprio box d'attacco con il box del bersaglio; su 1.20.1 `getMeleeAttackRangeSqr` aggiunge `target.getBbWidth()`;
  - più soffocamento alla vestizione vicino ai muri.
- **Contraccolpo:** non dipende dal box (`LivingEntity.knockback`).
- **Percorsi dei mob:** puntano alla posizione del giocatore, non al suo box.
- **Proiettili:** più facili da prendere su un box più grande; gli scheletri mirano a `getY(1/3)` del box, quindi alle gambe della tuta.

**Altezza degli occhi: 1,62·S = 2,67, non i 2,53 del pilota disegnato.** Gli occhi del pilota disegnato stanno a circa 1,62 + 0,914 = 2,53. La visiera della tuta (gli occhi del modello scalato di S attorno ai piedi) sta a 2,67, ed è quello che "vede" chi pilota un mech. La differenza è 0,14, dentro la testa della tuta, e in prima persona né la tuta né il pilota si vedono. Accovacciati, 2,10 cade a circa 0,1 dalla visiera accovacciata della tuta.

---

## 6. Targhetta del nome, culling, ombra, telecamera, pilota

**Targhetta del nome: si sistema da sola.**

- 1.20.1: `EntityRenderer.renderNameTag` trasla di `Entity.getNameTagOffsetY()` = `getBbHeight() + 0,5`.
- 1.21.1: `getAttachments().getNullable(NAME_TAG, 0, yRot)`, poi `y + 0,5`.
- Con la tuta: 2,97 + 0,5 = **3,47** dai piedi, circa 0,38 sopra la testa della tuta (3,09), su entrambe le versioni. Su 1.21.1 vale solo se l'aggancio deriva le dimensioni con `scale(...)` dalle vanilla: `scalable(w, h).withEyeHeight(e)` costruirebbe attacchi nuovi, con la targhetta all'altezza giusta ma il veicolo ai piedi (0) invece che scalato.
- L'avvertenza in `PilotPose` e `docs/compat.md` ("l'etichetta del nome … non sale") diventa superata.

**Culling: migliora ma non chiude.**

- `EntityRenderer.shouldRender` usa `entity.getBoundingBoxForCulling().inflate(0.5)`. `Entity.getBoundingBoxForCulling()` = `getBoundingBox()`. `LivingEntity` lo ridefinisce solo per la testa di drago (gonfia di 0,5), uguale sulle due versioni.
- In verticale con la tuta: fino a 3,47, oltre la testa della tuta (3,09).
- In orizzontale: ±0,9 (con larghezza 0,8) contro ±1,31 della tuta con i pezzi, quindi le braccia restano **0,41** fuori. Con 0,99 restano 0,315 fuori.
- Per chiudere serve un aggancio su `LivingEntity.getBoundingBoxForCulling()Lnet/minecraft/world/phys/AABB;`, con la stessa firma su V20, Fo20, V21 e Neo21: quindi un mixin **senza differenze di versione**, nella sezione `client`. Gonfiaggio orizzontale di almeno 1,31 − 0,4 − 0,5 = 0,41 per chi indossa la tuta.
- Costo: un mixin client in più. Non provato.

**Ombra.**

- 1.20.1: `PlayerRenderer` ha il campo `shadowRadius` fisso a 0,5, letto da `EntityRenderDispatcher`.
- 1.21.1: `LivingEntityRenderer.getShadowRadius` = 0,5 · `getScale()`.
- Con (a) resta 0,5 su entrambe: è un'ombra sotto i piedi della tuta, accettabile. Allargarla vuole un altro mixin client diverso per versione. Non consigliato ora.

**Telecamera.**

- `Camera.tick` porta `eyeHeight` verso `entity.getEyeHeight()` di metà della distanza per tick: alla vestizione la vista sale con morbidezza in pochi tick.
- `Camera.setup` in terza persona si allontana di `getMaxZoom(4.0)` su 1.20.1 e `getMaxZoom(4.0 · getScale())` su 1.21.1, partendo dagli occhi. `getMaxZoom` lancia raggi con scarti di ±0,1 e accorcia la distanza contro i blocchi.
- Con (a) la distanza resta 4 su entrambe, ma parte da 2,67: la vista guarda la tuta dall'alto (scatti in §9).
- Sotto un soffitto a 3 gli occhi stanno 0,33 sotto: nessun problema di piano vicino.
- Allungare la distanza:
  - NeoForge: `CalculateDetachedCameraDistanceEvent` (client);
  - Forge 1.20.1: nessun evento per la distanza (`ViewportEvent` dà solo angoli e campo visivo);
  - Fabric: un mixin.

**Pilota e tuta disegnati: nessun cambiamento obbligatorio.**

- `LivingEntityRenderer.render` disegna dalla posizione dell'entità, cioè dai piedi.
- Box e occhi non entrano nella posa del modello, né in `PilotPose.onScale` né nella scala di `SuitLayer` attorno ai piedi (y = 1,501). L'unica eccezione è il capovolgimento "Dinnerbone", che trasla di `getBbHeight() + 0,1`.
- Il sollevamento di 0,914 resta nello spazio del modello e non va toccato.
- Da aggiornare solo i commenti sulla targhetta e, se si vuole, `SuitLayer.wearsSuit`, perché usi la stessa regola comune dell'ingombro (§3.4).
- Lo scarto del pilota accovacciato (§10 del design) non cambia: è nel disegno, non nel box.

**Effetti sul codice esistente.** `DamageClassifierImpl` usa `victim.getEyeY()`, quindi le soglie si spostano da sole con la tuta:

- HEAD: sorgente a y ≥ piedi + 2,42 invece di + 1,37;
- FROM_ABOVE: tiratore sopra piedi + 3,67 invece di + 2,62.

È coerente con una testa a 3 blocchi, ma cambia la ripartizione del danno sui pezzi rispetto ai test di oggi.

---

## 7. Passo, portata, caduta e altre meccaniche (solo fatti e costi)

| Meccanica | Vanilla | Cosa stona con una tuta alta 3 | 1.21.1 (Fabric e NeoForge) | Forge 1.20.1 | Fabric 1.20.1 |
|---|---|---|---|---|---|
| Passo | 0,6 (`Entity.maxUpStep`; 1.21.1 `LivingEntity.maxUpStep()` legge `generic.step_height`) | Fermata da un gradino di un blocco: va saltato | Modificatore su `generic.step_height` (sincronizzato), come i 4 di `FrameAttributes`: **economico** | Attributo `forge:step_height_addition` (`ForgeMod.STEP_HEIGHT_ADDITION`, sincronizzato): **economico** | `Entity.setMaxUpStep(float)` pubblico, non sincronizzato: va impostato sui due lati (il movimento lo calcola il client) nel tick di §4; si pesta i piedi con altre mod. **Economico, meccanismo diverso** |
| Portata sui blocchi | 4,5 (5 in creativa), misurata dagli occhi (`canInteractWithBlock`: `AABB.distanceToSqr(getEyePosition())`) | Con gli occhi a 2,67 il cerchio raggiungibile sul pavimento passa da raggio 4,20 a **3,62** | `player.block_interaction_range`: **economico** | `forge:block_reach`: **economico** | Fissa: client `MultiPlayerGameMode.getPickRange()` (4,5 o 5); il server ammette fino a 6 dagli occhi (`MAX_INTERACTION_DISTANCE` = 36 = 6²). Fino a 6 basta **un mixin client** |
| Portata sulle entità | 3 dagli occhi | Un mob alto 0,7 si colpisce solo entro circa 2,46 blocchi in orizzontale invece di 3,06 | `player.entity_interaction_range`: **economico** | `forge:entity_reach`: **economico** | Fissa in `GameRenderer.pick` (soglia 3² = 9): **un mixin client** |
| Salto | `jump_strength` 0,42, circa 1,25 blocchi | Con passo 0,6 serve per ogni gradino; con un passo alto conta poco | `generic.jump_strength`: **economico** | `LivingJumpEvent` (si cambia il moto dopo il salto): **medio** | `getJumpPower()` protetto: **mixin** |
| Caduta | Sicura fino a 3, poi 1 per blocco | La caduta non dipende dall'altezza; già ripartita sui pezzi (`DamageKind.FALL`) | `generic.safe_fall_distance`, `generic.fall_damage_multiplier`: **economico** | `LivingFallEvent`: **economico** | Nessun attributo: dal percorso del danno già esistente (classificazione FALL) o con un mixin su `calculateFallDamage` |
| Acqua e respiro | Occhi in acqua a 1,62 | Con occhi a 2,67 la tuta respira e cammina sul fondo in acqua profonda fino a circa 2,6; inizia a nuotare solo se gli occhi sono sotto (`isUnderWater`). Ne viene un mech "che guada": da decidere se è voluto | — | — | — |
| Gravità | 0,08 | — | `generic.gravity` | `forge:entity_gravity` | Nessuna |
| Distanza della telecamera | 4 | Con la tuta in terza persona riempie lo schermo | NeoForge `CalculateDetachedCameraDistanceEvent`; Fabric mixin | Mixin | Mixin |
| Velocità, contraccolpo, armatura | — | Già in `FrameAttributes` | — | — | — |

---

## 8. Rischi aperti

- **Carrelli e barche sotto soffitti bassi.** Da passeggero la posa non si adatta allo spazio: occhi a circa 2,1-2,3 sopra il fondo del carrello, quindi soffocamento (`isInWall`, 1 di danno ogni mezzo secondo) nei tunnel da carrello alti 2. Dedotto dal bytecode, non provato. Opzioni: vietare di salire con la tuta, abbassare il punto di seduta, o accettare.
- **Risveglio dal letto** con `findStandUpPosition` per le dimensioni del tipo: può mettere la tuta in uno spazio dove finisce a carponi o incastrata. Opzione semplice: niente sonno con la tuta (Forge `PlayerSleepInBedEvent`, Fabric `EntitySleepEvents`, NeoForge `CanPlayerSleepEvent`; Architectury non ha un evento comune per il sonno, da verificare).
- **Giocatori remoti sul client.** Il ricalcolo per `RemotePlayer` passa dallo stesso `Player.tick` e quindi da `PLAYER_PRE` sui tre loader, ma senza un secondo giocatore non è stato visto. Se mancasse, gli altri vedrebbero box e targhetta vecchi fino a un cambio di posa.
- **Jar di produzione.** Le prove sono nell'ambiente di sviluppo. I nomi nei jar rimappati sono quelli giusti, ma l'applicazione nel jar finale resta per la checklist sul PC, come per i mixin client.
- **Avviso di `remapJar` con W1** per la classe dell'altra versione: va documentato, e `check_jars.sh` (e2) va adattato.
- **Altre mod di grandezza.** Pehkui o mod che usano gli eventi `Size` si compongono sopra; non provato.
- **Classificazione del danno:** le soglie HEAD e FROM_ABOVE si spostano (§6); i test in mondo di 3c vanno scritti con la tuta indossata.
- **Il telaio piazzato** (§5.4 del design) va ridimensionato a 0,8 × 2,97, altrimenti la guardia d'ingresso rifiuta spazi che il telaio occupa già.
- **Accovacciarsi più in basso per passare le porte** (≤ 1,95 con occhi ≤ circa 1,8): possibile con la stessa tabella, ma la tuta disegnata attraverserebbe l'architrave di circa 0,6 e la vista scenderebbe di quasi un blocco. Proposta: no; decisione di Luigi.

---

## 9. Esperimento: cosa è provato e cosa no

**Dove.** Clone usa e getta nello scratchpad della sessione (`m3b/spike-size/`) (ramo locale `spike-size`, remoto rimosso, nessun push), da `a934811`. Patch completa: `2026-11-13-m3b-grandezza-reale.patch`, in questa cartella.

**Cosa contiene:**

- `frame/SuitSize` (comune; `HEIGHT_SCALE` 1,65, larghezza 0,8);
- `mixin/PlayerSize120Mixin` e `mixin/PlayerSize121Mixin` (W1), ciascuno nella sezione `"mixins"` della configurazione della propria versione;
- `TickEvent.PLAYER_PRE.register(SuitSize::tick)` in `RedstoneExoframes.init()`;
- due test in mondo in `ExoframeGameTests`;
- una riga di log `SPIKE refresh` a ogni ricalcolo.

**Test in mondo** (`bash tools/run_gametests.sh <variante>`): **12 test obbligatori passati su tutte e quattro le varianti** (1.21.1-fabric, 1.20.1-forge, 1.21.1-neoforge, 1.20.1-fabric), con righe di log identiche sulle quattro:

```
SPIKE size before=pose=STANDING w=0.6000 h=1.8000 eye=1.6200 crouchEye=1.2700
         | wear,no refresh=pose=STANDING w=0.6000 h=1.8000 eye=1.6200 crouchEye=2.0955
         | suited=pose=STANDING w=0.8000 h=2.9700 eye=2.6730 crouchEye=2.0955
SPIKE size after remove=pose=STANDING w=0.6000 h=1.8000 eye=1.6200 crouchEye=1.2700
SPIKE low ceiling plain=pose=STANDING w=0.6000 h=1.8000 eye=1.6200
         | tick1=pose=SWIMMING w=0.8000 h=0.9900 eye=0.6600 | tick2=pose=SWIMMING w=0.8000 h=0.9900 eye=0.6600
```

- `spikeSuitGrowsBoxAndEyes`: con il segnaposto del torso, sul server, box 0,8 × 2,97 (anche nel box ricostruito: `maxY − getY()`), occhi 2,673 e occhi accovacciati 2,0955; tolto il segnaposto si torna a 0,6 × 1,8 e 1,62. La seconda colonna mostra che senza `refreshDimensions` il box non cambia.
- `spikeSuitCrawlsUnderLowCeiling`: giocatore finto tra pavimento e soffitto distanti 2 blocchi, con `ServerPlayer.doTick()`. Senza tuta resta in piedi; con la tuta, al primo tick, `PLAYER_PRE` ricalcola e `updatePlayerPose` lo mette a carponi (box 0,8 × 0,99, occhi 0,66).

**Jar rimappati** (`:1.21.1-fabric:remapJar`, `:1.20.1-forge:remapJar`, `:1.20.1-fabric:remapJar`):

- build riuscite;
- tre avvisi non bloccanti `Cannot remap ... does not exist in any of the targets` per la classe dell'altra versione;
- nomi nelle classi elencate: Forge `m_6972_` e `m_6431_`, Fabric 1.20.1 `method_18377` e `method_18394`, Fabric 1.21.1 `method_55694`.
- `bash tools/check_sources.sh`: `OK sources`. `check_jars.sh` non è stato lanciato: la sua regola e2 oggi rifiuta per costruzione la sezione `mixins`.

**Client senza schermo** (`tools/client/client_shot.sh`, con `EXO_ACCEPT_EULA=true`, colonna di lana alta 3 e `exoframe debug wear rottame 2`), su 1.21.1-fabric e 1.20.1-forge:

- uscita 0 e `log-check.txt` senza avvisi del mod o dei mixin;
- nel log del client `SPIKE refresh client=true player=Exo suited=true w=0.8 h=2.9699998 eye=2.6729999`, nello stesso istante del server: il giocatore locale ricalcola il box sul client tramite `PLAYER_PRE`, su Fabric 1.21.1 e su Forge 1.20.1;
- nessun "moved wrongly" nei log del server;
- gli scatti (`m3b/shot-1.21.1-fabric/` nello scratchpad della sessione e `…/shot-1.20.1-forge/`, `back.png`, `front.png`, `quarter.png`) mostrano la telecamera in terza persona più alta, che guarda la tuta dall'alto.

**Non provato:**

- **giocatori remoti** visti da un altro client (ricalcolo, targhetta a 3,47);
- **targhetta** a schermo (la propria non si vede);
- **culling** con le braccia fuori dal box;
- **carrelli e barche** in tunnel bassi, risveglio dal letto, portali, smonto;
- **jar di produzione** sul PC;
- **NeoForge e Fabric 1.20.1 sul client** (provati solo nei test in mondo del server);
- **W2** (seconda configurazione);
- l'aggancio facoltativo su `getMyRidingOffset` (1.20.1);
- **passo, portata, salto e caduta**: solo fatti dal bytecode, nessuna prova.
