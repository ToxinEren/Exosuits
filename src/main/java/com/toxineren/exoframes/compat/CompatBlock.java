package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.compat.impl.BlockBase;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Il tipo che i blocchi della mod senza block entity estendono, uguale sulle due versioni del gioco. Il clic destro su
 * un blocco cambia fra 1.20.1 ({@code use}) e 1.21.1 ({@code useItemOn} e {@code useWithoutItem}): lo traduce la base
 * in {@code compat/impl/} (vedi docs/compat.md), e le sottoclassi rispondono al clic con
 * {@code onClick(state, level, pos, player, hand, hit)}, ereditato, che di base passa e che il gioco chiama una volta
 * per mano sui due lati, senza vedere le firme delle due versioni. Allo stesso modo {@code playerWillDestroy}
 * restituisce {@code void} su 1.20.1 e {@code BlockState} su 1.21.1: le sottoclassi agiscono prima della rottura di un
 * giocatore con {@code onPlayerWillDestroy(level, pos, state, player)}, ereditato e vuoto di base, che la base chiama
 * prima di {@code super}. I blocchi con una block entity estendono invece {@link CompatEntityBlock}, con gli stessi
 * metodi e la stessa traduzione.
 * <p>
 * {@code updateShape}, {@code rotate} e {@code mirror} sono pubblici su 1.20.1 e protetti su 1.21.1: le sottoclassi
 * li ridefiniscono {@code public}, che compila sulle due versioni senza cuciture.
 */
public abstract class CompatBlock extends BlockBase {
    protected CompatBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
