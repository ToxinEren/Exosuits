package com.toxineren.exoframes.client;

import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.station.StationAction;
import com.toxineren.exoframes.core.station.StationAction.Kind;
import com.toxineren.exoframes.core.station.StationView;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.ToIntFunction;

/**
 * La disposizione della schermata della stazione (design §5.3), in Java puro, senza classi di Minecraft: i
 * rettangoli dei widget, i colori dei testi, il colore di una condizione, il giro dell'anteprima e la vista della tuta
 * dalla vista del telaio; le regole dei clic sui posti del pannello del telaio e sulla freccia per smontare, che dicono
 * quale numero mandare al server (design §5.3 e §5.6), lo stato di un pulsante, che sceglie il suo disegno, e se i
 * numeri della durata stanno accanto alla condizione; poi i disegni della schermata nella loro texture (parte 4d,
 * stile A:
 * {@code textures/gui/station.png}, scritta da tools/art/gui.py), con i nomi della mappa del generatore, anche quelli
 * dei widget delle parti 4a-2 e 4b, e quale disegno va con una condizione. I rettangoli sono in pixel dell'interfaccia,
 * relativi all'angolo in alto a sinistra della schermata, con il bordo destro e quello in basso esclusi; ogni disegno
 * ha la grandezza del rettangolo che riempie. Lo sfondo e' opaco (il mondo dietro la schermata non deve trasparire), i
 * bordi dei posti e i riempimenti della barra hanno i colori esatti della legenda, e nessun texel e' magenta per
 * tools/client/arm_measure.sh (il magenta, nella schermata, e' solo quello della tuta nell'anteprima): lo controlla
 * StationLayoutTest sulla PNG del mod, insieme ai colori dei testi.
 */
final class StationLayout {
    /** Larghezza della schermata. */
    static final int WIDTH = 316;
    /** Altezza della schermata. */
    static final int HEIGHT = 232;

    /** Il titolo, con l'etichetta in alto a sinistra. */
    static final Rect TITLE = new Rect(8, 6, 140, 15);
    /** Il pannello del telaio, a sinistra: i sei posti dei pezzi e la cella. */
    static final Rect FRAME_PANEL = new Rect(7, 17, 73, 135);
    /**
     * I posti dei pezzi nel pannello del telaio, in forma di corpo visto di fronte come l'anteprima all'inizio (il
     * braccio destro a sinistra): ciascuno e' uno slot di 18x18 dentro un bordo di 1 pixel.
     */
    static final Map<PieceSlot, Rect> PIECE_SLOTS = pieceSlots();
    /** Il posto della cella, sotto i pezzi. */
    static final Rect CELL = new Rect(19, 101, 39, 121);
    /** Il pannello centrale, con l'intestazione della scelta. */
    static final Rect PIECE_PANEL = new Rect(77, 17, 211, 135);
    /** Il nome della scelta: il pezzo, la cella o tutti i pezzi. */
    static final Rect NAME = new Rect(81, 22, 191, 31);
    /** La condizione del pezzo scelto, nel suo colore. */
    static final Rect CONDITION = new Rect(81, 34, 150, 43);
    /** La durata e la durata massima del pezzo scelto, allineate a destra. */
    static final Rect DURABILITY_NUMBERS = new Rect(152, 34, 207, 43);
    /** La barra della durata del pezzo scelto (o della carica della cella). */
    static final Rect DURABILITY_BAR = new Rect(81, 44, 207, 49);
    /** Il pannello dell'anteprima 3D della tuta. */
    static final Rect PREVIEW_PANEL = new Rect(215, 17, 309, 135);
    /** Il ritaglio dell'anteprima, dentro il suo pannello: la tuta non ne esce, e trascinarci dentro la gira. */
    static final Rect PREVIEW_CLIP = new Rect(216, 18, 308, 134);
    /** L'etichetta dell'inventario del giocatore. */
    static final Rect INVENTORY_LABEL = new Rect(8, 139, 80, 148);
    /** Le tre file dell'inventario principale del giocatore, cornici degli slot comprese. */
    static final Rect INVENTORY = new Rect(7, 149, 169, 203);
    /** La barra degli oggetti del giocatore, cornici degli slot comprese. */
    static final Rect HOTBAR = new Rect(7, 207, 169, 225);
    /**
     * La riga dell'armatura custodita, nella riga del titolo, sulla sua piastra: "Custodisce un'armatura", solo se il
     * telaio ne custodisce una (design §5.7); come si ritira lo dice il suo suggerimento. E' testo del codice: la
     * texture non ha un disegno per lei.
     */
    static final Rect STOWED = new Rect(144, 6, 274, 15);
    /** Il pulsante "Tutti", sotto i posti: sceglie tutti i pezzi montati (design §5.3). */
    static final Rect ALL_BUTTON = new Rect(41, 103, 71, 119);
    /** La freccia accanto al nome della scelta: smonta il pezzo scelto, o la cella, nell'inventario (design §5.3). */
    static final Rect UNMOUNT = new Rect(195, 20, 207, 32);
    /**
     * La freccia a sinistra in fondo all'anteprima, a 3 pixel dai bordi del ritaglio (prima della parte 4d era il
     * carattere "<" negli stessi pixel): dice che la tuta si gira trascinando.
     */
    static final Rect ARROW_LEFT = new Rect(219, 122, 224, 129);
    /** La freccia a destra in fondo all'anteprima, specchio di {@link #ARROW_LEFT} (prima il carattere ">"). */
    static final Rect ARROW_RIGHT = new Rect(300, 122, 305, 129);

