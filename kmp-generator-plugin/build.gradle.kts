import io.gitlab.arturbosch.detekt.Detekt

plugins {
    `kotlin-dsl`
    `java-library`
    `maven-publish`
    alias(libs.plugins.gradlePublishPlugin)
    alias(libs.plugins.ktlintPlugin)
    alias(libs.plugins.detektPlugin)
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
