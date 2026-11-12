plugins { id("com.android.application") }

android {
    namespace = "pl.atomservice.drukippoz"
    compileSdk = 35
    defaultConfig {
        applicationId = "pl.atomservice.drukippoz"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
