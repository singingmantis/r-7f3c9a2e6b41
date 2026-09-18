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
include("EvDiziBox")
project(":EvDiziBox").projectDir = file("DiziBox")
include("EvWebteIzle")
project(":EvWebteIzle").projectDir = file("WebteIzle")
include("EvYabanciDizi")
project(":EvYabanciDizi").projectDir = file("YabanciDizi")
include("EvDiziGom")
project(":EvDiziGom").projectDir = file("DiziGom")
include("EvSezonlukDizi")
project(":EvSezonlukDizi").projectDir = file("SezonlukDizi")
include("EvHDFilmCehennemi")
project(":EvHDFilmCehennemi").projectDir = file("HDFilmCehennemi")
include("EvFilmModu")
project(":EvFilmModu").projectDir = file("FilmModu")
include("EvKultFilmler")
project(":EvKultFilmler").projectDir = file("KultFilmler")
include("EvRareFilmm")
project(":EvRareFilmm").projectDir = file("RareFilmm")
include("EvDiziPal")
project(":EvDiziPal").projectDir = file("DiziPal")
include("EvFilmMakinesi")
project(":EvFilmMakinesi").projectDir = file("FilmMakinesi")
include("EvFullHDFilmizlesene")
project(":EvFullHDFilmizlesene").projectDir = file("FullHDFilmizlesene")
