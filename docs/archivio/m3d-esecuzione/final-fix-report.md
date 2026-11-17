# Ondata di correzioni dopo la revisione finale: m3d-braccio

FIX_BASE: ffe2ab2f73c305ad545b2a76486b89ead79c30e7

## FF-1 (la barra degli oggetti solo a 960x540)
- `docs/test-in-gioco/m3d-braccio.md` passo 2 (:68): guardare fin dove arriva il braccio di base rispetto alla barra (serve al passo 3).
- passo 3 (:69): tolto "fuori dalla barra degli oggetti"; l'avambraccio arriva piu' o meno dove arrivava il braccio di base al passo 2; a 960x540 resta fuori dalla barra; con un'altra finestra (anche 854x480) o certe scale il suo angolo puo' finire sotto l'ultima casella, come quello di base, e non e' un errore; da riportare solo se va molto piu' in la' del braccio di base.
- :48: finestra 16:9, la mano nella stessa posizione rispetto ai bordi (calcolo, non provato); la barra cresce a scatti con la scala ("Automatica" = la piu' grande permessa), per cui l'angolo puo' finirle sotto con altre finestre o scale.
- :18: "lasciano liberi nella finestra di prova (960x540)".
- `README.md:9`: "fuori dal mirino e, a 960×540, dalla barra degli oggetti".
- Formulazione prudente ("puo' finire"): le posizioni nelle altre finestre sono l'aritmetica della revisione, non misurate. SCALE e SIDE_SHIFT_PX non toccati.
- Copertura: documento; commit 8fb0b9b.

## FF-2 (Javadoc di PilotPose)
- `src/main/java/com/toxineren/exoframes/client/PilotPose.java:28-33`: "La logica del pilota sta qui"; gli agganci del pilota (i due di PlayerRendererMixin su scala e modello in posa, i due di ItemInHandLayerMixin) chiamano i metodi on...; il terzo aggancio di PlayerRendererMixin, il braccio in prima persona, chiama FirstPersonArm. Commit c299ba8, con T1-F2 e T1-F3.

## FF-3 (la riga gialla al passo 7)
- `m3d-braccio.md` passo 7 (:73): sotto la riga dell'energia una seconda riga gialla, nome di un pezzo seguito da "in stato critico" ("critical"), della parte 3c, attesa (con lo stato 2 tutti i pezzi sono critici). Testi verificati: `condition.redstone_exoframes.critical` in it_it.json ed en_us.json; colore in `EnergyHud` (giallo se critico). Commit 8fb0b9b.

## FF-4 (mostri nel mondo di prova)
- `m3d-braccio.md` passo 2 (:68): `/difficulty peaceful` subito dopo l'ingresso (comando indipendente dalla lingua; la pacifica toglie anche i mostri gia' nati, slime compresi, cosa che `doMobSpawning false` non fa). Nessun passo della checklist ha bisogno di mostri o della fame. Commit 8fb0b9b.

## T1-F2 (SuitLayer.suitView)
- `src/main/java/com/toxineren/exoframes/client/SuitLayer.java:66-68`: "Per PilotPose e FirstPersonArm, che non decodificano il riassunto una seconda volta; per FirstPersonArm null vuol dire lasciare il braccio al gioco base." Commit c299ba8.

## T1-F3 (SuitRenderer)
- `src/main/java/com/toxineren/exoframes/compat/client/SuitRenderer.java:10-14`: nomina il terzo uso, renderArm, chiamato dentro il disegno della mano in prima persona (a parole: nessun riferimento a `client/` da `compat/client/`). Commit c299ba8.

## T2-F5 (nomi italiani in m3d_shots.sh)
- `tools/client/m3d_shots.sh:206` `mis` -> `take_measure` (non `measure`, che e' gia' la variabile con il percorso di arm_measure.sh); `:223-224` `esito` -> `verdict`. 36 chiamate rinominate; testo delle righe e sentinella `misura-non-riuscita` invariati.
- Prova: `bash -n` ok; `M3D_SOLO_CONTROLLI=1` sugli scatti salvati di 1.20.1-forge e 1.21.1-neoforge (copie nella cartella temporanea): quattordici righe identiche a `<variante>.controlli.txt`, uscita 0. Prova negativa: con braccio-rottame.png sostituito da base-prima.png, cinque KO (dipinto, criteri, solo-tuta, telaio-piazzato, specchio), riga FAIL, uscita 1. Poi le quattro esecuzioni vere (sotto). Commit 64660a0.

## T3-F2 (passo 16)
- `m3d-braccio.md` passo 16 (:85): percorso italiano "Opzioni... → Grafica... → Scala" (verificato su it_it.json: options.video "Grafica...", options.guiScale "Scala"); campo visivo con il nome "Campo visivo"; prima F11, poi le scale 1, 3, 4 a schermo intero, "se un valore non c'e', annota il massimo che il gioco offre"; poi di nuovo finestra; l'angolo sotto l'ultima casella come quello di base non e' un errore ma si annota. Commit 8fb0b9b.

## T3-F1 (Aspetto...)
- `m3d-braccio.md:17` e `:78`: "Opzioni... → Aspetto... → Mano principale" (it_it.json: options.skinCustomisation "Aspetto..."). Il passo 14 rimanda al 12. Commit 8fb0b9b.

