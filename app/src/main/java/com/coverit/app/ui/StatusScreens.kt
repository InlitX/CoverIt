package com.coverit.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.coverit.app.R
import com.coverit.app.ui.components.AnimatedSeal
import com.coverit.app.ui.components.RedactingLines
import com.coverit.app.ui.components.Reveal
import com.coverit.app.ui.components.SecondaryButton

@Composable
fun WorkingScreen(message: Int) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RedactingLines()
        Spacer(Modifier.height(34.dp))
        AnimatedContent(
            targetState = message,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
            label = "working",
        ) {
            Text(stringResource(it), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.working_local), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FailedScreen(message: Int, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        AnimatedSeal(success = false, size = 64.dp)
        Spacer(Modifier.height(24.dp))
        Reveal(1) { Text(stringResource(R.string.failed_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) }
        Spacer(Modifier.height(6.dp))
        Reveal(2) { Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(28.dp))
        Reveal(3) { SecondaryButton(stringResource(R.string.back), Modifier.fillMaxWidth(), PhosphorIcons.Regular.ArrowLeft, onClick = onBack) }
    }
}
