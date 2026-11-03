package com.coverit.app.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.FileImage
import com.adamglin.phosphoricons.regular.FileText
import com.adamglin.phosphoricons.regular.FolderSimple
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.Plus
import com.adamglin.phosphoricons.regular.Trash
import com.adamglin.phosphoricons.regular.Tray
import com.coverit.app.R
import com.coverit.app.data.Folder
import com.coverit.app.data.Record
import com.coverit.app.data.SPACE_ICONS
import com.coverit.app.data.Space
import com.coverit.app.detect.text.Fold
import com.coverit.app.doc.Family
import com.coverit.app.ui.AppActions
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.common.CoverSheet
import com.coverit.app.ui.common.LibraryCard
import com.coverit.app.ui.common.NameDialog
import com.coverit.app.ui.common.NewButton
import com.coverit.app.ui.common.NewDocumentSheet
import com.coverit.app.ui.common.OpenOption
import com.coverit.app.ui.common.folderPages
import com.coverit.app.ui.common.spaceIcon
import com.coverit.app.ui.common.spaceIconFor
import com.coverit.app.ui.common.spaceName
import com.coverit.app.ui.components.CoverCheckbox
import com.coverit.app.ui.components.CoverDialog
import com.coverit.app.ui.components.Entrance
import com.coverit.app.ui.components.FolderArt
import com.coverit.app.ui.components.FolderCell
import com.coverit.app.ui.components.FolderLayer
import com.coverit.app.ui.components.GlassIconButton
import com.coverit.app.ui.components.IconBadge
import com.coverit.app.ui.components.InputField
import com.coverit.app.ui.components.Pill
import com.coverit.app.ui.components.RowDivider
import com.coverit.app.ui.components.SectionLabel
import com.coverit.app.ui.components.SettingRow
import com.coverit.app.ui.components.card
import com.coverit.app.ui.components.folderSheetRect
import com.coverit.app.ui.components.pressable
import com.coverit.app.ui.home.LocalTabVisible
import com.coverit.app.ui.home.pagePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LOOSE = "loose"

private enum class Sort { RECENT, NAME }

private enum class Filter(val label: Int, val keep: (Record) -> Boolean) {
    ALL(R.string.filter_all, { true }),
    STARRED(R.string.filter_starred, { it.starred }),
    PDF(R.string.filter_pdf, { it.type.family == Family.PDF }),
    PHOTOS(R.string.filter_photos, { it.type.family == Family.IMAGE }),
    OFFICE(R.string.filter_office, { it.type.family == Family.OFFICE }),
    REVIEW(R.string.filter_review, { !it.verified }),
}

@Composable
private fun FilterRow(records: List<Record>, filter: Filter, sort: Sort?, onSort: (Sort) -> Unit, onFilter: (Filter) -> Unit) {
    val offered = Filter.entries.filter { f -> f == Filter.ALL || f == filter || records.any(f.keep) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (sort != null) {
            item { Pill(stringResource(R.string.docs_sort_recent), selected = sort == Sort.RECENT) { onSort(Sort.RECENT) } }
            item { Pill(stringResource(R.string.docs_sort_name), selected = sort == Sort.NAME) { onSort(Sort.NAME) } }
            item { Spacer(Modifier.width(4.dp)) }
        }
        if (offered.size > 2) {
            items(offered, key = { it.name }) { f -> Pill(stringResource(f.label), selected = f == filter) { onFilter(f) } }
        }
    }
}

