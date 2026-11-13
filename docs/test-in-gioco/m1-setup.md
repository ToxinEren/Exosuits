# Checklist di avvio in gioco: milestone 1 (setup)

Per Jarvis, sul PC Windows di Luigi. Obiettivo: verificare che le quattro varianti del mod si avviino (client e server dedicato) e che l'oggetto di prova compaia. Esegui i passi in ordine e spunta le caselle `- [ ]`. Alla fine consegna a Luigi il riepilogo descritto in "Cosa riportare".

Contenuto da verificare in ogni variante:

- mod id `redstone_exoframes`, nome "Redstone Exoframes" nel menu mod;
- tab creativa "Redstone Exoframes" con la "Cella di redstone" (en: Redstone Cell), id `redstone_exoframes:redstone_cell`, texture segnaposto 16x16;
- all'avvio il log contiene la riga `Redstone Exoframes init (redstone_exoframes)`.

## 0. Procurarsi i jar

Scegli UNA delle due strade. Alla fine devono esserci quattro file:

- `redstone-exoframes-fabric-0.1.0+mc1.20.1.jar`
- `redstone-exoframes-fabric-0.1.0+mc1.21.1.jar`
- `redstone-exoframes-forge-0.1.0+mc1.20.1.jar`
- `redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar`

**A. Artefatto della CI (consigliato).** La CI (`.github/workflows/build.yml`) compila i quattro jar a ogni push e li carica nell'artefatto `redstone-exoframes-jars`.

