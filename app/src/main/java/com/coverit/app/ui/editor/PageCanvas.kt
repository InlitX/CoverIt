package com.coverit.app.ui.editor

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.CopySimple
import com.adamglin.phosphoricons.regular.EyeSlash
import com.coverit.app.R
import com.coverit.app.doc.Mark
import com.coverit.app.doc.Page
import com.coverit.app.pdf.Box as PageBox
import com.coverit.app.redact.Cover
import com.coverit.app.redact.Painter
import com.coverit.app.redact.Style
import com.coverit.app.ui.common.label
import com.coverit.app.ui.common.tag
import com.coverit.app.ui.components.hairline
import com.coverit.app.ui.components.lift
import com.coverit.app.ui.components.pressable
import com.coverit.app.ui.result.Check
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val PageShape = RoundedCornerShape(14.dp)

object PagePreviews {
    private val cache = object : android.util.LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    fun key(page: Page, base: Bitmap, style: Style, covered: List<Cover>, watermark: String?) = "${page.index}:${page.version}:${base.width}:$style:$watermark:$covered"

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, value: Bitmap) {
        cache.put(key, value)
    }

    fun clear() = cache.evictAll()
}

data class Grip(val mark: Mark, val index: Int) {
    val box: PageBox? get() = if (index == WHOLE) mark.boxes.takeIf { it.isNotEmpty() }?.reduce { a, b -> a.union(b) } else mark.boxes.getOrNull(index)

    companion object {
        const val WHOLE = -1
    }
}

data class Flash(val mark: Mark, val at: Long)

private enum class Part { BODY, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

class PageActions(
    val onEdit: (Grip?) -> Unit,
    val onCover: (Mark) -> Unit,
    val onReshapeStart: (Mark) -> Unit,
    val onReshape: (Mark, Int, PageBox) -> Unit,
    val onUncover: (Mark) -> Unit,
    val onMore: (Mark) -> Unit,
    val onLasso: (FloatArray) -> Unit,
    val onArea: (PageBox) -> Unit,
    val onScroll: (Float) -> Float,
    val onHold: (Float, Float) -> Unit,
    val onBrush: (FloatArray, Float) -> Unit = { _, _ -> },
    val onDoubleTap: ((Offset) -> Unit)? = null,
)

@Composable
fun PageCanvas(
    page: Page,
    marks: List<Mark>,
    style: Style,
    drawing: Boolean,
    tool: DrawTool = DrawTool.LASSO,
    editing: Grip?,
    flash: Flash?,
    render: suspend (Int, Int) -> Bitmap?,
    actions: PageActions,
    modifier: Modifier = Modifier,
    quality: Float = 1f,
    cached: (Int, Int) -> Bitmap? = { _, _ -> null },
    brush: Dp = 14.dp,
    watermark: String? = null,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(page.width / page.height)
            .hairline(PageShape)
            .clip(PageShape)
            .background(Color.White),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.roundToPx() }
        val scale = widthPx / page.width
        val bitmap by produceState(cached(page.index, (widthPx * quality).toInt()), page.index, page.version, widthPx, quality) {
            if (value == null) value = render(page.index, (widthPx * quality).toInt())
        }
        val resources = LocalContext.current.resources
        val covered = marks.filter { it.checked }.flatMap { m ->
            val tag = if (style == Style.LABEL) resources.getString(m.kind.tag) else ""
            if (m.stroke != null) {
                m.covers(tag)
            } else {
                Cover.of(m.boxes.filterIndexed { i, _ -> editing == null || editing.mark != m || editing.index != i }, tag)
            }
        }
        val preview by produceState(bitmap?.let { b -> PagePreviews.get(PagePreviews.key(page, b, style, covered, watermark)) }, bitmap, style, covered, watermark) {
            val base = bitmap ?: return@produceState
            val key = PagePreviews.key(page, base, style, covered, watermark)
            PagePreviews.get(key)?.let {
                value = it
                return@produceState
            }
            if (covered.isEmpty() && watermark == null) {
                value = base
                return@produceState
            }
            value = withContext(Dispatchers.Default) {
                val copy = base.copy(Bitmap.Config.ARGB_8888, true)
                val k = base.width / page.width
                covered.forEach { Painter.cover(copy, it, k, style) }
                watermark?.let { Painter.watermark(copy, it) }
                copy
            }.also { PagePreviews.put(key, it) }
        }
        val shown by animateFloatAsState(if (preview != null) 1f else 0f, tween(260), label = "pageFade")
        val stroke = remember { mutableStateListOf<Offset>() }
        val fade = remember { Animatable(0f) }
        val pulse = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val currentMarks by rememberUpdatedState(marks)
        val currentEditing by rememberUpdatedState(editing)
        val currentActions by rememberUpdatedState(actions)
        val phase = remember { Animatable(0f) }
        val tracing = stroke.isNotEmpty()
        LaunchedEffect(tracing) {
            if (!tracing) return@LaunchedEffect
            while (true) {
                phase.snapTo(0f)
                phase.animateTo(1f, tween(1400, easing = LinearEasing))
            }
        }
        val reach = with(density) { 26.dp.toPx() }
        val brushPx = with(density) { brush.toPx() }
        val minSide = with(density) { 10.dp.toPx() } / scale

