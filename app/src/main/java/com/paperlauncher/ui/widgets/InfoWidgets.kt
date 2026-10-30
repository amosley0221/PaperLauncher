package com.paperlauncher.ui.widgets

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paperlauncher.LauncherVM
import com.paperlauncher.data.Apps
import com.paperlauncher.data.BatteryInfo
import com.paperlauncher.data.SignalInfo
import com.paperlauncher.data.StorageInfo
import com.paperlauncher.data.WeatherData
import com.paperlauncher.data.glyphFor
import com.paperlauncher.model.Size
import com.paperlauncher.model.Widget
import com.paperlauncher.ui.Bar
import com.paperlauncher.ui.BatteryShape
import com.paperlauncher.ui.Glyph
import com.paperlauncher.ui.Ink
import com.paperlauncher.ui.Label
import com.paperlauncher.ui.LocalAccent
import com.paperlauncher.ui.Muted
import com.paperlauncher.ui.SignalBlock
import com.paperlauncher.ui.T
import com.paperlauncher.ui.Track
import com.paperlauncher.ui.VRule
import com.paperlauncher.ui.tap
import kotlin.math.roundToInt

// ---------- loading / error states (never spinners) ----------

@Composable
fun EmptyState(title: String, message: String?) {
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
        Label(title, 10f, color = Muted, tracking = .2f)
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(2.dp).border(1.dp, Track))
        if (message != null) Label(message, 10f, Modifier.padding(top = 6.dp), color = Muted, tracking = .15f)
    }
}

// ---------- weather ----------

@Composable
fun WeatherWidget(vm: LauncherVM, size: Size) {
    val r = vm.weather
    val d = r.data
    if (d == null) { EmptyState("WEATHER", r.error); return }
    val accent = LocalAccent.current
    when (size) {
        Size.S -> Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Glyph(d.glyph, 40f, color = accent)
            Column {
                Temp(d.temp, d.unit, 36f, 14f)
                Label(d.condition, 10f, Modifier.padding(top = 3.dp), tracking = .12f, maxLines = 1)
            }
        }
        Size.M -> Row(Modifier.fillMaxSize().padding(start = 16.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Glyph(d.glyph, 44f, color = accent)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Temp(d.temp, d.unit, 38f, 15f)
                Label(d.condition, 10f, tracking = .1f, maxLines = 1)
                Label(d.place, 10f, color = accent, tracking = .1f, maxLines = 1)
            }
            VRule(60.dp, Modifier.padding(horizontal = 4.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceAround) {
                d.days.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Label(day.name, 11f, tracking = .15f)
                        Glyph(glyphFor(day.code), 22f, color = accent)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            T("${day.hi}°", 15f, weight = FontWeight.SemiBold, lh = 1f)
                            T("${day.lo}°", 11f, color = Muted, lh = 1f)
                        }
                    }
                }
            }
        }
        Size.L -> WeatherLarge(vm, d)
    }
}

@Composable
private fun Temp(t: Int, unit: String, big: Float, small: Float) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        T(t.toString(), big, lh = .9f)
        T(unit, small, weight = FontWeight.SemiBold, lh = 1f, modifier = Modifier.padding(bottom = (big * .08f).dp))
    }
}

@Composable
private fun WeatherLarge(vm: LauncherVM, d: WeatherData) {
    val accent = LocalAccent.current
    Column(Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp)) {
                if (d.code in 1..3 && d.isDay) {
                    Glyph("sunny", 40f, color = accent)
                    Glyph("cloud", 38f, Modifier.padding(start = 12.dp, top = 16.dp))
                } else Glyph(d.glyph, 46f, color = accent)
            }
            Column {
                Temp(d.temp, d.unit, 48f, 18f)
                Label(d.condition, 11f, Modifier.padding(top = 4.dp), tracking = .1f, maxLines = 1)
                Label(d.place, 11f, color = accent, tracking = .1f, maxLines = 1)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Glyph("refresh", 14f, Modifier.tap { vm.refreshWeather() }, color = Muted)
                Label("H ${d.hi}°  L ${d.lo}°", 16f, Modifier.padding(top = 6.dp), tracking = .08f, maxLines = 1)
                Label("FEELS LIKE ${d.feels}°", 10f, Modifier.padding(top = 4.dp), color = Muted, tracking = .12f, maxLines = 1)
            }
        }
        Box(Modifier.padding(top = 8.dp, bottom = 6.dp).fillMaxWidth().height(2.dp).border(1.dp, Ink))
        Row(Modifier.fillMaxWidth().height(36.dp)) {
            Stat("HUMIDITY", "${d.humidity}%", Modifier.weight(1f))
            VRule(36.dp)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Label("WIND", 9f, color = Muted, tracking = .2f)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Glyph("navigation", 14f, color = accent, rotation = (d.windDir + 180f) % 360f)
                    Label(" ${d.wind} ${d.windUnit}", 16f, tracking = 0f, maxLines = 1)
                }
            }
            VRule(36.dp)
            Stat("UV", d.uv.toString(), Modifier.weight(1f))
            VRule(36.dp)
            Stat("SUN", "${d.sunrise} – ${d.sunset}", Modifier.weight(1.4f))
        }
        Box(Modifier.padding(top = 6.dp, bottom = 4.dp).fillMaxWidth().height(2.dp).border(1.dp, Ink))
        Label("NEXT 24 HOURS", 9f, color = Muted, tracking = .2f)
        HourlyChart(d.hourly, Modifier.fillMaxWidth().weight(1f))
        Row(Modifier.fillMaxWidth().padding(end = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val labels = d.hours.filterIndexed { i, _ -> i % 3 == 0 }.take(8)
            labels.forEachIndexed { i, h -> Label(if (i == 0) "NOW" else h, 9f, color = Muted, tracking = .15f) }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Label(label, 9f, color = Muted, tracking = .2f)
        Label(value, 16f, Modifier.padding(top = 2.dp), tracking = 0f, maxLines = 1)
    }
}

