pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "agent-manager"

// Modules are added by their roadmap tasks and by /new-module.
include(":app")
include(":core:domain")
include(":core:testing")
include(":tests:architecture")
