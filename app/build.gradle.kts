plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lucas.xgameassistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lucas.xgameassistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Bundled on-device OCR model; no cloud API is called.
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
