package com.toxineren.exoframes.core.station;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.station.StationAction.Kind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * La tabella dei pulsanti della stazione (design M4 §5.6): ogni numero valido con la sua azione e il suo argomento, i
 * buchi della tabella e i numeri fuori, negativi compresi, che non sono nulla; ogni azione torna al suo numero, e il
 * piu' alto sta nel byte del pacchetto dei pulsanti di 1.20.1.
 */
class StationActionTest {
    /** Gli slot nell'ordine dei numeri: elmo, torso, braccio sinistro e destro, gamba sinistra e destra. */
    private static final List<PieceSlot> SLOTS = List.of(PieceSlot.HELMET, PieceSlot.TORSO, PieceSlot.LEFT_ARM,
            PieceSlot.RIGHT_ARM, PieceSlot.LEFT_LEG, PieceSlot.RIGHT_LEG);

    @Test
    void selectionNumbersAreZeroToSeven() {
        for (int n = 0; n < 6; n++) {
            StationAction action = decoded(n);
            assertEquals(Kind.SELECT_PIECE, action.kind(), "number " + n);
            assertEquals(SLOTS.get(n), action.slot(), "number " + n);
            assertFalse(action.writes(), "number " + n + " writes");
        }
        assertEquals(Kind.SELECT_CELL, decoded(6).kind());
        assertFalse(decoded(6).writes());
        assertEquals(Kind.SELECT_ALL, decoded(7).kind());
        assertFalse(decoded(7).writes());
    }

    @Test
    void mountAndUnmountNumbersCarryTheSlot() {
        for (int i = 0; i < 6; i++) {
            StationAction mount = decoded(8 + i);
            assertEquals(Kind.MOUNT_PIECE, mount.kind(), "number " + (8 + i));
            assertEquals(SLOTS.get(i), mount.slot(), "number " + (8 + i));
            assertTrue(mount.writes(), "number " + (8 + i));
            StationAction unmount = decoded(16 + i);
            assertEquals(Kind.UNMOUNT_PIECE, unmount.kind(), "number " + (16 + i));
            assertEquals(SLOTS.get(i), unmount.slot(), "number " + (16 + i));
            assertTrue(unmount.writes(), "number " + (16 + i));
        }
        assertEquals(Kind.MOUNT_CELL, decoded(14).kind());
        assertTrue(decoded(14).writes());
        assertEquals(Kind.UNMOUNT_CELL, decoded(22).kind());
        assertTrue(decoded(22).writes());
    }

    @Test
    void repairLockPaintAndPresetNumbers() {
        assertAction(24, Kind.REPAIR_ONE, 0);
        assertAction(25, Kind.REPAIR_ALL, 0);
        assertAction(26, Kind.TOGGLE_LOCK, 0);
        for (int dye = 0; dye < 16; dye++) {
            assertAction(32 + dye, Kind.PAINT_PRIMARY, dye);
            assertAction(64 + dye, Kind.PAINT_SECONDARY, dye);
        }
        assertAction(48, Kind.PRIMARY_TIER_COLOUR, 0);
        assertAction(80, Kind.SECONDARY_TIER_COLOUR, 0);
        for (int preset = 0; preset < 3; preset++) {
            assertAction(96 + preset, Kind.APPLY_PRESET, preset);
            assertAction(100 + preset, Kind.SAVE_PRESET, preset);
        }
    }

    @Test
    void holesAndNumbersOutsideTheTableDecodeToNothing() {
        List<Integer> nothing = new ArrayList<>(List.of(15, 23, 99, -1, -128, 128, 255, Integer.MIN_VALUE,
                Integer.MAX_VALUE));
        for (int n = 27; n <= 31; n++) {
            nothing.add(n);
        }
        for (int n = 49; n <= 63; n++) {
            nothing.add(n);
        }
        for (int n = 81; n <= 95; n++) {
            nothing.add(n);
        }
        for (int n = 103; n <= 127; n++) {
            nothing.add(n);
        }
        for (int n : nothing) {
            assertEquals(Optional.empty(), StationAction.decode(n), "number " + n);
        }
    }

    @Test
    void everyActionEncodesBackToItsNumberAndFitsTheByte() {
        int valid = 0;
        int highest = Integer.MIN_VALUE;
        for (int n = -200; n <= 400; n++) {
            Optional<StationAction> action = StationAction.decode(n);
            if (action.isPresent()) {
                assertEquals(n, action.get().button(), "number " + n);
                assertEquals(n, StationAction.button(action.get().kind(), action.get().arg()), "number " + n);
                valid++;
                highest = Math.max(highest, n);
            }
        }
        assertEquals(65, valid);
        assertEquals(StationAction.HIGHEST, highest);
        assertEquals(102, StationAction.HIGHEST);
        assertTrue(StationAction.HIGHEST <= Byte.MAX_VALUE, "the highest number does not fit the 1.20.1 byte");
    }

    @Test
    void constructorRejectsArgumentsOutsideTheKind() {
        assertThrows(IllegalArgumentException.class, () -> new StationAction(Kind.MOUNT_PIECE, 6));
        assertThrows(IllegalArgumentException.class, () -> new StationAction(Kind.SELECT_CELL, 1));
        assertThrows(IllegalArgumentException.class, () -> new StationAction(Kind.PAINT_PRIMARY, 16));
        assertThrows(IllegalArgumentException.class, () -> new StationAction(Kind.SAVE_PRESET, -1));
        assertThrows(NullPointerException.class, () -> new StationAction(null, 0));
        assertThrows(IllegalStateException.class, () -> new StationAction(Kind.TOGGLE_LOCK, 0).slot());
    }

    /** L'azione del numero, che deve esserci. */
    private static StationAction decoded(int button) {
        return StationAction.decode(button).orElseThrow(() -> new AssertionError("number " + button + " decodes "
                + "to nothing"));
    }

    private static void assertAction(int button, Kind kind, int arg) {
        StationAction action = decoded(button);
        assertEquals(new StationAction(kind, arg), action, "number " + button);
        assertTrue(action.writes(), "number " + button + " does not write");
    }
}
