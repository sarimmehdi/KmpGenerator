package com.sarimmehdi.task.buildlogic.utils

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import org.gradle.api.file.DirectoryProperty
import java.io.File

internal fun generateProjectExts(
    pkg: String,
    outputDir: DirectoryProperty
) {
    val projectClass = ClassName("org.gradle.api", "Project")
    val pluginManagerClass = ClassName("org.gradle.api.plugins", "PluginManager")
    val libsClass = ClassName("org.gradle.accessors.dm", "LibrariesForLibs")
    val providerClass = ClassName("org.gradle.api.provider", "Provider")
    val providerConvertibleClass = ClassName("org.gradle.api.provider", "ProviderConvertible")
    val pluginDependencyClass = ClassName("org.gradle.plugin.use", "PluginDependency")

    val libsProperty = PropertySpec.builder("libs", libsClass)
        .receiver(projectClass)
        .addModifiers(KModifier.INTERNAL)
        .getter(
            FunSpec.getterBuilder()
                .addStatement("return the()")
                .build()
        )
        .build()

    val aliasProvider = FunSpec.builder("alias")
        .receiver(pluginManagerClass)
        .addModifiers(KModifier.INTERNAL)
        .addParameter("notation", providerClass.parameterizedBy(pluginDependencyClass))
        .addStatement("apply(notation.get().pluginId)")
        .build()

    val aliasConvertible = FunSpec.builder("alias")
        .receiver(pluginManagerClass)
        .addModifiers(KModifier.INTERNAL)
        .addParameter("notation", providerConvertibleClass.parameterizedBy(pluginDependencyClass))
        .addStatement("apply(notation.asProvider().get().pluginId)")
        .build()

    val fileSpec = FileSpec.builder("$pkg.utils", "ProjectExts")
        .addProperty(libsProperty)
        .addFunction(aliasProvider)
        .addFunction(aliasConvertible)
        .addImport("org.gradle.kotlin.dsl", "the")
        .build()

    val targetRoot = File(outputDir.get().asFile, "convention/src/main/kotlin")
    fileSpec.writeTo(targetRoot)
}