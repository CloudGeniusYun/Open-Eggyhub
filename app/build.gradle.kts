plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.eggyhub.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.eggyhub.android"
        minSdk = 23
        targetSdk = 36
        versionCode = 12
        versionName = "v1.5.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // NDK 配置
        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
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

    lint {
        disable += "Instantiatable"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.auth0.android:jwtdecode:2.0.2")
    implementation("com.github.yalantis:ucrop:2.2.8")

    // Retrofit for Bili API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room components
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // Lottie 动画
    implementation("com.airbnb.android:lottie:6.4.0")

    //PAG 动画
    implementation("com.tencent.tav:libpag:4.5.27")

    // Local dependencies (libs directory)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
}

// 构建 video-dex APK 并复制到 assets 目录
// 运行方式: ./gradlew app:prepareVideoDex
tasks.register<Copy>("prepareVideoDex") {
    dependsOn(":video-dex:assembleDebug")

    val videoDexApk = file("${rootProject.projectDir}/video-dex/build/outputs/apk/debug/video-dex-debug.apk")
    val assetsDir = file("${projectDir}/src/main/assets")

    doFirst {
        // 确保 assets 目录存在
        mkdir(assetsDir)
    }

    from(videoDexApk)
    into(assetsDir)
    rename(".*", "video_player.dex.apk")

    doLast {
        println("✅ video-dex APK 已复制到 app/src/main/assets/video_player.dex.apk")
    }
}

// 手动构建 video-dex APK 并复制到 assets
// 运行方式: ./gradlew app:prepareVideoDex
// 注意：已移除 preBuild 自动依赖，主 APK 不再包含 dex 文件，需要手动导入或从服务器下载
