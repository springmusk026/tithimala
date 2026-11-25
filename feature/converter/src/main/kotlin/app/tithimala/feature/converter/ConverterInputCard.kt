package app.tithimala.feature.converter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tithimala.core.calendarengine.BsDateFormatter
import app.tithimala.core.designsystem.component.TithimalaSurfaceCard
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.NumeralScript

private const val STACKED_FONT_SCALE_THRESHOLD = 1.4f
private const val YEAR_FIELD_WEIGHT = 1f
private const val MONTH_FIELD_WEIGHT = 1.55f
private const val NEPALI_LOCALE_LANGUAGE = "ne"
private val ERROR_ICON_SIZE = 16.dp
private val ERROR_ICON_TOP_OFFSET = 2.dp

@Immutable
internal data class MonthLabels(val items: List<String>)

@Composable
internal fun ConverterInputCard(
    state: ConverterUiState,
    onAction: (ConverterAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing
    val configuration = LocalConfiguration.current
    val primaryLocale = configuration.locales.get(0)
    val isNepaliLocale = primaryLocale?.language == NEPALI_LOCALE_LANGUAGE
    val numberScript = when {
        state.direction == ConversionDirection.BS_TO_AD -> state.numeralScript
        isNepaliLocale -> state.numeralScript
        else -> NumeralScript.LATIN
    }
    val monthArrayRes = when (state.direction) {
        ConversionDirection.BS_TO_AD -> R.array.converter_bs_months
        ConversionDirection.AD_TO_BS -> R.array.converter_ad_months
    }
    val rawMonthNames = stringArrayResource(monthArrayRes)
    val monthLabels = remember(monthArrayRes, configuration) {
        MonthLabels(rawMonthNames.toList())
    }

    TithimalaSurfaceCard(modifier = modifier.fillMaxWidth()) {
        InputCardHeader(state = state, numberScript = numberScript)

        Spacer(modifier = Modifier.height(spacing.sm))

        YearMonthFields(
            state = state,
            monthLabels = monthLabels,
            numberScript = numberScript,
            onAction = onAction
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        DayStepperRow(
            direction = state.direction,
            dayInput = state.dayInput,
            isError = isDayError(state.validationError),
            canDecrement = state.canDecrementDay,
            canIncrement = state.canIncrementDay,
            onValueChange = { onAction(ConverterAction.UpdateDay(it)) },
            onStep = { onAction(ConverterAction.StepDay(it)) }
        )

        Spacer(modifier = Modifier.height(spacing.xs))

        InputValidationFooter(
            state = state,
            monthLabels = monthLabels,
            numberScript = numberScript
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputCardHeader(state: ConverterUiState, numberScript: NumeralScript) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val cardTitle = when (state.direction) {
        ConversionDirection.BS_TO_AD -> stringResource(R.string.converter_input_bs_title)
        ConversionDirection.AD_TO_BS -> stringResource(R.string.converter_input_ad_title)
    }
    val supportedRangeText = when (state.direction) {
        ConversionDirection.BS_TO_AD -> stringResource(
            R.string.converter_supported_bs_years_format,
            BsDateFormatter.formatNumber(state.minSupportedYear, numberScript),
            BsDateFormatter.formatNumber(state.maxSupportedYear, numberScript)
        )
        ConversionDirection.AD_TO_BS -> stringResource(
            R.string.converter_supported_ad_years_format,
            BsDateFormatter.formatNumber(state.minSupportedYear, numberScript),
            BsDateFormatter.formatNumber(state.maxSupportedYear, numberScript)
        )
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(spacing.micro),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = cardTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = supportedRangeText,
            style = MaterialTheme.typography.bodySmall,
            color = extendedColors.textMuted
        )
    }
}

@Composable
private fun YearMonthFields(
    state: ConverterUiState,
    monthLabels: MonthLabels,
    numberScript: NumeralScript,
    onAction: (ConverterAction) -> Unit
) {
    val spacing = TithimalaTheme.spacing
    val useStackedFields = LocalDensity.current.fontScale >= STACKED_FONT_SCALE_THRESHOLD

    if (useStackedFields) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            YearInputField(
                yearInput = state.yearInput,
                isError = isYearError(state.validationError),
                onValueChange = { onAction(ConverterAction.UpdateYear(it)) },
                modifier = Modifier.fillMaxWidth()
            )
            MonthSelectorField(
                monthIndex = state.monthIndex,
                monthLabels = monthLabels,
                numberScript = numberScript,
                onSelectMonth = { onAction(ConverterAction.SelectMonth(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            YearInputField(
                yearInput = state.yearInput,
                isError = isYearError(state.validationError),
                onValueChange = { onAction(ConverterAction.UpdateYear(it)) },
                modifier = Modifier.weight(YEAR_FIELD_WEIGHT)
            )
            MonthSelectorField(
                monthIndex = state.monthIndex,
                monthLabels = monthLabels,
                numberScript = numberScript,
                onSelectMonth = { onAction(ConverterAction.SelectMonth(it)) },
                modifier = Modifier.weight(MONTH_FIELD_WEIGHT)
            )
        }
    }
}

@Composable
private fun InputValidationFooter(state: ConverterUiState, monthLabels: MonthLabels, numberScript: NumeralScript) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val error = state.validationError

    if (error != null) {
        val errorText = validationErrorMessage(
            error = error,
            monthLabels = monthLabels,
            numberScript = numberScript
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = ERROR_ICON_TOP_OFFSET)
                    .size(ERROR_ICON_SIZE)
            )
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    } else {
        Text(
            text = stringResource(
                R.string.converter_day_range_helper_format,
                BsDateFormatter.formatNumber(ConverterEvaluator.MIN_DAY, numberScript),
                BsDateFormatter.formatNumber(state.maxDayForCurrentMonth, numberScript)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = extendedColors.textMuted
        )
    }
}

@Composable
private fun validationErrorMessage(
    error: ConverterValidationError,
    monthLabels: MonthLabels,
    numberScript: NumeralScript
): String = when (error) {
    ConverterValidationError.EmptyInput -> stringResource(R.string.converter_error_empty_input)
    ConverterValidationError.EmptyYear -> stringResource(R.string.converter_error_empty_year)
    is ConverterValidationError.EmptyDay -> stringResource(
        R.string.converter_error_empty_day_format,
        BsDateFormatter.formatNumber(ConverterEvaluator.MIN_DAY, numberScript),
        BsDateFormatter.formatNumber(error.maxDay, numberScript)
    )
    is ConverterValidationError.YearOutOfRange -> stringResource(
        R.string.converter_error_year_range_format,
        BsDateFormatter.formatNumber(error.minYear, numberScript),
        BsDateFormatter.formatNumber(error.maxYear, numberScript)
    )
    is ConverterValidationError.InvalidDayForMonth -> {
        val monthLabel = monthLabels.items[(error.month - 1).coerceIn(0, monthLabels.items.lastIndex)]
        stringResource(
            R.string.converter_error_invalid_day_format,
            monthLabel,
            BsDateFormatter.formatNumber(error.year, numberScript),
            BsDateFormatter.formatNumber(error.maxDay, numberScript),
            BsDateFormatter.formatNumber(ConverterEvaluator.MIN_DAY, numberScript)
        )
    }
    is ConverterValidationError.DateBeforeSupportedRange -> stringResource(
        R.string.converter_error_date_before_min_format,
        error.minDateLabel
    )
    is ConverterValidationError.DateAfterSupportedRange -> stringResource(
        R.string.converter_error_date_after_max_format,
        error.maxDateLabel
    )
}

private fun isYearError(error: ConverterValidationError?): Boolean = when (error) {
    ConverterValidationError.EmptyInput,
    ConverterValidationError.EmptyYear,
    is ConverterValidationError.YearOutOfRange,
    is ConverterValidationError.DateBeforeSupportedRange,
    is ConverterValidationError.DateAfterSupportedRange -> true
    else -> false
}

private fun isDayError(error: ConverterValidationError?): Boolean = when (error) {
    ConverterValidationError.EmptyInput,
    is ConverterValidationError.EmptyDay,
    is ConverterValidationError.InvalidDayForMonth,
    is ConverterValidationError.DateBeforeSupportedRange,
    is ConverterValidationError.DateAfterSupportedRange -> true
    else -> false
}
