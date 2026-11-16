# Task 7 — Il generatore dei dati e i file committati

Stato: DONE. BASE `9619ec68ef115006f6234168b99f9642f2cc2fc0`, HEAD `b5db281cf7d73f5f109b6372dacb9e92cb47b18b`.
Un commit: `b5db281 data: generatore dei dati per versione; ricette, sblocchi, bottino e tag del caricatore`.
All'inizio HEAD era BASE e l'albero di lavoro era pulito: nessun tentativo precedente da riprendere.
`<tmp>` qui sotto e' la cartella temporanea della sessione (fuori dal repository).

## Che cosa ho implementato

**`tools/gen_data.py`** (solo libreria standard, gira con `python3 -I`; docstring con l'uso):

- tabelle `INGREDIENTS` (I, C, R, r, Q) e `RECIPES` (oggetto, categoria, righe con `_` per il vuoto) con i valori di
  "Ricette" del piano; `CATEGORIES`; `KINDS` e `VERSIONS` (per versione: radice dell'albero, cartella di ricette,
  avanzamenti, tabelle di bottino, tag dei blocchi e tag degli oggetti, chiave del risultato `item`/`id`);
- `build(version) -> dict` (percorso relativo a `src/main`, con `/` → contenuto): sette ricette, sette sblocchi, la
  tabella di bottino e il tag, con i contenuti di "Produces" alla lettera (in `pattern` il vuoto e' uno spazio; in
  `key` solo i simboli usati dalla ricetta);
- `serialize(obj)` = `json.dumps(obj, indent=2, sort_keys=True, ensure_ascii=True) + "\n"`; i file si scrivono in
  ASCII con `\n`;
- `validate(files_by_version)`: `ValueError` con un messaggio che comincia con il percorso del file. Impone tutte le
  voci del brief: `count` 1; categoria fra `building`, `redstone`, `equipment`, `misc`; `pattern` rettangolare da 1x1
  a 3x3; una chiave per ogni simbolo diverso dallo spazio e nessuna chiave non usata; ogni id `spazio:percorso` (id
  del file, ingredienti, risultato, oggetti del bottino, valori dei tag); per ogni ricetta il suo sblocco con lo
  stesso id nei premi; stessi id di ricette, sblocchi, bottini e tag sulle due versioni; nessun percorso nelle
  cartelle dell'altra versione; nessun `remove` nei tag. In piu' (vedi "Scelte"): la chiave del risultato della
  versione, `replace` falso nei tag, il tipo `minecraft:crafting_shaped`;
- `main()`: `--out <dir>` e `--selftest` (si escludono); `validate()` gira prima di ogni scrittura, e se rifiuta
  qualcosa esce con 1 senza scrivere nessun file;
- `--selftest`: i sette difetti del brief, uno alla volta su una copia in memoria; conta solo una segnalazione che
  nomina il file giusto e il motivo giusto (un pezzo del messaggio atteso), altrimenti `FAIL: <difetto>: segnalato
  per un altro motivo` o `difetto non segnalato` e uscita 1. Stampa `OK selftest (7 difetti segnalati)`.

**`tools/check_data.sh`** (eseguibile, commento di testa come `check_art.sh`): selftest; rigenera con `--out` in una
cartella temporanea; `FAIL: manca src/main/<percorso> (python3 -I tools/gen_data.py)`; `FAIL: src/main/<percorso>
diverso da quello generato (python3 -I tools/gen_data.py)` (`cmp -s`); `FAIL: <percorso> non viene dal generatore` per
ogni file dei due alberi che il generatore non scrive; `FAIL: <percorso>: i dati per versione non stanno nelle risorse
comuni` per tutto cio' che sta sotto `src/main/resources/data` e non e' una delle due `empty.nbt`; `OK data`. Come
`check_art.sh`, fallisce anche se `--out` non scrive nessun file.

**I 32 file** sotto `src/main/resources-mc1.20.1/data/` e `src/main/resources-mc1.21.1/data/`, sedici per albero, ai
percorsi della tabella "File dei dati". I sette sblocchi sono identici byte per byte sulle due versioni (`diff -r`).

**`tools/check_jars.sh` (f3)**, dopo (f2), dentro il ciclo dei jar: `err "$name" "cartella di dati di un'altra
versione: <percorso>"` per ogni percorso che corrisponde a `^data/[^/]+/(…)/` con le cartelle dell'altra versione (le
due espressioni del brief); `err "$name" "manca <percorso>"` per le sette ricette, i sette sblocchi, il bottino e il
tag nelle cartelle della versione del jar. Commento sopra con il perche'. Le due strutture vuote non sono toccate.

