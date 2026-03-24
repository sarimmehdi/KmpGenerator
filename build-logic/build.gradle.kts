plugins {
    `kotlin-dsl`
    `java-library`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    //noinspection UseTomlInstead
    implementation("com.squareup:kotlinpoet:2.2.0")

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
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
