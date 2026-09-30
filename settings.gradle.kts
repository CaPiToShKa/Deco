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
        // Salesforce Marketing Cloud MobilePush SDK is published here, not on Maven Central.
        maven("https://salesforce-marketingcloud.github.io/MarketingCloudSDK-Android/repository") {
            content { includeGroup("com.salesforce.marketingcloud") }
        }
    }
}

rootProject.name = "deco-socio"

include(":core:domain")
include(":core:api")
include(":core:data")
// `-PbffOnly=true` builds the server without the Android module (no Android SDK needed, e.g. in Docker).
if (providers.gradleProperty("bffOnly").orNull != "true") {
    include(":app")
}
include(":bff")
