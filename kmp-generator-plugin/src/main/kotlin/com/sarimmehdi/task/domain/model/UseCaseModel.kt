package com.sarimmehdi.task.domain.model

import java.io.Serial
import java.io.Serializable

data class UseCaseModel(
    val name: String,
    val repositoryDependencies: List<String> = emptyList(),
    val externalDependencies: List<ExternalDependency> = emptyList(),
    val isVanilla: Boolean = true,
) : Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 8918023779677850523L
    }
}
