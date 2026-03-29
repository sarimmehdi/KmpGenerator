package com.sarimmehdi.task.buildlogic

import com.squareup.kotlinpoet.ClassName
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
    val kmpExtensionClass = ClassName("org.jetbrains.kotlin.gradle.dsl", "KotlinMultiplatformExtension")

    val pluginType =
        TypeSpec
            .classBuilder("KmpDataPlugin")
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(ClassName("org.gradle.api", "Plugin").parameterizedBy(projectClass))
            .addFunction(buildApplyFunction(extensionClass, kmpExtensionClass))
            .addFunction(buildApplyBasePlugins())
            .addFunction(buildConfigureRoomSettings())
            .addFunction(buildConfigureKmpDependencies(extensionClass, kmpExtensionClass))
            .build()

    FileSpec
        .builder(pkg, "KmpDataPlugin")
        .addType(pluginType)
        .addImport("org.gradle.kotlin.dsl", "configure")
        .addImport("$pkg.utils", "libs", "configureAndroidTarget", "configureQualityTools")
        .indent("    ")
        .build()
        .writeTo(File(outputDir.get().asFile, "convention/src/main/kotlin"))
}

private fun buildApplyFunction(
    extensionClass: ClassName,
    kmpExtension: ClassName,
): FunSpec =
    FunSpec
        .builder("apply")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("target", ClassName("org.gradle.api", "Project"))
        .addCode(
            """
            val dataExtension =
                target.extensions.create("kmpData", %T::class.java).apply {
                    useRoom.convention(false)
                    useDatastore.convention(false)
                }

            with(target) {
                applyBasePlugins()
                configureQualityTools()
                extensions.configure<%T> {
                    configureAndroidTarget(this@with)
                }
                configureRoomSettings()
                afterEvaluate {
                    configureKmpDependencies(dataExtension)
                    if (dataExtension.useRoom.get()) {
                        dependencies.add("kspCommonMainMetadata", libs.roomCompilerLibrary.get())
                        dependencies.add("kspAndroid", libs.roomCompilerLibrary.get())
                    }
                }
            }
            """.trimIndent(),
            extensionClass,
            kmpExtension,
        ).build()

private fun buildApplyBasePlugins(): FunSpec =
    FunSpec
        .builder("applyBasePlugins")
        .addModifiers(KModifier.PRIVATE)
        .receiver(ClassName("org.gradle.api", "Project"))
        .addCode(
            """
            pluginManager.apply(
                libs.plugins.kotlinMultiplatformPlugin
                    .get()
                    .pluginId,
            )
            pluginManager.apply(
                libs.plugins.androidKotlinMultiplatformLibrary
                    .get()
                    .pluginId,
            )
            pluginManager.apply(
                libs.plugins.detektPlugin
                    .get()
                    .pluginId,
            )
            pluginManager.apply(
                libs.plugins.ktlintPlugin
                    .get()
                    .pluginId,
            )
            pluginManager.apply(
                libs.plugins.kspPlugin
                    .get()
                    .pluginId,
            )
            pluginManager.apply(
                libs.plugins.roomPlugin
                    .get()
                    .pluginId,
            )
            """.trimIndent(),
        ).addCode("\n")
        .build()

private fun buildConfigureRoomSettings(): FunSpec =
    FunSpec
        .builder("configureRoomSettings")
        .addModifiers(KModifier.PRIVATE)
        .receiver(ClassName("org.gradle.api", "Project"))
        .addCode(
            $$"""
            extensions.configure<%T> {
                arg("room.generateKotlin", "true")
            }
            extensions.configure<%T> {
                schemaDirectory("$projectDir/schemas")
            }
            """.trimIndent(),
            ClassName("com.google.devtools.ksp.gradle", "KspExtension"),
            ClassName("androidx.room.gradle", "RoomExtension"),
        ).build()

private fun buildConfigureKmpDependencies(
    extensionClass: ClassName,
    kmpExtension: ClassName,
): FunSpec =
    FunSpec
        .builder("configureKmpDependencies")
        .addModifiers(KModifier.PRIVATE)
        .receiver(ClassName("org.gradle.api", "Project"))
        .addParameter("dataExtension", extensionClass)
        .beginControlFlow("extensions.configure<%T>", kmpExtension)
        .beginControlFlow("sourceSets.named(%S)", "commonMain")
        .beginControlFlow("dependencies")
        .addStatement("implementation(libs.bundles.kotlinxEssentialsBundle)")
        .beginControlFlow("if (dataExtension.useRoom.get())")
        .addStatement("implementation(libs.bundles.roomCommonBundle)")
        .endControlFlow()
        .beginControlFlow("if (dataExtension.useDatastore.get())")
        .addStatement("implementation(libs.bundles.datastoreBundle)")
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .endControlFlow()
        .build()
