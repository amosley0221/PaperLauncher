package com.paperlauncher.data

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
)

data class Fixture(val text: String, val time: String)

data class SportsData(val game: Game?, val next: List<Fixture>, val team: String, val updatedAt: Long) {
    val live get() = game?.state == "in"
}

object Sports {
    private val Leagues = mapOf(
        "NBA" to "basketball/nba", "WNBA" to "basketball/wnba", "NFL" to "football/nfl",
        "MLB" to "baseball/mlb", "NHL" to "hockey/nhl", "MLS" to "soccer/usa.1", "EPL" to "soccer/eng.1",
        "LALIGA" to "soccer/esp.1", "NCAAF" to "football/college-football", "NCAAB" to "basketball/mens-college-basketball",
    )

    /** "NBA:GSW, NFL:SF" → first entry. */
    fun parse(teams: String): Pair<String, String>? {
        val first = teams.split(',').map { it.trim() }.firstOrNull { it.contains(':') } ?: return null
        val (l, t) = first.split(':', limit = 2).map { it.trim().uppercase() }
        return if (Leagues.containsKey(l) && t.isNotEmpty()) l to t else null
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

    private fun game(league: String, team: String, e: JSONObject): Game? {
        val comp = e.optJSONArray("competitions")?.optJSONObject(0) ?: return null
        val cs = comp.optJSONArray("competitors") ?: return null
        val sides = List(cs.length()) { cs.getJSONObject(it) }
        val mineJ = sides.firstOrNull { it.optJSONObject("team")?.optString("abbreviation").equals(team, true) } ?: return null
        val otherJ = sides.firstOrNull { it !== mineJ } ?: return null
        val status = (comp.optJSONObject("status") ?: e.optJSONObject("status"))?.optJSONObject("type")
        val state = status?.optString("state") ?: "pre"
        val detail = (status?.optString("shortDetail") ?: "").uppercase().replace(" - ", " · ")
        val tv = comp.optJSONArray("broadcasts")?.optJSONObject(0)?.let { b ->
            b.optJSONArray("names")?.optString(0) ?: b.optJSONObject("media")?.optString("shortName")
        } ?: ""
        return Game(
            league, state, detail, side(mineJ), side(otherJ),
            comp.optJSONObject("venue")?.optString("fullName")?.uppercase() ?: "", tv.uppercase(),
        )
    }

    suspend fun fetch(teams: String): SportsData? {
        val (league, team) = parse(teams) ?: return null
        val path = Leagues.getValue(league)
        val base = "https://site.api.espn.com/apis/site/v2/sports/$path"
        val board = runCatching { Http.json("$base/scoreboard") }.getOrNull()
        var g: Game? = null
        board?.optJSONArray("events")?.let { ev ->
            for (i in 0 until ev.length()) {
                g = game(league, team, ev.getJSONObject(i)) ?: continue
                break
            }
        }
        val sched = runCatching { Http.json("$base/teams/${team.lowercase()}/schedule") }.getOrNull()
        val next = mutableListOf<Fixture>()
        var lastPlayed: Game? = null
        val zone = ZoneId.systemDefault()
        val dayFmt = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        sched?.optJSONArray("events")?.let { ev ->
            for (i in 0 until ev.length()) {
                val e = ev.getJSONObject(i)
                val gm = game(league, team, e) ?: continue
                if (gm.state == "post") lastPlayed = gm
                if (gm.state == "pre" && next.size < 2) {
                    val home = e.optJSONArray("competitions")?.optJSONObject(0)?.optJSONArray("competitors")?.let { cs ->
                        (0 until cs.length()).map { cs.getJSONObject(it) }.firstOrNull { c ->
                            c.optJSONObject("team")?.optString("abbreviation").equals(team, true)
                        }?.optString("homeAway") == "home"
                    } ?: true
                    val t = runCatching { Instant.parse(e.optString("date").let { if (it.length == 17) it.dropLast(1) + ":00Z" else it }).atZone(zone) }.getOrNull()
                    val day = t?.format(dayFmt)?.uppercase() ?: ""
                    next += Fixture("$day · ${if (home) "VS" else "@"} ${gm.other.abbr}", t?.format(timeFmt) ?: "")
                }
            }
        }
        return SportsData(g ?: lastPlayed, next, team, System.currentTimeMillis())
    }
}
