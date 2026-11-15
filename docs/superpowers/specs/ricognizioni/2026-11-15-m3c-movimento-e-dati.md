# M3c, ricognizione: movimento della tuta (acqua, passo, scatto) e dati per versione

Ricognizione per la parte 3c (design M3 §5.6, §5.8 "Dati per versione", §8 punto 2(d)). Nulla è stato cambiato sotto `/home/user/exosuits`. I fatti vengono dal bytecode (`javap -c -p`) dei jar che il progetto ha già risolto e da esecuzioni del client vero senza schermo (`tools/client/client_shot.sh`) e del server dedicato sulle quattro varianti; l'esperimento è nel clone usa e getta `m3c-ricognizione/repo`, ramo `ricognizione-3c` (da `main` 2e35aa6), e la sua patch è accanto a questo documento (`2026-11-15-m3c-movimento-e-dati.patch`, §5).

Segni usati: **[javap]** fatto letto nel bytecode, **[prova]** misurato in un'esecuzione, **[deduzione]** ragionamento non provato a parte.

## Fonti

- **Minecraft (nomi Mojang)**, sotto `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/`, gli stessi quattro jar della ricognizione 3b: **V20** (`minecraft-merged` 1.20.1), **Fo20** (`forge-1.20.1-47.4.29-minecraft-merged`), **V21** (`minecraft-merged` 1.21.1), **Neo21** (`neoforge-21.1.266-minecraft-merged`). Le interfacce dei loader (`IForgeEntity`, `IForgeLivingEntity`, `IEntityExtension`, `ILivingEntityExtension`) dagli stessi jar.
- Classi lette: `Entity`, `LivingEntity`, `Player`, `LocalPlayer`, `ServerPlayer`, `RemotePlayer`, `ServerGamePacketListenerImpl`, `BubbleColumnBlock`, `Attributes` (1.21.1), `RecipeManager`, `LootDataType`, `TagManager`, `Registries`.
- Dove non è detto altro, V20 e Fo20 hanno lo stesso bytecode nei metodi citati, e così V21 e Neo21.
- Esecuzioni: cartelle sotto `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m3c-ricognizione/runs/` (§4).

## Riassunto

| Domanda | 1.20.1 Forge | 1.20.1 Fabric | 1.21.1 Fabric | 1.21.1 NeoForge | Provato |
|---|---|---|---|---|---|
| 1(a) acqua: affonda e cammina sul fondo | mixin comune `PlayerMovementMixin` su `Player.isAffectedByFluids()`, `isPushedByFluid()`, `updateSwimming()` (stessa classe sulle quattro, nessuna direttiva) | idem | idem | idem | sì, quattro varianti |
| 1(b) passo di un blocco | `setMaxUpStep(1.0)` sul solo giocatore locale (client) | idem | modificatore +0,4 su `generic.step_height`, sul server (sincronizzato) | idem | sì, quattro varianti |
| 1(c) scatto spento con l'energia bassa | mixin client `LocalPlayerSprintMixin` su `LocalPlayer.hasEnoughFoodToStartSprinting()` + `setSprinting(false)` sul server per i conti | idem | idem | idem | sì, quattro varianti; il solo lato server non basta (provato su 1.21.1 Fabric) |
| 2 dati per versione | già pronto: `resources.srcDir("src/main/resources-mc$mc")` in `build.gradle.kts`; nessuna modifica alla build | idem | idem | idem | sì: ricetta, tabella di bottino e tag caricati sulle quattro, assenti dai jar dell'altra versione |

---

## 1. Movimento di chi indossa la tuta

### 1(a) In acqua niente nuoto: la tuta affonda e cammina sul fondo

**Risposta.** Il gioco non offre un attributo o un evento che tolga a un giocatore il galleggiamento e il nuoto su tutte e quattro le varianti: l'unico punto comune è il metodo pubblico `Player.isAffectedByFluids()` (stesso corpo, `!abilities.flying`, su V20, Fo20, V21 e Neo21), che fa da interruttore unico a tre cose: il ramo "in acqua" di `LivingEntity.travel` (attrito dell'acqua, affondamento lento, Profondità/Grazia del delfino, risalita contro le pareti), il blocco dei salti di `LivingEntity.aiStep` (risalita con Spazio, ma anche il salto da terra) e la discesa con Shift di `LocalPlayer.aiStep`. Restituire falso per la tuta che tocca acqua (non lava), più `isPushedByFluid()` falso (la corrente) e il nuoto spento in coda a `Player.updateSwimming()`, dà una regola semplice che regge ovunque: **con la tuta, l'acqua non conta per il movimento**. Un solo mixin comune, senza direttive, elencato nella sezione `mixins` delle due configurazioni, con una chiamata a codice comune per aggancio; gira sui due lati (lo stesso codice decide sul client, che muove il giocatore, e sul server, che ne tiene posa e bandierine), non produce "moved wrongly" e costa una lettura dello slot del petto per chiamata. Provato sulle quattro varianti: tenendo Spazio sul fondo la tuta non sale (Y resta −60,0), sul fondo cammina 8,5-8,6 blocchi in 2 s (come a terra) contro 3,6 del controllo senza tuta.

**La regola per chi prova** (vale identica sulle quattro varianti):

