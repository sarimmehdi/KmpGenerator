package com.sarimmehdi

import com.sarimmehdi.task.KmpHelpReporter
import com.sarimmehdi.task.StarterDataReporter
import com.sarimmehdi.task.TomlGenerator
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, KmpGeneratorExtension::class.java)

        project.tasks.register(TomlGenerator.TASK_NAME) {
            group = TASK_GROUP
            description = TomlGenerator.TASK_DESCRIPTION
            doLast {
                TomlGenerator.generate(
                    project = project,
                    extension = extension,
                    excludedLibraries = extension.excludedLibraries.getOrElse(emptyList()),
                    excludedPlugins = extension.excludedPlugins.getOrElse(emptyList()),
                    excludedBundles = extension.excludedBundles.getOrElse(emptyList())
                )
            }
        }
        project.tasks.register(StarterDataReporter.TASK_NAME) {
            group = TASK_GROUP
            description = StarterDataReporter.TASK_DESCRIPTION

            doLast {
                StarterDataReporter.printAll()
            }
        }
        project.tasks.register(KmpHelpReporter.TASK_NAME) {
            group = TASK_GROUP
            description = KmpHelpReporter.TASK_DESCRIPTION
            doLast {
                KmpHelpReporter.printHelp()
            }
        }
    }

    companion object {
        private const val EXTENSION_NAME = "kmpGenerator"
        private const val TASK_GROUP = "kmp-generator"
    }
}