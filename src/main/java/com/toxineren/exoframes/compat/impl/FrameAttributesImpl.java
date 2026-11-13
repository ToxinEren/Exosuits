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
 * esattamente quella precedente, anche dopo un login o un cambio di dimensione, e i valori non si sommano mai.
 * I modificatori sono transitori perche' sono derivati dallo stato del telaio, che viene salvato altrove: un
 * modificatore permanente sopravviverebbe al salvataggio dell'entita' anche dopo aver tolto il telaio.
 * <p>
 * Le differenze di versione sono chiuse in {@code key} e {@code modifier}: su 1.21.1 l'id del modificatore e' il
 * ResourceLocation stesso; su 1.20.1 e' un UUID derivato dal ResourceLocation con {@code UUID.nameUUIDFromBytes},
 * quindi uguale a ogni avvio.
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
    /*// L'id con cui l'istanza dell'attributo conosce il modificatore: su 1.20.1 un UUID stabile derivato dal nome.
    private static UUID key(ResourceLocation id) {
        return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    // Il modificatore: "moltiplica totale" se richiesto, altrimenti addendo; il nome e' l'id testuale.
    private static AttributeModifier modifier(ResourceLocation id, boolean multiplyTotal, double amount) {
        return new AttributeModifier(key(id), id.toString(), amount,
                multiplyTotal ? AttributeModifier.Operation.MULTIPLY_TOTAL : AttributeModifier.Operation.ADDITION);
    }
    *///?}
}
