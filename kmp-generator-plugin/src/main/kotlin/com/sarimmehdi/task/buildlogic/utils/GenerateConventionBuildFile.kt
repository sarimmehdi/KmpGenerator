package com.sarimmehdi.task.buildlogic.utils

import org.gradle.api.provider.Property
import java.io.File

internal fun generateConventionBuildFile(
    dir: File,
    packageName: Property<String>,
) {
    val pkg = packageName.get()
    val plugins = listOf("KmpPlugin", "KmpPresentationPlugin", "KmpDataPlugin", "KmpDomainPlugin", "KmpDiPlugin")

    val content =
        """
        import org.jetbrains.kotlin.gradle.dsl.JvmTarget
        
        plugins {
            `kotlin-dsl`
            alias(libs.plugins.detektPlugin)
            alias(libs.plugins.ktlintPlugin)
        }
        
        group = "$pkg.buildlogic"
        
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
${plugins.joinToString("\n") { buildRegistrationBlock(it, pkg) }.prependIndent("                ")}
            }
        }
        """.trimIndent()

    File(dir, "build.gradle.kts").writeText(content)
}

private fun buildRegistrationBlock(
    name: String,
    pkg: String,
): String {
    val pluginAlias = name.replaceFirstChar { it.lowercase() }
    return """
        register("$name") {
            id =
                libs.plugins.$pluginAlias
                    .get()
                    .pluginId
            implementationClass = "$pkg.convention.$name"
        }
        """.trimIndent()
}
