plugins {
    alias(libs.plugins.tithimala.android.library)
    alias(libs.plugins.tithimala.android.hilt)
}

android {
    namespace = "app.tithimala.core.notifications"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.calendarEngine)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
}
