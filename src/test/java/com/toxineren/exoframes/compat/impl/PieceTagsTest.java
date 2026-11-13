package com.toxineren.exoframes.compat.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.compat.ItemDataAccess.PieceData;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Decal;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

/** Lettore condiviso dei tag di pezzo e cella (solo classi NBT, nessun bootstrap). */
class PieceTagsTest {

    private static CompoundTag piece(int durability, int max) {
        CompoundTag tag = new CompoundTag();
        tag.putString(NbtKeys.TIER, Tier.ARMORED.id());
        tag.putInt(NbtKeys.DURABILITY, durability);
        tag.putInt(NbtKeys.MAX_DURABILITY, max);
        return tag;
    }

    private static ListTag ids(String... ids) {
        ListTag list = new ListTag();
        for (String id : ids) {
            list.add(StringTag.valueOf(id));
        }
        return list;
    }

    @Test
    void validPieceIsReadAsIs() {
        CompoundTag tag = piece(120, 400);
        tag.put(NbtKeys.MODULES, ids("night_vision", "scanner"));
        tag.putInt(NbtKeys.COLOR_PRIMARY, 0x123456);
        tag.putInt(NbtKeys.COLOR_SECONDARY, 0xABCDEF);
        assertEquals(new PieceData(Tier.ARMORED, 120, 400, List.of(ModuleType.NIGHT_VISION, ModuleType.SCANNER),
                new Appearance(0x123456, 0xABCDEF, List.of())), PieceTags.readPiece(tag).orElseThrow());
    }

    @Test
    void durabilityIsClampedIntoRange() {
        assertEquals(0, PieceTags.readPiece(piece(-1, 400)).orElseThrow().durability());
        assertEquals(400, PieceTags.readPiece(piece(410, 400)).orElseThrow().durability());
        assertEquals(400, PieceTags.readPiece(piece(Integer.MAX_VALUE, 400)).orElseThrow().maxDurability());
    }

    @Test
    void nonPositiveMaxDurabilityGivesNoPiece() {
        assertTrue(PieceTags.readPiece(piece(10, 0)).isEmpty());
        assertTrue(PieceTags.readPiece(piece(10, -5)).isEmpty());
        CompoundTag onlyTier = new CompoundTag();
        onlyTier.putString(NbtKeys.TIER, Tier.SCRAP.id());
        assertTrue(PieceTags.readPiece(onlyTier).isEmpty());
    }

    @Test
    void wrongTagTypesFallBackToDefaults() {
        CompoundTag tierAsInt = piece(10, 100);
        tierAsInt.putInt(NbtKeys.TIER, 3);
        assertTrue(PieceTags.readPiece(tierAsInt).isEmpty());

        CompoundTag maxAsString = piece(10, 100);
        maxAsString.putString(NbtKeys.MAX_DURABILITY, "100");
        assertTrue(PieceTags.readPiece(maxAsString).isEmpty());

        CompoundTag odd = piece(10, 100);
        odd.putString(NbtKeys.DURABILITY, "10");
        ListTag intModules = new ListTag();
        intModules.add(IntTag.valueOf(1));
        odd.put(NbtKeys.MODULES, intModules);
        odd.putString(NbtKeys.COLOR_PRIMARY, "red");
        PieceData read = PieceTags.readPiece(odd).orElseThrow();
        assertEquals(0, read.durability());
        assertEquals(List.of(), read.modules());
        assertEquals(Appearance.UNPAINTED, read.appearance().primary());

        CompoundTag modulesAsString = piece(10, 100);
        modulesAsString.putString(NbtKeys.MODULES, "jetpack");
        assertEquals(List.of(), PieceTags.readPiece(modulesAsString).orElseThrow().modules());
    }

    @Test
    void modulesAreDeduplicatedInOrderAndCapped() {
        CompoundTag tag = piece(10, 100);
        ListTag list = ids("scanner", "night_vision", "scanner", "laser_eyes", "night_vision");
        for (int i = 0; i < 3; i++) {
            for (ModuleType module : ModuleType.values()) {
                list.add(StringTag.valueOf(module.id()));
            }
        }
        tag.put(NbtKeys.MODULES, list);
        List<ModuleType> modules = PieceTags.readPiece(tag).orElseThrow().modules();
        assertEquals(ModuleType.values().length, modules.size());
        assertEquals(List.of(ModuleType.SCANNER, ModuleType.NIGHT_VISION, ModuleType.JETPACK), modules.subList(0, 3));
        assertEquals(modules.size(), modules.stream().distinct().count());
    }

