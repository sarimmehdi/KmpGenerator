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

internal fun generateKmpPlugin(
    pkg: String,
    outputDir: DirectoryProperty,
) {
    val projectClass = ClassName("org.gradle.api", "Project")
    val pluginClass = ClassName("org.gradle.api", "Plugin").parameterizedBy(projectClass)
    val configClass = ClassName("$pkg.utils", "Config")
    val kmpConfigClass = configClass.nestedClass("KotlinMultiplatform")

    val pluginType =
        TypeSpec
            .classBuilder("KmpPlugin")
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(pluginClass)
            .addFunction(buildApplyFunction(kmpConfigClass))
            .addFunction(buildApplyPlugins())
            .addFunction(buildConfigureKotlinOptions(kmpConfigClass))
            .addFunction(buildConfigureKmpAndroidLibrary(kmpConfigClass))
            .addFunction(buildConfigureRoom())
            .addFunction(buildConfigureDependencies())
            .build()

    FileSpec
        .builder(pkg, "KmpPlugin")
        .addType(pluginType)
        .indent("    ")
        .addImport("org.gradle.kotlin.dsl", "configure", "dependencies")
        .addImport("org.gradle.api.plugins", "ExtensionAware")
        .addImport("$pkg.utils", "libs", "configureQualityTools")
        .addKotlinDefaultImports(includeJvm = false, includeJs = false)
        .build()
        .writeTo(File(outputDir.get().asFile, "convention/src/main/kotlin"))
}

private fun buildApplyFunction(configClass: ClassName) =
    FunSpec
        .builder("apply")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("target", ClassName("org.gradle.api", "Project"))
        .addCode(
            """
            val kotlinMultiplatformConfig = %T()

            with(target) {
                applyPlugins()
                configureQualityTools()

                pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
                    configureKotlinOptions(kotlinMultiplatformConfig)
                    configureKmpAndroidLibrary(kotlinMultiplatformConfig)
                    configureRoom()
                    configureDependencies()
                }
            }
            """.trimIndent(),
            configClass,
        ).build()

private fun buildApplyPlugins() =
    FunSpec
        .builder("applyPlugins")
        .addModifiers(KModifier.PRIVATE)
        .receiver(ClassName("org.gradle.api", "Project"))
        .addCode(
            CodeBlock
                .builder()
                .add(pluginApply("kotlinMultiplatformPlugin"))
                .add(pluginApply("composeMultiplatformPlugin"))
                .add(pluginApply("composeCompilerPlugin"))
                .add(pluginApply("composeHotReloadPlugin"))
                .add(pluginApply("roomPlugin"))
                .add(pluginApply("androidLintPlugin"))
                .add(pluginApply("kspPlugin"))
                .add(pluginApply("detektPlugin"))
                .add(pluginApply("ktlintPlugin"))
                .addStatement("pluginManager.apply(%S)", "com.android.kotlin.multiplatform.library")
                .build(),
        ).build()

private fun pluginApply(plugin: String) =
    CodeBlock.of(
        "pluginManager.apply(⇥\nlibs.plugins.$plugin⇥\n.get()⇥\n.pluginId,⇤⇤⇤\n)\n",
    )

private fun buildConfigureKotlinOptions(configClass: ClassName) =
    FunSpec
        .builder("configureKotlinOptions")
        .addModifiers(KModifier.PRIVATE)
        .receiver(ClassName("org.gradle.api", "Project"))
        .addParameter("config", configClass)
        .beginControlFlow(
            "tasks.withType(%T::class.java).configureEach",
            ClassName("org.jetbrains.kotlin.gradle.tasks", "KotlinCompile"),
        ).addStatement("compilerOptions.freeCompilerArgs.addAll(config.freeCompilerArgs)")
        .addStatement("compilerOptions.jvmTarget.set(config.jvmTarget)")
        .endControlFlow()
        .build()

private fun buildConfigureKmpAndroidLibrary(configClass: ClassName) =
    FunSpec
        .builder("configureKmpAndroidLibrary")
        .addModifiers(KModifier.PRIVATE)
        .receiver(ClassName("org.gradle.api", "Project"))
        .addParameter("config", configClass)
        .beginControlFlow(
            "extensions.configure<%T>",
            ClassName(
                "org.jetbrains.kotlin.gradle.dsl",
                "KotlinMultiplatformExtension",
            ),
        ).addCode(
            """
            (this as ExtensionAware)
                .extensions
                .configure<%T>("android") {
                    namespace = config.namespace
                    compileSdk = config.compileSdk
                    minSdk = config.minSdk
                    androidResources { enable = config.enableAndroidResources }
                }

            """.trimIndent(),
            ClassName("com.android.build.api.dsl", "KotlinMultiplatformAndroidLibraryExtension"),
        ).beginControlFlow("sourceSets.apply")
        .addCode(buildSourceSetsBlock())
        .endControlFlow()
        .endControlFlow()
        .build()

private fun buildSourceSetsBlock() =
    """
    commonMain.dependencies {
        implementation(libs.bundles.composeCoreBundle)
        implementation(libs.bundles.androidxLifecycleBundle)
        implementation(libs.bundles.koinCommonBundle)
        implementation(libs.bundles.kotlinxEssentialsBundle)
        implementation(libs.bundles.roomCommonBundle)
        implementation(libs.bundles.datastoreBundle)
        implementation(libs.composeUiToolingPreviewLibrary)
    }
    androidMain.dependencies { implementation(libs.bundles.androidUiSupportBundle) }
    commonTest.dependencies { implementation(libs.bundles.unitTestingBundle) }
    
    """.trimIndent()

private fun buildConfigureRoom() =
    FunSpec
        .builder("configureRoom")
        .addModifiers(KModifier.PRIVATE)
        .receiver(ClassName("org.gradle.api", "Project"))
        .beginControlFlow("extensions.configure<%T>", ClassName("androidx.room.gradle", "RoomExtension"))
        .addStatement("schemaDirectory(\"\$projectDir/schemas\")")
        .endControlFlow()
        .build()

private fun buildConfigureDependencies() =
    FunSpec
        .builder("configureDependencies")
        .addModifiers(KModifier.PRIVATE)
        .receiver(ClassName("org.gradle.api", "Project"))
        .beginControlFlow("dependencies")
        .addStatement("%S(libs.composeUiToolingLibrary)", "androidRuntimeClasspath")
        .addStatement("add(%S, libs.roomCompilerLibrary)", "kspAndroid")
        .endControlFlow()
        .build()
