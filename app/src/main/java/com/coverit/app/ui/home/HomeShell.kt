package com.coverit.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.fill.FolderSimple as FilledFolder
import com.adamglin.phosphoricons.fill.GearSix as FilledGear
import com.adamglin.phosphoricons.fill.House as FilledHouse
import com.adamglin.phosphoricons.regular.FolderSimple
import com.adamglin.phosphoricons.regular.GearSix
import com.adamglin.phosphoricons.regular.House
import com.coverit.app.R
import com.coverit.app.ui.AppActions
import com.coverit.app.ui.MainViewModel
import com.coverit.app.ui.Tab
import com.coverit.app.ui.common.SelectionBar
import com.coverit.app.ui.components.BottomEdgeBlur
import com.coverit.app.ui.components.NavBar
import com.coverit.app.ui.components.NavItem
import com.coverit.app.ui.components.TopEdgeBlur
import com.coverit.app.ui.library.LibraryTab
import com.coverit.app.ui.settings.SettingsTab
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun pagePadding(): PaddingValues {
    val density = LocalDensity.current
    val top = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val bottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    return PaddingValues(start = 20.dp, end = 20.dp, top = top + 28.dp, bottom = bottom + 120.dp)
}

val LocalTabVisible = staticCompositionLocalOf { true }

private val Blocked = Modifier.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
    }
}

@Composable
fun HomeShell(vm: MainViewModel, actions: AppActions) {
    val haze = remember { HazeState() }
    val items = listOf(
        NavItem(PhosphorIcons.Regular.House, PhosphorIcons.Fill.FilledHouse, stringResource(R.string.tab_home)),
        NavItem(PhosphorIcons.Regular.FolderSimple, PhosphorIcons.Fill.FilledFolder, stringResource(R.string.tab_library)),
        NavItem(PhosphorIcons.Regular.GearSix, PhosphorIcons.Fill.FilledGear, stringResource(R.string.tab_settings)),
    )
    Box(Modifier.fillMaxSize()) {
        // Each tab is built the first time it opens and then kept, like on iPhone: switching back is
        // only a fade, with no screen to build again, and every tab keeps where it was scrolled.
        val visited = remember { mutableStateListOf(vm.tab) }
        if (vm.tab !in visited) visited += vm.tab
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
            for (tab in Tab.entries) {
                if (tab !in visited) continue
                key(tab) {
                    val shown = tab == vm.tab
                    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(if (shown) 220 else 120), label = "tab")
                    Box(
                        Modifier
                            .fillMaxSize()
                            .zIndex(if (shown) 1f else 0f)
                            .graphicsLayer {
                                this.alpha = alpha
                                val s = 0.985f + 0.015f * alpha
                                scaleX = s
                                scaleY = s
                            }
                            .then(if (shown) Modifier else Blocked),
                    ) {
                        CompositionLocalProvider(LocalTabVisible provides shown) {
                            when (tab) {
                                Tab.HOME -> HomeTab(vm, actions)
                                Tab.LIBRARY -> LibraryTab(vm, actions)
                                Tab.SETTINGS -> SettingsTab(vm, actions)
                            }
                        }
                    }
                }
            }
        }
        TopEdgeBlur(extra = 20.dp)
        BottomEdgeBlur(height = 110.dp)
        BackHandler(vm.selected.isNotEmpty()) { vm.clearSelection() }
        SelectionBar(vm, actions, Modifier.align(Alignment.TopCenter))
        NavBar(
            items = items,
            selected = vm.tab.ordinal,
            haze = haze,
            onSelect = {
                vm.clearSelection()
                vm.tab = Tab.entries[it]
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 14.dp),
        )
    }
}
