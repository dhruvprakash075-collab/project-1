package com.openfiles.feature.browser

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.common.FileItem
import com.openfiles.core.common.UiState
import com.openfiles.core.data.ArchiveRepository
import com.openfiles.core.data.BookmarkRepository
import com.openfiles.core.data.FileRepository
import com.openfiles.core.data.RecentsRepository
import com.openfiles.core.data.SecurityRepository
import com.openfiles.core.data.TrashRepository
import com.openfiles.core.data.db.TrashItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject

data class BrowserTab(val id: String, val path: Path)

data class BrowserUiData(val currentPath: Path, val items: List<FileItem>)

enum class ClipboardMode { COPY, MOVE }
data class ClipboardOp(val items: List<FileItem>, val mode: ClipboardMode)
data class OpProgress(val mode: ClipboardMode, val done: Int, val total: Int)
enum class SortOption { NAME, DATE, SIZE }

sealed interface BrowserEvent {
    data class ShowUndoDelete(val item: TrashItem) : BrowserEvent
    data class ShowError(val message: String) : BrowserEvent
}

private val DEFAULT_ROOT: Path = Paths.get("/storage/emulated/0")

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val repo: FileRepository,
    private val recentsRepository: RecentsRepository,
    private val trashRepository: TrashRepository,
    private val archiveRepository: ArchiveRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val securityRepository: SecurityRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<BrowserUiData>>(UiState.Loading)
    val state: StateFlow<UiState<BrowserUiData>> = _state.asStateFlow()

    private val _currentPath = MutableStateFlow(DEFAULT_ROOT)
    val currentPath: StateFlow<Path> = _currentPath.asStateFlow()

    val isCurrentPathBookmarked: StateFlow<Boolean> = _currentPath
        .flatMapLatest { path -> bookmarkRepository.bookmarks }
        .flatMapLatest { list -> flowOf(list.any { it.path == _currentPath.value.toString() }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _clipboard = MutableStateFlow<ClipboardOp?>(null)
    val clipboard: StateFlow<ClipboardOp?> = _clipboard.asStateFlow()

    private val _opProgress = MutableStateFlow<OpProgress?>(null)
    val opProgress: StateFlow<OpProgress?> = _opProgress.asStateFlow()

    private val _renameTarget = MutableStateFlow<FileItem?>(null)
    val renameTarget: StateFlow<FileItem?> = _renameTarget.asStateFlow()

    private val _showNewFolderDialog = MutableStateFlow(false)
    val showNewFolderDialog: StateFlow<Boolean> = _showNewFolderDialog.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NAME)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _tabs = MutableStateFlow(listOf(BrowserTab(id = "tab-0", path = DEFAULT_ROOT)))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow("tab-0")
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private var rawItems: List<FileItem> = emptyList()

    private val _events = MutableSharedFlow<BrowserEvent>()
    val events: SharedFlow<BrowserEvent> = _events.asSharedFlow()

    init {
        open(DEFAULT_ROOT)
    }

    fun open(dir: Path) {
        _currentPath.value = dir
        _selected.value = emptySet()
        _searchQuery.value = ""
        _isSearchActive.value = false
        _tabs.value = _tabs.value.map { if (it.id == _activeTabId.value) it.copy(path = dir) else it }
        loadCurrentFolder()
    }

    fun addTab() {
        val newId = "tab-${System.currentTimeMillis()}"
        _tabs.value = _tabs.value + BrowserTab(newId, DEFAULT_ROOT)
        switchTab(newId)
    }

    fun switchTab(tabId: String) {
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        _activeTabId.value = tabId
        open(tab.path)
    }

    fun closeTab(tabId: String) {
        if (_tabs.value.size <= 1) return
        val closingActive = tabId == _activeTabId.value
        _tabs.value = _tabs.value.filterNot { it.id == tabId }
        if (closingActive) switchTab(_tabs.value.last().id)
    }

    fun goUp(): Boolean {
        val parent = _currentPath.value.parent ?: return false
        open(parent)
        return true
    }

    fun refresh() = loadCurrentFolder()

    private fun loadCurrentFolder() = viewModelScope.launch {
        _state.value = UiState.Loading
        try {
            rawItems = repo.listPath(_currentPath.value)
            applyFilterAndSort()
        } catch (e: Exception) {
            _state.value = UiState.Error(e.message ?: "Could not open folder", e)
        }
    }

    private fun applyFilterAndSort() {
        val query = _searchQuery.value.trim()
        val filtered = if (query.isEmpty()) rawItems else rawItems.filter { it.name.contains(query, ignoreCase = true) }
        val sorted = when (_sortOption.value) {
            SortOption.NAME -> filtered.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
            SortOption.DATE -> filtered.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.lastModified })
            SortOption.SIZE -> filtered.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenByDescending { it.sizeBytes })
        }
        _state.value = if (sorted.isEmpty()) UiState.Empty else UiState.Content(BrowserUiData(_currentPath.value, sorted))
    }

    fun activateSearch() {
        _isSearchActive.value = true
    }

    fun deactivateSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
        applyFilterAndSort()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilterAndSort()
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        applyFilterAndSort()
    }

    fun toggleSelected(item: FileItem) {
        val key = item.uri.toString()
        _selected.value = if (key in _selected.value) _selected.value - key else _selected.value + key
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun recordOpened(file: FileItem) = viewModelScope.launch { recentsRepository.recordOpened(file) }

    fun toggleBookmark() = viewModelScope.launch {
        val path = _currentPath.value.toString()
        if (isCurrentPathBookmarked.value) {
            bookmarkRepository.removeBookmark(path)
        } else {
            bookmarkRepository.addBookmark(path, _currentPath.value.fileName?.toString() ?: path)
        }
    }

    private fun selectedItems(): List<FileItem> {
        val content = (_state.value as? UiState.Content)?.data ?: return emptyList()
        return content.items.filter { it.uri.toString() in _selected.value }
    }

    fun copySelectionToClipboard() {
        _clipboard.value = ClipboardOp(selectedItems(), ClipboardMode.COPY)
        clearSelection()
    }

    fun moveSelectionToClipboard() {
        _clipboard.value = ClipboardOp(selectedItems(), ClipboardMode.MOVE)
        clearSelection()
    }

    fun cancelClipboard() {
        _clipboard.value = null
    }

    fun pasteIntoCurrentFolder() = viewModelScope.launch {
        val op = _clipboard.value ?: return@launch
        _clipboard.value = null
        val destination = _currentPath.value
        val paths = op.items.mapNotNull { it.path?.let(Paths::get) }
        if (paths.isEmpty()) return@launch
        _opProgress.value = OpProgress(op.mode, 0, paths.size)
        val flow = when (op.mode) {
            ClipboardMode.COPY -> repo.copy(paths, destination)
            ClipboardMode.MOVE -> repo.move(paths, destination)
        }
        flow
            .onCompletion { cause -> _opProgress.value = null; if (cause == null) refresh() }
            .catch { e -> _events.emit(BrowserEvent.ShowError(e.message ?: "Operation failed")) }
            .collect { done -> _opProgress.value = OpProgress(op.mode, done, paths.size) }
    }

    fun deleteSelection() = viewModelScope.launch {
        val items = selectedItems()
        clearSelection()
        items.forEach { item ->
            val path = item.path
            if (path == null) {
                _events.emit(BrowserEvent.ShowError("Cannot delete ${item.name}"))
                return@forEach
            }
            val trashed = trashRepository.moveToTrash(File(path))
            if (trashed != null) {
                _events.emit(BrowserEvent.ShowUndoDelete(trashed))
            } else {
                _events.emit(BrowserEvent.ShowError("Could not delete ${item.name}"))
            }
        }
        refresh()
    }

    fun undoDelete(item: TrashItem) = viewModelScope.launch {
        val restored = trashRepository.restore(item)
        if (!restored) {
            _events.emit(BrowserEvent.ShowError("Could not restore ${item.originalPath.substringAfterLast('/')}"))
        }
        refresh()
    }

    fun requestRename(item: FileItem) {
        _renameTarget.value = item
    }

    fun dismissRename() {
        _renameTarget.value = null
    }

    fun confirmRename(newName: String) = viewModelScope.launch {
        val item = _renameTarget.value ?: return@launch
        _renameTarget.value = null
        clearSelection()
        item.path?.let { repo.rename(Paths.get(it), newName) }
        refresh()
    }

    fun requestNewFolder() {
        _showNewFolderDialog.value = true
    }

    fun dismissNewFolder() {
        _showNewFolderDialog.value = false
    }

    fun confirmNewFolder(name: String) = viewModelScope.launch {
        _showNewFolderDialog.value = false
        repo.createFolder(_currentPath.value, name)
        refresh()
    }

    fun compressSelection() = viewModelScope.launch {
        val items = selectedItems()
        clearSelection()
        val sources = items.mapNotNull { it.path?.let(::File) }
        if (sources.isEmpty()) return@launch
        val name = if (sources.size == 1) "${sources.first().nameWithoutExtension}.zip" else "Archive_${System.currentTimeMillis()}.zip"
        val destination = File(_currentPath.value.toFile(), name)
        try {
            archiveRepository.createZip(sources, destination)
        } catch (e: Exception) {
            _events.emit(BrowserEvent.ShowError(e.message ?: "Could not create zip"))
        }
        refresh()
    }

    fun lockSelection() = viewModelScope.launch {
        val items = selectedItems()
        clearSelection()
        items.forEach { item ->
            item.path?.let { path -> securityRepository.lockFile(File(path)) }
        }
        refresh()
    }
}
