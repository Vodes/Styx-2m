rootProject.name = "Styx2m"
include(":styx2m")
includeBuild("context/Styx-Common-Compose")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}
