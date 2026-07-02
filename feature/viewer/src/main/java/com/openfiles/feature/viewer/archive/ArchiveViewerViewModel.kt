package com.openfiles.feature.viewer.archive

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.common.UiState
import com.openfiles.core.data.ArchiveEntryInfo
import com.openfiles.core.data.ArchiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ArchiveViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val archiveRepository: ArchiveRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<ArchiveEntryInfo>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ArchiveEntryInfo>>> = _state.asStateFlow()

    private var localCopy: File? = null

    fun open(uriString: String, name: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val file = withContext(Dispatchers.IO) { copyToCache(Uri.parse(uriString), name) }
                localCopy = file
                val entries = when {
                    name.endsWith(".zip", true) -> archiveRepository.listZipEntries(file)
                    name.endsWith(".rar", true) -> archiveRepository.listRarEntries(file)
                    else -> archiveRepository.listTarEntries(file)
                }
                if (entries.isEmpty()) UiState.Empty else UiState.Content(entries)
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Could not read archive", e)
            }
        }
    }

    fun extractAll(destination: File) {
        val file = localCopy ?: return
        viewModelScope.launch {
            when {
                file.name.endsWith(".zip", true) -> archiveRepository.extractZip(file, destination)
                file.name.endsWith(".rar", true) -> archiveRepository.extractRar(file, destination)
                else -> archiveRepository.extractGeneric(file, destination)
            }
        }
    }

    private fun copyToCache(uri: Uri, name: String): File {
        val out = File(context.cacheDir, "archive_$name")
        context.contentResolver.openInputStream(uri)!!.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return out
    }
}
