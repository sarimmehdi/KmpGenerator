package com.sarimmehdi.task.utils

fun validatePackageName(pkg: String) {
    val regex = "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$".toRegex()

    require(regex.matches(pkg)) {
        "Invalid package name: '$pkg'. It must follow standard Java/Kotlin naming conventions."
    }
}
