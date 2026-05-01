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

rootProject.name = "ManufacturingEnterprise"
include(":app")
include(":core")
include(":auth")
include(":admin")
include(":defect")
include(":drawings")
include(":timesheet")
include(":update")
include(":sync")
