// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.9.0")
    }
    configurations.classpath {
        resolutionStrategy {
            force(
                "com.pinterest.ktlint:ktlint-rule-engine:1.6.0",
                "com.pinterest.ktlint:ktlint-rule-engine-core:1.6.0",
                "com.pinterest.ktlint:ktlint-cli-reporter-core:1.6.0",
                "com.pinterest.ktlint:ktlint-cli-reporter-checkstyle:1.6.0",
                "com.pinterest.ktlint:ktlint-cli-reporter-json:1.6.0",
                "com.pinterest.ktlint:ktlint-cli-reporter-html:1.6.0",
                "com.pinterest.ktlint:ktlint-cli-reporter-plain:1.6.0",
                "com.pinterest.ktlint:ktlint-cli-reporter-sarif:1.6.0",
                "com.pinterest.ktlint:ktlint-ruleset-standard:1.6.0"
            )
        }
    }
}
plugins {
    id("com.android.application") version "8.11.0" apply false
    id("com.android.library") version "8.11.0" apply false
    id("androidx.room") version "2.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("org.jmailen.kotlinter") version "4.5.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
    // Related: https://github.com/NeoTech-Software/Android-Root-Coverage-Plugin?tab=readme-ov-file#4-compatibility
    id("nl.neotech.plugin.rootcoverage") version "1.10.0"
}

tasks.create("clean") {
    delete(rootProject.layout.buildDirectory)
}

rootCoverage {
    excludes = listOf(
        // Android
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*_Impl*",
        "**/Fragment*Args.*",
        "**/Fragment*Args\$*",
        "**/Fragment*Directions.*",
        "**/Fragment*Directions\$*",
        "**/*Directions\$*.*",
        "**/databinding/**"
    )

    generateXml = true

    executeAndroidTests = false
}