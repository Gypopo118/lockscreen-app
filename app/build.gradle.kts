plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.lockscreen"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lockscreen"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        getByName("debug") {
            // Постоянный ключ из корня репо: подпись одинакова на всех сборках,
            // иначе Android молча отклоняет обновление (другой ключ = другое приложение).
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}
