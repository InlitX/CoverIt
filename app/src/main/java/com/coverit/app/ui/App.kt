package com.coverit.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coverit.app.R
import com.coverit.app.data.Record
import com.coverit.app.ui.components.UndoBar
import com.coverit.app.ui.editor.EditorScreen
import com.coverit.app.ui.home.HomeShell
import com.coverit.app.ui.library.DetailScreen
import com.coverit.app.ui.onboarding.OnboardingScreen
import com.coverit.app.ui.result.CleanedScreen
import com.coverit.app.ui.result.ResultScreen
import com.coverit.app.ui.settings.AboutScreen
import com.coverit.app.ui.settings.DetectionScreen
import com.coverit.app.ui.settings.PrivacyScreen

class AppActions(
    val openPdf: () -> Unit,
    val openPhoto: () -> Unit,
    val cleanFiles: () -> Unit,
    val share: (Record, Boolean) -> Unit,
    val shareMany: (List<Record>) -> Unit,
    val save: (Record) -> Unit,
    val openUrl: (String) -> Unit,
    val setLanguage: (AppLanguage) -> Unit,
)

@Composable
fun App(vm: MainViewModel, actions: AppActions) {
    val depth = vm.stack.size
    BackHandler(enabled = depth > 1) { vm.back() }
    Box(Modifier.fillMaxSize()) {
        Routes(vm, actions)
        val undo = vm.undo
        UndoBar(
            key = undo,
            message = undo?.let { pluralStringResource(it.message, it.count, it.count) }.orEmpty(),
            action = stringResource(R.string.undo),
            onAction = { vm.undoLast() },
            onTimeout = { undo?.let(vm::settle) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, bottom = if (vm.route == Route.Home) 92.dp else 130.dp),
        )
    }
}

@Composable
private fun Routes(vm: MainViewModel, actions: AppActions) {
    AnimatedContent(
        targetState = vm.stack.toList(),
        contentKey = { stack -> stack.size to stack.last()::class },
        transitionSpec = {
            val forward = targetState.size >= initialState.size
            val enter = slideInHorizontally(tween(320)) { if (forward) it / 5 else -it / 5 } + fadeIn(tween(320))
            val exit = slideOutHorizontally(tween(320)) { if (forward) -it / 6 else it / 6 } + fadeOut(tween(220))
            enter togetherWith exit
        },
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        label = "routes",
    ) { stack ->
        when (val route = stack.last()) {
            Route.Home -> HomeShell(vm, actions)
            Route.Onboarding -> OnboardingScreen(vm)
            is Route.Working -> WorkingScreen(route.message)
            Route.Editor -> vm.session?.let { EditorScreen(vm, it) } ?: WorkingScreen(R.string.working_reading)
            is Route.Result -> ResultScreen(vm, route, actions)
            is Route.Cleaned -> CleanedScreen(vm, route, actions)
            is Route.Detail -> DetailScreen(vm, route.recordId, actions)
            Route.Detection -> DetectionScreen(vm)
            Route.About -> AboutScreen(vm, actions)
            Route.Privacy -> PrivacyScreen(vm, actions)
            is Route.Failed -> FailedScreen(route.message) { vm.back() }
        }
    }
}
