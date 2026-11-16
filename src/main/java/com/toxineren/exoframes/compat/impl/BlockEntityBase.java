package com.toxineren.exoframes.compat.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
//?}

/**
 * Base delle block entity della mod: chiude le differenze di lettura e scrittura dell'NBT di {@link BlockEntity} fra
 * 1.20.1 e 1.21.1 dietro due metodi neutri senza versione. Il codice della mod non la estende direttamente: estende
 * {@code compat.CompatBlockEntity}.
 * <p>
 * Differenze chiuse:
 * <ul>
 * <li>lettura: {@code load(CompoundTag)} (pubblico) su 1.20.1, {@code loadAdditional(CompoundTag,
 * HolderLookup.Provider)} (protetto) su 1.21.1; le sottoclassi leggono in {@link #readData};</li>
 * <li>scrittura: {@code saveAdditional(CompoundTag)} su 1.20.1, {@code saveAdditional(CompoundTag,
 * HolderLookup.Provider)} su 1.21.1; le sottoclassi scrivono in {@link #writeData}.</li>
 * </ul>
 * L'override di ogni versione e' {@code final} e chiama prima {@code super} (che su Forge e NeoForge legge e scrive i
 * dati del loader), poi il metodo neutro. Il provider dei registri non arriva alle sottoclassi: i dati delle block
 * entity della mod sono numeri e booleani, nessuno stack.
 */
public abstract class BlockEntityBase extends BlockEntity {
    protected BlockEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Legge dal tag della block entity i dati della sottoclasse. Il tag viene dal disco o da un comando: ogni valore va
     * limitato, e la lettura non deve mai lanciare.
     */
    protected abstract void readData(CompoundTag tag);

    /** Scrive nel tag della block entity i dati della sottoclasse, accanto a quelli del gioco. */
    protected abstract void writeData(CompoundTag tag);

    //? if >=1.21 {
    @Override
    protected final void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readData(tag);
    }

    @Override
    protected final void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeData(tag);
    }
    //?} else {
    /*@Override
    public final void load(CompoundTag tag) {
        super.load(tag);
        readData(tag);
    }

    @Override
    protected final void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeData(tag);
    }
    *///?}
}
