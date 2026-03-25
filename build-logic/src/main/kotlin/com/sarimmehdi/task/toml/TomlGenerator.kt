package com.sarimmehdi.task.toml

import com.sarimmehdi.StarterData
import org.gradle.api.DefaultTask
import com.sarimmehdi.model.Bundle
import com.sarimmehdi.model.Library
import com.sarimmehdi.model.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class TomlGenerator : DefaultTask() {
    interface TomlConfig {
        val additionalLibraries: ListProperty<Library>
        val additionalPlugins: ListProperty<Plugin>
        val additionalBundles: ListProperty<Bundle>
        val excludedLibraries: ListProperty<Library>
        val excludedPlugins: ListProperty<Plugin>
        val excludedBundles: ListProperty<Bundle>
        val overwriteExisting: Property<Boolean>
    }
    @get:Input
    abstract val additionalLibraries: ListProperty<Library>
    @get:Input
    abstract val additionalPlugins: ListProperty<Plugin>
    @get:Input
    abstract val additionalBundles: ListProperty<Bundle>
    @get:Input
    abstract val excludedLibraries: ListProperty<Library>
    @get:Input
    abstract val excludedPlugins: ListProperty<Plugin>
    @get:Input
    abstract val excludedBundles: ListProperty<Bundle>
    @get:Input
    abstract val overwriteExisting: Property<Boolean>
    @get:OutputFile
    abstract val tomlFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val fileToForge = tomlFile.get().asFile
        if (!fileToForge.parentFile.exists()) fileToForge.parentFile.mkdirs()

        val isOverwrite = overwriteExisting.getOrElse(false)

        val currentAdditionalLibs = additionalLibraries.getOrElse(emptyList())
        val currentAdditionalPlugins = additionalPlugins.getOrElse(emptyList())
        val currentAdditionalBundles = additionalBundles.getOrElse(emptyList())
        val currentExcludedLibs = excludedLibraries.getOrElse(emptyList())
        val currentExcludedPlugins = excludedPlugins.getOrElse(emptyList())
        val currentExcludedBundles = excludedBundles.getOrElse(emptyList())

        var currentContent = if (fileToForge.exists() && !isOverwrite) {
            fileToForge.readText()
        } else {
            ""
        }

        if (currentExcludedBundles.any { it.bundleName == PROTECTED_BUNDLE_NAME }) {
            println("KmpGenerator: [Warning] '$PROTECTED_BUNDLE_NAME' is critical for build-logic and cannot be excluded.")
        }

        val excludedProtectedLibs = currentExcludedLibs.map { it.libraryName }.filter { it in PROTECTED_LIBRARY_NAMES }
        if (excludedProtectedLibs.isNotEmpty()) {
            println($$"KmpGenerator: [Warning] The following libraries are required for build-logic and cannot be excluded: $excludedProtectedLibs")
        }

        val allLibraries = (StarterData.libraries + currentAdditionalLibs)
            .filter { lib ->
                if (lib.libraryName in PROTECTED_LIBRARY_NAMES) true
                else currentExcludedLibs.none { it.libraryName == lib.libraryName }
            }

        val allPlugins = (StarterData.plugins + currentAdditionalPlugins)
            .filter { plugin -> currentExcludedPlugins.none { it.pluginName == plugin.pluginName } }

        val excludedLibNames = currentExcludedLibs
            .map { it.libraryName }
            .filter { it !in PROTECTED_LIBRARY_NAMES }
            .toSet()

        val excludedBundleNames = currentExcludedBundles
            .map { it.bundleName }
            .filter { it != PROTECTED_BUNDLE_NAME }
            .toSet()

        val allBundles = (StarterData.bundles + currentAdditionalBundles)
            .filter { it.bundleName !in excludedBundleNames }
            .map { bundle ->
                if (bundle.bundleName == PROTECTED_BUNDLE_NAME) {
                    bundle
                } else {
                    bundle.copy(libraries = bundle.libraries.filter { it !in excludedLibNames })
                }
            }
            .filter { it.libraries.isNotEmpty() }

        val activeVersionNames = (allLibraries.map { it.versionName } + allPlugins.map { it.versionName }).toSet()

        val versionLines = (allLibraries.map { it.versionName to it.versionValue } +
                allPlugins.map { it.versionName to it.versionValue })
            .distinctBy { it.first }
            .filter { it.first in activeVersionNames }
            .map { "${it.first} = \"${it.second}\"" }

        currentContent = appendToSection(currentContent, SECTION_NAME_VERSIONS, versionLines)

        val libraryLines = allLibraries.map {
            "${it.libraryName} = { " +
                    "$LIBRARY_SECTION_GROUP = \"${it.group}\", " +
                    "$LIBRARY_SECTION_NAME = \"${it.name}\", " +
                    "$LIBRARY_SECTION_VERSION_REF = \"${it.versionName}\" }"
        }
        currentContent = appendToSection(currentContent, SECTION_NAME_LIBRARIES, libraryLines)

        val bundleLines = allBundles.map { bundle ->
            "${bundle.bundleName} = [\n    ${bundle.libraries.joinToString(",\n    ") { "\"$it\"" }}\n]"
        }
        currentContent = appendToSection(currentContent, SECTION_NAME_BUNDLES, bundleLines)

        val pluginLines = allPlugins.map {
            "${it.pluginName} = { " +
                    "$PLUGIN_SECTION_ID = \"${it.id}\", " +
                    "$PLUGIN_SECTION_VERSION_REF = \"${it.versionName}\" }"
        }
        currentContent = appendToSection(currentContent, SECTION_NAME_PLUGINS, pluginLines)

        fileToForge.writeText(currentContent.trim() + "\n")

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

    companion object {
        const val TASK_NAME = "generateToml"
        const val TASK_DESCRIPTION = "Forges the libs.version.toml file by merging StarterData with your custom extensions."
        val TASK_EXAMPLE = """
        Usage Example in build.gradle.kts:
        
        kmpGenerator {
            // Overwrite existing file or append new entries
            overwriteExisting.set(true)
            
            // Exclude specific default bundles/libraries/plugins
            excludedBundles.set(listOf("composeCoreBundle"))
            excludedLibraries.set(listOf("composeRuntimeLibrary"))
            excludedPlugins.set(listOf("kotlinSerializationPlugin"))
            
            // Add your own custom entries
            additionalLibraries.add(Library("myLib", "com.example", "core", "1.0.0", "myLibVersion"))
        }
        
        Run via terminal:
        ./gradlew $TASK_NAME
    """.trimIndent()
        private const val TOML_LOCATION = "gradle/libs.versions.toml"

        private const val SECTION_NAME_VERSIONS = "versions"
        private const val SECTION_NAME_LIBRARIES = "libraries"
        private const val LIBRARY_SECTION_GROUP = "group"
        private const val LIBRARY_SECTION_NAME = "name"
        private const val LIBRARY_SECTION_VERSION_REF = "version.ref"
        private const val SECTION_NAME_PLUGINS = "plugins"
        private const val PLUGIN_SECTION_ID = "id"
        private const val PLUGIN_SECTION_VERSION_REF = "version.ref"
        private const val SECTION_NAME_BUNDLES = "bundles"

        const val PROTECTED_BUNDLE_NAME = "gradlePluginBundle"
        val PROTECTED_LIBRARY_NAMES = setOf(
            "kotlinGradlePluginLibrary",
            "androidGradlePluginLibrary",
            "composeGradlePluginLibrary",
            "detektGradlePluginLibrary",
            "ktlintGradlePluginLibrary",
            "roomGradlePluginLibrary",
            "androidKotlinMultiplatformGradlePluginLibrary",
            "kspGradlePluginLibrary"
        )

        fun register(
            project: Project,
            taskGroup: String,
            config: TomlConfig
        ) {
            project.tasks.register(TASK_NAME, TomlGenerator::class.java) {
                group = taskGroup
                description = TASK_DESCRIPTION
                this.additionalLibraries.set(config.additionalLibraries)
                this.additionalPlugins.set(config.additionalPlugins)
                this.additionalBundles.set(config.additionalBundles)
                this.excludedLibraries.set(config.excludedLibraries)
                this.excludedPlugins.set(config.excludedPlugins)
                this.excludedBundles.set(config.excludedBundles)
                this.overwriteExisting.set(config.overwriteExisting)
                this.tomlFile.set(project.layout.projectDirectory.file(TOML_LOCATION))
            }
        }
    }
}