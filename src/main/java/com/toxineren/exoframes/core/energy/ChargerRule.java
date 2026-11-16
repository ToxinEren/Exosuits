package com.toxineren.exoframes.core.energy;

/**
 * La regola di carica del caricatore (design §12.11): Java puro, la block entity tiene lo stato e la chiama una volta
 * per tick del server.
 * <p>
 * Il caricatore funziona come una fornace: la polvere di redstone e' il combustibile, la riserva sono i RE della
 * polvere che sta bruciando. A ogni tick, se c'e' una cella non piena: se la riserva e' vuota e c'e' polvere, si
 * consuma una polvere e la riserva prende {@code rePerRedstone} RE; poi dalla riserva passano alla cella al piu'
 * {@code rePerSecond / 20} RE, con un riporto in millesimi di RE per la parte che non fa un RE intero. Una cella piena
 * (o sopra la capacita': vale piena, e la sua carica non si tocca) non brucia nulla: la riserva resta per la cella
 * dopo. Il riporto e' ritmo, non energia: si tiene solo se tutto il ritmo del tick e' passato, e si lascia cadere
 * quando la riserva finisce o la cella si riempie a meta' tick. L'energia invece si conserva sempre: carica + riserva
 * + polveri x {@code rePerRedstone} non cambia mai.
 * <p>
 * Con i predefiniti (500 RE per polvere, 50 RE al secondo, cella da 10 000 RE): 2,5 RE per tick (2 nei tick dispari,
 * 3 nei pari), una polvere dura 200 tick, una cella vuota e' piena in 4 000 tick (200 s) con 20 polveri.
 * <p>
 * Conseguenza dichiarata: si brucia al piu' una polvere per tick. Se {@code rePerSecond / 20} supera
 * {@code rePerRedstone}, il ritmo vero e' {@code rePerRedstone} RE per tick, sotto quello della config; con i
 * predefiniti il limite e' 500 RE per tick contro 2,5.
 */
public final class ChargerRule {
    /** Il riporto e' in millesimi di RE: sempre sotto un RE intero. */
    private static final int MILLI = 1000;

    private ChargerRule() {
    }

    /** Lo stato che il caricatore dice al giocatore, in ordine di precedenza. */
    public enum Status {
        /** Nessuna cella nel caricatore. */
        NO_CELL,
        /** La cella e' piena (o sopra la capacita'). */
        FULL,
        /** Ne' polvere ne' riserva: la cella aspetta. */
        NO_REDSTONE,
        /** C'e' una cella non piena e qualcosa da bruciare. */
        CHARGING
    }

    /**
     * I valori dopo un tick.
     *
     * @param stored     la carica della cella, in RE
     * @param dust       le polveri rimaste
     * @param reserve    i RE rimasti della polvere che sta bruciando
     * @param carryMilli il riporto del ritmo, in millesimi di RE, in [0, 999]
     * @param charging   vero se in questo tick il caricatore stava caricando (la proprieta' {@code lit} del blocco):
     *                   anche in un tick che non sposta nessun RE intero, e anche nel tick in cui la riserva finisce
     */
    public record Result(int stored, int dust, int reserve, int carryMilli, boolean charging) {
    }

