pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "PersonalCloudstream"
include("EvDiziBox", "EvWebteIzle")
project(":EvDiziBox").projectDir = file("DiziBox")
project(":EvWebteIzle").projectDir = file("WebteIzle")
