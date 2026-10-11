// Döngü motoru: saf Kotlin/JVM modülü. Android bağımlılığı yoktur; bu sayede
// birim testleri hızlı çalışır ve ileride Kotlin Multiplatform'a taşınabilir.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        allWarningsAsErrors = false
    }
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}
