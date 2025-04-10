import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.chung.a9rushtobus"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {

        val secretsPropertiesFile = rootProject.file("secrets.properties")
        val secretsProperties = Properties()
        if (secretsPropertiesFile.exists()) {
            secretsProperties.load(secretsPropertiesFile.inputStream())
        }

        applicationId = "com.chung.a9rushtobus"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("int", "REFRESH_INTERVAL", "60") // refresh interval in seconds
        val apiKey = secretsProperties.getProperty("MAPS_API_KEY", "")
        buildConfigField("String", "MAPS_API_KEY", "\"$apiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

secrets {
    // To add your Maps API key to this project:
    // 1. If the secrets.properties file does not exist, create it in the same folder as the local.properties file.
    // 2. Add this line, where YOUR_API_KEY is your API key:
    //        MAPS_API_KEY=YOUR_API_KEY
    propertiesFileName = "secrets.properties"

    // A properties file containing default secret values. This file can be
    // checked in version control.
    defaultPropertiesFileName = "local.defaults.properties"
}

dependencies {
    implementation(libs.core.splashscreen)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.preference)
    implementation(libs.okhttp)
    implementation(libs.swiperefreshlayout)
    implementation(libs.jsoup)
    implementation(libs.constraintlayout)
    implementation(libs.cronet.embedded)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
