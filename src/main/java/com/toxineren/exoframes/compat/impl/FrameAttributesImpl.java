package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.FrameAttributes;
import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.core.frame.FrameEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
//? if <1.21 {
/*import com.google.common.collect.MapMaker;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;*/
//?}

/**
 * Implementazione di {@link FrameAttributes}.
 * <p>
 * Ogni modificatore ha un id stabile ({@code redstone_exoframes:frame_*}): cosi' una nuova applicazione trova
 * esattamente quella precedente e i valori non si sommano mai. {@code apply} e' idempotente: legge il modificatore
 * presente e, se importo ({@code ==} sul {@code double}) e operazione sono gli stessi, non lo tocca; solo se uno dei
 * due e' diverso lo toglie, e aggiunge quello nuovo con {@code addTransientModifier} (lo aggiunge anche se mancava).
 * Mai {@code addOrUpdateTransientModifier}, che su 1.21.1 sporca l'attributo anche a valori uguali (confronta i
 * riferimenti), e mai {@code AttributeModifier.equals}, che su 1.20.1 confronta solo l'UUID: ogni attributo sporco
 * parte in un pacchetto al proprietario e a chi lo guarda.
 * I modificatori sono transitori perche' sono derivati dallo stato del telaio, che viene salvato altrove: un
 * modificatore permanente sopravviverebbe al salvataggio dell'entita' anche dopo aver tolto il telaio.
 * <p>
 * Gradino della tuta: su 1.21.1 un quinto modificatore, {@code frame_step}, +0,4 {@code ADD_VALUE} su
 * {@code generic.step_height} (0,6 + 0,4 = 1,0), dentro {@code apply} e {@code clear}; il client lo riceve con gli
 * attributi, e {@code localStep} non fa nulla. Su 1.20.1 il passo e' un campo dell'entita' e il {@code ServerPlayer}
 * ha gia' 1,0: niente quinto modificatore, e {@code localStep} alza il campo del solo giocatore locale mentre indossa.
 * <p>
 * Le differenze di versione sono chiuse in {@code key}, {@code modifier}, {@code same}, {@code applyStep},
 * {@code clearStep} e {@code localStep}: su 1.21.1 l'id del modificatore e' il ResourceLocation stesso; su 1.20.1 e'
 * un UUID derivato dal ResourceLocation con {@code UUID.nameUUIDFromBytes}, quindi uguale a ogni avvio, calcolato una
 * volta sola in un campo statico per ciascuno dei quattro modificatori.
 */
public final class FrameAttributesImpl implements FrameAttributes {
    private static final ResourceLocation SPEED = Ids.of("frame_speed");
    private static final ResourceLocation KNOCKBACK = Ids.of("frame_knockback");
    private static final ResourceLocation ARMOR = Ids.of("frame_armor");
    private static final ResourceLocation TOUGHNESS = Ids.of("frame_toughness");
    private static final ResourceLocation STEP = Ids.of("frame_step");

    /** Quanto il gradino della tuta aggiunge al passo vanilla (0,6) su 1.21.1: 1,0 esatto in {@code double}. */
    private static final double STEP_BONUS = 0.4;

    @Override
    public void apply(LivingEntity e, FrameEffects fx) {
        set(e.getAttribute(Attributes.MOVEMENT_SPEED), SPEED, true, fx.speedMultiplier() - 1.0);
        set(e.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK, false, fx.knockbackResistance());
        // armorPoints() e non protectionPoints: getArmorValue() tronca l'attributo, 19.995 diventerebbe 19
        set(e.getAttribute(Attributes.ARMOR), ARMOR, false, fx.armorPoints());
        set(e.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS, false, fx.toughness());
        applyStep(e);
    }

    @Override
    public void clear(LivingEntity e) {
        remove(e.getAttribute(Attributes.MOVEMENT_SPEED), SPEED);
        remove(e.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK);
        remove(e.getAttribute(Attributes.ARMOR), ARMOR);
        remove(e.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS);
        clearStep(e);
    }

    @Override
    public void localStep(Player player, boolean wearing) {
        //? if <1.21 {
        /*if (!player.isLocalPlayer()) {
            return; // mai su un ServerPlayer, che ha gia' passo 1,0 dal costruttore
        }
        if (wearing) {
            if (player.maxUpStep() < SUIT_STEP) {
                player.setMaxUpStep(SUIT_STEP);
                RAISED.add(player);
            }
        } else if (RAISED.remove(player) && player.maxUpStep() == SUIT_STEP) {
            // solo il passo alzato dalla mod e ancora com'era: quello messo da un'altra mod resta
            player.setMaxUpStep(VANILLA_STEP);
        }
        *///?}
    }

