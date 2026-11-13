package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

/**
 * Conversioni condivise dello schema NBT della mod, usate sia dai dati degli item ({@link ItemDataAccessImpl}) sia
 * dallo stato del telaio ({@link FrameStateCodec}): id degli slot, dei livelli e dei moduli, lettura sicura degli
 * UUID e avvisi una sola volta per chiave (id sconosciuti e voci scartate). La lettura di pezzi ed energia e' in
 * {@link PieceTags}. Nessuna condizione di versione: le API usate sono le stesse su 1.20.1 e 1.21.1.
 * <p>
 * Gli id letti dai tag sono dati non fidati (un giocatore con /give li sceglie): nel log e nelle chiavi degli avvisi
 * entrano solo nella forma di {@link #safeId}, e l'insieme delle chiavi gia' segnalate ha al massimo
 * {@link #MAX_WARNED} voci.
 */
final class SchemaIds {
    /** Numero massimo di chiavi ricordate dagli avvisi una volta per chiave. */
    static final int MAX_WARNED = 256;
    /** Lunghezza massima di un id nel log e nelle chiavi degli avvisi. */
    static final int MAX_ID_LENGTH = 64;

    /** Avvisi gia' emessi (id sconosciuti e voci scartate), condivisi da tutti i lettori dello schema. */
    private static final WarnOnce WARNINGS = new WarnOnce(MAX_WARNED, RedstoneExoframes.LOGGER);

    private SchemaIds() {
    }

    /** L'id di uno slot nello schema: il nome dell'enum in minuscolo. */
    static String slotId(PieceSlot slot) {
        return slot.name().toLowerCase(Locale.ROOT);
    }

    /** Lo slot con quell'id, o null (senza avviso: lo decide il chiamante, che conosce il contesto). */
    static PieceSlot slotById(String id) {
        for (PieceSlot slot : PieceSlot.values()) {
            if (slotId(slot).equals(id)) {
                return slot;
            }
        }
        return null;
    }

    /** Il livello con quell'id, o null con un avviso (una volta per id). */
    static Tier tierById(String id) {
        try {
            return Tier.byId(id);
        } catch (IllegalArgumentException e) {
            warnUnknown("tier", id);
            return null;
        }
    }

    /** Il modulo con quell'id, o null con un avviso (una volta per id). */
    static ModuleType moduleById(String id) {
        try {
            return ModuleType.byId(id);
        } catch (IllegalArgumentException e) {
            warnUnknown("module", id);
            return null;
        }
    }

    /**
     * L'UUID salvato sotto la chiave, presente solo se la chiave contiene un int array di esattamente 4 elementi;
     * qualsiasi altro valore vale assente, senza eccezioni (un tag manomesso non deve far fallire chi lo legge).
     */
    static Optional<UUID> readUuid(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_INT_ARRAY) || tag.getIntArray(key).length != 4) {
            return Optional.empty();
        }
        return Optional.of(NbtUtils.loadUUID(tag.get(key)));
    }

    /** Segnala nel log un id sconosciuto, una sola volta per coppia tipo/id (id reso sicuro con {@link #safeId}). */
    static void warnUnknown(String kind, String id) {
        String safe = safeId(id);
        WARNINGS.warn(kind + ":" + safe, "Ignoring unknown {} id in mod NBT data: {}", kind, safe);
    }

    /**
     * Avviso una sola volta per chiave, con lo stesso limite degli id sconosciuti. La chiave e gli argomenti non
     * devono contenere testo non fidato se non passato da {@link #safeId}.
     */
    static void warnOnce(String key, String format, Object... args) {
        WARNINGS.warn(key, format, args);
    }

    /**
     * L'id nella forma ammessa nel log e nelle chiavi: troncato a {@link #MAX_ID_LENGTH} caratteri e con i caratteri
     * di controllo sostituiti da {@code ?}.
     */
    static String safeId(String id) {
        String cut = id.length() > MAX_ID_LENGTH ? id.substring(0, MAX_ID_LENGTH) : id;
        StringBuilder out = new StringBuilder(cut.length());
        for (int i = 0; i < cut.length(); i++) {
            char c = cut.charAt(i);
            out.append(Character.isISOControl(c) ? '?' : c);
        }
        return out.toString();
    }

    /**
     * Insieme limitato di chiavi gia' segnalate: ogni chiave produce al massimo un avviso; raggiunto il limite, le
     * chiavi nuove non vengono piu' ricordate ne' segnalate, e una sola riga lo dice.
     */
    static final class WarnOnce {
        private final int capacity;
        private final Logger logger;
        private final Set<String> keys = new HashSet<>();
        private boolean suppressed;

        WarnOnce(int capacity, Logger logger) {
            this.capacity = capacity;
            this.logger = logger;
        }

        /** Emette l'avviso se la chiave e' nuova e c'e' ancora posto; true se l'ha emesso. */
        synchronized boolean warn(String key, String format, Object... args) {
            if (keys.contains(key)) {
                return false;
            }
            if (keys.size() >= capacity) {
                if (!suppressed) {
                    suppressed = true;
                    logger.warn("Too many distinct warnings about mod NBT data: further unknown ids suppressed");
                }
                return false;
            }
            keys.add(key);
            logger.warn(format, args);
            return true;
        }

        /** Numero di chiavi ricordate. */
        synchronized int size() {
            return keys.size();
        }
    }
}
