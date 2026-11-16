### Task 10: I due mixin nuovi con gestori vuoti

Primo passo del design §12.2: le due classi con i bersagli definitivi e gestori che lasciano il valore del gioco, provate sui quattro jar, con il client vero e con il server dedicato, prima di costruirci sopra. Un bersaglio sbagliato fa crollare il gioco all'avvio (`"required": true`, `"defaultRequire": 1`).

**Files:**
- Create: `src/main/java/com/toxineren/exoframes/mixin/PlayerSuitMixin.java`, `src/main/java/com/toxineren/exoframes/mixin/client/LocalPlayerSprintMixin.java`, `src/main/java/com/toxineren/exoframes/frame/SuitDamage.java` (solo `onArmorHurt`, vuoto), `src/main/java/com/toxineren/exoframes/frame/SuitMovement.java`
- Modify: `src/main/resources-mc1.20.1/redstone_exoframes.mixins.json`, `src/main/resources-mc1.21.1/redstone_exoframes.mixins.json`, `tools/check_jars.sh` ((e2): commento, `CLIENT`, `TARGETS`, `MOJANG`, controllo della sezione `mixins`), `docs/compat.md`
- Testi: nessuna chiave. `docs/compat.md`, sezione "Mixin (`mixin/` e `mixin/client/`)": un punto nuovo **Usura, acqua e scatto** (design §12.2: le due classi, i cinque agganci con descrittore e gestore, i nomi per loader della tabella qui sotto, nessuna direttiva né esclusione in `build.gradle.kts` perché le firme sono le stesse sulle quattro varianti, i gestori per ora lasciano il valore del gioco, nessun gestore e nessun metodo chiamato porta il nome di un bersaglio); punto **Configurazione**: `mixins` = la grandezza della versione più `PlayerSuitMixin`, `client` = quattro classi; punto **Nessun refmap**: i nomi nuovi nei jar; punto **Verifica**: l'avvio del client vero e del server dedicato del Task 10; "Cuciture client" (`(i tre mixin client` → `(i quattro mixin client`); "Tabella API vanilla/loader → interfaccia", riga nuova: "Usura dell'armatura, galleggiamento, corrente, nuoto e scatto del giocatore: nessun evento comune alle quattro varianti | Non codice per variante: `PlayerSuitMixin` e `LocalPlayerSprintMixin`, senza direttive; logica in `frame/SuitDamage` e `frame/SuitMovement`".

