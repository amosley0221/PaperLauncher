package com.paperlauncher.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import android.widget.Toast

data class AppInfo(val label: String, val component: ComponentName, val user: UserHandle) {
    val key: String get() = component.flattenToString()
}

/** Built-in shortcut slots that resolve to whatever app handles the intent. */
data class Builtin(val key: String, val label: String, val glyph: String)

val Builtins = listOf(
    Builtin("phone", "PHONE", "call"),
    Builtin("messages", "MESSAGES", "chat"),
    Builtin("photos", "PHOTOS", "local_florist"),
    Builtin("camera", "CAMERA", "photo_camera"),
    Builtin("browser", "BROWSER", "public"),
    Builtin("settings", "SETTINGS", "settings"),
    Builtin("maps", "MAPS", "map"),
    Builtin("mail", "MAIL", "mail"),
    Builtin("music", "MUSIC", "music_note"),
    Builtin("calendar", "CALENDAR", "calendar_month"),
)

/** Mono glyphs for well-known packages (the "icon pack"); others fall back to a tinted app icon. */
private val PackageGlyphs = listOf(
    "dialer" to "call", "phone" to "call", "messag" to "chat", "mms" to "chat", "whatsapp" to "forum",
    "telegram" to "send", "camera" to "photo_camera", "photos" to "local_florist", "gallery" to "local_florist",
    "chrome" to "public", "browser" to "public", "firefox" to "public", "android.gm" to "mail", "mail" to "mail",
    "outlook" to "mail", "maps" to "map", "calendar" to "calendar_month", "music" to "music_note",
    "spotify" to "music_note", "youtube" to "smart_display", "settings" to "settings", "clock" to "schedule",
    "deskclock" to "schedule", "calculator" to "calculate", "contacts" to "contacts", "files" to "folder",
    "myfiles" to "folder", "documents" to "folder", "notes" to "sticky_note_2", "keep" to "sticky_note_2",
    "claude" to "auto_awesome", "grok" to "bolt", "chatgpt" to "terminal", "translate" to "translate",
    "vending" to "shopping_bag", "weather" to "partly_cloudy_day",
    "tessie" to "electric_car", "tesla" to "electric_car", "bank" to "account_balance", "wallet" to "wallet",
    "podcast" to "podcasts", "netflix" to "movie", "duolingo" to "school", "authy" to "key",
)

object Apps {
    @Volatile private var cache: List<AppInfo>? = null

    fun list(ctx: Context, refresh: Boolean = false): List<AppInfo> {
        if (!refresh) cache?.let { return it }
        val la = ctx.getSystemService(LauncherApps::class.java)
        val out = mutableListOf<AppInfo>()
        val users = runCatching { la.profiles }.getOrDefault(listOf(Process.myUserHandle()))
        for (u in users) {
            runCatching { la.getActivityList(null, u) }.getOrNull()?.forEach { a ->
                if (a.componentName.packageName != ctx.packageName) {
                    out += AppInfo(a.label.toString().uppercase(), a.componentName, u)
                }
            }
        }
        val sorted = out.sortedBy { it.label }
        cache = sorted
        return sorted
    }

    fun find(ctx: Context, key: String): AppInfo? {
        val cn = ComponentName.unflattenFromString(key) ?: return null
        return list(ctx).firstOrNull { it.component == cn }
            ?: list(ctx).firstOrNull { it.component.packageName == cn.packageName }
    }

    fun launch(ctx: Context, app: AppInfo) {
        runCatching {
            ctx.getSystemService(LauncherApps::class.java).startMainActivity(app.component, app.user, null, null)
        }.onFailure { toast(ctx, "CAN'T OPEN ${app.label}") }
    }

    fun openInfo(ctx: Context, app: AppInfo) {
        runCatching {
            ctx.getSystemService(LauncherApps::class.java).startAppDetailsActivity(app.component, app.user, null, null)
        }
    }

    fun glyphFor(key: String): String? {
        Builtins.firstOrNull { it.key == key }?.let { return it.glyph }
        val pkg = (ComponentName.unflattenFromString(key)?.packageName ?: key).lowercase()
        return PackageGlyphs.firstOrNull { pkg.contains(it.first) }?.second
    }

    fun labelFor(ctx: Context, key: String): String =
        Builtins.firstOrNull { it.key == key }?.label ?: find(ctx, key)?.label ?: key.uppercase()

    fun icon(ctx: Context, app: AppInfo, px: Int): Bitmap? = runCatching {
        val la = ctx.getSystemService(LauncherApps::class.java)
        val info = la.resolveActivity(Intent().setComponent(app.component), app.user) ?: return null
        val icon = info.getIcon(0)
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        // Adaptive icons have opaque backgrounds; keep only the glyph layer so tinting yields a mono mark.
        if (icon is AdaptiveIconDrawable) {
            val layer = (if (Build.VERSION.SDK_INT >= 33) icon.monochrome else null) ?: icon.foreground
            val inset = px / 4
            layer?.setBounds(-inset, -inset, px + inset, px + inset)
            layer?.draw(c)
        } else {
            icon.setBounds(0, 0, px, px)
            icon.draw(c)
        }
        bmp
    }.getOrNull()

    /** Launch a shortcut entry (builtin key or flattened component). */
    fun open(ctx: Context, key: String) {
        val b = Builtins.firstOrNull { it.key == key }
        if (b == null) {
            find(ctx, key)?.let { launch(ctx, it) } ?: toast(ctx, "APP NOT INSTALLED")
            return
        }
        val intent = when (key) {
            "phone" -> Intent(Intent.ACTION_DIAL)
            "messages" -> Telephony.Sms.getDefaultSmsPackage(ctx)?.let { ctx.packageManager.getLaunchIntentForPackage(it) }
                ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
            "photos" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY)
            "camera" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            "browser" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
            "settings" -> Intent(Settings.ACTION_SETTINGS)
            "maps" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MAPS)
            "mail" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL)
            "music" -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
            "calendar" -> calendarIntent()
            else -> null
        } ?: return
        start(ctx, intent)
    }

    fun calendarIntent(): Intent = Intent(Intent.ACTION_VIEW).setData(
        CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            .appendPath(System.currentTimeMillis().toString()).build()
    )

    fun clockIntent(): Intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)

    fun start(ctx: Context, intent: Intent, fallbackToast: String = "NO APP FOR THIS"): Boolean = try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        toast(ctx, fallbackToast); false
    } catch (e: SecurityException) {
        toast(ctx, fallbackToast); false
    }

    fun web(ctx: Context, url: String) = start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun isInstalled(ctx: Context, pkg: String) =
        runCatching { ctx.packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)
}

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
