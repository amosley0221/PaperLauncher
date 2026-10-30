package com.paperlauncher.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paperlauncher.LauncherVM
import com.paperlauncher.data.Agents
import com.paperlauncher.data.CarData
import com.paperlauncher.data.Game
import com.paperlauncher.data.Media
import com.paperlauncher.data.MediaState
import com.paperlauncher.model.Size
import com.paperlauncher.model.Widget
import com.paperlauncher.ui.Bar
import com.paperlauncher.ui.CircleGlyph
import com.paperlauncher.ui.Glyph
import com.paperlauncher.ui.Ink
import com.paperlauncher.ui.Label
import com.paperlauncher.ui.LocalAccent
import com.paperlauncher.ui.LocalActions
import com.paperlauncher.ui.Muted
import com.paperlauncher.ui.Paper
import com.paperlauncher.ui.PhotoBox
import com.paperlauncher.ui.SquareGlyph
import com.paperlauncher.ui.T
import com.paperlauncher.ui.Tick
import com.paperlauncher.ui.Track
import com.paperlauncher.ui.VRule
import com.paperlauncher.ui.tap
import com.paperlauncher.ui.textStyle
import kotlinx.coroutines.delay

// ---------- music ----------

private fun mmss(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

@Composable
fun MusicWidget(vm: LauncherVM, size: Size) {
    val granted by Media.granted.collectAsState()
    val st by Media.state.collectAsState()
    val ctx = LocalContext.current
    if (!granted) { EmptyState("MUSIC", "TAP TO ALLOW MEDIA ACCESS"); return }
    val m = st
    if (m == null || m.title.isEmpty()) { EmptyState("MUSIC", "NOTHING PLAYING"); return }
    var pos by remember { mutableLongStateOf(m.positionNow()) }
    LaunchedEffect(m) {
        while (true) { pos = m.positionNow(); delay(1000) }
    }
    val frac = if (m.durationMs > 0) pos.toFloat() / m.durationMs else 0f
    val accent = LocalAccent.current
    when (size) {
        Size.S -> Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                T(m.title, 20f, lh = 1f, maxLines = 1, ellipsis = true)
                T(m.artist, 11f, Modifier.padding(top = 4.dp), maxLines = 1)
                Progress(frac, Modifier.padding(top = 8.dp))
            }
            CircleGlyph(if (m.playing) "pause" else "play_arrow", 34.dp, 18f) { Media.toggle() }
        }
        Size.M -> Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhotoBox(vm, null, Modifier.size(66.dp).tap { Media.openPlayer(ctx) }, placeholder = "ALBUM", fallback = m.art)
            Column(Modifier.weight(1f)) {
                T(m.title, 22f, lh = 1f, maxLines = 1, ellipsis = true)
                T(m.artist, 11f, Modifier.padding(top = 3.dp), maxLines = 1)
                Progress(frac, Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    T(mmss(pos), 9f); T(mmss(m.durationMs), 9f)
                }
            }
            Controls(m, 34, 18f, 8)
        }
        Size.L -> Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PhotoBox(vm, null, Modifier.width(160.dp).fillMaxHeight().tap { Media.openPlayer(ctx) }, placeholder = "ALBUM ART", fallback = m.art)
            Column(Modifier.weight(1f).fillMaxHeight().padding(top = 4.dp)) {
                Label(m.album.ifEmpty { "NOW PLAYING" }, 10f, color = Muted, tracking = .18f, lh = 1.4f, maxLines = 2)
                Box(Modifier.weight(1f))
                T(m.title, 30f, lh = 1f, maxLines = 1, ellipsis = true)
                T(listOf(m.artist, m.album).filter { it.isNotEmpty() }.joinToString(" · "), 12f, Modifier.padding(top = 4.dp), maxLines = 1)
                Progress(frac, Modifier.padding(top = 12.dp))
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    T("${mmss(pos)} / ${mmss(m.durationMs)}", 10f, Modifier.weight(1f))
                    Controls(m, 38, 20f, 10)
                }
            }
        }
    }
}

