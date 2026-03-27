package com.sarimmehdi.task

import com.sarimmehdi.task.buildlogic.GenerateBuildLogicTask
import com.sarimmehdi.task.toml.StarterDataReporter
import com.sarimmehdi.task.toml.TomlGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

abstract class KmpHelpReporter : DefaultTask() {
    @TaskAction
    fun printHelp() {
        println(messageToPrint())
    }

    companion object {
        const val TASK_NAME = "helpKmpGenerator"
        const val TASK_DESCRIPTION = "Displays the help guide for using KmpGenerator."

        private const val REPEAT_COUNTER = 50

        fun register(
            project: Project,
            taskGroup: String,
        ) {
            project.tasks.register(TASK_NAME, KmpHelpReporter::class.java) {
                group = taskGroup
                description = TASK_DESCRIPTION
            }
        }

        fun messageToPrint(): String {
            val horizontalRule = "=".repeat(REPEAT_COUNTER)

            return """
        |
        |$horizontalRule
        | KMP GENERATOR: USAGE GUIDE
        |$horizontalRule
        |
        |[AVAILABLE TASKS]
        |
        |> Task: ${TomlGenerator.TASK_NAME}
        |  Description: ${TomlGenerator.TASK_DESCRIPTION}
        |  Usage:       ${TomlGenerator.TASK_EXAMPLE}
        |
        |> Task: ${StarterDataReporter.TASK_NAME}
        |  Description: ${StarterDataReporter.TASK_DESCRIPTION}
        |  Usage:       ${StarterDataReporter.TASK_EXAMPLE}
        |  
        |> Task: ${GenerateBuildLogicTask.TASK_NAME}
        |  Description: ${GenerateBuildLogicTask.TASK_DESCRIPTION}
        |  Usage:       ${GenerateBuildLogicTask.TASK_EXAMPLE}
        |
        |[CONFIGURATION EXAMPLES]
        |Add to your build.gradle.kts:
        |
        |
        |$horizontalRule
                """.trimMargin()
        }
    }
}
