package app.tithimala.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    commonExtension.buildFeatures.compose = true

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        val enableMetrics = providers.gradleProperty("tithimala.enableComposeCompilerMetrics")
            .map(String::toBoolean)
            .getOrElse(false)
        if (enableMetrics) {
            metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
        }

        val enableReports = providers.gradleProperty("tithimala.enableComposeCompilerReports")
            .map(String::toBoolean)
            .getOrElse(false)
        if (enableReports) {
            reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
        }
    }

    dependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))
        add("implementation", libs.findLibrary("androidx-compose-runtime").get())
        add("implementation", libs.findLibrary("androidx-compose-ui").get())
        add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
        add("implementation", libs.findLibrary("androidx-compose-foundation").get())
        add("implementation", libs.findLibrary("androidx-compose-material3").get())
        add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
    }
}
