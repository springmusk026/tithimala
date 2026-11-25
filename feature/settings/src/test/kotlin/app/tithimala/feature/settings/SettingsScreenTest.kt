package app.tithimala.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.tithimala.core.calendarengine.BikramSambatEngine
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.model.WeekStartDay
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickingPreferencesDispatchesExpectedActions() {
        val actions = mutableListOf<SettingsAction>()
        val initialState = sampleSettingsUiState(
            preferences = UserPreferences(
                language = AppLanguage.SYSTEM,
                numeralScript = NumeralScript.DEVANAGARI,
                themeMode = ThemeMode.SYSTEM,
                weekStartDay = WeekStartDay.SUNDAY,
                backgroundSyncEnabled = true
            )
        )

        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                SettingsScreen(
                    state = initialState,
                    onAction = actions::add
                )
            }
        }

        composeTestRule.onNodeWithText("System default").assertIsSelected()
        composeTestRule.onNodeWithText("नेपाली").performClick()
        composeTestRule.onNodeWithText("Latin (0-9)").performClick()
        composeTestRule.onNodeWithText("Dark").performClick()
        composeTestRule.onNodeWithText("Monday").performClick()

        composeTestRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Check for updates"))
        composeTestRule.onNodeWithText("Weekly calendar data checks").performClick()
        composeTestRule.onNodeWithText("Check for updates").performClick()

        composeTestRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Data sources and licenses"))
        composeTestRule.onNodeWithText("Data sources and licenses").performClick()

        assertEquals(
            listOf(
                SettingsAction.SelectLanguage(AppLanguage.NEPALI),
                SettingsAction.SelectNumeralScript(NumeralScript.LATIN),
                SettingsAction.SelectThemeMode(ThemeMode.DARK),
                SettingsAction.SelectWeekStartDay(WeekStartDay.MONDAY),
                SettingsAction.SetBackgroundSyncEnabled(false),
                SettingsAction.CheckForUpdates,
                SettingsAction.OpenLicensesDialog
            ),
            actions
        )
    }

    @Test
    fun licensesDialogDisplaysNoticeAndFontCreditsAndDismisses() {
        val actions = mutableListOf<SettingsAction>()
        val bundledNotice = BikramSambatEngine.createBundled().noticeText
        val state = sampleSettingsUiState(
            isLicensesDialogVisible = true,
            noticeText = bundledNotice
        )

        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                SettingsScreen(
                    state = state,
                    onAction = actions::add
                )
            }
        }

        composeTestRule.onNodeWithText("Upstream NOTICE").assertIsDisplayed()
        composeTestRule.onNodeWithText(bundledNotice.trim()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Mukta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Noto Serif Devanagari").assertIsDisplayed()
        composeTestRule.onNodeWithText("Close").performClick()

        assertTrue(actions.contains(SettingsAction.DismissLicensesDialog))
    }
}
