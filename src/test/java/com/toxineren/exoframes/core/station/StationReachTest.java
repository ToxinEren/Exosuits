package com.toxineren.exoframes.core.station;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * La portata della stazione (design M4 §5.2). La stazione dei casi ha la meta' principale in (10, 64, 20) e la laterale
 * in (9, 64, 20): il rettangolo dell'impronta e' x [9, 11], z [20, 21], con la base a 64 e il centro in (10, 20,5).
 */
class StationReachTest {

    private static final StationReach.Footprint STATION = StationReach.of(10, 64, 20, 9, 20);

    @Test
    void distanceIsZeroOnTheFootprint() {
        assertEquals(0.0, StationReach.distance(STATION, 9.0, 20.0));
        assertEquals(0.0, StationReach.distance(STATION, 10.99, 20.5));
        assertEquals(0.0, StationReach.distance(STATION, 11.0, 21.0));
    }

    @Test
    void distanceIsEuclideanFromTheRectangle() {
        assertEquals(2.0, StationReach.distance(STATION, 13.0, 20.5), 1e-9);
        assertEquals(1.5, StationReach.distance(STATION, 9.5, 18.5), 1e-9);
        assertEquals(Math.sqrt(5.0), StationReach.distance(STATION, 12.0, 23.0), 1e-9);
        assertEquals(Math.sqrt(5.0), StationReach.distance(STATION, 7.0, 22.0), 1e-9);
    }

    @Test
    void reachIncludesItsBorder() {
        assertTrue(StationReach.inReach(STATION, 13.0, 64.0, 20.5, 2.0));
        assertFalse(StationReach.inReach(STATION, 13.0001, 64.0, 20.5, 2.0));
        assertTrue(StationReach.inReach(STATION, 11.5, 64.0, 20.5, 0.5));
        assertFalse(StationReach.inReach(STATION, 11.5001, 64.0, 20.5, 0.5));
        assertTrue(StationReach.inReach(STATION, 19.0, 64.0, 20.5, 8.0));
        assertFalse(StationReach.inReach(STATION, 19.0001, 64.0, 20.5, 8.0));
    }

    @Test
    void heightWindowIsOneBelowToTwoAbove() {
        assertTrue(StationReach.inReach(STATION, 10.5, 63.0, 20.5, 2.0), "one block below the base");
        assertFalse(StationReach.inReach(STATION, 10.5, 62.9999, 20.5, 2.0), "under one block below the base");
        assertTrue(StationReach.inReach(STATION, 10.5, 65.0, 20.5, 2.0), "on the top of the station");
        assertTrue(StationReach.inReach(STATION, 10.5, 66.0, 20.5, 2.0), "two blocks above the base");
        assertFalse(StationReach.inReach(STATION, 10.5, 66.0001, 20.5, 2.0), "over two blocks above the base");
    }

    @Test
    void footprintIsTheSameFromEitherHalfInEveryDirection() {
        StationReach.Footprint west = new StationReach.Footprint(9, 20, 10, 20, 64);
        assertEquals(west, StationReach.of(10, 64, 20, 9, 20));
        assertEquals(west, StationReach.of(9, 64, 20, 10, 20));
        StationReach.Footprint east = new StationReach.Footprint(10, 20, 11, 20, 64);
        assertEquals(east, StationReach.of(10, 64, 20, 11, 20));
        assertEquals(east, StationReach.of(11, 64, 20, 10, 20));
        StationReach.Footprint south = new StationReach.Footprint(10, 20, 10, 21, 64);
        assertEquals(south, StationReach.of(10, 64, 20, 10, 21));
        assertEquals(south, StationReach.of(10, 64, 21, 10, 20));
        StationReach.Footprint north = new StationReach.Footprint(10, 19, 10, 20, 64);
        assertEquals(north, StationReach.of(10, 64, 20, 10, 19));
        assertEquals(north, StationReach.of(10, 64, 19, 10, 20));
    }

    @Test
    void nearestPrefersTheFootprintThenTheCentreThenTheId() {
        // A ha l'id minore: vincerebbe se la distanza dall'impronta non contasse. D ha l'id maggiore: vince solo per
        // la distanza dal centro.
        StationReach.Candidate a = new StationReach.Candidate(id("00000000-0000-0000-0000-000000000000"),
                13.0, 64.0, 20.5);
        StationReach.Candidate b = new StationReach.Candidate(id("00000000-0000-0000-0000-000000000002"),
                10.5, 65.0, 20.5);
        StationReach.Candidate c = new StationReach.Candidate(id("00000000-0000-0000-0000-000000000001"),
                9.5, 65.0, 20.5);
        List<StationReach.Candidate> candidates = new ArrayList<>(List.of(a, b, c));
        assertEquals(Optional.of(c), StationReach.nearest(STATION, candidates));

        StationReach.Candidate d = new StationReach.Candidate(id("00000000-0000-0000-0000-000000000009"),
                10.0, 65.0, 20.5);
        candidates.add(d);
        assertEquals(Optional.of(d), StationReach.nearest(STATION, candidates));

        // A pari posto vince l'id minore in forma testuale, non nell'ordine di UUID.compareTo, che confronta i bit
        // con il segno e metterebbe prima "ffffffff-...".
        StationReach.Candidate low = new StationReach.Candidate(id("00000000-0000-0000-0000-000000000009"),
                10.0, 65.0, 20.5);
        StationReach.Candidate high = new StationReach.Candidate(id("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                10.0, 65.0, 20.5);
        assertEquals(Optional.of(low), StationReach.nearest(STATION, List.of(high, low)));
    }

    @Test
    void nearestOfNoCandidatesIsEmpty() {
        assertEquals(Optional.empty(), StationReach.nearest(STATION, List.of()));
    }

    @Test
    void footprintRejectsHalvesThatAreNotSideBySide() {
        assertThrows(IllegalArgumentException.class, () -> StationReach.of(10, 64, 20, 10, 20));
        assertThrows(IllegalArgumentException.class, () -> StationReach.of(10, 64, 20, 12, 20));
        assertThrows(IllegalArgumentException.class, () -> StationReach.of(10, 64, 20, 11, 21));
    }

    private static UUID id(String text) {
        return UUID.fromString(text);
    }
}
