package com.toxineren.exoframes.compat.client;

import com.toxineren.exoframes.compat.SuitBones;
import com.toxineren.exoframes.compat.client.SuitView.PieceView;
import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Le decisioni del renderer della tuta sulle ossa della geometria, in Java puro (nessun tipo di Minecraft o di
 * GeckoLib), cosi' i test le provano senza il gioco; {@code SuitRendererImpl} le applica e non ne tiene copie.
 * <p>
 * Le ossa di primo livello e del telaio sono sempre visibili. Delle ossa di un pezzo si vedono solo le tre dello stato
 * visivo del pezzo montato nel loro slot; per uno slot senza pezzo nessuna. Le ossa delle decalcomanie restano nascoste
 * (design §5.1). Si colorano solo le regioni primaria ({@code _p1}) e secondaria ({@code _p2}) delle ossa dei pezzi,
 * con i colori del pezzo del loro slot; la regione neutra ({@code _n}) e ogni altro osso restano bianchi.
 */
public final class SuitBoneRules {
    /** La regione di colore di un osso. */
    public enum Region {
        /** Colorata con il colore primario del pezzo. */
        PRIMARY,
        /** Colorata con il colore secondario del pezzo. */
        SECONDARY,
        /** Non colorata. */
        NEUTRAL
    }

    /** Il colore RGB di un osso non colorato. */
    public static final int WHITE = 0xFFFFFF;

    /**
     * Le ossa di cui le regole decidono la visibilita', ognuna una volta: prima le sei di primo livello e le sei del
     * telaio, poi quelle dei pezzi per slot, stato e regione, infine le decalcomanie. L'ordine conta per chi le
     * applica: in GeckoLib mostrare o nascondere un osso fa lo stesso con i figli, quindi le ossa che contengono le
     * altre vanno decise per prime.
     */
    public static final List<String> BONES;

    /** Che cosa e' ogni osso di {@link #BONES}. */
    private static final Map<String, Bone> KINDS;

    static {
        List<String> names = new ArrayList<>();
        Map<String, Bone> kinds = new HashMap<>();
        for (String name : SuitBones.TOP_LEVEL) {
            names.add(name);
            kinds.put(name, Bone.ALWAYS_VISIBLE);
        }
        for (String part : SuitBones.PARTS) {
            String name = SuitBones.frame(part);
            names.add(name);
            kinds.put(name, Bone.ALWAYS_VISIBLE);
        }
        for (PieceSlot slot : PieceSlot.values()) {
            for (int state = 0; state < SuitBones.STATES; state++) {
                for (String region : SuitBones.REGIONS) {
                    String name = SuitBones.piece(slot, state, region);
                    names.add(name);
                    kinds.put(name, new Bone(Kind.PIECE, slot, state, regionOf(region)));
                }
            }
        }
        for (PieceSlot slot : SuitBones.DECAL_SLOTS) {
            String name = SuitBones.decal(slot);
            names.add(name);
            kinds.put(name, Bone.HIDDEN);
        }
        BONES = List.copyOf(names);
        KINDS = Map.copyOf(kinds);
    }

    private SuitBoneRules() {
    }

    /**
     * True se l'osso si vede nella vista data.
     *
     * @throws IllegalArgumentException se l'osso non e' uno di {@link #BONES}
     */
    public static boolean visible(String bone, SuitView view) {
        Objects.requireNonNull(view, "view must not be null");
        Bone entry = KINDS.get(bone);
        if (entry == null) {
            throw new IllegalArgumentException("unknown suit bone: " + bone);
        }
        return switch (entry.kind()) {
            case ALWAYS_VISIBLE -> true;
            case HIDDEN -> false;
            case PIECE -> {
                PieceView piece = view.pieces().get(entry.slot());
                yield piece != null && piece.state() == entry.state();
            }
        };
    }

    /**
     * La regione di colore dell'osso: per le ossa dei pezzi quella del suffisso ({@code _p1}, {@code _p2}, {@code _n});
     * per ogni altro osso, anche fuori da {@link #BONES}, {@link Region#NEUTRAL}.
     */
    public static Region region(String bone) {
        Bone entry = KINDS.get(bone);
        return entry == null ? Region.NEUTRAL : entry.region();
    }

    /**
     * Il colore RGB dell'osso nella vista data: per una regione primaria o secondaria il colore corrispondente del pezzo
     * montato nel suo slot; {@link #WHITE} per le ossa neutre e per quelle di uno slot senza pezzo (che sono nascoste).
     */
    public static int rgb(String bone, SuitView view) {
        Objects.requireNonNull(view, "view must not be null");
        Bone entry = KINDS.get(bone);
        if (entry == null || entry.region() == Region.NEUTRAL) {
            return WHITE;
        }
        PieceView piece = view.pieces().get(entry.slot());
        if (piece == null) {
            return WHITE;
        }
        return entry.region() == Region.PRIMARY ? piece.primaryRgb() : piece.secondaryRgb();
    }

    private static Region regionOf(String suffix) {
        return switch (suffix) {
            case SuitBones.PRIMARY -> Region.PRIMARY;
            case SuitBones.SECONDARY -> Region.SECONDARY;
            default -> Region.NEUTRAL;
        };
    }

    /** Che cosa decide la visibilita' di un osso. */
    private enum Kind {
        /** Ossa di primo livello e del telaio. */
        ALWAYS_VISIBLE,
        /** Decalcomanie. */
        HIDDEN,
        /** Ossa dei pezzi: dipende dal pezzo montato nello slot. */
        PIECE
    }

    /** Un osso: il tipo e, per le ossa dei pezzi, slot, stato e regione (null e -1 per gli altri). */
    private record Bone(Kind kind, PieceSlot slot, int state, Region region) {
        static final Bone ALWAYS_VISIBLE = new Bone(Kind.ALWAYS_VISIBLE, null, -1, Region.NEUTRAL);
        static final Bone HIDDEN = new Bone(Kind.HIDDEN, null, -1, Region.NEUTRAL);
    }
}
