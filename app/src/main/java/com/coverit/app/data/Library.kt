package com.coverit.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.coverit.app.detect.Kind
import com.coverit.app.doc.DocType
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class Covered(val kind: Kind, val masked: String)

data class Space(val id: String) {
    val builtIn get() = this in builtIns

    companion object {
        val WORK = Space("WORK")
        val PERSONAL = Space("PERSONAL")
        val STUDY = Space("STUDY")
        val builtIns = listOf(WORK, PERSONAL, STUDY)
    }
}

data class SpaceInfo(val space: Space, val name: String, val icon: String, val created: Long)

val SPACE_ICONS = listOf("folder", "heart", "house", "bank", "health", "car", "star", "people")

data class Folder(val id: String, val name: String, val space: Space, val created: Long)

data class Record(
    val id: String,
    val name: String,
    val type: DocType,
    val created: Long,
    val folder: String?,
    val pages: Int,
    val hasOriginal: Boolean,
    val verified: Boolean,
    val covered: List<Covered>,
    val space: Space = Space.PERSONAL,
    val starred: Boolean = false,
)

class Library(context: Context) {

    private val root = File(context.filesDir, "docs").apply { mkdirs() }
    private val index = File(context.filesDir, "library.json")
    private val folderIndex = File(context.filesDir, "folders.json")
    private val spaceIndex = File(context.filesDir, "spaces.json")

    val customSpaces = mutableStateListOf<SpaceInfo>().apply { addAll(readSpaces()) }

    val spaces get() = Space.builtIns + customSpaces.map { it.space }

    val folders = mutableStateListOf<Folder>().apply { addAll(readFolders()) }
    val records = mutableStateListOf<Record>()

    init {
        val (list, migrated) = read()
        records.addAll(list)
        if (migrated) {
            writeFolders()
            write()
        }
        val known = records.map { it.id }.toSet()
        root.listFiles()?.filter { it.isDirectory && it.name !in known }?.forEach { it.deleteRecursively() }
    }

    fun dir(id: String) = File(root, id)

    fun clean(record: Record) = File(dir(record.id), "clean.${record.type.extension}")

    fun original(record: Record) = File(dir(record.id), "original.${record.type.extension}").takeIf { it.exists() }

    fun marks(record: Record) = File(dir(record.id), "marks.json")

    fun replace(record: Record, clean: File, original: File?) {
        val dir = dir(record.id).apply { mkdirs() }
        clean.copyTo(File(dir, "clean.${record.type.extension}"), overwrite = true)
        val kept = File(dir, "original.${record.type.extension}")
        if (original != null) {
            if (original.canonicalPath != kept.canonicalPath) original.copyTo(kept, overwrite = true)
        } else {
            kept.delete()
            File(dir, "marks.json").delete()
        }
        val i = records.indexOfFirst { it.id == record.id }
        val updated = record.copy(hasOriginal = original != null)
        if (i < 0) records.add(0, updated) else records[i] = updated
        write()
    }

    fun add(record: Record, clean: File, original: File?) {
        val dir = dir(record.id).apply { mkdirs() }
        clean.copyTo(File(dir, "clean.${record.type.extension}"), overwrite = true)
        original?.copyTo(File(dir, "original.${record.type.extension}"), overwrite = true)
        records.add(0, record.copy(hasOriginal = original != null))
        write()
    }

    fun find(id: String) = records.firstOrNull { it.id == id }

    fun folder(id: String?) = folders.firstOrNull { it.id == id }

    fun foldersIn(space: Space) = folders.filter { it.space == space }

    fun recordsIn(space: Space, folder: String?) = records.filter { it.space == space && it.folder == folder }

    fun move(id: String, space: Space, folder: String?) = update(id) { it.copy(space = space, folder = folder) }

