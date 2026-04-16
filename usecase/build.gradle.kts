plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
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
    implementation(project(":kernel"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.arrow)

    testFixturesImplementation(project(":kernel"))
    testFixturesImplementation(testFixtures(project(":kernel")))

    testImplementation(libs.bundles.kotest)
    testImplementation(testFixtures(project(":kernel")))
    testImplementation(testFixtures(project(":usecase")))
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    config.from(files("../detekt.yml"))
    buildUponDefaultConfig = true
}