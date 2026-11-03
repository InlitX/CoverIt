package com.coverit.app.ui.result

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Broom
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.DownloadSimple
import com.adamglin.phosphoricons.regular.Files
import com.adamglin.phosphoricons.regular.FolderSimple
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.ShareFat
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.regular.Warning
import com.coverit.app.R
import com.coverit.app.doc.Family
import com.coverit.app.doc.MetaField
import com.coverit.app.doc.MetaReader
import com.coverit.app.ui.AppActions
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.Route
import com.coverit.app.ui.common.HiddenDataSheet
import com.coverit.app.ui.common.MoveSheet
import com.coverit.app.ui.common.icon
import com.coverit.app.ui.common.label
import com.coverit.app.ui.common.spaceName
import com.coverit.app.ui.components.AnimatedSeal
import com.coverit.app.ui.components.BottomEdgeBlur
import com.coverit.app.ui.components.PrimaryButton
import com.coverit.app.ui.components.Reveal
import com.coverit.app.ui.components.RowDivider
import com.coverit.app.ui.components.SecondaryButton
import com.coverit.app.ui.components.SectionLabel
import com.coverit.app.ui.components.TopBar
import com.coverit.app.ui.components.card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ResultScreen(vm: MainViewModel, route: Route.Result, actions: AppActions) {
    val record = vm.library.find(route.recordId) ?: return
    val report = route.report
    var moving by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    val clean = vm.library.clean(record)
    val after by produceState<List<MetaField>?>(null, clean, clean.lastModified()) {
        value = withContext(Dispatchers.IO) { MetaReader.read(clean, record.type) }
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(84.dp))
            AnimatedSeal(report.clean)
            Spacer(Modifier.height(24.dp))
            Reveal(1) {
                Text(
                    stringResource(
                        when {
                            !report.clean -> R.string.result_review
                            route.hiddenOnly -> R.string.result_hidden_clean
                            else -> R.string.result_clean
                        },
                    ),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Reveal(2) {
                Text(record.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(28.dp))
            Reveal(3) {
                Column(Modifier.fillMaxWidth().card()) {
                    if (!route.hiddenOnly) {
                        if (report.leaked.isEmpty()) {
                            Check(PhosphorIcons.Regular.ShieldCheck, stringResource(R.string.result_text_ok), stringResource(R.string.result_text_ok_body))
                        } else {
                            Check(PhosphorIcons.Regular.Warning, stringResource(R.string.result_text_bad), report.leaked.joinToString(", "), warn = true)
                        }
                        RowDivider(52.dp)
                    }
                    val removed = report.before.size
                    if (report.leftovers.isEmpty()) {
                        Check(
                            PhosphorIcons.Regular.Broom,
                            stringResource(R.string.result_meta_ok),
                            (if (removed > 0) pluralStringResource(R.plurals.meta_removed_n, removed, removed) + " " else "") + stringResource(
                                when (report.type.family) {
                                    Family.PDF -> R.string.result_meta_ok_pdf
                                    Family.IMAGE -> R.string.result_meta_ok_image
                                    Family.OFFICE -> R.string.result_meta_ok_office
                                },
                            ),
                            action = stringResource(R.string.result_see),
                        ) { hidden = true }
                    } else {
                        Check(PhosphorIcons.Regular.Warning, stringResource(R.string.result_meta_bad), report.leftovers.joinToString(", "), warn = true, action = stringResource(R.string.result_see)) { hidden = true }
                    }
                    RowDivider(52.dp)
                    Check(
                        PhosphorIcons.Regular.FolderSimple,
                        stringResource(R.string.result_saved_hint),
                        spaceName(vm.library, record.space) + " · " + (vm.library.folder(record.folder)?.name ?: stringResource(R.string.docs_no_folder)),
                        action = stringResource(R.string.result_change),
                    ) { moving = true }
                }
            }
            if (report.remaining.isNotEmpty()) {
                Spacer(Modifier.height(26.dp))
                Reveal(4) { SectionLabel(stringResource(R.string.result_left_visible)) }
                Reveal(5) {
                    Column(Modifier.fillMaxWidth().card().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        report.remaining.forEach { (kind, value) ->
                            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(kind.icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(10.dp))
                                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(stringResource(kind.label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (record.hasOriginal && (report.remaining.isNotEmpty() || report.leaked.isNotEmpty())) {
                Spacer(Modifier.height(14.dp))
                SecondaryButton(stringResource(R.string.docs_edit), Modifier.fillMaxWidth(), PhosphorIcons.Regular.PencilSimple) { vm.reopen(record) }
            }
            Spacer(Modifier.height(190.dp))
        }

        TopBar(onBack = { vm.home() }, backLabel = stringResource(R.string.back))

        BottomEdgeBlur(height = 180.dp)
        Column(
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryButton(stringResource(R.string.share), Modifier.fillMaxWidth(), PhosphorIcons.Regular.ShareFat) { actions.share(record, false) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(R.string.save), Modifier.weight(1f), PhosphorIcons.Regular.DownloadSimple) { actions.save(record) }
                SecondaryButton(stringResource(R.string.done), Modifier.weight(1f), PhosphorIcons.Regular.Files) { vm.home() }
            }
        }
    }

    if (moving) MoveSheet(vm, record) { moving = false }
    if (hidden) HiddenDataSheet(report.before, after.orEmpty()) { hidden = false }
}

@Composable
fun Check(icon: ImageVector, title: String, detail: String, warn: Boolean = false, action: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.align(Alignment.Top).padding(top = 1.dp).size(20.dp), tint = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null && action != null) {
            Spacer(Modifier.width(10.dp))
            Text(action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Icon(PhosphorIcons.Regular.CaretRight, null, Modifier.padding(start = 2.dp).size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
