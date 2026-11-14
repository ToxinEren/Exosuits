package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.compat.impl.EntityBase;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Il tipo che le entita' semplici (non viventi) della mod estendono, uguale sulle due versioni del gioco. Le differenze
 * di {@code Entity} fra 1.20.1 e 1.21.1 le chiude la base in {@code compat/impl/} (vedi docs/compat.md): le
 * sottoclassi dichiarano i dati sincronizzati con {@code defineData(DataDefiner)}, dicono se le esplosioni le
 * toccano con {@code ignoresExplosions()} e se i portali le portano via con {@code usesPortals()}, senza vedere le
 * firme delle due versioni.
 */
public abstract class CompatEntity extends EntityBase {
    protected CompatEntity(EntityType<?> type, Level level) {
        super(type, level);
    }
}
