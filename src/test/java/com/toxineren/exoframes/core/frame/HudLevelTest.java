package com.toxineren.exoframes.core.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.energy.EnergyCell;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Il livello dell'HUD ({@link HudLevel}, design §5.12, "Livelli"): l'HUD intero con un elmo che funziona, anche
 * critico; la sola barra senza elmo, con l'elmo rotto o con il telaio nudo; niente senza la tuta addosso o senza la
 * copia dello stato. I pezzi sono di Rottame, con la durata massima 180 della config predefinita.
 */
class HudLevelTest {

    /** La durata massima del Rottame nella config predefinita. */
    private static final int SCRAP_MAX = 180;

    @Test
    void fullWithAWorkingHelmet() {
        assertEquals(HudLevel.FULL, level(state(true, piece(PieceSlot.HELMET, 180))));
        Piece damaged = piece(PieceSlot.HELMET, 90);
        assertEquals(PieceCondition.DAMAGED, damaged.condition());
        assertEquals(HudLevel.FULL, level(state(true, damaged)));
        // un elmo critico funziona ancora
        Piece critical = piece(PieceSlot.HELMET, 1);
        assertEquals(PieceCondition.CRITICAL, critical.condition());
        assertEquals(HudLevel.FULL, level(state(true, critical)));
        // la cella non conta
        assertEquals(HudLevel.FULL, level(state(false, piece(PieceSlot.HELMET, 180))));
    }

    @Test
    void barOnlyWithoutAWorkingHelmet() {
        Piece broken = piece(PieceSlot.HELMET, 0);
        assertEquals(PieceCondition.BROKEN, broken.condition());
        assertEquals(HudLevel.BAR_ONLY, level(state(true, broken, piece(PieceSlot.TORSO, 180))));
        assertEquals(HudLevel.BAR_ONLY, level(state(true, piece(PieceSlot.TORSO, 180))));
        // il telaio nudo
        assertEquals(HudLevel.BAR_ONLY, level(state(true)));
    }

    @Test
    void noneWithoutTheSuitOrTheView() {
        FrameState intactHelmet = state(true, piece(PieceSlot.HELMET, 180));
        assertEquals(HudLevel.NONE, HudLevel.of(false, Optional.of(intactHelmet)));
        assertEquals(HudLevel.NONE, HudLevel.of(true, Optional.empty()));
        assertEquals(HudLevel.NONE, HudLevel.of(false, Optional.empty()));
    }

    /** Il livello per chi indossa la tuta, con la copia dello stato data. */
    private static HudLevel level(FrameState view) {
        return HudLevel.of(true, Optional.of(view));
    }

    private static Piece piece(PieceSlot slot, int durability) {
        return new Piece(slot, Tier.SCRAP, SCRAP_MAX, durability);
    }

    /** Uno stato con la config predefinita, con una cella carica a meta' o senza cella, e i pezzi dati montati. */
    private static FrameState state(boolean cell, Piece... pieces) {
        FrameState state = new FrameState(ExoframesConfig.defaults());
        if (cell) {
            state.insertCell(new EnergyCell(10_000, 5_000));
        }
        for (Piece piece : pieces) {
            state.mount(piece);
        }
        return state;
    }
}
