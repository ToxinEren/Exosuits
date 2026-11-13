package com.toxineren.exoframes.compat.impl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.SuitBones;
import com.toxineren.exoframes.compat.client.SuitRenderer;
import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.compat.client.SuitView.PieceView;
import com.toxineren.exoframes.compat.impl.SuitModelItem;
import com.toxineren.exoframes.content.ModItems;
import com.toxineren.exoframes.core.armor.PieceSlot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
//? if >=1.21 {
import software.bernie.geckolib.renderer.specialty.DyeableGeoArmorRenderer;
import software.bernie.geckolib.util.Color;
//?} else {
/*import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.DyeableGeoArmorRenderer;
*///?}

/**
 * Implementazione di {@link SuitRenderer}: un {@code DyeableGeoArmorRenderer} di GeckoLib guidato a mano con uno
 * stack fittizio dell'item modello ({@link ModItems#SUIT_MODEL}) nello slot del torso, come fa GeckoLib dentro il
 * layer armatura vanilla (ricognizione M3, §3). Il renderer copia dal modello umanoide di base la posa delle sei
 * ossa di primo livello; la visibilita' e i colori delle ossa li decide la {@link SuitView} del disegno in corso.
 * <p>
 * Comportamento: una sola passata con una sola texture. Le ossa di primo livello e del telaio sono sempre visibili;
 * per ogni slot con un pezzo si mostrano solo le tre ossa dello stato dato e si nascondono le altre, per uno slot
 * senza pezzo si nascondono tutte; {@code _p1} si colora con il colore primario, {@code _p2} con il secondario,
 * {@code _n} resta bianca; le ossa delle decalcomanie restano nascoste. Un osso che la geometria non ha si salta, e il
 * suo nome va nel log una volta sola per sessione.
 * <p>
 * Differenze chiuse: il package di {@code DyeableGeoArmorRenderer} e di {@code Color} ({@code renderer} e
 * {@code core.object} in GeckoLib 4.8, {@code renderer.specialty} e {@code util} in 4.9); {@code prepForRender} a 4
 * argomenti in 4.8 e a 10 in 4.9 (dove quella a 4 e' deprecata). I colori passano da {@code getColorForBone}, che ha
 * la stessa firma nelle due versioni, quindi i ganci con il colore a quattro float o intero non si toccano.
 */
public final class SuitRendererImpl extends DyeableGeoArmorRenderer<SuitModelItem> implements SuitRenderer {
    private static final SuitView BARE = new SuitView(Map.of());

    /** Nomi delle ossa dei pezzi per [slot][stato][regione], nell'ordine di PieceSlot e di SuitBones.REGIONS. */
    private static final String[][][] PIECE_BONES = pieceBones();

    /** Ossa sempre visibili: le sei di primo livello e le sei del telaio. */
    private static final List<String> ALWAYS_VISIBLE = alwaysVisible();

    /** Ossa colorabili (regioni primaria e secondaria di ogni stato) con lo slot e la regione di cui prendono il colore. */
    private static final Map<String, Dye> DYEABLE = dyeableBones();

    private final ItemStack stack;
    private final Set<String> reportedMissing = new HashSet<>();
    private SuitView view = BARE;

    public SuitRendererImpl() {
        super(new SuitModel());
        this.stack = new ItemStack(ModItems.SUIT_MODEL.get());
    }