        LaunchedEffect(flash) {
            if (flash == null) return@LaunchedEffect
            repeat(2) {
                pulse.animateTo(1f, tween(260))
                pulse.animateTo(0.15f, tween(420))
            }
            pulse.animateTo(0f, tween(300))
        }

        (preview ?: bitmap)?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), alpha = if (preview != null) shown else 1f) }

        fun partAt(box: PageBox, pos: Offset): Part? {
            val l = box.left * scale
            val t = box.top * scale
            val r = box.right * scale
            val b = box.bottom * scale
            val corner = listOf(
                Part.TOP_LEFT to Offset(l, t),
                Part.TOP_RIGHT to Offset(r, t),
                Part.BOTTOM_LEFT to Offset(l, b),
                Part.BOTTOM_RIGHT to Offset(r, b),
            ).map { (part, at) -> part to (pos - at).getDistance() }.minBy { it.second }
            val inside = pos.x in l..r && pos.y in t..b
            return when {
                inside && corner.second > reach * 0.45f -> Part.BODY
                corner.second <= reach -> corner.first
                pos.x in (l - reach / 3)..(r + reach / 3) && pos.y in (t - reach / 3)..(b + reach / 3) -> Part.BODY
                else -> null
            }
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(drawing, tool, widthPx, page.version) {
                    if (drawing) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            scope.launch { fade.snapTo(1f) }
                            stroke.clear()
                            stroke += down.position
                            var scrolling = false
                            var lastConsumed = 0f
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break
                                if (pressed.size >= 2) {
                                    if (!scrolling) {
                                        scrolling = true
                                        stroke.clear()
                                    }
                                    val local = pressed.map { it.position.y - it.previousPosition.y }.average().toFloat()
                                    lastConsumed = currentActions.onScroll(-(local - lastConsumed))
                                } else if (!scrolling) {
                                    stroke += pressed[0].position
                                }
                                event.changes.forEach { it.consume() }
                            }
                            if (!scrolling && stroke.isNotEmpty() && tool == DrawTool.BRUSH) {
                                val points = FloatArray(stroke.size * 2)
                                stroke.forEachIndexed { i, o ->
                                    points[i * 2] = o.x / scale
                                    points[i * 2 + 1] = o.y / scale
                                }
                                currentActions.onBrush(points, brushPx / scale)
                                scope.launch {
                                    fade.animateTo(0f, tween(220))
                                    stroke.clear()
                                }
                            } else if (!scrolling && stroke.size > 1 && tool == DrawTool.BOX) {
                                val a = stroke.first()
                                val b = stroke.last()
                                currentActions.onArea(
                                    PageBox(
                                        (minOf(a.x, b.x) / scale).coerceAtLeast(0f),
                                        (minOf(a.y, b.y) / scale).coerceAtLeast(0f),
                                        (maxOf(a.x, b.x) / scale).coerceAtMost(page.width),
                                        (maxOf(a.y, b.y) / scale).coerceAtMost(page.height),
                                    ),
                                )
                                scope.launch {
                                    fade.animateTo(0f, tween(220))
                                    stroke.clear()
                                }
                            } else if (!scrolling && stroke.size > 1) {
                                val points = FloatArray(stroke.size * 2)
                                stroke.forEachIndexed { i, o ->
                                    points[i * 2] = o.x / scale
                                    points[i * 2 + 1] = o.y / scale
                                }
                                currentActions.onLasso(points)
                                scope.launch {
                                    fade.animateTo(0f, tween(320))
                                    stroke.clear()
                                }
                            } else {
                                stroke.clear()
                            }
                        }
                    } else {
                        detectTapGestures(
                            onDoubleTap = currentActions.onDoubleTap?.let { act -> { pos: Offset -> act(pos) } },
                            onTap = { pos ->
                                val x = pos.x / scale
                                val y = pos.y / scale
                                val e = currentEditing
                                val held = e?.mark?.boxes?.getOrNull(e.index)
                                if (held != null && partAt(held, pos) != null) return@detectTapGestures
                                var hit: Grip? = null
                                for (m in currentMarks) {
                                    m.boxes.forEachIndexed { i, b ->
                                        if (x in b.left..b.right && y in b.top..b.bottom) hit = Grip(m, if (m.stroke != null) Grip.WHOLE else i)
                                    }
                                }
                                val g = hit
                                when {
                                    g == null -> currentActions.onEdit(null)
                                    g.mark.checked -> currentActions.onEdit(g)
                                    else -> {
                                        currentActions.onCover(g.mark)
                                        currentActions.onEdit(null)
                                    }
                                }
                            },
                            onLongPress = { pos -> currentActions.onHold(pos.x / scale, pos.y / scale) },
                        )
                    }
                }
                .pointerInput(drawing, widthPx, page.version) {
                    if (drawing) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val e = currentEditing ?: return@awaitEachGesture
                        val start = e.mark.boxes.getOrNull(e.index) ?: return@awaitEachGesture
                        val part = partAt(start, down.position) ?: return@awaitEachGesture
                        down.consume()
                        var box = start
                        var started = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val d = change.positionChange() / scale
                            change.consume()
                            if (d == Offset.Zero) continue
                            if (!started) {
                                started = true
                                currentActions.onReshapeStart(e.mark)
                            }
                            box = when (part) {
                                Part.BODY -> PageBox(box.left + d.x, box.top + d.y, box.right + d.x, box.bottom + d.y)
                                Part.TOP_LEFT -> PageBox(minOf(box.left + d.x, box.right - minSide), minOf(box.top + d.y, box.bottom - minSide), box.right, box.bottom)
                                Part.TOP_RIGHT -> PageBox(box.left, minOf(box.top + d.y, box.bottom - minSide), maxOf(box.right + d.x, box.left + minSide), box.bottom)
                                Part.BOTTOM_LEFT -> PageBox(minOf(box.left + d.x, box.right - minSide), box.top, box.right, maxOf(box.bottom + d.y, box.top + minSide))
                                Part.BOTTOM_RIGHT -> PageBox(box.left, box.top, maxOf(box.right + d.x, box.left + minSide), maxOf(box.bottom + d.y, box.top + minSide))
                            }.let { b ->
                                val dx = when {
                                    b.left < 0f -> -b.left
                                    b.right > page.width -> page.width - b.right
                                    else -> 0f
                                }
                                val dy = when {
                                    b.top < 0f -> -b.top
                                    b.bottom > page.height -> page.height - b.bottom
                                    else -> 0f
                                }
                                if (part == Part.BODY) PageBox(b.left + dx, b.top + dy, b.right + dx, b.bottom + dy) else PageBox(maxOf(b.left, 0f), maxOf(b.top, 0f), minOf(b.right, page.width), minOf(b.bottom, page.height))
                            }
                            currentActions.onReshape(e.mark, e.index, box)
                        }
                    }
                },
        ) {
            for (mark in marks) {
                if (mark.checked) continue
                for (b in mark.boxes) {
                    drawRoundRect(
                        Color(0xFF7A7A7A),
                        Offset(b.left * scale, b.top * scale),
                        Size((b.right - b.left) * scale, (b.bottom - b.top) * scale),
                        CornerRadius(2.dp.toPx()),
                        style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f))),
                    )
                }
            }
            if (flash != null && flash.mark in marks && pulse.value > 0f) {
                for (b in flash.mark.boxes) drawFlash(b, scale, pulse.value)
            }
            editing?.let { e ->
                val box = e.box ?: return@let
                if (e.index == Grip.WHOLE) drawFrame(box, scale) else drawGrip(box, scale, style)
            }
            if (stroke.isNotEmpty() && tool == DrawTool.BRUSH) {
                drawBrush(stroke, brushPx, style, fade.value)
            } else if (stroke.size > 1) {
                if (tool == DrawTool.BOX) drawArea(stroke.first(), stroke.last(), fade.value) else drawLasso(stroke, phase.value, fade.value)
            }
        }

        editing?.let { e ->
            val box = e.box ?: return@let
            GripBar(
                box = box,
                scale = scale,
                pageHeightPx = page.height * scale,
                canMore = e.mark.value.isNotBlank(),
                onUncover = { actions.onUncover(e.mark) },
                onMore = { actions.onMore(e.mark) },
                onDone = { actions.onEdit(null) },
            )
        }
    }
}

