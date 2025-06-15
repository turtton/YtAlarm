// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath(libs.androidx.navigation.safe.args)
    }
    configurations.classpath {
        resolutionStrategy {
            force(
                libs.ktlint.rule.engine,
                libs.ktlint.rule.engine.core,
                libs.ktlint.cli.reporter.core,
                libs.ktlint.cli.reporter.checkstyle,
                libs.ktlint.cli.reporter.json,
                libs.ktlint.cli.reporter.html,
                libs.ktlint.cli.reporter.plain,
                libs.ktlint.cli.reporter.sarif,
                libs.ktlint.ruleset.standard
            )
        }
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlinter) apply false
    alias(libs.plugins.detekt) apply false
    // Related: https://github.com/NeoTech-Software/Android-Root-Coverage-Plugin?tab=readme-ov-file#4-compatibility
    alias(libs.plugins.root.coverage)
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