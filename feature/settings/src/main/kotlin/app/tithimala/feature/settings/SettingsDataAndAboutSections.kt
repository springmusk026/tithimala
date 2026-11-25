package app.tithimala.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tithimala.core.calendarengine.BsDateFormatter
import app.tithimala.core.designsystem.component.TithimalaBrandMark
import app.tithimala.core.designsystem.component.TithimalaSectionHeader
import app.tithimala.core.designsystem.component.TithimalaSurfaceCard
import app.tithimala.core.designsystem.theme.TithimalaTheme
import app.tithimala.core.model.AppLanguage
import app.tithimala.core.model.NumeralScript

private val ACTION_ICON_SIZE = 18.dp
private val PROGRESS_STROKE_WIDTH = 2.dp
private val BRAND_MARK_SIZE = 48.dp
private val CHEVRON_ICON_SIZE = 20.dp
private const val NEPALI_LANGUAGE_CODE = "ne"

@Composable
internal fun CalendarDataAndUpdatesSection(
    dataset: DatasetSummaryUiModel,
    language: AppLanguage,
    numeralScript: NumeralScript,
    backgroundSyncEnabled: Boolean,
    isCheckingForUpdates: Boolean,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        TithimalaSectionHeader(
            title = stringResource(R.string.settings_section_data_updates)
        )
        TithimalaSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(spacing.md)
        ) {
            BackgroundSyncToggleRow(
                checked = backgroundSyncEnabled,
                onCheckedChange = { onAction(SettingsAction.SetBackgroundSyncEnabled(it)) }
            )
            SettingsCardDivider()
            DatasetProvenanceDetails(
                dataset = dataset,
                language = language,
                numeralScript = numeralScript
            )
            Spacer(modifier = Modifier.height(spacing.md))
            CheckForUpdatesButton(
                isChecking = isCheckingForUpdates,
                onClick = { onAction(SettingsAction.CheckForUpdates) }
            )
        }
    }
}

@Composable
private fun BackgroundSyncToggleRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val spacing = TithimalaTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.minTouchTarget)
            .clip(MaterialTheme.shapes.small)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.micro)
        ) {
            Text(
                text = stringResource(R.string.settings_background_sync_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.settings_background_sync_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Composable
private fun DatasetProvenanceDetails(
    dataset: DatasetSummaryUiModel,
    language: AppLanguage,
    numeralScript: NumeralScript
) {
    val spacing = TithimalaTheme.spacing
    val activeLanguage = resolveActiveLanguage(language)
    val formattedVersion = BsDateFormatter.formatDigits(dataset.version, numeralScript)
    val formattedPatroYears = formatPatroYearsLabel(dataset, numeralScript)
    val formattedLastSync = formatLastSyncLabel(dataset, activeLanguage, numeralScript)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        DatasetMetadataRow(
            label = stringResource(R.string.settings_dataset_version_label),
            value = formattedVersion
        )
        DatasetMetadataRow(
            label = stringResource(R.string.settings_dataset_patro_years_label),
            value = formattedPatroYears
        )
        DatasetMetadataRow(
            label = stringResource(R.string.settings_dataset_commit_label),
            value = dataset.upstreamCommitShort
        )
        DatasetMetadataRow(
            label = stringResource(R.string.settings_dataset_last_sync_label),
            value = formattedLastSync
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DatasetMetadataRow(label: String, value: String) {
    val spacing = TithimalaTheme.spacing

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(spacing.micro)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CheckForUpdatesButton(isChecking: Boolean, onClick: () -> Unit) {
    val spacing = TithimalaTheme.spacing

    OutlinedButton(
        onClick = onClick,
        enabled = !isChecking,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            width = spacing.hairline,
            color = TithimalaTheme.colors.hairline
        ),
        contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.minTouchTarget)
    ) {
        if (isChecking) {
            CircularProgressIndicator(
                modifier = Modifier.size(ACTION_ICON_SIZE),
                strokeWidth = PROGRESS_STROKE_WIDTH,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(spacing.xs))
            Text(
                text = stringResource(R.string.settings_action_checking_updates),
                style = MaterialTheme.typography.labelLarge
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(ACTION_ICON_SIZE)
            )
            Spacer(modifier = Modifier.width(spacing.xs))
            Text(
                text = stringResource(R.string.settings_action_check_updates),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
internal fun AboutAndSourcesSection(onOpenLicenses: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TithimalaTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        TithimalaSectionHeader(
            title = stringResource(R.string.settings_section_about)
        )
        TithimalaSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                TithimalaBrandMark(
                    size = BRAND_MARK_SIZE,
                    contentDescription = stringResource(R.string.settings_brand_mark_cd)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.micro)
                ) {
                    Text(
                        text = stringResource(R.string.settings_app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_app_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = stringResource(R.string.settings_license_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SettingsCardDivider()
            DataSourcesRow(onClick = onOpenLicenses)
        }
    }
}

@Composable
private fun DataSourcesRow(onClick: () -> Unit) {
    val spacing = TithimalaTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.minTouchTarget)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ACTION_ICON_SIZE)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.micro)
        ) {
            Text(
                text = stringResource(R.string.settings_action_data_sources),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.settings_action_data_sources_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(CHEVRON_ICON_SIZE)
        )
    }
}

@Composable
private fun resolveActiveLanguage(language: AppLanguage): AppLanguage = when (language) {
    AppLanguage.ENGLISH -> AppLanguage.ENGLISH
    AppLanguage.NEPALI -> AppLanguage.NEPALI
    AppLanguage.SYSTEM -> {
        val localeLanguage = LocalConfiguration.current.locales.get(0)?.language
        if (localeLanguage == NEPALI_LANGUAGE_CODE) AppLanguage.NEPALI else AppLanguage.ENGLISH
    }
}

@Composable
private fun formatPatroYearsLabel(dataset: DatasetSummaryUiModel, numeralScript: NumeralScript): String {
    val startYear = dataset.patroStartYear
    val endYear = dataset.patroEndYear
    return when {
        startYear == null || endYear == null -> stringResource(R.string.settings_dataset_patro_years_unavailable)
        startYear == endYear -> stringResource(
            R.string.settings_dataset_patro_years_single,
            BsDateFormatter.formatNumber(startYear, numeralScript)
        )
        else -> stringResource(
            R.string.settings_dataset_patro_years_range,
            BsDateFormatter.formatNumber(startYear, numeralScript),
            BsDateFormatter.formatNumber(endYear, numeralScript)
        )
    }
}

@Composable
private fun formatLastSyncLabel(
    dataset: DatasetSummaryUiModel,
    language: AppLanguage,
    numeralScript: NumeralScript
): String {
    val syncedDate = dataset.lastSyncBsDate ?: return stringResource(R.string.settings_dataset_last_sync_never)
    val formattedDate = BsDateFormatter.formatLongDate(
        date = syncedDate,
        language = language,
        script = numeralScript,
        zeroPadDay = false
    )
    return if (dataset.isRemoteUpdateActive) {
        stringResource(R.string.settings_dataset_last_sync_remote_format, formattedDate)
    } else {
        stringResource(R.string.settings_dataset_last_sync_bundled_format, formattedDate)
    }
}
