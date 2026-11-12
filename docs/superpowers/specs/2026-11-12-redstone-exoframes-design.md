# Redstone Exoframes — Documento di design per la realizzazione

Data: 2026-11-12 · Stato: bozza in attesa di approvazione di Luigi
Fonte primaria: Claude Doc "Redstone Exoframes — Documento di design" (4 novembre 2026), scritto da Luigi con Claude nella chat "Guadagnare producendo mod Minecraft". Questo file ne è la versione operativa per chi costruisce: riprende le decisioni di quel documento, aggiunge gli approcci valutati e le decisioni prese il 12 novembre, e segna cosa resta aperto. In caso di conflitto vale il Claude Doc, salvo le decisioni elencate al §7.

## 1. Intento e perché

Redstone Exoframes è un mod di power armor originale per Minecraft Java, pensato per essere **monetizzato** (Modrinth e CurseForge con Rewards attivi, donazioni Ko-fi/Discord, candidatura a contest e modpack). Le meccaniche si ispirano a Fallout 4 — un telaio in cui si entra, pezzi separati con durabilità propria, energia da gestire, stazione di modifica — ma nomi, estetica e lore sono interamente originali.

Perché ora: Luigi ha già un mod di armature funzionante (Mjolnir Armor, 1.20.1 Forge, GeckoLib) che però è a tema Halo e quindi non monetizzabile. Exoframes riusa quell'esperienza e quel codice su un'IP propria.

Successo = una alpha pubblica giocabile (milestone 5) pubblicata su Modrinth e CurseForge con Rewards attivi, che gira su quattro target e convive con i modpack tech/avventura.

## 2. Obiettivi, non-obiettivi, anti-obiettivi

**Obiettivi**
- Quattro jar da un solo sorgente: 1.21.1 NeoForge, 1.21.1 Fabric, 1.20.1 Forge, 1.20.1 Fabric.
- Esperienza completa del telaio: ingresso/uscita, sei pezzi con danno localizzato, celle di energia, stazione di manutenzione, moduli, cinque tier, HUD.
- Tutti i valori di bilanciamento e i contenuti generati nel mondo disattivabili/regolabili da config (per i creator di modpack).
- Personalizzazione estetica curata fin dalla prima release (vernici, decalcomanie, preset): è ciò che i giocatori mostrano in screenshot e video.
- Codice `core/` in Java puro, senza import di Minecraft, coperto da test unitari.

**Non-obiettivi (per ora)**
- Compatibilità energia FE/Energy con altri mod: dopo la 1.0 (decisione §7).
- Versioni diverse da 1.20.1 e 1.21.1; Quilt.
- Dipendenza da Create o Mekanism: il mod deve convivere con loro, non dipenderne.
- Multiplayer avanzato (permessi, fazioni): basta l'opzione "telaio usabile solo dal proprietario".

**Anti-obiettivi (fallimento anche se funziona)**
- Qualsiasi nome, modello o elemento riconoscibile di Fallout o di Halo nel codice pubblicato, negli asset o nel testo (vincolo IP).
- Un mod che funziona solo su un loader o una versione: la promessa commerciale è "gira ovunque".
- Un telaio che dipende da mod esterni per funzionare (Pehkui, Create, ecc.).
- Build verdi ma gioco rotto: ogni milestone deve avviarsi su tutti e quattro i target prima di passare alla successiva.

## 3. Vincoli