private fun DrawScope.drawFlash(box: PageBox, scale: Float, amount: Float) {
    val pad = 5.dp.toPx() + 4.dp.toPx() * amount
    val topLeft = Offset(box.left * scale - pad, box.top * scale - pad)
    val size = Size((box.right - box.left) * scale + pad * 2, (box.bottom - box.top) * scale + pad * 2)
    val r = CornerRadius(6.dp.toPx())
    drawRoundRect(Color.Black.copy(alpha = 0.10f * amount), topLeft, size, r)
    drawRoundRect(Color.White.copy(alpha = amount), topLeft, size, r, style = Stroke(4.dp.toPx()))
    drawRoundRect(Color.Black.copy(alpha = amount), topLeft, size, r, style = Stroke(2.dp.toPx()))
}

private fun DrawScope.drawGrip(box: PageBox, scale: Float, style: Style) {
    val topLeft = Offset(box.left * scale, box.top * scale)
    val size = Size((box.right - box.left) * scale, (box.bottom - box.top) * scale)
    drawRect(if (style == Style.BLACK || style == Style.LABEL) Color.Black else Color(0xFF2B2B2E).copy(alpha = 0.82f), topLeft, size)
    val frame = 1.6.dp.toPx()
    drawRect(Color.White, topLeft - Offset(frame, frame), Size(size.width + frame * 2, size.height + frame * 2), style = Stroke(frame))
    drawRect(Color.Black.copy(alpha = 0.35f), topLeft - Offset(frame * 2, frame * 2), Size(size.width + frame * 4, size.height + frame * 4), style = Stroke(0.6.dp.toPx()))
    val r = 6.5.dp.toPx()
    for (c in listOf(topLeft, topLeft + Offset(size.width, 0f), topLeft + Offset(0f, size.height), topLeft + Offset(size.width, size.height))) {
        drawCircle(Color.Black.copy(alpha = 0.25f), r + 1.5.dp.toPx(), c)
        drawCircle(Color.White, r, c)
        drawCircle(Color.Black, r * 0.42f, c)
    }
}

