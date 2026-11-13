package com.toxineren.exoframes.compat.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.compat.NbtKeys;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/**
 * Memoria dei risultati di lettura dell'armatura stivata: solo il percorso che non tocca stack ne' registri (una voce
 * senza slot non si legge prima di arrivare al correttore o al codec), quindi senza bootstrap del gioco.
 */
class StackTagsTest {

    private static CompoundTag entryWithoutSlot(String itemId) {
        CompoundTag item = new CompoundTag();
        item.putString("id", itemId);
        CompoundTag entry = new CompoundTag();
        entry.putInt(NbtKeys.DATA_VERSION, 3955);
        entry.put(NbtKeys.ITEM, item);
        return entry;
    }

    @Test
    void unreadableEntryIsRememberedOncePerEntry() {
        StackTags.forgetOutcomes();
        CompoundTag entry = entryWithoutSlot("minecraft:iron_helmet");
        UUID frame = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        assertTrue(StackTags.read(null, entry, frame).isEmpty());
        assertTrue(StackTags.read(null, entry, frame).isEmpty());
        assertTrue(StackTags.read(null, entry.copy(), frame).isEmpty());
        assertEquals(1, StackTags.rememberedOutcomes());
    }

    @Test
    void creatingOrLoadingAFrameRegistryForgetsRememberedOutcomes() {
        CompoundTag first = entryWithoutSlot("minecraft:iron_helmet");
        CompoundTag second = entryWithoutSlot("minecraft:leather_boots");
        StackTags.forgetOutcomes();
        assertTrue(StackTags.read(null, first).isEmpty());
        assertTrue(StackTags.read(null, second).isEmpty());
        assertEquals(2, StackTags.rememberedOutcomes());

        // un registro caricato e' un server nuovo: nulla di quello che ha ricordato il server di prima resta
        FrameStateSavedData.load(new CompoundTag(), FrameStateCodec::read);
        assertEquals(0, StackTags.rememberedOutcomes());

        assertTrue(StackTags.read(null, first).isEmpty());
        assertEquals(1, StackTags.rememberedOutcomes());
        FrameStateSavedData.load(new CompoundTag(), FrameStateCodec::read);
        assertEquals(0, StackTags.rememberedOutcomes());
    }
}
