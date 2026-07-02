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
    }
}
rootProject.name = "OpenFiles"
include(":app")
include(":core:ui", ":core:data", ":core:common")
include(":feature:browser", ":feature:gallery", ":feature:viewer")
