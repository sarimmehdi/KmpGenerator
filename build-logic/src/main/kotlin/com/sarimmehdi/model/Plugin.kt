package com.sarimmehdi.model

import java.io.Serializable

data class Plugin(
    val versionName: String,
    val versionValue: String,
    val pluginName: String,
    val id: String
): Serializable
