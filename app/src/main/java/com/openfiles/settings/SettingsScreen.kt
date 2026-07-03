package com.openfiles.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfiles.core.data.ThemePref
import androidx.compose.ui.unit.dp

/** Settings: theme (Light/Dark/True Black/System), hidden files toggle, default view mode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text("Theme", modifier = Modifier.padding(16.dp))
            ThemePref.entries.forEach { theme ->
                ListItem(
                    headlineContent = { Text(theme.name) },
                    leadingContent = {
                        RadioButton(selected = state.theme == theme, onClick = { viewModel.setTheme(theme) })
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Show hidden files") },
                trailingContent = {
                    Switch(checked = state.showHiddenFiles, onCheckedChange = viewModel::setShowHiddenFiles)
                },
            )
            ListItem(
                headlineContent = { Text("Grid view by default") },
                trailingContent = {
                    Switch(checked = state.gridView, onCheckedChange = viewModel::setGridView)
                },
            )
        }
    }
}
