// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.2")
    }
}
plugins {
    id("com.android.application") version Versions.ANDROID apply false
    id("com.android.library") version Versions.ANDROID apply false
    id("org.jetbrains.kotlin.android") version Versions.KOTLIN apply false
}

tasks.create("clean") {
    delete(rootProject.buildDir)
}