package com.paperlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.paperlauncher.ui.LauncherRoot

class MainActivity : ComponentActivity() {
    private val vm: LauncherVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LauncherRoot(vm) }
    }

    override fun onResume() {
        super.onResume()
        applyStatusBar()
        vm.resume()
    }

    override fun onPause() {
        super.onPause()
        vm.pause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) vm.onHome()
    }

    fun applyStatusBar() {
        val c = WindowCompat.getInsetsController(window, window.decorView)
        c.isAppearanceLightStatusBars = true
        c.isAppearanceLightNavigationBars = true
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (vm.settings.hideStatusBar) c.hide(WindowInsetsCompat.Type.statusBars())
        else c.show(WindowInsetsCompat.Type.statusBars())
    }
}
