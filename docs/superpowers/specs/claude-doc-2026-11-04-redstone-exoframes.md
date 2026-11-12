# Redstone Exoframes — Documento di design

> Esportazione fedele del Claude Doc del 4 novembre 2026 (autore: Luigi Maldera, scritto con Claude nella chat "Guadagnare producendo mod Minecraft"). Questo è il documento di autorità per le meccaniche di gioco; `2026-11-12-redstone-exoframes-design.md` lo integra con le decisioni tecniche. Esportato il 12 novembre 2026 dalla revisione 14 del doc.

## Visione e vincoli

Redstone Exoframes è un mod di power armor originale, ambientato nel mondo di Minecraft, pensato per essere monetizzato e incluso nei modpack. Le meccaniche si ispirano a Fallout 4 (telaio in cui si entra, pezzi separati, energia da gestire, stazione di modifica), ma nomi, estetica e lore sono interamente originali.

- **Obiettivo commerciale:** pubblicazione su Modrinth e CurseForge con Rewards attivi, donazioni tramite Ko-fi/Discord, candidatura a contest e modpack.
- **Vincolo IP:** nessun nome, modello o elemento riconoscibile di Fallout (niente T-51, Fusion Core, Fratellanza, Vault-Tec). Materiali e tecnologia derivano da redstone, rame, ferro, diamante, netherite ed End.
- **Target:** modpack di avventura e tech; il mod deve funzionare bene da solo e convivere con mod come Create e Mekanism senza dipenderne.
- **Versioni:** 1.21.1 (NeoForge + Fabric) come principale, 1.20.1 (Forge + Fabric) come backport, da un unico codice sorgente.
- **Riuso:** sistema armature, abilità e modelli GeckoLib del mod Mjolnir Armor.

## Il telaio (Exoframe)

Il telaio è un'entità piazzabile nel mondo: il giocatore ci entra, non lo indossa come un'armatura normale. Questo è il pezzo tecnicamente nuovo rispetto a Mjolnir.

- **Piazzamento:** l'item telaio si piazza come un'entità (stile armor stand). I pezzi si montano sul telaio con tasto destro o dalla stazione di manutenzione.
- **Ingresso:** tasto destro sul telaio con una cella carica inserita. Animazione di apertura (GeckoLib), il giocatore viene "agganciato" e l'entità telaio viene rimossa; i pezzi passano negli slot armatura del giocatore, bloccati.
- **Uscita:** tasto dedicato (default `V`, tenuto premuto 1 s). Il telaio torna entità nel punto in cui si trova il giocatore, con tutti i pezzi e la cella.
- **Inventario:** l'inventario normale resta accessibile. Gli slot armatura sono bloccati finché si è nel telaio.
- **Effetti base del telaio (anche senza pezzi):** nessun danno da caduta (sostituito dall'onda d'urto se c'è il modulo), +knockback resistance, velocità −15%, non si nuota (si affonda e si cammina sul fondo), non si può sprintare se l'energia è sotto il 10%.
- **Furto e morte:** alla morte del giocatore il telaio cade come entità, non come item sparso, così non si perdono i pezzi. Un altro giocatore può entrarci solo se il proprietario l'ha sbloccato (opzione di config per i server).

## Pezzi, danni e riparazione

L'armatura è composta da sei pezzi indipendenti, ognuno con la propria durabilità: un pezzo rotto smette di proteggere e disattiva i moduli montati su di esso.

| Pezzo | Quota protezione | Slot moduli | Effetto se rotto |
|---|---|---|---|
| Elmo | 15% | 1 | Niente HUD avanzato, niente visori |
| Torso | 30% | 2 | Consumo energia +25% |
| Braccio sx | 12,5% | 1 | Modulo braccio disattivato |
| Braccio dx | 12,5% | 1 | Modulo braccio disattivato |
| Gamba sx | 15% | 1 | Velocità −15% |
| Gamba dx | 15% | 1 | Velocità −15% |

- **Distribuzione del danno:** il danno ricevuto va al pezzo più vicino al punto d'impatto (testa → elmo, proiettili dall'alto → elmo/torso, caduta → gambe), il resto si divide tra gli altri pezzi.
- **Stati visivi:** integro, danneggiato (texture graffiata sotto il 50%), critico (scintille sotto il 15%), rotto (pezzo annerito).
- **Riparazione:** alla stazione di manutenzione con il materiale del tier (ferro, diamante, netherite…). Riparazione rapida sul campo con un kit di riparazione consumabile (ripristina il 25% di un pezzo).
- **Pezzi rotti non spariscono:** restano montati finché non si riparano o sostituiscono.

