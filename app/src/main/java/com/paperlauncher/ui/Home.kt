package com.paperlauncher.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.paperlauncher.LauncherVM
import com.paperlauncher.MainActivity
import com.paperlauncher.Overlay
import com.paperlauncher.data.Apps
import com.paperlauncher.data.BatteryInfo
import com.paperlauncher.data.Calendars
import com.paperlauncher.data.Device
import com.paperlauncher.data.Media
import com.paperlauncher.data.Shade
import com.paperlauncher.data.SignalInfo
import com.paperlauncher.data.Sports
import com.paperlauncher.data.StorageInfo
import com.paperlauncher.data.Weather
import com.paperlauncher.data.toast
import com.paperlauncher.model.DisplayKey
import com.paperlauncher.model.Size
import com.paperlauncher.model.WType
import com.paperlauncher.model.Widget
import com.paperlauncher.ui.widgets.AppsWidget
import com.paperlauncher.ui.widgets.AskWidget
import com.paperlauncher.ui.widgets.BatteryWidget
import com.paperlauncher.ui.widgets.CalendarWidget
import com.paperlauncher.ui.widgets.CarWidget
import com.paperlauncher.ui.widgets.ClockWidget
import com.paperlauncher.ui.widgets.DateWidget
import com.paperlauncher.ui.widgets.HeroWidget
import com.paperlauncher.ui.widgets.MusicWidget
import com.paperlauncher.ui.widgets.PhotoWidget
import com.paperlauncher.ui.widgets.QuoteWidget
import com.paperlauncher.ui.widgets.SearchWidget
import com.paperlauncher.ui.widgets.SportsWidget
import com.paperlauncher.ui.widgets.StorageWidget
import com.paperlauncher.ui.widgets.WeatherWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/** Side effects that need an Activity (permissions, pickers, speech). */
class Actions(
    val requestLocation: () -> Unit,
    val requestCalendar: () -> Unit,
    val requestContacts: () -> Unit,
    val pickPhoto: (String) -> Unit,
    val pickApps: (String) -> Unit,
    val listen: ((String) -> Unit) -> Unit,
    val confirm: (String, () -> Unit) -> Unit,
    val backup: () -> Unit,
    val restore: () -> Unit,
)

val LocalActions = staticCompositionLocalOf<Actions> { error("Actions not provided") }

class Live(val battery: BatteryInfo, val signal: SignalInfo, val storage: StorageInfo)

