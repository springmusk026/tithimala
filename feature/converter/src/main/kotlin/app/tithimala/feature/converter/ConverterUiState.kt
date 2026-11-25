package app.tithimala.feature.converter

import androidx.compose.runtime.Immutable
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.ui.calendar.LunarPhaseVisual
import java.time.LocalDate

@Immutable
internal enum class ConversionDirection {
    BS_TO_AD,
    AD_TO_BS;

    fun opposite(): ConversionDirection = when (this) {
        BS_TO_AD -> AD_TO_BS
        AD_TO_BS -> BS_TO_AD
    }
}

@Immutable
internal sealed interface ConverterValidationError {
    data object EmptyInput : ConverterValidationError
    data object EmptyYear : ConverterValidationError
    data class EmptyDay(val maxDay: Int) : ConverterValidationError
    data class YearOutOfRange(val minYear: Int, val maxYear: Int) : ConverterValidationError
    data class InvalidDayForMonth(val month: Int, val year: Int, val maxDay: Int) : ConverterValidationError
    data class DateBeforeSupportedRange(val minDateLabel: String) : ConverterValidationError
    data class DateAfterSupportedRange(val maxDateLabel: String) : ConverterValidationError
}

@Immutable
internal data class ConvertedDateResult(
    val bsDate: BsDate,
    val adDate: LocalDate,
    val primaryDateText: String,
    val secondaryDateText: String,
    val weekdayText: String,
    val tithiLabel: String?,
    val lunarPhase: LunarPhaseVisual?,
    val isEstimatedTithi: Boolean,
    val isPublicHoliday: Boolean,
    val primaryEvent: String?,
    val copyText: String
)

@Immutable
internal data class ConverterUiState(
    val direction: ConversionDirection,
    val yearInput: String,
    val monthIndex: Int,
    val dayInput: String,
    val maxDayForCurrentMonth: Int,
    val minSupportedYear: Int,
    val maxSupportedYear: Int,
    val patroFirstYear: Int? = null,
    val patroLastYear: Int? = null,
    val numeralScript: NumeralScript = NumeralScript.LATIN,
    val canDecrementDay: Boolean = true,
    val canIncrementDay: Boolean = true,
    val validationError: ConverterValidationError? = null,
    val result: ConvertedDateResult? = null
)

internal sealed interface ConverterAction {
    data class SetDirection(val direction: ConversionDirection) : ConverterAction
    data object SwapDirection : ConverterAction
    data class UpdateYear(val value: String) : ConverterAction
    data class SelectMonth(val month: Int) : ConverterAction
    data class UpdateDay(val value: String) : ConverterAction
    data class StepDay(val delta: Int) : ConverterAction
    data object ResetToToday : ConverterAction
    data object CopyResult : ConverterAction
    data object OpenInCalendar : ConverterAction
}

internal sealed interface ConverterEffect {
    data class CopyToClipboard(val text: String) : ConverterEffect
    data class OpenDay(val bsDate: BsDate) : ConverterEffect
}
