package com.toxineren.exoframes.compat;

import net.minecraft.world.item.Item;

/**
 * Item comuni che servono al rendering della tuta. Codice comune: un server dedicato lo carica, quindi ne' questa
 * interfaccia ne' la sua implementazione toccano classi client.
 */
public interface SuitAssets {
    /**
     * L'item "modello della tuta": l'animatable GeckoLib ({@code GeoItem}) che il renderer della tuta disegna con uno
     * stack fittizio. Non si indossa mai e non sta in nessuna scheda creativa; va registrato (il renderer di
     * GeckoLib 4.9 interroga i tag dello stack), una volta sola, nel registro degli item della mod.
     */
    Item newSuitModelItem(Item.Properties props);
}
