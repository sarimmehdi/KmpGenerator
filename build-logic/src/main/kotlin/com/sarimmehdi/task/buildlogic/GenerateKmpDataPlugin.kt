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

internal fun generateKmpDataPlugin(
    pkg: String,
    outputDir: DirectoryProperty
) {
    val projectClass = ClassName("org.gradle.api", "Project")
    val pluginClass = ClassName("org.gradle.api", "Plugin").parameterizedBy(projectClass)
    val kmpExtensionClass = ClassName("org.jetbrains.kotlin.gradle.dsl", "KotlinMultiplatformExtension")
    val kspExtensionClass = ClassName("com.google.devtools.ksp.gradle", "KspExtension")
    val roomExtensionClass = ClassName("androidx.room.gradle", "RoomExtension")
    val kmpDataExtClass = ClassName("$pkg.utils", "KmpDataExtension")

    val applyBody = CodeBlock.builder()
        .addStatement("val dataExtension = target.extensions.create(%S, %T::class.java)", "kmpData", kmpDataExtClass)
        .beginControlFlow("with(target)")
        .beginControlFlow("pluginManager")
        .addStatement("apply(libs.plugins.kotlinMultiplatformPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.androidKotlinMultiplatformLibrary.get().pluginId)")
        .addStatement("apply(libs.plugins.detektPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.ktlintPlugin.get().pluginId)")
        .beginControlFlow("if (dataExtension.useRoom)")
        .addStatement("apply(libs.plugins.roomPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.kspPlugin.get().pluginId)")
        .endControlFlow()
        .endControlFlow()

        .addStatement("configureQualityTools()")

        .beginControlFlow("if (dataExtension.useRoom)")
        .addStatement("extensions.configure<%T> { arg(%S, %S) }", kspExtensionClass, "room.generateKotlin", "true")
        .addStatement("extensions.configure<%T> { schemaDirectory(%S) }", roomExtensionClass,
            $$"$projectDir/schemas"
        )
        .endControlFlow()

        .beginControlFlow("extensions.configure<%T>", kmpExtensionClass)
        .addStatement("configureAndroidTarget(target)")
        .beginControlFlow("sourceSets.apply")
        .beginControlFlow("commonMain.dependencies")
        .addStatement("implementation(libs.bundles.kotlinxEssentialsBundle)")
        .beginControlFlow("if (dataExtension.useRoom)")
        .addStatement("implementation(libs.bundles.roomCommonBundle)")
        .endControlFlow()
        .beginControlFlow("if (dataExtension.useDatastore)")
        .addStatement("implementation(libs.bundles.datastoreBundle)")
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()

        .beginControlFlow("if (dataExtension.useRoom)")
        .addStatement("dependencies.add(%S, libs.roomCompilerLibrary.get())", "kspCommonMainMetadata")
        .addStatement("dependencies.add(%S, libs.roomCompilerLibrary.get())", "kspAndroid")
        .endControlFlow()
        .endControlFlow()
        .build()

    val pluginType = TypeSpec.classBuilder("KmpDataPlugin")
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

    FileSpec.builder(pkg, "KmpDataPlugin")
        .addType(pluginType)
        .addImport("org.gradle.kotlin.dsl", "configure")
        .addImport("$pkg.utils", "libs", "configureAndroidTarget", "configureQualityTools")
        .build()
        .writeTo(File(outputDir.get().asFile, "convention/src/main/kotlin"))
}