@Composable
fun LauncherRoot(vm: LauncherVM) {
    val ctx = LocalContext.current
    val accent = Color(vm.settings.accent)
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var photoFor by remember { mutableStateOf<String?>(null) }
    var speechCb by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val locationPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.refreshWeather()
    }
    val calendarPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.refreshCalendar()
    }
    val contactsPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val id = photoFor
        if (uri != null && id != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.updateWidget(vm.displayKey, id) { it.copy(photo = uri.toString()) }
        }
        photoFor = null
    }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val text = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (res.resultCode == Activity.RESULT_OK && text != null) speechCb?.invoke(text)
        speechCb = null
    }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.backupJson().toByteArray()) }
            toast(ctx, "SETTINGS SAVED")
        }.onFailure { toast(ctx, "BACKUP FAILED") }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            toast(ctx, if (text != null && vm.restoreJson(text)) "SETTINGS RESTORED" else "NOT A PAPER LAUNCHER BACKUP")
        }
    }

    val actions = remember {
        Actions(
            requestLocation = { locationPerm.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            requestCalendar = { calendarPerm.launch(Manifest.permission.READ_CALENDAR) },
            requestContacts = { contactsPerm.launch(Manifest.permission.READ_CONTACTS) },
            pickPhoto = { id -> photoFor = id; runCatching { photoPicker.launch(arrayOf("image/*")) } },
            pickApps = { id -> vm.appPickerFor = id },
            listen = { cb ->
                speechCb = cb
                val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                runCatching { speech.launch(i) }.onFailure { toast(ctx, "NO SPEECH RECOGNIZER") }
            },
            confirm = { msg, yes -> confirm = msg to yes },
            backup = { runCatching { backup.launch("paper-launcher-backup.json") } },
            restore = { runCatching { restore.launch(arrayOf("application/json", "text/plain", "*/*")) } },
        )
    }

    LaunchedEffect(vm.settings.hideStatusBar) { (ctx as? MainActivity)?.applyStatusBar() }

    BackHandler(enabled = true) {
        when {
            confirm != null -> confirm = null
            vm.accentDialog -> vm.accentDialog = false
            vm.appPickerFor != null -> vm.appPickerFor = null
            vm.overlay != Overlay.NONE -> vm.overlay = Overlay.NONE
            vm.editing -> vm.editing = false
        }
    }

    CompositionLocalProvider(LocalAccent provides accent, LocalActions provides actions) {
        Box(Modifier.fillMaxSize().background(Paper)) {
            HomeScreen(vm)
            when (vm.overlay) {
                Overlay.ADD -> AddWidgetSheet(vm)
                Overlay.SETTINGS -> SettingsScreen(vm)
                Overlay.APPS -> AllAppsScreen(vm, search = false)
                Overlay.SEARCH -> AllAppsScreen(vm, search = true)
                Overlay.NONE -> {}
            }
            vm.appPickerFor?.let { AppPicker(vm, it) }
            if (vm.accentDialog) AccentDialog(vm)
            confirm?.let { (msg, yes) ->
                ConfirmDialog(msg, onYes = { confirm = null; yes() }, onNo = { confirm = null })
            }
        }
    }
}

/** Which layout set to show for the current window size (fold inner / cover / tablet). */
fun displayKeyFor(wdp: Float, hdp: Float, heroPreset: Boolean): DisplayKey = when {
    wdp < 600f -> DisplayKey.COVER
    hdp >= wdp * 1.3f -> DisplayKey.TAB_P
    wdp > hdp * 1.1f -> DisplayKey.TAB_L
    else -> if (heroPreset) DisplayKey.INNER_HERO else DisplayKey.INNER_GRID
}

private data class Geometry(val cols: Int, val rows: Int, val cell: Float, val gap: Float, val left: Float, val top: Float) {
    val step get() = cell + gap
    fun x(col: Int) = (col - 1) * step
    fun y(row: Int) = (row - 1) * step
    fun span(n: Int) = n * cell + (n - 1) * gap
    val width get() = span(cols)
}

