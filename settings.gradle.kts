import java.net.URI

pluginManagement {
    repositories {

        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }

        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url= URI("https://jitpack.io") }
        maven { url= URI("https://repository.liferay.com/nexus/content/repositories/public") }
        gradlePluginPortal()
    }
}

rootProject.name = "ReadX"
include(":app")
include(":pdfengine")
