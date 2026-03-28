package com.sarimmehdi

import com.sarimmehdi.task.buildlogic.GenerateBuildLogicTask
import com.sarimmehdi.task.domain.GenerateDomainModuleTask
import com.sarimmehdi.task.toml.TomlGenerator
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import javax.inject.Inject

abstract class KmpGeneratorExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
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

        val domain: NamedDomainObjectContainer<GenerateDomainModuleTask.GenerateDomainModuleConfig> =
            objects.domainObjectContainer(GenerateDomainModuleTask.GenerateDomainModuleConfig::class.java)

        @Suppress("unused")
        fun domain(action: Action<NamedDomainObjectContainer<GenerateDomainModuleTask.GenerateDomainModuleConfig>>) {
            action.execute(domain)
        }
    }
