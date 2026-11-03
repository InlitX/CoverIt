package com.coverit.app.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.AppWindow
import com.adamglin.phosphoricons.regular.Buildings
import com.adamglin.phosphoricons.regular.CalendarBlank
import com.adamglin.phosphoricons.regular.ChatCircleText
import com.adamglin.phosphoricons.regular.ClockCounterClockwise
import com.adamglin.phosphoricons.regular.Code
import com.adamglin.phosphoricons.regular.DeviceMobileCamera
import com.adamglin.phosphoricons.regular.Eraser
import com.adamglin.phosphoricons.regular.Fingerprint
import com.adamglin.phosphoricons.regular.Image
import com.adamglin.phosphoricons.regular.Info
import com.adamglin.phosphoricons.regular.MapPin
import com.adamglin.phosphoricons.regular.NotePencil
import com.adamglin.phosphoricons.regular.Paperclip
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.regular.Stack
import com.adamglin.phosphoricons.regular.TextAlignLeft
import com.adamglin.phosphoricons.regular.Textbox
import com.adamglin.phosphoricons.regular.User
import com.coverit.app.R
import com.coverit.app.doc.MetaField
import com.coverit.app.doc.MetaKind
import com.coverit.app.ui.components.IconBadge
import com.coverit.app.ui.components.RowDivider
import com.coverit.app.ui.components.Segmented
import com.coverit.app.ui.components.card

val MetaKind.label: Int
    get() = when (this) {
        MetaKind.LOCATION -> R.string.meta_location
        MetaKind.DEVICE -> R.string.meta_device
        MetaKind.DATE -> R.string.meta_date
        MetaKind.AUTHOR -> R.string.meta_author
        MetaKind.COMPANY -> R.string.meta_company
        MetaKind.SOFTWARE -> R.string.meta_software
        MetaKind.SERIAL -> R.string.meta_serial
        MetaKind.DESCRIPTION -> R.string.meta_description
        MetaKind.COMMENTS -> R.string.meta_comments
        MetaKind.REVISIONS -> R.string.meta_revisions
        MetaKind.THUMBNAIL -> R.string.meta_thumbnail
        MetaKind.HIDDEN -> R.string.meta_hidden
        MetaKind.FORMS -> R.string.meta_forms
        MetaKind.SCRIPTS -> R.string.meta_scripts
        MetaKind.ATTACHMENTS -> R.string.meta_attachments
        MetaKind.NOTES -> R.string.meta_notes
        MetaKind.OTHER -> R.string.meta_other
    }

val MetaKind.hint: Int
    get() = when (this) {
        MetaKind.LOCATION -> R.string.meta_location_hint
        MetaKind.DEVICE -> R.string.meta_device_hint
        MetaKind.DATE -> R.string.meta_date_hint
        MetaKind.AUTHOR -> R.string.meta_author_hint
        MetaKind.COMPANY -> R.string.meta_company_hint
        MetaKind.SOFTWARE -> R.string.meta_software_hint
        MetaKind.SERIAL -> R.string.meta_serial_hint
        MetaKind.DESCRIPTION -> R.string.meta_description_hint
        MetaKind.COMMENTS -> R.string.meta_comments_hint
        MetaKind.REVISIONS -> R.string.meta_revisions_hint
        MetaKind.THUMBNAIL -> R.string.meta_thumbnail_hint
        MetaKind.HIDDEN -> R.string.meta_hidden_hint
        MetaKind.FORMS -> R.string.meta_forms_hint
        MetaKind.SCRIPTS -> R.string.meta_scripts_hint
        MetaKind.ATTACHMENTS -> R.string.meta_attachments_hint
        MetaKind.NOTES -> R.string.meta_notes_hint
        MetaKind.OTHER -> R.string.meta_other_hint
    }

val MetaKind.icon: ImageVector
    get() = when (this) {
        MetaKind.LOCATION -> PhosphorIcons.Regular.MapPin
        MetaKind.DEVICE -> PhosphorIcons.Regular.DeviceMobileCamera
        MetaKind.DATE -> PhosphorIcons.Regular.CalendarBlank
        MetaKind.AUTHOR -> PhosphorIcons.Regular.User
        MetaKind.COMPANY -> PhosphorIcons.Regular.Buildings
        MetaKind.SOFTWARE -> PhosphorIcons.Regular.AppWindow
        MetaKind.SERIAL -> PhosphorIcons.Regular.Fingerprint
        MetaKind.DESCRIPTION -> PhosphorIcons.Regular.TextAlignLeft
        MetaKind.COMMENTS -> PhosphorIcons.Regular.ChatCircleText
        MetaKind.REVISIONS -> PhosphorIcons.Regular.ClockCounterClockwise
        MetaKind.THUMBNAIL -> PhosphorIcons.Regular.Image
        MetaKind.HIDDEN -> PhosphorIcons.Regular.Stack
        MetaKind.FORMS -> PhosphorIcons.Regular.Textbox
        MetaKind.SCRIPTS -> PhosphorIcons.Regular.Code
        MetaKind.ATTACHMENTS -> PhosphorIcons.Regular.Paperclip
        MetaKind.NOTES -> PhosphorIcons.Regular.NotePencil
        MetaKind.OTHER -> PhosphorIcons.Regular.Info
    }

@Composable
fun HiddenDataSheet(before: List<MetaField>?, after: List<MetaField>?, onDismiss: () -> Unit) {
    val both = before != null && after != null
    var tab by remember { mutableIntStateOf(0) }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.62f
    CoverSheet(onDismiss) { _ ->
        Text(stringResource(R.string.meta_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp))
        Text(
            stringResource(R.string.meta_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        if (both) {
            Segmented(
                options = listOf(
                    stringResource(R.string.meta_tab_before, before!!.size),
                    stringResource(R.string.meta_tab_after, after!!.size),
                ),
                index = tab,
                onChange = { tab = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val showingAfter = after != null && (before == null || tab == 1)
        AnimatedContent(
            targetState = showingAfter,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            label = "metaTab",
        ) { afterTab ->
            val fields = (if (afterTab) after else before).orEmpty()
            Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState())) {
                if (fields.isEmpty()) {
                    Row(Modifier.fillMaxWidth().card().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(PhosphorIcons.Regular.ShieldCheck)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(stringResource(R.string.meta_none), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                stringResource(if (afterTab) R.string.meta_none_after else R.string.meta_none_before),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().card()) {
                        fields.forEachIndexed { i, f ->
                            if (i > 0) RowDivider(64.dp)
                            val gone = !afterTab && (after == null || after.none { it.kind == f.kind && it.value == f.value })
                            MetaRow(f, gone, removed = after != null)
                        }
                    }
                }
                if (!afterTab && before != null && after == null && fields.isNotEmpty()) {
                    Text(
                        stringResource(R.string.meta_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, top = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaRow(field: MetaField, gone: Boolean, removed: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Top) {
        IconBadge(field.kind.icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(field.kind.label), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                if (gone) {
                    Icon(PhosphorIcons.Regular.Eraser, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(if (removed) R.string.meta_removed else R.string.meta_will_remove),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                field.value.ifEmpty { stringResource(R.string.meta_present) },
                style = MaterialTheme.typography.bodyMedium.copy(textDecoration = if (gone && removed) TextDecoration.LineThrough else null),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (gone && removed) 0.6f else 0.9f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(stringResource(field.kind.hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
