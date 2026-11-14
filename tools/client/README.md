# Client senza schermo

Scatti in terza persona dal client vero di Minecraft, senza schermo, per vedere la tuta come la vede un giocatore. Il client di sviluppo della variante gira sotto Xvfb con l'OpenGL software di Mesa (llvmpipe), entra in un server dedicato locale in modalità offline e lo script comanda il server via RCON e la telecamera con tasti finti (xdotool).

**Solo Linux.** È per la sessione cloud e per macchine simili alla CI, non per il PC Windows: lì vale la checklist in `docs/test-in-gioco/`. Le due cose non si sostituiscono: questo strumento usa l'ambiente di sviluppo, la checklist i jar di produzione.

## Cosa serve

- Xvfb, xdotool, ImageMagick (`import`, `identify`, `convert`), `glxinfo` e il driver software di Mesa. Su Debian/Ubuntu: `apt-get install -y xvfb xdotool imagemagick mesa-utils libgl1-mesa-dri`.
- JDK 21 (Gradle), `python3` (client RCON, solo libreria standard), `flock` (util-linux).
- Memoria: per un'esecuzione circa 3 GB per il gioco (client 2 GB, server 1 GB) più i due demoni di Gradle che lo avviano, uno per il server e uno per il client, fino a 3 GB ciascuno (`-Xmx3G` in `gradle.properties`), che restano accesi a riposo dopo la fine: fino a circa 9 GB in tutto. Due esecuzioni insieme (due varianti, o un secondo clone) arrivano a circa 18 GB, più della macchina cloud (15 GB): una alla volta, oppure `./gradlew --stop` tra l'una e l'altra se si fa altro.
- Rete verso i Maven del progetto e verso i server delle risorse di Minecraft, solo la prima volta.

Lo script controlla all'avvio che i programmi ci siano, che Xvfb parta e che sotto Xvfb ci sia OpenGL core 3.2 o più; se manca qualcosa lo nomina ed esce con 2. Il numero dello schermo virtuale lo sceglie Xvfb (`-displayfd`): due esecuzioni partite insieme non finiscono sullo stesso schermo.

## EULA

Il server di prova parte solo con `EXO_ACCEPT_EULA=true`. Impostarla significa accettare l'[EULA di Minecraft](https://aka.ms/MinecraftEULA) per quel server locale: lo script scrive allora `eula=true` nella propria cartella del server, e in nessun altro posto. Senza la variabile si ferma e lo spiega.

## Uso

Dalla radice del repository:

```
EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <variante> <cartella> ["comando" | "@passo" ...]
EXO_ACCEPT_EULA=true bash tools/client/m3a_shots.sh <variante> <cartella>
EXO_ACCEPT_EULA=true bash tools/client/m3b_shots.sh <variante> <cartella>
```

Varianti: `1.20.1-forge`, `1.20.1-fabric`, `1.21.1-fabric`, `1.21.1-neoforge`.

**`client_shot.sh`** fa un'esecuzione: server e client della variante, giocatore dentro (in creativa, mondo piatto, mezzogiorno, sereno, rivolto a sud in 0,5 -60 0,5), un passo avanti e ritorno per allineare il corpo alla testa (il client crea il giocatore con il corpo girato a caso, e da fermo il corpo resta indietro fino a 50 gradi: senza il passo, su 1.20.1 metà degli scatti "davanti" venivano di tre quarti), poi i comandi e i passi del client (sotto), in ordine; i comandi vanno via RCON, senza la barra iniziale; il giocatore si chiama `Exo` salvo `EXO_SHOT_USER`. Esempio:

```
EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh 1.21.1-fabric /tmp/scatti/rottame "execute as Exo run exoframe debug wear rottame 2"
```

Nella cartella lascia:

- `back.png` da dietro, `front.png` davanti, `quarter.png` da dietro a tre quarti, con l'interfaccia nascosta (F1);
- `<nome>.png` per ogni passo `@scatto <nome>` (e `@tieni` con uno scatto);
- `gradle-server.log`, `gradle-client.log`: uscita di Gradle e del gioco; `server.log`, `client.log`: i `latest.log`;
- `rcon.log`: comandi e risposte; `xvfb.log`: uscita di Xvfb;
- `log-check.txt`: avvisi ed errori del mod o dei mixin nei due log (righe WARN/ERROR che nominano il mod o i mixin, tracce che passano da `com.toxineren`, errori di applicazione dei mixin); vuoto se non ce ne sono. Non ferma lo script: lo segnala.

