package com.paperlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.paperlauncher.LauncherVM
import com.paperlauncher.data.Apps
import com.paperlauncher.data.Builtins
import com.paperlauncher.data.Calendars
import com.paperlauncher.ui.widgets.AppGlyph
import com.paperlauncher.ui.widgets.clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DialogButton(text: String, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(30.dp).border(2.dp, Ink).background(if (filled) Ink else Color.Transparent)
            .tap(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Label(text, 10f, color = if (filled) Paper else Ink, tracking = .2f) }
}

@Composable
private fun ButtonsRow(content: @Composable () -> Unit) {
    HRule()
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

// ---------- confirm ----------

@Composable
fun ConfirmDialog(message: String, onYes: () -> Unit, onNo: () -> Unit) {
    Sheet(onNo, maxWidth = 360.dp) {
        Header("CONFIRM", 36f, onNo)
        T(message, 26f, Modifier.padding(12.dp), lh = 1.25f)
        ButtonsRow {
            DialogButton("CANCEL", false, onClick = onNo)
            DialogButton("YES", true, onClick = onYes)
        }
    }
}

// ---------- text input ----------

data class TextEdit(val title: String, val value: String, val hint: String, val secret: Boolean = false, val onSave: (String) -> Unit)

@Composable
fun TextInputDialog(e: TextEdit, onClose: () -> Unit) {
    var text by remember(e) { mutableStateOf(e.value) }
    val accent = LocalAccent.current
    Sheet(onClose) {
        Header(e.title, 36f, onClose)
        Column(Modifier.padding(12.dp)) {
            Label(e.hint, 10f, color = Muted, tracking = .15f)
            BasicTextField(
                value = text, onValueChange = { text = it },
                textStyle = textStyle(22f, FontWeight.Medium, Ink, lh = 1.2f),
                cursorBrush = SolidColor(accent),
                visualTransformation = if (e.secret) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
            )
            HRule()
        }
        ButtonsRow {
            DialogButton("CANCEL", false, onClick = onClose)
            DialogButton("SAVE", true) { e.onSave(text); onClose() }
        }
    }
}

// ---------- calendars ----------

@Composable
fun CalendarPicker(vm: LauncherVM, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val cals by produceState(emptyList<com.paperlauncher.data.CalendarInfo>(), Unit) {
        value = withContext(Dispatchers.IO) { Calendars.list(ctx) }
    }
    val chosen = remember { mutableStateListOf<Long>().apply { addAll(vm.settings.calendarIds) } }
    Sheet(onClose) {
        Header("CALENDARS", 36f, onClose)
        Label("NONE SELECTED = SHOW ALL. OUTLOOK, GOOGLE AND SAMSUNG CALENDARS APPEAR HERE ONCE SYNCED.", 10f, Modifier.padding(12.dp), color = Muted, tracking = .12f)
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(cals, key = { it.id }) { c ->
                CheckRow(c.name.uppercase(), c.account.uppercase(), c.id in chosen) {
                    if (c.id in chosen) chosen.remove(c.id) else chosen.add(c.id)
                }
            }
        }
        ButtonsRow {
            DialogButton("CANCEL", false, onClick = onClose)
            DialogButton("SAVE", true) { vm.updateSettings { it.copy(calendarIds = chosen.toSet()) }; onClose() }
        }
    }
}

@Composable
private fun CheckRow(title: String, sub: String, checked: Boolean, glyph: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().tap(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(18.dp).border(2.dp, Ink).padding(3.dp).background(if (checked) LocalAccent.current else Color.Transparent))
        if (glyph != null) Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) { glyph() }
        Column(Modifier.weight(1f)) {
            T(title, 22f, lh = 1.05f, maxLines = 1, ellipsis = true)
            if (sub.isNotEmpty()) Label(sub, 9f, color = Muted, tracking = .12f, maxLines = 1)
        }
    }
}

// ---------- app picker for shortcut widgets ----------

