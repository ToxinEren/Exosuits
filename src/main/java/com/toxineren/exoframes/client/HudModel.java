package com.toxineren.exoframes.client;

import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.energy.EnergyState;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.frame.HudLevel;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

/**
 * Che cosa disegna l'HUD della tuta in un tick (design §5.12), in Java puro con i soli tipi di core/: il livello, la
 * sagoma con il colore di ogni slot, la percentuale e il riempimento della barra, il colore dell'energia, l'avviso e le
 * celle di scorta. SuitHud lo calcola una volta per tick del client e lo disegna con i disegni della texture dell'HUD
 * che {@link HudLayout} sceglie da questi colori. I colori, ARGB, sono l'unica legenda dell'HUD e della stazione:
 * {@link StationLayout} prende da qui i suoi, cosi' un cambio di colore si fa in un posto solo (design §5.3 e §5.12).
 * Lo prova HudModelTest.
 *
 * @param level       il livello dell'HUD
 * @param silhouette  il colore di ogni slot della sagoma: tutti e sei con {@link HudLevel#FULL}, nessuno altrimenti
 * @param hasCell     vero se il telaio ha una cella
 * @param percent     la carica in percentuale, per difetto, in [0, 100]; 0 senza cella
 * @param fillWidth   il riempimento della barra in pixel dell'interfaccia, per difetto, in [0, {@link #FILL_MAX}]; 0
 *                    senza cella
 * @param energyColor il colore dell'energia: avviso, percentuale e riempimento
 * @param warningSlot vero se l'avviso ha il suo posto ({@code LOW} ed {@code EMPTY}), anche nel mezzo secondo spento
 * @param warningOn   vero se l'avviso si disegna in questo tick
 * @param spareCells  le celle di scorta, almeno 0
 */
