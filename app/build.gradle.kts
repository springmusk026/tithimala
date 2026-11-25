plugins {
    alias(libs.plugins.tithimala.android.application)
    alias(libs.plugins.tithimala.android.compose)
    alias(libs.plugins.tithimala.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.tithimala"

    defaultConfig {
        applicationId = "app.tithimala"
        versionCode = 100
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(projects.core.calendarEngine)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.core.notifications)
    implementation(projects.core.ui)
    implementation(projects.feature.calendar)
    implementation(projects.feature.converter)
    implementation(projects.feature.day)
    implementation(projects.feature.reminders)
    implementation(projects.feature.settings)
    implementation(projects.widget)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
