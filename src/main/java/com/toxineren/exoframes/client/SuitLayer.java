package com.toxineren.exoframes.client;

import com.google.common.collect.MapMaker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.compat.Compat;
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
 * costruisce la {@link SuitView} ({@link SuitDraw#view}) e la disegna con {@link SuitDraw#draw}, annullando il
 * sollevamento del pilota. In ogni altro caso non disegna nulla, e non lancia mai: un errore si scrive nel log una
 * volta per caricamento dei renderer (vedi {@link SuitDraw}) e il fotogramma prosegue senza la tuta.
 * <p>
 * Il riassunto si decodifica una volta per stack, non a ogni fotogramma (vedi {@link #view(Player)}).
 */
public final class SuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /**
     * Vista decodificata per istanza di stack. Chiavi deboli e confrontate per identita' ({@code MapMaker.weakKeys}):
     * uno stack che il gioco non usa piu' (per esempio sostituito da un aggiornamento dell'equipaggiamento dal
     * server) esce dalla cache con il garbage collector. Nessun valore tiene un riferimento alla propria chiave.
     */
    private static final Map<ItemStack, Decoded> CACHE = new MapMaker().weakKeys().makeMap();

    public SuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
        SuitDraw.resetFailureLog();
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        SuitView view = suitView(player);
        if (view != null) {
            // Il disegno del giocatore e' sollevato nello spazio del modello dal mixin del renderer (PilotPose, in coda
            // a PlayerRenderer.scale): la tuta non lo segue, cosi' i suoi piedi restano sul corpo a terra in ogni posa.
            SuitDraw.draw(player, getParentModel(), pose, buffers, packedLight, partialTick, view,
                    SuitScale.PILOT_RAISE_PX);
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
            SuitDraw.logFailureOnce(e);
            return null;
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
        SuitView view = Compat.itemData().suitSummary(stack).map(SuitDraw::view).orElse(null);
        return new Decoded(stack.copy(), view);
    }

    /** Copia dello stack al momento della decodifica e vista che ne risulta (null: riassunto assente o illeggibile). */
    private record Decoded(ItemStack snapshot, SuitView view) {
    }
}
