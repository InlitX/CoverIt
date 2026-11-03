package com.coverit.app.redact

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.coverit.app.pdf.Box
import kotlin.math.max
import kotlin.math.roundToInt

class BrushLine(val points: FloatArray, val radius: Float)

data class Cover(val box: Box, val label: String = "", val line: BrushLine? = null, val cells: List<Box> = listOf(box)) {
    companion object {
        fun of(boxes: List<Box>, label: String): List<Cover> {
            val main = boxes.maxByOrNull { (it.right - it.left) * (it.bottom - it.top) }
            return boxes.map { Cover(it, if (it === main) label else "") }
        }
    }
}

object Painter {

    private val black = Paint().apply { color = Color.BLACK }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        letterSpacing = 0.06f
    }

    fun watermark(bitmap: Bitmap, text: String) {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val diagonal = kotlin.math.hypot(w, h)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(78, 40, 40, 40)
            textSize = diagonal * 0.032f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.04f
        }
        val line = text + "     "
        val step = paint.measureText(line).coerceAtLeast(1f)
        val rowGap = paint.textSize * 4.2f
        val canvas = Canvas(bitmap)
        canvas.save()
        canvas.rotate(-30f, w / 2, h / 2)
        var y = h / 2 - diagonal / 2
        var row = 0
        while (y < h / 2 + diagonal / 2) {
            var x = w / 2 - diagonal / 2 - (row % 2) * step / 2
            while (x < w / 2 + diagonal / 2) {
                canvas.drawText(line, x, y, paint)
                x += step
            }
            y += rowGap
            row++
        }
        canvas.restore()
    }

    fun cover(bitmap: Bitmap, cover: Cover, scale: Float, style: Style) {
        cover.line?.let {
            line(bitmap, it, scale, style)
            return
        }
        val b = cover.box
        cover(bitmap, RectF(b.left * scale, b.top * scale, b.right * scale, b.bottom * scale), style, cover.label)
    }

    fun cover(bitmap: Bitmap, area: RectF, style: Style, label: String = "") {
        val rect = Rect(
            area.left.roundToInt().coerceIn(0, bitmap.width),
            area.top.roundToInt().coerceIn(0, bitmap.height),
            area.right.roundToInt().coerceIn(0, bitmap.width),
            area.bottom.roundToInt().coerceIn(0, bitmap.height),
        )
        if (rect.width() <= 0 || rect.height() <= 0) return
        when (style) {
            Style.BLACK -> Canvas(bitmap).drawRect(rect, black)
            Style.PIXELATE -> pixelate(bitmap, rect)
            Style.BLUR -> blur(bitmap, rect)
            Style.LABEL -> label(bitmap, rect, label)
        }
    }

    private fun line(bitmap: Bitmap, line: BrushLine, scale: Float, style: Style) {
        val pts = line.points
        if (pts.size < 2) return
        val outline = android.graphics.Path()
        if (pts.size == 2) {
            outline.addCircle(pts[0] * scale, pts[1] * scale, line.radius * scale, android.graphics.Path.Direction.CW)
        } else {
            val path = android.graphics.Path().apply {
                moveTo(pts[0] * scale, pts[1] * scale)
                for (i in 2 until pts.size step 2) lineTo(pts[i] * scale, pts[i + 1] * scale)
            }
            val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = line.radius * 2 * scale
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            pen.getFillPath(path, outline)
        }
        val canvas = Canvas(bitmap)
        if (style == Style.BLACK || style == Style.LABEL) {
            canvas.drawPath(outline, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })
            return
        }
        val bounds = RectF().also { outline.computeBounds(it, true) }
        val rect = Rect(
            bounds.left.roundToInt().coerceIn(0, bitmap.width),
            bounds.top.roundToInt().coerceIn(0, bitmap.height),
            bounds.right.roundToInt().coerceIn(0, bitmap.width),
            bounds.bottom.roundToInt().coerceIn(0, bitmap.height),
        )
        if (rect.width() <= 0 || rect.height() <= 0) return
        val patch = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height()).copy(Bitmap.Config.ARGB_8888, true)
        cover(patch, RectF(0f, 0f, patch.width.toFloat(), patch.height.toFloat()), style)
        canvas.save()
        canvas.clipPath(outline)
        canvas.drawBitmap(patch, rect.left.toFloat(), rect.top.toFloat(), null)
        canvas.restore()
        patch.recycle()
    }

    @Synchronized
    private fun label(bitmap: Bitmap, rect: Rect, label: String) {
        val canvas = Canvas(bitmap)
        canvas.drawRect(rect, black)
        if (label.isBlank()) return
        val text = label.uppercase()
        val pad = rect.height() * 0.18f
        ink.textSize = rect.height() * 0.56f
        val natural = ink.measureText(text)
        if (natural > rect.width() - pad * 2) ink.textSize *= (rect.width() - pad * 2) / natural
        if (ink.textSize < 5f) return
        val fm = ink.fontMetrics
        val y = rect.exactCenterY() - (fm.ascent + fm.descent) / 2
        canvas.drawText(text, rect.exactCenterX() - ink.measureText(text) / 2, y, ink)
    }

    private fun pixelate(bitmap: Bitmap, rect: Rect) {
        val block = max(6, (minOf(rect.width(), rect.height()) / 2.2f).roundToInt())
        val pixels = IntArray(rect.width() * rect.height())
        bitmap.getPixels(pixels, 0, rect.width(), rect.left, rect.top, rect.width(), rect.height())
        var by = 0
        while (by < rect.height()) {
            var bx = 0
            while (bx < rect.width()) {
                val w = minOf(block, rect.width() - bx)
                val h = minOf(block, rect.height() - by)
                var r = 0L
                var g = 0L
                var b = 0L
                for (y in by until by + h) for (x in bx until bx + w) {
                    val p = pixels[y * rect.width() + x]
                    r += Color.red(p)
                    g += Color.green(p)
                    b += Color.blue(p)
                }
                val n = w * h
                val c = Color.rgb(quantize(r / n), quantize(g / n), quantize(b / n))
                for (y in by until by + h) for (x in bx until bx + w) pixels[y * rect.width() + x] = c
                bx += block
            }
            by += block
        }
        bitmap.setPixels(pixels, 0, rect.width(), rect.left, rect.top, rect.width(), rect.height())
    }

    private fun quantize(v: Long): Int = ((v / 24) * 24 + 12).toInt().coerceIn(0, 255)

    private fun blur(bitmap: Bitmap, rect: Rect) {
        val pad = rect.height() / 3
        val outer = Rect(
            (rect.left - pad).coerceAtLeast(0),
            (rect.top - pad).coerceAtLeast(0),
            (rect.right + pad).coerceAtMost(bitmap.width),
            (rect.bottom + pad).coerceAtMost(bitmap.height),
        )
        val source = Bitmap.createBitmap(bitmap, outer.left, outer.top, outer.width(), outer.height())
        val factor = max(4f, rect.height() / 3f)
        val tinyW = max(1, (outer.width() / factor).roundToInt())
        val tinyH = max(1, (outer.height() / factor).roundToInt())
        var smudge = Bitmap.createScaledBitmap(source, tinyW, tinyH, true)
        repeat(2) {
            val up = Bitmap.createScaledBitmap(smudge, tinyW * 3, tinyH * 3, true)
            smudge = Bitmap.createScaledBitmap(up, tinyW, tinyH, true)
        }
        val wide = Bitmap.createScaledBitmap(smudge, outer.width(), outer.height(), true)
        Canvas(bitmap).drawBitmap(
            wide,
            Rect(rect.left - outer.left, rect.top - outer.top, rect.right - outer.left, rect.bottom - outer.top),
            rect,
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
    }
}
