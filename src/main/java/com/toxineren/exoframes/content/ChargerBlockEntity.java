package com.toxineren.exoframes.content;

import com.toxineren.exoframes.compat.CompatBlockEntity;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.config.ConfigValidation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * La block entity del caricatore (design §12.11). Il suo contenuto sta nei dati del chunk, alla radice del suo tag,
 * mai nel registro dei telai; solo un operatore puo' scriverne l'NBT con un comando.
 * <p>
 * In questo primo passo tiene solo la riserva: i RE della polvere di redstone che sta bruciando.
 */
public final class ChargerBlockEntity extends CompatBlockEntity {
    /** RE della polvere che sta bruciando, in [0, {@link ConfigValidation#MAX_RE_PER_REDSTONE}]. */
    private int reserve;

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGER.get(), pos, state);
    }

    /** I RE della polvere che sta bruciando, non ancora passati alla cella. */
    public int reserve() {
        return reserve;
    }

    /** Non lancia mai: la riserva letta si limita a [0, {@link ConfigValidation#MAX_RE_PER_REDSTONE}]. */
    @Override
    protected void readData(CompoundTag tag) {
        reserve = Mth.clamp(tag.getInt(NbtKeys.CHARGER_RESERVE), 0, ConfigValidation.MAX_RE_PER_REDSTONE);
    }

    @Override
    protected void writeData(CompoundTag tag) {
        tag.putInt(NbtKeys.CHARGER_RESERVE, reserve);
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }
}
