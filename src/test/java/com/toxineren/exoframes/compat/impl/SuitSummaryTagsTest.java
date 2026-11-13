package com.toxineren.exoframes.compat.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.SuitSummary.PieceLook;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Decal;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

/** Riassunto visivo nel tag radice dello stack (solo classi NBT, nessun bootstrap). */
class SuitSummaryTagsTest {

    private static final Appearance PAINTED = new Appearance(0x123456, 0xABCDEF,
            List.of(new Decal("minecraft:creeper", "red")));

    private static SuitSummary roundTrip(SuitSummary summary) {
        CompoundTag root = new CompoundTag();
        SuitSummaryTags.write(root, summary);
        return SuitSummaryTags.read(root).orElseThrow();
    }

    /** Un tag radice con un riassunto scritto a mano: una voce per slot con livello e condizione dati. */
    private static CompoundTag rootWith(Map<String, CompoundTag> entries) {
        CompoundTag pieces = new CompoundTag();
        entries.forEach(pieces::put);
        CompoundTag visual = new CompoundTag();
        visual.put(NbtKeys.PIECES, pieces);
        CompoundTag root = new CompoundTag();
        root.put(NbtKeys.VISUAL, visual);
        return root;
    }

    private static CompoundTag entry(String tier, String condition) {
        CompoundTag tag = new CompoundTag();
        tag.putString(NbtKeys.TIER, tier);
        if (condition != null) {
            tag.putString(NbtKeys.CONDITION, condition);
        }
        return tag;
    }

    @Test
    void roundTripKeepsEveryPiece() {
        Map<PieceSlot, PieceLook> pieces = new EnumMap<>(PieceSlot.class);
        PieceCondition[] conditions = PieceCondition.values();
        for (PieceSlot slot : PieceSlot.values()) {
            Appearance appearance = slot == PieceSlot.TORSO ? PAINTED : Appearance.DEFAULT;
            pieces.put(slot, new PieceLook(Tier.SCRAP, conditions[slot.ordinal() % conditions.length], appearance));
        }
        SuitSummary summary = new SuitSummary(pieces);
        assertEquals(summary, roundTrip(summary));
        assertEquals(6, roundTrip(summary).pieces().size());
    }

    @Test
    void secondWriteReplacesTheFirst() {
        Map<PieceSlot, PieceLook> six = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            six.put(slot, new PieceLook(Tier.ARMORED, PieceCondition.INTACT, PAINTED));
        }
        assertEquals(6, six.size());
        SuitSummary two = new SuitSummary(Map.of(
                PieceSlot.HELMET, new PieceLook(Tier.SCRAP, PieceCondition.BROKEN, Appearance.DEFAULT),
                PieceSlot.LEFT_LEG, new PieceLook(Tier.PROTOTYPE, PieceCondition.DAMAGED, Appearance.DEFAULT)));

