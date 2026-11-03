package com.coverit.app.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowUUpLeft
import com.adamglin.phosphoricons.regular.ArrowUUpRight
import com.adamglin.phosphoricons.regular.Lasso
import com.adamglin.phosphoricons.regular.MagnifyingGlassPlus
import com.adamglin.phosphoricons.regular.PaintBrush
import com.adamglin.phosphoricons.regular.Selection
import com.adamglin.phosphoricons.regular.X
import com.coverit.app.R
import com.coverit.app.doc.Mark
import com.coverit.app.doc.Session
import com.coverit.app.ui.components.GlassIconButton
import com.coverit.app.ui.components.Pill
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FocusPage(
    session: Session,
    index: Int,
    render: suspend (Int, Int) -> Bitmap?,
    brushSize: Int,
    onBrushSize: (Int) -> Unit,
    onSelect: (Mark) -> Unit,
    onClose: () -> Unit,
) {
    val page = session.source.pages[index]
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var drawing by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(DrawTool.LASSO) }
    var editing by remember { mutableStateOf<Grip?>(null) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var view by remember { mutableStateOf(IntSize.Zero) }
    var content by remember { mutableStateOf(IntSize.Zero) }
    var hint by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2600)
        hint = false
    }
    BackHandler(onBack = onClose)

    fun clamp(p: Offset, z: Float): Offset {
        val maxX = (content.width * z - view.width).coerceAtLeast(0f) / 2
        val maxY = (content.height * z - view.height).coerceAtLeast(0f) / 2
        return Offset(p.x.coerceIn(-maxX, maxX), p.y.coerceIn(-maxY, maxY))
    }

    fun toggleZoom(at: Offset) {
        val fromZoom = zoom
        val fromPan = pan
        val toZoom = if (zoom < 1.6f) 2.8f else 1f
        val c = at - Offset(content.width / 2f, content.height / 2f)
        val toPan = clamp(-c * toZoom, toZoom)
        hint = false
        scope.launch {
            animate(0f, 1f, animationSpec = tween(280, easing = FastOutSlowInEasing)) { t, _ ->
                zoom = fromZoom + (toZoom - fromZoom) * t
                pan = fromPan + (toPan - fromPan) * t
            }
        }
    }

    val actions = PageActions(
        onEdit = { editing = it },
        onCover = { session.toggle(it, true) },
        onReshapeStart = { session.beginReshape() },
        onReshape = { mark, i, box -> session.reshape(mark, i, box) },
        onUncover = {
            editing = null
            session.uncover(it)
        },
        onMore = {
            editing = null
            onSelect(it)
        },
        onLasso = { points ->
            drawing = false
            val mark = session.lasso(index, points)
            if (mark != null && mark.value.isNotBlank()) onSelect(mark)
        },
        onArea = { box ->
            drawing = false
            session.area(index, box)
        },
        onScroll = { 0f },
        onHold = { x, y -> session.wordAt(index, x, y)?.let(onSelect) },
        onBrush = { points, radius ->
            session.brush(index, points, radius)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        },
        onDoubleTap = { toggleZoom(it) },
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds()
            .onSizeChanged { view = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size < 2) continue
                        val k = event.calculateZoom()
                        val c = event.calculateCentroid() - Offset(view.width / 2f, view.height / 2f)
                        val next = (zoom * k).coerceIn(1f, 6f)
                        val p = c - (c - pan) * (next / zoom) + event.calculatePan()
                        zoom = next
                        pan = clamp(p, next)
                        hint = false
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        if (zoom > 1f && !change.isConsumed && event.changes.count { it.pressed } == 1) {
                            pan = clamp(pan + change.positionChange(), zoom)
                            change.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        PageCanvas(
            page = page,
            marks = session.marks.filter { it.page == index },
            style = session.style,
            drawing = drawing,
            tool = tool,
            editing = editing?.takeIf { it.mark in session.marks },
            flash = null,
            render = render,
            actions = actions,
            quality = 2f,
            brush = BrushSizes[brushSize] / zoom.coerceAtLeast(1f),
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

        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(PhosphorIcons.Regular.X, stringResource(R.string.close), onClick = onClose)
            Text(
                stringResource(R.string.editor_page_short, index + 1) + " / " + session.source.pages.size,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
            )
            HistoryButtons(session) { editing = null }
        }

        Box(Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 20.dp)) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(drawing && tool == DrawTool.BRUSH, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
                    Box(Modifier.padding(bottom = 8.dp)) { BrushSizePicker(brushSize, onBrushSize) }
                }
                AnimatedVisibility(hint || drawing, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
                    Pill(
                        stringResource(
                            when {
                                !drawing -> R.string.focus_hint
                                tool == DrawTool.BOX -> R.string.editor_box_hint
                                tool == DrawTool.BRUSH -> R.string.editor_brush_hint
                                else -> R.string.editor_draw_hint
                            },
                        ),
                        icon = when {
                            !drawing -> PhosphorIcons.Regular.MagnifyingGlassPlus
                            tool == DrawTool.BOX -> PhosphorIcons.Regular.Selection
                            tool == DrawTool.BRUSH -> PhosphorIcons.Regular.PaintBrush
                            else -> PhosphorIcons.Regular.Lasso
                        },
                    )
                }
                Spacer(Modifier.height(10.dp))
                DrawTools(if (drawing) tool else null) { picked ->
                    editing = null
                    drawing = picked != null
                    if (picked != null) tool = picked
                }
            }
        }
    }
}

@Composable
fun HistoryButtons(session: Session, onChange: () -> Unit) {
    Row {
        Box(Modifier.graphicsLayer { alpha = if (session.canUndo) 1f else 0.35f }) {
            GlassIconButton(PhosphorIcons.Regular.ArrowUUpLeft, stringResource(R.string.undo)) {
                if (session.canUndo) {
                    onChange()
                    session.undo()
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.graphicsLayer { alpha = if (session.canRedo) 1f else 0.35f }) {
            GlassIconButton(PhosphorIcons.Regular.ArrowUUpRight, stringResource(R.string.redo)) {
                if (session.canRedo) {
                    onChange()
                    session.redo()
                }
            }
        }
    }
}
