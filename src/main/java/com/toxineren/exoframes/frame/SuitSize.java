package com.toxineren.exoframes.frame;

import com.google.common.collect.MapMaker;
import com.toxineren.exoframes.content.ModItems;
import java.util.Map;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Grandezza reale di chi indossa la tuta (design, par. 4.8): ingombro e altezza degli occhi crescono con la tuta, sul
 * server e sul client, con la stessa regola e le stesse costanti; nulla si invia, perche' il segnaposto del torso
 * arriva gia' a tutti con gli slot. Codice comune: lo chiamano i due mixin di {@code Player} (uno per versione del
 * gioco, una chiamata per aggancio, senza logica), il mixin client del culling e il tick del giocatore.
 * <p>
 * Le costanti non stanno nella configurazione: server e client devono avere per forza gli stessi numeri.
 */
public final class SuitSize {
    /** Scala verticale della tuta, la stessa con cui si disegna attorno ai piedi ({@code client/SuitScale.S}). */
    public static final float HEIGHT_SCALE = 1.65f;

    /**
     * Larghezza dell'ingombro con la tuta: passa i 13/16 di blocco lasciati da una scala a pioli, da una porta aperta
     * e da una botola aperta (0,6 * S non ci passerebbe).
     */
    public static final float WIDTH = 0.8f;

    /**
     * Allargamento orizzontale, per lato, del riquadro di culling di chi indossa la tuta: le braccia della tuta con i
     * pezzi escono di circa 0,41 dal riquadro dell'ingombro gonfiato da {@code EntityRenderer.shouldRender}.
     */
    public static final float CULLING_MARGIN = 0.5f;

    /** Larghezza vanilla del giocatore, in ogni posa che non ha dimensioni fisse. */
    private static final float VANILLA_WIDTH = 0.6f;

    /** Altezza vanilla del giocatore in piedi. */
    private static final float VANILLA_STANDING_HEIGHT = 1.8f;

    /**
     * Ultimo valore di "indossa la tuta" visto dal tick, per giocatore. Chiavi deboli e confrontate per identita'
     * ({@code MapMaker.weakKeys}), non {@code WeakHashMap}: {@code Entity.equals} confronta gli id, e in giocatore
     * singolo il giocatore del client e quello del server possono avere lo stesso id.
     */
    private static final Map<Player, Boolean> LAST_SEEN = new MapMaker().weakKeys().makeMap();

    private SuitSize() {
    }

    /**
     * True se il giocatore ha il segnaposto del torso nello slot del petto: la regola dell'ingombro, uguale sui due
     * lati. Falso se l'inventario non esiste ancora: il costruttore di {@code Entity} chiama {@code getDimensions}
     * (e su 1.20.1 l'altezza degli occhi) prima che {@code Player} abbia creato l'inventario.
     */
    public static boolean wearsSuit(Player player) {
        if (player.getInventory() == null) {
            return false;
        }
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.EXOFRAME_TORSO.get());
    }

    /**
     * Ingombro per la posa: quello vanilla largo {@link #WIDTH} e alto {@link #HEIGHT_SCALE} volte per chi indossa la
     * tuta, altrimenti quello vanilla. Le dimensioni fisse (sonno, morte) {@code scale} le restituisce invariate; su
     * 1.21.1 scala anche occhi, targhetta del nome e punto di seduta, che stanno nelle dimensioni.
     */
    public static EntityDimensions dimensions(Player player, Pose pose, EntityDimensions vanilla) {
        return wearsSuit(player) ? vanilla.scale(WIDTH / VANILLA_WIDTH, HEIGHT_SCALE) : vanilla;
    }

    /**
     * Solo 1.20.1 (su 1.21.1 gli occhi stanno nelle dimensioni): altezza degli occhi per la posa, {@link #HEIGHT_SCALE}
     * volte quella vanilla per chi indossa la tuta. Sonno e morte restano vanilla, come le loro dimensioni fisse.
     */
    public static float eyeHeight(Player player, Pose pose, float vanilla) {
        if (pose == Pose.SLEEPING || pose == Pose.DYING || !wearsSuit(player)) {
            return vanilla;
        }
        return vanilla * HEIGHT_SCALE;
    }

    /**
     * A ogni tick di ogni giocatore, sui due lati ({@code TickEvent.PLAYER_PRE}): confronta "indossa la tuta" con
     * l'ultimo valore visto e, se e' cambiato, ricalcola le dimensioni. Non c'e' un evento di cambio
     * dell'equipaggiamento sui due lati e su tutti i loader; il confronto con l'ultimo valore, invece che con il
     * riquadro atteso, non litiga con altre mod che cambiano la grandezza.
     */
    public static void tick(Player player) {
        boolean now = wearsSuit(player);
        Boolean before = LAST_SEEN.put(player, now);
        if (before == null ? now : before != now) {
            player.refreshDimensions();
        }
    }

    /** Il riquadro della tuta in piedi ({@link #WIDTH} x 1,8 * {@link #HEIGHT_SCALE}) con i piedi in {@code feet}. */
    public static AABB standingBox(Vec3 feet) {
        double half = WIDTH / 2f;
        double height = VANILLA_STANDING_HEIGHT * HEIGHT_SCALE;
        return new AABB(feet.x - half, feet.y, feet.z - half, feet.x + half, feet.y + height, feet.z + half);
    }

    /**
     * Riquadro di culling: quello vanilla allargato di {@link #CULLING_MARGIN} per lato in orizzontale per un
     * giocatore che indossa la tuta, altrimenti quello vanilla. Lo chiama il mixin client su
     * {@code LivingEntity.getBoundingBoxForCulling}, per ogni entita' vivente disegnata.
     */
    public static AABB cullingBox(LivingEntity entity, AABB vanilla) {
        if (entity instanceof Player player && wearsSuit(player)) {
            return vanilla.inflate(CULLING_MARGIN, 0, CULLING_MARGIN);
        }
        return vanilla;
    }
}
