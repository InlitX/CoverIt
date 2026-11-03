package com.coverit.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.CaretRight
import com.coverit.app.ui.theme.LocalIsDark
import com.coverit.app.ui.theme.borderColor
import kotlinx.coroutines.delay

val CardShape = RoundedCornerShape(22.dp)
val ButtonShape = RoundedCornerShape(18.dp)

@Composable
fun Modifier.hairline(shape: Shape): Modifier = border(0.5.dp, borderColor, shape)

@Composable
fun Modifier.lift(shape: Shape, elevation: Dp = 18.dp): Modifier {
    val dark = LocalIsDark.current
    return shadow(
        elevation = elevation,
        shape = shape,
        spotColor = Color.Black.copy(alpha = if (dark) 0.5f else 0.14f),
        ambientColor = Color.Black.copy(alpha = if (dark) 0.3f else 0.06f),
    )
}

@Composable
fun Modifier.pressable(enabled: Boolean = true, scale: Float = 0.95f, onLongClick: (() -> Unit)? = null, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val value by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = if (pressed) tween(90, easing = FastOutSlowInEasing) else tween(260, easing = EaseOutBack),
        label = "press",
    )
    return this
        .graphicsLayer {
            scaleX = value
            scaleY = value
        }
        .combinedClickable(interactionSource = interaction, indication = null, enabled = enabled, onLongClick = onLongClick, onClick = onClick)
}

@Composable
fun Modifier.card(color: Color = MaterialTheme.colorScheme.surface): Modifier = this
    .hairline(CardShape)
    .clip(CardShape)
    .background(color)

@Composable
fun Reveal(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(55L * index)
        progress.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
    }
    val shift = with(LocalDensity.current) { 16.dp.toPx() }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * shift
        },
    ) { content() }
}

@Composable
fun GlassIconButton(icon: ImageVector, description: String, size: Dp = 44.dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .pressable(scale = 0.9f, onClick = onClick)
            .lift(CircleShape, 12.dp)
            .hairline(CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, Modifier.size(size * 0.46f), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, tween(160), label = "enabled")
    Row(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { this.alpha = alpha }
            .pressable(enabled = enabled, scale = 0.97f, onClick = onClick)
            .lift(ButtonShape)
            .clip(ButtonShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(10.dp))
        }
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                (fadeIn(tween(200)) + slideInVertically(tween(220)) { it / 2 }) togetherWith
                    (fadeOut(tween(120)) + slideOutVertically(tween(160)) { -it / 2 })
            },
            label = "primaryText",
        ) {
            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .pressable(scale = 0.97f, onClick = onClick)
            .hairline(ButtonShape)
            .clip(ButtonShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = tint)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1)
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tween(180),
        label = "pillBg",
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        tween(180),
        label = "pillFg",
    )
    Row(
        modifier = modifier
            .height(36.dp)
            .then(if (onClick != null) Modifier.pressable(scale = 0.94f, onClick = onClick) else Modifier)
            .hairline(CircleShape)
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(16.dp), tint = fg)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

@Composable
fun CoverCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val on = MaterialTheme.colorScheme.primary
    val fill by animateColorAsState(if (checked) on else Color.Transparent, tween(120), label = "fill")
    val edge by animateColorAsState(if (checked) on else MaterialTheme.colorScheme.outline, tween(120), label = "edge")
    val tick by animateFloatAsState(if (checked) 1f else 0f, tween(180), label = "tick")
    val boxSize by animateDpAsState(if (checked) 22.dp else 20.dp, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "size")
    val tickColor = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = Modifier
            .requiredSize(32.dp)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(boxSize)) {
            val stroke = 1.4.dp.toPx()
            val radius = CornerRadius(7.dp.toPx())
            drawRoundRect(fill, cornerRadius = radius)
            drawRoundRect(edge, Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke), radius, style = Stroke(stroke))
            if (tick > 0f) {
                val path = Path().apply {
                    moveTo(size.width * 0.29f, size.height * 0.52f)
                    lineTo(size.width * 0.44f, size.height * 0.66f)
                    lineTo(size.width * 0.72f, size.height * 0.36f)
                }
                drawPath(path, tickColor.copy(alpha = tick), style = Stroke(1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
fun CoverSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val track by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        tween(180),
        label = "track",
    )
    val thumbColor by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(180),
        label = "thumb",
    )
    val offset by animateDpAsState(if (checked) 20.dp else 0.dp, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow), label = "offset")
    Box(
        Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(CircleShape)
            .background(track)
            .toggleable(checked, remember { MutableInteractionSource() }, null, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(4.dp),
    ) {
        Box(Modifier.offset(x = offset).size(20.dp).clip(CircleShape).background(thumbColor))
    }
}

