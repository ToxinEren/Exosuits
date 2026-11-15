package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.frame.FrameState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

/**
 * Le operazioni del registro dei telai: i record vivi e le voci che il file non ha potuto leggere. Il file e il
 * salvataggio stanno in {@link FrameStateSavedData}, la lettura e la scrittura del tag in {@link FrameRegistryFormat}.
 * <p>
 * Ogni telaio e' un {@link FrameRecord} (identita', stato, armatura stivata) e sta in una sola delle due mappe: gli
 * indossati per giocatore ({@link NbtKeys#WORN}) e i piazzati per identita' ({@link NbtKeys#PLACED}), con dimensione,
 * posizione e orientamento ({@link PlacedSpot}). Entrare ({@link #enter}) e uscire ({@link #exit}) spostano lo stesso
 * record fra le due mappe dentro questo stesso file: gli spostamenti dentro il registro sono atomici, e un crash non
 * puo' duplicare ne' perdere un telaio fra le due mappe (design §4.5). L'armatura stivata qui e' una lista di tag
 * opachi.
 * <p>
 * Un record tolto dal registro ({@link #removeWorn}, {@link #removePlaced}) con armatura stivata ancora dentro, che si
 * legga o no, non la perde: le voci passano identiche in {@link NbtKeys#ORPHANED}, un gruppo per telaio con
 * l'identita' del telaio e il proprietario se noto, con un avviso che nomina il telaio. La lista si riscrive identica
 * a ogni salvataggio (un valore che non e' una lista di compound si conserva dentro un gruppo, sotto
 * {@link NbtKeys#ENTRIES}); nessuno la legge ancora per il gioco. Chi toglie un telaio riprende prima l'armatura che
 * si legge: la lista e' la rete per quello che resta.
 * <p>
 * Solo sul thread del server; i record sono vivi: ogni operazione del registro qui segna da se' il dato come da
 * salvare, e chi cambia uno stato o un'armatura stivata da fuori deve chiamare {@link #setDirty()}. Un ingresso
 * nullo ({@code place}, {@code exit}, l'armatura stivata) e' rifiutato subito con un'eccezione, cosi' il
 * salvataggio non lancia mai. Le identita' create leggendo un file di schema 1 segnano il dato come da salvare gia'
 * al caricamento, cosi' restano le stesse.
 * <p>
 * Il registro non sa nulla del file: la sola lettura, il segno di dato da salvare e quello del cambio strutturale li
 * tiene il proprietario ({@link FrameStateSavedData}), e il registro li vede solo attraverso le tre funzioni che
 * riceve.
 */
final class FrameRegistry {
    /** Telai indossati, per giocatore. */
    final Map<UUID, FrameRecord> worn = new LinkedHashMap<>();
    /** Telai piazzati, per identita'. */
    final Map<UUID, Placed> placed = new LinkedHashMap<>();
    /**
     * Voci non lette di ogni sezione, per chiave originale, riscritte identiche in {@link FrameRegistryFormat#write}.
     */
    final Map<String, Tag> unreadWorn = new LinkedHashMap<>();
    final Map<String, Tag> unreadPlaced = new LinkedHashMap<>();
    final Map<String, Tag> unreadFrames = new LinkedHashMap<>();
    /** Gruppi dell'armatura stivata rimasta in telai tolti, riscritti identici sotto {@link NbtKeys#ORPHANED}. */
    final List<CompoundTag> orphaned = new ArrayList<>();
    /** Chiavi grezze gia' segnalate come spostate accanto a un record vivo, per avvisare una volta sola. */
    final Set<String> movedWarned = new HashSet<>();

    /** Vero se il proprietario e' in sola lettura: nessuna operazione cambia nulla. */
    private final BooleanSupplier readOnly;
    /** Segna il proprietario come da salvare. */
    private final Runnable dirty;
    /** Segna il proprietario come da salvare dopo un cambio strutturale. */
    private final Runnable structural;

    /** Un record piazzato con il suo posto. */
    record Placed(FrameRecord record, PlacedSpot spot) {
    }

    FrameRegistry(BooleanSupplier readOnly, Runnable dirty, Runnable structural) {
        this.readOnly = readOnly;
        this.dirty = dirty;
        this.structural = structural;
    }

