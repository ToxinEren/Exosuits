package com.toxineren.exoframes.core.armor;

import com.toxineren.exoframes.core.config.ExoframesConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Ripartisce un danno intero tra i pezzi montati. Calcolo puro: non modifica i pezzi, il chiamante
 * applica la ripartizione restituita (per esempio con {@link Piece#damage(int)}).
 *
 * <p>La quota primaria ({@code primaryShare}) va in parti uguali ai pezzi primari del tipo di danno
 * che sono montati e non rotti; il resto va agli altri pezzi montati e non rotti in proporzione a
 * {@code pieces.shares} della config (una posizione assente dalla mappa vale 0; se le quote dei
 * destinatari sommano a 0 il resto si divide in parti uguali). Se nessun primario puo' assorbire, la quota primaria si somma al
 * resto; se puo' assorbire solo qualche primario, il resto va ai primari. I pezzi rotti o assenti non
 * ricevono nulla. Gli interi si ottengono con il metodo dei resti maggiori (a parita' di resto vince il
 * pezzo che viene prima in {@link PieceSlot}), cosi' la somma e' esattamente {@code amount}.
 *
 * <p>La variante con riporto ({@link #route(DamageKind, int, Map, Map)}), quella usata in gioco, conserva per slot
 * le frazioni di punto in millesimi. Invarianti:
 * <ul>
 *   <li>l'usura non e' mai negativa e i riporti restano in [0, 999]; un colpo da 0 non cambia nulla;</li>
 *   <li>per ogni slot l'usura cumulata e' floor(somma delle quote in millesimi di ogni colpo / 1000), dove la
 *       quota in millesimi di un colpo e' arrotondata con i resti maggiori: la garanzia vale rispetto a queste
 *       quote intere in millesimi, non rispetto alla quota reale;</li>
 *   <li>l'usura totale cumulata e' in (danno totale - 6, danno totale]: il telaio non si usura mai piu' del danno
 *       ricevuto e la parte in sospeso e' al massimo 999 millesimi per slot;</li>
 *   <li>l'usura di un singolo colpo puo' differire dal suo danno, perche' le frazioni in sospeso si realizzano o
 *       si rimandano, ma non vengono mai inventate.</li>
 * </ul>
 */
public final class DamageRouter {

    /** Tolleranza per non perdere un'unita' quando una quota esatta esce come 29.999999999. */
    private static final double EPSILON = 1e-9;

    /** Millesimi di punto per punto di danno: l'unita' del riporto. */
    private static final int MILLI = 1000;

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
    private final Map<PieceSlot, Double> shares;

    /**
     * Router costruito dalla config: quota primaria da {@code damage.primaryShare}, quote dei pezzi da
     * {@code pieces.shares} (copiate alla costruzione).
     *
     * @throws IllegalArgumentException se la quota primaria non e' in [0, 1] o una quota e' negativa o non finita
     */
    public DamageRouter(ExoframesConfig.Damage damage, ExoframesConfig.Pieces pieces) {
        double primary = Objects.requireNonNull(damage, "damage").primaryShare;
        if (!(primary >= 0.0 && primary <= 1.0)) {
            throw new IllegalArgumentException("primaryShare must be in [0, 1]: " + primary);
        }
        Map<PieceSlot, Double> configured = Objects.requireNonNull(Objects.requireNonNull(pieces, "pieces").shares, "shares");
        Map<PieceSlot, Double> copy = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            Double share = configured.get(slot);
            double value = share == null ? 0.0 : share;
            if (!(Double.isFinite(value) && value >= 0.0)) {
                throw new IllegalArgumentException("share of " + slot + " must be finite and >= 0: " + share);
            }
            copy.put(slot, value);
        }
        this.primaryShare = primary;
        this.shares = copy;
    }

    /**
     * Ripartizione senza memoria di un singolo colpo: resti maggiori sulle quote esatte, con somma esattamente
     * {@code amount} (se qualche pezzo puo' assorbire). Il codice di gioco usa
     * {@link #route(DamageKind, int, Map, Map)}, che conserva le frazioni tra un colpo e l'altro.
     */
    public Split route(DamageKind kind, int amount, Map<PieceSlot, Piece> mounted) {
        requireNonNegative(amount);
        Quotas q = quotas(kind, amount, mounted);
        if (q == null) {
            return new Split(new EnumMap<>(PieceSlot.class), amount);
        }
        long[] whole = largestRemainder(q.values, amount);
        Map<PieceSlot, Integer> perPiece = new EnumMap<>(PieceSlot.class);
        for (int i = 0; i < q.slots.size(); i++) {
            if (whole[i] > 0) {
                perPiece.put(q.slots.get(i), Math.toIntExact(whole[i]));
            }
        }
        return new Split(perPiece, 0);
    }

    /**
     * Ripartisce il danno conservando per slot il riporto frazionario, in millesimi di punto, e lo aggiorna.
     * Le quote esatte dei destinatari in millesimi (resti maggiori, somma {@code amount x 1000}) si sommano al
     * riporto: {@code v = riporto + quota}; l'usura e' {@code v / 1000} e il nuovo riporto {@code v % 1000}.
     * Gli slot che non ricevono danno (assenti o rotti) restano invariati. Vedi gli invarianti nella
     * descrizione della classe.
     *
     * <p>Precondizione: ogni valore del riporto e' in [0, 999]; uno slot assente dalla mappa, o con valore
     * {@code null}, vale 0.
     */
    public Split route(DamageKind kind, int amount, Map<PieceSlot, Piece> mounted, Map<PieceSlot, Integer> carry) {
        Objects.requireNonNull(carry, "carry");
        requireNonNegative(amount);
        Quotas q = quotas(kind, amount, mounted);
        if (q == null) {
            return new Split(new EnumMap<>(PieceSlot.class), amount);
        }
        double[] milliQuotas = new double[q.values.length];
        for (int i = 0; i < milliQuotas.length; i++) {
            milliQuotas[i] = q.values[i] * MILLI;
        }
        long[] milli = largestRemainder(milliQuotas, (long) amount * MILLI);
        Map<PieceSlot, Integer> perPiece = new EnumMap<>(PieceSlot.class);
        for (int i = 0; i < q.slots.size(); i++) {
            PieceSlot slot = q.slots.get(i);
            Integer previous = carry.get(slot);
            long v = (previous == null ? 0 : previous) + milli[i];
            carry.put(slot, (int) (v % MILLI));
            if (v >= MILLI) {
                perPiece.put(slot, Math.toIntExact(v / MILLI));
            }
        }
        return new Split(perPiece, 0);
    }

    /** Slot destinatari, nell'ordine di {@link PieceSlot}, con le loro quote esatte in punti. */
    private record Quotas(List<PieceSlot> slots, double[] values) {}

    /** Quote esatte dei pezzi montati e non rotti; null se nessun pezzo puo' assorbire. */
    private Quotas quotas(DamageKind kind, int amount, Map<PieceSlot, Piece> mounted) {
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
        if (primary.isEmpty() && others.isEmpty()) {
            return null;
        }

        double primaryPart = primary.isEmpty() ? 0.0 : (others.isEmpty() ? amount : amount * primaryShare);
        double otherPart = amount - primaryPart;
        double otherShares = 0.0;
        for (PieceSlot slot : others) {
            otherShares += shares.get(slot);
        }

        List<PieceSlot> slots = new ArrayList<>();
        double[] values = new double[primary.size() + others.size()];
        for (PieceSlot slot : PieceSlot.values()) {
            if (primary.contains(slot)) {
                values[slots.size()] = primaryPart / primary.size();
                slots.add(slot);
            } else if (others.contains(slot)) {
                values[slots.size()] = otherShares > 0.0
                        ? otherPart * shares.get(slot) / otherShares
                        : otherPart / others.size();
                slots.add(slot);
            }
        }
        return new Quotas(slots, values);
    }

    /**
     * Interi che sommano a {@code total}: la parte intera di ogni quota, poi le unita' mancanti ai resti piu'
     * grandi (a parita' vince l'indice minore).
     */
    private static long[] largestRemainder(double[] quotas, long total) {
        int n = quotas.length;
        long[] result = new long[n];
        double[] remainders = new double[n];
        long assigned = 0;
        for (int i = 0; i < n; i++) {
            result[i] = (long) Math.floor(quotas[i] + EPSILON);
            remainders[i] = quotas[i] - result[i];
            assigned += result[i];
        }
        for (long left = total - assigned; left > 0; left--) {
            int best = 0;
            for (int i = 1; i < n; i++) {
                if (remainders[i] > remainders[best] + EPSILON) {
                    best = i;
                }
            }
            result[best]++;
            remainders[best] = Double.NEGATIVE_INFINITY;
        }
        return result;
    }

    private static void requireNonNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0: " + amount);
        }
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
