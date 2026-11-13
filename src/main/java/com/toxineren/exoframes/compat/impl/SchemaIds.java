package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

/**
 * Conversioni condivise dello schema NBT della mod, usate sia dai dati degli item ({@link ItemDataAccessImpl}) sia
 * dallo stato del telaio ({@link FrameStateCodec}), cosi' i due codec non possono divergere: id degli slot, dei
 * livelli e dei moduli, lettura sicura degli UUID e avviso una sola volta per id sconosciuto. Nessuna condizione di
 * versione: le API usate sono le stesse su 1.20.1 e 1.21.1.
 */
final class SchemaIds {
    /** Id sconosciuti gia' segnalati nel log (per tipo e id), per avvisare una volta sola. */
    private static final Set<String> WARNED_IDS = ConcurrentHashMap.newKeySet();

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

    /** Segnala nel log un id sconosciuto, una sola volta per coppia tipo/id. */
    static void warnUnknown(String kind, String id) {
        if (WARNED_IDS.add(kind + ":" + id)) {
            RedstoneExoframes.LOGGER.warn("Ignoring unknown {} id in mod NBT data: {}", kind, id);
        }
    }
}
