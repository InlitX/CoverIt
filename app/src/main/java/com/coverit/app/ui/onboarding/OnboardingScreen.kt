package com.coverit.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Archive
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.HandHeart
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.regular.WifiSlash
import com.coverit.app.R
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.components.AppMark
import com.coverit.app.ui.components.ButtonShape
import com.coverit.app.ui.components.CoverSwitch
import com.coverit.app.ui.components.PrimaryButton
import com.coverit.app.ui.components.Reveal
import com.coverit.app.ui.components.RowDivider
import com.coverit.app.ui.components.SectionLabel
import com.coverit.app.ui.components.SettingRow
import com.coverit.app.ui.components.card
import com.coverit.app.ui.components.hairline
import com.coverit.app.ui.components.lift
import com.coverit.app.ui.components.pressable
import com.coverit.app.ui.settings.StylePicker

private const val LAST = 1

@Composable
fun OnboardingScreen(vm: MainViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val finish = { vm.finishOnboarding() }
    val background = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(340)) { if (forward) it / 6 else -it / 6 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { if (forward) -it / 6 else it / 6 } + fadeOut(tween(160)))
            },
            modifier = Modifier.fillMaxSize(),
            label = "onboarding",
        ) { s ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 64.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                when (s) {
                    0 -> Welcome()
                    else -> Preferences(vm)
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(background).padding(start = 24.dp, end = 12.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0..LAST) {
                    val color by animateColorAsState(
                        if (i <= step) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant,
                        tween(280, easing = FastOutSlowInEasing),
                        label = "progress",
                    )
                    Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(color))
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.skip),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = finish).padding(horizontal = 10.dp, vertical = 12.dp),
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.verticalGradient(listOf(background.copy(alpha = 0f), background.copy(alpha = 0.92f), background))),
        )
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(step > 0, enter = fadeIn(tween(200)) + scaleIn(tween(240), initialScale = 0.6f), exit = fadeOut(tween(140)) + scaleOut(tween(180), targetScale = 0.6f)) {
                Box(
                    Modifier
                        .padding(end = 12.dp)
                        .size(56.dp)
                        .pressable(scale = 0.9f) { step-- }
                        .lift(CircleShape, 16.dp)
                        .hairline(CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(PhosphorIcons.Regular.ArrowLeft, stringResource(R.string.back), Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            PrimaryButton(
                if (step == LAST) stringResource(R.string.start) else stringResource(R.string.next),
                Modifier.weight(1f).lift(ButtonShape, 20.dp),
            ) { if (step == LAST) finish() else step++ }
        }
    }
}

@Composable
private fun Welcome() {
    Reveal(0) { AppMark(Modifier.padding(start = 4.dp)) }
    Spacer(Modifier.height(30.dp))
    Reveal(1) { SectionLabel(stringResource(R.string.onb_kicker)) }
    Reveal(2) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
    Spacer(Modifier.height(12.dp))
    Reveal(3) {
        Text(
            stringResource(R.string.onb_blurb),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
    Spacer(Modifier.height(24.dp))
    Reveal(4) {
        Column(Modifier.fillMaxWidth().card()) {
            Promise(PhosphorIcons.Regular.WifiSlash, R.string.onb_offline, R.string.onb_offline_why)
            RowDivider(52.dp)
            Promise(PhosphorIcons.Regular.ShieldCheck, R.string.onb_real, R.string.onb_real_why)
            RowDivider(52.dp)
            Promise(PhosphorIcons.Regular.HandHeart, R.string.onb_free, R.string.onb_free_why)
        }
    }
}

@Composable
private fun Promise(icon: ImageVector, title: Int, why: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Icon(icon, null, Modifier.padding(top = 1.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(why), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Preferences(vm: MainViewModel) {
    Reveal(0) { SectionLabel(stringResource(R.string.onb_prefs_kicker)) }
    Reveal(1) {
        Text(
            stringResource(R.string.onb_style_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
    Spacer(Modifier.height(10.dp))
    Reveal(2) {
        Text(
            stringResource(R.string.onb_style_why),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
    Spacer(Modifier.height(24.dp))
    Reveal(3) { StylePicker(vm.prefs.style) { vm.prefs.updateStyle(it) } }
    Spacer(Modifier.height(12.dp))
    Reveal(4) {
        Column(Modifier.fillMaxWidth().card()) {
            SettingRow(PhosphorIcons.Regular.Archive, stringResource(R.string.settings_keep_original), stringResource(R.string.settings_keep_original_sub)) {
                CoverSwitch(vm.prefs.keepOriginal) { vm.prefs.updateKeepOriginal(it) }
            }
        }
    }
}
