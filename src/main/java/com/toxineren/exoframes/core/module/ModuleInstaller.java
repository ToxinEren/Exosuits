package com.toxineren.exoframes.core.module;

import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import java.util.List;
import java.util.Objects;

/** Verifica se un modulo si puo' installare su un pezzo. */
public final class ModuleInstaller {

    public enum Reason {
        OK,
        WRONG_PIECE,
        NO_FREE_SLOT,
        TIER_TOO_LOW,
        DUPLICATE
    }

    private final ExoframesConfig.Pieces pieces;

    /** La capacita' degli slot si legge da {@code pieces.baseModuleSlots} (per riferimento, come {@code FrameRules}). */
    public ModuleInstaller(ExoframesConfig.Pieces pieces) {
        this.pieces = Objects.requireNonNull(pieces, "pieces");
    }

    /**
     * Restituisce il PRIMO motivo di rifiuto, in quest'ordine: WRONG_PIECE (la famiglia non accetta lo
     * slot), TIER_TOO_LOW (il livello del pezzo e' sotto il minimo del modulo), DUPLICATE (gia'
     * installato), NO_FREE_SLOT (installati >= capacita'); altrimenti OK.
     */
    public Reason canInstall(Piece piece, List<ModuleType> installedOnPiece, ModuleType module) {
        if (!module.family().accepts(piece.slot())) {
            return Reason.WRONG_PIECE;
        }
        if (!piece.tier().atLeast(module.minTier())) {
            return Reason.TIER_TOO_LOW;
        }
        if (installedOnPiece.contains(module)) {
            return Reason.DUPLICATE;
        }
        if (installedOnPiece.size() >= ModuleSlots.capacity(piece.slot(), piece.tier(), pieces)) {
            return Reason.NO_FREE_SLOT;
        }
        return Reason.OK;
    }
}