private fun DrawScope.drawFrame(box: PageBox, scale: Float) {
    val pad = 4.dp.toPx()
    val topLeft = Offset(box.left * scale - pad, box.top * scale - pad)
    val size = Size((box.right - box.left) * scale + pad * 2, (box.bottom - box.top) * scale + pad * 2)
    drawRoundRect(Color.White, topLeft, size, CornerRadius(6.dp.toPx()), style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))))
    drawRoundRect(Color.Black, topLeft, size, CornerRadius(6.dp.toPx()), style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))))
}

@Composable
private fun GripBar(
    box: PageBox,
    scale: Float,
    pageHeightPx: Float,
    canMore: Boolean,
    onUncover: () -> Unit,
    onMore: () -> Unit,
    onDone: () -> Unit,
) {
    val density = LocalDensity.current
    val barHeight = with(density) { 40.dp.toPx() }
    val gap = with(density) { 14.dp.toPx() }
    val above = box.top * scale - gap - barHeight
    val y = if (above > 0f) above else minOf(box.bottom * scale + gap, pageHeightPx - barHeight)
    val x = (box.left * scale).coerceAtLeast(with(density) { 8.dp.toPx() })
    Row(
        Modifier
            .offset { IntOffset(x.toInt(), y.toInt()) }
            .height(40.dp)
            .lift(CircleShape, 10.dp)
            .clip(CircleShape)
            .background(Color(0xFF111113))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GripAction(PhosphorIcons.Regular.EyeSlash, stringResource(R.string.grip_uncover), onUncover)
        if (canMore) GripAction(PhosphorIcons.Regular.CopySimple, stringResource(R.string.grip_more), onMore)
        GripAction(PhosphorIcons.Regular.Check, stringResource(R.string.done), onDone)
    }
}

