pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
includeBuild("../../..") {
    dependencySubstitution {
        substitute(module("app.calisthenics:data")).using(project(":data"))
    }
}
rootProject.name = "storage-browser-tests"
