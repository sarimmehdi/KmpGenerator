package com.sarimmehdi

import com.sarimmehdi.task.domain.GenerateDomainModuleTask
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
class GenerateDomainModuleTaskTest {
    @TempDir
    lateinit var testProjectDir: File

    @BeforeEach
    fun setup() {
        val gradleDir = File(testProjectDir, "gradle").apply { mkdirs() }
        File(gradleDir, "libs.versions.toml").writeText(
            """
            [plugins]
            kmpDomainPlugin = "kmp.domain:1.0.0"
            """.trimIndent(),
        )

        File(testProjectDir, "build.gradle.kts").writeText(
            """
            import com.sarimmehdi.task.domain.model.DomainModel
            import com.sarimmehdi.task.domain.model.KotlinType
            import com.sarimmehdi.task.domain.model.UseCaseModel

            plugins {
                id("$BASE_PACKAGE.kmp-generator")
            }

            kmpGenerator {
                domain {
                    // Updated to NamedDomainObjectContainer syntax
                    register("$FEATURE") {
                        feature.set("$FEATURE")
                        namespace.set("$NAMESPACE")
                        dependencies.set(listOf(":sidebar:domain", ":historical:domain"))

                        models.set(listOf(
                            DomainModel.EnumClass(
                                name = "Language",
                                constants = listOf("ENGLISH", "GERMAN")
                            ),
                            DomainModel.DataClass(
                                name = "SomethingElse",
                                createRepository = true,
                                properties = mapOf(
                                    "id" to KotlinType.KotlinLong(isNullable = false),
                                    "selectedLanguage" to KotlinType.Custom("Language", isNullable = true),
                                    "tags" to KotlinType.KotlinList(
                                        itemType = KotlinType.KotlinString(isNullable = true),
                                        isNullable = false
                                    )
                                )
                            )
                        ))
                        usecases.set(listOf(
                            UseCaseModel(
                                name = "SomethingElse", 
                                repositoryDependencies = listOf("SomethingElse"), 
                                isVanilla = true
                            ),
                            UseCaseModel(
                                name = "Analytics", 
                                repositoryDependencies = listOf("SomethingElse"), 
                                isVanilla = false
                            )
                        ))
                    }
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `task should fail when domain namespace contains invalid characters`() {
        updateDomainBuildFile(invalidNamespace = "com.sarim-example")

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(GenerateDomainModuleTask.TASK_NAME)
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.output).contains("Invalid package name: 'com.sarim-example'")
    }

    @Test
    fun `task should fail when domain namespace starts with a number`() {
        updateDomainBuildFile(invalidNamespace = "123.com.example")

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(GenerateDomainModuleTask.TASK_NAME)
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.output).contains("Invalid package name: '123.com.example'")
    }

    @Test
    fun `task should fail when domain namespace contains uppercase letters`() {
        updateDomainBuildFile(invalidNamespace = "Com.Sarim.Example")

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(GenerateDomainModuleTask.TASK_NAME)
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.output).contains("Invalid package name: 'Com.Sarim.Example'")
    }

    @Test
    fun `task should fail when feature name contains hyphens`() {
        updateDomainBuildFile(invalidFeature = "user-profile")

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(GenerateDomainModuleTask.TASK_NAME)
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.output).contains("Invalid feature name: 'user-profile'")
    }

    @Test
    fun `task should fail when feature name starts with a number`() {
        updateDomainBuildFile(invalidFeature = "1profile")

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(GenerateDomainModuleTask.TASK_NAME)
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.output).contains("Invalid feature name: '1profile'")
    }

    @Test
    fun `task should fail when feature name contains dots`() {
        updateDomainBuildFile(invalidFeature = "user.profile")

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(GenerateDomainModuleTask.TASK_NAME)
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.output).contains("Invalid feature name: 'user.profile'")
    }

    private fun updateDomainBuildFile(
        invalidFeature: String = FEATURE,
        invalidNamespace: String = NAMESPACE,
    ) {
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            import com.sarimmehdi.task.domain.model.DomainModel
            import com.sarimmehdi.task.domain.model.UseCaseModel

            plugins {
                id("$BASE_PACKAGE.kmp-generator")
            }

            kmpGenerator {
                domain {
                    register("testFeature") {
                        feature.set("$invalidFeature")
                        namespace.set("$invalidNamespace")
                        models.set(listOf(
                            DomainModel.EnumClass("Language", listOf("EN"))
                        ))
                    }
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
            .withArguments(GenerateDomainModuleTask.TASK_NAME)
            .withPluginClasspath()
            .build()

        val file = File(testProjectDir, relativePath)

        assertThat(file)
            .describedAs("File should exist at: $relativePath")
            .exists()

        assertThat(file.readText())
            .describedAs("Checking content for file: $relativePath")
            .isEqualToIgnoringWhitespace(expectedContent)
    }

    @Test
    fun `task should fail when usecase refers to non-existent repository`() {
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            import com.sarimmehdi.task.domain.model.DomainModel
            import com.sarimmehdi.task.domain.model.KotlinType
            import com.sarimmehdi.task.domain.model.UseCaseModel
            
            plugins {
                id("$BASE_PACKAGE.kmp-generator")
            }
            
            kmpGenerator {
                domain {
                    register("test") {
                        feature.set("test")
                        namespace.set("com.example")
                        models.set(listOf(
                            DomainModel.DataClass("Existing", properties = emptyMap(), createRepository = false)
                        ))
                        usecases.set(listOf(
                            UseCaseModel("Broken", repositoryDependencies = listOf("DoesNotExist"))
                        ))
                    }
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(GenerateDomainModuleTask.TASK_NAME)
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.output).contains("UseCase 'Broken' depends on 'DoesNotExist'")
    }

    @Test
    fun `verify cross-module dependency generation`() {
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            import com.sarimmehdi.task.domain.model.*

            plugins {
                id("$BASE_PACKAGE.kmp-generator")
            }

            kmpGenerator {
                domain {
                    register("something") {
                        feature.set("something")
                        namespace.set("$NAMESPACE")
                        models.set(listOf(
                            DomainModel.DataClass("Something", createRepository = true, properties = emptyMap())
                        ))
                    }

                    register("sidebar") {
                        feature.set("sidebar")
                        namespace.set("$NAMESPACE")
                        dependencies.set(listOf(":something:domain"))
                        usecases.set(listOf(
                            UseCaseModel(
                                name = "Sidebar",
                                // This UseCase uses a local repo AND external dependencies
                                externalDependencies = listOf(
                                    ExternalDependency("SomethingUseCase", "something", "$NAMESPACE"),
                                    ExternalDependency("SomethingRepository", "something", "$NAMESPACE")
                                )
                            )
                        ))
                    }
                }
            }
            """.trimIndent(),
        )

        GradleRunner
            .create()
            .withProjectDir(testProjectDir)
            .withArguments(GenerateDomainModuleTask.TASK_NAME)
            .withPluginClasspath()
            .build()

        val sidebarFile =
            File(
                testProjectDir,
                "sidebar/domain/src/commonMain/kotlin/" +
                    NAMESPACE.replace(".", "/") +
                    "/sidebar/domain/usecase/SidebarUseCase.kt",
            )

        assertThat(sidebarFile).exists()
        val content = sidebarFile.readText()

        assertThat(content).contains("import $NAMESPACE.something.domain.usecase.SomethingUseCase")
        assertThat(content).contains("import $NAMESPACE.something.domain.repository.SomethingRepository")

        assertThat(content).contains("private val somethingUseCase: SomethingUseCase")
        assertThat(content).contains("private val somethingRepository: SomethingRepository")
    }

