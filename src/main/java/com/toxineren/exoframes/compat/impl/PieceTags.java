package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.ItemDataAccess.PieceData;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.armor.Appearance;
import com.toxineren.exoframes.core.armor.Decal;
import com.toxineren.exoframes.core.armor.Tier;
import com.toxineren.exoframes.core.module.ModuleType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Lettore condiviso dei tag di pezzo e di cella, e scrittore condiviso dell'aspetto dei pezzi, usato dai dati degli
 * item ({@link ItemDataAccessImpl}) e dallo stato del telaio ({@link FrameStateCodec}): le regole sugli intervalli
 * stanno solo qui, cosi' i due codec leggono pezzi ed energia e leggono e scrivono l'aspetto allo stesso modo.
 * Nessuna condizione di versione.
 * <p>
 * Garanzie del pezzo letto: {@code maxDurability > 0} (altrimenti nessun pezzo, con un avviso), {@code durability} in
 * [0, maxDurability], moduli noti senza duplicati nell'ordine di arrivo e al massimo
 * {@code ModuleType.values().length}, colori {@link Appearance#UNPAINTED} se la chiave manca (o non e' numerica)
 * e altrimenti ridotti a 24 bit, decalcomanie valide nell'ordine di arrivo e al massimo
 * {@link Appearance#MAX_DECALS} (le voci malformate si saltano). L'energia letta e' sempre {@code >= 0}; il limite
 * superiore (la capacita' della cella) resta al chiamante.
 */
final class PieceTags {
    private PieceTags() {
    }

    /**
     * Legge un pezzo da un tag (il tag radice di un pezzo o una voce di {@link NbtKeys#PIECES}); vuoto se il livello
     * manca o e' sconosciuto o se la durabilita' massima non e' positiva. Le chiavi mancanti o di tipo sbagliato
     * valgono 0 o lista vuota; l'aspetto si legge con {@link #readAppearance(CompoundTag)}.
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
        return Optional.of(new PieceData(tier, durability, max, readModules(tag), readAppearance(tag)));
    }

    /**
     * L'aspetto salvato nel tag di un pezzo: un colore assente (o non numerico) vale {@link Appearance#UNPAINTED},
     * uno presente si riduce a 24 bit; delle decalcomanie si tengono le prime {@link Appearance#MAX_DECALS} valide.
     */
    static Appearance readAppearance(CompoundTag tag) {
        return new Appearance(readColor(tag, NbtKeys.COLOR_PRIMARY), readColor(tag, NbtKeys.COLOR_SECONDARY),
                readDecals(tag));
    }

    /**
     * Scrive l'aspetto nel tag di un pezzo, sovrascrivendo quello eventualmente presente: un colore
     * {@link Appearance#UNPAINTED} e una lista di decalcomanie vuota tolgono la chiave.
     */
    static void writeAppearance(CompoundTag tag, Appearance appearance) {
        writeColor(tag, NbtKeys.COLOR_PRIMARY, appearance.primary());
        writeColor(tag, NbtKeys.COLOR_SECONDARY, appearance.secondary());
        if (appearance.decals().isEmpty()) {
            tag.remove(NbtKeys.DECALS);
            return;
        }
        ListTag decals = new ListTag();
        for (Decal decal : appearance.decals()) {
            CompoundTag entry = new CompoundTag();
            entry.put(NbtKeys.PATTERN, StringTag.valueOf(decal.pattern()));
            entry.put(NbtKeys.COLOR, StringTag.valueOf(decal.color()));
            decals.add(entry);
        }
        tag.put(NbtKeys.DECALS, decals);
    }

    /** L'energia di una cella ({@link NbtKeys#ENERGY} nel tag dato), mai negativa; 0 se assente. */
    static int readEnergy(CompoundTag tag) {
        return Math.max(0, tag.getInt(NbtKeys.ENERGY));
    }

    private static int readColor(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) & 0xFFFFFF : Appearance.UNPAINTED;
    }

    private static void writeColor(CompoundTag tag, String key, int color) {
        if (color == Appearance.UNPAINTED) {
            tag.remove(key);
        } else {
            tag.putInt(key, color);
        }
    }

    /**
     * Le decalcomanie valide della lista, nell'ordine di arrivo, al massimo {@link Appearance#MAX_DECALS}; una voce
     * senza motivo o colore di tipo string, o con valori che {@link Decal} rifiuta, si salta.
     */
    private static List<Decal> readDecals(CompoundTag tag) {
        List<Decal> decals = new ArrayList<>();
        ListTag list = tag.getList(NbtKeys.DECALS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && decals.size() < Appearance.MAX_DECALS; i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.contains(NbtKeys.PATTERN, Tag.TAG_STRING) || !entry.contains(NbtKeys.COLOR, Tag.TAG_STRING)) {
                continue;
            }
            try {
                decals.add(new Decal(entry.getString(NbtKeys.PATTERN), entry.getString(NbtKeys.COLOR)));
            } catch (IllegalArgumentException e) {
                // stringa vuota o troppo lunga: la regola sta solo in Decal, la voce si salta
            }
        }
        return decals;
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
