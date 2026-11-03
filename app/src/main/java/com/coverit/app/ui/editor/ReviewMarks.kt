package com.coverit.app.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.CaretLeft
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.X
import com.coverit.app.R
import com.coverit.app.doc.Mark
import com.coverit.app.doc.Session
import com.coverit.app.ui.common.icon
import com.coverit.app.ui.common.label
import com.coverit.app.ui.components.AnimatedSeal
import com.coverit.app.ui.components.GlassIconButton
import com.coverit.app.ui.components.PrimaryButton
import com.coverit.app.ui.components.SecondaryButton
import com.coverit.app.ui.components.card
import com.coverit.app.ui.components.pressable
import kotlinx.coroutines.delay

@Composable
fun ReviewMarks(
    session: Session,
    render: suspend (Int, Int) -> Bitmap?,
    onSelect: (Mark) -> Unit,
    onClose: () -> Unit,
) {
    val order = remember { session.marks.sortedWith(compareBy({ it.page }, { it.boxes.firstOrNull()?.top ?: 0f })) }
    var index by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Grip?>(null) }
    var flash by remember { mutableStateOf<Flash?>(null) }
    var view by remember { mutableStateOf(IntSize.Zero) }
    var content by remember { mutableStateOf(IntSize.Zero) }
    val haptics = LocalHapticFeedback.current
    BackHandler(onBack = onClose)

    val live = order.filter { it in session.marks }
    val mark = live.getOrNull(index)
    LaunchedEffect(mark) {
        editing = null
        if (mark != null) {
            delay(240)
            flash = Flash(mark, System.nanoTime())
        }
    }

    fun decide(cover: Boolean) {
        val m = mark ?: return
        session.toggle(m, cover)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        index++
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(PhosphorIcons.Regular.X, stringResource(R.string.close), onClick = onClose)
            Text(
                if (mark != null) stringResource(R.string.review_of, index + 1, live.size) else stringResource(R.string.review_done_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
            )
            HistoryButtons(session) { editing = null }
        }
        val progress by animateFloatAsState(if (live.isEmpty()) 1f else index.toFloat() / live.size, tween(300), label = "reviewProgress")
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(progress).height(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }

        if (mark == null) {
            Done(live, Modifier.weight(1f), onClose) { index = 0 }
            return@Column
        }

        val page = session.source.pages[mark.page]
        val bounds = mark.boxes.reduce { a, b -> a.union(b) }
        val pageScale = if (content.width > 0) content.width / page.width else 1f
        val wantZoom = if (view.width == 0) 1f else minOf(
            view.width * 0.62f / ((bounds.right - bounds.left) * pageScale).coerceAtLeast(1f),
            view.height * 0.4f / ((bounds.bottom - bounds.top) * pageScale).coerceAtLeast(1f),
        ).coerceIn(1.2f, 3.5f)
        val centre = Offset((bounds.left + bounds.right) / 2 * pageScale, (bounds.top + bounds.bottom) / 2 * pageScale) - Offset(content.width / 2f, content.height / 2f)
        val maxX = (content.width * wantZoom - view.width).coerceAtLeast(0f) / 2
        val maxY = (content.height * wantZoom - view.height).coerceAtLeast(0f) / 2
        val wantPan = Offset((-centre.x * wantZoom).coerceIn(-maxX, maxX), (-centre.y * wantZoom).coerceIn(-maxY, maxY))
        val zoom by animateFloatAsState(wantZoom, tween(420, easing = FastOutSlowInEasing), label = "reviewZoom")
        val pan by animateOffsetAsState(wantPan, tween(420, easing = FastOutSlowInEasing), label = "reviewPan")

        Box(
            Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp).clipToBounds().onSizeChanged { view = it },
            contentAlignment = Alignment.Center,
        ) {
            PageCanvas(
                page = page,
                marks = session.marks.filter { it.page == mark.page },
                style = session.style,
                drawing = false,
                editing = editing?.takeIf { it.mark in session.marks },
                flash = flash?.takeIf { it.mark.page == mark.page },
                render = render,
                quality = 2f,
                actions = PageActions(
                    onEdit = { editing = it },
                    onCover = { session.toggle(it, true) },
                    onReshapeStart = { session.beginReshape() },
                    onReshape = { m, i, box -> session.reshape(m, i, box) },
                    onUncover = {
                        editing = null
                        session.uncover(it)
                    },
                    onMore = {
                        editing = null
                        onSelect(it)
                    },
                    onLasso = {},
                    onArea = {},
                    onScroll = { 0f },
                    onHold = { _, _ -> },
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .onSizeChanged { content = it }
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = pan.x
                        translationY = pan.y
                        transformOrigin = TransformOrigin.Center
                    },
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            AnimatedContent(
                targetState = mark,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 6 }) togetherWith
                        (fadeOut(tween(140)) + slideOutHorizontally(tween(200)) { -it / 6 })
                },
                label = "reviewItem",
            ) { m ->
                Row(Modifier.fillMaxWidth().card().padding(vertical = 12.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StepButton(PhosphorIcons.Regular.CaretLeft, stringResource(R.string.review_prev), index > 0) { index-- }
                    Icon(m.kind.icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            m.value.ifBlank { stringResource(m.kind.label) },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            stringResource(m.kind.label) + " · " + stringResource(R.string.editor_page_short, m.page + 1) + " · " +
                                stringResource(if (m.checked) R.string.editor_will_cover else R.string.editor_stays),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    StepButton(PhosphorIcons.Regular.CaretRight, stringResource(R.string.review_next), true) { index++ }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(stringResource(R.string.review_leave), Modifier.weight(1f)) { decide(false) }
                PrimaryButton(stringResource(R.string.review_cover), Modifier.weight(1f)) { decide(true) }
            }
        }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.3f }
            .pressable(enabled = enabled, scale = 0.88f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Done(marks: List<Mark>, modifier: Modifier, onClose: () -> Unit, onAgain: () -> Unit) {
    val covered = marks.count { it.checked }
    Column(
        modifier.fillMaxWidth().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedSeal(success = true)
        Spacer(Modifier.height(22.dp))
        Text(stringResource(R.string.review_done_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.review_done_body, covered, marks.size - covered),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        PrimaryButton(stringResource(R.string.review_back), Modifier.fillMaxWidth(), onClick = onClose)
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.review_again),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.pressable(scale = 0.95f, onClick = onAgain).padding(12.dp),
        )
    }
}
