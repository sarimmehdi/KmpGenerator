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
    outputDir: DirectoryProperty
) {
    val projectClass = ClassName("org.gradle.api", "Project")
    val pluginClass = ClassName("org.gradle.api", "Plugin").parameterizedBy(projectClass)
    val kmpExtensionClass = ClassName("org.jetbrains.kotlin.gradle.dsl", "KotlinMultiplatformExtension")

    val applyBody = CodeBlock.builder()
        .beginControlFlow("with(target)")
        .beginControlFlow("pluginManager.apply")
        .addStatement("apply(libs.plugins.kotlinMultiplatformPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.androidKotlinMultiplatformLibrary.get().pluginId)")
        .addStatement("apply(libs.plugins.composeMultiplatformPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.composeCompilerPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.detektPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.ktlintPlugin.get().pluginId)")
        .endControlFlow()

        .addStatement("configureQualityTools()")

        .beginControlFlow("extensions.configure<%T>", kmpExtensionClass)
        .addStatement("configureAndroidTarget(target)")
        .beginControlFlow("sourceSets.apply")
        .beginControlFlow("commonMain.dependencies")
        .addStatement("implementation(libs.bundles.composeCoreBundle)")
        .addStatement("implementation(libs.bundles.androidxLifecycleBundle)")
        .addStatement("implementation(libs.bundles.kotlinxEssentialsBundle)")
        .addStatement("implementation(libs.composeUiToolingPreviewLibrary)")
        .endControlFlow()
        .addStatement("androidMain.dependencies { implementation(libs.bundles.androidUiSupportBundle) }")
        .endControlFlow() // end sourceSets.apply
        .endControlFlow() // end extensions.configure
        .endControlFlow() // end with(target)
        .build()

    val pluginType = TypeSpec.classBuilder("KmpPresentationPlugin")
        .addModifiers(KModifier.INTERNAL)
        .addSuperinterface(pluginClass)
        .addFunction(
            FunSpec.builder("apply")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("target", projectClass)
                .addCode(applyBody)
                .build()
        )
        .build()

    FileSpec.builder(pkg, "KmpPresentationPlugin")
        .addType(pluginType)
        .addImport("org.gradle.kotlin.dsl", "configure")
        .addImport("$pkg.utils", "libs", "configureAndroidTarget", "configureQualityTools")
        .build()
        .writeTo(File(outputDir.get().asFile, "convention/src/main/kotlin"))
}