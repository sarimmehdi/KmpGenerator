package com.sarimmehdi

import com.sarimmehdi.task.buildlogic.GenerateBuildLogicTask
import com.sarimmehdi.task.toml.TomlGenerator
import org.gradle.api.Action
import org.gradle.api.tasks.Nested

abstract class KmpGeneratorExtension {
    @get:Nested
    abstract val toml: TomlGenerator.TomlConfig

    @Suppress("unused")
    fun toml(action: Action<TomlGenerator.TomlConfig>) {
        action.execute(toml)
    }

    @get:Nested
    abstract val buildLogic: GenerateBuildLogicTask.GenerateBuildLogicConfig

    @Suppress("unused")
    fun buildLogic(action: Action<GenerateBuildLogicTask.GenerateBuildLogicConfig>) {
        action.execute(buildLogic)
    }
}