**Interfaces:**
- Consumes: la patch della ricognizione `docs/superpowers/specs/ricognizioni/2026-11-15-m3c-movimento-e-dati.patch` (`PlayerMovementMixin` con tre agganci e `LocalPlayerSprintMixin`, provati sulle quattro varianti: qui la classe comune si chiama `PlayerSuitMixin` e ha anche l'aggancio dell'usura); in `tools/check_jars.sh` (e2): `CULLING`, `CLIENT`, `TARGETS`, `MOJANG`, il controllo `common != [size_mixin]`.
- Produces:
  - `public static void SuitDamage.onArmorHurt(Player player, DamageSource source, float amount)`: vuoto (l'usura arriva al Task 17).
  - `public static boolean SuitMovement.affectedByFluids(Player player, boolean vanilla)`, `public static boolean pushedByFluid(Player player, boolean vanilla)`, `public static boolean sprintAllowed(Player player, boolean vanilla)`: restituiscono `vanilla`; `public static void afterUpdateSwimming(Player player)`: non fa nulla (le regole al Task 19).
  - I due mixin, senza direttive, ciascun gestore una sola chiamata:

  | Classe (sezione) | Bersaglio, `@At` | Gestore → chiamata | Fabric (1.20.1 e 1.21.1) | Forge 1.20.1 | NeoForge |
  |---|---|---|---|---|---|
  | `mixin/PlayerSuitMixin`, `@Mixin(Player.class)` (`mixins`) | `hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V`, `HEAD` | `exoframes$wearPieces(DamageSource, float, CallbackInfo)` → `SuitDamage.onArmorHurt` | `method_6105` | `m_6472_` | Mojang |
  | | `isAffectedByFluids()Z`, `RETURN`, cancellabile | `exoframes$fluidMovement(CallbackInfoReturnable<Boolean>)` → `cir.setReturnValue(SuitMovement.affectedByFluids(p, cir.getReturnValueZ()))` | `method_29920` | `m_6129_` | Mojang |
  | | `isPushedByFluid()Z`, `RETURN`, cancellabile | `exoframes$fluidPush` → `SuitMovement.pushedByFluid` | `method_5675` | `m_6063_` | Mojang |
  | | `updateSwimming()V`, `TAIL` | `exoframes$noSwimming(CallbackInfo)` → `SuitMovement.afterUpdateSwimming` | `method_5790` | `m_5844_` | Mojang |
  | `mixin/client/LocalPlayerSprintMixin`, `@Mixin(LocalPlayer.class)` (`client`) | `hasEnoughFoodToStartSprinting()Z` (privato), `RETURN`, cancellabile | `exoframes$sprintGate` → `cir.setReturnValue(SuitMovement.sprintAllowed((LocalPlayer) (Object) this, cir.getReturnValueZ()))` | `method_46743` | `m_255269_` | Mojang |

  `p` = `(Player) (Object) this`; classi `abstract class … ` package-private come `PlayerSize121Mixin`, con Javadoc in italiano (bersagli verificati, perché un mixin: §12.2).

- [ ] **Step 1: scrivere** `SuitDamage`, `SuitMovement` (classi `public final`, Javadoc che dice che oggi restituiscono il valore del gioco), i due mixin con le annotazioni della tabella e le due configurazioni: 1.20.1 `"mixins": ["PlayerSize120Mixin", "PlayerSuitMixin"]`, 1.21.1 `"mixins": ["PlayerSize121Mixin", "PlayerSuitMixin"]`, tutte e due `"client": ["client.ItemInHandLayerMixin", "client.LivingEntityCullingMixin", "client.PlayerRendererMixin", "client.LocalPlayerSprintMixin"]`.
- [ ] **Step 2: Run** la build delle quattro varianti con `collectJar` (comando di Global Constraints) — Expected: `BUILD SUCCESSFUL` e nessun `Cannot remap` nell'uscita (sarebbe un bersaglio che `remapJar` non conosce).
- [ ] **Step 3: Run** `bash tools/check_jars.sh` — Expected: FAIL, con il controllo di oggi: per ogni jar `sezione mixins ['PlayerSize12xMixin', 'PlayerSuitMixin'] invece di ['PlayerSize12xMixin']` e `sezione client [...] invece di [...]`, e sul jar NeoForge `com/toxineren/exoframes/mixin/PlayerSuitMixin.class: nomi Mojang attesi nelle annotazioni` e `com/toxineren/exoframes/mixin/client/LocalPlayerSprintMixin.class: nomi Mojang attesi nelle annotazioni`.
- [ ] **Step 4: aggiornare `tools/check_jars.sh` (e2)**, valori esatti:
  ```python
  CULLING = "client.LivingEntityCullingMixin"
  SPRINT = "client.LocalPlayerSprintMixin"  # uguale al nome nel json: "mixin not in common + client" confronta quelli
  CLIENT = sorted(["client.ItemInHandLayerMixin", CULLING, "client.PlayerRendererMixin", SPRINT])
  TARGETS = {
      ("neoforge", "1.21.1"): {"PlayerSize121Mixin": [b"getDefaultDimensions("], CULLING: [b"getBoundingBoxForCulling("],
                               "PlayerSuitMixin": [b"hurtArmor(", b"isAffectedByFluids(", b"isPushedByFluid(",
                                                   b"updateSwimming("],
                               SPRINT: [b"hasEnoughFoodToStartSprinting("]},
      ("forge", "1.20.1"): {"PlayerSize120Mixin": [b"m_6972_(", b"m_6431_("], CULLING: [b"m_6921_("],
                            "PlayerSuitMixin": [b"m_6472_(", b"m_6129_(", b"m_6063_(", b"m_5844_("],
                            SPRINT: [b"m_255269_("]},
      ("fabric", "1.20.1"): {"PlayerSize120Mixin": [b"method_18377(", b"method_18394("], CULLING: [b"method_5830("],
                             "PlayerSuitMixin": [b"method_6105(", b"method_29920(", b"method_5675(", b"method_5790("],
                             SPRINT: [b"method_46743("]},
      ("fabric", "1.21.1"): {"PlayerSize121Mixin": [b"method_55694("], CULLING: [b"method_5830("],
                             "PlayerSuitMixin": [b"method_6105(", b"method_29920(", b"method_5675(", b"method_5790("],
                             SPRINT: [b"method_46743("]},
  }
  MOJANG = (b"setModelProperties", b"renderArmWithItem", b"getDimensions(", b"getStandingEyeHeight(",
            b"getDefaultDimensions(", b"getBoundingBoxForCulling(", b"hurtArmor(", b"isAffectedByFluids(",
            b"isPushedByFluid(", b"updateSwimming(", b"hasEnoughFoodToStartSprinting(")
  ```
  e il controllo della sezione comune: `if sorted(common) != sorted([size_mixin, "PlayerSuitMixin"]): print(f"{cfg_name}: sezione mixins {sorted(common)} invece di {sorted([size_mixin, 'PlayerSuitMixin'])} (la grandezza reale della versione e gli agganci comuni su Player)")`. I marcatori nuovi finiscono tutti con `(`: li fa scattare solo la stringa dell'annotazione (nome e descrittore attaccati), non i nomi dei gestori o dei metodi chiamati (`onArmorHurt`, `affectedByFluids`, `pushedByFluid`, `afterUpdateSwimming`, `sprintAllowed`). Commento (e2) riscritto: la sezione `mixins` elenca esattamente due classi, la grandezza reale della versione del jar e `PlayerSuitMixin` (usura e acqua, design §12.2); la sezione `client` esattamente le quattro di `CLIENT`; la grandezza, il culling, `PlayerSuitMixin` e lo scatto portano esattamente i nomi attesi dei loro bersagli per loader e versione.
- [ ] **Step 5: Run** `bash tools/check_jars.sh` — Expected: `OK jars`. Prova negativa, fuori dal repository: copiare i quattro jar di `build/libs/all/` in una cartella temporanea (`d=$(mktemp -d)`), riscrivere con `python3` e `zipfile` il jar `redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar` aggiungendo `"ExtraMixin"` alla sezione `mixins` di `redstone_exoframes.mixins.json` (tutte le altre voci copiate uguali), `bash tools/check_jars.sh "$d"` — Expected: `FAIL: redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar: redstone_exoframes.mixins.json: sezione mixins ['ExtraMixin', 'PlayerSize121Mixin', 'PlayerSuitMixin'] invece di ['PlayerSize121Mixin', 'PlayerSuitMixin'] …` e uscita 1; poi `rm -rf "$d"`.
- [ ] **Step 6: Run** `bash tools/check_sources.sh` — Expected: `OK sources` (`mixin/client/LocalPlayerSprintMixin` chiama `frame/`, codice comune: la regola 7 vieta solo il contrario). `bash tools/run_gametests.sh` — Expected: quattro `OK <variante>` con lo stesso numero del Task 9 (nessun test nuovo; il server dei test applica `PlayerSuitMixin`, e i giocatori finti passano dagli agganci).
- [ ] **Step 7: client vero**, su ciascuna delle quattro varianti, una alla volta: `EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <variante> <cartella> "execute as Exo run exoframe debug wear rottame"` — Expected: uscita 0, `log-check.txt` vuoto (nessun errore di mixin, `LocalPlayerSprintMixin` applicato), negli scatti la tuta indossata.
- [ ] **Step 8: server dedicato** avviato e fermato sulle quattro varianti, una alla volta (`./gradlew --console=plain :<v>:runServer` con `eula=true` in `versions/<v>/run/eula.txt`, fino a `Done (`, poi `stop`) — Expected: arriva a `Done (`; `python3 -I tools/check_log.py client --variant <v> versions/<v>/run/logs/latest.log` → nessuna uscita, uscita 0 (l'ambito client ammette la sola `No data fixer registered for exoframe`, esattamente una volta su Fabric e mai su Forge e NeoForge); in più nessuna riga con `InvalidInjectionException` o `Mixin apply` (`PlayerSuitMixin` si applica sul server; il mixin client non si carica).
- [ ] **Step 9: documenti** — `docs/compat.md` (le righe in **Files**).
- [ ] **Step 10: verifiche di fine task** — già fatte agli Step 2, 5 e 6 (build con `collectJar`, `OK jars`, `OK sources`, test in mondo); rifarle solo se dopo è cambiato del codice.
- [ ] **Step 11:** commit `mixin: agganci comuni su Player e scatto del giocatore locale, ancora senza effetto`.