@Composable
private fun GripAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .height(32.dp)
            .clip(CircleShape)
            .pressable(scale = 0.92f, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(15.dp), tint = Color.White)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1)
    }
}

enum class DrawTool { LASSO, BOX, BRUSH }

private fun DrawScope.drawBrush(points: List<Offset>, radius: Float, style: Style, alpha: Float) {
    val color = if (style == Style.BLACK || style == Style.LABEL) Color.Black else Color(0xFF2B2B2E)
    if (points.size == 1) {
        drawCircle(color.copy(alpha = 0.85f * alpha), radius, points[0])
        return
    }
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    drawPath(path, color.copy(alpha = 0.85f * alpha), style = Stroke(radius * 2, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawArea(a: Offset, b: Offset, alpha: Float) {
    val topLeft = Offset(minOf(a.x, b.x), minOf(a.y, b.y))
    val size = Size(kotlin.math.abs(a.x - b.x), kotlin.math.abs(a.y - b.y))
    drawRect(Color.Black.copy(alpha = 0.35f * alpha), topLeft, size)
    drawRect(Color.White.copy(alpha = alpha), topLeft, size, style = Stroke(2.5.dp.toPx()))
    drawRect(Color.Black.copy(alpha = alpha), topLeft, size, style = Stroke(1.dp.toPx()))
}

private fun DrawScope.drawLasso(points: List<Offset>, phase: Float, alpha: Float) {
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            quadraticTo(a.x, a.y, (a.x + b.x) / 2, (a.y + b.y) / 2)
        }
    }
    val span = size.width.coerceAtLeast(1f)
    val shift = phase * span * 2 - span
    val light = Brush.linearGradient(
        listOf(Color(0xFF111111), Color(0xFF8A8A8A), Color(0xFFF2F2F2), Color(0xFF8A8A8A), Color(0xFF111111)),
        start = Offset(shift, 0f),
        end = Offset(shift + span, size.height * 0.3f),
        tileMode = TileMode.Mirror,
    )
    val cap = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(path, Color.Black.copy(alpha = 0.10f * alpha), style = cap)
    drawPath(path, Color.Black.copy(alpha = 0.16f * alpha), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, light, alpha = alpha, style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}