> Con la tuta indossata, mentre tocca acqua (non lava): non nuota (niente posa a nuoto, nemmeno correndo sott'acqua), non sale e non salta tenendo Spazio, cade come all'aria aperta fino al fondo e sul fondo cammina e corre alla velocità di terra; la corrente non la sposta. Restano come nel gioco base: respiro (gli occhi sono a 2,67 dai piedi), fuoco spento e caduta azzerata dall'acqua, colonne di bolle (sollevano anche la tuta), lava. Per uscire da una vasca serve una sponda alta al più un blocco (il passo di 1(b)), una scala di blocchi, oppure uscire dal telaio.

Controlli numerici (dalle esecuzioni, §4): in una vasca profonda 3, Spazio tenuto 3 s → Y del fondo; da 2 blocchi sopra il fondo, dopo 1,5 s → Y del fondo; avanti 2 s sul fondo → almeno 8 blocchi; corrente 3 s da fermi → spostamento 0; in acqua profonda 1, Spazio 2 s → Y invariata; aria da 300 a circa 196 in 5 s sott'acqua.

#### Evidenze dal bytecode

**Che cosa mette un giocatore a nuotare** [javap]

- `Player.updateSwimming()` (pubblico, sui quattro jar): se `abilities.flying` → `setSwimming(false)`, altrimenti `super.updateSwimming()`. Un solo `return`: l'aggancio `@At("TAIL")` cade dopo tutto.
- `Entity.updateSwimming()` (V20 = V21): se già a nuoto, resta a nuoto finché `isSprinting() && isInWater() && !isPassenger()`; altrimenti entra a nuoto se `isSprinting() && isUnderWater() && !isPassenger()` e il fluido in `blockPosition` è acqua. Fo20 e Neo21 passano invece per `canStartSwimming()` (`IForgeEntity`/`IEntityExtension`: tipo di fluido agli occhi e `canSwimInFluidType`), ma il punto d'ingresso resta `Player.updateSwimming()`.
- Quindi si nuota **solo correndo con gli occhi sott'acqua**. Con la tuta gli occhi sono a 2,67: in acqua profonda meno di 2,67 non si nuota comunque; più a fondo sì.
- **Che cosa fa già il mixin della grandezza:** con `isSwimming()` vero `Player.updatePlayerPose()` sceglie la posa SWIMMING e `SuitSize.dimensions` la porta a 0,8 × 0,99 con occhi a 0,66. Senza l'aggancio sul nuoto, la tuta che corre sott'acqua diventerebbe alta 0,99 e passerebbe da buchi 1 × 1 nuotando. Con l'aggancio la posa SWIMMING resta solo per il carponi forzato da un soffitto basso (§4.8 del design), che non usa la bandierina del nuoto.
- `Player.travel` aggiunge la spinta verticale verso dove si guarda solo se `isSwimming()`: spento il nuoto, sparisce anche quella.

**Che cosa fa salire tenendo Spazio** [javap]

- `LivingEntity.aiStep` (V20, righe 461-637): `if (jumping && isAffectedByFluids()) { … } else noJumpDelay = 0`. Dentro: altezza del fluido (`getFluidHeight`), `getFluidJumpThreshold()`; in acqua e (non a terra oppure altezza > soglia) → `jumpInLiquid(WATER)`; in lava idem; altrimenti, a terra (o in acqua bassa sotto la soglia) e `noJumpDelay == 0` → `jumpFromGround()`.
- `LivingEntity.jumpInLiquid(TagKey)` (V20, V21): `deltaMovement + (0; 0,04; 0)`, costante. Su Fo20 e Neo21 `aiStep` chiama invece `jumpInFluid(FluidType)` (`IForgeLivingEntity`/`ILivingEntityExtension`): 0,04 × attributo `forge:swim_speed`/`neoforge:swim_speed`. **Il salto in acqua passa da metodi diversi per loader**: agganciare quello sarebbe per loader; `isAffectedByFluids()` invece è chiamato da `aiStep` sui quattro jar prima di ogni ramo.
- Conseguenza della regola: con `isAffectedByFluids()` falso **la tuta in acqua non salta affatto**, nemmeno in acqua bassa, perché anche `jumpFromGround` è dentro quel blocco (è lo stesso motivo per cui chi vola in creativa non salta).
- `LocalPlayer.aiStep` (V20 900-907, V21 933-940): `isInWater() && input.shiftKeyDown && isAffectedByFluids()` → `goDownInWater()` (Fo20/Neo21: `sinkInFluid(FluidType)`). Con la regola non serve: si cade già.

**Che cosa rallenta il cammino sott'acqua e che cosa offre il gioco per accelerarlo** [javap]

Ramo acqua di `LivingEntity.travel` (V20 63-361, V21 68-347), entrato solo se `isInWater() && isAffectedByFluids() && !canStandOnFluid(...)` (Fo20/Neo21: `(isInWater() || isInFluidType(fluido) && tipo ≠ lava) && isAffectedByFluids() && …`, poi `moveInFluid` per i fluidi delle mod):

- attrito orizzontale `f` = `getWaterSlowDown()` = 0,8 (0,9 se si corre); spinta `moveRelative(0,02)`; con `f` = 0,8 la velocità di regime è circa 0,02/(1−0,8) ≈ 0,1 blocchi/tick (2 blocchi/s; misurato 1,8 blocchi/s, §4);
- **Profondità** (Depth Strider): 1.20.1 `EnchantmentHelper.getDepthStrider` (livello, massimo 3), 1.21.1 attributo `generic.water_movement_efficiency` (l'incantesimo ne è diventato un modificatore); dimezzata se non a terra; avvicina `f` a 0,546 e la spinta a `getSpeed()`, cioè al cammino di terra;
- **Grazia del delfino**: `f` = 0,96;
- Forge/NeoForge: la spinta è moltiplicata per `swim_speed` (Fo20 320-343, Neo21 250-264);
- verticale: `vec.multiply(f; 0,8; f)` e poi `getFluidFallingAdjustedMovement(gravità, …)`: se non si corre, y − gravità/16 (0,005 per tick): si affonda piano, circa 0,4 blocchi/s; **correndo, nessuna gravità in acqua**;
- `horizontalCollision && isFree(x; y + 0,6 − …; z)` → y = 0,3: è la "risalita" contro la sponda che permette di uscire dall'acqua senza saltare.

Con la regola nessuno di questi conta per la tuta: il ramo non si apre e `travel` usa quello di terra (gravità 0,08, attrito del blocco sotto i piedi). Nessun attributo esistente su tutte e quattro sostituisce l'interruttore: `swim_speed` a 0 su Forge/NeoForge toglie la risalita ma anche la spinta orizzontale, e lascia attrito e affondamento lento; su Fabric la risalita (0,04) è costante; `generic.gravity` (1.21.1) e `forge:entity_gravity` cambierebbero anche il salto a terra.

**Corrente** [javap]: `Entity.updateInWaterStateAndDoWaterCurrentPushing` chiama `updateFluidHeightAndDoFluidPushing(WATER, 0,014)`, che spinge se `isPushedByFluid()` (sovrascritto da `Player`: `!abilities.flying`). Fo20 e Neo21 passano per `isPushedByFluid(FluidType)` di `IForgeEntity`/`IEntityExtension`, che chiama `Entity.isPushedByFluid()` e quindi l'aggancio. La stessa routine fa `resetFallDistance()` e `clearFire()` a ogni tick in acqua: la tuta che cade in acqua non prende danno da caduta, anche se cade "come all'aria".

**Colonne di bolle** [javap]: `BubbleColumnBlock.entityInside` chiama `Entity.onAboveBubbleCol(drag)` o `Entity.onInsideBubbleColumn(drag)` (y = min(0,7; y + 0,06), oppure max(−0,3; y − 0,03) sopra il magma), non passando da `isAffectedByFluids()`. [prova] la tuta sale fino al soffitto del tubo come senza tuta. Proposta: lasciarlo (un ascensore a bolle costruito apposta resta un ascensore).

**Lava** [prova]: la regola esclude la lava (`!isInLava()` nel codice comune): in lava la tuta si muove come nel gioco base (sale tenendo Spazio, misurato: Y da −62 a circa −60,4 in 2 s, con e senza tuta). Se la si includesse, la tuta camminerebbe in lava a velocità di terra: un vantaggio per uscirne, da decidere con Luigi.

**Respiro** [javap, prova]: il design non chiede di cambiarlo. `LivingEntity.baseTick` toglie aria quando gli occhi sono in acqua (non in una colonna di bolle) salvo Respirazione o Respirazione acquatica. Con la tuta gli occhi sono a 2,67: in acqua profonda fino a 2,6 circa la tuta respira e cammina; più a fondo l'aria cala come nel gioco base (misurato: da 298-299 a 195-197 in 5 s, uguale al controllo) e dopo circa 15 s arriva il danno da annegamento. Leve se servisse: 1.21.1 `generic.oxygen_bonus`; ovunque l'effetto Respirazione acquatica o l'incantesimo Respirazione sull'elmo (che oggi è il segnaposto). Anche lo scavo sott'acqua resta /5 (`Player.getDestroySpeed`, 1.21.1 attributo `player.submerged_mining_speed`).

**Correre sul fondo** [javap V20 `LocalPlayer.aiStep` 424-645, prova]: si comincia a correre se a terra o con gli occhi sott'acqua; si smette se `isInWater() && !isUnderWater()`. Quindi con la tuta: in acqua bassa (occhi fuori) niente corsa, come nel gioco base mentre si guada; sotto i 2,67 la corsa c'è ed è quella di terra (misurato 8,2-8,5 blocchi in 1,5 s; senza tuta si nuota, 4,2-4,4).

**Lato server** [javap]: `ServerGamePacketListenerImpl.handleMovePlayer` rifà `player.move(PLAYER, delta)` con il delta mandato dal client e avvisa "moved wrongly" solo se lo scarto supera 0,0625, "moved too quickly" se il delta al quadrato supera 100 (300 in volo); la posizione la decide il client. `isAffectedByFluids`, `isPushedByFluid` e `updateSwimming` girano sui due lati (`updateSwimming` dentro `Entity.baseTick`): con l'aggancio comune posa, bandierina del nuoto (sincronizzata in `DATA_SHARED_FLAGS`) e fisica coincidono, anche per chi guarda (i `RemotePlayer` passano dallo stesso `Player`).

#### Evidenze dalle esecuzioni [prova]

Vasca di vetro 5 × 15, acqua profonda 3 (fondo a Y −60, pelo dell'acqua a −57,11; gli occhi della tuta a −57,33, sott'acqua); scalino di pietra alto 1; pista piana per la corsa. Tutto in sopravvivenza, una esecuzione per variante con la tuta (`exoframe debug wear rottame`) e poi senza (`wear via`) come controllo. Posizioni dal server con `data get entity Exo Pos` (in `rcon.log` di ogni cartella). Cartelle `runs/<variante>-a`.

| Prova (tuta) | 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric | 1.21.1-neoforge |
|---|---|---|---|---|
| Teletrasportata 2 sopra il fondo, Y dopo 1,5 s | −60,0 | −60,0 | −60,0 | −60,0 |
| Sul fondo, Spazio tenuto 3 s, Y subito dopo | −60,0 | −60,0 | −60,0 | −60,0 |
| Sul fondo, avanti (W) 2 s, blocchi percorsi | 8,49 | 8,49 | 8,59 | 8,59 |

| Controllo (senza tuta) | 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric | 1.21.1-neoforge |
|---|---|---|---|---|
| Y dopo 1,5 s da −58 | −58,65 | −58,65 | −58,68 | −58,65 |
| Spazio 3 s, Y | −57,49 | −57,49 | −57,49 | −57,21 |
| Avanti 2 s sul fondo | 3,61 | 3,61 | 3,61 | 3,61 |

Casi limite, cartelle `runs/<variante>-b` (trincea d'acqua profonda 1 con sponde alte 1; acqua che scorre da una sorgente su terreno piatto; tubo 1 × 1 d'acqua alto 7 su sabbia delle anime con il tetto di vetro; vasca di lava profonda 2, con Resistenza al fuoco):

| Prova | Tuta (forge / fabric 1.20 / fabric 1.21 / neoforge) | Senza tuta (stesso ordine) |
|---|---|---|
| Acqua profonda 1, Spazio 2 s, Y (fondo −61) | −61,0 / −61,0 / −61,0 / −61,0 | −60,44 / −60,40 / −60,44 / −60,40 |
| Acqua profonda 1, avanti 1,5 s verso la sponda alta 1 | esce: Y −60, X 28,83 / 28,68 / 28,93 / 28,83 | resta: Y −61, X 24,70 (contro la sponda) |
| Corrente, fermi 3 s, X (partenza 42,5) | 42,5 / 42,5 / 42,5 / 42,5 | 46,04 / 46,00 / 46,02 / 46,02 |
| Colonna di bolle, Y dopo circa 2 s (tetto a −53) | −55,97 (testa al tetto) su tutte | −54,80 (testa al tetto) su tutte |
| Aria sul fondo della vasca da 3, 5 s | 298→197 / 298→196 / 299→195 / 299→197 | 298→196 / 298→197 / 298→196 / 298→196 |
| Corsa (Ctrl+W) 1,5 s sul fondo, blocchi | 8,23 / 8,36 / 8,51 / 8,23 | 4,36 / 4,17 / 4,36 / 4,36 (nuoto) |
| Lava profonda 2, Spazio 2 s, Y (fondo −62) | −60,42 / −60,38 / −60,43 / −60,38 | −60,42 / −60,39 / −60,44 / −60,38 |

Server: nessun "moved wrongly" in nessuna esecuzione. Ci sono avvisi "moved too quickly", da 1 a 6 per esecuzione, sia nella fase con la tuta sia nel controllo senza tuta, uno subito dopo un `tp` via RCON e con lo spostamento del `tp` (per esempio `12.0,2.0,0.0` dopo `tp Exo 12.5 -58 0.5` da 0,5 −60 0,5): sono un artefatto del teletrasporto con il giocatore in moto [deduzione dal valore e dai tempi], non del meccanismo; un test che li conta deve evitare i `tp` a giocatore in moto o ignorarli. `log-check.txt` vuoto in tutte (nessun errore di mixin).

#### Che cosa deve fare il piano, per variante

Uguale sulle quattro varianti:

- **Mixin** `mixin/PlayerMovementMixin` (nome da decidere), `@Mixin(Player.class)`, nella sezione `mixins` (comune ai due lati) di **entrambe** le `redstone_exoframes.mixins.json`: le tre firme esistono identiche su V20, Fo20, V21 e Neo21, quindi niente direttive e niente esclusione in `build.gradle.kts`.
  - `isAffectedByFluids()Z`, `@At("RETURN")`, cancellabile → `cir.setReturnValue(SuitMovement.affectedByFluids(player, cir.getReturnValueZ()))`;
  - `isPushedByFluid()Z`, `@At("RETURN")`, cancellabile → `SuitMovement.pushedByFluid(...)`;
  - `updateSwimming()V`, `@At("TAIL")` → `SuitMovement.afterUpdateSwimming(player)` (`setSwimming(false)` se indossa la tuta).
  - Nomi nei jar (dall'esperimento, `remapJar` senza "Cannot remap"): Fabric `method_29920`, `method_5675`, `method_5790` (uguali su 1.20.1 e 1.21.1); Forge `m_6129_`, `m_6063_`, `m_5844_`; NeoForge i nomi Mojang.
- **Logica** in codice comune senza direttive (`frame/SuitMovement` o dentro `SuitSize`): `affectedByFluids = vanilla && !(wearsSuit && isInWater() && !isInLava())`; `pushedByFluid = vanilla && !wearsSuit`.
- **Lati:** entrambi; nessun pacchetto (il segnaposto del torso è già sui due lati, come per la grandezza).
- **Costo per tick:** `isAffectedByFluids` 2-3 chiamate per giocatore e tick (`aiStep`, `travel`, `LocalPlayer.aiStep`), `isPushedByFluid` 2 (acqua e lava), `updateSwimming` 1; ciascuna una lettura dello slot del petto e un confronto d'item (`wearsSuit`). Trascurabile.
- **Da adattare:** `tools/check_jars.sh` (e2) oggi vuole nella sezione `mixins` solo il mixin della grandezza e nella sezione `client` solo i tre mixin di oggi, e riconosce i nomi Mojang di NeoForge da marcatori fissi: con la patch fallisce esattamente su questo (provato, §4); vanno aggiunti la nuova classe e i nuovi marcatori (`isAffectedByFluids`, `hasEnoughFoodToStartSprinting`). Anche `docs/compat.md` ("Mixin") e il design §3.
- **Test:**
  - in mondo (GameTest, lato server): un giocatore finto con la tuta in acqua profonda 3 → `isAffectedByFluids()` falso, `isPushedByFluid()` falso; con `setSprinting(true)` e gli occhi sott'acqua, dopo un tick `isSwimming()` falso; senza tuta i valori del gioco base; in lava `isAffectedByFluids()` vero;
  - client vero (come `tools/client/ricognizione-3c/prova.sh` e `prova_b.sh`): i numeri delle tabelle sopra come soglie (Y del fondo dopo Spazio 3 s; almeno 8 blocchi in 2 s sul fondo; 0 di deriva nella corrente; Y invariata con Spazio in acqua profonda 1);
  - checklist sul PC: le stesse prove a mano, più l'aspetto (niente posa a nuoto).

#### Che cosa resta incerto

- **Nessun salto in acqua.** È la conseguenza diretta dell'interruttore unico. Insieme al passo di 1(b) basta per uscire da fossi e sponde alte 1 (provato); senza 1(b) una tuta in un fosso con la sponda alta 1 resterebbe chiusa dentro, perché sparisce anche la risalita contro la sponda del ramo acqua. 1(a) dipende quindi da 1(b): vanno decisi insieme.
- **Velocità sul fondo uguale a terra.** È la scelta più semplice; se la si vuole più lenta, un modificatore di velocità transitorio mentre si è in acqua (via `FrameAttributes`, sul server, sincronizzato) — non provato.
- **Fluidi delle mod su Forge/NeoForge:** la regola guarda solo `isInWater()`; nei fluidi delle mod la tuta resta al comportamento del loader (ramo `moveInFluid`) [deduzione].
- **Più giocatori:** che un secondo client veda la tuta in acqua senza la posa a nuoto è dedotto dal codice comune sui due lati, non visto (manca un secondo client).
- **Jar di produzione:** come per la 3b, l'applicazione nel jar finale la prova solo la checklist sul PC.

---

### 1(b) Un gradino di un blocco senza saltare

**Risposta.** Il gioco lo offre già: su 1.21.1 (Fabric e NeoForge) l'attributo vanilla `generic.step_height` (0,6, sincronizzato) con un modificatore transitorio +0,4 applicato sul server; su 1.20.1 (Forge e Fabric) il campo `Entity.maxUpStep` con il metodo vanilla `setMaxUpStep(float)`, da impostare **solo sul client e solo per il giocatore locale**, perché nel gioco base `ServerPlayer` e `RemotePlayer` hanno già passo 1,0 (lo impostano nei costruttori) e solo il giocatore locale ha 0,6. Nessun mixin. Provato sulle quattro varianti: con la tuta, 1,5 s di W contro uno scalino alto 1 finiscono sopra (Y −59,0), senza tuta ci si ferma contro (Y −60,0); nessun "moved wrongly".

#### Evidenze

- [javap] 1.20.1: `Entity.maxUpStep()` legge il campo `maxUpStep`; `setMaxUpStep(F)` è pubblico. `LivingEntity` lo inizializza a 0,6. Lo cambiano anche `ServerPlayer.<init>` (`setMaxUpStep(1.0F)`, offset 211-213) e `RemotePlayer.<init>` (`setMaxUpStep(1.0F)`): il server non frena i gradini che il client sale, e gli altri giocatori li salgono nell'interpolazione.
- [javap] Fo20: `Entity.collide` usa `getStepHeight()` (`IForgeEntity`) = max(0; `maxUpStep` + attributo `forge:step_height_addition`) per le `LivingEntity`. Quindi su Forge funzionano tutti e due; `setMaxUpStep` è lo stesso meccanismo di Fabric 1.20.1, e lo si sceglie per avere una sola strada sulla versione.
- [javap] 1.21.1: niente campo; `LivingEntity.maxUpStep()` = `(float) getAttributeValue(Attributes.STEP_HEIGHT)`; `Entity.collide` lo usa. `generic.step_height` = `RangedAttribute(0,6; 0; 10)`, sincronizzato. `ServerPlayer` 1.21.1 non tocca più il passo.
- [javap] Lato server il passo del server conta poco: `handleMovePlayer` rifà `move` con il delta del client, che contiene già il +1 verticale.
- [prova] cartelle `runs/<variante>-a`, scalino di pietra alto 1, partenza a 2,5 blocchi, W per 1,5 s, `autoJump:false` nelle opzioni del client:

| | 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric | 1.21.1-neoforge |
|---|---|---|---|---|
| Tuta: Y, Z finali | −59,0; 6,83 | −59,0; 6,83 | −59,0; 6,83 | −59,0; 6,83 |
| Senza tuta: Y, Z finali | −60,0; 2,70 | −60,0; 2,70 | −60,0; 2,70 | −60,0; 2,70 |
| Tuta, fuori da acqua profonda 1 su sponda alta 1 (`-b`) | Y −60 | Y −60 | Y −60 | Y −60 |

#### Che cosa deve fare il piano, per variante

- **Dove:** in `compat/` (le differenze di versione stanno solo in `compat/impl/`): o un metodo nuovo di `FrameAttributes` (nell'esperimento `stepUp(Player, boolean)` in `FrameAttributesImpl`, con `//? if >=1.21`), o un'interfaccia piccola a sé. La chiamata viene dal tick del giocatore sui due lati (`TickEvent.PLAYER_PRE`, già registrato per `SuitSize.tick`).
- **1.21.1 Fabric e NeoForge:** sul server, modificatore transitorio con id stabile (`redstone_exoframes:frame_step`), +0,4 `ADD_VALUE`, aggiunto quando la tuta c'è e tolto quando non c'è (`hasModifier` prima, per non far reinviare l'attributo a ogni tick). Può stare anche dentro `FrameAttributes.apply/clear` insieme agli altri quattro, se `apply` viene chiamato all'ingresso, all'uscita, al login e alla rinascita (§5.6 del design). Il client lo riceve con gli attributi.
- **1.20.1 Forge e Fabric:** sul client, per `player.isLocalPlayer()` (metodo di `Player`, comune, vero solo per `LocalPlayer`: niente classi client nel codice comune), `setMaxUpStep(1.0f)` quando indossa la tuta e 0,6 quando smette. Mai sul `ServerPlayer` (riportarlo a 0,6 romperebbe il gioco base). Meglio solo al cambio di stato (come `SuitSize.tick`, che già vede le transizioni sui due lati) che a ogni tick, per non pestare altre mod che cambiano il passo; un nuovo `LocalPlayer` (rinascita, cambio di dimensione) non ha voce nella mappa e conta come transizione.
- **Costo per tick:** su 1.21.1 una ricerca del modificatore nella mappa dell'attributo, sul server; su 1.20.1 un confronto di float sul client. Trascurabile.
- **Che cosa vede l'altro lato:** nessun "moved wrongly" in nessuna delle nove esecuzioni; nessuna correzione visibile.
- **Test:** in mondo su 1.21.1 `player.maxUpStep() == 1.0` con la tuta e 0,6 senza; su 1.20.1 il server non ha nulla da controllare (il `ServerPlayer` è a 1,0 comunque), quindi la prova è nel client vero (scalino alto 1, W 1,5 s, Y +1) e nella checklist. **Il client di prova deve avere `autoJump:false`** (aggiunto nell'esperimento a `client_shot.sh`), altrimenti il salto automatico falsa il controllo.

#### Che cosa resta incerto

- Con passo 1,0 la tuta sale anche sui bordi alti 1 (muretti, bordo di una vasca): voluto per un mech, da far vedere a Luigi.
- La telecamera scatta di un blocco senza ammorbidimento (come sui cavalli) [deduzione].
- Altre mod che cambiano `maxUpStep` su 1.20.1: si sovrascrive solo alle transizioni, ma il ritorno a 0,6 cancella un loro valore [deduzione].

---

### 1(c) Scatto spento con l'energia bassa

**Risposta.** Lo scatto lo decide il client (`LocalPlayer.aiStep`) e il server lo riceve come comando (`START_SPRINTING`): spegnerlo dal server non basta, perché il client lo riaccende al tick dopo finché il tasto è tenuto (provato: con il solo lato server la tuta "senza energia" corre 11,04 blocchi in 2 s, come piena). Il gioco non offre attributi né eventi per vietare lo scatto (né Architectury, né i loader); l'unico divieto vanilla è la fame (`hasEnoughFoodToStartSprinting`: cibo > 6 o volo permesso), ed è proprio quel metodo privato di `LocalPlayer`, con la stessa firma sulle quattro varianti, il punto giusto: decide sia l'avvio (anche doppio W e scatto a interruttore, che passano da `canStartSprinting`) sia l'interruzione dello scatto in corso. Un mixin client con una chiamata a codice comune lo spegne; il server in più mette `setSprinting(false)` per i propri conti (attività "scatta", fame), non per far rispettare il divieto. Provato sulle quattro varianti: con la tuta e l'"energia bassa" 2 s di Ctrl+W percorrono 8,4-8,6 blocchi (cammino), contro 10,8-11,2 con l'energia (scatto) e 10,9-11,0 senza tuta.

#### Evidenze

- [javap] `LocalPlayer.canStartSprinting()` (privato) = `!isSprinting() && hasEnoughImpulseToStartSprinting() && hasEnoughFoodToStartSprinting() && !isUsingItem() && !hasEffect(BLINDNESS) && (!isPassenger() || vehicleCanSprint(...)) && !isFallFlying()`; stesso corpo su V20, Fo20, V21, Neo21 (metodi privati con gli stessi nomi: `canStartSprinting`, `hasEnoughImpulseToStartSprinting`, `hasEnoughFoodToStartSprinting`).
- [javap] `LocalPlayer.aiStep` (V20 380-645; uguale nelle altre a offset diversi): `canStartSprinting()` va nella variabile 6; con il doppio W (`sprintTriggerTime`) o con il tasto dello scatto → `setSprinting(true)`, entrambi solo se la variabile 6 è vera; poi, se sta correndo, `!input.hasForwardImpulse() || !hasEnoughFoodToStartSprinting()` → smette (righe 530-552, 638-645). **Un solo metodo copre avvio e fine.**
- [javap] `hasEnoughFoodToStartSprinting()` = `isPassenger() || cibo > 6 || abilities.mayfly`. L'aggancio fa `vanilla && consentito`, quindi vale anche in creativa.
- [prova, server da solo] `runs/1.21.1-fabric-c-solo-server`: tolto il mixin client dalla configurazione e lasciato `setSprinting(false)` nel tick del server: con tuta ed "energia bassa" 2 s di Ctrl+W → 11,04 e 11,04 blocchi, cioè scatto pieno. Il server non controlla la velocità oltre il "moved too quickly" (100 blocchi² per pacchetto) [javap], quindi un client senza la mod corre comunque: il divieto non è applicabile dal server.
- [deduzione] Alternative senza mixin sul client: rimettere `setSprinting(false)` nel tick del client non serve (`aiStep` lo riaccende prima di `travel` nello stesso tick se il tasto è tenuto); forzare `keySprint.setDown(false)` lascia il doppio W e interferisce con lo scatto a interruttore. Non provate.
- [prova] cartelle `runs/<variante>-a`, pista piana, Ctrl+W per 2 s (passo `@tieni ctrl+w 2`; l'esperimento ammette `+` nei nomi dei tasti di `client_shot.sh`). L'energia sotto il 10 % è simulata dalla polvere di redstone nella mano secondaria (lo stato vero dell'energia non è ancora sincronizzato):

| Blocchi in 2 s | 1.20.1-forge | 1.20.1-fabric | 1.21.1-fabric | 1.21.1-neoforge |
|---|---|---|---|---|
| Tuta, energia "piena" | 10,84 | 11,04 | 10,84 | 11,17 |
| Tuta, energia "bassa" | 8,49 | 8,59 | 8,49 | 8,37 |
| Senza tuta, redstone in mano (controllo) | 11,04 / 11,04 | 11,04 / 10,89 | 11,04 / 10,89 | 11,04 / 11,04 |

#### Che cosa deve fare il piano, per variante

Uguale sulle quattro:

- **Mixin client** `mixin/client/LocalPlayerSprintMixin`, `@Mixin(LocalPlayer.class)`, `hasEnoughFoodToStartSprinting()Z`, `@At("RETURN")`, cancellabile → `cir.setReturnValue(SuitMovement.sprintAllowed(player, cir.getReturnValueZ()))`; nella sezione `client` delle due configurazioni; un server dedicato non lo carica. Nomi nei jar: Fabric `method_46743` (uguale sulle due versioni), Forge `m_255269_`, NeoForge Mojang.
- **Logica** comune (`SuitMovement.sprintAllowed`): falso se indossa la tuta e l'energia della cella è sotto il 10 %, letta dalla copia dello stato che il client riceve (sincronizzazione al proprietario del §5.6). Codice comune, nessuna classe client: il mixin passa `this` come `Player`.
- **Server:** nel tick del telaio di §5.6, se l'energia è sotto il 10 % e `isSprinting()`, `setSprinting(false)`, così attività, fame e consumo sul server non contano uno scatto che non c'è (con il client della mod non succede mai; serve per un client senza la mod o un ritardo della sincronizzazione).
- **Che cosa vede l'altro lato:** con il mixin il client non manda mai `START_SPRINTING`; gli altri non vedono particelle di corsa. Nessun avviso nel registro del server.
- **Costo per tick:** al più due chiamate per tick sul client (avvio e fine), una lettura dello slot e dello stato in cache.
- **Test:** il comportamento è solo del client: client vero (Ctrl+W 2 s, soglia per esempio ≤ 9 blocchi con energia bassa e ≥ 10,5 con energia) e checklist sul PC (anche doppio W e scatto a interruttore). In mondo si prova solo la parte del server (`setSprinting(false)` con energia bassa).

#### Che cosa resta incerto

- Lo stato dell'energia sul client: la soglia va letta dalla stessa copia che il client usa per gli indicatori; fino al primo pacchetto di sincronizzazione vale l'ultimo stato noto [deduzione].
- Doppio W e scatto a interruttore (`toggleSprint`) passano da `canStartSprinting` [javap], ma non sono stati provati nel client.

---

## 2. Dati per versione di Minecraft nella build

**Risposta.** Non serve cambiare la build: `build.gradle.kts` aggiunge già a ogni variante la cartella della propria versione (`sourceSets.main { resources.srcDir(rootProject.file("src/main/resources-mc$mc")) }`, dove `mc = stonecutter.current.version`), e oggi la usa per la configurazione dei mixin e per le texture dell'armatura di 1.20.1. La cartella non passa da Stonecutter (è aggiunta per percorso assoluto dalla radice) e `processResources` la unisce alle risorse comuni. Scritti a mano una ricetta a forma, una tabella di bottino per un segnaposto e un tag di blocchi nei due formati, sotto `resources-mc1.20.1/data/…` e `resources-mc1.21.1/data/…`: si caricano sulle quattro varianti (il server dedicato li usa: `recipe give`, `loot give` e `execute if block … #tag` riescono) e ogni jar contiene solo l'albero della propria versione.

### Evidenze

- [javap] Cartelle: 1.20.1 `RecipeManager` → `"recipes"`; `LootDataType` → `"loot_tables"` (e `"predicates"`, `"item_modifiers"`); tag `TagManager.getTagDir` con le cartelle al plurale (`tags/blocks`, `tags/items`). 1.21.1 `Registries.elementsDirPath(chiave)` = percorso del registro (`recipe`, `loot_table`), `Registries.tagsDirPath(chiave)` = `"tags/"` + percorso (`tags/block`, `tags/item`). Un jar 1.21.1 con la cartella `recipes/` la ignorerebbe senza errori [deduzione dai percorsi]: per questo gli alberi separati sono più sicuri di uno comune.
- [prova] Jar costruiti con `remapJar` sulle quattro varianti (`unzip -l`):
  - 1.20.1-forge e 1.20.1-fabric: `data/redstone_exoframes/recipes/ricognizione_cella.json`, `loot_tables/blocks/ricognizione_segnaposto.json`, `tags/blocks/ricognizione_terra.json`; nessun file di 1.21.1;
  - 1.21.1-fabric e 1.21.1-neoforge: `recipe/…`, `loot_table/blocks/…`, `tags/block/…`; nessun file di 1.20.1.
- [prova] Server dedicato di ciascuna variante (cartelle `runs/<variante>-a`, comandi via RCON con `execute store success`, quindi un comando non riuscito avrebbe fermato l'esecuzione):
  - `recipe give Exo redstone_exoframes:ricognizione_cella` → "Unlocked 1 recipes for Exo";
  - `loot give Exo loot redstone_exoframes:blocks/ricognizione_segnaposto` → "Dropped 1 [Redstone Cell]";
  - `execute if block 0 -61 0 #redstone_exoframes:ricognizione_terra` (erba) → "Test passed";
  - nel `server.log` nessun errore di caricamento dei dati (solo gli ERROR noti e innocui: chiave yggdrasil, `No key layers`, `No data fixer registered for exoframe` su Fabric). Su 1.20.1 la riga "Loaded 7 recipes" conta i tipi di ricetta, su 1.21.1 "Loaded 1291 recipes" le ricette [prova; il significato diverso è una deduzione dai numeri].

### I formati che si sono caricati

**Ricetta a forma.** 1.20.1, `data/<ns>/recipes/<nome>.json`:

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "pattern": ["RCR", "CQC", "RCR"],
  "key": {
    "R": {"item": "minecraft:redstone"},
    "C": {"item": "minecraft:copper_ingot"},
    "Q": {"item": "minecraft:quartz"}
  },
  "result": {"item": "redstone_exoframes:redstone_cell", "count": 1}
}
```

1.21.1, `data/<ns>/recipe/<nome>.json`: identica salvo il risultato, `"result": {"id": "redstone_exoframes:redstone_cell", "count": 1}` (gli ingredienti restano oggetti `{"item": …}` su 1.21.1; diventano stringhe solo da 1.21.2, fatto non verificato qui). Il risultato con dati (per esempio una cella carica) su 1.21.1 andrebbe in `"components"`, su 1.20.1 in `"nbt"` (solo Forge) [deduzione, non provato].

**Tabella di bottino di un blocco.** Stesso JSON sulle due versioni, cartella diversa (`loot_tables/blocks/` e `loot_table/blocks/`):

```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [{"type": "minecraft:item", "name": "redstone_exoframes:redstone_cell"}],
      "conditions": [{"condition": "minecraft:survives_explosion"}]
    }
  ]
}
```

**Tag di blocchi.** Stesso JSON, cartella `tags/blocks/` su 1.20.1 e `tags/block/` su 1.21.1:

```json
{"replace": false, "values": ["minecraft:grass_block", "minecraft:dirt"]}
```

### Che cosa deve fare il piano

- **Build:** nulla. Il generatore (`tools/gen_data.py`, §5.8 del design) scrive in `src/main/resources-mc1.20.1/data/redstone_exoframes/{recipes,loot_tables,tags/blocks,tags/items}/` e in `src/main/resources-mc1.21.1/data/redstone_exoframes/{recipe,loot_table,tags/block,tags/item}/`, con le due differenze di formato sopra (cartelle e `item`/`id` nel risultato). Le ricette non vanno in `src/main/resources/data` (comune).
- Nota: la struttura vuota dei GameTest oggi sta nelle risorse comuni sia come `structures/empty.nbt` sia come `structure/empty.nbt`: funziona perché ogni versione ignora l'altra cartella; per i dati nuovi meglio gli alberi per versione.
- **Controlli:** in `tools/check_jars.sh`, che i jar 1.20.1 non abbiano `data/*/recipe/`, `loot_table/`, `tags/block/` e i 1.21.1 non abbiano `recipes/`, `loot_tables/`, `tags/blocks/`; nella CI, che il generatore riprodotto non cambi i file committati.
- **Test in mondo:** cercare la ricetta per id nel gestore delle ricette (1.20.1 `RecipeManager.byKey(ResourceLocation)` → `Optional<Recipe<?>>`, 1.21.1 → `Optional<RecipeHolder<?>>`: tipi diversi, quindi un metodo in `compat/`) e la tabella di bottino (1.20.1 `server.getLootData().getLootTable(id)`, 1.21.1 `server.reloadableRegistries().getLootTable(ResourceKey)`: anche questo in `compat/`) [javap dei nomi non fatto per questi due: da verificare quando si scrive il test]. In alternativa, come qui, comandi RCON sul server di prova.

### Che cosa resta incerto

- Ricette con condizioni di caricamento per loader (`fabric:load_conditions`, `forge:conditions`, `neoforge:conditions`) non servono per ora e non sono state provate.
- Il generatore e il suo controllo nella CI non sono stati scritti.

---

## 3. Che cosa cambia nella struttura del progetto

- Un mixin comune in più (`mixin/PlayerMovementMixin`, sezione `mixins` delle due configurazioni) e un mixin client in più (`mixin/client/LocalPlayerSprintMixin`, sezione `client` delle due). Entrambi senza direttive e senza esclusioni nella build, perché i bersagli hanno la stessa firma sulle quattro varianti. È la terza e quarta eccezione al "niente mixin": vanno in design §3 e §7 (scelte da far vedere a Luigi) e in `docs/compat.md`.
- Il passo è l'unico pezzo con differenze di versione: in `compat/impl/` (l'esperimento lo mette in `FrameAttributesImpl` con `//? if >=1.21`). `tools/check_sources.sh` passa con la patch (`OK sources`).
- `tools/check_jars.sh` (e2) va aggiornato per le nuove classi e i nuovi marcatori dei nomi; con la patch fallisce solo su questo (provato: "sezione mixins … invece di …", "nomi Mojang attesi nelle annotazioni").
- `tools/client/client_shot.sh`: per le prove di movimento servono `autoJump:false` nelle opzioni e la possibilità di tenere due tasti (`ctrl+w`); l'esperimento cambia due righe (§5).

## 4. Le esecuzioni

Tutte con `EXO_ACCEPT_EULA=true EXO_SHOT_PORT=25880`, una alla volta, sotto `/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m3c-ricognizione/runs/`. Ogni cartella ha `rcon.log` (comandi e posizioni), `server.log`, `client.log`, `log-check.txt` (vuoto in tutte) e gli scatti.

| Cartella | Script | Che cosa | Esito |
|---|---|---|---|
| `1.21.1-fabric-a`, `1.20.1-forge-a`, `1.20.1-fabric-a`, `1.21.1-neoforge-a` | `tools/client/ricognizione-3c/prova.sh` | vasca da 3 (affondare, Spazio, avanti), scalino, corsa con e senza "energia", controllo senza tuta, dati di §2 | uscita 0; 101-136 s ciascuna |
| `1.21.1-neoforge-b`, `1.20.1-forge-b`, `1.20.1-fabric-b`, `1.21.1-fabric-b` | `tools/client/ricognizione-3c/prova_b.sh` | acqua profonda 1, uscita su sponda alta 1, corrente, colonna di bolle, aria, corsa sul fondo, lava | uscita 0 |
| `1.21.1-fabric-c-solo-server` | comandi a mano (§1(c)) | mixin client tolto dalla configurazione 1.21.1, solo `setSprinting(false)` sul server | la tuta corre lo stesso (11,04 blocchi); configurazione rimessa |

Scatti utili: `salto-tuta.png` e `salto-nudo.png` in ogni cartella `-a` (a metà dei 3 s di Spazio: la tuta resta sul fondo con gli occhi sott'acqua, senza tuta si sale).

Tempo: le quattro varianti di entrambe le serie sono state eseguite; nulla è rimasto fuori.

## 5. L'esperimento (patch)

`/tmp/claude-0/-home-user/3f7b8ad1-7942-5139-8688-e3b7d5b2e5fc/scratchpad/m3c/ricognizione-movimento-e-dati.patch` = `git diff 2e35aa6..HEAD` del ramo `ricognizione-3c` (due commit):

- `frame/SuitMovement.java` (comune, senza direttive): le regole di 1(a), 1(c) e il tick; l'energia bassa è un segnaposto (`Items.REDSTONE` nella mano secondaria);
- `mixin/PlayerMovementMixin.java` e `mixin/client/LocalPlayerSprintMixin.java`, elencati nelle due `redstone_exoframes.mixins.json`;
- `compat/FrameAttributes.java` e `compat/impl/FrameAttributesImpl.java`: `stepUp(Player, boolean)` con le due strade del passo;
- `RedstoneExoframes.init()`: `TickEvent.PLAYER_PRE.register(SuitMovement::tick)`;
- i sei file di dati di §2 in `src/main/resources-mc1.20.1/` e `src/main/resources-mc1.21.1/`;
- `tools/client/client_shot.sh`: `autoJump:false` e `+` ammesso nei nomi dei tasti; `tools/client/ricognizione-3c/prova.sh` e `prova_b.sh` (con il percorso del clone scritto dentro).

Non è codice da unire: il tick a ogni tick su 1.20.1 riporta il passo a 0,6 anche senza transizione, l'energia è finta, e mancano test e documenti.
