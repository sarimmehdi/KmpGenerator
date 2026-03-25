package com.sarimmehdi

import com.sarimmehdi.task.KmpHelpReporter
import com.sarimmehdi.task.buildlogic.GenerateBuildLogicTask
import com.sarimmehdi.task.toml.StarterDataReporter
import com.sarimmehdi.task.toml.TomlGenerator
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("kmpGenerator", KmpGeneratorExtension::class.java)

        TomlGenerator.register(
            project = project,
            taskGroup = TASK_GROUP,
            config = extension.toml
        )

        StarterDataReporter.register(
            project = project,
            taskGroup = TASK_GROUP,
        )

        KmpHelpReporter.register(
            project = project,
            taskGroup = TASK_GROUP
        )

        GenerateBuildLogicTask.register(
            project = project,
            taskGroup = TASK_GROUP,
            config = extension.buildLogic
        )
    }

    companion object {
        const val TASK_GROUP = "kmp-generator"
    }
}