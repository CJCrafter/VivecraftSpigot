import org.jreleaser.model.Active

plugins {
    `java-library`
    `maven-publish`
    id("org.jreleaser") version "1.18.0"
}

repositories {
    mavenCentral()

    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(libs.spigotApi)

    // External "hooks" or plugins that we might interact with
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")

    // Shaded Dependencies
    compileOnly(libs.annotations)
    implementation(libs.bstats)
    implementation(libs.foliaScheduler)
    implementation(libs.xSeries)
}


val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("javadoc").map { it.outputs.files })
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)

            groupId = "com.cjcrafter"
            artifactId = "vivecraftspigot"
            version = findProperty("version").toString()

            pom {
                name.set("VivecraftSpigot")
                description.set("Spigot server plugin for improved Vivecraft modding support")
                url.set("https://github.com/CJCrafter/VivecraftSpigot")

                licenses {
                    license {
                        name.set("GNU General Public License v3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
                    }
                }
                developers {
                    developer {
                        id.set("CJCrafter")
                        name.set("Collin Barber")
                        email.set("collinjbarber@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/CJCrafter/VivecraftSpigot.git")
                    developerConnection.set("scm:git:ssh://github.com/CJCrafter/VivecraftSpigot.git")
                    url.set("https://github.com/CJCrafter/VivecraftSpigot")
                }
            }
        }
    }

    // Deploy this repository locally for staging, then let the root project actually
    // upload the maven repo using jReleaser
    repositories {
        maven {
            name = "stagingDeploy"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    gitRootSearch.set(true)

    project {
        name.set("VivecraftSpigot")
        group = "com.cjcrafter"
        version = findProperty("version").toString()
        description = "A plugin that adds scripting capabilities to Plugins"
        authors.add("CJCrafter <collinjbarber@gmail.com>")
        license = "GNU" // SPDX identifier
        copyright = "Copyright © 2025 CJCrafter"

        links {
            homepage.set("https://github.com/CJCrafter/VivecraftSpigot")
            documentation.set("https://github.com/CJCrafter/VivecraftSpigot#readme")
        }

        java {
            groupId = "com.cjcrafter"
            artifactId = "vivecraftspigot"
            version = findProperty("version").toString()
        }

        snapshot {
            fullChangelog.set(true)
        }
    }

    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
    }

    deploy {
        maven {
            mavenCentral {
                create("releaseDeploy") {
                    active.set(Active.RELEASE)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    // run ./gradlew vivecraft-core:publish before deployment
                    stagingRepository("build/staging-deploy")
                    // Credentials (JRELEASER_MAVENCENTRAL_USERNAME, JRELEASER_MAVENCENTRAL_PASSWORD or JRELEASER_MAVENCENTRAL_TOKEN)
                    // will be picked up from ~/.jreleaser/config.toml
                }
            }

            nexus2 {
                create("sonatypeSnapshots") {
                    active.set(Active.SNAPSHOT)
                    url.set("https://central.sonatype.com/repository/maven-snapshots/")
                    snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots/")
                    applyMavenCentralRules = true
                    snapshotSupported = true
                    closeRepository = true
                    releaseRepository = true
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }

    distributions {
        create("vivecraftspigot") {
            active.set(Active.ALWAYS)
            distributionType.set(org.jreleaser.model.Distribution.DistributionType.SINGLE_JAR)
            artifact {
                path.set(file("../vivecraft-build/build/libs/VivecraftSpigot-${findProperty("version")}.jar"))
            }
        }
    }

    release {
        github {
            repoOwner.set("CJCrafter")
            name.set("VivecraftSpigot")
            host.set("github.com")

            val version = findProperty("version").toString()
            val isSnapshot = version.endsWith("-SNAPSHOT")
            releaseName.set(if (isSnapshot) "SNAPSHOT" else "v$version")
            tagName.set("v{{projectVersion}}")
            draft.set(false)
            skipTag.set(isSnapshot)
            overwrite.set(false)
            update { enabled.set(isSnapshot) }

            prerelease {
                enabled.set(isSnapshot)
                pattern.set(".*-SNAPSHOT")
            }

            commitAuthor {
                name.set("Collin Barber")
                email.set("collinjbarber@gmail.com")
            }

            changelog {
                formatted.set(Active.ALWAYS)
                preset.set("conventional-commits")
                format.set("- {{commitShortHash}} {{commitTitle}}")
                contributors {
                    enabled.set(true)
                    format.set("{{contributorUsernameAsLink}}")
                }
                hide {
                    contributors.set(listOf("[bot]"))
                }
            }
        }
    }
}
