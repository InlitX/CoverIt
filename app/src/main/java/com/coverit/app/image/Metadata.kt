package com.coverit.app.image

import java.io.ByteArrayOutputStream

object Metadata {

    fun strip(bytes: ByteArray): ByteArray = when {
        isJpeg(bytes) -> runCatching { jpeg(bytes) }.getOrDefault(bytes)
        isPng(bytes) -> runCatching { png(bytes) }.getOrDefault(bytes)
        else -> bytes
    }

    private fun isJpeg(b: ByteArray) = b.size > 4 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte()

    private fun isPng(b: ByteArray) = b.size > 8 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'N'.code.toByte()

    private fun jpeg(b: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(b.size)
        out.write(b, 0, 2)
        var i = 2
        while (i + 4 <= b.size) {
            if (b[i] != 0xFF.toByte()) return b
            val marker = b[i + 1].toInt() and 0xFF
            if (marker == 0xDA) {
                out.write(b, i, b.size - i)
                return out.toByteArray()
            }
            val length = ((b[i + 2].toInt() and 0xFF) shl 8) or (b[i + 3].toInt() and 0xFF)
            val end = i + 2 + length
            if (length < 2 || end > b.size) return b
            if (marker != 0xE1 && marker != 0xED && marker != 0xFE) out.write(b, i, end - i)
            i = end
        }
        return b
    }

    private val PNG_DROP = setOf("eXIf", "tEXt", "iTXt", "zTXt", "tIME")

    private fun png(b: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(b.size)
        out.write(b, 0, 8)
        var i = 8
        while (i + 12 <= b.size) {
            val length = ((b[i].toInt() and 0xFF) shl 24) or ((b[i + 1].toInt() and 0xFF) shl 16) or
                ((b[i + 2].toInt() and 0xFF) shl 8) or (b[i + 3].toInt() and 0xFF)
            val end = i + 12 + length
            if (length < 0 || end > b.size) return b
            val type = String(b, i + 4, 4, Charsets.US_ASCII)
            if (type !in PNG_DROP) out.write(b, i, end - i)
            i = end
            if (type == "IEND") break
        }
        return out.toByteArray()
    }
}
