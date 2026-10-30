package com.paperlauncher.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paperlauncher.LauncherVM

/** grayscale(1) contrast(c) */
fun grayscale(contrast: Float = 1.05f): ColorFilter {
    val t = (1f - contrast) * 128f
    val r = 0.2126f * contrast
    val g = 0.7152f * contrast
    val b = 0.0722f * contrast
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                r, g, b, 0f, t,
                r, g, b, 0f, t,
                r, g, b, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )
}

/** Clickable without ripple — the paper aesthetic has no ink splashes. */
fun Modifier.tap(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    if (!enabled) this else this.clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)

@Composable
fun Tick(width: Dp = 20.dp, modifier: Modifier = Modifier, color: Color = LocalAccent.current) =
    Box(modifier.size(width, 2.dp).background(color))

@Composable
fun VRule(height: Dp, modifier: Modifier = Modifier) = Box(modifier.size(2.dp, height).background(Ink))

@Composable
fun HRule(modifier: Modifier = Modifier, color: Color = Ink) = Box(modifier.fillMaxWidth().height(2.dp).background(color))

/** 2px ink-bordered bar with inner padding and a fill. */
@Composable
fun Bar(
    fraction: Float,
    height: Dp,
    modifier: Modifier = Modifier,
    pad: Dp = 1.dp,
    track: Color = Color.Transparent,
    fill: Color = LocalAccent.current,
    marker: Float? = null,
) {
    Box(modifier.height(height)) {
        Box(Modifier.fillMaxSize().border(2.dp, Ink).background(track).padding(2.dp + pad)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(fill))
        }
        if (marker != null) {
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width * marker
                drawRect(Ink, topLeft = Offset(x - 1.dp.toPx(), -4.dp.toPx()), size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height + 4.dp.toPx()))
            }
        }
    }
}

/** Battery outline with nub, green fill. */
@Composable
fun BatteryShape(fraction: Float, width: Dp, height: Dp, pad: Dp = 3.dp, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(width, height).border(2.dp, Ink, androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                .padding(2.dp + pad - 1.dp)
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(BatteryGreen))
        }
        Box(Modifier.padding(start = 1.dp).size(4.dp, height * 0.4f).background(Ink))
    }
}

/** Dashed signal bars: [filled] of [total] 8dp dashes with 2dp gaps; rest drawn in Track. */
@Composable
fun Dashes(filled: Int, total: Int, height: Dp = 8.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size((total * 10 - 2).dp, height)) {
        val dash = 8.dp.toPx()
        val step = 10.dp.toPx()
        for (i in 0 until total) {
            drawRect(
                if (i < filled) Ink else Track,
                topLeft = Offset(i * step, 0f),
                size = androidx.compose.ui.geometry.Size(dash, size.height),
            )
        }
    }
}

@Composable
fun SignalBlock(cell: Int, wifi: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Dashes(cell, 10)
        Dashes(wifi, 10)
    }
}

/** Outlined circle button (music play/pause). */
@Composable
fun CircleGlyph(glyph: String, size: Dp, glyphSize: Float, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Box(
        Modifier.size(size).border(2.dp, accent, CircleShape).tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Glyph(glyph, glyphSize, color = accent) }
}

/** Square ink button; [active] = ink fill. */
@Composable
fun SquareGlyph(glyph: String, size: Dp, glyphSize: Float, active: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.size(size).border(2.dp, Ink).background(if (active) Ink else Color.Transparent).tap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Glyph(glyph, glyphSize, color = if (active) Paper else Ink) }
}

/** User photo (grayscale) or a quiet placeholder. */
@Composable
fun PhotoBox(
    vm: LauncherVM,
    uri: String?,
    modifier: Modifier = Modifier,
    placeholder: String = "LANDSCAPE PHOTO",
    contrast: Float = 1.05f,
    fit: ContentScale = ContentScale.Crop,
    fallback: Bitmap? = null,
) {
    val maxPx = with(LocalDensity.current) { 900.dp.roundToPx() }
    var bmp by remember(uri) { mutableStateOf(vm.cachedPhoto(uri)) }
    LaunchedEffect(uri) { if (uri != null && bmp == null) bmp = vm.loadPhoto(uri, maxPx) }
    val image = bmp ?: fallback
    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(
                image.asImageBitmap(), null, Modifier.fillMaxSize(),
                contentScale = fit, colorFilter = grayscale(contrast),
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val step = 10.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(Track, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 1.dp.toPx())
                    x += step
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Glyph("landscape", 22f, color = Muted)
                Label(placeholder, 8f, color = Muted, tracking = 0.2f)
            }
        }
    }
}

@Composable
fun Spacer(w: Dp = 0.dp, h: Dp = 0.dp) = androidx.compose.foundation.layout.Spacer(Modifier.width(w).height(h))