    /**
     * Un tick del server di un caricatore con una cella dentro.
     *
     * @param stored        la carica della cella, in RE; puo' superare {@code capacity} (vale piena)
     * @param capacity      la capacita' della cella, dalla config
     * @param dust          le polveri di redstone nello slot
     * @param reserve       i RE rimasti della polvere che sta bruciando
     * @param carryMilli    il riporto del tick prima, in millesimi di RE, in [0, 999]
     * @param rePerRedstone i RE di una polvere, dalla config
     * @param rePerSecond   il ritmo di carica in RE al secondo, dalla config
     * @return i valori nuovi; la carica non supera mai {@code capacity}, salvo quella che la superava gia'
     * @throws IllegalArgumentException se {@code capacity}, {@code rePerRedstone} o {@code rePerSecond} non sono
     *                                  positivi, se {@code stored}, {@code dust} o {@code reserve} sono negativi, se
     *                                  {@code carryMilli} e' fuori da [0, 999]
     */
    public static Result tick(int stored, int capacity, int dust, int reserve, int carryMilli, int rePerRedstone,
            int rePerSecond) {
        requirePositive("capacity", capacity);
        requireNonNegative("stored", stored);
        requireNonNegative("dust", dust);
        requireNonNegative("reserve", reserve);
        if (carryMilli < 0 || carryMilli >= MILLI) {
            throw new IllegalArgumentException("carryMilli must be in [0, 999]: " + carryMilli);
        }
        requirePositive("rePerRedstone", rePerRedstone);
        requirePositive("rePerSecond", rePerSecond);

        if (stored >= capacity) {
            // piena: nessuna polvere bruciata, la riserva resta per la cella dopo
            return new Result(stored, dust, reserve, 0, false);
        }
        if (reserve == 0 && dust > 0) {
            // una polvere al piu' per tick
            dust--;
            reserve = rePerRedstone;
        }
        if (reserve == 0) {
            return new Result(stored, dust, 0, 0, false);
        }
        // in long: rePerSecond * 1000 trabocca un int gia' ai limiti della config
        long budget = carryMilli + rePerSecond * 1000L / EnergyRates.TICKS_PER_SECOND;
        long whole = budget / MILLI;
        int rest = (int) (budget % MILLI);
        // moved <= capacity - stored: sta in un int e la carica non supera la capacita'
        int moved = (int) Math.min(whole, Math.min(reserve, capacity - stored));
        // il riporto si tiene solo se tutto il ritmo del tick e' passato
        return new Result(stored + moved, dust, reserve - moved, moved == whole ? rest : 0, true);
    }

    /**
     * Lo stato da mostrare, in ordine di precedenza: senza cella {@link Status#NO_CELL}; cella piena
     * {@link Status#FULL}; ne' polvere ne' riserva {@link Status#NO_REDSTONE}; altrimenti {@link Status#CHARGING}.
     *
     * @throws IllegalArgumentException se {@code capacity} non e' positiva, se {@code stored}, {@code dust} o
     *                                  {@code reserve} sono negativi
     */
    public static Status status(boolean hasCell, int stored, int capacity, int dust, int reserve) {
        requirePositive("capacity", capacity);
        requireNonNegative("stored", stored);
        requireNonNegative("dust", dust);
        requireNonNegative("reserve", reserve);
        if (!hasCell) {
            return Status.NO_CELL;
        }
        if (stored >= capacity) {
            return Status.FULL;
        }
        if (dust == 0 && reserve == 0) {
            return Status.NO_REDSTONE;
        }
        return Status.CHARGING;
    }

    /**
     * La carica in centesimi della capacita', da 0 a 100, arrotondata per difetto: 100 solo con la cella piena.
     *
     * @throws IllegalArgumentException se {@code capacity} non e' positiva o {@code stored} e' negativo
     */
    public static int percent(int stored, int capacity) {
        return scaled(100, stored, capacity);
    }

    /**
     * L'uscita per il comparatore, da 0 a 15: 0 senza cella, altrimenti la carica in quindicesimi della capacita',
     * arrotondata per difetto (0 anche con una cella vuota, 15 solo con la cella piena).
     *
     * @throws IllegalArgumentException se {@code capacity} non e' positiva o {@code stored} e' negativo
     */
    public static int comparator(boolean hasCell, int stored, int capacity) {
        int level = scaled(15, stored, capacity);
        return hasCell ? level : 0;
    }

    /** {@code stored} riportato in [0, {@code capacity}], in parti su {@code steps}, per difetto; prodotto in long. */
    private static int scaled(int steps, int stored, int capacity) {
        requirePositive("capacity", capacity);
        requireNonNegative("stored", stored);
        return (int) ((long) steps * Math.min(stored, capacity) / capacity);
    }

    private static void requirePositive(String name, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be > 0: " + value);
        }
    }

    private static void requireNonNegative(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0: " + value);
        }
    }
}
