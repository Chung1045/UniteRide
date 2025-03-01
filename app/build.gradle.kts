import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.chung.a9rushtobus"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.chung.a9rushtobus"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        val apiKey = localProperties.getProperty("MAPS_API_KEY", "YOUR_DEFAULT_API_KEY")
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = apiKey
        // Correct buildConfigField syntax
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$apiKey\"")
        buildConfigField("int", "REFRESH_INTERVAL", "60") // refresh interval in seconds
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

dependencies {

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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}