- **IP:** materiali e tecnologia derivano da redstone, rame, ferro, diamante, netherite ed End. Niente T-51, Fusion Core, Fratellanza, Vault-Tec, Spartan, MJOLNIR, UNSC ecc.
- **Target:** 1.21.1 (Java 21) principale; 1.20.1 (Java 17) backport. Un unico codice sorgente.
- **Riuso:** sistema armature, abilità e modelli GeckoLib di Mjolnir Armor (repo GitHub indicato da Luigi come base). Si riusa il **codice** (sistemi), non gli asset né i nomi.
- **Dipendenze ammesse:** Architectury API, GeckoLib; opzionalmente Cloth Config (solo per la schermata di config su Fabric).
- **Ambiente di lavoro:** lo sviluppo e la compilazione avvengono in una sessione cloud (Java 21, Gradle 8.14) senza Minecraft; i test in gioco li fanno Jarvis e le sessioni locali sul PC di Luigi. Ogni milestone va quindi consegnata con istruzioni di test in gioco precise.
- **Rete della sessione cloud:** servono gli host Maven di Mojang, Fabric, Architectury, NeoForge, Forge, kikugie (Stonecutter), Cloudsmith (GeckoLib), Modrinth maven; vanno autorizzati prima della milestone 1.

## 4. Approcci considerati per il multiloader/multiversione

| | A. Architectury multi-modulo + Stonecutter | B. Stonecutter multi-variante + Architectury API (scelto) | C. Due branch per versione, Architectury per branch |
|---|---|---|---|
| Struttura | `common/ fabric/ forge/ neoforge/` × versioni gestite da Stonecutter | Un solo source tree; 4 varianti Stonecutter `1.20.1-fabric`, `1.20.1-forge`, `1.21.1-fabric`, `1.21.1-neoforge`; loader e versione scelti da `//? if` solo in `compat/` | Branch `1.20.1` e `1.21.1`, ciascuno un progetto Architectury classico |
| Pro | È lo schema del Claude Doc; separazione netta dei loader in moduli | Un solo Gradle da capire; la configurazione dei target è dichiarativa; è lo schema che la maggior parte dei mod multiloader+multiversione usa oggi con Stonecutter; le build dei 4 jar sono un comando (`chiseledBuild`) | Nessuna magia; ogni branch è un progetto standard |
| Contro | Stonecutter × Architectury multi-modulo ha attriti noti (ogni versione duplica tre moduli, `chiseled` task su progetti annidati); setup lungo e fragile | Il codice specifico di loader convive nello stesso tree: va disciplinato (solo in `compat/` e `platform/`) | Ogni fix va portato a mano sull'altro branch; a regime raddoppia il lavoro |
| Rischio principale | Perdere la milestone 1 a far convivere i due plugin | Che una differenza di loader "scappi" fuori da `compat/` | Divergenza dei branch |

**Scelta: B.** Rispetta la sostanza del Claude Doc ("Architectury separa i loader, Stonecutter separa le versioni"): Architectury API continua a separare i loader a livello di registrazione, eventi, rete e menu; Stonecutter separa le versioni **e** sceglie il loader per ogni variante. Le regole del Claude Doc restano: nessun import Minecraft in `core/`, accesso ai dati degli item solo via `ItemDataAccess`, commenti condizionali solo in `compat/` (e nei pochi file di `platform/` che sono gli entrypoint dei loader). Se in milestone 1 B non regge su uno dei quattro target, si ripiega su C (branch per versione), non su A.

## 5. Architettura decisa

### 5.1 Layout del repository

```
redstone-exoframes/
  settings.gradle.kts         Stonecutter: 4 varianti (versione-loader)
  stonecutter.gradle.kts      variante attiva, parametri, swap
  build.gradle.kts            build comune a tutte le varianti (Architectury Loom)
  stonecutter.properties.toml versioni centralizzate per variante
  versions/                   una cartella per variante (generata da Stonecutter)
  src/main/java/it/<group>/exoframes/
    core/        energia, danni per pezzo, moduli, tier, config model: Java puro, testabile
    compat/      interfacce + implementazioni per versione/loader (unico posto con //? if)
                 ItemDataAccess, ArmorMaterials, Net, Ids, Platform
    content/     registrazione item, blocchi, entità, menu, suoni via Architectury
    entity/      ExoframeEntity (telaio piazzato), IllagerCorazzato (M6)
    block/       Stazione di manutenzione, Caricatore
    item/        pezzi, celle, kit, schema, moduli
    client/      HUD, GUI stazione, renderer GeckoLib, tasti
    platform/    entrypoint Fabric / Forge / NeoForge (solo glue)
  src/main/resources/         assets e data comuni; fabric.mod.json, mods.toml, neoforge.mods.toml
  src/test/java/              test unitari del core (JUnit 5)
  docs/superpowers/specs/     questo documento
  docs/test-in-gioco/         checklist per milestone da eseguire sul PC
```

