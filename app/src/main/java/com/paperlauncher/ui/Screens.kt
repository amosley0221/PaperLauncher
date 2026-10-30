package com.paperlauncher.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paperlauncher.LauncherVM
import com.paperlauncher.Overlay
import com.paperlauncher.data.AppInfo
import com.paperlauncher.data.Apps
import com.paperlauncher.data.Calendars
import com.paperlauncher.data.Media
import com.paperlauncher.data.Sports
import com.paperlauncher.data.Weather
import com.paperlauncher.data.toast
import com.paperlauncher.model.Size
import com.paperlauncher.model.WidgetGroups
import com.paperlauncher.ui.widgets.AppGlyph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Swallows touches so nothing underneath reacts. */
fun Modifier.blockTouches() = this.pointerInput(Unit) { detectTapGestures { } }

@Composable
fun Header(title: String, size: Float, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        T(title, size, Modifier.weight(1f), lh = .95f)
        Glyph("close", 24f, Modifier.tap(onClick = onClose), color = LocalAccent.current)
    }
    HRule()
}

@Composable
fun GroupLabel(text: String, size: Float = 16f) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Label(text, size * 0.62f, color = LocalAccent.current, tracking = .12f)
        Box(Modifier.padding(start = 10.dp).weight(1f).height(2.dp).background(Ink))
    }
}

/** Full-screen paper page with a 2px ink frame inset 10dp. */
@Composable
fun PaperPage(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Paper).blockTouches().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        Column(Modifier.fillMaxSize().padding(10.dp).border(2.dp, Ink), content = content)
    }
}

/** Centered modal over the scrim. */
@Composable
fun Sheet(onDismiss: () -> Unit, maxWidth: Dp = 420.dp, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Scrim).pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = maxWidth).fillMaxWidth().background(Paper).border(2.dp, Ink).blockTouches(),
            content = content,
        )
    }
}

// ---------- settings ----------

@Composable
private fun SettingRow(label: String, value: String, swatch: Color? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).tap(onClick != null) { onClick?.invoke() }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T(label, 26f, Modifier.weight(1f), lh = 1f, maxLines = 1)
        Box(Modifier.width(128.dp)) {
            if (swatch != null) Box(Modifier.size(22.dp).border(2.dp, Ink).padding(3.dp).background(swatch))
            else Label(value, 11f, color = Muted, tracking = .18f, maxLines = 1)
        }
    }
}

