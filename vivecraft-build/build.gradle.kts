plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.2.0"
}

dependencies {
    implementation(project(":vivecraft-core"))

    file("../vivecraft-platforms/paper").listFiles()?.forEach {
        implementation(project(":${it.name}", "reobf"))
    }
}

bukkitPluginYaml {
    val versionProperty = findProperty("version") as? String ?: throw IllegalArgumentException("version was null")

    main = "com.cjcrafter.vivecraft.VSE"
    name = "VivecraftSpigot"
    version = versionProperty
    apiVersion = "1.13"
    prefix = "Vivecraft"
    foliaSupported = true

    authors = listOf("jrbudda", "jaron780", "CJCrafter")
    softDepend = listOf("Vault")

    commands {
        register("Vive") {
            description = "Vivecraft Spigot Extensions"
            usage = "/vive <command>"
            aliases = listOf("vse")
        }
    }
}

tasks.shadowJar {
    val versionProperty = findProperty("version") as? String ?: throw IllegalArgumentException("version was null")
    archiveFileName.set("VivecraftSpigot-$versionProperty.jar")

    val libPackage = "com.cjcrafter.vivecraft.lib"

    relocate("org.bstats", "$libPackage.bstats")
    relocate("com.cryptomorin.xseries", "$libPackage.xseries")
    relocate("com.cjcrafter.foliascheduler", "$libPackage.foliascheduler")
}
