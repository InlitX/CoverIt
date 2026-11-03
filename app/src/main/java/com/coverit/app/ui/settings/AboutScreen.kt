package com.coverit.app.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.Code
import com.adamglin.phosphoricons.regular.Coffee
import com.adamglin.phosphoricons.regular.FileDoc
import com.adamglin.phosphoricons.regular.FilePdf
import com.adamglin.phosphoricons.regular.GithubLogo
import com.adamglin.phosphoricons.regular.LockSimple
import com.adamglin.phosphoricons.regular.SealCheck
import com.adamglin.phosphoricons.regular.Star
import com.adamglin.phosphoricons.regular.Warning
import com.adamglin.phosphoricons.regular.WifiSlash
import com.coverit.app.R
import com.coverit.app.ui.AppActions
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.components.AppMark
import com.coverit.app.ui.components.Entrance
import com.coverit.app.ui.components.IconBadge
import com.coverit.app.ui.components.RowDivider
import com.coverit.app.ui.components.SecondaryButton
import com.coverit.app.ui.components.SectionLabel
import com.coverit.app.ui.components.TopBar
import com.coverit.app.ui.components.card
import com.coverit.app.ui.components.pressable
import kotlinx.coroutines.delay

const val PROFILE_URL = "https://github.com/InlitX"
const val COFFEE_URL = "https://ko-fi.com/inlitx"

@Composable
fun AboutScreen(vm: MainViewModel, actions: AppActions) {
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val story = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(96.dp))
            Entrance(0, Modifier.align(Alignment.CenterHorizontally)) {
                val grow = remember { Animatable(0.88f) }
                LaunchedEffect(Unit) { grow.animateTo(1f, tween(620, easing = EaseOutBack)) }
                AppMark(
                    Modifier.graphicsLayer {
                        scaleX = grow.value
                        scaleY = grow.value
                    },
                    size = 76.dp,
                )
            }
            Spacer(Modifier.height(24.dp))
            Entrance(1) {
                Typewriter(
                    stringResource(R.string.app_name),
                    MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-1).sp, color = MaterialTheme.colorScheme.onBackground),
                    durationMs = 640,
                    delayMs = 120,
                )
            }
            Spacer(Modifier.height(10.dp))
            Entrance(2) {
                Typewriter(
                    stringResource(R.string.about_subtitle),
                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium, lineHeight = 24.sp, color = muted),
                    durationMs = 1000,
                    delayMs = 380,
                )
            }
            Spacer(Modifier.height(30.dp))
            Entrance(3) {
                Typewriter(
                    stringResource(R.string.about_story),
                    MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 25.sp, color = story),
                    durationMs = 2200,
                    delayMs = 1400,
                )
            }
            Spacer(Modifier.height(32.dp))
            Entrance(4) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("GitHub", Modifier.fillMaxWidth(), PhosphorIcons.Regular.Star) { actions.openUrl(SOURCE_URL) }
                    SecondaryButton(stringResource(R.string.about_coffee), Modifier.fillMaxWidth(), PhosphorIcons.Regular.Coffee) { actions.openUrl(COFFEE_URL) }
                }
            }
            Spacer(Modifier.height(40.dp))
            Entrance(5, Modifier.align(Alignment.CenterHorizontally)) {
                MadeBy { actions.openUrl(PROFILE_URL) }
            }
            Spacer(Modifier.height(4.dp))
            Entrance(5, Modifier.align(Alignment.CenterHorizontally)) {
                Text(
                    stringResource(R.string.about_more_apps),
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                    modifier = Modifier
                        .clip(CircleShape)
                        .pressable(scale = 0.95f) { actions.openUrl(PROFILE_URL) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Entrance(5, Modifier.align(Alignment.CenterHorizontally)) {
                Text("v$version", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp), color = muted.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(14.dp))
            Entrance(5) {
                Text(
                    stringResource(R.string.about_legal),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 17.sp),
                    color = muted.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(40.dp))
        }
        TopBar(onBack = { vm.back() }, backLabel = stringResource(R.string.back))
    }
}

