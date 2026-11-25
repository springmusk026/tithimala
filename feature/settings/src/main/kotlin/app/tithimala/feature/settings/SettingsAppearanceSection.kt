package app.tithimala.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.tithimala.core.calendarengine.DevanagariNumerals
import app.tithimala.core.designsystem.component.TithimalaSectionHeader
import app.tithimala.core.designsystem.component.TithimalaSurfaceCard
import app.tithimala.core.designsystem.theme.TithimalaMotion
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.designsystem.theme.rememberReducedMotionEnabled
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.NumeralScript
import app.tithimala.core.model.ThemeMode
import app.tithimala.core.model.UserPreferences
import app.tithimala.core.model.WeekStartDay

private val OPTION_CHECK_ICON_SIZE = 16.dp
private val DEVANAGARI_DIGIT_ZERO = DevanagariNumerals.format(0)
private val DEVANAGARI_DIGIT_NINE = DevanagariNumerals.format(9)
private const val LATIN_DIGIT_ZERO = "0"
private const val LATIN_DIGIT_NINE = "9"

@Composable
internal fun AppearanceAndLanguageSection(
    preferences: UserPreferences,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        TithimalaSectionHeader(
            title = stringResource(R.string.settings_section_appearance_language)
        )
        TithimalaSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(spacing.md)
        ) {
            LanguageOptionGroup(
                selected = preferences.language,
                onSelect = { onAction(SettingsAction.SelectLanguage(it)) }
            )
            SettingsCardDivider()
            NumeralScriptOptionGroup(
                selected = preferences.numeralScript,
                onSelect = { onAction(SettingsAction.SelectNumeralScript(it)) }
            )
            SettingsCardDivider()
            ThemeModeOptionGroup(
                selected = preferences.themeMode,
                onSelect = { onAction(SettingsAction.SelectThemeMode(it)) }
            )
            SettingsCardDivider()
            WeekStartOptionGroup(
                selected = preferences.weekStartDay,
                onSelect = { onAction(SettingsAction.SelectWeekStartDay(it)) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageOptionGroup(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    PreferenceOptionGroup(label = stringResource(R.string.settings_label_language)) {
        PreferenceOptionChip(
            label = stringResource(R.string.settings_language_system),
            selected = selected == AppLanguage.SYSTEM,
            onClick = { onSelect(AppLanguage.SYSTEM) }
        )
        PreferenceOptionChip(
            label = stringResource(R.string.settings_language_english),
            selected = selected == AppLanguage.ENGLISH,
            onClick = { onSelect(AppLanguage.ENGLISH) }
        )
        PreferenceOptionChip(
            label = stringResource(R.string.settings_language_nepali),
            selected = selected == AppLanguage.NEPALI,
            onClick = { onSelect(AppLanguage.NEPALI) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumeralScriptOptionGroup(selected: NumeralScript, onSelect: (NumeralScript) -> Unit) {
    PreferenceOptionGroup(label = stringResource(R.string.settings_label_numerals)) {
        PreferenceOptionChip(
            label = stringResource(
                R.string.settings_numeral_devanagari,
                DEVANAGARI_DIGIT_ZERO,
                DEVANAGARI_DIGIT_NINE
            ),
            selected = selected == NumeralScript.DEVANAGARI,
            onClick = { onSelect(NumeralScript.DEVANAGARI) }
        )
        PreferenceOptionChip(
            label = stringResource(
                R.string.settings_numeral_latin,
                LATIN_DIGIT_ZERO,
                LATIN_DIGIT_NINE
            ),
            selected = selected == NumeralScript.LATIN,
            onClick = { onSelect(NumeralScript.LATIN) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeModeOptionGroup(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    PreferenceOptionGroup(label = stringResource(R.string.settings_label_theme)) {
        PreferenceOptionChip(
            label = stringResource(R.string.settings_theme_system),
            selected = selected == ThemeMode.SYSTEM,
            onClick = { onSelect(ThemeMode.SYSTEM) }
        )
        PreferenceOptionChip(
            label = stringResource(R.string.settings_theme_light),
            selected = selected == ThemeMode.LIGHT,
            onClick = { onSelect(ThemeMode.LIGHT) }
        )
        PreferenceOptionChip(
            label = stringResource(R.string.settings_theme_dark),
            selected = selected == ThemeMode.DARK,
            onClick = { onSelect(ThemeMode.DARK) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekStartOptionGroup(selected: WeekStartDay, onSelect: (WeekStartDay) -> Unit) {
    PreferenceOptionGroup(label = stringResource(R.string.settings_label_week_start)) {
        PreferenceOptionChip(
            label = stringResource(R.string.settings_week_start_sunday),
            selected = selected == WeekStartDay.SUNDAY,
            onClick = { onSelect(WeekStartDay.SUNDAY) }
        )
        PreferenceOptionChip(
            label = stringResource(R.string.settings_week_start_monday),
            selected = selected == WeekStartDay.MONDAY,
            onClick = { onSelect(WeekStartDay.MONDAY) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceOptionGroup(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit
) {
    val spacing = TithimalaTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
            content = content
        )
    }
}

@Composable
private fun PreferenceOptionChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    val reducedMotion = rememberReducedMotionEnabled()
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
        animationSpec = TithimalaMotion.standardSpring(reducedMotion = reducedMotion),
        label = "PreferenceOptionContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colorScheme.primary else TithimalaTheme.colors.hairline,
        animationSpec = TithimalaMotion.standardSpring(reducedMotion = reducedMotion),
        label = "PreferenceOptionBorder"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
        animationSpec = TithimalaMotion.standardSpring(reducedMotion = reducedMotion),
        label = "PreferenceOptionContent"
    )
    val chipShape = MaterialTheme.shapes.small

    Surface(
        modifier = modifier
            .heightIn(min = spacing.minTouchTarget)
            .clip(chipShape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = chipShape,
        color = containerColor,
        border = BorderStroke(width = spacing.hairline, color = borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.micro)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(OPTION_CHECK_ICON_SIZE)
                )
            }
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
        }
    }
}

@Composable
internal fun SettingsCardDivider(modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing
    HorizontalDivider(
        modifier = modifier.padding(vertical = spacing.sm),
        thickness = spacing.hairline,
        color = TithimalaTheme.colors.hairline
    )
}
