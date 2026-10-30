package com.paperlauncher

import android.service.notification.NotificationListenerService

/** Exists only so the user can grant notification access, which unlocks MediaSession control. */
class MediaListener : NotificationListenerService()