    /** Segna il dato come da salvare (vedi {@link FrameStateSavedData#setDirty()}). */
    void setDirty() {
        dirty.run();
    }

    /**
     * Segna il dato come da salvare dopo un'operazione strutturale (vedi
     * {@link FrameStateSavedData#structuralUnsaved()}).
     */
    void markStructural() {
        structural.run();
    }

    /** Vista in sola lettura giocatore -> stato del telaio indossato (una copia della mappa; gli stati sono vivi). */
    Map<UUID, FrameState> frames() {
        Map<UUID, FrameState> states = new LinkedHashMap<>();
        worn.forEach((player, record) -> states.put(player, record.state()));
        return Collections.unmodifiableMap(states);
    }

    /** Il record indossato dal giocatore, o null. */
    FrameRecord worn(UUID player) {
        return worn.get(player);
    }

    /**
     * Il record indossato dal giocatore, creato con un'identita' nuova e lo stato di {@code factory} se assente (allora
     * il dato e' da salvare, come dopo ogni operazione strutturale, vedi
     * {@link FrameStateSavedData#structuralUnsaved()}); null, senza cambiare nulla, in sola lettura.
     */
    FrameRecord getOrCreateWorn(UUID player, Supplier<FrameState> factory) {
        if (readOnly.getAsBoolean()) {
            return null;
        }
        FrameRecord record = worn.get(player);
        if (record == null) {
            record = new FrameRecord(UUID.randomUUID(), factory.get());
            worn.put(player, record);
            markStructural();
        }
        return record;
    }

    /**
     * Applica la modifica allo stato del record indossato dal giocatore, creato come in {@link #getOrCreateWorn} se
     * assente, e segna il dato come da salvare con un cambio strutturale (anche se la modifica lancia): e' la scrittura
     * rara, che sincronizza. Falso, senza cambiare nulla, in sola lettura.
     */
    boolean updateWorn(UUID player, Supplier<FrameState> factory, Consumer<FrameState> change) {
        Objects.requireNonNull(change, "change must not be null");
        FrameRecord record = getOrCreateWorn(player, factory);
        if (record == null) {
            return false;
        }
        try {
            change.accept(record.state());
        } finally {
            markStructural();
        }
        return true;
    }

    /**
     * Applica la modifica allo stato del record indossato dal giocatore, solo se esiste, e segna il dato come da
     * salvare con un cambio strutturale (anche se la modifica lancia: disco e client allineati alla memoria): e' la
     * scrittura rara della 3c (cambio cella, comando dell'energia) al posto di {@link #updateWorn}, perche' non crea
     * mai un record. Falso, senza far girare la modifica e senza cambiare nulla, se il giocatore non indossa un telaio
     * o in sola lettura.
     */
    boolean changeWorn(UUID player, Consumer<FrameState> change) {
        Objects.requireNonNull(change, "change must not be null");
        FrameRecord record = readOnly.getAsBoolean() ? null : worn.get(player);
        if (record == null) {
            return false;
        }
        try {
            change.accept(record.state());
        } finally {
            markStructural();
        }
        return true;
    }

    /**
     * Applica la modifica allo stato del record indossato dal giocatore e segna il dato come da salvare (anche se la
     * modifica lancia), ma non come cambio strutturale: e' la scrittura ad alta frequenza (l'energia a ogni tick, in
     * una parte successiva), e un salvataggio a ogni uscita di un giocatore per questo sarebbe troppo. Falso, senza
     * cambiare nulla, se il giocatore non indossa un telaio o in sola lettura.
     */
    boolean mutateWorn(UUID player, Consumer<FrameState> change) {
        Objects.requireNonNull(change, "change must not be null");
        FrameRecord record = readOnly.getAsBoolean() ? null : worn.get(player);
        if (record == null) {
            return false;
        }
        try {
            change.accept(record.state());
        } finally {
            setDirty();
        }
        return true;
    }

    /**
     * Rimuove il record indossato dal giocatore; false se non c'era o se il dato e' in sola lettura. L'armatura
     * stivata rimasta va fra le orfane ({@link #orphaned()}), con il proprietario del telaio o, se non ne ha, il
     * giocatore.
     */
    boolean removeWorn(UUID player) {
        if (readOnly.getAsBoolean()) {
            return false;
        }
        FrameRecord record = worn.remove(player);
        if (record == null) {
            return false;
        }
        UUID owner = record.state().owner();
        orphan(record, owner != null ? owner : player);
        markStructural();
        return true;
    }

