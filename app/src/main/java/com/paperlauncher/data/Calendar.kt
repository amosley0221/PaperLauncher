package com.paperlauncher.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CalendarInfo(val id: Long, val name: String, val account: String)

data class CalEvent(val title: String, val start: Long, val allDay: Boolean)

data class CalendarData(val eventDays: Set<Int>, val upcoming: List<CalEvent>)

object Calendars {
    fun hasPermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun list(ctx: Context): List<CalendarInfo> {
        if (!hasPermission(ctx)) return emptyList()
        val out = mutableListOf<CalendarInfo>()
        runCatching {
            ctx.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME),
                "${CalendarContract.Calendars.VISIBLE} = 1", null, null,
            )?.use { c ->
                while (c.moveToNext()) out += CalendarInfo(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "")
            }
        }
        return out
    }

    /** Events for the month containing [today] plus the next few upcoming. */
    fun load(ctx: Context, ids: Set<Long>, today: LocalDate): CalendarData? {
        if (!hasPermission(ctx)) return null
        val zone = ZoneId.systemDefault()
        val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val horizon = maxOf(monthEnd, now + 14L * 86_400_000)
        val b = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(b, monthStart)
        ContentUris.appendId(b, horizon)
        val sel = if (ids.isEmpty()) null else "${CalendarContract.Instances.CALENDAR_ID} IN (${ids.joinToString(",")})"
        val days = mutableSetOf<Int>()
        val upcoming = mutableListOf<CalEvent>()
        runCatching {
            ctx.contentResolver.query(
                b.build(),
                arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY),
                sel, null, "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val begin = c.getLong(1)
                    val end = c.getLong(2)
                    val allDay = c.getInt(3) == 1
                    val title = (c.getString(0) ?: "").uppercase()
                    if (begin < monthEnd) {
                        val d = if (allDay) Instant.ofEpochMilli(begin).atZone(ZoneId.of("UTC")).toLocalDate()
                        else Instant.ofEpochMilli(begin).atZone(zone).toLocalDate()
                        if (d.month == today.month && d.year == today.year) days += d.dayOfMonth
                    }
                    if (end > now && upcoming.size < 3) upcoming += CalEvent(title, begin, allDay)
                }
            }
        }
        return CalendarData(days, upcoming)
    }
}
