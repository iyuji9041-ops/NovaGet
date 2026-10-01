pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        val localMvn = file("/root/maven/localMvnRepository")
        if (localMvn.exists()) {
            maven { url = uri(localMvn) }
        }
    }
}

rootProject.name = "VideoDownloader"
include(":app")