@Composable
private fun HomeScreen(vm: LauncherVM) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val slide = remember { Animatable(0f) }

    val battery by produceState(Device.battery(ctx)) { while (true) { value = Device.battery(ctx); delay(30_000) } }
    val signal by produceState(Device.signal(ctx)) { while (true) { value = Device.signal(ctx); delay(30_000) } }
    val storage by produceState(Device.storage(ctx)) { while (true) { value = Device.storage(ctx); delay(60_000) } }
    val live = Live(battery, signal, storage)

    // Keep the keyboard out of these insets: if it shrank the area, the layout would switch
    // (e.g. to tablet landscape), the focused field would vanish and the keyboard would close.
    val view = LocalView.current
    val imeBottom = WindowInsets.ime.getBottom(density).toFloat()
    val margin = with(density) { 16.dp.toPx() }
    val anchor = vm.imeAnchor
    val target = if (imeBottom > 0f && anchor != null) maxOf(0f, anchor + margin - (view.rootView.height - imeBottom)) else 0f
    val shift by animateFloatAsState(target, tween(200), label = "ime")
    LaunchedEffect(shift) { vm.imeShift = shift }

    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))) {
        val dk = displayKeyFor(maxWidth.value, maxHeight.value, vm.settings.innerHero)
        LaunchedEffect(dk) { vm.displayKey = dk }
        val layout = vm.layouts[dk].orEmpty()
        val page = vm.pageOf(dk).coerceAtMost(vm.pageCount(dk) - 1)
        val pageCount = vm.pageCount(dk)

        val geo = with(density) {
            val pad = 12.dp.toPx()
            val gap = 8.dp.toPx()
            val bottom = 36.dp.toPx()
            val cols = dk.cols
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            val availH = h - pad - bottom
            var cell = (w - 2 * pad - gap * (cols - 1)) / cols
            val used = layout.maxOfOrNull { it.row + it.h - 1 } ?: 1
            val fit = floor((availH + gap) / (cell + gap)).toInt()
            if (used > fit) cell = minOf(cell, (availH - gap * (used - 1)) / used)
            val rows = maxOf(used, floor((availH + gap) / (cell + gap)).toInt())
            val gridW = cols * cell + (cols - 1) * gap
            Geometry(cols, rows, cell, gap, (w - gridW) / 2, pad)
        }

        LaunchedEffect(geo.rows) { vm.gridRows = geo.rows }

        fun goPage(p: Int) {
            val target = p.coerceIn(0, pageCount - 1)
            if (target == page) return
            val dir = if (target > page) 1f else -1f
            vm.setPage(dk, target)
            scope.launch {
                slide.snapTo(dir * 0.35f)
                slide.animateTo(0f, tween(250))
            }
        }

        val editing = vm.editing
        val swipeThreshold = with(density) { 60.dp.toPx() }
        val upThreshold = with(density) { 80.dp.toPx() }

        Box(
            Modifier.fillMaxSize()
                .pointerInput(editing, dk, page, pageCount) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val slop = viewConfiguration.touchSlop
                        var mode = 0 // 1 horizontal swipe, 2 swipe up, 3 swipe down, -1 other
                        val timedOut = withTimeoutOrNull(500L) {
                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Initial)
                                val ch = ev.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
                                if (!ch.pressed) return@withTimeoutOrNull false
                                val d = ch.position - down.position
                                if (!editing && abs(d.x) > slop && abs(d.x) > abs(d.y)) { mode = 1; return@withTimeoutOrNull false }
                                if (!editing && d.y < -slop && abs(d.y) > abs(d.x)) { mode = 2; return@withTimeoutOrNull false }
                                if (!editing && d.y > slop && abs(d.y) > abs(d.x)) { mode = 3; return@withTimeoutOrNull false }
                                if (d.getDistance() > slop) { mode = -1; return@withTimeoutOrNull false }
                            }
                            @Suppress("UNREACHABLE_CODE") false
                        } == null
                        if (timedOut && mode == 0 && !editing) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.editing = true
                            // Swallow the rest of this gesture so the widget under the finger doesn't click.
                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Initial)
                                ev.changes.forEach { it.consume() }
                                if (ev.changes.none { it.pressed }) break
                            }
                        } else if (mode != 0 && !editing) {
                            // Any drag outside edit mode belongs to us, so the widget underneath never treats it as a tap.
                            var last = down.position
                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Initial)
                                val ch = ev.changes.firstOrNull { it.id == down.id }
                                ev.changes.forEach { it.consume() }
                                if (ch == null || !ch.pressed) break
                                last = ch.position
                            }
                            val d = last - down.position
                            if (mode == 1 && abs(d.x) > swipeThreshold) goPage(page + if (d.x < 0) 1 else -1)
                            if (mode == 2 && d.y < -upThreshold) runGesture(vm, ctx, vm.settings.swipeUp)
                            if (mode == 3 && d.y > upThreshold) runGesture(vm, ctx, vm.settings.swipeDown)
                        }
                    }
                }
        ) {
            Box(
                Modifier
                    .offset { IntOffset(geo.left.roundToInt(), geo.top.roundToInt()) }
                    .size(with(density) { geo.width.toDp() }, with(density) { geo.span(geo.rows).toDp() })
                    .graphicsLayer {
                        translationX = slide.value * size.width
                        translationY = -shift
                        alpha = 1f - abs(slide.value) * 1.2f
                    }
            ) {
                layout.filter { it.page == page }.forEach { w ->
                    key(w.id) { WidgetFrame(vm, dk, w, geo, live, pageCount) { goPage(it) } }
                }
            }
            BottomBar(vm, dk, page, pageCount, Modifier.align(Alignment.BottomCenter)) { goPage(it) }
        }
    }
}

