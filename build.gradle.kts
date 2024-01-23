// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.3")
    }
}
plugins {
    id("com.android.application") version "7.4.1" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id("org.jmailen.kotlinter") version "3.13.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.22.0" apply false
    id("nl.neotech.plugin.rootcoverage") version "1.6.0"
}

tasks.create("clean") {
    delete(rootProject.buildDir)
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