    /**
     * Aggiunge il record ai piazzati nel posto dato e ne restituisce l'identita'; null, senza cambiare nulla, se
     * l'identita' e' gia' nel registro (indossata o piazzata) o se il dato e' in sola lettura.
     */
    UUID place(FrameRecord record, PlacedSpot spot) {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(spot, "spot must not be null");
        if (readOnly.getAsBoolean() || contains(record.frameId())) {
            return null;
        }
        placed.put(record.frameId(), new Placed(record, spot));
        markStructural();
        return record.frameId();
    }

    /** Il record piazzato con questa identita', o null. */
    FrameRecord placed(UUID frameId) {
        Placed entry = placed.get(frameId);
        return entry == null ? null : entry.record();
    }

    /** Il posto del telaio piazzato con questa identita', o null. */
    PlacedSpot spot(UUID frameId) {
        Placed entry = placed.get(frameId);
        return entry == null ? null : entry.spot();
    }

    /** Le identita' dei telai piazzati nella dimensione data, nell'ordine in cui sono stati piazzati o letti. */
    List<UUID> placedIn(String dimension) {
        List<UUID> ids = new ArrayList<>();
        placed.forEach((frameId, entry) -> {
            if (entry.spot().dimension().equals(dimension)) {
                ids.add(frameId);
            }
        });
        return ids;
    }

    /**
     * Rimuove il record piazzato; false se non c'era o se il dato e' in sola lettura. L'armatura stivata rimasta va
     * fra le orfane ({@link #orphaned()}), con il proprietario del telaio se ne ha uno.
     */
    boolean removePlaced(UUID frameId) {
        if (readOnly.getAsBoolean()) {
            return false;
        }
        Placed entry = placed.remove(frameId);
        if (entry == null) {
            return false;
        }
        orphan(entry.record(), entry.record().state().owner());
        markStructural();
        return true;
    }

    /**
     * Se il telaio e' piazzato, applica la modifica al suo stato e segna il dato come da salvare con un cambio
     * strutturale (anche se la modifica lancia: montaggio, smontaggio, blocco); false, senza cambiare nulla, se non
     * e' piazzato o se il dato e' in sola lettura.
     */
    boolean updatePlaced(UUID frameId, Consumer<FrameState> change) {
        Objects.requireNonNull(change, "change must not be null");
        FrameRecord record = readOnly.getAsBoolean() ? null : placed(frameId);
        if (record == null) {
            return false;
        }
        try {
            change.accept(record.state());
        } finally {
            markStructural();
        }
        return true;
    }

    /**
     * Aggiunge le voci, in ordine, all'armatura stivata del telaio indossato dal giocatore e segna il dato come da
     * salvare; false, senza cambiare nulla, se il dato e' in sola lettura.
     *
     * @throws IllegalStateException se il giocatore non indossa un telaio
     */
    boolean addStowed(UUID player, List<CompoundTag> entries) {
        List<CompoundTag> copy = List.copyOf(entries);
        if (readOnly.getAsBoolean()) {
            return false;
        }
        FrameRecord record = worn.get(player);
        if (record == null) {
            throw new IllegalStateException("player " + player + " wears no frame to stow armor in");
        }
        record.stowed().addAll(copy);
        markStructural();
        return true;
    }

    /**
     * Toglie dall'armatura stivata del telaio indossato dal giocatore le voci che {@code reader} legge (riceve
     * identita' del telaio e voce) e le restituisce, nell'ordine; quelle che non legge restano identiche. Vuota se il
     * giocatore non indossa un telaio o se il dato e' in sola lettura; segna il dato come da salvare se il record c'e'.
     */
    <T> List<T> takeStowedOfWorn(UUID player, BiFunction<UUID, CompoundTag, Optional<T>> reader) {
        return take(readOnly.getAsBoolean() ? null : worn.get(player), reader);
    }