@Composable
fun LibraryTab(vm: MainViewModel, actions: AppActions) {
    val library = vm.library
    val space = vm.prefs.space
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    var home by remember { mutableStateOf<Rect?>(null) }
    var dealing by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var returning by remember { mutableStateOf<String?>(null) }
    val ghost = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val origins = remember { mutableStateMapOf<String, Rect>() }
    var root by remember { mutableStateOf(Offset.Zero) }

    fun openFolder(key: String) {
        if (closing) return
        vm.folder = key.takeIf { it != LOOSE }
        home = origins[key]
        returning = null
        dealing = true
        open = key
        scope.launch {
            ghost.snapTo(1f)
            ghost.animateTo(0f, tween(345, delayMillis = 115, easing = FastOutSlowInEasing))
        }
        scope.launch {
            delay(1100)
            dealing = false
        }
    }

    fun closeFolder() {
        if (closing || open == null) return
        val key = open
        if (home == null) {
            open = null
            vm.folder = null
            return
        }
        closing = true
        dealing = false
        scope.launch {
            launch { ghost.animateTo(1f, tween(200)) }
            delay(470)
            returning = key
            open = null
            closing = false
            home = null
            vm.folder = null
            ghost.snapTo(0f)
        }
    }

    val current = open
    if (current != null && current != LOOSE && library.folder(current)?.space != space) {
        LaunchedEffect(current) {
            open = null
            vm.folder = null
        }
    }

    BackHandler(open != null && LocalTabVisible.current) { closeFolder() }

    Box(Modifier.fillMaxSize().onGloballyPositioned { root = it.positionInRoot() }) {
        val key = open
        if (key == null) {
            FolderGrid(vm, actions, returning, origins) { openFolder(it) }
        } else {
            val ghostHome = home
            if (ghostHome != null) Ghost(ghostHome, root, ghost.value, FolderLayer.BACK, key)
            FolderView(
                vm = vm,
                actions = actions,
                key = key,
                home = home.takeIf { dealing || closing },
                closing = closing,
                onBack = { closeFolder() },
            )
            if (ghostHome != null) Ghost(ghostHome, root, ghost.value, FolderLayer.FRONT, key)
        }
    }
}

@Composable
private fun Ghost(home: Rect, root: Offset, value: Float, layer: FolderLayer, key: String) {
    if (value <= 0.001f) return
    val density = LocalDensity.current
    Box(
        Modifier
            .layoutAt(home, root)
            .graphicsLayer {
                alpha = value
                val s = 0.86f + 0.14f * value
                scaleX = s
                scaleY = s
                translationY = home.height * 0.12f * (1f - value)
            },
    ) {
        with(density) {
            FolderArt(
                if (key == LOOSE) PhosphorIcons.Regular.Tray else PhosphorIcons.Regular.FolderSimple,
                papers = 0,
                modifier = Modifier.width(home.width.toDp()),
                layer = layer,
            )
        }
    }
}

private fun Modifier.layoutAt(rect: Rect, root: Offset) =
    offset { IntOffset((rect.left - root.x).toInt(), (rect.top - root.y).toInt()) }

