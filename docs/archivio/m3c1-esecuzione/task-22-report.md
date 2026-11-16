# Task 22: La riga dell'energia — rapporto

**Stato:** DONE_WITH_CONCERNS (una preoccupazione di resa, sotto)
**BASE:** a102066ee1502ed2c6834be75be9faac3ccc5aac
**HEAD:** a5703f3cb0465c81929eae8a14b4a7ba3ecca464
**Commit:** `a5703f3 client: riga dell'energia per chi indossa la tuta`

## Che cosa ho fatto

- `client/EnergyLine.java` (nuovo): `record EnergyLine(int stored, int capacity, int percent, Optional<PieceSlot> worst, Optional<PieceCondition> worstCondition)` package-private, solo tipi di `core/`. `static Optional<EnergyLine> of(FrameState)`: vuoto senza cella; `percent = (int) (stored * 100L / capacity)`; il pezzo peggiore si cerca girando `PieceSlot.values()` in ordine con un confronto stretto (`isWorse`): solo `BROKEN`/`CRITICAL`, un `BROKEN` vince su ogni `CRITICAL`, poi `a.durability() * (long) b.maxDurability() < b.durability() * (long) a.maxDurability()`; a parità resta il primo slot visto.
- `client/EnergyHud.java` (nuovo, `final`, solo client): costanti `ENERGY`, `ENERGY_WORST`, `NO_CELL`, i quattro colori, `MARGIN = 4`, `BOTTOM = 12`; `static void render(GuiGraphics)` come nel brief (niente con `player == null`, `options.hideGui`, `!SuitSize.wearsSuit(player)` o `clientView` vuoto; testo da `EnergyLine`, con `ENERGY_WORST` + chiavi `piece.*`/`condition.*` quando c'è un pezzo peggiore, `NO_CELL` senza cella; colore solo dagli indicatori del server, bianco senza; `drawString(font, text, guiWidth - width - 4, guiHeight - 12, color, true)`). Javadoc: eccezione al "niente HUD" (§2, §12.13), sopra i sottotitoli (accettato), HUD vero in M4.
- `compat/client/ClientRenderHooks.java`: `void addHudRenderer(Consumer<GuiGraphics> renderer)` con Javadoc (ogni fotogramma sopra l'interfaccia; solo client, una volta dall'init client); Javadoc della classe aggiornato (la richiamata dell'HUD cambia fra versioni, non fra loader).
- `compat/impl/client/ClientRenderHooksImpl.java`: `Objects.requireNonNull(renderer, ...)` e `ClientGuiEvent.RENDER_HUD.register((graphics, partial) -> renderer.accept(graphics));`, una riga senza direttive, con il commento accanto sulla differenza 9.2.14 / 13.0.11.
- `client/RedstoneExoframesClient.init()`: `ClientCompat.renderHooks().addHudRenderer(EnergyHud::render);` subito dopo `addPlayerLayer`.
- Lang: le tre chiavi `hud.redstone_exoframes.energy|energy_worst|no_cell` dopo `condition.redstone_exoframes.broken` e prima di `tooltip.redstone_exoframes.exoframe.place`, in `en_us.json` e `it_it.json`, testi verbatim del brief (stesso insieme e stesso ordine di chiavi nei due file, controllato con `json.load`).
- `docs/compat.md`: `ClientRenderHooks` ("A cosa serve" con `addHudRenderer(disegno)`; "Differenze chiuse" con `RENDER_HUD` `(GuiGraphics, float)` / `(GuiGraphics, DeltaTracker)` chiusa senza direttive; "Condizione" precisa che `addHudRenderer` non ne ha; "Uso" con `addHudRenderer(EnergyHud::render)`); "Avvio del client": init con la riga dell'energia e un punto nuovo su `EnergyHud` e `EnergyLine`; tabella API: tolto "callback HUD di Architectury con `DeltaTracker`" dalle rinviate, riga nuova "`ClientGuiEvent.RENDER_HUD`: `(GuiGraphics, float)` → `(GuiGraphics, DeltaTracker)` | `ClientRenderHooks.addHudRenderer` (parte 3c-1)".
- `src/test/java/com/toxineren/exoframes/client/EnergyLineTest.java` (nuovo): i sei casi del brief.

## File cambiati

- `src/main/java/com/toxineren/exoframes/client/EnergyLine.java` (nuovo)
- `src/main/java/com/toxineren/exoframes/client/EnergyHud.java` (nuovo)
- `src/main/java/com/toxineren/exoframes/client/RedstoneExoframesClient.java`
- `src/main/java/com/toxineren/exoframes/compat/client/ClientRenderHooks.java`
- `src/main/java/com/toxineren/exoframes/compat/impl/client/ClientRenderHooksImpl.java`
- `src/main/resources/assets/redstone_exoframes/lang/en_us.json`, `it_it.json`
- `docs/compat.md`
- `src/test/java/com/toxineren/exoframes/client/EnergyLineTest.java` (nuovo)

## TDD

- **RED** — `./gradlew --console=plain :1.21.1-neoforge:test --tests '*EnergyLineTest'` → uscita 1, `:1.21.1-neoforge:compileTestJava FAILED`, `EnergyLineTest.java:26: error: cannot find symbol` (e righe seguenti): atteso, `EnergyLine` non esisteva.
- **GREEN** — `./gradlew --console=plain :1.20.1-fabric:test --tests '*EnergyLineTest' :1.21.1-neoforge:test --tests '*EnergyLineTest'` → `BUILD SUCCESSFUL`; XML dei risultati: `tests="6" failures="0" errors="0"` su tutte e due (rieseguiti, `compileJava` e `test` nel log). Il `build` finale li ha eseguiti anche su 1.20.1-forge e 1.21.1-fabric: 6/6 su ciascuna delle quattro varianti.

## Client vero (Step 5)

`EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <variante> <cartella> "gamemode survival Exo" "execute as Exo run exoframe debug wear rottame" "@attendi 2" "@scatto riga-energia" "execute as Exo run exoframe debug wear rottame 2" "@attendi 2" "@scatto riga-energia-critico"`, una variante alla volta:

- **1.20.1-fabric** (cartella `<scratchpad>/shot-1201fabric`): uscita 0, `log-check.txt` di 0 byte, `[71s] log: nessun avviso o errore del mod fuori dall'elenco degli avvisi voluti`. `riga-energia.png`: in basso a destra, verde, `Energy 9996 / 10000 RE (99%)`. `riga-energia-critico.png`: `Energy 9996 / 10000 RE (99%) · Helmet critical`, verde.
- **1.21.1-neoforge** (cartella `<scratchpad>/shot-1211neoforge`): uscita 0, `log-check.txt` di 0 byte, `[75s] log: nessun avviso o errore del mod fuori dall'elenco degli avvisi voluti`. Stesse due righe, stesso colore.
- `back.png`, `front.png`, `quarter.png` (interfaccia nascosta) delle due varianti: nessun pixel verde della riga nell'angolo in basso a destra (contati con PIL: 0 su ciascuno dei sei scatti) e nessuna riga a vista.

(La percentuale è 99 e non 100 perché il tick in sopravvivenza consuma pochi RE prima dello scatto, come il brief prevede.)

## Verifiche di fine task (Step 7)

- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` → `BUILD SUCCESSFUL in 24s` (ultima esecuzione, sul codice del commit), nessun `warning:` né `error:`.
- `bash tools/check_sources.sh` → `OK sources`
- `bash tools/check_jars.sh` → `OK jars`
- `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh` (sul codice del commit) → uscita 0:
  - `OK 1.20.1-forge: 168 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.20.1-fabric: 168 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-fabric: 168 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-neoforge: 168 test obbligatori passati, nessun avviso fuori elenco`

Nessun server, client o build lasciato in esecuzione (solo il demone di Gradle, inattivo).

## Scelte

- `EnergyHud.render` è package-private (`static void render`), come scrive il brief: lo chiama solo `RedstoneExoframesClient`, nello stesso package. Le costanti di `EnergyHud` sono package-private per lo stesso motivo.
- In `EnergyHud.text` ho controllato sia `worst` sia `worstCondition` prima di usarli, anche se `EnergyLine.of` li dà sempre insieme: costa una condizione e non lancia mai se qualcuno costruisce il record a mano.
- `docs/compat.md`, "Avvio del client": oltre a nominare la riga nell'elenco di `init()`, ho aggiunto un punto proprio per `EnergyHud` e `EnergyLine` (come hanno `ExitKey`/`ExitHold`), con regole del testo, del colore e del pezzo peggiore; la riga "Condizione" di `ClientRenderHooks` ora dice che `addHudRenderer` non ha direttive.
- Il Javadoc della classe `ClientRenderHooks` diceva "che ogni loader espone in modo diverso": l'ho allargato, perché l'HUD cambia fra versioni e non fra loader.

## Scostamenti dal brief

Nessuno nel comportamento. Solo il testo d'esempio dello Step 5 (`(100%)`): negli scatti la percentuale era 99 % (9996 RE), caso che il brief stesso ammette ("o pochi RE in meno").

## Revisione di me stesso

Rilette le differenze: firme e testi come nel brief, righe sotto i 120 caratteri, commenti e Javadoc in italiano con apostrofi ASCII, identificatori e messaggi d'eccezione in inglese, nessuna direttiva Stonecutter nuova, `EnergyLine` senza import di Minecraft (regole 1-8 di `check_sources.sh` passano), `EnergyHud`/`EnergyLine` nominati solo da `client/`.

## Preoccupazioni

1. **La riga può coprire la barra degli oggetti.** Negli scatti (finestra 960 × 540, scala dell'interfaccia 2, larghezza dell'interfaccia 480) la riga con il pezzo peggiore (`Energy 9996 / 10000 RE (99%) · Helmet critical`, circa 235 pixel d'interfaccia) parte da x ≈ 241, dentro la barra degli oggetti (x da 149 a 331): copre gli slot 7-9 (si vede in `riga-energia-critico.png`). Anche la riga italiana senza pezzo (`Energia 9996 / 10000 RE (99 %)`) arriva di poco sopra la barra. Con la scala automatica a 1920 × 1080 la larghezza dell'interfaccia è la stessa (480), quindi succede anche lì. La posizione è quella fissata dal brief e dal design (§12.13, "Riga dell'energia" del piano: `x = larghezza - larghezzaTesto - 4`, `y = altezza - 12`), quindi non l'ho cambiata: la decisione (accettarla fino a M4, o spostare la riga, per esempio sopra la barra o andando a capo) spetta a chi coordina.

## Fix round 1

**FIX_BASE:** a5703f3cb0465c81929eae8a14b4a7ba3ecca464. Albero pulito alla partenza (nessun tentativo interrotto da riprendere).

### T22-F1 — la riga in alto a sinistra (ruling del ledger)

- `client/EnergyHud.java`: prima riga (`ENERGY`, o `NO_CELL` senza cella) a `x = LEFT_MARGIN` (4), `y = TOP` (4), allineata a sinistra, con ombra, nel colore dello stato dell'energia come prima (indicatori del server; bianco senza). Seconda riga solo quando `EnergyLine` da' un pezzo peggiore: `Component.translatable(WORST, nome del pezzo, condizione)` a `x = 4`, `y = TOP + LINE_HEIGHT` (14), `LOW_COLOR` per `CRITICAL`, `EMPTY_COLOR` per `BROKEN` (metodo privato `drawWorst`). Costanti: `MARGIN` -> `LEFT_MARGIN`, `BOTTOM` -> `TOP` (package-visible); in piu' `LINE_HEIGHT = 10` (package-visible), per non scrivere 14 a mano. `ENERGY_WORST` -> `WORST`. Il metodo `text` sparisce (niente piu' unione con `·`). Javadoc della classe riscritto: angolo in alto a sinistra, due righe e loro colori, non tocca barra degli oggetti, indicatore del salvataggio e sottotitoli; la copre la schermata di F3.
- Lang: `hud.redstone_exoframes.energy_worst` sostituita al suo posto (riga 60 dei due file) da `hud.redstone_exoframes.worst` = `%s %s` in `en_us.json` e `it_it.json`.
- `docs/compat.md` (punto `EnergyHud` / `EnergyLine` di "Avvio del client"): posizione, due righe, chiave nuova, colori della seconda riga, `SuitDamage.pieceName`.
- `git grep -n 'basso a destra' -- docs README.md src`: restano solo tre frasi che non parlano della riga dell'energia (la sagoma dei pezzi dell'HUD di M4 nei due documenti di design generali, la X dell'inventario creativo nella checklist 3b); il design M3 §12.13 non fissa la posizione (§6 la lascia al costruttore), quindi non c'era nulla da cambiare. Il piano non e' toccato. Nessuno scatto della riga era nel repository (il commit a5703f3 non ne aggiunge).
- `EnergyLine` ed `EnergyLineTest` invariati.

### T22-F2 — chiave del nome del pezzo in un posto solo

- `frame/SuitDamage.pieceName(PieceSlot)` da `private` a `public` (Javadoc: unico posto che costruisce la chiave); `EnergyHud` la chiama per la seconda riga. `grep -rn '"piece\.' src/main/java` ora trova solo `SuitDamage.java:187`.

### Client vero (1.20.1-forge e 1.21.1-fabric, finestra 960x540, uno alla volta)

Comando, con `<v>` la variante e la cartella `.superpowers/sdd/2026-11-15-m3c1-consumare/task-22-client/<v>/`: `EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <v> <cartella> "gamemode survival Exo" "execute as Exo run exoframe debug wear rottame" "@attendi 2" "@scatto riga-energia" "execute as Exo run exoframe debug wear rottame 2" "@attendi 2" "@scatto riga-energia-critico" "bossbar add exo:prova \"Boss\"" "bossbar set exo:prova players Exo" "@attendi 1" "@scatto riga-energia-boss"` (i passi dello Step 5 piu' una barra del boss in coda, per vedere se la riga la tocca).

- **1.20.1-forge**: uscita 0, `log-check.txt` 0 byte, `[83s] log: nessun avviso o errore del mod fuori dall'elenco degli avvisi voluti`. RENDER_HUD scatta su Forge.
- **1.21.1-fabric**: uscita 0, `log-check.txt` 0 byte, `[74s] log: nessun avviso o errore del mod fuori dall'elenco degli avvisi voluti`. RENDER_HUD scatta su Fabric 1.21.1.
- Scatti (in ciascuna delle due cartelle), guardati:
  - `riga-energia.png`: in alto a sinistra, verde, `Energy 9996 / 10000 RE (99%)`; nient'altro del mod sullo schermo, barra degli oggetti libera.
  - `riga-energia-critico.png`: la stessa riga verde e sotto, gialla, `Helmet critical`; barra degli oggetti libera.
  - `riga-energia-boss.png`: le due righe e la barra del boss `Boss` in alto al centro; la prima riga finisce sopra l'estremo sinistro della barra senza toccarla (pixel verdi in x 4-156, y 4-11 dell'interfaccia; barra da x 149, y 12), la seconda corta.
  - `back.png`, `front.png`, `quarter.png` (F1): nessuna riga.
- Conteggio dei pixel (PIL, tolleranza 6 per canale; script nello scratchpad): sulle due varianti uguale, `riga-energia` 1320 pixel verdi nella fascia in alto a sinistra, 0 gialli sotto; `riga-energia-critico` 1320 verdi e 596 gialli nella fascia della seconda riga; nessun pixel verde o giallo della riga sotto y = 48 dello schermo (barra degli oggetti compresa) in nessuno scatto; 0 nei tre scatti F1.

### Verifiche di chiusura

- `./gradlew --console=plain :1.21.1-neoforge:build` (iterazione) -> `BUILD SUCCESSFUL in 7s`.
- `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` -> `BUILD SUCCESSFUL in 23s`, `52 actionable tasks: 23 executed, 29 up-to-date`, nessun `warning:` ne' `error:` (i `compileJava` di forge e 1.21.1-fabric erano gia' aggiornati dalle esecuzioni del client sullo stesso codice).
- `EnergyLineTest` (JUnit, dentro i `build`): `tests="6" failures="0" errors="0"` sulle quattro varianti.
- `bash tools/check_sources.sh` -> `OK sources`
- `bash tools/check_jars.sh` -> `OK jars`
- `EXO_ACCEPT_EULA=true bash tools/run_gametests.sh` -> uscita 0:
  - `OK 1.20.1-forge: 168 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.20.1-fabric: 168 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-fabric: 168 test obbligatori passati, nessun avviso fuori elenco`
  - `OK 1.21.1-neoforge: 168 test obbligatori passati, nessun avviso fuori elenco`

Nessun build, server o client lasciato in esecuzione.

### Preoccupazione

- **La seconda riga in italiano puo' toccare la barra del boss.** Il ruling dice che nessuna riga arriva alla barra del boss; in inglese e' vero (scatto `riga-energia-boss.png`), ma la barra sta in `y = 12..16` e parte da `x = larghezza/2 - 91` (149 a larghezza 480, 122 a circa 427), mentre la seconda riga occupa `y = 14..21`. Con le larghezze dei caratteri del font del gioco (6 px la maggior parte, `i` 2, `t` 4, spazio 4) `Braccio sinistro in stato critico` e' larga circa 158 px (fino a x 162) e `Gamba sinistra in stato critico` circa 154: con una barra del boss a schermo e un pezzo critico, in italiano, le ultime lettere coprono l'inizio della barra (circa 13 px a 480, circa 40 px a 427). La prima riga italiana (`Energia 10000 / 10000 RE (100 %)`, circa 174 px) resta sopra la barra (finisce a y 11-12). Non provato nel client in italiano: calcolo. Soluzioni possibili, se chi coordina la vuole: un'altra posizione per la seconda riga, o testi piu' corti.