@Composable
fun AppPicker(vm: LauncherVM, widgetId: String) {
    val ctx = LocalContext.current
    val key = vm.displayKey
    val widget = vm.layouts[key]?.firstOrNull { it.id == widgetId }
    if (widget == null) { vm.appPickerFor = null; return }
    val chosen = remember(widgetId) { mutableStateListOf<String>().apply { addAll(widget.apps) } }
    val apps by produceState(Apps.list(ctx), Unit) { value = withContext(Dispatchers.IO) { Apps.list(ctx) } }
    fun close() { vm.appPickerFor = null }
    fun toggle(k: String) {
        if (k in chosen) chosen.remove(k) else if (chosen.size < 6) chosen.add(k)
    }
    Sheet({ close() }) {
        Header("PICK APPS", 36f) { close() }
        Label("UP TO 6 · ${chosen.size} CHOSEN · SMALL SHOWS 1, MEDIUM 2, LARGE 6", 10f, Modifier.padding(12.dp), color = Muted, tracking = .12f)
        LazyColumn(Modifier.heightIn(max = 460.dp)) {
            item { GroupLabel("SHORTCUTS", 14f) }
            items(Builtins, key = { it.key }) { b ->
                CheckRow(b.label, "", b.key in chosen, glyph = { Glyph(b.glyph, 22f) }) { toggle(b.key) }
            }
            item { GroupLabel("APPS", 14f) }
            items(apps, key = { it.key + it.user.hashCode() }) { a ->
                CheckRow(a.label, "", a.key in chosen, glyph = { AppGlyph(a.key, 22f) }) { toggle(a.key) }
            }
        }
        ButtonsRow {
            DialogButton("CANCEL", false) { close() }
            DialogButton("SAVE", true) {
                vm.updateWidget(key, widgetId) { it.copy(apps = chosen.toList()) }
                close()
            }
        }
    }
}

// ---------- accent color ----------

private fun hsvOf(argb: Long): FloatArray = FloatArray(3).also { android.graphics.Color.colorToHSV(argb.toInt(), it) }
private fun argbOf(h: Float, s: Float, v: Float): Long = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)).toLong() and 0xFFFFFFFFL

@Composable
fun AccentDialog(vm: LauncherVM) {
    val start = remember { hsvOf(vm.settings.accent) }
    var h by remember { mutableStateOf(start[0]) }
    var s by remember { mutableStateOf(start[1]) }
    var v by remember { mutableStateOf(start[2]) }
    val pick = argbOf(h, s, v)
    val pickColor = Color(pick)
    val hex = "#%06X".format(pick and 0xFFFFFFL)
    val n = vm.clock()
    fun close() { vm.accentDialog = false }
    fun set(argb: Long) { val x = hsvOf(argb); h = x[0]; s = x[1]; v = x[2] }

    Sheet({ close() }, maxWidth = 380.dp) {
        Header("ACCENT COLOR", 36f) { close() }
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            T(n.hours, 58f, lh = .9f)
            T(" ${n.minutes}", 58f, color = pickColor, lh = .9f)
            Column(Modifier.padding(start = 12.dp)) {
                Label("SAME SKY\nDIFFERENT STORY", 9f, tracking = .2f, lh = 1.4f)
                Tick(color = pickColor, modifier = Modifier.padding(top = 6.dp))
            }
        }
        HRule()
        val rows = AccentSwatches.chunked(6)
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { sw ->
                        val selected = (sw and 0xFFFFFFL) == (pick and 0xFFFFFFL)
                        Box(
                            Modifier.size(40.dp).border(2.dp, Ink).background(Color(sw)).tap { set(sw) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Box(Modifier.size(30.dp).border(3.dp, Paper).padding(3.dp).border(2.dp, Ink))
                        }
                    }
                }
            }
        }
        HRule()
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HsvSlider("HUE", h / 360f) { h = it * 360f }
            HsvSlider("SATURATION", s) { s = it }
            HsvSlider("BRIGHTNESS", v) { v = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("HEX", 9f, Modifier.width(96.dp), tracking = .2f)
                T(hex, 26f, lh = 1f)
            }
        }
        ButtonsRow {
            DialogButton("RESET", false) { set(0xFF7A1F2E) }
            DialogButton("USE THIS", true) { vm.updateSettings { it.copy(accent = pick or 0xFF000000L) }; close() }
        }
    }
}

@Composable
private fun HsvSlider(name: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Label(name, 9f, Modifier.width(96.dp), tracking = .2f)
        BoxWithConstraints(
            Modifier.weight(1f).height(36.dp)
                .pointerInput(Unit) { detectTapGestures { onChange((it.x / size.width).coerceIn(0f, 1f)) } }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        onChange((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val pill = RoundedCornerShape(7.dp)
            Box(Modifier.fillMaxWidth().height(14.dp).clip(pill).background(Track))
            Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(14.dp).clip(pill).background(Ink))
            Box(Modifier.offset(x = (maxWidth - 4.dp) * value.coerceIn(0f, 1f)).size(4.dp, 36.dp).background(Ink))
        }
    }
}

