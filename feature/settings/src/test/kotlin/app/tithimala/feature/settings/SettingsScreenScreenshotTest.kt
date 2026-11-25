package app.tithimala.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.UserPreferences
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class SettingsScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)
    )

    @Test
    fun settingsScreenLightEnglish() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                SettingsScreen(
                    state = sampleSettingsUiState(
                        preferences = UserPreferences(
                            language = AppLanguage.ENGLISH,
                            numeralScript = NumeralScript.LATIN,
                            themeMode = ThemeMode.LIGHT
                        )
                    ),
                    onAction = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/settings_screen_light_en.png",
            roborazziOptions = roborazziOptions
        )
    }

    @Test
    fun settingsScreenDarkEnglish() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = true) {
                SettingsScreen(
                    state = sampleSettingsUiState(
                        preferences = UserPreferences(
                            language = AppLanguage.SYSTEM,
                            numeralScript = NumeralScript.DEVANAGARI,
                            themeMode = ThemeMode.DARK
                        )
                    ),
                    onAction = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/settings_screen_dark_en.png",
            roborazziOptions = roborazziOptions
        )
    }

    @Test
    @Config(qualifiers = "ne-w412dp-h915dp-xxhdpi")
    fun settingsScreenLightNepali() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                SettingsScreen(
                    state = sampleSettingsUiState(
                        preferences = UserPreferences(
                            language = AppLanguage.NEPALI,
                            numeralScript = NumeralScript.DEVANAGARI,
                            themeMode = ThemeMode.SYSTEM
                        )
                    ),
                    onAction = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/settings_screen_light_ne.png",
            roborazziOptions = roborazziOptions
        )
    }

    @Test
    fun settingsScreenLightEnglishFontScale200() {
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f)
            ) {
                TithimalaTheme(darkTheme = false) {
                    SettingsScreen(
                        state = sampleSettingsUiState(
                            preferences = UserPreferences(
                                language = AppLanguage.ENGLISH,
                                numeralScript = NumeralScript.LATIN,
                                themeMode = ThemeMode.LIGHT
                            )
                        ),
                        onAction = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/settings_screen_light_en_font200.png",
            roborazziOptions = roborazziOptions
        )
    }

    @Test
    fun settingsScreenAboutSectionLightEnglish() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                SettingsScreen(
                    state = sampleSettingsUiState(
                        preferences = UserPreferences(
                            language = AppLanguage.ENGLISH,
                            numeralScript = NumeralScript.LATIN,
                            themeMode = ThemeMode.LIGHT
                        )
                    ),
                    onAction = {}
                )
            }
        }
        composeTestRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Data sources and licenses"))
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/settings_screen_about_light_en.png",
            roborazziOptions = roborazziOptions
        )
    }

    @Test
    fun settingsScreenLicensesDialogLightEnglish() {
        val bundledNotice = BikramSambatEngine.createBundled().noticeText
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                SettingsScreen(
                    state = sampleSettingsUiState(
                        preferences = UserPreferences(
                            language = AppLanguage.ENGLISH,
                            numeralScript = NumeralScript.LATIN,
                            themeMode = ThemeMode.LIGHT
                        ),
                        isLicensesDialogVisible = true,
                        noticeText = bundledNotice
                    ),
                    onAction = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/settings_screen_licenses_dialog_light_en.png",
            roborazziOptions = roborazziOptions
        )
    }
}
