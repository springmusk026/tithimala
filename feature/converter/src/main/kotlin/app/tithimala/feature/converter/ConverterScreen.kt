package app.tithimala.feature.converter

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tithimala.core.designsystem.preview.TithimalaThemePreviews
import app.tithimala.core.designsystem.theme.TithimalaMotion
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.designsystem.theme.rememberReducedMotionEnabled
import app.tithimala.core.model.BsDate
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.ui.calendar.LunarPhaseVisual
import java.time.LocalDate

private const val MAX_SEGMENT_LINES = 2
private const val NEPALI_LANGUAGE_CODE = "ne"
private val HEADER_ICON_SIZE = 18.dp
private val SWAP_ICON_SIZE = 20.dp

@Composable
internal fun ConverterScreen(
    state: ConverterUiState,
    onAction: (ConverterAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.md, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            ConverterHeader(onResetToToday = { onAction(ConverterAction.ResetToToday) })

            DirectionToggleBar(
                direction = state.direction,
                onSelectDirection = { onAction(ConverterAction.SetDirection(it)) },
                onSwap = { onAction(ConverterAction.SwapDirection) }
            )

            ConverterInputCard(
                state = state,
                onAction = onAction
            )

            ConverterResultCard(
                state = state,
                onAction = onAction
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConverterHeader(onResetToToday: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.micro)) {
            Text(
                text = stringResource(R.string.converter_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.converter_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = onResetToToday,
            modifier = Modifier.heightIn(min = spacing.minTouchTarget),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(width = spacing.hairline, color = extendedColors.hairline),
            contentPadding = PaddingValues(horizontal = spacing.sm, vertical = spacing.xs)
        ) {
            Icon(
                imageVector = Icons.Rounded.Today,
                contentDescription = null,
                modifier = Modifier.size(HEADER_ICON_SIZE)
            )
            Spacer(modifier = Modifier.width(spacing.xs))
            Text(text = stringResource(R.string.converter_action_today))
        }
    }
}

@Composable
private fun DirectionToggleBar(
    direction: ConversionDirection,
    onSelectDirection: (ConversionDirection) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(width = spacing.hairline, color = extendedColors.hairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.micro)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(spacing.micro)
            ) {
                DirectionSegment(
                    label = stringResource(R.string.converter_direction_bs_to_ad),
                    accessibilityLabel = stringResource(R.string.converter_direction_bs_to_ad_cd),
                    selected = direction == ConversionDirection.BS_TO_AD,
                    onClick = { onSelectDirection(ConversionDirection.BS_TO_AD) },
                    modifier = Modifier.weight(1f)
                )
                DirectionSegment(
                    label = stringResource(R.string.converter_direction_ad_to_bs),
                    accessibilityLabel = stringResource(R.string.converter_direction_ad_to_bs_cd),
                    selected = direction == ConversionDirection.AD_TO_BS,
                    onClick = { onSelectDirection(ConversionDirection.AD_TO_BS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SwapDirectionButton(onClick = onSwap)
    }
}

@Composable
private fun SwapDirectionButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .widthIn(min = spacing.minTouchTarget)
            .heightIn(min = spacing.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(width = spacing.hairline, color = TithimalaTheme.colors.hairline)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = spacing.sm),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SwapHoriz,
                contentDescription = stringResource(R.string.converter_action_swap_cd),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(SWAP_ICON_SIZE)
            )
        }
    }
}

@Composable
private fun DirectionSegment(
    label: String,
    accessibilityLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing
    val reducedMotion = rememberReducedMotionEnabled()
    val segmentShape = MaterialTheme.shapes.small
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = TithimalaMotion.standardSpring(reducedMotion),
        label = "converter_segment_container"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = TithimalaMotion.standardSpring(reducedMotion),
        label = "converter_segment_content"
    )

    Surface(
        modifier = modifier
            .heightIn(min = spacing.minTouchTarget)
            .clip(segmentShape)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics { contentDescription = accessibilityLabel },
        shape = segmentShape,
        color = containerColor
    ) {
        Box(
            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = MAX_SEGMENT_LINES,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun converterPreviewState(): ConverterUiState {
    val primaryLocale = LocalConfiguration.current.locales.get(0)
    val script = if (primaryLocale?.language == NEPALI_LANGUAGE_CODE) {
        NumeralScript.DEVANAGARI
    } else {
        NumeralScript.LATIN
    }
    return ConverterUiState(
        direction = ConversionDirection.BS_TO_AD,
        yearInput = stringResource(R.string.converter_preview_year),
        monthIndex = 1,
        dayInput = stringResource(R.string.converter_preview_day),
        maxDayForCurrentMonth = 31,
        minSupportedYear = 1901,
        maxSupportedYear = 2199,
        patroFirstYear = 2077,
        patroLastYear = 2084,
        numeralScript = script,
        canDecrementDay = false,
        canIncrementDay = true,
        validationError = null,
        result = ConvertedDateResult(
            bsDate = BsDate(year = 2083, month = 1, day = 1),
            adDate = LocalDate.of(2026, 4, 14),
            primaryDateText = stringResource(R.string.converter_preview_primary_date),
            secondaryDateText = stringResource(R.string.converter_preview_secondary_date),
            weekdayText = stringResource(R.string.converter_preview_weekday),
            tithiLabel = stringResource(R.string.converter_preview_tithi),
            lunarPhase = LunarPhaseVisual.WAXING_GIBBOUS,
            isEstimatedTithi = false,
            isPublicHoliday = true,
            primaryEvent = stringResource(R.string.converter_preview_event),
            copyText = stringResource(R.string.converter_preview_copy_text)
        )
    )
}

@TithimalaThemePreviews
@Composable
private fun ConverterScreenPreview() {
    TithimalaTheme {
        ConverterScreen(
            state = converterPreviewState(),
            onAction = {}
        )
    }
}
