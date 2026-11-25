plugins {
    alias(libs.plugins.tithimala.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
