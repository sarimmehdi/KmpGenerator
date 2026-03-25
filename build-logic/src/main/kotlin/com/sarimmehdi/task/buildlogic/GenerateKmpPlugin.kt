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
    outputDir: DirectoryProperty
) {
    val projectClass = ClassName("org.gradle.api", "Project")
    val pluginClass = ClassName("org.gradle.api", "Plugin").parameterizedBy(projectClass)
    val kmpExtensionClass = ClassName("org.jetbrains.kotlin.gradle.dsl", "KotlinMultiplatformExtension")
    val kotlinCompileClass = ClassName("org.jetbrains.kotlin.gradle.tasks", "KotlinCompile")
    val kmpAndroidLibClass = ClassName("com.android.build.api.dsl", "KotlinMultiplatformAndroidLibraryExtension")
    val extensionAwareClass = ClassName("org.gradle.api.plugins", "ExtensionAware")
    val roomExtensionClass = ClassName("androidx.room.gradle", "RoomExtension")
    val configClass = ClassName("$pkg.utils", "Config")

    val applyBody = CodeBlock.builder()
        .addStatement("val kotlinMultiplatformConfig = %T.KotlinMultiplatform()", configClass)
        .beginControlFlow("with(target)")
        .beginControlFlow("pluginManager.apply")
        .addStatement("apply(libs.plugins.kotlinMultiplatformPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.composeMultiplatformPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.composeCompilerPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.composeHotReloadPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.roomPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.androidLintPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.kspPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.detektPlugin.get().pluginId)")
        .addStatement("apply(libs.plugins.ktlintPlugin.get().pluginId)")
        .addStatement("apply(%S)", "com.android.kotlin.multiplatform.library")
        .endControlFlow()

        .addStatement("configureQualityTools()")

        .beginControlFlow("pluginManager.withPlugin(%S)", "com.android.kotlin.multiplatform.library")
        .beginControlFlow("tasks.withType(%T::class.java).configureEach", kotlinCompileClass)
        .addStatement("compilerOptions.freeCompilerArgs.addAll(kotlinMultiplatformConfig.freeCompilerArgs)")
        .addStatement("compilerOptions.jvmTarget.set(kotlinMultiplatformConfig.jvmTarget)")
        .endControlFlow()

        .beginControlFlow("extensions.configure<%T>", kmpExtensionClass)
        .beginControlFlow("(this as %T).extensions.configure<%T>(%S)", extensionAwareClass, kmpAndroidLibClass, "android")
        .addStatement("namespace = kotlinMultiplatformConfig.namespace")
        .addStatement("compileSdk = kotlinMultiplatformConfig.compileSdk")
        .addStatement("minSdk = kotlinMultiplatformConfig.minSdk")
        .addStatement("androidResources { enable = kotlinMultiplatformConfig.enableAndroidResources }")
        .endControlFlow()

        .beginControlFlow("sourceSets.apply")
        .beginControlFlow("commonMain.dependencies")
        .addStatement("implementation(libs.bundles.composeCoreBundle)")
        .addStatement("implementation(libs.bundles.androidxLifecycleBundle)")
        .addStatement("implementation(libs.bundles.koinCommonBundle)")
        .addStatement("implementation(libs.bundles.kotlinxEssentialsBundle)")
        .addStatement("implementation(libs.bundles.roomCommonBundle)")
        .addStatement("implementation(libs.bundles.datastoreBundle)")
        .addStatement("implementation(libs.composeUiToolingPreviewLibrary)")
        .endControlFlow()
        .addStatement("androidMain.dependencies { implementation(libs.bundles.androidUiSupportBundle) }")
        .addStatement("commonTest.dependencies { implementation(libs.bundles.unitTestingBundle) }")
        .endControlFlow() // end sourceSets.apply
        .endControlFlow() // end extensions.configure<KmpExtension>

        .beginControlFlow("extensions.configure<%T>", roomExtensionClass)
        .addStatement("schemaDirectory(%S)", "${'$'}{projectDir}/schemas")
        .endControlFlow()

        .beginControlFlow("dependencies")
        .addStatement("%S(libs.composeUiToolingLibrary)", "androidRuntimeClasspath")
        .addStatement("add(%S, libs.roomCompilerLibrary)", "kspAndroid")
        .endControlFlow()

        .endControlFlow() // end withPlugin
        .endControlFlow() // end with(target)
        .build()

    val pluginType = TypeSpec.classBuilder("KmpPlugin")
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

    FileSpec.builder(pkg, "KmpPlugin")
        .addType(pluginType)
        .addImport("org.gradle.kotlin.dsl", "configure", "dependencies")
        .addImport("$pkg.utils", "libs", "configureQualityTools")
        .build()
        .writeTo(File(outputDir.get().asFile, "convention/src/main/kotlin"))
}