        CompoundTag root = new CompoundTag();
        SuitSummaryTags.write(root, new SuitSummary(six));
        SuitSummaryTags.write(root, two);
        assertEquals(Optional.of(two), SuitSummaryTags.read(root));
        assertEquals(2, root.getCompound(NbtKeys.VISUAL).getCompound(NbtKeys.PIECES).size());
    }

    @Test
    void bareFrameRoundTrips() {
        CompoundTag root = new CompoundTag();
        SuitSummaryTags.write(root, SuitSummary.BARE);
        assertTrue(root.contains(NbtKeys.VISUAL));
        assertEquals(Optional.of(SuitSummary.BARE), SuitSummaryTags.read(root));
    }

    @Test
    void missingVisualIsEmpty() {
        CompoundTag root = new CompoundTag();
        root.putInt(NbtKeys.ENERGY, 5);
        assertEquals(Optional.empty(), SuitSummaryTags.read(root));
    }

    @Test
    void visualOfWrongTypeIsEmpty() {
        CompoundTag root = new CompoundTag();
        root.putString(NbtKeys.VISUAL, "not a compound");
        assertEquals(Optional.empty(), SuitSummaryTags.read(root));
        root.put(NbtKeys.VISUAL, new ListTag());
        assertEquals(Optional.empty(), SuitSummaryTags.read(root));

        // visual compound ma con pieces o voci di tipo sbagliato: riassunto presente, pezzi saltati
        CompoundTag visual = new CompoundTag();
        visual.putInt(NbtKeys.PIECES, 7);
        root.put(NbtKeys.VISUAL, visual);
        assertEquals(Optional.of(SuitSummary.BARE), SuitSummaryTags.read(root));
        CompoundTag pieces = new CompoundTag();
        pieces.putInt("helmet", 3);
        CompoundTag wrongTier = new CompoundTag();
        wrongTier.putInt(NbtKeys.TIER, 1);
        pieces.put("torso", wrongTier);
        visual.put(NbtKeys.PIECES, pieces);
        assertEquals(Optional.of(SuitSummary.BARE), SuitSummaryTags.read(root));
    }

    @Test
    void unknownTierSkipsOnlyThatPiece() {
        Map<String, CompoundTag> entries = new HashMap<>();
        entries.put("helmet", entry("mythril", "intact"));
        entries.put("torso", entry(Tier.ARMORED.id(), "damaged"));
        entries.put("tail", entry(Tier.SCRAP.id(), "intact"));
        SuitSummary read = SuitSummaryTags.read(rootWith(entries)).orElseThrow();
        assertEquals(Map.of(PieceSlot.TORSO, new PieceLook(Tier.ARMORED, PieceCondition.DAMAGED, Appearance.DEFAULT)),
                read.pieces());
    }

    @Test
    void unknownConditionReadsAsIntact() {
        Map<String, CompoundTag> entries = new HashMap<>();
        entries.put("helmet", entry(Tier.SCRAP.id(), "melted"));
        entries.put("torso", entry(Tier.SCRAP.id(), null));
        CompoundTag wrongType = entry(Tier.SCRAP.id(), null);
        wrongType.put(NbtKeys.CONDITION, IntTag.valueOf(3));
        entries.put("left_arm", wrongType);
        entries.put("right_arm", entry(Tier.SCRAP.id(), "broken"));
        SuitSummary read = SuitSummaryTags.read(rootWith(entries)).orElseThrow();
        assertEquals(PieceCondition.INTACT, read.pieces().get(PieceSlot.HELMET).condition());
        assertEquals(PieceCondition.INTACT, read.pieces().get(PieceSlot.TORSO).condition());
        assertEquals(PieceCondition.INTACT, read.pieces().get(PieceSlot.LEFT_ARM).condition());
        assertEquals(PieceCondition.BROKEN, read.pieces().get(PieceSlot.RIGHT_ARM).condition());
    }

    @Test
    void ofFrameStateCopiesTierConditionAndAppearance() {
        FrameState state = new FrameState(ExoframesConfig.defaults());
        state.mount(new Piece(PieceSlot.HELMET, Tier.ARMORED, 400, 400));
        state.mount(new Piece(PieceSlot.TORSO, Tier.SCRAP, 100, 30));
        state.mount(new Piece(PieceSlot.LEFT_LEG, Tier.PROTOTYPE, 200, 0));
        state.setAppearance(PieceSlot.TORSO, PAINTED);

        SuitSummary summary = SuitSummary.of(state);
        assertEquals(Map.of(
                PieceSlot.HELMET, new PieceLook(Tier.ARMORED, PieceCondition.INTACT, Appearance.DEFAULT),
                PieceSlot.TORSO, new PieceLook(Tier.SCRAP, PieceCondition.DAMAGED, PAINTED),
                PieceSlot.LEFT_LEG, new PieceLook(Tier.PROTOTYPE, PieceCondition.BROKEN, Appearance.DEFAULT)),
                summary.pieces());
        assertEquals(SuitSummary.BARE, SuitSummary.of(new FrameState(ExoframesConfig.defaults())));

        // la mappa e' una copia non modificabile
        state.unmount(PieceSlot.HELMET);
        assertTrue(summary.pieces().containsKey(PieceSlot.HELMET));
        assertThrows(UnsupportedOperationException.class, () -> summary.pieces().remove(PieceSlot.TORSO));
        assertFalse(SuitSummary.of(state).pieces().containsKey(PieceSlot.HELMET));
    }

    @Test
    void writeKeepsOtherRootKeysAndAppearanceKeys() {
        CompoundTag root = new CompoundTag();
        root.putInt(NbtKeys.ENERGY, 42);
        SuitSummaryTags.write(root, new SuitSummary(
                Map.of(PieceSlot.TORSO, new PieceLook(Tier.SCRAP, PieceCondition.CRITICAL, PAINTED))));
        assertEquals(42, root.getInt(NbtKeys.ENERGY));
        CompoundTag torso = root.getCompound(NbtKeys.VISUAL).getCompound(NbtKeys.PIECES).getCompound("torso");
        assertEquals("scrap", torso.getString(NbtKeys.TIER));
        assertEquals("critical", torso.getString(NbtKeys.CONDITION));
        assertEquals(0x123456, torso.getInt(NbtKeys.COLOR_PRIMARY));
        assertEquals(StringTag.valueOf("red"),
                torso.getList(NbtKeys.DECALS, 10).getCompound(0).get(NbtKeys.COLOR));
    }
}