record HudModel(HudLevel level, Map<PieceSlot, Paint> silhouette, boolean hasCell, int percent, int fillWidth,
        int energyColor, boolean warningSlot, boolean warningOn, int spareCells) {

    /** Va bene: un pezzo integro, l'energia {@code NORMAL} (design §5.12, "Colori", riga "va bene": verde 55FF55). */
    static final int GREEN = 0xFF55FF55;
    /**
     * Danneggiato: un pezzo dal 15 al 50 % (design §5.12, "Colori", riga "danneggiato": giallo FFFF55). Sulla barra
     * non compare mai: il server ha tre stati dell'energia.
     */
    static final int YELLOW = 0xFFFFFF55;
    /** Agire subito: un pezzo critico, l'energia {@code LOW} (design §5.12, "Colori", riga "agire subito": FF5555). */
    static final int RED = 0xFFFF5555;
    /** Fuori servizio: un pezzo rotto, l'energia {@code EMPTY} (design §5.12, "Colori", "fuori servizio": 8A8A8A). */
    static final int GREY = 0xFF8A8A8A;
    /**
     * L'energia prima del primo pacchetto con gli indicatori, "bianca come la riga di oggi" (design §5.12, "Elementi",
     * riga "barra"), e le celle di scorta quando ce n'e' almeno una (riga "celle di scorta": "bianco").
     */
    static final int WHITE = 0xFFFFFFFF;
    /** Il fondo scuro di un pezzo non montato (design §5.12, "Elementi", riga "sagoma": 5A000000). */
    static final int UNMOUNTED_FILL = 0x5A000000;
    /** Il contorno grigio di un pezzo non montato (design §5.12, "Elementi", riga "sagoma": FF464646). */
    static final int UNMOUNTED_OUTLINE = 0xFF464646;
    /** Il riempimento pieno della barra, in pixel dell'interfaccia (design §5.12, "Elementi", riga "barra": 61x6). */
    static final int FILL_MAX = 61;
    /**
     * Mezzo secondo in tick: con {@code LOW} l'avviso e' acceso e poi spento per tanto (design §5.12, "Elementi", riga
     * "avviso": "mezzo secondo acceso e mezzo spento").
     */
    static final int BLINK_TICKS = 10;

    /**
     * Tutti i colori che l'HUD disegna da se', cioe' i suoi testi: la percentuale nel colore dell'energia (verde,
     * rosso, grigio, o bianco prima del primo pacchetto con gli indicatori), "xN" delle celle di scorta in bianco o in
     * grigio e il testo senza cella in grigio. Tutto il resto viene dalla texture dei disegni dell'HUD (parte 4d, stile
     * A), con i colori esatti della legenda: lo controlla HudLayoutTest sulla PNG, anche che nessun texel sia magenta.
     * Nessuno di questi e' magenta per tools/client/arm_measure.sh: con la mano destra l'HUD sta nella meta' sinistra
     * degli scatti, dove il controllo "dipinto" di m3d_shots.sh vuole zero magenta, e con la sinistra nella meta'
     * destra ("mancino"). Lo controlla HudModelTest.
     */
    static final List<Integer> ALL_COLOURS = List.of(GREEN, RED, GREY, WHITE);

    /** Il modello del livello {@link HudLevel#NONE}: nulla da disegnare. */
    static final HudModel NONE = new HudModel(HudLevel.NONE, Map.of(), false, 0, 0, WHITE, false, false, 0);

    /**
     * Copia la sagoma in una mappa ordinata come {@link PieceSlot} e non modificabile.
     *
     * @throws IllegalArgumentException se {@code spareCells} e' negativo, {@code percent} e' fuori da [0, 100] o
     *                                  {@code fillWidth} e' fuori da [0, {@link #FILL_MAX}]
     */
    HudModel {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(silhouette, "silhouette");
        if (spareCells < 0) {
            throw new IllegalArgumentException("spareCells must be >= 0: " + spareCells);
        }
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent must be in [0, 100]: " + percent);
        }
        if (fillWidth < 0 || fillWidth > FILL_MAX) {
            throw new IllegalArgumentException("fillWidth must be in [0, " + FILL_MAX + "]: " + fillWidth);
        }
        // new EnumMap<>(map) rifiuta una mappa vuota che non e' una EnumMap: si copia con putAll
        Map<PieceSlot, Paint> copy = new EnumMap<>(PieceSlot.class);
        copy.putAll(silhouette);
        silhouette = Collections.unmodifiableMap(copy);
    }

    /**
     * Il modello di un tick. Con il livello {@link HudLevel#NONE} e' {@link #NONE}, e {@code state} puo' essere nullo;
     * con gli altri {@code state} e' la copia dello stato del client. La sagoma, solo con {@link HudLevel#FULL}: per
     * ogni slot il colore della condizione del pezzo montato, con il contorno a un terzo, o il pezzo non montato.
     * Percentuale e riempimento per difetto, dalla carica della cella (0 e 0 senza cella; con poca carica la barra
     * puo' restare vuota, come "0 %": lo stato lo dice il colore, che viene dal server);
     * colore dell'energia e avviso dallo stato dell'energia del server ({@code energy}, vuoto prima del primo pacchetto
     * con gli indicatori), che lampeggia secondo {@code ticksInState} (vedi {@link #ticksInState}).
     *
     * @throws NullPointerException     se il livello non e' {@code NONE} e {@code state} e' nullo
     * @throws IllegalArgumentException se {@code spareCells} e' negativo
     */
    static HudModel of(HudLevel level, FrameState state, Optional<EnergyState> energy, int spareCells,
            long ticksInState) {
        if (Objects.requireNonNull(level, "level") == HudLevel.NONE) {
            return NONE;
        }
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(energy, "energy");
        Map<PieceSlot, Paint> silhouette = new EnumMap<>(PieceSlot.class);
        if (level == HudLevel.FULL) {
            for (PieceSlot slot : PieceSlot.values()) {
                Piece piece = state.pieces().get(slot);
                silhouette.put(slot, piece == null ? new Paint(UNMOUNTED_FILL, UNMOUNTED_OUTLINE)
                        : mounted(piece.condition()));
            }
        }
        EnergyCell cell = state.cell();
        int percent = cell == null ? 0 : (int) (cell.stored() * 100L / cell.capacity());
        int fillWidth = cell == null ? 0 : (int) (cell.stored() * (long) FILL_MAX / cell.capacity());
        return new HudModel(level, silhouette, cell != null, percent, fillWidth, energyColor(energy),
                warningSlot(energy), warningOn(energy, ticksInState), spareCells);
    }

    /**
     * Il colore della condizione di un pezzo, con la legenda (design §5.12, "Colori"): integro {@link #GREEN},
     * danneggiato {@link #YELLOW}, critico {@link #RED}, rotto {@link #GREY}. La stazione usa questa
     * ({@link StationLayout#conditionColor}).
     */
    static int conditionColor(PieceCondition condition) {
        return switch (Objects.requireNonNull(condition, "condition must not be null")) {
            case INTACT -> GREEN;
            case DAMAGED -> YELLOW;
            case CRITICAL -> RED;
            case BROKEN -> GREY;
        };
    }

    /**
     * Il colore dell'energia dallo stato del server, con la legenda (design §5.12, "Colori"): {@code NORMAL}
     * {@link #GREEN}, {@code LOW} {@link #RED}, {@code EMPTY} {@link #GREY}; prima del primo pacchetto con gli
     * indicatori {@link #WHITE}. E' il posto della domanda 9 del design (§9): un cambio dei colori dell'energia si fa
     * qui.
     */
    static int energyColor(Optional<EnergyState> energy) {
        if (energy.isEmpty()) {
            return WHITE;
        }
        return switch (energy.get()) {
            case NORMAL -> GREEN;
            case LOW -> RED;
            case EMPTY -> GREY;
        };
    }

    /**
     * Il contorno di un pezzo della sagoma, "a un terzo di quel colore" (design §5.12, "Elementi", riga "sagoma"):
     * opaco, ogni canale diviso per 3, per difetto.
     */
    static int outline(int argb) {
        return opaque(argb, channel -> channel / 3);
    }

    /**
     * La prima riga del riempimento della barra, piu' chiara (design §5.12, riga "barra"): opaca, ogni canale a meta'
     * strada verso il bianco, per difetto. Il codice che disegna non la usa, perche' la prima riga sta gia' nei disegni
     * delle texture: e' la regola contro cui HudLayoutTest.spritesCoverTheHudTextureAndCarryTheLegend (i riempimenti
     * della barra dell'energia) e StationLayoutTest.spritesCoverTheScreenTextureAndCarryTheLegend (quelli della barra
     * della durata) provano le texture committate.
     */
    static int lighter(int argb) {
        return opaque(argb, channel -> channel + (255 - channel) / 2);
    }

    /**
     * I tick passati nello stato dell'energia di adesso: 0 nel tick in cui lo stato cambia (anche da e verso nessuno
     * stato), altrimenti uno piu' del tick prima. SuitHud lo porta da un tick all'altro: il lampeggio riparte dal tick
     * del cambio (design §5.12, riga "avviso").
     */
    static long ticksInState(Optional<EnergyState> before, Optional<EnergyState> now, long ticksBefore) {
        Objects.requireNonNull(before, "before");
        return now.equals(before) ? ticksBefore + 1 : 0;
    }

    /**
     * Vero se l'avviso si disegna (design §5.12, riga "avviso"): con {@code EMPTY} sempre; con {@code LOW} nel primo
     * mezzo secondo dal tick del cambio, poi spento per mezzo secondo, poi acceso, e cosi' via; altrimenti mai.
     */
    static boolean warningOn(Optional<EnergyState> energy, long ticksInState) {
        if (energy.isEmpty()) {
            return false;
        }
        return switch (energy.get()) {
            case EMPTY -> true;
            case LOW -> (ticksInState / BLINK_TICKS) % 2 == 0;
            case NORMAL -> false;
        };
    }

    /** Il colore di "xN" delle celle di scorta: bianco con almeno una, grigio con zero (design §5.12). */
    int spareColor() {
        return spareCells > 0 ? WHITE : GREY;
    }

    /** Il colore di un pezzo montato: il pieno nel colore della sua condizione, il contorno a un terzo. */
    private static Paint mounted(PieceCondition condition) {
        int colour = conditionColor(condition);
        return new Paint(colour, outline(colour));
    }

    /**
     * Vero con {@code LOW} ed {@code EMPTY}: l'avviso ha il suo posto anche spento, cosi' la percentuale non salta due
     * volte al secondo.
     */
    private static boolean warningSlot(Optional<EnergyState> energy) {
        return energy.map(e -> e == EnergyState.LOW || e == EnergyState.EMPTY).orElse(false);
    }

    private static int opaque(int argb, IntUnaryOperator channel) {
        int red = channel.applyAsInt((argb >> 16) & 0xFF);
        int green = channel.applyAsInt((argb >> 8) & 0xFF);
        int blue = channel.applyAsInt(argb & 0xFF);
        return 0xFF << 24 | red << 16 | green << 8 | blue;
    }

    /**
     * Il colore di un pezzo della sagoma, ARGB: il pieno e il contorno di 1 pixel attorno. SuitHud sceglie la figura
     * della texture dal solo pieno ({@link HudLayout#figure}).
     *
     * @param fill    il pieno
     * @param outline il contorno; il codice che disegna non lo legge: e' la regola contro cui
     *                HudLayoutTest.spritesCoverTheHudTextureAndCarryTheLegend prova il contorno delle figure della
     *                texture committata
     */
    record Paint(int fill, int outline) {
    }
}
