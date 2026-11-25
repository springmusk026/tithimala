package app.tithimala.core.designsystem.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tithimala.core.designsystem.R
import app.tithimala.core.designsystem.component.TithimalaBrandMark
import app.tithimala.core.designsystem.component.TithimalaSectionHeader
import app.tithimala.core.designsystem.component.TithimalaSurfaceCard
import app.tithimala.core.designsystem.theme.TithimalaTheme

@Composable
internal fun HimalayanDawnShowcase(modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            ShowcaseBrandHeader()
            ShowcaseDateCard()
            TithimalaSectionHeader(title = stringResource(R.string.designsystem_palette_heading))
            ShowcasePaletteRow()
            ShowcaseActionRow()
        }
    }
}

@Composable
private fun ShowcaseBrandHeader() {
    val spacing = TithimalaTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        TithimalaBrandMark(
            size = 44.dp,
            contentDescription = stringResource(R.string.designsystem_brand_mark_cd)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.designsystem_brand_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.designsystem_theme_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = TithimalaTheme.colors.textMuted
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShowcaseDateCard() {
    val spacing = TithimalaTheme.spacing
    val extendedColors = TithimalaTheme.colors
    TithimalaSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.designsystem_sample_bs_date),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(spacing.micro))
        Text(
            text = stringResource(R.string.designsystem_sample_ad_date),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(spacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            TokenPill(
                label = stringResource(R.string.designsystem_sample_tithi),
                containerColor = extendedColors.tithiContainer,
                contentColor = extendedColors.onTithiContainer
            )
            TokenPill(
                label = stringResource(R.string.designsystem_sample_holiday),
                containerColor = extendedColors.holidayContainer,
                contentColor = extendedColors.onHolidayContainer
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShowcasePaletteRow() {
    val spacing = TithimalaTheme.spacing
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        ColorTokenSwatch(
            name = stringResource(R.string.designsystem_swatch_primary),
            color = MaterialTheme.colorScheme.primary
        )
        ColorTokenSwatch(
            name = stringResource(R.string.designsystem_swatch_secondary),
            color = MaterialTheme.colorScheme.secondary
        )
        ColorTokenSwatch(
            name = stringResource(R.string.designsystem_swatch_tertiary),
            color = MaterialTheme.colorScheme.tertiary
        )
        ColorTokenSwatch(
            name = stringResource(R.string.designsystem_swatch_surface),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShowcaseActionRow() {
    val spacing = TithimalaTheme.spacing
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Button(
            onClick = {},
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.heightIn(min = spacing.minTouchTarget)
        ) {
            Text(text = stringResource(R.string.designsystem_action_today))
        }
        OutlinedButton(
            onClick = {},
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(spacing.hairline, TithimalaTheme.colors.hairline),
            modifier = Modifier.heightIn(min = spacing.minTouchTarget)
        ) {
            Text(text = stringResource(R.string.designsystem_action_convert))
        }
    }
}

@Composable
private fun TokenPill(label: String, containerColor: Color, contentColor: Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(
                horizontal = TithimalaTheme.spacing.xs,
                vertical = TithimalaTheme.spacing.micro
            )
        )
    }
}

@Composable
private fun ColorTokenSwatch(name: String, color: Color) {
    val spacing = TithimalaTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .border(spacing.hairline, TithimalaTheme.colors.hairline, MaterialTheme.shapes.small)
            .padding(horizontal = spacing.xs, vertical = spacing.micro)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
                .border(spacing.hairline, TithimalaTheme.colors.hairline, CircleShape)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@TithimalaThemePreviews
@Composable
private fun HimalayanDawnShowcasePreview() {
    TithimalaTheme {
        HimalayanDawnShowcase()
    }
}
