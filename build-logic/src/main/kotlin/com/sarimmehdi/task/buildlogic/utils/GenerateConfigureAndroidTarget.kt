package com.sarimmehdi.task.buildlogic.utils

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import org.gradle.api.file.DirectoryProperty
import java.io.File

internal fun generateConfigureAndroidTarget(
    pkg: String,
    outputDir: DirectoryProperty
) {
    val projectClass = ClassName("org.gradle.api", "Project")
    val extensionAwareClass = ClassName("org.gradle.api.plugins", "ExtensionAware")
    val kmpExtensionClass = ClassName("org.jetbrains.kotlin.gradle.dsl", "KotlinMultiplatformExtension")
    val androidLibExtensionClass = ClassName("com.android.build.api.dsl", "KotlinMultiplatformAndroidLibraryExtension")
    val configClass = ClassName("$pkg.utils", "Config")

    val configureFunction = FunSpec.builder("configureAndroidTarget")
        .receiver(kmpExtensionClass)
        .addModifiers(KModifier.INTERNAL)
        .addParameter("target", projectClass)
        .addCode(
            $$"""
            val libraryConfig = %T.Library()
            (this as %T).extensions.configure<%T>("android") {
                val formattedPath = target.path.removePrefix(":").replace(":", ".")
                namespace = "${libraryConfig.namespace}.$formattedPath"
                compileSdk = libraryConfig.compileSdk
                minSdk = libraryConfig.minSdk

                withDeviceTestBuilder { sourceSetTreeName = libraryConfig.sourceSetTreeName }
                    .configure { instrumentationRunner = libraryConfig.instrumentationRunner }

                androidResources { enable = libraryConfig.enableAndroidResources }
            }
            """.trimIndent(),
            configClass,
            extensionAwareClass,
            androidLibExtensionClass
        )
        .build()

    val fileSpec = FileSpec.builder("$pkg.utils", "ConfigureAndroidTarget")
        .addFunction(configureFunction)
        .build()

    val targetRoot = File(outputDir.get().asFile, "convention/src/main/kotlin")
    fileSpec.writeTo(targetRoot)
}