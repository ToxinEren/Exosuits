package com.toxineren.exoframes.entity;

import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.CompatEntity;
import com.toxineren.exoframes.compat.PlacedFrame;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.frame.SuitSize;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Il telaio piazzato nel mondo, come lo vedono e lo cliccano i giocatori (design §4.2, §4.5, §5.4). E' solo una vista
 * del record piazzato del registro con la sua stessa identita': l'UUID dell'entita' e' il {@code frameId}, nel suo NBT
 * non c'e' nulla oltre ai dati vanilla, e ai client va solo il riassunto visivo dello stato, come {@link CompoundTag}
 * sincronizzato. Il registro e' la verita': al primo tick sul server l'entita' si confronta con il suo record (vedi
 * {@link #tick}).
 * <p>
 * Immobile: nessuna gravita', e nessun movimento (il tick non la muove e {@link #move} non fa nulla, quindi nemmeno i
 * pistoni la spostano). Si clicca ({@link #isPickable}), ma non si spinge, non spinge e non blocca il movimento di
 * nessuno ({@code canBeCollidedWith} resta falso come in {@code Entity}). Non si distrugge a colpi ne' con le
 * esplosioni, e nessun portale la porta via; {@code kill()} e {@code discard()} la tolgono come al solito (il record
 * resta, e il riallineamento la ricrea, vedi {@code frame/FrameRealign}).
 */
public final class ExoframeEntity extends CompatEntity {
    /** Il riassunto visivo, scritto con {@code FrameStateAccess.writeSummaryTag}. */
    private static final EntityDataAccessor<CompoundTag> SUMMARY =
            SynchedEntityData.defineId(ExoframeEntity.class, EntityDataSerializers.COMPOUND_TAG);

    /** Allargamento orizzontale, per lato, del riquadro di culling: braccia della tuta e un margine per i pezzi. */
    private static final double CULLING_INFLATE = SuitSize.CULLING_MARGIN + 0.5;

    /** Vero dopo il confronto con il registro del primo tick sul server; non si salva. */
    private boolean checkedRegistry;

    public ExoframeEntity(EntityType<? extends ExoframeEntity> type, Level level) {
        super(type, level);
    }

    /** L'identita' del telaio: l'UUID dell'entita'. */
    public UUID frameId() {
        return getUUID();
    }

    /** Il riassunto visivo sincronizzato; il telaio nudo se il tag non ne ha uno. */
    public SuitSummary summary() {
        return Compat.frameState().readSummaryTag(getEntityData().get(SUMMARY)).orElse(SuitSummary.BARE);
    }

    /** Imposta il riassunto visivo, che il gioco manda ai client che vedono l'entita'. */
    public void setSummary(SuitSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        getEntityData().set(SUMMARY, Compat.frameState().writeSummaryTag(summary));
    }

    @Override
    protected void defineData(DataDefiner definer) {
        definer.define(SUMMARY, Compat.frameState().writeSummaryTag(SuitSummary.BARE));
    }

    /**
     * Al primo tick sul server (l'entita' e' gia' nel livello) si confronta con il registro: senza un record piazzato
     * con la sua identita', o con il record in un'altra dimensione, si elimina (il riallineamento la ricrea dove
     * deve stare); altrimenti si porta nel punto e con l'orientamento del record e ne prende il riassunto visivo, che
     * non sta nell'NBT e dopo un caricamento manca.
     */
    @Override
    public void tick() {
        if (!checkedRegistry && level() instanceof ServerLevel serverLevel) {
            checkedRegistry = true;
            if (!alignToRecord(serverLevel)) {
                return;
            }
        }
        super.tick();
    }

    /** Il confronto con il registro del primo tick; falso se l'entita' si e' eliminata. */
    private boolean alignToRecord(ServerLevel level) {
        Optional<PlacedFrame> placed = Compat.frameState().placed(level.getServer(), frameId());
        if (placed.isEmpty() || !placed.get().dimension().equals(level.dimension())) {
            discard();
            return false;
        }
        PlacedFrame frame = placed.get();
        Vec3 spot = frame.position();
        if (!position().equals(spot) || getYRot() != frame.yaw()) {
            moveTo(spot.x, spot.y, spot.z, frame.yaw(), 0f);
        }
        setSummary(SuitSummary.of(frame.state()));
        return true;
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        // immobile: la posizione la decide il registro
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    /** Nessun danno, da nessuna fonte (giocatori in creativa compresi): il telaio si toglie solo dal registro. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected boolean ignoresExplosions() {
        return true;
    }

    /**
     * Nessun portale la porta via: la copia arrivata dall'altra parte si eliminerebbe (il record e' di qui) e il
     * riallineamento la ricreerebbe qui, a ogni giro caricando o generando l'altra parte.
     */
    @Override
    protected boolean usesPortals() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(CULLING_INFLATE, 0.0, CULLING_INFLATE);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // nulla di proprio: il contenuto sta nel registro, l'identita' e' l'UUID vanilla
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // nulla di proprio: il contenuto sta nel registro, l'identita' e' l'UUID vanilla
    }
}