**`.github/workflows/build.yml`**: il passo `Controllo dei dati generati` (`bash tools/check_data.sh`) dopo
"Controllo della grafica generata".

**`tools/README.md`**: le righe di `check_data.sh` e di `gen_data.py` nella tabella, "dati della sola versione del
jar" nella riga di `check_jars.sh`, e il paragrafo "Dati per versione: `gen_data.py`" (i due alberi, i 32 file, come si
rigenera, che cosa impone `validate`, che cosa fanno `--selftest`, `check_data.sh` e (f3)).

## Che cosa ho provato

### Step 1 — selftest

    $ python3 -I tools/gen_data.py --selftest; echo "uscita $?"
    OK selftest (7 difetti segnalati)
    uscita 0

Il selftest stesso messo alla prova (in memoria, senza toccare il file): con `_validate_tag` spento →
`FAIL: remove in un tag: difetto non segnalato`, uscita 1; con `tools` aggiunta alle categorie ammesse →
`FAIL: categoria tools: segnalato per un altro motivo: …`, uscita 1; con le cartelle non controllate →
`FAIL: un file di 1.20.1 sotto recipe/: difetto non segnalato`, uscita 1; con un ottavo "difetto" che non cambia
nulla → `difetto non segnalato`, uscita 1.

### Step 2 — il controllo che fallisce (RED)

`bash tools/check_data.sh` prima di scrivere i file: 32 righe, tutte `FAIL: manca src/main/… (python3 -I
tools/gen_data.py)`, uscita 1.

### Step 3 — i file (GREEN) e le prove negative

    $ python3 -I tools/gen_data.py && bash tools/check_data.sh
    32 file di dati scritti sotto /home/user/exosuits/src/main
    OK data
    $ find src/main/resources-mc1.20.1/data src/main/resources-mc1.21.1/data -type f | wc -l      → 32
    $ git status --short -uall src/main/resources-mc1.20.1/data src/main/resources-mc1.21.1/data | wc -l   → 32

Due esecuzioni con `--out` in due cartelle: `diff -r` vuoto (byte identici). Prove negative, tutte disfatte:

- `touch …/resources-mc1.21.1/data/redstone_exoframes/recipe/extra.json` →
  `FAIL: src/main/resources-mc1.21.1/data/redstone_exoframes/recipe/extra.json non viene dal generatore`, uscita 1;
- `count` 1 → 2 a mano in `recipes/exoframe.json` di 1.20.1 →
  `FAIL: src/main/resources-mc1.20.1/data/redstone_exoframes/recipes/exoframe.json diverso da quello generato (python3 -I tools/gen_data.py)`,
  uscita 1; `python3 -I tools/gen_data.py` la rimette, `OK data`;
