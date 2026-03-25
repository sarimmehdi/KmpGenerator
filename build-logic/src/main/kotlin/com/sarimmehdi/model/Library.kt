package com.sarimmehdi.model

import java.io.Serializable

data class Library(
    val versionName: String,
    val versionValue: String,
    val libraryName: String,
    val group: String,
    val name: String
): Serializable
