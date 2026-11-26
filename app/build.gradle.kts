import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.olauncher"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        applicationId = "app.litelauncher"
        minSdk = 24
        targetSdk = 35
        versionCode = 114
        versionName = "v6.6.23"

        resourceConfigurations.addAll(listOf("en", "ar", "de", "es-rES", "es-rUS", "fr", "he", "hr", "hu", "in", "it", "ja", "nl", "pl", "pt-rBR", "ru-rRU", "sv", "tr", "uk", "zh"))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystoreFile = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }?.let { File(it) }
    val hasReleaseSigning = keystoreFile?.exists() == true

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("playprotect") {
            dimension = "distribution"
            versionNameSuffix = "-PlayProtect"
            buildConfigField("boolean", "USE_ACCESSIBILITY_LOCK", "false")
        }
        create("standard") {
            dimension = "distribution"
            buildConfigField("boolean", "USE_ACCESSIBILITY_LOCK", "true")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    lint {
        disable += listOf("ExtraTranslation", "MissingTranslation")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.kotlin.stdlib)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.biometric)
    implementation(libs.recyclerview)

    // Android lifecycle
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Navigation
    implementation(libs.navigation.fragment.ktx)

    // Work Manager
    implementation(libs.work.runtime.ktx)

    // Material dependency
    implementation(libs.material)
}
