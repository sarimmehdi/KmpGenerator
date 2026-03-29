package com.sarimmehdi.task.buildlogic.utils

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import java.io.File

internal fun generateKmpDataExtension(
    pkg: String,
    outputDir: DirectoryProperty,
) {
    val propertyType =
        Property::class
            .asClassName()
            .parameterizedBy(Boolean::class.asClassName())

    val useRoomProperty =
        PropertySpec
            .builder("useRoom", propertyType)
            .build()

    val useDatastoreProperty =
        PropertySpec
            .builder("useDatastore", propertyType)
            .build()

    val extensionInterface =
        TypeSpec
            .interfaceBuilder("KmpDataExtension")
            .addProperty(useRoomProperty)
            .addProperty(useDatastoreProperty)
            .build()

    val fileSpec =
        FileSpec
            .builder("$pkg.utils", "KmpDataExtension")
            .addType(extensionInterface)
            .build()

    val targetRoot = File(outputDir.get().asFile, "convention/src/main/kotlin")
    fileSpec.writeTo(targetRoot)
}
