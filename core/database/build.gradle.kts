plugins {
    alias(libs.plugins.tithimala.android.library)
    alias(libs.plugins.tithimala.android.hilt)
    alias(libs.plugins.tithimala.android.room)
}

android {
    namespace = "app.tithimala.core.database"
}

dependencies {
    implementation(projects.core.model)
}