    /** Il testo. */
    static final int TEXT = 0xFFE6E6E6;
    /** Il testo spento: uno slot vuoto. */
    static final int TEXT_DIM = 0xFF9AA0A6;
    /** Un pezzo integro: il verde della legenda, che e' quella dell'HUD (design §5.3 e §5.12). */
    static final int CONDITION_INTACT = HudModel.GREEN;
    /** Un pezzo danneggiato: il giallo della legenda dell'HUD. */
    static final int CONDITION_DAMAGED = HudModel.YELLOW;
    /** Un pezzo critico: il rosso della legenda dell'HUD. */
    static final int CONDITION_CRITICAL = HudModel.RED;
    /** Un pezzo rotto: il grigio della legenda dell'HUD. */
    static final int CONDITION_BROKEN = HudModel.GREY;

    /**
     * Tutti i colori che la schermata disegna da se', cioe' i suoi testi, per il controllo del magenta e
     * dell'opacita': tutto il resto viene dalla texture ({@link #TEXTURE_W}).
     */
    static final List<Integer> ALL_COLOURS = List.of(TEXT, TEXT_DIM, CONDITION_INTACT, CONDITION_DAMAGED,
            CONDITION_CRITICAL, CONDITION_BROKEN);

    /**
     * Fra il bordo di un posto del pannello del telaio e la sua icona, o il fantasma di un posto vuoto: il bordo e la
     * cornice dello slot.
     */
    static final int ICON_INSET = 2;
    /** Il posto della cella, dopo i sei dei pezzi numerati come {@link PieceSlot}: lo stesso numero della scelta. */
    static final int CELL_PLACE = StationView.SELECT_CELL;
    /** I pixel che separano almeno, nell'intestazione, la condizione dai numeri della durata. */
    static final int HEADER_GAP = 4;

    /** La larghezza della texture della schermata (tools/art/gui.py, {@code textures/gui/station.png}). */
    static final int TEXTURE_W = 512;
    /** L'altezza della texture della schermata. */
    static final int TEXTURE_H = 256;

