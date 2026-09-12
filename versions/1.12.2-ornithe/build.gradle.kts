plugins {
    idea
    java
    id("net.fabricmc.fabric-loom-remap") version "1.17.+"
    id("ploceus") version "1.17.+"
}

stonecutter {
    constants.match(
        "ornithe",
        "ornithe",
        "forge",
    )
}

val modID = "$id$"
val modName = "$name$"
val mavenGroup = "$group$"
val modVersion = "$version$"
val mcVersion = "1.12.2"
val fabricLoaderVersion = "0.19.3"
val oslVersion = "0.20.3"

group = mavenGroup
version = modVersion
base.archivesName.set("$modName-Ornithe-mc$mcVersion")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

loom {
    mixin {
        defaultRefmapName.set("$modID.mixins.refmap.json")
    }
}

ploceus {
    setIntermediaryGeneration(2)
}

repositories {
    maven("https://repo.codemc.io/repository/maven-public/")
}

val shade: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings(ploceus.mcpMappings("stable", "1.12", "39"))

    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    ploceus.dependOsl(oslVersion)

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
        inputs.property("version", modVersion)
        inputs.property("mcVersion", mcVersion)

        filesMatching(listOf("mcmod.info", "fabric.mod.json", "$modID.mixins.json")) {
            expand(inputs.properties) {
                escapeBackslash = true
            }
        }
    }
}
