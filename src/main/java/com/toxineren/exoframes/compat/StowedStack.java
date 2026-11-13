package com.toxineren.exoframes.compat;

import java.util.Objects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Un oggetto dell'armatura stivata in un telaio, con lo slot da cui e' stato tolto (vedi
 * {@link FrameStateAccess#stow}): all'uscita torna in quello slot. Nessun componente puo' essere null; lo stack e'
 * quello passato, non una copia.
 */
public record StowedStack(EquipmentSlot slot, ItemStack stack) {
    public StowedStack {
        Objects.requireNonNull(slot, "slot must not be null");
        Objects.requireNonNull(stack, "stack must not be null");
    }
}