@Composable
fun Segmented(options: List<String>, index: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
    ) {
        val slot = maxWidth / options.size
        val x by animateDpAsState(slot * index, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow), label = "segment")
        Box(
            Modifier
                .offset(x = x)
                .width(slot)
                .fillMaxHeight()
                .lift(RoundedCornerShape(11.dp), 4.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surface),
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                val color by animateColorAsState(
                    if (i == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(160),
                    label = "segmentText",
                )
                Box(
                    Modifier
                        .width(slot)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, null) { onChange(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = if (options.size >= 4 || options.any { it.length > 12 }) MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp) else MaterialTheme.typography.labelMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 6.dp, bottom = 10.dp),
    )
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
    }
}

@Composable
fun RowDivider(indent: Dp = 64.dp) {
    HorizontalDivider(Modifier.padding(start = indent, end = 16.dp), 0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = tint)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
fun NavRow(icon: ImageVector, title: String, subtitle: String? = null, value: String? = null, onClick: () -> Unit) {
    SettingRow(icon, title, subtitle, onClick = onClick) {
        if (value != null) {
            Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
        }
        Icon(PhosphorIcons.Regular.CaretRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BoxScope.TopEdgeBlur(extra: Dp = 72.dp) {
    val inset = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    val background = MaterialTheme.colorScheme.background
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(inset + extra)
            .background(Brush.verticalGradient(0f to background, 0.72f to background, 1f to background.copy(alpha = 0f))),
    )
}

@Composable
fun BoxScope.BottomEdgeBlur(height: Dp = 140.dp) {
    val inset = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val background = MaterialTheme.colorScheme.background
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(inset + height)
            .background(Brush.verticalGradient(0f to background.copy(alpha = 0f), 0.45f to background.copy(alpha = 0.92f), 1f to background)),
    )
}

@Composable
fun BoxScope.TopBar(
    onBack: (() -> Unit)?,
    backLabel: String,
    title: String? = null,
    titleVisible: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopEdgeBlur()
    Row(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) GlassIconButton(PhosphorIcons.Regular.ArrowLeft, backLabel, onClick = onBack)
        val alpha by animateFloatAsState(if (titleVisible && title != null) 1f else 0f, tween(200), label = "barTitle")
        Text(
            title.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp).graphicsLayer { this.alpha = alpha },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    onDone: () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .height(54.dp)
            .card()
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
        }
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing()
    }
}

@Composable
fun CoverDialog(
    title: String,
    body: String,
    confirm: String,
    dismiss: String,
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .lift(CardShape, 24.dp)
                .card()
                .padding(22.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            if (body.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(dismiss, Modifier.weight(1f), onClick = onDismiss)
                if (danger) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .pressable(scale = 0.97f, onClick = onConfirm)
                            .clip(ButtonShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(confirm, style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                } else {
                    PrimaryButton(confirm, Modifier.weight(1f), onClick = onConfirm)
                }
            }
        }
    }
}

@Composable
fun FileTitle(text: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.headlineMedium
    val oneWord = text.none { it.isWhitespace() }
    androidx.compose.foundation.text.BasicText(
        text,
        modifier = modifier,
        style = style.copy(color = MaterialTheme.colorScheme.onBackground),
        maxLines = if (oneWord) 1 else 2,
        overflow = TextOverflow.Ellipsis,
        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = style.fontSize, stepSize = 1.sp),
    )
}
