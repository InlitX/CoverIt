package com.coverit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coverit.app.redact.Style
import kotlin.random.Random

@Composable
fun StylePreview(style: Style, modifier: Modifier = Modifier, tag: String = "") {
    val line = MaterialTheme.colorScheme.surfaceVariant
    val measurer = rememberTextMeasurer()
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(modifier.fillMaxWidth().height(40.dp)) {
        val h = size.height
        val barH = h * 0.42f
        val y = (h - barH) / 2
        val r = CornerRadius(barH / 2)
        drawRoundRect(line, Offset(0f, y), Size(size.width * 0.22f, barH), r)
        val left = size.width * 0.27f
        val width = size.width * 0.46f
        val top = h * 0.16f
        val height = h * 0.68f
        when (style) {
            Style.BLACK -> drawRoundRect(Color.Black, Offset(left, top), Size(width, height), CornerRadius(4.dp.toPx()))
            Style.PIXELATE -> {
                val random = Random(7)
                val cell = height / 3
                var x = left
                while (x < left + width) {
                    var yy = top
                    while (yy < top + height - 0.5f) {
                        val shade = 0.18f + random.nextFloat() * 0.5f
                        drawRect(ink.copy(alpha = shade), Offset(x, yy), Size(minOf(cell, left + width - x), cell))
                        yy += cell
                    }
                    x += cell
                }
            }
            Style.BLUR -> drawRoundRect(
                Brush.horizontalGradient(
                    0f to ink.copy(alpha = 0.05f),
                    0.2f to ink.copy(alpha = 0.32f),
                    0.5f to ink.copy(alpha = 0.22f),
                    0.8f to ink.copy(alpha = 0.34f),
                    1f to ink.copy(alpha = 0.05f),
                ),
                Offset(left, top),
                Size(width, height),
                CornerRadius(height / 2),
            )
            Style.LABEL -> {
                drawRoundRect(Color.Black, Offset(left, top), Size(width, height), CornerRadius(4.dp.toPx()))
                val layout = measurer.measure(tag.uppercase(), TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp))
                drawText(layout, topLeft = Offset(left + (width - layout.size.width) / 2, top + (height - layout.size.height) / 2))
            }
        }
        drawRoundRect(line, Offset(size.width * 0.78f, y), Size(size.width * 0.22f, barH), r)
    }
}
