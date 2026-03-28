import io.gitlab.arturbosch.detekt.Detekt

plugins {
    `kotlin-dsl`
    `java-library`
    `maven-publish`
    jacoco
    alias(libs.plugins.gradlePublishPlugin)
    alias(libs.plugins.ktlintPlugin)
    alias(libs.plugins.detektPlugin)
    alias(libs.plugins.sonarPlugin)
}

sonar {
    properties {
        property("sonar.projectKey", "KmpGenerator")
        property("sonar.projectName", "KmpGenerator")
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/gradle/kotlin/dsl/**",
                        "**/*$*",
                        "**/KmpGeneratorPlugin_*",
                    )
                }
            },
        ),
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.withType<Detekt>().configureEach {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt-$name.xml"))

        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt-$name.html"))

        sarif.required.set(true)
        sarif.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt-$name.sarif"))
    }
}

ktlint {
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
    }
    outputToConsole.set(true)
    ignoreFailures.set(true)
}

dependencies {
    implementation(libs.kotlinPoetLibrary)

    testImplementation(gradleTestKit())
    testImplementation(libs.bundles.testBundle)

    testRuntimeOnly(libs.bundles.testRuntime)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    finalizedBy(tasks.jacocoTestReport)
}

group = "com.sarimmehdi"
version = "1.0.0"

gradlePlugin {
    website.set("https://github.com/sarimmehdi/kmp-generator")
    vcsUrl.set("https://github.com/sarimmehdi/kmp-generator")

    plugins {
        create("kmpGenerator") {
            id = "com.sarimmehdi.kmp-generator"
            displayName = "KMP Version Catalog Generator"
            description = "A plugin to forge and manage libs.version.toml files."
            tags.set(listOf("kmp", "toml", "version-catalog"))
            implementationClass = "com.sarimmehdi.KmpGeneratorPlugin"
        }
    }
}
