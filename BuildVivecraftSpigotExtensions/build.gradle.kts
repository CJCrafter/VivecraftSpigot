import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask

group = "com.cjcrafter"
version = "3.1.0"

plugins {
    `java-library`
    id("com.github.breadmoirai.github-release") version "2.4.1"
    id("com.gradleup.shadow") version "8.3.3"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

// See https://github.com/Minecrell/plugin-yml
bukkit {
    name = "VivecraftSpigot"
    main = "com.cjcrafter.vivecraft.VSE"
    apiVersion = "1.13"
    authors = listOf("jrbudda", "jaron780", "CJCrafter")
    prefix = "Vivecraft"
    softDepend = listOf("Vault")

    foliaSupported = true

    commands {
        register("Vive") {
            description = "Vivecraft Spigot Extensions"
            usage = "/vive <command>"
            aliases = listOf("vse")
        }
    }
}

// https://github.com/BreadMoirai/github-release-gradle-plugin
tasks.register<GithubReleaseTask>("createGithubRelease").configure {

    owner.set("CJCrafter")
    repo.set("Vivecraft_Spigot_Extensions")
    authorization.set("Token ${findProperty("pass").toString()}")
    tagName.set("${project.version}")
    targetCommitish.set("master")
    releaseName.set("${project.version}")
    draft.set(false)
    prerelease.set(false)
    generateReleaseNotes.set(true)
    body.set("")
    overwrite.set(false)
    allowUploadToExisting.set(false)
    apiEndpoint.set("https://api.github.com")

    setReleaseAssets(file("build/libs").listFiles())

    // If set to true, you can debug that this would do
    dryRun.set(false)

    doFirst {
        println("Creating GitHub release")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":")) // base project

    listOf("19_R3", "20_R1", "20_R2", "20_R3", "20_R4", "21_R1").forEach {
        implementation(project(":Vivecraft_1_$it", "reobf"))
    }
}

// The shadowJar task builds a "fat jar" (a jar with all dependencies built in).
tasks.shadowJar {
    archiveFileName.set("VivecraftSpigot-${project.version}.jar")

    // This automatically "shades" (adds to jar) the bstats libs into the
    // org.vivecraft.bstats package.
    dependencies {
        include(project(":")) // base project

        listOf("19_R3", "20_R1", "20_R2", "20_R3", "20_R4", "21_R1").forEach {
            include(dependency(":Vivecraft_1_$it"))
        }

        relocate("org.bstats", "com.cjcrafter.vivecraft.bstats") {
            include(dependency("org.bstats:"))
        }

        relocate("com.cryptomorin.xseries", "com.cjcrafter.vivecraft.xseries") {
            include(dependency("com.github.cryptomorin:XSeries:"))
        }

        relocate("com.cjcrafter.foliascheduler", "com.cjcrafter.vivecraft.foliascheduler") {
            include(dependency("com.cjcrafter:foliascheduler:"))
        }
    }
}
