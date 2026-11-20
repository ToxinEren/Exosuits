package com.toxineren.exoframes.station;

import com.toxineren.exoframes.core.station.StationView;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Cio' che il server sa quando apre la stazione e manda al client nei dati extra dell'apertura (design §5.2 e §5.5):
 * la posizione della meta' principale della stazione, l'identita' del telaio su cui si apre, fissato all'apertura
 * per tutta la vita del menu, e la vista di quel telaio per chi apre ({@link StationView}, con la scelta iniziale). Si
 * scrive e si rilegge con i soli metodi di {@link FriendlyByteBuf} uguali sulle quattro varianti, nello stesso ordine
 * (la vista con {@link StationViewCodec}); nessun NBT e nessun codec degli stack.
 *
 * @param main la posizione della meta' principale della stazione
 * @param frameId l'identita' del telaio piazzato su cui il menu si apre
 * @param view la vista del telaio per il giocatore che apre
 */
public record StationOpening(BlockPos main, UUID frameId, StationView view) {
    public StationOpening {
        main = Objects.requireNonNull(main, "main must not be null").immutable();
        Objects.requireNonNull(frameId, "frameId must not be null");
        Objects.requireNonNull(view, "view must not be null");
    }

    /** Scrive l'apertura nei dati extra: lo scrittore che {@link StationMenu#open} passa ad Architectury. */
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(main);
        buf.writeUUID(frameId);
        StationViewCodec.INSTANCE.write(buf, view);
    }

    /**
     * Rilegge l'apertura scritta da {@link #write}. La chiama subito il costruttore del menu del client: su Fabric
     * 1.21.1 il buffer dei dati extra si rilascia appena quel costruttore torna.
     */
    public static StationOpening read(FriendlyByteBuf buf) {
        BlockPos main = buf.readBlockPos();
        UUID frameId = buf.readUUID();
        return new StationOpening(main, frameId, StationViewCodec.INSTANCE.read(buf));
    }
}
