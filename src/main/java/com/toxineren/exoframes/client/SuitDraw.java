package com.toxineren.exoframes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.client.ClientCompat;
import com.toxineren.exoframes.compat.client.SuitView;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

/**
 * Il disegno della tuta comune ai suoi due chiamanti, la tuta indossata ({@link SuitLayer}) e il telaio piazzato
 * ({@link ExoframeEntityRenderer}): la conversione dal riassunto visivo alla {@link SuitView} ({@link #view}) e il
 * disegno in scala {@link SuitScale#S} attorno ai piedi ({@link #draw}), cosi' i due sono lo stesso oggetto sullo
 * schermo (design §4.2 B). Nessuno dei due lancia: un errore si scrive nel log una volta per caricamento dei renderer
 * delle entita' ({@link #resetFailureLog}) e il fotogramma prosegue senza la tuta.
 */
public final class SuitDraw {
    /**
     * Altezza dei piedi nello spazio del modello del giocatore (y verso il basso), dopo la traslazione di
     * {@code LivingEntityRenderer}: il punto attorno a cui la tuta si scala.
     */
    public static final float FEET_Y = 1.501f;

    /**
     * True dopo il primo errore scritto nel log. Torna false a ogni costruzione dei renderer che disegnano la tuta,
     * cioe' a ogni ricostruzione dei renderer delle entita' (avvio e ricarica delle risorse): un primo errore
     * passeggero, per esempio la cache dei modelli di GeckoLib durante una ricarica, non nasconde per tutta la sessione
     * un errore che resta.
     */
    private static volatile boolean failureLogged;

    private SuitDraw() {
    }

    /**
     * La vista da disegnare per un riassunto visivo, con i colori non verniciati di {@link TierPalette}: l'unica
     * conversione, per la tuta indossata e per il telaio piazzato. I chiamanti la tengono in cache e la rifanno solo
     * quando il riassunto cambia.
     */
    public static SuitView view(SuitSummary summary) {
        return SuitView.of(summary, TierPalette::primary, TierPalette::secondary);
    }

    /**
     * Disegna la tuta con {@link ClientCompat#suitRenderer()} nella posa del modello umanoide {@code base}, S volte piu'
     * grande attorno ai piedi. {@code pose} e' nello spazio del modello del giocatore come lo prepara
     * {@code LivingEntityRenderer} (ribaltato, piedi a y = {@link #FEET_Y}); {@code undoRaisePx} e' il sollevamento,
     * in pixel del modello, che il chiamante ha gia' applicato al disegno e che la tuta non deve seguire (quello del
     * pilota per la tuta indossata, 0 per il telaio piazzato). Non lancia: {@code RuntimeException} e
     * {@code LinkageError} (un GeckoLib diverso da quello atteso) vanno nel log una volta per caricamento dei
     * renderer, e {@code pose} resta com'era.
     */
    public static void draw(Entity entity, HumanoidModel<?> base, PoseStack pose, MultiBufferSource buffers,
            int packedLight, float partialTick, SuitView view, float undoRaisePx) {
        try {
            // Stack privato che parte dalla posa corrente: GeckoLib fa push e pop propri solo sul percorso normale, e
            // se lancia a meta' disegno le sue pose restano su questo stack, che si butta, invece che su quello del
            // chiamante (LevelRenderer.checkPoseStack fa chiudere il client se a fine fotogramma non e' vuoto).
            // Su 1.21.1 la copia ha trustedNormals = true come ogni posa nuova (il campo non e' pubblico): le scale dei
            // due chiamanti sono uniformi in valore assoluto e non lo spengono, e se lo fossero l'unico effetto
            // sarebbero normali non rinormalizzate, cioe' una luce appena diversa.
            PoseStack local = new PoseStack();
            local.last().pose().set(pose.last().pose());
            local.last().normal().set(pose.last().normal());
            // Il sollevamento si annulla nello spazio del modello in cui e' stato applicato (y cresce verso il basso),
            // cosi' i piedi della tuta restano a terra in ogni posa.
            local.translate(0f, undoRaisePx / 16f, 0f);
            // Tuta S volte piu' grande attorno ai piedi.
            local.translate(0f, FEET_Y, 0f);
            local.scale(SuitScale.S, SuitScale.S, SuitScale.S);
            local.translate(0f, -FEET_Y, 0f);
            ClientCompat.suitRenderer().render(entity, base, local, buffers, packedLight, partialTick, view);
        } catch (RuntimeException | LinkageError e) {
            // Un'eccezione dal disegno di un'entita' farebbe chiudere il client: si salta la tuta e si avvisa.
            logFailureOnce(e);
        }
    }

    /** Scrive l'errore nel log se e' il primo dall'ultimo caricamento dei renderer delle entita'. */
    static void logFailureOnce(Throwable e) {
        if (!failureLogged) {
            failureLogged = true;
            RedstoneExoframes.LOGGER.error("Suit rendering failed with {}; the suit is not drawn (logged once until "
                    + "the entity renderers are reloaded)", e.getClass().getName(), e);
        }
    }

    /** Da chiamare alla costruzione dei renderer che disegnano la tuta: il prossimo errore torna nel log. */
    static void resetFailureLog() {
        failureLogged = false;
    }
}
