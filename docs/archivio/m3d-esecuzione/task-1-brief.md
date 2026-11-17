### Task 1: Il braccio della tuta al posto di quello del gioco

**Files:**
- Create: `client/FirstPersonArm.java`; `src/test/java/com/toxineren/exoframes/client/FirstPersonArmTest.java`; `tools/client/arm_measure.sh`
- Modify: `compat/client/SuitBoneRules.java`, `compat/client/SuitRenderer.java`, `compat/impl/client/SuitRendererImpl.java`, `mixin/client/PlayerRendererMixin.java`, `client/PilotPose.java` (solo il Javadoc della classe), `src/test/java/com/toxineren/exoframes/compat/client/SuitBoneRulesTest.java`, `src/test/java/com/toxineren/exoframes/client/PilotPoseTest.java` (solo: `handCubes` e `GEO` da `private static` diventano package-private), `tools/check_jars.sh`, `docs/compat.md`

**Interfaces:**
- Consumes (tutto esiste): `SuitLayer.suitView(Player) : SuitView` (statico, package-private in `client/`: la vista in cache del giocatore, `null` se non indossa la tuta o il riassunto non si legge; non lancia mai); `SuitDraw.logFailureOnce(Throwable)` (statico, package-private); `ClientCompat.suitRenderer() : SuitRenderer`; `PilotPose.Shift`; `SuitScale.S` (1,65); `SuitBoneRules.visible(String, SuitView)` e `BONES`; `SuitBones.ARMOR_RIGHT_ARM`, `ARMOR_LEFT_ARM`, `frame(String)`, `piece(PieceSlot, int, String)`, `decal(PieceSlot)`, `TOP_LEVEL`, `PARTS`, `REGIONS`, `DECAL_SLOTS`; in `SuitRendererImpl` i campi `view` e `BARE`, `render`, `applyBoneVisibilityBySlot`, `setVisible`; in `SuitBoneRulesTest` `BARE` e `full()` (i sei pezzi intatti); in `PilotPoseTest` (stesso package di `FirstPersonArmTest`) `static List<double[]> handCubes(String bone) throws IOException`, che legge dalla geometria generata i cubi dell'osso che finiscono a y ≤ 11,25, e la costante `GEO`: oggi `private static`, questo task li rende package-private e li chiama, senza copiarli; i passi di `client_shot.sh` (`@scatto`, `@attendi`).
- Produces:
  - **`SuitBoneRules.visibleForArm(String bone, SuitView view, PieceSlot arm)`**: vero solo per le ossa di quel braccio che la vista mostra: l'osso di primo livello del braccio (`ARMOR_RIGHT_ARM` o `ARMOR_LEFT_ARM`), l'osso del telaio del braccio (`frame("RightArm")` o `frame("LeftArm")`) e, fra le ossa del pezzo dello slot `arm`, quelle per cui `visible(bone, view)` è vero. Falso per ogni altro osso di `BONES`: le altre ossa di primo livello e del telaio, i pezzi degli altri slot (anche dell'altro braccio), le due decalcomanie. `arm` diverso da `RIGHT_ARM` e `LEFT_ARM` → `IllegalArgumentException("not an arm slot: " + arm)`; osso fuori da `BONES` → `IllegalArgumentException`, come `visible`; `view` o `arm` nulli → `NullPointerException`. Javadoc della classe: una frase sul disegno di un braccio solo.
  - **`SuitRenderer.renderArm`** (firma in "Nomi, numeri e testi comuni"). Contratto nel Javadoc: come `render` (stessa posa, stesso spazio, stessa entità non animabile), ma disegna le sole ossa che `SuitBoneRules.visibleForArm` mostra per `arm`, con i colori di `view`; `arm` che non è un braccio → `IllegalArgumentException` prima di toccare qualunque cosa; dopo la chiamata non resta nulla di impostato: il `render` successivo disegna la tuta intera. In `SuitRendererImpl` i due metodi passano da un solo disegno, e la visibilità si decide a ogni disegno per **tutte** le ossa di `BONES`:
    ```java
    private PieceSlot arm; // null: la tuta intera

    @Override
    public void render(Entity entity, HumanoidModel<?> base, PoseStack pose, MultiBufferSource buffers,
            int packedLight, float partialTick, SuitView view) {
        draw(entity, base, pose, buffers, packedLight, partialTick, view, null);
    }

    @Override
    public void renderArm(Entity entity, HumanoidModel<?> base, PoseStack pose, MultiBufferSource buffers,
            int packedLight, float partialTick, SuitView view, PieceSlot arm) {
        if (arm != PieceSlot.RIGHT_ARM && arm != PieceSlot.LEFT_ARM) {
            throw new IllegalArgumentException("not an arm slot: " + arm);
        }
        draw(entity, base, pose, buffers, packedLight, partialTick, view, arm);
    }
    ```
    `draw` è il corpo di `render` di oggi con `this.arm = arm` accanto a `this.view = view` e `this.arm = null` accanto a `this.view = BARE` nel `finally`; in `applyBoneVisibilityBySlot`, per ogni nome di `BONES`: `setVisible(name, arm == null ? SuitBoneRules.visible(name, view) : SuitBoneRules.visibleForArm(name, view, arm), missing)`. Nessuna direttiva nuova.
  - **`FirstPersonArm`**, parte pura (costanti e firme in "Nomi, numeri e testi comuni"): `baseHandPx(left)` = (±`BASE_HAND_OUT_PX`, `BASE_HAND_DOWN_PX`, 0); `suitPalmPx(left)` = (±`SUIT_PALM_OUT_PX`, `SUIT_PALM_DOWN_PX`, 0); `sideDirection(left)` = (±`SIDE_DIR_OUT`, `SIDE_DIR_DOWN`, `SIDE_DIR_BACK`); in tutte e tre x è negativa per il destro (`left` falso) e positiva per il sinistro; `pivotShiftPx(left, scale, sideShiftPx)` = `baseHandPx + sideShiftPx · sideDirection − scale · suitPalmPx`; `armSlot(left)` = `LEFT_ARM` o `RIGHT_ARM`. Javadoc della classe: che cosa si vede (design §13), l'aritmetica del posto, perché nel sistema del braccio, perché `young`.
  - **`FirstPersonArm.onHandPrepared`**, da scrivere così (né la firma né i test ne fissano l'ordine):
    ```java
    public static boolean onHandPrepared(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model,
            ModelPart arm, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        SuitView view = SuitLayer.suitView(player); // non lancia; null: senza tuta, disegna il gioco
        if (view == null) {
            return false;
        }
        try {
            boolean left = arm == model.leftArm;
            arm.xRot = 0f; // come fa il metodo di base nell'istruzione che segue l'aggancio
            // Il modello nasce "giovane" e lo corregge solo il disegno in terza persona: GeckoLib copia il segno
            // e dimezzerebbe il braccio. Un giocatore non e' mai un cucciolo.
            model.young = false;
            // Copia privata della posa, come SuitDraw.draw: se il renderer lancia a meta', le sue pose restano qui.
            PoseStack local = new PoseStack();
            local.last().pose().set(pose.last().pose());
            local.last().normal().set(pose.last().normal());
            arm.translateAndRotate(local); // nel sistema del braccio di base: perno e rotazione
            PilotPose.Shift pivot = pivotShiftPx(left, SCALE, SIDE_SHIFT_PX);
            local.translate(pivot.x() / 16f, pivot.y() / 16f, pivot.z() / 16f);
            local.scale(SCALE, SCALE, SCALE);
            // Ritorno allo spazio del modello: perno e rotazione del braccio li rimette il renderer della tuta.
            local.mulPose(new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot).conjugate());
            local.translate(-arm.x / 16f, -arm.y / 16f, -arm.z / 16f);
            ClientCompat.suitRenderer().renderArm(player, model, local, buffers, packedLight, 0f, view, armSlot(left));
            return true;
        } catch (RuntimeException | LinkageError e) {
            SuitDraw.logFailureOnce(e); // il gioco disegna il suo braccio
            return false;
        }
    }
    ```
    `partialTick` è 0: `renderHand` non ne ha uno, la tuta non ha animazioni, e leggerlo dal gioco cambia fra 1.20.1 e 1.21.1 (lo decide il piano). `Quaternionf` è `org.joml.Quaternionf`, lo stesso che usa `ModelPart.translateAndRotate` sulle due versioni.
  - **Il gestore**, terzo di `PlayerRendererMixin` (import in più: `ModelPart`, `FirstPersonArm`):
    ```java
    @Inject(method = "renderHand(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;"
            + "Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/model/PlayerModel;"
                            + "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"),
            cancellable = true)
    private void exoframes$suitArm(PoseStack pose, MultiBufferSource buffers, int packedLight,
            AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        if (FirstPersonArm.onHandPrepared(player, ((PlayerRenderer) (Object) this).getModel(), arm, pose, buffers,
                packedLight)) {
            ci.cancel();
        }
    }
    ```
    Javadoc della classe: il terzo aggancio (design §13), perché dopo `setupAnim` e non in testa (`setModelProperties` è privato), e la verifica del bersaglio: javap sui jar di 1.20.1 e 1.21.1, vanilla e con le patch di Forge e NeoForge, una sola chiamata a `PlayerModel.setupAnim` in `renderHand`, dopo `setModelProperties` e prima dei due disegni; le patch dei loader stanno in `renderRightHand` e `renderLeftHand`.
  - **`tools/check_jars.sh`, controllo (e2):** in `MOJANG` in più `b"renderHand("` e `b"setupAnim("`; in `TARGETS` la voce `"client.PlayerRendererMixin"`: `[b"renderHand(", b"setupAnim("]` per `("neoforge", "1.21.1")`, `[b"m_117775_(", b"m_6973_("]` per `("forge", "1.20.1")`, `[b"method_23205(", b"method_17087("]` per le due di Fabric; il commento di testa del controllo dice che anche il braccio in prima persona porta i nomi attesi dei suoi due bersagli.
  - **`docs/compat.md`:** voce `SuitRenderer`: il metodo `renderArm` in "A cosa serve" (che cosa disegna, chi decide le ossa, nulla resta impostato) e la riga **Uso** (`render` solo da `SuitDraw.draw`; `renderArm` solo da `FirstPersonArm.onHandPrepared`); il paragrafo di `SuitBoneRules`: `visibleForArm`; nella sezione "Mixin": `PlayerRendererMixin` ha tre agganci, e al posto della frase "La prima persona (`renderHand`, che chiama anch'esso `setModelProperties`) non cambia." il terzo aggancio (bersaglio, posizione, cancellabile, che cosa fa `FirstPersonArm`, il segno `young`, i nomi nei jar: Fabric `method_23205` e `method_17087`, Forge `m_117775_` e `m_6973_`, NeoForge i nomi Mojang); nel punto "Disegno del giocatore" accanto a `PilotPose` anche `FirstPersonArm`; nell'elenco delle classi di `client/` (sezione "Avvio del client e layer sul giocatore (`client/`)") una voce `FirstPersonArm` (grandezza, posto e disegno del braccio in prima persona; la chiama il terzo aggancio di `PlayerRendererMixin`).
  - **`client/PilotPose`, Javadoc della classe:** al posto di "La prima persona non cambia." → "In prima persona il braccio lo disegna {@link FirstPersonArm} (design, par. 13)."
  - **`tools/client/arm_measure.sh`** come in "Nomi, numeri e testi comuni" (Step 7). La sequenza `m3d_shots.sh`, `EXO_SHOT_MAIN_HAND` e il paragrafo di `tools/client/README.md` sono del Task 2.

- [ ] **Step 1: test JUnit della regola** — in coda a `SuitBoneRulesTest`; aiuto privato `Set<String> shownForArm(SuitView view, PieceSlot arm)` (le ossa di `BONES` per cui `visibleForArm` è vero) e `armBones(PieceSlot arm)` = `Set.of(` osso di primo livello e osso del telaio di quel braccio `)`:
  - `armDrawShowsOnlyTheBonesOfThatArm`: per `arm` in `RIGHT_ARM`, `LEFT_ARM`: `shownForArm(full(), arm)` è uguale a `armBones(arm)` più `SuitBones.piece(arm, 0, r)` per le tre regioni (cinque ossa); in particolare `visibleForArm(SuitBones.ARMOR_BODY, full(), arm)`, `visibleForArm(SuitBones.frame("Body"), full(), arm)` e `visibleForArm(SuitBones.piece(PieceSlot.TORSO, 0, SuitBones.PRIMARY), full(), arm)` sono falsi.
  - `armDrawWithoutTheArmPieceShowsTheFrameArmOnly`: `shownForArm(BARE, arm)` è `armBones(arm)`; lo stesso con una vista che ha i cinque pezzi tranne quello di `arm`; per i due bracci.
  - `armDrawFollowsTheStateOfItsOwnPiece`: `view` = `RIGHT_ARM` nello stato 2 e `LEFT_ARM` nello stato 1 (`new PieceView(2, 0xAA0000, 0x00BB00)`, `new PieceView(1, 0x0000CC, 0x00DD00)`): con `RIGHT_ARM` si vedono le tre `pieceRightArm_s2_*` e nessuna di `s0`, `s1`, `s3`, e `pieceLeftArm_s1_p1` è falso; con `LEFT_ARM` si vedono le tre `pieceLeftArm_s1_*` e `pieceRightArm_s2_p1` è falso.
  - `armDrawKeepsDecalsHidden`: `SuitBones.decal(LEFT_ARM)` e `decal(TORSO)` falsi con `full()` e con `BARE`, per i due bracci.
  - `armDrawRejectsOtherSlotsAndUnknownBones`: `assertThrows(IllegalArgumentException.class, …)` per `visibleForArm(SuitBones.ARMOR_RIGHT_ARM, BARE, slot)` con `slot` in `HELMET`, `TORSO`, `LEFT_LEG`, `RIGHT_LEG`, e per `visibleForArm("someOtherBone", BARE, RIGHT_ARM)`; `assertThrows(NullPointerException.class, …)` per `view` nulla e per `arm` nullo.
  - `armDrawShowsNothingTheWholeDrawHides`: per `view` in `BARE`, `full()` e la vista a due stati di `armDrawFollowsTheStateOfItsOwnPiece`, per i due bracci e per ogni osso di `BONES`: `visibleForArm(bone, view, arm)` non lancia (`assertDoesNotThrow`), e dove è vero è vero anche `visible(bone, view)` (design §13: il braccio mostra esattamente ciò che mostra la terza persona, mai un osso in più).
- [ ] **Step 2: Run** `./gradlew --console=plain :1.21.1-neoforge:test --tests '*SuitBoneRulesTest'` — Expected: FAIL (non compila: `visibleForArm` manca).
- [ ] **Step 3: implementare `visibleForArm`**, poi lo stesso comando — Expected: PASS (13 test nella classe).
- [ ] **Step 4: test JUnit dell'aritmetica** — `FirstPersonArmTest` (nuovo, in `client/` dei test; `assertShift(x, y, z, shift)` con tolleranza 1e-4 come in `PilotPoseTest`; candidati `float[] K = {1.65f, 1.3f, 1.0f}`):
  - `baseHandIsTheMiddleOfTheLastFourPixelsOfTheBaseArm`: `assertShift(-1f, 8f, 0f, baseHandPx(false))`, `assertShift(1f, 8f, 0f, baseHandPx(true))`; nel commento la scatola del braccio classico da cui vengono.
  - `suitPalmIsTheCentreOfThePalmCubeOfTheGeneratedGeometry`: dalla geometria generata, fra i cubi di `PilotPoseTest.handCubes("frameRightArm")` (quelli che finiscono a y ≤ 11,25) il più grande per volume è il palmo; il suo centro `(cx, cy, cz)` dà `assertShift(cx + 5, 22 - cy, cz, suitPalmPx(false))`, e vale (−4,75, 12,625, 0); lo stesso per `PilotPoseTest.handCubes("frameLeftArm")` con `cx - 5` e `suitPalmPx(true)`, che vale (4,75, 12,625, 0).
  - `palmFallsOnTheBaseHandAtEveryCandidateScale`: `assertShift(6.8375f, -12.83125f, 0f, pivotShiftPx(false, 1.65f, 0f))`, `assertShift(5.175f, -8.4125f, 0f, pivotShiftPx(false, 1.3f, 0f))`, `assertShift(3.75f, -4.625f, 0f, pivotShiftPx(false, 1.0f, 0f))`; e per ogni K e per i due bracci `pivotShiftPx(left, k, 0f) + k · suitPalmPx(left)` è `baseHandPx(left)`.
  - `sideShiftMovesThePalmAlongTheSideDirection`: per ogni K, per `s` in 0,5, 4,5, 8 e per i due bracci `pivotShiftPx(left, k, s) + k · suitPalmPx(left) − baseHandPx(left)` è `s · sideDirection(left)`; `assertShift(3.6774f, -6.75335f, 3.90555f, pivotShiftPx(false, 1.3f, 4.5f))`.
  - `sideDirectionPointsOutwardOnTheScreenAtRest`: con matrici 3×3 scritte nel test (rotazioni destrorse attorno a X, Y, Z), `R = Ry(f·45°) · Rz(f·120°) · Rx(200°) · Ry(−f·135°) · Rz(f·0,1 rad)` con `f` = 1 per il destro e −1 per il sinistro: `R · sideDirection(left)` vale (f, 0, 0) entro 2e-3, e `sideDirection` è lunga 1 entro 1e-3.
  - `constantsAreAmongTheDecidedValuesAndTheLeftArmMirrorsTheRight`: `FirstPersonArm.SCALE` è uno dei tre candidati e `1.65f == SuitScale.S`; `SIDE_SHIFT_PX` è fra 0 e 8 ed è un multiplo di 0,5; `armSlot(false) == PieceSlot.RIGHT_ARM`, `armSlot(true) == PieceSlot.LEFT_ARM`; per `baseHandPx`, `suitPalmPx`, `sideDirection` e `pivotShiftPx(·, 1.3f, 4.5f)` il sinistro ha la x opposta e le stesse y e z del destro.
  Run: `./gradlew --console=plain :1.21.1-neoforge:test --tests '*FirstPersonArmTest'` — Expected: FAIL (non compila: `FirstPersonArm` manca). `handCubes` e `GEO` di `PilotPoseTest` perdono `private` in questo step; nient'altro cambia in quel file.
- [ ] **Step 5: implementare la parte pura di `FirstPersonArm`** (costanti con `SCALE = SuitScale.S` e `SIDE_SHIFT_PX = 0f`, le cinque funzioni), poi lo stesso comando e `./gradlew --console=plain :1.20.1-forge:test --tests '*FirstPersonArmTest' --tests '*SuitBoneRulesTest'` — Expected: PASS (6 e 13 test).
- [ ] **Step 6: cucitura, disegno, aggancio, documenti** — `renderArm` in `SuitRenderer` e `SuitRendererImpl`, `onHandPrepared`, il gestore, il Javadoc di `PilotPose`, `check_jars.sh`, `docs/compat.md`, tutto come in "Produces". Prima di scrivere il Javadoc del gestore, la verifica del bersaglio: `for j in $(find ~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft -name '*.jar' ! -name '*sources*' \( -path '*/minecraft-merged/*' -o -path '*/forge-1.20.1-47.4.29-minecraft-merged/*' -o -path '*/neoforge-21.1.266-minecraft-merged/*' \) | sort); do javap -p -c -cp "$j" net.minecraft.client.renderer.entity.player.PlayerRenderer 2>/dev/null | awk '/private void renderHand\(/,/^$/' | grep -c 'PlayerModel.setupAnim'; done` — Expected: quattro righe `1`.
- [ ] **Step 7: `tools/client/arm_measure.sh`** (`chmod +x`), con `--selftest` che costruisce con `convert` in una cartella temporanea quattro immagini 960×540 e controlla sette uscite: `senza` = `xc:'rgb(120,170,230)'` con `-fill 'rgb(90,140,70)' -draw 'rectangle 0,300 959,539'`; `con` = `senza` più `-fill 'rgb(200,0,200)' -draw 'rectangle 700,400 899,539' -fill 'rgb(140,140,150)' -draw 'rectangle 690,370 739,399'`; `dentro` = `con` più `-fill 'rgb(90,140,110)' -draw 'rectangle 600,450 699,499' -fill 'rgb(120,170,231)' -draw 'rectangle 0,0 99,29' -fill 'rgb(120,190,230)' -draw 'rectangle 470,250 479,259'`; `altro` = `con` più `-fill 'rgb(60,60,70)' -draw 'rectangle 700,400 799,539'`. Attesi: `sagoma con senza` → `pixel=29500 copertura=5.69 mirino=0 barra=0 energia=0 riquadro=210x170+690+370`; `sagoma dentro senza` → `pixel=34600 copertura=6.67 mirino=100 barra=3500 energia=0 riquadro=430x290+470+250` (la differenza nel solo blu conta, quella di 1/255 no); `sagoma senza senza` → `pixel=0 copertura=0.00 mirino=0 barra=0 energia=0 riquadro=0x0+0+0`; `magenta con` → `magenta=28000 riquadro=200x140+700+400`; `magenta con 480x540+0+0` → `magenta=0 riquadro=0x0+0+0`; `magenta con 100x50+750+420` → `magenta=5000 riquadro=100x50+750+420`; `uguali con altro senza` → `sagoma=29500 uguali=15500`. Run: `bash tools/client/arm_measure.sh --selftest` — Expected: `OK arm_measure`; `bash tools/client/arm_measure.sh sagoma /dev/null /dev/null; echo $?` — Expected: una riga `ERRORE:` su stderr e `2`.
- [ ] **Step 8: build e controlli di fine task** — la build delle quattro varianti con `collectJar` → `BUILD SUCCESSFUL`; il conto dei JUnit → quattro righe con 331; `bash tools/check_sources.sh` → `OK sources`; `bash tools/check_jars.sh` → `OK jars` (i due nomi nuovi ci sono in ogni jar, nello spazio dei nomi del suo loader); `bash tools/run_gametests.sh` → quattro righe `OK <variante>: 208 test obbligatori passati, nessun avviso fuori elenco`; `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure`.
- [ ] **Step 9: prima vista nel client vero, sulle quattro varianti**, una alla volta: `EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <variante> <cartella> "execute as Exo run exoframe debug wear rottame 0 FF00FF FF00FF" "@attendi 2" "@scatto braccio" "execute as Exo run exoframe debug wear via" "@attendi 2" "@scatto base"` — Expected: uscita 0 e `log-check.txt` vuoto (nessun errore di applicazione dei mixin, nessun `Suit rendering failed`); `bash tools/client/arm_measure.sh magenta <cartella>/braccio.png` → `magenta` almeno 1000; `… magenta <cartella>/base.png` → `magenta=0 riquadro=0x0+0+0`. Guardare `braccio.png`: in basso a destra il braccio della tuta con le piastre magenta, grande (K è ancora 1,65) e non rimpicciolito, senza il braccio di pelle del giocatore; `base.png`: il braccio nudo di sempre.
- [ ] **Step 10: commit** `client: il braccio della tuta in prima persona (regola delle ossa, cucitura, aggancio); tools: arm_measure.sh`. `git status --short` non elenca scatti.

