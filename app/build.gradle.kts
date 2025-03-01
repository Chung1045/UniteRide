plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.chung.a9rushtobus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chung.a9rushtobus"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}