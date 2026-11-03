package com.coverit.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coverit.app.data.Library
import com.coverit.app.data.Prefs
import com.coverit.app.data.Space
import com.coverit.app.data.ThemeMode
import com.coverit.app.redact.Style
import com.coverit.app.ui.AppLanguage
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.Route
import com.coverit.app.ui.Tab
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarketingShots {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val inst = InstrumentationRegistry.getInstrumentation()
    private val app = inst.targetContext
    private lateinit var vm: MainViewModel
    private lateinit var words: android.content.res.Resources
    private lateinit var out: File

    @Test
    fun english() = shots("en", AppLanguage.EN, Names("lab-results.pdf", "rental-agreement.pdf", "payslip.pdf", "boarding-pass", listOf("Medical", "Home")))

    private class Names(val lab: String, val rent: String, val pay: String, val photo: String, val folders: List<String>)

    private fun shots(lang: String, language: AppLanguage, n: Names) {
        AppLanguage.write(app, language)
        Prefs(app).apply {
            updateOnboarded(true)
            updateStyle(Style.BLACK)
            updateKeepOriginal(true)
            updateTheme(ThemeMode.DARK)
            updateSpace(Space.PERSONAL)
        }
        Library(app).apply {
            wipe()
            folders.toList().forEach { deleteFolder(it.id) }
        }
        out = File(app.getExternalFilesDir(null), "shots/$lang").apply { deleteRecursively(); mkdirs() }
        words = AppLanguage.localized(app, language).resources
        val dir = File(app.cacheDir, "demo").apply { deleteRecursively(); mkdirs() }
        fun asset(name: String) = File(dir, name).also { f -> inst.context.assets.open("demo/$lang/$name").use { i -> f.outputStream().use { i.copyTo(it) } } }
        val lab = asset(n.lab)
        val rent = asset(n.rent)
        val pay = asset(n.pay)
        val photo = photoWithExif(asset("${n.photo}.png"), File(dir, "${n.photo}.jpg"))

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { vm = ViewModelProvider(it)[MainViewModel::class.java] }
            val medical = main { vm.library.addFolder(n.folders[0], Space.PERSONAL) }!!
            val home = main { vm.library.addFolder(n.folders[1], Space.PERSONAL) }!!

            open(rent)
            save(Space.PERSONAL, home.id)
            open(pay)
            save(Space.WORK, null)

            open(photo)
            main { vm.session!!.changeStyle(Style.PIXELATE) }
            list(2)
            shot("04-photo")
            list(0)
            click(R.string.meta_title)
            shot("07-hidden")
            inst.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            pause(800)
            save(Space.PERSONAL, null)

            open(lab)
            main { vm.session!!.changeStyle(Style.LABEL) }
            list(1)
            shot("02-found")
            list(2)
            shot("03-page")
            click(R.string.editor_zoom_short)
            compose.onAllNodes(isRoot())[0].performTouchInput { doubleClick(Offset(width * 0.24f, height * 0.38f)) }
            pause(1500)
            shot("03-zoom")
            inst.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            pause(800)
            save(Space.PERSONAL, medical.id)

            main {
                vm.home()
                vm.tab = Tab.HOME
            }
            shot("01-home")
            main { vm.tab = Tab.LIBRARY }
            shot("06-library")

            val record = vm.library.records.first { it.name == n.lab }
            main {
                vm.tab = Tab.HOME
                vm.push(Route.Detail(record.id))
            }
            pause(1200)
            click(R.string.docs_compare)
            shot("08-before-after")
        }
    }

    private fun <T> main(block: () -> T): T {
        var result: T? = null
        inst.runOnMainSync { result = block() }
        compose.waitForIdle()
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun pause(ms: Long) {
        Thread.sleep(ms)
        compose.waitForIdle()
    }

    private fun open(file: File) {
        main { vm.open(Uri.fromFile(file)) }
        compose.waitUntil(120_000) { vm.route == Route.Editor && vm.session != null }
        pause(1500)
    }

    private fun save(space: Space, folder: String?) {
        main {
            val s = vm.session!!
            vm.export(MainViewModel.SaveOptions(s.name, space, folder, true))
        }
        compose.waitUntil(120_000) { vm.route is Route.Result }
        pause(2500)
    }

    private fun list(index: Int) {
        compose.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(index)
        pause(1500)
    }

    private fun click(text: Int) {
        compose.onAllNodesWithText(words.getString(text))[0].performClick()
        pause(1500)
    }

    private fun shot(name: String) {
        pause(1200)
        val bitmap = inst.uiAutomation.takeScreenshot()
        File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun photoWithExif(png: File, jpg: File): File {
        val bitmap = BitmapFactory.decodeFile(png.path)
        jpg.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        ExifInterface(jpg).apply {
            setAttribute(ExifInterface.TAG_MAKE, "Google")
            setAttribute(ExifInterface.TAG_MODEL, "Pixel 8")
            setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:10:14 07:12:31")
            setAttribute(ExifInterface.TAG_DATETIME, "2026:10:14 07:12:31")
            setAttribute(ExifInterface.TAG_SOFTWARE, "HDR+ 1.0")
            setLatLong(38.7742, -9.1342)
            saveAttributes()
        }
        return jpg
    }
}
