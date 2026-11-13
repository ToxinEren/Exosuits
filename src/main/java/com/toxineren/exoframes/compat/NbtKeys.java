package com.toxineren.exoframes.compat;

/**
 * Chiavi dello schema NBT degli item della mod, tutte sotto il tag radice {@link #ROOT}.
 * <p>
 * E' l'unica fonte delle chiavi: 1.20.1 (tag dello stack) e 1.21.1 (componente vanilla custom_data) leggono e
 * scrivono lo stesso CompoundTag con queste chiavi, cosi' un mondo 1.20.1 aggiornato a 1.21.1 (il DFU di vanilla
 * copia il tag dello stack in custom_data cosi' com'e') conserva energia, durabilita' e il resto dei dati primitivi.
 * Gli stack annidati (gli item riposti di {@link #STOWED}) non vengono convertiti da vanilla e avranno bisogno di una
 * migrazione esplicita.
 * <p>
 * Lo stato del telaio per giocatore (FrameStateCodec in compat/impl) riusa le stesse chiavi e gli stessi tipi per
 * pezzi (aspetto compreso), cella, proprietario e flag, cosi' i due codec restano allineati; le chiavi {@link #CAPACITY},
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
    /** Colore primario (int RGB, ridotto a 24 bit in lettura); assente = non dipinto (Appearance#UNPAINTED). */
    public static final String COLOR_PRIMARY = "color_primary";
    /** Colore secondario (int RGB, ridotto a 24 bit in lettura); assente = non dipinto (Appearance#UNPAINTED). */
    public static final String COLOR_SECONDARY = "color_secondary";
    /**
     * Decalcomanie del pezzo (lista di compound con {@link #PATTERN} e {@link #COLOR}, al massimo
     * Appearance#MAX_DECALS); assente = nessuna.
     */
    public static final String DECALS = "decals";
    /** Motivo di una decalcomania (string non vuota, al massimo 64 caratteri), dentro una voce di {@link #DECALS}. */
    public static final String PATTERN = "pattern";
    /** Colore di una decalcomania (string non vuota, al massimo 64 caratteri), dentro una voce di {@link #DECALS}. */
    public static final String COLOR = "color";
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
    /**
     * Item riposti (lista di tag di item). Definita ma non ancora scritta ne' letta: arriva con la Milestone 3
     * (ingresso e uscita dal telaio). Gli stack staranno accanto al FrameState nella voce del giocatore del dato
     * salvato, salvati con il provider su 1.21.1, letti e scritti con due metodi di FrameStateAccess e non inviati al
     * client (vedi docs/compat.md, "Limiti noti e rinvii").
     */
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

    /**
     * Versione dello schema delle voci (int), accanto a {@code frames} nel dato salvato degli stati del telaio;
     * assente vale 1. Non c'e' marcatore sugli item.
     */
    public static final String SCHEMA = "schema";

    private NbtKeys() {
    }
}
