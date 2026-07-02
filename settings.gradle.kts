pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // poishadow (centic9/poi-on-android)
    }
}
rootProject.name = "OpenFiles"
include(":app")
include(":core:ui", ":core:data", ":core:common")
include(":feature:browser", ":feature:gallery", ":feature:viewer")
