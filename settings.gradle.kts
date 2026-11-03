pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("""com\.android.*""")
                includeGroupByRegex("""com\.google.*""")
                includeGroupByRegex("""androidx.*""")
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
        // Only for Tesseract4Android (the OCR), which is published there.
        maven("https://jitpack.io") {
            content { includeGroupByRegex("""com.github.adaptech-cz.*""") }
        }
    }
}

rootProject.name = "CoverIt"
include(":app")