- [ ] Scarica con `gh run download --repo ToxinEren/Exosuits -n redstone-exoframes-jars` (oppure dalla scheda Actions di GitHub, scegliendo l'ultima esecuzione verde del branch `m1-setup`).
- [ ] Verifica la presenza dei quattro jar con il comando: `bash tools/check_jars.sh <cartella-scaricata>` (dal repository).

**B. Build locale.** Serve JDK 21 per eseguire Gradle (il wrapper e' Gradle 9.9.0). La prima build scarica molto e puo' richiedere 10-15 minuti. Su Windows usa `gradlew.bat` al posto di `./gradlew`.

- [ ] Esegui dalla radice del repository:

```
./gradlew --console=plain clean :1.20.1-forge:build :1.20.1-fabric:build :1.21.1-fabric:build :1.21.1-neoforge:build :1.20.1-forge:collectJar :1.20.1-fabric:collectJar :1.21.1-fabric:collectJar :1.21.1-neoforge:collectJar
```

- [ ] I jar sono in `build/libs/all/`. Controlla con `bash tools/check_jars.sh` (cartella di default `build/libs/all`).
- [ ] Facoltativo: `bash tools/check_sources.sh` per i controlli sui sorgenti.

## 1. Regole generali

- Le quattro varianti vanno provate in **quattro istanze separate** (una per variante, con cartella `mods` propria). Non mescolare mai jar di varianti diverse nella stessa cartella.
- Usa la versione di Java richiesta dalla variante: Java 17 per 1.20.1, Java 21 per 1.21.1 (i launcher moderni la scelgono da soli).
- Metti nella cartella `mods` dell'istanza SOLO i jar indicati nella tabella della variante (mod + dipendenze), nessun altro mod.
- Per il test del server dedicato usa una cartella separata per variante, con la stessa versione di loader e gli stessi jar in `mods`; accetta l'EULA (`eula=true`).
- Il file di log e' `latest.log`: per il client in `<istanza>\logs\latest.log`, per il server in `<cartella-server>\logs\latest.log`.

## 2. Profili da preparare

| # | Variante | Minecraft | Java | Loader | Dipendenze (versioni esatte) | Jar del mod |
|---|----------|-----------|------|--------|------------------------------|-------------|
| 1 | 1.20.1 Forge | 1.20.1 | 17 | Forge 47.4.29 | Architectury API 9.2.14 (forge), GeckoLib 4.8.4 (forge-1.20.1) | `redstone-exoframes-forge-0.1.0+mc1.20.1.jar` |
| 2 | 1.20.1 Fabric | 1.20.1 | 17 | Fabric Loader 0.20.1 | Fabric API 0.92.12+1.20.1, Architectury API 9.2.14 (fabric), GeckoLib 4.8.4 (fabric-1.20.1) | `redstone-exoframes-fabric-0.1.0+mc1.20.1.jar` |
| 3 | 1.21.1 NeoForge | 1.21.1 | 21 | NeoForge 21.1.266 | Architectury API 13.0.11 (neoforge), GeckoLib 4.9.3 (neoforge-1.21.1) | `redstone-exoframes-neoforge-0.1.0+mc1.21.1.jar` |
| 4 | 1.21.1 Fabric | 1.21.1 | 21 | Fabric Loader 0.20.1 | Fabric API 0.116.18+1.21.1, Architectury API 13.0.11 (fabric), GeckoLib 4.9.3 (fabric-1.21.1) | `redstone-exoframes-fabric-0.1.0+mc1.21.1.jar` |

Prendi le dipendenze dalle pagine ufficiali (Modrinth o CurseForge), scegliendo esattamente la versione e il loader indicati.

## 3. Test per variante

Ripeti la sezione per ognuna delle quattro varianti, ciascuna nella propria istanza.

### 3.1 Client

- [ ] Crea l'istanza della variante (tabella sopra), installa il loader e metti in `mods` il jar del mod e le sue dipendenze.
- [ ] Avvia il client fino al menu principale: nessun crash, nessuna schermata di errore del loader.
- [ ] Menu "Mods" (Forge/NeoForge) oppure Mod Menu se installato (Fabric): la voce "Redstone Exoframes" e' presente. Su Fabric senza Mod Menu, salta questo punto e annotalo (non e' un errore).
- [ ] Crea un mondo creativo e apri l'inventario creativo: c'e' la tab "Redstone Exoframes".
- [ ] Nella tab c'e' la "Cella di redstone": l'icona NON e' viola/nera (niente texture mancante) e il nome e' in italiano ("Cella di redstone") con la lingua del gioco su italiano.
- [ ] Passa la lingua su inglese: il nome diventa "Redstone Cell".
- [ ] Prendi l'oggetto, mettilo in mano e nell'inventario: nessun errore.
- [ ] Esci dal mondo e chiudi il gioco normalmente.
- [ ] In `latest.log` cerca la riga `Redstone Exoframes init (redstone_exoframes)`: deve comparire una volta.
- [ ] In `latest.log` cerca le righe di livello ERROR o WARN che citano `redstone_exoframes` o `architectury`: devono essere assenti.

### 3.2 Server dedicato

- [ ] Prepara una cartella server per la variante con lo stesso loader e gli stessi jar in `mods` (per i server serve solo il lato comune: va bene lo stesso jar).
- [ ] Avvia il server. Attendi la riga con `Done (...)! For help, type "help"` in `logs\latest.log`.
- [ ] Nel log non compaiono errori che citano `redstone_exoframes` o `architectury`.
- [ ] La riga `Redstone Exoframes init (redstone_exoframes)` compare nel log del server.
- [ ] Facoltativo: con il client della stessa variante, collegati a `localhost` ed entra nel mondo senza errori.
- [ ] Ferma il server con `stop` e attendi la chiusura pulita.

## 4. Cosa riportare

Per ognuna delle quattro varianti riporta a Luigi:

- [ ] esito (OK / non OK) separato per client e server dedicato;
- [ ] il percorso del `latest.log` del client e il suo contenuto se c'e' un problema; altrimenti la riga `Redstone Exoframes init (redstone_exoframes)` copiata per intero;
- [ ] il percorso del `latest.log` del server e la riga `Done (...)`;
- [ ] uno screenshot della tab creativa "Redstone Exoframes" aperta, con la "Cella di redstone" ben visibile;
- [ ] ogni riga di errore o avviso che citi `redstone_exoframes` o `architectury` (copiata per intero, con alcune righe di contesto o lo stack trace);
- [ ] versioni realmente usate (loader, Java, dipendenze) se differiscono dalla tabella;
- [ ] eventuali passi saltati e il motivo.

Riepilogo finale: una tabella 4 righe x (client, server) con OK o non OK, piu' i dettagli dei fallimenti.
