package com.sarimmehdi.task.domain.model

import java.io.Serial
import java.io.Serializable

sealed interface DomainModel : Serializable {
    val name: String

    data class DataClass(
        override val name: String,
        val properties: Map<String, KotlinType>,
        val createRepository: Boolean = false,
    ) : DomainModel {
        companion object {
            @Serial
            private const val serialVersionUID: Long = 6475045820434042929L
        }
    }

    data class EnumClass(
        override val name: String,
        val constants: List<String>,
    ) : DomainModel {
        companion object {
            @Serial
            private const val serialVersionUID: Long = 8068851185297534952L
        }
    }
}
