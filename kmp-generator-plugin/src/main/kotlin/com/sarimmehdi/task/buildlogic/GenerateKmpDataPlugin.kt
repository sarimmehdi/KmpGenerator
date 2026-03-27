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
    outputDir: DirectoryProperty,
) {
    val projectClass = ClassName("org.gradle.api", "Project")
    val extensionClass = ClassName("$pkg.utils", "KmpDataExtension")

    val pluginType =
        TypeSpec
            .classBuilder("KmpDataPlugin")
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(ClassName("org.gradle.api", "Plugin").parameterizedBy(projectClass))
            .addFunction(buildApplyFunction(extensionClass))
            .addFunction(buildApplyBasePlugins(extensionClass))
            .addFunction(buildConfigureRoom())
            .addFunction(buildConfigureKmpTargets(extensionClass))
            .build()

    FileSpec
        .builder(pkg, "KmpDataPlugin")
        .addType(pluginType)
        .addImport("org.gradle.kotlin.dsl", "configure")
        .addImport("$pkg.utils", "libs", "configureAndroidTarget", "configureQualityTools")
        .indent("    ")
        .addKotlinDefaultImports(includeJvm = false, includeJs = false)
        .build()
        .writeTo(File(outputDir.get().asFile, "convention/src/main/kotlin"))
}

private fun buildApplyFunction(extensionClass: ClassName): FunSpec =
    FunSpec
        .builder("apply")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("target", ClassName("org.gradle.api", "Project"))
        .addCode(
            """
            val dataExtension = target.extensions.create("kmpData", %T::class.java)

            with(target) {
                applyBasePlugins(dataExtension)
                configureQualityTools()

                if (dataExtension.useRoom) {
                    configureRoom()
                }

                configureKmpTargets(dataExtension)
            }
            """.trimIndent(),
            extensionClass,
        ).build()

private fun buildApplyBasePlugins(extensionClass: ClassName): FunSpec =
    FunSpec
        .builder("applyBasePlugins")
        .addModifiers(KModifier.PRIVATE)
        .receiver(ClassName("org.gradle.api", "Project"))
        .addParameter("dataExtension", extensionClass)
        .addCode(
            CodeBlock
                .builder()
                .add(pluginApply("kotlinMultiplatformPlugin"))
                .add(pluginApply("androidKotlinMultiplatformLibrary"))
                .add(pluginApply("detektPlugin"))
                .add(pluginApply("ktlintPlugin"))
                .add("\n")
                .beginControlFlow("if (dataExtension.useRoom)")
                .add(pluginApply("roomPlugin"))
                .add(pluginApply("kspPlugin"))
                .endControlFlow()
                .build(),
        ).build()

// Helper to force the specific indentation for plugin IDs
private fun pluginApply(plugin: String): CodeBlock =
    CodeBlock.of("pluginManager.apply(⇥\nlibs.plugins.$plugin⇥\n.get()⇥\n.pluginId,⇤⇤⇤\n)\n")

private fun buildConfigureRoom(): FunSpec =
    FunSpec
        .builder("configureRoom")
        .addModifiers(KModifier.PRIVATE)
        .receiver(ClassName("org.gradle.api", "Project"))
        .addStatement(
            "extensions.configure<%T> { arg(%S, %S) }",
            ClassName("com.google.devtools.ksp.gradle", "KspExtension"),
            "room.generateKotlin",
            "true",
        ).addStatement(
            "extensions.configure<%T> { schemaDirectory(\"\$projectDir/schemas\") }",
            ClassName("androidx.room.gradle", "RoomExtension"),
        ).addCode("\n")
        .addStatement("dependencies.add(\"kspCommonMainMetadata\", libs.roomCompilerLibrary.get())")
        .addStatement("dependencies.add(\"kspAndroid\", libs.roomCompilerLibrary.get())")
        .build()

private fun buildConfigureKmpTargets(extensionClass: ClassName): FunSpec =
    FunSpec
        .builder("configureKmpTargets")
        .addModifiers(KModifier.PRIVATE)
        .receiver(ClassName("org.gradle.api", "Project"))
        .addParameter("dataExtension", extensionClass)
        .beginControlFlow(
            "extensions.configure<%T>",
            ClassName("org.jetbrains.kotlin.gradle.dsl", "KotlinMultiplatformExtension"),
        ).addStatement("configureAndroidTarget(this@configureKmpTargets)")
        .beginControlFlow("sourceSets.apply")
        .beginControlFlow("commonMain.dependencies")
        .addStatement("implementation(libs.bundles.kotlinxEssentialsBundle)")
        .addCode("\n")
        .beginControlFlow("if (dataExtension.useRoom)")
        .addStatement("implementation(libs.bundles.roomCommonBundle)")
        .endControlFlow()
        .beginControlFlow("if (dataExtension.useDatastore)")
        .addStatement("implementation(libs.bundles.datastoreBundle)")
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .build()