@Composable
private fun HourlyChart(temps: List<Int>, modifier: Modifier) {
    if (temps.size < 2) return
    val accent = LocalAccent.current
    val density = LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = com.paperlauncher.ui.textStyle(10f, FontWeight.SemiBold)
    Canvas(modifier) {
        val top = 14.dp.toPx()
        val bottom = 6.dp.toPx()
        val left = 8.dp.toPx()
        val right = 8.dp.toPx()
        val lo = temps.min()
        val hi = temps.max()
        val span = (hi - lo).coerceAtLeast(1)
        val h = size.height - top - bottom
        val w = size.width - left - right
        fun pt(i: Int) = Offset(left + w * i / (temps.size - 1), top + h * (hi - temps[i]) / span)
        val path = Path().apply {
            moveTo(pt(0).x, pt(0).y)
            for (i in 1 until temps.size) lineTo(pt(i).x, pt(i).y)
        }
        drawPath(path, Ink, style = Stroke(width = 2.5f * density.density, join = StrokeJoin.Round))
        for (i in temps.indices step 3) {
            val p = pt(i)
            drawCircle(accent, radius = 4.5f * density.density, center = p)
            val layout = measurer.measure("${temps[i]}°", style)
            drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height - 3.dp.toPx()))
        }
    }
}

// ---------- battery & signal ----------

@Composable
fun BatteryWidget(size: Size, b: BatteryInfo, sig: SignalInfo) {
    val frac = b.percent / 100f
    when (size) {
        Size.S -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            BatteryShape(frac, 78.dp, 26.dp)
            T("${b.percent}%", 34f, lh = 1f)
        }
        Size.M -> Row(Modifier.fillMaxSize().padding(start = 20.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SignalBlock(sig.cellBars, sig.wifiBars)
            Label("${sig.network}\n${sig.carrier}", 9f, tracking = .12f, lh = 1.5f, maxLines = 2)
            Box(Modifier.weight(1f))
            BatteryShape(frac, 110.dp, 14.dp, pad = 2.dp)
            T("${b.percent}%", 22f, lh = 1f)
        }
        Size.L -> Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            T("${b.percent}%", 60f, lh = .85f)
            BatteryShape(frac, 200.dp, 16.dp, pad = 2.dp, modifier = Modifier.padding(end = 8.dp))
            val sub = when {
                b.charging && b.minutesToFull > 0 -> "CHARGING · ${b.minutesToFull / 60}H ${b.minutesToFull % 60}M TO FULL"
                b.charging -> "CHARGING"
                else -> "ON BATTERY"
            }
            Label(sub, 10f, color = Muted, tracking = .2f, maxLines = 1)
            Box(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SignalBlock(sig.cellBars, sig.wifiBars)
                Label("${sig.network}\n${sig.carrier}", 9f, tracking = .12f, lh = 1.5f, maxLines = 2)
            }
        }
    }
}

// ---------- storage & RAM ----------

@Composable
fun StorageWidget(size: Size, s: StorageInfo) {
    val accent = LocalAccent.current
    fun gb(v: Double) = if (v >= 100) "%.0f".format(v) else "%.1f".format(v)
    when (size) {
        Size.S -> Column(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
            SmallBar("STORAGE", s.storagePct)
            SmallBar("RAM", s.ramPct)
        }
        Size.M, Size.L -> {
            val big = size == Size.L
            Row(Modifier.fillMaxSize().padding(start = if (big) 10.dp else 8.dp, end = if (big) 16.dp else 14.dp, top = if (big) 16.dp else 10.dp, bottom = if (big) 16.dp else 10.dp)) {
                Column(
                    Modifier.width(if (big) 90.dp else 80.dp).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                ) {
                    Glyph("cleaning_services", if (big) 48f else 38f, color = accent, rotation = -20f)
                    if (big) Label("CLEAN", 9f, color = Muted, tracking = .2f)
                }
                DashedRule(Modifier.fillMaxHeight())
                Column(
                    Modifier.weight(1f).fillMaxHeight().padding(start = if (big) 16.dp else 12.dp),
                    verticalArrangement = if (big) Arrangement.SpaceAround else Arrangement.SpaceBetween,
                ) {
                    BigBar("STORAGE", gb(s.usedGb), "${gb(s.totalGb)} GB", s.storagePct, big)
                    BigBar("RAM", gb(s.ramUsedGb), "${"%.0f".format(s.ramTotalGb)} GB", s.ramPct, big)
                }
            }
        }
    }
}