@Composable
private fun Progress(frac: Float, modifier: Modifier) {
    val accent = LocalAccent.current
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxWidth().height(10.dp)) {
        Box(Modifier.align(Alignment.CenterStart).fillMaxWidth().height(2.dp).background(Ink))
        Box(Modifier.align(Alignment.CenterStart).offset(x = (maxWidth - 10.dp) * frac.coerceIn(0f, 1f)).size(10.dp).clip(CircleShape).background(accent))
    }
}

@Composable
private fun Controls(m: MediaState, circle: Int, glyph: Float, gap: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
        Glyph("skip_previous", glyph, Modifier.tap { Media.prev() })
        CircleGlyph(if (m.playing) "pause" else "play_arrow", circle.dp, glyph) { Media.toggle() }
        Glyph("skip_next", glyph, Modifier.tap { Media.next() })
    }
}

// ---------- car · tessie ----------

private fun agoText(at: Long): String {
    val mins = ((System.currentTimeMillis() - at) / 60000).coerceAtLeast(0)
    return if (mins < 1) "JUST NOW" else if (mins < 60) "$mins MIN AGO" else "${mins / 60} H AGO"
}

private fun chargeLine(c: CarData): String = when {
    c.asleep -> "SLEEPING"
    c.charging -> "CHARGING · " + (if (c.minutesToFull > 0) "${c.minutesToFull / 60} H ${c.minutesToFull % 60} M" else "${c.chargerKw} KW")
    c.chargingState.equals("Complete", true) -> "CHARGE COMPLETE"
    else -> if (c.locked) "LOCKED" else "UNLOCKED"
}

@Composable
fun CarWidget(vm: LauncherVM, size: Size) {
    val r = vm.car
    val c = r.data
    if (c == null) { EmptyState("CAR · TESSIE", r.error ?: "LOADING"); return }
    val accent = LocalAccent.current
    val actions = LocalActions.current
    val frac = c.battery / 100f
    val lineColor = if (c.asleep) Muted else accent
    fun lock() {
        if (c.locked) actions.confirm("UNLOCK ${c.model}?") { vm.carCommand("unlock") } else vm.carCommand("lock")
    }
    fun climate() {
        vm.carCommand(if (c.climateOn) "stop_climate" else "start_climate")
    }
    when (size) {
        Size.S -> Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Glyph("electric_car", 34f, color = accent)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T("${c.battery}%", 32f, Modifier.weight(1f), lh = .9f)
                    Glyph(if (c.locked) "lock" else "lock_open", 16f)
                }
                Bar(frac, 8.dp, Modifier.fillMaxWidth().padding(top = 6.dp))
                Label("${c.rangeMi} MI · ${chargeLine(c)}", 9f, Modifier.padding(top = 3.dp), color = Muted, tracking = .12f, maxLines = 1)
            }
        }
        Size.M -> Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CarImage(vm, Modifier.size(72.dp, 56.dp))
            Column(Modifier.width(104.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    T("${c.battery}%", 36f, lh = .9f)
                    Label("${c.rangeMi} MI", 12f, color = Muted, tracking = .1f, maxLines = 1)
                }
                Bar(frac, 10.dp, Modifier.fillMaxWidth(), track = Track)
                Label(chargeLine(c), 10f, color = lineColor, tracking = .12f, maxLines = 1)
            }
            VRule(60.dp)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceAround) {
                CarButton(if (c.locked) "lock" else "lock_open", if (c.locked) "LOCK" else "OPEN", false) { lock() }
                CarButton("mode_fan", "${c.setTemp}°", c.climateOn) { climate() }
            }
        }
        Size.L -> Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CarImage(vm, Modifier.size(124.dp, 78.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        T("${c.battery}%", 44f, lh = .85f)
                        Label("${c.rangeMi} MI", 16f, color = Muted, tracking = .1f, maxLines = 1)
                    }
                    Label("${c.model} · ${c.location}", 11f, Modifier.padding(top = 4.dp), tracking = .12f, maxLines = 1)
                    Label(
                        if (c.charging) "CHARGING · ${c.chargerKw} KW · TO ${c.chargeLimit}%" else chargeLine(c),
                        11f, color = lineColor, tracking = .12f, maxLines = 1,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Glyph("refresh", 14f, Modifier.tap { vm.refreshCar() }, color = Muted)
                    Label(agoText(c.updatedAt), 9f, Modifier.padding(top = 6.dp), color = Muted, tracking = .12f)
                    Label("TESSIE", 9f, Modifier.padding(top = 2.dp), color = Muted, tracking = .12f)
                }
            }
            Bar(frac, 14.dp, Modifier.fillMaxWidth().padding(top = 10.dp), pad = 2.dp, track = Track, marker = c.chargeLimit / 100f)
            Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(2.dp).background(Ink))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(36.dp)) {
                CarStat("INSIDE", c.insideTemp?.let { "$it°" } ?: "—", Modifier.weight(1f))
                VRule(36.dp)
                CarStat("OUTSIDE", c.outsideTemp?.let { "$it°" } ?: "—", Modifier.weight(1f))
                VRule(36.dp)
                CarStat("TIRES", c.tirePsi?.let { "$it PSI" } ?: "—", Modifier.weight(1f))
                VRule(36.dp)
                CarStat("ODOMETER", "%,d".format(c.odometer), Modifier.weight(1f))
            }
            Box(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WideButton(if (c.locked) "lock" else "lock_open", if (c.locked) "LOCK" else "UNLOCKED", false, Modifier.weight(1f)) { lock() }
                WideButton("mode_fan", "${c.setTemp}°", c.climateOn, Modifier.weight(1f)) { climate() }
                WideButton("bolt", "PORT", false, Modifier.weight(1f)) { vm.carCommand("open_charge_port") }
                WideButton("volume_up", "HONK", false, Modifier.weight(1f)) { actions.confirm("HONK THE HORN?") { vm.carCommand("honk") } }
            }
        }
    }
}

