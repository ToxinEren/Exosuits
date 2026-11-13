package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.ItemDataAccess.PieceData;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Piece;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.module.ModuleType;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Oggetto di un pezzo d'armatura di un livello e di un tipo, da montare a mano sul telaio. Durabilita', moduli e
 * aspetto stanno nei dati dello stack ({@link Compat#itemData()}).
 */
public final class PieceItem extends Item {
    private final Tier tier;
    private final PieceKind kind;

    public PieceItem(Tier tier, PieceKind kind, Item.Properties properties) {
        super(properties);
        this.tier = Objects.requireNonNull(tier, "tier must not be null");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
    }

    public Tier tier() {
        return tier;
    }

    public PieceKind kind() {
        return kind;
    }

    /**
     * Il pezzo che lo stack rappresenta, montato nella posizione {@code slot}. Senza dati di pezzo e' un pezzo nuovo
     * del livello dell'oggetto (durabilita' piena dalla config). Con dati dello stesso livello si tengono durabilita'
     * e durabilita' massima salvate. Con dati di un altro livello (stack scritto a mano o da altre mod) vince il
     * livello dell'oggetto: la durabilita' massima e' quella del livello nella config e la durabilita' salvata viene
     * ridotta a quel massimo se lo supera.
     *
     * @throws IllegalArgumentException se {@code slot} non e' una posizione di questo tipo di pezzo
     */
    public Piece toPiece(ItemStack stack, PieceSlot slot) {
        Objects.requireNonNull(stack, "stack must not be null");
        Objects.requireNonNull(slot, "slot must not be null");
        if (!kind.slots().contains(slot)) {
            throw new IllegalArgumentException("a " + kind + " piece does not fit slot " + slot);
        }
        Optional<PieceData> data = Compat.itemData().piece(stack);
        if (data.isEmpty()) {
            return Piece.fresh(slot, tier, RedstoneExoframes.config().tiers);
        }
        PieceData stored = data.get();
        if (stored.tier() == tier) {
            return new Piece(slot, tier, stored.maxDurability(), stored.durability());
        }
        Piece fresh = Piece.fresh(slot, tier, RedstoneExoframes.config().tiers);
        return new Piece(slot, tier, fresh.maxDurability(), Math.min(stored.durability(), fresh.maxDurability()));
    }

    /**
     * Lo stack (uno) dell'oggetto del livello e del tipo del pezzo, con durabilita', moduli e aspetto nei dati.
     *
     * @throws IllegalArgumentException se non c'e' un oggetto per il livello del pezzo (in questa parte solo Rottame)
     */
    public static ItemStack fromPiece(Piece piece, List<ModuleType> modules, Appearance appearance) {
        Objects.requireNonNull(piece, "piece must not be null");
        PieceKind kind = PieceKind.of(piece.slot());
        for (RegistrySupplier<PieceItem> supplier : ModItems.PIECES) {
            PieceItem item = supplier.get();
            if (item.tier == piece.tier() && item.kind == kind) {
                ItemStack stack = new ItemStack(item);
                Compat.itemData().setPiece(stack, new PieceData(piece.tier(), piece.durability(),
                        piece.maxDurability(), modules, appearance));
                return stack;
            }
        }
        throw new IllegalArgumentException("no item for a " + piece.tier() + " " + kind + " piece");
    }
}
