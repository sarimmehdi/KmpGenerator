package com.sarimmehdi

import com.sarimmehdi.task.KmpHelpReporter
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KmpHelpReporterTest {
    @TempDir
    lateinit var testProjectDir: File

    private val buildFile by lazy { testProjectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { testProjectDir.resolve("settings.gradle.kts") }

    private fun setupProject() {
        settingsFile.writeText("rootProject.name = \"help-reporter-test\"")

        buildFile.writeText(
            """
            plugins {
                id("com.sarimmehdi.kmp-generator")
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `helpKmpGenerator output matches messageToPrint exactly`() {
        setupProject()

        val expectedOutput = KmpHelpReporter.messageToPrint()

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(KmpHelpReporter.TASK_NAME, "-q")
                .withPluginClasspath()
                .build()

        assertThat(result.output)
            .describedAs("The task output should contain the help message")
            .containsIgnoringWhitespaces(expectedOutput)
    }
}