    @Test
    fun `task should fail when external dependency is used without gradle project dependency`() {
        File(testProjectDir, "build.gradle.kts").writeText(
            """
            import com.sarimmehdi.task.domain.model.*

            plugins {
                id("$BASE_PACKAGE.kmp-generator")
            }

            kmpGenerator {
                domain {
                    register("something") {
                        feature.set("something")
                        namespace.set("$NAMESPACE")
                    }

                    register("sidebar") {
                        feature.set("sidebar")
                        namespace.set("$NAMESPACE")
                        // MISSING: dependencies.set(listOf(":something:domain"))
                        
                        usecases.set(listOf(
                            UseCaseModel(
                                name = "Sidebar",
                                externalDependencies = listOf(
                                    ExternalDependency("SomethingUseCase", "something", "$NAMESPACE")
                                )
                            )
                        ))
                    }
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withArguments(GenerateDomainModuleTask.TASK_NAME)
                .withPluginClasspath()
                .buildAndFail()

        assertThat(result.output)
            .contains("UseCase 'Sidebar' in feature 'sidebar' uses 'SomethingUseCase' from 'something'")
        assertThat(result.output).contains("but ':something:domain' is missing from dependencies")
    }

    companion object {
        private const val BASE_PACKAGE = "com.sarimmehdi"
        private const val FEATURE = "feature"
        private const val NAMESPACE = "com.sarim.example"

        @JvmStatic
        fun provideFileVerificationData(): Stream<Arguments> =
            Stream.of(
                getGitIgnoreData(),
                getBuildGradleData(),
                getLanguageEnumData(),
                getModelData(),
                getRepositoryData(),
                getVanillaUseCaseData(),
                getCustomUseCaseData(),
            )

        private fun getGitIgnoreData() =
            Arguments.of(
                "$FEATURE/domain/.gitignore",
                """
                /build
                """.trimIndent(),
            )

        private fun getBuildGradleData() =
            Arguments.of(
                "$FEATURE/domain/build.gradle.kts",
                """
                plugins {
                    alias(libs.plugins.kmpDomainPlugin)
                }
                
                kotlin {
                    sourceSets {
                        commonMain.dependencies {
                            implementation(project(":sidebar:domain"))
                            implementation(project(":historical:domain"))
                        }
                    }
                }
                """.trimIndent(),
            )

        private fun getLanguageEnumData() =
            Arguments.of(
                "$FEATURE/domain/src/commonMain/kotlin" +
                    "/${NAMESPACE.replace(".", "/")}/$FEATURE/" +
                    "domain/model/Language.kt",
                """
                package $NAMESPACE.$FEATURE.domain.model

                public enum class Language {
                  ENGLISH,
                  GERMAN,
                }
                """.trimIndent(),
            )

        private fun getModelData() =
            Arguments.of(
                "$FEATURE/domain/src/commonMain/kotlin" +
                    "/${NAMESPACE.replace(".", "/")}/" +
                    "$FEATURE/domain/model/SomethingElse.kt",
                """
                package $NAMESPACE.$FEATURE.domain.model

                import kotlin.Long
                import kotlin.String
                import kotlin.collections.List

                public data class SomethingElse(
                  public val id: Long,
                  public val selectedLanguage: Language?,
                  public val tags: List<String?>,
                )
                """.trimIndent(),
            )

        private fun getRepositoryData() =
            Arguments.of(
                "$FEATURE/domain/src/commonMain/kotlin" +
                    "/${NAMESPACE.replace(".", "/")}/" +
                    "$FEATURE/domain/repository/SomethingElseRepository.kt",
                """
                package $NAMESPACE.$FEATURE.domain.repository
                
                import $NAMESPACE.$FEATURE.domain.model.Language
                import $NAMESPACE.$FEATURE.domain.model.SomethingElse
                import kotlin.Long
                import kotlin.String
                import kotlin.collections.List
                import kotlinx.coroutines.flow.Flow
                
                public interface SomethingElseRepository {
                  public val somethingElse: Flow<SomethingElse>
                  
                  public suspend fun updateId(id: Long)
                
                  public suspend fun updateSelectedLanguage(selectedLanguage: Language?)
                
                  public suspend fun updateTags(tags: List<String?>)
                }
                """.trimIndent(),
            )

        private fun getVanillaUseCaseData() =
            Arguments.of(
                "$FEATURE/domain/src/commonMain/kotlin" +
                    "/${NAMESPACE.replace(".", "/")}/" +
                    "$FEATURE/domain/usecase/SomethingElseUseCase.kt",
                """
                package $NAMESPACE.$FEATURE.domain.usecase
                
                import $NAMESPACE.$FEATURE.domain.model.Language
                import $NAMESPACE.$FEATURE.domain.model.SomethingElse
                import $NAMESPACE.$FEATURE.domain.repository.SomethingElseRepository
                import kotlin.Long
                import kotlin.String
                import kotlin.Unit
                import kotlin.collections.List
                import kotlinx.coroutines.flow.Flow
                
                public class SomethingElseUseCase(
                  private val somethingElseRepository: SomethingElseRepository,
                ) {
                  public val somethingElse: Flow<SomethingElse> = somethingElseRepository.somethingElse
                
                  public suspend fun updateId(id: Long): Unit = somethingElseRepository.updateId(id)
                
                  public suspend fun updateSelectedLanguage(selectedLanguage: Language?): Unit =
                      somethingElseRepository.updateSelectedLanguage(selectedLanguage)
                
                  public suspend fun updateTags(tags: List<String?>): Unit = somethingElseRepository.updateTags(tags)
                }
                """.trimIndent(),
            )

        private fun getCustomUseCaseData() =
            Arguments.of(
                "$FEATURE/domain/src/commonMain/kotlin" +
                    "/${NAMESPACE.replace(".", "/")}/" +
                    "$FEATURE/domain/usecase/AnalyticsUseCase.kt",
                """
                package $NAMESPACE.$FEATURE.domain.usecase
                
                import $NAMESPACE.$FEATURE.domain.repository.SomethingElseRepository
                
                public class AnalyticsUseCase(
                  private val somethingElseRepository: SomethingElseRepository,
                )
                """.trimIndent(),
            )
    }
}
