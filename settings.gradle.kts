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
    }
}

// T03 (ADR 0001): fork CalisthenicsMemory @ 045b8577 as :app base;
// :domain = pure JVM domain (no Android imports), :data = Room library.
rootProject.name = "calisthenics"
include(":domain", ":data", ":app")