## Sistema energetico

Tutto funziona con le celle di redstone: una cella piena dura circa 20 minuti di uso normale, e senza energia l'armatura diventa un peso morto.

- **Cella di redstone:** item con 10.000 unità di energia (RE). Si crafta con redstone, rame e un cristallo di quarzo; si ricarica nel caricatore.
- **Caricatore:** blocco che ricarica le celle con redstone (1 polvere = 500 RE) o, se presente, con FE/Energy di altri mod (compatibilità opzionale, rapporto in config).
- **Cambio cella:** dall'interno del telaio con un tasto (animazione 2 s, vulnerabile durante il cambio). Le celle di scorta stanno nell'inventario normale.

| Azione | Consumo |
|---|---|
| Stare fermi | 2 RE/s |
| Camminare | 8 RE/s |
| Sprint | 20 RE/s |
| Salto | 15 RE a salto |
| Moduli attivi | Indicato per modulo |

- **Energia bassa (sotto il 10%):** niente sprint, moduli disattivati, avviso nell'HUD.
- **Energia a zero:** velocità −70%, protezione al 50%, nessun modulo. Il giocatore deve uscire o cambiare cella.
- I valori sono tutti in un file di config per il bilanciamento dei modpack.

## Tier e progressione

Cinque tier seguono la progressione vanilla, dal ferro all'End: ogni tier è un set di pezzi, montabili anche misti sullo stesso telaio.

| Tier | Materiale principale | Come si ottiene | Protezione (set completo) | Slot moduli extra | Tratto speciale |
|---|---|---|---|---|---|
| 1 Rottame | Ferro + rame | Crafting base, o trovato nei villaggi in rovina | Pari al ferro | 0 | Economico, si rompe in fretta |
| 2 Industriale | Ferro + redstone | Crafting alla stazione | Pari al diamante | +1 | Forza mineraria +20% |
| 3 Corazzata | Diamante | Crafting alla stazione | Diamante +30% | +2 | Resistenza ai proiettili |
| 4 Netherite | Netherite | Upgrade dalla Corazzata (smithing template) | Netherite +20% | +2 | Immune a fuoco e lava, galleggia sulla lava camminando |
| 5 Prototipo | Materiali dell'End | Schema trovato solo nel Laboratorio sepolto | Netherite +50% | +3 | Rigenera lentamente i pezzi usando energia |

- Il telaio è unico per tutti i tier; esiste un solo upgrade del telaio (rinforzato) che aumenta la durabilità base.
- I valori di protezione sono indicativi e da bilanciare nella milestone di test.

## Moduli

I moduli si installano alla stazione di manutenzione, ognuno su un pezzo specifico, e consumano energia solo quando sono attivi.

