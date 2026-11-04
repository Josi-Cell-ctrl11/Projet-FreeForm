plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.josias.floatx"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.josias.floatx"
        minSdk = 30
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.0-native"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { release { isMinifyEnabled = false } }
    lint { abortOnError = true }
}

dependencies {
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    testImplementation("junit:junit:4.13.2")
}
