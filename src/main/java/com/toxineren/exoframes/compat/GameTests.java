package com.toxineren.exoframes.compat;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Aggancio dei test in gioco (GameTest vanilla) al loader: l'unico punto con le differenze di registrazione tra
 * Fabric, Forge e NeoForge e di creazione del giocatore finto tra 1.20.1 e 1.21.1.
 */
public interface GameTests {
    /**
     * Registra la classe di test presso il framework GameTest, solo se e' attivo il server di test; altrimenti non fa
     * nulla, cosi' in produzione i test non esistono (neanche per il comando {@code /test}). Su Forge e NeoForge
     * aggiunge un ascoltatore di {@code RegisterGameTestsEvent} se e' impostata {@code forge/neoforge.gameTestServer};
     * su Fabric chiama {@code GameTestRegistry.register} se e' impostata {@code fabric-api.gametest} (nessun
     * entrypoint {@code fabric-gametest}: il modulo di Fabric lo registrerebbe a ogni avvio).
     */
    void register(Class<?> testClass);

    /**
     * Crea un giocatore finto connesso nel livello del test, con una connessione dotata di canale (su Forge 1.20.1
     * il giocatore finto vanilla non ha canale e fa fallire l'ingresso nel mondo). Come quello vanilla dice sempre di
     * essere in creativa ({@code isCreative()}); a differenza di quello vanilla {@code isSpectator()} segue la sua
     * modalita' di gioco, cosi' {@code setGameMode(GameType.SPECTATOR)} ne fa uno spettatore. Il giocatore resta
     * nella lista dei giocatori del server per il resto dell'esecuzione: i test non devono contare i giocatori ne'
     * scorrere la lista aspettandosi di trovarci solo i propri.
     */
    ServerPlayer mockServerPlayer(GameTestHelper helper);

    /**
     * Il giocatore finto di {@link #mockServerPlayer} in sopravvivenza e con la protezione d'ingresso finita: lo crea,
     * lo mette in {@code GameType.SURVIVAL} (capacita' {@code instabuild}, {@code invulnerable} e {@code mayfly} false;
     * {@code isCreative()} resta vero come per ogni giocatore finto) e chiama 60 volte {@code player.tick()}, perche'
     * {@code ServerPlayer.spawnInvulnerableTime} nasce a 60 e prima di zero i colpi non contano. {@code tick()} non
     * muove il giocatore, non manda eventi di Architectury e non cambia {@code tickCount}, ma scala anche
     * {@code invulnerableTime}: per questo si chiama solo qui, prima del primo colpo. Come il giocatore finto ricorda i
     * messaggi di sistema ({@link #messagesTo}). Nasce vicino al punto di rinascita del mondo: il test lo sposta nella
     * sua struttura. Esiste solo per i test.
     */
    ServerPlayer survivalPlayer(GameTestHelper helper);

    /**
     * I messaggi di sistema (chat e barra delle azioni) arrivati al giocatore finto di {@link #mockServerPlayer}, in
     * ordine, dalla sua creazione; vuota per un altro giocatore. Esiste solo per i test.
     */
    List<Component> messagesTo(ServerPlayer player);

    /**
     * Scrive {@code root} cosi' com'e', senza validarlo, come tag radice {@link NbtKeys#ROOT} dello stack, nello stesso
     * punto in cui lo legge {@link ItemDataAccess} (tag dello stack su 1.20.1, custom_data su 1.21.1); su uno stack
     * vuoto non fa nulla. Esiste solo per i test: simula dati scritti da altre mod, da {@code /give} o da versioni
     * vecchie, che il codice della mod non scriverebbe mai.
     */
    void writeRawItemData(ItemStack stack, CompoundTag root);

    /**
     * Aggiunge {@code entry} cosi' com'e', senza validarla, in coda all'armatura stivata del telaio piazzato con
     * questa identita' (vedi {@link FrameStateAccess#stow}) e segna il dato come da salvare. Esiste solo per i test:
     * simula voci scritte da una versione vecchia del gioco o rovinate, che il codice della mod non scriverebbe mai.
     *
     * @return false se il telaio non e' piazzato o se il dato salvato e' in sola lettura
     */
    boolean addRawStowed(MinecraftServer server, UUID frameId, CompoundTag entry);

    /**
     * Le voci stivate rimaste nei telai tolti dal registro con questa identita' (copie, nell'ordine), come le conserva
     * il dato salvato (vedi {@link FrameStateAccess#removePlaced}); vuota se non ce ne sono. Esiste solo per i test.
     */
    List<CompoundTag> orphanedStowed(MinecraftServer server, UUID frameId);

    /**
     * Toglie dal registro del server i gruppi di voci orfane del telaio con questa identita' (vedi
     * {@link #orphanedStowed}) e dice quanti ne ha tolti. Esiste solo per i test, che cosi' non lasciano voci orfane nel
     * dato condiviso: nel gioco nessuno toglie mai quelle voci.
     */
    int forgetOrphaned(MinecraftServer server, UUID frameId);

