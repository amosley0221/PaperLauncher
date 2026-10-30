package com.paperlauncher.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paperlauncher.LauncherVM
import com.paperlauncher.data.CalEvent
import com.paperlauncher.model.Size
import com.paperlauncher.model.Widget
import com.paperlauncher.ui.Ink
import com.paperlauncher.ui.Label
import com.paperlauncher.ui.LocalAccent
import com.paperlauncher.ui.Muted
import com.paperlauncher.ui.Paper
import com.paperlauncher.ui.PhotoBox
import com.paperlauncher.ui.T
import com.paperlauncher.ui.Tick
import com.paperlauncher.ui.verticalUp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

// ---------- time strings ----------

class Now(val t: LocalDateTime, clock24: Boolean) {
    val hours: String = if (clock24) "%02d".format(t.hour) else "%02d".format(((t.hour + 11) % 12) + 1)
    val minutes: String = "%02d".format(t.minute)
    val mon: String = t.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase().take(3)
    val month: String = t.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
    val day: String = "%02d".format(t.dayOfMonth)
    val year: String = t.year.toString()
    val weekday: String = t.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
    val wd: String = t.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase().take(3)
    val week: String = "%02d".format(t.toLocalDate().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
}

fun LauncherVM.clock() = Now(now, settings.clock24)

fun lines(s: String) = s.trim().split(Regex("\\s+")).joinToString("\n")

/** Absolute placement helpers mirroring the CSS `left/top`, `right/top`, `right/bottom`. */
fun Modifier.at(left: Dp, top: Dp) = this.offset(left, top)

@Composable
fun BoxScope.TopRight(right: Dp, top: Dp, content: @Composable () -> Unit) =
    Box(Modifier.align(Alignment.TopEnd).padding(end = right, top = top)) { content() }

@Composable
fun BoxScope.BottomRight(right: Dp, bottom: Dp, content: @Composable () -> Unit) =
    Box(Modifier.align(Alignment.BottomEnd).padding(end = right, bottom = bottom)) { content() }

@Composable
fun Mantra(text: String, size: Float, tracking: Float = 0.22f, lh: Float, align: TextAlign = TextAlign.End) =
    Label(lines(text), size, tracking = tracking, lh = lh, align = align)

/** Caption with the last word in accent, one word per line, right-aligned. */
@Composable
fun Caption(text: String, size: Float, lh: Float) {
    val words = text.trim().split(Regex("\\s+"))
    Column(horizontalAlignment = Alignment.End) {
        words.forEachIndexed { i, w ->
            Label(w, size, color = if (i == words.lastIndex) LocalAccent.current else Ink, tracking = 0.22f, lh = lh, align = TextAlign.End)
        }
    }
}

// ---------- clock ----------

@Composable
fun ClockWidget(vm: LauncherVM, size: Size) {
    val n = vm.clock()
    val accent = LocalAccent.current
    val s = vm.settings
    Box(Modifier.fillMaxSize()) {
        when (size) {
            Size.S -> Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                T(n.hours, 54f, lh = .85f); T(n.minutes, 54f, color = accent, lh = .85f)
            }
            Size.M -> {
                T(n.hours, 84f, Modifier.at(12.dp, 10.dp), lh = .8f, tracking = -.02f)
                T(n.minutes, 84f, Modifier.at(12.dp, 84.dp), color = accent, lh = .8f, tracking = -.02f)
                BottomRight(12.dp, 12.dp) { Label("${n.wd}\n${n.mon} ${n.day}", 11f, tracking = .22f, align = TextAlign.End) }
            }
            Size.L -> {
                T(n.hours, 92f, Modifier.at(12.dp, 14.dp), lh = .8f, tracking = -.02f)
                T(n.minutes, 92f, Modifier.at(12.dp, 94.dp), color = accent, lh = .8f, tracking = -.02f)
                T("${n.mon}\n${n.day}\n${n.year}", 24f, Modifier.at(128.dp, 14.dp), lh = 1.25f)
                Box(Modifier.at(128.dp, 132.dp)) { Mantra(s.mantra3, 11f, lh = 1.18f, align = TextAlign.Start) }
                T(n.weekday, 52f, Modifier.at(200.dp, 22.dp).verticalUp(), lh = 1f)
                TopRight(12.dp, 14.dp) { Mantra(s.mantra1, 11f, lh = 1.18f) }
                TopRight(12.dp, 62.dp) { Tick() }
                TopRight(12.dp, 132.dp) { Mantra(s.mantra2, 11f, lh = 1.18f) }
            }
        }
    }
}

