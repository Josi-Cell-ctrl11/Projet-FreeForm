plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.josias.floatx"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.josias.floatx"
        minSdk = 30
        targetSdk = 35
        manifestPlaceholders["appLabel"] = "FloatX"
        versionCode = 100
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs {
        if (!System.getenv("FLOATX_KEYSTORE_PATH").isNullOrBlank()) {
            create("stable") {
                storeFile = file(System.getenv("FLOATX_KEYSTORE_PATH"))
                storePassword = System.getenv("FLOATX_STORE_PASSWORD")
                keyAlias = System.getenv("FLOATX_KEY_ALIAS")
                keyPassword = System.getenv("FLOATX_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            manifestPlaceholders["appLabel"] = "FloatX Aperçu"
        }
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = signingConfigs.findByName("stable")
        }
    }
    lint { abortOnError = true }
}

dependencies {
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    testImplementation("junit:junit:4.13.2")
}
