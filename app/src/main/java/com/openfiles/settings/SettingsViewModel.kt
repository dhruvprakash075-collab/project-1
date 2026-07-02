package com.openfiles.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.data.SettingsRepository
import com.openfiles.core.data.ThemePref
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: ThemePref = ThemePref.System,
    val showHiddenFiles: Boolean = false,
    val gridView: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val state = combine(
        settingsRepository.theme,
        settingsRepository.showHiddenFiles,
        settingsRepository.gridView,
    ) { theme, hidden, grid -> SettingsUiState(theme, hidden, grid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setTheme(theme: ThemePref) = viewModelScope.launch { settingsRepository.setTheme(theme) }
    fun setShowHiddenFiles(show: Boolean) = viewModelScope.launch { settingsRepository.setShowHiddenFiles(show) }
    fun setGridView(grid: Boolean) = viewModelScope.launch { settingsRepository.setGridView(grid) }
}
