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

internal fun generateKmpDomainPlugin(
    pkg: String,
    outputDir: DirectoryProperty,
) {
    val projectClass = ClassName("org.gradle.api", "Project")
    val pluginClass = ClassName("org.gradle.api", "Plugin").parameterizedBy(projectClass)
    val kmpExtensionClass = ClassName("org.jetbrains.kotlin.gradle.dsl", "KotlinMultiplatformExtension")

    val applyBody =
        CodeBlock
            .builder()
            .beginControlFlow("with(target)")
            .beginControlFlow("pluginManager.apply")
            .add(pluginApplyBlock("kotlinMultiplatformPlugin"))
            .add(pluginApplyBlock("detektPlugin"))
            .add(pluginApplyBlock("ktlintPlugin"))
            .endControlFlow()
            .addStatement("configureQualityTools()")
            .beginControlFlow("extensions.configure<%T>", kmpExtensionClass)
            .addStatement("jvm()")
            .addStatement("sourceSets.commonMain.dependencies { implementation(libs.bundles.kotlinxEssentialsBundle) }")
            .addStatement("sourceSets.commonTest.dependencies { implementation(libs.bundles.unitTestingBundle) }")
            .endControlFlow()
            .endControlFlow()
            .build()

    val pluginType =
        TypeSpec
            .classBuilder("KmpDomainPlugin")
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(pluginClass)
            .addFunction(
                FunSpec
                    .builder("apply")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("target", projectClass)
                    .addCode(applyBody)
                    .build(),
            ).build()

    FileSpec
        .builder(pkg, "KmpDomainPlugin")
        .addType(pluginType)
        .indent("    ")
        .addImport("org.gradle.kotlin.dsl", "configure")
        .addImport("$pkg.utils", "libs", "configureQualityTools")
        .addKotlinDefaultImports(includeJvm = false, includeJs = false)
        .build()
        .writeTo(File(outputDir.get().asFile, "convention/src/main/kotlin"))
}

private fun pluginApplyBlock(pluginName: String): CodeBlock =
    CodeBlock.of(
        "apply(⇥\nlibs.plugins.$pluginName⇥\n.get()⇥\n.pluginId,⇤⇤⇤\n)\n",
    )