// ---------- single choice ----------

@Composable
fun ChoiceDialog(title: String, options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit, onClose: () -> Unit) {
    Sheet(onClose, maxWidth = 360.dp) {
        Header(title, 36f, onClose)
        Column(Modifier.padding(vertical = 8.dp)) {
            options.forEach { (key, label) ->
                CheckRow(label, "", key == selected) { onPick(key); onClose() }
            }
        }
    }
}

// ---------- sports teams ----------

@Composable
fun SportsPicker(vm: LauncherVM, onClose: () -> Unit) {
    val chosen = remember { mutableStateListOf<com.paperlauncher.data.TeamRef>().apply { addAll(com.paperlauncher.data.Sports.parse(vm.settings.sportsTeams)) } }
    var league by remember { mutableStateOf(chosen.firstOrNull()?.league ?: "NBA") }
    var filter by remember { mutableStateOf("") }
    val accent = LocalAccent.current
    val teams by produceState<List<com.paperlauncher.data.TeamRef>?>(null, league) {
        value = null
        value = runCatching { com.paperlauncher.data.Sports.teams(league) }.getOrDefault(emptyList())
    }
    fun isChosen(t: com.paperlauncher.data.TeamRef) = chosen.any { it.league == t.league && (it.id == t.id || (it.id.isEmpty() && t.abbr.isNotEmpty() && it.abbr.startsWith(t.abbr))) }
    Sheet(onClose) {
        Header("SPORTS TEAMS", 36f, onClose)
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label(if (chosen.isEmpty()) "NO TEAMS YET · UP TO 4 · LIVE GAMES SHOW FIRST" else "FOLLOWING · TAP TO REMOVE", 10f, color = Muted, tracking = .12f)
            if (chosen.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chosen.toList().forEach { t ->
                        Box(Modifier.height(28.dp).background(Ink).tap { chosen.remove(t) }.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                            Label("${t.league} ${t.abbr} ×", 10f, color = Paper, tracking = .15f)
                        }
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                com.paperlauncher.data.Sports.Leagues.forEach { l ->
                    val sel = l.key == league
                    Box(
                        Modifier.height(28.dp).border(2.dp, if (sel) accent else Ink).background(if (sel) accent else Color.Transparent)
                            .tap { league = l.key; filter = "" }.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Label(l.key, 10f, color = if (sel) Paper else Ink, tracking = .15f) }
                }
            }
            Box {
                if (filter.isEmpty()) T("FILTER TEAMS…", 20f, color = Muted, lh = 1.2f)
                BasicTextField(
                    value = filter, onValueChange = { filter = it }, singleLine = true,
                    textStyle = textStyle(20f, FontWeight.Medium, Ink, lh = 1.2f),
                    cursorBrush = SolidColor(accent), modifier = Modifier.fillMaxWidth(),
                )
            }
            HRule()
        }
        val list = teams
        Box(Modifier.heightIn(min = 120.dp, max = 380.dp)) {
            when {
                list == null -> Label("LOADING $league TEAMS…", 10f, Modifier.padding(12.dp), color = Muted, tracking = .15f)
                list.isEmpty() -> Label("COULDN'T LOAD TEAMS · CHECK CONNECTION", 10f, Modifier.padding(12.dp), color = Muted, tracking = .15f)
                else -> LazyColumn {
                    val q = filter.trim().uppercase()
                    items(list.filter { q.isEmpty() || it.name.contains(q) || it.abbr.contains(q) }, key = { it.league + it.id }) { t ->
                        CheckRow(t.name, t.abbr, isChosen(t)) {
                            if (isChosen(t)) chosen.removeAll { c -> c.league == t.league && (c.id == t.id || c.id.isEmpty()) && (c.id.isNotEmpty() || c.abbr.startsWith(t.abbr)) }
                            else if (chosen.size < 4) chosen.add(t)
                        }
                    }
                }
            }
        }
        ButtonsRow {
            DialogButton("CANCEL", false, onClick = onClose)
            DialogButton("SAVE", true) { vm.updateSettings { it.copy(sportsTeams = com.paperlauncher.data.Sports.encode(chosen)) }; onClose() }
        }
    }
}