All'inizio lo script cancella dalla cartella solo questi file, per nome (compresi gli scatti dei passi); il resto della cartella non lo tocca. `server.log` e `client.log` li copia solo se in questa esecuzione server e client sono partiti.

Ogni comando dell'utente passa da `execute store success`: se il server lo rifiuta (sintassi, bersaglio inesistente, colore non valido di `/exoframe`...) o non risponde entro 15 s lo script si ferma. Serve perché un errore dentro `execute as ... run` non arriva nella risposta RCON. Conta come rifiutato anche un comando che il server esegue ma con risultato 0, per esempio un `fill` che non cambia nessun blocco o un `execute as` senza bersagli.

### Passi del client

Fra i comandi possono stare passi che cominciano con `@`: li esegue il client, con mouse e tastiera finti (xdotool), nello stesso ordine dei comandi.

| Passo | Cosa fa |
|---|---|
| `@click destro`, `@click sinistro` | un clic del mouse dove punta il mirino (il clic destro usa l'oggetto in mano o tocca l'entità o il blocco puntati) |
| `@tieni <tasto> <secondi>` | tiene premuto un tasto per quei secondi; `<tasto>` è un nome di xdotool (`v`, `w`, `e`, `F5`, `Escape`, `space`...) |
| `@tieni <tasto> <secondi> <nome> <dopo>` | come sopra, e salva `<nome>.png` `<dopo>` secondi dopo la pressione, a tasto ancora premuto (`<dopo>` minore di `<secondi>`): per esempio l'avanzamento di un tasto da tenere |
| `@scatto <nome>` | salva `<nome>.png` subito, senza attese, con l'interfaccia com'è in quel momento (visibile, salvo un F1 dei passi) |
| `@attendi <secondi>` | aspetta |

I secondi possono avere decimali (`0.5`). Il nome di uno scatto ha lettere, cifre, `_` e `-`, non si ripete e non può essere `back`, `front` o `quarter`. Dove guarda il giocatore e cosa tiene in mano si decide con comandi del server fra un passo e l'altro, per esempio `tp Exo 0.5 -60 0.5 0 39` (yaw 0 = sud, pitch positivo = verso il basso; gli occhi sono a 1,62 dai piedi, 2,67 nella tuta) e `item replace entity Exo weapon.mainhand with ...`. La telecamera si cambia con `@tieni F5 0.2`. Esempio:

```
EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh 1.21.1-fabric /tmp/scatti/prova "@attendi 3" "@scatto mirino" "@click destro" "@tieni F5 0.2" "@attendi 2" "@scatto terza-persona" "@tieni F5 0.2" "@tieni F5 0.2"
```

Tutti i passi si controllano prima di avviare qualunque cosa: un passo sconosciuto o scritto male (argomenti in più o in meno, secondi non numerici o zero, nome di scatto non valido o ripetuto) ferma lo script con uscita 2 e una riga `ERRORE:` che lo nomina. I tasti dei passi `@tieni` si provano appena parte lo schermo virtuale, prima di server e client, perché xdotool con un nome sconosciuto avvisa ma esce con 0: un tasto sconosciuto dà uscita 2. Un passo che non si riesce a eseguire (client chiuso, giocatore uscito dal server, xdotool che segnala un errore, scatto quasi tutto di un colore) ferma lo script con uscita 1. I tre scatti finali partono dalla prima persona con l'interfaccia visibile: chi con i passi lascia la telecamera in terza persona (F5) o l'interfaccia nascosta (F1) li cambia, quindi i passi dovrebbero rimetterle com'erano.

