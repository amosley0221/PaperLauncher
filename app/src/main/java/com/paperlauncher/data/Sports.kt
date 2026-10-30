package com.paperlauncher.data

import android.content.Context
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Side(val abbr: String, val name: String, val score: String, val record: String, val lines: List<String>)

data class Game(
    val league: String,
    val state: String,          // pre / in / post
    val detail: String,         // "Q4 · 2:14", "FINAL", "SAT 19:30"
    val mine: Side,
    val other: Side,
    val venue: String,
    val tv: String,
    val start: Long,
)

data class Fixture(val text: String, val time: String, val at: Long)

data class SportsData(val game: Game?, val next: List<Fixture>, val updatedAt: Long) {
    val live get() = game?.state == "in"
}

/** A followed team. Stored as "LEAGUE:ABBR:ID:NAME"; [id] is ESPN's team id. */
data class TeamRef(val league: String, val abbr: String, val id: String, val name: String) {
    fun encode() = "$league:$abbr:$id:$name"
}

data class League(val key: String, val path: String, val espnWeb: String)

object Sports {
    val Leagues = listOf(
        League("NBA", "basketball/nba", "nba"),
        League("NFL", "football/nfl", "nfl"),
        League("MLB", "baseball/mlb", "mlb"),
        League("NHL", "hockey/nhl", "nhl"),
        League("WNBA", "basketball/wnba", "wnba"),
        League("MLS", "soccer/usa.1", "soccer"),
        League("EPL", "soccer/eng.1", "soccer"),
        League("LALIGA", "soccer/esp.1", "soccer"),
        League("NCAAF", "football/college-football", "college-football"),
        League("NCAAB", "basketball/mens-college-basketball", "mens-college-basketball"),
    )

    private fun league(key: String) = Leagues.firstOrNull { it.key == key }
    private fun base(l: League) = "https://site.api.espn.com/apis/site/v2/sports/${l.path}"

    /** Parses "NBA:GS:9:WARRIORS, NFL:SF:25:49ERS" (older "NBA:GSW" entries still load). */
    fun parse(s: String): List<TeamRef> = s.split(',').mapNotNull { e ->
        val p = e.trim().split(':')
        if (p.size < 2 || league(p[0].uppercase()) == null || p[1].isBlank()) null
        else TeamRef(p[0].uppercase(), p[1].uppercase(), p.getOrElse(2) { "" }, p.getOrElse(3) { p[1] }.uppercase())
    }

    fun encode(teams: List<TeamRef>) = teams.joinToString(",") { it.encode() }

    /** All teams in a league, for the picker. */
    suspend fun teams(leagueKey: String): List<TeamRef> {
        val l = league(leagueKey) ?: return emptyList()
        val j = Http.json("${base(l)}/teams?limit=1000")
        val arr = j.optJSONArray("sports")?.optJSONObject(0)?.optJSONArray("leagues")?.optJSONObject(0)?.optJSONArray("teams")
            ?: return emptyList()
        return List(arr.length()) { arr.getJSONObject(it).optJSONObject("team") ?: JSONObject() }
            .filter { it.optString("id").isNotEmpty() }
            .map {
                TeamRef(
                    l.key, it.optString("abbreviation").uppercase(), it.optString("id"),
                    it.optString("displayName").ifEmpty { it.optString("name") }.uppercase(),
                )
            }
            .sortedBy { it.name }
    }

    fun openEspn(ctx: Context, team: TeamRef?) {
        ctx.packageManager.getLaunchIntentForPackage("com.espn.score_center")?.let { if (Apps.start(ctx, it)) return }
        val l = team?.let { league(it.league) }
        val url = when {
            team == null || l == null -> "https://www.espn.com/"
            team.id.isNotEmpty() && l.espnWeb != "soccer" -> "https://www.espn.com/${l.espnWeb}/team/_/id/${team.id}"
            else -> "https://www.espn.com/${l.espnWeb}/"
        }
        Apps.web(ctx, url)
    }

    private fun isMine(c: JSONObject, t: TeamRef): Boolean {
        val team = c.optJSONObject("team") ?: return false
        if (t.id.isNotEmpty()) return team.optString("id") == t.id || c.optString("id") == t.id
        val abbr = team.optString("abbreviation")
        return abbr.equals(t.abbr, true) || (abbr.length >= 2 && t.abbr.startsWith(abbr, true))
    }

    private fun side(c: JSONObject): Side {
        val team = c.optJSONObject("team") ?: JSONObject()
        val score = c.optJSONObject("score")?.optString("displayValue") ?: c.optString("score", "")
        val rec = c.optJSONArray("records")?.optJSONObject(0)?.optString("summary")
            ?: c.optJSONArray("record")?.optJSONObject(0)?.optString("displayValue") ?: ""
        val ls = c.optJSONArray("linescores")
        val lines = if (ls == null) emptyList() else List(ls.length()) {
            val o = ls.optJSONObject(it)
            o?.optString("displayValue")?.takeIf { s -> s.isNotEmpty() } ?: o?.optDouble("value")?.toInt()?.toString() ?: ""
        }
        return Side(
            team.optString("abbreviation").uppercase(),
            (team.optString("shortDisplayName").ifEmpty { team.optString("name") }).uppercase(),
            score, rec.replace("-", " – "), lines,
        )
    }

