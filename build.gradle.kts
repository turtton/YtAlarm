// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.2")
        classpath("com.google.android.gms:oss-licenses-plugin:0.10.5")
    }
}
plugins {
    id("com.android.application") version "7.3.1" apply false
    id("com.android.library") version "7.3.1" apply false
    id("org.jetbrains.kotlin.android") version "1.7.20" apply false
    id("org.jmailen.kotlinter") version "3.12.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.21.0" apply false
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