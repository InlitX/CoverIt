package com.coverit.app.ui.common

import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Star as FilledStar
import com.adamglin.phosphoricons.regular.Broom
import com.adamglin.phosphoricons.regular.FileImage
import com.adamglin.phosphoricons.regular.FileText
import com.adamglin.phosphoricons.regular.FolderSimple
import com.adamglin.phosphoricons.regular.FolderSimplePlus
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.SealCheck
import com.adamglin.phosphoricons.regular.SealWarning
import com.adamglin.phosphoricons.regular.ShareFat
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.Star
import com.adamglin.phosphoricons.regular.Trash
import com.coverit.app.R
import com.coverit.app.data.Library
import com.coverit.app.data.Record
import com.coverit.app.data.Space
import com.coverit.app.ui.AppActions
import com.coverit.app.ui.LocalAppLanguage
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.components.CoverDialog
import com.coverit.app.ui.components.InputField
import com.coverit.app.ui.components.Pill
import com.coverit.app.ui.components.PrimaryButton
import com.coverit.app.ui.components.RowDivider
import com.coverit.app.ui.components.SectionLabel
import com.coverit.app.ui.components.SettingRow
import com.coverit.app.ui.components.CoverCheckbox
import com.coverit.app.ui.components.card
import com.coverit.app.ui.components.hairline
import com.coverit.app.ui.components.lift
import com.coverit.app.ui.components.pressable
import java.util.Date
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpacePicker(library: Library, selected: Space, onSelect: (Space) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        library.spaces.forEach { s ->
            Pill(spaceName(library, s), icon = spaceIcon(library, s), selected = s == selected) { onSelect(s) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.(close: (() -> Unit) -> Unit) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { after ->
        scope.launch { state.hide() }.invokeOnCompletion {
            onDismiss()
            after()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 8.dp).size(36.dp, 4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) },
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content(close)
        }
    }
}