## T3-F3 (scatti "uguali al pixel")
- `:46`: le misure uguali al pixel salvo due: il magenta della terza persona (di poco, anche fra due esecuzioni) e quello dell'anteprima, ritagliata su 1.21.1 e non su 1.20.1 (circa 1,75 volte).
- `:48`: "le stesse misure e scatti uguali a occhio"; la frase sui bordi come in FF-1. Commit 8fb0b9b.

## T3-F4 (passo 18)
- `m3d-braccio.md` passo 18 (:87): il secondo giocatore vede il telaio del passo 14, intero (testa, busto, due braccia, due gambe), con il solo Braccio Rottame sul lato dove e' stato montato. Commit 8fb0b9b.

## T3-F5 (commento di testa di m3d_shots.sh)
- `tools/client/m3d_shots.sh:62-67`: durate 207-225 s (111-119 e 76-86), come `tools/client/README.md:186`; magenta della terza persona 23593-23745; anteprima Forge 170-174 da x 320-321, magenta 18796-18830; Fabric 1.20.1 166-170 da x 321-323, magenta 18712-18786 (le larghezze tornano con README.md:135, "fra 166 e 174"). Commit 64660a0.
- Le quattro esecuzioni vere di questa ondata (sotto) hanno dato tre misure fuori da quegli intervalli: terza persona su 1.21.1-fabric magenta 23538; anteprima su 1.20.1-fabric larga 174 da x 319, magenta 18810; 1.20.1-forge 232 s, la prima sequenza dopo la build. Il commento di testa ora dice 23538-23745, Fabric 166-174 da x 319-323, magenta 18712-18810, e "232 s una volta, la prima sequenza dopo una build"; la stessa frase sui 232 s in tools/client/README.md:186. Le soglie non cambiano (larghezze contro 130 e 70). Commit b82a8da.

## Controlli di chiusura (su HEAD b82a8da)
- Build delle quattro varianti con collectJar (prima su 8fb0b9b, con i JUnit rieseguiti dopo il cambio dei Javadoc; poi su HEAD, tutto aggiornato): `BUILD SUCCESSFUL in 36s` / `BUILD SUCCESSFUL in 2s`, `52 actionable tasks: 52 up-to-date`.
- JUnit: `1.20.1-forge 331`, `1.20.1-fabric 331`, `1.21.1-fabric 331`, `1.21.1-neoforge 331`; nessun failure o error.
- `bash tools/check_sources.sh` → `OK sources`; `bash tools/check_jars.sh` → `OK jars`; `bash tools/check_data.sh` → `OK data`; `bash tools/check_art.sh` → `OK art`; `bash tools/client/arm_measure.sh --selftest` → `OK arm_measure`.
- `tools/run_gametests.sh` non eseguito: nel codice del mod sono cambiati solo Javadoc (tre file), nessuna riga di codice e nessun test.
- Client vero, `EXO_ACCEPT_EULA=true bash tools/client/m3d_shots.sh <variante> /home/user/m3d-scatti/<variante>`, una variante alla volta, con il codice di 64660a0 (identico a HEAD: fra 64660a0 e HEAD lo script cambia solo nel commento di testa):
  - `1.20.1-forge`: 14 ok, 0 KO, `log-check.txt` vuoto, uscita 0, 232 s; `OK 1.20.1-forge: scatti in /home/user/m3d-scatti/1.20.1-forge`
  - `1.20.1-fabric`: 14 ok, 0 KO, `log-check.txt` vuoto, uscita 0, 208 s
  - `1.21.1-fabric`: 14 ok, 0 KO, `log-check.txt` vuoto, uscita 0, 216 s
  - `1.21.1-neoforge`: 14 ok, 0 KO, `log-check.txt` vuoto, uscita 0, 210 s
  - Misure: criteri copertura 8.77 e riquadro 286x205 come prima; terza persona 23739 / 23665 / 23538 / 23745; anteprima 172x202+320+66 (18802), 174x202+319+66 (18810), 98x140+356+120 (10782, 10804).
- I `log-check.txt` di tutte e due le esecuzioni (`<variante>/` e `<variante>/mancino/`), otto file scritti dopo la build, sono tutti vuoti (0 byte).
- Con lo script di HEAD, `M3D_SOLO_CONTROLLI=1` sulle copie dei quattro gruppi di scatti nuovi: quattordici righe identiche a quelle delle esecuzioni, uscita 0.
- Nessun processo di Gradle, server, client o Xvfb lasciato attivo (`./gradlew --stop`, `pgrep` vuoto).

## Commit
- c299ba8 client: i Javadoc di PilotPose, SuitLayer.suitView e SuitRenderer nominano il braccio in prima persona (FF-2, T1-F2, T1-F3)
- 64660a0 tools: m3d_shots.sh, nomi delle funzioni in inglese e misure del commento di testa aggiornate (T2-F5, T3-F5)
- 8fb0b9b docs: checklist 3d, la barra degli oggetti nelle altre finestre, i nomi italiani dei menu, la riga gialla e la difficolta' pacifica (FF-1, FF-3, FF-4, T3-F1, T3-F2, T3-F3, T3-F4)
- b82a8da tools: m3d_shots.sh, il commento di testa e il README con le misure delle quattro esecuzioni di questa ondata (T3-F5)

Non corretti: nessuno. Contestati: nessuno.
