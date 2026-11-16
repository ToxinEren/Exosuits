### Task 10: Client vero, checklist per il PC, documenti, verifica finale della 3c

**Files:**
- Create: `tools/client/m3c2_shots.sh`, `docs/test-in-gioco/m3c-screenshot/` (scatti scelti del caricatore, senza metadati)
- Rename: `docs/test-in-gioco/m3c1-consumare.md` → `docs/test-in-gioco/m3c-giocarci.md` (`git mv`, poi le aggiunte)
- Modify: `tools/client/README.md`, `tools/README.md`, `README.md`, `docs/compat.md`, `docs/test-in-gioco/m3a-vedere-il-telaio.md` e `m3b-entrare-e-uscire.md` (solo la frase delle note di testa che annuncia la checklist della parte 3c: ora c'è), `docs/superpowers/specs/2026-11-13-m3-telaio-design.md` (solo dove l'esecuzione ha deciso altro, elenco sotto)

**Interfaces:**
- Consumes: `tools/client/client_shot.sh` e lo schema di `m3c_shots.sh` (`steps=(…)`, riga finale `OK`/`FAIL`); i comandi del gioco `recipe take`, `loot give … loot`, `execute if block … [stato]`, `execute if data block`, `data get block <pos> <chiave>` (il risultato è il numero); le chiavi NBT del caricatore; il giocatore `Exo` in 0,5 −60 0,5 rivolto a sud: con l'inclinazione 55 il mirino cade sull'erba del blocco 0 −61 1, a z 1,63 (il caricatore posato lì finisce in 0 −60 1, rivolto a nord; con 50 cadrebbe a z 1,86, troppo vicino al bordo del blocco a 2,0), con l'inclinazione 60 sul fianco nord di quel caricatore; `tools/client/m3c_shots.sh` e `m3c_rules_shots.sh`; la checklist della 3c-1 (passi numerati da 1 a 75 in sedici sezioni, più "Cosa riportare" e le domande per il proprietario).
- Produces:
  - `tools/client/m3c2_shots.sh <variante> <cartella>` (commento di testa come `m3c_shots.sh`; `<cella>` per variante come nel Task 5 Step 6), un'esecuzione di `client_shot.sh` in sopravvivenza con, in ordine:
    1. **sblocco:** `give $u minecraft:copper_ingot`, `@attendi 2`, poi per i sette oggetti `recipe take $u redstone_exoframes:<oggetto>` (riesce solo se la ricetta era stata sbloccata dal lingotto);
    2. **bottino:** `loot give $u loot redstone_exoframes:blocks/charger`, poi `execute store result score #car exo_shot run clear $u redstone_exoframes:charger 0` e `execute if score #car exo_shot matches 1`;
    3. **posa con l'oggetto:** `item replace entity $u weapon.mainhand with redstone_exoframes:charger`, `tp $u 0.5 -60 0.5 0 55`, `@attendi 1`, `@click destro`, `@attendi 1`, `execute if block 0 -60 1 redstone_exoframes:charger[facing=north,cell=false,lit=false]`, `@scatto caricatore-posato`;
    4. **cella:** `tp $u 0.5 -60 0.5 0 60`, `item replace entity $u weapon.mainhand with <cella da 3 000>`, `@attendi 1`, `@click destro`, `@attendi 1`, `execute if data block 0 -60 1 {has_cell:1b,cell_energy:3000}`, `execute if block 0 -60 1 redstone_exoframes:charger[cell=true,lit=false]`;
    5. **polvere e carica:** `item replace entity $u weapon.mainhand with minecraft:redstone 8`, `@click destro`, `@attendi 0.5`, `@scatto caricatore-acceso` (mezzo secondo dopo il clic: la riga dello stato c'è ancora), `@attendi 1.5`, `execute if block 0 -60 1 redstone_exoframes:charger[cell=true,lit=true]`, `execute store result score #e1 exo_shot run data get block 0 -60 1 cell_energy`, `@attendi 4`, `execute store result score #e2 exo_shot run data get block 0 -60 1 cell_energy`, `execute if score #e2 exo_shot > #e1 exo_shot`;
    6. **ritiro:** `@click destro` (mano vuota), `@attendi 1`, `@scatto caricatore-svuotato` (subito dopo l'attesa, prima dei controlli: la riga dello stato svanisce fra 2 e 3 s dal clic), `execute if block 0 -60 1 redstone_exoframes:charger[cell=false,lit=false]`, `execute store result score #celle exo_shot run clear $u redstone_exoframes:redstone_cell 0`, `execute if score #celle exo_shot matches 1`;
    7. **rottura:** `@click destro` (la cella torna dentro), `@attendi 1`, `gamemode creative $u`, `@click sinistro`, `@attendi 0.3`, `tp $u 0.5 -60 -5.5 0 15` (via dal raggio di raccolta prima che finisca il ritardo: cella e polvere cadono ferme al centro del blocco, a un blocco dal giocatore, con un ritardo di raccolta di 40 tick, e dopo 2 s lui le raccoglierebbe, anche in creativa, facendo fallire a caso i controlli che seguono), `execute if block 0 -60 1 minecraft:air`, `execute if entity @e[type=minecraft:item,nbt={Item:{id:"redstone_exoframes:redstone_cell"}}]`, `execute if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:redstone"}}]`, `gamemode survival $u`;
    8. `tp $u 0.5 -60 -5.5 0 15` per i tre scatti finali (resta, anche se il passo 7 ha già portato lì il giocatore).
    Non prova (lo dice il commento di testa e il README): il clic da accovacciati e il banco da lavoro (lo strumento non clicca con un tasto premuto e non trascina oggetti), i suoni, le tramogge (le provano i test in mondo con tramogge vere).
  - `docs/test-in-gioco/m3c-giocarci.md`: la checklist della 3c-1 rinominata e completata. **I passi da 1 a 75 tengono il loro numero e il loro testo** (chi sta già facendo la checklist della 3c-1 riporta gli stessi numeri); cambia ciò che la 3c-2 ha reso falso e si aggiungono le sezioni nuove prima di "Cosa riportare":
    - numeri delle sezioni: le sezioni da 0 a 15 tengono il loro numero; le cinque nuove sono "16. Ricette", "17. Caricatore", "18. Tramogge", "19. Rottura e salvataggio", "20. La partita completa"; "Cosa riportare" diventa la 21 e le domande per il proprietario la 22; la tabella di riepilogo di "Cosa riportare" copre le sezioni 1-20, e così la frase "Sono le sezioni 1-15" di "Già verificato nella sessione cloud"; i passi nuovi continuano da 76 nell'ordine delle sezioni 16-20, e quelli aggiunti alle sezioni esistenti prendono i numeri che seguono l'ultimo della sezione 20, in coda alla loro sezione (nessun passo vecchio cambia numero);
    - la sezione 15 "Server dedicato con valori diversi" (passi 72-74) è già il controllo sul PC che la decisione su FA-2 chiede (`client_rules` ricevuto da un server con la config diversa da quella del client): resta com'è, senza passi nuovi;
    - titolo e introduzione: "milestone 3, parte 3c (giocarci)", i jar di dopo l'unione della 3c-2, una riga per chi ha già fatto i passi 1-75 con i jar della 3c-1 (rifare solo le sezioni nuove e i passi segnati);
    - "Cosa c'è da vedere": in più il caricatore (che cosa fa, i casi del clic, i quattro testi di stato, le tramogge, i tempi: una polvere = 500 RE = 10 s, una cella vuota piena in 200 s con 20 polveri), le ricette (tabella delle forme, sblocco al primo lingotto di rame, la cella esce vuota), il suggerimento del caricatore (cinque righe);
    - "Cosa non c'è ancora": via caricatore e ricette; restano riparazione, animazione del cambio, tuta senza cella, HUD vero, incantesimi e regole dei mob;
    - "Già verificato nella sessione cloud": 208 test in mondo, `m3c_shots.sh`, `m3c_rules_shots.sh` e `m3c2_shots.sh` sulle quattro varianti, con la tabella dei controlli nuovi e l'elenco di ciò che il cloud non prova (banco da lavoro, clic da accovacciati, suoni del caricatore, tramogge viste, comparatore, tempi a orologio);
    - sezione "Ricette" (passi da 76): in sopravvivenza, raccolto un lingotto di rame, il libro delle ricette mostra le sette ricette; fabbricare ciascun oggetto al banco con la forma della tabella; la cella fabbricata dice "Vuota: caricala in un caricatore" e un telaio con quella cella rifiuta l'ingresso; un pezzo fabbricato dice "Nuovo";
    - sezione "Caricatore": posa (il fronte verso chi lo piazza; i tre aspetti: vuoto, con la cella, acceso, che fa luce 7); senza piccone si rompe ma non cade il blocco (cadono cella e polvere), con un piccone qualunque cade; i casi del clic uno per uno con il testo atteso sulla barra delle azioni e il suono; "accovacciato a mani vuote" con la nota che un oggetto nella mano secondaria (uno scudo) fa saltare il clic al gioco; scambio (la vecchia cella va in mano); creativa (la mano non si consuma); tempi a orologio (2 polveri: dal 0 al 10 % in 20 s; 20 polveri: 200 s); comparatore (0 senza cella e con la cella vuota, 15 a cella piena);
    - sezione "Tramogge": sopra con due celle (ne entra una), di lato con la polvere, sotto (esce solo la cella piena); una catena cassa → tramoggia → caricatore → tramoggia → cassa che carica quattro celle senza toccarle; chiunque può prendere la cella di un altro (domanda 11);
    - sezione "Rottura e salvataggio": rotto con cella e polvere dentro; salvare e uscire mentre carica, rientrare: continua da dove era; due giocatori sullo stesso caricatore, se ci sono due client;
    - sezione "La partita completa": mondo nuovo in sopravvivenza, materie prime con `/give` (34 ferro, 14 rame, 2 blocchi di redstone, 2 quarzi, 24 polveri), poi tutto a mano: fabbricare, caricare due celle, armare, entrare, consumare fino all'energia bassa, un colpo di cactus, cambio cella con R, uscire, ricaricare; annotare che cosa non torna e quanto dura;
    - passi aggiunti alle sezioni esistenti: il pacchetto di dati della mod che su 1.21.1 può comparire come "fatto per una versione più vecchia" (non è un errore); il passo fuori dal telaio con le regole del Task 7 della 3c-1 (un gradino basso come terra arata o tappeto si sale; falò, blocco di magma, cactus e gli altri pericoli si evitano); nessun passo aspetta "silenzio in chat" con il registro in sola lettura (lì `/exoframe debug energy` risponde "Non indossi un telaio");
    - T22-F3 della 3c-1: il passo della seconda riga della riga dell'energia (pezzo critico in giallo, rotto in rosso) resta; la riga "Energia: nessuna cella" resta non raggiungibile in gioco (non si entra senza una cella carica e il cambio ne rimette sempre una), e la checklist continua a dirlo;
    - controllare che ogni voce dell'elenco "Checklist per il PC" di design §12.15 abbia un passo, e aggiungere quelle che mancano;
    - "Cosa riportare" e le domande per il proprietario in coda, con tre domande nuove: il quarzo nella ricetta della cella (design §8 domanda 3), la garanzia del caricatore (domanda 10), chiunque può prendere una cella (domanda 11), più una sui tempi di carica.
  - Documenti:
    - `tools/client/README.md`: riga d'uso e paragrafo di `m3c2_shots.sh` (che cosa controlla ogni passo, gli scatti, che cosa non prova, durata misurata);
    - `README.md`: "Stato" (fatta la parte 3c-2: caricatore, ricette, dati per versione; la milestone 3 è verificata nel cloud e aspetta la conferma sul PC con `m3c-giocarci.md`); il paragrafo dei controlli con `check_data.sh` e `gen_data.py`; il numero dei test in gioco (208, o quello misurato); gli script del client vero; "Documenti": la checklist `m3c-giocarci.md` al posto di `m3c1-consumare.md`, i piani fino a M3c-2;
    - `tools/README.md`: riletto contro gli strumenti di oggi;
    - `docs/compat.md`: rilettura contro il codice di tutte le cuciture di questa parte (`CompatEntityBlock`, `CompatBlockEntity`, `CompatBlockItem`, i cinque aiuti nuovi di `GameTests`, lo schema NBT del caricatore, la tabella API, i limiti noti);
    - design, solo queste correzioni, ciascuna con il rimando al task: §12.10 e §12.11 (in un caricatore la carica si taglia quando la cella entra a mano; una tramoggia non taglia mai, né mettendo una cella da sopra né rimettendo quella che non è riuscita a prendere da sotto: `setItem` non distingue i due casi, e un taglio lì distruggerebbe la carica di una cella letta dal chunk sopra la capacità; Task 4, 5 e 6), §12.11 (il metodo neutro del clic è `onClick` e su 1.21.1 si ridefinisce solo `useItemOn`; la cella prende uno stack nuovo all'ingresso; la carica non si taglia al caricamento del chunk; lo scambio suona come un inserimento; con ritmi oltre una polvere per tick il ritmo vero è una polvere per tick), §12.12 (il tag sta sotto `data/minecraft/`; `tools/check_data.sh`; la forma a lista dei criteri di sblocco), §12.15 (gli script `m3c_rules_shots.sh` e `m3c2_shots.sh`, `EXO_SHOT_SERVER_CONFIG`, la checklist nata dalla rinomina), §12.16 (`content/ChargerItem`; i nomi degli aiuti di `GameTests`).

- [ ] **Step 1:** scrivere `tools/client/m3c2_shots.sh` come in "Produces" (`chmod +x`); `bash -n tools/client/m3c2_shots.sh` — Expected: nessuna uscita.
- [ ] **Step 2: client vero sulle quattro varianti**, una alla volta: `EXO_ACCEPT_EULA=true bash tools/client/m3c2_shots.sh <variante> <cartella>` e poi `EXO_ACCEPT_EULA=true bash tools/client/m3c_shots.sh <variante> <cartella-2>` per `1.20.1-forge`, `1.20.1-fabric`, `1.21.1-fabric`, `1.21.1-neoforge` — Expected: per ciascuna esecuzione `OK <variante>: scatti in <cartella>`, uscita 0, `log-check.txt` vuoto; guardare gli scatti: `caricatore-posato` (il fronte vuoto verso il giocatore), `caricatore-acceso` (fronte acceso, `Charger: charging …%`), `caricatore-svuotato` (`Charger: no cell`). Annotare le durate.
- [ ] **Step 3: scatti di riferimento** — `caricatore-posato` e `caricatore-acceso` di una variante 1.20.1 e di una 1.21.1: `convert <cartella>/<scatto>.png -strip -define png:exclude-chunks=date,time docs/test-in-gioco/m3c-screenshot/<variante>-<scatto>.png` — Expected: nessuna uscita da
  ```bash
  python3 -I -c 'import struct, sys
  for f in sys.argv[1:]:
      d = open(f, "rb").read(); i = 8; kinds = []
      while i < len(d):
          n = struct.unpack(">I", d[i:i + 4])[0]; kinds.append(d[i + 4:i + 8]); i += 12 + n
      bad = [k for k in kinds if k in (b"tEXt", b"iTXt", b"zTXt", b"eXIf", b"tIME")]
      assert not bad, (f, bad)' docs/test-in-gioco/m3c-screenshot/*.png
  ```
- [ ] **Step 4: server dedicato** avviato e fermato sulle quattro varianti, una alla volta (`eula=true` in `versions/<v>/run/eula.txt`, accettazione dell'EULA di Minecraft per il server locale di prova come fanno gli script del client; prima di ogni avvio `rm -rf versions/<v>/run/world`: il mondo di sviluppo che c'è è più vecchio del blocco, dell'oggetto e del tipo di block entity di questa parte, e con un mondo così Forge rinumera gli id e scrive un WARN che nomina la mod, `Registry minecraft:item: Object did not get ID it asked for`, come nel Task 8 della 3c-1; la cartella è fuori dal repository (`.gitignore`); poi `./gradlew --console=plain :<v>:runServer` fino a `Done (`, poi `stop`) — Expected: arriva a `Done (`; `python3 -I tools/check_log.py client --variant <v> versions/<v>/run/logs/latest.log` → nessuna uscita, uscita 0.
- [ ] **Step 5: la checklist** — `git mv docs/test-in-gioco/m3c1-consumare.md docs/test-in-gioco/m3c-giocarci.md`, poi le modifiche di "Produces". Run: `grep -c '^- \[ \] \*\*[0-9]*\.\*\*' docs/test-in-gioco/m3c-giocarci.md` — Expected: più di 75; `grep -n '^- \[ \] \*\*75\.\*\*' docs/test-in-gioco/m3c-giocarci.md` — una riga (i numeri vecchi ci sono ancora); `grep -c '^## [0-9]*\. ' docs/test-in-gioco/m3c-giocarci.md` — 23 (le sezioni da 0 a 22) e `grep -n '^## 15\. Server dedicato' docs/test-in-gioco/m3c-giocarci.md` — una riga; `grep -rn 'm3c1-consumare' README.md docs tools --include='*.md' | grep -v 'superpowers/plans'` — nessuna riga fuori dalla frase che racconta la rinomina.
- [ ] **Step 6: documenti** come in "Produces".
- [ ] **Step 7: verifica finale del "fatta quando" della 3c** (design §4.6 e §12: una partita completa, sulle quattro varianti, da un solo sorgente). Ogni riga della tabella va riscritta nel rapporto con l'esito su ciascuna variante:

  | Che cosa | Prova |
  |---|---|
  | costruire con le ricette | `everyRecipeIsFoundByKey`, `everyRecipeCraftsFromItsShape`, `aCopperIngotUnlocksTheSevenRecipes`; `m3c2_shots.sh` passo 1 |
  | caricare al caricatore | i test di `ChargerGameTests`; `m3c2_shots.sh` passi 3-6 |
  | entrare, consumare energia | `EnergyGameTests`; `m3c_shots.sh` |
  | prendere danno, pezzi che si consumano e si rompono | `DamageGameTests` |
  | cambiare cella | `CellGameTests`; `m3c_shots.sh` passo 10 |
  | ricaricare, tutto di fila | `survivalFromCraftingToRecharging` |
  | nulla si perde, nulla si duplica | Review Focus 1-3 con i loro test |
  | un solo sorgente, quattro jar | build, `check_sources.sh`, `check_jars.sh` |

  Comandi: `./gradlew --console=plain :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar` → `BUILD SUCCESSFUL`; `bash tools/check_sources.sh` → `OK sources`; `bash tools/check_jars.sh` → `OK jars`; `bash tools/check_art.sh` → `OK art`; `bash tools/check_data.sh` → `OK data`; `bash tools/run_gametests.sh` → quattro righe `OK <variante>: 208 test obbligatori passati, nessun avviso fuori elenco`; `grep -rn 'isCreative()' src/main/java/com/toxineren/exoframes/content src/main/java/com/toxineren/exoframes/core | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)'` → nessuna riga; `git diff --stat main -- tools/log_allowlist.txt` → nessuna riga (questa parte non aggiunge avvisi voluti).
- [ ] **Step 8: commit** `docs: checklist completa della 3c, prova del caricatore nel client vero, documenti`.

---

## Minori della 3c-1 lasciati aperti

Presi nel Task 1: FW-B1, FW-B2, FW-B3, la prova FA-2, e dei minori segnati "leave" T1-F1, T1-F2, T1-F3, T10-F1, T14-F1, T21-F1, T21-F2, più due raccomandazioni della revisione finale (il test dei file di lingua, l'ascoltatore della sonda). T22-F3 va nella checklist (Task 10). T18-F1 è già chiuso dall'ondata finale (FB-1). Restano aperti, perché non toccano un giocatore e non costano poco quanto valgono:

- T2-F1: le righe `OK`/`FAIL` degli script dicono "fuori elenco" anche per un conteggio sbagliato (cambiarle cambierebbe l'uscita attesa di ogni task).
- T3-F3: una riga di 121 caratteri in `SlotGuard`.
- T4-F1, T4-F2: posto di una frase e un rimando in più nei commenti del registro.
- T5-F1, T5-F4: testo dei due log quando file e copia sono rovinati in modi diversi; T5-F2: un `Path` allocato per chiamata.
- T6-F1: una riga vuota mancante; T6-F2: lo spareggio fra due telai attorno agli occhi senza test.
- T7-F1: la ricerca del carrello sotto il posto senza test.
- T8-R1-B1: una chiave ripetuta in una mappa della config rende non valido tutto il file (deciso).
- T9-F1: nessun test che un'uscita o un ingresso rifiutati non suonino.
- T11-F2: la cache degli indicatori sul client senza test proprio.
- T12-F2: il WARN una volta per nome di `ClientRules` senza test.
- T13-F1: `ActivityRule.reset()` su una finestra piena senza caso JUnit.
- T15-F2: con la tuta ogni clic destro su un letto è rifiutato, anche accovacciati con un oggetto (deciso); T15 (piano 3c-1, righe 927-928): il piano vecchio tiene il testo vecchio.
- T16-F2: i controlli del passo su un `ServerPlayer` non possono fallire.
- T17-F1: nella barra delle azioni "critico" può coprire "fuori uso" dello stesso colpo; T17-F2: un test vero per costruzione; T17-F3: deciso (i test impostano la modalità).
- T19-F2: un numero sbagliato nel testo di un commit vecchio.
- T20-F1: la lettura fidata del client non scarta una cella malformata.
- T22-R1-B1: la riga dell'energia sotto una barra del boss.
- T23-F1, T23-F2, T23-F3, T23-F4: stile e rami senza test di `/exoframe debug energy`.
- Raccomandazione non presa: un test che `init()` scriva un WARN per ogni problema del file di config.
