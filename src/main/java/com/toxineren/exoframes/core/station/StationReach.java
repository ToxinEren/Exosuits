package com.toxineren.exoframes.core.station;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Dove deve stare un telaio perche' la stazione di manutenzione lo trovi (design M4 §5.2), aritmetica pura.
 * L'impronta della stazione ({@link Footprint}) e' il rettangolo orizzontale dei suoi due blocchi; un telaio, con i
 * piedi nel punto (x, y, z), e' a portata ({@link #inReach}) se la sua distanza orizzontale (euclidea) da quel
 * rettangolo ({@link #distance}) e' al piu' {@code reachBlocks}, zero se i piedi stanno sopra l'impronta, e se i piedi
 * stanno fra {@value #BELOW} blocco sotto e {@value #ABOVE} sopra la base della stazione: sul pavimento accanto, un
 * gradino piu' in basso, o sul piano della stazione. Fra piu' telai a portata la stazione prende il piu' vicino
 * ({@link #nearest}). Che il telaio stia nella stessa dimensione e che il giocatore lo possa usare lo controlla chi
 * chiama.
 * <p>
 * Gli estremi sono compresi. I piedi di un telaio posato su un blocco pieno stanno a un'altezza intera, quindi i due
 * bordi della finestra d'altezza sono proprio le altezze di un telaio un gradino piu' in basso e di uno due blocchi
 * piu' in alto: con gli estremi esclusi quei telai resterebbero fuori. Lo stesso per la distanza: "entro 2 blocchi"
 * comprende i 2 blocchi, come i limiti della config ({@code station.reachBlocks} da 0,5 a 8,0, compresi). I confronti
 * sono fatti sui numeri che il chiamante passa, senza tolleranza: un telaio a 2,0001 blocchi e' fuori.
 */
public final class StationReach {
    /** Quanti blocchi sotto la base della stazione possono stare i piedi del telaio. */
    public static final double BELOW = 1.0;
    /** Quanti blocchi sopra la base della stazione possono stare i piedi del telaio. */
    public static final double ABOVE = 2.0;

    private StationReach() {
    }

    /**
     * L'impronta della stazione: i blocchi da {@code minX} a {@code maxX} e da {@code minZ} a {@code maxZ}, compresi,
     * quindi il rettangolo orizzontale [{@code minX}, {@code maxX} + 1] x [{@code minZ}, {@code maxZ} + 1], con la base
     * (la y dei due blocchi) {@code baseY}.
     */
    public record Footprint(int minX, int minZ, int maxX, int maxZ, int baseY) {
    }

    /**
     * Un telaio fra cui scegliere: la sua identita' e i suoi piedi.
     *
     * @throws NullPointerException se {@code frameId} e' null
     */
    public record Candidate(UUID frameId, double x, double y, double z) {
        public Candidate {
            Objects.requireNonNull(frameId, "frameId must not be null");
        }
    }

    /**
     * L'impronta della stazione con la meta' principale nel blocco ({@code mainX}, {@code mainY}, {@code mainZ}) e la
     * laterale nel blocco ({@code sideX}, {@code mainY}, {@code sideZ}). Non conta quale delle due e' la principale.
     *
     * @throws IllegalArgumentException se le due meta' non sono affiancate: esattamente una fra x e z deve differire,
     *         di 1
     */
    public static Footprint of(int mainX, int mainY, int mainZ, int sideX, int sideZ) {
        int dx = Math.abs(sideX - mainX);
        int dz = Math.abs(sideZ - mainZ);
        if (dx + dz != 1) {
            throw new IllegalArgumentException("station halves at (" + mainX + ", " + mainZ + ") and (" + sideX + ", "
                    + sideZ + ") are not side by side");
        }
        return new Footprint(Math.min(mainX, sideX), Math.min(mainZ, sideZ), Math.max(mainX, sideX),
                Math.max(mainZ, sideZ), mainY);
    }

    /**
     * La distanza orizzontale euclidea del punto (x, z) dal rettangolo dell'impronta: 0 dentro e sul bordo.
     */
    public static double distance(Footprint footprint, double x, double z) {
        double dx = Math.max(Math.max(footprint.minX() - x, x - (footprint.maxX() + 1)), 0.0);
        double dz = Math.max(Math.max(footprint.minZ() - z, z - (footprint.maxZ() + 1)), 0.0);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Vero se e solo se i piedi in (x, y, z) sono a portata della stazione: {@link #distance} al piu'
     * {@code reachBlocks} e {@code baseY - BELOW <= y <= baseY + ABOVE}, estremi compresi.
     */
    public static boolean inReach(Footprint footprint, double x, double y, double z, double reachBlocks) {
        return distance(footprint, x, z) <= reachBlocks
                && y >= footprint.baseY() - BELOW && y <= footprint.baseY() + ABOVE;
    }

    /**
     * Il telaio piu' vicino all'impronta (la {@link #distance} minima); a parita', il piu' vicino in orizzontale al
     * centro del rettangolo; poi quello con il {@code frameId} minore in forma testuale ({@code toString()}). La scelta
     * e' deterministica e non dipende dall'ordine dei candidati: un secondo telaio si sceglie spostando il primo. Vuoto
     * senza candidati.
     */
    public static Optional<Candidate> nearest(Footprint footprint, Collection<Candidate> candidates) {
        Objects.requireNonNull(footprint, "footprint must not be null");
        double centreX = (footprint.minX() + footprint.maxX() + 1) / 2.0;
        double centreZ = (footprint.minZ() + footprint.maxZ() + 1) / 2.0;
        Comparator<Candidate> nearestFirst = Comparator
                .comparingDouble((Candidate c) -> distance(footprint, c.x(), c.z()))
                .thenComparingDouble(c -> Math.hypot(c.x() - centreX, c.z() - centreZ))
                .thenComparing(c -> c.frameId().toString());
        return candidates.stream().min(nearestFirst);
    }
}
