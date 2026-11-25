plugins {
    alias(libs.plugins.tithimala.android.library)
    alias(libs.plugins.tithimala.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.tithimala.core.data"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.calendarEngine)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.notifications)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}
