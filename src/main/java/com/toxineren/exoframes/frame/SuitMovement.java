package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.FrameIndicators;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;

/**
 * Acqua e scatto di chi indossa la tuta (design §12.6). Codice comune, sui due lati: lo chiamano il mixin comune di
 * {@code Player} (galleggiamento, corrente, nuoto) e il mixin client del giocatore locale (scatto), una chiamata per
 * aggancio, senza logica nei mixin (design §12.2). Nessun metodo porta il nome del suo bersaglio.
 * <p>
 * <b>La regola per chi gioca.</b> Con la tuta, mentre tocca acqua (non lava), l'acqua non conta per il movimento: la
 * tuta non nuota (niente posa a nuoto, nemmeno correndo sott'acqua), non sale e non salta; cade come all'aria aperta
 * fino al fondo, sul fondo cammina e corre alla velocita' di terra, la corrente non la sposta (neanche quella della
 * lava, che per il resto resta come nel gioco base). Da una vasca si esce da una sponda alta al piu' un blocco, con il
 * gradino, oppure uscendo dal telaio. Lo scatto si spegne sotto il 10 % dell'energia.
 * <p>
 * <b>I due lati concordano senza pacchetto.</b> "Con la tuta" e' {@link SuitSize#wearsSuit} (il segnaposto del torso
 * nello slot del petto, lo stesso sul server e su ogni client) e non spettatore: server e client girano lo stesso
 * codice e concordano su posa, bandierina del nuoto e fisica, anche per chi guarda. Ogni condizione guarda prima
 * {@code wearsSuit}, che e' falso con l'inventario non ancora creato: un giocatore ancora in costruzione non arriva a
 * {@code isSpectator()}. Gradino e acqua non dipendono da energia o pezzi. {@code canSwim} degli indicatori non si
 * legge: nella parte 3 e' sempre falso.
 * <p>
 * <b>Lo scatto.</b> Lo decide il client: {@link #sprintAllowed} lo nega quando gli indicatori ricevuti dal server
 * dicono {@code canSprint} falso; senza indicatori (prima del primo pacchetto della sessione) e' permesso. Il server
 * spegne la corsa solo per i propri conti ({@code SuitTick}, passo 7).
 * <p>
 * <b>Spettatori.</b> Per uno spettatore valgono le regole del gioco: vola, e {@code isAffectedByFluids} e' gia' falso.
 */
public final class SuitMovement {
    /** Chiave del suggerimento sull'acqua al primo contatto dopo ogni ingresso (lo manda {@code SuitTick}, passo 9). */
    public static final String WATER_HINT = "message." + RedstoneExoframes.MOD_ID + ".water_hint";

    private SuitMovement() {
    }

    /**
     * Se i fluidi contano per il movimento di {@code player}; {@code vanilla} e' la risposta del gioco. Falso con la
     * tuta mentre tocca acqua e non lava (galleggiamento, nuoto verso l'alto, velocita' in acqua); altrimenti
     * {@code vanilla}.
     */
    public static boolean affectedByFluids(Player player, boolean vanilla) {
        if (suited(player) && player.isInWater() && !player.isInLava()) {
            return false;
        }
        return vanilla;
    }

    /**
     * Se la corrente dei fluidi sposta {@code player}; {@code vanilla} e' la risposta del gioco. Falso con la tuta, in
     * acqua e in lava (il metodo serve ai due fluidi); altrimenti {@code vanilla}.
     */
    public static boolean pushedByFluid(Player player, boolean vanilla) {
        if (suited(player)) {
            return false;
        }
        return vanilla;
    }

    /** Dopo che il gioco ha deciso se {@code player} nuota: con la tuta non nuota mai. */
    public static void afterUpdateSwimming(Player player) {
        if (suited(player) && player.isSwimming()) {
            player.setSwimming(false);
        }
    }

    /**
     * Se il giocatore locale {@code player} ha di che scattare (lato client); {@code vanilla} e' la risposta del gioco,
     * che decide sia l'avvio sia la fine dello scatto. Falso con la tuta quando gli indicatori ricevuti dicono
     * {@code canSprint} falso; senza indicatori, e altrimenti, {@code vanilla}.
     */
    public static boolean sprintAllowed(Player player, boolean vanilla) {
        if (suited(player)) {
            Optional<FrameIndicators> indicators = Compat.frameState().clientIndicators(player);
            if (indicators.isPresent() && !indicators.get().canSprint()) {
                return false;
            }
        }
        return vanilla;
    }

    /** Con la tuta: il segnaposto del torso indossato, e non spettatore ({@code wearsSuit} per primo). */
    private static boolean suited(Player player) {
        return SuitSize.wearsSuit(player) && !player.isSpectator();
    }
}
