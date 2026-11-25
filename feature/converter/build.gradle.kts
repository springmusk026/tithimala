plugins {
    alias(libs.plugins.tithimala.android.feature)
    alias(libs.plugins.tithimala.android.roborazzi)
}

android {
    namespace = "app.tithimala.feature.converter"
}

dependencies {
    implementation(projects.core.calendarEngine)
    implementation(projects.core.data)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
