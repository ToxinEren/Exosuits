package com.toxineren.exoframes.station;

import com.toxineren.exoframes.RedstoneExoframes;
import java.util.List;

/**
 * Le chiavi dei testi della stazione di manutenzione che il codice tiene in costanti (design M4 §5.16): quelle della
 * schermata e il messaggio della stazione che non e' riuscita a cambiare il telaio. Stanno nel codice comune perche'
 * il test in mondo dei testi le cerchi nei due file di lingua ({@link #KEYS}) senza nominare il codice del client.
 */
public final class StationTexts {
    private static final String GUI = "gui." + RedstoneExoframes.MOD_ID + ".";

    /** Il nome della cella, nell'intestazione e nel suggerimento. */
    public static final String CELL = GUI + "station.cell";
    /** L'intestazione della scelta di tutti i pezzi. */
    public static final String ALL_PIECES = GUI + "station.all_pieces";
    /** Il suggerimento di un posto senza pezzo (o senza cella), con il suo nome: "Torso: vuoto". */
    public static final String EMPTY_SLOT = GUI + "station.empty_slot";
    /** La durata e la durata massima nell'intestazione: "81 / 180". */
    public static final String DURABILITY = GUI + "station.durability";
    /** Il suggerimento dell'anteprima. */
    public static final String ROTATE = GUI + "station.rotate";
    /** L'etichetta del pulsante che sceglie tutti i pezzi montati. */
    public static final String ALL = GUI + "station.all";
    /** "Vuoto", sotto il nome di un posto vuoto scelto, nell'intestazione. */
    public static final String EMPTY = GUI + "station.empty";
    /** Il suggerimento della freccia che smonta la scelta. */
    public static final String UNMOUNT = GUI + "station.unmount";
    /** La riga di un telaio che custodisce un'armatura, nella riga del titolo. */
    public static final String HOLDS_ARMOR = GUI + "station.holds_armor";
    /** Il suggerimento della riga dell'armatura custodita: come la si ritira. */
    public static final String HOLDS_ARMOR_HOW = GUI + "station.holds_armor_how";
    /** La prima riga del suggerimento del lucchetto chiuso: il telaio e' bloccato. */
    public static final String LOCKED = GUI + "station.locked";
    /** La prima riga del suggerimento del lucchetto aperto: il telaio e' sbloccato. */
    public static final String UNLOCKED = GUI + "station.unlocked";
    /** La riga del suggerimento del lucchetto per chi non puo' bloccare o sbloccare il telaio. */
    public static final String OWNER_ONLY = GUI + "station.owner_only";
    /** La riga del suggerimento del lucchetto quando la regola {@code server.ownerOnlyFrames} del server e' spenta. */
    public static final String EVERYONE = GUI + "station.everyone";
    /** Il suggerimento dello scudo del telaio rinforzato. */
    public static final String REINFORCED = GUI + "station.reinforced";
    /** Il nome di una condizione: la chiave continua con il nome della condizione in minuscolo. */
    public static final String CONDITION_PREFIX = GUI + "condition.";
    /**
     * Il messaggio sulla barra delle azioni quando il registro rifiuta il cambio di un'azione della stazione (design
     * §5.6): nulla e' stato preso e nulla dato.
     */
    public static final String FAILED = "message." + RedstoneExoframes.MOD_ID + ".station.failed";

    /** Tutte le chiavi qui sopra, con i quattro nomi delle condizioni al posto del loro prefisso. */
    public static final List<String> KEYS = List.of(CELL, ALL_PIECES, EMPTY_SLOT, DURABILITY, ROTATE, ALL, EMPTY,
            UNMOUNT, HOLDS_ARMOR, HOLDS_ARMOR_HOW, LOCKED, UNLOCKED, OWNER_ONLY, EVERYONE, REINFORCED,
            CONDITION_PREFIX + "intact", CONDITION_PREFIX + "damaged", CONDITION_PREFIX + "critical",
            CONDITION_PREFIX + "broken", FAILED);

    private StationTexts() {
    }
}
