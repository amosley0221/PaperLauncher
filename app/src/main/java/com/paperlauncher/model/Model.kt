package com.paperlauncher.model

import org.json.JSONArray
import org.json.JSONObject

data class Span(val w: Int, val h: Int)

enum class Size { S, M, L }

enum class WType(val key: String, val title: String, val s: Span, val m: Span, val l: Span) {
    HERO("hero", "CLOCK + PHOTO (HERO)", Span(4, 4), Span(4, 6), Span(4, 8)),
    CLOCK("clock", "CLOCK", Span(2, 1), Span(2, 2), Span(4, 2)),
    DATE("date", "DATE / DAY", Span(2, 1), Span(1, 2), Span(2, 2)),
    WEATHER("weather", "WEATHER", Span(2, 1), Span(4, 1), Span(4, 3)),
    MUSIC("music", "MUSIC", Span(2, 1), Span(4, 1), Span(4, 2)),
    PHOTO("photo", "PHOTO", Span(4, 1), Span(4, 2), Span(8, 3)),
    QUOTE("quote", "QUOTE", Span(4, 1), Span(4, 2), Span(8, 3)),
    BATTERY("battery", "BATTERY & SIGNAL", Span(2, 1), Span(4, 1), Span(2, 2)),
    STORAGE("storage", "STORAGE & RAM", Span(2, 1), Span(4, 1), Span(4, 2)),
    CALENDAR("calendar", "CALENDAR", Span(2, 1), Span(4, 2), Span(4, 3)),
    SEARCH("search", "SEARCH", Span(2, 1), Span(4, 1), Span(8, 1)),
    APPS("apps", "APP SHORTCUTS", Span(1, 1), Span(2, 1), Span(2, 2)),
    CAR("car", "CAR · TESSIE", Span(2, 1), Span(4, 1), Span(4, 3)),
    SPORTS("sports", "SPORTS SCORES", Span(2, 1), Span(4, 1), Span(4, 3)),
    ASK("ask", "ASK AN AGENT", Span(2, 1), Span(4, 1), Span(4, 2));

    fun span(size: Size) = when (size) { Size.S -> s; Size.M -> m; Size.L -> l }

    fun sizeFor(w: Int, h: Int): Size = when {
        w >= l.w && h >= l.h -> Size.L
        w >= m.w && h >= m.h -> Size.M
        else -> Size.S
    }

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: CLOCK
    }
}

/** Groups for the Add Widget sheet. */
val WidgetGroups = listOf(
    "INFORMATION" to listOf(WType.BATTERY, WType.STORAGE, WType.WEATHER),
    "MEDIA" to listOf(WType.MUSIC, WType.PHOTO),
    "TEXT" to listOf(WType.QUOTE, WType.SEARCH),
    "TIME" to listOf(WType.CALENDAR, WType.CLOCK, WType.DATE, WType.HERO),
    "CONNECTED" to listOf(WType.CAR, WType.SPORTS, WType.ASK),
)

data class Widget(
    val id: String,
    val type: WType,
    val col: Int,
    val row: Int,
    val w: Int,
    val h: Int,
    val page: Int = 0,
    val agent: String = "claude",
    val apps: List<String> = emptyList(),
    val photo: String? = null,
) {
    val size: Size get() = type.sizeFor(w, h)

    fun hits(o: Widget) = page == o.page &&
        col < o.col + o.w && o.col < col + w && row < o.row + o.h && o.row < row + h

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("type", type.key).put("col", col).put("row", row)
        .put("w", w).put("h", h).put("page", page).put("agent", agent)
        .put("apps", JSONArray(apps)).apply { if (photo != null) put("photo", photo) }

    companion object {
        fun fromJson(o: JSONObject) = Widget(
            id = o.getString("id"),
            type = WType.of(o.getString("type")),
            col = o.getInt("col"), row = o.getInt("row"),
            w = o.getInt("w"), h = o.getInt("h"),
            page = o.optInt("page", 0),
            agent = o.optString("agent", "claude"),
            apps = o.optJSONArray("apps")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList(),
            photo = if (o.has("photo")) o.getString("photo") else null,
        )
    }
}

enum class DisplayKey(val cols: Int) { INNER_HERO(8), INNER_GRID(8), COVER(4), TAB_L(12), TAB_P(8) }

