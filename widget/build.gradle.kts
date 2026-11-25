plugins {
    alias(libs.plugins.tithimala.android.library)
    alias(libs.plugins.tithimala.android.compose)
}

android {
    namespace = "app.tithimala.widget"
}

dependencies {
    implementation(projects.core.calendarEngine)
    implementation(projects.core.model)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
}
