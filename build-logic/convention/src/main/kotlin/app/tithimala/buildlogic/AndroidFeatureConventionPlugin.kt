package app.tithimala.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("tithimala.android.library")
            pluginManager.apply("tithimala.android.compose")
            pluginManager.apply("tithimala.android.hilt")

            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:model"))
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-navigation3-runtime").get())
            }
        }
    }
}