// ---------- date ----------

@Composable
fun DateWidget(vm: LauncherVM, size: Size) {
    val n = vm.clock()
    val accent = LocalAccent.current
    Box(Modifier.fillMaxSize()) {
        when (size) {
            Size.S -> Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                T("${n.mon} ${n.day}", 38f, lh = 1f)
                Label(n.wd, 12f, color = accent, tracking = .22f)
            }
            Size.M -> Box(Modifier.align(Alignment.Center)) { T(n.weekday, 44f, Modifier.verticalUp(), lh = 1f) }
            Size.L -> {
                T("${n.mon}\n${n.day}\n${n.year}", 40f, Modifier.at(14.dp, 12.dp), lh = 1.15f)
                BottomRight(12.dp, 12.dp) { Label(n.weekday, 12f, color = accent, tracking = .3f) }
            }
        }
    }
}

// ---------- hero (clock + photo) ----------

@Composable
fun HeroWidget(vm: LauncherVM, w: Widget, size: Size) {
    val n = vm.clock()
    val s = vm.settings
    val accent = LocalAccent.current
    val numeral = if (s.heroWeek) n.week else n.day
    Column(Modifier.fillMaxSize()) {
        if (size == Size.S) {
            Box(Modifier.fillMaxWidth().height(196.dp)) {
                T(n.hours, 86f, Modifier.at(16.dp, 22.dp), lh = .8f, tracking = -.02f)
                T(n.minutes, 86f, Modifier.at(16.dp, 96.dp), color = accent, lh = .8f, tracking = -.02f)
                T("${n.mon}\n${n.day}\n${n.year}", 22f, Modifier.at(118.dp, 16.dp), lh = 1.27f)
                Box(Modifier.at(118.dp, 140.dp)) { Mantra(s.mantra3, 10f, lh = 1.2f, align = TextAlign.Start) }
                T(n.weekday, 50f, Modifier.at(184.dp, 30.dp).verticalUp(), lh = 1f)
                TopRight(16.dp, 16.dp) { Mantra(s.mantra1, 10f, lh = 1.2f) }
                TopRight(16.dp, 60.dp) { Tick() }
                TopRight(16.dp, 140.dp) { Mantra(s.mantra2, 10f, lh = 1.2f) }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                PhotoBox(vm, w.photo, Modifier.fillMaxSize())
                T(numeral, 86f, Modifier.at(16.dp, (-6).dp).alpha(.9f), color = accent, lh = .8f)
                TopRight(16.dp, 14.dp) { Caption(s.caption, 10f, 1.2f) }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(262.dp)) {
                T(n.hours, 116f, Modifier.at(18.dp, 38.dp), lh = .8f, tracking = -.02f)
                T(n.minutes, 116f, Modifier.at(18.dp, 132.dp), color = accent, lh = .8f, tracking = -.02f)
                T("${n.mon}\n${n.day}\n${n.year}", 28f, Modifier.at(138.dp, 20.dp), lh = 40f / 28f)
                Box(Modifier.at(138.dp, 186.dp)) { Mantra(s.mantra3, 13f, tracking = .25f, lh = 15f / 13f, align = TextAlign.Start) }
                Tick(22.dp, Modifier.at(138.dp, 240.dp))
                T(n.weekday, 64f, Modifier.at(222.dp, 56.dp).verticalUp(), lh = 1f)
                TopRight(20.dp, 22.dp) { Mantra(s.mantra1, 13f, tracking = .25f, lh = 15f / 13f) }
                TopRight(20.dp, 80.dp) { Tick(22.dp) }
                TopRight(20.dp, 178.dp) { Mantra(s.mantra2, 13f, tracking = .25f, lh = 15f / 13f) }
                TopRight(20.dp, 240.dp) { Tick(22.dp) }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                PhotoBox(vm, w.photo, Modifier.fillMaxSize())
                T(numeral, 116f, Modifier.at(18.dp, (-8).dp).alpha(.9f), color = accent, lh = .8f)
                TopRight(20.dp, 18.dp) { Caption(s.caption, 13f, 15f / 13f) }
                TopRight(20.dp, 82.dp) { Tick(22.dp) }
            }
        }
    }
}

