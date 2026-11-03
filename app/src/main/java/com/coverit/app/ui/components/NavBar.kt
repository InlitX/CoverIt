package com.coverit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.coverit.app.ui.Tab
import com.coverit.app.ui.theme.Glass
import com.coverit.app.ui.theme.LocalIsDark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class NavItem(val icon: ImageVector, val selectedIcon: ImageVector, val label: String)

private enum class Start { TAP, MOVED, HELD }

@Composable
fun NavBar(items: List<NavItem>, selected: Int, haze: HazeState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val slot = 88.dp
    val pad = 5.dp
    val slotPx = with(LocalDensity.current) { slot.toPx() }
    val count = items.size
    val lens = remember { Animatable(selected * slotPx) }
    var dragging by remember { mutableStateOf(false) }
    var hover by remember { mutableIntStateOf(selected) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(selected)
    val select by rememberUpdatedState(onSelect)

    LaunchedEffect(selected, slotPx) {
        if (!dragging) {
            hover = selected
            lens.animateTo(selected * slotPx, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val swell by animateFloatAsState(if (dragging) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "swell")

    val dark = LocalIsDark.current
    val style = Glass.Bar
    Box(
        modifier
            .height(64.dp)
            .width(slot * count + pad * 2)
            .lift(CircleShape, 22.dp)
            .hairline(CircleShape)
            .clip(CircleShape)
            .hazeEffect(haze, style)
            .padding(pad)
            .pointerInput(count, slotPx) {
                fun slotAt(x: Float) = (x / slotPx).toInt().coerceIn(0, count - 1)
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val from = slotAt(down.position.x)
                    val start = withTimeoutOrNull(320L) {
                        var result: Start? = null
                        while (result == null) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                            result = when {
                                change == null || !change.pressed -> Start.TAP
                                abs(change.position.x - down.position.x) > viewConfiguration.touchSlop -> Start.MOVED
                                else -> null
                            }
                        }
                        result
                    } ?: Start.HELD
                    when {
                        start == Start.TAP -> {
                            if (from != current) select(from)
                            return@awaitEachGesture
                        }
                        start == Start.MOVED && from != current -> return@awaitEachGesture
                    }
                    dragging = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    var x = down.position.x
                    fun follow() {
                        val target = (x - slotPx / 2).coerceIn(0f, slotPx * (count - 1))
                        scope.launch { lens.animateTo(target, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessHigh)) }
                        val now = slotAt(x)
                        if (now != hover) {
                            hover = now
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                    follow()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        x = change.position.x
                        change.consume()
                        follow()
                    }
                    val landed = slotAt(x)
                    dragging = false
                    hover = landed
                    scope.launch { lens.animateTo(landed * slotPx, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) }
                    if (landed != current) select(landed)
                }
            },
    ) {
        Box(
            Modifier
                .offset { IntOffset(lens.value.roundToInt(), 0) }
                .width(slot)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = 1f + 0.1f * swell
                    scaleY = 1f + 0.14f * swell
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = (if (dark) 0.11f else 0.07f) + 0.05f * swell)),
        )
        Row {
            items.forEachIndexed { i, item ->
                val active = i == (if (dragging) hover else selected)
                val tint by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(180),
                    label = "navTint",
                )
                Column(
                    Modifier
                        .width(slot)
                        .fillMaxHeight()
                        .semantics {
                            role = Role.Tab
                            this.selected = i == selected
                            onClick(item.label) {
                                onSelect(i)
                                true
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(if (active) item.selectedIcon else item.icon, null, Modifier.size(22.dp), tint = tint)
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = MaterialTheme.typography.bodySmall.letterSpacing), color = tint, maxLines = 1)
                }
            }
        }
    }
}
