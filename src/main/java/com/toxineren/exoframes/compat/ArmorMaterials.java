package com.toxineren.exoframes.compat;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;

/**
 * Il materiale d'armatura segnaposto "frame", condiviso dai quattro slot dei pezzi della mod.
 * <p>
 * Vanilla vuole un materiale per costruire un {@link ArmorItem}, ma la protezione reale dei pezzi NON passa da qui:
 * e' un attributo dinamico calcolato dallo stato del telaio (Task 6). Per questo il materiale ha difesa, robustezza,
 * resistenza al contraccolpo e incantabilita' a zero. Su 1.20.1 il materiale e' un valore Java; su 1.21.1 e' una
 * voce del registro vanilla dei materiali d'armatura, raggiunta tramite un Holder.
 */
public interface ArmorMaterials {
    /**
     * Registra il materiale dove la versione lo richiede (su 1.21.1 il registro dei materiali d'armatura; su 1.20.1
     * non fa nulla). Va chiamato esattamente una volta, nell'init comune, prima della registrazione degli item: su
     * 1.21.1 una seconda chiamata lancia un'eccezione.
     */
    void register();

    /**
     * Il materiale "frame" nella forma propria della versione: il valore di {@code ArmorMaterial} su 1.20.1, il
     * {@code Holder<ArmorMaterial>} su 1.21.1. Il tipo e' Object perche' le due forme non hanno un supertipo comune.
     */
    Object frameMaterialHandle();

    /**
     * Un {@link ArmorItem} del tipo dato con il materiale "frame" (difesa 0): chiude la differenza di costruttore.
     * L'item ha sempre la pila a 1, su entrambe le versioni; le proprieta' date possono avere o no una durabilita'
     * vanilla.
     */
    ArmorItem newPlaceholder(ArmorItem.Type type, Item.Properties props);
}
