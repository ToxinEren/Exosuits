package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.DamageClassifier;
import com.toxineren.exoframes.core.armor.DamageKind;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Implementazione di {@link DamageClassifier}.
 * <p>
 * Ordine e soglie: caduta per prima; poi FROM_ABOVE, che e' il caso piu' specifico tra i proiettili, prima di HEAD,
 * le cui condizioni altrimenti lo coprirebbero; poi HEAD; infine GENERIC.
 * <ul>
 * <li>FROM_ABOVE: proiettile e quota di chi lo ha causato ({@code source.getEntity()}) oltre
 * {@code victim.getEyeY() + 1.0}. Si usa di proposito la posizione del tiratore, e precisamente {@code getY()},
 * cioe' i suoi piedi: conta da dove e' partito il colpo (proiettili dall'alto: elmo e torso), non il punto in cui la
 * freccia ha toccato la vittima. Senza entita' causante si ripiega sulla posizione sorgente.</li>
 * <li>HEAD: posizione sorgente (punto d'impatto o posizione dell'entita' diretta) con
 * {@code y >= victim.getEyeY() - 0.25}.</li>
 * </ul>
 * {@code getEntity()} e {@code getSourcePosition()} possono essere null: senza posizione la condizione e' falsa.
 * Tag e metodi usati sono identici su 1.20.1 e 1.21.1, quindi qui non ci sono condizioni di versione.
 */
public final class DamageClassifierImpl implements DamageClassifier {
    private static final double FROM_ABOVE_MARGIN = 1.0;
    private static final double HEAD_MARGIN = 0.25;

    @Override
    public DamageKind classify(LivingEntity victim, DamageSource source) {
        if (isFall(source)) {
            return DamageKind.FALL;
        }
        double eyeY = victim.getEyeY();
        Vec3 sourcePos = source.getSourcePosition();
        if (isProjectile(source)) {
            Entity shooter = source.getEntity();
            if (shooter != null) {
                if (shooter.getY() > eyeY + FROM_ABOVE_MARGIN) {
                    return DamageKind.FROM_ABOVE;
                }
            } else if (sourcePos != null && sourcePos.y > eyeY + FROM_ABOVE_MARGIN) {
                return DamageKind.FROM_ABOVE;
            }
        }
        if (sourcePos != null && sourcePos.y >= eyeY - HEAD_MARGIN) {
            return DamageKind.HEAD;
        }
        return DamageKind.GENERIC;
    }

    @Override
    public boolean isProjectile(DamageSource source) {
        return source.is(DamageTypeTags.IS_PROJECTILE);
    }

    @Override
    public boolean isFall(DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL);
    }

    @Override
    public boolean bypassesArmor(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_ARMOR);
    }
}
