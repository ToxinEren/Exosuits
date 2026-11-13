package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/**
 * Dati tipizzati sugli item della mod (celle, pezzi, telaio), indipendenti dalla versione.
 * <p>
 * I dati stanno nel tag {@link NbtKeys#ROOT} con le chiavi di {@link NbtKeys}: su 1.20.1 nel tag dello stack, su
 * 1.21.1 nel componente vanilla custom_data (nessun componente registrato). Il contenuto del tag e' lo stesso sulle
 * due versioni, cosi' i dati sopravvivono all'aggiornamento del mondo. In lettura le chiavi mancanti assumono valori
 * di default e gli id sconosciuti sono ignorati (con un avviso nel log per id).
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

    /** Dati di un pezzo: livello, durabilita', moduli installati e colori (RGB). */
    record PieceData(Tier tier, int durability, int maxDurability, List<ModuleType> modules, int colorPrimary,
            int colorSecondary) {
    }

    /**
     * Dati di un telaio: proprietario (null se non assegnato), stato, energia della cella installata (vuota se
     * nessuna cella) e pezzi montati per slot.
     */
    record FrameItemData(UUID owner, boolean unlocked, boolean reinforced, Optional<Integer> cellEnergy,
            Map<PieceSlot, PieceData> pieces) {
    }
}
