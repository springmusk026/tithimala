package app.tithimala.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            pluginManager.withPlugin("com.android.application") {
                configureAndroidCompose(extensions.getByType<ApplicationExtension>())
            }
            pluginManager.withPlugin("com.android.library") {
                configureAndroidCompose(extensions.getByType<LibraryExtension>())
            }
        }
    }
}