@Composable
private fun SmallBar(label: String, pct: Float) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label(label, 10f, tracking = .18f)
            Label("${(pct * 100).roundToInt()}%", 10f, tracking = .04f)
        }
        Bar(pct, 8.dp, Modifier.fillMaxWidth().padding(top = 3.dp))
    }
}

@Composable
private fun BigBar(label: String, used: String, total: String, pct: Float, big: Boolean) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Label(label, if (big) 14f else 12f, tracking = .2f)
            if (big) Row(verticalAlignment = Alignment.Bottom) {
                T("$used ", 22f, lh = 1f)
                T("/ $total", 12f, color = Muted, lh = 1.2f)
            } else T("$used / $total", 11f, lh = 1.2f, maxLines = 1)
        }
        Bar(pct, if (big) 14.dp else 10.dp, Modifier.fillMaxWidth().padding(top = if (big) 4.dp else 2.dp), pad = if (big) 2.dp else 1.dp, track = Track)
    }
}

@Composable
private fun DashedRule(modifier: Modifier) {
    Canvas(modifier.width(2.dp)) {
        val dash = 6.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(Ink, Offset(size.width / 2, y), Offset(size.width / 2, minOf(y + dash, size.height)), strokeWidth = size.width)
            y += dash * 2
        }
    }
}

// ---------- search ----------

@Composable
fun SearchWidget(size: Size, onLens: () -> Unit, onMic: () -> Unit) {
    val accent = LocalAccent.current
    Row(
        Modifier.fillMaxSize().padding(horizontal = if (size == Size.L) 22.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (size == Size.S) Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally) else Arrangement.spacedBy(14.dp),
    ) {
        Glyph("search", if (size == Size.L) 28f else 26f, color = accent)
        when (size) {
            Size.S -> Glyph("mic", 22f, Modifier.tap(onClick = onMic))
            Size.M -> {
                Label("SEARCH", 14f, Modifier.weight(1f), color = Muted, tracking = .28f)
                Glyph("mic", 22f, Modifier.tap(onClick = onMic))
            }
            Size.L -> {
                Label("SEARCH APPS, CONTACTS, WEB", 15f, Modifier.weight(1f), color = Muted, tracking = .3f, maxLines = 1)
                Label("LENS", 10f, color = Muted, tracking = .2f)
                Glyph("photo_camera", 22f, Modifier.tap(onClick = onLens))
                Glyph("mic", 22f, Modifier.tap(onClick = onMic))
            }
        }
    }
}

// ---------- app shortcuts ----------

@Composable
fun AppsWidget(vm: LauncherVM, w: Widget, size: Size, editing: Boolean, onEmpty: () -> Unit) {
    val ctx = LocalContext.current
    val list = when (size) {
        Size.S -> w.apps.take(1)
        Size.M -> w.apps.take(2)
        Size.L -> w.apps.take(6)
    }
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize().tap(!editing, onEmpty), contentAlignment = Alignment.Center) {
            Glyph("add", 26f, color = Muted)
        }
        return
    }
    val iconSize = when (vm.settings.iconSize) { 0 -> 22f; 2 -> 32f; else -> 26f }
    val names = vm.settings.showAppNames
    @Composable
    fun Cell(key: String, modifier: Modifier) {
        Column(modifier.tap(!editing) { Apps.open(ctx, key) }, horizontalAlignment = Alignment.CenterHorizontally) {
            AppGlyph(key, iconSize)
            if (names) Label(Apps.labelFor(ctx, key), 8f, Modifier.padding(top = 3.dp), tracking = .12f, maxLines = 1, align = TextAlign.Center)
        }
    }
    if (size != Size.L || list.size <= 2) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = if (size == Size.S) 0.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) { list.forEach { Cell(it, Modifier) } }
    } else {
        val cols = if (list.size <= 4) 2 else 3
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp)) {
            list.chunked(cols).forEach { row ->
                Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    row.forEach { Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Cell(it, Modifier) } }
                    repeat(cols - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Mono glyph if we know one, otherwise the app's own icon tinted to ink. */
@Composable
fun AppGlyph(key: String, size: Float) {
    val glyph = Apps.glyphFor(key)
    if (glyph != null) { Glyph(glyph, size); return }
    val ctx = LocalContext.current
    val px = with(LocalDensity.current) { (size * 1.6f).dp.roundToPx() }
    val bmp by produceState<Bitmap?>(null, key) {
        value = Apps.find(ctx, key)?.let { Apps.icon(ctx, it, px) }
    }
    val b = bmp
    if (b != null) {
        Image(b.asImageBitmap(), null, Modifier.size((size * 1.1f).dp), colorFilter = ColorFilter.tint(Ink))
    } else Glyph("apps", size)
}
