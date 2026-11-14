package com.toxineren.exoframes.compat.impl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.client.SuitBoneRules;
import com.toxineren.exoframes.compat.client.SuitRenderer;
import com.toxineren.exoframes.compat.client.SuitView;
import com.toxineren.exoframes.compat.impl.SuitModelItem;
import com.toxineren.exoframes.content.ModItems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
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
 * Comportamento: una sola passata con una sola texture. Visibilita' e colori delle ossa li decide
 * {@link SuitBoneRules}, qui solo applicati: ossa di primo livello e del telaio sempre visibili, del pezzo di ogni
 * slot solo le tre dello stato dato, decalcomanie nascoste; {@code _p1} e {@code _p2} con i colori del pezzo,
 * {@code _n} bianca. Un osso che la geometria non ha si salta, e il suo nome va nel log una volta sola per sessione.
 * <p>
 * Differenze chiuse: il package di {@code DyeableGeoArmorRenderer} e di {@code Color} ({@code renderer} e
 * {@code core.object} in GeckoLib 4.8, {@code renderer.specialty} e {@code util} in 4.9); {@code prepForRender} a 4
 * argomenti in 4.8 e a 10 in 4.9 (dove quella a 4 e' deprecata). I colori passano da {@code getColorForBone}, che ha
 * la stessa firma nelle due versioni, quindi i ganci con il colore a quattro float o intero non si toccano.
 */
public final class SuitRendererImpl extends DyeableGeoArmorRenderer<SuitModelItem> implements SuitRenderer {
    private static final SuitView BARE = new SuitView(Map.of());

    private final ItemStack stack;
    private final Set<String> reportedMissing = new HashSet<>();
    private SuitView view = BARE;

    public SuitRendererImpl() {
        super(new SuitModel());
        this.stack = new ItemStack(ModItems.SUIT_MODEL.get());
    }

    @Override
    public void render(Entity entity, HumanoidModel<?> base, PoseStack pose, MultiBufferSource buffers,
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
     * ogni entita' che non e' un animatable GeckoLib (giocatori, umanoidi vanilla, il telaio piazzato) al posto della
     * visibilita' per slot, che leggerebbe i flag {@code visible} del modello di base: qui non si leggono.
     */
    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        List<String> missing = new ArrayList<>(0);
        for (String name : SuitBoneRules.BONES) {
            setVisible(name, SuitBoneRules.visible(name, view), missing);
        }
        if (!missing.isEmpty()) {
            RedstoneExoframes.LOGGER.warn("Suit geometry has no bones named {}: skipped (logged once per session)",
                    missing);
        }
    }

    @Override
    protected boolean isBoneDyeable(GeoBone bone) {
        return SuitBoneRules.region(bone.getName()) != SuitBoneRules.Region.NEUTRAL;
    }

    @Override
    protected Color getColorForBone(GeoBone bone) {
        // un osso di uno slot senza pezzo e' nascosto: il bianco che riceve non si vede
        return Color.ofOpaque(SuitBoneRules.rgb(bone.getName(), view));
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
}
