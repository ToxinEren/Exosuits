package com.toxineren.exoframes.compat.impl;

import com.mojang.serialization.Dynamic;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.compat.StowedStack;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
//? if >=1.21 {
import com.mojang.serialization.DataResult;
//?}

/**
 * Una voce dell'armatura stivata ({@link NbtKeys#STOWED}) fra {@link StowedStack} e tag: {@code {slot, data_version,
 * item}}, con lo slot come nome dell'{@link EquipmentSlot} in minuscolo, la versione dei dati di Minecraft che ha
 * scritto lo stack e lo stack nel formato di quella versione.
 * <p>
 * Condizione di versione: lo stack si scrive con {@code ItemStack.save(CompoundTag)} su 1.20.1 e con
 * {@code save(HolderLookup.Provider)} su 1.21.1, e si legge con {@code ItemStack.of} su 1.20.1 e con
 * {@code ItemStack.CODEC} e il provider su 1.21.1 (lo stesso codec di {@code parseOptional}, ma con l'errore in mano e
 * senza la riga di log di vanilla a ogni lettura). Una voce con una {@code data_version} piu' vecchia di quella
 * corrente (uno stack salvato su 1.20.1 e letto su 1.21.1) passa prima dal correttore di dati vanilla
 * ({@link References#ITEM_STACK}): vanilla non converte da solo gli stack annidati nel dato salvato della mod.
 * <p>
 * Una voce che non si legge (slot sconosciuto, versione mancante o piu' recente di quella corrente, item sconosciuto,
 * stack corrotto, errore del correttore) da' vuoto con un avviso una volta per voce e per telaio, che nomina il
 * telaio: il chiamante la lascia nel record cosi' com'e', e non si perde mai.
 * <p>
 * Il correttore di dati vanilla scrive righe sue nel log per certe voci rovinate (per esempio
 * {@code Unable to resolve BlockEntity for ItemStack} dallo schema V99 su 1.21.1), a ogni passaggio. Per questo il
 * risultato di ogni voce piu' vecchia (lo stack corretto o il motivo per cui non si legge) e quello di ogni voce
 * illeggibile si ricordano per voce: il correttore gira una volta sola per voce, e le prese e i controlli successivi
 * riusano il risultato. La memoria ha chiavi deboli (le voci vive nei record) e confronta le voci per contenuto.
 */
final class StackTags {
    /**
     * Per voce: lo stack gia' corretto ({@link CompoundTag}, nel formato corrente) o il motivo per cui la voce non si
     * legge ({@link String}). Solo le voci piu' vecchie e quelle illeggibili ci entrano.
     */
    private static final Map<CompoundTag, Object> OUTCOMES = Collections.synchronizedMap(new WeakHashMap<>());

    private StackTags() {
    }

    /** La versione dei dati di Minecraft in esecuzione (3465 su 1.20.1, 3955 su 1.21.1). */
    static int currentDataVersion() {
        return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    }

