package com.openfiles.feature.browser

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.common.FileItem
import com.openfiles.core.common.UiState
import com.openfiles.core.data.FileRepository
import com.openfiles.core.data.RecentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject

data class BrowserUiData(val currentPath: Path, val items: List<FileItem>)

sealed interface FileOpAction {
    data class Rename(val item: FileItem) : FileOpAction
    data class Delete(val items: List<FileItem>) : FileOpAction
    data class Copy(val items: List<FileItem>, val destination: Path) : FileOpAction
    data class Move(val items: List<FileItem>, val destination: Path) : FileOpAction
    data class NewFolder(val name: String) : FileOpAction
}

private val DEFAULT_ROOT: Path = Paths.get("/storage/emulated/0")

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val repo: FileRepository,
    private val recentsRepository: RecentsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<BrowserUiData>>(UiState.Loading)
    val state: StateFlow<UiState<BrowserUiData>> = _state.asStateFlow()

    private var currentPath: Path = DEFAULT_ROOT

    /** Multi-select state for the current listing, keyed by item uri string. */
    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    init {
        open(DEFAULT_ROOT)
    }

    fun open(dir: Path) {
        currentPath = dir
        _selected.value = emptySet()
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val items = repo.listPath(dir)
                if (items.isEmpty()) UiState.Empty else UiState.Content(BrowserUiData(dir, items))
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Could not open folder", e)
            }
        }
    }

    fun goUp(): Boolean {
        val parent = currentPath.parent ?: return false
        open(parent)
        return true
    }

    fun refresh() = open(currentPath)

    fun toggleSelected(item: FileItem) {
        val key = item.uri.toString()
        _selected.value = if (key in _selected.value) _selected.value - key else _selected.value + key
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun recordOpened(file: FileItem) = viewModelScope.launch { recentsRepository.recordOpened(file) }

    fun perform(action: FileOpAction) = viewModelScope.launch {
        when (action) {
            is FileOpAction.Rename -> action.item.path?.let { repo.rename(Paths.get(it), action.item.name) }
            is FileOpAction.Delete -> repo.delete(action.items.mapNotNull { it.path?.let(Paths::get) })
            is FileOpAction.Copy -> repo.copy(action.items.mapNotNull { it.path?.let(Paths::get) }, action.destination)
            is FileOpAction.Move -> repo.move(action.items.mapNotNull { it.path?.let(Paths::get) }, action.destination)
            is FileOpAction.NewFolder -> repo.createFolder(currentPath, action.name)
        }
        refresh()
    }
}
