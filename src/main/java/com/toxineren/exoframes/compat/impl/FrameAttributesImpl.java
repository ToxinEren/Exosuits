package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.FrameAttributes;
import com.toxineren.exoframes.compat.Ids;
import com.toxineren.exoframes.core.frame.FrameEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
//? if <1.21 {
/*import java.nio.charset.StandardCharsets;
import java.util.UUID;*/
//?}

/**
 * Implementazione di {@link FrameAttributes}.
 * <p>
 * Ogni modificatore ha un id stabile ({@code redstone_exoframes:frame_*}): cosi' una nuova applicazione rimuove
 * esattamente quella precedente e i valori non si sommano mai. Vanno riapplicati al login, al respawn (compresa
 * l'uscita dall'End: il nuovo ServerPlayer riceve solo i valori base degli attributi) e a ogni cambio del telaio; un
 * semplice cambio di dimensione conserva l'entita' e i modificatori.
 * I modificatori sono transitori perche' sono derivati dallo stato del telaio, che viene salvato altrove: un
 * modificatore permanente sopravviverebbe al salvataggio dell'entita' anche dopo aver tolto il telaio.
 * <p>
 * Le differenze di versione sono chiuse in {@code key} e {@code modifier}: su 1.21.1 l'id del modificatore e' il
 * ResourceLocation stesso; su 1.20.1 e' un UUID derivato dal ResourceLocation con {@code UUID.nameUUIDFromBytes},
 * quindi uguale a ogni avvio, calcolato una volta sola in un campo statico per ciascuno dei quattro modificatori.
 */
public final class FrameAttributesImpl implements FrameAttributes {
    private static final ResourceLocation SPEED = Ids.of("frame_speed");
    private static final ResourceLocation KNOCKBACK = Ids.of("frame_knockback");
    private static final ResourceLocation ARMOR = Ids.of("frame_armor");
    private static final ResourceLocation TOUGHNESS = Ids.of("frame_toughness");

    @Override
    public void apply(LivingEntity e, FrameEffects fx) {
        set(e.getAttribute(Attributes.MOVEMENT_SPEED), SPEED, true, fx.speedMultiplier() - 1.0);
        set(e.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK, false, fx.knockbackResistance());
        // armorPoints() e non protectionPoints: getArmorValue() tronca l'attributo, 19.995 diventerebbe 19
        set(e.getAttribute(Attributes.ARMOR), ARMOR, false, fx.armorPoints());
        set(e.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS, false, fx.toughness());
    }

    @Override
    public void clear(LivingEntity e) {
        remove(e.getAttribute(Attributes.MOVEMENT_SPEED), SPEED);
        remove(e.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK);
        remove(e.getAttribute(Attributes.ARMOR), ARMOR);
        remove(e.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS);
    }

    /** Sostituisce il modificatore con id dato: prima lo rimuove, poi aggiunge quello nuovo. Istanza null: niente. */
    private static void set(AttributeInstance inst, ResourceLocation id, boolean multiplyTotal, double amount) {
        if (inst == null) {
            return;
        }
        inst.removeModifier(key(id));
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
    *///?}
}