    /**
     * Lo sfondo della schermata, opaco, con le parti che non cambiano mai (cornice, pannelli, slot del giocatore, prese
     * dei posti, vaschetta della tavolozza, incavo della barra, fondo dell'anteprima, riga dei costi).
     */
    static final Sprite FONDO = new Sprite(0, 0, 316, 232);
    /** Il pulsante "Tutti" (41,103-71,119), normale. */
    static final Sprite TUTTI_NORMALE = new Sprite(318, 2, 30, 16);
    /** Il pulsante "Tutti" (41,103-71,119), sotto il mouse. */
    static final Sprite TUTTI_SOPRA = new Sprite(318, 18, 30, 16);
    /** Il pulsante "Tutti" (41,103-71,119), premuto. */
    static final Sprite TUTTI_PREMUTO = new Sprite(318, 34, 30, 16);
    /** Il pulsante "Tutti" (41,103-71,119), spento. */
    static final Sprite TUTTI_SPENTO = new Sprite(318, 50, 30, 16);
    /** Il pulsante "Ripara" (102,53-146,69), normale. */
    static final Sprite RIPARA_NORMALE = new Sprite(350, 2, 44, 16);
    /** Il pulsante "Ripara" (102,53-146,69), sotto il mouse. */
    static final Sprite RIPARA_SOPRA = new Sprite(350, 18, 44, 16);
    /** Il pulsante "Ripara" (102,53-146,69), premuto. */
    static final Sprite RIPARA_PREMUTO = new Sprite(350, 34, 44, 16);
    /** Il pulsante "Ripara" (102,53-146,69), spento. */
    static final Sprite RIPARA_SPENTO = new Sprite(350, 50, 44, 16);
    /** Il pulsante "Tutto: N" (148,53-207,69), normale. */
    static final Sprite RIPARA_TUTTO_NORMALE = new Sprite(396, 2, 59, 16);
    /** Il pulsante "Tutto: N" (148,53-207,69), sotto il mouse. */
    static final Sprite RIPARA_TUTTO_SOPRA = new Sprite(396, 18, 59, 16);
    /** Il pulsante "Tutto: N" (148,53-207,69), premuto. */
    static final Sprite RIPARA_TUTTO_PREMUTO = new Sprite(396, 34, 59, 16);
    /** Il pulsante "Tutto: N" (148,53-207,69), spento. */
    static final Sprite RIPARA_TUTTO_SPENTO = new Sprite(396, 50, 59, 16);
    /** Il pulsante "applica" di un preset, con il pennello, normale. */
    static final Sprite APPLICA_NORMALE = new Sprite(457, 2, 18, 16);
    /** Il pulsante "applica" di un preset, con il pennello, sotto il mouse. */
    static final Sprite APPLICA_SOPRA = new Sprite(457, 18, 18, 16);
    /** Il pulsante "applica" di un preset, con il pennello, premuto. */
    static final Sprite APPLICA_PREMUTO = new Sprite(457, 34, 18, 16);
    /** Il pulsante "applica" di un preset, con il pennello, spento. */
    static final Sprite APPLICA_SPENTO = new Sprite(457, 50, 18, 16);
    /** Il pulsante "salva" di un preset, con il libro, normale. */
    static final Sprite SALVA_NORMALE = new Sprite(477, 2, 18, 16);
    /** Il pulsante "salva" di un preset, con il libro, sotto il mouse. */
    static final Sprite SALVA_SOPRA = new Sprite(477, 18, 18, 16);
    /** Il pulsante "salva" di un preset, con il libro, premuto. */
    static final Sprite SALVA_PREMUTO = new Sprite(477, 34, 18, 16);
    /** Il pulsante "salva" di un preset, con il libro, spento. */
    static final Sprite SALVA_SPENTO = new Sprite(477, 50, 18, 16);
    /** Lo scudo di bronzo del telaio rinforzato (276,3-290,15). */
    static final Sprite RINFORZATO = new Sprite(497, 2, 14, 12);
    /** Un pulsante delle regioni, "Primario" o "Secondario", normale. */
    static final Sprite REGIONE_NORMALE = new Sprite(318, 68, 62, 12);
    /** Un pulsante delle regioni, "Primario" o "Secondario", sotto il mouse. */
    static final Sprite REGIONE_SOPRA = new Sprite(318, 80, 62, 12);
    /** Un pulsante delle regioni, "Primario" o "Secondario", premuto. */
    static final Sprite REGIONE_PREMUTO = new Sprite(318, 92, 62, 12);
    /** Un pulsante delle regioni, "Primario" o "Secondario", spento. */
    static final Sprite REGIONE_SPENTO = new Sprite(318, 104, 62, 12);
    /** Il pulsante a freccia che smonta la scelta (195,20-207,32), normale. */
    static final Sprite SMONTA_NORMALE = new Sprite(382, 68, 12, 12);
    /** Il pulsante a freccia che smonta la scelta (195,20-207,32), sotto il mouse. */
    static final Sprite SMONTA_SOPRA = new Sprite(382, 80, 12, 12);
    /** Il pulsante a freccia che smonta la scelta (195,20-207,32), premuto. */
    static final Sprite SMONTA_PREMUTO = new Sprite(382, 92, 12, 12);
    /** Il pulsante a freccia che smonta la scelta (195,20-207,32), spento. */
    static final Sprite SMONTA_SPENTO = new Sprite(382, 104, 12, 12);
    /** Il lucchetto (292,3-308,15), chiuso, normale. */
    static final Sprite BLOCCO_CHIUSO_NORMALE = new Sprite(396, 68, 16, 12);
    /** Il lucchetto (292,3-308,15), chiuso, sotto il mouse. */
    static final Sprite BLOCCO_CHIUSO_SOPRA = new Sprite(396, 80, 16, 12);
    /** Il lucchetto (292,3-308,15), chiuso, premuto. */
    static final Sprite BLOCCO_CHIUSO_PREMUTO = new Sprite(396, 92, 16, 12);
    /** Il lucchetto (292,3-308,15), chiuso, spento. */
    static final Sprite BLOCCO_CHIUSO_SPENTO = new Sprite(396, 104, 16, 12);
    /** Il lucchetto (292,3-308,15), aperto, normale. */
    static final Sprite BLOCCO_APERTO_NORMALE = new Sprite(414, 68, 16, 12);
    /** Il lucchetto (292,3-308,15), aperto, sotto il mouse. */
    static final Sprite BLOCCO_APERTO_SOPRA = new Sprite(414, 80, 16, 12);
    /** Il lucchetto (292,3-308,15), aperto, premuto. */
    static final Sprite BLOCCO_APERTO_PREMUTO = new Sprite(414, 92, 16, 12);
    /** Il lucchetto (292,3-308,15), aperto, spento. */
    static final Sprite BLOCCO_APERTO_SPENTO = new Sprite(414, 104, 16, 12);
    /** Il riquadro del materiale (81,52-99,70), quando il giocatore ne ha. */
    static final Sprite MATERIALE = new Sprite(432, 68, 18, 18);
    /** Il riquadro del materiale (81,52-99,70), spento: il giocatore non ne ha. */
    static final Sprite MATERIALE_SPENTO = new Sprite(432, 88, 18, 18);
    /** Il velo 16x16 sopra l'icona del materiale spento. */
    static final Sprite MATERIALE_VELO = new Sprite(452, 68, 16, 16);
    /** La freccia sinistra in fondo all'anteprima (ARROW_LEFT). */
    static final Sprite FRECCIA_SINISTRA = new Sprite(470, 68, 5, 7);
    /** La freccia destra in fondo all'anteprima (ARROW_RIGHT). */
    static final Sprite FRECCIA_DESTRA = new Sprite(477, 68, 5, 7);
    /** La freccia sinistra in fondo all'anteprima (ARROW_LEFT), piu' chiara, per il mouse nel ritaglio. */
    static final Sprite FRECCIA_SINISTRA_SOPRA = new Sprite(470, 77, 5, 7);
    /** La freccia destra in fondo all'anteprima (ARROW_RIGHT), piu' chiara, per il mouse nel ritaglio. */
    static final Sprite FRECCIA_DESTRA_SOPRA = new Sprite(477, 77, 5, 7);
    /** Il secondo bordo, bianco, attorno al posto scelto (il posto allargato di 1). */
    static final Sprite SCELTA = new Sprite(486, 68, 22, 22);
    /** L'anello di rame attorno alla tinta sotto il mouse (la tinta allargata di 1). */
    static final Sprite TINTA_SOPRA = new Sprite(486, 92, 14, 12);
    /** Un posto del pannello del telaio con un pezzo integro: il bordo verde 55FF55. */
    static final Sprite POSTO_INTEGRO = new Sprite(318, 118, 20, 20);
    /** Un posto del pannello del telaio con un pezzo danneggiato: il bordo giallo FFFF55. */
    static final Sprite POSTO_DANNEGGIATO = new Sprite(338, 118, 20, 20);
    /** Un posto del pannello del telaio con un pezzo critico: il bordo rosso FF5555. */
    static final Sprite POSTO_CRITICO = new Sprite(358, 118, 20, 20);
    /** Un posto del pannello del telaio con un pezzo rotto: il bordo grigio 8A8A8A. */
    static final Sprite POSTO_ROTTO = new Sprite(378, 118, 20, 20);
    /** Un posto del pannello del telaio senza pezzo (o senza cella): il bordo grigio 464646. */
    static final Sprite POSTO_VUOTO = new Sprite(398, 118, 20, 20);
    /** Il posto della cella con la cella: il bordo rosso della redstone C81E1E. */
    static final Sprite POSTO_CELLA = new Sprite(418, 118, 20, 20);
    /** L'icona in trasparenza dell'elmo in un posto vuoto (a +2 nel posto). */
    static final Sprite FANTASMA_ELMO = new Sprite(440, 118, 16, 16);
    /** L'icona in trasparenza del torso in un posto vuoto (a +2 nel posto). */
    static final Sprite FANTASMA_TORSO = new Sprite(456, 118, 16, 16);
    /** L'icona in trasparenza di un braccio in un posto vuoto (a +2 nel posto). */
    static final Sprite FANTASMA_BRACCIO = new Sprite(472, 118, 16, 16);
    /** L'icona in trasparenza di una gamba in un posto vuoto (a +2 nel posto). */
    static final Sprite FANTASMA_GAMBA = new Sprite(488, 118, 16, 16);
    /** L'icona in trasparenza della cella in un posto vuoto (a +2 nel posto). */
    static final Sprite FANTASMA_CELLA = new Sprite(456, 134, 16, 16);
    /** La cornice di una tinta (12x10): prima il colore della tinta, poi la cornice sopra. */
    static final Sprite TINTA = new Sprite(318, 140, 12, 10);
    /** Il velo di una tinta che il giocatore non ha, sopra il colore. */
    static final Sprite TINTA_SPENTA = new Sprite(332, 140, 12, 10);
    /** La cornice del colore del livello (193,88-207,110): prima i due colori, poi la cornice sopra. */
    static final Sprite PREDEFINITO = new Sprite(346, 140, 14, 22);
    /** L'anello di rame attorno al colore del livello sotto il mouse. */
    static final Sprite PREDEFINITO_SOPRA = new Sprite(362, 140, 16, 24);
    /** Il fondo dei dodici colori di un preset (colori.x0 - 1, colori.y0 - 1): poi i colori 5x8 sopra. */
    static final Sprite PRESET_COLORI = new Sprite(380, 140, 74, 10);
    /** Il fondo di un preset vuoto, su cui si scrive "vuoto". */
    static final Sprite PRESET_VUOTO = new Sprite(380, 152, 74, 10);
    /** Il fondo della barra della durata (81,44-207,49). */
    static final Sprite DURATA_FONDO = new Sprite(2, 234, 126, 5);
    /** Il riempimento della barra della durata di un pezzo integro: verde 55FF55, la prima riga piu' chiara. */
    static final Sprite DURATA_INTEGRO = new Sprite(2, 240, 126, 5);
    /** Il riempimento della barra della durata di un pezzo danneggiato: giallo FFFF55, la prima riga piu' chiara. */
    static final Sprite DURATA_DANNEGGIATO = new Sprite(2, 246, 126, 5);
    /** Il riempimento della barra della durata di un pezzo critico: rosso FF5555, la prima riga piu' chiara. */
    static final Sprite DURATA_CRITICO = new Sprite(130, 234, 126, 5);
    /** Il riempimento della barra della durata di un pezzo rotto: grigio 8A8A8A, la prima riga piu' chiara. */
    static final Sprite DURATA_ROTTO = new Sprite(130, 240, 126, 5);
    /**
     * Il riempimento della barra della durata della carica della cella: il rosso della redstone C81E1E, la prima riga
     * piu' chiara.
     */
    static final Sprite DURATA_CELLA = new Sprite(130, 246, 126, 5);


