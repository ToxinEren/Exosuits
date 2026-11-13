# Client senza schermo

Scatti in terza persona dal client vero di Minecraft, senza schermo, per vedere la tuta come la vede un giocatore. Il client di sviluppo della variante gira sotto Xvfb con l'OpenGL software di Mesa (llvmpipe), entra in un server dedicato locale in modalità offline e lo script comanda il server via RCON e la telecamera con tasti finti (xdotool).

**Solo Linux.** È per la sessione cloud e per macchine simili alla CI, non per il PC Windows: lì vale la checklist in `docs/test-in-gioco/`. Le due cose non si sostituiscono: questo strumento usa l'ambiente di sviluppo, la checklist i jar di produzione.

## Cosa serve

- Xvfb, xdotool, ImageMagick (`import`, `identify`, `convert`), `glxinfo` e il driver software di Mesa. Su Debian/Ubuntu: `apt-get install -y xvfb xdotool imagemagick mesa-utils libgl1-mesa-dri`.
- JDK 21 (Gradle), `python3` (client RCON, solo libreria standard), `flock` (util-linux).
- Circa 3 GB di memoria libera oltre ai demoni di Gradle: client 2 GB, server 1 GB.
- Rete verso i Maven del progetto e verso i server delle risorse di Minecraft, solo la prima volta.

Lo script controlla all'avvio che i programmi ci siano e che sotto Xvfb ci sia OpenGL core 3.2 o più; se manca qualcosa lo nomina ed esce con 2.

## EULA

Il server di prova parte solo con `EXO_ACCEPT_EULA=true`. Impostarla significa accettare l'[EULA di Minecraft](https://aka.ms/MinecraftEULA) per quel server locale: lo script scrive allora `eula=true` nella propria cartella del server, e in nessun altro posto. Senza la variabile si ferma e lo spiega.

## Uso

Dalla radice del repository:

```
EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh <variante> <cartella> ["comando" ...]
EXO_ACCEPT_EULA=true bash tools/client/m3a_shots.sh <variante> <cartella>
```

Varianti: `1.20.1-forge`, `1.20.1-fabric`, `1.21.1-fabric`, `1.21.1-neoforge`.

**`client_shot.sh`** fa un'esecuzione: server e client della variante, giocatore dentro (in creativa, mondo piatto, mezzogiorno, sereno, rivolto a sud), poi i comandi, in ordine, via RCON, senza la barra iniziale; il giocatore si chiama `Exo` salvo `EXO_SHOT_USER`. Esempio:

```
EXO_ACCEPT_EULA=true bash tools/client/client_shot.sh 1.21.1-fabric /tmp/scatti/rottame "execute as Exo run exoframe debug wear rottame 2"
```

Nella cartella lascia:

- `back.png` da dietro, `front.png` davanti, `quarter.png` da dietro a tre quarti, con l'interfaccia nascosta (F1);
- `gradle-server.log`, `gradle-client.log`: uscita di Gradle e del gioco; `server.log`, `client.log`: i `latest.log`;
- `rcon.log`: comandi e risposte;
- `log-check.txt`: avvisi ed errori del mod o dei mixin nei due log (righe WARN/ERROR che nominano il mod o i mixin, tracce che passano da `com.toxineren`, errori di applicazione dei mixin); vuoto se non ce ne sono. Non ferma lo script: lo segnala.

Ogni comando dell'utente passa da `execute store success`: se il server lo rifiuta (sintassi, bersaglio inesistente, colore non valido di `/exoframe`...) lo script si ferma. Serve perché un errore dentro `execute as ... run` non arriva nella risposta RCON.

**`m3a_shots.sh`** fa l'insieme standard della parte 3a, una esecuzione e una sottocartella per scatto: `01-nudo`, `02-rottame-s0` … `05-rottame-s3`, `06-colori` (`3A6EA5` e `D9D9D9`), `07-mani` (piccone di ferro nella mano principale, scudo nell'altra), `08-sdraiato` (telaio nudo, posa strisciante forzata da un blocco di vetro sopra la testa). Negli scatti 01 e 02 c'è una colonna di tre blocchi di lana bianca a tre blocchi dal giocatore, come metro per l'altezza. Alla fine monta `stati-fronte.png` (le quattro viste frontali degli stati affiancate) e stampa un riepilogo. Una esecuzione per scatto invece di una lunga: ogni scatto parte dallo stesso stato e un errore resta nel suo scatto; costa un minuto in più a scatto.