// ---------- photo / quote ----------

@Composable
fun PhotoWidget(vm: LauncherVM, w: Widget, size: Size) {
    val s = vm.settings
    val accent = LocalAccent.current
    val n = vm.clock()
    Box(Modifier.fillMaxSize()) {
        when (size) {
            Size.S -> {
                PhotoBox(vm, w.photo, Modifier.fillMaxSize())
            }
            Size.M -> {
                PhotoBox(vm, w.photo, Modifier.fillMaxSize())
                TopRight(14.dp, 14.dp) { Mantra("SIMPLE IS ENOUGH", 11f, lh = 13f / 11f) }
                TopRight(14.dp, 60.dp) { Tick() }
            }
            Size.L -> {
                PhotoBox(vm, w.photo, Modifier.fillMaxSize())
                T(if (s.heroWeek) n.week else n.day, 110f, Modifier.at(14.dp, (-8).dp).alpha(.9f), color = accent, lh = .8f)
                TopRight(14.dp, 16.dp) { Caption(s.caption, 12f, 14f / 12f) }
                TopRight(14.dp, 82.dp) { Tick() }
            }
        }
    }
}

@Composable
fun QuoteWidget(vm: LauncherVM, size: Size) {
    val s = vm.settings
    val accent = LocalAccent.current
    val n = vm.clock()
    Box(Modifier.fillMaxSize()) {
        when (size) {
            Size.S -> Box(Modifier.align(Alignment.Center).padding(horizontal = 12.dp)) {
                Label(s.quote, 12f, tracking = .32f, align = TextAlign.Center)
            }
            Size.M -> {
                Box(Modifier.align(Alignment.CenterStart).padding(start = 18.dp)) {
                    Column {
                        T(lines(s.quote), 22f, lh = 1.05f)
                        Spacer(Modifier.height(10.dp))
                        Tick()
                    }
                }
            }
            Size.L -> {
                T(if (s.heroWeek) n.week else n.day, 110f, Modifier.at(14.dp, (-8).dp), color = accent, lh = .8f)
                TopRight(14.dp, 16.dp) { Caption(s.caption, 12f, 14f / 12f) }
                Box(Modifier.align(Alignment.BottomStart).padding(16.dp)) { T(s.quote, 30f, lh = 1.05f) }
            }
        }
    }
}

@Composable
private fun Spacer(modifier: Modifier) = androidx.compose.foundation.layout.Spacer(modifier)

// ---------- calendar ----------

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

fun eventLine(e: CalEvent, today: LocalDate): Pair<String, String> {
    val zone = if (e.allDay) ZoneId.of("UTC") else ZoneId.systemDefault()
    val t = Instant.ofEpochMilli(e.start).atZone(zone)
    val d = t.toLocalDate()
    val time = if (e.allDay) "ALL DAY" else t.format(timeFmt)
    return when {
        !d.isAfter(today) -> time to e.title
        d == today.plusDays(1) -> "TOMORROW" to "${e.title} ${if (e.allDay) "" else time}".trim()
        else -> "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()} ${d.dayOfMonth}" to e.title
    }
}

