plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.antiscroller.kitty"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.clawse.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 17
        versionName = "1.0.16"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("../keystore/release.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "antiscroller123"
                keyAlias = "antiscroller"
                keyPassword = "antiscroller123"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.android.billingclient:billing-ktx:8.0.0")
    testImplementation("junit:junit:4.13.2")
}