@Composable
private fun CarImage(vm: LauncherVM, modifier: Modifier) {
    val img = vm.carImage
    if (img == null) {
        Box(modifier, contentAlignment = Alignment.Center) { Glyph("electric_car", 40f, color = Muted) }
    } else {
        PhotoBox(vm, null, modifier, fallback = img, fit = ContentScale.Fit, contrast = 1.1f)
    }
}

@Composable
private fun CarStat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Label(label, 9f, color = Muted, tracking = .2f)
        Label(value, 16f, Modifier.padding(top = 2.dp), tracking = 0f, maxLines = 1)
    }
}

@Composable
private fun CarButton(glyph: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SquareGlyph(glyph, 38.dp, 20f, active, onClick)
        Label(label, 9f, tracking = .15f)
    }
}

@Composable
private fun WideButton(glyph: String, label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val fg = if (active) Paper else Ink
    Row(
        modifier.height(40.dp).border(2.dp, Ink).background(if (active) Ink else Color.Transparent).tap(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Glyph(glyph, 18f, color = fg)
        Label(label, 10f, color = fg, tracking = .14f)
    }
}

// ---------- sports ----------

@Composable
fun SportsWidget(vm: LauncherVM, size: Size) {
    val r = vm.sports
    val d = r.data
    val g = d?.game
    if (d == null || (g == null && d.next.isEmpty())) { EmptyState("SPORTS", r.error ?: if (d != null) "NO GAMES SCHEDULED" else "LOADING"); return }
    val accent = LocalAccent.current
    val status = g?.let { statusText(it) } ?: ""
    when (size) {
        Size.S -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            if (g == null) {
                Label(d.next.first().text, 12f, tracking = .12f)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Label(g.mine.abbr, 11f, tracking = .15f); T(g.mine.score.ifEmpty { "–" }, 30f, lh = 1f) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Label(status, 9f, color = accent, tracking = .15f, maxLines = 1)
                    Tick()
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Label(g.other.abbr, 11f, tracking = .15f); T(g.other.score.ifEmpty { "–" }, 30f, lh = 1f) }
            }
        }
        Size.M -> Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (g != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Label(if (g.state == "in") "LIVE · $status" else status, 9f, color = accent, tracking = .2f, maxLines = 1)
                    ScoreLine(g.mine.name, g.mine.score, Ink)
                    ScoreLine(g.other.name, g.other.score, Muted)
                }
                VRule(60.dp)
            }
            NextList(d.next, Modifier.weight(1f))
        }
        Size.L -> Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Label(if (g?.state == "in") "LIVE" else if (g?.state == "post") "FINAL" else "NEXT", 10f, color = accent, tracking = .2f)
                Tick()
                Label(listOfNotNull(g?.league, status.takeIf { it.isNotEmpty() }).joinToString(" · "), 10f, Modifier.weight(1f), tracking = .2f, maxLines = 1)
                Glyph("refresh", 14f, Modifier.tap { vm.refreshSports() }, color = Muted)
            }
            if (g != null) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    BigSide(g.mine.name, g.mine.score, g.mine.record, Ink)
                    Label("${g.venue}\n${g.tv}".trim(), 10f, Modifier.weight(1f).padding(horizontal = 8.dp), color = Muted, tracking = .2f, lh = 1.8f, align = TextAlign.Center, maxLines = 2)
                    BigSide(g.other.name, g.other.score, g.other.record, Muted)
                }
                BoxScore(g, Modifier.padding(top = 10.dp))
            }
            Box(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                d.next.getOrNull(0)?.let { f ->
                    Row { Label("NEXT  ", 11f, color = Muted, tracking = .12f); Label("${f.text} · ${f.time}", 11f, tracking = .12f, maxLines = 1) }
                }
                d.next.getOrNull(1)?.let { f -> Label("${f.text} · ${f.time}", 11f, color = Muted, tracking = .12f, maxLines = 1) }
            }
        }
    }
}