    /** Come {@link #takeStowedOfWorn}, per il telaio piazzato con questa identita'. */
    <T> List<T> takeStowedOfPlaced(UUID frameId, BiFunction<UUID, CompoundTag, Optional<T>> reader) {
        return take(readOnly.getAsBoolean() ? null : placed(frameId), reader);
    }

    private <T> List<T> take(FrameRecord record, BiFunction<UUID, CompoundTag, Optional<T>> reader) {
        if (record == null) {
            return List.of();
        }
        List<T> taken = new ArrayList<>();
        try {
            for (Iterator<CompoundTag> it = record.stowed().iterator(); it.hasNext(); ) {
                Optional<T> read = reader.apply(record.frameId(), it.next());
                if (read.isPresent()) {
                    taken.add(read.get());
                    it.remove();
                }
            }
        } finally {
            markStructural();
        }
        return taken;
    }

    /** Copie dei gruppi dell'armatura stivata rimasta in telai tolti, nell'ordine in cui sono stati tolti o letti. */
    List<CompoundTag> orphaned() {
        List<CompoundTag> copies = new ArrayList<>();
        orphaned.forEach(group -> copies.add(group.copy()));
        return copies;
    }

    /**
     * Toglie i gruppi orfani del telaio con questa identita' e dice quanti erano. Solo per i test (vedi
     * {@code GameTests.forgetOrphaned}): nel gioco le voci orfane non si tolgono mai.
     */
    int forgetOrphaned(UUID frameId) {
        int before = orphaned.size();
        orphaned.removeIf(group -> SchemaIds.readUuid(group, NbtKeys.FRAME).filter(frameId::equals).isPresent());
        if (orphaned.size() != before) {
            setDirty();
        }
        return before - orphaned.size();
    }

    /** Mette fra le orfane, identiche, le voci stivate rimaste nel record tolto, con un avviso; nulla se non ce ne sono. */
    private void orphan(FrameRecord record, UUID owner) {
        if (record.stowed().isEmpty()) {
            return;
        }
        ListTag entries = new ListTag();
        record.stowed().forEach(entry -> entries.add(entry.copy()));
        CompoundTag group = new CompoundTag();
        group.put(NbtKeys.FRAME, NbtUtils.createUUID(record.frameId()));
        if (owner != null) {
            group.put(NbtKeys.OWNER, NbtUtils.createUUID(owner));
        }
        group.put(NbtKeys.ENTRIES, entries);
        orphaned.add(group);
        RedstoneExoframes.LOGGER.warn("Frame {} (owner {}) left the registry with {} stowed armor entries still inside: "
                + "kept in the saved data under '{}'", record.frameId(), owner == null ? "none" : owner, entries.size(),
                NbtKeys.ORPHANED);
    }

    /**
     * Sposta il record piazzato fra gli indossati del giocatore (lo stesso oggetto). False, senza cambiare nulla, se
     * il record non e' piazzato, se il giocatore ne indossa gia' uno o se il dato e' in sola lettura. Una voce
     * indossata non letta del giocatore non lo impedisce: resta nel file accanto al record (vedi
     * {@link FrameRegistryFormat}).
     */
    boolean enter(UUID player, UUID frameId) {
        if (readOnly.getAsBoolean() || worn.containsKey(player) || !placed.containsKey(frameId)) {
            return false;
        }
        worn.put(player, placed.remove(frameId).record());
        markStructural();
        return true;
    }

    /**
     * Sposta il record indossato dal giocatore fra i piazzati nel posto dato (lo stesso oggetto) e ne restituisce
     * l'identita'; vuoto, senza cambiare nulla, se il giocatore non ne indossa uno o se il dato e' in sola lettura.
     */
    Optional<UUID> exit(UUID player, PlacedSpot spot) {
        Objects.requireNonNull(spot, "spot must not be null");
        if (readOnly.getAsBoolean() || !worn.containsKey(player)) {
            return Optional.empty();
        }
        FrameRecord record = worn.remove(player);
        placed.put(record.frameId(), new Placed(record, spot));
        markStructural();
        return Optional.of(record.frameId());
    }

    private boolean contains(UUID frameId) {
        if (placed.containsKey(frameId)) {
            return true;
        }
        for (FrameRecord record : worn.values()) {
            if (record.frameId().equals(frameId)) {
                return true;
            }
        }
        return false;
    }
}
