import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
}

/*
 * Configuration comes from (first match wins): local.properties, Gradle properties (-P or
 * ~/.gradle/gradle.properties), environment variables (CI secrets). Nothing secret is committed.
 * See docs/KONFIGURACJA_SFMC.md for every key.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun config(key: String, default: String = ""): String =
    localProperties.getProperty(key)
        ?: providers.gradleProperty(key).orNull
        ?: System.getenv(key)
        ?: default

fun String.quoted(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

// Firebase (needed by SFMC MobilePush) is only wired in when google-services.json is present,
// so the demo builds and runs without any Firebase project.
val hasFirebaseConfig = file("google-services.json").exists()
if (hasFirebaseConfig) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.example.decosocio"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.decosocio"
        minSdk = 26 // SFMC MobilePush SDK 11 requires API 26
        targetSdk = 36 // Google Play requirement since 31 Aug 2026
        versionCode = 1
        versionName = "0.1.0-demo"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BACKEND_MODE", config("BACKEND_MODE", "demo").quoted())
        buildConfigField("String", "BFF_BASE_URL", config("BFF_BASE_URL").quoted())
        buildConfigField("String", "NEWS_FEED_URL", config("NEWS_FEED_URL").quoted())
        buildConfigField("String", "RENEW_URL", config("RENEW_URL", "https://www.deco.proteste.pt/").quoted())
        buildConfigField("String", "PRIVACY_POLICY_URL", config("PRIVACY_POLICY_URL", "https://www.deco.proteste.pt/").quoted())
        buildConfigField("String", "SFMC_APP_ID", config("SFMC_APP_ID").quoted())
        buildConfigField("String", "SFMC_ACCESS_TOKEN", config("SFMC_ACCESS_TOKEN").quoted())
        buildConfigField("String", "SFMC_SERVER_URL", config("SFMC_SERVER_URL").quoted())
        buildConfigField("String", "SFMC_MID", config("SFMC_MID").quoted())
        buildConfigField("String", "SFMC_SENDER_ID", config("SFMC_SENDER_ID").quoted())
        buildConfigField("boolean", "FIREBASE_CONFIGURED", hasFirebaseConfig.toString())
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the debug key only so CI can produce an installable demo APK.
            // Replace with a real signing config (Play App Signing upload key) before publishing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget("17")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging.resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
        excludes += "/META-INF/INDEX.LIST"
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.sfmc.marketingcloudsdk)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
