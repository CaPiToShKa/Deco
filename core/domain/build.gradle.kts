import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin: no Android APIs, so this module can move to a KMP `commonMain` for iOS later.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
