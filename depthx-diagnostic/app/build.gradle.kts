plugins { id("com.android.application") }
android {
    namespace = "com.josias.depthxdiag"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.josias.depthx"
        minSdk = 30
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { abortOnError = true }
}
dependencies { testImplementation("junit:junit:4.13.2") }
