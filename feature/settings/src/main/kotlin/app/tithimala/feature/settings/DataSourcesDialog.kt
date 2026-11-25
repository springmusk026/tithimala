package app.tithimala.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.tithimala.core.designsystem.theme.TithimalaTheme

@Composable
internal fun DataSourcesLicensesDialog(
    dataset: DatasetSummaryUiModel,
    noticeText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TithimalaTheme.spacing

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = {
            Text(
                text = stringResource(R.string.settings_licenses_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                LicenseSubsection(
                    heading = stringResource(R.string.settings_licenses_calendar_heading),
                    body = stringResource(
                        R.string.settings_licenses_calendar_summary,
                        dataset.upstreamRepo,
                        dataset.license
                    )
                )
                SettingsCardDivider()
                FontCreditsBlock()
                SettingsCardDivider()
                LicenseSubsection(
                    heading = stringResource(R.string.settings_licenses_app_heading),
                    body = stringResource(R.string.settings_licenses_app_detail)
                )
                if (noticeText.isNotBlank()) {
                    SettingsCardDivider()
                    NoticeBlock(noticeText = noticeText)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = spacing.minTouchTarget)
            ) {
                Text(
                    text = stringResource(R.string.settings_action_close),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

@Composable
private fun NoticeBlock(noticeText: String) {
    val spacing = TithimalaTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Text(
            text = stringResource(R.string.settings_licenses_notice_heading),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(
                width = spacing.hairline,
                color = TithimalaTheme.colors.hairline
            )
        ) {
            Text(
                text = noticeText.trim(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(spacing.sm)
            )
        }
    }
}

@Composable
private fun FontCreditsBlock() {
    val spacing = TithimalaTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Text(
            text = stringResource(R.string.settings_licenses_fonts_heading),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        FontCreditItem(
            title = stringResource(R.string.settings_licenses_font_mukta_title),
            detail = stringResource(R.string.settings_licenses_font_mukta_detail)
        )
        FontCreditItem(
            title = stringResource(R.string.settings_licenses_font_noto_title),
            detail = stringResource(R.string.settings_licenses_font_noto_detail)
        )
    }
}

@Composable
private fun FontCreditItem(title: String, detail: String) {
    val spacing = TithimalaTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.micro)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LicenseSubsection(heading: String, body: String) {
    val spacing = TithimalaTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.micro)
    ) {
        Text(
            text = heading,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
