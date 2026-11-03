package com.coverit.app.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Hash
import com.adamglin.phosphoricons.regular.Lasso
import com.adamglin.phosphoricons.regular.X
import com.coverit.app.R
import com.coverit.app.detect.Kind
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.common.hint
import com.coverit.app.ui.common.icon
import com.coverit.app.ui.common.label
import com.coverit.app.ui.components.CoverSwitch
import com.coverit.app.ui.components.RowDivider
import com.coverit.app.ui.components.SectionLabel
import com.coverit.app.ui.components.SettingRow
import com.coverit.app.ui.components.TopBar
import com.coverit.app.ui.components.card
import com.coverit.app.ui.components.pressable

@Composable
fun DetectionScreen(vm: MainViewModel) {
    val density = LocalDensity.current
    val top = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val bottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = top + 74.dp, bottom = bottom + 24.dp),
        ) {
            Text(stringResource(R.string.settings_detection), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.detection_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.detection_own))
            Column(Modifier.fillMaxWidth().card()) {
                if (vm.prefs.rules.isEmpty()) {
                    SettingRow(PhosphorIcons.Regular.Lasso, stringResource(R.string.detection_own_empty), stringResource(R.string.detection_own_empty_sub))
                }
                vm.prefs.rules.forEachIndexed { i, rule ->
                    if (i > 0) RowDivider()
                    SettingRow(PhosphorIcons.Regular.Hash, rule.label) {
                        Box(Modifier.size(36.dp).pressable(scale = 0.85f) { vm.prefs.removeRule(rule) }, contentAlignment = Alignment.Center) {
                            Icon(PhosphorIcons.Regular.X, stringResource(R.string.delete), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.detection_builtin))
            KindList(vm, Kind.detectable.filter { it !in Kind.spain })
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.detection_spain))
            KindList(vm, Kind.detectable.filter { it in Kind.spain })
        }
        TopBar(onBack = { vm.back() }, backLabel = stringResource(R.string.back))
    }
}

@Composable
private fun KindList(vm: MainViewModel, kinds: List<Kind>) {
    Column(Modifier.fillMaxWidth().card()) {
        kinds.forEachIndexed { i, kind ->
            if (i > 0) RowDivider()
            SettingRow(kind.icon, stringResource(kind.label), stringResource(kind.hint)) {
                CoverSwitch(kind !in vm.prefs.disabled) { vm.prefs.toggleKind(kind, it) }
            }
        }
    }
}
