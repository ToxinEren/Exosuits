# Checklist in gioco: milestone 2, seconda metà (strato di compatibilità)

Per Jarvis, sul PC Windows di Luigi, dopo la checklist della milestone 1 (`m1-setup.md`) e con gli stessi quattro jar. Questa milestone non aggiunge nulla di visibile al giocatore: aggiunge il materiale d'armatura segnaposto, il canale di rete per lo stato del telaio e il suo salvataggio sul server. Le prove servono a confermare che questi pezzi non rompono l'avvio, l'ingresso in un mondo e l'uscita.

## Già verificato nella sessione cloud (13 novembre 2026)

Server dedicato avviato dal codice della milestone su tutte e quattro le varianti, con l'EULA accettata nel solo server di prova:

| Variante | Mod caricato | Arriva a "Done" | Arresto con `stop` | Errori o avvisi del mod |
|---|---|---|---|---|
| 1.21.1 NeoForge | sì | sì | pulito | nessuno |
| 1.21.1 Fabric | sì | sì | pulito | nessuno |
| 1.20.1 Fabric | sì | sì | pulito | nessuno |
| 1.20.1 Forge | sì | sì | pulito | nessuno |

Non verificato nel cloud, perché serve un client: l'ingresso di un giocatore, il pacchetto di sincronizzazione, la morte con rinascita, l'uscita e il rientro.

## Da fare sul PC, per ognuno dei quattro jar

Usa l'istanza client e il server dedicato preparati per la milestone 1.

- [ ] **Client, mondo in singolo.** Crea un mondo, entra, resta dentro un minuto. Nel log non devono comparire righe `ERROR` o `WARN` che nominano `redstone_exoframes` o `Redstone Exoframes`.
- [ ] **Morte e rinascita.** Muori (per esempio `/kill`) e rinasci. Nessun errore nel log, il gioco continua normalmente.
- [ ] **Cambio di dimensione.** Entra nel Nether e torna indietro (va bene un portale creato in creativa). Nessun errore.
- [ ] **Uscita e rientro.** Esci al menu principale e rientra nello stesso mondo. Nessun errore.
- [ ] **Server dedicato con client.** Avvia il server dedicato con lo stesso jar, collegati dal client, ripeti morte e rinascita, poi scollegati e ricollegati. Né il log del server né quello del client devono mostrare errori del mod, e il client non deve essere espulso all'ingresso.

Cosa NON aspettarsi: nella cartella del mondo non compare ancora il file `data/redstone_exoframes_frames.dat`, perché in questa milestone nessuna funzione di gioco scrive lo stato del telaio. Comparirà con la milestone 3.

## Cosa riportare

Per ogni variante: esito di ogni voce, e per ogni problema le righe del log con qualche riga di contesto. Se un client viene espulso all'ingresso nel server, riporta il messaggio mostrato a schermo e le ultime cinquanta righe dei due log.
