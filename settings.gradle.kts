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
include(":core:process")
include(":core:storage")
include(":core:testing")
include(":feature:agent-runtime:api")
include(":feature:agent-runtime:impl")
include(":feature:command-palette:api")
include(":feature:command-palette:impl")
include(":feature:command-palette:ui")
include(":feature:mission-control:ui")
include(":feature:projects:api")
include(":feature:projects:impl")
include(":feature:projects:ui")
include(":feature:sessions:api")
include(":feature:sessions:impl")
include(":tests:architecture")
