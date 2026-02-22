plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("androidx.navigation.safeargs.kotlin")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.detekt)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kover)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// This versioning probably follows semver.org
val major = 0
val minor = 1
// Max:19
// Patch always calculated at five times in versionName and also adds abiFilter numberings in versionCode.
// Please see actualPatchVer.
val patch = 3

val hasNoSplits = hasProperty("noSplits")

android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "net.turtton.ytalarm"
        namespace = applicationId
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        val versionNamePatchVer = patch * 5
        val abiFilterList = property("abiFilters").toString().split(';')
        val singleAbiNum = when (abiFilterList.takeIf { it.size == 1 }?.first()) {
            "armeabi-v7a" -> 1
            "arm64-v8a" -> 2
            "x86" -> 3
            "x86_64" -> 4
            else -> if (hasNoSplits) 0 else 4
        }
        val versionCodePatchVer = versionNamePatchVer + singleAbiNum

        versionCode = major * 10000 + minor * 100 + versionCodePatchVer
        versionName = "$major.$minor.$versionNamePatchVer"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += abiFilterList
        }

        val androidConfig = android.defaultConfig
        val text = """
        versionCode=${androidConfig.versionCode}
        """.trimIndent()
        file("$rootDir/VERSION_CODE").writeText(text)
    }

    packaging {
        resources.excludes += "META-INF/atomicfu.kotlin_module"
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildTypes {
        debug {
        }
        release {
            isMinifyEnabled = false
            val proguardFile = getDefaultProguardFile("proguard-android-optimize.txt")
            proguardFiles(proguardFile, "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    val roomSchemaDir = "$projectDir/schemas"
    room {
        schemaDirectory(roomSchemaDir)
    }
    sourceSets {
        getByName("androidTest").assets.srcDirs(file(roomSchemaDir))
    }

    splits {
        abi {
            isEnable = !hasNoSplits
            reset()
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
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
        animationsDisabled = true
    }

    lint {
        warningsAsErrors = true

        // Aligned16KB: youtubedl-android library's native libs are not 16KB aligned
        // AndroidGradlePluginVersion: Gradle version recommendation, not a critical error
        disable +=
            listOf(
                "GradleDependency",
                "OldTargetApi",
                "Aligned16KB",
                "AndroidGradlePluginVersion"
            )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.bundles.androidx.navigation)
    androidTestImplementation(libs.androidx.navigation.testing)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.recyclerview.selection)
    implementation(libs.android.support.annotations)
    implementation(libs.bundles.androidx.room)
    ksp(libs.androidx.room.compiler)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    androidTestImplementation(libs.androidx.work.testing)

    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.bundles.androidx.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.bundles.kotlinx.serialization)

    implementation(libs.bundles.youtubedl)
    implementation(libs.bundles.androidx.media3)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.bundles.arrow)

    testImplementation(libs.junit)
    testImplementation(libs.bundles.kotest)
    androidTestImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.bundles.androidx.test.espresso)
    implementation(libs.androidx.test.espresso.idling.resource)
    androidTestImplementation(libs.screengrab)
    androidTestImplementation(libs.androidx.room.testing)
}

detekt {
    config.from(files("../detekt.yml"))
    buildUponDefaultConfig = true
}