package com.toxineren.exoframes.compat.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

/** Registro dei telai del dato salvato: mappe indossati e piazzati, voci non lette, marcatore di schema (solo NBT). */
class FrameStateSavedDataTest {

    private static final UUID GOOD = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BAD = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID FRAME_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID FRAME_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final PlacedSpot OVERWORLD_SPOT = new PlacedSpot("minecraft:overworld", 1.5, 64.0, -2.25, 90.0F);
    private static final PlacedSpot NETHER_SPOT = new PlacedSpot("minecraft:the_nether", -8.0, 32.5, 4.0, -45.0F);

    private static FrameState state() {
        FrameState state = new FrameState(ExoframesConfig.defaults());
        state.mount(new Piece(PieceSlot.HELMET, Tier.ARMORED, 400, 321));
        state.setUnlocked(true);
        return state;
    }

    /** Uno stato come tag (forma della voce di schema 1 e del campo {@code state} dello schema 2). */
    private static CompoundTag stateTag() {
        return FrameStateCodec.write(state());
    }

    /** Una voce di armatura stivata come la scrivera' il livello degli stack: qui e' solo un tag opaco. */
    private static CompoundTag stowedEntry(String slot, String itemId) {
        CompoundTag item = new CompoundTag();
        item.putString("id", itemId);
        item.putInt("count", 1);
        CompoundTag entry = new CompoundTag();
        entry.putString(NbtKeys.SLOT, slot);
        entry.putInt(NbtKeys.DATA_VERSION, 3955);
        entry.put(NbtKeys.ITEM, item);
        return entry;
    }

    /** Un record indossato di schema 2 con l'identita' data. */
    private static CompoundTag wornRecordTag(UUID frameId, CompoundTag state) {
        CompoundTag record = new CompoundTag();
        record.put(NbtKeys.ID, NbtUtils.createUUID(frameId));
        record.put(NbtKeys.STATE, state);
        record.put(NbtKeys.STOWED, new ListTag());
        return record;
    }

