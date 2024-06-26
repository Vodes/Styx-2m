import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.kotlinx.serialization)
}

repositories {
    mavenCentral()
    google()
    maven("https://repo.styx.moe/releases")
    maven("https://repo.styx.moe/snapshots")
    mavenLocal()
}

version = "0.0.3"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            implementation(libs.moko.permissions)
            implementation(libs.kstore)
            implementation(libs.kstore.file)
            implementation(libs.multiplatform.insets)
            implementation(libs.styx.common)
            implementation(libs.styx.common.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activityCompose)
            implementation(libs.compose.uitooling)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.android.libmpv)
        }

        iosMain.dependencies {
//            implementation("moe.styx:styx-common-compose:0.0.5-SNAPSHOT")
        }
    }
}

android {
    namespace = "moe.styx.styx2m"
    compileSdk = 34

    defaultConfig {
        minSdk = 27
        targetSdk = 34

        applicationId = "moe.styx.styx2m"
        versionCode = 3
        versionName = "${project.version}"
        archivesName = "$applicationId-v$versionName"
    }

    splits {
        density {
            isEnable = false
        }
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                project.file("../proguard.rules")
            )
        }
    }
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/resources")
        resources.srcDirs("src/commonMain/resources")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

buildConfig {
    val siteURL = System.getenv("STYX_SITEURL")
    buildConfigField("DEBUG_TOKEN", System.getenv("STYX_DEBUGTOKEN"))
    buildConfigField("APP_NAME", "Styx2m")
    buildConfigField("APP_VERSION", provider { "${project.version}" })
    buildConfigField("APP_SECRET", System.getenv("STYX_SECRET"))
    buildConfigField("BASE_URL", System.getenv("STYX_BASEURL")) // Example: https://api.company.com
    buildConfigField("SITE_URL", siteURL) // Example: https://company.com
    buildConfigField("IMAGE_URL", System.getenv("STYX_IMAGEURL")) // Example: https://images.company.com
    buildConfigField("SITE", siteURL.split("https://").getOrElse(1) { siteURL })
    buildConfigField("BUILD_TIME", (System.currentTimeMillis() / 1000))
    buildConfigField("VERSION_CHECK_URL", "https://raw.githubusercontent.com/Vodes/Styx-2m/master/styx2m/build.gradle.kts")
}

//buildConfig {
//    // BuildConfig configuration here.
//    // https://github.com/gmazzo/gradle-buildconfig-plugin#usage-in-kts
//}
