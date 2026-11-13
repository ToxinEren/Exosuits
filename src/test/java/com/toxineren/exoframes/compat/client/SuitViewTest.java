package com.toxineren.exoframes.compat.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.SuitSummary.PieceLook;
import com.toxineren.exoframes.compat.client.SuitView.PieceView;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

/** Vista del renderer ricavata dal riassunto visivo: stati per posizione e colori risolti. */
class SuitViewTest {

    /** Colori predefiniti finti, diversi per livello e per regione. */
    private static final ToIntFunction<Tier> PRIMARY = tier -> 0x100000 * tier.level();
    private static final ToIntFunction<Tier> SECONDARY = tier -> 0x000100 * tier.level();

    @Test
    void unpaintedResolvesToTierDefault() {
        SuitSummary summary = new SuitSummary(Map.of(
                PieceSlot.HELMET, new PieceLook(Tier.SCRAP, PieceCondition.INTACT, Appearance.DEFAULT),
                PieceSlot.TORSO, new PieceLook(Tier.ARMORED, PieceCondition.INTACT,
                        new Appearance(0x123456, Appearance.UNPAINTED, List.of())),
                PieceSlot.LEFT_ARM, new PieceLook(Tier.INDUSTRIAL, PieceCondition.INTACT,
                        new Appearance(Appearance.UNPAINTED, 0, List.of()))));

        SuitView view = SuitView.of(summary, PRIMARY, SECONDARY);

        assertEquals(new PieceView(0, 0x100000, 0x000100), view.pieces().get(PieceSlot.HELMET));
        assertEquals(new PieceView(0, 0x123456, 0x000300), view.pieces().get(PieceSlot.TORSO));
        assertEquals(new PieceView(0, 0x200000, 0), view.pieces().get(PieceSlot.LEFT_ARM));
        assertEquals(3, view.pieces().size());
    }

    @Test
    void conditionMapsToState() {
        Map<PieceSlot, PieceLook> pieces = new EnumMap<>(PieceSlot.class);
        pieces.put(PieceSlot.HELMET, new PieceLook(Tier.SCRAP, PieceCondition.INTACT, Appearance.DEFAULT));
        pieces.put(PieceSlot.TORSO, new PieceLook(Tier.SCRAP, PieceCondition.DAMAGED, Appearance.DEFAULT));
        pieces.put(PieceSlot.LEFT_LEG, new PieceLook(Tier.SCRAP, PieceCondition.CRITICAL, Appearance.DEFAULT));
        pieces.put(PieceSlot.RIGHT_LEG, new PieceLook(Tier.SCRAP, PieceCondition.BROKEN, Appearance.DEFAULT));

        SuitView view = SuitView.of(new SuitSummary(pieces), PRIMARY, SECONDARY);

        assertEquals(0, view.pieces().get(PieceSlot.HELMET).state());
        assertEquals(1, view.pieces().get(PieceSlot.TORSO).state());
        assertEquals(2, view.pieces().get(PieceSlot.LEFT_LEG).state());
        assertEquals(3, view.pieces().get(PieceSlot.RIGHT_LEG).state());
        // ogni condizione ha il suo stato, nell'ordine dell'enum
        for (PieceCondition condition : PieceCondition.values()) {
            SuitView single = SuitView.of(new SuitSummary(Map.of(PieceSlot.TORSO,
                    new PieceLook(Tier.SCRAP, condition, Appearance.DEFAULT))), PRIMARY, SECONDARY);
            assertEquals(condition.ordinal(), single.pieces().get(PieceSlot.TORSO).state());
        }
    }

    @Test
    void bareSummaryGivesEmptyView() {
        SuitView view = SuitView.of(SuitSummary.BARE, PRIMARY, SECONDARY);
        assertTrue(view.pieces().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> view.pieces().put(PieceSlot.HELMET, new PieceView(0, 0, 0)));
    }

    @Test
    void pieceViewKeepsOnlyRgbAndRejectsUnknownState() {
        // colori con bit di alfa (per esempio un predefinito scritto in ARGB): restano solo i 24 bit RGB, mai -1
        assertEquals(new PieceView(1, 0x336699, 0xFFFFFF), new PieceView(1, 0xFF336699, -1));
        SuitView view = SuitView.of(new SuitSummary(Map.of(PieceSlot.HELMET,
                new PieceLook(Tier.SCRAP, PieceCondition.INTACT, Appearance.DEFAULT))),
                tier -> 0x80ABCDEF, tier -> 0xFF000000);
        assertEquals(new PieceView(0, 0xABCDEF, 0), view.pieces().get(PieceSlot.HELMET));

        assertThrows(IllegalArgumentException.class, () -> new PieceView(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PieceView(4, 0, 0));
        assertThrows(NullPointerException.class, () -> new SuitView(null));
    }
}