### 5.2 Le interfacce di `compat/` (dal Claude Doc)

| Interfaccia | 1.20.1 | 1.21.1 |
|---|---|---|
| `ItemDataAccess` (energia cella, durabilità pezzi, moduli, colori) | NBT sull'ItemStack | Data components registrati |
| `ArmorMaterials` | Enum che implementa `ArmorMaterial` | Registry `ArmorMaterial` (Holder) |
| `Net` (sync HUD, attivazione moduli, uscita dal telaio) | Pacchetti con `FriendlyByteBuf` (via Architectury Networking) | `CustomPacketPayload` + `StreamCodec` |
| `Ids` | `new ResourceLocation(ns, path)` | `ResourceLocation.fromNamespaceAndPath(ns, path)` |
| `Platform` (config dir, isClient, isModLoaded, menu type) | per loader | per loader |

### 5.3 Dipendenze e versioni (lette dai Maven il 12 novembre 2026)

| | 1.20.1 | 1.21.1 |
|---|---|---|
| Java | 17 (bytecode `--release 17`, compilato con JDK 21) | 21 |
| Loader | Forge 1.20.1-47.4.29; Fabric Loader 0.20.1 | NeoForge 21.1.266; Fabric Loader 0.20.1 |
| Fabric API | 0.92.12+1.20.1 | 0.116.18+1.21.1 |
| Architectury API | 9.2.14 | 13.0.11 |
| GeckoLib | 4.8.4 (`geckolib-forge-1.20.1`, `geckolib-fabric-1.20.1`, maven Cloudsmith) | 4.9.3 (`geckolib-neoforge-1.21.1`, `geckolib-fabric-1.21.1`) |
| Toolchain | Architectury Loom 1.17.493 (plugin `dev.architectury.loom`), architectury-plugin 3.5.170, Stonecutter 0.10, Gradle 8.14.3 (wrapper scaricato da GitHub releases) | idem |
| Mappature | Mojang ufficiali su tutti i target (Forge: SRG solo a runtime via reobf) | Mojang ufficiali |

Se una combinazione non risolve (per esempio Loom 1.17 con Forge 1.20.1), si scende di versione di Loom, non di loader.

### 5.4 Modello dati del telaio indossato (sei pezzi su quattro slot) — scelta del costruttore

Sei pezzi logici (elmo, torso, braccio sx, braccio dx, gamba sx, gamba dx) non entrano nei quattro slot armatura vanilla, quindi:

- Lo **stato del telaio** (sei pezzi con durabilità, moduli e colori; cella inserita; tier; proprietario; armatura "civile" stivata) vive in un dato agganciato al giocatore (Forge/NeoForge: attachment/capability; Fabric: `fabric-data-attachment-api`), serializzato tramite `ItemDataAccess`-like codec in `core/`.
- I quattro slot armatura ricevono **quattro stack segnaposto bloccati** (`exoframe_helmet`, `exoframe_torso`, `exoframe_legs`, `exoframe_boots`) che non si possono togliere, non si possono buttare e non cadono alla morte; servono a far passare la protezione nel calcolo vanilla dell'armatura e a bloccare gli slot. Lo stack torso porta anche le due braccia; lo stack gambe porta le due gambe; `exoframe_boots` esiste solo per occupare lo slot.
- Il **rendering** non usa i segnaposto: un layer GeckoLib dedicato sul renderer del giocatore legge lo stato del telaio e disegna i sei pezzi con il loro stato visivo. Così braccia e gambe sinistre/destre hanno texture e danni indipendenti.
- L'**armatura che il giocatore indossava** prima di entrare viene stivata nello stato del telaio e rimessa addosso all'uscita; se all'uscita gli slot non sono liberi (non può succedere perché i segnaposto vengono rimossi per primi) cade a terra come item.
- La **ripartizione del danno** si calcola in `core/` a partire dalla sorgente (`DamageSource` → categoria: testa, alto, caduta, generico) e aggiorna lo stato; il valore di protezione dei segnaposto viene ricalcolato dai pezzi integri.
- Alla **morte**, lo stato del telaio diventa un'entità telaio al punto di morte (pezzi, cella, armatura stivata dentro) e lo stato del giocatore viene azzerato.