| Modulo | Pezzo | Attivazione | Consumo | Effetto |
|---|---|---|---|---|
| Jetpack | Torso | Tenere premuto salto in aria | 60 RE/s | Volo verticale breve, max 4 s, poi ricarica |
| Onda d'urto | Gambe (entrambe) | Atterraggio da oltre 6 blocchi | 200 RE | Danno e knockback ad area, nessun danno al giocatore |
| Visore notturno | Elmo | Toggle | 3 RE/s | Visione notturna |
| Scanner | Elmo | Toggle | 5 RE/s | Evidenzia mob ostili e minerali vicini |
| Respiratore | Elmo | Passivo | 4 RE/s sott'acqua | Respirazione subacquea |
| Lama idraulica | Braccio | Attacco | 10 RE a colpo | Danno corpo a corpo +6 |
| Trivella | Braccio | Tenere premuto | 25 RE/s | Scava 3x3 come un piccone del tier |
| Scudo energetico | Torso | Toggle | 30 RE/s | Assorbe i proiettili finché c'è energia |
| Rilevatore di Sculk | Elmo | Passivo | 1 RE/s | Mostra sensori e shrieker vicini, avvisa del Warden |
| Servomotori | Gambe | Passivo | +5 RE/s | Velocità +20%, annulla il malus del telaio |

- Numero di slot = slot base del pezzo + slot extra del tier.
- I moduli si craftano alla stazione; quelli avanzati (scudo, trivella) richiedono il tier Corazzata o superiore.

## Stazione di manutenzione e personalizzazione

La stazione di manutenzione è il blocco centrale del mod: ci si piazza davanti il telaio per montare pezzi, riparare, installare moduli e verniciare.

- **Struttura:** blocco 2x1 con GUI dedicata. Il telaio va posizionato sul blocco o entro 2 blocchi.
- **GUI:** a sinistra il telaio con i sei pezzi cliccabili; al centro gli slot del pezzo selezionato (materiali di riparazione, moduli); a destra anteprima 3D ruotabile.
- **Crafting:** i pezzi dei tier 2–5 e i moduli si craftano qui, non nel banco da lavoro.
- **Verniciatura:** ogni pezzo ha un colore primario e uno secondario, applicabili con le tinte vanilla.
- **Decalcomanie:** uno stendardo applicato alla spalla o al torso mostra il suo disegno sull'armatura.
- **Preset:** salvataggio di 3 combinazioni colore per giocatore.

La personalizzazione è puramente estetica ma è la funzione che i giocatori mostrano di più in screenshot e video: va curata fin dalla prima release.

## HUD e controlli

Dentro il telaio compare un HUD dedicato con energia, stato dei sei pezzi e moduli attivi; senza elmo (o con elmo rotto) si vede solo la barra dell'energia.

- **In basso a sinistra:** barra energia con percentuale e numero di celle di scorta.
- **In basso a destra:** sagoma dell'armatura con i sei pezzi colorati per stato (verde, giallo, rosso, grigio = rotto).
- **In alto a destra:** icone dei moduli attivi con consumo istantaneo.
- **Bordo schermo:** leggera vignetta con riflessi del vetro dell'elmo, disattivabile.

| Tasto (default) | Azione |
|---|---|
| V (tenuto 1 s) | Esci dal telaio |
| R | Cambia cella |
| G | Menu radiale moduli |
| N | Toggle visore notturno |
| H | Nascondi HUD |

Tutti i tasti sono rimappabili dalle impostazioni dei controlli.

## Contenuti nel mondo

Tre elementi generati nel mondo danno al giocatore un motivo per esplorare e creano i momenti da screenshot.

- **Villaggio in rovina con telaio abbandonato:** variante rara dei villaggi distrutti; contiene un telaio con 2–4 pezzi Rottame danneggiati e una cella scarica. È il primo incontro del giocatore con il mod.
- **Laboratorio sepolto:** struttura sotterranea nelle Deep Dark o vicino alle Ancient City, protetta da torrette a redstone. Contiene lo schema del tier Prototipo e moduli rari.
- **Illager Corazzato:** mob ostile in un telaio Industriale, compare nelle ondate finali dei raid. Lento, resistente, usa l'onda d'urto. Alla morte lascia pezzi danneggiati con una certa probabilità.
- **Bottino:** celle e kit di riparazione nei forzieri di miniere, avamposti dei saccheggiatori e città antiche.

Tutti i contenuti generati sono disattivabili da config, così i creator di modpack possono adattarli.

## Architettura tecnica

Un solo repository produce quattro jar: 1.21.1 NeoForge, 1.21.1 Fabric, 1.20.1 Forge, 1.20.1 Fabric. Architectury separa i loader, Stonecutter separa le versioni.

