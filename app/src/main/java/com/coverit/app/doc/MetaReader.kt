package com.coverit.app.doc

import androidx.exifinterface.media.ExifInterface
import com.coverit.app.office.OfficeDoc
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale

enum class MetaKind { LOCATION, DEVICE, DATE, AUTHOR, COMPANY, SOFTWARE, SERIAL, DESCRIPTION, COMMENTS, REVISIONS, THUMBNAIL, HIDDEN, FORMS, SCRIPTS, ATTACHMENTS, NOTES, OTHER }

class MetaField(val kind: MetaKind, val value: String)

object MetaReader {

    fun read(file: File, type: DocType): List<MetaField> = runCatching {
        when (type.family) {
            Family.IMAGE -> image(file)
            Family.PDF -> pdf(file)
            Family.OFFICE -> office(file, type)
        }
    }.getOrDefault(emptyList())

    private fun image(file: File): List<MetaField> = buildList {
        val exif = ExifInterface(file)
        fun tag(kind: MetaKind, vararg tags: String) {
            val value = tags.mapNotNull { exif.getAttribute(it)?.trim()?.takeIf { v -> v.isNotEmpty() && v.any(Char::isLetterOrDigit) } }.distinct().joinToString(" · ")
            if (value.isNotEmpty()) add(MetaField(kind, value))
        }
        exif.latLong?.let { (lat, lon) ->
            add(MetaField(MetaKind.LOCATION, String.format(Locale.ROOT, "%.5f, %.5f", lat, lon)))
        }
        val camera = listOfNotNull(exif.getAttribute(ExifInterface.TAG_MAKE), exif.getAttribute(ExifInterface.TAG_MODEL))
            .map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (camera.isNotEmpty()) add(MetaField(MetaKind.DEVICE, camera.joinToString(" ")))
        tag(MetaKind.DEVICE, ExifInterface.TAG_LENS_MAKE, ExifInterface.TAG_LENS_MODEL)
        tag(MetaKind.DATE, ExifInterface.TAG_DATETIME_ORIGINAL)
        if (exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) == null) tag(MetaKind.DATE, ExifInterface.TAG_DATETIME)
        tag(MetaKind.AUTHOR, ExifInterface.TAG_ARTIST, ExifInterface.TAG_CAMERA_OWNER_NAME, ExifInterface.TAG_COPYRIGHT)
        tag(MetaKind.SOFTWARE, ExifInterface.TAG_SOFTWARE)
        tag(MetaKind.SERIAL, ExifInterface.TAG_BODY_SERIAL_NUMBER, ExifInterface.TAG_LENS_SERIAL_NUMBER, ExifInterface.TAG_IMAGE_UNIQUE_ID)
        tag(MetaKind.DESCRIPTION, ExifInterface.TAG_IMAGE_DESCRIPTION, ExifInterface.TAG_USER_COMMENT)
        if (exif.getAttributeBytes(ExifInterface.TAG_XMP)?.isNotEmpty() == true) add(MetaField(MetaKind.HIDDEN, "XMP"))
        if (exif.hasThumbnail()) add(MetaField(MetaKind.THUMBNAIL, ""))
    }

    private fun pdf(file: File): List<MetaField> = PDDocument.load(file).use { doc ->
        buildList {
            val info = doc.documentInformation
            fun text(kind: MetaKind, value: String?) {
                value?.trim()?.takeIf { it.isNotEmpty() }?.let { add(MetaField(kind, it)) }
            }
            fun date(value: Calendar?) {
                value?.let { add(MetaField(MetaKind.DATE, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it.time))) }
            }
            text(MetaKind.AUTHOR, info.author)
            text(MetaKind.DESCRIPTION, listOfNotNull(info.title, info.subject, info.keywords).filter { it.isNotBlank() }.joinToString(" · "))
            text(MetaKind.SOFTWARE, listOfNotNull(info.creator, info.producer).filter { it.isNotBlank() }.distinct().joinToString(" · "))
            runCatching { date(info.creationDate) }
            runCatching { date(info.modificationDate) }
            val standard = setOf("Author", "Title", "Subject", "Keywords", "Creator", "Producer", "CreationDate", "ModDate", "Trapped")
            info.metadataKeys.filter { it !in standard }.forEach { key ->
                info.getCustomMetadataValue(key)?.takeIf { it.isNotBlank() }?.let { add(MetaField(MetaKind.OTHER, "$key: $it")) }
            }
            val catalog = doc.documentCatalog
            if (catalog.metadata != null) add(MetaField(MetaKind.HIDDEN, "XMP"))
            runCatching {
                val fields = catalog.acroForm?.fields?.size ?: 0
                if (fields > 0) add(MetaField(MetaKind.FORMS, fields.toString()))
            }
            runCatching {
                val names = catalog.names
                if (names?.javaScript != null || catalog.openAction != null) add(MetaField(MetaKind.SCRIPTS, ""))
                if (names?.embeddedFiles != null) add(MetaField(MetaKind.ATTACHMENTS, ""))
            }
            runCatching {
                val notes = doc.pages.sumOf { page -> page.annotations.count { it.subtype != "Link" } }
                if (notes > 0) add(MetaField(MetaKind.NOTES, notes.toString()))
            }
        }
    }

    private fun office(file: File, type: DocType): List<MetaField> {
        val doc = OfficeDoc.open(file, type.office!!)
        return buildList {
            doc.properties().forEach { (name, value) ->
                val kind = when (name.lowercase()) {
                    "creator", "initial-creator", "lastmodifiedby", "manager" -> MetaKind.AUTHOR
                    "company" -> MetaKind.COMPANY
                    "created", "modified", "creation-date", "date", "lastprinted", "print-date" -> MetaKind.DATE
                    "application", "generator", "template" -> MetaKind.SOFTWARE
                    "title", "subject", "description", "keywords", "keyword", "category" -> MetaKind.DESCRIPTION
                    "revision", "editing-cycles", "editing-duration", "totaltime", "document-statistic" -> return@forEach
                    else -> MetaKind.OTHER
                }
                add(MetaField(kind, if (kind == MetaKind.OTHER) "$name: $value" else value))
            }
            val left = doc.leftovers()
            if ("Comments" in left) add(MetaField(MetaKind.COMMENTS, ""))
            if ("Tracked changes" in left) add(MetaField(MetaKind.REVISIONS, ""))
            if ("Thumbnail" in left) add(MetaField(MetaKind.THUMBNAIL, ""))
        }
    }
}
