package com.coverit.app.image

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
import kotlin.math.max

class FoundCode(val value: String, val area: RectF)

object CodeScanner {

    private val twoD = setOf(BarcodeFormat.QR_CODE, BarcodeFormat.DATA_MATRIX, BarcodeFormat.AZTEC, BarcodeFormat.PDF_417)

    fun scan(bitmap: Bitmap): List<FoundCode> {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val image = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels)))
        val hints = mapOf(DecodeHintType.TRY_HARDER to true)
        val results = runCatching { GenericMultipleBarcodeReader(MultiFormatReader()).decodeMultiple(image, hints) }
            .getOrNull().orEmpty()
        return results.mapNotNull { r ->
            val points = r.resultPoints?.filterNotNull().orEmpty()
            if (points.isEmpty()) return@mapNotNull null
            var left = points.minOf { it.x }
            var right = points.maxOf { it.x }
            var top = points.minOf { it.y }
            var bottom = points.maxOf { it.y }
            val size = max(right - left, bottom - top)
            if (r.barcodeFormat in twoD) {
                val pad = size * 0.22f
                left -= pad; right += pad; top -= pad; bottom += pad
            } else {
                val half = max(size * 0.35f, 24f)
                left -= size * 0.08f; right += size * 0.08f; top -= half; bottom += half
            }
            FoundCode(
                r.text.orEmpty(),
                RectF(left.coerceAtLeast(0f), top.coerceAtLeast(0f), right.coerceAtMost(bitmap.width.toFloat()), bottom.coerceAtMost(bitmap.height.toFloat())),
            )
        }
    }
}