private fun statusText(g: Game) = when (g.state) {
    "post" -> "FINAL"
    else -> g.detail
}

@Composable
private fun ScoreLine(name: String, score: String, color: Color) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Label(name, 14f, color = color, tracking = .12f, maxLines = 1)
        T(score.ifEmpty { "–" }, 34f, color = color, lh = .9f)
    }
}

@Composable
private fun NextList(next: List<com.paperlauncher.data.Fixture>, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Label("NEXT", 9f, color = Muted, tracking = .2f)
        next.forEach { f ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Label(f.text, 12f, tracking = .1f, maxLines = 1)
                Label(f.time, 12f, color = Muted, tracking = .1f)
            }
        }
    }
}

@Composable
private fun BigSide(name: String, score: String, record: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Label(name, 12f, color = color, tracking = .15f, maxLines = 1)
        T(score.ifEmpty { "–" }, 62f, color = color, lh = .85f)
        Label(record, 9f, Modifier.padding(top = 4.dp), color = Muted, tracking = .15f)
    }
}

@Composable
private fun BoxScore(g: Game, modifier: Modifier) {
    val n = maxOf(g.mine.lines.size, g.other.lines.size, 4)
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Ink))
        Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Label("Q", 9f, Modifier.weight(1f), color = Muted, tracking = .2f)
                for (i in 1..n) Label(if (i <= 4) "$i" else "OT", 12f, Modifier.weight(1f), align = TextAlign.Center, tracking = 0f)
            }
            Row(Modifier.fillMaxWidth()) {
                Label(g.mine.abbr, 10f, Modifier.weight(1f), tracking = .15f)
                for (i in 0 until n) Label(g.mine.lines.getOrElse(i) { "" }, 12f, Modifier.weight(1f), align = TextAlign.Center, tracking = 0f)
            }
            Row(Modifier.fillMaxWidth()) {
                Label(g.other.abbr, 10f, Modifier.weight(1f), color = Muted, tracking = .15f)
                for (i in 0 until n) Label(g.other.lines.getOrElse(i) { "" }, 12f, Modifier.weight(1f), color = Muted, align = TextAlign.Center, tracking = 0f)
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Ink))
    }
}

// ---------- ask an agent ----------

