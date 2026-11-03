package com.coverit.app.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

object FaceFinder {

    private const val INPUT = 128
    private const val ANCHORS = 896
    private const val MIN_TILE = 96
    private const val OVERLAP = 0.3f

    private var context: Context? = null
    private val interpreter: Interpreter? by lazy {
        val context = context ?: return@lazy null
        runCatching {
            val fd = context.assets.openFd("models/face.tflite")
            val model = FileInputStream(fd.fileDescriptor).channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            Interpreter(model, Interpreter.Options().setNumThreads(2))
        }.getOrNull()
    }
    private val anchors: FloatArray = buildAnchors()

    fun install(context: Context) {
        this.context = context.applicationContext
    }

    private class Face(val box: RectF, val score: Float)

    @Synchronized
    fun find(bitmap: Bitmap): List<RectF> {
        val model = interpreter ?: return emptyList()
        val found = mutableListOf<Face>()
        val short = min(bitmap.width, bitmap.height)
        found += detect(model, bitmap, Rect(0, 0, bitmap.width, bitmap.height), 0.6f)
        for ((size, threshold) in listOf(short to 0.62f, short / 2 to 0.66f, short / 4 to 0.72f)) {
            if (size < MIN_TILE) continue
            val step = (size * (1 - OVERLAP)).toInt().coerceAtLeast(1)
            var y = 0
            while (true) {
                var x = 0
                while (true) {
                    val tile = Rect(x, y, min(x + size, bitmap.width), min(y + size, bitmap.height))
                    if (tile.width() >= MIN_TILE && tile.height() >= MIN_TILE) found += detect(model, bitmap, tile, threshold)
                    if (x + size >= bitmap.width) break
                    x = min(x + step, bitmap.width - size)
                }
                if (y + size >= bitmap.height) break
                y = min(y + step, bitmap.height - size)
            }
        }
        return merge(found).map { f -> grow(f.box, bitmap) }
    }

    private fun detect(model: Interpreter, bitmap: Bitmap, area: Rect, threshold: Float): List<Face> {
        val side = max(area.width(), area.height()).toFloat()
        val k = INPUT / side
        val input = Bitmap.createBitmap(INPUT, INPUT, Bitmap.Config.ARGB_8888)
        Canvas(input).apply {
            drawColor(Color.BLACK)
            drawBitmap(bitmap, area, RectF(0f, 0f, area.width() * k, area.height() * k), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        val pixels = IntArray(INPUT * INPUT)
        input.getPixels(pixels, 0, INPUT, 0, 0, INPUT, INPUT)
        input.recycle()
        val buffer = ByteBuffer.allocateDirect(INPUT * INPUT * 3 * 4).order(ByteOrder.nativeOrder())
        for (p in pixels) {
            buffer.putFloat(((p shr 16) and 0xFF) / 127.5f - 1f)
            buffer.putFloat(((p shr 8) and 0xFF) / 127.5f - 1f)
            buffer.putFloat((p and 0xFF) / 127.5f - 1f)
        }
        buffer.rewind()
        // The two outputs are told apart by shape: 16 numbers per anchor for boxes, 1 for scores.
        val first = model.getOutputTensor(0).shape().last()
        val boxes = Array(1) { Array(ANCHORS) { FloatArray(16) } }
        val scores = Array(1) { Array(ANCHORS) { FloatArray(1) } }
        val outputs = if (first == 16) mapOf(0 to boxes, 1 to scores) else mapOf(0 to scores, 1 to boxes)
        model.runForMultipleInputsOutputs(arrayOf(buffer), outputs)

        val out = mutableListOf<Face>()
        for (i in 0 until ANCHORS) {
            val score = 1f / (1f + exp(-scores[0][i][0].coerceIn(-100f, 100f)))
            if (score < threshold) continue
            val r = boxes[0][i]
            val cx = r[0] / INPUT + anchors[i * 2]
            val cy = r[1] / INPUT + anchors[i * 2 + 1]
            val w = r[2] / INPUT
            val h = r[3] / INPUT
            if (w <= 0f || h <= 0f) continue
            val box = RectF(
                area.left + (cx - w / 2) * INPUT / k,
                area.top + (cy - h / 2) * INPUT / k,
                area.left + (cx + w / 2) * INPUT / k,
                area.top + (cy + h / 2) * INPUT / k,
            )
            out += Face(box, score)
        }
        return out
    }

    private fun merge(faces: List<Face>): List<Face> {
        val kept = mutableListOf<Face>()
        for (f in faces.sortedByDescending { it.score }) {
            if (kept.none { overlap(it.box, f.box) > 0.3f || contains(it.box, f.box) }) kept += f
        }
        return kept
    }

    private fun overlap(a: RectF, b: RectF): Float {
        val w = min(a.right, b.right) - max(a.left, b.left)
        val h = min(a.bottom, b.bottom) - max(a.top, b.top)
        if (w <= 0f || h <= 0f) return 0f
        val inter = w * h
        return inter / (a.width() * a.height() + b.width() * b.height() - inter)
    }

    private fun contains(a: RectF, b: RectF): Boolean {
        val w = min(a.right, b.right) - max(a.left, b.left)
        val h = min(a.bottom, b.bottom) - max(a.top, b.top)
        return w > 0f && h > 0f && w * h > 0.6f * min(a.width() * a.height(), b.width() * b.height())
    }

    private fun grow(box: RectF, bitmap: Bitmap) = RectF(
        (box.left - box.width() * 0.18f).coerceAtLeast(0f),
        (box.top - box.height() * 0.35f).coerceAtLeast(0f),
        (box.right + box.width() * 0.18f).coerceAtMost(bitmap.width.toFloat()),
        (box.bottom + box.height() * 0.12f).coerceAtMost(bitmap.height.toFloat()),
    )

    private fun buildAnchors(): FloatArray {
        val out = FloatArray(ANCHORS * 2)
        var n = 0
        for ((grid, perCell) in listOf(16 to 2, 8 to 6)) {
            for (y in 0 until grid) {
                for (x in 0 until grid) {
                    repeat(perCell) {
                        out[n++] = (x + 0.5f) / grid
                        out[n++] = (y + 0.5f) / grid
                    }
                }
            }
        }
        return out
    }
}
