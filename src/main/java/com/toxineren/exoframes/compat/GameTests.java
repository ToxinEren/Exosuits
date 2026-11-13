package com.toxineren.exoframes.compat;

import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
     * il giocatore finto vanilla non ha canale e fa fallire l'ingresso nel mondo). Il giocatore resta nella lista
     * dei giocatori del server per il resto dell'esecuzione: i test non devono contare i giocatori ne' scorrere la
     * lista aspettandosi di trovarci solo i propri.
     */
    ServerPlayer mockServerPlayer(GameTestHelper helper);

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
}