### 5.5 Cosa si prende da Mjolnir Armor

Da stabilire con l'audit del repo (task 2). Candidati: struttura degli item armatura GeckoLib, renderer e modelli di armatura animata, sistema di abilità/energia (se presente), HUD, stazione di assemblaggio, gestione tasti. Esclusi a priori: asset, nomi, testi, Pehkui (Mjolnir scala il giocatore; il Claude Doc di Exoframes non lo prevede: il telaio è reso come armatura GeckoLib sovradimensionata senza cambiare l'hitbox, decisione aperta al §10).

## 6. Contenuti di gioco (sintesi; i dettagli sono nel Claude Doc)

- **Telaio (Exoframe):** item che si piazza come entità (stile armor stand). Ingresso con tasto destro e cella carica: animazione GeckoLib, il giocatore viene agganciato, l'entità rimossa, i pezzi passano negli slot armatura bloccati. Uscita con `V` tenuto 1 s: il telaio torna entità con pezzi e cella. Effetti base: niente danno da caduta, +knockback resistance, velocità −15%, non si nuota, niente sprint sotto il 10% di energia. Alla morte il telaio cade come entità; altri giocatori ci entrano solo se sbloccato (config).
- **Pezzi:** elmo 15% / torso 30% / braccia 12,5% ciascuno / gambe 15% ciascuna della protezione; slot moduli 1/2/1/1/1/1; il danno va al pezzo più vicino all'impatto, il resto si divide; stati visivi integro/danneggiato (<50%)/critico (<15%)/rotto; riparazione alla stazione col materiale del tier o kit da campo (+25%). I pezzi rotti restano montati.
- **Energia:** cella di redstone da 10.000 RE (redstone + rame + cristallo di quarzo); caricatore a redstone (1 polvere = 500 RE) con FE opzionale dopo la 1.0; cambio cella con `R` (2 s, vulnerabile). Consumi: fermo 2 RE/s, cammino 8, sprint 20, salto 15 RE, moduli come da tabella. Sotto il 10%: niente sprint e moduli; a zero: velocità −70%, protezione 50%.
- **Tier:** 1 Rottame (ferro+rame, pari al ferro, si trova nei villaggi in rovina) · 2 Industriale (ferro+redstone, pari al diamante, +1 slot, forza mineraria +20%) · 3 Corazzata (diamante, +30%, +2 slot, resistenza ai proiettili) · 4 Netherite (upgrade con smithing template, +20% su netherite, +2 slot, immune a fuoco/lava, cammina sulla lava) · 5 Prototipo (materiali dell'End, schema dal Laboratorio sepolto, +50%, +3 slot, rigenera i pezzi con energia). Telaio unico per tutti i tier, un solo upgrade "rinforzato".
- **Moduli:** jetpack (torso, 60 RE/s, 4 s) · onda d'urto (gambe, 200 RE, atterraggio >6 blocchi) · visore notturno (elmo, 3 RE/s) · scanner (elmo, 5 RE/s) · respiratore (elmo, 4 RE/s sott'acqua) · lama idraulica (braccio, 10 RE/colpo, +6 danno) · trivella (braccio, 25 RE/s, scava 3x3) · scudo energetico (torso, 30 RE/s, assorbe proiettili) · rilevatore di Sculk (elmo, 1 RE/s) · servomotori (gambe, +5 RE/s, velocità +20%). Avanzati (scudo, trivella) dal tier Corazzata.
- **Stazione di manutenzione:** blocco 2x1 con GUI (telaio a sinistra con sei pezzi cliccabili, slot del pezzo al centro, anteprima 3D a destra); crafting dei pezzi tier 2–5 e dei moduli; verniciatura (colore primario e secondario con tinte vanilla); decalcomanie da stendardo; 3 preset colore per giocatore.
- **HUD e tasti:** barra energia in basso a sinistra, sagoma pezzi in basso a destra, moduli attivi in alto a destra, vignetta elmo disattivabile. Tasti default: `V` esci, `R` cambia cella, `G` menu radiale moduli, `N` visore, `H` nascondi HUD; tutti rimappabili.
- **Mondo:** villaggio in rovina con telaio abbandonato (2–4 pezzi Rottame danneggiati + cella scarica), Laboratorio sepolto (Deep Dark/Ancient City, torrette a redstone, schema Prototipo), Illager Corazzato (raid, telaio Industriale, onda d'urto, lascia pezzi danneggiati), bottino nei forzieri. Tutto disattivabile da config.

## 7. Decisioni prese il 12 novembre (correggibili da Luigi)

1. **Nome provvisorio confermato per iniziare:** "Redstone Exoframes", mod id `redstone_exoframes`. Repo di destinazione creato da Luigi: `ToxinEren/Exosuits` (privato, branch `main`); il nome del repo non vincola il nome del mod. La ricerca web non trova mod con questo nome (i più vicini: EXO-Craft, Exotronics); l'API di Modrinth non era raggiungibile dalla sessione cloud: Luigi verifica dal browser prima della pubblicazione. Rinominare dopo è possibile ma tocca tutto il progetto.
2. **Licenza:** codice MIT, asset (texture, modelli, suoni, animazioni) "Tutti i diritti riservati". Facilita l'ingresso nei modpack e la fiducia, protegge la parte che costa di più. Il repo nasce **privato**; si rende pubblico quando Luigi decide.
3. **Base di codice:** nuovo repo con storia pulita; i sistemi riusabili di Mjolnir Armor vengono copiati con attribuzione nel README, non "forkati" (la storia di Mjolnir contiene asset Halo che non devono finire in un repo pubblico monetizzato).
4. **Compatibilità FE/Energy:** dopo la 1.0, dietro interfaccia già prevista nel caricatore.
5. **Telaio sui server:** default solo il proprietario; opzione di config per sbloccare.
6. **Stile visivo:** rimandato alla milestone 3 (serve prima vedere i modelli riusabili da Mjolnir); nel frattempo i modelli di prova sono grigi/neutri.
7. **Multiloader/multiversione:** approccio B del §4.
8. **Divisione del lavoro:** sessione cloud = codice, build, test unitari, documentazione; Jarvis e le sessioni locali = test in gioco, screenshot, bilanciamento, pubblicazione. Ogni milestone chiude con una checklist in `docs/test-in-gioco/`.
9. **Accesso al telaio nell'alpha (M5 arriva prima del mondo, M6):** il telaio e i pezzi Rottame si craftano al banco da lavoro (telaio: ferro + rame + redstone; pezzi T1 come da doc "crafting base"); il villaggio in rovina in M6 diventa una via alternativa, non l'unica.
10. **Personalizzazione per milestone:** vernici primaria/secondaria e preset in M4 (quindi nell'alpha); decalcomanie da stendardo in M7 (richiedono composizione delle texture a runtime). In attesa di conferma di Luigi.
11. **Asset:** si riusa da Mjolnir solo il codice (renderer, classi modello, controller di animazione, HUD); geometrie, texture, animazioni e suoni sono nuovi perché le forme MJOLNIR sono IP Halo. La sessione cloud produce modelli e texture placeholder puliti e una specifica asset (nomi dei bone, UV, maschere di tinta) per chi farà quelli definitivi. Chi li fa e se l'alpha può uscire con i placeholder: in attesa di Luigi (§10).
12. **Automazione in M1:** workflow GitHub Actions che compila i quattro jar a ogni push, così Jarvis scarica gli artefatti senza che la sessione cloud debba restare viva.
13. **Il telaio non cambia le dimensioni del giocatore** (nessuna dipendenza da Pehkui); da confermare entro la fine di M2 perché il sì cambierebbe M3 (mixin su dimensioni, camera e collisioni).

## 8. Milestone e criteri di accettazione

| # | Milestone | Fatto quando | Dove |
|---|---|---|---|
| 1 | Setup | Le 4 varianti compilano da zero (`clean chiseledBuild`) e producono 4 jar con un item di prova registrato, icona e nome; i 4 jar si avviano su client e server dedicati fino a "Done" | build: cloud · avvio: PC |
| 2 | Core | `core/` implementa energia, consumo per azione, danni per pezzo, stati, moduli, tier, con test unitari verdi (JUnit 5) e config model serializzabile; `compat/` implementato per 1.20.1 e 1.21.1 | cloud |
| 3 | Telaio | Entità telaio piazzabile, ingresso/uscita, pezzi negli slot bloccati, effetti base, tier Rottame completo con modelli GeckoLib (riuso Mjolnir), cella e caricatore | cloud + test PC |
| 4 | Stazione e HUD | Blocco stazione con GUI, riparazione, verniciatura, HUD, tasti | cloud + test PC |
| 5 | Alpha pubblica | Tier 1–3, cinque moduli (jetpack, onda d'urto, visore, lama, servomotori), caricatore e celle; pubblicazione su Modrinth e CurseForge, Rewards attivi, Discord aperto | PC (pubblicazione) |
| 6 | Mondo | Villaggio in rovina, Illager Corazzato, bottino | cloud + test PC |
| 7 | Release 1.0 | Tier 4–5, moduli restanti, Laboratorio sepolto, bilanciamento, config completa | cloud + test PC |

Regola trasversale (dal Claude Doc): ogni milestone si compila e si avvia su tutti e quattro i target prima della successiva.

## 9. Lasciato al costruttore

- Scelta esatta delle versioni di Loom/Stonecutter/loader che risolvono insieme (§5.3) e del wrapper Gradle.
- Forma del `DeferredRegister`/helper di registrazione sopra Architectury; ordine di registrazione deterministico.
- Formato interno dei dati (chiavi NBT / tipi di component) dietro `ItemDataAccess`, purché stabile e versionato.
- Struttura del config (file JSON Gson in `configDir`, un solo file, con sezione per modpack).
- Come il tier Rottame viene reso prima degli asset definitivi (placeholder neutri).
- Strumenti di datagen per modelli, blockstate, loot, ricette.

## 10. Domande aperte (per Luigi)

- [ ] **Arte:** chi realizza geometrie, texture, animazioni e suoni definitivi (Luigi con Blockbench, un artista esterno)? L'alpha può uscire con i placeholder della sessione cloud? Proposta in attesa: placeholder nell'alpha, asset definitivi entro la 1.0.
- [ ] **Decalcomanie:** nell'alpha o nella 1.0? Proposta: 1.0 (§7.10).
- [ ] **Provenienza del codice di Mjolnir:** tutto di Luigi, o contiene parti da template/tutorial/altri mod con licenza (GPL/LGPL) incompatibile con MIT? Verifica anche nell'audit.
- [ ] **Scadenze:** c'è una data obiettivo per l'alpha o un contest? Senza, le milestone restano come sono.
- [ ] Conferma del nome definitivo (vedi §7.1).
- [ ] Il telaio cambia le dimensioni del giocatore (come Mjolnir con Pehkui) o no? Proposta: no, nessuna dipendenza (§7.13).
- [ ] Repo privato all'inizio e MIT/ARR come licenza: va bene?
- [ ] Account/organizzazione GitHub di destinazione per `redstone-exoframes` (decide anche il package Java `it.<group>.exoframes`).

Il Claude Doc del 4 novembre è esportato integralmente in `docs/superpowers/specs/claude-doc-2026-11-04-redstone-exoframes.md`, così chi costruisce non dipende dall'accesso al doc originale.
