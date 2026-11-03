package com.coverit.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun UndoBar(key: Any?, message: String, action: String, onAction: () -> Unit, onTimeout: () -> Unit, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf<Pair<Any, String>?>(null) }
    if (key != null) shown = key to message
    LaunchedEffect(key) {
        if (key == null) return@LaunchedEffect
        delay(5000)
        onTimeout()
    }
    AnimatedVisibility(
        key != null,
        modifier = modifier,
        enter = fadeIn(tween(180)) + slideInVertically(tween(260)) { it },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .lift(RoundedCornerShape(16.dp), 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                shown?.second.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .pressable(scale = 0.92f, onClick = onAction)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
