plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.androidx.room)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val roomSchemaDir = "$projectDir/schemas"

room {
    schemaDirectory(roomSchemaDir)
}

android {
    namespace = "net.turtton.ytalarm.datasource"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs(file(roomSchemaDir))
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
    }

    lint {
        warningsAsErrors = true
        disable +=
            listOf(
                "GradleDependency",
                "OldTargetApi",
                "AndroidGradlePluginVersion"
            )
    }
}

dependencies {
    implementation(project(":kernel"))

    implementation(libs.bundles.androidx.room)
    ksp(libs.androidx.room.compiler)

    implementation(libs.bundles.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)

    implementation(libs.bundles.arrow)
    implementation(libs.bundles.youtubedl)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.bundles.kotlinx.serialization)
    androidTestImplementation(kotlin("test"))
}

detekt {
    config.from(files("../detekt.yml"))
    buildUponDefaultConfig = true
}