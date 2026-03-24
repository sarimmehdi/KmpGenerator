package com.sarimmehdi.task

object KmpHelpReporter {

    const val TASK_NAME = "helpKmpGenerator"
    const val TASK_DESCRIPTION = "Displays the help guide for using KmpGenerator."
    const val TASK_EXAMPLE = "./gradlew $TASK_NAME"

    fun printHelp() {
        println("\n" + "=".repeat(50))
        println(" KMP GENERATOR: USAGE GUIDE")
        println("=".repeat(50))

        println("\n[AVAILABLE TASKS]")

        printTask(
            TomlGenerator.TASK_NAME,
            TomlGenerator.TASK_DESCRIPTION,
            TomlGenerator.TASK_EXAMPLE
        )

        printTask(
            StarterDataReporter.TASK_NAME,
            StarterDataReporter.TASK_DESCRIPTION,
            StarterDataReporter.TASK_EXAMPLE
        )

        printTask(
            TASK_NAME,
            TASK_DESCRIPTION,
            TASK_EXAMPLE
        )

        println("\n[CONFIGURATION EXAMPLES]")
        println("Add to your build.gradle.kts:")
        println("""
    kmpGenerator {
        // Force overwrite instead of appending (Warning: Clears existing TOML)
        overwriteExisting.set(true)

        additionalLibraries.add(...)
    }
        """.trimIndent())

        println("\n" + "=".repeat(50))
    }

    private fun printTask(name: String, desc: String, example: String) {
        println("\n> Task: $name")
        println("  Description: $desc")
        println("  Usage:       $example")
    }
}