    /** I gradi di giro dell'anteprima per pixel dell'interfaccia trascinato (design §5.4). */
    static final double DEGREES_PER_PIXEL = 1.5;

    /** Che cosa c'e' sul cursore, per il posto cliccato: decide il clic su un posto (design §5.3). */
    enum Cursor {
        /** Niente. */
        EMPTY,
        /** Un pezzo del tipo del posto: il suo tipo si monta nello slot del posto. */
        FITTING_PIECE,
        /** Una cella. */
        CELL,
        /** Altro: un pezzo di un altro tipo, un oggetto qualunque (e un pezzo, per il posto della cella). */
        OTHER
    }

    /** Lo stato di un pulsante della schermata, con il suo disegno nella texture. */
    enum ButtonState {
        NORMAL,
        /** Sotto il mouse. */
        HOVER,
        /** Premuto: il tasto sinistro premuto sul pulsante e non ancora lasciato, o la scelta che il pulsante fa. */
        PRESSED,
        /** Spento: il clic non fa nulla. */
        OFF
    }

    private StationLayout() {
    }

    /** L'ascissa dell'angolo della schermata, centrata in un'interfaccia larga {@code guiWidth}. */
    static int leftPos(int guiWidth) {
        return (guiWidth - WIDTH) / 2;
    }

