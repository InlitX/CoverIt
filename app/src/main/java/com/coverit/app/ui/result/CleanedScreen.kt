package com.coverit.app.ui.result

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.Files
import com.adamglin.phosphoricons.regular.ShareFat
import com.adamglin.phosphoricons.regular.Warning
import com.coverit.app.R
import com.coverit.app.ui.AppActions
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.Route
import com.coverit.app.ui.common.icon
import com.coverit.app.ui.components.AnimatedSeal
import com.coverit.app.ui.components.BottomEdgeBlur
import com.coverit.app.ui.components.PrimaryButton
import com.coverit.app.ui.components.Reveal
import com.coverit.app.ui.components.RowDivider
import com.coverit.app.ui.components.SecondaryButton
import com.coverit.app.ui.components.TopBar
import com.coverit.app.ui.components.card
import com.coverit.app.ui.components.pressable

@Composable
fun CleanedScreen(vm: MainViewModel, route: Route.Cleaned, actions: AppActions) {
    val records = route.items.mapNotNull { item -> vm.library.find(item.recordId)?.let { it to item } }
    val clean = route.failed == 0 && records.all { it.second.leftovers.isEmpty() }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(84.dp))
            AnimatedSeal(clean)
            Spacer(Modifier.height(24.dp))
            Reveal(1) {
                Text(
                    pluralStringResource(R.plurals.cleaned_title, records.size, records.size),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Reveal(2) {
                Text(stringResource(R.string.cleaned_sub), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(28.dp))
            Reveal(3) {
                Column(Modifier.fillMaxWidth().card()) {
                    records.forEachIndexed { i, (record, item) ->
                        if (i > 0) RowDivider(52.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .pressable(scale = 0.98f) { vm.push(Route.Detail(record.id)) }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val warn = item.leftovers.isNotEmpty()
                            Icon(
                                if (warn) PhosphorIcons.Regular.Warning else record.type.icon,
                                null,
                                Modifier.size(20.dp),
                                tint = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(record.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    when {
                                        warn -> stringResource(R.string.result_meta_bad) + ": " + item.leftovers.joinToString(", ")
                                        item.removed > 0 -> pluralStringResource(R.plurals.meta_removed_n, item.removed, item.removed)
                                        else -> stringResource(R.string.cleaned_nothing)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(PhosphorIcons.Regular.CaretRight, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (route.failed > 0) {
                Spacer(Modifier.height(14.dp))
                Text(
                    pluralStringResource(R.plurals.cleaned_failed, route.failed, route.failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Spacer(Modifier.height(190.dp))
        }

        TopBar(onBack = { vm.home() }, backLabel = stringResource(R.string.back))

        BottomEdgeBlur(height = 180.dp)
        Column(
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryButton(stringResource(R.string.share_all), Modifier.fillMaxWidth(), PhosphorIcons.Regular.ShareFat) { actions.shareMany(records.map { it.first }) }
            SecondaryButton(stringResource(R.string.done), Modifier.fillMaxWidth(), PhosphorIcons.Regular.Files) { vm.home() }
        }
    }
}
