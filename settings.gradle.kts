pluginManagement {
    repositories {
        // Ogni Maven di modding serve solo i propri gruppi: niente richieste inutili a Maven Central,
        // che dal proxy della sessione cloud risponde spesso 429 e fa fallire la risoluzione.
        exclusiveContent {
            forRepository { maven("https://maven.kikugie.dev/releases") { name = "KikuGie" } }
            filter { includeGroupAndSubgroups("dev.kikugie") }
        }
        exclusiveContent {
            forRepository { maven("https://maven.architectury.dev/") { name = "Architectury" } }
            filter {
                includeGroupAndSubgroups("dev.architectury")
                includeGroup("architectury-plugin")
                includeGroupAndSubgroups("me.shedaniel")
            }
        }
        maven("https://maven.fabricmc.net/") {
            name = "FabricMC"
            content {
                includeGroupAndSubgroups("net.fabricmc")
                includeGroupAndSubgroups("com.mojang")
            }
        }
        maven("https://libraries.minecraft.net/") {
            name = "Mojang"
            content { includeGroupAndSubgroups("com.mojang") }
        }
        maven("https://maven.neoforged.net/releases/") {
            name = "NeoForged"
            content { includeGroupAndSubgroups("net.neoforged") }
        }
        maven("https://maven.minecraftforge.net/") {
            name = "MinecraftForge"
            content {
                includeGroupAndSubgroups("net.minecraftforge")
                includeGroupAndSubgroups("de.oceanlabs")
                includeGroupAndSubgroups("cpw.mods")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
}

stonecutter {
    create(rootProject) {
        version("1.20.1-fabric", "1.20.1")
        version("1.20.1-forge", "1.20.1")
        version("1.21.1-fabric", "1.21.1")
        version("1.21.1-neoforge", "1.21.1")
        vcsVersion = "1.21.1-neoforge"
    }
}

rootProject.name = "redstone-exoframes"
