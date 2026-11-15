package com.toxineren.exoframes.frame;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.core.armor.DamageKind;
import com.toxineren.exoframes.core.armor.DamageRouter;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import dev.architectury.event.events.common.PlayerEvent;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * Usura dei pezzi di chi indossa la tuta (design §12.7). Codice comune: lo chiama il mixin comune di {@code Player}
 * (una chiamata per aggancio, senza logica) in testa al metodo in cui il gioco consuma l'armatura.
 * <p>
 * <b>Perche' li'.</b> L'importo che arriva in quel punto e' gia' passato da difficolta', scudo e finestra di
 * invulnerabilita' (un secondo colpo uguale o minore nella finestra non arriva, uno maggiore arriva con la sola
 * differenza), e arriva solo per il danno che non aggira l'armatura: la tuta si consuma per cio' che l'armatura avrebbe
 * preso, una volta per colpo applicato. Lava e fuoco la consumano a ogni colpo (voluto: la regola del gioco per gli
 * oggetti immuni al fuoco sta dopo l'aggancio).
 * <p>
 * <b>Guardie</b>, nell'ordine: un {@code ServerPlayer}; un importo maggiore di zero (cosi' anche {@code NaN} esce;
 * dopo uno scudo che para tutto Fabric e NeoForge chiamano l'aggancio con zero); non in creativa
 * ({@code getAbilities().instabuild}, mai {@code isCreative()}: i giocatori finti dei test rispondono sempre vero) ne'
 * spettatore; un telaio indossato. Il registro in sola lettura si riconosce da {@code mutate} che restituisce falso.
 * <p>
 * <b>L'usura.</b> Il tipo di danno da {@code Compat.damage().classify}, l'importo in millesimi di punto, la
 * ripartizione con {@link DamageRouter#routeMilli} dell'unico ripartitore ({@link RedstoneExoframes#damageRouter()})
 * e il riporto del telaio, il tutto in un solo {@code mutate} non strutturale. Dopo, per ogni pezzo entrato in stato
 * critico un messaggio nella barra delle azioni, per ogni pezzo rotto un messaggio e un suono; se un pezzo ha perso
 * almeno un punto il record del tick diventa "da sincronizzare" ({@code SuitTick.markDirty}). Il pezzo rotto resta
 * montato: gli effetti (protezione, velocita', consumo) li ricalcola il tick dell'energia, il riassunto sul torso
 * {@code SlotGuard} al tick seguente.
 * <p>
 * <b>Che cosa non fa.</b> Non annulla mai il colpo, non tocca gli attributi e non riduce il danno (la protezione viene
 * dagli attributi), non lancia mai nel codice del danno del gioco: ogni {@code RuntimeException} si cattura, con una
 * riga ERROR alla prima per giocatore e sessione, poi silenzio.
 */
public final class SuitDamage {
    /** Chiave del messaggio "pezzo in stato critico", con il nome del pezzo. */
    public static final String PIECE_CRITICAL = "message." + RedstoneExoframes.MOD_ID + ".piece_critical";

    /** Chiave del messaggio "pezzo fuori uso", con il nome del pezzo. */
    public static final String PIECE_BROKEN = "message." + RedstoneExoframes.MOD_ID + ".piece_broken";

    /** I giocatori la cui usura ha gia' scritto la riga ERROR in questa sessione; si toglie all'uscita dal server. */
    private static final Set<UUID> FAILURES_LOGGED = ConcurrentHashMap.newKeySet();

    private SuitDamage() {
    }

    /**
     * Registra la pulizia del segno della riga ERROR all'uscita dal server ({@code PlayerEvent.PLAYER_QUIT}). Una
     * volta, nell'init comune.
     */
    public static void register() {
        PlayerEvent.PLAYER_QUIT.register(player -> FAILURES_LOGGED.remove(player.getUUID()));
    }

    /**
     * Il gioco sta per consumare l'armatura di {@code player} per un colpo di {@code amount} da {@code source}: se le
     * guardie lo consentono, consuma i pezzi del telaio indossato. Non annulla mai il colpo e non lancia mai.
     */
    public static void onArmorHurt(Player player, DamageSource source, float amount) {
        if (!(player instanceof ServerPlayer sp) || !(amount > 0F) || sp.getAbilities().instabuild
                || sp.isSpectator()) {
            return;
        }
        try {
            if (Compat.frameState().wornFrameId(sp).isPresent()) {
                wear(sp, Compat.damage().classify(sp, source), amount);
            }
        } catch (RuntimeException e) {
            if (FAILURES_LOGGED.add(sp.getUUID())) {
                RedstoneExoframes.LOGGER.error("Exoframe wear failed for {}", sp.getScoreboardName(), e);
            }
        }
    }

    /**
     * Consuma i pezzi del telaio indossato per un danno di {@code amount} punti di tipo {@code kind}, in un solo
     * {@code mutate}, poi dice i pezzi entrati in stato critico o rotti e segna il record del tick da sincronizzare se
     * un pezzo ha perso almeno un punto.
     */
    private static void wear(ServerPlayer player, DamageKind kind, float amount) {
        long milli = Math.max(0L, Math.min(Integer.MAX_VALUE, Math.round(amount * 1000.0)));
        Map<PieceSlot, PieceCondition> before = new EnumMap<>(PieceSlot.class);
        Map<PieceSlot, PieceCondition> after = new EnumMap<>(PieceSlot.class);
        boolean[] lost = {false};
        boolean changed = Compat.frameState().mutate(player, state -> {
            state.pieces().forEach((slot, piece) -> before.put(slot, piece.condition()));
            DamageRouter.Split split = RedstoneExoframes.damageRouter().routeMilli(kind, milli, state.pieces(),
                    state.damageCarryMilli());
            split.perPiece().forEach((slot, points) -> lost[0] |= state.pieces().get(slot).damage(points) > 0);
            state.pieces().forEach((slot, piece) -> after.put(slot, piece.condition()));
        });
        if (!changed) {
            return; // nessun record, o registro in sola lettura: nulla cambia
        }
        for (PieceSlot slot : PieceSlot.values()) {
            PieceCondition was = before.get(slot);
            PieceCondition now = after.get(slot);
            if (now == PieceCondition.BROKEN && was != PieceCondition.BROKEN) {
                player.displayClientMessage(Component.translatable(PIECE_BROKEN, pieceName(slot)), true);
                FrameSounds.play(player.serverLevel(), player.position(), FrameSounds.Sound.PIECE_BROKEN);
            } else if (now == PieceCondition.CRITICAL && was != PieceCondition.CRITICAL) {
                player.displayClientMessage(Component.translatable(PIECE_CRITICAL, pieceName(slot)), true);
            }
        }
        if (lost[0]) {
            SuitTick.markDirty(player);
        }
    }

    /** Il nome tradotto del pezzo dello slot. */
    private static Component pieceName(PieceSlot slot) {
        return Component.translatable("piece." + RedstoneExoframes.MOD_ID + "." + slot.name().toLowerCase(Locale.ROOT));
    }
}