    /** Un record piazzato di schema 2 (la chiave della voce e' l'identita'). */
    private static CompoundTag placedRecordTag(CompoundTag state) {
        CompoundTag record = new CompoundTag();
        record.put(NbtKeys.STATE, state);
        record.put(NbtKeys.STOWED, new ListTag());
        record.putString(NbtKeys.DIM, "minecraft:overworld");
        ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(1.0));
        pos.add(DoubleTag.valueOf(2.0));
        pos.add(DoubleTag.valueOf(3.0));
        record.put(NbtKeys.POS, pos);
        record.putFloat(NbtKeys.YAW, 0.0F);
        return record;
    }

    /**
     * Dato di schema 2 con un record indossato buono, uno il cui stato fa lanciare il lettore, uno con chiave non
     * UUID e uno non compound; fra i piazzati uno buono, uno senza posizione e uno con chiave non UUID.
     */
    private static CompoundTag mixedRoot() {
        CompoundTag worn = new CompoundTag();
        worn.put(GOOD.toString(), wornRecordTag(FRAME_A, stateTag()));
        CompoundTag corrupt = stateTag();
        corrupt.putBoolean("test_corrupt", true);
        worn.put(BAD.toString(), wornRecordTag(FRAME_B, corrupt));
        worn.put("not-a-uuid", wornRecordTag(UUID.randomUUID(), stateTag()));
        worn.put("00000000-0000-0000-0000-000000000003", StringTag.valueOf("garbage"));
        CompoundTag root = new CompoundTag();
        root.putInt(NbtKeys.SCHEMA, 2);
        root.put(NbtKeys.WORN, worn);
        CompoundTag placed = new CompoundTag();
        placed.put(FRAME_B.toString(), placedRecordTag(stateTag()));
        CompoundTag noPos = placedRecordTag(stateTag());
        noPos.remove(NbtKeys.POS);
        placed.put(UUID.randomUUID().toString(), noPos);
        placed.put("not-a-frame-id", placedRecordTag(stateTag()));
        root.put(NbtKeys.PLACED, placed);
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

    private static FrameStateSavedData empty() {
        return FrameStateSavedData.load(new CompoundTag(), FrameStateCodec::read);
    }

    private static FrameStateSavedData roundTrip(FrameStateSavedData data) {
        return FrameStateSavedData.load(data.write(new CompoundTag()), FrameStateCodec::read);
    }

    @Test
    void schema2RoundTripsWornAndPlaced() {
        FrameStateSavedData data = empty();
        FrameRecord worn = data.getOrCreateWorn(GOOD, FrameStateSavedDataTest::state);
        UUID placedId = data.place(new FrameRecord(FRAME_B, state()), OVERWORLD_SPOT);
        assertEquals(FRAME_B, placedId);

        CompoundTag written = data.write(new CompoundTag());
        assertEquals(2, written.getInt(NbtKeys.SCHEMA));
        assertFalse(written.contains("frames"));
        CompoundTag wornTag = written.getCompound(NbtKeys.WORN).getCompound(GOOD.toString());
        assertEquals(worn.frameId(), wornTag.getUUID(NbtKeys.ID));
        CompoundTag placedTag = written.getCompound(NbtKeys.PLACED).getCompound(FRAME_B.toString());
        assertEquals("minecraft:overworld", placedTag.getString(NbtKeys.DIM));

        FrameStateSavedData again = FrameStateSavedData.load(written, FrameStateCodec::read);
        assertEquals(worn.frameId(), again.worn(GOOD).frameId());
        assertEquals(FrameStateCodec.write(worn.state()), FrameStateCodec.write(again.worn(GOOD).state()));
        assertEquals(Set.of(GOOD), again.frames().keySet());
        assertEquals(FRAME_B, again.placed(FRAME_B).frameId());
        assertEquals(stateTag(), FrameStateCodec.write(again.placed(FRAME_B).state()));
        assertEquals(OVERWORLD_SPOT, again.spot(FRAME_B));
        assertEquals(written, again.write(new CompoundTag()));
    }

    @Test
    void enterMovesTheSameRecord() {
        FrameStateSavedData data = empty();
        FrameRecord record = new FrameRecord(FRAME_A, state());
        data.place(record, OVERWORLD_SPOT);

        assertTrue(data.enter(GOOD, FRAME_A));
        assertSame(record, data.worn(GOOD));
        assertNull(data.placed(FRAME_A));
        assertNull(data.spot(FRAME_A));
        assertTrue(data.placedIn("minecraft:overworld").isEmpty());
        assertSame(record.state(), data.frames().get(GOOD));
        assertFalse(data.enter(OTHER, FRAME_A));

        CompoundTag written = data.write(new CompoundTag());
        assertTrue(written.getCompound(NbtKeys.PLACED).isEmpty());
        assertEquals(Set.of(GOOD.toString()), written.getCompound(NbtKeys.WORN).getAllKeys());
    }

    @Test
    void enterFailsWhenPlayerAlreadyWearsOne() {
        FrameStateSavedData data = empty();
        FrameRecord worn = data.getOrCreateWorn(GOOD, FrameStateSavedDataTest::state);
        FrameRecord placed = new FrameRecord(FRAME_A, state());
        data.place(placed, OVERWORLD_SPOT);

        assertFalse(data.enter(GOOD, FRAME_A));
        assertSame(worn, data.worn(GOOD));
        assertSame(placed, data.placed(FRAME_A));
        assertEquals(OVERWORLD_SPOT, data.spot(FRAME_A));
        assertFalse(data.enter(OTHER, UUID.randomUUID()));
        assertNull(data.worn(OTHER));
    }

    @Test
    void exitMovesBackWithTheSpot() {
        FrameStateSavedData data = empty();
        FrameRecord worn = data.getOrCreateWorn(GOOD, FrameStateSavedDataTest::state);

        assertEquals(Optional.of(worn.frameId()), data.exit(GOOD, NETHER_SPOT));
        assertNull(data.worn(GOOD));
        assertTrue(data.frames().isEmpty());
        assertSame(worn, data.placed(worn.frameId()));
        assertEquals(NETHER_SPOT, data.spot(worn.frameId()));
        assertEquals(Optional.empty(), data.exit(GOOD, NETHER_SPOT));

        assertTrue(data.enter(OTHER, worn.frameId()));
        assertSame(worn, data.worn(OTHER));
    }

    @Test
    void placedInFiltersByDimension() {
        FrameStateSavedData data = empty();
        data.place(new FrameRecord(FRAME_A, state()), OVERWORLD_SPOT);
        data.place(new FrameRecord(FRAME_B, state()), NETHER_SPOT);
        UUID third = UUID.randomUUID();
        data.place(new FrameRecord(third, state()), new PlacedSpot("minecraft:overworld", 0, 70, 0, 0));

        assertEquals(List.of(FRAME_A, third), data.placedIn("minecraft:overworld"));
        assertEquals(List.of(FRAME_B), data.placedIn("minecraft:the_nether"));
        assertTrue(data.placedIn("minecraft:the_end").isEmpty());
        assertNull(data.place(new FrameRecord(FRAME_A, state()), NETHER_SPOT));
        assertEquals(OVERWORLD_SPOT, data.spot(FRAME_A));

        assertTrue(data.removePlaced(FRAME_A));
        assertFalse(data.removePlaced(FRAME_A));
        assertEquals(List.of(third), data.placedIn("minecraft:overworld"));
    }

    @Test
    void schema1FileBecomesWornRecords() {
        CompoundTag frames = new CompoundTag();
        frames.put(GOOD.toString(), stateTag());
        frames.put(OTHER.toString(), stateTag());
        CompoundTag corrupt = stateTag();
        corrupt.putBoolean("test_corrupt", true);
        frames.put(BAD.toString(), corrupt);
        CompoundTag root = new CompoundTag();
        root.putInt(NbtKeys.SCHEMA, 1);
        root.put("frames", frames);

        FrameStateSavedData data = loadThrowingOnMarked(root);
        assertEquals(Set.of(GOOD, OTHER), data.frames().keySet());
        assertNotNull(data.worn(GOOD).frameId());
        assertNotEquals(data.worn(GOOD).frameId(), data.worn(OTHER).frameId());
        assertEquals(stateTag(), FrameStateCodec.write(data.worn(GOOD).state()));
        assertTrue(data.worn(GOOD).stowed().isEmpty());

        CompoundTag written = data.write(new CompoundTag());
        assertEquals(2, written.getInt(NbtKeys.SCHEMA));
        assertEquals(Set.of(GOOD.toString(), OTHER.toString()), written.getCompound(NbtKeys.WORN).getAllKeys());
        // la voce di schema 1 che non si legge resta com'era, nella sezione di schema 1
        assertEquals(Set.of(BAD.toString()), written.getCompound("frames").getAllKeys());
        assertEquals(corrupt, written.getCompound("frames").get(BAD.toString()));

        FrameStateSavedData again = loadThrowingOnMarked(written);
        assertEquals(data.worn(GOOD).frameId(), again.worn(GOOD).frameId());
        assertEquals(written, again.write(new CompoundTag()));
        again.dropUnread(BAD);
        assertFalse(again.write(new CompoundTag()).contains("frames"));
    }

    @Test
    void writeStampsTheSchemaAndAbsentSchemaReadsAsOne() {
        CompoundTag frames = new CompoundTag();
        frames.put(GOOD.toString(), stateTag());
        CompoundTag root = new CompoundTag();
        root.put("frames", frames);
        FrameStateSavedData data = FrameStateSavedData.load(root, FrameStateCodec::read);
        assertEquals(Set.of(GOOD), data.frames().keySet());
        CompoundTag written = data.write(new CompoundTag());
        assertEquals(FrameStateSavedData.CURRENT_SCHEMA, written.getInt(NbtKeys.SCHEMA));
        assertEquals(2, FrameStateSavedData.CURRENT_SCHEMA);

        FrameStateSavedData again = FrameStateSavedData.load(written, FrameStateCodec::read);
        assertEquals(Set.of(GOOD), again.frames().keySet());
    }

    @Test
    void duplicateFrameIdStaysUnread() {
        CompoundTag worn = new CompoundTag();
        worn.put(GOOD.toString(), wornRecordTag(FRAME_A, stateTag()));
        CompoundTag placed = new CompoundTag();
        CompoundTag duplicatePlaced = placedRecordTag(stateTag());
        placed.put(FRAME_A.toString(), duplicatePlaced);
        placed.put(FRAME_B.toString(), placedRecordTag(stateTag()));
        CompoundTag root = new CompoundTag();
        root.putInt(NbtKeys.SCHEMA, 2);
        root.put(NbtKeys.WORN, worn);
        root.put(NbtKeys.PLACED, placed);

        FrameStateSavedData data = FrameStateSavedData.load(root, FrameStateCodec::read);
        assertEquals(FRAME_A, data.worn(GOOD).frameId());
        assertNull(data.placed(FRAME_A));
        assertNotNull(data.placed(FRAME_B));
        assertEquals(List.of(FRAME_B), data.placedIn("minecraft:overworld"));
        CompoundTag written = data.write(new CompoundTag());
        assertEquals(duplicatePlaced, written.getCompound(NbtKeys.PLACED).get(FRAME_A.toString()));

        // due indossati con la stessa identita': uno solo si legge, l'altro resta identico fra i non letti
        CompoundTag twice = new CompoundTag();
        twice.put(GOOD.toString(), wornRecordTag(FRAME_A, stateTag()));
        twice.put(OTHER.toString(), wornRecordTag(FRAME_A, stateTag()));
        CompoundTag root2 = new CompoundTag();
        root2.putInt(NbtKeys.SCHEMA, 2);
        root2.put(NbtKeys.WORN, twice);
        FrameStateSavedData data2 = FrameStateSavedData.load(root2, FrameStateCodec::read);
        assertEquals(1, data2.frames().size());
        assertEquals(twice, data2.write(new CompoundTag()).getCompound(NbtKeys.WORN));
    }

    @Test
    void stowedTagsSurviveARoundTrip() {
        FrameStateSavedData data = empty();
        FrameRecord worn = data.getOrCreateWorn(GOOD, FrameStateSavedDataTest::state);
        worn.stowed().add(stowedEntry("head", "minecraft:iron_helmet"));
        worn.stowed().add(stowedEntry("feet", "minecraft:leather_boots"));
        FrameRecord placed = new FrameRecord(FRAME_A, state(), List.of(stowedEntry("chest", "minecraft:elytra")));
        data.place(placed, OVERWORLD_SPOT);

        FrameStateSavedData again = roundTrip(data);
        assertEquals(List.of(stowedEntry("head", "minecraft:iron_helmet"), stowedEntry("feet", "minecraft:leather_boots")),
                again.worn(GOOD).stowed());
        assertEquals(List.of(stowedEntry("chest", "minecraft:elytra")), again.placed(FRAME_A).stowed());
        again.placed(FRAME_A).stowed().clear();
        assertTrue(roundTrip(again).placed(FRAME_A).stowed().isEmpty());

        // uno stowed che non e' una lista di compound non si legge: il record resta intero fra i non letti
        CompoundTag root = new CompoundTag();
        CompoundTag wornSection = new CompoundTag();
        CompoundTag broken = wornRecordTag(FRAME_B, stateTag());
        broken.putString(NbtKeys.STOWED, "not a list");
        wornSection.put(OTHER.toString(), broken);
        root.put(NbtKeys.WORN, wornSection);
        FrameStateSavedData withBroken = FrameStateSavedData.load(root, FrameStateCodec::read);
        assertNull(withBroken.worn(OTHER));
        assertEquals(broken, withBroken.write(new CompoundTag()).getCompound(NbtKeys.WORN).get(OTHER.toString()));
    }

    @Test
    void skippedEntriesAreWrittenBackVerbatim() {
        CompoundTag root = mixedRoot();
        CompoundTag original = root.copy();
        FrameStateSavedData data = loadThrowingOnMarked(root);
        assertEquals(Set.of(GOOD), data.frames().keySet());
        assertEquals(List.of(FRAME_B), data.placedIn("minecraft:overworld"));

        // le voci lette si riscrivono uguali (stesso codec), quelle saltate identiche
        assertEquals(original, data.write(new CompoundTag()));
    }

    @Test
    void unreadEntryIsDroppedOnlyForThatPlayer() {
        FrameStateSavedData data = loadThrowingOnMarked(mixedRoot());
        data.dropUnread(BAD);
        data.dropUnread(GOOD);
        CompoundTag worn = data.write(new CompoundTag()).getCompound(NbtKeys.WORN);
        assertFalse(worn.contains(BAD.toString()));
        assertTrue(worn.contains(GOOD.toString()));
        assertTrue(worn.contains("not-a-uuid"));
        assertTrue(worn.contains("00000000-0000-0000-0000-000000000003"));
    }

    @Test
    void enterRefusedWhileThePlayerHasAnUnreadWornRecord() {
        FrameStateSavedData data = loadThrowingOnMarked(mixedRoot());
        FrameRecord placed = new FrameRecord(UUID.randomUUID(), state());
        data.place(placed, OVERWORLD_SPOT);
        assertFalse(data.enter(BAD, placed.frameId()));
        assertSame(placed, data.placed(placed.frameId()));
        data.dropUnread(BAD);
        assertTrue(data.enter(BAD, placed.frameId()));
    }

    /** Prova ogni punto di modifica del dato e controlla che il salvataggio riproduca l'ingresso. */
    private static void assertReadOnlyAndUnchanged(CompoundTag root) {
        CompoundTag original = root.copy();
        FrameStateSavedData data = FrameStateSavedData.load(root, FrameStateCodec::read);
        assertTrue(data.readOnly());
        assertTrue(data.frames().isEmpty());
        assertNull(data.worn(GOOD));
        assertNull(data.placed(FRAME_A));
        assertTrue(data.placedIn("minecraft:overworld").isEmpty());

        assertNull(data.getOrCreateWorn(GOOD, FrameStateSavedDataTest::state));
        assertNull(data.getOrCreateWorn(UUID.randomUUID(), FrameStateSavedDataTest::state));
        assertFalse(data.removeWorn(GOOD));
        assertNull(data.place(new FrameRecord(UUID.randomUUID(), state()), OVERWORLD_SPOT));
        assertFalse(data.removePlaced(FRAME_A));
        assertFalse(data.enter(OTHER, FRAME_A));
        assertEquals(Optional.empty(), data.exit(GOOD, OVERWORLD_SPOT));
        data.dropUnread(BAD);
        data.dropUnread(GOOD);
        data.setDirty();
        assertFalse(data.isDirty());
        assertTrue(data.frames().isEmpty());

        assertEquals(original, data.write(new CompoundTag()));
    }

    @Test
    void newerSchemaIsStillReadOnly() {
        CompoundTag root = mixedRoot();
        root.putInt(NbtKeys.SCHEMA, 3);
        root.getCompound(NbtKeys.PLACED).put(FRAME_A.toString(), placedRecordTag(stateTag()));
        root.putString("added_by_schema_3", "kept");
        assertReadOnlyAndUnchanged(root);
    }

    @Test
    void nonNumericSchemaIsReadOnly() {
        CompoundTag root = mixedRoot();
        root.putString(NbtKeys.SCHEMA, "2");
        assertReadOnlyAndUnchanged(root);
    }

    @Test
    void currentSchemaIsWritable() {
        CompoundTag root = new CompoundTag();
        root.put(NbtKeys.WORN, new CompoundTag());
        root.put(NbtKeys.PLACED, new CompoundTag());
        root.putInt(NbtKeys.SCHEMA, 2);
        FrameStateSavedData data = FrameStateSavedData.load(root, FrameStateCodec::read);
        assertFalse(data.readOnly());
        FrameRecord created = data.getOrCreateWorn(GOOD, FrameStateSavedDataTest::state);
        assertSame(created, data.getOrCreateWorn(GOOD, FrameStateSavedDataTest::state));
        assertEquals(Set.of(GOOD), data.frames().keySet());
        data.setDirty();
        assertTrue(data.isDirty());
        assertTrue(data.removeWorn(GOOD));
        assertFalse(data.removeWorn(GOOD));
        assertTrue(data.frames().isEmpty());
    }
}