@Composable
private fun FolderGrid(
    vm: MainViewModel,
    actions: AppActions,
    returning: String?,
    origins: MutableMap<String, Rect>,
    onOpen: (String) -> Unit,
) {
    val library = vm.library
    val space = vm.prefs.space
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var spaceSheet by remember { mutableStateOf(false) }
    var newFolder by remember { mutableStateOf(false) }
    var folderMenu by remember { mutableStateOf<Folder?>(null) }
    val inSpace = library.records.filter { it.space == space }
    val folders = library.foldersIn(space)
    val loose = inSpace.filter { it.folder == null }
    val folded = Fold.text(query.trim())
    val searching = folded.isNotEmpty() || filter != Filter.ALL
    val results = if (!searching) emptyList() else inSpace.filter { filter.keep(it) && (folded.isEmpty() || Fold.text(it.name).contains(folded)) }.sortedByDescending { it.created }
    val play = returning == null

    LazyColumn(Modifier.fillMaxSize(), contentPadding = pagePadding()) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).pressable(scale = 0.97f) { spaceSheet = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(spaceName(library, space), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.width(8.dp))
                        Icon(PhosphorIcons.Regular.CaretDown, stringResource(R.string.space_pick), Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Text(
                        pluralStringResource(R.plurals.documents_n, inSpace.size, inSpace.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                GlassIconButton(PhosphorIcons.Regular.Plus, stringResource(R.string.docs_new_folder)) { newFolder = true }
            }
            if (inSpace.isNotEmpty()) {
                InputField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.docs_search_hint),
                    leading = PhosphorIcons.Regular.MagnifyingGlass,
                    capitalization = KeyboardCapitalization.None,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                )
                Spacer(Modifier.height(12.dp))
                FilterRow(inSpace, filter, null, {}) { filter = it }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (searching) {
            if (results.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.docs_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }
            items(results, key = { it.id }) { r ->
                LibraryCard(vm, r, Modifier.animateItem())
                Spacer(Modifier.height(12.dp))
            }
        } else {
            val cells = buildList {
                folders.forEach { add(it.id) }
                if (loose.isNotEmpty() || folders.isEmpty()) add(LOOSE)
            }
            cells.chunked(2).forEachIndexed { row, pair ->
                item(key = "row" + pair.joinToString()) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        pair.forEachIndexed { col, key ->
                            val index = row * 2 + col
                            val folder = library.folder(key)
                            val inside = if (key == LOOSE) loose else inSpace.filter { it.folder == key }
                            Entrance(index, Modifier.weight(1f), play = play || returning != key) {
                                FolderCell(
                                    title = folder?.name ?: stringResource(R.string.docs_no_folder),
                                    subtitle = pluralStringResource(R.plurals.documents_n, inside.size, inside.size),
                                    icon = if (folder == null) PhosphorIcons.Regular.Tray else PhosphorIcons.Regular.FolderSimple,
                                    papers = inside.size,
                                    pictures = folderPages(library, inside.sortedByDescending { it.created }),
                                    artModifier = Modifier.onGloballyPositioned { origins[key] = it.boundsInRoot() },
                                    onMenu = folder?.let { f -> { folderMenu = f } },
                                ) { onOpen(key) }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (folders.isEmpty()) {
                item {
                    Entrance(2, play = play) {
                        Text(
                            stringResource(R.string.library_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }

    if (spaceSheet) SpaceSheet(vm) { spaceSheet = false }
    if (newFolder) {
        NameDialog(
            title = stringResource(R.string.docs_new_folder),
            initial = "",
            placeholder = stringResource(R.string.docs_folder_name),
            confirm = stringResource(R.string.add),
            onConfirm = { library.addFolder(it, space) != null },
            onDismiss = { newFolder = false },
        )
    }
    folderMenu?.let { f -> FolderMenu(vm, f) { folderMenu = null } }
}

@Composable
private fun FolderView(
    vm: MainViewModel,
    actions: AppActions,
    key: String,
    home: Rect?,
    closing: Boolean,
    onBack: () -> Unit,
) {
    val library = vm.library
    val space = vm.prefs.space
    val folder = library.folder(key)
    var sort by rememberSaveable { mutableStateOf(Sort.RECENT) }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var newSheet by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val everything = library.recordsIn(space, folder?.id)
    val docs = everything.filter(filter.keep).let { all ->
        when (sort) {
            Sort.RECENT -> all.sortedByDescending { it.created }
            Sort.NAME -> all.sortedBy { it.name.lowercase() }
        }
    }
    val title = folder?.name ?: stringResource(R.string.docs_no_folder)
    val fade by animateFloatAsState(if (closing) 0f else 1f, tween(200), label = "folderFade")

    LazyColumn(Modifier.fillMaxSize(), contentPadding = pagePadding()) {
        item {
            Entrance(0, Modifier.graphicsLayer { alpha = fade }, play = !closing) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassIconButton(PhosphorIcons.Regular.ArrowLeft, stringResource(R.string.back), onClick = onBack)
                        Spacer(Modifier.weight(1f))
                        if (folder != null) {
                            GlassIconButton(PhosphorIcons.Regular.DotsThree, stringResource(R.string.docs_folder_options)) { menu = true }
                            Spacer(Modifier.width(8.dp))
                        }
                        NewButton { newSheet = true }
                    }
                    Spacer(Modifier.height(20.dp))
                    SectionLabel(spaceName(library, space))
                    Text(title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(pluralStringResource(R.plurals.documents_n, docs.size, docs.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (everything.size > 1) {
                        Spacer(Modifier.height(16.dp))
                        FilterRow(everything, filter, sort, { sort = it }) { filter = it }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
        if (docs.isEmpty()) {
            item {
                Entrance(1, Modifier.graphicsLayer { alpha = fade }, play = !closing) {
                    Column {
                        Text(
                            stringResource(R.string.docs_folder_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 16.dp),
                        )
                        OpenOption(PhosphorIcons.Regular.FileText, stringResource(R.string.docs_open_pdf), stringResource(R.string.docs_open_pdf_hint), onClick = actions.openPdf)
                        Spacer(Modifier.height(12.dp))
                        OpenOption(PhosphorIcons.Regular.FileImage, stringResource(R.string.docs_open_photo), stringResource(R.string.docs_open_photo_hint), onClick = actions.openPhoto)
                    }
                }
            }
        }
        items(docs, key = { it.id }) { r ->
            val index = docs.indexOf(r)
            Deal(home, index, closing, Modifier.animateItem()) {
                LibraryCard(vm, r)
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (newSheet) NewDocumentSheet(actions) { newSheet = false }
    if (menu && folder != null) FolderMenu(vm, folder) { menu = false }
}

@Composable
private fun Deal(home: Rect?, index: Int, closing: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dealt = remember { home != null }
    val progress = remember { Animatable(if (dealt) 0f else 1f) }
    var target by remember { mutableStateOf<Rect?>(null) }
    val measured = target != null
    LaunchedEffect(measured) {
        if (dealt && measured) {
            delay(40L * index.coerceIn(0, 7))
            progress.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 420f))
        }
    }
    LaunchedEffect(closing) {
        if (closing && home != null) {
            delay(18L * (6 - index.coerceIn(0, 6)))
            progress.animateTo(0f, spring(dampingRatio = 1f, stiffness = 650f))
        }
    }
    Box(
        modifier
            .onGloballyPositioned { target = it.boundsInRoot() }
            .graphicsLayer {
                val h = home
                val t = target
                if (h == null || t == null || t.width <= 0f) {
                    alpha = if (dealt && t == null) 0f else 1f
                    return@graphicsLayer
                }
                val sheet = folderSheetRect(h, index)
                val settle = progress.value
                val away = 1f - settle
                translationX = (sheet.center.x - t.center.x) * away
                translationY = (sheet.center.y - t.center.y) * away
                val small = (sheet.width / t.width).coerceIn(0.2f, 1f)
                val s = small + (1f - small) * settle
                scaleX = s
                scaleY = s
                alpha = (settle * 2.2f).coerceIn(0f, 1f)
            },
    ) { content() }
}

@Composable
fun SpaceSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Space?>(null) }
    CoverSheet(onDismiss) { close ->
        Text(stringResource(R.string.space_pick), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp))
        Text(stringResource(R.string.space_pick_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Column(Modifier.fillMaxWidth().card()) {
            vm.library.spaces.forEachIndexed { i, s ->
                if (i > 0) RowDivider()
                val count = vm.library.records.count { it.space == s }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pressable(scale = 0.98f) { close { vm.switchSpace(s) } }
                        .padding(start = 16.dp, end = 12.dp, top = 13.dp, bottom = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(spaceIcon(vm.library, s))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(spaceName(vm.library, s), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(pluralStringResource(R.plurals.documents_n, count, count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!s.builtIn) {
                        Box(Modifier.size(36.dp).pressable(scale = 0.85f) { editing = s }, contentAlignment = Alignment.Center) {
                            Icon(PhosphorIcons.Regular.DotsThree, stringResource(R.string.space_edit), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    CoverCheckbox(s == vm.prefs.space) { close { vm.switchSpace(s) } }
                }
            }
            RowDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(scale = 0.98f) { creating = true }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(PhosphorIcons.Regular.Plus)
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.space_new), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    if (creating) {
        SpaceDialog(
            title = stringResource(R.string.space_new),
            initialName = "",
            initialIcon = SPACE_ICONS.first(),
            onConfirm = { name, icon -> vm.library.addSpace(name, icon)?.also { vm.switchSpace(it) } != null },
            onDismiss = { creating = false },
        )
    }
    editing?.let { space ->
        val info = vm.library.spaceInfo(space)
        if (info != null) {
            SpaceDialog(
                title = stringResource(R.string.space_edit),
                initialName = info.name,
                initialIcon = info.icon,
                onConfirm = { name, icon -> vm.library.updateSpace(space, name, icon) },
                onDelete = { vm.deleteSpace(space) },
                onDismiss = { editing = null },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpaceDialog(
    title: String,
    initialName: String,
    initialIcon: String,
    onConfirm: (String, String) -> Boolean,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete && onDelete != null) {
        CoverDialog(
            title = name,
            body = stringResource(R.string.space_delete_body),
            confirm = stringResource(R.string.delete),
            dismiss = stringResource(R.string.cancel),
            danger = true,
            onConfirm = {
                onDelete()
                onDismiss()
            },
            onDismiss = { confirmDelete = false },
        )
        return
    }
    CoverDialog(
        title = title,
        body = "",
        confirm = stringResource(R.string.done),
        dismiss = stringResource(R.string.cancel),
        onConfirm = { if (onConfirm(name, icon)) onDismiss() },
        onDismiss = onDismiss,
    ) {
        Spacer(Modifier.height(16.dp))
        InputField(
            name,
            { name = it },
            stringResource(R.string.space_name),
            Modifier.fillMaxWidth(),
            leading = spaceIconFor(icon),
            onDone = { if (onConfirm(name, icon)) onDismiss() },
        )
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SPACE_ICONS.forEach { key ->
                val on = key == icon
                Box(
                    Modifier
                        .size(44.dp)
                        .pressable(scale = 0.9f) { icon = key }
                        .clip(CircleShape)
                        .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(spaceIconFor(key), null, Modifier.size(20.dp), tint = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        if (onDelete != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.space_delete),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clip(CircleShape)
                    .pressable(scale = 0.95f) { confirmDelete = true }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }
    }
}

private enum class FolderAction { MENU, RENAME, DELETE }

@Composable
fun FolderMenu(vm: MainViewModel, folder: Folder, onDismiss: () -> Unit) {
    var action by remember { mutableStateOf(FolderAction.MENU) }
    when (action) {
        FolderAction.MENU -> CoverSheet(onDismiss) { _ ->
            Text(folder.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            Column(Modifier.fillMaxWidth().card()) {
                SettingRow(PhosphorIcons.Regular.PencilSimple, stringResource(R.string.docs_rename_folder), onClick = { action = FolderAction.RENAME })
                RowDivider()
                SettingRow(
                    PhosphorIcons.Regular.Trash,
                    stringResource(R.string.docs_delete_folder),
                    stringResource(R.string.docs_delete_folder_body),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { action = FolderAction.DELETE },
                )
            }
        }
        FolderAction.RENAME -> NameDialog(
            title = stringResource(R.string.docs_rename_folder),
            initial = folder.name,
            placeholder = stringResource(R.string.docs_folder_name),
            confirm = stringResource(R.string.done),
            onConfirm = { vm.library.renameFolder(folder.id, it) },
            onDismiss = onDismiss,
        )
        FolderAction.DELETE -> CoverDialog(
            title = folder.name,
            body = stringResource(R.string.docs_delete_folder_body),
            confirm = stringResource(R.string.delete),
            dismiss = stringResource(R.string.cancel),
            danger = true,
            onConfirm = {
                vm.deleteFolder(folder.id)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}
