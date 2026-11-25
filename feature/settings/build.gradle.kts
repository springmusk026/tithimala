plugins {
    alias(libs.plugins.tithimala.android.feature)
    alias(libs.plugins.tithimala.android.roborazzi)
}

android {
    namespace = "app.tithimala.feature.settings"
}

dependencies {
    implementation(projects.core.calendarEngine)
    implementation(projects.core.data)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
