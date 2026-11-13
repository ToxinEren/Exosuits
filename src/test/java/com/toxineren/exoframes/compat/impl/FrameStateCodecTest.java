package com.toxineren.exoframes.compat.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Decal;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.energy.EnergyCell;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.core.module.ModuleInstaller;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

/** Round trip e lettura tollerante dello schema NBT dello stato del telaio (solo classi NBT, nessun bootstrap). */
class FrameStateCodecTest {

    private final ExoframesConfig cfg = ExoframesConfig.defaults();

    private FrameState populated() {
        FrameState state = new FrameState(cfg);
        state.mount(new Piece(PieceSlot.HELMET, Tier.ARMORED, 400, 321));
        state.mount(new Piece(PieceSlot.TORSO, Tier.ARMORED, 500, 17));
        state.mount(new Piece(PieceSlot.LEFT_LEG, Tier.SCRAP, 100, 100));
        assertEquals(ModuleInstaller.Reason.OK, state.installModule(PieceSlot.HELMET, ModuleType.NIGHT_VISION));
        assertEquals(ModuleInstaller.Reason.OK, state.installModule(PieceSlot.HELMET, ModuleType.SCANNER));
        assertEquals(ModuleInstaller.Reason.OK, state.installModule(PieceSlot.TORSO, ModuleType.ENERGY_SHIELD));
        assertEquals(ModuleInstaller.Reason.OK, state.installModule(PieceSlot.LEFT_LEG, ModuleType.SERVOS));
        state.damageCarryMilli().put(PieceSlot.HELMET, 250);
        state.damageCarryMilli().put(PieceSlot.TORSO, 999);
        state.insertCell(new EnergyCell(12_000, 4_321));
        state.activeToggles().add(ModuleType.NIGHT_VISION);
        state.activeToggles().add(ModuleType.ENERGY_SHIELD);
        state.setReinforced(true);
        state.setOwner(UUID.fromString("12345678-9abc-def0-1234-56789abcdef0"));
        state.setUnlocked(true);
        return state;
    }

    private static void assertSameState(FrameState expected, FrameState actual) {
        assertEquals(expected.pieces().keySet(), actual.pieces().keySet());
        for (Map.Entry<PieceSlot, Piece> e : expected.pieces().entrySet()) {
            Piece a = actual.pieces().get(e.getKey());
            assertEquals(e.getValue().slot(), a.slot());
            assertEquals(e.getValue().tier(), a.tier());
            assertEquals(e.getValue().durability(), a.durability());
            assertEquals(e.getValue().maxDurability(), a.maxDurability());
        }
        assertEquals(expected.modules(), actual.modules());
        assertEquals(expected.damageCarryMilli(), actual.damageCarryMilli());
        for (PieceSlot slot : PieceSlot.values()) {
            assertEquals(expected.appearance(slot), actual.appearance(slot));
        }
        if (expected.cell() == null) {
            assertNull(actual.cell());
        } else {
            assertEquals(expected.cell().capacity(), actual.cell().capacity());
            assertEquals(expected.cell().stored(), actual.cell().stored());
        }
        assertEquals(expected.activeToggles(), actual.activeToggles());
        assertEquals(expected.reinforced(), actual.reinforced());
        assertEquals(expected.owner(), actual.owner());
        assertEquals(expected.unlocked(), actual.unlocked());
    }

    @Test
    void populatedStateRoundTrips() {
        FrameState state = populated();
        assertSameState(state, FrameStateCodec.read(FrameStateCodec.write(state), cfg));
    }

    @Test
    void appearanceRoundTripsAfterMount() {
        FrameState state = populated();
        Appearance torso = new Appearance(0x112233, Appearance.UNPAINTED,
                List.of(new Decal("minecraft:stripe_bottom", "red"), new Decal("minecraft:creeper", "black")));
        state.setAppearance(PieceSlot.TORSO, torso);
        CompoundTag tag = FrameStateCodec.write(state);
        CompoundTag torsoTag = tag.getCompound(NbtKeys.PIECES).getCompound("torso");
        assertEquals(0x112233, torsoTag.getInt(NbtKeys.COLOR_PRIMARY));
        assertFalse(torsoTag.contains(NbtKeys.COLOR_SECONDARY));
        FrameState read = FrameStateCodec.read(tag, cfg);
        assertEquals(torso, read.appearance(PieceSlot.TORSO));
        assertEquals(Appearance.DEFAULT, read.appearance(PieceSlot.HELMET));
        assertSameState(state, read);
    }

    @Test
    void emptyStateRoundTrips() {
        FrameState state = new FrameState(cfg);
        FrameState read = FrameStateCodec.read(FrameStateCodec.write(state), cfg);
        assertSameState(state, read);
        assertTrue(read.pieces().isEmpty());
        assertNull(read.owner());
    }

