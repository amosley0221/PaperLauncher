package com.paperlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import com.paperlauncher.MediaListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MediaState(
    val title: String,
    val artist: String,
    val album: String,
    val art: Bitmap?,
    val playing: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    val positionAt: Long,  // elapsedRealtime when positionMs was sampled
    val speed: Float,
    val pkg: String,
) {
    fun positionNow(): Long =
        if (!playing) positionMs
        else (positionMs + ((SystemClock.elapsedRealtime() - positionAt) * speed).toLong()).coerceAtMost(durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE)
}

object Media {
    private val _state = MutableStateFlow<MediaState?>(null)
    val state: StateFlow<MediaState?> = _state
    private val _granted = MutableStateFlow(false)
    val granted: StateFlow<Boolean> = _granted

    private var msm: MediaSessionManager? = null
    private var controller: MediaController? = null
    private val main = Handler(Looper.getMainLooper())

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() { bind(null) }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list) }

    fun hasAccess(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(ctx.packageName)
    }

    fun requestAccess(ctx: Context) {
        Apps.start(ctx, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun start(ctx: Context) {
        _granted.value = hasAccess(ctx)
        if (!_granted.value) return
        val m = ctx.getSystemService(MediaSessionManager::class.java) ?: return
        msm = m
        val cn = ComponentName(ctx, MediaListener::class.java)
        runCatching {
            m.addOnActiveSessionsChangedListener(sessionsListener, cn, main)
            pick(m.getActiveSessions(cn))
        }
    }

    fun stop() {
        runCatching { msm?.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(callback)
        controller = null
    }

    private fun pick(list: List<MediaController>?) {
        val best = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        bind(best)
    }

    private fun bind(c: MediaController?) {
        if (c?.sessionToken == controller?.sessionToken && c != null) { publish(); return }
        controller?.unregisterCallback(callback)
        controller = c
        c?.registerCallback(callback, main)
        publish()
    }

    private fun publish() {
        val c = controller
        if (c == null) { _state.value = null; return }
        val md = c.metadata
        val ps = c.playbackState
        _state.value = MediaState(
            title = (md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "").uppercase(),
            artist = (md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "").uppercase(),
            album = (md?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "").uppercase(),
            art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            playing = ps?.state == PlaybackState.STATE_PLAYING,
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs = ps?.position ?: 0L,
            positionAt = ps?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
            speed = ps?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            pkg = c.packageName,
        )
    }

    fun toggle() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() = controller?.transportControls?.skipToNext()
    fun prev() = controller?.transportControls?.skipToPrevious()

    fun openPlayer(ctx: Context) {
        val c = controller
        val pi = c?.sessionActivity
        if (pi != null) {
            runCatching { pi.send() }.onSuccess { return }
        }
        val pkg = c?.packageName
        val launch = pkg?.let { ctx.packageManager.getLaunchIntentForPackage(it) }
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
        Apps.start(ctx, launch)
    }
}
