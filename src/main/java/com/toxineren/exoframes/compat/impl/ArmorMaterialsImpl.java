package com.toxineren.exoframes.compat.impl;

import com.toxineren.exoframes.compat.ArmorMaterials;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
//? if >=1.21 {
import com.toxineren.exoframes.RedstoneExoframes;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
//?} else {
/*import net.minecraft.sounds.SoundEvent;*/
//?}

/**
 * Implementazione di {@link ArmorMaterials}: il materiale segnaposto "frame", a difesa zero perche' la protezione
 * reale dei pezzi e' un attributo dinamico (Task 6), non una proprieta' del materiale.
 * <p>
 * Su 1.21.1 il materiale e' una voce del registro vanilla {@code armor_material} con id {@code redstone_exoframes:frame},
 * registrata con il DeferredRegister di Architectury; il {@link RegistrySupplier} restituito e' gia' un
 * {@code Holder<ArmorMaterial>} e si risolve solo all'uso, cosi' si puo' passare al costruttore di {@link ArmorItem}
 * (che legge il materiale pigramente) anche prima che la voce sia legata. Su 1.20.1 il materiale e' un enum privato:
 * il nome e' "frame" senza namespace perche' su Fabric il percorso vanilla della texture non ne accetta uno, e il
 * codice e' lo stesso per Fabric e Forge. Il layer armatura vanilla non disegna nulla per i segnaposto: su 1.21.1 il
 * materiale non ha layer; su 1.20.1 vanilla disegna sempre, con {@code minecraft:textures/models/armor/frame_layer_1.png}
 * e {@code frame_layer_2.png}, che i jar 1.20.1 portano completamente trasparenti (src/main/resources-mc1.20.1). Il
 * rendering dei pezzi passa dal layer della mod.
 * <p>
 * Gli item di {@code newPlaceholder} hanno sempre la pila a 1 su entrambe le versioni, e le proprieta' date possono
 * avere o no una durabilita' vanilla. Su 1.21.1 si imposta {@code stacksTo(1)}; su 1.20.1 le proprieta' passano
 * invariate, perche' li' {@code stacksTo} lancia se e' gia' impostata una durabilita' e il costruttore di
 * {@link ArmorItem} porta comunque la pila a 1.
 */
public final class ArmorMaterialsImpl implements ArmorMaterials {
    //? if >=1.21 {
    private static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(RedstoneExoframes.MOD_ID, Registries.ARMOR_MATERIAL);

    private static final RegistrySupplier<ArmorMaterial> FRAME = MATERIALS.register("frame", ArmorMaterialsImpl::frame);

    /**
     * Il materiale "frame": difesa 0 per ogni tipo, nessuna riparazione, nessun layer. Senza layer il layer armatura
     * vanilla ({@code HumanoidArmorLayer}, che cicla su {@code material.layers()}) non disegna nulla per i segnaposto:
     * la tuta la disegna il layer di rendering della mod.
     */
    private static ArmorMaterial frame() {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            defense.put(type, 0);
        }
        return new ArmorMaterial(defense, 0, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.EMPTY,
                List.of(), 0f, 0f);
    }

    @Override
    public void register() {
        MATERIALS.register();
    }

    @Override
    public Object frameMaterialHandle() {
        return FRAME;
    }

    @Override
    public ArmorItem newPlaceholder(ArmorItem.Type type, Item.Properties props) {
        return new ArmorItem(FRAME, type, props.stacksTo(1));
    }
    //?} else {
    /*@Override
    public void register() {
        // su 1.20.1 i materiali d'armatura non hanno un registro: niente da fare
    }

    @Override
    public Object frameMaterialHandle() {
        return FrameMaterial.FRAME;
    }

    @Override
    public ArmorItem newPlaceholder(ArmorItem.Type type, Item.Properties props) {
        return new ArmorItem(FrameMaterial.FRAME, type, props);
    }

    // Il materiale "frame" su 1.20.1: tutto a zero. Durabilita' 0 vuol dire item non danneggiabile per vanilla,
    // come su 1.21.1 dove il materiale non porta durabilita': l'usura dei pezzi e' gestita dalla mod.
    private enum FrameMaterial implements ArmorMaterial {
        FRAME;

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return 0;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return 0;
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_IRON;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return "frame";
        }

        @Override
        public float getToughness() {
            return 0f;
        }

        @Override
        public float getKnockbackResistance() {
            return 0f;
        }
    }
    *///?}
}
