rootProject.name = "Styx2m"
include(":styx2m")

val localCommonCompose = file("../Styx-Common-Compose")
if (localCommonCompose.isDirectory) {
    includeBuild(localCommonCompose) {
        dependencySubstitution {
            substitute(module("moe.styx:styx-common-compose"))
                .using(project(":styx-common-compose"))
        }
    }
}

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
