package com.paperlauncher.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import java.net.URLEncoder

data class Agent(val key: String, val name: String, val glyph: String, val pkg: String, val webUrl: String)

object Agents {
    val all = listOf(
        Agent("claude", "CLAUDE", "auto_awesome", "com.anthropic.claude", "https://claude.ai/new?q="),
        Agent("grok", "GROK", "bolt", "ai.x.grok", "https://grok.com/?q="),
        Agent("codex", "CODEX", "terminal", "com.openai.chatgpt", "https://chatgpt.com/?q="),
    )

    fun of(key: String) = all.firstOrNull { it.key == key } ?: all.first()

    /**
     * Hands the prompt to the agent's app via ACTION_SEND; if the app can't take shared text,
     * launches it with the prompt on the clipboard; if it isn't installed, opens the web app.
     */
    fun send(ctx: Context, key: String, text: String) {
        val a = of(key)
        val pm = ctx.packageManager
        val prompt = text.trim()
        if (Apps.isInstalled(ctx, a.pkg)) {
            if (prompt.isNotEmpty()) {
                val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, prompt).setPackage(a.pkg)
                if (share.resolveActivity(pm) != null && Apps.start(ctx, share)) return
                ctx.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("prompt", prompt))
                toast(ctx, "COPIED · PASTE IN ${a.name}")
            }
            pm.getLaunchIntentForPackage(a.pkg)?.let { Apps.start(ctx, it) }
            return
        }
        Apps.web(ctx, a.webUrl + URLEncoder.encode(prompt, "UTF-8"))
    }
}
