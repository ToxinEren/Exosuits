package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.core.frame.FrameState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Stato del telaio per giocatore ({@link FrameState}), con persistenza sul server e copia sincronizzata sul client.
 * <p>
 * Persistenza: un solo SavedData nel DimensionDataStorage dell'overworld, il registro dei telai (indossati per UUID
 * del giocatore, piazzati per identita' del telaio, vedi sotto). E' la
 * stessa implementazione sui quattro target (nessun attachment/capability del loader), e lo stato sopravvive alla
 * morte e al cambio di dimensione perche' non sta sull'entita'.
 * <p>
 * Sincronizzazione: il server e' l'unica autorita'. Ogni giocatore riceve solo il proprio stato, intero, con un
 * pacchetto S2C: all'ingresso nel mondo, al respawn, dopo ogni {@link #update} e dopo {@link #clear} (stato
 * assente). Il client conserva l'ultima copia ricevuta finche' non esce dal mondo. Le modifiche avvengono solo sul
 * server con i metodi di scrittura ({@link #update}, {@link #mutate}, {@link #clear}). I pacchetti C2S non portano
 * stato: chiedono un'azione (l'uscita dal telaio, {@code frame_exit}, senza contenuto), e il server decide solo in base
 * a chi li manda e a questo registro.
 * <p>
 * Modifiche ad alta frequenza (per esempio il consumo di energia a ogni tick): {@link #mutate} segna il dato come da
 * salvare senza sincronizzare, e il chiamante invia lo stato con {@link #syncTo} al proprio ritmo (al massimo 4-5
 * volte al secondo).
 * <p>
 * Registro dei telai (design §4.5 e §5.3): ogni telaio ha un'identita' ({@code frameId}) e sta in uno solo dei due
 * posti, indossato da un giocatore o piazzato nel mondo ({@link PlacedFrame}); {@link #enter} e {@link #exit} spostano
 * lo stesso record fra i due nello stesso dato salvato: gli spostamenti dentro il registro sono atomici, e un crash
 * non puo' duplicare ne' perdere un telaio fra i due posti. Cio' che passa fra un inventario e il registro (armatura
 * stivata e restituita, pezzi, celle, il telaio come oggetto) e' sul disco ogni volta che lo e' il file di un
 * giocatore: all'uscita di un giocatore, se il registro ha cambi strutturali non salvati, si salvano tutti i giocatori
 * e il registro insieme, e i salvataggi del gioco scrivono gli uni e l'altro. Resta solo un arresto brusco nei
 * millisecondi di un salvataggio. Ogni record porta l'armatura stivata ({@link StowedStack}), che non va mai ai client.
 * Le operazioni di registro sono solo sul server, sul thread di gioco; ogni scrittura segna il dato come da salvare.
 * Se il dato salvato e' in sola lettura (salvato da una versione piu' recente della mod, o illeggibile con la sua
 * copia precedente) le scritture non fanno nulla e danno falso, vuoto o null (un solo ERROR nel log), e le letture
 * non trovano nulla. Gli argomenti null sono rifiutati subito con un'eccezione.
 * <p>
 * Un telaio tolto dal registro ({@link #removePlaced}, {@link #clear}) con armatura stivata ancora dentro non la
 * perde: le voci rimaste, che si leggano o no, passano identiche in una lista del dato salvato
 * ({@code orphaned}), con l'identita' del telaio e il proprietario, e un avviso nel log. Chi toglie un telaio
 * riprende prima l'armatura che si legge; la lista e' la rete per quello che resta.
 */
public interface FrameStateAccess {
    /**
     * Lo stato del giocatore. Sul server logico legge il dato salvato senza crearlo; sul client restituisce la copia
     * sincronizzata ({@link #clientView}). L'oggetto restituito e' lo stato vivo: va trattato in sola lettura, e ogni
     * modifica passa da un metodo di scrittura ({@link #update}, {@link #mutate}, {@link #clear}).
     */
    Optional<FrameState> get(Player p);

    /**
     * Crea lo stato se assente, applica la modifica, segna il dato come da salvare e sincronizza il giocatore. Solo
     * sul server, sul thread di gioco. Una voce indossata del giocatore che il dato salvato non ha potuto leggere non
     * viene mai tolta ne' coperta: resta nel file, identica, accanto al record nuovo.
     */
    void update(ServerPlayer p, Consumer<FrameState> change);

    /**
     * Se il giocatore ha uno stato, applica la modifica e segna il dato come da salvare (anche se la modifica lancia),
     * senza crearlo e senza sincronizzare: e' il percorso per le modifiche ad alta frequenza, e il chiamante chiama
     * {@link #syncTo} al proprio ritmo. Solo sul server, sul thread di gioco.
     *
     * @return true se lo stato c'era ed e' stata applicata la modifica, false se il giocatore non ha uno stato
     */
    boolean mutate(ServerPlayer p, Consumer<FrameState> change);

    /**
     * Rimuove lo stato del giocatore, segna il dato come da salvare e invia al giocatore uno stato assente. L'armatura
     * stivata ancora nel record non si perde: passa, identica, fra quella dei telai tolti (vedi {@link #removePlaced}).
     * Una voce indossata del giocatore che il dato salvato non ha potuto leggere non viene tolta: resta nel file,
     * identica.
     */
    void clear(ServerPlayer p);

    /**
     * La copia sincronizzata lato client dello stato del giocatore, se ricevuta. E' la copia viva della cache: va
     * trattata in sola lettura (le modifiche passano solo dai metodi di scrittura, sul server).
     */
    Optional<FrameState> clientView(Player clientPlayer);

    /** Invia al giocatore il suo stato attuale (o l'assenza di stato). */
    void syncTo(ServerPlayer p);

    /**
     * Mette nel registro un telaio piazzato nuovo con lo stato dato (che da qui appartiene al registro: chi lo
     * cambia passa da {@link #updatePlaced}), nella dimensione del livello, alla posizione dei piedi e con
     * l'orientamento in gradi dati; nessuna armatura stivata.
     *
     * @return l'identita' nuova del telaio, o null se il dato salvato e' in sola lettura
     * @throws IllegalArgumentException se posizione o orientamento non sono finiti
     */
    UUID place(ServerLevel level, FrameState state, Vec3 position, float yaw);

    /** Il telaio piazzato con questa identita'; vuoto se non c'e' o se e' indossato. */
    Optional<PlacedFrame> placed(MinecraftServer server, UUID frameId);

    /** I telai piazzati nella dimensione del livello, nell'ordine in cui sono stati piazzati o letti. */
    List<PlacedFrame> placedIn(ServerLevel level);

    /**
     * Se il telaio e' piazzato, applica la modifica al suo stato e segna il dato come da salvare (anche se la
     * modifica lancia); nessuna sincronizzazione (lo stato di un piazzato non va a nessun client).
     *
     * @return true se il telaio era piazzato e la modifica e' stata applicata
     */
    boolean updatePlaced(MinecraftServer server, UUID frameId, Consumer<FrameState> change);

    /**
     * Toglie dal registro il telaio piazzato. Chi lo smonta riprende prima l'armatura stivata con
     * {@link #takeStowedOfPlaced}; quello che resta (voci che non si leggono) passa, identico, fra l'armatura dei
     * telai tolti nel dato salvato, con un avviso che nomina il telaio: non si perde mai.
     *
     * @return true se il telaio era piazzato ed e' stato tolto
     */
    boolean removePlaced(MinecraftServer server, UUID frameId);

    /**
     * Sposta il telaio piazzato fra gli indossati del giocatore (lo stesso record: stato e armatura stivata restano)
     * e sincronizza una volta il giocatore con il suo stato nuovo. Ad altri giocatori non manda nulla.
     *
     * @return false, senza cambiare nulla, se il telaio non e' piazzato o il giocatore ne indossa gia' uno
     */
    boolean enter(ServerPlayer player, UUID frameId);

    /**
     * Sposta il telaio indossato dal giocatore fra i piazzati della dimensione del livello dato (che puo' non essere
     * quella del giocatore, per esempio il posto sicuro ricordato dopo una morte nel vuoto), alla posizione e con
     * l'orientamento dati, e manda al giocatore lo stato assente.
     *
     * @return l'identita' del telaio, o vuoto, senza cambiare nulla, se il giocatore non ne indossa uno
     * @throws IllegalArgumentException se posizione o orientamento non sono finiti
     */
    Optional<UUID> exit(ServerPlayer player, ServerLevel level, Vec3 position, float yaw);

    /** L'identita' del telaio indossato dal giocatore, o vuoto. */
    Optional<UUID> wornFrameId(ServerPlayer player);

    /**
     * Aggiunge l'armatura, ognuna con il suo slot, all'armatura stivata del telaio indossato dal giocatore; gli stack
     * vuoti si saltano. Gli stack si scrivono subito come tag: chi chiama li toglie poi dal giocatore.
     * <p>
     * Con il dato in sola lettura non fa nulla e non lo dice al chiamante (solo l'ERROR nel log): chi stiva deve
     * averlo fatto dopo un {@link #enter} riuscito, che in sola lettura da' falso, e non deve togliere l'armatura al
     * giocatore se l'ingresso non e' riuscito.
     *
     * @throws IllegalStateException se il giocatore non indossa un telaio (l'armatura non avrebbe dove stare)
     */
    void stow(ServerPlayer player, List<StowedStack> armor);

    /**
     * Toglie e restituisce l'armatura stivata del telaio indossato dal giocatore, nell'ordine in cui e' stata stivata;
     * vuota se non ne indossa uno. Una voce che non si legge (item sconosciuto, tag corrotto) resta nel record cosi'
     * com'e', con un avviso una volta, e non viene restituita.
     */
    List<StowedStack> takeStowed(ServerPlayer player);

    /** Come {@link #takeStowed}, per il telaio piazzato con questa identita'; vuota se non e' piazzato. */
    List<StowedStack> takeStowedOfPlaced(MinecraftServer server, UUID frameId);

    /**
     * Il riassunto visivo come tag a se' (con la chiave {@link NbtKeys#VISUAL}, lo stesso schema del segnaposto del
     * torso), per i dati sincronizzati dell'entita' del telaio.
     */
    CompoundTag writeSummaryTag(SuitSummary summary);

    /**
     * Il riassunto letto da un tag scritto da {@link #writeSummaryTag}; vuoto se il tag non ne ha uno. Non lancia: i
     * pezzi illeggibili si saltano come in {@link ItemDataAccess#suitSummary}.
     */
    Optional<SuitSummary> readSummaryTag(CompoundTag tag);

    /**
     * Registrazioni comuni, da chiamare una volta nell'init comune dopo le altre: il pacchetto S2C di
     * sincronizzazione e gli eventi di ingresso e respawn che sincronizzano il giocatore.
     */
    void registerCommon();

    /**
     * Registrazioni solo client, da chiamare una volta e solo dagli entrypoint client: svuota la copia locale quando
     * il giocatore esce dal mondo. Su un server dedicato non va mai chiamato (carica classi del client).
     */
    void registerClient();
}
