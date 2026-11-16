package com.toxineren.exoframes.frame;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * I suoni delle azioni del telaio e del caricatore (design §12.13): un suono del gioco base ciascuna, volume 1 e tono
 * 1, tutti dal server con {@code level.playSound(null, ...)}. Con {@code null} come giocatore escluso li sentono tutti
 * quelli vicini, il giocatore che agisce compreso; {@code player.playSound(...)} sul server escluderebbe proprio lui,
 * e un suono suonato da un client non lo sentirebbe nessun altro. Solo costanti di {@code SoundEvents} che sono un
 * {@link SoundEvent} semplice sulle due versioni (non quelle che su 1.21.1 sono un {@code Holder}, come
 * {@code ARMOR_EQUIP_*}): nessuna cucitura.
 * <p>
 * La tabella: piazzare {@code METAL_PLACE} ({@code BLOCKS}); montare un pezzo o la cella {@code IRON_TRAPDOOR_CLOSE};
 * smontarli {@code IRON_TRAPDOOR_OPEN}; entrare {@code PISTON_CONTRACT}; uscire {@code PISTON_EXTEND}; cambio cella
 * {@code RESPAWN_ANCHOR_CHARGE}; pezzo rotto {@code ITEM_BREAK}; energia bassa {@code BEACON_DEACTIVATE}; energia
 * esaurita {@code REDSTONE_TORCH_BURNOUT} (tutti {@code PLAYERS} tranne il primo); cella inserita nel caricatore, o
 * scambiata con quella dentro, {@code ITEM_FRAME_ADD_ITEM}; cella ritirata dal caricatore
 * {@code ITEM_FRAME_REMOVE_ITEM} (tutti e due {@code BLOCKS}, al centro del blocco). Polvere aggiunta al caricatore e
 * tramogge: nessun suono. Gli slot armatura continuano a cambiare in silenzio: i suoni di ingresso e uscita sono
 * questi, non quelli dell'armatura indossata.
 * <p>
 * Il registratore ({@link #record}) serve solo ai test in mondo, per controllare quale suono fa ogni azione: senza
 * registrazioni aperte {@link #play} suona e basta.
 */
public final class FrameSounds {
    /** Le registrazioni aperte; i test girano insieme, ciascuno con la sua. */
    private static final List<Rec> RECORDINGS = new CopyOnWriteArrayList<>();

    /** Un suono per azione, con la sua fonte. */
    public enum Sound {
        PLACE(SoundEvents.METAL_PLACE, SoundSource.BLOCKS),
        MOUNT(SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS),
        UNMOUNT(SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS),
        ENTER(SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS),
        EXIT(SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS),
        CELL_SWAP(SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS),
        PIECE_BROKEN(SoundEvents.ITEM_BREAK, SoundSource.PLAYERS),
        ENERGY_LOW(SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS),
        ENERGY_EMPTY(SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.PLAYERS),
        CHARGER_INSERT(SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS),
        CHARGER_TAKE(SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS);

        private final SoundEvent event;
        private final SoundSource source;

        Sound(SoundEvent event, SoundSource source) {
            this.event = event;
            this.source = source;
        }
    }

    /** I suoni registrati in un livello e in un riquadro, finche' la registrazione resta aperta. Solo per i test. */
    public interface Recording extends AutoCloseable {
        /** I suoni suonati finora dentro il riquadro, in ordine. */
        List<Sound> sounds();

        /** Smette di registrare. */
        @Override
        void close();
    }

    private FrameSounds() {
    }

    /**
     * Suona {@code sound} nel punto {@code at} del livello, per tutti quelli vicini (giocatore che agisce compreso),
     * volume 1 e tono 1; lo aggiunge alle registrazioni aperte su quel livello che contengono il punto.
     */
    public static void play(ServerLevel level, Vec3 at, Sound sound) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(at, "at must not be null");
        Objects.requireNonNull(sound, "sound must not be null");
        level.playSound(null, at.x, at.y, at.z, sound.event, sound.source, 1.0F, 1.0F);
        for (Rec rec : RECORDINGS) {
            if (rec.level == level && rec.area.contains(at)) {
                rec.sounds.add(sound);
            }
        }
    }

    /**
     * Apre una registrazione dei suoni suonati con {@link #play} nel livello e nel riquadro dati, finche' non si
     * chiude. Solo per i test in mondo: ognuno registra il riquadro della sua struttura, cosi' i suoni dei test che
     * girano insieme non si mescolano.
     */
    public static Recording record(ServerLevel level, AABB area) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(area, "area must not be null");
        Rec rec = new Rec(level, area);
        RECORDINGS.add(rec);
        return rec;
    }

    /**
     * Una registrazione aperta. Una classe e non un record: {@link #close} toglie proprio questa istanza dall'elenco,
     * anche se un'altra registrazione ha lo stesso livello e lo stesso riquadro.
     */
    private static final class Rec implements Recording {
        private final ServerLevel level;
        private final AABB area;
        private final List<Sound> sounds = new ArrayList<>();

        Rec(ServerLevel level, AABB area) {
            this.level = level;
            this.area = area;
        }

        @Override
        public List<Sound> sounds() {
            return List.copyOf(sounds);
        }

        @Override
        public void close() {
            RECORDINGS.remove(this);
        }
    }
}
