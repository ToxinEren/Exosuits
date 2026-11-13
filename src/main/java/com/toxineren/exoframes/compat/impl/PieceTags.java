package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.ItemDataAccess.PieceData;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Lettore condiviso dei tag di pezzo e di cella, usato dai dati degli item ({@link ItemDataAccessImpl}) e dallo stato
 * del telaio ({@link FrameStateCodec}): le regole sugli intervalli stanno solo qui, cosi' i due codec leggono pezzi
 * ed energia allo stesso modo. Nessuna condizione di versione.
 * <p>
 * Garanzie del pezzo letto: {@code maxDurability > 0} (altrimenti nessun pezzo, con un avviso), {@code durability} in
 * [0, maxDurability], moduli noti senza duplicati nell'ordine di arrivo e al massimo
 * {@code ModuleType.values().length}, colori in [0, 0xFFFFFF]. L'energia letta e' sempre {@code >= 0}; il limite
 * superiore (la capacita' della cella) resta al chiamante.
 */
final class PieceTags {
    private PieceTags() {
    }

    /**
     * Legge un pezzo da un tag (il tag radice di un pezzo o una voce di {@link NbtKeys#PIECES}); vuoto se il livello
     * manca o e' sconosciuto o se la durabilita' massima non e' positiva. Le chiavi mancanti o di tipo sbagliato
     * valgono 0 o lista vuota.
     */
    static Optional<PieceData> readPiece(CompoundTag tag) {
        if (!tag.contains(NbtKeys.TIER, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        Tier tier = SchemaIds.tierById(tag.getString(NbtKeys.TIER));
        if (tier == null) {
            return Optional.empty();
        }
        int max = tag.getInt(NbtKeys.MAX_DURABILITY);
        if (max <= 0) {
            SchemaIds.warnOnce("piece max durability:" + max,
                    "Dropping mod piece with invalid max durability {}", max);
            return Optional.empty();
        }
        int durability = Math.max(0, Math.min(max, tag.getInt(NbtKeys.DURABILITY)));
        return Optional.of(new PieceData(tier, durability, max, readModules(tag),
                tag.getInt(NbtKeys.COLOR_PRIMARY) & 0xFFFFFF, tag.getInt(NbtKeys.COLOR_SECONDARY) & 0xFFFFFF));
    }

    /** L'energia di una cella ({@link NbtKeys#ENERGY} nel tag dato), mai negativa; 0 se assente. */
    static int readEnergy(CompoundTag tag) {
        return Math.max(0, tag.getInt(NbtKeys.ENERGY));
    }

    /** I moduli noti della lista, senza duplicati, nell'ordine di arrivo, al massimo uno per tipo di modulo. */
    private static List<ModuleType> readModules(CompoundTag tag) {
        int limit = ModuleType.values().length;
        Set<ModuleType> modules = new LinkedHashSet<>();
        ListTag list = tag.getList(NbtKeys.MODULES, Tag.TAG_STRING);
        for (int i = 0; i < list.size() && modules.size() < limit; i++) {
            ModuleType module = SchemaIds.moduleById(list.getString(i));
            if (module != null) {
                modules.add(module);
            }
        }
        return List.copyOf(modules);
    }
}
