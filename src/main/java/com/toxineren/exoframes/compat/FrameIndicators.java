package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.core.energy.EnergyState;
import com.toxineren.exoframes.core.frame.FrameEffects;
import java.util.Objects;

/**
 * I tre indicatori che il pacchetto al proprietario porta con lo stato del telaio (design §5.3, §12.4): li calcola il
 * server con {@code RedstoneExoframes.rules()} al momento del pacchetto, cosi' il client non valuta mai le regole con
 * la propria config e non confronta l'energia con una soglia propria. {@code canSwim} in M3 e' sempre falso e nessuno
 * lo legge (servira' a un modulo di nuoto). Letti sul client con {@link FrameStateAccess#clientIndicators}.
 */
public record FrameIndicators(boolean canSprint, boolean canSwim, EnergyState energyState) {
    public FrameIndicators {
        Objects.requireNonNull(energyState, "energyState");
    }

    /** Gli indicatori degli effetti calcolati dalle regole. */
    public static FrameIndicators of(FrameEffects fx) {
        return new FrameIndicators(fx.canSprint(), fx.canSwim(), fx.energyState());
    }
}
