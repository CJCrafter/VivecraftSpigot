plugins {
    `java-library`
    `maven-publish`
    signing
    id("io.codearte.nexus-staging") version "0.30.0"
    id("io.papermc.paperweight.userdev") version "1.7.1" apply false
}

repositories {
    mavenCentral()

    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:24.1.0")

    // soft depend
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")

    // We shade these
    implementation("com.github.cryptomorin:XSeries:11.1.0")
    implementation("com.cjcrafter:foliascheduler:0.5.0")
    implementation("org.bstats:bstats-bukkit:3.0.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
        options.release.set(16) // most servers use java 16 or higher
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
}


// Create javadocJar and sourcesJar tasks
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("javadoc"))
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

nexusStaging {
    serverUrl = "https://s01.oss.sonatype.org/service/local/"
    packageGroup = "com.cjcrafter"
    stagingProfileId = findProperty("OSSRH_ID").toString()
    username = findProperty("OSSRH_USERNAME").toString()
    password = findProperty("OSSRH_PASSWORD").toString()
    numberOfRetries = 30
    delayBetweenRetriesInMillis = 3000
}

// Signing artifacts
signing {
    isRequired = true

    useInMemoryPgpKeys(
        findProperty("SIGNING_KEY_ID").toString(),
        findProperty("SIGNING_PRIVATE_KEY").toString(),
        findProperty("SIGNING_PASSWORD").toString()
    )
    sign(publishing.publications)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(javadocJar)
            artifact(sourcesJar)

            pom {
                name.set("Vivecraft_Spigot_Extensions")
                description.set("Access VR player's head and hand positions in Spigot plugins.")
                url.set("https://github.com/CJCrafter/Vivecraft_Spigot_Extensions")

                groupId = "com.cjcrafter"
                artifactId = "vivecraft"
                // version is set in the BuildVivecraftSpigotExtensions' build.gradle.kts file
                version = "3.0.0"

                licenses {
                    license {
                        name.set("GNU General Public License v3.0")
                        url.set("https://github.com/CJCrafter/Vivecraft_Spigot_Extensions/blob/master/LICENSE")
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
                    connection.set("scm:git:https://github.com/CJCrafter/Vivecraft_Spigot_Extensions.git")
                    developerConnection.set("scm:git:ssh://github.com/CJCrafter/Vivecraft_Spigot_Extensions.git")
                    url.set("https://github.com/CJCrafter/Vivecraft_Spigot_Extensions")
                }
            }
        }
    }

    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials {
                username = findProperty("OSSRH_USERNAME").toString()
                password = findProperty("OSSRH_PASSWORD").toString()
            }
        }
    }
}

// After publishing, the nexus plugin will automatically close and release
tasks.named("publish") {
    finalizedBy("closeAndReleaseRepository")
}