    private fun instant(s: String): Instant? = runCatching {
        Instant.parse(if (s.length == 17 && s.endsWith("Z")) s.dropLast(1) + ":00Z" else s)
    }.getOrNull()

    private fun game(t: TeamRef, e: JSONObject): Game? {
        val comp = e.optJSONArray("competitions")?.optJSONObject(0) ?: return null
        val cs = comp.optJSONArray("competitors") ?: return null
        val sides = List(cs.length()) { cs.getJSONObject(it) }
        val mineJ = sides.firstOrNull { isMine(it, t) } ?: return null
        val otherJ = sides.firstOrNull { it !== mineJ } ?: return null
        val status = (comp.optJSONObject("status") ?: e.optJSONObject("status"))?.optJSONObject("type")
        val state = status?.optString("state")?.ifEmpty { null } ?: "pre"
        val detail = (status?.optString("shortDetail") ?: "").uppercase().replace(" - ", " · ")
        val tv = comp.optJSONArray("broadcasts")?.optJSONObject(0)?.let { b ->
            b.optJSONArray("names")?.optString(0) ?: b.optJSONObject("media")?.optString("shortName")
        } ?: ""
        return Game(
            t.league, state, detail, side(mineJ), side(otherJ),
            comp.optJSONObject("venue")?.optString("fullName")?.uppercase() ?: "", tv.uppercase(),
            instant(e.optString("date"))?.toEpochMilli() ?: 0L,
        )
    }

    private class TeamResult(val today: Game?, val last: Game?, val next: List<Fixture>)

    private suspend fun fetchTeam(ref: TeamRef, multi: Boolean): TeamResult {
        val l = league(ref.league) ?: return TeamResult(null, null, emptyList())
        // Older settings stored only an abbreviation (e.g. GSW); resolve it to ESPN's id (GS = 9).
        val t = if (ref.id.isNotEmpty()) ref else runCatching { teams(ref.league) }.getOrDefault(emptyList()).firstOrNull {
            it.abbr == ref.abbr || ref.abbr.startsWith(it.abbr)
        } ?: ref
        val base = base(l)
        var today: Game? = null
        runCatching { Http.json("$base/scoreboard") }.getOrNull()?.optJSONArray("events")?.let { ev ->
            for (i in 0 until ev.length()) {
                val g = game(t, ev.getJSONObject(i))
                if (g != null) { today = g; break }
            }
        }
        val teamKey = t.id.ifEmpty { t.abbr.lowercase() }
        var last: Game? = null
        val next = mutableListOf<Fixture>()
        val zone = ZoneId.systemDefault()
        val dayFmt = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        val now = System.currentTimeMillis()
        runCatching { Http.json("$base/teams/$teamKey/schedule") }.getOrNull()?.optJSONArray("events")?.let { ev ->
            for (i in 0 until ev.length()) {
                val e = ev.getJSONObject(i)
                val g = game(t, e) ?: continue
                if (g.state == "post" && (last?.start ?: 0L) <= g.start) last = g
                if (g.state == "pre" && g.start >= now - 3 * 3_600_000L) {
                    val home = e.optJSONArray("competitions")?.optJSONObject(0)?.optJSONArray("competitors")?.let { cs ->
                        (0 until cs.length()).map { cs.getJSONObject(it) }.firstOrNull { isMine(it, t) }?.optString("homeAway") == "home"
                    } ?: true
                    val at = Instant.ofEpochMilli(g.start).atZone(zone)
                    val who = if (multi) "${g.mine.abbr} " else ""
                    next += Fixture("${at.format(dayFmt).uppercase()} · $who${if (home) "VS" else "@"} ${g.other.abbr}", at.format(timeFmt), g.start)
                }
            }
        }
        return TeamResult(today, last, next.sortedBy { it.at }.take(3))
    }

    suspend fun fetch(teams: String): SportsData? {
        val list = parse(teams).take(4)
        if (list.isEmpty()) return null
        val results = list.map { fetchTeam(it, list.size > 1) }
        val todays = results.mapNotNull { it.today }
        val game = todays.firstOrNull { it.state == "in" }
            ?: todays.firstOrNull { it.state == "pre" }
            ?: todays.firstOrNull()
            ?: results.mapNotNull { it.last }.maxByOrNull { it.start }
        val next = results.flatMap { it.next }.sortedBy { it.at }.take(2)
        return SportsData(game, next, System.currentTimeMillis())
    }
}
