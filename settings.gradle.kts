try {
    val peClass = Class.forName("java.lang.ProcessEnvironment")
    val theEnvironmentField = peClass.getDeclaredField("theEnvironment").apply { isAccessible = true }
    (theEnvironmentField.get(null) as? MutableMap<*, *>)?.remove("ANDROID_PREFS_ROOT")

    val ciEnvField = peClass.getDeclaredField("theCaseInsensitiveEnvironment").apply { isAccessible = true }
    (ciEnvField.get(null) as? MutableMap<*, *>)?.remove("ANDROID_PREFS_ROOT")
} catch (_: Throwable) {}

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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "F-Store"
include(":app")
 