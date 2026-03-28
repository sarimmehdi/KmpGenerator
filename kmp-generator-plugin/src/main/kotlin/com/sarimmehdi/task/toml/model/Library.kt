package com.sarimmehdi.task.toml.model

import java.io.Serial
import java.io.Serializable

data class Library(
    val versionName: String,
    val versionValue: String,
    val libraryName: String,
    val group: String,
    val name: String,
) : Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -5289331450276759514L
    }
}
