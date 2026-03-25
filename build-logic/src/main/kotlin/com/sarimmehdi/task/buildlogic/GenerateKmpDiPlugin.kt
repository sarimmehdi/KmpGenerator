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
    outputDir: DirectoryProperty
) {
    val projectClass = ClassName("org.gradle.api", "Project")
    val pluginClass = ClassName("org.gradle.api", "Plugin").parameterizedBy(projectClass)
    val kmpExtensionClass = ClassName("org.jetbrains.kotlin.gradle.dsl", "KotlinMultiplatformExtension")
    val kmpDiExtClass = ClassName("$pkg.utils", "KmpDiExtension")

    val applyBody = CodeBlock.builder()
        .addStatement("val diExtension = target.extensions.create(%S, %T::class.java)", "kmpDi", kmpDiExtClass)
        .beginControlFlow("with(target)")
        .beginControlFlow("pluginManager")
        .addStatement("apply(libs.plugins.kotlinMultiplatformPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.androidKotlinMultiplatformLibrary.get().pluginId)")
        .addStatement("apply(libs.plugins.detektPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.ktlintPlugin.get().pluginId)")
        .endControlFlow()

        .addStatement("configureQualityTools()")

        .beginControlFlow("extensions.configure<%T>", kmpExtensionClass)
        .addStatement("configureAndroidTarget(target)")
        .beginControlFlow("afterEvaluate")
        .beginControlFlow("sourceSets.apply")
        .beginControlFlow("getByName(%S).dependencies", "commonMain")
        .addStatement("implementation(libs.bundles.koinCommonBundle)")
        .addStatement("implementation(libs.bundles.kotlinxEssentialsBundle)")
        .beginControlFlow("if (diExtension.useRoom)")
        .addStatement("implementation(libs.bundles.roomCommonBundle)")
        .endControlFlow()
        .beginControlFlow("if (diExtension.useDatastore)")
        .addStatement("implementation(libs.bundles.datastoreBundle)")
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .build()

    val pluginType = TypeSpec.classBuilder("KmpDiPlugin")
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

    FileSpec.builder(pkg, "KmpDiPlugin")
        .addType(pluginType)
        .addImport("org.gradle.kotlin.dsl", "configure")
        .addImport("$pkg.utils", "libs", "configureAndroidTarget", "configureQualityTools")
        .build()
        .writeTo(File(outputDir.get().asFile, "convention/src/main/kotlin"))
}