    /** L'ordinata dell'angolo della schermata, centrata in un'interfaccia alta {@code guiHeight}. */
    static int topPos(int guiHeight) {
        return (guiHeight - HEIGHT) / 2;
    }

    /**
     * Il colore della condizione di un pezzo: la legenda e' quella dell'HUD, design §5.3 e §5.12
     * ({@link HudModel#conditionColor}), cosi' un cambio di colore si fa in un posto solo.
     */
    static int conditionColor(PieceCondition condition) {
        return HudModel.conditionColor(condition);
    }

    /**
     * L'angolo dell'anteprima dopo un trascinamento di {@code dragX} pixel dell'interfaccia: {@link #DEGREES_PER_PIXEL}
     * gradi per pixel, riportato in [0, 360). Uno spostamento non finito (che il gioco non manda) lascia l'angolo dove
     * sta, cosi' l'anteprima non si perde.
     */
    static float rotate(float angleDegrees, double dragX) {
        double drag = Double.isFinite(dragX) ? dragX : 0.0;
        double turned = (angleDegrees + DEGREES_PER_PIXEL * drag) % 360.0;
        float angle = (float) (turned < 0.0 ? turned + 360.0 : turned);
        // un valore appena sotto 360 diventa 360 nel passaggio a float: e' di nuovo 0
        return angle >= 360f ? 0f : angle;
    }

