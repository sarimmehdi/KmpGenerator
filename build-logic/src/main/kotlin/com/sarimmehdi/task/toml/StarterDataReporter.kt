package com.sarimmehdi.task.toml

import com.sarimmehdi.StarterData
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

abstract class StarterDataReporter : DefaultTask() {

    @TaskAction
    fun printAll() {
        println("\n" + "=".repeat(40))
        println(" KMP GENERATOR: STARTER DATA INVENTORY")
        println("=".repeat(40))

        printSection("LIBRARIES") {
            StarterData.libraries.forEach { lib ->
                println("  - ${lib.libraryName.padEnd(35)} [${lib.group}:${lib.name}] ref: ${lib.versionName}")
            }
        }

        printSection("PLUGINS") {
            StarterData.plugins.forEach { plugin ->
                println("  - ${plugin.pluginName.padEnd(35)} [id: ${plugin.id}] ref: ${plugin.versionName}")
            }
        }

        printSection("BUNDLES") {
            StarterData.bundles.forEach { bundle ->
                println("  - ${bundle.bundleName.padEnd(25)} -> ${bundle.libraries.joinToString(", ")}")
            }
        }

        printSection("VERSIONS") {
            val uniqueVersions = (StarterData.libraries.map { it.versionName to it.versionValue } +
                    StarterData.plugins.map { it.versionName to it.versionValue })
                .distinctBy { it.first }
                .sortedBy { it.first }

            uniqueVersions.forEach { (name, value) ->
                println("  - ${name.padEnd(35)} = $value")
            }
        }

        println("\n" + "=".repeat(40))
    }

    private fun printSection(title: String, block: () -> Unit) {
        println("\n[$title]")
        block()
    }

    companion object {
        const val TASK_NAME = "listStarterData"
        const val TASK_DESCRIPTION = "Inventory Check: Lists every library, plugin, and bundle currently hardcoded in StarterData."
        const val TASK_EXAMPLE = "./gradlew $TASK_NAME"
        fun register(
            project: Project,
            taskGroup: String,
        ) {
            project.tasks.register(TASK_NAME, StarterDataReporter::class.java) {
                group = taskGroup
                description = TASK_DESCRIPTION
            }
        }
    }
}