- (in piu') `src/main/resources/data/redstone_exoframes/recipes/x.json` →
  `FAIL: src/main/resources/data/redstone_exoframes/recipes/x.json: i dati per versione non stanno nelle risorse comuni`,
  uscita 1; tolta, `OK data`.

### Step 4 — `check_jars.sh` (f3)

RED: sui jar di prima del task, 16 righe `manca …` per jar. Dopo la build con `collectJar` delle quattro: `OK jars`;
ogni jar ha 18 file sotto `data/` (i 16 della sua versione e le due `empty.nbt`) e i 16 sono identici ai sorgenti
(`unzip -p … | cmp`, 0 diversi su 16 per ciascuno dei quattro jar).

Prova negativa del brief, su una copia in `<tmp>/jars`:

    FAIL: redstone-exoframes-fabric-0.1.0+mc1.21.1.jar: cartella di dati di un'altra versione: data/redstone_exoframes/recipes/x.json
    uscita 1

Una seconda (in piu'), sul jar Forge 1.20.1 della copia: tolti il bottino e lo sblocco del caricatore, aggiunto
`data/minecraft/tags/item/x.json`:

    FAIL: redstone-exoframes-forge-0.1.0+mc1.20.1.jar: cartella di dati di un'altra versione: data/minecraft/tags/item/x.json
    FAIL: redstone-exoframes-forge-0.1.0+mc1.20.1.jar: manca data/redstone_exoframes/advancements/recipes/redstone/charger.json
    FAIL: redstone-exoframes-forge-0.1.0+mc1.20.1.jar: manca data/redstone_exoframes/loot_tables/blocks/charger.json
    uscita 1

### Step 5 — CI

`python3 -I -c '… assert "bash tools/check_data.sh" in t'` → nessuna uscita, uscita 0.

### Step 6 — i dati si caricano (e che cosa ho visto: la nota del ledger)

Il ledger chiedeva al primo task che incontra lo sblocco di dire che cosa ha visto: **il JSON dello sblocco si carica
sulle quattro varianti, senza righe WARN o ERROR**. Nel log dei GameTest:

| variante | `Loaded N recipes` | `Loaded N advancements` |
|---|---|---|
| 1.20.1-forge | 7 (su 1.20.1 conta i tipi) | 1278 |
| 1.20.1-fabric | 7 | 1279 |
| 1.21.1-fabric | 1298 | 1407 |
| 1.21.1-neoforge | 1298 | 1407 |

Prova negativa (in piu', disfatta subito): rovinati a mano nei due alberi la ricetta `exoframe` (la chiave del
risultato dell'altra versione) e lo sblocco `charger` (`minecraft:no_such_trigger`), poi
`bash tools/run_gametests.sh 1.20.1-fabric 1.21.1-neoforge` → uscita 1, con le righe che il brief prevedeva:

    FAIL 1.20.1-fabric: avvisi o errori del mod non previsti in tools/log_allowlist.txt:
    …server.log:113: riga non ammessa: [main/ERROR] (Minecraft) Parsing error loading recipe redstone_exoframes:exoframe
    …server.log:140: riga non ammessa: [main/ERROR] (Minecraft) Parsing error loading custom advancement redstone_exoframes:recipes/redstone/charger: Invalid criterion trigger: minecraft:no_such_trigger
    FAIL 1.21.1-neoforge: avvisi o errori del mod non previsti in tools/log_allowlist.txt:
    …server.log:94: riga non ammessa: [main/ERROR] [minecraft/RecipeManager]: Parsing error loading recipe redstone_exoframes:exoframe
    …server.log:126: riga non ammessa: [main/ERROR] [minecraft/ServerAdvancementManager]: Parsing error loading custom advancement redstone_exoframes:recipes/redstone/charger: Unknown registry key in ResourceKey[minecraft:root / minecraft:trigger_type]: minecraft:no_such_trigger …

e i conteggi scendono di uno (1.20.1-fabric 1279 → 1278 avanzamenti; 1.21.1-neoforge 1298 → 1297 ricette e 1407 →
1406 avanzamenti): quindi nell'esecuzione pulita quei file erano contati fra i caricati, e un file che il gioco non
legge fa davvero fallire `run_gametests.sh`. `check_data.sh` nel frattempo segnalava i quattro file come `diverso da
quello generato`. Poi `python3 -I tools/gen_data.py` → `OK data`, e le verifiche di chiusura qui sotto sono
sull'albero rimesso a posto.

Non provato qui (e' del Task 8): che il criterio `inventory_changed` scatti davvero per il giocatore finto e sblocchi
le sette ricette; che ogni ricetta dia il suo risultato dalla forma; che il bottino cada con il piccone.

### Step 7 — verifiche di chiusura (sull'albero del commit)

    $ ./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build \
        :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar
    BUILD SUCCESSFUL in 1s
    52 actionable tasks: 3 executed, 49 up-to-date
    $ bash tools/check_sources.sh     → OK sources
    $ bash tools/check_jars.sh        → OK jars
    $ bash tools/check_data.sh        → OK data
    $ bash tools/check_art.sh         → OK art
    $ bash tools/run_gametests.sh     (uscita 0)
    OK 1.20.1-forge: 202 test obbligatori passati, nessun avviso fuori elenco
    OK 1.20.1-fabric: 202 test obbligatori passati, nessun avviso fuori elenco
    OK 1.21.1-fabric: 202 test obbligatori passati, nessun avviso fuori elenco
    OK 1.21.1-neoforge: 202 test obbligatori passati, nessun avviso fuori elenco

(La prima build delle quattro con i dati nuovi, prima della prova negativa: `BUILD SUCCESSFUL in 34s`, 31 task
eseguiti; quella di chiusura trova quasi tutto aggiornato.) Dopo il commit `bash tools/check_data.sh` → `OK data`.
Nessun processo di Gradle di prova, server o client lasciato in esecuzione.

## TDD

- RED: Step 2 (32 `FAIL: manca`, uscita 1, prima dei file); (f3) sui jar vecchi (16 `manca` per jar).
- GREEN: `OK data` dopo `python3 -I tools/gen_data.py`; `OK jars` dopo la build.
- Le prove negative di `validate()` sono il `--selftest`, che `check_data.sh` esegue a ogni giro (CI compresa).

## File cambiati

- nuovi: `tools/gen_data.py`, `tools/check_data.sh` (100755), i 32 file dei dati;
- modificati: `tools/check_jars.sh` (+32 righe, (f3)), `.github/workflows/build.yml` (+2), `tools/README.md`.

Nessun file Java, nessuna cucitura nuova (`docs/compat.md` non cambia), nessun test in mondo nuovo (202, come dice la
tabella), nessuna riga nuova in `tools/log_allowlist.txt`, `build.gradle.kts` non toccato.

## Scelte

1. **`validate()` controlla anche la chiave del risultato per versione** (`item` su 1.20.1, `id` su 1.21.1; il
   risultato ha esattamente `count` e quella chiave). E' la sola differenza di contenuto fra le versioni, e una
   ricetta con la chiave dell'altra versione il gioco non la legge (visto nella prova negativa dello Step 6).
2. **`validate()` vuole `replace` falso nei tag** (oltre a nessun `remove`): il tag e' `minecraft:mineable/pickaxe`, e
   `replace: true` toglierebbe dal piccone tutti i blocchi del gioco. Una riga, nello spirito di "non scrive mai
   remove".
3. **`validate()` vuole il tipo `minecraft:crafting_shaped`** in ogni ricetta: le altre regole (pattern, key) valgono
   solo per quel tipo.
4. **Il selftest pretende file e motivo giusti**, non una `ValueError` qualunque: un difetto segnalato per un altro
   motivo non proverebbe il controllo (come le premesse di `tools/art/selftest.py`).
5. **"Una ricetta tolta da una sola versione"**: tolgo solo la ricetta (`redstone_cell` da 1.21.1), lo sblocco resta;
   la segnala il confronto fra le versioni, nominando il file di 1.20.1 che non ha il suo gemello. `validate()` non
   controlla il verso opposto dentro una versione (uno sblocco senza la sua ricetta): il brief non lo chiede, e il
   confronto fra le versioni piu' "ogni ricetta ha il suo sblocco" coprono i casi che il generatore puo' produrre.
6. **Messaggi di `gen_data.py` in italiano** (ASCII), come gli altri strumenti Python del repository
   (`tools/art/*.py`, `compare_png.py`): la regola "eccezioni in inglese" la leggo per il codice del mod.
7. **`check_data.sh` nasconde l'uscita del selftest** quando passa (come `check_art.sh` con il suo): cosi' lo Step 2
   da' esattamente le 32 righe `FAIL: manca`. Elenca i file in ordine (`sort -z`), cosi' l'uscita non dipende dal
   file system; per i file in piu' usa `find ! -type d`, che prende anche un collegamento simbolico.
8. **`check_data.sh` non fissa il numero 32**: fallisce se il generatore non scrive nulla (come `check_art.sh`); il
   numero esatto lo impongono il confronto dei due insiemi e, sui jar, l'elenco di (f3).
9. **(f3) segnala anche le voci di cartella del jar** (`data/x/recipes/`) con il nome dell'altra versione: e' quello
   che l'espressione del brief prende, e una cartella vuota con il nome sbagliato e' comunque un errore di chi l'ha
   messa.
10. **Una riga in piu' nel README**: nella riga di `check_jars.sh` l'elenco fra parentesi ora nomina i dati.

## Scostamenti dal brief

Nessuno nel comportamento. Due aggiunte fuori dal testo, tutte e due prove: la prova negativa dei dati rovinati nei
GameTest (Step 6) e la seconda prova negativa di (f3).

## Autorevisione

- Contenuti dei file riletti contro "Produces" e contro i formati della ricognizione §2 (ricetta, bottino, tag): gli
  stessi, a parte l'ordine delle chiavi (alfabetico, per il determinismo).
- Forme delle ricette rilette contro design §12.12 e la tabella del piano: sette su sette uguali.
- Nessun carattere non ASCII in `gen_data.py`, `check_data.sh` e nelle righe nuove di `check_jars.sh`; nessun dato
  personale; nulla copiato da fuori.
- Commit firmato dall'ambiente (intestazione `gpgsig` presente, come nel commit precedente).

## Osservazioni e dubbi

- (minore, non toccato: fuori dai file del task) il commento di `build.gradle.kts` sopra `resources.srcDir(…
  resources-mc$mc)` dice ancora "Oggi porta le texture trasparenti del layer armatura vanilla di 1.20.1": da questo
  commit la cartella porta anche i dati (e gia' prima la configurazione dei mixin).
- (minore) l'elenco dei sedici file per jar in (f3) e' scritto a mano in `check_jars.sh`, come dice il brief: una
  ricetta nuova in `RECIPES` che nessuno aggiunge li' passa `OK jars` senza che (f3) ne pretenda la presenza (il
  README e il docstring del generatore lo dicono; `check_data.sh` continua a garantire i sorgenti).
- (nota) su 1.20.1-fabric gli avanzamenti caricati sono 1279 contro i 1278 di Forge: la differenza di uno c'era
  anche con lo sblocco rovinato (1278 contro, presumibilmente, 1277) e non viene dai nostri file; non ho cercato da
  dove viene.
