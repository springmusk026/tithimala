package app.tithimala.feature.converter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.tithimala.core.calendarengine.BsDateFormatter
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.NumeralScript

private val STEPPER_ICON_SIZE = 20.dp

@Composable
internal fun YearInputField(
    yearInput: String,
    isError: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = yearInput,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(text = stringResource(R.string.converter_label_year)) },
        singleLine = true,
        isError = isError,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonthSelectorField(
    monthIndex: Int,
    monthLabels: MonthLabels,
    numberScript: NumeralScript,
    onSelectMonth: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val safeIndex = (monthIndex - 1).coerceIn(0, monthLabels.items.lastIndex)
    val selectedMonthName = monthLabels.items[safeIndex]

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedMonthName,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(text = stringResource(R.string.converter_label_month)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            monthLabels.items.forEachIndexed { index, name ->
                val monthNumber = index + 1
                MonthDropdownOption(
                    formattedNumber = BsDateFormatter.formatNumber(monthNumber, numberScript),
                    monthName = name,
                    isSelected = monthNumber == monthIndex,
                    onClick = {
                        expanded = false
                        onSelectMonth(monthNumber)
                    }
                )
            }
        }
    }
}

@Composable
private fun MonthDropdownOption(formattedNumber: String, monthName: String, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        modifier = Modifier.heightIn(min = TithimalaTheme.spacing.minTouchTarget),
        text = {
            Text(
                text = stringResource(R.string.converter_month_option_format, formattedNumber, monthName),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        trailingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            null
        },
        onClick = onClick
    )
}

@Composable
internal fun DayStepperRow(
    direction: ConversionDirection,
    dayInput: String,
    isError: Boolean,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onValueChange: (String) -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing
    val focusManager = LocalFocusManager.current
    val dayLabel = when (direction) {
        ConversionDirection.BS_TO_AD -> stringResource(R.string.converter_label_day_bs)
        ConversionDirection.AD_TO_BS -> stringResource(R.string.converter_label_day_ad)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        DayStepperButton(
            icon = Icons.Rounded.Remove,
            contentDescription = stringResource(R.string.converter_action_previous_day_cd),
            enabled = canDecrement,
            onClick = { onStep(-1) },
            modifier = Modifier.padding(top = spacing.xs)
        )
        OutlinedTextField(
            value = dayInput,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            label = { Text(text = dayLabel) },
            singleLine = true,
            isError = isError,
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        DayStepperButton(
            icon = Icons.Rounded.Add,
            contentDescription = stringResource(R.string.converter_action_next_day_cd),
            enabled = canIncrement,
            onClick = { onStep(1) },
            modifier = Modifier.padding(top = spacing.xs)
        )
    }
}

@Composable
private fun DayStepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    val iconTint = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        extendedColors.textMuted
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.sizeIn(
            minWidth = spacing.minTouchTarget,
            minHeight = spacing.minTouchTarget
        ),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(width = spacing.hairline, color = extendedColors.hairline)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(STEPPER_ICON_SIZE)
            )
        }
    }
}
