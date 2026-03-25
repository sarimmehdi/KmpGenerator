package com.sarimmehdi.task.buildlogic.utils

import com.sarimmehdi.task.buildlogic.GenerateBuildLogicTask.ParameterData
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import org.gradle.api.file.DirectoryProperty
import java.io.File

internal fun generateUtilsConfig(
    pkg: String,
    outputDir: DirectoryProperty
) {
    val jvmTargetClass = ClassName("org.jetbrains.kotlin.gradle.dsl", "JvmTarget")
    val stringList = List::class.parameterizedBy(String::class)

    val configInterface = TypeSpec.interfaceBuilder("Config")
        .addModifiers(KModifier.INTERNAL, KModifier.SEALED)
        .addType(generateDataClass("Detekt", pkg, listOf(
            ParameterData("configFilePath", String::class.asTypeName(), "%S", listOf("/config/detekt/detekt.yml")),
            ParameterData("buildUponDefaultConfig", Boolean::class.asTypeName(), "true"),
            ParameterData("allRules", Boolean::class.asTypeName(), "false"),
            ParameterData("inclusions", stringList, "listOf(%S, %S)", listOf("**/*.kt", "**/*.kts")),
            ParameterData("exclusions", stringList, "listOf(%S, %S)", listOf("**/resources/**", "**/build/**"))
        )))
        .addType(generateDataClass("Ktlint", pkg, listOf(
            ParameterData("version", String::class.asTypeName(), "%S", listOf("1.8.0")),
            ParameterData("verbose", Boolean::class.asTypeName(), "true"),
            ParameterData("outputToConsole", Boolean::class.asTypeName(), "true")
        )))
        .addType(generateDataClass("KotlinMultiplatform", pkg, listOf(
            ParameterData("freeCompilerArgs", stringList, "listOf(%S)", listOf("-Xexpect-actual-classes")),
            ParameterData("namespace", String::class.asTypeName(), "%S", listOf("$pkg.common")),
            ParameterData("compileSdk", Int::class.asTypeName(), "36"),
            ParameterData("minSdk", Int::class.asTypeName(), "24"),
            ParameterData("enableAndroidResources", Boolean::class.asTypeName(), "true"),
            ParameterData("jvmTarget", jvmTargetClass, "%T.JVM_17", listOf(jvmTargetClass))
        )))
        .addType(generateDataClass("Library", pkg, listOf(
            ParameterData("namespace", String::class.asTypeName(), "%S", listOf(pkg)),
            ParameterData("compileSdk", Int::class.asTypeName(), "36"),
            ParameterData("minSdk", Int::class.asTypeName(), "24"),
            ParameterData("jvmToolChain", Int::class.asTypeName(), "17"),
            ParameterData("sourceSetTreeName", String::class.asTypeName(), "%S", listOf("test")),
            ParameterData("instrumentationRunner", String::class.asTypeName(), "%S", listOf("androidx.test.runner.AndroidJUnitRunner")),
            ParameterData("enableAndroidResources", Boolean::class.asTypeName(), "true"),
            ParameterData("jvmTarget", jvmTargetClass, "%T.JVM_17", listOf(jvmTargetClass))
        )))
        .build()

    FileSpec.builder("$pkg.utils", "Config")
        .addType(configInterface)
        .build()
        .writeTo(File(outputDir.get().asFile, "convention/src/main/kotlin"))
}