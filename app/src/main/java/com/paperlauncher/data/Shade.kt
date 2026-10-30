package com.paperlauncher.data

import android.annotation.SuppressLint
import android.content.Context

/** Pulls down the system notification shade (what most launchers do on swipe-down). */
object Shade {
    @SuppressLint("WrongConstant")
    fun expand(ctx: Context, quick: Boolean) {
        val ok = runCatching {
            val sbm = ctx.getSystemService("statusbar")
            val method = Class.forName("android.app.StatusBarManager")
                .getMethod(if (quick) "expandSettingsPanel" else "expandNotificationsPanel")
            method.invoke(sbm)
        }.isSuccess
        if (!ok) toast(ctx, "CAN'T OPEN THE NOTIFICATION SHADE")
    }
}
