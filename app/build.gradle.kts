plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.chess.stats"
    androidResources {
        noCompress += "webm"
    }
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.chess.stats"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
