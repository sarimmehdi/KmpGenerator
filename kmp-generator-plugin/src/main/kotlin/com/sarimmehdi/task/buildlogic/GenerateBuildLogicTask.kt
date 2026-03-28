package com.sarimmehdi.task.buildlogic

import com.sarimmehdi.task.buildlogic.utils.generateConfigureAndroidTarget
import com.sarimmehdi.task.buildlogic.utils.generateConfigureQualityTools
import com.sarimmehdi.task.buildlogic.utils.generateConventionBuildFile
import com.sarimmehdi.task.buildlogic.utils.generateKmpDataExtension
import com.sarimmehdi.task.buildlogic.utils.generateKmpDiExtension
import com.sarimmehdi.task.buildlogic.utils.generateProjectExts
import com.sarimmehdi.task.buildlogic.utils.generateRootFiles
import com.sarimmehdi.task.buildlogic.utils.generateUtilsConfig
import com.sarimmehdi.task.utils.validatePackageName
import com.squareup.kotlinpoet.TypeName
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateBuildLogicTask : DefaultTask() {
    interface GenerateBuildLogicConfig {
        val basePackage: Property<String>
        val namespace: Property<String>
    }

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val namespace: Property<String>

    @TaskAction
    fun generate() {
        val pkg = packageName.get()
        val name = namespace.get()
        validatePackageName(pkg)

        val root = outputDir.get().asFile
        val conventionDir = File(root, "convention")

        if (!root.exists()) root.mkdirs()

        val packagePath = pkg.replace(".", "/")
        val kotlinSrcDir = File(conventionDir, "src/main/kotlin/$packagePath")

        kotlinSrcDir.mkdirs()

        generateRootFiles(root)
        generateConventionBuildFile(conventionDir, packageName)
        generateUtilsConfig(pkg, name, outputDir)
        generateConfigureAndroidTarget(pkg, outputDir)
        generateConfigureQualityTools(pkg, outputDir)
        generateProjectExts(pkg, outputDir)
        generateKmpDataExtension(pkg, outputDir)
        generateKmpDiExtension(pkg, outputDir)
        generateKmpPlugin(pkg, outputDir)
        generateKmpDataPlugin(pkg, outputDir)
        generateKmpDomainPlugin(pkg, outputDir)
        generateKmpPresentationPlugin(pkg, outputDir)
        generateKmpDiPlugin(pkg, outputDir)

        println("KmpGenerator: build-logic scaffolded successfully at ${root.path}")
    }

    internal data class ParameterData(
        val name: String,
        val type: TypeName,
        val defaultValueFormat: String,
        val args: List<Any> = emptyList(),
    )

    companion object {
        const val TASK_NAME = "generateBuildLogic"
        const val TASK_DESCRIPTION = "Scaffolds the build-logic directory with convention plugins."
        const val TASK_EXAMPLE =
            """
            // 1. Configure in build.gradle.kts:
            kmpGenerator {
                buildLogic {
                    basePackage.set("com.sarimmehdi")
                    namespace.set("example")
                }
            }
            
            // 2. Run the task:
            ./gradlew $TASK_NAME
            """

        fun register(
            project: Project,
            taskGroup: String,
            config: GenerateBuildLogicConfig,
        ) {
            project.tasks.register(TASK_NAME, GenerateBuildLogicTask::class.java) {
                group = taskGroup
                description = TASK_DESCRIPTION
                this.packageName.set(config.basePackage)
                this.namespace.set(config.namespace)
                this.outputDir.set(project.layout.projectDirectory.dir("build-logic"))
            }
        }
    }
}
