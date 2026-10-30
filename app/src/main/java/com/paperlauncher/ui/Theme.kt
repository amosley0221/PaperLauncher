package com.paperlauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.paperlauncher.R

val Paper = Color(0xFFEDE7D9)
val Ink = Color(0xFF1D1C1A)
val Muted = Color(0xFF5A5650)
val Track = Color(0xFFCFC9BB)
val BatteryGreen = Color(0xFF8CC63F)
val Scrim = Color(0xC746433C)

val DefaultAccent = Color(0xFF7A1F2E)
val AccentPresets = listOf(
    "Burgundy" to 0xFF7A1F2E, "Teal" to 0xFF1E8A82, "Gold" to 0xFFD9A21B,
    "Red" to 0xFFD3312A, "Cobalt" to 0xFF1E4FB8, "Olive" to 0xFF6B8E23,
)

/** Swatches shown in the accent dialog (6-column grid). */
val AccentSwatches = listOf(
    0xFF7A1F2E, 0xFFD3312A, 0xFFE0672A, 0xFFD9A21B, 0xFF6B8E23, 0xFF2E8B57,
    0xFF1E8A82, 0xFF1E4FB8, 0xFF3E3A9E, 0xFF7B2D8E, 0xFFD6478A, 0xFF7A4A1F,
    0xFF46433C,
)

val Oswald = FontFamily(
    Font(R.font.oswald_regular, FontWeight.Normal),
    Font(R.font.oswald_medium, FontWeight.Medium),
    Font(R.font.oswald_semibold, FontWeight.SemiBold),
    Font(R.font.oswald_bold, FontWeight.Bold),
)
val Symbols = FontFamily(Font(R.font.material_symbols_rounded))

val LocalAccent = staticCompositionLocalOf { DefaultAccent }

private val Trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

@Suppress("DEPRECATION")
private val NoPad = PlatformTextStyle(includeFontPadding = false)

/** Oswald text. [lh] is a multiple of the font size (CSS line-height). [tracking] is in em. */
@Composable
fun T(
    text: String,
    size: Float,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Medium,
    color: Color = Ink,
    tracking: Float = 0f,
    lh: Float = 1.15f,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    ellipsis: Boolean = false,
) {
    if ('\n' !in text && (maxLines == 1 || lh <= 1.2f)) {
        // CSS line box: exactly size × lh tall with the glyphs centred, even when lh is tighter
        // than the font's own height (Compose won't shrink a single line below it).
        val box = with(LocalDensity.current) { (size * lh).sp.toDp() }
        val alignment = when (align) {
            TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        }
        Box(modifier.height(box), contentAlignment = alignment) {
            androidx.compose.foundation.text.BasicText(
                text = text,
                modifier = Modifier.wrapContentHeight(unbounded = true),
                style = textStyle(size, weight, color, tracking, lh, align).copy(lineHeight = TextUnit.Unspecified, lineHeightStyle = null),
                maxLines = 1,
                softWrap = false,
                overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
            )
        }
        return
    }
    androidx.compose.foundation.text.BasicText(
        text = text,
        modifier = modifier,
        style = textStyle(size, weight, color, tracking, lh, align),
        maxLines = maxLines,
        softWrap = maxLines != 1,
        overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
    )
}

fun textStyle(
    size: Float,
    weight: FontWeight = FontWeight.Medium,
    color: Color = Ink,
    tracking: Float = 0f,
    lh: Float = 1.15f,
    align: TextAlign? = null,
): TextStyle = TextStyle(
    fontFamily = Oswald,
    fontWeight = weight,
    fontSize = size.sp,
    color = color,
    letterSpacing = tracking.em,
    lineHeight = (size * lh).sp,
    lineHeightStyle = Trim,
    platformStyle = NoPad,
    textAlign = align ?: TextAlign.Unspecified,
)

/** Small caps label: 600 weight, wide tracking. */
@Composable
fun Label(
    text: String,
    size: Float = 10f,
    modifier: Modifier = Modifier,
    color: Color = Ink,
    tracking: Float = 0.15f,
    align: TextAlign? = null,
    lh: Float = 1.3f,
    maxLines: Int = Int.MAX_VALUE,
) = T(text, size, modifier, FontWeight.SemiBold, color, tracking, lh, align, maxLines, ellipsis = maxLines == 1)

/** Material Symbols Rounded glyph by ligature name. */
@Composable
fun Glyph(name: String, size: Float, modifier: Modifier = Modifier, color: Color = Ink, rotation: Float = 0f) {
    androidx.compose.foundation.text.BasicText(
        text = name,
        modifier = if (rotation != 0f) modifier.rotate(rotation) else modifier,
        style = TextStyle(
            fontFamily = Symbols,
            fontSize = size.sp,
            lineHeight = size.sp,
            color = color,
            lineHeightStyle = Trim,
            platformStyle = NoPad,
        ),
        maxLines = 1,
        softWrap = false,
    )
}

/** Text that reads bottom-to-top (CSS writing-mode: vertical-rl + rotate 180°). */
fun Modifier.verticalUp(): Modifier = this.layout { m, _ ->
    val p = m.measure(Constraints())
    layout(p.height, p.width) {
        p.placeWithLayer(
            x = (p.height - p.width) / 2,
            y = (p.width - p.height) / 2,
        ) { rotationZ = -90f }
    }
}

@Composable
fun Centered(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Box(modifier, contentAlignment = Alignment.Center) { content() }

val Rule: Dp = 2.dp
