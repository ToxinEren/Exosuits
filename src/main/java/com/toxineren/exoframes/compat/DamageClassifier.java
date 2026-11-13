package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.core.armor.DamageKind;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Traduce un {@link DamageSource} vanilla nella {@link DamageKind} del core, che decide quali pezzi del telaio
 * assorbono il danno per primi. Le domande sul tipo di danno passano dai tag vanilla dei tipi di danno.
 */
public interface DamageClassifier {
    /**
     * Classifica il danno subito da {@code victim}, nell'ordine (la prima condizione vera vince):
     * <ol>
     * <li>{@link DamageKind#FALL} se {@link #isFall} e' vero;</li>
     * <li>{@link DamageKind#FROM_ABOVE} se {@link #isProjectile} e' vero e chi ha causato il danno sta oltre
     * {@code victim.getEyeY() + 1.0} (posizione dei piedi del tiratore; senza tiratore, la posizione sorgente);</li>
     * <li>{@link DamageKind#HEAD} se la posizione sorgente esiste e ha {@code y >= victim.getEyeY() - 0.25};</li>
     * <li>{@link DamageKind#GENERIC} altrimenti.</li>
     * </ol>
     */
    DamageKind classify(LivingEntity victim, DamageSource source);

    /** Vero se il tipo di danno e' nel tag dei proiettili. */
    boolean isProjectile(DamageSource source);

    /** Vero se il tipo di danno e' nel tag delle cadute. */
    boolean isFall(DamageSource source);

    /** Vero se il tipo di danno e' nel tag di quelli che ignorano l'armatura. */
    boolean bypassesArmor(DamageSource source);
}
