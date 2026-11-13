package com.toxineren.exoframes.core.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleInstallerTest {

    private final ModuleInstaller installer = new ModuleInstaller();

    private static Piece piece(PieceSlot slot, Tier tier) {
        return new Piece(slot, tier, 100, 100);
    }

    @Test
    void helmetModuleOnTorsoIsWrongPiece() {
        assertEquals(ModuleInstaller.Reason.WRONG_PIECE,
                installer.canInstall(piece(PieceSlot.TORSO, Tier.SCRAP), List.of(), ModuleType.NIGHT_VISION));
    }

    @Test
    void scrapHelmetHasOneSlot() {
        Piece helmet = piece(PieceSlot.HELMET, Tier.SCRAP);
        assertEquals(1, ModuleSlots.capacity(PieceSlot.HELMET, Tier.SCRAP));
        assertEquals(ModuleInstaller.Reason.OK, installer.canInstall(helmet, List.of(), ModuleType.NIGHT_VISION));
        assertEquals(ModuleInstaller.Reason.NO_FREE_SLOT,
                installer.canInstall(helmet, List.of(ModuleType.NIGHT_VISION), ModuleType.SCANNER));
    }

    @Test
    void prototypeHelmetHasFourSlots() {
        Piece helmet = piece(PieceSlot.HELMET, Tier.PROTOTYPE);
        assertEquals(4, ModuleSlots.capacity(PieceSlot.HELMET, Tier.PROTOTYPE));
        List<ModuleType> three = List.of(ModuleType.NIGHT_VISION, ModuleType.SCANNER, ModuleType.REBREATHER);
        assertEquals(ModuleInstaller.Reason.OK, installer.canInstall(helmet, three, ModuleType.SCULK_DETECTOR));
    }

    @Test
    void drillOnIndustrialArmIsTierTooLow() {
        assertEquals(ModuleInstaller.Reason.TIER_TOO_LOW,
                installer.canInstall(piece(PieceSlot.LEFT_ARM, Tier.INDUSTRIAL), List.of(), ModuleType.DRILL));
        assertEquals(ModuleInstaller.Reason.OK,
                installer.canInstall(piece(PieceSlot.LEFT_ARM, Tier.ARMORED), List.of(), ModuleType.DRILL));
    }

    @Test
    void sameModuleTwiceIsDuplicate() {
        Piece helmet = piece(PieceSlot.HELMET, Tier.PROTOTYPE);
        assertEquals(ModuleInstaller.Reason.DUPLICATE,
                installer.canInstall(helmet, List.of(ModuleType.SCANNER), ModuleType.SCANNER));
    }

    @Test
    void armFamilyAcceptsBothArms() {
        assertEquals(ModuleInstaller.Reason.OK,
                installer.canInstall(piece(PieceSlot.LEFT_ARM, Tier.SCRAP), List.of(), ModuleType.HYDRAULIC_BLADE));
        assertEquals(ModuleInstaller.Reason.OK,
                installer.canInstall(piece(PieceSlot.RIGHT_ARM, Tier.SCRAP), List.of(), ModuleType.HYDRAULIC_BLADE));
        assertEquals(ModuleInstaller.Reason.WRONG_PIECE,
                installer.canInstall(piece(PieceSlot.LEFT_LEG, Tier.SCRAP), List.of(), ModuleType.HYDRAULIC_BLADE));
    }
}