    @Override
    public void render(LivingEntity entity, HumanoidModel<?> base, PoseStack pose, MultiBufferSource buffers,
            int packedLight, float partialTick, SuitView view) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(base, "base model must not be null");
        this.view = Objects.requireNonNull(view, "view must not be null");
        try {
            // Va ripreparato prima di ogni disegno: GeckoLib azzera questi campi dopo il disegno (non su Forge 4.8).
            //? if >=1.21 {
            prepForRender(entity, stack, EquipmentSlot.CHEST, base, buffers, partialTick, 0, 0, 0, 0);
            //?} else {
            /*prepForRender(entity, stack, EquipmentSlot.CHEST, base);
            *///?}
            defaultRender(pose, getAnimatable(), buffers, null, null, 0f, partialTick, packedLight);
        } finally {
            this.view = BARE;
            // Su Forge 1.20.1 GeckoLib non azzera i campi dopo il disegno: senza questo il renderer terrebbe
            // l'ultima entita' disegnata (e con lei il livello) anche dopo l'uscita dal mondo.
            this.baseModel = null;
            this.currentEntity = null;
            this.currentStack = null;
            this.animatable = null;
            this.currentSlot = null;
        }
    }

    /**
     * Visibilita' delle ossa dalla vista del disegno in corso. GeckoLib chiama questo metodo in {@code preRender} per
     * ogni entita' che non e' un animatable GeckoLib (giocatori e umanoidi vanilla) al posto della visibilita' per
     * slot, che leggerebbe i flag {@code visible} del modello di base: qui non si leggono.
     */
    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        List<String> missing = new ArrayList<>(0);
        for (String name : ALWAYS_VISIBLE) {
            setVisible(name, true, missing);
        }
        PieceSlot[] slots = PieceSlot.values();
        for (int s = 0; s < slots.length; s++) {
            PieceView piece = view.pieces().get(slots[s]);
            for (int state = 0; state < SuitBones.STATES; state++) {
                boolean visible = piece != null && piece.state() == state;
                for (String name : PIECE_BONES[s][state]) {
                    setVisible(name, visible, missing);
                }
            }
        }
        for (PieceSlot slot : SuitBones.DECAL_SLOTS) {
            setVisible(SuitBones.decal(slot), false, missing);
        }
        if (!missing.isEmpty()) {
            RedstoneExoframes.LOGGER.warn("Suit geometry has no bones named {}: skipped (logged once per session)",
                    missing);
        }
    }

    @Override
    protected boolean isBoneDyeable(GeoBone bone) {
        return DYEABLE.containsKey(bone.getName());
    }

    @Override
    protected Color getColorForBone(GeoBone bone) {
        Dye dye = DYEABLE.get(bone.getName());
        PieceView piece = dye == null ? null : view.pieces().get(dye.slot());
        if (piece == null) {
            return Color.WHITE; // osso nascosto: il colore non si vede
        }
        return Color.ofOpaque(dye.primary() ? piece.primaryRgb() : piece.secondaryRgb());
    }

    /** Mostra o nasconde l'osso con i suoi figli; un osso assente si salta e il nome si annota la prima volta. */
    private void setVisible(String name, boolean visible, List<String> missing) {
        Optional<GeoBone> bone = getGeoModel().getBone(name);
        if (bone.isPresent()) {
            bone.get().setHidden(!visible);
        } else if (reportedMissing.add(name)) {
            missing.add(name);
        }
    }

    private static String[][][] pieceBones() {
        PieceSlot[] slots = PieceSlot.values();
        String[][][] names = new String[slots.length][SuitBones.STATES][SuitBones.REGIONS.size()];
        for (int s = 0; s < slots.length; s++) {
            for (int state = 0; state < SuitBones.STATES; state++) {
                for (int r = 0; r < SuitBones.REGIONS.size(); r++) {
                    names[s][state][r] = SuitBones.piece(slots[s], state, SuitBones.REGIONS.get(r));
                }
            }
        }
        return names;
    }

    private static List<String> alwaysVisible() {
        List<String> names = new ArrayList<>(SuitBones.TOP_LEVEL);
        for (String part : SuitBones.PARTS) {
            names.add(SuitBones.frame(part));
        }
        return List.copyOf(names);
    }

    private static Map<String, Dye> dyeableBones() {
        Map<String, Dye> dyeable = new HashMap<>();
        for (PieceSlot slot : PieceSlot.values()) {
            for (int state = 0; state < SuitBones.STATES; state++) {
                dyeable.put(SuitBones.piece(slot, state, SuitBones.PRIMARY), new Dye(slot, true));
                dyeable.put(SuitBones.piece(slot, state, SuitBones.SECONDARY), new Dye(slot, false));
            }
        }
        return Map.copyOf(dyeable);
    }

    /** Lo slot di un osso colorabile e se prende il colore primario (altrimenti il secondario). */
    private record Dye(PieceSlot slot, boolean primary) {
    }
}
