package com.sarimmehdi.task.buildlogic.utils

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import org.gradle.api.file.DirectoryProperty
import java.io.File

internal fun generateConfigureQualityTools(
    pkg: String,
    outputDir: DirectoryProperty
) {
    val projectClass = ClassName("org.gradle.api", "Project")
    val detektClass = ClassName("io.gitlab.arturbosch.detekt", "Detekt")
    val detektExtClass = ClassName("io.gitlab.arturbosch.detekt.extensions", "DetektExtension")
    val ktlintExtClass = ClassName("org.jlleitschuh.gradle.ktlint", "KtlintExtension")
    val kotlinExtClass = ClassName("org.jetbrains.kotlin.gradle.dsl", "kotlinExtension")
    val configClass = ClassName("$pkg.utils", "Config")

    val configureFunction = FunSpec.builder("configureQualityTools")
        .receiver(projectClass)
        .addModifiers(KModifier.INTERNAL)
        .addCode(
            """
            val detektConfig = %T.Detekt()
            val ktlintConfig = %T.Ktlint()

            extensions.configure<%T> {
                // Accessing the version from the catalog
                toolVersion = libs.versions.detektVersion.get()
                config.setFrom(files("${'$'}rootDir${'$'}{detektConfig.configFilePath}"))
                buildUponDefaultConfig = detektConfig.buildUponDefaultConfig
                allRules = detektConfig.allRules
            }

            extensions.configure<%T> {
                version.set(ktlintConfig.version)
                verbose.set(ktlintConfig.verbose)
                outputToConsole.set(ktlintConfig.outputToConsole)
                filter { exclude { it.file.path.contains("build/") } }
            }

            tasks.withType<%T>().configureEach {
                val buildDir = project.layout.buildDirectory.get().asFile.absolutePath
                val nonGeneratedSourceDirs = project.%T.sourceSets
                    .flatMap { it.kotlin.srcDirs }
                    .filter { dir -> dir.exists() && !dir.absolutePath.contains(buildDir) }

                setSource(files(nonGeneratedSourceDirs))
                detektConfig.inclusions.forEach { include(it) }
                detektConfig.exclusions.forEach { exclude(it) }
            }
            """.trimIndent(),
            configClass,
            configClass,
            detektExtClass,
            ktlintExtClass,
            detektClass,
            kotlinExtClass
        )
        .build()

    val fileSpec = FileSpec.builder("$pkg.utils", "ConfigureQualityTools")
        .addFunction(configureFunction)
        .addImport("org.gradle.kotlin.dsl", "configure", "withType")
        .build()

    val targetRoot = File(outputDir.get().asFile, "convention/src/main/kotlin")
    fileSpec.writeTo(targetRoot)
}