    /**
     * La vista della tuta dell'anteprima dalla vista del telaio (design §5.4): per ogni pezzo montato lo stato visivo
     * della sua condizione ({@link SuitView#visualState}, come per il telaio nel mondo), e un colore
     * {@link Appearance#UNPAINTED} prende il colore predefinito del livello da {@link TierPalette}; gli slot senza
     * pezzo restano fuori, come in {@link SuitView#of}.
     */
    static SuitView suitView(StationView view) {
        Map<PieceSlot, SuitView.PieceView> pieces = new EnumMap<>(PieceSlot.class);
        view.pieces().forEach((slot, info) -> pieces.put(slot, new SuitView.PieceView(
                SuitView.visualState(PieceCondition.of(info.durability(), info.maxDurability())),
                colour(info.primary(), info.tier(), TierPalette::primary),
                colour(info.secondary(), info.tier(), TierPalette::secondary))));
        return new SuitView(pieces);
    }

    private static int colour(int painted, Tier tier, ToIntFunction<Tier> tierDefault) {
        return painted == Appearance.UNPAINTED ? tierDefault.applyAsInt(tier) : painted;
    }

    /**
     * Il posto del pannello del telaio di un pezzo nella sua condizione, con il bordo del colore della legenda
     * ({@link #conditionColor}); senza pezzo {@link #POSTO_VUOTO}, con la cella {@link #POSTO_CELLA}.
     */
    static Sprite place(PieceCondition condition) {
        return switch (Objects.requireNonNull(condition, "condition must not be null")) {
            case INTACT -> POSTO_INTEGRO;
            case DAMAGED -> POSTO_DANNEGGIATO;
            case CRITICAL -> POSTO_CRITICO;
            case BROKEN -> POSTO_ROTTO;
        };
    }

    /**
     * Il riempimento della barra della durata di un pezzo nella sua condizione, nel colore della legenda; per la cella
     * {@link #DURATA_CELLA}. Si disegna largo quanto la durata, sopra {@link #DURATA_FONDO}.
     */
    static Sprite durability(PieceCondition condition) {
        return switch (Objects.requireNonNull(condition, "condition must not be null")) {
            case INTACT -> DURATA_INTEGRO;
            case DAMAGED -> DURATA_DANNEGGIATO;
            case CRITICAL -> DURATA_CRITICO;
            case BROKEN -> DURATA_ROTTO;
        };
    }

    /**
     * Lo stato di un pulsante: spento prima di tutto, poi premuto, poi sotto il mouse, altrimenti normale.
     *
     * @param enabled vero se il clic sul pulsante fa qualcosa
     * @param pressed vero se il pulsante e' premuto (il tasto sinistro tenuto, o la scelta che fa)
     * @param hover   vero se il mouse ci sta sopra
     */
    static ButtonState buttonState(boolean enabled, boolean pressed, boolean hover) {
        if (!enabled) {
            return ButtonState.OFF;
        }
        if (pressed) {
            return ButtonState.PRESSED;
        }
        return hover ? ButtonState.HOVER : ButtonState.NORMAL;
    }

