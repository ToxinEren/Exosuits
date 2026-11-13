package com.toxineren.exoframes.core.armor;

import com.toxineren.exoframes.core.config.ExoframesConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ripartisce un danno intero tra i pezzi montati. Calcolo puro: non modifica i pezzi, il chiamante
 * applica la ripartizione restituita (per esempio con {@link Piece#damage(int)}).
 *
 * <p>La quota primaria ({@code primaryShare}) va in parti uguali ai pezzi primari del tipo di danno
 * che sono montati e non rotti; il resto va agli altri pezzi montati e non rotti in proporzione a
 * {@link PieceSlot#protectionShare()}. Se nessun primario puo' assorbire, la quota primaria si somma al
 * resto; se puo' assorbire solo qualche primario, il resto va ai primari. I pezzi rotti o assenti non
 * ricevono nulla. Gli interi si ottengono con il metodo dei resti maggiori (a parita' di resto vince il
 * pezzo che viene prima in {@link PieceSlot}), cosi' la somma e' esattamente {@code amount}.
 */
public final class DamageRouter {

    /** Tolleranza per non perdere un'unita' quando una quota esatta esce come 29.999999999. */
    private static final double EPSILON = 1e-9;

    /**
     * Esito della ripartizione. {@code perPiece} contiene solo i pezzi che ricevono un danno &gt; 0;
     * {@code unabsorbed} e' il danno che nessun pezzo puo' assorbire (tutto, se nessun pezzo e' utilizzabile).
     */
    public record Split(Map<PieceSlot, Integer> perPiece, int unabsorbed) {
        public Split {
            perPiece = Collections.unmodifiableMap(new EnumMap<>(perPiece));
        }
    }

    private final double primaryShare;

    /** Router con la quota primaria della sezione danno della config ({@code damage.primaryShare}). */
    public DamageRouter(ExoframesConfig.Damage damage) {
        this(damage.primaryShare);
    }

    public DamageRouter(double primaryShare) {
        if (!(primaryShare >= 0.0 && primaryShare <= 1.0)) {
            throw new IllegalArgumentException("primaryShare must be in [0, 1]: " + primaryShare);
        }
        this.primaryShare = primaryShare;
    }

    public Split route(DamageKind kind, int amount, Map<PieceSlot, Piece> mounted) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0: " + amount);
        }
        Set<PieceSlot> primaries = primariesOf(kind);
        List<PieceSlot> primary = new ArrayList<>();
        List<PieceSlot> others = new ArrayList<>();
        for (PieceSlot slot : PieceSlot.values()) {
            Piece piece = mounted.get(slot);
            if (piece == null || piece.isBroken()) {
                continue;
            }
            (primaries.contains(slot) ? primary : others).add(slot);
        }
        Map<PieceSlot, Integer> perPiece = new EnumMap<>(PieceSlot.class);
        if (primary.isEmpty() && others.isEmpty()) {
            return new Split(perPiece, amount);
        }

        double primaryPart = primary.isEmpty() ? 0.0 : (others.isEmpty() ? amount : amount * primaryShare);
        double otherPart = amount - primaryPart;
        double otherShares = 0.0;
        for (PieceSlot slot : others) {
            otherShares += slot.protectionShare();
        }

        // Quote esatte, nell'ordine di dichiarazione di PieceSlot.
        List<PieceSlot> slots = new ArrayList<>();
        List<Double> quotas = new ArrayList<>();
        for (PieceSlot slot : PieceSlot.values()) {
            if (primary.contains(slot)) {
                slots.add(slot);
                quotas.add(primaryPart / primary.size());
            } else if (others.contains(slot)) {
                slots.add(slot);
                quotas.add(otherPart * slot.protectionShare() / otherShares);
            }
        }

        int[] shares = new int[slots.size()];
        double[] remainders = new double[slots.size()];
        int assigned = 0;
        for (int i = 0; i < slots.size(); i++) {
            double quota = quotas.get(i);
            shares[i] = (int) Math.floor(quota + EPSILON);
            remainders[i] = quota - shares[i];
            assigned += shares[i];
        }
        // Resti maggiori: le unita' mancanti vanno ai resti piu' grandi; a parita' vince l'indice minore.
        for (int left = amount - assigned; left > 0; left--) {
            int best = 0;
            for (int i = 1; i < remainders.length; i++) {
                if (remainders[i] > remainders[best] + EPSILON) {
                    best = i;
                }
            }
            shares[best]++;
            remainders[best] = Double.NEGATIVE_INFINITY;
        }

        for (int i = 0; i < slots.size(); i++) {
            if (shares[i] > 0) {
                perPiece.put(slots.get(i), shares[i]);
            }
        }
        return new Split(perPiece, 0);
    }

    private static Set<PieceSlot> primariesOf(DamageKind kind) {
        return switch (kind) {
            case HEAD -> EnumSet.of(PieceSlot.HELMET);
            case FROM_ABOVE -> EnumSet.of(PieceSlot.HELMET, PieceSlot.TORSO);
            case FALL -> EnumSet.of(PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG);
            case GENERIC -> EnumSet.noneOf(PieceSlot.class);
        };
    }
}
