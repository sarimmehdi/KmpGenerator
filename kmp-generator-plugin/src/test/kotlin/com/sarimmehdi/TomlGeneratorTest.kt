package com.sarimmehdi

import com.sarimmehdi.task.toml.StarterData
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.PROTECTED_BUNDLE_NAME
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.PROTECTED_LIBRARY_NAMES
import com.sarimmehdi.task.toml.StarterDataReporter.Companion.PROTECTED_PLUGIN_NAMES
import com.sarimmehdi.task.toml.model.Bundle
import com.sarimmehdi.task.toml.model.Library
import com.sarimmehdi.task.toml.model.Plugin
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

class TomlGeneratorTest {
    @TempDir
    lateinit var testProjectDir: File

    private val buildFile by lazy { testProjectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { testProjectDir.resolve("settings.gradle.kts") }
    private val tomlFile by lazy { testProjectDir.resolve("gradle/libs.versions.toml") }

    private fun setupProject(
        excludedBundles: List<String> = emptyList(),
        excludedLibraries: List<String> = emptyList(),
        excludedPlugins: List<String> = emptyList(),
    ) {
        settingsFile.writeText("rootProject.name = \"toml-generator-test\"")

        val bundleList = excludedBundles.joinToString { "\"$it\"" }
        val libList = excludedLibraries.joinToString { "\"$it\"" }
        val pluginList = excludedPlugins.joinToString { "\"$it\"" }

        buildFile.writeText(
            """
            import com.sarimmehdi.task.toml.StarterData

            plugins {
                id("com.sarimmehdi.kmp-generator")
            }
            
            kmpGenerator {
                toml {
                    overwriteExisting.set(true)
                    
                    excludedBundles.set(StarterData.bundles.filter { it.bundleName in listOf<String>($bundleList) })
                    excludedLibraries.set(StarterData.libraries.filter { it.libraryName in listOf<String>($libList) })
                    excludedPlugins.set(StarterData.plugins.filter { it.pluginName in listOf<String>($pluginList) })
                }
            }
            """.trimIndent(),
        )
    }

    private fun runGenerator(): String {
        GradleRunner
            .create()
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
    fun `exclude all plugins removes the all plugins except protected ones and unused versions`() {
        val allPluginNames = StarterData.plugins.map { it.pluginName }
        setupProject(excludedPlugins = allPluginNames)

        val content = runGenerator()

        verifyPluginsNotExist(content, StarterData.plugins - protectedPlugins.toSet())
        verifyVersions(content, StarterData.libraries, emptyList())
        verifyLibraries(content, StarterData.libraries)
        verifyBundles(content, StarterData.bundles)
    }

    @Test
    fun `exclude all libraries removes all libraries except protected ones`() {
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
            excludedLibraries = StarterData.libraries.map { it.libraryName },
        )

        val content = runGenerator()

        verifyLibrariesNotExist(content, StarterData.libraries - protectedLibraries.toSet())
        verifyBundlesNotExist(content, StarterData.bundles - protectedBundles.toSet())
        verifyVersions(content, emptyList(), StarterData.plugins)
        verifyPlugins(content, StarterData.plugins)
    }

    @Test
    fun `exclude all bundles and plugins leaves only libraries, versions, and protected plugins`() {
        setupProject(
            excludedBundles = StarterData.bundles.map { it.bundleName },
            excludedPlugins = StarterData.plugins.map { it.pluginName },
        )

        val content = runGenerator()

        verifyLibraries(content, StarterData.libraries)
        verifyBundlesNotExist(content, StarterData.bundles - protectedBundles.toSet())
        verifyVersions(content, StarterData.libraries, emptyList())
        verifyPluginsNotExist(content, StarterData.plugins - protectedPlugins.toSet())
    }

    @Test
    fun `exclude all libraries and plugins results in protected bundles and libraries only`() {
        setupProject(
            excludedLibraries = StarterData.libraries.map { it.libraryName },
            excludedPlugins = StarterData.plugins.map { it.pluginName },
        )

        val content = runGenerator()

        verifyBundles(content, protectedBundles)
        verifyLibraries(content, protectedLibraries)
        verifyVersions(content, protectedLibraries, protectedPlugins)
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
    fun `gradlePluginBundle, its libraries, and convention plugins cannot be excluded by the user`() {
        setupProject(
            excludedBundles = listOf(PROTECTED_BUNDLE_NAME),
            excludedLibraries = PROTECTED_LIBRARY_NAMES.toList(),
            excludedPlugins = PROTECTED_PLUGIN_NAMES.toList(),
        )

        val runner =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments("generateToml")
                .withPluginClasspath()
                .build()

        val content = tomlFile.readText()

        assertThat(runner.output)
            .describedAs("Should print a warning when user tries to exclude protected items")
            .contains("Warning")
            .contains(PROTECTED_BUNDLE_NAME)

        verifyBundles(content, protectedBundles)
        verifyLibraries(content, protectedLibraries)
        verifyVersions(content, protectedLibraries, StarterData.plugins)
        verifyPlugins(content, protectedPlugins)
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
            excludedPlugins = excludedPlugins.map { it.pluginName },
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

    private fun verifyLibraries(
        content: String,
        libraries: List<Library>,
    ) {
        if (libraries.isEmpty()) return

        val libHeaderIndex = content.indexOf("[libraries]")
        val bundleHeaderIndex = content.indexOf("[bundles]")
        val pluginHeaderIndex = content.indexOf("[plugins]")
        val versionsHeaderIndex = content.indexOf("[versions]")

        assertThat(versionsHeaderIndex != -1 && versionsHeaderIndex < libHeaderIndex)
            .describedAs("Versions header must be above libraries")
            .isTrue()

        if (bundleHeaderIndex != -1) {
            assertThat(libHeaderIndex)
                .describedAs("Bundles header must be below libraries")
                .isLessThan(bundleHeaderIndex)
        }

        if (pluginHeaderIndex != -1) {
            assertThat(libHeaderIndex)
                .describedAs("Plugins header must be below libraries")
                .isLessThan(pluginHeaderIndex)
        }

        libraries.forEach { lib ->
            val expectedLine =
                "${lib.libraryName} = { " +
                    "group = \"${lib.group}\", " +
                    "name = \"${lib.name}\", " +
                    "version.ref = \"${lib.versionName}\" " +
                    "}"
            val lineIndex = content.indexOf(expectedLine)

            assertThat(lineIndex)
                .describedAs("Missing library line for ${lib.libraryName}")
                .isNotEqualTo(-1)

            assertThat(lineIndex)
                .describedAs("Library ${lib.libraryName} must be below [libraries] header")
                .isGreaterThan(libHeaderIndex)

            val nextHeaderIndex =
                listOf(bundleHeaderIndex, pluginHeaderIndex)
                    .filter { it > libHeaderIndex }
                    .minOrNull() ?: content.length

            assertThat(lineIndex)
                .describedAs("Library ${lib.libraryName} is placed after the end of the libraries section")
                .isLessThan(nextHeaderIndex)
        }
    }

    private fun verifyPlugins(
        content: String,
        plugins: List<Plugin>,
    ) {
        if (plugins.isEmpty()) return

        val pluginHeaderIndex = content.indexOf("[plugins]")
        val libHeaderIndex = content.indexOf("[libraries]")
        val bundleHeaderIndex = content.indexOf("[bundles]")
        val versionsHeaderIndex = content.indexOf("[versions]")

        listOf(versionsHeaderIndex, libHeaderIndex, bundleHeaderIndex)
            .filter { it != -1 }
            .forEach { headerIndex ->
                assertThat(headerIndex)
                    .describedAs("Header at index $headerIndex must be above plugins header")
                    .isLessThan(pluginHeaderIndex)
            }

        plugins.forEach { plugin ->
            val expectedLine =
                "${plugin.pluginName} = { " +
                    "id = \"${plugin.id}\", " +
                    "version.ref = \"${plugin.versionName}\" " +
                    "}"
            val lineIndex = content.indexOf(expectedLine)

            assertThat(lineIndex)
                .describedAs("Missing plugin line for ${plugin.pluginName}")
                .isNotEqualTo(-1)

            assertThat(lineIndex)
                .describedAs("Plugin ${plugin.pluginName} must be below [plugins] header")
                .isGreaterThan(pluginHeaderIndex)
        }
    }

    private fun verifyBundles(
        content: String,
        bundles: List<Bundle>,
        excludedLibraryNames: List<String> = emptyList(),
    ) {
        if (bundles.isEmpty()) return

        val bundleHeaderIndex = content.indexOf("[bundles]")
        validateSectionOrdering(content, bundleHeaderIndex)

        bundles.forEach { bundle ->
            val bundleRegex = Regex("""${bundle.bundleName}\s*=\s*\[([^]]*)]""", RegexOption.DOT_MATCHES_ALL)
            val match =
                bundleRegex.find(content) ?: run {
                    fail("Could not find bundle block for ${bundle.bundleName}")
                }

            assertThat(match.range.first)
                .describedAs("Bundle ${bundle.bundleName} must be below [bundles] header")
                .isGreaterThan(bundleHeaderIndex)

            verifyLibraryInclusion(
                bundleName = bundle.bundleName,
                bundleContent = match.groupValues[1],
                libraries = bundle.libraries,
                excludedLibraryNames = excludedLibraryNames,
            )
        }
    }

    private fun validateSectionOrdering(
        content: String,
        bundleHeaderIndex: Int,
    ) {
        val pluginHeaderIndex = content.indexOf("[plugins]")
        val libHeaderIndex = content.indexOf("[libraries]")
        val versionsHeaderIndex = content.indexOf("[versions]")

        assertThat(versionsHeaderIndex != -1 && versionsHeaderIndex < bundleHeaderIndex)
            .describedAs("Versions must be above bundles")
            .isTrue()

        assertThat(libHeaderIndex != -1 && libHeaderIndex < bundleHeaderIndex)
            .describedAs("Libraries must be above bundles")
            .isTrue()

        if (pluginHeaderIndex != -1) {
            assertThat(bundleHeaderIndex)
                .describedAs("Plugins must be below bundles")
                .isLessThan(pluginHeaderIndex)
        }
    }

    private fun verifyLibraryInclusion(
        bundleName: String,
        bundleContent: String,
        libraries: List<String>,
        excludedLibraryNames: List<String>,
    ) {
        libraries.forEach { libName ->
            val isExcluded = libName in excludedLibraryNames
            val containsLib = bundleContent.contains("\"$libName\"")

            if (isExcluded) {
                assertThat(containsLib)
                    .describedAs("Bundle $bundleName should NOT contain excluded library: $libName")
                    .isFalse()
            } else {
                assertThat(containsLib)
                    .describedAs("Bundle $bundleName missing active library: $libName")
                    .isTrue()
            }
        }
    }

    private fun verifyVersions(
        content: String,
        libraries: List<Library>,
        plugins: List<Plugin>,
    ) {
        val versionHeaderIndex = content.indexOf("[versions]")
        if (versionHeaderIndex == -1) return

        val nextHeaderIndex =
            listOf(
                content.indexOf("[libraries]"),
                content.indexOf("[bundles]"),
                content.indexOf("[plugins]"),
            ).filter { it != -1 }.minOrNull() ?: content.length

        val uniqueVersions =
            (
                libraries.map { it.versionName to it.versionValue } +
                    plugins.map { it.versionName to it.versionValue }
            ).distinct()

        uniqueVersions.forEach { (name, value) ->
            val expectedLine = "$name = \"$value\""
            val lineIndex = content.indexOf(expectedLine)

            assertThat(lineIndex)
                .describedAs("Missing version definition: $expectedLine")
                .isNotEqualTo(-1)

            assertThat(lineIndex)
                .describedAs("Version $name must be below [versions] header")
                .isGreaterThan(versionHeaderIndex)

            assertThat(lineIndex)
                .describedAs("Version $name must be above all other sections")
                .isLessThan(nextHeaderIndex)
        }
    }

    private fun verifyLibrariesNotExist(
        content: String,
        libraries: List<Library>,
    ) {
        libraries.forEach { lib ->
            val fullLine =
                "${lib.libraryName} = { " +
                    "group = \"${lib.group}\", " +
                    "name = \"${lib.name}\", " +
                    "version.ref = \"${lib.versionName}\" " +
                    "}"
            assertThat(content.contains(fullLine))
                .describedAs("Library definition for '${lib.libraryName}' should be entirely absent.")
                .isFalse()
        }

        if (StarterData.libraries.all { it in libraries }) {
            assertThat(content.contains("[libraries]"))
                .describedAs("Header [libraries] should not exist.")
                .isFalse()
        }
    }

    private fun verifyPluginsNotExist(
        content: String,
        plugins: List<Plugin>,
    ) {
        plugins.forEach { plugin ->
            val fullLine = "${plugin.pluginName} = { id = \"${plugin.id}\", version.ref = \"${plugin.versionName}\" }"
            assertThat(content.contains(fullLine))
                .describedAs("Plugin definition for '${plugin.pluginName}' should be entirely absent.")
                .isFalse()
        }

        if (StarterData.plugins.all { it in plugins }) {
            assertThat(content.contains("[plugins]"))
                .describedAs("Header [plugins] should not exist.")
                .isFalse()
        }
    }

    private fun verifyBundlesNotExist(
        content: String,
        bundles: List<Bundle>,
    ) {
        bundles.forEach { bundle ->
            val bundleStart = "${bundle.bundleName} = ["
            assertThat(content.contains(bundleStart))
                .describedAs("Bundle '${bundle.bundleName}' should be entirely absent.")
                .isFalse()
        }

        if (StarterData.bundles.all { it in bundles }) {
            assertThat(content.contains("[bundles]"))
                .describedAs("Header [bundles] should not exist.")
                .isFalse()
        }
    }

    companion object {
        private val protectedBundles = StarterData.bundles.filter { it.bundleName == PROTECTED_BUNDLE_NAME }
        private val protectedLibraries = StarterData.libraries.filter { it.libraryName in PROTECTED_LIBRARY_NAMES }
        private val protectedPlugins = StarterData.plugins.filter { it.pluginName in PROTECTED_PLUGIN_NAMES }

        @JvmStatic
        fun provideExcludeScenarios(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    (StarterData.bundles - protectedBundles.toSet())
                        .filter { it.bundleName == "composeCoreBundle" },
                    emptyList<Library>(),
                    emptyList<Plugin>(),
                ),
                Arguments.of(
                    emptyList<Bundle>(),
                    (StarterData.libraries - protectedLibraries.toSet())
                        .filter { it.libraryName == "androidx-core-ktx" },
                    StarterData.plugins.filter { it.pluginName == "kotlin-android" },
                ),
                Arguments.of(
                    (StarterData.bundles - protectedBundles.toSet())
                        .filter { it.bundleName in listOf("composeCoreBundle", "koinCommonBundle") },
                    (StarterData.libraries - protectedLibraries.toSet())
                        .filter { it.libraryName.contains("room") },
                    StarterData.plugins.filter { it.pluginName == "google-services" },
                ),
                Arguments.of(emptyList<Bundle>(), emptyList<Library>(), emptyList<Plugin>()),
            )
    }
}
