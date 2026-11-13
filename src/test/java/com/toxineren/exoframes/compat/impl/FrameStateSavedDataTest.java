package com.toxineren.exoframes.compat.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

/** Voci non lette conservate e marcatore di schema del dato salvato (solo classi NBT e SavedData). */
class FrameStateSavedDataTest {

    private static final UUID GOOD = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BAD = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static CompoundTag entry() {
        FrameState state = new FrameState(ExoframesConfig.defaults());
        state.mount(new Piece(PieceSlot.HELMET, Tier.ARMORED, 400, 321));
        state.setUnlocked(true);
        return FrameStateCodec.write(state);
    }

    /** Dato con una voce buona, una che fa lanciare il lettore, una con chiave non UUID e una non compound. */
    private static CompoundTag mixedRoot() {
        CompoundTag frames = new CompoundTag();
        frames.put(GOOD.toString(), entry());
        CompoundTag corrupt = entry();
        corrupt.putBoolean("test_corrupt", true);
        frames.put(BAD.toString(), corrupt);
        frames.put("not-a-uuid", entry());
        frames.put("00000000-0000-0000-0000-000000000003", StringTag.valueOf("garbage"));
        CompoundTag root = new CompoundTag();
        root.put("frames", frames);
        return root;
    }

    private static FrameStateSavedData loadThrowingOnMarked(CompoundTag root) {
        return FrameStateSavedData.load(root, (tag, config) -> {
            if (tag.getBoolean("test_corrupt")) {
                throw new IllegalStateException("corrupt entry");
            }
            return FrameStateCodec.read(tag, config);
        });
    }

    @Test
    void skippedEntriesAreWrittenBackVerbatim() {
        CompoundTag root = mixedRoot();
        CompoundTag original = root.getCompound("frames").copy();
        FrameStateSavedData data = loadThrowingOnMarked(root);
        assertEquals(Set.of(GOOD), data.frames().keySet());

        CompoundTag written = data.write(new CompoundTag());
        CompoundTag frames = written.getCompound("frames");
        assertEquals(original.getAllKeys(), frames.getAllKeys());
        for (String key : original.getAllKeys()) {
            if (!key.equals(GOOD.toString())) {
                assertEquals(original.get(key), frames.get(key), key);
            }
        }
    }

    @Test
    void unreadEntryIsDroppedOnlyForThatPlayer() {
        FrameStateSavedData data = loadThrowingOnMarked(mixedRoot());
        data.dropUnread(BAD);
        data.dropUnread(GOOD);
        CompoundTag frames = data.write(new CompoundTag()).getCompound("frames");
        assertFalse(frames.contains(BAD.toString()));
        assertTrue(frames.contains(GOOD.toString()));
        assertTrue(frames.contains("not-a-uuid"));
        assertTrue(frames.contains("00000000-0000-0000-0000-000000000003"));
    }

    @Test
    void writeStampsTheSchemaAndAbsentSchemaReadsAsOne() {
        CompoundTag root = new CompoundTag();
        CompoundTag frames = new CompoundTag();
        frames.put(GOOD.toString(), entry());
        root.put("frames", frames);
        FrameStateSavedData data = FrameStateSavedData.load(root, FrameStateCodec::read);
        assertEquals(Set.of(GOOD), data.frames().keySet());
        CompoundTag written = data.write(new CompoundTag());
        assertEquals(1, written.getInt(NbtKeys.SCHEMA));

        FrameStateSavedData again = FrameStateSavedData.load(written, FrameStateCodec::read);
        assertEquals(Set.of(GOOD), again.frames().keySet());
    }

    @Test
    void futureSchemaKeepsEveryEntryRaw() {
        CompoundTag root = mixedRoot();
        root.putInt(NbtKeys.SCHEMA, 2);
        CompoundTag original = root.getCompound("frames").copy();
        FrameStateSavedData data = FrameStateSavedData.load(root, FrameStateCodec::read);
        assertTrue(data.frames().isEmpty());

        CompoundTag written = data.write(new CompoundTag());
        assertEquals(original, written.getCompound("frames"));
        assertEquals(2, written.getInt(NbtKeys.SCHEMA));
    }
}
