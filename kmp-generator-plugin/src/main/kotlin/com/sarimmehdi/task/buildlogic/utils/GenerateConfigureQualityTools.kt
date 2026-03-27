package com.sarimmehdi.task.buildlogic.utils

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import org.gradle.api.file.DirectoryProperty
import java.io.File

internal fun generateConfigureQualityTools(
    pkg: String,
    outputDir: DirectoryProperty,
) {
    val projectClass = ClassName("org.gradle.api", "Project")

    val configureFunction =
        FunSpec
            .builder("configureQualityTools")
            .receiver(projectClass)
            .addModifiers(KModifier.INTERNAL)
            .addCode(buildQualityToolsBody(pkg))
            .build()

    val fileSpec =
        FileSpec
            .builder("$pkg.utils", "ConfigureQualityTools")
            .addFunction(configureFunction)
            .addImport("org.gradle.kotlin.dsl", "configure", "withType")
            .indent("    ")
            .addKotlinDefaultImports(includeJvm = false, includeJs = false)
            .build()

    val targetRoot = File(outputDir.get().asFile, "convention/src/main/kotlin")
    fileSpec.writeTo(targetRoot)
}

private fun buildQualityToolsBody(pkg: String): CodeBlock {
    val detektClass = ClassName("io.gitlab.arturbosch.detekt", "Detekt")
    val detektExtClass = ClassName("io.gitlab.arturbosch.detekt.extensions", "DetektExtension")
    val ktlintExtClass = ClassName("org.jlleitschuh.gradle.ktlint", "KtlintExtension")
    val kotlinExtClass = ClassName("org.jetbrains.kotlin.gradle.dsl", "kotlinExtension")
    val configClass = ClassName("$pkg.utils", "Config")

    return CodeBlock
        .builder()
        .addStatement("val detektConfig = %T.Detekt()", configClass)
        .addStatement("val ktlintConfig = %T.Ktlint()", configClass)
        .add("\n")
        .beginControlFlow("extensions.configure<%T>", detektExtClass)
        .addStatement("toolVersion = libs.versions.detektVersion.get()")
        .addStatement("config.setFrom(files(\"\$rootDir\${detektConfig.configFilePath}\"))")
        .addStatement("buildUponDefaultConfig = detektConfig.buildUponDefaultConfig")
        .addStatement("allRules = detektConfig.allRules")
        .endControlFlow()
        .add("\n")
        .beginControlFlow("extensions.configure<%T>", ktlintExtClass)
        .addStatement("version.set(ktlintConfig.version)")
        .addStatement("verbose.set(ktlintConfig.verbose)")
        .addStatement("outputToConsole.set(ktlintConfig.outputToConsole)")
        .addStatement("filter { exclude { it.file.path.contains(\"build/\") } }")
        .endControlFlow()
        .add("\n")
        .beginControlFlow("tasks.withType<%T>().configureEach", detektClass)
        .add("val buildDir =⇥\nproject.layout.buildDirectory⇥\n.get()⇥\n.asFile.absolutePath⇤⇤⇤\n")
        .add(
            "val nonGeneratedSourceDirs =⇥\n%T.sourceSets⇥\n.flatMap { it.kotlin.srcDirs }⇥\n.filter " +
                "{ dir -> dir.exists() && !dir.absolutePath.contains(buildDir) }⇤⇤⇤\n\n",
            kotlinExtClass,
        ).addStatement("setSource(files(nonGeneratedSourceDirs))")
        .addStatement("detektConfig.inclusions.forEach { include(it) }")
        .addStatement("detektConfig.exclusions.forEach { exclude(it) }")
        .endControlFlow()
        .build()
}