    /**
     * La voce dello stack nello slot, con la versione dei dati corrente. Lo stack non deve essere vuoto (uno stack
     * vuoto non si stiva: su 1.21.1 non si potrebbe nemmeno scrivere).
     */
    static CompoundTag write(HolderLookup.Provider registries, EquipmentSlot slot, ItemStack stack) {
        Objects.requireNonNull(registries, "registries must not be null");
        Objects.requireNonNull(slot, "slot must not be null");
        Objects.requireNonNull(stack, "stack must not be null");
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("an empty stack cannot be stowed");
        }
        CompoundTag entry = new CompoundTag();
        entry.putString(NbtKeys.SLOT, slotId(slot));
        entry.putInt(NbtKeys.DATA_VERSION, currentDataVersion());
        //? if >=1.21 {
        entry.put(NbtKeys.ITEM, stack.save(registries));
        //?} else {
        /*entry.put(NbtKeys.ITEM, stack.save(new CompoundTag()));*/
        //?}
        return entry;
    }

    /** Come {@link #read(HolderLookup.Provider, CompoundTag, UUID)} senza un telaio da nominare nell'avviso. */
    static Optional<StowedStack> read(HolderLookup.Provider registries, CompoundTag tag) {
        return read(registries, tag, null);
    }

    /**
     * Lo stack della voce con il suo slot; vuoto, con un avviso una volta per voce e per telaio (che l'avviso nomina,
     * se {@code frameId} non e' null), se la voce non si legge. Non cambia il tag dato. Non lancia.
     */
    static Optional<StowedStack> read(HolderLookup.Provider registries, CompoundTag tag, UUID frameId) {
        Object known = OUTCOMES.get(tag);
        if (known instanceof String reason) {
            return unreadable(tag, frameId, reason);
        }
        if (!tag.contains(NbtKeys.SLOT, Tag.TAG_STRING)) {
            return unreadable(tag, frameId, "missing slot");
        }
        EquipmentSlot slot = slotById(tag.getString(NbtKeys.SLOT));
        if (slot == null) {
            return unreadable(tag, frameId, "unknown slot");
        }
        if (!tag.contains(NbtKeys.DATA_VERSION, Tag.TAG_ANY_NUMERIC)) {
            return unreadable(tag, frameId, "missing data version");
        }
        int version = tag.getInt(NbtKeys.DATA_VERSION);
        int current = currentDataVersion();
        if (version > current) {
            return unreadable(tag, frameId, "saved by a newer game version");
        }
        if (!tag.contains(NbtKeys.ITEM, Tag.TAG_COMPOUND)) {
            return unreadable(tag, frameId, "missing item");
        }
        // una copia: ne' il correttore ne' la lettura devono toccare il tag che resta nel record
        CompoundTag item = tag.getCompound(NbtKeys.ITEM).copy();
        try {
            if (known instanceof CompoundTag fixedBefore) {
                item = fixedBefore.copy();
            } else if (version < current) {
                Tag fixed = DataFixers.getDataFixer()
                        .update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, item), version, current)
                        .getValue();
                if (!(fixed instanceof CompoundTag fixedItem)) {
                    return unreadable(tag, frameId, "data fixer did not return a compound");
                }
                OUTCOMES.put(tag, fixedItem.copy());
                item = fixedItem;
            }
            //? if >=1.21 {
            DataResult<ItemStack> result =
                    ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), item);
            Optional<ItemStack> stack = result.result();
            if (stack.isEmpty()) {
                return unreadable(tag, frameId,
                        result.error().map(DataResult.Error::message).orElse("invalid stack"));
            }
            return Optional.of(new StowedStack(slot, stack.get()));
            //?} else {
            /*ItemStack stack = ItemStack.of(item);
            if (stack.isEmpty()) {
                return unreadable(tag, frameId, "unknown item or invalid stack");
            }
            return Optional.of(new StowedStack(slot, stack));*/
            //?}
        } catch (RuntimeException e) {
            return unreadable(tag, frameId, e.toString());
        }
    }

    /** L'id di uno slot nella voce: il nome dell'enum in minuscolo. */
    private static String slotId(EquipmentSlot slot) {
        return slot.name().toLowerCase(Locale.ROOT);
    }

    /** Lo slot con quell'id, o null. */
    private static EquipmentSlot slotById(String id) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slotId(slot).equals(id)) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Vuoto, con il motivo ricordato per la voce e un avviso una volta per telaio e per voce (la chiave e' il telaio
     * con l'hash del contenuto della voce, quindi la stessa voce riletta a ogni presa non si ripete nel log, e la
     * stessa voce in due telai si segnala per ciascuno). Id, versione e motivo passano da {@link SchemaIds#safeId}.
     */
    private static Optional<StowedStack> unreadable(CompoundTag tag, UUID frameId, String reason) {
        OUTCOMES.put(tag, reason);
        String itemId = tag.getCompound(NbtKeys.ITEM).getString("id");
        Tag version = tag.get(NbtKeys.DATA_VERSION);
        SchemaIds.warnOnce("stowed:" + frameId + ":" + Integer.toHexString(tag.hashCode()),
                "Unreadable stowed armor entry in frame {} (item {}, data version {}), kept in the frame registry "
                        + "unchanged: {}",
                frameId == null ? "unknown" : frameId, SchemaIds.safeId(itemId),
                SchemaIds.safeId(String.valueOf(version)), SchemaIds.safeId(reason));
        return Optional.empty();
    }
}
