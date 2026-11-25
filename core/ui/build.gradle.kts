plugins {
    alias(libs.plugins.tithimala.android.library)
    alias(libs.plugins.tithimala.android.compose)
    alias(libs.plugins.tithimala.android.roborazzi)
}

android {
    namespace = "app.tithimala.core.ui"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
}
