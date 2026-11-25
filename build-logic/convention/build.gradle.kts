import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "app.tithimala.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.roborazzi.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.tithimala.android.application.get().pluginId
            implementationClass = "app.tithimala.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.tithimala.android.library.get().pluginId
            implementationClass = "app.tithimala.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.tithimala.android.compose.get().pluginId
            implementationClass = "app.tithimala.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.tithimala.android.feature.get().pluginId
            implementationClass = "app.tithimala.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = libs.plugins.tithimala.android.hilt.get().pluginId
            implementationClass = "app.tithimala.buildlogic.AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = libs.plugins.tithimala.android.room.get().pluginId
            implementationClass = "app.tithimala.buildlogic.AndroidRoomConventionPlugin"
        }
        register("androidRoborazzi") {
            id = libs.plugins.tithimala.android.roborazzi.get().pluginId
            implementationClass = "app.tithimala.buildlogic.AndroidRoborazziConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.tithimala.jvm.library.get().pluginId
            implementationClass = "app.tithimala.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
