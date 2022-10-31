plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.7.20-1.0.7"
    id("androidx.navigation.safeargs.kotlin")
    kotlin("plugin.serialization") version "1.7.20"
    id("org.jmailen.kotlinter")
    id("io.gitlab.arturbosch.detekt")
}

// This versioning probably follows semver.org
val major = 0
val minor = 1
// Max:19
// Patch always calculated at five times in versionName and also adds abiFilter numberings in versionCode.
// Please see actualPatchVer.
val patch = 0

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "net.turtton.ytalarm"
        namespace = applicationId
        minSdk = 24
        targetSdk = 33

        val versionNamePatchVer = patch * 5
        val abiFilterList = property("abiFilters").toString().split(';')
        val singleAbiNum = when (abiFilterList.takeIf { it.size == 1 }?.first()) {
            "armeabi-v7a" -> 1
            "arm64-v8a" -> 2
            "x86" -> 3
            "x86_64" -> 4
            else -> 0
        }
        val versionCodePatchVer = versionNamePatchVer + singleAbiNum

        versionCode = major * 10000 + minor * 100 + versionCodePatchVer
        versionName = "$major.$minor.$versionNamePatchVer"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += abiFilterList
        }

        tasks.preBuild {
            doLast {
                val androidConfig = android.defaultConfig
                val text = """
                versionCode=${androidConfig.versionCode}
                versionName=${androidConfig.versionName}
                """.trimIndent()
                file("$rootDir/VERSION").writeText(text)
            }
        }
    }

    packagingOptions {
        resources.excludes += "META-INF/atomicfu.kotlin_module"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            val proguardFile = getDefaultProguardFile("proguard-android-optimize.txt")
            proguardFiles(proguardFile, "proguard-rules.pro")
        }
    }
    val schemaDir = "$projectDir/schemas"
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)

        ksp {
            arg("room.schemaLocation", schemaDir)
        }
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs(file(schemaDir))
    }

    splits {
        abi {
            isEnable = !hasProperty("noSplits")
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
        animationsDisabled = true
    }

    lint {
        warningsAsErrors = true

        disable += "GradleDependency"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")?.version?.also {
        implementation("androidx.navigation:navigation-ui-ktx:$it")
        androidTestImplementation("androidx.navigation:navigation-testing:$it")
    }
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation("com.android.support:support-annotations:28.0.0")
    val room = implementation("androidx.room:room-ktx:2.4.3")
    ksp("androidx.room:room-compiler:${room?.version}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")?.also {
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:${it.version}")
    }
    implementation("androidx.activity:activity-ktx:1.6.1")
    implementation("androidx.fragment:fragment-ktx:1.5.4")
    implementation("androidx.work:work-runtime-ktx:2.7.1")?.version?.also {
        androidTestImplementation("androidx.work:work-testing:$it")
    }

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")?.version?.also {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$it")
    }

    implementation("com.github.yausername.youtubedl-android:library:4f00394209")?.also {
        implementation("com.github.yausername.youtubedl-android:common:${it.version}")
    }
    implementation("com.github.bumptech.glide:glide:4.14.2")?.also {
        annotationProcessor("com.github.bumptech.glide:compiler:${it.version}")
    }
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.16")?.also {
        implementation("com.michael-bull.kotlin-result:kotlin-result-coroutines:${it.version}")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.3")?.version?.also {
        testImplementation("io.kotest:kotest-assertions-core:$it")?.also { dep ->
            androidTestImplementation(dep)
        }
        testImplementation("io.kotest:kotest-property:$it")
    }
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.robolectric:robolectric:4.9")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.3")
    androidTestImplementation("androidx.test:rules:1.4.0")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")?.version?.also {
        implementation("androidx.test.espresso:espresso-idling-resource:$it")
        androidTestImplementation("androidx.test.espresso:espresso-contrib:$it")
    }
    androidTestImplementation("androidx.test.espresso:espresso-contrib:")
    androidTestImplementation("tools.fastlane:screengrab:2.1.1")
    androidTestImplementation("androidx.room:room-testing:${room?.version}")
}

kotlinter {
    experimentalRules = true
}

detekt {
    config = files("../detekt.yml")
    buildUponDefaultConfig = true
}