    @Test
    void colorsAreMaskedTo24Bits() {
        CompoundTag tag = piece(10, 100);
        tag.putInt(NbtKeys.COLOR_PRIMARY, 0xFF123456);
        tag.putInt(NbtKeys.COLOR_SECONDARY, -1);
        PieceData read = PieceTags.readPiece(tag).orElseThrow();
        assertEquals(0x123456, read.appearance().primary());
        assertEquals(0xFFFFFF, read.appearance().secondary());
    }

    private static CompoundTag decal(String pattern, String color) {
        CompoundTag tag = new CompoundTag();
        tag.putString(NbtKeys.PATTERN, pattern);
        tag.putString(NbtKeys.COLOR, color);
        return tag;
    }

    @Test
    void missingColorsReadAsUnpainted() {
        PieceData read = PieceTags.readPiece(piece(10, 100)).orElseThrow();
        assertEquals(Appearance.DEFAULT, read.appearance());

        CompoundTag tag = piece(10, 100);
        PieceTags.writeAppearance(tag, new Appearance(0x00AA00, Appearance.UNPAINTED, List.of()));
        assertTrue(tag.contains(NbtKeys.COLOR_PRIMARY));
        assertFalse(tag.contains(NbtKeys.COLOR_SECONDARY));
        assertEquals(new Appearance(0x00AA00, Appearance.UNPAINTED, List.of()),
                PieceTags.readPiece(tag).orElseThrow().appearance());
    }

    @Test
    void decalsRoundTrip() {
        Appearance appearance = new Appearance(0, 0xFFFFFF,
                List.of(new Decal("minecraft:stripe_bottom", "red"), new Decal("minecraft:creeper", "black")));
        CompoundTag tag = piece(10, 100);
        PieceTags.writeAppearance(tag, appearance);
        assertEquals(appearance, PieceTags.readAppearance(tag));
        assertEquals(appearance, PieceTags.readPiece(tag).orElseThrow().appearance());

        // riscrivere l'aspetto predefinito sullo stesso tag toglie colori e decalcomanie
        PieceTags.writeAppearance(tag, Appearance.DEFAULT);
        assertFalse(tag.contains(NbtKeys.COLOR_PRIMARY));
        assertFalse(tag.contains(NbtKeys.DECALS));
        assertEquals(Appearance.DEFAULT, PieceTags.readAppearance(tag));
    }

    @Test
    void malformedDecalsAreSkipped() {
        ListTag decals = new ListTag();
        decals.add(decal("minecraft:stripe_bottom", "red"));
        decals.add(decal("", "red"));
        decals.add(decal("minecraft:creeper", ""));
        decals.add(decal("x".repeat(65), "red"));
        decals.add(decal("minecraft:creeper", "c".repeat(65)));
        CompoundTag patternAsInt = decal("ignored", "blue");
        patternAsInt.putInt(NbtKeys.PATTERN, 7);
        decals.add(patternAsInt);
        CompoundTag noColor = new CompoundTag();
        noColor.putString(NbtKeys.PATTERN, "minecraft:cross");
        decals.add(noColor);
        decals.add(decal("minecraft:cross", "white"));
        CompoundTag tag = piece(10, 100);
        tag.put(NbtKeys.DECALS, decals);
        assertEquals(List.of(new Decal("minecraft:stripe_bottom", "red"), new Decal("minecraft:cross", "white")),
                PieceTags.readAppearance(tag).decals());

        CompoundTag stringList = piece(10, 100);
        stringList.put(NbtKeys.DECALS, ids("minecraft:creeper"));
        assertEquals(List.of(), PieceTags.readAppearance(stringList).decals());
        CompoundTag notAList = piece(10, 100);
        notAList.putString(NbtKeys.DECALS, "minecraft:creeper");
        assertEquals(List.of(), PieceTags.readAppearance(notAList).decals());
    }

    @Test
    void decalsAreCutAtFour() {
        ListTag decals = new ListTag();
        for (int i = 0; i < 6; i++) {
            decals.add(decal("pattern_" + i, "red"));
        }
        CompoundTag tag = piece(10, 100);
        tag.put(NbtKeys.DECALS, decals);
        List<Decal> read = PieceTags.readAppearance(tag).decals();
        assertEquals(Appearance.MAX_DECALS, read.size());
        assertEquals("pattern_0", read.get(0).pattern());
        assertEquals("pattern_3", read.get(3).pattern());
    }

    @Test
    void cellEnergyIsNeverNegative() {
        CompoundTag tag = new CompoundTag();
        assertEquals(0, PieceTags.readEnergy(tag));
        tag.putInt(NbtKeys.ENERGY, -40);
        assertEquals(0, PieceTags.readEnergy(tag));
        tag.putInt(NbtKeys.ENERGY, 7_500);
        assertEquals(7_500, PieceTags.readEnergy(tag));
        tag.putString(NbtKeys.ENERGY, "lots");
        assertEquals(0, PieceTags.readEnergy(tag));
    }
}
