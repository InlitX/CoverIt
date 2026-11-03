package com.coverit.app.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowClockwise
import com.adamglin.phosphoricons.regular.ArrowCounterClockwise
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.X
import com.coverit.app.R
import com.coverit.app.data.ThemeMode
import com.coverit.app.doc.Framing
import com.coverit.app.doc.ImageSource
import com.coverit.app.pdf.Box as PageBox
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.components.GlassIconButton
import com.coverit.app.ui.components.PrimaryButton
import com.coverit.app.ui.components.pressable
import com.coverit.app.ui.result.Check
import com.coverit.app.ui.theme.CoverItTheme
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Handle { MOVE, LEFT, TOP, RIGHT, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

private const val MIN_SIDE = 0.08f

@Composable
fun AdjustPhoto(vm: MainViewModel, photo: ImageSource, onClose: () -> Unit) {
    CoverItTheme(ThemeMode.DARK) { AdjustContent(vm, photo, onClose) }
}

@Composable
private fun AdjustContent(vm: MainViewModel, photo: ImageSource, onClose: () -> Unit) {
    val start = remember { photo.framing }
    val startCrop = remember {
        start.crop?.let { c ->
            val (w, h) = photo.turnedSize(start.turns)
            Rect(c.left / w, c.top / h, c.right / w, c.bottom / h)
        } ?: Rect(0f, 0f, 1f, 1f)
    }
    var turns by remember { mutableIntStateOf(start.turns % 4) }
    var crop by remember { mutableStateOf(startCrop) }
    var busy by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val spin = remember { Animatable(0f) }
    val zoom = remember { Animatable(1f) }
    var turning by remember { mutableStateOf(false) }
    val frameAlpha by animateFloatAsState(if (turning) 0f else 1f, tween(140), label = "frame")
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) { preview = withContext(Dispatchers.Default) { photo.preview(turns) } }
    var area by remember { mutableStateOf(Size.Zero) }
    var controls by remember { mutableIntStateOf(0) }
    val changed = turns != start.turns % 4 || crop != startCrop
    val grid by animateFloatAsState(if (dragging) 1f else 0f, tween(180), label = "grid")
    BackHandler(onBack = onClose)

    fun turn(clockwise: Boolean) {
        val shown = preview ?: return
        if (turning || area.width <= 0f) return
        turning = true
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val next = (turns + if (clockwise) 1 else 3) % 4
        val fit = minOf(area.width / shown.width, area.height / shown.height)
        val fitTurned = minOf(area.width / shown.height, area.height / shown.width)
        scope.launch {
            val picture = async(Dispatchers.Default) { photo.preview(next) }
            val ease = tween<Float>(320, easing = FastOutSlowInEasing)
            launch { zoom.animateTo(fitTurned / fit, ease) }
            spin.animateTo(if (clockwise) 90f else -90f, ease)
            val turned = picture.await()
            val c = crop
            crop = if (clockwise) Rect(1f - c.bottom, c.left, 1f - c.top, c.right) else Rect(c.top, 1f - c.right, c.bottom, 1f - c.left)
            preview = turned
            turns = next
            spin.snapTo(0f)
            zoom.snapTo(1f)
            turning = false
        }
    }

    fun apply() {
        if (busy) return
        val (w, h) = photo.turnedSize(turns)
        val full = crop.left <= 0.001f && crop.top <= 0.001f && crop.right >= 0.999f && crop.bottom >= 0.999f
        val box = if (full) null else PageBox(crop.left * w, crop.top * h, crop.right * w, crop.bottom * h)
        busy = true
        vm.reframe(Framing(turns, box)) { onClose() }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0C))) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 24.dp, end = 24.dp, top = 80.dp)
                .padding(bottom = with(LocalDensity.current) { controls.toDp() } + 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = preview ?: return@BoxWithConstraints
            val density = LocalDensity.current
            val availW = with(density) { maxWidth.toPx() }
            val availH = with(density) { maxHeight.toPx() }
            SideEffect { area = Size(availW, availH) }
            val fit = minOf(availW / bitmap.width, availH / bitmap.height)
            val w = bitmap.width * fit
            val h = bitmap.height * fit
            val reach = with(density) { 26.dp.toPx() }
            Box(
                Modifier
                    .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                    .graphicsLayer {
                        rotationZ = spin.value
                        scaleX = zoom.value
                        scaleY = zoom.value
                    },
            ) {
                Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize())
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = frameAlpha }
                        .pointerInput(w, h) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val c = crop
                                val l = c.left * w
                                val t = c.top * h
                                val r = c.right * w
                                val b = c.bottom * h
                                val p = down.position
                                val nearL = abs(p.x - l) < reach
                                val nearR = abs(p.x - r) < reach
                                val nearT = abs(p.y - t) < reach
                                val nearB = abs(p.y - b) < reach
                                val inX = p.x in (l - reach)..(r + reach)
                                val inY = p.y in (t - reach)..(b + reach)
                                val handle = when {
                                    nearL && nearT -> Handle.TOP_LEFT
                                    nearR && nearT -> Handle.TOP_RIGHT
                                    nearL && nearB -> Handle.BOTTOM_LEFT
                                    nearR && nearB -> Handle.BOTTOM_RIGHT
                                    nearL && inY -> Handle.LEFT
                                    nearR && inY -> Handle.RIGHT
                                    nearT && inX -> Handle.TOP
                                    nearB && inX -> Handle.BOTTOM
                                    p.x in l..r && p.y in t..b -> Handle.MOVE
                                    else -> return@awaitEachGesture
                                }
                                down.consume()
                                dragging = true
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val d = change.positionChange()
                                    change.consume()
                                    val dx = d.x / w
                                    val dy = d.y / h
                                    val o = crop
                                    crop = when (handle) {
                                        Handle.MOVE -> {
                                            val mx = dx.coerceIn(-o.left, 1f - o.right)
                                            val my = dy.coerceIn(-o.top, 1f - o.bottom)
                                            Rect(o.left + mx, o.top + my, o.right + mx, o.bottom + my)
                                        }
                                        else -> {
                                            val left = if (handle in setOf(Handle.LEFT, Handle.TOP_LEFT, Handle.BOTTOM_LEFT)) (o.left + dx).coerceIn(0f, o.right - MIN_SIDE) else o.left
                                            val right = if (handle in setOf(Handle.RIGHT, Handle.TOP_RIGHT, Handle.BOTTOM_RIGHT)) (o.right + dx).coerceIn(o.left + MIN_SIDE, 1f) else o.right
                                            val top = if (handle in setOf(Handle.TOP, Handle.TOP_LEFT, Handle.TOP_RIGHT)) (o.top + dy).coerceIn(0f, o.bottom - MIN_SIDE) else o.top
                                            val bottom = if (handle in setOf(Handle.BOTTOM, Handle.BOTTOM_LEFT, Handle.BOTTOM_RIGHT)) (o.bottom + dy).coerceIn(o.top + MIN_SIDE, 1f) else o.bottom
                                            Rect(left, top, right, bottom)
                                        }
                                    }
                                }
                                dragging = false
                            }
                        },
                ) {
                    val c = crop
                    val frame = Rect(c.left * size.width, c.top * size.height, c.right * size.width, c.bottom * size.height)
                    val shade = Color.Black.copy(alpha = 0.58f)
                    drawRect(shade, Offset.Zero, Size(size.width, frame.top))
                    drawRect(shade, Offset(0f, frame.bottom), Size(size.width, size.height - frame.bottom))
                    drawRect(shade, Offset(0f, frame.top), Size(frame.left, frame.height))
                    drawRect(shade, Offset(frame.right, frame.top), Size(size.width - frame.right, frame.height))
                    if (grid > 0f) {
                        for (k in 1..2) {
                            val x = frame.left + frame.width * k / 3
                            val y = frame.top + frame.height * k / 3
                            drawLine(Color.White.copy(alpha = 0.45f * grid), Offset(x, frame.top), Offset(x, frame.bottom), 1.dp.toPx())
                            drawLine(Color.White.copy(alpha = 0.45f * grid), Offset(frame.left, y), Offset(frame.right, y), 1.dp.toPx())
                        }
                    }
                    drawRect(Color.White.copy(alpha = 0.9f), frame.topLeft, frame.size, style = Stroke(1.2.dp.toPx()))
                    val arm = minOf(22.dp.toPx(), frame.width / 3, frame.height / 3)
                    val thick = 3.dp.toPx()
                    val corners = listOf(
                        frame.topLeft to Offset(1f, 1f),
                        Offset(frame.right, frame.top) to Offset(-1f, 1f),
                        Offset(frame.left, frame.bottom) to Offset(1f, -1f),
                        frame.bottomRight to Offset(-1f, -1f),
                    )
                    for ((at, dir) in corners) {
                        drawLine(Color.Black.copy(alpha = 0.55f), at, at + Offset(arm * dir.x, 0f), thick + 2.dp.toPx(), StrokeCap.Square)
                        drawLine(Color.Black.copy(alpha = 0.55f), at, at + Offset(0f, arm * dir.y), thick + 2.dp.toPx(), StrokeCap.Square)
                    }
                    for ((at, dir) in corners) {
                        drawLine(Color.White, at, at + Offset(arm * dir.x, 0f), thick, StrokeCap.Square)
                        drawLine(Color.White, at, at + Offset(0f, arm * dir.y), thick, StrokeCap.Square)
                    }
                }
            }
        }

        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(PhosphorIcons.Regular.X, stringResource(R.string.close), onClick = onClose)
            Text(
                stringResource(R.string.adjust_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
            )
            if (turns != 0 || crop != Rect(0f, 0f, 1f, 1f)) {
                Text(
                    stringResource(R.string.adjust_reset),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .pressable(scale = 0.94f) {
                            if (!turning) {
                                crop = Rect(0f, 0f, 1f, 1f)
                                if (turns != 0) {
                                    scope.launch {
                                        preview = withContext(Dispatchers.Default) { photo.preview(0) }
                                        turns = 0
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { controls = it.height }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.adjust_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(PhosphorIcons.Regular.ArrowCounterClockwise, stringResource(R.string.adjust_turn_left), size = 52.dp) { turn(false) }
                GlassIconButton(PhosphorIcons.Regular.ArrowClockwise, stringResource(R.string.adjust_turn_right), size = 52.dp) { turn(true) }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                stringResource(if (busy) R.string.adjust_working else R.string.adjust_apply),
                Modifier.fillMaxWidth(),
                PhosphorIcons.Regular.Check,
            ) { if (changed) apply() else onClose() }
        }
    }
}
