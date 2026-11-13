package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.List;
import java.util.Objects;

/**
 * Nomi delle ossa della geometria della tuta ({@code geo/exoframe.geo.json}): e' l'unico posto in cui il codice li
 * costruisce, e i test li confrontano con il file generato. Codice comune senza dipendenze client.
 * <p>
 * Convenzioni (design §5.1): sei ossa di primo livello {@code armor<Parte>} con i perni del modello armatura di
 * GeckoLib; sotto ciascuna il telaio nudo {@code frame<Parte>} e, per il pezzo dello slot, quattro copie per stato
 * visivo (0 integro, 1 danneggiato, 2 critico, 3 rotto) divise in tre regioni di colore:
 * {@code piece<Slot>_s<stato>_p1} (primaria), {@code _p2} (secondaria) e {@code _n} (neutra, non colorata). Le ossa
 * {@code piece<Slot>_decal} sono riservate alle decalcomanie.
 */
public final class SuitBones {
    public static final String ARMOR_HEAD = "armorHead";
    public static final String ARMOR_BODY = "armorBody";
    public static final String ARMOR_RIGHT_ARM = "armorRightArm";
    public static final String ARMOR_LEFT_ARM = "armorLeftArm";
    public static final String ARMOR_RIGHT_LEG = "armorRightLeg";
    public static final String ARMOR_LEFT_LEG = "armorLeftLeg";

    /** Le sei ossa di primo livello, nell'ordine di {@link #PARTS}. */
    public static final List<String> TOP_LEVEL = List.of(ARMOR_HEAD, ARMOR_BODY, ARMOR_RIGHT_ARM, ARMOR_LEFT_ARM,
            ARMOR_RIGHT_LEG, ARMOR_LEFT_LEG);

    /** Le sei parti del telaio per {@link #frame(String)}; la parte i sta sotto l'osso i di {@link #TOP_LEVEL}. */
    public static final List<String> PARTS = List.of("Head", "Body", "RightArm", "LeftArm", "RightLeg", "LeftLeg");

    /** Regione primaria di un pezzo, colorata con il colore primario. */
    public static final String PRIMARY = "p1";
    /** Regione secondaria di un pezzo, colorata con il colore secondario. */
    public static final String SECONDARY = "p2";
    /** Regione neutra di un pezzo (metallo, cavi): non si colora. */
    public static final String NEUTRAL = "n";
    /** Le tre regioni di un pezzo. */
    public static final List<String> REGIONS = List.of(PRIMARY, SECONDARY, NEUTRAL);

    /** Numero di stati visivi di un pezzo: gli stati vanno da 0 a {@code STATES - 1}. */
    public static final int STATES = 4;

    /** Gli slot che hanno un osso di decalcomania riservato (design §5.1). */
    public static final List<PieceSlot> DECAL_SLOTS = List.of(PieceSlot.TORSO, PieceSlot.LEFT_ARM);

    private SuitBones() {
    }

    /**
     * L'osso di una regione del pezzo di uno slot in uno stato visivo, per esempio
     * {@code piece(LEFT_ARM, 2, "p1")} = {@code pieceLeftArm_s2_p1}.
     *
     * @throws IllegalArgumentException se lo stato non e' in [0, 3] o la regione non e' una di {@link #REGIONS}
     */
    public static String piece(PieceSlot slot, int state, String region) {
        String name = slotName(slot);
        if (state < 0 || state >= STATES) {
            throw new IllegalArgumentException("piece state must be in [0, " + (STATES - 1) + "]: " + state);
        }
        if (region == null || !REGIONS.contains(region)) { // List.of(...).contains(null) lancerebbe NPE
            throw new IllegalArgumentException("unknown piece region: " + region + " (expected one of " + REGIONS + ")");
        }
        return "piece" + name + "_s" + state + "_" + region;
    }

    /**
     * L'osso del telaio nudo di una parte, per esempio {@code frame("Head")} = {@code frameHead}.
     *
     * @throws IllegalArgumentException se la parte non e' una di {@link #PARTS}
     */
    public static String frame(String part) {
        if (part == null || !PARTS.contains(part)) {
            throw new IllegalArgumentException("unknown frame part: " + part + " (expected one of " + PARTS + ")");
        }
        return "frame" + part;
    }

    /** L'osso riservato alla decalcomania del pezzo di uno slot, per esempio {@code pieceTorso_decal}. */
    public static String decal(PieceSlot slot) {
        return "piece" + slotName(slot) + "_decal";
    }

    private static String slotName(PieceSlot slot) {
        Objects.requireNonNull(slot, "slot must not be null");
        return switch (slot) {
            case HELMET -> "Helmet";
            case TORSO -> "Torso";
            case LEFT_ARM -> "LeftArm";
            case RIGHT_ARM -> "RightArm";
            case LEFT_LEG -> "LeftLeg";
            case RIGHT_LEG -> "RightLeg";
        };
    }
}