    @Test
    void carryOutOfRangeIsClamped() {
        CompoundTag tag = FrameStateCodec.write(populated());
        CompoundTag pieces = tag.getCompound(NbtKeys.PIECES);
        pieces.getCompound("helmet").putInt(NbtKeys.CARRY_MILLI, 5_000);
        pieces.getCompound("torso").putInt(NbtKeys.CARRY_MILLI, -7);
        FrameState read = FrameStateCodec.read(tag, cfg);
        assertEquals(999, read.damageCarryMilli().get(PieceSlot.HELMET));
        assertEquals(0, read.damageCarryMilli().getOrDefault(PieceSlot.TORSO, 0));
    }

    @Test
    void unknownIdsAreIgnored() {
        CompoundTag tag = FrameStateCodec.write(populated());
        CompoundTag pieces = tag.getCompound(NbtKeys.PIECES);
        pieces.put("tail", pieces.getCompound("helmet").copy());
        pieces.getCompound("left_leg").putString(NbtKeys.TIER, "adamantium");
        ListTag helmetModules = pieces.getCompound("helmet").getList(NbtKeys.MODULES, 8);
        helmetModules.add(StringTag.valueOf("laser_eyes"));
        tag.getList(NbtKeys.ACTIVE, 8).add(StringTag.valueOf("cloak"));
        FrameState read = FrameStateCodec.read(tag, cfg);
        assertEquals(Set.of(PieceSlot.HELMET, PieceSlot.TORSO), read.pieces().keySet());
        assertEquals(List.of(ModuleType.NIGHT_VISION, ModuleType.SCANNER), read.modules().get(PieceSlot.HELMET));
        assertEquals(Set.of(ModuleType.NIGHT_VISION, ModuleType.ENERGY_SHIELD), read.activeToggles());
    }

    @Test
    void togglesAreRestoredOnlyForInstalledToggleModules() {
        CompoundTag tag = FrameStateCodec.write(populated());
        ListTag active = tag.getList(NbtKeys.ACTIVE, 8);
        active.add(StringTag.valueOf(ModuleType.SCANNER.id()));
        active.add(StringTag.valueOf(ModuleType.SERVOS.id()));
        active.add(StringTag.valueOf(ModuleType.JETPACK.id()));
        FrameState read = FrameStateCodec.read(tag, cfg);
        // SCANNER e' un toggle installato; SERVOS e' installato ma passivo; JETPACK non e' installato
        assertEquals(Set.of(ModuleType.NIGHT_VISION, ModuleType.ENERGY_SHIELD, ModuleType.SCANNER),
                read.activeToggles());

        CompoundTag bare = FrameStateCodec.write(new FrameState(cfg));
        bare.getList(NbtKeys.ACTIVE, 8).add(StringTag.valueOf(ModuleType.NIGHT_VISION.id()));
        assertTrue(FrameStateCodec.read(bare, cfg).activeToggles().isEmpty());
    }

    @Test
    void pieceEntriesUseTheSharedReader() {
        CompoundTag tag = FrameStateCodec.write(populated());
        CompoundTag pieces = tag.getCompound(NbtKeys.PIECES);
        pieces.getCompound("helmet").putInt(NbtKeys.DURABILITY, 9_999);
        pieces.getCompound("torso").putInt(NbtKeys.MAX_DURABILITY, 0);
        ListTag legModules = pieces.getCompound("left_leg").getList(NbtKeys.MODULES, 8);
        legModules.add(StringTag.valueOf(ModuleType.SERVOS.id()));
        legModules.add(StringTag.valueOf(ModuleType.SHOCKWAVE.id()));
        FrameState read = FrameStateCodec.read(tag, cfg);
        assertEquals(400, read.pieces().get(PieceSlot.HELMET).durability());
        assertFalse(read.pieces().containsKey(PieceSlot.TORSO));
        List<ModuleType> leg = read.modules().get(PieceSlot.LEFT_LEG);
        assertEquals(leg.size(), leg.stream().distinct().count());
        assertEquals(ModuleType.SERVOS, leg.get(0));
    }

    @Test
    void trustedReadKeepsModulesTheReaderConfigWouldRefuse() {
        ExoframesConfig writer = ExoframesConfig.defaults();
        writer.pieces.baseModuleSlots.put(PieceSlot.TORSO, 2);
        ExoframesConfig reader = ExoframesConfig.defaults();
        reader.pieces.baseModuleSlots.put(PieceSlot.TORSO, 0);
        FrameState state = new FrameState(writer);
        state.mount(Piece.fresh(PieceSlot.TORSO, Tier.SCRAP, writer.tiers));
        assertEquals(ModuleInstaller.Reason.OK, state.installModule(PieceSlot.TORSO, ModuleType.JETPACK));
        state.restoreModules(PieceSlot.TORSO, List.of(ModuleType.JETPACK, ModuleType.ENERGY_SHIELD));
        state.activeToggles().add(ModuleType.ENERGY_SHIELD);
        CompoundTag tag = FrameStateCodec.write(state);

        FrameState trusted = FrameStateCodec.readTrusted(tag, reader);
        assertSameState(state, trusted);
        assertEquals(List.of(ModuleType.JETPACK, ModuleType.ENERGY_SHIELD), trusted.modules().get(PieceSlot.TORSO));

        FrameState validated = FrameStateCodec.read(tag, reader);
        assertTrue(validated.modules().getOrDefault(PieceSlot.TORSO, List.of()).isEmpty());
        assertTrue(validated.activeToggles().isEmpty());
    }

