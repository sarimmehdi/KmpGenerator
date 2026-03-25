package com.sarimmehdi

import com.sarimmehdi.task.KmpHelpReporter
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class KmpHelpReporterTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile by lazy { testProjectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { testProjectDir.resolve("settings.gradle.kts") }

    private fun setupProject() {
        settingsFile.writeText("rootProject.name = \"help-reporter-test\"")

        buildFile.writeText("""
            plugins {
                id("com.sarimmehdi.kmp-generator")
            }
        """.trimIndent())
    }

    @Test
    fun `helpKmpGenerator output matches messageToPrint exactly`() {
        setupProject()

        val expectedOutput = KmpHelpReporter.messageToPrint()

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(KmpHelpReporter.TASK_NAME, "-q")
            .withPluginClasspath()
            .build()

        assertTrue(
            result.output.contains(expectedOutput),
            "The task output did not match the expected help message from messageToPrint()"
        )
    }
}