Variabili d'ambiente:

| Variabile | Default | Significato |
|---|---|---|
| `EXO_ACCEPT_EULA` | (nessuno) | `true` per accettare l'EULA del server di prova; obbligatoria |
| `EXO_SHOT_PORT` | 25601 | porta del server; RCON usa la porta + 10 |
| `EXO_SHOT_W`, `EXO_SHOT_H` | 960, 540 | misura della finestra e dello schermo virtuale |
| `EXO_SHOT_USER` | Exo | nome del giocatore offline |
| `EXO_SHOT_SETTLE` | 8 | secondi di attesa dopo i comandi, prima degli scatti |

Lo script lavora sul checkout in cui si trova (compila il codice com'è, anche le modifiche non committate) in cartelle di esecuzione proprie, `versions/<variante>/run-shot-client` e `run-shot-server`, ignorate da git; non tocca `run/` e `run-gametest/`. Il mondo del server si ricrea a ogni esecuzione. Una sola esecuzione per variante alla volta (c'è un lucchetto); varianti diverse in parallelo vogliono porte diverse, ma con quattro processori il rendering software rallenta molto.

## Errori

Uscita 0: scatti salvati. Uscita 1: qualcosa si è fermato durante l'esecuzione, con una riga `ERRORE:` che dice cosa (build fallita, server o client che non partono, giocatore che non entra o viene espulso, comando rifiutato, scatto quasi tutto di un colore). Uscita 2: uso o ambiente sbagliati (variante sconosciuta, programmi mancanti, EULA non accettata, porta occupata, un'altra esecuzione sulla stessa variante). Uscita 130: interrotto. In ogni caso, anche con Ctrl-C, lo script ferma il server con `stop`, chiude il client e Xvfb, e copia i log.

Uno scatto non si salva se il client non è più vivo o il giocatore ha lasciato il server: uno scatto del menu principale non passa mai per buono.

## Quanto dura

Misurato nella sessione cloud (4 processori, 15 GB), a cache calde: circa un minuto per esecuzione, di cui 15-35 s per il server e 25-50 s perché il client entri. L'insieme standard di `m3a_shots.sh` dura circa 9-13 minuti per variante (tempi misurati nel rapporto del Task 10 della parte 3a). La prima esecuzione in assoluto scarica circa 800 MB di risorse del gioco e compila la variante: qualche minuto in più.

## Cosa non copre

- **Vista laterale vera.** Si gira solo la testa (mouse finto); il corpo la segue in ritardo, fino a 50 gradi, quindi `quarter.png` è una vista a tre quarti più o meno di lato. Per un lato esatto servirebbe un secondo client come telecamera.
- **Audio** (non c'è un dispositivo audio), **fluidità** e animazioni nel tempo (si scattano fotogrammi fermi, a 30 fps in rendering software), **un secondo giocatore** che guarda il primo.
- Il **jar di produzione**: qui gira l'ambiente di sviluppo di Loom. Che i mixin si applichino nel jar finale lo prova solo la checklist sul PC.
- Pose da comandi e tasti diversi da quelli usati qui (accovacciarsi, correre, nuotare, inventario): vedi la checklist.

## Messaggi innocui nei log

- `UnknownHostException` o errori di connessione verso `sessionserver.mojang.com` e `api.minecraftservices.com`, `Failed to request yggdrasil public key`, `Couldn't look up profile properties`, `Ignoring chat session ... missing Services public key`: account offline di sviluppo.
- `Error starting SoundSystem. Turning off sounds & music`, `Failed to open OpenAL device`, `Missing sound for event`: niente audio.
- `Reference map 'geckolib.refmap.json' for geckolib.mixins.json could not be read. If this is a development environment you can ignore this message`: GeckoLib in sviluppo; `log-check.txt` la esclude.
- `Shader rendertype_entity_translucent_emissive could not find sampler named Sampler2`: avviso del gioco base su 1.21.1.
- Sul server: `No key layers in MapLike[{}]` (impostazioni del mondo piatto lasciate vuote) e gli avvisi `SERVER IS RUNNING IN OFFLINE/INSECURE MODE`.
