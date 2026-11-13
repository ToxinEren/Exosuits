package com.toxineren.exoframes.compat;

import java.nio.file.Path;

/** Informazioni sull'ambiente di esecuzione (lato, loader, cartelle, mod presenti) indipendenti da versione e loader. */
public interface Platform {
    /** Vero se il gioco gira sul client fisico. */
    boolean isClient();

    /** Vero se il gioco gira su un server dedicato. */
    boolean isDedicatedServer();

    /** Vero nell'ambiente di sviluppo (runClient/runServer da Gradle). */
    boolean isDevelopment();

    /** La cartella di configurazione del gioco. */
    Path configDir();

    /** Vero se la mod con l'id indicato e' caricata. */
    boolean isModLoaded(String modId);

    /** Il nome del loader in uso: "fabric", "forge" o "neoforge". */
    String loaderName();
}
