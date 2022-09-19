plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
    id("androidx.navigation.safeargs.kotlin")
    kotlin("plugin.serialization") version "1.7.10"
    id("org.jmailen.kotlinter")
    jacoco
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "net.turtton.ytalarm"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += setOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        }
    }

    packagingOptions {
        resources.excludes += "META-INF/atomicfu.kotlin_module"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            val proguardFile = getDefaultProguardFile("proguard-android-optimize.txt")
            proguardFiles(proguardFile, "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
    }


    applicationVariants.all {
        val variantName = name.capitalize()
        val realVariantName = name

        if (buildType.name != "debug") {
            return@all
        }

        val testTaskName = "test${variantName}UnitTest"
        task<JacocoReport>("jacoco${variantName}TestReport") {
            dependsOn(
                "create${variantName}CoverageReport",
                "test${variantName}UnitTest"
            )

            group = "testing"
            description = "Generate Jacoco coverage reports for $realVariantName"

            reports {
                xml.required.set(false)
                html.required.set(true)
            }

            val fileFilter = listOf(
                "**/R.class",
                "**/R$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "android/**/*.*",
                "androidx/**/*.*",
                "**/Lambda$*.class",
                "**/Lambda.class",
                "**/*Lambda.class",
                "**/*Lambda*.class",
                "**/*Lambda*.*",
                "**/*Builder.*"
            )

//            val javaDebugTree = fileTree(
//                "dir" to "$buildDir/intermediates/javac/$realVariantName/compile${variantName}JavaWithJavac/classes",
//                "excludes" to fileFilter
//            )
            val kotlinDebugTree = fileTree(
                "dir" to "$buildDir/tmp/kotlin-classes/$realVariantName",
                "excludes" to fileFilter
            )
            val mainSrc = "${project.projectDir}/src/main/kotlin"

            sourceDirectories.setFrom(files(mainSrc))
            classDirectories.setFrom(files(kotlinDebugTree))
            executionData.setFrom(
                fileTree(
                    "dir" to project.projectDir,
                    "includes" to listOf("**/*.exec", "**/*.ec")
                )
            )
        }.also {
            tasks[testTaskName].finalizedBy(it)
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.2")?.version?.also {
        implementation("androidx.navigation:navigation-ui-ktx:$it")
    }
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation("com.android.support:support-annotations:28.0.0")
    val room = implementation("androidx.room:room-ktx:2.4.3")
    ksp("androidx.room:room-compiler:${room?.version}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")?.also {
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:${it.version}")
    }

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")

    implementation("com.github.yausername.youtubedl-android:library:0.14.0")?.also {
        implementation("com.github.yausername.youtubedl-android:common:${it.version}")
    }
    implementation("com.github.bumptech.glide:glide:4.13.2")?.also {
        annotationProcessor("com.github.bumptech.glide:compiler:${it.version}")
    }
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.16")?.also {
        implementation("com.michael-bull.kotlin-result:kotlin-result-coroutines:${it.version}")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.4.2")?.version?.also {
        testImplementation("io.kotest:kotest-assertions-core:$it")
        testImplementation("io.kotest:kotest-property:$it")
    }
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.robolectric:robolectric:4.8")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.room:room-testing:${room?.version}")
}

kotlinter {
    experimentalRules = true
}