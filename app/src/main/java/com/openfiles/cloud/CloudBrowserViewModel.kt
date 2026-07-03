package com.openfiles.cloud

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.common.UiState
import com.openfiles.core.data.SmbFileItem
import com.openfiles.core.data.SmbRepository
import com.openfiles.core.data.db.SmbConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface CloudBrowserEvent {
    data class OpenFile(val intent: Intent) : CloudBrowserEvent
    data class ShowError(val message: String) : CloudBrowserEvent
}

@HiltViewModel
class CloudBrowserViewModel @Inject constructor(
    private val repository: SmbRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _connection = MutableStateFlow<SmbConnection?>(null)
    private val _path = MutableStateFlow("")
    val path: StateFlow<String> = _path.asStateFlow()
    private val _state = MutableStateFlow<UiState<List<SmbFileItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<SmbFileItem>>> = _state.asStateFlow()
    private val _events = MutableSharedFlow<CloudBrowserEvent>()
    val events: SharedFlow<CloudBrowserEvent> = _events.asSharedFlow()
    private var started = false
    private val tempFiles = mutableListOf<File>()

    fun start(connectionId: Long, initialPath: String) {
        if (started) return
        started = true
        _path.value = initialPath
        viewModelScope.launch {
            repository.connections.collect { list ->
                _connection.value = list.firstOrNull { it.id == connectionId } ?: _connection.value
                load()
            }
        }
    }

    fun open(path: String) { _path.value = path; load() }

    private fun load() = viewModelScope.launch {
        val connection = _connection.value ?: return@launch
        _state.value = UiState.Loading
        repository.list(connection, _path.value)
            .onSuccess { items -> _state.value = if (items.isEmpty()) UiState.Empty else UiState.Content(items) }
            .onFailure { e -> _state.value = UiState.Error(e.message ?: "Could not connect", e) }
    }

    fun openFile(item: SmbFileItem) = viewModelScope.launch {
        val connection = _connection.value ?: return@launch
        val safeName = item.name.replace(Regex("""[\\/:*?"<>|]"""), "_")
        val cacheFile = File(context.cacheDir, "smb_${System.currentTimeMillis()}_$safeName")
        val remotePath = if (_path.value.isBlank()) item.name else "${_path.value}\\${item.name}"
        repository.download(connection, remotePath, cacheFile)
            .onSuccess { file ->
                tempFiles += file
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                _events.emit(CloudBrowserEvent.OpenFile(Intent.createChooser(intent, null)))
            }
            .onFailure { e -> _events.emit(CloudBrowserEvent.ShowError(e.message ?: "Could not download ${item.name}")) }
    }

    override fun onCleared() {
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
        super.onCleared()
    }
}
