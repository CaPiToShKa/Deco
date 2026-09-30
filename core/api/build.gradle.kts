import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// HTTP contract (DTOs + routes) shared by the app's BFF client and the BFF server.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    api(project(":core:domain"))
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
