package com.sarimmehdi.task.domain.utils

import com.sarimmehdi.task.domain.model.DomainModel
import com.sarimmehdi.task.domain.model.UseCaseModel
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

internal fun generateUseCaseFiles(
    useCases: List<UseCaseModel>,
    models: List<DomainModel>,
    basePackage: String,
    outputDir: File,
) {
    if (useCases.isEmpty()) return

    useCases.forEach { uc ->
        validateUseCaseDependencies(uc, models)

        val ucBuilder = TypeSpec.classBuilder("${uc.name}UseCase")
        val constructor = FunSpec.constructorBuilder()

        addLocalRepositories(uc, models, basePackage, ucBuilder, constructor)
        addExternalDependencies(uc, ucBuilder, constructor)

        ucBuilder.primaryConstructor(constructor.build())

        FileSpec
            .builder("$basePackage.usecase", "${uc.name}UseCase")
            .addType(ucBuilder.build())
            .build()
            .writeTo(outputDir)
    }
}

private fun validateUseCaseDependencies(
    uc: UseCaseModel,
    models: List<DomainModel>,
) {
    uc.repositoryDependencies.forEach { repoName ->
        val model = models.filterIsInstance<DomainModel.DataClass>().find { it.name == repoName }
        if (model == null || !model.createRepository) {
            error(
                "UseCase '${uc.name}' depends on '$repoName', but '$repoName' " +
                    "does not have 'createRepository = true' set in its definition.",
            )
        }
    }
}

private fun addLocalRepositories(
    uc: UseCaseModel,
    models: List<DomainModel>,
    basePackage: String,
    ucBuilder: TypeSpec.Builder,
    constructor: FunSpec.Builder,
) {
    uc.repositoryDependencies.forEach { repoName ->
        val repoClassName = ClassName("$basePackage.repository", "${repoName}Repository")
        val propName = "${repoName.replaceFirstChar { it.lowercase() }}Repository"

        constructor.addParameter(propName, repoClassName)
        ucBuilder.addProperty(
            PropertySpec
                .builder(propName, repoClassName)
                .initializer(propName)
                .addModifiers(KModifier.PRIVATE)
                .build(),
        )

        if (uc.isVanilla) {
            addVanillaMethods(repoName, models, basePackage, propName, ucBuilder)
        }
    }
}

private fun addVanillaMethods(
    repoName: String,
    models: List<DomainModel>,
    basePackage: String,
    propName: String,
    ucBuilder: TypeSpec.Builder,
) {
    val model = models.filterIsInstance<DomainModel.DataClass>().first { it.name == repoName }
    val modelPackage = "$basePackage.model"
    val flowClass = ClassName("kotlinx.coroutines.flow", "Flow")
    val modelClass = ClassName(modelPackage, model.name)
    val flowPropertyName = model.name.replaceFirstChar { it.lowercase() }

    ucBuilder.addProperty(
        PropertySpec
            .builder(flowPropertyName, flowClass.parameterizedBy(modelClass))
            .initializer("$propName.$flowPropertyName")
            .build(),
    )

    model.properties.forEach { (pName, pType) ->
        val typeName = mapToTypeName(pType, modelPackage)
        val methodName = "update${pName.replaceFirstChar { it.uppercase() }}"
        ucBuilder.addFunction(
            FunSpec
                .builder(methodName)
                .addModifiers(KModifier.SUSPEND)
                .addParameter(pName, typeName)
                .addStatement("return $propName.$methodName($pName)")
                .build(),
        )
    }
}

private fun addExternalDependencies(
    uc: UseCaseModel,
    ucBuilder: TypeSpec.Builder,
    constructor: FunSpec.Builder,
) {
    uc.externalDependencies.forEach { ext ->
        val subPackage = if (ext.name.endsWith("Repository")) "repository" else "usecase"
        val extPackage = "${ext.namespace}.${ext.featureName}.domain.$subPackage"
        val extClassName = ClassName(extPackage, ext.name)
        val propName = ext.name.replaceFirstChar { it.lowercase() }

        constructor.addParameter(propName, extClassName)
        ucBuilder.addProperty(
            PropertySpec
                .builder(propName, extClassName)
                .initializer(propName)
                .addModifiers(KModifier.PRIVATE)
                .build(),
        )
    }
}
