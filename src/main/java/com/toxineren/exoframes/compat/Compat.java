package com.toxineren.exoframes.compat;

import com.toxineren.exoframes.compat.impl.ArmorMaterialsImpl;
import com.toxineren.exoframes.compat.impl.ItemDataAccessImpl;
import com.toxineren.exoframes.compat.impl.PlatformImpl;

/**
 * Punto di accesso unico alle interfacce di compat/: ogni accessor restituisce il singleton dell'unica
 * implementazione in compat/impl, creato alla prima chiamata (idioma del holder, sicuro tra thread).
 */
public final class Compat {
    private Compat() {
    }

    public static Platform platform() {
        return PlatformHolder.INSTANCE;
    }

    public static ItemDataAccess itemData() {
        return ItemDataHolder.INSTANCE;
    }

    public static ArmorMaterials armorMaterials() {
        return ArmorMaterialsHolder.INSTANCE;
    }

    private static final class PlatformHolder {
        static final Platform INSTANCE = new PlatformImpl();
    }

    private static final class ItemDataHolder {
        static final ItemDataAccess INSTANCE = new ItemDataAccessImpl();
    }

    private static final class ArmorMaterialsHolder {
        static final ArmorMaterials INSTANCE = new ArmorMaterialsImpl();
    }
}
