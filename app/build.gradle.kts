plugins { id("com.android.application") }

android {
    namespace = "pl.atomservice.drukippoz"
    compileSdk = 35
    defaultConfig {
        applicationId = "pl.atomservice.drukippoz"
        minSdk = 26
        targetSdk = 34
        versionCode = 9
        versionName = "1.3.4"
    }
    signingConfigs {
        create("stableRelease") {
            val keystorePath = System.getenv("PPOZ_SIGNING_KEYSTORE")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("PPOZ_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("PPOZ_KEY_ALIAS")
                keyPassword = System.getenv("PPOZ_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (!System.getenv("PPOZ_SIGNING_KEYSTORE").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("stableRelease")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Secure temporary read-only content URI for PDF attachments.
dependencies { implementation("androidx.core:core:1.15.0") }
