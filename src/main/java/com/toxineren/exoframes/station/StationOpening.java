package com.toxineren.exoframes.station;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Cio' che il server sa quando apre la stazione e manda al client nei dati extra dell'apertura (design §5.2 e §5.5):
 * la posizione della meta' principale della stazione. Si scrive e si rilegge con i soli metodi di
 * {@link FriendlyByteBuf} uguali sulle quattro varianti, nello stesso ordine; nessun NBT e nessun codec degli stack.
 *
 * @param main la posizione della meta' principale della stazione
 */
public record StationOpening(BlockPos main) {
    public StationOpening {
        main = Objects.requireNonNull(main, "main must not be null").immutable();
    }

    /** Scrive l'apertura nei dati extra: lo scrittore che {@link StationMenu#open} passa ad Architectury. */
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(main);
    }

    /**
     * Rilegge l'apertura scritta da {@link #write}. La chiama subito il costruttore del menu del client: su Fabric
     * 1.21.1 il buffer dei dati extra si rilascia appena quel costruttore torna.
     */
    public static StationOpening read(FriendlyByteBuf buf) {
        return new StationOpening(buf.readBlockPos());
    }
}