private var seq = 0
private fun W(type: WType, col: Int, row: Int, w: Int, h: Int, page: Int = 0, agent: String = "claude", apps: String = "") =
    Widget("d" + (++seq), type, col, row, w, h, page, agent, apps.split(',').map { it.trim() }.filter { it.isNotEmpty() })

private const val TWO = "phone,messages"
private const val FOUR = "phone,messages,photos,camera"

fun defaultLayout(key: DisplayKey): List<Widget> = when (key) {
    DisplayKey.INNER_HERO -> listOf(
        W(WType.HERO, 1, 1, 4, 9), W(WType.WEATHER, 5, 1, 4, 3), W(WType.BATTERY, 5, 4, 4, 1),
        W(WType.STORAGE, 5, 5, 4, 1), W(WType.CAR, 5, 6, 4, 1), W(WType.CALENDAR, 5, 7, 4, 2),
        W(WType.SPORTS, 5, 9, 2, 1), W(WType.APPS, 7, 9, 2, 1, apps = TWO),
        W(WType.HERO, 1, 1, 4, 9, 1), W(WType.ASK, 5, 1, 4, 2, 1),
        W(WType.ASK, 5, 3, 4, 1, 1, "claude"), W(WType.ASK, 5, 4, 4, 1, 1, "grok"), W(WType.ASK, 5, 5, 4, 1, 1, "codex"),
        W(WType.MUSIC, 5, 6, 4, 1, 1), W(WType.APPS, 5, 7, 4, 2, 1, apps = "browser,mail,maps,settings,calendar,music"),
    )
    DisplayKey.INNER_GRID -> listOf(
        W(WType.CLOCK, 1, 1, 4, 2), W(WType.DATE, 5, 1, 2, 2), W(WType.BATTERY, 7, 1, 2, 2),
        W(WType.PHOTO, 1, 3, 4, 3), W(WType.WEATHER, 5, 3, 4, 3), W(WType.MUSIC, 1, 6, 4, 1),
        W(WType.STORAGE, 5, 6, 4, 1), W(WType.CALENDAR, 1, 7, 4, 2), W(WType.SEARCH, 5, 7, 4, 1),
        W(WType.APPS, 5, 8, 2, 2, apps = FOUR), W(WType.APPS, 7, 8, 2, 2, apps = FOUR), W(WType.QUOTE, 1, 9, 4, 1),
    )
    DisplayKey.COVER -> listOf(
        W(WType.CLOCK, 1, 1, 2, 2), W(WType.DATE, 3, 1, 2, 2), W(WType.PHOTO, 1, 3, 4, 2),
        W(WType.WEATHER, 1, 5, 4, 1), W(WType.BATTERY, 1, 6, 4, 1), W(WType.STORAGE, 1, 7, 4, 1),
        W(WType.MUSIC, 1, 8, 4, 1), W(WType.SEARCH, 1, 9, 4, 1),
        W(WType.APPS, 1, 10, 2, 1, apps = FOUR), W(WType.APPS, 3, 10, 2, 1, apps = FOUR),
    )
    DisplayKey.TAB_L -> listOf(
        W(WType.CLOCK, 1, 1, 4, 2), W(WType.DATE, 5, 1, 2, 2), W(WType.PHOTO, 7, 1, 6, 3),
        W(WType.WEATHER, 1, 3, 4, 3), W(WType.BATTERY, 5, 3, 2, 2), W(WType.STORAGE, 7, 4, 4, 1),
        W(WType.APPS, 11, 4, 2, 1, apps = TWO), W(WType.MUSIC, 5, 5, 4, 1), W(WType.CALENDAR, 9, 5, 4, 3),
        W(WType.SEARCH, 1, 6, 8, 1), W(WType.APPS, 1, 7, 2, 1, apps = FOUR), W(WType.APPS, 3, 7, 2, 1, apps = FOUR),
        W(WType.QUOTE, 5, 7, 4, 1),
    )
    DisplayKey.TAB_P -> listOf(
        W(WType.CLOCK, 1, 1, 4, 2), W(WType.DATE, 5, 1, 2, 2), W(WType.BATTERY, 7, 1, 2, 2),
        W(WType.PHOTO, 1, 3, 8, 3), W(WType.WEATHER, 1, 6, 4, 3), W(WType.CALENDAR, 5, 6, 4, 3),
        W(WType.MUSIC, 1, 9, 4, 1), W(WType.STORAGE, 5, 9, 4, 1), W(WType.SEARCH, 1, 10, 8, 1),
        W(WType.APPS, 1, 11, 2, 2, apps = FOUR), W(WType.APPS, 3, 11, 2, 2, apps = FOUR),
        W(WType.APPS, 5, 11, 2, 2, apps = FOUR), W(WType.CALENDAR, 7, 11, 2, 1), W(WType.DATE, 7, 12, 2, 1),
    )
}.map { it.copy(id = key.name + "-" + it.id) }

