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

internal fun generateKmpDiPlugin(
    pkg: String,
    outputDir: DirectoryProperty,
) {
    val projectClass = ClassName("org.gradle.api", "Project")
    val pluginClass = ClassName("org.gradle.api", "Plugin").parameterizedBy(projectClass)
    val diExtClass = ClassName("$pkg.utils", "KmpDiExtension")

    val pluginType =
        TypeSpec
            .classBuilder("KmpDiPlugin")
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(pluginClass)
            .addFunction(
                FunSpec
                    .builder("apply")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("target", projectClass)
                    .addCode(buildApplyBody(diExtClass))
                    .build(),
            ).build()

    val fileSpec =
        FileSpec
            .builder(pkg, "KmpDiPlugin")
            .addType(pluginType)
            .indent("    ")
            .addImport("org.gradle.kotlin.dsl", "configure")
            .addImport("$pkg.utils", "libs", "configureAndroidTarget", "configureQualityTools")
            .addKotlinDefaultImports(includeJvm = false, includeJs = false)
            .build()

    writeCorrectedFile(pkg, fileSpec, outputDir)
}

private fun buildApplyBody(diExtClass: ClassName): CodeBlock {
    val kmpExt = ClassName("org.jetbrains.kotlin.gradle.dsl", "KotlinMultiplatformExtension")
    return CodeBlock
        .builder()
        .addStatement("val diExtension = target.extensions.create(%S, %T::class.java)", "kmpDi", diExtClass)
        .add("\n")
        .beginControlFlow("with(target)")
        .add(buildPluginManagerBlock())
        .add("\n")
        .addStatement("configureQualityTools()")
        .add("\n")
        .beginControlFlow("extensions.configure<%T>", kmpExt)
        .addStatement("configureAndroidTarget(target)")
        .add("\n")
        .add(buildAfterEvaluateBlock())
        .endControlFlow()
        .endControlFlow()
        .build()
}

private fun buildPluginManagerBlock(): CodeBlock =
    CodeBlock
        .builder()
        .beginControlFlow("pluginManager.apply")
        .add(pluginApplyBlock("kotlinMultiplatformPlugin"))
        .add(pluginApplyBlock("androidKotlinMultiplatformLibrary"))
        .add(pluginApplyBlock("detektPlugin"))
        .add(pluginApplyBlock("ktlintPlugin"))
        .endControlFlow()
        .build()

private fun buildAfterEvaluateBlock(): CodeBlock =
    CodeBlock
        .builder()
        .beginControlFlow("afterEvaluate")
        .beginControlFlow("sourceSets.apply")
        .beginControlFlow("getByName(%S).dependencies", "commonMain")
        .addStatement("implementation(libs.bundles.koinCommonBundle)")
        .addStatement("implementation(libs.bundles.kotlinxEssentialsBundle)")
        .add("\n")
        .beginControlFlow("if (diExtension.useRoom)")
        .addStatement("implementation(libs.bundles.roomCommonBundle)")
        .endControlFlow()
        .beginControlFlow("if (diExtension.useDatastore)")
        .addStatement("implementation(libs.bundles.datastoreBundle)")
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .build()

private fun pluginApplyBlock(pluginName: String): CodeBlock =
    CodeBlock.of(
        "apply(⇥\nlibs.plugins.$pluginName⇥\n.get()⇤⇥\n.pluginId,⇤⇤\n)\n",
    )

private fun writeCorrectedFile(
    pkg: String,
    fileSpec: FileSpec,
    outputDir: DirectoryProperty,
) {
    val packagePath = pkg.replace(".", "/")
    val targetRoot = File(outputDir.get().asFile, "convention/src/main/kotlin/$packagePath")
    targetRoot.mkdirs()
    val code = fileSpec.toString()
    File(targetRoot, "KmpDiPlugin.kt").writeText(code)
}
