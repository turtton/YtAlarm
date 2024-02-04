// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.6")
    }
}
plugins {
    id("com.android.application") version "8.3.0-alpha05" apply false
    id("com.android.library") version "8.3.0-alpha05" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jmailen.kotlinter") version "3.15.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.5" apply false
    // Related: https://github.com/NeoTech-Software/Android-Root-Coverage-Plugin?tab=readme-ov-file#4-compatibility
    id("nl.neotech.plugin.rootcoverage") version "1.8.0-SNAPSHOT"
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