    /**
     * Salva il registro dei telai del server come il gioco salva i dati dell'overworld (a ogni salvataggio automatico:
     * {@code DimensionDataStorage.save}) e da' il percorso del suo file. Esiste solo per i test.
     */
    Path saveRegistry(MinecraftServer server);

    /**
     * La cartella dei dati salvati dell'overworld in cui il server tiene il file del registro, come la usa il registro
     * a ogni richiesta (non ricalcolata: lo stesso oggetto a ogni chiamata per lo stesso server). Solo per i test.
     */
    Path registryFolder(MinecraftServer server);

    /** Il registro del server come lo scriverebbe adesso il salvataggio (il tag sotto {@code data}). Solo per i test. */
    CompoundTag registryContents(MinecraftServer server);

    /**
     * Legge il registro dalla cartella dei dati {@code dataFolder} come il server legge il suo all'avvio: un
     * {@code DimensionDataStorage} nuovo su quella cartella e la factory della mod, con il ripiego sulla copia
     * precedente se il file non si legge. Da' il registro letto come lo scriverebbe il salvataggio, o vuoto se e'
     * rimasto in sola lettura (nulla si e' potuto leggere, o il file non e' un registro). Non tocca il registro del
     * server. Torna quando il loader non usa piu' la cartella (NeoForge 1.21.1, dopo un file letto, ci pulisce i file
     * temporanei dal suo lavoratore di I/O, e qui lo si aspetta): chi chiama puo' cancellarla subito. Esiste solo per i
     * test.
     */
    Optional<CompoundTag> readRegistry(MinecraftServer server, Path dataFolder);

    /**
     * Vero se il registro del server ha un cambio strutturale (un telaio piazzato, indossato, lasciato, armatura
     * stivata...) non ancora scritto sul disco. Esiste solo per i test.
     */
    boolean registryChangeUnsaved(MinecraftServer server);

    /**
     * Quante volte {@code FrameStateAccess.syncTo} e' stato chiamato per questo giocatore da quando e' entrato nel
     * server, anche quando il loader non ha spedito il pacchetto (su NeoForge 1.21.1 il giocatore finto non ha il
     * canale). Esiste solo per i test.
     */
    int syncCount(ServerPlayer player);

    /**
     * Gli id di registro degli attributi del giocatore che il gioco deve ancora mandare (al proprietario e a chi lo
     * guarda), letti dalla mappa degli attributi con il metodo della versione; poi svuota l'insieme del gioco, quindi
     * due letture di fila danno la seconda vuota. Esiste solo per i test: prova che un'applicazione senza cambi non fa
     * reinviare nulla.
     */
    Set<ResourceLocation> attributesToSync(ServerPlayer player);

    /**
     * Vero se il gradino della tuta e' un modificatore di attributo ({@code frame_step} su {@code generic.step_height},
     * 1.21.1), falso se e' il campo del passo del solo giocatore locale (1.20.1). Esiste solo per i test.
     */
    boolean stepIsAttribute();

    /**
     * L'importo del modificatore {@code redstone_exoframes:<id>} su qualunque attributo del giocatore, vuoto se non c'e'
     * su nessuno. Esiste solo per i test.
     */
    OptionalDouble frameModifier(ServerPlayer player, String id);

    /**
     * Fa rinascere il giocatore come la rinascita dopo la morte o il ritorno dall'End ({@code keepEverything}): crea un
     * oggetto giocatore nuovo, con i soli valori base degli attributi, e lo restituisce. L'oggetto nuovo e' un
     * {@code ServerPlayer} del gioco, non il giocatore finto: non ricorda i messaggi e {@code isCreative()} segue la
     * modalita' di gioco. Esiste solo per i test.
     */
    ServerPlayer respawn(ServerPlayer player, boolean keepEverything);

    /**
     * True se almeno uno dei controlli che i portali fanno su un'entita' prima di portarla via la accetta, come li fa
     * la versione del gioco: su 1.20.1 {@code canChangeDimensions()} (portali del Nether e dell'End) e
     * {@code TheEndGatewayBlockEntity.canEntityTeleport} (passaggio dell'End); su 1.21.1 {@code canUsePortal(false)}
     * (i tre blocchi portale) e {@code canChangeDimensions(livello dell'entita', destination)} (il viaggio). Non sposta
     * nulla e non carica l'altra dimensione. Esiste solo per i test.
     */
    boolean portalsAccept(Entity entity, ServerLevel destination);

    /**
     * Comincia a contare gli eventi di gioco {@code EQUIP} che arrivano entro 16 blocchi da {@code center}, finche' il
     * contatore non si chiude. {@code LivingEntity.onEquipItem} emette quell'evento nello stesso ramo in cui suona
     * l'equipaggiamento: zero eventi dopo un cambio degli slot armatura vuol dire nessun suono. Esiste solo per i test.
     */
    EquipEventCounter countEquipEvents(ServerLevel level, BlockPos center);

    /** Il contatore di {@link #countEquipEvents}; {@link #close} smette di ascoltare. */
    interface EquipEventCounter extends AutoCloseable {
        /** Gli eventi {@code EQUIP} arrivati dall'apertura. */
        int count();

        @Override
        void close();
    }
}
