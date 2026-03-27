package com.sarimmehdi.model

import java.io.Serial
import java.io.Serializable

data class Bundle(
    val bundleName: String,
    val libraries: List<String>,
) : Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 839686524069068671L
    }
}
