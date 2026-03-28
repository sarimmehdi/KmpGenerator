package com.sarimmehdi.task.domain.utils

import java.io.File

internal fun generateModuleRootFiles(
    moduleDir: File,
    dependencies: List<String>,
) {
    File(moduleDir, ".gitignore").writeText("/build\n")

    val validDeps = dependencies.filter { it.startsWith(":") }

    val dependencyBlock =
        if (validDeps.isNotEmpty()) {
            """
        |
        |kotlin {
        |    sourceSets {
        |        commonMain.dependencies {
        |            ${validDeps.joinToString("\n            ") { "implementation(project(\"$it\"))" }}
        |        }
        |    }
        |}
            """.trimMargin()
        } else {
            ""
        }

    File(moduleDir, "build.gradle.kts").writeText(
        """
        plugins {
            alias(libs.plugins.kmpDomainPlugin)
        }
        $dependencyBlock
        """.trimIndent(),
    )
}