    /** Il disegno del pulsante "Tutti" ({@link #ALL_BUTTON}) nello stato dato; l'etichetta la scrive il codice. */
    static Sprite allSprite(ButtonState state) {
        return switch (Objects.requireNonNull(state, "state must not be null")) {
            case NORMAL -> TUTTI_NORMALE;
            case HOVER -> TUTTI_SOPRA;
            case PRESSED -> TUTTI_PREMUTO;
            case OFF -> TUTTI_SPENTO;
        };
    }

    /** Il disegno della freccia che smonta ({@link #UNMOUNT}) nello stato dato. */
    static Sprite unmountSprite(ButtonState state) {
        return switch (Objects.requireNonNull(state, "state must not be null")) {
            case NORMAL -> SMONTA_NORMALE;
            case HOVER -> SMONTA_SOPRA;
            case PRESSED -> SMONTA_PREMUTO;
            case OFF -> SMONTA_SPENTO;
        };
    }

    /**
     * Il fantasma di un posto vuoto, l'icona in trasparenza del suo tipo, da disegnare dove starebbe l'icona (a
     * {@link #ICON_INSET} nel posto): l'elmo, il torso, un braccio per i due bracci, una gamba per le due gambe e la
     * cella per il posto della cella.
     *
     * @param place il posto: 0-5 i pezzi nell'ordine di {@link PieceSlot}, {@link #CELL_PLACE} la cella
     * @throws IllegalArgumentException per un posto che non c'e'
     */
    static Sprite ghostSprite(int place) {
        checkPlace(place);
        if (place == CELL_PLACE) {
            return FANTASMA_CELLA;
        }
        return switch (PieceSlot.values()[place]) {
            case HELMET -> FANTASMA_ELMO;
            case TORSO -> FANTASMA_TORSO;
            case LEFT_ARM, RIGHT_ARM -> FANTASMA_BRACCIO;
            case LEFT_LEG, RIGHT_LEG -> FANTASMA_GAMBA;
        };
    }

    /**
     * Il posto del pannello del telaio nel punto dato, relativo all'angolo della schermata: 0-5 i pezzi nell'ordine di
     * {@link PieceSlot}, {@link #CELL_PLACE} la cella; vuoto fuori dai posti.
     */
    static OptionalInt placeAt(double x, double y) {
        for (PieceSlot slot : PieceSlot.values()) {
            if (PIECE_SLOTS.get(slot).contains(x, y)) {
                return OptionalInt.of(slot.ordinal());
            }
        }
        return CELL.contains(x, y) ? OptionalInt.of(CELL_PLACE) : OptionalInt.empty();
    }

    /** Vero se nella vista il posto ha il suo pezzo, o la sua cella, montato. */
    static boolean mounted(StationView view, int place) {
        checkPlace(place);
        return place == CELL_PLACE ? view.cell().isPresent() : view.pieces().containsKey(PieceSlot.values()[place]);
    }

    /**
     * Il numero che manda il clic sinistro su un posto del pannello del telaio (design §5.3 e §5.6), o vuoto se il clic
     * non manda nulla. Con le maiuscole smonta cio' che e' montato nel posto (16 + slot, 22 la cella), qualunque cosa
     * ci sia sul cursore, e su un posto vuoto non fa nulla. Senza: con il cursore vuoto sceglie il posto (lo slot, 6 la
     * cella); con un pezzo del tipo del posto lo monta (8 + slot: se lo slot e' occupato lo dice il server); con una
     * cella sul posto della cella la monta (14, o la scambia con quella montata); con altro non fa nulla. Il posto
     * cliccato e' quello: diversamente dalle mani, nessun ripiego sull'altro lato. Il server ricontrolla tutto.
     *
     * @param place   il posto: 0-5 i pezzi nell'ordine di {@link PieceSlot}, {@link #CELL_PLACE} la cella
     * @param cursor  che cosa c'e' sul cursore, per quel posto
     * @param shift   vero con le maiuscole premute
     * @param mounted vero se nella vista il posto ha il suo pezzo, o la sua cella ({@link #mounted})
     * @throws IllegalArgumentException per un posto che non c'e'
     */
    static OptionalInt placeButton(int place, Cursor cursor, boolean shift, boolean mounted) {
        checkPlace(place);
        Objects.requireNonNull(cursor, "cursor must not be null");
        boolean cell = place == CELL_PLACE;
        if (shift) {
            if (!mounted) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(cell ? StationAction.button(Kind.UNMOUNT_CELL, 0)
                    : StationAction.button(Kind.UNMOUNT_PIECE, place));
        }
        return switch (cursor) {
            case EMPTY -> OptionalInt.of(cell ? StationAction.button(Kind.SELECT_CELL, 0)
                    : StationAction.button(Kind.SELECT_PIECE, place));
            case FITTING_PIECE -> cell ? OptionalInt.empty() : OptionalInt.of(StationAction.button(Kind.MOUNT_PIECE,
                    place));
            case CELL -> cell ? OptionalInt.of(StationAction.button(Kind.MOUNT_CELL, 0)) : OptionalInt.empty();
            case OTHER -> OptionalInt.empty();
        };
    }