@Composable
private fun BottomBar(vm: LauncherVM, key: DisplayKey, page: Int, count: Int, modifier: Modifier, onPage: (Int) -> Unit) {
    val accent = LocalAccent.current
    Box(modifier.fillMaxWidth().height(36.dp).padding(horizontal = 12.dp)) {
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until count) {
                Box(
                    Modifier.size(8.dp)
                        .then(if (i == page) Modifier.background(Ink) else Modifier.border(2.dp, Ink))
                        .tap { onPage(i) }
                )
            }
        }
        if (vm.editing) {
            Row(Modifier.align(Alignment.CenterStart), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BarButton("+ ADD WIDGET", accent, Color.Transparent, accent) { vm.overlay = Overlay.ADD }
                BarButton("SETTINGS", Ink, Color.Transparent, Ink) { vm.overlay = Overlay.SETTINGS }
            }
            Box(Modifier.align(Alignment.CenterEnd)) {
                BarButton("DONE", Ink, Paper, Ink) { vm.editing = false }
            }
        }
    }
}

@Composable
private fun BarButton(text: String, border: Color, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier.height(24.dp).border(2.dp, border).background(bg).tap(onClick = onClick).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Label(text, 9f, color = fg, tracking = .2f) }
}

private val Triangle = GenericShape { size, _ ->
    moveTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
private fun WidgetFrame(
    vm: LauncherVM,
    key: DisplayKey,
    w: Widget,
    geo: Geometry,
    live: Live,
    pageCount: Int,
    onPage: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val accent = LocalAccent.current
    val actions = LocalActions.current
    val ctx = LocalContext.current
    val editing = vm.editing
    val shape = if (vm.settings.rounded) RoundedCornerShape(22.dp) else RectangleShape
    val cur by rememberUpdatedState(w)
    val pages by rememberUpdatedState(pageCount)
    val size = w.size

    val wPx = geo.span(w.w)
    val hPx = geo.span(w.h)
    // Content is authored against an 88dp cell; scale it to the real cell so layouts stay pixel-close.
    val designW = (w.w * 88 + (w.w - 1) * 8) * density.density
    val scale = (wPx / designW).coerceIn(0.55f, 1.6f)

    Box(
        Modifier
            .offset { IntOffset(geo.x(w.col).roundToInt(), geo.y(w.row).roundToInt()) }
            .size(with(density) { wPx.toDp() }, with(density) { hPx.toDp() })
            .clip(shape)
            .background(Paper)
            .border(2.dp, Ink, shape)
            .padding(2.dp)
            .tap(!editing) { onWidgetTap(vm, ctx, actions, w) }
    ) {
        CompositionLocalProvider(LocalDensity provides Density(density.density * scale, 1f)) {
            Box(Modifier.fillMaxSize()) { WidgetContent(vm, w, size, editing, live) }
        }

        if (editing) {
            Box(
                Modifier.fillMaxSize()
                    .background(Paper.copy(alpha = .25f))
                    .border(2.dp, accent)
                    .pointerInput(w.id) {
                        detectTapGestures(onTap = {
                            when (cur.type) {
                                WType.APPS -> actions.pickApps(cur.id)
                                WType.PHOTO, WType.HERO -> actions.pickPhoto(cur.id)
                                WType.ASK -> vm.updateWidget(key, cur.id) { x ->
                                    x.copy(agent = when (x.agent) { "claude" -> "grok"; "grok" -> "codex"; else -> "claude" })
                                }
                                else -> {}
                            }
                        })
                    }
                    .pointerInput(w.id, geo) {
                        var orig: Widget? = null
                        var base = cur
                        var acc = androidx.compose.ui.geometry.Offset.Zero
                        var edgeSince = 0L
                        val edge = 28.dp.toPx()
                        detectDragGestures(
                            onDragStart = { orig = cur; base = cur; acc = androidx.compose.ui.geometry.Offset.Zero; edgeSince = 0L },
                            onDragEnd = { orig?.let { vm.commit(key, cur.id, it) }; orig = null },
                            onDragCancel = { orig?.let { vm.commit(key, cur.id, it) }; orig = null },
                        ) { change, amount ->
                            change.consume()
                            acc += amount
                            val dc = (acc.x / geo.step).roundToInt()
                            val dr = (acc.y / geo.step).roundToInt()
                            val col = (base.col + dc).coerceIn(1, maxOf(1, geo.cols - base.w + 1))
                            val row = (base.row + dr).coerceIn(1, maxOf(1, geo.rows - base.h + 1))
                            if (col != cur.col || row != cur.row) vm.setLive(key, cur.copy(col = col, row = row))
                            // Hold at the screen edge to carry the widget to the neighbouring page.
                            val absX = geo.x(cur.col) + change.position.x
                            val dir = when {
                                absX < edge -> -1
                                absX > geo.width - edge -> 1
                                else -> 0
                            }
                            val target = cur.page + dir
                            if (dir == 0 || target < 0 || target >= pages) {
                                edgeSince = 0L
                            } else if (edgeSince == 0L) {
                                edgeSince = change.uptimeMillis
                            } else if (change.uptimeMillis - edgeSince > 600) {
                                val moved = cur.copy(page = target, col = if (dir > 0) 1 else maxOf(1, geo.cols - cur.w + 1))
                                vm.setLive(key, moved)
                                onPage(target)
                                base = moved
                                acc = androidx.compose.ui.geometry.Offset.Zero
                                edgeSince = 0L
                            }
                        }
                    }
            )
            Column(Modifier.padding(start = 6.dp, top = 4.dp)) {
                Label("${w.type.key.uppercase()} · $size", 9f, color = accent, tracking = .12f)
                Label("${w.w}×${w.h}", 9f, color = accent, tracking = .12f)
            }
            Box(
                Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                    .background(Paper).border(2.dp, Ink)
                    .tap { vm.remove(key, w.id) },
                contentAlignment = Alignment.Center,
            ) { Glyph("close", 16f) }
            Box(
                Modifier.align(Alignment.BottomEnd).size(26.dp).clip(Triangle).background(accent)
                    .pointerInput(w.id, geo) {
                        var orig: Widget? = null
                        var acc = androidx.compose.ui.geometry.Offset.Zero
                        detectDragGestures(
                            onDragStart = { orig = cur; acc = androidx.compose.ui.geometry.Offset.Zero },
                            onDragEnd = { orig?.let { vm.commit(key, cur.id, it) }; orig = null },
                            onDragCancel = { orig?.let { vm.commit(key, cur.id, it) }; orig = null },
                        ) { change, amount ->
                            change.consume()
                            acc += amount
                            val o = orig ?: return@detectDragGestures
                            val nw = (o.w + (acc.x / geo.step).roundToInt()).coerceIn(1, geo.cols - o.col + 1)
                            val nh = (o.h + (acc.y / geo.step).roundToInt()).coerceIn(1, maxOf(1, geo.rows - o.row + 1))
                            if (nw != cur.w || nh != cur.h) vm.setLive(key, cur.copy(w = nw, h = nh))
                        }
                    }
            )
        }
    }
}

/** Gesture actions selectable in Settings → Gestures. */
val GestureActions = listOf(
    "notifications" to "NOTIFICATIONS",
    "quick_settings" to "QUICK SETTINGS",
    "apps" to "ALL APPS",
    "search" to "SEARCH",
    "none" to "NOTHING",
)

fun runGesture(vm: LauncherVM, ctx: android.content.Context, action: String) {
    when (action) {
        "notifications" -> Shade.expand(ctx, quick = false)
        "quick_settings" -> Shade.expand(ctx, quick = true)
        "apps" -> vm.overlay = Overlay.APPS
        "search" -> vm.overlay = Overlay.SEARCH
        else -> {}
    }
}

private fun onWidgetTap(vm: LauncherVM, ctx: android.content.Context, actions: Actions, w: Widget) {
    when (w.type) {
        WType.CLOCK, WType.HERO -> Apps.openClock(ctx)
        WType.DATE -> Apps.openCalendar(ctx)
        WType.CALENDAR -> if (!Calendars.hasPermission(ctx)) actions.requestCalendar() else Apps.openCalendar(ctx)
        WType.WEATHER -> if (!Weather.hasPermission(ctx)) actions.requestLocation() else vm.refreshWeather()
        WType.MUSIC -> if (!Media.hasAccess(ctx)) Media.requestAccess(ctx) else Media.openPlayer(ctx)
        WType.BATTERY -> Apps.startAny(ctx, Intent(Intent.ACTION_POWER_USAGE_SUMMARY), Intent(AndroidSettings.ACTION_BATTERY_SAVER_SETTINGS), Intent(AndroidSettings.ACTION_SETTINGS))
        WType.STORAGE -> Apps.startAny(ctx, Intent(AndroidSettings.ACTION_INTERNAL_STORAGE_SETTINGS), Intent(AndroidSettings.ACTION_SETTINGS))
        WType.SEARCH -> vm.overlay = Overlay.SEARCH
        WType.CAR -> when {
            vm.settings.tessieToken.isBlank() -> vm.overlay = Overlay.SETTINGS
            vm.car.data == null -> vm.refreshCar()
            else -> ctx.packageManager.getLaunchIntentForPackage("com.tessie.app")?.let { Apps.start(ctx, it) } ?: vm.refreshCar()
        }
        WType.SPORTS -> Sports.openEspn(ctx, Sports.parse(vm.settings.sportsTeams).firstOrNull())
        WType.APPS, WType.PHOTO, WType.QUOTE, WType.ASK -> {}
    }
}

@Composable
private fun WidgetContent(vm: LauncherVM, w: Widget, size: Size, editing: Boolean, live: Live) {
    val ctx = LocalContext.current
    val actions = LocalActions.current
    when (w.type) {
        WType.HERO -> HeroWidget(vm, w, size)
        WType.CLOCK -> ClockWidget(vm, size)
        WType.DATE -> DateWidget(vm, size)
        WType.WEATHER -> WeatherWidget(vm, size)
        WType.MUSIC -> MusicWidget(vm, size)
        WType.PHOTO -> PhotoWidget(vm, w, size)
        WType.QUOTE -> QuoteWidget(vm, size)
        WType.BATTERY -> BatteryWidget(size, live.battery, live.signal)
        WType.STORAGE -> StorageWidget(size, live.storage)
        WType.CALENDAR -> CalendarWidget(vm, size, Calendars.hasPermission(ctx))
        WType.SEARCH -> SearchWidget(
            size,
            onLens = {
                ctx.packageManager.getLaunchIntentForPackage("com.google.ar.lens")?.let { Apps.start(ctx, it) }
                    ?: Apps.start(ctx, Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            },
            onMic = { actions.listen { q -> vm.searchQuery = q; vm.overlay = Overlay.SEARCH } },
        )
        WType.APPS -> AppsWidget(vm, w, size, editing) { actions.pickApps(w.id) }
        WType.CAR -> CarWidget(vm, size)
        WType.SPORTS -> SportsWidget(vm, size)
        WType.ASK -> AskWidget(vm, w, size, editing) { a -> vm.updateWidget(vm.displayKey, w.id) { it.copy(agent = a) } }
    }
}