**`m3a_shots.sh`** fa l'insieme standard della parte 3a, una esecuzione e una sottocartella per scatto: `01-nudo`, `02-rottame-s0` … `05-rottame-s3`, `06-colori` (`3A6EA5` e `D9D9D9`), `07-mani` (piccone di ferro nella mano principale, scudo nell'altra), `08-sdraiato` (telaio nudo, posa strisciante forzata da un blocco di vetro sopra la testa; qui `front.png` non serve, perché la telecamera davanti finisce contro la testa: contano `back.png` e `quarter.png`). Negli scatti 01 e 02 c'è una colonna di tre blocchi di lana bianca a tre blocchi dal giocatore, verso ovest, come metro per l'altezza. Alla fine monta `stati-fronte.png` (le quattro viste frontali degli stati affiancate) e stampa un riepilogo. Una esecuzione per scatto invece di una lunga: ogni scatto parte dallo stesso stato e un errore resta nel suo scatto; costa un minuto in più a scatto.

**`m3b_shots.sh`** fa la sequenza della parte 3b in una sola esecuzione, con clic e tasti veri: il giocatore in sopravvivenza, con elmo e corazza di ferro, piazza il telaio con l'oggetto (clic destro sull'erba), monta elmo, braccio destro (cliccando il lato destro del telaio) e cella carica con l'oggetto in mano, entra a mano vuota, esce tenendo `V`, rientra, si sposta nella tuta, muore (`kill`) e, rinato, rientra nel telaio lasciato dalla morte e ne riesce. Dopo ogni passo uno scatto (`01-piazzato` … `15-armatura-ritrovata`, elencati in testa allo script); fra i passi comandi `execute if`/`unless` controllano dove sta l'entità telaio e quante ce ne sono, i segnaposto negli slot armatura solo mentre il giocatore è dentro, l'armatura restituita, niente a terra dopo la morte; `data get entity` sul telaio scrive il suo stato in `rcon.log`. Un controllo che non torna ferma l'esecuzione con uscita 1. Alla fine i tre scatti di `client_shot.sh`, con il giocatore otto blocchi davanti al telaio.

Variabili d'ambiente:

| Variabile | Default | Significato |
|---|---|---|
| `EXO_ACCEPT_EULA` | (nessuno) | `true` per accettare l'EULA del server di prova; obbligatoria |
| `EXO_SHOT_PORT` | 25601 | porta del server; RCON usa la porta + 10 |
| `EXO_SHOT_W`, `EXO_SHOT_H` | 960, 540 | misura della finestra e dello schermo virtuale |
| `EXO_SHOT_USER` | Exo | nome del giocatore offline |
| `EXO_SHOT_SETTLE` | 8 | secondi di attesa dopo i comandi, prima degli scatti |

Lo script lavora sul checkout in cui si trova (compila il codice com'è, anche le modifiche non committate) in cartelle di esecuzione proprie, `versions/<variante>/run-shot-client` e `run-shot-server`, ignorate da git; non tocca `run/` e `run-gametest/`. Il mondo del server si ricrea a ogni esecuzione. Server e RCON ascoltano solo su 127.0.0.1 e il client si collega a quell'indirizzo; la password RCON è nuova a ogni esecuzione, non si stampa, passa a `rcon.py` nella variabile d'ambiente `EXO_RCON_PASSWORD` (non sulla riga di comando) e resta solo in `server.properties` della cartella del server, leggibile solo dal proprietario. Una sola esecuzione per variante alla volta (c'è un lucchetto); varianti diverse in parallelo vogliono porte diverse, ma con quattro processori il rendering software rallenta molto.

## Errori

Uscita 0: scatti salvati. Uscita 1: qualcosa si è fermato durante l'esecuzione, con una riga `ERRORE:` su stderr che dice cosa (build fallita, server o client che non partono, giocatore che non entra o viene espulso, comando rifiutato o senza risposta, passo del client non riuscito, scatto quasi tutto di un colore). Uscita 2: uso o ambiente sbagliati (variante sconosciuta, passo sconosciuto o scritto male, tasto sconosciuto, programmi mancanti, EULA non accettata, porta occupata, un'altra esecuzione sulla stessa variante, Xvfb che non parte, OpenGL core 3.2 assente); `m3a_shots.sh` allora non prova gli scatti successivi. Uscita 130: interrotto con Ctrl-C; 143: terminato con TERM. In ogni caso lo script ferma il server con `stop`, manda TERM ai suoi due processi Gradle (che annullano la build nel demone, così un server o un client non ancora partiti non partono più) e alle JVM del gioco di quell'esecuzione, dopo 30 s le uccide, chiude Xvfb e copia i log. La chiusura dura al massimo circa 35 s, e un secondo Ctrl-C non la interrompe.

