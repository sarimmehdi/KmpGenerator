package com.sarimmehdi

import com.sarimmehdi.KmpGeneratorPlugin.Companion.TASK_GROUP
import com.sarimmehdi.task.toml.StarterData
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.BUNDLES_HEADER
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.BUNDLES_PAD_COUNTER
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.LIBRARIES_HEADER
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.LIBRARIES_PAD_COUNTER
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.MAIN_HEADER
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.PLUGINS_HEADER
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.PLUGINS_PAD_COUNTER
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.PROTECTED_BUNDLES_HEADER
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.PROTECTED_LIBRARIES_HEADER
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.PROTECTED_LIBRARY_NAMES
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.PROTECTED_PLUGINS_HEADER
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.PROTECTED_PLUGIN_NAMES
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.TASK_DESCRIPTION
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.TASK_NAME
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.VERSIONS_HEADER
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.VERSIONS_PAD_COUNTER
import com.sarimmehdi.task.toml.model.Bundle
import com.sarimmehdi.task.toml.model.Library
import com.sarimmehdi.task.toml.model.Plugin
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class StarterDataReporterTest {
    @TempDir
    lateinit var testProjectDir: File

    private val buildFile by lazy { testProjectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { testProjectDir.resolve("settings.gradle.kts") }

    private fun setupProject() {
        settingsFile.writeText("rootProject.name = \"reporter-test\"")

        buildFile.writeText(
            """
            plugins {
                id("com.sarimmehdi.kmp-generator")
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `listStarterData task is registered and has correct description`() {
        setupProject()

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--group", TASK_GROUP)
                .withPluginClasspath()
                .build()

        assertThat(result.output)
            .contains(TASK_NAME)
            .contains(TASK_DESCRIPTION)
    }

    @Test
    fun `listStarterData prints data in correct categorical order`() {
        setupProject()

        val output =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(TASK_NAME)
                .withPluginClasspath()
                .build()
                .output

        assertThat(output).contains(MAIN_HEADER)

        val indices = captureHeaderIndices(output)
        verifyHeaderOrder(indices)

        // Assert individual sections
        verifyStandardSections(output, indices)
        verifyProtectedSections(output, indices)
    }

    private fun captureHeaderIndices(output: String) =
        mapOf(
            LIBRARIES_HEADER to output.indexOf("[$LIBRARIES_HEADER]"),
            PLUGINS_HEADER to output.indexOf("[$PLUGINS_HEADER]"),
            BUNDLES_HEADER to output.indexOf("[$BUNDLES_HEADER]"),
            VERSIONS_HEADER to output.indexOf("[$VERSIONS_HEADER]"),
            PROTECTED_LIBRARIES_HEADER to output.indexOf("[$PROTECTED_LIBRARIES_HEADER]"),
            PROTECTED_PLUGINS_HEADER to output.indexOf("[$PROTECTED_PLUGINS_HEADER]"),
            PROTECTED_BUNDLES_HEADER to output.indexOf("[$PROTECTED_BUNDLES_HEADER]"),
        )

    private fun verifyHeaderOrder(indices: Map<String, Int>) {
        assertThat(indices[LIBRARIES_HEADER]).isLessThan(indices[PLUGINS_HEADER])
        assertThat(indices[PLUGINS_HEADER]).isLessThan(indices[BUNDLES_HEADER])
        assertThat(indices[BUNDLES_HEADER]).isLessThan(indices[VERSIONS_HEADER])
        assertThat(indices[VERSIONS_HEADER]).isLessThan(indices[PROTECTED_LIBRARIES_HEADER])
        assertThat(indices[PROTECTED_LIBRARIES_HEADER]).isLessThan(indices[PROTECTED_PLUGINS_HEADER])
        assertThat(indices[PROTECTED_PLUGINS_HEADER]).isLessThan(indices[PROTECTED_BUNDLES_HEADER])
    }

    private fun verifyStandardSections(
        output: String,
        indices: Map<String, Int>,
    ) {
        StarterData.libraries.forEach { lib ->
            assertOutputLine(output, formatLibrary(lib), indices[LIBRARIES_HEADER]!!, indices[PLUGINS_HEADER]!!)
        }
        StarterData.plugins.forEach { plugin ->
            assertOutputLine(output, formatPlugin(plugin), indices[PLUGINS_HEADER]!!, indices[BUNDLES_HEADER]!!)
        }
        StarterData.bundles.forEach { bundle ->
            assertOutputLine(output, formatBundle(bundle), indices[BUNDLES_HEADER]!!, indices[VERSIONS_HEADER]!!)
        }
        verifyVersions(output, indices[VERSIONS_HEADER]!!, indices[PROTECTED_LIBRARIES_HEADER]!!)
    }

    private fun verifyVersions(
        output: String,
        start: Int,
        end: Int,
    ) {
        (
            StarterData.libraries.map { it.versionName to it.versionValue } +
                StarterData.plugins.map { it.versionName to it.versionValue }
        ).distinctBy { it.first }
            .sortedBy { it.first }
            .forEach { (name, value) ->
                val expected = "  - ${name.padEnd(VERSIONS_PAD_COUNTER)} = $value"
                assertOutputLine(output, expected, start, end)
            }
    }

    private fun verifyProtectedSections(
        output: String,
        indices: Map<String, Int>,
    ) {
        val libStart = indices[PROTECTED_LIBRARIES_HEADER]!!
        val pluginStart = indices[PROTECTED_PLUGINS_HEADER]!!
        val bundleStart = indices[PROTECTED_BUNDLES_HEADER]!!

        StarterData.libraries.filter { it.libraryName in PROTECTED_LIBRARY_NAMES }.forEach {
            assertOutputLine(output, formatLibrary(it), libStart, pluginStart)
        }
        StarterData.plugins.filter { it.pluginName in PROTECTED_PLUGIN_NAMES }.forEach {
            assertOutputLine(output, formatPlugin(it), pluginStart, bundleStart)
        }
    }

    private fun assertOutputLine(
        output: String,
        expected: String,
        min: Int,
        max: Int,
    ) {
        val index = output.indexOf(expected, min)
        assertThat(index)
            .describedAs("Line '$expected' missing or out of bounds")
            .isGreaterThan(min)
            .isLessThan(max)
    }

    private fun formatLibrary(lib: Library) =
        "  - ${lib.libraryName.padEnd(LIBRARIES_PAD_COUNTER)} [${lib.group}:${lib.name}] ref: ${lib.versionName}"

    private fun formatPlugin(p: Plugin) =
        "  - " +
            "${p.pluginName.padEnd(PLUGINS_PAD_COUNTER)} [id: ${p.id}] ref: ${p.versionName}"

    private fun formatBundle(b: Bundle) =
        "  - " +
            "${b.bundleName.padEnd(BUNDLES_PAD_COUNTER)} -> ${b.libraries.joinToString(", ")}"
}
