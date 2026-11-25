package app.tithimala.core.ui.calendar

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tithimala.core.designsystem.theme.TithimalaTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CalendarCompositesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tithiMoonBadge_displaysLabelAndPhase_withoutEstimatedTag() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                TithiMoonBadge(
                    tithiLabel = "Purnima",
                    phase = LunarPhaseVisual.FULL_MOON,
                    isEstimated = false
                )
            }
        }

        composeTestRule.onNodeWithText("Purnima").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Full moon").assertIsDisplayed()
        composeTestRule.onNodeWithText("(Est.)", substring = true).assertDoesNotExist()
    }

    @Test
    fun tithiMoonBadge_displaysEstimatedTag_whenEstimated() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                TithiMoonBadge(
                    tithiLabel = "Ekadashi",
                    phase = LunarPhaseVisual.WAXING_GIBBOUS,
                    isEstimated = true
                )
            }
        }

        composeTestRule.onNodeWithText("Ekadashi (Est.)").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Waxing gibbous").assertIsDisplayed()
    }

    @Test
    fun tithiMoonBadge_rendersLabel_whenPhaseIsNull() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                TithiMoonBadge(
                    tithiLabel = "Saptami",
                    phase = null,
                    isEstimated = false
                )
            }
        }

        composeTestRule.onNodeWithText("Saptami").assertIsDisplayed()
    }

    @Test
    fun todayHeroHeader_displaysHolidayFestivalAndEstimatedTithi_andInvokesCallbacks() {
        var headerClicks = 0
        var trailingActionClicks = 0

        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                TodayHeroHeader(
                    state = TodayHeroUiModel(
                        bsDayNumber = "10",
                        bsMonthYear = "Mangsir 2083",
                        adDateLabel = "25 November 2026",
                        weekdayLabel = "Wednesday",
                        tithiLabel = "Purnima",
                        lunarPhase = LunarPhaseVisual.FULL_MOON,
                        isEstimatedTithi = true,
                        primaryFestival = "Udhauli Parva",
                        isHoliday = true
                    ),
                    onClick = { headerClicks++ },
                    trailingAction = {
                        Button(onClick = { trailingActionClicks++ }) {
                            Text(text = "Today")
                        }
                    }
                )
            }
        }

        composeTestRule.onNodeWithText("10").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mangsir 2083").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wednesday, 25 November 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Purnima (Est.)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Public holiday").assertIsDisplayed()
        composeTestRule.onNodeWithText("Udhauli Parva").assertIsDisplayed()

        composeTestRule.onNodeWithText("Today").performClick()
        assertEquals(1, trailingActionClicks)
        assertEquals(0, headerClicks)

        composeTestRule.onNodeWithText("Mangsir 2083").performClick()
        assertEquals(1, headerClicks)
    }

    @Test
    fun todayHeroHeader_hidesOptionalIndicators_whenAbsent() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                TodayHeroHeader(
                    state = TodayHeroUiModel(
                        bsDayNumber = "11",
                        bsMonthYear = "Mangsir 2083",
                        adDateLabel = "26 November 2026",
                        weekdayLabel = "Thursday",
                        tithiLabel = null,
                        lunarPhase = null,
                        isEstimatedTithi = false,
                        primaryFestival = null,
                        isHoliday = false
                    ),
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("11").assertIsDisplayed()
        composeTestRule.onNodeWithText("Public holiday").assertDoesNotExist()
        composeTestRule.onNodeWithText("(Est.)", substring = true).assertDoesNotExist()
    }

    @Test
    fun dayCell_exposesSelectedAndUnselectedSemantics_andInvokesClick() {
        var clicked = false
        val selectedDescription = "14 Mangsir 2083, 29 November 2026, Selected"
        val unselectedDescription = "15 Mangsir 2083, 30 November 2026"

        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                Row {
                    DayCell(
                        day = DayCellUiModel(
                            id = "day-14",
                            bsDayText = "14",
                            adDayText = "29",
                            isCurrentMonth = true,
                            isToday = false,
                            isSelected = true,
                            isSaturday = false,
                            isPublicHoliday = false,
                            hasEvents = false,
                            hasReminders = true,
                            accessibilityDescription = selectedDescription
                        ),
                        onClick = { clicked = true }
                    )
                    DayCell(
                        day = DayCellUiModel(
                            id = "day-15",
                            bsDayText = "15",
                            adDayText = "30",
                            isCurrentMonth = true,
                            isToday = false,
                            isSelected = false,
                            isSaturday = false,
                            isPublicHoliday = false,
                            hasEvents = false,
                            hasReminders = false,
                            accessibilityDescription = unselectedDescription
                        ),
                        onClick = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription(selectedDescription)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsSelected()
            .performClick()

        composeTestRule.onNodeWithContentDescription(unselectedDescription)
            .assertIsDisplayed()
            .assertIsNotSelected()

        assertEquals(true, clicked)
    }

    @Test
    fun monthGrid_rendersWeekdayHeaders_andInvokesCallbackWithClickedDay() {
        var clickedDay: DayCellUiModel? = null
        val headers = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val targetDay = regularTestDay(10).copy(
            adDayText = "25",
            isToday = true,
            isPublicHoliday = true,
            hasEvents = true,
            accessibilityDescription = "10 Mangsir 2083, 25 November 2026, Today"
        )
        val selectedDay = regularTestDay(14).copy(
            adDayText = "29",
            isSelected = true,
            hasReminders = true,
            accessibilityDescription = "14 Mangsir 2083, 29 November 2026"
        )
        val days = (1..35).map { index ->
            when (index) {
                10 -> targetDay
                14 -> selectedDay
                else -> regularTestDay(index)
            }
        }

        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                MonthGrid(
                    grid = MonthGridUiModel(
                        weekdayHeaders = headers,
                        saturdayColumnIndex = 6,
                        days = days
                    ),
                    onDayClick = { clickedDay = it }
                )
            }
        }

        headers.forEach { header -> composeTestRule.onNodeWithText(header).assertIsDisplayed() }
        composeTestRule.onNodeWithContentDescription(selectedDay.accessibilityDescription).assertIsSelected()
        composeTestRule.onNodeWithContentDescription(targetDay.accessibilityDescription)
            .assertIsNotSelected()
            .performClick()

        assertEquals(targetDay, clickedDay)
    }

    private fun regularTestDay(index: Int): DayCellUiModel = DayCellUiModel(
        id = "bs-2083-08-cell-$index",
        bsDayText = index.toString(),
        adDayText = ((index % 28) + 1).toString(),
        isCurrentMonth = index in 2..30,
        isToday = false,
        isSelected = false,
        isSaturday = index % 7 == 0,
        isPublicHoliday = false,
        hasEvents = false,
        hasReminders = false,
        accessibilityDescription = "Day $index"
    )
}
