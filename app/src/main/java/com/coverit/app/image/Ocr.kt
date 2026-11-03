package com.coverit.app.image

import android.content.Context
import android.graphics.Bitmap
import com.coverit.app.pdf.Box
import com.coverit.app.pdf.PageText
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

object Ocr {

    private const val LANGUAGES = "eng+spa"
    private const val MAX_SIDE = 2400

    private const val MIN_CONFIDENCE = 30f

    private var context: Context? = null
    private var dataDir: File? = null
    private var api: TessBaseAPI? = null

    fun install(context: Context) {
        this.context = context.applicationContext
    }

    private fun prepare(context: Context) {
        val root = File(context.noBackupFilesDir, "ocr")
        val tessdata = File(root, "tessdata").apply { mkdirs() }
        for (lang in LANGUAGES.split('+')) {
            val target = File(tessdata, "$lang.traineddata")
            if (target.exists() && target.length() > 0) continue
            runCatching {
                val tmp = File(tessdata, "$lang.tmp")
                context.assets.open("tessdata/$lang.traineddata").use { input -> tmp.outputStream().use { input.copyTo(it) } }
                tmp.renameTo(target)
            }
        }
        dataDir = root
    }

    @Synchronized
    fun read(bitmap: Bitmap, index: Int = 0, scale: Float = 1f): PageText? {
        val engine = engine() ?: return null
        val side = maxOf(bitmap.width, bitmap.height)
        val probe = if (side > MAX_SIDE) Images.scaled(bitmap, bitmap.width * MAX_SIDE / side) else bitmap
        val k = bitmap.width.toFloat() / probe.width * scale
        return try {
            engine.setImage(probe)
            engine.getUTF8Text()
            val it = engine.resultIterator ?: return null
            val text = StringBuilder()
            val boxes = ArrayList<Box?>()
            var skipping = false
            try {
                it.begin()
                do {
                    if (it.isAtBeginningOf(TessBaseAPI.PageIteratorLevel.RIL_WORD)) {
                        skipping = it.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD) < MIN_CONFIDENCE
                        if (!skipping && text.isNotEmpty()) {
                            text.append(if (it.isAtBeginningOf(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)) '\n' else ' ')
                            boxes.add(null)
                        }
                    }
                    if (skipping) continue
                    val symbol = it.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_SYMBOL) ?: continue
                    val r = it.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_SYMBOL)
                    val box = Box(r.left * k, r.top * k, r.right * k, r.bottom * k)
                    for (c in symbol) {
                        text.append(c)
                        boxes.add(box)
                    }
                } while (it.next(TessBaseAPI.PageIteratorLevel.RIL_SYMBOL))
            } finally {
                it.delete()
            }
            if (text.none { c -> c.isLetterOrDigit() }) null else PageText(index, bitmap.width * scale, bitmap.height * scale, text.toString(), boxes)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { engine.clear() }
            if (probe !== bitmap) probe.recycle()
        }
    }

    private fun engine(): TessBaseAPI? {
        api?.let { return it }
        if (dataDir == null) context?.let { prepare(it) }
        val dir = dataDir ?: return null
        return runCatching {
            TessBaseAPI().also { t ->
                if (!t.init(dir.path, LANGUAGES, 1 /* LSTM only */)) {
                    t.recycle()
                    return null
                }
                t.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                t.setVariable("preserve_interword_spaces", "1")
            }
        }.getOrNull()?.also { api = it }
    }
}
