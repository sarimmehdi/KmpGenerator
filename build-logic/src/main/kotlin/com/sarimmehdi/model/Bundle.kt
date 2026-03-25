package com.sarimmehdi.model

import java.io.Serializable

data class Bundle(
    val bundleName: String,
    val libraries: List<String>
): Serializable
