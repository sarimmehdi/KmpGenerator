package com.sarimmehdi.task.buildlogic.utils

import java.io.File

internal fun generateRootFiles(root: File) {
    File(root, ".gitignore").writeText("/build\n")

    File(root, "settings.gradle.kts").writeText("""
            dependencyResolutionManagement {
                repositories {
                    gradlePluginPortal()
                    google()
                    mavenCentral()
                }
                versionCatalogs {
                    create("libs") {
                        from(files("../gradle/libs.versions.toml"))
                    }
                }
            }
            rootProject.name = "build-logic"
            include(":convention")
        """.trimIndent())
}