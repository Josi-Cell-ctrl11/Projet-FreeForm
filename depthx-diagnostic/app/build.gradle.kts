plugins { id("com.android.application") }
android {
    namespace = "com.josias.depthxdiag"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.josias.depthxdiag"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-diagnostic"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { abortOnError = true }
}
dependencies { testImplementation("junit:junit:4.13.2") }
