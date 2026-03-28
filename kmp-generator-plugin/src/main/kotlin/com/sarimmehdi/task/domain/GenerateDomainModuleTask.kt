package com.sarimmehdi.task.domain

import com.sarimmehdi.task.domain.model.DomainModel
import com.sarimmehdi.task.domain.model.UseCaseModel
import com.sarimmehdi.task.domain.utils.generateModelFiles
import com.sarimmehdi.task.domain.utils.generateModuleRootFiles
import com.sarimmehdi.task.domain.utils.generateRepositoryFiles
import com.sarimmehdi.task.domain.utils.generateUseCaseFiles
import com.sarimmehdi.task.utils.validatePackageName
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateDomainModuleTask : DefaultTask() {
    interface GenerateDomainModuleConfig {
        @get:Input
        val name: String
        val feature: Property<String>
        val namespace: Property<String>
        val models: ListProperty<DomainModel>
        val dependencies: ListProperty<String>
        val usecases: ListProperty<UseCaseModel>
    }

    @get:Input
    abstract val moduleConfigs: ListProperty<GenerateDomainModuleConfig>

    @get:OutputDirectory
    abstract val projectRoot: DirectoryProperty

    @TaskAction
    fun generate() {
        moduleConfigs.get().forEach { config ->
            generateSingleModule(config)
        }
    }

    private fun generateSingleModule(config: GenerateDomainModuleConfig) {
        val featureName = config.feature.get().lowercase()
        val baseNamespace = config.namespace.get()
        val deps = config.dependencies.getOrElse(emptyList())
        val domainModels = config.models.getOrElse(emptyList())
        val usecaseModels = config.usecases.getOrElse(emptyList())

        validateCrossModuleDependencies(featureName, deps, usecaseModels)
        validatePackageName(baseNamespace)
        validateFeatureName(featureName)
        validateDependencyPaths(featureName, deps)

        val moduleDir = File(projectRoot.get().asFile, "$featureName/domain")
        if (!moduleDir.exists()) moduleDir.mkdirs()

        val kotlinRootDir = moduleDir.resolve("src/commonMain/kotlin")
        if (!kotlinRootDir.exists()) kotlinRootDir.mkdirs()

        generateModuleRootFiles(moduleDir, deps)

        val basePackage = "$baseNamespace.$featureName.domain"
        generateModelFiles(domainModels, basePackage, kotlinRootDir)
        generateRepositoryFiles(domainModels, basePackage, kotlinRootDir)
        generateUseCaseFiles(usecaseModels, domainModels, basePackage, kotlinRootDir)

        println("Domain module for '$featureName' scaffolded at ${moduleDir.path}")
    }

    private fun validateCrossModuleDependencies(
        featureName: String,
        deps: List<String>,
        usecaseModels: List<UseCaseModel>,
    ) {
        usecaseModels.forEach { useCase ->
            useCase.externalDependencies.forEach { ext ->
                val expectedProjectDep = ":${ext.featureName}:domain"
                if (!deps.contains(expectedProjectDep)) {
                    error(
                        "UseCase '${useCase.name}' in feature '$featureName' uses '${ext.name}' " +
                            "from '${ext.featureName}', but '$expectedProjectDep' is missing from dependencies",
                    )
                }
            }
        }
    }

    private fun validateDependencyPaths(
        featureName: String,
        deps: List<String>,
    ) {
        deps.forEach {
            require(it.startsWith(":")) {
                "Invalid module dependency '$it' in feature '$featureName'. Paths must start with ':'"
            }
        }
    }

    private fun validateFeatureName(name: String) {
        val regex = "^[a-z][a-z0-9_]*$".toRegex()

        require(regex.matches(name)) {
            "Invalid feature name: '$name'. Feature names must be lowercase, " +
                "start with a letter, and contain only alphanumeric characters or underscores."
        }
    }

    companion object {
        const val TASK_NAME = "generateDomainModules"
        const val TASK_DESCRIPTION = "Generates domain modules for features using type-safe models."

        const val TASK_EXAMPLE = """
            // 1. Configure in build.gradle.kts:
            import com.sarimmehdi.task.domain.model.DomainModel
            import com.sarimmehdi.task.domain.model.KotlinType
            import com.sarimmehdi.task.domain.model.UseCaseModel
            import com.sarimmehdi.task.domain.model.ExternalDependency
            
            kmpGenerator {
                domain {
                    // First module: Header
                    register("header") {
                        feature.set("header")
                        namespace.set("com.sarim.example")
                        models.set(listOf(
                            DomainModel.DataClass(
                                name = "Header",
                                createRepository = true,
                                properties = mapOf("title" to KotlinType.KotlinString())
                            )
                        ))
                        usecases.set(listOf(
                            UseCaseModel(name = "Header", repositoryDependencies = listOf("Header"))
                        ))
                    }
            
                    // Second module: User Profile (Depends on Header)
                    register("user_profile") {
                        feature.set("user_profile")
                        namespace.set("com.sarim.example")
                        
                        // Gradle project dependency
                        dependencies.set(listOf(":header:domain"))
                        
                        models.set(listOf(
                            DomainModel.DataClass(
                                name = "UserProfile",
                                createRepository = true,
                                properties = mapOf(
                                    "id" to KotlinType.KotlinLong(),
                                    "username" to KotlinType.KotlinString()
                                )
                            )
                        ))
                        
                        usecases.set(listOf(
                            UseCaseModel(
                                name = "Profile",
                                // Local repository dependency
                                repositoryDependencies = listOf("UserProfile"),
                                // External dependency from the 'header' module
                                externalDependencies = listOf(
                                    ExternalDependency(
                                        name = "HeaderUseCase",
                                        featureName = "header",
                                        namespace = "com.sarim.example"
                                    )
                                ),
                                isVanilla = false
                            )
                        ))
                    }
                }
            }
            
            // 2. Run the task to scaffold all modules:
            ./gradlew $TASK_NAME
            """

        fun register(
            project: Project,
            taskGroup: String,
            configs: ListProperty<GenerateDomainModuleConfig>,
        ) {
            project.tasks.register(TASK_NAME, GenerateDomainModuleTask::class.java) {
                group = taskGroup
                description = TASK_DESCRIPTION
                this.moduleConfigs.set(configs)
                this.projectRoot.set(project.layout.projectDirectory)
            }
        }
    }
}