data class Settings(
    val accent: Long = 0xFF7A1F2E,
    val rounded: Boolean = false,
    val innerHero: Boolean = true,
    val fahrenheit: Boolean = false,
    val clock24: Boolean = true,
    val weekMonday: Boolean = true,
    val hideStatusBar: Boolean = true,
    val iconSize: Int = 1, // 0 S, 1 M, 2 L
    val showAppNames: Boolean = false,
    val tessieToken: String = "",
    val sportsTeams: String = "NBA:GSW",
    val calendarIds: Set<Long> = emptySet(),
    val heroWeek: Boolean = true,
    val mantra1: String = "KEEP IT SIMPLE",
    val mantra2: String = "GO OUTSIDE TODAY",
    val mantra3: String = "PLANT SOMETHING TODAY",
    val caption: String = "CLIMB PAUSE LISTEN REPEAT",
    val quote: String = "FEWER SCREENS MORE STARS",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("accent", accent).put("rounded", rounded).put("innerHero", innerHero)
        .put("fahrenheit", fahrenheit).put("clock24", clock24).put("weekMonday", weekMonday)
        .put("hideStatusBar", hideStatusBar).put("iconSize", iconSize).put("showAppNames", showAppNames)
        .put("tessieToken", tessieToken).put("sportsTeams", sportsTeams)
        .put("calendarIds", JSONArray(calendarIds.toList())).put("heroWeek", heroWeek)
        .put("mantra1", mantra1).put("mantra2", mantra2).put("mantra3", mantra3)
        .put("caption", caption).put("quote", quote)

    companion object {
        fun fromJson(o: JSONObject): Settings {
            val d = Settings()
            return Settings(
                accent = o.optLong("accent", d.accent),
                rounded = o.optBoolean("rounded", d.rounded),
                innerHero = o.optBoolean("innerHero", d.innerHero),
                fahrenheit = o.optBoolean("fahrenheit", d.fahrenheit),
                clock24 = o.optBoolean("clock24", d.clock24),
                weekMonday = o.optBoolean("weekMonday", d.weekMonday),
                hideStatusBar = o.optBoolean("hideStatusBar", d.hideStatusBar),
                iconSize = o.optInt("iconSize", d.iconSize),
                showAppNames = o.optBoolean("showAppNames", d.showAppNames),
                tessieToken = o.optString("tessieToken", d.tessieToken),
                sportsTeams = o.optString("sportsTeams", d.sportsTeams),
                calendarIds = o.optJSONArray("calendarIds")?.let { a -> (0 until a.length()).map { a.getLong(it) }.toSet() } ?: emptySet(),
                heroWeek = o.optBoolean("heroWeek", d.heroWeek),
                mantra1 = o.optString("mantra1", d.mantra1),
                mantra2 = o.optString("mantra2", d.mantra2),
                mantra3 = o.optString("mantra3", d.mantra3),
                caption = o.optString("caption", d.caption),
                quote = o.optString("quote", d.quote),
            )
        }
    }
}

fun layoutsToJson(layouts: Map<DisplayKey, List<Widget>>): JSONObject = JSONObject().apply {
    layouts.forEach { (k, list) -> put(k.name, JSONArray(list.map { it.toJson() })) }
}

fun layoutsFromJson(o: JSONObject?): Map<DisplayKey, List<Widget>> = DisplayKey.entries.associateWith { k ->
    val arr = o?.optJSONArray(k.name)
    if (arr == null) defaultLayout(k) else List(arr.length()) { Widget.fromJson(arr.getJSONObject(it)) }
}