    fun rename(id: String, name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) update(id) { it.copy(name = clean) }
    }

    fun star(id: String, starred: Boolean) = update(id) { it.copy(starred = starred) }

    class Removed(val records: List<Pair<Int, Record>>)

    fun remove(ids: Collection<String>): Removed {
        val gone = records.withIndex().filter { it.value.id in ids }.map { it.index to it.value }
        records.removeAll { it.id in ids }
        write()
        return Removed(gone)
    }

    fun restore(removed: Removed) {
        for ((i, r) in removed.records.sortedBy { it.first }) {
            if (records.none { it.id == r.id }) records.add(i.coerceAtMost(records.size), r)
        }
        write()
    }

    fun purge(removed: Removed) {
        removed.records.forEach { (_, r) -> if (records.none { it.id == r.id }) dir(r.id).deleteRecursively() }
    }

    fun spaceInfo(space: Space) = customSpaces.firstOrNull { it.space == space }

    fun addSpace(name: String, icon: String): Space? {
        val clean = name.trim()
        if (clean.isEmpty() || customSpaces.any { it.name.equals(clean, ignoreCase = true) }) return null
        val info = SpaceInfo(Space(UUID.randomUUID().toString()), clean, icon, System.currentTimeMillis())
        customSpaces += info
        writeSpaces()
        return info.space
    }

    fun updateSpace(space: Space, name: String, icon: String): Boolean {
        val i = customSpaces.indexOfFirst { it.space == space }
        val clean = name.trim()
        if (i < 0 || clean.isEmpty()) return false
        if (customSpaces.any { it.space != space && it.name.equals(clean, ignoreCase = true) }) return false
        customSpaces[i] = customSpaces[i].copy(name = clean, icon = icon)
        writeSpaces()
        return true
    }

    fun deleteSpace(space: Space) {
        if (space.builtIn) return
        customSpaces.removeAll { it.space == space }
        folders.indices.filter { folders[it].space == space }.forEach { i ->
            val f = folders[i]
            val taken = folders.any { it.space == Space.PERSONAL && it.name.equals(f.name, ignoreCase = true) }
            folders[i] = f.copy(space = Space.PERSONAL, name = if (taken) "${f.name} (2)" else f.name)
        }
        records.indices.filter { records[it].space == space }.forEach { records[it] = records[it].copy(space = Space.PERSONAL) }
        writeSpaces()
        writeFolders()
        write()
    }

    fun addFolder(name: String, space: Space): Folder? {
        val clean = name.trim()
        if (clean.isEmpty() || foldersIn(space).any { it.name.equals(clean, ignoreCase = true) }) return null
        return Folder(UUID.randomUUID().toString(), clean, space, System.currentTimeMillis()).also {
            folders += it
            writeFolders()
        }
    }

    fun renameFolder(id: String, name: String): Boolean {
        val i = folders.indexOfFirst { it.id == id }
        val clean = name.trim()
        if (i < 0 || clean.isEmpty()) return false
        if (foldersIn(folders[i].space).any { it.id != id && it.name.equals(clean, ignoreCase = true) }) return false
        folders[i] = folders[i].copy(name = clean)
        writeFolders()
        return true
    }

    fun deleteFolder(id: String) {
        folders.removeAll { it.id == id }
        records.indices.filter { records[it].folder == id }.forEach { records[it] = records[it].copy(folder = null) }
        writeFolders()
        write()
    }

    fun wipe() {
        root.deleteRecursively()
        root.mkdirs()
        records.clear()
        folders.clear()
        customSpaces.clear()
        writeSpaces()
        write()
        writeFolders()
    }

    private fun update(id: String, change: (Record) -> Record) {
        val i = records.indexOfFirst { it.id == id }
        if (i < 0) return
        records[i] = change(records[i])
        write()
    }

    private fun read(): Pair<List<Record>, Boolean> {
        if (!index.exists()) return emptyList<Record>() to false
        val array = runCatching { JSONArray(index.readText()) }.getOrNull() ?: return emptyList<Record>() to false
        val list = (0 until array.length()).mapNotNull { runCatching { fromJson(array.getJSONObject(it)) }.getOrNull() }
            .filter { dir(it.id).exists() }
        var migrated = false
        val fixed = list.map { r ->
            if (r.folder == null || folders.any { it.id == r.folder }) return@map r
            migrated = true
            val folder = folders.firstOrNull { it.space == r.space && it.name == r.folder }
                ?: Folder(UUID.randomUUID().toString(), r.folder, r.space, r.created).also { folders += it }
            r.copy(folder = folder.id)
        }
        return fixed to migrated
    }

    private fun write() {
        val array = JSONArray()
        records.forEach { array.put(toJson(it)) }
        atomic(index, array.toString())
    }

    private fun readFolders(): List<Folder> {
        if (!folderIndex.exists()) return emptyList()
        val array = runCatching { JSONArray(folderIndex.readText()) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                Folder(o.getString("id"), o.getString("name"), known(Space(o.getString("space"))), o.getLong("created"))
            }.getOrNull()
        }
    }

    private fun known(space: Space) = if (space in spaces) space else Space.PERSONAL

    private fun readSpaces(): List<SpaceInfo> {
        if (!spaceIndex.exists()) return emptyList()
        val array = runCatching { JSONArray(spaceIndex.readText()) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                SpaceInfo(Space(o.getString("id")), o.getString("name"), o.optString("icon", "folder"), o.getLong("created"))
            }.getOrNull()
        }
    }

    private fun writeSpaces() {
        val array = JSONArray()
        customSpaces.forEach { array.put(JSONObject().put("id", it.space.id).put("name", it.name).put("icon", it.icon).put("created", it.created)) }
        atomic(spaceIndex, array.toString())
    }

    private fun writeFolders() {
        val array = JSONArray()
        folders.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("space", it.space.id).put("created", it.created)) }
        atomic(folderIndex, array.toString())
    }

    private fun atomic(file: File, text: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        tmp.renameTo(file)
    }

    private fun toJson(r: Record) = JSONObject().apply {
        put("id", r.id)
        put("name", r.name)
        put("type", r.type.name)
        put("created", r.created)
        put("folder", r.folder ?: JSONObject.NULL)
        put("pages", r.pages)
        put("hasOriginal", r.hasOriginal)
        put("verified", r.verified)
        put("space", r.space.id)
        put("starred", r.starred)
        put("covered", JSONArray().apply {
            r.covered.forEach { put(JSONObject().put("kind", it.kind.name).put("masked", it.masked)) }
        })
    }

    private fun fromJson(o: JSONObject): Record {
        val covered = o.getJSONArray("covered")
        return Record(
            id = o.getString("id"),
            name = o.getString("name"),
            type = DocType.valueOf(o.getString("type")),
            created = o.getLong("created"),
            folder = if (o.isNull("folder")) null else o.getString("folder"),
            pages = o.getInt("pages"),
            hasOriginal = o.getBoolean("hasOriginal"),
            verified = o.getBoolean("verified"),
            covered = (0 until covered.length()).map {
                val c = covered.getJSONObject(it)
                Covered(Kind.valueOf(c.getString("kind")), c.getString("masked"))
            },
            space = known(Space(o.optString("space", Space.PERSONAL.id))),
            starred = o.optBoolean("starred", false),
        )
    }
}
