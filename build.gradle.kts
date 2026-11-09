// Estrategia Kotlin: AGP 9.x traz Kotlin integrado (built-in). O KGP efetivo e fixado
// pelo classpath abaixo para alinhar compilador, plugin Compose e testes.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
