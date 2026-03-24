package com.sarimmehdi.task

import com.sarimmehdi.KmpGeneratorExtension
import com.sarimmehdi.StarterData
import com.sarimmehdi.model.Bundle
import com.sarimmehdi.model.Library
import com.sarimmehdi.model.Plugin
import java.io.File
import org.gradle.api.Project

object TomlGenerator {
    fun generate(
        project: Project,
        extension: KmpGeneratorExtension,
        excludedLibraries: List<Library> = emptyList(),
        excludedPlugins: List<Plugin> = emptyList(),
        excludedBundles: List<Bundle> = emptyList()
    ) {
        val tomlFile = File(project.rootDir, TOML_LOCATION)
        if (!tomlFile.parentFile.exists()) tomlFile.parentFile.mkdirs()

        val isOverwrite = extension.overwriteExisting.getOrElse(false)
        var currentContent = if (tomlFile.exists() && !isOverwrite) {
            tomlFile.readText()
        } else {
            ""
        }

        val allLibraries = (StarterData.libraries + extension.additionalLibraries.get())
            .filter { lib -> excludedLibraries.none { it.libraryName == lib.libraryName } }

        val allPlugins = (StarterData.plugins + extension.additionalPlugins.get())
            .filter { plugin -> excludedPlugins.none { it.pluginName == plugin.pluginName } }

        val excludedLibNames = excludedLibraries.map { it.libraryName }.toSet()
        val excludedBundleNames = excludedBundles.map { it.bundleName }.toSet()

        val allBundles = (StarterData.bundles + extension.additionalBundles.get())
            .filter { it.bundleName !in excludedBundleNames }
            .map { bundle ->
                bundle.copy(libraries = bundle.libraries.filter { it !in excludedLibNames })
            }
            .filter { it.libraries.isNotEmpty() }

        val versionLines = (allLibraries.map { it.versionName to it.versionValue } +
                allPlugins.map { it.versionName to it.versionValue })
            .distinctBy { it.first }
            .map { "${it.first} = \"${it.second}\"" }

        currentContent = appendToSection(currentContent, SECTION_NAME_VERSIONS, versionLines)

        val libraryLines = allLibraries.map {
            "${it.libraryName} = { " +
                    "$LIBRARY_SECTION_GROUP = \"${it.group}\", " +
                    "$LIBRARY_SECTION_NAME = \"${it.name}\", " +
                    "$LIBRARY_SECTION_VERSION_REF = \"${it.versionName}\" }"
        }
        currentContent = appendToSection(currentContent, SECTION_NAME_LIBRARIES, libraryLines)

        val pluginLines = allPlugins.map {
            "${it.pluginName} = { " +
                    "$PLUGIN_SECTION_ID = \"${it.id}\", " +
                    "$PLUGIN_SECTION_VERSION_REF = \"${it.versionName}\" }"
        }
        currentContent = appendToSection(currentContent, SECTION_NAME_PLUGINS, pluginLines)

        val bundleLines = allBundles.map { bundle ->
            "${bundle.bundleName} = [\n    ${bundle.libraries.joinToString(",\n    ") { "\"$it\"" }}\n]"
        }
        currentContent = appendToSection(currentContent, SECTION_NAME_BUNDLES, bundleLines)

        tomlFile.writeText(currentContent.trim() + "\n")

        val statusMessage = if (isOverwrite) "recreated (Overwritten)" else "forged (Appended)"
        println("KmpGenerator: libs.version.toml has been $statusMessage successfully.")
    }

    private fun appendToSection(content: String, sectionName: String, newLines: List<String>): String {
        if (newLines.isEmpty()) return content

        val sectionHeader = "[$sectionName]"
        val lines = content.lines().toMutableList()
        val sectionIndex = lines.indexOfFirst { it.trim() == sectionHeader }

        return if (sectionIndex != -1) {
            var insertIndex = sectionIndex + 1
            while (insertIndex < lines.size && !lines[insertIndex].trim().startsWith("[")) {
                insertIndex++
            }

            val uniqueNewLines = newLines.filter { newLine ->
                val key = newLine.split("=").first().trim()
                lines.none { it.trim().startsWith(key) }
            }

            lines.addAll(insertIndex, uniqueNewLines)
            lines.joinToString("\n")
        } else {
            val prefix = if (content.isEmpty()) "" else "\n\n"
            content + "$prefix$sectionHeader\n" + newLines.joinToString("\n")
        }
    }

    const val TASK_NAME = "generateToml"
    const val TASK_DESCRIPTION = "Forges the libs.version.toml file by merging StarterData with your custom extensions."
    const val TASK_EXAMPLE = "./gradlew $TASK_NAME"
    private const val TOML_LOCATION = "gradle/libs.version.toml"

    private const val SECTION_NAME_VERSIONS = "versions"
    private const val SECTION_NAME_LIBRARIES = "libraries"
    private const val LIBRARY_SECTION_GROUP = "group"
    private const val LIBRARY_SECTION_NAME = "name"
    private const val LIBRARY_SECTION_VERSION_REF = "version.ref"
    private const val SECTION_NAME_PLUGINS = "plugins"
    private const val PLUGIN_SECTION_ID = "id"
    private const val PLUGIN_SECTION_VERSION_REF = "version.ref"
    private const val SECTION_NAME_BUNDLES = "bundles"
}