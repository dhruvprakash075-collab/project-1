package com.openfiles.appmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.common.UiState
import com.openfiles.core.data.AppManagerRepository
import com.openfiles.core.data.InstalledApp
import com.openfiles.shizuku.ShizukuUninstallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface AppManagerEvent {
    data class BackupComplete(val path: String) : AppManagerEvent
    data class ShowError(val message: String) : AppManagerEvent
    data class BulkUninstallComplete(val succeeded: Int, val failed: Int) : AppManagerEvent
}

@HiltViewModel
class AppManagerViewModel @Inject constructor(
    private val repository: AppManagerRepository,
    private val shizukuUninstallRepository: ShizukuUninstallRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<InstalledApp>>>(UiState.Loading)
    val state: StateFlow<UiState<List<InstalledApp>>> = _state.asStateFlow()

    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _events = MutableSharedFlow<AppManagerEvent>()
    val events: SharedFlow<AppManagerEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun toggleShowSystemApps() {
        _showSystemApps.value = !_showSystemApps.value
        load()
    }

    private fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        val apps = repository.listInstalledApps(includeSystemApps = _showSystemApps.value)
        _state.value = if (apps.isEmpty()) UiState.Empty else UiState.Content(apps)
    }

    fun backup(app: InstalledApp) = viewModelScope.launch {
        val downloads = File("/storage/emulated/0/Download/OpenFiles-Backups").apply { mkdirs() }
        val result = repository.backupApk(app, downloads)
        if (result != null) {
            _events.emit(AppManagerEvent.BackupComplete(result.absolutePath))
        } else {
            _events.emit(AppManagerEvent.ShowError("Could not back up ${app.label}"))
        }
    }

    fun toggleSelected(packageName: String) {
        _selected.value = if (packageName in _selected.value) {
            _selected.value - packageName
        } else {
            _selected.value + packageName
        }
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun bulkUninstallSelected() = viewModelScope.launch {
        val targets = _selected.value.toList()
        if (targets.isEmpty()) return@launch
        val results = shizukuUninstallRepository.uninstallPackages(targets)
        _selected.value = emptySet()
        load()
        _events.emit(
            AppManagerEvent.BulkUninstallComplete(
                succeeded = results.count { it.value },
                failed = results.count { !it.value },
            ),
        )
    }
}
