package com.coverit.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.DotsThree
import com.coverit.app.ui.theme.LocalIsDark

const val FOLDER_RATIO = 1.28f

private class Sheet(val left: Float, val top: Float, val width: Float, val turn: Float, val lines: Int)

private val sheets = listOf(
    Sheet(0.12f, 0.0f, 0.5f, -0.075f, 8),
    Sheet(0.46f, 0.05f, 0.3f, 0.05f, 5),
    Sheet(0.65f, 0.14f, 0.24f, 0.09f, 4),
)

private const val LIFT = 0.08f
private const val RADIANS_TO_DEGREES = 57.29578f

fun folderSheetRect(folder: Rect, index: Int): Rect {
    val s = sheets.getOrNull(index)
        ?: return Rect(folder.center.x - folder.width * 0.15f, folder.center.y - folder.height * 0.25f, folder.center.x + folder.width * 0.15f, folder.center.y + folder.height * 0.25f)
    val left = folder.left + folder.width * s.left
    val top = folder.top + folder.height * (s.top - LIFT)
    return Rect(left, top, left + folder.width * s.width, top + folder.height * (0.86f - s.top))
}

fun folderSheetTurn(index: Int): Float =
    (sheets.getOrNull(index)?.turn ?: if (index % 2 == 0) 0.06f else -0.06f) * RADIANS_TO_DEGREES

enum class FolderLayer { WHOLE, BACK, FRONT }

private class FolderFront(private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Generic(path(size))

    fun path(size: Size, inset: Float = 0f): Path {
        val l = inset
        val t = inset
        val w = size.width - inset * 2
        val h = size.height - inset * 2
        val r = radius.coerceIn(0f, h * 0.3f)
        val tabTop = t + h * 0.2f
        val bodyTop = t + h * 0.38f
        val tabEnd = l + w * 0.51f
        val slopeEnd = l + w * 0.63f
        val middle = (tabEnd + slopeEnd) / 2
        return Path().apply {
            moveTo(l, tabTop + r)
            quadraticTo(l, tabTop, l + r, tabTop)
            lineTo(tabEnd, tabTop)
            cubicTo(middle, tabTop, middle, bodyTop, slopeEnd, bodyTop)
            lineTo(l + w - r, bodyTop)
            quadraticTo(l + w, bodyTop, l + w, bodyTop + r)
            lineTo(l + w, t + h - r)
            quadraticTo(l + w, t + h, l + w - r, t + h)
            lineTo(l + r, t + h)
            quadraticTo(l, t + h, l, t + h - r)
            close()
        }
    }
}

