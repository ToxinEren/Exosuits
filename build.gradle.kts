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

// Test in gioco senza client: :<variante>:runGameTestServer avvia il server GameTest vanilla nella cartella
// versions/<variante>/run-gametest (separata da run/, il mondo di runServer). Il server esce con il numero di test
// obbligatori falliti, quindi un test fallito fa fallire il task. Un'uscita 0 da sola non prova che i test siano
// girati (Forge e NeoForge escono con 0 anche senza test): tools/run_gametests.sh controlla la riga
// "All N required tests passed" con N > 0.
val gameTestRunDir = "run-gametest"
loom {
    runs {
        create("gameTestServer") {
            server()
            name("GameTest Server")
            runDir(gameTestRunDir)
            ideConfigGenerated(false)
            val modId = project.property("mod.id").toString()
            when (loader) {
                "fabric" -> {
                    // Basta la presenza della proprieta': il modulo fabric-gametest sostituisce il server dedicato.
                    property("fabric-api.gametest")
                    property("fabric-api.gametest.report-file", project.layout.buildDirectory.file("gametest/report.xml").get().asFile.absolutePath)
                }
                "forge", "neoforge" -> {
                    // server() impone il launch target del server normale e il template non porta le sue proprieta':
                    // senza gameTestServer=true parte un server dedicato qualunque (Forge senza eula esce con 0 e zero
                    // test, NeoForge resta acceso).
                    forgeTemplate("gameTestServer")
                    property("$loader.enabledGameTestNamespaces", modId)
                    property("$loader.enableGameTest", "true")
                    property("$loader.gameTestServer", "true")
                }
            }
        }
    }
}

// Mixin (solo client, vedi docs/compat.md "Mixin client"). Fabric e NeoForge leggono la configurazione dai metadati
// (fabric.mod.json, neoforge.mods.toml); Forge 1.20.1 dall'attributo MixinConfigs del manifest, che Loom scrive nel jar
// da forge.mixinConfigs e passa anche alle esecuzioni di sviluppo. Nessun refmap: con questa Loom il processore di
// annotazioni dei mixin e' spento (useLegacyMixinAp = false), e remapJar riscrive direttamente i nomi nelle
// annotazioni dei mixin (estensione mixin di tiny-remapper) verso intermediary su Fabric e SRG su Forge.
if (loader == "forge") {
    loom.forge.mixinConfig("${project.property("mod.id")}.mixins.json")
}

// Mondo nuovo a ogni esecuzione: su Forge 1.20.1 il server GameTest usa il mondo normale della cartella, che
// altrimenti resta tra un'esecuzione e l'altra.
val cleanGameTestWorld = tasks.register<Delete>("cleanGameTestWorld") {
    description = "Cancella il mondo del server GameTest."
    delete(layout.projectDirectory.dir("$gameTestRunDir/world"))
}
tasks.matching { it.name == "runGameTestServer" }.configureEach { dependsOn(cleanGameTestWorld) }

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
            // Solo per le esecuzioni di sviluppo (runClient): GeckoLib 4.8.4 per Forge porta mclib dentro il proprio
            // jar e Loom non lo mette nel classpath di sviluppo, quindi il client crolla al caricamento delle risorse
            // (NoClassDefFoundError com/eliotlash/mclib/math/IValue). Non finisce nel jar della mod; in produzione
            // mclib arriva dal jar di GeckoLib, e il server di sviluppo non carica i modelli e non ne ha bisogno.
            "forgeRuntimeLibrary"("com.eliotlash.mclib:mclib:20")
        }
        "neoforge" -> {
            "neoForge"("net.neoforged:neoforge:${dep("neoforge")}")
            modImplementation("dev.architectury:architectury-neoforge:${dep("architectury")}")
        }
        else -> error("Loader sconosciuto nella variante $variant: $loader")
    }
    modImplementation("software.bernie.geckolib:geckolib-$loader-$mc:${dep("geckolib")}")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
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

// Risorse per versione di Minecraft: src/main/resources-mc<versione> (es. resources-mc1.20.1) si aggiunge alle risorse
// comuni delle varianti di quella versione. La cartella puo' mancare: Gradle ignora una srcDir inesistente. Oggi porta
// le texture trasparenti del layer armatura vanilla di 1.20.1 per il materiale "frame".
sourceSets.main {
    resources.srcDir(rootProject.file("src/main/resources-mc$mc"))
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
        // Versioni minime dichiarate nei metadati: quelle con cui il mod e' compilato e provato.
        "architectury_version" to project.property("deps.architectury").toString(),
        "geckolib_version" to project.property("deps.geckolib").toString(),
        "fabric_api_version" to (project.findProperty("deps.fabric_api") ?: "").toString().substringBefore('+'),
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
