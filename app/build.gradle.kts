import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}


val keystorePropertiesFile = project.file("keystore.properties")
val keystoreProperties = Properties().apply {
    load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.repkap11.browserwrapper"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.repkap11.browserwrapper"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        getByName("release") {
            // Enable minification and resource shrinking for release build
            isMinifyEnabled = true
            isShrinkResources = true
            // Assign the release signing config to the release build type
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
}