package com.toxineren.exoframes.compat;

/**
 * Chiavi dello schema NBT degli item della mod, tutte sotto il tag radice {@link #ROOT}.
 * <p>
 * E' l'unica fonte delle chiavi: 1.20.1 (tag dello stack) e 1.21.1 (componente vanilla custom_data) leggono e
 * scrivono lo stesso CompoundTag con queste chiavi, cosi' un mondo 1.20.1 aggiornato a 1.21.1 (il DFU di vanilla
 * migra il tag dello stack in custom_data) conserva energia, durabilita' e il resto dei dati.
 * <p>
 * Lo stato del telaio per giocatore (FrameStateCodec in compat/impl) riusa le stesse chiavi e gli stessi tipi per
 * pezzi, cella, proprietario e flag, cosi' i due codec restano allineati; le chiavi {@link #CAPACITY},
 * {@link #ACTIVE} e {@link #CARRY_MILLI} esistono solo nello stato del telaio.
 */
public final class NbtKeys {
    /** Tag radice che contiene tutti i dati della mod sullo stack. */
    public static final String ROOT = "exoframes";
    /** Energia immagazzinata (int). */
    public static final String ENERGY = "energy";
    /** Id del livello del pezzo (string, vedi Tier#id). */
    public static final String TIER = "tier";
    /** Durabilita' corrente (int). */
    public static final String DURABILITY = "durability";
    /** Durabilita' massima (int). */
    public static final String MAX_DURABILITY = "max_durability";
    /** Moduli installati (lista di string, vedi ModuleType#id). */
    public static final String MODULES = "modules";
    /** Colore primario (int RGB). */
    public static final String COLOR_PRIMARY = "color_primary";
    /** Colore secondario (int RGB). */
    public static final String COLOR_SECONDARY = "color_secondary";
    /** Proprietario del telaio (UUID in forma di int array). */
    public static final String OWNER = "owner";
    /** Telaio sbloccato (boolean). */
    public static final String UNLOCKED = "unlocked";
    /** Telaio rinforzato (boolean). */
    public static final String REINFORCED = "reinforced";
    /**
     * Cella installata nel telaio (tag annidato con {@link #ENERGY}; nello stato del telaio per giocatore anche con
     * {@link #CAPACITY}).
     */
    public static final String CELL = "cell";
    /** Pezzi montati sul telaio (tag con una voce per id di PieceSlot in minuscolo). */
    public static final String PIECES = "pieces";
    /** Item riposti (lista di tag di item). */
    public static final String STOWED = "stowed";
    /**
     * Capacita' della cella (int), dentro il tag {@link #CELL} dello stato del telaio per giocatore: serve a
     * ricostruire la cella con capacita' e carica.
     */
    public static final String CAPACITY = "capacity";
    /** Moduli toggle accesi dal giocatore (lista di string, vedi ModuleType#id), nello stato del telaio. */
    public static final String ACTIVE = "active";
    /**
     * Riporto frazionario del danno dello slot in millesimi di punto (int in [0, 999]), dentro la voce del pezzo in
     * {@link #PIECES} dello stato del telaio; assente vale 0.
     */
    public static final String CARRY_MILLI = "carry_milli";

    private NbtKeys() {
    }
}
