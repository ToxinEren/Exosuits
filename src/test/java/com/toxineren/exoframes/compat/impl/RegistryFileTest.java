package com.toxineren.exoframes.compat.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.frame.FrameState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Il file del registro dei telai sul disco ({@link RegistryFile}) e il dato che ne nasce quando il gioco non lo ha
 * letto ({@link FrameStateSavedData#afterUnreadFile}), su cartelle temporanee.
 */
class RegistryFileTest {
    private static final UUID FRAME = UUID.fromString("00000000-0000-0000-0000-00000000000f");
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final PlacedSpot SPOT = new PlacedSpot("minecraft:overworld", 1.5, 64.0, -2.5, 90.0F);
    private static final int DATA_VERSION = 3955;

    @TempDir
    Path folder;

    private Path file() {
        return folder.resolve(FrameStateSavedData.NAME + ".dat");
    }

    private static FrameStateSavedData empty() {
        return FrameStateSavedData.load(new CompoundTag(), FrameStateCodec::read);
    }

    /** Un registro con un telaio piazzato con un elmo e uno indossato. */
    private static FrameStateSavedData registry() {
        FrameStateSavedData data = empty();
        FrameState state = new FrameState(ExoframesConfig.defaults());
        state.mount(new Piece(PieceSlot.HELMET, Tier.ARMORED, 400, 321));
        data.place(new FrameRecord(FRAME, state), SPOT);
        data.getOrCreateWorn(PLAYER, () -> new FrameState(ExoframesConfig.defaults()));
        return data;
    }

    private static CompoundTag contents(FrameStateSavedData data) {
        return data.write(new CompoundTag());
    }

    /** Il file come lo scrive il gioco ({@code DimensionDataStorage.save}): il tag radice con {@code data}. */
    private static void writeFile(Path file, FrameStateSavedData data) throws IOException {
        RegistryFile.write(file, data.fileRoot(DATA_VERSION));
    }

    private static void truncate(Path file, int size) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        Files.write(file, java.util.Arrays.copyOf(bytes, size));
    }

    private List<Path> unreadableCopies() throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(p -> p.getFileName().toString().contains(RegistryFile.UNREADABLE_SUFFIX)).toList();
        }
    }

    @Test
    void writeThenReadGivesTheSameRegistryAndKeepsThePreviousCopy() throws IOException {
        FrameStateSavedData data = registry();
        writeFile(file(), data);
        CompoundTag root = RegistryFile.read(file());
        assertEquals(DATA_VERSION, root.getInt("DataVersion"));
        assertEquals(contents(data), contents(FrameStateSavedData.load(root.getCompound("data"),
                FrameStateCodec::read)));
        assertFalse(Files.exists(RegistryFile.oldCopy(file())), "a first write has no previous copy");

        byte[] first = Files.readAllBytes(file());
        assertTrue(data.removePlaced(FRAME));
        writeFile(file(), data);
        assertArrayEquals(first, Files.readAllBytes(RegistryFile.oldCopy(file())), "previous copy is not the old file");
        assertNull(FrameStateSavedData.load(RegistryFile.read(file()).getCompound("data"), FrameStateCodec::read)
                .placed(FRAME));
        try (Stream<Path> files = Files.list(folder)) {
            assertEquals(2, files.count(), "temporary files left in the folder");
        }
    }

    /**
     * Una scrittura che si interrompe a meta' (qui chi scrive manda qualche byte e poi lancia, come un disco pieno)
     * lascia il file in uso e la copia precedente come erano, nessun temporaneo, e il dato ancora da salvare; la
     * scrittura successiva che riesce lo salva.
     */
    @Test
    void failedWriteLeavesTheFileIntactAndTheDataDirty() throws IOException {
        FrameStateSavedData data = registry();
        writeFile(file(), data);
        writeFile(file(), data);
        byte[] current = Files.readAllBytes(file());
        byte[] previous = Files.readAllBytes(RegistryFile.oldCopy(file()));
        assertTrue(data.removePlaced(FRAME));
        assertTrue(data.isDirty() && data.structuralUnsaved());

        boolean saved = data.saveTo(file(), data.fileRoot(DATA_VERSION), (root, out) -> {
            out.write(new byte[] {31, -117, 8, 0});
            throw new IOException("No space left on device");
        });

        assertFalse(saved);
        assertArrayEquals(current, Files.readAllBytes(file()), "the file was touched by a failed write");
        assertArrayEquals(previous, Files.readAllBytes(RegistryFile.oldCopy(file())), "the previous copy changed");
        assertTrue(data.isDirty() && data.structuralUnsaved(), "a failed write marked the data as saved");
        try (Stream<Path> files = Files.list(folder)) {
            assertEquals(2, files.count(), "temporary file left after a failed write");
        }

        assertTrue(data.saveTo(file(), data.fileRoot(DATA_VERSION), net.minecraft.nbt.NbtIo::writeCompressed));
        assertFalse(data.isDirty() || data.structuralUnsaved());
        assertEquals(contents(data), contents(FrameStateSavedData.load(RegistryFile.read(file())
                .getCompound("data"), FrameStateCodec::read)));
    }

    /**
     * Il file troncato (un arresto durante la scrittura) con una copia precedente buona: si usa quella, il dato e' da
     * salvare, e il file troncato e' messo da parte intero sotto un nome che dice che cos'e'.
     */
    @Test
    void truncatedFileWithAGoodPreviousCopyUsesTheCopy() throws IOException {
        FrameStateSavedData data = registry();
        writeFile(file(), data);
        writeFile(file(), data);
        truncate(file(), 40);
        byte[] damaged = Files.readAllBytes(file());
        assertThrows(IOException.class, () -> RegistryFile.read(file()));

        FrameStateSavedData empty = empty();
        FrameStateSavedData used = FrameStateSavedData.afterUnreadFile(file(), empty);

        assertNotSame(empty, used);
        assertFalse(used.readOnly());
        assertEquals(contents(data), contents(used));
        assertTrue(used.isDirty(), "the registry from the previous copy is not to be saved");
        assertFalse(Files.exists(file()), "the damaged file is still in place");
        List<Path> kept = unreadableCopies();
        assertEquals(1, kept.size(), "damaged file not kept aside: " + kept);
        assertArrayEquals(damaged, Files.readAllBytes(kept.get(0)));

        // il salvataggio successivo rimette il file e non tocca la copia precedente ne' quella messa da parte
        byte[] previous = Files.readAllBytes(RegistryFile.oldCopy(file()));
        assertTrue(used.saveTo(file(), used.fileRoot(DATA_VERSION), net.minecraft.nbt.NbtIo::writeCompressed));
        assertArrayEquals(previous, Files.readAllBytes(RegistryFile.oldCopy(file())));
        assertArrayEquals(damaged, Files.readAllBytes(kept.get(0)));
        assertEquals(contents(data), contents(FrameStateSavedData.load(RegistryFile.read(file())
                .getCompound("data"), FrameStateCodec::read)));
    }

    /**
     * Ne' il file ne' la copia precedente si leggono: il dato e' vuoto ma in sola lettura (nessuna operazione lo
     * cambia, non risulta mai da salvare), il file resta dov'e' identico e una sua copia e' messa da parte.
     */
    @Test
    void bothDamagedIsReadOnlyAndKeepsTheFile() throws IOException {
        FrameStateSavedData data = registry();
        writeFile(file(), data);
        writeFile(file(), data);
        truncate(file(), 40);
        truncate(RegistryFile.oldCopy(file()), 30);
        byte[] damaged = Files.readAllBytes(file());
        byte[] damagedOld = Files.readAllBytes(RegistryFile.oldCopy(file()));

        FrameStateSavedData empty = empty();
        FrameStateSavedData used = FrameStateSavedData.afterUnreadFile(file(), empty);

        assertSame(empty, used);
        assertTrue(used.readOnly());
        assertNull(used.place(new FrameRecord(UUID.randomUUID(), new FrameState(ExoframesConfig.defaults())), SPOT));
        assertNull(used.getOrCreateWorn(PLAYER, () -> new FrameState(ExoframesConfig.defaults())));
        used.setDirty();
        assertFalse(used.isDirty(), "a read-only registry is to be saved");
        assertArrayEquals(damaged, Files.readAllBytes(file()), "the unreadable file was changed");
        assertArrayEquals(damagedOld, Files.readAllBytes(RegistryFile.oldCopy(file())));
        List<Path> kept = unreadableCopies();
        assertEquals(1, kept.size(), "no copy of the unreadable file kept aside: " + kept);
        assertArrayEquals(damaged, Files.readAllBytes(kept.get(0)));
    }

    /** Il file manca ma la copia precedente c'e' (un arresto fra la copia e lo spostamento): si usa quella. */
    @Test
    void missingFileWithAPreviousCopyUsesTheCopy() throws IOException {
        FrameStateSavedData data = registry();
        writeFile(file(), data);
        writeFile(file(), data);
        Files.delete(file());

        FrameStateSavedData used = FrameStateSavedData.afterUnreadFile(file(), empty());

        assertEquals(contents(data), contents(used));
        assertTrue(unreadableCopies().isEmpty());
    }

    /** Senza file e senza copia precedente e' un mondo nuovo: il dato vuoto dato, scrivibile, e nessun file nuovo. */
    @Test
    void noFileIsANewWorld() throws IOException {
        FrameStateSavedData empty = empty();
        FrameStateSavedData used = FrameStateSavedData.afterUnreadFile(file(), empty);
        assertSame(empty, used);
        assertFalse(used.readOnly());
        assertNotNull(used.getOrCreateWorn(PLAYER, () -> new FrameState(ExoframesConfig.defaults())));
        try (Stream<Path> files = Files.list(folder)) {
            assertEquals(0, files.count());
        }
    }
}
