plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.antiscroller.kitty"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.clawse.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 38
        versionName = "1.0.37"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Cloudflare Worker that relays guardian alerts as push notifications (see /worker)
        buildConfigField("String", "GUARDIAN_RELAY_URL", "\"https://clawback-guardian.clawback-guardian-worker.workers.dev\"")
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
        buildConfig = true
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
    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    testImplementation("junit:junit:4.13.2")
}
