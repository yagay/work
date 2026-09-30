plugins {
    id("com.android.application")
}

val ciArm64Only = providers.gradleProperty("ciArm64Only").orNull == "true"

android {
    namespace = "com.example.workhours"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.workhours"
        minSdk = 31
        targetSdk = 37
        versionCode = 28
        versionName = "1.28"

        if (ciArm64Only) {
            ndk {
                abiFilters.clear()
                abiFilters += "arm64-v8a"
            }
        }
    }
}
