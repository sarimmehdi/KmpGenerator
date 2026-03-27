package com.sarimmehdi.task.buildlogic

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.file.DirectoryProperty
import java.io.File

internal fun generateKmpPresentationPlugin(
    pkg: String,
    outputDir: DirectoryProperty,
) {
    val projectClass = ClassName("org.gradle.api", "Project")
    val pluginClass = ClassName("org.gradle.api", "Plugin").parameterizedBy(projectClass)

    val pluginType =
        TypeSpec
            .classBuilder("KmpPresentationPlugin")
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(pluginClass)
            .addFunction(
                FunSpec
                    .builder("apply")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("target", projectClass)
                    .addCode(buildPresentationApplyBody())
                    .build(),
            ).build()

    val fileSpec =
        FileSpec
            .builder(pkg, "KmpPresentationPlugin")
            .addType(pluginType)
            .indent("    ")
            .addImport("org.gradle.internal.Actions", "with")
            .addImport("org.gradle.kotlin.dsl", "configure")
            .addImport("$pkg.utils", "libs", "configureAndroidTarget", "configureQualityTools")
            .addKotlinDefaultImports(includeJvm = false, includeJs = false)
            .build()

    writeCorrectedFile(pkg, fileSpec, outputDir)
}

private fun buildPresentationApplyBody(): CodeBlock {
    val kmpExt = ClassName("org.jetbrains.kotlin.gradle.dsl", "KotlinMultiplatformExtension")
    return CodeBlock
        .builder()
        .beginControlFlow("with(target)")
        .beginControlFlow("pluginManager.apply")
        .add(pluginApplyBlock("kotlinMultiplatformPlugin"))
        .add(pluginApplyBlock("androidKotlinMultiplatformLibrary"))
        .add(pluginApplyBlock("composeMultiplatformPlugin"))
        .add(pluginApplyBlock("composeCompilerPlugin"))
        .add(pluginApplyBlock("detektPlugin"))
        .add(pluginApplyBlock("ktlintPlugin"))
        .endControlFlow()
        .addStatement("configureQualityTools()")
        .beginControlFlow("extensions.configure<%T>", kmpExt)
        .addStatement("configureAndroidTarget(target)")
        .beginControlFlow("sourceSets.apply")
        .beginControlFlow("commonMain.dependencies")
        .addStatement("implementation(libs.bundles.composeCoreBundle)")
        .addStatement("implementation(libs.bundles.androidxLifecycleBundle)")
        .addStatement("implementation(libs.bundles.kotlinxEssentialsBundle)")
        .addStatement("implementation(libs.composeUiToolingPreviewLibrary)")
        .endControlFlow()
        .addStatement("androidMain.dependencies { implementation(libs.bundles.androidUiSupportBundle) }")
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .build()
}

private fun pluginApplyBlock(pluginName: String): CodeBlock =
    CodeBlock.of("apply(⇥\nlibs.plugins.$pluginName⇥\n.get()⇥\n.pluginId,⇥\n⇤⇤⇤)\n")

private fun writeCorrectedFile(
    pkg: String,
    fileSpec: FileSpec,
    outputDir: DirectoryProperty,
) {
    val rawCode = fileSpec.toString()
    val correctedCode =
        rawCode.replace(
            "import org.gradle.`internal`.Actions.with",
            "import org.gradle.internal.Actions.with",
        )

    val packagePath = pkg.replace(".", "/")
    val targetRoot = File(outputDir.get().asFile, "convention/src/main/kotlin/$packagePath")
    targetRoot.mkdirs()
    File(targetRoot, "KmpPresentationPlugin.kt").writeText(correctedCode)
}
