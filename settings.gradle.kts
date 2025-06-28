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
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/") // Adventure Snapshots
        maven(url = "https://libraries.minecraft.net/") // Brigadier, GameProfile
        maven(url = "https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
        maven(url = "https://jitpack.io") // Vault
        maven(url = "https://maven.enginehub.org/repo/") // WorldGuard
        maven(url = "https://mvn.lumine.io/repository/maven-public/") // MythicMobs
        maven(url = "https://repo.opencollab.dev/main/") // GeyserMC
        maven(url = "https://repo.jeff-media.com/public/") // SpigotUpdateChecker
        maven(url = "https://repo.codemc.org/repository/maven-public/") // NBTAPI from CommandAPI
        maven(url = "https://repo.codemc.io/repository/maven-releases/") // PacketEvents
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
