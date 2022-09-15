plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
    id("androidx.navigation.safeargs.kotlin")
    kotlin("plugin.serialization") version Versions.KOTLIN
}

android {
    compileSdk = 32

    defaultConfig {
        applicationId = "net.turtton.ytalarm"
        minSdk = 24
        targetSdk = 32
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
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    implementation("androidx.core:core-ktx:${Versions.CORE}")
    implementation("androidx.appcompat:appcompat:${Versions.APP_COMPAT}")
    implementation("com.google.android.material:material:${Versions.MATERIAL}")
    implementation("androidx.constraintlayout:constraintlayout:${Versions.CONSTRAINT_LAYOUT}")
    implementation("androidx.navigation:navigation-fragment-ktx:${Versions.NAVIGATION}")
    implementation("androidx.navigation:navigation-ui-ktx:${Versions.NAVIGATION}")
    implementation("androidx.recyclerview:recyclerview:${Versions.RECYCLER_VIEW}")
    implementation("com.android.support:support-annotations:${Versions.ANNOTATIONS}")
    implementation("androidx.room:room-ktx:${Versions.ROOM}")
    ksp("androidx.room:room-compiler:${Versions.ROOM}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.LIFECYCLE}")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${Versions.LIFECYCLE}")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.COROUTINE}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.SERIALIZATION}")

    implementation("com.github.yausername.youtubedl-android:library:${Versions.YT_DL}")
    implementation("com.github.yausername.youtubedl-android:common:${Versions.YT_DL}")
    implementation("com.github.bumptech.glide:glide:${Versions.GLIDE}")
    annotationProcessor("com.github.bumptech.glide:compiler:${Versions.GLIDE}")
    implementation("com.michael-bull.kotlin-result:kotlin-result:${Versions.KOTLIN_RESULT}")
    implementation("com.michael-bull.kotlin-result:kotlin-result-coroutines:${Versions.KOTLIN_RESULT}")

    testImplementation("junit:junit:${Versions.JUNIT}")
    androidTestImplementation("androidx.test.ext:junit:${Versions.JUNIT_ANDROID}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Versions.ESPRESSO}")
    androidTestImplementation("androidx.room:room-testing:${Versions.ROOM}")
}