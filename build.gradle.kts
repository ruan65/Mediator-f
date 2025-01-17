plugins {
    kotlin("jvm") version "1.6.10" apply false
    id("org.jetbrains.compose") version "1.1.1" apply false
    id("com.bybutter.sisyphus.protobuf") version "1.3.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0" apply false
    id("org.jetbrains.changelog") version "1.3.0"
}

allprojects {
    group = "io.kanro"
    version = "2.0.0"

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

changelog {
    version.set(project.version.toString())
    groups.set(emptyList())
}
