plugins {
    `java-library`
    id("io.papermc.paperweight.userdev")
}

dependencies {
    compileOnly(project(":vivecraft-core"))
    compileOnly(libs.foliaScheduler)
    paperweight.paperDevBundle("1.20.2-R0.1-SNAPSHOT")
}
