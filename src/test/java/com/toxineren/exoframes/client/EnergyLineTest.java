package com.toxineren.exoframes.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Il contenuto della riga dell'energia ({@link EnergyLine}): percentuale per difetto e pezzo messo peggio. */
class EnergyLineTest {

    private static Piece piece(PieceSlot slot, int max, int durability) {
        return new Piece(slot, Tier.SCRAP, max, durability);
    }

    private static int percent(int capacity, int stored) {
        FrameState s = new FrameState(ExoframesConfig.defaults());
        s.insertCell(new EnergyCell(capacity, stored));
        return EnergyLine.of(s).get().percent();
    }

    @Test
    void percentRoundsDown() {
        FrameState s = new FrameState(ExoframesConfig.defaults());
        s.insertCell(new EnergyCell(10_000, 7_429));
        EnergyLine line = EnergyLine.of(s).get();
        assertEquals(7_429, line.stored());
        assertEquals(10_000, line.capacity());
        assertEquals(74, line.percent());
        assertEquals(99, percent(10_000, 9_999));
        assertEquals(0, percent(10_000, 99));
        assertEquals(100, percent(10_000, 10_000));
        // 999 999 * 100 non sta in un int: il prodotto si fa in long.
        assertEquals(99, percent(1_000_000, 999_999));
    }

    @Test
    void worstPieceIsTheBrokenOne() {
        FrameState s = new FrameState(ExoframesConfig.defaults());
        s.insertCell(new EnergyCell(10_000, 5_000));
        s.mount(piece(PieceSlot.HELMET, 180, 10)); // critico
        s.mount(piece(PieceSlot.LEFT_LEG, 180, 0)); // rotto
        EnergyLine line = EnergyLine.of(s).get();
        assertEquals(Optional.of(PieceSlot.LEFT_LEG), line.worst());
        assertEquals(Optional.of(PieceCondition.BROKEN), line.worstCondition());
    }

    @Test
    void lowestRelativeDurabilityAmongCritical() {
        FrameState s = new FrameState(ExoframesConfig.defaults());
        s.insertCell(new EnergyCell(10_000, 5_000));
        s.mount(piece(PieceSlot.HELMET, 180, 20)); // 11 %
        s.mount(piece(PieceSlot.TORSO, 250, 20)); // 8 %: stessa durata assoluta, vince la relativa
        EnergyLine line = EnergyLine.of(s).get();
        assertEquals(Optional.of(PieceSlot.TORSO), line.worst());
        assertEquals(Optional.of(PieceCondition.CRITICAL), line.worstCondition());
    }

    @Test
    void tieFollowsSlotOrder() {
        FrameState critical = new FrameState(ExoframesConfig.defaults());
        critical.insertCell(new EnergyCell(10_000, 5_000));
        critical.mount(piece(PieceSlot.RIGHT_ARM, 180, 10));
        critical.mount(piece(PieceSlot.LEFT_ARM, 180, 10));
        EnergyLine first = EnergyLine.of(critical).get();
        assertEquals(Optional.of(PieceSlot.LEFT_ARM), first.worst());
        assertEquals(Optional.of(PieceCondition.CRITICAL), first.worstCondition());

        FrameState broken = new FrameState(ExoframesConfig.defaults());
        broken.insertCell(new EnergyCell(10_000, 5_000));
        broken.mount(piece(PieceSlot.RIGHT_LEG, 180, 0));
        broken.mount(piece(PieceSlot.TORSO, 180, 0));
        EnergyLine second = EnergyLine.of(broken).get();
        assertEquals(Optional.of(PieceSlot.TORSO), second.worst());
        assertEquals(Optional.of(PieceCondition.BROKEN), second.worstCondition());
    }

    @Test
    void noWorstPieceWhenNoneIsCriticalOrBroken() {
        FrameState s = new FrameState(ExoframesConfig.defaults());
        s.insertCell(new EnergyCell(10_000, 5_000));
        s.mount(piece(PieceSlot.HELMET, 180, 90)); // danneggiato
        s.mount(piece(PieceSlot.TORSO, 180, 180));
        EnergyLine line = EnergyLine.of(s).get();
        assertEquals(Optional.empty(), line.worst());
        assertEquals(Optional.empty(), line.worstCondition());
    }

    @Test
    void noCellGivesNoLine() {
        FrameState s = new FrameState(ExoframesConfig.defaults());
        s.mount(piece(PieceSlot.HELMET, 180, 10));
        s.mount(piece(PieceSlot.TORSO, 180, 180));
        assertTrue(EnergyLine.of(s).isEmpty());
    }
}
