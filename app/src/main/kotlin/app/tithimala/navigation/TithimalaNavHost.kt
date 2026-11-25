package app.tithimala.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import app.tithimala.feature.calendar.CalendarRoute
import app.tithimala.feature.converter.ConverterRoute
import app.tithimala.feature.day.DayRoute
import app.tithimala.feature.reminders.RemindersRoute
import app.tithimala.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
private data object CalendarDestination : NavKey

@Serializable
private data object DayDestination : NavKey

@Serializable
private data object ConverterDestination : NavKey

@Serializable
private data object RemindersDestination : NavKey

@Serializable
private data object SettingsDestination : NavKey

@Composable
internal fun TithimalaNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(CalendarDestination)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryProvider = entryProvider {
            entry<CalendarDestination> {
                CalendarRoute()
            }
            entry<DayDestination> {
                DayRoute()
            }
            entry<ConverterDestination> {
                ConverterRoute()
            }
            entry<RemindersDestination> {
                RemindersRoute()
            }
            entry<SettingsDestination> {
                SettingsRoute()
            }
        }
    )
}
