package com.openfiles.feature.viewer.office

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfiles.core.common.OfficeKind
import com.openfiles.core.common.UiState
import com.openfiles.core.data.ExcelRepository
import com.openfiles.core.data.SlidesRepository
import com.openfiles.core.data.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
}

/**
 * Read-only Office viewer backed by Apache POI (see ADR: POI kept read-only for v1).
 * Parsing always happens off the main thread; the UI only ever sees Loading/Content/Error.
 * PPTX with little to no extractable text (image-heavy slides) falls back to a neutral system
 * chooser (ACTION_VIEW) instead of showing an empty/useless text view.
 */
@HiltViewModel
class OfficeViewerViewModel @Inject constructor(
    private val excelRepository: ExcelRepository,
    private val wordRepository: WordRepository,
    private val slidesRepository: SlidesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<OfficeContent>>(UiState.Loading)
    val state: StateFlow<UiState<OfficeContent>> = _state.asStateFlow()

    private val _events = MutableSharedFlow<OfficeEvent>()
    val events: SharedFlow<OfficeEvent> = _events.asSharedFlow()

    fun open(uriString: String, kind: OfficeKind) {
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
}
