package com.sarimmehdi

import com.sarimmehdi.model.Bundle
import com.sarimmehdi.model.Library
import com.sarimmehdi.model.Plugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

interface KmpGeneratorExtension {
    val additionalLibraries: ListProperty<Library>
    val additionalPlugins: ListProperty<Plugin>
    val additionalBundles: ListProperty<Bundle>

    val excludedLibraries: ListProperty<Library>
    val excludedPlugins: ListProperty<Plugin>
    val excludedBundles: ListProperty<Bundle>
    val overwriteExisting: Property<Boolean>
}