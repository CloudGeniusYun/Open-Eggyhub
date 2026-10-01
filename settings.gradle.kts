pluginManagement {
    repositories {
        maven{url=uri("https://maven.aliyun.com/repository/google")}
        maven{url=uri("https://maven.aliyun.com/repository/central")}
        maven { url=uri ("https://maven.aliyun.com/repository/public")}
        maven { url=uri ("https://maven.aliyun.com/repository/gradle-plugin")}
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://jitpack.io/com/tencent/shadow") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/tencent/shadow")
            credentials {
                username = "readonlypat"
                password = "\u0067hp_s3VOOZnLf1bTyvHWblPfaessrVYyEU4JdNbs"
            }
        }
    }
}

rootProject.name = "Eggyhub"
include(":app")
