package com.paperlauncher

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paperlauncher.data.CalendarData
import com.paperlauncher.data.Calendars
import com.paperlauncher.data.CarData
import com.paperlauncher.data.HttpException
import com.paperlauncher.data.Media
import com.paperlauncher.data.Sports
import com.paperlauncher.data.SportsData
import com.paperlauncher.data.Tessie
import com.paperlauncher.data.Weather
import com.paperlauncher.data.WeatherData
import com.paperlauncher.data.toast
import com.paperlauncher.model.DisplayKey
import com.paperlauncher.model.Settings
import com.paperlauncher.model.Size
import com.paperlauncher.model.WType
import com.paperlauncher.model.Widget
import com.paperlauncher.model.defaultLayout
import com.paperlauncher.model.layoutsFromJson
import com.paperlauncher.model.layoutsToJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

enum class Overlay { NONE, ADD, SETTINGS, APPS, SEARCH }

/** Remote data slot: loading until first result, then data or an error. */
data class Remote<T>(val data: T? = null, val error: String? = null, val loading: Boolean = false, val at: Long = 0L)

class LauncherVM(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("paper", Context.MODE_PRIVATE)
    private val ctx: Context get() = getApplication()

    var layouts by mutableStateOf(layoutsFromJson(prefs.getString("layouts", null)?.let { runCatching { JSONObject(it) }.getOrNull() }))
        private set
    var settings by mutableStateOf(prefs.getString("settings", null)?.let { runCatching { Settings.fromJson(JSONObject(it)) }.getOrNull() } ?: Settings())
        private set

    var editing by mutableStateOf(false)
    var overlay by mutableStateOf(Overlay.NONE)
    var accentDialog by mutableStateOf(false)
    var appPickerFor by mutableStateOf<String?>(null)
    var searchQuery by mutableStateOf("")
    var gridRows by mutableStateOf(9)
    val pages = mutableStateMapOf<DisplayKey, Int>()
    var displayKey by mutableStateOf(DisplayKey.INNER_HERO)

    var now by mutableStateOf(LocalDateTime.now())
        private set
    var weather by mutableStateOf(Remote<WeatherData>())
        private set
    var car by mutableStateOf(Remote<CarData>())
        private set
    var carImage by mutableStateOf<Bitmap?>(null)
        private set
    var sports by mutableStateOf(Remote<SportsData>())
        private set
    var calendar by mutableStateOf<CalendarData?>(null)
        private set
    var recentAsk by mutableStateOf(prefs.getString("recentAsk", "") ?: "")
        private set

    private val photos = LruCache<String, Bitmap>(8)
    private var loops: Job? = null

    // ---------- lifecycle ----------

    fun resume() {
        Media.start(ctx)
        loops?.cancel()
        loops = viewModelScope.launch {
            launch { while (true) { now = LocalDateTime.now(); delay(60_000L - System.currentTimeMillis() % 60_000L + 50) } }
            launch { while (true) { refreshCalendar(); delay(5 * 60_000L) } }
            launch { while (true) { if (uses(WType.WEATHER)) refreshWeather(); delay(30 * 60_000L) } }
            launch { while (true) { if (uses(WType.CAR)) refreshCar(); delay(4 * 60_000L) } }
            launch {
                while (true) {
                    if (uses(WType.SPORTS)) refreshSports()
                    delay(if (sports.data?.live == true) 60_000L else 30 * 60_000L)
                }
            }
        }
    }

    fun pause() {
        loops?.cancel()
        Media.stop()
        if (editing) editing = false
    }

    fun onHome() {
        editing = false
        overlay = Overlay.NONE
        accentDialog = false
        appPickerFor = null
        pages[displayKey] = 0
    }

    private fun uses(t: WType) = layouts.values.any { l -> l.any { it.type == t } }

    // ---------- remote data ----------

    fun refreshWeather() = viewModelScope.launch {
        if (!Weather.hasPermission(ctx)) { weather = Remote(error = "TAP TO ALLOW LOCATION"); return@launch }
        weather = weather.copy(loading = true)
        weather = runCatching { Weather.fetch(ctx, settings.fahrenheit) }.fold(
            { if (it == null) Remote(error = "NO LOCATION · TAP TO RETRY") else Remote(it, at = System.currentTimeMillis()) },
            { Remote(weather.data, "NO DATA · TAP TO RETRY") },
        )
    }

    fun refreshCar() = viewModelScope.launch {
        val token = settings.tessieToken
        if (token.isBlank()) { car = Remote(error = "ADD TESSIE TOKEN IN SETTINGS"); return@launch }
        car = car.copy(loading = true)
        car = runCatching { Tessie.fetch(token) }.fold(
            { if (it == null) Remote(error = "NO VEHICLE FOUND") else Remote(it, at = System.currentTimeMillis()) },
            { Remote(car.data, if (it is HttpException && it.code == 401) "TESSIE TOKEN REJECTED" else "NO DATA · TAP TO RETRY") },
        )
        if (carImage == null) carImage = runCatching { Tessie.render() }.getOrNull()
    }

    fun carCommand(command: String) = viewModelScope.launch {
        val c = car.data ?: return@launch
        val ok = Tessie.command(settings.tessieToken, c.vin, command)
        toast(ctx, if (ok) "SENT · ${command.replace('_', ' ').uppercase()}" else "COMMAND FAILED")
        if (ok) { delay(4000); refreshCar() }
    }

    fun refreshSports() = viewModelScope.launch {
        if (Sports.parse(settings.sportsTeams).isEmpty()) { sports = Remote(error = "PICK A TEAM IN SETTINGS"); return@launch }
        sports = sports.copy(loading = true)
        sports = runCatching { Sports.fetch(settings.sportsTeams) }.fold(
            { if (it == null) Remote(error = "NO DATA · TAP TO RETRY") else Remote(it, at = System.currentTimeMillis()) },
            { Remote(sports.data, "NO DATA · TAP TO RETRY") },
        )
    }

    fun refreshCalendar() = viewModelScope.launch {
        calendar = withContext(Dispatchers.IO) { Calendars.load(ctx, settings.calendarIds, LocalDate.now()) }
    }

    fun rememberAsk(text: String) {
        if (text.isBlank()) return
        recentAsk = text.trim()
        prefs.edit().putString("recentAsk", recentAsk).apply()
    }

    // ---------- photos ----------

    fun cachedPhoto(uri: String?): Bitmap? = uri?.let { photos.get(it) }

    suspend fun loadPhoto(uri: String, maxPx: Int): Bitmap? {
        photos.get(uri)?.let { return it }
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                val u = Uri.parse(uri)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, opts) }
            }.getOrNull()
        }
        if (bmp != null) photos.put(uri, bmp)
        return bmp
    }

    // ---------- layout ----------

    fun pageOf(key: DisplayKey) = pages[key] ?: 0

    fun pageCount(key: DisplayKey): Int =
        maxOf(1, (layouts[key].orEmpty().maxOfOrNull { it.page } ?: 0) + 1) + if (editing) 1 else 0

    fun setPage(key: DisplayKey, p: Int) {
        pages[key] = p.coerceIn(0, pageCount(key) - 1)
    }

    private fun saveLayouts() = prefs.edit().putString("layouts", layoutsToJson(layouts).toString()).apply()

    fun updateLayout(key: DisplayKey, persist: Boolean = true, fn: (List<Widget>) -> List<Widget>) {
        layouts = layouts.toMutableMap().also { it[key] = fn(it[key].orEmpty()) }
        if (persist) saveLayouts()
    }

    /** Live update during a drag/resize; not persisted. */
    fun setLive(key: DisplayKey, w: Widget) = updateLayout(key, persist = false) { l -> l.map { if (it.id == w.id) w else it } }

    /** End of drag/resize: revert to [orig] if the result overlaps another widget. */
    fun commit(key: DisplayKey, id: String, orig: Widget) {
        val lay = layouts[key].orEmpty()
        val me = lay.firstOrNull { it.id == id } ?: return
        val clash = lay.any { it.id != id && it.hits(me) }
        updateLayout(key) { l -> if (clash) l.map { if (it.id == id) orig else it } else l }
        if (clash) toast(ctx, "SPACE TAKEN · MOVED BACK")
    }

    fun remove(key: DisplayKey, id: String) = updateLayout(key) { l -> l.filterNot { it.id == id } }

    fun updateWidget(key: DisplayKey, id: String, fn: (Widget) -> Widget) =
        updateLayout(key) { l -> l.map { if (it.id == id) fn(it) else it } }

    /** Place at the first free slot (row-major) on the current page, then later pages. */
    fun add(key: DisplayKey, type: WType, size: Size, rows: Int) {
        val span = type.span(size)
        val cols = key.cols
        val w = minOf(span.w, cols)
        val lay = layouts[key].orEmpty()
        val start = pageOf(key)
        var placed: Widget? = null
        var page = start
        val id = "w" + System.currentTimeMillis().toString(36)
        while (placed == null && page < start + 20) {
            loop@ for (r in 1..maxOf(1, rows - span.h + 1)) for (c in 1..(cols - w + 1)) {
                val cand = Widget(id, type, c, r, w, span.h, page, apps = if (type == WType.APPS) defaultApps(size) else emptyList())
                if (lay.none { it.hits(cand) }) { placed = cand; break@loop }
            }
            if (placed == null) page++
        }
        val p = placed ?: return
        updateLayout(key) { it + p }
        pages[key] = p.page
        overlay = Overlay.NONE
    }

    private fun defaultApps(size: Size) = when (size) {
        Size.S -> listOf("phone")
        Size.M -> listOf("phone", "messages")
        Size.L -> listOf("phone", "messages", "photos", "camera")
    }

    fun resetLayout(key: DisplayKey) = updateLayout(key) { defaultLayout(key) }

    // ---------- settings ----------

    fun updateSettings(fn: (Settings) -> Settings) {
        val old = settings
        settings = fn(settings)
        prefs.edit().putString("settings", settings.toJson().toString()).apply()
        if (old.fahrenheit != settings.fahrenheit) refreshWeather()
        if (old.tessieToken != settings.tessieToken) refreshCar()
        if (old.sportsTeams != settings.sportsTeams) refreshSports()
        if (old.calendarIds != settings.calendarIds) refreshCalendar()
    }

    fun backupJson(): String = JSONObject()
        .put("version", 1)
        .put("settings", settings.toJson())
        .put("layouts", layoutsToJson(layouts))
        .toString(2)

    fun restoreJson(text: String): Boolean = runCatching {
        val o = JSONObject(text)
        val s = Settings.fromJson(o.getJSONObject("settings"))
        val l = layoutsFromJson(o.getJSONObject("layouts"))
        settings = s
        layouts = l
        prefs.edit().putString("settings", s.toJson().toString()).apply()
        saveLayouts()
        resume()
        true
    }.getOrDefault(false)
}
