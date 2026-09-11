plugins {
    idea
    java
    id("gg.essential.loom") version "1.15.+"
    id("com.gradleup.shadow") version "9.6.+"
}

val modID = "$id$"
val modName = "$name$"
val mavenGroup = "$group$"
val modVersion = "$version$"
val mcVersion = "1.12.2"

group = mavenGroup
version = modVersion
base.archivesName.set("$modName-Forge-mc$mcVersion")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

// Legacy launchwrapper requires a Java 8 URLClassLoader, so force run tasks off the Gradle daemon's JVM.
tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}

loom {
    runConfigs {
        getByName("client") {
            property("mixin.debug.verbose", "true")
            property("mixin.debug.export", "true")
            programArgs("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
        remove(getByName("server"))
    }

    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        //accessTransformer(rootProject.file("src/main/resources/${modID}_at.cfg"))
        mixinConfig("$modID.mixins.json")
    }

    mixin {
        defaultRefmapName.set("$modID.mixins.refmap.json")
    }

    // For some reason loom defaults to tab indentation
    decompilers {
        named("vineflower") {
            options.put("indent-string", "    ")
        }
    }
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

val shade: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.12.2")
    mappings("de.oceanlabs.mcp:mcp_stable:39-1.12")
    forge("net.minecraftforge:forge:1.12.2-14.23.5.2847")

    shade("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }

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

sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        inputs.property("modID", modID)
        inputs.property("modName", modName)
        inputs.property("version", version)
        inputs.property("mcVersion", mcVersion)

        filesMatching(listOf("mcmod.info", "$modID.mixins.json")) {
            expand(inputs.properties) {
                escapeBackslash = true
            }
        }

        //rename("(.+_at.cfg)", "META-INF/$1")
    }

    shadowJar {
        archiveClassifier.set("dev")
        configurations = listOf(shade)
        exclude("META-INF/maven/**")
    }

    jar {
        dependsOn(shadowJar)
        duplicatesStrategy = DuplicatesStrategy.FAIL

        manifest.attributes(mapOf(
            "ModSide" to "CLIENT",
            //"FMLAT" to "${modID}_at.cfg",
            "FMLCorePluginContainsFMLMod" to true,
            "ForceLoadAsMod" to true,
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
        ))
    }

    remapJar {
        inputFile.set(shadowJar.get().archiveFile)
        archiveClassifier.set("")
    }
}
