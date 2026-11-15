package com.toxineren.exoframes.core.energy;

/**
 * L'attivita' del giocatore, tick per tick, dalla distanza orizzontale percorsa (design §12.3). Un'istanza per
 * giocatore: tiene la finestra degli ultimi {@link #WINDOW} campioni, corrente compreso.
 * <p>
 * La finestra c'e' perche' in un tick del server arrivano da 0 a 2 pacchetti di movimento: guardando un tick solo, un
 * giocatore che cammina risulterebbe spesso fermo. Un tick e' "in movimento" se almeno un campione della finestra
 * supera {@link #MOVING} (ciascuno da solo, non la somma). Si guarda solo l'orizzontale: chi sale una scala a pioli
 * conta come fermo (accettato). Un passeggero, e piu' di {@link #TELEPORT} blocchi in un tick (un teletrasporto),
 * contano come fermi e fanno ripartire la finestra; la fa ripartire anche chi chiama, con {@link #reset()}, al cambio
 * di dimensione e quando l'orologio torna indietro.
 * <p>
 * Le costanti stanno qui e non nella config: descrivono il modo in cui il server riceve il movimento, non il bilancio
 * della mod.
 */
public final class ActivityRule {
    /** Quanti campioni tiene la finestra, corrente compreso. */
    public static final int WINDOW = 4;

    /** Blocchi in un tick oltre i quali (strettamente) un campione e' in movimento. */
    public static final double MOVING = 0.01;

    /** Blocchi in un tick oltre i quali (strettamente) lo spostamento e' un teletrasporto. */
    public static final double TELEPORT = 8.0;

    private final double[] samples = new double[WINDOW];
    private int next;
    private int count;

    /**
     * Aggiunge il campione del tick e restituisce l'attivita' del tick.
     *
     * @param horizontalDistance la distanza orizzontale fra la posizione attuale e quella del tick prima
     * @param sprinting          se il giocatore corre
     * @param passenger          se il giocatore era passeggero di qualcosa in questo tick
     * @return {@link Activity#IDLE} per un passeggero o un teletrasporto (e la finestra riparte); altrimenti
     *         {@link Activity#SPRINT} o {@link Activity#WALK} se la finestra e' in movimento, {@link Activity#IDLE}
     *         se no
     */
    public Activity next(double horizontalDistance, boolean sprinting, boolean passenger) {
        if (passenger || horizontalDistance > TELEPORT) {
            reset();
            return Activity.IDLE;
        }
        samples[next] = horizontalDistance;
        next = (next + 1) % WINDOW;
        if (count < WINDOW) {
            count++;
        }
        boolean moving = false;
        for (int i = 0; i < count; i++) {
            if (samples[i] > MOVING) {
                moving = true;
                break;
            }
        }
        if (!moving) {
            return Activity.IDLE;
        }
        return sprinting ? Activity.SPRINT : Activity.WALK;
    }

    /** Svuota la finestra: il prossimo tick guarda solo il suo campione. */
    public void reset() {
        next = 0;
        count = 0;
    }
}
