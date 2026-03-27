package com.sarimmehdi.task.buildlogic.utils

import com.sarimmehdi.task.buildlogic.GenerateBuildLogicTask.ParameterData
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import kotlin.collections.forEach

internal fun generateDataClass(
    className: String,
    pkg: String,
    params: List<ParameterData>,
): TypeSpec {
    val constructor = FunSpec.constructorBuilder()
    val properties = mutableListOf<PropertySpec>()

    params.forEach { param ->
        val defaultValue =
            CodeBlock
                .builder()
                .add(param.defaultValueFormat, args = param.args.toTypedArray())
                .build()

        constructor.addParameter(
            ParameterSpec
                .builder(param.name, param.type)
                .defaultValue(defaultValue)
                .build(),
        )

        properties.add(
            PropertySpec
                .builder(param.name, param.type)
                .initializer(param.name)
                .build(),
        )
    }

    return TypeSpec
        .classBuilder(className)
        .addModifiers(KModifier.DATA)
        .addSuperinterface(ClassName("$pkg.utils", "Config"))
        .primaryConstructor(constructor.build())
        .addProperties(properties)
        .build()
}