Uno scatto non si salva se il client non è più vivo o il giocatore ha lasciato il server: uno scatto del menu principale non passa mai per buono.

**OK non vuol dire che l'immagine sia giusta.** Lo script controlla che il client sia vivo, che il giocatore sia nel server e che lo scatto abbia almeno 64 colori; non controlla che i tasti (F1, F5, il passo avanti) abbiano avuto effetto né che il terreno sia già disegnato. Un tasto perso darebbe, per esempio, un `front.png` in prima persona con uscita 0. Ogni scatto va guardato.

## Quanto dura

Misurato nella sessione cloud (4 processori, 15 GB), a cache calde: circa un minuto per esecuzione, di cui 15-35 s per il server e 25-50 s perché il client entri. L'insieme standard di `m3a_shots.sh` dura circa 8,5-10 minuti per variante (misurati 503-589 s sulle quattro varianti); la sequenza di `m3b_shots.sh` circa due minuti (misurati 115-132 s). La prima esecuzione in assoluto scarica circa 800 MB di risorse del gioco e compila la variante: qualche minuto in più.

## Cosa non copre

- **Davanti da sdraiati.** Con la posa strisciante la telecamera davanti finisce contro la testa: lo scatto non mostra nulla di utile.
- **Vista laterale vera.** Si gira solo la testa (mouse finto); il corpo la segue in ritardo, fino a 50 gradi, quindi `quarter.png` è una vista a tre quarti più o meno di lato. Per un lato esatto servirebbe un secondo client come telecamera.
- **Audio** (non c'è un dispositivo audio), **fluidità** e animazioni nel tempo (si scattano fotogrammi fermi, a 30 fps in rendering software), **un secondo giocatore** che guarda il primo.
- Il **jar di produzione**: qui gira l'ambiente di sviluppo di Loom. Che i mixin si applichino nel jar finale lo prova solo la checklist sul PC.
- Pose da comandi e tasti diversi da quelli usati qui (accovacciarsi, correre, nuotare): vedi la checklist.
- **Trascinare oggetti nelle schermate**: i passi cliccano dove punta il mirino e non spostano il puntatore, quindi non si può prendere un oggetto da uno slot dell'inventario e lasciarlo fuori (per esempio lanciare un segnaposto). Lo prova la checklist.

## Messaggi innocui nei log

- `UnknownHostException` o errori di connessione verso `sessionserver.mojang.com` e `api.minecraftservices.com`, `Failed to request yggdrasil public key`, `Couldn't look up profile properties`, `Ignoring chat session ... missing Services public key`: account offline di sviluppo.
- `Error starting SoundSystem. Turning off sounds & music`, `Failed to open OpenAL device`, `Missing sound for event`: niente audio.
- `Reference map 'geckolib.refmap.json' for geckolib.mixins.json could not be read. If this is a development environment you can ignore this message`: GeckoLib in sviluppo; `log-check.txt` la esclude.
- Su NeoForge: `Attempted to select a dependency jar for JarJar which was passed in as source: library:mixinextras.neoforge`: ambiente di sviluppo; `log-check.txt` la esclude.
- `Shader rendertype_entity_translucent_emissive could not find sampler named Sampler2`: avviso del gioco base su 1.21.1.
- Sulle due varianti Fabric, in client e server: `[ERROR] (Minecraft) No data fixer registered for exoframe`, il tipo dell'entità telaio senza correttore di dati (vedi `docs/compat.md`, `CompatEntity`); non nomina il mod per intero, quindi `log-check.txt` non la prende.
- Sul server: `No key layers in MapLike[{}]` (impostazioni del mondo piatto lasciate vuote) e gli avvisi `SERVER IS RUNNING IN OFFLINE/INSECURE MODE`.