@Composable
fun CalendarWidget(vm: LauncherVM, size: Size, hasPermission: Boolean) {
    val n = vm.clock()
    val accent = LocalAccent.current
    val today = vm.now.toLocalDate()
    val cal = vm.calendar
    if (size == Size.S) {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                T(today.dayOfMonth.toString(), 54f, lh = .85f)
                Column {
                    Label(n.mon, 11f, tracking = .22f, lh = 1.5f)
                    Label(n.wd, 11f, color = accent, tracking = .22f, lh = 1.5f)
                }
            }
        }
        return
    }
    val big = size == Size.L
    val events = cal?.upcoming.orEmpty().map { eventLine(it, today) }
    Column(Modifier.fillMaxSize().padding(if (big) 16.dp else 14.dp, if (big) 12.dp else 8.dp, if (big) 16.dp else 14.dp, if (big) 10.dp else 6.dp)) {
        Row(verticalAlignment = if (big) Alignment.Top else Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (big) 16.dp else 12.dp)) {
            T(n.month, if (big) 48f else 34f, lh = 1f, maxLines = 1)
            Box(Modifier.weight(1f).padding(top = if (big) 4.dp else 0.dp)) {
                if (!hasPermission) {
                    Label("TAP TO ALLOW CALENDAR", if (big) 11f else 10f, color = Muted, maxLines = 1)
                } else if (big) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (events.isEmpty()) Label("NO UPCOMING EVENTS", 11f, color = Muted)
                        events.forEach { (a, b) -> EventText(a, b, 11f) }
                    }
                } else {
                    events.firstOrNull()?.let { (a, b) -> EventText(a, b, 10f) } ?: Label("NO EVENTS", 10f, color = Muted)
                }
            }
            Label(n.year, if (big) 13f else 11f, color = Muted, tracking = .25f, modifier = Modifier.padding(top = if (big) 4.dp else 0.dp))
        }
        val monday = vm.settings.weekMonday
        val heads = if (monday) "MTWTFSS" else "SMTWTFS"
        Row(Modifier.fillMaxWidth().padding(top = if (big) 6.dp else 2.dp)) {
            heads.forEach { Label(it.toString(), if (big) 11f else 9f, Modifier.weight(1f), color = Muted, tracking = .1f, align = TextAlign.Center) }
        }
        Box(Modifier.fillMaxWidth().padding(top = if (big) 4.dp else 2.dp).height(2.dp).background(Ink))
        val first = today.withDayOfMonth(1)
        val lead = if (monday) first.dayOfWeek.value - 1 else first.dayOfWeek.value % 7
        val daysIn = today.lengthOfMonth()
        val rows = (lead + daysIn + 6) / 7
        Column(Modifier.fillMaxWidth().weight(1f)) {
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    for (c in 0 until 7) {
                        val d = r * 7 + c - lead + 1
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (d in 1..daysIn) {
                                val isToday = d == today.dayOfMonth
                                if (isToday) {
                                    Box(Modifier.background(accent).padding(horizontal = if (big) 6.dp else 4.dp)) {
                                        T(d.toString(), if (big) 14f else 11f, weight = FontWeight.SemiBold, color = Paper, lh = if (big) 18f / 14f else 14f / 11f)
                                    }
                                } else {
                                    T(d.toString(), if (big) 14f else 11f, weight = FontWeight.SemiBold, lh = 1.2f)
                                }
                                if (!isToday && cal?.eventDays?.contains(d) == true) {
                                    Box(
                                        Modifier.align(Alignment.BottomCenter).offset(y = if (big) 2.dp else 3.dp)
                                            .size(if (big) 5.dp else 4.dp).clip(CircleShape).background(accent)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventText(a: String, b: String, size: Float) {
    Row {
        Label(a, size, color = LocalAccent.current, maxLines = 1)
        Label(" · $b", size, maxLines = 1)
    }
}
