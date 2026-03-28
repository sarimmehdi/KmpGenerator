package com.sarimmehdi.task.toml.model

import java.io.Serial
import java.io.Serializable

data class Plugin(
    val versionName: String,
    val versionValue: String,
    val pluginName: String,
    val id: String,
) : Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -3748924948807032266L
    }
}
