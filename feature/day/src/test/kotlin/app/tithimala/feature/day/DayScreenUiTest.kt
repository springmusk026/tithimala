package app.tithimala.feature.day

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.ReminderRecurrence
import app.tithimala.core.model.TithiSource
import app.tithimala.core.ui.calendar.LunarPhaseVisual
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class DayScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dayScreen_rendersHolidayEventsReminders_andDispatchesActions() {
        val actions = mutableListOf<DayAction>()
        var backClicks = 0

        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                DayScreen(
                    state = sampleDayUiState(),
                    onAction = { actions += it },
                    onBack = { backClicks++ }
                )
            }
        }

        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Baishakh 2081").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saturday, 13 April 2024").assertIsDisplayed()
        composeTestRule.onNodeWithText("Panchami").assertIsDisplayed()
        composeTestRule.onNodeWithText("Public holiday").assertIsDisplayed()
        composeTestRule.onNodeWithText("New Year").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mesh Sankranti").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bisket Jatra").assertIsDisplayed()
        composeTestRule.onNodeWithText("Morning family puja").assertIsDisplayed()
        composeTestRule.onNodeWithText("Yearly (BS)").assertIsDisplayed()
        composeTestRule.onNodeWithText("One-time").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        composeTestRule.onNodeWithContentDescription("Previous day").performClick()
        composeTestRule.onNodeWithContentDescription("Next day").performClick()
        composeTestRule.onNodeWithContentDescription("Copy date").performClick()
        composeTestRule.onNodeWithText("Add reminder").performClick()
        composeTestRule.onNodeWithContentDescription("Reminder: Morning family puja").performClick()
        composeTestRule.onNodeWithContentDescription("Delete reminder: Morning family puja").performClick()

        assertEquals(1, backClicks)
        assertEquals(
            listOf(
                DayAction.PreviousDay,
                DayAction.NextDay,
                DayAction.CopyDateClicked,
                DayAction.AddReminderClicked,
                DayAction.ToggleReminder(id = 1L, enabled = false),
                DayAction.DeleteReminder(id = 1L)
            ),
            actions
        )
    }

    @Test
    fun dayScreen_displaysEstimatedTithiAndVerbatimMarker() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                DayScreen(
                    state = DayUiState(
                        bsDate = BsDate(2083, 8, 10),
                        bsDayNumber = "10",
                        bsMonthYear = "Mangsir 2083",
                        weekdayLabel = "Wednesday",
                        adDateLabel = "25 November 2026",
                        tithiLabel = "Pratipadha (K)",
                        tithiSource = TithiSource.ESTIMATED,
                        lunarPhase = LunarPhaseVisual.WAXING_CRESCENT,
                        isPublicHoliday = false,
                        isSaturday = false,
                        events = emptyList(),
                        reminders = listOf(
                            DayReminderUiModel(
                                id = 9L,
                                title = "Check calendar",
                                notes = "",
                                formattedTime = "09:00",
                                recurrence = ReminderRecurrence.ONE_TIME,
                                isEnabled = true
                            )
                        )
                    ),
                    onAction = {},
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Pratipadha (K) (Est.)").assertIsDisplayed()
        composeTestRule.onNodeWithText("No festivals or observances on this date").assertIsDisplayed()
    }

    @Test
    fun dayScreen_displaysUnavailableAlmanacNoteAndEmptyRemindersInvitation() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                DayScreen(
                    state = DayUiState(
                        bsDate = BsDate(2070, 6, 15),
                        bsDayNumber = "15",
                        bsMonthYear = "Ashoj 2070",
                        weekdayLabel = "Tuesday",
                        adDateLabel = "1 October 2013",
                        tithiLabel = null,
                        tithiSource = TithiSource.UNAVAILABLE,
                        lunarPhase = null,
                        isPublicHoliday = false,
                        isSaturday = false,
                        events = emptyList(),
                        reminders = emptyList()
                    ),
                    onAction = {},
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No almanac data for this year").assertIsDisplayed()
        composeTestRule.onNodeWithText("No reminders for this day").assertIsDisplayed()
    }

    @Test
    fun dayScreen_displaysSaturdayBadge_whenSaturdayAndNotPublicHoliday() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                DayScreen(
                    state = DayUiState(
                        bsDate = BsDate(2081, 1, 8),
                        bsDayNumber = "8",
                        bsMonthYear = "Baishakh 2081",
                        weekdayLabel = "Saturday",
                        adDateLabel = "20 April 2024",
                        tithiLabel = "Dwadashi",
                        tithiSource = TithiSource.VERIFIED,
                        lunarPhase = LunarPhaseVisual.WAXING_GIBBOUS,
                        isPublicHoliday = false,
                        isSaturday = true
                    ),
                    onAction = {},
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Saturday").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dwadashi").assertIsDisplayed()
    }
}
