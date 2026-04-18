plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.bundles.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.arrow)

    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.bundles.arrow)

    testImplementation(libs.bundles.kotest)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    config.from(files("../detekt.yml"))
    buildUponDefaultConfig = true
}