package com.sarimmehdi.task.toml

import com.sarimmehdi.model.Bundle
import com.sarimmehdi.model.Library
import com.sarimmehdi.model.Plugin
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

abstract class StarterDataReporter : DefaultTask() {
    @TaskAction
    fun printAll() {
        println("\n" + "=".repeat(REPEAT_COUNTER))
        println(" $MAIN_HEADER")
        println("=".repeat(REPEAT_COUNTER))

        reportStandardSections()
        reportProtectedSections()

        println("\n" + "=".repeat(REPEAT_COUNTER))
    }

    private fun reportStandardSections() {
        printSection(LIBRARIES_HEADER) {
            StarterData.libraries.forEach { println(formatLibrary(it)) }
        }
        printSection(PLUGINS_HEADER) {
            StarterData.plugins.forEach { println(formatPlugin(it)) }
        }
        printSection(BUNDLES_HEADER) {
            StarterData.bundles.forEach { println(formatBundle(it)) }
        }
        reportVersions()
    }

    private fun reportVersions() =
        printSection(VERSIONS_HEADER) {
            (
                StarterData.libraries.map { it.versionName to it.versionValue } +
                    StarterData.plugins.map { it.versionName to it.versionValue }
            ).distinctBy { it.first }
                .sortedBy { it.first }
                .forEach { (name, value) ->
                    println("  - ${name.padEnd(VERSIONS_PAD_COUNTER)} = $value")
                }
        }

    private fun reportProtectedSections() {
        printSection(PROTECTED_LIBRARIES_HEADER) {
            StarterData.libraries
                .filter { it.libraryName in PROTECTED_LIBRARY_NAMES }
                .forEach { println(formatLibrary(it)) }
        }
        printSection(PROTECTED_PLUGINS_HEADER) {
            StarterData.plugins
                .filter { it.pluginName in PROTECTED_PLUGIN_NAMES }
                .forEach { println(formatPlugin(it)) }
        }
        printSection(PROTECTED_BUNDLES_HEADER) {
            StarterData.bundles
                .filter { it.bundleName == PROTECTED_BUNDLE_NAME }
                .forEach { println(formatBundle(it)) }
        }
    }

    private fun formatLibrary(lib: Library): String =
        "  - ${lib.libraryName.padEnd(LIBRARIES_PAD_COUNTER)} [${lib.group}:${lib.name}] ref: ${lib.versionName}"

    private fun formatPlugin(plugin: Plugin): String =
        "  - ${plugin.pluginName.padEnd(PLUGINS_PAD_COUNTER)} [id: ${plugin.id}] ref: ${plugin.versionName}"

    private fun formatBundle(bundle: Bundle): String =
        "  - ${bundle.bundleName.padEnd(BUNDLES_PAD_COUNTER)} -> ${bundle.libraries.joinToString(", ")}"

    private fun printSection(
        title: String,
        block: () -> Unit,
    ) {
        println("\n[$title]")
        block()
    }

    companion object {
        const val TASK_NAME = "listStarterData"
        const val TASK_DESCRIPTION =
            "Inventory Check: Lists every library, plugin, " +
                "and bundle currently hardcoded in StarterData. It also lists protected versions," +
                " libraries, plugins, and bundles. These cannot be excluded!"
        const val TASK_EXAMPLE = "./gradlew $TASK_NAME"

        const val REPEAT_COUNTER = 40
        const val LIBRARIES_PAD_COUNTER = 35
        const val PLUGINS_PAD_COUNTER = 35
        const val BUNDLES_PAD_COUNTER = 25
        const val VERSIONS_PAD_COUNTER = 35

        const val MAIN_HEADER = "KMP GENERATOR: STARTER DATA INVENTORY"
        const val VERSIONS_HEADER = "VERSIONS"
        const val LIBRARIES_HEADER = "LIBRARIES"
        const val BUNDLES_HEADER = "BUNDLES"
        const val PLUGINS_HEADER = "PLUGINS"
        const val PROTECTED_LIBRARIES_HEADER = "PROTECTED_LIBRARIES"
        const val PROTECTED_BUNDLES_HEADER = "PROTECTED_BUNDLES"
        const val PROTECTED_PLUGINS_HEADER = "PROTECTED_PLUGINS"

        const val PROTECTED_BUNDLE_NAME = "gradlePluginBundle"
        val PROTECTED_LIBRARY_NAMES =
            setOf(
                "kotlinGradlePluginLibrary",
                "androidGradlePluginLibrary",
                "composeGradlePluginLibrary",
                "detektGradlePluginLibrary",
                "ktlintGradlePluginLibrary",
                "roomGradlePluginLibrary",
                "androidKotlinMultiplatformGradlePluginLibrary",
                "kspGradlePluginLibrary",
            )
        val PROTECTED_PLUGIN_NAMES =
            setOf(
                "kmpPlugin",
                "kmpPresentationPlugin",
                "kmpDomainPlugin",
                "kmpDataPlugin",
                "kmpDiPlugin",
            )

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
