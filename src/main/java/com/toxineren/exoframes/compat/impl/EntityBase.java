package com.toxineren.exoframes.compat.impl;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
//? if >=1.21 {
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Explosion;
//?}

/**
 * Base delle entita' semplici (non viventi) della mod: chiude le differenze di {@link Entity} fra 1.20.1 e 1.21.1 dietro
 * agganci senza versione. Il codice della mod non la estende direttamente: estende {@code compat.CompatEntity}.
 * <p>
 * Differenze chiuse:
 * <ul>
 * <li>dati sincronizzati: {@code defineSynchedData()} con {@code entityData.define} su 1.20.1,
 * {@code defineSynchedData(SynchedEntityData.Builder)} con {@code builder.define} su 1.21.1; le sottoclassi li
 * dichiarano in {@link #defineData}, che il costruttore di {@link Entity} chiama prima di quello della sottoclasse
 * (quindi le chiavi vanno in campi statici);</li>
 * <li>esplosioni: {@code ignoreExplosion()} su 1.20.1, {@code ignoreExplosion(Explosion)} su 1.21.1; le sottoclassi
 * rispondono con {@link #ignoresExplosions}.</li>
 * <li>portali: su 1.20.1 i blocchi portale del Nether e dell'End chiedono {@code canChangeDimensions()} e il portale di
 * passaggio dell'End (dal tick del suo block entity, {@code TheEndGatewayBlockEntity.canEntityTeleport}) chiede
 * {@code isOnPortalCooldown()}; su 1.21.1 i tre blocchi chiedono {@code canUsePortal(boolean)}, e
 * {@code Entity.handlePortal} anche {@code canChangeDimensions(Level, Level)}. Le sottoclassi rispondono con
 * {@link #usesPortals}.</li>
 * </ul>
 * Il pacchetto di spawn resta quello vanilla sulle due versioni: {@code getAddEntityPacket()} non e' astratto in
 * {@link Entity} di 1.20.1 (crea un {@code ClientboundAddEntityPacket}, anche con le patch di Forge 47.4) e
 * {@code getAddEntityPacket(ServerEntity)} di 1.21.1 fa lo stesso; i loader sincronizzano il registro dei tipi, quindi
 * il client crea l'entita' dal tipo nel pacchetto. L'altezza degli occhi non e' toccata.
 */
public abstract class EntityBase extends Entity {
    protected EntityBase(EntityType<?> type, Level level) {
        super(type, level);
    }

    /**
     * Dichiara i dati sincronizzati dell'entita', ognuno con il suo valore iniziale. Lo chiama il costruttore di
     * {@link Entity}: i campi d'istanza della sottoclasse non sono ancora inizializzati.
     */
    protected abstract void defineData(DataDefiner definer);

    /** True se le esplosioni non toccano l'entita' (niente danno, niente spinta); falso di base. */
    protected boolean ignoresExplosions() {
        return false;
    }

    /** False se nessun portale (Nether, End, passaggio dell'End) porta via l'entita'; vero di base. */
    protected boolean usesPortals() {
        return true;
    }

    //? if >=1.21 {
    @Override
    protected final void defineSynchedData(SynchedEntityData.Builder builder) {
        defineData(new DataDefiner() {
            @Override
            public <T> void define(EntityDataAccessor<T> key, T initial) {
                builder.define(key, initial);
            }
        });
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return ignoresExplosions() || super.ignoreExplosion(explosion);
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return usesPortals() && super.canUsePortal(allowPassengers);
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return usesPortals() && super.canChangeDimensions(from, to);
    }
    //?} else {
    /*@Override
    protected final void defineSynchedData() {
        defineData(new DataDefiner() {
            @Override
            public <T> void define(EntityDataAccessor<T> key, T initial) {
                entityData.define(key, initial);
            }
        });
    }

    @Override
    public boolean ignoreExplosion() {
        return ignoresExplosions() || super.ignoreExplosion();
    }

    @Override
    public boolean canChangeDimensions() {
        return usesPortals() && super.canChangeDimensions();
    }

    @Override
    public boolean isOnPortalCooldown() {
        return !usesPortals() || super.isOnPortalCooldown();
    }
    *///?}

    /** Chi dichiara i dati sincronizzati in {@link #defineData}: una chiave con il suo valore iniziale. */
    public interface DataDefiner {
        <T> void define(EntityDataAccessor<T> key, T initial);
    }
}
