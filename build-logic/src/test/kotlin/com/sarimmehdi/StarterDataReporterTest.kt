package com.sarimmehdi

import com.sarimmehdi.KmpGeneratorPlugin.Companion.TASK_GROUP
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class StarterDataReporterTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile by lazy { testProjectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { testProjectDir.resolve("settings.gradle.kts") }

    private fun setupProject() {
        settingsFile.writeText("rootProject.name = \"reporter-test\"")

        buildFile.writeText("""
            plugins {
                id("com.sarimmehdi.kmp-generator")
            }
        """.trimIndent())
    }

    @Test
    fun `listStarterData task is registered and has correct description`() {
        setupProject()

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--group", TASK_GROUP)
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("listStarterData"), "Task listStarterData should be registered")
        assertTrue(
            result.output.contains("Inventory Check: Lists every library, plugin, and bundle"),
            "Task should have the correct description"
        )
    }

    @Test
    fun `listStarterData prints all libraries, plugins, and bundles from StarterData`() {
        setupProject()

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("listStarterData")
            .withPluginClasspath()
            .build()

        val output = result.output

        assertTrue(output.contains("KMP GENERATOR: STARTER DATA INVENTORY"))
        assertTrue(output.contains("[LIBRARIES]"))
        assertTrue(output.contains("[PLUGINS]"))
        assertTrue(output.contains("[BUNDLES]"))
        assertTrue(output.contains("[VERSIONS]"))

        StarterData.libraries.forEach { lib ->
            assertTrue(
                output.contains(lib.libraryName) && output.contains(lib.group),
                "Output should contain library: ${lib.libraryName}"
            )
        }

        StarterData.plugins.forEach { plugin ->
            assertTrue(
                output.contains(plugin.pluginName) && output.contains(plugin.id),
                "Output should contain plugin: ${plugin.pluginName}"
            )
        }

        StarterData.bundles.forEach { bundle ->
            assertTrue(
                output.contains(bundle.bundleName),
                "Output should contain bundle: ${bundle.bundleName}"
            )
            bundle.libraries.take(1).forEach { libName ->
                assertTrue(output.contains(libName), "Bundle output should list library: $libName")
            }
        }

        val firstVersion = StarterData.libraries.firstOrNull()?.versionName
        if (firstVersion != null) {
            assertTrue(output.contains(firstVersion), "Output should contain version ref: $firstVersion")
        }
    }
}