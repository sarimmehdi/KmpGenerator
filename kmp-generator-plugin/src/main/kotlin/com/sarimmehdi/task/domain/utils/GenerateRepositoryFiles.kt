package com.sarimmehdi.task.domain.utils

import com.sarimmehdi.task.domain.model.DomainModel
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

internal fun generateRepositoryFiles(
    modelFiles: List<DomainModel>,
    basePackage: String,
    outputDir: File,
) {
    val dataClassesWithRepo =
        modelFiles
            .filterIsInstance<DomainModel.DataClass>()
            .filter { it.createRepository }

    if (dataClassesWithRepo.isEmpty()) return

    val repoPackage = "$basePackage.repository"
    val modelPackage = "$basePackage.model"

    dataClassesWithRepo.forEach { model ->
        val repoName = "${model.name}Repository"
        val modelClassName = ClassName(modelPackage, model.name)

        val flowClass = ClassName("kotlinx.coroutines.flow", "Flow")
        val modelFlow = flowClass.parameterizedBy(modelClassName)

        val flowPropertyName = model.name.replaceFirstChar { it.lowercase() }

        val repoInterface =
            TypeSpec
                .interfaceBuilder(repoName)
                .apply {
                    addProperty(
                        PropertySpec
                            .builder(flowPropertyName, modelFlow)
                            .build(),
                    )

                    model.properties.forEach { (propName, type) ->
                        val typeName = mapToTypeName(type, modelPackage)
                        val methodName = "update${propName.replaceFirstChar { it.uppercase() }}"

                        addFunction(
                            FunSpec
                                .builder(methodName)
                                .addModifiers(KModifier.SUSPEND)
                                .addParameter(propName, typeName)
                                .addModifiers(KModifier.ABSTRACT)
                                .build(),
                        )
                    }
                }.build()

        FileSpec
            .builder(repoPackage, repoName)
            .addType(repoInterface)
            .build()
            .writeTo(outputDir)
    }
}
