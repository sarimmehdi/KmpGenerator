package com.sarimmehdi

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class TomlGeneratorTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile by lazy { testProjectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { testProjectDir.resolve("settings.gradle.kts") }
    private val tomlFile by lazy { testProjectDir.resolve("gradle/libs.version.toml") }

    @Test
    fun `generateToml task creates a toml file with starter data`() {
        settingsFile.writeText("rootProject.name = \"test-project\"")
        buildFile.writeText("""
            plugins {
                id("com.sarimmehdi.kmp-generator")
            }
            
            kmpGenerator {
                overwriteExisting.set(true)
            }
        """.trimIndent())

        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("generateToml")
            .withPluginClasspath()
            .build()

        assertTrue(tomlFile.exists(), "TOML file should be created")

        val content = tomlFile.readText()
        assertTrue(content.contains("[versions]"), "Should contain versions section")
        assertTrue(content.contains("[libraries]"), "Should contain libraries section")
    }
}