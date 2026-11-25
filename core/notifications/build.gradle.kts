plugins {
    alias(libs.plugins.tithimala.android.library)
    alias(libs.plugins.tithimala.android.hilt)
}

android {
    namespace = "app.tithimala.core.notifications"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.calendarEngine)
}
