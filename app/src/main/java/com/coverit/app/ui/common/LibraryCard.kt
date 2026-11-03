package com.coverit.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.Star as FilledStar
import com.adamglin.phosphoricons.regular.DotsThree
import com.adamglin.phosphoricons.regular.FolderSimple
import com.adamglin.phosphoricons.regular.ShareFat
import com.adamglin.phosphoricons.regular.Star
import com.adamglin.phosphoricons.regular.Trash
import com.adamglin.phosphoricons.regular.X
import com.coverit.app.R
import com.coverit.app.data.Record
import com.coverit.app.ui.AppActions
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.Route
import com.coverit.app.ui.components.CardShape
import com.coverit.app.ui.components.GlassIconButton
import com.coverit.app.ui.components.lift
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryCard(vm: MainViewModel, record: Record, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val selecting = vm.selected.isNotEmpty()
    val state = rememberSwipeToDismissBoxState(positionalThreshold = { it * 0.32f })
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = !selecting,
        enableDismissFromEndToStart = !selecting,
        onDismiss = { direction ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                vm.deleteRecords(listOf(record.id))
            } else {
                vm.starRecords(listOf(record.id), !record.starred)
                scope.launch { state.reset() }
            }
        },
        backgroundContent = { SwipeBackground(state.dismissDirection, record.starred) },
    ) {
        DocumentCard(
            vm.library,
            record,
            selected = if (selecting) record.id in vm.selected else null,
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.toggleSelected(record.id)
            },
        ) {
            if (selecting) vm.toggleSelected(record.id) else vm.push(Route.Detail(record.id))
        }
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue, starred: Boolean) {
    if (direction == SwipeToDismissBoxValue.Settled) return
    val delete = direction == SwipeToDismissBoxValue.EndToStart
    Box(
        Modifier
            .fillMaxSize()
            .clip(CardShape)
            .background(if (delete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            .padding(horizontal = 24.dp),
        contentAlignment = if (delete) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Icon(
            when {
                delete -> PhosphorIcons.Regular.Trash
                starred -> PhosphorIcons.Regular.Star
                else -> PhosphorIcons.Fill.FilledStar
            },
            null,
            Modifier.size(24.dp),
            tint = if (delete) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
fun SelectionBar(vm: MainViewModel, actions: AppActions, modifier: Modifier = Modifier) {
    val records = vm.selected.mapNotNull { vm.library.find(it) }
    var moving by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf(false) }
    if (records.isEmpty()) return
    val allStarred = records.all { it.starred }
    Row(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .lift(CardShape, 14.dp)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        GlassIconButton(PhosphorIcons.Regular.X, stringResource(R.string.cancel), size = 40.dp) { vm.clearSelection() }
        Spacer(Modifier.width(6.dp))
        Text(records.size.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        GlassIconButton(PhosphorIcons.Regular.ShareFat, stringResource(R.string.share), size = 40.dp) {
            actions.shareMany(records)
        }
        GlassIconButton(if (allStarred) PhosphorIcons.Fill.FilledStar else PhosphorIcons.Regular.Star, stringResource(if (allStarred) R.string.docs_unstar else R.string.docs_star), size = 40.dp) {
            vm.starRecords(records.map { it.id }, !allStarred)
        }
        GlassIconButton(PhosphorIcons.Regular.FolderSimple, stringResource(R.string.docs_move), size = 40.dp) { moving = true }
        GlassIconButton(PhosphorIcons.Regular.Trash, stringResource(R.string.delete), size = 40.dp) {
            vm.deleteRecords(records.map { it.id })
        }
        if (records.size == 1) {
            GlassIconButton(PhosphorIcons.Regular.DotsThree, stringResource(R.string.more), size = 40.dp) { options = true }
        }
    }
    if (moving) MoveSheet(vm, records) { moving = false }
    if (options && records.size == 1) {
        DocumentOptions(vm, records[0], actions) {
            options = false
            vm.clearSelection()
        }
    }
}
