# Checklist in gioco: milestone 3, parte 3b (entrare e uscire)

Per Jarvis, sul PC Windows di Luigi, con i quattro jar di produzione di questa parte. Non serve aver seguito il lavoro: qui sotto c'è tutto ciò che occorre sapere. I passi sono numerati; accanto a ciascuno c'è ciò che deve succedere. Annota l'esito di ogni passo (OK / non OK, con uno screenshot quando non è OK).

## Cosa c'è da vedere

Dalla parte 3a la tuta si poteva solo "indossare" con un comando di prova. Da questa parte il telaio esiste nel mondo:

- **Piazzarlo.** L'oggetto **Telaio** (scheda creativa "Redstone Exoframes"), usato con il clic destro su un blocco, mette in piedi sul blocco un telaio vuoto, alto circa tre blocchi, rivolto verso chi lo piazza. Chi lo piazza ne è il proprietario.
- **Armarlo a mano**, senza schermate: con il clic destro sul telaio tenendo in mano un pezzo Rottame (elmo, torso, braccio, gamba) il pezzo si monta; braccia e gambe vanno sul lato del telaio che si clicca (il suo lato, non quello di chi guarda: il braccio destro del telaio è a sinistra per chi lo guarda di fronte). Con una **Cella di redstone** carica in mano la cella si inserisce (non si vede da fuori).
- **Smontarlo:** accovacciati (Shift) a mano vuota, clic destro: prima esce l'armatura che il telaio custodisce (vedi sotto), poi il pezzo della parte cliccata (gambe in basso, busto e braccia in mezzo, testa in alto) o il più vicino della stessa altezza, poi la cella; un telaio del tutto vuoto torna oggetto.
- **Entrare:** a mano vuota, in piedi, clic destro. Serve una cella carica. Il giocatore va nel punto del telaio, girato come il telaio, e indossa la tuta: diventa grande come lei (vede da circa 2,7 blocchi d'altezza, non passa da una porta alta due). L'armatura che aveva addosso entra nel telaio; nei quattro slot dell'armatura restano quattro segnaposto (gli "attacchi del telaio") che non si possono togliere.
- **Uscire:** tenere premuto **V** per un secondo (sulla barra sopra la barra veloce compare "Tieni premuto per uscire dal telaio… N%"). Il telaio resta in piedi dove si è, l'armatura torna negli slot.
- **Morire dentro:** il telaio resta nel mondo dove si è morti, con dentro l'armatura che si aveva prima di entrare; il proprietario, rientrando, la riprende. Niente segnaposto a terra.
- **Blocco e comandi:** un telaio è di chi lo ha piazzato; gli altri non possono toccarlo (né entrare, né montare, né smontare) finché il proprietario non lo sblocca con `/exoframe unlock` guardandolo (o indossandolo); `/exoframe lock` lo blocca di nuovo. `/exoframe remove` (solo operatori) toglie il telaio che si guarda e lascia a terra quello che conteneva.
- **Nessun suono** in questa parte: arrivano con la prossima.

Messaggi sulla barra delle azioni (in italiano; tra parentesi il testo inglese):

| Quando | Messaggio |
|---|---|
| il telaio lì non ci sta, o per entrare non c'è spazio | Qui non c'è abbastanza spazio per il telaio (There is not enough room for the exoframe here) |
| il telaio è bloccato e non è tuo | Questo telaio è bloccato dal suo proprietario (This exoframe is locked by its owner) |
| quello slot ha già un pezzo, o c'è già una cella | Quello slot è già occupato (That slot is already taken) |
| si prova a entrare senza cella carica | Al telaio serve una cella carica (The exoframe needs a charged cell) |
| si prova a entrare in un secondo telaio | Sei già dentro un telaio (You are already inside an exoframe) |
| un altro giocatore prova a entrare in un telaio che custodisce l'armatura del proprietario | Questo telaio contiene ancora un'armatura: prima tirala fuori (accovacciato e clic destro) |

## Già verificato nella sessione cloud (14 novembre 2026)

Con `tools/client/m3b_shots.sh`, sul client di **sviluppo** di ogni variante (non sui jar di produzione), con clic del mouse e tasti veri: il giocatore, in sopravvivenza con elmo e corazza di ferro, piazza il telaio con l'oggetto, monta elmo, braccio destro e cella, entra, esce tenendo V, rientra, si sposta, muore con `/kill`, rientra nel telaio lasciato dalla morte e ne riesce. Dopo ogni passo uno scatto, e fra un passo e l'altro il server controlla lo stato (dove sta il telaio, quanti ce ne sono, i segnaposto negli slot, l'armatura restituita, niente a terra). Tutte e quattro le esecuzioni sono finite senza errori, e ogni scatto è stato guardato:

| Controllo | 1.21.1 NeoForge | 1.21.1 Fabric | 1.20.1 Fabric | 1.20.1 Forge |
|---|---|---|---|---|
| Telaio piazzato con l'oggetto, rivolto verso il giocatore; l'oggetto lascia la mano | sì | sì | sì | sì |
| Elmo, braccio e cella montati con il clic; il braccio sul lato destro del telaio (a sinistra per chi guarda) | sì | sì | sì | sì |
| Ingresso a mano vuota: subito la tuta grande, mai il giocatore alla misura normale, nessun salto indietro | sì | sì | sì | sì |
| Dentro: segnaposto nei quattro slot, armatura di ferro nel telaio, entità del telaio sparita | sì | sì | sì | sì |
| V tenuto: avanzamento sulla barra delle azioni (60-65 % a 0,6 s), poi uscita | sì | sì | sì | sì |
| Dopo l'uscita: telaio in piedi dov'era il giocatore, armatura di ferro di nuovo negli slot, nessun segnaposto | sì | sì | sì | sì |
| Dopo la morte: un solo telaio, dove il giocatore è morto; niente a terra, inventario vuoto | sì | sì | sì | sì |
| Rientro del proprietario: l'armatura rimasta nel telaio torna nell'inventario; uscita di nuovo con V | sì | sì | sì | sì |
| Log: avvisi del mod o errori dei mixin | nessuno | nessuno | nessuno | nessuno |

Subito dopo il clic d'ingresso la tuta si vede per un istante girata di lato: il giocatore viene girato come il telaio (che guardava verso di lui) e il corpo segue la testa in qualche fotogramma, come per ogni giocatore girato di colpo. Non è un lampo della misura normale. Lo si vede in `m3b-screenshot/1.21.1-neoforge-entrato-subito.png`.

Gli scatti di riferimento sono in `m3b-screenshot/`, uno per scena, da varianti diverse (sulle quattro varianti gli scatti della stessa scena sono uguali): `1.21.1-neoforge-piazzato.png`, `1.20.1-forge-braccio-destro.png`, `1.21.1-neoforge-entrato-subito.png`, `1.20.1-fabric-dentro-davanti.png`, `1.21.1-fabric-uscita-tasto-tenuto.png`, `1.20.1-forge-telaio-dopo-uscita.png`, `1.21.1-neoforge-telaio-dopo-morte.png`, `1.21.1-fabric-armatura-ritrovata.png`. Sono del client di sviluppo con grafica "Dettagliata", campo visivo 70, distanza di rendering 4, interfaccia visibile e testi in inglese.

Il cloud non prova: il **jar di produzione**; il blocco degli slot **provato a mano** (trascinare, lanciare: lo strumento del cloud clicca solo dove punta il mirino); **un secondo giocatore**; i posti stretti, i veicoli, i portali, il riavvio del server, la morte nel vuoto, la fluidità con molti telai. Sono le sezioni 3-5.

## 0. Preparazione

- [ ] **1.** Procurati i quattro jar di questa parte come nella sezione 0 di `m1-setup.md`: artefatto `redstone-exoframes-jars` dell'ultima esecuzione verde della CI sul branch **`m3b-entrare-uscire`** (non su `main` né su `m3-telaio`: la parte 3b sta su quel branch finché non viene unita; dopo l'unione gli stessi jar vengono da `main`), oppure build locale con `collectJar` da quel branch. Sono `redstone-exoframes-fabric-0.1.0+mc1.20.1.jar`, `redstone-exoframes-fabric-0.1.0+mc1.21.1.jar`, `redstone-exoframes-forge-0.1.0+mc1.20.1.jar`, `redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar`. Controllali con `bash tools/check_jars.sh <cartella>`: deve stampare `OK jars`.
- [ ] **2.** Usa le quattro istanze client e i quattro server dedicati della milestone 1 (tabella dei profili in `m1-setup.md`, sezione 2). Sostituisci nella cartella `mods` il jar vecchio con quello nuovo; nessun altro mod oltre alle dipendenze della tabella (loader API e GeckoLib).
- [ ] **3.** Tieni aperta la cartella `docs/test-in-gioco/m3b-screenshot/` del repository (scatti di riferimento, citati sotto).
- [ ] **4.** Screenshot con F2 (finiscono in `<istanza>\screenshots\`); F1 nasconde l'interfaccia, F5 cambia la telecamera (una volta: da dietro; due volte: davanti; tre: di nuovo in prima persona).

Ripeti le sezioni 1-4 per ognuna delle quattro varianti, ciascuna nella propria istanza; la sezione 5 (due giocatori, server) basta su una variante 1.20.1 e una 1.21.1, e le sezioni 6-7 una volta.

## 1. Avvio dal jar di produzione

- [ ] **5.** Avvia il client fino al menu principale: nessun crash, nessuna schermata d'errore del loader.
- [ ] **6.** In Opzioni → Controlli → Comandi (Key Binds) c'è la categoria "Redstone Exoframes" con "Esci dal telaio" sul tasto **V**.
- [ ] **7.** Crea un mondo nuovo: **sopravvivenza, superpiatto, trucchi attivi**. Entra. In `logs\latest.log` nessuna riga `ERROR` o `WARN` che nomina `redstone_exoframes`, `Mixin`, `InvalidInjectionException` o `MixinApplyError`. Sulle due varianti Fabric c'è una riga `No data fixer registered for exoframe`: è nota e innocua.

Se il client si chiude: riporta il `crash-report` per intero e le ultime cento righe di `latest.log`, e passa alla variante successiva.

## 2. La sequenza del cloud, a mano

Stai su terreno piano, in sopravvivenza. Prendi gli oggetti dalla scheda creativa passando un momento in creativa (`/gamemode creative`, poi `/gamemode survival`), o con `/give @s redstone_exoframes:exoframe` e simili. La cella va presa dalla scheda creativa: quella data con `/give` è scarica.

- [ ] **8.** Mettiti addosso un elmo e una corazza di ferro. Con l'oggetto **Telaio** in mano, clic destro sull'erba due blocchi davanti a te. Il telaio nudo compare in piedi su quel blocco, rivolto verso di te, alto circa tre blocchi; l'oggetto lascia la mano. Riferimento: `m3b-screenshot/1.21.1-neoforge-piazzato.png`.
- [ ] **9.** Clic destro sul telaio con un oggetto che non c'entra (un blocco di terra): non succede nulla al telaio.
- [ ] **10.** Elmo Rottame in mano, clic destro sul telaio: l'elmo si monta sulla testa del telaio. Riprova con un secondo elmo: "Quello slot è già occupato", l'elmo resta in mano.
- [ ] **11.** Braccio Rottame in mano, clic destro sul **lato sinistro del telaio come lo vedi tu** (che è il suo lato destro), all'altezza delle braccia: il braccio compare da quel lato. Riferimento: `m3b-screenshot/1.20.1-forge-braccio-destro.png`. Con un secondo braccio clicca lo stesso lato: va sull'altro braccio, che è libero.
- [ ] **12.** Prova a entrare (mano vuota, clic destro) prima della cella: "Al telaio serve una cella carica", non succede altro.
- [ ] **13.** Cella di redstone (dalla scheda creativa) in mano, clic destro: la cella entra (da fuori non si vede) e lascia la mano.
- [ ] **14.** F5 una volta (da dietro). Mano vuota, clic destro sul telaio. Subito la tuta grande, girata come il telaio (la visuale si gira: guardi dalla parte in cui guardava il telaio); per un istante la tuta può vedersi di lato mentre il corpo finisce di girarsi, mai il giocatore alla misura normale, e nessun salto indietro alla posizione di prima. Riferimento: `m3b-screenshot/1.21.1-neoforge-entrato-subito.png`. Annota se ti sembra fastidioso.
- [ ] **15.** F5 ancora (davanti): il pilota dentro la tuta, con i pezzi montati (elmo, un braccio). Riferimento: `m3b-screenshot/1.20.1-fabric-dentro-davanti.png`. L'armatura di ferro non si vede più e la barra dell'armatura sopra la salute è sparita. Nessun suono di armatura entrando.
- [ ] **16.** Tieni premuto **V**: sulla barra delle azioni "Tieni premuto per uscire dal telaio… N%" che sale; lascialo prima di un secondo: il conto si azzera e non esci. Tienilo di nuovo per più di un secondo: esci. Riferimento: `m3b-screenshot/1.21.1-fabric-uscita-tasto-tenuto.png`.
- [ ] **17.** Dopo l'uscita: il telaio è in piedi dov'eri, con i pezzi montati; tu hai di nuovo la tua misura, l'elmo e la corazza di ferro negli slot e la barra dell'armatura. Allontanati di qualche passo e guarda: riferimento `m3b-screenshot/1.20.1-forge-telaio-dopo-uscita.png`.
- [ ] **18.** Rientra (mano vuota, clic destro), cammina qualche blocco più in là dentro la tuta, poi `/kill`. Rinasci. Torna dove sei morto: il telaio è lì, in piedi, uno solo; a terra non c'è nulla (né segnaposto né armatura di ferro). Riferimento: `m3b-screenshot/1.21.1-neoforge-telaio-dopo-morte.png`.
- [ ] **19.** Mano vuota, clic destro sul telaio della morte: entri, e l'elmo e la corazza di ferro che avevi prima di morire sono nel tuo inventario (non addosso). Esci con V e apri l'inventario: riferimento `m3b-screenshot/1.21.1-fabric-armatura-ritrovata.png`.
- [ ] **20.** Smonta tutto, accovacciato a mano vuota: clic in alto (elmo), all'altezza delle braccia sul lato del braccio (braccio), poi un clic qualunque (la cella), poi un altro clic: il telaio vuoto torna oggetto Telaio nella tua mano o nell'inventario. Ogni pezzo arriva nell'inventario; con l'inventario pieno cade ai tuoi piedi, fermo, e si raccoglie dopo un paio di secondi.
- [ ] **21.** Piazza un telaio contro un muro o sotto un soffitto basso (meno di tre blocchi): "Qui non c'è abbastanza spazio per il telaio", l'oggetto resta in mano. Entra camminando nel punto di un telaio piazzato (il telaio non blocca il passo) ed esegui `/exoframe debug place nudo`, che piazza ai tuoi piedi: rifiutato con lo stesso messaggio, perché lì c'è già un telaio.

## 3. Il blocco degli slot, a mano

Entra in un telaio con una cella carica (passi 8-14). Fai ogni prova in sopravvivenza e poi in creativa (`/gamemode creative`), e annota per ognuna se il segnaposto **lampeggia** (sparisce e ricompare per un istante: è noto e accettato per ora) o fa altro.

- [ ] **22.** Apri l'inventario (E). Nei quattro slot dell'armatura ci sono i segnaposto ("Attacco elmo del telaio" e gli altri, nel suggerimento al passaggio del mouse). Prendine uno con un clic e lascialo in un altro slot dell'inventario: torna da solo nel suo slot, nessuna copia resta nell'inventario né attaccata al puntatore.
- [ ] **23.** Prendine uno e cliccalo fuori dalla finestra dell'inventario (lancio): a terra non compare nulla, o al massimo per un istante un oggetto senza nome (su Forge e NeoForge il gioco lo crea comunque e la mod lo svuota subito); il segnaposto torna nel suo slot. Prova anche con il tasto Q sopra lo slot.
- [ ] **24.** Apri una cassa: Shift+clic su un segnaposto dello slot armatura (o trascinalo nella cassa). Nella cassa non resta nulla; il segnaposto torna nel suo slot.
- [ ] **25.** Metti un segnaposto nella griglia di fabbricazione 2×2 dell'inventario, o nella mano secondaria (tasto F sopra lo slot): sparisce da lì e torna nello slot armatura.
- [ ] **26.** In creativa, nell'inventario creativo: prova a buttare un segnaposto nello slot di distruzione (la X in basso a destra) e a prenderne copie con il clic centrale. Lo slot armatura resta con il segnaposto, nessuna copia resta da nessuna parte.
- [ ] **27.** Con `/give @s redstone_exoframes:exoframe_helmet` (dentro o fuori dal telaio): la copia data sparisce subito dall'inventario.
- [ ] **28.** Prova a mettere un'armatura vera nello slot dell'elmo mentre sei dentro: torna nell'inventario (o a terra se è pieno), il segnaposto resta.

## 4. Ciò che il cloud non vede

- [ ] **29.** **Porte, scale, soffitti.** Con la tuta: passi sotto un soffitto alto tre blocchi; **non** passi da una porta normale alta due (nemmeno accovacciato); passi da un vano largo uno e alto tre, anche con una porta aperta nella parte bassa; sali una scala a gradini e una scala a pioli in un pozzo 1×1. In una stanza alta due il gioco ti mette da solo a carponi. Annota ogni caso in cui la tuta resta incastrata, attraversa un blocco o la visuale finisce dentro un blocco.
- [ ] **30.** **Carrelli, barche, letti.** Con la tuta sali su un carrello in un tunnel alto due e su una barca; scendi; prova a dormire in un letto in una stanza alta tre e in una alta due. Non c'è un risultato atteso (domanda 4 sotto): annota con uno screenshot cosa succede (testa nel soffitto, risveglio incastrato, discesa nel blocco).
- [ ] **31.** **Portali.** Piazza un telaio, poi costruiscigli attorno una cornice di ossidiana 4×5 (interno largo due e alto tre, con il telaio dentro) e accendila: il telaio sta nel portale del Nether. Piazza un secondo telaio, guarda con F3 le coordinate x y z del blocco su cui poggia e metti un blocco di portale dell'End dove stanno i suoi piedi: `/setblock x y+1 z minecraft:end_portal` (con i numeri al posto delle lettere). Aspetta almeno dieci secondi: i due telai restano dove sono, nessuno viene portato nell'altra dimensione.
- [ ] **32.** **Molti telai.** Con `/exoframe debug place rottame` piazza una ventina di telai armati qua e là, tutti in vista. Guarda il contatore F3 (fps) per un minuto e confrontalo con lo stesso posto senza telai (`/kill @e[type=redstone_exoframes:exoframe]` non basta: i telai tornano dal registro dopo pochi secondi; usa `/exoframe remove` guardandoli uno per uno, o un mondo nuovo). Annota i due valori: un calo oltre un terzo è da riportare.
- [ ] **33.** **Morte nel vuoto.** In sopravvivenza, dentro un telaio, fermo a terra per qualche secondo; poi `/tp @s ~ -200 ~`. Muori nel vuoto. Il telaio resta nell'ultimo punto in cui eri a terra (quello da cui ti sei teletrasportato), con dentro l'armatura che avevi entrando.
- [ ] **34.** **keepInventory.** `/gamerule keepInventory true`, entra in un telaio con un'armatura addosso, `/kill`. Rinato: nel tuo inventario non c'è nessun segnaposto, il telaio è dove sei morto con dentro l'armatura; rientrando la riprendi. Rimetti `/gamerule keepInventory false`.

## 5. Due giocatori e il server

Sul server dedicato della variante (come in `m1-setup.md`), con due client (due account o due istanze). Il primo giocatore (A) è operatore (`op <nome>` dalla console), il secondo (B) no.

- [ ] **35.** **Ingombro e targhetta.** A entra in un telaio. B lo guarda da davanti, di lato e da dietro, fermo e mentre cammina: vede la tuta grande con il pilota dentro e la **targhetta col nome di A sopra la testa della tuta** (a circa 3,5 blocchi da terra). B vede A entrare e uscire (il telaio piazzato sparisce quando A entra, ricompare quando esce).
- [ ] **36.** **Telaio bloccato.** A piazza e arma un telaio. B prova a montare un pezzo, a smontare (accovacciato) e a entrare: ogni volta "Questo telaio è bloccato dal suo proprietario", nulla cambia. B prova `/exoframe unlock` guardandolo: "Questo telaio non è tuo".
- [ ] **37.** **Sbloccato.** A, guardando il telaio, `/exoframe unlock`: "Telaio sbloccato: chiunque può usarlo". Ora B può montare, smontare ed entrare. A, guardandolo di nuovo (o indossandolo), `/exoframe lock`: B torna a essere rifiutato.
- [ ] **38.** **L'armatura di A in un telaio sbloccato.** A, con un'armatura addosso, entra in un telaio, poi `/kill`: il telaio resta con l'armatura di A dentro. A lo sblocca (`/exoframe unlock` guardandolo). B, a mano vuota, prova a entrare: "Questo telaio contiene ancora un'armatura: prima tirala fuori (accovacciato e clic destro)", nulla cade a terra. B si accovaccia e fa clic destro: l'armatura di A va nell'inventario di **B**. Ora B può entrare. Vedi la domanda 1.
- [ ] **39.** **Rimozione.** A, operatore, guardando un telaio armato: `/exoframe remove`. Il telaio sparisce per sempre (non torna dopo qualche secondo) e pezzi e cella sono a terra ai suoi piedi. B, che non è operatore, non ha il comando `remove` (il gioco non lo suggerisce e lo dice sconosciuto).
- [ ] **40.** **Riavvio.** Lascia un telaio piazzato e armato, e A dentro un altro telaio con un'armatura stivata. `stop` dalla console del server; riavvia; A e B rientrano. Il telaio piazzato è dov'era, con gli stessi pezzi; A è ancora dentro la tuta, con i segnaposto negli slot; con V esce e riprende la sua armatura. Nessun oggetto in più o in meno.

## 6. Cosa riportare

Per ognuna delle quattro varianti:

- [ ] esito (OK / non OK) di ogni passo numerato fatto su quella variante;
- [ ] per la sezione 1, se il client non parte: crash report e ultime cento righe di `latest.log`;
- [ ] per ogni passo non OK: uno screenshot e una riga che dice cosa si vede di diverso da ciò che è scritto (o dal riferimento);
- [ ] gli screenshot chiesti anche quando va tutto bene: passo 14 (ingresso), 23 (lancio di un segnaposto), 30 (carrello, barca, letto), 35 (la tuta e la targhetta viste da B);
- [ ] per la sezione 3, per ogni prova: lampeggio sì o no;
- [ ] gli fps del passo 32, con e senza telai;
- [ ] ogni riga `ERROR` o `WARN` del log (client e server) che nomina il mod, GeckoLib o un mixin, con qualche riga di contesto;
- [ ] versioni realmente usate (loader, Java, dipendenze) se diverse dalla tabella di `m1-setup.md`, e i passi saltati con il motivo;
- [ ] le risposte di Luigi alle domande qui sotto.

Riepilogo finale: una tabella con le quattro varianti per righe e le sezioni 1-5 per colonne, con OK o non OK, più i dettagli dei problemi.

## 7. Domande per Luigi

Nessuna blocca il lavoro: per ognuna c'è scritto cosa fa la mod oggi, e se Luigi non risponde resta così.

1. **Un amico in un telaio sbloccato che custodisce ancora l'armatura del proprietario** (passo 38). Oggi l'amico non può entrare finché l'armatura è dentro, ma può tirarla fuori lui stesso, accovacciato, e se la ritrova nell'inventario. Va bene così, o l'armatura deve restare chiusa nel telaio finché non torna il suo proprietario? (Servirebbe ricordare di chi è ogni pezzo d'armatura custodito.)
2. **L'ombra.** Sotto la tuta indossata e sotto il telaio piazzato l'ombra è quella di un giocatore normale, piccola e in gran parte coperta dai piedi della tuta. Ne vuoi una più grande per tutte e due?
3. **I suoni.** In questa parte non ce n'è nessuno: piazzare, montare, entrare e uscire sono muti, e anche gli slot dell'armatura cambiano senza il suono dell'equipaggiamento. Nella prossima parte è previsto un suono del gioco base per ciascuna di queste azioni. Va bene?
4. **Le domande aperte della grandezza reale** (design §8, domanda 2), che questa parte tocca:
   - (a) a carponi l'ingombro della tuta è 0,8 × 0,99, quindi una tuta che non ha altro posto striscia anche in un cunicolo alto uno; con un'altezza oltre 1 resterebbe incastrata invece di strisciare;
   - (c) carrelli, barche, cavalcature e letti con la tuta: vietarli o accettare i casi limite (testa nel soffitto dei tunnel alti due, risveglio in uno spazio basso)? Oggi sono permessi (passo 30);
   - (d) passo di un blocco, portata e distanza della telecamera adeguati a una tuta alta tre blocchi: economici su 1.21.1 e Forge, un po' meno su Fabric 1.20.1; proposta: il passo sì, nella parte 3c con gli altri attributi. Oggi sono quelli di un giocatore normale: un gradino di un blocco ferma la tuta.
5. **Uscire a mezz'aria sopra il vuoto.** Se si esce dal telaio con V mentre si cade nel vuoto, il telaio va nell'ultimo punto sicuro, ma l'armatura torna al giocatore, che sta per cadere e perderla. La teniamo invece nel telaio in quel caso?
