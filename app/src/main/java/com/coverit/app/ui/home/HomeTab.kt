package com.coverit.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowUpRight
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.Broom
import com.adamglin.phosphoricons.regular.FileImage
import com.adamglin.phosphoricons.regular.FileText
import com.adamglin.phosphoricons.regular.FolderSimple
import com.adamglin.phosphoricons.regular.FolderSimplePlus
import com.coverit.app.R
import com.coverit.app.data.Record
import com.coverit.app.ui.AppActions
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.Route
import com.coverit.app.ui.Tab
import com.coverit.app.ui.common.CoverSheet
import com.coverit.app.ui.common.LibraryCard
import com.coverit.app.ui.common.DocumentOptions
import com.coverit.app.ui.common.NameDialog
import com.coverit.app.ui.common.SpacePicker
import com.coverit.app.ui.common.OpenOption
import com.coverit.app.ui.common.icon
import com.coverit.app.ui.common.rememberThumb
import com.coverit.app.ui.common.spaceIcon
import com.coverit.app.ui.common.spaceName
import com.coverit.app.ui.components.AppMark
import com.coverit.app.ui.components.Entrance
import com.coverit.app.ui.components.Pill
import com.coverit.app.ui.components.PrimaryButton
import com.coverit.app.ui.components.RedactingLines
import com.coverit.app.ui.components.SectionLabel
import com.coverit.app.ui.components.card
import com.coverit.app.ui.components.hairline
import com.coverit.app.ui.components.pressable

private const val RECENT = 6

@Composable
fun HomeTab(vm: MainViewModel, actions: AppActions) {
    val library = vm.library
    var destination by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf<Record?>(null) }
    val recent = library.records.sortedByDescending { it.created }.take(RECENT)
    val starred = library.records.filter { it.starred }
    val folderName = library.folder(vm.folder)?.name ?: stringResource(R.string.docs_no_folder)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = pagePadding()) {
        item {
            Entrance(0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppMark(size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                        Text(stringResource(R.string.home_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Entrance(1) {
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.height(18.dp))
            Entrance(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile(PhosphorIcons.Regular.FileText, stringResource(R.string.docs_open_pdf), stringResource(R.string.docs_open_pdf_hint), Modifier.weight(1f), actions.openPdf)
                    ActionTile(PhosphorIcons.Regular.FileImage, stringResource(R.string.docs_open_photo), stringResource(R.string.docs_open_photo_hint), Modifier.weight(1f), actions.openPhoto)
                }
            }
            Spacer(Modifier.height(12.dp))
            Entrance(3) {
                OpenOption(PhosphorIcons.Regular.Broom, stringResource(R.string.clean_title), stringResource(R.string.clean_hint), PhosphorIcons.Regular.ArrowUpRight, accent = false, onClick = actions.cleanFiles)
            }
            Spacer(Modifier.height(14.dp))
            Entrance(3) {
                Row(
                    Modifier
                        .clip(CircleShape)
                        .clickable(remember { MutableInteractionSource() }, null) { destination = true }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(spaceIcon(vm.library, vm.prefs.space), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.home_saved_in, spaceName(vm.library, vm.prefs.space) + " · " + folderName),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(PhosphorIcons.Regular.CaretDown, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (library.records.isEmpty()) {
            item {
                Entrance(4) {
                    Column(Modifier.padding(top = 28.dp)) {
                        Box(Modifier.fillMaxWidth().card().padding(vertical = 30.dp), contentAlignment = Alignment.Center) { RedactingLines() }
                        Spacer(Modifier.height(18.dp))
                        Text(stringResource(R.string.docs_empty_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 4.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.docs_empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
        }

        if (starred.isNotEmpty()) {
            item {
                Entrance(4) {
                    Column {
                        SectionLabel(stringResource(R.string.docs_favorites), Modifier.padding(top = 26.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(starred, key = { it.id }) { r -> FavoriteCard(vm, r, onLongClick = { options = r }) { vm.push(Route.Detail(r.id)) } }
                        }
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            item {
                Entrance(5) {
                    Row(Modifier.padding(top = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel(stringResource(R.string.docs_recent), Modifier.weight(1f))
                        Text(
                            stringResource(R.string.home_see_all),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .clip(CircleShape)
                                .clickable(remember { MutableInteractionSource() }, null) { vm.tab = Tab.LIBRARY }
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            items(recent, key = { it.id }) { r ->
                Entrance(6 + recent.indexOf(r), Modifier.animateItem()) {
                    LibraryCard(vm, r)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (destination) DestinationSheet(vm) { destination = false }
    options?.let { r -> DocumentOptions(vm, r, actions) { options = null } }
}

@Composable
private fun ActionTile(icon: ImageVector, title: String, hint: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .aspectRatio(0.95f)
            .pressable(scale = 0.96f, onClick = onClick)
            .card()
            .padding(16.dp),
    ) {
        Row {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.weight(1f))
            Icon(PhosphorIcons.Regular.ArrowUpRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DestinationSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    var space by remember { mutableStateOf(vm.prefs.space) }
    var folder by remember { mutableStateOf(vm.folder) }
    var creating by remember { mutableStateOf(false) }
    CoverSheet(onDismiss) { close ->
        Text(stringResource(R.string.home_destination), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp))
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
                vm.switchSpace(space)
                vm.folder = folder
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

@Composable
private fun FavoriteCard(vm: MainViewModel, record: Record, onLongClick: () -> Unit, onClick: () -> Unit) {
    val thumb = rememberThumb(vm.library, record, 320)
    Column(
        Modifier
            .width(132.dp)
            .pressable(scale = 0.96f, onLongClick = onLongClick, onClick = onClick)
            .card()
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .hairline(RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White),
        ) {
            thumb?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter) }
        }
        Spacer(Modifier.height(8.dp))
        Text(record.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                record.type.icon,
                null,
                Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                pluralStringResource(R.plurals.covered_n, record.covered.size, record.covered.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
