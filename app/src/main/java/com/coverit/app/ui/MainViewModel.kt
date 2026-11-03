package com.coverit.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coverit.app.R
import com.coverit.app.data.Covered
import com.coverit.app.data.Library
import com.coverit.app.data.Prefs
import com.coverit.app.data.Record
import com.coverit.app.data.Space
import com.coverit.app.detect.Kind
import com.coverit.app.doc.DetectionConfig
import com.coverit.app.doc.DocType
import com.coverit.app.doc.Exporter
import com.coverit.app.doc.Family
import com.coverit.app.doc.Framing
import com.coverit.app.doc.ImageSource
import com.coverit.app.doc.Loader
import com.coverit.app.doc.MarkStore
import com.coverit.app.doc.Masking
import com.coverit.app.doc.MetaReader
import com.coverit.app.doc.Report
import com.coverit.app.doc.Session
import com.coverit.app.doc.Unsupported
import com.coverit.app.ui.common.tag
import com.coverit.app.ui.editor.PagePreviews
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = Prefs(app)
    val library = Library(app)

    val stack = mutableStateListOf<Route>(if (prefs.onboarded) Route.Home else Route.Onboarding)
    val route get() = stack.last()

    init {
        if (prefs.space !in library.spaces) prefs.updateSpace(Space.PERSONAL)
        AppLanguage.system.country.takeIf { it.length == 2 }?.let { com.coverit.app.detect.Detector.region = it.uppercase() }
    }

    var tab by mutableStateOf(Tab.HOME)
    var folder by mutableStateOf<String?>(null)
    var session by mutableStateOf<Session?>(null)
        private set

    private val work get() = File(getApplication<Application>().cacheDir, "work")

    fun push(route: Route) {
        stack.add(route)
    }

    fun back() {
        when (route) {
            is Route.Working -> return
            Route.Editor -> closeSession()
            is Route.Result, is Route.Cleaned -> {
                home()
                return
            }
            else -> Unit
        }
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun home() {
        closeSession()
        stack.clear()
        stack.add(Route.Home)
    }

    fun finishOnboarding() {
        prefs.updateOnboarded(true)
        stack.clear()
        stack.add(Route.Home)
    }

    fun replayOnboarding() {
        push(Route.Onboarding)
    }

    fun config() = DetectionConfig(prefs.enabledKinds, prefs.rules.toList())

    fun open(uri: Uri) {
        if (route is Route.Working) return
        closeSession()
        push(Route.Working(R.string.working_reading))
        viewModelScope.launch {
            val loaded = runCatching { withContext(Dispatchers.IO) { load(uri) } }
            replaceTop(
                loaded.fold(
                    onSuccess = {
                        session = it
                        Route.Editor
                    },
                    onFailure = { Route.Failed(if (it is Unsupported) R.string.failed_format else R.string.failed_open) },
                ),
            )
        }
    }

    fun reopen(record: Record) {
        val original = library.original(record) ?: return
        if (route is Route.Working) return
        closeSession()
        push(Route.Working(R.string.working_reading))
        viewModelScope.launch {
            val loaded = runCatching {
                withContext(Dispatchers.IO) {
                    val dir = File(work, UUID.randomUUID().toString()).apply { mkdirs() }
                    val file = original.copyTo(File(dir, original.name), overwrite = true)
                    val fresh = Loader.load(file, record.type, record.name, config(), prefs.style, record.id)
                    MarkStore.read(library.marks(record))?.let { saved ->
                        fresh.restoreFraming(saved.framing)
                        fresh.replaceAll(saved.marks)
                        if (record.type.family != Family.OFFICE) fresh.restoreStyle(saved.style)
                        fresh.restoreFinish(saved.watermark, saved.removed)
                    }
                    fresh
                }
            }
            replaceTop(
                loaded.fold(
                    onSuccess = {
                        session = it
                        Route.Editor
                    },
                    onFailure = { Route.Failed(R.string.failed_open) },
                ),
            )
        }
    }

    fun cleanHidden(uris: List<Uri>) {
        if (uris.isEmpty() || route is Route.Working) return
        closeSession()
        push(Route.Working(R.string.working_cleaning))
        viewModelScope.launch {
            val done = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    runCatching {
                        val s = load(uri, DetectionConfig.NONE)
                        try {
                            val target = SaveOptions(s.name, prefs.space, folder?.takeIf { library.folder(it)?.space == prefs.space }, keepOriginal = false)
                            save(s, target, DetectionConfig.NONE)
                        } finally {
                            s.source.close()
                        }
                    }
                }
            }
            closeSession()
            val saved = done.mapNotNull { it.getOrNull() }
            when {
                saved.isEmpty() -> replaceTop(Route.Failed(if (done.first().exceptionOrNull() is Unsupported) R.string.failed_format else R.string.failed_open))
                saved.size == 1 && uris.size == 1 -> {
                    stack.clear()
                    stack.add(Route.Home)
                    stack.add(Route.Result(saved[0].first.id, saved[0].second, hiddenOnly = true))
                }
                else -> {
                    stack.clear()
                    stack.add(Route.Home)
                    stack.add(Route.Cleaned(saved.map { (record, report) -> Route.Cleaned.Item(record.id, report.before.size, report.leftovers) }, uris.size - saved.size))
                }
            }
        }
    }

    class SaveOptions(val name: String, val space: Space, val folder: String?, val keepOriginal: Boolean)

    fun saveOptions(s: Session): SaveOptions {
        val existing = s.recordId?.let { library.find(it) }
        return if (existing != null) {
            SaveOptions(existing.name, existing.space, existing.folder, existing.hasOriginal)
        } else {
            SaveOptions(s.name, prefs.space, folder?.takeIf { library.folder(it)?.space == prefs.space }, prefs.keepOriginal)
        }
    }

    fun export(options: SaveOptions) {
        val s = session ?: return
        push(Route.Working(R.string.working_covering))
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { save(s, options) } }
            stack.removeAt(stack.lastIndex)
            result.fold(
                onSuccess = { (record, report) ->
                    closeSession()
                    stack.clear()
                    stack.add(Route.Home)
                    stack.add(Route.Result(record.id, report))
                },
                onFailure = { push(Route.Failed(R.string.failed_export)) },
            )
        }
    }

    fun reframe(framing: Framing, done: () -> Unit = {}) {
        val s = session ?: return
        val photo = s.source as? ImageSource ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    val b = photo.build(framing)
                    b to Loader.scanImage(b, config())
                }
            }.onSuccess { (built, scan) ->
                if (session === s) s.reframe(framing, built, scan.marks, scan.text)
            }
            done()
        }
    }

    private val pages = object : android.util.LruCache<String, Bitmap>(64 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    private fun pageKey(page: Int, widthPx: Int): String? {
        val s = session ?: return null
        val version = s.source.pages.getOrNull(page)?.version ?: return null
        return "${System.identityHashCode(s)}:$page:$version:$widthPx"
    }

    fun cachedPage(page: Int, widthPx: Int): Bitmap? = pageKey(page, widthPx)?.let { pages.get(it) }

    suspend fun render(page: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = pageKey(page, widthPx)
        key?.let { pages.get(it) }?.let { return@withContext it }
        runCatching { session?.source?.render(page, widthPx) }.getOrNull()?.also { b -> key?.let { pages.put(it, b) } }
    }

    class Undo(val message: Int, val count: Int, val revert: () -> Unit, val commit: () -> Unit)

    var undo by mutableStateOf<Undo?>(null)
        private set

    val selected = mutableStateListOf<String>()

    fun toggleSelected(id: String) {
        if (id in selected) selected.remove(id) else selected.add(id)
    }

    fun clearSelection() = selected.clear()

    fun undoLast() {
        undo?.revert?.invoke()
        undo = null
    }

    fun settle(which: Undo) {
        if (undo !== which) return
        which.commit()
        undo = null
    }

    private fun offer(next: Undo) {
        undo?.commit?.invoke()
        undo = next
    }

    fun deleteRecords(ids: Collection<String>) {
        val set = ids.toSet()
        if (set.isEmpty()) return
        val removed = library.remove(set)
        selected.removeAll(set)
        if ((route as? Route.Detail)?.recordId in set) back()
        offer(Undo(R.plurals.deleted_n, removed.records.size, { library.restore(removed) }, { library.purge(removed) }))
    }

    fun starRecords(ids: Collection<String>, starred: Boolean) {
        ids.forEach { library.star(it, starred) }
    }

    fun moveRecords(ids: Collection<String>, space: Space, folder: String?) {
        ids.forEach { library.move(it, space, folder) }
    }

    fun switchSpace(space: Space) {
        prefs.updateSpace(space)
        folder = null
    }

    fun deleteSpace(space: Space) {
        library.deleteSpace(space)
        if (prefs.space == space) switchSpace(Space.PERSONAL)
    }

    fun deleteFolder(id: String) {
        library.deleteFolder(id)
        if (folder == id) folder = null
    }

    fun wipe() {
        undo?.commit?.invoke()
        undo = null
        selected.clear()
        library.wipe()
        folder = null
    }

    private fun replaceTop(route: Route) {
        stack[stack.lastIndex] = route
    }

    private fun load(uri: Uri, config: DetectionConfig = config()): Session {
        val context = getApplication<Application>()
        val resolver = context.contentResolver
        val name = displayName(uri)
        val type = DocType.of(resolver.getType(uri), name)
        val dir = File(work, UUID.randomUUID().toString()).apply { mkdirs() }
        val ext = name.substringAfterLast('.', "").lowercase().takeIf { it.length in 2..5 && it.all(Char::isLetterOrDigit) }
        val file = File(dir, "source.${ext ?: type.extension}")
        resolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
        return Loader.load(file, type, name, config, prefs.style)
    }

    private fun save(s: Session, options: SaveOptions, config: DetectionConfig = config()): Pair<Record, Report> {
        val out = File(work, "out").apply { deleteRecursively(); mkdirs() }
        val target = File(out, "clean.${s.source.type.extension}")
        val before = MetaReader.read(s.source.file, s.source.type)
        val app = getApplication<Application>()
        val words = AppLanguage.localized(app, AppLanguage.read(app)).resources
        val report = Exporter.export(s, target, config) { words.getString(it.tag) }.also { it.before = before }
        val chosen = s.chosen
        val existing = s.recordId?.let { library.find(it) }
        val record = Record(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = withExtension(options.name, s.name),
            type = s.source.type,
            created = existing?.created ?: System.currentTimeMillis(),
            folder = options.folder?.takeIf { library.folder(it)?.space == options.space },
            space = options.space,
            pages = s.source.pages.size - s.removed.size,
            hasOriginal = options.keepOriginal,
            verified = report.clean,
            covered = chosen.map { Covered(it.kind, if (it.kind == Kind.MANUAL || it.kind == Kind.FACE) "" else Masking.mask(it.value)) },
            starred = existing?.starred ?: false,
        )
        val original = if (options.keepOriginal) s.source.file else null
        if (existing != null) library.replace(record, target, original) else library.add(record, target, original)
        if (original != null) MarkStore.write(library.marks(record), s.marks.toList(), s.style, (s.source as? ImageSource)?.framing ?: Framing(), s.watermark, s.removed)
        return library.find(record.id)!! to report
    }

    private fun withExtension(typed: String, current: String): String {
        val name = typed.trim().ifBlank { return current }
        val ext = current.substringAfterLast('.', "")
        if (ext.isEmpty() || name.endsWith(".$ext", ignoreCase = true)) return name
        return "$name.$ext"
    }

    private fun closeSession() {
        pages.evictAll()
        PagePreviews.clear()
        session?.source?.close()
        session = null
        work.listFiles()?.filter { it.isDirectory && it.name != "out" }?.forEach { it.deleteRecursively() }
    }

    private fun displayName(uri: Uri): String {
        val resolver = getApplication<Application>().contentResolver
        val fromQuery = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull()
        return fromQuery ?: uri.lastPathSegment?.substringAfterLast('/') ?: "document"
    }

    override fun onCleared() {
        undo?.commit?.invoke()
        closeSession()
    }
}

fun exportName(record: Record): String {
    val base = record.name.substringBeforeLast('.').ifBlank { "document" }
    return "$base (CoverIt).${record.type.extension}"
}
