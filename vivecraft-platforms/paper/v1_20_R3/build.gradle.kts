plugins {
    `java-library`
    id("io.papermc.paperweight.userdev")
}


repositories {
    maven(url = "https://central.sonatype.com/repository/maven-snapshots/") // FoliaScheduler Snapshots
}

dependencies {
    compileOnly(project(":vivecraft-core"))
    compileOnly(libs.foliaScheduler)
    paperweight.paperDevBundle("1.20.3-R0.1-SNAPSHOT")
}
