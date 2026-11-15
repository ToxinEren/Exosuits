package com.toxineren.exoframes.core.energy;

import java.util.Objects;

/**
 * La scelta della cella per il cambio dall'interno della tuta (design §12.10): la piu' carica fra le celle
 * dell'inventario che contengono piu' energia di quella installata (qualunque carica, purche' non vuota, se non ce n'e'
 * una installata); a parita' vince l'indice piu' basso. Deroga dichiarata a §5.6, che diceva "la prima cella carica":
 * cosi' un cambio porta sempre la cella migliore, e una cella piu' scarica dell'installata non si prende mai.
 * <p>
 * Gli indici seguono l'ordine dell'inventario: 0-35 sono gli slot 0-35 (barra degli oggetti 0-8, poi l'inventario
 * 9-35), {@link #OFFHAND} la mano secondaria (slot 40 dell'inventario), per ultima.
 */
public final class CellSwapRule {
    /** Le voci dell'array delle cariche: i 36 slot dell'inventario principale e la mano secondaria. */
    public static final int SLOTS = 37;

    /** L'indice della mano secondaria nell'array delle cariche. */
    public static final int OFFHAND = 36;

    private CellSwapRule() {
    }

    /**
     * L'indice della cella da mettere al posto di quella installata.
     *
     * @param installedStored la carica della cella installata, {@code -1} se non ce n'e' una
     * @param slotEnergies    {@value #SLOTS} voci: la carica della cella di ogni slot, gia' tagliata alla capacita',
     *                        {@code -1} se lo slot non ha una cella
     * @return l'indice della cella scelta, {@code -1} se nessuna ha piu' energia dell'installata
     * @throws NullPointerException     se {@code slotEnergies} e' null
     * @throws IllegalArgumentException se {@code slotEnergies} non ha {@value #SLOTS} voci
     */
    public static int pick(int installedStored, int[] slotEnergies) {
        Objects.requireNonNull(slotEnergies, "slotEnergies must not be null");
        if (slotEnergies.length != SLOTS) {
            throw new IllegalArgumentException("slotEnergies must have 37 entries: " + slotEnergies.length);
        }
        int best = -1;
        // senza cella installata (-1) vale qualunque carica sopra zero: una cella vuota non si prende mai
        int bestEnergy = Math.max(0, installedStored);
        for (int i = 0; i < SLOTS; i++) {
            int e = slotEnergies[i];
            // confronto stretto: a parita' resta l'indice piu' basso, gia' visto
            if (e >= 0 && e > bestEnergy) {
                best = i;
                bestEnergy = e;
            }
        }
        return best;
    }
}
