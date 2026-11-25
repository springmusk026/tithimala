package app.tithimala.feature.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.BsDate
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class CalendarScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun calendarScreen_displaysHeroMonthNavGridAndFestivals_andDispatchesActions() {
        val dispatchedActions = mutableListOf<CalendarAction>()

        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                CalendarScreen(
                    state = previewMangsir2083UiState(isShowingTodayMonth = true),
                    onAction = dispatchedActions::add
                )
            }
        }

        composeTestRule.onNodeWithText("Nov/Dec 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Next month").performClick()
        assertEquals(CalendarAction.NextMonth, dispatchedActions.last())

        composeTestRule.onNodeWithContentDescription("Previous month").performClick()
        assertEquals(CalendarAction.PreviousMonth, dispatchedActions.last())

        composeTestRule.onNodeWithText("Tuesday, 24 November 2026").performClick()
        assertEquals(CalendarAction.OpenDayDetail(BsDate(2083, 8, 8)), dispatchedActions.last())

        composeTestRule.onNodeWithContentDescription(
            "Friday, Mangsir 18, 2083, 4 December 2026, Public holiday, Has festivals, Has reminders"
        ).performClick()
        assertEquals(CalendarAction.OpenDayDetail(BsDate(2083, 8, 18)), dispatchedActions.last())
    }

    @Test
    fun calendarScreen_showsTodayJumpButton_whenViewingDifferentMonth() {
        val dispatchedActions = mutableListOf<CalendarAction>()

        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                CalendarScreen(
                    state = previewMangsir2083UiState(isShowingTodayMonth = false),
                    onAction = dispatchedActions::add
                )
            }
        }

        composeTestRule.onNodeWithText("Today")
            .assertIsDisplayed()
            .performClick()

        assertEquals(listOf<CalendarAction>(CalendarAction.JumpToToday), dispatchedActions)
    }
}
