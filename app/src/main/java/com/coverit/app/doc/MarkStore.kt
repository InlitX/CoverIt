package com.coverit.app.doc

import com.coverit.app.detect.Kind
import com.coverit.app.pdf.Box
import com.coverit.app.redact.Style
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object MarkStore {

    class Saved(val marks: List<Mark>, val style: Style, val framing: Framing, val watermark: String?, val removed: Set<Int>)

    fun write(file: File, marks: List<Mark>, style: Style, framing: Framing = Framing(), watermark: String? = null, removed: Set<Int> = emptySet()) {
        val array = JSONArray()
        for (m in marks) {
            val boxes = JSONArray()
            m.boxes.forEach { b -> boxes.put(JSONArray().put(b.left.toDouble()).put(b.top.toDouble()).put(b.right.toDouble()).put(b.bottom.toDouble())) }
            array.put(
                JSONObject()
                    .put("id", m.id)
                    .put("page", m.page)
                    .put("kind", m.kind.name)
                    .put("value", m.value)
                    .put("checked", m.checked)
                    .put("boxes", boxes)
                    .apply {
                        m.stroke?.let { line ->
                            val pts = JSONArray()
                            line.points.forEach { pts.put(it.toDouble()) }
                            put("radius", line.radius.toDouble()).put("stroke", pts)
                        }
                    },
            )
        }
        val o = JSONObject().put("style", style.name).put("marks", array).put("turns", framing.turns)
        watermark?.let { o.put("watermark", it) }
        if (removed.isNotEmpty()) o.put("removed", JSONArray().apply { removed.sorted().forEach { put(it) } })
        framing.crop?.let { c -> o.put("crop", JSONArray().put(c.left.toDouble()).put(c.top.toDouble()).put(c.right.toDouble()).put(c.bottom.toDouble())) }
        file.writeText(o.toString())
    }

    fun read(file: File): Saved? {
        if (!file.exists()) return null
        return runCatching {
            val o = JSONObject(file.readText())
            val array = o.getJSONArray("marks")
            val marks = (0 until array.length()).map { i ->
                val m = array.getJSONObject(i)
                val boxes = m.getJSONArray("boxes")
                Mark(
                    m.getLong("id"),
                    m.getInt("page"),
                    Kind.valueOf(m.getString("kind")),
                    m.getString("value"),
                    (0 until boxes.length()).map { j ->
                        val b = boxes.getJSONArray(j)
                        Box(b.getDouble(0).toFloat(), b.getDouble(1).toFloat(), b.getDouble(2).toFloat(), b.getDouble(3).toFloat())
                    },
                    m.getBoolean("checked"),
                    m.optJSONArray("stroke")?.let { pts ->
                        com.coverit.app.redact.BrushLine(FloatArray(pts.length()) { k -> pts.getDouble(k).toFloat() }, m.getDouble("radius").toFloat())
                    },
                )
            }
            val crop = o.optJSONArray("crop")?.let { c -> Box(c.getDouble(0).toFloat(), c.getDouble(1).toFloat(), c.getDouble(2).toFloat(), c.getDouble(3).toFloat()) }
            val removed = o.optJSONArray("removed")?.let { r -> (0 until r.length()).map { r.getInt(it) }.toSet() }.orEmpty()
            Saved(marks, Style.valueOf(o.optString("style", Style.BLACK.name)), Framing(o.optInt("turns", 0), crop), o.optString("watermark").ifEmpty { null }, removed)
        }.getOrNull()
    }
}
