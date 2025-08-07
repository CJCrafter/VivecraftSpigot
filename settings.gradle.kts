pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/") // FoliaScheduler Snapshots
        maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot API
        maven(url = "https://jitpack.io") // Vault
    }
}

rootProject.name = "VivecraftSpigot"

include("vivecraft-build")
include("vivecraft-core")

file("./vivecraft-platforms/paper").listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
    val subProjectName = subDir.name
    include(":$subProjectName")
    project(":$subProjectName").projectDir = subDir
}
