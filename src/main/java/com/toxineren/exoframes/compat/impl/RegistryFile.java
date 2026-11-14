package com.toxineren.exoframes.compat.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
//? if >=1.21 {
import net.minecraft.nbt.NbtAccounter;
//?}

/**
 * Il file del registro dei telai sul disco ({@link FrameStateSavedData}): come si scrive senza mai lasciare un file a
 * meta' al posto di quello buono, e che cosa si fa quando il gioco non ha potuto leggerlo. Solo file e tag: nessuna
 * classe del gioco oltre a NBT, cosi' i test JUnit la provano su cartelle temporanee.
 * <p>
 * Scrittura ({@link #write}): il tag va in un file temporaneo accanto, che si forza sul disco; poi il file in uso, se
 * c'e', si copia accanto come copia precedente ({@code <nome>_old}, come fa il gioco per i file dei giocatori), e il
 * temporaneo prende il suo posto con uno spostamento atomico. In ogni momento il file in uso e' intero, il vecchio o il
 * nuovo; se la scrittura fallisce il file in uso e la copia precedente restano come erano.
 * <p>
 * Lettura dopo un fallimento del gioco ({@link #recover}): il gioco legge il file da se' e, se non ci riesce, lo dice
 * solo nel log e da' alla mod un dato vuoto. Allora si guarda la copia precedente: se si legge, il file rovinato si
 * mette da parte con un nome che dice che cos'e' e si usa quella; se non si legge nemmeno quella, il file rovinato si
 * copia da parte e resta dov'e', e chi chiama tiene il registro in sola lettura, cosi' nulla lo sovrascrive. Un file
 * messo da parte non si sovrascrive e non si cancella mai.
 */
final class RegistryFile {
    /** Suffisso della copia precedente, accanto al file in uso. */
    static final String OLD_SUFFIX = "_old";
    /** Suffisso, seguito da data e ora, di un file che non si e' potuto leggere, messo da parte. */
    static final String UNREADABLE_SUFFIX = ".unreadable-";

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    private RegistryFile() {
    }

    /** Chi scrive il tag radice nel flusso; il flusso non va chiuso (lo chiude {@link #write}). */
    interface TagWriter {
        void write(CompoundTag root, OutputStream out) throws IOException;
    }

    /** Che cosa e' rimasto da leggere dopo che il gioco non ha letto il file. */
    enum Kind {
        /** Ne' il file ne' la copia precedente esistono: un mondo nuovo, il registro vuoto. */
        NEW,
        /** La copia precedente si legge: {@link Recovery#root()} e' il suo tag radice. */
        PREVIOUS,
        /** Nulla si legge: il registro resta in sola lettura per questa sessione. */
        UNREADABLE
    }

    /**
     * L'esito di {@link #recover}: il tipo, il tag radice della copia precedente (solo per {@link Kind#PREVIOUS}) e
     * dove e' stato messo da parte il file che non si leggeva (null se non c'era o non si e' potuto).
     */
    record Recovery(Kind kind, CompoundTag root, Path keptAs) {
        Recovery {
            Objects.requireNonNull(kind, "kind must not be null");
        }
    }

    /** La copia precedente del file: {@code <nome>_old} accanto. */
    static Path oldCopy(Path file) {
        return file.resolveSibling(file.getFileName() + OLD_SUFFIX);
    }

    /** Scrive il tag radice compresso, come il gioco scrive i dati salvati, con le garanzie della classe. */
    static void write(Path file, CompoundTag root) throws IOException {
        write(file, root, NbtIo::writeCompressed);
    }

    /** Come {@link #write(Path, CompoundTag)}, con chi scrive il tag dato (i test ne passano uno che fallisce). */
    static void write(Path file, CompoundTag root, TagWriter writer) throws IOException {
        Objects.requireNonNull(root, "root must not be null");
        Path target = file.toAbsolutePath();
        Path folder = target.getParent();
        Files.createDirectories(folder);
        Path temporary = Files.createTempFile(folder, target.getFileName() + "-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                OutputStream stream = new FilterOutputStream(Channels.newOutputStream(channel)) {
                    @Override
                    public void write(byte[] bytes, int offset, int length) throws IOException {
                        out.write(bytes, offset, length);
                    }

                    @Override
                    public void close() throws IOException {
                        // chi scrive il tag chiude il suo flusso: il canale resta aperto per forzarlo sul disco
                        flush();
                    }
                };
                writer.write(root, stream);
                stream.flush();
                channel.force(true);
            }
            if (Files.exists(target)) {
                Files.copy(target, oldCopy(target), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /** Legge il tag radice compresso del file; lancia se il file manca o non si legge. */
    static CompoundTag read(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            //? if >=1.21 {
            return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            //?} else {
            /*return NbtIo.readCompressed(in);*/
            //?}
        }
    }

    /**
     * Che cosa usare quando il gioco non ha letto il file (vedi la classe). Se il file manca e la copia precedente si
     * legge, si usa quella senza mettere da parte nulla. Se la copia precedente si legge ma il file rovinato non si puo'
     * spostare, l'esito e' {@link Kind#UNREADABLE}: meglio la sola lettura che riscrivere sopra le prove.
     */
    static Recovery recover(Path file) {
        Path old = oldCopy(file);
        boolean exists = Files.exists(file);
        if (!exists && !Files.exists(old)) {
            return new Recovery(Kind.NEW, null, null);
        }
        CompoundTag previous = null;
        if (Files.exists(old)) {
            try {
                previous = read(old);
            } catch (IOException | RuntimeException e) {
                previous = null;
            }
        }
        if (previous != null) {
            if (!exists) {
                return new Recovery(Kind.PREVIOUS, previous, null);
            }
            Path kept = keepAside(file, true);
            if (kept != null) {
                return new Recovery(Kind.PREVIOUS, previous, kept);
            }
        }
        return new Recovery(Kind.UNREADABLE, null, exists ? keepAside(file, false) : null);
    }

    /**
     * Mette da parte il file, spostandolo o copiandolo, sotto {@code <nome>.unreadable-<data>-<ora>} (con un numero in
     * coda se il nome e' preso): mai sopra un altro file. Null se non si e' potuto.
     */
    private static Path keepAside(Path file, boolean move) {
        String base = file.getFileName() + UNREADABLE_SUFFIX + LocalDateTime.now().format(STAMP);
        Path target = file.resolveSibling(base);
        for (int n = 2; Files.exists(target); n++) {
            target = file.resolveSibling(base + "-" + n);
        }
        try {
            if (move) {
                Files.move(file, target);
            } else {
                Files.copy(file, target);
            }
            return target;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
