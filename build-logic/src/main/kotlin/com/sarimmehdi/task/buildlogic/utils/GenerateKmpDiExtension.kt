package com.sarimmehdi.task.buildlogic.utils

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.file.DirectoryProperty
import java.io.File

internal fun generateKmpDiExtension(
    pkg: String,
    outputDir: DirectoryProperty
) {
    val useRoomProperty = PropertySpec.builder("useRoom", Boolean::class)
        .mutable(true)
        .initializer("false")
        .build()

    val useDatastoreProperty = PropertySpec.builder("useDatastore", Boolean::class)
        .mutable(true)
        .initializer("false")
        .build()

    val extensionClass = TypeSpec.classBuilder("KmpDiExtension")
        .addModifiers(KModifier.OPEN)
        .addProperty(useRoomProperty)
        .addProperty(useDatastoreProperty)
        .build()

    val fileSpec = FileSpec.builder("$pkg.utils", "KmpDiExtension")
        .addType(extensionClass)
        .build()

    val targetRoot = File(outputDir.get().asFile, "convention/src/main/kotlin")
    fileSpec.writeTo(targetRoot)
}