@Composable
fun NewButton(onClick: () -> Unit) {
    Row(
        Modifier
            .height(44.dp)
            .pressable(scale = 0.93f, onClick = onClick)
            .lift(CircleShape, 10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PhosphorIcons.Regular.Plus, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.docs_new), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun NewDocumentSheet(actions: AppActions, onDismiss: () -> Unit) {
    CoverSheet(onDismiss) { close ->
        Text(stringResource(R.string.docs_new), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        OpenOption(PhosphorIcons.Regular.FileText, stringResource(R.string.docs_open_pdf), stringResource(R.string.docs_open_pdf_hint)) { close(actions.openPdf) }
        OpenOption(PhosphorIcons.Regular.FileImage, stringResource(R.string.docs_open_photo), stringResource(R.string.docs_open_photo_hint)) { close(actions.openPhoto) }
        OpenOption(PhosphorIcons.Regular.Broom, stringResource(R.string.clean_title), stringResource(R.string.clean_hint), accent = false) { close(actions.cleanFiles) }
    }
}

@Composable
fun OpenOption(icon: ImageVector, title: String, hint: String, trailing: ImageVector = PhosphorIcons.Regular.Plus, accent: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(scale = 0.97f, onClick = onClick)
            .card()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(24.dp), tint = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(trailing, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NameDialog(title: String, initial: String, placeholder: String, confirm: String, onConfirm: (String) -> Boolean, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    CoverDialog(
        title = title,
        body = "",
        confirm = confirm,
        dismiss = stringResource(R.string.cancel),
        onConfirm = { if (onConfirm(name)) onDismiss() },
        onDismiss = onDismiss,
    ) {
        Spacer(Modifier.height(16.dp))
        InputField(name, { name = it }, placeholder, Modifier.fillMaxWidth(), leading = PhosphorIcons.Regular.PencilSimple, onDone = { if (onConfirm(name)) onDismiss() })
    }
}

@Composable
fun rememberThumb(library: Library, record: Record, widthPx: Int): Bitmap? {
    val file = library.clean(record)
    val bitmap by produceState(Thumbs.peek(file, widthPx), record, widthPx) {
        if (value == null) value = Thumbs.load(file, record.type, widthPx)
    }
    return bitmap
}

@Composable
fun folderPages(library: Library, records: List<Record>): List<Bitmap?> =
    records.take(3).map { r -> androidx.compose.runtime.key(r.id) { rememberThumb(library, r, 180) } }

@Composable
fun DocumentCard(library: Library, record: Record, modifier: Modifier = Modifier, selected: Boolean? = null, onLongClick: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    val thumbPx = with(LocalDensity.current) { 64.dp.roundToPx() }
    val thumb = rememberThumb(library, record, thumbPx * 2)
    val shown by animateFloatAsState(if (thumb != null) 1f else 0f, tween(240), label = "thumb")
    val locale = LocalAppLanguage.current.locale
    val date = remember(record.created, locale) { dateTime(context, record.created, locale, java.text.DateFormat.MEDIUM) }
    Row(
        modifier
            .fillMaxWidth()
            .pressable(scale = 0.98f, onLongClick = onLongClick, onClick = onClick)
            .card()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 68.dp)
                .hairline(RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White),
        ) {
            thumb?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize().graphicsLayer { alpha = shown }, contentScale = ContentScale.Crop) }
            if (selected == true) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Icon(PhosphorIcons.Regular.Check, null, Modifier.size(26.dp), tint = Color.White)
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    record.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (record.starred) {
                    Spacer(Modifier.width(6.dp))
                    Icon(PhosphorIcons.Fill.FilledStar, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    record.type.icon,
                    null,
                    Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    pluralStringResource(R.plurals.covered_n, record.covered.size, record.covered.size) +
                        (library.folder(record.folder)?.let { " · " + it.name } ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (selected != null) {
            CoverCheckbox(selected) { onClick() }
            return@Row
        }
        Icon(
            if (record.verified) PhosphorIcons.Regular.SealCheck else PhosphorIcons.Regular.SealWarning,
            stringResource(if (record.verified) R.string.docs_verified else R.string.docs_check_failed),
            Modifier.size(22.dp),
            tint = if (record.verified) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        )
    }
}

private enum class DocDialog { NONE, RENAME, MOVE }

@Composable
fun DocumentOptions(vm: MainViewModel, record: Record, actions: AppActions, onDismiss: () -> Unit) {
    var dialog by remember { mutableStateOf(DocDialog.NONE) }
    if (dialog == DocDialog.NONE) {
        CoverSheet(onDismiss = { if (dialog == DocDialog.NONE) onDismiss() }) { close ->
            Text(
                record.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            Column(Modifier.fillMaxWidth().card()) {
                SettingRow(PhosphorIcons.Regular.ShareFat, stringResource(R.string.share), onClick = { close { actions.share(record, false) } })
                RowDivider()
                SettingRow(
                    if (record.starred) PhosphorIcons.Fill.FilledStar else PhosphorIcons.Regular.Star,
                    stringResource(if (record.starred) R.string.docs_unstar else R.string.docs_star),
                    onClick = { close { vm.library.star(record.id, !record.starred) } },
                )
                RowDivider()
                SettingRow(PhosphorIcons.Regular.PencilSimple, stringResource(R.string.docs_rename), onClick = { dialog = DocDialog.RENAME })
                RowDivider()
                SettingRow(PhosphorIcons.Regular.FolderSimple, stringResource(R.string.docs_move), onClick = { dialog = DocDialog.MOVE })
                RowDivider()
                SettingRow(
                    PhosphorIcons.Regular.Trash,
                    stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { close { vm.deleteRecords(listOf(record.id)) } },
                )
            }
        }
    }
    when (dialog) {
        DocDialog.RENAME -> NameDialog(
            title = stringResource(R.string.docs_rename),
            initial = record.name,
            placeholder = stringResource(R.string.docs_name),
            confirm = stringResource(R.string.done),
            onConfirm = { vm.library.rename(record.id, it); it.isNotBlank() },
            onDismiss = onDismiss,
        )
        DocDialog.MOVE -> MoveSheet(vm, record, onDismiss)
        DocDialog.NONE -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoveSheet(vm: MainViewModel, record: Record, onDismiss: () -> Unit) = MoveSheet(vm, listOf(record), onDismiss)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoveSheet(vm: MainViewModel, records: List<Record>, onDismiss: () -> Unit) {
    val first = records.firstOrNull() ?: return
    var space by remember { mutableStateOf(first.space) }
    var folder by remember { mutableStateOf(first.folder.takeIf { f -> records.all { it.folder == f } }) }
    var creating by remember { mutableStateOf(false) }
    CoverSheet(onDismiss) { close ->
        Text(stringResource(R.string.docs_move), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp))
        SectionLabel(stringResource(R.string.docs_space), Modifier.padding(start = 4.dp, top = 4.dp))
        SpacePicker(vm.library, space) {
            space = it
            folder = null
        }
        SectionLabel(stringResource(R.string.docs_folder), Modifier.padding(start = 4.dp, top = 4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(stringResource(R.string.docs_no_folder), selected = folder == null) { folder = null }
            vm.library.foldersIn(space).forEach { f -> Pill(f.name, icon = PhosphorIcons.Regular.FolderSimple, selected = folder == f.id) { folder = f.id } }
            Pill(stringResource(R.string.docs_new_folder), icon = PhosphorIcons.Regular.FolderSimplePlus) { creating = true }
        }
        PrimaryButton(stringResource(R.string.done), Modifier.fillMaxWidth().padding(top = 8.dp)) {
            close {
                vm.moveRecords(records.map { it.id }, space, folder)
                vm.clearSelection()
            }
        }
    }
    if (creating) {
        NameDialog(
            title = stringResource(R.string.docs_new_folder),
            initial = "",
            placeholder = stringResource(R.string.docs_folder_name),
            confirm = stringResource(R.string.add),
            onConfirm = { name -> vm.library.addFolder(name, space)?.also { folder = it.id } != null },
            onDismiss = { creating = false },
        )
    }
}

fun dateTime(context: android.content.Context, millis: Long, locale: java.util.Locale, style: Int): String {
    val d = Date(millis)
    val time = java.text.SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", locale)
    return java.text.DateFormat.getDateInstance(style, locale).format(d) + " · " + time.format(d)
}
