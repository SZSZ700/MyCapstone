plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myfinaltopapplication"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.myfinaltopapplication"
        minSdk = 24
        targetSdk = 37
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true

        unitTests.all {
            it.jvmArgs(
                "-XX:+EnableDynamicAgentLoading",

                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    // Application dependencies
    implementation(libs.mpandroidchart)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.okhttp)

    // JVM unit tests
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockwebserver)
    // Provides ApplicationProvider for Robolectric JVM tests
    //noinspection UseTomlInstead
    testImplementation("androidx.test:core:1.7.0")
    testImplementation(libs.mockito.kotlin)
    // Android instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.runner)
}