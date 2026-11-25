package app.tithimala.feature.calendar

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import app.tithimala.core.designsystem.theme.TithimalaTheme
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
class CalendarScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)
    )

    @Test
    fun calendarScreenLightEnglish() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                CalendarScreen(
                    state = previewMangsir2083UiState(),
                    onAction = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/calendar_screen_light_en.png",
            roborazziOptions = roborazziOptions
        )
    }

    @Test
    fun calendarScreenDarkEnglish() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = true) {
                CalendarScreen(
                    state = previewMangsir2083UiState(),
                    onAction = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/calendar_screen_dark_en.png",
            roborazziOptions = roborazziOptions
        )
    }

    @Test
    @Config(qualifiers = "ne-w412dp-h915dp-xxhdpi")
    fun calendarScreenLightNepali() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                CalendarScreen(
                    state = previewMangsir2083UiState(),
                    onAction = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/calendar_screen_light_ne.png",
            roborazziOptions = roborazziOptions
        )
    }

    @Test
    @Config(qualifiers = "w412dp-h1400dp-xxhdpi")
    fun calendarScreenLightEnglishFontScale200() {
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f)
            ) {
                TithimalaTheme(darkTheme = false) {
                    CalendarScreen(
                        state = previewMangsir2083UiState(),
                        onAction = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/calendar_screen_light_en_font200.png",
            roborazziOptions = roborazziOptions
        )
    }
}
