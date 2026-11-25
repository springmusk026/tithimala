package app.tithimala.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoborazziConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")

            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> {
                    testOptions.unitTests.isIncludeAndroidResources = true
                }
            }
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> {
                    testOptions.unitTests.isIncludeAndroidResources = true
                }
            }

            dependencies {
                add("testImplementation", libs.findLibrary("robolectric").get())
                add("testImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
                add("testImplementation", libs.findLibrary("roborazzi").get())
                add("testImplementation", libs.findLibrary("roborazzi-compose").get())
                add("testImplementation", libs.findLibrary("roborazzi-junit-rule").get())
                add("testImplementation", libs.findLibrary("androidx-test-ext-junit").get())
            }
        }
    }
}
