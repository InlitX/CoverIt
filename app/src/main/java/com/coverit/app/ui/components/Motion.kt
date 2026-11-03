package com.coverit.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class Line(val width: Float, val barStart: Float, val barEnd: Float)

private val lines = listOf(
    Line(0.92f, 0.30f, 0.72f),
    Line(0.78f, 0.0f, 0.0f),
    Line(0.86f, 0.08f, 0.46f),
    Line(0.64f, 0.0f, 0.0f),
    Line(0.88f, 0.52f, 0.88f),
)

@Composable
fun RedactingLines(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "redacting")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    val text = MaterialTheme.colorScheme.surfaceVariant
    val ink = MaterialTheme.colorScheme.onBackground
    Canvas(modifier.size(width = 180.dp, height = 112.dp)) {
        val rowHeight = size.height / lines.size
        val barHeight = rowHeight * 0.42f
        val radius = CornerRadius(barHeight / 2)
        val redacted = lines.filter { it.barEnd > it.barStart }
        lines.forEachIndexed { i, line ->
            val y = i * rowHeight + (rowHeight - barHeight) / 2
            drawRoundRect(text, Offset(0f, y), Size(size.width * line.width, barHeight), radius)
            val order = redacted.indexOf(line)
            if (order >= 0) {
                val start = 0.08f + order * 0.2f
                val grow = ((t - start) / 0.18f).coerceIn(0f, 1f)
                val fade = if (t > 0.86f) 1f - ((t - 0.86f) / 0.14f) else 1f
                val eased = FastOutSlowInEasing.transform(grow)
                if (eased > 0f) {
                    val left = size.width * line.barStart
                    val full = size.width * (line.barEnd - line.barStart)
                    drawRoundRect(
                        ink.copy(alpha = fade.coerceIn(0f, 1f)),
                        Offset(left, y - 2.dp.toPx()),
                        Size(full * eased, barHeight + 4.dp.toPx()),
                        CornerRadius(4.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
fun AppMark(modifier: Modifier = Modifier, size: Dp = 88.dp) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(250)
        grow.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
    }
    val tile = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier.size(size)) {
        val s = this.size.width
        drawRoundRect(tile, cornerRadius = CornerRadius(s * 0.28f))
        val left = s * 0.26f
        val thin = s * 0.035f
        drawRoundRect(ink.copy(alpha = 0.38f), Offset(left, s * 0.31f), Size(s * 0.36f, thin * 2), CornerRadius(thin))
        drawRoundRect(ink, Offset(left, s * 0.44f), Size(s * 0.48f * grow.value.coerceAtLeast(0.02f), s * 0.12f), CornerRadius(s * 0.035f))
        drawRoundRect(ink.copy(alpha = 0.38f), Offset(left, s * 0.62f), Size(s * 0.42f, thin * 2), CornerRadius(thin))
    }
}

@Composable
fun AnimatedSeal(success: Boolean, size: Dp = 84.dp) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(220)) }
        launch { scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
        delay(180)
        draw.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    val fill = MaterialTheme.colorScheme.primary
    val mark = MaterialTheme.colorScheme.onPrimary
    Canvas(Modifier.size(size)) {
        val s = scale.value
        val r = this.size.minDimension / 2 * s
        drawCircle(fill.copy(alpha = alpha.value), r)
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            if (success) {
                moveTo(w * 0.31f, h * 0.52f)
                lineTo(w * 0.45f, h * 0.65f)
                lineTo(w * 0.70f, h * 0.38f)
            } else {
                moveTo(w * 0.5f, h * 0.30f)
                lineTo(w * 0.5f, h * 0.56f)
                moveTo(w * 0.5f, h * 0.69f)
                lineTo(w * 0.5f, h * 0.70f)
            }
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val partial = Path()
        if (success) {
            measure.getSegment(0f, measure.length * draw.value, partial, true)
        } else if (draw.value > 0f) {
            partial.addPath(path)
        }
        drawPath(
            partial,
            mark.copy(alpha = if (success) 1f else draw.value),
            style = Stroke(size.toPx() * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
fun Entrance(index: Int, modifier: Modifier = Modifier, play: Boolean = true, content: @Composable () -> Unit) {
    val progress = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!play) return@LaunchedEffect
        delay(40L * index.coerceIn(0, 5))
        progress.animateTo(1f, tween(300, easing = LinearEasing))
    }
    val rise = with(androidx.compose.ui.platform.LocalDensity.current) { 16.dp.toPx() }
    androidx.compose.foundation.layout.Box(
        modifier.graphicsLayer {
            val t = progress.value
            alpha = androidx.compose.animation.core.EaseOut.transform(t)
            translationY = (1f - androidx.compose.animation.core.EaseOutCubic.transform(t)) * rise
        },
    ) { content() }
}
