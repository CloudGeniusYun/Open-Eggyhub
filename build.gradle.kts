// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }
    }
    

}

extra.apply {
    set("compileSdkVersion", 35)
    set("targetSdkVersion", 28)
    set("minSdk", 24)
    set("versionCode", 400)
    set("versionName", "4.0.0")
    set("xVersion", "1.1.0")
    set("hiddenApiBypass", "4.3")
    set("shadowVersion", "local-4e9f70a6-SNAPSHOT")
}
plugins {
    id("com.android.application") version "8.12.0" apply false
    id("com.android.library") version "8.12.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}