package com.sarimmehdi.task.buildlogic.utils

import com.sarimmehdi.task.buildlogic.GenerateBuildLogicTask.ParameterData
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.file.DirectoryProperty
import java.io.File

internal fun generateUtilsConfig(
    pkg: String,
    namespace: String,
    outputDir: DirectoryProperty,
) {
    val configInterface =
        TypeSpec
            .interfaceBuilder("Config")
            .addModifiers(KModifier.INTERNAL, KModifier.SEALED)
            .addType(buildDetektConfig(pkg))
            .addType(buildKtlintConfig(pkg))
            .addType(buildKmpConfig(pkg, namespace))
            .addType(buildLibraryConfig(pkg, namespace))
            .build()

    FileSpec
        .builder("$pkg.utils", "Config")
        .addType(configInterface)
        .indent("    ")
        .addKotlinDefaultImports(includeJvm = false, includeJs = false)
        .build()
        .writeTo(File(outputDir.get().asFile, "convention/src/main/kotlin"))
}

private fun buildDetektConfig(pkg: String): TypeSpec {
    val stringType = ClassName("kotlin", "String")
    val booleanType = ClassName("kotlin", "Boolean")
    val listType =
        ClassName("kotlin.collections", "List")
            .parameterizedBy(stringType)

    val listTemplate = "listOf(\n⇥%S,\n%S,\n⇤)"

    return generateDataClass(
        "Detekt",
        pkg,
        listOf(
            ParameterData(
                "configFilePath",
                stringType,
                "%S",
                listOf("/config/detekt/detekt.yml"),
            ),
            ParameterData(
                "buildUponDefaultConfig",
                booleanType,
                "true",
            ),
            ParameterData(
                "allRules",
                booleanType,
                "false",
            ),
            ParameterData(
                "inclusions",
                listType,
                listTemplate,
                listOf("**/*.kt", "**/*.kts"),
            ),
            ParameterData(
                "exclusions",
                listType,
                listTemplate,
                listOf("**/resources/**", "**/build/**"),
            ),
        ),
    )
}

private fun buildKtlintConfig(pkg: String): TypeSpec =
    generateDataClass(
        "Ktlint",
        pkg,
        listOf(
            ParameterData(
                "version",
                ClassName("kotlin", "String"),
                "%S",
                listOf("1.8.0"),
            ),
            ParameterData(
                "verbose",
                ClassName("kotlin", "Boolean"),
                "true",
            ),
            ParameterData(
                "outputToConsole",
                ClassName("kotlin", "Boolean"),
                "true",
            ),
        ),
    )

private fun buildKmpConfig(
    pkg: String,
    namespace: String,
): TypeSpec {
    val jvmTargetClass = ClassName("org.jetbrains.kotlin.gradle.dsl", "JvmTarget")
    return generateDataClass(
        "KotlinMultiplatform",
        pkg,
        listOf(
            ParameterData(
                "freeCompilerArgs",
                ClassName("kotlin.collections", "List")
                    .parameterizedBy(ClassName("kotlin", "String")),
                "listOf(%S)",
                listOf("-Xexpect-actual-classes"),
            ),
            ParameterData(
                "namespace",
                ClassName("kotlin", "String"),
                "%S",
                listOf("$namespace.common"),
            ),
            ParameterData(
                "compileSdk",
                ClassName("kotlin", "Int"),
                "36",
            ),
            ParameterData(
                "minSdk",
                ClassName("kotlin", "Int"),
                "24",
            ),
            ParameterData(
                "enableAndroidResources",
                ClassName("kotlin", "Boolean"),
                "true",
            ),
            ParameterData(
                "jvmTarget",
                jvmTargetClass,
                "%T.JVM_17",
                listOf(jvmTargetClass),
            ),
        ),
    )
}

private fun buildLibraryConfig(
    pkg: String,
    namespace: String,
): TypeSpec {
    val jvmTargetClass = ClassName("org.jetbrains.kotlin.gradle.dsl", "JvmTarget")
    return generateDataClass(
        "Library",
        pkg,
        listOf(
            ParameterData(
                "namespace",
                ClassName("kotlin", "String"),
                "%S",
                listOf(namespace),
            ),
            ParameterData(
                "compileSdk",
                ClassName("kotlin", "Int"),
                "36",
            ),
            ParameterData(
                "minSdk",
                ClassName("kotlin", "Int"),
                "24",
            ),
            ParameterData(
                "jvmToolChain",
                ClassName("kotlin", "Int"),
                "17",
            ),
            ParameterData(
                "sourceSetTreeName",
                ClassName("kotlin", "String"),
                "%S",
                listOf("test"),
            ),
            ParameterData(
                "instrumentationRunner",
                ClassName("kotlin", "String"),
                "%S",
                listOf("androidx.test.runner.AndroidJUnitRunner"),
            ),
            ParameterData(
                "enableAndroidResources",
                ClassName("kotlin", "Boolean"),
                "true",
            ),
            ParameterData(
                "jvmTarget",
                jvmTargetClass,
                "%T.JVM_17",
                listOf(jvmTargetClass),
            ),
        ),
    )
}
