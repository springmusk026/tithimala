package app.tithimala.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.Lint
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

internal const val COMPILE_SDK = 37
internal const val COMPILE_SDK_MINOR = 1
internal const val TARGET_SDK = 37
internal const val MIN_SDK = 26
internal const val JVM_TOOLCHAIN_VERSION = 21

internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk {
            version = release(COMPILE_SDK) {
                minorApiLevel = COMPILE_SDK_MINOR
            }
        }

        defaultConfig.minSdk = MIN_SDK

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    extensions.configure<KotlinBaseExtension> {
        jvmToolchain(JVM_TOOLCHAIN_VERSION)
    }
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    extensions.configure<KotlinBaseExtension> {
        jvmToolchain(JVM_TOOLCHAIN_VERSION)
    }
}

internal fun Lint.configureTithimalaLint() {
    warningsAsErrors = true
    abortOnError = true
    checkDependencies = false
}

internal fun Project.configureKtlint(isAndroid: Boolean) {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    extensions.configure<KtlintExtension> {
        android.set(isAndroid)
        outputToConsole.set(true)
        ignoreFailures.set(false)
    }
}

internal fun Project.configureDetekt() {
    pluginManager.apply("io.gitlab.arturbosch.detekt")
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    }
}
