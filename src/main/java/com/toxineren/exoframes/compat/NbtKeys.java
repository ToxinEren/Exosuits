package com.toxineren.exoframes.compat;

/**
 * Chiavi dello schema NBT degli item della mod, tutte sotto il tag radice {@link #ROOT}.
 * <p>
 * E' l'unica fonte delle chiavi: 1.20.1 (tag dello stack) e 1.21.1 (componente vanilla custom_data) leggono e
 * scrivono lo stesso CompoundTag con queste chiavi, cosi' un mondo 1.20.1 aggiornato a 1.21.1 (il DFU di vanilla
 * copia il tag dello stack in custom_data cosi' com'e') conserva energia, durabilita' e il resto dei dati primitivi.
 * Gli stack annidati (l'armatura stivata di {@link #STOWED}, nel dato salvato) non vengono convertiti da vanilla:
 * portano accanto {@link #DATA_VERSION} per passare dal correttore di dati in lettura.
 * <p>
 * Lo stato del telaio per giocatore (FrameStateCodec in compat/impl) riusa le stesse chiavi e gli stessi tipi per
 * pezzi (aspetto compreso), cella, proprietario e flag, cosi' i due codec restano allineati; le chiavi {@link #CAPACITY},
 * {@link #ACTIVE} e {@link #CARRY_MILLI} esistono solo nello stato del telaio. Il registro dei telai del dato salvato
 * (FrameStateSavedData) ha le sue chiavi, dalla radice del file: {@link #SCHEMA}, {@link #WORN}, {@link #PLACED} e
 * quelle dei record.
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
     * Armatura stivata di un telaio (lista di compound con {@link #SLOT}, {@link #DATA_VERSION} e {@link #ITEM}),
     * dentro ogni record del registro dei telai del dato salvato (voci di {@link #WORN} e di {@link #PLACED}), accanto
     * a {@link #STATE}; assente vale vuota. Mai inviata al client. Il dato salvato la conserva come tag opachi; gli
     * stack si scrivono e si leggono (con il provider su 1.21.1 e il correttore di dati vanilla per le voci di una
     * versione piu' vecchia) nel livello che li trasforma in ItemStack.
     */
    public static final String STOWED = "stowed";
    /** Slot di provenienza di una voce di {@link #STOWED} (string: il nome dell'EquipmentSlot in minuscolo). */
    public static final String SLOT = "slot";
    /** Versione dei dati di Minecraft (int) con cui e' stato scritto lo stack di una voce di {@link #STOWED}. */
    public static final String DATA_VERSION = "data_version";
    /** Lo stack di una voce di {@link #STOWED} (compound nel formato degli stack della versione che l'ha scritto). */
    public static final String ITEM = "item";
    /**
     * Telai indossati nel dato salvato (compound con una voce per giocatore, chiave = UUID in forma testuale; ogni
     * voce ha {@link #ID}, {@link #STATE} e {@link #STOWED}).
     */
    public static final String WORN = "worn";
    /**
     * Telai piazzati nel mondo nel dato salvato (compound con una voce per identita' del telaio, chiave = UUID in
     * forma testuale; ogni voce ha {@link #STATE}, {@link #STOWED}, {@link #DIM}, {@link #POS} e {@link #YAW}).
     */
    public static final String PLACED = "placed";
    /** Identita' del telaio (UUID in forma di int array), in una voce di {@link #WORN}. */
    public static final String ID = "id";
    /** Stato del telaio (compound con le chiavi dello stato del telaio), in un record del registro dei telai. */
    public static final String STATE = "state";
    /** Dimensione di un telaio piazzato (string: id della dimensione, es. {@code minecraft:overworld}). */
    public static final String DIM = "dim";
    /** Posizione di un telaio piazzato (lista di tre double: x, y, z). */
    public static final String POS = "pos";
    /** Orientamento di un telaio piazzato (float, gradi). */
    public static final String YAW = "yaw";
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
     * Riassunto visivo del telaio indossato (compound con {@link #PIECES}: una voce per id di PieceSlot in minuscolo
     * con {@link #TIER}, {@link #CONDITION} e le chiavi dell'aspetto), nel tag radice del segnaposto del torso; vedi
     * SuitSummary. Assente = nessun riassunto.
     */
    public static final String VISUAL = "visual";
    /**
     * Stato visivo di un pezzo (string: il nome di PieceCondition in minuscolo, {@code intact}, {@code damaged},
     * {@code critical} o {@code broken}), dentro una voce del riassunto {@link #VISUAL}; assente o sconosciuto =
     * {@code intact}.
     */
    public static final String CONDITION = "condition";

    /**
     * Versione dello schema del dato salvato del registro dei telai (int), accanto a {@link #WORN} e {@link #PLACED}
     * (schema 2; lo schema 1 aveva solo {@code frames}, uno stato per giocatore); assente vale 1. Non c'e' marcatore
     * sugli item.
     */
    public static final String SCHEMA = "schema";

    private NbtKeys() {
    }
}