    /**
     * Imposta il modificatore con id dato: se ce n'e' gia' uno con lo stesso importo e la stessa operazione non lo
     * tocca (l'attributo non si sporca e non si reinvia); altrimenti toglie quello presente e aggiunge il nuovo.
     * Istanza null: niente.
     */
    private static void set(AttributeInstance inst, ResourceLocation id, boolean multiplyTotal, double amount) {
        if (inst == null) {
            return;
        }
        AttributeModifier present = inst.getModifier(key(id));
        if (present != null) {
            if (same(present, multiplyTotal, amount)) {
                return;
            }
            inst.removeModifier(key(id));
        }
        inst.addTransientModifier(modifier(id, multiplyTotal, amount));
    }

    /** Rimuove il modificatore con id dato, se l'entita' ha l'attributo. */
    private static void remove(AttributeInstance inst, ResourceLocation id) {
        if (inst != null) {
            inst.removeModifier(key(id));
        }
    }

    //? if >=1.21 {
    /** L'id con cui l'istanza dell'attributo conosce il modificatore: su 1.21.1 il ResourceLocation stesso. */
    private static ResourceLocation key(ResourceLocation id) {
        return id;
    }

    /** Il modificatore: "moltiplica totale" se richiesto, altrimenti addendo. */
    private static AttributeModifier modifier(ResourceLocation id, boolean multiplyTotal, double amount) {
        return new AttributeModifier(id, amount,
                multiplyTotal ? AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL : AttributeModifier.Operation.ADD_VALUE);
    }

    /** Vero se il modificatore ha gia' questo importo e questa operazione. */
    private static boolean same(AttributeModifier m, boolean multiplyTotal, double amount) {
        return m.amount() == amount && m.operation() == (multiplyTotal
                ? AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL : AttributeModifier.Operation.ADD_VALUE);
    }

    /** Il gradino della tuta: il quinto modificatore, +0,4 sul passo. */
    private static void applyStep(LivingEntity e) {
        set(e.getAttribute(Attributes.STEP_HEIGHT), STEP, false, STEP_BONUS);
    }

    /** Toglie il gradino della tuta, se c'e'. */
    private static void clearStep(LivingEntity e) {
        remove(e.getAttribute(Attributes.STEP_HEIGHT), STEP);
    }
    //?} else {
    /*// Gli UUID dei quattro modificatori su 1.20.1, derivati una volta sola dai ResourceLocation.
    private static final UUID SPEED_UUID = uuidOf(SPEED);
    private static final UUID KNOCKBACK_UUID = uuidOf(KNOCKBACK);
    private static final UUID ARMOR_UUID = uuidOf(ARMOR);
    private static final UUID TOUGHNESS_UUID = uuidOf(TOUGHNESS);

    // Un UUID stabile derivato dal nome: uguale a ogni avvio.
    private static UUID uuidOf(ResourceLocation id) {
        return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    // L'id con cui l'istanza dell'attributo conosce il modificatore: su 1.20.1 l'UUID precalcolato dell'id.
    private static UUID key(ResourceLocation id) {
        if (id == SPEED) {
            return SPEED_UUID;
        } else if (id == KNOCKBACK) {
            return KNOCKBACK_UUID;
        } else if (id == ARMOR) {
            return ARMOR_UUID;
        } else if (id == TOUGHNESS) {
            return TOUGHNESS_UUID;
        }
        throw new IllegalArgumentException("Unknown frame attribute id: " + id);
    }

    // Il modificatore: "moltiplica totale" se richiesto, altrimenti addendo; il nome e' l'id testuale.
    private static AttributeModifier modifier(ResourceLocation id, boolean multiplyTotal, double amount) {
        return new AttributeModifier(key(id), id.toString(), amount,
                multiplyTotal ? AttributeModifier.Operation.MULTIPLY_TOTAL : AttributeModifier.Operation.ADDITION);
    }

    // Vero se il modificatore ha gia' questo importo e questa operazione (equals confronterebbe solo l'UUID).
    private static boolean same(AttributeModifier m, boolean multiplyTotal, double amount) {
        return m.getAmount() == amount && m.getOperation() == (multiplyTotal
                ? AttributeModifier.Operation.MULTIPLY_TOTAL : AttributeModifier.Operation.ADDITION);
    }

    // Su 1.20.1 il passo e' un campo del client: localStep. key(STEP) qui non si chiama mai.
    private static void applyStep(LivingEntity e) {
    }

    // Su 1.20.1 il passo e' un campo del client: localStep.
    private static void clearStep(LivingEntity e) {
    }

    // Passo della tuta e passo vanilla del giocatore, per localStep.
    private static final float SUIT_STEP = 1.0F;
    private static final float VANILLA_STEP = 0.6F;

    // I giocatori locali a cui localStep ha alzato il passo: chiavi deboli confrontate per identita' (come
    // SuitSize.LAST_SEEN: Entity.equals confronta gli id). Un LocalPlayer nuovo (rinascita, cambio di dimensione) non
    // c'e'.
    private static final Set<Player> RAISED = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
    *///?}
}
