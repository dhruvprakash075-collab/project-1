package com.openfiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.openfiles.core.data.SettingsRepository
import com.openfiles.core.data.ThemePref
import com.openfiles.core.ui.theme.OpenFilesTheme
import com.openfiles.navigation.OpenFilesNavGraph
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
        setContent {
            val theme by settingsRepository.theme.collectAsState(initial = ThemePref.System)
            OpenFilesTheme(theme = theme) {
                OpenFilesNavGraph()
            }
        }
    }
}
