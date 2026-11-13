# Grafica di Redstone Exoframes

La grafica (modelli, texture, animazioni) è generata da script nella sessione cloud, su decisione di Luigi del 13 novembre 2026. Questo file tiene la direzione artistica, le scelte fatte e le anteprime mostrate.

## Direzione

Esoscheletro industriale pesante: sagoma massiccia, piastre rivettate, pistoni e tubi a vista, una fonte di energia sulla schiena. L'ispirazione generale indicata da Luigi sono le armature potenziate di Fallout 4 e Iron Monger (Iron Man, 2008). Si prende l'idea, non le forme: nessun elmo, piastra, simbolo o scritta riconoscibile di design esistenti.

Stile Minecraft: scatole allineate agli assi, texture a pixel senza sfumature morbide, 16 pixel per blocco. Il telaio si disegna attorno a un giocatore di dimensioni normali e non ne cambia l'ingombro.

## Livello Rottame: tre direzioni proposte (13 novembre 2026)

Le tavole sono in `anteprime/2026-11-13-direzioni/`. I riquadri chiari su petto e spalla segnano solo il posto riservato alle decalcomanie; il manichino dentro il telaio nudo non fa parte del modello.

| Direzione | Idea | Giudizio di chi l'ha disegnata |
|---|---|---|
| A, piastre squadrate | Spalle alte, elmo incassato con fessura luminosa, guanti pesanti | Sagoma più pulita e leggibile |
| B, telaio a vista | Piastre piccole e asimmetriche, pistoni scoperti, mezza maschera con una lente | La più debole: confusa da lontano |
| C, caldaia | Caldaia di rame sulla schiena con comignolo, manometro e tubi; avambracci grossi; elmo piccolo a oblò | La più caratteristica; davanti un po' affollata |

**Scelta di Luigi:** in attesa. **Predefinita se non risponde:** C per il Rottame (sembra costruita con pezzi di recupero), A come direzione dei livelli più avanzati.

## Colori predefiniti del Rottame

Usati quando un pezzo non è verniciato. Primario `0xB5651D` (arancio ruggine), secondario `0x5F7D7A` (verde rame spento). Il task dei pezzi Rottame può cambiarli se la resa lo richiede; il codice li legge da qui.

## Dove sta cosa

- Generatore: `tools/art/` (il punto di partenza è lo spike in `tools/art/spike/`, che il piano della parte 3a riscrive).
- File generati: `src/main/resources/assets/redstone_exoframes/geo/`, `textures/entity/`, `animations/`.
- Convenzioni sui nomi delle ossa e sui perni: `docs/superpowers/specs/2026-11-13-m3-telaio-design.md`, §5.1.
