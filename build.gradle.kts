plugins {
    id("dev.architectury.loom")
}

val mc: String = stonecutter.current.version            // es. "1.20.1"
val variant: String = stonecutter.current.project       // es. "1.20.1-forge"
val loader: String = variant.substringAfterLast('-')    // fabric | forge | neoforge

fun dep(name: String): String = property("deps.$name").toString()

group = property("mod.group").toString()
version = "${property("mod.version")}+mc$mc"
base.archivesName = "redstone-exoframes-$loader"

// Loom registra un repository esclusivo "MavenCentralLWJGL" che obbliga tutto org.lwjgl a venire da
// Maven Central; dal proxy della sessione cloud Central risponde 429 a intermittenza. Lo ripuntiamo al
// Maven di Mojang, che pubblica gli stessi artefatti LWJGL. Non aggiungere un altro exclusiveContent per
// org.lwjgl o com.mojang: due esclusivi sullo stesso gruppo rendono LWJGL irrisolvibile.
repositories.configureEach {
    if (name == "MavenCentralLWJGL" && this is MavenArtifactRepository) {
        setUrl("https://libraries.minecraft.net/")
    }
    // Loom registra anche mavenCentral() ("MavenRepo") prima dei nostri repository: il Maven di NeoForged
    // e' un proxy completo di Maven Central e lo usiamo al suo posto (Central vero resta in coda).
    if (name == "MavenRepo" && this is MavenArtifactRepository) {
        setUrl("https://maven.neoforged.net/releases/")
    }
}

repositories {
    exclusiveContent {
        forRepository { maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") { name = "GeckoLib" } }
        filter {
            includeGroupAndSubgroups("software.bernie")
            includeGroup("com.eliotlash.mclib") // dipendenza di GeckoLib, pubblicata sullo stesso Maven
        }
    }
    exclusiveContent {
        forRepository { maven("https://maven.architectury.dev/") { name = "Architectury" } }
        filter {
            includeGroupAndSubgroups("dev.architectury")
            includeGroupAndSubgroups("me.shedaniel")
        }
    }
    // NON esclusivi: Loom pubblica le dipendenze trasformate di Forge/NeoForge in repository locali con
    // gruppi sintetici (es. net.neoforged.fancymodloader.<sha256>); un exclusiveContent li renderebbe
    // irrisolvibili.
    maven("https://maven.neoforged.net/releases/") {
        name = "NeoForged"
        content {
            includeGroupAndSubgroups("net.neoforged")
            includeGroupAndSubgroups("cpw.mods") // modlauncher 11.x di NeoForge sta qui, non sul Maven di Forge
        }
    }
    maven("https://maven.minecraftforge.net/") {
        name = "MinecraftForge"
        content {
            includeGroupAndSubgroups("net.minecraftforge")
            includeGroupAndSubgroups("de.oceanlabs")
            includeGroupAndSubgroups("cpw.mods")
        }
    }
    maven("https://maven.fabricmc.net/") {
        name = "FabricMC"
        content { includeGroupAndSubgroups("net.fabricmc") }
    }
    // In coda: un altro proxy completo di Central e infine Maven Central vero.
    maven("https://maven.minecraftforge.net/") { name = "ForgeCentralMirror" }
    maven("https://repo.maven.apache.org/maven2/") { name = "MavenCentralFallback" }
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    mappings(loom.officialMojangMappings())

    when (loader) {
        "fabric" -> {
            modImplementation("net.fabricmc:fabric-loader:${dep("fabric_loader")}")
            modImplementation("net.fabricmc.fabric-api:fabric-api:${dep("fabric_api")}")
            modImplementation("dev.architectury:architectury-fabric:${dep("architectury")}")
        }
        "forge" -> {
            "forge"("net.minecraftforge:forge:$mc-${dep("forge")}")
            modImplementation("dev.architectury:architectury-forge:${dep("architectury")}")
        }
        "neoforge" -> {
            "neoForge"("net.neoforged:neoforge:${dep("neoforge")}")
            modImplementation("dev.architectury:architectury-neoforge:${dep("architectury")}")
        }
        else -> error("Loader sconosciuto nella variante $variant: $loader")
    }
    modImplementation("software.bernie.geckolib:geckolib-$loader-$mc:${dep("geckolib")}")
}

val javaRelease: Int = property("mod.java").toString().toInt()

tasks.withType<JavaCompile> {
    options.release = javaRelease
    options.encoding = "UTF-8"
}

java {
    sourceCompatibility = JavaVersion.toVersion(javaRelease)
    targetCompatibility = JavaVersion.toVersion(javaRelease)
    withSourcesJar()
}

tasks.processResources {
    // Ogni jar porta solo i metadati del proprio loader.
    if (loader != "fabric") exclude("fabric.mod.json")
    if (loader != "forge") exclude("META-INF/mods.toml")
    if (loader != "neoforge") exclude("META-INF/neoforge.mods.toml")

    val props = mapOf(
        "version" to project.version.toString(),
        "mc_range" to project.property("mod.mc_range").toString(),
        "loader_range" to project.property("mod.loader_range").toString(),
        "neoforge_range" to (project.findProperty("mod.neoforge_range") ?: "").toString(),
        "pack_format" to project.property("mod.pack_format").toString(),
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(props)
    }
}

tasks.register<Copy>("collectJar") {
    group = "build"
    description = "Copia il jar rimappato di questa variante in build/libs/all della radice."
    from(tasks.named<AbstractArchiveTask>("remapJar").flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/all"))
}
