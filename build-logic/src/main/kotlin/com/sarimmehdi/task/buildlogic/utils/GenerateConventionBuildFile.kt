package com.sarimmehdi.task.buildlogic.utils

import org.gradle.api.provider.Property
import java.io.File

internal fun generateConventionBuildFile(
    dir: File,
    packageName: Property<String>
) {
    val pkg = packageName.get()

    File(dir, "build.gradle.kts").writeText("""
        plugins {
            `kotlin-dsl`
        }

        group = "$pkg.buildlogic"

        java {
            sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
            targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            }
        }

        dependencies {
            // Accessing the version catalog bundle
            implementation(libs.bundles.gradlePluginBundle)
            
            // This allows the build-logic to see the classes of THIS plugin 
            // (useful for accessing StarterData or shared models)
            implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
        }
    """.trimIndent())
}