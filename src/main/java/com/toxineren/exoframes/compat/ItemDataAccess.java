package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/**
 * Dati tipizzati sugli item della mod (celle, pezzi, telaio), indipendenti dalla versione.
 * <p>
 * I dati stanno nel tag {@link NbtKeys#ROOT} con le chiavi di {@link NbtKeys}: su 1.20.1 nel tag dello stack, su
 * 1.21.1 nel componente vanilla custom_data (nessun componente registrato). Il contenuto del tag e' lo stesso sulle
 * due versioni, cosi' i dati sopravvivono all'aggiornamento del mondo. In lettura le chiavi mancanti assumono valori
 * di default e gli id sconosciuti sono ignorati (con un avviso nel log per id). Le scritture su uno stack vuoto non
 * fanno nulla.
 */
public interface ItemDataAccess {
    /** Energia della cella ({@link NbtKeys#ENERGY} nel tag radice); 0 se assente. */
    int cellEnergy(ItemStack stack);

    /** Scrive l'energia della cella, conservando le altre chiavi del tag radice. */
    void setCellEnergy(ItemStack stack, int energy);

    /** I dati del pezzo; vuoto se manca il tag radice o se il livello manca o e' sconosciuto. */
    Optional<PieceData> piece(ItemStack stack);

    /** Scrive tutti i campi del pezzo, conservando le altre chiavi del tag radice. */
    void setPiece(ItemStack stack, PieceData data);

    /** I dati del telaio; vuoto se manca il tag radice. */
    Optional<FrameItemData> frame(ItemStack stack);

    /** Scrive tutti i campi del telaio, conservando le altre chiavi del tag radice. */
    void setFrame(ItemStack stack, FrameItemData data);

    /** Vero se lo stack ha il tag radice {@link NbtKeys#ROOT}. */
    boolean hasData(ItemStack stack);

    /**
     * Dati di un pezzo: livello, durabilita', moduli installati e colori (RGB). {@code tier} e {@code modules} non
     * possono essere null.
     */
    record PieceData(Tier tier, int durability, int maxDurability, List<ModuleType> modules, int colorPrimary,
            int colorSecondary) {
        public PieceData {
            Objects.requireNonNull(tier, "tier must not be null");
            Objects.requireNonNull(modules, "modules must not be null");
        }
    }

    /**
     * Dati di un telaio: proprietario (vuoto se non assegnato; in scrittura la chiave {@link NbtKeys#OWNER} c'e' solo
     * se presente, in lettura una chiave assente o non valida da' vuoto), stato, energia della cella installata
     * (vuota se nessuna cella) e pezzi montati per slot. {@code owner}, {@code cellEnergy} e {@code pieces} non
     * possono essere null.
     */
    record FrameItemData(Optional<UUID> owner, boolean unlocked, boolean reinforced, Optional<Integer> cellEnergy,
            Map<PieceSlot, PieceData> pieces) {
        public FrameItemData {
            Objects.requireNonNull(owner, "owner must not be null");
            Objects.requireNonNull(cellEnergy, "cellEnergy must not be null");
            Objects.requireNonNull(pieces, "pieces must not be null");
        }
    }
}
