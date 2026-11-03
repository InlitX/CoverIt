package com.coverit.app.doc

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coverit.app.detect.Kind
import com.coverit.app.redact.Style
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SessionTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun session(): Session {
        val file = File(context.cacheDir, "nota.txt").apply {
            writeText("Paciente: Ana Pérez García\nEmail ana.perez@correo.es\nCódigo interno AB-1234 y AB-9876.\n")
        }
        val config = DetectionConfig(Kind.detectable.toSet(), emptyList())
        return Loader.load(file, DocType.TXT, "nota.txt", config, Style.BLACK)
    }

    @Test
    fun undoAndRedoEveryKindOfChange() {
        val s = session()
        val start = s.marks.map { it.value to it.checked }
        assertFalse(s.edited)

        val email = s.marks.first { it.kind == Kind.EMAIL }
        s.toggle(email, false)
        val codes = Regex("""AB-\d{4}""")
        val uncovered = s.countUncovered(codes)
        assertTrue(uncovered > 0)
        assertEquals(uncovered, s.coverAll(codes, Kind.CUSTOM))
        assertFalse(email.checked)
        val customs = s.marks.count { it.kind == Kind.CUSTOM }
        s.setKind(setOf(Kind.NAME), false)
        assertTrue(s.edited)

        s.undo()
        assertTrue(s.marks.first { it.kind == Kind.NAME }.checked)
        s.undo()
        assertEquals(0, s.marks.count { it.kind == Kind.CUSTOM })
        s.undo()
        assertEquals(start, s.marks.map { it.value to it.checked })
        assertFalse(s.canUndo)

        s.redo()
        s.redo()
        assertEquals(customs, s.marks.count { it.kind == Kind.CUSTOM })
        assertTrue(s.marks.any { it === email })
        assertFalse(email.checked)
    }

    @Test
    fun aDragIsOneStep() {
        val s = session()
        val mark = s.marks.first()
        val before = mark.boxes
        s.beginReshape()
        repeat(5) { i -> s.reshape(mark, 0, before[0].copy(right = before[0].right + i * 10f)) }
        s.undo()
        assertEquals(before, mark.boxes)
        assertFalse(s.canUndo)
    }

    @Test
    fun marksSurviveSavingForLater() {
        val s = session()
        s.coverAll(Regex("AB-1234"), Kind.CUSTOM)
        s.toggle(s.marks.first { it.kind == Kind.EMAIL }, false)
        val file = File(context.cacheDir, "marks.json")
        MarkStore.write(file, s.marks, Style.PIXELATE)
        val saved = MarkStore.read(file)!!
        assertEquals(Style.PIXELATE, saved.style)
        assertEquals(s.marks.map { Triple(it.kind, it.value, it.checked) }, saved.marks.map { Triple(it.kind, it.value, it.checked) })
        assertEquals(s.marks.map { it.boxes }, saved.marks.map { it.boxes })
    }
}
