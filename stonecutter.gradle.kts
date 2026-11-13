plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.17.493" apply false
}

stonecutter active "1.21.1-neoforge"

// Costanti di loader per i commenti condizionali (//? if fabric ...), ricavate dal nome della variante.
stonecutter.parameters {
    val p = node.metadata.project // es. "1.21.1-neoforge"
    constants {
        put("fabric", p.endsWith("-fabric"))
        put("forge", p.endsWith("-forge"))
        put("neoforge", p.endsWith("-neoforge"))
    }
}
