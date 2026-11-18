plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "mx.udg.alumnos.tanken"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "mx.udg.alumnos.tanken"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { viewBinding = true }
    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")
}

dependencies {
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    androidTestImplementation(libs.room.testing)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.process)
    implementation(libs.lifecycle.viewmodel.savedstate)
    implementation(libs.lifecycle.livedata.core)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.coordinatorlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
