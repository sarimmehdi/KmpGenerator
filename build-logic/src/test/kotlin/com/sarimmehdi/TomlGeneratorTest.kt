package com.sarimmehdi

import com.sarimmehdi.model.Bundle
import com.sarimmehdi.model.Library
import com.sarimmehdi.model.Plugin
import com.sarimmehdi.task.toml.TomlGenerator.Companion.PROTECTED_BUNDLE_NAME
import com.sarimmehdi.task.toml.TomlGenerator.Companion.PROTECTED_LIBRARY_NAMES
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TomlGeneratorTest {

    @TempDir
    lateinit var testProjectDir: File

    private val buildFile by lazy { testProjectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { testProjectDir.resolve("settings.gradle.kts") }
    private val tomlFile by lazy { testProjectDir.resolve("gradle/libs.versions.toml") }

    private fun setupProject(
        excludedBundles: List<String> = emptyList(),
        excludedLibraries: List<String> = emptyList(),
        excludedPlugins: List<String> = emptyList()
    ) {
        settingsFile.writeText("rootProject.name = \"toml-generator-test\"")

        val bundleList = excludedBundles.joinToString { "\"$it\"" }
        val libList = excludedLibraries.joinToString { "\"$it\"" }
        val pluginList = excludedPlugins.joinToString { "\"$it\"" }

        buildFile.writeText("""
    import com.sarimmehdi.StarterData

    plugins {
        id("com.sarimmehdi.kmp-generator")
    }
    
    kmpGenerator {
        // Properties are now nested inside the 'toml' block
        toml {
            overwriteExisting.set(true)
            
            excludedBundles.set(StarterData.bundles.filter { it.bundleName in listOf<String>($bundleList) })
            excludedLibraries.set(StarterData.libraries.filter { it.libraryName in listOf<String>($libList) })
            excludedPlugins.set(StarterData.plugins.filter { it.pluginName in listOf<String>($pluginList) })
        }
    }
    """.trimIndent())
    }

    private fun runGenerator(): String {
        GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("generateToml")
            .withPluginClasspath()
            .build()
        return tomlFile.readText()
    }

    @Test
    fun `exclude all bundles removes the bundles header`() {
        val allBundleNames = StarterData.bundles.map { it.bundleName }
        setupProject(excludedBundles = allBundleNames)

        val content = runGenerator()

        verifyBundlesNotExist(content, StarterData.bundles - protectedBundles.toSet())
        verifyVersions(content, StarterData.libraries, StarterData.plugins)
        verifyLibraries(content, StarterData.libraries)
        verifyPlugins(content, StarterData.plugins)
    }

    @Test
    fun `exclude all plugins removes the plugins header and unused versions`() {
        val allPluginNames = StarterData.plugins.map { it.pluginName }
        setupProject(excludedPlugins = allPluginNames)

        val content = runGenerator()

        verifyPluginsNotExist(content, StarterData.plugins)
        verifyVersions(content, StarterData.libraries, emptyList())
        verifyLibraries(content, StarterData.libraries)
        verifyBundles(content, StarterData.bundles)
    }

    @Test
    fun `exclude all libraries removes the libraries header`() {
        val allLibNames = StarterData.libraries.map { it.libraryName }
        setupProject(excludedLibraries = allLibNames)

        val content = runGenerator()

        verifyLibrariesNotExist(content, StarterData.libraries - protectedLibraries.toSet())
        verifyVersions(content, emptyList(), StarterData.plugins)
        verifyBundlesNotExist(content, StarterData.bundles - protectedBundles.toSet())
        verifyPlugins(content, StarterData.plugins)
    }

    @Test
    fun `exclude all bundles and libraries leaves only plugins and versions`() {
        setupProject(
            excludedBundles = StarterData.bundles.map { it.bundleName },
            excludedLibraries = StarterData.libraries.map { it.libraryName }
        )

        val content = runGenerator()

        verifyLibrariesNotExist(content, StarterData.libraries - protectedLibraries.toSet())
        verifyBundlesNotExist(content, StarterData.bundles - protectedBundles.toSet())
        verifyVersions(content, emptyList(), StarterData.plugins)
        verifyPlugins(content, StarterData.plugins)
    }

    @Test
    fun `exclude all bundles and plugins leaves only libraries and versions`() {
        setupProject(
            excludedBundles = StarterData.bundles.map { it.bundleName },
            excludedPlugins = StarterData.plugins.map { it.pluginName }
        )

        val content = runGenerator()

        verifyLibraries(content, StarterData.libraries)
        verifyBundlesNotExist(content, StarterData.bundles - protectedBundles.toSet())
        verifyVersions(content, StarterData.libraries, emptyList())
        verifyPluginsNotExist(content, StarterData.plugins)
    }

    @Test
    fun `exclude all libraries and plugins results in protected bundles and libraries only`() {
        setupProject(
            excludedLibraries = StarterData.libraries.map { it.libraryName },
            excludedPlugins = StarterData.plugins.map { it.pluginName }
        )

        val content = runGenerator()

        verifyBundles(content, protectedBundles)
        verifyLibraries(content, protectedLibraries)
        verifyVersions(content, protectedLibraries, emptyList())
    }

    @Test
    fun `generateToml task includes all entries from StarterData`() {
        setupProject()
        val content = runGenerator()

        verifyPlugins(content, StarterData.plugins)
        verifyLibraries(content, StarterData.libraries)
        verifyBundles(content, StarterData.bundles)
        verifyVersions(content, StarterData.libraries, StarterData.plugins)
    }

    @Test
    fun `gradlePluginBundle and its libraries cannot be excluded by the user`() {
        setupProject(
            excludedBundles = listOf(PROTECTED_BUNDLE_NAME),
            excludedLibraries = PROTECTED_LIBRARY_NAMES.toList()
        )

        val runner = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("generateToml")
            .withPluginClasspath()
            .build()

        val content = tomlFile.readText()

        assertTrue(runner.output.contains("Warning"), "Should print a warning when user tries to exclude protected items")
        assertTrue(runner.output.contains(PROTECTED_BUNDLE_NAME), "Warning should mention the protected bundle name")

        verifyBundles(content, protectedBundles)
        verifyLibraries(content, protectedLibraries)
        verifyVersions(content, protectedLibraries, StarterData.plugins)
    }

    @ParameterizedTest
    @MethodSource("provideExcludeScenarios")
    fun `generateToml respects excludedBundles and removes them from output`(
        excludedBundles: List<Bundle>,
        excludedLibraries: List<Library>,
        excludedPlugins: List<Plugin>,
    ) {
        setupProject(
            excludedBundles = excludedBundles.map { it.bundleName },
            excludedLibraries = excludedLibraries.map { it.libraryName },
            excludedPlugins = excludedPlugins.map { it.pluginName }
        )
        val content = runGenerator()

        val includedLibraries = StarterData.libraries - excludedLibraries.toSet()
        val includedBundles = StarterData.bundles - excludedBundles.toSet()
        val includedPlugins = StarterData.plugins - excludedPlugins.toSet()
        verifyLibraries(content, includedLibraries)
        verifyBundles(content, includedBundles, excludedLibraries.map { it.libraryName })
        verifyPlugins(content, includedPlugins)
        verifyVersions(content, includedLibraries, includedPlugins)
        verifyLibrariesNotExist(content, excludedLibraries)
        verifyBundlesNotExist(content, excludedBundles)
        verifyPluginsNotExist(content, excludedPlugins)
    }

    private fun verifyLibraries(content: String, libraries: List<Library>) {
        if (libraries.isEmpty()) return

        val libHeaderIndex = content.indexOf("[libraries]")
        val bundleHeaderIndex = content.indexOf("[bundles]")
        val pluginHeaderIndex = content.indexOf("[plugins]")
        val versionsHeaderIndex = content.indexOf("[versions]")

        assertTrue(versionsHeaderIndex != -1 && versionsHeaderIndex < libHeaderIndex, "Versions header must be above libraries")
        if (bundleHeaderIndex != -1) assertTrue(libHeaderIndex < bundleHeaderIndex, "Bundles header must be below libraries")
        if (pluginHeaderIndex != -1) assertTrue(libHeaderIndex < pluginHeaderIndex, "Plugins header must be below libraries")

        libraries.forEach { lib ->
            val expectedLine = "${lib.libraryName} = { group = \"${lib.group}\", name = \"${lib.name}\", version.ref = \"${lib.versionName}\" }"
            val lineIndex = content.indexOf(expectedLine)

            assertTrue(lineIndex != -1, "Missing library line for ${lib.libraryName}")
            assertTrue(lineIndex > libHeaderIndex, "Library ${lib.libraryName} must be below [libraries] header")

            val nextHeaderIndex = listOf(bundleHeaderIndex, pluginHeaderIndex).filter { it > libHeaderIndex }.minOrNull() ?: content.length
            assertTrue(lineIndex < nextHeaderIndex, "Library ${lib.libraryName} is placed after the end of the libraries section")
        }
    }

    private fun verifyPlugins(content: String, plugins: List<Plugin>) {
        if (plugins.isEmpty()) return

        val pluginHeaderIndex = content.indexOf("[plugins]")
        val libHeaderIndex = content.indexOf("[libraries]")
        val bundleHeaderIndex = content.indexOf("[bundles]")
        val versionsHeaderIndex = content.indexOf("[versions]")

        listOf(versionsHeaderIndex, libHeaderIndex, bundleHeaderIndex).filter { it != -1 }.forEach {
            assertTrue(it < pluginHeaderIndex, "Header index $it must be above plugins header")
        }

        plugins.forEach { plugin ->
            val expectedLine = "${plugin.pluginName} = { id = \"${plugin.id}\", version.ref = \"${plugin.versionName}\" }"
            val lineIndex = content.indexOf(expectedLine)

            assertTrue(lineIndex != -1, "Missing plugin line for ${plugin.pluginName}")
            assertTrue(lineIndex > pluginHeaderIndex, "Plugin ${plugin.pluginName} must be below [plugins] header")
        }
    }

    private fun verifyBundles(
        content: String,
        bundles: List<Bundle>,
        excludedLibraryNames: List<String> = emptyList()
    ) {
        if (bundles.isEmpty()) return

        val bundleHeaderIndex = content.indexOf("[bundles]")
        val pluginHeaderIndex = content.indexOf("[plugins]")
        val libHeaderIndex = content.indexOf("[libraries]")
        val versionsHeaderIndex = content.indexOf("[versions]")

        assertTrue(versionsHeaderIndex != -1 && versionsHeaderIndex < bundleHeaderIndex, "Versions must be above bundles")
        assertTrue(libHeaderIndex != -1 && libHeaderIndex < bundleHeaderIndex, "Libraries must be above bundles")
        if (pluginHeaderIndex != -1) assertTrue(bundleHeaderIndex < pluginHeaderIndex, "Plugins must be below bundles")

        bundles.forEach { bundle ->
            val bundleRegex = Regex("""${bundle.bundleName}\s*=\s*\[([^]]*)]""", RegexOption.DOT_MATCHES_ALL)
            val match = bundleRegex.find(content)

            assertTrue(match != null, "Could not find bundle block for ${bundle.bundleName}")
            assertTrue(match.range.first > bundleHeaderIndex, "Bundle ${bundle.bundleName} must be below [bundles] header")

            val bundleContent = match.groupValues[1]

            bundle.libraries.forEach { libName ->
                if (libName in excludedLibraryNames) {
                    assertFalse(
                        bundleContent.contains("\"$libName\""),
                        "Bundle ${bundle.bundleName} should NOT contain excluded library: $libName"
                    )
                } else {
                    assertTrue(
                        bundleContent.contains("\"$libName\""),
                        "Bundle ${bundle.bundleName} missing active library: $libName"
                    )
                }
            }
        }
    }

    private fun verifyVersions(content: String, libraries: List<Library>, plugins: List<Plugin>) {
        val versionHeaderIndex = content.indexOf("[versions]")
        if (versionHeaderIndex == -1) return

        val nextHeaderIndex = listOf(
            content.indexOf("[libraries]"),
            content.indexOf("[bundles]"),
            content.indexOf("[plugins]")
        ).filter { it != -1 }.minOrNull() ?: content.length

        val uniqueVersions = (libraries.map { it.versionName to it.versionValue } +
                plugins.map { it.versionName to it.versionValue }).distinct()

        uniqueVersions.forEach { (name, value) ->
            val expectedLine = "$name = \"$value\""
            val lineIndex = content.indexOf(expectedLine)

            assertTrue(lineIndex != -1, "Missing version definition: $expectedLine")
            assertTrue(lineIndex > versionHeaderIndex, "Version $name must be below [versions] header")
            assertTrue(lineIndex < nextHeaderIndex, "Version $name must be above all other sections")
        }
    }

    private fun verifyLibrariesNotExist(content: String, libraries: List<Library>) {
        libraries.forEach { lib ->
            val fullLine = "${lib.libraryName} = { group = \"${lib.group}\", name = \"${lib.name}\", version.ref = \"${lib.versionName}\" }"
            assertFalse(
                content.contains(fullLine),
                "Library definition for '${lib.libraryName}' should be entirely absent."
            )
        }

        if (StarterData.libraries.all { it in libraries }) {
            assertFalse(content.contains("[libraries]"), "Header [libraries] should not exist.")
        }
    }

    private fun verifyPluginsNotExist(content: String, plugins: List<Plugin>) {
        plugins.forEach { plugin ->
            val fullLine = "${plugin.pluginName} = { id = \"${plugin.id}\", version.ref = \"${plugin.versionName}\" }"
            assertFalse(
                content.contains(fullLine),
                "Plugin definition for '${plugin.pluginName}' should be entirely absent."
            )
        }

        if (StarterData.plugins.all { it in plugins }) {
            assertFalse(content.contains("[plugins]"), "Header [plugins] should not exist.")
        }
    }

    private fun verifyBundlesNotExist(content: String, bundles: List<Bundle>) {
        bundles.forEach { bundle ->
            val bundleStart = "${bundle.bundleName} = ["
            assertFalse(
                content.contains(bundleStart),
                "Bundle '${bundle.bundleName}' should be entirely absent."
            )
        }

        if (StarterData.bundles.all { it in bundles }) {
            assertFalse(content.contains("[bundles]"), "Header [bundles] should not exist.")
        }
    }

    companion object {
        private val protectedBundles = StarterData.bundles.filter { it.bundleName == PROTECTED_BUNDLE_NAME }
        private val protectedLibraries = StarterData.libraries.filter { it.libraryName in PROTECTED_LIBRARY_NAMES }
        @JvmStatic
        fun provideExcludeScenarios(): Stream<Arguments> = Stream.of(
            Arguments.of(
                (StarterData.bundles - protectedBundles.toSet())
                    .filter { it.bundleName == "composeCoreBundle" },
                emptyList<Library>(),
                emptyList<Plugin>()
            ),
            Arguments.of(
                emptyList<Bundle>(),
                (StarterData.libraries - protectedLibraries.toSet())
                    .filter { it.libraryName == "androidx-core-ktx" },
                StarterData.plugins.filter { it.pluginName == "kotlin-android" }
            ),
            Arguments.of(
                (StarterData.bundles - protectedBundles.toSet())
                    .filter { it.bundleName in listOf("composeCoreBundle", "koinCommonBundle") },
                (StarterData.libraries - protectedLibraries.toSet())
                    .filter { it.libraryName.contains("room") },
                StarterData.plugins.filter { it.pluginName == "google-services" }
            ),
            Arguments.of(emptyList<Bundle>(), emptyList<Library>(), emptyList<Plugin>())
        )
    }
}