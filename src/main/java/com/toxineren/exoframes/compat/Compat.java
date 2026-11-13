package com.toxineren.exoframes.compat;

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

    private static final class PlatformHolder {
        static final Platform INSTANCE = new PlatformImpl();
    }
}
