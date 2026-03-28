package com.sarimmehdi.task.domain.utils

import com.sarimmehdi.task.domain.model.KotlinType
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName

internal fun mapToTypeName(
    type: KotlinType,
    currentPackage: String,
): TypeName {
    val baseType: TypeName =
        when (type) {
            is KotlinType.KotlinString -> STRING
            is KotlinType.KotlinInt -> INT
            is KotlinType.KotlinLong -> LONG
            is KotlinType.KotlinDouble -> DOUBLE
            is KotlinType.KotlinFloat -> FLOAT
            is KotlinType.KotlinBoolean -> BOOLEAN
            is KotlinType.KotlinList -> LIST.parameterizedBy(mapToTypeName(type.itemType, currentPackage))
            is KotlinType.KotlinSet -> SET.parameterizedBy(mapToTypeName(type.itemType, currentPackage))
            is KotlinType.KotlinMap ->
                MAP.parameterizedBy(
                    mapToTypeName(type.keyType, currentPackage),
                    mapToTypeName(type.valueType, currentPackage),
                )
            is KotlinType.Custom -> ClassName(type.packageName ?: currentPackage, type.name)
        }

    return baseType.copy(nullable = type.isNullable)
}