@Composable
fun SettingsScreen(vm: LauncherVM) {
    val ctx = LocalContext.current
    val actions = LocalActions.current
    val s = vm.settings
    var textEdit by remember { mutableStateOf<TextEdit?>(null) }
    var calendarsOpen by remember { mutableStateOf(false) }
    var sportsOpen by remember { mutableStateOf(false) }
    var gesture by remember { mutableStateOf<String?>(null) }
    PaperPage {
        Header("SETTINGS", 50f) { vm.overlay = Overlay.NONE }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            GroupLabel("GENERAL")
            SettingRow("HOME APP", "PAPER LAUNCHER") { Apps.start(ctx, Intent(AndroidSettings.ACTION_HOME_SETTINGS)) }
            SettingRow("CLOCK", if (s.clock24) "24-HOUR" else "12-HOUR") { vm.updateSettings { it.copy(clock24 = !it.clock24) } }
            SettingRow("TEMPERATURE", if (s.fahrenheit) "FAHRENHEIT" else "CELSIUS") { vm.updateSettings { it.copy(fahrenheit = !it.fahrenheit) } }
            SettingRow("WEEK STARTS ON", if (s.weekMonday) "MONDAY" else "SUNDAY") { vm.updateSettings { it.copy(weekMonday = !it.weekMonday) } }
            SettingRow("STATUS BAR", if (s.hideStatusBar) "HIDDEN" else "SHOWN") { vm.updateSettings { it.copy(hideStatusBar = !it.hideStatusBar) } }
            SettingRow("BACKUP SETTINGS", "SAVE A FILE") { actions.backup() }
            SettingRow("RESTORE SETTINGS", "OPEN A FILE") { actions.restore() }

            GroupLabel("ACCOUNTS")
            val car = vm.car.data
            SettingRow("TESSIE", if (s.tessieToken.isBlank()) "ADD TOKEN" else if (car != null) "CONNECTED · ${car.model}" else "TOKEN SAVED") {
                textEdit = TextEdit("TESSIE TOKEN", s.tessieToken, "PASTE YOUR API TOKEN FROM DASH.TESSIE.COM → SETTINGS → API", secret = true) { v ->
                    vm.updateSettings { it.copy(tessieToken = v.trim()) }
                }
            }
            val teams = Sports.parse(s.sportsTeams)
            SettingRow("SPORTS TEAMS", if (teams.isEmpty()) "PICK TEAMS" else teams.joinToString { it.abbr }) { sportsOpen = true }
            SettingRow("CALENDARS", if (!Calendars.hasPermission(ctx)) "TAP TO ALLOW" else if (s.calendarIds.isEmpty()) "ALL" else "${s.calendarIds.size} SELECTED") {
                if (Calendars.hasPermission(ctx)) calendarsOpen = true else actions.requestCalendar()
            }
            SettingRow("AI AGENTS", "CLAUDE, GROK, CODEX") {
                val have = listOf("com.anthropic.claude" to "CLAUDE", "ai.x.grok" to "GROK", "com.openai.chatgpt" to "CHATGPT")
                    .filter { Apps.isInstalled(ctx, it.first) }.joinToString { it.second }
                toast(ctx, if (have.isEmpty()) "NO AGENT APPS · WILL OPEN THE WEB" else "INSTALLED · $have")
            }
            SettingRow("MEDIA ACCESS", if (Media.hasAccess(ctx)) "ALLOWED" else "TAP TO ALLOW") { Media.requestAccess(ctx) }
            SettingRow("LOCATION", if (Weather.hasPermission(ctx)) "ALLOWED" else "TAP TO ALLOW") {
                if (Weather.hasPermission(ctx)) Apps.start(ctx, Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                else actions.requestLocation()
            }

            GroupLabel("LOOK")
            SettingRow("ACCENT COLOR", "", swatch = Color(s.accent)) { vm.accentDialog = true }
            SettingRow("FOLD INNER LAYOUT", if (s.innerHero) "HERO LEFT" else "WIDGET GRID") { vm.updateSettings { it.copy(innerHero = !it.innerHero) } }
            SettingRow("BIG NUMERAL", if (s.heroWeek) "WEEK OF YEAR" else "DAY OF MONTH") { vm.updateSettings { it.copy(heroWeek = !it.heroWeek) } }
            SettingRow("WALLPAPER", "PAPER")
            SettingRow("ICON PACK", "MONO GLYPHS")
            SettingRow("APP ICON SIZE", listOf("SMALL", "MEDIUM", "LARGE")[s.iconSize.coerceIn(0, 2)]) { vm.updateSettings { it.copy(iconSize = (it.iconSize + 1) % 3) } }
            SettingRow("CORNER ROUNDING", if (s.rounded) "ROUNDED" else "SQUARE") { vm.updateSettings { it.copy(rounded = !it.rounded) } }
            SettingRow("APP NAMES ON HOME", if (s.showAppNames) "SHOWN" else "HIDDEN") { vm.updateSettings { it.copy(showAppNames = !it.showAppNames) } }

            GroupLabel("GESTURES")
            fun gestureLabel(k: String) = GestureActions.firstOrNull { it.first == k }?.second ?: "NOTHING"
            SettingRow("SWIPE DOWN", gestureLabel(s.swipeDown)) { gesture = "down" }
            SettingRow("SWIPE UP", gestureLabel(s.swipeUp)) { gesture = "up" }
            SettingRow("LONG-PRESS", "EDIT HOME")
            SettingRow("SWIPE LEFT / RIGHT", "CHANGE PAGE")

            GroupLabel("WORDS")
            @Composable
            fun words(title: String, v: String, set: (String) -> Unit) = SettingRow(title, v) {
                textEdit = TextEdit(title, v, "SHOWN IN CAPITALS, ONE WORD PER LINE") { set(it.trim().uppercase()) }
            }
            words("MANTRA · TOP", s.mantra1) { v -> vm.updateSettings { it.copy(mantra1 = v) } }
            words("MANTRA · BOTTOM", s.mantra2) { v -> vm.updateSettings { it.copy(mantra2 = v) } }
            words("MANTRA · SIDE", s.mantra3) { v -> vm.updateSettings { it.copy(mantra3 = v) } }
            words("PHOTO CAPTION", s.caption) { v -> vm.updateSettings { it.copy(caption = v) } }
            words("QUOTE", s.quote) { v -> vm.updateSettings { it.copy(quote = v) } }

            GroupLabel("LAYOUT")
            SettingRow("EDIT HOME SCREEN", "LONG-PRESS") { vm.overlay = Overlay.NONE; vm.editing = true }
            SettingRow("RESET THIS SCREEN", vm.displayKey.name.replace('_', ' ')) {
                actions.confirm("RESET THIS SCREEN'S WIDGETS?") { vm.resetLayout(vm.displayKey) }
            }
            SettingRow("VERSION", runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0")
        }
    }
    textEdit?.let { e -> TextInputDialog(e) { textEdit = null } }
    if (calendarsOpen) CalendarPicker(vm) { calendarsOpen = false }
    if (sportsOpen) SportsPicker(vm) { sportsOpen = false }
    gesture?.let { which ->
        ChoiceDialog(
            if (which == "down") "SWIPE DOWN" else "SWIPE UP",
            GestureActions,
            if (which == "down") s.swipeDown else s.swipeUp,
            onPick = { k -> vm.updateSettings { if (which == "down") it.copy(swipeDown = k) else it.copy(swipeUp = k) } },
            onClose = { gesture = null },
        )
    }
}

// ---------- add widget ----------

@Composable
fun AddWidgetSheet(vm: LauncherVM) {
    val accent = LocalAccent.current
    Sheet({ vm.overlay = Overlay.NONE }) {
        Header("ADD WIDGET", 36f) { vm.overlay = Overlay.NONE }
        Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
            WidgetGroups.forEach { (group, types) ->
                GroupLabel("WIDGET · $group", 14f)
                types.forEach { t ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        T(t.title, 26f, Modifier.weight(1f), lh = 1f)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Size.entries.forEach { z ->
                                val sp = t.span(z)
                                val fits = sp.w <= vm.displayKey.cols
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier.size(28.dp).border(2.dp, if (fits) accent else Track)
                                            .tap(fits) { vm.add(vm.displayKey, t, z, vm.gridRows) },
                                        contentAlignment = Alignment.Center,
                                    ) { Label(z.name, 11f, color = if (fits) accent else Track, tracking = 0f) }
                                    Label("${sp.w}×${sp.h}", 7f, color = Muted, tracking = .05f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- all apps / search ----------

data class ContactHit(val name: String, val uri: Uri)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsScreen(vm: LauncherVM, search: Boolean) {
    val ctx = LocalContext.current
    val actions = LocalActions.current
    val accent = LocalAccent.current
    var query by remember { mutableStateOf(vm.searchQuery) }
    LaunchedEffect(Unit) { vm.searchQuery = "" }
    val apps by produceState(Apps.list(ctx), Unit) { value = withContext(Dispatchers.IO) { Apps.list(ctx, refresh = true) } }
    val q = query.trim().uppercase()
    val filtered = if (q.isEmpty()) apps else apps.filter { it.label.contains(q) }
    val contactsAllowed = ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    val contacts by produceState(emptyList<ContactHit>(), q, contactsAllowed) {
        value = if (q.length < 2 || !contactsAllowed) emptyList() else withContext(Dispatchers.IO) { queryContacts(ctx, q) }
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(search) { if (search) runCatching { focus.requestFocus() } }
    fun close() { vm.overlay = Overlay.NONE }
    fun launch(a: AppInfo) { Apps.launch(ctx, a); close() }
    fun web() { Apps.web(ctx, "https://www.google.com/search?q=" + Uri.encode(query.trim())); close() }

    PaperPage {
        Header(if (search) "SEARCH" else "ALL APPS", 50f) { close() }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) T("SEARCH…", 30f, color = Muted, lh = 1.1f)
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = textStyle(30f, FontWeight.Medium, Ink, lh = 1.1f),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { filtered.firstOrNull()?.let { launch(it) } ?: web() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            Glyph("mic", 24f, Modifier.tap { actions.listen { query = it } })
        }
        HRule()
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            if (q.isEmpty()) {
                val groups = filtered.groupBy { a -> a.label.firstOrNull()?.let { if (it.isLetter()) it.toString() else "#" } ?: "#" }
                    .toSortedMap(compareBy<String> { if (it == "#") 0 else 1 }.thenBy { it })
                groups.forEach { (letter, list) ->
                    item(key = "h-$letter") { GroupLabel(letter, 20f) }
                    items(list, key = { it.key + it.user.hashCode() }) { a -> AppRow(a, { launch(a) }, { Apps.openInfo(ctx, a) }) }
                }
            } else {
                if (filtered.isNotEmpty()) item { GroupLabel("APPS", 16f) }
                items(filtered, key = { it.key + it.user.hashCode() }) { a -> AppRow(a, { launch(a) }, { Apps.openInfo(ctx, a) }) }
                item { GroupLabel("CONTACTS", 16f) }
                if (!contactsAllowed) {
                    item { Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).tap { actions.requestContacts() }) { Label("TAP TO ALLOW CONTACTS", 12f, color = Muted, tracking = .18f) } }
                } else {
                    items(contacts) { c ->
                        T(c.name, 30f, Modifier.fillMaxWidth().tap { Apps.start(ctx, Intent(Intent.ACTION_VIEW, c.uri)); close() }.padding(horizontal = 12.dp, vertical = 6.dp), lh = 1f, maxLines = 1)
                    }
                    if (contacts.isEmpty()) item { Label("NO MATCHES", 12f, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Muted, tracking = .18f) }
                }
                item { GroupLabel("WEB", 16f) }
                item {
                    Row(Modifier.fillMaxWidth().tap { web() }.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Glyph("public", 24f, color = accent)
                        T("  SEARCH “${q}”", 30f, lh = 1f, maxLines = 1, ellipsis = true)
                    }
                }
            }
            item { Box(Modifier.height(12.dp)) }
        }
        HRule()
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                listOf("phone", "messages", "browser", "camera", "mail", "maps").forEach { k ->
                    Box(Modifier.tap { Apps.open(ctx, k); close() }) { AppGlyph(k, 22f) }
                }
            }
            Box(Modifier.width(20.dp))
            Box(
                Modifier.size(34.dp).border(2.dp, Ink).tap { vm.overlay = Overlay.SETTINGS },
                contentAlignment = Alignment.Center,
            ) { Glyph("tune", 20f) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(a: AppInfo, onClick: () -> Unit, onLong: () -> Unit) {
    T(
        a.label, 30f,
        Modifier.fillMaxWidth()
            .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onLongClick = onLong, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        lh = 1f, maxLines = 1, ellipsis = true,
    )
}

private fun queryContacts(ctx: android.content.Context, q: String): List<ContactHit> = runCatching {
    val out = mutableListOf<ContactHit>()
    val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(q))
    ctx.contentResolver.query(
        uri,
        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
        null, null, null,
    )?.use { c ->
        while (c.moveToNext() && out.size < 8) {
            out += ContactHit((c.getString(2) ?: "").uppercase(), ContactsContract.Contacts.getLookupUri(c.getLong(0), c.getString(1)))
        }
    }
    out
}.getOrDefault(emptyList())
