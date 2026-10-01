plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.eggyhub.android.videodex"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.eggyhub.android.videodex"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // GSYVideoPlayer - 全量版（播放器核心）
    implementation("com.github.CarGuo.GSYVideoPlayer:GSYVideoPlayer:v8.6.0-release-jitpack")
}