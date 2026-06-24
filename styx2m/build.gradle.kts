import java.util.Properties

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
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

version = "0.2.2-beta5"

val appVersionCode = 14
val iosMarketingVersion = project.version.toString().substringBefore('-')

val localProperties by lazy {
    Properties().apply {
        listOf(
            rootProject.file("iosApp/Config/Local.xcconfig"),
            rootProject.file("local.properties"),
        ).filter { it.isFile }.forEach { file ->
            file.inputStream().use(::load)
        }
    }
}

fun localConfig(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(name)?.takeIf { it.isNotBlank() }

fun requiredLocalConfig(name: String) =
    providers.provider {
        localConfig(name)
            ?: error("Missing $name. Add it to local.properties, ~/.gradle/gradle.properties, -P$name=..., or ORG_GRADLE_PROJECT_$name.")
    }

kotlin {
    jvmToolchain(17)
    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "moe.styx.styx2m.ComposeApp")
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
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(libs.moko.permissions)
            implementation("moe.styx:styx-common-compose:0.5.1") {
                exclude(group = "com.github.luben")
            }
        }

        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activityCompose)
            implementation(libs.compose.uitooling)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.android.libmpv)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
            implementation(libs.jellyfin.media3.ffmpeg.decoder)
            implementation(libs.android.libass.media)
            //noinspection UseTomlInstead
            implementation("com.github.luben:zstd-jni:1.5.6-9@aar")
        }

        iosMain.dependencies {
//            implementation("moe.styx:styx-common-compose:0.0.5-SNAPSHOT")
        }
    }
}

android {
    namespace = "moe.styx.styx2m"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        //noinspection EditedTargetSdkVersion
        targetSdk = 36

        applicationId = "moe.styx.styx2m"
        versionCode = appVersionCode
        versionName = "${project.version}"
        base.archivesName = "$applicationId-v$versionName"
    }

    signingConfigs {
        create("release") {
            storeFile = localConfig("KEY_FILE_PATH").orEmpty()
                .let { if (it.isBlank()) file("../styx2m.jks") else File(it) }
            storePassword = localConfig("STYX_SIGNING_KEY_PASS")
            keyAlias = localConfig("STYX_SIGNING_ALIAS")
            keyPassword = localConfig("STYX_SIGNING_KEY_PASS")
        }
    }

    splits {
        density {
            isEnable = false
        }
        abi {
            isEnable = true
            reset()
            //noinspection ChromeOsAbiSupport - this should be covered by universal (?)
            include("arm64-v8a")
            isUniversalApk = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                project.file("../proguard.rules")
            )
        }
    }
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
        resources.srcDirs("src/commonMain/resources")
    }

    packaging {
        jniLibs {
            pickFirsts += "**/libc++_shared.so"
        }
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
    val siteURL = requiredLocalConfig("STYX_SITEURL")
    buildConfigField("DEBUG_TOKEN", localConfig("STYX_DEBUGTOKEN"))
    buildConfigField("APP_NAME", "Styx2m")
    buildConfigField("APP_VERSION", provider { "${project.version}" })
    buildConfigField("APP_SECRET", requiredLocalConfig("STYX_SECRET"))
    buildConfigField("BASE_URL", requiredLocalConfig("STYX_BASEURL")) // Example: https://api.company.com
    buildConfigField("SITE_URL", siteURL) // Example: https://company.com
    buildConfigField("IMAGE_URL", requiredLocalConfig("STYX_IMAGEURL")) // Example: https://images.company.com
    buildConfigField("SITE", siteURL.map { it.removePrefix("https://").removePrefix("http://").trimEnd('/') })
    buildConfigField("BUILD_TIME", (System.currentTimeMillis() / 1000))
    buildConfigField("VERSION_CHECK_URL", "https://api.github.com/repos/Vodes/Styx-2m/tags")
}

tasks.register("printIosBundleVersion") {
    group = "versioning"
    description = "Prints iOS bundle version settings derived from the Gradle project version."
    notCompatibleWithConfigurationCache("This print-only helper is consumed by Xcode build scripts.")

    doLast {
        println("MARKETING_VERSION=$iosMarketingVersion")
        println("CURRENT_PROJECT_VERSION=$appVersionCode")
    }
}
