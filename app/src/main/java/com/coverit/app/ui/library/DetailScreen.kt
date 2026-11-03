package com.coverit.app.ui.library

import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Star as FilledStar
import com.adamglin.phosphoricons.regular.ArrowsLeftRight
import com.adamglin.phosphoricons.regular.CaretLeft
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.DownloadSimple
import com.adamglin.phosphoricons.regular.Fingerprint
import com.adamglin.phosphoricons.regular.FolderSimple
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.SealCheck
import com.adamglin.phosphoricons.regular.SealWarning
import com.adamglin.phosphoricons.regular.ShareFat
import com.adamglin.phosphoricons.regular.Star
import com.adamglin.phosphoricons.regular.Trash
import com.coverit.app.R
import com.coverit.app.data.Record
import com.coverit.app.doc.MetaField
import com.coverit.app.doc.MetaReader
import com.coverit.app.ui.AppActions
import com.coverit.app.ui.LocalAppLanguage
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.common.HiddenDataSheet
import com.coverit.app.ui.common.MoveSheet
import com.coverit.app.ui.common.NameDialog
import com.coverit.app.ui.common.Thumbs
import com.coverit.app.ui.common.dateTime
import com.coverit.app.ui.common.icon
import com.coverit.app.ui.common.label
import com.coverit.app.ui.common.spaceName
import com.coverit.app.ui.components.BottomEdgeBlur
import com.coverit.app.ui.components.FileTitle
import com.coverit.app.ui.components.GlassIconButton
import com.coverit.app.ui.components.NavRow
import com.coverit.app.ui.components.Pill
import com.coverit.app.ui.components.PrimaryButton
import com.coverit.app.ui.components.RowDivider
import com.coverit.app.ui.components.SecondaryButton
import com.coverit.app.ui.components.SectionLabel
import com.coverit.app.ui.components.Segmented
import com.coverit.app.ui.components.TopBar
import com.coverit.app.ui.components.card
import com.coverit.app.ui.components.hairline
import com.coverit.app.ui.components.pressable
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(vm: MainViewModel, id: String, actions: AppActions) {
    val record = vm.library.find(id) ?: return
    val context = LocalContext.current
    var view by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(0) }
    var moving by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    val clean = vm.library.clean(record)
    val original = vm.library.original(record)
    val metaBefore by produceState<List<MetaField>?>(null, original, original?.lastModified()) {
        value = original?.let { withContext(Dispatchers.IO) { MetaReader.read(it, record.type) } }
    }
    val metaAfter by produceState<List<MetaField>?>(null, clean, clean.lastModified()) {
        value = withContext(Dispatchers.IO) { MetaReader.read(clean, record.type) }
    }
    val views = buildList {
        add(R.string.docs_clean_copy)
        if (original != null) add(R.string.docs_compare)
        add(R.string.docs_original)
    }
    val current = views.getOrElse(view) { views.first() }
    val showOriginal = current == R.string.docs_original
    val locale = LocalAppLanguage.current.locale
    val date = remember(record.created, locale) { dateTime(context, record.created, locale, java.text.DateFormat.LONG) }
    val top = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = top + 74.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                com.coverit.app.ui.components.FileTitle(
                    record.name.substringBeforeLast('.').ifBlank { record.name },
                    Modifier.weight(1f).pressable(scale = 0.98f) { renaming = true },
                )
                Spacer(Modifier.width(10.dp))
                GlassIconButton(PhosphorIcons.Regular.PencilSimple, stringResource(R.string.docs_rename), size = 40.dp) { renaming = true }
                Spacer(Modifier.width(8.dp))
                GlassIconButton(
                    if (record.starred) PhosphorIcons.Fill.FilledStar else PhosphorIcons.Regular.Star,
                    stringResource(if (record.starred) R.string.docs_unstar else R.string.docs_star),
                    size = 40.dp,
                ) { vm.library.star(record.id, !record.starred) }
            }
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(
                    stringResource(if (record.verified) R.string.docs_verified else R.string.docs_check_failed),
                    icon = if (record.verified) PhosphorIcons.Regular.SealCheck else PhosphorIcons.Regular.SealWarning,
                )
                Pill(pluralStringResource(R.plurals.covered_n, record.covered.size, record.covered.size))
                Pill(
                    spaceName(vm.library, record.space) + " · " + (vm.library.folder(record.folder)?.name ?: stringResource(R.string.docs_no_folder)),
                    icon = PhosphorIcons.Regular.FolderSimple,
                ) { moving = true }
            }
            Spacer(Modifier.height(22.dp))
            Segmented(
                options = views.map { stringResource(it) },
                index = views.indexOf(current),
                onChange = { view = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            AnimatedContent(
                targetState = current,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "preview",
            ) { shown ->
                val p = page.coerceIn(0, record.pages - 1)
                when {
                    shown == R.string.docs_compare && original != null -> Compare(record, original, clean, p)
                    shown == R.string.docs_original && original == null -> Box(Modifier.fillMaxWidth().card().padding(20.dp)) {
                        Text(stringResource(R.string.docs_original_missing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    shown == R.string.docs_original -> Preview(record, original!!, p)
                    else -> Preview(record, clean, p)
                }
            }
            if (record.pages > 1) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    GlassIconButton(PhosphorIcons.Regular.CaretLeft, stringResource(R.string.back), size = 38.dp) { if (page > 0) page-- }
                    Text(
                        "${page + 1} / ${record.pages}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                    GlassIconButton(PhosphorIcons.Regular.CaretRight, stringResource(R.string.next), size = 38.dp) { if (page < record.pages - 1) page++ }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (original != null) {
                SecondaryButton(stringResource(R.string.docs_edit), Modifier.fillMaxWidth(), PhosphorIcons.Regular.PencilSimple) { vm.reopen(record) }
            } else {
                Text(
                    stringResource(R.string.docs_edit_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            Spacer(Modifier.height(26.dp))
            SectionLabel(stringResource(R.string.meta_title))
            Column(Modifier.fillMaxWidth().card()) {
                val after = metaAfter
                NavRow(
                    PhosphorIcons.Regular.Fingerprint,
                    when {
                        after == null -> stringResource(R.string.meta_reading)
                        after.isEmpty() -> stringResource(R.string.meta_clean_copy)
                        else -> pluralStringResource(R.plurals.meta_left_n, after.size, after.size)
                    },
                    when {
                        original == null -> stringResource(R.string.meta_no_original)
                        else -> metaBefore?.let { pluralStringResource(R.plurals.meta_original_n, it.size, it.size) } ?: ""
                    },
                ) { if (after != null) hidden = true }
            }
            Spacer(Modifier.height(26.dp))
            SectionLabel(stringResource(R.string.docs_covered_title))
            Column(Modifier.fillMaxWidth().card()) {
                if (record.covered.isEmpty()) {
                    Text(
                        stringResource(R.string.docs_nothing_covered),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                record.covered.groupBy { it.kind }.entries.forEachIndexed { i, (kind, items) ->
                    if (i > 0) RowDivider(52.dp)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Icon(kind.icon, null, Modifier.padding(top = 1.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(kind.label), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            val masked = items.map { it.masked }.filter { it.isNotBlank() }
                            if (masked.isNotEmpty()) {
                                Text(masked.joinToString("   "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("${items.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            SecondaryButton(stringResource(R.string.delete), Modifier.fillMaxWidth(), PhosphorIcons.Regular.Trash, tint = MaterialTheme.colorScheme.error) { vm.deleteRecords(listOf(record.id)) }
            Spacer(Modifier.height(170.dp))
        }

        TopBar(onBack = { vm.back() }, backLabel = stringResource(R.string.back))

        BottomEdgeBlur(height = 120.dp)
        Row(
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryButton(stringResource(R.string.save), Modifier.weight(1f), PhosphorIcons.Regular.DownloadSimple) { actions.save(record) }
            PrimaryButton(
                stringResource(if (showOriginal && original != null) R.string.docs_share_original else R.string.share),
                Modifier.weight(1.4f),
                PhosphorIcons.Regular.ShareFat,
            ) { actions.share(record, showOriginal && original != null) }
        }
    }

    if (hidden) HiddenDataSheet(metaBefore, metaAfter.orEmpty()) { hidden = false }
    if (moving) MoveSheet(vm, record) { moving = false }
    if (renaming) {
        NameDialog(
            title = stringResource(R.string.docs_rename),
            initial = record.name,
            placeholder = stringResource(R.string.docs_name),
            confirm = stringResource(R.string.done),
            onConfirm = { vm.library.rename(record.id, it); it.isNotBlank() },
            onDismiss = { renaming = false },
        )
    }
}

@Composable
private fun Compare(record: Record, original: File, clean: File, page: Int) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val before by produceState<Bitmap?>(null, original, original.lastModified(), page, widthPx) { value = Thumbs.load(original, record.type, widthPx, page) }
        val after by produceState<Bitmap?>(null, clean, clean.lastModified(), page, widthPx) { value = Thumbs.load(clean, record.type, widthPx, page) }
        var split by remember { mutableFloatStateOf(0.5f) }
        val ratio = (after ?: before)?.let { it.width.toFloat() / it.height } ?: 0.72f
        val handle = 40.dp
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .hairline(RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .pointerInput(widthPx) {
                    detectTapGestures { split = (it.x / size.width).coerceIn(0f, 1f) }
                }
                .pointerInput(widthPx) {
                    detectDragGestures { change, _ -> split = (change.position.x / size.width).coerceIn(0f, 1f) }
                },
        ) {
            after?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
            before?.let {
                Image(
                    it.asImageBitmap(),
                    null,
                    Modifier
                        .fillMaxSize()
                        .drawWithContent { clipRect(right = size.width * split) { this@drawWithContent.drawContent() } },
                )
            }
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .offset { IntOffset((widthPx * split).toInt() - 1.dp.roundToPx(), 0) }
                    .background(Color.Black),
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((widthPx * split).toInt() - (handle / 2).roundToPx(), 0) }
                    .size(handle)
                    .border(2.dp, Color.White, CircleShape)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Icon(PhosphorIcons.Regular.ArrowsLeftRight, null, Modifier.size(18.dp), tint = Color.White)
            }
            CompareTag(stringResource(R.string.compare_before), Modifier.align(Alignment.TopStart))
            CompareTag(stringResource(R.string.compare_after), Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun CompareTag(text: String, modifier: Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = modifier
            .padding(10.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun Preview(record: Record, file: File, page: Int) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val bitmap by produceState<Bitmap?>(null, file, file.lastModified(), page, widthPx) { value = Thumbs.load(file, record.type, widthPx, page) }
        val ratio = bitmap?.let { it.width.toFloat() / it.height } ?: 0.72f
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .hairline(RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White),
        ) {
            bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
        }
    }
}
