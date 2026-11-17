# Task 1 — Il braccio della tuta al posto di quello del gioco: rapporto

Stato: **DONE**. BASE 78e15a0b39cae10eff54712c3b723afe12e20146, HEAD 3bfc80c155296aa792b7accd15ca09e1b1542a8d, ramo `m3d-braccio`.
Due commit, firmati dall'ambiente:
- `6ddc8b9 client: il braccio della tuta in prima persona (regola delle ossa, cucitura, aggancio); tools: arm_measure.sh` (il task: su questo codice sono girati tutti i controlli di fine task e lo Step 9);
- `3bfc80c docs: compat.md, i nomi nei jar e il controllo (e2) con i due bersagli del braccio in prima persona` (solo `docs/compat.md`, due righe: vedi "Scostamenti dal brief"; dopo, `bash tools/check_sources.sh` → `OK sources`).

## Che cosa ho fatto

- **`SuitBoneRules.visibleForArm(bone, view, arm)`** (Java puro): vero per l'osso di primo livello e l'osso del telaio di quel braccio e, fra le ossa del pezzo dello slot `arm`, per quelle che `visible` mostra; falso per ogni altro osso di `BONES`. Ordine dei controlli: `view` nulla e `arm` nullo (NPE), slot che non e' un braccio (`IllegalArgumentException("not an arm slot: " + arm)`), osso fuori da `BONES` (`IllegalArgumentException`, come `visible`). Una frase in piu' nel Javadoc della classe.
- **`SuitRenderer.renderArm`** (firma del piano) con il contratto nel Javadoc; in `SuitRendererImpl` `render` e `renderArm` passano da un solo `draw` privato (il corpo di `render` di prima), con `this.arm = arm` accanto a `this.view = ...` e `this.arm = null` accanto a `this.view = BARE` nel `finally`; `applyBoneVisibilityBySlot` passa su ogni nome di `BONES` nei due modi (`arm == null ? visible : visibleForArm`). Nessuna direttiva nuova.
- **`client/FirstPersonArm`** (nuova): le nove costanti con la ragione del numero, le cinque funzioni pure, `onHandPrepared` scritto come nel brief (copia privata della posa, `try/catch` di `RuntimeException | LinkageError`, `SuitDraw.logFailureOnce`, `false` dopo l'errore, `model.young = false`, `arm.xRot = 0f`). `SCALE = SuitScale.S`, `SIDE_SHIFT_PX = 0f`.
- **Terzo gestore di `PlayerRendererMixin`**, `exoframes$suitArm`, come nel brief (INVOKE su `PlayerModel.setupAnim`, `shift = AFTER`, cancellabile, una chiamata); Javadoc della classe con il terzo aggancio, il perche' della posizione e la verifica del bersaglio.
- **`PilotPose`**: solo la frase del Javadoc della classe. **`PilotPoseTest`**: solo `GEO` e `handCubes` da `private static` a package-private.
- **`tools/check_jars.sh` (e2)**: `renderHand(` e `setupAnim(` in `MOJANG`; la voce `client.PlayerRendererMixin` in `TARGETS` per le quattro coppie; commento di testa del controllo e commento sopra `TARGETS` aggiornati.
- **`docs/compat.md`**: i sei punti del brief (`renderArm` in "A cosa serve", la riga Uso, `visibleForArm` nel paragrafo di `SuitBoneRules`, i tre agganci di `PlayerRendererMixin` con il terzo al posto della frase "La prima persona ... non cambia.", `FirstPersonArm` nel punto "Disegno del giocatore", la voce `FirstPersonArm` nell'elenco delle classi di `client/`).
- **`tools/client/arm_measure.sh`** (eseguibile): `sagoma`, `magenta`, `uguali`, `--selftest`, con le formule del piano.

## File cambiati

Creati: `src/main/java/com/toxineren/exoframes/client/FirstPersonArm.java`, `src/test/java/com/toxineren/exoframes/client/FirstPersonArmTest.java`, `tools/client/arm_measure.sh`.
Modificati: `compat/client/SuitBoneRules.java`, `compat/client/SuitRenderer.java`, `compat/impl/client/SuitRendererImpl.java`, `mixin/client/PlayerRendererMixin.java`, `client/PilotPose.java` (Javadoc), `src/test/.../compat/client/SuitBoneRulesTest.java`, `src/test/.../client/PilotPoseTest.java`, `tools/check_jars.sh`, `docs/compat.md`.
Primo commit: 12 file, 857 righe aggiunte, 22 tolte; secondo: `docs/compat.md`, 2 righe. `git status --short` dopo i commit: vuoto (gli scatti stanno fuori dal repository).

## Prove TDD

**Regola delle ossa (Step 1-3).**
- RED: `./gradlew --console=plain :1.21.1-neoforge:test --tests '*SuitBoneRulesTest'` con i sei test nuovi e senza `visibleForArm`:
  ```
  SuitBoneRulesTest.java:145: error: cannot find symbol
              if (SuitBoneRules.visibleForArm(bone, view, arm)) {
    symbol:   method visibleForArm(String,SuitView,PieceSlot)
    location: class SuitBoneRules
  16 errors
  > Task :1.21.1-neoforge:compileTestJava FAILED
  ```
  Atteso: il metodo non esisteva.
- GREEN: stesso comando dopo l'implementazione: `BUILD SUCCESSFUL`, `SuitBoneRulesTest tests="13" skipped="0" failures="0" errors="0"`.

**Aritmetica del posto (Step 4-5).**
- RED: `./gradlew --console=plain :1.21.1-neoforge:test --tests '*FirstPersonArmTest'` senza la classe:
  ```
  FirstPersonArmTest.java:26: error: cannot find symbol
          assertShift(-1f, 8f, 0f, FirstPersonArm.baseHandPx(false));
    symbol:   variable FirstPersonArm
  > Task :1.21.1-neoforge:compileTestJava FAILED
  BUILD FAILED in 1s
  ```
  Atteso: la classe non esisteva.
- GREEN: stesso comando dopo la parte pura: `BUILD SUCCESSFUL`, `FirstPersonArmTest tests="6" skipped="0" failures="0" errors="0"`; `./gradlew --console=plain :1.20.1-forge:test --tests '*FirstPersonArmTest' --tests '*SuitBoneRulesTest'`: `BUILD SUCCESSFUL`, 6 e 13 test, zero falliti.

## Verifica del bersaglio (Step 6)

Il comando del brief (javap sui quattro jar: Forge 47.4.29, vanilla 1.20.1, vanilla 1.21.1, NeoForge 21.1.266): quattro righe `1`. Nel bytecode di `renderHand` (letto su NeoForge): `setModelProperties`, i tre campi, `PlayerModel.setupAnim:(LivingEntity;FFFFF)V`, poi `arm.xRot = 0` e i due disegni.

**Il bersaglio INVOKE dopo remapJar (mai provato prima, nota del registro):** si rimappa su tutte e quattro. Nelle classi dei jar di `build/libs/all`:
- Fabric 1.20.1 e 1.21.1: `method_23205(Lnet/minecraft/class_4587;...)V` e `Lnet/minecraft/class_591;method_17087(Lnet/minecraft/class_1309;FFFFF)V`
- Forge 1.20.1: `m_117775_(...)V` e `Lnet/minecraft/client/model/PlayerModel;m_6973_(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V`
- NeoForge 1.21.1: `renderHand(...)V` e `Lnet/minecraft/client/model/PlayerModel;setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V`
Nessun `Cannot remap` nel log della build delle quattro varianti (0 righe).

## Controlli di fine task (Step 8), tutti sul codice del commit

- Build: `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar`
  ```
  BUILD SUCCESSFUL in 27s
  52 actionable tasks: 34 executed, 18 up-to-date
  ```
  Nessun `warning:` e nessun `error:` nel log.
- Conto dei JUnit (comando del piano):
  ```
  1.20.1-forge 331
  1.20.1-fabric 331
  1.21.1-fabric 331
  1.21.1-neoforge 331
  ```
- `bash tools/check_sources.sh` → `OK sources`
- `bash tools/check_jars.sh` → `OK jars`
- `bash tools/run_gametests.sh` (con `EXO_ACCEPT_EULA=true`), uscita 0:
  ```
  OK 1.20.1-forge: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.20.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-fabric: 208 test obbligatori passati, nessun avviso fuori elenco
  OK 1.21.1-neoforge: 208 test obbligatori passati, nessun avviso fuori elenco
  ```
- `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure` (le sette uscite attese del brief, alla lettera); `bash tools/client/arm_measure.sh sagoma /dev/null /dev/null; echo $?` → `ERRORE: lo scatto '/dev/null' non si legge come immagine` su stderr e `2`.

Dopo questi controlli non ho piu' toccato nessun file prima del commit `6ddc8b9` (lo Step 9 non ha chiesto correzioni); il secondo commit cambia solo `docs/compat.md`, che nessuno di questi controlli legge.

## Prima vista nel client vero (Step 9), quattro varianti, una alla volta

Comando del brief, cartelle fuori dal repository. Per ogni variante: uscita 0, `log-check.txt` vuoto (0 byte), nessuna riga `Suit rendering failed` in `client.log`.

| Variante | durata | `magenta braccio.png` | `magenta base.png` |
|---|---|---|---|
| 1.20.1-forge | 77 s | `magenta=57297 riquadro=457x224+503+316` | `magenta=0 riquadro=0x0+0+0` |
| 1.20.1-fabric | 62 s | `magenta=57261 riquadro=457x224+503+316` | `magenta=0 riquadro=0x0+0+0` |
| 1.21.1-fabric | 63 s | `magenta=57084 riquadro=457x224+503+316` | `magenta=0 riquadro=0x0+0+0` |
| 1.21.1-neoforge | 73 s | `magenta=57401 riquadro=457x224+503+316` | `magenta=0 riquadro=0x0+0+0` |

Ho guardato `braccio.png` e `base.png` di 1.21.1-neoforge e di 1.20.1-forge (una per versione di Minecraft): in `braccio.png` in basso a destra c'e' il braccio della tuta con le piastre magenta, la barra d'acciaio del telaio e l'ottone in cima, grande (non a meta' misura: il riquadro del magenta e' largo 457 pixel), senza braccio di pelle; e' il primo scatto di un client appena avviato, prima di ogni F5 e dell'inventario (il segno `young`). In `base.png`, dopo `debug wear via`, il braccio nudo di sempre e nessun pixel magenta. Il riquadro del magenta e' lo stesso al pixel sulle quattro varianti.

Scatti e log: `<area-temporanea>/scratchpad/shot-<variante>/` (cartella temporanea della sessione, fuori dal repository).

## Cio' che ho visto e che serve al Task 2

- A K = 1,65 con spostamento 0 il braccio passa dietro la barra degli oggetti, come prevedeva il piano: `magenta braccio.png 380x100+290+440` → circa 9 000 pixel (`magenta=9241 riquadro=167x100+503+440` su NeoForge, 9137 Forge, 9101 e 8924 Fabric). Non e' un difetto di questo task: grandezza e posto li sceglie il Task 2.
- In `braccio.png` della prima vista c'e' ancora la riga di chat del `gamemode creative` dello strumento (x 0-664, y 442-460) e in tutti e due gli scatti l'avviso "Chat messages can't be verified" in alto a destra (x circa 500-960, y 0-110): la prima vista misura solo il magenta, che non li tocca; per le misure di `sagoma` vale la regola di `@attendi 8` del piano. Quell'avviso c'e' ancora a 11 s dall'ingresso del giocatore (ingresso a 39 s, `base.png` a 50 s; su NeoForge): chi scrive la sequenza del Task 2 lo tenga presente per le coppie di scatti misurate con `sagoma` e `uguali` (se compare in uno solo dei due scatti entra nella sagoma, fuori dai tre rettangoli ma dentro `pixel`, `copertura` e `riquadro`).
- `-fx` di ImageMagick rende `magenta` la misura piu' lenta: 1,0 s per scatto (misurato su `braccio.png`); il selftest intero dura 4,4 s.

## Scelte

- **Ordine dei controlli in `visibleForArm`:** `view`, `arm` (NPE), poi lo slot (IAE), poi l'osso (IAE). Il brief fissa le eccezioni ma non l'ordine; cosi' uno slot sbagliato si rifiuta anche con un osso valido, e un osso sconosciuto non diventa mai `false`.
- **`arm_measure.sh`, il riquadro:** prima di `%@` aggiungo una cornice nera di un pixel e la tolgo dalle coordinate, e con zero pixel stampo `0x0+0+0` senza chiedere nulla a ImageMagick: `%@` ritaglia il colore degli angoli, e su una maschera tutta bianca o che tocca i bordi darebbe un riquadro sbagliato (provato: scatto tutto magenta → `960x540+0+0`; rettangolo `10x10+950+530` → `10x10+950+530`).
- **`arm_measure.sh`, uscite:** 2 per uso sbagliato, scatto illeggibile o non 960×540, rettangolo scritto male o fuori dallo scatto, programmi mancanti; 1 per una misura che non riesce o per il selftest che non torna. Il piano fissa solo "960×540, altrimenti uscita 2" e "uscita 0 con una riga"; il resto segue `client_shot.sh`.
- **`arm_measure.sh`, "uguali":** un pixel e' uguale in `a` e in `b` se non "differisce" con la stessa soglia del 4 % (il contrario esatto di "differiscono"); e' la lettura che da' il valore atteso del selftest.
- **`arm_measure.sh`, canale alfa:** gli scatti si leggono con `-alpha off`, cosi' un PNG con il canale alfa non cambia le misure.
- **`check_jars.sh`:** la classe ha una costante `RENDERER = "client.PlayerRendererMixin"` usata sia in `CLIENT` sia in `TARGETS`, come `CULLING` e `SPRINT`.
- **`docs/compat.md`, punto "Disegno del giocatore":** dopo la frase su `PilotPose` ho scritto che il terzo aggancio chiama `FirstPersonArm.onHandPrepared`, che agisce per un giocatore di cui si legge la vista della tuta (`SuitLayer.suitView`): e' la condizione vera del codice del brief (non c'e' il controllo "non e' spettatore", che in prima persona non serve: da spettatore il gioco non chiama `renderHand`).
- **Test:** la vista a due stati sta in un aiuto privato `twoStates()` usato da due test; `ARMS` e' una costante della classe di test.

## Scostamenti dal brief

Nessuno nel codice, nei numeri o nei testi. Due note di forma:
- Il commento di testa del controllo (e2) e il commento sopra `TARGETS` dicono tutti e due che il braccio in prima persona porta i nomi attesi dei suoi due bersagli (il brief chiede il primo; il secondo descriveva la tabella ed era diventato incompleto).
- Javadoc di `SuitRendererImpl`: oltre a `draw` ho aggiunto due frasi (classe e `applyBoneVisibilityBySlot`) che dicono che ogni osso si decide a ogni disegno: e' la proprieta' del punto 1 di Review Focus, che li' JUnit non prova.

- Secondo commit, solo documenti: nella sezione "Mixin" di `docs/compat.md` il punto "Nessun refmap" elencava i nomi rimappati nei jar e diceva per quali mixin (e2) fissa i nomi esatti; con questo task era diventato incompleto, e il brief non lo nominava. Ho aggiunto i quattro nomi nuovi (`method_23205`, `class_591.method_17087`, `m_117775_`, `m_6973_`, letti nei jar) e il braccio in prima persona fra i bersagli fissati, e al punto "Verifica" una frase su come si e' provato il terzo aggancio (lo Step 9). Nessun file Java toccato dopo i controlli di fine task.

## Lasciato alla revisione (fuori dai file del task)

- `client/SuitLayer.java`, Javadoc di `suitView`: dice "Per {@link PilotPose}, che non decodifica il riassunto una seconda volta"; ora lo chiama anche `FirstPersonArm.onHandPrepared`. Il file non e' fra quelli del task e non l'ho toccato.
- `tools/client/README.md` non nomina ancora `arm_measure.sh`: il paragrafo e' del Task 2, come dice il brief.

## Autorevisione

- Completezza: ogni voce di "Produces" c'e'; i sei punti di `docs/compat.md` spuntati uno per uno; 6 + 6 test nuovi (13 e 6 nelle classi, 331 in tutto).
- Review Focus 1: in `applyBoneVisibilityBySlot` il ciclo e' su tutto `BONES` nei due modi; `arm` torna `null` nel `finally` insieme a `view`; `renderArm` rifiuta lo slot prima di toccare lo stato. Focus 2: `model.young = false` prima del disegno; visto nello scatto. Focus 3: nessun braccio di pelle in `braccio.png`. Focus 4: i nomi per loader in (e2), le quattro varianti partono. Focus 5: il corpo di `onHandPrepared` e' quello del brief. Focus 6 e 8: i JUnit nominati ci sono e passano. Focus 9: `base.png` senza magenta, il braccio nudo.
- Regole: nessuna direttiva Stonecutter nuova; `FirstPersonArm` non ha campi statici di tipi di Minecraft (il test la carica senza il gioco); nessun gestore e nessun metodo chiamato porta il nome di un bersaglio (`check_jars.sh` verde); nessuna riga di log nuova; identificatori ed eccezioni in inglese, commenti in italiano con apostrofi ASCII.
- Non ho trovato nulla da correggere dopo la prima stesura, a parte una frase del Javadoc di `SuitBoneRules` ("le tre del braccio" → "le ossa di quel braccio") e la forma di `arm_measure.sh` (le misure in variabili locali, con uscita 1 se una non riesce), sistemate prima dei controlli di fine task.

## Dubbi

Nessuno che fermi il task. Cio' che questo task non puo' provare e resta alla revisione e ai task seguenti, come dice il piano: la tuta intera dopo il braccio nello stesso fotogramma (forma del codice qui; `m3d_shots.sh` nel Task 2), il mancino e la mappa nel client vero (Task 2), i jar di produzione sul PC (Task 3).
Processi: alla fine restano solo i due demoni di Gradle in attesa; nessun server, client o Xvfb.