@Composable
fun FolderArt(
    icon: ImageVector?,
    papers: Int,
    modifier: Modifier = Modifier,
    pictures: List<Bitmap?> = emptyList(),
    lifted: Boolean = false,
    layer: FolderLayer = FolderLayer.WHOLE,
) {
    val dark = LocalIsDark.current
    val lift by animateFloatAsState(if (lifted) LIFT else 0f, tween(280, easing = EaseOutBack), label = "lift")
    val panel = if (dark) listOf(Color(0xFF26262A), Color(0xFF18181B)) else listOf(Color(0xFF303033), Color(0xFF1C1C1E))
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(FOLDER_RATIO)) {
        val w = maxWidth
        val h = maxHeight
        val radius = w * 0.058f
        val withBack = layer != FolderLayer.FRONT
        val withFront = layer != FolderLayer.BACK
        Box(Modifier.fillMaxSize()) {
            if (withBack) {
                Box(
                    Modifier
                        .padding(start = w * 0.1f, end = w * 0.1f, top = h * 0.55f, bottom = h * 0.05f)
                        .fillMaxSize()
                        .shadow(
                            22.dp,
                            RoundedCornerShape(radius),
                            ambientColor = Color.Black.copy(alpha = if (dark) 0.5f else 0.22f),
                            spotColor = Color.Black.copy(alpha = if (dark) 0.5f else 0.22f),
                        ),
                )
                Box(
                    Modifier
                        .padding(horizontal = w * 0.048f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(radius * 0.9f))
                        .background(Brush.verticalGradient(panel)),
                )
                sheets.take(papers.coerceIn(0, sheets.size)).withIndex().reversed().forEach { (i, s) ->
                    Paper(
                        picture = pictures.getOrNull(i),
                        modifier = Modifier
                            .offset(x = w * s.left, y = h * (s.top - lift))
                            .size(w * s.width, h * (0.86f - s.top))
                            .graphicsLayer { rotationZ = s.turn * RADIANS_TO_DEGREES },
                    )
                }
            }
        }
        if (withFront) {
            val radiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { radius.toPx() }
            val shape = remember(radiusPx) { FolderFront(radiusPx) }
            // A frosted front without a live blur: blurring every folder on every frame made
            // the library stutter, and the papers behind never move anyway.
            val frost = if (dark) listOf(Color(0xFF3A3A40), Color(0xFF232327)) else listOf(Color(0xFF4A4A50), Color(0xFF2A2A2E))
            val rim = Color.White.copy(alpha = if (dark) 0.5f else 0.8f)
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Brush.verticalGradient(frost.map { it.copy(alpha = 0.94f) })),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(
                        Brush.verticalGradient(
                            0.2f to Color.White.copy(alpha = 0.24f),
                            0.36f to Color.White.copy(alpha = 0f),
                            0.58f to Color.Black.copy(alpha = 0f),
                            0.96f to Color.Black.copy(alpha = 0.72f),
                        ),
                    )
                    drawRect(
                        Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            0.13f to Color.Black.copy(alpha = 0f),
                            0.87f to Color.Black.copy(alpha = 0f),
                            1f to Color.Black.copy(alpha = 0.6f),
                        ),
                    )
                    val stroke = 1.dp.toPx()
                    drawPath(
                        shape.path(size, stroke / 2),
                        Brush.verticalGradient(0.22f to rim, 0.62f to rim.copy(alpha = 0f)),
                        style = Stroke(stroke),
                    )
                }
                if (icon != null) {
                    Icon(
                        icon,
                        null,
                        Modifier
                            .align(androidx.compose.ui.BiasAlignment(0f, 0.58f))
                            .size(h * 0.16f),
                        tint = Color(0xFFF2F2F2),
                    )
                }
            }
        }
    }
}

@Composable
private fun Paper(picture: Bitmap?, modifier: Modifier) {
    val shown by animateFloatAsState(if (picture != null) 1f else 0f, tween(200), label = "paperPicture")
    if (picture == null && shown == 0f) return
    BoxWithConstraints(modifier.graphicsLayer { alpha = shown }) {
        val shape = RoundedCornerShape(maxWidth * 0.08f)
        Box(
            Modifier
                .fillMaxSize()
                .shadow(3.dp, shape, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.2f))
                .clip(shape)
                .background(Color.White),
        ) {
            if (picture != null) {
                Image(picture.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
            }
        }
    }
}

@Composable
fun FolderCell(
    title: String,
    subtitle: String,
    icon: ImageVector?,
    papers: Int,
    modifier: Modifier = Modifier,
    pictures: List<Bitmap?> = emptyList(),
    artModifier: Modifier = Modifier,
    hidden: Boolean = false,
    onMenu: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(140), label = "folderPress")
    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(interaction, null, onLongClick = onMenu, onClick = onClick),
    ) {
        FolderArt(icon, papers, artModifier.graphicsLayer { alpha = if (hidden) 0f else 1f }, pictures = pictures, lifted = pressed)
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = TextStyle(fontFamily = MaterialTheme.typography.titleMedium.fontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (onMenu != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(remember { MutableInteractionSource() }, null, onClick = onMenu)
                        .padding(start = 5.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                ) {
                    Icon(PhosphorIcons.Regular.DotsThree, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(
            subtitle,
            style = TextStyle(fontFamily = MaterialTheme.typography.bodySmall.fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