    /**
     * Il numero che manda la freccia ({@link #UNMOUNT}): smonta lo slot scelto se ha un pezzo (16 + slot), la cella se
     * e' scelta e c'e' (22); vuoto altrimenti, e con tutti i pezzi scelti: la freccia e' spenta.
     */
    static OptionalInt unmountButton(StationView view) {
        int selection = view.selection();
        if (selection == StationView.SELECT_ALL || !mounted(view, selection)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(selection == CELL_PLACE ? StationAction.button(Kind.UNMOUNT_CELL, 0)
                : StationAction.button(Kind.UNMOUNT_PIECE, selection));
    }

    /**
     * Vero se, nella riga della condizione dell'intestazione, i numeri allineati a destra in
     * {@link #DURABILITY_NUMBERS} stanno accanto alla condizione scritta da {@link #CONDITION} con almeno
     * {@link #HEADER_GAP} pixel fra le due: altrimenti i numeri di un pezzo perdono "/ massimo" (design §5.3; con
     * durate massime da 10 000 in su "10000 / 10000" e' largo 74 pixel). La cella non ha condizione: la larghezza a
     * sinistra e' 0, e i suoi numeri stanno nella riga intera.
     *
     * @param leftWidth  la larghezza del testo della condizione, in pixel dell'interfaccia
     * @param rightWidth la larghezza del testo dei numeri
     */
    static boolean fitsBeside(int leftWidth, int rightWidth) {
        return CONDITION.x0() + leftWidth + HEADER_GAP <= DURABILITY_NUMBERS.x1() - rightWidth;
    }

    /** @throws IllegalArgumentException per un posto che non e' fra 0 e {@link #CELL_PLACE} */
    private static void checkPlace(int place) {
        if (place < 0 || place > CELL_PLACE) {
            throw new IllegalArgumentException("place must be in [0, " + CELL_PLACE + "]: " + place);
        }
    }

    private static Map<PieceSlot, Rect> pieceSlots() {
        Map<PieceSlot, Rect> slots = new EnumMap<>(PieceSlot.class);
        slots.put(PieceSlot.HELMET, new Rect(30, 21, 50, 41));
        slots.put(PieceSlot.RIGHT_ARM, new Rect(9, 43, 29, 63));
        slots.put(PieceSlot.TORSO, new Rect(30, 43, 50, 63));
        slots.put(PieceSlot.LEFT_ARM, new Rect(51, 43, 71, 63));
        slots.put(PieceSlot.RIGHT_LEG, new Rect(19, 65, 39, 85));
        slots.put(PieceSlot.LEFT_LEG, new Rect(41, 65, 61, 85));
        return Collections.unmodifiableMap(slots);
    }

    /** Un rettangolo [x0, x1) x [y0, y1) in pixel dell'interfaccia, relativo all'angolo della schermata. */
    record Rect(int x0, int y0, int x1, int y1) {
        /** Vero se il punto, relativo all'angolo della schermata, sta nel rettangolo. */
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }

        int width() {
            return x1 - x0;
        }

        int height() {
            return y1 - y0;
        }

        /** Il rettangolo allargato di {@code by} pixel per lato, come il bordo della scelta attorno a un posto. */
        Rect grow(int by) {
            return new Rect(x0 - by, y0 - by, x1 + by, y1 + by);
        }
    }
}
