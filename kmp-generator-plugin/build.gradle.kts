plugins {
    `kotlin-dsl`
    `java-library`
    `maven-publish`
    alias(libs.plugins.gradlePublishPlugin)
    alias(libs.plugins.ktlintPlugin)
    alias(libs.plugins.detektPlugin)
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
