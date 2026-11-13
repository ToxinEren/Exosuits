package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.compat.SuitSummary;
import com.toxineren.exoframes.compat.SuitSummary.PieceLook;
import com.toxineren.exoframes.core.armor.PieceCondition;
import com.toxineren.exoframes.core.armor.PieceSlot;
import com.toxineren.exoframes.core.armor.Tier;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Lettura e scrittura del riassunto visivo ({@link SuitSummary}) sotto la chiave {@link NbtKeys#VISUAL} del tag
 * radice di uno stack: {@code visual: {pieces: {<slot>: {tier, condition, color_primary?, color_secondary?,
 * decals?}}}}. Gli id di slot e livello passano da {@link SchemaIds}, l'aspetto da {@link PieceTags}. Nessuna
 * condizione di versione.
 * <p>
 * La lettura non lancia mai (i dati degli item non sono fidati): si guardano solo le voci dei sei slot noti, quindi
 * le chiavi sconosciute o in eccesso non costano nulla; una voce non compound, senza livello di tipo string o con un
 * livello sconosciuto si salta; una condizione assente, non string o sconosciuta vale {@link PieceCondition#INTACT}.
 */
final class SuitSummaryTags {
    private SuitSummaryTags() {
    }

    /**
     * Il riassunto nel tag radice; vuoto se {@link NbtKeys#VISUAL} manca o non e' un compound. Un {@code visual}
     * senza {@link NbtKeys#PIECES} compound vale il telaio nudo.
     */
    static Optional<SuitSummary> read(CompoundTag root) {
        if (!root.contains(NbtKeys.VISUAL, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag pieces = root.getCompound(NbtKeys.VISUAL).getCompound(NbtKeys.PIECES);
        Map<PieceSlot, PieceLook> looks = new EnumMap<>(PieceSlot.class);
        for (PieceSlot slot : PieceSlot.values()) {
            String key = SchemaIds.slotId(slot);
            if (!pieces.contains(key, Tag.TAG_COMPOUND)) {
                continue;
            }
            CompoundTag entry = pieces.getCompound(key);
            if (!entry.contains(NbtKeys.TIER, Tag.TAG_STRING)) {
                continue;
            }
            Tier tier = SchemaIds.tierById(entry.getString(NbtKeys.TIER));
            if (tier != null) {
                looks.put(slot, new PieceLook(tier, readCondition(entry), PieceTags.readAppearance(entry)));
            }
        }
        return Optional.of(new SuitSummary(looks));
    }

    /** Scrive il riassunto nel tag radice, sostituendo quello presente; le altre chiavi della radice restano. */
    static void write(CompoundTag root, SuitSummary summary) {
        CompoundTag pieces = new CompoundTag();
        for (Map.Entry<PieceSlot, PieceLook> entry : summary.pieces().entrySet()) {
            PieceLook look = entry.getValue();
            CompoundTag tag = new CompoundTag();
            tag.putString(NbtKeys.TIER, look.tier().id());
            tag.putString(NbtKeys.CONDITION, conditionId(look.condition()));
            PieceTags.writeAppearance(tag, look.appearance());
            pieces.put(SchemaIds.slotId(entry.getKey()), tag);
        }
        CompoundTag visual = new CompoundTag();
        visual.put(NbtKeys.PIECES, pieces);
        root.put(NbtKeys.VISUAL, visual);
    }

    /** L'id di una condizione nello schema: il nome dell'enum in minuscolo. */
    private static String conditionId(PieceCondition condition) {
        return condition.name().toLowerCase(Locale.ROOT);
    }

    /** La condizione della voce; {@link PieceCondition#INTACT} se assente, non string o sconosciuta (con avviso). */
    private static PieceCondition readCondition(CompoundTag entry) {
        if (!entry.contains(NbtKeys.CONDITION, Tag.TAG_STRING)) {
            return PieceCondition.INTACT;
        }
        String id = entry.getString(NbtKeys.CONDITION);
        for (PieceCondition condition : PieceCondition.values()) {
            if (conditionId(condition).equals(id)) {
                return condition;
            }
        }
        SchemaIds.warnUnknown("piece condition", id);
        return PieceCondition.INTACT;
    }
}
