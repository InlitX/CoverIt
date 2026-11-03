package com.coverit.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowCounterClockwise
import com.adamglin.phosphoricons.regular.ArrowsOut
import com.adamglin.phosphoricons.regular.FileMinus
import com.adamglin.phosphoricons.regular.Stamp
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.CaretUp
import com.adamglin.phosphoricons.regular.ClockCounterClockwise
import com.adamglin.phosphoricons.regular.CopySimple
import com.adamglin.phosphoricons.regular.Crop
import com.adamglin.phosphoricons.regular.Fingerprint
import com.adamglin.phosphoricons.regular.FolderSimple
import com.adamglin.phosphoricons.regular.Hash
import com.adamglin.phosphoricons.regular.Lasso
import com.adamglin.phosphoricons.regular.ListChecks
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.PaintBrush
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.Selection
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.regular.Square
import com.adamglin.phosphoricons.regular.SquareHalf
import com.adamglin.phosphoricons.regular.Warning
import com.adamglin.phosphoricons.regular.X
import com.coverit.app.R
import com.coverit.app.detect.CustomRule
import com.coverit.app.detect.Kind
import com.coverit.app.doc.Family
import com.coverit.app.doc.ImageSource
import com.coverit.app.doc.Mark
import com.coverit.app.doc.MetaField
import com.coverit.app.doc.MetaReader
import com.coverit.app.doc.Session
import com.coverit.app.redact.Style
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.common.CoverSheet
import com.coverit.app.ui.common.HiddenDataSheet
import com.coverit.app.ui.common.NameDialog
import com.coverit.app.ui.common.OpenOption
import com.coverit.app.ui.common.hint
import com.coverit.app.ui.common.icon
import com.coverit.app.ui.common.label
import com.coverit.app.ui.common.spaceName
import com.coverit.app.ui.components.BottomEdgeBlur
import com.coverit.app.ui.components.CoverCheckbox
import com.coverit.app.ui.components.CoverDialog
import com.coverit.app.ui.components.CoverSwitch
import com.coverit.app.ui.components.FileTitle
import com.coverit.app.ui.components.GlassIconButton
import com.coverit.app.ui.components.IconBadge
import com.coverit.app.ui.components.InputField
import com.coverit.app.ui.components.NavRow
import com.coverit.app.ui.components.Pill
import com.coverit.app.ui.components.PrimaryButton
import com.coverit.app.ui.components.RowDivider
import com.coverit.app.ui.components.SecondaryButton
import com.coverit.app.ui.components.SectionLabel
import com.coverit.app.ui.components.Segmented
import com.coverit.app.ui.components.SettingRow
import com.coverit.app.ui.components.TopBar
import com.coverit.app.ui.components.card
import com.coverit.app.ui.components.hairline
import com.coverit.app.ui.components.lift
import com.coverit.app.ui.components.pressable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val FOLDED = 6

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(vm: MainViewModel, session: Session) {
    // No live blur here: the pages are big pictures, and blurring them on every frame of a
    // scroll is what made the editor heavy. The edges fade with plain gradients instead.
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var drawing by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(DrawTool.LASSO) }
    var selection by remember { mutableStateOf<Mark?>(null) }
    var editing by remember { mutableStateOf<Grip?>(null) }
    var flash by remember { mutableStateOf<Flash?>(null) }
    var focus by remember { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    var unfolded by rememberSaveable { mutableStateOf(false) }
    var adjusting by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var marking by remember { mutableStateOf(false) }
    var reviewing by remember { mutableStateOf(false) }
    var brushSize by rememberSaveable { mutableStateOf(1) }
    val haptics = LocalHapticFeedback.current
    val meta by produceState<List<MetaField>?>(null, session) {
        value = withContext(Dispatchers.IO) { MetaReader.read(session.source.file, session.source.type) }
    }
    val photo = session.source as? ImageSource
    val density = LocalDensity.current
    val top = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val bottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val scrolled by remember { derivedStateOf { list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 120 } }
    val all by remember(session) { derivedStateOf { session.marks.sortedWith(compareBy({ it.page }, { it.boxes.firstOrNull()?.top ?: 0f })) } }
    val chosen by remember(session) { derivedStateOf { session.marks.count { it.checked } } }
    val byPage by remember(session) { derivedStateOf { session.marks.groupBy { it.page } } }
    val scanned = session.scannedPages
    val firstPage = 2 + scanned.size

    fun leave() {
        if (session.edited) leaving = true else vm.back()
    }
    BackHandler(enabled = focus == null && !adjusting) { leave() }
    BackHandler(enabled = focus == null && !adjusting && (editing != null || drawing)) {
        editing = null
        drawing = false
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pageWidthPx = with(density) { (maxWidth - 40.dp).toPx() }
        val viewHeightPx = with(density) { maxHeight.toPx() }

        fun jumpTo(mark: Mark) {
            val page = session.source.pages[mark.page]
            val y = (mark.boxes.firstOrNull()?.top ?: 0f) * pageWidthPx / page.width
            scope.launch {
                list.animateScrollToItem(firstPage + mark.page, (y - viewHeightPx * 0.32f).toInt().coerceAtLeast(0))
                flash = Flash(mark, System.nanoTime())
            }
        }

        val pageActions = { index: Int ->
            PageActions(
                onEdit = { editing = it },
                onCover = { session.toggle(it, true) },
                onReshapeStart = { session.beginReshape() },
                onReshape = { mark, i, box -> session.reshape(mark, i, box) },
                onUncover = {
                    editing = null
                    session.uncover(it)
                },
                onMore = {
                    editing = null
                    selection = it
                },
                onLasso = { points ->
                    drawing = false
                    val mark = session.lasso(index, points)
                    if (mark != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (mark != null && mark.value.isNotBlank()) selection = mark
                },
                onArea = { box ->
                    drawing = false
                    if (session.area(index, box) != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onScroll = { dy -> list.dispatchRawDelta(dy) },
                onHold = { x, y ->
                    session.wordAt(index, x, y)?.let {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        selection = it
                    }
                },
                onBrush = { points, radius ->
                    if (session.brush(index, points, radius) != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
            )
        }
        val actionsFor = remember(session) { HashMap<Int, PageActions>() }

        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 74.dp, bottom = bottom + 120.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.Top) {
                    com.coverit.app.ui.components.FileTitle(
                        session.name.substringBeforeLast('.').ifBlank { session.name },
                        Modifier.weight(1f).pressable(scale = 0.98f) { renaming = true },
                    )
                    Spacer(Modifier.width(10.dp))
                    GlassIconButton(PhosphorIcons.Regular.PencilSimple, stringResource(R.string.docs_rename), size = 40.dp) { renaming = true }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(session.source.type.extension.uppercase() + " · " + pluralStringResource(R.plurals.pages_n, session.source.pages.size, session.source.pages.size), icon = session.source.type.icon)
                    Pill(pluralStringResource(R.plurals.covered_n, chosen, chosen), icon = PhosphorIcons.Regular.ShieldCheck)
                }
                Spacer(Modifier.height(26.dp))
                SectionLabel(stringResource(R.string.editor_tools))
                Column(Modifier.fillMaxWidth().card()) {
                    if (photo != null) {
                        NavRow(PhosphorIcons.Regular.Crop, stringResource(R.string.adjust_title), stringResource(R.string.adjust_sub)) {
                            editing = null
                            drawing = false
                            adjusting = true
                        }
                        RowDivider()
                    }
                    val found = meta
                    NavRow(
                        PhosphorIcons.Regular.Fingerprint,
                        stringResource(R.string.meta_title),
                        when {
                            found == null -> stringResource(R.string.meta_reading)
                            found.isEmpty() -> stringResource(R.string.meta_none_before)
                            else -> found.map { stringResource(it.kind.label) }.distinct().joinToString(", ")
                        },
                        value = found?.size?.takeIf { it > 0 }?.toString(),
                    ) { if (found != null) hidden = true }
                    if (session.canWatermark) {
                        RowDivider()
                        NavRow(
                            PhosphorIcons.Regular.Stamp,
                            stringResource(R.string.watermark_title),
                            session.watermark ?: stringResource(R.string.watermark_sub),
                        ) {
                            drawing = false
                            editing = null
                            marking = true
                        }
                    }
                    RowDivider()
                    val where = vm.saveOptions(session)
                    NavRow(
                        PhosphorIcons.Regular.FolderSimple,
                        stringResource(R.string.editor_where),
                        spaceName(vm.library, where.space) + " · " + (vm.library.folder(where.folder)?.name ?: stringResource(R.string.docs_no_folder)),
                    ) {
                        drawing = false
                        editing = null
                        saving = true
                    }
                }
                Spacer(Modifier.height(26.dp))
                if (session.source.type.family == Family.OFFICE) {
                    Text(stringResource(R.string.editor_office_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
                } else {
                    SectionLabel(stringResource(R.string.editor_style))
                    Segmented(
                        options = Style.entries.map { stringResource(it.label) },
                        index = session.style.ordinal,
                        onChange = { session.changeStyle(Style.entries[it]) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(session.style.hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
                }
                Spacer(Modifier.height(26.dp))
                if (session.hasText) {
                    SectionLabel(stringResource(R.string.editor_search))
                    SearchCover(session)
                    Spacer(Modifier.height(26.dp))
                }
            }

            scanned.forEach { index ->
                item(key = "scan$index") {
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).card().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(PhosphorIcons.Regular.Warning)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.editor_scanned, index + 1), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            item {
                if (all.size > 1) {
                    OpenOption(
                        PhosphorIcons.Regular.ListChecks,
                        stringResource(R.string.review_start),
                        stringResource(R.string.review_start_sub, all.size),
                        trailing = PhosphorIcons.Regular.CaretRight,
                    ) {
                        drawing = false
                        editing = null
                        reviewing = true
                    }
                    Spacer(Modifier.height(18.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(stringResource(R.string.editor_found), Modifier.weight(1f))
                    if (all.isNotEmpty()) {
                        TextAction(stringResource(R.string.editor_all)) { session.setAll(true) }
                        TextAction(stringResource(R.string.editor_none)) { session.setAll(false) }
                    }
                }
                if (all.isEmpty()) {
                    Row(Modifier.fillMaxWidth().card().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(PhosphorIcons.Regular.ShieldCheck)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(stringResource(R.string.editor_none_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.editor_none_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    val groups = all.groupBy { if (it.kind == Kind.CUSTOM) Kind.MANUAL else it.kind }
                    if (groups.size > 1) {
                        FlowRow(
                            Modifier.padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            groups.forEach { (kind, marks) ->
                                val on = marks.count { it.checked }
                                Pill(
                                    stringResource(kind.label) + "  " + if (on == marks.size) "${marks.size}" else "$on/${marks.size}",
                                    icon = kind.icon,
                                    selected = on == marks.size,
                                ) {
                                    val kinds = if (kind == Kind.MANUAL) setOf(Kind.MANUAL, Kind.CUSTOM) else setOf(kind)
                                    session.setKind(kinds, on < marks.size)
                                }
                            }
                        }
                    }
                    val shown = if (unfolded || all.size <= FOLDED + 1) all else all.take(FOLDED)
                    Column(Modifier.fillMaxWidth().card()) {
                        shown.forEachIndexed { i, mark ->
                            if (i > 0) RowDivider(58.dp)
                            MarkRow(
                                mark,
                                onToggle = { session.toggle(mark, it) },
                                onOpen = { jumpTo(mark) },
                                onRemove = if (mark.kind == Kind.MANUAL || mark.kind == Kind.CUSTOM) ({ session.remove(mark) }) else null,
                            )
                        }
                        if (all.size > FOLDED + 1) {
                            RowDivider(16.dp)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(remember { MutableInteractionSource() }, null) { unfolded = !unfolded }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    if (unfolded) stringResource(R.string.editor_show_less) else stringResource(R.string.editor_show_all, all.size),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(if (unfolded) PhosphorIcons.Regular.CaretUp else PhosphorIcons.Regular.CaretDown, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.editor_list_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, top = 8.dp),
                    )
                }
                Spacer(Modifier.height(26.dp))
                SectionLabel(stringResource(R.string.editor_pages))
            }

            items(session.source.pages, key = { "page${it.index}" }) { page ->
                val covered = session.pageCovered(page.index)
                val gone = page.index in session.removed
                val toggleRemoved = {
                    editing = null
                    session.toggleRemoved(page.index)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                val togglePage = {
                    editing = null
                    if (covered) session.uncoverPage(page.index) else session.coverPage(page.index)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                Row(Modifier.fillMaxWidth().padding(start = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${page.index + 1} / ${session.source.pages.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (gone) {
                        Pill(stringResource(R.string.page_restore), icon = PhosphorIcons.Regular.ArrowCounterClockwise, onClick = toggleRemoved)
                    } else {
                        if (session.canRemovePages) {
                            Pill(stringResource(R.string.page_remove), icon = PhosphorIcons.Regular.FileMinus, onClick = toggleRemoved)
                            Spacer(Modifier.width(8.dp))
                        }
                        Pill(
                            stringResource(if (covered) R.string.page_uncover_short else R.string.page_cover_short),
                            icon = if (covered) PhosphorIcons.Regular.SquareHalf else PhosphorIcons.Regular.Square,
                            onClick = togglePage,
                        )
                        Spacer(Modifier.width(8.dp))
                        Pill(stringResource(R.string.editor_zoom_short), icon = PhosphorIcons.Regular.ArrowsOut) {
                            editing = null
                            drawing = false
                            focus = page.index
                        }
                    }
                }
                Box(Modifier.graphicsLayer { alpha = if (gone) 0.3f else 1f }) {
                    PageCanvas(
                        page = page,
                        marks = byPage[page.index].orEmpty(),
                        style = session.style,
                        drawing = drawing,
                        tool = tool,
                        editing = editing?.takeIf { it.mark.page == page.index && it.mark in session.marks },
                        flash = flash?.takeIf { it.mark.page == page.index },
                        render = vm::render,
                        cached = vm::cachedPage,
                        actions = actionsFor.getOrPut(page.index) { pageActions(page.index) },
                        brush = BrushSizes[brushSize],
                        watermark = session.watermark,
                    )
                    if (covered && !gone) {
                        Pill(
                            stringResource(R.string.page_covered_note),
                            Modifier.align(Alignment.Center),
                            icon = PhosphorIcons.Regular.SquareHalf,
                            selected = true,
                            onClick = togglePage,
                        )
                    }
                    if (gone) {
                        Box(Modifier.matchParentSize().clickable(remember { MutableInteractionSource() }, null, onClick = toggleRemoved))
                    }
                }
                if (gone) {
                    Text(
                        stringResource(R.string.page_removed_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, top = 8.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }

        TopBar(
            onBack = { leave() },
            backLabel = stringResource(R.string.back),
            title = session.name,
            titleVisible = scrolled,
        ) {
            HistoryButtons(session) { editing = null }
        }

        BottomEdgeBlur(height = 150.dp)
        Column(
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                drawing,
                enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 2 },
                exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 2 },
            ) {
                Column(Modifier.padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (tool == DrawTool.BRUSH) {
                        BrushSizePicker(brushSize) { brushSize = it }
                        Spacer(Modifier.height(8.dp))
                    }
                    Pill(stringResource(tool.hint), icon = tool.icon)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                DrawTools(if (drawing) tool else null) { picked ->
                    editing = null
                    drawing = picked != null
                    if (picked != null) tool = picked
                }
                Spacer(Modifier.width(12.dp))
                PrimaryButton(
                    text = stringResource(R.string.editor_save_short),
                    icon = PhosphorIcons.Regular.ShieldCheck,
                    modifier = Modifier.weight(1f),
                ) {
                    drawing = false
                    editing = null
                    saving = true
                }
            }
        }

        focus?.let { index ->
            FocusPage(session, index, vm::render, brushSize, { brushSize = it }, onSelect = { selection = it }) { focus = null }
        }
        if (adjusting && photo != null) AdjustPhoto(vm, photo) { adjusting = false }
        if (reviewing) ReviewMarks(session, vm::render, onSelect = { selection = it }) { reviewing = false }
    }
    selection?.let { mark -> SelectionSheet(vm, session, mark) { selection = null } }
    if (saving) SaveSheet(vm, session) { saving = false }
    if (hidden) HiddenDataSheet(meta.orEmpty(), null) { hidden = false }
    if (marking) {
        WatermarkDialog(session.watermark) {
            session.changeWatermark(it)
            marking = false
        }
    }
    if (renaming) {
        NameDialog(
            title = stringResource(R.string.docs_rename),
            initial = session.name.substringBeforeLast('.').ifBlank { session.name },
            placeholder = stringResource(R.string.docs_name),
            confirm = stringResource(R.string.done),
            onConfirm = { name ->
                session.rename(name)
                name.isNotBlank()
            },
            onDismiss = { renaming = false },
        )
    }
    if (leaving) {
        CoverDialog(
            title = stringResource(R.string.editor_leave_title),
            body = stringResource(if (session.recordId != null) R.string.editor_leave_body_saved else R.string.editor_leave_body),
            confirm = stringResource(R.string.editor_leave),
            dismiss = stringResource(R.string.editor_keep_editing),
            danger = true,
            onConfirm = {
                leaving = false
                vm.back()
            },
            onDismiss = { leaving = false },
        )
    }
}

@Composable
private fun WatermarkDialog(current: String?, onDone: (String?) -> Unit) {
    var text by remember { mutableStateOf(current.orEmpty()) }
    CoverDialog(
        title = stringResource(R.string.watermark_title),
        body = stringResource(R.string.watermark_body),
        confirm = stringResource(R.string.done),
        dismiss = stringResource(R.string.cancel),
        onConfirm = { onDone(text) },
        onDismiss = { onDone(current) },
    ) {
        Spacer(Modifier.height(16.dp))
        InputField(text, { text = it }, stringResource(R.string.watermark_hint), Modifier.fillMaxWidth(), leading = PhosphorIcons.Regular.Stamp, onDone = { onDone(text) })
    }
}

@Composable
private fun SearchCover(session: Session) {
    var query by rememberSaveable { mutableStateOf("") }
    val regex = remember(query) { query.trim().takeIf { it.length >= 2 }?.let { Regex(CustomRule.exact(it)) } }
    val count = regex?.let { session.countUncovered(it) } ?: 0
    val cover = { if (regex != null) session.coverAll(regex, Kind.CUSTOM) }
    InputField(
        value = query,
        onValueChange = { query = it },
        placeholder = stringResource(R.string.editor_search_hint),
        leading = PhosphorIcons.Regular.MagnifyingGlass,
        capitalization = KeyboardCapitalization.None,
        onDone = { cover() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        AnimatedVisibility(count > 0, enter = fadeIn(tween(160)) + scaleIn(tween(200), initialScale = 0.8f), exit = fadeOut(tween(120)) + scaleOut(tween(160), targetScale = 0.8f)) {
            Box(
                Modifier
                    .height(42.dp)
                    .pressable(scale = 0.92f) { cover() }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(pluralStringResource(R.plurals.cover_n, count, count), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
    AnimatedVisibility(regex != null && count == 0) {
        Text(
            stringResource(R.string.editor_search_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, top = 8.dp),
        )
    }
}

@Composable
private fun SelectionSheet(vm: MainViewModel, session: Session, mark: Mark, onClose: () -> Unit) {
    var keep by remember { mutableStateOf(false) }
    val same = remember(mark) { CustomRule.exact(mark.value) }
    val format = remember(mark) { if (CustomRule.hasFormat(mark.value)) CustomRule.format(mark.value) else null }
    val sameCount = session.countUncovered(Regex(same))
    val formatCount = format?.let { session.countUncovered(Regex(it)) } ?: 0
    fun apply(label: String, pattern: String) {
        session.coverAll(Regex(pattern), Kind.CUSTOM)
        if (keep) vm.prefs.addRule(label, pattern)
    }
    CoverSheet(onClose) { close ->
        SectionLabel(stringResource(R.string.sel_covered), Modifier.padding(start = 4.dp))
        Text(
            "“${mark.value}”",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        if (sameCount > 0) {
            OpenOption(PhosphorIcons.Regular.CopySimple, stringResource(R.string.sel_all_same), pluralStringResource(R.plurals.sel_more, sameCount, sameCount)) {
                close { apply(mark.value, same) }
            }
        }
        if (format != null && formatCount > sameCount) {
            OpenOption(
                PhosphorIcons.Regular.Hash,
                stringResource(R.string.sel_all_format, CustomRule.describe(mark.value)),
                pluralStringResource(R.plurals.sel_more, formatCount, formatCount),
            ) { close { apply(CustomRule.describe(mark.value), format) } }
        }
        Column(Modifier.fillMaxWidth().card()) {
            SettingRow(PhosphorIcons.Regular.ClockCounterClockwise, stringResource(R.string.sel_remember), stringResource(R.string.sel_remember_sub)) {
                CoverSwitch(keep) { keep = it }
            }
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(stringResource(R.string.sel_undo), Modifier.weight(1f)) {
                close { session.uncover(mark) }
            }
            PrimaryButton(stringResource(R.string.done), Modifier.weight(1f)) {
                close { if (keep) vm.prefs.addRule(mark.value, same) }
            }
        }
    }
}

@Composable
private fun TextAction(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(bottom = 10.dp)
            .clip(CircleShape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}

val DrawTool.icon: ImageVector
    get() = when (this) {
        DrawTool.LASSO -> PhosphorIcons.Regular.Lasso
        DrawTool.BOX -> PhosphorIcons.Regular.Selection
        DrawTool.BRUSH -> PhosphorIcons.Regular.PaintBrush
    }

val DrawTool.label: Int
    get() = when (this) {
        DrawTool.LASSO -> R.string.editor_draw
        DrawTool.BOX -> R.string.editor_box
        DrawTool.BRUSH -> R.string.editor_brush
    }

val DrawTool.hint: Int
    get() = when (this) {
        DrawTool.LASSO -> R.string.editor_draw_hint
        DrawTool.BOX -> R.string.editor_box_hint
        DrawTool.BRUSH -> R.string.editor_brush_hint
    }

@Composable
fun DrawTools(active: DrawTool?, onPick: (DrawTool?) -> Unit) {
    Row(
        Modifier
            .height(56.dp)
            .lift(CircleShape, 14.dp)
            .hairline(CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrawTool.entries.forEach { t ->
            val on = t == active
            val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else Color.Transparent, tween(180), label = "toolBg")
            val fg by animateColorAsState(if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, tween(180), label = "toolFg")
            Box(
                Modifier
                    .size(48.dp)
                    .pressable(scale = 0.88f) { onPick(if (on) null else t) }
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (on) PhosphorIcons.Regular.X else t.icon, stringResource(t.label), Modifier.size(21.dp), tint = fg)
            }
        }
    }
}

val BrushSizes = listOf(7.dp, 13.dp, 20.dp)

@Composable
fun BrushSizePicker(selected: Int, onPick: (Int) -> Unit) {
    Row(
        Modifier
            .height(48.dp)
            .lift(CircleShape, 10.dp)
            .hairline(CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrushSizes.forEachIndexed { i, size ->
            val on = i == selected
            val fill by animateColorAsState(if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline, tween(160), label = "brushDot")
            Box(Modifier.size(44.dp).pressable(scale = 0.85f) { onPick(i) }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(minOf(size * 1.1f, 30.dp)).clip(CircleShape).background(fill))
            }
        }
    }
}

@Composable
private fun MarkRow(mark: Mark, onToggle: (Boolean) -> Unit, onOpen: () -> Unit, onRemove: (() -> Unit)?) {
    val alpha by animateFloatAsState(if (mark.checked) 1f else 0.5f, tween(160), label = "markAlpha")
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null, onClick = onOpen)
            .padding(start = 12.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverCheckbox(mark.checked, onToggle)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).graphicsLayer { this.alpha = alpha }) {
            Text(
                mark.value.ifBlank { stringResource(mark.kind.label) },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(mark.kind.label) + " · " + stringResource(R.string.editor_page_short, mark.page + 1) +
                    if (!mark.checked) " · " + stringResource(R.string.editor_stays) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (onRemove != null) {
            Box(Modifier.size(36.dp).pressable(scale = 0.85f, onClick = onRemove), contentAlignment = Alignment.Center) {
                Icon(PhosphorIcons.Regular.X, stringResource(R.string.delete), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Icon(mark.kind.icon, null, Modifier.size(18.dp).graphicsLayer { this.alpha = alpha }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
