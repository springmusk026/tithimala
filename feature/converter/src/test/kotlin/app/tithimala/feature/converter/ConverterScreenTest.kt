package app.tithimala.feature.converter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.NumeralScript
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class ConverterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysConvertedResultAndDispatchesActions() {
        val actions = mutableListOf<ConverterAction>()

        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                ConverterScreen(
                    state = converterPreviewState(),
                    onAction = actions::add
                )
            }
        }

        composeTestRule.onNodeWithText("Date converter").assertIsDisplayed()
        composeTestRule.onNodeWithText("14 April 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tuesday · Baishakh 1, 2083").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dwadashi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Public holiday").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mesh Sankranti").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Convert from Bikram Sambat to Gregorian")
            .assertIsSelected()
        composeTestRule.onNodeWithContentDescription("Convert from Gregorian to Bikram Sambat")
            .assertIsNotSelected()
            .performClick()
        composeTestRule.onNodeWithContentDescription("Swap conversion direction").performClick()
        composeTestRule.onNodeWithText("Today").performClick()
        composeTestRule.onNodeWithText("Copy date").performClick()
        composeTestRule.onNodeWithText("View day details").performClick()

        assertEquals(
            listOf(
                ConverterAction.SetDirection(ConversionDirection.AD_TO_BS),
                ConverterAction.SwapDirection,
                ConverterAction.ResetToToday,
                ConverterAction.CopyResult,
                ConverterAction.OpenInCalendar
            ),
            actions
        )
    }

    @Test
    fun displaysInlineValidationErrorWhenDateIsInvalid() {
        composeTestRule.setContent {
            TithimalaTheme(darkTheme = false) {
                ConverterScreen(
                    state = ConverterUiState(
                        direction = ConversionDirection.BS_TO_AD,
                        yearInput = "2081",
                        monthIndex = 1,
                        dayInput = "32",
                        maxDayForCurrentMonth = 31,
                        minSupportedYear = 1901,
                        maxSupportedYear = 2199,
                        numeralScript = NumeralScript.LATIN,
                        canDecrementDay = true,
                        canIncrementDay = false,
                        validationError = ConverterValidationError.InvalidDayForMonth(
                            month = 1,
                            year = 2081,
                            maxDay = 31
                        ),
                        result = null
                    ),
                    onAction = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Baishakh 2081 has 31 days (enter 1–31)").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Enter a valid date above to see the converted date, weekday, and Tithi"
        ).assertIsDisplayed()
    }
}
