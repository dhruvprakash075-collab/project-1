package com.openfiles

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.openfiles.core.common.Route
import com.openfiles.core.data.SettingsRepository
import com.openfiles.core.data.ThemePref
import com.openfiles.core.ui.theme.OpenFilesTheme
import com.openfiles.navigation.OpenFilesNavGraph
import com.openfiles.shortcuts.ACTION_OPEN_BOOKMARK
import com.openfiles.shortcuts.EXTRA_BOOKMARK_PATH
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startRoute = routeFromIntent(intent)
        setContent {
            val theme by settingsRepository.theme.collectAsState(initial = ThemePref.System)
            OpenFilesTheme(theme = theme) {
                OpenFilesNavGraph(startRoute = startRoute)
            }
        }
    }

    private fun routeFromIntent(intent: Intent?): Route {
        val path = intent?.takeIf { it.action == ACTION_OPEN_BOOKMARK }
            ?.getStringExtra(EXTRA_BOOKMARK_PATH)
        return if (path != null) Route.Browser(path) else Route.Browser()
    }
}