@Composable
private fun MadeBy(onClick: () -> Unit) {
    val handle = stringResource(R.string.dev_handle)
    val text = stringResource(R.string.made_by, handle)
    val start = text.indexOf(handle)
    val accent = MaterialTheme.colorScheme.onBackground
    Text(
        buildAnnotatedString {
            if (start < 0) {
                append(text)
            } else {
                append(text.substring(0, start))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accent)) { append(handle) }
                append(text.substring(start + handle.length))
            }
        },
        style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(CircleShape)
            .pressable(scale = 0.96f, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun Typewriter(text: String, style: TextStyle, durationMs: Int, delayMs: Int, modifier: Modifier = Modifier) {
    val progress = remember(text) { Animatable(0f) }
    LaunchedEffect(text) {
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
    }
    val color = style.color
    val typed = text.length * progress.value
    val full = typed.toInt().coerceIn(0, text.length)
    Text(
        buildAnnotatedString {
            append(text.substring(0, full))
            if (full < text.length) {
                withStyle(SpanStyle(color = color.copy(alpha = color.alpha * (typed - full)))) { append(text[full]) }
                if (full + 1 < text.length) withStyle(SpanStyle(color = Color.Transparent)) { append(text.substring(full + 1)) }
            }
        },
        style = style,
        modifier = modifier,
    )
}

private class PrivacyTopic(val icon: ImageVector, val title: Int, val body: Int)

@Composable
fun PrivacyScreen(vm: MainViewModel, actions: AppActions) {
    val topics = listOf(
        PrivacyTopic(PhosphorIcons.Regular.WifiSlash, R.string.privacy_offline, R.string.letter_2),
        PrivacyTopic(PhosphorIcons.Regular.FilePdf, R.string.privacy_pdf, R.string.letter_3),
        PrivacyTopic(PhosphorIcons.Regular.FileDoc, R.string.privacy_office, R.string.letter_4),
        PrivacyTopic(PhosphorIcons.Regular.SealCheck, R.string.privacy_check, R.string.letter_5),
        PrivacyTopic(PhosphorIcons.Regular.LockSimple, R.string.privacy_storage, R.string.letter_6),
        PrivacyTopic(PhosphorIcons.Regular.Warning, R.string.privacy_limits, R.string.letter_7),
        PrivacyTopic(PhosphorIcons.Regular.Code, R.string.privacy_open, R.string.letter_8),
    )
    var open by remember { mutableStateOf<Int?>(null) }
    val body = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 24.sp)
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(84.dp))
            Entrance(0) {
                Text(stringResource(R.string.letter_title), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.height(6.dp))
            Entrance(1) {
                Text(stringResource(R.string.letter_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
            Entrance(2) {
                Column(Modifier.fillMaxWidth().card().padding(horizontal = 20.dp, vertical = 22.dp)) {
                    Text(stringResource(R.string.letter_greeting), style = body.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.letter_1), style = body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f))
                }
            }
            Spacer(Modifier.height(26.dp))
            Entrance(3) {
                Column {
                    SectionLabel(stringResource(R.string.privacy_how))
                    Column(Modifier.fillMaxWidth().card()) {
                        topics.forEachIndexed { i, topic ->
                            if (i > 0) RowDivider()
                            Topic(topic, open == i) { open = if (open == i) null else i }
                        }
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
            Entrance(4, Modifier.align(Alignment.CenterHorizontally)) {
                Text(
                    stringResource(R.string.letter_closing),
                    style = body,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(10.dp))
            Entrance(4, Modifier.align(Alignment.CenterHorizontally)) {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .pressable(scale = 0.95f) { actions.openUrl(PROFILE_URL) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(PhosphorIcons.Regular.GithubLogo, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.dev_handle),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            Spacer(Modifier.height(48.dp))
        }
        TopBar(onBack = { vm.back() }, backLabel = stringResource(R.string.back))
    }
}

@Composable
private fun Topic(topic: PrivacyTopic, expanded: Boolean, onToggle: () -> Unit) {
    val text = stringResource(topic.body)
    val cut = Regex("""[.!?]\s""").find(text)?.range?.first?.plus(1) ?: text.length
    val turn by animateFloatAsState(if (expanded) 180f else 0f, tween(260), label = "topicCaret")
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null, onClick = onToggle)
            .animateContentSize(tween(260))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(topic.icon)
            Spacer(Modifier.width(14.dp))
            Text(stringResource(topic.title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(
                PhosphorIcons.Regular.CaretDown,
                null,
                Modifier.size(16.dp).graphicsLayer { rotationZ = turn },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (expanded) text else text.substring(0, cut),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 48.dp),
        )
    }
}
