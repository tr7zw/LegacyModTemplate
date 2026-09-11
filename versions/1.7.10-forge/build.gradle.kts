import com.gtnewhorizons.retrofuturagradle.modutils.ModUtils

buildscript {
    repositories {
        maven {
            name = "forge"
            url = uri("https://maven.minecraftforge.net")
            mavenContent {
                includeGroup("net.minecraftforge")
                includeGroup("net.minecraftforge.srg2source")
                includeGroup("de.oceanlabs.mcp")
                includeGroup("cpw.mods")
            }
        }
        maven {
            // Srg2Source needs an eclipse dependency.
            name = "eclipse"
            url = uri("https://repo.eclipse.org/content/groups/eclipse/")
            mavenContent { includeGroup("org.eclipse.jdt") }
        }
        maven {
            name = "mojang"
            url = uri("https://libraries.minecraft.net/")
            mavenContent {
                includeGroup("com.ibm.icu")
                includeGroup("com.mojang")
                includeGroup("com.paulscode")
                includeGroup("org.lwjgl.lwjgl")
                includeGroup("tv.twitch")
                includeGroup("net.minecraft")
            }
        }
        maven {
            name = "fabric"
            url = uri("https://maven.fabricmc.net/")
            mavenContent { includeGroup("net.fabricmc") }
        }
        mavenCentral()
    }
}

plugins {
    idea
    java
    id("com.gtnewhorizons.retrofuturagradle") version "2.0.4"
}

val modID = "$id$"
val modName = "$name$"
val mavenGroup = "$group$"
val modVersion = "$version$"
val forgeMcVersion = "1.7.10"

group = mavenGroup
version = modVersion
base.archivesName.set("$modName-Forge-mc$forgeMcVersion")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

minecraft {
    mcVersion.set(forgeMcVersion)
    extraTweakClasses.add("org.spongepowered.asm.launch.MixinTweaker")
}

repositories {
    mavenCentral()
    maven("https://nexus.gtnewhorizons.com/repository/public/")
    maven {
        name = "tr7zw-proxy"
        url = uri("https://maven.tr7zw.dev/repository/maven-public/")
    }
}

val mixinProviderSpec = "io.github.legacymoddingmc:unimixins:0.3.1"
val modUtils = extensions.getByType<ModUtils>()

val shade: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    annotationProcessor("org.ow2.asm:asm-debug-all:5.0.4")
    annotationProcessor("com.google.guava:guava:24.1.1-jre")
    annotationProcessor("com.google.code.gson:gson:2.13.2")
    annotationProcessor("$mixinProviderSpec:dev")

    implementation(modUtils.enableMixins("$mixinProviderSpec:dev", "$modID.mixins.refmap.json"))

    applyExternalDependencies(project.projectDir)
    applyExternalDependencies(project.rootProject.rootDir)
}

fun applyExternalDependencies(projectDir: File) {
    val gradleFile = File(projectDir, "dependencies.gradle")
    if (gradleFile.exists()) {
        project.apply(mapOf("from" to gradleFile.absolutePath))
        return
    }

    val gradleKtsFile = File(projectDir, "dependencies.gradle.kts")
    if (gradleKtsFile.exists()) {
        project.apply(mapOf("from" to gradleKtsFile.absolutePath))
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("modID", modID)
    inputs.property("modName", modName)
    inputs.property("version", version)
    inputs.property("mcVersion", forgeMcVersion)

    filesMatching(listOf("mcmod.info", "$modID.mixins.json")) {
        expand(inputs.properties) {
            escapeBackslash = true
        }
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(shade.map { zipTree(it) })

    manifest.attributes(mapOf(
        "FMLCorePluginContainsFMLMod" to true,
        "ForceLoadAsMod" to true,
        "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
        "MixinConfigs" to "$modID.mixins.json",
    ))
}