```
redstone-exoframes/
  common/          logica di gioco + astrazioni (nessun import di loader)
    core/          energia, danni per pezzo, moduli, tier: Java puro, testabile
    compat/        interfacce: ItemDataAccess, ArmorMaterials, Net, Ids
    content/       registrazione item, blocchi, entità via Architectury
    client/        HUD, GUI stazione, renderer GeckoLib
  fabric/          entrypoint e glue Fabric
  neoforge/        entrypoint e glue NeoForge (1.21.1)
  forge/           entrypoint e glue Forge (1.20.1)
  versions/        configurazioni Stonecutter per 1.20.1 e 1.21.1
```

Le differenze tra versioni sono chiuse dentro quattro interfacce in `compat/`, con un'implementazione per versione scelta da Stonecutter:

| Interfaccia | 1.20.1 | 1.21.1 |
|---|---|---|
| ItemDataAccess (energia cella, durabilità pezzi, moduli, colori) | NBT sull'ItemStack | Data components registrati |
| ArmorMaterials | Enum che implementa ArmorMaterial | Registry ArmorMaterial (Holder) |
| Net (sincronizzazione HUD, attivazione moduli, uscita dal telaio) | Pacchetti con FriendlyByteBuf | CustomPacketPayload + StreamCodec |
| Ids | `new ResourceLocation(ns, path)` | `ResourceLocation.fromNamespaceAndPath(ns, path)` |

**Regole per Claude Code:**

1. Nessuna classe in `core/` importa classi di Minecraft che cambiano tra 1.20.1 e 1.21.1.
2. Ogni accesso ai dati degli item passa da `ItemDataAccess`, mai NBT o componenti diretti.
3. I commenti condizionali di Stonecutter (`//? if >=1.21 {`) sono ammessi solo nei file di `compat/`.
4. Ogni milestone si compila e si avvia su tutti e quattro i target prima di passare alla successiva.

**Dipendenze:** Architectury API, GeckoLib (modelli e animazioni di telaio e pezzi), opzionalmente Cloth Config per la config su Fabric.

## Roadmap

Lo sviluppo procede in sette milestone; la prima release pubblica arriva alla milestone 5, con un set giocabile completo.

1. **Setup:** progetto Architectury + Stonecutter con i quattro target che compilano e avviano un item di prova.
2. **Core:** sistema energia, danni per pezzo e interfacce `compat/` con test unitari sul codice in `core/`.
3. **Telaio:** entità piazzabile, ingresso e uscita, tier Rottame completo con modelli GeckoLib (riuso da Mjolnir).
4. **Stazione e HUD:** blocco stazione con GUI, riparazione, verniciatura, HUD.
5. **Alpha pubblica:** tier 1–3, cinque moduli (jetpack, onda d'urto, visore, lama, servomotori), caricatore e celle. Pubblicazione su Modrinth e CurseForge, Rewards attivi, Discord aperto.
6. **Mondo:** villaggio in rovina, Illager Corazzato, bottino nei forzieri.
7. **Release 1.0:** tier 4–5, moduli restanti, Laboratorio sepolto, bilanciamento e config completa.

## Decisioni aperte

- [ ] Nome definitivo del mod: "Redstone Exoframes" è provvisorio; va verificato che non sia già usato su Modrinth e CurseForge.
- [ ] Stile visivo: industriale e squadrato (stile Create) o più militare e liscio?
- [ ] Verificare in milestone 1 che Stonecutter e Architectury convivano senza problemi sui quattro target; in alternativa, due branch separati per versione.
- [ ] Compatibilità energia con altri mod (FE/Energy): già nell'alpha o dopo la 1.0?
- [ ] Il telaio può essere usato da altri giocatori sui server? Default proposto: solo dal proprietario.
- [ ] Licenza del codice: open source (es. MIT/LGPL, aiuta l'ingresso nei modpack) o All Rights Reserved?