/** Uppercase display without changing the underlying text length. */
private val Caps = VisualTransformation { t ->
    TransformedText(AnnotatedString(String(CharArray(t.text.length) { t.text[it].uppercaseChar() })), OffsetMapping.Identity)
}

@Composable
fun AskWidget(vm: LauncherVM, w: Widget, size: Size, editing: Boolean, onAgent: (String) -> Unit) {
    val ctx = LocalContext.current
    val actions = LocalActions.current
    val focus = LocalFocusManager.current
    val accent = LocalAccent.current
    val agent = Agents.of(w.agent)
    var text by rememberSaveable(w.id) { mutableStateOf("") }
    fun send() {
        vm.rememberAsk(text)
        Agents.send(ctx, agent.key, text)
        text = ""
        focus.clearFocus()
    }
    @Composable
    fun Field(fontSize: Float, modifier: Modifier) {
        val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
        var focused by remember { mutableStateOf(false) }
        fun report() {
            val c = coords[0]
            if (focused && c != null && c.isAttached) vm.imeAnchor = c.boundsInRoot().bottom + vm.imeShift
        }
        Box(
            modifier
                .onGloballyPositioned { coords[0] = it; if (focused && vm.imeShift == 0f) report() }
                .onFocusChanged { f ->
                    focused = f.hasFocus
                    if (focused) report() else vm.imeAnchor = null
                }
        ) {
            if (text.isEmpty()) T("ASK ANYTHING…", fontSize, color = Muted, maxLines = 1)
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                enabled = !editing,
                singleLine = true,
                textStyle = textStyle(fontSize, FontWeight.Medium, Ink, lh = 1.2f),
                cursorBrush = SolidColor(accent),
                visualTransformation = Caps,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    when (size) {
        Size.S -> Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp).tap(!editing) { Agents.send(ctx, agent.key, "") },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Glyph(agent.glyph, 24f, color = accent)
            Label("ASK ${agent.name}", 12f, Modifier.weight(1f), color = Muted, tracking = .22f, maxLines = 1)
            Glyph("arrow_forward", 20f)
        }
        Size.M -> Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Glyph(agent.glyph, 28f, color = accent)
            Column(Modifier.weight(1f)) {
                Label(agent.name, 9f, color = accent, tracking = .22f)
                Field(22f, Modifier.fillMaxWidth())
            }
            Glyph("mic", 22f, Modifier.tap(!editing) { actions.listen { text = it } })
            SendButton(34) { send() }
        }
        Size.L -> Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Label("ASK", 10f, color = accent, tracking = .22f)
                Tick()
                Label("SENDS TO THE SELECTED AGENT", 10f, color = Muted, tracking = .22f, maxLines = 1)
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Agents.all.forEach { a ->
                    val sel = a.key == agent.key
                    val fg = if (sel) Paper else Ink
                    Row(
                        Modifier.weight(1f).height(36.dp).border(2.dp, Ink).background(if (sel) Ink else Color.Transparent)
                            .tap(!editing) { onAgent(a.key) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    ) {
                        Glyph(a.glyph, 18f, color = fg)
                        Label(a.name, 11f, color = fg, tracking = .18f)
                    }
                }
            }
            Box(Modifier.weight(1f))
            Field(30f, Modifier.fillMaxWidth().padding(bottom = 6.dp))
            Box(Modifier.fillMaxWidth().height(2.dp).background(Ink))
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Label(vm.recentAsk.takeIf { it.isNotEmpty() }?.let { "RECENT · ${it.uppercase()}" } ?: "", 10f, Modifier.weight(1f).tap(!editing) { text = vm.recentAsk }, color = Muted, tracking = .18f, maxLines = 1)
                Glyph("mic", 22f, Modifier.tap(!editing) { actions.listen { text = it } })
                SendButton(38) { send() }
            }
        }
    }
}

@Composable
private fun SendButton(size: Int, onClick: () -> Unit) {
    Box(Modifier.size(size.dp).background(Ink).tap(onClick = onClick), contentAlignment = Alignment.Center) {
        Glyph("arrow_forward", 20f, color = Paper)
    }
}
