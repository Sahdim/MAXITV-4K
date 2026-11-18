plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.maxitv"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.maxitv.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.leanback)
    implementation(libs.glide)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
