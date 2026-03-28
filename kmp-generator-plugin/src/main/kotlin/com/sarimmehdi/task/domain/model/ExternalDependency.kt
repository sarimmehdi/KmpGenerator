package com.sarimmehdi.task.domain.model

import java.io.Serial
import java.io.Serializable

data class ExternalDependency(
    val name: String,
    val featureName: String,
    val namespace: String,
) : Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -1311315710479687604L
    }
}
