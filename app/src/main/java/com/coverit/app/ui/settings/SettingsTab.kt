package com.coverit.app.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Archive
import com.adamglin.phosphoricons.regular.ArrowCounterClockwise
import com.adamglin.phosphoricons.regular.Bug
import com.adamglin.phosphoricons.regular.CircleHalf
import com.adamglin.phosphoricons.regular.Coffee
import com.adamglin.phosphoricons.regular.GithubLogo
import com.adamglin.phosphoricons.regular.Info
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.Scales
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.regular.Translate
import com.adamglin.phosphoricons.regular.Trash
import com.coverit.app.R
import com.coverit.app.data.ThemeMode
import com.coverit.app.detect.Kind
import com.coverit.app.redact.Style
import com.coverit.app.ui.AppActions
import com.coverit.app.ui.AppLanguage
import com.coverit.app.ui.LocalAppLanguage
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.Route
import com.coverit.app.ui.common.hint
import com.coverit.app.ui.common.icon
import com.coverit.app.ui.common.label
import com.coverit.app.ui.components.CoverCheckbox
import com.coverit.app.ui.components.CoverDialog
import com.coverit.app.ui.components.CoverSwitch
import com.coverit.app.ui.components.IconBadge
import com.coverit.app.ui.components.NavRow
import com.coverit.app.ui.components.RowDivider
import com.coverit.app.ui.components.SectionLabel
import com.coverit.app.ui.components.Segmented
import com.coverit.app.ui.components.SettingRow
import com.coverit.app.ui.components.StylePreview
import com.coverit.app.ui.components.card
import com.coverit.app.ui.home.pagePadding

const val SOURCE_URL = "https://github.com/InlitX/CoverIt"
const val ISSUES_URL = "$SOURCE_URL/issues"
const val LICENSE_URL = "$SOURCE_URL/blob/main/LICENSE"

@Composable
fun SettingsTab(vm: MainViewModel, actions: AppActions) {
    val prefs = vm.prefs
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    var wipe by remember { mutableStateOf(false) }
    val language = LocalAppLanguage.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pagePadding())) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(24.dp))

        SectionLabel(stringResource(R.string.settings_appearance))
        Column(Modifier.fillMaxWidth().card().padding(bottom = 16.dp)) {
            SettingRow(PhosphorIcons.Regular.CircleHalf, stringResource(R.string.settings_theme))
            Segmented(
                options = listOf(stringResource(R.string.theme_system), stringResource(R.string.theme_light), stringResource(R.string.theme_dark)),
                index = prefs.theme.ordinal,
                onChange = { prefs.updateTheme(ThemeMode.entries[it]) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(6.dp))
            SettingRow(PhosphorIcons.Regular.Translate, stringResource(R.string.settings_language))
            Segmented(
                options = listOf(stringResource(R.string.language_system), "English", "Español"),
                index = language.ordinal,
                onChange = { if (it != language.ordinal) actions.setLanguage(AppLanguage.entries[it]) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
        Spacer(Modifier.height(24.dp))

        SectionLabel(stringResource(R.string.settings_default_style))
        Column(Modifier.fillMaxWidth().card().padding(16.dp)) {
            Segmented(
                options = Style.entries.map { stringResource(it.label) },
                index = prefs.style.ordinal,
                onChange = { prefs.updateStyle(Style.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            StylePreview(prefs.style, tag = stringResource(R.string.tag_name))
            Text(stringResource(prefs.style.hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(24.dp))

        SectionLabel(stringResource(R.string.settings_detection))
        Column(Modifier.fillMaxWidth().card()) {
            NavRow(
                PhosphorIcons.Regular.MagnifyingGlass,
                stringResource(R.string.settings_detection),
                stringResource(R.string.settings_detection_sub),
                value = "${prefs.enabledKinds.size}/${Kind.detectable.size}",
            ) { vm.push(Route.Detection) }
        }
        Spacer(Modifier.height(24.dp))

        SectionLabel(stringResource(R.string.settings_documents))
        Column(Modifier.fillMaxWidth().card()) {
            SettingRow(PhosphorIcons.Regular.Archive, stringResource(R.string.settings_keep_original), stringResource(R.string.settings_keep_original_sub)) {
                CoverSwitch(prefs.keepOriginal) { prefs.updateKeepOriginal(it) }
            }
            RowDivider()
            SettingRow(
                PhosphorIcons.Regular.Trash,
                stringResource(R.string.settings_wipe),
                stringResource(R.string.settings_wipe_sub),
                tint = MaterialTheme.colorScheme.error,
                onClick = { wipe = true },
            )
        }
        Spacer(Modifier.height(24.dp))

        SectionLabel(stringResource(R.string.settings_about))
        Column(Modifier.fillMaxWidth().card()) {
            NavRow(PhosphorIcons.Regular.Info, stringResource(R.string.settings_about_app), "v$version") { vm.push(Route.About) }
            RowDivider()
            NavRow(PhosphorIcons.Regular.ShieldCheck, stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_sub)) { vm.push(Route.Privacy) }
            RowDivider()
            NavRow(PhosphorIcons.Regular.ArrowCounterClockwise, stringResource(R.string.settings_intro)) { vm.replayOnboarding() }
            RowDivider()
            NavRow(PhosphorIcons.Regular.GithubLogo, stringResource(R.string.settings_source), "github.com/InlitX/CoverIt") { actions.openUrl(SOURCE_URL) }
            RowDivider()
            NavRow(PhosphorIcons.Regular.Bug, stringResource(R.string.settings_feedback), stringResource(R.string.settings_feedback_sub)) { actions.openUrl(ISSUES_URL) }
            RowDivider()
            NavRow(PhosphorIcons.Regular.Scales, stringResource(R.string.settings_license), "GPL-3.0") { actions.openUrl(LICENSE_URL) }
        }
        Spacer(Modifier.height(28.dp))

        Column(Modifier.fillMaxWidth().card()) {
            NavRow(PhosphorIcons.Regular.Coffee, stringResource(R.string.about_coffee), stringResource(R.string.settings_coffee_title)) { actions.openUrl(COFFEE_URL) }
        }
    }

    if (wipe) {
        CoverDialog(
            title = stringResource(R.string.settings_wipe_title),
            body = stringResource(R.string.settings_wipe_body),
            confirm = stringResource(R.string.delete),
            dismiss = stringResource(R.string.cancel),
            danger = true,
            onConfirm = {
                vm.wipe()
                wipe = false
            },
            onDismiss = { wipe = false },
        )
    }
}

@Composable
fun StylePicker(selected: Style, onSelect: (Style) -> Unit) {
    Column(Modifier.fillMaxWidth().card()) {
        Style.entries.forEachIndexed { i, style ->
            if (i > 0) RowDivider(16.dp)
            val active = style == selected
            val edge by animateColorAsState(
                if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
                tween(180),
                label = "styleEdge",
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(remember { MutableInteractionSource() }, null) { onSelect(style) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                IconBadge(style.icon)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(style.label), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(style.hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, edge, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                    ) {
                        StylePreview(style, tag = stringResource(R.string.tag_name))
                    }
                }
                Spacer(Modifier.width(12.dp))
                CoverCheckbox(active) { if (it) onSelect(style) }
            }
        }
    }
}
