plugins {
    alias(libs.plugins.tithimala.android.library)
    alias(libs.plugins.tithimala.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.tithimala.core.data"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.calendarEngine)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.hilt.compiler)
}
