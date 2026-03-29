package com.sarimmehdi

import com.sarimmehdi.task.buildlogic.GenerateBuildLogicTask.Companion.TASK_NAME
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

@Suppress("LongMethod", "LargeClass")
class GenerateBuildLogicTaskTest {
    @TempDir
    lateinit var testProjectDir: File

    private lateinit var buildFile: File
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        settingsFile =
            File(testProjectDir, "settings.gradle.kts").apply {
                writeText("rootProject.name = \"build-logic-test\"")
            }
        buildFile =
            File(testProjectDir, "build.gradle.kts").apply {
                writeText(
                    """
                    plugins {
                        id("${BASE_PACKAGE}.kmp-generator")
                    }

                    kmpGenerator {
                        buildLogic {
                            basePackage.set("$BASE_PACKAGE")
                            namespace.set("$NAMESPACE")
                        }
                    }
                    """.trimIndent(),
                )
            }
    }

    @Test
    fun `task should succeed when package name is valid`() {
        GradleRunner
            .create()
            .withProjectDir(testProjectDir)
            .withArguments(TASK_NAME)
            .withPluginClasspath()
            .build()

        val expectedDir = File(testProjectDir, "build-logic/convention/src/main/kotlin/com/sarimmehdi")
        assertThat(expectedDir).exists().isDirectory()
    }

    @Test
    fun `task should fail when package name starts with a number`() {
        updateBuildFile(invalidPackage = "123.bad.package")

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(TASK_NAME)
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.output).contains("Invalid package name: '123.bad.package'")
    }

    @Test
    fun `task should fail when package name contains uppercase letters`() {
        updateBuildFile(invalidPackage = "com.Sarim.Example")

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(TASK_NAME)
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.output).contains("Invalid package name: 'com.Sarim.Example'")
    }

    @Test
    fun `task should fail when package name contains invalid characters`() {
        updateBuildFile(invalidPackage = "com.sarim-mehdi.task")

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(TASK_NAME)
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.output).contains("Invalid package name: 'com.sarim-mehdi.task'")
    }

    private fun updateBuildFile(invalidPackage: String) {
        buildFile.writeText(
            """
            plugins {
                id("$BASE_PACKAGE.kmp-generator")
            }

            kmpGenerator {
                buildLogic {
                    basePackage.set("$invalidPackage")
                    namespace.set("$NAMESPACE")
                }
            }
            """.trimIndent(),
        )
    }

    @ParameterizedTest
    @MethodSource("provideFileVerificationData")
    fun `verify generated file contents`(
        relativePath: String,
        expectedContent: String,
    ) {
        GradleRunner
            .create()
            .withProjectDir(testProjectDir)
            .withArguments(TASK_NAME)
            .withPluginClasspath()
            .build()

        val file = File(testProjectDir, relativePath)

        assertThat(file).exists()

        assertThat(file.readText())
            .describedAs("Checking content for file: $relativePath")
            .isEqualToIgnoringWhitespace(expectedContent)
    }

    companion object {
        private const val BASE_PACKAGE = "com.sarimmehdi"
        private const val NAMESPACE = "$BASE_PACKAGE.sample"

        @JvmStatic
        fun provideFileVerificationData(): Stream<Arguments> =
            Stream.of(
                getGitIgnoreData(),
                getSettingsGradleData(),
                getBuildGradleData(),
                getConfigKtData(),
                getAndroidTargetData(),
                getQualityToolsData(),
                getDataExtensionData(),
                getDiExtensionData(),
                getProjectExtsData(),
                getKmpDataPluginData(),
                getKmpDiPluginData(),
                getKmpDomainPluginData(),
                getKmpPluginData(),
                getKmpPresentationPluginData(),
            )

        private fun getGitIgnoreData() =
            Arguments.of(
                "build-logic/.gitignore",
                """
                /build
                """.trimIndent(),
            )

        private fun getSettingsGradleData() =
            Arguments.of(
                "build-logic/settings.gradle.kts",
                """
                dependencyResolutionManagement {
                    repositories {
                        gradlePluginPortal()
                        google()
                        mavenCentral()
                    }
                    versionCatalogs {
                        create("libs") {
                            from(files("../gradle/libs.versions.toml"))
                        }
                    }
                }
                rootProject.name = "build-logic"
                include(":convention")
                """.trimIndent(),
            )

        private fun getBuildGradleData() =
            Arguments.of(
                "build-logic/convention/build.gradle.kts",
                """
                import org.jetbrains.kotlin.gradle.dsl.JvmTarget
                
                plugins {
                    `kotlin-dsl`
                    alias(libs.plugins.detektPlugin)
                    alias(libs.plugins.ktlintPlugin)
                }
                
                group = "$BASE_PACKAGE.buildlogic"
                
                ktlint {
                    android.set(false)
                    verbose.set(true)
                }
                
                detekt {
                    config.setFrom(layout.projectDirectory.file("../../config/detekt/detekt.yml"))
                    buildUponDefaultConfig = true
                }
                
                java {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                
                tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
                    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
                }
                
                dependencies {
                    implementation(libs.bundles.gradlePluginBundle)
                    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
                }
                
                gradlePlugin {
                    plugins {
                        register("KmpPlugin") {
                            id =
                                libs.plugins.kmpPlugin
                                    .get()
                                    .pluginId
                            implementationClass = "$BASE_PACKAGE.convention.KmpPlugin"
                        }
                        register("KmpPresentationPlugin") {
                            id =
                                libs.plugins.kmpPresentationPlugin
                                    .get()
                                    .pluginId
                            implementationClass = "$BASE_PACKAGE.convention.KmpPresentationPlugin"
                        }
                        register("KmpDataPlugin") {
                            id =
                                libs.plugins.kmpDataPlugin
                                    .get()
                                    .pluginId
                            implementationClass = "$BASE_PACKAGE.convention.KmpDataPlugin"
                        }
                        register("KmpDomainPlugin") {
                            id =
                                libs.plugins.kmpDomainPlugin
                                    .get()
                                    .pluginId
                            implementationClass = "$BASE_PACKAGE.convention.KmpDomainPlugin"
                        }
                        register("KmpDiPlugin") {
                            id =
                                libs.plugins.kmpDiPlugin
                                    .get()
                                    .pluginId
                            implementationClass = "$BASE_PACKAGE.convention.KmpDiPlugin"
                        }
                    }
                }

                """.trimIndent(),
            )

        private fun getConfigKtData() =
            Arguments.of(
                "build-logic/convention/src/main/kotlin/" +
                    BASE_PACKAGE.replace(".", "/") +
                    "/utils/Config.kt",
                """
                package $BASE_PACKAGE.utils

                import org.jetbrains.kotlin.gradle.dsl.JvmTarget
                
                internal sealed interface Config {
                    public data class Detekt(
                        public val configFilePath: String = "/config/detekt/detekt.yml",
                        public val buildUponDefaultConfig: Boolean = true,
                        public val allRules: Boolean = false,
                        public val inclusions: List<String> =
                            listOf(
                                "**/*.kt",
                                "**/*.kts",
                            ),
                        public val exclusions: List<String> =
                            listOf(
                                "**/resources/**",
                                "**/build/**",
                            ),
                    ) : Config
                
                    public data class Ktlint(
                        public val version: String = "1.8.0",
                        public val verbose: Boolean = true,
                        public val outputToConsole: Boolean = true,
                    ) : Config
                
                    public data class KotlinMultiplatform(
                        public val freeCompilerArgs: List<String> = listOf("-Xexpect-actual-classes"),
                        public val namespace: String = "$NAMESPACE.common",
                        public val compileSdk: Int = 36,
                        public val minSdk: Int = 24,
                        public val enableAndroidResources: Boolean = true,
                        public val jvmTarget: JvmTarget = JvmTarget.JVM_17,
                    ) : Config
                
                    public data class Library(
                        public val namespace: String = "$NAMESPACE",
                        public val compileSdk: Int = 36,
                        public val minSdk: Int = 24,
                        public val jvmToolChain: Int = 17,
                        public val sourceSetTreeName: String = "test",
                        public val instrumentationRunner: String = "androidx.test.runner.AndroidJUnitRunner",
                        public val enableAndroidResources: Boolean = true,
                        public val jvmTarget: JvmTarget = JvmTarget.JVM_17,
                    ) : Config
                }

                """.trimIndent(),
            )

        private fun getAndroidTargetData() =
            Arguments.of(
                "build-logic/convention/src/main/kotlin/" +
                    BASE_PACKAGE.replace(".", "/") +
                    "/utils/ConfigureAndroidTarget.kt",
                $$"""
                    package $$BASE_PACKAGE.utils
                    
                    import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
                    import com.sarimmehdi.Config
                    import org.gradle.api.Project
                    import org.gradle.api.plugins.ExtensionAware
                    import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
                    
                    internal fun KotlinMultiplatformExtension.configureAndroidTarget(target: Project) {
                        val libraryConfig = Config.Library()
                        (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("android") {
                            val formattedPath = target.path.removePrefix(":").replace(":", ".")
                            namespace = "${libraryConfig.namespace}.$formattedPath"
                    
                            compileSdk = libraryConfig.compileSdk
                            minSdk = libraryConfig.minSdk
                    
                            withDeviceTestBuilder { sourceSetTreeName = libraryConfig.sourceSetTreeName }
                                .configure { instrumentationRunner = libraryConfig.instrumentationRunner }
                    
                            androidResources { enable = libraryConfig.enableAndroidResources }
                        }
                    }

                """.trimIndent(),
            )

        private fun getQualityToolsData() =
            Arguments.of(
                "build-logic/convention/src/main/kotlin/" +
                    BASE_PACKAGE.replace(".", "/") +
                    "/utils/ConfigureQualityTools.kt",
                $$"""
                    package $$BASE_PACKAGE.utils
                    
                    import io.gitlab.arturbosch.detekt.Detekt
                    import io.gitlab.arturbosch.detekt.extensions.DetektExtension
                    import org.gradle.api.Project
                    import org.gradle.kotlin.dsl.configure
                    import org.gradle.kotlin.dsl.withType
                    import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
                    import org.jlleitschuh.gradle.ktlint.KtlintExtension
                    
                    internal fun Project.configureQualityTools() {
                        val detektConfig = Config.Detekt()
                        val ktlintConfig = Config.Ktlint()
                    
                        extensions.configure<DetektExtension> {
                            toolVersion = libs.versions.detektVersion.get()
                            config.setFrom(files("$rootDir${detektConfig.configFilePath}"))
                            buildUponDefaultConfig = detektConfig.buildUponDefaultConfig
                            allRules = detektConfig.allRules
                        }
                    
                        extensions.configure<KtlintExtension> {
                            version.set(ktlintConfig.version)
                            verbose.set(ktlintConfig.verbose)
                            outputToConsole.set(ktlintConfig.outputToConsole)
                            filter { exclude { it.file.path.contains("build/") } }
                        }
                    
                        tasks.withType<Detekt>().configureEach {
                            val buildDir =
                                project.layout.buildDirectory
                                    .get()
                                    .asFile.absolutePath
                            val nonGeneratedSourceDirs =
                                kotlinExtension.sourceSets
                                    .flatMap { it.kotlin.srcDirs }
                                    .filter { dir -> dir.exists() && !dir.absolutePath.contains(buildDir) }
                    
                            setSource(files(nonGeneratedSourceDirs))
                            detektConfig.inclusions.forEach { include(it) }
                            detektConfig.exclusions.forEach { exclude(it) }
                        }
                    }

                """.trimIndent(),
            )

        private fun getDataExtensionData() =
            Arguments.of(
                "build-logic/convention/src/main/kotlin/" +
                    BASE_PACKAGE.replace(".", "/") +
                    "/utils/KmpDataExtension.kt",
                $$"""
                    package $$BASE_PACKAGE.utils
                    
                    import kotlin.Boolean
                    import org.gradle.api.provider.Property
                    
                    public interface KmpDataExtension {
                        public val useRoom: Property<Boolean>
                        public val useDatastore: Property<Boolean>
                    }

                """.trimIndent(),
            )

        private fun getDiExtensionData() =
            Arguments.of(
                "build-logic/convention/src/main/kotlin/" +
                    BASE_PACKAGE.replace(".", "/") +
                    "/utils/KmpDiExtension.kt",
                $$"""
                        package $$BASE_PACKAGE.utils
                        
                        import kotlin.Boolean
                        
                        public open class KmpDiExtension {
                            public var useRoom: Boolean = false
                            public var useDatastore: Boolean = false
                        }
                        
                """.trimIndent(),
            )

        private fun getProjectExtsData() =
            Arguments.of(
                "build-logic/convention/src/main/kotlin/" +
                    BASE_PACKAGE.replace(".", "/") +
                    "/utils/ProjectExts.kt",
                $$"""
                        package $$BASE_PACKAGE.utils
                        
                        import org.gradle.accessors.dm.LibrariesForLibs
                        import org.gradle.api.Project
                        import org.gradle.api.plugins.PluginManager
                        import org.gradle.api.provider.Provider
                        import org.gradle.api.provider.ProviderConvertible
                        import org.gradle.kotlin.dsl.the
                        import org.gradle.plugin.use.PluginDependency
                        
                        internal val Project.libs: LibrariesForLibs
                            get() = the()
                        
                        internal fun PluginManager.alias(notation: Provider<PluginDependency>) {
                            apply(notation.get().pluginId)
                        }
                        
                        internal fun PluginManager.alias(notation: ProviderConvertible<PluginDependency>) {
                            apply(notation.asProvider().get().pluginId)
                        }
                        
                """.trimIndent(),
            )

        private fun getKmpDataPluginData() =
            Arguments.of(
                "build-logic/convention/src/main/kotlin/" +
                    BASE_PACKAGE.replace(".", "/") +
                    "/KmpDataPlugin.kt",
                $$"""
                    package $$BASE_PACKAGE
                    
                    import androidx.room.gradle.RoomExtension
                    import com.google.devtools.ksp.gradle.KspExtension
                    import $$BASE_PACKAGE.utils.KmpDataExtension
                    import $$BASE_PACKAGE.utils.configureAndroidTarget
                    import $$BASE_PACKAGE.utils.configureQualityTools
                    import $$BASE_PACKAGE.utils.libs
                    import org.gradle.api.Plugin
                    import org.gradle.api.Project
                    import org.gradle.kotlin.dsl.configure
                    import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
                    
                    internal class KmpDataPlugin : Plugin<Project> {
                        override fun apply(target: Project) {
                            val dataExtension =
                                target.extensions.create("kmpData", KmpDataExtension::class.java).apply {
                                    useRoom.convention(false)
                                    useDatastore.convention(false)
                                }
                    
                            with(target) {
                                applyBasePlugins()
                                configureQualityTools()
                                extensions.configure<KotlinMultiplatformExtension> {
                                    configureAndroidTarget(this@with)
                                }
                                configureRoomSettings()
                                afterEvaluate {
                                    configureKmpDependencies(dataExtension)
                                    if (dataExtension.useRoom.get()) {
                                        dependencies.add("kspCommonMainMetadata", libs.roomCompilerLibrary.get())
                                        dependencies.add("kspAndroid", libs.roomCompilerLibrary.get())
                                    }
                                }
                            }
                        }
                    
                        private fun Project.applyBasePlugins() {
                            pluginManager.apply(
                                libs.plugins.kotlinMultiplatformPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.androidKotlinMultiplatformLibrary
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.detektPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.ktlintPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.kspPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.roomPlugin
                                    .get()
                                    .pluginId,
                            )
                        }
                    
                        private fun Project.configureRoomSettings() {
                            extensions.configure<KspExtension> {
                                arg("room.generateKotlin", "true")
                            }
                            extensions.configure<RoomExtension> {
                                schemaDirectory("$projectDir/schemas")
                            }
                        }
                    
                        private fun Project.configureKmpDependencies(dataExtension: KmpDataExtension) {
                            extensions.configure<KotlinMultiplatformExtension> {
                                sourceSets.named("commonMain") {
                                    dependencies {
                                        implementation(libs.bundles.kotlinxEssentialsBundle)
                    
                                        if (dataExtension.useRoom.get()) {
                                            implementation(libs.bundles.roomCommonBundle)
                                        }
                                        if (dataExtension.useDatastore.get()) {
                                            implementation(libs.bundles.datastoreBundle)
                                        }
                                    }
                                }
                            }
                        }
                    }

                """.trimIndent(),
            )

        private fun getKmpDiPluginData() =
            Arguments.of(
                "build-logic/convention/src/main/kotlin/" +
                    BASE_PACKAGE.replace(".", "/") +
                    "/KmpDiPlugin.kt",
                $$"""
                    package $$BASE_PACKAGE
                    
                    import $$BASE_PACKAGE.utils.KmpDiExtension
                    import $$BASE_PACKAGE.utils.configureAndroidTarget
                    import $$BASE_PACKAGE.utils.configureQualityTools
                    import $$BASE_PACKAGE.utils.libs
                    import org.gradle.api.Plugin
                    import org.gradle.api.Project
                    import org.gradle.kotlin.dsl.configure
                    import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
                    
                    internal class KmpDiPlugin : Plugin<Project> {
                        override fun apply(target: Project) {
                            val diExtension = target.extensions.create("kmpDi", KmpDiExtension::class.java)
                    
                            with(target) {
                                pluginManager.apply {
                                    apply(
                                        libs.plugins.kotlinMultiplatformPlugin
                                            .get()
                                            .pluginId,
                                    )
                                    apply(
                                        libs.plugins.androidKotlinMultiplatformLibrary
                                            .get()
                                            .pluginId,
                                    )
                                    apply(
                                        libs.plugins.detektPlugin
                                            .get()
                                            .pluginId,
                                    )
                                    apply(
                                        libs.plugins.ktlintPlugin
                                            .get()
                                            .pluginId,
                                    )
                                }
                    
                                configureQualityTools()
                    
                                extensions.configure<KotlinMultiplatformExtension> {
                                    configureAndroidTarget(target)
                    
                                    afterEvaluate {
                                        sourceSets.apply {
                                            getByName("commonMain").dependencies {
                                                implementation(libs.bundles.koinCommonBundle)
                                                implementation(libs.bundles.kotlinxEssentialsBundle)
                    
                                                if (diExtension.useRoom) {
                                                    implementation(libs.bundles.roomCommonBundle)
                                                }
                                                if (diExtension.useDatastore) {
                                                    implementation(libs.bundles.datastoreBundle)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                """.trimIndent(),
            )

        private fun getKmpDomainPluginData() =
            Arguments.of(
                "build-logic/convention/src/main/kotlin/" +
                    BASE_PACKAGE.replace(".", "/") +
                    "/KmpDomainPlugin.kt",
                $$"""
                    package $$BASE_PACKAGE
                    
                    import $$BASE_PACKAGE.utils.configureQualityTools
                    import $$BASE_PACKAGE.utils.libs
                    import org.gradle.api.Plugin
                    import org.gradle.api.Project
                    import org.gradle.kotlin.dsl.configure
                    import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
                    
                    internal class KmpDomainPlugin : Plugin<Project> {
                        override fun apply(target: Project) {
                            with(target) {
                                pluginManager.apply {
                                    apply(
                                        libs.plugins.kotlinMultiplatformPlugin
                                            .get()
                                            .pluginId,
                                    )
                                    apply(
                                        libs.plugins.detektPlugin
                                            .get()
                                            .pluginId,
                                    )
                                    apply(
                                        libs.plugins.ktlintPlugin
                                            .get()
                                            .pluginId,
                                    )
                                }
                                configureQualityTools()
                                extensions.configure<KotlinMultiplatformExtension> {
                                    jvm()
                                    sourceSets.commonMain.dependencies { implementation(libs.bundles.kotlinxEssentialsBundle) }
                                    sourceSets.commonTest.dependencies { implementation(libs.bundles.unitTestingBundle) }
                                }
                            }
                        }
                    }

                """.trimIndent(),
            )

        private fun getKmpPluginData() =
            Arguments.of(
                "build-logic/convention/src/main/kotlin/" +
                    BASE_PACKAGE.replace(".", "/") +
                    "/KmpPlugin.kt",
                $$"""
                    package $$BASE_PACKAGE
                    
                    import androidx.room.gradle.RoomExtension
                    import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
                    import $$BASE_PACKAGE.utils.Config
                    import $$BASE_PACKAGE.utils.configureQualityTools
                    import $$BASE_PACKAGE.utils.libs
                    import org.gradle.api.Plugin
                    import org.gradle.api.Project
                    import org.gradle.api.plugins.ExtensionAware
                    import org.gradle.kotlin.dsl.configure
                    import org.gradle.kotlin.dsl.dependencies
                    import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
                    import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
                    
                    internal class KmpPlugin : Plugin<Project> {
                        override fun apply(target: Project) {
                            val kotlinMultiplatformConfig = Config.KotlinMultiplatform()
                    
                            with(target) {
                                applyPlugins()
                                configureQualityTools()
                    
                                pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
                                    configureKotlinOptions(kotlinMultiplatformConfig)
                                    configureKmpAndroidLibrary(kotlinMultiplatformConfig)
                                    configureRoom()
                                    configureDependencies()
                                }
                            }
                        }
                    
                        private fun Project.applyPlugins() {
                            pluginManager.apply(
                                libs.plugins.kotlinMultiplatformPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.composeMultiplatformPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.composeCompilerPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.composeHotReloadPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.roomPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.androidLintPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.kspPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.detektPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply(
                                libs.plugins.ktlintPlugin
                                    .get()
                                    .pluginId,
                            )
                            pluginManager.apply("com.android.kotlin.multiplatform.library")
                        }
                    
                        private fun Project.configureKotlinOptions(config: Config.KotlinMultiplatform) {
                            tasks.withType(KotlinCompile::class.java).configureEach {
                                compilerOptions.freeCompilerArgs.addAll(config.freeCompilerArgs)
                                compilerOptions.jvmTarget.set(config.jvmTarget)
                            }
                        }
                    
                        private fun Project.configureKmpAndroidLibrary(config: Config.KotlinMultiplatform) {
                            extensions.configure<KotlinMultiplatformExtension> {
                                (this as ExtensionAware)
                                    .extensions
                                    .configure<KotlinMultiplatformAndroidLibraryExtension>("android") {
                                        namespace = config.namespace
                                        compileSdk = config.compileSdk
                                        minSdk = config.minSdk
                                        androidResources { enable = config.enableAndroidResources }
                                    }
                    
                                sourceSets.apply {
                                    commonMain.dependencies {
                                        implementation(libs.bundles.composeCoreBundle)
                                        implementation(libs.bundles.androidxLifecycleBundle)
                                        implementation(libs.bundles.koinCommonBundle)
                                        implementation(libs.bundles.kotlinxEssentialsBundle)
                                        implementation(libs.bundles.roomCommonBundle)
                                        implementation(libs.bundles.datastoreBundle)
                                        implementation(libs.composeUiToolingPreviewLibrary)
                                    }
                                    androidMain.dependencies { implementation(libs.bundles.androidUiSupportBundle) }
                                    commonTest.dependencies { implementation(libs.bundles.unitTestingBundle) }
                                }
                            }
                        }
                    
                        private fun Project.configureRoom() {
                            extensions.configure<RoomExtension> {
                                schemaDirectory("$projectDir/schemas")
                            }
                        }
                    
                        private fun Project.configureDependencies() {
                            dependencies {
                                "androidRuntimeClasspath"(libs.composeUiToolingLibrary)
                                add("kspAndroid", libs.roomCompilerLibrary)
                            }
                        }
                    }

                """.trimIndent(),
            )

        private fun getKmpPresentationPluginData() =
            Arguments.of(
                "build-logic/convention/src/main/kotlin/" +
                    BASE_PACKAGE.replace(".", "/") +
                    "/KmpPresentationPlugin.kt",
                $$"""
                    package $$BASE_PACKAGE
                    
                    import $$BASE_PACKAGE.utils.configureAndroidTarget
                    import $$BASE_PACKAGE.utils.configureQualityTools
                    import $$BASE_PACKAGE.utils.libs
                    import org.gradle.internal.Actions.with
                    import org.gradle.api.Plugin
                    import org.gradle.api.Project
                    import org.gradle.kotlin.dsl.configure
                    import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
                    
                    internal class KmpPresentationPlugin : Plugin<Project> {
                        override fun apply(target: Project) {
                            with(target) {
                                pluginManager.apply {
                                    apply(
                                        libs.plugins.kotlinMultiplatformPlugin
                                            .get()
                                            .pluginId,
                                    )
                                    apply(
                                        libs.plugins.androidKotlinMultiplatformLibrary
                                            .get()
                                            .pluginId,
                                    )
                                    apply(
                                        libs.plugins.composeMultiplatformPlugin
                                            .get()
                                            .pluginId,
                                    )
                                    apply(
                                        libs.plugins.composeCompilerPlugin
                                            .get()
                                            .pluginId,
                                    )
                                    apply(
                                        libs.plugins.detektPlugin
                                            .get()
                                            .pluginId,
                                    )
                                    apply(
                                        libs.plugins.ktlintPlugin
                                            .get()
                                            .pluginId,
                                    )
                                }
                                configureQualityTools()
                                extensions.configure<KotlinMultiplatformExtension> {
                                    configureAndroidTarget(target)
                                    sourceSets.apply {
                                        commonMain.dependencies {
                                            implementation(libs.bundles.composeCoreBundle)
                                            implementation(libs.bundles.androidxLifecycleBundle)
                                            implementation(libs.bundles.kotlinxEssentialsBundle)
                                            implementation(libs.composeUiToolingPreviewLibrary)
                                        }
                                        androidMain.dependencies { implementation(libs.bundles.androidUiSupportBundle) }
                                    }
                                }
                            }
                        }
                    }

                """.trimIndent(),
            )
    }
}
