package com.sarimmehdi.task.domain.utils

import com.sarimmehdi.task.domain.model.DomainModel
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

internal fun generateModelFiles(
    modelFiles: List<DomainModel>,
    basePackage: String,
    outputDir: File,
) {
    if (modelFiles.isEmpty()) return
    val modelPackage = "$basePackage.model"

    modelFiles.forEach { model ->
        val typeSpec = buildModelTypeSpec(model, modelPackage)

        FileSpec
            .builder(modelPackage, model.name)
            .addType(typeSpec)
            .build()
            .writeTo(outputDir)
    }
}

private fun buildModelTypeSpec(
    model: DomainModel,
    modelPackage: String,
): TypeSpec =
    when (model) {
        is DomainModel.DataClass -> buildDataClass(model, modelPackage)
        is DomainModel.EnumClass -> buildEnumClass(model)
    }

private fun buildDataClass(
    model: DomainModel.DataClass,
    modelPackage: String,
): TypeSpec {
    val constructor = FunSpec.constructorBuilder()
    val properties =
        model.properties.map { (name, type) ->
            val typeName = mapToTypeName(type, modelPackage)
            constructor.addParameter(name, typeName)
            PropertySpec
                .builder(name, typeName)
                .initializer(name)
                .build()
        }

    return TypeSpec
        .classBuilder(model.name)
        .addModifiers(KModifier.DATA)
        .primaryConstructor(constructor.build())
        .addProperties(properties)
        .build()
}

private fun buildEnumClass(model: DomainModel.EnumClass): TypeSpec =
    TypeSpec
        .enumBuilder(model.name)
        .apply {
            model.constants.forEach { addEnumConstant(it) }
        }.build()