    @Test
    void trustedReadDropsDuplicateModules() {
        CompoundTag tag = FrameStateCodec.write(populated());
        ListTag helmetModules = tag.getCompound(NbtKeys.PIECES).getCompound("helmet").getList(NbtKeys.MODULES, 8);
        helmetModules.add(StringTag.valueOf(ModuleType.SCANNER.id()));
        helmetModules.add(StringTag.valueOf(ModuleType.NIGHT_VISION.id()));
        helmetModules.add(StringTag.valueOf("laser_eyes"));
        FrameState read = FrameStateCodec.readTrusted(tag, cfg);
        assertEquals(List.of(ModuleType.NIGHT_VISION, ModuleType.SCANNER), read.modules().get(PieceSlot.HELMET));
        assertSameState(populated(), read);
    }

    @Test
    void malformedOwnerIsAbsent() {
        CompoundTag tag = FrameStateCodec.write(populated());
        tag.put(NbtKeys.OWNER, new IntArrayTag(new int[] {1, 2, 3}));
        FrameState read = FrameStateCodec.read(tag, cfg);
        assertNull(read.owner());
        assertTrue(read.unlocked());
    }

    @Test
    void schemaReadUuidRequiresFourInts() {
        CompoundTag tag = new CompoundTag();
        assertFalse(SchemaIds.readUuid(tag, NbtKeys.OWNER).isPresent());
        tag.put(NbtKeys.OWNER, new IntArrayTag(new int[] {1, 2, 3}));
        assertFalse(SchemaIds.readUuid(tag, NbtKeys.OWNER).isPresent());
        tag.put(NbtKeys.OWNER, new IntArrayTag(new int[] {1, 2, 3, 4, 5}));
        assertFalse(SchemaIds.readUuid(tag, NbtKeys.OWNER).isPresent());
        tag.putString(NbtKeys.OWNER, "not-an-array");
        assertFalse(SchemaIds.readUuid(tag, NbtKeys.OWNER).isPresent());
        tag.put(NbtKeys.OWNER, new IntArrayTag(new int[] {1, 2, 3, 4}));
        assertEquals(new UUID(0x0000000100000002L, 0x0000000300000004L),
                SchemaIds.readUuid(tag, NbtKeys.OWNER).orElseThrow());
    }

    @Test
    void savedDataWithMalformedOwnerKeepsBothEntries() {
        UUID good = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID odd = UUID.fromString("00000000-0000-0000-0000-000000000002");
        CompoundTag frames = new CompoundTag();
        frames.put(good.toString(), FrameStateCodec.write(populated()));
        CompoundTag malformed = FrameStateCodec.write(populated());
        malformed.put(NbtKeys.OWNER, new IntArrayTag(new int[] {1, 2, 3}));
        frames.put(odd.toString(), malformed);
        CompoundTag root = new CompoundTag();
        root.put("frames", frames);
        FrameStateSavedData data = FrameStateSavedData.load(root, FrameStateCodec::read);
        assertSameState(populated(), data.frames().get(good));
        assertNull(data.frames().get(odd).owner());
    }

    @Test
    void savedDataSkipsOnlyTheEntryWhoseReadThrows() {
        UUID good = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID bad = UUID.fromString("00000000-0000-0000-0000-000000000002");
        CompoundTag frames = new CompoundTag();
        frames.put(good.toString(), FrameStateCodec.write(populated()));
        CompoundTag corrupt = FrameStateCodec.write(populated());
        corrupt.putBoolean("test_corrupt", true);
        frames.put(bad.toString(), corrupt);
        frames.put("not-a-uuid", FrameStateCodec.write(populated()));
        CompoundTag root = new CompoundTag();
        root.put("frames", frames);
        FrameStateSavedData data = FrameStateSavedData.load(root, (tag, config) -> {
            if (tag.getBoolean("test_corrupt")) {
                throw new IllegalStateException("corrupt entry");
            }
            return FrameStateCodec.read(tag, config);
        });
        assertEquals(Set.of(good), data.frames().keySet());
        assertSameState(populated(), data.frames().get(good));
    }
}
