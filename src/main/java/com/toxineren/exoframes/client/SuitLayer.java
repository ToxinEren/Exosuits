package com.toxineren.exoframes.client;

import com.google.common.collect.MapMaker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.client.ClientCompat;
import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.frame.SuitSize;
import java.util.Map;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Layer del renderer del giocatore che disegna la tuta: se il giocatore la indossa ({@link SuitSize#wearsSuit}: il
 * segnaposto {@link ModItems#EXOFRAME_TORSO} nello slot del petto) e il segnaposto ha un riassunto visivo leggibile,
 * costruisce la {@link SuitView} (colori non verniciati da {@link TierPalette}) e chiama il
 * {@link com.toxineren.exoframes.compat.client.SuitRenderer} con la tuta scalata di {@link SuitScale#S} attorno ai
 * piedi. In ogni altro caso non disegna nulla, e non lancia mai: un errore nel disegno si scrive nel log una volta
 * per caricamento dei renderer (vedi il campo failureLogged) e il fotogramma prosegue senza la tuta.
 * <p>
 * Il riassunto si decodifica una volta per stack, non a ogni fotogramma (vedi {@link #view(Player)}).
 */
public final class SuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Altezza dei piedi nello spazio del modello (y verso il basso), dopo la traslazione di LivingEntityRenderer. */
    private static final float FEET_Y = 1.501f;

    /**
     * Vista decodificata per istanza di stack. Chiavi deboli e confrontate per identita' ({@code MapMaker.weakKeys}):
     * uno stack che il gioco non usa piu' (per esempio sostituito da un aggiornamento dell'equipaggiamento dal
     * server) esce dalla cache con il garbage collector. Nessun valore tiene un riferimento alla propria chiave.
     */
    private static final Map<ItemStack, Decoded> CACHE = new MapMaker().weakKeys().makeMap();

    /**
     * True dopo il primo errore scritto nel log. Torna false a ogni costruzione del layer, cioe' a ogni ricostruzione
     * dei renderer delle entita' (avvio e ricarica delle risorse): un primo errore passeggero, per esempio la cache
     * dei modelli di GeckoLib durante una ricarica, non nasconde per tutta la sessione un errore che resta.
     */
    private static volatile boolean failureLogged;

    public SuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
        failureLogged = false;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        try {
            SuitView view = view(player);
            if (view == null) {
                return;
            }
            // Stack privato che parte dalla posa corrente: GeckoLib fa push e pop propri solo sul percorso normale, e
            // se lancia a meta' disegno le sue pose restano su questo stack, che si butta, invece che su quello del
            // chiamante (LevelRenderer.checkPoseStack fa chiudere il client se a fine fotogramma non e' vuoto).
            // Su 1.21.1 la copia ha trustedNormals = true come ogni posa nuova (il campo non e' pubblico): le scale del
            // renderer del giocatore sono uniformi in valore assoluto e non lo spengono, e se lo fossero l'unico
            // effetto sarebbero normali non rinormalizzate, cioe' una luce appena diversa.
            PoseStack local = new PoseStack();
            local.last().pose().set(pose.last().pose());
            local.last().normal().set(pose.last().normal());
            // Il disegno del giocatore e' sollevato nello spazio del modello dal mixin del renderer (PilotPose, in coda a
            // PlayerRenderer.scale): si annulla nello stesso spazio (y cresce verso il basso), cosi' i piedi della tuta
            // restano sul corpo a terra in ogni posa.
            local.translate(0f, SuitScale.PILOT_RAISE_PX / 16f, 0f);
            // Tuta S volte piu' grande attorno ai piedi.
            local.translate(0f, FEET_Y, 0f);
            local.scale(SuitScale.S, SuitScale.S, SuitScale.S);
            local.translate(0f, -FEET_Y, 0f);
            ClientCompat.suitRenderer().render(player, getParentModel(), local, buffers, packedLight, partialTick,
                    view);
        } catch (RuntimeException | LinkageError e) {
            // Un'eccezione da un layer farebbe chiudere il client, e cosi' un GeckoLib di versione diversa da quella
            // attesa (LinkageError): si salta la tuta e si avvisa una volta per caricamento dei renderer.
            logFailureOnce(e);
        }
    }

    /**
     * True se la tuta del giocatore va disegnata: la indossa per la regola comune dell'ingombro
     * ({@link SuitSize#wearsSuit}, il segnaposto del torso nello slot del petto) e il segnaposto ha un riassunto
     * visivo leggibile. L'unico posto che decide il disegno. Non lancia mai: i mixin del pilota lo chiamano a ogni
     * fotogramma di ogni giocatore, e un errore vale "non la indossa" (scritto nel log una volta per caricamento dei
     * renderer).
     */
    public static boolean wearsSuit(Player player) {
        return suitView(player) != null;
    }

    /**
     * La vista in cache del giocatore, o null se la sua tuta non va disegnata (vedi {@link #wearsSuit}). Non lancia
     * mai: un errore vale null. Per {@link PilotPose}, che non decodifica il riassunto una seconda volta.
     */
    static SuitView suitView(Player player) {
        try {
            return view(player);
        } catch (RuntimeException | LinkageError e) {
            logFailureOnce(e);
            return null;
        }
    }

    private static void logFailureOnce(Throwable e) {
        if (!failureLogged) {
            failureLogged = true;
            RedstoneExoframes.LOGGER.error("Suit layer failed with {}; the suit is not drawn (logged once until the "
                    + "entity renderers are reloaded)", e.getClass().getName(), e);
        }
    }

    /**
     * La vista da disegnare per il giocatore, o null se la sua tuta non va disegnata. La lettura del riassunto copia
     * il tag a ogni chiamata, quindi il risultato si tiene in cache per istanza di stack insieme a una copia dello
     * stack presa al momento della decodifica. Validita': {@code ItemStack.matches(copia, stack)}, che confronta
     * oggetto, quantita' e dati (il tag NBT su 1.20.1, i componenti su 1.21.1) con lo stesso metodo vanilla sulle due
     * versioni. Copre anche lo stack modificato sul posto (inventario creativo, comandi in giocatore singolo, che
     * su 1.20.1 cambiano il tag senza cambiare istanza). Costo per fotogramma: su 1.21.1 il componente dei dati e'
     * immutabile e condiviso dalla copia, quindi il confronto si ferma all'identita'; su 1.20.1 e' un confronto in
     * profondita' del tag, piccolo (un compound per pezzo), senza le copie e le allocazioni della decodifica.
     */
    private static SuitView view(Player player) {
        if (!SuitSize.wearsSuit(player)) {
            return null;
        }
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        Decoded cached = CACHE.get(stack);
        if (cached == null || !ItemStack.matches(cached.snapshot(), stack)) {
            cached = decode(stack);
            CACHE.put(stack, cached);
        }
        return cached.view();
    }

    private static Decoded decode(ItemStack stack) {
        SuitView view = Compat.itemData().suitSummary(stack)
                .map(summary -> SuitView.of(summary, TierPalette::primary, TierPalette::secondary))
                .orElse(null);
        return new Decoded(stack.copy(), view);
    }

    /** Copia dello stack al momento della decodifica e vista che ne risulta (null: riassunto assente o illeggibile). */
    private record Decoded(ItemStack snapshot, SuitView view) {
    }
}
