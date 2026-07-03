package com.openfiles.feature.viewer.office

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.common.OfficeKind
import com.openfiles.core.common.UiState
import com.openfiles.core.data.AnnotationRepository
import com.openfiles.core.data.DocSearchMatch
import com.openfiles.core.data.DocumentSearchRepository
import com.openfiles.core.data.ExcelRepository
import com.openfiles.core.data.SlidesRepository
import com.openfiles.core.data.WordRepository
import com.openfiles.core.data.db.DocAnnotation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OfficeContent {
    data class Excel(val sheets: List<ExcelRepository.Sheet>) : OfficeContent
    data class Word(val paragraphs: List<String>) : OfficeContent
    data class Slides(val slides: List<SlidesRepository.Slide>) : OfficeContent
}

sealed interface OfficeEvent {
    data class RequestExternalViewer(val uriString: String) : OfficeEvent
    data class JumpToMatch(val anchorIndex: Int) : OfficeEvent
}

/**
 * Read-only Office viewer backed by Apache POI (see ADR: POI kept read-only for v1).
 * Parsing always happens off the main thread; the UI only ever sees Loading/Content/Error.
 * PPTX with little to no extractable text (image-heavy slides) falls back to a neutral system
 * chooser (ACTION_VIEW) instead of showing an empty/useless text view.
 *
 * Ring 3 F3 additions: in-document text search over the same extracted text already used to
 * render the screen (no re-parsing), and lightweight per-paragraph text annotations (Room-backed,
 * ADR-029). PDF is intentionally not covered -- see DocumentSearchRepository's kdoc.
 */
@HiltViewModel
class OfficeViewerViewModel @Inject constructor(
    private val excelRepository: ExcelRepository,
    private val wordRepository: WordRepository,
    private val slidesRepository: SlidesRepository,
    private val searchRepository: DocumentSearchRepository,
    private val annotationRepository: AnnotationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<OfficeContent>>(UiState.Loading)
    val state: StateFlow<UiState<OfficeContent>> = _state.asStateFlow()

    private val _events = MutableSharedFlow<OfficeEvent>()
    val events: SharedFlow<OfficeEvent> = _events.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<DocSearchMatch>>(emptyList())
    val searchResults: StateFlow<List<DocSearchMatch>> = _searchResults.asStateFlow()

    private val _searchActive = MutableStateFlow(false)
    val searchActive: StateFlow<Boolean> = _searchActive.asStateFlow()

    private var currentUri: Uri? = null
    private var currentKind: OfficeKind? = null

    fun annotationsFlow(documentUri: String): Flow<List<DocAnnotation>> =
        annotationRepository.annotationsForDocument(documentUri)

    fun open(uriString: String, kind: OfficeKind) {
        currentUri = Uri.parse(uriString)
        currentKind = kind
        viewModelScope.launch {
            _state.value = UiState.Loading
            val uri = Uri.parse(uriString)
            _state.value = try {
                when (kind) {
                    OfficeKind.XLSX -> {
                        val sheets = excelRepository.readWorkbook(uri)
                        if (sheets.all { it.rows.isEmpty() }) UiState.Empty else UiState.Content(OfficeContent.Excel(sheets))
                    }
                    OfficeKind.DOCX -> {
                        val paragraphs = wordRepository.readParagraphs(uri)
                        if (paragraphs.isEmpty()) UiState.Empty else UiState.Content(OfficeContent.Word(paragraphs))
                    }
                    OfficeKind.PPTX -> {
                        val slides = slidesRepository.readSlides(uri)
                        val totalText = slides.sumOf { slide -> slide.textBlocks.sumOf { it.length } }
                        if (slides.isEmpty() || totalText < 20) {
                            _events.emit(OfficeEvent.RequestExternalViewer(uriString))
                            UiState.Empty
                        } else {
                            UiState.Content(OfficeContent.Slides(slides))
                        }
                    }
                }
            } catch (e: Exception) {
                UiState.Error(e.message ?: "Could not open document", e)
            }
        }
    }

    fun toggleSearch() {
        _searchActive.value = !_searchActive.value
        if (!_searchActive.value) {
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        val uri = currentUri ?: return
        val kind = currentKind ?: return
        viewModelScope.launch {
            _searchResults.value = when (kind) {
                OfficeKind.DOCX -> searchRepository.searchWord(uri, query)
                OfficeKind.PPTX -> searchRepository.searchSlides(uri, query)
                OfficeKind.XLSX -> searchRepository.searchExcel(uri, query)
            }
        }
    }

    fun jumpTo(match: DocSearchMatch) = viewModelScope.launch {
        _events.emit(OfficeEvent.JumpToMatch(match.anchorIndex))
    }

    fun addAnnotation(anchorIndex: Int, note: String, sheetName: String? = null) {
        val uri = currentUri ?: return
        viewModelScope.launch {
            annotationRepository.addAnnotation(uri.toString(), anchorIndex, note, sheetName)
        }
    }

    fun removeAnnotation(annotation: DocAnnotation) = viewModelScope.launch {
        annotationRepository.removeAnnotation(annotation)
    }
}
